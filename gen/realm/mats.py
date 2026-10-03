"""Material painters for box-UV creature textures (2 texels per model unit).

Every painter has the signature (cube, face, W, H, rng) -> (rgb HxWx3, alpha HxW, glow HxWx4 | None)."""
import math
import numpy as np
from paint import noise2, lerp, border, glow_img, draw_line, cracks
from px import hexc, ramp

FACES = ('top', 'bottom', 'west', 'north', 'east', 'south')


def _ramp(stops, t):
    return ramp(stops, np.clip(t, 0, 1))


def face_light(face):
    return {'top': 0.18, 'bottom': -0.35, 'north': 0.05, 'south': -0.08, 'west': -0.02, 'east': -0.02}[face]


def edge_shade(H, W, amount=0.35, width=1):
    """darkens a 1-2 texel frame like ambient occlusion"""
    yy, xx = np.mgrid[0:H, 0:W]
    d = np.minimum(np.minimum(yy, H - 1 - yy), np.minimum(xx, W - 1 - xx)).astype(np.float32)
    return 1.0 - amount * np.clip(1 - d / max(width, 1), 0, 1)


def vgrad(H, W, top=0.15, bottom=-0.15):
    yy = np.mgrid[0:H, 0:W][0].astype(np.float32)
    return top + (bottom - top) * yy / max(H - 1, 1)


def glow_layer(H, W, color, mask):
    return glow_img(H, W, hexc(color) if isinstance(color, str) else color, mask)


def merge_glow(a, b):
    if a is None:
        return b
    if b is None:
        return a
    out = a.copy()
    al = b[..., 3:4] / 255.0
    out[..., :3] = out[..., :3] * (1 - al) + b[..., :3] * al
    out[..., 3] = np.maximum(out[..., 3], b[..., 3])
    return out


# ---------------------------------------------------------------- base surfaces
def skin(stops, fib=0.4, spots=None, veins=None, scale=3.0):
    def f(c, face, W, H, r):
        n = noise2(H, W, r, scale)
        yy, xx = np.mgrid[0:H, 0:W]
        wav = noise2(H, W, r, 6, 1)
        fibers = 0.5 + 0.5 * np.sin(xx * 1.7 + wav * 6)
        t = 0.5 + (n - 0.5) * 0.7 + (fibers - 0.5) * fib * 0.5 + face_light(face) + vgrad(H, W, 0.08, -0.12)
        col = _ramp(stops, t)
        if spots:
            m = noise2(H, W, r, 2.5) > 0.68
            col[m] = lerp(col[m], hexc(spots), 0.55)
        glow = None
        if veins and face not in ('bottom',):
            cm = cracks(H, W, r, max(1, int((W * H) / 160 + 1)), length=0.4)
            core = cm >= 1
            col[core] = hexc(veins)
            glow = glow_layer(H, W, veins, core * 0.9 + (cm > 0) * 0.15)
        col *= edge_shade(H, W, 0.3)[..., None]
        return col, np.ones((H, W)), glow
    return f


