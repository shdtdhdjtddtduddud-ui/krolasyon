"""Ayna Hayaleti (Mirror Phantom) - humanoid assembled from floating mirror shards."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, glow_img
import mats
from mats import P as pal
from arch import *

ID = 'mirror_phantom'
SPEC = dict(
    faction='SHADOW', tr='Ayna Hayaleti', en='Mirror Phantom', hp=44, armor=4, attack=6.0, speed=0.27, size=(0.7, 2.0, 1.0), reach=2.5, kb=0.2, follow=38,
    voice=('warden', 1.9), egg=(0xC8D8F0, 0x7040C0), spawn=(5, 1, 2, ['umbral_reach', 'gloam_forest']), xp=18, death_ticks=22, anim=(1.6, 2.0),
    abilities=[
        dict(kind='MELEE', anim='slash', time=(18, 9), cd=20, dmg=1.0, p=(0.4, 0), color=0xCFE8FF, fx='crit'),
        dict(kind='SHIELD', anim='reflect', time=(50, 4), cd=200, w=3, range=(0, 12), p=(0.9, 1.5), color=0xCFE8FF, fx='rod'),
        dict(kind='VOLLEY', anim='shards', time=(30, 16), cd=150, w=4, range=(4, 16), dmg=0.7, p=(6, 0.8), color=0xCFE8FF, fx='rod', los=True),
        dict(kind='BURST', anim='shatter', time=(36, 20), cd=240, w=2, range=(0, 6), dmg=1.1, p=(5.5, 1.0), color=0xCFE8FF, fx='rod', eff=('minecraft:slowness', 60, 1)),
        dict(kind='TELEPORT_STRIKE', anim='slipstep', time=(24, 7, 15), cd=150, w=3, range=(3, 14), dmg=1.3, p=(0, 0), color=0xAAB8F0, fx='rod'),
    ],
    drops=[('krolasyonbosses:shadow_essence', 0, 2, 0.5), ('minecraft:glass', 1, 3, 0.6), ('minecraft:amethyst_shard', 0, 2, 0.6)],
    lore='Gölge Tarikatı\'nın kırık aynalardan çağırdığı hayaletler. Saldırıyı geri yansıtır; kendi yüzünüzü görmek için fazla yaklaşmayın.',
)

GLASS = pal('#2a3a5a', '#5a78a8', '#9ab8e0', '#d0e4fa', '#f4faff')
VIO = pal('#1a1038', '#38288a', '#6a50c8', '#a898f0')
GLOW = (200, 220, 255)


def build():
    M = Model(ID, 128, 128)
    K = humanoid(M, leg=(11, 11), leg_w=3.6, foot=(4.4, 2.2, 6.8), hip_w=8.2, pelvis_h=4, torso=(8.4, 11, 5), neck=2.0, head=(7, 7.6, 7),
                 arm=(10, 9.5), arm_w=3.2, hand=(2.8, 3.6, 2.8), hunch=3, arm_out=0.6, bend=(0.3, 0.3), head_fwd=1,
                 mats=dict(pelvis='glass', torso='glass', head='face', arm='glass', fore='glass2', hand='glass2', leg='glass', shin='glass2', foot='glass2'))
    hy = K['hip_y']; hc = K['head_c']; w = K['waist']
    # shard crown and shoulder shards (tilted planes)
    for k, (x, rz, h) in enumerate(((-2.4, -22, 8), (0, 0, 11), (2.4, 22, 8), (-4.2, -46, 6), (4.2, 46, 6))):
        world_bone(M, f'crown{k}', 'head', (x, hc[1] - 3.4, hc[2] + 1), 0, 0, rz)
        M.cube_l(f'crown{k}', (-1.5, -h, 0), (3.0, h, 0), 'shard', plane=True)
    for sx, s in ((-1, 'right'), (1, 'left')):
        A = K[f'{s}_A']
        for k in range(3):
            world_bone(M, f'{s}_sh{k}', f'{s}_arm', (A[0] + sx * (1.5 + k * 1.4), A[1] - 0.4, A[2] + (k - 1) * 1.6), 0, 0, sx * (28 + k * 16))
            M.cube_l(f'{s}_sh{k}', (-1.4, -6 - k, 0), (2.8, 6 + k, 0), 'shard', plane=True)
    # orbiting shards
    for k in range(6):
        zero_bone(M, f'orb{k}', 'chest', (0, w[1] - 6 - (k % 3) * 4, 0))
        M.cube_l(f'orb{k}', (11 - 1.2, -2.2, -0.2), (2.4, 4.4, 0.6), 'shard')
    # fractured core in the chest
    box(M, 'chest', (0, w[1] - 7, -2.8), (4.6, 5.2, 1.0), 'core')

    Pt = Painter(M)
    Pt.mats['glass'] = mats.crystal(GLASS, GLOW, 3.4)
    Pt.mats['glass2'] = mats.crystal(pal('#38288a', '#6a50c8', '#a898f0', '#d8d0ff'), (170, 150, 255), 3.0)
    Pt.mats['shard'] = mats.crystal(pal('#7a98c8', '#b8d0f0', '#e0eeff', '#ffffff'), GLOW, 2.4)
    Pt.mats['core'] = mats.solid((200, 160, 255))
    Pt.mats['face'] = mats.face_on(mats.crystal(GLASS, GLOW, 3.4), 'north', eyes=[(0.2, 0.4, 0.14, 0.14, 0.0, 'round')], eye_col=(255, 255, 255), glow_col=(230, 240, 255),
                                   extra=lambda col, g, H, W, r: _cracks(col, g, H, W, r))

    ba = dict(stride=30, shin=40, arm=14, elbow=10, lean=3)
    A = biped_set(M, ba, death='shrink', death_len=22 / 20)
    for nm in ('idle', 'walk', 'run'):
        for k in range(6):
            gen(A[nm], f'orb{k}', 'r', lambda ph, k=k: (0, 360 * ph + k * 60, 0), n=12)
    A['slash'] = swing('SLASH', 1.1, [(0, {}), (0.3, {'right_arm': (-120, 0, -14), 'chest': (-8, -20, 0), 'head': (-8, 0, 0), 'left_arm': (-30, 0, 14)}), (0.45, {'right_arm': (-30, 0, 8), 'chest': (20, 22, 0), 'head': (8, 0, 0)}), (1.1, {})])
    A['reflect'] = swing('REFLECT', 2.5, [(0, {}), (0.3, {'right_arm': (-80, 0, -40), 'left_arm': (-80, 0, 40), 'right_fore': (-60, 0, 0), 'left_fore': (-60, 0, 0), 'chest': (4, 0, 0)}), (2.1, {'right_arm': (-84, 0, -42), 'left_arm': (-84, 0, 42), 'right_fore': (-62, 0, 0), 'left_fore': (-62, 0, 0)}), (2.5, {})])
    scales(A['reflect'], [(0, {'base': (1, 1, 1)}), (0.3, {'base': (1.1, 1.05, 1.1)}), (2.1, {'base': (1.1, 1.05, 1.1)}), (2.5, {'base': (1, 1, 1)})])
    A['shards'] = swing('SHARDS', 1.5, [(0, {}), (0.4, {'right_arm': (-60, 0, -40), 'left_arm': (-60, 0, 40), 'chest': (-10, 0, 0), 'head': (-10, 0, 0)}), (0.6, {'right_arm': (-100, 0, -10), 'left_arm': (-100, 0, 10), 'chest': (10, 0, 0), 'head': (8, 0, 0)}), (1.5, {})])
    A['shatter'] = swing('SHATTER', 1.8, [(0, {}), (0.6, {'right_arm': (-160, 0, -20), 'left_arm': (-160, 0, 20), 'chest': (-18, 0, 0), 'head': (-20, 0, 0)}), (0.85, {'right_arm': (-20, 0, -30), 'left_arm': (-20, 0, 30), 'chest': (30, 0, 0), 'head': (16, 0, 0)}), (1.8, {})])
    A['slipstep'] = swing('SLIPSTEP', 1.2, [(0, {}), (0.3, {'chest': (-10, 0, 0), 'right_arm': (-30, 0, -40), 'left_arm': (-30, 0, 40)}), (0.45, {}), (0.7, {'right_arm': (-120, 0, -10), 'chest': (20, 10, 0), 'head': (8, 0, 0)}), (1.2, {})])
    scales(A['slipstep'], [(0, {'base': (1, 1, 1)}), (0.28, {'base': (0.25, 1.1, 0.25)}), (0.42, {'base': (1, 1, 1)})])
    return M, Pt, A


def _cracks(col, g, H, W, r):
    from paint import cracks
    cm = cracks(H, W, r, 2, 0.5, 0.2)
    col = np.where(cm[..., None] > 0.7, np.array((20, 20, 60), np.float32), col)
    return col, g
