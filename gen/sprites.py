"""Pixel-art sprite toolkit for item / block textures.

Shapes are rasterised at 8x then reduced; shading comes from a blurred height field (bevel look), a palette ramp and a
dark outline, so every sprite gets consistent hand-drawn style lighting.  Emissive pixels are brightened separately."""
import math
import numpy as np
from PIL import Image

SS = 8


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(*h):
    return [hexc(x) if isinstance(x, str) else tuple(x) for x in h]


def ramp_colors(p, n):
    """interpolate a palette to n colours"""
    p = np.array(p, np.float32)
    xs = np.linspace(0, len(p) - 1, n)
    out = []
    for x in xs:
        i = int(math.floor(x)); f = x - i
        j = min(i + 1, len(p) - 1)
        out.append(tuple(p[i] * (1 - f) + p[j] * f))
    return out


class Canvas:
    def __init__(self, size=16):
        self.n = size
        self.big = size * SS
        self.img = np.zeros((size, size, 4), np.float32)
        self.yy, self.xx = np.mgrid[0:self.big, 0:self.big]
        self.u = (self.xx + 0.5) / SS      # in sprite pixel units
        self.v = (self.yy + 0.5) / SS

    # ---- masks (in sprite pixel units, origin top-left)
    def mask_poly(self, pts):
        px = np.array([p[0] for p in pts]); py = np.array([p[1] for p in pts])
        inside = np.zeros((self.big, self.big), bool)
        n = len(pts)
        j = n - 1
        for i in range(n):
            xi, yi, xj, yj = px[i], py[i], px[j], py[j]
            cond = ((yi > self.v) != (yj > self.v)) & (self.u < (xj - xi) * (self.v - yi) / (yj - yi + 1e-9) + xi)
            inside ^= cond
            j = i
        return inside

    def mask_circle(self, cx, cy, r):
        return (self.u - cx) ** 2 + (self.v - cy) ** 2 <= r * r

    def mask_ellipse(self, cx, cy, rx, ry, rot=0.0):
        c, s = math.cos(rot), math.sin(rot)
        x = (self.u - cx) * c + (self.v - cy) * s
        y = -(self.u - cx) * s + (self.v - cy) * c
        return (x / rx) ** 2 + (y / ry) ** 2 <= 1

    def mask_rect(self, x0, y0, x1, y1):
        return (self.u >= x0) & (self.u <= x1) & (self.v >= y0) & (self.v <= y1)

    def mask_line(self, x0, y0, x1, y1, w):
        dx, dy = x1 - x0, y1 - y0
        L2 = dx * dx + dy * dy + 1e-9
        t = np.clip(((self.u - x0) * dx + (self.v - y0) * dy) / L2, 0, 1)
        px, py = x0 + t * dx, y0 + t * dy
        return (self.u - px) ** 2 + (self.v - py) ** 2 <= (w / 2) ** 2

    def mask_heart(self, cx, cy, s):
        x = (self.u - cx) / s
        y = -(self.v - cy) / s
        return ((x * x + y * y - 1) ** 3 - x * x * y ** 3) <= 0

    # ---- shading
    def _reduce(self, m):
        return m.reshape(self.n, SS, self.n, SS).mean((1, 3))

    def draw(self, mask, colors, light=(-0.6, -0.8), bevel=1.6, outline=None, emissive=0.0, noise=0.0, seed=0, flat=None, spec=0.0):
        """paint mask with palette `colors` (dark->light). bevel controls how round the shading looks."""
        cov = self._reduce(mask)
        m = cov > 0.5
        if not m.any():
            return self
        # height field: blurred distance-like
        h = self._blur(cov, bevel)
        gy, gx = np.gradient(h)
        lx, ly = light
        shade = -(gx * lx + gy * ly) * 6.0
        t = 0.47 + shade * 0.85 + (h - h[m].mean()) * 0.6
        if noise:
            r = np.random.default_rng(seed + 7)
            t = t + (r.random(t.shape) - 0.5) * noise
        if flat is not None:
            t = np.full_like(t, flat)
        n = len(colors)
        idx = np.clip(np.round(t * (n - 1)).astype(int), 0, n - 1)
        arr = np.array(colors, np.float32)
        col = arr[idx]
        if spec > 0:
            hl = (t > 0.92)
            col = np.where(hl[..., None], np.minimum(col * 1.25 + 30 * spec, 255), col)
        if emissive:
            col = np.minimum(col * (1 + emissive * 0.5) + 40 * emissive, 255)
        out = self.img.copy()
        out[m, :3] = col[m]
        out[m, 3] = 255
        self.img = out
        if outline is not None:
            self.outline(m, outline)
        return self

    def outline(self, mask_small, color, only_empty=True):
        m = mask_small
        e = np.zeros_like(m)
        e[1:] |= m[:-1]; e[:-1] |= m[1:]; e[:, 1:] |= m[:, :-1]; e[:, :-1] |= m[:, 1:]
        ring = e & ~m if only_empty else e
        arr = np.array(color, np.float32)
        # outline only pixels not already opaque
        free = ring & (self.img[..., 3] < 1)
        self.img[free, :3] = arr
        self.img[free, 3] = 255
        return self

    def outline_all(self, color):
        self.outline(self.img[..., 3] > 0, color)
        return self

    def _blur(self, a, k):
        # separable box blur approximating gaussian
        r = max(1, int(k))
        out = a.astype(np.float32)
        for _ in range(2):
            pad = np.pad(out, ((r, r), (0, 0)), mode='edge')
            c = np.cumsum(pad, 0)
            c = np.vstack([np.zeros((1, c.shape[1])), c])
            out = (c[2 * r + 1:] - c[:-2 * r - 1]) / (2 * r + 1)
            pad = np.pad(out, ((0, 0), (r, r)), mode='edge')
            c = np.cumsum(pad, 1)
            c = np.hstack([np.zeros((c.shape[0], 1)), c])
            out = (c[:, 2 * r + 1:] - c[:, :-2 * r - 1]) / (2 * r + 1)
        return out

    def pixel(self, x, y, color, alpha=255):
        if 0 <= x < self.n and 0 <= y < self.n:
            self.img[y, x, :3] = color
            self.img[y, x, 3] = alpha
        return self

    def speckle(self, mask, colors, density=0.15, seed=1):
        cov = self._reduce(mask) > 0.5
        r = np.random.default_rng(seed)
        for y in range(self.n):
            for x in range(self.n):
                if cov[y, x] and r.random() < density:
                    self.pixel(x, y, colors[r.integers(len(colors))])
        return self

    def glow_dots(self, pts, color, strength=1.0):
        for (x, y) in pts:
            self.pixel(int(x), int(y), color)
        return self

    def to_image(self):
        return Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8), 'RGBA')


def save(canvas, path):
    canvas.to_image().save(path)
