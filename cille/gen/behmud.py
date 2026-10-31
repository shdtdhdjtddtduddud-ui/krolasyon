"""Behmud (Cille) — flying cosmic whale: model, textures and animations.

Model units: belly at y=24 (entity bottom), y down, front = -z. Rendered at scale 0.5 => 32 units per block.
Reference: long dark-navy whale with white star speckles, pale cyan helmet-like hood over the top of the head,
dark face ringed with white dots and glowing eyes, wide mouth with long pale barbels (whiskers) hanging down,
long pale wing-like pectoral fins, small cyan dorsal fins, dark tail fluke fading to grey, lighter grey belly
and a white wavy flank stripe.
"""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, glow_img
from animlib import pose_seq, sym
from shapes import ellipsoid, ellipsoid_l, limb, texel_world, Noise3, light, lerp, ramp, smooth

M = Model('behmud', 1024, 1024)


def H(h):
    return 24 - h


# ---------------------------------------------------------------- skeleton + volumes
M.bone('base', None, (0, H(34), 0))
M.bone('body', 'base', (0, H(34), 0))
ellipsoid(M, 'body', (0, H(34), 8), (31, 32, 58), 'body', axis='z', slices=10)
ellipsoid(M, 'body', (0, H(24), 0), (27, 20, 50), 'belly', axis='z', slices=8)          # pale throat / belly
# dorsal fins (small, cyan, swept back)
for i, z in enumerate((-6, 22, 46)):
    n = f'dorsal{i}'
    M.bone(n, 'body', (0, H(64 - i * 4), z), (-52, 0, 0))
    M.cube_l(n, (-1.6, -11 + i * 2, -5), (3.2, 11 - i * 2, 10), 'fin_cyan')
    M.cube_l(n, (-1.1, -16 + i * 3, -3), (2.2, 5.5, 6), 'fin_cyan')

# ---- head: blunt, with the pale hood on top
M.bone('head', 'body', (0, H(36), -44))
ellipsoid(M, 'head', (0, H(36), -66), (28, 28, 26), 'face', axis='z', slices=6)
ellipsoid(M, 'head', (0, H(54), -62), (29, 13, 30), 'hood', axis='z', slices=6)         # hood dome
ellipsoid(M, 'head', (0, H(47), -86), (24, 7, 8), 'hood', axis='x', slices=6)           # hood front lip
for sx in (-1, 1):
    ellipsoid(M, 'head', (sx * 24, H(46), -60), (7, 12, 24), 'hood', axis='z', slices=4)  # hood side flaps
    # glowing eyes low on the face
    M.cube('head', (sx * 13 - 3.5, H(40), -89.4), (7, 5, 1), 'eye')
# jaw (lower lip) — opens for beams, roars and the clamp
M.bone('jaw', 'head', (0, H(22), -50))
ellipsoid(M, 'jaw', (0, H(19), -70), (24, 7, 22), 'jaw', axis='z', slices=5)
# barbels (long whiskers) from the mouth corners
for side, sx in (('right', -1), ('left', 1)):
    pts = [(sx * 20, H(24), -82), (sx * 23, H(10), -82), (sx * 25, H(-8), -74), (sx * 26, H(-24), -62), (sx * 26, H(-36), -50)]
    parent = 'head'
    for i in range(4):
        n = f'{side}_barbel{i}'
        w = 3.0 - i * 0.5
        limb(M, n, parent, pts[i], pts[i + 1], w, w, w * 0.85, w * 0.85, 'barbel', extend=0.8, slices=1)
        parent = n
    M.cube_l(f'{side}_barbel3', (-1.8, 15, -1.8), (3.6, 3, 3.6), 'barbel')       # bulb at the tip

# ---- pectoral fins: long, wing-like
for side, sx in (('right', -1), ('left', 1)):
    A = (sx * 24, H(18), -28)
    B = (sx * 56, H(8), -14)
    C = (sx * 92, H(2), 8)
    limb(M, f'{side}_fin', 'body', A, B, 6, 22, 4.5, 18, 'fin', extend=3, slices=3)
    limb(M, f'{side}_fin_tip', f'{side}_fin', B, C, 4.5, 18, 2.5, 6, 'fin', extend=2, slices=4)

