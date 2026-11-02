"""Textures, block models, blockstates, item models, loot tables and lang for the haunted-house blocks."""
import os, json, math, numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), '../common/src/main/resources')
A = os.path.join(ROOT, 'assets/krolasyonhorror')
TEX = os.path.join(A, 'textures/block')
for d in (TEX, A + '/models/block', A + '/models/item', A + '/blockstates',
          ROOT + '/data/krolasyonhorror/loot_tables/blocks', ROOT + '/data/krolasyonhorror/loot_table/blocks'):
    os.makedirs(d, exist_ok=True)
R = np.random.default_rng(99)


def noise(h, w, scale, oct=3, seed=None):
    r = np.random.default_rng(seed) if seed is not None else R
    out = np.zeros((h, w)); amp = 1; tot = 0
    for _ in range(oct):
        gh, gw = max(2, int(h / scale)), max(2, int(w / scale))
        g = r.random((gh, gw))
        g = np.pad(g, ((0, 1), (0, 1)), mode='wrap')          # tileable
        ys = np.linspace(0, gh, h, endpoint=False); xs = np.linspace(0, gw, w, endpoint=False)
        y0, x0 = ys.astype(int), xs.astype(int)
        fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy)
        tot += amp; amp *= 0.5; scale = max(1, scale / 2)
    return out / tot


def mix(a, b, t):
    t = np.clip(np.asarray(t, np.float32), 0, 1)[..., None]
    return np.asarray(a, np.float32) * (1 - t) + np.asarray(b, np.float32) * t


def save(name, rgb, alpha=None):
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)
    if alpha is None:
        Image.fromarray(rgb, 'RGB').save(f'{TEX}/{name}.png')
    else:
        Image.fromarray(np.dstack([rgb, (np.clip(alpha, 0, 1) * 255).astype(np.uint8)]), 'RGBA').save(f'{TEX}/{name}.png')


def plaster(base, dark, size=32, grime=0.35):
    n = noise(size, size, 6); n2 = noise(size, size, 2, 2)
    col = mix(dark, base, 0.75 + (n - 0.5) * 0.6 + (n2 - 0.5) * 0.25)
    yy = np.arange(size)[:, None] / size
    return mix(col, dark, grime * (noise(size, size, 10) > 0.62) * 0.6)


def floral(size=32, base=(232, 218, 188), ink=(150, 96, 58), stain=0.0):
    img = Image.new('RGB', (size * 3, size * 3), base)
    d = ImageDraw.Draw(img)
    r = np.random.default_rng(4)
    # half-drop repeat of small flower clusters
    motifs = [(8, 6), (24, 22), (24, 6), (8, 22)]
    for (mx, my) in motifs:
        for ox in range(3):
            for oy in range(3):
                cx, cy = mx + ox * size, my + oy * size
                for k in range(6):
                    a = k / 6 * math.tau + (mx * 0.1)
                    px, py = cx + math.cos(a) * 3.2, cy + math.sin(a) * 3.2
                    d.ellipse((px - 1.6, py - 1.6, px + 1.6, py + 1.6), fill=tuple(int(c * (0.85 + 0.3 * r.random())) for c in ink))
                d.ellipse((cx - 1.2, cy - 1.2, cx + 1.2, cy + 1.2), fill=(110, 70, 40))
                for k in range(3):
                    a = r.random() * math.tau
                    d.line((cx + math.cos(a) * 4, cy + math.sin(a) * 4, cx + math.cos(a) * 7, cy + math.sin(a) * 7), fill=(170, 140, 100))
    img = img.crop((size, size, 2 * size, 2 * size)).filter(ImageFilter.GaussianBlur(0.35))
    col = np.asarray(img).astype(np.float32)
    col = mix(col, (200, 182, 150), noise(size, size, 5) * 0.35)
    col[:, ::8] = mix(col[:, ::8], (205, 188, 156), 0.25)       # faint stripes
    if stain:
        st = noise(size, size, 8, 3, seed=7)
        col = mix(col, (150, 118, 72), np.clip((st - 0.55) * 4, 0, 1) * stain)
        col = col * (0.82 + 0.18 * noise(size, size, 4))[..., None]
    return col


# ------------------------------------------------------------------ walls / floors
save('wallpaper_floral', floral())
save('wallpaper_floral_stained', floral(base=(214, 196, 160), ink=(128, 84, 52), stain=0.9))
save('wall_cream', plaster((228, 216, 190), (186, 172, 142)))
save('wall_olive', plaster((132, 122, 80), (96, 88, 56)))
save('ceiling_plaster', plaster((236, 232, 222), (196, 190, 176), grime=0.2))


