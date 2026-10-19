#!/usr/bin/env python3
"""Zümrüt Kök Kılıcı dokuları (blok atlasına girer)."""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "emeraldblade", "textures")
os.makedirs(os.path.join(OUT, "block"), exist_ok=True)
os.makedirs(os.path.join(OUT, "item"), exist_ok=True)


def save(img, name, anim=None):
    img.save(os.path.join(OUT, "block", name + ".png"))
    if anim:
        with open(os.path.join(OUT, "block", name + ".png.mcmeta"), "w") as f:
            json.dump({"animation": anim}, f)


rnd = random.Random(11)

# --- zümrüt kristal: dikey çizikler ve iç parıltı ---
S = 64
em = Image.new("RGBA", (S, S))
for y in range(S):
    for x in range(S):
        v = 0.55 + 0.25 * math.sin(x * 0.35 + math.sin(y * 0.05) * 2) + rnd.uniform(-0.06, 0.06)
        g = int(130 + 110 * v)
        em.putpixel((x, y), (int(20 + 40 * v), min(255, g), int(70 + 50 * v), 225))
d = ImageDraw.Draw(em)
for i in range(26):
    x = rnd.randint(0, S - 1)
    y0 = rnd.randint(0, S - 1)
    ln = rnd.randint(6, 30)
    c = (170, 255, 200, 235) if rnd.random() < 0.5 else (10, 90, 50, 235)
    d.line([(x, y0), (x + rnd.randint(-3, 3), y0 + ln)], fill=c, width=1)
for i in range(10):
    x, y = rnd.randint(0, S - 1), rnd.randint(0, S - 1)
    d.line([(x, y), (x + rnd.randint(-8, 8), y + rnd.randint(-2, 2))], fill=(210, 255, 225, 240))
save(em, "emerald_crystal")

# --- iç öz: yukarı akan ışık damarları (animasyonlu) ---
N, F = 32, 16
core = Image.new("RGBA", (N, N * F))
for f in range(F):
    for y in range(N):
        for x in range(N):
            yy = (y + f * 2) % N
            vein = math.exp(-((x - 16 - 6 * math.sin(yy * 0.4)) ** 2) / 6.0) + 0.6 * math.exp(-((x - 7 - 3 * math.sin(yy * 0.6 + 1)) ** 2) / 3.0) \
                + 0.6 * math.exp(-((x - 25 - 3 * math.sin(yy * 0.5 + 2)) ** 2) / 3.0)
            spark = 1.0 if (x * 7 + yy * 13) % 97 == 0 else 0.0
            v = min(1.0, vein * (0.6 + 0.4 * math.sin(yy * 0.4 - f * 0.8)) + spark)
            core.putpixel((x, y + f * N), (int(60 + 150 * v), 255, int(110 + 120 * v), 255))
save(core, "emerald_core", {"frametime": 2, "interpolate": True})

# --- kök kabuğu: lifli kahverengi ---
B = 32
bark = Image.new("RGBA", (B, B))
for y in range(B):
    for x in range(B):
        fib = 0.5 + 0.5 * math.sin(x * 1.3 + math.sin(y * 0.3) * 1.5)
        n = rnd.uniform(-0.08, 0.08)
        v = 0.55 + 0.3 * fib + n
        bark.putpixel((x, y), (int(120 * v + 30), int(78 * v + 18), int(45 * v + 10), 255))
bd = ImageDraw.Draw(bark)
for i in range(14):
    x = rnd.randint(0, B - 1)
    y = rnd.randint(0, B - 1)
    bd.line([(x, y), (x + rnd.randint(-1, 1), y + rnd.randint(3, 8))], fill=(60, 36, 20, 255))
for i in range(8):
    x = rnd.randint(0, B - 1)
    y = rnd.randint(0, B - 1)
    bd.point((x, y), fill=(200, 160, 110, 255))
save(bark, "root_bark")

# --- kök üzerinde parlayan yosun/özsu çizgileri (glow overlay) ---
rg = Image.new("RGBA", (B, B), (0, 0, 0, 0))
rgd = ImageDraw.Draw(rg)
for i in range(6):
    x = rnd.randint(0, B - 1)
    y = rnd.randint(0, B - 1)
    rgd.line([(x, y), (x + rnd.randint(-2, 2), y + rnd.randint(4, 10))], fill=(120, 255, 150, 255))
save(rg, "root_glow")

# --- parçacık / kırılma ---
save(em.resize((16, 16), Image.LANCZOS), "emerald_particle")

# --- envanter yedeği için 2D eşya simgesi (ör. kurşun boru modlarında) ---
ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for i in range(16):
    pass
pix = [
    "...........GGGG.",
    "..........GgGGG.",
    ".........GgGGG..",
    "........GgGGG...",
    ".......GgGGG....",
    "......GgGGG.....",
    ".....GgGGG......",
    "..B.GgGBG.......",
    "...BBGBG........",
    "....BBB.........",
    "...BBBBB........",
    "..B..B..B.......",
    ".....B..........",
    "....B...........",
    "..eB............",
    "..ee............",
]
pal = {".": (0, 0, 0, 0), "G": (40, 200, 100, 255), "g": (170, 255, 200, 255), "B": (110, 70, 40, 255), "e": (60, 230, 120, 255)}
for y, row in enumerate(pix):
    for x, ch in enumerate(row):
        ic.putpixel((x, y), pal[ch])
ic.save(os.path.join(OUT, "item", "emerald_root_blade.png"))
print("ok")
