"""Download Higgsfield effect images and convert them into textures for the Omnitrix mod."""
import json, os, urllib.request
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, '../src/main/resources/assets/omnitrix/textures')
RAW = os.path.join(HERE, 'textures_raw')
os.makedirs(RAW, exist_ok=True)
urls = {k: v for k, v in json.load(open(os.path.join(HERE, 'higgsfield_textures.json'))).items() if not k.startswith('_')}


def fetch(name):
    p = os.path.join(RAW, name + '.png')
    if not os.path.exists(p):
        with urllib.request.urlopen(urls[name]) as r:
            open(p, 'wb').write(r.read())
    return Image.open(p).convert('RGB')


def glow_alpha(img, cut=0.15):
    a = np.asarray(img).astype(np.float32) / 255.0
    alpha = np.clip((a.max(axis=2) - cut) / (1 - cut), 0, 1)
    rgb = np.where(alpha[..., None] > 0, a / np.maximum(a.max(axis=2, keepdims=True), 1e-3), 0)
    return Image.fromarray((np.dstack([rgb, alpha]) * 255).astype(np.uint8), 'RGBA')


def bbox_square(img, thr=24, pad=0.06):
    a = np.asarray(img).max(axis=2)
    ys, xs = np.where(a > thr)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    s = max(x1 - x0, y1 - y0) * (1 + pad)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return img.crop((int(cx - s / 2), int(cy - s / 2), int(cx + s / 2), int(cy + s / 2)))


def darken_edges(img):
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


def sheet(name, prefix, size=16):
    sh = fetch(name)
    w, h = sh.size
    for i in range(4):
        cell = sh.crop(((i % 2) * w // 2, (i // 2) * h // 2, (i % 2 + 1) * w // 2, (i // 2 + 1) * h // 2))
        save(glow_alpha(bbox_square(cell).resize((size, size), Image.LANCZOS)), 'particle/%s_%d.png' % (prefix, i))


sheet('fire', 'fire')
sheet('slime', 'slime')
sheet('frost', 'frost')
sheet('ecto', 'ecto')
save(fetch('crystal').resize((32, 32), Image.LANCZOS), 'fx/crystal.png')
save(darken_edges(bbox_square(fetch('sonic')).resize((64, 64), Image.LANCZOS)), 'fx/sonic.png')
save(darken_edges(bbox_square(fetch('wind')).resize((64, 64), Image.LANCZOS)), 'fx/wind.png')
save(darken_edges(bbox_square(fetch('dial'), pad=0.02).resize((128, 128), Image.LANCZOS)), 'fx/dial_holo.png')
