"""JSON element models for items, with packed per-face textures and an icon renderer."""
import math
import json
import numpy as np
from PIL import Image
import tex as T

FACE_DIMS = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}


class El:
    def __init__(self, frm, to, mat, glow=False, translucent=False, seed=0, faces=None, decals=None):
        self.frm = list(frm)
        self.to = list(to)
        self.mat = mat
        self.glow = glow
        self.translucent = translucent
        self.seed = seed
        self.faces = faces or ['north', 'south', 'east', 'west', 'up', 'down']
        self.decals = decals or {}
        self.rects = {}
        self.rot = None


class ItemModel:
    def __init__(self, name, density=4):
        self.name = name
        self.density = density
        self.els = []
        self.global_rot = None  # (axis, angle, origin)

    def el(self, frm, to, mat, **kw):
        e = El(frm, to, mat, seed=hash(self.name) % 10000 + len(self.els) * 131, **kw)
        self.els.append(e)
        return e

    def cel(self, cx, y0, cz, w, h, d, mat, **kw):
        """Element centred on x/z."""
        return self.el((cx - w / 2, y0, cz - d / 2), (cx + w / 2, y0 + h, cz + d / 2), mat, **kw)

    def pack_and_paint(self):
        rects = []
        for e in self.els:
            size = [e.to[i] - e.frm[i] for i in range(3)]
            for f in e.faces:
                a, b = FACE_DIMS[f]
                pw = max(1, int(round(size[a] * self.density)))
                ph = max(1, int(round(size[b] * self.density)))
                rects.append((ph, pw, e, f))
        rects.sort(key=lambda r: -r[0])
        TS = 32
        while True:
            x = y = shelf = 0
            ok = True
            placed = []
            for ph, pw, e, f in rects:
                if pw > TS:
                    ok = False
                    break
                if x + pw > TS:
                    x, y, shelf = 0, y + shelf, 0
                placed.append((x, y, pw, ph, e, f))
                x += pw
                shelf = max(shelf, ph)
            if ok and y + shelf <= TS:
                break
            TS *= 2
        img = T.new(TS, TS)
        for (x, y, pw, ph, e, f) in placed:
            fi = e.mat(pw, ph, e.seed + hash(f) % 97, 'top' if f == 'up' else 'bottom' if f == 'down' else 'front')
            g = T.new(pw, ph)
            for dec in e.decals.get(f, []) + e.decals.get('all', []):
                dec(fi, g)
            T.put(img, fi, x, y)
            e.rects[f] = (x, y, pw, ph)
        self.TS = TS
        self.texture = img
        return img

    def json(self, texpath, display, render_type=None):
        TS = self.TS
        els = []
        for e in self.els:
            faces = {}
            for f in e.faces:
                x, y, pw, ph = e.rects[f]
                faces[f] = {'uv': [round(x * 16 / TS, 4), round(y * 16 / TS, 4), round((x + pw) * 16 / TS, 4), round((y + ph) * 16 / TS, 4)], 'texture': '#0'}
            j = {'from': [round(v, 4) for v in e.frm], 'to': [round(v, 4) for v in e.to], 'faces': faces}
            rot = e.rot or self.global_rot
            if rot:
                j['rotation'] = {'angle': rot[1], 'axis': rot[0], 'origin': rot[2]}
            if e.glow:
                j['forge_data'] = {'block_light': 15, 'sky_light': 15}
                j['shade'] = False
            els.append(j)
        out = {'textures': {'0': texpath, 'particle': texpath}, 'elements': els, 'display': display}
        if render_type:
            out['render_type'] = render_type
        out['gui_light'] = 'front'
        return out

    # ------------------------------------------------------------ icon rendering
    def icon(self, size=32, yaw=0.0, pitch=0.0, supersample=8, pad=0.06):
        S = size * supersample
        quads = []
        for e in self.els:
            x0, y0, z0 = e.frm
            x1, y1, z1 = e.to
            corners = {
                'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
                'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
                'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
                'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
                'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
                'down': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
            }
            rot = e.rot or self.global_rot
            for f in e.faces:
                pts = np.array(corners[f], dtype=float)
                if rot:
                    pts = rotate(pts, rot)
                quads.append((pts, e.rects[f], e.translucent, f))
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

        def view(p):
            q = p - 8
            x = q[:, 0] * cy + q[:, 2] * sy
            z = -q[:, 0] * sy + q[:, 2] * cy
            y = q[:, 1] * cp - z * sp
            z2 = q[:, 1] * sp + z * cp
            return np.stack([x, y, z2], 1)

        allp = np.concatenate([view(q[0]) for q in quads])
        mn, mx = allp.min(0), allp.max(0)
        span = max(mx[0] - mn[0], mx[1] - mn[1])
        scale = S * (1 - 2 * pad) / span
        ccx, ccy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
        img = np.zeros((S, S, 4))
        zb = np.full((S, S), -1e9)
        tex = self.texture
        light = {'up': 1.0, 'down': 0.55, 'north': 0.8, 'south': 0.95, 'east': 0.72, 'west': 0.85}
        # draw opaque first then translucent
        for pass_t in (False, True):
            for pts, (rx, ry, rw, rh), transl, f in quads:
                if transl != pass_t:
                    continue
                v = view(pts)
                scr = np.stack([(v[:, 0] - ccx) * scale + S / 2, S / 2 - (v[:, 1] - ccy) * scale, v[:, 2]], 1)
                uvs = np.array([(rx + rw, ry), (rx, ry), (rx, ry + rh), (rx + rw, ry + rh)], dtype=float)
                if f in ('north', 'east', 'south', 'west'):
                    uvs = np.array([(rx, ry), (rx + rw, ry), (rx + rw, ry + rh), (rx, ry + rh)], dtype=float)
                for tri in ((0, 1, 2), (0, 2, 3)):
                    raster(img, zb, scr[list(tri)], uvs[list(tri)], tex, light[f], transl)
        im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').resize((size, size), Image.LANCZOS)
        a = np.asarray(im).astype(np.float32) / 255
        return outline(a)


