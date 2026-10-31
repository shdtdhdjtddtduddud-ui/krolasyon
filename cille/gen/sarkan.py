"""Şarkan (Cille) — sand and rock warrior: model, textures and animations.

Model units: feet at y=24, y down, front = -z. Rendered at scale 0.5 => 32 units per block (~3.4 blocks tall).
Reference: heavy humanoid, dark grey body, beige cloth wrapped diagonally over the chest, around the waist and the
forearms, a beige loincloth flap hanging at the front, dark draped cloak / scarf over the shoulders, small dark
hooded head, big dark gauntlet fists and huge blue-grey armoured boots up to the knees.
"""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, glow_img
from animlib import pose_seq, sym
from shapes import ellipsoid, ellipsoid_l, limb, texel_world, Noise3, light, lerp, ramp, smooth

M = Model('sarkan', 1024, 1024)


def H(h):
    return 24 - h


# ---------------------------------------------------------------- skeleton
from shapes import ROUND
R5 = dict(cross=ROUND)

M.bone('base', None, (0, 24, 0))
M.bone('body', 'base', (0, H(52), 0))                               # pelvis
ellipsoid(M, 'body', (0, H(52), 0), (12, 7, 8.5), 'body', axis='y', slices=4, **R5)
ellipsoid(M, 'body', (0, H(55), 0), (12.6, 4, 9), 'wrap', axis='y', slices=3, **R5)       # belt wrap
# front loincloth: a long tapering beige flap ending in a point at knee height
M.bone('cloth', 'body', (0, H(53), -9))
for i, (w, h) in enumerate(((11, 7), (10, 7), (8, 6), (6, 5), (3.5, 4))):
    y0 = sum(hh for _, hh in ((11, 7), (10, 7), (8, 6), (6, 5), (3.5, 4))[:i])
    M.cube_l('cloth', (-w / 2, y0, -1.1), (w, h + 0.2, 2.2), 'cloth')
M.bone('back_cloth', 'body', (0, H(53), 9))
M.cube_l('back_cloth', (-7, 0, -1.0), (14, 10, 2.0), 'cloth')

M.bone('chest', 'body', (0, H(57), 0))
ellipsoid(M, 'chest', (0, H(64), 0), (11.5, 8, 8), 'wrap', axis='y', slices=4, **R5)      # bandaged belly
ellipsoid(M, 'chest', (0, H(77), 0), (16, 10, 9.5), 'torso', axis='y', slices=5, **R5)    # chest
# beige scarf collar around the neck and upper chest
ellipsoid(M, 'chest', (0, H(87), -1), (13, 4.5, 10.5), 'wrap', axis='y', slices=3, **R5)
ellipsoid(M, 'chest', (0, H(81), -6.5), (9, 6.5, 4), 'wrap', axis='y', slices=4, **R5)          # scarf hanging over the chest
# dark cloak: big rounded shoulder drapes, draped over the back
for sx in (-1, 1):
    ellipsoid(M, 'chest', (sx * 16, H(85), 1), (10, 7, 11), 'cloak', axis='y', slices=4, **R5)
ellipsoid(M, 'chest', (0, H(84), 6), (17, 6, 7), 'cloak', axis='y', slices=3, **R5)
M.bone('cape', 'chest', (0, H(84), 9))
for i, (w, h) in enumerate(((32, 10), (30, 10), (27, 9), (23, 8))):
    y0 = sum(hh for _, hh in ((32, 10), (30, 10), (27, 9), (23, 8))[:i])
    M.cube_l('cape', (-w / 2, y0, -1.6 + i * 0.3), (w, h + 0.2, 3.0), 'cloak')
