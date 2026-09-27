"""Güneş Alevi Hükümdarı (Solar Flame Sovereign) — crimson flame-haired emperor in a burning robe with gold ornaments,
carrying a colossal lava-veined greatsword."""
import math, numpy as np
from core import Model, Anim
from formlib import Rig, humanoid, FormAnims, wave_chain, R0
from fpaint import solid, veins, pattern, fire_cloth, flame_mat, emissive, eyes_face, painter_with, ramp3, outline
from paint import noise2, lerp, glow_img

M = Model('solar', 512, 256)
rig = Rig(M)
S = humanoid(M, chest=((-7.6, -52.5, -4.3), (15.2, 10.5, 8.6)), belly=((-5.2, -43, -3.5), (10.4, 7.5, 7)),
             shoulder=(9.0, -49.5, 0.0), arm_w=(4.4, 4.6), fore_w=(3.9, 4.0), elbow=(12.6, -35.5, 1.5), wrist=(14.8, -23, -0.5),
             head=((-3.2, -63.2, -3.3), (6.4, 7.4, 6.6)),
             mats=dict(pelvis='gold', waist='armor', belly='armor', chest='armor_chest', neck='skin', head='skin', face='face',
                       arm='skin_tattoo', fore='skin_tattoo', hand='gold_hand', leg='pants', shin='boot', foot='boot'))

# gold ornaments: collar, sternum gem, belt, bracers, tassets
M.cube('chest', (-5.2, -54.2, -3.6), (10.4, 2.4, 7.2), 'gold')
M.cube('chest', (-1.1, -50.5, -5.0), (2.2, 2.2, 0.8), 'gem')
M.cube('chest', (-3.5, -46, -4.3), (7, 1.2, 0.6), 'gold')
M.cube('hips', (-5.2, -29.8, -3.6), (10.4, 2.4, 7.2), 'gold')
M.cube('hips', (-1.5, -28.5, -4.1), (3, 5, 0.6), 'gold')
for side, sx in (('right', -1), ('left', 1)):
    # curling gold pauldron with swept flame horns
    pd = f'{side}_pauldron'
    M.bone_w(pd, f'{side}_arm', (sx * 9.8, -50, 0), R0)
    M.cube_l(pd, (-3.8, -3.2, -4.4), (7.6, 4.6, 8.8), 'armor')
    M.cube_l(pd, (-4.2, -3.8, -4.8), (8.4, 1.4, 9.6), 'gold')
    rig.spike(f'{side}_ph0', pd, (sx * 12, -52.5, -1), (sx * 17, -58.5, 1.5), 2.4, 'gold')
    rig.spike(f'{side}_ph1', pd, (sx * 10.5, -53, 2), (sx * 13, -60, 5), 2.0, 'gold')
    rig.spike(f'{side}_ph2', pd, (sx * 13, -49, 0), (sx * 16.5, -45, 2), 1.8, 'gold')
    L = M.bones[f'{side}_fore'].cubes[0].size[1] - 2.0
    M.cube_l(f'{side}_fore', (-2.4, L * 0.55, -2.4), (4.8, L * 0.45, 4.8), 'gold')
    # knee plates
    kn = f'{side}_knee'
    M.bone_w(kn, f'{side}_shin', (sx * 4.6, -3, -1.5), R0)
    M.cube_l(kn, (-2.5, -2.4, -3.4), (5, 4.8, 2.2), 'gold')
    M.cube_l(f'{side}_foot', (-2.6, 5.8, -5.4), (5.2, 1.4, 7.8), 'gold')

# spiky crimson hair on top, huge flowing mane behind
HAIRSPIKES = [((0, -63, -1), (0, -70, 3), 2.6), ((-2, -63, -0.5), (-6.5, -69, 3), 2.3), ((2, -63, -0.5), (6.5, -69, 3), 2.3),
              ((-3, -62, 0.5), (-9.5, -65, 4), 2.0), ((3, -62, 0.5), (9.5, -65, 4), 2.0), ((0, -62.5, 2.5), (0, -68, 9), 2.4),
              ((-1.8, -63, -2.5), (-2.5, -67.5, -5.5), 1.8), ((1.8, -63, -2.5), (3.5, -66.5, -5.5), 1.6)]
for i, (A, B, w) in enumerate(HAIRSPIKES):
    rig.spike(f'hs{i}', 'head', A, B, w, 'hair', tipmat='hair_tip')
MANE = []
for i, (px, ry, w) in enumerate(((-3.2, 18, 7), (0, 0, 8), (3.2, -18, 7), (-1.5, 35, 6), (1.5, -35, 6))):
    names = rig.panel_chain(f'mane{i}_', 'head', (px, -61.5, 3.3), (12 + i * 2, ry, 0), -w / 2, w, (12, 14, 14), ('mane', 'mane', 'mane_tip'))
    MANE.append(names)

