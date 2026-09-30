"""Generate 10 redesigned "Naruto Reacts to Aizen" YouTube thumbnails (1280x720)
from the original thumbnail. Requires Pillow + numpy and the Anton, Bangers and
Luckiest Guy fonts (Google Fonts) in FONT_DIR."""
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont, ImageOps

HERE = os.path.dirname(os.path.abspath(__file__))
FONT_DIR = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "fonts")
OUT = os.path.join(HERE, "output")
os.makedirs(OUT, exist_ok=True)
W, H = 1280, 720

SRC = Image.open(os.path.join(HERE, "original.jpg")).convert("RGB")
SRC = ImageEnhance.Color(SRC).enhance(1.25)
SRC = ImageEnhance.Contrast(SRC).enhance(1.1)

BOX = {
    "aizen": (452, 165, 828, 482),
    "aizen_face": (500, 190, 790, 480),
    "kakashi": (0, 0, 225, 330),
    "ino": (215, 15, 420, 330),
    "lee": (15, 320, 220, 520),
    "kiba": (195, 295, 430, 505),
    "gaara": (835, 20, 1045, 320),
    "sakura": (1045, 25, 1270, 330),
    "sasuke": (840, 320, 1030, 515),
    "hinata": (1035, 315, 1280, 590),
}
FACE = {
    "kakashi": (45, 70, 205, 230), "ino": (240, 60, 400, 220),
    "lee": (40, 335, 210, 505), "kiba": (215, 320, 385, 490),
    "gaara": (845, 55, 1015, 225), "sakura": (1075, 70, 1245, 240),
    "sasuke": (850, 340, 1015, 505), "hinata": (1075, 335, 1245, 505),
}

YELLOW, PURPLE, RED, WHITE, BLACK = "#FFD60A", "#7B2CFF", "#FF2E2E", "#FFFFFF", "#000000"


def font(name, size):
    return ImageFont.truetype(os.path.join(FONT_DIR, name), size)


ANTON = lambda s: font("Anton-Regular.ttf", s)
BANG = lambda s: font("Bangers-Regular.ttf", s)
LUCK = lambda s: font("LuckiestGuy-Regular.ttf", s)


def crop(name, w=None, h=None, faces=False):
    im = SRC.crop((FACE if faces else BOX)[name])
    if w and h:
        im = ImageOps.fit(im, (w, h), Image.LANCZOS)
    elif w:
        im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    elif h:
        im = im.resize((round(im.width * h / im.height), h), Image.LANCZOS)
    return im


