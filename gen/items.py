"""Item + HUD textures for the player boss form: Kalp Kırıcı Kılıcı (32x32 handheld) and 5 ability icons (64x64)."""
import os, math, numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolasyonbosses/textures')


def heart(x, y, cx, cy, s):
    X = (x - cx) / s
    Y = -(y - cy) / s
    return (X * X + Y * Y - 1) ** 3 - X * X * Y ** 3 <= 0


def sword():
    S = 8
    N = 32 * S
    yy, xx = (np.mgrid[0:N, 0:N] + 0.5) / S
    a = np.array([3.2, 28.8]); b = np.array([29.5, 2.5])
    ax = (b - a) / np.linalg.norm(b - a)
    pe = np.array([ax[1], -ax[0]])
    u = (xx - a[0]) * ax[0] + (yy - a[1]) * ax[1]
    v = (xx - a[0]) * pe[0] + (yy - a[1]) * pe[1]
    L = np.linalg.norm(b - a)
    col = np.zeros((N, N, 3)); alpha = np.zeros((N, N), bool)

    def put(mask, c):
        nonlocal col, alpha
        col[mask] = c; alpha |= mask

    # pommel thorn + grip
    pom = ((u - 1.4) ** 2 + v ** 2 < 1.9 ** 2)
    put(pom, (120, 16, 34))
    put(pom & ((u - 1.0) ** 2 + (v + 0.5) ** 2 < 0.6), (240, 90, 100))
    grip = (u > 2.6) & (u < 8.4) & (np.abs(v) < 1.15)
    put(grip, (38, 12, 18))
    put(grip & (((u - 2.6) % 1.6) < 0.55), (150, 28, 44))
    # horned crossguard curving toward the tip
    for sd in (-1, 1):
        vv = v * sd
        gu = 9.2 + np.clip(vv - 1, 0, None) ** 1.7 * 0.33
        g = (vv > -0.5) & (vv < 6.4) & (np.abs(u - gu) < 1.25 - np.clip(vv - 1, 0, None) * 0.14)
        put(g, (150, 22, 40))
        put(g & (u - gu < -0.35), (225, 60, 72))
    # blade
    bu = (u - 10.2) / (L - 10.2)
    w = 3.0 * (1 - np.clip(bu, 0, 1) ** 1.4) + 0.15
    blade = (u > 10.2) & (u < L) & (np.abs(v) < w)
    put(blade, (26, 8, 14))
    edge = blade & (np.abs(v) > w - 0.6)
    put(edge, (205, 38, 56))
    put(blade & (np.abs(v) > w - 0.3) & (v < 0), (255, 140, 150))
    put(blade & (np.abs(v) < 0.35) & (bu < 0.8), (120, 10, 30))
    # thorn barbs on the back edge, pointing to the hilt
    for k, bu0 in enumerate((0.12, 0.3, 0.48, 0.64)):
        u0 = 10.2 + bu0 * (L - 10.2)
        w0 = 3.0 * (1 - bu0 ** 1.4)
        t = np.clip((u0 - u) / 2.6, -1, 2)
        barb = (t > 0) & (t < 1) & (v > w0 - 0.4) & (v < w0 + 1.7 * (1 - t) ** 1.3 - 0.2) & (u < u0)
        put(barb, (30, 8, 14))
        put(barb & (v > w0 + 1.7 * (1 - t) ** 1.3 - 0.9), (170, 30, 48))
    # heart gem in the guard
    hg = heart(u, -v, 9.4, 0.15, 1.35)
    put(hg, (255, 120, 140))
    put(heart(u, -v, 9.2, 0.35, 0.6), (255, 225, 230))
    img = np.zeros((N, N, 4))
    img[..., :3] = col; img[..., 3] = alpha * 255
    small = Image.fromarray(img.astype(np.uint8), 'RGBA').resize((32, 32), Image.LANCZOS)
    arr = np.asarray(small).astype(float)
    arr[..., 3] = np.where(arr[..., 3] > 90, 255, 0)
    # dark outline for readability
    m = arr[..., 3] > 0
    out = np.zeros_like(m)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        out |= np.roll(np.roll(m, dx, 1), dy, 0)
    out &= ~m
    arr[out] = (20, 4, 10, 255)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')


def icon_base(S=256):
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:S, 0:S] / S - 0.5
    r = np.sqrt(xx ** 2 + yy ** 2)
    base = np.zeros((S, S, 4))
    t = np.clip(r / 0.5, 0, 1)
    base[..., 0] = 90 * (1 - t) + 18 * t
    base[..., 1] = 10 * (1 - t) + 4 * t
    base[..., 2] = 24 * (1 - t) + 8 * t
    base[..., 3] = 255
    im = Image.fromarray(base.astype(np.uint8), 'RGBA')
    d = ImageDraw.Draw(im)
    d.rectangle((4, 4, S - 5, S - 5), outline=(160, 28, 48, 255), width=10)
    d.rectangle((16, 16, S - 17, S - 17), outline=(40, 8, 16, 255), width=4)
    return im, d