def fuzz(base, dark, size=32, dirt=0.0, seed=1):
    r = np.random.default_rng(seed)
    n = noise(size, size, 4, 3, seed=seed)
    col = mix(dark, base, 0.6 + (n - 0.5) * 0.7 + (r.random((size, size)) - 0.5) * 0.25)
    if dirt:
        sp = r.random((size, size)) < 0.06 * dirt
        col[sp] = mix(col[sp], (40, 26, 18), 0.7)
        bl = noise(size, size, 7, 2, seed=seed + 5) > 0.68
        col[bl] = mix(col[bl], (58, 36, 24), 0.5)
    return col


save('carpet_brown_dirty', fuzz((112, 64, 46), (70, 40, 30), dirt=1.0, seed=3))
save('carpet_beige', fuzz((206, 180, 136), (176, 150, 110), dirt=0.12, seed=4))

# persian rug field + border
S = 32
yy, xx = np.mgrid[0:S, 0:S]
field = np.zeros((S, S, 3)) + (150, 74, 70)
dist = np.abs(xx - 15.5) + np.abs(yy - 15.5)
field[(dist > 9) & (dist < 12)] = (226, 206, 170)
field[(dist <= 9) & (dist > 6)] = (60, 40, 46)
field[dist <= 6] = (200, 120, 96)
field[dist <= 2.5] = (230, 214, 178)
field[((xx + yy) % 8 == 0) & (dist > 12)] = (190, 150, 110)
field = mix(field, (90, 60, 52), noise(S, S, 5) * 0.35)
save('rug_persian', field)
edge = np.zeros((S, S, 3)) + (130, 60, 58)
edge[0:3] = (60, 40, 44); edge[3:5] = (226, 206, 170); edge[20:22] = (226, 206, 170)
band = (yy >= 6) & (yy < 19)
edge[band & (((xx + yy) % 6) < 3)] = (210, 170, 120)
edge[band & ((np.abs((xx % 8) - 4) + np.abs(yy - 12.5)) < 3)] = (60, 40, 46)
edge = mix(edge, (90, 60, 52), noise(S, S, 5) * 0.35)
save('rug_persian_edge', edge)

runner = fuzz((128, 136, 146), (90, 96, 106), seed=8)
runner[:, 2:4] = (64, 68, 80); runner[:, 28:30] = (64, 68, 80)
runner[:, 5] = (176, 172, 160); runner[:, 26] = (176, 172, 160)
runner[:, :2] = (70, 58, 50); runner[:, 30:] = (70, 58, 50)
save('runner_rug', runner)

# ------------------------------------------------------------------ furniture textures (16px)


def wood(base=(70, 44, 28), dark=(42, 26, 16), size=16, seed=2):
    n = noise(size, size, 3, 2, seed=seed)
    grain = 0.5 + 0.5 * np.sin(np.arange(size)[None, :] * 1.9 + n * 6)
    return mix(dark, base, grain * 0.7 + n * 0.3)


save('dark_wood', wood())
save('handrail_wood', wood((168, 96, 48), (120, 64, 30), seed=5))
paint = mix((238, 236, 228), (200, 196, 184), noise(16, 16, 4))
save('white_paint', paint)
tt = mix((230, 226, 214), (180, 170, 150), noise(16, 16, 3))
r = np.random.default_rng(3)
for _ in range(7):
    y, x = r.integers(0, 16, 2); tt[y, x] = (110, 86, 60)