def rotate(pts, rot):
    axis, ang, origin = rot
    a = math.radians(ang)
    c, s = math.cos(a), math.sin(a)
    p = pts - np.array(origin)
    if axis == 'z':
        x = p[:, 0] * c - p[:, 1] * s
        y = p[:, 0] * s + p[:, 1] * c
        p = np.stack([x, y, p[:, 2]], 1)
    elif axis == 'y':
        x = p[:, 0] * c + p[:, 2] * s
        z = -p[:, 0] * s + p[:, 2] * c
        p = np.stack([x, p[:, 1], z], 1)
    else:
        y = p[:, 1] * c - p[:, 2] * s
        z = p[:, 1] * s + p[:, 2] * c
        p = np.stack([p[:, 0], y, z], 1)
    return p + np.array(origin)


def raster(img, zb, tri, uv, tex, bright, transl):
    S = img.shape[0]
    a, b, c = tri
    x0, x1 = int(max(0, min(a[0], b[0], c[0]))), int(min(S - 1, max(a[0], b[0], c[0])) + 1)
    y0, y1 = int(max(0, min(a[1], b[1], c[1]))), int(min(S - 1, max(a[1], b[1], c[1])) + 1)
    if x1 <= x0 or y1 <= y0:
        return
    ys, xs = np.mgrid[y0:y1, x0:x1] + 0.5
    den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
    if abs(den) < 1e-9:
        return
    l1 = ((b[1] - c[1]) * (xs - c[0]) + (c[0] - b[0]) * (ys - c[1])) / den
    l2 = ((c[1] - a[1]) * (xs - c[0]) + (a[0] - c[0]) * (ys - c[1])) / den
    l3 = 1 - l1 - l2
    inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
    if not inside.any():
        return
    z = l1 * a[2] + l2 * b[2] + l3 * c[2]
    u = l1 * uv[0][0] + l2 * uv[1][0] + l3 * uv[2][0]
    v = l1 * uv[0][1] + l2 * uv[1][1] + l3 * uv[2][1]
    th, tw = tex.shape[:2]
    col = tex[np.clip(v.astype(int), 0, th - 1), np.clip(u.astype(int), 0, tw - 1)]
    region = zb[y0:y1, x0:x1]
    ok = inside & (z > region - (1e9 if transl else 0)) & (col[:, :, 3] > 0.05)
    if transl:
        ok = inside & (z > region - 0.01) & (col[:, :, 3] > 0.05)
        al = col[:, :, 3:4] * 0.85
        dst = img[y0:y1, x0:x1]
        mixed = dst[:, :, :3] * (1 - al) + col[:, :, :3] * bright * al
        dst[:, :, :3][ok] = mixed[ok]
        dst[:, :, 3][ok] = np.maximum(dst[:, :, 3][ok], al[:, :, 0][ok] + 0.3)
        return
    region[ok] = z[ok]
    img[y0:y1, x0:x1, :3][ok] = col[:, :, :3][ok] * bright
    img[y0:y1, x0:x1, 3][ok] = 1


def outline(a, color=(0.05, 0.04, 0.08)):
    """1px dark outline around the icon silhouette (classic Minecraft item look)."""
    al = a[:, :, 3] > 0.3
    sh = np.zeros_like(al)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        sh |= np.roll(np.roll(al, dx, 1), dy, 0)
    edge = sh & ~al
    out = a.copy()
    out[edge, :3] = color
    out[edge, 3] = 0.85
    return out


HANDHELD = {
    'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
}

OBJECT = {
    'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 1.5], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 1.5], 'scale': [0.5, 0.5, 0.5]},
    'firstperson_righthand': {'rotation': [0, -20, 0], 'translation': [1.5, 2.5, 0], 'scale': [0.55, 0.55, 0.55]},
    'firstperson_lefthand': {'rotation': [0, 20, 0], 'translation': [1.5, 2.5, 0], 'scale': [0.55, 0.55, 0.55]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
    'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [0.8, 0.8, 0.8]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.7, 0.7, 0.7]},
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
}
