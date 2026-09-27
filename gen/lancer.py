"""Cehennem Mızrakçısı (Inferno Lancer) — a slender crimson demon covered in flame-shaped spines, long gold-tipped horns,
twin whip tails ending in golden arrowheads and a long spiked trident with a burning head."""
import math, numpy as np
from core import Model
from formlib import Rig, humanoid, FormAnims, wave_chain, R0
from fpaint import solid, veins, pattern, flame_mat, emissive, eyes_face, painter_with, ramp3, outline, organic
from paint import noise2, lerp, glow_img, draw_line, cracks

M = Model('lancer', 512, 256)
rig = Rig(M)
S = humanoid(M, chest=((-6.6, -52.5, -3.9), (13.2, 10.5, 7.8)), belly=((-4.4, -43, -3.0), (8.8, 7.5, 6.0)),
             waist=((-2.8, -36.5, -2.4), (5.6, 9.0, 4.8)), shoulder=(8.2, -49.5, 0.0), arm_w=(3.4, 3.6), fore_w=(3.0, 3.1),
             elbow=(11.8, -35, 1.5), wrist=(13.4, -21, -0.5), leg_w=(4.0, 4.4), shin_w=(3.3, 3.6),
             head=((-2.8, -63.0, -3.0), (5.6, 7.2, 6.0)),
             mats=dict(pelvis='demon', waist='demon_abs', belly='demon_abs', chest='demon_chest', neck='demon', head='demon_head', face='face',
                       arm='demon', fore='demon', hand='demon_dark', leg='demon', shin='demon', foot='demon_dark'))
# skull-like jaw, horns
M.cube('head', (-2.0, -57.6, -3.6), (4.0, 1.8, 1.0), 'teeth')
for side, sx in (('right', -1), ('left', 1)):
    rig.spike(f'{side}_horn0', 'head', (sx * 2.0, -62.8, -0.5), (sx * 4.2, -70.5, 1.5), 2.2, 'horn')
    rig.spike(f'{side}_horn1', f'{side}_horn0', (sx * 4.2, -70.5, 1.5), (sx * 3.8, -78, 4), 1.4, 'horn_tip')
    # flame-shaped spines along the limbs and shoulders
    for k in range(3):
        rig.spike(f'{side}_sspine{k}', 'chest', (sx * (6 + k * 1.2), -52 + k * 1.5, 1), (sx * (8.5 + k * 1.8), -57.5 + k * 1.8, 3.5 + k), 1.5, 'spine')
    for k in range(3):
        rig.spike(f'{side}_aspine{k}', f'{side}_fore', (sx * (12.2 + k * 0.4), -32 + k * 3.5, 1.5), (sx * (14.5 + k * 0.6), -35 + k * 3.5, 5.5), 1.2, 'spine')
        rig.spike(f'{side}_lspine{k}', f'{side}_shin', (sx * (5 + k * 0.3), 1 + k * 5, 1.5), (sx * (6.5 + k * 0.4), -2 + k * 5, 5.5), 1.2, 'spine')
        rig.spike(f'{side}_tspine{k}', f'{side}_leg', (sx * (4.4 + k * 0.4), -20 + k * 5.5, 2.2), (sx * (6.5 + k * 0.4), -23 + k * 5.5, 6.0), 1.3, 'spine')
    for i, fz in enumerate((-1.3, 0, 1.3)):
        rig.local(f'{side}_f{i}', f'{side}_hand', (0, 3.9, fz), (0, 0, sx * -10))
        M.cube_l(f'{side}_f{i}', (-0.45, 0, -0.45), (0.9, 4.5, 0.9), 'claw')
for k in range(5):
    rig.spike(f'backspine{k}', 'chest', (0, -52 + k * 3.4, 3.8), (0, -54.5 + k * 3.4, 8.5), 1.8 - k * 0.15, 'spine')
for k in range(4):
    rig.spike(f'mane{k}', 'head', ((k - 1.5) * 1.6, -62.5, 2.5), ((k - 1.5) * 3.2, -65, 7.5), 1.6, 'spine')

# twin whip tails with golden arrowheads
TAILS = []
for side, sx, pts in (('right', -1, [(-1.5, -26, 3), (-6, -24, 11), (-13, -20, 16), (-20, -12, 17), (-25, -2, 13), (-27, 8, 6), (-25, 15, -2)]),
                      ('left', 1, [(1.5, -26, 3), (7, -23, 10), (15, -21, 14), (23, -17, 14), (30, -11, 10), (34, -3, 3), (35, 6, -3)])):
    names = rig.chain(f'{side}_tail', 'hips', pts, (2.2, 2.0, 1.8, 1.6, 1.4, 1.2), 'tail', ext=0.5)
    rig.local(f'{side}_arrow', names[-1], (0, 4, 0))
    M.cube_l(f'{side}_arrow', (-3, -1, -0.5), (6, 6, 1.0), 'arrow')
    TAILS.append(names + [f'{side}_arrow'])

# long spiked trident, held at the middle with the burning head pointing down
rig.local('weapon', 'right_hand', (0, 2.0, 0))
M.cube_l('weapon', (-0.8, -40, -0.8), (1.6, 72, 1.6), 'shaft')
for k in range(7):
    rig.local(f'wspk{k}', 'weapon', (0, -36 + k * 10, 0), (0, k * 70, 40 if k % 2 else -40))
    M.cube_l(f'wspk{k}', (-0.4, -3.5, -0.4), (0.8, 3.5, 0.8), 'spine')