# ---- tail: three segments and the fluke
TP = [(0, H(36), 58), (0, H(38), 82), (0, H(40), 104), (0, H(42), 124)]
TW = ((28, 32), (19, 23), (12, 14), (7, 8))
parent = 'body'
for i in range(3):
    n = f'tail{i}'
    limb(M, n, parent, TP[i], TP[i + 1], TW[i][0], TW[i][1], TW[i + 1][0], TW[i + 1][1], 'tail', extend=3, slices=3)
    parent = n
# the bones point along +z (backwards); fluke lobes spread sideways from the end of tail2
M.bone('fluke', 'tail2', (0, 0, 0))
M.bones['fluke'].off = [0.0, float(np.linalg.norm(np.array(TP[3]) - np.array(TP[2]))), 0.0]
for side, sx in (('right', -1), ('left', 1)):
    n = f'{side}_lobe'
    M.bone(n, 'fluke', (0, 0, 0), (0, 0, sx * -70))
    M.bones[n].off = [0.0, 0.0, 0.0]
    limb_pts = None
    # flat lobe: wide in local x (after rotation it extends sideways), thin in z
    for k, (y0, L, w) in enumerate(((0, 12, 13), (12, 12, 11), (24, 10, 7))):
        M.cube_l(n, (-w / 2 - k * 2.5 * 0, y0 - 1, -2.2), (w, L + 1, 4.4), 'fluke')

# ---------------------------------------------------------------- textures (world space)
P = Painter(M)
NAVY = [(4, 7, 16), (10, 16, 32), (20, 30, 56), (40, 56, 92)]
GREY = [(64, 76, 92), (100, 114, 130), (140, 154, 168), (182, 194, 204)]
HOOD = [(110, 158, 168), (160, 205, 212), (200, 232, 236), (232, 248, 250)]
N1, N2, N3 = Noise3(21, freq=0.12), Noise3(22, freq=0.3), Noise3(23, freq=0.05)
rng = np.random.default_rng(5)
STARS = rng.normal(size=(900, 3))
STARS /= np.linalg.norm(STARS, axis=1, keepdims=True)


def world_mat(fn):
    def wrapped(c, face, W, H_, r):
        Pw, n = texel_world(M, c, face, W, H_)
        col, alpha, glow = fn(c, face, Pw, n, W, H_)
        L = light(n)
        under = 0.85 if n[1] > 0.6 else 1.0
        col = col * L * under
        return np.clip(col, 0, 255), alpha if alpha is not None else np.ones((H_, W)), glow
    return wrapped


def stars(Pw, density=0.93):
    """white speckles: sparse bright dots from high-frequency noise"""
    s = np.sin(Pw[..., 0] * 1.7 + 3.1) * np.sin(Pw[..., 1] * 1.9 + 1.3) * np.sin(Pw[..., 2] * 1.6 + 0.7)
    s2 = N2(Pw * 1.7)
    return smooth(density, 1.0, np.abs(s) * 0.5 + s2 * 0.5)


def flank_stripe(Pw):
    """the white wavy marking along the side, from mid body to behind the head"""
    side = np.abs(Pw[..., 0])
    h = 24 - Pw[..., 1]
    wave = 44 + 3 * np.sin(Pw[..., 2] * 0.07) + 1.5 * np.sin(Pw[..., 2] * 0.21 + 1.0)
    band = smooth(3.0, 1.2, np.abs(h - wave)) * (0.6 + 0.4 * smooth(0.3, 0.8, N1(Pw * 2)))
    along = smooth(-40, -20, Pw[..., 2]) * smooth(46, 26, Pw[..., 2])
    return band * along * smooth(18, 26, side)


def body_col(Pw, n):
    t = 0.38 + 0.18 * (N1(Pw) - 0.5)
    h = 24 - Pw[..., 1]
    col = ramp(t + 0.12 * smooth(40, 66, h), NAVY)
    belly = smooth(26, 14, h) * smooth(-0.2, 0.6, n[1])
    col = lerp(col, ramp(0.5 + 0.15 * (N2(Pw) - 0.5), GREY), belly)
    col = lerp(col, (226, 234, 240), flank_stripe(Pw) * 0.9)
    st = stars(Pw) * (1 - belly)
    col = lerp(col, (236, 242, 255), st)
    return col, st


