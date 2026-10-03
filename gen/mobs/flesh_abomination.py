"""Et Yığını (Flesh Abomination) - stitched bloated butcher-horror with a meat hook and a belly maw."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'flesh_abomination'
SPEC = dict(
    faction='BLOOD', tr='Et Yığını', en='Flesh Abomination', hp=120, armor=4, attack=10.0, speed=0.2, size=(1.7, 3.0, 1.0), reach=3.4, kb=0.7, follow=34,
    voice=('warden', 0.7), egg=(0xA84858, 0x3A0A14), spawn=(4, 1, 1, ['crimson_gardens', 'blood_spires']), xp=34, death_ticks=34, anim=(1.2, 1.3),
    abilities=[
        dict(kind='MELEE', anim='cleave', time=(26, 14), cd=26, dmg=1.0, p=(1.0, 0.15), color=0xC8102E, fx='drip'),
        dict(kind='PULL', anim='hook', time=(30, 14), cd=130, w=4, range=(4, 14), dmg=0.8, p=(1.8, 0), color=0xB0B0B8, fx='crit', eff=('minecraft:slowness', 50, 1), los=True),
        dict(kind='SLAM', anim='slam', time=(36, 22), cd=150, w=3, range=(0, 7), dmg=1.2, p=(5.5, 1.5), color=0xC8102E, fx='drip'),
        dict(kind='CLOUD', anim='vomit', time=(34, 18), cd=210, w=3, range=(3, 12), dmg=0, p=(3.6, 160), color=0x70B020, fx='spore', eff=('minecraft:poison', 100, 1)),
        dict(kind='BUFF', anim='regen', time=(40, 16), cd=380, w=1, range=(0, 20), p=(0, 24), color=0xFF4060, fx='heart', eff=('minecraft:regeneration', 140, 1)),
    ],
    drops=[('minecraft:rotten_flesh', 3, 8, 1.0), ('krolasyonbosses:blood_vial', 1, 3, 0.7), ('minecraft:iron_ingot', 0, 2, 0.4)],
    lore='Kan Konseyi\'nın cerrahi hataları. Birçok bedenden dikilmiş, hâlâ açlıkla titreyen bir yığın; kancası yakaladığı hiçbir şeyi bırakmaz.',
)

FLESH = pal('#2a0e12', '#5c2228', '#8a3a40', '#b8605e', '#d98e82')
SKIN2 = pal('#2a1c1c', '#4e3834', '#7a5a52', '#a68474', '#cfae98')
IRON = pal('#0e1014', '#1e2128', '#363b46', '#565d6a', '#8a93a2')


def build():
    M = Model(ID, 256, 256)
    K = humanoid(M, leg=(9, 9.5), leg_w=7.0, foot=(8.5, 3.2, 10.5), hip_w=15, pelvis_h=6, torso=(20, 16, 14), neck=0.5, head=(8, 8, 8),
                 arm=(13, 12), arm_w=6.6, hand=(8, 8, 8), hunch=24, arm_out=1.6, stance=0.8, head_fwd=2, bend=(0.2, 0.15),
                 mats=dict(pelvis='skin2', torso='flesh', head='face', arm='skin2', fore='flesh', hand='flesh', leg='skin2', shin='flesh', foot='skin2'))
    hy = K['hip_y']; hc = K['head_c']
    w = K['waist']
    # belly maw
    box(M, 'chest', (0, w[1] - 5.5, -7.6), (11, 9, 2.0), 'maw')
    for k in range(6):
        box(M, 'chest', (-4.4 + k * 1.76, w[1] - 8.5, -9.0), (0.9, 2.2, 0.9), 'tooth')
        box(M, 'chest', (-4.4 + k * 1.76, w[1] - 2.8, -9.0), (0.9, 2.2, 0.9), 'tooth')
    # exposed ribs and spine
    zero_bone(M, 'ribs', 'chest', (0, w[1] - 14, 6.5))
    for k in range(5):
        local(M, f'rib{k}', 'ribs', (0, k * 2.6, 0), (0, 0, 0))
        M.cube_l(f'rib{k}', (-8, 0, 0), (16, 1.0, 1.0), 'bone')
    # stitches / straps
    for y in (4, 9, 13):
        box(M, 'chest', (0, w[1] - y, 0), (20.6, 1.0, 14.6), 'strap')
    # extra small arms (fused to the ribs)
    for sx, s in ((-1, 'right'), (1, 'left')):
        zero_bone(M, f'{s}_mini', 'chest', (sx * 9.5, w[1] - 6, -6))
        M.cube_l(f'{s}_mini', (-1.2, 0, -1.2), (2.4, 7, 2.4), 'skin2')
        M.cube_l(f'{s}_mini', (-1.4, 7, -1.8), (2.8, 2.6, 2.8), 'flesh')
    # meat hook + chain on the left hand
    zero_bone(M, 'hook', 'left_hand', K['hands']['left'])
    for k in range(3):
        M.cube_l('hook', (-1.0, 2 + k * 2.6, -0.5 + (k % 2) * 0.6), (2.0, 2.0, 1.0), 'iron')
    M.cube_l('hook', (-1.2, 9.6, -1.2), (2.4, 5.0, 2.4), 'iron')
    M.cube_l('hook', (-1.2, 12.6, -1.2), (2.4, 2.4, 5.0), 'iron')
    M.cube_l('hook', (-1.2, 9.6, 2.4), (2.4, 5.0, 2.4), 'iron')
    # cleaver on the right
    sword(M, 'right_hand', length=20, width=9, thick=1.4, mat='iron', guard=(0.1, 0.1, 0.1), guard_mat='iron', grip=3.5, grip_mat='strap', dir='fwd', offset=(0, 0, 0))
    # tiny head with stitched mask, rusted bucket helm
    box(M, 'head', (hc[0], hc[1] - 3.2, hc[2]), (9.2, 3.4, 9.2), 'iron')
    # tubes from head to shoulders
    for sx in (-1, 1):
        chain(M, f'tube{sx}', 'chest', [(sx * 3, w[1] - 17, -3), (sx * 6.5, w[1] - 15, -5.5), (sx * 9.0, w[1] - 12, -5)], (1.4, 1.2), 'tube', ext=0.2)

    Pt = Painter(M)
    Pt.mats['flesh'] = mats.flesh(FLESH, vein=(110, 20, 40), stitches=True)
    Pt.mats['skin2'] = mats.flesh(SKIN2, vein=(100, 40, 50), stitches=True)
    Pt.mats['bone'] = mats.bone(pal('#4b4436', '#7d735c', '#b5a888', '#d9cfae'), 0.5)
    Pt.mats['strap'] = mats.cloth(pal('#0c0806', '#1c130c', '#2e2014', '#483220'), 0.4, weave=False, trim=(120, 120, 130))
    Pt.mats['iron'] = mats.metal(IRON)
    Pt.mats['tube'] = mats.flesh(pal('#3a0a14', '#7a1a28', '#b02a40', '#e04a60'), stitches=False)
    Pt.mats['tooth'] = mats.horn(pal('#4a3a30', '#8a7a68', '#d8c8b0', '#f4ead8'), 3)
    Pt.mats['maw'] = mats.flesh(pal('#2a0610', '#6a1020', '#a01c34', '#d83a52'), vein=(255, 60, 80), stitches=False, glow_vein=(255, 40, 70))
    Pt.mats['face'] = mats.face_on(mats.flesh(SKIN2, stitches=True), 'north', eyes=[(0.18, 0.46, 0.1, 0.1, 0.0, 'round', True)], mouth=dict(y=0.82, w=0.34, h=0.08, teeth=5, col=(30, 6, 10)),
                                   eye_col=(255, 70, 70), glow_col=(255, 40, 40))

    ba = dict(stride=22, shin=30, arm=14, elbow=8, bob=1.0, twist=10, lean=8, walk_len=1.2, run_len=0.85, run_mul=1.4, head_bob=5)
    A = biped_set(M, ba, death='collapse', death_len=34 / 20)
    A['cleave'] = swing('CLEAVE', 1.4, [(0, {}), (0.4, {'right_arm': (-135, 0, -10), 'right_fore': (-30, 0, 0), 'chest': (-14, -20, 0), 'head': (-10, 0, 0), 'left_arm': (-30, 0, 14)}),
                                       (0.56, {'right_arm': (-20, 0, 6), 'right_fore': (-8, 0, 0), 'chest': (32, 20, 0), 'head': (10, 0, 0)}), (0.9, {'right_arm': (-20, 0, 6), 'chest': (28, 16, 0)}), (1.4, {})])
    A['hook'] = swing('HOOK', 1.5, [(0, {}), (0.4, {'left_arm': (-90, 0, 40), 'left_fore': (-30, 0, 0), 'chest': (-8, 26, 0), 'head': (-8, -12, 0)}), (0.62, {'left_arm': (-92, 0, -6), 'left_fore': (-6, 0, 0), 'chest': (24, -22, 0), 'head': (10, 10, 0)}),
                                   (1.1, {'left_arm': (-60, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (18, -10, 0)}), (1.5, {})])
    A['slam'] = swing('SLAM', 1.8, [(0, {}), (0.6, {'right_arm': (-165, 0, -16), 'left_arm': (-165, 0, 16), 'chest': (-24, 0, 0), 'head': (-20, 0, 0)}),
                                   (0.82, {'right_arm': (-28, 0, -4), 'left_arm': (-28, 0, 4), 'chest': (44, 0, 0), 'head': (22, 0, 0), 'right_shin': (22, 0, 0), 'left_shin': (22, 0, 0)}), (1.3, {'right_arm': (-28, 0, -4), 'left_arm': (-28, 0, 4), 'chest': (38, 0, 0)}), (1.8, {})])
    moves(A['slam'], [(0, {}), (0.82, {'hips': (0, -3.0, 0)}), (1.3, {'hips': (0, -2.5, 0)}), (1.8, {})])
    A['vomit'] = swing('VOMIT', 1.7, [(0, {}), (0.5, {'chest': (-20, 0, 0), 'head': (-34, 0, 0), 'right_arm': (-30, 0, -20), 'left_arm': (-30, 0, 20)}), (0.7, {'chest': (50, 0, 0), 'head': (34, 0, 0), 'right_arm': (10, 0, -12), 'left_arm': (10, 0, 12)}), (1.3, {'chest': (46, 0, 0), 'head': (30, 0, 0)}), (1.7, {})])
    A['regen'] = swing('REGEN', 2.0, [(0, {}), (0.5, {'chest': (14, 0, 0), 'head': (20, 0, 0), 'right_arm': (-70, 0, 10), 'left_arm': (-70, 0, -10)}), (1.5, {'chest': (16, 0, 0), 'head': (22, 0, 0), 'right_arm': (-74, 0, 12), 'left_arm': (-74, 0, -12)}), (2.0, {})])
    scales(A['regen'], [(0, {'chest': (1, 1, 1)}), (0.6, {'chest': (1.08, 1.06, 1.08)}), (1.0, {'chest': (0.98, 0.98, 0.98)}), (1.4, {'chest': (1.08, 1.06, 1.08)}), (2.0, {'chest': (1, 1, 1)})])
    return M, Pt, A
