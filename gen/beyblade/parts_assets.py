"""Assets of the beyblade parts system, read from the part catalogue in BeyPart.java:

  textures/item/part_<id>.png + models/item/part_<id>.json   inventory icons of every part
  textures/entity/part_plain.png                              texture of the drawn tracks / tips (tinted per part)
  the Beyblade Workbench block (textures, model, blockstate, loot)
  lang entries (en_us, tr_tr) and the crafting recipes of the craftable parts
"""
import collections, json, math, os, re
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
RES = os.path.join(ROOT, 'src/main/resources')
A = os.path.join(RES, 'assets/librabeyblade')
D = os.path.join(RES, 'data/librabeyblade')
JAVA = open(os.path.join(ROOT, 'src/main/java/com/krolasyon/beyblade/parts/BeyPart.java')).read()


def col(h):
    v = int(h, 16)
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


# ---------------- parse the catalogue ----------------
PARTS = []
for m in re.finditer(r'^\s+bolt\("(\w+)", (-?\d+), (-?\d+), (-?\d+), (-?\d+), (\d), 0x(\w+), 0x(\w+)\);', JAVA, re.M):
    PARTS.append(dict(id=m[1], type='bolt', rarity=int(m[6]), c=col(m[7]), c2=col(m[8])))
for m in re.finditer(r'^\s+ring\("(\w+)", (-?\d+), (-?\d+), (-?\d+), (-?\d+), ([\d.]+)F, (\d), 0x(\w+), 0x(\w+)\);', JAVA, re.M):
    PARTS.append(dict(id=m[1], type='ring', rarity=int(m[7]), c=col(m[8]), c2=col(m[9])))
for m in re.finditer(r'^\s+wheel\("(\w+)", "(\w+)", (-?\d+), (-?\d+), (-?\d+), (-?\d+), ([\d.]+)F, (\d), 0x(\w+), 0x(\w+)\);', JAVA, re.M):
    PARTS.append(dict(id=m[1], type='wheel', mesh=m[2], rarity=int(m[8]), c=col(m[9]), c2=col(m[10])))
COMMON_IDS = re.search(r'String\[\] commonIds = \{(.*?)\};', JAVA, re.S)[1]
COMMON_IDS = re.findall(r'"(\w+)"', COMMON_IDS)
COMMON_COL = re.findall(r'0x(\w+)', re.search(r'int\[\] commonCol = \{(.*?)\};', JAVA, re.S)[1])
for i, cid in enumerate(COMMON_IDS):
    PARTS.append(dict(id='wheel_' + cid, type='wheel', mesh='common_%d' % i, rarity=0, c=(176, 180, 188), c2=col(COMMON_COL[i])))
for m in re.finditer(r'^\s+track\("(\w+)", (\d+), Trait\.(\w+), (-?\d+), (-?\d+), (-?\d+), (-?\d+), ([\d.]+)F, (\d), 0x(\w+), 0x(\w+)\);', JAVA, re.M):
    PARTS.append(dict(id=m[1], type='track', h=int(m[2]), trait=m[3], rarity=int(m[9]), c=col(m[10]), c2=col(m[11])))
for m in re.finditer(r'^\s+tip\("(\w+)", Tip\.(\w+), (-?\d+), (-?\d+), (-?\d+), (-?\d+), ([\d.]+)F, (\d), 0x(\w+), 0x(\w+)\);', JAVA, re.M):
    PARTS.append(dict(id=m[1], type='tip', tip=m[2], rarity=int(m[8]), c=col(m[9]), c2=col(m[10])))
print(len(PARTS), 'parts', collections.Counter(p['type'] for p in PARTS))

# ---------------- names ----------------
OWNER_EN = {'libra': 'Libra', 'ldrago': 'L-Drago', 'leone': 'Leone', 'eagle': 'Eagle', 'pegasus': 'Pegasus', 'tempo': 'Tempo',
            'variares': 'Variares', 'beffal': 'Befall', 'bigbang': 'Big Bang', 'phoenix': 'Phoenix', 'inferno': 'Inferno',
            'standard': 'Standard'}
OWNER_TR = dict(OWNER_EN, standard='Standart')
WHEEL_EN = {'libra': 'Flame', 'ldrago': 'Destruction', 'leone': 'Rock', 'eagle': 'Earth', 'pegasus': 'Storm', 'tempo': 'Twisted',
            'variares': 'Variares', 'beffal': 'Evil', 'bigbang': 'Big Bang', 'phoenix': 'Phoenix', 'inferno': 'Inferno'}
