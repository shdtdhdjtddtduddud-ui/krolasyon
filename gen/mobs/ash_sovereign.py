"""Kül Hükümdarı Mor'Vael (Ash Sovereign) - the ghost-emperor of Azrakor: ash armour, burning crown, spectral greatsword."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'ash_sovereign'
SPEC = dict(
    faction='OUTCAST', tr='Kül Hükümdarı Mor\'Vael', en='Mor\'Vael, the Ash Sovereign', hp=1100, armor=12, attack=20.0, speed=0.3, size=(1.5, 4.4, 1.0), reach=4.8, kb=1.0, follow=64,
    voice=('demon', 0.7), egg=(0x1A1A20, 0xFF7A20), spawn=(0, 1, 1, []), xp=1200, death_ticks=100, anim=(1.2, 1.4), boss=('RED', False),
    abilities=[
        dict(kind='MELEE', anim='cleave', time=(38, 14, 24, 32), cd=26, dmg=0.85, p=(1.2, 0), color=0xFF9A40, fx='flame'),
        dict(kind='SLAM', anim='plunge', time=(46, 28), cd=150, w=3, range=(0, 10), dmg=1.3, p=(8.0, 2.2), color=0xFF7A20, fx='flame', eff=('minecraft:slowness', 70, 1)),
        dict(kind='BEAM', anim='ruinbeam', time=(80, 26, 46), cd=290, w=3, range=(6, 32), dmg=0.8, p=(50, 0), color=0xFFB060, fx='flame', los=True),
        dict(kind='METEOR', anim='skyfall', time=(64, 20, 24, 28, 32, 36, 40, 44), cd=280, w=3, range=(5, 30), dmg=1.2, p=(3.2, 9), color=0xFF5A10, fx='flame'),
        dict(kind='SUMMON', anim='court', time=(56, 28), cd=520, w=2, range=(0, 32), p=(2, 3), summon='bone_knight', color=0x9AE8FF, fx='soul'),
        dict(kind='SUMMON', anim='court', time=(56, 28), cd=540, w=2, range=(0, 32), p=(1, 2), summon='magma_brute', color=0xFF7A20, fx='flame'),
        dict(kind='SUMMON', anim='court', time=(56, 28), cd=520, w=2, range=(0, 32), p=(2, 3), summon='crimson_duelist', color=0xC8102E, fx='drip'),
        dict(kind='SUMMON', anim='court', time=(56, 28), cd=540, w=2, range=(0, 32), p=(2, 3), summon='shade_stalker', color=0x8A5CFF, fx='void'),
        dict(kind='SUMMON', anim='court', time=(56, 28), cd=560, w=2, range=(0, 32), p=(1, 2), summon='spore_shambler', color=0x9CC030, fx='spore'),
        dict(kind='TELEPORT_STRIKE', anim='strike', time=(30, 8, 20), cd=170, w=3, range=(4, 22), dmg=1.4, p=(0, 0), color=0xFF7A20, fx='flame'),
        dict(kind='SHIELD', anim='aegis', time=(70, 4), cd=330, w=2, range=(0, 12), p=(0.88, 0.7), color=0xFFB060, fx='crit'),
        dict(kind='DRAIN', anim='devour', time=(80, 14), cd=240, w=3, range=(3, 14), dmg=0.8, p=(0, 1.0), color=0xFF7A20, fx='flame', los=True, p2=True),
        dict(kind='BURST', anim='cataclysm', time=(54, 34), cd=250, w=3, range=(0, 13), dmg=1.3, p=(13, 1.6), color=0xFFA040, fx='flame', p2=True),
        dict(kind='PULL', anim='chains', time=(40, 20), cd=170, w=3, range=(5, 24), dmg=0.9, p=(2.4, 0), color=0xB0B0C0, fx='crit', eff=('minecraft:slowness', 70, 2), los=True),
    ],
    drops=[('krolasyonbosses:ash_crown', 1, 1, 1.0), ('krolasyonbosses:sovereign_seal', 1, 1, 1.0), ('krolasyonbosses:hellsteel_ingot', 6, 12, 1.0), ('minecraft:nether_star', 1, 1, 1.0), ('minecraft:experience_bottle', 10, 16, 1.0)],
    lore='Bin yıl önce Azrakor\'u yöneten imparator; ruhunu tahtın külüne gömdü ki hanedanlar birbirini yesin ve gerçek bir hükümdar ortaya çıksın.',
)

ASH = pal('#121214', '#222226', '#363640', '#50505e', '#74748a')
GOLD = pal('#5a4010', '#8f6a18', '#d8a828', '#ffe070')
EM = (255, 120, 24)


def build():
    M = Model(ID, 256, 256)
    K = humanoid(M, leg=(15, 15), leg_w=5.4, foot=(6.6, 3.4, 10), hip_w=12, pelvis_h=6, torso=(13.4, 16, 8.2), neck=2.6, head=(9.6, 10.2, 9.6),
                 arm=(15.5, 14), arm_w=4.6, hand=(4.6, 5.2, 4.6), hunch=3, arm_out=1.0, bend=(0.75, 0.2), head_fwd=0.8,
                 mats=dict(pelvis='armor', torso='armor_chest', head='face', arm='armor', fore='gauntlet', hand='armor', leg='greave', shin='greave', foot='boot'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    box(M, 'chest', (0, w[1] - 9.0, -4.8), (14.4, 12, 3.0), 'plate')
    box(M, 'chest', (0, w[1] - 9.0, 4.2), (13, 12.5, 2.0), 'plate_dark')
    box(M, 'chest', (0, w[1] - 16.4, -1.0), (13.4, 2.8, 9.6), 'gold')
    box(M, 'hips', (0, hy - 1.0, 0), (13.4, 3.0, 9.0), 'gold')
    box(M, 'chest', (0, w[1] - 10.0, -6.6), (5.4, 5.4, 0.8), 'core')
    for k, sx in enumerate((-5.0, -1.7, 1.7, 5.0)):
        zero_bone(M, f'tas{k}', 'hips', (sx, hy - 1, -4.8))
        M.cube_l(f'tas{k}', (-1.6, 0, -0.7), (3.2, 11 - (k % 2), 1.4), 'plate')
        zero_bone(M, f'tasb{k}', 'hips', (sx, hy - 1, 4.6))
        M.cube_l(f'tasb{k}', (-1.6, 0, -0.7), (3.2, 12 - (k % 2), 1.4), 'plate_dark')
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        box(M, f'{s}_arm', (A[0] + sx * 2.2, A[1] - 1.0, A[2]), (10, 5.0, 10.6), 'plate')
        box(M, f'{s}_arm', (A[0] + sx * 4.4, A[1] + 2.4, A[2]), (5.6, 3.4, 9.4), 'plate_dark')
        for k in range(4):
            zero_bone(M, f'{s}_ps{k}', f'{s}_arm', (A[0] + sx * (2.0 + k * 1.8), A[1] - 3.2, A[2] + (k - 1.5) * 2.6))
            M.cube_l(f'{s}_ps{k}', (-1.0, -7.4 - k * 1.2, -1.0), (2.0, 7.4 + k * 1.2, 2.0), 'horn')
        Bp = K[f'{s}_B']; Cp = K['hands'][s]
        box(M, f'{s}_fore', ((Bp[0] + Cp[0]) / 2, (Bp[1] + Cp[1]) / 2, (Bp[2] + Cp[2]) / 2), (6.0, 7.0, 6.0), 'plate')
        kn = K[f'{s}_knee']
        box(M, f'{s}_leg', (kn[0], kn[1], kn[2] - 2.9), (6.2, 4.6, 3.8), 'gold')
        box(M, f'{s}_shin', (kn[0], kn[1] + 8.2, kn[2] - 1.4), (6.4, 9.0, 6.0), 'plate')
    # burning crown: 9 spikes + floating halo of embers
    zero_bone(M, 'crown', 'head', (0, hc[1] - 5.0, hc[2]))
    for k in range(9):
        a = (k - 4) * 0.42
        local(M, f'cr{k}', 'crown', (math.sin(a) * 4.8, -abs(a) * 0.7, -math.cos(a) * 1.0 + 0.8), (0, 0, math.degrees(a) * 0.5))
        h = 9 - abs(k - 4) * 1.0
        M.cube_l(f'cr{k}', (-1.0, -h, -1.0), (2.0, h, 2.0), 'gold')
        M.cube_l(f'cr{k}', (-0.7, -h - 2.4, -0.7), (1.4, 2.4, 1.4), 'core')
    box(M, 'head', (hc[0], hc[1] - 5.0, hc[2]), (11, 1.8, 11), 'gold')
    # horns
    for sx in (-1, 1):
        chain(M, f'horn{sx}', 'head', [(sx * 5.0, hc[1] - 3.0, hc[2]), (sx * 9.4, hc[1] - 5.0, hc[2] - 0.5), (sx * 12.0, hc[1] - 13.0, hc[2] - 1.0)], (3.8, 2.4), 'horn', ext=0.2)
    # spectral greatsword: huge blade of grey fire
    sword(M, 'right_hand', length=46, width=7.0, thick=1.8, mat='blade', guard=(14.0, 2.0, 2.8), guard_mat='gold', grip=6.0, grip_mat='grip', dir='fwd', offset=(0, 0, 0.8))
    box(M, 'right_hand', (0, 1.0, -13.0), (2.2, 9.0, 1.0), 'core')
    box(M, 'right_hand', (0, 1.0, -30.0), (2.2, 9.0, 1.0), 'core')
    # cape of ash: 7 long strips + shoulder mantle of drifting embers
    for k, (x, hgt) in enumerate(((-5.4, 40), (0, 48), (5.4, 40), (-9.6, 30), (9.6, 30), (-13, 18), (13, 18))):
        zero_bone(M, f'cape{k}', 'chest', (x, w[1] - 15.4 + abs(x) * 0.25, 5.4))
        M.cube_l(f'cape{k}', (-2.4, 0, -0.5), (4.8, hgt, 1.0), 'cape')
    # aura flames behind the shoulders
    for k, (ry, hgt, x) in enumerate(((0, 22, 0), (40, 16, 5), (-40, 16, -5))):
        world_bone(M, f'aura{k}', 'chest', (x, w[1] - 17, 4.4), -10, ry, 0)
        M.cube_l(f'aura{k}', (-3.4, -hgt, 0), (6.8, hgt, 0), 'flame', plane=True)

    Pt = Painter(M)
    Pt.mats['armor'] = mats.plates(ASH, rivets=True, band=5, glow_seam=EM)
    Pt.mats['armor_chest'] = mats.plates(ASH, rivets=False, band=6, glow_seam=EM)
    Pt.mats['plate'] = mats.plates(pal('#161618', '#2a2a30', '#43434e', '#64647a', '#8c8ca4'), rivets=True, band=6, glow_seam=EM)
    Pt.mats['plate_dark'] = mats.plates(pal('#0a0a0c', '#141418', '#202028', '#32323e'), rivets=False, band=7)
    Pt.mats['gauntlet'] = mats.plates(ASH, rivets=True, band=4, glow_seam=EM)
    Pt.mats['greave'] = mats.plates(ASH, rivets=True, band=6)
    Pt.mats['boot'] = mats.plates(pal('#0a0a0c', '#141418', '#202028', '#32323e'), rivets=False, band=4)
    Pt.mats['gold'] = mats.metal(GOLD, glow_edge=(255, 180, 60))
    Pt.mats['horn'] = mats.horn(pal('#0e0e12', '#26262e', '#58586a', '#9a9ab0'), 2, tip_glow=(255, 150, 60))
    Pt.mats['core'] = mats.flame((255, 244, 190), (255, 150, 40), (220, 70, 10), 0.3)
    Pt.mats['flame'] = mats.flame((255, 244, 190), (255, 150, 40), (220, 60, 10), 0.8)
    Pt.mats['blade'] = mats.metal(pal('#2a2a30', '#5a5a6a', '#a0a0b8', '#d8d8ec', '#ffffff'), brushed=False, glow_edge=(255, 170, 70))
    Pt.mats['grip'] = mats.cloth(pal('#0a0806', '#1c1410', '#2e2018'), 0.3)
    Pt.mats['cape'] = mats.ragged(mats.skin(pal('#0e0e12', '#1c1c22', '#2e2e38', '#46465a', '#686880'), 0.5, veins=EM, vgrad=0.5), depth=12)
    Pt.mats['face'] = mats.face_on(mats.bone(pal('#2a2a30', '#50505c', '#808092', '#b8b8cc'), 0.4), 'north', eyes=[(0.22, 0.38, 0.14, 0.13, 0.0, 'round')],
                                   nose=dict(x=0.5, y=0.6, w=0.07, h=0.1, col=(14, 14, 18)), mouth=dict(y=0.87, w=0.32, h=0.08, teeth=7, col=(255, 110, 30)), eye_col=(255, 244, 200), glow_col=(255, 170, 60))

    ba = dict(stride=24, shin=34, arm=10, elbow=8, bob=0.9, twist=7, lean=3, walk_len=1.15, run_len=0.8, run_mul=1.5)
    A = biped_set(M, ba, death='collapse', death_len=100 / 20)
    for nm in ('idle', 'walk', 'run'):
        for k in range(7):
            gen(A[nm], f'cape{k}', 'r', lambda ph, k=k: (4 * S(ph, 1, k * 0.15), 0, 3 * S(ph, 1, k * 0.2 + 0.3)), n=8)
        for k in range(3):
            gen(A[nm], f'aura{k}', 'r', lambda ph, k=k: (6 * S(ph, 1, k * 0.2), 0, 8 * S(ph, 1, k * 0.13)), n=8)
    def pz(**kw):
        return kw
    A['cleave'] = swing('CLEAVE', 1.9, [(0, {}), (0.5, {'right_arm': (-100, 0, -34), 'right_fore': (-30, 0, 0), 'chest': (-10, -38, 0), 'head': (-6, 14, 0), 'left_arm': (-30, 0, 26)}),
                                       (0.7, {'right_arm': (-92, 0, 24), 'right_fore': (-12, 0, 0), 'chest': (8, 40, 0), 'head': (4, -16, 0)}),
                                       (0.95, {'right_arm': (-95, 0, -30), 'right_fore': (-18, 0, 0), 'chest': (6, -34, 0)}),
                                       (1.1, {'right_arm': (-168, 0, -10), 'right_fore': (-30, 0, 0), 'chest': (-18, 0, 0), 'head': (-14, 0, 0)}),
                                       (1.3, {'right_arm': (-30, 0, 4), 'right_fore': (-8, 0, 0), 'chest': (38, 0, 0), 'head': (14, 0, 0), 'right_leg': (-26, 0, 0), 'left_leg': (18, 0, 0)}), (1.9, {})])
    A['plunge'] = swing('PLUNGE', 2.4, [(0, {}), (0.9, {'right_arm': (-178, 0, -10), 'left_arm': (-170, 0, 12), 'right_fore': (-20, 0, 0), 'chest': (-26, 0, 0), 'head': (-26, 0, 0)}),
                                       (1.2, {'right_arm': (-40, 0, -4), 'left_arm': (-40, 0, 4), 'chest': (46, 0, 0), 'head': (24, 0, 0), 'right_leg': (-28, 0, 0), 'left_leg': (-28, 0, 0), 'right_shin': (24, 0, 0), 'left_shin': (24, 0, 0)}),
                                       (1.9, {'right_arm': (-40, 0, -4), 'left_arm': (-40, 0, 4), 'chest': (40, 0, 0), 'head': (20, 0, 0)}), (2.4, {})])
    moves(A['plunge'], [(0, {}), (1.2, {'hips': (0, -3.5, 0)}), (1.9, {'hips': (0, -3.0, 0)}), (2.4, {})])
    A['ruinbeam'] = swing('RUINBEAM', 4.0, [(0, {}), (0.9, {'right_arm': (-90, 0, 0), 'right_fore': (-4, 0, 0), 'chest': (6, 0, 0), 'left_arm': (-60, 0, 40)}), (3.4, {'right_arm': (-92, 0, 0), 'chest': (8, 0, 0), 'left_arm': (-60, 0, 44)}), (4.0, {})])
    A['skyfall'] = swing('SKYFALL', 3.2, [(0, {}), (0.8, {'right_arm': (-170, 0, -20), 'left_arm': (-170, 0, 20), 'chest': (-20, 0, 0), 'head': (-30, 0, 0)}), (2.6, {'right_arm': (-176, 0, -20), 'left_arm': (-176, 0, 20), 'chest': (-22, 0, 0), 'head': (-34, 0, 0)}), (3.2, {})])
    moves(A['skyfall'], [(0, {}), (0.8, {'hips': (0, 3.0, 0)}), (2.6, {'hips': (0, 3.0, 0)}), (3.2, {})])
    A['court'] = swing('COURT', 2.8, [(0, {}), (0.7, {'right_arm': (-80, 0, -66), 'left_arm': (-80, 0, 66), 'chest': (-14, 0, 0), 'head': (-24, 0, 0)}), (1.8, {'right_arm': (-90, 0, -72), 'left_arm': (-90, 0, 72), 'chest': (-16, 0, 0), 'head': (-28, 0, 0)}), (2.8, {})])
    A['strike'] = swing('STRIKE', 1.5, [(0, {}), (0.3, {'chest': (-12, 0, 0), 'right_arm': (-30, 0, -40), 'left_arm': (-30, 0, 40), 'head': (-12, 0, 0)}), (0.5, {}), (0.8, {'right_arm': (-130, 0, -6), 'chest': (20, 10, 0), 'head': (8, 0, 0)}), (1.0, {'right_arm': (-40, 0, 10), 'chest': (28, -10, 0)}), (1.5, {})])
    scales(A['strike'], [(0, {'base': (1, 1, 1)}), (0.28, {'base': (0.2, 1.1, 0.2)}), (0.42, {'base': (1, 1, 1)})])
    A['aegis'] = swing('AEGIS', 3.5, [(0, {}), (0.5, {'right_arm': (-100, 0, -50), 'left_arm': (-100, 0, 50), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (8, 0, 0), 'head': (8, 0, 0)}), (3.0, {'right_arm': (-104, 0, -54), 'left_arm': (-104, 0, 54), 'chest': (10, 0, 0)}), (3.5, {})])
    A['devour'] = swing('DEVOUR', 4.0, [(0, {}), (0.7, {'right_arm': (-100, 0, -16), 'left_arm': (-100, 0, 16), 'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'chest': (10, 0, 0), 'head': (-10, 0, 0)}), (3.6, {'right_arm': (-104, 0, -20), 'left_arm': (-104, 0, 20), 'chest': (12, 0, 0), 'head': (-12, 0, 0)}), (4.0, {})])
    A['cataclysm'] = swing('CATACLYSM', 2.7, [(0, {}), (0.9, {'right_arm': (-176, 0, -30), 'left_arm': (-176, 0, 30), 'chest': (-26, 0, 0), 'head': (-30, 0, 0)}), (1.2, {'right_arm': (-30, 0, -30), 'left_arm': (-30, 0, 30), 'chest': (44, 0, 0), 'head': (20, 0, 0)}), (2.7, {})])
    moves(A['cataclysm'], [(0, {}), (1.2, {'hips': (0, -4.0, 0)}), (2.7, {})])
    A['chains'] = swing('CHAINS', 2.0, [(0, {}), (0.5, {'left_arm': (-100, 0, 40), 'left_fore': (-20, 0, 0), 'chest': (-8, 22, 0), 'head': (-8, -12, 0)}), (0.8, {'left_arm': (-100, 0, 0), 'left_fore': (-6, 0, 0), 'chest': (20, -18, 0), 'head': (8, 10, 0)}), (1.4, {'left_arm': (-70, 0, 0), 'chest': (14, -8, 0)}), (2.0, {})])
    return M, Pt, A
