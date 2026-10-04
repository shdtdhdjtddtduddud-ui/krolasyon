"""NPC skins, race feature textures, item textures and item model JSONs."""
import os, json, zlib, math
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/krolasyonbosses')
NPC = os.path.join(ASSETS, 'textures/entity/npc')
ITEM_TEX = os.path.join(ASSETS, 'textures/item')
MODELS = os.path.join(ASSETS, 'models/item')
MODID = 'krolasyonbosses'


def rgb(c):
    return np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255], dtype=np.float64)


def rng_for(s):
    return np.random.default_rng(zlib.crc32(s.encode()))


RACES = ['HUMAN', 'ELF', 'DWARF', 'DEMON', 'GIANT', 'ORC', 'BEASTKIN', 'HALFLING', 'DARK_ELF']
OUTFITS = ['RAGS', 'COMMON', 'WORKER', 'FINE', 'SMITH', 'ROBE', 'PRIEST', 'NOBLE', 'ROYAL', 'BANDIT', 'ADVENTURER']

# skin, hair options, eye
RACE_LOOK = {
    'HUMAN': ([0xF0C8A0, 0xC89070], [0x4A3020, 0xD8B050], 0x3060A0),
    'ELF': ([0xF4E0CC, 0xEAD2BA], [0xE8E0C0, 0xC0C8D0], 0x40A060),
    'DWARF': ([0xE4AC88, 0xD09878], [0xA04020, 0x5A3A20], 0x404040),
    'DEMON': ([0xB83A30, 0x7A3A8A], [0x1A1010, 0x2A0A0A], 0xFFD000),
    'GIANT': ([0x9AA6B4, 0xC8B49A], [0xE8E8F0, 0xD8C080], 0x60A0E0),
    'ORC': ([0x6A9A4A, 0x4A7A3A], [0x1A1A1A, 0x3A2A1A], 0xC02020),
    'BEASTKIN': ([0xC88440, 0x8A6A50], [0xA06030, 0x4A3A30], 0x80C020),
    'HALFLING': ([0xF0C8A8, 0xD8A880], [0x7A4A2A, 0xC08040], 0x5A4030),
    'DARK_ELF': ([0x4A4060, 0x3A3450], [0xF0F0F8, 0xC0B0E0], 0xFF4040),
}


def skin_canvas():
    return np.zeros((64, 64, 4), dtype=np.float64)


def paint(img, x0, y0, x1, y1, col, a=255, noise=0.0, r=None):
    region = img[y0:y1, x0:x1]
    c = rgb(col) if isinstance(col, int) else col
    if noise and r is not None:
        n = 1 + (r.random(region.shape[:2]) - 0.5) * noise
        region[..., :3] = np.clip(c[None, None, :] * n[..., None], 0, 255)
    else:
        region[..., :3] = c
    region[..., 3] = a


# body part UV boxes: (x, y, w, h, d) for player skin
HEAD = (0, 0, 8, 8, 8)
HAT = (32, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)
JACKET = (16, 32, 8, 12, 4)
RARM = (40, 16, 4, 12, 4)
RSLEEVE = (40, 32, 4, 12, 4)
LARM = (32, 48, 4, 12, 4)
LSLEEVE = (48, 48, 4, 12, 4)
RLEG = (0, 16, 4, 12, 4)
RPANTS = (0, 32, 4, 12, 4)
LLEG = (16, 48, 4, 12, 4)
LPANTS = (0, 48, 4, 12, 4)


def box_faces(b):
    x, y, w, h, d = b
    return {
        'top': (x + d, y, x + d + w, y + d),
        'bottom': (x + d + w, y, x + d + 2 * w, y + d),
        'right': (x, y + d, x + d, y + d + h),
        'front': (x + d, y + d, x + d + w, y + d + h),
        'left': (x + d + w, y + d, x + 2 * d + w, y + d + h),
        'back': (x + 2 * d + w, y + d, x + 2 * d + 2 * w, y + d + h),
    }


