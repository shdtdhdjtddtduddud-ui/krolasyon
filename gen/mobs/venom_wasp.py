"""Zehir Arısı (Venom Wasp) - giant armoured wasp with a glowing stinger."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'venom_wasp'
SPEC = dict(
    faction='ROT', tr='Zehir Arısı', en='Venom Wasp', hp=22, armor=2, attack=4.0, speed=0.34, size=(0.8, 0.8, 1.0), fly=1.8, reach=1.8, follow=36,
    voice=('hound', 2.0), egg=(0xE8C030, 0x2A1A06), spawn=(11, 2, 4, ['rot_bog', 'weeping_marsh']), xp=7, death_ticks=18, anim=(3.0, 3.0), head='head',
    abilities=[
        dict(kind='MELEE', anim='sting', time=(12, 6), cd=14, dmg=1.0, p=(0.2, 0), color=0xD0FF30, fx='spore', eff=('minecraft:poison', 80, 1)),
        dict(kind='DIVE', anim='dive', time=(22, 6, 15), cd=90, w=4, range=(4, 14), dmg=1.3, p=(1.4, 0.8), color=0xD0FF30, fx='spore', eff=('minecraft:poison', 100, 1), air=True),
        dict(kind='VOLLEY', anim='darts', time=(26, 12), cd=130, w=3, range=(4, 16), dmg=0.5, p=(4, 0.5), color=0xD0FF30, fx='spore', los=True, eff=('minecraft:poison', 60, 0)),
        dict(kind='BUFF', anim='frenzy', time=(24, 8), cd=320, w=1, range=(0, 8), p=(8, 0), color=0xFFE040, fx='spore', eff=('minecraft:speed', 120, 1)),
    ],
    drops=[('krolasyonbosses:rot_spore', 0, 1, 0.4), ('minecraft:spider_eye', 0, 1, 0.3)],
    lore='Çürük Divanı\'nın sürüleri. Tek bir iğne büyük bir bedeni yavaş yavaş çürütür.',
)

CHIT = pal('#1a1204', '#3a2a08', '#6a4c10', '#a87c18', '#e8b830')
BLK = pal('#08060a', '#14101a', '#241e2c', '#3a3248')


def build():
    M = Model(ID, 128, 64)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 14, 0))
    box(M, 'body', (0, 13.5, -1.5), (5.4, 5.4, 6), 'thorax')
    M.bone('abdomen', 'body', (0, 14, 1.5))
    box(M, 'abdomen', (0, 14.2, 6), (6.6, 6.4, 10), 'abd')
    box(M, 'abdomen', (0, 14.6, 12.6), (3.6, 3.6, 3.4), 'abd')
    zero_bone(M, 'sting', 'abdomen', (0, 15, 14.5))
    M.cube_l('sting', (-0.6, -0.6, 0), (1.2, 1.2, 6.5), 'stinger')
    M.bone('head', 'body', (0, 12, -5))
    box(M, 'head', (0, 12.2, -7.2), (5.8, 5.2, 4.8), 'head')
    for sx in (-1, 1):
        box(M, 'head', (sx * 2.0, 14.6, -9.2), (1.6, 2.0, 1.4), 'jaw')
        chain(M, f'ant{sx}_', 'head', [(sx * 1.2, 9.6, -8.5), (sx * 2.4, 6.6, -10.5), (sx * 3.4, 4.4, -11.4)], (0.8, 0.7), 'jaw', ext=0.1)
    for sx in (-1, 1):
        for k, z in enumerate((-2.5, -0.5, 1.5)):
            seg(M, f'{"left" if sx > 0 else "right"}_leg{k}', 'body', (sx * 1.8, 16, z), (sx * 3.4, 20.4, z + (k - 1) * 1.2), 0.9, 0.9, 'jaw', ext=0.2)
    # four wings (translucent veined cards)
    for sx, s in ((-1, 'right_wing'), (1, 'left_wing')):
        for i, (zo, length, hh) in enumerate(((-1.5, 12, 5), (2.5, 9, 4))):
            sh = (sx * 2.4, 11.2, zo)
            tip = (sx * (2.4 + length), 9.6 - i * 0.5, zo + 2.5)
            seg(M, s + str(i), 'body', sh, tip, 0.8, 0.8, 'jaw', ext=0.2)
            zero_bone(M, f'{s}_m{i}', s + str(i), sh)
            x0, x1 = sorted((sh[0], tip[0]))
            M.cube(f'{s}_m{i}', (x0, 9.8, zo + 1.2), (x1 - x0, hh * 2, 0), 'wing', plane=True)

    Pt = Painter(M)
    Pt.mats['thorax'] = mats.fur(pal('#1a1204', '#3a2a08', '#5a4010', '#7a5a18'), 2)
    Pt.mats['abd'] = striped()
    Pt.mats['stinger'] = mats.solid((220, 255, 60))
    Pt.mats['jaw'] = mats.horn(pal('#0e0a04', '#2a1e08', '#5a4418', '#8a6a28'), 3)
    Pt.mats['head'] = mats.face_on(mats.chitin(CHIT, 6), 'north', eyes=[(0.25, 0.38, 0.18, 0.22, 0.0, 'round')], eye_col=(30, 40, 0), glow_col=(210, 255, 60), eye_glow=0.5,
                                   mouth=dict(y=0.82, w=0.2, h=0.06, col=(30, 14, 4)))
    Pt.mats['wing'] = mats.membrane(pal('#a8b8a0', '#c8d8c0', '#e4f0dc', '#f6fff0'), vein_col=(120, 150, 100), scallops=3)

    A = flyer_set(M, dict(body='body', head='head', wings=(('right_wing', -1), ('left_wing', 1)), wing_segs=('0', '1'), flap=34, flap_len=0.22, idle_len=0.3, lean=10, tail=['abdomen'], legs=('right_leg0', 'left_leg0'), bob=0.7), death_len=18 / 20, drop=18)
    A['sting'] = swing('STING', 0.6, [(0, {}), (0.15, {'abdomen': (-26, 0, 0), 'body': (-10, 0, 0)}), (0.28, {'abdomen': (36, 0, 0), 'body': (14, 0, 0)}), (0.6, {})])
    A['dive'] = swing('DIVE', 1.1, [(0, {}), (0.3, {'body': (-30, 0, 0), 'abdomen': (-30, 0, 0)}), (0.5, {'body': (62, 0, 0), 'abdomen': (30, 0, 0)}), (0.8, {'body': (40, 0, 0)}), (1.1, {})])
    A['darts'] = swing('DARTS', 1.3, [(0, {}), (0.4, {'abdomen': (-40, 0, 0), 'body': (-16, 0, 0)}), (0.6, {'abdomen': (30, 0, 0), 'body': (10, 0, 0)}), (1.3, {})])
    A['frenzy'] = swing('FRENZY', 1.2, [(0, {}), (0.3, {'body': (-20, 0, 0), 'head': (-20, 0, 0)}), (0.8, {'body': (-24, 0, 0), 'head': (-26, 0, 0)}), (1.2, {})])
    scales(A['frenzy'], [(0, {'abdomen': (1, 1, 1)}), (0.3, {'abdomen': (1.25, 1.2, 1.25)}), (0.8, {'abdomen': (1.3, 1.25, 1.3)}), (1.2, {'abdomen': (1, 1, 1)})])
    return M, Pt, A


def striped():
    base = mats.chitin(CHIT, 5, shine=True)
    black = mats.chitin(BLK, 5, shine=True)

    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        colb, _, _ = black(c, face, W, H, r)
        yy, xx = np.mgrid[0:H, 0:W]
        stripe = ((xx if face in ('north', 'south') else (yy if False else xx)) // 3) % 2 == 0
        if face in ('east', 'west'):
            stripe = (xx // 3) % 2 == 0
        elif face in ('top', 'bottom'):
            stripe = (yy // 3) % 2 == 0
        else:
            stripe = (yy // 3) % 2 == 0
        col = np.where(stripe[..., None], colb, col)
        return col, a, g
    return f
