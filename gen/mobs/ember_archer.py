"""Kor Okçusu (Ember Archer) - lean charred sharpshooter with a flame crest and a horn bow."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'ember_archer'
SPEC = dict(
    faction='EMBER', tr='Kor Okçusu', en='Ember Archer', hp=34, armor=2, attack=5.0, speed=0.27, size=(0.65, 2.0, 1.0), reach=13, kb=0.1, follow=40,
    voice=('demon', 1.15), egg=(0x3A2018, 0xFFB030), spawn=(8, 2, 3, ['ember_wastes', 'ashen_barrens']), xp=12, death_ticks=22, anim=(1.6, 1.8),
    abilities=[
        dict(kind='BOLT', anim='shoot', time=(26, 14), cd=26, dmg=1.0, range=(5, 22), p=(1.7, 0), color=0xFF8A22, fx='flame', los=True),
        dict(kind='VOLLEY', anim='volley', time=(34, 20), cd=150, w=4, range=(5, 20), dmg=0.8, p=(5, 0.45), color=0xFF6A10, fx='flame', los=True),
        dict(kind='RETREAT', anim='leap', time=(20, 6), cd=110, w=5, range=(0, 5), p=(1.0, 0.5), color=0xFFA040, fx='flame'),
        dict(kind='METEOR', anim='skyshot', time=(40, 14, 18, 22, 26), cd=300, w=2, range=(8, 24), dmg=1.1, p=(2.2, 6), color=0xFF5A10, fx='flame'),
    ],
    drops=[('minecraft:arrow', 1, 4, 0.7), ('krolasyonbosses:ember_shard', 0, 1, 0.3), ('minecraft:blaze_rod', 0, 1, 0.25)],
    lore='Kor Hanedanı\'nın gözcüleri. Ok uçları kor halindedir; hedef yandığında bile atış durmaz.',
)

CHAR = pal('#1c1512', '#33241d', '#50382b', '#74523e', '#9a6f52')
LEATH = pal('#1c110a', '#33200f', '#523418', '#7a4e24')
EM = (255, 120, 24)


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(11, 11), leg_w=3.7, foot=(4.4, 2.4, 7), hip_w=8, pelvis_h=4, torso=(8, 11, 5), neck=2.0, head=(7, 7.4, 7),
                 arm=(10, 9.5), arm_w=3.0, hand=(2.6, 3.2, 2.6), hunch=6, arm_out=0.6, bend=(0.25, 0.9), head_fwd=1.5,
                 mats=dict(pelvis='cloth', torso='torso', head='head', arm='skin', fore='wrap', hand='skin', leg='wrap', shin='wrap', foot='boot'))
    hy = K['hip_y']; hc = K['head_c']
    # flame crest (three crossing flame cards)
    zero_bone(M, 'crest', 'head', (0, hc[1] - 3.2, hc[2] + 0.6))
    for k, ry in enumerate((0, 60, 120)):
        local(M, f'cr{k}', 'crest', (0, 0, 0), (0, ry, 0))
        M.cube_l(f'cr{k}', (-3.4, -10 + k, 0), (6.8, 10 - k, 0), 'flame', plane=True)
    # chest harness + pauldron + loincloth
    box(M, 'chest', (0, K['waist'][1] - 7.5, -2.9), (8.8, 8.2, 1.2), 'leather')
    box(M, 'chest', (0, K['waist'][1] - 7.5, -3.6), (1.4, 8.6, 0.8), 'strap')
    A_l = K['left_A']
    box(M, 'left_arm', (A_l[0] + 1.2, A_l[1] - 0.8, A_l[2]), (5.6, 3.4, 6.0), 'leather')
    box(M, 'hips', (0, hy + 2.0, -2.4), (6, 9.5, 0.9), 'cloth')
    box(M, 'hips', (0, hy + 2.0, 2.4), (6, 10.5, 0.9), 'cloth')
    box(M, 'hips', (0, hy - 0.6, 0), (8.8, 1.8, 5.8), 'strap')
    # quiver on the back
    zero_bone(M, 'quiver', 'chest', (2.5, K['waist'][1] - 12, 3.4))
    M.cube_l('quiver', (-1.8, 0, -1.8), (3.6, 10.5, 3.6), 'leather')
    for k in range(4):
        M.cube_l('quiver', (-1.2 + k * 0.7, -2.8 - (k % 2), -0.4 + (k % 3) * 0.4), (0.5, 3.4, 0.5), 'arrow')
    # horn bow in the left hand: a flat arc in the YZ plane
    hl = K['hands']['left']
    ptsb = [(hl[0], hl[1] - 12, hl[2] - 1.5), (hl[0], hl[1] - 7.5, hl[2] - 4.8), (hl[0], hl[1] - 2.0, hl[2] - 6.0), (hl[0], hl[1] + 3.5, hl[2] - 4.9), (hl[0], hl[1] + 8.0, hl[2] - 1.6)]
    zero_bone(M, 'bow', 'left_hand', hl)
    for i in range(len(ptsb) - 1):
        M.seg(f'bow{i}', 'bow', ptsb[i], ptsb[i + 1], 1.5, 1.6, 'horn', extend=0.2)
    zero_bone(M, 'string', 'bow', (hl[0], hl[1] - 12, hl[2] - 1.5))
    M.cube_l('string', (-0.25, 0, -0.25), (0.5, 20.0, 0.5), 'string')
    M.cube_l('bow', (-1.0, -3, -5.4), (2.0, 5, 2.4), 'strap')
    # shoulder scarf
    zero_bone(M, 'scarf', 'chest', (0, K['waist'][1] - 12.4, 2.5))
    M.cube_l('scarf', (-5.5, -1.5, 0), (11, 1.8, 0.8), 'cloth')
    M.cube_l('scarf', (-1.8, 0.2, 0), (3.6, 9.5, 0.7), 'cloth')

    Pt = Painter(M)
    Pt.mats['skin'] = mats.skin(CHAR, 0.32, veins=EM, scar=0.5)
    Pt.mats['torso'] = mats.skin(CHAR, 0.3, veins=EM, scar=0.6)
    Pt.mats['wrap'] = mats.cloth(pal('#1a0e0a', '#2e1a12', '#4a2c1c', '#6a4028'), 0.7, hem=(255, 150, 40))
    Pt.mats['cloth'] = mats.cloth(pal('#2a0a06', '#4c140c', '#7a2210', '#a63a18'), 0.9, hem=(255, 160, 50))
    Pt.mats['boot'] = mats.cloth(LEATH, 0.3)
    Pt.mats['leather'] = mats.cloth(LEATH, 0.5, weave=False)
    Pt.mats['strap'] = mats.metal(pal('#2a1a08', '#5a3a14', '#8a5a20', '#c8902c'))
    Pt.mats['flame'] = mats.flame((255, 244, 190), (255, 150, 40), (220, 60, 10), 0.8)
    Pt.mats['arrow'] = mats.flame((255, 244, 190), (255, 170, 60), (220, 70, 10))
    Pt.mats['horn'] = mats.horn(pal('#1a1210', '#3a2418', '#6a4a30', '#a68058'), 2, tip_glow=(255, 150, 40))
    Pt.mats['string'] = mats.solid((255, 190, 90))
    Pt.mats['head'] = mats.face_on(mats.skin(CHAR, 0.3, veins=EM), 'north', eyes=[(0.22, 0.4, 0.15, 0.1, 0.4, 'almond')],
                                   mouth=dict(y=0.78, w=0.25, h=0.06, col=(255, 130, 30), teeth=0), brow=dict(y=0.28, w=0.2, th=0.06, angle=0.5, col=(8, 4, 4)),
                                   eye_col=(255, 244, 170), glow_col=(255, 210, 80))

    ba = dict(stride=30, shin=40, arm=10, elbow=6, lean=3)
    A = biped_set(M, ba, death='fall_back', death_len=22 / 20)
    pull = {'left_arm': (-92, 0, 0), 'left_fore': (-6, 0, 0), 'right_arm': (-95, 25, -4), 'right_fore': (-125, 0, 0), 'chest': (2, -22, 0), 'head': (2, 22, 0)}
    rel = {'left_arm': (-95, 0, 0), 'right_arm': (-70, 25, -10), 'right_fore': (-10, 0, 0), 'chest': (3, -18, 0), 'head': (2, 20, 0)}
    A['shoot'] = swing('SHOOT', 1.3, [(0, {}), (0.25, {'left_arm': (-70, 0, 0), 'right_arm': (-40, 10, -10), 'right_fore': (-80, 0, 0), 'chest': (0, -18, 0), 'head': (0, 18, 0)}),
                                     (0.62, pull), (0.72, rel), (1.0, {'left_arm': (-60, 0, 0), 'chest': (0, -8, 0), 'head': (0, 8, 0)}), (1.3, {})])
    A['volley'] = swing('VOLLEY', 1.7, [(0, {}), (0.3, {'left_arm': (-120, 0, 0), 'right_arm': (-120, 20, -4), 'right_fore': (-120, 0, 0), 'chest': (-14, -16, 0), 'head': (-14, 16, 0)}),
                                       (1.0, {'left_arm': (-118, 0, 0), 'right_arm': (-118, 20, -4), 'right_fore': (-118, 0, 0), 'chest': (-16, -16, 0), 'head': (-14, 16, 0)}),
                                       (1.1, {'left_arm': (-122, 0, 0), 'right_arm': (-80, 20, -10), 'right_fore': (-10, 0, 0), 'chest': (-16, -16, 0)}),
                                       (1.7, {})])
    A['leap'] = swing('LEAP', 1.0, [(0, {}), (0.25, {'chest': (24, 0, 0), 'right_leg': (-40, 0, 0), 'left_leg': (-40, 0, 0), 'right_shin': (70, 0, 0), 'left_shin': (70, 0, 0), 'right_arm': (30, 0, -20), 'left_arm': (30, 0, 20), 'head': (-10, 0, 0)}),
                                   (0.45, {'chest': (-14, 0, 0), 'right_leg': (20, 0, 0), 'left_leg': (20, 0, 0), 'right_shin': (6, 0, 0), 'left_shin': (6, 0, 0), 'right_arm': (-70, 0, -40), 'left_arm': (-70, 0, 40)}),
                                   (1.0, {})])
    moves(A['leap'], [(0, {}), (0.25, {'hips': (0, -3.5, 0)}), (0.5, {'hips': (0, 2.0, 0)}), (1.0, {})])
    A['skyshot'] = swing('SKYSHOT', 2.0, [(0, {}), (0.4, {'left_arm': (-150, 0, 0), 'right_arm': (-150, 20, -4), 'right_fore': (-120, 0, 0), 'chest': (-30, -10, 0), 'head': (-34, 10, 0)}),
                                         (1.5, {'left_arm': (-150, 0, 0), 'right_arm': (-150, 20, -4), 'right_fore': (-120, 0, 0), 'chest': (-34, -10, 0), 'head': (-38, 10, 0)}),
                                         (2.0, {})])
    return M, Pt, A