def thorn_path(d, pts, width, col, barbs=True, rng=None):
    rng = rng or np.random.default_rng(3)
    for i in range(len(pts) - 1):
        w = int(width * (1 - i / len(pts)) + 3)
        d.line([pts[i], pts[i + 1]], fill=col, width=w)
        if barbs and i % 3 == 1:
            x, y = pts[i]; x2, y2 = pts[i + 1]
            ang = math.atan2(y2 - y, x2 - x) + (1 if i % 2 else -1) * 2.2
            d.polygon([(x - 5, y), (x + 5, y), (x + math.cos(ang) * 28, y + math.sin(ang) * 28)], fill=col)


def glowify(im):
    g = im.filter(ImageFilter.GaussianBlur(10))
    return Image.alpha_composite(g, im)


def icons():
    S = 256
    out = {}
    # 1 thorn whip
    im, d = icon_base()
    layer = Image.new('RGBA', (S, S)); ld = ImageDraw.Draw(layer)
    pts = [(40 + k * 7, 200 - k * 6 - 45 * math.sin(k * 0.35)) for k in range(26)]
    thorn_path(ld, pts, 20, (255, 70, 90, 255))
    thorn_path(ld, [(x + 4, y + 4) for x, y in pts], 12, (20, 4, 10, 255), barbs=False)
    im = Image.alpha_composite(im, glowify(layer)); out['whip'] = im
    # 2 heart volley
    im, d = icon_base()
    layer = Image.new('RGBA', (S, S)); ld = ImageDraw.Draw(layer)
    yy, xx = np.mgrid[0:S, 0:S]
    arr = np.zeros((S, S, 4))
    for cx, cy, s, c in ((88, 150, 38, (255, 90, 120)), (170, 110, 30, (255, 150, 170)), (150, 190, 22, (255, 60, 90)), (70, 70, 18, (255, 190, 200))):
        m = heart(xx, yy, cx, cy, s)
        arr[m] = (*c, 255)
        m2 = heart(xx, yy, cx - s * 0.25, cy - s * 0.25, s * 0.3)
        arr[m2] = (255, 240, 240, 255)
    for k in range(4):
        ld.line([(40 + k * 10, 230 - k * 4), (80 + k * 25, 190 - k * 20)], fill=(255, 80, 100, 160), width=4)
    layer = Image.alpha_composite(layer, Image.fromarray(arr.astype(np.uint8), 'RGBA'))
    im = Image.alpha_composite(im, glowify(layer)); out['hearts'] = im
    # 3 shadow dash
    im, d = icon_base()
    layer = Image.new('RGBA', (S, S)); ld = ImageDraw.Draw(layer)
    for k, (y, L) in enumerate(((80, 120), (118, 170), (156, 140), (192, 100))):
        ld.line([(210 - L, y), (210, y)], fill=(255, 60, 80, 255) if k % 2 else (40, 8, 16, 255), width=14)
    ld.polygon([(150, 60), (230, 128), (150, 196), (175, 128)], fill=(20, 4, 10, 255), outline=(255, 90, 110, 255), width=6)
    im = Image.alpha_composite(im, glowify(layer)); out['dash'] = im
    # 4 scythe tempest
    im, d = icon_base()
    layer = Image.new('RGBA', (S, S)); ld = ImageDraw.Draw(layer)
    for k in range(3):
        a0 = k * 120
        ld.arc((40 + k * 18, 40 + k * 18, 216 - k * 18, 216 - k * 18), a0, a0 + 200, fill=(255, 70 + k * 40, 90 + k * 30, 255), width=18 - k * 4)
    ld.ellipse((108, 108, 148, 148), fill=(20, 4, 10, 255), outline=(255, 80, 100, 255), width=5)
    im = Image.alpha_composite(im, glowify(layer)); out['spin'] = im
    # 5 heart judgement (leap slam)
    im, d = icon_base()
    layer = Image.new('RGBA', (S, S)); ld = ImageDraw.Draw(layer)
    arr = np.zeros((S, S, 4))
    m = heart(xx, yy, 128, 96, 48)
    arr[m] = (230, 40, 64, 255)
    crack = (np.abs(xx - 128 - 10 * np.sin(yy / 9.0)) < 4) & m
    arr[crack] = (255, 235, 235, 255)
    layer = Image.fromarray(arr.astype(np.uint8), 'RGBA'); ld = ImageDraw.Draw(layer)
    for k in range(-3, 4):
        x = 128 + k * 26
        ld.polygon([(x - 11, 222), (x + 11, 222), (x + k * 4, 222 - (60 - abs(k) * 12))], fill=(26, 6, 12, 255), outline=(255, 70, 90, 255))
    ld.line([(30, 222), (226, 222)], fill=(255, 70, 90, 255), width=6)
    im = Image.alpha_composite(im, glowify(layer)); out['judgement'] = im
    return {k: v.resize((64, 64), Image.LANCZOS) for k, v in out.items()}


if __name__ == '__main__':
    os.makedirs(os.path.join(ROOT, 'item'), exist_ok=True)
    os.makedirs(os.path.join(ROOT, 'gui'), exist_ok=True)
    sword().save(os.path.join(ROOT, 'item/heartbreaker_blade.png'))
    for k, im in icons().items():
        im.save(os.path.join(ROOT, f'gui/ability_{k}.png'))
    print('items ok')
