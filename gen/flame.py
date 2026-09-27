"""Alev Ruhu (Flame Spirit) — a living fire elemental: molten skin with glowing veins, a head of roaring flame, black and
gold armour on the shoulders and legs, flames streaming from the arms, and a blade of pure fire."""
import math, numpy as np
from core import Model
from formlib import Rig, humanoid, FormAnims, wave_chain, R0
from fpaint import solid, veins, pattern, flame_mat, emissive, eyes_face, painter_with, ramp3, outline, organic
from paint import noise2, lerp, glow_img, draw_line, cracks

M = Model('flame', 512, 256)
rig = Rig(M)
S = humanoid(M, chest=((-7.4, -52.5, -4.3), (14.8, 10.5, 8.6)), belly=((-5.0, -43, -3.4), (10.0, 7.5, 6.8)),
             shoulder=(9.0, -49.5, 0.0), arm_w=(4.4, 4.6), fore_w=(3.9, 4.0), elbow=(12.4, -35.5, 1.5), wrist=(14.2, -23, -0.5),
             head=((-3.0, -63.0, -3.2), (6.0, 7.2, 6.4)),
             mats=dict(pelvis='armor', waist='molten_abs', belly='molten_abs', chest='molten_chest', neck='molten', head='molten', face='face',
                       arm='molten', fore='molten', hand='molten', leg='pants', shin='armor_leg', foot='boot'))
# chest harness + big black pauldrons with gold rims
M.cube('chest', (-7.6, -53, -4.5), (15.2, 2.2, 9), 'armor')
M.cube('hips', (-5, -29.8, -3.5), (10, 3, 7), 'armor')
M.cube('hips', (-5.3, -30.4, -3.7), (10.6, 1.0, 7.4), 'gold')
for side, sx in (('right', -1), ('left', 1)):
    pd = f'{side}_pauldron'
    M.bone_w(pd, f'{side}_arm', (sx * 10, -50.5, 0), R0)
    M.cube_l(pd, (-4.4, -3.6, -4.8), (8.8, 6.6, 9.6), 'armor')
    M.cube_l(pd, (-4.7, 2.6, -5.1), (9.4, 1.0, 10.2), 'gold')
    rig.spike(f'{side}_pspike', pd, (sx * 12.5, -53.5, 0), (sx * 15, -59, 1), 2.0, 'armor', tipmat='gold')
    kn = f'{side}_knee'
    M.bone_w(kn, f'{side}_shin', (sx * 4.6, -3, -1.5), R0)
    M.cube_l(kn, (-2.6, -3.2, -3.6), (5.2, 6.4, 2.6), 'armor')
    rig.spike(f'{side}_kspike', kn, (sx * 5.2, -1, -3.6), (sx * 7.5, -8, -6), 2.0, 'armor', tipmat='gold')
    rig.spike(f'{side}_kspike2', kn, (sx * 6.8, -1.5, -1.5), (sx * 10, -4, 0), 1.4, 'gold')
    # flames streaming off the arms
    for k in range(3):
        n = f'{side}_armfire{k}'
        rig.local(n, f'{side}_fore' if k else f'{side}_arm', (0, 4 + k * 5, 1.5), (35, sx * (40 + k * 25), 0))
        M.cube_l(n, (-3, -10, 0), (6, 10, 0), 'flame', plane=True)

# head of roaring flame
HEADFIRE = []
for i, (px, pz, rx, ry, w, h) in enumerate(((0, 0, -15, 0, 8, 14), (0, 0, -15, 90, 7, 12), (-2.5, 2, 20, 30, 6, 12), (2.5, 2, 20, -30, 6, 12),
                                               (0, 3, 35, 0, 7, 11))):
    n = f'headfire{i}'
    rig.local(n, 'head', (px, -6, pz), (rx, ry, 0))
    M.cube_l(n, (-w / 2, -h, 0), (w, h, 0), 'flame_up', plane=True)
    HEADFIRE.append(n)
# flames licking from shoulders and back
BACKFIRE = []
for i, (px, rz) in enumerate(((-6, 25), (6, -25), (0, 0))):
    n = f'backfire{i}'
    rig.local(n, 'chest', (px, -16, 3.5), (25, 0, rz))
    M.cube_l(n, (-4, -12, 0), (8, 12, 0), 'flame_up', plane=True)
    BACKFIRE.append(n)
# fiery sash streaming from the waist
SASHFIRE = [rig.panel_chain('sashfire', 'hips', (3.5, -28, -2), (-10, -60, -15), -3, 6, (10, 11, 10), ('flame_ribbon', 'flame_ribbon', 'flame'))]

# fire blade
rig.local('weapon', 'right_hand', (0, 2.2, 0))
M.cube_l('weapon', (-0.8, -6, -0.8), (1.6, 8.5, 1.6), 'hilt')
M.cube_l('weapon', (-3.2, 2.2, -1.0), (6.4, 1.8, 2.0), 'gold')
M.cube_l('weapon', (-1.6, 4.0, -0.6), (3.2, 30, 1.2), 'fire_blade')
M.cube_l('weapon', (-1.0, 34, -0.5), (2.0, 6, 1.0), 'fire_blade')
rig.local('wfire', 'weapon', (0, 38, 0))
M.cube_l('wfire', (-3.5, -30, 0), (7, 34, 0), 'flame_blade', plane=True)
rig.local('wfire2', 'weapon', (0, 38, 0), (0, 90, 0))
M.cube_l('wfire2', (-2.5, -30, 0), (5, 34, 0), 'flame_blade', plane=True)

