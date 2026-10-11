"""Shared helpers: load the exported Higgsfield meshes, normalise them, software-render previews."""
import os
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
HEIGHTS = {'heatblast': 2.05, 'terraspin': 2.0, 'diamondhead': 2.3, 'goop': 2.25, 'xlr8': 1.9, 'fourarms': 2.6,
           'bigchill': 2.3, 'ultimate_echo_echo': 2.3, 'ghostfreak': 2.2, 'omnitrix': 1.0}


def load(name):
    d = np.load(os.path.join(HERE, 'mesh', name + '.npz'))
    tex = Image.open(os.path.join(HERE, 'mesh', name + '.png')).convert('RGBA')
    v = d['v'].astype(np.float64)
    lo, hi = v.min(0), v.max(0)
    s = HEIGHTS[name] / (hi[1] - lo[1])
    c = (lo + hi) / 2
    v = (v - np.array([c[0], lo[1], c[2]])) * s
    return v, d['f'], d['uv'], tex


def render(v, f, uv, tex, view='front', size=420, span=None, extra=None):
    """orthographic textured z-buffer render. view: front (+z towards camera), side (+x towards camera), back."""
    if view == 'front':
        P = np.stack([v[:, 0], v[:, 1], v[:, 2]], 1)
    elif view == 'back':
        P = np.stack([-v[:, 0], v[:, 1], -v[:, 2]], 1)
    elif view == 'side':
        P = np.stack([-v[:, 2], v[:, 1], v[:, 0]], 1)
    else:  # top
        P = np.stack([v[:, 0], -v[:, 2], v[:, 1]], 1)
    if span is None:
        span = max(np.ptp(P[:, 0]), np.ptp(P[:, 1])) * 1.1
    cx = (P[:, 0].max() + P[:, 0].min()) / 2
    cy = (P[:, 1].max() + P[:, 1].min()) / 2
    sx = (P[:, 0] - cx) / span * size + size / 2
    sy = size / 2 - (P[:, 1] - cy) / span * size
    T = np.asarray(tex).astype(np.float32)
    th, tw = T.shape[:2]
    img = np.full((size, size, 3), 235, np.float32)
    zb = np.full((size, size), -1e9)
    for tri in f:
        x = sx[tri]; y = sy[tri]; z = P[tri, 2]
        x0, x1 = int(max(0, np.floor(x.min()))), int(min(size - 1, np.ceil(x.max())))
        y0, y1 = int(max(0, np.floor(y.min()))), int(min(size - 1, np.ceil(y.max())))
        if x1 < x0 or y1 < y0:
            continue
        den = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2])
        if abs(den) < 1e-9:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        a = ((y[1] - y[2]) * (gx - x[2]) + (x[2] - x[1]) * (gy - y[2])) / den
        b = ((y[2] - y[0]) * (gx - x[2]) + (x[0] - x[2]) * (gy - y[2])) / den
        c = 1 - a - b
        m = (a >= -0.01) & (b >= -0.01) & (c >= -0.01)
        if not m.any():
            continue
        zz = a * z[0] + b * z[1] + c * z[2]
        sub = zb[y0:y1 + 1, x0:x1 + 1]
        m &= zz > sub
        if not m.any():
            continue
        u = a * uv[tri[0], 0] + b * uv[tri[1], 0] + c * uv[tri[2], 0]
        w = a * uv[tri[0], 1] + b * uv[tri[1], 1] + c * uv[tri[2], 1]
        tx = np.clip((u % 1.0) * tw, 0, tw - 1).astype(int)
        ty = np.clip((1 - (w % 1.0)) * th, 0, th - 1).astype(int)
        col = T[ty, tx, :3]
        if extra is not None:
            col = col * 0.35 + extra(tri) * 0.65
        sub[m] = zz[m]
        img[y0:y1 + 1, x0:x1 + 1][m] = col[m]
    im = Image.fromarray(img.clip(0, 255).astype(np.uint8))
    return im, (cx, cy, span, size)


def to_screen(p, view, cam):
    cx, cy, span, size = cam
    if view == 'front':
        x, y = p[0], p[1]
    elif view == 'back':
        x, y = -p[0], p[1]
    elif view == 'side':
        x, y = -p[2], p[1]
    else:
        x, y = p[0], -p[2]
    return (x - cx) / span * size + size / 2, size / 2 - (y - cy) / span * size


def grid(im, view, cam, step=0.25):
    d = ImageDraw.Draw(im)
    cx, cy, span, size = cam
    for k in np.arange(-3, 3.01, step):
        X, _ = to_screen((k, 0, k), view, cam)
        _, Y = to_screen((0, k, 0), view, cam)
        col = (255, 0, 0) if abs(k) < 1e-6 else (180, 180, 220)
        d.line([(X, 0), (X, size)], fill=col, width=1)
        d.line([(0, Y), (size, Y)], fill=col, width=1)
        if abs(k * 4 - round(k * 4)) < 1e-6 and abs(k * 2 - round(k * 2)) < 1e-6:
            d.text((X + 1, 1), '%.1f' % k, fill=(0, 0, 160))
            d.text((1, Y + 1), '%.1f' % k, fill=(0, 0, 160))
    return im