save('table_top', tt)
fab = mix((214, 208, 190), (168, 160, 140), noise(16, 16, 3, seed=11))
for y in range(0, 16, 4):
    for x in range((y // 4) % 2 * 2, 16, 4):
        fab[y, x] = (150, 128, 112); fab[(y + 1) % 16, x] = (120, 140, 110)
tears = noise(16, 16, 4, seed=12) > 0.72
fab[tears] = mix(fab[tears], (120, 96, 70), 0.6)
save('armchair_fabric', fab)
save('crt_body', mix((60, 58, 54), (34, 32, 30), noise(16, 16, 4, seed=13)))
# animated static screen: 16x(16*12) strip
frames = []
for i in range(12):
    f = np.random.default_rng(100 + i).random((16, 16))
    f = 0.25 + 0.75 * f
    f = f * (0.8 + 0.2 * np.random.default_rng(200 + i).random((16, 1)))
    yy, xx = np.mgrid[0:16, 0:16]
    vig = 1 - 0.35 * np.clip(np.hypot(xx - 7.5, yy - 7.5) / 9, 0, 1) ** 2
    frames.append(np.stack([f * 220 * vig, f * 226 * vig, f * 234 * vig], -1))
save('crt_screen', np.concatenate(frames, 0))
with open(f'{TEX}/crt_screen.png.mcmeta', 'w') as fh:
    json.dump({'animation': {'frametime': 1, 'interpolate': False}}, fh)
save('brass', mix((180, 140, 70), (110, 80, 36), noise(16, 16, 2, seed=14)))
save('candle_wax', mix((240, 232, 210), (210, 200, 176), noise(16, 16, 2, seed=15)))
fl = np.zeros((16, 16, 3)); fl[:] = (255, 210, 120); fl[4:12, 5:11] = (255, 244, 200)
save('flame', fl)
dome = mix((248, 236, 206), (220, 200, 160), noise(16, 16, 3, seed=16)); save('glass_dome', dome)
save('bulb', np.zeros((16, 16, 3)) + (255, 240, 200))
save('string', np.zeros((16, 16, 3)) + (40, 40, 44))


def balloon(base, hi):
    yy, xx = np.mgrid[0:16, 0:16]
    col = mix(base, (np.array(base) * 0.6), np.hypot(xx - 6, yy - 5) / 14)
    col[3:6, 4:7] = mix(col[3:6, 4:7], hi, 0.7)
    return col


save('balloon_white', balloon((226, 224, 220), (255, 255, 255)))
save('balloon_gold', balloon((196, 176, 100), (250, 240, 170)))


def picture(name, art_fn, frame=(176, 140, 70), inner=(236, 228, 206)):
    S = 32
    img = Image.new('RGB', (S, S), frame)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, S - 1, S - 1), outline=(120, 90, 40))
    d.rectangle((3, 3, S - 4, S - 4), fill=inner, outline=(140, 108, 50))
    art_fn(d)
    a = np.asarray(img).astype(np.float32)
    a = mix(a, (90, 80, 60), noise(S, S, 6, seed=hash(name) % 1000) * 0.25)
    save(name, a)


picture('picture_sketch', lambda d: (d.ellipse((9, 9, 16, 15), outline=(150, 120, 110)), d.line((12, 16, 22, 24), fill=(120, 110, 120)),
                                     d.rectangle((17, 8, 22, 12), outline=(170, 150, 120))))
picture('picture_portrait', lambda d: (d.rectangle((6, 6, 25, 25), fill=(46, 40, 36)), d.ellipse((12, 9, 19, 17), fill=(150, 130, 110)),
                                       d.rectangle((10, 17, 21, 25), fill=(30, 26, 24)), d.rectangle((13, 12, 18, 13), fill=(20, 16, 14))),
        frame=(130, 100, 50))
picture('picture_landscape', lambda d: (d.rectangle((6, 6, 25, 15), fill=(150, 160, 160)), d.rectangle((6, 16, 25, 25), fill=(90, 100, 70)),
                                        d.polygon([(6, 18), (14, 11), (22, 19)], fill=(80, 84, 76)), d.line((17, 20, 17, 25), fill=(30, 26, 24))),
        frame=(70, 50, 34), inner=(200, 196, 180))

# ------------------------------------------------------------------ models
M = {}


def el(fr, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down'), uv=None):
    return {'from': fr, 'to': to, 'faces': {f: {'texture': '#' + tex, **({'uv': uv} if uv else {})} for f in faces}}


def model(name, textures, elements, particle=None, render='minecraft:cutout'):
    tx = {k: f'krolasyonhorror:block/{v}' for k, v in textures.items()}
    tx['particle'] = f'krolasyonhorror:block/{particle or list(textures.values())[0]}'
    M[name] = {'parent': 'minecraft:block/block', 'render_type': render, 'textures': tx, 'elements': elements}


def cube(name, tex):
    M[name] = {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'krolasyonhorror:block/{tex}'}}


def carpet(name, tex):
    M[name] = {'parent': 'minecraft:block/carpet', 'textures': {'wool': f'krolasyonhorror:block/{tex}'}}


for n in ('wallpaper_floral', 'wallpaper_floral_stained', 'wall_cream', 'wall_olive', 'ceiling_plaster'):
    cube(n, n)
for n in ('carpet_brown_dirty', 'carpet_beige', 'rug_persian', 'rug_persian_edge', 'runner_rug'):
    carpet(n, n)

