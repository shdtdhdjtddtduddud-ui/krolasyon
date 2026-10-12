"""Ultimate Echo Echo (Sonorosian): blue armoured body covered in glowing cyan sonic discs, oval head with green eyes,
long arms with dark three-fingered hands, thin thighs into big tapered lower legs, Ultimatrix symbol on the chest."""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('echoecho', 512, 256)
hc = box_helpers(M)
FIT = 82

BLUE = (46, 128, 222)
BLUE_D = (22, 74, 150)
CYAN = (40, 250, 255)
DARK = (34, 38, 56)


def X(s, x, w):
    return x if s == -1 else -(x + w)


M.bone('base', None, (0, 48 - 36, 0))
M.bone('body', 'base', (0, 48 - 36, 0))
hc('body', -3.2, 45, -2.4, 6.4, 9, 4.8, 'waist')
hc('body', -9, 58, -6, 18, 14, 12, 'torso')
hc('body', -10, 56, -5, 20, 10, 10, 'torso_side')
hc('body', -7, 60, -4.5, 14, 2, 9, 'blue')
# body discs (protruding): two on the chest sides, one under the symbol
for nm, (x, h, z, rx) in {'disc_c1': (-6.6, 54.5, -6.6, 0), 'disc_c2': (6.6 - 4, 54.5, -6.6, 0)}.items():
    hc('body', x if nm == 'disc_c1' else 2.6, h, z, 4, 4, 1, 'disc')
M.bone('head', 'body', (0, 48 - 59, 0))
hc('head', -1.5, 61.5, -1.5, 3, 2.5, 3, 'dark')
hc('head', -7, 70, -5.5, 14, 8.5, 11, 'head')
hc('head', -5.5, 71.5, -4.5, 11, 1.5, 9, 'blue')
hc('head', -4, 69.5, -6.4, 3.4, 3.4, 1, 'disc')
hc('head', 0.6, 69.5, -6.4, 3.4, 3.4, 1, 'disc')
hc('head', -7.8, 68, -2, 1, 4, 4, 'disc')
hc('head', 6.8, 68, -2, 1, 4, 4, 'disc')


def arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 11.5, 48 - 55, 0), (0, 0, -s * 6))
    hc(a, X(s, -15.5, 8), 59, -4, 8, 8, 8, 'blue')
    d = f'{side}_disc'
    M.bone(d, a, (s * 15.8, 48 - 55, 0))
    M.cube(d, (X(s, -16.5, 1.2), 48 - 58, -3), (1.2, 6, 6), 'disc')
    hc(a, X(s, -13.4, 3.4), 51.5, -1.7, 3.4, 12, 3.4, 'blue_stripe')
    f = f'{side}_fore'
    M.bone(f, a, (s * 11.7, 48 - 40, 0))
    hc(f, X(s, -14.4, 5.4), 40, -2.7, 5.4, 14, 5.4, 'blue_stripe')
    hc(f, X(s, -15.3, 1.0), 34, -2.2, 1.0, 4.4, 4.4, 'disc')
    h = f'{side}_hand'
    M.bone(h, f, (s * 11.7, 48 - 26, 0))
    hc(h, X(s, -13.8, 4.2), 26.5, -1.8, 4.2, 3.5, 3.6, 'dark')
    for fz in (-1.7, 0.3):
        hc(h, X(s, -13.6, 1.3), 23.2, fz, 1.3, 5, 1.3, 'dark')
    hc(h, X(s, -10.6, 1.2), 24.2, -1.6, 1.2, 4, 1.2, 'dark')


def leg(side):
    s = -1 if side == 'right' else 1
    l = f'{side}_leg'
    M.bone(l, 'base', (s * 3.8, 48 - 36, 0))
    hc(l, X(s, -5.6, 3.6), 36.5, -1.8, 3.6, 9, 3.6, 'blue_stripe')
    sh = f'{side}_shin'
    M.bone(sh, l, (s * 3.8, 48 - 28, 0))
    hc(sh, X(s, -6.8, 6), 28.5, -3, 6, 13, 6, 'leg_upper')
    hc(sh, X(s, -8.0, 8.4), 16, -4.2, 8.4, 16, 8.4, 'leg_lower')
    hc(sh, X(s, -6.4, 4.8), 26, -3.6, 4.8, 4.8, 1, 'disc')
    hc(sh, X(s, -6.2, 4.4), 11, -4.9, 4.4, 4.4, 1, 'disc')
    hc(sh, X(s, -7.2, 6.4), 21.5, -5.0, 6.4, 6.4, 1, 'disc')


