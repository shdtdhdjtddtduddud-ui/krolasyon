"""Eziyetli Ruh (Tormented Soul) - chained, wailing soul that bursts into screams."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'tormented_soul'
SPEC = dict(
    faction='OUTCAST', tr='Eziyetli Ruh', en='Tormented Soul', hp=26, armor=0, attack=4.5, speed=0.22, size=(0.8, 1.9, 1.0), fly=1.2, reach=2.2, follow=34,
    voice=('revenge', 1.8), egg=(0xB8C8E0, 0x2A3A5A), spawn=(9, 1, 3, ['bone_marches', 'weeping_marsh', 'umbral_reach', 'ashen_barrens']), xp=10, death_ticks=24, anim=(1.8, 1.8), head='head',
    abilities=[
        dict(kind='MELEE', anim='reach', time=(18, 9), cd=18, dmg=1.0, p=(0.3, 0.4), color=0xA8C8F0, fx='soul', eff=('minecraft:weakness', 60, 0)),
        dict(kind='BURST', anim='scream', time=(36, 20), cd=170, w=4, range=(0, 7), dmg=0.8, p=(6.5, 0.8), color=0xD0E4FF, fx='soul', eff=('minecraft:slowness', 80, 1)),
        dict(kind='CLOUD', anim='despair', time=(30, 14), cd=190, w=3, range=(3, 13), dmg=0, p=(3.4, 140), color=0x5A6A9A, fx='soul', eff=('minecraft:mining_fatigue', 100, 1)),
        dict(kind='DRAIN', anim='drain', time=(50, 10), cd=170, w=2, range=(2, 10), dmg=0.4, p=(0, 0.7), color=0x90B0E8, fx='soul', los=True),
    ],
    drops=[('krolasyonbosses:soul_ember', 0, 2, 0.5), ('minecraft:glowstone_dust', 0, 2, 0.4)],
    lore='Cellâtların kayıt defterine adı yazılmamış mahkûmlar. Zincirleri kopmaz; çığlıkları ise kimsenin duymadığı kadar uzağa ulaşır.',
)

GHOST = pal('#1c2438', '#34486c', '#5a76a8', '#8caad8', '#c8dcf8')


def build():
    M = Model(ID, 128, 128)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 10, 0))
    # tapering wispy body
    box(M, 'body', (0, 12.5, 0), (9, 7, 6), 'ghost')
    box(M, 'body', (0, 7.5, 0), (7, 5, 5), 'ghost')
    for k, (x, z, h) in enumerate(((-2.6, -1.4, 8), (0, -2, 10), (2.6, -1, 7), (-1.2, 1.8, 9), (1.8, 1.6, 8))):
        zero_bone(M, f'wisp{k}', 'body', (x, 16, z))
        M.cube_l(f'wisp{k}', (-1.5, 0, -0.4), (3.0, h, 0.8), 'wisp')
    # head with an open screaming mouth
    M.bone('head', 'body', (0, 6.5, -0.3))
    box(M, 'head', (0, 3.5, -0.3), (7.6, 8, 7), 'face')
    box(M, 'head', (0, 8.2, -2.6), (4.6, 3.4, 1.2), 'maw')
    # arms stretched, with shackles
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = (sx * 4.6, 7.4, 0)
        B = (sx * 8.4, 11.5, -3)
        C = (sx * 9.0, 15.5, -8)
        seg(M, f'{s}_arm', 'body', A, B, 2.4, 2.4, 'ghost', ext=0.5)
        seg(M, f'{s}_fore', f'{s}_arm', B, C, 2.0, 2.0, 'ghost', ext=0.4)
        zero_bone(M, f'{s}_hand', f'{s}_fore', C)
        M.cube_l(f'{s}_hand', (-1.2, -0.2, -1.2), (2.4, 2.4, 2.4), 'ghost')
        for k in range(4):
            M.cube_l(f'{s}_hand', (-1.1 + k * 0.75, 2.0, -0.8 + (k % 2) * 0.4), (0.5, 3.6, 0.5), 'claw')
        box(M, f'{s}_fore', ((B[0] + C[0]) / 2, (B[1] + C[1]) / 2 + 1, (B[2] + C[2]) / 2), (3.8, 1.8, 3.8), 'iron')
        zero_bone(M, f'{s}_chain', f'{s}_fore', C)
        for k in range(7):
            M.cube_l(f'{s}_chain', (-0.9 + (k % 2) * 0.2, 0.5 + k * 2.1, -0.5 + (k % 2) * 0.6), (1.8, 1.8, 1.0), 'iron')
    box(M, 'body', (0, 10, 0), (9.6, 1.8, 6.6), 'iron')

    Pt = Painter(M)
    Pt.mats['ghost'] = mats.ghost(GHOST, 0.5, glow_col=(150, 190, 255))
    Pt.mats['wisp'] = mats.ragged(mats.ghost(GHOST, 0.5, glow_col=(150, 190, 255)), depth=6)
    Pt.mats['claw'] = mats.horn(pal('#1c2438', '#5a76a8', '#a8c4ec', '#e0f0ff'), 3)
    Pt.mats['iron'] = mats.metal(pal('#0e1014', '#1e2128', '#363b46', '#565d6a'))
    Pt.mats['maw'] = mats.solid((8, 10, 24), glow=False)
    Pt.mats['face'] = mats.face_on(mats.ghost(GHOST, 0.2, glow_col=(150, 190, 255), ragged=False), 'north', eyes=[(0.22, 0.34, 0.14, 0.16, 0.0, 'round')],
                                   mouth=dict(y=0.78, w=0.24, h=0.14, col=(6, 8, 20), teeth=0), eye_col=(10, 14, 30), glow_col=(120, 170, 255), eye_glow=0.0,
                                   brow=dict(y=0.22, w=0.2, th=0.05, angle=-0.5, col=(20, 30, 60)))

    A = {}
    for name, L, amp in (('IDLE', 3.0, 0.6), ('WALK', 1.5, 1.0)):
        a = Anim(name, L, True)
        gen(a, 'body', 'p', lambda ph: (0, 1.5 * S(ph), 0), n=10)
        gen(a, 'body', 'r', lambda ph, amp=amp: (4 * amp * S(ph, 2, 0.1) + (10 if name == 'WALK' else 0), 5 * S(ph, 1, 0.5), 4 * S(ph, 1, 0.2)), n=10)
        gen(a, 'head', 'r', lambda ph: (5 * S(ph, 2, 0.2) - 6, 8 * S(ph, 1, 0.25), 3 * S(ph, 1, 0.4)), n=10)
        for sx, s in ((-1, 'right'), (1, 'left')):
            gen(a, f'{s}_arm', 'r', lambda ph, sx=sx: (-24 + 14 * S(ph, 1, 0.25 * sx), 0, sx * (10 + 8 * S(ph, 1, 0.1))), n=10)
            gen(a, f'{s}_fore', 'r', lambda ph, sx=sx: (-12 + 10 * S(ph, 1, 0.45), 0, 0), n=10)
            gen(a, f'{s}_chain', 'r', lambda ph, sx=sx: (10 * S(ph, 1, 0.2 * sx), 0, 8 * S(ph, 2, 0.3)), n=10)
        for k in range(5):
            gen(a, f'wisp{k}', 'r', lambda ph, k=k, amp=amp: (10 * S(ph, 1, k * 0.17), 0, 12 * S(ph, 1, k * 0.23 + 0.3) * (0.5 + amp * 0.5)), n=10)
        A[name.lower()] = a
    d = Anim('DEATH', 24 / 20, False)
    L = d.length
    poses(d, [(0, {}), (L * 0.5, {'body': (-20, 0, 12), 'head': (-34, 0, 0), 'right_arm': (-110, 0, -40), 'left_arm': (-110, 0, 40)}), (L, {'body': (-50, 0, 22), 'head': (-50, 0, 0), 'right_arm': (-150, 0, -60), 'left_arm': (-150, 0, 60)})])
    scales(d, [(0, {'base': (1, 1, 1)}), (L * 0.6, {'base': (1.1, 0.95, 1.1)}), (L, {'base': (1.5, 0.05, 1.5)})])
    A['death'] = d
    A['reach'] = swing('REACH', 0.9, [(0, {}), (0.25, {'right_arm': (-140, 0, -14), 'left_arm': (-30, 0, 10), 'body': (-14, -18, 0), 'head': (-12, 0, 0)}), (0.4, {'right_arm': (-40, 0, 10), 'body': (22, 22, 0), 'head': (12, 0, 0)}), (0.9, {})])
    A['scream'] = swing('SCREAM', 1.8, [(0, {}), (0.6, {'body': (-24, 0, 0), 'head': (-40, 0, 0), 'right_arm': (-30, 0, -70), 'left_arm': (-30, 0, 70)}), (1.3, {'body': (-26, 0, 0), 'head': (-44, 0, 0), 'right_arm': (-34, 0, -76), 'left_arm': (-34, 0, 76)}), (1.8, {})])
    scales(A['scream'], [(0, {'head': (1, 1, 1)}), (0.6, {'head': (1.2, 1.25, 1.2)}), (1.3, {'head': (1.25, 1.3, 1.25)}), (1.8, {'head': (1, 1, 1)})])
    A['despair'] = swing('DESPAIR', 1.5, [(0, {}), (0.45, {'right_arm': (-60, 0, -50), 'left_arm': (-60, 0, 50), 'body': (-14, 0, 0), 'head': (-24, 0, 0)}), (0.7, {'right_arm': (-110, 0, -20), 'left_arm': (-110, 0, 20), 'body': (16, 0, 0), 'head': (10, 0, 0)}), (1.5, {})])
    A['drain'] = swing('DRAIN', 2.5, [(0, {}), (0.5, {'right_arm': (-100, 0, -16), 'left_arm': (-100, 0, 16), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'body': (14, 0, 0), 'head': (-14, 0, 0)}), (2.1, {'right_arm': (-104, 0, -20), 'left_arm': (-104, 0, 20), 'right_fore': (-34, 0, 0), 'left_fore': (-34, 0, 0), 'body': (16, 0, 0), 'head': (-16, 0, 0)}), (2.5, {})])
    return M, Pt, A
