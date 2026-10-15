"""Xenomorph — biomechanical stalker: model, HD textures and keyframe animations."""
import math, numpy as np
from core import Model, Anim
from hd import HDPainter, vnoise, mix, long_axis
from animlib import pose_seq, sym

S = 4
M = Model('xenomorph', 128, 128)

# ------------------------------------------------------------------ skeleton (designed unrotated, posed by rest rotations)
M.bone('base', None, (0, 24, 0))
M.bone('hips', 'base', (0, 5, 1.5), (8, 0, 0))
M.cube('hips', (-3.4, 3, -0.8), (6.8, 5, 5.2), 'ribbed')
M.cube('hips', (-2.2, 7.4, -0.2), (4.4, 2.2, 4), 'skin')

M.bone('waist', 'hips', (0, 4, 1.5), (14, 0, 0))
M.cube('waist', (-2.3, -4.5, -0.6), (4.6, 8.5, 4.2), 'ribbed')
M.cube('waist', (-1.0, -4.5, 3.2), (2, 8.5, 1.2), 'spine')

M.bone('chest', 'waist', (0, -3.5, 1.5), (16, 0, 0))
M.cube('chest', (-3.9, -13.5, -2.4), (7.8, 10.5, 6.8), 'ribs')
M.cube('chest', (-1.6, -12.5, -2.9), (3.2, 9, 1), 'sternum')
M.cube('chest', (-5.4, -15.2, -1.8), (10.8, 3.2, 5.6), 'skin')
M.cube('chest', (-3.2, -15, 3.8), (6.4, 10, 1.8), 'spine')

# dorsal tubes (the four back pipes)
for i, (x, y, rx, rz, L) in enumerate(((-2.0, -13.6, -100, 16, 8.5), (2.0, -13.6, -100, -16, 8.5),
                                       (-3.3, -10.6, -88, 34, 7.0), (3.3, -10.6, -88, -34, 7.0))):
    n = f'tube{i}'
    M.bone(n, 'chest', (x, y, 4.6), (rx, 0, rz))
    M.cube_l(n, (-0.9, -L, -0.9), (1.8, L, 1.8), 'tube')
    M.cube_l(n, (-0.6, -L - 1.6, -0.6), (1.2, 1.8, 1.2), 'tube_tip')

M.bone('neck', 'chest', (0, -14.5, 0.6), (26, 0, 0))
M.cube('neck', (-1.5, -19.2, -1.0), (3, 5.2, 3.2), 'ribbed')

M.bone('head', 'neck', (0, -18.6, 0.4), (-50, 0, 0))
M.cube('head', (-2.1, -20.3, -6.2), (4.2, 3.4, 7.6), 'face')
M.cube('head', (-1.9, -21.9, -4.6), (3.8, 1.9, 15.5), 'skull_ridge')
M.cube('head', (-2.55, -22.4, -6.8), (5.1, 3.2, 14.2), 'dome', inflate=0.12)
M.cube('head', (-1.95, -22.0, 7.2), (3.9, 2.5, 5.2), 'dome', inflate=0.08)
M.cube('head', (-1.75, -17.2, -6.75), (3.5, 1.15, 1.3), 'teeth')
M.cube('head', (-2.3, -17.6, -5.6), (0.7, 1.4, 4.2), 'lip')
M.cube('head', (1.6, -17.6, -5.6), (0.7, 1.4, 4.2), 'lip')

M.bone('jaw', 'head', (0, -17.0, -0.6), (6, 0, 0))
M.cube('jaw', (-1.65, -17.0, -6.1), (3.3, 1.5, 5.8), 'jaw')
M.cube('jaw', (-1.55, -17.7, -6.4), (3.1, 0.8, 1.1), 'teeth')

M.bone('inner_jaw', 'head', (0, -17.6, -1.6))
M.cube('inner_jaw', (-0.65, -18.1, -5.3), (1.3, 1.1, 4.6), 'inner')
M.cube('inner_jaw', (-0.8, -18.25, -6.1), (1.6, 1.4, 0.9), 'teeth')

