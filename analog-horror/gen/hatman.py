"""The Hatman - high detail GeckoLib model (authored upright, posture comes from bone rest rotations)."""
import math, numpy as np
from geo import Geo

G = Geo('hatman', 512, 512)
S_ = {'right': -1, 'left': 1}

# =============================== torso ===============================
G.bone('root', None, (0, 0, 0))
G.bone('base', 'root', (0, 30, 0))
G.bone('hips', 'base', (0, 30, 0))
G.box('hips', (-2.3, 28.6, -1.2), (4.6, 3.2, 2.6), 'skin')
for s in (-1, 1):
    G.rod('hips', (s * 0.9, 32.0, 0.1), (s * 2.9, 30.0, -0.7), 1.0, 2.0, 'skin_bone')        # iliac crest
    G.box('hips', (s * 2.25 - 0.8, 29.0, -0.8), (1.6, 1.7, 1.7), 'skin_joint')                # hip ball
G.box('hips', (-1.1, 28.9, 1.2), (2.2, 2.6, 0.6), 'skin_bone')                                 # sacrum

G.bone('waist', 'hips', (0, 31.5, 0.2), (10, 0, 0))
G.box('waist', (-0.7, 31.3, 0.0), (1.4, 7.4, 1.4), 'skin_bone')                                # spine column
G.box('waist', (-1.3, 31.6, -1.1), (2.6, 6.2, 1.5), 'skin_belly')                               # sunken abdomen
for i in range(5):
    G.box('waist', (-0.45, 31.8 + i * 1.45, 1.25), (0.9, 0.7, 0.55), 'skin_bone')              # vertebra knobs
for s in (-1, 1):
    G.rod('waist', (s * 0.7, 38.2, -0.9), (s * 2.3, 36.2, -0.2), 0.5, 0.6, 'rib')               # floating ribs
    G.rod('waist', (s * 0.7, 37.1, -0.9), (s * 2.0, 35.5, -0.1), 0.45, 0.55, 'rib')

G.bone('chest', 'waist', (0, 38.5, 0.3), (20, 0, 0))
G.box('chest', (-2.5, 38.4, -1.4), (5.0, 8.4, 3.4), 'skin_chest')
G.box('chest', (-1.5, 39.4, -1.95), (3.0, 6.2, 0.6), 'sternum')
for k, y in enumerate((45.6, 44.2, 42.8, 41.4, 40.0)):
    for s in (-1, 1):
        G.rod('chest', (s * 2.45, y, 1.5), (s * 2.6, y - 1.0, -1.1), 0.55, 0.6, 'rib')
        G.rod('chest', (s * 2.45, y - 1.0, -1.35), (s * 1.3, y - 1.6 - k * 0.1, -1.85), 0.55, 0.5, 'rib')
for s in (-1, 1):
    G.rod('chest', (s * 0.35, 46.4, -1.6), (s * 3.5, 46.7, -0.3), 0.6, 0.6, 'skin_bone')        # collarbone
    G.box('chest', (s * 1.75 - 1.15, 42.6, 1.75), (2.3, 3.6, 0.5), 'skin_bone', pivot=(s * 1.75, 44.4, 2.0), rot=(0, -s * 14, s * 6))  # scapula
G.box('chest', (-3.3, 45.3, -0.9), (6.6, 1.6, 2.7), 'skin')                                     # shoulder yoke
for i in range(5):
    G.box('chest', (-0.45, 39.0 + i * 1.6, 1.85), (0.9, 0.75, 0.6), 'skin_bone')

G.bone('neck', 'chest', (0, 46.8, -0.1), (28, 0, 0))
G.box('neck', (-0.65, 46.5, -0.7), (1.3, 3.2, 1.3), 'skin')
for i in range(3):
    G.box('neck', (-0.4, 46.9 + i * 0.9, 0.5), (0.8, 0.55, 0.45), 'skin_bone')
for s in (-1, 1):
    G.rod('neck', (s * 0.75, 46.9, -0.9), (s * 0.3, 49.4, -0.5), 0.4, 0.4, 'skin_joint')

G.bone('head', 'neck', (0, 49.4, -0.2), (-48, 0, 0))
G.box('head', (-1.6, 50.3, -1.9), (3.2, 2.9, 3.6), 'skull')
G.box('head', (-1.3, 49.5, -2.45), (2.6, 1.8, 0.8), 'face')
G.box('head', (-1.4, 51.75, -2.25), (2.8, 0.5, 0.6), 'skull')                                   # brow ridge
G.box('head', (-1.0, 48.95, -2.6), (2.0, 0.85, 2.3), 'jaw', pivot=(0, 49.6, -0.3), rot=(10, 0, 0))
for s in (-1, 1):
    G.box('head', (s * 1.45 - 0.3, 50.6, -2.05), (0.6, 0.55, 1.3), 'skull')                      # cheekbones
