"""Furniture models (vanilla ModelPart space, 1u = 1/32 block, ground at y=48, front = -z, +x = viewer's right
when looking at the front). Coordinates are written as (x0,x1) (h0,h1) (z0,z1) with h = height above ground."""
import numpy as np
from ftex import *
from paint import Painter

G = 48


def newbone(m, name, px=0.0, ph=0.0, pz=0.0, rot=(0, 0, 0)):
    m.bone(name, None, (px, G - ph, pz), rot)


def box(m, bone, x, h, z, mat, **kw):
    return m.cube(bone, (x[0], G - h[1], z[0]), (x[1] - x[0], h[1] - h[0], z[1] - z[0]), mat, feat=kw)


def pal_of(c, default):
    return PALS[c.feat.get('pal', default)]


def make_painter(m):
    P = Painter(m)

    def wood(c, face, W, H, r):
        col = wood_face(c, face, W, H, r, pal_of(c, 'mah'))
        return col, np.ones((H, W)), None
    P.mats['wood'] = wood

    def fabric(c, face, W, H, r):
        return fabric_face(c, face, W, H, r, pal_of(c, 'cream'), c.feat.get('seam')), np.ones((H, W)), None
    P.mats['fabric'] = fabric

    def metal(c, face, W, H, r):
        return metal_face(c, face, W, H, r, pal_of(c, 'steel')), np.ones((H, W)), None
    P.mats['metal'] = metal

    def gold(c, face, W, H, r):
        return metal_face(c, face, W, H, r, GOLD), np.ones((H, W)), None
    P.mats['gold'] = gold

    def bristle(c, face, W, H, r):
        nz = noise2(H, W, r, scale=2.0, oct=2)
        f = 0.55 + (nz - 0.5) * 0.7
        if face in ('north', 'south', 'west', 'east') and W > 2:
            for x in range(0, W, 2):
                f[:, x] -= 0.12
        col, _ = quant(np.clip(f, 0, 1), BRISTLE)
        if face == 'bottom':
            col = col * 0.8
        return col, np.ones((H, W)), None
    P.mats['bristle'] = bristle

    def interior(c, face, W, H, r):
        col = wood_face(c, face, W, H, r, MAH_DARK) * 0.75
        return col, np.ones((H, W)), None
    P.mats['interior'] = interior

    def framed(c, face, W, H, r):
        """front face: bright border + dark inset; other faces plain wood"""
        front = c.feat.get('front', 'north')
        if face != front:
            return wood(c, face, W, H, r)
        b = c.feat.get('border', 2)
        col = wood_face(c, face, W, H, r, MAH_LT)
        inner = wood_face(c, face, W, H, r, pal_of_inner(c))
        yy, xx = np.mgrid[0:H, 0:W]
        ins = (yy >= b) & (yy < H - b) & (xx >= b) & (xx < W - b)
        col = np.where(ins[..., None], inner, col)
        # bevel inside the inset
        if ins.any():
            dark = (yy == b) & ins | ((xx == b) & ins)
            col = np.where(dark[..., None], col * 0.72, col)
        return col, np.ones((H, W)), None
    P.mats['framed'] = framed

    def pal_of_inner(c):
        return PALS[c.feat.get('inner', 'dark')]

    def ring(c, face, W, H, r):
        if face == 'north':
            col, a = ring_handle(W, H)
            return col, a, None
        return metal_face(c, face, W, H, r), np.ones((H, W)), None
    P.mats['ring'] = ring

    def keyw(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32)
        col[:] = IVORY[3]
        nz = noise2(H, W, r, scale=2.0, oct=1)
        col -= (nz[..., None] - 0.5) * 10
        if face == 'top':
            col[:, 0] = IVORY[1]
            col[:, -1] = IVORY[0]
        elif face in ('north',):
            col[:] = IVORY[2]
            col[:, 0] = IVORY[1]; col[:, -1] = IVORY[0]
            col[-1, :] = IVORY[0]
        else:
            col[:] = IVORY[1]
        return col, np.ones((H, W)), None
    P.mats['key_w'] = keyw

    def keyb(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32)
        col[:] = EBONY[1]
        if face == 'top':
            col[:] = EBONY[2]
            col[:, 0] = EBONY[3]
            col[0:1, :] = EBONY[3]
        elif face == 'north':
            col[:] = EBONY[1]
            col[0, :] = EBONY[3]
        return col, np.ones((H, W)), None
    P.mats['key_b'] = keyb
    return P


