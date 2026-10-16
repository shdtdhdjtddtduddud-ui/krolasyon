"""Evil Befall extras: launcher + rival item textures and the effect sprites
(peacock.png: dark peacock with its tail fanned out, eyefeather.png: one eye feather, evileye.png: the hypnotic eye)."""
import colorsys, math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade')
ITEM, FX = os.path.join(A, 'textures/item'), os.path.join(A, 'textures/fx')


def hue_shift(img, target_h, sat_mul=1.0):
    a = np.asarray(img.convert('RGBA')).astype(np.float32) / 255
    out = a.copy()
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            h, l, s = colorsys.rgb_to_hls(*a[y, x, :3])
            out[y, x, :3] = colorsys.hls_to_rgb(target_h, l, min(1, s * sat_mul))
    return Image.fromarray((out * 255).astype(np.uint8), 'RGBA')


# launcher: the Variares launcher palette turned pink
hue_shift(Image.open(os.path.join(ITEM, 'variares_launcher.png')), 0.9, 0.9).save(os.path.join(ITEM, 'beffal_launcher.png'))
# rival item: the icon with a violet cast
icon = Image.open(os.path.join(ITEM, 'beffal_icon.png')).convert('RGBA').resize((32, 32), Image.LANCZOS)
ia = np.asarray(icon).astype(np.float32)
ia[..., :3] = ia[..., :3] * 0.6 + np.array([150, 40, 200]) * 0.4
ia[..., 3] = np.where(ia[..., 3] > 110, 255, 0)
Image.fromarray(ia.astype(np.uint8), 'RGBA').save(os.path.join(ITEM, 'rival_beffal.png'))


def feather(d, cx, cy, ang, length, width, s=1.0):
    """one eye feather from (cx, cy) outward at angle ang (radians, 0 = up)"""
    ex, ey = cx + math.sin(ang) * length, cy - math.cos(ang) * length
    d.line([(cx, cy), (ex, ey)], fill=(200, 150, 230, 255), width=max(1, int(3 * s)))
    # barbs: a soft vane around the outer two thirds
    for k in range(30):
        f = 0.3 + 0.7 * k / 29
        px, py = cx + math.sin(ang) * length * f, cy - math.cos(ang) * length * f
        w = width * (0.35 + 0.65 * math.sin(f * math.pi * 0.95)) * 0.5
        col = (int(60 + 120 * f), int(20 + 40 * f), int(110 + 90 * f), 220)
        for sgn in (-1, 1):
            bx = px + math.cos(ang) * w * sgn + math.sin(ang) * w * 0.4
            by = py + math.sin(ang) * w * sgn - math.cos(ang) * w * 0.4
            d.line([(px, py), (bx, by)], fill=col, width=max(1, int(2 * s)))
    # the eye at the tip: gold, magenta, teal ring, dark pupil
    r = width * 0.42
    for rr, col in ((1.0, (255, 200, 80, 255)), (0.82, (230, 60, 170, 255)), (0.6, (40, 200, 190, 255)), (0.38, (40, 10, 90, 255))):
        d.ellipse([ex - r * rr * 0.8, ey - r * rr, ex + r * rr * 0.8, ey + r * rr], fill=col)


def peacock():
    N = 1024
    im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx, cy = N / 2, N * 0.86
    n = 23
    for i in range(n):
        ang = -1.45 + 2.9 * i / (n - 1)
        L = N * (0.5 + 0.06 * math.cos(ang * 2))
        feather(d, cx, cy, ang, L, 92, 2.0)
    for i in range(n - 1):        # a second, shorter row in between
        ang = -1.45 + 2.9 * (i + 0.5) / (n - 1)
        feather(d, cx, cy, ang, N * 0.36, 66, 1.6)
    # body: a dark violet peacock standing in front of the fan, head and crest
    body = [(cx - 70, cy + 40), (cx - 85, cy - 60), (cx - 50, cy - 160), (cx - 30, cy - 260), (cx - 42, cy - 330), (cx - 20, cy - 375),
            (cx + 18, cy - 372), (cx + 60, cy - 352), (cx + 22, cy - 335), (cx + 12, cy - 270), (cx + 40, cy - 170), (cx + 85, cy - 70),
            (cx + 70, cy + 40)]
    d.polygon(body, fill=(55, 10, 85, 255))
    d.polygon([(cx - 20, cy - 360), (cx - 2, cy - 395), (cx + 6, cy - 360)], fill=(55, 10, 85, 255))
    for k in range(5):               # crest
        a = -0.5 + 0.25 * k
        x0, y0 = cx - 8, cy - 372
        x1, y1 = x0 + math.sin(a) * 60, y0 - math.cos(a) * 60
        d.line([(x0, y0), (x1, y1)], fill=(120, 40, 170, 255), width=4)
        d.ellipse([x1 - 9, y1 - 9, x1 + 9, y1 + 9], fill=(230, 80, 200, 255))
    d.ellipse([cx - 4, cy - 362, cx + 10, cy - 348], fill=(255, 60, 120, 255))   # glowing red eye
    a = np.asarray(im).astype(np.float32)
    glow = np.asarray(im.filter(ImageFilter.GaussianBlur(10))).astype(np.float32)
    out = a.copy()
    edge = (glow[..., 3] > 0) & (a[..., 3] < 10)
    out[edge, :3] = (230, 90, 220)
    out[edge, 3] = glow[edge, 3] * 0.55
    Image.fromarray(out.astype(np.uint8), 'RGBA').resize((512, 512), Image.LANCZOS).save(os.path.join(FX, 'peacock.png'))


def eyefeather():
    im = Image.new('RGBA', (256, 512), (0, 0, 0, 0))
    feather(ImageDraw.Draw(im), 128, 500, 0, 400, 150, 2.2)
    im.resize((128, 256), Image.LANCZOS).save(os.path.join(FX, 'eyefeather.png'))


def evileye():
    N = 256
    im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = N / 2
    # almond outline (two arcs), violet glow, magenta iris, slit pupil
    pts = [(c + 118 * math.cos(t), c + 62 * math.sin(t) * abs(math.sin(t)) ** 0.3) for t in np.linspace(0, 2 * math.pi, 80)]
    d.polygon(pts, fill=(250, 220, 255, 255))
    d.ellipse([c - 52, c - 52, c + 52, c + 52], fill=(200, 40, 170, 255))
    d.ellipse([c - 36, c - 36, c + 36, c + 36], fill=(150, 20, 200, 255))
    d.ellipse([c - 9, c - 44, c + 9, c + 44], fill=(15, 0, 25, 255))
    d.line(pts + [pts[0]], fill=(80, 0, 110, 255), width=7)
    glow = im.filter(ImageFilter.GaussianBlur(9))
    base = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    ga = np.asarray(glow).astype(np.float32)
    ga[..., :3] = (210, 80, 255)
    ga[..., 3] *= 0.7
    base = Image.fromarray(ga.astype(np.uint8), 'RGBA')
    base.alpha_composite(im)
    base.resize((128, 128), Image.LANCZOS).save(os.path.join(FX, 'evileye.png'))


peacock()
eyefeather()
evileye()
print('ok')
