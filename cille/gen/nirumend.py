"""Nirumend (Cille) — giant earth mammoth: model, textures and animations.

Model units: feet at y=24, y down, front = -z. Rendered at scale 0.5 => 32 units per block.
Reference: sand-beige elephant/mammoth, huge fan ears with darker inner ridges, carved mask-like forehead with an
orange round emblem in the middle and two glowing cyan gems for eyes, ridged ringed trunk, long grey tusks curving
up with gold-orange tips, an orange leopard-spotted saddle cloth with a shaggy brown fringe over the back, spotted
straps around the ankles, thick column legs.
"""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, glow_img
from animlib import pose_seq, sym
from shapes import ellipsoid, ellipsoid_l, limb, texel_world, Noise3, light, lerp, ramp, smooth, ROUND

M = Model('nirumend', 1024, 1024)
R5 = dict(cross=ROUND)


def H(h):
    return 24 - h


# ---------------------------------------------------------------- skeleton + volumes
M.bone('base', None, (0, 24, 0))
M.bone('body', 'base', (0, H(72), 10))
ellipsoid(M, 'body', (0, H(74), 10), (31, 29, 46), 'skin', axis='z', slices=8)
ellipsoid(M, 'body', (0, H(90), 8), (33, 16, 45), 'saddle', axis='z', slices=8)      # spotted saddle cloth over the back
ellipsoid(M, 'body', (0, H(78), -26), (30, 8, 5), 'strap', axis='y', slices=2)        # chest strap
# shaggy fringe hanging from the saddle edge
fi = 0
for z in range(-28, 50, 9):
    for sx in (-1, 1):
        n = f'fringe{fi}'
        M.bone(n, 'body', (sx * 31, H(80), z), (0, 0, sx * -8))
        M.cube_l(n, (-1.6, 0, -4.2), (3.2, 12 + (fi % 3) * 2, 8.4), 'fringe')
        fi += 1
M.bone('tail', 'body', (0, H(80), 56), (25, 0, 0))
M.cube_l('tail', (-2, 0, -2), (4, 20, 4), 'skin')
M.cube_l('tail', (-3, 18, -3), (6, 6, 6), 'fringe')

# ---- head
M.bone('head', 'body', (0, H(84), -34))
ellipsoid_l(M, 'head', (0, -3, -14), (22, 24, 19), 'skin', axis='y', slices=7, **R5)
ellipsoid_l(M, 'head', (0, -12, -27), (16, 12, 6), 'mask', axis='y', slices=5, **R5)   # carved forehead mask
M.cube_l('head', (-5.5, -19, -33.6), (11, 11, 1.4), 'emblem')                          # orange round emblem
for sx in (-1, 1):
    M.cube_l('head', (sx * 10 - 3, -14, -32.2), (6, 6, 1.4), 'gem')                     # glowing cyan gems
# ears: huge fans
for side, sx in (('right', -1), ('left', 1)):
    n = f'{side}_ear'
    M.bone(n, 'head', (0, 0, 0))
    M.bones[n].off = [sx * 16.0, -10.0, -10.0]
    ellipsoid_l(M, n, (sx * 24, 2, 0), (24, 22, 2.2), 'ear', axis='x', slices=8, **R5)
# trunk: ringed, hanging and curling forward at the tip
hw = M.world('head')
TR = [(0, 8, -28), (0, 22, -33), (0, 36, -35), (0, 50, -34), (0, 61, -30), (0, 69, -24), (0, 73, -17)]
TRW = (13, 11.5, 10, 8.5, 7, 6, 5)
pts = [hw[:3, :3] @ np.array(p) + hw[:3, 3] for p in TR]
parent = 'head'
for i in range(6):
    n = f'trunk{i}'
    limb(M, n, parent, pts[i], pts[i + 1], TRW[i], TRW[i], TRW[i + 1], TRW[i + 1], 'trunk', extend=1.5, slices=2)
    parent = n
