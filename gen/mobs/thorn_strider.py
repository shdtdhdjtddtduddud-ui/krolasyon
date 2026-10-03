"""Diken Gezgini (Thorn Strider) - spider with thorned legs and a rose-bud abdomen."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'thorn_strider'
SPEC = dict(
    faction='ROT', tr='Diken Gezgini', en='Thorn Strider', hp=36, armor=3, attack=6.0, speed=0.3, size=(1.5, 1.2, 1.0), reach=2.4, kb=0.2, follow=34,
    voice=('hound', 1.5), egg=(0x2A4A1A, 0xE04060), spawn=(9, 1, 3, ['rot_bog', 'gloam_forest']), xp=12, death_ticks=22, anim=(2.4, 3.0), head='head',
    abilities=[
        dict(kind='MELEE', anim='bite', time=(14, 7), cd=16, dmg=1.0, p=(0.4, 0), color=0x7CB030, fx='spore', eff=('minecraft:poison', 60, 0)),
        dict(kind='VOLLEY', anim='thorns', time=(26, 14), cd=120, w=4, range=(4, 16), dmg=0.6, p=(5, 0.7), color=0x6A9A28, fx='crit', los=True),
        dict(kind='LEAP', anim='pounce', time=(26, 9), cd=100, w=4, range=(4, 12), dmg=1.2, p=(1.3, 0.6), color=0x7CB030, fx='spore'),
        dict(kind='CLOUD', anim='web', time=(26, 12), cd=190, w=3, range=(3, 13), dmg=0, p=(3.4, 140), color=0xE8E8E0, fx='spore', eff=('minecraft:slowness', 80, 3)),
    ],
    drops=[('minecraft:string', 1, 3, 0.8), ('minecraft:spider_eye', 0, 1, 0.4), ('krolasyonbosses:rot_spore', 0, 1, 0.4)],
    lore='Çürük Divanı\'nın avcıları. Karınlarındaki tomurcuk, avı yakaladığında açılır; dikenlerinin her biri ayrı bir zehir taşır.',
)

BARK = pal('#0e1408', '#1c2a10', '#304a1c', '#4c722c', '#78a044')
THORN = pal('#2a1810', '#5a3820', '#9a6a38', '#d8a860')
ROSE = pal('#2a0614', '#5c0e2c', '#98194a', '#d4306a', '#ff6a98')


def build():
    M = Model(ID, 128, 128)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 15, 0))
    box(M, 'body', (0, 14.5, -2), (9, 7, 10), 'bark')
    M.bone('head', 'body', (0, 14, -7))
    box(M, 'head', (0, 13.8, -9.5), (7, 6, 6), 'head')
    for sx in (-1, 1):
        zero_bone(M, f'mand{sx}', 'head', (sx * 1.6, 15.5, -12.3))
        M.cube_l(f'mand{sx}', (-0.7, 0, -0.7), (1.4, 4.0, 1.4), 'thorn')
    # rose-bud abdomen: central bulb + 6 petals
    M.bone('abdomen', 'body', (0, 14, 4))
    box(M, 'abdomen', (0, 13.5, 9), (10, 9, 11), 'bark2')
    for k in range(6):
        a = k * math.tau / 6
        world_bone(M, f'petal{k}', 'abdomen', (math.cos(a) * 3.0, 9.5, 9.5 + math.sin(a) * 3.0), 0, -math.degrees(a) + 90, 0)
        local(M, f'petal{k}t', f'petal{k}', (0, 0, 0), (0, 0, 0))
        M.cube_l(f'petal{k}t', (-2.6, -7, 0), (5.2, 7, 0), 'petal', plane=True)
    box(M, 'abdomen', (0, 8.2, 9.5), (4.4, 4.4, 4.4), 'core')
    # abdomen thorns
    for k, (x, z) in enumerate(((-4.4, 6), (4.4, 6), (-4.0, 12), (4.0, 12), (0, 14.5))):
        zero_bone(M, f'ath{k}', 'abdomen', (x, 13.5, z))
        M.cube_l(f'ath{k}', (-0.8, -4.6, -0.8), (1.6, 4.6, 1.6), 'thorn')
    # eight thorned legs: 3 segments each (a: thigh up-out, b: shin down)
    zs = [-5.5, -2.0, 1.5, 5.0]
    for i in range(8):
        side = 1 if i % 2 == 0 else -1     # naming: even = right (x<0), odd = left
        sx = -side
        z0 = zs[i // 2]
        spread = (1.0, 0.45, -0.3, -0.9)[i // 2]
        A_ = (sx * 4.0, 14.8, z0)
        B_ = (sx * 11.5, 9.5, z0 + spread * 3)
        C_ = (sx * 18.0, 16.0, z0 + spread * 8)
        D_ = (sx * 21.0, 24.0, z0 + spread * 10)
        M.seg(f'leg{i}_a', 'body', A_, B_, 1.8, 1.8, 'bark', extend=0.3)
        M.seg(f'leg{i}_b', f'leg{i}_a', B_, C_, 1.6, 1.6, 'bark', extend=0.2)
        M.seg(f'leg{i}_c', f'leg{i}_b', C_, D_, 1.3, 1.3, 'thorn', extend=0.2)
        zero_bone(M, f'leg{i}_t', f'leg{i}_b', B_)
        M.cube_l(f'leg{i}_t', (-0.5, -2.8, -0.5), (1.0, 2.8, 1.0), 'thorn')

    Pt = Painter(M)
    Pt.mats['bark'] = mats.scales(BARK, 3.2, glow_edge=None, shine=0.2)
    Pt.mats['bark2'] = mats.scales(pal('#1c0a14', '#3a1424', '#5c1e3a', '#8c2c58'), 3.4, shine=0.2)
    Pt.mats['thorn'] = mats.horn(THORN, 2)
    Pt.mats['petal'] = mats.feather_sheet(ROSE, tip=(255, 120, 170), glow_tip=None, rows=2, fw=4, tip_depth=4, tip_start=0.7)
    Pt.mats['core'] = mats.solid((255, 90, 150))
    Pt.mats['head'] = mats.face_on(mats.scales(BARK, 3.0), 'north', eyes=[(0.18, 0.3, 0.1, 0.1, 0.0, 'round', True), (0.34, 0.5, 0.06, 0.06, 0.0, 'round', True)],
                                   eye_col=(255, 120, 170), glow_col=(255, 80, 150))

    A = spider_set(M, dict(death_len=22 / 20))
    for nm in ('idle', 'walk', 'run'):
        for i in range(8):
            if f'leg{i}_c' in M.bones:
                gen(A[nm], f'leg{i}_c', 'r', lambda ph, i=i: (0, 0, (1 if i % 2 == 0 else -1) * 10 * max(0.0, C(ph, 1, ((i // 2) % 2) * 0.5))), n=8)
        gen(A[nm], 'abdomen', 'r', lambda ph: (3 * S(ph, 2, 0.1), 0, 0), n=8)
    A['bite'] = swing('BITE', 0.75, [(0, {}), (0.18, {'head': (-20, 0, 0), 'body': (-10, 0, 0), 'mand-1': (0, 0, 22), 'mand1': (0, 0, -22)}), (0.3, {'head': (18, 0, 0), 'body': (12, 0, 0), 'mand-1': (0, 0, -10), 'mand1': (0, 0, 10)}), (0.75, {})])
    A['thorns'] = swing('THORNS', 1.3, [(0, {}), (0.4, {'body': (-16, 0, 0), 'abdomen': (-40, 0, 0), 'head': (-10, 0, 0)}), (0.6, {'body': (6, 0, 0), 'abdomen': (30, 0, 0)}), (1.3, {})])
    for k in range(6):
        scales(A['thorns'], [(0, {f'petal{k}': (1, 1, 1)}), (0.4, {f'petal{k}': (1.3, 1.4, 1.3)}), (0.6, {f'petal{k}': (0.8, 0.8, 0.8)}), (1.3, {f'petal{k}': (1, 1, 1)})])
    A['pounce'] = swing('POUNCE', 1.3, [(0, {}), (0.4, {'body': (-12, 0, 0), 'head': (-8, 0, 0)}), (0.6, {'body': (14, 0, 0), 'head': (10, 0, 0)}), (1.3, {})])
    scales(A['pounce'], [(0, {'base': (1, 1, 1)}), (0.4, {'base': (1.1, 0.8, 1.1)}), (0.55, {'base': (0.95, 1.12, 0.95)}), (1.3, {'base': (1, 1, 1)})])
    A['web'] = swing('WEB', 1.3, [(0, {}), (0.4, {'abdomen': (-36, 0, 0), 'body': (10, 0, 0)}), (0.55, {'abdomen': (14, 0, 0), 'body': (-6, 0, 0)}), (1.3, {})])
    return M, Pt, A
