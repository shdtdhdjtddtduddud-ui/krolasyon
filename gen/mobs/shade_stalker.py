"""Gölge Avcısı (Shade Stalker) - lanky smoke-bodied assassin that steps out of shadows behind its prey."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'shade_stalker'
SPEC = dict(
    faction='SHADOW', tr='Gölge Avcısı', en='Shade Stalker', hp=38, armor=1, attack=7.0, speed=0.34, size=(0.65, 2.3, 1.0), reach=2.8, kb=0.15, follow=40,
    voice=('revenge', 1.65), egg=(0x14082A, 0xC8A0FF), spawn=(7, 1, 2, ['umbral_reach', 'gloam_forest']), xp=15, death_ticks=22, anim=(1.8, 2.4),
    abilities=[
        dict(kind='MELEE', anim='claw', time=(18, 7, 12), cd=18, dmg=0.85, p=(0.35, 0), color=0xA070FF, fx='void', eff=('minecraft:wither', 40, 0)),
        dict(kind='TELEPORT_STRIKE', anim='ambush', time=(26, 7, 15), cd=120, w=5, range=(3, 16), dmg=1.5, p=(0, 0), color=0x7040C0, fx='void'),
        dict(kind='BLINK_AWAY', anim='fade', time=(14, 5), cd=140, w=2, range=(0, 6), p=(10, 0), color=0x7040C0, fx='void'),
        dict(kind='BUFF', anim='cloak', time=(24, 10), cd=360, w=1, range=(0, 12), p=(0, 4), color=0x7040C0, fx='void', eff=('minecraft:speed', 160, 1)),
    ],
    drops=[('krolasyonbosses:shadow_essence', 0, 2, 0.5), ('minecraft:ender_pearl', 0, 1, 0.15)],
    lore='Gölge Tarikatı\'nın sessiz bıçakları. Işık azaldığında şekil bulur, gölgeyi bırakırken yok olur; son bildiğin şey boynundaki soğuktur.',
)

VOID = pal('#05030a', '#0e0820', '#1c1238', '#30205c', '#4c3690')
GLOW = (190, 140, 255)


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(12.5, 12.5), leg_w=2.8, foot=(3.4, 2.0, 7), hip_w=6.6, pelvis_h=3.6, torso=(6.4, 12, 4), neck=2.6, head=(5.6, 9, 5.6),
                 arm=(13.5, 13.5), arm_w=2.3, hand=(2.2, 3.0, 2.2), hunch=14, arm_out=0.4, bend=(0.15, 0.15), head_fwd=3, stance=0.3,
                 mats=dict(pelvis='smoke', torso='smoke', head='face', arm='smoke', fore='smoke', hand='claw', leg='smoke', shin='smoke', foot='claw'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    # long claws
    for sx, s in ((-1, 'right'), (1, 'left')):
        for k in range(3):
            M.cube_l(f'{s}_hand', (-1.1 + k * 0.9, 2.4, -0.8 + (k % 2) * 0.5), (0.5, 6.4 - (k % 2) * 1.2, 0.5), 'claw')
    # smoky shreds trailing from the back and shoulders
    for k, (x, y, h) in enumerate(((-3.0, 4, 12), (0, 2, 16), (3.0, 4, 12), (-1.6, 9, 10), (1.8, 9, 11))):
        zero_bone(M, f'shred{k}', 'chest', (x, w[1] - 12 + y, 3.0))
        M.cube_l(f'shred{k}', (-1.3, 0, -0.3), (2.6, h, 0.6), 'shred')
    # horn-like head spikes and shoulder spikes
    for sx in (-1, 1):
        chain(M, f'hs{sx}', 'head', [(sx * 1.8, hc[1] - 3.5, hc[2] + 1.5), (sx * 3.4, hc[1] - 6.0, hc[2] + 3.4), (sx * 3.6, hc[1] - 8.2, hc[2] + 6.4)], (1.8, 1.2), 'claw', ext=0.2)
        A = K['left_A'] if sx > 0 else K['right_A']
        zero_bone(M, f'sp{sx}', 'chest', (A[0] + sx * 1.5, A[1] - 1, A[2]))
        M.cube_l(f'sp{sx}', (-0.8, -7, -0.8), (1.6, 7, 1.6), 'claw')
    # spine ridge
    for k in range(5):
        zero_bone(M, f'ridge{k}', 'chest', (0, w[1] - 11 + k * 2.3, 2.6))
        M.cube_l(f'ridge{k}', (-0.7, -1.2, 0), (1.4, 1.2, 3.2 - k * 0.3), 'claw')

    Pt = Painter(M)
    Pt.mats['smoke'] = mats.with_glow_streaks(mats.skin(VOID, 0.5, vgrad=0.25, pores=0.03), GLOW, 0.25, True)
    Pt.mats['shred'] = mats.ragged(mats.skin(VOID, 0.5, vgrad=0.1), depth=7)
    Pt.mats['claw'] = mats.horn(pal('#0a0618', '#201440', '#4a3490', '#8a6ad8'), 3, tip_glow=GLOW)
    Pt.mats['face'] = mats.face_on(mats.skin(VOID, 0.4, vgrad=0.1), 'north', eyes=[(0.2, 0.34, 0.14, 0.05, 0.5, 'slit')], eye_col=(240, 230, 255), glow_col=GLOW, eye_glow=1.0)

    ba = dict(stride=34, shin=46, arm=18, elbow=10, lean=8, tail=None, bob=0.5)
    A = biped_set(M, ba, death='shrink', death_len=22 / 20)
    for k in range(5):
        for nm in ('IDLE', 'WALK', 'RUN'):
            if nm.lower() in A:
                gen(A[nm.lower()], f'shred{k}', 'r', lambda ph, k=k: (12 * S(ph, 1, k * 0.2), 0, 10 * S(ph, 1, k * 0.15 + 0.3)), n=8)
    A['claw'] = swing('CLAW', 1.0, [(0, {}), (0.22, {'right_arm': (-110, 0, -12), 'left_arm': (-30, 0, 16), 'chest': (-8, -26, 0), 'head': (-6, 0, 0)}), (0.34, {'right_arm': (-30, 0, 18), 'chest': (22, 28, 0), 'head': (10, 0, 0)}),
                                   (0.52, {'left_arm': (-110, 0, 12), 'right_arm': (-30, 0, -10), 'chest': (-6, 26, 0)}), (0.64, {'left_arm': (-30, 0, -18), 'chest': (22, -28, 0)}), (1.0, {})])
    A['ambush'] = swing('AMBUSH', 1.3, [(0, {}), (0.3, {'chest': (-12, 0, 0), 'right_arm': (-30, 0, -40), 'left_arm': (-30, 0, 40), 'head': (-12, 0, 0)}), (0.46, {}), (0.7, {'right_arm': (-130, 0, -6), 'left_arm': (-110, 0, 6), 'chest': (20, 10, 0), 'head': (8, 0, 0)}),
                                       (0.85, {'right_arm': (-30, 0, 12), 'left_arm': (-30, 0, -12), 'chest': (26, -10, 0)}), (1.3, {})])
    scales(A['ambush'], [(0, {'base': (1, 1, 1)}), (0.28, {'base': (0.25, 1.15, 0.25)}), (0.42, {'base': (1, 1, 1)})])
    A['fade'] = swing('FADE', 0.7, [(0, {}), (0.25, {'chest': (10, 0, 0)}), (0.7, {})])
    scales(A['fade'], [(0, {'base': (1, 1, 1)}), (0.25, {'base': (0.2, 1.1, 0.2)}), (0.7, {'base': (1, 1, 1)})])
    A['cloak'] = swing('CLOAK', 1.2, [(0, {}), (0.4, {'right_arm': (-50, 0, -60), 'left_arm': (-50, 0, 60), 'chest': (-10, 0, 0), 'head': (-20, 0, 0)}), (0.8, {'right_arm': (-30, 0, -20), 'left_arm': (-30, 0, 20), 'chest': (10, 0, 0)}), (1.2, {})])
    return M, Pt, A
