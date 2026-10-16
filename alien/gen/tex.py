"""Procedural 32x32 block / item textures and HUD images for the Sevastopol station."""
import math, os, numpy as np
from PIL import Image, ImageDraw, ImageFont
from hd import vnoise

R = np.random.default_rng(1979)
S = 32


def img(a):
    a = np.clip(a, 0, 255).astype(np.uint8)
    return Image.fromarray(a, 'RGBA' if a.shape[2] == 4 else 'RGB')


def base(col, var=10, cell=6, size=S, seed=None):
    r = np.random.default_rng(seed if seed is not None else R.integers(1 << 30))
    n = vnoise(size, size, r, cell=cell) - 0.5
    fine = r.random((size, size)) - 0.5
    a = np.zeros((size, size, 4), np.float32)
    for i in range(3):
        a[..., i] = col[i] + n * var * 2 + fine * var * 0.6
    a[..., 3] = 255
    return a


def rect(a, x0, y0, x1, y1, col, alpha=1.0):
    sub = a[y0:y1, x0:x1, :3]
    sub[:] = sub * (1 - alpha) + np.array(col, np.float32) * alpha


def shade(a, x0, y0, x1, y1, k):
    a[y0:y1, x0:x1, :3] *= k


def bevel(a, x0, y0, x1, y1, hi=1.18, lo=0.7, inset=False):
    if inset:
        hi, lo = lo, hi
    shade(a, x0, y0, x1, y0 + 1, hi)
    shade(a, x0, y0, x0 + 1, y1, hi)
    shade(a, x0, y1 - 1, x1, y1, lo)
    shade(a, x1 - 1, y0, x1, y1, lo)


def grime(a, amount=0.25, bottom=True, seed=None):
    r = np.random.default_rng(seed if seed is not None else R.integers(1 << 30))
    n = vnoise(a.shape[0], a.shape[1], r, cell=5)
    g = np.clip((n - 0.55) * 3, 0, 1) * amount
    if bottom:
        yy = np.linspace(0, 1, a.shape[0])[:, None]
        g = g + np.clip(yy - 0.6, 0, 1) * amount * 0.9
    a[..., :3] *= (1 - g[..., None] * 0.6)
    return a


def rivets(a, pts, col=(150, 150, 145)):
    for x, y in pts:
        a[y, x, :3] = col
        a[y + 1, x + 1, :3] *= 0.6


# ------------------------------------------------------------------ station surfaces
def station_wall():
    a = base((206, 204, 196), 7)
    bevel(a, 0, 0, S, S, 1.05, 0.62)
    bevel(a, 3, 3, S - 3, S - 3, inset=True)
    bevel(a, 5, 5, S - 5, S - 5)
    rect(a, 6, 8, 12, 9, (120, 120, 116), 0.6)          # label strip
    rivets(a, [(1, 1), (S - 3, 1), (1, S - 3), (S - 3, S - 3)])
    return grime(a, 0.18)


def station_wall_dark():
    a = base((66, 70, 62), 8)
    bevel(a, 0, 0, S, S, 1.15, 0.55)
    # nested blocky relief like the Nostromo wall panels
    for (x0, y0, x1, y1) in ((3, 3, 15, 14), (17, 3, 29, 14), (3, 17, 29, 29), (6, 20, 26, 26)):
        bevel(a, x0, y0, x1, y1, 1.3, 0.6)
    for (x0, y0, x1, y1) in ((6, 6, 12, 11), (20, 6, 26, 11)):
        bevel(a, x0, y0, x1, y1, 0.65, 1.25)
    rect(a, 9, 22, 23, 24, (140, 120, 60), 0.25)
    return grime(a, 0.25)


