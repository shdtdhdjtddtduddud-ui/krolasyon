"""Model toolkit: bones/cubes in absolute vanilla model space (y down, front = -z, feet at y=24),
box-UV packing, Java LayerDefinition + AnimationDefinition export, and a software preview renderer
that uses the exact same vanilla transform + UV conventions."""
import math, numpy as np
from PIL import Image


class Cube:
    def __init__(self, frm, size, mat, inflate=0.0, feat=None, plane=False, seed=0):
        self.frm = [float(v) for v in frm]
        self.size = [float(v) for v in size]
        self.mat = mat
        self.inflate = inflate
        self.feat = feat or {}
        self.plane = plane
        self.seed = seed
        self.uv = None


class Bone:
    def __init__(self, name, parent, pivot, rot=(0, 0, 0)):
        self.name, self.parent = name, parent
        self.pivot = [float(v) for v in pivot]
        self.rot = [float(v) for v in rot]  # degrees, vanilla ZYX order
        self.cubes = []
        self.children = []


class Model:
    def __init__(self, name, tex_w, tex_h):
        self.name, self.tw, self.th = name, tex_w, tex_h
        self.bones = {}
        self.order = []
        self._seed = 1

    def bone(self, name, parent, pivot, rot=(0, 0, 0)):
        b = Bone(name, parent, pivot, rot)
        pv = self.bones[parent].pivot if parent else [0.0, 0.0, 0.0]
        b.off = [b.pivot[i] - pv[i] for i in range(3)]
        self.bones[name] = b
        self.order.append(name)
        if parent:
            self.bones[parent].children.append(name)
        return b

    def cube(self, bone, frm, size, mat, **kw):
        self._seed += 1
        b = self.bones[bone]
        c = Cube(frm, size, mat, seed=self._seed, **kw)
        c.lfrm = [c.frm[i] - b.pivot[i] for i in range(3)]
        b.cubes.append(c)
        return c

    def cube_l(self, bone, lfrom, size, mat, **kw):
        self._seed += 1
        c = Cube(lfrom, size, mat, seed=self._seed, **kw)
        c.lfrm = [float(v) for v in lfrom]
        self.bones[bone].cubes.append(c)
        return c

    def world(self, name):
        """rest-pose world matrix (4x4) of bone"""
        if name is None:
            return np.eye(4)
        b = self.bones[name]
        M = np.eye(4)
        M[:3, 3] = b.off
        M[:3, :3] = rot_zyx(*np.radians(b.rot))
        return self.world(b.parent) @ M

    def bone_w(self, name, parent, wpivot, wR):
        """bone given its world pivot and world rotation matrix"""
        P = self.world(parent)
        Rl = P[:3, :3].T @ wR
        off = P[:3, :3].T @ (np.array(wpivot, float) - P[:3, 3])
        y = math.asin(max(-1, min(1, -Rl[2, 0])))
        x = math.atan2(Rl[2, 1], Rl[2, 2])
        z = math.atan2(Rl[1, 0], Rl[0, 0])
        b = Bone(name, parent, wpivot, [math.degrees(x), math.degrees(y), math.degrees(z)])
        b.off = [float(v) for v in off]
        self.bones[name] = b
        self.order.append(name)
        if parent:
            self.bones[parent].children.append(name)
        return b

    def seg(self, name, parent, A, B, w, d, mat, extend=0.0, **kw):
        """limb segment: bone at A whose local +y points at B; cube spans the segment"""
        A = np.array(A, float); B = np.array(B, float)
        v = B - A; L = np.linalg.norm(v); v = v / L
        x = math.asin(max(-1, min(1, v[2])))
        z = math.atan2(-v[0], v[1])
        R = rot_zyx(x, 0, z)
        self.bone_w(name, parent, A, R)
        self.cube_l(name, (-w / 2, -extend, -d / 2), (w, L + 2 * extend, d), mat, **kw)
        return L

    # ---------------- UV packing (box UV) ----------------
    def pack(self):
        cubes = [c for b in self.order for c in self.bones[b].cubes]
        def dims(c):
            w, h, d = [math.ceil(v) for v in c.size]
            return 2 * (d + w), d + h
        idx = sorted(range(len(cubes)), key=lambda i: -dims(cubes[i])[1])
        x = y = rowh = 0
        for i in idx:
            c = cubes[i]
            W, H = dims(c)
            W, H = max(W, 1), max(H, 1)
            if x + W > self.tw:
                x, y, rowh = 0, y + rowh, 0
            c.uv = (x, y)
            x += W
            rowh = max(rowh, H)
        if y + rowh > self.th:
            raise RuntimeError(f"texture too small for {self.name}: need {y + rowh}")
        return y + rowh

    # face rects in texture space: name -> (u, v, w, h)
    @staticmethod
    def face_rects(c):
        u, v = c.uv
        dx, dy, dz = c.size
        return {
            'top': (u + dz, v, dx, dz),
            'bottom': (u + dz + dx, v, dx, dz),
            'west': (u, v + dz, dz, dy),          # minX side
            'north': (u + dz, v + dz, dx, dy),    # front (-z)
            'east': (u + dz + dx, v + dz, dz, dy),  # maxX side
            'south': (u + 2 * dz + dx, v + dz, dx, dy),  # back (+z)
        }

    # ---------------- JSON export (runtime loaded by MobModelLoader) ----------------
    def to_json(self):
        bones = []
        for name in self.order:
            b = self.bones[name]
            cubes = []
            for c in b.cubes:
                o = c.lfrm
                cubes.append([int(c.uv[0]), int(c.uv[1]), _r(o[0]), _r(o[1]), _r(o[2]), _r(c.size[0]), _r(c.size[1]), _r(c.size[2]), _r(c.inflate)])
            bones.append([name, b.parent, [_r(v) for v in b.off], [_r(math.radians(a)) for a in b.rot], cubes])
        return {'tw': self.tw, 'th': self.th, 'bones': bones}

    # ---------------- Java export ----------------
    def java_layer(self):
        out = []
        var = {None: 'root'}
        for name in self.order:
            b = self.bones[name]
            off = b.off
            cl = "CubeListBuilder.create()"
            for c in b.cubes:
                o = c.lfrm
                cl += (f".texOffs({c.uv[0]}, {c.uv[1]}).addBox({f(o[0])}, {f(o[1])}, {f(o[2])}, "
                       f"{f(c.size[0])}, {f(c.size[1])}, {f(c.size[2])}, new CubeDeformation({f(c.inflate)}))")
            r = [math.radians(a) for a in b.rot]
            pose = (f"PartPose.offsetAndRotation({f(off[0])}, {f(off[1])}, {f(off[2])}, "
                    f"{f(r[0])}, {f(r[1])}, {f(r[2])})")
            pvar = var[b.parent]
            v = 'p_' + name
            var[name] = v
            out.append(f"        PartDefinition {v} = {pvar}.addOrReplaceChild(\"{name}\", {cl}, {pose});")
        return "\n".join(out)


