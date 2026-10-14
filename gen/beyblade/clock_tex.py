"""Clock dial effect texture for Twisted Tempo: textures/fx/clock.png (white glowing ring, ticks, Roman numerals; alpha)."""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade/textures/fx/clock.png')
N = 1024
img = Image.new('L', (N, N), 0)
d = ImageDraw.Draw(img)
c = N / 2
for r, w in ((500, 10), (470, 4), (330, 6), (318, 2)):
    d.ellipse([c - r, c - r, c + r, c + r], outline=255, width=w)
for k in range(60):
    a = k / 60 * math.tau
    r0 = 440 if k % 5 else 415
    d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * 468, c + math.sin(a) * 468)], fill=255, width=6 if k % 5 == 0 else 3)
font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf', 70)
nums = ['XII', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI']
for k, s in enumerate(nums):
    a = -math.pi / 2 + k / 12 * math.tau
    x, y = c + math.cos(a) * 372, c + math.sin(a) * 372
    t = Image.new('L', (220, 100), 0)
    ImageDraw.Draw(t).text((110, 50), s, fill=255, font=font, anchor='mm')
    t = t.rotate(-math.degrees(a + math.pi / 2), resample=Image.BICUBIC, expand=True)
    img.paste(255, (int(x - t.width / 2), int(y - t.height / 2)), t)
# inner gear ring
for k in range(24):
    a = k / 24 * math.tau
    d.polygon([(c + math.cos(a - 0.05) * 300, c + math.sin(a - 0.05) * 300), (c + math.cos(a) * 280, c + math.sin(a) * 280),
               (c + math.cos(a + 0.05) * 300, c + math.sin(a + 0.05) * 300)], fill=255)
m = np.asarray(img).astype(np.float32) / 255
glow = np.asarray(img.filter(ImageFilter.GaussianBlur(8))).astype(np.float32) / 255
a = np.clip(m + glow * 0.6, 0, 1)
out = np.zeros((N, N, 4), np.float32)
out[..., 0] = a * 0.85 + m * 0.15
out[..., 1] = a * 0.92 + m * 0.08
out[..., 2] = a
out[..., 3] = a
Image.fromarray((out * 255).astype(np.uint8), 'RGBA').resize((512, 512), Image.LANCZOS).save(OUT)
print('ok')
