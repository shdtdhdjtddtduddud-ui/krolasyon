"""Bedrock / GeckoLib geometry toolkit.

Space: y up, feet at y=0, the model faces north (-z), its right side is -x (bedrock convention).
Effective rotation for bone / cube euler (x, y, z) degrees: R = Rz(-z) . Ry(y) . Rx(-x)
  -> +x swings a hanging limb backward and leans an upright torso forward, +z moves a right limb outward.
Per-face UV atlas at D texels per unit, procedural painters, geo/animation JSON export and a software preview.
"""
import json, math, numpy as np
from PIL import Image

D = 4  # texels per model unit


def Rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def Ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def Rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def R_eff(r):
    x, y, z = np.radians(r)
    return Rz(-z) @ Ry(y) @ Rx(-x)


def euler_for_dir(d):
    """cube/bone euler so that local -y points along unit vector d (y=0 component)"""
    d = np.asarray(d, float); d = d / np.linalg.norm(d)
    a = math.asin(max(-1, min(1, -d[2])))
    c = math.atan2(d[0], -d[1])
    return (-math.degrees(a), 0.0, -math.degrees(c))


class Cube:
    def __init__(self, origin, size, mat, pivot=None, rot=(0, 0, 0), inflate=0.0, seed=0, tag=None):
        self.origin = np.array(origin, float); self.size = np.array(size, float)
        self.mat, self.inflate, self.seed, self.tag = mat, inflate, seed, tag or {}
        self.pivot = np.array(pivot if pivot is not None else self.origin + self.size / 2, float)
        self.rot = tuple(float(v) for v in rot)
        self.uv = {}


class Bone:
    def __init__(self, name, parent, pivot, rot):
        self.name, self.parent = name, parent
        self.pivot = np.array(pivot, float)
        self.rot = tuple(float(v) for v in rot)
        self.cubes = []


FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def face_dims(c):
    w, h, d = c.size
    return {'north': (w, h), 'south': (w, h), 'east': (d, h), 'west': (d, h), 'up': (w, d), 'down': (w, d)}


