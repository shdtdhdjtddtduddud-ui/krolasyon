"""Boşluk Tarikatçısı (Void Cultist) - hooded caster of the Shadow Covenant with a void-orb staff and a rune halo."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'void_cultist'
SPEC = dict(
    faction='SHADOW', tr='Boşluk Tarikatçısı', en='Void Cultist', hp=34, armor=1, attack=5.0, speed=0.24, size=(0.65, 2.0, 1.0), reach=13, kb=0.1, follow=40,
    voice=('revenge', 1.2), egg=(0x1C1038, 0xA070FF), spawn=(7, 1, 2, ['umbral_reach', 'gloam_forest']), xp=14, death_ticks=22, anim=(1.5, 1.8),
    abilities=[
        dict(kind='BOLT', anim='cast', time=(22, 12), cd=26, dmg=1.0, range=(4, 20), p=(1.2, 1.2), color=0x8A5CFF, fx='void', los=True, eff=('minecraft:blindness', 40, 0)),
        dict(kind='PULL', anim='grasp', time=(30, 14), cd=140, w=4, range=(4, 15), dmg=0.7, p=(1.8, 0), color=0x8A5CFF, fx='void', eff=('minecraft:slowness', 50, 1), los=True),
        dict(kind='CLOUD', anim='darkness', time=(34, 16), cd=200, w=3, range=(3, 14), dmg=0, p=(3.8, 150), color=0x1A0A30, fx='void', eff=('minecraft:blindness', 80, 0)),
        dict(kind='SUMMON', anim='summon', time=(44, 22), cd=420, w=2, range=(0, 18), p=(1, 2), summon='shade_stalker', color=0x8A5CFF, fx='void'),
        dict(kind='BLINK_AWAY', anim='fade', time=(14, 5), cd=130, w=3, range=(0, 5), p=(9, 0), color=0x7040C0, fx='void'),
    ],
    drops=[('krolasyonbosses:shadow_essence', 0, 2, 0.6), ('minecraft:ender_pearl', 0, 1, 0.2)],
    lore='Gölge Tarikatı\'nın vaizleri. Başlarındaki halka boşluğun kapısıdır; çağırdıklarının hiçbiri geri dönmek istemez.',
)

ROBE = pal('#07040f', '#120a24', '#201440', '#34246a', '#50408f')
PALE = pal('#3a3248', '#6a5e84', '#9c90b8', '#cfc4e4')
GLOW = (170, 120, 255)


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(11, 11), leg_w=3.6, foot=(4.4, 2.2, 6.6), hip_w=8, pelvis_h=4, torso=(8.2, 11, 5.2), neck=2.2, head=(7, 7.6, 7),
                 arm=(10, 9), arm_w=3.3, hand=(2.8, 3.6, 2.8), hunch=3, arm_out=0.5, bend=(0.7, 0.45), head_fwd=1,
                 mats=dict(pelvis='robe', torso='robe', head='face', arm='sleeve', fore='sleeve', hand='pale', leg='robe', shin='robe', foot='boot'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    zero_bone(M, 'skirt', 'hips', (0, hy - 1, 0))
    M.cube_l('skirt', (-6.2, 0, -4.4), (12.4, 21.5, 8.8), 'robe_hem')
    # deep hood
    box(M, 'head', (hc[0], hc[1] - 3.6, hc[2] + 0.1), (8.8, 2.8, 8.8), 'hood')
    box(M, 'head', (hc[0], hc[1] + 0.4, hc[2] + 3.7), (8.8, 9.0, 2.0), 'hood')
    for sx in (-1, 1):
        box(M, 'head', (hc[0] + sx * 4.0, hc[1] - 0.1, hc[2] + 0.5), (1.6, 8.6, 8.2), 'hood')
    box(M, 'head', (hc[0], hc[1] - 4.5, hc[2] - 2.7), (6.4, 1.1, 3.4), 'hood')
    # void halo: ring of 10 blocks standing behind the head (vertical plane)
    zero_bone(M, 'halo', 'head', (0, hc[1] - 0.5, hc[2] + 6.5))
    for k in range(16):
        a = k * math.tau / 16
        M.cube_l('halo', (math.cos(a) * 10.0 - 1.7, math.sin(a) * 10.0 - 1.7, -0.8), (3.4, 3.4, 1.6), 'rune')
    # shoulder mantle and belt
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']; B = K[f'{s}_B']
        box(M, f'{s}_fore', ((B[0] + K['hands'][s][0]) / 2 + sx * 0.8, (B[1] + K['hands'][s][1]) / 2 + 1.2, (B[2] + K['hands'][s][2]) / 2), (6.4, 6.4, 6.0), 'sleeve_wide')
        box(M, f'{s}_arm', (A[0] + sx * 1.2, A[1] - 0.5, A[2]), (5.4, 2.6, 6.2), 'mantle')
    box(M, 'chest', (0, w[1] - 2.0, 0), (9.0, 1.8, 6.0), 'rune')
    # staff with void orb and curved prongs
    hr = K['hands']['right']
    M.cube_l('right_hand', (-0.6, -26, -0.6), (1.2, 40, 1.2), 'staff')
    zero_bone(M, 'orb', 'right_hand', (hr[0], hr[1] - 26, hr[2]))
    M.cube_l('orb', (-2.2, -4.4, -2.2), (4.4, 4.4, 4.4), 'orb')
    for sx in (-1, 1):
        M.cube_l('orb', (sx * 2.6 - 0.5, -6.6, -0.5), (1.0, 6.4, 1.0), 'staff')
    # orbiting runes
    for k in range(3):
        zero_bone(M, f'orbit{k}', 'chest', (0, w[1] - 8, 0))
        M.cube_l(f'orbit{k}', (9.0 - 1.0, -1.0, -1.0), (2.0, 2.0, 2.0), 'rune')

    Pt = Painter(M)
    Pt.mats['robe'] = mats.cloth(ROBE, 1.0)
    Pt.mats['robe_hem'] = mats.cloth(ROBE, 1.1, hem=(120, 80, 220), stripe=(120, 80, 220), trim=(90, 60, 180))
    Pt.mats['sleeve'] = mats.cloth(ROBE, 0.8)
    Pt.mats['sleeve_wide'] = mats.cloth(ROBE, 1.2, hem=(120, 80, 220))
    Pt.mats['hood'] = mats.cloth(pal('#04020a', '#0c0618', '#180e30', '#2a1c50'), 0.9)
    Pt.mats['mantle'] = mats.cloth(pal('#0a0618', '#1c1238', '#34246a', '#5a46a8'), 0.7, hem=(150, 110, 255))
    Pt.mats['pale'] = mats.skin(PALE, 0.25)
    Pt.mats['boot'] = mats.cloth(pal('#07040f', '#120a24', '#201440'), 0.3)
    Pt.mats['rune'] = mats.solid((160, 110, 255))
    Pt.mats['orb'] = mats.crystal(pal('#1c0a40', '#3c1c88', '#6a3cd0', '#a07aff'), (170, 120, 255), 3)
    Pt.mats['staff'] = mats.horn(pal('#0c0818', '#1c1230', '#30224e', '#48367a'), 4)
    Pt.mats['face'] = mats.face_on(mats.skin(PALE, 0.25, vgrad=0.1), 'north', eyes=[(0.22, 0.4, 0.12, 0.1, 0.1, 'almond')],
                                   mouth=dict(y=0.8, w=0.2, h=0.05, col=(60, 40, 100), teeth=0), eye_col=(220, 190, 255), glow_col=GLOW)

    ba = dict(stride=24, shin=34, arm=8, elbow=6, lean=2)
    A = biped_set(M, ba, death='fall_back', death_len=22 / 20)
    for nm in ('idle', 'walk', 'run'):
        gen(A[nm], 'halo', 'r', lambda ph: (0, 0, 360 * ph), n=12, L=A[nm].length) if False else None
    for k in range(3):
        a = Anim('X', 1, True)
        for nm in ('idle', 'walk', 'run'):
            gen(A[nm], f'orbit{k}', 'r', lambda ph, k=k: (0, 360 * ph + k * 120, 0), n=12)
    A['cast'] = swing('CAST', 1.1, [(0, {}), (0.3, {'right_arm': (-40, 0, -20), 'right_fore': (-60, 0, 0), 'left_arm': (-50, 0, 24), 'chest': (-8, 0, 0), 'head': (-12, 0, 0)}),
                                   (0.5, {'right_arm': (-100, 0, -10), 'right_fore': (-10, 0, 0), 'left_arm': (-90, 0, 10), 'chest': (10, 0, 0), 'head': (8, 0, 0)}), (1.1, {})])
    A['grasp'] = swing('GRASP', 1.5, [(0, {}), (0.4, {'left_arm': (-100, 0, 30), 'left_fore': (-20, 0, 0), 'chest': (-8, 14, 0), 'head': (-8, -10, 0)}), (0.62, {'left_arm': (-100, 0, 0), 'left_fore': (-80, 0, 0), 'chest': (14, -10, 0)}), (1.5, {})])
    A['darkness'] = swing('DARKNESS', 1.7, [(0, {}), (0.5, {'right_arm': (-100, 0, -50), 'left_arm': (-100, 0, 50), 'chest': (-12, 0, 0), 'head': (-22, 0, 0)}), (0.8, {'right_arm': (-70, 0, -20), 'left_arm': (-70, 0, 20), 'chest': (14, 0, 0), 'head': (10, 0, 0)}), (1.7, {})])
    A['summon'] = swing('SUMMON', 2.2, [(0, {}), (0.5, {'right_arm': (-80, 0, -10), 'right_fore': (-20, 0, 0), 'left_arm': (-110, 0, 30), 'chest': (-10, 0, 0), 'head': (-20, 0, 0)}), (1.1, {'right_arm': (-130, 0, -10), 'left_arm': (-130, 0, 20), 'chest': (-14, 0, 0), 'head': (-26, 0, 0)}), (2.2, {})])
    A['fade'] = swing('FADE', 0.7, [(0, {}), (0.25, {'chest': (10, 0, 0)}), (0.7, {})])
    scales(A['fade'], [(0, {'base': (1, 1, 1)}), (0.25, {'base': (0.2, 1.1, 0.2)}), (0.7, {'base': (1, 1, 1)})])
    return M, Pt, A
