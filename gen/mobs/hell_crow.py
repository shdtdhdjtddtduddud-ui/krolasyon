"""Cehennem Kuzgunu (Hell Crow) - ember-eyed carrion crow that hunts in flocks."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'hell_crow'
SPEC = dict(
    faction='OUTCAST', tr='Cehennem Kuzgunu', en='Hell Crow', hp=18, armor=0, attack=3.5, speed=0.3, size=(0.7, 0.7, 1.0), fly=2.2, reach=1.8, follow=40,
    voice=('hound', 1.9), egg=(0x14101A, 0xFF5A20), spawn=(12, 3, 5, ['ember_wastes', 'ashen_barrens', 'bone_marches', 'ossuary_fields']), xp=5, death_ticks=18, anim=(3.0, 3.0), head='head',
    abilities=[
        dict(kind='MELEE', anim='peck', time=(12, 6), cd=14, dmg=1.0, p=(0.2, 0), color=0xFF6A20, fx='flame'),
        dict(kind='DIVE', anim='dive', time=(22, 6, 15), cd=100, w=4, range=(4, 15), dmg=1.2, p=(1.3, 0.8), color=0xFF6A20, fx='flame', air=True),
        dict(kind='VOLLEY', anim='darts', time=(24, 12), cd=140, w=3, range=(4, 16), dmg=0.5, p=(3, 0.5), color=0x7A5AA0, fx='ash', los=True),
        dict(kind='BURST', anim='caw', time=(30, 14), cd=240, w=1, range=(0, 6), dmg=0.2, p=(5, 0.1), color=0xFF9A60, fx='flame', eff=('minecraft:blindness', 40, 0)),
    ],
    drops=[('minecraft:feather', 1, 3, 0.9), ('minecraft:coal', 0, 2, 0.4)],
    lore='Savaş alanlarının sürekli yoldaşı. Kor gibi parlayan gözleri, ölmek üzere olanı çok önceden bilir.',
)

FEA = pal('#08060c', '#14101c', '#241c32', '#3a2e50', '#5a4a80')


def build():
    M = Model(ID, 128, 64)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 14, 0))
    box(M, 'body', (0, 14, 0), (6, 6, 11), 'plume')
    box(M, 'body', (0, 14.6, -4.8), (5, 5, 3), 'plume2')
    M.bone('head', 'body', (0, 12.2, -5.8))
    box(M, 'head', (0, 11.4, -7.4), (4.8, 4.6, 4.8), 'head')
    box(M, 'head', (0, 12.0, -10.6), (2.0, 2.0, 3.8), 'beak')
    box(M, 'head', (0, 13.2, -10.0), (1.6, 0.8, 3.0), 'beak')
    for k in range(3):
        zero_bone(M, f'crest{k}', 'head', (0, 9.2, -6 + k * 1.4))
        M.cube_l(f'crest{k}', (-0.4, -2.4 - (k == 1) * 0.8, -0.4), (0.8, 2.4 + (k == 1) * 0.8, 1.4), 'plume')
    for sx in (-1, 1):
        seg(M, f'{"left" if sx > 0 else "right"}_leg', 'body', (sx * 1.6, 16.5, 1.5), (sx * 1.8, 20.5, 2.0), 1.0, 1.0, 'beak', ext=0.2)
        zero_bone(M, f'{"left" if sx > 0 else "right"}_foot', f'{"left" if sx > 0 else "right"}_leg', (sx * 1.8, 20.5, 2.0))
        for k in range(3):
            M.cube_l(f'{"left" if sx > 0 else "right"}_foot', (-1 + k * 0.8, 0, -2.4), (0.5, 0.7, 2.8), 'beak')
    for k in range(5):
        world_bone(M, f'tf{k}', 'body', (0, 13.5, 5.2), 0, (k - 2) * 12, 0)
        M.cube_l(f'tf{k}', (-1.4, 0, 0), (2.8, 0.5, 9 - abs(k - 2)), 'plume')
    for sx, s in ((-1, 'right_wing'), (1, 'left_wing')):
        sh = (sx * 3.2, 12.4, -1.5)
        e1, e2, e3 = (sx * 10, 8.5, -0.5), (sx * 17, 7.5, 1.5), (sx * 24, 11.5, 4.5)
        seg(M, s + '0', 'body', sh, e1, 1.6, 1.6, 'bone', ext=0.3)
        seg(M, s + '1', s + '0', e1, e2, 1.3, 1.3, 'bone', ext=0.3)
        seg(M, s + '2', s + '1', e2, e3, 1.0, 1.0, 'bone', ext=0.3)
        for i, (pa, pb, hh) in enumerate(((sh, e1, 9), (e1, e2, 13), (e2, e3, 12))):
            zero_bone(M, f'{s}_m{i}', s + str(i), pa)
            x0, x1 = sorted((pa[0], pb[0]))
            M.cube(f'{s}_m{i}', (x0, min(pa[1], pb[1]) + 0.2, (pa[2] + pb[2]) / 2), (x1 - x0, hh, 0), 'wing', plane=True)

    Pt = Painter(M)
    Pt.mats['plume'] = mats.fur(FEA, 3.0, tips=(120, 80, 200))
    Pt.mats['plume2'] = mats.fur(pal('#14101c', '#241c32', '#3a2e50', '#5a4a80', '#8a70c0'), 2.5)
    Pt.mats['beak'] = mats.horn(pal('#14100e', '#2e2420', '#5a4a40', '#8a7464'), 3)
    Pt.mats['bone'] = mats.horn(pal('#0c0a10', '#1c1626', '#30284a', '#4a3e70'), 3)
    Pt.mats['head'] = mats.face_on(mats.fur(FEA, 2.5), 'north', eyes=[(0.26, 0.36, 0.14, 0.14, 0.0, 'round')], eye_col=(255, 150, 60), glow_col=(255, 100, 30))
    Pt.mats['wing'] = mats.feather_sheet(FEA, tip=(255, 110, 40), glow_tip=(255, 90, 30), rows=3, fw=3, tip_depth=4, tip_start=0.75)

    A = flyer_set(M, dict(body='body', head='head', wings=(('right_wing', -1), ('left_wing', 1)), flap=36, flap_len=0.34, idle_len=0.5, lean=10, tail=None, legs=('right_leg', 'left_leg'), bob=0.9), death_len=18 / 20, drop=18)
    A['peck'] = swing('PECK', 0.6, [(0, {}), (0.14, {'head': (-26, 0, 0), 'body': (-10, 0, 0)}), (0.26, {'head': (34, 0, 0), 'body': (14, 0, 0)}), (0.6, {})])
    A['dive'] = swing('DIVE', 1.1, [(0, {}), (0.3, {'body': (-30, 0, 0)}), (0.5, {'body': (60, 0, 0), 'right_wing0': (0, 0, 50), 'left_wing0': (0, 0, -50)}), (0.8, {'body': (40, 0, 0)}), (1.1, {})])
    A['darts'] = swing('DARTS', 1.2, [(0, {}), (0.35, {'body': (-14, 0, 0), 'right_wing0': (0, 0, -30), 'left_wing0': (0, 0, 30)}), (0.55, {'body': (6, 0, 0), 'right_wing0': (0, 0, 22), 'left_wing0': (0, 0, -22)}), (1.2, {})])
    A['caw'] = swing('CAW', 1.5, [(0, {}), (0.3, {'body': (-24, 0, 0), 'head': (-30, 0, 0)}), (1.1, {'body': (-26, 0, 0), 'head': (-34, 0, 0)}), (1.5, {})])
    return M, Pt, A