# ---------------------------------------------------------------------------------------------------- SOFA
def sofa():
    m = Model('sofa', 256, 256)
    for n, p in (('body', (0, 0, 0)), ('seat_a', (12, 13, 0)), ('seat_b', (-12, 13, 0)), ('back_pad', (0, 19, 12))):
        newbone(m, n, *p)
    W = 'wood'
    # legs: chunky blocks with a wider foot plate
    for sx in (-1, 1):
        for fz in (-1, 1):
            x0, x1 = (23, 31) if sx > 0 else (-31, -23)
            z0, z1 = (-14, -6) if fz < 0 else (6, 14)
            box(m, 'body', (x0, x1), (2, 7), (z0, z1), W, pal='mah')
            box(m, 'body', (x0 - 1, x1 + 1), (0, 2), (z0 - 1, z1 + 1), W, pal='lt')
    # base frame
    box(m, 'body', (-32, 32), (7, 12), (-16, 16), W, pal='mah')
    box(m, 'body', (-31, 31), (12, 13), (-15.5 if False else -15, 14), W, pal='dark')
    # front rim (lighter wood) and back panel
    box(m, 'body', (-32, 32), (11, 14), (-17, -15), W, pal='lt')
    # back: lavender fabric panel + wooden rails (stepped top)
    box(m, 'body', (-31, 31), (13, 30), (12, 16), 'fabric', pal='lav')
    box(m, 'body', (-32, 32), (30, 34), (10, 17), W, pal='mah')
    box(m, 'body', (-27, 27), (34, 37), (11, 16), W, pal='lt')
    box(m, 'body', (-22, 22), (37, 38), (12, 15), W, pal='lt')
    # arms (outer lavender, cream top plate, wooden end caps)
    for sx in (-1, 1):
        x0, x1 = (24, 32) if sx > 0 else (-32, -24)
        box(m, 'body', (x0, x1), (13, 24), (-16, 16), 'fabric', pal='lav')
        a0, a1 = (25, 31) if sx > 0 else (-31, -25)
        box(m, 'body', (a0, a1), (24, 26), (-14, 12), 'fabric', pal='cream', seam=False)
        box(m, 'body', (x0, x1), (13, 21), (-18, -16), W, pal='mah')
        box(m, 'body', (x0, x1), (21, 25), (-17, -16), W, pal='lt')
        # bolster pillow leaning on the arm
        b0, b1 = (17, 23) if sx > 0 else (-23, -17)
        box(m, 'body', (b0, b1), (19, 27), (-12, 4), 'fabric', pal='cream')
    # seat cushions (animated) + backrest pad (animated)
    box(m, 'seat_a', (0, 23), (13, 19), (-14, 11), 'fabric', pal='cream', seam=False)
    box(m, 'seat_b', (-23, 0), (13, 19), (-14, 11), 'fabric', pal='cream', seam=False)
    box(m, 'back_pad', (-22, 22), (19, 30), (9, 12), 'fabric', pal='cream')
    return m, 'sofa'


# ---------------------------------------------------------------------------------------------------- PIANO
KEYS_W = 14
KW = 3.5
KX0 = -24.5
BLACK_AFTER = [0, 1, 3, 4, 5, 7, 8, 10, 11, 12]  # black key sits after white key idx
PIANO_H = 52