def plate(stops, trim=None, rivets='#d8c8a0', engrave=None, glow_engrave=False, bands=0):
    """forged armour plate: bevelled edges, vertical gradient, rivets, optional engraved rune line"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 4, 2)
        scratch = (noise2(H, W, r, 1.0, 1) > 0.86) & (r.random((H, W)) < 0.5)
        t = 0.55 + (n - 0.5) * 0.25 + vgrad(H, W, 0.2, -0.2) + face_light(face)
        col = _ramp(stops, t)
        col[scratch] = lerp(col[scratch], hexc(stops[-1]), 0.35)
        # bevel
        b1 = (yy == 0) | (xx == 0)
        b2 = (yy == H - 1) | (xx == W - 1)
        col[b1] = lerp(col[b1], hexc(stops[-1]), 0.45)
        col[b2] = lerp(col[b2], hexc(stops[0]), 0.6)
        if trim and min(W, H) >= 6:
            tm = ((yy == 1) | (yy == H - 2) | (xx == 1) | (xx == W - 2)) & ~b1 & ~b2
            col[tm] = lerp(col[tm], hexc(trim), 0.85)
        for k in range(bands):
            y = int((k + 1) * H / (bands + 1))
            col[y] = lerp(col[y], hexc(stops[0]), 0.7)
            if y + 1 < H:
                col[y + 1] = lerp(col[y + 1], hexc(stops[-1]), 0.3)
        if rivets and min(W, H) >= 6:
            for rx in range(2, W - 2, 3):
                col[1 if not trim else 2, rx] = hexc(rivets)
        glow = None
        if engrave and face in ('north', 'south', 'east', 'west') and W >= 7 and H >= 8:
            em = np.zeros((H, W), np.float32)
            cy = int(H * 0.62)
            for x in range(2, W - 2):
                k = (x - 2) % 4
                dy = (0, -1, 0, 1)[k]
                em[cy + dy, x] = 1
                if k == 0:
                    em[cy, x] = 1
            col[em > 0] = hexc(engrave)
            if glow_engrave:
                glow = glow_layer(H, W, engrave, em)
        return col, np.ones((H, W)), glow
    return f


def scales(stops, rim='#000000', size=3):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        row = yy // size
        off = (row % 2) * (size // 2 + 1)
        lx = (xx + off) % (size + 1)
        ly = yy % size
        d = np.hypot(lx - size / 2, ly - size + 0.5) / size
        t = 0.75 - d * 0.6 + face_light(face) + (r.random((H, W)) - 0.5) * 0.1 + vgrad(H, W, 0.05, -0.15)
        col = _ramp(stops, t)
        edge = (ly == size - 1) | (lx == 0)
        col[edge] = lerp(col[edge], hexc(rim), 0.5)
        col *= edge_shade(H, W, 0.25)[..., None]
        return col, np.ones((H, W)), None
    return f


def chitin(stops, band='#000000', shine='#ffffff', glow=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 5, 2)
        seg = (yy % 5 == 4)
        t = 0.45 + 0.25 * (n - 0.5) + face_light(face) + 0.25 * np.cos((xx / max(W - 1, 1) - 0.5) * math.pi)
        col = _ramp(stops, t)
        col[seg] = lerp(col[seg], hexc(band), 0.6)
        sh = (np.abs(xx - W * 0.3) < 1) & ~seg & (face != 'bottom')
        col[sh] = lerp(col[sh], hexc(shine), 0.35)
        g = None
        if glow:
            gm = seg & (r.random((H, W)) < 0.5) & (face in ('top', 'west', 'east'))
            col[gm] = hexc(glow)
            g = glow_layer(H, W, glow, gm)
        col *= edge_shade(H, W, 0.3)[..., None]
        return col, np.ones((H, W)), g
    return f


def bone(stops=('#4a4238', '#a89a84', '#d8ccb4', '#f4ecdc'), crack=True):
    def f(c, face, W, H, r):
        n = noise2(H, W, r, 3)
        t = 0.55 + 0.3 * (n - 0.5) + face_light(face)
        col = _ramp(list(stops), t)
        if crack and W * H > 30:
            cm = cracks(H, W, r, 1, length=0.3)
            col[cm >= 1] = lerp(col[cm >= 1], hexc(stops[0]), 0.7)
        col *= edge_shade(H, W, 0.35)[..., None]
        return col, np.ones((H, W)), None
    return f


def cloth(stops, trim=None, glyph=None, folds=True, tattered=False):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 4, 2)
        fold = 0.5 + 0.5 * np.sin(xx * 0.9 + noise2(H, W, r, 8, 1) * 4) if folds else 0.5
        weave = ((xx + yy) % 2) * 0.05
        t = 0.45 + 0.25 * (fold - 0.5) + 0.2 * (n - 0.5) + weave + face_light(face) + vgrad(H, W, 0.1, -0.1)
        col = _ramp(stops, t)
        alpha = np.ones((H, W))
        g = None
        if trim and H >= 6:
            tm = (yy >= H - 3) & (yy <= H - 2)
            col[tm] = lerp(col[tm], hexc(trim), 0.9)
            if glyph:
                gm = (yy == H - 5) & ((xx % 4) < 2)
                col[gm] = hexc(glyph)
                g = glow_layer(H, W, glyph, gm)
        if tattered:
            cut = H - 1 - (noise2(1, W, r, 2, 1)[0] * min(6, H * 0.3)).astype(int)
            alpha[yy > cut[None, :]] = 0
        col *= edge_shade(H, W, 0.2)[..., None]
        return col, alpha, g
    return f


def fur(stops, tip=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 3)
        streak = noise2(H, max(W, 2), r, 1.2, 1)[:, :W]
        strands = 0.5 + 0.5 * np.sin(xx * 2.1 + streak * 6 + yy * 0.25)
        t = 0.25 + 0.35 * n + 0.3 * strands + face_light(face)
        col = _ramp(stops, t)
        if tip:
            g = np.clip((yy / max(H - 1, 1) - 0.6) / 0.4, 0, 1)
            col = lerp(col, hexc(tip), g * 0.6)
        col *= edge_shade(H, W, 0.3)[..., None]
        return col, np.ones((H, W)), None
    return f


def crystal(stops, glow_col=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        k = r.random(6)
        t = 0.35 + 0.4 * np.abs(np.sin(xx * k[0] * 0.8 + yy * k[1] * 0.6 + k[2] * 6)) + face_light(face) + vgrad(H, W, 0.25, -0.1)
        col = _ramp(stops, t)
        edge = border(H, W)
        col[edge] = lerp(col[edge], hexc(stops[-1]), 0.6)
        g = None
        if glow_col:
            g = glow_layer(H, W, glow_col, np.clip((t - 0.55) * 1.6, 0, 1) * 0.7 + edge * 0.3)
        return col, np.ones((H, W)), g
    return f


def magma(rock=('#120606', '#2a1210', '#46201a', '#5a2a20'), hot='#ff6a10', core='#ffd060', density=1.3):
    def f(c, face, W, H, r):
        n = noise2(H, W, r, 3)
        t = 0.3 + 0.5 * n + face_light(face)
        col = _ramp(list(rock), t)
        cm = cracks(H, W, r, max(1, int(density * (W * H) / 70 + 1)), length=0.6)
        core_m = cm >= 1
        halo = (cm > 0) & ~core_m
        col[halo] = lerp(col[halo], hexc('#8a2008'), 0.7)
        col[core_m] = hexc(hot)
        hotm = core_m & (r.random((H, W)) < 0.3)
        col[hotm] = hexc(core)
        g = glow_layer(H, W, hot, core_m * 1.0 + halo * 0.35)
        g[hotm, :3] = hexc(core)
        col *= edge_shade(H, W, 0.3)[..., None]
        return col, np.ones((H, W)), g
    return f


def ash(stops=('#1e1c1c', '#3a3634', '#5e5854', '#8a827c'), ember='#ff7a20'):
    def f(c, face, W, H, r):
        n = noise2(H, W, r, 2, 3)
        g_ = r.random((H, W))
        t = 0.3 + 0.5 * n + 0.2 * (g_ - 0.5) + face_light(face)
        col = _ramp(list(stops), t)
        em = r.random((H, W)) < 0.025
        col[em] = hexc(ember)
        cm = cracks(H, W, r, max(1, (W * H) // 220), length=0.35)
        core = cm >= 1
        col[core] = lerp(col[core], hexc(ember), 0.8)
        glow = glow_layer(H, W, ember, em * 1.0 + core * 0.8)
        col *= edge_shade(H, W, 0.3)[..., None]
        return col, np.ones((H, W)), glow
    return f


def ghost(stops, alpha=170, wisp='#ffffff'):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 4, 2)
        t = 0.5 + 0.35 * (n - 0.5) + face_light(face) + vgrad(H, W, 0.15, -0.2)
        col = _ramp(stops, t)
        a = np.full((H, W), alpha, np.float32) / 255.0
        a = a * (0.75 + 0.25 * n)
        wm = (0.5 + 0.5 * np.sin(xx * 0.7 + yy * 0.4 + n * 6)) > 0.93
        col[wm] = lerp(col[wm], hexc(wisp), 0.6)
        g = glow_layer(H, W, stops[-1], np.clip(n - 0.35, 0, 1) * 0.6)
        return col, a, g
    return f


def flame(cols=('#ff2a10', '#ff8a20', '#ffe070'), shape=1.0):
    """plane sprite: tongue of fire, base at bottom of the face rect"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 3, 2)
        u = (xx + 0.5) / W * 2 - 1
        v = 1 - (yy + 0.5) / H
        width = (1 - v) ** 0.8 * (0.85 + 0.25 * np.sin(v * 9 + n * 4)) * shape
        inside = np.abs(u + 0.15 * np.sin(v * 6 + n * 3)) < width
        heat = np.clip(1 - np.abs(u) / np.maximum(width, 1e-3), 0, 1) * (1 - v * 0.6)
        col = _ramp(list(cols), heat)
        a = inside.astype(np.float32)
        g = glow_layer(H, W, cols[1], a)
        g[..., :3] = col
        return col, a, g
    return f


