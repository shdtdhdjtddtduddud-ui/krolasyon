"""Procedural texture painting for box-UV cubes."""
import numpy as np, math
from core import Model


def rng(seed):
    return np.random.default_rng(seed * 7919 + 13)


def noise2(h, w, r, scale=3.0, oct=3):
    out = np.zeros((h, w))
    amp = 1.0
    tot = 0
    for o in range(oct):
        gh, gw = max(2, int(h / scale) + 2), max(2, int(w / scale) + 2)
        g = r.random((gh, gw))
        ys = np.linspace(0, gh - 1.001, h)
        xs = np.linspace(0, gw - 1.001, w)
        y0 = ys.astype(int); x0 = xs.astype(int)
        fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy)
        tot += amp
        amp *= 0.5
        scale = max(1.0, scale / 2)
    return out / tot


def lerp(a, b, t):
    a = np.array(a, np.float32); b = np.array(b, np.float32)
    t = np.asarray(t, np.float32)[..., None]
    return a * (1 - t) + b * t


FACES = ['top', 'bottom', 'west', 'north', 'east', 'south']


class Painter:
    def __init__(self, model):
        self.m = model
        self.tex = np.zeros((model.th, model.tw, 4), np.float32)
        self.glow = np.zeros((model.th, model.tw, 4), np.float32)
        self.mats = {}

    def mat(self, name):
        def deco(fn):
            self.mats[name] = fn
            return fn
        return deco

    def run(self):
        for bn in self.m.order:
            for c in self.m.bones[bn].cubes:
                rects = Model.face_rects(c)
                fn = self.mats[c.mat]
                for face, (u, v, w, h) in rects.items():
                    W, H = int(math.ceil(w)), int(math.ceil(h))
                    if W <= 0 or H <= 0:
                        continue
                    u, v = int(u), int(v)
                    col, alpha, glow = fn(c, face, W, H, rng(c.seed * 10 + FACES.index(face)))
                    self.tex[v:v + H, u:u + W, :3] = col
                    self.tex[v:v + H, u:u + W, 3] = alpha * 255
                    if glow is not None:
                        self.glow[v:v + H, u:u + W] = glow
                if c.plane:
                    # back face = horizontal mirror of front so double-sided planes line up
                    u, v, w, h = rects['north']
                    su, sv, _, _ = rects['south']
                    W, H = int(math.ceil(w)), int(math.ceil(h))
                    self.tex[int(sv):int(sv) + H, int(su):int(su) + W] = self.tex[int(v):int(v) + H, int(u):int(u) + W][:, ::-1]
                    self.glow[int(sv):int(sv) + H, int(su):int(su) + W] = self.glow[int(v):int(v) + H, int(u):int(u) + W][:, ::-1]
        return self

    def images(self):
        from PIL import Image
        t = Image.fromarray(np.clip(self.tex, 0, 255).astype(np.uint8), 'RGBA')
        gl = self.glow.copy()
        gl[..., :3] *= gl[..., 3:4] / 255.0  # premultiplied: eyes render type is additive
        g = Image.fromarray(np.clip(gl, 0, 255).astype(np.uint8), 'RGBA')
        return t, g


def border(H, W, width=1):
    yy, xx = np.mgrid[0:H, 0:W]
    return (yy < width) | (xx < width) | (yy >= H - width) | (xx >= W - width)


def glow_img(H, W, color, mask):
    g = np.zeros((H, W, 4), np.float32)
    g[..., :3] = color
    g[..., 3] = np.clip(mask, 0, 1) * 255
    return g


def draw_line(mask, x0, y0, x1, y1, val=1.0, thick=0):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    H, W = mask.shape
    for i in range(n + 1):
        t = i / n
        x = int(round(x0 + (x1 - x0) * t)); y = int(round(y0 + (y1 - y0) * t))
        for dy in range(-thick, thick + 1):
            for dx in range(-thick, thick + 1):
                if 0 <= y + dy < H and 0 <= x + dx < W:
                    mask[y + dy, x + dx] = max(mask[y + dy, x + dx], val)


def cracks(H, W, r, count, length=0.8, branch=0.35):
    """lightning-like vein mask: 1 for core, 0.5 for halo"""
    m = np.zeros((H, W), np.float32)
    for _ in range(count):
        x, y = r.random() * W, r.random() * H
        ang = r.random() * math.tau
        steps = int((H + W) * length * (0.5 + r.random()))
        stack = [(x, y, ang, steps)]
        while stack:
            x, y, ang, steps = stack.pop()
            for s in range(steps):
                ang += r.normal(0, 0.45)
                nx, ny = x + math.cos(ang) * 1.3, y + math.sin(ang) * 1.3
                draw_line(m, x, y, nx, ny)
                x, y = nx, ny
                if not (0 <= x < W and 0 <= y < H):
                    break
                if r.random() < branch / 10 and steps - s > 4:
                    stack.append((x, y, ang + r.choice([-1, 1]) * (0.6 + r.random() * 0.6), (steps - s) // 2))
    halo = np.zeros_like(m)
    halo[1:] = np.maximum(halo[1:], m[:-1]); halo[:-1] = np.maximum(halo[:-1], m[1:])
    halo[:, 1:] = np.maximum(halo[:, 1:], m[:, :-1]); halo[:, :-1] = np.maximum(halo[:, :-1], m[:, 1:])
    return np.maximum(m, halo * 0.5)
