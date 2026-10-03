"""Obsidyen Dev Tormak (Obsidian Colossus) - titan of black glass and magma that wanders the Ashen Barrens."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'obsidian_colossus'
SPEC = dict(
    faction='OUTCAST', tr='Obsidyen Dev Tormak', en='Tormak, the Obsidian Colossus', hp=760, armor=14, attack=19.0, speed=0.2, size=(2.6, 6.0, 1.0), reach=5.4, kb=1.0, follow=48,
    voice=('warden', 0.45), egg=(0x08060C, 0xFF5A10), spawn=(0, 1, 1, []), xp=600, death_ticks=90, anim=(1.0, 1.1), boss=('PURPLE', False),
    abilities=[
        dict(kind='MELEE', anim='hammer', time=(40, 22), cd=34, dmg=1.0, p=(1.6, 0), color=0xFF7A20, fx='flame'),
        dict(kind='SLAM', anim='quake', time=(54, 32), cd=170, w=3, range=(0, 12), dmg=1.3, p=(9.5, 2.5), color=0xFF7A20, fx='flame', eff=('minecraft:slowness', 80, 2)),
        dict(kind='CHARGE', anim='rush', time=(44, 14), cd=240, w=3, range=(8, 24), dmg=1.2, p=(22, 0.8), color=0x6A5AA0, fx='smoke'),
        dict(kind='BEAM', anim='coreray', time=(80, 30, 48), cd=300, w=3, range=(6, 30), dmg=0.8, p=(50, 0), color=0xFF9A30, fx='flame', los=True),
        dict(kind='METEOR', anim='boulders', time=(60, 20, 24, 28, 32, 36, 40), cd=290, w=3, range=(5, 30), dmg=1.2, p=(3.0, 8), color=0x6A5AA0, fx='smoke'),
        dict(kind='SHIELD', anim='carapace', time=(80, 4), cd=340, w=2, range=(0, 14), p=(0.85, 0.6), color=0x8A7ACC, fx='crit'),
        dict(kind='BUFF', anim='rage', time=(50, 20), cd=480, w=1, range=(0, 12), p=(0, 40), color=0xFF5A10, fx='flame', eff=('minecraft:strength', 240, 1), p2=True),
        dict(kind='BURST', anim='eruption', time=(50, 32), cd=260, w=3, range=(0, 13), dmg=1.2, p=(12, 1.5), color=0xFF7A20, fx='flame', p2=True),
    ],
    drops=[('krolasyonbosses:obsidian_core', 1, 1, 1.0), ('minecraft:obsidian', 8, 16, 1.0), ('krolasyonbosses:hellsteel_ingot', 3, 6, 1.0), ('minecraft:netherite_scrap', 1, 3, 0.8), ('minecraft:experience_bottle', 6, 10, 1.0)],
    lore='Hiçbir hanedanın efendisi değil; yanardağın kalbinden uyanmış bir öfke. Taht için değil, yalnızca yıkmak için yürür.',
)

OBS = pal('#030206', '#0a0810', '#15101f', '#272038', '#41376a')
LAVA = (255, 110, 20)


def build():
    M = Model(ID, 256, 256)
    K = humanoid(M, leg=(13, 13), leg_w=9.0, foot=(11, 4.4, 14), hip_w=18, pelvis_h=8, torso=(24, 20, 14), neck=1.5, head=(11, 10, 11),
                 arm=(17, 16), arm_w=9.0, hand=(12, 12, 12), hunch=10, arm_out=1.6, stance=1.0, head_fwd=3, bend=(0.55, 0.15),
                 mats=dict(pelvis='rock', torso='torso', head='head', arm='rock', fore='rock_hot', hand='fist', leg='rock', shin='rock_hot', foot='rock'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    # obsidian crystal armour: shoulder crowns, back spikes, knee spikes
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        box(M, f'{s}_arm', (A[0] + sx * 3.0, A[1] - 2.0, A[2]), (13, 9, 14), 'obsidian')
        for k in range(4):
            local(M, f'{s}_cr{k}', f'{s}_arm', (sx * (2 + k * 2.2), -6, (k % 2) * 3 - 1.5), (0, 0, sx * (-12 - k * 12)))
            M.cube_l(f'{s}_cr{k}', (-2.2, -13 + k * 1.5, -2.2), (4.4, 13 - k * 1.5, 4.4), 'obsidian')
        kn = K[f'{s}_knee']
        local(M, f'{s}_ks', f'{s}_leg', (0, kn[1] - K[f'{s}_knee'][1] + 13, -4), (-30, 0, 0))
        M.cube_l(f'{s}_ks', (-2.0, -7, -2.0), (4, 7, 4), 'obsidian')
    zero_bone(M, 'spikes', 'chest', (0, w[1] - 18, 6.5))
    for k, (y, h) in enumerate(((-2, 14), (3, 17), (8, 16), (13, 13), (18, 10))):
        local(M, f'bsp{k}', 'spikes', (0, y, 1.5 + k * 0.3), (-30 - k * 4, 0, 0))
        M.cube_l(f'bsp{k}', (-3, -h, -3), (6, h, 6), 'obsidian')
    # chest core (glowing magma heart behind a cracked plate)
    box(M, 'chest', (0, w[1] - 10.5, -7.2), (11, 11, 2.0), 'core')
    for sx in (-1, 1):
        box(M, 'chest', (sx * 8.0, w[1] - 10.5, -7.0), (4, 13, 2.4), 'obsidian')
    # brow/crown on the head
    for sx in (-1, 1):
        chain(M, f'horn{sx}', 'head', [(sx * 4.6, hc[1] - 4.2, hc[2]), (sx * 9.0, hc[1] - 6.0, hc[2] - 1), (sx * 11.0, hc[1] - 14, hc[2] - 2)], (4.4, 3.0), 'obsidian', ext=0.2)
    box(M, 'head', (hc[0], hc[1] - 3.6, hc[2] - 4.8), (13, 3.0, 4), 'obsidian')
    # war hammer in the right hand
    sword(M, 'right_hand', length=26, width=4.4, thick=4.4, mat='haft', guard=(0.1, 0.1, 0.1), guard_mat='haft', grip=4.0, grip_mat='haft', dir='fwd', offset=(0, 0, 0))
    box(M, 'right_hand', (0, 1.0, -34), (14, 14, 18), 'hammer')
    box(M, 'right_hand', (0, 1.0, -34), (16.4, 6.4, 20), 'band')
    # magma drip planes along the arms
    for sx, s in ((-1, 'right'), (1, 'left')):
        Bp = K[f'{s}_B']
        zero_bone(M, f'{s}_drip', f'{s}_arm', (Bp[0] + sx * 3, Bp[1], Bp[2]))
        M.cube_l(f'{s}_drip', (-2.0, 0, 0), (4.0, 10, 0), 'drip', plane=True)

    Pt = Painter(M)
    Pt.mats['rock'] = mats.rock(OBS, lava=LAVA, cell=7.5, crack_w=0.55)
    Pt.mats['rock_hot'] = mats.rock(pal('#08060c', '#120e1a', '#20182e', '#352a4c'), lava=(255, 140, 30), cell=6.5, crack_w=0.6)
    Pt.mats['torso'] = mats.rock(OBS, lava=(255, 120, 24), cell=9, crack_w=0.7)
    Pt.mats['fist'] = mats.rock(pal('#0c0a14', '#18142a', '#2c2450', '#44387a'), lava=(255, 170, 50), cell=6, crack_w=0.7)
    Pt.mats['obsidian'] = mats.crystal(pal('#030206', '#0a0810', '#15101f', '#272038', '#41376a'), (140, 90, 255), facets=5)
    Pt.mats['core'] = mats.flame((255, 244, 190), (255, 140, 30), (210, 50, 10), 0.3)
    Pt.mats['head'] = mats.face_on(mats.rock(OBS, lava=LAVA, cell=5, crack_w=0.6), 'north', eyes=[(0.2, 0.4, 0.14, 0.1, 0.45, 'slit')], mouth=dict(y=0.78, w=0.36, h=0.12, teeth=5, col=(255, 130, 30), curve=-0.3),
                                   brow=dict(y=0.26, w=0.22, th=0.1, angle=0.3, col=(4, 3, 8)), eye_col=(255, 244, 190), glow_col=(255, 200, 80))
    Pt.mats['haft'] = mats.horn(pal('#0c0a14', '#1c1626', '#30284a', '#4a3e70'), 4)
    Pt.mats['hammer'] = mats.rock(pal('#08060c', '#120e1a', '#20182e', '#352a4c'), lava=(255, 130, 30), cell=6, crack_w=0.7)
    Pt.mats['band'] = mats.metal(pal('#2a2010', '#6a5018', '#c8a028', '#ffe070'), glow_edge=(255, 200, 80))
    Pt.mats['drip'] = mats.flame((255, 244, 190), (255, 130, 30), (200, 50, 10), 0.2)

    ba = dict(stride=20, shin=26, arm=12, elbow=8, bob=1.4, twist=8, lean=6, walk_len=1.3, run_len=0.95, run_mul=1.35, head_bob=3)
    A = biped_set(M, ba, death='collapse', death_len=90 / 20)
    A['hammer'] = swing('HAMMER', 1.9, [(0, {}), (0.6, {'right_arm': (-150, 0, -10), 'right_fore': (-30, 0, 0), 'chest': (-20, -22, 0), 'head': (-10, 0, 0), 'left_arm': (-30, 0, 14)}),
                                       (0.82, {'right_arm': (-25, 0, 8), 'right_fore': (-6, 0, 0), 'chest': (34, 22, 0), 'head': (12, 0, 0)}), (1.3, {'right_arm': (-25, 0, 8), 'chest': (30, 18, 0)}), (1.9, {})])
    A['quake'] = swing('QUAKE', 2.7, [(0, {}), (1.0, {'right_arm': (-178, 0, -12), 'left_arm': (-170, 0, 12), 'chest': (-26, 0, 0), 'head': (-24, 0, 0), 'right_leg': (-6, 0, 0), 'left_leg': (-6, 0, 0)}),
                                     (1.3, {'right_arm': (-30, 0, -4), 'left_arm': (-30, 0, 4), 'chest': (46, 0, 0), 'head': (22, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (-30, 0, 0), 'right_shin': (24, 0, 0), 'left_shin': (24, 0, 0)}),
                                     (2.0, {'right_arm': (-30, 0, -4), 'left_arm': (-30, 0, 4), 'chest': (40, 0, 0), 'head': (20, 0, 0)}), (2.7, {})])
    moves(A['quake'], [(0, {}), (1.3, {'hips': (0, -4.5, 0)}), (2.0, {'hips': (0, -4.0, 0)}), (2.7, {})])
    A['rush'] = swing('RUSH', 2.2, [(0, {}), (0.7, {'chest': (30, 0, 0), 'head': (-4, 0, 0), 'right_arm': (24, 0, -22), 'left_arm': (24, 0, 22), 'right_leg': (22, 0, 0), 'left_leg': (-18, 0, 0)}),
                                   (1.0, {'chest': (36, 0, 0), 'right_arm': (-70, 0, -20), 'left_arm': (-70, 0, 20), 'right_leg': (-34, 0, 0), 'left_leg': (30, 0, 0), 'left_shin': (40, 0, 0)}),
                                   (1.4, {'chest': (36, 0, 0), 'right_arm': (-80, 0, -22), 'left_arm': (-80, 0, 22), 'right_leg': (28, 0, 0), 'left_leg': (-34, 0, 0), 'right_shin': (40, 0, 0)}), (2.2, {})])
    A['coreray'] = swing('CORERAY', 4.0, [(0, {}), (0.9, {'chest': (-16, 0, 0), 'head': (-20, 0, 0), 'right_arm': (-30, 0, -70), 'left_arm': (-30, 0, 70)}), (3.4, {'chest': (-18, 0, 0), 'head': (-24, 0, 0), 'right_arm': (-34, 0, -74), 'left_arm': (-34, 0, 74)}), (4.0, {})])
    scales(A['coreray'], [(0, {'chest': (1, 1, 1)}), (0.9, {'chest': (1.12, 1.12, 1.12)}), (3.4, {'chest': (1.12, 1.12, 1.12)}), (4.0, {'chest': (1, 1, 1)})])
    A['boulders'] = swing('BOULDERS', 3.0, [(0, {}), (0.8, {'right_arm': (-170, 0, -20), 'left_arm': (-170, 0, 20), 'chest': (-22, 0, 0), 'head': (-30, 0, 0)}), (2.5, {'right_arm': (-176, 0, -20), 'left_arm': (-176, 0, 20), 'chest': (-24, 0, 0), 'head': (-34, 0, 0)}), (3.0, {})])
    A['carapace'] = swing('CARAPACE', 4.0, [(0, {}), (0.6, {'right_arm': (-90, 0, -50), 'left_arm': (-90, 0, 50), 'right_fore': (-60, 0, 0), 'left_fore': (-60, 0, 0), 'chest': (20, 0, 0), 'head': (24, 0, 0)}), (3.4, {'right_arm': (-94, 0, -54), 'left_arm': (-94, 0, 54), 'chest': (22, 0, 0), 'head': (26, 0, 0)}), (4.0, {})])
    A['rage'] = swing('RAGE', 2.5, [(0, {}), (0.7, {'chest': (-26, 0, 0), 'head': (-40, 0, 0), 'right_arm': (-60, 0, -60), 'left_arm': (-60, 0, 60)}), (1.6, {'chest': (-28, 0, 0), 'head': (-44, 0, 0), 'right_arm': (-64, 0, -66), 'left_arm': (-64, 0, 66)}), (2.5, {})])
    scales(A['rage'], [(0, {'base': (1, 1, 1)}), (0.8, {'base': (1.06, 1.06, 1.06)}), (1.6, {'base': (1.06, 1.06, 1.06)}), (2.5, {'base': (1, 1, 1)})])
    A['eruption'] = swing('ERUPTION', 2.5, [(0, {}), (0.9, {'right_arm': (-176, 0, -30), 'left_arm': (-176, 0, 30), 'chest': (-26, 0, 0), 'head': (-30, 0, 0)}), (1.2, {'right_arm': (-30, 0, -30), 'left_arm': (-30, 0, 30), 'chest': (40, 0, 0), 'head': (20, 0, 0)}), (2.5, {})])
    moves(A['eruption'], [(0, {}), (1.2, {'hips': (0, -4.0, 0)}), (2.5, {})])
    return M, Pt, A
