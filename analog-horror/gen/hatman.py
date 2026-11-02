"""The Hatman - emaciated analog-horror stalker: tiny head under a wide straw hat, glossy charcoal skin,
pale sternum, absurdly long multi-jointed limbs and fork-like fingers."""
import os, sys, math, numpy as np
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../gen'))
from core import Model, Anim, rot_zyx
from paint import Painter, noise2, lerp

M = Model('hatman', 128, 128)


def up(name, parent, A, B, w, d, mat, extend=0.0, **kw):
    """bone at A whose local -y points at B (upward segments keep natural rotation axes)"""
    A = np.array(A, float); B = np.array(B, float)
    v = B - A; L = np.linalg.norm(v); v = -v / L
    x = math.asin(max(-1, min(1, v[2])))
    z = math.atan2(-v[0], v[1])
    M.bone_w(name, parent, A, rot_zyx(x, 0, z))
    M.cube_l(name, (-w / 2, -L - extend, -d / 2), (w, L + 2 * extend, d), mat, **kw)
    return L


def local(name, parent, off, rot=(0, 0, 0)):
    M.bone(name, parent, (0, 0, 0))
    M.bones[name].off = [float(v) for v in off]
    M.bones[name].rot = [float(v) for v in rot]


# ---------------- skeleton (world rest pose: y down, front -z, feet at y=24) ----------------
M.bone('base', None, (0, -6, 0))
M.bone('hips', 'base', (0, -6, 0))
M.cube('hips', (-2.6, -7.5, -1.6), (5.2, 3, 3.2), 'skin_dark')
up('waist', 'hips', (0, -7, 0), (0, -13.5, -1.4), 2.6, 2.2, 'skin_ribs', extend=0.5)
up('chest', 'waist', (0, -13.5, -1.4), (0, -21.5, -4.6), 6.2, 3.4, 'skin_chest')
M.cube_l('chest', (-1.6, -7.4, -2.05), (3.2, 6.2, 0.6), 'sternum')
M.cube_l('chest', (-3.9, -8.6, -1.3), (7.8, 2.0, 2.6), 'skin_dark')            # bony shoulder bar
up('neck', 'chest', (0, -21.3, -4.6), (0, -23.4, -6.9), 1.5, 1.5, 'skin', extend=0.3)
local('head', 'neck', (0, -2.9, 0), (-38, 0, 0))
M.cube_l('head', (-1.7, -3.6, -2.0), (3.4, 3.8, 3.4), 'head')
M.cube_l('head', (-1.2, -0.9, -2.3), (2.4, 1.3, 1.6), 'jaw')
local('hat', 'head', (0, -3.4, 0), (6, 0, -7))
M.cube_l('hat', (-2.2, -2.7, -2.2), (4.4, 2.6, 4.4), 'hat_crown')
M.cube_l('hat', (-2.35, -0.95, -2.35), (4.7, 0.8, 4.7), 'hat_band')
M.cube_l('hat', (-6.2, -0.35, -4.3), (12.4, 0.5, 8.6), 'hat_brim')
M.cube_l('hat', (-4.3, -0.37, -6.2), (8.6, 0.5, 12.4), 'hat_brim')
M.cube_l('hat', (-5.1, -0.36, -5.1), (10.2, 0.5, 10.2), 'hat_brim')