def piano():
    m = Model('piano', 256, 256)
    newbone(m, 'body')
    newbone(m, 'pedal_a', -6, 3, -8)
    newbone(m, 'pedal_b', 6, 3, -8)
    W = 'wood'
    for sx in (-1, 1):
        x0, x1 = (20.5, 27.5) if sx > 0 else (-27.5, -20.5)
        box(m, 'body', (x0, x1), (0, 13), (-10, 2), W, pal='mah')
        box(m, 'body', (x0, x1), (0, 2), (-11, 3), W, pal='lt')
        box(m, 'body', (x0 + 1, x1 - 1), (12, 13), (-11, -9), 'metal')
        box(m, 'body', (x0, x1), (0, 13), (8, 14), W, pal='dark')
    # lower panel, foot rail, bottom
    box(m, 'body', (-20.5, 20.5), (6, 13), (-3, 0), 'framed', pal='dark', border=2, inner='dark')
    box(m, 'body', (-20.5, 20.5), (3, 6), (-6, -2), W, pal='mah')
    box(m, 'body', (-20.5, 20.5), (0, 3), (0, 14), W, pal='dark')
    # main case + side cheeks
    box(m, 'body', (-24.5, 24.5), (13, 48), (0, 14), W, pal='mah')
    for sx in (-1, 1):
        x0, x1 = (24.5, 27.5) if sx > 0 else (-27.5, -24.5)
        box(m, 'body', (x0, x1), (11, 48), (-4, 15), W, pal='mah')
    # top lid
    box(m, 'body', (-29, 29), (48, 52), (-6, 16), W, pal='lt')
    # upper front panel: frame, dark windows
    box(m, 'body', (-24.5, 24.5), (30, 48), (-3, 0), W, pal='lt')
    box(m, 'body', (-22.5, -12.5), (35, 44), (-4, -3), 'framed', pal='dark', border=1, inner='dark')
    box(m, 'body', (12.5, 22.5), (35, 44), (-4, -3), 'framed', pal='dark', border=1, inner='dark')
    box(m, 'body', (-9.5, 9.5), (37, 42), (-4, -3), 'framed', pal='dark', border=1, inner='dark')
    # fallboard shelf above the keys
    box(m, 'body', (-24.5, 24.5), (27, 31), (-9, -3), W, pal='lt')
    # key bed and slip
    box(m, 'body', (-24.5, 24.5), (20, 25), (-14, 0), W, pal='mah')
    for sx in (-1, 1):
        x0, x1 = (24.5, 28.5) if sx > 0 else (-28.5, -24.5)
        box(m, 'body', (x0, x1), (14, 28), (-14, -3), W, pal='mah')
        box(m, 'body', (x0, x1), (28, 30), (-13, -4), W, pal='lt')
    # keys
    for i in range(KEYS_W):
        n = f'wk{i}'
        x0 = KX0 + KW * i
        newbone(m, n, x0 + KW / 2, 27, -3)
        box(m, n, (x0, x0 + KW), (25, 27), (-13, -3), 'key_w')
    for j, k in enumerate(BLACK_AFTER):
        n = f'bk{j}'
        xc = KX0 + KW * (k + 1)
        newbone(m, n, xc, 29, -3)
        box(m, n, (xc - 1, xc + 1), (27, 29), (-13, -7), 'key_b')
    box(m, 'pedal_a', (-9, -3), (2, 4), (-12, -4), 'gold')
    box(m, 'pedal_b', (3, 9), (2, 4), (-12, -4), 'gold')
    return m, 'piano'