# long flame scarf looping over the shoulders and trailing behind
SCARF = [(-9, -54, 3), (-4, -58, 7), (4, -58, 7), (11, -53, 5), (17, -47, 9), (23, -40, 14), (28, -33, 17)]
scarf = rig.chain('scarf', 'chest', SCARF, (3.2, 3.2, 3.2, 3.0, 2.6, 2.2), 'scarf', depth_ratio=0.35, ext=0.7)
rig.local('scarf_end', scarf[-1], (0, 6, 0))
M.cube_l('scarf_end', (-4, 0, 0), (8, 9, 0), 'flame', plane=True)

# long split robe with flame hem
COAT = [('robe_fr', (-3.6, -27.8, -3.8), (-6, 24, 4), (-6.5, 6.5)), ('robe_fl', (3.6, -27.8, -3.8), (-6, -24, -4), (0, 6.5)),
        ('robe_sr', (-5.2, -27.8, 0), (0, 90, 9), (-4, 8)), ('robe_sl', (5.2, -27.8, 0), (0, -90, -9), (-4, 8)),
        ('robe_b', (0, -27.8, 3.8), (9, 0, 0), (-6.5, 13))]
ROBE = []
for n, piv, rot, (x0, w) in COAT:
    ROBE.append(rig.panel_chain(n, 'hips', piv, rot, x0, w, (20, 22), ('robe_up', 'robe_lo')))
rig.local('robe_front', 'hips', (0, -3, -3.9), (-4, 0, 0))
M.cube_l('robe_front', (-2.2, 0, 0), (4.4, 18, 0), 'loin', plane=True)

# the greatsword, held in the right hand, blade pointing to the ground
rig.local('weapon', 'right_hand', (0, 2.2, 0))
M.cube_l('weapon', (-0.9, -9, -0.9), (1.8, 12, 1.8), 'grip')
M.cube_l('weapon', (-1.4, -12, -1.4), (2.8, 3, 2.8), 'gold')
M.cube_l('weapon', (-5.5, 3, -1.8), (11, 3.2, 3.6), 'gold')          # guard
M.cube_l('weapon', (-1.8, 2.2, -2.4), (3.6, 3.6, 0.8), 'gem')
rig.spike_local('wg0', 'weapon', (-5, 4, 0), (-9.5, -3, 0), 2.2, 'gold')
rig.spike_local('wg1', 'weapon', (5, 4, 0), (9.5, -3, 0), 2.2, 'gold')
M.cube_l('weapon', (-4.4, 6.2, -1.1), (8.8, 42, 2.2), 'blade')
M.cube_l('weapon', (-3.4, 48.2, -1.1), (6.8, 6, 2.2), 'blade')
M.cube_l('weapon', (-2.0, 54.2, -1.0), (4.0, 4, 2.0), 'blade')
for k in range(3):
    rig.local(f'wflame{k}', 'weapon', (0, 12 + k * 13, 0), (0, k * 60, 0))
    M.cube_l(f'wflame{k}', (-6, -8, 0), (12, 8, 0), 'flame', plane=True)

# ---------------- textures ----------------
INK = (30, 6, 6)
SKIN = ((150, 72, 50), (214, 142, 104), (246, 196, 150))
CRIMSON = ((70, 10, 12), (150, 26, 22), (214, 70, 40))
GOLD = ((120, 76, 20), (214, 160, 54), (255, 230, 140))
FLAME = ((170, 30, 10), (255, 120, 20), (255, 236, 150))
skin = solid(SKIN, INK, 0.35, 0.2)
armor = solid(CRIMSON, INK, 0.5, 0.2)


def hair(c, face, W, H, r, tip=False):
    yy, xx = np.mgrid[0:H, 0:W]
    v = yy / max(H - 1, 1)
    strands = 0.5 + 0.5 * np.sin(xx * 2.2 + noise2(H, W, r, 2) * 5)
    t = np.clip(0.35 + 0.35 * strands + (v * 0.5 if tip else 0), 0, 1)
    col = ramp3(t, (110, 10, 12), (206, 36, 26), (255, 150, 60))
    return col, np.ones((H, W)), glow_img(H, W, (255, 120, 40), (v * 0.6 if tip else 0.0) * np.ones((H, W)))


def mane(c, face, W, H, r, tip=False):
    yy, xx = np.mgrid[0:H, 0:W]
    v = yy / max(H - 1, 1)
    vv = v * 0.5 + (0.5 if tip else 0)
    strands = 0.5 + 0.5 * np.sin(xx * 1.9 + noise2(H, W, r, 3) * 6)
    col = ramp3(np.clip(0.25 + vv * 0.8 + (strands - 0.5) * 0.35, 0, 1), (120, 12, 14), (230, 70, 24), (255, 214, 110))
    a = np.ones((H, W))
    if tip and face in ('north', 'south'):
        from fpaint import flame_shape
        m, _ = flame_shape(H, W, r, 0.3, 3)
        a = m.astype(float)
    return col, a, glow_img(H, W, (255, 150, 50), np.clip(vv - 0.3, 0, 1) * 0.8 * a)


