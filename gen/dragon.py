"""Ejder Muhafızı (Dragon Warden) — dragon-masked warrior in red scale armour with golden dragon-head pauldrons that
breathe flame, a burning sash and a flame-crowned glaive with a sun orb."""
import math, numpy as np
from core import Model
from formlib import Rig, humanoid, FormAnims, wave_chain, R0
from fpaint import solid, veins, pattern, fire_cloth, flame_mat, emissive, eyes_face, painter_with, ramp3, outline, organic, edge_of
from paint import noise2, lerp, glow_img, draw_line

M = Model('dragon', 512, 256)
rig = Rig(M)
S = humanoid(M, chest=((-7.2, -52.5, -4.2), (14.4, 10.5, 8.4)), shoulder=(8.8, -49.5, 0.0), elbow=(12.2, -35.5, 1.5), wrist=(14.4, -23, -0.5),
             head=((-3.0, -63.0, -3.2), (6.0, 7.0, 6.4)), face=None,
             mats=dict(pelvis='dark', waist='scales', belly='scales', chest='scales_chest', neck='dark', head='mask', face='mask',
                       arm='limb', fore='limb', hand='limb_dark', leg='limb', shin='gold', foot='gold'))
# high dark collar
M.cube('chest', (-4.2, -56.5, -3.2), (8.4, 4.5, 6.8), 'dark')
# dragon mask: snout, brow ridges, glowing eyes, horns
M.cube('head', (-2.4, -60.5, -6.8), (4.8, 3.6, 3.8), 'mask')
M.cube('head', (-1.8, -58.2, -7.6), (3.6, 1.6, 1.0), 'mask_dark')
M.cube('head', (-3.1, -61.8, -3.9), (6.2, 2.0, 0.8), 'mask_eyes')
for side, sx in (('right', -1), ('left', 1)):
    rig.spike(f'{side}_horn0', 'head', (sx * 2.2, -62.5, -1), (sx * 4.5, -69.5, 2.5), 1.9, 'horn')
    rig.spike(f'{side}_horn1', f'{side}_horn0', (sx * 4.5, -69.5, 2.5), (sx * 4.2, -73, 7), 1.2, 'horn')
    rig.spike(f'{side}_crest', 'head', (sx * 3.1, -59, 0), (sx * 4.5, -60.5, 5.5), 1.2, 'mask_dark')
FLAMEHAIR = []
for i, (px, ry, w) in enumerate(((-2.5, 20, 6), (0, 0, 7), (2.5, -20, 6))):
    FLAMEHAIR.append(rig.panel_chain(f'fh{i}_', 'head', (px, -62.5, 3.2), (35, ry, 0), -w / 2, w, (10, 11, 12), ('fire_hair', 'fire_hair', 'flame')))

# golden dragon-head pauldrons breathing flame ribbons
MOUTHFIRE = []
for side, sx in (('right', -1), ('left', 1)):
    pd = f'{side}_pauldron'
    M.bone_w(pd, f'{side}_arm', (sx * 10, -50.5, 0), R0)
    M.cube_l(pd, (-3.8, -3.6, -4.2), (7.6, 5.6, 8.4), 'gold_dragon')
    M.cube_l(pd, (sx * 1.5 - 2.2, -1.4, -7.0), (4.4, 3.2, 3.2), 'gold_snout')
    rig.spike(f'{side}_pdh0', pd, (sx * 11, -53.5, 1), (sx * 13, -58, 5), 1.6, 'horn')
    rig.spike(f'{side}_pdh1', pd, (sx * 13.5, -52, 1), (sx * 17, -54, 4), 1.4, 'gold')
    names = rig.chain(f'{side}_mf', pd, [(sx * 11.5, -50.5, -7.5), (sx * 15, -56, -6), (sx * 16, -63, -1), (sx * 12, -67, 5)],
                      (2.6, 2.2, 1.6), 'fire_ribbon', depth_ratio=0.4, ext=0.8)
    MOUTHFIRE.append(names)
    M.cube_l(f'{side}_fore', (-2.1, 6, -2.1), (4.2, 7.5, 4.2), 'gold')
    rig.spike(f'{side}_fin', f'{side}_fore', (sx * 14, -30, 2.5), (sx * 16.5, -38, 6), 1.4, 'fin')
    kn = f'{side}_knee'
    M.bone_w(kn, f'{side}_shin', (sx * 4.6, -3, -1.5), R0)
    M.cube_l(kn, (-2.5, -2.4, -3.4), (5, 4.8, 2.2), 'gold')
    rig.spike(f'{side}_kspike', kn, (sx * 5, -3, -3.4), (sx * 5.5, -8.5, -5), 1.4, 'gold')
    for i, tx in enumerate((-1.6, 0, 1.6)):
        rig.spike(f'{side}_claw{i}', f'{side}_foot', (sx * 5.6 + tx, 23, -5), (sx * 5.6 + tx * 1.4, 24, -8.5), 1.2, 'gold')