model('crt_tv', {'body': 'crt_body', 'screen': 'crt_screen', 'wood': 'dark_wood'}, [
    el([1, 0, 1], [2, 6, 2], 'wood'), el([14, 0, 1], [15, 6, 2], 'wood'), el([1, 0, 14], [2, 6, 15], 'wood'), el([14, 0, 14], [15, 6, 15], 'wood'),
    el([0, 6, 0], [16, 7, 16], 'wood'),
    el([1, 7, 3], [15, 19, 14], 'body'),
    el([3, 9, 2.5], [13, 17, 3], 'screen', faces=('north',), uv=[0, 0, 16, 16]),
    el([4, 9, 14], [12, 16, 17], 'body'),
    el([13.2, 8, 2.6], [14.2, 9, 3], 'wood', faces=('north',)),
], particle='crt_body')
model('armchair', {'fabric': 'armchair_fabric', 'wood': 'dark_wood'}, [
    el([2, 0, 2], [3.5, 3, 3.5], 'wood'), el([12.5, 0, 2], [14, 3, 3.5], 'wood'), el([2, 0, 12.5], [3.5, 3, 14], 'wood'), el([12.5, 0, 12.5], [14, 3, 14], 'wood'),
    el([1, 3, 1], [15, 8, 15], 'fabric'),
    el([1, 8, 12], [15, 20, 15.5], 'fabric'),
    el([0, 3, 1], [2.5, 12, 14], 'fabric'), el([13.5, 3, 1], [16, 12, 14], 'fabric'),
    el([2.5, 8, 1.5], [13.5, 9.5, 12], 'fabric'),
])
model('coffee_table', {'top': 'table_top', 'wood': 'dark_wood'}, [
    el([1, 8, 1], [15, 10, 15], 'top'),
    el([2, 0, 2], [3.5, 8, 3.5], 'wood'), el([12.5, 0, 2], [14, 8, 3.5], 'wood'), el([2, 0, 12.5], [3.5, 8, 14], 'wood'), el([12.5, 0, 12.5], [14, 8, 14], 'wood'),
    el([2, 2, 2.5], [14, 3, 3], 'wood'), el([2, 2, 13], [14, 3, 13.5], 'wood'),
], particle='table_top')
for pic, (w0, h0, w1, h1) in (('picture_sketch', (2, 2, 14, 14)), ('picture_portrait', (3, 1, 13, 15)), ('picture_landscape', (1, 4, 15, 13))):
    model(pic, {'art': pic, 'frame': 'dark_wood'}, [
        {'from': [w0, h0, 15], 'to': [w1, h1, 16], 'faces': {'north': {'texture': '#art', 'uv': [0, 0, 16, 16]},
                                                              'east': {'texture': '#frame'}, 'west': {'texture': '#frame'}, 'up': {'texture': '#frame'}, 'down': {'texture': '#frame'}}}])
model('candle_sconce', {'brass': 'brass', 'wax': 'candle_wax', 'flame': 'flame'}, [
    el([6.5, 3, 15], [9.5, 13, 16], 'brass'),
    el([7.5, 6, 11], [8.5, 7, 15], 'brass'),
    el([6, 7, 9], [10, 8, 13], 'brass'),
    el([7, 8, 10], [9, 12, 12], 'wax'),
    el([7.6, 12, 10.6], [8.4, 13.6, 11.4], 'flame'),
], particle='brass')
model('ceiling_lamp', {'brass': 'brass', 'glass': 'glass_dome'}, [
    el([5, 15, 5], [11, 16, 11], 'brass'),
    el([5.5, 12, 5.5], [10.5, 15, 10.5], 'glass'),
    el([4.5, 12.5, 6.5], [11.5, 14.5, 9.5], 'glass'),
    el([6.5, 12.5, 4.5], [9.5, 14.5, 11.5], 'glass'),
    el([7.5, 11.2, 7.5], [8.5, 12, 8.5], 'brass'),
], particle='glass_dome')
model('bulb_fixture', {'brass': 'brass', 'bulb': 'bulb'}, [
    el([6, 15, 6], [10, 16, 10], 'brass'),
    el([7, 13, 7], [9, 15, 9], 'brass'),
    el([6.5, 10, 6.5], [9.5, 13, 9.5], 'bulb'),
], particle='bulb')
for b in ('balloon_white', 'balloon_gold'):
    model(b, {'b': b, 's': 'string'}, [
        el([4, 6, 4], [12, 15, 12], 'b'), el([5, 5, 5], [11, 16, 11], 'b'),
        el([3, 7.5, 5], [13, 13.5, 11], 'b'), el([5, 7.5, 3], [11, 13.5, 13], 'b'),
        el([7.3, 4, 7.3], [8.7, 5, 8.7], 'b'),
        el([7.9, -14, 7.9], [8.1, 4, 8.1], 's'),
    ])
