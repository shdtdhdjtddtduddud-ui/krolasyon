"""Gargoyl (Gargoyle) - winged stone sentinel that dives out of the sky."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'gargoyle'
SPEC = dict(
    faction='OUTCAST', tr='Gargoyl', en='Gargoyle', hp=70, armor=7, attack=9.0, speed=0.24, size=(1.1, 1.9, 1.0), fly=2.6, reach=2.8, kb=0.6, follow=40,
    voice=('warden', 1.05), egg=(0x6A6A74, 0xE05030), spawn=(5, 1, 2, ['ember_wastes', 'bone_marches', 'ashen_barrens']), xp=22, death_ticks=26, anim=(2.0, 2.0), head='head',
    abilities=[
        dict(kind='MELEE', anim='claw', time=(18, 9), cd=20, dmg=1.0, p=(0.7, 0), color=0xB0B0B8, fx='crit'),
        dict(kind='DIVE', anim='dive', time=(30, 10, 22), cd=120, w=4, range=(6, 20), dmg=1.6, p=(1.5, 0.9), color=0xB0B0B8, fx='crit', air=True),
        dict(kind='SHIELD', anim='stone', time=(60, 4), cd=260, w=2, range=(0, 14), p=(0.85, 0.6), color=0xB0B0B8, fx='crit'),
        dict(kind='SLAM', anim='slam', time=(34, 18), cd=160, w=3, range=(0, 6), dmg=1.2, p=(5, 1.5), color=0xB0B0B8, fx='ash', eff=('minecraft:slowness', 40, 1)),
    ],
    drops=[('minecraft:cobblestone', 2, 6, 0.9), ('minecraft:obsidian', 0, 2, 0.4), ('krolasyonbosses:hellsteel_nugget', 0, 2, 0.3)],
    lore='Katedral cephelerinden sökülüp savaşa sürülmüş taş bekçiler. Gökten düştüklerinde yalnızca bir gölge duyarsınız.',
)

STONE = pal('#16161a', '#2a2a32', '#44444e', '#666672', '#8c8c9a')
EM = (255, 90, 40)


def build():
    M = Model(ID, 256, 128)
    K = humanoid(M, leg=(9, 9), leg_w=5.0, foot=(5.6, 3.0, 7.6), hip_w=10, pelvis_h=5, torso=(12, 12, 7.5), neck=1.2, head=(8.6, 8.2, 8.2),
                 arm=(11, 10), arm_w=4.6, hand=(4.4, 5, 4.4), hunch=18, arm_out=1.0, bend=(0.3, 0.3), head_fwd=2.5, stance=0.3,
                 mats=dict(pelvis='stone', torso='stone', head='face', arm='stone', fore='stone2', hand='claw', leg='stone', shin='stone2', foot='claw'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    # horns and jaw
    for sx in (-1, 1):
        chain(M, f'horn{sx}', 'head', [(sx * 3.2, hc[1] - 3.6, hc[2] - 1.0), (sx * 6.4, hc[1] - 4.6, hc[2] - 2.0), (sx * 8.2, hc[1] - 9.0, hc[2] - 3.5)], (3.2, 2.2), 'claw', ext=0.2)
        zero_bone(M, f'ear{sx}', 'head', (sx * 4.4, hc[1] - 1.0, hc[2] + 1.0))
        M.cube_l(f'ear{sx}', (0 if sx > 0 else -3, -3.4, -0.4), (3, 4.4, 0.8), 'stone2')
    box(M, 'head', (hc[0], hc[1] + 3.8, hc[2] - 2.6), (6.4, 2.0, 3.4), 'stone2')
    # claws
    for sx, s in ((-1, 'right'), (1, 'left')):
        for k in range(3):
            M.cube_l(f'{s}_hand', (-1.7 + k * 1.3, 4.4, -1.8), (0.9, 3.4, 0.9), 'claw')
    # shoulder spikes + back ridge
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        zero_bone(M, f'{s}_sp', f'{s}_arm', (A[0] + sx * 3, A[1] - 1, A[2]))
        M.cube_l(f'{s}_sp', (-1.2, -6, -1.2), (2.4, 6, 2.4), 'claw')
    # chest crack glow
    box(M, 'chest', (0, w[1] - 8, -3.9), (4.4, 7, 0.8), 'crack')
    # tail
    chain(M, 'tl', 'hips', [(0, hy - 1, 3), (0, hy + 3, 8), (0, hy + 8, 12), (0, hy + 13, 14)], (2.6, 2.0, 1.4), 'stone', ext=0.3)
    zero_bone(M, 'tltip', 'tl2', (0, hy + 13, 14))
    M.cube_l('tltip', (-0.1, -1, -2.4), (0.2, 7, 4.8), 'stone2')
    # wings: bat-like with stone struts and slabs
    for sx, s in ((-1, 'right_wing'), (1, 'left_wing')):
        sh = (sx * 5.0, w[1] - 10, 4)
        e1, e2, e3 = (sx * 17, w[1] - 20, 7), (sx * 28, w[1] - 17, 9), (sx * 37, w[1] - 6, 12)
        seg(M, s + '0', 'chest', sh, e1, 3.0, 3.0, 'stone', ext=0.4)
        seg(M, s + '1', s + '0', e1, e2, 2.4, 2.4, 'stone', ext=0.3)
        seg(M, s + '2', s + '1', e2, e3, 2.0, 2.0, 'claw', ext=0.3)
        for i, (pa, pb, hh) in enumerate(((sh, e1, 14), (e1, e2, 18), (e2, e3, 18))):
            zero_bone(M, f'{s}_m{i}', s + str(i), pa)
            x0, x1 = sorted((pa[0], pb[0]))
            M.cube(f'{s}_m{i}', (x0, min(pa[1], pb[1]) + 0.2, (pa[2] + pb[2]) / 2), (x1 - x0, hh, 0), 'wing', plane=True)

    Pt = Painter(M)
    Pt.mats['stone'] = mats.rock(STONE, lava=None, cell=6, crack_w=0.5, moss=(60, 70, 40))
    Pt.mats['stone2'] = mats.rock(pal('#1c1c22', '#34343e', '#52525e', '#747482'), cell=5, crack_w=0.5)
    Pt.mats['claw'] = mats.horn(pal('#0c0c10', '#1e1e26', '#3a3a46', '#5c5c6c'), 3)
    Pt.mats['crack'] = mats.flame((255, 244, 190), (255, 130, 40), (200, 50, 10), 0.3)
    Pt.mats['wing'] = mats.membrane(pal('#1a1a20', '#2e2e38', '#46465a', '#62627a'), vein_col=(255, 100, 50), scallops=2)
    Pt.mats['face'] = mats.face_on(mats.rock(STONE, cell=5, crack_w=0.5), 'north', eyes=[(0.22, 0.38, 0.14, 0.1, 0.45, 'slit')], mouth=dict(y=0.8, w=0.3, h=0.08, teeth=4, col=(20, 20, 24)),
                                   brow=dict(y=0.26, w=0.22, th=0.09, angle=0.35, col=(10, 10, 12)), eye_col=(255, 200, 120), glow_col=EM)

    A = flyer_set(M, dict(body='chest', head='head', wings=(('right_wing', -1), ('left_wing', 1)), flap=32, flap_len=0.9, idle_len=1.8, lean=6, tail=['tl0', 'tl1', 'tl2'], legs=('right_leg', 'left_leg'), bob=1.5), death_len=26 / 20, drop=26)
    A['claw'] = swing('CLAW', 0.9, [(0, {}), (0.25, {'right_arm': (-130, 0, -12), 'left_arm': (-30, 0, 14), 'chest': (-10, -22, 0)}), (0.4, {'right_arm': (-30, 0, 10), 'chest': (20, 24, 0)}), (0.9, {})])
    A['dive'] = swing('DIVE', 1.5, [(0, {}), (0.45, {'chest': (-30, 0, 0), 'right_wing0': (0, 0, -40), 'left_wing0': (0, 0, 40), 'right_arm': (-30, 0, -30), 'left_arm': (-30, 0, 30)}),
                                   (0.7, {'chest': (60, 0, 0), 'right_wing0': (0, 0, 55), 'left_wing0': (0, 0, -55), 'right_wing1': (0, 0, 40), 'left_wing1': (0, 0, -40), 'right_arm': (-150, 0, -10), 'left_arm': (-150, 0, 10), 'head': (-30, 0, 0)}), (1.2, {'chest': (40, 0, 0)}), (1.5, {})])
    A['stone'] = swing('STONE', 3.0, [(0, {}), (0.4, {'right_arm': (-70, 0, -40), 'left_arm': (-70, 0, 40), 'right_wing0': (0, 0, 40), 'left_wing0': (0, 0, -40), 'chest': (14, 0, 0), 'head': (20, 0, 0)}), (2.6, {'right_arm': (-70, 0, -40), 'left_arm': (-70, 0, 40), 'right_wing0': (0, 0, 44), 'left_wing0': (0, 0, -44), 'chest': (14, 0, 0), 'head': (22, 0, 0)}), (3.0, {})])
    A['slam'] = swing('SLAM', 1.7, [(0, {}), (0.55, {'right_arm': (-165, 0, -14), 'left_arm': (-165, 0, 14), 'chest': (-24, 0, 0), 'head': (-20, 0, 0)}), (0.75, {'right_arm': (-30, 0, -4), 'left_arm': (-30, 0, 4), 'chest': (42, 0, 0), 'head': (20, 0, 0)}), (1.2, {'chest': (36, 0, 0)}), (1.7, {})])
    return M, Pt, A
