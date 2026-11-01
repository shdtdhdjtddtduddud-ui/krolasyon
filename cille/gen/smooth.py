"""Smooth meshes for the Cilles.

Every bone's blocky volume (the stacked cube slices that shape the creature) is turned into a signed distance field,
the boxes are blended with a smooth minimum, and marching cubes extracts a smooth, organic surface in the bone's own
space. The surface is decimated, relaxed, and coloured per vertex with the same world-space paint functions that
draw the cube textures (stripes, spots, stars, hood, eyes...). Glowing parts get a full-bright flag.

Output (big-endian binary, read by CilleMesh.java):
  'CMSH' int version int boneCount
  per bone: UTF name, UTF parent ('' for none), int vertexCount, int triangleCount,
            vertexCount * (float x y z  float nx ny nz  ubyte r g b glow), triangleCount * (int a b c)
Coordinates are in blocks (model units / 16) in the bone's local frame, exactly what ModelPart transforms expect.
"""
import math, struct, numpy as np
from skimage.measure import marching_cubes
import fast_simplification
from core import Model
from shapes import light


def box_sdf(P, lo, hi):
    c = (lo + hi) / 2
    h = (hi - lo) / 2
    q = np.abs(P - c) - h
    out = np.linalg.norm(np.maximum(q, 0), axis=-1)
    inside = np.minimum(np.max(q, axis=-1), 0)
    return out + inside


def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


def cube_bounds(c):
    lo = np.array(c.lfrm, float) - c.inflate
    hi = lo + np.array(c.size, float) + 2 * c.inflate
    # paper-thin planes (feathers, fins) still need some body to become a surface
    thin = hi - lo < 1.2
    mid = (lo + hi) / 2
    lo = np.where(thin, mid - 0.6, lo)
    hi = np.where(thin, mid + 0.6, hi)
    return lo, hi


def vertex_normals(V, F):
    n = np.zeros_like(V)
    fn = np.cross(V[F[:, 1]] - V[F[:, 0]], V[F[:, 2]] - V[F[:, 0]])
    for i in range(3):
        np.add.at(n, F[:, i], fn)
    l = np.linalg.norm(n, axis=1, keepdims=True)
    return n / np.maximum(l, 1e-9)


def taubin(V, F, iters=6, lam=0.5, mu=-0.53):
    import scipy.sparse as sp
    n = len(V)
    I = np.concatenate([F[:, 0], F[:, 1], F[:, 2], F[:, 1], F[:, 2], F[:, 0]])
    J = np.concatenate([F[:, 1], F[:, 2], F[:, 0], F[:, 0], F[:, 1], F[:, 2]])
    A = sp.coo_matrix((np.ones(len(I)), (I, J)), shape=(n, n)).tocsr()
    A.data[:] = 1
    deg = np.asarray(A.sum(axis=1)).ravel()
    deg[deg == 0] = 1
    for _ in range(iters):
        for f in (lam, mu):
            avg = A @ V / deg[:, None]
            V = V + f * (avg - V)
    return V


def ell_sdf(P, c, r):
    q = (P - c) / r
    k0 = np.linalg.norm(q, axis=-1)
    k1 = np.linalg.norm((P - c) / (r * r), axis=-1)
    return k0 * (k0 - 1) / np.maximum(k1, 1e-9)


def limb_sdf(P, p):
    """tapered limb with rounded (ellipsoidal) ends: an elliptic capsule along +y"""
    y0, y1 = p['y0'], p['y1']
    yc = np.clip(P[..., 1], y0, y1)
    t = (yc - y0) / max(1e-6, y1 - y0)
    rx = (p['w0'] + (p['w1'] - p['w0']) * t) / 2
    rz = (p['d0'] + (p['d1'] - p['d0']) * t) / 2
    ry = np.minimum(rx, rz)
    q = np.stack([P[..., 0] / rx, (P[..., 1] - yc) / ry, P[..., 2] / rz], -1)
    q2 = np.stack([P[..., 0] / rx ** 2, (P[..., 1] - yc) / ry ** 2, P[..., 2] / rz ** 2], -1)
    k0 = np.linalg.norm(q, axis=-1)
    k1 = np.linalg.norm(q2, axis=-1)
    return k0 * (k0 - 1) / np.maximum(k1, 1e-9)


