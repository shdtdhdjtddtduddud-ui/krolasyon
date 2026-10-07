"""Pixel-art material painters for the furniture models (box-UV, 1 texel = 1/32 block)."""
import os, sys, math
import numpy as np
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../gen'))
from core import Model
from paint import Painter, noise2


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


# palettes sampled from the reference art (dark -> light)
MAH = [hexc(x) for x in ('#5a2a30', '#7a3c3c', '#9a5444', '#bd7452')]
MAH_DARK = [hexc(x) for x in ('#3a1c26', '#4b2630', '#5e3038', '#74404a')]
MAH_LT = [hexc(x) for x in ('#8a4a48', '#a8624e', '#c98458', '#e0a468')]
CREAM = [hexc(x) for x in ('#cdb99a', '#e0d0b0', '#eee2c4', '#f8f0d8')]
LAV = [hexc(x) for x in ('#6c5860', '#8a7276', '#a08a8c', '#bba6a6')]
STEEL = [hexc(x) for x in ('#34506a', '#4c7088', '#6e98ae', '#a4cad8')]
BOARD = [hexc(x) for x in ('#1c4038', '#22493f', '#285448', '#305f52')]
BRISTLE = [hexc(x) for x in ('#b8968c', '#cfb0a2', '#e2c8b6', '#f0dcc4')]
GOLD = [hexc(x) for x in ('#9c6a14', '#c8901c', '#e8b030', '#f8d868')]
IVORY = [hexc(x) for x in ('#b8aa90', '#d8ccb0', '#ece4cc', '#faf6e6')]
EBONY = [hexc(x) for x in ('#0c0a10', '#18141c', '#262030', '#3a3446')]
CHALK = (232, 228, 214)

PALS = {'mah': MAH, 'dark': MAH_DARK, 'lt': MAH_LT, 'cream': CREAM, 'lav': LAV, 'steel': STEEL,
        'gold': GOLD, 'ivory': IVORY, 'ebony': EBONY, 'bristle': BRISTLE}

AX = {'top': ('x', 'z'), 'bottom': ('x', 'z'), 'north': ('x', 'y'), 'south': ('x', 'y'),
      'west': ('z', 'y'), 'east': ('z', 'y')}


def long_axis(c):
    s = dict(x=c.size[0], y=c.size[1], z=c.size[2])
    return max(s, key=s.get)


def grain_dir(c, face):
    aw, ah = AX[face]
    la = c.feat.get('grain') or long_axis(c)
    if la == aw:
        return 'h'
    if la == ah:
        return 'v'
    return 'end'


def quant(f, pal, bias=0.0):
    t = np.clip(f + bias, 0, 0.9999)
    idx = (t * len(pal)).astype(int)
    arr = np.array(pal, np.float32)
    return arr[idx], idx


def smooth1(a, k=2):
    out = a.copy()
    for _ in range(k):
        out = (np.roll(out, 1) + out * 2 + np.roll(out, -1)) / 4
    return out


def wood_face(c, face, W, H, r, pal):
    g = grain_dir(c, face)
    f = np.zeros((H, W))
    nz = noise2(H, W, r, scale=3.0, oct=2)
    if g == 'v':
        line = smooth1(r.random(W), 1)
        line = (line - line.min()) / (np.ptp(line) + 1e-6)
        f = line[None, :] * 0.62 + nz * 0.38
        # vertical broken streaks
        for _ in range(max(1, W // 5)):
            x = r.integers(0, W)
            y0 = r.integers(0, max(1, H)); y1 = min(H, y0 + r.integers(2, max(3, H // 2 + 2)))
            f[y0:y1, x] -= 0.25
    elif g == 'h':
        line = smooth1(r.random(H), 1)
        line = (line - line.min()) / (np.ptp(line) + 1e-6)
        f = line[:, None] * 0.62 + nz * 0.38
        for _ in range(max(1, H // 5)):
            y = r.integers(0, H)
            x0 = r.integers(0, max(1, W)); x1 = min(W, x0 + r.integers(2, max(3, W // 2 + 2)))
            f[y, x0:x1] -= 0.25
    else:
        yy, xx = np.mgrid[0:H, 0:W]
        d = np.hypot(yy - H / 2, xx - W / 2)
        f = (np.sin(d * 1.3) * 0.5 + 0.5) * 0.5 + nz * 0.5
    f = np.clip(f * 0.8 + 0.1, 0, 1)
    col, idx = quant(f, pal, bias=0.0)
    # light from the top: edge accents
    top_face = face == 'top'
    if H >= 3 and W >= 2:
        if top_face:
            col[0, :] = np.array(pal[-1]) * 0.96
            col[:, 0] = np.array(pal[min(3, len(pal) - 1)]) * 0.92
        elif face not in ('bottom',):
            col[0, :] = np.array(pal[-1]) * 0.92 if face in ('north', 'west', 'east', 'south') else col[0, :]
            col[-1, :] = np.array(pal[0]) * 0.9
    if face == 'bottom':
        col *= 0.7
    return col


def fabric_face(c, face, W, H, r, pal, seam=None):
    nz = noise2(H, W, r, scale=2.0, oct=2)
    f = 0.55 + (nz - 0.5) * 0.5
    if face in ('top',):
        f += 0.18
    col, idx = quant(np.clip(f, 0, 1), pal)
    # stitched piping on edges
    if W > 3 and H > 3:
        col[-1, :] = np.array(pal[0]) * 0.95
        if face != 'top':
            col[0, :] = np.array(pal[min(len(pal) - 1, 3)])
        else:
            col[0, :] = np.array(pal[min(len(pal) - 1, 3)])
            col[:, 0] = np.array(pal[min(len(pal) - 1, 3)]) * 0.97
            col[:, -1] = np.array(pal[1])
        if seam:
            col[:, W // 2] = np.array(pal[0]) * 1.02
    if face == 'bottom':
        col *= 0.6
    return col


def metal_face(c, face, W, H, r, pal=STEEL):
    nz = noise2(H, W, r, scale=2.0, oct=2)
    f = 0.5 + (nz - 0.5) * 0.6
    col, _ = quant(np.clip(f, 0, 1), pal)
    if H >= 2:
        col[0, :] = np.array(pal[-1])
        col[-1, :] = np.array(pal[0])
    if face == 'bottom':
        col *= 0.7
    return col


def ring_handle(W, H, pal=STEEL):
    """square ring handle with dark hole + tab, drawn on a flat plate"""
    col = np.zeros((H, W, 3), np.float32)
    a = np.zeros((H, W), np.float32)
    cx, cy = W // 2, H // 2
    s = min(W, H)
    ring = np.zeros((H, W), bool)
    r0 = max(1, s // 2 - 1)
    for y in range(H):
        for x in range(W):
            dx, dy = abs(x - (W - 1) / 2), abs(y - (H - 1) / 2)
            m = max(dx, dy)
            if m <= r0 + 0.3:
                ring[y, x] = True
    hole = np.zeros((H, W), bool)
    for y in range(H):
        for x in range(W):
            dx, dy = abs(x - (W - 1) / 2), abs(y - (H - 1) / 2)
            if max(dx, dy) <= r0 - 1.7:
                hole[y, x] = True
    for y in range(H):
        for x in range(W):
            if ring[y, x]:
                a[y, x] = 1
                col[y, x] = pal[2]
                if y == 0 or x == 0:
                    col[y, x] = pal[3]
                if y == H - 1 or x == W - 1:
                    col[y, x] = pal[0]
            if hole[y, x]:
                col[y, x] = (38, 22, 30)
    return col, a