# tusks: grey, sweeping down-forward then up, gold tips
for side, sx in (('right', -1), ('left', 1)):
    TK = [(sx * 10, 10, -26), (sx * 13, 24, -32), (sx * 16, 36, -42), (sx * 18, 42, -56), (sx * 18, 38, -70), (sx * 16, 28, -80), (sx * 13, 16, -84)]
    tw = (6.5, 6, 5.5, 5, 4.2, 3.4, 2.2)
    pts = [hw[:3, :3] @ np.array(p) + hw[:3, 3] for p in TK]
    parent = 'head'
    for i in range(6):
        n = f'{side}_tusk{i}'
        limb(M, n, parent, pts[i], pts[i + 1], tw[i], tw[i], tw[i + 1], tw[i + 1], 'tusk_gold' if i >= 4 else 'tusk', extend=1.0, slices=1)
        parent = n

# ---- legs: thick columns with spotted ankle straps and wide feet
for side, sx in (('right', -1), ('left', 1)):
    for end, z in (('front', -26), ('hind', 40)):
        A = (sx * 18, H(62), z)
        B = (sx * 19, H(32), z + (2 if end == 'front' else -3))
        C = (sx * 19, H(6), z)
        up = f'{side}_{end}_leg'
        lo = f'{side}_{end}_shin'
        limb(M, up, 'body', A, B, 22, 22, 18, 18, 'skin', extend=5, slices=4)
        limb(M, lo, up, B, C, 17, 17, 17, 17, 'skin', extend=2, slices=3)
        ellipsoid_l(M, lo, (0, 18, 0), (10, 3.2, 10), 'strap', axis='y', slices=2, **R5)    # ankle strap
        foot = f'{side}_{end}_foot'
        M.bone_w(foot, lo, C, np.eye(3))
        ellipsoid_l(M, foot, (0, 3, -1), (11, 3.6, 11.5), 'foot', axis='y', slices=2, **R5)

# ---------------------------------------------------------------- textures (world space)
P = Painter(M)
SKIN = [(118, 92, 58), (170, 140, 96), (206, 180, 130), (232, 214, 172)]
SADDLE = [(140, 66, 22), (196, 112, 44), (226, 150, 70), (244, 186, 110)]
TUSK = [(96, 96, 104), (142, 142, 150), (182, 182, 188), (214, 214, 220)]
GOLD = [(150, 80, 20), (210, 130, 40), (240, 172, 70), (255, 214, 130)]
N1, N2, N3 = Noise3(41, freq=0.12), Noise3(42, freq=0.35), Noise3(43, freq=0.6)


def world_mat(fn):
    def wrapped(c, face, W, H_, r):
        Pw, n = texel_world(M, c, face, W, H_)
        col, alpha, glow = fn(c, face, Pw, n, W, H_)
        L = light(n)
        under = 0.82 if n[1] > 0.6 else 1.0
        return np.clip(col * L * under, 0, 255), alpha if alpha is not None else np.ones((H_, W)), glow
    return wrapped


@world_mat
def skin_mat(c, face, Pw, n, W, H_):
    t = 0.55 + 0.14 * (N1(Pw) - 0.5)
    wrink = smooth(0.82, 0.96, np.abs(np.sin(Pw[..., 1] * 0.55 + N2(Pw) * 3)))
    col = ramp(t, SKIN)
    return lerp(col, (130, 104, 68), wrink * 0.22), None, None


def spots(Pw):
    """leopard spots: dark ring rosettes"""
    v = N3(Pw * 1.4)
    return smooth(0.68, 0.76, v), smooth(0.78, 0.86, v)


@world_mat
def saddle_mat(c, face, Pw, n, W, H_):
    col = ramp(0.5 + 0.18 * (N1(Pw) - 0.5), SADDLE)
    ring, core = spots(Pw)
    col = lerp(col, (70, 34, 12), ring * 0.9)
    col = lerp(col, (176, 96, 36), core * 0.8)
    # bottom edge darker (where the fringe starts)
    h = 24 - Pw[..., 1]
    col = lerp(col, (120, 60, 24), smooth(82, 76, h) * 0.6)
    return col, None, None


P.mats['strap'] = saddle_mat


