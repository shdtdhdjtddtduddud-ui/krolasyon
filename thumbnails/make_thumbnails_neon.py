"""Neon-outline "reacts to" thumbnails in the Mr.Zombie / Jiang-Si style:
blurred dark background, cut-out characters with glowing neon outlines and a
dripping horror title in the middle. Cut-outs live in cutouts/ (made with
rembg SAM from original.jpg). Usage: python make_thumbnails_neon.py FONT_DIR"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont
from scipy import ndimage as nd

HERE = os.path.dirname(os.path.abspath(__file__))
FONT_DIR = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "fonts")
OUT = os.path.join(HERE, "output_neon")
os.makedirs(OUT, exist_ok=True)
W, H = 1280, 720

SRC = Image.open(os.path.join(HERE, "original.jpg")).convert("RGB")
AIZEN_BOX = (478, 165, 802, 482)


def font(name, size):
    return ImageFont.truetype(os.path.join(FONT_DIR, name), size)


CREEP = lambda s: font("Creepster-Regular.ttf", s)
NOSIF = lambda s: font("Nosifer-Regular.ttf", s)
BUTCH = lambda s: font("Butcherman-Regular.ttf", s)
METAL = lambda s: font("MetalMania-Regular.ttf", s)
EATER = lambda s: font("Eater-Regular.ttf", s)


def rgba(c, a=255):
    r, g, b = Image.new("RGB", (1, 1), c).getpixel((0, 0))
    return (r, g, b, a)


def background(tint="#1a0010", blur=18, bright=0.33, src=None):
    bg = (src or SRC).filter(ImageFilter.GaussianBlur(blur))
    bg = ImageEnhance.Color(bg).enhance(0.35)
    bg = ImageEnhance.Brightness(bg).enhance(bright).convert("RGBA")
    bg.alpha_composite(Image.new("RGBA", (W, H), rgba(tint, 110)))
    # vignette
    m = Image.radial_gradient("L").resize((int(W * 1.5), int(H * 1.9)))
    v = Image.new("L", (W, H), 255)
    v.paste(m, ((W - m.width) // 2, (H - m.height) // 2))
    v = v.point(lambda x: max(0, min(230, int((x - 100) * 1.4))))
    bg.alpha_composite(Image.merge("RGBA", [Image.new("L", (W, H), 0)] * 3 + [v]))
    return bg


def char(name, height=None, width=None, flip=False, bright=1.1):
    im = Image.open(os.path.join(HERE, "cutouts", f"{name}.png")).convert("RGBA")
    if flip:
        im = im.transpose(Image.FLIP_LEFT_RIGHT)
    if height:
        im = im.resize((round(im.width * height / im.height), height), Image.LANCZOS)
    elif width:
        im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    rgb = ImageEnhance.Contrast(ImageEnhance.Color(im.convert("RGB")).enhance(1.2)).enhance(1.08)
    rgb = ImageEnhance.Brightness(rgb).enhance(bright)
    out = rgb.convert("RGBA")
    out.putalpha(im.split()[3])
    return out


def aizen(height=560, shape="oval"):
    """Aizen panel softened into the darkness (his hair can't be separated from
    the dark panel background, so he gets a soft silhouette instead of a hard cut)."""
    im = SRC.crop(AIZEN_BOX)
    im = ImageEnhance.Contrast(ImageEnhance.Color(im).enhance(1.15)).enhance(1.12)
    im = im.resize((round(im.width * height / im.height), height), Image.LANCZOS)
    w, h = im.size
    m = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(m)
    if shape == "oval":
        d.ellipse((w * 0.04, h * 0.02, w * 0.96, h * 1.25), fill=255)
    else:
        d.rounded_rectangle((12, 12, w - 12, h - 12), 40, fill=255)
    m = m.filter(ImageFilter.GaussianBlur(10))
    out = im.convert("RGBA")
    out.putalpha(m)
    return out


def neon(canvas, im, pos, color, core="#ffffff", line=5, glow=22, hard=True):
    """Paste a cut-out with a crisp neon line hugging the silhouette plus an outer glow."""
    pad = glow * 3 + line
    a = np.array(im.split()[3]) > 110
    a = np.pad(a, pad)
    size = (a.shape[1], a.shape[0])
    ring = nd.binary_dilation(a, iterations=line) & ~nd.binary_erosion(a, iterations=1)
    wide = nd.binary_dilation(a, iterations=line + 3)
    col = Image.new("RGBA", size, rgba(color))
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    g = Image.new("RGBA", size, (0, 0, 0, 0))
    g.paste(col, (0, 0), Image.fromarray((wide * 255).astype("uint8")))
    g = g.filter(ImageFilter.GaussianBlur(glow))
    for _ in range(3):
        layer.alpha_composite(g)
    ring_img = Image.fromarray((ring * 255).astype("uint8"))
    layer.paste(col, (0, 0), ring_img)
    if hard:
        inner = Image.fromarray((nd.binary_erosion(ring, iterations=max(1, line // 3)) * 255).astype("uint8"))
        layer.paste(Image.new("RGBA", size, rgba(core, 200)), (0, 0), inner.filter(ImageFilter.GaussianBlur(1)))
    layer.alpha_composite(im, (pad, pad))
    canvas.alpha_composite(layer, (pos[0] - pad, pos[1] - pad))


def soft_glow(canvas, im, pos, color, radius=35, times=3):
    pad = radius * 3
    g = Image.new("RGBA", (im.width + 2 * pad, im.height + 2 * pad), (0, 0, 0, 0))
    g.paste(Image.new("RGBA", im.size, rgba(color)), (pad, pad), im.split()[3])
    g = g.filter(ImageFilter.GaussianBlur(radius))
    for _ in range(times):
        canvas.alpha_composite(g, (pos[0] - pad, pos[1] - pad))
    canvas.alpha_composite(im, pos)


def title(canvas, lines, center, fnt, fill="#E3121B", stroke="#2b0000", glow="#ff0000",
          sw=4, spacing=0.92, glow_r=14):
    """Stacked centered horror title with a colored outer glow."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    lf = lines_fonts(lines, fnt)
    heights = [d.textbbox((0, 0), "Ag", font=f)[3] - d.textbbox((0, 0), "Ag", font=f)[1] for _, f in lf]
    total = sum(int(h * spacing) for h in heights) + int(heights[-1] * (1 - spacing))
    y = center[1] - total // 2
    for (ln, f), h in zip(lf, heights):
        b = d.textbbox((0, 0), ln, font=f)
        d.text((center[0] - (b[2] - b[0]) // 2 - b[0], y - b[1]), ln, font=f, fill=fill,
               stroke_width=sw, stroke_fill=stroke)
        y += int(h * spacing)
    g = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g.paste(Image.new("RGBA", (W, H), rgba(glow)), (0, 0), layer.split()[3])
    g = g.filter(ImageFilter.GaussianBlur(glow_r))
    canvas.alpha_composite(g)
    canvas.alpha_composite(g)
    sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sh.paste(Image.new("RGBA", (W, H), (0, 0, 0, 220)), (5, 6), layer.split()[3])
    canvas.alpha_composite(sh)
    canvas.alpha_composite(layer)


def lines_fonts(lines, fnt):
    return [(ln, fnt(size)) for ln, size in lines]


def save(img, n, slug):
    path = os.path.join(OUT, f"{n:02d}_{slug}.jpg")
    img.convert("RGB").save(path, quality=93)
    print(path)
    return path


STD = [("Naruto", 100), ("Characters", 92), ("React", 92), ("To", 84), ("Aizen", 118)]


def scene(n, slug, tint, az_color, chars, lines=STD, fnt=CREEP, tcenter=None,
          az_h=560, az_side="left", **tkw):
    """chars: (name, height, neon color[, flip][, x]) anchored to the bottom edge.
    Without explicit x, reactors are stacked from the edge opposite Aizen, the
    one nearest the title in front (like the reference thumbnail)."""
    img = background(tint)
    az = aizen(az_h)
    if az_side == "left":
        soft_glow(img, az, (-130, H - az.height + 30), az_color, 40, 3)
        az_edge = az.width - 130
    elif az_side == "right":
        az = az.transpose(Image.FLIP_LEFT_RIGHT)
        soft_glow(img, az, (W - az.width + 130, H - az.height + 30), az_color, 40, 3)
        az_edge = W - az.width + 130
    else:
        soft_glow(img, az, (W // 2 - az.width // 2, -30), az_color, 45, 3)
        az_edge = None
    ims = [(char(c[0], c[1], flip=len(c) > 3 and c[3] is True), c[2], c[4] if len(c) > 4 else None) for c in chars]
    placed = []
    if az_side in ("left", "right") and all(x is None for _, _, x in ims):
        edge = W + 10 if az_side == "left" else -10
        for im, color, _ in reversed(ims):  # far edge first
            x = edge - im.width if az_side == "left" else edge
            placed.insert(0, (im, color, x))
            edge = x + im.width * 0.28 if az_side == "left" else x + im.width - im.width * 0.28
        for im, color, x in reversed(placed):
            neon(img, im, (int(x), H - im.height + 8), color)
        inner = placed[0][2] if az_side == "left" else placed[0][2] + placed[0][0].width
        if tcenter is None:
            tcenter = ((az_edge + inner) // 2, 350)
    else:
        for im, color, x in ims:
            neon(img, im, (x, H - im.height + 8), color)
    title(img, lines, tcenter or (640, 350), fnt, **tkw)
    return save(img, n, slug)


def n01():  # closest to the reference: villain left red, two girls right pink/blue
    return scene(1, "red_pink_blue", "#200008", "#ff1a1a",
                 [("sakura", 540, "#FF3FD2"), ("hinata", 500, "#2E9BFF")])


def n02():
    return scene(2, "purple_kakashi_sasuke", "#12002a", "#9D4EDD",
                 [("kakashi", 740, "#E8E8FF"), ("sasuke", 380, "#33A1FF")],
                 lines=[("Naruto", 62), ("Characters", 48), ("React To", 56), ("Aizen", 84)], fnt=NOSIF,
                 fill="#C77DFF", stroke="#1a0033", glow="#8a2be2")


def n03():  # Aizen centered on top, one reactor each side
    return scene(3, "aizen_center_gaara_ino", "#1a0000", "#ff2020",
                 [("gaara", 540, "#FF3B3B", False, -10), ("ino", 560, "#FFD60A", False, 1280 - 330)],
                 lines=[("Naruto Characters", 88), ("React To Aizen", 104)], tcenter=(640, 610),
                 az_h=470, az_side="center")


def n04():
    return scene(4, "naruto_girls_pink_yellow", "#240018", "#ff1a1a",
                 [("sakura", 560, "#FF3FD2"), ("ino", 520, "#FFD60A")],
                 lines=[("Naruto", 104), ("Girls", 104), ("React", 96), ("To", 84), ("Aizen", 120)],
                 fill="#FF2A6D", stroke="#300010", glow="#ff007f")


def n05():
    return scene(5, "gaara_sasuke_butcher", "#050018", "#8A2BE2",
                 [("gaara", 540, "#FF2E2E", True), ("sasuke", 400, "#2E9BFF")],
                 fnt=BUTCH, lines=[("Naruto", 84), ("Characters", 68), ("React", 80), ("To", 72), ("Aizen", 100)])


def n06():
    return scene(6, "green_lee_kiba", "#001a0a", "#ff1a1a",
                 [("lee", 580, "#2DFF7A"), ("kiba", 380, "#FFFFFF")],
                 fill="#39FF14", stroke="#002200", glow="#00ff44")


def n07():  # one big reaction
    return scene(7, "hinata_solo_lavender", "#10001f", "#ff1a1a",
                 [("hinata", 680, "#B28DFF")],
                 lines=[("Hinata", 110), ("Reacts", 100), ("To", 88), ("Aizen", 124)])


def n08():  # whole squad around a central Aizen
    return scene(8, "full_squad", "#1a0008", "#ff1a1a",
                 [("kakashi", 520, "#DDE6FF", False, -30), ("ino", 430, "#FFD60A", False, 170),
                  ("lee", 330, "#2DFF7A", False, 40), ("gaara", 430, "#FF3B3B", False, 900),
                  ("sakura", 470, "#FF3FD2", False, 1010), ("sasuke", 280, "#33A1FF", False, 960)],
                 lines=[("Naruto Characters", 78), ("React To Aizen", 92)], tcenter=(640, 610),
                 az_h=450, az_side="center")


def n09():  # mirrored layout: Aizen right
    return scene(9, "mirrored_sakura_sasuke", "#200008", "#ff1a1a",
                 [("sakura", 540, "#FF3FD2"), ("sasuke", 420, "#2E9BFF")], az_side="right")


def n10():
    return scene(10, "fire_kakashi_gaara", "#1f0800", "#FF6A00",
                 [("kakashi", 740, "#FF8C00", True), ("gaara", 500, "#FF2E2E")],
                 fnt=METAL, fill="#FF7A00", stroke="#2a0a00", glow="#ff4500")


def contact_sheet(paths):
    tw, th = 640, 360
    sheet = Image.new("RGB", (tw * 2 + 30, (th + 10) * 5 + 10), "#111111")
    for i, p in enumerate(paths):
        sheet.paste(Image.open(p).resize((tw, th), Image.LANCZOS),
                    (10 + (i % 2) * (tw + 10), 10 + (i // 2) * (th + 10)))
    out = os.path.join(OUT, "00_all_neon.jpg")
    sheet.save(out, quality=90)
    print(out)


if __name__ == "__main__":
    contact_sheet([f() for f in (n01, n02, n03, n04, n05, n06, n07, n08, n09, n10)])