# arms
for side, sx in (('right', -1), ('left', 1)):
    ax = sx * 5.1
    M.bone(f'{side}_arm', 'chest', (ax, -13.6, 0.8), (-18, 0, sx * -10))
    M.cube(f'{side}_arm', (ax - 1.6, -15.2, -0.6), (3.2, 3.2, 2.8), 'skin')
    M.cube(f'{side}_arm', (ax - 1.15, -13.6, -0.35), (2.3, 10.6, 2.3), 'limb')
    M.bone(f'{side}_fore', f'{side}_arm', (ax, -3.4, 0.8), (-42, 0, sx * 4))
    M.cube(f'{side}_fore', (ax - 0.95, -3.6, -0.2), (1.9, 10.4, 2.0), 'limb')
    M.cube(f'{side}_fore', (ax - 0.35, -4.4, 1.6), (0.7, 3.4, 1.4), 'claw')
    M.bone(f'{side}_hand', f'{side}_fore', (ax, 6.8, 0.8), (-6, 0, 0))
    M.cube(f'{side}_hand', (ax - 1.3, 6.6, -0.2), (2.6, 2.7, 2.0), 'limb')
    M.bone(f'{side}_fingers', f'{side}_hand', (ax, 9.2, 0.6), (-20, 0, 0))
    for k in range(3):
        fx = ax - 1.25 + k * 0.92
        M.cube(f'{side}_fingers', (fx, 9.0, -0.15), (0.66, 5.6, 0.7), 'finger')
    M.cube(f'{side}_fingers', (ax - sx * 1.25 - 0.3, 8.6, -0.9), (0.6, 3.6, 0.6), 'finger')

# legs (long, back bent shins, heel spur)
for side, sx in (('right', -1), ('left', 1)):
    lx = sx * 2.7
    M.bone(f'{side}_thigh', 'hips', (lx, 6.6, 1.6), (-36, 0, sx * -3))
    M.cube(f'{side}_thigh', (lx - 1.7, 5.6, -0.4), (3.4, 11.8, 3.8), 'limb_leg')
    M.bone(f'{side}_shin', f'{side}_thigh', (lx, 17.2, 1.6), (62, 0, 0))
    M.cube(f'{side}_shin', (lx - 1.25, 17.0, 0.2), (2.5, 9.6, 2.8), 'limb_leg')
    M.bone(f'{side}_spur', f'{side}_shin', (lx, 18.6, 2.8), (-22, 0, 0))
    M.cube_l(f'{side}_spur', (-0.35, -5.2, -0.5), (0.7, 5.4, 1.0), 'claw')
    M.bone(f'{side}_foot', f'{side}_shin', (lx, 26.3, 1.6), (-34, 0, 0))
    M.cube(f'{side}_foot', (lx - 1.45, 26.1, -3.6), (2.9, 1.6, 6.0), 'limb_leg')
    M.cube(f'{side}_foot', (lx - 1.6, 26.5, -5.7), (3.2, 1.2, 2.2), 'claw')
    M.cube(f'{side}_foot', (lx - 0.35, 26.3, 2.2), (0.7, 1.0, 1.6), 'claw')

# tail: chain of shrinking vertebrae, blade at the tip
TAIL_L = [6.2, 6.0, 5.6, 5.4, 5.2, 5.0, 4.6, 4.4, 4.2]
TAIL_W = [2.6, 2.35, 2.1, 1.9, 1.7, 1.5, 1.3, 1.15, 1.0]
TAIL_R = [-52, 12, 10, 9, 6, 4, -4, -10, -16]
z = 4.6
for i, (L, w, rx) in enumerate(zip(TAIL_L, TAIL_W, TAIL_R)):
    n = f'tail{i}'
    M.bone(n, 'hips' if i == 0 else f'tail{i - 1}', (0, 6.0, z), (rx, 0, 0))
    M.cube(n, (-w / 2, 6.0 - w / 2, z - 0.3), (w, w, L + 0.3), 'tail')
    if i < 7:
        M.cube(n, (-0.25, 6.0 - w / 2 - 0.9, z + 0.6), (0.5, 1.0, L - 1.4), 'spine')
    z += L
M.cube('tail8', (-0.22, 4.4, z - 0.6), (0.44, 3.2, 5.4), 'blade')

# lift whole body so the lowest foot touches the ground (y = 24)
def lowest():
    lo = -1e9
    for n in M.order:
        W = M.world(n)
        for c in M.bones[n].cubes:
            if not n.endswith('_foot'):
                continue
            for dx in (0, c.size[0]):
                for dy in (0, c.size[1]):
                    for dz in (0, c.size[2]):
                        p = W @ np.array([c.lfrm[0] + dx, c.lfrm[1] + dy, c.lfrm[2] + dz, 1.0])
                        lo = max(lo, p[1])
    return lo


_shift = 24.0 - lowest()
M.bones['hips'].off[1] += _shift


# ------------------------------------------------------------------ textures
P = HDPainter(M, S)
DARK = (17, 16, 15)
MID = (44, 41, 36)
HI = (128, 124, 108)
OLIVE = (66, 64, 50)