def _r(x, n=5):
    return round(float(x), n)


def f(x):
    s = f"{x:.5f}".rstrip('0').rstrip('.')
    if s in ('-0', ''):
        s = '0'
    if '.' not in s:
        s += '.0'
    return s + 'F'


# ---------------- Animations ----------------
class Anim:
    def __init__(self, name, length, loop=False):
        self.name, self.length, self.loop = name, length, loop
        self.ch = []  # (bone, kind, [(t, (x,y,z), interp)])

    def rot(self, bone, *keys, interp='C'):
        self.ch.append((bone, 'ROTATION', [(k[0], k[1], k[2] if len(k) > 2 else interp) for k in keys]))
        return self

    def pos(self, bone, *keys, interp='C'):
        self.ch.append((bone, 'POSITION', [(k[0], k[1], k[2] if len(k) > 2 else interp) for k in keys]))
        return self

    def scl(self, bone, *keys, interp='C'):
        self.ch.append((bone, 'SCALE', [(k[0], k[1], k[2] if len(k) > 2 else interp) for k in keys]))
        return self

    def java(self, const):
        s = f"    public static final AnimationDefinition {const} = make{const}();\n\n    private static AnimationDefinition make{const}() {{\n        return AnimationDefinition.Builder.withLength({f(self.length)})"
        if self.loop:
            s += ".looping()"
        for bone, kind, keys in self.ch:
            fn = {'ROTATION': 'degreeVec', 'POSITION': 'posVec', 'SCALE': 'scaleVec'}[kind]
            kf = ", ".join(
                f"new Keyframe({f(t)}, KeyframeAnimations.{fn}({f(v[0])}, {f(v[1])}, {f(v[2])}), "
                f"AnimationChannel.Interpolations.{'CATMULLROM' if i == 'C' else 'LINEAR'})" for t, v, i in keys)
            s += f"\n        .addAnimation(\"{bone}\", new AnimationChannel(AnimationChannel.Targets.{kind}, {kf}))"
        return s + "\n        .build();\n    }"

    def to_json(self):
        ch = []
        for bone, kind, keys in self.ch:
            k = {'ROTATION': 'r', 'POSITION': 'p', 'SCALE': 's'}[kind]
            ch.append([bone, k, [[_r(t, 4), _r(v[0], 4), _r(v[1], 4), _r(v[2], 4), 'c' if i == 'C' else 'l'] for t, v, i in keys]])
        return {'len': self.length, 'loop': bool(self.loop), 'ch': ch}

    def sample(self, t):
        """returns dict bone -> {'ROTATION':vec,'POSITION':vec,'SCALE':vec} (vanilla semantic, pos in posVec units)"""
        res = {}
        for bone, kind, keys in self.ch:
            ts = [k[0] for k in keys]
            if t <= ts[0]:
                v = keys[0][1]
            elif t >= ts[-1]:
                v = keys[-1][1]
            else:
                i = max(j for j in range(len(ts)) if ts[j] <= t)
                a, b = keys[i], keys[i + 1]
                u = (t - a[0]) / (b[0] - a[0]) if b[0] > a[0] else 0
                v = [a[1][k] + (b[1][k] - a[1][k]) * u for k in range(3)]
            res.setdefault(bone, {})[kind] = v
        return res


