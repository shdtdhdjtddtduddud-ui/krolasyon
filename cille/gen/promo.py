"""CurseForge promo images: logo, banner and one card per Cille, from the real model + texture data."""
import sys, os, importlib, math
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
from core import render

OUT = os.path.join(os.path.dirname(__file__), '..', 'curseforge')
BOLD = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
REG = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
KEY = (255, 0, 255)


def cut(mod, anim=None, t=0.0, yaw=-35, pitch=8, size=900):
    K = importlib.import_module(mod)
    K.M.pack()
    tex, glow = K.P.run().images()
    pose = K.A[anim].sample(t) if anim else None
    im = render(K.M, tex, pose, yaw=yaw, pitch=pitch, size=size, bg=KEY, extra_glow=glow).convert('RGBA')
    a = np.asarray(im).copy()
    m = (a[..., 0] == 255) & (a[..., 1] == 0) & (a[..., 2] == 255)
    a[m, 3] = 0
    im = Image.fromarray(a)
    return im.crop(im.getbbox())


def sky(w, h, top, bottom, ground=None):
    yy = np.linspace(0, 1, h)[:, None, None]
    c = np.array(top, np.float32) * (1 - yy) + np.array(bottom, np.float32) * yy
    img = np.repeat(c, w, axis=1)
    if ground:
        g0 = int(h * 0.78)
        img[g0:] = np.array(ground, np.float32) * (1 - 0.25 * np.linspace(0, 1, h - g0)[:, None, None])
    return Image.fromarray(img.astype(np.uint8)).convert('RGBA')


PAD = 48


def shadow(img, off=10, blur=14, alpha=150):
    # padded canvas so the blur is not clipped into a visible box at the image edges
    a = img.split()[3].point(lambda v: alpha if v > 0 else 0)
    sh = Image.new('RGBA', (img.width + 2 * PAD, img.height + 2 * PAD), (0, 0, 0, 0))
    m = Image.new('L', sh.size, 0)
    m.paste(a, (PAD, PAD))
    sh.putalpha(m)
    return sh.filter(ImageFilter.GaussianBlur(blur))


def paste(bg, img, cx, bottom, height, maxw=None):
    s = height / img.height
    w = max(1, int(img.width * s))
    if maxw and w > maxw:
        s = maxw / img.width
        w = int(maxw)
        height = img.height * s
    im = img.resize((w, max(1, int(height))), Image.LANCZOS)
    a = np.asarray(im).copy()
    a[..., 3] = np.where(a[..., 3] < 90, 0, 255)          # crisp blocky edges, no resampling halo
    im = Image.fromarray(a)
    x, y = int(cx - im.width / 2), int(bottom - im.height)
    sh = shadow(im)
    bg.alpha_composite(sh, (x + 12 - PAD, y + 14 - PAD)) if x + 12 - PAD >= 0 and y + 14 - PAD >= 0 else bg.paste(sh, (x + 12 - PAD, y + 14 - PAD), sh)
    bg.alpha_composite(im, (x, y))


