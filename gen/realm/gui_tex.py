"""GUI background, spell orb sprite and armour layer textures."""
import math
import numpy as np
from px import rng, hexc, ramp, tile_noise, img, mix


def spell_orb(n=32):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32) + 0.5
    d = np.hypot(xx - n / 2, yy - n / 2) / (n / 2)
    ang = np.arctan2(yy - n / 2, xx - n / 2)
    rays = 0.5 + 0.5 * np.cos(ang * 6)
    core = np.clip(1 - d * 1.6, 0, 1)
    halo = np.clip(1 - d, 0, 1) ** 2 * (0.6 + 0.4 * rays)
    v = np.clip(core + halo * 0.8, 0, 1)
    rgb = np.dstack([v * 255] * 3)
    return img(rgb, v * 255)


def chronicle(w=320, h=216):
    r = rng(77)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    n = np.kron(tile_noise(64, r, 8, 4), np.ones((4, 5)))[:h, :w]
    # leather frame
    rgb = ramp(['#1a0606', '#3a0e0c', '#5a1a14', '#6e2418'], 0.3 + 0.5 * n)
    edge = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))
    rgb[edge < 2] = hexc('#0a0202')
    # gold inner border
    inner = (edge >= 7) & (edge < 9)
    rgb[inner] = hexc('#c89a3a')
    rgb[(edge >= 9) & (edge < 10)] = hexc('#6a4a18')
    # parchment page (dark, aged)
    page = edge >= 10
    pn = np.kron(tile_noise(64, rng(78), 4, 4), np.ones((4, 5)))[:h, :w]
    vign = np.clip(1 - (np.abs(xx - w / 2) / (w / 2)) ** 3 - (np.abs(yy - h / 2) / (h / 2)) ** 3, 0, 1)
    prgb = ramp(['#140808', '#22100c', '#2e1812', '#3a2016'], 0.25 + 0.45 * pn * vign + 0.2 * vign)
    rgb[page] = prgb[page]
    # spine shadow in the middle (open book)
    spine = page & (np.abs(xx - w / 2) < 6)
    rgb[spine] = rgb[spine] * (0.55 + 0.45 * (np.abs(xx[spine] - w / 2) / 6))[:, None]
    # gold corner ornaments
    for cx, cy in ((12, 12), (w - 13, 12), (12, h - 13), (w - 13, h - 13)):
        d = np.hypot(xx - cx, yy - cy)
        rgb[(d < 5) & (d > 3)] = hexc('#e8c060')
        rgb[d < 2] = hexc('#ff4030')
    # header flourish
    hl = (np.abs(yy - 33) < 0.6) & (np.abs(xx - w / 2) < w * 0.35)
    rgb[hl] = hexc('#8a6428')
    return img(rgb)


def armor_layer(stops, trim, glow, layer, crown=False):
    """64x32 humanoid armour layout"""
    W, H = 64, 32
    r = rng(5 + layer)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    n = np.kron(tile_noise(16, r, 4, 3), np.ones((2, 4)))
    rgb = ramp(stops, 0.35 + 0.4 * n)
    a = np.zeros((H, W), np.float32)
    if crown:
        # only the hat layer: a gold band around the head with spikes
        band = (yy >= 12) & (yy < 16) & (xx >= 32)
        spikes = (yy >= 8) & (yy < 12) & (xx >= 32) & ((xx.astype(int) % 4) == 1) & (yy >= 8 + ((xx.astype(int) // 4) % 2) * 2)
        crown_m = band | spikes
        rgb[crown_m] = ramp(['#6a4008', '#c08a18', '#ffd040', '#fff4a0'], 0.4 + 0.5 * n[crown_m] + 0.2 * (yy[crown_m] < 13))
        gem = band & (yy == 14) & ((xx.astype(int) % 8) == 4)
        rgb[gem] = hexc('#ff2030')
        a[crown_m] = 255
        return img(rgb, a)
    regions = []
    if layer == 1:
        regions = [(0, 0, 32, 16), (16, 16, 24, 16), (40, 16, 16, 16), (0, 16, 16, 16)]   # head, body, arms, boots(legs)
    else:
        regions = [(0, 16, 16, 16), (16, 16, 24, 16)]   # legs, body (belt)
    for x0, y0, w, h in regions:
        m = (xx >= x0) & (xx < x0 + w) & (yy >= y0) & (yy < y0 + h)
        a[m] = 255
    # plate seams, rivets and glowing rune trim
    seam = ((yy.astype(int) % 4) == 3) & (a > 0)
    rgb[seam] = mix(rgb[seam], hexc(stops[0]), 0.6)
    hi = ((yy.astype(int) % 4) == 0) & (a > 0)
    rgb[hi] = mix(rgb[hi], hexc(stops[-1]), 0.3)
    rivet = (a > 0) & ((xx.astype(int) % 6) == 2) & ((yy.astype(int) % 8) == 1)
    rgb[rivet] = hexc(trim)
    if layer == 1:
        # helmet face: visor slit with glowing eyes, brow trim
        visor = (yy == 12) & (xx >= 9) & (xx < 15)
        rgb[(yy >= 11) & (yy <= 13) & (xx >= 8) & (xx < 16)] = hexc('#0a0204')
        rgb[visor & ((xx == 10) | (xx == 13))] = hexc(glow)
        rgb[(yy == 8) & (xx >= 8) & (xx < 16)] = hexc(trim)
        # chest emblem
        cx, cy = 24, 24
        d = np.abs(xx - cx + 0.5) + np.abs(yy - cy + 0.5)
        rgb[(d < 3) & (xx >= 20) & (xx < 28)] = hexc(glow)
        rgb[(d >= 3) & (d < 4) & (xx >= 20) & (xx < 28) & (yy >= 20)] = hexc(trim)
    return img(rgb, a)
