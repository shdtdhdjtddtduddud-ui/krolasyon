"""İntikam (Revenge) — crimson flesh demon with blood lance, arm blade, porous robe and blood pool."""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, noise2, lerp, border, glow_img, draw_line
from animlib import add_rot, sym, pose_seq

M = Model('revenge', 512, 512)
R0 = np.eye(3)


def local_bone(name, parent, off, rot=(0, 0, 0)):
    M.bone(name, parent, (0, 0, 0))
    M.bones[name].off = [float(v) for v in off]
    M.bones[name].rot = [float(v) for v in rot]
    return M.bones[name]


# ---------------- skeleton ----------------
M.bone('base', None, (0, -52, 0))
M.bone('hips', 'base', (0, -52, 0))
M.cube('hips', (-7, -57, -4), (14, 7, 8), 'flesh_dark')
M.cube('hips', (-8.5, -59, -5), (17, 5, 10), 'skirt_band')
# porous robe (double sided planes, split at the front like the artwork)
M.bone('skirt_fr', 'hips', (-2.2, -56, -5.3), (-6, 16, 0))
M.cube_l('skirt_fr', (-8, 0, 0), (8, 70, 0), 'skirt', plane=True)
M.bone('skirt_fl', 'hips', (2.2, -56, -5.3), (-6, -16, 0))
M.cube_l('skirt_fl', (0, 0, 0), (8, 70, 0), 'skirt', plane=True)
M.cube('hips', (-4, -56, -5.25), (8, 14, 0), 'skirt_band', plane=True)
M.bone('skirt_b', 'hips', (0, -56, 5.3), (7, 0, 0))
M.cube_l('skirt_b', (-9.5, 0, 0), (19, 74, 0), 'skirt_long', plane=True)
M.bone('skirt_r', 'hips', (-8.4, -56, 0), (0, 90, 7))
M.cube_l('skirt_r', (-6, 0, 0), (12, 72, 0), 'skirt_long', plane=True)
M.bone('skirt_l', 'hips', (8.4, -56, 0), (0, -90, -7))
M.cube_l('skirt_l', (-6, 0, 0), (12, 72, 0), 'skirt_long', plane=True)

M.bone('waist', 'hips', (0, -57, 0))
M.cube('waist', (-5.5, -69, -3.5), (11, 12, 7), 'flesh_abs')
M.bone('chest', 'waist', (0, -69, 0))
M.cube('chest', (-9.5, -85, -4.8), (19, 16, 9.6), 'flesh_chest')
M.cube('chest', (-9, -84, -5.6), (8.8, 7, 1), 'flesh_pec')
M.cube('chest', (0.2, -84, -5.6), (8.8, 7, 1), 'flesh_pec')
M.cube('chest', (-7.5, -87.5, -3.5), (15, 3, 7), 'flesh')
M.bone('neck', 'chest', (0, -86, 0))
M.cube('neck', (-2.3, -92, -2.2), (4.6, 7, 4.4), 'flesh')
M.bone('head', 'neck', (0, -91, 0))
M.cube('head', (-4, -102, -4.3), (8, 11.5, 8.4), 'rv_head')
M.cube('head', (-3.7, -100.5, -4.9), (7.4, 9, 0.7), 'rv_face')
M.cube('head', (-1.6, -104.5, -3.5), (3.2, 3, 10), 'flesh_dark')      # swept crest
# long white hair flowing back and to the left side
HAIR = [((-2.5, -102, 2), (4, -95, 12), (12, -80, 20)),
        ((0, -103, 3), (8, -97, 13), (20, -86, 18)),
        ((2, -102, 3), (12, -94, 9), (25, -80, 10)),
        ((-1, -101, 4), (5, -90, 12), (10, -70, 16)),
        ((3, -100, 2), (14, -90, 6), (22, -72, 8)),
        ((1, -104, 1), (10, -101, 6), (24, -96, 6))]
for i, (a, b, c) in enumerate(HAIR):
    M.seg(f'hair{i}a', 'head', a, b, 5.2, 1.3, 'hair', extend=0.5)
    M.seg(f'hair{i}b', f'hair{i}a', b, c, 4.4, 1.1, 'hair_tip', extend=0.6)