G.box('head', (-1.2, 50.8, 1.6), (2.4, 2.0, 0.5), 'skull')                                      # back bump

# hat: straw sun hat, octagonal crown, 16-slab drooping brim with rolled rim
G.bone('hat', 'head', (0, 52.6, -0.1), (-6, 0, 7))
HY = 52.55
G.box('hat', (-2.3, HY, -2.4), (4.6, 2.7, 4.6), 'straw')
G.box('hat', (-2.3, HY, -2.4), (4.6, 2.64, 4.6), 'straw', pivot=(0, HY, -0.1), rot=(0, 45, 0))
G.box('hat', (-1.9, HY + 2.55, -0.3), (3.8, 0.35, 0.4), 'straw_dark')                           # pinch dent
G.box('hat', (-2.4, HY - 0.05, -2.5), (4.8, 0.8, 4.8), 'band')
G.box('hat', (-2.4, HY - 0.06, -2.5), (4.8, 0.78, 4.8), 'band', pivot=(0, HY, -0.1), rot=(0, 45, 0))
G.box('hat', (-2.7, HY - 0.35, -2.8), (5.4, 0.4, 5.4), 'brim')
N = 16
for i in range(N):
    a = i * 360 / N
    hgt = 0.012 * (i % 2)
    G.box('hat', (-1.45, HY - 0.36 + hgt, -0.1 - 6.7), (2.9, 0.38, 4.5), 'brim', pivot=(0, HY - 0.2, -0.1), rot=(5, a, 0))
    G.box('hat', (-1.35, HY - 0.92 + hgt, -0.1 - 6.95), (2.7, 0.55, 0.6), 'brim_rim', pivot=(0, HY - 0.2, -0.1), rot=(5, a + 11.25, 0))

# =============================== legs ===============================
for side, s in S_.items():
    H = np.array((s * 1.7, 30.0, 0.2)); K = np.array((s * 1.9, 15.2, 0.0)); A = np.array((s * 1.8, 1.7, 0.45))
    M = H + (K - H) * 0.45
    G.bone(f'{side}_thigh', 'hips', H, (-12, 0, 0))
    G.rod(f'{side}_thigh', H, M, 1.9, 2.0, 'skin')
    G.rod(f'{side}_thigh', M, K + (0, 0.6, 0), 1.45, 1.6, 'skin')
    G.rod(f'{side}_thigh', H + (-s * 0.5, -1, -0.9), K + (-s * 0.3, 2.2, -0.6), 0.55, 0.55, 'sinew')
    G.rod(f'{side}_thigh', K + (0, 5.5, 0.85), K + (0, 0.8, 0.9), 0.5, 0.5, 'sinew')
    G.box(f'{side}_thigh', K + (-1.0, -1.0, -1.0), (2.0, 2.1, 2.0), 'skin_joint')
    G.box(f'{side}_thigh', K + (-0.7, -0.6, -1.65), (1.4, 1.7, 0.6), 'skin_bone')                     # kneecap

    G.bone(f'{side}_shin', f'{side}_thigh', K, (20, 0, 0))
    G.rod(f'{side}_shin', K, A, 1.1, 1.25, 'skin')
    G.rod(f'{side}_shin', K + (0, -0.6, -0.8), A + (0, 1.3, -0.65), 0.55, 0.45, 'skin_bone')           # tibia ridge
    G.rod(f'{side}_shin', K + (0, -1.8, 0.55), K + (0, -7.5, 0.5), 1.25, 1.1, 'skin')                  # calf
    for sx in (-1, 1):
        G.box(f'{side}_shin', A + (sx * 0.75 - 0.35, -0.3, -0.35), (0.7, 0.8, 0.7), 'skin_joint')     # ankle bones

    G.bone(f'{side}_foot', f'{side}_shin', A, (-8, 0, 0))
    ax, ay, az = A
    G.box(f'{side}_foot', (ax - 1.2, 0.0, az - 4.6), (2.4, 0.45, 6.2), 'sole')
    G.box(f'{side}_foot', (ax - 1.1, 0.4, az - 3.6), (2.2, 1.35, 4.9), 'leather')
    G.box(f'{side}_foot', (ax - 0.95, 0.4, az - 4.45), (1.9, 1.0, 1.0), 'leather')                      # toe cap
    G.box(f'{side}_foot', (ax - 1.0, 0.0, az + 0.8), (2.0, 0.75, 1.0), 'sole')                          # heel
    G.box(f'{side}_foot', (ax - 0.8, 1.55, az - 3.2), (1.6, 0.3, 2.0), 'leather_dark', pivot=(ax, 1.6, az - 2.2), rot=(-12, 0, 0))
    G.box(f'{side}_foot', (ax - 0.8, 1.72, az - 1.2), (1.6, 0.06, 1.7), 'shoe_hole')