# the cloak's long fold hanging over the right shoulder / arm (as in the picture)
M.bone('drape', 'chest', (-18, H(86), 0))
M.bones['drape'].rot = [0.0, 0.0, 14.0]
for i, (w, h, d) in enumerate(((11, 9, 12), (9.5, 9, 10.5), (8, 8, 9))):
    y0 = sum(hh for _, hh, _ in ((11, 9, 12), (9.5, 9, 10.5), (8, 8, 9))[:i])
    M.cube_l('drape', (-w / 2 - 2, y0, -d / 2), (w, h + 0.2, d), 'cloak')

# perfectly round dark head
M.bone('head', 'chest', (0, H(90), -1))
ellipsoid_l(M, 'head', (0, -9, -0.5), (8.6, 9.2, 8.8), 'head', axis='y', slices=8, **R5)
ellipsoid_l(M, 'head', (0, -0.5, 0.5), (6.5, 2.5, 6.5), 'head', axis='y', slices=2, **R5)    # neck

# ---- arms: dark upper arms, beige-wrapped forearms, big rounded gauntlet fists
for side, sx in (('right', -1), ('left', 1)):
    A = (sx * 19, H(83), 0)
    B = (sx * 26, H(65), -1)
    C = (sx * 29, H(48), -3)
    limb(M, f'{side}_arm', 'chest', A, B, 10.5, 10.5, 9, 9, 'body', extend=3, slices=4)
    limb(M, f'{side}_fore', f'{side}_arm', B, C, 9.5, 9.5, 10, 10, 'wrap', extend=2, slices=4)
    M.bone_w(f'{side}_hand', f'{side}_fore', C, M.world(f'{side}_fore')[:3, :3])
    ellipsoid_l(M, f'{side}_hand', (0, 5.5, -0.5), (7.6, 7.4, 7.2), 'gauntlet', axis='y', slices=6, **R5)
    ellipsoid_l(M, f'{side}_hand', (0, -0.5, 0), (6.2, 2.5, 6.2), 'gauntlet', axis='y', slices=2, **R5)   # cuff

# ---- legs: long dark legs and huge rounded armoured boots from the knee down
for side, sx in (('right', -1), ('left', 1)):
    A = (sx * 8, H(50), 0)
    B = (sx * 10, H(30), -1)
    C = (sx * 11, H(7), 1)
    limb(M, f'{side}_thigh', 'body', A, B, 11, 11, 9.5, 9.5, 'body', extend=3, slices=4)
    limb(M, f'{side}_shin', f'{side}_thigh', B, C, 9, 9, 8, 8, 'body', extend=1, slices=2)
    # big rounded armoured boot shell from the knee down, with a rim at the top
    ellipsoid_l(M, f'{side}_shin', (0, 12.5, 0), (11.5, 12.5, 11.8), 'boot', axis='y', slices=7, **R5)
    ellipsoid_l(M, f'{side}_shin', (0, 1.0, 0), (10.0, 2.4, 10.2), 'boot_plate', axis='y', slices=2, **R5)
    ellipsoid_l(M, f'{side}_shin', (0, 0.5, -3.5), (7.5, 4.5, 6.5), 'boot_plate', axis='y', slices=3, **R5)   # knee cap
    M.bone_w(f'{side}_foot', f'{side}_shin', C, np.eye(3))
    ellipsoid_l(M, f'{side}_foot', (0, 2.8, -3.5), (9.5, 3.8, 12), 'boot', axis='z', slices=5, **R5)

# ---------------------------------------------------------------- textures (world space)
P = Painter(M)
SKIN = [(30, 31, 36), (52, 54, 60), (78, 80, 88), (110, 112, 120)]
BEIGE = [(140, 126, 100), (184, 172, 146), (214, 206, 184), (236, 230, 212)]
CLOAK = [(26, 26, 30), (46, 46, 52), (70, 70, 78), (98, 98, 106)]
BOOT = [(30, 36, 46), (58, 68, 84), (88, 102, 120), (126, 140, 158)]
N1, N2, N3 = Noise3(31, freq=0.15), Noise3(32, freq=0.4), Noise3(33, freq=0.07)