for s, side in ((-1, 'right'), (1, 'left')):
    X = lambda x: s * x
    # legs
    hip, knee, ankle = (X(1.5), -6, 0), (X(2.4), 9, -1.6), (X(2.0), 22.2, 0.7)
    M.seg(f'{side}_thigh', 'hips', hip, knee, 1.8, 1.8, 'skin_leg', extend=0.3)
    M.cube_l(f'{side}_thigh', (-1.1, 13.6, -1.2), (2.2, 2.4, 2.3), 'skin_joint')
    M.seg(f'{side}_shin', f'{side}_thigh', knee, ankle, 1.5, 1.5, 'skin_leg', extend=0.2)
    M.seg(f'{side}_foot', f'{side}_shin', ankle, (X(2.0), 23.0, -3.6), 1.8, 1.6, 'skin_dark')
    M.cube_l(f'{side}_foot', (-1.15, -0.6, -1.0), (2.3, 4.8, 1.9), 'shoe')
    # arms
    sh, el, wr, pa = (X(3.5), -20.0, -3.6), (X(5.3), -7.5, -2.6), (X(5.8), 5.5, -4.6), (X(5.9), 8.4, -5.0)
    M.seg(f'{side}_upper', 'chest', sh, el, 1.6, 1.6, 'skin_arm', extend=0.4)
    M.cube_l(f'{side}_upper', (-1.05, 11.6, -1.05), (2.1, 2.0, 2.1), 'skin_joint')
    M.seg(f'{side}_fore', f'{side}_upper', el, wr, 1.3, 1.3, 'skin_arm', extend=0.2)
    M.seg(f'{side}_hand', f'{side}_fore', wr, pa, 2.0, 0.8, 'skin_dark', extend=0.2)
    for i, dx in enumerate((-1.25, -0.42, 0.42, 1.25)):
        k1 = (pa[0] + X(dx) * 1.3, pa[1] + 4.0, pa[2] - 0.6 + abs(dx) * 0.25)
        k2 = (pa[0] + X(dx) * 1.9, pa[1] + 8.2 - abs(dx) * 0.9, pa[2] - 1.5 + abs(dx) * 0.3)
        M.seg(f'{side}_f{i}a', f'{side}_hand', (pa[0] + X(dx) * 0.8, pa[1] - 0.2, pa[2]), k1, 0.45, 0.45, 'finger', extend=0.1)
        M.seg(f'{side}_f{i}b', f'{side}_f{i}a', k1, k2, 0.4, 0.4, 'finger_tip', extend=0.05)
    tb = (pa[0] - X(1.1), pa[1] - 1.0, pa[2] - 0.4)
    tk = (pa[0] - X(2.6), pa[1] + 2.4, pa[2] - 1.4)
    M.seg(f'{side}_thumb_a', f'{side}_hand', tb, tk, 0.45, 0.45, 'finger', extend=0.1)
    M.seg(f'{side}_thumb_b', f'{side}_thumb_a', tk, (pa[0] - X(3.0), pa[1] + 5.6, pa[2] - 2.2), 0.4, 0.4, 'finger_tip')

# ---------------- textures ----------------
P = Painter(M)
DARK, MID, HI = (14, 11, 10), (38, 30, 27), (88, 74, 66)


def skin(c, face, W, H, r, lo=DARK, mid=MID, hi=HI, ribs=False, joint=False, sheen=1.0):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 2.5)
    t = np.clip(0.25 + (n - 0.5) * 0.9, 0, 1)
    col = lerp(lo, mid, t)
    # glossy wet sheen along one edge column (looks like the slick skin in the photo)
    if face in ('north', 'west', 'top') and W > 1:
        k = (xx == (1 if W > 2 else 0)) & (r.random((H, W)) < 0.75)
        col[k] = lerp(col[k], hi, 0.6 * sheen)
    if ribs and face in ('north', 'west', 'east'):
        col[(yy % 2 == 0)] = lerp(col[(yy % 2 == 0)], hi, 0.28)
    if joint:
        col = lerp(col, (70, 58, 52), 0.35 * (n > 0.5))
    if face == 'bottom':
        col *= 0.6
    return col, np.ones((H, W)), None


P.mats['skin'] = skin
P.mats['skin_arm'] = skin
P.mats['skin_leg'] = lambda c, f, W, H, r: skin(c, f, W, H, r, sheen=1.2)
P.mats['skin_dark'] = lambda c, f, W, H, r: skin(c, f, W, H, r, lo=(8, 7, 7), mid=(26, 21, 19), sheen=0.6)
P.mats['skin_ribs'] = lambda c, f, W, H, r: skin(c, f, W, H, r, ribs=True)
P.mats['skin_joint'] = lambda c, f, W, H, r: skin(c, f, W, H, r, joint=True, sheen=1.4)
P.mats['finger'] = lambda c, f, W, H, r: skin(c, f, W, H, r, lo=(10, 8, 8), mid=(30, 24, 22), sheen=1.5)


