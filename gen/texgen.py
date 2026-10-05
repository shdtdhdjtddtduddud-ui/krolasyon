"""Block textures, armor layer textures, gate/portal textures, NPC skins."""
import math
import numpy as np
from PIL import Image, ImageDraw
from paint import noise2, lerp


def _rng(seed):
    return np.random.default_rng(seed * 7717 + 5)


def premul(img):
    a = np.asarray(img).astype(np.float32)
    a[..., :3] *= a[..., 3:4] / 255.0
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), 'RGBA')


def to_img(a, alpha=None):
    a = np.clip(a, 0, 255)
    if a.shape[-1] == 3:
        al = np.full(a.shape[:2] + (1,), 255.0) if alpha is None else alpha[..., None] * 255.0
        a = np.concatenate([a, al], -1)
    return Image.fromarray(a.astype(np.uint8), 'RGBA')


def bricks(S, lo, hi, mortar, rows=4, cols=2, seed=1, glow=None, jitter=0.12, tile=False):
    r = _rng(seed)
    yy, xx = np.mgrid[0:S, 0:S]
    n = noise2(S, S, r, 3)
    rh = S // rows
    out = np.zeros((S, S, 3), np.float32)
    g = np.zeros((S, S, 4), np.float32)
    for row in range(rows):
        off = 0 if tile else (S // cols // 2) * (row % 2)
        bw = S // cols
        for c in range(-1, cols + 1):
            x0 = c * bw + off
            tone = (r.random() - 0.5) * jitter * 2
            ys = slice(row * rh, (row + 1) * rh)
            for y in range(row * rh, (row + 1) * rh):
                for x in range(max(0, x0), min(S, x0 + bw)):
                    t = np.clip(0.5 + (n[y, x] - 0.5) * 0.9 + tone, 0, 1)
                    # bevel: light at top-left, dark bottom-right
                    lx, ly = x - x0, y - row * rh
                    if ly == 0 or lx == 0:
                        t = min(1, t + 0.28)
                    if ly == rh - 2 or lx == bw - 2:
                        t = max(0, t - 0.2)
                    if ly == rh - 1 or lx == bw - 1:
                        out[y, x] = mortar
                        continue
                    out[y, x] = np.array(lo) * (1 - t) + np.array(hi) * t
    if glow is not None:
        m = (r.random((S, S)) > 0.965)
        for _ in range(3):
            x, y = r.integers(1, S - 1, 2)
            L = r.integers(3, 7)
            for k in range(L):
                yy2 = min(S - 1, y + k)
                xx2 = int(np.clip(x + r.integers(-1, 2), 0, S - 1))
                m[yy2, xx2] = True
                x = xx2
        out[m] = glow
        g[m, :3] = glow
        g[m, 3] = 255
    return out, g


def stone_noise(S, lo, hi, seed=1, speck=None, speck_p=0.04):
    r = _rng(seed)
    n = noise2(S, S, r, 3)
    out = np.array(lo) * (1 - n[..., None]) + np.array(hi) * n[..., None]
    if speck is not None:
        m = r.random((S, S)) < speck_p
        out[m] = speck
    return out


def make_blocks(S=16):
    B = {}
    # (top/all, optional side)
    a, _ = bricks(S, (52, 60, 80), (96, 108, 134), (22, 26, 36), seed=3)
    B['dungeon_bricks'] = a
    a, _ = bricks(S, (40, 46, 64), (84, 94, 120), (18, 22, 32), rows=2, cols=2, seed=4, tile=True)
    a[S // 2 - 1:S // 2 + 1, :] = (70, 150, 230)
    a[:, S // 2 - 1:S // 2 + 1] = (70, 150, 230)
    B['dungeon_floor'] = a
    base = stone_noise(S, (60, 66, 86), (110, 120, 146), 5)
    side = base.copy()
    side[:, 0:2] = (30, 34, 48); side[:, S - 2:] = (30, 34, 48)
    side[:, 2] = (130, 140, 170); side[:, S - 3] = (40, 44, 60)
    for y in range(0, S, 5):
        side[y, 2:S - 2] = (40, 44, 60)
    B['dungeon_pillar_side'] = side
    top = base.copy(); top[0:2] = (30, 34, 48); top[S - 2:] = (30, 34, 48); top[:, 0:2] = (30, 34, 48); top[:, S - 2:] = (30, 34, 48)
    top[5:S - 5, 5:S - 5] = (50, 56, 76)
    B['dungeon_pillar_top'] = top
    a, _ = bricks(S, (150, 130, 90), (230, 205, 150), (96, 80, 50), rows=4, cols=2, seed=6, tile=True)
    yy, xx = np.mgrid[0:S, 0:S]
    a[(abs(xx - 7.5) < 1) & (yy > 3) & (yy < 12)] = (255, 215, 110)
    a[(abs(yy - 7.5) < 1) & (xx > 4) & (xx < 11)] = (255, 215, 110)
    B['temple_stone'] = a
    a, _ = bricks(S, (110, 160, 210), (220, 244, 255), (60, 100, 160), seed=7)
    B['ice_bricks'] = a
    a, g = bricks(S, (26, 12, 20), (74, 34, 52), (10, 4, 8), seed=8, glow=(255, 60, 70))
    B['demon_bricks'] = a
    B['_demon_bricks_glow'] = g
    B['shadow_stone'] = stone_noise(S, (14, 8, 30), (56, 34, 110), 9, speck=(170, 110, 255), speck_p=0.05)
    r = _rng(10)
    hv = np.zeros((S, S, 3), np.float32)
    n = noise2(S, S, r, 3)
    for y in range(S):
        for x in range(S):
            cx, cy = (x % 8) - 3.5, (y % 8) - 3.5
            d = math.hypot(cx, cy)
            t = 0.35 + n[y, x] * 0.3
            c = np.array((120, 70, 40)) * (1 - t) + np.array((190, 130, 80)) * t
            if 2.4 < d < 3.5:
                c = np.array((70, 36, 22))
            if d <= 2.4:
                c = np.array((150, 96, 56)) * (0.8 + 0.2 * n[y, x])
            hv[y, x] = c
    B['ant_hive'] = hv
    st = stone_noise(S, (50, 60, 48), (110, 124, 96), 11, speck=(70, 150, 60), speck_p=0.18)
    B['swamp_stone'] = st
    B['goblin_stone'] = stone_noise(S, (74, 62, 52), (140, 124, 106), 12, speck=(46, 38, 32), speck_p=0.06)
    # ore: goblin stone + crystals
    ore = stone_noise(S, (84, 84, 94), (130, 130, 142), 13)
    og = np.zeros((S, S, 4), np.float32)
    r = _rng(14)
    for (cx, cy) in ((3, 4), (10, 3), (6, 10), (12, 11), (2, 12)):
        cols = ((110, 220, 255), (190, 130, 255), (120, 255, 170))[r.integers(0, 3)]
        for dy in range(-2, 2):
            for dx in range(-1, 2):
                if abs(dx) + abs(dy) * 0.5 <= 1.6:
                    y, x = cy + dy, cx + dx
                    if 0 <= x < S and 0 <= y < S:
                        t = 0.6 + 0.4 * (dy < 0)
                        ore[y, x] = np.array(cols) * t
    B['mana_crystal_ore'] = ore
    r = _rng(15)
    yy, xx = np.mgrid[0:S, 0:S]
    n = noise2(S, S, r, 2)
    cb = np.array((60, 150, 220)) * (1 - n[..., None]) + np.array((190, 245, 255)) * n[..., None]
    for k in range(-S, S, 5):
        m = (abs((xx - yy) - k) < 1)
        cb[m] = cb[m] * 0.6 + np.array((230, 252, 255)) * 0.4
    cb[0, :] = cb[-1, :] = cb[:, 0] = cb[:, -1] = (40, 100, 170)
    B['mana_crystal_block'] = cb
    lamp = np.zeros((S, S, 3), np.float32)
    d = np.hypot(xx - 7.5, yy - 7.5)
    lamp[:] = np.array((250, 252, 255)) * np.clip(1.1 - d / 12, 0.4, 1)[..., None]
    lamp[0, :] = lamp[-1, :] = lamp[:, 0] = lamp[:, -1] = (70, 120, 190)
    lamp[(xx % 8 == 0) | (yy % 8 == 0)] = (90, 150, 220)
    B['mana_lamp'] = lamp
    # teleport pad
    tp = np.zeros((S, S, 3), np.float32) + np.array((26, 32, 52))
    d = np.hypot(xx - 7.5, yy - 7.5)
    tp[(d > 5.2) & (d < 6.4)] = (90, 190, 255)
    tp[(d > 2.4) & (d < 3.2)] = (60, 140, 230)
    tp[(abs(xx - 7.5) < 0.8) | (abs(yy - 7.5) < 0.8)] = np.where(d[(abs(xx - 7.5) < 0.8) | (abs(yy - 7.5) < 0.8)][:, None] < 6.4, (140, 220, 255), (26, 32, 52))
    tp[d < 1.5] = (230, 250, 255)
    B['teleport_pad_top'] = tp
    ts = stone_noise(S, (50, 56, 76), (96, 106, 132), 16)
    ts[0, :] = (140, 190, 240); ts[-1, :] = (24, 28, 42)
    B['teleport_pad_side'] = ts
    return B


def block_glow(name, img):
    return None


# ------------------------------------------------------------ board textures (wood+paper) used by 3D board models in build
def wood_tex(S=16, seed=21, lo=(72, 46, 26), hi=(140, 96, 54)):
    r = _rng(seed)
    yy, xx = np.mgrid[0:S, 0:S]
    n = noise2(S, S, r, 2)
    g = 0.5 + 0.5 * np.sin(yy * 1.9 + n * 5)
    out = np.array(lo) * (1 - g[..., None]) + np.array(hi) * g[..., None]
    out[(yy % 8) == 7] *= 0.6
    return out


# ------------------------------------------------------------ gate textures
def swirl(S=256, arms=4, twist=5.0, seed=1):
    r = _rng(seed)
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    c = S / 2
    dx, dy = (xx - c) / c, (yy - c) / c
    d = np.hypot(dx, dy)
    ang = np.arctan2(dy, dx)
    spiral = 0.5 + 0.5 * np.sin(arms * ang + twist * np.log(d + 0.05) * 3 - d * 6)
    n = noise2(S, S, r, 18, 4)
    inten = np.clip(spiral * 0.65 + n * 0.5, 0, 1)
    fall = np.clip(1.0 - d, 0, 1) ** 0.6
    edge = np.clip((1.0 - d) * 8, 0, 1)
    a = inten * fall * edge
    core = np.exp(-(d / 0.18) ** 2)
    a = np.clip(a * 0.95 + core * 0.9, 0, 1)
    rgb = np.zeros((S, S, 3), np.float32) + 255
    rgb *= np.clip(0.55 + a[..., None] * 0.6, 0, 1)
    img = np.zeros((S, S, 4), np.float32)
    img[..., :3] = rgb
    img[..., 3] = a * 255
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


def gate_ring(S=256):
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    c = S / 2
    d = np.hypot(xx - c, yy - c) / c
    ang = np.arctan2(yy - c, xx - c)
    m = np.zeros((S, S))
    m = np.maximum(m, np.exp(-((d - 0.93) / 0.025) ** 2))
    m = np.maximum(m, np.exp(-((d - 0.82) / 0.012) ** 2) * 0.8)
    m = np.maximum(m, np.exp(-((d - 0.70) / 0.008) ** 2) * 0.6)
    ticks = (np.abs(((ang / (2 * np.pi) * 64) % 1) - 0.5) < 0.18) & (d > 0.835) & (d < 0.915)
    m = np.maximum(m, ticks * 0.9)
    glyph = (np.abs(((ang / (2 * np.pi) * 12) % 1) - 0.5) < 0.1) & (d > 0.71) & (d < 0.8)
    m = np.maximum(m, glyph * 0.85)
    img = np.zeros((S, S, 4), np.float32)
    img[..., :3] = 255
    img[..., 3] = np.clip(m, 0, 1) * 255
    return Image.fromarray(img.astype(np.uint8), 'RGBA')


def beam_tex(S=64):
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    d = np.abs(xx - S / 2) / (S / 2)
    a = np.clip(1 - d, 0, 1) ** 1.6
    img = np.zeros((S, S, 4), np.float32)
    img[..., :3] = 255
    img[..., 3] = a * 255
    return Image.fromarray(img.astype(np.uint8), 'RGBA')


def magic_circle(S=256, seed=3):
    """summoning / shadow-extraction circle"""
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    c = S / 2
    d = np.hypot(xx - c, yy - c) / c
    ang = np.arctan2(yy - c, xx - c)
    m = np.exp(-((d - 0.96) / 0.02) ** 2)
    m = np.maximum(m, np.exp(-((d - 0.86) / 0.01) ** 2))
    m = np.maximum(m, np.exp(-((d - 0.45) / 0.012) ** 2) * 0.9)
    for k in range(6):
        a0 = k / 6 * math.tau
        for pt in range(2):
            pass
    for tri in range(2):
        pts = [(c + math.cos(math.radians(-90 + tri * 60 + k * 120)) * c * 0.84, c + math.sin(math.radians(-90 + tri * 60 + k * 120)) * c * 0.84) for k in range(3)]
        for k in range(3):
            (x0, y0), (x1, y1) = pts[k], pts[(k + 1) % 3]
            vx, vy = x1 - x0, y1 - y0
            t = np.clip(((xx - x0) * vx + (yy - y0) * vy) / (vx * vx + vy * vy), 0, 1)
            dist = np.hypot(xx - (x0 + t * vx), yy - (y0 + t * vy))
            m = np.maximum(m, np.exp(-(dist / (S * 0.006)) ** 2))
    ticks = (np.abs(((ang / math.tau * 72) % 1) - 0.5) < 0.2) & (d > 0.88) & (d < 0.94)
    m = np.maximum(m, ticks * 0.9)
    img = np.zeros((S, S, 4), np.float32)
    img[..., :3] = 255
    img[..., 3] = np.clip(m, 0, 1) * 255
    return Image.fromarray(img.astype(np.uint8), 'RGBA')


# ------------------------------------------------------------ armor layers (64x32 vanilla layout)
def _boxrects(u, v, dx, dy, dz):
    return {'top': (u + dz, v, dx, dz), 'bottom': (u + dz + dx, v, dx, dz), 'west': (u, v + dz, dz, dy), 'north': (u + dz, v + dz, dx, dy),
            'east': (u + dz + dx, v + dz, dz, dy), 'south': (u + 2 * dz + dx, v + dz, dx, dy)}


def armor_layers(P, setid, seed=1):
    """P = palette dict from items3d ARMOR_SETS. returns (layer1, layer2) PIL images."""
    from entlib import mat_plate, mat_cloth, mat_glow
    plate = mat_plate(P['plate'][0], P['plate'][1], seams=True, trim=P['trim'][1])
    plate2 = mat_plate(P['plate2'][0], P['plate2'][1], seams=False, trim=P['trim'][0])
    trim = mat_plate(P['trim'][0], P['trim'][1], seams=False, rivets=False)
    cloth = mat_cloth(P['cloth'][0], P['cloth'][1])
    l1 = np.zeros((32, 64, 4), np.float32)
    l2 = np.zeros((32, 64, 4), np.float32)
    k = [0]

    def paint(L, rects, mat_fn, rows=None):
        for face, (x, y, w, h) in rects.items():
            if w <= 0 or h <= 0:
                continue
            k[0] += 1
            col, a, _ = mat_fn(None, face, int(w), int(h), _rng(seed * 100 + k[0]))
            sl = (slice(int(y), int(y + h)), slice(int(x), int(x + w)))
            L[sl][..., :3] = col
            L[sl][..., 3] = a * 255
            if rows is not None:
                lo, hi = rows
                if face not in ('top', 'bottom'):
                    L[int(y):int(y) + lo, int(x):int(x + w), 3] = 0
                    L[int(y) + hi:int(y + h), int(x):int(x + w), 3] = 0 if hi < h else L[int(y) + hi:int(y + h), int(x):int(x + w), 3]

    def region_alpha_cut(L, rects, keep_from=None, keep_to=None):
        for face, (x, y, w, h) in rects.items():
            if face in ('top', 'bottom'):
                continue
            if keep_from is not None:
                L[int(y):int(y + keep_from), int(x):int(x + w), 3] = 0
            if keep_to is not None:
                L[int(y + keep_to):int(y + h), int(x):int(x + w), 3] = 0

    # layer 1: head, body, arms (sleeves), legs (boots only: lower 5 rows)
    head = _boxrects(0, 0, 8, 8, 8)
    paint(l1, head, plate)
    # visor / eyes on the front
    x, y, w, h = head['north']
    x, y, w, h = int(x), int(y), int(w), int(h)
    l1[y + 3:y + 6, x + 1:x + w - 1, :3] = P['visor']
    l1[y + 4, x + 1:x + 3, :3] = P['eye'][1]; l1[y + 4, x + w - 3:x + w - 1, :3] = P['eye'][1]
    l1[y + 4, x + 1:x + 3, :3] = P['eye'][0]; l1[y + 4, x + w - 3:x + w - 1, :3] = P['eye'][0]
    l1[y, x:x + w, :3] = np.array(P['trim'][1])
    body = _boxrects(16, 16, 8, 12, 4)
    paint(l1, body, plate)
    bx, by, bw, bh = [int(v) for v in body['north']]
    # chest gem + trim
    l1[by + 3:by + 6, bx + 3:bx + 5, :3] = P['gem'][0]
    l1[by + bh - 2, bx:bx + bw, :3] = np.array(P['trim'][1])
    l1[by + 1, bx:bx + bw, :3] = np.array(P['trim'][0])
    arm = _boxrects(40, 16, 4, 12, 4)
    paint(l1, arm, plate2)
    for face, (x, y, w, h) in arm.items():
        if face == 'top':
            l1[int(y):int(y + h), int(x):int(x + w), :3] = np.array(P['trim'][1])
    leg = _boxrects(0, 16, 4, 12, 4)
    paint(l1, leg, plate2)
    region_alpha_cut(l1, leg, keep_from=7)
    for face, (x, y, w, h) in leg.items():
        if face not in ('top', 'bottom'):
            l1[int(y) + 7, int(x):int(x + w), :3] = np.array(P['trim'][1])
    # layer 2: leggings (belt on body bottom, legs upper)
    paint(l2, body, plate2)
    region_alpha_cut(l2, body, keep_from=8)
    for face, (x, y, w, h) in body.items():
        if face not in ('top', 'bottom'):
            l2[int(y) + 8, int(x):int(x + w), :3] = np.array(P['trim'][1])
    paint(l2, leg, plate)
    region_alpha_cut(l2, leg, keep_to=7)
    for face, (x, y, w, h) in leg.items():
        if face not in ('top', 'bottom'):
            l2[int(y) + 6, int(x):int(x + w), :3] = np.array(P['trim'][1])
    # gem on belt
    l2[by + 8:by + 10, bx + 3:bx + 5, :3] = P['gem'][0]
    return to_img(l1[..., :3], l1[..., 3] / 255.0), to_img(l2[..., :3], l2[..., 3] / 255.0)


# ------------------------------------------------------------ NPC skins (64x64 player layout)
def skin_texture(skin, hair, outfit, accent, style='suit', hair_style='short', eye=(30, 30, 40), seed=1):
    r = _rng(seed)
    im = np.zeros((64, 64, 4), np.float32)

    def fill(u, v, dx, dy, dz, fn):
        for face, (x, y, w, h) in _boxrects(u, v, dx, dy, dz).items():
            if w <= 0 or h <= 0:
                continue
            for yy in range(int(h)):
                for xx in range(int(w)):
                    c = fn(face, xx, yy, int(w), int(h))
                    if c is not None:
                        im[int(y) + yy, int(x) + xx] = (*c, 255)

    def noisy(c, a=6):
        return tuple(int(np.clip(v + r.integers(-a, a + 1), 0, 255)) for v in c)

    sk = skin
    dsk = tuple(int(v * 0.88) for v in skin)
    def head_fn(face, x, y, w, h):
        if face == 'north':
            if y in (3, 4) and x in (1, 2, 5, 6):
                return (255, 255, 255) if x in (1, 6) else eye
            if y == 6 and 2 <= x <= 5:
                return (min(255, sk[0] + 25), int(sk[1] * 0.7), int(sk[2] * 0.7))
            if y < 2 and hair_style != 'bald':
                return noisy(hair)
        if face in ('top',):
            return noisy(hair) if hair_style != 'bald' else noisy(sk)
        if face in ('west', 'east', 'south') and y < 4 and hair_style != 'bald':
            return noisy(hair)
        if face == 'south' and hair_style == 'long':
            return noisy(hair)
        return noisy(sk if y > 1 else sk)
    fill(0, 0, 8, 8, 8, head_fn)
    def cloth_fn(c1, c2, tie=False):
        def f(face, x, y, w, h):
            base = c1 if (y // 2) % 2 == 0 or style != 'stripe' else c2
            if face == 'north' and tie and x in (w // 2 - 1, w // 2) and 1 < y < 9:
                return noisy(accent)
            if y == 0:
                return noisy(c2)
            return noisy(base, 5)
        return f
    fill(16, 16, 8, 12, 4, cloth_fn(outfit[0], outfit[1], tie=(style == 'suit')))
    fill(40, 16, 4, 12, 4, lambda f, x, y, w, h: noisy(outfit[0]) if y < 8 else noisy(sk))
    fill(32, 48, 4, 12, 4, lambda f, x, y, w, h: noisy(outfit[0]) if y < 8 else noisy(sk))
    fill(0, 16, 4, 12, 4, lambda f, x, y, w, h: noisy(outfit[1]) if y < 10 else noisy((30, 30, 36)))
    fill(16, 48, 4, 12, 4, lambda f, x, y, w, h: noisy(outfit[1]) if y < 10 else noisy((30, 30, 36)))
    # hair overlay (long hair back)
    if hair_style in ('long',):
        for face, (x, y, w, h) in _boxrects(32, 0, 8, 8, 8).items():
            if face in ('south', 'west', 'east'):
                for yy in range(int(h) + 4 if False else int(h)):
                    for xx in range(int(w)):
                        im[int(y) + yy, int(x) + xx] = (*noisy(hair), 255)
    return Image.fromarray(np.clip(im, 0, 255).astype(np.uint8), 'RGBA')
