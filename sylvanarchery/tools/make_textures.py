#!/usr/bin/env python3
"""Orman Bekçisi okçuluk seti dokuları (blok atlasına girer)."""
import math
import os
import random

from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "sylvanarchery", "textures")
os.makedirs(os.path.join(OUT, "block"), exist_ok=True)
os.makedirs(os.path.join(OUT, "item"), exist_ok=True)
rnd = random.Random(5)


def save(img, name):
    img.save(os.path.join(OUT, "block", name + ".png"))


def noise_fill(img, base, var):
    w, h = img.size
    for y in range(h):
        for x in range(w):
            n = rnd.uniform(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, int(c + n))) for c in base) + (255,))


# koyu, çatlaklı yay kolu (taşlaşmış ahşap)
limb = Image.new("RGBA", (32, 32))
noise_fill(limb, (62, 64, 70), 10)
d = ImageDraw.Draw(limb)
for i in range(9):
    x, y = rnd.randint(0, 31), rnd.randint(0, 31)
    d.line([(x, y), (x + rnd.randint(-6, 6), y + rnd.randint(2, 9))], fill=(34, 35, 40, 255))
for i in range(6):
    x, y = rnd.randint(0, 31), rnd.randint(0, 31)
    d.line([(x, y), (x + rnd.randint(-4, 4), y + rnd.randint(1, 5))], fill=(98, 100, 108, 255))
save(limb, "limb")

# altın sargı
gold = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        stripe = (x + y) % 6 < 2
        v = 0.75 + 0.25 * math.sin(y * 0.6) + (0.12 if stripe else 0)
        gold.putpixel((x, y), (min(255, int(205 * v)), min(255, int(160 * v)), int(70 * v), 255))
ImageDraw.Draw(gold).line([(0, 0), (15, 0)], fill=(120, 85, 30, 255))
save(gold, "gold_wrap")

# koyu sargı (deri bant)
wrap = Image.new("RGBA", (16, 16))
noise_fill(wrap, (48, 48, 52), 6)
wd = ImageDraw.Draw(wrap)
for y in range(0, 16, 4):
    wd.line([(0, y), (15, y + 2)], fill=(30, 30, 33, 255))
save(wrap, "dark_wrap")

# tüy (turkuaz, sarı-yeşil şeritli)
F = 32
feather = Image.new("RGBA", (F, F), (0, 0, 0, 0))
for y in range(F):
    for x in range(F):
        cx = (x - 15.5) / 15.5
        t = y / (F - 1)
        width = math.sin(math.pi * min(1, t * 1.15)) * (0.95 - 0.2 * t)
        if abs(cx) > width:
            continue
        band = (y // 5) % 2 == 0
        base = (40, 110, 110) if not band else (60, 140, 120)
        if abs(cx) < 0.07:
            c = (210, 220, 180)
        elif 0.25 < abs(cx) < 0.45 and band:
            c = (190, 210, 90)
        else:
            c = base
        edge = abs(cx) > width - 0.12
        if edge:
            c = tuple(int(v * 0.6) for v in c)
        feather.putpixel((x, y), c + (255,))
save(feather, "feather")

# ok gövdesi
shaft = Image.new("RGBA", (16, 16))
noise_fill(shaft, (110, 90, 60), 8)
save(shaft, "shaft")

# yay kirişi
string = Image.new("RGBA", (4, 16), (225, 220, 200, 255))
save(string, "bowstring")

# sadak derisi: yeşil, altın çapraz dikişler
Q = 64
leather = Image.new("RGBA", (Q, Q))
for y in range(Q):
    for x in range(Q):
        n = rnd.uniform(-8, 8)
        v = 0.85 + 0.15 * math.sin(x * 0.2) * math.sin(y * 0.13)
        leather.putpixel((x, y), (int(50 * v + n), int(105 * v + n), int(85 * v + n), 255))
ld = ImageDraw.Draw(leather)
for cy in (10, 30, 50):
    for cx in (16, 48):
        ld.line([(cx - 4, cy - 4), (cx + 4, cy + 4)], fill=(215, 175, 85, 255), width=2)
        ld.line([(cx - 4, cy + 4), (cx + 4, cy - 4)], fill=(215, 175, 85, 255), width=2)
ld.line([(32, 0), (32, 63)], fill=(35, 75, 60, 255))
save(leather, "quiver_leather")

# kayış
belt = Image.new("RGBA", (32, 8))
noise_fill(belt, (55, 55, 60), 6)
bd = ImageDraw.Draw(belt)
bd.line([(0, 0), (31, 0)], fill=(30, 30, 34, 255))
bd.line([(0, 7), (31, 7)], fill=(30, 30, 34, 255))
for x in range(2, 32, 8):
    bd.rectangle([x, 3, x + 1, 4], fill=(200, 160, 80, 255))
save(belt, "belt")

# turkuaz taşlı altın plaka
plate = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
pd = ImageDraw.Draw(plate)
pd.polygon([(8, 0), (15, 8), (8, 15), (0, 8)], fill=(210, 165, 70, 255), outline=(130, 90, 30, 255))
pd.polygon([(8, 4), (11, 8), (8, 11), (5, 8)], fill=(40, 170, 150, 255))
pd.point((7, 6), fill=(200, 255, 240, 255))
save(plate, "gem_plate")

# parçacık
save(feather.resize((16, 16)), "particle")

# 2B yedek eşya simgeleri
for name in ("sylvan_bow", "sylvan_arrow", "sylvan_quiver"):
    ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ImageDraw.Draw(ic).line([(2, 13), (13, 2)], fill=(40, 120, 110, 255), width=2)
    ic.save(os.path.join(OUT, "item", name + ".png"))
print("ok")
