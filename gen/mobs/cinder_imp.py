"""Kül İmp (Cinder Imp) - small flying fire imp of the Ember Dominion."""
import math, numpy as np
from core import Model, Anim
from paint import Painter
import mats
from mats import P as pal
from arch import *

ID = 'cinder_imp'
SPEC = dict(
    faction='EMBER', tr='Kül İmp', en='Cinder Imp', hp=22, armor=1, attack=3.5, speed=0.24, size=(0.8, 1.4, 1.0), fly=2.2, reach=2.2,
    voice=('demon', 1.7), egg=(0x2A1E1A, 0xFF8A1E), spawn=(10, 2, 4, ['ember_wastes', 'ashen_barrens']), xp=6, death_ticks=22,
    anim=(2.6, 2.6),
    abilities=[
        dict(kind='MELEE', anim='attack', time=(14, 7), cd=18, dmg=1.0, p=(0.3, 0), color=0xFF7A1A, fx='flame'),
        dict(kind='BOLT', anim='spit', time=(20, 10), cd=70, w=4, range=(4, 18), dmg=1.4, p=(1.0, 0), color=0xFF8A22, fx='flame', los=True),
        dict(kind='DIVE', anim='dive', time=(26, 8, 18), cd=150, w=3, range=(5, 16), dmg=1.6, p=(1.25, 0.8), color=0xFF5A10, fx='flame', air=True),
    ],
    drops=[('minecraft:blaze_powder', 0, 2, 0.6), ('krolasyonbosses:ember_shard', 0, 1, 0.25)],
    lore='Kor Hanedanı\'nın ateşten doğan haberci şeytancıkları. Sürüler hâlinde gelir, kıvılcım tükürür.',
)


