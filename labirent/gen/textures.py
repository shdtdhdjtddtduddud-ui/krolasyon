"""Block, item and effect textures for the Labirent mod (procedural pixel art)."""
import os, math
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/labirent/textures')
BLOCK = os.path.join(ROOT, 'block')
ITEM = os.path.join(ROOT, 'item')
EFFECT = os.path.join(ROOT, 'mob_effect')
for d in (BLOCK, ITEM, EFFECT):
    os.makedirs(d, exist_ok=True)


def rng(s):
    return np.random.default_rng(s)


def vnoise(h, w, r, scale=4.0, octaves=3, tile=True):
    """tileable value noise in [0,1]"""
    out = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for _ in range(octaves):
        gh, gw = max(2, int(round(h / scale))), max(2, int(round(w / scale)))
        g = r.random((gh, gw))
        ys = np.arange(h) / h * gh
        xs = np.arange(w) / w * gw
        y0 = np.floor(ys).astype(int); x0 = np.floor(xs).astype(int)
        fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        y1 = (y0 + 1) % gh; x1 = (x0 + 1) % gw
        a = g[y0][:, x0]; b = g[y0][:, x1]; c = g[y1][:, x0]; d = g[y1][:, x1]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy)
        tot += amp
        amp *= 0.5
        scale = max(1.0, scale / 2)
    return out / tot


def mix(a, b, t):
    a = np.array(a, float); b = np.array(b, float)
    t = np.asarray(t, float)[..., None]
    return a * (1 - t) + b * t


def save(arr, path, alpha=None):
    arr = np.clip(arr, 0, 255).astype(np.uint8)
    if alpha is None:
        Image.fromarray(arr, 'RGB').save(path)
    else:
        a = np.clip(alpha * 255, 0, 255).astype(np.uint8)
        Image.fromarray(np.dstack([arr, a]), 'RGBA').save(path)


# ------------------------------------------------------------------ blocks
def concrete(seed, size=16, base=(118, 118, 112)):
    r = rng(seed)
    n = vnoise(size, size, r, 6) * 0.6 + vnoise(size, size, r, 2) * 0.4
    col = mix((np.array(base) * 0.72), (np.array(base) * 1.12), n)
    # vertical rain streaks
    for x in range(size):
        if r.random() < 0.35:
            y0 = r.integers(0, size)
            ln = r.integers(3, size)
            for y in range(y0, min(size, y0 + ln)):
                col[y, x] *= 0.86
    # panel seam on the left / top edge and a tie-hole
    col[:, 0] *= 0.78
    col[0, :] *= 0.85
    if seed % 3 == 0:
        col[11, 11] = col[11, 11] * 0.45
    pores = r.random((size, size)) < 0.05
    col[pores] *= 0.7
    return col


def moss(col, seed, amount, bottom=True):
    r = rng(seed + 100)
    h, w = col.shape[:2]
    n = vnoise(h, w, r, 4)
    yy = np.arange(h)[:, None] / (h - 1)
    grad = yy if bottom else 1 - yy
    m = n * 0.7 + grad * 0.6 > 1.05 - amount
    tone = vnoise(h, w, r, 2)
    col = col.copy()
    col[m] = mix((52, 78, 30), (98, 128, 52), tone)[m]
    return col


def cracks_on(col, seed):
    r = rng(seed + 300)
    h, w = col.shape[:2]
    col = col.copy()
    x, y = r.integers(3, w - 3), 0
    while y < h:
        col[y, x] *= 0.45
        if 0 < x < w - 1:
            col[y, x + 1] *= 0.85
        y += 1
        x = int(np.clip(x + r.integers(-1, 2), 1, w - 2))
        if r.random() < 0.15:
            bx, by = x, y
            for k in range(r.integers(2, 5)):
                bx = int(np.clip(bx + r.choice([-1, 1]), 0, w - 1)); by = min(h - 1, by + 1)
                col[by, bx] *= 0.55
    return col