# right arm raised, holding the blood lance over the shoulder
A, B, C = (-12.5, -83, 0), (-20, -69, -2), (-13, -84, -8)
M.seg('right_arm', 'chest', A, B, 6.8, 6.8, 'flesh_arm', extend=2)
M.seg('right_fore', 'right_arm', B, C, 5.6, 5.6, 'flesh_arm', extend=1.5)
M.bone_w('right_hand', 'right_fore', C, R0)
M.cube_l('right_hand', (-2.6, -2.5, -2.4), (5.2, 5.5, 4.8), 'flesh_dark')
T = (40, -112, 12)
L = M.seg('spear', 'right_hand', C, T, 1.9, 1.9, 'spear')
M.cube_l('spear', (-0.95, -36, -0.95), (1.9, 36, 1.9), 'spear')
M.cube_l('spear', (-0.65, L, -0.65), (1.3, 7, 1.3), 'spear')
M.cube_l('spear', (-0.35, L + 7, -0.35), (0.7, 6, 0.7), 'spear')
for k, (y, z) in enumerate(((-34, 55), (-34, -55), (-32, 25), (-32, -25), (-28, 80), (-24, -70), (-20, 60))):
    local_bone(f'barb{k}', 'spear', (0, y, 0), (0, k * 45, z))
    M.cube_l(f'barb{k}', (-0.7, -9 + k * 0.6, -0.7), (1.4, 9 - k * 0.6, 1.4), 'spear')
    local_bone(f'barb{k}t', f'barb{k}', (0, -9 + k * 0.6, 0), (0, 0, -z * 0.45))
    M.cube_l(f'barb{k}t', (-0.4, -5, -0.4), (0.8, 5, 0.8), 'spear')

# left arm hangs down and grows into a long blood blade
A, B, C = (12.5, -83, 0), (18, -63, 2), (20, -45, -1)
M.seg('left_arm', 'chest', A, B, 6.8, 6.8, 'flesh_arm', extend=2)
M.seg('left_fore', 'left_arm', B, C, 5.6, 5.6, 'flesh_arm', extend=1.5)
M.bone_w('left_hand', 'left_fore', C, R0)
M.cube_l('left_hand', (-2.6, 0, -2.4), (5.2, 6, 4.8), 'flesh_dark')
M.seg('left_blade', 'left_hand', (20, -40, -1), (30, 8, -6), 2.8, 1.3, 'blade', extend=1)
M.cube_l('left_blade', (-0.5, 49, -0.4), (1.0, 7, 0.8), 'blade')
for k in range(5):
    local_bone(f'fbarb{k}', 'left_fore', (2.6, 3 + k * 3.2, 0), (0, 0, -60 - k * 4))
    M.cube_l(f'fbarb{k}', (-0.5, -7 + k * 0.5, -0.5), (1, 7 - k * 0.5, 1), 'blade')

# long legs
for side, sx in (('right', -1), ('left', 1)):
    A, B, C = (sx * 4.6, -53, 0), (sx * 5.6, -16, -1.5), (sx * 5.2, 19, 0.5)
    M.seg(f'{side}_leg', 'hips', A, B, 6.4, 6.8, 'flesh_leg', extend=2)
    M.seg(f'{side}_shin', f'{side}_leg', B, C, 5, 5.2, 'flesh_leg', extend=1.5)
    M.bone_w(f'{side}_foot', f'{side}_shin', C, R0)
    M.cube_l(f'{side}_foot', (-2.6, 0, -6.5), (5.2, 5, 9), 'flesh_dark')

# blood pool + splashes around the feet (does not follow body sway)
M.bone('pool', None, (0, 24, 0))
M.cube_l('pool', (-22, -0.3, -18), (44, 0.3, 36), 'blood_pool')
rs = np.random.default_rng(3)
for i in range(13):
    a = i / 13 * math.tau + rs.random() * 0.3
    Rr = 10 + rs.random() * 7
    w, h = 8 + rs.random() * 6, 7 + rs.random() * 12
    n = f'splash{i}'
    M.bone(n, 'pool', (math.cos(a) * Rr, 24, math.sin(a) * Rr), (-18 - rs.random() * 20, 90 - math.degrees(a), 0))
    M.cube_l(n, (-w / 2, -h, 0), (w, h, 0), 'splash', plane=True)

