"""Exports GeckoLib geo/animation/texture assets + overlay textures for the horror mod."""
import os, json, numpy as np
from PIL import Image, ImageFilter
import hatman, paint_hatman, hatman_anim
from geo import render, anim_file

ROOT = os.path.join(os.path.dirname(__file__), '../common/src/main/resources/assets/krolasyonhorror')
for d in ('geo/entity', 'animations/entity', 'textures/entity', 'textures/gui', 'textures/item', 'lang'):
    os.makedirs(os.path.join(ROOT, d), exist_ok=True)

G = hatman.G
G.pack()
tex = G.paint(paint_hatman.PAINTERS)
tex.save(os.path.join(ROOT, 'textures/entity/hatman.png'))
with open(os.path.join(ROOT, 'geo/entity/hatman.geo.json'), 'w') as f:
    json.dump(G.geo_json('geometry.hatman'), f, separators=(',', ':'))
with open(os.path.join(ROOT, 'animations/entity/hatman.animation.json'), 'w') as f:
    json.dump(anim_file(list(hatman_anim.A.values()), 'animation.hatman'), f, separators=(',', ':'))

# ---- VHS overlay textures
rng = np.random.default_rng(5)
for i in range(4):
    n = rng.random((128, 256))
    lines = rng.random((128, 1)) * 0.6
    a = np.clip((n * 0.8 + lines) ** 2, 0, 1)
    img = np.zeros((128, 256, 4), np.uint8)
    v = (n * 255).astype(np.uint8)
    img[..., 0] = v; img[..., 1] = v; img[..., 2] = v; img[..., 3] = (a * 255).astype(np.uint8)
    Image.fromarray(img, 'RGBA').save(os.path.join(ROOT, f'textures/gui/static_{i}.png'))
yy, xx = np.mgrid[0:256, 0:256] / 255.0
r = np.hypot(xx - 0.5, (yy - 0.5) * 1.1) / 0.72
vig = np.zeros((256, 256, 4), np.uint8)
vig[..., 3] = (np.clip(r, 0, 1) ** 2.4 * 255).astype(np.uint8)
Image.fromarray(vig, 'RGBA').save(os.path.join(ROOT, 'textures/gui/vignette.png'))

# ---- jumpscare frame: the creature mid-scream lunging into the lens, VHS treated
pose = hatman_anim.A['scream'].sample(0.75)
img, _ = render(G, tex, pose, yaw=8, pitch=-22, size=1400, bg=(10, 9, 8), light=(0.0, 0.3, -1.0))
img = img.crop((330, 20, 1070, 575)).resize((512, 384), Image.BILINEAR)
a = np.asarray(img).astype(np.float32)
a = 255 * (a / 255) ** 0.6 * np.array([1.5, 1.15, 1.0])
a *= 1.0 - 0.6 * np.clip(np.hypot(*np.mgrid[-1:1:384j, -1:1:512j]) - 0.5, 0, 1)[..., None]
a[:, :, 0] = np.roll(a[:, :, 0], 3, 1); a[:, :, 2] = np.roll(a[:, :, 2], -3, 1)
a += rng.normal(0, 18, a.shape)
a[::2] *= 0.82
img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.7))
img.save(os.path.join(ROOT, 'textures/gui/jumpscare.png'))
print('assets written, atlas', G.tw, G.th, 'anims', len(hatman_anim.A))
