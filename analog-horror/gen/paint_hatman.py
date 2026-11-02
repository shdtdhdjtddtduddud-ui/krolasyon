"""Procedural textures for the Hatman (4 texels / unit)."""
import numpy as np


def noise(H, W, r, scale=4.0, octaves=3):
    out = np.zeros((H, W)); amp = 1.0; tot = 0
    for _ in range(octaves):
        gh, gw = max(2, int(H / scale) + 2), max(2, int(W / scale) + 2)
        g = r.random((gh, gw))
        ys = np.linspace(0, gh - 1.001, H); xs = np.linspace(0, gw - 1.001, W)
        y0, x0 = ys.astype(int), xs.astype(int)
        fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy)
        tot += amp; amp *= 0.5; scale = max(1.0, scale / 2)
    return out / tot


def mix(a, b, t):
    a = np.asarray(a, np.float32); b = np.asarray(b, np.float32)
    t = np.clip(np.asarray(t, np.float32), 0, 1)[..., None]
    return a * (1 - t) + b * t


def rgba(col, alpha=None):
    H, W = col.shape[:2]
    a = np.full((H, W, 1), 255.0) if alpha is None else alpha[..., None] * 255.0
    return np.concatenate([col, a], -1)


SHADE = {'up': 1.0, 'north': 0.95, 'west': 0.92, 'east': 0.92, 'south': 0.85, 'down': 0.6}


