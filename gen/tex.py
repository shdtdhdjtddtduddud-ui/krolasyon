"""Procedural painting library used by every texture generator.

Images are float32 numpy arrays (H, W, 4) in 0..1. Materials paint a face of a given pixel size and return an image.
"""
import math
import numpy as np
from PIL import Image

RNG = np.random.default_rng(1234)


def rng(seed):
    return np.random.default_rng(seed & 0xFFFFFFFF)


def hexc(h, a=1.0):
    h = h.lstrip('#')
    return np.array([int(h[0:2], 16) / 255, int(h[2:4], 16) / 255, int(h[4:6], 16) / 255, a], dtype=np.float32)


def new(w, h, color=(0, 0, 0, 0)):
    img = np.zeros((h, w, 4), dtype=np.float32)
    img[:, :] = color
    return img


def save(img, path):
    a = np.clip(img * 255 + 0.5, 0, 255).astype(np.uint8)
    Image.fromarray(a, 'RGBA').save(path)


def load(path):
    return np.asarray(Image.open(path).convert('RGBA')).astype(np.float32) / 255


# ---------------------------------------------------------------- noise

def value_noise(w, h, cell, seed, octaves=3, persistence=0.5):
    """Smooth multi-octave value noise in 0..1."""
    r = rng(seed)
    out = np.zeros((h, w), dtype=np.float32)
    amp, total = 1.0, 0.0
    c = max(1.0, float(cell))
    for _ in range(octaves):
        gw, gh = int(w / c) + 3, int(h / c) + 3
        grid = r.random((gh, gw)).astype(np.float32)
        ys = np.arange(h) / c
        xs = np.arange(w) / c
        y0 = np.floor(ys).astype(int)
        x0 = np.floor(xs).astype(int)
        fy = (ys - y0)[:, None]
        fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy)
        fx = fx * fx * (3 - 2 * fx)
        a = grid[y0][:, x0]
        b = grid[y0][:, x0 + 1]
        cc = grid[y0 + 1][:, x0]
        d = grid[y0 + 1][:, x0 + 1]
        out += amp * (a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + cc * (1 - fx) * fy + d * fx * fy)
        total += amp
        amp *= persistence
        c = max(1.0, c / 2)
    return out / total


def white(w, h, seed):
    return rng(seed).random((h, w)).astype(np.float32)


def mix(a, b, t):
    t = np.asarray(t, dtype=np.float32)
    if t.ndim == 2:
        t = t[:, :, None]
    return a * (1 - t) + b * t


def shade(img, f):
    out = img.copy()
    f = np.asarray(f, dtype=np.float32)
    if f.ndim == 2:
        f = f[:, :, None]
    out[:, :, :3] = np.clip(out[:, :, :3] * f, 0, 1)
    return out


def colorize(base, t, lo=0.75, hi=1.2):
    """Base colour modulated by a 0..1 field."""
    h, w = t.shape
    img = np.zeros((h, w, 4), dtype=np.float32)
    img[:, :, :3] = np.clip(base[:3][None, None, :] * (lo + (hi - lo) * t[:, :, None]), 0, 1)
    img[:, :, 3] = base[3]
    return img


def ramp(colors, t):
    """Map a 0..1 field through a list of colours."""
    t = np.clip(t, 0, 1)
    n = len(colors) - 1
    idx = np.minimum((t * n).astype(int), n - 1)
    f = (t * n - idx)[:, :, None]
    cs = np.stack(colors)
    return cs[idx] * (1 - f) + cs[idx + 1] * f


def edge_dark(img, amount=0.25, width=1):
    h, w = img.shape[:2]
    f = np.ones((h, w), dtype=np.float32)
    for i in range(width):
        k = 1 - amount * (1 - i / width)
        f[i, :] = np.minimum(f[i, :], k)
        f[h - 1 - i, :] = np.minimum(f[h - 1 - i, :], k)
        f[:, i] = np.minimum(f[:, i], k)
        f[:, w - 1 - i] = np.minimum(f[:, w - 1 - i], k)
    return shade(img, f)


