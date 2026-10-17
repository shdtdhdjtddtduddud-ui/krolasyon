"""Generates the mod's textures (run from the repo root: python3 tools/gen_textures.py)."""
import random
from PIL import Image

OUT = "src/main/resources/assets/mirrorworld/textures/"
random.seed(7)


def clamp(v):
    return max(0, min(255, int(v)))


def frame():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            grain = (x * 3 + (y // 4) * 5) % 7
            base = 52 + grain * 3 + random.randint(-4, 4)
            img.putpixel((x, y), (clamp(base * 1.05), clamp(base * 0.72), clamp(base * 0.5), 255))
    gold = [(232, 190, 92), (196, 150, 60), (250, 220, 130)]
    for i in range(16):
        for (x, y) in [(i, 0), (i, 15), (0, i), (15, i)]:
            img.putpixel((x, y), gold[(i // 2) % 2])
        if i % 4 == 1:
            for (x, y) in [(i, 1), (i, 14), (1, i), (14, i)]:
                img.putpixel((x, y), gold[2])
    img.save(OUT + "block/mirror_frame.png")


def backdrop():
    # the far side of the glass: a hazy silver-blue sky over 32 pixels, split into two block textures
    full = Image.new("RGBA", (16, 32))
    top, mid, bottom = (176, 186, 222), (122, 136, 184), (58, 70, 108)
    for y in range(32):
        t = y / 31
        a, b, k = (top, mid, t / 0.55) if t < 0.55 else (mid, bottom, (t - 0.55) / 0.45)
        for x in range(16):
            edge = min(x, 15 - x) / 7.5
            shade = 0.82 + 0.18 * min(1.0, edge * 1.6)
            c = [clamp((a[i] + (b[i] - a[i]) * k) * shade + random.randint(-3, 3)) for i in range(3)]
            full.putpixel((x, y), (*c, 255))
    full.crop((0, 0, 16, 16)).save(OUT + "block/mirror_back_top.png")
    full.crop((0, 16, 16, 32)).save(OUT + "block/mirror_back_bottom.png")


def glass():
    # added on top of everything (additive), so black means "no sheen"
    w, h = 32, 64
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            d1 = abs((x + y * 0.55) - 22)
            d2 = abs((x + y * 0.55) - 36)
            s = 10
            s += max(0, 70 - d1 * 14)
            s += max(0, 40 - d2 * 16)
            edge = min(x, w - 1 - x, y, h - 1 - y)
            s += max(0, 30 - edge * 10)
            img.putpixel((x, y), (clamp(s * 0.92), clamp(s * 0.97), clamp(s * 1.08), 255))
    img.save(OUT + "misc/mirror_glass.png")


def item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    gold, dark = (214, 170, 78, 255), (120, 84, 40, 255)
    for y in range(2, 16):
        for x in range(4, 12):
            border = x in (4, 11) or y in (2, 15)
            if border:
                img.putpixel((x, y), gold if (x + y) % 3 else dark)
            else:
                t = (y - 3) / 11
                c = (clamp(190 - 70 * t), clamp(200 - 60 * t), clamp(235 - 50 * t), 255)
                if abs((x + y * 0.6) - 11) < 0.8:
                    c = (245, 248, 255, 255)
                img.putpixel((x, y), c)
    for (x, y) in [(7, 0), (8, 0), (7, 1), (8, 1)]:
        img.putpixel((x, y), (170, 110, 230, 255))
    img.putpixel((7, 0), (220, 180, 255, 255))
    img.save(OUT + "item/magic_mirror.png")


frame()
backdrop()
glass()
item()
