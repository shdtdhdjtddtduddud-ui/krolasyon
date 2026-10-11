"""Effect sprites, GUI art (dial, core, badge, alien portraits from the reference art) and the Omnitrix item icon."""
import os, math, numpy as np
from PIL import Image, ImageFilter
from collections import deque

HERE = os.path.dirname(__file__)
RES = os.path.join(HERE, '..', 'src/main/resources/assets/krolben10/textures')
for d in ('fx', 'gui', 'gui/portrait', 'item', 'block'):
    os.makedirs(os.path.join(RES, d), exist_ok=True)


def save(arr, path):
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA').save(os.path.join(RES, path))


def radial(S):
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    return np.hypot(xx - S / 2, yy - S / 2) / (S / 2)


rng = np.random.default_rng(7)
S = 64
d = radial(S)
# soft glow (used additively)
g = np.clip(1 - d, 0, 1) ** 2
a = np.zeros((S, S, 4)); a[..., :3] = 255; a[..., 3] = g * 255
a[..., :3] *= g[..., None]
save(a, 'fx/glow.png')
# shockwave ring
S = 128
d = radial(S)
ring = np.exp(-((d - 0.86) / 0.06) ** 2) + 0.4 * np.exp(-((d - 0.7) / 0.04) ** 2)
ring = np.clip(ring, 0, 1)
a = np.zeros((S, S, 4)); a[..., :3] = 255 * ring[..., None]; a[..., 3] = 255 * ring
save(a, 'fx/ring.png')
# sonic ring: concentric bands
son = np.clip(np.exp(-((d - 0.9) / 0.05) ** 2) + 0.6 * np.exp(-((d - 0.72) / 0.03) ** 2) + 0.35 * np.exp(-((d - 0.55) / 0.025) ** 2), 0, 1)
a = np.zeros((S, S, 4)); a[..., :3] = 255 * son[..., None]; a[..., 3] = 255 * son
save(a, 'fx/sonic.png')
# shock (star burst for spheres)
yy, xx = np.mgrid[0:S, 0:S] + 0.5
ang = np.arctan2(yy - S / 2, xx - S / 2)
rays = (0.5 + 0.5 * np.cos(ang * 12)) ** 6
sh = np.clip(np.clip(1 - d, 0, 1) ** 1.5 * (0.45 + 0.55 * rays) + np.exp(-((d - 0.8) / 0.08) ** 2) * 0.8, 0, 1) * (d < 1)
a = np.zeros((S, S, 4)); a[..., :3] = 255 * sh[..., None]; a[..., 3] = 255 * sh
save(a, 'fx/shock.png')
# flame strip (tileable vertically) 32x128
W, H = 32, 128
yy, xx = np.mgrid[0:H, 0:W] + 0.5
n = np.zeros((H, W))
for k in range(4):
    fr = 2 ** k
    ph = rng.random() * 6
    n += np.sin(yy / H * math.tau * fr * 2 + np.sin(xx / W * math.tau + ph) * 1.5 + ph) / fr
cx = 1 - np.abs(xx - W / 2) / (W / 2)
f = np.clip(cx * 1.4 - 0.2 + n * 0.18, 0, 1) ** 1.3
col = np.stack([255 * np.ones_like(f), 120 + 135 * f, 30 + 160 * f ** 3], -1)
a = np.zeros((H, W, 4)); a[..., :3] = col * f[..., None]; a[..., 3] = 255 * f
save(a, 'fx/flame.png')
# beam strip 32x32 (tileable along v)
W, H = 32, 32
yy, xx = np.mgrid[0:H, 0:W] + 0.5
cx = 1 - np.abs(xx - W / 2) / (W / 2)
b = np.clip(cx ** 2 * (0.75 + 0.25 * np.sin(yy / H * math.tau * 2)), 0, 1)
a = np.zeros((H, W, 4)); a[..., :3] = 255 * b[..., None]; a[..., 3] = 255 * b
save(a, 'fx/beam.png')
# wind streaks (translucent white)
W, H = 64, 32
yy, xx = np.mgrid[0:H, 0:W] + 0.5
streak = np.zeros((H, W))
for _ in range(14):
    y0 = rng.random() * H; x0 = rng.random() * W; L = 10 + rng.random() * 30
    m = np.exp(-((yy - y0) / 0.9) ** 2) * np.clip(1 - np.abs(((xx - x0) % W) - L / 2) / (L / 2), 0, 1)
    streak = np.maximum(streak, m)
