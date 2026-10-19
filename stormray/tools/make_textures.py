#!/usr/bin/env python3
"""Fırtına Vatozu dokuları (blok atlasına girer)."""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "stormray", "textures", "block")
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(21)
S = 64


def save(img, name, anim=None):
    img.save(os.path.join(OUT, name + ".png"))
    if anim:
        with open(os.path.join(OUT, name + ".png.mcmeta"), "w") as f:
            json.dump({"animation": anim}, f)


# sırt derisi: koyu lacivert-turkuaz, damarlı ve lekeli
skin = Image.new("RGBA", (S, S))
for y in range(S):
    for x in range(S):
        n = (math.sin(x * 0.21 + math.sin(y * 0.13) * 2) + math.sin(y * 0.17 + x * 0.05)) * 0.25
        v = 0.6 + n + rnd.uniform(-0.05, 0.05)
        skin.putpixel((x, y), (int(14 + 18 * v), int(38 + 40 * v), int(58 + 50 * v), 255))
d = ImageDraw.Draw(skin)
for i in range(40):
    x, y = rnd.randint(0, S - 1), rnd.randint(0, S - 1)
    r = rnd.choice([0, 0, 1])
    d.ellipse([x - r, y - r, x + r, y + r], fill=(30, 110, 130, 255))
for i in range(10):
    x, y = rnd.randint(0, S - 1), rnd.randint(0, S - 1)
    pts = [(x, y)]
    for k in range(6):
        x += rnd.randint(-5, 5)
        y += rnd.randint(-5, 5)
        pts.append((x, y))
    d.line(pts, fill=(22, 70, 90, 255))
save(skin, "ray_skin")

# karın: daha açık, mavimsi
belly = Image.new("RGBA", (S, S))
for y in range(S):
    for x in range(S):
        v = 0.75 + rnd.uniform(-0.05, 0.05)
        belly.putpixel((x, y), (int(40 * v), int(60 * v), int(95 * v), 255))
save(belly, "ray_belly")

# kenar: mor-mavi parlak şerit
rim = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        v = 0.7 + 0.3 * math.sin(x * 0.8) + rnd.uniform(-0.05, 0.05)
        rim.putpixel((x, y), (int(60 * v), int(70 * v), int(170 * v), 255))
save(rim, "ray_rim")

# biyolüminesan benekler ve damarlar (parlayan katman, animasyonlu yanıp sönme)
F = 8
glow = Image.new("RGBA", (S, S * F), (0, 0, 0, 0))
spots = [(rnd.randint(0, S - 1), rnd.randint(0, S - 1), rnd.random(), rnd.choice([0, 0, 1, 1, 2])) for _ in range(70)]
veins = []
for i in range(7):
    x, y = rnd.randint(0, S - 1), rnd.randint(0, S - 1)
    pts = [(x, y)]
    for k in range(8):
        x += rnd.randint(-6, 6)
        y += rnd.randint(-6, 6)
        pts.append((x, y))
    veins.append(pts)
for f in range(F):
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    for pts in veins:
        a = int(110 + 80 * math.sin(f / F * math.tau + pts[0][0]))
        ld.line(pts, fill=(60, 200, 230, max(40, a)), width=1)
    for (x, y, ph, r) in spots:
        b = 0.5 + 0.5 * math.sin(f / F * math.tau + ph * 6.28)
        c = (int(90 + 140 * b), int(220 + 35 * b), 255, int(120 + 135 * b))
        ld.ellipse([x - r, y - r, x + r, y + r], fill=c)
    glow.paste(layer, (0, f * S))
save(glow, "ray_glow", {"frametime": 3, "interpolate": True})

# çekirdek: parlak turkuaz-beyaz plazma
C = 32
core = Image.new("RGBA", (C, C * F))
for f in range(F):
    for y in range(C):
        for x in range(C):
            dx, dy = (x - 15.5) / 15.5, (y - 15.5) / 15.5
            r = math.hypot(dx, dy)
            sw = 0.5 + 0.5 * math.sin(math.atan2(dy, dx) * 5 + f / F * math.tau + r * 6)
            v = max(0.0, 1 - r) * (0.7 + 0.3 * sw)
            core.putpixel((x, y + f * C), (int(80 + 175 * v), int(200 + 55 * v), 255, 255))
save(core, "ray_core", {"frametime": 2, "interpolate": True})

# şimşek telleri
bolt = Image.new("RGBA", (4, 16), (200, 250, 255, 255))
save(bolt, "ray_bolt")

# göz
eye = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
ImageDraw.Draw(eye).ellipse([0, 1, 7, 6], fill=(140, 255, 255, 255))
ImageDraw.Draw(eye).ellipse([3, 2, 5, 5], fill=(255, 255, 255, 255))
save(eye, "ray_eye")

# eyer
saddle = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        v = 0.8 + 0.2 * math.sin(y * 0.7) + rnd.uniform(-0.05, 0.05)
        saddle.putpixel((x, y), (int(110 * v), int(70 * v), int(40 * v), 255))
ImageDraw.Draw(saddle).rectangle([0, 0, 15, 15], outline=(200, 160, 80, 255))
save(saddle, "ray_saddle")
print("ok")
