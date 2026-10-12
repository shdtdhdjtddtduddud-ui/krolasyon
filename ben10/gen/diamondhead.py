"""Diamondhead (Petrosapien): mint-green crystal head and huge faceted arms, half black / half white suit, Omnitrix on the chest."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('diamondhead', 512, 256)
hc = box_helpers(M)
FIT = 84

CRY = (196, 252, 220)
CRY_D = (120, 214, 170)
CRY_DD = (60, 140, 104)


def X(s, x, w):
    return x if s == -1 else -(x + w)


M.bone('base', None, (0, 48 - 34, 0))
M.bone('body', 'base', (0, 48 - 34, 0))
hc('body', -6.5, 35, -4, 13, 6, 8, 'pelvis')
hc('body', -7, 44, -4.25, 14, 9.5, 8.5, 'abs')
hc('body', -10, 56, -5, 20, 12.5, 10, 'chest')
M.bone('head', 'body', (0, 48 - 56, 0))
hc('head', -2.5, 58, -2.5, 5, 3, 5, 'crystal_d')
hc('head', -4.5, 69, -4.8, 9, 11.5, 9, 'head')
hc('head', -3.5, 62.5, -6.2, 7, 4, 2, 'jaw')
M.bone('crest', 'head', (0, 48 - 69, 0), (-28, 45, 0))
M.cube('crest', (-2.6, 48 - 69 - 6, -2.6), (5.2, 6, 5.2), 'crystal')
M.cube('crest', (-1.4, 48 - 69 - 9, -1.4), (2.8, 3, 2.8), 'crystal_tip')
for sd in (-1, 1):
    n = f'cheek_{"r" if sd < 0 else "l"}'
    M.bone(n, 'head', (sd * 4.6, 48 - 64, -1), (0, 0, sd * -20))
    M.cube(n, (sd * 4.6 - 1.2, 48 - 64 - 5, -3.5), (2.4, 5, 6), 'crystal_d')


def arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 12.5, 48 - 53, 0))
    hc(a, X(s, -19.5, 11), 59, -5.5, 11, 10, 11, 'crystal')
    hc(a, X(s, -18.5, 9), 61, -4.5, 9, 2, 9, 'crystal_d')
    hc(a, X(s, -18.0, 8.5), 49.5, -4.25, 8.5, 13, 8.5, 'crystal')
    hc(a, X(s, -19.0, 2.5), 47, -3, 2.5, 8, 6, 'crystal_d')
    # back spike rising from the shoulder blade
    sp = f'{side}_spike'
    M.bone(sp, a, (s * 9.5, 48 - 58, 3.5), (-22, s * 10, s * -14))
    M.cube(sp, (s * 9.5 - 2, 48 - 58 - 15, 3.5 - 2), (4, 15, 4), 'crystal')
    M.cube(sp, (s * 9.5 - 1.2, 48 - 58 - 21, 3.5 - 1.2), (2.4, 6, 2.4), 'crystal_tip')
    f = f'{side}_fore'
    M.bone(f, a, (s * 13.75, 48 - 36.5, 0))
    hc(f, X(s, -19.25, 11), 37, -5.5, 11, 17, 11, 'crystal')
    hc(f, X(s, -18.0, 8.5), 39, -6.2, 8.5, 13, 1.2, 'crystal_d')
    h = f'{side}_hand'
    M.bone(h, f, (s * 13.75, 48 - 20, 0))
    hc(h, X(s, -18.5, 9.5), 20.5, -4.5, 9.5, 5, 9, 'crystal')
    for k, fz in enumerate((-4.2, -1.3, 1.6)):
        hc(h, X(s, -18.2, 3.0), 15.5, fz, 3.0, 7 - k * 0.5, 2.8, 'crystal_d')
    hc(h, X(s, -11.5, 3.0), 18, -4, 3.0, 6, 3, 'crystal_d')
    if side == 'right':
        M.bone('shield', f, (s * 13.75, 48 - 30, -6))
        M.cube('shield', (s * 13.75 - 9, 48 - 46, -8.5), (18, 26, 2.5), 'crystal_shield')
        M.cube('shield', (s * 13.75 - 6, 48 - 50, -8.0), (12, 4, 1.5), 'crystal_shield')
        M.cube('shield', (s * 13.75 - 6, 48 - 20, -8.0), (12, 3, 1.5), 'crystal_shield')


def leg(side):
    s = -1 if side == 'right' else 1
    l = f'{side}_leg'
    M.bone(l, 'base', (s * 3.3, 48 - 34, 0))
    hc(l, X(s, -6.4, 6.2), 34.5, -3.1, 6.2, 16.5, 6.2, 'suit_' + side)
    sh = f'{side}_shin'
    M.bone(sh, l, (s * 3.3, 48 - 18, 0))
    hc(sh, X(s, -6.1, 5.6), 18.5, -2.8, 5.6, 16, 5.6, 'suit_' + side)
    hc(sh, X(s, -6.4, 6.2), 3, -6.0, 6.2, 3, 9, 'foot_' + side)


arm('right'); arm('left'); leg('right'); leg('left')

P = Painter(M)


def crystal(c, face, W, H, r, tone=0):
    hi = CRY if tone == 0 else (CRY_D if tone == 1 else (220, 255, 235))
    lo = CRY_D if tone == 0 else (CRY_DD if tone == 1 else CRY)
    col = base(H, W, hi, face, lo, r, 0.05, 0.35)
    yy, xx = grid(H, W)
    # facet planes: a diagonal split with a lighter half
    if W > 3 and H > 3:
        k = r.random() * 0.6 + 0.2
        split = (xx / W + yy / H * k) > (0.55 + r.random() * 0.2)
        col[split] = lerp(col[split], lo, 0.35)
        col = paint(col, stroke(H, W, [(W * 0.15, 0), (W * 0.15 + H * 0.4, H)], 0.6) * 0.5, (240, 255, 248))
    col = outline(col, H, W, (40, 110, 80), 1, 0.85)
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)), (150, 255, 200), 0.08)


P.mats['crystal'] = crystal
P.mats['crystal_d'] = lambda c, f, W, H, r: crystal(c, f, W, H, r, 1)
P.mats['crystal_tip'] = lambda c, f, W, H, r: crystal(c, f, W, H, r, 2)


@P.mat('crystal_shield')
def crystal_shield(c, face, W, H, r):
    col, a, g = crystal(c, face, W, H, r, 2)
    return col, np.full((H, W), 1.0), gmask(H, W, np.ones((H, W)), (150, 255, 200), 0.4)


def split_suit(c, face, W, H, r, chest=False):
    """black on the alien's right (-x), white on his left (+x)"""
    yy, xx = grid(H, W)
    blk = base(H, W, (26, 26, 30), face, (12, 12, 14), r, 0.03, 0.2)
    wht = base(H, W, (240, 240, 242), face, (172, 172, 182), r, 0.03, 0.2)
    if face == 'north':
        m = xx < W / 2
    elif face == 'south':
        m = xx >= W / 2
    elif face == 'west':
        m = np.ones((H, W), bool)
    elif face == 'east':
        m = np.zeros((H, W), bool)
    else:  # top / bottom: u runs along +x
        m = xx < W / 2
    col = np.where(m[..., None], blk, wht)
    g = None
    if face == 'north':
        col = paint(col, band_v(H, W, W / 2 - 0.5, W / 2 + 0.5), (90, 90, 100))
        if chest:
            cx, cy = W * 0.73, H * 0.36
            col = apply(col, omnitrix_symbol(H, W, cx, cy, 2.6, ring=(20, 20, 22), rim=(60, 60, 64), green=(235, 235, 235), dark=(20, 20, 22)))
            g = None
    col = outline(col, H, W, (60, 60, 70), 1, 0.5)
    return col, np.ones((H, W)), g