def gloss(W, H, r, across, period, sharp=3.0, base=DARK, hi=HI, noise_amt=0.25):
    """tubular ridges running along the long axis with wet specular highlights"""
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    a = xx if across == 'x' else yy
    ph = r.random() * 6.28
    wob = (vnoise(H, W, r, cell=16) - 0.5) * 9.0 + (vnoise(H, W, r, cell=5) - 0.5) * 2.5
    s = np.sin((a + wob) * 2 * math.pi / (period * 2.2) + ph)
    ridge = np.clip(s, 0, 1) ** sharp
    groove = np.clip(-s, 0, 1) ** 2
    n = vnoise(H, W, r, cell=5)
    col = mix(base, MID, n * 0.7 + 0.15)
    col = mix(col, OLIVE, vnoise(H, W, r, cell=12) * 0.45)
    col = mix(col, (6, 6, 6), groove * 0.7)
    col = mix(col, hi, ridge * (0.45 + noise_amt * n))
    return col


def rings(W, H, r, along, period, base=DARK):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    a = yy if along == 'y' else xx
    wob = (vnoise(H, W, r, cell=12) - 0.5) * 4.0
    s = np.sin((a + wob) * 2 * math.pi / (period * 2.0) + r.random())
    col = mix(base, MID, vnoise(H, W, r, cell=4) * 0.6 + 0.2)
    col = mix(col, (5, 5, 5), np.clip(-s, 0, 1) ** 3 * 0.85)
    col = mix(col, HI, np.clip(s, 0, 1) ** 6 * 0.6)
    return col


def edge_dark(col, W, H, k=0.35):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    e = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy))
    t = np.clip(1 - e / 3.0, 0, 1)
    return mix(col, (4, 4, 4), t * k)


def vert(face, c):
    return long_axis(face, c)


@P.mat('skin')
def skin(c, face, W, H, r):
    across = 'x' if vert(face, c) else 'y'
    return edge_dark(gloss(W, H, r, across, 7.5), W, H), 1.0, None


@P.mat('limb')
def limb(c, face, W, H, r):
    across = 'x' if vert(face, c) else 'y'
    col = gloss(W, H, r, across, 4.5, sharp=4)
    # joint bands every ~10 units along the bone
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    along = yy if across == 'x' else xx
    band = np.exp(-((along % 40) - 4) ** 2 / 8.0)
    col = mix(col, (8, 8, 8), band * 0.6)
    return edge_dark(col, W, H, 0.25), 1.0, None


@P.mat('limb_leg')
def limb_leg(c, face, W, H, r):
    across = 'x' if vert(face, c) else 'y'
    col = gloss(W, H, r, across, 5.0, sharp=3)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    along = yy if across == 'x' else xx
    ten = np.sin(along * 2 * math.pi / 9.0) * 0.5 + 0.5
    col = mix(col, MID, ten * 0.18)
    return edge_dark(col, W, H, 0.25), 1.0, None


@P.mat('finger')
def finger(c, face, W, H, r):
    col = rings(W, H, r, 'y', 5.0)
    yy = np.mgrid[0:H, 0:W][0].astype(np.float32)
    col = mix(col, (2, 2, 2), np.clip((yy - H * 0.7) / (H * 0.3), 0, 1))
    return col, 1.0, None


@P.mat('ribbed')
def ribbed(c, face, W, H, r):
    along = 'y' if vert(face, c) else 'x'
    col = rings(W, H, r, along, 3.6)
    col = mix(col, gloss(W, H, r, 'x' if along == 'y' else 'y', 6.0), 0.35)
    return edge_dark(col, W, H, 0.3), 1.0, None


@P.mat('ribs')
def ribs(c, face, W, H, r):
    if face in ('top', 'bottom'):
        return edge_dark(gloss(W, H, r, 'x', 7), W, H), 1.0, None
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    # curved ribs: bands bending down towards the sides
    cx = W / 2
    bend = ((xx - cx) / max(cx, 1)) ** 2 * 5.0 if face in ('north', 'south') else (xx / max(W, 1)) * 4.0
    s = np.sin((yy - bend * 3) * 2 * math.pi / 13.0)
    col = mix(DARK, MID, vnoise(H, W, r, cell=4) * 0.6)
    col = mix(col, (3, 3, 3), np.clip(-s, 0, 1) ** 1.5 * 0.95)
    col = mix(col, HI, np.clip(s, 0, 1) ** 5 * 0.75)
    if face == 'south':
        spine = np.exp(-((xx - cx) ** 2) / 6.0)
        col = mix(col, (6, 6, 6), spine * 0.7)
    return edge_dark(col, W, H, 0.3), 1.0, None