class Geo:
    def __init__(self, name, tw, th):
        self.name, self.tw, self.th = name, tw, th
        self.bones = {}
        self.order = []
        self._seed = 11

    def bone(self, name, parent, pivot, rot=(0, 0, 0)):
        b = Bone(name, parent, pivot, rot)
        self.bones[name] = b; self.order.append(name)
        return b

    def box(self, bone, origin, size, mat, pivot=None, rot=(0, 0, 0), inflate=0.0, **tag):
        self._seed += 1
        c = Cube(origin, size, mat, pivot, rot, inflate, self._seed, tag)
        self.bones[bone].cubes.append(c)
        return c

    def rod(self, bone, A, B, w, d, mat, ext=0.0, roll=0.0, **tag):
        """box spanning segment A->B (cross section w x d), rotated about A"""
        A = np.array(A, float); B = np.array(B, float)
        L = np.linalg.norm(B - A)
        rx, ry, rz = euler_for_dir(B - A)
        o = (A[0] - w / 2, A[1] - L - ext, A[2] - d / 2)
        return self.box(bone, o, (w, L + 2 * ext, d), mat, pivot=A, rot=(rx, ry + roll, rz), **tag)

    def vrod(self, bone, top, length, w, d, mat, **tag):
        """vertical box hanging down from top-centre point"""
        return self.box(bone, (top[0] - w / 2, top[1] - length, top[2] - d / 2), (w, length, d), mat, **tag)

    # ---------------- atlas ----------------
    def pack(self):
        rects = []
        for bn in self.order:
            for c in self.bones[bn].cubes:
                for f, (a, b) in face_dims(c).items():
                    W, H = max(1, math.ceil(a * D - 1e-6)), max(1, math.ceil(b * D - 1e-6))
                    rects.append((H, W, c, f))
        rects.sort(key=lambda r: (-r[0], -r[1]))
        x = y = rowh = 0
        for H, W, c, f in rects:
            if x + W > self.tw:
                x, y, rowh = 0, y + rowh, 0
            c.uv[f] = (x, y, W, H)
            x += W; rowh = max(rowh, H)
        if y + rowh > self.th:
            raise RuntimeError(f'atlas overflow: need {y + rowh} rows of {self.th}')
        return y + rowh

    def paint(self, painters):
        img = np.zeros((self.th, self.tw, 4), np.float32)
        for bn in self.order:
            for c in self.bones[bn].cubes:
                for f in FACES:
                    u, v, W, H = c.uv[f]
                    rng = np.random.default_rng(c.seed * 31 + FACES.index(f))
                    rgba = painters[c.mat](c, f, W, H, rng)
                    img[v:v + H, u:u + W] = rgba
        return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')

    # ---------------- export ----------------
    def geo_json(self, ident):
        bones = []
        for bn in self.order:
            b = self.bones[bn]
            jb = {'name': b.name, 'pivot': r3(b.pivot)}
            if b.parent:
                jb['parent'] = b.parent
            if any(abs(v) > 1e-6 for v in b.rot):
                jb['rotation'] = r3(b.rot)
            cubes = []
            for c in b.cubes:
                jc = {'origin': r3(c.origin), 'size': r3(c.size)}
                if any(abs(v) > 1e-6 for v in c.rot):
                    jc['pivot'] = r3(c.pivot); jc['rotation'] = r3(c.rot)
                if c.inflate:
                    jc['inflate'] = round(c.inflate, 4)
                jc['uv'] = {f: {'uv': [c.uv[f][0], c.uv[f][1]], 'uv_size': [c.uv[f][2], c.uv[f][3]]} for f in FACES}
                cubes.append(jc)
            if cubes:
                jb['cubes'] = cubes
            bones.append(jb)
        return {'format_version': '1.12.0', 'minecraft:geometry': [{
            'description': {'identifier': ident, 'texture_width': self.tw, 'texture_height': self.th,
                            'visible_bounds_width': 6, 'visible_bounds_height': 6, 'visible_bounds_offset': [0, 2.5, 0]},
            'bones': bones}]}

    # ---------------- preview ----------------
    def world_mats(self, pose=None):
        pose = pose or {}
        mats = {}
        for bn in self.order:
            b = self.bones[bn]
            p = pose.get(bn, {})
            rot = np.array(b.rot) + np.array(p.get('rotation', (0, 0, 0)))
            off = np.array(p.get('position', (0, 0, 0)), float)
            sc = np.array(p.get('scale', (1, 1, 1)), float)
            M = np.eye(4)
            T1 = np.eye(4); T1[:3, 3] = b.pivot + off
            Rm = np.eye(4); Rm[:3, :3] = R_eff(rot) @ np.diag(sc)
            T2 = np.eye(4); T2[:3, 3] = -b.pivot
            M = T1 @ Rm @ T2
            mats[bn] = (mats[b.parent] if b.parent else np.eye(4)) @ M
        return mats

    def quads(self, pose=None):
        mats = self.world_mats(pose)
        out = []
        for bn in self.order:
            M = mats[bn]
            for c in self.bones[bn].cubes:
                x0, y0, z0 = c.origin - c.inflate
                x1, y1, z1 = c.origin + c.size + c.inflate
                C = np.eye(4)
                if any(c.rot):
                    T1 = np.eye(4); T1[:3, 3] = c.pivot
                    Rm = np.eye(4); Rm[:3, :3] = R_eff(c.rot)
                    T2 = np.eye(4); T2[:3, 3] = -c.pivot
                    C = T1 @ Rm @ T2
                MC = M @ C
                # corners ordered (u0v0, u1v0, u1v1, u0v1) as seen from outside, v0 = top
                F = {
                    'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
                    'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
                    'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
                    'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
                    'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
                    'down': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
                }
                for f, pts in F.items():
                    P = np.array([(MC @ np.array([*p, 1.0]))[:3] for p in pts])
                    out.append((P, c.uv[f]))
        return out


def r3(v):
    return [round(float(x), 4) for x in v]


