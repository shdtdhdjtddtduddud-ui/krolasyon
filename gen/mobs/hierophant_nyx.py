"""Başrahip Nyx (Hierophant Nyx) - floating archmage of the Shadow Covenant with a rune halo and a void orb."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'hierophant_nyx'
SPEC = dict(
    faction='SHADOW', tr='Başrahip Nyx', en='Nyx, the Hierophant', hp=420, armor=6, attack=13.0, speed=0.24, size=(1.2, 3.3, 0.85), fly=1.4, reach=4.0, kb=0.9, follow=60, head='head',
    voice=('revenge', 0.85), egg=(0x14082A, 0xC8A0FF), spawn=(0, 1, 1, []), xp=500, death_ticks=70, anim=(1.5, 1.6), boss=('PURPLE', True),
    abilities=[
        dict(kind='BOLT', anim='orb', time=(26, 14), cd=26, dmg=1.0, range=(4, 26), p=(1.5, 1.6), color=0x8A5CFF, fx='void', los=True, eff=('minecraft:blindness', 50, 0)),
        dict(kind='BEAM', anim='voidbeam', time=(70, 22, 40), cd=260, w=3, range=(5, 30), dmg=0.7, p=(40, 0), color=0xC8A0FF, fx='void', los=True, eff=('minecraft:wither', 60, 1)),
        dict(kind='PULL', anim='blackhole', time=(44, 22), cd=170, w=3, range=(4, 20), dmg=1.0, p=(2.4, 0), color=0x5A2AA0, fx='void', eff=('minecraft:slowness', 80, 2), los=True),
        dict(kind='CLOUD', anim='darkness', time=(40, 20), cd=220, w=3, range=(3, 20), dmg=0, p=(5.0, 200), color=0x1A0A30, fx='void', eff=('minecraft:blindness', 100, 0)),
        dict(kind='SUMMON', anim='summon', time=(56, 28), cd=470, w=2, range=(0, 30), p=(2, 4), summon='shade_stalker', color=0x8A5CFF, fx='void'),
        dict(kind='SUMMON', anim='summon', time=(56, 28), cd=520, w=2, range=(0, 30), p=(1, 3), summon='mirror_phantom', color=0xCFE8FF, fx='rod'),
        dict(kind='BLINK_AWAY', anim='fade', time=(18, 7), cd=150, w=3, range=(0, 8), p=(14, 0), color=0x7040C0, fx='void'),
        dict(kind='METEOR', anim='voidrain', time=(64, 20, 24, 28, 32, 36, 40), cd=300, w=3, range=(4, 30), dmg=1.1, p=(3.0, 8), color=0x8A5CFF, fx='void', p2=True),
        dict(kind='BURST', anim='nova', time=(46, 30), cd=270, w=3, range=(0, 10), dmg=1.2, p=(10, 1.4), color=0xC8A0FF, fx='void', eff=('minecraft:blindness', 60, 0), p2=True),
    ],
    drops=[('krolasyonbosses:nyx_eye', 1, 1, 1.0), ('krolasyonbosses:shadow_sigil', 1, 1, 1.0), ('krolasyonbosses:shadow_essence', 6, 12, 1.0), ('krolasyonbosses:hellsteel_ingot', 2, 5, 1.0), ('minecraft:experience_bottle', 4, 8, 1.0)],
    lore='Gölge Tarikatı\'nın kör bilgesi. Boşluğa bakmayı öğrenmiş, boşluktan geri bakılmayı kabul etmiş; fısıltısı kapıları açar.',
)

ROBE = pal('#07040f', '#120a24', '#201440', '#34246a', '#50408f')
PALE = pal('#3a3248', '#6a5e84', '#9c90b8', '#cfc4e4')
GOLD = pal('#3a2a50', '#6a4aa0', '#a888f0', '#e0d0ff')
GLOW = (170, 120, 255)


def Y(h):
    """height above the robe bottom -> model y (the robe hovers one block above the ground)"""
    return 8.0 - h


def build():
    M = Model(ID, 256, 256)
    M.bone('base', None, (0, 24, 0))
    M.bone('hips', 'base', (0, Y(20), 0))
    # layered floating robe: 5 stacked tapering boxes + ragged hem strips (no legs)
    for i, (y, w, d) in enumerate(((50, 15, 10), (42, 13, 9), (34, 12, 8), (26, 11, 7.4), (18, 9, 6.4))):
        pass
    zero_bone(M, 'chest', 'hips', (0, Y(26), 0))
    box(M, 'chest', (0, Y(36), 0), (13, 16, 8.4), 'robe')          # torso
    box(M, 'hips', (0, Y(21), 0), (11.4, 12, 7.8), 'robe')
    box(M, 'hips', (0, Y(11), 0), (9.2, 10, 6.6), 'robe_hem')
    for k in range(8):
        a = k * math.tau / 8
        zero_bone(M, f'rag{k}', 'hips', (math.cos(a) * 3.6, Y(6), math.sin(a) * 3.0))
        M.cube_l(f'rag{k}', (-1.5, 0, -0.4), (3.0, 9 + (k % 3) * 3, 0.8), 'rag')
    box(M, 'chest', (0, Y(28.5), -4.6), (14.0, 2.6, 1.4), 'gold')    # sash
    box(M, 'chest', (0, Y(44.2), 0), (15.6, 3.0, 10.0), 'mantle')     # mantle / collar
    box(M, 'chest', (0, Y(45.6), 0), (11.0, 2.2, 8.0), 'gold')
    # head with hood
    zero_bone(M, 'neck', 'chest', (0, Y(44), 0))
    zero_bone(M, 'head', 'neck', (0, Y(44), -0.5))
    box(M, 'head', (0, Y(40.0), -0.5), (8.2, 8.6, 8.2), 'face')
    box(M, 'head', (0, Y(44.6), -0.4), (10.4, 3.4, 10.0), 'hood')
    box(M, 'head', (0, Y(40.4), 4.4), (10.4, 12.0, 2.4), 'hood')
    for sx in (-1, 1):
        box(M, 'head', (sx * 4.8, Y(40.6), 0.4), (1.8, 10.4, 8.8), 'hood')
    box(M, 'head', (0, Y(45.6), -4.0), (7.0, 1.4, 3.6), 'hood')
    # rune halo (vertical ring behind the head)
    zero_bone(M, 'halo', 'head', (0, Y(40.0), 6.6))
    for k in range(18):
        a = k * math.tau / 18
        M.cube_l('halo', (math.cos(a) * 12.0 - 1.9, math.sin(a) * 12.0 - 1.9, -0.9), (3.8, 3.8, 1.8), 'rune' if k % 3 else 'rune2')
    # long sleeves and hands
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = (sx * 9.0, Y(41.0), 0)
        B = (sx * 12.0, Y(35.0), -3.0)
        C = (sx * 12.4, Y(28.0), -8.0)
        M.seg(f'{s}_arm', 'chest', A, B, 4.4, 4.4, 'robe', extend=0.6)
        M.seg(f'{s}_fore', f'{s}_arm', B, C, 3.8, 3.8, 'robe', extend=0.4)
        zero_bone(M, f'{s}_hand', f'{s}_fore', C)
        M.cube_l(f'{s}_hand', (-1.6, -0.2, -1.6), (3.2, 3.8, 3.2), 'pale')
        for k in range(4):
            M.cube_l(f'{s}_hand', (-1.5 + k * 1.0, 3.4, -1.0 + (k % 2) * 0.5), (0.7, 3.6, 0.7), 'pale')
        box(M, f'{s}_fore', ((B[0] + C[0]) / 2 + sx * 0.8, (B[1] + C[1]) / 2 + 2, (B[2] + C[2]) / 2), (7.6, 8.4, 7.2), 'sleeve')
        box(M, f'{s}_arm', (A[0] + sx * 1.4, A[1] - 0.4, A[2]), (6.6, 3.0, 7.4), 'gold')
    # void orb between the hands (centre in front of the chest)
    zero_bone(M, 'orb', 'chest', (0, Y(28), -12))
    M.cube_l('orb', (-3.0, -3.0, -3.0), (6.0, 6.0, 6.0), 'orb')
    M.cube_l('orb', (-4.6, -4.6, -4.6), (9.2, 9.2, 9.2), 'orb_halo')
    # orbiting rune stones
    for k in range(6):
        zero_bone(M, f'orbit{k}', 'chest', (0, Y(30 + (k % 3) * 5), 0))
        M.cube_l(f'orbit{k}', (15.0 - 1.4, -1.8, -1.4), (2.8, 3.6, 2.8), 'rune')
    # star-cloak: hanging planes with a starfield
    for k, (x, hgt) in enumerate(((-5.5, 30), (0, 36), (5.5, 30))):
        zero_bone(M, f'cloak{k}', 'chest', (x, Y(44), 5.4))
        M.cube_l(f'cloak{k}', (-2.8, 0, -0.5), (5.6, hgt, 1.0), 'starcloak')

    Pt = Painter(M)
    Pt.mats['robe'] = mats.cloth(ROBE, 1.0, trim=(120, 80, 220))
    Pt.mats['robe_hem'] = mats.cloth(ROBE, 1.1, hem=(120, 80, 220), stripe=(120, 80, 220))
    Pt.mats['rag'] = mats.ragged(mats.cloth(ROBE, 1.0, hem=(120, 80, 220)), depth=6)
    Pt.mats['sleeve'] = mats.cloth(ROBE, 0.8, hem=(150, 110, 255))
    Pt.mats['mantle'] = mats.cloth(pal('#0a0618', '#1c1238', '#34246a', '#5a46a8'), 0.7, hem=(150, 110, 255))
    Pt.mats['gold'] = mats.metal(GOLD, glow_edge=(170, 120, 255))
    Pt.mats['hood'] = mats.cloth(pal('#04020a', '#0c0618', '#180e30', '#2a1c50'), 0.9)
    Pt.mats['pale'] = mats.skin(PALE, 0.25)
    Pt.mats['face'] = mats.face_on(mats.skin(PALE, 0.2, vgrad=0.1), 'north', eyes=[(0.2, 0.4, 0.12, 0.1, 0.1, 'almond')], eye_col=(230, 210, 255), glow_col=GLOW,
                                   mouth=dict(y=0.8, w=0.16, h=0.04, col=(60, 40, 100), teeth=0))
    Pt.mats['rune'] = mats.solid((160, 110, 255))
    Pt.mats['rune2'] = mats.solid((230, 210, 255))
    Pt.mats['orb'] = mats.solid((20, 6, 40), glow=False)
    Pt.mats['orb_halo'] = mats.crystal(pal('#1c0a40', '#3c1c88', '#6a3cd0', '#a07aff'), (170, 120, 255), 3)
    def starcloak(c, face, W, H, r):
        base = mats.ragged(mats.cloth(pal('#030208', '#080414', '#100a24', '#1c1240'), 1.0, trim=(120, 80, 220)), depth=8)
        col, a, g = base(c, face, W, H, r)
        col = col.copy(); g = np.zeros((H, W, 4), np.float32)
        for _ in range(max(2, W * H // 60)):
            x, y = int(r.random() * W), int(r.random() * H)
            col[y, x] = (240, 230, 255); g[y, x] = (240, 230, 255, 255)
        return col, a, g
    Pt.mats['starcloak'] = starcloak

    A = {}
    for name, L, amp in (('IDLE', 3.2, 0.6), ('WALK', 1.8, 1.0)):
        a = Anim(name, L, True)
        gen(a, 'hips', 'p', lambda ph: (0, 1.6 * S(ph), 0), n=10)
        gen(a, 'hips', 'r', lambda ph, amp=amp: (3 * amp * S(ph, 2, 0.1) + (8 if name == 'WALK' else 0), 4 * S(ph, 1, 0.5), 2 * S(ph, 1, 0.2)), n=10)
        gen(a, 'head', 'r', lambda ph: (2 * S(ph, 2, 0.2), 5 * S(ph, 1, 0.25), 1.5 * S(ph, 1, 0.4)), n=10)
        for sx, s in ((-1, 'right'), (1, 'left')):
            gen(a, f'{s}_arm', 'r', lambda ph, sx=sx: (-30 + 6 * S(ph, 1, 0.25 * sx), 0, sx * (8 + 5 * S(ph, 1, 0.1))), n=10)
            gen(a, f'{s}_fore', 'r', lambda ph: (-30 + 8 * S(ph, 1, 0.45), 0, 0), n=10)
        for k in range(8):
            gen(a, f'rag{k}', 'r', lambda ph, k=k, amp=amp: (10 * S(ph, 1, k * 0.13), 0, 12 * S(ph, 1, k * 0.17 + 0.3) * (0.5 + amp * 0.5)), n=10)
        for k in range(3):
            gen(a, f'cloak{k}', 'r', lambda ph, k=k: (5 * S(ph, 1, k * 0.2), 0, 4 * S(ph, 1, k * 0.15)), n=10)
        for k in range(6):
            gen(a, f'orbit{k}', 'r', lambda ph, k=k: (0, 360 * ph + k * 60, 0), n=12)
        gen(a, 'halo', 'r', lambda ph: (0, 0, 360 * ph), n=12) if False else None
        gen(a, 'orb', 'r', lambda ph: (0, 360 * ph * 2, 0), n=12)
        A[name.lower()] = a
    d = Anim('DEATH', 70 / 20, False)
    L = d.length
    poses(d, [(0, {}), (L * 0.35, {'hips': (-14, 0, 8), 'head': (-24, 0, 0), 'right_arm': (-80, 0, -50), 'left_arm': (-80, 0, 50)}), (L * 0.7, {'hips': (-30, 0, 18), 'head': (-40, 0, 0), 'right_arm': (-130, 0, -60), 'left_arm': (-130, 0, 60)}), (L, {'hips': (-40, 0, 25), 'head': (-44, 0, 0), 'right_arm': (-150, 0, -70), 'left_arm': (-150, 0, 70)})])
    moves(d, [(0, {}), (L * 0.5, {'base': (0, -6, 0)}), (L, {'base': (0, -22, 0)})])
    scales(d, [(0, {'base': (1, 1, 1)}), (L * 0.8, {'base': (1, 1, 1)}), (L, {'base': (1.0, 0.7, 1.0)})])
    A['death'] = d
    def arms(x, z, fx=0):
        return {'right_arm': (x, 0, -z), 'left_arm': (x, 0, z), 'right_fore': (fx, 0, 0), 'left_fore': (fx, 0, 0)}
    A['orb'] = swing('ORB', 1.3, [(0, {}), (0.3, {**arms(-60, 20, -40), 'hips': (-8, 0, 0), 'head': (-10, 0, 0)}), (0.55, {**arms(-110, 6, -10), 'hips': (10, 0, 0), 'head': (8, 0, 0)}), (1.3, {})])
    scales(A['orb'], [(0, {'orb': (1, 1, 1)}), (0.3, {'orb': (1.6, 1.6, 1.6)}), (0.55, {'orb': (0.8, 0.8, 0.8)}), (1.3, {'orb': (1, 1, 1)})])
    A['voidbeam'] = swing('VOIDBEAM', 3.5, [(0, {}), (0.8, {**arms(-90, 6, -4), 'hips': (8, 0, 0)}), (3.0, {**arms(-92, 6, -4), 'hips': (10, 0, 0)}), (3.5, {})])
    scales(A['voidbeam'], [(0, {'orb': (1, 1, 1)}), (0.8, {'orb': (2.2, 2.2, 2.2)}), (3.0, {'orb': (2.2, 2.2, 2.2)}), (3.5, {'orb': (1, 1, 1)})])
    A['blackhole'] = swing('BLACKHOLE', 2.2, [(0, {}), (0.7, {**arms(-120, 40, -20), 'hips': (-12, 0, 0), 'head': (-20, 0, 0)}), (1.4, {**arms(-120, 6, -20), 'hips': (14, 0, 0), 'head': (10, 0, 0)}), (2.2, {})])
    A['darkness'] = swing('DARKNESS', 2.0, [(0, {}), (0.6, {**arms(-100, 60), 'hips': (-14, 0, 0), 'head': (-24, 0, 0)}), (0.9, {**arms(-70, 20), 'hips': (14, 0, 0), 'head': (10, 0, 0)}), (2.0, {})])
    A['summon'] = swing('SUMMON', 2.8, [(0, {}), (0.7, {**arms(-80, 14, -20), 'hips': (-10, 0, 0), 'head': (-22, 0, 0)}), (1.2, {**arms(-140, 30, -20), 'hips': (-16, 0, 0), 'head': (-30, 0, 0)}), (2.2, {**arms(-140, 30, -20), 'hips': (-16, 0, 0)}), (2.8, {})])
    A['fade'] = swing('FADE', 0.8, [(0, {}), (0.8, {})])
    scales(A['fade'], [(0, {'base': (1, 1, 1)}), (0.3, {'base': (0.2, 1.1, 0.2)}), (0.8, {'base': (1, 1, 1)})])
    A['voidrain'] = swing('VOIDRAIN', 3.2, [(0, {}), (0.7, {**arms(-170, 20), 'hips': (-20, 0, 0), 'head': (-30, 0, 0)}), (2.6, {**arms(-176, 20), 'hips': (-22, 0, 0), 'head': (-34, 0, 0)}), (3.2, {})])
    moves(A['voidrain'], [(0, {}), (0.7, {'base': (0, 5, 0)}), (2.6, {'base': (0, 5, 0)}), (3.2, {})])
    A['nova'] = swing('NOVA', 2.3, [(0, {}), (0.7, {**arms(-40, -30), 'hips': (14, 0, 0), 'head': (20, 0, 0)}), (1.0, {**arms(-100, 70), 'hips': (-20, 0, 0), 'head': (-30, 0, 0)}), (1.6, {**arms(-100, 70), 'hips': (-20, 0, 0)}), (2.3, {})])
    scales(A['nova'], [(0, {'base': (1, 1, 1)}), (0.7, {'base': (0.88, 0.9, 0.88)}), (1.0, {'base': (1.15, 1.1, 1.15)}), (2.3, {'base': (1, 1, 1)})])
    return M, Pt, A
