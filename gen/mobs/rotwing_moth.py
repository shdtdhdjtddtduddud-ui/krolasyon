"""Çürük Güve (Rotwing Moth) - huge dusty moth with eye-spotted wings that sheds blinding spores."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'rotwing_moth'
SPEC = dict(
    faction='ROT', tr='Çürük Güve', en='Rotwing Moth', hp=30, armor=0, attack=4.0, speed=0.2, size=(1.2, 1.0, 1.0), fly=2.4, reach=2.2, follow=34,
    voice=('hound', 2.0), egg=(0x7A6A3A, 0xE8E060), spawn=(10, 2, 4, ['rot_bog', 'weeping_marsh', 'gloam_forest']), xp=10, death_ticks=22, anim=(2.4, 2.4), head='head',
    abilities=[
        dict(kind='MELEE', anim='stab', time=(14, 7), cd=18, dmg=1.0, p=(0.3, 0), color=0xC8C050, fx='spore', eff=('minecraft:poison', 60, 0)),
        dict(kind='CLOUD', anim='dust', time=(28, 12), cd=130, w=4, range=(0, 12), dmg=0, p=(3.6, 150), color=0xC8C050, fx='spore', eff=('minecraft:blindness', 80, 0)),
        dict(kind='CONE', anim='gust', time=(40, 12, 18), cd=190, w=3, range=(0, 9), dmg=0.35, p=(18, 0), color=0xB8B060, fx='spore', eff=('minecraft:weakness', 80, 0)),
        dict(kind='DIVE', anim='dive', time=(24, 7, 15), cd=130, w=3, range=(5, 15), dmg=1.2, p=(1.1, 0.8), color=0xC8C050, fx='spore', air=True),
    ],
    drops=[('minecraft:string', 0, 2, 0.5), ('krolasyonbosses:rot_spore', 0, 2, 0.7)],
    lore='Çürük Divanı\'nın kanatlı bahçıvanları. Kanatlarındaki göz lekeleri bakışı çeker; toz ise gözü bir daha açmaz.',
)

FUR = pal('#1e180c', '#3a2e18', '#5e4c2a', '#8a7440', '#b8a060')
WING = pal('#2a2010', '#5a4624', '#8c7038', '#c4a45a', '#e8d08a')


def build():
    M = Model(ID, 256, 128)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 13, 0))
    box(M, 'body', (0, 13, 0), (6.6, 6.6, 9), 'fur')
    box(M, 'body', (0, 13.4, 7.2), (5.2, 5.2, 8), 'fur2')
    M.bone('head', 'body', (0, 11.5, -5))
    box(M, 'head', (0, 11.6, -6.4), (5.6, 5.0, 4.6), 'head')
    box(M, 'head', (0, 12.8, -9.0), (0.8, 0.8, 3.6), 'tooth')
    for sx in (-1, 1):
        # feathery antennae
        chain(M, f'ant{sx}_', 'head', [(sx * 1.4, 9.4, -7.4), (sx * 3.0, 5.4, -9.0), (sx * 4.6, 2.4, -9.6)], (0.9, 0.7), 'tooth', ext=0.1)
        zero_bone(M, f'antf{sx}', f'ant{sx}_1', (sx * 3.0, 5.4, -9.0))
        M.cube_l(f'antf{sx}', (-1.6, -2.2, 0), (3.2, 5.0, 0), 'antfeather', plane=True)
    # legs
    for sx in (-1, 1):
        for k, z in enumerate((-3, 0, 3)):
            seg(M, f'{"left" if sx > 0 else "right"}_leg{k}', 'body', (sx * 2.0, 16, z), (sx * 3.6, 20, z - 1), 1.0, 1.0, 'tooth', ext=0.2)
    # wings: forewing + hindwing as vertical cards, eye-spots painted
    for sx, s in ((-1, 'right_wing'), (1, 'left_wing')):
        sh = (sx * 3.0, 11.0, 0.5)
        e1, e2, e3 = (sx * 13, 6.0, 1.5), (sx * 24, 5.0, 3.5), (sx * 34, 8.5, 6.5)
        seg(M, s + '0', 'body', sh, e1, 1.5, 1.5, 'tooth', ext=0.3)
        seg(M, s + '1', s + '0', e1, e2, 1.3, 1.3, 'tooth', ext=0.3)
        seg(M, s + '2', s + '1', e2, e3, 1.1, 1.1, 'tooth', ext=0.3)
        for i, (pa, pb, hh) in enumerate(((sh, e1, 17), (e1, e2, 20), (e2, e3, 18))):
            zero_bone(M, f'{s}_m{i}', s + str(i), pa)
            x0, x1 = sorted((pa[0], pb[0]))
            M.cube(f'{s}_m{i}', (x0, min(pa[1], pb[1]) + 0.2, (pa[2] + pb[2]) / 2), (x1 - x0, hh, 0), f'wing{i}', plane=True)

    Pt = Painter(M)
    Pt.mats['fur'] = mats.fur(FUR, 2.2)
    Pt.mats['fur2'] = mats.fur(pal('#3a2e18', '#5e4c2a', '#8a7440', '#c0a860'), 2.2)
    Pt.mats['tooth'] = mats.horn(pal('#1c140a', '#3a2c18', '#6a5430', '#a08850'), 3)
    Pt.mats['antfeather'] = mats.feather_sheet(pal('#3a2c18', '#6a5430', '#a08850'), rows=2, fw=2, tip_depth=3)
    Pt.mats['head'] = mats.face_on(mats.fur(FUR, 2.2), 'north', eyes=[(0.24, 0.4, 0.17, 0.2, 0.0, 'round')], eye_col=(255, 240, 120), glow_col=(230, 230, 80))

    def wing(idx):
        base = mats.feather_sheet(WING, tip=(120, 100, 50), rows=3, fw=3, tip_depth=3, tip_start=0.8)

        def f(c, face, W, H, r):
            col, a, g = base(c, face, W, H, r)
            col = col.copy()
            g = np.zeros((H, W, 4), np.float32)
            yy, xx = np.mgrid[0:H, 0:W]
            cx, cy = W * (0.55 if idx != 2 else 0.5), H * 0.5
            d = np.hypot(xx - cx, (yy - cy) * 1.1)
            rad = min(W, H) * 0.26
            for rr, colr in ((rad, (30, 20, 10)), (rad * 0.75, (230, 220, 120)), (rad * 0.5, (30, 20, 10)), (rad * 0.28, (255, 255, 200))):
                col = np.where((d < rr)[..., None], np.array(colr, np.float32), col)
            g[d < rad * 0.3] = (255, 255, 160, 200)
            return col, a, g
        return f
    for i in range(3):
        Pt.mats[f'wing{i}'] = wing(i)

    A = flyer_set(M, dict(body='body', head='head', wings=(('right_wing', -1), ('left_wing', 1)), flap=30, flap_len=0.55, idle_len=1.1, lean=8, tail=None, bob=1.2), death_len=22 / 20, drop=20)
    A['stab'] = swing('STAB', 0.65, [(0, {}), (0.18, {'head': (-20, 0, 0), 'body': (-8, 0, 0)}), (0.3, {'head': (30, 0, 0), 'body': (12, 0, 0)}), (0.65, {})])
    A['dust'] = swing('DUST', 1.4, [(0, {}), (0.4, {'right_wing0': (0, 0, -30), 'left_wing0': (0, 0, 30), 'body': (-14, 0, 0)}), (0.6, {'right_wing0': (0, 0, 40), 'left_wing0': (0, 0, -40), 'body': (8, 0, 0)}), (1.4, {})])
    A['gust'] = swing('GUST', 2.0, [(0, {}), (0.5, {'right_wing0': (0, 0, -36), 'left_wing0': (0, 0, 36), 'body': (-18, 0, 0), 'head': (-10, 0, 0)}), (0.7, {'right_wing0': (0, 0, 46), 'left_wing0': (0, 0, -46), 'body': (12, 0, 0)}), (1.4, {'right_wing0': (0, 0, -30), 'left_wing0': (0, 0, 30)}), (2.0, {})])
    A['dive'] = swing('DIVE', 1.2, [(0, {}), (0.35, {'body': (-30, 0, 0)}), (0.55, {'body': (60, 0, 0), 'right_wing0': (0, 0, 50), 'left_wing0': (0, 0, -50)}), (0.85, {'body': (40, 0, 0)}), (1.2, {})])
    return M, Pt, A