edge = np.clip(np.minimum(yy, H - yy) / 6, 0, 1)
a = np.zeros((H, W, 4)); a[..., :3] = 255; a[..., 3] = 200 * streak * edge
save(a, 'fx/wind.png')
# goo puddle
S = 64
d = radial(S)
yy, xx = np.mgrid[0:S, 0:S] + 0.5
ang = np.arctan2(yy - S / 2, xx - S / 2)
edge = 0.8 + 0.12 * np.sin(ang * 5 + 1) + 0.06 * np.sin(ang * 11)
m = (d < edge).astype(float)
col = np.zeros((S, S, 3)); col[:] = (110, 220, 40)
col[d < edge * 0.75] = (130, 235, 50)
hl = np.exp(-(((xx - S * 0.38) / 6) ** 2 + ((yy - S * 0.36) / 4) ** 2))
col = col * (1 - hl[..., None] * 0.6) + np.array([220, 255, 170]) * hl[..., None] * 0.6
rim = (d < edge) & (d > edge - 0.08)
col[rim] = (40, 120, 20)
a = np.zeros((S, S, 4)); a[..., :3] = col; a[..., 3] = 230 * m
save(a, 'fx/goo.png')
# smoke puff
S = 32
d = radial(S)
nz = rng.random((S, S))
img = Image.fromarray((nz * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2))
nz = np.asarray(img) / 255.0
m = np.clip((1 - d) * 1.6 - 0.2 + (nz - 0.5) * 0.8, 0, 1)
a = np.zeros((S, S, 4)); a[..., :3] = 255; a[..., 3] = 255 * m
save(a, 'fx/smoke.png')
# fireball core
S = 32
d = radial(S)
yy, xx = np.mgrid[0:S, 0:S] + 0.5
ang = np.arctan2(yy - S / 2, xx - S / 2)
edge = 0.75 + 0.18 * np.sin(ang * 7) * (0.5 + 0.5 * np.sin(ang * 3))
m = np.clip((edge - d) / 0.15, 0, 1)
t = np.clip(d / edge, 0, 1)
col = np.stack([255 * np.ones_like(t), 250 - 150 * t, 200 - 190 * t], -1)
a = np.zeros((S, S, 4)); a[..., :3] = col * m[..., None]; a[..., 3] = 255 * m
save(a, 'fx/fireball.png')
# acid blob
S = 32
d = radial(S)
m = np.clip((0.85 - d) / 0.1, 0, 1)
col = np.zeros((S, S, 3)); col[:] = (120, 230, 40)
hl = np.exp(-(((xx - S * 0.38) / 4) ** 2 + ((yy - S * 0.35) / 3) ** 2))
col = col * (1 - hl[..., None]) + np.array([235, 255, 200]) * hl[..., None]
col[(d > 0.7) & (d < 0.85)] = (50, 130, 20)
a = np.zeros((S, S, 4)); a[..., :3] = col; a[..., 3] = 255 * m
save(a, 'fx/acid.png')

# ---------------- GUI ----------------
S = 256
d = radial(S)
yy, xx = np.mgrid[0:S, 0:S] + 0.5
ang = np.arctan2(yy - S / 2, xx - S / 2)
col = np.zeros((S, S, 3)); alpha = np.zeros((S, S))
band = (d > 0.52) & (d < 0.98)
col[band] = (34, 38, 36)
seg = (np.abs(((ang / math.tau * 9) % 1) - 0.5) < 0.04) & band
col[seg] = (20, 22, 20)
col[(d > 0.95) & (d < 0.98)] = (120, 130, 125)
col[(d > 0.52) & (d < 0.555)] = (90, 230, 50)
col[(d > 0.9) & (d < 0.92)] = (60, 160, 40)
alpha[band] = 235
# grip notches
notch = (d > 0.98) & (d < 1.0) & (np.abs(((ang / math.tau * 36) % 1) - 0.5) < 0.25)
col[notch] = (70, 74, 72); alpha[notch] = 255
a = np.zeros((S, S, 4)); a[..., :3] = col; a[..., 3] = alpha
save(a, 'gui/dial.png')
S = 72
d = radial(S)
yy, xx = np.mgrid[0:S, 0:S] + 0.5
col = np.zeros((S, S, 3)); alpha = np.zeros((S, S))
col[d < 1] = (60, 64, 62); alpha[d < 1] = 255
col[d < 0.9] = (16, 40, 14)
g2 = np.clip(1 - d / 0.88, 0, 1)
col[d < 0.88] = np.array([40, 140, 30]) * g2[d < 0.88][:, None] + np.array([10, 30, 10]) * (1 - g2[d < 0.88][:, None])
a = np.zeros((S, S, 4)); a[..., :3] = col; a[..., 3] = alpha
save(a, 'gui/core.png')


