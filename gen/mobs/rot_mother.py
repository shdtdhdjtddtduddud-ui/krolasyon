"""Çürük Ana (Rot Mother) - colossal fungal matriarch: a hollow tree-body, mushroom crown, writhing tendrils and spore sacs."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'rot_mother'
SPEC = dict(
    faction='ROT', tr='Çürük Ana', en='The Rot Mother', hp=560, armor=7, attack=15.0, speed=0.17, size=(2.4, 5.2, 0.95), reach=5.2, kb=1.0, follow=48,
    voice=('hound', 0.5), egg=(0x4A5A2A, 0xFF6AA0), spawn=(0, 1, 1, []), xp=500, death_ticks=80, anim=(1.0, 1.0), boss=('GREEN', True),
    abilities=[
        dict(kind='MELEE', anim='tendril', time=(30, 14, 22), cd=26, dmg=0.85, p=(1.2, 0), color=0x7CB030, fx='spore', eff=('minecraft:poison', 80, 1)),
        dict(kind='SLAM', anim='slam', time=(44, 26), cd=160, w=3, range=(0, 10), dmg=1.3, p=(8.0, 2.0), color=0x6A9A28, fx='spore', eff=('minecraft:slowness', 70, 2)),
        dict(kind='CLOUD', anim='storm', time=(56, 28), cd=210, w=3, range=(0, 18), dmg=0, p=(7.0, 260), color=0xA8C838, fx='spore', eff=('minecraft:poison', 140, 1)),
        dict(kind='ERUPT', anim='roots', time=(48, 24, 30, 36), cd=220, w=3, range=(2, 22), dmg=0.9, p=(3, 9), color=0x4A8A20, fx='spore'),
        dict(kind='SUMMON', anim='sow', time=(54, 28), cd=460, w=2, range=(0, 30), p=(2, 4), summon='spore_shambler', color=0xA8C838, fx='spore'),
        dict(kind='SUMMON', anim='sow', time=(54, 28), cd=420, w=2, range=(0, 30), p=(3, 6), summon='venom_wasp', color=0xE8D030, fx='spore'),
        dict(kind='PULL', anim='vines', time=(40, 20), cd=150, w=3, range=(5, 22), dmg=0.8, p=(2.2, 0), color=0x4A8A20, fx='crit', eff=('minecraft:slowness', 70, 2), los=True),
        dict(kind='BUFF', anim='bloom', time=(50, 22), cd=420, w=1, range=(0, 12), p=(0, 40), color=0xFF6AA0, fx='heart', eff=('minecraft:regeneration', 200, 2)),
        dict(kind='BURST', anim='sporeburst', time=(46, 28), cd=250, w=3, range=(0, 12), dmg=1.1, p=(11, 1.3), color=0xD8E860, fx='spore', eff=('minecraft:nausea', 120, 0), p2=True),
    ],
    drops=[('krolasyonbosses:rot_heart', 1, 1, 1.0), ('krolasyonbosses:rot_sigil', 1, 1, 1.0), ('krolasyonbosses:rot_spore', 8, 16, 1.0), ('krolasyonbosses:hellsteel_ingot', 2, 5, 1.0), ('minecraft:experience_bottle', 4, 8, 1.0)],
    lore='Çürük Divanı\'nın kutsal anası. Bataklığın her mantarı onun ciğeridir; ölen her şey onun çocuklarını besler.',
)

BARK = pal('#14100a', '#2a2012', '#46341c', '#6a502c', '#8e703c')
MOSS = pal('#101a08', '#1e3010', '#34501a', '#5a7e2a', '#88aa40')
CAP = pal('#2a0a1a', '#5a1634', '#8c2450', '#c0407a', '#e878a8')
PINK = pal('#2a0614', '#5c0e2c', '#98194a', '#d4306a', '#ff6a98')
SPORE = (200, 255, 80)


def build():
    M = Model(ID, 256, 256)
    M.bone('base', None, (0, 24, 0))
    # root mound
    box(M, 'base', (0, 21, 0), (34, 6, 34), 'root')
    # gnarled trunk: 4 stacked segments leaning back and forth
    pts = [(0, 20, 0), (-1.5, 6, -1.5), (1.5, -8, 1.5), (-1, -22, -1), (0, -34, 0)]
    names = chain(M, 'trunk', 'base', pts, (22, 19, 17, 14), 'trunk', ext=0.8, depth=0.9)
    # face carved on the third segment: use a front plate
    top = pts[-1]
    zero_bone(M, 'head', names[-1], (0, top[1] + 4, top[2]))
    box(M, 'head', (0, top[1] - 4, top[2] - 1), (14, 14, 12), 'face')
    # mushroom crown: big cap with gills, ring of smaller caps, glowing spots
    box(M, 'head', (0, top[1] - 13, top[2]), (30, 5, 30), 'cap')
    box(M, 'head', (0, top[1] - 17, top[2]), (22, 5, 22), 'cap')
    box(M, 'head', (0, top[1] - 20.5, top[2]), (13, 4, 13), 'cap')
    box(M, 'head', (0, top[1] - 10.2, top[2]), (27, 1.6, 27), 'gills')
    for k in range(8):
        a = k * math.tau / 8
        zero_bone(M, f'cm{k}', 'head', (math.cos(a) * 13, top[1] - 14, top[2] + math.sin(a) * 13))
        M.cube_l(f'cm{k}', (-2.0, -6, -2.0), (4, 6, 4), 'stem')
        M.cube_l(f'cm{k}', (-4.5, -9, -4.5), (9, 3.4, 9), 'cap2')
    # eyes: two glowing lures hanging down from the cap on stalks
    for sx in (-1, 1):
        chain(M, f'lure{sx}_', 'head', [(sx * 6, top[1] - 8, top[2] - 10), (sx * 8, top[1] - 1, top[2] - 13), (sx * 9, top[1] + 5, top[2] - 12)], (2.2, 1.8), 'vine', ext=0.2)
        zero_bone(M, f'bulb{sx}', f'lure{sx}_1', (sx * 9, top[1] + 5, top[2] - 12))
        M.cube_l(f'bulb{sx}', (-2.4, 0, -2.4), (4.8, 4.8, 4.8), 'lure')
    # six tendril arms: 3 segments each, sprouting from the trunk
    arms = []
    for k, (side, zh) in enumerate(((-1, 6), (1, 6), (-1, -10), (1, -10), (-1, -24), (1, -24))):
        z0 = -2 if k < 4 else 0
        pa = (side * 9, zh, z0)
        pts_a = [pa, (side * 20, zh - 6, z0 - 6), (side * 28, zh + 2, z0 - 12), (side * 30, zh + 12, z0 - 16)]
        par = names[0] if zh > 0 else (names[1] if zh > -14 else names[2])
        nm = chain(M, f'arm{k}_', par, pts_a, (6.0, 4.6, 3.4), 'tendril', ext=0.4)
        arms.append(nm)
        zero_bone(M, f'claw{k}', nm[-1], pts_a[-1])
        for c in range(3):
            M.cube_l(f'claw{k}', (-1.2 + c * 1.2, 0, -1), (0.8, 5, 0.8), 'thorn')
    # spore sacs (bulbs) on the back
    for k, (x, y, z) in enumerate(((-6, 8, 9), (6, 6, 10), (0, -6, 10), (-7, -16, 9), (7, -18, 8))):
        zero_bone(M, f'sac{k}', names[min(3, max(0, (y + 30) // 14))] if False else names[1], (x, y, z))
        M.cube_l(f'sac{k}', (-3.4, -3.4, -3.4), (6.8, 6.8, 6.8), 'sac')
        M.cube_l(f'sac{k}', (-1.8, -5.6, -1.8), (3.6, 3, 3.6), 'sac')
    # hanging moss strips on the front
    for k, x in enumerate((-10, -5, 0, 5, 10)):
        zero_bone(M, f'moss{k}', names[2], (x, -10 + (k % 2) * 3, -10))
        M.cube_l(f'moss{k}', (-1.6, 0, -0.4), (3.2, 12 + (k % 3) * 4, 0.8), 'strand')

    Pt = Painter(M)
    Pt.mats['root'] = mats.rock(pal('#1a1208', '#2e2010', '#46341a', '#684c26'), cell=6, crack_w=0.6, moss=MOSS[2])
    Pt.mats['trunk'] = mats.fungus(BARK, spot=(210, 200, 120), spot_n=0.2, glow_spot=SPORE)
    Pt.mats['face'] = mats.face_on(mats.fungus(BARK, spot_n=0.1), 'north', eyes=[(0.2, 0.38, 0.14, 0.12, 0.0, 'round')], mouth=dict(y=0.74, w=0.34, h=0.14, teeth=6, col=(14, 6, 10), curve=0.4),
                                   brow=dict(y=0.24, w=0.22, th=0.08, angle=0.4, col=(10, 14, 4)), eye_col=(255, 120, 190), glow_col=(255, 90, 170))
    Pt.mats['cap'] = mats.fungus(CAP, spot=(250, 235, 210), spot_n=0.3)
    Pt.mats['cap2'] = mats.fungus(CAP, spot=(250, 235, 210), spot_n=0.35)
    Pt.mats['gills'] = mats.fungus(pal('#3a1428', '#6a2448', '#a04070', '#e8b0c8'), spot_n=0, gills=True)
    Pt.mats['stem'] = mats.fungus(pal('#4a4030', '#8a7a58', '#c0b088', '#e8dcb8'), spot_n=0, gills=False)
    Pt.mats['vine'] = mats.scales(pal('#10200a', '#223a12', '#3c5e1e', '#5c8a2c'), 3.0)
    Pt.mats['lure'] = mats.eyeball((255, 120, 190), (255, 230, 240))
    Pt.mats['tendril'] = mats.scales(pal('#14200a', '#2a4214', '#44681e', '#68922c', '#98c24c'), 3.4, shine=0.15)
    Pt.mats['thorn'] = mats.horn(pal('#2a1810', '#5a3820', '#9a6a38', '#d8a860'), 2)
    Pt.mats['sac'] = mats.fungus(pal('#3a4a14', '#5a7a20', '#88aa30', '#c0d860'), spot=(230, 255, 120), spot_n=0.3, glow_spot=SPORE)
    Pt.mats['strand'] = mats.ragged(mats.fur(MOSS, 4), depth=8)

    A = {}
    for name, L, amp in (('IDLE', 4.0, 0.5), ('WALK', 2.0, 1.0)):
        a = Anim(name, L, True)
        for i, n in enumerate(names):
            gen(a, n, 'r', lambda ph, i=i, amp=amp: (2.5 * amp * S(ph, 1, i * 0.12), 3 * S(ph, 1, 0.25 + i * 0.1), 2.5 * S(ph, 1, i * 0.1 + 0.4)), n=10)
        gen(a, 'head', 'r', lambda ph: (2 * S(ph, 2, 0.2), 4 * S(ph, 1, 0.5), 0), n=10)
        for k, nm in enumerate(arms):
            sd = 1 if k % 2 else -1
            for i, n in enumerate(nm[:3]):
                gen(a, n, 'r', lambda ph, i=i, sd=sd, k=k, amp=amp: (6 * amp * S(ph, 1, i * 0.14 + k * 0.09), 0, sd * 8 * S(ph, 1, i * 0.17 + k * 0.11)), n=10)
        for k in range(8):
            gen(a, f'cm{k}', 'r', lambda ph, k=k: (2 * S(ph, 1, k * 0.1), 0, 3 * S(ph, 1, k * 0.13)), n=8)
        for sx in (-1, 1):
            for i in range(2):
                gen(a, f'lure{sx}_{i}', 'r', lambda ph, i=i, sx=sx: (8 * S(ph, 1, i * 0.12 + 0.1 * sx), 0, sx * 6 * S(ph, 1, i * 0.15)), n=10)
        for k in range(5):
            gen(a, f'moss{k}', 'r', lambda ph, k=k: (6 * S(ph, 1, k * 0.15), 0, 5 * S(ph, 1, k * 0.19 + 0.3)), n=8)
        A[name.lower()] = a
    d = Anim('DEATH', 80 / 20, False)
    L = d.length
    fr = [(0, {}), (L * 0.4, {'trunk0': (0, 0, 6), 'trunk1': (-8, 0, 10), 'trunk2': (-20, 0, 8), 'trunk3': (-30, 0, 8), 'head': (-24, 0, 0)}), (L, {'trunk0': (0, 0, 12), 'trunk1': (-28, 0, 16), 'trunk2': (-60, 0, 14), 'trunk3': (-70, 0, 12), 'head': (-50, 0, 0)})]
    for k, nm in enumerate(arms):
        sd = 1 if k % 2 else -1
        fr[1][1][nm[0]] = (20, 0, sd * 30); fr[2][1][nm[0]] = (40, 0, sd * 50)
        fr[2][1][nm[1]] = (30, 0, sd * 40)
    poses(d, fr)
    scales(d, [(0, {'base': (1, 1, 1)}), (L * 0.6, {'base': (1, 0.9, 1)}), (L, {'base': (1.2, 0.18, 1.2)})])
    A['death'] = d
    def arm_pose(f, side=None):
        out = {}
        for k, nm in enumerate(arms):
            if side is not None and (k % 2 == 0) != (side == 'right'):
                continue
            for i, n in enumerate(nm[:3]):
                out[n] = f(i, 1 if k % 2 else -1)
        return out
    A['tendril'] = swing('TENDRIL', 1.5, [(0, {}), (0.4, {'trunk3': (-12, -14, 0), **arm_pose(lambda i, sd: (-40 - i * 10, 0, sd * -30), 'right')}), (0.6, {'trunk3': (14, 16, 0), **arm_pose(lambda i, sd: (50 + i * 6, 0, sd * 10), 'right')}),
                                          (0.85, {'trunk3': (-12, 14, 0), **arm_pose(lambda i, sd: (-40 - i * 10, 0, sd * -30), 'left')}), (1.1, {'trunk3': (14, -16, 0), **arm_pose(lambda i, sd: (50 + i * 6, 0, sd * 10), 'left')}), (1.5, {})])
    A['slam'] = swing('SLAM', 2.2, [(0, {}), (0.8, {'trunk1': (-8, 0, 0), 'trunk2': (-14, 0, 0), 'trunk3': (-18, 0, 0), 'head': (-14, 0, 0), **arm_pose(lambda i, sd: (-90 - i * 20, 0, sd * -20))}),
                                   (1.1, {'trunk1': (10, 0, 0), 'trunk2': (20, 0, 0), 'trunk3': (24, 0, 0), 'head': (16, 0, 0), **arm_pose(lambda i, sd: (60 + i * 6, 0, sd * 10))}), (1.6, {'trunk2': (16, 0, 0), 'trunk3': (20, 0, 0), 'head': (14, 0, 0)}), (2.2, {})])
    A['storm'] = swing('STORM', 2.8, [(0, {}), (0.7, {'trunk3': (-10, 0, 0), 'head': (-22, 0, 0), **arm_pose(lambda i, sd: (-60 - i * 20, 0, sd * -60))}), (2.2, {'trunk3': (-12, 0, 0), 'head': (-26, 0, 0), **arm_pose(lambda i, sd: (-64 - i * 20, 0, sd * -66))}), (2.8, {})])
    A['roots'] = swing('ROOTS', 2.4, [(0, {}), (0.7, {'trunk2': (-10, 0, 0), 'trunk3': (-16, 0, 0), 'head': (-20, 0, 0), **arm_pose(lambda i, sd: (-120, 0, sd * -20))}), (1.0, {'trunk2': (12, 0, 0), 'trunk3': (22, 0, 0), 'head': (14, 0, 0), **arm_pose(lambda i, sd: (70, 0, sd * 10))}), (1.8, {'trunk3': (18, 0, 0), 'head': (12, 0, 0)}), (2.4, {})])
    A['sow'] = swing('SOW', 2.7, [(0, {}), (0.7, {'trunk3': (-12, 0, 0), 'head': (-24, 0, 0), **arm_pose(lambda i, sd: (-70, 0, sd * -50))}), (1.6, {'trunk3': (-14, 0, 0), 'head': (-28, 0, 0), **arm_pose(lambda i, sd: (-76, 0, sd * -56))}), (2.7, {})])
    A['vines'] = swing('VINES', 2.0, [(0, {}), (0.5, {'trunk3': (-8, 0, 0), 'head': (-10, 0, 0), **arm_pose(lambda i, sd: (-60 - i * 14, 0, sd * -40))}), (0.8, {'trunk3': (14, 0, 0), 'head': (12, 0, 0), **arm_pose(lambda i, sd: (55, 0, sd * 8))}), (1.4, {'trunk3': (10, 0, 0)}), (2.0, {})])
    A['bloom'] = swing('BLOOM', 2.5, [(0, {}), (0.6, {'trunk3': (-8, 0, 0), 'head': (-18, 0, 0), **arm_pose(lambda i, sd: (-40, 0, sd * -70))}), (1.8, {'trunk3': (-10, 0, 0), 'head': (-22, 0, 0), **arm_pose(lambda i, sd: (-44, 0, sd * -76))}), (2.5, {})])
    for k in range(8):
        scales(A['bloom'], [(0, {f'cm{k}': (1, 1, 1)}), (0.7, {f'cm{k}': (1.5, 1.5, 1.5)}), (1.8, {f'cm{k}': (1.5, 1.5, 1.5)}), (2.5, {f'cm{k}': (1, 1, 1)})])
    A['sporeburst'] = swing('SPOREBURST', 2.3, [(0, {}), (0.8, {'trunk3': (-14, 0, 0), 'head': (-30, 0, 0), **arm_pose(lambda i, sd: (-50, 0, sd * -80))}), (1.1, {'trunk3': (16, 0, 0), 'head': (16, 0, 0), **arm_pose(lambda i, sd: (30, 0, sd * -10))}), (2.3, {})])
    for k in range(5):
        scales(A['sporeburst'], [(0, {f'sac{k}': (1, 1, 1)}), (0.8, {f'sac{k}': (1.6, 1.6, 1.6)}), (1.0, {f'sac{k}': (0.7, 0.7, 0.7)}), (2.3, {f'sac{k}': (1, 1, 1)})])
    return M, Pt, A