ROUNDED = {'eye': (0.62, 0.42, 1.2), 'gem': (0.75, 0.75, 1.2), 'emblem': (0.85, 0.85, 1.0), 'feather': (0.95, 0.55, 0.9)}


class Part:
    """one analytic piece of a bone: ellipsoid, tapered limb or (rounded) box"""

    def __init__(self, kind, mat, cube, **kw):
        self.kind, self.mat, self.cube = kind, mat, cube
        self.__dict__.update(kw)

    def sdf(self, P):
        if self.kind == 'ell':
            return ell_sdf(P, self.c, self.r)
        if self.kind == 'limb':
            return limb_sdf(P, self.p)
        return box_sdf(P, self.lo, self.hi) - self.round

    def bounds(self):
        if self.kind == 'ell':
            return self.c - self.r, self.c + self.r
        if self.kind == 'limb':
            p = self.p
            m = max(p['w0'], p['d0'], p['w1'], p['d1']) / 2
            return np.array([-m, p['y0'] - m, -m]), np.array([m, p['y1'] + m, m])
        return self.lo - self.round, self.hi + self.round

    def thickness(self):
        lo, hi = self.bounds()
        return float(np.min(hi - lo))


def bone_parts(M, bone):
    parts = []
    for rec in getattr(M, 'prims', {}).get(bone, []):
        if not rec['cubes']:
            continue
        if rec['kind'] == 'ell':
            parts.append(Part('ell', rec['mat'], rec['cubes'][0], c=rec['c'], r=rec['r']))
        else:
            parts.append(Part('limb', rec['mat'], rec['cubes'][0], p=rec))
    for c in M.bones[bone].cubes:
        if getattr(c, 'prim', False):
            continue
        lo, hi = cube_bounds(c)
        if c.mat in ROUNDED:
            # eyes, gems, emblems, feathers: soft lens / leaf shapes instead of plates
            fx, fy, fz = ROUNDED[c.mat]
            parts.append(Part('ell', c.mat, c, c=(lo + hi) / 2, r=np.maximum((hi - lo) / 2 * np.array([fx, fy, fz]), 0.45)))
            continue
        rnd = float(min(1.2, np.min(hi - lo) * 0.3))
        parts.append(Part('box', c.mat, c, lo=lo + rnd, hi=hi - rnd, round=rnd))
    return parts


def bone_mesh(parts, density):
    if not parts:
        return None
    los, his = zip(*[p.bounds() for p in parts])
    los, his = np.array(los), np.array(his)
    mind = max(0.8, min(p.thickness() for p in parts))
    v = float(np.clip(mind / 3.0, 0.35, 1.6))
    k = float(np.clip(mind * 0.35, 0.5, 3.0))
    margin = k + 2 * v
    lo = los.min(0) - margin
    hi = his.max(0) + margin
    dims = np.ceil((hi - lo) / v).astype(int) + 1
    while np.prod(dims) > 5.0e6:
        v *= 1.12
        dims = np.ceil((hi - lo) / v).astype(int) + 1
    gx, gy, gz = [lo[i] + np.arange(dims[i]) * v for i in range(3)]
    G = np.stack(np.meshgrid(gx, gy, gz, indexing='ij'), -1)
    d = None
    for p in parts:
        s = p.sdf(G)
        d = s if d is None else smin(d, s, k)
    if d.min() >= 0:
        return None
    V, F, _, _ = marching_cubes(d, 0.0, spacing=(v, v, v))
    V = V + lo
    F = F[:, ::-1].copy()            # outward winding
    area = 0.5 * np.linalg.norm(np.cross(V[F[:, 1]] - V[F[:, 0]], V[F[:, 2]] - V[F[:, 0]]), axis=1).sum()
    target = max(80, int(area * density))
    if len(F) > target:
        V, F = fast_simplification.simplify(V.astype(np.float32), F.astype(np.int32), target_reduction=1 - target / len(F))
        V = V.astype(float)
    V = taubin(V, F, iters=3)
    return V, F