def skin(c, f, W, H, r, lo=(9, 8, 7), mid=(34, 27, 24), hi=(118, 104, 94), gloss=1.0, crease=0.0, ridge=0.0):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise(H, W, r, 5) ; n2 = noise(H, W, r, 1.5, 2)
    col = mix(lo, mid, 0.25 + (n - 0.5) * 1.1 + (n2 - 0.5) * 0.35)
    # wet, glossy streaks running along the limb (fluorescent highlights in the photo)
    if f != 'down':
        for _ in range(max(1, W // 3)):
            x = r.integers(0, W)
            ln = (xx == x) & (r.random((H, W)) < 0.85)
            col[ln] = mix(col[ln], hi, (0.25 + 0.35 * r.random()) * gloss)
        edge = (xx == 0) if f in ('north', 'west') else (xx == W - 1)
        col[edge] = mix(col[edge], hi, 0.32 * gloss)
    if crease:
        k = (yy % 3 == 0) & (r.random((H, W)) < 0.7)
        col[k] = mix(col[k], (4, 3, 3), crease)
        k = (yy % 3 == 1)
        col[k] = mix(col[k], hi, crease * 0.25)
    if ridge:
        col = mix(col, (78, 66, 58), ridge * (n2 > 0.55))
    # grime blotches
    g = noise(H, W, r, 7) > 0.66
    col[g] = mix(col[g], (26, 22, 16), 0.5)
    return rgba(col * SHADE[f])


def sinew(c, f, W, H, r):
    return skin(c, f, W, H, r, mid=(48, 38, 34), gloss=1.5)


def skin_joint(c, f, W, H, r):
    return skin(c, f, W, H, r, mid=(46, 38, 33), gloss=1.2, crease=0.6)


def skin_bone(c, f, W, H, r):
    return skin(c, f, W, H, r, mid=(52, 44, 38), gloss=1.3, ridge=0.6)


def skin_belly(c, f, W, H, r):
    px = skin(c, f, W, H, r, lo=(5, 4, 4), mid=(22, 18, 16), gloss=0.6)
    if f == 'north':
        px[:, W // 2, :3] *= 0.5
    return px


def skin_hand(c, f, W, H, r):
    return skin(c, f, W, H, r, gloss=1.3, crease=0.35)


def rib(c, f, W, H, r):
    return skin(c, f, W, H, r, mid=(72, 60, 52), hi=(150, 136, 122), gloss=1.4)


def skin_chest(c, f, W, H, r):
    px = skin(c, f, W, H, r, lo=(6, 5, 5), mid=(26, 21, 18))
    yy, xx = np.mgrid[0:H, 0:W]
    if f in ('north', 'east', 'west'):
        k = (yy % 6 >= 4)
        px[k, :3] *= 0.55          # shadows between ribs
    return px


def sternum(c, f, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise(H, W, r, 3); n2 = noise(H, W, r, 1.2, 2)
    col = mix((112, 104, 96), (204, 198, 188), n * 0.9 + 0.2)
    rib_gap = (yy % 5 == 4)
    col[rib_gap] = mix(col[rib_gap], (60, 52, 46), 0.45 + 0.3 * n2[rib_gap])
    mid = np.abs(xx - (W - 1) / 2) < 0.6
    col[mid] = mix(col[mid], (80, 72, 66), 0.4)
    stain = n2 > 0.62
    col[stain] = mix(col[stain], (96, 82, 66), 0.5)
    edge = (xx == 0) | (xx == W - 1)
    col[edge] = mix(col[edge], (30, 24, 22), 0.7)
    return rgba(col * SHADE[f])


def skull(c, f, W, H, r):
    return skin(c, f, W, H, r, lo=(5, 4, 4), mid=(22, 18, 16), gloss=0.9)


def face(c, f, W, H, r):
    px = skin(c, f, W, H, r, lo=(3, 3, 3), mid=(16, 13, 12), gloss=0.5)
    if f == 'north':
        # deep eye sockets with tiny glinting pupils, slit of a mouth
        for ex in (2, W - 4):
            px[1:4, ex:ex + 2, :3] = (1, 1, 1)
            px[2, ex + (1 if ex < W / 2 else 0), :3] = (170, 160, 140)
        px[H - 2, 2:W - 2, :3] = (2, 2, 2)
        px[H - 2, 3:W - 3:2, :3] = (70, 62, 54)
    return px


def jaw(c, f, W, H, r):
    px = skin(c, f, W, H, r, lo=(4, 3, 3), mid=(18, 15, 13), gloss=0.7)
    if f == 'north':
        px[0, :, :3] = (2, 2, 2)
        px[0, 1::2, :3] = (120, 110, 96)     # small teeth
    return px


def knuckle(c, f, W, H, r):
    return skin(c, f, W, H, r, mid=(50, 42, 37), gloss=1.5, crease=0.5)


def finger(c, f, W, H, r):
    return skin(c, f, W, H, r, mid=(38, 31, 27), gloss=1.6)


def claw(c, f, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    t = yy / max(H - 1, 1)
    col = mix((60, 52, 44), (196, 186, 166), t)
    return rgba(col * SHADE[f])


def claw_tip(c, f, W, H, r):
    col = np.zeros((H, W, 3)) + (214, 206, 188)
    return rgba(col * SHADE[f])


def straw(c, f, W, H, r, base=(188, 162, 118), dark=(116, 92, 60), light=(222, 204, 160)):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise(H, W, r, 4)
    col = mix(dark, base, 0.55 + (n - 0.5) * 0.9)
    # woven straw: diagonal over/under strands
    w1 = ((xx + yy) // 2) % 2 == 0
    w2 = ((xx - yy) // 2) % 2 == 0
    col[w1 & ~w2] = mix(col[w1 & ~w2], light, 0.35)
    col[~w1 & w2] = mix(col[~w1 & w2], dark, 0.35)
    col[(xx + yy) % 4 == 0] *= 0.82
    grime = noise(H, W, r, 6) > 0.7
    col[grime] = mix(col[grime], (96, 78, 54), 0.45)
    return col


def straw_mat(c, f, W, H, r):
    col = straw(c, f, W, H, r)
    if f == 'up':
        col = mix(col, (150, 124, 86), 0.2)
    return rgba(col * SHADE[f])


def straw_dark(c, f, W, H, r):
    return rgba(straw(c, f, W, H, r) * 0.72 * SHADE[f])


def brim(c, f, W, H, r):
    col = straw(c, f, W, H, r)
    if f == 'down':
        col = col * 0.5
    elif f == 'up':
        yy, xx = np.mgrid[0:H, 0:W]
        col = mix(col, (230, 214, 172), 0.12 * (yy < H * 0.3))
    return rgba(col * (1.0 if f in ('up', 'down') else 0.8))


def brim_rim(c, f, W, H, r):
    col = straw(c, f, W, H, r, base=(162, 136, 96), dark=(96, 76, 50))
    return rgba(col * SHADE[f])


def band(c, f, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise(H, W, r, 3)
    col = mix((14, 10, 9), (48, 34, 28), n)
    col[yy == 0] = mix(col[yy == 0], (90, 70, 56), 0.5)
    return rgba(col * SHADE[f])


def leather(c, f, W, H, r, lo=(52, 34, 22), hi=(140, 98, 60)):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise(H, W, r, 4); n2 = noise(H, W, r, 1.5, 2)
    col = mix(lo, hi, 0.35 + (n - 0.5) * 1.0 + (n2 - 0.5) * 0.3)
    # creases + stitching + scuffs
    k = (r.random((H, W)) < 0.05)
    col[k] = mix(col[k], (24, 16, 10), 0.6)
    if f in ('north', 'south', 'east', 'west') and H > 2:
        col[1, ::2] = mix(col[1, ::2], (196, 170, 130), 0.55)
    if f == 'up':
        col[(xx + yy * 3) % 9 == 0] = mix(col[(xx + yy * 3) % 9 == 0], (30, 20, 12), 0.5)
    sc = noise(H, W, r, 3) > 0.72
    col[sc] = mix(col[sc], (170, 136, 98), 0.4)
    return rgba(col * SHADE[f])


def leather_dark(c, f, W, H, r):
    return leather(c, f, W, H, r, lo=(36, 24, 16), hi=(96, 66, 40))


def sole(c, f, W, H, r):
    n = noise(H, W, r, 3)
    col = mix((18, 14, 12), (54, 42, 34), n)
    return rgba(col * SHADE[f])


def shoe_hole(c, f, W, H, r):
    col = np.zeros((H, W, 3)) + (6, 5, 5)
    return rgba(col)


PAINTERS = {
    'skin': skin, 'sinew': sinew, 'skin_joint': skin_joint, 'skin_bone': skin_bone, 'skin_belly': skin_belly,
    'skin_hand': skin_hand, 'rib': rib, 'skin_chest': skin_chest, 'sternum': sternum, 'skull': skull, 'face': face,
    'jaw': jaw, 'knuckle': knuckle, 'finger': finger, 'claw': claw, 'claw_tip': claw_tip, 'straw': straw_mat,
    'straw_dark': straw_dark, 'brim': brim, 'brim_rim': brim_rim, 'band': band, 'leather': leather,
    'leather_dark': leather_dark, 'sole': sole, 'shoe_hole': shoe_hole,
}
