"""Block/item JSON element-model toolkit.

Coordinates are Minecraft block-model space (x east, y up, z south, 0..16 = one block).
Every element face gets its own rect in an auto-packed texture atlas and is painted procedurally by a
material function. Includes an exact-UV software renderer so models can be previewed as PNGs.
"""
import math, json
import numpy as np
from PIL import Image

FACES = ['north', 'east', 'south', 'west', 'up', 'down']
# vertex order exactly as FaceBakery/FaceInfo (minX..maxX etc.)
def _verts(face, a, b):
    x0, y0, z0 = a; x1, y1, z1 = b
    return {
        'down': [(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)],
        'up': [(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)],
        'north': [(x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)],
        'south': [(x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)],
        'west': [(x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)],
        'east': [(x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)],
    }[face]

SHADE = {'up': 1.0, 'north': 0.86, 'south': 0.86, 'east': 0.72, 'west': 0.72, 'down': 0.55}


class El:
    def __init__(self, a, b, mat, rot=None, faces=None, seed=0, tag=None):
        self.a = [float(v) for v in a]
        self.b = [float(v) for v in b]
        self.mat, self.rot, self.seed, self.tag = mat, rot, seed, tag
        self.faces = faces or FACES
        self.rect = {}


class BM:
    def __init__(self, name, size=128, ppu=2.0):
        self.name, self.size, self.ppu = name, size, ppu
        self.els = []
        self.mats = {}
        self.display = None
        self.fixed_tex = None  # use a premade PIL image instead of procedural painting

    def box(self, a, b, mat, rot=None, faces=None, tag=None):
        e = El(a, b, mat, rot, faces, seed=len(self.els) + 1, tag=tag)
        self.els.append(e)
        return e

    def rot_box(self, a, b, mat, axis, angle, origin, **kw):
        return self.box(a, b, mat, rot=(axis, angle, origin), **kw)

    # ---- packing ----
    def _dims(self, e, face):
        dx, dy, dz = [abs(e.b[i] - e.a[i]) for i in range(3)]
        w, h = {'north': (dx, dy), 'south': (dx, dy), 'east': (dz, dy), 'west': (dz, dy),
                'up': (dx, dz), 'down': (dx, dz)}[face]
        return max(1, int(round(w * self.ppu))), max(1, int(round(h * self.ppu)))

    def pack(self):
        items = []
        for e in self.els:
            for f in e.faces:
                w, h = self._dims(e, f)
                if (e.b[0] - e.a[0] == 0 and f in ('east', 'west', 'up', 'down')) or \
                   (e.b[1] - e.a[1] == 0 and f in ('up', 'down', 'north', 'south', 'east', 'west') and f in ('up', 'down')) or \
                   (e.b[2] - e.a[2] == 0 and f in ('east', 'west', 'up', 'down')):
                    pass
                items.append((e, f, w, h))
        items.sort(key=lambda t: -t[3])
        x = y = rowh = 0
        S = self.size
        for e, f, w, h in items:
            if x + w + 1 > S:
                x, y, rowh = 0, y + rowh + 1, 0
            if y + h > S:
                raise RuntimeError(f'{self.name}: atlas {S} too small')
            e.rect[f] = (x, y, w, h)
            x += w + 1
            rowh = max(rowh, h)

    # ---- painting ----
    def paint(self, mats):
        """mats: name -> fn(face, W, H, rng, el) -> (H,W,4) float RGBA 0..255"""
        S = self.size
        tex = np.zeros((S, S, 4), np.float32)
        self.ylo = min(e.a[1] for e in self.els); self.yhi = max(e.b[1] for e in self.els)
        for e in self.els:
            e.bm = self
            fn = mats[e.mat]
            for f, (x, y, w, h) in e.rect.items():
                rng = np.random.default_rng(e.seed * 131 + FACES.index(f) * 7 + 3)
                tex[y:y + h, x:x + w] = np.clip(fn(f, w, h, rng, e), 0, 255)
        self.tex = Image.fromarray(tex.astype(np.uint8), 'RGBA')
        return self.tex

    # ---- export ----
    def to_json(self, textures_key='0', tex_path=None, display=None):
        S = self.size
        els = []
        for e in self.els:
            d = {'from': [round(v, 4) for v in e.a], 'to': [round(v, 4) for v in e.b], 'faces': {}}
            if e.rot:
                axis, ang, org = e.rot
                d['rotation'] = {'angle': ang, 'axis': axis, 'origin': [round(v, 4) for v in org]}
            for f, (x, y, w, h) in e.rect.items():
                d['faces'][f] = {'uv': [round(x / S * 16, 4), round(y / S * 16, 4), round((x + w) / S * 16, 4), round((y + h) / S * 16, 4)],
                                 'texture': '#' + textures_key}
            els.append(d)
        out = {'textures': {textures_key: tex_path, 'particle': tex_path}, 'elements': els}
        if display:
            out['display'] = display
        return out

    # ---- preview ----
    def render(self, yaw=-35, pitch=20, roll=0, size=512, bg=(58, 62, 70), center=(8, 8, 8), zoom=1.0):
        tex = np.asarray(self.tex).astype(np.float32)
        S = self.size
        ya, pa, ra = math.radians(yaw), math.radians(pitch), math.radians(roll)
        Ry = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
        Rx = np.array([[1, 0, 0], [0, math.cos(pa), -math.sin(pa)], [0, math.sin(pa), math.cos(pa)]])
        Rz = np.array([[math.cos(ra), -math.sin(ra), 0], [math.sin(ra), math.cos(ra), 0], [0, 0, 1]])
        V = Rz @ Rx @ Ry
        c = np.array(center, float)
        quads = []
        for e in self.els:
            R = np.eye(3)
            org = np.zeros(3)
            if e.rot:
                axis, ang, org = e.rot
                org = np.array(org, float)
                t = math.radians(ang)
                if axis == 'x': R = np.array([[1, 0, 0], [0, math.cos(t), -math.sin(t)], [0, math.sin(t), math.cos(t)]])
                if axis == 'y': R = np.array([[math.cos(t), 0, math.sin(t)], [0, 1, 0], [-math.sin(t), 0, math.cos(t)]])
                if axis == 'z': R = np.array([[math.cos(t), -math.sin(t), 0], [math.sin(t), math.cos(t), 0], [0, 0, 1]])
            for f, (x, y, w, h) in e.rect.items():
                vs = _verts(f, e.a, e.b)
                P = []
                for v in vs:
                    q = np.array(v, float)
                    if e.rot:
                        q = R @ (q - org) + org
                    q = V @ (q - c)
                    P.append(q)
                u0, v0, u1, v1 = x, y, x + w, y + h
                quads.append((np.array(P), (u0, v0, u1, v1), SHADE[f]))
        allp = np.concatenate([q[0] for q in quads])
        ext = max(np.ptp(allp[:, 0]), np.ptp(allp[:, 1]), 1e-6)
        scale = (size - 30) / ext * zoom
        cx = (allp[:, 0].min() + allp[:, 0].max()) / 2
        cy = (allp[:, 1].min() + allp[:, 1].max()) / 2
        img = np.zeros((size, size, 3), np.float32); img[:] = bg
        zb = np.full((size, size), -1e9, np.float32)
        uvs_v = [(0, 0), (0, 1), (1, 1), (1, 0)]  # vertex -> (ufrac idx, vfrac idx)  v0:(u0,v0) v1:(u0,v1) v2:(u1,v1) v3:(u1,v0)
        for P, (u0, v0, u1, v1), sh in quads:
            Sx = (P[:, 0] - cx) * scale + size / 2
            Sy = -(P[:, 1] - cy) * scale + size / 2
            Z = P[:, 2]
            n = np.cross(P[1] - P[0], P[3] - P[0])
            # camera looks along -z (we look from +z): visible if normal z>0 for CCW verts; draw both sides, shade by light
            for tri in ((0, 1, 2), (0, 2, 3)):
                a, b, cc = tri
                xs = [Sx[a], Sx[b], Sx[cc]]; ys = [Sy[a], Sy[b], Sy[cc]]
                xmin, xmax = int(max(0, min(xs))), int(min(size - 1, max(xs) + 1))
                ymin, ymax = int(max(0, min(ys))), int(min(size - 1, max(ys) + 1))
                if xmax < xmin or ymax < ymin:
                    continue
                den = (ys[1] - ys[2]) * (xs[0] - xs[2]) + (xs[2] - xs[1]) * (ys[0] - ys[2])
                if abs(den) < 1e-9:
                    continue
                X, Y = np.meshgrid(np.arange(xmin, xmax + 1) + 0.5, np.arange(ymin, ymax + 1) + 0.5)
                l1 = ((ys[1] - ys[2]) * (X - xs[2]) + (xs[2] - xs[1]) * (Y - ys[2])) / den
                l2 = ((ys[2] - ys[0]) * (X - xs[2]) + (xs[0] - xs[2]) * (Y - ys[2])) / den
                l3 = 1 - l1 - l2
                m = (l1 >= -1e-4) & (l2 >= -1e-4) & (l3 >= -1e-4)
                if not m.any():
                    continue
                zz = l1 * Z[a] + l2 * Z[b] + l3 * Z[cc]
                fu = l1 * uvs_v[a][0] + l2 * uvs_v[b][0] + l3 * uvs_v[cc][0]
                fv = l1 * uvs_v[a][1] + l2 * uvs_v[b][1] + l3 * uvs_v[cc][1]
                tx = np.clip((u0 + fu * (u1 - u0)).astype(int), 0, S - 1)
                ty = np.clip((v0 + fv * (v1 - v0)).astype(int), 0, S - 1)
                col = tex[ty, tx]
                m &= col[..., 3] > 12
                yy, xx = np.nonzero(m)
                gy, gx = yy + ymin, xx + xmin
                z = zz[yy, xx]
                ok = z > zb[gy, gx]
                gy, gx, z = gy[ok], gx[ok], z[ok]
                cl = col[yy[ok], xx[ok], :3]
                al = col[yy[ok], xx[ok], 3:4] / 255.0
                img[gy, gx] = img[gy, gx] * (1 - al) + cl * al
                opq = (al[:, 0] > 0.97)
                zb[gy[opq], gx[opq]] = z[opq]
        return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


