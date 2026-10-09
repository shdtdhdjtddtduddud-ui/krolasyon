"""Crops item icons from the reference sheet and paints armor layer textures.

Usage (from repo root): python3 vocation_mod/gen/make_assets.py <reference.jpg>
Icons are cut from the reference slots, so they match the artwork exactly.
"""
import os, sys
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
RES = os.path.join(ROOT, 'src/main/resources/assets/krolasyonvocations')
ICON_DIR = os.path.join(RES, 'textures/item')
ARMOR_DIR = os.path.join(RES, 'textures/models/armor')
HALF = 34  # crop half-size around each slot centre (reference px), stays inside the slot frame

# Slot centres on the reference sheet: x per column (L, C, R) and y per row.
LAYOUT = {
    'knight':   {'x': (86, 187, 291),  'L': [185, 268, 388], 'C': [150, 250, 345, 445], 'R': [180, 285, 388]},
    'paladin':  {'x': (435, 537, 640), 'L': [185, 268, 388], 'C': [150, 250, 345, 445], 'R': [180, 285, 388]},
    'druid':    {'x': (86, 187, 291),  'L': [690, 775, 880], 'C': [655, 750, 850, 955], 'R': [680, 785, 880]},
    'sorcerer': {'x': (435, 537, 640), 'L': [690, 775, 880], 'C': [655, 750, 850, 955], 'R': [680, 785, 880]},
}
# Which reference slot becomes which item icon.
SLOTS = {
    'amulet': ('L', 0), 'weapon': ('L', 1), 'ring': ('L', 2),
    'helmet': ('C', 0), 'armor': ('C', 1), 'legs': ('C', 2), 'boots': ('C', 3),
    'ammo': ('R', 2),
}

# Paint palettes per set: head, chest, legs, boots, trim, accent (RGB).
PALETTES = {
    'knight':   dict(head=(156, 163, 173), chest=(140, 148, 158), legs=(120, 126, 136), boots=(60, 62, 70),
                     trim=(201, 162, 39), accent=(43, 45, 51)),
    'paladin':  dict(head=(232, 230, 236), chest=(214, 214, 222), legs=(200, 200, 210), boots=(120, 128, 150),
                     trim=(179, 32, 42), accent=(31, 42, 92)),
    'druid':    dict(head=(107, 74, 43), chest=(75, 122, 58), legs=(190, 70, 40), boots=(75, 44, 107),
                     trim=(212, 165, 70), accent=(60, 40, 24)),
    'sorcerer': dict(head=(31, 74, 51), chest=(33, 60, 44), legs=(45, 30, 58), boots=(58, 34, 88),
                     trim=(212, 165, 44), accent=(20, 40, 30)),
}


def crop_icon(ref, cx, cy):
    box = (cx - HALF, cy - HALF, cx + HALF, cy + HALF)
    img = ref.crop(box).convert('RGBA').resize((32, 32), Image.LANCZOS)
    px = img.load()
    for y in range(32):
        for x in range(32):
            r, g, b, _ = px[x, y]
            # Slot background is neutral grey at about 44-85; drop it, keep darker outlines.
            grey = max(r, g, b) - min(r, g, b) < 12 and 44 <= max(r, g, b) < 85
            if grey:
                px[x, y] = (r, g, b, 0)
    return img


def paint_armor(name, p):
    """Vanilla 64x32 humanoid UV layout: head (0,0), body (16,16), arms (40,16), legs (0,16)."""
    l1 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    l2 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    px = l1.load()
    def fill(img, x0, y0, x1, y1, c):
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), c + (255,))
    # Helmet: head faces and a trim band across the top.
    fill(l1, 0, 0, 32, 16, p['head'])
    fill(l1, 8, 0, 16, 2, p['trim'])
    fill(l1, 0, 0, 8, 8, p['accent'])
    # Chest and sleeves, with trim at the collar and hem.
    fill(l1, 16, 16, 40, 32, p['chest'])
    fill(l1, 16, 16, 40, 18, p['trim'])
    fill(l1, 16, 30, 40, 32, p['accent'])
    fill(l1, 40, 16, 56, 32, p['chest'])
    # Boots use the lower legs region of layer 1.
    fill(l1, 0, 16, 16, 32, p['legs'])
    fill(l1, 0, 27, 16, 32, p['boots'])
    # Leggings use layer 2: legs plus a trim stripe at the waist.
    fill(l2, 0, 16, 16, 32, p['legs'])
    fill(l2, 0, 16, 16, 18, p['trim'])
    fill(l2, 16, 16, 32, 32, p['accent'])
    l1.save(os.path.join(ARMOR_DIR, f'{name}_layer_1.png'))
    l2.save(os.path.join(ARMOR_DIR, f'{name}_layer_2.png'))


def main():
    ref = Image.open(sys.argv[1]).convert('RGB')
    os.makedirs(ICON_DIR, exist_ok=True)
    os.makedirs(ARMOR_DIR, exist_ok=True)
    for vocation, lay in LAYOUT.items():
        for slot, (col, row) in SLOTS.items():
            cx = lay['x'][{'L': 0, 'C': 1, 'R': 2}[col]]
            cy = lay[col][row]
            name = f'{vocation}_{slot}'
            crop_icon(ref, cx, cy).save(os.path.join(ICON_DIR, f'{name}.png'))
        paint_armor(vocation, PALETTES[vocation])


if __name__ == '__main__':
    main()