@world_mat
def fringe_mat(c, face, Pw, n, W, H_):
    strands = 0.5 + 0.5 * np.sin(Pw[..., 0] * 2.1 + Pw[..., 2] * 1.7 + N2(Pw) * 4)
    return ramp(0.3 + 0.35 * strands, [(70, 36, 14), (120, 64, 26), (170, 100, 44), (206, 140, 70)]), None, None


@world_mat
def mask_mat(c, face, Pw, n, W, H_):
    col = ramp(0.68 + 0.08 * (N2(Pw) - 0.5), SKIN)
    # carved ridges outlining the mask
    hl = (Pw - M.world('head')[:3, 3]) @ M.world('head')[:3, :3]
    ridge = smooth(0.8, 0.95, np.abs(np.sin(hl[..., 0] * 0.25 + hl[..., 1] * 0.12)))
    return lerp(col, (130, 100, 64), ridge * 0.5), None, None


@world_mat
def ear_mat(c, face, Pw, n, W, H_):
    col = ramp(0.58 + 0.1 * (N1(Pw) - 0.5), SKIN)
    hl = (Pw - M.world('head')[:3, 3]) @ M.world('head')[:3, :3]
    r = np.hypot(np.abs(hl[..., 0]) - 16, hl[..., 1] + 8)
    ang = np.arctan2(hl[..., 1] + 8, np.abs(hl[..., 0]) - 16)
    veins = smooth(0.85, 0.97, np.abs(np.sin(ang * 4 + r * 0.05)))
    col = lerp(col, (132, 102, 66), veins * 0.55)
    rim = smooth(19, 23, r)
    return lerp(col, (150, 118, 76), rim * 0.6), None, None


@world_mat
def trunk_mat(c, face, Pw, n, W, H_):
    col = ramp(0.58 + 0.1 * (N1(Pw) - 0.5), SKIN)
    rings = smooth(0.7, 0.92, np.abs(np.sin(Pw[..., 1] * 0.75)))
    return lerp(col, (110, 84, 54), rings * 0.75), None, None


@world_mat
def tusk_mat(c, face, Pw, n, W, H_):
    return ramp(0.55 + 0.12 * (N2(Pw) - 0.5), TUSK), None, None


@world_mat
def tusk_gold_mat(c, face, Pw, n, W, H_):
    return ramp(0.6 + 0.12 * (N2(Pw) - 0.5), GOLD), None, None


@world_mat
def foot_mat(c, face, Pw, n, W, H_):
    col = ramp(0.5 + 0.1 * (N1(Pw) - 0.5), SKIN)
    nails = smooth(0.85, 0.96, np.sin(Pw[..., 0] * 0.6)) * smooth(-0.3, -0.8, n[2])
    return lerp(col, (236, 224, 196), nails), None, None


P.mats.update({'skin': skin_mat, 'saddle': saddle_mat, 'fringe': fringe_mat, 'mask': mask_mat, 'ear': ear_mat,
               'trunk': trunk_mat, 'tusk': tusk_mat, 'tusk_gold': tusk_gold_mat, 'foot': foot_mat})


@P.mat('emblem')
def emblem(c, face, W, H_, r):
    yy, xx = np.mgrid[0:H_, 0:W]
    col = np.zeros((H_, W, 3), np.float32)
    col[:] = (206, 180, 130)
    alpha = np.ones((H_, W))
    if face == 'north':
        d = np.hypot((xx + 0.5 - W / 2) / (W / 2), (yy + 0.5 - H_ / 2) / (H_ / 2))
        col[d < 1.0] = (90, 44, 14)
        col[d < 0.86] = (232, 124, 40)
        cres = (np.hypot((xx + 0.5 - W / 2) / (W / 2), (yy + 0.5 - H_ * 0.35) / (H_ / 2)) < 0.45) & (d < 0.6)
        col[cres] = (70, 34, 10)
    return col, alpha, None


