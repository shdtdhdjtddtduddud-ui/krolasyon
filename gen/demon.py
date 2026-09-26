"""Kalp Kırıcı İblis (Heartbreaker Demon) — horned crimson demon, glowing heart sigil, black thorns, scythe tail."""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, noise2, lerp, border, glow_img, draw_line
from animlib import add_rot, sym, pose_seq

M = Model('heart_demon', 512, 256)
R0 = np.eye(3)


def local_bone(name, parent, off, rot=(0, 0, 0)):
    M.bone(name, parent, (0, 0, 0))
    M.bones[name].off = [float(v) for v in off]
    M.bones[name].rot = [float(v) for v in rot]
    return M.bones[name]


def chain(prefix, parent, pts, widths, mat, depth_ratio=1.0, ext=0.6):
    p = parent
    for i in range(len(pts) - 1):
        n = f'{prefix}{i}'
        w = widths[min(i, len(widths) - 1)]
        M.seg(n, p, pts[i], pts[i + 1], w, w * depth_ratio, mat if isinstance(mat, str) else mat[min(i, len(mat) - 1)], extend=ext)
        p = n
    return p


# ---------------- skeleton ----------------
M.bone('base', None, (0, -35, 0))
M.bone('hips', 'base', (0, -35, 0))
M.cube('hips', (-7.5, -37.5, -4.3), (15, 6.5, 8.6), 'pants')
M.cube('hips', (-8, -38.5, -4.9), (16, 2.8, 9.8), 'belt')
M.cube('hips', (-2.4, -39.2, -5.6), (4.8, 4, 0.9), 'buckle')
M.cube('hips', (-8.6, -36.5, -2.5), (1, 10, 1), 'chain')
M.cube('hips', (7.6, -36.5, -1.5), (1, 8, 1), 'chain')
M.cube('hips', (-7.2, -36.5, -5.2), (2.2, 9, 0.6), 'belt')

M.bone('waist', 'hips', (0, -38, 0))
M.cube('waist', (-6, -49.5, -3.7), (12, 11.5, 7.4), 'skin_abs')
M.bone('chest', 'waist', (0, -49, 0))
M.cube('chest', (-10.5, -63.5, -5.2), (21, 14.5, 10.4), 'skin_chest')
M.cube('chest', (-7.5, -65.5, -3.2), (15, 3, 6.4), 'skin_hi')
M.bone('neck', 'chest', (0, -64.5, 0))
M.cube('neck', (-2.7, -70, -2.6), (5.4, 6.5, 5.2), 'skin_hi')
M.bone('head', 'neck', (0, -69, 0))
M.cube('head', (-4.6, -79.5, -4.7), (9.2, 10.5, 9.4), 'demon_head')
M.cube('head', (-3.6, -70, -4.3), (7.2, 2, 7), 'demon_head')

# horns: two tall curved ones, two side ones
for sx, side in ((-1, 'r'), (1, 'l')):
    chain(f'horn_{side}', 'head', [(sx * 3, -79, -0.5), (sx * 6.5, -85, -0.5), (sx * 7.2, -92, 0.5), (sx * 5.5, -99, 1.5)],
          (2.8, 2.0, 1.2), ('horn0', 'horn1', 'horn2'), ext=0.4)
    chain(f'shorn_{side}', 'head', [(sx * 4.4, -75.5, 0), (sx * 10, -78, 0.5), (sx * 12.5, -84, 1)], (2.0, 1.3), ('horn1', 'horn2'), ext=0.3)
    # crown thorns
    for k, (x, y, rz) in enumerate(((2.2, -79.5, 18), (4.2, -78.5, 42))):
        n = f'crown_{side}{k}'
        M.bone(n, 'head', (sx * x, y, 1), (-10, 0, sx * rz))
        M.cube_l(n, (-3, -7, 0), (6, 7, 0), 'thorn', plane=True)

# shoulder thorn clusters
for sx, side in ((-1, 'r'), (1, 'l')):
    for k, (x, y, z, rx, rz, w, h) in enumerate(((11, -64, -1, -10, 35, 12, 17), (13.5, -62, 1.5, 15, 60, 11, 16), (8, -65, 2, 20, 12, 9, 13))):
        n = f'sthorn_{side}{k}'
        M.bone(n, 'chest', (sx * x, y, z), (rx, sx * 20, sx * rz))
        M.cube_l(n, (-w / 2, -h, 0), (w, h, 0), 'thorn', plane=True)

# arms
for side, sx in (('right', -1), ('left', 1)):
    A, B, C = (sx * 12.8, -61.5, 0), (sx * 15.5, -46, 1.2), (sx * 16.8, -31, 0.2)
    M.seg(f'{side}_arm', 'chest', A, B, 7.2, 7.2, 'skin_arm', extend=2.2)
    M.seg(f'{side}_fore', f'{side}_arm', B, C, 6.2, 6.2, 'skin_fore', extend=1.6)
    M.bone_w(f'{side}_hand', f'{side}_fore', C, R0)
    M.cube_l(f'{side}_hand', (-3, 0, -2.8), (6, 6.5, 5.6), 'skin_lo')
    for k in range(4):
        M.cube_l(f'{side}_hand', (-2.6 + k * 1.5, 6.3, -2.6), (1.1, 2.2, 1.1), 'claw')
    for k, (y, rz, w, h) in enumerate(((2, -55, 7, 11), (7, -70, 8, 12), (11, -40, 6, 9))):
        n = f'athorn_{side}{k}'
        local_bone(n, f'{side}_fore', (sx * 3.0, y, 0.5), (0, sx * 25, sx * -rz))
        M.cube_l(n, (-w / 2, -h, 0), (w, h, 0), 'thorn', plane=True)