def fill_box(img, b, col, r=None, noise=0.08, a=255):
    for f in box_faces(b).values():
        paint(img, *f, col, a, noise, r)


def side_rows(img, b, y_from, y_to, col, r=None, noise=0.06, a=255):
    """paint rows [y_from, y_to) (0 = top of the side faces) on the four side faces"""
    faces = box_faces(b)
    for k in ('right', 'front', 'left', 'back'):
        x0, y0, x1, y1 = faces[k]
        paint(img, x0, y0 + y_from, x1, y0 + min(y_to, y1 - y0), col, a, noise, r)


def skin(race, female, outfit, variant):
    r = rng_for(f'{race}{female}{outfit}{variant}')
    skins, hairs, eye = RACE_LOOK[race]
    sk = skins[variant % len(skins)]
    hair = hairs[(variant + (1 if female else 0)) % len(hairs)]
    img = skin_canvas()
    # base skin everywhere
    for b in (HEAD, BODY, RARM, LARM, RLEG, LLEG):
        fill_box(img, b, sk, r, 0.05)
    # face
    f = box_faces(HEAD)['front']
    fx, fy = f[0], f[1]
    ey = 4
    for ex in (1, 5):
        paint(img, fx + ex, fy + ey, fx + ex + 2, fy + ey + 1, 0xF8F8F8)
        paint(img, fx + ex + (1 if ex == 1 else 0), fy + ey, fx + ex + (2 if ex == 1 else 1), fy + ey + 1, eye)
    paint(img, fx + 1, fy + 3, fx + 3, fy + 4, rgb(hair) * 0.8)
    paint(img, fx + 5, fy + 3, fx + 7, fy + 4, rgb(hair) * 0.8)
    paint(img, fx + 3, fy + 6, fx + 5, fy + 7, rgb(sk) * 0.6 + rgb(0x802020) * 0.4)
    paint(img, fx + 3, fy + 5, fx + 5, fy + 6, rgb(sk) * 0.85)
    if race == 'BEASTKIN':
        paint(img, fx + 2, fy + 5, fx + 6, fy + 8, rgb(sk) * 1.15 + 20)
        paint(img, fx + 3, fy + 5, fx + 5, fy + 6, 0x201810)
    if race == 'DEMON':
        paint(img, fx + 1, fy + ey, fx + 3, fy + ey + 1, eye)
        paint(img, fx + 5, fy + ey, fx + 7, fy + ey + 1, eye)
    # hair: top, back, sides upper rows
    hf = box_faces(HEAD)
    paint(img, *hf['top'], hair, 255, 0.15, r)
    hx0, hy0, hx1, hy1 = hf['back']
    paint(img, hx0, hy0, hx1, hy0 + (7 if female else 4), hair, 255, 0.15, r)
    for k in ('right', 'left'):
        x0, y0, x1, y1 = hf[k]
        paint(img, x0, y0, x1, y0 + (6 if female else 3), hair, 255, 0.15, r)
    paint(img, fx, fy, fx + 8, fy + (2 if not female else 2), hair, 255, 0.15, r)
    if female:
        paint(img, fx, fy + 2, fx + 1, fy + 6, hair, 255, 0.1, r)
        paint(img, fx + 7, fy + 2, fx + 8, fy + 6, hair, 255, 0.1, r)
        # long hair on the hat layer (back)
        hb = box_faces(HAT)['back']
        paint(img, hb[0], hb[1], hb[2], hb[3], hair, 255, 0.15, r)
    if not female and race in ('DWARF', 'GIANT', 'HUMAN') and variant == 1 or race == 'DWARF' and not female:
        paint(img, fx + 1, fy + 5, fx + 7, fy + 8, rgb(hair) * 0.9, 255, 0.15, r)
        paint(img, fx + 3, fy + 6, fx + 5, fy + 7, rgb(sk) * 0.6 + rgb(0x802020) * 0.4)
    if race == 'ORC':
        paint(img, fx + 2, fy + 7, fx + 3, fy + 8, 0xF0F0E0)
        paint(img, fx + 5, fy + 7, fx + 6, fy + 8, 0xF0F0E0)
    # outfit
    o = outfit
    pal = {
        'RAGS': (0x6A5A40, 0x4A3A28, None, 0x5A4A38),
        'COMMON': ([0x3A6A3A, 0x3A4A7A, 0x8A6A3A, 0x6A3A3A][variant * 2 % 4 + (1 if female else 0)], 0x5A4030, 0x3A2A1A, None),
        'WORKER': (0xD8C8A0, 0x6A6A6A, 0x3A2A1A, 0x8A5A30),
        'FINE': ([0x5A2A7A, 0x1A6A6A][variant], 0x2A2A3A, 0x1A1A1A, 0xE0B040),
        'SMITH': (0x3A3A3A, 0x4A4A4A, 0x1A1A1A, 0x6A4020),
        'ROBE': ([0x1A2A6A, 0x4A1A5A][variant], None, 0x1A1A2A, 0xE0C040),
        'PRIEST': (0xF0F0E8, None, 0x8A7A5A, 0xE0B040),
        'NOBLE': ([0x8A1A2A, 0x1A3A8A][variant], 0x1A1A1A, 0x101010, 0xE0C040),
        'ROYAL': (0x6A1A8A, 0x4A0A5A, 0x101010, 0xF0D040),
        'BANDIT': (0x2A2420, 0x1A1A1A, 0x101010, 0x4A3A30),
        'ADVENTURER': (0x3A5A2A, 0x5A4030, 0x3A2A1A, 0x7A5030),
    }[o]
    shirt, pants, boots, accent = pal
    shirt_c = shirt
    # torso + arms
    fill_box(img, BODY, shirt_c, r, 0.12)
    sleeve_rows = 12 if o in ('ROBE', 'PRIEST', 'ROYAL', 'NOBLE', 'FINE', 'SMITH', 'BANDIT') else 5
    if o == 'RAGS':
        sleeve_rows = 3
    for arm in (RARM, LARM):
        side_rows(img, arm, 0, sleeve_rows, shirt_c, r, 0.12)
        top = box_faces(arm)['top']
        paint(img, *top, shirt_c, 255, 0.1, r)
    if o in ('NOBLE', 'FINE'):
        for arm in (RARM, LARM):
            side_rows(img, arm, 4, 11, 0xF0F0F0, r, 0.05)
    # legs
    legcol = pants if pants is not None else shirt_c
    for leg in (RLEG, LLEG):
        side_rows(img, leg, 0, 12, legcol, r, 0.1)
        paint(img, *box_faces(leg)['top'], legcol, 255, 0.1, r)
        if boots is not None:
            side_rows(img, leg, 9, 12, boots, r, 0.1)
            paint(img, *box_faces(leg)['bottom'], boots)
        elif o == 'RAGS':
            side_rows(img, leg, 10, 12, sk, r, 0.05)
    # robes cover the legs
    if o in ('ROBE', 'PRIEST', 'ROYAL'):
        for leg in (RLEG, LLEG):
            side_rows(img, leg, 0, 11, shirt_c, r, 0.1)
    # belts, trims, aprons
    bf = box_faces(BODY)
    x0, y0, x1, y1 = bf['front']
    if accent is not None:
        if o in ('SMITH', 'WORKER'):
            paint(img, x0 + 1, y0 + 3, x1 - 1, y1, accent, 255, 0.1, r)
        elif o == 'RAGS':
            for _ in range(4):
                px, py = x0 + r.integers(0, 7), y0 + r.integers(0, 11)
                paint(img, px, py, px + 1, py + 1, accent)
        else:
            side_rows(img, BODY, 8, 9, accent, r, 0.05)
            paint(img, x0 + 3, y0, x0 + 5, y1 - 4, accent if o in ('ROBE', 'PRIEST', 'ROYAL', 'NOBLE', 'FINE') else rgb(shirt_c) * 0.8)
    if o == 'ROYAL':
        side_rows(img, BODY, 0, 2, 0xF8F8F8, r, 0.02)
        for i in range(0, 8, 3):
            paint(img, x0 + i, y0 + 1, x0 + i + 1, y0 + 2, 0x101010)
    if o == 'ADVENTURER':
        side_rows(img, BODY, 0, 9, 0x6A4A2A, r, 0.12)
        paint(img, x0 + 2, y0, x0 + 6, y0 + 9, shirt_c, 255, 0.1, r)
        side_rows(img, BODY, 9, 10, 0x2A1A10, r, 0.02)
    if o == 'BANDIT':
        hb = box_faces(HAT)
        for k, f2 in hb.items():
            if k == 'bottom':
                continue
            paint(img, *f2, 0x1A1614, 255, 0.1, r)
        hx0, hy0, hx1, hy1 = hb['front']
        img[hy0 + 1:hy1, hx0 + 1:hx1 - 1, 3] = 0
        paint(img, fx, fy + 5, fx + 8, fy + 8, 0x1A1A1A, 255, 0.1, r)
    if o == 'PRIEST' or o == 'ROBE':
        hb = box_faces(HAT)
        if o == 'ROBE' and variant == 0:
            for k, f2 in hb.items():
                if k in ('bottom',):
                    continue
                paint(img, *f2, shirt_c, 255, 0.1, r)
            hx0, hy0, hx1, hy1 = hb['front']
            img[hy0 + 2:hy1, hx0 + 1:hx1 - 1, 3] = 0
    if o == 'RAGS':
        for _ in range(12):
            px, py = r.integers(16, 40), r.integers(20, 32)
            img[py, px, :3] = rgb(sk)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


