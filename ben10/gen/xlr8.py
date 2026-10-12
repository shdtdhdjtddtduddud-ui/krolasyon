"""XLR8 (Kineceleran): hunched black speedster, blue visor face with green eyes, swept-back crest, striped tail, ball-wheel feet."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('xlr8', 256, 256)
hc = box_helpers(M)
FIT = 66

BLK = (24, 26, 32)
BLK_D = (10, 10, 14)
BLUE = (60, 168, 196)
BLUE_D = (30, 112, 140)
GREY = (120, 124, 140)


def X(s, x, w):
    return x if s == -1 else -(x + w)


M.bone('base', None, (0, 48 - 27, 0))
M.bone('body', 'base', (0, 48 - 27, 0), (34, 0, 0))
hc('body', -4.2, 30, -3, 8.4, 5, 6, 'black')
hc('body', -4.6, 39, -3.2, 9.2, 9.5, 6.4, 'abs')
hc('body', -5.6, 48, -3.6, 11.2, 9.5, 7.2, 'chest')
M.bone('head', 'body', (0, 48 - 48, -1.5), (-30, 0, 0))
hc('head', -1.6, 51.5, -1.8, 3.2, 4.5, 3.2, 'black')
hc('head', -3.4, 58.5, -5.6, 6.8, 7.5, 7.2, 'helmet')
hc('head', -3.0, 57.6, -6.4, 6.0, 5.2, 1.2, 'visor')
hc('head', -2.4, 52.8, -6.0, 4.8, 1.8, 3.0, 'blue')
M.bone('crest', 'head', (0, 48 - 58, 1), (-32, 0, 0))
M.cube('crest', (-1.8, 48 - 58 - 4, 1 - 1), (3.6, 4, 9), 'black')
M.cube('crest', (-1.2, 48 - 58 - 3.2, 9.5), (2.4, 3, 5), 'black')
M.cube('crest', (-0.6, 48 - 58 - 2.4, 14), (1.2, 2.2, 4), 'black')


def arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 6.2, 48 - 46.5, -0.5))
    hc(a, X(s, -7.8, 3.6), 47.5, -2.3, 3.6, 4, 4, 'black')
    hc(a, X(s, -7.4, 2.8), 44.5, -1.4, 2.8, 8, 2.8, 'black')
    f = f'{side}_fore'
    M.bone(f, a, (s * 6.0, 48 - 37, -0.5))
    hc(f, X(s, -7.6, 3.2), 37.5, -1.6, 3.2, 8.5, 3.2, 'blue')
    fin = f'{side}_fin'
    M.bone(fin, f, (s * 6.0, 48 - 36, 1.5), (40, 0, 0))
    M.cube(fin, (s * 6.0 - 0.5, 48 - 36 - 1, 1.5), (1, 2, 6), 'black')
    M.cube(fin, (s * 6.0 - 0.4, 48 - 36 - 0.6, 7.5), (0.8, 1.2, 3), 'black')
    h = f'{side}_hand'
    M.bone(h, f, (s * 6.0, 48 - 29, -0.5))
    hc(h, X(s, -7.5, 3.0), 29.5, -1.5, 3.0, 2.5, 3.0, 'blue')
    for fz in (-1.6, 0.4):
        hc(h, X(s, -7.2, 1.2), 27.2, fz, 1.2, 3.6, 1.2, 'claw')
    hc(h, X(s, -5.0, 1.0), 27.6, -1.4, 1.0, 3.0, 1.0, 'claw')


def leg(side):
    s = -1 if side == 'right' else 1
    l = f'{side}_leg'
    M.bone(l, 'base', (s * 2.6, 48 - 27, 0), (-28, 0, 0))
    hc(l, X(s, -4.4, 3.8), 28, -1.9, 3.8, 13, 3.8, 'black')
    sh = f'{side}_shin'
    M.bone(sh, l, (s * 2.6, 48 - 16, 0), (52, 0, 0))
    hc(sh, X(s, -4.1, 3.2), 16.5, -1.6, 3.2, 11, 3.2, 'black')
    ft = f'{side}_foot'
    M.bone(ft, sh, (s * 2.6, 48 - 6, 0), (-24, 0, 0))
    hc(ft, X(s, -4.3, 3.6), 6.5, -2.8, 3.6, 3.5, 4.2, 'blue')
    hc(ft, X(s, -4.6, 4.2), 3.6, -3.4, 4.2, 4.2, 4.2, 'wheel')


arm('right'); arm('left'); leg('right'); leg('left')

# striped tail: chain of tapering segments from the lower back
prev = 'base'
pz = 3.0
py = 48 - 28
rots = [(60, 0, 0), (12, 0, 0), (10, 0, 0), (10, 0, 0), (14, 0, 0), (18, 0, 0)]
sizes = [4.2, 3.8, 3.4, 3.0, 2.5, 1.8]
L = 6.5
for i in range(6):
    n = f'tail{i + 1}'
    M.bone(n, prev, (0, py, pz), rots[i])
    s = sizes[i]
    M.cube(n, (-s / 2, py, pz - s / 2), (s, L, s), 'tail_a' if i % 2 == 0 else 'tail_b')
    prev = n
    py += L
M.cube('tail6', (-0.6, py, pz - 0.6), (1.2, 3.5, 1.2), 'black')

P = Painter(M)


def flat_mat(col, shade, ink=(0, 0, 0)):
    def fn(c, face, W, H, r):
        cc = base(H, W, col, face, shade, r, 0.04, 0.25)
        cc = outline(cc, H, W, ink, 1, 0.7)
        return cc, np.ones((H, W)), None
    return fn


P.mats['black'] = flat_mat(BLK, BLK_D, (70, 74, 90))
P.mats['blue'] = flat_mat(BLUE, BLUE_D, (10, 50, 70))
P.mats['claw'] = flat_mat((40, 42, 50), (12, 12, 16), (90, 90, 110))
P.mats['wheel'] = flat_mat((70, 72, 86), (30, 30, 40), (10, 10, 12))


def tail(c, face, W, H, r, blue):
    cc = base(H, W, BLUE if blue else BLK, face, BLUE_D if blue else BLK_D, r, 0.04, 0.2)
    if face not in ('top', 'bottom'):
        n = 2
        for k in range(n):
            y0 = H * (k + 0.5) / n
            m = band_h(H, W, y0 - 0.9, y0 + 0.9)
            cc = paint(cc, m, BLK if blue else BLUE)
    cc = outline(cc, H, W, (10, 30, 40), 1, 0.5)
    return cc, np.ones((H, W)), None


P.mats['tail_a'] = lambda c, f, W, H, r: tail(c, f, W, H, r, False)
P.mats['tail_b'] = lambda c, f, W, H, r: tail(c, f, W, H, r, True)


@P.mat('chest')
def chest(c, face, W, H, r):
    cc = base(H, W, BLK, face, BLK_D, r, 0.04, 0.25)
    if face == 'north':
        cc = paint(cc, poly(H, W, [(W * 0.32, 0), (W * 0.68, 0), (W * 0.6, H), (W * 0.4, H)]), (232, 232, 236))
        cc = apply(cc, omnitrix_symbol(H, W, W * 0.5, H * 0.55, 2.1, ring=(40, 40, 44), rim=(140, 140, 150), green=(80, 80, 88), dark=(220, 220, 224)))
    if face in ('west', 'east'):
        cc = paint(cc, band_h(H, W, 1, 3), BLUE)
    cc = outline(cc, H, W, (70, 74, 90), 1, 0.7)
    return cc, np.ones((H, W)), None


@P.mat('abs')
def abs_(c, face, W, H, r):
    cc = base(H, W, BLK, face, BLK_D, r, 0.04, 0.25)
    if face == 'north':
        cc = paint(cc, poly(H, W, [(W * 0.4, 0), (W * 0.6, 0), (W * 0.55, H), (W * 0.45, H)]), (232, 232, 236))
    cc = outline(cc, H, W, (70, 74, 90), 1, 0.7)
    return cc, np.ones((H, W)), None


@P.mat('helmet')
def helmet(c, face, W, H, r):
    cc = base(H, W, BLK, face, BLK_D, r, 0.04, 0.2)
    if face in ('west', 'east'):
        cc = paint(cc, stroke(H, W, [(0, H * 0.75), (W, H * 0.45)], 1.0), BLUE)
    cc = outline(cc, H, W, (70, 74, 90), 1, 0.7)
    return cc, np.ones((H, W)), None


@P.mat('visor')
def visor(c, face, W, H, r):
    cc = base(H, W, BLUE, face, BLUE_D, r, 0.03, 0.2)
    g = None
    if face == 'north':
        yy, xx = grid(H, W)
        # visor glass band across the eyes, green eyes behind it
        cc = paint(cc, band_h(H, W, 0.8, H * 0.55), (30, 50, 60))
        e = ellipse(H, W, W * 0.3, H * 0.33, 0.9, 0.7) + ellipse(H, W, W * 0.7, H * 0.33, 0.9, 0.7)
        cc = paint(cc, e, (170, 255, 40))
        g = gmask(H, W, np.clip(e, 0, 1), (170, 255, 60), 1.0)
        g = merge_glow(g, gmask(H, W, band_h(H, W, 0.8, H * 0.55), (60, 200, 255), 0.25))
        cc = paint(cc, stroke(H, W, [(W * 0.5, H * 0.6), (W * 0.5, H)], 0.7), BLUE_D)
    cc = outline(cc, H, W, (10, 40, 60), 1, 0.6)
    return cc, np.ones((H, W)), g


# ---------------- animations ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'base',
     'tail1', 'tail2', 'tail3']
RUN = {'body': (35, 0, 0), 'head': (-30, 0, 0), 'right_arm': (50, 0, 15), 'left_arm': (50, 0, -15), 'right_leg': (-40, 0, 0),
       'left_leg': (35, 0, 0), 'right_shin': (30, 0, 0), 'left_shin': (60, 0, 0), 'tail1': (25, 0, 0), 'tail2': (10, 0, 0)}
a = Anim('dash', 0.4)
pose_seq(a, [(0, {}), (0.06, RUN), (0.32, RUN), (0.4, {})], B)
A['ab1'] = a
a = Anim('cyclone', 3.0)
SP = {'body': (30, 0, 25), 'right_arm': (40, 0, 60), 'left_arm': (40, 0, -60), 'right_leg': (-50, 0, 0), 'left_leg': (40, 0, 0), 'tail1': (20, 30, 0)}
SP2 = {'body': (30, 0, 25), 'right_arm': (40, 0, 60), 'left_arm': (40, 0, -60), 'right_leg': (40, 0, 0), 'left_leg': (-50, 0, 0), 'tail1': (20, 30, 0)}
frames = [(0, {})]
t = 0.1
k = 0
while t < 2.85:
    frames.append((t, SP if k % 2 == 0 else SP2))
    t += 0.1
    k += 1
frames.append((3.0, {}))
pose_seq(a, frames, B, interp='L')
A['ab2'] = a
a = Anim('blitz', 1.8)
SL1 = {'right_arm': (-100, 30, 0), 'right_fore': (-20, 0, 0), 'body': (25, 30, 0), 'left_arm': (30, 0, -20), 'right_leg': (-30, 0, 0)}
SL2 = {'left_arm': (-100, -30, 0), 'left_fore': (-20, 0, 0), 'body': (25, -30, 0), 'right_arm': (30, 0, 20), 'left_leg': (-30, 0, 0)}
frames = [(0, {})]
for i in range(7):
    frames.append((0.12 + i * 0.25, SL1 if i % 2 == 0 else SL2))
frames.append((1.8, {}))
pose_seq(a, frames, B)
A['ab3'] = a
a = Anim('overdrive', 6.0)
OD = {'body': (25, 0, 0), 'head': (-20, 0, 0), 'right_arm': (35, 0, 12), 'left_arm': (35, 0, -12), 'tail1': (15, 0, 0)}
pose_seq(a, [(0, {}), (0.3, {**OD, 'body': (-15, 0, 0), 'head': (-40, 0, 0), 'right_arm': (-30, 0, 40), 'left_arm': (-30, 0, -40)}),
             (0.6, OD), (5.8, OD), (6.0, {})], B)
A['ab4'] = a
G = {'body': (25, 0, 0), 'right_arm': (-60, -35, 0), 'left_arm': (-60, 35, 0), 'right_fore': (-70, 0, 0), 'left_fore': (-70, 0, 0),
     'right_leg': (-30, 0, 0), 'right_shin': (20, 0, 0), 'left_leg': (25, 0, 0)}
a = Anim('block', 0.4, loop=True)
pose_seq(a, [(0, G), (0.2, {**G, 'base': (0, 0, 0)}), (0.4, G)], B)
a.pos('base', (0, (-1.5, 0, 0)), (0.1, (1.5, 0, 0)), (0.2, (-1.5, 0, 0)), (0.3, (1.5, 0, 0)), (0.4, (-1.5, 0, 0)), interp='L')
A['block'] = a