@P.mat('gem')
def gem(c, face, W, H_, r):
    yy, xx = np.mgrid[0:H_, 0:W]
    col = np.zeros((H_, W, 3), np.float32)
    col[:] = (40, 120, 130)
    glow = np.zeros((H_, W, 4), np.float32)
    if face == 'north':
        d = np.hypot((xx + 0.5 - W / 2) / (W / 2), (yy + 0.5 - H_ / 2) / (H_ / 2))
        col[d < 0.9] = (60, 220, 230)
        col[d < 0.35] = (220, 255, 255)
        glow = glow_img(H_, W, (90, 240, 250), (d < 0.9) * 1.0)
    return col, np.ones((H_, W)), glow


# ---------------------------------------------------------------- animations
LEGS = [f'{s}_{e}_{p}' for s in ('right', 'left') for e in ('front', 'hind') for p in ('leg', 'shin', 'foot')]
TRUNK = [f'trunk{i}' for i in range(6)]
EARS = ['right_ear', 'left_ear']
ALL = ['body', 'head', 'tail'] + EARS + TRUNK + LEGS
A = {}


def leg(s, amp, lift):
    if s < 0.5:
        k = s / 0.5
        up, bend = -amp + 2 * amp * k, 0.0
    else:
        k = (s - 0.5) / 0.5
        e = 0.5 - 0.5 * math.cos(k * math.pi)
        up, bend = amp - 2 * amp * e, lift * math.sin(k * math.pi)
    return up, bend


def gait(name, L, amp, lift, phases, bob, steps=12):
    a = Anim(name, L, loop=True)
    fr, pos = [], []
    for i in range(steps + 1):
        u = i / steps
        w = 2 * math.pi * u
        f = {}
        for key, ph in phases.items():
            up, bend = leg((u + ph) % 1.0, amp, lift)
            f[f'{key}_leg'] = (up - bend * 0.3, 0, 0)
            f[f'{key}_shin'] = (bend if 'front' in key else bend * 0.9, 0, 0)
            f[f'{key}_foot'] = (-up - bend * 0.6, 0, 0)
        f['body'] = (0, 0, 1.5 * math.sin(w))
        f['head'] = (2 * math.sin(2 * w + 0.5), 0, 0)
        for k in range(6):
            f[f'trunk{k}'] = (6 * math.sin(w - k * 0.5), 0, 4 * math.sin(w * 1 - k * 0.6))
        f['right_ear'] = (0, 8 * math.sin(2 * w), 0)
        f['left_ear'] = (0, -8 * math.sin(2 * w), 0)
        f['tail'] = (0, 0, 12 * math.sin(w))
        fr.append((L * u, f))
        pos.append((L * u, (0, bob * (0.5 + 0.5 * math.cos(4 * math.pi * u)), 0)))
    pose_seq(a, fr, ALL)
    a.pos('base', *pos)
    return a


def idle():
    a = Anim('idle', 4.0, loop=True)
    fr = []
    for i in range(9):
        u = i / 8
        w = 2 * math.pi * u
        f = {'body': (0.8 * math.sin(w), 0, 0), 'head': (2.5 * math.sin(w + 0.6), 5 * math.sin(w * 0.5) if i < 8 else 0, 0),
             'right_ear': (0, 14 * max(0, math.sin(w * 2)), 0), 'left_ear': (0, -14 * max(0, math.sin(w * 2 + 1)), 0),
             'tail': (0, 0, 15 * math.sin(w))}
        for k in range(6):
            f[f'trunk{k}'] = (5 * math.sin(w - k * 0.5), 0, 6 * math.sin(w - k * 0.7))
        fr.append((4.0 * u, f))
    fr[-1] = (4.0, dict(fr[0][1]))
    pose_seq(a, fr, ALL)
    return a


A['IDLE'] = idle()
A['WALK'] = gait('walk', 1.6, 16, 26, {'right_front': 0.0, 'left_hind': 0.1, 'left_front': 0.5, 'right_hind': 0.6}, 1.2)
A['RUN'] = gait('run', 0.9, 26, 40, {'right_front': 0.0, 'left_hind': 0.2, 'left_front': 0.5, 'right_hind': 0.7}, 4)

