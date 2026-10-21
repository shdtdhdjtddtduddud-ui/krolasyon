"""Efsane kılıçların dokuları. Bıçak dokularında x = enine (u), y = boyuna (üst = uç)."""
import math, os, random, shutil
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'mythblades', 'textures', 'block')
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)


def hexc(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


def save(name, rgb, a=None):
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)
    if a is None:
        a = np.full(rgb.shape[:2], 255, np.uint8)
    img = np.dstack([rgb, np.clip(a, 0, 255).astype(np.uint8)])
    Image.fromarray(img, 'RGBA').save(os.path.join(OUT, name + '.png'))


def grid(W, H):
    x = (np.arange(W) + 0.5) / W
    t = 1 - (np.arange(H) + 0.5) / H
    return np.meshgrid(x, t)


def noise(W, H, scale, amp):
    n = rng.random((H // scale + 2, W // scale + 2))
    img = Image.fromarray((n * 255).astype(np.uint8)).resize((W + scale * 2, H + scale * 2), Image.BICUBIC)
    return (np.asarray(img, float)[scale:scale + H, scale:scale + W] / 255 - 0.5) * 2 * amp


def lerp(a, b, f):
    f = np.clip(f, 0, 1)[..., None]
    return a * (1 - f) + b * f


# ------------------------------------------------------------------ gökkuşağı bıçağı
def rainbow():
    W, H = 64, 256
    u, t = grid(W, H)
    cols = [hexc(c) for c in ('#f7a6c9', '#ffc79a', '#fff0a0', '#bdf0b4', '#a6d9f7', '#c4b6f7')]
    b = u * 6
    idx = np.clip(np.floor(b).astype(int), 0, 5)
    f = b - np.floor(b)
    rgb = np.zeros((H, W, 3))
    for i in range(6):
        nxt = cols[min(5, i + 1)]
        m = idx == i
        mix = np.clip((f - 0.7) / 0.3, 0, 1) * 0.5
        rgb[m] = lerp(cols[i][None, None, :].repeat(H, 0).repeat(W, 1), nxt[None, None, :].repeat(H, 0).repeat(W, 1), mix)[m]
    rgb = lerp(rgb, hexc('#fff6fb'), np.clip((0.09 - u) / 0.09, 0, 1) * 0.8)
    rgb = lerp(rgb, hexc('#eaf6ff'), np.clip((u - 0.88) / 0.12, 0, 1) * 0.85)
    # sırt pahı ve cam çizgileri
    rgb = lerp(rgb, hexc('#ffffff'), ((u > 0.12) & (u < 0.15)) * 0.35)
    streak = (np.mod(t * 11 - u * 1.6, 1) < 0.045) * 0.18
    rgb = lerp(rgb, hexc('#ffffff'), streak)
    rgb *= (1 + noise(W, H, 8, 0.03))[..., None]
    a = np.full((H, W), 255.0)
    for tc, d in ((0.60, 0.022), (0.71, 0.017), (0.80, 0.012)):
        notch = u < 0.2 * np.clip(1 - np.abs(t - tc) / d, 0, 1)
        a[notch] = 0
    save('rainbow_blade', rgb, a)


def cloud():
    W = H = 32
    u, t = grid(W, H)
    rgb = lerp(np.broadcast_to(hexc('#5d69b6'), (H, W, 3)).copy(), hexc('#a3aeea'), t)
    rgb *= (1 + noise(W, H, 4, 0.06))[..., None]
    save('cloud', rgb)


def gold_wrap():
    W, H = 32, 64
    u, t = grid(W, H)
    base = np.broadcast_to(hexc('#e9b84a'), (H, W, 3)).copy()
    band = np.mod(t * 9 + u * 1.0, 1)
    rgb = lerp(base, hexc('#9c6c1a'), (band < 0.2) * 1.0)
    rgb = lerp(rgb, hexc('#ffe39a'), ((band > 0.45) & (band < 0.6)) * 0.6)
    rgb *= (1 + noise(W, H, 4, 0.04))[..., None]
    save('gold_wrap', rgb)


def gold():
    W = H = 16
    u, t = grid(W, H)
    rgb = lerp(np.broadcast_to(hexc('#a8761e'), (H, W, 3)).copy(), hexc('#ffd774'), 0.5 + 0.5 * np.sin(u * 6.28 + t * 2))
    rgb *= (1 + noise(W, H, 4, 0.05))[..., None]
    save('gold', rgb)


# ------------------------------------------------------------------ kızıl damar
def c_half(y):
    if y < 1 or y > 25:
        return 0
    if y < 5:
        return 2.1 + 0.3 * (y - 1) / 4
    if y < 6.6:
        return 2.4 + 1.0 * (y - 5) / 1.6
    if y < 13:
        return 2.5 + 1.6 * math.sin((y - 6.6) / 6.4 * math.pi / 2)
    s = (y - 13) / 12
    return 4.1 * (1 - s ** 1.7)


def veins():
    segs = [(0, 3.0, 0, 21.0)]
    for y in (8.5, 12.5, 16.5):
        for side in (-1, 1):
            tx = side * c_half(y + 3.4) * 0.66
            segs.append((0, y, tx * 0.55, y + 2.0))
            segs.append((tx * 0.55, y + 2.0, tx, y + 4.2))
            segs.append((tx * 0.3, y + 1.2, tx * 0.45, y + 3.2))
    prev = None
    y = 6.8
    while y <= 23.6:
        hx = max(0, c_half(y) - 1.45)
        if prev:
            segs.append((-prev[0], prev[1], -hx, y))
            segs.append((prev[0], prev[1], hx, y))
        prev = (hx, y)
        y += 0.4
    return segs


def seg_dist(px, py, segs):
    best = np.full(px.shape, 99.0)
    for x0, y0, x1, y1 in segs:
        dx, dy = x1 - x0, y1 - y0
        l2 = dx * dx + dy * dy
        tt = np.clip(((px - x0) * dx + (py - y0) * dy) / max(l2, 1e-6), 0, 1)
        ex, ey = px - x0 - dx * tt, py - y0 - dy * tt
        best = np.minimum(best, np.sqrt(ex * ex + ey * ey))
    return best


def crimson():
    W, H = 64, 192
    u, t = grid(W, H)
    y = 1 + 24 * t
    hw = np.vectorize(c_half)(y)
    x = (u * 2 - 1) * hw
    vd = seg_dist(x, y, veins())
    edge = np.abs(u * 2 - 1)
    rgb = np.broadcast_to(hexc('#b59aa5'), (H, W, 3)).copy()
    rgb *= (1 + noise(W, H, 6, 0.05))[..., None]
    rgb = lerp(rgb, hexc('#e6d6da'), np.clip((edge - 0.8) / 0.2, 0, 1) * 0.75)
    rgb = lerp(rgb, hexc('#8a7080'), np.clip((0.05 - edge) / 0.05, 0, 1) * 0.5)
    stem = (y < 6.2) & (np.abs(x) < 0.9)
    core = vd < 0.17
    outline = (vd < 0.33) & ~core
    rgb = lerp(rgb, hexc('#5a1208'), outline * 1.0)
    rgb = lerp(rgb, hexc('#c93a1c'), core * 1.0)
    rgb = lerp(rgb, hexc('#ff7a48'), (vd < 0.07) * 0.5)
    rgb = lerp(rgb, hexc('#6e7a2e'), stem * (vd < 0.4) * 1.0)
    save('crimson_blade', rgb)
    g = np.zeros((H, W, 3))
    g = lerp(g, hexc('#ff4a20'), core * ~stem * 1.0)
    g = lerp(g, hexc('#ffb070'), (vd < 0.07) * ~stem * 1.0)
    g = lerp(g, hexc('#5a1000'), outline * ~stem * 0.6)
    save('crimson_glow', g)


def olive():
    W = H = 32
    u, t = grid(W, H)
    rgb = lerp(np.broadcast_to(hexc('#4c5a20'), (H, W, 3)).copy(), hexc('#9aac4a'), t * 0.8 + noise(W, H, 4, 0.2) + 0.1)
    rgb = lerp(rgb, hexc('#c4d070'), (np.mod(u * 4 + t * 2, 1) < 0.08) * 0.4)
    save('olive', rgb)


def ruby():
    W = H = 16
    u, t = grid(W, H)
    d = np.sqrt((u - 0.35) ** 2 + (t - 0.7) ** 2)
    rgb = lerp(np.broadcast_to(hexc('#7a0a0a'), (H, W, 3)).copy(), hexc('#ff5040'), np.clip(1 - d * 1.6, 0, 1))
    rgb = lerp(rgb, hexc('#ffffff'), (d < 0.12) * 0.8)
    save('ruby', rgb)


# ------------------------------------------------------------------ yosunkök
def stone():
    W, H = 64, 256
    u, t = grid(W, H)
    # fiziksel ölçek: genişlik ~8 px, boy ~28 px
    px, py = u * 8, t * 28
    pts = rng.random((70, 2)) * [8, 28]
    d = np.stack([np.sqrt((px - a) ** 2 + (py - b) ** 2) for a, b in pts])
    order = np.argsort(d, axis=0)
    d1 = np.take_along_axis(d, order[:1], 0)[0]
    d2 = np.take_along_axis(d, order[1:2], 0)[0]
    cell = order[0]
    shade = 0.86 + 0.24 * (np.sin(cell * 12.9898) * 43758.5453 % 1)
    rgb = np.broadcast_to(hexc('#9c8a92'), (H, W, 3)) * shade[..., None]
    rgb = lerp(rgb, hexc('#5c4c54'), (d2 - d1 < 0.12) * 0.8)
    rgb = lerp(rgb, hexc('#cfc2c6'), np.clip((0.1 - u) / 0.1, 0, 1) * 0.6)
    rgb = lerp(rgb, hexc('#c0b2b8'), np.clip((u - 0.9) / 0.1, 0, 1) * 0.4)
    rgb *= (1 + noise(W, H, 4, 0.05))[..., None]
    mossn = noise(W, H, 10, 0.5) + noise(W, H, 4, 0.25)
    moss = (mossn > -0.15 + t * 1.2) & (t < 0.55)
    rgb = lerp(rgb, hexc('#6e9a2a'), moss * 1.0)
    rgb = lerp(rgb, hexc('#a8d050'), moss * (mossn > 0.3 + t * 1.4) * 1.0)
    a = np.full((H, W), 255.0)
    for tc, dd, side in ((0.34, 0.025, 0), (0.58, 0.03, 0), (0.70, 0.022, 1), (0.47, 0.018, 1)):
        prof = 0.16 * np.clip(1 - np.abs(t - tc) / dd, 0, 1)
        notch = (u < prof) if side == 0 else (u > 1 - prof)
        a[notch] = 0
    save('stone_blade', rgb, a)


def moss():
    W = H = 16
    rgb = lerp(np.broadcast_to(hexc('#4f7a1e'), (H, W, 3)).copy(), hexc('#b4d85a'), 0.5 + noise(W, H, 2, 0.6))
    save('moss', rgb)


def leaf():
    W = H = 16
    u, t = grid(W, H)
    rgb = lerp(np.broadcast_to(hexc('#5f9a2a'), (H, W, 3)).copy(), hexc('#b8e060'), t)
    rgb = lerp(rgb, hexc('#3e6a18'), (np.abs(u - 0.5) < 0.05) * 0.6)
    save('leaf', rgb)


def bark():
    W, H = 16, 32
    u, t = grid(W, H)
    base = np.broadcast_to(hexc('#7a3a24'), (H, W, 3)).copy()
    stripes = np.sin(u * 40 + noise(W, H, 4, 2.0) * 3)
    rgb = lerp(base, hexc('#4a1e12'), (stripes > 0.6) * 0.8)
    rgb = lerp(rgb, hexc('#b06a40'), (stripes < -0.8) * 0.5)
    rgb *= (1 + noise(W, H, 2, 0.08))[..., None]
    save('bark', rgb)


for f in (rainbow, cloud, gold_wrap, gold, crimson, olive, ruby, stone, moss, leaf, bark):
    f()
print('ok', OUT)
