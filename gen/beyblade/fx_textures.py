"""Effect textures (white/greyscale, tinted by vertex colour; rendered additively) + launcher item texture."""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade/textures')
R = np.random.default_rng(11)


def save(arr, name, sub='fx'):
    os.makedirs(os.path.join(OUT, sub), exist_ok=True)
    a = np.clip(arr, 0, 1)
    if a.ndim == 2:
        img = np.dstack([a, a, a, a])  # rgb for additive, alpha for translucent use
    else:
        img = a
    Image.fromarray((img * 255).astype(np.uint8), 'RGBA').save(os.path.join(OUT, sub, name + '.png'))


def tile_noise(n, freq, octaves=4, seed=0):
    rng = np.random.default_rng(seed)
    out = np.zeros((n, n))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        f = freq * 2 ** o
        g = rng.random((f, f))
        ys = np.arange(n) / n * f
        y0 = np.floor(ys).astype(int); fy = ys - y0
        fy = fy * fy * (3 - 2 * fy)
        a = g[y0 % f][:, y0 % f]; b = g[y0 % f][:, (y0 + 1) % f]
        c = g[(y0 + 1) % f][:, y0 % f]; d = g[(y0 + 1) % f][:, (y0 + 1) % f]
        fx = fy[None, :]; fyy = fy[:, None]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fyy) + (c * (1 - fx) + d * fx) * fyy)
        tot += amp
        amp *= 0.5
    return out / tot


# soft band: u = along (tiles), v = across. bright core, soft edges, faint streaks
def soft():
    n = 128
    v = (np.arange(n) + 0.5) / n
    prof = np.sin(np.pi * v) ** 2.2
    core = np.exp(-((v - 0.5) / 0.08) ** 2) * 0.6
    streak = 0.75 + 0.25 * tile_noise(n, 4, 3, 1)
    a = (prof + core)[:, None] * streak
    return a / a.max()


# hard-edged wave front: bright leading edge (v=1) with a decaying tail toward v=0
def wave():
    n = 128
    v = (np.arange(n) + 0.5) / n
    prof = np.exp(-(1 - v) * 5.0) * (1 - np.exp(-(1 - v) * 60)) * 1.15
    prof = np.where(v > 0.985, prof * (1 - v) / 0.015, prof)
    ripple = 0.85 + 0.15 * np.sin(v * 40)
    streak = 0.8 + 0.2 * tile_noise(n, 8, 2, 2)
    a = (prof * ripple)[:, None] * streak
    return a / a.max()


def hexgrid():
    n = 256
    img = Image.new('L', (n, n), 0)
    d = ImageDraw.Draw(img)
    s = n / 8                      # hex size so the pattern tiles: 8 columns
    w = s
    h = s * math.sqrt(3) / 2
    # use a pattern that tiles: width n = 8*w*0.75*... simpler: flat-top hexes, columns spaced 1.5*r, rows sqrt3*r
    r = n / 12.0
    cols = 8; rows = int(round(n / (math.sqrt(3) * r)))
    r = n / (cols * 1.5)
    rh = n / rows / math.sqrt(3)
    for ci in range(-1, cols + 2):
        for ri in range(-1, rows + 2):
            cx = ci * 1.5 * r
            cy = ri * math.sqrt(3) * rh + (ci % 2) * math.sqrt(3) * rh / 2
            pts = [(cx + r * 0.94 * math.cos(math.radians(60 * k)), cy + rh * 0.94 * math.sin(math.radians(60 * k)) * 1.0) for k in range(7)]
            d.line(pts, fill=255, width=3)
    a = np.asarray(img.filter(ImageFilter.GaussianBlur(1.6))).astype(np.float32) / 255
    a2 = np.asarray(img).astype(np.float32) / 255
    a = np.maximum(a * 1.6, a2 * 0.9)
    return np.clip(a + 0.08, 0, 1)


def noise_wisp():
    n = 128
    a = tile_noise(n, 4, 5, 3)
    # vertically stretched flame / sand wisps: sample with stretched coordinates
    b = tile_noise(n, 3, 4, 4)
    w = np.clip((a * 0.6 + b * 0.6 - 0.45) * 2.4, 0, 1) ** 1.6
    return w


def flare():
    n = 128
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.hypot(xx - n / 2 + 0.5, yy - n / 2 + 0.5) / (n / 2)
    core = np.exp(-(r / 0.18) ** 2)
    halo = np.exp(-(r / 0.5) ** 2) * 0.55
    ang = np.arctan2(yy - n / 2, xx - n / 2)
    rays = (np.abs(np.cos(ang * 4)) ** 30 + np.abs(np.cos(ang * 4 + 0.4)) ** 60 * 0.5) * np.exp(-r * 2.4) * 0.7
    a = np.clip(core + halo + rays, 0, 1) * np.clip(1 - r, 0, 1) ** 0.5
    return a