# ---------------- Preview renderer ----------------
def rot_zyx(xr, yr, zr):
    cx, sx, cy, sy, cz, sz = math.cos(xr), math.sin(xr), math.cos(yr), math.sin(yr), math.cos(zr), math.sin(zr)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def render(model, tex, pose=None, yaw=0.0, pitch=0.0, size=600, bg=(60, 64, 70), extra_glow=None):
    """pose: dict bone->{'ROTATION','POSITION','SCALE'}; yaw degrees rotates the model around Y for viewing."""
    tex = np.asarray(tex.convert('RGBA')).astype(np.float32)
    glow = np.asarray(extra_glow.convert('RGBA')).astype(np.float32) if extra_glow is not None else None
    pose = pose or {}
    mats = {}
    for name in model.order:
        b = model.bones[name]
        off = np.array(b.off, float)
        rot = np.radians(b.rot)
        sc = np.ones(3)
        p = pose.get(name, {})
        if 'POSITION' in p:
            v = p['POSITION']; off = off + np.array([v[0], -v[1], v[2]])
        if 'ROTATION' in p:
            rot = rot + np.radians(p['ROTATION'])
        if 'SCALE' in p:
            sc = np.array(p['SCALE'])
        M = np.eye(4)
        M[:3, 3] = off
        M[:3, :3] = rot_zyx(*rot) @ np.diag(sc)
        parentM = mats[b.parent] if b.parent else np.eye(4)
        mats[name] = parentM @ M
    # view: model rendered upright: flip y (y down -> up), yaw rotate
    ya, pa = math.radians(yaw), math.radians(pitch)
    V = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
    P = np.array([[1, 0, 0], [0, math.cos(pa), -math.sin(pa)], [0, math.sin(pa), math.cos(pa)]])
    # camera looks from -z toward +z (sees front). screen x = -model x (model -x on viewer left)
    quads = []
    for name in model.order:
        b = model.bones[name]
        M = mats[name]
        for c in b.cubes:
            x0, y0, z0 = [c.lfrm[i] - c.inflate for i in range(3)]
            x1, y1, z1 = [c.lfrm[i] + c.size[i] + c.inflate for i in range(3)]
            R = Model.face_rects(c)
            # corner definitions: (corner for texture (u0,v0),(u1,v0),(u1,v1),(u0,v1))
            faces = {
                'north': [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],
                'south': [(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)],
                'west': [(x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1)],
                'east': [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)],
                'top': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
                'bottom': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
            }
            for fn, pts in faces.items():
                u, v, w, h = R[fn]
                if w <= 0 or h <= 0:
                    continue
                P3 = []
                for p in pts:
                    q = M @ np.array([p[0], p[1], p[2], 1.0])
                    q = q[:3]
                    q = np.array([-q[0], -(q[1] - 24.0), q[2]])  # vanilla scale(-1,-1,1): y up, feet at 0
                    q = P @ (V @ q)
                    P3.append(q)
                quads.append((np.array(P3), (u, v, w, h)))
    allp = np.concatenate([q[0] for q in quads])
    minx, maxx = allp[:, 0].min(), allp[:, 0].max()
    miny, maxy = allp[:, 1].min(), allp[:, 1].max()
    scale = (size - 40) / max(maxx - minx, maxy - miny)
    cxm, cym = (minx + maxx) / 2, (miny + maxy) / 2
    img = np.zeros((size, size, 3), np.float32); img[:] = bg
    zb = np.full((size, size), 1e9, np.float32)
    light = np.array([0.4, 0.8, -0.5]); light /= np.linalg.norm(light)
    for P3, (u, v, w, h) in quads:
        S = np.stack([(-(P3[:, 0] - cxm)) * scale + size / 2, -(P3[:, 1] - cym) * scale + size / 2, P3[:, 2]], 1)
        n = np.cross(P3[1] - P3[0], P3[3] - P3[0])
        nn = np.linalg.norm(n)
        shade = 1.0
        if nn > 1e-9:
            n = n / nn
            shade = 0.55 + 0.45 * abs(np.dot(n, light))
        for tri, uvs in (((0, 1, 2), ((0, 0), (1, 0), (1, 1))), ((0, 2, 3), ((0, 0), (1, 1), (0, 1)))):
            a, b_, c_ = S[tri[0]], S[tri[1]], S[tri[2]]
            xmin, xmax = int(max(0, min(a[0], b_[0], c_[0]))), int(min(size - 1, max(a[0], b_[0], c_[0]) + 1))
            ymin, ymax = int(max(0, min(a[1], b_[1], c_[1]))), int(min(size - 1, max(a[1], b_[1], c_[1]) + 1))
            if xmax < xmin or ymax < ymin:
                continue
            den = (b_[1] - c_[1]) * (a[0] - c_[0]) + (c_[0] - b_[0]) * (a[1] - c_[1])
            if abs(den) < 1e-9:
                continue
            X, Y = np.meshgrid(np.arange(xmin, xmax + 1) + 0.5, np.arange(ymin, ymax + 1) + 0.5)
            l1 = ((b_[1] - c_[1]) * (X - c_[0]) + (c_[0] - b_[0]) * (Y - c_[1])) / den
            l2 = ((c_[1] - a[1]) * (X - c_[0]) + (a[0] - c_[0]) * (Y - c_[1])) / den
            l3 = 1 - l1 - l2
            m = (l1 >= -1e-4) & (l2 >= -1e-4) & (l3 >= -1e-4)
            if not m.any():
                continue
            Z = l1 * a[2] + l2 * b_[2] + l3 * c_[2]
            fu = l1 * uvs[0][0] + l2 * uvs[1][0] + l3 * uvs[2][0]
            fv = l1 * uvs[0][1] + l2 * uvs[1][1] + l3 * uvs[2][1]
            tx = np.clip((u + fu * w).astype(int), 0, tex.shape[1] - 1)
            ty = np.clip((v + fv * h).astype(int), 0, tex.shape[0] - 1)
            col = tex[ty, tx]
            m &= col[..., 3] > 10
            ys, xs = np.nonzero(m)
            gy, gx = ys + ymin, xs + xmin
            zz = Z[ys, xs]
            closer = zz < zb[gy, gx]
            gy, gx, zz = gy[closer], gx[closer], zz[closer]
            cc = col[ys[closer], xs[closer], :3] * shade
            if glow is not None:
                gc = glow[ty[ys[closer], xs[closer]], tx[ys[closer], xs[closer]]]
                a_ = gc[:, 3:4] / 255.0
                cc = cc * (1 - a_) + gc[:, :3] * a_
            zb[gy, gx] = zz
            img[gy, gx] = cc
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
