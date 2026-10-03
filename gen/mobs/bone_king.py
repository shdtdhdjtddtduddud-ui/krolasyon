"""Kemik Kral Vorthak (Bone King) - towering crowned skeleton with a spectral greatsword and a tattered royal cloak."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'bone_king'
SPEC = dict(
    faction='BONE', tr='Kemik Kral Vorthak', en='Vorthak, the Bone King', hp=520, armor=9, attack=16.0, speed=0.26, size=(1.5, 4.2, 1.0), reach=4.6, kb=0.95, follow=56,
    voice=('warden', 0.62), egg=(0xE8E0C8, 0x30B8E8), spawn=(0, 1, 1, []), xp=500, death_ticks=70, anim=(1.15, 1.3), boss=('WHITE', True),
    abilities=[
        dict(kind='MELEE', anim='attack', time=(36, 14, 26), cd=28, dmg=0.95, p=(1.2, 0), color=0x9AE8FF, fx='soul'),
        dict(kind='SLAM', anim='plunge', time=(44, 26), cd=150, w=3, range=(0, 9), dmg=1.3, p=(7.5, 2.0), color=0x9AE8FF, fx='soul', eff=('minecraft:slowness', 60, 1)),
        dict(kind='ERUPT', anim='spikes', time=(40, 20, 26, 32), cd=210, w=3, range=(2, 20), dmg=0.9, p=(3, 7), color=0xE8E0C8, fx='ash'),
        dict(kind='SUMMON', anim='raise', time=(52, 28), cd=460, w=2, range=(0, 30), p=(2, 4), summon='bone_knight', color=0x7AE0FF, fx='soul'),
        dict(kind='SUMMON', anim='raise', time=(52, 28), cd=500, w=2, range=(0, 30), p=(2, 4), summon='bone_hound', color=0x7AE0FF, fx='soul'),
        dict(kind='VOLLEY', anim='spears', time=(40, 22), cd=170, w=3, range=(4, 26), dmg=0.7, p=(7, 0.8), color=0xBFEFFF, fx='soul', los=True),
        dict(kind='SHIELD', anim='ward', time=(70, 4), cd=300, w=2, range=(0, 12), p=(0.85, 0.5), color=0x9AE8FF, fx='soul'),
        dict(kind='BEAM', anim='soulbeam', time=(70, 24, 40), cd=300, w=3, range=(5, 30), dmg=0.7, p=(40, 0), color=0xBFEFFF, fx='soul', los=True, p2=True),
        dict(kind='BURST', anim='scream', time=(46, 28), cd=260, w=3, range=(0, 10), dmg=1.0, p=(10, 1.2), color=0xBFEFFF, fx='soul', eff=('minecraft:wither', 100, 1), p2=True),
    ],
    drops=[('krolasyonbosses:bone_crown', 1, 1, 1.0), ('krolasyonbosses:bone_sigil', 1, 1, 1.0), ('krolasyonbosses:bone_dust', 8, 16, 1.0), ('krolasyonbosses:hellsteel_ingot', 2, 5, 1.0), ('minecraft:experience_bottle', 4, 8, 1.0)],
    lore='Kemik Krallığı\'nın hiç ölmeyen hükümdarı. Kanunları taş gibi sert, ordusu ölüm kadar sadıktır; tahtı yalnızca hak edene devreder.',
)

IVORY = pal('#4b4436', '#7d735c', '#b5a888', '#d9cfae', '#f1e9cf')
OLD = pal('#2a2418', '#4a402c', '#7a6c4c', '#a89870', '#cfc09a')
GOLD = pal('#5a4010', '#8f6a18', '#d8a828', '#ffe070')
SOUL = (110, 230, 255)


def build():
    M = Model(ID, 256, 256)
    K = humanoid(M, leg=(14, 14), leg_w=5.0, foot=(6.5, 3.2, 9.4), hip_w=11, pelvis_h=5.5, torso=(12.5, 15, 7.4), neck=2.4, head=(9.6, 10, 9.6),
                 arm=(14.5, 13.5), arm_w=4.2, hand=(4.2, 5, 4.2), hunch=4, arm_out=1.0, bend=(0.7, 0.2), head_fwd=1.0,
                 mats=dict(pelvis='bone', torso='ribs', head='skull', arm='bone2', fore='bone', hand='bone', leg='bone2', shin='bone', foot='bone'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    # royal armour pieces: gorget, pauldrons, belt, greaves
    box(M, 'chest', (0, w[1] - 15.4, -0.6), (13.4, 2.4, 9.0), 'gold')
    box(M, 'hips', (0, hy - 1.0, 0), (11.8, 2.4, 7.8), 'gold')
    box(M, 'chest', (0, w[1] - 9.5, -4.6), (6.0, 6.0, 1.0), 'soul')
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        box(M, f'{s}_arm', (A[0] + sx * 2.0, A[1] - 1.0, A[2]), (9.6, 4.6, 9.8), 'plate')
        for k in range(3):
            zero_bone(M, f'{s}_ps{k}', f'{s}_arm', (A[0] + sx * (2.0 + k * 1.8), A[1] - 3.0, A[2] + (k - 1) * 2.6))
            M.cube_l(f'{s}_ps{k}', (-1.0, -6.6 - k, -1.0), (2.0, 6.6 + k, 2.0), 'bone')
        kn = K[f'{s}_knee']
        box(M, f'{s}_leg', (kn[0], kn[1], kn[2] - 2.8), (6.0, 4.6, 3.6), 'gold')
        box(M, f'{s}_shin', (kn[0], kn[1] + 7.8, kn[2] - 1.4), (6.2, 8.4, 5.8), 'plate')
    # crown with soul-lit gems
    for k in range(7):
        a = (k - 3) * 0.5
        zero_bone(M, f'cr{k}', 'head', (math.sin(a) * 4.6, hc[1] - 5.0, hc[2] - math.cos(a) * 1.0 + 0.6))
        h = 8.0 - abs(k - 3) * 1.1
        M.cube_l(f'cr{k}', (-0.9, -h, -0.9), (1.8, h, 1.8), 'gold')
        M.cube_l(f'cr{k}', (-0.6, -h - 1.6, -0.6), (1.2, 1.6, 1.2), 'soul')
    box(M, 'head', (hc[0], hc[1] - 4.6, hc[2]), (10.6, 1.8, 10.6), 'gold')
    # great horns
    for sx in (-1, 1):
        chain(M, f'horn{sx}', 'head', [(sx * 4.8, hc[1] - 3.0, hc[2]), (sx * 9.0, hc[1] - 5.0, hc[2] - 0.5), (sx * 11.5, hc[1] - 12.0, hc[2] - 1.0)], (3.6, 2.4), 'bone', ext=0.2)
    # spectral greatsword (right hand), pointing forward
    sword(M, 'right_hand', length=40, width=6.0, thick=1.8, mat='spectral', guard=(12.0, 1.8, 2.6), guard_mat='gold', grip=5.4, grip_mat='grip', dir='fwd', offset=(0, 0, 0.8))
    box(M, 'right_hand', (0, 1.0, -12.0), (2.0, 8.0, 1.0), 'soul')
    # tattered royal cloak and a back banner of soul flames
    for k, (x, hgt) in enumerate(((-4.6, 36), (0, 42), (4.6, 36), (-8.4, 26), (8.4, 26))):
        zero_bone(M, f'cloak{k}', 'chest', (x, w[1] - 14.4 + abs(x) * 0.2, 5.0))
        M.cube_l(f'cloak{k}', (-2.1, 0, -0.5), (4.2, hgt, 1.0), 'cloak')
    for k, (ry, hgt) in enumerate(((0, 20), (40, 16), (-40, 16))):
        world_bone(M, f'sf{k}', 'chest', (0, w[1] - 17, 4.0), -12, ry, 0)
        M.cube_l(f'sf{k}', (-3.0, -hgt, 0), (6.0, hgt, 0), 'soulflame', plane=True)
    # spinal spikes, ribs and extra skulls on the knees
    zero_bone(M, 'spine', 'chest', (0, w[1] - 14.8, 5.0))
    for k in range(5):
        local(M, f'sp{k}', 'spine', (0, k * 3.0, 0.6 * k), (-26 - k * 5, 0, 0))
        M.cube_l(f'sp{k}', (-1.5, -9.0 + k, -1.5), (3, 9 - k, 3), 'bone')

    Pt = Painter(M)
    Pt.mats['bone'] = mats.bone(IVORY, 0.6)
    Pt.mats['bone2'] = mats.bone(OLD, 0.7)
    Pt.mats['skull'] = mats.face_on(mats.bone(IVORY, 0.4), 'north', eyes=[(0.22, 0.38, 0.14, 0.16, 0.0, 'round')], nose=dict(x=0.5, y=0.6, w=0.07, h=0.1, col=(24, 20, 16)),
                                    mouth=dict(y=0.87, w=0.34, h=0.08, teeth=8, col=(20, 16, 12)), eye_col=(180, 250, 255), glow_col=SOUL)
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
    Pt.mats['gold'] = mats.metal(GOLD, glow_edge=(255, 220, 120))
    Pt.mats['plate'] = mats.plates(pal('#0e1014', '#1e2128', '#363b46', '#565d6a', '#8a93a2'), rivets=True, band=6, glow_seam=SOUL)
    Pt.mats['soul'] = mats.flame((230, 252, 255), (100, 220, 255), (30, 120, 210), 0.3)
    Pt.mats['soulflame'] = mats.flame((230, 252, 255), (100, 220, 255), (30, 120, 210), 0.9)
    Pt.mats['spectral'] = mats.metal(pal('#1a3a4a', '#3a7a9a', '#80c8e8', '#c8f0ff', '#ffffff'), brushed=False, glow_edge=(150, 240, 255))
    Pt.mats['grip'] = mats.cloth(pal('#0a0806', '#1c1410', '#2e2018'), 0.3)
    Pt.mats['cloak'] = mats.ragged(mats.cloth(pal('#07060a', '#110d18', '#1c1428', '#2a1e3c', '#3c2c5a'), 1.0, hem=(201, 162, 39), trim=(201, 162, 39)), depth=12)

    ba = dict(stride=22, shin=30, arm=10, elbow=8, bob=1.0, twist=7, lean=4, walk_len=1.2, run_len=0.85, run_mul=1.4, head_bob=3)
    A = biped_set(M, ba, death='collapse', death_len=70 / 20)
    for nm in ('idle', 'walk', 'run'):
        for k in range(5):
            gen(A[nm], f'cloak{k}', 'r', lambda ph, k=k: (4 * S(ph, 1, k * 0.17), 0, 3 * S(ph, 1, k * 0.2 + 0.3)), n=8)
        for k in range(3):
            gen(A[nm], f'sf{k}', 'r', lambda ph, k=k: (6 * S(ph, 1, k * 0.2), 0, 8 * S(ph, 1, k * 0.13)), n=8)
    A['attack'] = swing('ATTACK', 1.8, [(0, {}), (0.5, {'right_arm': (-100, 0, -34), 'right_fore': (-30, 0, 0), 'chest': (-10, -36, 0), 'head': (-6, 14, 0), 'left_arm': (-30, 0, 26)}),
                                       (0.7, {'right_arm': (-92, 0, 22), 'right_fore': (-12, 0, 0), 'chest': (8, 38, 0), 'head': (4, -16, 0)}),
                                       (1.0, {'right_arm': (-165, 0, -10), 'right_fore': (-30, 0, 0), 'chest': (-18, 0, 0), 'head': (-14, 0, 0)}),
                                       (1.2, {'right_arm': (-30, 0, 4), 'right_fore': (-8, 0, 0), 'chest': (36, 0, 0), 'head': (14, 0, 0), 'right_leg': (-24, 0, 0), 'left_leg': (18, 0, 0)}), (1.8, {})])
    A['plunge'] = swing('PLUNGE', 2.3, [(0, {}), (0.9, {'right_arm': (-178, 0, -10), 'left_arm': (-170, 0, 12), 'right_fore': (-20, 0, 0), 'chest': (-26, 0, 0), 'head': (-26, 0, 0)}),
                                       (1.2, {'right_arm': (-40, 0, -4), 'left_arm': (-40, 0, 4), 'chest': (46, 0, 0), 'head': (24, 0, 0), 'right_leg': (-28, 0, 0), 'left_leg': (-28, 0, 0), 'right_shin': (24, 0, 0), 'left_shin': (24, 0, 0)}),
                                       (1.8, {'right_arm': (-40, 0, -4), 'left_arm': (-40, 0, 4), 'chest': (40, 0, 0), 'head': (20, 0, 0)}), (2.3, {})])
    moves(A['plunge'], [(0, {}), (1.2, {'hips': (0, -3.5, 0)}), (1.8, {'hips': (0, -3.0, 0)}), (2.3, {})])
    A['spikes'] = swing('SPIKES', 2.0, [(0, {}), (0.7, {'right_arm': (-80, 0, -20), 'left_arm': (-80, 0, 20), 'chest': (-14, 0, 0), 'head': (-20, 0, 0)}), (1.0, {'right_arm': (-20, 0, -10), 'left_arm': (-20, 0, 10), 'chest': (24, 0, 0), 'head': (14, 0, 0)}), (1.7, {'chest': (22, 0, 0), 'head': (12, 0, 0)}), (2.0, {})])
    A['raise'] = swing('RAISE', 2.6, [(0, {}), (0.7, {'right_arm': (-130, 0, -40), 'left_arm': (-130, 0, 40), 'chest': (-18, 0, 0), 'head': (-30, 0, 0)}), (1.8, {'right_arm': (-140, 0, -46), 'left_arm': (-140, 0, 46), 'chest': (-20, 0, 0), 'head': (-34, 0, 0)}), (2.6, {})])
    A['spears'] = swing('SPEARS', 2.0, [(0, {}), (0.6, {'right_arm': (-130, 0, -10), 'left_arm': (-120, 0, 20), 'chest': (-14, 14, 0), 'head': (-14, 0, 0)}), (0.9, {'right_arm': (-60, 0, 0), 'left_arm': (-50, 0, 10), 'chest': (20, -14, 0)}), (2.0, {})])
    A['ward'] = swing('WARD', 3.5, [(0, {}), (0.5, {'right_arm': (-100, 0, -50), 'left_arm': (-100, 0, 50), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (8, 0, 0), 'head': (8, 0, 0)}), (3.0, {'right_arm': (-104, 0, -54), 'left_arm': (-104, 0, 54), 'chest': (10, 0, 0)}), (3.5, {})])
    A['soulbeam'] = swing('SOULBEAM', 3.5, [(0, {}), (0.8, {'right_arm': (-90, 0, 0), 'right_fore': (-4, 0, 0), 'chest': (6, 0, 0), 'left_arm': (-60, 0, 40)}), (3.0, {'right_arm': (-92, 0, 0), 'chest': (8, 0, 0)}), (3.5, {})])
    A['scream'] = swing('SCREAM', 2.3, [(0, {}), (0.7, {'chest': (-26, 0, 0), 'head': (-42, 0, 0), 'right_arm': (-40, 0, -70), 'left_arm': (-40, 0, 70)}), (1.5, {'chest': (-28, 0, 0), 'head': (-46, 0, 0), 'right_arm': (-44, 0, -76), 'left_arm': (-44, 0, 76)}), (2.3, {})])
    scales(A['scream'], [(0, {'head': (1, 1, 1)}), (0.7, {'head': (1.2, 1.2, 1.2)}), (1.5, {'head': (1.25, 1.25, 1.25)}), (2.3, {'head': (1, 1, 1)})])
    return M, Pt, A
