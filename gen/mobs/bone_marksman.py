"""Kemik Nişancı (Bone Marksman) - hooded skeletal sniper with a long bone bow that fires soul arrows."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'bone_marksman'
SPEC = dict(
    faction='BONE', tr='Kemik Nişancı', en='Bone Marksman', hp=30, armor=1, attack=5.5, speed=0.25, size=(0.65, 2.0, 1.0), reach=14, kb=0.1, follow=44,
    voice=('warden', 1.5), egg=(0xB5A888, 0x3A2A5A), spawn=(7, 1, 2, ['bone_marches', 'ossuary_fields']), xp=12, death_ticks=22, anim=(1.5, 1.8),
    abilities=[
        dict(kind='BOLT', anim='shoot', time=(26, 14), cd=26, dmg=1.0, range=(5, 24), p=(1.8, 0), color=0x9AE8FF, fx='soul', los=True),
        dict(kind='SNIPE', anim='snipe', time=(46, 36), cd=170, w=4, range=(8, 34), dmg=2.6, p=(0, 0), color=0xCFF6FF, fx='soul', los=True),
        dict(kind='RETREAT', anim='leap', time=(20, 6), cd=110, w=5, range=(0, 5), p=(1.0, 0.5), color=0xBFEFFF, fx='soul'),
        dict(kind='VOLLEY', anim='volley', time=(34, 20), cd=170, w=3, range=(5, 22), dmg=0.8, p=(5, 0.4), color=0x9AE8FF, fx='soul', los=True),
    ],
    drops=[('minecraft:bone', 1, 2, 0.8), ('minecraft:arrow', 1, 5, 0.8), ('krolasyonbosses:bone_dust', 0, 1, 0.4)],
    lore='Kemik Krallığı\'nın görünmez nişancıları. Kapüşonlarının altında yalnızca iki ruh alevi parlar; ok, hedefe varmadan önce karar verilmiştir.',
)

IVORY = pal('#4b4436', '#7d735c', '#b5a888', '#d9cfae', '#f1e9cf')
SOUL = (110, 230, 255)


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(11, 11), leg_w=3.3, foot=(4.2, 2.2, 6.8), hip_w=8, pelvis_h=4, torso=(8, 11, 5), neck=2.2, head=(7, 7.6, 7),
                 arm=(10, 9.5), arm_w=2.9, hand=(2.6, 3.2, 2.6), hunch=6, arm_out=0.6, bend=(0.2, 0.9), head_fwd=1.5,
                 mats=dict(pelvis='bone', torso='ribs', head='skull', arm='bone', fore='bone', hand='bone', leg='bone', shin='bone', foot='bone'))
    hy = K['hip_y']; hc = K['head_c']
    # hood + tattered cloak
    box(M, 'head', (hc[0], hc[1] - 3.6, hc[2] + 0.1), (8.8, 2.6, 8.8), 'cloak')
    box(M, 'head', (hc[0], hc[1] + 0.2, hc[2] + 3.6), (8.8, 8.8, 2.0), 'cloak')
    for sx in (-1, 1):
        box(M, 'head', (hc[0] + sx * 4.0, hc[1] - 0.2, hc[2] + 0.4), (1.6, 8.2, 8.4), 'cloak')
    box(M, 'head', (hc[0], hc[1] - 4.3, hc[2] - 2.6), (6.0, 1.0, 3.6), 'cloak')
    zero_bone(M, 'cloak', 'chest', (0, K['waist'][1] - 12.4, 3.4))
    M.cube_l('cloak', (-6.0, 0, -0.5), (12, 21, 1.2), 'cloak_rag')
    zero_bone(M, 'cloakL', 'chest', (-5.5, K['waist'][1] - 12.0, -2.0))
    M.cube_l('cloakL', (-1.0, 0, 0), (1.2, 14, 6.5), 'cloak_rag')
    zero_bone(M, 'cloakR', 'chest', (5.5, K['waist'][1] - 12.0, -2.0))
    M.cube_l('cloakR', (-0.2, 0, 0), (1.2, 14, 6.5), 'cloak_rag')
    # shoulder mantle of bone shards
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        box(M, f'{s}_arm', (A[0] + sx * 1.3, A[1] - 0.6, A[2]), (4.8, 2.4, 5.6), 'bone')
    # belt + quiver of glowing arrows
    box(M, 'hips', (0, hy - 0.5, 0), (8.8, 1.6, 5.8), 'leather')
    zero_bone(M, 'quiver', 'chest', (2.6, K['waist'][1] - 12.4, 3.2))
    M.cube_l('quiver', (-1.8, 0, -1.8), (3.6, 11, 3.6), 'leather')
    for k in range(4):
        M.cube_l('quiver', (-1.2 + k * 0.7, -3.0 - (k % 2), -0.5 + (k % 3) * 0.5), (0.5, 3.6, 0.5), 'arrow')
    # long bone bow (left hand)
    hl = K['hands']['left']
    ptsb = [(hl[0], hl[1] - 15, hl[2] - 1.0), (hl[0], hl[1] - 9, hl[2] - 5.4), (hl[0], hl[1] - 2.0, hl[2] - 7.0), (hl[0], hl[1] + 5, hl[2] - 5.6), (hl[0], hl[1] + 11, hl[2] - 1.2)]
    zero_bone(M, 'bow', 'left_hand', hl)
    for i in range(len(ptsb) - 1):
        M.seg(f'bow{i}', 'bow', ptsb[i], ptsb[i + 1], 1.5, 1.5, 'bone', extend=0.2)
    zero_bone(M, 'string', 'bow', (hl[0], hl[1] - 15, hl[2] - 1.0))
    M.cube_l('string', (-0.25, 0, -0.25), (0.5, 26, 0.5), 'string')

    Pt = Painter(M)
    Pt.mats['bone'] = mats.bone(IVORY, 0.5)
    Pt.mats['skull'] = mats.face_on(mats.bone(IVORY, 0.4), 'north', eyes=[(0.22, 0.4, 0.12, 0.12, 0.0, 'round')], nose=dict(x=0.5, y=0.6, w=0.06, h=0.09, col=(34, 28, 22)),
                                    mouth=dict(y=0.87, w=0.28, h=0.06, teeth=5, col=(24, 18, 14)), eye_col=(160, 250, 255), glow_col=SOUL)
    def ribs(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32) + (24, 20, 18)
        if face in ('north', 'south'):
            for y in range(1, H - 1, 3):
                col[y:y + 1, 1:W - 1] = IVORY[3]; col[y + 1:y + 2, 1:W - 1] = IVORY[1]
            col[:, W // 2 - 1:W // 2 + 1] = IVORY[2]
        else:
            col = lerp(IVORY[1], IVORY[3], noise2(H, W, r, 2))
        return col, np.ones((H, W)), None
    Pt.mats['ribs'] = ribs
    Pt.mats['cloak'] = mats.cloth(pal('#07050c', '#120c20', '#1e1536', '#30224f'), 0.9)
    Pt.mats['cloak_rag'] = mats.ragged(mats.cloth(pal('#07050c', '#120c20', '#1e1536', '#30224f'), 1.0, trim=(60, 40, 100)), depth=6)
    Pt.mats['leather'] = mats.cloth(pal('#0c0806', '#1c130c', '#2e2014', '#483220'), 0.4, weave=False)
    Pt.mats['arrow'] = mats.flame((235, 252, 255), (110, 225, 255), (40, 120, 210))
    Pt.mats['string'] = mats.solid((170, 240, 255))

    ba = dict(stride=30, shin=40, arm=8, elbow=6, lean=2)
    A = biped_set(M, ba, death='fall_back', death_len=22 / 20)
    pull = {'left_arm': (-92, 0, 0), 'left_fore': (-6, 0, 0), 'right_arm': (-95, 25, -4), 'right_fore': (-125, 0, 0), 'chest': (2, -22, 0), 'head': (2, 22, 0)}
    rel = {'left_arm': (-95, 0, 0), 'right_arm': (-70, 25, -10), 'right_fore': (-10, 0, 0), 'chest': (3, -18, 0), 'head': (2, 20, 0)}
    A['shoot'] = swing('SHOOT', 1.3, [(0, {}), (0.25, {'left_arm': (-70, 0, 0), 'right_arm': (-40, 10, -10), 'right_fore': (-80, 0, 0), 'chest': (0, -18, 0), 'head': (0, 18, 0)}),
                                     (0.62, pull), (0.72, rel), (1.0, {'left_arm': (-60, 0, 0), 'chest': (0, -8, 0), 'head': (0, 8, 0)}), (1.3, {})])
    A['snipe'] = swing('SNIPE', 2.3, [(0, {}), (0.4, {'left_arm': (-80, 0, 0), 'right_arm': (-60, 20, -10), 'right_fore': (-90, 0, 0), 'chest': (3, -20, 0), 'head': (0, 20, 0), 'right_leg': (-6, 0, 0), 'left_leg': (8, 0, 0)}),
                                     (1.8, {'left_arm': (-93, 0, 0), 'right_arm': (-97, 25, -4), 'right_fore': (-132, 0, 0), 'chest': (5, -24, 0), 'head': (4, 24, 0), 'right_leg': (-6, 0, 0), 'left_leg': (8, 0, 0)}),
                                     (1.95, {'left_arm': (-96, 0, 0), 'right_arm': (-70, 25, -10), 'right_fore': (-10, 0, 0), 'chest': (4, -18, 0)}), (2.3, {})])
    A['volley'] = swing('VOLLEY', 1.7, [(0, {}), (0.3, {'left_arm': (-120, 0, 0), 'right_arm': (-120, 20, -4), 'right_fore': (-120, 0, 0), 'chest': (-14, -16, 0), 'head': (-14, 16, 0)}),
                                       (1.0, {'left_arm': (-118, 0, 0), 'right_arm': (-118, 20, -4), 'right_fore': (-118, 0, 0), 'chest': (-16, -16, 0), 'head': (-14, 16, 0)}),
                                       (1.1, {'left_arm': (-122, 0, 0), 'right_arm': (-80, 20, -10), 'right_fore': (-10, 0, 0), 'chest': (-16, -16, 0)}), (1.7, {})])
    A['leap'] = swing('LEAP', 1.0, [(0, {}), (0.25, {'chest': (24, 0, 0), 'right_leg': (-40, 0, 0), 'left_leg': (-40, 0, 0), 'right_shin': (70, 0, 0), 'left_shin': (70, 0, 0), 'right_arm': (30, 0, -20), 'left_arm': (30, 0, 20), 'head': (-10, 0, 0)}),
                                   (0.45, {'chest': (-14, 0, 0), 'right_leg': (20, 0, 0), 'left_leg': (20, 0, 0), 'right_shin': (6, 0, 0), 'left_shin': (6, 0, 0), 'right_arm': (-70, 0, -40), 'left_arm': (-70, 0, 40)}), (1.0, {})])
    moves(A['leap'], [(0, {}), (0.25, {'hips': (0, -3.5, 0)}), (0.5, {'hips': (0, 2.0, 0)}), (1.0, {})])
    return M, Pt, A