# back tendrils hanging behind towards the left of the image
for k, pts in enumerate(([(-5, -58, 5.3), (-13, -48, 9), (-19, -34, 11), (-22, -18, 9)],
                         [(4, -57, 5.3), (-4, -42, 10), (-12, -26, 12), (-14, -10, 10)])):
    p = 'chest'
    for i in range(3):
        n = f'tend{k}_{i}'
        M.seg(n, p, pts[i], pts[i + 1], 1.4, 1.4, 'tail', extend=0.4)
        local_bone(f'{n}p', n, (0, 0, 0), (0, 0, 0))
        L = float(np.linalg.norm(np.array(pts[i + 1]) - np.array(pts[i])))
        M.cube_l(f'{n}p', (-5, -1, 0), (10, L + 2, 0), 'thorn_long', plane=True)
        p = n

# legs: baggy trousers + boots
for side, sx in (('right', -1), ('left', 1)):
    A, B, C = (sx * 5, -35.5, 0), (sx * 6.8, -10, -1.5), (sx * 6.8, 12.5, 0.5)
    M.seg(f'{side}_leg', 'hips', A, B, 9.2, 9.6, 'pants', extend=2.5)
    M.seg(f'{side}_shin', f'{side}_leg', B, C, 8.8, 9.2, 'pants_lo', extend=1.5)
    M.cube_l(f'{side}_shin', (-4.9, 19.5, -5.1), (9.8, 3, 10.2), 'cuff')
    M.bone_w(f'{side}_foot', f'{side}_shin', C, R0)
    M.cube_l(f'{side}_foot', (-4, -1.5, -6.5), (8, 13, 10.5), 'boot')

# thorn tail ending in a crimson scythe
TAIL = [(0, -37, 5), (5, -30, 11), (12, -20, 15), (18, -8, 15), (23, 3, 11), (26, 12, 5)]
last = chain('tail', 'hips', TAIL, (2.4, 2.1, 1.8, 1.6, 1.4), 'tail', ext=0.5)
for i in range(len(TAIL) - 1):
    n = f'tthorn{i}'
    local_bone(n, f'tail{i}', (0, 3, 0), (0, 45 * i, 35))
    M.cube_l(n, (-3, -7, 0), (6, 7, 0), 'thorn', plane=True)
# scythe crescent (arc in the XY plane at the tail tip)
end = np.array(TAIL[-1], float)
cx, cy = end[0] + 4, end[1] - 11
arc = [(-100 + k * 30) for k in range(8)]
pts = [(cx + 12 * math.cos(math.radians(a)), cy + 12 * math.sin(math.radians(a)) * 1.0, end[2]) for a in arc]
M.seg('scythe_root', last, tuple(end), pts[3], 1.6, 1.2, 'tail', extend=0.3)
p = last
for i in range(len(pts) - 1):
    w = 4.2 - abs(i - 3) * 0.75
    M.seg(f'scythe{i}', 'scythe_root' if i == 0 else f'scythe{i - 1}', pts[i], pts[i + 1], max(1.0, w), 0.9, 'scythe', extend=0.5)

# ---------------- textures ----------------
P = Painter(M)
INK = (60, 8, 22)


def skin(c, face, W, H, r, top=(232, 104, 104), mid=(186, 50, 62), low=(104, 20, 38), grad=(0.0, 1.0), lines=1.0):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 4)
    vt = grad[0] + (grad[1] - grad[0]) * yy / max(H - 1, 1)
    if face == 'top':
        vt = np.full((H, W), grad[0])
    if face == 'bottom':
        vt = np.full((H, W), grad[1])
    col = np.where(vt[..., None] < 0.5, lerp(top, mid, vt * 2), lerp(mid, low, (vt - 0.5) * 2))
    col = col * (0.9 + 0.2 * n[..., None])
    # sparse muscle definition strokes
    if lines > 0 and face not in ('top', 'bottom'):
        m = np.zeros((H, W))
        for _ in range(int(W * H / 60 * lines)):
            x0, y0 = r.random() * W, r.random() * H
            ang = math.pi / 2 + r.normal(0, 0.4)
            L = 2 + r.random() * 4
            draw_line(m, x0, y0, x0 + math.cos(ang) * L, y0 + math.sin(ang) * L)
        col[m > 0] = lerp(col[m > 0], INK, 0.45)
    b = border(H, W)
    col[b] = lerp(col[b], INK, 0.55)
    return col, np.ones((H, W)), None


def heart_mask(H, W, cx, cy, size):
    yy, xx = np.mgrid[0:H, 0:W]
    x = (xx - cx) / size
    y = -(yy - cy) / size
    return ((x * x + y * y - 1) ** 3 - x * x * y ** 3) <= 0


P.mats['skin_hi'] = lambda c, f, W, H, r: skin(c, f, W, H, r, grad=(0.0, 0.3))
P.mats['skin_abs'] = lambda c, f, W, H, r: abs_skin(c, f, W, H, r)
P.mats['skin_fore'] = lambda c, f, W, H, r: skin(c, f, W, H, r, grad=(0.45, 0.85))
P.mats['skin_lo'] = lambda c, f, W, H, r: skin(c, f, W, H, r, grad=(0.8, 1.0))


