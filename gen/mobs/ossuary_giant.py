"""Kemik Dev (Ossuary Giant) - towering patchwork of skeletons bound with chains, swinging a femur club."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'ossuary_giant'
SPEC = dict(
    faction='BONE', tr='Kemik Dev', en='Ossuary Giant', hp=180, armor=6, attack=14.0, speed=0.2, size=(2.0, 4.6, 1.0), reach=4.2, kb=0.85, follow=40,
    voice=('warden', 0.55), egg=(0xD9CFAE, 0x30B8E8), spawn=(2, 1, 1, ['ossuary_fields']), xp=45, death_ticks=40, anim=(1.1, 1.2),
    abilities=[
        dict(kind='MELEE', anim='club', time=(30, 17), cd=28, dmg=1.0, p=(1.3, 0), color=0xDFF6FF, fx='ash'),
        dict(kind='SLAM', anim='slam', time=(40, 24), cd=140, w=4, range=(0, 8), dmg=1.2, p=(6.5, 2.0), color=0xE8E0C8, fx='ash', eff=('minecraft:slowness', 60, 1)),
        dict(kind='VOLLEY', anim='throw', time=(36, 20), cd=170, w=3, range=(5, 22), dmg=0.6, p=(6, 0.6), color=0xE8E0C8, fx='ash', los=True),
        dict(kind='SUMMON', anim='roar', time=(50, 28), cd=400, w=2, range=(0, 24), p=(2, 4), summon='bone_hound', color=0x7AE0FF, fx='soul'),
    ],
    drops=[('minecraft:bone', 4, 9, 1.0), ('krolasyonbosses:bone_dust', 2, 5, 0.9), ('minecraft:bone_block', 1, 3, 0.6)],
    lore='Bir savaşta ölen yüzlerce askerin kemiğinden dikilmiş devasa nöbetçi. Her adımında mezarlar sarsılır.',
)

IVORY = pal('#4b4436', '#7d735c', '#b5a888', '#d9cfae', '#f1e9cf')
OLD = pal('#2a2418', '#4a402c', '#7a6c4c', '#a89870', '#cfc09a')
SOUL = (110, 230, 255)


def build():
    M = Model(ID, 256, 256)
    K = humanoid(M, leg=(11, 11), leg_w=6.4, foot=(8.5, 3.2, 11), hip_w=14, pelvis_h=6, torso=(17, 15, 10), neck=1.5, head=(9, 9, 9),
                 arm=(14, 13), arm_w=6.0, hand=(7, 8, 7), hunch=14, arm_out=1.2, stance=0.5, head_fwd=3, bend=(0.45, 0.1),
                 mats=dict(pelvis='bone', torso='ribs', head='skull', arm='bone2', fore='bone', hand='bone', leg='bone2', shin='bone', foot='bone'))
    hy = K['hip_y']; hc = K['head_c']
    # extra skull plates stitched on the shoulders and knees
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        box(M, f'{s}_arm', (A[0] + sx * 2.0, A[1] - 1.2, A[2]), (9, 6, 9), 'skull_big')
        zero_bone(M, f'{s}_spk', f'{s}_arm', (A[0] + sx * 5, A[1] - 3, A[2]))
        M.cube_l(f'{s}_spk', (-1.4, -8.0, -1.4), (2.8, 8.0, 2.8), 'bone')
        knee = K[f'{s}_knee']
        box(M, f'{s}_leg', (knee[0], knee[1], knee[2] - 3.0), (6.4, 5, 4.0), 'skull_big')
    # spine and rib bones sticking out the back
    zero_bone(M, 'spine', 'chest', (0, K['waist'][1] - 14.5, 5.4))
    for k in range(5):
        local(M, f'sp{k}', 'spine', (0, k * 3.2, 0.4 * k), (-24 - k * 4, 0, 0))
        M.cube_l(f'sp{k}', (-1.4, -9 + k, -1.4), (2.8, 9 - k, 2.8), 'bone')
    # glowing soul in the ribcage
    box(M, 'chest', (0, K['waist'][1] - 8, -2.6), (7, 8, 3.4), 'soul')
    # chains wrapped around the torso
    for k, y in enumerate((6.0, 11.0)):
        box(M, 'chest', (0, K['waist'][1] - y, 0), (18.2, 1.8, 11.2), 'chain')
    # jaw/teeth: oversize skull with horns
    for sx in (-1, 1):
        chain(M, f'horn{sx}', 'head', [(sx * 4.2, hc[1] - 3.5, hc[2]), (sx * 8.4, hc[1] - 5.5, hc[2] - 0.5), (sx * 10.5, hc[1] - 11, hc[2] - 1)], (3.4, 2.2), 'bone', ext=0.2)
    # femur club in the right hand (points forward, head raised)
    sword(M, 'right_hand', length=26, width=5.2, thick=5.2, mat='club', guard=(0.1, 0.1, 0.1), guard_mat='club', grip=3.0, grip_mat='bone', dir='fwd', offset=(0, 0, 0))
    box(M, 'right_hand', (0, 1.0, -34), (8.5, 8.5, 8), 'club_end')
    # tattered banner of the kingdom on the back
    zero_bone(M, 'banner', 'chest', (0, K['waist'][1] - 13.5, 6.2))
    M.cube_l('banner', (-5, 0, 0), (10, 22, 0.8), 'banner')

    Pt = Painter(M)
    Pt.mats['bone'] = mats.bone(IVORY, 0.6)
    Pt.mats['bone2'] = mats.bone(OLD, 0.7)
    Pt.mats['skull'] = mats.face_on(mats.bone(IVORY, 0.4), 'north', eyes=[(0.22, 0.38, 0.14, 0.16, 0.0, 'round')], nose=dict(x=0.5, y=0.6, w=0.07, h=0.1, col=(24, 20, 16)),
                                    mouth=dict(y=0.87, w=0.34, h=0.08, teeth=7, col=(20, 16, 12)), eye_col=(170, 250, 255), glow_col=SOUL)
    Pt.mats['skull_big'] = mats.face_on(mats.bone(OLD, 0.5), 'north', eyes=[(0.22, 0.4, 0.15, 0.16, 0.0, 'round')], nose=dict(x=0.5, y=0.62, w=0.07, h=0.1, col=(24, 20, 16)),
                                        mouth=dict(y=0.85, w=0.3, h=0.08, teeth=5, col=(20, 16, 12)), eye_col=(150, 235, 255), glow_col=SOUL)
    def ribs(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32) + (22, 18, 16)
        if face in ('north', 'south'):
            for y in range(1, H - 1, 3):
                col[y:y + 1, 1:W - 1] = IVORY[3]; col[y + 1:y + 2, 1:W - 1] = IVORY[1]
            col[:, W // 2 - 1:W // 2 + 1] = IVORY[2]
        else:
            col = lerp(IVORY[1], IVORY[3], noise2(H, W, r, 2))
        return col, np.ones((H, W)), None
    Pt.mats['ribs'] = ribs
    Pt.mats['soul'] = mats.flame((220, 250, 255), (90, 210, 255), (30, 110, 200), 0.3)
    Pt.mats['chain'] = mats.metal(pal('#0e1014', '#1e2128', '#363b46', '#565d6a'))
    Pt.mats['club'] = mats.bone(pal('#3a3224', '#6a5c42', '#a89870', '#d8c9a2', '#f4ead0'), 0.5)
    Pt.mats['club_end'] = mats.bone(pal('#3a3224', '#6a5c42', '#a89870', '#d8c9a2', '#f4ead0'), 0.8)
    Pt.mats['banner'] = mats.ragged(mats.cloth(pal('#07060a', '#110d18', '#1c1428', '#2a1e3c'), 0.9, hem=(201, 162, 39), trim=(201, 162, 39)), depth=7)

    ba = dict(stride=22, shin=28, arm=12, elbow=8, bob=1.2, twist=8, lean=6, walk_len=1.2, run_len=0.85, run_mul=1.4, head_bob=4)
    A = biped_set(M, ba, death='collapse', death_len=40 / 20)
    A['club'] = swing('CLUB', 1.5, [(0, {}), (0.45, {'right_arm': (-140, 0, -10), 'right_fore': (-30, 0, 0), 'chest': (-20, -20, 0), 'head': (-12, 0, 0), 'left_arm': (-30, 0, 14)}),
                                   (0.62, {'right_arm': (-20, 0, 6), 'right_fore': (-8, 0, 0), 'chest': (34, 22, 0), 'head': (12, 0, 0)}), (0.95, {'right_arm': (-20, 0, 6), 'chest': (30, 18, 0), 'head': (10, 0, 0)}), (1.5, {})])
    A['slam'] = swing('SLAM', 2.0, [(0, {}), (0.7, {'right_arm': (-175, 0, -14), 'left_arm': (-170, 0, 14), 'right_fore': (-20, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (-28, 0, 0), 'head': (-24, 0, 0)}),
                                   (0.95, {'right_arm': (-30, 0, -4), 'left_arm': (-30, 0, 4), 'chest': (46, 0, 0), 'head': (24, 0, 0), 'right_leg': (-26, 0, 0), 'left_leg': (-26, 0, 0), 'right_shin': (22, 0, 0), 'left_shin': (22, 0, 0)}),
                                   (1.5, {'right_arm': (-30, 0, -4), 'left_arm': (-30, 0, 4), 'chest': (40, 0, 0), 'head': (20, 0, 0)}), (2.0, {})])
    moves(A['slam'], [(0, {}), (0.95, {'hips': (0, -3.0, 0)}), (1.5, {'hips': (0, -2.5, 0)}), (2.0, {})])
    A['throw'] = swing('THROW', 1.8, [(0, {}), (0.5, {'left_arm': (-120, 0, 20), 'left_fore': (-60, 0, 0), 'chest': (-10, 24, 0), 'head': (-10, -10, 0), 'right_arm': (-30, 0, -10)}),
                                     (0.75, {'left_arm': (-40, 0, 6), 'chest': (20, -26, 0), 'head': (8, 10, 0)}), (1.8, {})])
    A['roar'] = swing('ROAR', 2.5, [(0, {}), (0.6, {'chest': (-24, 0, 0), 'head': (-40, 0, 0), 'right_arm': (-60, 0, -50), 'left_arm': (-60, 0, 50)}), (1.8, {'chest': (-26, 0, 0), 'head': (-44, 0, 0), 'right_arm': (-64, 0, -56), 'left_arm': (-64, 0, 56)}), (2.5, {})])
    return M, Pt, A
