"""Bataklık Ağzı (Bog Maw) - rooted giant carnivorous plant with a toothed maw and grasping vines."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'bog_maw'
SPEC = dict(
    faction='ROT', tr='Bataklık Ağzı', en='Bog Maw', hp=90, armor=3, attack=11.0, speed=0.1, size=(1.6, 2.6, 1.0), reach=3.8, kb=0.9, follow=24,
    voice=('warden', 0.8), egg=(0x3A6A1A, 0xE0306A), spawn=(4, 1, 2, ['rot_bog', 'weeping_marsh']), xp=26, death_ticks=30, anim=(1.0, 1.0), head='head',
    abilities=[
        dict(kind='MELEE', anim='chomp', time=(24, 13), cd=24, dmg=1.0, p=(0.7, 0.2), color=0x7CB030, fx='spore', eff=('minecraft:poison', 80, 1)),
        dict(kind='PULL', anim='vine', time=(30, 14), cd=110, w=5, range=(4, 15), dmg=0.7, p=(2.0, 0), color=0x4A8A20, fx='crit', eff=('minecraft:slowness', 60, 2), los=True),
        dict(kind='CLOUD', anim='spit', time=(30, 14), cd=150, w=3, range=(4, 14), dmg=0, p=(3.4, 150), color=0x90D030, fx='spore', eff=('minecraft:poison', 120, 1)),
        dict(kind='ERUPT', anim='roots', time=(36, 14, 20, 26), cd=240, w=2, range=(3, 14), dmg=0.8, p=(3, 5), color=0x4A8A20, fx='spore'),
    ],
    drops=[('minecraft:vine', 2, 6, 0.9), ('minecraft:slime_ball', 1, 3, 0.6), ('krolasyonbosses:rot_spore', 1, 3, 0.7)],
    lore='Bataklığın dibinden fışkıran açgözlü ağız. Kökleri yerin altında kilometrelerce uzanır; yürümez, bekler.',
)

GREEN = pal('#0c1806', '#1c3a10', '#34621c', '#5a9230', '#8ac850')
PINK = pal('#2a0614', '#5c0e2c', '#98194a', '#d4306a', '#ff6a98')


def build():
    M = Model(ID, 128, 128)
    M.bone('base', None, (0, 24, 0))
    # root mound
    box(M, 'base', (0, 21.5, 0), (18, 5, 18), 'root')
    # stalk: 3 segments curving forward
    pts = [(0, 21, 3), (0, 14, 1.5), (0, 7, -1.5), (0, 0, -5)]
    names = chain(M, 'stalk', 'base', pts, (9, 8, 7), 'stalk', ext=0.6)
    # head: upper & lower jaw hinged at the back
    hp = (0, -1, -3)
    zero_bone(M, 'head', names[-1], hp)
    box(M, 'head', (0, -5.5, -5.5), (13, 6, 13), 'head')
    box(M, 'head', (0, -9.5, -5.5), (15, 2.4, 15), 'lip')
    for k in range(7):
        box(M, 'head', (-5.4 + k * 1.8, -2.4, -11.8), (1.0, 2.6, 1.0), 'tooth')
    zero_bone(M, 'jaw', 'head', (0, 2.0, -2.0))
    M.cube_l('jaw', (-7.0, -1.4, -14), (14, 3.4, 14), 'head')
    M.cube_l('jaw', (-8.0, -0.4, -15), (16, 1.4, 16), 'lip')
    for k in range(7):
        M.cube_l('jaw', (-5.4 + k * 1.8, -3.0, -13.2), (1.0, 2.6, 1.0), 'tooth')
    M.cube_l('jaw', (-4.5, -1.2, -11), (9, 1.0, 9), 'gullet')
    # petals/leaves around the neck
    for k in range(5):
        a = k * math.tau / 5 + 0.3
        world_bone(M, f'leaf{k}', names[1], (math.cos(a) * 4.2, 9.5, 0.5 + math.sin(a) * 4.2), 0, -math.degrees(a) + 90, -35)
        M.cube_l(f'leaf{k}', (-3.8, -10, 0), (7.6, 10, 0), 'leaf', plane=True)
    # eye-bulbs on stalks
    for sx in (-1, 1):
        chain(M, f'eyes{sx}_', 'head', [(sx * 4.5, -8.5, -2), (sx * 5.5, -13.0, -3.5)], (1.6,), 'stalk', ext=0.2)
        zero_bone(M, f'eye{sx}', f'eyes{sx}_0', (sx * 5.5, -13.0, -3.5))
        M.cube_l(f'eye{sx}', (-1.3, -2.2, -1.3), (2.6, 2.6, 2.6), 'eye')
    # grasping vines (2)
    for sx in (-1, 1):
        pts_v = [(sx * 6, 20, 2), (sx * 10, 16, -3), (sx * 13, 11, -8), (sx * 12, 6, -13)]
        chain(M, f'vine{sx}_', 'base', pts_v, (2.6, 2.2, 1.8), 'vine', ext=0.3)
        zero_bone(M, f'vtip{sx}', f'vine{sx}_2', pts_v[-1])
        M.cube_l(f'vtip{sx}', (-1.6, -1.0, -1.6), (3.2, 4.0, 3.2), 'tooth')

    Pt = Painter(M)
    Pt.mats['root'] = mats.rock(pal('#1a1208', '#2e2010', '#46341a', '#684c26'), cell=5, crack_w=0.6, moss=GREEN[2])
    Pt.mats['stalk'] = mats.scales(GREEN, 4.0, shine=0.1)
    Pt.mats['head'] = mats.skin(PINK, 0.4, spots=0.12, vgrad=0.2)
    Pt.mats['lip'] = mats.scales(GREEN, 3.0)
    Pt.mats['gullet'] = mats.solid((40, 6, 20), glow=False)
    Pt.mats['tooth'] = mats.horn(pal('#4a3a20', '#8a7a48', '#d0c080', '#f4ecc0'), 3)
    Pt.mats['leaf'] = mats.feather_sheet(GREEN, tip=(200, 255, 100), rows=2, fw=4, tip_depth=5, tip_start=0.8)
    Pt.mats['eye'] = mats.eyeball((255, 210, 60), (230, 240, 200))
    Pt.mats['vine'] = mats.scales(pal('#10200a', '#223a12', '#3c5e1e', '#5c8a2c'), 3.0)

    A = {}
    for name, L, amp in (('IDLE', 3.0, 0.6), ('WALK', 1.6, 1.0)):
        a = Anim(name, L, True)
        for i, n in enumerate(names):
            gen(a, n, 'r', lambda ph, i=i, amp=amp: (3 * amp * S(ph, 1, i * 0.12), 4 * S(ph, 1, 0.25 + i * 0.1), 3 * S(ph, 1, i * 0.1 + 0.4)), n=10)
        gen(a, 'head', 'r', lambda ph: (2 * S(ph, 2, 0.2), 5 * S(ph, 1, 0.5), 0), n=10)
        gen(a, 'jaw', 'r', lambda ph: (6 + 5 * S(ph, 2), 0, 0), n=10)
        for sx in (-1, 1):
            for i in range(3):
                gen(a, f'vine{sx}_{i}', 'r', lambda ph, i=i, sx=sx: (6 * S(ph, 1, i * 0.12 + 0.1 * sx), 0, sx * 8 * S(ph, 1, i * 0.15)), n=10)
        for k in range(5):
            gen(a, f'leaf{k}', 'r', lambda ph, k=k: (0, 0, 4 * S(ph, 1, k * 0.2)), n=8)
        A[name.lower()] = a
    d = Anim('DEATH', 30 / 20, False)
    L = d.length
    poses(d, [(0, {}), (L * 0.4, {'stalk0': (0, 0, 8), 'stalk1': (-10, 0, 10), 'stalk2': (-30, 0, 8), 'head': (-20, 0, 0), 'jaw': (50, 0, 0)}), (L, {'stalk0': (0, 0, 14), 'stalk1': (-40, 0, 18), 'stalk2': (-70, 0, 12), 'head': (-40, 0, 0), 'jaw': (60, 0, 0)})])
    scales(d, [(0, {'base': (1, 1, 1)}), (L, {'base': (1.1, 0.2, 1.1)})])
    A['death'] = d
    A['chomp'] = swing('CHOMP', 1.3, [(0, {}), (0.4, {'stalk0': (-14, 0, 0), 'stalk1': (-20, 0, 0), 'stalk2': (-24, 0, 0), 'head': (-24, 0, 0), 'jaw': (54, 0, 0)}), (0.6, {'stalk0': (10, 0, 0), 'stalk1': (16, 0, 0), 'stalk2': (22, 0, 0), 'head': (14, 0, 0), 'jaw': (-6, 0, 0)}), (1.0, {'jaw': (10, 0, 0), 'stalk1': (8, 0, 0)}), (1.3, {})])
    A['vine'] = swing('VINE', 1.5, [(0, {}), (0.4, {'vine-1_0': (-30, 0, 20), 'vine-1_1': (-30, 0, 20), 'vine1_0': (-30, 0, -20), 'vine1_1': (-30, 0, -20), 'head': (-10, 0, 0)}),
                                   (0.62, {'vine-1_0': (50, 0, 0), 'vine-1_1': (40, 0, 0), 'vine-1_2': (30, 0, 0), 'vine1_0': (50, 0, 0), 'vine1_1': (40, 0, 0), 'vine1_2': (30, 0, 0), 'head': (12, 0, 0)}), (1.5, {})])
    A['spit'] = swing('SPIT', 1.5, [(0, {}), (0.45, {'stalk0': (-8, 0, 0), 'stalk1': (-18, 0, 0), 'stalk2': (-20, 0, 0), 'head': (-26, 0, 0), 'jaw': (40, 0, 0)}), (0.65, {'stalk1': (12, 0, 0), 'stalk2': (14, 0, 0), 'head': (18, 0, 0), 'jaw': (50, 0, 0)}), (1.5, {})])
    A['roots'] = swing('ROOTS', 1.9, [(0, {}), (0.5, {'stalk0': (-8, 0, 0), 'stalk1': (-14, 0, 0), 'stalk2': (-16, 0, 0), 'head': (-22, 0, 0), 'jaw': (30, 0, 0)}), (1.3, {'stalk0': (-8, 0, 0), 'stalk1': (-14, 0, 0), 'stalk2': (-16, 0, 0), 'head': (-22, 0, 0), 'jaw': (30, 0, 0)}), (1.9, {})])
    return M, Pt, A