def world_mat(fn):
    def wrapped(c, face, W, H_, r):
        Pw, n = texel_world(M, c, face, W, H_)
        col, alpha, glow = fn(c, face, Pw, n, W, H_)
        L = light(n)
        under = 0.82 if n[1] > 0.6 else 1.0
        return np.clip(col * L * under, 0, 255), alpha if alpha is not None else np.ones((H_, W)), glow
    return wrapped


def cloth_wrap(Pw, base):
    """beige bandage cloth: overlapping bands with seams and stitches"""
    h = 24 - Pw[..., 1]
    band = np.sin(h * 0.9 + Pw[..., 0] * 0.15) * 0.5 + 0.5
    t = base + 0.12 * (N2(Pw) - 0.5) + 0.1 * band
    col = ramp(t, BEIGE)
    seam = smooth(0.85, 0.97, band)
    return lerp(col, (120, 106, 82), seam * 0.6)


def chest_bandage(Pw):
    """the diagonal sash from the right shoulder to the left hip + a band under the chest"""
    h = 24 - Pw[..., 1]
    under = smooth(3.5, 2.0, np.abs(h - 68))
    return np.clip(under, 0, 1)


@world_mat
def body_mat(c, face, Pw, n, W, H_):
    t = 0.45 + 0.14 * (N1(Pw) - 0.5)
    col = ramp(t, SKIN)
    return col, None, None


@world_mat
def torso_mat(c, face, Pw, n, W, H_):
    col = ramp(0.45 + 0.14 * (N1(Pw) - 0.5), SKIN)
    m = chest_bandage(Pw)
    return lerp(col, cloth_wrap(Pw, 0.55), m), None, None


@world_mat
def wrap_mat(c, face, Pw, n, W, H_):
    return cloth_wrap(Pw, 0.55), None, None


@world_mat
def cloth_mat(c, face, Pw, n, W, H_):
    col = cloth_wrap(Pw, 0.6)
    fray = smooth(0.7, 0.9, N2(Pw * 3)) * smooth(30, 26, 24 - Pw[..., 1])
    return lerp(col, (150, 136, 110), fray), None, None


@world_mat
def cloak_mat(c, face, Pw, n, W, H_):
    folds = 0.5 + 0.5 * np.sin(Pw[..., 0] * 0.7 + N1(Pw) * 4)
    return ramp(0.35 + 0.2 * folds + 0.1 * (N2(Pw) - 0.5), CLOAK), None, None


@world_mat
def head_mat(c, face, Pw, n, W, H_):
    # smooth dark blue-grey head with a soft highlight on top
    h = 24 - Pw[..., 1]
    return ramp(0.36 + 0.08 * (N2(Pw) - 0.5) + 0.12 * smooth(96, 106, h), [(22, 26, 36), (40, 46, 60), (62, 70, 88), (92, 100, 120)]), None, None


@world_mat
def hood_mat(c, face, Pw, n, W, H_):
    col = ramp(0.38 + 0.12 * (N1(Pw) - 0.5), CLOAK)
    # face opening: lighter grey mask band across the eyes at the front
    h = 24 - Pw[..., 1]
    front = smooth(-3, -7, Pw[..., 2])
    mask = front * smooth(3.5, 2.0, np.abs(h - 98.5))
    return lerp(col, (20, 20, 24), mask), None, None


@world_mat
def gauntlet_mat(c, face, Pw, n, W, H_):
    col = ramp(0.38 + 0.12 * (N2(Pw) - 0.5), CLOAK)
    knuck = smooth(0.8, 0.95, np.sin(Pw[..., 0] * 1.4)) * smooth(0, -0.6, n[2])
    return lerp(col, (96, 98, 108), knuck * 0.7), None, None