arm('right'); arm('left'); leg('right'); leg('left')

# guard: ring of discs spinning in front
M.bone('disc_wall', 'body', (0, 48 - 50, -13))
for i in range(10):
    a = i * math.tau / 10
    x, y = math.cos(a) * 13, math.sin(a) * 13
    M.cube('disc_wall', (x - 3, 48 - 50 + y - 3, -14), (6, 6, 1.6), 'disc_big')

P = Painter(M)


def blue(c, face, W, H, r, stripe=False):
    col = base(H, W, BLUE, face, BLUE_D, r, 0.04, 0.3)
    yy, xx = grid(H, W)
    if face in ('north', 'south', 'west', 'east') and W > 2:
        col = paint(col, band_v(H, W, W * 0.15, W * 0.15 + 0.9) * 0.6, (130, 190, 255))
        if stripe:
            col = paint(col, band_v(H, W, W * 0.55, W * 0.55 + max(0.8, W * 0.18)), (14, 16, 26))
    col = outline(col, H, W, (6, 20, 50), 1, 0.8)
    return col, np.ones((H, W)), None


P.mats['blue'] = blue
P.mats['blue_stripe'] = lambda c, f, W, H, r: blue(c, f, W, H, r, True)
P.mats['torso_side'] = blue


@P.mat('waist')
def waist(c, face, W, H, r):
    col, a, _ = blue(c, face, W, H, r)
    if face in ('north', 'south'):
        col = paint(col, band_v(H, W, W * 0.3, W * 0.42), (14, 16, 26))
        col = paint(col, band_v(H, W, W * 0.6, W * 0.72), (14, 16, 26))
    return col, a, None


def disc_paint(H, W, col, cx, cy, rx, ry):
    col = paint(col, ellipse(H, W, cx, cy, rx + 1.1, ry + 1.1), DARK)
    col = paint(col, ellipse(H, W, cx, cy, rx + 0.5, ry + 0.5), (14, 16, 26))
    col = paint(col, ellipse(H, W, cx, cy, rx, ry), CYAN)
    col = paint(col, ellipse(H, W, cx - rx * 0.25, cy - ry * 0.25, rx * 0.35, ry * 0.35), (210, 255, 255))
    return col, gmask(H, W, ellipse(H, W, cx, cy, rx, ry), (40, 240, 255), 1.0)


@P.mat('disc')
def disc(c, face, W, H, r):
    col = np.zeros((H, W, 3), np.float32) + DARK
    if W >= 3 and H >= 3:
        col, g = disc_paint(H, W, col, W / 2, H / 2, W / 2 - 1.1, H / 2 - 1.1)
        return col, np.ones((H, W)), g
    return col, np.ones((H, W)), None


@P.mat('disc_big')
def disc_big(c, face, W, H, r):
    col, a, g = disc(c, face, W, H, r)
    return col, a, g


@P.mat('torso')
def torso(c, face, W, H, r):
    col, a, _ = blue(c, face, W, H, r)
    g = None
    if face == 'north':
        col = apply(col, omnitrix_symbol(H, W, W * 0.32, H * 0.3, 2.4, ring=(30, 30, 34), rim=(110, 110, 118), ultimate=True))
        g = symbol_glow(H, W, W * 0.32, H * 0.3, 2.4)
        col, g2 = disc_paint(H, W, col, W * 0.68, H * 0.62, 2.0, 1.6)
        g = merge_glow(g, g2)
        col = paint(col, stroke(H, W, [(1, H * 0.55), (W * 0.5, H * 0.75), (W - 1, H * 0.55)], 0.7), BLUE_D)
    if face in ('west', 'east'):
        col, g = disc_paint(H, W, col, W / 2, H / 2, W * 0.28, H * 0.26)
    return col, a, g


