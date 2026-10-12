"""Goop (Polymorph): translucent green slime body, pointed head with almond eyes, shoulder spikes, drippy hands, and the anti-gravity UFO."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('goop', 256, 256)
hc = box_helpers(M)
FIT = 84

G1 = (126, 222, 48)
G2 = (58, 160, 34)
HL = (196, 255, 130)
ALPHA = 0.86


def X(s, x, w):
    return x if s == -1 else -(x + w)


M.bone('base', None, (0, 48 - 30, 0))
M.bone('body', 'base', (0, 48 - 30, 0))
hc('body', -4.2, 32, -2.8, 8.4, 5, 5.6, 'slime')
hc('body', -3.6, 41, -2.6, 7.2, 9.5, 5.2, 'slime')
hc('body', -5.2, 50, -3.0, 10.4, 9.5, 6.0, 'chest')
for sd in (-1, 1):
    n = f'spike_{"r" if sd < 0 else "l"}'
    M.bone(n, 'body', (sd * 4.6, 48 - 49, 0), (0, 0, sd * 30))
    M.cube(n, (sd * 4.6 - 1.4, 48 - 49 - 7, -1.4), (2.8, 7, 2.8), 'slime')
    M.cube(n, (sd * 4.6 - 0.8, 48 - 49 - 11, -0.8), (1.6, 4, 1.6), 'slime_hl')
M.bone('head', 'body', (0, 48 - 50, 0))
hc('head', -1.6, 52, -1.6, 3.2, 2.5, 3.2, 'slime')
hc('head', -3.6, 60, -3.2, 7.2, 8.5, 6.4, 'head')
M.bone('peak', 'head', (0, 48 - 60, 0.4), (-12, 0, 0))
M.cube('peak', (-2.0, 48 - 60 - 4.5, -1.6), (4.0, 4.5, 4.0), 'slime')
M.cube('peak', (-1.2, 48 - 60 - 8.5, -0.8), (2.4, 4.0, 2.4), 'slime_hl')
M.cube('peak', (-0.6, 48 - 60 - 11, -0.2), (1.2, 2.5, 1.2), 'slime_hl')
M.bone('ufo', 'head', (0, 48 - 76, 0))
M.cube('ufo', (-5.5, 48 - 76, -5.5), (11, 1.6, 11), 'ufo')
M.cube('ufo', (-3.5, 48 - 77.6, -3.5), (7, 1.6, 7), 'ufo_top')
M.cube('ufo', (-2.5, 48 - 74.4, -2.5), (5, 1.0, 5), 'ufo_band')
M.cube('ufo', (-1.0, 48 - 73.4, -1.0), (2, 1.2, 2), 'ufo_light')


def arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 6.0, 48 - 48, 0), (0, 0, -s * 12))
    hc(a, X(s, -7.8, 3.6), 49.5, -1.8, 3.6, 13, 3.6, 'slime')
    f = f'{side}_fore'
    M.bone(f, a, (s * 6.0, 48 - 37, 0))
    hc(f, X(s, -7.6, 3.2), 37.5, -1.6, 3.2, 12, 3.2, 'slime')
    h = f'{side}_hand'
    M.bone(h, f, (s * 6.0, 48 - 26, 0))
    hc(h, X(s, -8.2, 4.4), 26.5, -2.2, 4.4, 3, 4.4, 'slime')
    for k, (fx, fz) in enumerate(((-8.0, -2.0), (-7.4, 0.3), (-5.3, -1.0))):
        hc(h, X(s, fx, 1.3), 23.8, fz, 1.3, 6.0 - k, 1.3, 'slime_hl' if k == 2 else 'slime')


def leg(side):
    s = -1 if side == 'right' else 1
    l = f'{side}_leg'
    M.bone(l, 'base', (s * 2.4, 48 - 30, 0), (0, 0, -s * 13))
    hc(l, X(s, -4.4, 4.0), 30.5, -2.0, 4.0, 14, 4.0, 'slime')
    sh = f'{side}_shin'
    M.bone(sh, l, (s * 2.6, 48 - 17, 0), (0, 0, s * 4))
    hc(sh, X(s, -4.6, 4.2), 17.5, -2.1, 4.2, 10, 4.2, 'slime')
    hc(sh, X(s, -5.6, 6.0), 8.0, -3.0, 6.0, 5, 6.0, 'slime')
    hc(sh, X(s, -6.8, 8.4), 3.4, -4.6, 8.4, 3.4, 8.8, 'slime_foot')


arm('right'); arm('left'); leg('right'); leg('left')

P = Painter(M)


def slime(c, face, W, H, r, tone=0):
    col = base(H, W, G1 if tone != 1 else HL, face, G2, r, 0.06, 0.45)
    yy, xx = grid(H, W)
    # glossy highlight streak + inner darker core
    if face in ('north', 'south', 'west', 'east') and W >= 3:
        col = paint(col, band_v(H, W, W * 0.22, W * 0.22 + max(0.8, W * 0.12)) * 0.75, HL)
        col = paint(col, band_v(H, W, W * 0.55, W * 0.85) * 0.35, G2)
    # bubbles
    for _ in range(max(0, W * H // 60)):
        col = paint(col, ellipse(H, W, r.random() * W, r.random() * H, 0.5, 0.5) * 0.7, HL)
    col = outline(col, H, W, (30, 100, 20), 1, 0.65)
    return col, np.full((H, W), ALPHA), gmask(H, W, np.ones((H, W)), (110, 230, 40), 0.06)


P.mats['slime'] = slime
P.mats['slime_hl'] = lambda c, f, W, H, r: slime(c, f, W, H, r, 1)
P.mats['slime_foot'] = slime


@P.mat('chest')
def chest(c, face, W, H, r):
    col, a, g = slime(c, face, W, H, r)
    if face == 'north':
        col = apply(col, omnitrix_symbol(H, W, W * 0.62, H * 0.4, 1.7, ring=(20, 20, 20), rim=(60, 60, 60), green=(130, 240, 40), dark=(20, 30, 20)))
        a = np.maximum(a, ellipse(H, W, W * 0.62, H * 0.4, 1.8, 1.8))
        g = merge_glow(g, symbol_glow(H, W, W * 0.62, H * 0.4, 1.7))
    return col, a, g


@P.mat('head')
def head(c, face, W, H, r):
    col, a, g = slime(c, face, W, H, r)
    if face == 'north':
        el = poly(H, W, [(0.8, H * 0.28), (W / 2 - 0.5, H * 0.2), (W / 2 - 0.6, H * 0.62), (1.2, H * 0.45)])
        er = poly(H, W, [(W - 0.8, H * 0.28), (W / 2 + 0.5, H * 0.2), (W / 2 + 0.6, H * 0.62), (W - 1.2, H * 0.45)])
        col = paint(col, el + er, (14, 20, 10))
        il = poly(H, W, [(1.4, H * 0.31), (W / 2 - 0.9, H * 0.27), (W / 2 - 1.0, H * 0.53), (1.7, H * 0.43)])
        ir = poly(H, W, [(W - 1.4, H * 0.31), (W / 2 + 0.9, H * 0.27), (W / 2 + 1.0, H * 0.53), (W - 1.7, H * 0.43)])
        col = paint(col, il + ir, (150, 240, 70))
        a = np.maximum(a, np.clip(el + er, 0, 1))
    return col, a, g


def metal(c, face, W, H, r, colr=(110, 112, 124), shade=(60, 62, 72)):
    col = base(H, W, colr, face, shade, r, 0.04, 0.3)
    col = outline(col, H, W, (20, 20, 26), 1, 0.8)
    return col, np.ones((H, W)), None


P.mats['ufo'] = metal
P.mats['ufo_top'] = lambda c, f, W, H, r: metal(c, f, W, H, r, (140, 142, 156), (90, 92, 104))


@P.mat('ufo_band')
def ufo_band(c, face, W, H, r):
    col, a, _ = metal(c, face, W, H, r, (40, 40, 48), (20, 20, 24))
    if face in ('north', 'south', 'west', 'east'):
        yy, xx = grid(H, W)
        col[(xx.astype(int) % 2) == 0] = (250, 220, 60)
        return col, a, gmask(H, W, ((xx.astype(int) % 2) == 0).astype(float), (255, 230, 80), 0.9)
    return col, a, None


@P.mat('ufo_light')
def ufo_light(c, face, W, H, r):
    col = np.zeros((H, W, 3), np.float32) + (255, 250, 150)
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)), (255, 250, 160), 1.0)


# ---------------- animations ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'base']
a = Anim('spit', 0.6)
pose_seq(a, [(0, {}), (0.15, {'body': (-15, 0, 0), 'head': (-25, 0, 0)}), (0.27, {'body': (25, 0, 0), 'head': (20, 0, 0)}), (0.6, {})], B)
a.scl('head', (0, (1, 1, 1)), (0.15, (1.1, 0.85, 1.1)), (0.27, (0.9, 1.2, 0.9)), (0.45, (1, 1, 1)))
A['ab1'] = a
a = Anim('whip', 0.7)
pose_seq(a, [(0, {}), (0.1, {'right_arm': (-140, 0, 20), 'body': (0, -25, 0)}),
             (0.25, {'right_arm': (-90, 0, 0), 'body': (10, 20, 0)}), (0.5, {'right_arm': (-88, 0, 0), 'body': (10, 20, 0)}), (0.7, {})], B)
a.scl('right_fore', (0, (1, 1, 1)), (0.15, (0.9, 1.5, 0.9)), (0.3, (0.6, 5.0, 0.6)), (0.45, (0.6, 5.0, 0.6)), (0.6, (1, 1, 1)))
A['ab2'] = a
a = Anim('engulf', 3.0)
OPEN = {'right_arm': (-100, 30, 0), 'left_arm': (-100, -30, 0), 'body': (20, 0, 0)}
HUG = {'right_arm': (-70, -35, 0), 'left_arm': (-70, 35, 0), 'body': (10, 0, 0)}
pose_seq(a, [(0, {}), (0.2, OPEN), (0.35, HUG), (2.6, HUG), (2.75, {'right_arm': (-40, 0, 40), 'left_arm': (-40, 0, -40), 'body': (-15, 0, 0)}), (3.0, {})], B)
a.scl('body', (0, (1, 1, 1)), (0.35, (1.35, 1.05, 1.6)), (2.6, (1.35, 1.05, 1.6)), (2.75, (0.9, 1.1, 0.9)), (3.0, (1, 1, 1)))
A['ab3'] = a
a = Anim('wave', 1.5)
pose_seq(a, [(0, {}), (0.6, {'right_arm': (0, 0, 80), 'left_arm': (0, 0, -80)}), (0.7, {'right_arm': (0, 0, 100), 'left_arm': (0, 0, -100)}), (1.5, {})], B)
a.scl('base', (0, (1, 1, 1)), (0.55, (2.2, 0.18, 2.2)), (0.62, (2.6, 0.14, 2.6)), (0.8, (0.85, 1.25, 0.85)), (1.1, (1.05, 0.95, 1.05)), (1.5, (1, 1, 1)))
A['ab4'] = a
GU = {'right_arm': (-40, 0, 50), 'left_arm': (-40, 0, -50), 'body': (15, 0, 0)}
a = Anim('block', 1.0, loop=True)
pose_seq(a, [(0, GU), (0.5, {**GU, 'body': (18, 0, 0)}), (1.0, GU)], B)
A['block'] = a