@world_mat
def boot_mat(c, face, Pw, n, W, H_):
    h = 24 - Pw[..., 1]
    t = 0.45 + 0.12 * (N1(Pw) - 0.5) + 0.12 * smooth(-0.2, -0.8, n[2])
    col = ramp(t, BOOT)
    seam = smooth(1.2, 0.4, np.abs(h - 22)) + smooth(1.2, 0.4, np.abs(h - 13))     # two plate edges
    col = lerp(col, (24, 28, 36), np.clip(seam, 0, 1) * 0.7)
    return col, None, None


@world_mat
def boot_plate_mat(c, face, Pw, n, W, H_):
    return ramp(0.62 + 0.1 * (N2(Pw) - 0.5), BOOT), None, None


P.mats.update({'body': body_mat, 'torso': torso_mat, 'wrap': wrap_mat, 'cloth': cloth_mat, 'cloak': cloak_mat,
               'head': head_mat, 'hood': hood_mat, 'gauntlet': gauntlet_mat, 'boot': boot_mat, 'boot_plate': boot_plate_mat})


@P.mat('eye')
def eye(c, face, W, H_, r):
    col = np.zeros((H_, W, 3), np.float32)
    col[:] = (230, 210, 150)
    return col, np.ones((H_, W)), glow_img(H_, W, (255, 220, 140), np.ones((H_, W)) * 0.9)


# ---------------------------------------------------------------- animations
ARMS = ['right_arm', 'right_fore', 'right_hand', 'left_arm', 'left_fore', 'left_hand']
LEGS = ['right_thigh', 'right_shin', 'right_foot', 'left_thigh', 'left_shin', 'left_foot']
ALL = ['body', 'chest', 'head', 'cloth', 'back_cloth', 'cape'] + ARMS + LEGS
A = {}

guard = sym({'right_arm': (-20, 0, 10), 'right_fore': (-70, 20, 0), 'right_hand': (0, 0, 0)})


def loop(name, L, fn, steps=12):
    a = Anim(name, L, loop=True)
    fr, pos = [], []
    for i in range(steps + 1):
        u = i / steps
        f, p = fn(2 * math.pi * u)
        fr.append((L * u, f))
        pos.append((L * u, p))
    pose_seq(a, fr, ALL)
    a.pos('base', *pos)
    return a


def idle(w):
    f = dict(guard)
    f['chest'] = (2 + 1.5 * math.sin(w), 0, 0)
    f['head'] = (-2 * math.sin(w), 4 * math.sin(w * 0.5), 0)
    f['right_fore'] = (-70 + 3 * math.sin(w), 20, 0)
    f['left_fore'] = (-70 + 3 * math.sin(w + 1), -20, 0)
    f['cape'] = (6 + 3 * math.sin(w), 0, 0)
    f['cloth'] = (2 * math.sin(w + 1), 0, 0)
    f['right_thigh'] = (-6, 0, 4); f['left_thigh'] = (6, 0, -4)
    f['right_shin'] = (8, 0, 0); f['left_shin'] = (4, 0, 0)
    return f, (0, -1 + 0.6 * math.sin(w), 0)


def walk(w, amp=28, run=False):
    s = math.sin(w)
    f = {
        'right_thigh': (-amp * s, 0, 0), 'left_thigh': (amp * s, 0, 0),
        'right_shin': (max(0, amp * 1.4 * math.sin(w - 1.2)), 0, 0), 'left_shin': (max(0, -amp * 1.4 * math.sin(w - 1.2)), 0, 0),
        'right_foot': (amp * 0.3 * s, 0, 0), 'left_foot': (-amp * 0.3 * s, 0, 0),
        'right_arm': (amp * 0.8 * s - (20 if run else 0), 0, 8), 'left_arm': (-amp * 0.8 * s - (20 if run else 0), 0, -8),
        'right_fore': (-30 - (50 if run else 0), 0, 0), 'left_fore': (-30 - (50 if run else 0), 0, 0),
        'chest': ((12 if run else 4), 8 * s, 0), 'head': ((-8 if run else -2), -6 * s, 0),
        'cape': ((30 if run else 12) + 6 * math.sin(2 * w), 0, 0), 'cloth': (amp * 0.4 * abs(s), 0, 0), 'back_cloth': (10 if run else 4, 0, 0),
    }
    return f, (0, (4 if run else 2) * abs(math.cos(w)), 0)