@world_mat
def body_mat(c, face, Pw, n, W, H_):
    col, st = body_col(Pw, n)
    return col, None, glow_img(H_, W, (200, 225, 255), st * 0.55)


P.mats['body'] = body_mat
P.mats['tail'] = body_mat


@world_mat
def belly_mat(c, face, Pw, n, W, H_):
    col, st = body_col(Pw, n)
    h = 24 - Pw[..., 1]
    col = lerp(col, ramp(0.55 + 0.12 * (N2(Pw) - 0.5), GREY), smooth(30, 18, h))
    # throat grooves
    g = smooth(0.75, 0.95, np.sin(Pw[..., 0] * 0.9)) * smooth(22, 12, h) * smooth(10, -20, Pw[..., 2])
    col = lerp(col, (70, 82, 98), g * 0.6)
    return col, None, None


P.mats['belly'] = belly_mat


@world_mat
def face_mat(c, face, Pw, n, W, H_):
    col = ramp(0.3 + 0.12 * (N1(Pw) - 0.5), NAVY)
    # ring of white dots around the face front
    front = smooth(-82, -90, Pw[..., 2])
    d = stars(Pw, 0.88) * front
    col = lerp(col, (240, 246, 255), d)
    return col, None, glow_img(H_, W, (200, 230, 255), d * 0.8)


P.mats['face'] = face_mat


@world_mat
def hood_mat(c, face, Pw, n, W, H_):
    t = 0.55 + 0.18 * (N2(Pw) - 0.5) + 0.15 * smooth(-0.2, -0.9, n[1])
    col = ramp(t, HOOD)
    # ridges running front-to-back like a helmet
    rid = smooth(0.8, 0.97, np.abs(np.sin(Pw[..., 0] * 0.35)))
    col = lerp(col, (96, 140, 150), rid * 0.6)
    # dark rim along the lower edge
    h = 24 - Pw[..., 1]
    col = lerp(col, (40, 60, 72), smooth(42, 36, h) * 0.8)
    return col, None, None


P.mats['hood'] = hood_mat


@world_mat
def jaw_mat(c, face, Pw, n, W, H_):
    col = ramp(0.4 + 0.1 * (N2(Pw) - 0.5), GREY)
    if face == 'top':
        col[:] = (40, 24, 40)                 # inside of the mouth
    h = 24 - Pw[..., 1]
    col = lerp(col, ramp(0.3, NAVY), smooth(18, 24, h) * 0.8)
    return col, None, None


P.mats['jaw'] = jaw_mat


@world_mat
def fin_mat(c, face, Pw, n, W, H_):
    under = smooth(-0.1, 0.5, n[1])
    top = ramp(0.36 + 0.14 * (N1(Pw) - 0.5), NAVY)
    top = lerp(top, (230, 238, 250), stars(Pw))
    bot = ramp(0.62 + 0.1 * (N2(Pw) - 0.5), GREY)
    col = lerp(top, bot, under)
    # pale leading edge
    edge = smooth(0.4, 0.9, np.abs(n[2])) * 0.4
    return lerp(col, (200, 214, 226), edge), None, None


P.mats['fin'] = fin_mat


@world_mat
def fin_cyan_mat(c, face, Pw, n, W, H_):
    return ramp(0.55 + 0.15 * (N2(Pw) - 0.5), HOOD), None, None


P.mats['fin_cyan'] = fin_cyan_mat


@world_mat
def fluke_mat(c, face, Pw, n, W, H_):
    side = np.abs(Pw[..., 0])
    t = smooth(6, 34, side)
    col = lerp(ramp(0.32, NAVY), ramp(0.55, GREY), t * 0.85)
    col = lerp(col, (230, 238, 250), stars(Pw) * (1 - t))
    return col, None, None


P.mats['fluke'] = fluke_mat


@world_mat
def barbel_mat(c, face, Pw, n, W, H_):
    return ramp(0.62 + 0.1 * (N2(Pw) - 0.5), GREY), None, None


