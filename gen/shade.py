"""Kızıl Gölge (Crimson Shade) — white-haired red demon with black horns and a black mask, open white jacket, spiked
black gauntlets, baggy black trousers with two long tattered tails, and a jagged black blade."""
import math, numpy as np
from core import Model
from formlib import Rig, humanoid, FormAnims, wave_chain, R0
from fpaint import solid, veins, pattern, emissive, eyes_face, painter_with, ramp3, outline, organic, flame_shape
from paint import noise2, lerp, glow_img, draw_line, cracks

M = Model('shade', 512, 256)
rig = Rig(M)
S = humanoid(M, chest=((-7.2, -52.5, -4.2), (14.4, 10.5, 8.4)), belly=((-5.0, -43, -3.3), (10.0, 7.5, 6.6)),
             shoulder=(8.8, -49.5, 0.0), arm_w=(4.2, 4.4), fore_w=(3.8, 3.9), elbow=(12.2, -35.5, 1.5), wrist=(14.2, -23, -0.5),
             leg_w=(5.6, 6.0), shin_w=(4.6, 5.0), head=((-3.0, -63.0, -3.2), (6.0, 7.2, 6.4)),
             mats=dict(pelvis='pants', waist='red_abs', belly='red_abs', chest='red_chest', neck='red', head='mask', face='face',
                       arm='red', fore='gauntlet', hand='red_dark', leg='pants', shin='red_leg', foot='claw_foot'))
# open white jacket (vest) with a high collar
for side, sx in (('right', -1), ('left', 1)):
    x0 = -7.6 if sx < 0 else 3.4
    M.cube('chest', (x0, -53, -4.6), (4.2, 12, 0.6), 'white')
    M.cube('chest', (sx * 7.3 - 0.4, -53, -4.4), (0.8, 12, 8.8), 'white')
    rig.local(f'{side}_collar', 'chest', (sx * 3.8, -17.5, -1), (-10, sx * -20, sx * 12))
    M.cube_l(f'{side}_collar', (-2.2, -5, -0.3), (4.4, 6, 0.6), 'white', plane=False)
    M.cube_l(f'{side}_arm', (-2.4, 2, -2.5), (4.8, 6, 5.0), 'white')      # short sleeves
    M.cube_l(f'{side}_arm', (-2.3, 9, -2.4), (4.6, 1.2, 4.8), 'black')    # arm band
    for k in range(3):
        rig.spike(f'{side}_gspike{k}', f'{side}_fore', (sx * (12.8 + k * 0.6), -33 + k * 3.8, 2), (sx * (16 + k), -37 + k * 3.8, 5.5), 1.4, 'black')
    for i, fz in enumerate((-1.3, 0, 1.3)):
        rig.local(f'{side}_f{i}', f'{side}_hand', (0, 3.9, fz), (0, 0, sx * -12))
        M.cube_l(f'{side}_f{i}', (-0.5, 0, -0.5), (1.0, 3.2, 1.0), 'claw')
    kn = f'{side}_knee'
    M.bone_w(kn, f'{side}_shin', (sx * 4.6, -3, -1.5), R0)
    M.cube_l(kn, (-2.6, -2.2, -3.6), (5.2, 4.4, 1.2), 'black')
M.cube('chest', (-7.3, -53, 3.8), (14.6, 12.5, 0.8), 'white')
# belt with a silver buckle
M.cube('hips', (-5.0, -29.5, -3.4), (10, 2.4, 6.8), 'black')
M.cube('hips', (-1.4, -29.8, -3.9), (2.8, 2.8, 0.6), 'silver')
# horns sweeping out and up, spiky white hair
for side, sx in (('right', -1), ('left', 1)):
    rig.spike(f'{side}_horn0', 'head', (sx * 2.6, -61.5, -0.5), (sx * 8.5, -64, 0), 2.2, 'horn')
    rig.spike(f'{side}_horn1', f'{side}_horn0', (sx * 8.5, -64, 0), (sx * 10, -71.5, 1.5), 1.5, 'horn')