def graffiti(a, seed):
    r = np.random.default_rng(seed)
    im = img(a)
    d = ImageDraw.Draw(im)
    cols = [(60, 30, 90), (40, 110, 60), (20, 20, 22), (150, 40, 40), (70, 60, 140)]
    for _ in range(3):
        c = cols[r.integers(len(cols))]
        x, y = r.integers(2, 28), r.integers(4, 26)
        pts = [(x, y)]
        for _ in range(r.integers(4, 9)):
            x = int(np.clip(x + r.integers(-6, 7), 1, 30)); y = int(np.clip(y + r.integers(-4, 5), 1, 30))
            pts.append((x, y))
        d.line(pts, fill=c + (230,), width=1)
    return np.asarray(im).astype(np.float32)


def station_wall_dirty():
    a = station_wall()
    a = grime(a, 0.45, seed=4)
    return graffiti(a, 11)


def station_wall_green():
    a = base((92, 116, 70), 8)
    bevel(a, 0, 0, S, S, 1.12, 0.6)
    for y in (6, 7, 24, 25):
        rect(a, 0, y, S, y + 1, (60, 80, 45), 0.8)
    bevel(a, 3, 10, S - 3, 22, inset=True)
    rect(a, 5, 14, 9, 18, (210, 200, 180), 0.8)        # "LAB" sign plate
    rect(a, 6, 15, 8, 17, (170, 40, 40), 0.9)
    return grime(a, 0.3)


def station_floor():
    a = base((160, 160, 154), 8)
    bevel(a, 0, 0, S, S, 1.1, 0.55)
    bevel(a, 1, 1, 16, 16, 1.08, 0.75)
    bevel(a, 16, 16, S - 1, S - 1, 1.08, 0.75)
    # lift handles like the Sevastopol floor plates
    for x, y in ((5, 22), (21, 6)):
        rect(a, x, y, x + 6, y + 2, (70, 70, 68), 0.9)
        bevel(a, x, y, x + 6, y + 2, inset=True)
    return grime(a, 0.3, bottom=False)


def floor_grate():
    a = base((52, 54, 52), 6)
    for y in range(S):
        for x in range(S):
            if (x % 4 in (1, 2)) and (y % 4 in (1, 2)):
                a[y, x, :3] = (10, 11, 10)
    bevel(a, 0, 0, S, S, 1.2, 0.5)
    return grime(a, 0.2, bottom=False)


def station_ceiling():
    a = base((196, 196, 190), 6)
    for i in (0, 16):
        bevel(a, i, 0, i + 16, S, 1.08, 0.68)
    for y in (0, 16):
        shade(a, 0, y, S, y + 1, 0.7)
    return grime(a, 0.15, bottom=False)