lie = {}
for s in ('right', 'left'):
    lie[f'{s}_front_leg'] = (-75, 0, 0); lie[f'{s}_front_shin'] = (0, 0, 0); lie[f'{s}_front_foot'] = (75, 0, 0)
    lie[f'{s}_hind_leg'] = (75, 0, 0); lie[f'{s}_hind_shin'] = (0, 0, 0); lie[f'{s}_hind_foot'] = (-75, 0, 0)
lie['head'] = (8, 0, 0)
for k in range(6): lie[f'trunk{k}'] = (-8 if k < 3 else -14, 0, 0)
a = Anim('lie', 4.0, loop=True)
pose_seq(a, [(0, lie), (2.0, {**lie, 'head': (10, 6, 0), 'right_ear': (0, 10, 0), 'left_ear': (0, -10, 0)}), (4.0, lie)], ALL)
a.pos('base', (0, (0, -28, 0)), (4.0, (0, -28, 0)))
A['SIT'] = a


def act(name, length, frames, base_pos=None, base_rot=None, base_scale=None):
    a = Anim(name, length)
    pose_seq(a, frames, ALL)
    if base_pos: a.pos('base', *base_pos)
    if base_rot: a.rot('base', *base_rot)
    if base_scale: a.scl('base', *base_scale)
    return a


def trunk_curve(x0, step, z=0):
    return {f'trunk{k}': (x0 + step * k, 0, z) for k in range(6)}


ears_out = {'right_ear': (0, 35, 0), 'left_ear': (0, -35, 0)}
trunk_up = trunk_curve(-35, -14)
trunk_curl = trunk_curve(-20, 22)
rear = {'body': (-28, 0, 0), 'head': (-14, 0, 0), **ears_out, **trunk_up,
        'right_front_leg': (-60, 0, 0), 'right_front_shin': (60, 0, 0), 'left_front_leg': (-60, 0, 0), 'left_front_shin': (60, 0, 0),
        'right_hind_leg': (24, 0, 0), 'left_hind_leg': (24, 0, 0), 'right_hind_foot': (4, 0, 0), 'left_hind_foot': (4, 0, 0)}
slam = {'body': (6, 0, 0), 'head': (16, 0, 0), **ears_out, **trunk_curve(10, 4),
        'right_front_leg': (-14, 0, 0), 'left_front_leg': (-14, 0, 0), 'right_hind_leg': (-6, 0, 0), 'left_hind_leg': (-6, 0, 0)}

A['ATTACK'] = act('tusk_swipe', 1.0, [(0, {}), (0.3, {'head': (-12, 25, 0), 'body': (0, 8, 0), **trunk_up}),
                                      (0.55, {'head': (18, -30, 0), 'body': (0, -8, 0), **trunk_curve(10, 6)}), (1.0, {})],
                  base_pos=[(0, (0, 0, 0)), (0.55, (0, 0, -8)), (1.0, (0, 0, 0))])
A['BUMEHEN'] = act('bumehen', 2.0, [(0, {}), (0.6, rear), (0.9, rear), (1.1, slam), (1.6, slam), (2.0, {})],
                   base_pos=[(0, (0, 0, 0)), (0.6, (0, 16, 10)), (0.9, (0, 18, 10)), (1.1, (0, -4, 0)), (2.0, (0, 0, 0))])
grab = {'head': (10, 0, 0), **trunk_curve(-8, -10)}
lift_ = {'head': (-28, 0, 0), **ears_out, **trunk_curve(-40, -18), 'body': (-8, 0, 0)}
A['AJIRAK'] = act('ajirak', 2.4, [(0, {}), (0.4, {**grab, 'head': (20, 0, 0)}), (0.6, grab), (1.2, lift_), (1.5, lift_), (1.8, {**slam, **trunk_curve(30, 10)}), (2.4, {})],
                  base_pos=[(0, (0, 0, 0)), (0.6, (0, 0, -6)), (1.2, (0, 4, 4)), (1.8, (0, -3, -4)), (2.4, (0, 0, 0))])
