"""Aigoar — abyssal tide lord the player transforms into (navy body, silver spiked armour, cyan liquid claws,
spiked crown, glowing chest core, long split coat tails and a living water serpent)."""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, noise2, lerp, border, glow_img, draw_line
from animlib import add_rot, pose_seq

M = Model('aigoar', 512, 128)
R0 = np.eye(3)


def local_bone(name, parent, off, rot=(0, 0, 0)):
    M.bone(name, parent, (0, 0, 0))
    M.bones[name].off = [float(v) for v in off]
    M.bones[name].rot = [float(v) for v in rot]
    return M.bones[name]


def spike(name, parent, A, B, w, mat, d=None):
    """tapered spike: seg bone with three stacked cubes of shrinking width"""
    d = w if d is None else d
    L = M.seg(name, parent, A, B, w, d, mat, extend=0.3)
    M.cube_l(name, (-w * 0.32, L * 0.55, -d * 0.32), (w * 0.64, L * 0.3, d * 0.64), mat)
    M.cube_l(name, (-w * 0.15, L * 0.85, -d * 0.15), (w * 0.3, L * 0.25, d * 0.3), mat + '_tip' if mat + '_tip' in TIPS else mat)
    # first cube only spans half the length
    M.bones[name].cubes[0].size[1] = L * 0.58
    return L


TIPS = {'crown_tip', 'silver_tip', 'fin_tip'}


def chain(prefix, parent, pts, widths, mat, depth_ratio=1.0, ext=0.6):
    p = parent
    for i in range(len(pts) - 1):
        n = f'{prefix}{i}'
        w = widths[min(i, len(widths) - 1)]
        M.seg(n, p, pts[i], pts[i + 1], w, w * depth_ratio, mat, extend=ext)
        p = n
    return p


# ---------------- skeleton ----------------
M.bone('base', None, (0, -24, 0))
M.bone('hips', 'base', (0, -24, 0))
M.cube('hips', (-4.5, -28, -3), (9, 6, 6), 'navy_plate')
M.cube('hips', (-5, -29.5, -3.4), (10, 2.6, 6.8), 'silver')
M.cube('hips', (-3.2, -27, -3.8), (6.4, 5, 0.6), 'silver_v')          # pointed front tasset
M.bone('waist', 'hips', (0, -28, 0))
M.cube('waist', (-3.2, -36.5, -2.6), (6.4, 9, 5.2), 'navy_rib')
M.bone('chest', 'waist', (0, -36, 0))
M.cube('chest', (-4.8, -42.5, -3.3), (9.6, 7, 6.6), 'navy_plate')
M.cube('chest', (-7.6, -52, -4.2), (15.2, 10, 8.4), 'chest_armor')
M.cube('chest', (-5.6, -51.5, -4.9), (11.2, 17, 0.8), 'core')
M.cube('chest', (-4.2, -53.5, -3), (8.4, 2.6, 6), 'navy')
# back fins (cyan blades rising behind the shoulders)
for side, sx in (('right', -1), ('left', 1)):
    spike(f'{side}_fin', 'chest', (sx * 3.6, -49, 3.8), (sx * 12.5, -71, 8.5), 6.8, 'fin', d=0.9)
    spike(f'{side}_fin2', 'chest', (sx * 5, -46, 3.6), (sx * 17, -60, 6.5), 4.6, 'fin', d=0.8)

M.bone('neck', 'chest', (0, -52.5, 0))
M.cube('neck', (-1.5, -56.5, -1.6), (3, 5, 3.2), 'navy')
M.bone('head', 'neck', (0, -56, 0))
M.cube('head', (-3.2, -63, -3.3), (6.4, 7.2, 6.6), 'helm')
M.cube('head', (-2.9, -62.2, -3.9), (5.8, 5.8, 0.7), 'face')
M.cube('head', (-1.9, -56.6, -3.4), (3.8, 1.6, 4.4), 'helm')
M.cube('head', (-0.75, -64.2, -4.2), (1.5, 2.4, 0.7), 'gem')
for side, sx in (('right', -1), ('left', 1)):
    spike(f'{side}_cheek', 'head', (sx * 3.1, -59.5, -1), (sx * 5.5, -62.5, 5.5), 1.4, 'crown')
# spiked crown, swept up and back like flames
CROWN = [(('c0'), (0, -62.8, -1.2), (0, -76, 0.5), 2.4),
         (('c1r'), (-1.9, -62.8, -0.6), (-4.8, -74, 2), 2.0), (('c1l'), (1.9, -62.8, -0.6), (4.8, -74, 2), 2.0),
         (('c2r'), (-2.8, -62.3, 0.2), (-9, -71.5, 3.5), 1.8), (('c2l'), (2.8, -62.3, 0.2), (9, -71.5, 3.5), 1.8),
         (('c3r'), (-3.1, -61.2, 1.2), (-11.5, -66, 4.5), 1.5), (('c3l'), (3.1, -61.2, 1.2), (11.5, -66, 4.5), 1.5),
         (('c4'), (0, -62, 2.4), (0, -71, 10), 2.0),
         (('c5r'), (-2, -61, 3), (-5.5, -66, 10.5), 1.6), (('c5l'), (2, -61, 3), (5.5, -66, 10.5), 1.6)]
for n, A, B, w in CROWN:
    spike('crown_' + n, 'head', A, B, w, 'crown')

# arms: navy upper arm, silver spiked pauldron, forearm bursting into liquid, long liquid claws
for side, sx in (('right', -1), ('left', 1)):
    A, B, C = (sx * 8.4, -49.5, 0), (sx * 12.3, -35.5, 1.5), (sx * 15.2, -23, -0.5)
    M.seg(f'{side}_arm', 'chest', A, B, 3.8, 4.0, 'navy_arm', extend=1.5)
    pd = f'{side}_pauldron'
    M.bone_w(pd, f'{side}_arm', (sx * 9.5, -50, 0), R0)
    M.cube_l(pd, (-4.8, -3.6, -4.6), (9.6, 5.8, 9.2), 'silver')
    M.cube_l(pd, (-3.6, 1.8, -3.6), (7.2, 2.2, 7.2), 'navy_plate')
    spike(f'{side}_ps0', pd, (sx * 12.5, -51.5, 0), (sx * 23.5, -56.5, 0.5), 5.0, 'silver', d=2.0)
    spike(f'{side}_ps1', pd, (sx * 10.5, -52.5, -1.2), (sx * 18, -62, 1), 4.0, 'silver', d=1.8)
    spike(f'{side}_ps2', pd, (sx * 8.5, -53, 1.2), (sx * 11.5, -64.5, 3.5), 3.0, 'silver', d=1.6)
    spike(f'{side}_ps3', pd, (sx * 13, -48.5, 0.5), (sx * 17.5, -44, 1.5), 2.0, 'silver')
    L = M.seg(f'{side}_fore', f'{side}_arm', B, C, 3.2, 3.4, 'navy_arm', extend=1.0)
    M.cube_l(f'{side}_fore', (-2.9, L * 0.2, -2.9), (5.8, L * 0.85, 5.8), 'water_sleeve')
    spike(f'{side}_elbow', f'{side}_arm', (sx * 12.5, -36.5, 2.5), (sx * 15.5, -40, 7.5), 1.7, 'silver')
    for k in range(3):
        n = f'{side}_flick{k}'
        local_bone(n, f'{side}_fore', (0, L * (0.35 + k * 0.22), 0), (0, k * 120 + 20, sx * (55 + k * 10)))
        M.cube_l(n, (-2.5, -6, 0), (5, 6, 0), 'splash', plane=True)
    h = f'{side}_hand'
    local_bone(h, f'{side}_fore', (0, L, 0))
    M.cube_l(h, (-2.1, -0.3, -2.5), (4.2, 4.2, 5.0), 'water')
    for i, z in enumerate((-1.9, -0.6, 0.7, 1.9)):
        fa, fb = f'{side}_f{i}a', f'{side}_f{i}b'
        local_bone(fa, h, (0, 3.6, z), (z * 6, 0, sx * -8))
        M.cube_l(fa, (-0.65, 0, -0.65), (1.3, 4.6 + (1 if i in (1, 2) else 0), 1.3), 'water')
        local_bone(fb, fa, (0, 4.6 + (1 if i in (1, 2) else 0), 0), (0, 0, sx * -18))
        M.cube_l(fb, (-0.5, 0, -0.5), (1.0, 4.6, 1.0), 'claw')
    local_bone(f'{side}_thumb', h, (sx * -1.8, 2.2, -1.6), (-30, 0, sx * -40))
    M.cube_l(f'{side}_thumb', (-0.55, 0, -0.55), (1.1, 4.2, 1.1), 'claw')