A['IDLE'] = loop('idle', 3.0, idle)
A['WALK'] = loop('walk', 1.1, lambda w: walk(w, 26))
A['RUN'] = loop('run', 0.65, lambda w: walk(w, 42, True))
kneel = sym({'right_thigh': (-80, 0, 6), 'right_shin': (110, 0, 0), 'right_foot': (-30, 0, 0), 'chest': (6, 0, 0),
             'right_arm': (-10, 0, 14), 'right_fore': (-40, 0, 0)})
kneel['left_thigh'] = (10, 0, -4); kneel['left_shin'] = (120, 0, 0); kneel['left_foot'] = (-40, 0, 0)
A['SIT'] = loop('kneel', 4.0, lambda w: ({**kneel, 'chest': (6 + 1.5 * math.sin(w), 0, 0), 'cape': (4, 0, 0)}, (0, -22, 0)))


def act(name, length, frames, base_pos=None, base_rot=None, base_scale=None):
    a = Anim(name, length)
    pose_seq(a, frames, ALL)
    if base_pos: a.pos('base', *base_pos)
    if base_rot: a.rot('base', *base_rot)
    if base_scale: a.scl('base', *base_scale)
    return a


jab_r = {**guard, 'right_arm': (-85, 0, 0), 'right_fore': (-5, 0, 0), 'chest': (4, -25, 0), 'body': (0, -10, 0)}
jab_l = {**guard, 'left_arm': (-85, 0, 0), 'left_fore': (-5, 0, 0), 'chest': (4, 25, 0), 'body': (0, 10, 0)}
wind_r = {**guard, 'right_arm': (10, 0, 20), 'right_fore': (-90, 0, 0), 'chest': (0, 25, 0)}
A['ATTACK'] = act('punch', 0.9, [(0, guard), (0.2, wind_r), (0.32, jab_r), (0.45, {**guard, 'chest': (0, 15, 0)}), (0.58, jab_l), (0.9, guard)],
                  base_pos=[(0, (0, 0, 0)), (0.32, (0, 0, -4)), (0.58, (0, 0, -5)), (0.9, (0, 0, 0))])

# sand vortex: arms circle in front, then thrust both palms forward
circle1 = {'right_arm': (-90, 0, 30), 'right_fore': (-30, 0, 0), 'left_arm': (-60, 0, -40), 'left_fore': (-50, 0, 0), 'chest': (0, 20, 0)}
circle2 = {'right_arm': (-60, 0, 40), 'right_fore': (-50, 0, 0), 'left_arm': (-90, 0, -30), 'left_fore': (-30, 0, 0), 'chest': (0, -20, 0)}
thrust = sym({'right_arm': (-90, 0, -8), 'right_fore': (-5, 0, 0), 'right_hand': (-30, 0, 0), 'chest': (8, 0, 0), 'right_thigh': (-25, 0, 0), 'right_shin': (30, 0, 0)})
A['SAND_VORTEX'] = act('sand_vortex', 2.0, [(0, guard), (0.35, circle1), (0.6, circle2), (0.85, circle1), (1.1, thrust), (1.6, thrust), (2.0, guard)],
                       base_pos=[(0, (0, 0, 0)), (1.1, (0, -4, -6)), (1.6, (0, -4, -6)), (2.0, (0, 0, 0))])

# atlas: crouch, huge leap, double-fist hammer down
crouch = sym({'right_thigh': (-50, 0, 6), 'right_shin': (80, 0, 0), 'right_foot': (-30, 0, 0), 'chest': (25, 0, 0),
              'right_arm': (30, 0, 20), 'right_fore': (-40, 0, 0)})
