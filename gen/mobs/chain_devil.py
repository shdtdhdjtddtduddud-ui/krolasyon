"""Zincir Şeytanı (Chain Devil) - horned devil bound in iron chains that it uses as whips."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'chain_devil'
SPEC = dict(
    faction='OUTCAST', tr='Zincir Şeytanı', en='Chain Devil', hp=58, armor=5, attack=8.0, speed=0.26, size=(0.8, 2.2, 1.0), reach=4.2, kb=0.4, follow=36,
    voice=('demon', 0.85), egg=(0x5A5A64, 0xC82030), spawn=(5, 1, 2, ['ember_wastes', 'ashen_barrens', 'bone_marches']), xp=20, death_ticks=26, anim=(1.5, 1.8),
    abilities=[
        dict(kind='MELEE', anim='lash', time=(22, 10, 15), cd=22, dmg=0.85, p=(0.5, 0), color=0xB0B0C0, fx='crit'),
        dict(kind='PULL', anim='hook', time=(28, 13), cd=120, w=4, range=(4, 15), dmg=0.8, p=(2.0, 0), color=0xB0B0C0, fx='crit', eff=('minecraft:slowness', 60, 1), los=True),
        dict(kind='SLAM', anim='whirl', time=(40, 22), cd=170, w=3, range=(0, 6), dmg=1.1, p=(5.0, 0), color=0xB0B0C0, fx='crit', eff=('minecraft:weakness', 80, 0)),
        dict(kind='BUFF', anim='clank', time=(26, 10), cd=380, w=1, range=(0, 6), p=(0, 8), color=0xC82030, fx='flame', eff=('minecraft:resistance', 160, 1)),
    ],
    drops=[('minecraft:iron_ingot', 1, 3, 0.8), ('minecraft:chain', 1, 4, 0.8), ('krolasyonbosses:hellsteel_nugget', 0, 3, 0.4)],
    lore='Hiçbir krallığa bağlı olmayan kölelerin ve cellâtların ruhu. Zincirlerini kendi elleriyle kırmadılar; şimdi başkalarını bağlıyorlar.',
)

SKIN = pal('#2a1014', '#4c1a22', '#7a2a34', '#a64450', '#cc6a74')
IRON = pal('#0e1014', '#1e2128', '#363b46', '#565d6a', '#8a93a2')


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(11, 11), leg_w=4.0, foot=(5, 2.6, 7.4), hip_w=8.6, pelvis_h=4.4, torso=(9.6, 11.5, 5.8), neck=2.0, head=(7.4, 7.8, 7.4),
                 arm=(10.5, 10), arm_w=3.6, hand=(3.2, 3.8, 3.2), hunch=8, arm_out=0.6, bend=(0.4, 0.4), head_fwd=1.5,
                 mats=dict(pelvis='cloth', torso='skin', head='face', arm='skin', fore='cuff', hand='skin', leg='cloth', shin='cloth', foot='iron'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    # horns
    for sx in (-1, 1):
        chain(M, f'horn{sx}', 'head', [(sx * 2.8, hc[1] - 3.6, hc[2] - 0.5), (sx * 5.2, hc[1] - 5.4, hc[2] - 1.5), (sx * 6.0, hc[1] - 9.6, hc[2] - 3.0)], (2.8, 1.8), 'horn', ext=0.2)
    # chains wrapped around the torso (X shape) and spiked collar
    for k, y in enumerate((3.2, 7.4, 11)):
        box(M, 'chest', (0, w[1] - y, 0), (10.4, 1.6, 6.6), 'chain')
    box(M, 'chest', (0, w[1] - 7.2, -3.2), (1.4, 12.0, 0.8), 'chain')
    for sx in (-1, 1):
        zero_bone(M, f'collar{sx}', 'neck', (sx * 3.6, K['neck'][1] + 0.4, K['neck'][2]))
        M.cube_l(f'collar{sx}', (-0.8, -3.6, -0.8), (1.6, 3.6, 1.6), 'iron')
        # iron cuffs
        for s in ('right', 'left'):
            pass
    for sx, s in ((-1, 'right'), (1, 'left')):
        Cc = K['hands'][s]
        box(M, f'{s}_fore', (Cc[0], Cc[1] - 2.6, Cc[2] + 1.0), (4.8, 2.4, 4.8), 'iron')
        # trailing chain from each wrist: 8 links ending in a hook/ball
        zero_bone(M, f'{s}_chain', f'{s}_hand', Cc)
        for k in range(9):
            M.cube_l(f'{s}_chain', (-0.9 + (k % 2) * 0.2, 1.5 + k * 2.1, -0.5 + (k % 2) * 0.6), (1.8, 1.8, 1.0), 'chainlink')
        M.cube_l(f'{s}_chain', (-1.8, 21, -1.8), (3.6, 3.6, 3.6), 'iron_ball')
    # loincloth & ragged cape
    zero_bone(M, 'cloth_f', 'hips', (0, hy - 0.8, -3.2))
    M.cube_l('cloth_f', (-3.4, 0, -0.4), (6.8, 11, 0.8), 'rag')
    zero_bone(M, 'tail', 'hips', (0, hy - 1, 3))
    chain(M, 'tl', 'hips', [(0, hy - 1, 3), (0, hy + 4, 7), (0, hy + 9, 10), (0, hy + 12, 15)], (1.8, 1.4, 1.1), 'skin', ext=0.3)

    Pt = Painter(M)
    Pt.mats['skin'] = mats.skin(SKIN, 0.35, veins=(255, 80, 60), scar=0.5)
    Pt.mats['cuff'] = mats.skin(SKIN, 0.35, scar=0.3)
    Pt.mats['cloth'] = mats.cloth(pal('#0c0608', '#1c0c10', '#34161e', '#4c2230'), 0.7)
    Pt.mats['rag'] = mats.ragged(mats.cloth(pal('#0c0608', '#1c0c10', '#34161e', '#4c2230'), 0.9, hem=(120, 120, 130)), depth=4)
    Pt.mats['iron'] = mats.metal(IRON)
    Pt.mats['iron_ball'] = mats.rock(IRON, cell=3.5, crack_w=0.3)
    Pt.mats['chain'] = mats.metal(pal('#101216', '#2a2e36', '#4a505c', '#7a8494'))
    Pt.mats['chainlink'] = mats.metal(pal('#101216', '#2a2e36', '#4a505c', '#7a8494', '#b8c0d0'))
    Pt.mats['horn'] = mats.horn(pal('#14100e', '#2e2420', '#5a4a40', '#8a7464'), 2)
    Pt.mats['face'] = mats.face_on(mats.skin(SKIN, 0.3, veins=(255, 80, 60)), 'north', eyes=[(0.22, 0.4, 0.13, 0.1, 0.4, 'almond')], mouth=dict(y=0.8, w=0.28, h=0.07, teeth=5, col=(255, 70, 60), curve=-0.4),
                                   brow=dict(y=0.28, w=0.2, th=0.06, angle=0.5, col=(10, 4, 4)), eye_col=(255, 220, 140), glow_col=(255, 120, 60))

    ba = dict(stride=28, shin=38, arm=12, elbow=10, lean=3, tail=['tl0', 'tl1', 'tl2'])
    A = biped_set(M, ba, death='fall_back', death_len=26 / 20)
    for nm in ('idle', 'walk', 'run'):
        for s in ('right', 'left'):
            gen(A[nm], f'{s}_chain', 'r', lambda ph, s=s: (10 * S(ph, 2, 0.1), 0, 8 * S(ph, 1, 0.3)), n=8)
    A['lash'] = swing('LASH', 1.2, [(0, {}), (0.3, {'right_arm': (-130, 0, -20), 'right_fore': (-30, 0, 0), 'chest': (-10, -24, 0), 'left_arm': (-30, 0, 20), 'head': (-8, 0, 0)}), (0.44, {'right_arm': (-40, 0, 10), 'chest': (20, 26, 0), 'head': (8, 0, 0)}),
                                     (0.62, {'left_arm': (-130, 0, 20), 'left_fore': (-30, 0, 0), 'chest': (-10, 24, 0)}), (0.76, {'left_arm': (-40, 0, -10), 'chest': (20, -26, 0)}), (1.2, {})])
    A['hook'] = swing('HOOK', 1.5, [(0, {}), (0.4, {'left_arm': (-100, 0, 40), 'left_fore': (-20, 0, 0), 'chest': (-8, 22, 0), 'head': (-8, -12, 0)}), (0.62, {'left_arm': (-100, 0, 0), 'left_fore': (-6, 0, 0), 'chest': (20, -18, 0), 'head': (8, 10, 0)}), (1.1, {'left_arm': (-70, 0, 0), 'chest': (14, -8, 0)}), (1.5, {})])
    A['whirl'] = swing('WHIRL', 2.0, [(0, {}), (0.4, {'right_arm': (-80, 0, -70), 'left_arm': (-80, 0, 70), 'chest': (0, 0, 0)}), (1.1, {'right_arm': (-80, 0, -80), 'left_arm': (-80, 0, 80), 'chest': (6, 360, 0)}), (2.0, {})])
    A['clank'] = swing('CLANK', 1.3, [(0, {}), (0.4, {'right_arm': (-90, 0, -50), 'left_arm': (-90, 0, 50), 'chest': (-14, 0, 0), 'head': (-24, 0, 0)}), (0.7, {'right_arm': (-30, 0, -10), 'left_arm': (-30, 0, 10), 'chest': (14, 0, 0), 'head': (10, 0, 0)}), (1.3, {})])
    return M, Pt, A
