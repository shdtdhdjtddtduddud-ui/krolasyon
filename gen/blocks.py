"""Block textures (32x32 HD), models, blockstates, loot tables and tags."""
import os, json, math
import numpy as np
import tex as T

H = T.hexc
S = 32


def W(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=1)


def noise_tex(base, var=0.25, cell=4, seed=1, speck=0.1):
    n = T.value_noise(S, S, cell, seed, 3)
    w = T.white(S, S, seed + 1)
    return T.colorize(H(base), np.clip(0.5 + (n - 0.5) * 1.2 + (w - 0.5) * speck, 0, 1), 1 - var, 1 + var * 0.6)


def asphalt(seed=3):
    img = noise_tex('#3A3C40', 0.25, 3, seed, 0.5)
    r = T.rng(seed)
    for _ in range(40):
        x, y = r.integers(0, S, 2)
        T.rect(img, x, y, x + 1, y + 1, np.append(H('#5A5C60')[:3] * r.uniform(0.8, 1.3), 1))
    return img


def painted(base_img, color, wear=0.25, seed=5):
    img = base_img.copy()
    n = T.value_noise(S, S, 3, seed, 2)
    m = (n > wear).astype(np.float32)
    T.draw_mask(img, m * 0.9, H(color))
    return img


def tiles(base, grout, size=8, seed=7, bevel=True):
    img = noise_tex(base, 0.18, 4, seed, 0.2)
    r = T.rng(seed)
    for ty in range(0, S, size):
        off = (size // 2) if (ty // size) % 2 else 0
        for tx in range(-size, S, size):
            x0 = tx + off
            shade = r.uniform(0.9, 1.1)
            T.rect(img, x0, ty, x0 + size, ty + size, np.append(np.clip(H(base)[:3] * shade, 0, 1), 0.35))
            T.rect(img, x0, ty, x0 + size, ty + 1, H(grout))
            T.rect(img, x0, ty, x0 + 1, ty + size, H(grout))
            if bevel:
                T.rect(img, x0 + 1, ty + 1, x0 + size, ty + 2, np.append(np.clip(H(base)[:3] * 1.25, 0, 1), 0.5))
    return img


def bricks(base, mortar, bw=16, bh=8, seed=9, cracks=0, crack_color=None, glow_runes=None):
    img = noise_tex(base, 0.2, 4, seed, 0.15)
    r = T.rng(seed)
    for row in range(0, S, bh):
        off = (bw // 2) if (row // bh) % 2 else 0
        for col in range(-bw, S, bw):
            x0 = col + off
            sh = r.uniform(0.82, 1.15)
            T.rect(img, x0 + 1, row + 1, x0 + bw, row + bh, np.append(np.clip(H(base)[:3] * sh, 0, 1), 0.6))
            T.rect(img, x0 + 1, row + 1, x0 + bw, row + 2, np.append(np.clip(H(base)[:3] * 1.35, 0, 1), 0.5))
            T.rect(img, x0, row, x0 + bw, row + 1, H(mortar))
            T.rect(img, x0, row, x0 + 1, row + bh, H(mortar))
    for _ in range(cracks):
        x, y = r.uniform(0, S), r.uniform(0, S)
        for _k in range(4):
            a = r.uniform(0, 2 * math.pi)
            x2, y2 = x + math.cos(a) * 4, y + math.sin(a) * 4
            T.line(img, x, y, x2, y2, H(crack_color or '#000000'), 1.0)
            x, y = x2, y2
    if glow_runes:
        T.decal_runes(img, glow_runes, seed + 3, None, 1.2)
    return img


def panels(base, seam, cols=2, rows=2, seed=11, gloss=False):
    img = noise_tex(base, 0.08, 6, seed, 0.05)
    for i in range(cols):
        x = i * S // cols
        T.rect(img, x, 0, x + 1, S, H(seam))
    for j in range(rows):
        y = j * S // rows
        T.rect(img, 0, y, S, y + 1, H(seam))
    if gloss:
        ys, xs = np.mgrid[0:S, 0:S]
        hl = np.clip(1 - np.abs((xs + ys) - 20) / 6, 0, 1) * 0.25
        T.draw_mask(img, hl, H('#FFFFFF'))
    return img


def glass_facade(seed=13):
    ys = np.linspace(0, 1, S)[:, None]
    img = T.ramp([H('#1E3A5E'), H('#3E78B0'), H('#9FD0F0')], np.broadcast_to(np.clip(1 - ys * 0.9 + T.value_noise(S, S, 8, seed, 2) * 0.2, 0, 1), (S, S)))
    img[:, :, 3] = 1
    ys2, xs = np.mgrid[0:S, 0:S]
    refl = np.clip(1 - np.abs((xs * 0.7 + ys2) - 14) / 5, 0, 1) * 0.35
    T.draw_mask(img, refl, H('#E8F6FF'))
    for k in (0, 16):
        T.rect(img, k, 0, k + 1, S, H('#AFC2D4'))
        T.rect(img, 0, k, S, k + 1, H('#AFC2D4'))
    return img


def window(lit, seed=17):
    img = T.new(S, S, H('#B8BEC6'))
    inner = T.new(28, 28, H('#FFD98A') if lit else H('#14243A'))
    if lit:
        n = T.value_noise(28, 28, 6, seed, 2)
        inner = T.ramp([H('#FFB84A'), H('#FFE8A8'), H('#FFF6DA')], n)
        inner[:, :, 3] = 1
        for y in range(2, 28, 4):  # blinds
            T.rect(inner, 0, y, 28, y + 1, np.append(H('#C8902A')[:3], 0.6))
        T.rect(inner, 18, 14, 24, 28, np.append(H('#4A3A2A')[:3], 0.7))  # silhouette of a plant / person
    else:
        ys, xs = np.mgrid[0:28, 0:28]
        refl = np.clip(1 - np.abs((xs + ys) - 18) / 4, 0, 1) * 0.4
        T.draw_mask(inner, refl, H('#5A8AC0'))
    T.put(img, inner, 2, 2)
    T.rect(img, 15, 2, 17, 30, H('#9AA2AC'))
    return T.edge_dark(img, 0.3)


def neon(color, seed=19):
    img = T.new(S, S, H('#0E0E14'))
    c = H(color)
    glow = T.ellipse_mask(S, S, S / 2, S / 2, S * 0.7, S * 0.18, 1.5)
    T.draw_mask(img, glow * 0.45, c)
    T.rect(img, 3, 14, 29, 18, np.clip(c * 1.2 + 0.25, 0, 1))
    T.rect(img, 3, 15, 29, 17, H('#FFFFFF'))
    for x in (6, 16, 26):
        T.rect(img, x, 6, x + 2, 26, np.append(c[:3], 0.6))
    return img


def emblem(bg, fg, kind, seed=23):
    img = panels(bg, '#00000080'[:7], 1, 1, seed, gloss=True)
    c = H(fg)
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32) + 0.5
    cx = cy = S / 2
    if kind == 'association':  # hexagon with wings and a star
        hexm = ((np.abs(xs - cx) * 0.866 + np.abs(ys - cy) * 0.5) < 11) & (np.abs(ys - cy) < 12)
        T.draw_mask(img, hexm.astype(np.float32), c)
        inner = ((np.abs(xs - cx) * 0.866 + np.abs(ys - cy) * 0.5) < 8) & (np.abs(ys - cy) < 9)
        T.draw_mask(img, inner.astype(np.float32), H(bg))
        for i in range(5):
            a = -math.pi / 2 + i * 2 * math.pi / 5
            T.line(img, cx, cy, cx + math.cos(a) * 6, cy + math.sin(a) * 6, c, 2)
    elif kind == 'hunters':  # golden H with eagle wings
        T.rect(img, 10, 8, 13, 24, c)
        T.rect(img, 19, 8, 22, 24, c)
        T.rect(img, 10, 15, 22, 18, c)
        for s in (-1, 1):
            for k in range(4):
                T.line(img, cx + s * 7, 10 + k * 3, cx + s * 14, 6 + k * 3, c, 1.5)
    elif kind == 'white_tiger':  # tiger head with stripes
        T.draw_mask(img, T.ellipse_mask(S, S, cx, cy, 11, 10, 0.3), c)
        for k, y in enumerate((9, 13, 17)):
            T.line(img, 8, y, 14, y + 2, H('#1A1A1A'), 1.5)
            T.line(img, 24, y, 18, y + 2, H('#1A1A1A'), 1.5)
        for s in (-1, 1):
            T.draw_mask(img, T.ellipse_mask(S, S, cx + s * 5, cy + 1, 2, 1.5, 0.3), H('#3A8AFF'))
    elif kind == 'fiend':  # horned skull
        T.draw_mask(img, T.ellipse_mask(S, S, cx, cy + 1, 9, 9, 0.3), c)
        for s in (-1, 1):
            T.line(img, cx + s * 6, cy - 6, cx + s * 12, cy - 13, c, 2.5)
            T.draw_mask(img, T.ellipse_mask(S, S, cx + s * 4, cy, 2.5, 2.5, 0.3), H('#0A0A0A'))
        T.rect(img, 12, 21, 20, 23, H('#0A0A0A'))
    elif kind == 'knights':  # shield with a sword
        sh = ((np.abs(xs - cx) < 10) & (ys > 6) & (ys < 18)) | ((np.abs(xs - cx) < 10 - (ys - 18) * 0.9) & (ys >= 18) & (ys < 28))
        T.draw_mask(img, sh.astype(np.float32), c)
        T.rect(img, 15, 7, 17, 26, H('#E8E8F0'))
        T.rect(img, 11, 11, 21, 13, H('#E8C860'))
    elif kind == 'ahjin':  # shadow crown
        T.rect(img, 7, 18, 25, 23, c)
        for x in (8, 15, 22):
            T.line(img, x + 1, 18, x + 1, 8, c, 3)
            T.draw_mask(img, T.ellipse_mask(S, S, x + 1, 7, 1.8, 1.8, 0.3), H('#E8D8FF'))
    return img


def mana_ore(seed=29):
    img = noise_tex('#2A2E3A', 0.3, 3, seed, 0.3)
    r = T.rng(seed)
    for _ in range(6):
        x, y = r.uniform(4, 28), r.uniform(4, 28)
        L = r.uniform(3, 7)
        a = r.uniform(0, math.pi)
        T.line(img, x, y, x + math.cos(a) * L, y + math.sin(a) * L, H('#6AD8FF'), 2.2)
        T.line(img, x, y, x + math.cos(a) * L * 0.7, y + math.sin(a) * L * 0.7, H('#E0FAFF'), 0.9)
    return img


def hive(seed=31):
    img = noise_tex('#6A4A2E', 0.3, 4, seed, 0.2)
    ys, xs = np.mgrid[0:S, 0:S].astype(np.float32)
    for cy in range(0, S + 8, 8):
        for cx in range(0, S + 8, 8):
            off = 4 if (cy // 8) % 2 else 0
            m = T.ellipse_mask(S, S, cx + off, cy, 3.5, 3.2, 0.6)
            T.draw_mask(img, m * 0.7, H('#2A1A0E'))
    return img


def pillar_side(seed=37):
    img = noise_tex('#D6C49A', 0.15, 6, seed, 0.12)
    for x in range(2, S, 6):
        T.rect(img, x, 0, x + 2, S, np.append(H('#A8946A')[:3], 0.8))
        T.rect(img, x + 2, 0, x + 3, S, np.append(H('#F0E2C0')[:3], 0.6))
    T.rect(img, 0, 0, S, 2, H('#B8A47A'))
    T.rect(img, 0, S - 2, S, S, H('#B8A47A'))
    return img


def portal_frames(n=16):
    frames = []
    for k in range(n):
        ys, xs = np.mgrid[0:S, 0:S].astype(np.float32) + 0.5
        dx, dy = xs - S / 2, ys - S / 2
        r = np.sqrt(dx * dx + dy * dy) / (S / 2)
        a = np.arctan2(dy, dx)
        t = k / n * 2 * math.pi
        swirl = (np.sin(a * 3 + r * 9 - t * 2) + 1) / 2
        v = np.clip(swirl * 0.6 + (1 - r) * 0.6, 0, 1)
        img = T.ramp([H('#1A0A4A'), H('#4A3AD0'), H('#7FD8FF'), H('#FFFFFF')], v)
        img[:, :, 3] = np.clip(0.55 + v * 0.45, 0, 1)
        frames.append(img)
    return np.concatenate(frames, 0)


def news_frames(n=8):
    frames = []
    for k in range(n):
        img = T.new(S, S, H('#06080E'))
        T.rect(img, 0, 0, S, 7, H('#B0141E'))
        for i, x in enumerate(range(2, 30, 4)):
            T.rect(img, x, 2, x + 2, 5, H('#FFFFFF'))
        r = T.rng(100 + k)
        for row, y in enumerate((10, 15, 20)):
            w = r.integers(14, 28)
            T.rect(img, 2, y, 2 + w, y + 2, np.append(H('#E8F0FF')[:3], 0.9))
        T.rect(img, 0, 26, S, 32, H('#FFC83A'))
        off = (k * 4) % 32
        for x in range(-32, 32, 8):
            T.rect(img, x + off, 28, x + off + 5, 30, H('#2A1A00'))
        frames.append(img)
    return np.concatenate(frames, 0)


SIMPLE = {
    'asphalt': lambda: asphalt(),
    'road_line': lambda: painted(asphalt(4), '#F2C230', 0.3, 5),
    'road_crossing': lambda: painted(asphalt(6), '#F2F2F0', 0.28, 7),
    'sidewalk': lambda: tiles('#A8A49A', '#6E6A62', 8, 41),
    'concrete_panel': lambda: panels('#D8DADC', '#A8AAAE', 2, 2, 43),
    'dark_panel': lambda: panels('#1E2026', '#08090C', 2, 2, 47, gloss=True),
    'glass_facade': lambda: glass_facade(),
    'office_window': lambda: window(True),
    'dark_window': lambda: window(False),
    'neon_blue': lambda: neon('#3AA8FF'),
    'neon_red': lambda: neon('#FF2A3A'),
    'neon_purple': lambda: neon('#B04AFF'),
    'neon_gold': lambda: neon('#FFC83A'),
    'association_emblem': lambda: emblem('#14306A', '#5FC8FF', 'association'),
    'emblem_hunters': lambda: emblem('#141414', '#E8C547', 'hunters'),
    'emblem_white_tiger': lambda: emblem('#2A3A4A', '#F2F2F2', 'white_tiger'),
    'emblem_fiend': lambda: emblem('#1A0A0A', '#D6453B', 'fiend'),
    'emblem_knights': lambda: emblem('#E8ECF2', '#4B86E8', 'knights'),
    'emblem_ahjin': lambda: emblem('#0E0A16', '#8E5BFF', 'ahjin'),
    'dungeon_bricks': lambda: bricks('#3A4050', '#16181E', cracks=2, crack_color='#0A0A0E'),
    'rune_bricks': lambda: bricks('#2E3444', '#10121A', glow_runes='#5FE8FF', seed=53),
    'dungeon_floor': lambda: tiles('#40444E', '#1A1C22', 16, 59),
    'temple_stone': lambda: bricks('#CDB98C', '#9A8460', 16, 16, 61, cracks=1, crack_color='#7A6440'),
    'demon_bricks': lambda: bricks('#4A1414', '#1A0606', cracks=3, crack_color='#FF5A1A', seed=67),
    'hive_wall': lambda: hive(),
    'mana_crystal_ore': lambda: mana_ore(),
}


def run(res_root, data_root, java_simple_ids):
    bt = os.path.join(res_root, 'textures', 'block')
    os.makedirs(bt, exist_ok=True)
    for name, fn in SIMPLE.items():
        T.save(fn(), os.path.join(bt, name + '.png'))
        W(os.path.join(res_root, 'models', 'block', name + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'sololeveling:block/' + name}})
        W(os.path.join(res_root, 'blockstates', name + '.json'), {'variants': {'': {'model': 'sololeveling:block/' + name}}})
    # pillar
    T.save(pillar_side(), os.path.join(bt, 'temple_pillar.png'))
    T.save(tiles('#CDB98C', '#9A8460', 16, 71), os.path.join(bt, 'temple_pillar_top.png'))
    W(os.path.join(res_root, 'models', 'block', 'temple_pillar.json'), {'parent': 'minecraft:block/cube_column', 'textures': {'side': 'sololeveling:block/temple_pillar', 'end': 'sololeveling:block/temple_pillar_top'}})
    W(os.path.join(res_root, 'blockstates', 'temple_pillar.json'), {'variants': {'': {'model': 'sololeveling:block/temple_pillar'}}})
    # portal (animated, translucent)
    T.save(portal_frames(), os.path.join(bt, 'region_portal.png'))
    W(os.path.join(bt, 'region_portal.png.mcmeta'), {'animation': {'frametime': 2, 'interpolate': True}})
    W(os.path.join(res_root, 'models', 'block', 'region_portal.json'), {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                                                         'textures': {'all': 'sololeveling:block/region_portal'}})
    W(os.path.join(res_root, 'blockstates', 'region_portal.json'), {'variants': {'': {'model': 'sololeveling:block/region_portal'}}})
    # news board
    T.save(news_frames(), os.path.join(bt, 'news_board_screen.png'))
    W(os.path.join(bt, 'news_board_screen.png.mcmeta'), {'animation': {'frametime': 6}})
    T.save(panels('#1A1C22', '#08090C', 1, 1, 73), os.path.join(bt, 'news_board_side.png'))
    W(os.path.join(res_root, 'models', 'block', 'news_board.json'), {
        'parent': 'minecraft:block/block', 'textures': {'particle': 'sololeveling:block/news_board_side', 'screen': 'sololeveling:block/news_board_screen', 'side': 'sololeveling:block/news_board_side'},
        'elements': [
            {'from': [0, 0, 6], 'to': [16, 16, 10], 'faces': {
                'north': {'uv': [0, 0, 16, 16], 'texture': '#screen'}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#side'},
                'east': {'uv': [6, 0, 10, 16], 'texture': '#side'}, 'west': {'uv': [6, 0, 10, 16], 'texture': '#side'},
                'up': {'uv': [0, 6, 16, 10], 'texture': '#side'}, 'down': {'uv': [0, 6, 16, 10], 'texture': '#side'}}},
            {'from': [0.5, 0.5, 5.9], 'to': [15.5, 15.5, 6], 'shade': False, 'forge_data': {'block_light': 15, 'sky_light': 15},
             'faces': {'north': {'uv': [0.5, 0.5, 15.5, 15.5], 'texture': '#screen'}}}]})
    W(os.path.join(res_root, 'blockstates', 'news_board.json'), {'variants': {
        'facing=north': {'model': 'sololeveling:block/news_board'}, 'facing=east': {'model': 'sololeveling:block/news_board', 'y': 90},
        'facing=south': {'model': 'sololeveling:block/news_board', 'y': 180}, 'facing=west': {'model': 'sololeveling:block/news_board', 'y': 270}}})
    # item models for blocks
    all_blocks = list(SIMPLE) + ['temple_pillar', 'region_portal', 'news_board']
    for b in all_blocks:
        W(os.path.join(res_root, 'models', 'item', b + '.json'), {'parent': 'sololeveling:block/' + b})
    # loot tables
    lt = os.path.join(data_root, 'loot_tables', 'blocks')
    for b in all_blocks:
        if b == 'region_portal':
            continue
        if b == 'mana_crystal_ore':
            W(os.path.join(lt, b + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
                {'type': 'minecraft:alternatives', 'children': [
                    {'type': 'minecraft:item', 'name': 'sololeveling:mana_crystal_ore', 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}]},
                    {'type': 'minecraft:item', 'name': 'sololeveling:magic_stone_d', 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}},
                                                                                                  {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'}]}]}]}]})
            continue
        W(os.path.join(lt, b + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'sololeveling:' + b}],
                                                                                  'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    mine = [f'sololeveling:{b}' for b in all_blocks if b != 'region_portal']
    W(os.path.join(os.path.dirname(data_root), 'minecraft', 'tags', 'blocks', 'mineable', 'pickaxe.json'), {'replace': False, 'values': mine})
    return all_blocks
