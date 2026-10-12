"""Four Arms (Tetramand): huge red brute, four arms, white shirt with black yoke + centre stripe, black pants with grey stripes."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('fourarms', 512, 256)
hc = box_helpers(M)
FIT = 92
REF = 'fourarms'

SKIN = (206, 18, 50)
SKIN_D = (138, 8, 32)
WHITE = (238, 238, 240)
WHITE_D = (178, 178, 186)
BLACK = (22, 22, 26)

M.bone('base', None, (0, 48 - 38, 0))
M.bone('body', 'base', (0, 48 - 40, 0))
hc('body', -10.5, 47, -6.5, 21, 9, 13, 'pants_top')
hc('body', -11.5, 57, -7.5, 23, 11, 15, 'shirt_abs')
hc('body', -13.5, 72, -8.5, 27, 16, 17, 'shirt_chest')
hc('body', -9, 75.5, -6.5, 18, 4, 13, 'shirt_traps')

M.bone('head', 'body', (0, 48 - 73, -2))
hc('head', -5.5, 84, -9.5, 11, 11.5, 11, 'head')
hc('head', -4.5, 75, -9.8, 9, 3, 6, 'jaw')


def X(s, x, w):
    return x if s == -1 else -(x + w)


def upper_arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 15.5, 48 - 68, 0), (0, 0, -s * 10))
    hc(a, X(s, -22.5, 10), 74, -5.5, 10, 9.5, 11, 'sleeve_omni' if s == 1 else 'sleeve')
    hc(a, X(s, -21.5, 8.5), 65, -4.25, 8.5, 14, 8.5, 'skin')
    f = f'{side}_fore'
    M.bone(f, a, (s * 17.25, 48 - 51, 0))
    hc(f, X(s, -22.0, 9.5), 51.5, -4.75, 9.5, 16, 9.5, 'skin')
    for k, (hh, ln) in enumerate(((49, 4.0), (44.5, 4.5), (40, 3.5))):
        n = f'{side}_spike{k}'
        M.bone(n, f, (s * 21.5, 48 - hh, 2.5), (35, 0, s * -55))
        M.cube(n, (s * 21.5 - 1.2, 48 - hh - ln, 2.5 - 1.2), (2.4, ln, 2.4), 'skin_spike')
    h = f'{side}_hand'
    M.bone(h, f, (s * 17.25, 48 - 36, 0))
    hc(h, X(s, -22.0, 9.5), 36.5, -4.75, 9.5, 6, 9.5, 'glove')
    for fz in (-4.2, -1.5, 1.2):
        hc(h, X(s, -21.4, 2.6), 30.5, fz, 2.6, 5, 2.6, 'skin')
    hc(h, X(s, -14.6, 2.4), 33, -3.5, 2.4, 4.5, 2.4, 'skin')


def lower_arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm2'
    M.bone(a, 'body', (s * 12.5, 48 - 58, 0.5), (0, 0, s * 3))
    hc(a, X(s, -17.0, 7.5), 60, -2.25, 7.5, 13, 7.5, 'skin')
    f = f'{side}_fore2'
    M.bone(f, a, (s * 13.25, 48 - 47, 1.5))
    hc(f, X(s, -17.5, 8.5), 47.5, -2.75, 8.5, 14, 8.5, 'skin')
    for k, (hh, ln) in enumerate(((45, 3.5), (40.5, 3.5))):
        n = f'{side}_spike2{k}'
        M.bone(n, f, (s * 17.0, 48 - hh, 4.0), (35, 0, s * -55))
        M.cube(n, (s * 17.0 - 1.0, 48 - hh - ln, 4.0 - 1.0), (2.0, ln, 2.0), 'skin_spike')
    h = f'{side}_hand2'
    M.bone(h, f, (s * 13.25, 48 - 33.5, 1.5))
    hc(h, X(s, -17.5, 8.5), 34, -2.75, 8.5, 5.5, 8.5, 'glove')
    for fz in (-2.3, 0.1, 2.5):
        hc(h, X(s, -17.0, 2.3), 28.5, fz, 2.3, 4.5, 2.3, 'skin')


def leg(side):
    s = -1 if side == 'right' else 1
    l = f'{side}_leg'
    M.bone(l, 'base', (s * 5.5, 48 - 38, 0))
    hc(l, X(s, -10.5, 10), 39, -5, 10, 17, 10, 'pants')
    sh = f'{side}_shin'
    M.bone(sh, l, (s * 5.5, 48 - 22, 0))
    hc(sh, X(s, -10.0, 9), 22.5, -4.5, 9, 17, 9, 'pants')
    hc(sh, X(s, -10.6, 10.2), 6.2, -6.0, 10.2, 6.2, 11.5, 'foot')
    hc(sh, X(s, -10.4, 4.4), 3.8, -9.6, 4.4, 3.8, 4.0, 'foot')
    hc(sh, X(s, -5.6, 4.4), 3.8, -9.6, 4.4, 3.8, 4.0, 'foot')


for sd in ('right', 'left'):
    upper_arm(sd)
    lower_arm(sd)
    leg(sd)

P = Painter(M)


def skin(c, face, W, H, r):
    col = base(H, W, SKIN, face, SKIN_D, r, 0.05, 0.25)
    yy, xx = grid(H, W)
    if H > 8 and face in ('north', 'west', 'east', 'south'):
        # muscle contour strokes
        for _ in range(max(1, W * H // 90)):
            x0 = r.random() * W; y0 = r.random() * H
            m = stroke(H, W, [(x0, y0), (x0 + (r.random() - 0.5) * 3, y0 + 2 + r.random() * 3)], 0.6)
            col = paint(col, m * 0.7, SKIN_D)
    col = outline(col, H, W, (70, 0, 14), 1, 0.7)
    return col, np.ones((H, W)), None


P.mats['skin'] = skin


@P.mat('skin_spike')
def skin_spike(c, face, W, H, r):
    col = base(H, W, (176, 14, 40), face, (120, 6, 26), r, 0.05, 0.6)
    col = outline(col, H, W, (60, 0, 12), 1, 0.7)
    return col, np.ones((H, W)), None


def shirt(c, face, W, H, r, kind):
    col = base(H, W, WHITE, face, WHITE_D, r, 0.03, 0.2)
    yy, xx = grid(H, W)
    if kind == 'chest':
        if face in ('north', 'south'):
            # black yoke across the top and the centre stripe running down (front only)
            col = paint(col, band_h(H, W, 0, 4), BLACK)
            if face == 'north':
                col = paint(col, band_v(H, W, W / 2 - 2, W / 2 + 2), BLACK)
            # pec shading lines
            col = paint(col, stroke(H, W, [(2, H * 0.72), (W / 2 - 3, H * 0.86)], 0.7) * 0.6, WHITE_D)
            col = paint(col, stroke(H, W, [(W - 2, H * 0.72), (W / 2 + 3, H * 0.86)], 0.7) * 0.6, WHITE_D)
        elif face in ('west', 'east'):
            col = paint(col, band_h(H, W, 0, 4), BLACK)
        elif face == 'top':
            col = paint(col, np.ones((H, W)), BLACK)
    elif kind == 'traps':
        col = paint(col, np.ones((H, W)), BLACK)
    elif kind == 'abs':
        if face == 'north':
            col = paint(col, band_v(H, W, W / 2 - 2, W / 2 + 2), BLACK)
    elif kind in ('sleeve', 'sleeve_omni'):
        if face != 'top':
            col = paint(col, band_h(H, W, H - 2.5, H), BLACK)
        if face == 'top':
            col = paint(col, band_v(H, W, W / 2 - 1.5, W / 2 + 1.5), BLACK)
        if kind == 'sleeve_omni' and face == 'south':
            layers = omnitrix_symbol(H, W, W * 0.5, H * 0.4, 2.6, ring=(40, 40, 44), rim=(120, 120, 128), green=(150, 150, 156), dark=(60, 60, 64))
            col = apply(col, layers)
    col = outline(col, H, W, (40, 40, 48), 1, 0.65)
    return col, np.ones((H, W)), None


for k in ('chest', 'abs', 'traps', 'sleeve', 'sleeve_omni'):
    P.mats['shirt_' + k if k in ('chest', 'abs', 'traps') else k] = (lambda k: lambda c, f, W, H, r: shirt(c, f, W, H, r, k))(k)


def pants(c, face, W, H, r, top=False):
    col = base(H, W, (24, 24, 28), face, (10, 10, 12), r, 0.04, 0.15)
    if face in ('west', 'east'):
        # grey side stripe
        col = paint(col, band_v(H, W, W / 2 - 1.2, W / 2 + 1.2), (120, 122, 136))
    if top and face == 'north':
        col = paint(col, band_h(H, W, 0, 2), (60, 60, 70))
    col = outline(col, H, W, (0, 0, 0), 1, 0.5)
    return col, np.ones((H, W)), None


P.mats['pants'] = pants
P.mats['pants_top'] = lambda c, f, W, H, r: pants(c, f, W, H, r, True)


@P.mat('foot')
def foot(c, face, W, H, r):
    col = base(H, W, SKIN, face, SKIN_D, r, 0.05, 0.4)
    col = outline(col, H, W, (70, 0, 14), 1, 0.7)
    return col, np.ones((H, W)), None


@P.mat('glove')
def glove(c, face, W, H, r):
    col = base(H, W, (30, 30, 34), face, (14, 14, 16), r, 0.04, 0.2)
    yy, xx = grid(H, W)
    if face in ('north', 'west', 'east', 'south'):
        col = paint(col, band_h(H, W, 1.5, 3), (128, 128, 140))
    col = outline(col, H, W, (0, 0, 0), 1, 0.5)
    return col, np.ones((H, W)), None


@P.mat('head')
def head(c, face, W, H, r):
    col = base(H, W, SKIN, face, SKIN_D, r, 0.04, 0.2)
    g = None
    if face == 'north':
        cx = W / 2
        col = paint(col, band_v(H, W, cx - 1.0, cx + 1.0) * (grid(H, W)[0] > H * 0.18), BLACK)
        eyes = 0
        for ex, ey in ((cx - 2.9, H * 0.36), (cx + 2.9, H * 0.36), (cx - 2.5, H * 0.56), (cx + 2.5, H * 0.56)):
            col = paint(col, ellipse(H, W, ex, ey, 1.35, 1.0), BLACK)
            e = ellipse(H, W, ex, ey, 0.95, 0.65)
            col = paint(col, e, (255, 176, 40))
            eyes = np.maximum(eyes, e)
        g = gmask(H, W, eyes, (255, 160, 40), 0.85)
        # brow ridge
        col = paint(col, band_h(H, W, H * 0.2, H * 0.25), SKIN_D)
    if face == 'top':
        col = paint(col, ellipse(H, W, W / 2, H / 2, W * 0.35, H * 0.35) * 0.25, (255, 120, 140))
    col = outline(col, H, W, (70, 0, 14), 1, 0.7)
    return col, np.ones((H, W)), g


@P.mat('jaw')
def jaw(c, face, W, H, r):
    col = base(H, W, SKIN, face, SKIN_D, r, 0.04, 0.3)
    if face == 'north':
        cx = W / 2
        col = paint(col, band_v(H, W, cx - 1.0, cx + 1.0), BLACK)
        col = paint(col, band_h(H, W, 0.6, 1.6) * band_v(H, W, cx - 2.6, cx + 2.6), BLACK)
    col = outline(col, H, W, (70, 0, 14), 1, 0.7)
    return col, np.ones((H, W)), None


# ---------------- animations ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'right_arm2', 'left_arm2', 'right_fore2', 'left_fore2',
     'right_leg', 'left_leg', 'right_shin', 'left_shin', 'base']
UP = {'right_arm': (-165, 0, 15), 'left_arm': (-165, 0, -15), 'right_arm2': (-150, 0, 25), 'left_arm2': (-150, 0, -25),
      'right_fore': (-20, 0, 0), 'left_fore': (-20, 0, 0), 'body': (-12, 0, 0)}
SLAM = {'right_arm': (-55, 0, 8), 'left_arm': (-55, 0, -8), 'right_arm2': (-45, 0, 12), 'left_arm2': (-45, 0, -12),
        'right_fore': (-15, 0, 0), 'left_fore': (-15, 0, 0), 'body': (42, 0, 0), 'head': (-30, 0, 0),
        'right_leg': (-45, 0, 0), 'left_leg': (-45, 0, 0), 'right_shin': (75, 0, 0), 'left_shin': (75, 0, 0)}
a = Anim('slam', 1.2)
pose_seq(a, [(0, {}), (0.4, UP), (0.55, SLAM), (0.9, SLAM), (1.2, {})], B)
a.pos('base', (0, (0, 0, 0)), (0.4, (0, 1, 0)), (0.55, (0, -7, 0)), (0.9, (0, -7, 0)), (1.2, (0, 0, 0)))
A['ab1'] = a
OPEN = {'right_arm': (-90, 60, 0), 'left_arm': (-90, -60, 0), 'right_arm2': (-80, 55, 0), 'left_arm2': (-80, -55, 0), 'body': (-10, 0, 0), 'head': (-10, 0, 0)}
CLAP = {'right_arm': (-90, -12, 0), 'left_arm': (-90, 12, 0), 'right_arm2': (-75, -15, 0), 'left_arm2': (-75, 15, 0), 'body': (15, 0, 0)}
a = Anim('clap', 0.9)
pose_seq(a, [(0, {}), (0.28, OPEN), (0.38, CLAP), (0.65, CLAP), (0.9, {})], B)
A['ab2'] = a
CROUCH = {'body': (30, 0, 0), 'right_leg': (-60, 0, 0), 'left_leg': (-60, 0, 0), 'right_shin': (95, 0, 0), 'left_shin': (95, 0, 0),
          'right_arm': (30, 0, 20), 'left_arm': (30, 0, -20), 'right_arm2': (40, 0, 25), 'left_arm2': (40, 0, -25)}
AIR = {'body': (-10, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (10, 0, 0), 'right_shin': (40, 0, 0), 'left_shin': (60, 0, 0),
       'right_arm': (-170, 0, -5), 'left_arm': (-170, 0, 5), 'right_arm2': (-140, 0, -10), 'left_arm2': (-140, 0, 10)}
a = Anim('leap', 3.0)
pose_seq(a, [(0, {}), (0.2, CROUCH), (0.32, AIR), (3.0, AIR)], B)
a.pos('base', (0, (0, 0, 0)), (0.2, (0, -8, 0)), (0.32, (0, 0, 0)))
A['ab3'] = a
REACH = {'right_arm': (-90, 15, 0), 'left_arm': (-90, -15, 0), 'right_arm2': (-80, 20, 0), 'left_arm2': (-80, -20, 0), 'body': (20, 0, 0)}
HOLD = {'right_arm': (-175, 0, 20), 'left_arm': (-175, 0, -20), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0),
        'right_arm2': (-60, 0, 30), 'left_arm2': (-60, 0, -30), 'body': (-15, 0, 0), 'head': (-10, 0, 0)}
THROW = {'right_arm': (-60, 0, 10), 'left_arm': (-60, 0, -10), 'right_arm2': (-40, 0, 15), 'left_arm2': (-40, 0, -15), 'body': (35, 0, 0),
         'right_leg': (-30, 0, 0), 'left_leg': (25, 0, 0), 'right_shin': (30, 0, 0)}
a = Anim('grab', 2.0)
pose_seq(a, [(0, {}), (0.22, REACH), (0.3, REACH), (0.8, HOLD), (1.35, HOLD), (1.5, {**HOLD, 'body': (-25, 0, 0)}), (1.62, THROW), (1.8, THROW), (2.0, {})], B)
A['ab4'] = a
GUARD = {'right_arm': (-75, -38, 10), 'left_arm': (-75, 38, -10), 'right_fore': (-75, 0, 0), 'left_fore': (-75, 0, 0),
         'right_arm2': (-45, -40, 0), 'left_arm2': (-45, 40, 0), 'right_fore2': (-55, 0, 0), 'left_fore2': (-55, 0, 0), 'body': (12, 0, 0),
         'head': (12, 0, 0)}
a = Anim('block', 1.0, loop=True)
pose_seq(a, [(0, GUARD), (0.5, {**GUARD, 'body': (14, 0, 0)}), (1.0, GUARD)], B)
A['block'] = a