@P.mat('head')
def head(c, face, W, H, r):
    col, a, _ = blue(c, face, W, H, r)
    g = None
    if face == 'north':
        el = ellipse(H, W, W * 0.3, H * 0.58, 1.7, 1.2)
        er = ellipse(H, W, W * 0.7, H * 0.58, 1.7, 1.2)
        col = paint(col, ellipse(H, W, W * 0.3, H * 0.58, 2.3, 1.7) + ellipse(H, W, W * 0.7, H * 0.58, 2.3, 1.7), (14, 16, 26))
        col = paint(col, el + er, (150, 220, 60))
        col = paint(col, band_h(H, W, H * 0.86, H * 0.86 + 0.7) * band_v(H, W, W * 0.3, W * 0.7), (14, 16, 26))
        g = gmask(H, W, np.clip(el + er, 0, 1), (150, 230, 60), 0.85)
    if face == 'top':
        col, g = disc_paint(H, W, col, W / 2, H / 2, W * 0.22, H * 0.22)
    return col, a, g


def leg_paint(c, face, W, H, r, lower):
    col, a, _ = blue(c, face, W, H, r)
    if face in ('west', 'east', 'south'):
        for k in range(2 if lower else 1):
            y0 = H * (0.3 + k * 0.4)
            col = paint(col, band_h(H, W, y0, y0 + 1.6), (14, 16, 26))
    return col, a, None


P.mats['leg_upper'] = lambda c, f, W, H, r: leg_paint(c, f, W, H, r, False)
P.mats['leg_lower'] = lambda c, f, W, H, r: leg_paint(c, f, W, H, r, True)
P.mats['dark'] = lambda c, f, W, H, r: (outline(base(H, W, DARK, f, (14, 16, 26), r, 0.04), H, W, (0, 0, 0), 1, 0.6), np.ones((H, W)), None)

# ---------------- animations ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'base']
a = Anim('discs', 0.7)
pose_seq(a, [(0, {}), (0.15, {'right_arm': (-30, 0, 70), 'right_fore': (-40, 0, 0), 'body': (0, -25, 0)}),
             (0.27, {'right_arm': (-95, -30, 0), 'right_fore': (0, 0, 0), 'body': (0, 25, 0)}), (0.7, {})], B)
A['ab1'] = a
SC = {'right_arm': (-90, 20, 0), 'left_arm': (-90, -20, 0), 'right_fore': (-15, 0, 0), 'left_fore': (-15, 0, 0), 'body': (-8, 0, 0), 'head': (-15, 0, 0),
      'right_leg': (-15, 0, 8), 'left_leg': (15, 0, -8)}
a = Anim('beam', 2.5)
pose_seq(a, [(0, {}), (0.3, SC), (2.3, {**SC, 'body': (-10, 0, 0)}), (2.5, {})], B)
A['ab2'] = a
OP = {'right_arm': (-20, 0, 110), 'left_arm': (-20, 0, -110), 'body': (-12, 0, 0), 'head': (-20, 0, 0)}
a = Anim('barrage', 1.5)
pose_seq(a, [(0, {}), (0.2, OP), (1.3, OP), (1.5, {})], B)
A['ab3'] = a
CH = {'right_arm': (-160, 0, -20), 'left_arm': (-160, 0, 20), 'body': (-15, 0, 0), 'head': (-30, 0, 0)}
DN = {'right_arm': (-80, 25, 0), 'left_arm': (-80, -25, 0), 'body': (20, 0, 0)}
a = Anim('doom', 2.5)
pose_seq(a, [(0, {}), (0.3, CH), (1.6, CH), (1.72, DN), (2.2, DN), (2.5, {})], B)
A['ab4'] = a
GU = {'right_arm': (-90, 0, -20), 'left_arm': (-90, 0, 20), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0)}
a = Anim('block', 1.0, loop=True)
pose_seq(a, [(0, GU), (1.0, GU)], B)
A['block'] = a