COMMON_EN = {'iron_bull': 'Iron Bull', 'frost_wolf': 'Frost Wolf', 'jade_turtle': 'Jade Turtle', 'thunder_viper': 'Thunder Viper',
             'shadow_bat': 'Shadow Bat', 'sky_falcon': 'Sky Falcon', 'crimson_scorpion': 'Crimson Scorpion',
             'golden_phoenix': 'Golden Phoenix', 'ocean_shark': 'Ocean Shark', 'storm_fox': 'Storm Fox'}
COMMON_TR = {'iron_bull': 'Demir Boğa', 'frost_wolf': 'Buz Kurdu', 'jade_turtle': 'Yeşim Kaplumbağa', 'thunder_viper': 'Gök Gürültüsü Engereği',
             'shadow_bat': 'Gölge Yarasa', 'sky_falcon': 'Gök Şahini', 'crimson_scorpion': 'Kızıl Akrep',
             'golden_phoenix': 'Altın Anka', 'ocean_shark': 'Okyanus Köpekbalığı', 'storm_fox': 'Fırtına Tilkisi'}
TRACK_EN = {'BOOST_DISK': 'Boost Disk', 'CLAW': 'Claw', 'WING': 'Wing', 'UPPER_WING': 'Upper Wing', 'TORNADO': 'Tornado',
            'DEFENSE_RING': 'Defense', 'RUBBER_RING': 'Rubber', 'TWIN_HEIGHT': 'Twin Height', 'GRAVITY': 'Gravity'}
TRACK_TR = {'BOOST_DISK': 'Takviye Diski', 'CLAW': 'Pençe', 'WING': 'Kanat', 'UPPER_WING': 'Üst Kanat', 'TORNADO': 'Kasırga',
            'DEFENSE_RING': 'Savunma', 'RUBBER_RING': 'Lastik', 'TWIN_HEIGHT': 'Çift Yükseklik', 'GRAVITY': 'Yerçekimi'}
TIP_EN = {'tip_f': 'F (Flat)', 'tip_rf': 'RF (Rubber Flat)', 'tip_r2f': 'R2F (Right Rubber Flat)', 'tip_xf': 'XF (Extreme Flat)',
          'tip_sf': 'SF (Semi Flat)', 'tip_s': 'S (Sharp)', 'tip_es': 'ES (Eternal Sharp)', 'tip_cs': 'CS (Coat Sharp)',
          'tip_d': 'D (Defense)', 'tip_wd': 'WD (Wide Defense)', 'tip_ewd': 'EWD (Eternal Wide Defense)', 'tip_b': 'B (Ball)',
          'tip_wb': 'WB (Wide Ball)', 'tip_mf': 'MF (Metal Flat)', 'tip_rsf': 'RSF (Rubber Semi Flat)', 'tip_fd': 'F:D (Final Drive)',
          'tip_fs': 'F:S (Final Survive)'}
TIP_TR = {'tip_f': 'F (Düz)', 'tip_rf': 'RF (Lastik Düz)', 'tip_r2f': 'R2F (Sağ Lastik Düz)', 'tip_xf': 'XF (Aşırı Düz)',
          'tip_sf': 'SF (Yarı Düz)', 'tip_s': 'S (Sivri)', 'tip_es': 'ES (Sonsuz Sivri)', 'tip_cs': 'CS (Kaplamalı Sivri)',
          'tip_d': 'D (Savunma)', 'tip_wd': 'WD (Geniş Savunma)', 'tip_ewd': 'EWD (Sonsuz Geniş Savunma)', 'tip_b': 'B (Top)',
          'tip_wb': 'WB (Geniş Top)', 'tip_mf': 'MF (Metal Düz)', 'tip_rsf': 'RSF (Lastik Yarı Düz)', 'tip_fd': 'F:D (Son Sürüş)',
          'tip_fs': 'F:S (Son Direniş)'}


