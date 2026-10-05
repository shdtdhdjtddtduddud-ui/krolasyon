"""3D models for potions, mana crystals, keys, coins, scrolls, cards, maps + armor piece icons."""
import math
from PIL import ImageDraw, ImageFont
from blockmodel import *
from weapons import CX, CZ


# --------------------------------------------------------------- special 2D face materials
def card_mat(base, stripe, text='HUNTER', sub='LICENSE', emblem=(120, 200, 255)):
    f = ImageFont.load_default()
    def fn(face, W, H, rng, el):
        im = Image.new('RGBA', (W * 4, H * 4), tuple(base) + (255,))
        d = ImageDraw.Draw(im)
        if W >= 8 and H >= 8:
            d.rectangle([0, 0, W * 4 - 1, H * 4 - 1], outline=tuple(stripe) + (255,), width=3)
            d.rectangle([4, 4, W * 4 - 5, H * 4 // 4 + 4], fill=tuple(stripe) + (255,))
        if face in ('north', 'south') and W >= 8:
            d.text((8, 6), text, fill=(10, 14, 24, 255), font=f)
            d.text((8, H * 4 // 2), sub, fill=tuple(emblem) + (255,), font=f)
            cx, cy, r = W * 4 - 22, H * 4 - 22, 14
            d.polygon([(cx, cy - r), (cx + r * 0.6, cy), (cx, cy + r), (cx - r * 0.6, cy)], outline=tuple(emblem) + (255,), width=2)
            d.line([(cx - r * 0.6, cy), (cx + r * 0.6, cy)], fill=tuple(emblem) + (255,), width=1)
        im = im.resize((W, H), Image.LANCZOS)
        a = np.asarray(im).astype(np.float32)
        a[..., :3] *= SHADE[face] * 0.4 + 0.6
        return a
    return fn


def paper_mat(kind):
    f = ImageFont.load_default()
    def fn(face, W, H, rng, el):
        S = 4
        im = Image.new('RGBA', (W * S, H * S), (226, 208, 168, 255) if kind == 'map' else (216, 216, 210, 255))
        d = ImageDraw.Draw(im)
        arr = (rng.random((H * S, W * S)) * 18).astype(np.uint8)
        if face in ('north', 'south') and W >= 8:
            if kind == 'map':
                # crude Korea peninsula + island + gates
                pts = [(0.45, 0.08), (0.62, 0.15), (0.66, 0.35), (0.7, 0.55), (0.6, 0.78), (0.45, 0.82), (0.36, 0.68), (0.4, 0.5), (0.32, 0.34), (0.38, 0.16)]
                d.polygon([(x * W * S, y * H * S) for x, y in pts], fill=(150, 176, 112, 255), outline=(70, 60, 40, 255))
                d.ellipse([W * S * 0.38, H * S * 0.88, W * S * 0.5, H * S * 0.97], fill=(150, 176, 112, 255), outline=(70, 60, 40, 255))
                for gx, gy, col in ((0.47, 0.35, (60, 140, 255)), (0.6, 0.62, (255, 60, 60)), (0.44, 0.92, (190, 60, 255))):
                    r = 3
                    d.ellipse([gx * W * S - r, gy * H * S - r, gx * W * S + r, gy * H * S + r], fill=col + (255,))
                d.rectangle([2, 2, W * S - 3, H * S - 3], outline=(120, 90, 50, 255), width=2)
            else:
                d.text((6, 4), 'HUNTER', fill=(20, 20, 24, 255), font=f)
                d.text((6, 16), 'NEWS', fill=(150, 20, 20, 255), font=f)
                for i in range(int(H * S / 8) - 3):
                    d.line([(6, 30 + i * 8), (W * S - 6 - (i % 3) * 12, 30 + i * 8)], fill=(70, 70, 74, 255), width=2)
                d.rectangle([W * S * 0.55, 30, W * S - 6, 30 + H * S * 0.28], fill=(120, 130, 150, 255))
        im = im.resize((W, H), Image.LANCZOS)
        a = np.asarray(im).astype(np.float32)
        a[..., :3] -= np.asarray(Image.fromarray(arr).resize((W, H)))[..., None] * 0.5
        a[..., :3] *= SHADE[face] * 0.35 + 0.65
        return a
    return fn


def rune_mat(base, glow_c):
    def fn(face, W, H, rng, el):
        xx, yy = grid(W, H)
        rgb = lerpc(base, tuple(int(v * 1.5) for v in base), noise(W, H, rng, 2)) * SHADE[face]
        out = np.zeros((H, W, 4), np.float32)
        out[..., :3] = rgb
        out[..., 3] = 255
        if face in ('north', 'south') and W > 6:
            m = np.zeros((H, W), bool)
            cx, cy = W / 2, H / 2
            m |= (np.abs(xx - cx) < 0.8) & (np.abs(yy - cy) < H * 0.32)
            m |= (np.abs(yy - cy * 0.8) < 0.8) & (np.abs(xx - cx) < W * 0.25)
            m |= (np.abs((xx - cx) - (yy - cy)) < 0.8) & (np.abs(xx - cx) < W * 0.22)
            out[m, :3] = glow_c
        return out
    return fn


# --------------------------------------------------------------- bottles
def bottle(spec):
    bm = BM(spec['id'], size=128, ppu=2.0)
    v = spec['shape']
    if v == 'round':      # small round flask
        for (w, y0, y1) in ((3.4, 0.4, 1.2), (5.0, 1.2, 2.6), (6.0, 2.6, 5.9), (5.0, 5.9, 6.8)):
            bm.box((CX - w / 2, y0, CZ - w / 2), (CX + w / 2, y1, CZ + w / 2), 'liquid')
        for (w, y0, y1) in ((4, 0, 1), (6, 1, 2.5), (7, 2.5, 6), (6, 6, 7.2), (4, 7.2, 8)):
            bm.box((CX - w / 2, y0, CZ - w / 2), (CX + w / 2, y1, CZ + w / 2), 'glass')
        neck_y = 8
    elif v == 'vial':     # tall vial
        bm.box((CX - 2.6, 0.5, CZ - 2.6), (CX + 2.6, 8.4, CZ + 2.6), 'liquid')
        bm.box((CX - 2.6, 0, CZ - 2.6), (CX + 2.6, 0.8, CZ + 2.6), 'glass')
        bm.box((CX - 3.2, 0.8, CZ - 3.2), (CX + 3.2, 10, CZ + 3.2), 'glass')
        bm.box((CX - 2.6, 10, CZ - 2.6), (CX + 2.6, 10.8, CZ + 2.6), 'glass')
        bm.box((CX - 1.5, 10.8, CZ - 1.5), (CX + 1.5, 12.5, CZ + 1.5), 'glass')
        neck_y = 12.5
    else:                 # big round flask with wide base
        for (w, y0, y1) in ((4.4, 0.4, 1), (7, 1, 2), (9, 2, 6.6), (7, 6.6, 8)):
            bm.box((CX - w / 2, y0, CZ - w / 2), (CX + w / 2, y1, CZ + w / 2), 'liquid')
        for (w, y0, y1) in ((5, 0, 1), (8, 1, 2), (10, 2, 7), (8, 7, 8.5), (6, 8.5, 9.5), (3.2, 9.5, 12)):
            bm.box((CX - w / 2, y0, CZ - w / 2), (CX + w / 2, y1, CZ + w / 2), 'glass')
        neck_y = 12
    # label
    if v == 'round':
        bm.box((CX - 3.65, 3.6, CZ - 3.65), (CX + 3.65, 4.4, CZ + 3.65), 'label')
    elif v == 'vial':
        bm.box((CX - 3.3, 5.0, CZ - 3.3), (CX + 3.3, 5.9, CZ + 3.3), 'label')
    else:
        bm.box((CX - 5.1, 4.2, CZ - 5.1), (CX + 5.1, 5.2, CZ + 5.1), 'label')
    # stopper
    st = spec['stopper']
    if st == 'cork':
        bm.box((CX - 1.6, neck_y, CZ - 1.6), (CX + 1.6, neck_y + 2.2, CZ + 1.6), 'cork')
    elif st == 'gold':
        bm.box((CX - 1.9, neck_y, CZ - 1.9), (CX + 1.9, neck_y + 1.4, CZ + 1.9), 'gold')
        bm.box((CX - 1.2, neck_y + 1.4, CZ - 1.2), (CX + 1.2, neck_y + 2.8, CZ + 1.2), 'gold')
        bm.box((CX - 0.9, neck_y + 2.8, CZ - 0.9), (CX + 0.9, neck_y + 3.8, CZ + 0.9), 'gem')
    else:
        bm.box((CX - 1.8, neck_y, CZ - 1.8), (CX + 1.8, neck_y + 1.2, CZ + 1.8), 'dark')
        bm.box((CX - 1.0, neck_y + 1.2, CZ - 1.0), (CX + 1.0, neck_y + 3.4, CZ + 1.0), 'gem')
    bm.pack()
    bm.paint({'glass': glass(spec.get('tint', (205, 230, 255)), 48), 'liquid': liquid(spec['liq'][0], spec['liq'][1]),
              'label': wrap(spec['label'][0], spec['label'][1], stripe=2, vertical=True),
              'cork': solid((150, 110, 70), 0.12), 'gold': metal((150, 100, 20), (255, 225, 110)),
              'dark': metal((24, 24, 32), (90, 90, 110)), 'gem': gem(spec['liq'][0], spec['liq'][1])})
    return bm


BOTTLES = [
    dict(id='hp_potion_small', shape='round', liq=((255, 110, 120), (200, 20, 40)), label=((230, 230, 230), (210, 40, 50)), stopper='cork'),
    dict(id='hp_potion_medium', shape='vial', liq=((255, 90, 100), (190, 10, 30)), label=((235, 235, 235), (210, 30, 40)), stopper='cork'),
    dict(id='hp_potion_large', shape='big', liq=((255, 70, 90), (170, 0, 24)), label=((240, 220, 130), (200, 30, 40)), stopper='gold'),
    dict(id='mp_potion_small', shape='round', liq=((120, 190, 255), (30, 70, 220)), label=((230, 230, 230), (40, 90, 220)), stopper='cork'),
    dict(id='mp_potion_large', shape='big', liq=((110, 170, 255), (20, 50, 200)), label=((240, 220, 130), (40, 90, 220)), stopper='gold'),
    dict(id='antidote', shape='vial', liq=((190, 255, 160), (50, 180, 70)), label=((235, 235, 235), (60, 170, 70)), stopper='cork'),
    dict(id='stamina_potion', shape='round', liq=((255, 240, 120), (240, 160, 20)), label=((230, 230, 230), (230, 150, 20)), stopper='cork'),
    dict(id='elixir_of_life', shape='big', liq=((255, 250, 170), (255, 190, 40)), label=((255, 240, 170), (230, 150, 30)), stopper='gem', tint=(255, 245, 210)),
    dict(id='rebirth_elixir', shape='vial', liq=((230, 160, 255), (110, 30, 200)), label=((40, 20, 60), (160, 90, 230)), stopper='gem', tint=(235, 215, 255)),
]


# --------------------------------------------------------------- crystals (magic stones)
def crystal_cluster(spec):
    bm = BM(spec['id'], size=128, ppu=2.0)
    base, hi = spec['c']
    def prism(cx, cz, h, w, lean=0.0, y0=0.0):
        steps = [(1.0, 0.0, 0.62), (0.82, 0.62, 0.9), (0.55, 0.9, 1.0)]
        for sc, a, b in steps:
            ww = w * sc
            x = cx + lean * (a + b) / 2
            bm.box((x - ww / 2, y0 + h * a, cz - ww / 2), (x + ww / 2, y0 + h * b + (0.1 if b < 1 else 0), cz + ww / 2), 'crystal')
    prism(CX, CZ, 11, 4.0)
    prism(CX - 3.4, CZ + 1, 6.5, 2.8, lean=-1.0, y0=0)
    prism(CX + 3.2, CZ - 1.2, 7.5, 3.0, lean=1.2, y0=0)
    prism(CX + 1, CZ + 3.2, 5, 2.4, lean=0.4)
    prism(CX - 1.2, CZ - 3.2, 4.4, 2.2, lean=-0.4)
    bm.box((CX - 5, -0.8, CZ - 4), (CX + 5, 0.4, CZ + 4), 'rock')
    bm.pack()
    bm.paint({'crystal': crystal(base, hi, 235, 0.55), 'rock': solid((50, 46, 56), 0.2)})
    return bm


CRYSTALS = [
    dict(id='mana_crystal_e', c=((120, 130, 150), (225, 232, 245))),
    dict(id='mana_crystal_d', c=((40, 150, 70), (170, 255, 190))),
    dict(id='mana_crystal_c', c=((40, 90, 210), (160, 200, 255))),
    dict(id='mana_crystal_b', c=((120, 40, 200), (225, 170, 255))),
    dict(id='mana_crystal_a', c=((220, 120, 20), (255, 230, 140))),
    dict(id='mana_crystal_s', c=((200, 20, 40), (255, 150, 160))),
    dict(id='shadow_essence', c=((20, 8, 40), (160, 80, 255))),
]


# --------------------------------------------------------------- misc
def coin(spec):
    bm = BM(spec['id'], size=128, ppu=3.0)
    for k in range(3):
        y = k * 1.0
        bm.box((CX - 2.0, y, CZ - 3.4 + k * 0.3), (CX + 2.0, y + 0.9, CZ + 3.4 + k * 0.3), 'gold')
        bm.box((CX - 3.4, y, CZ - 2.0 + k * 0.3), (CX + 3.4, y + 0.9, CZ + 2.0 + k * 0.3), 'gold')
        bm.box((CX - 3.0, y, CZ - 3.0 + k * 0.3), (CX + 3.0, y + 0.9, CZ + 3.0 + k * 0.3), 'gold')
    bm.box((CX - 1.2, 3.0, CZ - 1.2), (CX + 1.2, 3.3, CZ + 1.2), 'gem')
    bm.pack()
    bm.paint({'gold': metal((170, 110, 20), (255, 232, 120), global_grad=False), 'gem': gem((255, 250, 200), (230, 160, 30))})
    return bm


def key(spec):
    bm = BM(spec['id'], size=128, ppu=2.5)
    c = spec['c']
    # bow (ring)
    for (x0, y0, x1, y1) in ((-2.5, 9, 2.5, 10.2), (-2.5, 13.2, 2.5, 14.4), (-3.7, 10.2, -2.5, 13.2), (2.5, 10.2, 3.7, 13.2)):
        bm.box((CX + x0, y0, CZ - 0.8), (CX + x1, y1, CZ + 0.8), 'metal')
    bm.box((CX - 0.5, 14.4, CZ - 0.8), (CX + 0.5, 15.8, CZ + 0.8), 'metal')
    bm.box((CX - 1.0, 6.6, CZ - 1.0), (CX + 1.0, 9, CZ + 1.0), 'metal')
    bm.box((CX - 0.7, 0, CZ - 0.7), (CX + 0.7, 6.6, CZ + 0.7), 'metal')
    for i, (y, l) in enumerate(((0.2, 2.6), (1.6, 1.8), (2.8, 3.0), (4.2, 2.0))):
        bm.box((CX + 0.7, y, CZ - 0.6), (CX + 0.7 + l, y + 0.9, CZ + 0.6), 'metal')
    bm.box((CX - 1.4, 10.4, CZ - 1.1), (CX + 1.4, 13.0, CZ + 1.1), 'gem')
    bm.pack()
    bm.paint({'metal': metal(c[0], c[1]), 'gem': gem(spec['g'][0], spec['g'][1])})
    return bm


def scroll(spec):
    bm = BM(spec['id'], size=128, ppu=2.5)
    bm.box((CX - 4, 3, CZ - 1.6), (CX + 4, 14, CZ + 1.6), 'paper')
    bm.box((CX - 4.5, 2.4, CZ - 2), (CX + 4.5, 3.2, CZ + 2), 'wood')
    bm.box((CX - 4.5, 13.8, CZ - 2), (CX + 4.5, 14.6, CZ + 2), 'wood')
    bm.box((CX - 5.2, 2.0, CZ - 1.2), (CX - 4.5, 15, CZ + 1.2), 'gold')
    bm.box((CX + 4.5, 2.0, CZ - 1.2), (CX + 5.2, 15, CZ + 1.2), 'gold')
    bm.box((CX - 4.1, 7.2, CZ - 1.9), (CX + 4.1, 8.8, CZ + 1.9), 'band')
    bm.box((CX - 1.0, 7.0, CZ - 2.2), (CX + 1.0, 9.0, CZ + 2.2), 'gem')
    bm.pack()
    bm.paint({'paper': rune_mat((190, 170, 130), spec['g'][0]), 'wood': wood((60, 36, 20), (110, 70, 40)),
              'gold': metal((150, 100, 20), (255, 225, 110)), 'band': solid(spec['g'][1], 0.1), 'gem': gem(spec['g'][0], spec['g'][1])})
    return bm


def flat_card(spec):
    bm = BM(spec['id'], size=128, ppu=4.0)
    bm.box((CX - 5.5, 3, CZ - 0.3), (CX + 5.5, 12, CZ + 0.3), 'card')
    bm.box((CX - 5.8, 2.8, CZ - 0.2), (CX + 5.8, 3.0, CZ + 0.2), 'edge')
    bm.pack()
    bm.paint({'card': card_mat(spec['base'], spec['stripe']), 'edge': metal((150, 150, 160), (240, 240, 250))})
    return bm


def map_item(spec):
    bm = BM(spec['id'], size=256, ppu=4.0)
    kind = spec['kind']
    if kind == 'map':
        bm.box((CX - 6, 1, CZ - 0.6), (CX + 6, 14, CZ + 0.6), 'paper')
        bm.box((CX - 6.4, 0.6, CZ - 1.0), (CX + 6.4, 1.4, CZ + 1.0), 'wood')
        bm.box((CX - 6.4, 13.6, CZ - 1.0), (CX + 6.4, 14.6, CZ + 1.0), 'wood')
    else:
        bm.box((CX - 5.5, 1, CZ - 0.4), (CX + 5.5, 14.5, CZ + 0.4), 'paper')
        bm.box((CX - 5.6, 7.7, CZ - 0.6), (CX + 5.6, 8.3, CZ + 0.6), 'fold')
    bm.pack()
    bm.paint({'paper': paper_mat(kind), 'wood': wood((60, 36, 20), (110, 70, 40)), 'fold': solid((190, 190, 184), 0.05)})
    return bm


def rune_stone(spec):
    bm = BM(spec['id'], size=128, ppu=2.5)
    for (w, d, y0, y1) in ((7, 3, 0, 2), (9, 4, 2, 11), (7, 3, 11, 13), (4, 2, 13, 14.4)):
        bm.box((CX - w / 2, y0, CZ - d / 2), (CX + w / 2, y1, CZ + d / 2), 'stone')
    bm.pack()
    bm.paint({'stone': rune_mat(spec['c'], spec['g'])})
    return bm


MISC = [
    ('gold_coin', coin, dict()),
    ('dungeon_key', key, dict(c=((20, 30, 60), (90, 130, 210)), g=((130, 220, 255), (30, 90, 200)))),
    ('red_gate_key', key, dict(c=((60, 14, 20), (200, 60, 70)), g=((255, 140, 140), (180, 20, 30)))),
    ('skill_scroll', scroll, dict(g=((190, 120, 255), (90, 30, 180)))),
    ('hunter_license', flat_card, dict(base=(220, 226, 236), stripe=(40, 90, 200))),
    ('world_map', map_item, dict(kind='map')),
    ('hunter_news', map_item, dict(kind='news')),
    ('rune_stone', rune_stone, dict(c=(48, 44, 64), g=(130, 200, 255))),
]


# --------------------------------------------------------------- armor piece icons (3D)
def armor_piece(setspec, piece):
    bm = BM(f"{setspec['id']}_{piece}", size=256, ppu=2.5)
    trim = setspec.get('trim', 'plain')
    if piece == 'helmet':
        bm.box((CX - 4.2, 3, CZ - 4.2), (CX + 4.2, 10.4, CZ + 4.2), 'plate')
        bm.box((CX - 3.4, 10.4, CZ - 3.4), (CX + 3.4, 11.6, CZ + 3.4), 'plate')
        bm.box((CX - 3.6, 4, CZ - 4.7), (CX + 3.6, 7.2, CZ - 4.2), 'visor')
        bm.box((CX - 2.6, 4.7, CZ - 4.9), (CX - 0.4, 5.5, CZ - 4.6), 'eye')
        bm.box((CX + 0.4, 4.7, CZ - 4.9), (CX + 2.6, 5.5, CZ - 4.6), 'eye')
        bm.box((CX - 0.6, 11.6, CZ - 4.4), (CX + 0.6, 12.4, CZ + 4.4), 'trim')
        bm.box((CX - 4.6, 3, CZ - 3.8), (CX - 4.2, 8, CZ + 3.8), 'trim')
        bm.box((CX + 4.2, 3, CZ - 3.8), (CX + 4.6, 8, CZ + 3.8), 'trim')
        if setspec.get('horns'):
            for sd in (-1, 1):
                for k in range(4):
                    bm.box((CX + sd * (3.6 + k * 0.9) - 0.5, 9 + k * 1.4, CZ - 0.6), (CX + sd * (3.6 + k * 0.9) + 0.5, 10.6 + k * 1.4, CZ + 0.6), 'trim')
        if setspec.get('crest'):
            for k in range(4):
                bm.box((CX - 0.5, 11.6 + k * 1.2, CZ - 2.5 + k * 0.5), (CX + 0.5, 12.8 + k * 1.2, CZ + 3.5 + k * 0.7), 'trim')
    elif piece == 'chestplate':
        bm.box((CX - 5, 1.5, CZ - 3), (CX + 5, 12, CZ + 3), 'plate')
        bm.box((CX - 4.4, 0.2, CZ - 2.6), (CX + 4.4, 1.5, CZ + 2.6), 'trim')
        bm.box((CX - 3.6, 6, CZ - 3.5), (CX + 3.6, 11.2, CZ - 3.0), 'plate2')
        bm.box((CX - 1.2, 5.2, CZ - 3.8), (CX + 1.2, 8.8, CZ - 3.4), 'gem')
        for sd in (-1, 1):
            bm.box((CX + sd * 5 - (0 if sd > 0 else 3.4), 8.6, CZ - 2.6), (CX + sd * 5 + (3.4 if sd > 0 else 0), 13, CZ + 2.6), 'plate2')
            bm.box((CX + sd * 5.6 - (0 if sd > 0 else 2.4), 12.4, CZ - 2.2), (CX + sd * 5.6 + (2.4 if sd > 0 else 0), 14.4, CZ + 2.2), 'trim')
            bm.box((CX + sd * 6.2 - 0.6, 3.5, CZ - 1.8), (CX + sd * 6.2 + 0.6, 8.6, CZ + 1.8), 'plate')
        if setspec.get('cape'):
            bm.box((CX - 4.6, -3, CZ + 3.0), (CX + 4.6, 11, CZ + 3.5), 'cloth')
    elif piece == 'leggings':
        bm.box((CX - 5, 9, CZ - 2.6), (CX + 5, 12, CZ + 2.6), 'plate')
        bm.box((CX - 5.2, 11.2, CZ - 2.8), (CX + 5.2, 12.4, CZ + 2.8), 'trim')
        bm.box((CX - 1, 9.4, CZ - 3.1), (CX + 1, 11.2, CZ - 2.6), 'gem')
        for sd in (-1, 1):
            x0, x1 = (CX + 0.4, CX + 4.8) if sd > 0 else (CX - 4.8, CX - 0.4)
            bm.box((x0, 3.4, CZ - 2.2), (x1, 9, CZ + 2.2), 'plate2')
            bm.box((x0 + 0.4, 5.5, CZ - 2.7), (x1 - 0.4, 7.4, CZ - 2.2), 'trim')
            bm.box((x0, 0.2, CZ - 2.0), (x1, 3.4, CZ + 2.0), 'plate')
    else:
        for sd in (-1, 1):
            x0, x1 = (CX + 0.3, CX + 4.8) if sd > 0 else (CX - 4.8, CX - 0.3)
            bm.box((x0, 4, CZ - 2.2), (x1, 9, CZ + 2.2), 'plate2')
            bm.box((x0, 1.4, CZ - 2.4), (x1, 4, CZ + 2.4), 'plate')
            bm.box((x0 - 0.2, 0, CZ - 4.6), (x1 + 0.2, 1.4, CZ + 2.6), 'plate2')
            bm.box((x0, 4.6, CZ - 2.8), (x1, 6, CZ - 2.2), 'trim')
    bm.pack()
    P = setspec['p']
    mats = {'plate': metal(P['plate'][0], P['plate'][1], global_grad=True), 'plate2': metal(P['plate2'][0], P['plate2'][1], global_grad=True),
            'trim': metal(P['trim'][0], P['trim'][1], global_grad=True), 'gem': gem(P['gem'][0], P['gem'][1]),
            'visor': solid(P['visor'], 0.05), 'eye': gem(P['eye'][0], P['eye'][1]), 'cloth': wrap(P['cloth'][0], P['cloth'][1], stripe=3, vertical=True)}
    bm.paint(mats)
    return bm


ARMOR_SETS = [
    dict(id='hunter', name='Hunter', p=dict(plate=((24, 30, 52), (82, 98, 140)), plate2=((34, 42, 70), (110, 130, 175)), trim=((110, 116, 130), (220, 226, 240)),
         gem=((130, 210, 255), (40, 100, 200)), visor=(14, 16, 26), eye=((160, 230, 255), (40, 120, 220)), cloth=((20, 26, 48), (40, 52, 90)))),
    dict(id='knight', name='Red Knight', crest=True, cape=True, p=dict(plate=((54, 12, 20), (170, 34, 48)), plate2=((34, 24, 28), (110, 90, 98)), trim=((120, 90, 40), (255, 220, 120)),
         gem=((255, 120, 120), (180, 10, 20)), visor=(10, 8, 10), eye=((255, 200, 160), (255, 40, 30)), cloth=((70, 10, 16), (140, 24, 34)))),
    dict(id='ice', name='Ice', crest=True, p=dict(plate=((80, 130, 190), (210, 240, 255)), plate2=((110, 160, 215), (235, 250, 255)), trim=((150, 200, 240), (255, 255, 255)),
         gem=((220, 250, 255), (70, 170, 255)), visor=(30, 50, 90), eye=((255, 255, 255), (90, 200, 255)), cloth=((90, 150, 210), (190, 230, 255)))),
    dict(id='monarch', name='Shadow Monarch', horns=True, cape=True, p=dict(plate=((8, 6, 18), (58, 40, 110)), plate2=((14, 10, 28), (84, 58, 150)), trim=((50, 30, 100), (170, 120, 255)),
         gem=((220, 170, 255), (100, 30, 210)), visor=(4, 2, 10), eye=((230, 190, 255), (150, 50, 255)), cloth=((10, 6, 22), (38, 24, 80)))),
]
ARMOR_PIECES = ['helmet', 'chestplate', 'leggings', 'boots']

DISPLAY_ARMOR = {k: dict(v) for k, v in DISPLAY_ITEM.items()}
DISPLAY_ARMOR['gui'] = {'rotation': [20, -30, 0], 'translation': [0, -1.0, 0], 'scale': [1.05, 1.05, 1.05]}


def all_models():
    out = {}
    for s in BOTTLES:
        out[s['id']] = (bottle(s), DISPLAY_ITEM)
    for s in CRYSTALS:
        out[s['id']] = (crystal_cluster(s), DISPLAY_ITEM)
    for name, fn, kw in MISC:
        out[name] = (fn(dict(id=name, **kw)), DISPLAY_ITEM)
    for st in ARMOR_SETS:
        for pc in ARMOR_PIECES:
            out[f"{st['id']}_{pc}"] = (armor_piece(st, pc), DISPLAY_ARMOR)
    return out


if __name__ == '__main__':
    import os
    os.makedirs('/tmp/prev', exist_ok=True)
    ms = all_models()
    tiles = [bm.render(yaw=-30, pitch=18, size=240) for bm, _ in ms.values()]
    cols = 8
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new('RGB', (240 * cols, 240 * rows))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % cols) * 240, (i // cols) * 240))
    sheet.save('/tmp/prev/items.png')
    print(len(ms), list(ms)[:3])