def abs_skin(c, face, W, H, r):
    col, a, g = skin(c, face, W, H, r, grad=(0.3, 0.65), lines=0.4)
    if face == 'north':
        for y in range(2, H - 1, 3):
            col[y, 2:W - 2] = lerp(col[y, 2:W - 2], INK, 0.5)
        col[:, W // 2] = lerp(col[:, W // 2], INK, 0.6)
    return col, a, g


@P.mat('skin_chest')
def skin_chest(c, face, W, H, r):
    col, a, _ = skin(c, face, W, H, r, grad=(0.0, 0.35), lines=0.5)
    if face != 'north':
        return col, a, None
    S = 4
    hm = heart_mask(H * S, W * S, W * S / 2, H * S * 0.45, W * S * 0.2)
    inner = heart_mask(H * S, W * S, W * S / 2, H * S * 0.45, W * S * 0.12)
    ring = hm & ~inner
    m = np.zeros((H * S, W * S))
    m[ring] = 1
    m[inner] = 0.55
    # tribal wings from the heart toward the collarbones and down the sternum
    tm = np.zeros((H * S, W * S))
    cxs, cys = W * S / 2, H * S * 0.42
    for sx in (-1, 1):
        for k in range(3):
            draw_line(tm, cxs + sx * W * S * 0.08, cys - k * 5, cxs + sx * W * S * (0.3 + k * 0.06), cys - H * S * (0.3 - k * 0.08), thick=1)
        draw_line(tm, cxs + sx * 3, cys + H * S * 0.12, cxs + sx * W * S * 0.12, cys + H * S * 0.45, thick=1)
    draw_line(tm, cxs, cys + H * S * 0.15, cxs, cys + H * S * 0.55, thick=1)
    m = np.maximum(m, tm * 0.85)
    m = m.reshape(H, S, W, S).mean((1, 3))
    glow = m > 0.25
    col[glow] = lerp(col[glow], (255, 225, 210), np.clip(m[glow], 0, 1))
    g = glow_img(H, W, (255, 170, 160), np.clip(m * 1.3, 0, 1))
    return col, a, g


@P.mat('skin_arm')
def skin_arm(c, face, W, H, r):
    col, a, _ = skin(c, face, W, H, r, grad=(0.05, 0.5))
    if face in ('north', 'west', 'east'):
        S = 4
        hm = heart_mask(H * S, W * S, W * S / 2, H * S * 0.22, W * S * 0.22)
        inner = heart_mask(H * S, W * S, W * S / 2, H * S * 0.22, W * S * 0.12)
        m = (hm & ~inner).astype(float).reshape(H, S, W, S).mean((1, 3))
        col[m > 0.3] = lerp(col[m > 0.3], (255, 215, 200), 0.8)
        return col, a, glow_img(H, W, (255, 160, 150), np.clip(m * 1.2, 0, 1))
    return col, a, None


@P.mat('demon_head')
def demon_head(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 3)
    col = lerp((14, 6, 10), (48, 16, 26), n)
    b = border(H, W)
    col[b] = lerp(col[b], (110, 20, 34), 0.5)
    if face != 'north' or H < 6:
        return col, np.ones((H, W)), None
    S = 4
    Y, X = np.mgrid[0:H * S, 0:W * S] / S
    g = np.zeros((H, W))
    m = np.zeros((H * S, W * S))
    for sx in (-1, 1):
        # slanted almond eye rising towards the outside
        cxe, cye = W / 2 + sx * W * 0.22, H * 0.42
        u = (X - cxe) * sx
        v = (Y - cye) + u * 0.45
        eye = (u / (W * 0.2)) ** 2 + (v / (H * 0.08)) ** 2 < 1
        m[eye] = 1
    m = m.reshape(H, S, W, S).mean((1, 3))
    col[m > 0.2] = lerp(col[m > 0.2], (255, 238, 228), np.clip(m[m > 0.2] * 1.2, 0, 1))
    # nose ridge
    col[int(H * 0.5):int(H * 0.8), W // 2] = (60, 22, 32)
    gl = glow_img(H, W, (255, 200, 200), np.clip(m * 1.4, 0, 1))
    # soft pink halo
    halo = np.zeros((H, W))
    halo[1:] = np.maximum(halo[1:], m[:-1]); halo[:-1] = np.maximum(halo[:-1], m[1:])
    halo[:, 1:] = np.maximum(halo[:, 1:], m[:, :-1]); halo[:, :-1] = np.maximum(halo[:, :-1], m[:, 1:])
    h2 = (halo > 0.2) & (m <= 0.2)
    gl[h2] = (255, 110, 130, 120)
    return col, np.ones((H, W)), gl


def horn(c, face, W, H, r, t0, t1):
    yy, xx = np.mgrid[0:H, 0:W]
    t = t0 + (t1 - t0) * (1 - yy / max(H - 1, 1)) if face not in ('top', 'bottom') else np.full((H, W), t1 if face == 'top' else t0)
    col = lerp((40, 10, 16), (232, 44, 56), np.clip(t, 0, 1))
    ring = (yy % 3 == 0) & (face not in ('top', 'bottom'))
    col[ring] = col[ring] * 0.75
    return col, np.ones((H, W)), glow_img(H, W, (255, 50, 60), np.clip((t - 0.6) * 1.2, 0, 0.6))


P.mats['horn0'] = lambda c, f, W, H, r: horn(c, f, W, H, r, 0.0, 0.3)
P.mats['horn1'] = lambda c, f, W, H, r: horn(c, f, W, H, r, 0.3, 0.7)
P.mats['horn2'] = lambda c, f, W, H, r: horn(c, f, W, H, r, 0.7, 1.0)


def thorn_shape(c, face, W, H, r, holes=2):
    """black tribal thorn silhouette on a plane: base at bottom, tip at top"""
    S = 3
    Hs, Ws = H * S, W * S
    yy, xx = np.mgrid[0:Hs, 0:Ws] / S
    u = (xx - W / 2) / (W / 2)
    v = 1 - yy / H
    wob = noise2(Hs, Ws, r, 4 * S, 2)
    spine = 0.35 * np.sin(v * 3.2 + r.random() * 3) * v
    width = (1 - v) ** 0.8 * 0.55 + 0.05
    inside = np.abs(u - spine) < width
    # side barbs
    for _ in range(3 + int(r.random() * 3)):
        by = r.random() * 0.7 + 0.1
        sd = r.choice([-1, 1])
        L = 0.4 + r.random() * 0.5
        bx0 = spine_at = 0.35 * math.sin(by * 3.2) * by
        for k in range(30):
            t = k / 29
            px = bx0 + sd * t * L
            py = by + t * (0.25 + r.random() * 0.02)
            w_ = (1 - t) * 0.09 + 0.01
            inside |= (np.abs(u - px) < w_ * 2) & (np.abs(v - py) < w_)
    hole = np.zeros_like(inside)
    for _ in range(holes):
        hx, hy = (r.random() - 0.5) * 0.4, 0.15 + r.random() * 0.4
        hole |= ((u - hx) / 0.14) ** 2 + ((v - hy) / 0.08) ** 2 < 1
    inside &= ~hole
    edge = inside & ~(np.roll(inside, 2, 1) & np.roll(inside, -2, 1) & np.roll(inside, 2, 0) & np.roll(inside, -2, 0))
    col = np.zeros((Hs, Ws, 3)) + (26, 8, 14)
    col = col * (0.8 + 0.4 * wob[..., None])
    col[edge] = (150, 26, 44)
    col = col.reshape(H, S, W, S, 3).mean((1, 3))
    alpha = (inside.reshape(H, S, W, S).mean((1, 3)) > 0.4).astype(float)
    em = edge.reshape(H, S, W, S).mean((1, 3))
    return col, alpha, glow_img(H, W, (200, 30, 50), np.clip(em * 0.6, 0, 0.5))


P.mats['thorn'] = thorn_shape
P.mats['thorn_long'] = lambda c, f, W, H, r: thorn_shape(c, f, W, H, r, holes=3)


def pants(c, face, W, H, r, low=False):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 3)
    folds = 0.5 + 0.5 * np.sin(xx * 0.9 + n * 4 + (yy * 0.35 if low else 0))
    t = np.clip(0.45 + (folds - 0.5) * 0.5 + (n - 0.5) * 0.3 - (0.2 * yy / H if not low else 0.1), 0, 1)
    col = lerp((70, 30, 42), (146, 70, 84), t)
    if low:
        for y in range(3, H, 5):
            col[y] = lerp(col[y], (50, 20, 30), 0.5)
    b = border(H, W)
    col[b] = lerp(col[b], (40, 14, 24), 0.6)
    return col, np.ones((H, W)), None


P.mats['pants'] = pants
P.mats['pants_lo'] = lambda c, f, W, H, r: pants(c, f, W, H, r, True)


@P.mat('cuff')
def cuff(c, face, W, H, r):
    col = lerp((60, 26, 36), (95, 44, 56), noise2(H, W, r, 2))
    col[:, ::4] = (40, 14, 22)
    return col, np.ones((H, W)), None


@P.mat('belt')
def belt(c, face, W, H, r):
    col = lerp((40, 22, 24), (80, 46, 44), noise2(H, W, r, 2))
    col[0] = (20, 10, 12); col[-1] = (20, 10, 12)
    for x in range(3, W, 6):
        if H > 2: col[H // 2, x] = (160, 150, 150)
    return col, np.ones((H, W)), None


@P.mat('buckle')
def buckle(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    col = lerp((150, 150, 165), (235, 235, 245), noise2(H, W, r, 2))
    col[border(H, W)] = (90, 90, 105)
    if face == 'north':
        col[H // 2, 1:W - 1] = (80, 80, 95)
    return col, np.ones((H, W)), glow_img(H, W, (200, 200, 220), (xx == 1) * 0.25)


@P.mat('chain')
def chain_m(c, face, W, H, r):
    col = np.zeros((H, W, 3)) + (170, 170, 185)
    col[::2] = (90, 90, 105)
    return col, np.ones((H, W)), None


@P.mat('boot')
def boot(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    col = lerp((34, 14, 20), (84, 32, 42), noise2(H, W, r, 3))
    if face in ('north', 'west', 'east', 'south'):
        for y in range(2, H - 3, 3):
            col[y] = lerp(col[y], (20, 8, 12), 0.6)
        col[H - 2:] = (20, 8, 12)
    col[border(H, W)] = lerp(col[border(H, W)], (15, 5, 10), 0.6)
    return col, np.ones((H, W)), None


@P.mat('claw')
def claw(c, face, W, H, r):
    col = np.zeros((H, W, 3)) + (30, 8, 14)
    col[-1:] = (120, 20, 34)
    return col, np.ones((H, W)), None


@P.mat('tail')
def tail(c, face, W, H, r):
    col = lerp((16, 6, 10), (60, 16, 28), noise2(H, W, r, 2))
    return col, np.ones((H, W)), None


@P.mat('scythe')
def scythe(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    t = xx / max(W - 1, 1)
    col = lerp((120, 12, 30), (255, 70, 82), np.clip(t * 1.2, 0, 1))
    col[:, -1:] = (255, 170, 170)
    return col, np.ones((H, W)), glow_img(H, W, (255, 60, 76), np.clip(t * 0.8, 0.15, 0.8))


# ---------------- animations ----------------
A = {}
LIMBS = ['right_arm', 'right_fore', 'right_hand', 'left_arm', 'left_fore', 'left_hand', 'right_leg', 'right_shin', 'left_leg',
         'left_shin', 'right_foot', 'left_foot', 'chest', 'waist', 'head', 'hips', 'neck', 'tail0', 'tail1', 'tail2', 'tail3', 'tail4']
THORNS = [n for n in M.order if 'thorn' in n or n.startswith('crown_') or n.endswith('p') and n.startswith('tend')]


def tail_wave(a, length, amp=1.0, speed=1, lift=0.0):
    for i in range(5):
        keys = []
        for k in range(9):
            t = length * k / 8
            ph = math.tau * k / 8 * speed - i * 0.8
            keys.append((t, (lift + 6 * amp * math.sin(ph), 10 * amp * math.cos(ph), 5 * amp * math.sin(ph + 1))))
        a.rot(f'tail{i}', *keys)
    for k in range(2):
        for i in range(3):
            keys = []
            for j in range(9):
                t = length * j / 8
                ph = math.tau * j / 8 * speed - i * 0.9 - k * 2
                keys.append((t, (4 * amp * math.sin(ph), 0, 8 * amp * math.cos(ph))))
            a.rot(f'tend{k}_{i}', *keys)


def thorn_flicker(a, length, amp=1.0):
    for i, n in enumerate(THORNS):
        keys = []
        for k in range(9):
            t = length * k / 8
            s = 1 + 0.12 * amp * math.sin(k * 2.1 + i * 1.3)
            keys.append((t, (1, s, 1)))
        a.scl(n, *keys)


idle = Anim('idle', 3.2, True)
idle.pos('base', (0, (0, 0, 0)), (1.6, (0, -0.6, 0)), (3.2, (0, 0, 0)))
idle.rot('chest', (0, (0, 0, 0)), (1.6, (-4, 0, 0)), (3.2, (0, 0, 0)))
idle.rot('head', (0, (0, 0, 0)), (0.8, (4, 8, 0)), (1.6, (-2, 0, 0)), (2.4, (4, -8, 0)), (3.2, (0, 0, 0)))
add_rot(idle, [(0, sym({'right_arm': (0, 0, 0)})), (1.6, sym({'right_arm': (0, 0, 4), 'right_fore': (-6, 0, 0)})), (3.2, sym({'right_arm': (0, 0, 0), 'right_fore': (0, 0, 0)}))])
tail_wave(idle, 3.2, 0.8)
thorn_flicker(idle, 3.2)
A['IDLE'] = idle


def wf(s, big=1.0):
    return {'right_leg': (-28 * s * big, 0, 0), 'right_shin': (max(0, 36 * s) * big + 4, 0, 0),
            'left_leg': (28 * s * big, 0, 0), 'left_shin': (max(0, -36 * s) * big + 4, 0, 0),
            'right_arm': (24 * s * big, 0, 0), 'left_arm': (-24 * s * big, 0, 0), 'right_fore': (-12 * big, 0, 0), 'left_fore': (-12 * big, 0, 0),
            'hips': (0, 8 * s, 0), 'chest': (3 * big, -10 * s, 0)}


walk = Anim('walk', 1.2, True)
add_rot(walk, [(0, wf(1)), (0.3, wf(0)), (0.6, wf(-1)), (0.9, wf(0)), (1.2, wf(1))])
walk.pos('base', (0, (0, 0, 0)), (0.3, (0, 1.2, 0)), (0.6, (0, 0, 0)), (0.9, (0, 1.2, 0)), (1.2, (0, 0, 0)))
tail_wave(walk, 1.2, 1.0, speed=1)
thorn_flicker(walk, 1.2)
A['WALK'] = walk

run = Anim('run', 0.7, True)
rf = lambda s: wf(s, 1.7) | {'base': (16, 0, 0), 'head': (-14, 0, 0), 'right_fore': (-60, 0, 0), 'left_fore': (-60, 0, 0)}
add_rot(run, [(0, rf(1)), (0.175, rf(0)), (0.35, rf(-1)), (0.525, rf(0)), (0.7, rf(1))])
run.pos('base', (0, (0, 0, 0)), (0.175, (0, 2.5, 0)), (0.35, (0, 0, 0)), (0.525, (0, 2.5, 0)), (0.7, (0, 0, 0)))
tail_wave(run, 0.7, 0.7, speed=1, lift=-25)
thorn_flicker(run, 0.7, 2)
A['RUN'] = run

atk = Anim('attack', 1.0)
pose_seq(atk, [
    (0.0, {}),
    (0.2, {'right_arm': (-130, 0, 40), 'right_fore': (-40, 0, 0), 'chest': (-8, 25, 0), 'waist': (0, 10, 0)}),
    (0.35, {'right_arm': (-30, 20, -30), 'right_fore': (-10, 0, 0), 'chest': (12, -25, 0), 'waist': (0, -10, 0), 'left_arm': (-40, 0, 0)}),
    (0.55, {'left_arm': (-130, 0, -40), 'left_fore': (-40, 0, 0), 'chest': (-8, -25, 0), 'right_arm': (10, 0, 0)}),
    (0.7, {'left_arm': (-30, -20, 30), 'left_fore': (-10, 0, 0), 'chest': (12, 25, 0), 'waist': (0, 10, 0)}),
    (1.0, {}),
], LIMBS)
atk.pos('base', (0, (0, 0, 0)), (0.35, (0, -1, -4)), (0.7, (0, -1, -5)), (1.0, (0, 0, 0)))
tail_wave(atk, 1.0, 1.2, speed=1)
A['ATTACK'] = atk

whip = Anim('whip', 1.2)
pose_seq(whip, [
    (0.0, {}),
    (0.35, {'right_arm': (-40, -30, 80), 'right_fore': (-60, 0, 0), 'chest': (0, 35, 0), 'waist': (0, 12, 0), 'head': (0, -20, 0)}),
    (0.5, {'right_arm': (-95, 40, -10), 'right_fore': (0, 0, 0), 'chest': (8, -35, 0), 'waist': (0, -12, 0), 'head': (0, 15, 0), 'left_arm': (20, 0, 0)}),
    (0.75, {'right_arm': (-80, 30, 0), 'right_fore': (-10, 0, 0), 'chest': (6, -25, 0), 'waist': (0, -8, 0)}),
    (1.2, {}),
], LIMBS)
for k in range(3):
    whip.scl(f'athorn_right{k}', (0, (1, 1, 1)), (0.45, (1, 1, 1)), (0.55, (1.6, 3.5, 1.6)), (0.8, (1.3, 2.0, 1.3)), (1.2, (1, 1, 1)))
tail_wave(whip, 1.2, 1.3)
A['WHIP'] = whip

heart = Anim('heart', 1.8)
pose_seq(heart, [
    (0.0, {}),
    (0.4, sym({'right_arm': (-50, 30, -20), 'right_fore': (-100, 0, 0)}) | {'head': (20, 0, 0), 'chest': (15, 0, 0)}),
    (0.6, sym({'right_arm': (-70, -20, 60), 'right_fore': (-10, 0, 0)}) | {'head': (-25, 0, 0), 'chest': (-20, 0, 0)}),
    (1.4, sym({'right_arm': (-75, -20, 65), 'right_fore': (-10, 0, 0)}) | {'head': (-25, 0, 0), 'chest': (-22, 0, 0)}),
    (1.8, {}),
], LIMBS)
heart.pos('base', (0, (0, 0, 0)), (0.4, (0, -2, 0)), (0.6, (0, 3, 1)), (1.4, (0, 3, 1)), (1.8, (0, 0, 0)))
heart.scl('chest', (0, (1, 1, 1)), (0.4, (0.95, 0.95, 0.95)), (0.6, (1.12, 1.12, 1.12)), (0.75, (1.04, 1.04, 1.04)), (1.8, (1, 1, 1)))
tail_wave(heart, 1.8, 1.0, speed=2)
thorn_flicker(heart, 1.8, 3)
A['HEART'] = heart

dash = Anim('dash', 1.0)
pose_seq(dash, [
    (0.0, {}),
    (0.25, sym({'right_arm': (40, 0, 20), 'right_leg': (-50, 0, 0), 'right_shin': (70, 0, 0)}) | {'base': (30, 0, 0), 'head': (-25, 0, 0), 'left_leg': (-20, 0, 0), 'left_shin': (40, 0, 0)}),
    (0.35, sym({'right_arm': (70, 0, 15), 'right_fore': (-20, 0, 0)}) | {'base': (45, 0, 0), 'head': (-35, 0, 0), 'right_leg': (30, 0, 0), 'left_leg': (-50, 0, 0), 'left_shin': (20, 0, 0)}),
    (0.6, sym({'right_arm': (60, 0, 15)}) | {'base': (40, 0, 0), 'head': (-30, 0, 0), 'right_leg': (35, 0, 0), 'left_leg': (-45, 0, 0)}),
    (1.0, {}),
], LIMBS + ['base'])
dash.pos('base', (0, (0, 0, 0)), (0.25, (0, -6, 2)), (0.35, (0, -4, -2)), (0.6, (0, -4, -2)), (1.0, (0, 0, 0)))
tail_wave(dash, 1.0, 0.5, lift=-35)
thorn_flicker(dash, 1.0, 3)
A['DASH'] = dash

cage = Anim('cage', 1.5)
pose_seq(cage, [
    (0.0, {}),
    (0.4, {'right_arm': (-160, 0, 20), 'right_fore': (-20, 0, 0), 'right_hand': (0, 0, 0), 'head': (-20, 0, 0), 'chest': (-10, 0, 0)}),
    (0.6, {'right_arm': (-40, 0, 5), 'right_fore': (-30, 0, 0), 'head': (20, 0, 0), 'chest': (25, 0, 0), 'waist': (10, 0, 0),
           'right_leg': (-45, 0, 0), 'right_shin': (80, 0, 0), 'left_leg': (-5, 0, 0), 'left_shin': (60, 0, 0)}),
    (1.1, {'right_arm': (-40, 0, 5), 'right_fore': (-30, 0, 0), 'head': (20, 0, 0), 'chest': (25, 0, 0), 'waist': (10, 0, 0),
           'right_leg': (-45, 0, 0), 'right_shin': (80, 0, 0), 'left_leg': (-5, 0, 0), 'left_shin': (60, 0, 0)}),
    (1.5, {}),
], LIMBS)
cage.pos('base', (0, (0, 0, 0)), (0.4, (0, 3, 0)), (0.6, (0, -10, 0)), (1.1, (0, -10, 0)), (1.5, (0, 0, 0)))
tail_wave(cage, 1.5, 0.8)
A['CAGE'] = cage

spin = Anim('spin', 1.6)
pose_seq(spin, [(0.0, {}), (0.3, sym({'right_arm': (0, 0, 80), 'right_fore': (-20, 0, 0)}) | {'base': (0, 0, 0)}),
                (1.3, sym({'right_arm': (0, 0, 85), 'right_fore': (-20, 0, 0)})), (1.6, {})], LIMBS)
spin.rot('base', (0, (0, 0, 0)), (0.3, (0, -90, 0)), (1.3, (0, -1170, 0)), (1.6, (0, -1260, 0)), interp='L')
spin.pos('base', (0, (0, 0, 0)), (0.3, (0, 4, 0)), (1.3, (0, 5, 0)), (1.6, (0, 0, 0)))
for i in range(5):
    spin.rot(f'tail{i}', (0, (0, 0, 0)), (0.3, (-40 if i == 0 else -12, 0, 0)), (1.3, (-40 if i == 0 else -12, 0, 0)), (1.6, (0, 0, 0)))
thorn_flicker(spin, 1.6, 3)
A['SPIN'] = spin

death = Anim('death', 3.0)
pose_seq(death, [
    (0.0, {}),
    (0.4, sym({'right_arm': (-30, 0, 30)}) | {'head': (-40, 0, 0), 'chest': (-20, 0, 0)}),
    (1.2, sym({'right_arm': (10, 0, 20), 'right_fore': (-20, 0, 0)}) | {'head': (30, 0, 0), 'chest': (30, 0, 0),
                                                                      'right_leg': (-90, 0, 0), 'left_leg': (-70, 0, 0), 'right_shin': (100, 0, 0), 'left_shin': (110, 0, 0)}),
    (3.0, sym({'right_arm': (15, 0, 20), 'right_fore': (-20, 0, 0)}) | {'head': (50, 0, 0), 'chest': (40, 0, 0),
                                                                      'right_leg': (-90, 0, 0), 'left_leg': (-70, 0, 0), 'right_shin': (100, 0, 0), 'left_shin': (110, 0, 0)}),
], LIMBS)
death.pos('base', (0, (0, 0, 0)), (0.4, (0, 2, 0)), (1.2, (0, -24, 0)), (3.0, (0, -26, 0)))
for n in THORNS:
    death.scl(n, (0, (1, 1, 1)), (1.5, (1, 1, 1)), (3.0, (0.01, 0.01, 0.01)))
A['DEATH'] = death


# ================= player "boss form" animations =================
BODY = LIMBS + ['base']

jump = Anim('jump', 0.8, True)
jp = lambda s: sym({'right_arm': (-35 + 6 * s, 0, 38), 'right_fore': (-35, 0, 0)}) | {
    'right_leg': (-55, 0, 0), 'right_shin': (75 + 5 * s, 0, 0), 'left_leg': (-18, 0, 0), 'left_shin': (45 - 5 * s, 0, 0),
    'chest': (8, 0, 0), 'head': (-10, 0, 0)}
add_rot(jump, [(0, jp(1)), (0.4, jp(-1)), (0.8, jp(1))])
tail_wave(jump, 0.8, 0.6, speed=1, lift=-30)
thorn_flicker(jump, 0.8, 2)
A['JUMP'] = jump

crouch = Anim('crouch', 1.6, True)
cp = lambda s: sym({'right_arm': (-25, 0, 18 + s * 3), 'right_fore': (-45, 0, 0), 'right_leg': (-50, 0, 0), 'right_shin': (70, 0, 0)}) | {
    'chest': (22, 0, 0), 'waist': (10, 0, 0), 'head': (-24, 0, 0)}
add_rot(crouch, [(0, cp(1)), (0.8, cp(-1)), (1.6, cp(1))])
crouch.pos('base', (0, (0, -5, 0)), (1.6, (0, -5, 0)))
A['CROUCH'] = crouch

flip = Anim('flip', 0.55)
pose_seq(flip, [
    (0.0, {}),
    (0.12, sym({'right_arm': (-60, 0, 30), 'right_fore': (-70, 0, 0), 'right_leg': (-90, 0, 0), 'right_shin': (110, 0, 0)}) | {'head': (30, 0, 0), 'chest': (25, 0, 0)}),
    (0.4, sym({'right_arm': (-60, 0, 30), 'right_fore': (-70, 0, 0), 'right_leg': (-90, 0, 0), 'right_shin': (110, 0, 0)}) | {'head': (30, 0, 0), 'chest': (25, 0, 0)}),
    (0.55, {}),
], LIMBS)
flip.rot('base', (0, (0, 0, 0)), (0.45, (-360, 0, 0)), (0.451, (0, 0, 0)), (0.55, (0, 0, 0)), interp='L')
tail_wave(flip, 0.55, 1.4, speed=1)
A['FLIP'] = flip


def slash(side):
    s = 1 if side == 'right' else -1
    o = 'left' if side == 'right' else 'right'
    a = Anim(f'slash_{side}', 0.4)
    pose_seq(a, [
        (0.0, {}),
        (0.08, {f'{side}_arm': (-150, 0, 55 if s > 0 else -55), f'{side}_fore': (-50, 0, 0), 'chest': (-6, 30 * s, 0), 'waist': (0, 10 * s, 0), f'{o}_arm': (20, 0, 0)}),
        (0.18, {f'{side}_arm': (-40, -30 * s, -40 * s), f'{side}_fore': (-5, 0, 0), 'chest': (10, -30 * s, 0), 'waist': (0, -12 * s, 0), f'{o}_arm': (-25, 0, 0), 'head': (0, 12 * s, 0)}),
        (0.4, {}),
    ], LIMBS)
    for k in range(3):
        a.scl(f'athorn_{side}{k}', (0, (1, 1, 1)), (0.1, (1.2, 1.8, 1.2)), (0.2, (1.5, 2.6, 1.5)), (0.4, (1, 1, 1)))
    return a


A['SLASH_R'] = slash('right')
A['SLASH_L'] = slash('left')

tr = Anim('transform', 1.8)
fetal = sym({'right_arm': (-70, 30, -30), 'right_fore': (-110, 0, 0), 'right_leg': (-95, 0, 0), 'right_shin': (130, 0, 0)}) | {
    'head': (45, 0, 0), 'chest': (40, 0, 0), 'waist': (20, 0, 0)}
roar = sym({'right_arm': (-30, 0, 85), 'right_fore': (-30, 0, 0), 'right_leg': (-10, 0, 8), 'right_shin': (15, 0, 0)}) | {
    'head': (-40, 0, 0), 'chest': (-22, 0, 0), 'waist': (-8, 0, 0)}
pose_seq(tr, [(0.0, fetal), (0.55, fetal), (0.8, roar), (1.35, roar), (1.8, {})], LIMBS)
tr.pos('base', (0, (0, -30, 0)), (0.55, (0, -30, 0)), (0.8, (0, 4, 0)), (1.35, (0, 2, 0)), (1.8, (0, 0, 0)))
tr.scl('base', (0, (0.55, 0.55, 0.55)), (0.55, (0.62, 0.62, 0.62)), (0.8, (1.12, 1.12, 1.12)), (1.0, (1.0, 1.0, 1.0)), (1.8, (1, 1, 1)))
for n in THORNS:
    tr.scl(n, (0, (0.01, 0.01, 0.01)), (0.55, (0.05, 0.05, 0.05)), (0.75, (1.4, 1.6, 1.4)), (1.0, (1, 1, 1)), (1.8, (1, 1, 1)))
for i in range(5):
    tr.rot(f'tail{i}', (0, (60, 40, 0)), (0.55, (60, 40, 0)), (0.8, (-30, 0, 20 * (1 if i % 2 else -1))), (1.35, (-10, 0, 0)), (1.8, (0, 0, 0)))
thorn_flicker(tr, 1.8, 4)
A['TRANSFORM'] = tr

ur = Anim('ult_rise', 1.4)
up = sym({'right_arm': (-165, 0, 25), 'right_fore': (-25, 0, 0), 'right_leg': (-60, 0, 0), 'right_shin': (95, 0, 0)}) | {'head': (-25, 0, 0), 'chest': (-18, 0, 0)}
pose_seq(ur, [(0.0, {}), (0.15, sym({'right_arm': (30, 0, 20), 'right_leg': (-55, 0, 0), 'right_shin': (90, 0, 0)}) | {'chest': (35, 0, 0), 'head': (20, 0, 0)}),
              (0.35, up), (1.4, up)], LIMBS)
ur.pos('base', (0, (0, 0, 0)), (0.15, (0, -9, 0)), (0.35, (0, 0, 0)), (1.4, (0, 0, 0)))
tail_wave(ur, 1.4, 1.2, speed=2, lift=-40)
thorn_flicker(ur, 1.4, 4)
A['ULT_RISE'] = ur

us = Anim('ult_slam', 1.0)
slam = sym({'right_arm': (-70, 0, 20), 'right_fore': (-20, 0, 0), 'right_leg': (-70, 0, 20), 'right_shin': (100, 0, 0)}) | {'head': (30, 0, 0), 'chest': (45, 0, 0), 'waist': (15, 0, 0)}
pose_seq(us, [(0.0, up), (0.08, slam), (0.6, slam), (1.0, {})], LIMBS)
us.pos('base', (0, (0, 0, 0)), (0.08, (0, -14, 0)), (0.6, (0, -13, 0)), (1.0, (0, 0, 0)))
for n in THORNS:
    us.scl(n, (0, (1, 1, 1)), (0.1, (1.8, 2.4, 1.8)), (0.5, (1.3, 1.5, 1.3)), (1.0, (1, 1, 1)))
tail_wave(us, 1.0, 1.4, speed=1)
A['ULT_SLAM'] = us

tempest = Anim('tempest', 1.6)
pose_seq(tempest, [(0.0, {}), (0.3, sym({'right_arm': (0, 0, 80), 'right_fore': (-20, 0, 0)})),
                   (1.3, sym({'right_arm': (0, 0, 85), 'right_fore': (-20, 0, 0)})), (1.6, {})], LIMBS)
tempest.rot('base', (0, (0, 0, 0)), (0.3, (0, -90, 0)), (1.3, (0, -990, 0)), (1.6, (0, -1080, 0)), interp='L')
tempest.pos('base', (0, (0, 0, 0)), (0.3, (0, 4, 0)), (1.3, (0, 5, 0)), (1.6, (0, 0, 0)))
for i in range(5):
    tempest.rot(f'tail{i}', (0, (0, 0, 0)), (0.3, (-40 if i == 0 else -12, 0, 0)), (1.3, (-40 if i == 0 else -12, 0, 0)), (1.6, (0, 0, 0)))
thorn_flicker(tempest, 1.6, 3)
A['TEMPEST'] = tempest