def names(p):
    t, i = p['type'], p['id']
    key = i.split('_', 1)[1]
    if t == 'bolt':
        return OWNER_EN[key] + ' Face Bolt', OWNER_TR[key] + ' Yüz Cıvatası'
    if t == 'ring':
        return OWNER_EN[key] + ' Energy Ring', OWNER_TR[key] + ' Enerji Halkası'
    if t == 'wheel':
        if key in COMMON_EN:
            return COMMON_EN[key] + ' Fusion Wheel', COMMON_TR[key] + ' Füzyon Çarkı'
        return WHEEL_EN[key] + ' Fusion Wheel', WHEEL_EN[key] + ' Füzyon Çarkı'
    if t == 'track':
        code = key.upper()
        tr = p['trait']
        if tr in TRACK_EN:
            return '%s (%s) Spin Track' % (code, TRACK_EN[tr]), '%s (%s) Dönüş Yolu' % (code, TRACK_TR[tr])
        return code + ' Spin Track', code + ' Dönüş Yolu'
    return TIP_EN[i] + ' Performance Tip', TIP_TR[i] + ' Performans Ucu'


# ---------------- icons ----------------
ITEM = os.path.join(A, 'textures/item')
MODEL = os.path.join(A, 'models/item')


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def icon_bolt(p):
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    hexp = [(32 + 26 * math.cos(math.radians(60 * k)), 30 + 26 * math.sin(math.radians(60 * k))) for k in range(6)]
    d.polygon([(x, y + 4) for x, y in hexp], fill=shade(p['c'], 0.45) + (255,))
    d.polygon(hexp, fill=p['c'] + (255,), outline=shade(p['c'], 0.5) + (255,))
    inner = [(32 + 17 * math.cos(math.radians(60 * k)), 30 + 17 * math.sin(math.radians(60 * k))) for k in range(6)]
    d.polygon(inner, fill=shade(p['c'], 0.75) + (255,))
    # a little emblem: flame / wing strokes in the accent colour
    for k in range(5):
        a = math.radians(-150 + 30 * k)
        d.line([(32, 36), (32 + 12 * math.cos(a), 34 + 12 * math.sin(a))], fill=p['c2'] + (255,), width=3)
    d.ellipse([28, 32, 36, 40], fill=p['c2'] + (255,))
    return im


def icon_ring(p):
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    a = np.zeros((64, 64, 4), np.float32)
    yy, xx = np.mgrid[0:64, 0:64]
    r = np.hypot(xx - 32, (yy - 32) * 1.15)
    ring = (r < 29) & (r > 15)
    ang = np.arctan2(yy - 32, xx - 32)
    wave = 0.75 + 0.25 * np.sin(ang * 6 + r * 0.3)
    c, c2 = np.array(p['c'], np.float32), np.array(p['c2'], np.float32)
    mix = np.clip((r - 15) / 14, 0, 1)[..., None]
    rgb = c * (1 - mix * 0.5) + c2 * mix * 0.5
    a[..., :3] = rgb * wave[..., None]
    a[..., 3] = np.where(ring, 225, 0)
    hl = ring & (np.abs(ang + 2.2) < 0.35)
    a[hl, :3] = np.minimum(255, a[hl, :3] + 90)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), 'RGBA')


def icon_wheel(p):
    face = Image.open(os.path.join(A, 'textures/entity/%s_face.png' % p['mesh'])).convert('RGBA').resize((64, 64), Image.LANCZOS)
    f = np.asarray(face).copy()
    f[..., 3] = np.where(f[..., 3] > 100, 255, 0)
    return Image.fromarray(f, 'RGBA')


def icon_track(p):
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    h = int(14 + 30 * min(1.0, p['h'] / 230))
    top, bot = 32 - h // 2, 32 + h // 2
    c, c2 = p['c'], p['c2']
    d.ellipse([12, top - 5, 52, top + 5], fill=shade(c, 1.25) + (255,))
    d.rectangle([22, top, 42, bot], fill=c + (255,))
    d.rectangle([22, top, 26, bot], fill=shade(c, 1.4) + (255,))
    d.ellipse([18, bot - 4, 46, bot + 4], fill=shade(c, 0.8) + (255,))
    tr = p['trait']
    if tr != 'NONE':
        y = {'BOOST_DISK': 0.65, 'DEFENSE_RING': 0.75, 'UPPER_WING': 0.2}.get(tr, 0.45)
        yc = int(top + (bot - top) * y)
        w = {'BOOST_DISK': 28, 'WING': 26, 'UPPER_WING': 26, 'CLAW': 24}.get(tr, 20)
        d.ellipse([32 - w, yc - 4, 32 + w, yc + 4], fill=c2 + (255,), outline=shade(c2, 0.6) + (255,))
    # the height number in tiny pixels at the bottom
    return im