def features(race):
    skins, hairs, eye = RACE_LOOK[race]
    sk = rgb(skins[0])
    hair = rgb(hairs[0])
    img = np.zeros((32, 64, 4))
    r = rng_for('feat' + race)

    def p(x0, y0, x1, y1, c, noise=0.08):
        n = 1 + (r.random((y1 - y0, x1 - x0)) - 0.5) * noise
        img[y0:y1, x0:x1, :3] = np.clip(c[None, None, :] * n[..., None], 0, 255)
        img[y0:y1, x0:x1, 3] = 255

    p(0, 0, 16, 8, sk)                       # elf ears
    horn = rgb(0x2A2020) if race == 'DEMON' else rgb(0xE0D8C0)
    p(0, 8, 16, 16, horn)                    # horns
    p(16, 0, 24, 8, hair if race == 'BEASTKIN' else sk)   # beast ears
    img[1:4, 17:22, :3] = np.clip(sk * 1.2, 0, 255)
    p(24, 0, 32, 8, rgb(0xF0EEDC))           # tusks
    p(32, 0, 64, 8, hair)                    # beard
    tail = hair if race == 'BEASTKIN' else rgb(0x8A1A10) if race == 'DEMON' else sk
    p(0, 16, 32, 32, tail, 0.2)
    if race == 'DEMON':
        p(20, 16, 32, 24, rgb(0x2A0A0A))
    return Image.fromarray(img.astype(np.uint8), 'RGBA')