# diagonal sash (left shoulder -> right hip) and the long flame sash hanging in front
for k in range(6):
    x = 5.5 - k * 2.2
    y = -51 + k * 3.0
    M.cube('chest' if k < 4 else 'waist', (x - 1.4, y, -4.6), (2.8, 3.6, 0.6), 'sash')
SASH = rig.panel_chain('sash', 'hips', (-2.5, -28, -3.6), (-4, 4, 0), -3.5, 7, (16, 14, 12), ('sash_panel', 'sash_panel', 'sash_tip'))
M.cube('hips', (-5, -29.5, -3.4), (10, 2.2, 6.8), 'gold')
M.cube('hips', (-1.5, -30.6, -3.9), (3, 3, 0.6), 'gold_swirl')
TAILS = [rig.panel_chain('ctail_l', 'hips', (4.2, -27.5, 2.5), (8, -25, -6), -3, 6, (14, 14), ('limb_cloth', 'sash_tip'))]

# the glaive: flame crowned shaft, golden eye ring with a sun orb, curved lava blade pointing down
rig.local('weapon', 'right_hand', (0, 2.0, 0))
M.cube_l('weapon', (-0.9, -26, -0.9), (1.8, 36, 1.8), 'shaft')
for y in (-24, -16, -6, 4):
    M.cube_l('weapon', (-1.2, y, -1.2), (2.4, 1.6, 2.4), 'gold')
rig.local('wtop', 'weapon', (0, -26, 0))
M.cube_l('wtop', (-3.5, -8, 0), (7, 8, 0), 'flame', plane=True)
rig.local('wtop2', 'wtop', (0, 0, 0), (0, 90, 0))
M.cube_l('wtop2', (-3.5, -8, 0), (7, 8, 0), 'flame', plane=True)
M.cube_l('weapon', (-4.2, 10, -1.0), (8.4, 9.5, 2.0), 'gold_ring')
M.cube_l('weapon', (-2.3, 12.5, -1.6), (4.6, 4.6, 3.2), 'orb')
rig.spike_local('wcurl0', 'weapon', (-3.5, 12, 0), (-7.5, 4, 0), 1.6, 'gold')
rig.spike_local('wcurl1', 'weapon', (3.5, 18, 0), (6, 26, 0), 1.6, 'gold')
BL = [(0, 19.5, 0), (1.5, 33, 0), (4.5, 45, 0), (8.5, 55, 0)]
prev = 'weapon'
for i in range(3):
    A, B = rig.world_pt('weapon', BL[i]), rig.world_pt('weapon', BL[i + 1])
    w = (7.5, 7.0, 5.0)[i]
    M.seg(f'wblade{i}', 'weapon', A, B, w, 1.8, 'blade', extend=0.8)

# ---------------- textures ----------------
INK = (26, 6, 6)
RED = ((90, 14, 12), (178, 40, 30), (228, 96, 64))
DARK = ((22, 8, 8), (52, 18, 16), (90, 36, 30))
GOLD = ((120, 80, 24), (206, 160, 64), (250, 226, 150))
FLAME = ((180, 40, 10), (255, 130, 30), (255, 238, 150))