# ---------------------------------------------------------------------------------------------------- CHALKBOARD
def chalkboard(clean=False):
    m = Model('chalkboard', 128, 128)
    newbone(m, 'body')
    W = 'wood'
    for sx in (-1, 1):
        x0, x1 = (10, 16) if sx > 0 else (-16, -10)
        box(m, 'body', (x0, x1), (0, 9), (-6, 0), W, pal='mah')
        box(m, 'body', (x0 - 1, x1 + 1), (0, 2), (-7, 1), W, pal='lt')
    box(m, 'body', (-3, 3), (0, 12), (3, 8), W, pal='dark')
    box(m, 'body', (-18, 18), (8, 12), (-5, 5), W, pal='dark')
    box(m, 'body', (-18, 18), (12, 13), (-5, 5), W, pal='lt')
    for sx in (-1, 1):
        x0, x1 = (12, 17) if sx > 0 else (-17, -12)
        box(m, 'body', (x0, x1), (13, 37), (-3, 3), W, pal='mah')
        box(m, 'body', (x0 - 1, x1 + 1), (36, 40), (-4, 4), W, pal='lt')
        box(m, 'body', (x0, x1), (13, 15), (-4, 4), W, pal='dark')
    box(m, 'body', (-19, 19), (40, 43), (-5, 5), W, pal='lt')
    box(m, 'body', (-18, 18), (43, 44), (-4, 4), W, pal='mah')
    # crown ornament
    box(m, 'body', (-3, 3), (44, 46), (-1, 1), W, pal='mah')
    box(m, 'body', (-5, -3), (46, 50), (-1, 1), W, pal='mah')
    box(m, 'body', (3, 5), (46, 50), (-1, 1), W, pal='mah')
    box(m, 'body', (-7, -5), (48, 50), (-1, 1), W, pal='lt')
    box(m, 'body', (5, 7), (48, 50), (-1, 1), W, pal='lt')
    box(m, 'body', (-2, 2), (46, 48), (-1, 1), W, pal='lt')
    # slate (front face is painted)
    box(m, 'body', (-12, 12), (13, 37), (-2, 2), 'board', clean=clean)
    return m, 'chalkboard'


# ---------------------------------------------------------------------------------------------------- NIGHTSTAND
def nightstand():
    m = Model('nightstand', 256, 256)
    newbone(m, 'body')
    newbone(m, 'drawer_up', 0, 21, 0)
    newbone(m, 'drawer_dn', 0, 10, 0)
    W = 'wood'
    for sx in (-1, 1):
        for fz in (-1, 1):
            x0, x1 = (11, 15) if sx > 0 else (-15, -11)
            z0, z1 = (-14, -10) if fz < 0 else (10, 14)
            box(m, 'body', (x0, x1), (0, 4), (z0, z1), W, pal='dark')
    # shell
    for sx in (-1, 1):
        x0, x1 = (13, 16) if sx > 0 else (-16, -13)
        box(m, 'body', (x0, x1), (4, 28), (-14, 14), 'framed', pal='mah', front='east' if sx > 0 else 'west', border=2, inner='dark')
    box(m, 'body', (-13, 13), (4, 6), (-13, 13), 'interior')
    box(m, 'body', (-13, 13), (6, 26), (12, 14), 'interior')
    box(m, 'body', (-13, 13), (26, 28), (-13, 13), W, pal='dark')
    box(m, 'body', (-13, 13), (15, 17), (-12, 12), W, pal='mah')
    # front frame bars
    box(m, 'body', (-13, 13), (15, 17), (-14, -12), W, pal='lt')
    box(m, 'body', (-13, 13), (26, 28), (-14, -12), W, pal='lt')
    box(m, 'body', (-13, 13), (4, 6), (-14, -12), W, pal='lt')
    # top slab
    box(m, 'body', (-16, 16), (28, 31), (-15, 15), W, pal='lt', grain='x')
    # drawers
    for name, h0, h1 in (('drawer_up', 17, 26), ('drawer_dn', 6, 15)):
        hm = (h0 + h1) / 2
        box(m, name, (-12.5 if False else -13, 13), (h0, h1), (-15, -13), 'framed', pal='lt', border=2, inner='dark')
        box(m, name, (-2.5 if False else -3, 3), (hm - 3, hm + 3), (-16, -15), 'ring')
        box(m, name, (-12, 12), (h0 + 1, h0 + 2), (-13, 11), 'interior')
        box(m, name, (-12, -11), (h0 + 1, h1 - 1), (-13, 11), W, pal='mah')
        box(m, name, (11, 12), (h0 + 1, h1 - 1), (-13, 11), W, pal='mah')
        box(m, name, (-12, 12), (h0 + 1, h1 - 1), (11, 12), W, pal='mah')
    return m, 'nightstand'


