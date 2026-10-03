"""Kan Rahibi (Blood Acolyte) - robed priest of the Blood Conclave with a chalice staff."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'blood_acolyte'
SPEC = dict(
    faction='BLOOD', tr='Kan Rahibi', en='Blood Acolyte', hp=32, armor=1, attack=4.5, speed=0.23, size=(0.65, 2.0, 1.0), reach=12, kb=0.1, follow=40,
    voice=('revenge', 1.3), egg=(0x5A0F1C, 0xF0E0D0), spawn=(7, 1, 2, ['crimson_gardens', 'blood_spires']), xp=13, death_ticks=22, anim=(1.5, 1.8),
    abilities=[
        dict(kind='BOLT', anim='cast', time=(22, 12), cd=26, dmg=1.0, range=(4, 20), p=(1.3, 1.0), color=0xC8102E, fx='drip', los=True, eff=('minecraft:hunger', 60, 1)),
        dict(kind='DRAIN', anim='drain', time=(60, 14), cd=170, w=4, range=(3, 13), dmg=0.5, p=(0, 1.0), color=0xE0102E, fx='drip', los=True),
        dict(kind='HEAL_ALLIES', anim='blessing', time=(36, 18), cd=240, w=3, range=(0, 14), p=(10, 14), color=0xFF4060, fx='heart'),
        dict(kind='SUMMON', anim='summon', time=(40, 20), cd=330, w=2, range=(0, 18), p=(2, 4), summon='leech_bat', color=0xC8102E, fx='drip'),
        dict(kind='RETREAT', anim='step', time=(18, 5), cd=100, w=3, range=(0, 4), p=(0.9, 0.35), color=0xC8102E, fx='drip'),
    ],
    drops=[('krolasyonbosses:blood_vial', 0, 2, 0.6), ('minecraft:redstone', 1, 4, 0.8)],
    lore='Kan Konseyi\'nın rahipleri. Kadehlerinde sabaha kadar dökülen kan taşınır; yaralıyı iyileştirir, düşmanın damarlarını boşaltır.',
)

WINE = pal('#14040a', '#2c0a14', '#4c1022', '#74182f', '#9c2440')
PALE = pal('#5a4a48', '#8a7670', '#b8a39a', '#dcc8bc', '#f2e4d8')
GOLD = pal('#5a4010', '#8f6a18', '#c9a227', '#f0d060')


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(11, 11), leg_w=3.6, foot=(4.4, 2.2, 6.6), hip_w=8, pelvis_h=4, torso=(8.4, 11, 5.4), neck=2.2, head=(7, 7.6, 7),
                 arm=(10, 9), arm_w=3.4, hand=(2.8, 3.6, 2.8), hunch=4, arm_out=0.5, bend=(0.7, 0.45), head_fwd=1,
                 mats=dict(pelvis='robe', torso='robe', head='face', arm='sleeve', fore='sleeve', hand='pale', leg='robe', shin='robe', foot='boot'))
    hy = K['hip_y']; hc = K['head_c']
    # long robe skirt
    zero_bone(M, 'skirt', 'hips', (0, hy - 1, 0))
    M.cube_l('skirt', (-6.4, 0, -4.4), (12.8, 21.5, 8.8), 'robe_hem')
    # hood + collar
    box(M, 'head', (hc[0], hc[1] - 3.6, hc[2] + 0.1), (8.8, 2.8, 8.8), 'hood')
    box(M, 'head', (hc[0], hc[1] + 0.4, hc[2] + 3.7), (8.8, 9.0, 2.0), 'hood')
    for sx in (-1, 1):
        box(M, 'head', (hc[0] + sx * 4.0, hc[1] - 0.1, hc[2] + 0.5), (1.6, 8.6, 8.2), 'hood')
    box(M, 'head', (hc[0], hc[1] - 4.5, hc[2] - 2.7), (6.4, 1.1, 3.4), 'hood')
    box(M, 'neck', (0, K['neck'][1] + 0.8, K['neck'][2]), (10.4, 2.6, 7.0), 'collar')
    # gold sash and wide sleeves
    box(M, 'chest', (0, K['waist'][1] - 3.2, -0.2), (9.2, 2.0, 6.2), 'gold')
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']; B = K[f'{s}_B']
        box(M, f'{s}_fore', ((B[0] + K['hands'][s][0]) / 2 + sx * 0.8, (B[1] + K['hands'][s][1]) / 2 + 1.2, (B[2] + K['hands'][s][2]) / 2), (6.6, 6.6, 6.0), 'sleeve_wide')
        box(M, f'{s}_arm', (A[0] + sx * 1.2, A[1] - 0.5, A[2]), (5.2, 2.4, 6.0), 'gold')
    # chalice staff in the right hand
    hr = K['hands']['right']
    M.cube_l('right_hand', (-0.6, -26, -0.6), (1.2, 40, 1.2), 'staff')
    for k in range(3):
        M.cube_l('right_hand', (-1.2, -9 - k * 6, -1.2), (2.4, 1.2, 2.4), 'gold')
    zero_bone(M, 'chalice', 'right_hand', (hr[0], hr[1] - 25.6, hr[2]))
    M.cube_l('chalice', (-2.6, -5.2, -2.6), (5.2, 3.4, 5.2), 'gold')
    M.cube_l('chalice', (-2.0, -7.0, -2.0), (4.0, 2.0, 4.0), 'blood')
    M.cube_l('chalice', (-1.2, -9.2, -1.2), (2.4, 2.4, 2.4), 'blood')
    # rosary beads hanging from the belt
    zero_bone(M, 'beads', 'hips', (-4.2, hy - 1, -4.6))
    for k in range(5):
        M.cube_l('beads', (-0.5, k * 1.9, -0.5), (1.0, 1.2, 1.0), 'blood')

    Pt = Painter(M)
    Pt.mats['robe'] = mats.cloth(WINE, 1.0)
    Pt.mats['robe_hem'] = mats.cloth(WINE, 1.1, hem=(201, 162, 39), stripe=(201, 162, 39), trim=(201, 162, 39))
    Pt.mats['sleeve'] = mats.cloth(WINE, 0.8)
    Pt.mats['sleeve_wide'] = mats.cloth(WINE, 1.2, hem=(201, 162, 39))
    Pt.mats['hood'] = mats.cloth(pal('#0c0206', '#1a050c', '#2e0a16', '#4a1224'), 0.9)
    Pt.mats['collar'] = mats.cloth(PALE, 0.5, hem=(201, 162, 39))
    Pt.mats['gold'] = mats.metal(GOLD)
    Pt.mats['pale'] = mats.skin(PALE, 0.25)
    Pt.mats['boot'] = mats.cloth(pal('#0a0408', '#1a0a10', '#2c141c'), 0.3)
    Pt.mats['face'] = mats.face_on(mats.skin(PALE, 0.25, vgrad=0.1), 'north', eyes=[(0.22, 0.4, 0.12, 0.1, 0.1, 'almond')],
                                   mouth=dict(y=0.8, w=0.2, h=0.05, col=(120, 20, 40), teeth=0), brow=dict(y=0.28, w=0.17, th=0.05, angle=0.2, col=(60, 20, 30)),
                                   eye_col=(255, 60, 80), glow_col=(255, 40, 70))
    Pt.mats['staff'] = mats.horn(pal('#1a0c08', '#3a1c10', '#5a3018', '#7a4824'), 4)
    Pt.mats['blood'] = mats.solid((220, 20, 50))

    ba = dict(stride=24, shin=34, arm=8, elbow=6, lean=2)
    A = biped_set(M, ba, death='fall_back', death_len=22 / 20)
    A['cast'] = swing('CAST', 1.1, [(0, {}), (0.3, {'right_arm': (-40, 0, -20), 'right_fore': (-60, 0, 0), 'left_arm': (-50, 0, 24), 'chest': (-8, 0, 0), 'head': (-12, 0, 0)}),
                                   (0.5, {'right_arm': (-100, 0, -10), 'right_fore': (-10, 0, 0), 'left_arm': (-90, 0, 10), 'chest': (10, 0, 0), 'head': (8, 0, 0)}), (1.1, {})])
    A['drain'] = swing('DRAIN', 3.0, [(0, {}), (0.7, {'right_arm': (-100, 0, -10), 'left_arm': (-100, 0, 10), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (10, 0, 0), 'head': (-10, 0, 0)}),
                                     (2.6, {'right_arm': (-104, 0, -14), 'left_arm': (-104, 0, 14), 'right_fore': (-34, 0, 0), 'left_fore': (-34, 0, 0), 'chest': (12, 0, 0), 'head': (-12, 0, 0)}), (3.0, {})])
    A['blessing'] = swing('BLESSING', 1.8, [(0, {}), (0.5, {'right_arm': (-150, 0, -40), 'left_arm': (-150, 0, 40), 'chest': (-14, 0, 0), 'head': (-24, 0, 0)}), (1.3, {'right_arm': (-154, 0, -44), 'left_arm': (-154, 0, 44), 'chest': (-16, 0, 0), 'head': (-28, 0, 0)}), (1.8, {})])
    A['summon'] = swing('SUMMON', 2.0, [(0, {}), (0.5, {'right_arm': (-80, 0, -10), 'right_fore': (-20, 0, 0), 'left_arm': (-110, 0, 30), 'chest': (-10, 0, 0), 'head': (-20, 0, 0)}),
                                       (1.0, {'right_arm': (-130, 0, -10), 'left_arm': (-130, 0, 20), 'chest': (-14, 0, 0), 'head': (-26, 0, 0)}), (2.0, {})])
    A['step'] = swing('STEP', 0.9, [(0, {}), (0.22, {'chest': (-8, 0, 0), 'right_leg': (14, 0, 0), 'left_leg': (14, 0, 0), 'right_shin': (30, 0, 0), 'left_shin': (30, 0, 0)}), (0.45, {'chest': (10, 0, 0), 'right_leg': (-14, 0, 0), 'left_leg': (-14, 0, 0)}), (0.9, {})])
    return M, Pt, A