@P.mat('finger_tip')
def finger_tip(c, face, W, H, r):
    col, a, _ = skin(c, face, W, H, r, lo=(10, 8, 8), mid=(30, 24, 22), sheen=1.5)
    col[-2:] = (150, 138, 120)       # pale claw tips
    col[-1:] = (196, 186, 166)
    return col, a, None


@P.mat('skin_chest')
def skin_chest(c, face, W, H, r):
    col, a, _ = skin(c, face, W, H, r)
    yy, xx = np.mgrid[0:H, 0:W]
    if face in ('north', 'west', 'east'):
        rib = (yy % 2 == 1) & (yy > H * 0.25)
        col[rib] = lerp(col[rib], (96, 82, 72), 0.35)
    if face == 'south':  # visible spine knobs
        col[(xx == W // 2) & (yy % 2 == 0)] = (84, 72, 64)
    return col, a, None


@P.mat('sternum')
def sternum(c, face, W, H, r):
    # the pale, greyish ribcage patch on the creature's chest
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 1.5)
    col = lerp((120, 112, 104), (196, 190, 182), np.clip(n * 1.1, 0, 1))
    col[(yy % 2 == 1)] = lerp(col[(yy % 2 == 1)], (70, 62, 58), 0.45)
    edge = (xx == 0) | (xx == W - 1)
    col[edge] = lerp(col[edge], MID, 0.6)
    return col, np.ones((H, W)), None


@P.mat('head')
def head(c, face, W, H, r):
    col, a, _ = skin(c, face, W, H, r, lo=(6, 5, 5), mid=(22, 18, 16), sheen=0.5)
    if face == 'north':
        # barely visible, sunken pinprick eyes
        col[1, 0] = (52, 46, 40); col[1, W - 1] = (52, 46, 40)
    return col, a, None


@P.mat('jaw')
def jaw(c, face, W, H, r):
    col, a, _ = skin(c, face, W, H, r, lo=(6, 5, 5), mid=(20, 16, 14), sheen=0.4)
    if face == 'north':
        col[H - 1] = (90, 82, 70)
        col[H - 1, ::2] = (20, 14, 12)
    return col, a, None


def straw(c, face, W, H, r, base=(176, 152, 112), dark=(120, 98, 68), weave=True):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 1.7)
    col = lerp(dark, base, np.clip(0.45 + (n - 0.5) * 1.2, 0, 1))
    if weave:
        k = ((xx + yy) % 3 == 0)
        col[k] = lerp(col[k], dark, 0.35)
        k2 = ((xx - yy) % 5 == 0)
        col[k2] = lerp(col[k2], (214, 196, 160), 0.3)
    return col


@P.mat('hat_brim')
def hat_brim(c, face, W, H, r):
    col = straw(c, face, W, H, r)
    a = np.ones((H, W))
    if face in ('top', 'bottom'):
        yy, xx = np.mgrid[0:H, 0:W]
        # rounded silhouette - the three overlapping slabs form a disc
        cx, cy = (W - 1) / 2, (H - 1) / 2
        rad = np.hypot((xx - cx) / (W / 2), (yy - cy) / (H / 2))
        a = (rad <= 1.04).astype(float)
        rim = (rad > 0.86) & (rad <= 1.04)
        col[rim] = lerp(col[rim], (132, 108, 74), 0.55)
        if face == 'bottom':
            col *= 0.55
    return col, a, None


@P.mat('hat_crown')
def hat_crown(c, face, W, H, r):
    col = straw(c, face, W, H, r, base=(190, 168, 128))
    if face == 'top':
        col = lerp(col, (120, 96, 64), 0.25)
    return col, np.ones((H, W)), None


@P.mat('hat_band')
def hat_band(c, face, W, H, r):
    n = noise2(H, W, r, 2)
    col = lerp((22, 16, 14), (60, 44, 34), n)
    return col, np.ones((H, W)), None


@P.mat('shoe')
def shoe(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 2)
    col = lerp((58, 40, 26), (122, 88, 56), np.clip(n * 1.2, 0, 1))
    if face in ('west', 'east', 'north', 'south'):
        col[-1:] = (30, 22, 16)
    return col, np.ones((H, W)), None
