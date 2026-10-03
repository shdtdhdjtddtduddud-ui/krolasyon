"""Kemik Şövalye (Bone Knight) - armoured skeletal footsoldier of the Bone Kingdom: sword, kite shield, soul-flame eyes."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'bone_knight'
SPEC = dict(
    faction='BONE', tr='Kemik Şövalye', en='Bone Knight', hp=46, armor=6, attack=6.0, speed=0.25, size=(0.7, 2.05, 1.0), reach=2.6, kb=0.3,
    voice=('warden', 1.35), egg=(0xE6DCC0, 0x3AC8E8), spawn=(9, 2, 3, ['bone_marches', 'ossuary_fields']), xp=12, death_ticks=26, anim=(1.5, 1.7),
    abilities=[
        dict(kind='MELEE', anim='attack', time=(22, 12), cd=20, dmg=1.0, p=(0.8, 0), color=0x9AE8FF, fx='soul'),
        dict(kind='CHARGE', anim='bash', time=(26, 8), cd=130, w=3, range=(3, 11), dmg=1.2, p=(10, 0.95), color=0xE8E0C8, fx='ash', eff=('minecraft:slowness', 50, 1)),
        dict(kind='SHIELD', anim='guard', time=(50, 4), cd=200, w=2, range=(0, 9), p=(0.85, 0.5), color=0x9AE8FF, fx='soul'),
        dict(kind='BUFF', anim='rally', time=(30, 12), cd=420, w=1, range=(0, 14), p=(10, 4), color=0x9AE8FF, fx='soul', eff=('minecraft:strength', 200, 0)),
    ],
    drops=[('minecraft:bone', 1, 3, 0.9), ('krolasyonbosses:bone_dust', 0, 2, 0.5), ('minecraft:iron_nugget', 0, 3, 0.4)],
    lore='Kemik Krallığı\'nın değişmez ordusu. Yemin ettikleri kralın emri dışında hiçbir şey duymazlar; kalkanları kırılmadan asla geri çekilmezler.',
)

IVORY = pal('#4b4436', '#7d735c', '#b5a888', '#d9cfae', '#f1e9cf')
STEEL = pal('#16181d', '#2b2e36', '#454a56', '#6b7280', '#9aa3b2')
GOLD = pal('#5a4010', '#8f6a18', '#c9a227', '#f0d060')
SOUL = (110, 230, 255)


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(10.5, 10.5), leg_w=3.6, foot=(5, 2.4, 7.5), hip_w=9, pelvis_h=4.2, torso=(9.5, 11.5, 6), neck=2.0, head=(8, 8.4, 8),
                 arm=(9.5, 9.5), arm_w=3.0, hand=(2.8, 3.4, 2.8), arm_out=0.8, mats=dict(torso='ribs', pelvis='bone', head='skull', arm='bone', fore='bone', hand='bone', leg='bone', shin='bone', foot='bone'), bend=(0.75, 0.45))
    hy = K['hip_y']
    # plate armour over the skeleton
    box(M, 'chest', (0, K['waist'][1] - 7.5, -3.3), (11.6, 8.2, 2.2), 'plate')
    box(M, 'chest', (0, K['waist'][1] - 7.4, 3.2), (10.4, 8.6, 1.6), 'plate_dark')
    box(M, 'chest', (0, K['waist'][1] - 11.9, -1.8), (9.5, 1.8, 3.6), 'gold')  # gorget
    box(M, 'hips', (0, hy - 0.5, -0.2), (10.4, 2.2, 6.8), 'gold')  # belt
    for sx in (-1, 1):
        s = 'right' if sx < 0 else 'left'
        top = K[f'{s}_A']
        box(M, f'{s}_arm', (top[0] + sx * 1.0, top[1] - 0.6, top[2]), (5.6, 3.2, 6.4), 'plate')
        box(M, f'{s}_arm', (top[0] + sx * 1.6, top[1] + 1.4, top[2]), (4.6, 2.2, 5.6), 'plate_dark')
        B = K[f'{s}_B']
        box(M, f'{s}_fore', ((B[0] + K['hands'][s][0]) / 2, (B[1] + K['hands'][s][1]) / 2, (B[2] + K['hands'][s][2]) / 2), (4.2, 5.0, 4.2), 'plate')
        knee = K[f'{s}_knee']
        box(M, f'{s}_leg', (knee[0], knee[1] - 0.2, knee[2] - 1.8), (4.4, 3.4, 2.6), 'gold')
        box(M, f'{s}_shin', (knee[0] + sx * 0.1, knee[1] + 4.8, knee[2] - 0.6), (4.6, 6.2, 4.6), 'plate')
        # tassets (thigh guards)
        box(M, 'hips', (sx * 3.6, hy + 3.2, -2.6), (4.2, 5.6, 1.4), 'plate_dark')
    # helmet: cranial plate + nasal bar + crest
    hc = K['head_c']
    box(M, 'head', (hc[0], hc[1] - 3.0, hc[2]), (9.2, 3.0, 9.2), 'helm')
    box(M, 'head', (hc[0], hc[1] - 0.2, hc[2] + 3.6), (9.0, 6.4, 2.0), 'helm_dark')
    box(M, 'head', (hc[0], hc[1] - 0.8, hc[2] - 4.7), (1.1, 3.6, 0.8), 'gold')
    for sx in (-1, 1):
        box(M, 'head', (sx * 4.55, hc[1] + 0.2, hc[2] + 1.2), (1.2, 5.6, 5.0), 'helm')
    zero_bone(M, 'crest', 'head', (0, hc[1] - 4.6, hc[2] + 0.8))
    M.cube_l('crest', (-0.8, -4.4, -4.0), (1.6, 4.4, 8.4), 'plume')
    zero_bone(M, 'plume1', 'crest', (0, hc[1] - 4.6, hc[2] + 4.8))
    M.cube_l('plume1', (-0.7, -1.0, 0.0), (1.4, 8.0, 2.2), 'plume')
    # tabard
    zero_bone(M, 'tabard_f', 'hips', (0, hy - 0.8, -4.1))
    M.cube_l('tabard_f', (-3.6, 0, -0.5), (7.2, 11.5, 0.9), 'tabard')
    zero_bone(M, 'tabard_b', 'hips', (0, hy - 0.8, 3.3))
    M.cube_l('tabard_b', (-3.4, 0, -0.4), (6.8, 12.5, 0.9), 'tabard_back')
    # sword (right hand) and kite shield (left forearm)
    hr = K['hands']['right']
    sword(M, 'right_hand', length=16, width=2.4, thick=0.9, mat='blade', guard=(6.0, 1.3, 1.8), guard_mat='gold', grip=2.6, grip_mat='grip', dir='fwd', offset=(0, 0, 1.0))
    hl = K['hands']['left']
    M.bone('shield', 'left_fore', (0, 0, 0)); local(M, 'shield', 'left_fore', (0, 0, 0))
    M.bones['shield'].pivot = list(hl)
    # re-create properly: shield bone as world-aligned child at the hand
    del M.bones['shield']; M.order.remove('shield'); M.bones['left_fore'].children.remove('shield')
    zero_bone(M, 'shield', 'left_fore', hl)
    M.cube_l('shield', (1.4, -9.5, -6.2), (1.7, 17.5, 11.5), 'shield')
    M.cube_l('shield', (3.0, -6.0, -3.4), (0.8, 6.0, 6.0), 'shield_boss')

    # ---------------- materials
    Pt = Painter(M)
    Pt.mats['bone'] = mats.bone(IVORY, 0.5)
    Pt.mats['skull'] = mats.face_on(mats.bone(IVORY, 0.4), 'north', eyes=[(0.22, 0.38, 0.14, 0.13, 0.0, 'round')],
                                    nose=dict(x=0.5, y=0.58, w=0.07, h=0.1, col=(34, 28, 22)), mouth=dict(y=0.86, w=0.3, h=0.07, teeth=6, col=(24, 18, 14)),
                                    eye_col=(140, 240, 255), glow_col=SOUL)

    def ribs(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32) + (24, 20, 18)
        g = None
        if face == 'north' or face == 'south':
            for y in range(1, H - 1, 3):
                col[y:y + 1, 1:W - 1] = IVORY[3]
                col[y + 1:y + 2, 1:W - 1] = IVORY[1]
            col[:, W // 2 - 1:W // 2 + 1] = IVORY[2]
            g = np.zeros((H, W, 4), np.float32)
            g[H // 2 - 1:H // 2 + 2, W // 2 - 2:W // 2 + 2] = (*SOUL, 200)
        else:
            n = noise2(H, W, r, 2)
            col = lerp(IVORY[1], IVORY[3], n)
        return col, np.ones((H, W)), g
    Pt.mats['ribs'] = ribs
    Pt.mats['plate'] = mats.plates(STEEL, rivets=True, band=5, glow_seam=None)
    Pt.mats['plate_dark'] = mats.plates(pal('#0c0d10', '#181a20', '#262932', '#3a3f4a'), rivets=False, band=6)
    Pt.mats['gold'] = mats.metal(GOLD)
    Pt.mats['helm'] = mats.plates(STEEL, rivets=True, band=6)
    Pt.mats['helm_dark'] = mats.plates(pal('#0c0d10', '#181a20', '#262932', '#3a3f4a'), rivets=False, band=6)
    Pt.mats['plume'] = mats.cloth(pal('#0a0610', '#1a1030', '#2e1c52', '#46308a'), 0.8, weave=False)
    Pt.mats['tabard'] = emblem_cloth()
    Pt.mats['tabard_back'] = mats.cloth(pal('#07060a', '#110d18', '#1c1428', '#2a1e3c'), 0.9, hem=(201, 162, 39))
    Pt.mats['blade'] = mats.metal(pal('#2e3a44', '#5a6a78', '#9fb0be', '#d8e6f0', '#ffffff'), glow_edge=SOUL)
    Pt.mats['guard'] = mats.metal(GOLD)
    Pt.mats['grip'] = mats.cloth(pal('#0a0806', '#1c1410', '#2e2018'), 0.3)
    Pt.mats['shield'] = shield_mat()
    Pt.mats['shield_boss'] = mats.metal(GOLD)

    # ---------------- animations
    ba = dict(stride=28, shin=38, arm=14, elbow=10, lean=1, twist=6, tail=None)
    A = biped_set(M, ba, death='collapse', death_len=26 / 20)
    # arms hold weapons: damp the swing of both arms in locomotion
    A['attack'] = swing('ATTACK', 1.1, [
        (0.00, {}),
        (0.30, {'right_arm': (-150, 0, -8), 'right_fore': (-70, 0, 0), 'chest': (-8, -14, 0), 'head': (-8, 0, 0), 'left_arm': (-20, 0, 12), 'right_leg': (10, 0, 0), 'left_leg': (-14, 0, 0)}),
        (0.48, {'right_arm': (-40, 0, 0), 'right_fore': (-18, 0, 0), 'chest': (22, 18, 0), 'head': (10, 0, 0), 'left_arm': (-30, 0, 10), 'right_leg': (-16, 0, 0), 'left_leg': (12, 0, 0)}),
        (0.66, {'right_arm': (-26, 0, 0), 'right_fore': (-16, 0, 0), 'chest': (16, 12, 0), 'head': (6, 0, 0), 'right_leg': (-16, 0, 0), 'left_leg': (12, 0, 0)}),
        (1.10, {})])
    A['bash'] = swing('BASH', 1.3, [
        (0.00, {}),
        (0.40, {'left_arm': (-50, 0, 20), 'left_fore': (-70, 0, 0), 'chest': (-10, 22, 0), 'right_arm': (-20, 0, -10), 'right_leg': (14, 0, 0), 'left_leg': (-20, 0, 0)}),
        (0.55, {'left_arm': (-90, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (28, -10, 0), 'head': (12, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (26, 0, 0)}),
        (1.00, {'left_arm': (-80, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (26, -6, 0), 'head': (10, 0, 0), 'right_leg': (-24, 0, 0), 'left_leg': (20, 0, 0)}),
        (1.30, {})])
    A['guard'] = swing('GUARD', 2.5, [
        (0.00, {}),
        (0.30, {'left_arm': (-96, 0, 10), 'left_fore': (-72, 0, 0), 'chest': (6, 8, 0), 'right_arm': (-30, 0, -14), 'right_fore': (-60, 0, 0), 'head': (6, 0, 0)}),
        (1.20, {'left_arm': (-96, 0, 10), 'left_fore': (-74, 0, 0), 'chest': (7, 8, 0), 'right_arm': (-34, 0, -14), 'right_fore': (-62, 0, 0), 'head': (6, 0, 0)}),
        (2.50, {})])
    A['rally'] = swing('RALLY', 1.5, [
        (0.00, {}),
        (0.45, {'right_arm': (-170, 0, -22), 'right_fore': (-20, 0, 0), 'chest': (-14, 0, 0), 'head': (-24, 0, 0), 'left_arm': (-24, 0, 14)}),
        (0.62, {'right_arm': (-175, 0, -26), 'right_fore': (-14, 0, 0), 'chest': (-18, 0, 0), 'head': (-30, 0, 0)}),
        (1.00, {'right_arm': (-160, 0, -22), 'chest': (-12, 0, 0), 'head': (-20, 0, 0)}),
        (1.50, {})])
    return M, Pt, A


def emblem_cloth():
    base = mats.cloth(pal('#07060a', '#110d18', '#1c1428', '#2a1e3c'), 0.9, hem=(201, 162, 39), trim=(201, 162, 39))
    bm = ['..#####..',
          '.#######.',
          '#.#.#.#.#',
          '###...###',
          '.#######.',
          '..#.#.#..',
          '.#.....#.']

    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        col = col.copy()
        if face == 'north' and W >= 9:
            stamp(col, bm, W // 2 - 4, H // 3, {'#': (232, 214, 150)})
        return col, a, g
    return f


def shield_mat():
    base = mats.plates(pal('#0e0f13', '#1e2128', '#33373f', '#4c515c'), rivets=True, band=7)
    bm = ['..#####..',
          '.#######.',
          '##.###.##',
          '###...###',
          '.#######.',
          '..#.#.#..',
          '..#.#.#..']

    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        col = col.copy()
        # gold rim
        if face in ('east', 'west') and W > 6:
            col[:2] = (201, 162, 39); col[-2:] = (201, 162, 39)
            col[:, :2] = (201, 162, 39); col[:, -2:] = (201, 162, 39)
            stamp(col, bm, max(1, W // 2 - 4), max(1, H // 2 - 4), {'#': (236, 226, 200)})
        return col, a, g
    return f
