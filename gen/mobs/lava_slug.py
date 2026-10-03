"""Lav Sümüklüsü (Lava Slug) - segmented magma crawler with eye stalks and an obsidian shell."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'lava_slug'
SPEC = dict(
    faction='EMBER', tr='Lav Sümüklüsü', en='Lava Slug', hp=40, armor=3, attack=5.0, speed=0.18, size=(1.5, 1.2, 1.0), reach=2.4, kb=0.4,
    voice=('hound', 0.5), egg=(0x2A1A14, 0xFF7A18), spawn=(9, 1, 3, ['ember_wastes', 'lava_lakes']), xp=10, death_ticks=26, anim=(2.0, 2.0), head='head',
    abilities=[
        dict(kind='MELEE', anim='bite', time=(18, 9), cd=20, dmg=1.0, p=(0.5, 0), color=0xFF7A18, fx='flame'),
        dict(kind='ERUPT', anim='spit', time=(26, 14), cd=140, w=4, range=(3, 14), dmg=0.8, p=(1, 5), color=0xFF6A10, fx='flame'),
        dict(kind='LEAP', anim='flop', time=(30, 12), cd=170, w=3, range=(4, 12), dmg=1.2, p=(1.2, 0.55), color=0xFF7A18, fx='flame', eff=('minecraft:slowness', 60, 1)),
    ],
    drops=[('minecraft:magma_cream', 1, 3, 0.8), ('krolasyonbosses:ember_shard', 0, 1, 0.2)],
    lore='Yanardağ göllerinde yaşayan yavaş ama ölümcül yaratık. Geçtiği yerde zemin kızarır, ısırığı kemik eritir.',
)

SHELL = pal('#0a0709', '#17100f', '#2a1d18', '#42302a', '#5e463a')
FLESH = pal('#2a0e06', '#5a1e0a', '#9a3a10', '#d8620e', '#ffa030')
LAVA = (255, 120, 20)


def build():
    M = Model(ID, 128, 128)
    M.bone('base', None, (0, 24, 0))
    # body: 5 segments from head (front, -z) to tail (back, +z)
    zs = [-9.0, -3.5, 2.5, 8.0, 13.0]
    sizes = [(13, 9, 8), (15, 11, 8), (16, 12, 8.5), (13, 9.5, 8), (9, 7, 7)]
    prev = 'base'
    for i, (z, sz) in enumerate(zip(zs, sizes)):
        n = f'seg{i}'
        piv = (0, 24 - sz[1] * 0.45, z)
        M.bone(n, 'base' if i == 0 else f'seg{i - 1}', piv)
        box(M, n, (0, 24 - sz[1] / 2, z), sz, 'flesh')
        # obsidian shell plate over the back of every segment
        if i > 0:
            box(M, n, (0, 24 - sz[1] - 0.6, z), (sz[0] - 2.5, 2.2, sz[2] + 0.4), 'shell')
            for k in (-1, 1):
                local(M, f'{n}_sp{k}', n, (k * (sz[0] / 2 - 2.5), -sz[1] * 0.5 - 0.6, 0), (0, 0, k * 22))
                M.cube_l(f'{n}_sp{k}', (-1.3, -5.5, -1.3), (2.6, 5.5, 2.6), 'shell')
    # head with eye stalks and maw
    M.bone('head', 'seg0', (0, 24 - 5, -14.5))
    box(M, 'head', (0, 24 - 5.2, -14.5), (11, 8, 6), 'flesh')
    box(M, 'head', (0, 24 - 2.2, -17.3), (9, 2.6, 2.4), 'jaw')
    for sx in (-1, 1):
        zero_bone(M, f'stalk{sx}', 'head', (sx * 3.4, 24 - 8.8, -15.5))
        M.cube_l(f'stalk{sx}', (-0.9, -6.0, -0.9), (1.8, 6.0, 1.8), 'flesh')
        zero_bone(M, f'eye{sx}', f'stalk{sx}', (sx * 3.4, 24 - 14.5, -15.5))
        M.cube_l(f'eye{sx}', (-1.1, -1.1, -1.1), (2.2, 2.2, 2.2), 'eye')
    for k in range(5):
        M.cube_l('head', (-4.3 + k * 2.15, 22.0 - 24 + 24 - 0, -17.6) if False else (-4.4 + k * 2.2, -0.2 - 0, -18.2), (0.9, 2.2, 0.9), 'tooth') if False else None
    for k in range(5):
        box(M, 'head', (-4.0 + k * 2.0, 24 - 4.0, -17.9), (0.9, 2.4, 0.9), 'tooth')
    # drips of lava under the belly (planes)
    for k, (x, z) in enumerate(((-3, -4), (3, 4), (0, 10))):
        zero_bone(M, f'drip{k}', f'seg{1 + k}', (x, 24 - 2, z))
        M.cube_l(f'drip{k}', (-1.5, -2, 0), (3, 4.2, 0), 'flame', plane=True)

    Pt = Painter(M)
    Pt.mats['flesh'] = mats.rock(pal('#1a0c08', '#2e150d', '#4c2213', '#74361a', '#a0501e'), lava=(255, 130, 30), cell=6.5, crack_w=0.7)
    Pt.mats['shell'] = mats.rock(SHELL, lava=LAVA, cell=5, crack_w=0.5)
    Pt.mats['jaw'] = mats.skin(pal('#1a0a06', '#3a1a0c', '#6a2c10', '#8a4018'), 0.3)
    Pt.mats['tooth'] = mats.horn(pal('#3a2a1c', '#7a5a38', '#c8a878', '#f0dcb0'), 3)
    Pt.mats['eye'] = mats.eyeball((255, 200, 60), (255, 244, 210))
    Pt.mats['flame'] = mats.flame()

    segs = ['seg0', 'seg1', 'seg2', 'seg3', 'seg4']
    A = {}
    # crawl: travelling squash & stretch wave + gentle sway
    for name, L, amp in (('IDLE', 2.6, 0.3), ('WALK', 1.2, 1.0)):
        a = Anim(name, L, True)
        for i, s in enumerate(segs):
            gen(a, s, 'r', lambda ph, i=i, amp=amp: (4.5 * amp * S(ph, 1, -i * 0.13), 5 * amp * S(ph, 1, -i * 0.1 + 0.2), 0), n=10)
            gen(a, s, 's', lambda ph, i=i, amp=amp: (1 - 0.04 * amp * S(ph, 1, -i * 0.13), 1 + 0.09 * amp * S(ph, 1, -i * 0.13 + 0.25), 1 + 0.05 * amp * S(ph, 1, -i * 0.13)), n=10)
        gen(a, 'head', 'r', lambda ph, amp=amp: (3 * amp * S(ph, 1, 0.3), 6 * S(ph, 1, 0.5) * (0.4 + amp * 0.6), 0), n=10)
        for sx in (-1, 1):
            gen(a, f'stalk{sx}', 'r', lambda ph, sx=sx: (10 * S(ph, 1, 0.1 * sx), 0, sx * 8 * S(ph, 1, 0.3)), n=10)
        A[name.lower()] = a
    d = Anim('DEATH', 26 / 20, False)
    L = d.length
    poses(d, [(0, {}), (L * 0.4, {'head': (-10, 0, 0), 'stalk-1': (30, 0, -20), 'stalk1': (30, 0, 20)}), (L, {'head': (-20, 0, 0), 'stalk-1': (50, 0, -40), 'stalk1': (50, 0, 40)})])
    scales(d, [(0, {'base': (1, 1, 1)}), (L * 0.5, {'base': (1.12, 0.7, 1.12)}), (L, {'base': (1.5, 0.12, 1.5)})])
    A['death'] = d
    A['bite'] = swing('BITE', 0.9, [(0, {}), (0.25, {'seg0': (-14, 0, 0), 'head': (-30, 0, 0), 'seg1': (-8, 0, 0)}), (0.45, {'seg0': (12, 0, 0), 'head': (28, 0, 0), 'seg1': (6, 0, 0)}), (0.9, {})])
    moves(A['bite'], [(0, {}), (0.45, {'seg0': (0, 0, -2.5)}), (0.9, {})])
    A['spit'] = swing('SPIT', 1.3, [(0, {}), (0.4, {'seg0': (-26, 0, 0), 'head': (-34, 0, 0), 'seg1': (-14, 0, 0), 'seg2': (-8, 0, 0), 'stalk-1': (-30, 0, -10), 'stalk1': (-30, 0, 10)}),
                                   (0.7, {'seg0': (12, 0, 0), 'head': (24, 0, 0), 'seg1': (8, 0, 0)}), (1.3, {})])
    scales(A['spit'], [(0, {'seg1': (1, 1, 1)}), (0.4, {'seg1': (1.15, 1.2, 1.1)}), (0.7, {'seg1': (0.9, 0.85, 0.9)}), (1.3, {'seg1': (1, 1, 1)})])
    A['flop'] = swing('FLOP', 1.5, [(0, {}), (0.5, {'seg0': (-24, 0, 0), 'seg1': (-16, 0, 0), 'seg2': (-8, 0, 0), 'head': (-20, 0, 0)}), (0.75, {'seg0': (28, 0, 0), 'seg1': (14, 0, 0), 'head': (14, 0, 0)}), (1.5, {})])
    scales(A['flop'], [(0, {'base': (1, 1, 1)}), (0.5, {'base': (0.88, 1.35, 0.9)}), (0.8, {'base': (1.35, 0.5, 1.35)}), (1.5, {'base': (1, 1, 1)})])
    return M, Pt, A