def membrane(stops, vein='#000000', edge_col=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 5, 2)
        t = 0.45 + 0.3 * (n - 0.5) + vgrad(H, W, 0.15, -0.15)
        col = _ramp(stops, t)
        for k in range(1, 4):
            x0 = int(k * W / 4)
            for y in range(H):
                x = int(x0 + (y / max(H, 1)) * (k - 2) * W / 6)
                if 0 <= x < W:
                    col[y, x] = lerp(col[y, x], hexc(vein), 0.7)
        # scalloped trailing edge
        cut = H - 1 - (np.abs(np.sin(xx / max(W, 1) * math.pi * 3)) * min(5, H * 0.25)).astype(int)
        a = (yy <= cut).astype(np.float32)
        side = c.feat.get('wing') if c.feat else None
        if side:
            dist = xx / max(W - 1, 1) if side == 'r' else 1 - xx / max(W - 1, 1)
            v = yy / max(H - 1, 1)
            a[dist > 0.3 + 0.95 * v] = 0
        if edge_col:
            em = (yy == cut) | (yy == cut - 1)
            col[em] = hexc(edge_col)
        return col, a, None
    return f


def solid(colr, glow=False):
    def f(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32) + hexc(colr)
        col *= (1 + face_light(face))
        col *= edge_shade(H, W, 0.2)[..., None]
        g = glow_layer(H, W, colr, np.ones((H, W))) if glow else None
        return col, np.ones((H, W)), g
    return f