def nearest_part(P, parts):
    best = np.full(len(P), np.inf)
    idx = np.zeros(len(P), int)
    for i, p in enumerate(parts):
        s = p.sdf(P)
        m = s < best
        best[m] = s[m]
        idx[m] = i
    return idx


def is_world(fn):
    return 'fn' in getattr(fn.__code__, 'co_freevars', ())


def world_inner(fn):
    return [c.cell_contents for c, nm in zip(fn.__closure__, fn.__code__.co_freevars) if nm == 'fn'][0]


NBINS = np.array([[x, y, z] for x in (-1, 0, 1) for y in (-1, 0, 1) for z in (-1, 0, 1) if (x, y, z) != (0, 0, 0)], float)
NBINS /= np.linalg.norm(NBINS, axis=1, keepdims=True)


def colour(M, P, bone, parts, Vl, Nl):
    W = M.world(bone)
    Pw = Vl @ W[:3, :3].T + W[:3, 3]
    Nw = Nl @ W[:3, :3].T
    owner = nearest_part(Vl, parts)
    col = np.zeros((len(Vl), 3))
    glow = np.zeros(len(Vl), bool)
    for ci in np.unique(owner):
        c = parts[ci].cube
        fn = P.mats[parts[ci].mat]
        sel = np.nonzero(owner == ci)[0]
        if is_world(fn):
            inner = world_inner(fn)
            bins = np.argmax(Nw[sel] @ NBINS.T, axis=1)
            for b in np.unique(bins):
                s2 = sel[bins == b]
                n = NBINS[b]
                face = 'top' if n[1] < -0.7 else ('bottom' if n[1] > 0.7 else 'north')
                cc, _, gl = inner(c, face, Pw[s2][:, None, :], n, 1, len(s2))
                cc = np.asarray(cc, float).reshape(len(s2), 3)
                under = 0.82 if n[1] > 0.6 else 1.0
                col[s2] = cc * light(n) * under
                if gl is not None:
                    glow[s2] = np.asarray(gl).reshape(len(s2), 4)[:, 3] > 110
        else:
            # flat painters (eyes, gems, emblems, feathers): one colour for the whole cube
            w = max(1, int(math.ceil(c.size[0]))); h = max(1, int(math.ceil(c.size[1])))
            cc, a, gl = fn(c, 'north', w, h, np.random.default_rng(0))
            cc = np.asarray(cc, float)
            a = np.asarray(a, float)
            if gl is not None and np.asarray(gl)[..., 3].max() > 0:
                g = np.asarray(gl)[..., 3]
                m = g > 0.5 * g.max()
                col[sel] = cc[m].mean(0)
                glow[sel] = True
            else:
                m = a > 0.5
                col[sel] = cc[m].mean(0) if m.any() else cc.reshape(-1, 3).mean(0)
    return np.clip(col, 0, 255), glow


def build(M, P, out_path, density=0.17, target=13000):
    bones, total = _build(M, P, out_path, density)
    if total < target * 0.75:
        bones, total = _build(M, P, out_path, min(0.6, density * target / max(total, 1)))
    return bones, total


def _build(M, P, out_path, density):
    M.pack()
    bones = []
    total = 0
    for name in M.order:
        b = M.bones[name]
        parts = bone_parts(M, name)
        mesh = bone_mesh(parts, density)
        if mesh is None:
            bones.append((name, b.parent or '', np.zeros((0, 3)), np.zeros((0, 3)), np.zeros((0, 3)), np.zeros(0, bool), np.zeros((0, 3), int)))
            continue
        V, F = mesh
        N = vertex_normals(V, F)
        col, glow = colour(M, P, name, parts, V, N)
        bones.append((name, b.parent or '', V, N, col, glow, F))
        total += len(F)
    with open(out_path, 'wb') as f:
        f.write(b'CMSH')
        f.write(struct.pack('>ii', 1, len(bones)))
        for name, parent, V, N, col, glow, F in bones:
            for s in (name, parent):
                e = s.encode('utf-8')
                f.write(struct.pack('>H', len(e)) + e)
            f.write(struct.pack('>ii', len(V), len(F)))
            if len(V):
                vb = np.zeros(len(V), dtype=[('p', '>f4', 3), ('n', '>f4', 3), ('c', 'u1', 4)])
                vb['p'] = V / 16.0
                vb['n'] = N
                vb['c'][:, :3] = col.astype(np.uint8)
                vb['c'][:, 3] = glow.astype(np.uint8)
                f.write(vb.tobytes())
                f.write(F.astype('>i4').tobytes())
    return bones, total


