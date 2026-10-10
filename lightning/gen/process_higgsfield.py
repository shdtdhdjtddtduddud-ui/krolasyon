"""Download Higgsfield-generated images and convert them into Minecraft textures.

Black backgrounds are turned into alpha (glow sprites keep their colour, alpha = brightness),
the tome icon is keyed out and reduced to a 16x16 item sprite.
Usage: python process_higgsfield.py   (needs pillow + numpy, internet)
"""
import json, os, io, urllib.request
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '../src/main/resources/assets/stormtree/textures')
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
    """black background -> transparent; alpha from brightness, colour un-premultiplied"""
    a = np.asarray(img).astype(np.float32) / 255.0
    alpha = a.max(axis=2)
    alpha = np.clip((alpha - 0.06) / 0.94, 0, 1)
    rgb = np.where(alpha[..., None] > 1e-3, a / np.maximum(a.max(axis=2, keepdims=True), 1e-3), 0)
    out = np.dstack([rgb, alpha])
    return Image.fromarray((out * 255).astype(np.uint8), 'RGBA')


def bbox_square(img, thr=24, pad=0.04):
    a = np.asarray(img).max(axis=2)
    ys, xs = np.where(a > thr)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    s = max(x1 - x0, y1 - y0) * (1 + pad)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return img.crop((int(cx - s / 2), int(cy - s / 2), int(cx + s / 2), int(cy + s / 2)))


def save(img, rel):
    p = os.path.join(TEX, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print('wrote', rel, img.size)


# ball lightning orb (additive sprite, keep black)
save(bbox_square(fetch('ball_lightning')).resize((64, 64), Image.LANCZOS), 'fx/ball_lightning.png')

# spark sheet 2x2 -> 4 particle frames
sp = fetch('sparks')
w, h = sp.size
for i in range(4):
    cell = sp.crop(((i % 2) * w // 2, (i // 2) * h // 2, (i % 2 + 1) * w // 2, (i // 2 + 1) * h // 2))
    save(glow_alpha(bbox_square(cell).resize((16, 16), Image.LANCZOS)), 'particle/spark_%d.png' % i)

# shield hex texture (additive, tiled on the dome)
save(fetch('shield_hex').resize((64, 64), Image.LANCZOS), 'fx/shield_hex.png')

# skull mark
save(glow_alpha(bbox_square(fetch('skull')).resize((32, 32), Image.LANCZOS)), 'fx/skull.png')

# tome item: key out the black background on the full-res image (flood fill), premultiplied box downscale to 32x32
from collections import deque
a = np.asarray(fetch('storm_tome')).astype(int)
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
save(Image.fromarray(np.dstack([np.clip(rgb * 1.1, 0, 255), np.where(o[..., 3] > 120, 255, 0)]).astype(np.uint8), 'RGBA'), 'item/storm_tome.png')