HAIR = [((0, -63, 0), (0, -67.5, 2), 3.0), ((-2, -63, -1), (-4.5, -66, -3), 2.4), ((2, -63, -1), (4.5, -66, -3), 2.4),
        ((-2.5, -62.5, 1.5), (-6, -64.5, 5), 2.2), ((2.5, -62.5, 1.5), (6, -64.5, 5), 2.2), ((0, -62, 3), (0, -63.5, 8), 2.6),
        ((-1, -63, -2.8), (-1.5, -62, -5.5), 1.8), ((1.2, -63, -2.8), (2, -61.5, -5.5), 1.6)]
for i, (A, B, w) in enumerate(HAIR):
    rig.spike(f'hair{i}', 'head', A, B, w, 'hair')
# two long tattered tails from the belt down to the ground
TAILS = []
for side, sx in (('right', -1), ('left', 1)):
    TAILS.append(rig.panel_chain(f'{side}_tail', 'hips', (sx * 5.2, -28, 0.5), (4, sx * -80, sx * 10), -3.5, 7, (16, 16, 16),
                                 ('tail', 'tail', 'tail_tip')))
SASH = [rig.panel_chain('sash', 'hips', (1.5, -27.5, -3.6), (-4, 0, 0), -1.2, 2.4, (14, 14), ('black_cloth', 'black_cloth'))]

# the jagged black blade
rig.local('weapon', 'right_hand', (0, 2.2, 0))
M.cube_l('weapon', (-0.8, -7, -0.8), (1.6, 9.5, 1.6), 'hilt')
M.cube_l('weapon', (-2.6, 2.5, -0.9), (5.2, 1.6, 1.8), 'black')
M.cube_l('weapon', (-2.2, 4.1, -0.6), (4.4, 38, 1.2), 'shade_blade')
M.cube_l('weapon', (-1.3, 42, -0.5), (2.6, 7, 1.0), 'shade_blade')
for k, (y, h) in enumerate(((10, 5), (19, 6), (28, 5), (36, 4))):
    rig.local(f'wjag{k}', 'weapon', (2.2, y, 0), (0, 0, -35))
    M.cube_l(f'wjag{k}', (-0.4, -h, -0.5), (1.4, h, 1.0), 'shade_blade')
rig.local('waura', 'weapon', (0, 26, 0))
M.cube_l('waura', (-4, -22, 0), (8, 44, 0), 'aura', plane=True)

# ---------------- textures ----------------
INK = (10, 4, 6)
RED = ((96, 10, 16), (178, 24, 30), (226, 70, 64))
BLACK = ((6, 6, 8), (22, 20, 24), (54, 50, 56))
WHITE = ((170, 170, 176), (226, 226, 230), (252, 252, 255))


def red_skin(c, face, W, H, r, abs_=False):
    col, a, _ = solid(RED, INK, 0.4, 0.2)(c, face, W, H, r)
    m = cracks(H, W, r, 2, 0.45)
    col[m >= 1] = lerp(col[m >= 1], INK, 0.8)
    if abs_ and face == 'north':
        yy, xx = np.mgrid[0:H, 0:W]
        col[(yy % 3 == 0) | (np.abs(xx - (W - 1) / 2) < 0.6)] = lerp(col[(yy % 3 == 0) | (np.abs(xx - (W - 1) / 2) < 0.6)], (70, 6, 10), 0.6)
    return col, a, None


def tail(c, face, W, H, r, tip=False):
    col, a, _ = solid(BLACK, INK, 0.4, 0.0)(c, face, W, H, r)
    g = None
    yy, xx = np.mgrid[0:H, 0:W]
    if face in ('north', 'south'):
        spots = organic(H, W, r, 2.5, 0.72) & (yy > H * 0.3)
        col[spots] = (220, 20, 30)
        g = glow_img(H, W, (255, 30, 40), spots * 0.5)
        if tip:
            m, _ = flame_shape(H, W, r, 0.45, 2, 1.6)
            a = m.astype(float)
        else:
            rag = noise2(H, W, r, 2, 1) > 0.8
            a = (~(rag & ((xx == 0) | (xx == W - 1)))).astype(float)
    return col, a, g


def shade_blade(c, face, W, H, r):
    col, a, _ = solid(BLACK, INK, 0.4, 0.0)(c, face, W, H, r)
    g = None
    if face in ('north', 'south'):
        m = cracks(H, W, r, 2, 0.6)
        col[m >= 1] = (230, 20, 30)
        g = glow_img(H, W, (255, 20, 40), (m >= 1) * 0.9)
    return col, a, g


