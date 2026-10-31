"""Kabkaba (Cille) — model, textures and animations.

Model units: feet at y=24, y down, front = -z. Rendered at scale 0.4 => 40 units per block.
Built from rounded volumes (stacked cube slices) and painted in world space so the creature reads as one
smooth cartoon body: huge dark-navy shaggy mane over head and chest, big rounded light-grey muzzle,
white slit eyes, two forward-curving brown horns, grey striped body, dark lower legs and big paws,
long grey tail rising in an S with a red feather tuft near the top.
"""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, noise2, glow_img
from animlib import pose_seq, sym, add_rot
from shapes import ellipsoid, ellipsoid_l, limb, texel_world, Noise3, light, lerp, ramp, smooth

M = Model('kabkaba', 1024, 1024)
R0 = np.eye(3)


def H(h):
    """height above ground (units) -> model y"""
    return 24 - h


# ---------------------------------------------------------------- skeleton + volumes
M.bone('base', None, (0, 24, 0))
M.bone('body', 'base', (0, H(50), 14))
ellipsoid(M, 'body', (0, H(50), 28), (21, 19, 36), 'body', axis='z', slices=8)
ellipsoid(M, 'body', (0, H(48), 48), (19, 18, 16), 'body', axis='z', slices=4)          # haunches
M.bone('chest', 'body', (0, H(52), 2))
ellipsoid(M, 'chest', (0, H(54), -18), (24, 24, 26), 'chest', axis='z', slices=6)

# ---- mane: big shaggy rounded mass over neck, shoulders and chest
M.bone('mane', 'chest', (0, H(70), -26))
ellipsoid(M, 'mane', (0, H(68), -38), (31, 33, 25), 'mane', axis='z', slices=7)
ellipsoid(M, 'mane', (0, H(76), -14), (27, 22, 15), 'mane', axis='z', slices=4)         # cape over the shoulders
ellipsoid(M, 'mane', (0, H(42), -40), (25, 9, 20), 'mane_dark', axis='y', slices=3)     # chest fringe


def tuft(parent, name, pos, L, w, rx, rz, ry=0.0, mat='mane_tuft'):
    """tapered hair spike pointing along local -y"""
    M.bone(name, parent, pos, (rx, ry, rz))
    M.cube_l(name, (-w / 2, -L * 0.55, -w / 2), (w, L * 0.55, w), mat)
    M.cube_l(name, (-w * 0.32, -L, -w * 0.32), (w * 0.64, L * 0.47, w * 0.64), mat)


ti = 0
for z in (-54, -40, -26, -12):
    for sx in (-1, 1):
        tuft('mane', f'mt{ti}', (sx * 12, H(98), z), 13, 11, -40, sx * -20); ti += 1
        tuft('mane', f'mt{ti}', (sx * 28, H(82), z + 3), 12, 11, -12, sx * -70); ti += 1
        tuft('mane', f'mt{ti}', (sx * 28, H(56), z - 3), 12, 10, 12, sx * -100); ti += 1
for x in (-20, -7, 7, 20):
    tuft('mane', f'mt{ti}', (x, H(36), -46 + abs(x) * 0.2), 14, 10, 168, x * 0.8, mat='mane_dark'); ti += 1
    tuft('mane', f'mt{ti}', (x * 1.05, H(78), -3), 13, 10, -72, x * 0.4); ti += 1
tuft('mane', f'mt{ti}', (0, H(100), -30), 15, 12, -32, 0); ti += 1

# ---- head (face at the front of the mane)
M.bone_w('head', 'mane', (0, H(74), -58), rot_zyx(math.radians(4), 0, 0))
ellipsoid_l(M, 'head', (0, -3, -12), (23, 23, 14), 'face', axis='z', slices=4)
ellipsoid_l(M, 'head', (0, -18, -21), (22, 5, 7), 'face_brow', axis='x', slices=5)      # brow hood over the eyes
ellipsoid_l(M, 'head', (0, 12, -29), (19, 12, 13), 'muzzle', axis='z', slices=5)       # big rounded snout
for sx in (-1, 1):
    ellipsoid_l(M, 'head', (sx * 10, 15, -33), (10, 8, 9), 'muzzle', axis='z', slices=3)   # whisker pads
for sx in (-1, 1):
    x0 = 5 if sx > 0 else -20
    M.cube_l('head', (x0, -15, -25.6), (15, 9, 1), 'eye')
    hp = [(sx * 13, -24, -8), (sx * 17, -40, -14), (sx * 16, -52, -26), (sx * 12, -57, -38)]
    hw = M.world('head')
    pts = [hw[:3, :3] @ np.array(p) + hw[:3, 3] for p in hp]
    parent = 'head'
    for i, (w0, w1) in enumerate(((10, 8), (8, 5.5), (5.5, 2.5))):
        n = f'horn_{"l" if sx > 0 else "r"}{i}'
        limb(M, n, parent, pts[i], pts[i + 1], w0, w0, w1, w1, 'horn', extend=1.5 if i else 3, slices=3)
        parent = n
