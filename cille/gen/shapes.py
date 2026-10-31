"""Rounded-volume helpers (ellipsoids/capsules from stacked cubes) and world-space texture painting,
so a creature built from many small cubes reads as one smooth cartoon body instead of a stack of boxes."""
import math, numpy as np

# 3-box cross section approximates a rounded (12-gon) slice
CROSS = ((1.0, 0.48), (0.88, 0.80), (0.48, 1.0))


def ellipsoid_l(M, bone, center, radii, mat, axis='y', slices=6, fill=1.0, cross=CROSS, **kw):
    """ellipsoid in bone-local coordinates built from `slices` slabs along `axis`"""
    ai = 'xyz'.index(axis)
    oi = [i for i in range(3) if i != ai]
    r = list(radii)
    th = 2 * r[ai] / slices
    cubes = []
    for s in range(slices):
        t0 = -r[ai] + s * th
        tm = t0 + th / 2
        k = math.sqrt(max(0.0, 1 - (tm / r[ai]) ** 2)) ** fill
        k = max(k, 0.32)
        for fa, fb in cross:
            size = [0.0, 0.0, 0.0]
            frm = [0.0, 0.0, 0.0]
            size[ai] = th + 0.02
            frm[ai] = center[ai] + t0 - 0.01
            for o, f in zip(oi, (fa, fb)):
                size[o] = max(0.6, 2 * r[o] * k * f)
                frm[o] = center[o] - size[o] / 2
            cubes.append(M.cube_l(bone, frm, size, mat, **kw))
    return cubes


def ellipsoid(M, bone, center, radii, mat, **kw):
    """world-space center (bone must be unrotated relative to world at rest)"""
    pv = M.bones[bone].pivot
    return ellipsoid_l(M, bone, [center[i] - pv[i] for i in range(3)], radii, mat, **kw)


def limb(M, name, parent, A, B, w0, d0, w1, d1, mat, extend=2.0, slices=4):
    """tapered rounded limb segment: bone at A pointing (+y) to B"""
    from core import rot_zyx
    A = np.array(A, float); B = np.array(B, float)
    v = B - A; L = float(np.linalg.norm(v)); v = v / L
    x = math.asin(max(-1, min(1, v[2])))
    z = math.atan2(-v[0], v[1])
    M.bone_w(name, parent, A, rot_zyx(x, 0, z))
    total = L + 2 * extend
    th = total / slices
    for s in range(slices):
        f = (s + 0.5) / slices
        w = w0 + (w1 - w0) * f
        d = d0 + (d1 - d0) * f
        y0 = -extend + s * th
        for fa, fb in CROSS:
            M.cube_l(name, (-w * fa / 2, y0 - 0.01, -d * fb / 2), (w * fa, th + 0.02, d * fb), mat)
    return L


# ---------------------------------------------------------------- world-space painting
FACE_N = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0), 'top': (0, -1, 0), 'bottom': (0, 1, 0)}


def texel_world(M, c, face, W, H):
    """world (rest pose) position of every texel of a cube face + world normal"""
    yy, xx = np.mgrid[0:H, 0:W]
    u = (xx + 0.5) / W
    v = (yy + 0.5) / H
    x0, y0, z0 = c.lfrm
    sx, sy, sz = c.size
    x1, y1, z1 = x0 + sx, y0 + sy, z0 + sz
    if face == 'north':
        P = (x0 + u * sx, y0 + v * sy, np.full_like(u, z0))
    elif face == 'south':
        P = (x1 - u * sx, y0 + v * sy, np.full_like(u, z1))
    elif face == 'west':
        P = (np.full_like(u, x0), y0 + v * sy, z1 - u * sz)
    elif face == 'east':
        P = (np.full_like(u, x1), y0 + v * sy, z0 + u * sz)
    elif face == 'top':
        P = (x0 + u * sx, np.full_like(u, y0), z1 - v * sz)
    else:
        P = (x0 + u * sx, np.full_like(u, y1), z0 + v * sz)
    P = np.stack(P, -1)
    Wm = M.world(c.bone)
    Pw = P @ Wm[:3, :3].T + Wm[:3, 3]
    n = Wm[:3, :3] @ np.array(FACE_N[face], float)
    return Pw, n


class Noise3:
    """cheap smooth 3D noise: sum of randomly oriented sine waves"""

    def __init__(self, seed, waves=10, freq=0.08):
        r = np.random.default_rng(seed)
        d = r.normal(size=(waves, 3))
        d /= np.linalg.norm(d, axis=1, keepdims=True)
        self.k = d * (freq * (0.6 + r.random((waves, 1)) * 1.2))
        self.ph = r.random(waves) * math.tau

    def __call__(self, P):
        s = np.zeros(P.shape[:-1])
        for k, ph in zip(self.k, self.ph):
            s += np.sin(P @ k + ph)
        return 0.5 + 0.5 * s / math.sqrt(len(self.ph)) / 1.2


LIGHT = np.array((-0.35, -1.0, -0.55))
LIGHT /= np.linalg.norm(LIGHT)


def light(n, soft=0.35):
    """baked soft cel light: 1 lit .. ~0.72 shadow side"""
    d = float(np.dot(n, LIGHT))
    return 1.0 - soft * (1 - (d + 1) / 2) ** 1.4


def lerp(a, b, t):
    a = np.array(a, np.float32); b = np.array(b, np.float32)
    t = np.asarray(t, np.float32)[..., None]
    return a * (1 - t) + b * t


def ramp(t, cols):
    """multi stop colour ramp, t in [0,1]"""
    t = np.clip(t, 0, 1) * (len(cols) - 1)
    i = np.minimum(t.astype(int), len(cols) - 2)
    f = t - i
    C = np.array(cols, np.float32)
    return C[i] * (1 - f[..., None]) + C[i + 1] * f[..., None]


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


# 5-box cross section: a much rounder (20-gon like) silhouette for heads and smooth parts
ROUND = ((1.0, 0.32), (0.95, 0.62), (0.81, 0.81), (0.62, 0.95), (0.32, 1.0))
