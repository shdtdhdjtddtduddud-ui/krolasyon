"""High resolution box-UV painter: every model unit gets S texels, the Java layer keeps the logical size
so vanilla samples the larger PNG with normalised UVs."""
import math, numpy as np
from core import Model

FACES = ['top', 'bottom', 'west', 'north', 'east', 'south']


def rng(seed):
    return np.random.default_rng(seed * 7919 + 13)


def vnoise(h, w, r, cell=6.0, octaves=3):
    out = np.zeros((h, w)); amp = 1.0; tot = 0.0
    for _ in range(octaves):
        gh, gw = int(h / cell) + 3, int(w / cell) + 3
        g = r.random((gh, gw))
        ys = np.arange(h) / cell; xs = np.arange(w) / cell
        y0 = ys.astype(int); x0 = xs.astype(int)
        fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy)
        tot += amp; amp *= 0.5; cell = max(1.0, cell / 2)
    return out / tot


def mix(a, b, t):
    a = np.array(a, np.float32); b = np.array(b, np.float32)
    t = np.clip(np.asarray(t, np.float32), 0, 1)[..., None]
    return a * (1 - t) + b * t


class HDPainter:
    def __init__(self, model, S):
        self.m, self.S = model, S
        self.tex = np.zeros((model.th * S, model.tw * S, 4), np.float32)
        self.glow = np.zeros_like(self.tex)
        self.mats = {}

    def mat(self, name):
        def deco(fn):
            self.mats[name] = fn
            return fn
        return deco

    def run(self):
        S = self.S
        for bn in self.m.order:
            for c in self.m.bones[bn].cubes:
                for face, (u, v, w, h) in Model.face_rects(c).items():
                    W, H = int(math.ceil(w)) * S, int(math.ceil(h)) * S
                    if W <= 0 or H <= 0:
                        continue
                    u0, v0 = int(u * S), int(v * S)
                    col, alpha, glow = self.mats[c.mat](c, face, W, H, rng(c.seed * 10 + FACES.index(face)))
                    hh, ww = self.tex[v0:v0 + H, u0:u0 + W].shape[:2]
                    self.tex[v0:v0 + hh, u0:u0 + ww, :3] = col[:hh, :ww]
                    self.tex[v0:v0 + hh, u0:u0 + ww, 3] = (alpha if np.ndim(alpha) == 0 else alpha[:hh, :ww]) * 255
                    if glow is not None:
                        self.glow[v0:v0 + hh, u0:u0 + ww] = glow[:hh, :ww]
        return self

    def images(self):
        from PIL import Image
        t = Image.fromarray(np.clip(self.tex, 0, 255).astype(np.uint8), 'RGBA')
        gl = self.glow.copy()
        gl[..., :3] *= gl[..., 3:4] / 255.0
        return t, Image.fromarray(np.clip(gl, 0, 255).astype(np.uint8), 'RGBA')


def long_axis(face, c):
    """True when the long axis of the cube runs vertically on this face's texture rect"""
    dx, dy, dz = c.size
    if face in ('top', 'bottom'):
        return dz >= dx
    if face in ('west', 'east'):
        return dy >= dz
    return dy >= dx