for k, (x, y, z, rz) in enumerate(((-24, -8, -12, 78), (24, -8, -12, -78), (-23, 10, -12, 112), (23, 10, -12, -112))):
    M.bone(f'ft{k}', 'head', (0, 0, 0), (0, 0, rz))
    M.bones[f'ft{k}'].off = [x, y, z]
    M.cube_l(f'ft{k}', (-6, -8, -7), (12, 8, 14), 'mane_tuft')
    M.cube_l(f'ft{k}', (-4, -15, -5), (8, 7.5, 10), 'mane_tuft')

M.bone('jaw', 'head', (0, 0, 0))
M.bones['jaw'].off = [0.0, 21.0, -17.0]
ellipsoid_l(M, 'jaw', (0, 3.5, -10), (14, 4.5, 11), 'jaw', axis='z', slices=4)

# ---------------------------------------------------------------- legs
for side, sx in (('right', -1), ('left', 1)):
    A = (sx * 17, H(56), -22)
    B = (sx * 18, H(26), -20)
    C = (sx * 18, H(7), -24)
    limb(M, f'{side}_arm', 'chest', A, B, 19, 21, 15, 16, 'leg', extend=5)
    limb(M, f'{side}_fore', f'{side}_arm', B, C, 14, 15, 12, 13, 'leg', extend=2)
    M.bone_w(f'{side}_hand', f'{side}_fore', C, R0)
    ellipsoid_l(M, f'{side}_hand', (0, 3, -3), (10.5, 4, 12), 'paw', axis='z', slices=4)
    for fx in (-7, -2.4, 2.4, 7):
        ellipsoid_l(M, f'{side}_hand', (fx, 4, -14), (2.6, 3, 3), 'paw', axis='z', slices=2)
    A = (sx * 17, H(58), 44)
    B = (sx * 19, H(30), 34)
    C = (sx * 19, H(16), 52)
    D = (sx * 19, H(7), 48)
    limb(M, f'{side}_thigh', 'body', A, B, 21, 27, 15, 17, 'leg', extend=7)
    limb(M, f'{side}_shin', f'{side}_thigh', B, C, 13, 13, 11, 11, 'leg', extend=2)
    limb(M, f'{side}_meta', f'{side}_shin', C, D, 11, 11, 11, 11, 'leg', extend=2, slices=2)
    M.bone_w(f'{side}_paw', f'{side}_meta', D, R0)
    ellipsoid_l(M, f'{side}_paw', (0, 3, -2), (9.5, 4, 11), 'paw', axis='z', slices=4)
    for fx in (-6.3, -2.1, 2.1, 6.3):
        ellipsoid_l(M, f'{side}_paw', (fx, 4, -12), (2.3, 3, 2.8), 'paw', axis='z', slices=2)

# ---------------------------------------------------------------- tail
TP = [(0, 62, 58), (0, 72, 72), (0, 92, 82), (0, 116, 86), (0, 138, 84), (0, 154, 76), (0, 160, 64)]
TP = [(x, H(h), z) for x, h, z in TP]
TW = (10, 8.5, 7.5, 6.5, 5.5, 4.5, 3.5)
parent = 'body'
for i in range(6):
    limb(M, f'tail{i}', parent, TP[i], TP[i + 1], TW[i], TW[i], TW[i + 1], TW[i + 1], 'tail', extend=1.5, slices=2)
    parent = f'tail{i}'
for k, (ry, rz) in enumerate(((90, 0), (90, 35))):
    n = f'tuft{k}'
    M.bone(n, 'tail3', (0, 0, 0), (0, ry, rz))
    M.bones[n].off = [0.0, 6.0, 0.0]
    M.cube_l(n, (0, -10, 0), (30, 22, 0), 'feather', plane=True)

# ---------------------------------------------------------------- textures (world-space)
P = Painter(M)
MANE = [(10, 11, 19), (22, 25, 38), (38, 43, 64), (66, 76, 106)]
GREY = [(70, 74, 88), (102, 107, 121), (134, 139, 152), (168, 172, 184)]
DARK = [(16, 18, 27), (30, 33, 47), (48, 53, 72), (70, 77, 102)]
N1, N2, N3, N4 = Noise3(11, freq=0.10), Noise3(12, freq=0.22), Noise3(13, freq=0.06), Noise3(14, freq=0.35)


def world_mat(fn):
    def wrapped(c, face, W, H_, r):
        Pw, n = texel_world(M, c, face, W, H_)
        col, alpha, glow = fn(c, face, Pw, n, W, H_)
        L = light(n)
        under = 0.82 if n[1] > 0.6 else 1.0
        col = col * L * under
        return np.clip(col, 0, 255), alpha if alpha is not None else np.ones((H_, W)), glow
    return wrapped