# ---------------------------------------------------------------------------------------------------- WARDROBE
def wardrobe():
    m = Model('wardrobe', 256, 320)
    newbone(m, 'body')
    newbone(m, 'door_r', 14, 0, -13)   # hinge on +x side
    newbone(m, 'door_l', -14, 0, -13)  # hinge on -x side
    newbone(m, 'drawer', 0, 8, 0)
    W = 'wood'
    for sx in (-1, 1):
        for fz in (-1, 1):
            x0, x1 = (12, 16) if sx > 0 else (-16, -12)
            z0, z1 = (-15, -11) if fz < 0 else (11, 15)
            box(m, 'body', (x0, x1), (0, 2), (z0, z1), W, pal='dark')
    box(m, 'body', (-16, 16), (2, 4), (-15, 15), W, pal='mah')
    for sx in (-1, 1):
        x0, x1 = (13, 16) if sx > 0 else (-16, -13)
        box(m, 'body', (x0, x1), (4, 13), (-15, 15), W, pal='mah')
    box(m, 'body', (-13, 13), (4, 13), (13, 15), 'interior')
    box(m, 'body', (-13, 13), (4, 6), (-14, 13), 'interior')
    box(m, 'body', (-16, 16), (13, 15), (-15, 15), W, pal='lt')
    box(m, 'body', (-13, 13), (4, 5), (-15, -13), W, pal='lt')
    box(m, 'body', (-13, 13), (12, 13), (-15, -13), W, pal='lt')
    # upper body
    for sx in (-1, 1):
        x0, x1 = (14, 16) if sx > 0 else (-16, -14)
        box(m, 'body', (x0, x1), (15, 47), (-14, 14), 'framed', pal='mah', front='east' if sx > 0 else 'west', border=3, inner='dark')
    box(m, 'body', (-14, 14), (15, 47), (12, 14), 'interior')
    box(m, 'body', (-14, 14), (15, 17), (-14, 12), 'interior')
    box(m, 'body', (-14, 14), (45, 47), (-14, 12), W, pal='dark')
    box(m, 'body', (-14, 14), (30, 32), (-12, 12), W, pal='mah')
    box(m, 'body', (-13, 13), (39, 41), (-1, 1), W, pal='lt')
    # cornice
    box(m, 'body', (-17, 17), (47, 50), (-15, 15), W, pal='lt')
    box(m, 'body', (-16, 16), (50, 52), (-14, 14), W, pal='mah')
    # front trim
    box(m, 'body', (-14, 14), (45, 47), (-14, -12), W, pal='lt')
    box(m, 'body', (-14, 14), (15, 17), (-14, -12), W, pal='lt')
    box(m, 'body', (-1, 1), (17, 45), (-14, -12), W, pal='dark')
    for name, sx in (('door_r', 1), ('door_l', -1)):
        x0, x1 = (0.5, 14) if sx > 0 else (-14, -0.5)
        box(m, name, (x0, x1), (17, 45), (-14, -12), 'framed', pal='mah', border=2, inner='dark', front='north')
        for ra, rb in (((11, 12), (7, 8), (0.5, 1.5)) if sx > 0 else ((-12, -11), (-8, -7), (-1.5, -0.5))):
            pass
        ribs = ((11, 12), (7, 8), (0.5, 1.5)) if sx > 0 else ((-12, -11), (-8, -7), (-1.5, -0.5))
        for ra, rb in ribs:
            box(m, name, (ra, rb), (18, 44), (-15, -14), W, pal='lt')
        h0, h1 = (6, 14) if sx > 0 else (-14, -6)
        box(m, name, (h0, h1), (41, 44), (-16, -14), 'metal')
        box(m, name, (h0, h1), (18, 21), (-16, -14), 'metal')
        p0, p1 = (2, 6) if sx > 0 else (-6, -2)
        box(m, name, (p0, p1), (27, 33), (-16, -14), 'ring')
    box(m, 'drawer', (-13, 13), (5, 12), (-17, -15), 'framed', pal='lt', border=2, inner='dark')
    box(m, 'drawer', (-4, 4), (7, 10), (-18, -17), 'ring')
    box(m, 'drawer', (-12, 12), (6, 7), (-15, 13), 'interior')
    box(m, 'drawer', (-12, -11), (6, 11), (-15, 13), W, pal='mah')
    box(m, 'drawer', (11, 12), (6, 11), (-15, 13), W, pal='mah')
    box(m, 'drawer', (-12, 12), (6, 11), (12, 13), W, pal='mah')
    return m, 'wardrobe'


