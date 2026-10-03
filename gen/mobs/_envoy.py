"""Shared builder for the five house envoys (peaceful standard-bearers that give quests)."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

STYLES = {
    'ember': dict(skin=pal('#2a1410', '#4c2418', '#7a3a24', '#a85a3a'), robe=pal('#1a0a08', '#3a140c', '#6a2410', '#a03c18', '#e8601c'), trim=(255, 170, 50), eye=(255, 220, 120), glow=(255, 160, 50),
                  flag=pal('#3a0c06', '#8a2410', '#d8501c', '#ffa030'), head='horned', staff=(255, 140, 40), emblem='flame'),
    'bone': dict(skin=pal('#4b4436', '#7d735c', '#b5a888', '#d9cfae', '#f1e9cf'), robe=pal('#2a2a30', '#4a4a56', '#7a7a8a', '#b0b0c0', '#e0e0ee'), trim=(110, 230, 255), eye=(160, 245, 255), glow=(110, 230, 255),
                 flag=pal('#101820', '#26343e', '#c8d4dc', '#f4f8fa'), head='skull', staff=(110, 230, 255), emblem='skull'),
    'blood': dict(skin=pal('#5a4a48', '#8a7670', '#b8a39a', '#dcc8bc', '#f2e4d8'), robe=pal('#14040a', '#2c0a14', '#4c1022', '#74182f', '#9c2440'), trim=(201, 162, 39), eye=(255, 70, 90), glow=(255, 40, 70),
                  flag=pal('#14040a', '#4c1022', '#9c2440', '#e0304c'), head='pale', staff=(230, 30, 60), emblem='drop'),
    'shadow': dict(skin=pal('#3a3248', '#6a5e84', '#9c90b8', '#cfc4e4'), robe=pal('#07040f', '#120a24', '#201440', '#34246a', '#50408f'), trim=(150, 110, 255), eye=(220, 190, 255), glow=(170, 120, 255),
                   flag=pal('#07040f', '#1c1238', '#4a3a88', '#8a6ad8'), head='hooded', staff=(170, 120, 255), emblem='eye'),
    'rot': dict(skin=pal('#26340a', '#4a6a14', '#7aa022', '#b4d84a'), robe=pal('#101a08', '#1e3010', '#34501a', '#5a7e2a', '#88aa40'), trim=(210, 200, 100), eye=(230, 255, 120), glow=(200, 255, 80),
                flag=pal('#10200a', '#223a12', '#5a7e2a', '#c8e060'), head='mushroom', staff=(200, 255, 80), emblem='spore'),
}


def emblem_flag(style):
    ST = STYLES[style]
    base = mats.cloth(ST['flag'], 0.9, hem=ST['trim'], trim=ST['trim'])
    bm = {'flame': ['....#....', '...###...', '..#####..', '.#######.', '.#######.', '..#####..', '...###...'],
          'skull': ['..#####..', '.#######.', '##.###.##', '###...###', '.#######.', '..#.#.#..', '..#.#.#..'],
          'drop': ['....#....', '...###...', '..#####..', '.#######.', '.#######.', '..#####..', '...###...'],
          'eye': ['.........', '..#####..', '.#######.', '###.#.###', '.#######.', '..#####..', '.........'],
          'spore': ['..#####..', '.#######.', '#########', '...###...', '...###...', '...###...', '...###...']}[ST['emblem']]

    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        col = col.copy()
        if face in ('north', 'south') and W >= 9 and H >= 9:
            stamp(col, bm, W // 2 - 4, H // 3, {'#': ST['trim']})
        return col, a, g
    return f


def make(ID, style):
    ST = STYLES[style]
    M = Model(ID, 128, 128)
    mt = dict(pelvis='robe', torso='robe', head='face', arm='sleeve', fore='sleeve', hand='skin', leg='robe', shin='robe', foot='boot')
    K = humanoid(M, leg=(11, 11), leg_w=3.6, foot=(4.4, 2.2, 6.6), hip_w=8, pelvis_h=4, torso=(8.4, 11, 5.4), neck=2.2, head=(7, 7.6, 7), arm=(10, 9), arm_w=3.3,
                 hand=(2.8, 3.6, 2.8), hunch=2, arm_out=0.5, bend=(0.55, 0.25), head_fwd=0.8, mats=mt)
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    zero_bone(M, 'skirt', 'hips', (0, hy - 1, 0))
    M.cube_l('skirt', (-6.2, 0, -4.4), (12.4, 21.5, 8.8), 'robe_hem')
    box(M, 'chest', (0, w[1] - 3.4, -0.2), (9.0, 1.8, 6.2), 'trim')
    box(M, 'neck', (0, K['neck'][1] + 0.6, K['neck'][2]), (10.4, 2.6, 7.0), 'collar')
    for sx, s in ((-1, 'right'), (1, 'left')):
        Bp = K[f'{s}_B']
        box(M, f'{s}_fore', ((Bp[0] + K['hands'][s][0]) / 2 + sx * 0.8, (Bp[1] + K['hands'][s][1]) / 2 + 1.2, (Bp[2] + K['hands'][s][2]) / 2), (6.4, 6.4, 6.0), 'sleeve_wide')
    h = ST['head']
    if h == 'horned':
        for sx in (-1, 1):
            chain(M, f'horn{sx}', 'head', [(sx * 2.8, hc[1] - 3.6, hc[2]), (sx * 5.4, hc[1] - 5.8, hc[2] - 0.5), (sx * 6.2, hc[1] - 10.5, hc[2] - 1.0)], (2.8, 1.8), 'horn', ext=0.2)
    elif h == 'skull':
        box(M, 'head', (hc[0], hc[1] - 4.0, hc[2]), (8.6, 2.0, 8.6), 'trim')
        for k in range(5):
            zero_bone(M, f'cr{k}', 'head', ((k - 2) * 1.8, hc[1] - 4.8, hc[2]))
            M.cube_l(f'cr{k}', (-0.5, -4.0 + abs(k - 2) * 0.6, -0.5), (1.0, 4.0 - abs(k - 2) * 0.6, 1.0), 'trim')
    elif h == 'pale':
        box(M, 'head', (hc[0], hc[1] - 4.6, hc[2] + 0.3), (8.2, 3.0, 8.4), 'hair')
        box(M, 'head', (hc[0], hc[1] - 0.4, hc[2] + 4.2), (8.2, 9.0, 1.8), 'hair')
    elif h == 'hooded':
        box(M, 'head', (hc[0], hc[1] - 3.6, hc[2] + 0.1), (8.8, 2.8, 8.8), 'hood')
        box(M, 'head', (hc[0], hc[1] + 0.4, hc[2] + 3.7), (8.8, 9.0, 2.0), 'hood')
        for sx in (-1, 1):
            box(M, 'head', (hc[0] + sx * 4.0, hc[1] - 0.1, hc[2] + 0.5), (1.6, 8.6, 8.2), 'hood')
        box(M, 'head', (hc[0], hc[1] - 4.5, hc[2] - 2.7), (6.4, 1.1, 3.4), 'hood')
    elif h == 'mushroom':
        box(M, 'head', (hc[0], hc[1] - 4.6, hc[2]), (13, 2.6, 13), 'cap')
        box(M, 'head', (hc[0], hc[1] - 7.0, hc[2]), (9, 2.6, 9), 'cap')
        box(M, 'head', (hc[0], hc[1] - 9.0, hc[2]), (4.4, 2.0, 4.4), 'cap')
    # staff with glowing head and banner pole on the back
    M.cube_l('right_hand', (-0.6, -24, -0.6), (1.2, 38, 1.2), 'pole')
    zero_bone(M, 'staffhead', 'right_hand', (K['hands']['right'][0], K['hands']['right'][1] - 24, K['hands']['right'][2]))
    M.cube_l('staffhead', (-1.8, -3.6, -1.8), (3.6, 3.6, 3.6), 'gem')
    zero_bone(M, 'pole', 'chest', (0, w[1] - 3, 4.4))
    M.cube_l('pole', (-0.6, -38, -0.6), (1.2, 40, 1.2), 'pole')
    zero_bone(M, 'flag', 'pole', (0, w[1] - 38, 4.4))
    M.cube_l('flag', (-5.0, 0, -0.4), (10, 14, 0.8), 'flag')
    M.cube_l('pole', (-3.0, -39.5, -1.0), (6, 1.6, 2.0), 'trim')
    Pt = Painter(M)
    Pt.mats['robe'] = mats.cloth(ST['robe'], 1.0, trim=ST['trim'])
    Pt.mats['robe_hem'] = mats.cloth(ST['robe'], 1.1, hem=ST['trim'], stripe=ST['trim'], trim=ST['trim'])
    Pt.mats['sleeve'] = mats.cloth(ST['robe'], 0.8)
    Pt.mats['sleeve_wide'] = mats.cloth(ST['robe'], 1.2, hem=ST['trim'])
    Pt.mats['collar'] = mats.cloth(ST['robe'], 0.6, hem=ST['trim'], trim=ST['trim'])
    Pt.mats['trim'] = mats.metal(pal('#3a2a0a', '#8f6a18', '#d8a828', '#ffe070'))
    Pt.mats['skin'] = mats.skin(ST['skin'], 0.25)
    Pt.mats['boot'] = mats.cloth(ST['robe'], 0.3)
    Pt.mats['hood'] = mats.cloth(ST['robe'], 0.9)
    Pt.mats['hair'] = mats.fur(pal('#08040a', '#18080e', '#2c0e18', '#4a1a2a'), 3)
    Pt.mats['horn'] = mats.horn(pal('#1a0f0a', '#3a1a10', '#7a2c10', '#c4501a'), 2, tip_glow=ST['glow'])
    Pt.mats['cap'] = mats.fungus(pal('#2a1c10', '#5a3c20', '#8c6a38', '#c8a45a'), spot=(250, 240, 210), spot_n=0.25)
    Pt.mats['pole'] = mats.horn(pal('#1a120c', '#3a281c', '#5a4030', '#7a5a44'), 4)
    Pt.mats['gem'] = mats.solid(ST['staff'])
    Pt.mats['flag'] = emblem_flag(style)
    skin_mat = mats.skin(ST['skin'], 0.22, vgrad=0.1) if style != 'bone' else mats.bone(ST['skin'], 0.4)
    kw = dict(eyes=[(0.22, 0.4, 0.12, 0.1, 0.1, 'almond')], eye_col=ST['eye'], glow_col=ST['glow'])
    if style == 'bone':
        kw = dict(eyes=[(0.22, 0.38, 0.14, 0.13, 0.0, 'round')], nose=dict(x=0.5, y=0.58, w=0.06, h=0.1, col=(34, 28, 22)), mouth=dict(y=0.86, w=0.3, h=0.07, teeth=6, col=(24, 18, 14)), eye_col=ST['eye'], glow_col=ST['glow'])
    elif style == 'ember':
        kw['mouth'] = dict(y=0.8, w=0.22, h=0.05, col=(255, 120, 40), teeth=0)
        kw['brow'] = dict(y=0.28, w=0.2, th=0.05, angle=0.4, col=(10, 4, 4))
    Pt.mats['face'] = mats.face_on(skin_mat, 'north', **kw)
    ba = dict(stride=22, shin=32, arm=6, elbow=6, lean=1)
    A = biped_set(M, ba, death='fall_back', death_len=22 / 20, run=False)
    for nm in ('idle', 'walk'):
        gen(A[nm], 'flag', 'r', lambda ph: (5 * S(ph, 1, 0.1), 0, 4 * S(ph, 1, 0.3)), n=8)
    A['attack'] = swing('ATTACK', 0.8, [(0, {}), (0.2, {'right_arm': (-110, 0, -10), 'chest': (-8, -14, 0)}), (0.35, {'right_arm': (-30, 0, 4), 'chest': (14, 14, 0)}), (0.8, {})])
    return M, Pt, A


def spec(ID, fac, tr, en, egg, lore):
    return dict(faction=fac.upper(), tr=tr, en=en, hp=40, armor=2, attack=3.0, speed=0.22, size=(0.65, 2.0, 1.0), reach=2.2, kb=0.3, follow=16,
                voice=('revenge', 1.3), egg=egg, spawn=(0, 1, 1, []), xp=0, death_ticks=22, anim=(1.5, 1.8), npc=True,
                abilities=[dict(kind='MELEE', anim='attack', time=(14, 7), cd=20, dmg=1.0, p=(0.3, 0), color=0xFFFFFF, fx='crit')], drops=[], lore=lore)
