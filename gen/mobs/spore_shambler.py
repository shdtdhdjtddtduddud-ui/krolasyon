"""Spor Sürüngeni (Spore Shambler) - hunched fungal giant with mushroom caps on its back."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'spore_shambler'
SPEC = dict(
    faction='ROT', tr='Spor Sürüngeni', en='Spore Shambler', hp=60, armor=2, attack=8.0, speed=0.2, size=(1.1, 2.4, 1.0), reach=3.0, kb=0.4, follow=34,
    voice=('hound', 0.6), egg=(0x4A5A2A, 0xD8E860), spawn=(9, 1, 3, ['rot_bog', 'weeping_marsh']), xp=16, death_ticks=28, anim=(1.3, 1.5),
    abilities=[
        dict(kind='MELEE', anim='smash', time=(22, 12), cd=22, dmg=1.0, p=(0.8, 0), color=0x9CC030, fx='spore', eff=('minecraft:hunger', 80, 0)),
        dict(kind='CLOUD', anim='puff', time=(30, 14), cd=130, w=4, range=(0, 8), dmg=0, p=(3.8, 160), color=0xA8C838, fx='spore', eff=('minecraft:poison', 120, 0)),
        dict(kind='BURST', anim='burst', time=(34, 20), cd=240, w=2, range=(0, 6), dmg=1.0, p=(5.5, 0.9), color=0xD8E860, fx='spore', eff=('minecraft:nausea', 100, 0)),
        dict(kind='BUFF', anim='regrow', time=(40, 16), cd=380, w=1, range=(0, 12), p=(0, 14), color=0x9CC030, fx='spore', eff=('minecraft:regeneration', 120, 1)),
    ],
    drops=[('minecraft:brown_mushroom', 1, 4, 0.9), ('minecraft:red_mushroom', 0, 3, 0.6), ('krolasyonbosses:rot_spore', 0, 2, 0.6)],
    lore='Çürük Divanı\'nın yürüyen bahçeleri. Sırtındaki mantarlar her ölüm bildirimiyle biraz daha büyür; patladığında etraf havasız kalır.',
)

BARK = pal('#14100a', '#2a2012', '#46341c', '#6a502c', '#8e703c')
MOSS = pal('#101a08', '#1e3010', '#34501a', '#5a7e2a', '#88aa40')
CAP = pal('#2a0e0a', '#5a1c14', '#8c3220', '#c05a30', '#e8904a')
SPORE = (200, 255, 80)


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(8.5, 8.5), leg_w=5.4, foot=(7, 3, 8.5), hip_w=11, pelvis_h=5, torso=(13, 13, 9), neck=0.5, head=(7, 7, 7),
                 arm=(13, 12), arm_w=5.0, hand=(6.4, 7.4, 6.4), hunch=30, arm_out=1.0, bend=(0.1, 0.1), head_fwd=3.5, stance=0.4,
                 mats=dict(pelvis='bark', torso='fungal', head='face', arm='bark', fore='moss', hand='moss', leg='bark', shin='moss', foot='moss'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    # mushroom caps on the back and shoulders
    for k, (x, yo, z, s, tilt) in enumerate(((0, 13, 7.5, 11, -25), (-5, 8, 8.5, 8, -35), (5.5, 9, 8.2, 7, -15), (-6.5, 15, 4.5, 6, -10), (0, 4, 9, 6, -40))):
        zero_bone(M, f'cap{k}', 'chest', (x, w[1] - yo, z))
        local(M, f'cap{k}t', f'cap{k}', (0, 0, 0), (tilt, 0, 0))
        M.cube_l(f'cap{k}t', (-1.2, -s * 0.6, -1.2), (2.4, s * 0.6, 2.4), 'stem')
        M.cube_l(f'cap{k}t', (-s / 2, -s * 0.6 - 2.2, -s / 2), (s, 2.4, s), 'cap')
        M.cube_l(f'cap{k}t', (-s * 0.35, -s * 0.6 - 3.6, -s * 0.35), (s * 0.7, 1.6, s * 0.7), 'cap')
    # head cap (a mushroom hat)
    box(M, 'head', (hc[0], hc[1] - 4.4, hc[2]), (11, 2.4, 11), 'cap')
    box(M, 'head', (hc[0], hc[1] - 6.6, hc[2]), (7.6, 2.2, 7.6), 'cap')
    # moss strands hanging off arms and chin
    for sx, s in ((-1, 'right'), (1, 'left')):
        B = K[f'{s}_B']
        zero_bone(M, f'{s}_moss', f'{s}_arm', (B[0] + sx * 1.8, B[1] - 3, B[2]))
        M.cube_l(f'{s}_moss', (-1.5, 0, -2), (3, 9, 0.6), 'strand')
        M.cube_l(f'{s}_moss', (-0.5, 0, 1), (3, 7, 0.6), 'strand')
    # bulbous growths on the belly
    box(M, 'chest', (3.5, w[1] - 4.5, -4.8), (5, 5, 2.4), 'bulb')
    box(M, 'chest', (-3.5, w[1] - 8.5, -4.8), (4, 4, 2.0), 'bulb')

    Pt = Painter(M)
    Pt.mats['bark'] = mats.rock(BARK, cell=5, crack_w=0.6, moss=MOSS[2])
    Pt.mats['fungal'] = mats.fungus(BARK, spot=(210, 200, 120), spot_n=0.25, glow_spot=SPORE)
    Pt.mats['moss'] = mats.fur(MOSS, 3)
    Pt.mats['strand'] = mats.ragged(mats.fur(MOSS, 4), depth=5)
    Pt.mats['stem'] = mats.fungus(pal('#4a4030', '#8a7a58', '#c0b088', '#e8dcb8'), spot_n=0, gills=False)
    Pt.mats['cap'] = mats.fungus(CAP, spot=(240, 230, 200), spot_n=0.3, glow_spot=None)
    Pt.mats['bulb'] = mats.fungus(pal('#3a4a14', '#5a7a20', '#88aa30', '#c0d860'), spot=(230, 255, 120), spot_n=0.3, glow_spot=SPORE)
    Pt.mats['face'] = mats.face_on(mats.rock(BARK, cell=5, crack_w=0.6), 'north', eyes=[(0.22, 0.4, 0.13, 0.13, 0.0, 'round')], mouth=dict(y=0.78, w=0.3, h=0.1, col=(10, 14, 4), teeth=0),
                                   eye_col=(230, 255, 120), glow_col=SPORE)

    ba = dict(stride=24, shin=32, arm=14, elbow=6, bob=1.0, twist=8, lean=8, walk_len=1.2, run_len=0.85, run_mul=1.4, head_bob=4)
    A = biped_set(M, ba, death='collapse', death_len=28 / 20)
    for nm in ('idle', 'walk', 'run'):
        for k in range(5):
            gen(A[nm], f'cap{k}', 'r', lambda ph, k=k: (2 * S(ph, 1, k * 0.2), 0, 3 * S(ph, 1, k * 0.15)), n=8)
    A['smash'] = swing('SMASH', 1.3, [(0, {}), (0.35, {'right_arm': (-140, 0, -10), 'right_fore': (-30, 0, 0), 'chest': (-12, -18, 0), 'head': (-10, 0, 0), 'left_arm': (-20, 0, 10)}),
                                     (0.52, {'right_arm': (-18, 0, 6), 'right_fore': (-8, 0, 0), 'chest': (30, 20, 0), 'head': (12, 0, 0)}), (0.9, {'right_arm': (-18, 0, 6), 'chest': (26, 16, 0)}), (1.3, {})])
    A['puff'] = swing('PUFF', 1.5, [(0, {}), (0.5, {'chest': (-16, 0, 0), 'head': (-22, 0, 0), 'right_arm': (-30, 0, -26), 'left_arm': (-30, 0, 26)}), (0.7, {'chest': (28, 0, 0), 'head': (16, 0, 0), 'right_arm': (-70, 0, -10), 'left_arm': (-70, 0, 10)}), (1.5, {})])
    A['burst'] = swing('BURST', 1.7, [(0, {}), (0.7, {'chest': (-24, 0, 0), 'head': (-30, 0, 0), 'right_arm': (-80, 0, -50), 'left_arm': (-80, 0, 50)}), (1.0, {'chest': (22, 0, 0), 'head': (14, 0, 0), 'right_arm': (-20, 0, -10), 'left_arm': (-20, 0, 10)}), (1.7, {})])
    scales(A['burst'], [(0, {'chest': (1, 1, 1)}), (0.7, {'chest': (1.2, 1.15, 1.2)}), (0.9, {'chest': (0.9, 0.9, 0.9)}), (1.7, {'chest': (1, 1, 1)})])
    A['regrow'] = swing('REGROW', 2.0, [(0, {}), (0.5, {'chest': (14, 0, 0), 'head': (18, 0, 0), 'right_arm': (-30, 0, -10), 'left_arm': (-30, 0, 10)}), (1.5, {'chest': (16, 0, 0), 'head': (20, 0, 0)}), (2.0, {})])
    for k in range(5):
        scales(A['regrow'], [(0, {f'cap{k}': (1, 1, 1)}), (0.7, {f'cap{k}': (1.35, 1.35, 1.35)}), (1.5, {f'cap{k}': (1.35, 1.35, 1.35)}), (2.0, {f'cap{k}': (1, 1, 1)})])
    return M, Pt, A
