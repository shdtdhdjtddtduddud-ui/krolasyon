"""Block textures of the Crimson Realm."""
import math
import numpy as np
from PIL import Image
from px import rng, hexc, ramp, tile_noise, voronoi, img, shade, mix

N = 16
YY, XX = np.mgrid[0:N, 0:N]


def stone(seed, stops, cells=7, crack=0.25, grain=0.35):
    r = rng(seed)
    cid, d1, d2 = voronoi(N, r, cells)
    edge = np.clip((d2 - d1) / 1.6, 0, 1)
    tone = r.random(cells)[cid]
    n = tile_noise(N, r, 4, 3)
    t = 0.25 + 0.4 * tone + grain * (n - 0.5) + 0.25 * (edge - 0.5)
    t = np.where(edge < 0.28, t * 0.45, t)
    # highlight upper-left inside each cell
    t = t + 0.08 * (np.roll(edge, 1, 0) > edge) - 0.04
    rgb = ramp(stops, t)
    if crack:
        c = (edge < 0.18) & (r.random((N, N)) < crack)
        rgb[c] = shade(rgb[c], 0.6)
    return rgb


def bricks(seed, stops, mortar='#140406', bh=4, bw=8, bevel=0.22, var=0.25):
    r = rng(seed)
    rgb = np.zeros((N, N, 3), np.float32)
    n = tile_noise(N, r, 2, 2)
    for row in range(N // bh):
        off = (bw // 2) * (row % 2)
        for col in range(-1, N // bw + 1):
            x0 = col * bw + off
            tone = 0.45 + (r.random() - 0.5) * var * 2
            for y in range(row * bh, row * bh + bh):
                for x in range(x0, x0 + bw):
                    if not (0 <= x < N):
                        continue
                    ly, lx = y - row * bh, x - x0
                    if ly == bh - 1 or lx == bw - 1:
                        rgb[y, x] = hexc(mortar)
                        continue
                    t = tone + 0.25 * (n[y, x] - 0.5)
                    if ly == 0 or lx == 0:
                        t += bevel
                    if ly == bh - 2 or lx == bw - 2:
                        t -= bevel * 0.7
                    rgb[y, x] = ramp(stops, np.array(t))
    return rgb


def soil(seed, stops, speck=None, speck_p=0.05, cell=4):
    r = rng(seed)
    n = tile_noise(N, r, cell, 3)
    g = r.random((N, N))
    t = 0.2 + 0.6 * n + 0.25 * (g - 0.5)
    rgb = ramp(stops, t)
    if speck:
        m = r.random((N, N)) < speck_p
        rgb[m] = hexc(speck)
    return rgb


def top_side(top_rgb, base_rgb, seed, drip=4):
    """grass-like side: top layer hanging down with ragged edge"""
    r = rng(seed)
    side = base_rgb.copy()
    depth = np.clip(2 + r.integers(0, drip, N) - (r.random(N) < 0.3) * 1, 1, 6)
    for x in range(N):
        for y in range(depth[x]):
            side[y, x] = top_rgb[(y + 3) % N, x] * (1.0 - 0.06 * y)
        side[depth[x], x] = shade(side[depth[x], x], 0.6)
    return side


def ore(base, seed, stops, clusters=4, glint='#ffffff'):
    r = rng(seed)
    rgb = base.copy()
    m = np.zeros((N, N), bool)
    for _ in range(clusters):
        cx, cy = r.integers(2, N - 2, 2)
        x, y = cx, cy
        for _ in range(r.integers(5, 9)):
            m[y % N, x % N] = True
            x, y = x + r.integers(-1, 2), y + r.integers(-1, 2)
    # dark outline around ore
    o = np.zeros_like(m)
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        o |= np.roll(np.roll(m, dy, 0), dx, 1)
    o &= ~m
    rgb[o] = shade(rgb[o], 0.55)
    t = r.random((N, N)) * 0.6 + 0.2
    up = np.roll(m, 1, 0)
    t = np.where(~up & m, t + 0.35, t)
    rgb[m] = ramp(stops, t)[m]
    gl = m & (r.random((N, N)) < 0.18)
    rgb[gl] = hexc(glint)
    return rgb


def log_side(seed, stops, vein=None):
    r = rng(seed)
    n = tile_noise(N, r, 4, 2)
    rgb = np.zeros((N, N, 3), np.float32)
    phase = r.random(N) * 6
    for x in range(N):
        groove = 0.5 + 0.5 * math.sin(x * 1.7 + phase[x] * 0.2)
        for y in range(N):
            t = 0.3 + 0.35 * groove + 0.25 * (n[y, x] - 0.5) + 0.1 * math.sin(y * 0.9 + phase[x])
            rgb[y, x] = ramp(stops, np.array(t))
    if vein:
        for x in r.choice(N, 3, replace=False):
            y0 = r.integers(0, N)
            for k in range(r.integers(4, 10)):
                rgb[(y0 + k) % N, x] = hexc(vein)
    return rgb


def log_top(seed, bark, ring_stops, core=None):
    r = rng(seed)
    d = np.hypot(XX - 7.5, YY - 7.5)
    rings = 0.5 + 0.5 * np.sin(d * 2.2 + tile_noise(N, r, 4, 2) * 1.5)
    rgb = ramp(ring_stops, 0.3 + 0.5 * rings - d * 0.02)
    edge = (XX == 0) | (YY == 0) | (XX == N - 1) | (YY == N - 1)
    rgb[edge] = ramp(bark, r.random((N, N)) * 0.6 + 0.2)[edge]
    if core:
        rgb[d < 1.6] = hexc(core)
    return rgb


def planks(seed, stops):
    r = rng(seed)
    rgb = np.zeros((N, N, 3), np.float32)
    n = tile_noise(N, r, 8, 2)
    for row in range(4):
        tone = 0.4 + (r.random() - 0.5) * 0.3
        cut = r.integers(3, 13)
        for y in range(row * 4, row * 4 + 4):
            for x in range(N):
                t = tone + 0.15 * math.sin(x * 0.8 + row * 3 + n[y, x] * 4) + 0.1 * (n[y, x] - 0.5)
                if y == row * 4 + 3:
                    t = 0.05
                elif y == row * 4:
                    t += 0.12
                if x == cut and y != row * 4 + 3:
                    t -= 0.25
                rgb[y, x] = ramp(stops, np.array(t))
    return rgb


def wart(seed, stops, spot='#ffd0a0', spots=6):
    r = rng(seed)
    cid, d1, d2 = voronoi(N, r, 10)
    bump = 1 - np.clip(d1 / 4.0, 0, 1)
    t = 0.2 + 0.6 * bump + 0.1 * r.random((N, N))
    rgb = ramp(stops, t)
    for _ in range(spots):
        x, y = r.integers(0, N, 2)
        rgb[y, x] = hexc(spot)
        rgb[(y + 1) % N, x] = mix(rgb[(y + 1) % N, x], hexc(spot), 0.4)
    return rgb


def crystal(seed, stops, facets=6):
    r = rng(seed)
    cid, d1, d2 = voronoi(N, r, facets)
    ang = r.random(facets)[cid]
    t = 0.2 + 0.6 * ang + 0.25 * np.clip(1 - (d2 - d1) / 3, 0, 1)
    rgb = ramp(stops, t)
    edge = (d2 - d1) < 0.7
    rgb[edge] = mix(rgb[edge], hexc(stops[-1]), 0.6)
    for _ in range(3):
        x, y = r.integers(1, N - 1, 2)
        rgb[y, x] = hexc('#ffffff')
    return rgb


def veins(rgb, seed, col, count=3, length=10, glow_col=None):
    r = rng(seed)
    out = rgb.copy()
    for _ in range(count):
        x, y = r.random() * N, r.random() * N
        a = r.random() * math.tau
        for _ in range(length):
            a += r.normal(0, 0.6)
            x = (x + math.cos(a)) % N
            y = (y + math.sin(a)) % N
            out[int(y), int(x)] = hexc(col)
            if glow_col:
                for dx, dy in ((1, 0), (0, 1)):
                    X, Y = (int(x) + dx) % N, (int(y) + dy) % N
                    out[Y, X] = mix(out[Y, X], hexc(glow_col), 0.35)
    return out


# ---------------------------------------------------------------- cross plants (alpha)
def plant(kind, seed):
    r = rng(seed)
    rgb = np.zeros((N, N, 3), np.float32)
    a = np.zeros((N, N), np.float32)

    def px_(x, y, c, al=255):
        if 0 <= x < N and 0 <= y < N:
            rgb[y, x] = hexc(c) if isinstance(c, str) else c
            a[y, x] = al

    if kind == 'ember_flower':
        for y in range(7, 16):
            px_(8 + (1 if y < 10 else 0), y, '#3a1a10')
        for (x, y) in ((6, 12), (5, 11), (11, 13), (12, 12)):
            px_(x, y, '#5a2412')
        petals = [(-2, 0), (2, 0), (0, -2), (0, 2), (-1, -1), (1, 1), (1, -1), (-1, 1), (-3, 0), (3, 0), (0, -3)]
        for dx, dy in petals:
            d = abs(dx) + abs(dy)
            px_(9 + dx, 6 + dy, '#ff5a1a' if d >= 3 else '#ff9a2a' if d == 2 else '#ffcf4a')
        px_(9, 6, '#fff6c0')
        px_(4, 5, '#ffb040')
        px_(13, 3, '#ff7a20')
    elif kind == 'soul_lily':
        for y in range(6, 16):
            px_(7, y, '#1e3a40')
        for (x, y, c) in ((5, 13, '#1e4a4a'), (4, 12, '#2a5a5a'), (9, 14, '#1e4a4a'), (10, 13, '#2a5a5a')):
            px_(x, y, c)
        cup = [(5, 3), (6, 4), (7, 4), (8, 4), (9, 3), (5, 2), (9, 2), (6, 5), (7, 5), (8, 5), (4, 1), (10, 1), (7, 3)]
        for x, y in cup:
            px_(x, y + 1, '#bff6ff' if y < 3 else '#5fd8f0')
        px_(7, 4, '#ffffff')
        px_(7, 2, '#e0ffff')
        px_(12, 6, '#8fe8ff', 200)
    elif kind == 'blood_thorn':
        for s in range(4):
            x0 = 3 + s * 3 + r.integers(-1, 2)
            h = r.integers(8, 14)
            for k in range(h):
                x = x0 + int(round(math.sin(k * 0.6 + s) * 0.8))
                y = 15 - k
                px_(x, y, '#5a0a12' if k % 3 else '#8a1420')
                if k % 3 == 1:
                    px_(x + (1 if s % 2 else -1), y, '#c82030')
            px_(x0, 15 - h, '#ff4050')
    elif kind == 'shadow_fern':
        for s, (x0, lean) in enumerate(((4, -0.4), (8, 0.0), (11, 0.35))):
            h = 9 + s % 2 * 3
            for k in range(h):
                x = int(round(x0 + lean * k))
                y = 15 - k
                px_(x, y, '#2a1040')
                if k > 2 and k % 2 == 0:
                    px_(x - 1, y, '#6a2aa8')
                    px_(x + 1, y - 1, '#8a4ae0')
            px_(int(round(x0 + lean * h)), 15 - h, '#d6a8ff')
    return img(rgb, a)


# ---------------------------------------------------------------- portal (animated)
def portal(frames=32):
    out = np.zeros((N * frames, N, 4), np.uint8)
    r = rng(99)
    base = tile_noise(N, r, 4, 3)
    for f in range(frames):
        t = f / frames
        ang = np.arctan2(YY - 7.5, XX - 7.5)
        d = np.hypot(YY - 7.5, XX - 7.5)
        swirl = 0.5 + 0.5 * np.sin(ang * 3 + d * 0.9 - t * math.tau * 2 + base * 3)
        v = 0.35 * swirl + 0.45 * np.roll(base, f // 2, 0) + 0.2 * (0.5 + 0.5 * np.sin(d - t * math.tau))
        rgb = ramp(['#2a0006', '#7a0618', '#e01a30', '#ff7a30', '#ffd890'], v)
        a = 170 + 70 * v
        out[f * N:(f + 1) * N, :, :3] = np.clip(rgb, 0, 255)
        out[f * N:(f + 1) * N, :, 3] = np.clip(a, 0, 255)
    return Image.fromarray(out, 'RGBA')


# ---------------------------------------------------------------- environment
def eclipse(n=256):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32) + 0.5
    d = np.hypot(xx - n / 2, yy - n / 2) / (n / 2)
    ang = np.arctan2(yy - n / 2, xx - n / 2)
    r = rng(5)
    flick = 0.5 + 0.5 * np.sin(ang * 13 + np.sin(ang * 5) * 2) * np.sin(ang * 7 + 1)
    corona = np.clip(1 - (d - 0.42) / (0.45 + 0.25 * flick), 0, 1) ** 2
    rgb = ramp(['#3a0004', '#c0100a', '#ff6a1a', '#ffe0a0'], corona * 1.1)
    a = corona * 255
    disk = d < 0.43
    rim = (d >= 0.40) & (d < 0.46)
    rgb[disk] = hexc('#0a0003')
    a[disk] = 255
    rgb[rim] = mix(rgb[rim], hexc('#fff0c0'), 0.8)
    a[rim] = 255
    return img(rgb, a)


def shattered_moon(n=128):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32) + 0.5
    r = rng(8)
    d = np.hypot(xx - n / 2, yy - n / 2) / (n / 2)
    tex = ramp(['#401010', '#8a3a30', '#d08a6a', '#f0c8a0'], 0.3 + 0.5 * (1 - d) + 0.2 * np.kron(r.random((16, 16)), np.ones((8, 8))) - 0.1)
    a = np.where(d < 0.62, 255, 0).astype(np.float32)
    # crack through the moon
    crack = np.abs((xx - n / 2) * 0.8 + (yy - n / 2) * 0.6 + 6 * np.sin(yy * 0.15)) < 2.2
    tex[crack & (d < 0.62)] = hexc('#ff4020')
    halo = (d >= 0.62) & (d < 0.9)
    tex[halo] = hexc('#ff5030')
    a[halo] = np.clip((0.9 - d[halo]) / 0.28, 0, 1) ** 2 * 110
    return img(tex, a)


# ---------------------------------------------------------------- all blocks
STONE = ['#1a0607', '#3a0f0e', '#5a1c16', '#7a2e22', '#9a4430']
OBS = ['#05020a', '#120822', '#22103a', '#3a1a5a']


def build():
    T = {}
    st = stone(1, STONE)
    T['infernal_stone'] = img(veins(st, 11, '#2a0806', 2, 8))
    T['infernal_bricks'] = img(bricks(2, ['#2a0a0a', '#4a1414', '#6a2018', '#8a3020']))
    cb = bricks(3, ['#2a0a0a', '#4a1414', '#6a2018', '#8a3020'], bh=8, bw=16)
    ring = (np.abs(np.hypot(XX - 7.5, YY - 7.5) - 4.5) < 0.9)
    rune = ((XX == 7) | (XX == 8)) & (YY > 3) & (YY < 12) | ((YY == 7) | (YY == 8)) & (XX > 4) & (XX < 11)
    cb[ring | rune] = hexc('#ff8a2a')
    cb[(np.abs(np.hypot(XX - 7.5, YY - 7.5) - 4.5) < 1.6) & ~ring & ~rune] *= 0.6
    T['chiseled_infernal_bricks'] = img(cb)
    ash = soil(4, ['#2a2626', '#4a4442', '#6e6662', '#948a84'], speck='#ff7a2a', speck_p=0.025)
    T['ash_soil'] = img(ash)
    moss = soil(5, ['#3a0410', '#6a0a1a', '#9a1626', '#c8303a'], speck='#ff7080', speck_p=0.03, cell=2)
    T['blood_moss_top'] = img(moss)
    T['blood_moss_side'] = img(top_side(moss, st, 6))
    turf = soil(7, ['#12081e', '#24103a', '#3a1a5a', '#5a2a8a'], speck='#c890ff', speck_p=0.03, cell=2)
    T['shadow_turf_top'] = img(turf)
    T['shadow_turf_side'] = img(top_side(turf, stone(8, OBS[:2] + STONE[1:3]), 9))
    sc = soil(10, ['#0a0606', '#1a1210', '#2a1e1a', '#3a2a24'])
    sc = veins(sc, 12, '#ff6a1a', 4, 12, '#ff3a10')
    T['scorched_earth_top'] = img(sc)
    T['scorched_earth_side'] = img(top_side(sc, st, 13, drip=3))
    T['crimson_sand'] = img(soil(14, ['#5a1a14', '#8a2a1e', '#b0442a', '#d0663a'], speck='#ffd0a0', speck_p=0.02))
    cbl = soil(15, ['#300006', '#5a000c', '#8a0614', '#c02030'], cell=4)
    rr = rng(16)
    for _ in range(7):
        x, y = rr.integers(0, N, 2)
        cbl[y, x] = hexc('#ff8090')
        cbl[(y + 1) % N, x] = mix(cbl[(y + 1) % N, x], hexc('#e04050'), 0.6)
    T['coagulated_blood'] = img(cbl)
    co = stone(17, OBS, cells=6, crack=0)
    co = veins(co, 18, '#ff1a2a', 3, 12, '#a00818')
    T['cursed_obsidian'] = img(co)
    T['infernal_steel_ore'] = img(ore(st, 19, ['#5a1a1a', '#a0303a', '#e06060', '#ffc0b0'], glint='#ffe0d0'))
    T['arcane_crystal_ore'] = img(ore(st, 20, ['#3a0a6a', '#7a2ad0', '#c070ff', '#f0d0ff'], clusters=3, glint='#ffffff'))
    sb = bricks(21, ['#4a0a10', '#8a1a20', '#c03030', '#e06060'], bh=8, bw=8, bevel=0.3, var=0.1)
    T['infernal_steel_block'] = img(sb)
    T['blood_stem'] = img(log_side(22, ['#2a0408', '#5a0a12', '#8a1a20', '#b02a2a'], vein='#ff3040'))
    T['blood_stem_top'] = img(log_top(23, ['#2a0408', '#5a0a12'], ['#4a0810', '#8a1a1a', '#c04040'], core='#ff5a5a'))
    T['blood_cap'] = img(wart(24, ['#4a0008', '#8a0a14', '#c01a24', '#e83a3a'], spot='#ffb070'))
    T['blood_planks'] = img(planks(25, ['#3a0810', '#6a1420', '#8a2028', '#a83030']))
    T['shadow_stem'] = img(log_side(26, ['#06030c', '#140a24', '#22123a', '#3a1e5a'], vein='#b070ff'))
    T['shadow_stem_top'] = img(log_top(27, ['#06030c', '#140a24'], ['#140a24', '#2a1648', '#4a2a7a'], core='#d0a0ff'))
    T['shadow_crystal'] = img(crystal(28, ['#2a0a5a', '#6a2ab0', '#a060f0', '#e0c0ff']))
    T['shadow_planks'] = img(planks(29, ['#0a0614', '#1a1030', '#2a1a48', '#3a2460']))
    for k in ('ember_flower', 'soul_lily', 'blood_thorn', 'shadow_fern'):
        T[k] = plant(k, sum(map(ord, k)))
    lan = bricks(30, ['#3a1a08', '#6a3010', '#8a4418', '#a05a20'], bh=16, bw=16, bevel=0.3)
    core = (XX > 2) & (XX < 13) & (YY > 2) & (YY < 13)
    glow = ramp(['#ff6a10', '#ffb030', '#ffe890', '#fffbe0'], 1 - np.hypot(XX - 7.5, YY - 7.5) / 8 + 0.1 * rng(31).random((N, N)))
    lan[core] = glow[core]
    bars = core & ((XX == 5) | (XX == 10) | (YY == 7))
    lan[bars] = hexc('#2a1006')
    T['infernal_lantern'] = img(lan)
    # altars per kingdom
    acc = {'ash': ('#ff8a2a', ['#1a1414', '#3a3230', '#5a4e48', '#7a6c64']),
           'blood': ('#ff2a3a', ['#200408', '#4a0a12', '#6a1420', '#8a2028']),
           'shadow': ('#b070ff', OBS),
           'legion': ('#ffc040', ['#141414', '#2a2a2c', '#46464a', '#626268']),
           'soul': ('#4fe0ff', ['#2a2622', '#5a544a', '#9a9480', '#d8d4c0'])}
    for fid, (glowc, stops) in acc.items():
        side = bricks(40 + len(fid), stops, bh=4, bw=16, bevel=0.25)
        side[6:10, 2:14] = hexc('#0a0404')
        rune_mask = (YY >= 7) & (YY <= 8) & (XX >= 3) & (XX <= 12) & (((XX + YY) % 3) != 0)
        side[rune_mask] = hexc(glowc)
        T[f'altar_{fid}_side'] = img(side)
        top = stone(50 + len(fid), stops, cells=5, crack=0)
        rr = np.hypot(XX - 7.5, YY - 7.5)
        top[(np.abs(rr - 5) < 0.8)] = hexc(glowc)
        top[rr < 2.2] = mix(hexc(glowc), hexc('#ffffff'), 0.4)
        T[f'altar_{fid}_top'] = img(top)
    gold = bricks(60, ['#5a3a08', '#a07010', '#e0b030', '#fff090'], bh=4, bw=4, bevel=0.35, var=0.1)
    T['throne_gold'] = img(gold)
    cush = soil(61, ['#3a0008', '#6a0010', '#9a1020', '#c02030'], cell=8)
    cush[(XX % 5 == 2) & (YY % 5 == 2)] = hexc('#ffd060')
    T['throne_cushion'] = img(cush)
    return T