def hair(Pw, n, base, amp=0.3):
    # streaks flow down and back over the mane
    st = np.sin(Pw[..., 0] * 0.55 + Pw[..., 2] * 0.35 + N1(Pw) * 7.0)
    t = base + 0.18 * (N2(Pw) - 0.5) + amp * 0.5 * smooth(0.35, 0.9, st)
    t = t - 0.12 * smooth(0.6, 0.9, N4(Pw))           # dark clumps
    h = (24 - Pw[..., 1])
    t = t + 0.18 * smooth(70, 104, h)                  # sheen on top
    return ramp(t, MANE)


def mane_mat(lightness, tip=False):
    @world_mat
    def f(c, face, Pw, n, W, H_):
        col = hair(Pw, n, lightness)
        if tip:
            yy = np.mgrid[0:H_, 0:W][0] / max(H_ - 1, 1)
            col = lerp(col, MANE[3], 0.0 + 0.0 * yy)
        return col, None, None
    return f


P.mats['mane'] = mane_mat(0.40)
P.mats['mane_dark'] = mane_mat(0.28)
P.mats['mane_tuft'] = mane_mat(0.44)
P.mats['face'] = mane_mat(0.36)
P.mats['face_brow'] = mane_mat(0.26)


def stripes(Pw, n):
    s = np.sin(Pw[..., 2] * 0.30 + N3(Pw) * 5.0 + np.abs(Pw[..., 0]) * 0.08)
    side = smooth(0.15, 0.55, np.abs(n[0])) + smooth(0.4, 0.8, -n[1])
    return smooth(0.45, 0.62, s) * np.clip(side, 0, 1)


@world_mat
def body_mat(c, face, Pw, n, W, H_):
    t = 0.55 + 0.16 * (N2(Pw) - 0.5)
    h = 24 - Pw[..., 1]
    t = t - 0.12 * smooth(46, 30, h)                  # darker belly
    col = ramp(t, GREY)
    col = lerp(col, (64, 68, 84), stripes(Pw, n) * 0.85)
    return col, None, None


P.mats['body'] = body_mat
P.mats['chest'] = body_mat


@world_mat
def leg_mat(c, face, Pw, n, W, H_):
    h = 24 - Pw[..., 1]
    t = 0.55 + 0.14 * (N2(Pw) - 0.5)
    col = ramp(t, GREY)
    col = lerp(col, (64, 68, 84), stripes(Pw, n) * 0.7 * smooth(26, 36, h))
    dark = ramp(0.45 + 0.2 * (N2(Pw) - 0.5), DARK)
    return lerp(col, dark, smooth(32, 20, h)), None, None


P.mats['leg'] = leg_mat


@world_mat
def paw_mat(c, face, Pw, n, W, H_):
    return ramp(0.42 + 0.2 * (N2(Pw) - 0.5) + 0.15 * smooth(0, 7, 24 - Pw[..., 1]), DARK), None, None


P.mats['paw'] = paw_mat
HEADW = M.world('head')


def head_local(Pw):
    return (Pw - HEADW[:3, 3]) @ HEADW[:3, :3]


@world_mat
def muzzle_mat(c, face, Pw, n, W, H_):
    L = head_local(Pw)
    t = 0.62 + 0.1 * (N2(Pw) - 0.5) - 0.18 * smooth(14, 26, L[..., 1])     # darker underside
    t = t - 0.1 * smooth(12, 22, np.abs(L[..., 0]))                       # rounder: darker sides
    col = ramp(t, GREY)
    # nose pad + nostrils at the front top of the snout
    for nx in (-4.5, 4.5):
        d = np.hypot((L[..., 0] - nx) / 2.4, (L[..., 1] - 5) / 1.6)
        col[(d < 1) & (L[..., 2] < -38)] = (36, 38, 50)
    # mouth line between the whisker pads
    ml = (np.abs(L[..., 0]) < 0.9) & (L[..., 1] > 10) & (L[..., 2] < -39)
    col[ml] = (40, 42, 54)
    return col, None, None


P.mats['muzzle'] = muzzle_mat


@world_mat
def jaw_mat(c, face, Pw, n, W, H_):
    col = ramp(0.48 + 0.08 * (N2(Pw) - 0.5), GREY)
    if face == 'top':
        L = head_local(Pw)
        col[:] = (74, 18, 28)
        tongue = np.hypot(L[..., 0] / 6, (L[..., 2] + 30) / 6) < 1
        col[tongue] = (156, 56, 68)
        teeth = (np.abs(np.abs(L[..., 0]) - 9) < 1.6) & (L[..., 2] < -34)
        col[teeth] = (240, 238, 228)
    return col, None, None


P.mats['jaw'] = jaw_mat
HORN_ROOTS = {s: np.array(M.world(f'horn_{s}0')[:3, 3]) for s in 'lr'}