def hazard_panel():
    a = base((200, 196, 186), 6)
    yy, xx = np.mgrid[0:S, 0:S]
    stripe = ((xx + yy) // 6) % 2 == 0
    a[stripe, :3] = np.array((176, 38, 34), np.float32) + (R.random((stripe.sum(), 1)) - 0.5) * 18
    bevel(a, 0, 0, S, S, 1.1, 0.55)
    return grime(a, 0.35)


def station_trim():
    a = base((86, 88, 84), 6)
    for y in range(2, S, 5):
        shade(a, 0, y, S, y + 1, 1.3)
        shade(a, 0, y + 1, S, y + 2, 0.6)
    bevel(a, 0, 0, S, S, 1.1, 0.55)
    return grime(a, 0.2)


def ceiling_light(on):
    a = base((180, 180, 176), 5)
    bevel(a, 0, 0, S, S, 1.1, 0.6)
    rect(a, 3, 9, S - 3, S - 9, (60, 62, 60), 1)
    if on:
        yy = np.abs(np.arange(9, S - 9) - (S / 2 - 0.5))[:, None]
        tube = np.clip(1 - yy / 7.5, 0, 1)
        sub = a[9:S - 9, 4:S - 4, :3]
        sub[:] = sub * (1 - tube[..., None]) + np.array((250, 252, 255), np.float32) * tube[..., None]
        sub[:] = np.maximum(sub, np.array((205, 225, 235), np.float32) * 0.85)
    else:
        rect(a, 4, 12, S - 4, S - 12, (120, 122, 120), 1)
    for x in range(4, S - 4, 6):
        rect(a, x, 9, x + 1, S - 9, (140, 140, 138), 0.5)
    return a


def emergency_light():
    a = base((70, 70, 70), 5)
    bevel(a, 0, 0, S, S)
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    m = d < 11
    a[m, :3] = np.array((255, 60, 40), np.float32) * (1 - d[m, None] / 22)
    a[m & (d < 5), :3] = (255, 170, 140)
    return a


def vent():
    a = base((72, 74, 72), 6)
    bevel(a, 0, 0, S, S, 1.2, 0.5)
    bevel(a, 3, 3, S - 3, S - 3, inset=True)
    for y in range(5, S - 5, 3):
        rect(a, 4, y, S - 4, y + 2, (14, 15, 15), 1)
        rect(a, 4, y, S - 4, y + 1, (100, 102, 100), 0.6)
    rivets(a, [(1, 1), (S - 3, 1), (1, S - 3), (S - 3, S - 3)], (40, 40, 40))
    return grime(a, 0.25)


def locker(part):
    a = base((126, 136, 140), 6)
    if part in ('front_lower', 'front_upper'):
        bevel(a, 0, 0, S, S, 1.1, 0.5)
        bevel(a, 2, 0 if part == 'front_lower' else 2, S - 2, S - 2 if part == 'front_lower' else S, 1.15, 0.7)
        if part == 'front_upper':
            for y in range(6, 18, 3):
                rect(a, 7, y, S - 7, y + 2, (20, 22, 24), 1)
                rect(a, 7, y + 2, S - 7, y + 3, (170, 180, 184), 0.5)
            rect(a, 10, 22, 22, 26, (200, 200, 190), 0.8)   # name tag
        else:
            rect(a, S - 8, 2, S - 6, 10, (40, 42, 44), 1)  # handle
            for y in range(18, 28, 3):
                rect(a, 7, y, S - 7, y + 2, (20, 22, 24), 1)
    elif part == 'side':
        bevel(a, 0, 0, S, S, 1.08, 0.6)
    else:
        bevel(a, 0, 0, S, S, 1.1, 0.6)
    return grime(a, 0.25)


def save_station(part):
    if part == 'front':
        a = base((54, 62, 82), 6)
        bevel(a, 0, 0, S, S, 1.2, 0.5)
        rect(a, 3, 2, S - 3, 6, (210, 210, 205), 1)         # EMERGENCY header
        for x in range(5, S - 5, 2):
            rect(a, x, 3, x + 1, 5, (40, 40, 40), 0.85)
        rect(a, 3, 7, S - 3, 8, (240, 240, 230), 0.9)         # strip light
        rect(a, 4, 9, S - 4, 13, (220, 180, 40), 1)          # yellow warning label
        rect(a, 5, 10, 14, 11, (40, 30, 10), 0.8)
        rect(a, 5, 14, S - 5, 24, (160, 162, 158), 1)         # control panel
        for (x, y, c) in ((7, 16, (230, 210, 120)), (11, 16, (230, 210, 120)), (15, 16, (230, 210, 120)),
                          (7, 20, (200, 200, 200)), (11, 20, (200, 200, 200)), (21, 16, (240, 200, 60)), (21, 20, (240, 200, 60))):
            rect(a, x, y, x + 2, y + 2, c, 1)
        rect(a, 6, 25, 9, 28, (190, 40, 40), 1)               # red / green buttons
        rect(a, 21, 25, 25, 28, (60, 200, 80), 1)
        rect(a, 13, 25, 18, 28, (40, 60, 120), 1)
        return a
    a = base((44, 52, 70), 5)
    bevel(a, 0, 0, S, S, 1.1, 0.6)
    return a


def terminal(part):
    if part == 'front':
        a = base((176, 170, 150), 5)
        bevel(a, 0, 0, S, S, 1.1, 0.55)
        rect(a, 3, 3, S - 3, 22, (10, 14, 10), 1)
        r = np.random.default_rng(5)
        for i, y in enumerate(range(5, 20, 3)):
            w = r.integers(8, 22)
            rect(a, 5, y, 5 + w, y + 1, (70, 255, 110), 0.9)
        shade(a, 3, 3, S - 3, 22, 1.0)
        rect(a, 4, 25, 10, 28, (90, 86, 76), 1)
        rect(a, 22, 25, 28, 27, (220, 120, 40), 1)
        return a
    a = base((166, 160, 142), 5)
    bevel(a, 0, 0, S, S, 1.1, 0.6)
    for y in range(6, S - 4, 3):
        rect(a, 6, y, S - 6, y + 1, (90, 88, 80), 0.7)
    return a


def desk(part):
    a = base((150, 152, 150) if part == 'top' else (110, 112, 110), 6)
    bevel(a, 0, 0, S, S, 1.12, 0.6)
    if part == 'top':
        rect(a, 4, 4, 14, 10, (200, 196, 180), 0.6)   # papers
        rect(a, 18, 18, 28, 24, (60, 60, 64), 0.5)
    return grime(a, 0.2, bottom=False)


def debris():
    a = np.zeros((S, S, 4), np.float32)
    r = np.random.default_rng(77)
    im = Image.fromarray(a.astype(np.uint8), 'RGBA')
    d = ImageDraw.Draw(im)
    for _ in range(9):
        x, y = r.integers(0, 28), r.integers(0, 28)
        w, h = r.integers(3, 8), r.integers(3, 7)
        c = [(225, 222, 210), (200, 196, 180), (150, 130, 100), (60, 64, 70), (190, 60, 50)][r.integers(5)]
        ang = r.random() * math.pi
        pts = [(x + math.cos(ang) * dx - math.sin(ang) * dy, y + math.sin(ang) * dx + math.cos(ang) * dy)
               for dx, dy in ((0, 0), (w, 0), (w, h), (0, h))]
        d.polygon(pts, fill=c + (255,))
    for _ in range(3):
        pts = [(int(r.integers(0, 32)), int(r.integers(0, 32))) for _ in range(4)]
        d.line(pts, fill=(25, 25, 25, 255), width=1)
    return np.asarray(im).astype(np.float32)


def door(color, part):
    a = base(color, 8)
    dark = np.array(color, np.float32) * 0.55
    bevel(a, 0, 0, S, S, 1.15, 0.5)
    if part == 'top':
        # octagonal window frame
        rect(a, 5, 6, S - 5, S, (25, 30, 32), 1)
        rect(a, 7, 8, S - 7, S, (40, 52, 58), 1)
        for i in range(4):
            rect(a, 7 + i, 8 + i, 8 + i, 9 + i, color, 1)
            rect(a, S - 8 - i, 8 + i, S - 7 - i, 9 + i, color, 1)
        rect(a, 15, 8, 17, S, dark, 1)                   # window bar
        a[9:14, 9:13, :3] = a[9:14, 9:13, :3] * 0.6 + 120 * 0.4   # glass glare
        rect(a, 0, 1, S, 3, dark, 0.7)
    else:
        rect(a, 5, 0, S - 5, 14, (25, 30, 32), 1)
        rect(a, 7, 0, S - 7, 12, (40, 52, 58), 1)
        for i in range(4):
            rect(a, 7 + i, 11 - i, 8 + i, 12 - i, color, 1)
            rect(a, S - 8 - i, 11 - i, S - 7 - i, 12 - i, color, 1)
        rect(a, 15, 0, 17, 12, dark, 1)
        rect(a, 21, 16, 26, 22, (30, 30, 30), 1)          # keypad
        rect(a, 22, 17, 25, 19, (90, 220, 120), 1)
        rect(a, 22, 20, 23, 21, (230, 60, 50), 1)
        rect(a, 0, S - 4, S, S - 2, dark, 0.8)
    return grime(a, 0.3)


def generator(part, fuses=0):
    a = base((150, 128, 40), 8)
    bevel(a, 0, 0, S, S, 1.15, 0.5)
    if part == 'front':
        rect(a, 3, 3, S - 3, 9, (30, 30, 28), 1)
        for i in range(3):
            x = 5 + i * 8
            rect(a, x, 12, x + 6, 26, (25, 25, 25), 1)
            if i < fuses:
                rect(a, x + 1, 13, x + 5, 25, (200, 200, 190), 1)
                rect(a, x + 2, 15, x + 4, 23, (240, 120, 40), 1)
            lit = (90, 255, 110) if i < fuses else (120, 20, 20)
            rect(a, x + 2, 5, x + 4, 7, lit, 1)
        for y in range(28, 31):
            rect(a, 3, y, S - 3, y + 1, (20, 20, 20) if y % 2 else (210, 180, 50), 1)
    elif part == 'side':
        for y in range(5, S - 5, 3):
            rect(a, 5, y, S - 5, y + 1, (60, 52, 20), 0.9)
    else:
        rect(a, 6, 6, S - 6, S - 6, (70, 66, 50), 1)
        bevel(a, 6, 6, S - 6, S - 6, inset=True)
    return grime(a, 0.3)


def seal(kind):
    if kind == 'power':
        a = hazard_panel()
        rect(a, 8, 10, S - 8, S - 10, (40, 40, 42), 1)
        bevel(a, 8, 10, S - 8, S - 10, inset=True)
        rect(a, 12, 13, S - 12, S - 13, (200, 30, 25), 1)
        return a
    a = base((52, 70, 110), 6)
    bevel(a, 0, 0, S, S, 1.15, 0.5)
    for y in (4, S - 6):
        rect(a, 2, y, S - 2, y + 2, (220, 190, 50), 1)
    rect(a, 11, 11, 21, 21, (20, 22, 26), 1)
    rect(a, 13, 13, 19, 14, (60, 60, 60), 1)
    rect(a, 14, 17, 18, 19, (230, 60, 40), 1)
    return grime(a, 0.25)


def console(part):
    a = base((170, 172, 168), 6)
    bevel(a, 0, 0, S, S, 1.12, 0.55)
    if part == 'front':
        rect(a, 3, 3, S - 3, 15, (8, 14, 10), 1)
        rect(a, 5, 5, 20, 7, (90, 255, 120), 0.9)
        rect(a, 5, 9, 14, 11, (90, 255, 120), 0.7)
        rect(a, 4, 18, S - 4, 21, (220, 190, 50), 1)
        rect(a, 12, 23, 20, 29, (200, 30, 30), 1)
        bevel(a, 12, 23, 20, 29, 1.3, 0.6)
    return grime(a, 0.2)


# ------------------------------------------------------------------ items (32x32 transparent)
def canvas():
    return Image.new('RGBA', (S, S), (0, 0, 0, 0))


def item_tracker():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rectangle((6, 4, 26, 19), fill=(70, 76, 58, 255), outline=(30, 32, 26, 255))
    d.rectangle((9, 6, 23, 16), fill=(6, 22, 10, 255))
    for r_ in (4, 7, 10):
        d.arc((16 - r_, 15 - r_, 16 + r_, 15 + r_), 200, 340, fill=(60, 200, 90, 255))
    d.point((14, 10), fill=(200, 255, 210, 255)); d.point((15, 10), fill=(200, 255, 210, 255))
    d.rectangle((13, 19, 19, 29), fill=(40, 42, 36, 255), outline=(20, 20, 18, 255))
    for y in range(20, 29, 2):
        d.line((13, y, 19, y), fill=(25, 25, 22, 255))
    d.rectangle((24, 7, 26, 9), fill=(150, 30, 30, 255))
    return im


def item_flamethrower():
    im = canvas(); d = ImageDraw.Draw(im)
    d.line((3, 26, 26, 6), fill=(60, 62, 60, 255), width=4)
    d.line((3, 26, 26, 6), fill=(100, 104, 100, 255), width=1)
    d.rectangle((10, 16, 18, 22), fill=(170, 60, 30, 255), outline=(60, 20, 10, 255))   # fuel tank
    d.line((14, 20, 12, 28), fill=(40, 40, 40, 255), width=3)                          # grip
    d.polygon([(26, 3), (30, 6), (28, 10), (24, 8)], fill=(40, 40, 40, 255))
    d.point((29, 3), fill=(255, 160, 40, 255)); d.point((30, 2), fill=(255, 220, 90, 255))
    return im


def item_fuel():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rounded_rectangle((9, 7, 23, 28), 3, fill=(190, 70, 30, 255), outline=(70, 25, 10, 255))
    d.rectangle((13, 3, 19, 7), fill=(60, 60, 60, 255))
    d.rectangle((11, 13, 21, 19), fill=(230, 200, 60, 255))
    d.line((12, 16, 20, 16), fill=(40, 30, 10, 255))
    return im


def item_noisemaker():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rounded_rectangle((8, 9, 24, 26), 3, fill=(80, 84, 78, 255), outline=(30, 30, 28, 255))
    d.ellipse((12, 13, 20, 21), fill=(30, 30, 30, 255))
    d.ellipse((14, 15, 18, 19), fill=(230, 50, 40, 255))
    d.line((16, 9, 16, 4), fill=(50, 50, 50, 255), width=2)
    d.rectangle((10, 23, 22, 24), fill=(220, 190, 50, 255))
    return im


def item_flare():
    im = canvas(); d = ImageDraw.Draw(im)
    d.line((8, 25, 22, 9), fill=(200, 40, 35, 255), width=4)
    d.line((8, 25, 11, 22), fill=(230, 230, 220, 255), width=4)
    d.ellipse((20, 4, 28, 12), fill=(255, 120, 60, 200))
    d.ellipse((22, 6, 26, 10), fill=(255, 230, 150, 255))
    return im


def item_flashlight(on):
    im = canvas(); d = ImageDraw.Draw(im)
    if on:
        d.polygon([(20, 8), (31, 0), (31, 20)], fill=(255, 250, 200, 110))
    d.line((4, 26, 18, 12), fill=(50, 52, 54, 255), width=5)
    d.polygon([(16, 8), (24, 16), (20, 20), (12, 12)], fill=(80, 84, 86, 255))
    d.line((17, 10, 22, 15), fill=(250, 245, 200, 255) if on else (150, 150, 140, 255), width=2)
    d.point((9, 21), fill=(200, 50, 40, 255))
    return im


def item_medkit():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rounded_rectangle((5, 9, 27, 26), 3, fill=(225, 225, 218, 255), outline=(90, 90, 86, 255))
    d.rectangle((14, 12, 18, 23), fill=(200, 40, 40, 255))
    d.rectangle((10, 16, 22, 19), fill=(200, 40, 40, 255))
    d.rectangle((12, 6, 20, 9), outline=(90, 90, 86, 255))
    return im


def item_station_core():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rectangle((6, 6, 26, 26), fill=(190, 190, 184, 255), outline=(50, 50, 50, 255))
    d.rectangle((9, 9, 23, 23), fill=(20, 26, 22, 255))
    d.ellipse((12, 12, 20, 20), outline=(90, 255, 120, 255))
    d.line((16, 6, 16, 9), fill=(200, 40, 40, 255))
    d.rectangle((15, 15, 17, 17), fill=(200, 255, 210, 255))
    return im


def item_fuse():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rectangle((11, 5, 21, 27), fill=(210, 210, 200, 255), outline=(60, 60, 60, 255))
    d.rectangle((13, 9, 19, 23), fill=(240, 120, 40, 255))
    d.rectangle((11, 2, 21, 5), fill=(150, 150, 150, 255))
    d.rectangle((11, 27, 21, 30), fill=(150, 150, 150, 255))
    d.line((16, 10, 16, 22), fill=(255, 220, 120, 255))
    return im


def item_keycard():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rounded_rectangle((4, 8, 28, 24), 2, fill=(225, 225, 215, 255), outline=(70, 70, 70, 255))
    d.rectangle((4, 11, 28, 14), fill=(40, 40, 40, 255))
    d.rectangle((7, 17, 13, 21), fill=(220, 180, 50, 255))
    d.rectangle((16, 17, 25, 18), fill=(90, 90, 120, 255))
    d.rectangle((16, 20, 22, 21), fill=(90, 90, 120, 255))
    return im


def door_item(color):
    im = canvas(); d = ImageDraw.Draw(im)
    c = tuple(int(v) for v in color) + (255,)
    dk = tuple(int(v * 0.55) for v in color) + (255,)
    d.rectangle((7, 2, 24, 30), fill=c, outline=dk)
    d.rectangle((10, 6, 21, 16), fill=(35, 45, 50, 255), outline=dk)
    d.line((15, 6, 15, 16), fill=dk); d.line((16, 6, 16, 16), fill=dk)
    d.rectangle((19, 20, 22, 23), fill=(30, 30, 30, 255))
    d.point((20, 21), fill=(90, 220, 120, 255))
    return im


# ------------------------------------------------------------------ tracker 3d item textures
def tracker_body():
    a = base((74, 80, 62), 6, size=32)
    bevel(a, 0, 0, 32, 32, 1.15, 0.6)
    for y in range(4, 28, 4):
        rect(a, 3, y, 29, y + 1, (40, 44, 34), 0.8)
    rect(a, 4, 22, 12, 26, (220, 190, 50), 0.9)
    rect(a, 22, 4, 26, 8, (170, 40, 40), 1)
    return grime(a, 0.25)


def tracker_screen():
    a = np.zeros((32, 32, 4), np.float32)
    a[..., :3] = (6, 24, 10); a[..., 3] = 255
    im = img(a); d = ImageDraw.Draw(im)
    for r_ in (8, 15, 22):
        d.arc((16 - r_, 30 - r_, 16 + r_, 30 + r_), 200, 340, fill=(60, 200, 90, 255))
    d.line((16, 30, 4, 12), fill=(30, 120, 50, 255)); d.line((16, 30, 28, 12), fill=(30, 120, 50, 255))
    d.rectangle((15, 29, 17, 31), fill=(150, 255, 170, 255))
    d.rectangle((20, 12, 21, 13), fill=(200, 255, 210, 255))
    d.text((2, 1), "12m", fill=(90, 255, 120, 255))
    return np.asarray(im).astype(np.float32)


def tracker_grip():
    a = base((40, 42, 38), 4, size=32)
    for y in range(0, 32, 3):
        rect(a, 0, y, 32, y + 1, (20, 20, 18), 0.9)
    return a


# ------------------------------------------------------------------ hud images
def grain():
    r = np.random.default_rng(3)
    n = r.random((256, 256))
    a = np.zeros((256, 256, 4), np.float32)
    v = (n > 0.5)
    a[..., 0] = a[..., 1] = a[..., 2] = np.where(v, 255, 0)
    a[..., 3] = np.abs(n - 0.5) * 2 * 255 * 0.7
    # occasional scratches
    for _ in range(14):
        x = r.integers(0, 256)
        a[:, x, :3] = 230
        a[:, x, 3] = np.maximum(a[:, x, 3], 60 * r.random(256))
    return a


def vignette():
    yy, xx = np.mgrid[0:256, 0:256].astype(np.float32)
    d = np.sqrt(((xx - 127.5) / 128) ** 2 * 1.0 + ((yy - 127.5) / 128) ** 2 * 1.15)
    al = np.clip((d - 0.55) / 0.6, 0, 1) ** 1.6
    a = np.zeros((256, 256, 4), np.float32)
    a[..., :3] = (4, 6, 5)
    a[..., 3] = al * 255
    return a


def locker_slats():
    a = np.zeros((256, 256, 4), np.float32)
    a[..., :3] = (8, 9, 10)
    a[..., 3] = 250
    top, bot = 92, 164
    n = 5
    h = (bot - top) / (n * 2)
    for i in range(n):
        y0 = int(top + (2 * i + 0.5) * h)
        y1 = int(y0 + h * 0.9)
        a[y0:y1, 30:226, 3] = 0
        a[y0:y0 + 1, 30:226, :3] = 70
        a[y0:y0 + 1, 30:226, 3] = 200
        a[y1:y1 + 2, 30:226, :3] = 30
    # slight metal texture on the inside of the door
    r = np.random.default_rng(9)
    a[..., :3] += (r.random((256, 256, 1)) * 8)
    return a


def save_all(root):
    bt = os.path.join(root, 'textures/block'); it = os.path.join(root, 'textures/item'); mt = os.path.join(root, 'textures/misc')
    for d in (bt, it, mt):
        os.makedirs(d, exist_ok=True)
    blocks = {
        'station_wall': station_wall(), 'station_wall_dark': station_wall_dark(), 'station_wall_dirty': station_wall_dirty(),
        'station_wall_green': station_wall_green(), 'station_floor': station_floor(), 'floor_grate': floor_grate(),
        'station_ceiling': station_ceiling(), 'hazard_panel': hazard_panel(), 'station_trim': station_trim(),
        'ceiling_light_on': ceiling_light(True), 'ceiling_light_off': ceiling_light(False), 'emergency_light': emergency_light(),
        'vent': vent(), 'locker_front_lower': locker('front_lower'), 'locker_front_upper': locker('front_upper'),
        'locker_side': locker('side'), 'locker_top': locker('top'), 'save_station_front': save_station('front'),
        'save_station_side': save_station('side'), 'terminal_front': terminal('front'), 'terminal_side': terminal('side'),
        'desk_top': desk('top'), 'desk_side': desk('side'), 'debris': debris(),
        'lab_door_top': door((88, 128, 70), 'top'), 'lab_door_bottom': door((88, 128, 70), 'bottom'),
        'security_door_top': door((200, 120, 40), 'top'), 'security_door_bottom': door((200, 120, 40), 'bottom'),
        'generator_side': generator('side'), 'generator_top': generator('top'),
        'generator_front_0': generator('front', 0), 'generator_front_1': generator('front', 1),
        'generator_front_2': generator('front', 2), 'generator_front_3': generator('front', 3),
        'power_seal': seal('power'), 'keycard_seal': seal('keycard'),
        'escape_console_front': console('front'), 'escape_console_side': console('side'),
        'tracker_body': tracker_body(), 'tracker_screen': tracker_screen(), 'tracker_grip': tracker_grip(),
    }
    for k, v in blocks.items():
        img(v).convert('RGBA').save(os.path.join(bt, k + '.png'))
    items = {'motion_tracker': item_tracker(), 'flamethrower': item_flamethrower(), 'fuel': item_fuel(),
             'noisemaker': item_noisemaker(), 'flare': item_flare(), 'flashlight': item_flashlight(False),
             'flashlight_on': item_flashlight(True), 'medkit': item_medkit(), 'station_core': item_station_core(),
             'fuse': item_fuse(), 'keycard': item_keycard(),
             'lab_door': door_item((88, 128, 70)), 'security_door': door_item((200, 120, 40))}
    for k, v in items.items():
        v.save(os.path.join(it, k + '.png'))
    img(grain()).save(os.path.join(mt, 'grain.png'))
    img(vignette()).save(os.path.join(mt, 'vignette.png'))
    img(locker_slats()).save(os.path.join(mt, 'locker_slats.png'))
    return list(blocks), list(items)