@P.mat('sternum')
def sternum(c, face, W, H, r):
    return rings(W, H, r, 'y', 4.2, base=(24, 22, 19)), 1.0, None


@P.mat('spine')
def spine(c, face, W, H, r):
    along = 'y' if vert(face, c) else 'x'
    col = rings(W, H, r, along, 3.0, base=(12, 11, 10))
    return col, 1.0, None


@P.mat('tube')
def tube(c, face, W, H, r):
    col = rings(W, H, r, 'y', 4.0)
    col = mix(col, gloss(W, H, r, 'x', 3.5, sharp=6), 0.4)
    return edge_dark(col, W, H, 0.2), 1.0, None


@P.mat('tube_tip')
def tube_tip(c, face, W, H, r):
    return np.full((H, W, 3), (10, 10, 10), np.float32), 1.0, None


@P.mat('face')
def face_m(c, face, W, H, r):
    col = gloss(W, H, r, 'y' if face in ('west', 'east') else 'x', 5.0)
    if face == 'north':
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        col = mix(col, (3, 3, 3), np.clip((yy - H * 0.55) / (H * 0.45), 0, 1) * 0.8)
    return edge_dark(col, W, H, 0.3), 1.0, None


@P.mat('skull_ridge')
def skull_ridge(c, face, W, H, r):
    along = 'y' if vert(face, c) else 'x'
    col = rings(W, H, r, along, 3.2, base=(46, 40, 32))
    col = mix(col, (90, 80, 62), vnoise(H, W, r, cell=6) * 0.35)
    return col, 1.0, None


