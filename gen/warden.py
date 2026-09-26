"""Mühür Bekçisi (Seal Warden) — model, textures, animations."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, border, glow_img, draw_line, cracks

M = Model('seal_warden', 256, 256)


def mirror_bone(src, dst_parent_map):
    """mirror right_* bones to left_* (x -> -x)."""
    pass


def cube_rel(bone, lfrom, size, mat, **kw):
    b = M.bones[bone]
    return M.cube(bone, [b.pivot[i] + lfrom[i] for i in range(3)], size, mat, **kw)


# ---------------- skeleton ----------------
M.bone('base', None, (0, -38, 0))
M.bone('hips', 'base', (0, -38, 0))
M.cube('hips', (-7, -42, -4), (14, 7, 8), 'plate')
M.cube('hips', (-7.5, -43, -4.5), (15, 3, 9), 'band')
# loin cloth (double sided planes)
M.bone('cloth_f', 'hips', (0, -41, -4.8))
M.cube('cloth_f', (-8.5, -41, -4.8), (17, 28, 0), 'cloth', plane=True)
M.bone('cloth_b', 'hips', (0, -41, 4.8))
M.cube('cloth_b', (-8.5, -41, 4.8), (17, 25, 0), 'cloth', plane=True)
M.bone('cloth_r', 'hips', (-7.8, -41, 0), (0, 90, 0))
cube_rel('cloth_r', (-6, 0, 0), (12, 36, 0), 'cloth_long', plane=True)
M.bone('cloth_l', 'hips', (7.8, -41, 0), (0, -90, 0))
cube_rel('cloth_l', (-6, 0, 0), (12, 22, 0), 'cloth', plane=True)

M.bone('waist', 'hips', (0, -42, 0))
M.cube('waist', (-6, -53, -3.5), (12, 11, 7), 'plate_abs')
M.bone('chest', 'waist', (0, -53, 0))
M.cube('chest', (-10, -69, -5), (20, 16, 10), 'plate_chest')
M.cube('chest', (0.2, -68, -5.8), (9.2, 8, 1), 'plate_pec')
M.cube('chest', (-9.4, -68, -5.8), (9.2, 8, 1), 'plate_pec')
M.cube('chest', (-3.5, -62.8, -6.5), (7, 7, 1), 'emblem')
M.cube('chest', (-5.5, -69.6, -6.0), (11, 3, 1), 'collar')
M.cube('chest', (-8.5, -71.5, -3.5), (17, 3, 7), 'plate')
M.cube('chest', (-7, -60, 4.6), (14, 12, 1.2), 'plate_dark')   # back plate

M.bone('neck', 'chest', (0, -70, 0.5))
M.cube('neck', (-2.5, -75.5, -2), (5, 6.5, 4), 'plate_dark')
M.bone('head', 'neck', (0, -74.5, 0.5))
M.cube('head', (-4.5, -86.5, -4.5), (9, 13, 8), 'helm')
M.cube('head', (-4, -85.5, -5.3), (8, 11.5, 1), 'helmface')
M.cube('head', (-3.5, -75.5, -4.8), (7, 2.5, 6), 'plate_dark')
M.cube('head', (-5.3, -83, -3.5), (1, 7, 6), 'helm')
M.cube('head', (4.3, -83, -3.5), (1, 7, 6), 'helm')
M.cube('head', (-4.6, -82.6, -6.0), (9.2, 1.4, 1.1), 'helm_dark')

M.bone('crown', 'head', (0, -86.5, 0))
M.cube('crown', (-3.5, -88.5, -3.5), (7, 2.5, 7), 'crystal_base')
CRYSTALS = [
    ('c_main', (0, -88, 0), (0, 45, 0), 3.4, 17),
    ('c_l1', (-1.6, -88, 0.3), (0, 30, -18), 2.5, 12),
    ('c_r1', (1.6, -88, 0.3), (0, -30, 18), 2.5, 12),
    ('c_l2', (-2.9, -87.8, 0.6), (5, 10, -38), 1.9, 8),
    ('c_r2', (2.9, -87.8, 0.6), (5, -10, 38), 1.9, 8),
    ('c_f', (0, -88, -2.2), (26, 20, 6), 1.8, 8),
    ('c_b', (0, -88, 2.2), (-24, 10, -8), 2.2, 10),
]
for name, pv, rot, s, L in CRYSTALS:
    M.bone(name, 'crown', pv, rot)
    cube_rel(name, (-s / 2, -L, -s / 2), (s, L, s), 'crystal')
    t = s * 0.55
    cube_rel(name, (-t / 2, -L - L * 0.22, -t / 2), (t, L * 0.22, t), 'crystal_tip')


def arm(side):
    sx = -1 if side == 'right' else 1
    def X(x, w=0):  # mirror helper for "from" coords written for the right side
        return x if sx == -1 else -(x + w)
    a = f'{side}_arm'
    M.bone(a, 'chest', (sx * 11, -66, 0))
    M.cube(a, (X(-16.8, 7.2), -70, -4), (7.2, 8.5, 8), 'plate_delt')
    M.cube(a, (X(-15.2, 5.6), -63, -2.9), (5.6, 16.5, 5.6), 'plate_arm')
    f = f'{side}_fore'
    M.bone(f, a, (sx * 12.4, -47.5, 0))
    M.cube(f, (X(-14.8, 4.8), -48, -2.4), (4.8, 15.5, 4.8), 'plate_arm')
    M.cube(f, (X(-15.4, 1), -45.5, -1.6), (1, 11, 3.2), 'plate_dark')
    h = f'{side}_hand'
    M.bone(h, f, (sx * 12.4, -33, 0))
    M.cube(h, (X(-15.2, 5.8), -33, -1.3), (5.8, 6.2, 2.6), 'plate_hand')
    g = f'{side}_fingers'
    M.bone(g, h, (sx * 12.4, -27, 0))
    for fx, L in ((-15.1, 7.5), (-13.65, 9), (-12.2, 8.5), (-10.75, 7)):
        M.cube(g, (X(fx, 1.25), -27, -1.0), (1.25, L, 1.5), 'plate_finger')
    t = f'{side}_thumb'
    M.bone(t, h, (sx * 9.8, -30, -0.4), (0, 0, sx * 38))
    cube_rel(t, (-0.65, 0, -0.75), (1.3, 6.5, 1.5), 'plate_finger')


arm('right')
arm('left')

# floating seal orbs + triangle beams
M.bone('orbs', 'chest', (0, -60, 6))
ORB = {'orb_top': (0, -98.5), 'orb_r': (-29.5, -51.5), 'orb_l': (29.5, -51.5)}
for n, (x, y) in ORB.items():
    M.bone(n, 'orbs', (x, y, 7))
    cube_rel(n, (-5, -5, -0.8), (10, 10, 1.6), 'orb')
M.bone('seal', 'orbs', (0, -60, 7))


def beam(name, a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    L = math.hypot(dx, dy)
    ang = math.degrees(math.atan2(dy, dx))
    M.bone(name, 'seal', (a[0], a[1], 7), (0, 0, ang))
    cube_rel(name, (4.5, -0.6, -0.6), (L - 9, 1.2, 1.2), 'beam')


beam('beam_bottom', ORB['orb_r'], ORB['orb_l'])
beam('beam_right', ORB['orb_r'], ORB['orb_top'])
beam('beam_left', ORB['orb_top'], ORB['orb_l'])


def leg(side):
    sx = -1 if side == 'right' else 1
    def X(x, w):
        return x if sx == -1 else -(x + w)
    l = f'{side}_leg'
    M.bone(l, 'hips', (sx * 4, -38, 0))
    M.cube(l, (X(-7, 6), -39.5, -3), (6, 30.5, 6), 'plate_leg')
    M.cube(l, (X(-6.6, 5.2), -12.5, -3.7), (5.2, 4.2, 1.2), 'plate_dark')
    s = f'{side}_shin'
    M.bone(s, l, (sx * 4, -10, 0))
    M.cube(s, (X(-6.3, 4.6), -10, -2.4), (4.6, 30, 4.6), 'plate_shin')
    M.cube(s, (X(-5.9, 3.8), -6, -2.9), (3.8, 16, 0.8), 'plate_dark')
    ft = f'{side}_foot'
    M.bone(ft, s, (sx * 4, 20, 0))
    M.cube(ft, (X(-5.9, 3.8), 19.5, -5.5), (3.8, 4.5, 7.5), 'plate_foot')
    M.cube(ft, (X(-5.3, 2.6), 21.8, -8.2), (2.6, 2.2, 2.8), 'plate_foot')


leg('right')
leg('left')

# ---------------- textures ----------------
P = Painter(M)
INK = (48, 16, 30)
WOUND = (132, 70, 68)
BLOOD = (84, 16, 34)

PLATES = {
    'plate': dict(hi=(214, 190, 172), lo=(138, 104, 106), wounds=0.3, seams=7),
    'plate_dark': dict(hi=(150, 112, 112), lo=(88, 56, 66), wounds=0.3, seams=4),
    'plate_abs': dict(hi=(205, 180, 165), lo=(125, 92, 98), wounds=0.4, seams=3, abs=True),
    'plate_chest': dict(hi=(218, 196, 178), lo=(142, 108, 110), wounds=0.45, seams=8),
    'plate_pec': dict(hi=(225, 204, 186), lo=(150, 116, 116), wounds=0.45, seams=0),
    'plate_delt': dict(hi=(220, 196, 178), lo=(140, 104, 106), wounds=0.6, seams=4),
    'plate_arm': dict(hi=(212, 186, 168), lo=(132, 98, 102), wounds=0.55, seams=6),
    'plate_hand': dict(hi=(205, 180, 162), lo=(128, 94, 98), wounds=0.5, seams=0),
    'plate_finger': dict(hi=(190, 164, 150), lo=(112, 80, 88), wounds=0.4, seams=3),
    'plate_leg': dict(hi=(214, 188, 170), lo=(134, 100, 104), wounds=0.45, seams=9),
    'plate_shin': dict(hi=(210, 184, 166), lo=(128, 95, 100), wounds=0.6, seams=7),
    'plate_foot': dict(hi=(200, 174, 158), lo=(120, 88, 94), wounds=0.5, seams=0),
    'helm': dict(hi=(208, 184, 176), lo=(128, 96, 108), wounds=0.3, seams=0),
    'helm_dark': dict(hi=(120, 80, 96), lo=(70, 34, 54), wounds=0, seams=0),
}


BRIGHT = 1.22


def plate_paint(p, face, W, H, r):
    p = dict(p, hi=tuple(min(255, c * BRIGHT) for c in p['hi']), lo=tuple(min(255, c * BRIGHT) for c in p['lo']))
    n = noise2(H, W, r, 4)
    yy, xx = np.mgrid[0:H, 0:W]
    grad = 0.75 - 0.35 * (yy / max(H - 1, 1)) + 0.15 * (1 - xx / max(W - 1, 1))
    if face == 'top':
        grad = grad * 0 + 0.95
    if face == 'bottom':
        grad = grad * 0 + 0.25
    t = np.clip(grad * 0.8 + (n - 0.5) * 0.5, 0, 1)
    col = lerp(p['lo'], p['hi'], t)
    # plate seams (horizontal on tall faces)
    if p.get('seams') and H > 6 and face not in ('top', 'bottom'):
        k = max(1, H // p['seams'] if p['seams'] < H else 1)
        pos = sorted(set(int(v) for v in np.linspace(3, H - 3, max(1, H // 7)) + r.integers(-1, 2, max(1, H // 7))))
        for y in pos:
            if 1 < y < H - 1:
                col[y] = lerp(col[y], INK, 0.75)
                if y + 1 < H - 1:
                    col[y + 1] = lerp(col[y + 1], p['hi'], 0.35)
        # vertical split on wide faces
        if W > 7 and r.random() < 0.6:
            x = int(W * (0.3 + r.random() * 0.4))
            col[:, x] = lerp(col[:, x], INK, 0.6)
    if p.get('abs') and face == 'north':
        for y in range(2, H - 1, 3):
            col[y, 1:W - 1] = lerp(col[y, 1:W - 1], INK, 0.7)
        col[:, W // 2] = lerp(col[:, W // 2], INK, 0.7)
    # wounds / rust patches with blood drips
    nw = int(round(p['wounds'] * (W * H) / 60.0 + r.random() * p['wounds']))
    for _ in range(nw):
        cx, cy = r.random() * W, r.random() * H
        rx, ry = 0.8 + r.random() * 1.8, 0.8 + r.random() * 2.0
        d = ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2
        col[d < 1.9] = lerp(col[d < 1.9], WOUND, 0.8)
        col[d < 0.8] = lerp(col[d < 0.8], BLOOD, 0.85)
        if face not in ('top', 'bottom') and r.random() < 0.6:
            L = int(2 + r.random() * 5)
            x = int(cx)
            for y in range(int(cy), min(H, int(cy) + L)):
                if 0 <= x < W:
                    col[y, x] = lerp(col[y, x], BLOOD, 0.8)
    b = border(H, W)
    col[b] = lerp(col[b], INK, 0.85)
    return col, np.ones((H, W)), None


for k, p in PLATES.items():
    P.mats[k] = (lambda p: (lambda c, face, W, H, r: plate_paint(p, face, W, H, r)))(p)


@P.mat('helmface')
def helmface(c, face, W, H, r):
    col, a, _ = plate_paint(PLATES['helm'], face, W, H, r)
    if face != 'north':
        return col, a, None
    yy, xx = np.mgrid[0:H, 0:W]
    col[:, W // 2 - 0] = lerp(col[:, W // 2], INK, 0.5)
    # shadowed mask area below eye
    col[int(H * 0.72):] = lerp(col[int(H * 0.72):], (90, 40, 60), 0.5)
    # eye
    ex, ey = (W - 1) / 2, H * 0.62
    d = np.hypot(xx - ex, (yy - ey) * 1.15)
    col[d < 2.4] = INK
    col[d < 1.7] = (255, 110, 205)
    col[d < 0.8] = (255, 236, 252)
    g = glow_img(H, W, (255, 120, 210), (d < 1.7) * 1.0)
    g[d < 0.8, :3] = (255, 240, 252)
    return col, a, g


@P.mat('band')
def band(c, face, W, H, r):
    n = noise2(H, W, r, 3)
    col = lerp((52, 18, 34), (92, 42, 62), n)
    col[H // 2] = lerp(col[H // 2], (120, 70, 90), 0.4)
    b = border(H, W)
    col[b] = lerp(col[b], (25, 5, 15), 0.8)
    return col, np.ones((H, W)), None


def cloth_paint(c, face, W, H, r, cut=0.33):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 3)
    folds = 0.5 + 0.5 * np.sin(xx * 1.3 + n * 4 + r.random() * 6)
    t = np.clip(0.35 + 0.35 * folds + (n - 0.5) * 0.4 - 0.25 * (yy < 3), 0, 1)
    col = lerp((70, 22, 50), (168, 96, 136), t)
    alpha = np.ones((H, W))
    if face in ('north', 'south'):
        # ragged torn bottom edge
        depth = np.zeros(W)
        v = r.random() * cut * H
        for x in range(W):
            v += r.normal(0, H * 0.09)
            v = np.clip(v, 0, cut * H * 1.3)
            depth[x] = v
        for x in range(W):
            edge = H - int(depth[x])
            alpha[edge:, x] = 0
            for k in range(2):
                if 0 <= edge - 1 - k < H:
                    col[edge - 1 - k, x] = lerp(col[edge - 1 - k, x], (40, 8, 26), 0.7 - 0.3 * k)
        # holes
        for _ in range(int(1 + r.random() * 2)):
            cx, cy = r.random() * W, H * (0.35 + r.random() * 0.4)
            rx, ry = 0.8 + r.random() * 1.6, 1 + r.random() * 2
            d = ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2
            alpha[d < 1] = 0
            ring = (d >= 1) & (d < 2.2)
            col[ring] = lerp(col[ring], (40, 8, 26), 0.7)
        # blood stains near top
        for _ in range(int(r.random() * 3)):
            cx, cy = r.random() * W, r.random() * H * 0.4
            d = np.hypot(xx - cx, yy - cy)
            col[d < 1.6] = lerp(col[d < 1.6], (70, 0, 20), 0.6)
        col[0:2] = lerp(col[0:2], (40, 8, 26), 0.6)
    return col, alpha, None


P.mats['cloth'] = cloth_paint
P.mats['cloth_long'] = lambda c, face, W, H, r: cloth_paint(c, face, W, H, r, cut=0.55)


def crystal_paint(c, face, W, H, r, tip=False):
    yy, xx = np.mgrid[0:H, 0:W]
    t = 0.35 + 0.65 * (1 - yy / max(H - 1, 1)) if not tip else np.full((H, W), 0.95)
    facet = ((xx + yy * 0.5 + r.integers(0, 4)) % 5 < 1.2)
    col = lerp((150, 58, 150), (255, 214, 250), np.clip(t, 0, 1))
    col[facet] = lerp(col[facet], (255, 245, 255), 0.45)
    col[:, :1] = lerp(col[:, :1], (120, 40, 120), 0.6)
    g = np.zeros((H, W, 4), np.float32)
    g[..., :3] = col
    g[..., 3] = np.clip(t * 200, 60, 210)
    return col, np.ones((H, W)), g


P.mats['crystal'] = crystal_paint
P.mats['crystal_tip'] = lambda c, face, W, H, r: crystal_paint(c, face, W, H, r, True)


@P.mat('crystal_base')
def crystal_base(c, face, W, H, r):
    n = noise2(H, W, r, 2)
    col = lerp((70, 24, 60), (150, 70, 140), n)
    b = border(H, W)
    col[b] = (40, 10, 30)
    return col, np.ones((H, W)), None


@P.mat('emblem')
def emblem(c, face, W, H, r):
    if face not in ('north',):
        col = np.zeros((H, W, 3)) + (80, 30, 60)
        return col, np.ones((H, W)), None
    S = 4
    yy, xx = np.mgrid[0:H * S, 0:W * S] / S
    cx, cy = W / 2, H / 2
    d = np.hypot(xx - cx + 0.5 / S, yy - cy + 0.5 / S)
    ang = np.arctan2(yy - cy, xx - cx)
    rr = W / 2
    star_r = rr * (0.32 + 0.3 * (0.5 + 0.5 * np.cos(6 * ang + math.pi / 2)) ** 3)
    col = np.zeros((H * S, W * S, 3)) + (95, 38, 72)
    col[d > rr * 0.86] = (60, 14, 40)
    col[(d > rr * 0.7) & (d <= rr * 0.86)] = (230, 150, 215)
    star = d < star_r
    col[star] = (250, 190, 240)
    col[d < rr * 0.14] = (255, 245, 255)
    alpha = (d < rr).astype(float)
    glowm = ((d > rr * 0.7) & (d <= rr * 0.86)) | star
    # downsample
    col = col.reshape(H, S, W, S, 3).mean((1, 3))
    alpha = (alpha.reshape(H, S, W, S).mean((1, 3)) > 0.4).astype(float)
    gm = glowm.reshape(H, S, W, S).mean((1, 3))
    return col, alpha, glow_img(H, W, (255, 160, 230), gm * 0.95)


@P.mat('collar')
def collar(c, face, W, H, r):
    col, a, _ = plate_paint(PLATES['plate_dark'], face, W, H, r)
    if face != 'north':
        return col, a, None
    m = np.zeros((H, W))
    draw_line(m, 0, 0, (W - 1) / 2, H - 1)
    draw_line(m, W - 1, 0, (W - 1) / 2, H - 1)
    col[m > 0] = (245, 120, 210)
    return col, a, glow_img(H, W, (255, 130, 220), m)


@P.mat('orb')
def orb(c, face, W, H, r):
    if face not in ('north', 'south'):
        col = np.zeros((H, W, 3)) + (60, 20, 45)
        return col, np.ones((H, W)), glow_img(H, W, (255, 110, 200), np.full((H, W), 0.35))
    S = 4
    yy, xx = np.mgrid[0:H * S, 0:W * S] / S
    cx, cy = W / 2, H / 2
    d = np.hypot(xx - cx + 0.5 / S, yy - cy + 0.5 / S) / (W / 2)
    col = np.zeros((H * S, W * S, 3))
    gm = np.zeros((H * S, W * S))
    col[:] = (70, 24, 50)                       # dark body
    ring = (d > 0.72) & (d <= 1.0)
    col[ring] = (255, 150, 220); gm[ring] = 0.9  # glowing outer ring
    col[(d > 0.62) & (d <= 0.72)] = (60, 14, 40)
    col[d <= 0.62] = (100, 40, 80)
    iris = d <= 0.42
    col[iris] = (255, 200, 245); gm[iris] = 1
    col[d <= 0.2] = (150, 30, 100); gm[d <= 0.2] = 0.6
    col[d <= 0.08] = (255, 255, 255)
    alpha = (d <= 1.0).astype(float)
    col = col.reshape(H, S, W, S, 3).mean((1, 3))
    alpha = (alpha.reshape(H, S, W, S).mean((1, 3)) > 0.45).astype(float)
    gm = gm.reshape(H, S, W, S).mean((1, 3))
    return col, alpha, glow_img(H, W, (255, 150, 225), gm)


@P.mat('beam')
def beam_m(c, face, W, H, r):
    col = np.zeros((H, W, 3)) + (255, 170, 230)
    return col, np.ones((H, W)), glow_img(H, W, (255, 150, 225), np.full((H, W), 0.85))


# ---------------- animations ----------------
A = {}
# neutral-to-casting pose used by idle (matches the artwork: hands raised, palms forward)
CAST_R = dict(arm=(-20, 0, 72), fore=(-15, 0, -118), hand=(0, 0, -20), fingers=(-8, 0, 0))


def arms_pose(a, t, rarm, rfore, rhand, rfing, interp='C'):
    L = lambda v: (v[0], -v[1], -v[2])
    a.ch  # noqa
    return [(f'right_arm', rarm), (f'right_fore', rfore), (f'right_hand', rhand), (f'right_fingers', rfing),
            (f'left_arm', L(rarm)), (f'left_fore', L(rfore)), (f'left_hand', L(rhand)), (f'left_fingers', L(rfing))]


def pose_keys(frames):
    """frames: list of (t, dict bone->rot). returns dict bone->[(t, rot)]"""
    out = {}
    for t, d in frames:
        for b, v in d.items():
            out.setdefault(b, []).append((t, v))
    return out


def sym(d):
    """add mirrored left_* from right_* rotation entries"""
    o = dict(d)
    for k, v in d.items():
        if k.startswith('right_'):
            o['left_' + k[6:]] = (v[0], -v[1], -v[2])
    return o


def add_rot(a, frames, interp='C'):
    for b, keys in pose_keys(frames).items():
        a.rot(b, *[(t, v, interp) for t, v in keys])


# idle: casting stance, orbs spin/bob, breathing
idle = Anim('idle', 4.0, True)
cast = sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98), 'right_hand': (10, 0, 8),
            'right_fingers': (-6, 0, 0), 'right_thumb': (0, 0, 0)})
cast2 = sym({'right_arm': (-28, 0, 82), 'right_fore': (-24, 0, 100), 'right_hand': (14, 0, 10),
             'right_fingers': (-14, 0, 0)})
add_rot(idle, [(0, cast), (2, cast2), (4, cast)])
idle.rot('chest', (0, (0, 0, 0)), (2, (-3, 0, 0)), (4, (0, 0, 0)))
idle.rot('head', (0, (0, 0, 0)), (1, (2, 3, 0)), (2, (4, 0, 0)), (3, (2, -3, 0)), (4, (0, 0, 0)))
idle.pos('base', (0, (0, 0, 0)), (2, (0, 1.2, 0)), (4, (0, 0, 0)))
idle.pos('orbs', (0, (0, 0, 0)), (1, (0, 1.5, 0)), (2, (0, 0, 0)), (3, (0, -1.5, 0)), (4, (0, 0, 0)))
for i, n in enumerate(('orb_top', 'orb_r', 'orb_l')):
    idle.rot(n, (0, (0, 0, 0)), (4, (0, 0, 360)), interp='L')
for _b in ('beam_bottom', 'beam_right', 'beam_left'):
    idle.scl(_b, (0, (1, 1, 1)), (1, (1, 1.35, 1.35)), (2, (1, 1, 1)), (3, (1, 1.35, 1.35)), (4, (1, 1, 1)))
for n in ('cloth_f', 'cloth_b'):
    idle.rot(n, (0, (0, 0, 0)), (2, (4 if n == 'cloth_f' else -4, 0, 0)), (4, (0, 0, 0)))
idle.rot('cloth_r', (0, (0, 0, 0)), (2, (0, 0, 4)), (4, (0, 0, 0)))
A['IDLE'] = idle

# walk: heavy strides, arms low and swinging, orbs trailing
walk = Anim('walk', 1.6, True)
def walk_frame(s):
    return {'right_leg': (-28 * s, 0, 0), 'right_shin': (max(0, 30 * s) + 8, 0, 0), 'right_foot': (-10 * s, 0, 0),
            'left_leg': (28 * s, 0, 0), 'left_shin': (max(0, -30 * s) + 8, 0, 0), 'left_foot': (10 * s, 0, 0),
            'right_arm': (22 * s, 0, 12), 'left_arm': (-22 * s, 0, -12),
            'right_fore': (-20, 0, -6), 'left_fore': (-20, 0, 6),
            'hips': (0, 6 * s, 0), 'chest': (6, -8 * s, 0)}
add_rot(walk, [(0, walk_frame(1)), (0.4, walk_frame(0)), (0.8, walk_frame(-1)), (1.2, walk_frame(0)), (1.6, walk_frame(1))])
walk.pos('base', (0, (0, 0, 0)), (0.4, (0, 1.2, 0)), (0.8, (0, 0, 0)), (1.2, (0, 1.2, 0)), (1.6, (0, 0, 0)))
walk.rot('cloth_f', (0, (-10, 0, 0)), (0.4, (-4, 0, 0)), (0.8, (-10, 0, 0)), (1.2, (-4, 0, 0)), (1.6, (-10, 0, 0)))
walk.rot('cloth_b', (0, (12, 0, 0)), (0.8, (6, 0, 0)), (1.6, (12, 0, 0)))
walk.rot('cloth_r', (0, (0, 0, 8)), (0.8, (0, 0, 2)), (1.6, (0, 0, 8)))
walk.rot('orbs', (0, (8, 0, 0)), (0.8, (4, 0, 0)), (1.6, (8, 0, 0)))
A['WALK'] = walk

# run: fast, leaning glide
run = Anim('run', 0.8, True)
def run_frame(s):
    return {'right_leg': (-45 * s, 0, 0), 'right_shin': (max(0, 55 * s) + 10, 0, 0),
            'left_leg': (45 * s, 0, 0), 'left_shin': (max(0, -55 * s) + 10, 0, 0),
            'right_arm': (40 * s + 20, 0, 20), 'left_arm': (-40 * s + 20, 0, -20),
            'right_fore': (-45, 0, 0), 'left_fore': (-45, 0, 0), 'base': (14, 0, 0), 'chest': (8, -10 * s, 0),
            'head': (-12, 10 * s, 0)}
add_rot(run, [(0, run_frame(1)), (0.2, run_frame(0)), (0.4, run_frame(-1)), (0.6, run_frame(0)), (0.8, run_frame(1))])
run.pos('base', (0, (0, 0, 0)), (0.2, (0, 2, 0)), (0.4, (0, 0, 0)), (0.6, (0, 2, 0)), (0.8, (0, 0, 0)))
run.rot('cloth_f', (0, (-35, 0, 0)), (0.4, (-25, 0, 0)), (0.8, (-35, 0, 0)))
run.rot('cloth_b', (0, (30, 0, 0)), (0.4, (22, 0, 0)), (0.8, (30, 0, 0)))
run.rot('cloth_r', (0, (0, 0, 20)), (0.4, (0, 0, 12)), (0.8, (0, 0, 20)))
run.rot('orbs', (0, (25, 0, 0)), (0.8, (25, 0, 0)))
A['RUN'] = run

# melee: double backhand claw sweep
atk = Anim('attack', 1.0)
add_rot(atk, [
    (0.0, sym({'right_arm': (-20, 0, 40), 'right_fore': (-30, 0, -60)})),
    (0.3, {'right_arm': (-150, -30, 50), 'right_fore': (-40, 0, -20), 'right_fingers': (-10, 0, 0),
           'left_arm': (-20, 0, -30), 'chest': (0, 25, 0), 'waist': (0, 10, 0)}),
    (0.5, {'right_arm': (-40, 40, -20), 'right_fore': (-10, 0, -10), 'right_fingers': (-60, 0, 0),
           'left_arm': (-150, 30, -50), 'chest': (10, -25, 0), 'waist': (0, -10, 0)}),
    (0.7, {'right_arm': (-30, 0, 30), 'left_arm': (-40, -40, 20), 'left_fingers': (-60, 0, 0),
           'chest': (12, 20, 0), 'waist': (0, 5, 0), 'right_fore': (-30, 0, -40), 'right_fingers': (-10, 0, 0)}),
    (1.0, sym({'right_arm': (-20, 0, 40), 'right_fore': (-30, 0, -60), 'right_fingers': (0, 0, 0)}) |
     {'chest': (0, 0, 0), 'waist': (0, 0, 0)}),
])
A['ATTACK'] = atk

# cast (laser triangle): arms thrust forward, orbs spread
cast_a = Anim('cast_laser', 2.0)
add_rot(cast_a, [
    (0.0, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98)}) | {'chest': (0, 0, 0), 'head': (0, 0, 0)}),
    (0.4, sym({'right_arm': (-60, 0, 90), 'right_fore': (-10, 0, 115), 'right_fingers': (20, 0, 0)}) |
     {'chest': (-12, 0, 0), 'head': (-15, 0, 0)}),
    (0.7, sym({'right_arm': (-95, 15, 10), 'right_fore': (0, 0, -5), 'right_hand': (-40, 0, 0), 'right_fingers': (25, 0, 0)}) |
     {'chest': (8, 0, 0), 'head': (5, 0, 0)}),
    (1.6, sym({'right_arm': (-92, 18, 12), 'right_fore': (0, 0, -5), 'right_hand': (-45, 0, 0), 'right_fingers': (25, 0, 0)}) |
     {'chest': (10, 0, 0), 'head': (5, 0, 0)}),
    (2.0, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98), 'right_hand': (0, 0, 0), 'right_fingers': (0, 0, 0)}) |
     {'chest': (0, 0, 0), 'head': (0, 0, 0)}),
])
cast_a.scl('orbs', (0, (1, 1, 1)), (0.5, (1.25, 1.25, 1.25)), (1.6, (1.25, 1.25, 1.25)), (2.0, (1, 1, 1)))
for _b in ('beam_bottom', 'beam_right', 'beam_left'):
    cast_a.scl(_b, (0, (1, 1, 1)), (0.6, (1, 2.6, 2.6)), (1.6, (1, 2.6, 2.6)), (2.0, (1, 1, 1)))
for n in ('orb_top', 'orb_r', 'orb_l'):
    cast_a.rot(n, (0, (0, 0, 0)), (2, (0, 0, 1080)), interp='L')
cast_a.pos('orbs', (0, (0, 0, 0)), (0.5, (0, 3, -4)), (1.6, (0, 3, -4)), (2.0, (0, 0, 0)))
A['CAST_LASER'] = cast_a

# summon crystals: both palms slam downward to the ground
slam = Anim('cast_ground', 1.6)
add_rot(slam, [
    (0.0, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98)}) | {'waist': (0, 0, 0), 'chest': (0, 0, 0)}),
    (0.45, sym({'right_arm': (-170, 0, 20), 'right_fore': (-20, 0, 0), 'right_fingers': (-30, 0, 0)}) |
     {'waist': (-10, 0, 0), 'chest': (-15, 0, 0), 'head': (-20, 0, 0)}),
    (0.7, sym({'right_arm': (-60, 0, 10), 'right_fore': (-20, 0, 0), 'right_hand': (-70, 0, 0), 'right_fingers': (40, 0, 0)}) |
     {'waist': (25, 0, 0), 'chest': (20, 0, 0), 'head': (15, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (-30, 0, 0),
      'right_shin': (45, 0, 0), 'left_shin': (45, 0, 0), 'right_foot': (-15, 0, 0), 'left_foot': (-15, 0, 0)}),
    (1.1, sym({'right_arm': (-60, 0, 10), 'right_fore': (-20, 0, 0), 'right_hand': (-70, 0, 0), 'right_fingers': (40, 0, 0)}) |
     {'waist': (25, 0, 0), 'chest': (20, 0, 0), 'head': (15, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (-30, 0, 0),
      'right_shin': (45, 0, 0), 'left_shin': (45, 0, 0), 'right_foot': (-15, 0, 0), 'left_foot': (-15, 0, 0)}),
    (1.6, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98), 'right_hand': (0, 0, 0), 'right_fingers': (0, 0, 0)}) |
     {'waist': (0, 0, 0), 'chest': (0, 0, 0), 'head': (0, 0, 0), 'right_leg': (0, 0, 0), 'left_leg': (0, 0, 0),
      'right_shin': (0, 0, 0), 'left_shin': (0, 0, 0), 'right_foot': (0, 0, 0), 'left_foot': (0, 0, 0)}),
])
slam.pos('base', (0, (0, 0, 0)), (0.45, (0, 3, 0)), (0.7, (0, -9, 0)), (1.1, (0, -9, 0)), (1.6, (0, 0, 0)))
for _b in ('beam_bottom', 'beam_right', 'beam_left'):
    slam.scl(_b, (0, (1, 1, 1)), (0.7, (1, 3, 3)), (1.0, (1, 1, 1)))
A['CAST_GROUND'] = slam

# prison: one arm points, other clenches, head tilts; triangle spins
prison = Anim('cast_prison', 1.5)
add_rot(prison, [
    (0.0, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98)}) | {'head': (0, 0, 0)}),
    (0.35, {'right_arm': (-100, -20, 20), 'right_fore': (-10, 0, 0), 'right_fingers': (10, 0, 0),
            'left_arm': (-30, 0, -80), 'left_fore': (-60, 0, 100), 'left_fingers': (-90, 0, 0), 'head': (-10, 15, 10), 'chest': (0, 20, 0)}),
    (0.8, {'right_arm': (-95, -25, 20), 'right_fore': (-5, 0, 0), 'right_fingers': (-85, 0, 0),
           'left_arm': (-30, 0, -85), 'left_fore': (-60, 0, 105), 'left_fingers': (-90, 0, 0), 'head': (-10, 15, 10), 'chest': (0, 22, 0)}),
    (1.5, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98), 'right_fingers': (0, 0, 0)}) | {'head': (0, 0, 0), 'chest': (0, 0, 0)}),
])
prison.rot('seal', (0, (0, 0, 0)), (1.5, (0, 0, 0)))
prison.rot('orbs', (0, (0, 0, 0)), (0.8, (0, 0, 120)), (1.5, (0, 0, 120)), interp='L')
A['CAST_PRISON'] = prison

# blink: collapse into itself then reappear
blink = Anim('blink', 1.0)
blink.scl('base', (0, (1, 1, 1)), (0.3, (0.2, 1.5, 0.2)), (0.45, (0.01, 2, 0.01)), (0.55, (0.01, 2, 0.01)),
          (0.7, (1.3, 0.8, 1.3)), (1.0, (1, 1, 1)))
blink.rot('base', (0, (0, 0, 0)), (0.45, (0, 540, 0)), (0.55, (0, 540, 0)), (1.0, (0, 720, 0)), interp='L')
A['BLINK'] = blink

# summon watchers: arms wide to the sky
summon = Anim('summon', 2.0)
add_rot(summon, [
    (0.0, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98)}) | {'head': (0, 0, 0), 'chest': (0, 0, 0)}),
    (0.6, sym({'right_arm': (0, 0, 150), 'right_fore': (0, 0, -10), 'right_fingers': (20, 0, 0)}) |
     {'head': (-35, 0, 0), 'chest': (-15, 0, 0)}),
    (1.4, sym({'right_arm': (0, 0, 160), 'right_fore': (0, 0, -10), 'right_fingers': (25, 0, 0)}) |
     {'head': (-40, 0, 0), 'chest': (-18, 0, 0)}),
    (2.0, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98), 'right_fingers': (0, 0, 0)}) | {'head': (0, 0, 0), 'chest': (0, 0, 0)}),
])
summon.pos('base', (0, (0, 0, 0)), (0.6, (0, 6, 0)), (1.4, (0, 7, 0)), (2.0, (0, 0, 0)))
summon.scl('orbs', (0, (1, 1, 1)), (0.8, (1.5, 1.5, 1.5)), (1.4, (1.5, 1.5, 1.5)), (2.0, (1, 1, 1)))
for n in ('orb_top', 'orb_r', 'orb_l'):
    summon.rot(n, (0, (0, 0, 0)), (2, (0, 0, -720)), interp='L')
A['SUMMON'] = summon

# death: kneel, arms fall, crown cracks off, collapse
death = Anim('death', 3.0)
add_rot(death, [
    (0.0, sym({'right_arm': (-25, 0, 78), 'right_fore': (-20, 0, 98)}) | {'head': (0, 0, 0), 'chest': (0, 0, 0),
                                                                             'right_leg': (0, 0, 0), 'left_leg': (0, 0, 0), 'right_shin': (0, 0, 0), 'left_shin': (0, 0, 0)}),
    (0.5, sym({'right_arm': (-10, 0, 120), 'right_fore': (-30, 0, -40)}) | {'head': (-40, 0, 0), 'chest': (-20, 0, 0)}),
    (1.3, sym({'right_arm': (10, 0, 15), 'right_fore': (-10, 0, 0)}) | {'head': (30, 10, 0), 'chest': (25, 0, 0),
                                                                         'right_leg': (-80, 0, 0), 'left_leg': (-20, 0, 0),
                                                                         'right_shin': (80, 0, 0), 'left_shin': (100, 0, 0)}),
    (3.0, sym({'right_arm': (10, 0, 12), 'right_fore': (-10, 0, 0)}) | {'head': (45, 10, 0), 'chest': (35, 0, 0),
                                                                        'right_leg': (-80, 0, 0), 'left_leg': (-20, 0, 0),
                                                                        'right_shin': (80, 0, 0), 'left_shin': (100, 0, 0)}),
])
death.pos('base', (0, (0, 0, 0)), (1.3, (0, -22, 0)), (3.0, (0, -24, 0)))
death.scl('orbs', (0, (1, 1, 1)), (0.8, (1.3, 1.3, 1.3)), (1.6, (0.01, 0.01, 0.01)), (3.0, (0.01, 0.01, 0.01)))
death.pos('crown', (0, (0, 0, 0)), (1.2, (0, 0, 0)), (1.8, (3, 4, -2)), (3.0, (8, -20, -6)))
death.rot('crown', (0, (0, 0, 0)), (1.2, (0, 0, 0)), (3.0, (-60, 0, 80)))
A['DEATH'] = death
