"""Gölge Gözü (Gloom Eye) - floating eyeball trailing tentacles; its gaze burns, its flash blinds."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'gloom_eye'
SPEC = dict(
    faction='SHADOW', tr='Gölge Gözü', en='Gloom Eye', hp=40, armor=2, attack=6.0, speed=0.18, size=(1.2, 1.4, 1.0), fly=2.4, reach=2.6, follow=44,
    voice=('warden', 1.7), egg=(0x1C1038, 0xFFE8FF), spawn=(5, 1, 2, ['umbral_reach', 'gloam_forest']), xp=18, death_ticks=26, anim=(2.0, 2.0), head='core',
    abilities=[
        dict(kind='MELEE', anim='lash', time=(18, 9), cd=20, dmg=0.9, p=(0.4, 0), color=0x8A5CFF, fx='void'),
        dict(kind='BEAM', anim='gaze', time=(50, 22, 22), cd=160, w=4, range=(5, 22), dmg=0.6, p=(22, 0), color=0xC8A0FF, fx='void', los=True, eff=('minecraft:wither', 60, 0)),
        dict(kind='BURST', anim='flash', time=(30, 16), cd=220, w=3, range=(0, 8), dmg=0.4, p=(8, 0.4), color=0xF0E0FF, fx='rod', eff=('minecraft:blindness', 100, 0)),
        dict(kind='BLINK_AWAY', anim='fade', time=(14, 5), cd=150, w=2, range=(0, 8), p=(10, 0), color=0x7040C0, fx='void'),
    ],
    drops=[('krolasyonbosses:shadow_essence', 1, 2, 0.7), ('minecraft:spider_eye', 0, 2, 0.5)],
    lore='Gölge Tarikatı\'nın seyirci gözleri. Hiç kırpmaz; baktığı yerde karanlık da, saklanan da yanar.',
)

FLESH = pal('#1a0e2a', '#3a2858', '#6a4e96', '#a58ac8', '#d8c8ec')
GLOW = (190, 140, 255)


def build():
    M = Model(ID, 128, 128)
    M.bone('base', None, (0, 24, 0))
    M.bone('core', 'base', (0, 11, 0))
    box(M, 'core', (0, 11, 0), (11.4, 11.4, 15.2), 'eye_front')
    box(M, 'core', (0, 11, 0), (15.2, 11.4, 11.4), 'sclera')
    box(M, 'core', (0, 11, 0), (11.4, 15.2, 11.4), 'sclera')
    box(M, 'core', (0, 11, 0), (13.4, 13.4, 13.4), 'sclera')
    # eyelids
    zero_bone(M, 'lid_top', 'core', (0, 4.2, -6.4))
    M.cube_l('lid_top', (-6.2, -0.8, -1.4), (12.4, 3.2, 3.2), 'lid')
    M.cube_l('lid_top', (-5.0, -0.8, 1.8), (10.0, 2.4, 8.4), 'lid')
    zero_bone(M, 'lid_bot', 'core', (0, 17.8, -6.4))
    M.cube_l('lid_bot', (-6.2, -2.4, -1.4), (12.4, 3.2, 3.2), 'lid')
    # brow horns and side fins
    for sx in (-1, 1):
        chain(M, f'horn{sx}', 'core', [(sx * 5.5, 5.5, -4), (sx * 8.6, 2.0, -3), (sx * 9.8, -3.0, -2)], (2.6, 1.8), 'horn', ext=0.2)
        zero_bone(M, f'fin{sx}', 'core', (sx * 6.8, 11, 2))
        M.cube_l(f'fin{sx}', (0 if sx > 0 else -9, -4, -0.3), (9, 8, 0.6), 'fin')
    # tentacles hanging below
    for k in range(7):
        a = k * math.tau / 7
        x, z = math.cos(a) * 3.6, math.sin(a) * 3.6
        L = 11 + (k % 3) * 3
        pts = [(x, 18, z), (x * 1.4, 18 + L * 0.33, z * 1.4), (x * 1.7, 18 + L * 0.66, z * 1.7), (x * 1.9, 18 + L, z * 1.9)]
        chain(M, f'tent{k}_', 'core', pts, (2.4, 1.9, 1.4), 'tentacle', ext=0.3)

    Pt = Painter(M)
    def eye_front(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        base = mats.skin(pal('#a89ec0', '#c8c0dc', '#e4dff0', '#f6f2fb'), 0.15, vgrad=0.05)
        col, a, g = base(c, face, W, H, r)
        col = col.copy()
        # red-violet veins
        from paint import cracks
        vm = cracks(H, W, r, max(2, W * H // 120), 0.4, 0.4)
        col = np.where(vm[..., None] > 0.7, np.array((150, 70, 130), np.float32), col)
        g = np.zeros((H, W, 4), np.float32)
        if face == 'north':
            cx, cy = W / 2, H / 2
            d = np.hypot(xx + 0.5 - cx, (yy + 0.5 - cy))
            iris = d < W * 0.36
            col = np.where(iris[..., None], np.array((90, 40, 190), np.float32) * (0.75 + 0.5 * (1 - d / (W * 0.36)))[..., None], col)
            rim = (d > W * 0.3) & iris
            col = np.where(rim[..., None], np.array((40, 16, 100), np.float32), col)
            pup = (np.abs(xx + 0.5 - cx) < W * 0.06) & (np.abs(yy + 0.5 - cy) < H * 0.3)
            col = np.where(pup[..., None], np.array((4, 2, 10), np.float32), col)
            g[..., :3] = (190, 130, 255)
            g[..., 3] = np.where(iris & ~pup, 170 * (1 - d / (W * 0.4)), 0)
        return col, a, g
    Pt.mats['eye_front'] = eye_front
    Pt.mats['sclera'] = mats.skin(pal('#a89ec0', '#c8c0dc', '#e4dff0'), 0.15, vgrad=0.05)
    Pt.mats['lid'] = mats.skin(FLESH, 0.4, veins=(190, 130, 255))
    Pt.mats['horn'] = mats.horn(pal('#0c0618', '#201440', '#3c2878', '#6a4ec0'), 3, tip_glow=GLOW)
    Pt.mats['fin'] = mats.membrane(pal('#1a0e2a', '#3a2858', '#5a3e90', '#8a68c8'), vein_col=(190, 140, 255), scallops=3)
    Pt.mats['tentacle'] = mats.with_glow_streaks(mats.skin(FLESH, 0.4), (190, 140, 255), 0.4, True)

    A = {}
    for name, L, amp in (('IDLE', 3.0, 0.5), ('WALK', 1.6, 1.0)):
        a = Anim(name, L, True)
        gen(a, 'core', 'p', lambda ph: (0, 1.4 * S(ph), 0), n=10)
        gen(a, 'core', 'r', lambda ph, amp=amp: (5 * amp * S(ph, 2, 0.1) + (8 if name == 'WALK' else 0), 6 * S(ph, 1, 0.3), 3 * S(ph, 1, 0.1)), n=10)
        for k in range(7):
            for i in range(3):
                gen(a, f'tent{k}_{i}', 'r', lambda ph, k=k, i=i, amp=amp: (10 * amp * S(ph, 1, k * 0.14 - i * 0.1), 0, 12 * S(ph, 1, k * 0.2 - i * 0.12 + 0.2) * (0.6 + amp * 0.4)), n=10)
        for sx in (-1, 1):
            gen(a, f'fin{sx}', 'r', lambda ph, sx=sx: (0, 0, sx * 14 * S(ph, 2, 0.1 * sx)), n=10)
        gen(a, 'lid_top', 'r', lambda ph: (-6 - 6 * max(0, S(ph, 1, 0.6)), 0, 0), n=10)
        gen(a, 'lid_bot', 'r', lambda ph: (6 + 4 * max(0, S(ph, 1, 0.6)), 0, 0), n=10)
        A[name.lower()] = a
    d = Anim('DEATH', 26 / 20, False)
    L = d.length
    poses(d, [(0, {}), (L * 0.4, {'core': (-20, 0, 20), 'lid_top': (30, 0, 0), 'lid_bot': (-30, 0, 0)}), (L, {'core': (-40, 0, 90), 'lid_top': (40, 0, 0), 'lid_bot': (-40, 0, 0)})])
    for k in range(7):
        poses(d, [(0, {}), (L, {f'tent{k}_0': (10, 0, 20), f'tent{k}_1': (10, 0, 30), f'tent{k}_2': (10, 0, 30)})])
    moves(d, [(0, {}), (L, {'base': (0, -9, 0)})])
    scales(d, [(0, {'base': (1, 1, 1)}), (L, {'base': (1.0, 0.9, 1.0)})])
    A['death'] = d
    A['lash'] = swing('LASH', 0.9, [(0, {}), (0.25, {'core': (-14, 0, 0)}), (0.4, {'core': (26, 0, 0)}), (0.9, {})])
    for k in range(7):
        poses(A['lash'], [(0, {}), (0.25, {f'tent{k}_0': (-30, 0, 0), f'tent{k}_1': (-30, 0, 0)}), (0.4, {f'tent{k}_0': (50, 0, 0), f'tent{k}_1': (40, 0, 0), f'tent{k}_2': (30, 0, 0)}), (0.9, {})])
    A['gaze'] = swing('GAZE', 2.5, [(0, {}), (0.6, {'core': (-8, 0, 0), 'lid_top': (-26, 0, 0), 'lid_bot': (26, 0, 0)}), (2.2, {'core': (-8, 0, 0), 'lid_top': (-28, 0, 0), 'lid_bot': (28, 0, 0)}), (2.5, {})])
    scales(A['gaze'], [(0, {'core': (1, 1, 1)}), (0.6, {'core': (1.2, 1.2, 1.2)}), (2.2, {'core': (1.25, 1.25, 1.25)}), (2.5, {'core': (1, 1, 1)})])
    A['flash'] = swing('FLASH', 1.5, [(0, {}), (0.5, {'core': (-14, 0, 0), 'lid_top': (-30, 0, 0), 'lid_bot': (30, 0, 0)}), (0.8, {'core': (12, 0, 0), 'lid_top': (-34, 0, 0), 'lid_bot': (34, 0, 0)}), (1.5, {})])
    scales(A['flash'], [(0, {'core': (1, 1, 1)}), (0.5, {'core': (0.85, 0.85, 0.85)}), (0.8, {'core': (1.4, 1.4, 1.4)}), (1.5, {'core': (1, 1, 1)})])
    A['fade'] = swing('FADE', 0.7, [(0, {}), (0.7, {})])
    scales(A['fade'], [(0, {'base': (1, 1, 1)}), (0.25, {'base': (0.2, 0.2, 0.2)}), (0.7, {'base': (1, 1, 1)})])
    return M, Pt, A
