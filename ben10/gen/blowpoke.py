"""Blowpoke: huge olive shell with five dark ports, pale green flipper arms with black spots, small peeking head.
Two rigs in one model: 'stand' (walking) and 'ball' (curled up for rolling/guard)."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('blowpoke', 512, 256)
hc = box_helpers(M)
FIT = 70

SHELL = (152, 146, 58)
SHELL_D = (96, 92, 30)
SHELL_DD = (62, 60, 20)
SKIN = (160, 186, 98)
SKIN_D = (110, 136, 62)


def X(s, x, w):
    return x if s == -1 else -(x + w)


M.bone('stand', None, (0, 48, 0))
M.bone('base', 'stand', (0, 48 - 22, 0))
M.bone('body', 'base', (0, 48 - 22, 0))
hc('body', -12.5, 52, -8.5, 25, 26, 17, 'shell_core')
hc('body', -11, 55, -11.5, 22, 31, 3, 'shell_front')
hc('body', -11, 55, 8.5, 22, 31, 3, 'shell_back')
hc('body', -10, 58, -8, 20, 3.5, 17, 'shell_rim')
hc('body', -10, 25.5, -8, 20, 3.5, 17, 'shell_rim')
hc('body', -14, 49, -6.5, 28, 20, 13, 'shell_side')
M.bone('head', 'body', (0, 48 - 52, -9))
hc('head', -4, 57.5, -12.5, 8, 6.5, 5, 'head')
hc('head', -3, 59, -11.5, 6, 1.5, 4, 'shell_rim')


def arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 13.5, 48 - 50, 0), (0, 0, -s * 22))
    hc(a, X(s, -20.0, 9), 54, -4.5, 9, 13, 9, 'skin')
    f = f'{side}_fore'
    M.bone(f, a, (s * 14.5, 48 - 41.5, 0), (0, 0, s * 8))
    hc(f, X(s, -22.0, 11), 42, -3.0, 11, 25, 6, 'flipper')
    hc(f, X(s, -21.0, 8), 18, -2.5, 8, 7, 5, 'flipper_tip')
    M.bone(f'{side}_hand', f, (s * 14.5, 48 - 19, 0))


def leg(side):
    s = -1 if side == 'right' else 1
    l = f'{side}_leg'
    M.bone(l, 'base', (s * 5.5, 48 - 22, 0))
    hc(l, X(s, -9.5, 8), 23, -4, 8, 12, 8, 'leg')
    sh = f'{side}_shin'
    M.bone(sh, l, (s * 5.5, 48 - 11, 0))
    hc(sh, X(s, -9.8, 8.6), 11.5, -4.3, 8.6, 11.5, 8.6, 'leg')


arm('right'); arm('left'); leg('right'); leg('left')

# curled-up ball (rolling / guard)
M.bone('ball', None, (0, 48 - 20, 0))
M.cube('ball', (-15, 48 - 20 - 12, -15), (30, 24, 30), 'ball')
M.cube('ball', (-12, 48 - 20 - 15, -12), (24, 30, 24), 'ball')
M.cube('ball', (-13.5, 48 - 20 - 13.5, -13.5), (27, 27, 27), 'ball_band')

P = Painter(M)


def shell(c, face, W, H, r, kind):
    col = base(H, W, SHELL if kind != 'core' else SHELL_D, face, SHELL_DD, r, 0.05, 0.3)
    yy, xx = grid(H, W)
    g = None
    if kind == 'front' and face == 'north':
        # dark ports arranged around the central Omnitrix (reference: 5 big holes + 2 small)
        cx, cy = W / 2, H * 0.45
        ports = [(cx - W * 0.27, cy - H * 0.22, 3.6, 3.0), (cx + W * 0.27, cy - H * 0.22, 3.6, 3.0),
                 (cx - W * 0.3, cy + H * 0.12, 3.2, 3.6), (cx + W * 0.3, cy + H * 0.12, 3.2, 3.6),
                 (cx - W * 0.13, cy + H * 0.36, 2.4, 3.2), (cx + W * 0.13, cy + H * 0.36, 2.4, 3.2), (cx, cy - H * 0.36, 3.6, 2.2)]
        for px, py, rx, ry in ports:
            col = paint(col, ellipse(H, W, px, py, rx + 0.7, ry + 0.7), SHELL_DD)
            col = paint(col, ellipse(H, W, px, py, rx, ry), (16, 16, 12))
        col = paint(col, ellipse(H, W, cx, cy, 5.2, 5.2), SHELL_D)
        col = apply(col, omnitrix_symbol(H, W, cx, cy, 3.4, ring=(30, 30, 30), rim=(130, 130, 136)))
        g = symbol_glow(H, W, cx, cy, 3.4)
    elif kind in ('back', 'side', 'rim', 'core'):
        # armour plate seams
        for k in range(1, 4):
            col = paint(col, band_h(H, W, H * k / 4 - 0.5, H * k / 4 + 0.5) * 0.8, SHELL_DD)
        if W > 8:
            col = paint(col, band_v(H, W, W / 2 - 0.5, W / 2 + 0.5) * 0.8, SHELL_DD)
    col = outline(col, H, W, (30, 30, 8), 1, 0.85)
    return col, np.ones((H, W)), g


for k in ('front', 'back', 'side', 'rim', 'core'):
    P.mats['shell_' + k] = (lambda k: lambda c, f, W, H, r: shell(c, f, W, H, r, k))(k)


def skin(c, face, W, H, r, spots=False, tip=False):
    col = base(H, W, SKIN, face, SKIN_D, r, 0.05, 0.3)
    if spots and face in ('north', 'south'):
        col = paint(col, band_v(H, W, 0, W * 0.22), SHELL_D)
        for k in range(3):
            col = paint(col, stroke(H, W, [(W * 0.3, H * (0.2 + k * 0.25)), (W * 0.8, H * (0.25 + k * 0.25))], 0.6) * 0.6, SKIN_D)
    if tip and face in ('north', 'south'):
        for k in range(3):
            col = paint(col, ellipse(H, W, W * (0.25 + k * 0.25), H * 0.5, W * 0.09, H * 0.28), (16, 16, 12))
    col = outline(col, H, W, (50, 70, 20), 1, 0.7)
    return col, np.ones((H, W)), None


P.mats['skin'] = skin
P.mats['leg'] = skin
P.mats['flipper'] = lambda c, f, W, H, r: skin(c, f, W, H, r, spots=True)
P.mats['flipper_tip'] = lambda c, f, W, H, r: skin(c, f, W, H, r, tip=True)


@P.mat('head')
def head(c, face, W, H, r):
    col = base(H, W, SKIN, face, SKIN_D, r, 0.04, 0.3)
    g = None
    if face == 'north':
        col = paint(col, band_h(H, W, 0, H * 0.35), SHELL)
        e = ellipse(H, W, W * 0.72, H * 0.52, 1.3, 1.0)
        col = paint(col, ellipse(H, W, W * 0.72, H * 0.52, 1.8, 1.4), (20, 30, 10))
        col = paint(col, e, (190, 255, 60))
        col = paint(col, stroke(H, W, [(W * 0.15, H * 0.8), (W * 0.5, H * 0.95), (W * 0.85, H * 0.8)], 0.7), (40, 60, 20))
        g = gmask(H, W, e, (190, 255, 60), 0.9)
    col = outline(col, H, W, (40, 60, 20), 1, 0.7)
    return col, np.ones((H, W)), g


def ball(c, face, W, H, r, band=False):
    col = base(H, W, SHELL, face, SHELL_D, r, 0.05, 0.2)
    yy, xx = grid(H, W)
    for k in range(1, 3):
        col = paint(col, band_h(H, W, H * k / 3 - 0.6, H * k / 3 + 0.6), SHELL_DD)
        col = paint(col, band_v(H, W, W * k / 3 - 0.6, W * k / 3 + 0.6), SHELL_DD)
    if not band:
        col = paint(col, ellipse(H, W, W / 2, H / 2, W * 0.12, H * 0.12), (16, 16, 12))
    col = outline(col, H, W, (30, 30, 8), 1, 0.85)
    return col, np.ones((H, W)), None


P.mats['ball'] = ball
P.mats['ball_band'] = lambda c, f, W, H, r: ball(c, f, W, H, r, True)

# ---------------- animations ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'base']
# rolling / launch / bounce / ricochet are all shown with the ball (spun procedurally); the stand clips are short wind-ups
a = Anim('roll', 5.0)
pose_seq(a, [(0, {}), (5.0, {})], B)
a.scl('ball', (0, (0.7, 0.7, 0.7)), (0.15, (1.08, 1.08, 1.08)), (0.25, (1, 1, 1)), (5.0, (1, 1, 1)))
A['ab1'] = a
a = Anim('launch', 1.5)
pose_seq(a, [(0, {}), (1.5, {})], B)
a.scl('ball', (0, (0.7, 0.7, 0.7)), (0.25, (0.85, 1.2, 0.85)), (0.32, (1.15, 0.85, 1.15)), (0.45, (1, 1, 1)), (1.5, (1, 1, 1)))
A['ab2'] = a
a = Anim('bounce', 3.0)
pose_seq(a, [(0, {}), (3.0, {})], B)
a.scl('ball', (0, (0.7, 0.7, 0.7)), (0.18, (1.25, 0.75, 1.25)), (0.3, (0.9, 1.15, 0.9)), (0.45, (1, 1, 1)), (3.0, (1, 1, 1)))
A['ab3'] = a
a = Anim('ricochet', 2.5)
pose_seq(a, [(0, {}), (2.5, {})], B)
a.scl('ball', (0, (0.7, 0.7, 0.7)), (0.2, (1, 1, 1)), (2.5, (1, 1, 1)))
A['ab4'] = a
a = Anim('block', 1.0, loop=True)
pose_seq(a, [(0, {}), (1.0, {})], B)
a.scl('ball', (0, (1.0, 1.0, 1.0)), (0.5, (1.03, 0.98, 1.03)), (1.0, (1.0, 1.0, 1.0)))
A['block'] = a