def preview(M, bones, size=600, yaw=-35, pitch=8, bg=(60, 64, 70)):
    """software render of the rest pose for checking"""
    from PIL import Image
    ya, pa = math.radians(yaw), math.radians(pitch)
    Vw, Nw, Cw, Fw = [], [], [], []
    off = 0
    for name, parent, V, N, col, glow, F in bones:
        if not len(V):
            continue
        W = M.world(name)
        Vw.append(V @ W[:3, :3].T + W[:3, 3]); Nw.append(N @ W[:3, :3].T)
        c = col.copy(); c[glow] = np.maximum(c[glow], 200)
        Cw.append(c); Fw.append(F + off); off += len(V)
    V = np.concatenate(Vw); N = np.concatenate(Nw); C = np.concatenate(Cw); F = np.concatenate(Fw)
    P = np.stack([-V[:, 0], -(V[:, 1] - 24), V[:, 2]], 1)
    N = np.stack([-N[:, 0], -N[:, 1], N[:, 2]], 1)
    Ry = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(pa), -math.sin(pa)], [0, math.sin(pa), math.cos(pa)]])
    R = Rx @ Ry
    P = P @ R.T; N = N @ R.T
    L = np.array([0.4, 0.8, -0.5]); L /= np.linalg.norm(L)
    shade = 0.55 + 0.45 * np.clip(np.abs(N @ L), 0, 1)
    C = C * shade[:, None]
    mn, mx = P[:, :2].min(0), P[:, :2].max(0)
    s = (size - 40) / max(mx - mn)
    S = np.stack([(-(P[:, 0] - (mn[0] + mx[0]) / 2)) * s + size / 2, -(P[:, 1] - (mn[1] + mx[1]) / 2) * s + size / 2, P[:, 2]], 1)
    img = np.zeros((size, size, 3)); img[:] = bg
    zb = np.full((size, size), 1e9)
    for tri in F:
        a, b, c = S[tri]
        x0, x1 = int(max(0, min(a[0], b[0], c[0]))), int(min(size - 1, max(a[0], b[0], c[0]) + 1))
        y0, y1 = int(max(0, min(a[1], b[1], c[1]))), int(min(size - 1, max(a[1], b[1], c[1]) + 1))
        if x1 < x0 or y1 < y0:
            continue
        den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
        if abs(den) < 1e-9:
            continue
        X, Y = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        l1 = ((b[1] - c[1]) * (X - c[0]) + (c[0] - b[0]) * (Y - c[1])) / den
        l2 = ((c[1] - a[1]) * (X - c[0]) + (a[0] - c[0]) * (Y - c[1])) / den
        l3 = 1 - l1 - l2
        m = (l1 >= -1e-4) & (l2 >= -1e-4) & (l3 >= -1e-4)
        if not m.any():
            continue
        Z = l1 * a[2] + l2 * b[2] + l3 * c[2]
        col = (C[tri[0]][None, None] * l1[..., None] + C[tri[1]][None, None] * l2[..., None] + C[tri[2]][None, None] * l3[..., None])
        ys, xs = np.nonzero(m)
        gy, gx = ys + y0, xs + x0
        zz = Z[ys, xs]
        cl = zz < zb[gy, gx]
        zb[gy[cl], gx[cl]] = zz[cl]
        img[gy[cl], gx[cl]] = col[ys[cl], xs[cl]]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