@world_mat
def horn_mat(c, face, Pw, n, W, H_):
    root = HORN_ROOTS['l' if Pw[..., 0].mean() > 0 else 'r']
    d = np.linalg.norm(Pw - root, axis=-1) / 42
    col = lerp((184, 118, 66), (98, 54, 30), np.clip(d, 0, 1))
    rings = smooth(0.7, 0.9, np.sin(d * 34))
    col = col * (1 - 0.18 * rings[..., None])
    return col, None, None


P.mats['horn'] = horn_mat


@world_mat
def tail_mat(c, face, Pw, n, W, H_):
    return ramp(0.6 + 0.1 * (N2(Pw) - 0.5), GREY), None, None


P.mats['tail'] = tail_mat


@P.mat('eye')
def eye(c, face, W, H_, r):
    yy, xx = np.mgrid[0:H_, 0:W]
    col = np.zeros((H_, W, 3), np.float32)
    col[:] = (20, 22, 34)
    glow = np.zeros((H_, W, 4), np.float32)
    if face == 'north':
        left = c.lfrm[0] < 0
        u = (xx + 0.5) / W
        v = (yy + 0.5) / H_
        inner = 1 - u if left else u
        center = 0.55 + 0.18 * (inner - 0.5) * 2
        half = 0.32 * np.sin(np.clip(u, 0.02, 0.98) * math.pi) ** 0.7
        m = np.abs(v - center) < half
        rim = (np.abs(v - center) < half + 0.16) & ~m
        col[rim] = (56, 62, 86)
        col[m] = (236, 238, 246)
        glow = glow_img(H_, W, (220, 228, 255), m * 1.0)
    return col, np.ones((H_, W)), glow


@P.mat('feather')
def feather(c, face, W, H_, r):
    yy, xx = np.mgrid[0:H_, 0:W]
    col = np.zeros((H_, W, 3), np.float32)
    alpha = np.zeros((H_, W))
    if face in ('north', 'south'):
        u = 1 - (xx + 0.5) / W
        v = (yy + 0.5) / H_
        top = 0.5 - 0.42 * (u ** 0.6)
        bot = 0.5 + 0.30 * (u ** 0.8) + 0.08 * np.abs(np.sin(u * 22)) * u
        m = (v > top) & (v < bot) & (u > 0.03)
        alpha[m] = 1
        n = noise2(H_, W, r, 3)
        t = 0.45 + 0.3 * n + 0.25 * (1 - v)
        col[:] = ramp(t, [(98, 10, 16), (176, 28, 34), (226, 66, 58)])
        vein = np.abs(v - (top + bot) / 2) < 0.04
        col[vein & m] = (110, 14, 18)
    return col, alpha, None


# ---------------------------------------------------------------- animations
LEGS = ['right_arm', 'right_fore', 'right_hand', 'left_arm', 'left_fore', 'left_hand',
        'right_thigh', 'right_shin', 'right_meta', 'right_paw', 'left_thigh', 'left_shin', 'left_meta', 'left_paw']
TAIL = [f'tail{i}' for i in range(6)]
CORE = ['body', 'chest', 'mane', 'head', 'jaw']
ALL = CORE + LEGS + TAIL
A = {}


def leg_pose(s, amp, lift, front):
    """s in [0,1): 0..0.5 stance (foot moves front->back), 0.5..1 swing (back->front, lifted)"""
    if s < 0.5:
        k = s / 0.5
        up = -amp + 2 * amp * k
        bend = 0.0
    else:
        k = (s - 0.5) / 0.5
        e = 0.5 - 0.5 * math.cos(k * math.pi)
        up = amp - 2 * amp * e
        bend = lift * math.sin(k * math.pi)
    if front:
        return {'arm': up - bend * 0.25, 'fore': bend, 'hand': -up - bend * 0.55}
    return {'thigh': up - bend * 0.2, 'shin': bend * 0.9, 'meta': -bend * 0.9, 'paw': -up + bend * 0.15}


def gait(name, L, amp, lift, phases, bob, spine=0.0, headbob=0.0, tailwave=8, steps=12, gallop=False):
    a = Anim(name, L, loop=True)
    frames = []
    pos = []
    for i in range(steps + 1):
        t = L * i / steps
        u = i / steps
        f = {}
        for leg, ph in phases.items():
            s = (u + ph) % 1.0
            front = leg in ('right_f', 'left_f')
            side = leg.split('_')[0]
            p = leg_pose(s, amp, lift, front)
            for k, v in p.items():
                f[f'{side}_{k}'] = (v, 0, 0)
        if gallop:
            fl = math.sin(2 * math.pi * u)
            f['body'] = (spine * fl, 0, 0)
            f['chest'] = (-spine * fl * 1.2, 0, 0)
            y = bob * max(0.0, math.sin(2 * math.pi * u + 0.6))
        else:
            f['body'] = (0, 0, 1.6 * math.sin(2 * math.pi * u))
            f['chest'] = (0, 0, -1.2 * math.sin(2 * math.pi * u))
            y = bob * (0.5 + 0.5 * math.cos(4 * math.pi * u))
        f['mane'] = (headbob * 0.4 * math.sin(4 * math.pi * u + 1.0), 0, 0)
        f['head'] = (headbob * math.sin(4 * math.pi * u + 0.5), 0, 0)
        f['jaw'] = (4 if gallop else 0, 0, 0)
        for k, tb in enumerate(TAIL):
            f[tb] = (-3 * math.sin(2 * math.pi * u - k * 0.5) - (6 if gallop and k < 2 else 0), 0,
                     tailwave * math.sin(2 * math.pi * u - k * 0.7))
        frames.append((t, f))
        pos.append((t, (0, y, 0)))
    pose_seq(a, frames, ALL)
    a.pos('base', *pos)
    return a