lowered = {'head': (34, 0, 0), 'body': (8, 0, 0), 'right_ear': (0, -20, 0), 'left_ear': (0, 20, 0), **trunk_curve(30, 10)}
A['SKULL_BASH'] = act('skull_bash', 2.0, [(0, {}), (0.3, lowered), (1.4, lowered), (1.6, {'head': (-20, 0, 0), **ears_out}), (2.0, {})])
A['HARD_ROCK'] = act('hard_rock', 2.0, [(0, {}), (0.5, rear), (0.8, rear), (1.0, slam), (1.3, {**slam, 'head': (-20, 0, 0), **trunk_up}), (1.5, {**slam, 'head': (20, 0, 0)}), (2.0, {})],
                     base_pos=[(0, (0, 0, 0)), (0.5, (0, 14, 8)), (0.8, (0, 16, 8)), (1.0, (0, -4, 0)), (2.0, (0, 0, 0))])
brace = {'body': (2, 0, 0), 'head': (-8, 0, 0), **ears_out, **trunk_up,
         'right_front_leg': (-12, 0, 10), 'left_front_leg': (-12, 0, -10), 'right_hind_leg': (10, 0, 10), 'left_hind_leg': (10, 0, -10)}
A['ELECTRIC_SHIELD'] = act('electric_shield', 1.6, [(0, {}), (0.4, brace), (1.2, {**brace, 'head': (-14, 0, 0)}), (1.6, {})],
                           base_pos=[(0, (0, 0, 0)), (0.4, (0, -4, 0)), (1.2, (0, -4, 0)), (1.6, (0, 0, 0))])
A['DUST_CLOUD'] = act('dust_cloud', 2.0, [(0, {}), (0.3, {'right_front_leg': (-40, 0, 0), 'right_front_shin': (40, 0, 0), **ears_out}), (0.5, {**slam}),
                                          (0.8, {'left_front_leg': (-40, 0, 0), 'left_front_shin': (40, 0, 0), **ears_out}), (1.0, slam),
                                          (1.4, {**brace, **trunk_curve(-20, -6)}), (2.0, {})])
A['STOMP'] = act('stomp', 2.0, [(0, {}), (0.7, rear), (1.0, {**rear, 'head': (-24, 0, 0)}), (1.2, slam), (1.7, slam), (2.0, {})],
                 base_pos=[(0, (0, 0, 0)), (0.7, (0, 20, 12)), (1.0, (0, 22, 12)), (1.2, (0, -6, 0)), (1.7, (0, -4, 0)), (2.0, (0, 0, 0))])
A['RELEASE'] = act('release', 1.8, [(0, {}), (0.6, {**ears_out, **trunk_up, 'head': (-20, 0, 0)}), (1.3, {**ears_out, **trunk_up, 'head': (-26, 0, 0)}), (1.8, {})],
                   base_scale=[(0, (0.02, 0.02, 0.02)), (0.35, (1.08, 1.08, 1.08)), (0.55, (0.97, 0.97, 0.97)), (0.7, (1, 1, 1)), (1.8, (1, 1, 1))])
A['RECALL'] = act('recall', 1.25, [(0, {}), (0.45, lie), (1.25, lie)],
                  base_pos=[(0, (0, 0, 0)), (0.45, (0, -28, 0)), (1.25, (0, -28, 0))],
                  base_scale=[(0, (1, 1, 1)), (0.5, (1, 1, 1)), (1.0, (0.05, 0.05, 0.05)), (1.25, (0.01, 0.01, 0.01))])
dead = dict(lie)
dead['head'] = (16, 0, 0)
A['DEATH'] = act('death', 3.0, [(0, {}), (0.5, {'head': (-26, 0, 0), **trunk_up, **ears_out}), (1.4, dead), (3.0, dead)],
                 base_pos=[(0, (0, 0, 0)), (1.4, (0, 30, 0)), (3.0, (0, 30, 0))],
                 base_rot=[(0, (0, 0, 0)), (0.5, (0, 0, 0)), (1.4, (0, 0, 85)), (3.0, (0, 0, 88))])
