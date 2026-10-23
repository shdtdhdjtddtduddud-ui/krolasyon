"""Gerçekçi Hava dokuları ve kaynak dosyaları (PIL + numpy)."""
import json, math, os
import numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'realweather')
D = os.path.join(ROOT, 'data', 'realweather')
rng = np.random.default_rng(42)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        if isinstance(obj, str):
            f.write(obj)
        else:
            json.dump(obj, f, indent=1, ensure_ascii=False)


def save(rel, arr):
    p = os.path.join(A, 'textures', rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA').save(p)


# ------------------------------------------------------------------ yağış perdeleri (dikeyde döşenir)
def rain():
    W, H = 64, 512
    img = np.zeros((H, W, 4))
    for _ in range(170):
        x = rng.integers(0, W)
        y = rng.integers(0, H)
        ln = rng.integers(10, 42)
        a = rng.uniform(0.25, 0.75)
        width = 1 if rng.random() < 0.8 else 2
        for k in range(ln):
            f = k / ln
            al = a * math.sin(math.pi * f) ** 0.6
            for dx in range(width):
                yy = (y + k) % H
                xx = (x + dx) % W
                img[yy, xx, :3] = [205, 220, 240]
                img[yy, xx, 3] = max(img[yy, xx, 3], al * 255 * (0.7 if dx else 1))
    save('environment/rain.png', img)


def snow():
    W, H = 128, 512
    img = np.zeros((H, W, 4))
    yy, xx = np.mgrid[0:H, 0:W]
    for _ in range(260):
        cx, cy = rng.uniform(0, W), rng.uniform(0, H)
        r = rng.choice([0.7, 1.0, 1.4, 1.9, 2.6], p=[0.3, 0.3, 0.2, 0.13, 0.07])
        dx = np.minimum(np.abs(xx - cx), W - np.abs(xx - cx))
        dy = np.minimum(np.abs(yy - cy), H - np.abs(yy - cy))
        d = np.sqrt(dx ** 2 + dy ** 2)
        a = np.clip(1.2 - d / r, 0, 1) * rng.uniform(0.6, 1.0)
        img[..., 3] = np.maximum(img[..., 3], a * 255)
    img[..., :3] = 250
    save('environment/snow.png', img)


def sand():
    W, H = 256, 128
    img = np.zeros((H, W, 4))
    for _ in range(900):
        x, y = rng.integers(0, W), rng.integers(0, H)
        ln = rng.integers(3, 26)
        a = rng.uniform(0.2, 0.8)
        col = np.array([222, 184, 128]) * rng.uniform(0.75, 1.1)
        for k in range(ln):
            f = k / ln
            xx = (x + k) % W
            img[y, xx, :3] = col
            img[y, xx, 3] = max(img[y, xx, 3], a * 255 * math.sin(math.pi * f))
    # ince toz perdesi
    haze = Image.fromarray((rng.random((H // 8, W // 8)) * 255).astype(np.uint8)).resize((W, H), Image.BICUBIC)
    h = np.asarray(haze, float) / 255
    img[..., :3] = np.where(img[..., 3:4] > 0, img[..., :3], np.array([210, 170, 115]))
    img[..., 3] = np.maximum(img[..., 3], h * 12)
    save('environment/sand.png', img)


# ------------------------------------------------------------------ parçacıklar
def blob(n, r, soft=1.0, col=(255, 255, 255), noise=0.0):
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    d = np.sqrt((xx - n / 2) ** 2 + (yy - n / 2) ** 2) / r
    a = np.clip(1 - d, 0, 1) ** soft
    if noise:
        nz = Image.fromarray((rng.random((n // 4 + 1, n // 4 + 1)) * 255).astype(np.uint8)).resize((n, n), Image.BICUBIC)
        a *= 1 - noise + noise * np.asarray(nz, float) / 255
    img = np.zeros((n, n, 4))
    img[..., :3] = col
    img[..., 3] = a * 255
    return img


def flake(n, arms):
    img = np.zeros((n, n, 4))
    c = n / 2
    pil = Image.new('L', (n * 4, n * 4), 0)
    from PIL import ImageDraw
    dr = ImageDraw.Draw(pil)
    for k in range(arms):
        a = math.pi * 2 * k / arms
        x1, y1 = c * 4 + math.cos(a) * n * 1.7, c * 4 + math.sin(a) * n * 1.7
        dr.line([(c * 4, c * 4), (x1, y1)], fill=255, width=3)
        for s in (0.45, 0.7):
            bx, by = c * 4 + math.cos(a) * n * 1.7 * s, c * 4 + math.sin(a) * n * 1.7 * s
            for side in (-1, 1):
                b = a + side * 0.7
                dr.line([(bx, by), (bx + math.cos(b) * n * 0.5, by + math.sin(b) * n * 0.5)], fill=255, width=2)
    pil = pil.resize((n, n), Image.LANCZOS)
    img[..., :3] = 255
    img[..., 3] = np.asarray(pil, float)
    return img


def particles():
    for i in range(4):
        save('particle/snowflake_%d.png' % i, flake(16, 6) if i < 2 else blob(8, 3.5, 1.4))
    # yaprak: gri tonlu (biyom rengiyle boyanır), damarlı
    for i in range(3):
        n = 8
        img = np.zeros((n, n, 4))
        yy, xx = np.mgrid[0:n, 0:n] + 0.5
        ang = i * 0.6
        u = (xx - n / 2) * math.cos(ang) + (yy - n / 2) * math.sin(ang)
        v = -(xx - n / 2) * math.sin(ang) + (yy - n / 2) * math.cos(ang)
        inside = (u / 3.6) ** 2 + (v / 2.0) ** 2 < 1
        val = 200 + 40 * (np.abs(v) < 0.5)
        img[..., :3] = np.where(inside[..., None], val[..., None], 0)
        img[..., 3] = inside * 255
        save('particle/leaf_%d.png' % i, img)
    save('particle/sand_0.png', blob(4, 2.2, 0.8, (255, 255, 255)))
    save('particle/firefly_0.png', blob(8, 4, 1.6, (255, 255, 255)))
    # su halkası
    n = 16
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    d = np.sqrt((xx - 8) ** 2 + (yy - 8) ** 2)
    ring = np.clip(1 - np.abs(d - 6.2) / 1.2, 0, 1)
    img = np.zeros((n, n, 4))
    img[..., :3] = [220, 235, 255]
    img[..., 3] = ring * 220
    save('particle/ripple_0.png', img)
    for i in range(3):
        save('particle/mist_%d.png' % i, blob(32, 15, 1.8, (255, 255, 255), noise=0.5))
    save('particle/breath_0.png', blob(16, 7, 1.5, (255, 255, 255), noise=0.3))
    save('particle/splash_0.png', blob(4, 2, 1.0, (230, 240, 255)))
    for k, count in (('snowflake', 4), ('leaf', 3), ('sand', 1), ('firefly', 1), ('ripple', 1), ('mist', 3), ('breath', 1), ('splash', 1)):
        w(os.path.join(A, 'particles', k + '.json'), {"textures": ["realweather:%s_%d" % (k, i) for i in range(count)]})


# ------------------------------------------------------------------ ekran kenarı efektleri
def vignettes():
    W, H = 512, 288
    yy, xx = np.mgrid[0:H, 0:W] + 0.5
    nx, ny = (xx / W - 0.5) * 2, (yy / H - 0.5) * 2
    d = np.sqrt(nx ** 2 * 0.9 + ny ** 2 * 1.1)
    def noise(scale, amp):
        n = Image.fromarray((rng.random((H // scale + 2, W // scale + 2)) * 255).astype(np.uint8)).resize((W, H), Image.BICUBIC)
        return (np.asarray(n, float) / 255 - 0.5) * amp
    # kum: kenarlarda toz, ortada hafif sarı perde
    a = np.clip((d - 0.35) / 0.9, 0, 1) ** 1.3 * 0.95 + 0.18 + noise(6, 0.25)
    img = np.zeros((H, W, 4))
    img[..., 0], img[..., 1], img[..., 2] = 196, 152, 96
    img[..., 3] = np.clip(a, 0, 1) * 255
    save('misc/sand_vignette.png', img)
    # buz: kenarlardan içeri büyüyen kristaller
    cryst = np.clip(noise(3, 1.6) + noise(12, 1.2) + (d - 0.75) * 2.4, 0, 1)
    img = np.zeros((H, W, 4))
    img[..., :3] = np.dstack([215 + 30 * cryst, 232 + 20 * cryst, 255 * np.ones_like(cryst)])
    img[..., 3] = cryst * 230
    save('misc/frost_vignette.png', img)
    # ıslak: kenarlarda damlalar ve hafif koyulaşma
    img = np.zeros((H, W, 4))
    img[..., :3] = 40
    img[..., 3] = np.clip((d - 0.6) * 0.6, 0, 1) * 255
    for _ in range(90):
        cx, cy = rng.uniform(0, W), rng.uniform(0, H)
        if math.hypot((cx / W - 0.5) * 2, (cy / H - 0.5) * 2) < 0.55:
            continue
        r = rng.uniform(2, 7)
        m = (xx - cx) ** 2 + ((yy - cy) * 0.8) ** 2 < r * r
        img[m, :3] = [200, 215, 235]
        img[m, 3] = 140
    save('misc/wet_vignette.png', img)


def barometer():
    n = 16
    img = np.zeros((n, n, 4))
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    d = np.sqrt((xx - 8) ** 2 + (yy - 8) ** 2)
    rim = (d < 7.5) & (d >= 6.2)
    face = d < 6.2
    img[rim] = [180, 110, 50, 255]
    img[face] = [236, 228, 205, 255]
    for k in range(12):
        a = k / 12 * math.pi * 2
        x, y = int(8 + math.cos(a) * 5), int(8 + math.sin(a) * 5)
        img[y, x] = [60, 50, 40, 255]
    for t in np.linspace(0, 4.6, 12):
        x, y = int(8 + math.cos(-0.9) * t), int(8 + math.sin(-0.9) * t)
        img[y, x] = [170, 30, 30, 255]
    img[8, 8] = [40, 30, 20, 255]
    edge = (d < 8) & (d >= 7.5)
    img[edge] = [90, 50, 20, 255]
    save('item/barometer.png', img)


rain(); snow(); sand(); particles(); vignettes(); barometer()

# ------------------------------------------------------------------ sesler, dil, eşya, tarif
S = {}
for k in ('rain_heavy', 'rain_light', 'rain_indoor', 'rain_leaves', 'wind_soft', 'wind_strong', 'blizzard', 'sandstorm', 'leaves', 'crickets'):
    S[k] = {"sounds": [{"name": "realweather:" + k, "stream": True}], "subtitle": "subtitles.realweather." + k}
S['thunder_near'] = {"sounds": [{"name": "realweather:thunder_near%d" % i} for i in (1, 2, 3)], "subtitle": "subtitles.realweather.thunder"}
S['thunder_far'] = {"sounds": [{"name": "realweather:thunder_far%d" % i} for i in (1, 2, 3)], "subtitle": "subtitles.realweather.thunder"}
w(os.path.join(A, 'sounds.json'), S)
tr = {"item.realweather.barometer": "Barometre", "tooltip.realweather.barometer": "Sağ tık: hava durumu, rüzgâr ve yaklaşan cephe",
      "subtitles.realweather.rain_heavy": "Sağanak", "subtitles.realweather.rain_light": "Yağmur", "subtitles.realweather.rain_indoor": "Çatıda yağmur",
      "subtitles.realweather.rain_leaves": "Yapraklarda yağmur", "subtitles.realweather.wind_soft": "Rüzgâr", "subtitles.realweather.wind_strong": "Sert rüzgâr",
      "subtitles.realweather.blizzard": "Tipi", "subtitles.realweather.sandstorm": "Kum fırtınası", "subtitles.realweather.leaves": "Yaprak hışırtısı",
      "subtitles.realweather.crickets": "Cırcır böcekleri", "subtitles.realweather.thunder": "Gök gürültüsü"}
en = {"item.realweather.barometer": "Barometer", "tooltip.realweather.barometer": "Right click: weather, wind and the coming front",
      "subtitles.realweather.rain_heavy": "Downpour", "subtitles.realweather.rain_light": "Rain", "subtitles.realweather.rain_indoor": "Rain on the roof",
      "subtitles.realweather.rain_leaves": "Rain on leaves", "subtitles.realweather.wind_soft": "Wind", "subtitles.realweather.wind_strong": "Strong wind",
      "subtitles.realweather.blizzard": "Blizzard", "subtitles.realweather.sandstorm": "Sandstorm", "subtitles.realweather.leaves": "Rustling leaves",
      "subtitles.realweather.crickets": "Crickets", "subtitles.realweather.thunder": "Thunder"}
w(os.path.join(A, 'lang', 'tr_tr.json'), tr)
w(os.path.join(A, 'lang', 'en_us.json'), en)
w(os.path.join(A, 'models', 'item', 'barometer.json'), {"parent": "minecraft:item/generated", "textures": {"layer0": "realweather:item/barometer"}})
w(os.path.join(D, 'recipes', 'barometer.json'), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" C ", "CGC", " R "],
  "key": {"C": {"item": "minecraft:copper_ingot"}, "G": {"item": "minecraft:glass_pane"}, "R": {"item": "minecraft:redstone"}},
  "result": {"item": "realweather:barometer", "count": 1}})
w(os.path.join(ROOT, 'pack.mcmeta'), {"pack": {"description": "Krolasyon Real Weather resources", "pack_format": 15}})
w(os.path.join(ROOT, 'META-INF', 'mods.toml'), '''modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"

[[mods]]
modId="realweather"
version="1.0.0"
displayName="Krolasyon Gerçekçi Hava ve İklim"
authors="krolasyon"
description=\'\'\'
Kademeli gelen hava cepheleri (açık, bulutlu, sis, çiseleme, yağmur, fırtına) ve biyoma göre görünüşleri: soğukta kar ve tipi,
çölde kum fırtınası, ılıman yerlerde gök gürültülü sağanak. Rüzgârla eğilen yağış, sis ve gök rengi, uzak şimşekler, su halkaları,
düşen yapraklar, ateş böcekleri, sabah pusu ve nefes buharı; sentezlenmiş yağmur, rüzgâr, tipi, kum, yaprak, cırcır böceği ve gök
gürültüsü sesleri (içeride boğuklaşır). Tipide donma, karın birikip erimesi, yağmurda hızlı büyüyen ekinler, barometre.
\'\'\'

[[dependencies.realweather]]
    modId="forge"
    mandatory=true
    versionRange="[47,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.realweather]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.21)"
    ordering="NONE"
    side="BOTH"
''')
print('ok')