# ---------------------------------------------------------------------------------------------------- BROOM
def broom():
    m = Model('broom', 64, 64)
    newbone(m, 'body')
    # upright: head at the bottom (h 0..), handle up
    box(m, 'body', (-5, 5), (0, 9), (-3, 3), 'bristle')
    box(m, 'body', (-5.5, 5.5), (4, 7), (-3.5, 3.5), 'metal')
    box(m, 'body', (-2, 2), (9, 14), (-2, 2), 'metal')
    box(m, 'body', (-1.5, 1.5), (14, 50), (-1.5, 1.5), 'wood', pal='mah')
    box(m, 'body', (-2, 2), (18, 20), (-2, 2), 'metal')
    box(m, 'body', (-2, 2), (38, 40), (-2, 2), 'metal')
    box(m, 'body', (-1.5, 1.5), (50, 52), (-1.5, 1.5), 'wood', pal='dark')
    return m, 'broom'


def board_painter(P, clean=False):
    """'board' material: green slate, white chalk frame with corner crosses, optional sample text lines"""
    def board(c, face, W, H, r):
        if face == 'north':
            col = np.zeros((H, W, 3), np.float32)
            nz = noise2(H, W, r, scale=3.0, oct=2)
            base, _ = quant(np.clip(0.2 + nz * 0.55, 0, 1), BOARD)
            col[:] = base
            dust = r.random((H, W)) < 0.05
            col[dust] = np.array(BOARD[-1]) * 1.3
            ch = np.array(CHALK, np.float32)
            fr = np.zeros((H, W), bool)
            fr[0, :] = True; fr[-1, :] = True; fr[:, 0] = True; fr[:, -1] = True
            fr[1, 1:-1] = True; fr[-2, 1:-1] = True; fr[1:-1, 1] = True; fr[1:-1, -2] = True

            def cross(cx, cy):
                for d in range(-3, 4):
                    for t in (0, 1):
                        if 0 <= cy + t < H and 0 <= cx + d < W:
                            fr[cy + t, cx + d] = True
                        if 0 <= cy + d < H and 0 <= cx + t < W:
                            fr[cy + d, cx + t] = True
            cross(5, 4); cross(W - 7, 4); cross(5, H - 6); cross(W - 7, H - 6)
            rr = r.random(fr.shape)
            col[fr] = (ch[None, :] * (0.88 + 0.12 * rr[fr])[:, None])
            if not c.feat.get('clean'):
                def seg(x0, y, w):
                    col[y:y + 1, x0:x0 + w] = ch * 0.93
                y0 = 8
                seg(6, y0, 5); seg(12, y0, 1); seg(15, y0, 3); seg(19, y0, 2)
                seg(6, y0 + 2, 3); seg(10, y0 + 2, 1); seg(12, y0 + 2, 1); seg(15, y0 + 2, 4)
                seg(6, y0 + 5, 11); seg(6, y0 + 8, 11); seg(6, y0 + 11, 9)
            return col, np.ones((H, W)), None
        col = wood_face(c, face, W, H, r, MAH_DARK)
        return col, np.ones((H, W)), None
    P.mats['board'] = board


BUILDERS = {'sofa': sofa, 'piano': piano, 'nightstand': nightstand, 'wardrobe': wardrobe, 'broom': broom,
            'chalkboard': lambda: chalkboard(False)}