raise_ = sym({'right_arm': (-170, 0, -10), 'right_fore': (-30, 0, 0), 'chest': (-15, 0, 0), 'head': (-15, 0, 0),
              'right_thigh': (-30, 0, 0), 'right_shin': (60, 0, 0), 'cape': (40, 0, 0)})
smash = sym({'right_arm': (-40, 0, -10), 'right_fore': (-10, 0, 0), 'chest': (40, 0, 0), 'head': (10, 0, 0),
             'right_thigh': (-70, 0, 8), 'right_shin': (100, 0, 0), 'right_foot': (-30, 0, 0)})
A['ATLAS'] = act('atlas', 2.0, [(0, guard), (0.35, crouch), (0.55, raise_), (0.95, raise_), (1.15, smash), (1.6, smash), (2.0, guard)],
                 base_pos=[(0, (0, 0, 0)), (0.35, (0, -14, 0)), (0.55, (0, 6, 0)), (1.15, (0, -18, 0)), (1.6, (0, -16, 0)), (2.0, (0, 0, 0))])

# deveran: rapid alternating blows behind a raised guard
frames = [(0, guard), (0.25, {**guard, 'chest': (8, 0, 0)})]
t = 0.3
k = 0
while t < 1.75:
    frames.append((t, jab_r if k % 2 == 0 else jab_l))
    frames.append((t + 0.07, {**guard, 'chest': (6, 0, 0)}))
    t += 0.15
    k += 1
uppercut = {**guard, 'right_arm': (-160, 0, 0), 'right_fore': (-60, 0, 0), 'chest': (-15, -20, 0), 'right_thigh': (-20, 0, 0)}
frames += [(1.85, wind_r), (2.0, uppercut), (2.4, guard)]
A['DEVERAN'] = act('deveran', 2.4, frames, base_pos=[(0, (0, 0, 0)), (0.3, (0, -3, -3)), (1.75, (0, -3, -6)), (2.0, (0, 4, -8)), (2.4, (0, 0, 0))])

# desert storm: arms spread wide, spin
spread = sym({'right_arm': (0, 0, 80), 'right_fore': (0, 0, 10), 'chest': (-8, 0, 0), 'head': (-12, 0, 0), 'cape': (50, 0, 0)})
A['STORM'] = act('storm', 2.2, [(0, guard), (0.4, spread), (1.8, spread), (2.2, guard)],
                 base_rot=[(0, (0, 0, 0)), (0.4, (0, 0, 0)), (0.9, (0, 360, 0), 'L'), (1.4, (0, 720, 0), 'L'), (1.8, (0, 720, 0)), (2.2, (0, 720, 0))])

# sand sea: sweep both arms low across, push a wave forward
sweep1 = sym({'right_arm': (-30, 0, 40), 'right_fore': (-10, 0, 0), 'chest': (30, 0, 0), 'right_thigh': (-40, 0, 6), 'right_shin': (60, 0, 0)})
sweep2 = sym({'right_arm': (-70, 0, -20), 'right_fore': (0, 0, 0), 'right_hand': (-40, 0, 0), 'chest': (15, 0, 0), 'right_thigh': (-30, 0, 0), 'right_shin': (40, 0, 0)})
A['SAND_SEA'] = act('sand_sea', 2.0, [(0, guard), (0.45, sweep1), (0.8, sweep2), (1.5, sweep2), (2.0, guard)],
                    base_pos=[(0, (0, 0, 0)), (0.45, (0, -10, 0)), (0.8, (0, -6, -4)), (2.0, (0, 0, 0))])

# sand shield: arms crossed in front, braced stance
crossed = {'right_arm': (-80, 0, -30), 'right_fore': (-40, 50, 0), 'left_arm': (-80, 0, 30), 'left_fore': (-40, -50, 0),
           'chest': (10, 0, 0), 'head': (12, 0, 0), 'right_thigh': (-25, 0, 10), 'right_shin': (30, 0, 0), 'left_thigh': (10, 0, -10), 'left_shin': (20, 0, 0)}
