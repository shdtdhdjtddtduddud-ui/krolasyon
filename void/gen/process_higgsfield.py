"""Download the Higgsfield-generated images and convert them into Minecraft textures for the void tree mod.

Additive sprites keep their black background; particles get alpha from brightness; the grimoire icon is keyed out.
Usage: python process_higgsfield.py   (needs pillow + numpy, internet)
"""
import json, os, urllib.request
from collections import deque
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '../src/main/resources/assets/voidtree/textures')
RAW = os.path.join(HERE, 'higgsfield_raw')
os.makedirs(RAW, exist_ok=True)
urls = {k: v for k, v in json.load(open(os.path.join(HERE, 'higgsfield_assets.json'))).items() if not k.startswith('_')}


def fetch(name):
    p = os.path.join(RAW, name + '.png')
    if not os.path.exists(p):
        with urllib.request.urlopen(urls[name]) as r:
            open(p, 'wb').write(r.read())
    return Image.open(p).convert('RGB')


def glow_alpha(img):
    a = np.asarray(img).astype(np.float32) / 255.0
    alpha = np.clip((a.max(axis=2) - 0.18) / 0.82, 0, 1)
    rgb = np.where(alpha[..., None] > 0, a / np.maximum(a.max(axis=2, keepdims=True), 1e-3), 0)
    return Image.fromarray((np.dstack([rgb, alpha]) * 255).astype(np.uint8), 'RGBA')


def bbox_square(img, thr=24, pad=0.04):
    a = np.asarray(img).max(axis=2)
    ys, xs = np.where(a > thr)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    s = max(x1 - x0, y1 - y0) * (1 + pad)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return img.crop((int(cx - s / 2), int(cy - s / 2), int(cx + s / 2), int(cy + s / 2)))


def darken_edges(img):
    """fade the sprite to black towards the border so additive quads never show a hard edge"""
    a = np.asarray(img).astype(np.float32)
    h, w = a.shape[:2]
    y, x = np.mgrid[0:h, 0:w]
    r = np.sqrt(((x - w / 2 + 0.5) / (w / 2)) ** 2 + ((y - h / 2 + 0.5) / (h / 2)) ** 2)
    f = np.clip((1.0 - r) / 0.2, 0, 1)[..., None]
    return Image.fromarray((a * f).astype(np.uint8), 'RGB')


def save(img, rel):
    p = os.path.join(TEX, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print('wrote', rel, img.size)


sh = fetch('wisps')
w, h = sh.size
for i in range(4):
    cell = sh.crop(((i % 2) * w // 2, (i // 2) * h // 2, (i % 2 + 1) * w // 2, (i // 2 + 1) * h // 2))
    save(glow_alpha(bbox_square(cell).resize((16, 16), Image.LANCZOS)), 'particle/wisp_%d.png' % i)

save(darken_edges(bbox_square(fetch('black_hole')).resize((64, 64), Image.LANCZOS)), 'fx/black_hole.png')
save(darken_edges(bbox_square(fetch('rift'), pad=0.1).resize((64, 64), Image.LANCZOS)), 'fx/rift.png')
save(fetch('starfield').resize((64, 64), Image.LANCZOS), 'fx/starfield.png')
save(darken_edges(bbox_square(fetch('vortex')).resize((64, 64), Image.LANCZOS)), 'fx/vortex.png')
save(darken_edges(bbox_square(fetch('slash'), pad=0.1).resize((64, 64), Image.LANCZOS)), 'fx/slash.png')
star = bbox_square(fetch('star'))
save(glow_alpha(star.resize((16, 16), Image.LANCZOS)), 'particle/star.png')
save(darken_edges(star.resize((32, 32), Image.LANCZOS)), 'fx/star.png')

# grimoire item: flood-fill the black background on the full-res image, premultiplied box downscale to 32x32
a = np.asarray(fetch('grimoire')).astype(int)
dark = a.max(axis=2) < 40
H, W = dark.shape
mask = np.zeros_like(dark)
q = deque([(0, 0), (0, W - 1), (H - 1, 0), (H - 1, W - 1)])
while q:
    y, x = q.popleft()
    if 0 <= y < H and 0 <= x < W and not mask[y, x] and dark[y, x]:
        mask[y, x] = True
        q.extend([(y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)])
ys, xs = np.where(~mask)
s = int(max(xs.max() - xs.min(), ys.max() - ys.min()) * 1.04)
cx, cy = (xs.min() + xs.max()) // 2, (ys.min() + ys.max()) // 2
img = Image.fromarray(np.dstack([a, np.where(mask, 0, 255)]).astype(np.uint8), 'RGBA').crop((cx - s // 2, cy - s // 2, cx + s // 2, cy + s // 2))
arr = np.asarray(img).astype(float)
al = arr[..., 3:] / 255
pm = Image.fromarray(np.dstack([arr[..., :3] * al, arr[..., 3]]).astype(np.uint8), 'RGBA').resize((32, 32), Image.BOX)
o = np.asarray(pm).astype(float)
al = o[..., 3:] / 255
rgb = np.where(al > 0.01, o[..., :3] / np.maximum(al, 0.01), 0)
save(Image.fromarray(np.dstack([np.clip(rgb * 1.1, 0, 255), np.where(o[..., 3] > 120, 255, 0)]).astype(np.uint8), 'RGBA'), 'item/void_grimoire.png')