# ---------------- textures ----------------
P = Painter(M)
INK = (30, 4, 8)


def flesh(c, face, W, H, r, lo=(66, 8, 14), mid=(128, 22, 28), hi=(186, 52, 48), fib=1.0, abs_=False, pec=False):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 3)
    wav = noise2(H, W, r, 6, 1)
    fibers = 0.5 + 0.5 * np.sin(xx * 1.9 + wav * 7)
    t = np.clip(0.5 + (n - 0.5) * 0.7 + (fibers - 0.5) * 0.45 * fib - 0.25 * (yy / max(H - 1, 1)), 0, 1)
    if face == 'top':
        t = np.clip(t + 0.15, 0, 1)
    if face == 'bottom':
        t = t * 0.4
    col = np.where(t[..., None] < 0.5, lerp(lo, mid, t * 2), lerp(mid, hi, (t - 0.5) * 2))
    dark = fibers < 0.12
    col[dark] = lerp(col[dark], INK, 0.55)
    if abs_ and face == 'north':
        for y in range(2, H - 1, 3):
            col[y, 1:W - 1] = lerp(col[y, 1:W - 1], INK, 0.65)
        col[:, W // 2] = lerp(col[:, W // 2], INK, 0.7)
    if pec and face == 'north':
        col[-1] = INK
    b = border(H, W)
    col[b] = lerp(col[b], INK, 0.6)
    return col, np.ones((H, W)), None


P.mats['flesh'] = flesh
P.mats['flesh_arm'] = flesh
P.mats['flesh_leg'] = lambda c, f, W, H, r: flesh(c, f, W, H, r, fib=1.3)
P.mats['flesh_dark'] = lambda c, f, W, H, r: flesh(c, f, W, H, r, lo=(40, 4, 10), mid=(88, 14, 20), hi=(130, 30, 34))
P.mats['flesh_abs'] = lambda c, f, W, H, r: flesh(c, f, W, H, r, abs_=True)
P.mats['flesh_chest'] = lambda c, f, W, H, r: flesh(c, f, W, H, r, fib=0.7)
P.mats['flesh_pec'] = lambda c, f, W, H, r: flesh(c, f, W, H, r, fib=0.5, pec=True, hi=(200, 64, 58))
P.mats['rv_head'] = lambda c, f, W, H, r: flesh(c, f, W, H, r, lo=(50, 6, 12), mid=(105, 18, 24), hi=(150, 38, 38), fib=0.5)


@P.mat('rv_face')
def rv_face(c, face, W, H, r):
    col, a, _ = flesh(c, face, W, H, r, lo=(50, 6, 12), mid=(105, 18, 24), hi=(150, 38, 38), fib=0.3)
    if face != 'north':
        return col, a, None
    g = np.zeros((H, W, 4), np.float32)
    # dark visor with grille slits
    col[0:3] = (22, 6, 10)
    for x in range(1, W - 1, 2):
        col[1, x] = (140, 20, 26)
    g[1, 1:W - 1:2] = (255, 40, 40, 150)
    # wide grin: teeth rows
    for y in (H - 4, H - 3):
        for x in range(W):
            col[y, x] = (235, 222, 200) if x % 2 == 0 else (90, 70, 60)
    col[H - 5] = (40, 5, 10)
    col[H - 2] = (40, 5, 10)
    return col, a, g


@P.mat('hair')
def hair(c, face, W, H, r, tip=False):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 2)
    strands = 0.5 + 0.5 * np.sin(xx * 2.6 + n * 5)
    t = np.clip(0.55 + (strands - 0.5) * 0.6 + (n - 0.5) * 0.3 - (0.25 * yy / max(H - 1, 1) if tip else 0), 0, 1)
    col = lerp((120, 118, 128), (236, 236, 240), t)
    alpha = np.ones((H, W))
    if tip and face in ('north', 'south', 'west', 'east'):
        # wispy ends
        for x in range(W):
            cut = int(r.random() * H * 0.25)
            if cut:
                alpha[H - cut:, x] = 0
    return col, alpha, None


