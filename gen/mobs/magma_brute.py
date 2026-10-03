"""Magma Canavarı (Magma Brute) - hulking volcanic-rock bruiser of the Ember Dominion."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'magma_brute'
SPEC = dict(
    faction='EMBER', tr='Magma Canavarı', en='Magma Brute', hp=110, armor=8, attack=11.0, speed=0.21, size=(1.7, 3.4, 1.0), reach=3.3, kb=0.75,
    voice=('warden', 0.62), egg=(0x241812, 0xFF5A10), spawn=(5, 1, 2, ['ember_wastes', 'ashen_barrens']), xp=30, death_ticks=34, anim=(1.25, 1.35),
    abilities=[
        dict(kind='MELEE', anim='attack', time=(26, 13, 19), cd=24, dmg=1.0, p=(1.0, 0), color=0xFF6A1A, fx='flame'),
        dict(kind='SLAM', anim='slam', time=(34, 20), cd=130, w=4, range=(0, 7), dmg=1.3, p=(5.5, 1.5), color=0xFF7A20, fx='flame', eff=('minecraft:slowness', 50, 1)),
        dict(kind='CHARGE', anim='charge', time=(34, 12), cd=190, w=3, range=(6, 16), dmg=1.2, p=(16, 0.85), color=0xFF5A10, fx='smoke'),
        dict(kind='ERUPT', anim='stomp', time=(36, 16, 22, 28), cd=240, w=2, range=(2, 14), dmg=0.9, p=(1, 6), color=0xFF6A1A, fx='flame'),
    ],
    drops=[('krolasyonbosses:ember_shard', 1, 3, 0.8), ('minecraft:magma_cream', 0, 2, 0.5), ('minecraft:obsidian', 0, 2, 0.5)],
    lore='Eriyik kayanın ve öfkenin yoğrulduğu dev. Kor Hanedanı\'nın kuşatma makinesi; yumruğunun düştüğü yerde yer yarılır.',
)

ROCK = pal('#0f0c0b', '#1c1613', '#2c231e', '#42352c', '#5c4a3c')
OBS = pal('#050408', '#0c0a12', '#16121f', '#262036', '#3c3354')
LAVA = (255, 110, 20)


def build():
    M = Model(ID, 256, 256)
    K = humanoid(M, leg=(10, 10.5), leg_w=7.6, foot=(9, 4.0, 11), hip_w=15, pelvis_h=7, torso=(19, 17, 11), neck=1.0, head=(9, 7.5, 9),
                 arm=(13, 12), arm_w=7.6, hand=(10, 10, 10), hunch=20, arm_out=1.4, stance=0.8, head_fwd=3,
                 mats=dict(pelvis='rock', torso='torso', head='head', arm='rock', fore='rock_hot', hand='fist', leg='rock', shin='rock_hot', foot='rock'))
    hy = K['hip_y']
    # obsidian shoulder boulders
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        box(M, f'{s}_arm', (A[0] + sx * 2.2, A[1] - 1.5, A[2]), (10, 7, 11), 'obsidian')
        for k in range(3):
            local(M, f'{s}_spk{k}', f'{s}_arm', (sx * (3 + k * 1.5), -4.0 - k * 0.5, 1.5 * k - 1.5), (0, 0, sx * (-18 - k * 14)))
            M.cube_l(f'{s}_spk{k}', (-1.6, -9 + k, -1.6), (3.2, 9 - k, 3.2), 'obsidian')
        # lava dripping from fists
    # back spikes (obsidian ridge) following the spine
    zero_bone(M, 'spikes', 'chest', (0, K['waist'][1] - 12, 5))
    for k, (y, sz) in enumerate(((-2, 10), (3, 12), (8, 11), (13, 9))):
        local(M, f'sp{k}', 'spikes', (0, y, 1.5 + k * 0.4), (-28 - k * 6, 0, 0))
        M.cube_l(f'sp{k}', (-2.4, -sz, -2.4), (4.8, sz, 4.8), 'obsidian')
    # glowing core pit in the chest (a slightly inset lava plate)
    box(M, 'chest', (0, K['waist'][1] - 9.5, -6.3), (9, 8, 1.4), 'core')
    # tusks and brow
    hc = K['head_c']
    for sx in (-1, 1):
        zero_bone(M, f'tusk{sx}', 'head', (sx * 3.2, hc[1] + 3.0, hc[2] - 4.2))
        M.cube_l(f'tusk{sx}', (-1.1, -6.0, -1.1), (2.2, 6.0, 2.2), 'tusk')
    box(M, 'head', (hc[0], hc[1] - 1.8, hc[2] - 3.4), (10.4, 2.6, 4.4), 'obsidian')
    # belt of chains/rock
    box(M, 'hips', (0, hy - 1.0, 0), (16.6, 3, 11.4), 'obsidian')
    # lava drip plane on each fist
    # horns on head
    for sx in (-1, 1):
        chain(M, f'horn{sx}', 'head', [(sx * 3.4, hc[1] - 2.5, hc[2] + 0.5), (sx * 6.6, hc[1] - 4.5, hc[2] + 0.2), (sx * 8.6, hc[1] - 9.5, hc[2] - 1.0)], (2.8, 2.0), 'obsidian', ext=0.2)

    Pt = Painter(M)
    Pt.mats['rock'] = mats.rock(ROCK, lava=LAVA, cell=7.5, crack_w=0.55)
    Pt.mats['rock_hot'] = mats.rock(pal('#150f0c', '#241a15', '#382a22', '#52402f'), lava=(255, 140, 30), cell=6.5, crack_w=0.6)
    Pt.mats['torso'] = mats.rock(ROCK, lava=(255, 120, 24), cell=9, crack_w=0.6)
    Pt.mats['fist'] = mats.rock(pal('#1a120e', '#2e211a', '#4a3626', '#6a4e34'), lava=(255, 170, 50), cell=6, crack_w=0.7)
    Pt.mats['obsidian'] = mats.crystal(OBS, (120, 70, 200), facets=5)
    Pt.mats['core'] = mats.flame((255, 244, 190), (255, 140, 30), (210, 50, 10), 0.3)
    Pt.mats['tusk'] = mats.horn(pal('#2a2218', '#5c4a34', '#a08868', '#e0cfa8'), 3)
    Pt.mats['head'] = mats.face_on(mats.rock(ROCK, lava=LAVA, cell=5, crack_w=0.8), 'north',
                                   eyes=[(0.22, 0.38, 0.14, 0.1, 0.5, 'slit')], mouth=dict(y=0.78, w=0.38, h=0.12, teeth=4, col=(255, 120, 30), curve=-0.3),
                                   brow=dict(y=0.26, w=0.22, th=0.1, angle=0.3, col=(10, 6, 6)), eye_col=(255, 240, 160), glow_col=(255, 200, 60))

    ba = dict(stride=22, shin=30, arm=16, elbow=8, bob=1.1, twist=9, lean=8, walk_len=1.15, run_len=0.8, run_mul=1.4, head_bob=4)
    A = biped_set(M, ba, death='collapse', death_len=34 / 20)
    A['attack'] = swing('ATTACK', 1.3, [
        (0.00, {}),
        (0.38, {'right_arm': (-135, 0, -14), 'right_fore': (-40, 0, 0), 'chest': (-18, -22, 0), 'head': (-10, 0, 0), 'left_arm': (-30, 0, 12)}),
        (0.52, {'right_arm': (-15, 0, 4), 'right_fore': (-10, 0, 0), 'chest': (30, 24, 0), 'head': (10, 0, 0), 'left_arm': (-50, 0, 10)}),
        (0.76, {'left_arm': (-130, 0, 14), 'left_fore': (-40, 0, 0), 'chest': (-14, 26, 0), 'right_arm': (-20, 0, -6), 'head': (-8, 0, 0)}),
        (0.88, {'left_arm': (-12, 0, -4), 'left_fore': (-10, 0, 0), 'chest': (32, -24, 0), 'right_arm': (-20, 0, -6), 'head': (10, 0, 0)}),
        (1.30, {})])
    A['slam'] = swing('SLAM', 1.7, [
        (0.00, {}),
        (0.60, {'right_arm': (-170, 0, -16), 'left_arm': (-170, 0, 16), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (-26, 0, 0), 'head': (-22, 0, 0), 'right_leg': (-8, 0, 0), 'left_leg': (-8, 0, 0)}),
        (0.78, {'right_arm': (-30, 0, -4), 'left_arm': (-30, 0, 4), 'right_fore': (-8, 0, 0), 'left_fore': (-8, 0, 0), 'chest': (42, 0, 0), 'head': (22, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (-30, 0, 0), 'right_shin': (24, 0, 0), 'left_shin': (24, 0, 0)}),
        (1.20, {'right_arm': (-30, 0, -4), 'left_arm': (-30, 0, 4), 'chest': (36, 0, 0), 'head': (18, 0, 0), 'right_leg': (-26, 0, 0), 'left_leg': (-26, 0, 0), 'right_shin': (20, 0, 0), 'left_shin': (20, 0, 0)}),
        (1.70, {})])
    moves(A['slam'], [(0, {}), (0.78, {'hips': (0, -3.5, 0)}), (1.2, {'hips': (0, -3.0, 0)}), (1.7, {})])
    A['charge'] = swing('CHARGE', 1.7, [
        (0.00, {}),
        (0.55, {'chest': (28, 0, 0), 'head': (-4, 0, 0), 'right_arm': (24, 0, -22), 'left_arm': (24, 0, 22), 'right_leg': (22, 0, 0), 'left_leg': (-18, 0, 0), 'right_shin': (30, 0, 0)}),
        (0.75, {'chest': (36, 0, 0), 'head': (-8, 0, 0), 'right_arm': (-70, 0, -20), 'left_arm': (-70, 0, 20), 'right_leg': (-34, 0, 0), 'left_leg': (30, 0, 0), 'left_shin': (40, 0, 0)}),
        (1.00, {'chest': (36, 0, 0), 'head': (-8, 0, 0), 'right_arm': (-80, 0, -22), 'left_arm': (-80, 0, 22), 'right_leg': (28, 0, 0), 'left_leg': (-34, 0, 0), 'right_shin': (40, 0, 0)}),
        (1.25, {'chest': (36, 0, 0), 'right_leg': (-34, 0, 0), 'left_leg': (30, 0, 0), 'left_shin': (40, 0, 0), 'right_arm': (-80, 0, -22), 'left_arm': (-80, 0, 22)}),
        (1.70, {})])
    A['stomp'] = swing('STOMP', 1.8, [
        (0.00, {}),
        (0.50, {'right_leg': (-82, 0, 0), 'right_shin': (70, 0, 0), 'chest': (-14, 0, 0), 'right_arm': (-60, 0, -30), 'left_arm': (-60, 0, 30), 'head': (-10, 0, 0)}),
        (0.70, {'right_leg': (-6, 0, 0), 'right_shin': (2, 0, 0), 'chest': (18, 0, 0), 'right_arm': (-30, 0, -20), 'left_arm': (-30, 0, 20), 'head': (10, 0, 0)}),
        (1.00, {'left_leg': (-82, 0, 0), 'left_shin': (70, 0, 0), 'chest': (-14, 0, 0), 'right_arm': (-60, 0, -30), 'left_arm': (-60, 0, 30)}),
        (1.20, {'left_leg': (-6, 0, 0), 'chest': (18, 0, 0), 'head': (10, 0, 0)}),
        (1.8, {})])
    return M, Pt, A