def gradient(c1, c2, size=(W, H), radial=False, center=(0.5, 0.5)):
    c1, c2 = Image.new("RGB", (1, 1), c1).getpixel((0, 0)), Image.new("RGB", (1, 1), c2).getpixel((0, 0))
    w, h = size
    if radial:
        mask = Image.radial_gradient("L").resize((int(w * 1.6), int(w * 1.6)))
        cx, cy = int(center[0] * w), int(center[1] * h)
        m = Image.new("L", size, 255)
        m.paste(mask, (cx - mask.width // 2, cy - mask.height // 2))
    else:
        m = Image.linear_gradient("L").resize(size)
    return Image.composite(Image.new("RGB", size, c2), Image.new("RGB", size, c1), m)


def rays(img, center, color, n=24, alpha=70, spread=0.5):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = center
    r = 2000
    for i in range(n):
        a = 2 * math.pi * i / n
        b = a + 2 * math.pi / n * spread
        d.polygon([(cx, cy), (cx + r * math.cos(a), cy + r * math.sin(a)),
                   (cx + r * math.cos(b), cy + r * math.sin(b))], fill=_rgba(color, alpha))
    img.alpha_composite(layer) if img.mode == "RGBA" else img.paste(layer, (0, 0), layer)


def _rgba(c, a=255):
    r, g, b = Image.new("RGB", (1, 1), c).getpixel((0, 0))
    return (r, g, b, a)


def glow(img, box_img, pos, color, radius=30, strength=3):
    """Paste box_img at pos with a colored outer glow."""
    pad = radius * 3
    g = Image.new("RGBA", (box_img.width + 2 * pad, box_img.height + 2 * pad), (0, 0, 0, 0))
    alpha = box_img.split()[3] if box_img.mode == "RGBA" else Image.new("L", box_img.size, 255)
    solid = Image.new("RGBA", box_img.size, _rgba(color))
    g.paste(solid, (pad, pad), alpha)
    g = g.filter(ImageFilter.GaussianBlur(radius))
    for _ in range(strength):
        img.alpha_composite(g, (pos[0] - pad, pos[1] - pad))
    img.alpha_composite(box_img.convert("RGBA"), pos)


def sticker(im, border=8, color=WHITE, radius=28, shadow=True, circle=False):
    """Rounded (or circular) panel with a thick outline and drop shadow."""
    w, h = im.size
    mask = Image.new("L", (w, h), 0)
    md = ImageDraw.Draw(mask)
    if circle:
        md.ellipse((0, 0, w - 1, h - 1), fill=255)
    else:
        md.rounded_rectangle((0, 0, w - 1, h - 1), radius, fill=255)
    out = Image.new("RGBA", (w + 2 * border + 20, h + 2 * border + 20), (0, 0, 0, 0))
    ow, oh = w + 2 * border, h + 2 * border
    bm = Image.new("L", (ow, oh), 0)
    bd = ImageDraw.Draw(bm)
    if circle:
        bd.ellipse((0, 0, ow - 1, oh - 1), fill=255)
    else:
        bd.rounded_rectangle((0, 0, ow - 1, oh - 1), radius + border, fill=255)
    if shadow:
        sh = Image.new("RGBA", out.size, (0, 0, 0, 0))
        sh.paste(Image.new("RGBA", (ow, oh), (0, 0, 0, 170)), (14, 14), bm)
        out.alpha_composite(sh.filter(ImageFilter.GaussianBlur(6)))
    out.paste(Image.new("RGBA", (ow, oh), _rgba(color)), (0, 0), bm)
    out.paste(im.convert("RGBA"), (border, border), mask)
    return out


def text(img, xy, s, fnt, fill=YELLOW, stroke=BLACK, sw=10, anchor="la", shadow=8,
         rotate=0, gradient_to=None):
    """Thick-outlined thumbnail text with drop shadow, optional vertical gradient and tilt."""
    tmp = Image.new("RGBA", (W * 2, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(tmp)
    x0, y0 = 60, 60
    bbox = d.textbbox((x0, y0), s, font=fnt, stroke_width=sw)
    if shadow:
        d.text((x0 + shadow, y0 + shadow), s, font=fnt, fill=(0, 0, 0, 200), stroke_width=sw, stroke_fill=(0, 0, 0, 200))
    d.text((x0, y0), s, font=fnt, fill=fill, stroke_width=sw, stroke_fill=stroke)
    if gradient_to:
        inner = Image.new("L", tmp.size, 0)
        ImageDraw.Draw(inner).text((x0, y0), s, font=fnt, fill=255)
        g = gradient(fill, gradient_to, (tmp.width, bbox[3] - bbox[1]))
        full = Image.new("RGB", tmp.size)
        full.paste(g, (0, bbox[1]))
        tmp.paste(full, (0, 0), inner)
    tmp = tmp.crop((bbox[0] - 4, bbox[1] - 4, bbox[2] + shadow + 4, bbox[3] + shadow + 4))
    if rotate:
        tmp = tmp.rotate(rotate, expand=True, resample=Image.BICUBIC)
    x, y = xy
    if anchor[0] == "m":
        x -= tmp.width // 2
    elif anchor[0] == "r":
        x -= tmp.width
    if anchor[1] == "m":
        y -= tmp.height // 2
    elif anchor[1] == "b":
        y -= tmp.height
    img.alpha_composite(tmp, (int(x), int(y)))
    return (int(x), int(y), int(x) + tmp.width, int(y) + tmp.height)


def banner(img, xy, s, fnt, bg=PURPLE, fg=WHITE, pad=(28, 10), skew=18, anchor="mm", outline=WHITE):
    d = ImageDraw.Draw(img)
    b = d.textbbox((0, 0), s, font=fnt)
    tw, th = b[2] - b[0], b[3] - b[1]
    w, h = tw + 2 * pad[0], th + 2 * pad[1]
    x, y = xy
    if anchor == "mm":
        x, y = x - w // 2, y - h // 2
    poly = [(x + skew, y), (x + w + skew, y), (x + w - skew, y + h), (x - skew, y + h)]
    d.polygon([(px + 6, py + 6) for px, py in poly], fill=(0, 0, 0, 180))
    d.polygon(poly, fill=bg, outline=outline, width=5)
    d.text((x + pad[0] - b[0], y + pad[1] - b[1]), s, font=fnt, fill=fg)


def shock_marks(img, xy, size=90, rot=-12):
    text(img, xy, "!?", BANG(size), fill=RED, stroke=WHITE, sw=7, shadow=4, rotate=rot)


def vignette(img, strength=200):
    m = Image.radial_gradient("L").resize((int(W * 1.5), int(H * 1.9)))
    full = Image.new("L", (W, H), 255)
    full.paste(m, ((W - m.width) // 2, (H - m.height) // 2))
    full = full.point(lambda v: max(0, min(255, int((v - 90) * 1.6))) * strength // 255)
    img.alpha_composite(Image.merge("RGBA", [Image.new("L", (W, H), 0)] * 3 + [full]))


def lightning(d, p0, p1, color, width=10, jag=40, seed=1):
    rnd = random.Random(seed)
    pts = [p0]
    n = 9
    for i in range(1, n):
        t = i / n
        x = p0[0] + (p1[0] - p0[0]) * t + rnd.randint(-jag, jag)
        y = p0[1] + (p1[1] - p0[1]) * t
        pts.append((x, y))
    pts.append(p1)
    d.line(pts, fill=WHITE, width=width + 10, joint="curve")
    d.line(pts, fill=color, width=width, joint="curve")
    return pts


def canvas(bg):
    return bg.convert("RGBA") if isinstance(bg, Image.Image) else Image.new("RGBA", (W, H), bg)


def save(img, n, slug):
    path = os.path.join(OUT, f"{n:02d}_{slug}.jpg")
    img.convert("RGB").save(path, quality=93)
    print(path)
    return path


# ---------------------------------------------------------------- designs

def t01_vs_split():
    img = canvas(gradient("#1A0633", "#000000", radial=True, center=(0.75, 0.45)))
    left = gradient("#FF8A00", "#C1121F", (W, H))
    lm = Image.new("L", (W, H), 0)
    ImageDraw.Draw(lm).polygon([(0, 0), (620, 0), (520, H), (0, H)], fill=255)
    img.paste(left, (0, 0), lm)
    rays(img, (260, 360), WHITE, n=20, alpha=35)
    # Aizen huge on the right
    az = crop("aizen", h=720)
    img.alpha_composite(az.convert("RGBA"), (W - az.width + 60, 0))
    # the reacting group on the left
    for name, pos, h in [("kakashi", (10, 20), 330), ("ino", (250, 40), 310), ("lee", (20, 380), 300), ("kiba", (270, 360), 300)]:
        img.alpha_composite(sticker(crop(name, h=h), border=6), pos)
    shock_marks(img, (430, 20), 110, -10)
    d = ImageDraw.Draw(img)
    lightning(d, (620, -10), (520, H + 10), YELLOW, width=14, jag=45)
    text(img, (W - 40, H - 20), "AIZEN?!", ANTON(190), fill=YELLOW, sw=12, anchor="rb", rotate=4)
    banner(img, (330, 690 - 30), "NARUTO REACTS", BANG(56), bg=PURPLE)
    return save(img, 1, "vs_split")


def t02_god_aura():
    img = canvas(gradient("#B04DFF", "#0B0014", radial=True, center=(0.5, 0.42)))
    rays(img, (640, 300), "#E6C7FF", n=28, alpha=55)
    face = crop("aizen", w=560)
    glow(img, sticker(face, border=0, radius=40, shadow=False), (640 - 280, 60), "#C77DFF", radius=40, strength=4)
    for name, pos, rot in [("sakura", (20, 330), 6), ("sasuke", (930, 330), -6)]:
        s = sticker(crop(name, 300, 340), border=8, color=YELLOW).rotate(rot, expand=True, resample=Image.BICUBIC)
        img.alpha_composite(s, pos)
    shock_marks(img, (240, 300), 100, -14)
    shock_marks(img, (930, 290), 100, 12)
    text(img, (640, 10), "HE'S A GOD?!", ANTON(150), fill=WHITE, stroke="#2A0055", sw=12, anchor="ma")
    banner(img, (640, 660), "NARUTO REACTS TO AIZEN", BANG(52), bg=YELLOW, fg=BLACK, outline=BLACK)
    vignette(img, 160)
    return save(img, 2, "god_aura")


def t03_shattered_illusion():
    base = SRC.copy().filter(ImageFilter.GaussianBlur(10))
    base = ImageEnhance.Brightness(base).enhance(0.35)
    img = canvas(base)
    img.alpha_composite(Image.new("RGBA", (W, H), (60, 0, 110, 120)))
    az = crop("aizen", h=560)
    ax, ay = 640 - az.width // 2, 70
    rnd = random.Random(7)
    cx, cy = 640, 330
    # shards: pie slices of Aizen, each slightly displaced
    n = 11
    angles = sorted(rnd.uniform(0, 2 * math.pi) for _ in range(n))
    full = Image.new("RGBA", (W, H))
    full.paste(az, (ax, ay))
    for i in range(n):
        a, b = angles[i], angles[(i + 1) % n] + (2 * math.pi if i == n - 1 else 0)
        m = Image.new("L", (W, H), 0)
        ImageDraw.Draw(m).polygon([(cx, cy), (cx + 1500 * math.cos(a), cy + 1500 * math.sin(a)),
                                   (cx + 1500 * math.cos((a + b) / 2), cy + 1500 * math.sin((a + b) / 2)),
                                   (cx + 1500 * math.cos(b), cy + 1500 * math.sin(b))], fill=255)
        piece = Image.new("RGBA", (W, H))
        piece.paste(full, (0, 0), Image.composite(full.split()[3], Image.new("L", (W, H), 0), m))
        off = rnd.randint(6, 22)
        mid = (a + b) / 2
        img.alpha_composite(piece, (int(off * math.cos(mid)), int(off * math.sin(mid))))
    d = ImageDraw.Draw(img)
    for i in range(n):
        a = angles[i]
        pts = [(cx, cy)]
        r = 0
        while r < 900:
            r += rnd.randint(60, 130)
            aa = a + rnd.uniform(-0.05, 0.05)
            pts.append((cx + r * math.cos(aa), cy + r * math.sin(aa)))
        d.line(pts, fill=(200, 250, 255, 255), width=4)
    for r in (40, 110):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(200, 250, 255, 220), width=3)
    for name, pos in [("hinata", (30, 380)), ("kakashi", (1010, 40))]:
        s = sticker(crop(name, faces=True).resize((220, 220)), border=7, color="#7FF6FF", circle=True)
        img.alpha_composite(s, pos)
    text(img, (640, 700), "IT WAS AN ILLUSION?!", ANTON(118), fill=YELLOW, sw=11, anchor="mb")
    banner(img, (230, 60), "NARUTO REACTS", BANG(50), bg="#00B4D8", fg=BLACK, outline=WHITE)
    return save(img, 3, "shattered_illusion")


def t04_too_strong():
    img = canvas(gradient("#2B0A3D", "#07010D"))
    rays(img, (640, -50), "#9D4EDD", n=16, alpha=45, spread=0.35)
    az = crop("aizen", w=900)
    fade = Image.linear_gradient("L").resize(az.size)
    fade = fade.point(lambda v: 255 if v < 150 else max(0, 255 - (v - 150) * 3))
    az_rgba = az.convert("RGBA")
    az_rgba.putalpha(fade)
    glow(img, az_rgba, (190, -40), "#9D4EDD", 40, 2)
    d = ImageDraw.Draw(img)
    lightning(d, (90, 0), (230, 360), "#C77DFF", 7, 35, seed=3)
    lightning(d, (1190, 0), (1050, 330), "#C77DFF", 7, 35, seed=5)
    # the whole squad, small, bottom row looking up
    names = ["kakashi", "ino", "lee", "kiba", "gaara", "sakura", "sasuke", "hinata"]
    x = 20
    for i, name in enumerate(names):
        s = sticker(crop(name, faces=True).resize((138, 138)), border=5, circle=True)
        img.alpha_composite(s, (x, 560 + (i % 2) * -20))
        x += 156
    text(img, (640, 555), "TOO STRONG?!", ANTON(170), fill=WHITE, stroke=RED, sw=12, anchor="mb")
    vignette(img, 120)
    return save(img, 4, "too_strong")


def t05_minimal_who():
    img = canvas(gradient("#8E2DE2", "#3A0CA3"))
    rays(img, (360, 420), WHITE, n=18, alpha=22)
    sak = crop("sakura", 470, 640)
    img.alpha_composite(sticker(sak, border=10, color=YELLOW, radius=36), (30, 60))
    shock_marks(img, (350, 40), 150, -12)
    az = crop("aizen_face", 470, 470)
    img.alpha_composite(sticker(az, border=10, color=WHITE, circle=True), (760, 60))
    text(img, (1255, 700), "WHO IS HE?!", ANTON(130), fill=YELLOW, sw=12, anchor="rb")
    return save(img, 5, "minimal_who_is_he")


def t06_power_meter():
    img = canvas(ImageEnhance.Brightness(SRC).enhance(0.75))
    d = ImageDraw.Draw(img)
    # cover the old title
    bottom = Image.linear_gradient("L").resize((W, 260)).point(lambda v: min(255, v * 6))
    img.paste(Image.new("RGBA", (W, 260), (10, 0, 20, 255)), (0, 460), bottom)
    # red circle + arrow on Aizen
    d.ellipse((470, 170, 815, 470), outline=RED, width=14)
    d.line([(1010, 80), (840, 200)], fill=RED, width=22)
    d.polygon([(815, 215), (880, 215), (840, 160)], fill=RED)
    # power meter
    mx, my, mw, mh = 1150, 120, 70, 440
    d.rounded_rectangle((mx, my, mx + mw, my + mh), 18, fill=(20, 20, 20, 230), outline=WHITE, width=5)
    for i in range(10):
        c = ["#2DC653", "#80ED99", "#FFD60A", "#FF9F1C", "#FF2E2E"][min(4, i // 2)]
        y = my + mh - 14 - (i + 1) * 41
        d.rounded_rectangle((mx + 10, y, mx + mw - 10, y + 33), 6, fill=c)
    text(img, (mx + mw // 2, my - 20), "MAX", BANG(70), fill=RED, stroke=WHITE, sw=6, anchor="mb", rotate=-10)
    text(img, (mx + mw // 2, my + mh + 6), "POWER", BANG(44), fill=WHITE, sw=6, anchor="ma")
    text(img, (40, 700), "NARUTO REACTS TO", ANTON(92), fill=WHITE, sw=9, anchor="lb")
    text(img, (910, 712), "AIZEN", ANTON(150), fill=RED, stroke=WHITE, sw=10, anchor="lb", rotate=6)
    return save(img, 6, "power_meter")


def t07_sasuke_vs_aizen():
    img = canvas("#000000")
    img.paste(gradient("#00B4FF", "#001233", (W // 2 + 60, H), radial=True, center=(0.4, 0.5)), (0, 0))
    right = gradient("#B388FF", "#1A0033", (W // 2 + 60, H), radial=True, center=(0.6, 0.5))
    m = Image.new("L", (W, H), 0)
    ImageDraw.Draw(m).polygon([(700, 0), (W, 0), (W, H), (580, H)], fill=255)
    full_r = Image.new("RGB", (W, H))
    full_r.paste(right, (W // 2 - 60, 0))
    img.paste(full_r, (0, 0), m)
    sas = crop("sasuke", faces=True).resize((520, 520), Image.LANCZOS)
    glow(img, sticker(sas, border=0, radius=30, shadow=False), (60, 70), "#4CC9F0", 30, 3)
    az = crop("aizen_face", 520, 520)
    glow(img, sticker(az, border=0, radius=30, shadow=False), (700, 70), "#C77DFF", 30, 3)
    d = ImageDraw.Draw(img)
    rnd = random.Random(2)
    for i in range(16):
        a = rnd.uniform(0, 2 * math.pi)
        r = rnd.randint(60, 200)
        d.line([(640, 330), (640 + r * math.cos(a), 330 + r * math.sin(a))], fill=WHITE, width=rnd.randint(3, 8))
    lightning(d, (640, 0), (640, H), "#E0AAFF", 10, 50, seed=9)
    text(img, (640, 710), "SASUKE", ANTON(130), fill=WHITE, sw=11, anchor="rb")
    text(img, (640, 710), " vs ", ANTON(130), fill=RED, sw=11, anchor="lb")
    text(img, (835, 710), "AIZEN?!", ANTON(130), fill=WHITE, sw=11, anchor="lb")
    banner(img, (640, 40), "NARUTO REACTS", BANG(46), bg=YELLOW, fg=BLACK, outline=BLACK)
    return save(img, 7, "sasuke_vs_aizen")


def t08_byakugan_scan():
    img = canvas(gradient("#12001F", "#000000", radial=True, center=(0.7, 0.4)))
    az = crop("aizen", h=640)
    az = ImageEnhance.Color(az).enhance(0.8)
    img.alpha_composite(az.convert("RGBA"), (W - az.width - 30, 40))
    d = ImageDraw.Draw(img)
    ax0 = W - az.width - 30
    # HUD scanlines + brackets
    hud = Image.new("RGBA", (W, H))
    hd = ImageDraw.Draw(hud)
    for y in range(40, 680, 6):
        hd.line([(ax0, y), (W - 30, y)], fill=(255, 0, 60, 45), width=2)
    img.alpha_composite(hud)
    L = 60
    for (x, y, sx, sy) in [(ax0 + 20, 70, 1, 1), (W - 50, 70, -1, 1), (ax0 + 20, 650, 1, -1), (W - 50, 650, -1, -1)]:
        d.line([(x, y), (x + sx * L, y)], fill=RED, width=8)
        d.line([(x, y), (x, y + sy * L)], fill=RED, width=8)
    banner(img, (ax0 + az.width // 2, 360), "ERROR: OVERLOAD", BANG(58), bg=RED, fg=WHITE, outline=BLACK)
    hin = crop("hinata", 470, 560)
    hin = ImageEnhance.Brightness(hin).enhance(1.05)
    img.alpha_composite(sticker(hin, border=9, color="#E0E1FF", radius=30), (20, 30))
    # byakugan veins hint
    for (x0, y0) in [(215, 260), (320, 260)]:
        for k in range(5):
            a = math.pi * (0.9 + 0.2 * k)
            d.line([(x0, y0), (x0 + 45 * math.cos(a) * (1 if x0 < 300 else -1), y0 + 30 * math.sin(a))], fill=(210, 200, 255, 200), width=3)
    shock_marks(img, (400, 30), 100, 10)
    text(img, (30, 700), "SHE SAW HIS POWER...", ANTON(96), fill=WHITE, sw=9, anchor="lb")
    return save(img, 8, "byakugan_scan")


def t09_polished_original():
    img = canvas(SRC.copy())
    # darken the edges and the old title area
    vignette(img, 170)
    bottom = Image.linear_gradient("L").resize((W, 300)).point(lambda v: min(255, v * 4))
    img.paste(Image.new("RGBA", (W, 300), (5, 0, 15, 255)), (0, 420), bottom)
    rays(img, (640, 330), "#FFF3B0", n=22, alpha=28)
    # re-add characters on top of rays so the rays sit behind them
    for name in ["kakashi", "ino", "gaara", "sakura"]:
        b = BOX[name]
        img.alpha_composite(SRC.crop(b).convert("RGBA"), (b[0], b[1]))
    for name in ["lee", "kiba", "sasuke", "hinata"]:
        b = BOX[name]
        piece = SRC.crop(b).convert("RGBA")
        fade = Image.linear_gradient("L").resize(piece.size).point(lambda v: 255 - max(0, (v - 150) * 2))
        piece.putalpha(fade)
        img.alpha_composite(piece, (b[0], b[1]))
    az = crop("aizen", w=470)
    glow(img, sticker(az, border=8, color=WHITE, radius=22, shadow=False), (640 - 243, 50), "#B388FF", 28, 3)
    shock_marks(img, (180, 170), 110, -10)
    shock_marks(img, (1050, 10), 110, 12)
    text(img, (640, 610), "NARUTO", LUCK(160), fill="#FFE066", gradient_to="#FF7B00", sw=12, anchor="mb", shadow=10)
    banner(img, (640, 655), "REACTS TO AIZEN", LUCK(58), bg=PURPLE, fg=WHITE)
    # PART 1 badge
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((1080, 600, 1260, 700), 20, fill=RED, outline=WHITE, width=6)
    text(img, (1170, 650), "PART 1", ANTON(58), fill=WHITE, sw=5, anchor="mm", shadow=3)
    return save(img, 9, "polished_original")


def t10_eight_vs_one():
    img = canvas(gradient("#FF6B00", "#1B0036", radial=True, center=(0.5, 0.45)))
    rays(img, (640, 320), "#FFE066", n=30, alpha=40)
    az = crop("aizen_face", 330, 330)
    glow(img, sticker(az, border=10, color=YELLOW, circle=True, shadow=False), (640 - 175, 140), "#FFD60A", 26, 3)
    names = ["kakashi", "ino", "gaara", "sakura", "hinata", "sasuke", "kiba", "lee"]
    cx, cy, rx, ry = 640, 310, 470, 230
    for i, name in enumerate(names):
        a = math.radians([200, 235, 305, 340, 20, 160, 130, 50][i])
        x, y = cx + rx * math.cos(a), cy + ry * math.sin(a)
        s = sticker(crop(name, faces=True).resize((170, 170)), border=6, circle=True)
        img.alpha_composite(s, (int(x - 92), int(y - 92)))
    text(img, (640, 715), "8 NINJAS vs 1 GOD", ANTON(120), fill=WHITE, sw=11, anchor="mb")
    text(img, (640, 600), "NARUTO REACTS", BANG(58), fill=YELLOW, sw=7, anchor="mb", shadow=4)
    return save(img, 10, "eight_vs_one")


def contact_sheet(paths):
    tw, th = 640, 360
    sheet = Image.new("RGB", (tw * 2 + 30, (th + 50) * 5 + 10), "#111111")
    d = ImageDraw.Draw(sheet)
    for i, p in enumerate(paths):
        im = Image.open(p).resize((tw, th), Image.LANCZOS)
        x, y = 10 + (i % 2) * (tw + 10), 10 + (i // 2) * (th + 50)
        sheet.paste(im, (x, y))
        d.text((x, y + th + 6), os.path.basename(p), font=ANTON(30), fill="white")
    out = os.path.join(OUT, "00_all_thumbnails.jpg")
    sheet.save(out, quality=90)
    print(out)


if __name__ == "__main__":
    paths = [f() for f in (t01_vs_split, t02_god_aura, t03_shattered_illusion, t04_too_strong,
                           t05_minimal_who, t06_power_meter, t07_sasuke_vs_aizen,
                           t08_byakugan_scan, t09_polished_original, t10_eight_vs_one)]
    contact_sheet(paths)
