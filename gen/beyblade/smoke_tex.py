"""Tileable storm-cloud texture for the grey tornadoes (textures/fx/smoke.png): rgb = light grey shading, alpha = streaky noise."""
import os
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade/textures/fx/smoke.png')
N = 128
rng = np.random.default_rng(11)


def tile_noise(fx, fy, octaves):
    acc = np.zeros((N, N))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        gx, gy = fx * 2 ** o, fy * 2 ** o
        g = rng.random((gy, gx))
        # bicubic-ish upsample with wrap
        yy = np.arange(N) * gy / N
        xx = np.arange(N) * gx / N
        y0 = np.floor(yy).astype(int); x0 = np.floor(xx).astype(int)
        ty = yy - y0; tx = xx - x0
        ty = ty * ty * (3 - 2 * ty); tx = tx * tx * (3 - 2 * tx)
        a = g[y0 % gy][:, x0 % gx]; b = g[y0 % gy][:, (x0 + 1) % gx]
        c = g[(y0 + 1) % gy][:, x0 % gx]; d = g[(y0 + 1) % gy][:, (x0 + 1) % gx]
        top = a * (1 - tx) + b * tx
        bot = c * (1 - tx) + d * tx
        acc += amp * (top * (1 - ty[:, None]) + bot * ty[:, None])
        tot += amp
        amp *= 0.55
    return acc / tot


streak = tile_noise(4, 16, 4)        # streaks run along u (around the funnel)
blob = tile_noise(4, 4, 4)
a = np.clip((streak * 0.6 + blob * 0.4 - 0.32) * 2.4, 0, 1) ** 1.2
shade = 0.72 + 0.28 * tile_noise(8, 8, 3)
img = np.zeros((N, N, 4))
img[..., 0] = shade * 255
img[..., 1] = shade * 255
img[..., 2] = (shade * 0.98 + 0.02) * 255
img[..., 3] = a * 255
Image.fromarray(img.astype(np.uint8), 'RGBA').save(OUT)
print('ok', a.mean())
