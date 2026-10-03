"""Mezar Hayaleti (Grave Wraith) - floating shrouded spirit with a hollow skull face and long claws."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'grave_wraith'
SPEC = dict(
    faction='BONE', tr='Mezar Hayaleti', en='Grave Wraith', hp=36, armor=0, attack=5.0, speed=0.2, size=(0.8, 2.0, 1.0), fly=1.4, reach=2.4, follow=36,
    voice=('revenge', 1.5), egg=(0x6A7A88, 0xB8F2FF), spawn=(7, 1, 2, ['bone_marches', 'weeping_marsh']), xp=14, death_ticks=26, anim=(1.8, 1.8), head='head',
    abilities=[
        dict(kind='MELEE', anim='claw', time=(18, 8, 13), cd=18, dmg=0.8, p=(0.3, 0.5), color=0x9AD8FF, fx='soul', eff=('minecraft:weakness', 80, 0)),
        dict(kind='DRAIN', anim='drain', time=(56, 12), cd=150, w=4, range=(2, 12), dmg=0.5, p=(0, 0.8), color=0x7AB8FF, fx='soul', los=True),
        dict(kind='CLOUD', anim='wither', time=(30, 14), cd=190, w=3, range=(3, 14), dmg=0, p=(3.2, 140), color=0x3A4658, fx='soul', eff=('minecraft:wither', 100, 0)),
        dict(kind='BURST', anim='wail', time=(40, 24), cd=260, w=2, range=(0, 8), dmg=0.5, p=(7, 0.3), color=0xB8F2FF, fx='soul', eff=('minecraft:blindness', 60, 0)),
        dict(kind='BLINK_AWAY', anim='fade', time=(16, 6), cd=160, w=2, range=(0, 7), p=(11, 0), color=0x7A9AB8, fx='soul'),
    ],
    drops=[('krolasyonbosses:bone_dust', 1, 3, 0.8), ('minecraft:ghast_tear', 0, 1, 0.15)],
    lore='Kemik Krallığı\'nın mezarlarından sürüklenen ruhlar. Dokunuşu hayat gücünü emer; çığlığı kulakları değil, ruhu sağır eder.',
)

SHROUD = pal('#0a1018', '#18242e', '#2a3c4a', '#456074', '#6e8ea4')
IVORY = pal('#4b4436', '#7d735c', '#b5a888', '#d9cfae', '#f1e9cf')


def build():
    M = Model(ID, 128, 128)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 10, 0))
    # tapering shroud made of 4 stacked boxes, tattered strips at the bottom
    box(M, 'body', (0, 8.0, 0), (10, 10, 6.4), 'shroud')
    box(M, 'body', (0, 14.2, 0), (9, 6, 6), 'shroud2')
    box(M, 'body', (0, 18.4, 0.2), (7.4, 4.6, 5.2), 'shroud2')
    for k, (x, z, h) in enumerate(((-3.6, -2.0, 7), (0.5, -2.6, 9), (3.4, -1.0, 6), (-2.0, 2.4, 8), (2.8, 2.6, 7), (-0.2, 0.6, 5))):
        zero_bone(M, f'rag{k}', 'body', (x, 19.5, z))
        M.cube_l(f'rag{k}', (-1.5, 0, -0.4), (3.0, h, 0.8), 'rag')
    # head with hood
    M.bone('head', 'body', (0, 4.5, -0.3))
    box(M, 'head', (0, 1.0, -0.3), (7, 7.4, 7), 'skull')
    box(M, 'head', (0, -4.4, 0.3), (9.4, 2.6, 9.2), 'hood')
    box(M, 'head', (0, 0.6, 4.3), (9.4, 10, 2.2), 'hood')
    for sx in (-1, 1):
        box(M, 'head', (sx * 4.3, 0.4, 0.5), (1.8, 9.4, 8.6), 'hood')
    box(M, 'head', (0, -4.6, -3.6), (6.6, 1.2, 2.4), 'hood')
    for sx in (-1, 1):
        zero_bone(M, f'horn{sx}', 'head', (sx * 3.4, -2.2, 0))
        M.cube_l(f'horn{sx}', (-0.8, -5.5, -0.8), (1.6, 5.5, 1.6), 'bone')
    # long arms with claws: shoulders at the upper shroud
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = (sx * 5.0, 8.6, 0)
        B = (sx * 8.0, 15.0, -2.4)
        C = (sx * 8.8, 22.0, -5.6)
        seg(M, f'{s}_arm', 'body', A, B, 2.6, 2.6, 'bone_arm', ext=0.6)
        seg(M, f'{s}_fore', f'{s}_arm', B, C, 2.2, 2.2, 'bone_arm', ext=0.4)
        zero_bone(M, f'{s}_hand', f'{s}_fore', C)
        M.cube_l(f'{s}_hand', (-1.3, -0.2, -1.3), (2.6, 2.4, 2.6), 'bone')
        for k in range(4):
            M.cube_l(f'{s}_hand', (-1.2 + k * 0.8, 2.0, -1.0 + (k % 2) * 0.4), (0.6, 4.6 - (k % 2), 0.6), 'bone')
        # tattered sleeve
        box(M, f'{s}_arm', ((A[0] + B[0]) / 2 + sx * 0.4, (A[1] + B[1]) / 2, (A[2] + B[2]) / 2), (4.6, 6.6, 4.2), 'shroud2')
    # chains hanging from the waist
    zero_bone(M, 'chain', 'body', (0, 11.5, -3.4))
    for k in range(6):
        M.cube_l('chain', (-0.9, k * 2.4, -0.4 + (k % 2) * 0.5), (1.8, 1.7, 0.7), 'chainlink')

    Pt = Painter(M)
    Pt.mats['shroud'] = mats.ragged(mats.cloth(SHROUD, 1.1, trim=(110, 150, 175)), depth=3)
    Pt.mats['shroud2'] = mats.cloth(SHROUD, 1.0, hem=(110, 150, 175))
    Pt.mats['rag'] = mats.ragged(mats.cloth(SHROUD, 1.0), depth=5)
    Pt.mats['hood'] = mats.cloth(pal('#06090e', '#0e1620', '#1a2a38', '#2c4458'), 0.9)
    Pt.mats['bone'] = mats.bone(IVORY, 0.5)
    Pt.mats['bone_arm'] = mats.bone(pal('#3a4a56', '#5a7080', '#8aa2b2', '#b8d0de'), 0.6)
    Pt.mats['skull'] = mats.face_on(mats.bone(IVORY, 0.4), 'north', eyes=[(0.23, 0.38, 0.15, 0.17, 0.0, 'round')], nose=dict(x=0.5, y=0.62, w=0.06, h=0.1, col=(20, 20, 24)),
                                    mouth=dict(y=0.86, w=0.3, h=0.09, teeth=5, col=(10, 12, 16), curve=0.4), eye_col=(190, 245, 255), glow_col=(120, 220, 255))
    Pt.mats['chainlink'] = mats.metal(pal('#101216', '#2a2e36', '#4a505c', '#7a8494'))

    A = {}
    for name, L, amp in (('IDLE', 3.2, 0.6), ('WALK', 1.6, 1.0)):
        a = Anim(name, L, True)
        gen(a, 'body', 'p', lambda ph, amp=amp: (0, 1.6 * S(ph), 0), n=10)
        gen(a, 'body', 'r', lambda ph, amp=amp: (4 * amp * S(ph, 2, 0.1) + (10 if name == 'WALK' else 0), 5 * S(ph, 1, 0.5), 3 * S(ph, 1, 0.2)), n=10)
        gen(a, 'head', 'r', lambda ph: (3 * S(ph, 2, 0.2), 6 * S(ph, 1, 0.25), 2 * S(ph, 1, 0.4)), n=10)
        for sx, s in ((-1, 'right'), (1, 'left')):
            gen(a, f'{s}_arm', 'r', lambda ph, sx=sx, amp=amp: (-14 + 8 * S(ph, 1, 0.25 * sx), 0, sx * (8 + 6 * S(ph, 1, 0.1))), n=10)
            gen(a, f'{s}_fore', 'r', lambda ph, amp=amp: (-18 + 10 * S(ph, 1, 0.45), 0, 0), n=10)
        for k in range(6):
            gen(a, f'rag{k}', 'r', lambda ph, k=k, amp=amp: (10 * S(ph, 1, k * 0.17), 0, 12 * S(ph, 1, k * 0.23 + 0.3) * (0.5 + amp * 0.5)), n=10)
        A[name.lower()] = a
    d = Anim('DEATH', 26 / 20, False)
    L = d.length
    poses(d, [(0, {}), (L * 0.5, {'body': (-20, 0, 12), 'head': (-30, 0, 0), 'right_arm': (-60, 0, -30), 'left_arm': (-60, 0, 30)}), (L, {'body': (-60, 0, 25), 'head': (-50, 0, 0), 'right_arm': (-110, 0, -50), 'left_arm': (-110, 0, 50)})])
    scales(d, [(0, {'base': (1, 1, 1)}), (L * 0.6, {'base': (1.1, 0.9, 1.1)}), (L, {'base': (1.4, 0.05, 1.4)})])
    A['death'] = d
    A['claw'] = swing('CLAW', 0.9, [(0, {}), (0.25, {'right_arm': (-130, 0, -10), 'left_arm': (-30, 0, 10), 'body': (-12, -20, 0), 'head': (-12, 0, 0)}), (0.4, {'right_arm': (-30, 0, 10), 'right_fore': (-14, 0, 0), 'body': (20, 24, 0), 'head': (10, 0, 0)}),
                                   (0.6, {'left_arm': (-130, 0, 10), 'body': (-10, 22, 0)}), (0.72, {'left_arm': (-30, 0, -10), 'body': (20, -24, 0)}), (0.9, {})])
    A['drain'] = swing('DRAIN', 2.8, [(0, {}), (0.5, {'right_arm': (-100, 0, -16), 'left_arm': (-100, 0, 16), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'body': (14, 0, 0), 'head': (-14, 0, 0)}),
                                     (2.4, {'right_arm': (-104, 0, -20), 'left_arm': (-104, 0, 20), 'right_fore': (-34, 0, 0), 'left_fore': (-34, 0, 0), 'body': (16, 0, 0), 'head': (-16, 0, 0)}), (2.8, {})])
    A['wither'] = swing('WITHER', 1.5, [(0, {}), (0.45, {'right_arm': (-60, 0, -50), 'left_arm': (-60, 0, 50), 'body': (-14, 0, 0), 'head': (-24, 0, 0)}), (0.7, {'right_arm': (-110, 0, -20), 'left_arm': (-110, 0, 20), 'body': (16, 0, 0), 'head': (10, 0, 0)}), (1.5, {})])
    A['wail'] = swing('WAIL', 2.0, [(0, {}), (0.6, {'body': (-24, 0, 0), 'head': (-44, 0, 0), 'right_arm': (-40, 0, -70), 'left_arm': (-40, 0, 70)}), (1.6, {'body': (-26, 0, 0), 'head': (-48, 0, 0), 'right_arm': (-44, 0, -76), 'left_arm': (-44, 0, 76)}), (2.0, {})])
    scales(A['wail'], [(0, {'head': (1, 1, 1)}), (0.6, {'head': (1.15, 1.15, 1.15)}), (1.6, {'head': (1.2, 1.2, 1.2)}), (2.0, {'head': (1, 1, 1)})])
    A['fade'] = swing('FADE', 0.8, [(0, {}), (0.4, {'body': (0, 0, 0)}), (0.8, {})])
    scales(A['fade'], [(0, {'base': (1, 1, 1)}), (0.3, {'base': (0.4, 1.2, 0.4)}), (0.8, {'base': (1, 1, 1)})])
    return M, Pt, A
