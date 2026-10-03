"""Block textures (16x16, tileable) and the Java block table.  python3 gen/blocks.py"""
import os, sys, json, math
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from sprites import pal, hexc

ROOT = os.path.join(os.path.dirname(__file__), '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/krolasyonbosses')
DATA = os.path.join(ROOT, 'src/main/resources/data')
JAVA = os.path.join(ROOT, 'src/main/java/com/krolasyon/bosses/registry')
N = 16


def tnoise(seed, cells=4, oct=3, n=N):
    """tileable value noise in [0,1]"""
    r = np.random.default_rng(seed)
    out = np.zeros((n, n))
    amp, tot = 1.0, 0.0
    for o in range(oct):
        c = cells * (2 ** o)
        g = r.random((c, c))
        ys = np.arange(n) / n * c
        xs = np.arange(n) / n * c
        y0 = ys.astype(int) % c; x0 = xs.astype(int) % c
        y1 = (y0 + 1) % c; x1 = (x0 + 1) % c
        fy = (ys - np.floor(ys))[:, None]; fx = (xs - np.floor(xs))[None, :]
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        a = g[y0][:, x0]; b = g[y0][:, x1]; cc = g[y1][:, x0]; d = g[y1][:, x1]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (cc * (1 - fx) + d * fx) * fy)
        tot += amp
        amp *= 0.5
    return out / tot


BAY = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]], np.float32) / 16 + 1 / 32