def icon_tip(p):
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c, c2 = p['c'], p['c2']
    t = p['tip']
    d.ellipse([16, 10, 48, 20], fill=shade(c, 1.2) + (255,))
    d.rectangle([18, 15, 46, 30], fill=c + (255,))
    contact = {'FLAT': 9, 'RUBBER_FLAT': 11, 'EXTREME_FLAT': 13, 'SEMI_FLAT': 5, 'METAL_FLAT': 9, 'RUBBER_SEMI_FLAT': 6,
               'FINAL_DRIVE': 8}.get(t, 0)
    wide = {'WIDE_DEFENSE': 20, 'ETERNAL_WIDE_DEFENSE': 22, 'WIDE_BALL': 19, 'DEFENSE': 16, 'FINAL_DRIVE': 18, 'FINAL_SURVIVE': 18}.get(t, 14)
    if t in ('BALL', 'WIDE_BALL'):
        d.ellipse([32 - wide, 22, 32 + wide, 54], fill=c2 + (255,))
    else:
        d.polygon([(32 - wide, 28), (32 + wide, 28), (32 + max(contact, 1), 54), (32 - max(contact, 1), 54)], fill=c2 + (255,))
        if contact:
            d.rectangle([32 - contact, 50, 32 + contact, 55], fill=shade(c2, 0.7) + (255,))
    return im


ICON = {'bolt': icon_bolt, 'ring': icon_ring, 'wheel': icon_wheel, 'track': icon_track, 'tip': icon_tip}
RARE_RIM = {0: None, 1: (90, 200, 255), 2: (255, 200, 60), 3: (220, 120, 255)}
for p in PARTS:
    im = ICON[p['type']](p).resize((32, 32), Image.LANCZOS)
    a = np.asarray(im).copy()
    a[..., 3] = np.where(a[..., 3] > 90, 255, 0)
    im = Image.fromarray(a, 'RGBA')
    rim = RARE_RIM[p['rarity']]
    if rim:
        # a soft rarity glow behind the part
        g = Image.fromarray((np.asarray(im)[..., 3] > 0).astype(np.uint8) * 255).filter(ImageFilter.MaxFilter(3))
        bg = Image.new('RGBA', (32, 32), rim + (0,))
        bg.putalpha(g.point(lambda v: 110 if v else 0))
        bg.alpha_composite(im)
        im = bg
    im.save(os.path.join(ITEM, 'part_%s.png' % p['id']))
    json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'librabeyblade:item/part_%s' % p['id']}},
              open(os.path.join(MODEL, 'part_%s.json' % p['id']), 'w'))

# texture of the drawn tracks / tips: near white with a fine brushed grain (tinted by vertex colour)
rng = np.random.default_rng(5)
pl = np.clip(235 + rng.normal(0, 9, (16, 16)) + np.sin(np.arange(16) / 16 * math.tau * 3)[None, :] * 6, 0, 255).astype(np.uint8)
Image.fromarray(np.dstack([pl, pl, pl, np.full((16, 16), 255, np.uint8)]), 'RGBA').save(os.path.join(A, 'textures/entity/part_plain.png'))

# ---------------- workbench block ----------------
BT = os.path.join(A, 'textures/block')
os.makedirs(BT, exist_ok=True)


def tex(fn, draw):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
    draw(ImageDraw.Draw(im), im)
    im.save(os.path.join(BT, fn))


def top(d, im):
    d.rectangle([0, 0, 15, 15], fill=(58, 44, 84))
    d.rectangle([0, 0, 15, 0], fill=(90, 74, 120)); d.rectangle([0, 15, 15, 15], fill=(36, 26, 52))
    d.ellipse([3, 3, 12, 12], fill=(150, 156, 168), outline=(210, 214, 222))
    d.ellipse([5, 5, 10, 10], fill=(220, 70, 150))
    d.rectangle([7, 7, 8, 8], fill=(255, 220, 120))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        im.putpixel((x, y), (200, 200, 210, 255))


def side(d, im):
    d.rectangle([0, 0, 15, 15], fill=(78, 82, 92))
    d.rectangle([0, 0, 15, 2], fill=(58, 44, 84)); d.rectangle([0, 13, 15, 15], fill=(52, 54, 62))
    for x in (2, 6, 10):
        d.rectangle([x, 5, x + 2, 10], fill=(120, 126, 138))
        d.rectangle([x, 5, x + 2, 5], fill=(170, 176, 188))
    d.rectangle([13, 4, 14, 11], fill=(220, 70, 150))