def ivy_overlay(seed, size=16):
    """transparent leaves hanging over a wall"""
    r = rng(seed)
    col = np.zeros((size, size, 3))
    alpha = np.zeros((size, size))
    n = vnoise(size, size, r, 5)
    # a few hanging vines
    for k in range(5):
        x = r.integers(0, size)
        for y in range(size):
            if r.random() < 0.85:
                x = int(np.clip(x + r.integers(-1, 2), 0, size - 1))
                alpha[y, x] = 1
                col[y, x] = (40, 70, 26)
    # leaf clusters
    for k in range(int(size * size * 0.18)):
        x, y = r.integers(0, size), r.integers(0, size)
        if n[y, x] < 0.35:
            continue
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            xx, yy = (x + dx) % size, (y + dy) % size
            alpha[yy, xx] = 1
            t = r.random()
            col[yy, xx] = mix((38, 82, 30), (110, 160, 60), t)
    hi = (alpha > 0) & (r.random((size, size)) < 0.12)
    col[hi] = (150, 196, 90)
    return col, alpha


def write_blocks():
    w = concrete(1)
    save(w, f'{BLOCK}/maze_wall.png')
    save(moss(concrete(2), 2, 0.55), f'{BLOCK}/maze_wall_mossy.png')
    save(cracks_on(concrete(3), 3), f'{BLOCK}/maze_wall_cracked.png')
    save(moss(concrete(4), 4, 0.35, bottom=False), f'{BLOCK}/maze_wall_ivy_base.png')
    c, a = ivy_overlay(5)
    save(c, f'{BLOCK}/maze_wall_ivy_leaves.png', a)

    # sliding door: dark steel-concrete slabs with vertical grooves
    r = rng(7)
    d = concrete(7, base=(88, 90, 92))
    for x in (3, 8, 12):
        d[:, x] *= 0.55
        d[:, x + 1] *= 1.15
    d[2, 1:15] *= 0.7; d[13, 1:15] *= 0.7
    for (y, x) in ((1, 1), (1, 14), (14, 1), (14, 14)):
        d[y, x] = (170, 170, 168)
    save(d, f'{BLOCK}/maze_door.png')

    # floor: packed earth with pebbles
    r = rng(9)
    n = vnoise(16, 16, r, 4)
    f = mix((92, 72, 50), (138, 112, 82), n)
    for k in range(14):
        x, y = r.integers(0, 16), r.integers(0, 16)
        f[y, x] = mix((110, 108, 100), (170, 166, 156), r.random())
        if r.random() < 0.5:
            f[y, (x + 1) % 16] = f[y, x] * 0.8
    save(f, f'{BLOCK}/maze_floor.png')
    save(moss(f, 10, 0.6, bottom=False), f'{BLOCK}/maze_floor_mossy.png')

    # Box floor: iron grating
    g = np.zeros((16, 16, 3)) + (40, 40, 44)
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0:
                g[y, x] = (128, 128, 132) if (x + y) % 2 == 0 else (110, 110, 116)
    rust = vnoise(16, 16, rng(11), 4) > 0.65
    g[rust] = mix(g[rust], (120, 70, 40), 0.5)
    save(g, f'{BLOCK}/box_floor.png')

    # lab
    lw = np.zeros((16, 16, 3)) + (222, 226, 228)
    lw[0, :] = lw[:, 0] = (180, 186, 190)
    lw[8, 1:] = (200, 204, 208)
    save(lw, f'{BLOCK}/lab_wall.png')
    ll = np.zeros((16, 16, 3)) + (250, 252, 255)
    ll[0, :] = ll[-1, :] = ll[:, 0] = ll[:, -1] = (160, 170, 180)
    save(ll, f'{BLOCK}/lab_light.png')

    # WICKED plate (32 px so the letters read)
    p = np.zeros((32, 32, 3)) + (70, 74, 80)
    p += (vnoise(32, 32, rng(13), 6)[..., None] - 0.5) * 30
    p[0, :] = p[:, 0] = (40, 42, 46); p[-1, :] = p[:, -1] = (120, 124, 130)
    font = {
        'W': ["1...1", "1...1", "1.1.1", "1.1.1", ".1.1."], 'I': ["111", ".1.", ".1.", ".1.", "111"],
        'C': [".111", "1...", "1...", "1...", ".111"], 'K': ["1..1", "1.1.", "11..", "1.1.", "1..1"],
        'E': ["111", "1..", "11.", "1..", "111"], 'D': ["11.", "1.1", "1.1", "1.1", "11."]}
    x = 2
    for ch in "WICKED":
        glyph = font[ch]
        for gy, row in enumerate(glyph):
            for gx, v in enumerate(row):
                if v == '1':
                    p[13 + gy, x + gx] = (230, 230, 220)
        x += len(glyph[0]) + 1
    p[21, 2:30] = (160, 30, 30)
    save(p, f'{BLOCK}/wicked_plate.png')

    # code stones: mossy carved stone with a glowing numeral
    digits = {1: [".1.", "11.", ".1.", ".1.", "111"], 2: ["11.", "..1", ".1.", "1..", "111"], 3: ["11.", "..1", ".1.", "..1", "11."],
              4: ["1.1", "1.1", "111", "..1", "..1"], 5: ["111", "1..", "11.", "..1", "11."], 6: [".11", "1..", "111", "1.1", "111"],
              7: ["111", "..1", ".1.", ".1.", ".1."], 8: ["111", "1.1", "111", "1.1", "111"]}
    for k in range(1, 9):
        s = moss(concrete(20 + k, base=(100, 104, 96)), 20 + k, 0.4)
        s[1, :] *= 0.7; s[-2, :] *= 0.7
        for gy, row in enumerate(digits[k]):
            for gx, v in enumerate(row):
                if v == '1':
                    s[5 + gy, 6 + gx] = (90, 230, 255)
        for x in (2, 3, 12, 13):
            s[4:11, x] *= 0.6   # carved runes
        save(s, f'{BLOCK}/code_stone_{k}.png')

    # map table
    t = mix((90, 60, 36), (130, 92, 58), vnoise(16, 16, rng(31), 4))
    for y in range(0, 16, 4):
        t[y, :] *= 0.8
    save(t, f'{BLOCK}/map_table_side.png')
    m = np.zeros((16, 16, 3)) + (226, 214, 180)
    r = rng(32)
    for y in range(1, 15):
        for x in range(1, 15):
            if (x % 3 == 0 and r.random() < 0.7) or (y % 3 == 0 and r.random() < 0.7):
                m[y, x] = (80, 70, 60)
    m[0, :] = m[-1, :] = m[:, 0] = m[:, -1] = (90, 60, 36)
    m[7, 7] = m[8, 8] = (180, 40, 30)
    save(m, f'{BLOCK}/map_table_top.png')

    # code terminal
    side = np.zeros((16, 16, 3)) + (52, 56, 62)
    side += (vnoise(16, 16, rng(41), 4)[..., None] - 0.5) * 20
    side[0, :] = side[-1, :] = (30, 32, 36)
    save(side, f'{BLOCK}/code_terminal_side.png')
    front = side.copy()
    front[2:11, 2:14] = (8, 24, 14)
    for y in (3, 5, 7, 9):
        front[y, 3:3 + rng(y).integers(4, 10)] = (60, 255, 120)
    for x in range(3, 13, 2):
        front[13, x] = (90, 90, 96)
    save(front, f'{BLOCK}/code_terminal_front.png')

    # griever hole (only the item / particle use it)
    hole = np.zeros((16, 16, 3))
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    a = np.arctan2(yy - 7.5, xx - 7.5)
    swirl = 0.5 + 0.5 * np.sin(a * 3 + d * 0.9)
    hole[:] = mix((10, 4, 18), (90, 50, 140), np.clip(swirl * (1 - d / 10), 0, 1))
    save(hole, f'{BLOCK}/griever_hole.png')


