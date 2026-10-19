#!/usr/bin/env python3
"""Ada Tohumu eşya dokusunu (16x16) çizer."""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                   "assets", "skyislands", "textures", "item", "island_seed.png")
rows = [
    "................",
    "......LLL.......",
    ".....LLlLL......",
    "....LLLLlLL.....",
    ".....LLTLL......",
    ".......T........",
    "..GGgGGTGGgGG...",
    ".GgGGGGGGGGGgG..",
    ".dDDdDDDdDDdDw..",
    "..sSSsSSSsSSSw..",
    "...SsSSsSSsS.w..",
    "....SSsSSsS..w..",
    ".....sSSSv...b..",
    "......SsS.v.....",
    ".......S..v.....",
    "................",
]
pal = {
    ".": (0, 0, 0, 0), "L": (62, 160, 52, 255), "l": (120, 200, 80, 255), "T": (110, 78, 40, 255),
    "G": (82, 170, 60, 255), "g": (140, 210, 90, 255), "d": (120, 85, 55, 255), "D": (96, 68, 44, 255),
    "S": (120, 120, 132, 255), "s": (150, 150, 165, 255), "w": (110, 190, 255, 255), "b": (200, 235, 255, 255),
    "v": (90, 200, 120, 255),
}
img = Image.new("RGBA", (16, 16))
for y, row in enumerate(rows):
    for x, ch in enumerate(row):
        img.putpixel((x, y), pal[ch])
os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("wrote", OUT)