def bottom(d, im):
    d.rectangle([0, 0, 15, 15], fill=(52, 54, 62))
    for k in range(0, 16, 4): d.line([(k, 0), (k, 15)], fill=(62, 64, 72))


tex('bey_workbench_top.png', top)
tex('bey_workbench_side.png', side)
tex('bey_workbench_bottom.png', bottom)
os.makedirs(os.path.join(A, 'models/block'), exist_ok=True)
json.dump({'parent': 'minecraft:block/cube_bottom_top', 'textures': {'top': 'librabeyblade:block/bey_workbench_top',
          'side': 'librabeyblade:block/bey_workbench_side', 'bottom': 'librabeyblade:block/bey_workbench_bottom'}},
          open(os.path.join(A, 'models/block/bey_workbench.json'), 'w'))
json.dump({'variants': {'': {'model': 'librabeyblade:block/bey_workbench'}}}, open(os.path.join(A, 'blockstates/bey_workbench.json'), 'w'))
json.dump({'parent': 'librabeyblade:block/bey_workbench'}, open(os.path.join(MODEL, 'bey_workbench.json'), 'w'))
os.makedirs(os.path.join(D, 'loot_tables/blocks'), exist_ok=True)
json.dump({'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'librabeyblade:bey_workbench'}],
          'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}, open(os.path.join(D, 'loot_tables/blocks/bey_workbench.json'), 'w'), indent=1)

# ---------------- recipes ----------------
R = os.path.join(D, 'recipes')


def shaped(name, pattern, key, result, count=1):
    json.dump({'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {k: {'item': v} for k, v in key.items()},
               'result': {'item': result, 'count': count}}, open(os.path.join(R, name + '.json'), 'w'), indent=1)


def shapeless(name, items, result):
    json.dump({'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': i} for i in items],
               'result': {'item': result}}, open(os.path.join(R, name + '.json'), 'w'), indent=1)


shaped('bey_workbench', ['PRP', 'ICI', 'SSS'], {'P': 'minecraft:purple_dye', 'R': 'minecraft:redstone', 'I': 'minecraft:iron_ingot',
       'C': 'minecraft:crafting_table', 'S': 'minecraft:smooth_stone'}, 'librabeyblade:bey_workbench')
# tracks: an iron ingot and something that gives the height / mechanism
TRACK_RECIPES = {
    'track_85': ['minecraft:flint'], 'track_90': ['minecraft:clay_ball'], 'track_100': ['minecraft:iron_nugget'],
    'track_105': ['minecraft:feather'], 'track_125': ['minecraft:bone'], 'track_145': ['minecraft:stick'],
    'track_230': ['minecraft:stick', 'minecraft:stick', 'minecraft:iron_ingot'], 'track_bd145': ['minecraft:stick', 'minecraft:iron_block'],
    'track_c145': ['minecraft:stick', 'minecraft:shears'], 'track_w105': ['minecraft:feather', 'minecraft:phantom_membrane'],
    'track_uw145': ['minecraft:stick', 'minecraft:feather', 'minecraft:pink_dye'], 'track_t125': ['minecraft:bone', 'minecraft:string'],
    'track_d125': ['minecraft:bone', 'minecraft:shield'], 'track_r145': ['minecraft:stick', 'minecraft:slime_ball'],
    'track_th170': ['minecraft:stick', 'minecraft:stick', 'minecraft:gold_ingot'],
}
for tid, extra in TRACK_RECIPES.items():
    shapeless('part_' + tid, ['minecraft:iron_ingot'] + extra, 'librabeyblade:part_' + tid)
TIP_RECIPES = {
    'tip_f': ['minecraft:stone'], 'tip_rf': ['minecraft:slime_ball'], 'tip_r2f': ['minecraft:slime_ball', 'minecraft:cyan_dye'],
    'tip_xf': ['minecraft:slime_ball', 'minecraft:sugar'], 'tip_sf': ['minecraft:cobblestone'], 'tip_s': ['minecraft:flint'],
    'tip_es': ['minecraft:flint', 'minecraft:amethyst_shard'], 'tip_cs': ['minecraft:flint', 'minecraft:slime_ball'],
    'tip_d': ['minecraft:iron_nugget'], 'tip_wd': ['minecraft:iron_nugget', 'minecraft:iron_nugget'],
    'tip_ewd': ['minecraft:iron_nugget', 'minecraft:amethyst_shard'], 'tip_b': ['minecraft:snowball'],
    'tip_wb': ['minecraft:snowball', 'minecraft:snowball'], 'tip_mf': ['minecraft:iron_ingot'],
    'tip_rsf': ['minecraft:slime_ball', 'minecraft:cobblestone'], 'tip_fd': ['minecraft:diamond', 'minecraft:slime_ball'],
    'tip_fs': ['minecraft:diamond', 'minecraft:flint'],
}
for tid, extra in TIP_RECIPES.items():
    shapeless('part_' + tid, ['minecraft:iron_nugget'] + extra, 'librabeyblade:part_' + tid)