# long legs: navy thighs, spiked silver knees, silver greaves melting into liquid clawed feet
for side, sx in (('right', -1), ('left', 1)):
    A, B, C = (sx * 3.3, -24.5, 0), (sx * 4.8, -3, -1.5), (sx * 6.2, 17, 0.8)
    M.seg(f'{side}_leg', 'hips', A, B, 4.4, 5.0, 'navy_leg', extend=1.5)
    L = M.seg(f'{side}_shin', f'{side}_leg', B, C, 3.8, 4.2, 'greave', extend=1.0)
    M.cube_l(f'{side}_shin', (-2.5, L * 0.62, -2.6), (5.0, L * 0.42, 5.2), 'water_sleeve')
    kn = f'{side}_knee'
    M.bone_w(kn, f'{side}_shin', B, R0)
    M.cube_l(kn, (-2.7, -2.6, -3.6), (5.4, 5.6, 2.6), 'silver')
    spike(f'{side}_ks0', kn, (sx * 4.8, -4.5, -3.2), (sx * 5.4, -11, -6.2), 2.2, 'silver')
    spike(f'{side}_ks1', kn, (sx * 6.8, -2.5, -1.8), (sx * 10, -6.5, -0.5), 1.7, 'silver')
    ft = f'{side}_foot'
    M.bone_w(ft, f'{side}_shin', C, R0)
    M.cube_l(ft, (-2.4, 0, -2.4), (4.8, 4.2, 4.6), 'water')
    M.cube_l(ft, (-2.9, 4.2, -5.5), (5.8, 2.8, 8), 'water')
    for i, tx in enumerate((-1.9, 0, 1.9)):
        spike(f'{side}_toe{i}', ft, (sx * 6.2 + tx, 22.5, -5), (sx * 6.2 + tx * 1.5, 24, -9.5), 1.3, 'claw')
    spike(f'{side}_heel', ft, (sx * 6.2, 21, 2.5), (sx * 6.2, 23.5, 6), 1.3, 'claw')
    for k in range(3):
        n = f'{side}_fsplash{k}'
        local_bone(n, ft, (sx * 1.5 * (k - 1), 7, -1 + k), (-15, k * 60 + sx * 25, 0))
        M.cube_l(n, (-3, -5, 0), (6, 5, 0), 'splash', plane=True)

# split coat tails (upper + lower segment each so they can wave)
COAT = [('coat_fr', (-4.0, -26.8, -3.2), (-7, 32, 6), (-7, 7)), ('coat_fl', (4.0, -26.8, -3.2), (-7, -32, -6), (0, 7)),
        ('coat_sr', (-4.9, -26.8, 0), (0, 90, 11), (-4, 8)), ('coat_sl', (4.9, -26.8, 0), (0, -90, -11), (-4, 8)),
        ('coat_br', (-2.6, -26.8, 3.5), (11, -14, 6), (-7.4, 7.4)), ('coat_bl', (2.6, -26.8, 3.5), (11, 14, -6), (0, 7.4))]
for n, piv, rot, (x0, w) in COAT:
    M.bone(n, 'hips', piv, rot)
    M.cube_l(n, (x0, 0, 0), (w, 22, 0), 'coat_up', plane=True)
    local_bone(n + '2', n, (0, 22, 0))
    M.cube_l(n + '2', (x0, 0, 0), (w, 25, 0), 'coat_lo', plane=True)

# living water serpent coiling from the lower back around the legs down to the ground
SERP = [(0, -25, 4), (-6, -20, 10), (-13, -13, 12.5), (-18, -4, 9.5), (-19.5, 5, 3.5), (-15, 12, 5.5), (-6, 16.5, 10.5),
        (4, 18.5, 11), (12.5, 19, 6.5), (18.5, 16.5, 0.5), (22, 11, -3.5)]
chain('serp', 'hips', SERP, (3.8, 3.6, 3.4, 3.1, 2.9, 2.7, 2.5, 2.2, 1.9, 1.5), 'water', depth_ratio=0.8, ext=0.9)
for i in (2, 5, 8):
    n = f'serpfin{i}'
    local_bone(n, f'serp{i}', (0, 2, 0), (0, 90, 70))
    M.cube_l(n, (-3, -5, 0), (6, 5, 0), 'splash', plane=True)

# floating droplets (their own root so body sway doesn't move them)
M.bone('fx', None, (0, 24, 0))
rs = np.random.default_rng(11)
DROPS = [(-17, -44, -3), (19, -40, 2), (-22, -26, 4), (23, -18, -2), (-12, -62, 6), (14, -58, -4), (-25, -6, -1),
         (26, 2, 3), (-9, 8, -9), (10, -8, -10), (0, -70, 5), (-20, 14, -5)]
for i, p in enumerate(DROPS):
    s = 0.8 + rs.random() * 0.9
    M.bone(f'drop{i}', 'fx', p)
    M.cube_l(f'drop{i}', (-s / 2, -s / 2, -s / 2), (s, s, s), 'drop')

# ---------------- textures ----------------
P = Painter(M)
INK = (12, 14, 28)
NAVY = ((18, 22, 42), (38, 49, 80), (74, 92, 132))
SILV = ((88, 94, 108), (160, 167, 178), (222, 226, 232))
CYAN = ((8, 104, 128), (42, 206, 214), (170, 255, 250))
GLOWC = (60, 255, 245)


def ramp(t, pal):
    lo, mid, hi = pal
    t = np.clip(t, 0, 1)
    return np.where(t[..., None] < 0.5, lerp(lo, mid, t * 2), lerp(mid, hi, (t - 0.5) * 2))


def shade_face(face, t):
    if face == 'top':
        return np.clip(t + 0.12, 0, 1)
    if face == 'bottom':
        return t * 0.55
    return t