A['SAND_SHIELD'] = act('sand_shield', 1.4, [(0, guard), (0.3, crossed), (1.0, crossed), (1.4, guard)],
                       base_pos=[(0, (0, 0, 0)), (0.3, (0, -5, 0)), (1.0, (0, -5, 0)), (1.4, (0, 0, 0))])

# rock throw: crouch, rip the boulder up overhead, hurl it
lift = sym({'right_arm': (-175, 0, -20), 'right_fore': (-20, 0, 0), 'chest': (-12, 0, 0), 'head': (-10, 0, 0)})
throw = sym({'right_arm': (-60, 0, -10), 'right_fore': (0, 0, 0), 'chest': (30, 0, 0), 'right_thigh': (-30, 0, 0), 'right_shin': (30, 0, 0)})
A['ROCK'] = act('rock', 1.6, [(0, guard), (0.35, crouch), (0.6, lift), (0.85, lift), (1.0, throw), (1.6, guard)],
                base_pos=[(0, (0, 0, 0)), (0.35, (0, -14, 0)), (0.6, (0, 0, 0)), (1.0, (0, -3, -5)), (1.6, (0, 0, 0))])

# sand walls: stomp and raise both hands
stomp = {'right_thigh': (-70, 0, 0), 'right_shin': (40, 0, 0), 'chest': (-5, 0, 0), 'right_arm': (-10, 0, 40), 'left_arm': (-10, 0, -40)}
rise = sym({'right_arm': (-130, 0, 30), 'right_fore': (-20, 0, 0), 'chest': (-10, 0, 0), 'head': (-10, 0, 0)})
A['WALLS'] = act('walls', 1.8, [(0, guard), (0.3, stomp), (0.5, {**guard, 'right_thigh': (0, 0, 0)}), (0.9, rise), (1.4, rise), (1.8, guard)],
                 base_pos=[(0, (0, 0, 0)), (0.3, (0, 2, 0)), (0.5, (0, -4, 0)), (1.8, (0, 0, 0))])

A['RELEASE'] = act('release', 1.8, [(0, crouch), (0.5, crouch), (0.9, spread), (1.3, {**spread, 'head': (-20, 0, 0)}), (1.8, guard)],
                   base_scale=[(0, (0.02, 0.02, 0.02)), (0.35, (1.08, 1.08, 1.08)), (0.55, (0.97, 0.97, 0.97)), (0.7, (1, 1, 1)), (1.8, (1, 1, 1))],
                   base_pos=[(0, (0, -14, 0)), (0.5, (0, -14, 0)), (0.9, (0, 0, 0)), (1.8, (0, 0, 0))])
A['RECALL'] = act('recall', 1.25, [(0, guard), (0.45, kneel), (1.25, kneel)],
                  base_pos=[(0, (0, 0, 0)), (0.45, (0, -22, 0)), (1.25, (0, -22, 0))],
                  base_scale=[(0, (1, 1, 1)), (0.5, (1, 1, 1)), (1.0, (0.05, 0.05, 0.05)), (1.25, (0.01, 0.01, 0.01))])
fallen = sym({'right_thigh': (-80, 0, 6), 'right_shin': (100, 0, 0), 'chest': (30, 0, 0), 'head': (20, 0, 0), 'right_arm': (-40, 0, 20)})
A['DEATH'] = act('death', 3.0, [(0, guard), (0.6, kneel), (1.6, fallen), (3.0, fallen)],
                 base_pos=[(0, (0, 0, 0)), (0.6, (0, -22, 0)), (1.6, (0, -40, -20)), (3.0, (0, -40, -20))],
                 base_rot=[(0, (0, 0, 0)), (0.6, (0, 0, 0)), (1.6, (70, 0, 0)), (3.0, (80, 0, 0))])