# the three new wheel sets (from the photos) and the standard parts of the common beyblades
shaped('part_wheel_bigbang', ['DND', 'NBN', 'DND'], {'D': 'minecraft:diamond', 'N': 'minecraft:iron_block', 'B': 'minecraft:end_crystal'}, 'librabeyblade:part_wheel_bigbang')
shaped('part_wheel_phoenix', ['DAD', 'ABA', 'DAD'], {'D': 'minecraft:diamond', 'A': 'minecraft:amethyst_block', 'B': 'minecraft:blaze_rod'}, 'librabeyblade:part_wheel_phoenix')
shaped('part_wheel_inferno', ['DGD', 'GBG', 'DGD'], {'D': 'minecraft:diamond', 'G': 'minecraft:gold_block', 'B': 'minecraft:magma_cream'}, 'librabeyblade:part_wheel_inferno')
for k, dye, gem in (('bigbang', 'minecraft:blue_dye', 'minecraft:lapis_block'), ('phoenix', 'minecraft:purple_dye', 'minecraft:amethyst_shard'),
                    ('inferno', 'minecraft:red_dye', 'minecraft:gold_ingot')):
    shapeless('part_ring_' + k, ['minecraft:glass_pane', 'minecraft:glass_pane', dye, gem], 'librabeyblade:part_ring_' + k)
    shapeless('part_bolt_' + k, ['minecraft:iron_ingot', dye, gem], 'librabeyblade:part_bolt_' + k)
shapeless('part_ring_standard', ['minecraft:glass_pane', 'minecraft:glass_pane', 'minecraft:white_dye'], 'librabeyblade:part_ring_standard')
shapeless('part_bolt_standard', ['minecraft:iron_ingot', 'minecraft:iron_nugget'], 'librabeyblade:part_bolt_standard')

# ---------------- lang ----------------
EN = collections.OrderedDict()
TR = collections.OrderedDict()
for p in PARTS:
    en, tr = names(p)
    EN['part.librabeyblade.' + p['id']] = en
    TR['part.librabeyblade.' + p['id']] = tr
    EN['item.librabeyblade.part_' + p['id']] = en
    TR['item.librabeyblade.part_' + p['id']] = tr