P.mats['barbel'] = barbel_mat


@P.mat('eye')
def eye(c, face, W, H_, r):
    yy, xx = np.mgrid[0:H_, 0:W]
    col = np.zeros((H_, W, 3), np.float32)
    col[:] = (10, 14, 26)
    glow = np.zeros((H_, W, 4), np.float32)
    if face == 'north':
        d = np.hypot((xx + 0.5 - W / 2) / (W * 0.42), (yy + 0.5 - H_ / 2) / (H_ * 0.42))
        m = d < 1
        col[m] = (236, 244, 255)
        col[d < 0.45] = (150, 220, 255)
        glow = glow_img(H_, W, (200, 235, 255), m * 1.0)
    return col, np.ones((H_, W)), glow


# ---------------------------------------------------------------- animations
FINS = ['right_fin', 'right_fin_tip', 'left_fin', 'left_fin_tip']
TAIL = ['tail0', 'tail1', 'tail2', 'fluke']
BARB = [f'{s}_barbel{i}' for s in ('right', 'left') for i in range(4)]
ALL = ['body', 'head', 'jaw'] + FINS + TAIL + BARB
A = {}


def swim(name, L, stroke, fin, roll=0.0, steps=12, barb=1.0, loop=True, pitch=0.0):
    a = Anim(name, L, loop=loop)
    fr, pos = [], []
    for i in range(steps + 1):
        u = i / steps
        w = 2 * math.pi * u
        f = {
            'body': (pitch + stroke * 0.12 * math.sin(w - 0.4), 0, roll * math.sin(w)),
            'head': (-stroke * 0.12 * math.sin(w - 0.9), 0, 0),
            'jaw': (2 + 1.5 * math.sin(w), 0, 0),
            'tail0': (stroke * 0.35 * math.sin(w), 0, 0),
            'tail1': (stroke * 0.55 * math.sin(w - 0.6), 0, 0),
            'tail2': (stroke * 0.75 * math.sin(w - 1.2), 0, 0),
            'fluke': (stroke * 0.9 * math.sin(w - 1.8), 0, 0),
            'right_fin': (fin * 0.3 * math.sin(w + 0.5), 0, fin * math.sin(w + 0.5)),
            'right_fin_tip': (0, 0, fin * 0.6 * math.sin(w - 0.2)),
            'left_fin': (fin * 0.3 * math.sin(w + 0.5), 0, -fin * math.sin(w + 0.5)),
            'left_fin_tip': (0, 0, -fin * 0.6 * math.sin(w - 0.2)),
        }
        for s, sx in (('right', 1), ('left', -1)):
            for k in range(4):
                f[f'{s}_barbel{k}'] = (barb * (10 * math.sin(w - k * 0.7) + 6), 0, sx * barb * 4 * math.sin(w * 0.5 - k * 0.5))
        fr.append((L * u, f))
        pos.append((L * u, (0, 2.0 * math.sin(w + 1.2), 0)))
    pose_seq(a, fr, ALL)
    a.pos('base', *pos)
    return a


A['IDLE'] = swim('idle', 3.6, 14, 8, roll=3, barb=1.0)
A['WALK'] = swim('fly', 1.6, 22, 12, roll=4, barb=1.2, pitch=-2)
A['RUN'] = swim('fly_fast', 0.8, 34, 6, roll=2, barb=1.6, pitch=-6)
A['SIT'] = swim('hover', 5.0, 8, 5, roll=2, barb=0.6)


def act(name, length, frames, base_pos=None, base_scale=None, base_rot=None):
    a = Anim(name, length)
    pose_seq(a, frames, ALL)
    if base_pos: a.pos('base', *base_pos)
    if base_scale: a.scl('base', *base_scale)
    if base_rot: a.rot('base', *base_rot)
    return a


