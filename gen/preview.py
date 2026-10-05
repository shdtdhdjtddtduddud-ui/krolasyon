"""Tiny software renderer that mimics Minecraft's ModelPart cubes + UVs, used to eyeball models without the game."""
import math
import numpy as np
from PIL import Image
import tex as T


def rot_zyx(rx, ry, rz):
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def model_quads(m, pose=None):
    """Returns list of (verts4x3 in model space (y down), uv4x2 in texels)."""
    pose = pose or {}
    quads = []
    # bone world transforms (MC model space)
    mats = {}
    for b in m.order:
        if b.parent:
            px, py, pz = b.pivot[0] - b.parent.pivot[0], -(b.pivot[1] - b.parent.pivot[1]), b.pivot[2] - b.parent.pivot[2]
        else:
            px, py, pz = b.pivot[0], 24 - b.pivot[1], b.pivot[2]
        rx, ry, rz = [a * math.pi / 180 for a in b.rot]
        if b.name in pose:
            ex = pose[b.name]
            rx, ry, rz = rx + ex[0], ry + ex[1], rz + ex[2]
        R = rot_zyx(rx, ry, rz)
        local = np.eye(4)
        local[:3, :3] = R
        local[:3, 3] = [px, py, pz]
        mats[b.name] = (mats[b.parent.name] @ local) if b.parent else local
        M = mats[b.name]
        for c in b.cubes:
            ox = c.x - b.pivot[0]
            oy = b.pivot[1] - c.y - c.h
            oz = c.z - b.pivot[2]
            g = c.inflate
            x0, y0, z0 = ox - g, oy - g, oz - g
            x1, y1, z1 = ox + c.w + g, oy + c.h + g, oz + c.d + g
            if c.mirror:
                x0, x1 = x1, x0
            v = [np.array(p) for p in [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]]
            u, vv, dx, dy, dz = c.u, c.v, c.w, c.h, c.d
            U = [u, u + dz, u + dz + dx, u + dz + dx + dx, u + dz + dx + dz, u + dz + dx + dz + dx]
            V = [vv, vv + dz, vv + dz + dy]
            polys = [((5, 4, 0, 1), U[1], V[0], U[2], V[1]), ((2, 3, 7, 6), U[2], V[1], U[3], V[0]), ((0, 4, 7, 3), U[0], V[1], U[1], V[2]),
                     ((1, 0, 3, 2), U[1], V[1], U[2], V[2]), ((5, 1, 2, 6), U[2], V[1], U[4], V[2]), ((4, 5, 6, 7), U[4], V[1], U[5], V[2])]
            for idx, ua, va, ub, vb in polys:
                uvs = [(ub, va), (ua, va), (ua, vb), (ub, vb)]
                vs = [v[i] for i in idx]
                if c.mirror:
                    vs = vs[::-1]
                    uvs = uvs[::-1]
                wv = [(M @ np.append(p, 1))[:3] for p in vs]
                quads.append((np.array(wv), np.array(uvs, dtype=float)))
    return quads


def render(m, texture, yaw=-35, pitch=15, size=360, pose=None, bg=(24, 30, 44)):
    quads = model_quads(m, pose)
    th, tw = texture.shape[:2]
    sx, sy = tw / m.tex_w, th / m.tex_h
    yr, pr = math.radians(yaw), math.radians(pitch)
    Ry = np.array([[math.cos(yr), 0, math.sin(yr)], [0, 1, 0], [-math.sin(yr), 0, math.cos(yr)]])
    Rx = np.array([[1, 0, 0], [0, math.cos(pr), -math.sin(pr)], [0, math.sin(pr), math.cos(pr)]])
    R = Rx @ Ry
    allv = np.concatenate([q[0] for q in quads])
    # model is y down, looks at -z: flip to y up and view from the front (camera on -z side)
    def tf(p):
        q = p.copy()
        q[:, 1] = -q[:, 1]
        q[:, 2] = -q[:, 2]
        q[:, 0] = -q[:, 0]
        return q @ R.T
    tv = tf(allv)
    mn, mx = tv.min(0), tv.max(0)
    scale = size * 0.85 / max(mx[0] - mn[0], mx[1] - mn[1], 1)
    cx, cy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
    img = np.zeros((size, size, 3))
    img[:] = np.array(bg) / 255
    zb = np.full((size, size), -1e9)
    light = np.array([0.4, 0.8, -0.45])
    light /= np.linalg.norm(light)
    for verts, uvs in quads:
        p = tf(verts)
        scr = np.stack([(p[:, 0] - cx) * scale + size / 2, size / 2 - (p[:, 1] - cy) * scale, p[:, 2]], 1)
        n = np.cross(p[1] - p[0], p[2] - p[0])
        if np.linalg.norm(n) < 1e-9:
            continue
        n /= np.linalg.norm(n)
        bright = 0.55 + 0.45 * max(0, float(n @ light))
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = scr[list(tri)]
            ua, ub, uc = uvs[list(tri)]
            x0, x1 = int(max(0, min(a[0], b[0], c[0]))), int(min(size - 1, max(a[0], b[0], c[0])) + 1)
            y0, y1 = int(max(0, min(a[1], b[1], c[1]))), int(min(size - 1, max(a[1], b[1], c[1])) + 1)
            if x1 <= x0 or y1 <= y0:
                continue
            ys, xs = np.mgrid[y0:y1, x0:x1] + 0.5
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            l1 = ((b[1] - c[1]) * (xs - c[0]) + (c[0] - b[0]) * (ys - c[1])) / den
            l2 = ((c[1] - a[1]) * (xs - c[0]) + (a[0] - c[0]) * (ys - c[1])) / den
            l3 = 1 - l1 - l2
            inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
            if not inside.any():
                continue
            z = l1 * a[2] + l2 * b[2] + l3 * c[2]
            u = l1 * ua[0] + l2 * ub[0] + l3 * uc[0]
            v = l1 * ua[1] + l2 * ub[1] + l3 * uc[1]
            ti = np.clip((v * sy).astype(int), 0, th - 1)
            tj = np.clip((u * sx).astype(int), 0, tw - 1)
            col = texture[ti, tj]
            region = zb[y0:y1, x0:x1]
            ok = inside & (z > region) & (col[:, :, 3] > 0.1)
            region[ok] = z[ok]
            img[y0:y1, x0:x1][ok] = col[:, :, :3][ok] * bright
    return img


def sheet(models_textures, path, cols=4, size=300):
    tiles = []
    for name, m, texture in models_textures:
        a = render(m, texture, -35, 12, size)
        b = render(m, texture, 145, 12, size)
        tile = np.concatenate([a, b], 1)
        im = Image.fromarray((np.clip(tile, 0, 1) * 255).astype(np.uint8))
        from PIL import ImageDraw
        ImageDraw.Draw(im).text((6, 6), name, fill=(255, 255, 255))
        tiles.append(np.asarray(im))
    rows = [np.concatenate(tiles[i:i + cols] + [np.zeros_like(tiles[0])] * (cols - len(tiles[i:i + cols])), 1) for i in range(0, len(tiles), cols)]
    Image.fromarray(np.concatenate(rows, 0)).save(path)
