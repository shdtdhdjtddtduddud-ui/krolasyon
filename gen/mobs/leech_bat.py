"""Kan Yarasası (Leech Bat) - bat with a ring-toothed leech maw that drinks blood."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'leech_bat'
SPEC = dict(
    faction='BLOOD', tr='Kan Yarasası', en='Leech Bat', hp=14, armor=0, attack=3.0, speed=0.26, size=(0.7, 0.8, 1.0), fly=1.8, reach=1.8, follow=32,
    voice=('hound', 2.0), egg=(0x3A0A14, 0xFF2040), spawn=(12, 3, 6, ['crimson_gardens', 'blood_spires']), xp=4, death_ticks=18, anim=(3.0, 3.0), head='head',
    abilities=[
        dict(kind='MELEE', anim='bite', time=(12, 6), cd=14, dmg=1.0, p=(0.2, 1.2), color=0xC8102E, fx='drip'),
        dict(kind='DIVE', anim='dive', time=(22, 6, 15), cd=100, w=4, range=(4, 14), dmg=1.2, p=(1.2, 0.8), color=0xC8102E, fx='drip', air=True),
        dict(kind='BURST', anim='screech', time=(28, 12), cd=240, w=1, range=(0, 6), dmg=0.2, p=(5, 0.1), color=0xFF6080, fx='drip', eff=('minecraft:blindness', 40, 0)),
    ],
    drops=[('krolasyonbosses:blood_vial', 0, 1, 0.25), ('minecraft:leather', 0, 1, 0.4)],
    lore='Kan Konseyi\'nın küçük gözcüleri. Kanın kokusunu çok uzaktan alırlar ve sürüler hâlinde yaklaşırlar.',
)

FUR = pal('#0c0306', '#1c070e', '#340e1c', '#541830', '#7a2442')
EM = (255, 30, 60)


def build():
    M = Model(ID, 128, 64)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 14, 0))
    box(M, 'body', (0, 14, 0), (6, 6, 8), 'fur')
    box(M, 'body', (0, 14.6, -3.9), (5, 5, 2), 'chest')
    # head: round with big ears and leech mouth
    M.bone('head', 'body', (0, 11.5, -4.0))
    box(M, 'head', (0, 10.5, -5.6), (6, 5.4, 5.4), 'head')
    box(M, 'head', (0, 11.0, -9.0), (3.6, 3.6, 2.0), 'maw')
    box(M, 'head', (0, 11.0, -10.2), (2.4, 2.4, 1.2), 'maw_in')
    for k in range(6):
        a = k * math.tau / 6
        box(M, 'head', (math.cos(a) * 1.5, 11.0 + math.sin(a) * 1.5, -10.6), (0.7, 0.7, 1.2), 'tooth')
    for sx in (-1, 1):
        world_bone(M, f'ear{sx}', 'head', (sx * 2.2, 8.2, -5.0), 0, 0, sx * -12)
        M.cube_l(f'ear{sx}', (-2.4, -6.5, 0), (4.8, 6.5, 0), 'ear', plane=True)
    # feet + tail
    for sx in (-1, 1):
        seg(M, f'{"left" if sx > 0 else "right"}_leg', 'body', (sx * 1.6, 17, 2.4), (sx * 1.8, 20.5, 2.8), 1.4, 1.4, 'fur', ext=0.2)
    chain(M, 'tail', 'body', [(0, 16.5, 4), (0, 17.6, 7.5), (0, 17.2, 10.5)], (1.8, 1.2), 'fur', ext=0.2)
    # wings with membrane
    for sx, s in ((-1, 'right_wing'), (1, 'left_wing')):
        sh = (sx * 2.8, 12.5, -0.5)
        e1, e2, e3 = (sx * 9.5, 8.0, -1.5), (sx * 17.0, 7.0, 0.5), (sx * 22.5, 12.0, 4.5)
        seg(M, s + '0', 'body', sh, e1, 1.3, 1.3, 'bone', ext=0.3)
        seg(M, s + '1', s + '0', e1, e2, 1.1, 1.1, 'bone', ext=0.3)
        seg(M, s + '2', s + '1', e2, e3, 0.9, 0.9, 'bone', ext=0.3)
        for i, (pa, pb, hh) in enumerate(((sh, e1, 8), (e1, e2, 12), (e2, e3, 11))):
            zero_bone(M, f'{s}_m{i}', s + str(i), pa)
            x0, x1 = sorted((pa[0], pb[0]))
            M.cube(f'{s}_m{i}', (x0, min(pa[1], pb[1]) + 0.2, (pa[2] + pb[2]) / 2), (x1 - x0, hh, 0), 'membrane', plane=True)

    Pt = Painter(M)
    Pt.mats['fur'] = mats.fur(FUR, 2.5)
    Pt.mats['chest'] = mats.fur(pal('#2a0a14', '#561428', '#8a1e3a', '#b83050'), 2)
    Pt.mats['head'] = mats.face_on(mats.fur(FUR, 2.5), 'north', eyes=[(0.24, 0.28, 0.14, 0.14, 0.0, 'round')], eye_col=(255, 70, 90), glow_col=(255, 30, 60))
    Pt.mats['maw'] = mats.flesh(pal('#2a0610', '#5a1020', '#8a1c34', '#b83a52'), vein=(60, 6, 20), stitches=False)
    Pt.mats['maw_in'] = mats.solid((40, 4, 12), glow=False)
    Pt.mats['tooth'] = mats.horn(pal('#4a3a30', '#8a7a68', '#d8c8b0', '#f4ead8'), 3)
    Pt.mats['ear'] = mats.skin(pal('#1c0610', '#3a0c1c', '#661430', '#8c2044'), 0.35, vgrad=0.05)
    Pt.mats['bone'] = mats.horn(pal('#1a0a0e', '#3a1a24', '#6a3a48', '#a06878'), 3)
    Pt.mats['membrane'] = mats.membrane(pal('#1c0610', '#3a0c1c', '#661430', '#8c2044', '#b43058'), vein_col=(255, 50, 80), scallops=2)

    A = flyer_set(M, dict(body='body', head='head', wings=(('right_wing', -1), ('left_wing', 1)), flap=42, flap_len=0.3, idle_len=0.45, lean=10, tail=['tail0', 'tail1'], legs=('right_leg', 'left_leg'), bob=0.8), death_len=18 / 20, drop=18)
    A['bite'] = swing('BITE', 0.6, [(0, {}), (0.15, {'head': (-26, 0, 0), 'body': (-14, 0, 0)}), (0.3, {'head': (24, 0, 0), 'body': (16, 0, 0)}), (0.6, {})])
    A['dive'] = swing('DIVE', 1.1, [(0, {}), (0.3, {'body': (-30, 0, 0), 'head': (-20, 0, 0)}), (0.5, {'body': (60, 0, 0), 'head': (10, 0, 0), 'right_wing0': (0, 0, 50), 'left_wing0': (0, 0, -50)}), (0.8, {'body': (40, 0, 0)}), (1.1, {})])
    A['screech'] = swing('SCREECH', 1.4, [(0, {}), (0.3, {'body': (-24, 0, 0), 'head': (-34, 0, 0)}), (1.0, {'body': (-26, 0, 0), 'head': (-38, 0, 0)}), (1.4, {})])
    scales(A['screech'], [(0, {'head': (1, 1, 1)}), (0.3, {'head': (1.25, 1.25, 1.25)}), (1.0, {'head': (1.3, 1.3, 1.3)}), (1.4, {'head': (1, 1, 1)})])
    return M, Pt, A