# ---------------------------------------------------------------- items
def blank():
    return np.zeros((16, 16, 4))


def px(img, x, y, c, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        img[y, x, :3] = rgb(c) if isinstance(c, int) else c
        img[y, x, 3] = a


def save_item(img, name):
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA').save(os.path.join(ITEM_TEX, name + '.png'))


def shade(c, f):
    return np.clip(rgb(c) * f, 0, 255)


def outline(img, col=0x101010):
    a = img[..., 3] > 0
    out = np.zeros_like(a)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        out |= np.roll(np.roll(a, dy, 0), dx, 1)
    out &= ~a
    img[out, :3] = rgb(col)
    img[out, 3] = 255


def coin(c1, c2, name):
    img = blank()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6.2:
                f = 1.15 - (x + y) / 40
                px(img, x, y, shade(c1, f))
            if 4.2 < d < 5.0:
                px(img, x, y, shade(c2, 1.0))
    px(img, 6, 6, 0xFFFFFF)
    px(img, 7, 6, 0xFFFFFF)
    outline(img, 0x2A1A0A)
    save_item(img, name)


def tome():
    base, over = blank(), blank()
    for y in range(2, 14):
        for x in range(3, 13):
            px(base, x, y, shade(0x6A4A2A, 1.1 - y / 30))
    for y in range(3, 13):
        px(base, 12, y, 0xE8E0C8)
        px(base, 11, y, 0xD0C8B0)
    for y in range(2, 14):
        px(base, 3, y, 0x3A2A1A)
    outline(base, 0x1A1008)
    for y in range(4, 12):
        for x in range(5, 10):
            if abs(x - 7) + abs(y - 7.5) < 3.2:
                px(over, x, y, 0xFFFFFF)
    for x in range(4, 11):
        px(over, x, 3, 0xD0D0D0)
        px(over, x, 12, 0xD0D0D0)
    save_item(base, 'spell_tome')
    save_item(over, 'spell_tome_overlay')


def staff(tier, name):
    img = blank()
    wood = [0x8A6A3A, 0x6A4A2A, 0x4A3A3A, 0x2A2A3A, 0xE0E0F0][tier]
    gem = [0x60C0FF, 0xA060FF, 0xFF6040, 0x40FFA0, 0xFFF080][tier]
    for i in range(12):
        x, y = 2 + i, 14 - i
        px(img, x, y, shade(wood, 1.1 - i / 30))
    for dx, dy in ((0, 0), (1, 0), (0, -1), (1, -1)):
        px(img, 13 + dx, 2 + dy, gem)
    px(img, 14, 1, 0xFFFFFF)
    px(img, 12, 4, shade(0xE0B040, 1.0))
    px(img, 14, 3, shade(0xE0B040, 1.0))
    if tier >= 2:
        px(img, 12, 1, gem, 200)
        px(img, 15, 4, gem, 200)
    outline(img)
    save_item(img, name)


def potion(c, name, big=False):
    img = blank()
    for y in range(5, 15):
        for x in range(4, 12):
            if math.hypot(x - 7.5, y - 10) < (4.6 if big else 4.0):
                px(img, x, y, shade(c, 1.2 - y / 25))
    for y in range(2, 6):
        px(img, 7, y, 0xC0C8D0)
        px(img, 8, y, 0xC0C8D0)
    px(img, 7, 1, 0x8A5A30)
    px(img, 8, 1, 0x8A5A30)
    px(img, 6, 8, 0xFFFFFF)
    outline(img, 0x202028)
    save_item(img, name)


def simple(name, draw):
    img = blank()
    draw(img)
    outline(img)
    save_item(img, name)


def draw_ring(img):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 9)
            if 3 < d < 5:
                px(img, x, y, shade(0xF0C040, 1.1 - y / 30))
    for dx in range(-1, 2):
        for dy in range(-1, 2):
            px(img, 7 + dx, 3 + dy, 0xE0F8FF if dx == 0 and dy == 0 else 0x80D0FF)