# ---------------- textures ----------------
INK = (20, 6, 4)
MOLTEN = ((150, 40, 10), (228, 96, 30), (255, 170, 70))
ARMOR = ((12, 12, 16), (34, 32, 38), (70, 66, 74))
GOLD = ((120, 84, 30), (200, 160, 80), (245, 220, 150))


def molten(c, face, W, H, r, muscle=0.0):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 4)
    t = 0.5 + (n - 0.5) * 0.45 - 0.15 * (yy / max(H - 1, 1) - 0.5)
    if face == 'north' and muscle:
        t = t + muscle * 0.25 * (((yy % 4) == 0) * -1 + (np.abs(xx - (W - 1) / 2) < 0.6) * -1)
    col = ramp3(t, *MOLTEN)
    m = cracks(H, W, r, 3, 0.55)
    core = m >= 1
    col[core] = (255, 235, 150)
    col[(m > 0) & ~core] = lerp(col[(m > 0) & ~core], (255, 200, 90), 0.6)
    outline(col, H, W, (120, 30, 8), 0.4)
    return col, np.ones((H, W)), glow_img(H, W, (255, 170, 60), core * 1.0 + ((m > 0) & ~core) * 0.4 + 0.12)


def molten_chest(c, face, W, H, r):
    col, a, g = molten(c, face, W, H, r, 1.0)
    if face == 'north':
        yy, xx = np.mgrid[0:H, 0:W]
        heart = np.hypot((xx - (W - 1) / 2) / 2.2, (yy - H * 0.35) / 1.6) < 1
        col[heart] = (255, 245, 200)
        g = g.copy()
        g[heart] = (255, 230, 150, 255)
    return col, a, g


def fire_blade(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    t = 0.6 + 0.4 * (1 - np.abs(xx - (W - 1) / 2) / max(W / 2, 1)) + (noise2(H, W, r, 3) - 0.5) * 0.3
    col = ramp3(np.clip(t, 0, 1), (230, 80, 10), (255, 160, 40), (255, 245, 190))
    return col, np.ones((H, W)), glow_img(H, W, (255, 180, 70), np.ones((H, W)))


mats = {
    'molten': molten, 'molten_abs': lambda c, f, W, H, r: molten(c, f, W, H, r, 1.0), 'molten_chest': molten_chest,
    'face': eyes_face(molten, eye=(255, 255, 220), glow=(255, 250, 200), slant=1),
    'armor': pattern(solid(ARMOR, INK, 0.4, 0.2), GOLD, 5, 0.66, INK), 'armor_leg': pattern(solid(ARMOR, INK, 0.4, 0.2), GOLD, 4, 0.62, INK),
    'pants': solid(((14, 14, 18), (36, 34, 42), (64, 60, 70)), INK, 0.4, 0.2), 'boot': solid(((70, 30, 16), (120, 56, 30), (170, 90, 50)), INK, 0.4, 0.2),
    'gold': solid(GOLD, INK, 0.3, 0.3), 'hilt': solid(((30, 20, 16), (70, 44, 30), (110, 70, 44)), INK, 0.4, 0.0),
    'flame': flame_mat(), 'flame_up': flame_mat(((200, 50, 10), (255, 150, 30), (255, 245, 180)), 0.95, 3),
    'flame_ribbon': emissive((255, 140, 30), (255, 130, 30), 0.8, (255, 230, 150)), 'flame_blade': flame_mat(((230, 90, 10), (255, 170, 40), (255, 250, 210)), 1.0, 1),
    'fire_blade': fire_blade,
}
P = painter_with(M, mats)


def flicker(a, names, length, speed, amp=0.35):
    for k, n in enumerate(names):
        a.scl(n, *[(length * j / 8, (1 + 0.1 * amp * math.sin(math.tau * j / 8 * speed * 3 + k), 1 + amp * math.sin(math.tau * j / 8 * speed * 3 + k * 1.7), 1))
                   for j in range(9)])


def dress(a, length, amp, speed, flare, back, lift):
    flicker(a, HEADFIRE + BACKFIRE + ['wfire', 'wfire2'], length, max(1, speed), 0.3 + 0.05 * amp)
    flicker(a, [f'{s}_armfire{k}' for s in ('right', 'left') for k in range(3)], length, max(1, speed), 0.35)
    for i, n in enumerate(HEADFIRE + BACKFIRE):
        a.rot(n, *[(length * j / 8, (back * 0.5 + 5 * amp * math.sin(math.tau * j / 8 * speed + i * 1.3), 0, 4 * amp * math.cos(math.tau * j / 8 * speed)))
                   for j in range(9)])
    for names in SASHFIRE:
        wave_chain(a, names, length, amp, speed * 2, phase=0.4, axis=(0.8, 0.5, 0.8), lift=back * 0.6 + lift, step=0.9)


CARRY = {'right_arm': (-5, 0, 12), 'right_fore': (-25, 0, 0), 'weapon': (35, 0, -10)}
MOVE = {'right_arm': (5, 0, 14), 'right_fore': (-30, 0, 0), 'weapon': (70, 0, -10)}
F = FormAnims(dress, CARRY, MOVE).locomotion()
F.ability(0, 'blink', length=1.1, vanish=0.25, back=0.45)
F.ability(1, 'cast_up', length=1.8, t=0.6)
F.ability(2, 'roar', length=1.8, t=0.7)
F.ability(3, 'flurry', length=1.2, n=3, t0=0.15)
F.ability(4, 'hover', length=4.0, rise=0.5, fall=3.5)
A = F.A