balus = []
for x in (1, 5, 9, 13):
    balus += [el([x + 0.5, 1.5, 7.5], [x + 1.5, 13, 8.5], 'white'), el([x, 4, 7], [x + 2, 6, 9], 'white'), el([x, 9, 7], [x + 2, 10, 9], 'white')]
model('banister', {'white': 'white_paint', 'rail': 'handrail_wood'}, [
    el([0, 0, 6.5], [16, 1.5, 9.5], 'white'), *balus, el([0, 13, 6], [16, 15, 10], 'rail'),
], particle='white_paint')
model('banister_post', {'white': 'white_paint', 'rail': 'handrail_wood'}, [
    el([5, 0, 5], [11, 15, 11], 'white'), el([4, 15, 4], [12, 17, 12], 'rail'),
], particle='white_paint')

for n, m in M.items():
    with open(f'{A}/models/block/{n}.json', 'w') as fh:
        json.dump(m, fh, indent=1)
    with open(f'{A}/models/item/{n}.json', 'w') as fh:
        json.dump({'parent': f'krolasyonhorror:block/{n}'}, fh)

FACING = {'crt_tv', 'armchair', 'picture_sketch', 'picture_portrait', 'picture_landscape', 'candle_sconce', 'banister', 'rug_persian_edge', 'runner_rug'}
for n in M:
    if n in FACING:
        bs = {'variants': {f'facing={f}': {'model': f'krolasyonhorror:block/{n}', **({'y': y} if y else {})}
                           for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}}
    else:
        bs = {'variants': {'': {'model': f'krolasyonhorror:block/{n}'}}}
    with open(f'{A}/blockstates/{n}.json', 'w') as fh:
        json.dump(bs, fh, indent=1)
    loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'krolasyonhorror:{n}'}],
                                                  'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
    for folder in ('loot_tables', 'loot_table'):
        with open(f'{ROOT}/data/krolasyonhorror/{folder}/blocks/{n}.json', 'w') as fh:
            json.dump(loot, fh)

NAMES = {
    'wallpaper_floral': ('Floral Wallpaper', 'Çiçekli Duvar Kâğıdı'), 'wallpaper_floral_stained': ('Stained Floral Wallpaper', 'Lekeli Çiçekli Duvar Kâğıdı'),
    'wall_cream': ('Cream Plaster', 'Krem Sıva'), 'wall_olive': ('Olive Plaster', 'Zeytin Yeşili Sıva'), 'ceiling_plaster': ('Ceiling Plaster', 'Tavan Sıvası'),
    'carpet_brown_dirty': ('Dirty Brown Carpet', 'Kirli Kahverengi Halı'), 'carpet_beige': ('Beige Carpet', 'Bej Halı'),
    'rug_persian': ('Persian Rug', 'İran Halısı'), 'rug_persian_edge': ('Persian Rug Border', 'İran Halısı Kenarı'), 'runner_rug': ('Runner Rug', 'Yolluk'),
    'crt_tv': ('Old CRT Television', 'Eski Tüplü Televizyon'), 'armchair': ('Worn Armchair', 'Yıpranmış Koltuk'), 'coffee_table': ('Coffee Table', 'Sehpa'),
    'picture_sketch': ('Faded Sketch', 'Solmuş Karakalem'), 'picture_portrait': ('Old Portrait', 'Eski Portre'), 'picture_landscape': ('Dark Landscape', 'Karanlık Manzara'),
    'candle_sconce': ('Candle Sconce', 'Mumlu Aplik'), 'ceiling_lamp': ('Dome Ceiling Lamp', 'Kubbe Tavan Lambası'), 'bulb_fixture': ('Bare Bulb', 'Çıplak Ampul'),
    'balloon_white': ('White Balloon', 'Beyaz Balon'), 'balloon_gold': ('Gold Balloon', 'Altın Balon'), 'banister': ('Banister', 'Merdiven Korkuluğu'),
    'banister_post': ('Banister Post', 'Korkuluk Babası'),
}
for lang, i in (('en_us', 0), ('tr_tr', 1)):
    p = f'{A}/lang/{lang}.json'
    d = json.load(open(p))
    for n, v in NAMES.items():
        d[f'block.krolasyonhorror.{n}'] = v[i]
    d['itemGroup.krolasyonhorror.house'] = ('Hatman: Haunted House', 'Şapkalı Adam: Lanetli Ev')[i]
    d['commands.krolasyonhorror.house'] = ('The house is waiting.', 'Ev seni bekliyor.')[i]
    json.dump(d, open(p, 'w'), ensure_ascii=False, indent=2)
print(len(M), 'blocks')
