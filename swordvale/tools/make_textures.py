#!/usr/bin/env python3
"""Kılıçlar Vadisi dokuları: rün taşı, rün parçası."""
import os
import random

from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "swordvale", "textures")
rnd = random.Random(4)

# rün taşı: koyu taş üzerinde parlayan turkuaz kazınmış rün
img = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        v = rnd.randint(-10, 10)
        img.putpixel((x, y), (78 + v, 82 + v, 86 + v, 255))
d = ImageDraw.Draw(img)
glow, edge = (120, 240, 220, 255), (40, 90, 90, 255)
strokes = [[(5, 2), (5, 13)], [(5, 3), (10, 7)], [(10, 7), (5, 10)], [(10, 9), (11, 13)]]
for s in strokes:
    d.line([(p[0] + 1, p[1] + 1) for p in s], fill=edge)
for s in strokes:
    d.line(s, fill=glow)
img.save(os.path.join(BASE, "block", "rune_stone.png"))

# rün parçası (eşya)
shard = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
sd = ImageDraw.Draw(shard)
sd.polygon([(8, 1), (13, 6), (11, 14), (5, 15), (2, 8)], fill=(88, 92, 98, 255), outline=(50, 52, 58, 255))
sd.line([(7, 4), (7, 12)], fill=glow)
sd.line([(7, 6), (10, 8)], fill=glow)
sd.line([(10, 8), (7, 10)], fill=glow)
shard.save(os.path.join(BASE, "item", "rune_shard.png"))
print("ok")
