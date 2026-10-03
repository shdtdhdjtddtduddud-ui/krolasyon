"""Gölge Yılanı (Umbral Serpent) - long hooded serpent covered in glowing void runes."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'umbral_serpent'
SPEC = dict(
    faction='SHADOW', tr='Gölge Yılanı', en='Umbral Serpent', hp=52, armor=2, attack=7.0, speed=0.28, size=(1.3, 1.5, 1.0), reach=3.2, kb=0.35, follow=36,
    voice=('hound', 0.7), egg=(0x14082A, 0x7A4AE0), spawn=(6, 1, 2, ['umbral_reach', 'gloam_forest', 'weeping_marsh']), xp=20, death_ticks=30, anim=(2.0, 2.4), head='head',
    abilities=[
        dict(kind='MELEE', anim='bite', time=(16, 8), cd=18, dmg=1.0, p=(0.5, 0), color=0x8A5CFF, fx='void', eff=('minecraft:wither', 60, 0)),
        dict(kind='CHARGE', anim='strike', time=(26, 8), cd=100, w=4, range=(4, 14), dmg=1.3, p=(9, 1.1), color=0x8A5CFF, fx='smoke'),
        dict(kind='CLOUD', anim='spit', time=(30, 14), cd=170, w=3, range=(3, 13), dmg=0, p=(3.4, 140), color=0x5A2AA0, fx='void', eff=('minecraft:poison', 100, 1)),
        dict(kind='CONE', anim='breath', time=(46, 14, 24), cd=240, w=2, range=(0, 10), dmg=0.4, p=(24, 0), color=0x2A1050, fx='smoke', eff=('minecraft:blindness', 60, 0)),
    ],
    drops=[('krolasyonbosses:shadow_essence', 0, 2, 0.6), ('minecraft:spider_eye', 0, 2, 0.5)],
    lore='Gölge Tarikatı\'nın tapınak bekçileri. Pullarındaki rünler karanlıkta uyanır; zehri karşısındakinin gölgesini bile soldurur.',
)

VOID = pal('#0c0818', '#1a1230', '#2e2254', '#4a3a88', '#7a66c8')
GLOW = (170, 120, 255)


def build():
    M = Model(ID, 128, 128)
    M.bone('base', None, (0, 24, 0))
    path = [(0, 6.5, -19), (0, 10.5, -15), (0, 16.0, -10), (0, 20.2, -4), (1.5, 20.6, 3), (3.5, 20.9, 10), (2, 21.2, 17), (-2, 21.5, 24), (-3.5, 21.8, 31), (-1, 22.1, 38),
            (2.5, 22.4, 45), (3, 22.7, 52), (1, 23.0, 58)]
    widths = [6.6, 7.2, 8.2, 8.6, 8.4, 8.0, 7.4, 6.8, 6.0, 5.2, 4.2, 3.2]
    names = chain(M, 'seg', 'base', path, widths, 'scales', depth=0.92, ext=0.5)
    # head on seg0
    hp = path[0]
    zero_bone(M, 'head', names[0], (hp[0], hp[1] - 1, hp[2] - 2.4))
    box(M, 'head', (0, hp[1] - 1.5, hp[2] - 4.0), (6.8, 4.6, 8.4), 'head')
    box(M, 'head', (0, hp[1] + 0.5, hp[2] - 4.8), (5.6, 1.8, 7.2), 'jaw')
    for sx in (-1, 1):
        box(M, 'head', (sx * 2.2, hp[1] + 1.8, hp[2] - 7.0), (0.7, 3.2, 0.7), 'fang')
    box(M, 'head', (0, hp[1] + 0.6, hp[2] - 10.6), (0.8, 0.5, 4.4), 'tongue')
    # cobra hood: wide card behind the head (child of seg1 so it sways with the neck)
    zero_bone(M, 'hood', names[1], (path[1][0], path[1][1] - 2, path[1][2] - 0.5))
    M.cube_l('hood', (-8.5, -9, 0), (17, 17, 0), 'hood', plane=True)
    # dorsal fin crest along the neck
    for k in range(4):
        zero_bone(M, f'crest{k}', names[k], (path[k][0], path[k][1] - widths[k] * 0.5, path[k][2]))
        M.cube_l(f'crest{k}', (-0.4, -3.2 - (k % 2), -1.2), (0.8, 3.2 + (k % 2), 2.4), 'fang')
    # eyes: glowing slits on the head (painted)

    Pt = Painter(M)
    Pt.mats['scales'] = mats.scales(VOID, 3.4, glow_edge=GLOW, shine=0.25)
    Pt.mats['head'] = mats.face_on(mats.scales(VOID, 3.0), 'north', eyes=[(0.26, 0.34, 0.16, 0.06, 0.4, 'slit')], eye_col=(240, 220, 255), glow_col=GLOW)
    Pt.mats['jaw'] = mats.scales(pal('#0e0820', '#1c1238', '#30205c', '#4c3690'), 3.0)
    Pt.mats['fang'] = mats.horn(pal('#2a2038', '#5a4a78', '#a898c8', '#e8e0f8'), 3)
    Pt.mats['tongue'] = mats.solid((200, 120, 255))
    base = mats.membrane(pal('#0c0618', '#1c1238', '#30205c', '#4c3690', '#6a54b0'), vein_col=(180, 130, 255), scallops=4)

    def hood(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        col = col.copy(); g = g.copy() if g is not None else np.zeros((H, W, 4), np.float32)
        # glowing eye-sigil in the centre of the hood
        cx, cy = W / 2, H * 0.4
        yy, xx = np.mgrid[0:H, 0:W]
        d = np.hypot(xx - cx, (yy - cy) * 1.4)
        ring = (np.abs(d - W * 0.22) < 0.9) | (d < W * 0.06)
        col = np.where(ring[..., None], np.array((220, 190, 255), np.float32), col)
        g[ring] = (190, 140, 255, 230)
        return col, a, g
    Pt.mats['hood'] = hood

    A = serpent_set(M, names[1:], 'head', dict(wave=18, death_len=30 / 20))
    A['bite'] = swing('BITE', 0.8, [(0, {}), (0.2, {'seg0': (-22, 0, 0), 'seg1': (-10, 0, 0), 'head': (-24, 0, 0)}), (0.34, {'seg0': (22, 0, 0), 'seg1': (10, 0, 0), 'head': (22, 0, 0)}), (0.8, {})])
    A['strike'] = swing('STRIKE', 1.3, [(0, {}), (0.45, {'seg0': (-30, 0, 0), 'seg1': (-24, 0, 0), 'seg2': (-12, 0, 0), 'head': (-16, 0, 0), 'seg4': (0, 18, 0), 'seg6': (0, -18, 0)}),
                                       (0.6, {'seg0': (28, 0, 0), 'seg1': (16, 0, 0), 'head': (20, 0, 0)}), (1.3, {})])
    A['spit'] = swing('SPIT', 1.5, [(0, {}), (0.45, {'seg0': (-30, 0, 0), 'seg1': (-16, 0, 0), 'head': (-34, 0, 0)}), (0.65, {'seg0': (14, 0, 0), 'head': (28, 0, 0)}), (1.5, {})])
    A['breath'] = swing('BREATH', 2.3, [(0, {}), (0.5, {'seg0': (-30, 0, 0), 'seg1': (-22, 0, 0), 'head': (-40, 0, 0)}), (0.7, {'seg0': (-8, 0, 0), 'head': (4, 0, 0)}), (1.7, {'seg0': (-8, 0, 0), 'head': (4, 12, 0)}), (2.3, {})])
    for k in ('bite', 'strike', 'spit', 'breath'):
        scales(A[k], [(0, {'hood': (1, 1, 1)}), (A[k].length * 0.3, {'hood': (1.25, 1.2, 1)}), (A[k].length * 0.7, {'hood': (1.25, 1.2, 1)}), (A[k].length, {'hood': (1, 1, 1)})])
    return M, Pt, A