# =============================== arms ===============================
for side, s in S_.items():
    Sh = np.array((s * 3.5, 45.2, 0.2)); E = np.array((s * 3.9, 32.0, 0.4)); W = np.array((s * 3.9, 18.4, 0.1))
    G.bone(f'{side}_upper', 'chest', Sh, (-34, 0, -s * 4))
    G.box(f'{side}_upper', Sh + (-0.95, -1.1, -0.95), (1.9, 2.1, 1.9), 'skin_joint')
    Mu = Sh + (E - Sh) * 0.4
    G.rod(f'{side}_upper', Sh, Mu, 1.5, 1.6, 'skin')
    G.rod(f'{side}_upper', Mu, E + (0, 0.5, 0), 1.15, 1.25, 'skin')
    G.rod(f'{side}_upper', Sh + (0, -2.5, -0.9), E + (0, 2.2, -0.75), 0.45, 0.45, 'sinew')
    G.box(f'{side}_upper', E + (-0.8, -0.85, -0.8), (1.6, 1.7, 1.7), 'skin_joint')
    G.box(f'{side}_upper', E + (-0.45, -0.7, 0.8), (0.9, 1.3, 0.8), 'skin_bone')                        # olecranon

    G.bone(f'{side}_fore', f'{side}_upper', E, (-14, 0, 0))
    G.rod(f'{side}_fore', E + (-s * 0.32, 0, -0.3), W + (-s * 0.3, 0.3, -0.25), 0.75, 0.8, 'skin')     # radius
    G.rod(f'{side}_fore', E + (s * 0.32, 0, 0.35), W + (s * 0.3, 0.3, 0.3), 0.7, 0.75, 'skin_bone')   # ulna
    G.rod(f'{side}_fore', E + (0, -0.6, 0), E + (0, -5.2, -0.1), 1.3, 1.3, 'skin')                   # forearm muscle
    G.box(f'{side}_fore', W + (-0.75, -0.3, -0.6), (1.5, 1.0, 1.2), 'skin_joint')

    G.bone(f'{side}_hand', f'{side}_fore', W, (-6, 0, 0))
    G.box(f'{side}_hand', W + (-0.42, -3.3, -1.3), (0.84, 3.4, 2.65), 'skin_hand')
    for i, dz in enumerate((-1.0, -0.33, 0.33, 1.0)):
        G.rod(f'{side}_hand', W + (s * 0.4, -0.2, dz * 0.6), W + (s * 0.45, -3.3, dz), 0.3, 0.38, 'skin_bone')  # metacarpal ridges
    tips = (2.3, 2.6, 2.5, 2.0)
    for i, dz in enumerate((-1.05, -0.36, 0.36, 1.05)):
        base = W + (0, -3.3, dz)
        lens = (3.1 * tips[i] / 2.5, 2.7 * tips[i] / 2.5, 2.2 * tips[i] / 2.5)
        par = f'{side}_hand'
        p = base.copy()
        for k, L in enumerate(lens):
            bn = f'{side}_f{i}{"abc"[k]}'
            G.bone(bn, par, p, ((-10 + i * 6.5) if k == 0 else 4, 0, 0))
            G.box(bn, p + (-0.37, -0.45, -0.37), (0.74, 0.8, 0.74), 'knuckle')
            G.vrod(bn, p, L, 0.5, 0.5, 'finger')
            if k == 2:
                G.vrod(bn, p + (0, -L, 0), 0.9, 0.36, 0.36, 'claw')
                G.vrod(bn, p + (0, -L - 0.9, 0), 0.7, 0.2, 0.2, 'claw_tip')
            par = bn
            p = p + (0, -L, 0)
    tb = W + (-s * 0.1, -1.6, -1.35)
    G.bone(f'{side}_thumb_a', f'{side}_hand', tb, (-28, 0, s * 8))
    G.box(f'{side}_thumb_a', tb + (-0.37, -0.4, -0.37), (0.74, 0.8, 0.74), 'knuckle')
    G.vrod(f'{side}_thumb_a', tb, 2.4, 0.52, 0.52, 'finger')
    tk = tb + (0, -2.4, 0)
    G.bone(f'{side}_thumb_b', f'{side}_thumb_a', tk, (10, 0, 0))
    G.box(f'{side}_thumb_b', tk + (-0.35, -0.4, -0.35), (0.7, 0.75, 0.7), 'knuckle')
    G.vrod(f'{side}_thumb_b', tk, 1.9, 0.46, 0.46, 'finger')
    G.vrod(f'{side}_thumb_b', tk + (0, -1.9, 0), 0.8, 0.34, 0.34, 'claw')
    G.vrod(f'{side}_thumb_b', tk + (0, -2.7, 0), 0.6, 0.18, 0.18, 'claw_tip')
