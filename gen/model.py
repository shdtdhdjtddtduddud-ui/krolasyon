"""Box-model builder: author models in y-up block-pixel coordinates, pack box UVs, paint textures, emit Java."""
import numpy as np
import tex as T

FACES = ('top', 'bottom', 'right', 'front', 'left', 'back')


class Cube:
    def __init__(self, bone, x, y, z, w, h, d, mat, inflate=0.0, mirror=False, decals=None, glow=False, seed=0):
        self.bone, self.x, self.y, self.z = bone, x, y, z
        self.w, self.h, self.d = int(w), int(h), int(d)
        self.mat, self.inflate, self.mirror = mat, inflate, mirror
        self.decals = decals or {}
        self.glow = glow
        self.seed = seed
        self.u = self.v = 0

    def uv_size(self):
        return 2 * (self.w + self.d), self.d + self.h


class Bone:
    def __init__(self, name, parent, pivot, rot):
        self.name, self.parent, self.pivot, self.rot = name, parent, pivot, rot
        self.cubes = []
        self.children = []


class Model:
    def __init__(self, name, tex_w=128, res=4):
        self.name = name
        self.tex_w = tex_w
        self.res = res
        self.bones = {}
        self.order = []
        self.cubes = []
        self.seed = abs(hash(name)) % 100000

    def bone(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0)):
        b = Bone(name, self.bones[parent] if parent else None, pivot, rot)
        if parent:
            self.bones[parent].children.append(b)
        self.bones[name] = b
        self.order.append(b)
        return name

    def box(self, bone, x, y, z, w, h, d, mat, inflate=0.0, mirror=False, decals=None, glow=False):
        w, h, d = max(1, int(round(w))), max(1, int(round(h))), max(1, int(round(d)))
        c = Cube(self.bones[bone], x, y, z, w, h, d, mat, inflate, mirror, decals, glow, self.seed + len(self.cubes) * 7919)
        self.bones[bone].cubes.append(c)
        self.cubes.append(c)
        return c

    def cbox(self, bone, cx, y, cz, w, h, d, mat, **kw):
        """Box centred on x/z."""
        w, h, d = max(1, int(round(w))), max(1, int(round(h))), max(1, int(round(d)))
        return self.box(bone, cx - w / 2, y, cz - d / 2, w, h, d, mat, **kw)

    # ------------------------------------------------------------ packing
    def pack(self):
        W = self.tex_w
        items = sorted(self.cubes, key=lambda c: -c.uv_size()[1])
        x = y = shelf = 0
        for c in items:
            uw, uh = c.uv_size()
            if uw > W:
                raise ValueError(f'{self.name}: cube too wide for texture ({uw} > {W})')
            if x + uw > W:
                x = 0
                y += shelf
                shelf = 0
            c.u, c.v = x, y
            x += uw
            shelf = max(shelf, uh)
        H = y + shelf
        h = 16
        while h < H:
            h *= 2
        self.tex_h = h

    # ------------------------------------------------------------ painting
    def paint(self):
        r = self.res
        img = T.new(self.tex_w * r, self.tex_h * r)
        glow = T.new(self.tex_w * r, self.tex_h * r)
        for c in self.cubes:
            u, v, w, h, d = c.u, c.v, c.w, c.h, c.d
            rects = {
                'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
                'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
                'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
            }
            for i, face in enumerate(FACES):
                fx, fy, fw, fh = rects[face]
                if fw <= 0 or fh <= 0:
                    continue
                pw, ph = fw * r, fh * r
                face_img = c.mat(pw, ph, c.seed + i * 31, face)
                face_glow = T.new(pw, ph)
                for dec in c.decals.get(face, []):
                    dec(face_img, face_glow)
                for dec in c.decals.get('all', []):
                    dec(face_img, face_glow)
                face_img = T.edge_dark(face_img, 0.22, max(1, r // 4))
                T.put(img, face_img, fx * r, fy * r)
                if c.glow:
                    T.put(glow, face_img, fx * r, fy * r)
                else:
                    T.paste(glow, face_glow, fx * r, fy * r)
        return img, glow

    # ------------------------------------------------------------ java
    def java(self):
        out = []
        out.append('        MeshDefinition mesh = new MeshDefinition();')
        out.append('        PartDefinition root = mesh.getRoot();')
        names = {}
        for b in self.order:
            var = 'p_' + b.name
            names[b.name] = var
            parent = names[b.parent.name] if b.parent else 'root'
            if b.parent:
                px = b.pivot[0] - b.parent.pivot[0]
                py = -(b.pivot[1] - b.parent.pivot[1])
                pz = b.pivot[2] - b.parent.pivot[2]
            else:
                px, py, pz = b.pivot[0], 24 - b.pivot[1], b.pivot[2]
            cl = 'CubeListBuilder.create()'
            for c in b.cubes:
                ox = c.x - b.pivot[0]
                oy = b.pivot[1] - c.y - c.h
                oz = c.z - b.pivot[2]
                cl += f'.texOffs({c.u}, {c.v})'
                if c.mirror:
                    cl += '.mirror()'
                cl += f'.addBox({f(ox)}, {f(oy)}, {f(oz)}, {c.w}F, {c.h}F, {c.d}F, new CubeDeformation({f(c.inflate)}))'
                if c.mirror:
                    cl += '.mirror(false)'
            rx, ry, rz = [x * np.pi / 180 for x in b.rot]
            out.append(f'        PartDefinition {var} = {parent}.addOrReplaceChild("{b.name}", {cl}, '
                       f'PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)}));')
        out.append(f'        return LayerDefinition.create(mesh, {self.tex_w}, {self.tex_h});')
        return '\n'.join(out)

    def paths(self):
        res = []
        for b in self.order:
            p, n = [], b
            while n:
                p.append(n.name)
                n = n.parent
            res.append('/'.join(reversed(p)))
        return res


def f(x):
    s = f'{float(x):.4f}'.rstrip('0').rstrip('.')
    if s in ('-0', ''):
        s = '0'
    return s + 'F'