open_mouth = {'head': (-12, 0, 0), 'jaw': (32, 0, 0)}
fins_back = {'right_fin': (10, 30, 30), 'left_fin': (10, -30, -30), 'right_fin_tip': (0, 0, 10), 'left_fin_tip': (0, 0, -10)}
fins_up = {'right_fin': (-10, 0, 40), 'left_fin': (-10, 0, -40), 'right_fin_tip': (0, 0, 20), 'left_fin_tip': (0, 0, -20)}
fins_wrap = {'right_fin': (-20, -50, 20), 'left_fin': (-20, 50, -20), 'right_fin_tip': (0, -40, 0), 'left_fin_tip': (0, 40, 0)}
tail_up = {'tail0': (-12, 0, 0), 'tail1': (-18, 0, 0), 'tail2': (-22, 0, 0), 'fluke': (-25, 0, 0)}
tail_down = {'tail0': (14, 0, 0), 'tail1': (20, 0, 0), 'tail2': (24, 0, 0), 'fluke': (28, 0, 0)}

# basic attack: tail slap swinging around + bite
A['ATTACK'] = act('attack', 0.9, [
    (0, {}),
    (0.25, {**open_mouth, 'body': (-6, 14, 0), 'tail0': (0, -20, 0), 'tail1': (0, -25, 0), 'tail2': (0, -25, 0)}),
    (0.45, {'body': (8, -18, 0), 'head': (10, 0, 0), 'jaw': (4, 0, 0), 'tail0': (0, 30, 0), 'tail1': (0, 35, 0), 'tail2': (0, 30, 0), 'fluke': (-20, 0, 0)}),
    (0.65, {'body': (2, -6, 0), 'tail0': (0, 10, 0)}),
    (0.9, {}),
], base_pos=[(0, (0, 0, 0)), (0.25, (0, 2, 6)), (0.45, (0, -2, -12)), (0.9, (0, 0, 0))])

# ice beam: rear back, open wide, hold steady while the beam pours out
beam_pose = {**open_mouth, 'body': (-8, 0, 0), 'head': (-6, 0, 0), 'jaw': (42, 0, 0), **fins_up}
A['ICE_BEAM'] = act('ice_beam', 2.6, [(0, {}), (0.4, {**beam_pose, 'head': (-22, 0, 0), **tail_down}), (0.6, beam_pose), (2.2, {**beam_pose, 'body': (-6, 0, 0)}), (2.6, {})],
                    base_pos=[(0, (0, 0, 0)), (0.4, (0, 4, 8)), (0.6, (0, 0, 0)), (2.2, (0, 0, 4)), (2.6, (0, 0, 0))])

# whirlpool: barrel-roll spin that whips the water around
A['WHIRLPOOL'] = act('whirlpool', 2.2, [(0, {}), (0.3, {**fins_back, **tail_up}), (1.6, {**fins_back, **tail_up, 'jaw': (20, 0, 0)}), (2.2, {})],
                     base_rot=[(0, (0, 0, 0)), (0.3, (0, 0, 0)), (0.8, (0, 0, 360), 'L'), (1.3, (0, 0, 720), 'L'), (1.6, (0, 0, 720)), (2.2, (0, 0, 720))])

# sound wave: head up, jaw open, body pulses
sonic = {**open_mouth, 'head': (-26, 0, 0), 'jaw': (46, 0, 0), 'body': (-10, 0, 0), **fins_up}
A['SONIC'] = act('sonic', 1.8, [(0, {}), (0.35, {**sonic, 'head': (-10, 0, 0), 'jaw': (10, 0, 0)}), (0.6, sonic), (0.8, {**sonic, 'body': (-4, 0, 0)}), (1.0, sonic), (1.4, sonic), (1.8, {})],
                  base_scale=[(0, (1, 1, 1)), (0.55, (1, 1, 1)), (0.62, (1.06, 1.06, 1.06)), (0.75, (1, 1, 1)), (0.95, (1.05, 1.05, 1.05)), (1.1, (1, 1, 1)), (1.8, (1, 1, 1))])

# exploding fish: spit fish out in bursts
spit = {**open_mouth, 'jaw': (36, 0, 0)}
spit_c = {'head': (8, 0, 0), 'jaw': (6, 0, 0)}
A['FISH_BOMB'] = act('fish_bomb', 1.8, [(0, {}), (0.3, spit_c), (0.45, spit), (0.65, spit_c), (0.8, spit), (1.0, spit_c), (1.15, spit), (1.5, spit_c), (1.8, {})])

