"""Neon-outline variants that keep the original background and Aizen's photo
untouched: every character stays where it is and gets a neon outline, Aizen's
framed photo gets a glowing frame, and the old title is replaced by a dripping
horror title. Usage: python make_thumbnails_keep.py FONT_DIR"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

from make_thumbnails_neon import (BUTCH, CREEP, EATER, METAL, NOSIF, H, W, neon, rgba, title)

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "output_keep")
os.makedirs(OUT, exist_ok=True)

SRC = Image.open(os.path.join(HERE, "original.jpg")).convert("RGBA")
OFF = json.load(open(os.path.join(HERE, "cutouts", "offsets.json")))
FRAME = (443, 157, 834, 560)  # white frame around Aizen's photo
BACK = ["kakashi", "ino", "gaara", "sakura"]  # back row first so the front row overlaps it
FRONT = ["lee", "kiba", "sasuke", "hinata"]
# colors: one per character, in BACK + FRONT order
PALETTES = {
    "reference": ["#FF3FD2", "#2E9BFF", "#FF3FD2", "#2E9BFF", "#2E9BFF", "#FF3FD2", "#2E9BFF", "#FF3FD2"],
    "character": ["#DDE6FF", "#FFD60A", "#FF3B3B", "#FF6FD8", "#2DFF7A", "#FFFFFF", "#33A1FF", "#B28DFF"],
    "red": ["#FF1A1A"] * 8,
    "purple": ["#B84DFF"] * 8,
    "cyan": ["#00E5FF"] * 8,
    "fire": ["#FF8C00", "#FF3B3B", "#FF8C00", "#FF3B3B", "#FF3B3B", "#FF8C00", "#FF3B3B", "#FF8C00"],
    "toxic": ["#39FF14"] * 8,
}


def cutout(name):
    return Image.open(os.path.join(HERE, "cutouts", f"{name}.png")).convert("RGBA")


def hide_old_title(img, strength=255):
    """Soft dark patch over the old "NARUTO / REACTS TO AIZEN" title."""
    m = Image.new("L", (W, H), 0)
    ImageDraw.Draw(m).rounded_rectangle((318, 462, 962, 740), 50, fill=strength)
    m = m.filter(ImageFilter.GaussianBlur(12))
    dark = Image.new("RGBA", (W, H), (8, 0, 6, 255))
    dark.putalpha(m)
    img.alpha_composite(dark)


def frame_glow(img, color, width=6, radius=22):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(layer).rectangle(FRAME, outline=rgba(color), width=width + 8)
    glow = layer.filter(ImageFilter.GaussianBlur(radius))
    for _ in range(3):
        img.alpha_composite(glow)
    d = ImageDraw.Draw(img)
    d.rectangle(FRAME, outline=rgba(color), width=width)
    d.rectangle((FRAME[0] + 2, FRAME[1] + 2, FRAME[2] - 2, FRAME[3] - 2), outline=(255, 255, 255, 220), width=2)
    # keep the photo itself exactly as in the original
    photo = SRC.crop((FRAME[0] + width, FRAME[1] + width, FRAME[2] - width + 1, 505))
    img.alpha_composite(photo, (FRAME[0] + width, FRAME[1] + width))


def make(n, slug, palette, frame, lines, fnt, **tkw):
    img = SRC.copy()
    frame_glow(img, frame)
    hide_old_title(img)
    colors = PALETTES[palette]
    for name, color in zip(BACK + FRONT, colors):
        neon(img, cutout(name), tuple(OFF[name]), color, line=4, glow=16)
    title(img, lines, (640, 612), fnt, **tkw)
    path = os.path.join(OUT, f"{n:02d}_{slug}.jpg")
    img.convert("RGB").save(path, quality=93)
    print(path)
    return path


TWO = [("Naruto Characters", 78), ("React To Aizen", 92)]
VARIANTS = [
    ("reference_red", "reference", "#FF1A1A", TWO, CREEP, {}),
    ("character_colors", "character", "#FF1A1A", TWO, CREEP, {}),
    ("all_red", "red", "#FF1A1A", TWO, BUTCH, {"fill": "#E3121B"}),
    ("purple_nosifer", "purple", "#B84DFF", [("Naruto Characters", 50), ("React To Aizen", 64)], NOSIF,
     {"fill": "#C77DFF", "stroke": "#1a0033", "glow": "#8a2be2"}),
    ("cyan_ice", "cyan", "#FF1A1A", TWO, CREEP, {"fill": "#7DF9FF", "stroke": "#001a22", "glow": "#00bfff"}),
    ("fire_metal", "fire", "#FF6A00", TWO, METAL, {"fill": "#FF7A00", "stroke": "#2a0a00", "glow": "#ff4500"}),
    ("toxic_green", "toxic", "#39FF14", TWO, CREEP, {"fill": "#39FF14", "stroke": "#002200", "glow": "#00ff44"}),
    ("pink_blue_pink_title", "reference", "#FF3FD2", TWO, CREEP, {"fill": "#FF2A6D", "stroke": "#300010", "glow": "#ff007f"}),
    ("gold_frame_eater", "character", "#FFD60A", [("Naruto Characters", 60), ("React To Aizen", 72)], EATER,
     {"fill": "#E3121B"}),
    ("white_red_title", "red", "#FFFFFF", [("Naruto Reacts", 84), ("To Aizen", 96)], CREEP,
     {"fill": "#FFFFFF", "stroke": "#300000", "glow": "#ff0000"}),
]

if __name__ == "__main__":
    paths = [make(i + 1, *v[:5], **v[5]) for i, v in enumerate(VARIANTS)]
    tw, th = 640, 360
    sheet = Image.new("RGB", (tw * 2 + 30, (th + 10) * 5 + 10), "#111111")
    for i, p in enumerate(paths):
        sheet.paste(Image.open(p).resize((tw, th), Image.LANCZOS), (10 + (i % 2) * (tw + 10), 10 + (i // 2) * (th + 10)))
    sheet.save(os.path.join(OUT, "00_all_keep.jpg"), quality=90)