def vgrad(img, top=1.08, bottom=0.82):
    h = img.shape[0]
    t = np.linspace(0, 1, h, dtype=np.float32)[:, None]
    return shade(img, np.broadcast_to(top + (bottom - top) * t, img.shape[:2]))


def paste(dst, src, x, y):
    h, w = src.shape[:2]
    H, W = dst.shape[:2]
    x0, y0 = max(0, x), max(0, y)
    x1, y1 = min(W, x + w), min(H, y + h)
    if x1 <= x0 or y1 <= y0:
        return
    s = src[y0 - y:y1 - y, x0 - x:x1 - x]
    a = s[:, :, 3:4]
    d = dst[y0:y1, x0:x1]
    out_a = a + d[:, :, 3:4] * (1 - a)
    rgb = (s[:, :, :3] * a + d[:, :, :3] * d[:, :, 3:4] * (1 - a)) / np.maximum(out_a, 1e-6)
    dst[y0:y1, x0:x1, :3] = rgb
    dst[y0:y1, x0:x1, 3:4] = out_a


def put(dst, src, x, y):
    """Copy without blending."""
    h, w = src.shape[:2]
    dst[y:y + h, x:x + w] = src


def rect(img, x0, y0, x1, y1, c):
    h, w = img.shape[:2]
    x0, y0, x1, y1 = max(0, int(x0)), max(0, int(y0)), min(w, int(x1)), min(h, int(y1))
    if x1 > x0 and y1 > y0:
        if c[3] >= 1:
            img[y0:y1, x0:x1] = c
        else:
            region = img[y0:y1, x0:x1]
            region[:, :, :3] = region[:, :, :3] * (1 - c[3]) + c[:3] * c[3]
            region[:, :, 3] = np.maximum(region[:, :, 3], c[3])


def ellipse_mask(w, h, cx, cy, rx, ry, soft=0.0):
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    d = ((xs + 0.5 - cx) / max(rx, 1e-3)) ** 2 + ((ys + 0.5 - cy) / max(ry, 1e-3)) ** 2
    if soft <= 0:
        return (d <= 1).astype(np.float32)
    return np.clip((1 + soft - d) / soft, 0, 1)


def draw_mask(img, mask, c):
    m = mask[:, :, None] * c[3]
    img[:, :, :3] = img[:, :, :3] * (1 - m) + c[:3] * m
    img[:, :, 3] = np.maximum(img[:, :, 3], mask * c[3])


def line(img, x0, y0, x1, y1, c, width=1.0):
    h, w = img.shape[:2]
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32) + 0.5
    dx, dy = x1 - x0, y1 - y0
    L2 = dx * dx + dy * dy + 1e-6
    t = np.clip(((xs - x0) * dx + (ys - y0) * dy) / L2, 0, 1)
    px, py = x0 + t * dx, y0 + t * dy
    d = np.sqrt((xs - px) ** 2 + (ys - py) ** 2)
    m = np.clip(width / 2 + 0.5 - d, 0, 1)
    draw_mask(img, m, c)


def resize(img, w, h):
    a = np.clip(img * 255 + 0.5, 0, 255).astype(np.uint8)
    im = Image.fromarray(a, 'RGBA').resize((w, h), Image.LANCZOS)
    return np.asarray(im).astype(np.float32) / 255


def nearest(img, w, h):
    a = np.clip(img * 255 + 0.5, 0, 255).astype(np.uint8)
    im = Image.fromarray(a, 'RGBA').resize((w, h), Image.NEAREST)
    return np.asarray(im).astype(np.float32) / 255


# ---------------------------------------------------------------- materials
# every material: f(w, h, seed, face) -> image ; face in {'top','bottom','front','back','left','right'}