# torpedo: tail cocked back then whip forward, launching three torpedoes
A['TORPEDO'] = act('torpedo', 1.5, [(0, {}), (0.35, {**tail_up, **open_mouth, 'body': (-6, 0, 0)}), (0.5, {**tail_down, 'jaw': (36, 0, 0), 'body': (6, 0, 0)}),
                                    (0.7, {**tail_up, 'jaw': (30, 0, 0)}), (0.85, {**tail_down, 'jaw': (36, 0, 0)}), (1.0, {**tail_up}), (1.15, {**tail_down, 'jaw': (36, 0, 0)}), (1.5, {})])

# quick attack: streamlined dash (fins pinned back)
dash = {**fins_back, 'head': (6, 0, 0), 'tail0': (-6, 0, 0), 'tail1': (8, 0, 0), 'tail2': (-8, 0, 0), 'fluke': (12, 0, 0)}
A['RUSH'] = act('rush', 2.0, [(0, {}), (0.2, dash), (0.5, {**dash, 'jaw': (30, 0, 0)}), (0.7, dash), (1.0, {**dash, 'jaw': (30, 0, 0)}), (1.2, dash), (1.5, {**dash, 'jaw': (30, 0, 0)}), (2.0, {})],
                base_rot=[(0, (0, 0, 0)), (0.5, (0, 0, -25)), (1.0, (0, 0, 25)), (1.5, (0, 0, -20)), (2.0, (0, 0, 0))])

# waterspout: dive the head down and fling the tail up
A['WATERSPOUT'] = act('waterspout', 2.0, [(0, {}), (0.5, {'body': (24, 0, 0), 'head': (10, 0, 0), **tail_up, **fins_up}),
                                          (0.8, {'body': (-20, 0, 0), **tail_down, **fins_back, 'jaw': (30, 0, 0)}), (1.4, {'body': (-10, 0, 0), 'jaw': (20, 0, 0)}), (2.0, {})],
                      base_pos=[(0, (0, 0, 0)), (0.5, (0, 6, 0)), (0.8, (0, -4, 0)), (2.0, (0, 0, 0))])

# ice shield: curl up, fins wrapped around
curl = {**fins_wrap, 'body': (14, 0, 0), 'head': (16, 0, 0), 'tail0': (-20, 0, 0), 'tail1': (-28, 0, 0), 'tail2': (-30, 0, 0), 'fluke': (-30, 0, 0)}
A['ICE_SHIELD'] = act('ice_shield', 1.6, [(0, {}), (0.4, curl), (1.2, curl), (1.6, {})])

# quick defence: snap roll to the side and a counter tail swat
A['QUICK_GUARD'] = act('quick_guard', 0.8, [(0, {}), (0.15, {**fins_back, 'body': (0, 20, 0)}), (0.4, {'body': (0, -25, 0), 'tail0': (0, 30, 0), 'tail1': (0, 35, 0), 'fluke': (-20, 0, 0)}), (0.8, {})],
                       base_rot=[(0, (0, 0, 0)), (0.25, (0, 0, 90)), (0.45, (0, 0, 180), 'L'), (0.6, (0, 0, 360), 'L'), (0.8, (0, 0, 360))])

# Erkemas clamp: huge lunge, jaws wide, snap shut and shake
A['ERKEMAS'] = act('erkemas', 2.4, [
    (0, {}),
    (0.35, {**open_mouth, 'head': (-24, 0, 0), 'jaw': (55, 0, 0), **fins_back, **tail_down}),
    (0.55, {'head': (4, 0, 0), 'jaw': (0, 0, 0), **fins_back, **tail_up}),
    (0.8, {'head': (6, 14, 0), 'jaw': (0, 0, 0), 'body': (0, -10, 0)}),
    (1.05, {'head': (6, -14, 0), 'jaw': (0, 0, 0), 'body': (0, 10, 0)}),
    (1.3, {'head': (6, 14, 0), 'jaw': (0, 0, 0), 'body': (0, -10, 0)}),
    (1.55, {'head': (6, -14, 0), 'jaw': (0, 0, 0), 'body': (0, 10, 0)}),
    (1.9, {'head': (-30, 0, 0), 'jaw': (40, 0, 0), 'body': (-14, 0, 0), **tail_down}),
    (2.4, {}),
], base_pos=[(0, (0, 0, 0)), (0.35, (0, 2, 10)), (0.55, (0, 0, -22)), (1.6, (0, 0, -14)), (1.9, (0, 6, -10)), (2.4, (0, 0, 0))])