P.mats['hair_tip'] = lambda c, f, W, H, r: hair(c, f, W, H, r, tip=True)


def porous(c, face, W, H, r, cut=0.35, long_drips=True):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 3)
    vt = yy / max(H - 1, 1)
    base = lerp(lerp((128, 104, 114), (120, 40, 52), np.clip(vt * 2.0, 0, 1)), (78, 14, 26), np.clip(vt * 1.4 - 0.4, 0, 1))
    col = lerp(base * 0.9, base * 1.1, n)
    alpha = np.ones((H, W))
    # cells (organic pores)
    cells = int(W * H / 38)
    for _ in range(cells):
        cx, cy = r.random() * W, r.random() * H
        rx, ry = 1.0 + r.random() * 1.2, 1.8 + r.random() * 3.0
        d = ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2
        rim = (d >= 1) & (d < 1.9)
        inner = d < 1
        col[rim] = lerp(col[rim], (150, 96, 106), 0.5)
        col[inner] = lerp(col[inner], (38, 8, 16), 0.8)
        if face in ('north', 'south') and cy > H * 0.45 and r.random() < 0.3:
            alpha[inner] = 0
    if face in ('north', 'south'):
        v = r.random() * cut * H
        for x in range(W):
            v += r.normal(0, H * 0.06)
            v = np.clip(v, 0, cut * H)
            drip = int(r.random() * H * 0.18) if long_drips and r.random() < 0.3 else 0
            edge = H - int(v)
            alpha[edge:, x] = 0
            if drip and edge > 0:
                alpha[edge:min(H, edge + drip), x] = 1
                col[edge:min(H, edge + drip), x] = (80, 10, 20)
            for k in range(2):
                if 0 <= edge - 1 - k < H:
                    col[edge - 1 - k, x] = lerp(col[edge - 1 - k, x], (30, 4, 10), 0.6)
    return col, alpha, None


P.mats['skirt'] = lambda c, f, W, H, r: porous(c, f, W, H, r, cut=0.3)
P.mats['skirt_long'] = lambda c, f, W, H, r: porous(c, f, W, H, r, cut=0.22)


@P.mat('skirt_band')
def skirt_band(c, face, W, H, r):
    col, a, g = porous(c, face, W, H, r, cut=0)
    return col, np.ones((H, W)), None


