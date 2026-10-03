"""Kemik Av Köpeği (Bone Hound) - skeletal war hound with a soul flame burning in its ribcage."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'bone_hound'
SPEC = dict(
    faction='BONE', tr='Kemik Av Köpeği', en='Bone Hound', hp=30, armor=2, attack=5.0, speed=0.34, size=(0.9, 1.25, 1.0), reach=2.2, kb=0.1, follow=40,
    voice=('hound', 1.25), egg=(0xD9CFAE, 0x4AD8FF), spawn=(11, 3, 5, ['bone_marches', 'ossuary_fields']), xp=8, death_ticks=22, anim=(2.4, 2.8), head='head',
    abilities=[
        dict(kind='MELEE', anim='bite', time=(14, 7), cd=16, dmg=1.0, p=(0.35, 0), color=0xBFEFFF, fx='soul', eff=('minecraft:wither', 40, 0)),
        dict(kind='LEAP', anim='pounce', time=(26, 9), cd=90, w=5, range=(3, 11), dmg=1.2, p=(1.2, 0.6), color=0xBFEFFF, fx='soul'),
        dict(kind='BURST', anim='howl', time=(36, 20), cd=240, w=2, range=(0, 9), dmg=0.3, p=(7, 0.2), color=0x7AE0FF, fx='soul', eff=('minecraft:slowness', 80, 1)),
        dict(kind='SUMMON', anim='howl', time=(36, 22), cd=420, w=1, range=(0, 20), p=(1, 4), summon='bone_hound', color=0x7AE0FF, fx='soul'),
    ],
    drops=[('minecraft:bone', 1, 3, 0.9), ('krolasyonbosses:bone_dust', 0, 2, 0.4)],
    lore='Kemik Krallığı\'nın avcı sürüleri. Göğüs kafesinde yanan ruh alevi sahibinin sesini taşır; uluması ordunun hareket emridir.',
)

IVORY = pal('#4b4436', '#7d735c', '#b5a888', '#d9cfae', '#f1e9cf')
SOUL = (110, 230, 255)


def build():
    M = Model(ID, 128, 128)
    K = quadruped(M, body=(8.5, 9, 18), leg=(5.0, 5.4), leg_w=3.0, foot=(3.4, 1.8, 5.0), neck_len=3.6, neck_rise=1.8, neck_w=5.0,
                  head=(6.5, 6.2, 6.0), snout=(4.0, 3.2, 6.2), jaw=(3.6, 1.4, 6.0), tail_segs=4, tail_len=3.6, tail_w=1.8,
                  mats=dict(body='ribs', neck='bone', head='skull', snout='skull', jaw='bone', leg='bone', shin='bone', foot='bone', tail='bone'), rise=0.0)
    bc = K['body_c']
    # spine ridge: row of small vertebra spikes
    for i in range(7):
        z = bc[2] - 8 + i * 2.6
        zero_bone(M, f'vert{i}', 'body', (0, bc[1] - 4.0, z))
        M.cube_l(f'vert{i}', (-0.9, -3.0 - (i % 2) * 0.8, -0.9), (1.8, 3.0 + (i % 2) * 0.8, 1.8), 'bone')
    # glowing soul heart inside the ribs
    box(M, 'body', (bc[0], bc[1] + 0.5, bc[2] - 2), (4.6, 4.0, 5.0), 'soul')
    # armoured collar with spikes
    nb = K['neck_base']
    box(M, 'neck', (0, nb[1] - 1.8, nb[2] - 0.5), (7.4, 2.0, 5.4), 'iron')
    for sx in (-1, 0, 1):
        zero_bone(M, f'coll{sx}', 'neck', (sx * 2.8, nb[1] - 3.0, nb[2] - 0.5))
        M.cube_l(f'coll{sx}', (-0.7, -3.0, -0.7), (1.4, 3.0, 1.4), 'iron')
    # ears (pointed bone shards)
    hc = K['head_c']
    for sx in (-1, 1):
        zero_bone(M, f'ear{sx}', 'head', (sx * 2.2, hc[1] - 3.0, hc[2] + 1.2))
        M.cube_l(f'ear{sx}', (-0.7, -3.4, -0.7), (1.4, 3.4, 1.4), 'bone')
    # soul flame at the tail tip
    tp = K['tail_pts'][-1]
    zero_bone(M, 'tflame', K['tail_names'][-1], tp)
    for k, ry in enumerate((0, 90)):
        local(M, f'tfl{k}', 'tflame', (0, 0, 0), (0, ry, 0))
        M.cube_l(f'tfl{k}', (-1.6, -4.2, 0), (3.2, 4.2, 0), 'flame', plane=True)

    Pt = Painter(M)
    Pt.mats['bone'] = mats.bone(IVORY, 0.5)
    Pt.mats['skull'] = mats.face_on(mats.bone(IVORY, 0.4), 'north', eyes=[(0.27, 0.34, 0.14, 0.12, 0.25, 'almond')], nose=dict(x=0.5, y=0.78, w=0.12, h=0.06, col=(30, 24, 20)),
                                    eye_col=(150, 245, 255), glow_col=SOUL)
    def ribs(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32) + (22, 18, 16)
        g = None
        if face in ('west', 'east'):
            for x in range(1, W - 1, 3):
                col[1:H - 1, x:x + 1] = IVORY[3]
                col[1:H - 1, x + 1:x + 2] = IVORY[1]
            col[:2] = IVORY[2]
        elif face == 'top':
            col = lerp(IVORY[1], IVORY[3], noise2(H, W, r, 2))
        else:
            col = lerp(IVORY[1], IVORY[3], noise2(H, W, r, 2))
        return col, np.ones((H, W)), g
    Pt.mats['ribs'] = ribs
    Pt.mats['soul'] = mats.flame((220, 250, 255), (90, 210, 255), (30, 110, 200), 0.4)
    Pt.mats['flame'] = mats.flame((230, 250, 255), (100, 220, 255), (30, 120, 210), 0.8)
    Pt.mats['iron'] = mats.metal(pal('#0e1014', '#1e2128', '#363b46', '#565d6a'))

    A = quad_set(M, dict(body='body', head='head', jaw='jaw', tail=K['tail_names'], ears=['ear-1', 'ear1'], stride=34, shin=34, walk_len=0.8, run_len=0.45, run_mul=1.6), death='side', death_len=22 / 20)
    A['bite'] = swing('BITE', 0.75, [(0, {}), (0.18, {'head': (-22, 0, 0), 'jaw': (38, 0, 0), 'body': (-6, 0, 0), 'neck': (-8, 0, 0)}), (0.3, {'head': (16, 0, 0), 'jaw': (-6, 0, 0), 'body': (10, 0, 0), 'neck': (14, 0, 0)}), (0.75, {})])
    moves(A['bite'], [(0, {}), (0.3, {'body': (0, 0, -3.0)}), (0.75, {})])
    A['pounce'] = swing('POUNCE', 1.3, [(0, {}), (0.4, {'body': (-14, 0, 0), 'fl_leg': (30, 0, 0), 'fr_leg': (30, 0, 0), 'bl_leg': (-40, 0, 0), 'br_leg': (-40, 0, 0), 'head': (6, 0, 0)}),
                                       (0.6, {'body': (14, 0, 0), 'fl_leg': (-60, 0, 0), 'fr_leg': (-60, 0, 0), 'bl_leg': (50, 0, 0), 'br_leg': (50, 0, 0), 'jaw': (40, 0, 0), 'head': (-14, 0, 0)}),
                                       (0.9, {'body': (4, 0, 0), 'fl_leg': (-30, 0, 0), 'fr_leg': (-30, 0, 0), 'bl_leg': (20, 0, 0), 'br_leg': (20, 0, 0)}), (1.3, {})])
    A['howl'] = swing('HOWL', 1.8, [(0, {}), (0.4, {'body': (-24, 0, 0), 'neck': (-26, 0, 0), 'head': (-34, 0, 0), 'jaw': (44, 0, 0), 'fl_leg': (10, 0, 0), 'fr_leg': (10, 0, 0)}),
                                   (1.4, {'body': (-24, 0, 0), 'neck': (-28, 0, 0), 'head': (-36, 0, 0), 'jaw': (48, 0, 0), 'fl_leg': (10, 0, 0), 'fr_leg': (10, 0, 0)}), (1.8, {})])
    moves(A['howl'], [(0, {}), (0.4, {'body': (0, 1.0, 0)}), (1.4, {'body': (0, 1.0, 0)}), (1.8, {})])
    return M, Pt, A