def ramp(t, colors, dither=True):
    n = len(colors)
    x = np.clip(t, 0, 0.9999) * (n - 1)
    lo = np.floor(x).astype(int)
    fr = x - lo
    bay = np.tile(BAY, (N // 4, N // 4))
    pick = lo + (np.clip((fr - 0.25) * 2, 0, 1) > bay) if dither else lo + (fr > 0.5)
    return np.array(colors, np.float32)[np.clip(pick, 0, n - 1)]


def voronoi_t(seed, cell=5, n=N):
    """tileable voronoi: returns (distance to nearest, gap to 2nd nearest, cell id value)"""
    r = np.random.default_rng(seed)
    c = n // cell
    pts = (np.stack(np.meshgrid(np.arange(c), np.arange(c), indexing='ij'), -1) + r.random((c, c, 2)) * 0.9 + 0.05) * cell
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    ds = []
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            for i in range(c):
                for j in range(c):
                    py, px = pts[i, j] + np.array([dy, dx]) * n
                    ds.append((np.hypot(yy - py, xx - px), (i * 7 + j * 13) % 11 / 11.0))
    ds.sort(key=lambda t: 0)
    D = np.stack([d for d, _ in ds])
    V = np.stack([np.full((n, n), v) for _, v in ds])
    idx = np.argsort(D, 0)
    d1 = np.take_along_axis(D, idx[:1], 0)[0]
    d2 = np.take_along_axis(D, idx[1:2], 0)[0]
    v1 = np.take_along_axis(V, idx[:1], 0)[0]
    return d1, d2 - d1, v1


def img(arr, alpha=None):
    a = np.clip(arr, 0, 255).astype(np.uint8)
    if alpha is None:
        alpha = np.full((N, N), 255, np.uint8)
    return Image.fromarray(np.dstack([a, alpha]), 'RGBA')


def stone(seed, colors, crack=0.35, spots=0.0):
    t = tnoise(seed, 4, 3) * 0.8 + tnoise(seed + 1, 8, 1) * 0.35 - 0.1
    d1, gap, v = voronoi_t(seed + 2, 5)
    t = t + (v - 0.5) * 0.15 - np.clip(1.0 - gap / 1.4, 0, 1) * crack
    col = ramp(t, colors)
    if spots:
        r = np.random.default_rng(seed + 9)
        m = r.random((N, N)) < spots
        col[m] = np.array(colors[0], np.float32) * 0.7
    return col


def tex_ashstone():
    return img(stone(11, pal('#1c1c20', '#2c2c32', '#3e3e46', '#52525c', '#6a6a76'), 0.4, 0.03))


def tex_ashstone_bricks():
    base = stone(12, pal('#20202a', '#323240', '#46465a', '#5e5e76'), 0.1)
    yy, xx = np.mgrid[0:N, 0:N]
    mortar = (yy % 8 == 0) | (((xx + (yy // 8) * 4) % 8) == 0)
    base[mortar] = (14, 14, 20)
    hl = ((yy % 8) == 1) & ~mortar
    base[hl] = np.minimum(base[hl] * 1.25, 255)
    return img(base)


def tex_ember_rock():
    d1, gap, v = voronoi_t(21, 5)
    t = tnoise(22, 4, 3) * 0.7 + (v - 0.5) * 0.3
    base = ramp(t, pal('#0e0a0a', '#1a1210', '#2a1c16', '#3e2a20', '#5a3e2e'))
    crack = gap < 0.9
    glow = np.clip(1.0 - gap / 1.8, 0, 1)
    lava = ramp(glow * 0.9 + tnoise(23, 6, 1) * 0.2, pal('#8a1c04', '#e0480c', '#ff8a1c', '#ffd060'), False)
    col = np.where(crack[..., None], lava, base)
    return img(col)


def tex_bone_sand():
    t = tnoise(31, 6, 3)
    col = ramp(t, pal('#b8aa88', '#d2c6a4', '#e6dcbc', '#f4ecd2'))
    r = np.random.default_rng(32)
    for _ in range(5):
        x, y = r.integers(0, N, 2)
        for k in range(3):
            col[(y + k // 2) % N, (x + k) % N] = (250, 244, 224) if k != 1 else (170, 158, 128)
    return img(col)


def tex_blood_moss():
    t = tnoise(41, 5, 3) * 0.8 + stretch(42)
    col = ramp(t, pal('#1c0408', '#3a0a14', '#5e1424', '#8a2036', '#b83450'))
    r = np.random.default_rng(43)
    m = r.random((N, N)) < 0.05
    col[m] = (230, 70, 90)
    return img(col)


def stretch(seed):
    return (tnoise(seed, 8, 1) * 0.3)


def tex_shadow_loam():
    t = tnoise(51, 4, 3)
    col = ramp(t, pal('#08040f', '#120a24', '#1e1238', '#2e1e58', '#44308a'))
    r = np.random.default_rng(52)
    m = r.random((N, N)) < 0.04
    col[m] = (170, 120, 255)
    return img(col)


def tex_rot_mud():
    t = tnoise(61, 4, 3)
    col = ramp(t, pal('#1a1608', '#2e260e', '#46391a', '#645228', '#84703a'))
    g = tnoise(62, 3, 2) > 0.62
    col[g] = ramp(tnoise(63, 5, 2), pal('#1c3a10', '#34621c', '#5a9230'))[g]
    r = np.random.default_rng(64)
    m = r.random((N, N)) < 0.03
    col[m] = (210, 250, 90)
    return img(col)


def tex_hellsteel_ore():
    base = stone(71, pal('#1c1c20', '#2c2c32', '#3e3e46', '#52525c', '#6a6a76'), 0.35)
    r = np.random.default_rng(72)
    for _ in range(4):
        cx, cy = r.integers(2, N - 2, 2)
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 3 + r.integers(0, 2):
                    shade = (dx + dy) % 3
                    base[(cy + dy) % N, (cx + dx) % N] = [(36, 30, 52), (66, 56, 90), (104, 92, 140)][shade]
        base[cy % N, cx % N] = (230, 36, 60)
        if r.random() < 0.7:
            base[(cy + 1) % N, (cx + 1) % N] = (255, 120, 130)
    return img(base)


def tex_hellgate_stone():
    yy, xx = np.mgrid[0:N, 0:N]
    t = tnoise(81, 4, 2) * 0.6
    col = ramp(t, pal('#06040a', '#0e0a16', '#1a1428', '#2c2244'))
    # carved border
    edge = (yy == 0) | (yy == N - 1) | (xx == 0) | (xx == N - 1)
    col[edge] = (40, 28, 52)
    # glowing rune cross
    rune = ((np.abs(xx - 7.5) < 1.0) & (yy > 3) & (yy < 12)) | ((np.abs(yy - 7.5) < 1.0) & (xx > 3) & (xx < 12)) | ((np.abs(xx - 7.5) + np.abs(yy - 7.5) == 5))
    col[rune] = (220, 36, 60)
    col[(np.abs(xx - 7.5) < 0.6) & (np.abs(yy - 7.5) < 0.6)] = (255, 160, 140)
    return img(col)


def lamp(seed, ring, mid, core):
    yy, xx = np.mgrid[0:N, 0:N]
    d = np.maximum(np.abs(xx - 7.5), np.abs(yy - 7.5))
    t = np.clip(1.0 - d / 7.2, 0, 1) + (tnoise(seed, 4, 2) - 0.5) * 0.25
    col = ramp(t, [ring[0], ring[1], mid[0], mid[1], core[0], core[1]])
    frame = (d > 6.4)
    col[frame] = ring[0]
    col[(d > 5.6) & (d <= 6.4)] = ring[1]
    bars = ((xx % 8) == 0) | ((yy % 8) == 0)
    col[bars & (d < 6.4)] = np.array(col[bars & (d < 6.4)]) * 0.7
    return img(col)


def tex_ember_lamp():
    return lamp(91, ((60, 30, 14), (110, 56, 20)), ((200, 90, 20), (255, 140, 30)), ((255, 200, 80), (255, 244, 190)))


def tex_soul_lamp():
    return lamp(92, ((20, 40, 56), (40, 80, 110)), ((40, 150, 200), (80, 210, 250)), ((160, 240, 255), (240, 252, 255)))


def tex_blood_lamp():
    return lamp(93, ((54, 10, 20), (100, 20, 36)), ((190, 30, 56), (240, 60, 90)), ((255, 130, 150), (255, 220, 224)))


def tex_shadow_lamp():
    return lamp(94, ((26, 14, 50), (52, 30, 100)), ((110, 70, 210), (160, 120, 255)), ((210, 190, 255), (245, 240, 255)))


def tex_rot_lamp():
    return lamp(95, ((30, 44, 14), (56, 84, 24)), ((120, 180, 40), (170, 230, 70)), ((220, 255, 120), (248, 255, 210)))


def portal_frames(nf=16):
    """vertical strip of swirling red-black portal frames (16 x 16*nf)"""
    out = Image.new('RGBA', (N, N * nf))
    yy, xx = np.mgrid[0:N, 0:N]
    cx = cy = 7.5
    ang = np.arctan2(yy - cy, xx - cx)
    rad = np.hypot(xx - cx, yy - cy)
    base = tnoise(100, 4, 3)
    for f in range(nf):
        ph = f / nf * math.tau
        swirl = np.sin(ang * 3 + rad * 0.9 - ph) * 0.5 + 0.5
        n2 = np.roll(base, f, 1)
        t = swirl * 0.65 + n2 * 0.45 - rad * 0.02
        t = np.clip(t, 0, 1)
        col = ramp(t, pal('#12020a', '#3a0614', '#7a0c24', '#c8203a', '#ff5a3a', '#ffc090'), False)
        a = np.full((N, N), 215, np.uint8)
        out.paste(img(col, a), (0, f * N))
    return out


BLOCKS = [
    # id, tr, en, texture fn, hardness, resistance, light, kind
    ('ashstone', 'Kül Taşı', 'Ashstone', tex_ashstone, 1.8, 6.0, 0, 'plain'),
    ('ashstone_bricks', 'Kül Taşı Tuğlası', 'Ashstone Bricks', tex_ashstone_bricks, 2.0, 7.0, 0, 'plain'),
    ('ember_rock', 'Kor Kayası', 'Ember Rock', tex_ember_rock, 2.0, 6.0, 7, 'plain'),
    ('bone_sand', 'Kemik Kumu', 'Bone Sand', tex_bone_sand, 0.8, 1.0, 0, 'plain'),
    ('blood_moss', 'Kan Yosunu', 'Blood Moss', tex_blood_moss, 0.8, 1.0, 0, 'plain'),
    ('shadow_loam', 'Gölge Toprağı', 'Shadow Loam', tex_shadow_loam, 1.0, 1.5, 0, 'plain'),
    ('rot_mud', 'Çürük Çamur', 'Rot Mud', tex_rot_mud, 0.8, 1.0, 0, 'plain'),
    ('hellsteel_ore', 'Cehennem Çeliği Cevheri', 'Hellsteel Ore', tex_hellsteel_ore, 3.5, 6.0, 0, 'ore'),
    ('hellgate_stone', 'Cehennem Kapısı Taşı', 'Hellgate Stone', tex_hellgate_stone, 35.0, 1200.0, 4, 'plain'),
    ('ember_lamp', 'Kor Feneri', 'Ember Lamp', tex_ember_lamp, 0.5, 1.0, 15, 'plain'),
    ('soul_lamp', 'Ruh Feneri', 'Soul Lamp', tex_soul_lamp, 0.5, 1.0, 14, 'plain'),
    ('blood_lamp', 'Kan Feneri', 'Blood Lamp', tex_blood_lamp, 0.5, 1.0, 14, 'plain'),
    ('shadow_lamp', 'Gölge Feneri', 'Shadow Lamp', tex_shadow_lamp, 0.5, 1.0, 12, 'plain'),
    ('rot_lamp', 'Çürük Feneri', 'Rot Lamp', tex_rot_lamp, 0.5, 1.0, 14, 'plain'),
]


def main():
    os.makedirs(ASSETS + '/textures/block', exist_ok=True)
    os.makedirs(ASSETS + '/blockstates', exist_ok=True)
    os.makedirs(ASSETS + '/models/block', exist_ok=True)
    os.makedirs(ASSETS + '/models/item', exist_ok=True)
    os.makedirs(DATA + '/krolasyonbosses/loot_tables/blocks', exist_ok=True)
    os.makedirs(DATA + '/minecraft/tags/blocks/mineable', exist_ok=True)
    os.makedirs(DATA + '/minecraft/tags/blocks', exist_ok=True)
    en = json.load(open(f'{ASSETS}/lang/en_us.json'))
    tr = json.load(open(f'{ASSETS}/lang/tr_tr.json'))
    rows = []
    for bid, btr, ben, fn, h, r, light, kind in BLOCKS:
        fn().save(f'{ASSETS}/textures/block/{bid}.png')
        json.dump({'variants': {'': {'model': f'krolasyonbosses:block/{bid}'}}}, open(f'{ASSETS}/blockstates/{bid}.json', 'w'))
        json.dump({'parent': 'minecraft:block/cube_all', 'textures': {'all': f'krolasyonbosses:block/{bid}'}}, open(f'{ASSETS}/models/block/{bid}.json', 'w'))
        json.dump({'parent': f'krolasyonbosses:block/{bid}'}, open(f'{ASSETS}/models/item/{bid}.json', 'w'))
        drop = 'krolasyonbosses:raw_hellsteel' if kind == 'ore' else f'krolasyonbosses:{bid}'
        entry = {'type': 'minecraft:item', 'name': drop}
        if kind == 'ore':
            entry['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}, {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'}, {'function': 'minecraft:explosion_decay'}]
        loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [entry], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
        json.dump(loot, open(f'{DATA}/krolasyonbosses/loot_tables/blocks/{bid}.json', 'w'), indent=1)
        en[f'block.krolasyonbosses.{bid}'] = ben
        tr[f'block.krolasyonbosses.{bid}'] = btr
        rows.append((bid, h, r, light, kind))
    # portal block: blockstate with axis, animated texture
    pf = portal_frames()
    pf.save(f'{ASSETS}/textures/block/hell_portal.png')
    json.dump({'animation': {'frametime': 2, 'interpolate': True}}, open(f'{ASSETS}/textures/block/hell_portal.png.mcmeta', 'w'))
    json.dump({'variants': {'axis=x': {'model': 'krolasyonbosses:block/hell_portal_ns'}, 'axis=z': {'model': 'krolasyonbosses:block/hell_portal_ew'}}}, open(f'{ASSETS}/blockstates/hell_portal.json', 'w'))
    def pm(fr, to, faces):
        return {'ambientocclusion': False, 'textures': {'particle': 'krolasyonbosses:block/hell_portal', 'portal': 'krolasyonbosses:block/hell_portal'},
                'render_type': 'minecraft:translucent',
                'elements': [{'from': fr, 'to': to, 'shade': False, 'faces': {f: {'texture': '#portal', 'uv': [0, 0, 16, 16]} for f in faces}}]}
    json.dump(pm([0, 0, 6], [16, 16, 10], ['north', 'south']), open(f'{ASSETS}/models/block/hell_portal_ns.json', 'w'))
    json.dump(pm([6, 0, 0], [10, 16, 16], ['east', 'west']), open(f'{ASSETS}/models/block/hell_portal_ew.json', 'w'))
    en['block.krolasyonbosses.hell_portal'] = 'Hellgate Portal'
    tr['block.krolasyonbosses.hell_portal'] = 'Cehennem Geçidi'
    # tags
    names = [f'krolasyonbosses:{r[0]}' for r in rows]
    json.dump({'replace': False, 'values': names}, open(f'{DATA}/minecraft/tags/blocks/mineable/pickaxe.json', 'w'))
    json.dump({'replace': False, 'values': ['krolasyonbosses:hellsteel_ore', 'krolasyonbosses:hellgate_stone']}, open(f'{DATA}/minecraft/tags/blocks/needs_iron_tool.json', 'w'))
    json.dump({'replace': False, 'values': ['krolasyonbosses:rot_mud']}, open(f'{DATA}/minecraft/tags/blocks/mushroom_grow_block.json', 'w'))
    json.dump(en, open(f'{ASSETS}/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
    json.dump(tr, open(f'{ASSETS}/lang/tr_tr.json', 'w'), indent=1, ensure_ascii=False)
    with open(f'{JAVA}/BlockTable.java', 'w') as f:
        f.write('// GENERATED by gen/blocks.py - do not edit by hand\npackage com.krolasyon.bosses.registry;\n\nfinal class BlockTable {\n    private BlockTable() {}\n\n'
                '    /** id, hardness, resistance, light, kind */\n    static final Object[][] ROWS = {\n' + ''.join(f'        {{"{b}", {h}F, {r}F, {l}, "{k}"}},\n' for b, h, r, l, k in rows) + '    };\n}\n')
    print('blocks', len(rows))


if __name__ == '__main__':
    main()