def build():
    M = Model(ID, 128, 64)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 14, 0))
    box(M, 'body', (0, 13.5, 0), (7, 8, 5), 'skin')
    box(M, 'body', (0, 18.2, 0.2), (5.4, 3, 4), 'skin_lo')
    box(M, 'body', (0, 11.5, -2.55), (4.6, 3.6, 0.6), 'belly')
    # head
    M.bone('head', 'body', (0, 9.5, -0.5))
    box(M, 'head', (0, 5.2, -0.5), (8, 7, 7), 'head')
    box(M, 'head', (0, 6.9, -4.6), (4, 2.6, 2), 'snout')
    box(M, 'head', (0, 8.9, -3.9), (4.4, 1.2, 3), 'jaw')
    for sx, s in ((-1, 'right'), (1, 'left')):
        chain(M, f'{s}_horn', 'head', [(sx * 2.4, 2.2, -1.5), (sx * 4.0, -1.5, -1.2), (sx * 4.6, -5.0, 0.2), (sx * 3.4, -7.0, 1.8)], (2.8, 2.1, 1.4), ('horn', 'horn', 'horn_tip'), ext=0.3)
        local(M, f'{s}_ear', 'head', (sx * 4.0, 4.0, -0.5), (0, sx * 55, sx * -12))
        M.cube_l(f'{s}_ear', (-3.0, -2.5, 0) if sx < 0 else (-3.0, -2.5, 0), (6, 4.5, 0), 'ear', plane=True)
        # spikes along the brow
    # arms
    for sx, s in ((-1, 'right'), (1, 'left')):
        seg(M, f'{s}_arm', 'body', (sx * 4.4, 11.2, 0), (sx * 5.8, 15.8, -1.4), 2.3, 2.3, 'skin', ext=0.5)
        seg(M, f'{s}_fore', f'{s}_arm', (sx * 5.8, 15.8, -1.4), (sx * 5.2, 19.5, -3.2), 2.0, 2.0, 'skin_lo', ext=0.3)
        zero_bone(M, f'{s}_hand', f'{s}_fore', (sx * 5.2, 19.5, -3.2))
        M.cube_l(f'{s}_hand', (-1.4, -0.2, -1.4), (2.8, 2.2, 2.8), 'skin_lo')
        for k in range(3):
            M.cube_l(f'{s}_hand', (-1.4 + k * 1.0, 1.8, -1.6), (0.8, 2.2, 0.8), 'claw')
    # legs
    for sx, s in ((-1, 'right'), (1, 'left')):
        seg(M, f'{s}_leg', 'body', (sx * 2.0, 18.0, 0.8), (sx * 2.6, 21.3, -0.8), 2.4, 2.4, 'skin', ext=0.4)
        seg(M, f'{s}_shin', f'{s}_leg', (sx * 2.6, 21.3, -0.8), (sx * 2.3, 24.2, 0.6), 2.0, 2.0, 'skin_lo', ext=0.3)
        zero_bone(M, f'{s}_foot', f'{s}_shin', (sx * 2.3, 24.2, 0.6))
        M.cube_l(f'{s}_foot', (-1.4, -0.3, -2.8), (2.8, 1.4, 3.6), 'skin_lo')
        for k in range(3):
            M.cube_l(f'{s}_foot', (-1.4 + k * 1.0, -0.1, -4.2), (0.8, 1.0, 1.6), 'claw')
    # tail with flame tip
    tail_pts = [(0, 17.5, 2.2), (0, 19.0, 6.0), (0, 19.6, 10.0), (0, 18.3, 14.0), (0, 16.0, 17.0)]
    names = chain(M, 'tail', 'body', tail_pts, (2.6, 2.1, 1.7, 1.3), 'tail', ext=0.4)
    zero_bone(M, 'flame', names[-1], tail_pts[-1])
    for k, ry in enumerate((0, 90)):
        M.bone(f'flame{k}', 'flame', (0, 0, 0)); local(M, f'flame{k}', 'flame', (0, 0, 0), (0, ry, 0))
        M.cube_l(f'flame{k}', (-3.2, -9.0, 0), (6.4, 9.0, 0), 'flame', plane=True)
    # wings
    for sx, s in ((-1, 'right_wing'), (1, 'left_wing')):
        shoulder = (sx * 3.4, 11.0, 2.2)
        e1, e2, e3 = (sx * 11.5, 5.0, 3.6), (sx * 19.0, 2.0, 5.2), (sx * 28.0, 5.5, 9.0)
        seg(M, s + '0', 'body', shoulder, e1, 1.8, 1.8, 'bonewing', ext=0.4)
        seg(M, s + '1', s + '0', e1, e2, 1.5, 1.5, 'bonewing', ext=0.3)
        seg(M, s + '2', s + '1', e2, e3, 1.2, 1.2, 'bonewing', ext=0.3)
        # membranes: vertical sheets hanging from each strut
        for i, (pa, pb, hh) in enumerate(((shoulder, e1, 8.5), (e1, e2, 11.5), (e2, e3, 9.5))):
            zero_bone(M, f'{s}_m{i}', s + str(i), (pa[0], pa[1], pa[2]))
            x0, x1 = sorted((pa[0], pb[0]))
            ytop = min(pa[1], pb[1]) + 0.3
            # sheet centred under the strut, plane at the strut's z
            zc = (pa[2] + pb[2]) / 2
            M.cube(f'{s}_m{i}', (x0, ytop, zc), (x1 - x0, hh, 0), 'membrane', plane=True)
    # ---------------- textures
    Pt = Painter(M)
    ash = pal('#1c1411', '#33241d', '#4d372b', '#6e4e3c', '#94705a')
    em = (255, 130, 30)
    Pt.mats['skin'] = mats.skin(ash, 0.3, veins=em, scar=0.4)
    Pt.mats['skin_lo'] = mats.skin(ash, 0.3, veins=em)
    Pt.mats['belly'] = mats.plates(pal('#2a1a12', '#4a2c1c', '#7a4a28', '#b06a30'), rivets=False, band=3, glow_seam=em)
    Pt.mats['head'] = mats.face_on(mats.skin(ash, 0.28, veins=em), 'north',
                                   eyes=[(0.24, 0.40, 0.17, 0.12, 0.35, 'almond')], brow=dict(y=0.28, w=0.2, th=0.06, angle=0.45, col=(8, 4, 4)),
                                   mouth=dict(y=0.78, w=0.36, h=0.09, teeth=6, curve=-0.4, col=(255, 120, 30)), eye_col=(255, 236, 150), glow_col=(255, 200, 70))
    Pt.mats['snout'] = mats.skin(ash, 0.25)
    Pt.mats['jaw'] = mats.skin(ash, 0.25)
    Pt.mats['horn'] = mats.horn(pal('#1a0f0a', '#3a1a10', '#7a2c10', '#c4501a'), 2)
    Pt.mats['horn_tip'] = mats.horn(pal('#3a1a10', '#c4501a', '#ff9a30', '#fff0a0'), 2, tip_glow=(255, 170, 60))
    Pt.mats['claw'] = mats.horn(pal('#0e0a08', '#2a1c14', '#5a4030', '#9a7a58'), 3)
    Pt.mats['ear'] = mats.skin(pal('#241410', '#4a2418', '#7a3a20', '#a85a30'), 0.3, veins=em)
    Pt.mats['tail'] = mats.scales(ash, 2.6, glow_edge=em)
    Pt.mats['flame'] = mats.flame()
    Pt.mats['bonewing'] = mats.horn(pal('#1a0f0a', '#3a2418', '#6a4028', '#a06a40'), 3)
    Pt.mats['membrane'] = mats.membrane(pal('#3a0f0a', '#6a1c10', '#9a2c14', '#c8501e', '#f08a38'), vein_col=(255, 170, 60), scallops=3)

    A = {}
    A.update(flyer_set(M, dict(body='body', head='head', wings=(('right_wing', -1), ('left_wing', 1)), flap=34, tail=names[:3] + [names[3]],
                               legs=('right_leg', 'left_leg'), flap_len=0.42, idle_len=0.9, lean=12), death_len=1.1, drop=22))
    A['attack'] = swing('ATTACK', 0.7, [(0, {}), (0.18, {'body': (-12, 0, 0), 'right_arm': (-30, 0, 0), 'left_arm': (-30, 0, 0), 'head': (-14, 0, 0)}),
                                       (0.34, {'body': (24, 0, 0), 'right_arm': (-110, 0, -10), 'right_fore': (-30, 0, 0), 'left_arm': (-60, 0, 10), 'head': (12, 0, 0)}),
                                       (0.5, {'body': (14, 0, 0), 'right_arm': (-10, 0, 0), 'left_arm': (-100, 0, 10), 'left_fore': (-30, 0, 0)}),
                                       (0.7, {})])
    A['spit'] = swing('SPIT', 1.0, [(0, {}), (0.3, {'body': (-18, 0, 0), 'head': (-24, 0, 0), 'right_arm': (-20, 0, -30), 'left_arm': (-20, 0, 30)}),
                                   (0.5, {'body': (8, 0, 0), 'head': (22, 0, 0), 'right_arm': (-70, 0, 0), 'left_arm': (-70, 0, 0)}),
                                   (1.0, {})])
    A['dive'] = swing('DIVE', 1.3, [(0, {}), (0.4, {'body': (-30, 0, 0), 'head': (-30, 0, 0), 'right_arm': (-40, 0, -40), 'left_arm': (-40, 0, 40), 'right_leg': (-30, 0, 0), 'left_leg': (-30, 0, 0)}),
                                   (0.6, {'body': (70, 0, 0), 'head': (-30, 0, 0), 'right_arm': (-160, 0, 0), 'left_arm': (-160, 0, 0), 'right_leg': (20, 0, 0), 'left_leg': (20, 0, 0)}),
                                   (0.95, {'body': (40, 0, 0), 'right_arm': (-120, 0, 0), 'left_arm': (-120, 0, 0)}),
                                   (1.3, {})])
    return M, Pt, A
