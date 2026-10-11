"""Rig the Higgsfield meshes and write the .omx models + textures used by the mod.

.omx (little endian): 'OMX1', int bones, per bone (short len, utf8 name, int parent, 3 floats pivot),
int verts, per vertex (3f pos, 2f uv, 3f normal, byte b0, byte b1, float w0), int tris, 3 ints each.
"""
import os, struct, sys
import numpy as np
from PIL import Image
import meshlib
from rigs import RIGS

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '../src/main/resources/assets/omnitrix')


def seg_dist(p, a, b):
    ab = b - a
    t = np.clip(((p - a) @ ab) / max(ab @ ab, 1e-9), 0, 1)
    return np.linalg.norm(p - (a + t[:, None] * ab), axis=1)


def weights(v, rig):
    names = list(rig)
    D = np.full((len(v), len(names)), np.inf)
    for j, n in enumerate(names):
        parent, piv, end, c = rig[n]
        d = seg_dist(v, np.array(piv, float), np.array(end, float))
        ok = np.ones(len(v), bool)
        for k, axis in (('x', 0), ('y', 1), ('z', 2)):
            if k + 'min' in c: ok &= v[:, axis] >= c[k + 'min']
            if k + 'max' in c: ok &= v[:, axis] <= c[k + 'max']
        D[ok, j] = d[ok]
    # vertices no bone may claim fall back to the root bone ('body' or first)
    root = names.index('body') if 'body' in names else 0
    none = ~np.isfinite(D).any(1)
    D[none, root] = 0
    order = np.argsort(D, 1)
    b0, b1 = order[:, 0], order[:, 1]
    d0 = D[np.arange(len(v)), b0]
    d1 = D[np.arange(len(v)), b1]
    related = np.array([rig[names[a]][0] == names[b] or rig[names[b]][0] == names[a] for a, b in zip(b0, b1)])
    w0 = np.ones(len(v))
    blend = related & np.isfinite(d1) & (d1 < d0 * 1.8 + 0.03)
    p0, p1 = (d0 + 1e-3) ** -4, (d1 + 1e-3) ** -4
    w0[blend] = (p0 / (p0 + p1))[blend]
    b1 = np.where(blend, b1, b0)
    return names, b0, b1, w0


def normals(v, f):
    n = np.zeros_like(v)
    fn = np.cross(v[f[:, 1]] - v[f[:, 0]], v[f[:, 2]] - v[f[:, 0]])
    for k in range(3):
        np.add.at(n, f[:, k], fn)
    return n / np.maximum(np.linalg.norm(n, axis=1, keepdims=True), 1e-9)


def write(name, v, f, uv, tex, rig):
    names, b0, b1, w0 = weights(v, rig)
    n = normals(v, f)
    os.makedirs(os.path.join(RES, 'models/alien'), exist_ok=True)
    os.makedirs(os.path.join(RES, 'textures/alien'), exist_ok=True)
    with open(os.path.join(RES, 'models/alien', name + '.omx'), 'wb') as o:
        o.write(b'OMX1')
        o.write(struct.pack('<i', len(names)))
        for nm in names:
            parent, piv, _, _ = rig[nm]
            e = nm.encode()
            o.write(struct.pack('<h', len(e)) + e)
            o.write(struct.pack('<i3f', names.index(parent) if parent else -1, *piv))
        o.write(struct.pack('<i', len(v)))
        uvf = uv.copy()
        uvf[:, 1] = 1 - uvf[:, 1]
        for i in range(len(v)):
            o.write(struct.pack('<3f2f3fBBf', *v[i], *uvf[i], *n[i], int(b0[i]), int(b1[i]), float(w0[i])))
        o.write(struct.pack('<i', len(f)))
        o.write(np.asarray(f, '<i4').tobytes())
    tex.save(os.path.join(RES, 'textures/alien', name + '.png'))
    return names, b0, b1, w0


PALETTE = np.array([[230, 25, 75], [60, 180, 75], [255, 225, 25], [0, 130, 200], [245, 130, 48], [145, 30, 180], [70, 240, 240],
                    [240, 50, 230], [210, 245, 60], [250, 190, 212], [0, 128, 128], [220, 190, 255], [170, 110, 40], [128, 0, 0]], float)

if __name__ == '__main__':
    preview = sys.argv[1] if len(sys.argv) > 1 else None
    rows = []
    for name, rig in RIGS.items():
        v, f, uv, tex = meshlib.load(name)
        names, b0, b1, w0 = write(name, v, f, uv, tex, rig)
        print(name, len(v), 'verts', len(f), 'tris', 'bones', names)
        if preview:
            col = PALETTE[b0 % len(PALETTE)]
            ims = []
            for view in ('front', 'side'):
                im, cam = meshlib.render(v, f, uv, tex, view, size=300, extra=lambda tri: col[tri[0]])
                ims.append(im)
            row = Image.new('RGB', (600, 300))
            row.paste(ims[0], (0, 0)); row.paste(ims[1], (300, 0))
            rows.append(row)
    if preview:
        S = Image.new('RGB', (1800, 300 * ((len(rows) + 2) // 3)))
        for i, r in enumerate(rows):
            S.paste(r, ((i % 3) * 600, (i // 3) * 300))
        S.save(preview)