# ---- idle
a = Anim('idle', 4.0, loop=True)
fr = []
for i in range(9):
    u = i / 8
    t = 4.0 * u
    w = 2 * math.pi * u
    f = {'body': (0.6 * math.sin(w), 0, 0), 'chest': (-1.4 * math.sin(w), 0, 0), 'mane': (1.6 * math.sin(w + 0.4), 0, 0),
         'head': (2.5 * math.sin(w + 0.8), 6 * math.sin(w * 0.5 + 0.3) if i < 8 else 6 * math.sin(0.3), 0),
         'jaw': (2 + 2 * math.sin(w + 1.2), 0, 0)}
    for k, tb in enumerate(TAIL):
        f[tb] = (-2.5 * math.sin(w - k * 0.6), 0, 7 * math.sin(w - k * 0.8))
    fr.append((t, f))
fr[-1] = (4.0, dict(fr[0][1]))
pose_seq(a, fr, ALL)
a.pos('base', *[(4.0 * i / 8, (0, -0.8 * math.sin(2 * math.pi * i / 8), 0)) for i in range(9)])
a.scl('chest', *[(4.0 * i / 8, (1 + 0.015 * math.sin(2 * math.pi * i / 8),) * 3) for i in range(9)])
A['IDLE'] = a

A['WALK'] = gait('walk', 1.3, 20, 34, {'right_f': 0.0, 'left_h': 0.05, 'left_f': 0.5, 'right_h': 0.55}, bob=1.6, headbob=2.5)
A['RUN'] = gait('run', 0.72, 34, 52, {'right_f': 0.0, 'left_f': 0.12, 'right_h': 0.5, 'left_h': 0.62}, bob=7, spine=9, headbob=5,
                tailwave=4, steps=12, gallop=True)

# ---- sit / stay (lying down, sphinx pose)
a = Anim('sit', 4.0, loop=True)
lie = {'right_arm': (-62, 0, 0), 'right_fore': (-18, 0, 0), 'right_hand': (80, 0, 0),
       'right_thigh': (-50, 0, -8), 'right_shin': (100, 0, 0), 'right_meta': (-60, 0, 0), 'right_paw': (10, 0, 0),
       'body': (-4, 0, 0), 'chest': (2, 0, 0), 'head': (4, 0, 0), 'jaw': (0, 0, 0)}
lie = sym(lie)
f0 = dict(lie)
f1 = dict(lie)
f1['head'] = (8, 10, 0)
f1['mane'] = (2, 0, 0)
for k, tb in enumerate(TAIL):
    f0[tb] = (40 if k == 0 else 10, 0, 10 + k * 4)
    f1[tb] = (40 if k == 0 else 12, 0, 14 + k * 4)
pose_seq(a, [(0, f0), (2.0, f1), (4.0, f0)], ALL)
a.pos('base', (0, (0, -27, 0)), (2.0, (0, -27.6, 0)), (4.0, (0, -27, 0)))
A['SIT'] = a

# ---- melee: rear slightly, right paw swipe, then bite
a = Anim('attack', 0.8)
pose_seq(a, [
    (0.0, {}),
    (0.22, {'body': (-6, 0, 0), 'chest': (-8, 6, 0), 'head': (-14, 8, 0), 'jaw': (30, 0, 0),
            'right_arm': (-70, 0, 18), 'right_fore': (-30, 0, 0), 'right_hand': (20, 0, 0)}),
    (0.36, {'body': (4, 0, 0), 'chest': (10, -10, 0), 'head': (14, -12, 0), 'jaw': (8, 0, 0),
            'right_arm': (20, 0, -10), 'right_fore': (10, 0, 0), 'right_hand': (-20, 0, 0)}),
    (0.46, {'chest': (8, -4, 0), 'head': (22, -4, 0), 'jaw': (38, 0, 0), 'right_arm': (10, 0, 0)}),
    (0.56, {'chest': (6, 0, 0), 'head': (14, 0, 0), 'jaw': (0, 0, 0)}),
    (0.8, {}),
], ALL)
a.pos('base', (0, (0, 0, 0)), (0.22, (0, 4, 4)), (0.38, (0, 0, -10)), (0.56, (0, 0, -6)), (0.8, (0, 0, 0)))
A['ATTACK'] = a