P.mats['chest'] = lambda c, f, W, H, r: split_suit(c, f, W, H, r, True)
P.mats['abs'] = split_suit
P.mats['pelvis'] = split_suit


def leg_suit(c, face, W, H, r, black):
    col = base(H, W, (26, 26, 30) if black else (240, 240, 242), face, (12, 12, 14) if black else (172, 172, 182), r, 0.03, 0.25)
    col = outline(col, H, W, (70, 70, 80) if black else (120, 120, 130), 1, 0.6)
    return col, np.ones((H, W)), None


P.mats['suit_right'] = lambda c, f, W, H, r: leg_suit(c, f, W, H, r, True)
P.mats['suit_left'] = lambda c, f, W, H, r: leg_suit(c, f, W, H, r, False)
P.mats['foot_right'] = lambda c, f, W, H, r: leg_suit(c, f, W, H, r, True)
P.mats['foot_left'] = lambda c, f, W, H, r: leg_suit(c, f, W, H, r, False)


@P.mat('head')
def head(c, face, W, H, r):
    col, a, g = crystal(c, face, W, H, r, 0)
    if face == 'north':
        yy, xx = grid(H, W)
        # brow ridge + angry yellow eyes
        col = paint(col, band_h(H, W, H * 0.3, H * 0.4), CRY_DD)
        eL = poly(H, W, [(1.0, H * 0.42), (W / 2 - 0.6, H * 0.5), (W / 2 - 0.8, H * 0.6), (1.4, H * 0.55)])
        eR = poly(H, W, [(W - 1.0, H * 0.42), (W / 2 + 0.6, H * 0.5), (W / 2 + 0.8, H * 0.6), (W - 1.4, H * 0.55)])
        col = paint(col, eL + eR, (20, 30, 20))
        el = poly(H, W, [(1.6, H * 0.45), (W / 2 - 1.0, H * 0.52), (W / 2 - 1.2, H * 0.57), (1.8, H * 0.53)])
        er = poly(H, W, [(W - 1.6, H * 0.45), (W / 2 + 1.0, H * 0.52), (W / 2 + 1.2, H * 0.57), (W - 1.8, H * 0.53)])
        col = paint(col, el + er, (255, 220, 40))
        g = gmask(H, W, np.clip(el + er, 0, 1), (255, 220, 60), 0.9)
        col = paint(col, stroke(H, W, [(W / 2, H * 0.62), (W / 2, H * 0.95)], 0.8), CRY_DD)
    return col, a, g


