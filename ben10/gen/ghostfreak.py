"""Ghostfreak (Ectonurite): pale grey floating ghost with black crack stripes, long hooked head with one magenta eye,
long clawed arms, body tapering into a wispy tail; a hidden chest eye opens for fear/possession."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('ghostfreak', 256, 256)
hc = box_helpers(M)
FIT = 74

GREY = (196, 196, 198)
GREY_D = (132, 132, 138)
BLK = (14, 14, 16)


def X(s, x, w):
    return x if s == -1 else -(x + w)


M.bone('base', None, (0, 48 - 30, 0))
M.bone('body', 'base', (0, 48 - 30, 0))
hc('body', -4.5, 38, -3, 9, 8, 6, 'skin')
hc('body', -6.5, 51, -3.6, 13, 13, 7.2, 'chest')
M.bone('chest_eye', 'body', (0, 48 - 45, -3.6))
M.cube('chest_eye', (-2.6, 48 - 47.5, -4.4), (5.2, 4, 1), 'chest_eye')
M.bone('head', 'body', (0, 48 - 51, -1))
hc('head', -1.6, 53, -1.6, 3.2, 2.5, 3.2, 'skin')
hc('head', -3.8, 61, -5.6, 7.6, 8.5, 9, 'head')
M.bone('hook', 'head', (0, 48 - 60, 3.0), (38, 0, 0))
M.cube('hook', (-3.0, 48 - 60 - 3.5, 3.0), (6, 5, 7), 'skin')
M.cube('hook', (-2.0, 48 - 60 - 2.5, 9.5), (4, 3.5, 5), 'skin')
M.cube('hook', (-1.0, 48 - 60 - 1.5, 14.0), (2, 2, 4), 'skin')


def arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 7.0, 48 - 49, 0))
    hc(a, X(s, -9.0, 3.2), 50, -1.6, 3.2, 12, 3.2, 'skin')
    f = f'{side}_fore'
    M.bone(f, a, (s * 7.4, 48 - 38.5, 0))
    hc(f, X(s, -8.8, 2.8), 39, -1.4, 2.8, 11.5, 2.8, 'skin')
    h = f'{side}_hand'
    M.bone(h, f, (s * 7.4, 48 - 27.5, 0))
    hc(h, X(s, -9.2, 3.6), 28, -1.8, 3.6, 2.5, 3.6, 'skin')
    for k, (fx, fz) in enumerate(((-9.2, -1.8), (-8.0, -0.5), (-6.8, 0.8), (-6.0, -2.2))):
        cl = f'{side}_claw{k}'
        M.bone(cl, h, (s * 7.4, 48 - 25.5, fz + 0.5), (-15 - k * 5, 0, -s * (k * 6 - 6)))
        M.cube(cl, (X(s, fx, 1.0), 48 - 25.5, fz), (1.0, 8 - k * 0.8, 1.0), 'skin_d')


arm('right'); arm('left')

prev = 'base'
py = 48 - 30
sizes = [(8.5, 6.0), (7.0, 5.0), (5.4, 4.0), (3.6, 3.0), (2.0, 2.0)]
for i, (w, d) in enumerate(sizes):
    n = f'tail{i + 1}'
    M.bone(n, prev, (0, py, 0), (8 if i else 4, 0, 0))
    M.cube(n, (-w / 2, py, -d / 2), (w, 6, d), 'tail')
    prev = n
    py += 6

P = Painter(M)


def skin(c, face, W, H, r, dark=False, cracks=True):
    col = base(H, W, GREY if not dark else GREY_D, face, GREY_D if not dark else (90, 90, 96), r, 0.05, 0.35)
    if cracks and face in ('north', 'south', 'west', 'east') and H > 5:
        x0 = r.random() * W
        pts = [(x0, 0)]
        for k in range(1, 4):
            pts.append((np.clip(pts[-1][0] + (r.random() - 0.5) * W * 0.8, 0, W), H * k / 3))
        col = paint(col, stroke(H, W, pts, 0.9), BLK)
    col = outline(col, H, W, (40, 40, 46), 1, 0.7)
    return col, np.ones((H, W)), None


P.mats['skin'] = skin
P.mats['skin_d'] = lambda c, f, W, H, r: skin(c, f, W, H, r, True, False)


@P.mat('tail')
def tail(c, face, W, H, r):
    col, a, g = skin(c, face, W, H, r)
    return col, np.full((H, W), 0.92), g


@P.mat('chest')
def chest(c, face, W, H, r):
    col, a, _ = skin(c, face, W, H, r)
    g = None
    if face == 'north':
        col = paint(col, stroke(H, W, [(0, H * 0.2), (W * 0.45, H * 0.45), (W * 0.3, H)], 1.2), BLK)
        col = paint(col, stroke(H, W, [(W, H * 0.3), (W * 0.65, H * 0.55)], 1.2), BLK)
        cx, cy = W * 0.68, H * 0.28
        col = apply(col, omnitrix_symbol(H, W, cx, cy, 2.1, ring=(14, 14, 16), rim=(14, 14, 16), green=(240, 240, 240), dark=(14, 14, 16)))
    return col, a, g


@P.mat('chest_eye')
def chest_eye(c, face, W, H, r):
    col = np.zeros((H, W, 3), np.float32) + BLK
    e = ellipse(H, W, W / 2, H / 2, W * 0.38, H * 0.32)
    col = paint(col, e, (230, 60, 200))
    col = paint(col, ellipse(H, W, W / 2, H / 2, 0.6, H * 0.28), BLK)
    return col, np.ones((H, W)), gmask(H, W, e, (255, 60, 220), 1.0)


@P.mat('head')
def head(c, face, W, H, r):
    col, a, _ = skin(c, face, W, H, r, cracks=False)
    g = None
    if face == 'north':
        col = paint(col, stroke(H, W, [(W * 0.15, 0), (W * 0.35, H * 0.4), (W * 0.2, H)], 1.0), BLK)
        col = paint(col, stroke(H, W, [(W * 0.8, H * 0.1), (W * 0.65, H * 0.7)], 1.0), BLK)
        cx, cy = W * 0.62, H * 0.42
        col = paint(col, ellipse(H, W, cx, cy, 1.8, 1.5), BLK)
        e = ellipse(H, W, cx, cy, 1.3, 1.0)
        col = paint(col, e, (230, 60, 200))
        col = paint(col, ellipse(H, W, cx, cy, 0.4, 0.8), BLK)
        g = gmask(H, W, e, (255, 60, 220), 1.0)
    elif face in ('west', 'east'):
        col = paint(col, stroke(H, W, [(0, H * 0.5), (W * 0.6, H * 0.35), (W, H * 0.6)], 1.0), BLK)
    return col, a, g


# ---------------- animations ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'base', 'tail1', 'tail2']
a = Anim('claw', 0.7)
pose_seq(a, [(0, {}), (0.1, {'right_arm': (-150, 0, 30), 'body': (-10, -20, 0)}),
             (0.25, {'right_arm': (-40, 30, -10), 'body': (35, 25, 0), 'tail1': (30, 0, 0), 'tail2': (20, 0, 0)}), (0.7, {})], B)
A['ab1'] = a
SC = {'right_arm': (-110, 0, 50), 'left_arm': (-110, 0, -50), 'body': (-25, 0, 0), 'head': (-15, 0, 0)}
a = Anim('fear', 1.5)
pose_seq(a, [(0, {}), (0.35, SC), (1.2, {**SC, 'body': (-28, 0, 0)}), (1.5, {})], B)
a.scl('body', (0, (1, 1, 1)), (0.4, (1.08, 1.08, 1.08)), (1.2, (1.08, 1.08, 1.08)), (1.5, (1, 1, 1)))
A['ab2'] = a
DV = {'right_arm': (-170, 0, 0), 'left_arm': (-170, 0, 0), 'body': (60, 0, 0), 'head': (20, 0, 0), 'tail1': (40, 0, 0), 'tail2': (30, 0, 0)}
a = Anim('possess', 6.0)
pose_seq(a, [(0, {}), (0.35, DV), (6.0, DV)], B)
A['ab3'] = a
PH = {'right_arm': (-40, 0, 40), 'left_arm': (-40, 0, -40), 'body': (10, 0, 0)}
a = Anim('phase', 5.0)
pose_seq(a, [(0, {}), (0.3, PH), (4.7, PH), (5.0, {})], B)
A['ab4'] = a
GU = {'right_arm': (-90, 0, -40), 'left_arm': (-90, 0, 40), 'right_fore': (-50, 0, 0), 'left_fore': (-50, 0, 0), 'body': (15, 0, 0)}
a = Anim('block', 1.0, loop=True)
pose_seq(a, [(0, GU), (1.0, GU)], B)
A['block'] = a