# ---- black hole: rear up on hind legs, roar to the sky, slam down and thrust head forward
a = Anim('black_hole', 2.2)
rear = {'body': (-38, 0, 0), 'chest': (-8, 0, 0), 'head': (-30, 0, 0), 'jaw': (40, 0, 0),
        'right_arm': (-50, 0, 12), 'right_fore': (-60, 0, 0), 'right_hand': (40, 0, 0),
        'right_thigh': (26, 0, 0), 'right_shin': (-10, 0, 0), 'right_meta': (10, 0, 0), 'right_paw': (12, 0, 0)}
rear = sym(rear)
for k, tb in enumerate(TAIL):
    rear[tb] = (20 if k < 2 else -8, 0, 0)
slam = sym({'body': (6, 0, 0), 'chest': (10, 0, 0), 'head': (8, 0, 0), 'jaw': (44, 0, 0),
            'right_arm': (-26, 0, 0), 'right_fore': (6, 0, 0), 'right_hand': (20, 0, 0), 'right_thigh': (-10, 0, 0)})
pose_seq(a, [
    (0.0, {}),
    (0.25, sym({'body': (8, 0, 0), 'chest': (8, 0, 0), 'head': (16, 0, 0), 'right_arm': (14, 0, 0), 'right_fore': (-10, 0, 0)})),
    (0.7, rear),
    (1.0, {**rear, 'head': (-38, 0, 0), 'jaw': (46, 0, 0)}),
    (1.2, slam),
    (1.7, {**slam, 'head': (2, 0, 0), 'jaw': (36, 0, 0)}),
    (2.2, {}),
], ALL)
a.pos('base', (0, (0, 0, 0)), (0.25, (0, -3, 0)), (0.7, (0, 10, 12)), (1.0, (0, 12, 12)), (1.2, (0, -2, -4)), (1.7, (0, -2, -4)), (2.2, (0, 0, 0)))
A['BLACK_HOLE'] = a

# ---- blink (teleport strike): sink into own shadow, vanish, burst out pouncing
a = Anim('blink', 1.3)
crouch = sym({'body': (6, 0, 0), 'chest': (6, 0, 0), 'head': (10, 0, 0), 'right_arm': (-20, 0, 0), 'right_fore': (30, 0, 0),
              'right_hand': (-10, 0, 0), 'right_thigh': (-30, 0, 0), 'right_shin': (40, 0, 0), 'right_meta': (-20, 0, 0)})
pounce = sym({'body': (-6, 0, 0), 'chest': (-6, 0, 0), 'head': (-18, 0, 0), 'jaw': (45, 0, 0), 'right_arm': (-80, 0, 10),
              'right_fore': (-20, 0, 0), 'right_hand': (30, 0, 0), 'right_thigh': (40, 0, 0), 'right_shin': (-10, 0, 0)})
strike = sym({'body': (6, 0, 0), 'chest': (12, 0, 0), 'head': (20, 0, 0), 'jaw': (10, 0, 0), 'right_arm': (10, 0, -6), 'right_fore': (10, 0, 0)})
pose_seq(a, [(0, {}), (0.3, crouch), (0.45, crouch), (0.62, pounce), (0.82, strike), (1.05, strike), (1.3, {})], ALL)
a.scl('base', (0, (1, 1, 1)), (0.3, (1.05, 0.8, 1.05)), (0.45, (0.02, 0.02, 0.02), 'L'), (0.55, (0.02, 0.02, 0.02), 'L'),
      (0.66, (1.12, 1.12, 1.12)), (0.82, (1, 1, 1)), (1.3, (1, 1, 1)))
a.pos('base', (0, (0, 0, 0)), (0.3, (0, -6, 0)), (0.55, (0, -6, 0)), (0.62, (0, 10, 6)), (0.82, (0, 0, -14)), (1.05, (0, 0, -10)), (1.3, (0, 0, 0)))
A['BLINK'] = a

# ---- energy drain: plant feet, lower head, mouth wide, body pulling back in pulses
a = Anim('drain', 3.0)
brace = sym({'body': (4, 0, 0), 'chest': (-4, 0, 0), 'mane': (-4, 0, 0), 'head': (12, 0, 0), 'jaw': (42, 0, 0),
             'right_arm': (-22, 0, 8), 'right_fore': (6, 0, 0), 'right_hand': (16, 0, 0),
             'right_thigh': (-14, 0, -4), 'right_shin': (24, 0, 0), 'right_meta': (-10, 0, 0)})
br2 = dict(brace)
br2['head'] = (8, 0, 0)
br2['jaw'] = (48, 0, 0)
br2['mane'] = (-8, 0, 0)
for k, tb in enumerate(TAIL):
    brace[tb] = (-10, 0, 0)
    br2[tb] = (-14, 0, 6 if k > 2 else 0)
frames = [(0, {}), (0.4, brace)]
tt = 0.4
while tt < 2.6:
    frames.append((min(tt + 0.25, 2.6), br2))
    frames.append((min(tt + 0.5, 2.6), brace))
    tt += 0.5