def m_skin(base, spots=None, var=0.18):
    base = hexc(base) if isinstance(base, str) else base

    def f(w, h, seed, face):
        n = value_noise(w, h, max(2, w / 4), seed, 3)
        fine = white(w, h, seed + 1)
        t = 0.5 + (n - 0.5) * 1.6 + (fine - 0.5) * 0.12
        img = colorize(base, np.clip(t, 0, 1), 1 - var, 1 + var * 0.7)
        if spots:
            sp = value_noise(w, h, max(2, w / 6), seed + 9, 2)
            draw_mask(img, np.clip((sp - 0.62) * 6, 0, 1) * 0.6, hexc(spots))
        return vgrad(img, 1.08, 0.86) if face not in ('top', 'bottom') else img
    return f


def m_fur(base, tip=None, dark=None, length=3):
    base = hexc(base)
    tipc = hexc(tip) if tip else base * np.array([1.2, 1.2, 1.2, 1])
    darkc = hexc(dark) if dark else base * np.array([0.55, 0.55, 0.6, 1])

    def f(w, h, seed, face):
        r = rng(seed)
        strands = value_noise(w, h * 4, 1.2, seed, 1)
        strands = np.asarray(Image.fromarray((strands * 255).astype(np.uint8)).resize((w, h), Image.BILINEAR)).astype(np.float32) / 255
        big = value_noise(w, h, max(3, w / 3), seed + 5, 2)
        t = np.clip(strands * 0.6 + big * 0.5, 0, 1)
        img = ramp([darkc, base, tipc], t)
        img[:, :, 3] = 1
        # little strand highlights
        for _ in range(int(w * h / 18)):
            x, y = r.integers(0, w), r.integers(0, h)
            L = r.integers(1, length + 1)
            rect(img, x, y, x + 1, min(h, y + L), np.append(np.clip(tipc[:3] * 1.1, 0, 1), 0.55))
        return vgrad(img, 1.1, 0.8) if face not in ('top', 'bottom') else img
    return f


def m_scales(base, light=None, dark=None, size=4):
    base = hexc(base)
    lightc = hexc(light) if light else np.clip(base * 1.35, 0, 1)
    darkc = hexc(dark) if dark else base * 0.45
    darkc[3] = 1

    def f(w, h, seed, face):
        img = new(w, h)
        ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
        s = float(size)
        row = np.floor(ys / (s * 0.75))
        off = (row % 2) * s / 2
        cx = (np.floor((xs + off) / s) + 0.5) * s - off
        cy = (row + 0.9) * s * 0.75
        d = np.sqrt(((xs - cx) / s) ** 2 + ((ys - cy) / (s * 0.9)) ** 2)
        t = np.clip(1 - d * 1.5, 0, 1)
        n = value_noise(w, h, max(2, w / 3), seed, 2)
        img = ramp([darkc, base, lightc], np.clip(t * 0.85 + n * 0.3, 0, 1))
        img[:, :, 3] = 1
        rim = np.clip((d - 0.6) * 3, 0, 1)
        img = shade(img, 1 - rim * 0.3)
        return vgrad(img, 1.05, 0.85) if face not in ('top', 'bottom') else img
    return f