def render(geo, tex, pose=None, yaw=0.0, pitch=8.0, size=512, bg=(200, 195, 180), view=None, light=(0.35, 0.75, -0.55), fov_fit=True):
    """orthographic preview. yaw 0 = looking at the model's front."""
    tex = np.asarray(tex.convert('RGBA')).astype(np.float32)
    qs = geo.quads(pose)
    ya, pa = math.radians(yaw), math.radians(pitch)
    # camera space: x right, y up, z depth (into screen). Front view: camera at -z looking +z, right = -x
    V = Rx(-pa) @ Ry(-ya)
    flip = np.diag([-1.0, 1.0, 1.0])
    allp = np.concatenate([(flip @ V @ q[0].T).T for q in qs])
    if view is None:
        mn, mx = allp.min(0), allp.max(0)
        sc = (size - 30) / max(mx[0] - mn[0], mx[1] - mn[1])
        cx, cy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
    else:
        sc, cx, cy = view
    img = np.zeros((size, size, 3), np.float32); img[:] = bg
    zb = np.full((size, size), 1e9, np.float32)
    L = np.array(light, float); L /= np.linalg.norm(L)
    for P, (u, v, W, H) in qs:
        n = np.cross(P[1] - P[0], P[3] - P[0]); nn = np.linalg.norm(n)
        shade = 0.5 + 0.5 * max(0.0, float(np.dot(n / nn, L))) if nn > 1e-9 else 0.8
        Q = (flip @ V @ P.T).T
        S = np.stack([(Q[:, 0] - cx) * sc + size / 2, -(Q[:, 1] - cy) * sc + size / 2, Q[:, 2]], 1)
        for tri, uvs in (((0, 1, 2), ((0, 0), (1, 0), (1, 1))), ((0, 2, 3), ((0, 0), (1, 1), (0, 1)))):
            a, b, c = S[tri[0]], S[tri[1]], S[tri[2]]
            xmin, xmax = int(max(0, min(a[0], b[0], c[0]))), int(min(size - 1, max(a[0], b[0], c[0]) + 1))
            ymin, ymax = int(max(0, min(a[1], b[1], c[1]))), int(min(size - 1, max(a[1], b[1], c[1]) + 1))
            if xmax < xmin or ymax < ymin:
                continue
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            X, Y = np.meshgrid(np.arange(xmin, xmax + 1) + 0.5, np.arange(ymin, ymax + 1) + 0.5)
            l1 = ((b[1] - c[1]) * (X - c[0]) + (c[0] - b[0]) * (Y - c[1])) / den
            l2 = ((c[1] - a[1]) * (X - c[0]) + (a[0] - c[0]) * (Y - c[1])) / den
            l3 = 1 - l1 - l2
            m = (l1 >= -1e-4) & (l2 >= -1e-4) & (l3 >= -1e-4)
            if not m.any():
                continue
            Z = l1 * a[2] + l2 * b[2] + l3 * c[2]
            fu = l1 * uvs[0][0] + l2 * uvs[1][0] + l3 * uvs[2][0]
            fv = l1 * uvs[0][1] + l2 * uvs[1][1] + l3 * uvs[2][1]
            tx = np.clip((u + fu * W).astype(int), u, u + W - 1)
            ty = np.clip((v + fv * H).astype(int), v, v + H - 1)
            col = tex[ty, tx]
            m &= col[..., 3] > 10
            ys, xs = np.nonzero(m)
            gy, gx = ys + ymin, xs + xmin
            zz = Z[ys, xs]
            closer = zz < zb[gy, gx]
            zb[gy[closer], gx[closer]] = zz[closer]
            img[gy[closer], gx[closer]] = col[ys[closer], xs[closer], :3] * shade
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)), (sc, cx, cy)


# ---------------- animations ----------------
class Anim:
    def __init__(self, name, length, loop=False):
        self.name, self.length, self.loop = name, length, loop
        self.ch = {}  # bone -> {'rotation': [(t, vec, lerp)], 'position': ...}

    def key(self, bone, kind, t, v, interp='catmullrom'):
        self.ch.setdefault(bone, {}).setdefault(kind, []).append((round(t, 4), [float(x) for x in v], interp))
        return self

    def json(self):
        bones = {}
        for b, kinds in self.ch.items():
            jb = {}
            for kind, keys in kinds.items():
                keys = sorted(keys, key=lambda k: k[0])
                jb[kind] = {f'{t:.4f}'.rstrip('0').rstrip('.') if t else '0.0':
                            ({'post': r3(v), 'lerp_mode': 'catmullrom'} if i == 'catmullrom' else r3(v)) for t, v, i in keys}
            bones[b] = jb
        j = {'animation_length': self.length, 'bones': bones}
        if self.loop:
            j['loop'] = True
        return j

    def sample(self, t):
        res = {}
        for b, kinds in self.ch.items():
            for kind, keys in kinds.items():
                keys = sorted(keys, key=lambda k: k[0])
                ts = [k[0] for k in keys]
                if t <= ts[0]:
                    v = keys[0][1]
                elif t >= ts[-1]:
                    v = keys[-1][1]
                else:
                    i = max(j for j in range(len(ts)) if ts[j] <= t)
                    a, c = keys[i], keys[i + 1]
                    u = (t - a[0]) / (c[0] - a[0])
                    if c[2] == 'catmullrom':
                        p0 = keys[i - 1][1] if i > 0 else a[1]
                        p3 = keys[i + 2][1] if i + 2 < len(keys) else c[1]
                        v = [catmull(p0[k], a[1][k], c[1][k], p3[k], u) for k in range(3)]
                    else:
                        v = [a[1][k] + (c[1][k] - a[1][k]) * u for k in range(3)]
                res.setdefault(b, {})[kind] = v
        return res


def catmull(p0, p1, p2, p3, t):
    return 0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3)


def anim_file(anims, prefix):
    return {'format_version': '1.8.0', 'animations': {f'{prefix}.{a.name}': a.json() for a in anims}}