def metal(stops, glow=None):
    """polished blade / steel with a bright specular stripe"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        t = 0.45 + 0.35 * np.cos((xx / max(W - 1, 1) - 0.35) * math.pi * 1.2) + face_light(face)
        n = noise2(H, W, r, 2, 1)
        t += (n - 0.5) * 0.1
        col = _ramp(stops, t)
        col[border(H, W)] = lerp(col[border(H, W)], hexc(stops[-1]), 0.4)
        g = glow_layer(H, W, glow, np.clip(t - 0.4, 0, 1)) if glow else None
        return col, np.ones((H, W)), g
    return f


def wood(stops=('#1a0e08', '#3a2412', '#5a3a1e', '#7a5430')):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 6, 1)
        t = 0.45 + 0.3 * np.sin(xx * 1.3 + n * 5) + face_light(face)
        col = _ramp(list(stops), t)
        return col, np.ones((H, W)), None
    return f


# ---------------------------------------------------------------- faces
def with_face(base, kind, eye='#ff3020', eye2='#ffe0a0', mouth=None, teeth='#e8e0d0'):
    """decorates the north face of a head cube"""
    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        if face != 'north':
            return col, a, g
        gm = np.zeros((H, W), np.float32)
        yy, xx = np.mgrid[0:H, 0:W]
        ey = int(H * 0.38)

        def eye_at(x, y, rad=1.3, slit=False):
            d = np.hypot((xx - x) * (0.8 if not slit else 1.4), (yy - y) * (1.3 if not slit else 0.7))
            col[d < rad + 1.0] = lerp(col[d < rad + 1.0], hexc('#0a0204'), 0.85)
            m = d < rad
            col[m] = hexc(eye)
            gm[m] = 1
            pc = d < rad * 0.45
            col[pc] = hexc(eye2)
        if kind in ('eyes2', 'beast', 'helm_eyes'):
            eye_at(W * 0.28, ey, max(1.0, W * 0.09))
            eye_at(W * 0.72 - 1, ey, max(1.0, W * 0.09))
        elif kind == 'eyes4':
            for x in (0.2, 0.4, 0.6, 0.8):
                eye_at(W * x - 0.5, ey + (1 if x in (0.2, 0.8) else -1), max(0.8, W * 0.06))
        elif kind == 'cyclops':
            eye_at(W / 2 - 0.5, ey, max(1.5, W * 0.18))
        elif kind == 'visor':
            m = (np.abs(yy - ey) <= 0) & (xx > 1) & (xx < W - 2)
            col[(np.abs(yy - ey) <= 1) & (xx > 0) & (xx < W - 1)] = hexc('#060204')
            col[m] = hexc(eye)
            gm[m] = 1
            cm = (xx == W // 2) & (yy > ey) & (yy < H - 1)
            col[cm] = hexc('#060204')
        elif kind == 'skull':
            for x in (W * 0.3, W * 0.7 - 1):
                d = np.hypot(xx - x, (yy - ey) * 1.1)
                col[d < max(1.6, W * 0.13)] = hexc('#060204')
                m = d < max(0.8, W * 0.05)
                col[m] = hexc(eye)
                gm[m] = 1
            nose = (np.abs(xx - W / 2 + 0.5) < 1) & (yy == int(H * 0.58))
            col[nose] = hexc('#060204')
            tm = (yy >= int(H * 0.72)) & (yy <= int(H * 0.8)) & (xx > W * 0.25) & (xx < W * 0.75)
            col[tm] = hexc(teeth)
            col[tm & (xx % 2 == 0)] = hexc('#3a2a20')
        elif kind == 'hood':
            dark = (np.hypot((xx - W / 2 + 0.5) / (W * 0.4), (yy - H * 0.55) / (H * 0.45)) < 1)
            col[dark] = hexc('#050206')
            for x in (W * 0.38, W * 0.62 - 1):
                m = np.hypot(xx - x, yy - H * 0.5) < max(0.8, W * 0.06)
                col[m] = hexc(eye)
                gm[m] = 1
        elif kind == 'mask':
            mm = (np.abs(xx - W / 2 + 0.5) < W * 0.42) & (yy > 1) & (yy < H - 1)
            col[mm] = lerp(col[mm], hexc('#f0e8e0'), 0.85)
            for x in (W * 0.3, W * 0.7 - 1):
                m = (np.abs(xx - x) < max(1, W * 0.08)) & (np.abs(yy - ey) < 1)
                col[m] = hexc(eye)
                gm[m] = 1
            sm = (np.abs(xx - W / 2 + 0.5) < 0.6) & (yy > ey + 2)
            col[sm] = hexc('#200810')
        if mouth:
            my = int(H * 0.72)
            mm = (np.abs(yy - my) <= 1) & (xx > W * 0.2) & (xx < W * 0.8)
            col[mm] = hexc(mouth)
            tm = (yy == my - 1) & (xx > W * 0.2) & (xx < W * 0.8) & (xx % 2 == 0)
            col[tm] = hexc(teeth)
        g = merge_glow(g, glow_layer(H, W, eye, gm))
        return col, a, g
    return f


def open_front(base, keep_top=0.0, frame=1):
    """hoods & helmets: the north face is cut open so the face underneath shows"""
    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        if face == 'north':
            yy, xx = np.mgrid[0:H, 0:W]
            hole = (yy >= int(H * keep_top) + (frame if keep_top == 0 else 0)) & (xx >= frame) & (xx < W - frame)
            a = a.copy()
            a[hole] = 0
            if g is not None:
                g = g.copy()
                g[hole] = 0
        return col, a, g
    return f