def aura(c, face, W, H, r):
    S = 3
    yy, xx = np.mgrid[0:H * S, 0:W * S] / S
    u = (xx - W / 2) / (W / 2)
    v = yy / H
    n = noise2(H * S, W * S, r, 3 * S, 3)
    inside = (np.abs(u) < 0.35 + 0.35 * n) & (n > 0.45)
    a = (inside.reshape(H, S, W, S).mean((1, 3)) > 0.5).astype(float)
    col = np.zeros((H, W, 3)) + (40, 0, 8)
    return col, a, glow_img(H, W, (200, 10, 30), a * 0.55)


mats = {
    'red': red_skin, 'red_abs': lambda c, f, W, H, r: red_skin(c, f, W, H, r, True), 'red_chest': lambda c, f, W, H, r: red_skin(c, f, W, H, r, True),
    'red_dark': solid(((60, 6, 10), (110, 14, 20), (160, 40, 40)), INK, 0.3, 0.2), 'red_leg': solid(((50, 6, 10), (100, 14, 20), (150, 36, 36)), INK, 0.4, 0.2),
    'mask': solid(BLACK, INK, 0.3, 0.2), 'face': eyes_face(solid(BLACK, INK, 0.2, 0.0), eye=(255, 40, 40), glow=(255, 30, 40), slant=1),
    'white': solid(WHITE, (90, 90, 96), 0.3, 0.25), 'black': solid(BLACK, INK, 0.4, 0.1), 'silver': solid(((120, 120, 130), (190, 190, 200), (240, 240, 250)), INK, 0.3, 0.3),
    'gauntlet': pattern(solid(BLACK, INK, 0.4, 0.1), RED, 3.5, 0.72), 'claw': solid(((140, 20, 26), (210, 40, 40), (250, 120, 110)), INK, 0.2, -0.4),
    'claw_foot': pattern(solid(BLACK, INK, 0.3, 0.2), RED, 3, 0.7), 'pants': solid(BLACK, INK, 0.45, 0.2),
    'horn': solid(((4, 4, 6), (18, 16, 20), (60, 56, 64)), INK, 0.2, -0.4), 'hair': solid(WHITE, (120, 120, 126), 0.5, -0.3),
    'tail': tail, 'tail_tip': lambda c, f, W, H, r: tail(c, f, W, H, r, True), 'black_cloth': tail,
    'hilt': solid(((30, 10, 12), (70, 20, 24), (110, 40, 40)), INK, 0.3, 0.0), 'shade_blade': shade_blade, 'aura': aura,
}
P = painter_with(M, mats)


def dress(a, length, amp, speed, flare, back, lift):
    for i, names in enumerate(TAILS):
        wave_chain(a, names, length, amp * 1.0, speed, phase=i * 1.9, axis=(1.0, 0.2, 0.6), lift=flare * 0.7 * (1 if i else 1) + back * 0.7 + lift * 0.5, step=1.0)
    for names in SASH:
        wave_chain(a, names, length, amp * 0.6, speed, phase=0.7, axis=(1.0, 0.1, 0.3), lift=-flare * 0.3 + back * 0.4, step=1.0)
    a.scl('waura', *[(length * j / 8, (1 + 0.2 * math.sin(math.tau * j / 8 * speed * 2), 1 + 0.1 * math.cos(math.tau * j / 8 * speed * 2), 1)) for j in range(9)])


CARRY = {'right_arm': (-5, 0, 10), 'right_fore': (-20, 0, 0), 'weapon': (20, 0, -8)}
MOVE = {'right_arm': (10, 0, 14), 'right_fore': (-25, 0, 0), 'weapon': (60, 0, -10)}
F = FormAnims(dress, CARRY, MOVE).locomotion()
F.ability(0, 'blink', length=1.0, vanish=0.2, back=0.4)
F.ability(1, 'flurry', length=1.5, n=5, t0=0.15)
F.ability(2, 'spin', length=1.4, turns=1, t0=0.25, low=True)
F.ability(3, 'stab', length=1.5, t=0.55)
F.ability(4, 'roar', length=2.2, t=0.8)
A = F.A