def blade(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 5)
    col = ramp3(0.4 + (n - 0.5) * 0.4, (40, 10, 10), (86, 26, 26), (130, 60, 50))
    a = np.ones((H, W))
    g = np.zeros((H, W, 4), np.float32)
    if face in ('north', 'south'):
        cx = (W - 1) / 2 + np.sin(yy * 0.25) * 0.8
        vein = np.abs(xx - cx) < 0.9 + 0.5 * np.sin(yy * 0.5)
        col[vein] = (255, 200, 60)
        halo = (np.abs(xx - cx) < 1.8) & ~vein
        col[halo] = lerp(col[halo], (255, 90, 20), 0.6)
        from paint import cracks
        cr = cracks(H, W, r, 3, 0.35) >= 1
        col[cr] = (230, 60, 20)
        g = glow_img(H, W, (255, 150, 40), vein * 1.0 + halo * 0.4 + cr * 0.6)
    outline(col, H, W, INK, 0.5)
    return col, a, g


mats = {
    'skin': skin, 'skin_tattoo': veins(skin, (200, 30, 20), (255, 60, 20), 2, 0.5),
    'face': eyes_face(skin, eye=(255, 236, 150), glow=(255, 210, 90), mark=(200, 20, 20)),
    'armor': pattern(armor, ((60, 8, 10), (110, 18, 16), (150, 30, 24)), 3.5, 0.55, INK),
    'armor_chest': pattern(armor, ((50, 8, 10), (95, 16, 14), (140, 30, 22)), 3.0, 0.5, INK),
    'gold': solid(GOLD, INK, 0.35, 0.3), 'gold_hand': veins(solid(GOLD, INK, 0.3, 0.2), (255, 240, 170), (255, 200, 80), 1, 0.6),
    'gem': emissive((255, 60, 30), (255, 90, 30)), 'pants': solid(((14, 10, 12), (36, 22, 22), (70, 40, 36)), INK, 0.4, 0.3),
    'boot': pattern(solid(((70, 14, 12), (130, 34, 24), (180, 70, 40)), INK, 0.4, 0.2), GOLD, 4, 0.62, INK),
    'hair': lambda c, f, W, H, r: hair(c, f, W, H, r), 'hair_tip': lambda c, f, W, H, r: hair(c, f, W, H, r, True),
    'mane': lambda c, f, W, H, r: mane(c, f, W, H, r), 'mane_tip': lambda c, f, W, H, r: mane(c, f, W, H, r, True),
    'scarf': veins(solid(((120, 14, 12), (220, 50, 24), (255, 140, 50)), INK, 0.4, 0.0), (255, 200, 90), (255, 140, 40), 1, 0.4),
    'flame': flame_mat(), 'robe_up': fire_cloth(CRIMSON, FLAME, INK, 2, 1.0, 0.4, top=1.2),
    'robe_lo': fire_cloth(CRIMSON, FLAME, INK, 2, 0.5, 0.6, top=0.55), 'loin': fire_cloth(((10, 8, 10), (30, 20, 22), (60, 30, 30)), FLAME, INK, 1, 0.7, 0.5, top=0.7),
    'grip': solid(((14, 10, 10), (40, 24, 20), (70, 44, 30)), INK, 0.4, 0.0), 'blade': blade,
}
P = painter_with(M, mats)

# ---------------- animations ----------------
HAIRS = [f'hs{i}' for i in range(len(HAIRSPIKES))]


def dress(a, length, amp, speed, flare, back, lift):
    for i, names in enumerate(MANE):
        wave_chain(a, names, length, amp * 0.8, speed, phase=i * 1.1, axis=(1.0, 0.2, 0.5), lift=lift * 1.5 + back * 0.6, step=0.9)
    for i, names in enumerate(ROBE):
        front = i < 2
        wave_chain(a, names, length, amp * 0.7, speed, phase=i * 1.3, axis=(1.0, 0.1, 0.4), lift=(-flare if front else flare) * 0.6 + back * 0.5, step=1.0)
    wave_chain(a, scarf + ['scarf_end'], length, amp * 0.9, speed, phase=0.5, axis=(0.8, 0.6, 0.8), lift=back * 0.3, step=0.7, grow=1.15)
    for k in range(3):
        a.scl(f'wflame{k}', *[(length * j / 8, (1, 1 + 0.35 * math.sin(math.tau * j / 8 * speed * 2 + k * 2), 1)) for j in range(9)])


CARRY = {'right_arm': (-20, 0, 18), 'right_fore': (-55, 0, 0), 'weapon': (60, 0, -10)}
MOVE = {'right_arm': (-15, 0, 20), 'right_fore': (-50, 0, 0), 'weapon': (95, 0, -15)}
F = FormAnims(dress, CARRY, MOVE).locomotion()
F.ability(0, 'slam', length=1.4, hit=0.55)
F.ability(1, 'spin', length=1.8, turns=2, t0=0.3)
F.ability(2, 'stab', length=1.6, t=0.55)
F.ability(3, 'thrust', length=1.1, hit=0.3)
F.ability(4, 'cast_up', length=1.8, t=0.7)
A = F.A
