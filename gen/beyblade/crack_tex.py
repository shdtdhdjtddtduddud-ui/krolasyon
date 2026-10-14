"""Radial impact cracks decal for the stadium floor: textures/fx/cracks.png (dark lines, alpha = crack)."""
import math, os, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade/textures/fx/cracks.png')
N = 512
rnd = random.Random(4)
img = Image.new('L', (N, N), 0)
d = ImageDraw.Draw(img)


def branch(x, y, ang, length, w, depth):
    while length > 4 and w >= 1:
        step = rnd.uniform(8, 18)
        ang += rnd.uniform(-0.35, 0.35)
        nx, ny = x + math.cos(ang) * step, y + math.sin(ang) * step
        d.line([(x, y), (nx, ny)], fill=255, width=int(w))
        x, y = nx, ny
        length -= step
        w *= 0.93
        if depth < 3 and rnd.random() < 0.18:
            branch(x, y, ang + rnd.choice([-1, 1]) * rnd.uniform(0.5, 1.0), length * 0.6, w * 0.8, depth + 1)


c = N / 2
for i in range(11):
    a = i / 11 * math.tau + rnd.uniform(-0.2, 0.2)
    branch(c + math.cos(a) * 14, c + math.sin(a) * 14, a, rnd.uniform(150, 240), 7, 0)
# concentric fracture arcs
for r in (60, 115):
    a0 = rnd.uniform(0, math.tau)
    for k in range(5):
        s = a0 + k * math.tau / 5
        d.arc([c - r, c - r, c + r, c + r], math.degrees(s), math.degrees(s + rnd.uniform(0.4, 0.8)), fill=200, width=3)
d.ellipse([c - 22, c - 22, c + 22, c + 22], fill=180)
m = np.asarray(img).astype(np.float32) / 255
soft = np.asarray(img.filter(ImageFilter.GaussianBlur(3))).astype(np.float32) / 255
yy, xx = np.mgrid[0:N, 0:N]
fall = np.clip(1 - np.hypot(xx - c, yy - c) / (N / 2), 0, 1) ** 0.6
alpha = np.clip(m * 0.95 + soft * 0.35, 0, 1) * fall
rgb = np.stack([0.16 + 0.1 * soft, 0.15 + 0.08 * soft, 0.15 + 0.06 * soft], -1)
out = np.concatenate([rgb, alpha[..., None]], -1)
Image.fromarray((out * 255).astype(np.uint8), 'RGBA').save(OUT)
print('ok')