def navy(c, face, W, H, r, pal=NAVY, streak=True, vgrad=0.2):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 4)
    t = 0.48 + (n - 0.5) * 0.5 - vgrad * (yy / max(H - 1, 1) - 0.5)
    if streak:
        wav = noise2(H, W, r, 7, 1)
        st = 0.5 + 0.5 * np.sin(xx * 1.3 + wav * 6)
        t = t + (st > 0.93) * 0.22
    t = shade_face(face, t)
    col = ramp(t, pal)
    b = border(H, W)
    col[b] = lerp(col[b], INK, 0.55)
    return col, np.ones((H, W)), None


def organic(H, W, r, scale=3.0, thr=0.55):
    n = noise2(H, W, r, scale, 2)
    return n > thr, n


def silver(c, face, W, H, r, holes=0.34, tip=False):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 6)
    t = 0.7 - 0.3 * (yy / max(H - 1, 1)) + (n - 0.5) * 0.25
    t = shade_face(face, t)
    col = ramp(t, SILV)
    # navy tendril holes like the ink work on the armour
    hole, hn = organic(H, W, r, 5.0, 1 - holes * 0.8)
    col[hole] = lerp(col[hole], NAVY[1], 0.9)
    edge = np.zeros_like(hole)
    edge[1:] |= hole[:-1] & ~hole[1:]; edge[:-1] |= hole[1:] & ~hole[:-1]
    edge[:, 1:] |= hole[:, :-1] & ~hole[:, 1:]; edge[:, :-1] |= hole[:, 1:] & ~hole[:, :-1]
    col[edge] = lerp(col[edge], INK, 0.6)
    col[0, :] = lerp(col[0, :], SILV[2], 0.5)
    b = border(H, W)
    col[b] = lerp(col[b], INK, 0.45)
    return col, np.ones((H, W)), None


P.mats['navy'] = navy
P.mats['navy_arm'] = lambda c, f, W, H, r: navy(c, f, W, H, r, vgrad=0.35)
P.mats['navy_leg'] = lambda c, f, W, H, r: navy(c, f, W, H, r, vgrad=0.3)
P.mats['helm'] = lambda c, f, W, H, r: navy(c, f, W, H, r, streak=False, vgrad=0.4)
P.mats['silver'] = silver
P.mats['silver_tip'] = lambda c, f, W, H, r: silver(c, f, W, H, r, holes=0.15)


@P.mat('navy_plate')
def navy_plate(c, face, W, H, r):
    col, a, _ = navy(c, face, W, H, r, streak=False)
    m, n = organic(H, W, r, 4.5, 0.6)
    sc, _, _ = silver(c, face, W, H, r, holes=0.0)
    col[m] = sc[m]
    return col, a, None


@P.mat('greave')
def greave(c, face, W, H, r):
    col, a, _ = navy(c, face, W, H, r, streak=False)
    yy, xx = np.mgrid[0:H, 0:W]
    m, n = organic(H, W, r, 4.0, 0.5)
    m &= (yy < H * 0.7)
    if face == 'north':
        m |= (np.abs(xx - (W - 1) / 2) < 1.2) & (yy < H * 0.6)
    sc, _, _ = silver(c, face, W, H, r, holes=0.0)
    col[m] = sc[m]
    return col, a, None


@P.mat('navy_rib')
def navy_rib(c, face, W, H, r):
    col, a, _ = navy(c, face, W, H, r, streak=False)
    yy, xx = np.mgrid[0:H, 0:W]
    if face in ('north', 'west', 'east'):
        rib = (yy % 3 == 1)
        sc = ramp(0.55 + 0 * yy, SILV)
        col[rib] = lerp(col[rib], sc[rib], 0.55)
    return col, a, None


@P.mat('chest_armor')
def chest_armor(c, face, W, H, r):
    col, a, _ = navy(c, face, W, H, r, streak=False)
    yy, xx = np.mgrid[0:H, 0:W]
    cx = (W - 1) / 2
    m, _ = organic(H, W, r, 4.5, 0.5)
    if face == 'north':
        # silver pectoral shells framing the core
        shell = (np.abs(xx - cx) > W * 0.3) & (yy < H * 0.85)
        m |= shell
    sc, _, _ = silver(c, face, W, H, r, holes=0.12)
    col[m] = sc[m]
    return col, a, None


@P.mat('silver_v')
def silver_v(c, face, W, H, r):
    col, a, _ = silver(c, face, W, H, r, holes=0.2)
    yy, xx = np.mgrid[0:H, 0:W]
    if face in ('north', 'south'):
        cx = (W - 1) / 2
        a = (np.abs(xx - cx) <= (W / 2) * (1 - yy / H * 0.85)).astype(float)
    return col, a, None


def water(c, face, W, H, r, glow=0.33, bright=0.0):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 5)
    sw = np.sin(xx * 0.7 + yy * 0.45 + n * 6)
    t = 0.5 + (n - 0.5) * 0.6 + sw * 0.18 + bright
    t = shade_face(face, t)
    col = ramp(t, CYAN)
    hl = sw > 0.93
    col[hl] = lerp(col[hl], (235, 255, 255), 0.7)
    spec = r.random((H, W)) > 0.985
    col[spec] = (245, 255, 255)
    g = glow_img(H, W, GLOWC, glow + hl * 0.45 + spec * 0.5)
    return col, np.ones((H, W)), g


P.mats['water'] = water
P.mats['drop'] = lambda c, f, W, H, r: water(c, f, W, H, r, glow=0.8, bright=0.25)


@P.mat('water_sleeve')
def water_sleeve(c, face, W, H, r):
    col, a, g = water(c, face, W, H, r)
    if face in ('north', 'south', 'west', 'east'):
        a = a.copy()
        v = 0.0
        for x in range(W):
            v = np.clip(v + r.normal(0, 1.0), 0, H * 0.3)
            cut = int(v)
            a[:cut, x] = 0
            g[:cut, x, 3] = 0
            if cut < H:
                col[cut, x] = (200, 255, 255)
    elif face == 'top':
        a = np.zeros((H, W))
        g[..., 3] = 0
    return col, a, g


