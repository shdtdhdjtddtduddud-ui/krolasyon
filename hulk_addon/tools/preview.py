"""Tiny software renderer used to check the model / texture / poses without the game.
Conventions follow Bedrock: rotation [rx,ry,rz] degrees -> math angles (-rx, +ry, -rz), applied X then Y then Z."""
import math
import numpy as np
from PIL import Image


def rot_matrix(rx, ry, rz):
    ax, ay, az = math.radians(-rx), math.radians(ry), math.radians(-rz)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def face_corners(o, s):
    x0, y0, z0 = o; x1, y1, z1 = x0 + s[0], y0 + s[1], z0 + s[2]
    return {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "up": [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
        "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
    }


NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def bone_chain(model, name):
    ch = []
    while name:
        ch.append(name)
        name = model.bones[name]["parent"]
    return ch  # child -> root


def transform_point(model, bone, p, pose):
    p = np.array(p, float)
    for b in bone_chain(model, bone):
        pv = np.array(model.bones[b]["pivot"], float)
        pz = pose.get(b, {})
        rot = pz.get("rot", (0, 0, 0)); pos = pz.get("pos", (0, 0, 0)); sc = pz.get("scale", (1, 1, 1))
        q = (p - pv) * np.array(sc)
        q = rot_matrix(*rot) @ q
        p = pv + q + np.array(pos)
    return p


def render(model, atlas, geo="hulk", pose=None, yaw=0, pitch=0, size=(640, 900), scale=13.0, center=(0, 24, 0), bg=(58, 62, 72)):
    pose = pose or {}
    tex = np.asarray(atlas.convert("RGBA"))
    W, H = size
    img = np.zeros((H, W, 3), np.float32); img[:] = bg
    zbuf = np.full((H, W), 1e9, np.float32)
    cy_, sy_ = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    Ry = np.array([[cy_, 0, sy_], [0, 1, 0], [-sy_, 0, cy_]])
    Rp = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    V = Rp @ Ry
    light = np.array([0.35, 0.8, -0.5]); light /= np.linalg.norm(light)

    def project(p):
        q = V @ (np.array(p) - np.array(center))
        return np.array([W / 2 - q[0] * scale, H / 2 - q[1] * scale, q[2]])

    for c in model.cubes:
        if c.geo != geo:
            continue
        corners = face_corners(c.origin, c.size)
        for f, quad in corners.items():
            wp = [transform_point(model, c.bone, q, pose) for q in quad]
            # world normal for shading + backface cull
            n = np.array(NORMALS[f], float)
            # approximate normal via transformed edges
            e1 = wp[1] - wp[0]; e2 = wp[3] - wp[0]
            nn = np.cross(e1, e2)
            ln = np.linalg.norm(nn)
            if ln < 1e-9:
                continue
            nn /= ln
            # orientation check with the untransformed normal direction
            ref = np.cross(np.array(quad[1]) - np.array(quad[0]), np.array(quad[3]) - np.array(quad[0]))
            if np.dot(ref, n) < 0:
                nn = -nn
            vn = V @ nn
            if vn[2] > 0.0:  # facing away (camera looks toward +z... camera at -z, normal must have -z)
                continue
            shade = 0.55 + 0.45 * max(0.0, float(np.dot(nn, light)))
            x, y, w, h = c.uv[f]
            uvs = [(x, y), (x + w, y), (x + w, y + h), (x, y + h)]
            pts = [project(p) for p in wp]
            for tri in ((0, 1, 2), (0, 2, 3)):
                _raster(img, zbuf, tex, [pts[i] for i in tri], [uvs[i] for i in tri], shade)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


def _raster(img, zbuf, tex, P, UV, shade):
    H, W = zbuf.shape
    xs = [p[0] for p in P]; ys = [p[1] for p in P]
    x0, x1 = max(0, int(math.floor(min(xs)))), min(W - 1, int(math.ceil(max(xs))))
    y0, y1 = max(0, int(math.floor(min(ys)))), min(H - 1, int(math.ceil(max(ys))))
    if x1 < x0 or y1 < y0:
        return
    (ax, ay, az), (bx, by, bz), (cx, cy, cz) = P
    den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
    if abs(den) < 1e-9:
        return
    gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
    l1 = ((by - cy) * (gx - cx) + (cx - bx) * (gy - cy)) / den
    l2 = ((cy - ay) * (gx - cx) + (ax - cx) * (gy - cy)) / den
    l3 = 1 - l1 - l2
    inside = (l1 >= -0.002) & (l2 >= -0.002) & (l3 >= -0.002)
    if not inside.any():
        return
    z = l1 * az + l2 * bz + l3 * cz
    u = l1 * UV[0][0] + l2 * UV[1][0] + l3 * UV[2][0]
    v = l1 * UV[0][1] + l2 * UV[1][1] + l3 * UV[2][1]
    ui = np.clip(u.astype(int), 0, tex.shape[1] - 1); vi = np.clip(v.astype(int), 0, tex.shape[0] - 1)
    col = tex[vi, ui]
    sub = zbuf[y0:y1 + 1, x0:x1 + 1]
    ok = inside & (z < sub) & (col[..., 3] > 128)
    sub[ok] = z[ok]
    im = img[y0:y1 + 1, x0:x1 + 1]
    im[ok] = col[..., :3][ok] * shade