# ------------------------------------------------------------------ items
class Canvas:
    def __init__(self, size=16):
        self.c = np.zeros((size, size, 3))
        self.a = np.zeros((size, size))
        self.s = size

    def px(self, x, y, col):
        if 0 <= x < self.s and 0 <= y < self.s:
            self.c[y, x] = col
            self.a[y, x] = 1

    def line(self, x0, y0, x1, y1, col, cols=None):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n):
            t = i / max(1, n - 1)
            x = round(x0 + (x1 - x0) * t); y = round(y0 + (y1 - y0) * t)
            self.px(x, y, cols[i % len(cols)] if cols else col)

    def rect(self, x0, y0, x1, y1, col):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, col)

    def outline(self, col=(20, 18, 22)):
        a = self.a > 0
        out = np.zeros_like(a)
        out[1:] |= a[:-1]; out[:-1] |= a[1:]; out[:, 1:] |= a[:, :-1]; out[:, :-1] |= a[:, 1:]
        edge = out & ~a
        self.c[edge] = col
        self.a[edge] = 1

    def save(self, name, folder=ITEM):
        save(self.c, f'{folder}/{name}.png', self.a)


def write_items():
    # journal
    c = Canvas()
    c.rect(3, 2, 12, 13, (110, 62, 34))
    c.rect(4, 3, 11, 12, (138, 82, 46))
    c.line(4, 2, 4, 13, (70, 40, 22))
    c.rect(6, 5, 10, 6, (220, 200, 150))
    c.line(6, 8, 10, 8, (200, 180, 130))
    c.px(10, 13, (200, 40, 40)); c.px(10, 14, (200, 40, 40))
    c.outline(); c.save('glade_journal')

    # captain's baton: dark wood staff with a brass head and red sash
    c = Canvas()
    c.line(3, 13, 11, 5, None, [(92, 58, 32), (120, 80, 44)])
    c.line(4, 13, 12, 5, None, [(70, 44, 24)])
    c.rect(11, 2, 13, 4, (220, 180, 60)); c.px(12, 3, (255, 240, 150)); c.px(14, 3, (200, 160, 40)); c.px(12, 1, (200, 160, 40))
    c.px(7, 9, (200, 30, 30)); c.px(8, 9, (200, 30, 30)); c.px(7, 10, (160, 20, 20)); c.px(6, 11, (200, 30, 30))
    c.outline(); c.save('captain_baton')

    # runner map
    c = Canvas()
    c.rect(2, 3, 13, 12, (226, 212, 172))
    for x in (5, 8, 11):
        c.line(x, 3, x, 12, (196, 180, 140))
    r = rng(5)
    for y in range(4, 12):
        for x in range(3, 13):
            if r.random() < 0.3:
                c.px(x, y, (70, 64, 56))
    c.px(7, 7, (200, 40, 30)); c.px(8, 8, (200, 40, 30))
    c.outline(); c.save('runner_map')

    # serum syringe
    c = Canvas()
    c.line(3, 12, 10, 5, None, [(200, 220, 230)])
    c.line(4, 12, 11, 5, None, [(160, 180, 190)])
    c.line(5, 10, 9, 6, None, [(60, 160, 255)])
    c.line(5, 11, 10, 6, None, [(30, 110, 230)])
    c.line(11, 4, 13, 2, (220, 220, 220))
    c.line(1, 14, 3, 12, (120, 120, 130)); c.px(2, 11, (120, 120, 130)); c.px(4, 14, (120, 120, 130))
    c.outline(); c.save('grief_serum')

    # bandage
    c = Canvas()
    c.rect(4, 5, 11, 11, (236, 232, 222))
    c.rect(5, 6, 10, 10, (250, 248, 240))
    c.rect(7, 7, 8, 9, (200, 196, 186))
    c.line(11, 9, 14, 12, (236, 232, 222))
    c.px(9, 6, (210, 60, 60))
    c.outline((90, 86, 80)); c.save('bandage')

    # stinger
    c = Canvas()
    c.line(2, 13, 12, 3, None, [(40, 40, 46), (70, 72, 80)])
    c.line(3, 13, 12, 4, None, [(30, 30, 34)])
    c.line(11, 4, 14, 1, None, [(140, 200, 70), (200, 240, 130)])
    c.outline(); c.save('griever_stinger')

    # plate
    c = Canvas()
    c.rect(3, 4, 12, 11, (70, 74, 80))
    c.rect(4, 5, 11, 10, (100, 104, 112))
    for p in ((4, 5), (11, 5), (4, 10), (11, 10)):
        c.px(*p, (180, 180, 186))
    c.line(5, 8, 10, 8, (60, 62, 68))
    c.outline(); c.save('griever_plate')

    # core
    c = Canvas()
    yy, xx = np.mgrid[0:16, 0:16]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5.5:
                c.px(x, y, mix((50, 54, 60), (110, 116, 126), (x + y) / 30))
            if d < 3:
                c.px(x, y, mix((40, 220, 255), (200, 255, 255), 1 - d / 3))
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        c.line(round(7.5 + math.cos(a) * 5), round(7.5 + math.sin(a) * 5), round(7.5 + math.cos(a) * 7), round(7.5 + math.sin(a) * 7), (30, 30, 36))
    c.outline(); c.save('griever_core')

    # beetle eye
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5:
                c.px(x, y, (70, 72, 80))
            if d < 3:
                c.px(x, y, mix((255, 40, 30), (120, 0, 0), d / 3))
    c.px(6, 6, (255, 220, 200))
    c.outline(); c.save('beetle_eye')

    # code word scrap
    c = Canvas()
    c.rect(2, 4, 13, 11, (232, 222, 190))
    for x in range(3, 13):
        if x % 3 != 2:
            c.px(x, 7, (40, 120, 200)); c.px(x, 8, (40, 120, 200))
    c.px(13, 11, (0, 0, 0)); c.a[11, 13] = 0
    c.outline((120, 100, 70)); c.save('code_word')

    # machete
    c = Canvas()
    c.line(4, 12, 13, 3, None, [(200, 206, 214), (230, 234, 240)])
    c.line(5, 12, 13, 4, None, [(150, 156, 166)])
    c.line(1, 15, 4, 12, None, [(70, 44, 26), (100, 64, 36)])
    c.px(4, 11, (60, 60, 66)); c.px(5, 13, (60, 60, 66))
    c.outline(); c.save('machete')

    # spear
    c = Canvas()
    c.line(1, 14, 11, 4, None, [(120, 84, 48), (150, 108, 62)])
    c.line(11, 4, 14, 1, None, [(190, 196, 206)])
    c.px(12, 2, (220, 226, 236)); c.px(13, 3, (160, 166, 176)); c.px(11, 2, (160, 166, 176))
    c.line(9, 6, 10, 5, (60, 40, 30))
    c.outline(); c.save('glade_spear')

    # frying pan
    c = Canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 9.5, y - 6.5)
            if d < 5.2:
                c.px(x, y, (60, 62, 68) if d > 4 else mix((40, 40, 46), (80, 82, 90), (x - y + 10) / 20))
    c.line(1, 14, 6, 10, None, [(90, 60, 36), (60, 40, 24)])
    c.outline(); c.save('frying_pan')

    # stew bowl
    c = Canvas()
    c.rect(3, 9, 12, 12, (120, 80, 46)); c.rect(4, 13, 11, 13, (100, 64, 36))
    c.rect(3, 7, 12, 8, (150, 80, 40))
    c.px(5, 7, (230, 140, 60)); c.px(8, 7, (90, 160, 60)); c.px(10, 8, (230, 200, 120)); c.px(6, 8, (180, 60, 40))
    c.px(6, 5, (220, 220, 220)); c.px(7, 4, (200, 200, 200)); c.px(9, 5, (220, 220, 220))
    c.outline(); c.save('glade_stew')

    # bread
    c = Canvas()
    c.rect(2, 7, 13, 11, (190, 130, 60)); c.rect(3, 6, 12, 6, (210, 150, 76))
    for x in (5, 8, 11):
        c.px(x, 7, (240, 200, 130))
    c.rect(3, 12, 12, 12, (150, 96, 44))
    c.outline(); c.save('glade_bread')

    # Frypan's breakfast: plate with eggs and bacon
    c = Canvas()
    for y in range(16):
        for x in range(16):
            if math.hypot((x - 7.5) / 1.2, y - 8.5) < 5.6:
                c.px(x, y, (230, 230, 226))
    c.rect(4, 7, 6, 9, (255, 255, 255)); c.px(5, 8, (255, 200, 40))
    c.rect(8, 6, 10, 8, (255, 255, 255)); c.px(9, 7, (255, 200, 40))
    c.line(5, 11, 11, 10, None, [(180, 60, 50), (230, 150, 130)])
    c.outline((120, 120, 120)); c.save('frypan_breakfast')

    # hole item uses the block swirl
    img = Image.open(f'{BLOCK}/griever_hole.png')
    img.save(f'{ITEM}/griever_hole.png')


def write_effects():
    c = Canvas(18)
    for y in range(18):
        for x in range(18):
            d = math.hypot(x - 8.5, (y - 10) * 1.1)
            if d < 5.5 and y > 4:
                c.px(x, y, mix((60, 140, 30), (160, 230, 80), 1 - d / 5.5))
    c.line(8, 1, 9, 6, (40, 40, 46))
    c.px(7, 9, (220, 255, 180))
    c.outline((20, 40, 10)); c.save('griever_venom', EFFECT)
    c = Canvas(18)
    for y in range(18):
        for x in range(18):
            d = math.hypot(x - 8.5, y - 8.5)
            a = math.atan2(y - 8.5, x - 8.5)
            if d < 7.5 and math.sin(a * 2 + d) > -0.2:
                c.px(x, y, mix((60, 90, 200), (190, 220, 255), 1 - d / 7.5))
    c.rect(7, 7, 10, 10, (255, 255, 255))
    c.outline((20, 30, 80)); c.save('changing', EFFECT)


write_blocks()
write_items()
write_effects()
print('textures written')