# ---------------------------------------------------------------- materials
def _q(c, levels):
    return np.round(c / 255.0 * levels) / levels * 255.0


def pal(c):
    return np.array(c, np.float32)


def lerpc(a, b, t):
    t = np.asarray(t, np.float32)[..., None]
    return pal(a) * (1 - t) + pal(b) * t


def grid(W, H):
    yy, xx = np.mgrid[0:H, 0:W]
    return xx.astype(np.float32), yy.astype(np.float32)


def edge_mask(W, H, k=1):
    xx, yy = grid(W, H)
    return (xx < k) | (yy < k) | (xx >= W - k) | (yy >= H - k)


def noise(W, H, rng, scale=2.0):
    g = rng.random((H // max(1, int(scale)) + 2, W // max(1, int(scale)) + 2))
    return np.kron(g, np.ones((int(max(1, scale)), int(max(1, scale)))))[:H, :W]


def shade_rgba(rgb, face, alpha=255.0):
    H, W = rgb.shape[:2]
    out = np.zeros((H, W, 4), np.float32)
    out[..., :3] = rgb * SHADE[face]
    out[..., 3] = alpha
    return out


def world_y(el, H):
    """per-row world y for side faces (row 0 = top)"""
    return el.b[1] - (np.arange(H, dtype=np.float32) + 0.5) / H * (el.b[1] - el.a[1])


def metal(lo, hi, edge=None, grain=0.06, bands=5, vertical=True, fuller=None, global_grad=True):
    """brushed metal; lo..hi along world y (hi = top) so segmented blades shade continuously."""
    edge = edge or tuple(min(255, int(v * 1.35 + 18)) for v in hi)
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        if global_grad and face not in ('up', 'down'):
            wy = world_y(el, H)
            t1 = np.clip((wy - el.bm.ylo) / max(1e-6, el.bm.yhi - el.bm.ylo), 0, 1)
            t = np.repeat(t1[:, None], W, 1)
        else:
            t = 1 - yy / max(1, H - 1)
        t = t * 0.9 + noise(W, H, rng, 2) * 0.1
        rgb = lerpc(lo, hi, t)
        if face in ('north', 'south'):
            cxw = 8.0
            wx = el.a[0] + (np.arange(W, dtype=np.float32) + 0.5) / W * (el.b[0] - el.a[0])
            ridge = 1 - np.clip(np.abs(wx - cxw) / 2.6, 0, 1)
            rgb = rgb * (0.72 + 0.4 * ridge[None, :, None])
        rgb = rgb * (1 + (rng.random((H, W, 1)) - 0.5) * grain * 2)
        em = edge_mask(W, H, 1)
        if W > 3 and H > 3:
            rgb[em] = lerpc(rgb[em], edge, 0.35)
        return shade_rgba(_q(rgb, bands * 4), face)
    return fn


def solid(color, var=0.05, bands=6, edge_dark=0.0, alpha=255.0, glow=False):
    def fn(face, W, H, rng, el):
        rgb = np.zeros((H, W, 3), np.float32) + pal(color)
        rgb = rgb * (1 + (rng.random((H, W, 1)) - 0.5) * var * 2)
        if edge_dark:
            em = edge_mask(W, H, 1)
            rgb[em] *= (1 - edge_dark)
        out = shade_rgba(_q(rgb, bands * 4), face, alpha)
        if glow:
            out[..., :3] = rgb
        return out
    return fn


def gem(core, rim, glow=True):
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        d = np.hypot((xx - (W - 1) / 2) / max(1, W / 2), (yy - (H - 1) / 2) / max(1, H / 2))
        rgb = lerpc((255, 255, 255), core, np.clip(d * 1.6, 0, 1))
        rgb = lerpc(rgb, rim, np.clip((d - 0.55) * 2, 0, 1))
        out = np.zeros((H, W, 4), np.float32)
        out[..., :3] = rgb * (1.0 if glow else SHADE[face])
        out[..., 3] = 255
        return out
    return fn


def wrap(c1, c2, stripe=2, bands=4, vertical=False):
    """leather wrap / cord stripes"""
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        s = (xx if vertical else yy) + (yy * 0.0 if vertical else xx * 0.5)
        m = ((s // stripe) % 2).astype(np.float32)
        rgb = lerpc(c1, c2, m)
        rgb = rgb * (1 + (rng.random((H, W, 1)) - 0.5) * 0.12)
        return shade_rgba(_q(rgb, bands * 4), face)
    return fn


def crystal(base, hi, alpha=225.0, glow=0.0):
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        t = np.clip((xx / max(1, W - 1) * 0.5 + (1 - yy / max(1, H - 1)) * 0.5), 0, 1)
        t = np.clip(t + (noise(W, H, rng, 2) - 0.5) * 0.5, 0, 1)
        rgb = lerpc(base, hi, t)
        out = shade_rgba(rgb, face, alpha)
        out[..., :3] = out[..., :3] * (1 - glow) + rgb * glow
        return out
    return fn


def glass(tint=(200, 230, 255), alpha=110.0):
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        rgb = np.zeros((H, W, 3), np.float32) + pal(tint)
        hl = (xx < max(1, W * 0.22)) & (yy > H * 0.1) & (yy < H * 0.9)
        rgb[hl] = 255
        a = np.full((H, W), alpha, np.float32)
        a[hl] = alpha + 90
        em = edge_mask(W, H, 1)
        a[em] = alpha + 60
        out = np.zeros((H, W, 4), np.float32)
        out[..., :3] = rgb * (0.7 + 0.3 * SHADE[face])
        out[..., 3] = a
        return out
    return fn


def liquid(top, bottom, glowing=True, alpha=235.0):
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        t = yy / max(1, H - 1)
        rgb = lerpc(top, bottom, t)
        bub = (rng.random((H, W)) > 0.93)
        rgb[bub] = lerpc(rgb[bub], (255, 255, 255), 0.5)
        out = np.zeros((H, W, 4), np.float32)
        out[..., :3] = rgb if glowing else rgb * SHADE[face]
        out[..., 3] = alpha
        return out
    return fn


def energy(c1, c2):
    """glowing runic/energy streaks along the vertical axis"""
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        t = np.clip(noise(W, H, rng, 2) * 0.7 + (1 - yy / max(1, H - 1)) * 0.3, 0, 1)
        rgb = lerpc(c1, c2, t)
        out = np.zeros((H, W, 4), np.float32)
        out[..., :3] = rgb
        out[..., 3] = 255
        return out
    return fn


def wood(c1, c2):
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        t = (np.sin(xx * 1.7 + noise(W, H, rng, 3) * 4) * 0.5 + 0.5)
        rgb = lerpc(c1, c2, t)
        return shade_rgba(_q(rgb, 20), face)
    return fn


def write_item_model(path_json, bm, tex_loc, display):
    j = bm.to_json(tex_path=tex_loc, display=display)
    with open(path_json, 'w') as f:
        json.dump(j, f, indent=1)


# ------------------------------------------------------------ display presets
DISPLAY_SWORD = {
    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 3.5, 0.5], 'scale': [0.75, 0.75, 0.75]},
    'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 3.5, 0.5], 'scale': [0.75, 0.75, 0.75]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'gui': {'rotation': [20, -40, 38], 'translation': [0, 0, 0], 'scale': [0.62, 0.62, 0.62]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 90, 35], 'translation': [0, 0, 0], 'scale': [0.7, 0.7, 0.7]},
}
DISPLAY_ITEM = {
    'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 0], 'scale': [0.55, 0.55, 0.55]},
    'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 0], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_righthand': {'rotation': [0, -20, 0], 'translation': [1, 3, 1], 'scale': [0.6, 0.6, 0.6]},
    'firstperson_lefthand': {'rotation': [0, 20, 0], 'translation': [1, 3, 1], 'scale': [0.6, 0.6, 0.6]},
    'gui': {'rotation': [25, -35, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]},
}