def draw_seal(img):
    for y in range(16):
        for x in range(16):
            if math.hypot(x - 7.5, y - 7.5) < 6.5:
                px(img, x, y, shade(0x4A3A30, 1.1 - y / 25))


def draw_seal_overlay(img):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.5 or 5.0 < d < 5.6:
                px(img, x, y, 0xFFFFFF)


def draw_essence(img):
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 8) * 0.8
            if d < 5:
                px(img, x, y, shade(0xA040E0, 1.3 - d / 6))
    px(img, 6, 6, 0xFFFFFF)


def draw_herb(img):
    for i in range(10):
        px(img, 7, 14 - i, 0x3A7A2A)
    for (x, y) in ((5, 6), (4, 5), (9, 7), (10, 6), (6, 9), (5, 10), (9, 10), (10, 11), (7, 3), (6, 2), (8, 2)):
        px(img, x, y, 0x60C040)
    px(img, 7, 2, 0xF0F080)


def draw_pendant(img):
    for i in range(8):
        px(img, 3 + i, 2 + abs(i - 3) // 2, 0xC0C0C8)
    for y in range(7, 13):
        for x in range(5, 11):
            if abs(x - 7.5) + abs(y - 9.5) < 3.5:
                px(img, x, y, shade(0x60E0A0, 1.2 - y / 30))
    px(img, 7, 8, 0xFFFFFF)


def draw_royal(img):
    for y in range(16):
        for x in range(16):
            if math.hypot(x - 7.5, y - 8) < 6:
                px(img, x, y, shade(0xC02030, 1.1 - y / 25))
    for x in (5, 7, 9):
        px(img, x, 6, 0xF0D040)
        px(img, x, 5, 0xF0D040)
    for x in range(5, 10):
        px(img, x, 7, 0xF0D040)


def draw_deed(img):
    for y in range(3, 13):
        for x in range(2, 14):
            px(img, x, y, shade(0xE8DCB8, 1.05 - y / 40))
    for y in (5, 7, 9):
        for x in range(4, 12):
            px(img, x, y, 0x6A5A40)
    for x, y in ((11, 11), (12, 11), (11, 12), (12, 12)):
        px(img, x, y, 0xC02020)


def draw_trophy(img):
    for y in range(4, 10):
        for x in range(4, 12):
            if abs(x - 7.5) < 4 - (y - 4) * 0.3:
                px(img, x, y, shade(0xF0C040, 1.2 - y / 20))
    for y in range(10, 13):
        px(img, 7, y, 0xC09030)
        px(img, 8, y, 0xC09030)
    for x in range(5, 11):
        px(img, x, 13, 0x6A4A2A)
    px(img, 3, 5, 0xF0C040)
    px(img, 12, 5, 0xF0C040)


def draw_letter(img):
    for y in range(4, 12):
        for x in range(2, 14):
            px(img, x, y, 0xF0E8D0)
    for i in range(6):
        px(img, 2 + i, 4 + i // 1 if i < 4 else 7, 0xA09070)
        px(img, 13 - i, 4 + i // 1 if i < 4 else 7, 0xA09070)
    px(img, 7, 8, 0xC02020)
    px(img, 8, 8, 0xC02020)


def sword(s):
    sid, _, _, _, tier, blade, guard, grip, gem, shape = s[:10]
    img = blank()
    L = {'dagger': 7, 'rapier': 11, 'great': 12, 'katana': 11, 'cleaver': 9}.get(shape, 10)
    # blade along the diagonal from (5,10) toward (15,0)
    for i in range(L):
        bx, by = 5 + i, 10 - i
        t = i / max(1, L - 1)
        f = 1.15 - t * 0.25
        width = {'broad': 2, 'great': 2, 'cleaver': 3, 'flame': 2, 'crystal': 2}.get(shape, 1)
        if shape == 'flame' and i % 3 == 0:
            width = 1
        if shape == 'serrated' and i % 2 == 0:
            px(img, bx + 1, by + 1, shade(blade, 0.8))
        px(img, bx, by, shade(blade, f + 0.15))
        if width >= 2:
            px(img, bx - 1, by, shade(blade, f))
            px(img, bx, by + 1, shade(blade, f - 0.1))
        if width >= 3:
            px(img, bx - 1, by + 1, shade(blade, f - 0.2))
            px(img, bx + 1, by + 1, shade(blade, f - 0.15))
        if shape == 'curved' or shape == 'katana':
            px(img, bx + 1, by + 1, shade(blade, f - 0.2))
        if shape == 'crystal' and i % 2 == 1:
            px(img, bx + 1, by, shade(blade, 1.3))
        if shape == 'bone' and i % 3 == 1:
            px(img, bx - 1, by - 1, shade(blade, 0.9))
    tip = (5 + L, 10 - L)
    px(img, tip[0], tip[1], shade(blade, 1.3))
    # guard
    gw = 3 if shape in ('great', 'broad', 'cleaver') else 2 if shape not in ('dagger', 'katana') else 1
    for k in range(-gw, gw + 1):
        px(img, 4 + k, 10 + k, shade(guard, 1.0 - abs(k) * 0.08))
    # grip
    for i in range(3 if shape != 'great' else 4):
        px(img, 3 - i, 12 + i - 1, shade(grip, 1.0 - i * 0.1))
    px(img, 0 if shape == 'great' else 1, 14 if shape == 'great' else 13, shade(guard, 1.0))
    if gem is not None:
        px(img, 4, 10, gem)
    outline(img, 0x0A0A0A)
    save_item(img, sid)


def model(name, parent, layers):
    os.makedirs(MODELS, exist_ok=True)
    data = {'parent': parent}
    if layers:
        data['textures'] = {f'layer{i}': f'{MODID}:item/{l}' for i, l in enumerate(layers)}
    with open(os.path.join(MODELS, name + '.json'), 'w') as f:
        json.dump(data, f, indent=2)


def main(defs):
    os.makedirs(NPC, exist_ok=True)
    os.makedirs(ITEM_TEX, exist_ok=True)
    for race in RACES:
        for female in (False, True):
            for o in OUTFITS:
                for v in (0, 1):
                    skin(race, female, o, v).save(os.path.join(NPC, f"{race.lower()}_{'f' if female else 'm'}_{o.lower()}_{v}.png"))
        features(race).save(os.path.join(NPC, f'features_{race.lower()}.png'))
    coin(0xC07040, 0x8A4A20, 'copper_coin')
    coin(0xD8D8E0, 0x9090A0, 'silver_coin')
    coin(0xF0C040, 0xB08020, 'gold_coin')
    tome()
    for i in range(5):
        staff(i, f'staff_{i + 1}')
    potion(0x3070F0, 'mana_potion')
    potion(0x6040F0, 'greater_mana_potion', True)
    potion(0xE03040, 'healing_potion')
    simple('engagement_ring', draw_ring)
    simple('ancient_seal', draw_seal)
    img = blank(); draw_seal_overlay(img); save_item(img, 'ancient_seal_overlay')
    simple('monster_essence', draw_essence)
    simple('healing_herb', draw_herb)
    simple('elf_pendant', draw_pendant)
    simple('royal_seal', draw_royal)
    simple('house_deed', draw_deed)
    simple('boss_trophy', draw_trophy)
    simple('sealed_letter', draw_letter)
    for s in defs.SWORDS:
        sword(s)
        model(s[0], 'minecraft:item/handheld', [s[0]])
    for n in ('copper_coin', 'silver_coin', 'gold_coin', 'mana_potion', 'greater_mana_potion', 'healing_potion', 'engagement_ring',
              'monster_essence', 'healing_herb', 'elf_pendant', 'royal_seal', 'house_deed', 'boss_trophy', 'sealed_letter'):
        model(n, 'minecraft:item/generated', [n])
    model('spell_tome', 'minecraft:item/generated', ['spell_tome', 'spell_tome_overlay'])
    model('ancient_seal', 'minecraft:item/generated', ['ancient_seal', 'ancient_seal_overlay'])
    for i in range(5):
        model(f'staff_{i + 1}', 'minecraft:item/handheld_rod', [f'staff_{i + 1}'])
    for m in defs.MONSTERS + defs.BOSSES:
        model(m.id + '_spawn_egg', 'minecraft:item/template_spawn_egg', None)