# sealed power: arch back, everything flares, then a violent release
flare = {**fins_up, 'body': (-18, 0, 0), 'head': (-28, 0, 0), 'jaw': (50, 0, 0), **tail_up}
A['SEAL'] = act('seal', 3.0, [(0, {}), (0.8, {**fins_wrap, 'body': (16, 0, 0), 'head': (16, 0, 0)}), (1.6, {**fins_wrap, 'body': (20, 0, 0), 'head': (20, 0, 0)}),
                              (1.9, flare), (2.5, flare), (3.0, {})],
                base_scale=[(0, (1, 1, 1)), (1.6, (0.92, 0.92, 0.92)), (1.9, (1.18, 1.18, 1.18)), (2.2, (1.08, 1.08, 1.08)), (3.0, (1, 1, 1))],
                base_pos=[(0, (0, 0, 0)), (1.6, (0, -4, 0)), (1.9, (0, 10, 0)), (3.0, (0, 0, 0))])

# space power: rise up, turn belly to the sky and call the stars
A['SPACE'] = act('space', 2.6, [(0, {}), (0.6, {**fins_up, 'body': (-30, 0, 0), 'head': (-20, 0, 0), 'jaw': (30, 0, 0)}),
                                (1.6, {**fins_up, 'body': (-34, 0, 0), 'head': (-26, 0, 0), 'jaw': (40, 0, 0)}), (2.6, {})],
                 base_pos=[(0, (0, 0, 0)), (0.6, (0, 16, 0)), (1.6, (0, 18, 0)), (2.6, (0, 0, 0))],
                 base_rot=[(0, (0, 0, 0)), (0.6, (0, 0, 0)), (1.6, (0, 0, 180)), (2.2, (0, 0, 360)), (2.6, (0, 0, 360))])

# entrance from the marble: grow out of a point, breach-spin and sing
A['RELEASE'] = act('release', 1.8, [(0, {}), (0.6, {**fins_up, **open_mouth, 'head': (-24, 0, 0), 'jaw': (40, 0, 0)}), (1.3, {**fins_up, 'jaw': (30, 0, 0)}), (1.8, {})],
                   base_scale=[(0, (0.02, 0.02, 0.02)), (0.35, (1.1, 1.1, 1.1)), (0.55, (0.96, 0.96, 0.96)), (0.7, (1, 1, 1)), (1.8, (1, 1, 1))],
                   base_rot=[(0, (0, 0, 0)), (0.35, (0, 0, 180)), (0.7, (0, 0, 360)), (1.8, (0, 0, 360))])

A['RECALL'] = act('recall', 1.25, [(0, {}), (0.45, curl), (1.25, curl)],
                  base_scale=[(0, (1, 1, 1)), (0.5, (1, 1, 1)), (1.0, (0.05, 0.05, 0.05)), (1.25, (0.01, 0.01, 0.01))])

A['DEATH'] = act('death', 3.0, [(0, {}), (0.4, {**open_mouth, 'head': (-30, 0, 0), 'jaw': (50, 0, 0), **fins_up}), (1.4, {'jaw': (20, 0, 0), 'right_fin': (0, 0, 40), 'left_fin': (0, 0, -40), **tail_down}),
                                 (3.0, {'jaw': (20, 0, 0), 'right_fin': (0, 0, 50), 'left_fin': (0, 0, -50), **tail_down})],
                 base_rot=[(0, (0, 0, 0)), (0.4, (0, 0, 0)), (1.6, (0, 0, 160)), (3.0, (0, 0, 175))],
                 base_pos=[(0, (0, 0, 0)), (1.6, (0, 40, 0)), (3.0, (0, 40, 0))])