@P.mat('dome')
def dome(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    lng = vert(face, c)
    u = (xx / max(W - 1, 1)) if lng else (yy / max(H - 1, 1))
    t = (yy / max(H - 1, 1)) if lng else (xx / max(W - 1, 1))
    base = mix((78, 78, 70), (150, 148, 132), vnoise(H, W, r, cell=10) * 0.6 + 0.2)
    # long wet specular streak running the length of the dome
    streak = np.exp(-((u - 0.35) ** 2) / 0.012) * (0.6 + 0.4 * np.sin(t * 9 + r.random() * 6) ** 2)
    if face in ('top',):
        streak = streak * 1.3
    if face == 'bottom':
        streak = streak * 0.2
    col = mix(base, (232, 230, 220), streak * 0.85)
    col = mix(col, (30, 28, 24), np.clip(1 - np.minimum(u, 1 - u) * 6, 0, 1) * 0.4)
    alpha = np.clip(0.7 + streak * 0.28, 0, 0.98)
    return col, alpha, None


@P.mat('teeth')
def teeth(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    gap = (xx % 3 == 0) if face in ('north', 'south') else (xx % 3 == 0) if face in ('west', 'east') else (xx % 3 == 0)
    col = mix((196, 196, 190), (240, 240, 235), vnoise(H, W, r, cell=2))
    col = mix(col, (90, 88, 84), yy / max(H - 1, 1) * 0.4)
    col[gap] = (25, 22, 20)
    return col, 1.0, None


@P.mat('lip')
def lip(c, face, W, H, r):
    col = rings(W, H, r, 'x', 2.5, base=(30, 26, 24))
    return col, 1.0, None


@P.mat('jaw')
def jaw(c, face, W, H, r):
    col = gloss(W, H, r, 'y', 4.0)
    return edge_dark(col, W, H, 0.4), 1.0, None


@P.mat('inner')
def inner(c, face, W, H, r):
    col = rings(W, H, r, 'x', 3.0, base=(58, 48, 46))
    col = mix(col, (120, 112, 108), vnoise(H, W, r, cell=3) * 0.3)
    return col, 1.0, None


@P.mat('claw')
def claw(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    col = mix((8, 8, 8), (34, 33, 30), vnoise(H, W, r, cell=3))
    col = mix(col, (140, 138, 128), np.exp(-((xx - W * 0.3) ** 2) / 2.0) * 0.5)
    return col, 1.0, None


@P.mat('tail')
def tail(c, face, W, H, r):
    along_v = vert(face, c)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    a = yy if along_v else xx
    s = np.sin(a * 2 * math.pi / 11.0 + r.random())
    col = mix(DARK, MID, vnoise(H, W, r, cell=4) * 0.6 + 0.2)
    col = mix(col, (3, 3, 3), np.clip(-s, 0, 1) ** 2 * 0.9)
    col = mix(col, HI, np.clip(s, 0, 1) ** 5 * 0.55)
    return edge_dark(col, W, H, 0.3), 1.0, None


@P.mat('blade')
def blade(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    col = mix((10, 10, 10), (60, 58, 52), vnoise(H, W, r, cell=3))
    col = mix(col, (170, 168, 160), np.clip(1 - yy / 3.0, 0, 1) * 0.6)
    return col, 1.0, None


# ------------------------------------------------------------------ animations
A = {}
LEGS = ['thigh', 'shin', 'foot']
ARM = ['arm', 'fore', 'hand', 'fingers']
TAILS = [f'tail{i}' for i in range(9)]
NAMES = ['hips', 'waist', 'chest', 'neck', 'head', 'jaw'] + [f'{s}_{p}' for s in ('right', 'left') for p in LEGS + ARM] + TAILS


def tail_wave(t, amp_y, amp_x, speed, phase=0.0, lift=0.0):
    d = {}
    for i, n in enumerate(TAILS):
        k = (i + 1) / len(TAILS)
        d[n] = (lift * (1 if i == 0 else 0) + amp_x * math.sin(speed * t * 2 * math.pi - i * 0.55 + phase) * k,
                amp_y * math.sin(speed * t * 2 * math.pi - i * 0.6 + phase) * (0.4 + k), 0)
    return d


def frames(length, n, fn):
    return [(length * i / n, fn(length * i / n)) for i in range(n + 1)]


# idle: slow breathing, curious head tilts, slow tail sway, finger twitch
def idle_f(t):
    b = math.sin(t / 4.0 * 2 * math.pi)
    d = {'chest': (b * 2.5, 0, 0), 'waist': (-b * 1.0, 0, 0), 'neck': (b * -1.5, math.sin(t / 4.0 * 2 * math.pi * 0.5) * 6, 0),
         'head': (math.sin(t * 1.3) * 3, math.sin(t / 4.0 * 2 * math.pi) * 8, math.sin(t / 4.0 * 4 * math.pi) * 7),
         'jaw': (max(0, math.sin(t * 1.6)) * 6, 0, 0),
         'right_arm': (b * 3, 0, 0), 'left_arm': (-b * 3, 0, 0),
         'right_fingers': (math.sin(t * 5) * 6, 0, 0), 'left_fingers': (math.sin(t * 4 + 1) * 6, 0, 0)}
    d.update(tail_wave(t / 4.0, 12, 3, 1.0))
    return d


idle = Anim('idle', 4.0, True)
pose_seq(idle, frames(4.0, 16, idle_f), NAMES)
A['IDLE'] = idle


# stalk walk: low crouch, head level, deliberate digitigrade steps
def walk_f(t):
    p = t / 1.6 * 2 * math.pi
    s = math.sin(p); c = math.cos(p)
    d = {'hips': (6, s * 4, 0), 'waist': (6, 0, s * 2), 'chest': (10, -s * 4, 0), 'neck': (-4, 0, 0), 'head': (-10, s * 3, 0),
         'right_thigh': (-12 + s * 24, 0, 0), 'left_thigh': (-12 - s * 24, 0, 0),
         'right_shin': (14 + max(0, -c) * 30, 0, 0), 'left_shin': (14 + max(0, c) * 30, 0, 0),
         'right_foot': (-6 - s * 14, 0, 0), 'left_foot': (-6 + s * 14, 0, 0),
         'right_arm': (-10 - s * 14, 0, 0), 'left_arm': (-10 + s * 14, 0, 0),
         'right_fore': (-12, 0, 0), 'left_fore': (-12, 0, 0),
         'right_fingers': (-10, 0, 0), 'left_fingers': (-10, 0, 0)}
    d.update(tail_wave(t / 1.6, 14, 4, 1.0, lift=14))
    return d


walk = Anim('walk', 1.6, True)
pose_seq(walk, frames(1.6, 16, walk_f), NAMES)
walk.pos('base', *[(1.6 * i / 16, (0, -2.2 + abs(math.sin(i / 16 * 2 * math.pi)) * 0.8, 0)) for i in range(17)])
A['WALK'] = walk


# sprint: torso almost horizontal, arms reaching, tail straight back
def run_f(t):
    p = t / 0.7 * 2 * math.pi
    s = math.sin(p); c = math.cos(p)
    d = {'hips': (14, s * 6, 0), 'waist': (12, 0, 0), 'chest': (22, -s * 6, 0), 'neck': (-6, 0, 0), 'head': (-30, 0, 0),
         'right_thigh': (-20 + s * 42, 0, 0), 'left_thigh': (-20 - s * 42, 0, 0),
         'right_shin': (10 + max(0, -c) * 55, 0, 0), 'left_shin': (10 + max(0, c) * 55, 0, 0),
         'right_foot': (-s * 25, 0, 0), 'left_foot': (s * 25, 0, 0),
         'right_arm': (-40 - s * 36, 0, -6), 'left_arm': (-40 + s * 36, 0, 6),
         'right_fore': (-20 - max(0, s) * 30, 0, 0), 'left_fore': (-20 - max(0, -s) * 30, 0, 0),
         'right_fingers': (-25, 0, 0), 'left_fingers': (-25, 0, 0)}
    d.update(tail_wave(t / 0.7, 8, 6, 1.0, lift=22))
    return d


run = Anim('run', 0.7, True)
pose_seq(run, frames(0.7, 14, run_f), NAMES)
run.pos('base', *[(0.7 * i / 14, (0, -3.0 + abs(math.sin(i / 14 * 2 * math.pi)) * 2.0, 0)) for i in range(15)])
A['RUN'] = run

# search: stands taller, sweeps its head like a bird of prey, sniffs
search = Anim('search', 3.0)
up = {'hips': (-6, 0, 0), 'waist': (-8, 0, 0), 'chest': (-8, 0, 0), 'neck': (6, 0, 0), 'right_arm': (-14, 0, -8), 'left_arm': (-14, 0, 8),
      'right_fore': (-25, 0, 0), 'left_fore': (-25, 0, 0)}
pose_seq(search, [
    (0.0, {}),
    (0.35, {**up, 'head': (8, 0, 0)}),
    (0.9, {**up, 'neck': (6, -25, 0), 'head': (2, -30, 18)}),
    (1.15, {**up, 'neck': (6, -25, 0), 'head': (-6, -30, 22)}),
    (1.3, {**up, 'neck': (6, -25, 0), 'head': (2, -28, 18)}),
    (2.0, {**up, 'neck': (6, 25, 0), 'head': (2, 30, -18)}),
    (2.25, {**up, 'neck': (6, 25, 0), 'head': (-6, 30, -22)}),
    (2.6, {**up, 'head': (6, 0, 0)}),
    (3.0, {}),
], NAMES)
search.rot('tail1', (0, (0, 0, 0)), (1.0, (10, 20, 0)), (2.0, (10, -20, 0)), (3.0, (0, 0, 0)))
A['SEARCH'] = search

# hiss / roar: rears back then thrusts the head, jaws wide, arms spread
hiss = Anim('hiss', 1.6)
pose_seq(hiss, [
    (0.0, {}),
    (0.35, {'chest': (-14, 0, 0), 'neck': (-10, 0, 0), 'head': (-20, 0, 0), 'jaw': (10, 0, 0),
            'right_arm': (-30, 0, -40), 'left_arm': (-30, 0, 40), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0)}),
    (0.6, {'chest': (12, 0, 0), 'neck': (14, 0, 0), 'head': (-20, 0, 0), 'jaw': (48, 0, 0),
           'right_arm': (-50, 0, -55), 'left_arm': (-50, 0, 55), 'right_fore': (-40, 0, 0), 'left_fore': (-40, 0, 0),
           'right_fingers': (30, 0, 0), 'left_fingers': (30, 0, 0)}),
    (1.15, {'chest': (14, 0, 0), 'neck': (12, 0, 4), 'head': (-22, 0, -6), 'jaw': (52, 0, 0),
            'right_arm': (-50, 0, -55), 'left_arm': (-50, 0, 55), 'right_fore': (-40, 0, 0), 'left_fore': (-40, 0, 0),
            'right_fingers': (30, 0, 0), 'left_fingers': (30, 0, 0)}),
    (1.6, {}),
], NAMES)
hiss.pos('inner_jaw', (0, (0, 0, 0)), (0.6, (0, 0, 0)), (0.8, (0, 0, -2.5)), (1.15, (0, 0, -2.0)), (1.4, (0, 0, 0)))
for i, n in enumerate(TAILS[:6]):
    hiss.rot(n, (0, (0, 0, 0)), (0.5, (14, 20 - i * 4, 0)), (0.9, (14, -20 + i * 4, 0)), (1.3, (6, 12, 0)), (1.6, (0, 0, 0)))
A['HISS'] = hiss

# claw swipe
atk = Anim('attack', 0.8)
pose_seq(atk, [
    (0.0, {}),
    (0.22, {'chest': (-6, -18, 0), 'right_arm': (-140, 0, -30), 'right_fore': (-30, 0, 0), 'right_fingers': (30, 0, 0), 'head': (-8, 0, 0), 'jaw': (25, 0, 0)}),
    (0.38, {'chest': (18, 22, 0), 'right_arm': (-30, 0, 30), 'right_fore': (-10, 0, 0), 'right_fingers': (-30, 0, 0), 'head': (10, 0, 0), 'jaw': (40, 0, 0)}),
    (0.8, {}),
], NAMES)
A['ATTACK'] = atk

# kill: grab, open the jaws slowly, inner jaw strikes twice
kill = Anim('kill', 2.4)
grab = {'chest': (24, 0, 0), 'waist': (6, 0, 0), 'neck': (10, 0, 0), 'head': (-40.0, 0, 0),
        'right_arm': (-80, 0, 10), 'left_arm': (-80, 0, -10), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0),
        'right_fingers': (-55, 0, 0), 'left_fingers': (-55, 0, 0)}
pose_seq(kill, [
    (0.0, {}),
    (0.3, {**grab, 'chest': (30, 0, 0)}),
    (0.8, {**grab, 'head': (-36.0, 0, 10), 'jaw': (20, 0, 0)}),
    (1.35, {**grab, 'head': (-38.0, 0, -6), 'jaw': (50, 0, 0)}),
    (1.55, {**grab, 'neck': (4, 0, 0), 'head': (-44.0, 0, 0), 'jaw': (55, 0, 0)}),
    (1.65, {**grab, 'neck': (18, 0, 0), 'head': (-30.0, 0, 0), 'jaw': (58, 0, 0)}),
    (1.95, {**grab, 'neck': (8, 0, 0), 'head': (-38.0, 0, 0), 'jaw': (50, 0, 0)}),
    (2.05, {**grab, 'neck': (18, 0, 0), 'head': (-30.0, 0, 0), 'jaw': (58, 0, 0)}),
    (2.4, {**grab, 'jaw': (30, 0, 0)}),
], NAMES)
kill.pos('inner_jaw', (0, (0, 0, 0)), (1.5, (0, 0, 0)), (1.62, (0, 0, -7.0)), (1.8, (0, 0, -1.0)), (1.98, (0, 0, -1.0)),
         (2.05, (0, 0, -7.0)), (2.3, (0, 0, 0)))
A['KILL'] = kill

# vent enter: crouch then leap up into the ceiling
vin = Anim('vent_enter', 1.2)
pose_seq(vin, [
    (0.0, {}),
    (0.4, {'hips': (20, 0, 0), 'chest': (25, 0, 0), 'right_thigh': (-50, 0, 0), 'left_thigh': (-50, 0, 0),
           'right_shin': (60, 0, 0), 'left_shin': (60, 0, 0), 'right_arm': (-30, 0, 0), 'left_arm': (-30, 0, 0), 'head': (-20, 0, 0)}),
    (0.75, {'hips': (-10, 0, 0), 'chest': (-20, 0, 0), 'neck': (-20, 0, 0), 'right_arm': (-175, 0, -10), 'left_arm': (-175, 0, 10),
            'right_fore': (0, 0, 0), 'left_fore': (0, 0, 0), 'right_thigh': (20, 0, 0), 'left_thigh': (20, 0, 0),
            'right_shin': (-20, 0, 0), 'left_shin': (-20, 0, 0)}),
    (1.2, {'hips': (-10, 0, 0), 'chest': (-20, 0, 0), 'right_arm': (-175, 0, -10), 'left_arm': (-175, 0, 10),
           'right_thigh': (30, 0, 0), 'left_thigh': (30, 0, 0)}),
], NAMES)
vin.pos('base', (0, (0, 0, 0)), (0.4, (0, -5, 0)), (0.6, (0, 4, 0)), (1.0, (0, 40, 0)), (1.2, (0, 48, 0)))
for i, n in enumerate(TAILS):
    vin.rot(n, (0, (0, 0, 0)), (0.7, (30, 0, 0)), (1.2, (45 if i == 0 else 4, 0, 0)))
A['VENT_ENTER'] = vin

# vent exit: drops from the ceiling, lands crouched, rises and hisses
vout = Anim('vent_exit', 1.6)
land = {'hips': (24, 0, 0), 'chest': (28, 0, 0), 'neck': (-10, 0, 0), 'head': (-20, 0, 0),
        'right_thigh': (-60, 0, 0), 'left_thigh': (-60, 0, 0), 'right_shin': (70, 0, 0), 'left_shin': (70, 0, 0),
        'right_foot': (-10, 0, 0), 'left_foot': (-10, 0, 0),
        'right_arm': (-60, 0, -20), 'left_arm': (-60, 0, 20), 'right_fore': (-20, 0, 0), 'left_fore': (-20, 0, 0)}
pose_seq(vout, [
    (0.0, {'right_arm': (-170, 0, -10), 'left_arm': (-170, 0, 10), 'right_thigh': (20, 0, 0), 'left_thigh': (20, 0, 0)}),
    (0.3, {'right_arm': (-120, 0, -20), 'left_arm': (-120, 0, 20)}),
    (0.42, land),
    (0.65, land),
    (1.05, {'chest': (6, 0, 0), 'head': (-4, 0, 0), 'jaw': (40, 0, 0), 'right_arm': (-30, 0, -40), 'left_arm': (-30, 0, 40)}),
    (1.35, {'chest': (6, 0, 0), 'head': (4, 0, 0), 'jaw': (45, 0, 0), 'right_arm': (-30, 0, -40), 'left_arm': (-30, 0, 40)}),
    (1.6, {}),
], NAMES)
vout.pos('base', (0, (0, 48, 0)), (0.12, (0, 44, 0)), (0.36, (0, 0, 0)), (0.42, (0, -6, 0)), (0.65, (0, -5, 0)), (1.05, (0, 0, 0)), (1.6, (0, 0, 0)))
A['VENT_EXIT'] = vout

# flinch from fire: recoils, shields its head, screeches
fl = Anim('flinch', 1.3)
recoil = {'hips': (-10, 0, 0), 'chest': (-26, 0, 0), 'neck': (-14, 0, 0), 'head': (-30, 0, 0), 'jaw': (55, 0, 0),
          'right_arm': (-120, 0, -40), 'left_arm': (-120, 0, 40), 'right_fore': (-70, 0, 0), 'left_fore': (-70, 0, 0),
          'right_fingers': (30, 0, 0), 'left_fingers': (30, 0, 0)}
pose_seq(fl, [(0.0, {}), (0.15, recoil), (0.6, {**recoil, 'head': (-24, 18, 10)}), (0.85, {**recoil, 'head': (-24, -18, -10)}), (1.3, {})], NAMES)
fl.pos('base', (0, (0, 0, 0)), (0.15, (0, 0, 3)), (1.0, (0, 0, 2)), (1.3, (0, 0, 0)))
for i, n in enumerate(TAILS):
    fl.rot(n, (0, (0, 0, 0)), (0.2, (20, 25, 0)), (0.5, (16, -25, 0)), (0.8, (14, 20, 0)), (1.3, (0, 0, 0)))
A['FLINCH'] = fl

# locker check: leans in close, slow sniffing
lc = Anim('locker_check', 3.5)
lean = {'hips': (8, 0, 0), 'chest': (20, 0, 0), 'neck': (14, 0, 0), 'head': (-34, 0, 0),
        'right_arm': (-30, 0, -6), 'left_arm': (-30, 0, 6), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0)}
kf = [(0.0, {}), (0.5, lean)]
for i in range(10):
    t = 0.7 + i * 0.22
    kf.append((t, {**lean, 'head': (-34 + (4 if i % 2 else -2), (i % 3 - 1) * 6, (12 if i < 5 else -12)), 'jaw': (6 if i % 2 else 0, 0, 0)}))
kf += [(3.0, lean), (3.5, {})]
pose_seq(lc, kf, NAMES)
A['LOCKER_CHECK'] = lc

# collapse (only reachable through /kill)
death = Anim('death', 2.0)
pose_seq(death, [
    (0.0, {}),
    (0.5, {'chest': (-20, 0, 0), 'head': (-30, 0, 0), 'jaw': (50, 0, 0), 'right_arm': (-90, 0, -30), 'left_arm': (-90, 0, 30)}),
    (1.2, {'hips': (30, 0, 0), 'chest': (30, 0, 0), 'right_thigh': (-80, 0, 0), 'left_thigh': (-80, 0, 0), 'right_shin': (100, 0, 0),
           'left_shin': (100, 0, 0), 'head': (20, 0, 0), 'jaw': (30, 0, 0)}),
    (2.0, {'hips': (30, 0, 0), 'chest': (40, 0, 0), 'right_thigh': (-80, 0, 0), 'left_thigh': (-80, 0, 0), 'right_shin': (100, 0, 0),
           'left_shin': (100, 0, 0), 'head': (30, 0, 0), 'jaw': (20, 0, 0)}),
], NAMES)
death.pos('base', (0, (0, 0, 0)), (1.2, (0, -12, -4)), (2.0, (0, -14, -6)))
A['DEATH'] = death
