"""Big Chill (Necrofriggian): blue moth-cloak of folded wings with black veins and pale-blue spots, hooded head with green compound eyes,
thin black arms with long claws, thin blue legs. The two wings spread open when flying."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp
from toon import *
from animlib import pose_seq

M = Model('bigchill', 512, 256)
hc = box_helpers(M)
FIT = 80

BLUE = (22, 108, 160)
BLUE_D = (10, 64, 104)
PALE = (70, 196, 250)
BLK = (14, 14, 18)


def X(s, x, w):
    return x if s == -1 else -(x + w)


M.bone('base', None, (0, 48 - 32, 0))
M.bone('body', 'base', (0, 48 - 32, 0))
hc('body', -4, 49, -2.5, 8, 17, 5, 'inner')
hc('body', -7.5, 51, -4.4, 15, 29, 1.6, 'cloak_front')       # front cloak panel, ragged hem
hc('body', -7, 52, -3, 14, 3, 7.5, 'cloak_d')               # collar / shoulders
M.bone('head', 'body', (0, 48 - 51, -0.5))
M.bone('hood', 'head', (0, 48 - 52, -0.5), (-8, 0, 0))
M.cube('hood', (-5, 48 - 66, -5), (10, 14, 10), 'hood')
M.cube('hood', (-3.5, 48 - 69, -2), (7, 3.5, 7), 'cloak')
M.cube('hood', (-2, 48 - 71, 0), (4, 2.5, 4), 'cloak_d')


def wing(side):
    s = -1 if side == 'right' else 1
    w = f'{side}_wing'
    M.bone(w, 'body', (s * 3.5, 48 - 51, 2.5), (0, s * 72, 0))
    # panel hangs from the shoulder, extends sideways (-x for right) so opening = rotate around y
    M.cube(w, (X(s, -3.5 - 13, 13), 48 - 51, 2.5), (13, 34, 1.5), 'wing')


def arm(side):
    s = -1 if side == 'right' else 1
    a = f'{side}_arm'
    M.bone(a, 'body', (s * 6.0, 48 - 46, -1))
    hc(a, X(s, -7.4, 2.8), 47, -2.4, 2.8, 11, 2.8, 'black')
    f = f'{side}_fore'
    M.bone(f, a, (s * 6.0, 48 - 36, -1))
    hc(f, X(s, -7.2, 2.4), 36.5, -2.2, 2.4, 10, 2.4, 'black')
    h = f'{side}_hand'
    M.bone(h, f, (s * 6.0, 48 - 27, -1))
    hc(h, X(s, -7.6, 3.2), 27.5, -2.6, 3.2, 2.5, 3.2, 'black')
    for k, (fx, fz) in enumerate(((-7.6, -2.6), (-6.4, -0.6), (-5.0, -2.2))):
        cl = f'{side}_claw{k}'
        M.bone(cl, h, (s * 6.0, 48 - 25, fz + 0.5), (-10 - k * 8, 0, -s * (6 - k * 8)))
        M.cube(cl, (X(s, fx, 1.0), 48 - 25, fz), (1.0, 7 - k, 1.0), 'claw')


def leg(side):
    s = -1 if side == 'right' else 1
    l = f'{side}_leg'
    M.bone(l, 'base', (s * 2.2, 48 - 32, 0))
    hc(l, X(s, -3.8, 3.2), 32.5, -1.6, 3.2, 15, 3.2, 'leg')
    sh = f'{side}_shin'
    M.bone(sh, l, (s * 2.2, 48 - 18, 0))
    hc(sh, X(s, -3.5, 2.6), 18.5, -1.3, 2.6, 15, 2.6, 'leg')
    for k, fz in enumerate((-4.5, -4.5, 0.8)):
        fx = (-4.4, -2.4, -3.4)[k]
        hc(sh, X(s, fx, 1.1), 3.5, fz, 1.1, 1.0, 4.0 if k < 2 else 2.2, 'claw')
    hc(sh, X(s, -3.8, 3.2), 4.5, -1.6, 3.2, 1.6, 3.2, 'black')


wing('right'); wing('left'); arm('right'); arm('left'); leg('right'); leg('left')

P = Painter(M)


def veins(H, W, r, n=3):
    m = np.zeros((H, W), np.float32)
    for _ in range(n):
        x0 = r.random() * W
        pts = [(x0, 0)]
        for k in range(1, 5):
            pts.append((pts[-1][0] + (r.random() - 0.5) * W * 0.5, H * k / 4))
        m = np.maximum(m, stroke(H, W, pts, 1.6))
    m = np.maximum(m, stroke(H, W, [(0, H * 0.35), (W * 0.5, H * 0.5), (W, H * 0.4)], 1.4))
    return m


def cloak(c, face, W, H, r, kind='plain'):
    col = base(H, W, BLUE, face, BLUE_D, r, 0.05, 0.3)
    alpha = np.ones((H, W))
    yy, xx = grid(H, W)
    if face in ('north', 'south') and H > 10:
        col = paint(col, veins(H, W, r, 3 if W > 10 else 2), BLK)
        for _ in range(3):
            cx, cy = r.random() * W, H * (0.15 + r.random() * 0.6)
            col = paint(col, poly(H, W, [(cx, cy - 2.4), (cx + 1.3, cy), (cx, cy + 2.4), (cx - 1.3, cy)]), PALE)
        if kind in ('front', 'wing'):
            # ragged pointed hem
            n = max(2, int(W / 4))
            for x in range(W):
                ph = (x / W * n) % 1.0
                depth = (abs(ph - 0.5) * 2) * 5.0
                alpha[int(H - depth):, x] = 0
                lo = int(H - depth) - 1
                if 0 <= lo < H:
                    col[lo, x] = lerp(col[lo, x], BLK, 0.8)
    col = outline(col, H, W, (4, 20, 40), 1, 0.7)
    return col, alpha, None


P.mats['cloak'] = cloak
P.mats['cloak_d'] = lambda c, f, W, H, r: (outline(base(H, W, BLUE_D, f, (6, 40, 70), r, 0.04), H, W, (4, 20, 40), 1, 0.7), np.ones((H, W)), None)
P.mats['cloak_front'] = lambda c, f, W, H, r: cloak(c, f, W, H, r, 'front')
P.mats['wing'] = lambda c, f, W, H, r: cloak(c, f, W, H, r, 'wing')


@P.mat('hood')
def hood(c, face, W, H, r):
    col, a, _ = cloak(c, face, W, H, r)
    g = None
    if face == 'north':
        # dark face opening framed by the hood, huge green compound eyes, little fangs
        col = paint(col, poly(H, W, [(1.2, H * 0.2), (W - 1.2, H * 0.2), (W - 1.8, H * 0.95), (1.8, H * 0.95)]), BLK)
        el = poly(H, W, [(1.8, H * 0.32), (W / 2 - 0.5, H * 0.4), (W / 2 - 0.9, H * 0.6), (2.2, H * 0.52)])
        er = poly(H, W, [(W - 1.8, H * 0.32), (W / 2 + 0.5, H * 0.4), (W / 2 + 0.9, H * 0.6), (W - 2.2, H * 0.52)])
        col = paint(col, el + er, (130, 230, 60))
        yy, xx = grid(H, W)
        grid_ = ((xx.astype(int) + yy.astype(int)) % 2 == 0) * np.clip(el + er, 0, 1)
        col = paint(col, grid_ * 0.35, (60, 140, 30))
        col = paint(col, band_h(H, W, H * 0.72, H * 0.8) * band_v(H, W, W * 0.35, W * 0.65), (180, 220, 240))
        for k in range(3):
            col = paint(col, band_v(H, W, W * (0.37 + k * 0.1), W * (0.37 + k * 0.1) + 0.5) * band_h(H, W, H * 0.72, H * 0.8), BLK)
        g = gmask(H, W, np.clip(el + er, 0, 1), (140, 255, 70), 0.9)
    return col, a, g


def flat_c(color, shade, ink):
    def fn(c, face, W, H, r):
        col = base(H, W, color, face, shade, r, 0.04, 0.3)
        return outline(col, H, W, ink, 1, 0.6), np.ones((H, W)), None
    return fn


P.mats['inner'] = flat_c((30, 34, 46), (14, 16, 22), (0, 0, 0))
P.mats['black'] = flat_c(BLK, (4, 4, 6), (60, 70, 90))
P.mats['claw'] = flat_c((30, 30, 36), (8, 8, 10), (90, 100, 120))


@P.mat('leg')
def leg_m(c, face, W, H, r):
    col = base(H, W, (60, 170, 220), face, (30, 110, 160), r, 0.05, 0.3)
    if face in ('north', 'south', 'west', 'east'):
        for k in range(2):
            col = paint(col, stroke(H, W, [(0, H * (0.3 + k * 0.4)), (W, H * (0.4 + k * 0.4))], 1.0), BLK)
    col = outline(col, H, W, (6, 30, 50), 1, 0.6)
    return col, np.ones((H, W)), None


# ---------------- animations ----------------
A = {}
B = ['body', 'head', 'right_arm', 'left_arm', 'right_fore', 'left_fore', 'right_leg', 'left_leg', 'right_shin', 'left_shin', 'base',
     'right_wing', 'left_wing']
BR = {'head': (-25, 0, 0), 'body': (-10, 0, 0), 'right_arm': (-30, 0, 40), 'left_arm': (-30, 0, -40), 'right_wing': (0, 30, 20), 'left_wing': (0, -30, -20)}
a = Anim('breath', 2.5)
pose_seq(a, [(0, {}), (0.2, {**BR, 'head': (-35, 0, 0)}), (2.3, BR), (2.5, {})], B)
A['ab1'] = a
a = Anim('prison', 1.0)
pose_seq(a, [(0, {}), (0.25, {'right_arm': (-100, -10, 0), 'left_arm': (-100, 10, 0), 'right_fore': (-10, 0, 0), 'body': (10, 0, 0)}),
             (0.75, {'right_arm': (-100, 15, 0), 'left_arm': (-100, -15, 0), 'body': (12, 0, 0)}), (1.0, {})], B)
A['ab2'] = a
a = Anim('spear', 0.7)
pose_seq(a, [(0, {}), (0.18, {'right_arm': (-170, 0, 20), 'right_fore': (-40, 0, 0), 'body': (-10, -20, 0)}),
             (0.27, {'right_arm': (-70, 0, 0), 'right_fore': (0, 0, 0), 'body': (15, 20, 0)}), (0.7, {})], B)
A['ab3'] = a
BL = {'right_arm': (-160, 0, -30), 'left_arm': (-160, 0, 30), 'right_wing': (0, 70, 30), 'left_wing': (0, -70, -30), 'body': (-10, 0, 0),
      'head': (-20, 0, 0), 'right_leg': (15, 0, 10), 'left_leg': (15, 0, -10)}
a = Anim('blizzard', 5.0)
pose_seq(a, [(0, {}), (0.5, BL), (2.5, {**BL, 'right_wing': (0, 80, 30), 'left_wing': (0, -80, -30)}), (4.7, BL), (5.0, {})], B)
A['ab4'] = a
GU = {'right_wing': (0, -30, -10), 'left_wing': (0, 30, 10), 'right_arm': (-60, 0, -30), 'left_arm': (-60, 0, 30), 'body': (12, 0, 0), 'head': (15, 0, 0)}
a = Anim('block', 1.0, loop=True)
pose_seq(a, [(0, GU), (1.0, GU)], B)
A['block'] = a
