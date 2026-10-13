"""Texture atlas for the beyblade stadium (512x512), styled after stadium_reference.jpg:
grey plastic bowl, red/white hazard-striped wall strips with yellow 'BATTLE END' badges."""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade/textures/entity/stadium.png')
R = np.random.default_rng(5)

img = Image.new('RGBA', (512, 512), (150, 152, 156, 255))
a = np.asarray(img).astype(np.float32)

# A: floor (0..256): planar map of the bowl, centre = (128,128), edge radius 124 px
yy, xx = np.mgrid[0:256, 0:256]
r = np.hypot(xx - 128 + 0.5, yy - 128 + 0.5) / 124
base = np.array([160, 162, 167], np.float32)
shade = 1.04 - 0.12 * r ** 2 + R.normal(0, 0.012, r.shape)
floor = base[None, None] * shade[..., None]
# subtle moulded rings
for rr, w in ((0.18, 0.012), (0.55, 0.008), (0.95, 0.02)):
    floor *= (1 - 0.08 * np.exp(-((r - rr) / w) ** 2))[..., None]
a[0:256, 0:256, :3] = floor

def plastic(x0, y0, w, h, col, noise=0.015):
    c = np.array(col, np.float32)
    s = 1 + R.normal(0, noise, (h, w)) + np.linspace(0.04, -0.04, h)[:, None]
    a[y0:y0 + h, x0:x0 + w, :3] = c[None, None] * s[..., None]

plastic(256, 0, 256, 128, (128, 130, 136))   # B: base sides / tiers
plastic(256, 128, 256, 128, (168, 170, 175))  # C: lip + tier tops
plastic(0, 320, 512, 64, (110, 112, 118))     # E: wall backs / tops

# D: wall strip 512x64 at y=256: white with red diagonal stripes, grey rims, yellow badge in the middle
strip = Image.new('RGBA', (512, 64), (235, 235, 235, 255))
d = ImageDraw.Draw(strip)
for x in range(-64, 576, 22):
    d.polygon([(x, 8), (x + 10, 8), (x + 10 + 48, 56), (x + 48, 56)], fill=(205, 40, 50, 255))
d.rectangle((0, 0, 511, 7), fill=(150, 152, 158, 255))
d.rectangle((0, 56, 511, 63), fill=(150, 152, 158, 255))
# badge
bx0, bx1 = 176, 336
d.rounded_rectangle((bx0, 12, bx1, 52), radius=8, fill=(250, 200, 40, 255), outline=(200, 60, 30, 255), width=3)
try:
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 20)
except OSError:
    font = ImageFont.load_default()
text = 'BATTLE END'
tw = d.textlength(text, font=font)
d.text(((bx0 + bx1 - tw) / 2, 20), text, fill=(190, 30, 30, 255), font=font)
a[256:320, :, :] = np.asarray(strip).astype(np.float32)

# F: stadium label for the tier front (y 384..448): grey with a red/yellow 'BEYBLADE STADIUM' tag
lab = Image.new('RGBA', (512, 64), (128, 130, 136, 255))
d = ImageDraw.Draw(lab)
d.rounded_rectangle((120, 14, 392, 50), radius=6, fill=(250, 200, 40, 255), outline=(190, 40, 40, 255), width=3)
t2 = 'BEYBLADE STADIUM'
tw = d.textlength(t2, font=font)
d.text(((512 - tw) / 2, 20), t2, fill=(190, 30, 30, 255), font=font)
a[384:448, :, :] = np.asarray(lab).astype(np.float32)

Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), 'RGBA').save(OUT)
# item icon: top-down mini stadium
icon = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(icon)
oct_ = [(16 + 15.5 * math.cos(math.radians(22.5 + 45 * k)), 16 + 15.5 * math.sin(math.radians(22.5 + 45 * k))) for k in range(8)]
d.polygon(oct_, fill=(120, 122, 128, 255), outline=(80, 82, 88, 255))
d.ellipse((4, 4, 27, 27), fill=(165, 167, 172, 255), outline=(200, 40, 50, 255), width=2)
d.ellipse((12, 12, 19, 19), outline=(140, 142, 147, 255))
for k in range(4):
    ang = math.radians(45 + 90 * k)
    x, y = 16 + 12 * math.cos(ang), 16 + 12 * math.sin(ang)
    d.rectangle((x - 2, y - 1, x + 2, y + 1), fill=(250, 200, 40, 255))
icon.save(os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade/textures/item/stadium.png'))
print('ok')
