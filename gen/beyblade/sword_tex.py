"""Lightning sword sprite for Variares (textures/fx/sword.png): blade pointing down, hilt at the top."""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade/textures/fx/sword.png')
W, H = 256, 1024
S = 2
img = Image.new('RGBA', (W * S, H * S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
c = W * S / 2


def P(x, y):
    return (c + x * S, y * S)


# blade
d.polygon([P(-30, 250), P(30, 250), P(26, 900), P(0, 1010), P(-26, 900)], fill=(205, 215, 235, 255))
d.polygon([P(-6, 260), P(6, 260), P(5, 920), P(0, 960), P(-5, 920)], fill=(255, 255, 255, 255))        # fuller highlight
d.polygon([P(-30, 250), P(-6, 260), P(-5, 920), P(-26, 900)], fill=(160, 170, 195, 255))                # shaded half
# crossguard: swept war-god wings
d.polygon([P(-120, 205), P(-40, 225), P(0, 262), P(40, 225), P(120, 205), P(95, 250), P(30, 262), P(0, 285), P(-30, 262), P(-95, 250)],
          fill=(200, 30, 35, 255))
d.polygon([P(-120, 205), P(-150, 185), P(-110, 220)], fill=(220, 180, 60, 255))
d.polygon([P(120, 205), P(150, 185), P(110, 220)], fill=(220, 180, 60, 255))
d.ellipse([P(-22, 222), P(22, 266)], fill=(240, 200, 70, 255))
d.ellipse([P(-12, 232), P(12, 256)], fill=(220, 20, 30, 255))
# grip and pommel
d.rectangle([P(-12, 90), P(12, 215)], fill=(60, 20, 22, 255))
for y in range(95, 215, 14):
    d.line([P(-12, y), P(12, y + 8)], fill=(180, 150, 60, 255), width=4)
d.polygon([P(0, 40), P(28, 75), P(0, 110), P(-28, 75)], fill=(230, 190, 70, 255))
d.ellipse([P(-9, 66), P(9, 84)], fill=(220, 20, 30, 255))
img = img.resize((W, H), Image.LANCZOS)
a = np.asarray(img).astype(np.float32) / 255
glow = np.asarray(img.split()[3].filter(ImageFilter.GaussianBlur(10))).astype(np.float32) / 255
out = a.copy()
halo = np.clip(glow * 0.8 - a[..., 3] * 0.8, 0, 1)
for k, col in enumerate((1.0, 0.85, 0.5)):
    out[..., k] = np.where(a[..., 3] > 0.05, a[..., k], col)
out[..., 3] = np.clip(a[..., 3] + halo, 0, 1)
Image.fromarray((out * 255).astype(np.uint8), 'RGBA').save(OUT)
print('ok')
