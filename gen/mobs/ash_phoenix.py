"""Kül Anka (Ash Phoenix) - great ash-grey firebird with ember-tipped feathers."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'ash_phoenix'
SPEC = dict(
    faction='EMBER', tr='Kül Anka', en='Ash Phoenix', hp=64, armor=3, attack=7.0, speed=0.22, size=(1.5, 1.3, 1.35), fly=3.2, reach=2.8, kb=0.3, follow=40,
    voice=('hound', 1.8), egg=(0x5A5048, 0xFF8A20), spawn=(5, 1, 2, ['ember_wastes', 'ashen_barrens']), xp=22, death_ticks=30, anim=(2.0, 2.0), head='head',
    abilities=[
        dict(kind='MELEE', anim='peck', time=(16, 8), cd=18, dmg=1.0, p=(0.5, 0), color=0xFF8A20, fx='flame'),
        dict(kind='DIVE', anim='dive', time=(30, 10, 22), cd=140, w=4, range=(6, 20), dmg=1.5, p=(1.4, 0.9), color=0xFF5A10, fx='flame', air=True),
        dict(kind='VOLLEY', anim='feathers', time=(30, 16), cd=130, w=4, range=(5, 20), dmg=0.7, p=(7, 0.7), color=0xFF9A30, fx='flame', los=True),
        dict(kind='CONE', anim='breath', time=(44, 14, 24), cd=210, w=3, range=(0, 11), dmg=0.55, p=(24, 0), color=0xFF7A1A, fx='flame'),
    ],
    drops=[('krolasyonbosses:ember_shard', 1, 2, 0.7), ('minecraft:feather', 1, 4, 0.9), ('minecraft:blaze_powder', 0, 2, 0.5)],
    lore='Küllerinden defalarca doğan kuş. Kanat çırpışı sıcak bir fırtına estirir; tüyleri düştüğü yerde kor bırakır.',
)

ASH = pal('#2a2623', '#463f3a', '#6a615a', '#948a80', '#c0b8ac')
FEA = pal('#242120', '#3c3633', '#5c5550', '#857c74', '#aaa196')
EMBER = (255, 120, 24)


def build():
    M = Model(ID, 256, 128)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 12, 0))
    box(M, 'body', (0, 11, 0), (9, 8.5, 15), 'body')
    box(M, 'body', (0, 11.5, -6.4), (7.6, 6, 3), 'chest')
    box(M, 'body', (0, 8.5, -4.6), (4.2, 1.2, 4), 'core_glow')
    # neck + head
    M.bone('neck', 'body', (0, 9, -7))
    seg(M, 'neck0', 'neck', (0, 9, -7), (0, 5.2, -10.8), 4.6, 4.6, 'body', ext=0.5)
    M.bone_w('head', 'neck0', (0, 4.6, -11.6), R0)
    box(M, 'head', (0, 3.4, -12.8), (5.2, 5, 6), 'head')
    box(M, 'head', (0, 4.6, -17.2), (2.6, 2, 3.6), 'beak')
    box(M, 'head', (0, 6.0, -16.4), (2.2, 1.4, 2.4), 'beak')
    box(M, 'head', (0, 6.6, -14.8), (2.2, 1.4, 4.0), 'beak')
    for k, (ry, hh) in enumerate(((0, 9), (55, 8), (-55, 8))):
        world_bone(M, f'crest{k}', 'head', (0, 1.2, -11.6), 0, ry, 0)
        M.cube_l(f'crest{k}', (-2.0, -hh, 0), (4, hh, 0), 'flame', plane=True)
    # wings
    for sx, s in ((-1, 'right_wing'), (1, 'left_wing')):
        sh = (sx * 4.2, 9.5, -1)
        e1, e2, e3 = (sx * 14, 6, 0), (sx * 25, 4.5, 3.0), (sx * 38, 8, 7.5)
        seg(M, s + '0', 'body', sh, e1, 2.4, 2.4, 'bonewing', ext=0.4)
        seg(M, s + '1', s + '0', e1, e2, 2.0, 2.0, 'bonewing', ext=0.3)
        seg(M, s + '2', s + '1', e2, e3, 1.6, 1.6, 'bonewing', ext=0.3)
        for i, (pa, pb, hh) in enumerate(((sh, e1, 13), (e1, e2, 19), (e2, e3, 17))):
            zero_bone(M, f'{s}_m{i}', s + str(i), pa)
            x0, x1 = sorted((pa[0], pb[0]))
            zc = (pa[2] + pb[2]) / 2
            M.cube(f'{s}_m{i}', (x0, min(pa[1], pb[1]) + 0.2, zc), (x1 - x0, hh, 0), 'wingsheet', plane=True)
        # second, shorter row of coverts on top for depth
        zero_bone(M, f'{s}_cov', s + '0', sh)
        x0, x1 = sorted((sh[0], e2[0]))
        M.cube(f'{s}_cov', (x0, min(sh[1], e2[1]) - 0.4, sh[2] - 1.0), (x1 - x0, 7, 0), 'coverts', plane=True)
    # tail: fan of long feathers
    M.bone('tail', 'body', (0, 11, 7))
    for k in range(-3, 4):
        world_bone(M, f'tf{k}', 'tail', (0, 11.5 + abs(k) * 0.3, 7.0), 0, k * 16, 0)
        M.cube_l(f'tf{k}', (-2.0, -0.2, 0.2), (4.4, 0.4, 22 - abs(k) * 3.0), 'feather_tail_h')
    # talons tucked
    for sx, s in ((-1, 'right'), (1, 'left')):
        seg(M, f'{s}_leg', 'body', (sx * 2.2, 14.5, 2), (sx * 2.4, 19.5, 0.5), 2.0, 2.0, 'beak', ext=0.3)
        zero_bone(M, f'{s}_foot', f'{s}_leg', (sx * 2.4, 19.5, 0.5))
        for k in range(3):
            M.cube_l(f'{s}_foot', (-1.5 + k * 1.1, -0.2, -3.2), (0.8, 0.9, 3.6), 'beak')

    Pt = Painter(M)
    Pt.mats['body'] = mats.fur(ASH, 3.0, tips=(255, 130, 30))
    Pt.mats['chest'] = mats.fur(pal('#3a2c24', '#6a4630', '#a8663a', '#d88a40'), 2.5)
    Pt.mats['core_glow'] = mats.flame()
    Pt.mats['head'] = mats.face_on(mats.fur(ASH, 3), 'north', eyes=[(0.26, 0.40, 0.13, 0.14, 0.2, 'round')], eye_col=(255, 230, 120), glow_col=(255, 190, 60))
    Pt.mats['beak'] = mats.horn(pal('#2a1c10', '#5a3a1c', '#a06a2c', '#e0a448'), 3)
    Pt.mats['flame'] = mats.flame((255, 244, 190), (255, 150, 40), (220, 60, 10), 0.7)
    Pt.mats['bonewing'] = mats.horn(pal('#1a1210', '#3a2c26', '#6a5a50', '#9a8a7a'), 4)
    Pt.mats['wingsheet'] = mats.feather_sheet(FEA, tip=(255, 120, 30), glow_tip=(255, 110, 20), rows=3, fw=4, tip_depth=6, tip_start=0.6)
    Pt.mats['coverts'] = mats.feather_sheet(ASH, tip=(255, 150, 50), glow_tip=(255, 110, 20), rows=2, fw=3, tip_depth=2, tip_start=0.75)
    Pt.mats['feather_tail_h'] = mats.feather_sheet(FEA, tip=(255, 120, 30), glow_tip=(255, 100, 20), rows=2, fw=4, tip_depth=3, tip_start=0.6)

    A = flyer_set(M, dict(body='body', head='neck', wings=(('right_wing', -1), ('left_wing', 1)), flap=34, flap_len=0.8, idle_len=1.6, lean=8, tail=['tail'], legs=('right_leg', 'left_leg'), bob=1.8), death_len=1.5, drop=26)
    A['peck'] = swing('PECK', 0.8, [(0, {}), (0.2, {'neck': (-22, 0, 0), 'body': (-8, 0, 0)}), (0.38, {'neck': (36, 0, 0), 'body': (12, 0, 0)}), (0.8, {})])
    A['dive'] = swing('DIVE', 1.6, [(0, {}), (0.45, {'body': (-34, 0, 0), 'neck': (-20, 0, 0), 'right_wing0': (0, 0, -40), 'left_wing0': (0, 0, 40)}),
                                   (0.75, {'body': (60, 0, 0), 'neck': (-30, 0, 0), 'right_wing0': (0, 0, 55), 'left_wing0': (0, 0, -55), 'right_wing1': (0, 0, 40), 'left_wing1': (0, 0, -40), 'right_wing2': (0, 0, 40), 'left_wing2': (0, 0, -40)}),
                                   (1.2, {'body': (40, 0, 0), 'neck': (-20, 0, 0)}), (1.6, {})])
    A['feathers'] = swing('FEATHERS', 1.5, [(0, {}), (0.4, {'body': (-16, 0, 0), 'right_wing0': (0, 0, -34), 'left_wing0': (0, 0, 34), 'right_wing1': (0, 0, -18), 'left_wing1': (0, 0, 18), 'neck': (-10, 0, 0)}),
                                           (0.6, {'body': (6, 0, 0), 'right_wing0': (0, 0, 22), 'left_wing0': (0, 0, -22), 'right_wing1': (0, 0, 14), 'left_wing1': (0, 0, -14)}), (1.5, {})])
    A['breath'] = swing('BREATH', 2.2, [(0, {}), (0.5, {'neck': (-34, 0, 0), 'body': (-14, 0, 0), 'right_wing0': (0, 0, -20), 'left_wing0': (0, 0, 20)}),
                                       (0.7, {'neck': (14, 0, 0), 'body': (4, 0, 0)}), (1.7, {'neck': (14, 8, 0), 'body': (4, 0, 0)}), (2.2, {})])
    return M, Pt, A