def scales(c, face, W, H, r, pal=RED):
    yy, xx = np.mgrid[0:H, 0:W]
    row = yy // 2
    cx = (xx + (row % 2)) % 3
    edge = (yy % 2 == 1) | (cx == 0)
    t = 0.55 + (noise2(H, W, r, 4) - 0.5) * 0.4 - 0.2 * (yy / max(H - 1, 1))
    col = ramp3(t, *pal)
    col[edge] = lerp(col[edge], INK, 0.45)
    outline(col, H, W, INK, 0.5)
    return col, np.ones((H, W)), None


def mask(c, face, W, H, r):
    col, a, _ = solid(RED, INK, 0.3, 0.3)(c, face, W, H, r)
    g = None
    if face == 'north':
        m = organic(H, W, r, 2.5, 0.6)
        col[m] = lerp(col[m], (110, 20, 16), 0.7)
    return col, a, g


def mask_eyes(c, face, W, H, r):
    col, a, _ = solid(RED, INK, 0.2, 0.0)(c, face, W, H, r)
    if face != 'north':
        return col, a, None
    m = np.zeros((H, W))
    draw_line(m, 0, 0, W // 2 - 1, H - 1)
    draw_line(m, W - 1, 0, W // 2, H - 1)
    col[m > 0] = (255, 170, 60)
    return col, a, glow_img(H, W, (255, 150, 40), m)


def gold_dragon(c, face, W, H, r):
    col, a, _ = pattern(solid(GOLD, INK, 0.35, 0.3), ((100, 64, 20), (160, 110, 40), (200, 150, 60)), 2.5, 0.55, INK)(c, face, W, H, r)
    g = None
    if face in ('west', 'east'):
        yy, xx = np.mgrid[0:H, 0:W]
        eye = (yy == H // 3) & (np.abs(xx - W * 0.3) < 1)
        col[eye] = (255, 90, 30)
        g = glow_img(H, W, (255, 110, 40), eye * 1.0)
    return col, a, g


def gold_snout(c, face, W, H, r):
    col, a, _ = solid(GOLD, INK, 0.3, 0.3)(c, face, W, H, r)
    g = None
    if face == 'north':
        col[H // 2:, :] = (60, 14, 8)
        col[H // 2, :] = (255, 120, 30)
        g = glow_img(H, W, (255, 140, 40), np.arange(H)[:, None].repeat(W, 1) >= H // 2)
    return col, a, g


def limb(c, face, W, H, r):
    col, a, _ = solid(DARK, INK, 0.4, 0.2)(c, face, W, H, r)
    yy, xx = np.mgrid[0:H, 0:W]
    sw = np.sin(xx * 0.9 + yy * 0.35 + noise2(H, W, r, 4) * 5) > 0.55
    col[sw] = lerp(col[sw], (180, 44, 32), 0.85)
    return col, a, None


def blade(c, face, W, H, r):
    col, a, _ = solid(((30, 8, 8), (64, 18, 16), (100, 40, 30)), INK, 0.4, 0.0)(c, face, W, H, r)
    g = None
    if face in ('north', 'south'):
        from paint import cracks
        m = cracks(H, W, r, 3, 0.6)
        col[m >= 1] = (255, 120, 30)
        col[(m > 0) & (m < 1)] = lerp(col[(m > 0) & (m < 1)], (255, 80, 20), 0.5)
        g = glow_img(H, W, (255, 130, 40), (m >= 1) * 1.0 + ((m > 0) & (m < 1)) * 0.3)
        col[:, -1] = (200, 170, 150)
    return col, a, g


mats = {
    'dark': solid(DARK, INK, 0.3, 0.2), 'scales': scales, 'scales_chest': scales, 'mask': mask, 'mask_dark': solid(((60, 10, 10), (110, 20, 16), (150, 40, 30)), INK, 0.3, 0.2),
    'mask_eyes': mask_eyes, 'horn': solid(((20, 8, 8), (50, 22, 18), (100, 60, 40)), INK, 0.3, -0.3), 'limb': limb, 'limb_dark': solid(DARK, INK, 0.3, 0.2),
    'limb_cloth': fire_cloth(DARK, FLAME, INK, 1, 0.5, 0.4, top=0.6), 'gold': solid(GOLD, INK, 0.35, 0.3), 'gold_swirl': pattern(solid(GOLD, INK, 0.3, 0.3), DARK, 2, 0.6),
    'gold_dragon': gold_dragon, 'gold_snout': gold_snout, 'fin': solid(RED, INK, 0.3, -0.3),
    'fire_hair': veins(solid(((200, 60, 20), (255, 140, 40), (255, 220, 120)), INK, 0.4, -0.3), (255, 240, 180), (255, 160, 60), 1, 0.7),
    'flame': flame_mat(), 'fire_ribbon': emissive((255, 150, 40), (255, 130, 30), 0.8, (255, 240, 170)),
    'sash': solid(((180, 60, 30), (230, 110, 50), (255, 180, 90)), INK, 0.3, 0.0),
    'sash_panel': fire_cloth(((180, 60, 30), (220, 100, 50), (250, 150, 80)), ((230, 150, 60), (250, 210, 110), (255, 245, 200)), INK, 1, 1.0, 0.3, top=0.8),
    'sash_tip': fire_cloth(((180, 60, 30), (220, 100, 50), (250, 150, 80)), ((230, 150, 60), (250, 210, 110), (255, 245, 200)), INK, 2, 0.4, 0.5, top=0.2),
    'shaft': solid(((20, 8, 8), (48, 20, 16), (80, 40, 30)), INK, 0.4, 0.0), 'gold_ring': solid(GOLD, INK, 0.3, 0.2),
    'orb': emissive((255, 110, 40), (255, 120, 40), 1.0), 'blade': blade,
}
P = painter_with(M, mats)


def dress(a, length, amp, speed, flare, back, lift):
    for i, names in enumerate(FLAMEHAIR):
        wave_chain(a, names, length, amp * 1.1, speed * 2, phase=i * 1.2, axis=(1.0, 0.3, 0.6), lift=lift + back * 0.4, step=0.9)
    for i, names in enumerate(MOUTHFIRE):
        wave_chain(a, names, length, amp * 0.8, speed * 2, phase=i * 2.0, axis=(0.6, 0.6, 0.8), step=0.8, grow=1.1)
    wave_chain(a, SASH, length, amp * 0.7, speed, phase=0.3, axis=(1.0, 0.1, 0.4), lift=-flare * 0.5 + back * 0.5, step=1.0)
    for names in TAILS:
        wave_chain(a, names, length, amp * 0.8, speed, phase=1.4, axis=(1.0, 0.2, 0.5), lift=flare * 0.5 + back * 0.5, step=1.0)
    for n in ('wtop', 'wtop2'):
        a.scl(n, *[(length * j / 8, (1, 1 + 0.35 * math.sin(math.tau * j / 8 * speed * 2), 1)) for j in range(9)])


CARRY = {'right_arm': (-18, 0, 15), 'right_fore': (-50, 0, 0), 'weapon': (55, 0, -8)}
MOVE = {'right_arm': (-10, 0, 18), 'right_fore': (-60, 0, 0), 'weapon': (100, 0, -12)}
F = FormAnims(dress, CARRY, MOVE).locomotion()
F.ability(0, 'channel', length=2.6, t0=0.5, t1=2.2)
F.ability(1, 'cast_forward', length=1.6, t=0.55)
F.ability(2, 'spin', length=1.6, turns=1, t0=0.3)
F.ability(3, 'leap', length=2.2, jump=0.3, land=1.0)
F.ability(4, 'roar', length=2.2, t=0.8)
A = F.A