@P.mat('claw')
def claw(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    t = 0.55 + 0.45 * yy / max(H - 1, 1)
    col = lerp(CYAN[1], (220, 255, 255), t)
    col[:, 0] = lerp(col[:, 0], CYAN[0], 0.4)
    g = glow_img(H, W, GLOWC, 0.35 + 0.5 * t)
    return col, np.ones((H, W)), g


@P.mat('gem')
def gem(c, face, W, H, r):
    col = np.zeros((H, W, 3)) + (120, 255, 250)
    col[0] = (230, 255, 255)
    return col, np.ones((H, W)), glow_img(H, W, GLOWC, np.ones((H, W)))


def crown(c, face, W, H, r, tip=False):
    col, a, _ = navy(c, face, W, H, r, streak=False, vgrad=-0.2)
    yy, xx = np.mgrid[0:H, 0:W]
    g = np.zeros((H, W, 4), np.float32)
    if face in ('north', 'east', 'west', 'south'):
        edge = (xx == 0) if face in ('north', 'east') else (xx == W - 1)
        col[edge] = lerp(col[edge], CYAN[1], 0.75)
        g = glow_img(H, W, GLOWC, edge * 0.6)
    if tip:
        col = lerp(col, CYAN[1], 0.25)
    return col, a, g


P.mats['crown'] = crown
P.mats['crown_tip'] = lambda c, f, W, H, r: crown(c, f, W, H, r, True)


def fin(c, face, W, H, r, tip=False):
    yy, xx = np.mgrid[0:H, 0:W]
    v = yy / max(H - 1, 1)
    t = 0.45 + 0.4 * v + (noise2(H, W, r, 3) - 0.5) * 0.3
    col = ramp(t, CYAN)
    vein = np.abs(xx - (W - 1) / 2) < 0.6
    col[vein] = lerp(col[vein], (225, 255, 255), 0.6)
    a = np.ones((H, W))
    if face in ('north', 'south') and W > 2:
        cx = (W - 1) / 2
        prof = 1 - 0.55 * v if not tip else 0.45 * (1 - v)
        a = (np.abs(xx - cx) <= (W / 2) * prof + 0.2).astype(float)
        edge = a.astype(bool) & ~((np.abs(xx - cx) <= (W / 2) * prof - 0.8))
        col[edge] = lerp(col[edge], NAVY[0], 0.5)
    g = glow_img(H, W, GLOWC, 0.3 + vein * 0.5)
    return col, a, g


P.mats['fin'] = fin
P.mats['fin_tip'] = lambda c, f, W, H, r: fin(c, f, W, H, r, True)


@P.mat('core')
def core(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    cx = (W - 1) / 2
    v = yy / max(H - 1, 1)
    a = np.ones((H, W))
    col = ramp(0.55 + 0.35 * (1 - v) + (noise2(H, W, r, 2) - 0.5) * 0.2, CYAN)
    if face in ('north', 'south'):
        inside = np.abs(xx - cx) <= (W / 2) * (1 - v * 0.92)
        a = inside.astype(float)
        rib = (yy % 2 == 1) & inside
        col[rib] = lerp(col[rib], CYAN[0], 0.55)
        mid = np.abs(xx - cx) < 0.6
        col[mid & inside] = lerp(col[mid & inside], (230, 255, 255), 0.6)
        rim = inside & ~(np.abs(xx - cx) <= (W / 2) * (1 - v * 0.92) - 1.0)
        col[rim] = SILV[1]
        glow = inside * (0.55 + 0.35 * (1 - v)) * ~rim
    else:
        glow = np.zeros((H, W))
    return col, a, glow_img(H, W, GLOWC, glow)


@P.mat('face')
def face_m(c, face, W, H, r):
    col = np.zeros((H, W, 3)) + (16, 19, 32)
    col[0] = NAVY[1]
    g = np.zeros((H, W, 4), np.float32)
    if face != 'north':
        return col, np.ones((H, W)), None
    m = np.zeros((H, W))
    # slanted V eye slits
    draw_line(m, 0, 1, W // 2 - 1, 2)
    draw_line(m, W - 1, 1, W // 2, 2)
    col[m > 0] = (150, 255, 250)
    # silver cheek plates
    col[H - 2:, :] = lerp(col[H - 2:, :], SILV[1], 0.6)
    col[3:, 0] = SILV[0]; col[3:, W - 1] = SILV[0]
    return col, np.ones((H, W)), glow_img(H, W, GLOWC, m)


def coat(c, face, W, H, r, lower=False):
    yy, xx = np.mgrid[0:H, 0:W]
    v = yy / max(H - 1, 1)
    n = noise2(H, W, r, 4)
    vv = (v * 0.5 + 0.5) if lower else v * 0.5
    base = lerp(lerp(NAVY[0], NAVY[1], np.clip(n + 0.1, 0, 1)), (92, 128, 142), np.clip((vv - 0.45) * 1.8, 0, 1))
    folds = 0.5 + 0.5 * np.sin(xx * 1.1 + noise2(H, W, r, 6, 1) * 4)
    col = lerp(base * 0.8, base * 1.15, folds)
    a = np.ones((H, W))
    trim = (xx == 0) | (xx == W - 1)
    col[trim] = lerp(col[trim], SILV[1], 0.55)
    if not lower:
        m, _ = organic(H, W, r, 2.4, 0.66)
        m &= yy < H * 0.35
        col[m] = lerp(col[m], SILV[1], 0.8)
    else:
        # pointed ragged tip
        cx = (W - 1) / 2 + (r.random() - 0.5) * W * 0.3
        start = 0.35
        prof = np.clip((1 - v) / (1 - start), 0, 1)
        jag = (noise2(H, W, r, 2, 1) - 0.5) * 1.2
        a = ((v < start) | (np.abs(xx - cx) <= (W / 2) * prof + jag)).astype(float)
        edge = a.astype(bool) & (v > start) & ~(np.abs(xx - cx) <= (W / 2) * prof + jag - 1)
        col[edge] = lerp(col[edge], (150, 190, 200), 0.5)
    return col, a, None


P.mats['coat_up'] = coat
P.mats['coat_lo'] = lambda c, f, W, H, r: coat(c, f, W, H, r, True)


@P.mat('splash')
def splash(c, face, W, H, r):
    S = 3
    Hs, Ws = H * S, W * S
    yy, xx = np.mgrid[0:Hs, 0:Ws] / S
    u = (xx - W / 2) / (W / 2)
    v = 1 - yy / H
    wob = noise2(Hs, Ws, r, 5 * S, 2)
    top = 0.3 + 0.6 * np.clip(1 - np.abs(u + 0.3 * np.sin(v * 3)) ** 1.5, 0, 1) + (wob - 0.5) * 0.35
    inside = (v < top) & (np.abs(u) < 0.95 - v * 0.3)
    for _ in range(4):
        cx, cy = r.random() * W, H * (0.05 + r.random() * 0.3)
        inside |= np.hypot(xx - cx, yy - cy) < 0.5 + r.random() * 0.7
    col = lerp(CYAN[0], CYAN[2], np.clip(v * 1.2, 0, 1))
    hi = np.abs(v - top + 0.06) < 0.05
    col[hi] = (230, 255, 255)
    col = col.reshape(H, S, W, S, 3).mean((1, 3))
    alpha = (inside.astype(float).reshape(H, S, W, S).mean((1, 3)) > 0.45).astype(float)
    return col, alpha, glow_img(H, W, GLOWC, alpha * 0.45)


# ---------------- animations ----------------
A = {}
SIDES = ('right', 'left')
ARM = [f'{s}_{p}' for s in SIDES for p in ('arm', 'fore', 'hand')]
LEG = [f'{s}_{p}' for s in SIDES for p in ('leg', 'shin', 'foot')]
LIMBS = ARM + LEG + ['chest', 'waist', 'head', 'hips', 'neck', 'base']
COATS = [n for n, *_ in COAT]
NSERP = len(SERP) - 1


def mirror(d):
    o = {}
    for k, v in d.items():
        if k.startswith('right_'):
            o['left_' + k[6:]] = (v[0], -v[1], -v[2])
        elif k.startswith('left_'):
            o['right_' + k[5:]] = (v[0], -v[1], -v[2])
        else:
            o[k] = (v[0], -v[1], -v[2])
    return o


def cyc(length, n=8):
    return [(length * k / n, math.tau * k / n) for k in range(n + 1)]


def coat_wave(a, length, amp=1.0, flare=0.0, speed=1, back=0.0):
    for i, n in enumerate(COATS):
        front = n in ('coat_fr', 'coat_fl')
        s = -1 if front else 1
        k1, k2 = [], []
        for t, ph in cyc(length):
            p = ph * speed + i * 1.1
            k1.append((t, (s * flare + back + 4 * amp * math.sin(p), 0, 2.5 * amp * math.cos(p))))
            k2.append((t, (s * flare * 0.4 + back * 0.6 + 7 * amp * math.sin(p - 1.2), 0, 3 * amp * math.cos(p - 1.0))))
        a.rot(n, *k1)
        a.rot(n + '2', *k2)


def serp_wave(a, length, amp=1.0, speed=1, lift=0.0):
    for i in range(NSERP):
        keys = []
        for t, ph in cyc(length):
            p = ph * speed - i * 0.75
            keys.append((t, (lift + 7 * amp * math.sin(p), 6 * amp * math.cos(p * 1.0 + 0.4), 5 * amp * math.sin(p + 1.1))))
        a.rot(f'serp{i}', *keys)


def fin_wave(a, length, amp=1.0, spread=0.0, speed=1):
    for s, sx in (('right', -1), ('left', 1)):
        for n, m in ((f'{s}_fin', 1.0), (f'{s}_fin2', 1.4)):
            keys = [(t, (-spread * 0.3 + 3 * amp * math.sin(ph * speed) * m, 0, sx * (spread + 3 * amp * math.cos(ph * speed + 0.5) * m))) for t, ph in cyc(length)]
            a.rot(n, *keys)


def flicks(a, length, amp=1.0, speed=1):
    for s in SIDES:
        for k in range(3):
            a.scl(f'{s}_flick{k}', *[(t, (1, 1 + 0.35 * amp * math.sin(ph * speed + k * 2), 1)) for t, ph in cyc(length)])
            for j in range(3):
                pass
        for k in range(3):
            a.scl(f'{s}_fsplash{k}', *[(t, (1, 1 + 0.3 * amp * math.sin(ph * speed + k * 2.2), 1)) for t, ph in cyc(length)])


def drops(a, length, amp=1.0, speed=1, spin=0.0):
    for i in range(len(DROPS)):
        keys = [(t, (0, 1.6 * amp * math.sin(ph * speed + i * 1.7), 0)) for t, ph in cyc(length)]
        a.pos(f'drop{i}', *keys)
        if spin:
            a.rot(f'drop{i}', (0, (0, 0, 0)), (length, (spin, spin * 1.3, 0)), interp='L')


def fingers(a, frames):
    """frames: [(t, curl_degrees)]; curls every claw finger on both hands"""
    for s, sx in (('right', -1), ('left', 1)):
        for i in range(4):
            a.rot(f'{s}_f{i}a', *[(t, (0, 0, sx * -c * 0.6)) for t, c in frames])
            a.rot(f'{s}_f{i}b', *[(t, (0, 0, sx * -c)) for t, c in frames])


def dressing(a, length, amp=1.0, speed=1, flare=0.0, back=0.0, fin_spread=0.0):
    coat_wave(a, length, amp, flare, speed, back)
    serp_wave(a, length, amp, speed)
    fin_wave(a, length, amp * 0.8, fin_spread, speed)
    flicks(a, length, amp, speed * 2)
    drops(a, length, 1.0, 1)


idle = Anim('idle', 4.0, True)
idle.pos('base', (0, (0, 0, 0)), (2, (0, -0.6, 0)), (4, (0, 0, 0)))
idle.rot('chest', (0, (0, 0, 0)), (2, (-2.5, 0, 0)), (4, (0, 0, 0)))
idle.rot('head', (0, (0, 0, 0)), (1.3, (3, -5, 2)), (2.7, (-2, 5, -2)), (4, (0, 0, 0)))
idle.rot('right_arm', (0, (0, 0, 0)), (2, (-3, 0, -3)), (4, (0, 0, 0)))
idle.rot('left_arm', (0, (0, 0, 0)), (2, (-3, 0, 3)), (4, (0, 0, 0)))
fingers(idle, [(0, 0), (1, 12), (2, 4), (3, 14), (4, 0)])
dressing(idle, 4.0, 0.7)
A['IDLE'] = idle


def walk_frame(s, big=1.0):
    return {'right_leg': (-28 * s * big, 0, 0), 'right_shin': (max(0, 36 * s) * big + 5, 0, 0),
            'left_leg': (28 * s * big, 0, 0), 'left_shin': (max(0, -36 * s) * big + 5, 0, 0),
            'right_arm': (20 * s * big, 0, 0), 'left_arm': (-20 * s * big, 0, 0),
            'right_fore': (-10 - 8 * s, 0, 0), 'left_fore': (-10 + 8 * s, 0, 0),
            'hips': (0, 7 * s, 0), 'chest': (3 * big, -9 * s, 0), 'head': (0, 6 * s, 0)}


walk = Anim('walk', 1.2, True)
add_rot(walk, [(0, walk_frame(1)), (0.3, walk_frame(0)), (0.6, walk_frame(-1)), (0.9, walk_frame(0)), (1.2, walk_frame(1))])
walk.pos('base', (0, (0, 0, 0)), (0.3, (0, 1.2, 0)), (0.6, (0, 0, 0)), (0.9, (0, 1.2, 0)), (1.2, (0, 0, 0)))
coat_wave(walk, 1.2, 1.4, 3, 2, back=4)
serp_wave(walk, 1.2, 1.2, 2)
fin_wave(walk, 1.2, 0.8, 0, 2)
flicks(walk, 1.2, 1.2, 2)
drops(walk, 1.2)
A['WALK'] = walk


def run_frame(s):
    f = walk_frame(s, 1.7)
    f.update({'base': (16, 0, 0), 'head': (-14, 8 * s, 0), 'chest': (6, -12 * s, 0),
              'right_arm': (55 + 25 * s, 0, -25), 'left_arm': (55 - 25 * s, 0, 25), 'right_fore': (-25, 0, 0), 'left_fore': (-25, 0, 0)})
    return f


run = Anim('run', 0.72, True)
add_rot(run, [(0, run_frame(1)), (0.18, run_frame(0)), (0.36, run_frame(-1)), (0.54, run_frame(0)), (0.72, run_frame(1))])
run.pos('base', (0, (0, 0, 0)), (0.18, (0, 2.2, 0)), (0.36, (0, 0, 0)), (0.54, (0, 2.2, 0)), (0.72, (0, 0, 0)))
coat_wave(run, 0.72, 1.8, 6, 2, back=38)
serp_wave(run, 0.72, 1.5, 2, lift=10)
fin_wave(run, 0.72, 1.0, 12, 2)
flicks(run, 0.72, 1.5, 2)
drops(run, 0.72)
A['RUN'] = run

jump = Anim('jump', 1.0, True)
jp = {'right_leg': (-45, 0, 8), 'right_shin': (70, 0, 0), 'left_leg': (-10, 0, -6), 'left_shin': (35, 0, 0),
      'right_arm': (-40, 0, 45), 'left_arm': (-40, 0, -45), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (8, 0, 0), 'head': (-10, 0, 0)}
jp2 = {k: (v[0] * 1.08, v[1], v[2] * 1.1) for k, v in jp.items()}
add_rot(jump, [(0, jp), (0.5, jp2), (1.0, jp)])
coat_wave(jump, 1.0, 1.5, 10, 2, back=-18)
serp_wave(jump, 1.0, 1.3, 2, lift=-8)
fin_wave(jump, 1.0, 1.0, 14, 2)
drops(jump, 1.0)
A['JUMP'] = jump

fall = Anim('fall', 1.0, True)
fp = {'right_leg': (-18, 0, 12), 'right_shin': (25, 0, 0), 'left_leg': (8, 0, -12), 'left_shin': (15, 0, 0),
      'right_arm': (-70, 0, 70), 'left_arm': (-70, 0, -70), 'right_fore': (-20, 0, 0), 'left_fore': (-20, 0, 0), 'chest': (-6, 0, 0), 'head': (12, 0, 0)}
fp2 = {k: (v[0] + 4, v[1], v[2] * 0.92) for k, v in fp.items()}
add_rot(fall, [(0, fp), (0.5, fp2), (1.0, fp)])
coat_wave(fall, 1.0, 2.2, -8, 3, back=-35)
serp_wave(fall, 1.0, 1.6, 3, lift=-15)
fin_wave(fall, 1.0, 1.4, 8, 3)
drops(fall, 1.0)
A['FALL'] = fall

crouch = Anim('crouch', 2.0, True)
cp = {'right_leg': (-38, 0, 6), 'right_shin': (60, 0, 0), 'left_leg': (-38, 0, -6), 'left_shin': (60, 0, 0), 'right_foot': (-22, 0, 0), 'left_foot': (-22, 0, 0),
      'chest': (22, 0, 0), 'waist': (10, 0, 0), 'head': (-25, 0, 0), 'right_arm': (-30, 0, -15), 'left_arm': (-30, 0, 15), 'right_fore': (-35, 0, 0), 'left_fore': (-35, 0, 0)}
add_rot(crouch, [(0, cp), (1.0, {k: (v[0] + 2, v[1], v[2]) for k, v in cp.items()}), (2.0, cp)])
crouch.pos('base', (0, (0, -7, -1)), (1, (0, -7.4, -1)), (2, (0, -7, -1)))
coat_wave(crouch, 2.0, 0.6, 12, 1)
serp_wave(crouch, 2.0, 0.6, 1)
A['CROUCH'] = crouch

swim = Anim('swim', 1.4, True)
sw = lambda s: {'right_arm': (-150 + 60 * s, 0, 30 - 20 * s), 'left_arm': (-150 - 60 * s, 0, -30 - 20 * s),
                'right_leg': (22 * s, 0, 0), 'left_leg': (-22 * s, 0, 0), 'right_shin': (20 + 15 * s, 0, 0), 'left_shin': (20 - 15 * s, 0, 0),
                'head': (-40, 0, 0), 'chest': (0, 8 * s, 0)}
add_rot(swim, [(0, sw(1)), (0.35, sw(0)), (0.7, sw(-1)), (1.05, sw(0)), (1.4, sw(1))])
coat_wave(swim, 1.4, 2.0, 0, 2, back=-50)
serp_wave(swim, 1.4, 2.0, 2)
fin_wave(swim, 1.4, 1.2, 10, 2)
drops(swim, 1.4)
A['SWIM'] = swim

# claw swipes for normal attacks
atk = Anim('attack_r', 0.5)
pose_seq(atk, [
    (0.0, {}),
    (0.1, {'right_arm': (-150, 20, -15), 'right_fore': (-30, 0, 0), 'chest': (-4, 28, 0), 'waist': (0, 10, 0), 'left_arm': (10, 0, 12)}),
    (0.22, {'right_arm': (-45, -55, 15), 'right_fore': (-5, 0, 0), 'chest': (10, -30, 0), 'waist': (4, -12, 0), 'left_arm': (-15, 0, 20), 'base': (6, 0, 0)}),
    (0.5, {}),
], LIMBS)
atk.pos('base', (0, (0, 0, 0)), (0.22, (0, -1, -3)), (0.5, (0, 0, 0)))
fingers(atk, [(0, 0), (0.1, -25), (0.22, 35), (0.5, 0)])
A['ATTACK_R'] = atk
atk_l = Anim('attack_l', 0.5)
pose_seq(atk_l, [(0.0, {}),
                 (0.1, mirror({'right_arm': (-150, 20, -15), 'right_fore': (-30, 0, 0), 'chest': (-4, 28, 0), 'waist': (0, 10, 0), 'left_arm': (10, 0, 12)})),
                 (0.22, mirror({'right_arm': (-45, -55, 15), 'right_fore': (-5, 0, 0), 'chest': (10, -30, 0), 'waist': (4, -12, 0), 'left_arm': (-15, 0, 20), 'base': (6, 0, 0)})),
                 (0.5, {})], LIMBS)
atk_l.pos('base', (0, (0, 0, 0)), (0.22, (0, -1, -3)), (0.5, (0, 0, 0)))
fingers(atk_l, [(0, 0), (0.1, -25), (0.22, 35), (0.5, 0)])
A['ATTACK_L'] = atk_l

# transformation: a curled cocoon of water unfurls into the tide lord and roars
tr = Anim('transform', 2.4)
curl = {'right_leg': (-70, 0, 10), 'right_shin': (110, 0, 0), 'left_leg': (-70, 0, -10), 'left_shin': (110, 0, 0),
        'right_foot': (-30, 0, 0), 'left_foot': (-30, 0, 0), 'chest': (40, 0, 0), 'waist': (20, 0, 0), 'head': (35, 0, 0),
        'right_arm': (-70, -40, -50), 'left_arm': (-70, 40, 50), 'right_fore': (-80, 0, 0), 'left_fore': (-80, 0, 0)}
roar = {'right_arm': (-40, 0, 75), 'left_arm': (-40, 0, -75), 'right_fore': (-20, 0, 0), 'left_fore': (-20, 0, 0),
        'chest': (-22, 0, 0), 'waist': (-8, 0, 0), 'head': (-38, 0, 0), 'right_leg': (-8, 0, 12), 'left_leg': (-8, 0, -12)}
roar2 = {k: (v[0] - 3, v[1], v[2] * 1.05) for k, v in roar.items()}
pose_seq(tr, [(0.0, curl), (0.75, {k: (v[0] * 0.85, v[1], v[2]) for k, v in curl.items()}), (1.05, roar), (1.4, roar2), (1.75, roar), (2.4, {})], LIMBS)
tr.pos('base', (0, (0, -16, 0)), (0.75, (0, -13, 0)), (1.05, (0, 3, 0)), (1.3, (0, 0, 0)), (2.4, (0, 0, 0)))
tr.scl('base', (0, (0.35, 0.35, 0.35)), (0.75, (0.8, 0.8, 0.8)), (1.05, (1.12, 1.12, 1.12)), (1.3, (1, 1, 1)), (2.4, (1, 1, 1)))
fingers(tr, [(0, 40), (0.75, 40), (1.05, -30), (1.75, -30), (2.4, 0)])
coat_wave(tr, 2.4, 1.6, 20, 2)
serp_wave(tr, 2.4, 1.8, 2)
fin_wave(tr, 2.4, 1.4, 22, 2)
drops(tr, 2.4, 2.0, 2, spin=360)
for s in SIDES:
    for n in (f'{s}_fin', f'{s}_fin2'):
        tr.scl(n, (0, (0.2, 0.2, 0.2)), (0.9, (0.4, 0.4, 0.4)), (1.1, (1.35, 1.35, 1.35)), (1.5, (1, 1, 1)), (2.4, (1, 1, 1)))
A['TRANSFORM'] = tr

# 1) Tidal Rend: lunge with the right claw, left cross slash, rising right slash
rend = Anim('rend', 1.2)
pose_seq(rend, [
    (0.0, {}),
    (0.14, {'right_arm': (40, 20, -30), 'right_fore': (-60, 0, 0), 'chest': (10, 30, 0), 'left_arm': (-40, 0, 30),
            'right_leg': (-40, 0, 0), 'right_shin': (60, 0, 0), 'left_leg': (15, 0, 0), 'left_shin': (30, 0, 0), 'base': (10, 0, 0)}),
    (0.3, {'right_arm': (-95, -10, 10), 'right_fore': (-5, 0, 0), 'chest': (18, -20, 0), 'left_arm': (35, 0, 25), 'base': (28, 0, 0),
           'right_leg': (-50, 0, 0), 'right_shin': (30, 0, 0), 'left_leg': (40, 0, 0), 'left_shin': (20, 0, 0), 'head': (-25, 0, 0)}),
    (0.45, {'left_arm': (-150, 10, -25), 'left_fore': (-30, 0, 0), 'right_arm': (-40, 0, 20), 'chest': (0, -30, 0), 'base': (12, 0, 0),
            'right_leg': (-30, 0, 0), 'right_shin': (40, 0, 0), 'left_leg': (20, 0, 0)}),
    (0.58, {'left_arm': (-40, 55, -10), 'left_fore': (-5, 0, 0), 'right_arm': (-20, 0, 25), 'chest': (10, 35, 0), 'waist': (4, 12, 0), 'base': (15, 0, 0)}),
    (0.68, {'right_arm': (20, 30, -20), 'right_fore': (-40, 0, 0), 'left_arm': (-30, 30, -20), 'chest': (15, 10, 0), 'base': (18, 0, 0),
            'right_leg': (-45, 0, 0), 'right_shin': (70, 0, 0), 'left_leg': (-20, 0, 0), 'left_shin': (60, 0, 0)}),
    (0.8, {'right_arm': (-175, 10, 20), 'right_fore': (-10, 0, 0), 'left_arm': (10, 0, 30), 'chest': (-18, -15, 0), 'head': (-20, 0, 0), 'base': (-6, 0, 0)}),
    (1.2, {}),
], LIMBS)
rend.pos('base', (0, (0, 0, 0)), (0.14, (0, -4, 2)), (0.3, (0, -3, -8)), (0.58, (0, -2, -4)), (0.68, (0, -6, -2)), (0.8, (0, 3, -2)), (1.2, (0, 0, 0)))
fingers(rend, [(0, 0), (0.14, -30), (0.3, 30), (0.45, -30), (0.58, 35), (0.68, -30), (0.8, 30), (1.2, 0)])
coat_wave(rend, 1.2, 2.0, 12, 2, back=25)
serp_wave(rend, 1.2, 2.0, 2)
fin_wave(rend, 1.2, 1.4, 14, 2)
drops(rend, 1.2)
A['REND'] = rend

# 2) Maelstrom: both claws spin a vortex overhead, then hurl it forward
mael = Anim('maelstrom', 1.8)
up = {'right_arm': (-165, 0, -10), 'left_arm': (-165, 0, 10), 'right_fore': (-25, 0, 0), 'left_fore': (-25, 0, 0), 'head': (-30, 0, 0), 'chest': (-12, 0, 0)}
pose_seq(mael, [
    (0.0, {}),
    (0.25, dict(up, chest=(-12, 25, 0))),
    (0.4, dict(up, chest=(-12, -25, 0), right_arm=(-160, 20, 10), left_arm=(-160, 20, -10))),
    (0.55, dict(up, chest=(-12, 25, 0), right_arm=(-160, -20, 10), left_arm=(-160, -20, -10))),
    (0.72, {'right_arm': (-75, 10, -5), 'left_arm': (-75, -10, 5), 'right_fore': (-5, 0, 0), 'left_fore': (-5, 0, 0), 'chest': (20, 0, 0), 'head': (10, 0, 0),
            'right_leg': (-30, 0, 0), 'right_shin': (35, 0, 0), 'left_leg': (20, 0, 0), 'base': (10, 0, 0)}),
    (1.3, {'right_arm': (-70, 10, -5), 'left_arm': (-70, -10, 5), 'right_fore': (-5, 0, 0), 'left_fore': (-5, 0, 0), 'chest': (16, 0, 0), 'head': (8, 0, 0),
           'right_leg': (-30, 0, 0), 'right_shin': (35, 0, 0), 'left_leg': (20, 0, 0), 'base': (8, 0, 0)}),
    (1.8, {}),
], LIMBS)
mael.pos('base', (0, (0, 0, 0)), (0.4, (0, 1.5, 0)), (0.72, (0, -2, -3)), (1.3, (0, -2, -3)), (1.8, (0, 0, 0)))
fingers(mael, [(0, 0), (0.25, -35), (0.55, -35), (0.72, -40), (1.3, -30), (1.8, 0)])
coat_wave(mael, 1.8, 1.8, 18, 3)
serp_wave(mael, 1.8, 2.2, 3, lift=-10)
fin_wave(mael, 1.8, 1.4, 18, 3)
drops(mael, 1.8, 2.5, 3, spin=720)
A['MAELSTROM'] = mael

# 3) Abyssal Geysers: hop up, double-claw slam into the ground
gey = Anim('geyser', 1.4)
pose_seq(gey, [
    (0.0, {}),
    (0.3, {'right_arm': (-170, 0, -15), 'left_arm': (-170, 0, 15), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (-15, 0, 0), 'head': (-20, 0, 0),
           'right_leg': (-50, 0, 0), 'right_shin': (80, 0, 0), 'left_leg': (-30, 0, 0), 'left_shin': (70, 0, 0)}),
    (0.5, {'right_arm': (-35, 0, -10), 'left_arm': (-35, 0, 10), 'right_fore': (-10, 0, 0), 'left_fore': (-10, 0, 0), 'chest': (42, 0, 0), 'waist': (15, 0, 0),
           'head': (-15, 0, 0), 'right_leg': (-75, 0, 10), 'right_shin': (100, 0, 0), 'left_leg': (-20, 0, -10), 'left_shin': (95, 0, 0), 'left_foot': (-30, 0, 0)}),
    (0.95, {'right_arm': (-38, 0, -10), 'left_arm': (-38, 0, 10), 'right_fore': (-10, 0, 0), 'left_fore': (-10, 0, 0), 'chest': (40, 0, 0), 'waist': (15, 0, 0),
            'head': (-18, 0, 0), 'right_leg': (-75, 0, 10), 'right_shin': (100, 0, 0), 'left_leg': (-20, 0, -10), 'left_shin': (95, 0, 0), 'left_foot': (-30, 0, 0)}),
    (1.4, {}),
], LIMBS)
gey.pos('base', (0, (0, 0, 0)), (0.3, (0, 6, 0)), (0.5, (0, -12, -2)), (0.95, (0, -12, -2)), (1.4, (0, 0, 0)))
fingers(gey, [(0, 0), (0.3, 30), (0.5, 45), (0.95, 45), (1.4, 0)])
coat_wave(gey, 1.4, 1.8, 25, 2)
serp_wave(gey, 1.4, 1.8, 2)
fin_wave(gey, 1.4, 1.2, 20, 2)
drops(gey, 1.4)
A['GEYSER'] = gey

# 4) Pressure Beam: gather a water orb between the claws, then fire a torrent
beam = Anim('beam', 3.2)
charge = {'right_arm': (-70, -35, 10), 'left_arm': (-70, 35, -10), 'right_fore': (-45, 0, 0), 'left_fore': (-45, 0, 0), 'chest': (10, 0, 0),
          'right_leg': (-25, 0, 8), 'right_shin': (35, 0, 0), 'left_leg': (15, 0, -8), 'left_shin': (20, 0, 0), 'head': (-5, 0, 0)}
fire = {'right_arm': (-92, -14, 0), 'left_arm': (-92, 14, 0), 'right_fore': (-5, 0, 0), 'left_fore': (-5, 0, 0), 'chest': (-8, 0, 0),
        'right_leg': (-30, 0, 8), 'right_shin': (30, 0, 0), 'left_leg': (25, 0, -8), 'left_shin': (15, 0, 0), 'head': (-4, 0, 0)}
frames = [(0.0, {}), (0.35, charge), (0.8, {k: (v[0] - 4, v[1], v[2]) for k, v in charge.items()})]
t = 0.9
k = 0
while t < 2.85:
    j = 1.5 if k % 2 else -1.5
    frames.append((t, {kk: (v[0] + j, v[1], v[2]) for kk, v in fire.items()}))
    t += 0.15
    k += 1
frames.append((3.2, {}))
pose_seq(beam, frames, LIMBS)
beam.pos('base', (0, (0, 0, 0)), (0.8, (0, -3, 0)), (0.9, (0, -3, 2.5)), (2.8, (0, -3, 2)), (3.2, (0, 0, 0)))
fingers(beam, [(0, 0), (0.35, -20), (0.8, -25), (0.9, -45), (2.8, -45), (3.2, 0)])
coat_wave(beam, 3.2, 1.2, 10, 4, back=20)
serp_wave(beam, 3.2, 1.3, 4)
fin_wave(beam, 3.2, 1.0, 26, 4)
drops(beam, 3.2, 1.5, 2, spin=540)
A['BEAM'] = beam

# 5) Tsunami Crash: crouch, leap into a front flip, crash down with both claws
tsu = Anim('tsunami', 2.4)
crouch_p = {'right_leg': (-70, 0, 8), 'right_shin': (110, 0, 0), 'left_leg': (-70, 0, -8), 'left_shin': (110, 0, 0), 'right_foot': (-35, 0, 0), 'left_foot': (-35, 0, 0),
            'chest': (35, 0, 0), 'right_arm': (45, 0, -20), 'left_arm': (45, 0, 20), 'head': (-30, 0, 0)}
tuck = {'right_leg': (-100, 0, 10), 'right_shin': (130, 0, 0), 'left_leg': (-100, 0, -10), 'left_shin': (130, 0, 0),
        'chest': (30, 0, 0), 'right_arm': (-50, 0, -30), 'left_arm': (-50, 0, 30), 'right_fore': (-80, 0, 0), 'left_fore': (-80, 0, 0), 'head': (20, 0, 0)}
slam = {'right_arm': (-30, 0, -20), 'left_arm': (-30, 0, 20), 'right_fore': (-10, 0, 0), 'left_fore': (-10, 0, 0), 'chest': (45, 0, 0), 'waist': (15, 0, 0),
        'head': (-25, 0, 0), 'right_leg': (-90, 0, 20), 'right_shin': (110, 0, 0), 'left_leg': (-40, 0, -20), 'left_shin': (120, 0, 0), 'left_foot': (-30, 0, 0)}
pose_seq(tsu, [(0.0, {}), (0.28, crouch_p), (0.45, {'right_arm': (-170, 0, -20), 'left_arm': (-170, 0, 20), 'head': (-20, 0, 0), 'chest': (-15, 0, 0)}),
               (0.6, tuck), (0.85, tuck), (1.0, slam), (1.6, slam), (2.4, {})], [n for n in LIMBS if n != 'base'])
tsu.rot('base', (0, (0, 0, 0)), (0.28, (12, 0, 0)), (0.45, (-10, 0, 0)), (0.55, (-100, 0, 0), 'L'), (0.7, (-230, 0, 0), 'L'), (0.85, (-340, 0, 0), 'L'),
        (0.95, (-360, 0, 0), 'L'), (1.0, (-352, 0, 0)), (1.6, (-352, 0, 0)), (2.4, (-360, 0, 0)))
tsu.pos('base', (0, (0, 0, 0)), (0.28, (0, -10, 0)), (0.45, (0, 4, 0)), (0.7, (0, 10, 0)), (0.95, (0, 4, 0)), (1.0, (0, -12, -2)), (1.6, (0, -12, -2)), (2.4, (0, 0, 0)))
fingers(tsu, [(0, 0), (0.45, -30), (0.6, 40), (1.0, 45), (1.6, 45), (2.4, 0)])
coat_wave(tsu, 2.4, 2.2, 22, 3, back=-10)
serp_wave(tsu, 2.4, 2.4, 3, lift=-12)
fin_wave(tsu, 2.4, 1.6, 24, 3)
drops(tsu, 2.4, 2.0, 2, spin=720)
A['TSUNAMI'] = tsu

# death: knees buckle, collapses and melts into water
death = Anim('death', 1.0)
pose_seq(death, [(0.0, {}), (0.25, {'head': (-35, 0, 0), 'chest': (-15, 0, 0), 'right_arm': (-30, 0, 45), 'left_arm': (-30, 0, -45)}),
                 (1.0, {'head': (40, 0, 10), 'chest': (40, 0, 0), 'right_arm': (30, 0, -10), 'left_arm': (20, 0, 20), 'right_leg': (-90, 0, 0), 'left_leg': (-70, 0, 0),
                        'right_shin': (100, 0, 0), 'left_shin': (110, 0, 0)})], [n for n in LIMBS if n != 'base'])
death.pos('base', (0, (0, 0, 0)), (0.25, (0, 2, 0)), (1.0, (0, -26, 2)))
death.scl('base', (0, (1, 1, 1)), (0.5, (1, 1, 1)), (1.0, (1.25, 0.35, 1.25)))
A['DEATH'] = death