def title(draw, text, xy, size, fill=(255, 244, 214), stroke=(40, 24, 10), anchor='mm', sw=None):
    f = ImageFont.truetype(BOLD, size)
    draw.text(xy, text, font=f, fill=fill, stroke_width=sw or max(2, size // 14), stroke_fill=stroke, anchor=anchor)


CILLES = [
    ('kabkaba', 'Kabkaba', 'Karanlık Cille', None, 0, -35, ((40, 34, 70), (150, 120, 170), (120, 96, 70)),
     ['Kara Delik', 'Işınlanma', 'Enerji Emme', 'Saldırı Yansıtma', 'Gölge Saldırısı', 'Güçlü Savunma', 'Yer Sarsma']),
    ('behmud', 'Behmud', 'Uzay Balinası', 'WALK', 0.4, -40, ((10, 30, 80), (60, 140, 210), None),
     ['Buz Işını', 'Su Sarmalı', 'Ses Dalgası', 'Patlayan Balık', 'Torpido Ateşi', 'Hızlı Saldırı', 'Hortum', 'Buz Kalkanı',
      'Hızlı Savunma', 'Erkemas Kıskacı', 'Mühürlü Güç', 'Uzay Gücü']),
    ('sarkan', 'Şarkan', 'Kum ve Kaya Savaşçısı', None, 0, -30, ((150, 110, 60), (236, 196, 130), (190, 150, 96)),
     ['Kum Girdabı', 'Atlas Saldırısı', 'Deveran Saldırısı', 'Çöl Fırtınası', 'Kum Denizi', 'Kum Kalkanı', 'Kaya Fırlatma', 'Kum Duvarları']),
    ('nirumend', 'Nirumend', 'Dev Toprak Mamutu', None, 0, -35, ((90, 150, 210), (200, 225, 240), (176, 150, 110)),
     ['Bumehen Saldırısı', 'Ajirak Saldırısı', 'Kemik Kafa Darbesi', 'Sert Kaya Hamlesi', 'Elektrik Kalkanı', 'Toz Bulutu', 'Yer Tepme']),
]

renders = {}
for key, name, sub, anim, t, yaw, cols, abil in CILLES:
    renders[key] = cut(key, anim, t, yaw=yaw, size=1400)

# ---------------- per-Cille cards (1280x720)
for key, name, sub, anim, t, yaw, (top, bot, ground), abil in CILLES:
    W, Hh = 1280, 720
    bg = sky(W, Hh, top, bot, ground)
    paste(bg, renders[key], 370, 660 if key != 'behmud' else 520, 560 if key != 'behmud' else 380, maxw=680)
    d = ImageDraw.Draw(bg)
    title(d, name.upper(), (980, 110), 76)
    title(d, sub, (980, 178), 30, fill=(240, 240, 240), stroke=(20, 20, 30), sw=2)
    f = ImageFont.truetype(BOLD, 24 if len(abil) <= 8 else 21)
    y = 230
    step = 52 if len(abil) <= 8 else 38
    panel = Image.new('RGBA', (500, step * len(abil) + 30), (10, 10, 20, 150))
    bg.alpha_composite(panel, (730, y - 15))
    for i, a in enumerate(abil):
        d.text((760, y + i * step), f'{i + 1}. {a}', font=f, fill=(255, 236, 190), stroke_width=2, stroke_fill=(0, 0, 0))
    bg.convert('RGB').save(os.path.join(OUT, f'kart_{key}.png'))

# ---------------- banner (1920x1080)
bg = sky(1920, 1080, (26, 40, 90), (230, 170, 110), (150, 118, 80))
paste(bg, renders['nirumend'], 1450, 1000, 640)
paste(bg, renders['kabkaba'], 470, 1010, 560)
paste(bg, renders['sarkan'], 960, 1030, 660)
paste(bg, renders['behmud'], 960, 470, 300, maxw=760)
d = ImageDraw.Draw(bg)
title(d, 'CİLLE', (960, 120), 150)
title(d, 'Marble Battles', (960, 236), 50, fill=(255, 255, 255), stroke=(20, 20, 40), sw=3)
title(d, 'Minecraft 1.20.1 Forge', (960, 1050), 34, fill=(255, 255, 255), stroke=(20, 20, 40), sw=3)
bg.convert('RGB').save(os.path.join(OUT, 'kapak_banner.png'))

# ---------------- logo (512x512): glass marble with the four Cilles
S = 512
logo = Image.new('RGBA', (S, S), (0, 0, 0, 0))
yy, xx = np.mgrid[0:S, 0:S]
c = S / 2
r = np.hypot(xx - c, yy - c) / (S * 0.48)
nz = np.sqrt(np.clip(1 - r ** 2, 0, 1))
dd = (-(xx - c) * 0.5 - (yy - c) * 0.6) / (S * 0.48) + nz * 0.62
t = np.clip(0.3 + 0.4 * (dd + 1) / 2, 0, 1)
cols = np.array([(10, 20, 60), (30, 60, 150), (80, 130, 220), (170, 200, 250)], np.float32)
ti = t * 3
i0 = np.minimum(ti.astype(int), 2)
f = (ti - i0)[..., None]
col = cols[i0] * (1 - f) + cols[i0 + 1] * f
arr = np.zeros((S, S, 4), np.uint8)
arr[..., :3] = col.astype(np.uint8)
arr[..., 3] = ((r < 1) * 255).astype(np.uint8)
ball = Image.fromarray(arr)
logo.alpha_composite(ball)
inner = Image.new('RGBA', (S, S), (0, 0, 0, 0))
paste(inner, renders['nirumend'], 340, 380, 200)
paste(inner, renders['kabkaba'], 170, 390, 170)
paste(inner, renders['sarkan'], 256, 410, 220)
paste(inner, renders['behmud'], 256, 200, 100, maxw=300)
mask = Image.fromarray(((r < 0.97) * 255).astype(np.uint8))
inner.putalpha(Image.fromarray(np.minimum(np.asarray(inner.split()[3]), np.asarray(mask))))
logo.alpha_composite(inner)
spec = Image.new('RGBA', (S, S), (0, 0, 0, 0))
ImageDraw.Draw(spec).ellipse([330, 62, 392, 100], fill=(255, 255, 255, 120))
logo.alpha_composite(spec.filter(ImageFilter.GaussianBlur(6)))
ring = Image.new('RGBA', (S, S), (0, 0, 0, 0))
ImageDraw.Draw(ring).ellipse([6, 6, S - 7, S - 7], outline=(20, 30, 60, 255), width=8)
logo.alpha_composite(ring)
d = ImageDraw.Draw(logo)
title(d, 'CİLLE', (S // 2, 462), 72)
logo.save(os.path.join(OUT, 'logo.png'))
print('done')