def blood(c, face, W, H, r, bright=1.0):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 2)
    col = lerp((95, 6, 16), (205, 36, 44), np.clip(n * 0.8 + 0.2, 0, 1) * bright)
    shine = (xx == 0) | ((xx + yy // 3) % 7 == 0)
    col[shine] = lerp(col[shine], (255, 130, 120), 0.45)
    return col, np.ones((H, W)), glow_img(H, W, (255, 40, 50), shine * 0.35)


P.mats['spear'] = blood
P.mats['blade'] = lambda c, f, W, H, r: blood(c, f, W, H, r, 1.1)


@P.mat('blood_pool')
def blood_pool(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    if face != 'top':
        return np.zeros((H, W, 3)) + (80, 5, 15), np.zeros((H, W)), None
    cx, cy = (W - 1) / 2, (H - 1) / 2
    ang = np.arctan2(yy - cy, xx - cx)
    d = np.hypot((xx - cx) / (W / 2), (yy - cy) / (H / 2))
    edge = 0.72 + 0.18 * np.sin(ang * 5 + 1) + 0.08 * np.sin(ang * 13) + (noise2(H, W, r, 4) - 0.5) * 0.25
    alpha = (d < edge).astype(float)
    n = noise2(H, W, r, 3)
    col = lerp((70, 4, 14), (170, 20, 30), np.clip(n + (1 - d) * 0.3, 0, 1))
    hl = (np.abs(np.sin(xx * 0.35 + yy * 0.2 + n * 3)) > 0.97) & (d < edge - 0.05)
    col[hl] = (230, 90, 90)
    rim = (d < edge) & (d > edge - 0.06)
    col[rim] = (50, 2, 10)
    return col, alpha, None


@P.mat('splash')
def splash(c, face, W, H, r):
    S = 3
    Hs, Ws = H * S, W * S
    yy, xx = np.mgrid[0:Hs, 0:Ws] / S
    u = (xx - W / 2) / (W / 2)
    v = 1 - yy / H
    wob = noise2(Hs, Ws, r, 5 * S, 2)
    top = 0.35 + 0.55 * np.clip(1 - np.abs(u + 0.3 * np.sin(v * 3)) ** 1.6, 0, 1) + (wob - 0.5) * 0.35
    inside = (v < top) & (np.abs(u) < 0.95 - v * 0.3)
    # curling crest droplets
    for _ in range(4):
        cx, cy = r.random() * W, H * (0.1 + r.random() * 0.3)
        rr = 0.5 + r.random() * 1.0
        inside |= np.hypot(xx - cx, yy - cy) < rr
    holes = np.zeros_like(inside)
    for _ in range(3):
        cx, cy = r.random() * W, H * (0.4 + r.random() * 0.4)
        holes |= np.hypot((xx - cx) / 1.4, (yy - cy) / 2.2) < 1
    inside &= ~holes
    col = lerp((60, 2, 12), (200, 40, 48), np.clip(v * 1.3, 0, 1))
    hi = np.abs(v - top + 0.06) < 0.04
    col[hi] = (240, 110, 110)
    alpha = inside.astype(float)
    col = col.reshape(H, S, W, S, 3).mean((1, 3))
    alpha = (alpha.reshape(H, S, W, S).mean((1, 3)) > 0.45).astype(float)
    return col, alpha, None


# ---------------- animations ----------------
A = {}
LIMBS = ['right_arm', 'right_fore', 'right_hand', 'left_arm', 'left_fore', 'left_hand', 'spear', 'left_blade',
         'right_leg', 'right_shin', 'left_leg', 'left_shin', 'right_foot', 'left_foot', 'chest', 'waist', 'head', 'hips', 'neck']
SKIRT = ['skirt_fr', 'skirt_fl', 'skirt_b', 'skirt_r', 'skirt_l']


def hair_wave(a, length, amp=1.0, lift=0.0, speed=1):
    for i in range(len(HAIR)):
        for s, n in enumerate((f'hair{i}a', f'hair{i}b')):
            keys = []
            for k in range(9):
                t = length * k / 8
                ph = math.tau * k / 8 * speed - s * 1.1 - i * 0.9
                keys.append((t, (lift + 5 * amp * math.sin(ph), 4 * amp * math.cos(ph), 6 * amp * math.sin(ph + 0.7))))
            a.rot(n, *keys)


def skirt_wave(a, length, amp=1.0, flare=0.0, speed=1):
    for i, n in enumerate(SKIRT):
        keys = []
        for k in range(9):
            t = length * k / 8
            ph = math.tau * k / 8 * speed + i * 1.3
            s = -1 if n in ('skirt_fr', 'skirt_fl') else 1
            keys.append((t, (s * (flare + 3 * amp * math.sin(ph)), 0, 2 * amp * math.cos(ph))))
        a.rot(n, *keys)


idle = Anim('idle', 4.0, True)
idle.pos('base', (0, (0, 0, 0)), (2, (0, -0.8, 0)), (4, (0, 0, 0)))
idle.rot('chest', (0, (0, 0, 0)), (2, (-3, 2, 0)), (4, (0, 0, 0)))
idle.rot('head', (0, (0, 0, 0)), (1.3, (4, -6, 3)), (2.6, (-2, 5, -2)), (4, (0, 0, 0)))
idle.rot('right_arm', (0, (0, 0, 0)), (2, (-3, 0, 2)), (4, (0, 0, 0)))
idle.rot('left_arm', (0, (0, 0, 0)), (2, (4, 0, -3)), (4, (0, 0, 0)))
idle.rot('left_blade', (0, (0, 0, 0)), (2, (0, 0, 4)), (4, (0, 0, 0)))
hair_wave(idle, 4.0, 0.8)
skirt_wave(idle, 4.0, 0.7)
for i in range(13):
    idle.scl(f'splash{i}', (0, (1, 1, 1)), (1 + (i % 3) * 0.5, (1.05, 1.25, 1.05)), (4, (1, 1, 1)))
A['IDLE'] = idle


def walk_frame(s, big=1.0):
    return {'right_leg': (-26 * s * big, 0, 0), 'right_shin': (max(0, 34 * s) * big + 6, 0, 0),
            'left_leg': (26 * s * big, 0, 0), 'left_shin': (max(0, -34 * s) * big + 6, 0, 0),
            'left_arm': (-18 * s * big, 0, 0), 'right_arm': (6 * s, 0, 0), 'hips': (0, 7 * s, 0), 'chest': (4 * big, -9 * s, 0)}


walk = Anim('walk', 1.6, True)
add_rot(walk, [(0, walk_frame(1)), (0.4, walk_frame(0)), (0.8, walk_frame(-1)), (1.2, walk_frame(0)), (1.6, walk_frame(1))])
walk.pos('base', (0, (0, 0, 0)), (0.4, (0, 1.4, 0)), (0.8, (0, 0, 0)), (1.2, (0, 1.4, 0)), (1.6, (0, 0, 0)))
hair_wave(walk, 1.6, 1.0, lift=-8, speed=2)
skirt_wave(walk, 1.6, 1.5, flare=4, speed=2)
A['WALK'] = walk

run = Anim('run', 0.9, True)
rf = lambda s: walk_frame(s, 1.6) | {'base': (14, 0, 0), 'head': (-12, 0, 0), 'left_arm': (-40 * s + 15, 0, 0)}
add_rot(run, [(0, rf(1)), (0.225, rf(0)), (0.45, rf(-1)), (0.675, rf(0)), (0.9, rf(1))])
run.pos('base', (0, (0, 0, 0)), (0.225, (0, 2.5, 0)), (0.45, (0, 0, 0)), (0.675, (0, 2.5, 0)), (0.9, (0, 0, 0)))
hair_wave(run, 0.9, 1.0, lift=-25, speed=2)
skirt_wave(run, 0.9, 2, flare=16, speed=2)
A['RUN'] = run

# melee: spear thrust then blade backhand
atk = Anim('attack', 1.1)
pose_seq(atk, [
    (0.0, {}),
    (0.25, {'right_arm': (35, -20, -20), 'right_fore': (25, 0, 0), 'chest': (0, 25, 0), 'spear': (15, 0, 0)}),
    (0.42, {'right_arm': (-60, 15, 15), 'right_fore': (-35, 0, 0), 'chest': (10, -20, 0), 'spear': (-40, 0, 0), 'waist': (5, -10, 0)}),
    (0.62, {'left_arm': (-70, 30, -30), 'left_fore': (-20, 0, 0), 'chest': (5, 20, 0), 'right_arm': (-20, 0, 0)}),
    (0.8, {'left_arm': (-20, -40, 40), 'left_fore': (-10, 0, 0), 'chest': (5, -25, 0), 'waist': (0, -10, 0)}),
    (1.1, {}),
], LIMBS)
atk.pos('base', (0, (0, 0, 0)), (0.42, (0, -1, -5)), (0.8, (0, -1, -3)), (1.1, (0, 0, 0)))
hair_wave(atk, 1.1, 1.2, speed=1)
A['ATTACK'] = atk

# lance throw
lance = Anim('lance', 1.5)
pose_seq(lance, [
    (0.0, {}),
    (0.45, {'right_arm': (55, -10, -30), 'right_fore': (30, 0, 0), 'chest': (-8, 35, 0), 'waist': (0, 15, 0), 'left_arm': (-35, 0, -20),
            'right_leg': (-15, 0, 0), 'left_leg': (20, 0, 0), 'left_shin': (15, 0, 0)}),
    (0.7, {'right_arm': (-95, 20, 10), 'right_fore': (-30, 0, 0), 'spear': (-60, 0, 0), 'chest': (18, -30, 0), 'waist': (8, -15, 0),
           'left_arm': (30, 0, 10), 'right_leg': (20, 0, 0), 'left_leg': (-20, 0, 0), 'right_shin': (10, 0, 0)}),
    (1.0, {'right_arm': (-60, 10, 10), 'right_fore': (-20, 0, 0), 'spear': (-30, 0, 0), 'chest': (10, -15, 0), 'waist': (4, -8, 0)}),
    (1.5, {}),
], LIMBS)
lance.pos('base', (0, (0, 0, 0)), (0.45, (0, -2, 3)), (0.7, (0, -1, -6)), (1.5, (0, 0, 0)))
hair_wave(lance, 1.5, 1.3, lift=-10)
A['LANCE'] = lance

# blood tide: raise both arms, slam into the pool
tide = Anim('tide', 1.7)
pose_seq(tide, [
    (0.0, {}),
    (0.5, {'right_arm': (-30, 0, 40), 'left_arm': (-150, 0, -30), 'left_fore': (-20, 0, 0), 'head': (-25, 0, 0), 'chest': (-12, 0, 0)}),
    (0.7, {'right_arm': (10, 0, 10), 'left_arm': (-40, 0, 10), 'left_fore': (-10, 0, 0), 'head': (20, 0, 0), 'chest': (25, 0, 0), 'waist': (15, 0, 0),
           'right_leg': (-40, 0, 0), 'left_leg': (-10, 0, 0), 'right_shin': (70, 0, 0), 'left_shin': (40, 0, 0)}),
    (1.2, {'right_arm': (10, 0, 10), 'left_arm': (-40, 0, 10), 'left_fore': (-10, 0, 0), 'head': (20, 0, 0), 'chest': (25, 0, 0), 'waist': (15, 0, 0),
           'right_leg': (-40, 0, 0), 'left_leg': (-10, 0, 0), 'right_shin': (70, 0, 0), 'left_shin': (40, 0, 0)}),
    (1.7, {}),
], LIMBS)
tide.pos('base', (0, (0, 0, 0)), (0.5, (0, 4, 0)), (0.7, (0, -12, 0)), (1.2, (0, -12, 0)), (1.7, (0, 0, 0)))
skirt_wave(tide, 1.7, 1.0, flare=18)
for i in range(13):
    tide.scl(f'splash{i}', (0, (1, 1, 1)), (0.7, (1, 1, 1)), (0.85, (1.4, 2.6, 1.4)), (1.3, (1.1, 1.3, 1.1)), (1.7, (1, 1, 1)))
A['TIDE'] = tide

# blade dance: three sweeping slashes
dance = Anim('dance', 1.5)
pose_seq(dance, [
    (0.0, {}),
    (0.2, {'left_arm': (-60, 40, -60), 'chest': (0, 30, 0), 'waist': (0, 10, 0)}),
    (0.35, {'left_arm': (-40, -50, 50), 'left_fore': (-10, 0, 0), 'chest': (10, -30, 0), 'waist': (0, -10, 0)}),
    (0.6, {'left_arm': (-120, 0, -20), 'left_fore': (-30, 0, 0), 'chest': (-10, 10, 0)}),
    (0.75, {'left_arm': (-10, 0, 10), 'left_fore': (0, 0, 0), 'chest': (25, 0, 0), 'waist': (10, 0, 0)}),
    (1.0, {'left_arm': (-50, -40, 70), 'chest': (0, -35, 0), 'waist': (0, -12, 0), 'right_arm': (-30, 0, 0)}),
    (1.15, {'left_arm': (-60, 60, -60), 'chest': (10, 35, 0), 'waist': (0, 12, 0), 'right_arm': (-20, 0, 0)}),
    (1.5, {}),
], LIMBS)
dance.pos('base', (0, (0, 0, 0)), (0.2, (0, 0, -4)), (0.6, (0, 2, -6)), (0.75, (0, -3, -8)), (1.15, (0, 0, -6)), (1.5, (0, 0, 0)))
hair_wave(dance, 1.5, 1.5, lift=-12, speed=2)
skirt_wave(dance, 1.5, 2.0, flare=10, speed=2)
A['DANCE'] = dance

# hemorrhage: arms wide, chest out, head back, floating
drain = Anim('drain', 2.5)
pose_seq(drain, [
    (0.0, {}),
    (0.5, {'right_arm': (-20, 0, 55), 'left_arm': (-20, 0, -45), 'left_fore': (-10, 0, 0), 'head': (-30, 0, 0), 'chest': (-15, 0, 0),
           'right_leg': (0, 0, 8), 'left_leg': (0, 0, -8)}),
    (2.1, {'right_arm': (-25, 0, 60), 'left_arm': (-25, 0, -50), 'left_fore': (-15, 0, 0), 'head': (-35, 0, 0), 'chest': (-18, 0, 0),
           'right_leg': (0, 0, 8), 'left_leg': (0, 0, -8)}),
    (2.5, {}),
], LIMBS)
drain.pos('base', (0, (0, 0, 0)), (0.5, (0, 6, 0)), (2.1, (0, 8, 0)), (2.5, (0, 0, 0)))
hair_wave(drain, 2.5, 1.2, lift=-20, speed=2)
skirt_wave(drain, 2.5, 1.5, flare=10, speed=2)
A['DRAIN'] = drain

# vengeance: guard and absorb, tremble, then explode outward
ven = Anim('vengeance', 3.0)
guard = {'right_arm': (-40, 30, -40), 'left_arm': (-70, -40, 40), 'left_fore': (-50, 0, 0), 'head': (20, 0, 0), 'chest': (20, 0, 0),
         'waist': (10, 0, 0), 'right_leg': (-25, 0, 0), 'left_leg': (-10, 0, 0), 'right_shin': (40, 0, 0), 'left_shin': (25, 0, 0)}
g2 = {k: (v[0] + 3, v[1], v[2] + 2) for k, v in guard.items()}
pose_seq(ven, [(0, {}), (0.4, guard), (0.8, g2), (1.2, guard), (1.6, g2), (2.0, guard),
               (2.25, {'right_arm': (-40, 0, 70), 'left_arm': (-40, 0, -70), 'left_fore': (0, 0, 0), 'head': (-40, 0, 0), 'chest': (-25, 0, 0)}),
               (2.6, {'right_arm': (-30, 0, 60), 'left_arm': (-30, 0, -60), 'head': (-30, 0, 0), 'chest': (-20, 0, 0)}),
               (3.0, {})], LIMBS)
ven.pos('base', (0, (0, 0, 0)), (0.4, (0, -6, 0)), (2.0, (0, -7, 0)), (2.25, (0, 5, 0)), (3.0, (0, 0, 0)))
hair_wave(ven, 3.0, 0.6, speed=3)
skirt_wave(ven, 3.0, 0.8, flare=0, speed=3)
ven.rot('skirt_b', (0, (0, 0, 0)), (2.0, (0, 0, 0)), (2.3, (40, 0, 0)), (3.0, (0, 0, 0)))
ven.rot('skirt_fr', (0, (0, 0, 0)), (2.0, (0, 0, 0)), (2.3, (-40, 0, 0)), (3.0, (0, 0, 0)))
ven.rot('skirt_fl', (0, (0, 0, 0)), (2.0, (0, 0, 0)), (2.3, (-40, 0, 0)), (3.0, (0, 0, 0)))
A['VENGEANCE'] = ven

death = Anim('death', 3.0)
pose_seq(death, [
    (0.0, {}),
    (0.4, {'head': (-35, 0, 0), 'chest': (-15, 0, 0), 'right_arm': (-20, 0, 40), 'left_arm': (-20, 0, -30)}),
    (1.2, {'head': (30, 0, 10), 'chest': (25, 0, 0), 'right_arm': (40, 0, -10), 'right_fore': (40, 0, 0), 'spear': (70, 0, 40),
           'left_arm': (20, 0, 10), 'right_leg': (-85, 0, 0), 'left_leg': (-60, 0, 0), 'right_shin': (95, 0, 0), 'left_shin': (100, 0, 0)}),
    (3.0, {'head': (45, 0, 15), 'chest': (40, 0, 0), 'right_arm': (45, 0, -10), 'right_fore': (40, 0, 0), 'spear': (80, 0, 50),
           'left_arm': (25, 0, 10), 'right_leg': (-85, 0, 0), 'left_leg': (-60, 0, 0), 'right_shin': (95, 0, 0), 'left_shin': (100, 0, 0)}),
], LIMBS)
death.pos('base', (0, (0, 0, 0)), (0.4, (0, 2, 0)), (1.2, (0, -30, 0)), (3.0, (0, -34, 2)))
for i in range(13):
    death.scl(f'splash{i}', (0, (1, 1, 1)), (1.2, (1.5, 2.0, 1.5)), (3.0, (1.8, 0.3, 1.8)))
A['DEATH'] = death