frames.append((3.0, {}))
pose_seq(a, frames, ALL)
a.pos('base', (0, (0, 0, 0)), (0.4, (0, -3, 6)), (2.6, (0, -3, 6)), (3.0, (0, 0, 0)))
a.scl('mane', (0, (1, 1, 1)), (0.4, (1.08, 1.08, 1.04)), (2.6, (1.08, 1.08, 1.04)), (3.0, (1, 1, 1)))
A['DRAIN'] = a

# ---- reflect: crouched guard, mane flared, head low, front paw raised forward as a ward
a = Anim('reflect', 3.5)
ward = sym({'body': (8, 0, 0), 'chest': (6, 0, 0), 'mane': (-6, 0, 0), 'head': (18, 0, 0), 'jaw': (14, 0, 0),
            'right_thigh': (-24, 0, -6), 'right_shin': (36, 0, 0), 'right_meta': (-14, 0, 0)})
ward['right_arm'] = (-60, 0, 14)
ward['right_fore'] = (-30, 0, 0)
ward['right_hand'] = (60, 0, 0)
ward['left_arm'] = (-10, 0, -6)
for k, tb in enumerate(TAIL):
    ward[tb] = (30 if k == 0 else 18, 0, 0)
w2 = dict(ward)
w2['head'] = (14, 6, 0)
w2['jaw'] = (24, 0, 0)
pose_seq(a, [(0, {}), (0.25, ward), (1.2, w2), (2.2, ward), (3.1, w2), (3.5, {})], ALL)
a.scl('mane', (0, (1, 1, 1)), (0.25, (1.15, 1.15, 1.1)), (3.1, (1.15, 1.15, 1.1)), (3.5, (1, 1, 1)))
a.pos('base', (0, (0, 0, 0)), (0.25, (0, -7, 0)), (3.1, (0, -7, 0)), (3.5, (0, 0, 0)))
A['REFLECT'] = a

# ---- shadow attack: rise on the forelegs and stomp the ground three times, roaring
a = Anim('shadow', 2.0)
lift_ = sym({'body': (-14, 0, 0), 'chest': (-12, 0, 0), 'head': (-16, 0, 0), 'jaw': (30, 0, 0),
             'right_arm': (-48, 0, 0), 'right_fore': (-30, 0, 0), 'right_hand': (30, 0, 0)})
stomp = sym({'body': (4, 0, 0), 'chest': (10, 0, 0), 'head': (14, 0, 0), 'jaw': (44, 0, 0),
             'right_arm': (-6, 0, 10), 'right_fore': (4, 0, 0), 'right_hand': (0, 0, 0)})
pose_seq(a, [(0, {}), (0.45, lift_), (0.7, stomp), (0.92, lift_), (1.1, stomp), (1.32, lift_), (1.5, stomp), (2.0, {})], ALL)
a.pos('base', (0, (0, 0, 0)), (0.45, (0, 8, 4)), (0.7, (0, -2, 0)), (0.92, (0, 6, 2)), (1.1, (0, -2, 0)), (1.32, (0, 6, 2)), (1.5, (0, -3, 0)), (2.0, (0, 0, 0)))
A['SHADOW'] = a

# ---- guard: hunker down, mane bristles up into armour
a = Anim('guard', 1.5)
hunker = sym({'body': (2, 0, 0), 'chest': (4, 0, 0), 'mane': (-10, 0, 0), 'head': (24, 0, 0), 'jaw': (20, 0, 0),
              'right_arm': (-18, 0, 12), 'right_fore': (24, 0, 0), 'right_hand': (-6, 0, 0),
              'right_thigh': (-28, 0, -8), 'right_shin': (44, 0, 0), 'right_meta': (-16, 0, 0)})
for k, tb in enumerate(TAIL):
    hunker[tb] = (35 if k == 0 else 20, 0, 0)
pose_seq(a, [(0, {}), (0.3, hunker), (0.55, {**hunker, 'head': (-10, 0, 0), 'jaw': (46, 0, 0)}), (1.0, hunker), (1.5, {})], ALL)
a.scl('mane', (0, (1, 1, 1)), (0.3, (1.12, 1.2, 1.1)), (0.55, (1.2, 1.25, 1.15)), (1.0, (1.12, 1.18, 1.1)), (1.5, (1, 1, 1)))
a.pos('base', (0, (0, 0, 0)), (0.3, (0, -9, 0)), (1.0, (0, -9, 0)), (1.5, (0, 0, 0)))
A['GUARD'] = a

