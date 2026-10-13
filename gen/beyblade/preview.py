"""Software preview of libra_mesh.bin with its textures (same conventions as the in-game renderer)."""
import math, os, struct, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade')


def load():
    data = open(os.path.join(ASSETS, 'models/libra_mesh.bin'), 'rb').read()
    magic, n = struct.unpack_from('>ii', data, 0)
    off = 8
    groups = {}
    for _ in range(n):
        gid, cnt = struct.unpack_from('>ii', data, off)
        off += 8
        arr = np.frombuffer(data, '>f4', cnt * 32, off).astype(np.float32).reshape(cnt, 4, 8)
        off += cnt * 32 * 4
        groups[gid] = arr
    return groups


def render(yaw, pitch, size=700, glow=0.0, out=None):
    g = load()
    face = np.asarray(Image.open(os.path.join(ASSETS, 'textures/entity/libra_face.png')).convert('RGBA')).astype(np.float32)
    parts = np.asarray(Image.open(os.path.join(ASSETS, 'textures/entity/libra_parts.png')).convert('RGBA')).astype(np.float32)
    gl = np.asarray(Image.open(os.path.join(ASSETS, 'textures/entity/libra_glow.png')).convert('RGBA')).astype(np.float32)
    ya, pa = math.radians(yaw), math.radians(pitch)
    Ry = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(pa), -math.sin(pa)], [0, math.sin(pa), math.cos(pa)]])
    V = Rx @ Ry
    img = np.zeros((size, size, 3), np.float32); img[:] = (40, 44, 52)
    zb = np.full((size, size), 1e9, np.float32)
    light = np.array([0.2, 1.0, -0.7]); light /= np.linalg.norm(light)
    sc = size / 3.2
    for gid, quads in g.items():
        tex = parts if gid == 2 else face
        for q in quads:
            P = (V @ (q[:, :3] - np.array([0, 0.7, 0])).T).T
            N = (V @ q[:, 5:8].T).T
            S = np.stack([P[:, 0] * sc + size / 2, -P[:, 1] * sc + size / 2, -P[:, 2]], 1)
            for tri in ((0, 1, 2), (0, 2, 3)):
                a, b, c = S[list(tri)]
                den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
                if abs(den) < 1e-9:
                    continue
                xmin, xmax = int(max(0, min(a[0], b[0], c[0]))), int(min(size - 1, max(a[0], b[0], c[0]) + 1))
                ymin, ymax = int(max(0, min(a[1], b[1], c[1]))), int(min(size - 1, max(a[1], b[1], c[1]) + 1))
                if xmax < xmin or ymax < ymin:
                    continue
                X, Y = np.meshgrid(np.arange(xmin, xmax + 1) + 0.5, np.arange(ymin, ymax + 1) + 0.5)
                l1 = ((b[1] - c[1]) * (X - c[0]) + (c[0] - b[0]) * (Y - c[1])) / den
                l2 = ((c[1] - a[1]) * (X - c[0]) + (a[0] - c[0]) * (Y - c[1])) / den
                l3 = 1 - l1 - l2
                m = (l1 >= -1e-3) & (l2 >= -1e-3) & (l3 >= -1e-3)
                if not m.any():
                    continue
                Z = l1 * a[2] + l2 * b[2] + l3 * c[2]
                uvs = q[list(tri), 3:5]
                u = l1 * uvs[0, 0] + l2 * uvs[1, 0] + l3 * uvs[2, 0]
                v = l1 * uvs[0, 1] + l2 * uvs[1, 1] + l3 * uvs[2, 1]
                n = l1[..., None] * N[tri[0]] + l2[..., None] * N[tri[1]] + l3[..., None] * N[tri[2]]
                shade = 0.45 + 0.55 * np.abs(n @ light) / (np.linalg.norm(n, axis=-1) + 1e-6)
                tx = np.clip((u * tex.shape[1]).astype(int), 0, tex.shape[1] - 1)
                ty = np.clip((v * tex.shape[0]).astype(int), 0, tex.shape[0] - 1)
                col = tex[ty, tx]
                m &= col[..., 3] > 10
                m &= Z < zb[ymin:ymax + 1, xmin:xmax + 1]
                ys, xs = np.nonzero(m)
                c3 = col[ys, xs, :3] * shade[ys, xs, None]
                if glow and gid == 0:
                    gx = np.clip((u * gl.shape[1]).astype(int), 0, gl.shape[1] - 1)
                    gy = np.clip((v * gl.shape[0]).astype(int), 0, gl.shape[0] - 1)
                    c3 = c3 + gl[gy[ys, xs], gx[ys, xs], :3] * glow
                img[ys + ymin, xs + xmin] = c3
                zb[ys + ymin, xs + xmin] = Z[ys, xs]
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
    if out:
        im.save(out)
    return im


if __name__ == '__main__':
    out = sys.argv[1]
    ims = [render(0, 90, 420), render(20, 35, 420), render(0, 8, 420), render(45, 55, 420, glow=0.8)]
    W = Image.new('RGB', (840, 840))
    for i, im in enumerate(ims):
        W.paste(im, ((i % 2) * 420, (i // 2) * 420))
    W.save(out)