@P.mat('jaw')
def jaw(c, face, W, H, r):
    col, a, g = crystal(c, face, W, H, r, 1)
    if face == 'north':
        col = paint(col, band_h(H, W, H * 0.45, H * 0.6) * band_v(H, W, 1.5, W - 1.5), CRY_DD)
    return col, a, None


# ---------------- animations ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'base']
a = Anim('shards', 0.6)
pose_seq(a, [(0, {}), (0.12, {'right_arm': (-40, 0, 30), 'right_fore': (-60, 0, 0), 'body': (0, -20, 0)}),
             (0.22, {'right_arm': (-95, -10, 0), 'right_fore': (0, 0, 0), 'body': (5, 20, 0), 'left_arm': (25, 0, -10)}),
             (0.4, {'right_arm': (-92, -10, 0), 'body': (5, 18, 0)}), (0.6, {})], B)
A['ab1'] = a
a = Anim('spikes', 1.2)
pose_seq(a, [(0, {}), (0.25, {'right_arm': (-160, 0, 15), 'body': (-10, 0, 0)}),
             (0.33, {'right_arm': (-30, 0, 5), 'right_fore': (-20, 0, 0), 'body': (40, 0, 0), 'right_leg': (-45, 0, 0), 'right_shin': (70, 0, 0), 'left_leg': (15, 0, 0)}),
             (0.95, {'right_arm': (-30, 0, 5), 'right_fore': (-20, 0, 0), 'body': (38, 0, 0), 'right_leg': (-45, 0, 0), 'right_shin': (70, 0, 0), 'left_leg': (15, 0, 0)}),
             (1.2, {})], B)
a.pos('base', (0, (0, 0, 0)), (0.33, (0, -6, 0)), (0.95, (0, -6, 0)), (1.2, (0, 0, 0)))
A['ab2'] = a
a = Anim('wall', 0.8)
pose_seq(a, [(0, {}), (0.2, {'right_arm': (-170, 0, 10), 'left_arm': (-170, 0, -10), 'body': (-12, 0, 0)}),
             (0.32, {'right_arm': (-70, 0, 10), 'left_arm': (-70, 0, -10), 'body': (30, 0, 0)}),
             (0.6, {'right_arm': (-70, 0, 10), 'left_arm': (-70, 0, -10), 'body': (28, 0, 0)}), (0.8, {})], B)
A['ab3'] = a
UPP = {'right_arm': (-170, 0, -15), 'left_arm': (-170, 0, 15), 'right_fore': (-15, 0, 0), 'left_fore': (-15, 0, 0), 'body': (-15, 0, 0), 'head': (-35, 0, 0)}
a = Anim('storm', 2.5)
pose_seq(a, [(0, {}), (0.4, UPP), (1.2, {**UPP, 'body': (-18, 0, 0)}), (2.2, UPP), (2.5, {})], B)
A['ab4'] = a
G = {'right_arm': (-60, -28, 0), 'right_fore': (-70, 0, 0), 'left_arm': (-30, 20, 0), 'left_fore': (-40, 0, 0), 'body': (6, 10, 0)}
a = Anim('block', 1.0, loop=True)
pose_seq(a, [(0, G), (1.0, G)], B)
A['block'] = a