# ---- slam: crouch, huge leap, crash down front-first
a = Anim('slam', 2.0)
pose_seq(a, [
    (0, {}),
    (0.4, crouch),
    (0.55, pounce),
    (0.95, sym({'body': (-10, 0, 0), 'chest': (-10, 0, 0), 'head': (-20, 0, 0), 'jaw': (30, 0, 0), 'right_arm': (-90, 0, 14),
                'right_fore': (-10, 0, 0), 'right_hand': (40, 0, 0), 'right_thigh': (50, 0, 0), 'right_shin': (-20, 0, 0)})),
    (1.2, sym({'body': (10, 0, 0), 'chest': (16, 0, 0), 'head': (18, 0, 0), 'jaw': (46, 0, 0), 'right_arm': (-10, 0, 22),
               'right_fore': (14, 0, 0), 'right_thigh': (-30, 0, -6), 'right_shin': (40, 0, 0), 'right_meta': (-12, 0, 0)})),
    (1.6, sym({'body': (6, 0, 0), 'chest': (8, 0, 0), 'head': (-12, 0, 0), 'jaw': (50, 0, 0), 'right_arm': (-6, 0, 14)})),
    (2.0, {}),
], ALL)
a.pos('base', (0, (0, 0, 0)), (0.4, (0, -8, 0)), (0.55, (0, 6, 0)), (1.2, (0, -8, 0)), (1.6, (0, -4, 0)), (2.0, (0, 0, 0)))
A['SLAM'] = a

# ---- dodge: snap sideways into a shadow blink
a = Anim('dodge', 0.45)
pose_seq(a, [(0, {}), (0.12, crouch), (0.3, crouch), (0.45, {})], ALL)
a.scl('base', (0, (1, 1, 1)), (0.12, (0.7, 0.5, 0.7)), (0.18, (0.05, 0.05, 0.05), 'L'), (0.26, (0.05, 0.05, 0.05), 'L'), (0.4, (1, 1, 1)), (0.45, (1, 1, 1)))
A['DODGE'] = a

# ---- release from the marble: grow out of a point, shake the mane and roar
a = Anim('release', 1.8)
roar = sym({'body': (-8, 0, 0), 'chest': (-10, 0, 0), 'mane': (-6, 0, 0), 'head': (-26, 0, 0), 'jaw': (50, 0, 0),
            'right_arm': (-14, 0, 8), 'right_thigh': (8, 0, 0)})
for k, tb in enumerate(TAIL):
    roar[tb] = (-12, 0, 10 * (1 if k % 2 else -1))
pose_seq(a, [(0, crouch), (0.45, crouch), (0.7, {}), (0.95, roar), (1.2, {**roar, 'head': (-30, 8, 0)}), (1.45, {**roar, 'head': (-26, -8, 0)}), (1.8, {})], ALL)
a.scl('base', (0, (0.02, 0.02, 0.02)), (0.35, (1.1, 1.1, 1.1)), (0.55, (0.96, 0.96, 0.96)), (0.7, (1, 1, 1)), (1.8, (1, 1, 1)))
a.scl('mane', (0, (1, 1, 1)), (0.9, (1, 1, 1)), (1.05, (1.14, 1.14, 1.1)), (1.5, (1.1, 1.1, 1.08)), (1.8, (1, 1, 1)))
A['RELEASE'] = a

# ---- recall into the marble: collapse and shrink into a point
a = Anim('recall', 1.25)
lie2 = dict(lie)
pose_seq(a, [(0, {}), (0.45, lie2), (1.25, lie2)], ALL)
a.pos('base', (0, (0, 0, 0)), (0.45, (0, -20, 0)), (1.25, (0, -20, 0)))
a.scl('base', (0, (1, 1, 1)), (0.5, (1, 1, 1)), (1.0, (0.05, 0.05, 0.05)), (1.25, (0.01, 0.01, 0.01)))
A['RECALL'] = a

# ---- death (wild Kabkaba): stagger, collapse onto its side
a = Anim('death', 3.0)
dead = sym({'body': (0, 0, 0), 'head': (10, 0, 0), 'jaw': (30, 0, 0), 'right_arm': (-30, 0, 0), 'right_fore': (20, 0, 0),
            'right_thigh': (-20, 0, 0), 'right_shin': (20, 0, 0)})
for k, tb in enumerate(TAIL):
    dead[tb] = (50 if k == 0 else 10, 0, 0)
pose_seq(a, [
    (0, {}),
    (0.4, sym({'body': (-10, 0, 0), 'chest': (-8, 0, 0), 'head': (-34, 0, 0), 'jaw': (50, 0, 0), 'right_arm': (-30, 0, 0)})),
    (0.9, sym({'body': (6, 0, 0), 'chest': (14, 0, 0), 'head': (20, 0, 0), 'jaw': (20, 0, 0), 'right_arm': (30, 0, 0), 'right_fore': (40, 0, 0)})),
    (1.6, dead),
    (3.0, dead),
], ALL)
a.rot('base', (0, (0, 0, 0)), (0.9, (0, 0, 0)), (1.6, (0, 0, 90)), (3.0, (0, 0, 90)))
a.pos('base', (0, (0, 0, 0)), (0.9, (0, -6, 0)), (1.6, (0, 26, 0)), (3.0, (0, 26, 0)))
A['DEATH'] = a