def m_metal(base, rivets=True, scratches=True, bevel=True, trim=None):
    base = hexc(base)
    trimc = hexc(trim) if trim else None

    def f(w, h, seed, face):
        r = rng(seed)
        n = value_noise(w, h, max(2, w / 3), seed, 3)
        brushed = value_noise(w * 4, h, 1.0, seed + 3, 1)
        brushed = np.asarray(Image.fromarray((brushed * 255).astype(np.uint8)).resize((w, h), Image.BILINEAR)).astype(np.float32) / 255
        t = np.clip(0.45 + (n - 0.5) * 0.7 + (brushed - 0.5) * 0.25, 0, 1)
        img = colorize(base, t, 0.65, 1.35)
        if bevel and w > 3 and h > 3:
            rect(img, 0, 0, w, 1, np.append(np.clip(base[:3] * 1.7, 0, 1), 0.85))
            rect(img, 0, 0, 1, h, np.append(np.clip(base[:3] * 1.4, 0, 1), 0.6))
            rect(img, 0, h - 1, w, h, np.append(base[:3] * 0.4, 0.9))
            rect(img, w - 1, 0, w, h, np.append(base[:3] * 0.55, 0.8))
        if trimc is not None and h > 6:
            rect(img, 0, 1, w, 2, trimc)
            rect(img, 0, h - 3, w, h - 2, trimc)
        if scratches:
            for _ in range(max(1, w * h // 60)):
                x, y = r.uniform(0, w), r.uniform(0, h)
                a = r.uniform(0, math.pi)
                L = r.uniform(1, max(2, w / 3))
                line(img, x, y, x + math.cos(a) * L, y + math.sin(a) * L, np.append(np.clip(base[:3] * 1.6, 0, 1), 0.35), 0.6)
        if rivets and w >= 6 and h >= 6:
            for (x, y) in [(1.5, 1.5), (w - 2.5, 1.5), (1.5, h - 2.5), (w - 2.5, h - 2.5)]:
                draw_mask(img, ellipse_mask(w, h, x + 0.5, y + 0.5, 0.9, 0.9), np.append(np.clip(base[:3] * 1.8, 0, 1), 1))
                draw_mask(img, ellipse_mask(w, h, x + 0.8, y + 0.8, 0.5, 0.5), np.append(base[:3] * 0.5, 0.8))
        return vgrad(img, 1.12, 0.8)
    return f


def m_cloth(base, pattern=None, trim=None):
    base = hexc(base)
    trimc = hexc(trim) if trim else None

    def f(w, h, seed, face):
        n = value_noise(w, h, max(2, w / 3), seed, 2)
        ys, xs = np.mgrid[0:h, 0:w]
        weave = ((xs + ys) % 2) * 0.06
        folds = np.sin(xs / max(1, w) * math.pi * (2 + seed % 3) + n * 3) * 0.12
        t = np.clip(0.5 + (n - 0.5) * 0.6 + weave + folds, 0, 1)
        img = colorize(base, t, 0.7, 1.25)
        if trimc is not None and h > 4:
            rect(img, 0, h - max(1, h // 8), w, h, trimc)
        return vgrad(img, 1.08, 0.78) if face not in ('top', 'bottom') else img
    return f


def m_stone(base, cracks=True, moss=None, carved=False):
    base = hexc(base)

    def f(w, h, seed, face):
        r = rng(seed)
        n = value_noise(w, h, max(2, w / 3), seed, 4, 0.55)
        speck = white(w, h, seed + 7)
        t = np.clip(0.5 + (n - 0.5) * 1.3 + (speck - 0.5) * 0.18, 0, 1)
        img = colorize(base, t, 0.62, 1.25)
        if cracks:
            for _ in range(max(1, w * h // 90)):
                x, y = r.uniform(0, w), r.uniform(0, h)
                for _k in range(r.integers(2, 5)):
                    a = r.uniform(0, 2 * math.pi)
                    L = r.uniform(1.5, max(2, w / 4))
                    x2, y2 = x + math.cos(a) * L, y + math.sin(a) * L
                    line(img, x, y, x2, y2, np.append(base[:3] * 0.35, 0.8), 0.7)
                    x, y = x2, y2
        if carved and w > 6 and h > 6:
            rect(img, 1, 1, w - 1, 2, np.append(base[:3] * 0.6, 0.8))
            rect(img, 1, h - 2, w - 1, h - 1, np.append(base[:3] * 0.6, 0.8))
        if moss:
            mm = value_noise(w, h, max(2, w / 4), seed + 11, 2)
            draw_mask(img, np.clip((mm - 0.6) * 4, 0, 1) * 0.8, hexc(moss))
        return vgrad(img, 1.06, 0.84)
    return f


def m_chitin(base, gloss=None, segments=0):
    base = hexc(base)
    glossc = hexc(gloss) if gloss else np.clip(base * 2.2 + 0.15, 0, 1)

    def f(w, h, seed, face):
        n = value_noise(w, h, max(2, w / 3), seed, 2)
        ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
        hl = np.exp(-(((xs - w * 0.35) / (w * 0.35 + 0.5)) ** 2 + ((ys - h * 0.3) / (h * 0.35 + 0.5)) ** 2))
        img = colorize(base, np.clip(0.4 + n * 0.4, 0, 1), 0.7, 1.3)
        draw_mask(img, hl * 0.45, glossc)
        if segments:
            for k in range(1, segments):
                y = int(h * k / segments)
                rect(img, 0, y, w, y + 1, np.append(base[:3] * 0.3, 0.9))
                rect(img, 0, y + 1, w, y + 2, np.append(glossc[:3], 0.25))
        return edge_dark(img, 0.35)
    return f


def m_bone(base='#E8DFC8'):
    base = hexc(base)

    def f(w, h, seed, face):
        n = value_noise(w, h, max(2, w / 2), seed, 3)
        img = colorize(base, n, 0.75, 1.1)
        return vgrad(img, 1.05, 0.8)
    return f


def m_leather(base, stitch=None):
    base = hexc(base)
    st = hexc(stitch) if stitch else np.clip(base * 1.6, 0, 1)

    def f(w, h, seed, face):
        n = value_noise(w, h, max(2, w / 3), seed, 3)
        pores = white(w, h, seed + 2)
        img = colorize(base, np.clip(n * 0.8 + pores * 0.15, 0, 1), 0.65, 1.2)
        if w > 4 and h > 4:
            for x in range(1, w - 1, 2):
                rect(img, x, 1, x + 1, 2, np.append(st[:3], 0.7))
                rect(img, x, h - 2, x + 1, h - 1, np.append(st[:3], 0.7))
        return vgrad(img, 1.08, 0.8)
    return f


def m_wood(base, grain=None):
    base = hexc(base)

    def f(w, h, seed, face):
        n = value_noise(w, h * 6, 2, seed, 2)
        n = np.asarray(Image.fromarray((n * 255).astype(np.uint8)).resize((w, h), Image.BILINEAR)).astype(np.float32) / 255
        ys, xs = np.mgrid[0:h, 0:w]
        rings = (np.sin((xs + n * 6) * 1.3) + 1) / 2
        img = colorize(base, np.clip(rings * 0.5 + n * 0.5, 0, 1), 0.7, 1.2)
        return vgrad(img, 1.05, 0.85)
    return f


def m_glow(base, core=None, pulse=True):
    """Emissive looking material (eyes, magic, fire)."""
    base = hexc(base)
    corec = hexc(core) if core else np.array([1, 1, 1, 1], dtype=np.float32)

    def f(w, h, seed, face):
        m = ellipse_mask(w, h, w / 2, h / 2, w / 3 + 0.3, h / 3 + 0.3, 0.8)
        img = new(w, h, base)
        n = value_noise(w, h, max(1, w / 3), seed, 2)
        img = shade(img, 0.8 + n * 0.4)
        draw_mask(img, m * 0.7, corec)
        return img
    return f


def m_crystal(base, light=None):
    base = hexc(base)
    lc = hexc(light) if light else np.clip(base * 1.8 + 0.2, 0, 1)

    def f(w, h, seed, face):
        r = rng(seed)
        img = colorize(base, value_noise(w, h, max(2, w / 2), seed, 2), 0.7, 1.3)
        for _ in range(max(1, w * h // 20)):
            x, y = r.uniform(0, w), r.uniform(0, h)
            line(img, x, y, x + r.uniform(-3, 3), y + r.uniform(-3, 3), np.append(lc[:3], 0.6), 0.8)
        return edge_dark(img, 0.3)
    return f


def m_flat(color):
    c = hexc(color)

    def f(w, h, seed, face):
        return new(w, h, c)
    return f


def m_hair(base, highlight=None):
    base = hexc(base)
    hl = hexc(highlight) if highlight else np.clip(base * 1.6 + 0.08, 0, 1)

    def f(w, h, seed, face):
        strands = value_noise(w * 2, h, 1.0, seed, 1)
        strands = np.asarray(Image.fromarray((strands * 255).astype(np.uint8)).resize((w, h), Image.BILINEAR)).astype(np.float32) / 255
        img = ramp([base * np.array([0.5, 0.5, 0.5, 1], dtype=np.float32), base, hl], np.clip(strands * 0.9 + 0.1, 0, 1))
        img[:, :, 3] = 1
        return vgrad(img, 1.15, 0.75)
    return f


# ---------------------------------------------------------------- decals (eyes, mouths, symbols) painted on faces

def decal_eyes(img, color, pupil=None, y=0.38, spacing=0.22, size=0.12, slant=0.0, glow_img=None, shape='round'):
    h, w = img.shape[:2]
    c = hexc(color)
    for side in (-1, 1):
        cx = w / 2 + side * w * spacing
        cy = h * y
        rx, ry = max(0.8, w * size), max(0.6, w * size * (0.55 if shape == 'slit' else 0.8))
        m = ellipse_mask(w, h, cx, cy, rx, ry, 0.35)
        if slant:
            ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
            m = m * ((ys - cy) * 1.0 > (xs - cx) * side * slant - ry * 0.8)
        # dark socket
        draw_mask(img, np.clip(ellipse_mask(w, h, cx, cy, rx * 1.5, ry * 1.6, 0.6) - m, 0, 1) * 0.6, np.array([0, 0, 0, 1], np.float32))
        draw_mask(img, m, c)
        if pupil:
            draw_mask(img, ellipse_mask(w, h, cx, cy, rx * 0.35, ry * 0.8, 0.3), hexc(pupil))
        if glow_img is not None:
            draw_mask(glow_img, m, c)
            draw_mask(glow_img, ellipse_mask(w, h, cx, cy, rx * 0.45, ry * 0.45, 0.4), np.array([1, 1, 1, 1], np.float32))


def decal_mouth(img, y=0.72, width=0.6, teeth='#F2EEDC', open_=0.12, color='#3A0A0A'):
    h, w = img.shape[:2]
    cy = h * y
    m = ellipse_mask(w, h, w / 2, cy, w * width / 2, max(0.7, h * open_), 0.3)
    draw_mask(img, m, hexc(color))
    if teeth:
        n = max(2, int(w * width / 2))
        for i in range(n):
            x = w / 2 - w * width / 2 + (i + 0.5) * (w * width / n)
            tri = np.zeros(img.shape[:2], np.float32)
            ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
            tri = ((ys >= cy - h * open_) & (ys <= cy - h * open_ + max(1.0, h * open_ * 1.2) * (1 - abs(xs - x) / max(0.8, w * width / n / 2)))).astype(np.float32)
            draw_mask(img, tri * (abs(xs - x) < w * width / n / 2), hexc(teeth))


def decal_band(img, y0, y1, color):
    h, w = img.shape[:2]
    rect(img, 0, int(h * y0), w, max(int(h * y0) + 1, int(h * y1)), hexc(color))


def decal_vstripe(img, x0, x1, color):
    h, w = img.shape[:2]
    rect(img, int(w * x0), 0, max(int(w * x0) + 1, int(w * x1)), h, hexc(color))


def decal_runes(img, color, seed, glow_img=None, density=0.5):
    h, w = img.shape[:2]
    r = rng(seed)
    c = hexc(color)
    for _ in range(int(max(1, w * h / 40) * density)):
        x, y = r.uniform(1, w - 1), r.uniform(1, h - 1)
        for _k in range(3):
            a = r.choice([0, math.pi / 2, math.pi / 4, -math.pi / 4])
            L = r.uniform(1, 3)
            x2, y2 = x + math.cos(a) * L, y + math.sin(a) * L
            line(img, x, y, x2, y2, c, 0.8)
            if glow_img is not None:
                line(glow_img, x, y, x2, y2, c, 0.8)
            x, y = x2, y2