def sigil():
    """Libra judgement sigil: concentric rings, rune ticks, the ♎ glyph and a balance scale."""
    n = 1024
    img = Image.new('L', (n, n), 0)
    d = ImageDraw.Draw(img)
    c = n / 2
    for rr, w in ((500, 10), (470, 4), (430, 6), (300, 5), (280, 3), (150, 6)):
        d.ellipse((c - rr, c - rr, c + rr, c + rr), outline=255, width=w)
    # rune ticks between rings
    for k in range(72):
        a = math.radians(k * 5)
        l1, l2 = (436, 466) if k % 3 else (436, 495)
        d.line((c + l1 * math.cos(a), c + l1 * math.sin(a), c + l2 * math.cos(a), c + l2 * math.sin(a)), fill=255, width=4 if k % 3 else 7)
    # twelve zodiac-style glyph marks in the band
    for k in range(12):
        a = math.radians(k * 30 + 15)
        x, y = c + 365 * math.cos(a), c + 365 * math.sin(a)
        s = 34
        d.arc((x - s, y - s, x + s, y + s), 200, 340, fill=255, width=6)
        d.line((x - s, y + s * 0.5, x + s, y + s * 0.5), fill=255, width=6)
        d.line((x, y - s * 0.2, x, y + s * 0.5), fill=255, width=5)
    # hexagram between ring 300 and 150
    for off in (90, 30):
        pts = [(c + 290 * math.cos(math.radians(off + 120 * k)), c + 290 * math.sin(math.radians(off + 120 * k))) for k in range(4)]
        d.line(pts, fill=255, width=6)
    # central libra glyph: arc over a line + base line
    s = 110
    d.arc((c - s, c - s * 1.25, c + s, c + s * 0.75), 200, 340, fill=255, width=18)
    d.line((c - s * 1.15, c + s * 0.12, c - s * 0.42, c + s * 0.12), fill=255, width=18)
    d.line((c + s * 0.42, c + s * 0.12, c + s * 1.15, c + s * 0.12), fill=255, width=18)
    d.line((c - s * 1.15, c + s * 0.55, c + s * 1.15, c + s * 0.55), fill=255, width=18)
    a = np.asarray(img).astype(np.float32) / 255
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(9))).astype(np.float32) / 255
    out = np.clip(a * 0.95 + glow * 1.4, 0, 1)
    return np.asarray(Image.fromarray((out * 255).astype(np.uint8)).resize((512, 512), Image.LANCZOS)).astype(np.float32) / 255


def lightning():
    """vertical jagged bolt strip, u across, v along (tiles vertically)"""
    n = 128
    a = np.zeros((n, 64))
    x = 32.0
    for y in range(n):
        x += R.normal(0, 1.6)
        x = 32 + (x - 32) * 0.92
        xs = np.arange(64)
        a[y] = np.maximum(a[y], np.exp(-((xs - x) / 2.2) ** 2) + 0.35 * np.exp(-((xs - x) / 9) ** 2))
    # make it tile: blend the end into the start
    m = 16
    a[-m:] = a[-m:] * np.linspace(1, 0, m)[:, None] + a[:m] * np.linspace(0, 1, m)[:, None]
    return np.clip(a, 0, 1)


def claw():
    """three parallel curved claw slashes, pointed ends"""
    n = 256
    img = Image.new('L', (n, n), 0)
    d = ImageDraw.Draw(img)
    for k in (-1, 0, 1):
        pts = []
        for i in range(41):
            f = i / 40
            x = 30 + f * 196
            y = 128 + k * 46 + math.sin(f * math.pi) * -38 + f * 20
            pts.append((x, y))
        for i in range(40):
            f = i / 40
            w = int(1 + 13 * math.sin(f * math.pi) ** 0.8)
            d.line([pts[i], pts[i + 1]], fill=255, width=w)
    a = np.asarray(img).astype(np.float32) / 255
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(7))).astype(np.float32) / 255
    return np.clip(a + glow * 1.2, 0, 1)