EN.update({
    'parttype.librabeyblade.bolt': 'Face Bolt', 'parttype.librabeyblade.ring': 'Energy Ring', 'parttype.librabeyblade.wheel': 'Fusion Wheel',
    'parttype.librabeyblade.track': 'Spin Track', 'parttype.librabeyblade.tip': 'Performance Tip',
    'rarity.librabeyblade.0': 'Common', 'rarity.librabeyblade.1': 'Uncommon', 'rarity.librabeyblade.2': 'Rare', 'rarity.librabeyblade.3': 'Legendary',
    'stat.librabeyblade.atk.short': 'ATK', 'stat.librabeyblade.def.short': 'DEF', 'stat.librabeyblade.sta.short': 'STA', 'stat.librabeyblade.spd.short': 'SPD',
    'stat.librabeyblade.weight': 'Weight: %s g', 'stat.librabeyblade.height': 'Height: %s mm',
    'item.librabeyblade.part.hint': 'Fit it into a beyblade at the Beyblade Workbench',
    'item.librabeyblade.part.level': 'Upgraded: ★%s / %s',
    'item.librabeyblade.part.missing': '- missing -',
    'item.librabeyblade.parts.stats': 'ATK %s  DEF %s  STA %s  SPD %s  · %s g',
    'msg.librabeyblade.incomplete': 'This beyblade is missing a part! Fit all five parts at the Beyblade Workbench.',
    'block.librabeyblade.bey_workbench': 'Beyblade Workbench', 'item.librabeyblade.bey_workbench': 'Beyblade Workbench',
    'container.librabeyblade.workbench': 'Beyblade Workbench',
    'screen.librabeyblade.workbench.empty': 'Put a beyblade in the golden slot. Its five parts appear around it: take them out, fit other parts and upgrade them with materials.',
    'screen.librabeyblade.workbench.material': 'Material',
    'screen.librabeyblade.workbench.type': 'Type: %s',
    'screen.librabeyblade.workbench.incomplete': 'Missing part - cannot launch',
    'screen.librabeyblade.workbench.upgrade': 'Upgrade to ★%s: %s× %s in the material slot',
    'screen.librabeyblade.workbench.maxed': 'Fully upgraded (★5)',
    'tip.librabeyblade.flat': 'Flat tip: fast, aggressive movement', 'tip.librabeyblade.rubber_flat': 'Rubber flat: grips the floor, top speed and attack',
    'tip.librabeyblade.extreme_flat': 'Extreme flat: the fastest, very little stamina', 'tip.librabeyblade.semi_flat': 'Semi flat: balanced movement',
    'tip.librabeyblade.sharp': 'Sharp tip: stays put, long spin', 'tip.librabeyblade.eternal_sharp': 'Eternal sharp: free spinning, loses less spin in hits',
    'tip.librabeyblade.coat_sharp': 'Coat sharp: stamina with a bit of grip', 'tip.librabeyblade.defense': 'Defense tip: hard to knock out',
    'tip.librabeyblade.wide_defense': 'Wide defense: stable and enduring', 'tip.librabeyblade.eternal_wide_defense': 'Eternal wide defense: free spinning wide tip',
    'tip.librabeyblade.ball': 'Ball tip: absorbs hits, balanced', 'tip.librabeyblade.wide_ball': 'Wide ball: heavy defense',
    'tip.librabeyblade.metal_flat': 'Metal flat: heavy, fast attack', 'tip.librabeyblade.rubber_semi_flat': 'Rubber semi flat: attack with control',
    'tip.librabeyblade.final_drive': 'Final Drive (4D): goes flat and faster when its spin runs low',
    'tip.librabeyblade.final_survive': 'Final Survive (4D): holds on at the very end of its spin', 'tip.librabeyblade.none': '-',
    'trait.librabeyblade.boost_disk': 'Boost Disk: heavy disk, much harder to push away',
    'trait.librabeyblade.claw': 'Claw: strikes hard from above lower beyblades',
    'trait.librabeyblade.wing': 'Wing: lighter air resistance, spins longer',
    'trait.librabeyblade.upper_wing': 'Upper Wing: lifts lower beyblades on contact',
    'trait.librabeyblade.tornado': 'Tornado: spins longer', 'trait.librabeyblade.defense_ring': 'Defense ring: takes less damage',
    'trait.librabeyblade.rubber_ring': 'Rubber ring: soaks up knockback', 'trait.librabeyblade.twin_height': 'Twin Height: counts as taller against anything up to 14.5',
    'trait.librabeyblade.gravity': 'Gravity (23 mm): crushing upper attacks, but easier to knock over',
    'trait.librabeyblade.spirit': 'Bit-beast: rarer bolts raise the Blader Spirit faster', 'trait.librabeyblade.four_d': '4D system',
})
TR.update({
    'parttype.librabeyblade.bolt': 'Yüz Cıvatası', 'parttype.librabeyblade.ring': 'Enerji Halkası', 'parttype.librabeyblade.wheel': 'Füzyon Çarkı',
    'parttype.librabeyblade.track': 'Dönüş Yolu', 'parttype.librabeyblade.tip': 'Performans Ucu',
    'rarity.librabeyblade.0': 'Sıradan', 'rarity.librabeyblade.1': 'Nadir Olmayan', 'rarity.librabeyblade.2': 'Nadir', 'rarity.librabeyblade.3': 'Efsanevi',
    'stat.librabeyblade.atk.short': 'SAL', 'stat.librabeyblade.def.short': 'SAV', 'stat.librabeyblade.sta.short': 'DAY', 'stat.librabeyblade.spd.short': 'HIZ',
    'stat.librabeyblade.weight': 'Ağırlık: %s g', 'stat.librabeyblade.height': 'Yükseklik: %s mm',
    'item.librabeyblade.part.hint': 'Bey Atölyesi\'nde bir beybladeye tak',
    'item.librabeyblade.part.level': 'Geliştirme: ★%s / %s',
    'item.librabeyblade.part.missing': '- eksik -',
    'item.librabeyblade.parts.stats': 'SAL %s  SAV %s  DAY %s  HIZ %s  · %s g',
    'msg.librabeyblade.incomplete': 'Bu beybladenin bir parçası eksik! Bey Atölyesi\'nde beş parçanın hepsini tak.',
    'block.librabeyblade.bey_workbench': 'Bey Atölyesi', 'item.librabeyblade.bey_workbench': 'Bey Atölyesi',
    'container.librabeyblade.workbench': 'Bey Atölyesi',
    'screen.librabeyblade.workbench.empty': 'Altın yuvaya bir beyblade koy. Beş parçası etrafında belirir: çıkar, başka parçalar tak ve malzemeyle geliştir.',
    'screen.librabeyblade.workbench.material': 'Malzeme',
    'screen.librabeyblade.workbench.type': 'Tür: %s',
    'screen.librabeyblade.workbench.incomplete': 'Eksik parça - fırlatılamaz',
    'screen.librabeyblade.workbench.upgrade': '★%s geliştirme: malzeme yuvasına %s× %s',
    'screen.librabeyblade.workbench.maxed': 'Tamamen geliştirildi (★5)',
    'tip.librabeyblade.flat': 'Düz uç: hızlı, saldırgan hareket', 'tip.librabeyblade.rubber_flat': 'Lastik düz: zemini tutar, en yüksek hız ve saldırı',
    'tip.librabeyblade.extreme_flat': 'Aşırı düz: en hızlısı, çok az dayanıklılık', 'tip.librabeyblade.semi_flat': 'Yarı düz: dengeli hareket',
    'tip.librabeyblade.sharp': 'Sivri uç: yerinde durur, uzun döner', 'tip.librabeyblade.eternal_sharp': 'Sonsuz sivri: serbest döner, çarpışmada az dönüş kaybeder',
    'tip.librabeyblade.coat_sharp': 'Kaplamalı sivri: biraz tutuşlu dayanıklılık', 'tip.librabeyblade.defense': 'Savunma ucu: sahadan atılması zor',
    'tip.librabeyblade.wide_defense': 'Geniş savunma: dengeli ve dayanıklı', 'tip.librabeyblade.eternal_wide_defense': 'Sonsuz geniş savunma: serbest dönen geniş uç',
    'tip.librabeyblade.ball': 'Top uç: darbeleri emer, dengeli', 'tip.librabeyblade.wide_ball': 'Geniş top: ağır savunma',
    'tip.librabeyblade.metal_flat': 'Metal düz: ağır, hızlı saldırı', 'tip.librabeyblade.rubber_semi_flat': 'Lastik yarı düz: kontrollü saldırı',
    'tip.librabeyblade.final_drive': 'Son Sürüş (4D): dönüşü azalınca düzleşir ve hızlanır',
    'tip.librabeyblade.final_survive': 'Son Direniş (4D): dönüşünün en sonunda direnir', 'tip.librabeyblade.none': '-',
    'trait.librabeyblade.boost_disk': 'Takviye Diski: ağır disk, itilmesi çok daha zor',
    'trait.librabeyblade.claw': 'Pençe: alçak beybladelere yukarıdan sert vurur',
    'trait.librabeyblade.wing': 'Kanat: daha az hava direnci, daha uzun döner',
    'trait.librabeyblade.upper_wing': 'Üst Kanat: çarpışınca alçak beybladeleri havaya kaldırır',
    'trait.librabeyblade.tornado': 'Kasırga: daha uzun döner', 'trait.librabeyblade.defense_ring': 'Savunma halkası: daha az hasar alır',
    'trait.librabeyblade.rubber_ring': 'Lastik halka: geri tepmeyi emer', 'trait.librabeyblade.twin_height': 'Çift Yükseklik: 14.5 mm\'ye kadar her şeye karşı uzun sayılır',
    'trait.librabeyblade.gravity': 'Yerçekimi (23 mm): ezici üstten saldırı, ama devrilmesi kolay',
    'trait.librabeyblade.spirit': 'Bit-beast: nadir cıvatalar Blader Ruhunu daha hızlı doldurur', 'trait.librabeyblade.four_d': '4D sistem',
})
for fn, kv in (('en_us', EN), ('tr_tr', TR)):
    p = os.path.join(A, 'lang/%s.json' % fn)
    d = json.load(open(p), object_pairs_hook=collections.OrderedDict)
    d.update(kv)
    json.dump(d, open(p, 'w'), ensure_ascii=False, indent=1)
print('ok')
