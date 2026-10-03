"""Kor Kraliçesi İgnara (Ember Queen) - winged queen of the Ember Dominion in molten armour with a flaming sword."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'ember_queen'
SPEC = dict(
    faction='EMBER', tr='Kor Kraliçesi İgnara', en='Ignara, the Ember Queen', hp=480, armor=10, attack=15.0, speed=0.29, size=(1.3, 3.5, 1.0), reach=3.8, kb=0.9, follow=56,
    voice=('demon', 0.8), egg=(0x2A0E08, 0xFFB030), spawn=(0, 1, 1, []), xp=500, death_ticks=70, anim=(1.3, 1.5), boss=('RED', True),
    abilities=[
        dict(kind='MELEE', anim='attack', time=(34, 12, 20, 28), cd=26, dmg=0.75, p=(0.9, 0), color=0xFF7A1A, fx='flame'),
        dict(kind='CONE', anim='breath', time=(50, 14, 30), cd=190, w=3, range=(0, 13), dmg=0.5, p=(30, 0), color=0xFF7A1A, fx='flame'),
        dict(kind='METEOR', anim='meteor', time=(60, 18, 22, 26, 30, 34, 38, 42), cd=300, w=3, range=(4, 28), dmg=1.1, p=(3.0, 8), color=0xFF5A10, fx='flame'),
        dict(kind='ERUPT', anim='pillars', time=(44, 22, 28), cd=210, w=3, range=(2, 18), dmg=1.0, p=(1, 8), color=0xFF7A1A, fx='flame'),
        dict(kind='SUMMON', anim='summon', time=(50, 26), cd=480, w=2, range=(0, 30), p=(3, 6), summon='cinder_imp', color=0xFF8A20, fx='flame'),
        dict(kind='LEAP', anim='leap', time=(36, 14), cd=170, w=3, range=(5, 18), dmg=1.3, p=(1.5, 0.8), color=0xFF7A1A, fx='flame', eff=('minecraft:slowness', 50, 1)),
        dict(kind='BEAM', anim='beam', time=(70, 24, 40), cd=300, w=3, range=(4, 28), dmg=0.7, p=(40, 0), color=0xFFE090, fx='flame', los=True, p2=True),
        dict(kind='BURST', anim='nova', time=(46, 30), cd=260, w=3, range=(0, 9), dmg=1.2, p=(9, 1.4), color=0xFFA030, fx='flame', p2=True),
    ],
    drops=[('krolasyonbosses:queen_heart', 1, 1, 1.0), ('krolasyonbosses:ember_sigil', 1, 1, 1.0), ('krolasyonbosses:ember_shard', 6, 12, 1.0), ('krolasyonbosses:hellsteel_ingot', 2, 5, 1.0), ('minecraft:experience_bottle', 4, 8, 1.0)],
    lore='Kor Hanedanı\'nın ateşten tahtında oturan kraliçe. Savaşı bir onur borcu sayar; sözünü tutanın dostu, hile yapanın külüdür.',
)

DARK = pal('#0e0a0a', '#1c1412', '#2e211c', '#46342a', '#68503e')
GOLD = pal('#5a4010', '#8f6a18', '#d8a828', '#ffe070')
SKIN = pal('#1e1210', '#3a2218', '#5e3624', '#8a5238', '#b07050')
EM = (255, 120, 24)


def build():
    M = Model(ID, 256, 256)
    K = humanoid(M, leg=(13, 13), leg_w=4.6, foot=(5.6, 3.2, 8.4), hip_w=10.5, pelvis_h=5.2, torso=(11.4, 13.5, 7.4), neck=2.6, head=(8.2, 8.8, 8.2),
                 arm=(13.5, 12.5), arm_w=4.0, hand=(3.8, 4.4, 3.8), hunch=3, arm_out=0.8, bend=(0.7, 0.2), head_fwd=0.8,
                 mats=dict(pelvis='armor', torso='armor_chest', head='face', arm='armor', fore='gauntlet', hand='skin', leg='greave', shin='greave', foot='boot'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    # breastplate + collar + belt + tassets
    box(M, 'chest', (0, w[1] - 8.2, -4.2), (12.8, 10.5, 2.6), 'plate')
    box(M, 'chest', (0, w[1] - 8.0, 4.0), (11.6, 11.0, 1.8), 'plate_dark')
    box(M, 'chest', (0, w[1] - 14.2, -1.0), (11.4, 2.4, 8.6), 'gold')
    box(M, 'chest', (0, w[1] - 2.6, -0.2), (12.0, 2.2, 8.4), 'gold')
    box(M, 'chest', (0, w[1] - 8.8, -5.6), (4.6, 4.6, 0.8), 'core')   # glowing sigil
    for k, sx in enumerate((-4.2, -1.4, 1.4, 4.2)):
        zero_bone(M, f'tas{k}', 'hips', (sx, hy - 1, -4.2))
        M.cube_l(f'tas{k}', (-1.4, 0, -0.6), (2.8, 9.5 - (k % 2), 1.2), 'plate')
        zero_bone(M, f'tasb{k}', 'hips', (sx, hy - 1, 4.0))
        M.cube_l(f'tasb{k}', (-1.4, 0, -0.6), (2.8, 10.5 - (k % 2), 1.2), 'plate_dark')
    # pauldrons with horns
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        box(M, f'{s}_arm', (A[0] + sx * 1.8, A[1] - 0.8, A[2]), (8.4, 4.2, 9.0), 'plate')
        box(M, f'{s}_arm', (A[0] + sx * 3.6, A[1] + 2.0, A[2]), (5.0, 3.0, 8.0), 'plate_dark')
        for k in range(3):
            zero_bone(M, f'{s}_pspike{k}', f'{s}_arm', (A[0] + sx * (2.0 + k * 1.6), A[1] - 2.5, A[2] + (k - 1) * 2.4))
            M.cube_l(f'{s}_pspike{k}', (-0.9, -6.0 - k, -0.9), (1.8, 6.0 + k, 1.8), 'horn')
        Bp = K[f'{s}_B']; Cp = K['hands'][s]
        box(M, f'{s}_fore', ((Bp[0] + Cp[0]) / 2, (Bp[1] + Cp[1]) / 2, (Bp[2] + Cp[2]) / 2), (5.4, 6.4, 5.4), 'plate')
        kn = K[f'{s}_knee']
        box(M, f'{s}_leg', (kn[0], kn[1], kn[2] - 2.6), (5.4, 4.2, 3.4), 'gold')
        box(M, f'{s}_shin', (kn[0], kn[1] + 7.5, kn[2] - 1.2), (5.8, 8.0, 5.4), 'plate')
    # crown of fire + hair flames
    zero_bone(M, 'crown', 'head', (0, hc[1] - 4.2, hc[2]))
    for k in range(5):
        a = (k - 2) * 0.55
        local(M, f'cr{k}', 'crown', (math.sin(a) * 3.8, -abs(a) * 0.9, -math.cos(a) * 1.2 + 1.0), (0, 0, math.degrees(a) * 0.6))
        M.cube_l(f'cr{k}', (-1.0, -7.0 + abs(k - 2), -1.0), (2.0, 7.0 - abs(k - 2), 2.0), 'gold')
    for k, (ry, hgt) in enumerate(((20, 16), (-20, 16), (0, 20), (50, 12), (-50, 12))):
        world_bone(M, f'hair{k}', 'head', (0, hc[1] - 1.5, hc[2] + 3.4), -18, ry, 0)
        M.cube_l(f'hair{k}', (-2.4, -hgt * 0.2, 0), (4.8, hgt, 0), 'flame', plane=True)
    # molten greatsword (right hand)
    sword(M, 'right_hand', length=34, width=5.2, thick=1.4, mat='blade', guard=(10.0, 1.6, 2.2), guard_mat='gold', grip=4.6, grip_mat='grip', dir='fwd', offset=(0, 0, 0.8))
    box(M, 'right_hand', (0, 1.0, -9.4), (1.8, 7.0, 1.0), 'fuller')
    # ember orb floating over the left palm
    zero_bone(M, 'orb', 'left_hand', K['hands']['left'])
    M.cube_l('orb', (-2.2, -9.0, -2.2), (4.4, 4.4, 4.4), 'core')
    M.cube_l('orb', (-3.4, -10.2, -3.4), (6.8, 6.8, 6.8), 'orb_halo')
    # cape of cinders
    for k, (x, hgt) in enumerate(((-4.0, 30), (0, 36), (4.0, 30), (-7.0, 20), (7.0, 20))):
        zero_bone(M, f'cape{k}', 'chest', (x, w[1] - 13.0 + abs(x) * 0.15, 4.8))
        M.cube_l(f'cape{k}', (-1.9, 0, -0.5), (3.8, hgt, 0.9), 'cape')
    # wings
    wings(M, 'chest', (4.6, w[1] - 11, 5.0), ((16, w[1] - 26, 8), (30, w[1] - 22, 10), (46, w[1] - 8, 14)), (15, 22, 22), 'bonewing', ('w0', 'w1', 'w2'), widths=(3.2, 2.6, 2.0), cov_mat='w0')

    Pt = Painter(M)
    Pt.mats['armor'] = mats.plates(DARK, rivets=True, band=5, glow_seam=EM)
    Pt.mats['armor_chest'] = mats.plates(DARK, rivets=False, band=6, glow_seam=EM)
    Pt.mats['plate'] = mats.plates(pal('#14100e', '#261c18', '#3e2e26', '#5e463a', '#86675a'), rivets=True, band=6, glow_seam=EM)
    Pt.mats['plate_dark'] = mats.plates(pal('#0a0807', '#16100e', '#241a16', '#382a22'), rivets=False, band=7)
    Pt.mats['gauntlet'] = mats.plates(DARK, rivets=True, band=4, glow_seam=EM)
    Pt.mats['greave'] = mats.plates(DARK, rivets=True, band=6)
    Pt.mats['gold'] = mats.metal(GOLD, glow_edge=(255, 180, 60))
    Pt.mats['boot'] = mats.plates(pal('#0a0807', '#16100e', '#241a16', '#382a22'), rivets=False, band=4)
    Pt.mats['skin'] = mats.skin(SKIN, 0.3, veins=EM)
    Pt.mats['face'] = mats.face_on(mats.skin(SKIN, 0.25, veins=EM), 'north', eyes=[(0.22, 0.4, 0.14, 0.09, 0.3, 'almond')], mouth=dict(y=0.8, w=0.2, h=0.05, col=(255, 90, 30), teeth=0, curve=0.3),
                                   brow=dict(y=0.28, w=0.2, th=0.05, angle=0.35, col=(10, 4, 4)), eye_col=(255, 244, 200), glow_col=(255, 200, 80))
    Pt.mats['horn'] = mats.horn(pal('#0e0a0a', '#2e211c', '#8a5a2a', '#e0a040'), 2, tip_glow=(255, 170, 60))
    Pt.mats['core'] = mats.flame((255, 244, 190), (255, 150, 40), (220, 70, 10), 0.3)
    Pt.mats['orb_halo'] = mats.crystal(pal('#6a1c06', '#c8480c', '#ff8a1c', '#ffd060'), (255, 150, 40), 3)
    Pt.mats['flame'] = mats.flame((255, 244, 190), (255, 150, 40), (220, 60, 10), 0.8)
    Pt.mats['blade'] = mats.metal(pal('#3a0a04', '#a8300a', '#ff7a14', '#ffc040', '#fff0b0'), brushed=False, glow_edge=(255, 200, 80))
    Pt.mats['fuller'] = mats.solid((255, 230, 150))
    Pt.mats['grip'] = mats.cloth(pal('#0a0806', '#1c1410', '#2e2018'), 0.3)
    Pt.mats['cape'] = mats.ragged(mats.skin(pal('#14080a', '#2c100c', '#58200c', '#a03a10', '#e86a1a'), 0.5, veins=EM, vgrad=0.5), depth=10)
    Pt.mats['bonewing'] = mats.horn(pal('#0e0a0a', '#2e211c', '#5a4030', '#8a6850'), 4)
    Pt.mats['w0'] = mats.feather_sheet(pal('#1c1412', '#342622', '#523c32', '#7a5a4a'), tip=(255, 120, 30), glow_tip=(255, 110, 20), rows=3, fw=4, tip_depth=7, tip_start=0.55)
    Pt.mats['w1'] = mats.feather_sheet(pal('#1c1412', '#342622', '#523c32', '#7a5a4a'), tip=(255, 130, 40), glow_tip=(255, 110, 20), rows=4, fw=4, tip_depth=9, tip_start=0.5)
    Pt.mats['w2'] = mats.feather_sheet(pal('#1c1412', '#342622', '#523c32', '#7a5a4a'), tip=(255, 150, 50), glow_tip=(255, 120, 30), rows=4, fw=4, tip_depth=9, tip_start=0.5)

    ba = dict(stride=26, shin=36, arm=10, elbow=8, bob=0.9, twist=7, lean=3, walk_len=1.1, run_len=0.75, run_mul=1.5)
    A = biped_set(M, ba, death='collapse', death_len=70 / 20)
    for nm in ('idle', 'walk', 'run'):
        add_wing_flap(A[nm], amp={'idle': 0.35, 'walk': 0.6, 'run': 0.9}[nm], speed=1, base=8)
        for k in range(5):
            gen(A[nm], f'cape{k}', 'r', lambda ph, k=k: (4 * S(ph, 1, k * 0.17), 0, 3 * S(ph, 1, k * 0.2 + 0.3)), n=8)
        for k in range(5):
            gen(A[nm], f'hair{k}', 'r', lambda ph, k=k: (6 * S(ph, 1, k * 0.2), 0, 8 * S(ph, 1, k * 0.13)), n=8)
    sw = {'right_arm': (-70, 0, -10), 'right_fore': (-50, 0, 0)}
    A['attack'] = swing('ATTACK', 1.7, [(0, {}),
        (0.45, {'right_arm': (-100, 0, -30), 'right_fore': (-30, 0, 0), 'chest': (-8, -34, 0), 'head': (-4, 14, 0), 'left_arm': (-30, 0, 24)}),
        (0.6, {'right_arm': (-90, 0, 20), 'right_fore': (-14, 0, 0), 'chest': (6, 36, 0), 'head': (4, -16, 0)}),
        (0.8, {'right_arm': (-95, 0, 10), 'right_fore': (-20, 0, 0), 'chest': (4, 30, 0)}),
        (1.0, {'right_arm': (-90, 0, -24), 'right_fore': (-16, 0, 0), 'chest': (6, -34, 0), 'head': (4, 14, 0)}),
        (1.15, {'right_arm': (-160, 0, -10), 'right_fore': (-30, 0, 0), 'chest': (-16, 0, 0), 'head': (-12, 0, 0)}),
        (1.3, {'right_arm': (-30, 0, 4), 'right_fore': (-8, 0, 0), 'chest': (34, 0, 0), 'head': (14, 0, 0), 'right_leg': (-22, 0, 0), 'left_leg': (16, 0, 0)}),
        (1.7, {})])
    A['breath'] = swing('BREATH', 2.5, [(0, {}), (0.7, {'chest': (-18, 0, 0), 'head': (-40, 0, 0), 'right_arm': (-30, 0, -50), 'left_arm': (-30, 0, 50), 'right_wing0': (0, 0, -20), 'left_wing0': (0, 0, 20)}),
                                       (0.9, {'chest': (10, 0, 0), 'head': (6, 0, 0), 'right_arm': (-60, 0, -40), 'left_arm': (-60, 0, 40)}), (1.8, {'chest': (10, 14, 0), 'head': (6, 0, 0), 'right_arm': (-60, 0, -40), 'left_arm': (-60, 0, 40)}), (2.5, {})])
    A['meteor'] = swing('METEOR', 3.0, [(0, {}), (0.7, {'right_arm': (-170, 0, -20), 'left_arm': (-170, 0, 20), 'chest': (-20, 0, 0), 'head': (-30, 0, 0), 'right_wing0': (0, 0, -30), 'left_wing0': (0, 0, 30)}),
                                       (2.4, {'right_arm': (-176, 0, -20), 'left_arm': (-176, 0, 20), 'chest': (-22, 0, 0), 'head': (-34, 0, 0)}), (3.0, {})])
    moves(A['meteor'], [(0, {}), (0.7, {'hips': (0, 3.0, 0)}), (2.4, {'hips': (0, 3.0, 0)}), (3.0, {})])
    A['pillars'] = swing('PILLARS', 2.2, [(0, {}), (0.7, {'right_arm': (-170, 0, -8), 'right_fore': (-30, 0, 0), 'chest': (-22, 0, 0), 'head': (-20, 0, 0)}),
                                         (0.9, {'right_arm': (-40, 0, 0), 'right_fore': (-8, 0, 0), 'chest': (40, 0, 0), 'head': (20, 0, 0), 'right_leg': (-34, 0, 0), 'left_leg': (26, 0, 0), 'left_shin': (40, 0, 0)}), (1.7, {'right_arm': (-40, 0, 0), 'chest': (36, 0, 0), 'head': (18, 0, 0), 'right_leg': (-34, 0, 0), 'left_leg': (26, 0, 0), 'left_shin': (40, 0, 0)}), (2.2, {})])
    A['summon'] = swing('SUMMON', 2.5, [(0, {}), (0.6, {'right_arm': (-80, 0, -60), 'left_arm': (-80, 0, 60), 'chest': (-14, 0, 0), 'head': (-24, 0, 0), 'right_wing0': (0, 0, -30), 'left_wing0': (0, 0, 30)}), (1.6, {'right_arm': (-90, 0, -66), 'left_arm': (-90, 0, 66), 'chest': (-16, 0, 0), 'head': (-28, 0, 0)}), (2.5, {})])
    A['leap'] = swing('LEAP', 1.8, [(0, {}), (0.5, {'chest': (24, 0, 0), 'right_leg': (-50, 0, 0), 'left_leg': (-50, 0, 0), 'right_shin': (80, 0, 0), 'left_shin': (80, 0, 0), 'right_arm': (-40, 0, -20), 'left_arm': (-40, 0, 20)}),
                                   (0.7, {'chest': (-10, 0, 0), 'right_leg': (20, 0, 0), 'left_leg': (20, 0, 0), 'right_wing0': (0, 0, 50), 'left_wing0': (0, 0, -50), 'right_arm': (-150, 0, -10)}), (1.2, {'chest': (30, 0, 0), 'right_arm': (-40, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (-30, 0, 0)}), (1.8, {})])
    moves(A['leap'], [(0, {}), (0.5, {'hips': (0, -5, 0)}), (0.8, {'hips': (0, 5, 0)}), (1.8, {})])
    A['beam'] = swing('BEAM', 3.5, [(0, {}), (0.8, {'right_arm': (-90, 0, 0), 'right_fore': (-4, 0, 0), 'chest': (6, 0, 0), 'left_arm': (-60, 0, 40), 'right_wing0': (0, 0, -34), 'left_wing0': (0, 0, 34)}), (3.0, {'right_arm': (-92, 0, 0), 'chest': (8, 0, 0), 'left_arm': (-60, 0, 44)}), (3.5, {})])
    A['nova'] = swing('NOVA', 2.3, [(0, {}), (0.7, {'right_arm': (-40, 0, 30), 'left_arm': (-40, 0, -30), 'chest': (20, 0, 0), 'head': (20, 0, 0), 'right_wing0': (0, 0, 30), 'left_wing0': (0, 0, -30)}),
                                   (1.0, {'right_arm': (-100, 0, -70), 'left_arm': (-100, 0, 70), 'chest': (-24, 0, 0), 'head': (-30, 0, 0), 'right_wing0': (0, 0, -50), 'left_wing0': (0, 0, 50)}), (1.6, {'right_arm': (-100, 0, -70), 'left_arm': (-100, 0, 70), 'chest': (-24, 0, 0), 'head': (-30, 0, 0)}), (2.3, {})])
    return M, Pt, A