def fang():
    """single wind fang: sharp crescent with a soft glow"""
    n = 128
    img = Image.new('L', (n, n), 0)
    d = ImageDraw.Draw(img)
    pts = []
    for i in range(41):
        f = i / 40
        a = math.pi * (0.15 + 0.7 * f)
        pts.append((64 + math.cos(a) * 46, 84 - math.sin(a) * 46))
    for i in range(40):
        f = i / 40
        d.line([pts[i], pts[i + 1]], fill=255, width=int(1 + 10 * math.sin(f * math.pi) ** 0.7))
    a = np.asarray(img).astype(np.float32) / 255
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(5))).astype(np.float32) / 255
    return np.clip(a + glow * 1.2, 0, 1)


def eagle():
    """spectral eagle, wings raised and spread (front view, head in profile); rgb = glow, alpha = silhouette"""
    n = 512
    img = Image.new('L', (n, n), 0)
    d = ImageDraw.Draw(img)
    c = n / 2
    for side in (-1, 1):
        def P(x, y):
            return (c + side * x, y)
        # inner wing (coverts) + arm sweeping up and out
        d.polygon([P(20, 230), P(70, 170), P(150, 110), P(215, 70), P(240, 78), P(205, 140), P(150, 215), P(60, 300), P(25, 300)], fill=255)
        # primaries: long separated fingers at the wing tip
        for k in range(5):
            bx, by = 205 - k * 22, 95 + k * 22
            ang = math.radians(-35 + k * 14)
            L = 95 - k * 8
            tx, ty = bx + math.cos(ang) * L, by + math.sin(ang) * L - 40
            d.polygon([P(bx - 9, by + 6), P(bx + 9, by - 6), P(tx + 3, ty), P(tx - 4, ty + 6)], fill=255)
        # secondaries: ragged trailing edge
        for k in range(7):
            x0 = 175 - k * 21
            y0 = 160 + k * 21
            d.polygon([P(x0 - 12, y0 - 10), P(x0 + 12, y0 - 18), P(x0 + 2, y0 + 48 - k * 3)], fill=255)
    # body
    d.polygon([(c - 34, 215), (c + 34, 215), (c + 30, 330), (c, 350), (c - 30, 330)], fill=255)
    # tail fan
    for k in range(-3, 4):
        a = math.radians(90 + k * 11)
        d.polygon([(c - 10, 330), (c + 10, 330), (c + math.cos(a) * 95 + 9, 335 + math.sin(a) * 95), (c + math.cos(a) * 95 - 9, 335 + math.sin(a) * 95)], fill=255)
    # head in profile with hooked beak, looking left
    d.ellipse((c - 42, 140, c + 36, 222), fill=255)
    d.polygon([(c - 36, 168), (c - 78, 172), (c - 92, 186), (c - 86, 208), (c - 76, 196), (c - 40, 202)], fill=255)   # hooked beak
    d.polygon([(c + 26, 160), (c + 62, 140), (c + 40, 176)], fill=255)     # crest feathers
    d.polygon([(c + 18, 150), (c + 46, 118), (c + 32, 160)], fill=255)
    d.polygon([(c + 30, 182), (c + 66, 176), (c + 38, 196)], fill=255)
    d.ellipse((c - 26, 164, c - 12, 178), fill=0)                          # eye
    d.line([(c - 44, 160), (c - 6, 158)], fill=0, width=4)                 # fierce brow
    d.line([(c - 80, 190), (c - 50, 190)], fill=0, width=2)                # beak line
    sharp = np.asarray(img).astype(np.float32) / 255
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(10))).astype(np.float32) / 255
    rgb = np.clip(sharp * 0.85 + glow * 0.9, 0, 1)
    out = np.dstack([rgb, rgb, rgb, sharp])
    return np.asarray(Image.fromarray((out * 255).astype(np.uint8), 'RGBA').resize((256, 256), Image.LANCZOS)).astype(np.float32) / 255


def feather():
    """single metallic blade-feather, pointing up (u across, v along)"""
    n = 128
    img = Image.new('L', (64, n), 0)
    d = ImageDraw.Draw(img)
    d.polygon([(32, 4), (46, 40), (42, 100), (32, 124), (22, 100), (18, 40)], fill=255)
    d.line([(32, 8), (32, 120)], fill=120, width=2)
    sharp = np.asarray(img).astype(np.float32) / 255
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(4))).astype(np.float32) / 255
    rgb = np.clip(sharp * 0.9 + glow * 0.8, 0, 1)
    return np.dstack([rgb, rgb, rgb, np.maximum(sharp, glow * 0.6)])


