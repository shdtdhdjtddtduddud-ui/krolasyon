#!/usr/bin/env python3
"""Eterik Çekirdek dokularını üretir (blok atlasına girer; animasyonlu olanlar .mcmeta ile)."""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                   "assets", "aethercore", "textures", "block")
os.makedirs(OUT, exist_ok=True)


def save(img, name, frames=None, frametime=2, interpolate=True):
    img.save(os.path.join(OUT, name + ".png"))
    if frames:
        with open(os.path.join(OUT, name + ".png.mcmeta"), "w") as f:
            json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, f)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


# --- zırh plakası: inci beyazı metal, altın kenarlar, mavi kanallar ---
S = 64
shell = Image.new("RGBA", (S, S))
glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
rnd = random.Random(3)
for y in range(S):
    for x in range(S):
        t = y / (S - 1)
        base = lerp((236, 232, 222), (178, 176, 182), t * 0.8)
        n = rnd.randint(-6, 6)
        base = tuple(max(0, min(255, c + n)) for c in base)
        shell.putpixel((x, y), base + (255,))
d = ImageDraw.Draw(shell)
gd = ImageDraw.Draw(glow)
gold, gold_d = (214, 170, 82, 255), (150, 108, 46, 255)
# altın kenar bantları
for i in range(5):
    c = gold if i in (1, 2, 3) else gold_d
    d.rectangle([i, i, S - 1 - i, S - 1 - i], outline=c)
# iç panel çizgileri (koyu lacivert oyuk + parlayan çekirdek)
for (x0, y0, x1, y1) in [(10, 10, 53, 22), (10, 41, 53, 53), (10, 25, 22, 38), (41, 25, 53, 38)]:
    d.rectangle([x0, y0, x1, y1], outline=(60, 70, 110, 255))
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], outline=(110, 120, 150, 255))
for (x0, y0, x1, y1) in [(12, 15, 51, 17), (12, 46, 51, 48)]:
    d.rectangle([x0, y0, x1, y1], fill=(40, 70, 140, 255))
    gd.rectangle([x0, y0, x1, y1], fill=(90, 200, 255, 255))
for (x0, y0, x1, y1) in [(15, 28, 17, 35), (46, 28, 48, 35)]:
    d.rectangle([x0, y0, x1, y1], fill=(40, 70, 140, 255))
    gd.rectangle([x0, y0, x1, y1], fill=(120, 220, 255, 255))
# perçinler
for (x, y) in [(7, 7), (56, 7), (7, 56), (56, 56), (31, 7), (31, 56)]:
    d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(250, 230, 160, 255))
save(shell, "aether_shell")
save(glow, "aether_shell_glow")

# --- ekvator bandı ---
band = Image.new("RGBA", (64, 16))
bd = ImageDraw.Draw(band)
for y in range(16):
    c = lerp((200, 160, 80), (245, 225, 160), 1 - abs(y - 7.5) / 7.5)
    bd.line([(0, y), (63, y)], fill=c + (255,))
for x in range(0, 64, 8):
    bd.rectangle([x + 2, 5, x + 5, 10], fill=(50, 80, 150, 255))
bd.line([(0, 0), (63, 0)], fill=(130, 95, 40, 255))
bd.line([(0, 15), (63, 15)], fill=(130, 95, 40, 255))
save(band, "aether_band")
bandg = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
bgd = ImageDraw.Draw(bandg)
for x in range(0, 64, 8):
    bgd.rectangle([x + 2, 5, x + 5, 10], fill=(110, 210, 255, 255))
save(bandg, "aether_band_glow")

# --- plazma çekirdek (animasyonlu, döşenebilir hücresel desen) ---
N, F = 32, 16
pts = [(random.Random(i).random() * N, random.Random(i + 99).random() * N) for i in range(9)]
core = Image.new("RGBA", (N, N * F))
for f in range(F):
    ph = f / F * math.tau
    moved = [((px + 3 * math.cos(ph + i)) % N, (py + 3 * math.sin(ph * 1 + i * 2)) % N) for i, (px, py) in enumerate(pts)]
    for y in range(N):
        for x in range(N):
            ds = []
            for (px, py) in moved:
                dx = min(abs(x - px), N - abs(x - px))
                dy = min(abs(y - py), N - abs(y - py))
                ds.append(math.hypot(dx, dy))
            ds.sort()
            edge = ds[1] - ds[0]
            v = math.exp(-edge * 0.55)
            sw = 0.5 + 0.5 * math.sin(x * 0.4 + y * 0.25 + ph)
            r = int(40 + 120 * v + 20 * sw)
            g = int(120 + 120 * v + 15 * sw)
            b = int(230 + 25 * v)
            a = int(150 + 105 * v)
            core.putpixel((x, y + f * N), (min(r, 255), min(g, 255), min(b, 255), a))
save(core, "aether_core_energy", frames=True, frametime=2)

# --- mercek / mücevher (nabız) ---
G, GF = 32, 8
gem = Image.new("RGBA", (G, G * GF))
for f in range(GF):
    pulse = 0.75 + 0.25 * math.sin(f / GF * math.tau)
    for y in range(G):
        for x in range(G):
            dx, dy = (x - 15.5) / 15.5, (y - 15.5) / 15.5
            r = math.hypot(dx * 0.8, dy)
            if r > 1:
                gem.putpixel((x, y + f * G), (0, 0, 0, 0))
                continue
            core_v = math.exp(-r * r * 3.0) * pulse
            ring = math.exp(-((r - 0.85) ** 2) * 120)
            c = (int(70 + 185 * core_v), int(170 + 85 * core_v), 255, int(200 + 55 * max(core_v, ring)))
            gem.putpixel((x, y + f * G), c)
save(gem, "aether_gem", frames=True, frametime=3)

# --- altıgen kalkan karosu ---
H = 32
hexi = Image.new("RGBA", (H, H), (0, 0, 0, 0))
hd = ImageDraw.Draw(hexi)
poly = [(16 + 15 * math.cos(math.radians(60 * k + 30)), 16 + 15 * math.sin(math.radians(60 * k + 30))) for k in range(6)]
hd.polygon(poly, fill=(120, 200, 255, 50))
inner = [(16 + 12 * math.cos(math.radians(60 * k + 30)), 16 + 12 * math.sin(math.radians(60 * k + 30))) for k in range(6)]
hd.line(poly + [poly[0]], fill=(170, 235, 255, 255), width=2)
hd.line(inner + [inner[0]], fill=(120, 200, 255, 120), width=1)
save(hexi, "aether_hex")

# --- kalkan kubbesi karosu (yalnız ince altıgen çizgileri) ---
dome = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
dd = ImageDraw.Draw(dome)
r = 9.2
for cy in range(-1, 3):
    for cx in range(-1, 3):
        ox = cx * r * math.sqrt(3) + (cy % 2) * r * math.sqrt(3) / 2
        oy = cy * r * 1.5
        pts6 = [(ox + r * math.cos(math.radians(60 * k + 30)), oy + r * math.sin(math.radians(60 * k + 30))) for k in range(6)]
        dd.line(pts6 + [pts6[0]], fill=(150, 225, 255, 255), width=1)
dome2 = Image.new("RGBA", (32, 32), (60, 140, 255, 40))
dome2.alpha_composite(dome)
save(dome2, "aether_dome")

# --- kaide / kırılma parçacığı ---
save(shell.resize((16, 16), Image.LANCZOS), "aether_particle")
print("textures written to", OUT)
