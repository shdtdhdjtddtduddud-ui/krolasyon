"""Heatblast (Pyronite): maroon lava-rock body with glowing yellow cracks, flame head, pale-yellow hands/feet."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('heatblast', 256, 256)
hc = box_helpers(M)
FIT = 76

ROCK = (150, 24, 26)
ROCK_D = (104, 12, 18)
CRACK = (255, 236, 140)
FLAME = (255, 244, 170)
FLAME_O = (255, 178, 50)

M.bone('base', None, (0, 48 - 27, 0))
M.bone('body', 'base', (0, 48 - 27, 0))
hc('body', -4.5, 30, -2.6, 9, 5, 5.2, 'rock')            # pelvis
hc('body', -4.8, 38, -2.8, 9.6, 8.5, 5.6, 'rock')         # abdomen
hc('body', -6.4, 47, -3.3, 12.8, 9.5, 6.6, 'rock_chest')  # chest

M.bone('head', 'body', (0, 48 - 47, 0))
hc('head', -1.8, 49, -1.8, 3.6, 2.5, 3.6, 'rock')         # neck
hc('head', -3.6, 56.5, -3.8, 7.2, 8, 7.0, 'head')          # skull / face
hc('head', -4.3, 59.0, -3.4, 8.6, 3.2, 7.6, 'flame')         # flame crown base
# flame tongues (thin tapered boxes) fanning upward/back
for i, (x, h, w, lean, yr) in enumerate(((0, 6.5, 3.6, 4, 0), (-2.8, 4.5, 2.8, 28, -25), (2.8, 4.5, 2.8, -28, 25),
                                          (-1.5, 5.5, 2.4, 14, 30), (1.5, 5.5, 2.4, -14, -30))):
    n = f'flame{i}'
    M.bone(n, 'head', (x, 48 - 59, 0.6), (-18, yr, lean))
    M.cube(n, (x - w / 2, 48 - 59 - h, 0.6 - w / 2), (w, h, w), 'flame')
    M.cube(n, (x - w * 0.3, 48 - 59 - h - h * 0.35, 0.6 - w * 0.3), (w * 0.6, h * 0.35, w * 0.6), 'flame_tip')


def arm(side):
    s = -1 if side == 'right' else 1
    X = (lambda x, w: x) if s == -1 else (lambda x, w: -(x + w))
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 7.6, 48 - 45.5, 0))
    hc(a, X(-10.0, 4.6), 47.2, -2.3, 4.6, 11.5, 4.6, 'rock')
    f = f'{side}_fore'
    M.bone(f, a, (s * 7.7, 48 - 35.5, 0))
    hc(f, X(-9.8, 4.2), 35.8, -2.1, 4.2, 9.5, 4.2, 'rock')
    h = f'{side}_hand'
    M.bone(h, f, (s * 7.7, 48 - 26.5, 0))
    hc(h, X(-10.1, 4.8), 26.8, -2.3, 4.8, 4.2, 4.6, 'hand')
    for fz in (-2.1, -0.6, 0.9):
        hc(h, X(-9.6, 1.4), 22.8, fz, 1.4, 3.6, 1.3, 'hand')
    hc(h, X(-7.0, 1.2), 24.0, -1.6, 1.2, 3.0, 1.2, 'hand')


def leg(side):
    s = -1 if side == 'right' else 1
    X = (lambda x, w: x) if s == -1 else (lambda x, w: -(x + w))
    l = f'{side}_leg'
    M.bone(l, 'base', (s * 2.6, 48 - 27, 0))
    hc(l, X(-5.2, 5.0), 27.5, -2.5, 5.0, 13.5, 5.0, 'rock')
    sh = f'{side}_shin'
    M.bone(sh, l, (s * 2.7, 48 - 14, 0))
    hc(sh, X(-4.9, 4.4), 14.2, -2.2, 4.4, 11.5, 4.4, 'rock')
    hc(sh, X(-5.2, 5.0), 3.0, -4.6, 5.0, 3.0, 7.0, 'hand')     # glowing foot


arm('right'); arm('left'); leg('right'); leg('left')

P = Painter(M)


def rock_paint(c, face, W, H, r, chest=False):
    col = base(H, W, ROCK, face, ROCK_D, r, 0.06, 0.25)
    m = voronoi_cracks(H, W, r, cell=6.5, width=0.5)
    col = paint(col, m, CRACK)
    g = gmask(H, W, m, (255, 220, 110), 0.9)
    if chest and face == 'north':
        layers = omnitrix_symbol(H, W, W / 2 + 0.2, H * 0.36, 1.8, ring=(25, 25, 25), rim=(70, 70, 70))
        col = apply(col, layers)
        cut = ellipse(H, W, W / 2 + 0.2, H * 0.36, 1.9, 1.9)
        g[..., 3] *= (1 - cut)
        g = merge_glow(g, symbol_glow(H, W, W / 2 + 0.2, H * 0.36, 1.8))
    col = outline(col, H, W, (60, 6, 8), 1, 0.6)
    return col, np.ones((H, W)), g


P.mats['rock'] = rock_paint
P.mats['rock_chest'] = lambda c, f, W, H, r: rock_paint(c, f, W, H, r, True)


@P.mat('hand')
def hand(c, face, W, H, r):
    col = base(H, W, (255, 246, 190), face, (240, 200, 110), r, 0.08, 0.5)
    col = outline(col, H, W, (200, 120, 40), 1, 0.5)
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)), (255, 230, 150), 0.75)


@P.mat('flame')
def flame(c, face, W, H, r):
    yy, xx = grid(H, W)
    n = noise2(H, W, r, 2)
    t = np.clip(yy / max(H - 1, 1) * 0.9 + (n - 0.5) * 0.5, 0, 1)
    col = lerp(FLAME, FLAME_O, t)
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)) * 0.95, (255, 225, 120), 1.0)


@P.mat('flame_tip')
def flame_tip(c, face, W, H, r):
    col = np.zeros((H, W, 3), np.float32) + (255, 252, 220)
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)), (255, 250, 210), 1.0)


@P.mat('head')
def head(c, face, W, H, r):
    if face != 'north':
        return flame(c, face, W, H, r)
    # dark-red face plate framed by flame, yellow brow/eye glow and jaw lines (reference: angry pyronite mask)
    col = base(H, W, (255, 240, 160), face, (255, 190, 70), r, 0.1)
    yy, xx = grid(H, W)
    mask = poly(H, W, [(0.6, 1.4), (W - 0.6, 1.4), (W - 0.8, H * 0.7), (W / 2, H - 0.2), (0.8, H * 0.7)])
    col = paint(col, mask, (120, 14, 18))
    eyes = poly(H, W, [(1.2, 2.6), (W / 2 - 0.4, 3.6), (W / 2 - 0.6, 4.6), (1.4, 3.8)]) + \
        poly(H, W, [(W - 1.2, 2.6), (W / 2 + 0.4, 3.6), (W / 2 + 0.6, 4.6), (W - 1.4, 3.8)])
    col = paint(col, eyes, (255, 250, 210))
    mouth = poly(H, W, [(2.0, 5.6), (W - 2.0, 5.6), (W / 2, 7.2)])
    col = paint(col, mouth, (255, 220, 120))
    g = gmask(H, W, np.clip(1 - mask + eyes + mouth, 0, 1), (255, 235, 150), 0.9)
    return col, np.ones((H, W)), g


# ---------------- animations (seconds) ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'right_leg', 'left_leg', 'right_shin', 'left_shin']

# 1 fireball: wind-up then hurl with right arm
a = Anim('fireball', 0.6)
pose_seq(a, [(0, {}), (0.18, {'right_arm': (-40, 0, 40), 'right_fore': (-70, 0, 0), 'body': (0, -25, 0)}),
             (0.32, {'right_arm': (-160, 0, -10), 'right_fore': (-10, 0, 0), 'body': (8, 25, 0), 'left_arm': (20, 0, -20)}),
             (0.6, {})], B)
A['ab1'] = a
# 2 flamethrower: both hands forward, held
a = Anim('flamethrower', 2.2)
pose_seq(a, [(0, {}), (0.2, {'right_arm': (-85, 12, 0), 'left_arm': (-85, -12, 0), 'right_fore': (-10, 0, 0), 'left_fore': (-10, 0, 0), 'body': (12, 0, 0), 'right_leg': (-20, 0, 0), 'left_leg': (20, 0, 0)}),
             (2.0, {'right_arm': (-88, 14, 0), 'left_arm': (-88, -14, 0), 'right_fore': (-14, 0, 0), 'left_fore': (-14, 0, 0), 'body': (14, 0, 0), 'right_leg': (-20, 0, 0), 'left_leg': (20, 0, 0)}),
             (2.2, {})], B)
A['ab2'] = a
# 3 supernova: curl up charging, then explode outward
a = Anim('supernova', 1.6)
pose_seq(a, [(0, {}), (0.7, {'body': (35, 0, 0), 'head': (20, 0, 0), 'right_arm': (-40, 0, 30), 'left_arm': (-40, 0, -30), 'right_fore': (-110, 0, 0), 'left_fore': (-110, 0, 0), 'right_leg': (-40, 0, 0), 'left_leg': (-40, 0, 0), 'right_shin': (70, 0, 0), 'left_shin': (70, 0, 0)}),
             (0.85, {'body': (-20, 0, 0), 'head': (-25, 0, 0), 'right_arm': (0, 0, 120), 'left_arm': (0, 0, -120), 'right_leg': (0, 0, 25), 'left_leg': (0, 0, -25)}),
             (1.3, {'body': (-15, 0, 0), 'right_arm': (0, 0, 110), 'left_arm': (0, 0, -110), 'right_leg': (0, 0, 20), 'left_leg': (0, 0, -20)}),
             (1.6, {})], B)
A['ab3'] = a
# 4 fire pillars: punch the ground
a = Anim('pillars', 1.0)
pose_seq(a, [(0, {}), (0.3, {'right_arm': (-170, 0, 10), 'left_arm': (-170, 0, -10), 'body': (-15, 0, 0)}),
             (0.45, {'right_arm': (-60, 0, 10), 'left_arm': (-60, 0, -10), 'body': (45, 0, 0), 'right_leg': (-50, 0, 0), 'right_shin': (60, 0, 0), 'left_leg': (20, 0, 0)}),
             (0.75, {'right_arm': (-60, 0, 10), 'left_arm': (-60, 0, -10), 'body': (40, 0, 0), 'right_leg': (-50, 0, 0), 'right_shin': (60, 0, 0), 'left_leg': (20, 0, 0)}),
             (1.0, {})], B)
A['ab4'] = a
# block: arms crossed in front of the face
a = Anim('block', 1.0, loop=True)
GB = {'right_arm': (-70, -38, 0), 'left_arm': (-70, 38, 0), 'right_fore': (-75, 0, 0), 'left_fore': (-75, 0, 0), 'body': (8, 0, 0), 'head': (10, 0, 0)}
pose_seq(a, [(0, GB), (1.0, GB)], B)
A['block'] = a