def launcher_tex():
    """64x64 palette sheet for the launcher JSON model"""
    P = np.zeros((32, 32, 4), np.float32)
    P[..., 3] = 1

    def fill(x0, y0, w, h, col, noise=0.04, grad=0.15):
        c = np.array(col, np.float32) / 255
        yy = np.arange(h)[:, None] / max(1, h - 1)
        sh = 1 + grad * (0.5 - yy) + R.normal(0, noise, (h, w))
        P[y0:y0 + h, x0:x0 + w, :3] = np.clip(c[None, None] * sh[..., None], 0, 1)

    fill(0, 0, 16, 16, (210, 214, 222))     # white/silver body
    fill(16, 0, 16, 16, (150, 220, 40))     # libra lime-green accent
    fill(0, 16, 16, 16, (40, 44, 52))       # dark grip
    fill(16, 16, 8, 8, (240, 170, 30))      # orange
    fill(24, 16, 8, 8, (60, 80, 200))       # navy-blue
    fill(16, 24, 16, 8, (120, 124, 132))    # metal grey
    # grip ridges
    for y in range(17, 32, 3):
        P[y, 0:16, :3] *= 0.6
    # gear teeth hint on the silver body
    for x in range(1, 16, 3):
        P[1, x, :3] *= 0.7
    return P


def ldrago_launcher_tex():
    """same layout as launcher_tex but in L-Drago colours (left-spin launcher)"""
    P = launcher_tex()
    swap = {(0, 0): (60, 62, 70), (16, 0): (210, 20, 60), (0, 16): (25, 25, 30), (16, 16): (240, 200, 40), (24, 16): (230, 230, 235), (16, 24): (150, 150, 158)}
    for (x0, y0), col in swap.items():
        w = 16 if (x0, y0) in ((0, 0), (16, 0), (0, 16), (16, 24)) else 8
        h = 16 if (x0, y0) in ((0, 0), (16, 0), (0, 16)) else 8
        blk = P[y0:y0 + h, x0:x0 + w, :3]
        lum = blk.mean(axis=2, keepdims=True)
        P[y0:y0 + h, x0:x0 + w, :3] = np.clip(np.array(col) / 255.0 * (lum / max(1e-3, lum.mean())), 0, 1)
    return P


def leone_launcher_tex():
    """launcher in Rock Leone colours"""
    P = launcher_tex()
    swap = {(0, 0): (225, 228, 232), (16, 0): (30, 150, 60), (0, 16): (40, 60, 45), (16, 16): (220, 190, 90), (24, 16): (120, 80, 40), (16, 24): (140, 142, 150)}
    for (x0, y0), col in swap.items():
        w = 16 if (x0, y0) in ((0, 0), (16, 0), (0, 16), (16, 24)) else 8
        h = 16 if (x0, y0) in ((0, 0), (16, 0), (0, 16)) else 8
        blk = P[y0:y0 + h, x0:x0 + w, :3]
        lum = blk.mean(axis=2, keepdims=True)
        P[y0:y0 + h, x0:x0 + w, :3] = np.clip(np.array(col) / 255.0 * (lum / max(1e-3, lum.mean())), 0, 1)
    return P


def eagle_launcher_tex():
    """launcher in Earth Eagle colours"""
    P = launcher_tex()
    swap = {(0, 0): (200, 202, 210), (16, 0): (120, 60, 200), (0, 16): (40, 30, 60), (16, 16): (210, 50, 50), (24, 16): (240, 200, 80), (16, 24): (170, 172, 180)}
    for (x0, y0), col in swap.items():
        w = 16 if (x0, y0) in ((0, 0), (16, 0), (0, 16), (16, 24)) else 8
        h = 16 if (x0, y0) in ((0, 0), (16, 0), (0, 16)) else 8
        blk = P[y0:y0 + h, x0:x0 + w, :3]
        lum = blk.mean(axis=2, keepdims=True)
        P[y0:y0 + h, x0:x0 + w, :3] = np.clip(np.array(col) / 255.0 * (lum / max(1e-3, lum.mean())), 0, 1)
    return P


if __name__ == '__main__':
    save(soft(), 'soft')
    save(wave(), 'wave')
    save(hexgrid(), 'hex')
    save(noise_wisp(), 'wisp')
    save(flare(), 'flare')
    save(sigil(), 'sigil')
    save(lightning(), 'bolt')
    save(claw(), 'claw')
    save(fang(), 'fang')
    save(eagle(), 'eagle')
    save(feather(), 'feather')
    save(eagle_launcher_tex(), 'eagle_launcher', 'item')
    save(leone_launcher_tex(), 'leone_launcher', 'item')
    save(ldrago_launcher_tex(), 'ldrago_launcher', 'item')
    save(launcher_tex(), 'launcher', 'item')
    print('fx ok')
