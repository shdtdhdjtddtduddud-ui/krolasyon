"""Kızıl Düellocu (Crimson Duelist) - masked fencer in a red coat who steps through blood."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'crimson_duelist'
SPEC = dict(
    faction='BLOOD', tr='Kızıl Düellocu', en='Crimson Duelist', hp=42, armor=3, attack=6.5, speed=0.32, size=(0.65, 2.0, 1.0), reach=2.7, kb=0.2, follow=36,
    voice=('revenge', 1.45), egg=(0xB01830, 0xF2EAE0), spawn=(6, 1, 2, ['crimson_gardens', 'blood_spires']), xp=16, death_ticks=22, anim=(1.8, 2.2),
    abilities=[
        dict(kind='MELEE', anim='slash', time=(24, 7, 13, 18), cd=22, dmg=0.8, p=(0.4, 0), color=0xFF2040, fx='drip'),
        dict(kind='TELEPORT_STRIKE', anim='bloodstep', time=(26, 8, 16), cd=130, w=4, range=(3, 14), dmg=1.5, p=(0, 0), color=0xC8102E, fx='drip', eff=('minecraft:poison', 60, 0)),
        dict(kind='CHARGE', anim='lunge', time=(26, 9), cd=110, w=3, range=(4, 12), dmg=1.4, p=(7, 1.2), color=0xFF2040, fx='crit'),
        dict(kind='SHIELD', anim='parry', time=(40, 4), cd=180, w=2, range=(0, 6), p=(0.9, 1.0), color=0xFF6070, fx='crit'),
    ],
    drops=[('krolasyonbosses:blood_vial', 0, 1, 0.5), ('minecraft:gold_nugget', 1, 4, 0.7), ('minecraft:feather', 0, 2, 0.4)],
    lore='Kan Konseyi\'nın düello ustaları. Maskelerinin altındaki yüzü kimse görmedi; rakibinin arkasında beliriverir ve gülümseyerek bıçaklar.',
)

RED = pal('#1a0408', '#3c0a14', '#6c1424', '#a0203a', '#d03654')
BLK = pal('#08060a', '#14101a', '#241c2e', '#382c46')
PALE = pal('#6a5a58', '#a8948c', '#d8c8bc', '#f4eae0')
GOLD = pal('#5a4010', '#8f6a18', '#c9a227', '#f0d060')


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(11.5, 11.5), leg_w=3.2, foot=(4.0, 2.2, 7), hip_w=7.4, pelvis_h=3.8, torso=(7.4, 10.5, 4.6), neck=2.0, head=(6.8, 7.4, 6.8),
                 arm=(10, 9.5), arm_w=2.8, hand=(2.4, 3.2, 2.4), hunch=5, arm_out=0.6, bend=(0.55, 0.3), head_fwd=1,
                 mats=dict(pelvis='black', torso='coat', head='mask', arm='coat', fore='glove', hand='glove', leg='black', shin='boot', foot='boot'))
    hy = K['hip_y']; hc = K['head_c']
    # coat tails and lapels
    zero_bone(M, 'tails', 'hips', (0, hy - 1.5, 2.2))
    M.cube_l('tails', (-4.4, 0, 0), (8.8, 17.0, 1.0), 'coat_tail')
    for sx in (-1, 1):
        zero_bone(M, f'tail{sx}', 'hips', (sx * 3.8, hy - 1.5, 1.0))
        M.cube_l(f'tail{sx}', (-0.5, 0, -2.0), (1.0, 14, 4.4), 'coat_tail')
    box(M, 'chest', (0, K['waist'][1] - 7.2, -2.6), (3.0, 9.6, 1.0), 'ruff')
    box(M, 'neck', (0, K['neck'][1] + 0.6, K['neck'][2]), (7.8, 2.6, 6.0), 'ruff')
    # hat: wide brim + plume
    box(M, 'head', (hc[0], hc[1] - 4.4, hc[2]), (11.6, 1.0, 11.0), 'hat')
    box(M, 'head', (hc[0], hc[1] - 6.4, hc[2] + 0.3), (7.8, 3.6, 7.6), 'hat')
    box(M, 'head', (hc[0], hc[1] - 5.2, hc[2] - 3.8), (7.9, 1.0, 0.6), 'gold')
    zero_bone(M, 'plume', 'head', (3.4, hc[1] - 7.0, hc[2] + 1.0))
    M.cube_l('plume', (-0.8, -9, -0.8), (1.6, 9, 4.6), 'plume')
    # rapier in the right hand, cape draped on the left shoulder
    sword(M, 'right_hand', length=26, width=1.4, thick=0.8, mat='blade', guard=(5.6, 1.0, 1.0), guard_mat='gold', grip=3.4, grip_mat='glove', dir='fwd', offset=(0, 0, 0.5))
    box(M, 'right_hand', (0, 1.0, -2.6), (3.2, 3.2, 1.0), 'gold')  # cup hilt
    zero_bone(M, 'cape', 'chest', (4.2, K['waist'][1] - 11.6, 1.8))
    M.cube_l('cape', (-3, 0, 0), (9.0, 18.0, 0.9), 'cape')

    Pt = Painter(M)
    Pt.mats['coat'] = mats.cloth(RED, 0.9, trim=(201, 162, 39))
    Pt.mats['coat_tail'] = mats.ragged(mats.cloth(RED, 1.0, hem=(201, 162, 39)), depth=2)
    Pt.mats['black'] = mats.cloth(BLK, 0.5)
    Pt.mats['boot'] = mats.cloth(pal('#07050a', '#140e18', '#241a2c', '#3a2c48'), 0.3, hem=(201, 162, 39))
    Pt.mats['glove'] = mats.cloth(pal('#0c0608', '#1c0c12', '#34161e', '#4c2230'), 0.3, weave=False)
    Pt.mats['ruff'] = mats.cloth(pal('#6a5a58', '#a8948c', '#d8c8bc', '#f8f0e8'), 1.2)
    Pt.mats['hat'] = mats.cloth(pal('#07050a', '#14101a', '#241c2e', '#382c46'), 0.7)
    Pt.mats['plume'] = mats.fur(pal('#6c1424', '#a0203a', '#d03654', '#ff7088'), 3)
    Pt.mats['gold'] = mats.metal(GOLD)
    Pt.mats['cape'] = mats.ragged(mats.cloth(pal('#1a0408', '#3c0a14', '#6c1424', '#a0203a'), 1.1, trim=(201, 162, 39)), depth=3)
    Pt.mats['blade'] = mats.metal(pal('#3a4450', '#6a7a88', '#aebccc', '#e8f0f8', '#ffffff'), glow_edge=(255, 60, 90))
    Pt.mats['mask'] = mats.face_on(mats.skin(PALE, 0.15, vgrad=0.1), 'north', eyes=[(0.22, 0.4, 0.13, 0.11, 0.0, 'slit')],
                                   mouth=dict(y=0.78, w=0.2, h=0.05, col=(150, 24, 44), teeth=0, curve=-0.6), eye_col=(255, 60, 80), glow_col=(255, 40, 70),
                                   extra=lambda col, g, H, W, r: _tear(col, g, H, W))

    ba = dict(stride=36, shin=44, arm=10, elbow=8, lean=3)
    A = biped_set(M, ba, death='fall_back', death_len=22 / 20)
    A['slash'] = swing('SLASH', 1.2, [(0, {}), (0.25, {'right_arm': (-80, 0, -10), 'right_fore': (-50, 0, 0), 'chest': (0, -22, 0), 'left_arm': (-20, 0, 40), 'head': (0, 18, 0)}),
                                     (0.33, {'right_arm': (-95, 0, 0), 'right_fore': (-20, 0, 0), 'chest': (10, 14, 0)}), (0.5, {'right_arm': (-90, 20, -20), 'right_fore': (-30, 0, 0), 'chest': (4, -18, 0)}),
                                     (0.64, {'right_arm': (-95, -10, 4), 'right_fore': (-10, 0, 0), 'chest': (12, 16, 0)}), (1.2, {})])
    A['bloodstep'] = swing('BLOODSTEP', 1.3, [(0, {}), (0.3, {'chest': (-12, 0, 0), 'head': (-10, 0, 0), 'right_arm': (-40, 0, -30), 'left_arm': (-40, 0, 30)}), (0.5, {}),
                                             (0.8, {'right_arm': (-100, 0, 0), 'right_fore': (-20, 0, 0), 'chest': (16, 12, 0), 'left_arm': (-20, 0, 40)}), (1.3, {})])
    scales(A['bloodstep'], [(0, {'base': (1, 1, 1)}), (0.28, {'base': (0.3, 1.1, 0.3)}), (0.42, {'base': (1, 1, 1)})])
    A['lunge'] = swing('LUNGE', 1.3, [(0, {}), (0.4, {'right_arm': (-70, 0, -10), 'right_fore': (-80, 0, 0), 'chest': (-10, -26, 0), 'right_leg': (14, 0, 0), 'left_leg': (-10, 0, 0), 'head': (0, 20, 0), 'left_arm': (-30, 0, 50)}),
                                     (0.55, {'right_arm': (-95, 0, 0), 'right_fore': (-5, 0, 0), 'chest': (30, 8, 0), 'right_leg': (-40, 0, 0), 'left_leg': (30, 0, 0), 'right_shin': (4, 0, 0), 'left_shin': (30, 0, 0), 'left_arm': (30, 0, 50)}),
                                     (1.0, {'right_arm': (-95, 0, 0), 'chest': (28, 8, 0), 'right_leg': (-40, 0, 0), 'left_leg': (30, 0, 0)}), (1.3, {})])
    A['parry'] = swing('PARRY', 2.0, [(0, {}), (0.3, {'right_arm': (-60, 0, -40), 'right_fore': (-90, 0, 0), 'chest': (-6, -16, 0), 'left_arm': (-10, 0, 60), 'head': (0, 12, 0)}),
                                     (1.6, {'right_arm': (-64, 0, -44), 'right_fore': (-92, 0, 0), 'chest': (-6, -18, 0), 'left_arm': (-10, 0, 64), 'head': (0, 12, 0)}), (2.0, {})])
    return M, Pt, A


def _tear(col, g, H, W):
    # a single crimson tear under the right eye
    x, y = int(W * 0.3), int(H * 0.52)
    for k in range(3):
        if 0 <= y + k < H:
            col[y + k, x] = (190, 20, 50)
            g[y + k, x] = (255, 40, 70, 120)
    return col, g
