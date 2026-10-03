"""Pixel-art helpers for block (16x16) and item (32x32) textures: noise, colour ramps, polygons, outlines, bevels."""
import math
import numpy as np
from PIL import Image


def rng(seed):
    return np.random.default_rng(seed)


def hexc(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], np.float32)


def ramp(stops, t):
    """stops: list of hex colours (evenly spaced) or (pos, hex); t: array 0..1 -> rgb"""
    if isinstance(stops[0], str):
        stops = [(i / (len(stops) - 1), s) for i, s in enumerate(stops)]
    t = np.clip(np.asarray(t, np.float32), 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    for (p0, c0), (p1, c1) in zip(stops[:-1], stops[1:]):
        m = (t >= p0) & (t <= p1)
        u = ((t - p0) / max(p1 - p0, 1e-6))[..., None]
        out[m] = (hexc(c0) * (1 - u) + hexc(c1) * u)[m]
    return out


def tile_noise(n, r, cell=4, octaves=3, persist=0.55):
    """seamless value noise on an n x n torus"""
    out = np.zeros((n, n), np.float32)
    amp, tot = 1.0, 0.0
    c = cell
    for _ in range(octaves):
        g = max(1, n // c)
        grid = r.random((g, g))
        ys = np.arange(n) / c
        y0 = np.floor(ys).astype(int)
        fy = ys - y0
        fy = fy * fy * (3 - 2 * fy)
        a = grid[y0 % g][:, y0 % g]
        b = grid[y0 % g][:, (y0 + 1) % g]
        cc = grid[(y0 + 1) % g][:, y0 % g]
        d = grid[(y0 + 1) % g][:, (y0 + 1) % g]
        fx = fy[None, :]
        fyy = fy[:, None]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fyy) + (cc * (1 - fx) + d * fx) * fyy)
        tot += amp
        amp *= persist
        c = max(1, c // 2)
    out /= tot
    return (out - out.min()) / max(out.max() - out.min(), 1e-6)


def voronoi(n, r, count, wrap=True):
    """returns (cell id, distance to nearest, distance to second nearest) for an n x n tile"""
    pts = r.random((count, 2)) * n
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32) + 0.5
    d = np.full((count, n, n), 1e9, np.float32)
    for i, (px_, py_) in enumerate(pts):
        offs = [(0, 0)] if not wrap else [(ox, oy) for ox in (-n, 0, n) for oy in (-n, 0, n)]
        for ox, oy in offs:
            d[i] = np.minimum(d[i], np.hypot(xx - px_ - ox, yy - py_ - oy))
    s = np.sort(d, axis=0)
    return np.argmin(d, axis=0), s[0], s[1]


def img(rgb, alpha=None):
    h, w = rgb.shape[:2]
    a = np.full((h, w), 255, np.float32) if alpha is None else alpha
    arr = np.dstack([np.clip(rgb, 0, 255), np.clip(a, 0, 255)]).astype(np.uint8)
    return Image.fromarray(arr, 'RGBA')


def shade(rgb, f):
    return np.clip(rgb * f, 0, 255)


def mix(a, b, t):
    t = np.asarray(t, np.float32)
    if t.ndim:
        t = t[..., None]
    return a * (1 - t) + b * t


# ---------------------------------------------------------------- sprite canvas (items)
class Canvas:
    def __init__(self, n=32):
        self.n = n
        self.rgb = np.zeros((n, n, 3), np.float32)
        self.a = np.zeros((n, n), np.float32)
        self.glow = np.zeros((n, n), np.float32)

    def mask_poly(self, pts, ss=4):
        """anti-aliased-free polygon coverage via supersampling; pts in pixel coords"""
        n = self.n
        yy, xx = np.mgrid[0:n * ss, 0:n * ss].astype(np.float32)
        xx = (xx + 0.5) / ss
        yy = (yy + 0.5) / ss
        inside = np.zeros_like(xx, bool)
        P = list(pts)
        j = len(P) - 1
        for i in range(len(P)):
            xi, yi = P[i]
            xj, yj = P[j]
            cond = ((yi > yy) != (yj > yy)) & (xx < (xj - xi) * (yy - yi) / ((yj - yi) + 1e-9) + xi)
            inside ^= cond
            j = i
        cov = inside.reshape(n, ss, n, ss).mean(axis=(1, 3))
        return cov > 0.45

    def mask_circle(self, cx, cy, r):
        yy, xx = np.mgrid[0:self.n, 0:self.n].astype(np.float32) + 0.5
        return np.hypot(xx - cx, yy - cy) <= r

    def mask_line(self, x0, y0, x1, y1, w):
        yy, xx = np.mgrid[0:self.n, 0:self.n].astype(np.float32) + 0.5
        dx, dy = x1 - x0, y1 - y0
        L2 = dx * dx + dy * dy + 1e-9
        t = np.clip(((xx - x0) * dx + (yy - y0) * dy) / L2, 0, 1)
        d = np.hypot(xx - (x0 + t * dx), yy - (y0 + t * dy))
        return d <= w / 2, t

    def fill(self, mask, col, alpha=255):
        col = np.asarray(col, np.float32)
        if col.ndim == 1:
            self.rgb[mask] = col
        else:
            self.rgb[mask] = col[mask]
        self.a[mask] = alpha

    def fill_ramp(self, mask, stops, t):
        t = np.broadcast_to(np.asarray(t, np.float32), mask.shape)
        self.rgb[mask] = ramp(stops, t)[mask]
        self.a[mask] = 255

    def outline(self, col='#1a0608', only_outer=True):
        m = self.a > 0
        o = np.zeros_like(m)
        o[1:] |= m[:-1]
        o[:-1] |= m[1:]
        o[:, 1:] |= m[:, :-1]
        o[:, :-1] |= m[:, 1:]
        o &= ~m
        self.rgb[o] = hexc(col)
        self.a[o] = 255

    def inner_edge(self, mask, light='#ffffff', dark='#000000', amt=0.35):
        """bevel: lighten top-left edge pixels, darken bottom-right ones"""
        up = np.zeros_like(mask)
        up[1:] = mask[:-1]
        left = np.zeros_like(mask)
        left[:, 1:] = mask[:, :-1]
        down = np.zeros_like(mask)
        down[:-1] = mask[1:]
        right = np.zeros_like(mask)
        right[:, :-1] = mask[:, 1:]
        tl = mask & (~up | ~left)
        br = mask & (~down | ~right) & ~tl
        self.rgb[tl] = mix(self.rgb[tl], hexc(light), amt)
        self.rgb[br] = mix(self.rgb[br], hexc(dark), amt)

    def sparkle(self, x, y, col='#ffffff', size=1):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1))[:1 + 4 * (size > 0)]:
            X, Y = x + dx, y + dy
            if 0 <= X < self.n and 0 <= Y < self.n:
                k = 1.0 if (dx, dy) == (0, 0) else 0.55
                self.rgb[Y, X] = mix(self.rgb[Y, X], hexc(col), k)
                self.a[Y, X] = 255

    def image(self):
        return img(self.rgb, self.a)