M.cube_l('weapon', (-5.5, 32, -1.0), (11, 2.4, 2.0), 'trident')
for i, x in enumerate((-4.6, 0, 4.6)):
    L = 15 if i == 1 else 12
    M.cube_l('weapon', (x - 0.9, 34, -0.9), (1.8, L, 1.8), 'trident')
    M.cube_l('weapon', (x - 0.5, 34 + L, -0.5), (1.0, 3, 1.0), 'trident_tip')
for k in range(3):
    rig.local(f'wflame{k}', 'weapon', (0, 44, 0), (0, k * 60, 0))
    M.cube_l(f'wflame{k}', (-6, -12, 0), (12, 14, 0), 'flame', plane=True)
rig.local('wtop', 'weapon', (0, -40, 0))
M.cube_l('wtop', (-2, -6, 0), (4, 7, 0), 'flame', plane=True)

# ---------------- textures ----------------
INK = (24, 4, 4)
RED = ((80, 8, 8), (168, 24, 18), (222, 70, 40))


def demon(c, face, W, H, r, abs_=False):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 3)
    ridge = 0.5 + 0.5 * np.sin(xx * 1.6 + yy * 0.8 + noise2(H, W, r, 5, 1) * 5)
    t = 0.45 + (n - 0.5) * 0.4 + (ridge - 0.5) * 0.35 - 0.15 * (yy / max(H - 1, 1) - 0.5)
    col = ramp3(t, *RED)
    m = cracks(H, W, r, 2, 0.4)
    col[m >= 1] = (255, 120, 40)
    if abs_ and face == 'north':
        col[np.abs(xx - (W - 1) / 2) < 0.6] = (255, 110, 30)
    outline(col, H, W, INK, 0.5)
    glow = (m >= 1) * 0.7 + (abs_ and face == 'north') * (np.abs(xx - (W - 1) / 2) < 0.6) * 0.8
    return col, np.ones((H, W)), glow_img(H, W, (255, 110, 40), glow)


mats = {
    'demon': demon, 'demon_abs': lambda c, f, W, H, r: demon(c, f, W, H, r, True), 'demon_chest': lambda c, f, W, H, r: demon(c, f, W, H, r, True),
    'demon_dark': solid(((50, 6, 6), (100, 16, 14), (150, 40, 30)), INK, 0.3, 0.2), 'demon_head': solid(((70, 8, 8), (140, 20, 16), (200, 60, 40)), INK, 0.35, 0.2),
    'face': eyes_face(solid(((70, 8, 8), (140, 20, 16), (200, 60, 40)), INK, 0.3, 0.0), eye=(255, 230, 120), glow=(255, 200, 80)),
    'teeth': solid(((170, 160, 140), (220, 214, 196), (250, 248, 236)), INK, 0.2, 0.0),
    'horn': solid(((70, 10, 10), (150, 30, 20), (220, 90, 50)), INK, 0.2, -0.5), 'horn_tip': solid(((200, 120, 30), (250, 200, 90), (255, 245, 180)), INK, 0.2, -0.3),
    'spine': solid(((90, 10, 10), (190, 40, 24), (255, 120, 60)), INK, 0.2, -0.5), 'claw': solid(((200, 40, 30), (240, 90, 60), (255, 180, 140)), INK, 0.2, -0.4),
    'tail': solid(((70, 8, 8), (150, 24, 18), (210, 70, 40)), INK, 0.35, 0.0), 'arrow': emissive((255, 200, 80), (255, 180, 60), 0.9),
    'shaft': solid(((40, 6, 6), (100, 16, 14), (150, 40, 30)), INK, 0.3, 0.0), 'trident': solid(((150, 30, 16), (230, 90, 30), (255, 170, 80)), INK, 0.3, -0.3),
    'trident_tip': emissive((255, 210, 100), (255, 180, 60), 1.0), 'flame': flame_mat(),
}
P = painter_with(M, mats)


def dress(a, length, amp, speed, flare, back, lift):
    for i, names in enumerate(TAILS):
        wave_chain(a, names, length, amp * 1.1, speed, phase=i * 2.3, axis=(0.8, 0.8, 0.6), lift=lift * 0.5 + back * 0.3, step=0.7, grow=1.1)
    for k in range(3):
        a.scl(f'wflame{k}', *[(length * j / 8, (1, 1 + 0.35 * math.sin(math.tau * j / 8 * speed * 2 + k * 2), 1)) for j in range(9)])


CARRY = {'right_arm': (-25, 0, 15), 'right_fore': (-50, 0, 0), 'weapon': (30, 0, -35)}
MOVE = {'right_arm': (-20, 0, 15), 'right_fore': (-60, 0, 0), 'weapon': (80, 0, -30)}
F = FormAnims(dress, CARRY, MOVE).locomotion()
F.ability(0, 'throw', length=1.3, release=0.55)
F.ability(1, 'thrust', length=1.3, hit=0.3)
F.ability(2, 'spin', length=1.4, turns=2, t0=0.25, low=True)
F.ability(3, 'stab', length=1.6, t=0.55)
F.ability(4, 'hover', length=3.4, rise=0.5, fall=2.9)
A = F.A