def cutout(path):
    """white-background removal by flood fill from the borders, crop to content"""
    im = np.asarray(Image.open(path).convert('RGB')).astype(np.int32)
    H, W, _ = im.shape
    white = (im.min(axis=2) > 232)
    bg = np.zeros((H, W), bool)
    q = deque()
    for x in range(W):
        for y in (0, H - 1):
            if white[y, x] and not bg[y, x]:
                bg[y, x] = True; q.append((y, x))
    for y in range(H):
        for x in (0, W - 1):
            if white[y, x] and not bg[y, x]:
                bg[y, x] = True; q.append((y, x))
    while q:
        y, x = q.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < H and 0 <= nx < W and white[ny, nx] and not bg[ny, nx]:
                bg[ny, nx] = True; q.append((ny, nx))
    rgba = np.zeros((H, W, 4), np.uint8)
    rgba[..., :3] = im
    rgba[..., 3] = np.where(bg, 0, 255)
    img = Image.fromarray(rgba, 'RGBA')
    ys, xs = np.nonzero(~bg)
    return img.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def fit(img, S, pad=2, top=False):
    w, h = img.size
    k = (S - 2 * pad) / max(w, h)
    img = img.resize((max(1, int(w * k)), max(1, int(h * k))), Image.LANCZOS)
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out.paste(img, ((S - img.size[0]) // 2, pad if top else (S - img.size[1]) // 2), img)
    return out


REF = os.path.join(HERE, 'ref')
for name in ('heatblast', 'fourarms', 'diamondhead', 'xlr8', 'goop', 'cannonbolt', 'bigchill', 'echoecho', 'ghostfreak'):
    c = cutout(os.path.join(REF, name + '.jpg'))
    # portraits focus on the upper body/head so they read at 32 px
    w, h = c.size
    if h > w * 1.3:
        c = c.crop((0, 0, w, int(min(h, w * 1.25))))
    fit(c, 64, 2).save(os.path.join(RES, 'gui/portrait', name + '.png'))
om = cutout(os.path.join(REF, 'omnitrix.jpg'))
fit(om, 32, 1).save(os.path.join(RES, 'item/omnitrix.png'))
fit(om, 18, 0).save(os.path.join(RES, 'gui/badge.png'))

# ---------------- temp blocks ----------------
S = 16
yy, xx = np.mgrid[0:S, 0:S]
col = np.zeros((S, S, 3)); col[:] = (110, 235, 175)
facet = ((xx + yy) % 6 < 1) | ((xx - yy) % 7 < 1)
col[facet] = (200, 255, 225)
edge = (xx == 0) | (yy == 0) | (xx == S - 1) | (yy == S - 1)
col[edge] = (40, 150, 100)
a = np.zeros((S, S, 4)); a[..., :3] = col; a[..., 3] = np.where(edge, 240, 170)
save(a, 'block/crystal_wall.png')
col = np.zeros((S, S, 3)); col[:] = (190, 225, 255)
crack = ((xx * 3 + yy * 5) % 11 < 1) | ((xx * 7 - yy * 2) % 13 < 1)
col[crack] = (250, 255, 255)
col[edge] = (120, 170, 230)
a = np.zeros((S, S, 4)); a[..., :3] = col; a[..., 3] = np.where(edge, 230, 160)
save(a, 'block/frost_ice.png')
print('assets ok')
