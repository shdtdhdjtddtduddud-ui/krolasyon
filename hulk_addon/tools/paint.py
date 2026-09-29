"""Procedural painter for the Hulk atlas.  Stage 0..4:
0 = Bruce Banner (human skin, shirt, trousers)   1/2 = mid transformation
3 = Hulk                                          4 = Hulk in RAGE (glowing veins/eyes)
"""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from model import ATLAS, PAD

# stage parameters ---------------------------------------------------------------------
HUMAN = 1.0  # peach share per stage
STAGES = {
    0: dict(human=1.00, veins=0.00, shirt=1.00, glow=0.0, hair=0.0, snarl=0.0),
    1: dict(human=0.62, veins=0.35, shirt=0.55, glow=0.25, hair=0.4, snarl=0.4),
    2: dict(human=0.22, veins=0.75, shirt=0.15, glow=0.6, hair=0.8, snarl=0.8),
    3: dict(human=0.00, veins=1.00, shirt=0.00, glow=1.0, hair=1.0, snarl=1.0),
    4: dict(human=0.00, veins=1.9, shirt=0.00, glow=2.0, hair=1.0, snarl=1.0),
}

HULK_RAMP = [(0.0, (16, 22, 20)), (0.30, (46, 58, 52)), (0.58, (88, 102, 90)),
             (0.82, (128, 143, 124)), (1.0, (176, 188, 162))]
HUMAN_RAMP = [(0.0, (92, 52, 46)), (0.35, (168, 106, 86)), (0.65, (226, 172, 140)),
              (1.0, (250, 214, 184))]
RAGE_RAMP = [(0.0, (12, 26, 16)), (0.30, (40, 68, 44)), (0.58, (84, 122, 76)),
             (0.82, (128, 170, 100)), (1.0, (190, 224, 130))]


def ramp(t, stops):
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    xs = [s[0] for s in stops]
    for ch in range(3):
        out[..., ch] = np.interp(t, xs, [s[1][ch] for s in stops])
    return out


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a + 1e-9), 0, 1)
    return t * t * (3 - 2 * t)


def value_noise(h, w, rng, cell):
    gh, gw = max(2, int(h / cell) + 2), max(2, int(w / cell) + 2)
    g = rng.random((gh, gw)).astype(np.float32)
    ys = np.linspace(0, gh - 1.001, h)
    xs = np.linspace(0, gw - 1.001, w)
    y0, x0 = ys.astype(int), xs.astype(int)
    fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
    fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
    a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def fbm(h, w, rng, cell, octs=3):
    out = np.zeros((h, w), np.float32); amp = 1.0; tot = 0
    for _ in range(octs):
        out += amp * value_noise(h, w, rng, max(1.5, cell))
        tot += amp; amp *= 0.5; cell /= 2
    return out / tot


def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8))
    return np.asarray(im.filter(ImageFilter.GaussianBlur(r)), np.float32) / 255.0


# ---------------------------------------------------------------------------------- veins
def vein_layer(W, H, rng, ppu, density=1.0):
    """branching lightning-like veins, returns (core, glow) 0..1"""
    im = Image.new("L", (W, H), 0)
    dr = ImageDraw.Draw(im)
    n = max(1, int(round(W * H / (ppu * ppu * 55) * density)))
    n = min(n, 4)

    def walk(x, y, ang, length, width, depth):
        pts = [(x, y)]
        for _ in range(int(length)):
            ang += rng.normal(0, 0.55)
            x += math.cos(ang) * ppu * 0.5
            y += math.sin(ang) * ppu * 0.5
            pts.append((x, y))
            if depth < 3 and rng.random() < 0.10:
                walk(x, y, ang + rng.choice([-1, 1]) * rng.uniform(0.5, 1.2), length * 0.55, max(1, width - 1), depth + 1)
        dr.line(pts, fill=255, width=max(1, int(width)))

    for _ in range(n):
        side = rng.integers(0, 4)
        if side == 0: x, y, a = rng.random() * W, 0, math.pi / 2
        elif side == 1: x, y, a = rng.random() * W, H, -math.pi / 2
        elif side == 2: x, y, a = 0, rng.random() * H, 0
        else: x, y, a = W, rng.random() * H, math.pi
        walk(x, y, a + rng.normal(0, 0.4), rng.integers(14, 34), max(1, int(round(ppu / 3.2))), 0)
    core = np.asarray(im.filter(ImageFilter.GaussianBlur(max(0.5, ppu / 9))), np.float32) / 255.0
    core = np.clip(core * 1.25, 0, 1)
    glow = np.asarray(im.filter(ImageFilter.GaussianBlur(max(1.5, ppu / 2.2))), np.float32) / 255.0
    return core, glow


# ---------------------------------------------------------------------------------- faces
FACE_LIGHT = {"north": 1.0, "south": 0.72, "east": 0.84, "west": 0.84, "up": 1.12, "down": 0.5}


def make_face(cube, face, W, H, stage, faceidx):
    P = STAGES[stage]
    rng = np.random.default_rng(cube.seed * 131 + faceidx * 17)
    rng2 = np.random.default_rng(cube.seed * 977 + faceidx * 31 + 5)  # stage independent noise
    fw, fh = {"north": (cube.size[0], cube.size[1]), "south": (cube.size[0], cube.size[1]),
              "east": (cube.size[2], cube.size[1]), "west": (cube.size[2], cube.size[1]),
              "up": (cube.size[0], cube.size[2]), "down": (cube.size[0], cube.size[2])}[face]
    ppu = W / max(fw, 0.01)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u = (xx + 0.5) / W; v = (yy + 0.5) / H
    # distance from border in model units
    edge = np.minimum(np.minimum(u * fw, (1 - u) * fw), np.minimum(v * fh, (1 - v) * fh))
    mat, feat = cube.mat, cube.feat

    def pix(col):
        return np.ones((H, W, 3), np.float32) * np.array(col, np.float32)

    # ------------------------------------------------------------- non skin materials
    if mat in ("shorts", "waist", "fray"):
        f = fbm(H, W, rng2, ppu * 2.2, 3)
        thread = 0.5 + 0.5 * np.sin((xx * 0.9 + yy * 0.4) * 1.6 / max(1, ppu / 5))
        hulk_c = np.array((70, 52, 88) if mat != "fray" else (58, 44, 74), np.float32)
        human_c = np.array((118, 118, 128) if mat != "waist" else (96, 96, 108), np.float32)
        base = (hulk_c * (1 - P["human"]) + human_c * P["human"])
        shade = 0.62 + 0.55 * f + 0.06 * thread
        if mat == "waist":
            shade *= 0.85
        shade *= FACE_LIGHT[face] * (0.7 + 0.3 * smoothstep(0, 0.8, edge))
        # darker rips + light fibres near bottom edge
        rip = smoothstep(0.55, 0.95, fbm(H, W, rng2, ppu * 1.1, 3))
        shade *= 1 - 0.28 * rip
        rgb = base[None, None, :] * shade[..., None]
        rgb = np.where(rip[..., None] > 0.9, rgb * 1.25, rgb)
        a = np.ones((H, W), np.float32)
        if mat == "fray":
            # ragged tips
            tip = fbm(H, W, rng2, ppu * 0.9, 2)
            a = (v < 0.78 + 0.35 * tip).astype(np.float32) if face in ("north", "south", "east", "west") else a
            if stage == 0:
                a = np.ones_like(a)
        return rgb, a
    if mat == "hair":
        f = fbm(H, W, rng2, ppu * 1.0, 3)
        streak = 0.5 + 0.5 * np.sin(xx / max(1.0, ppu * 0.5) * 3.1 + f * 6)
        hulk_c = np.array((14, 16, 15), np.float32)
        human_c = np.array((70, 46, 30), np.float32)
        base = hulk_c * P["hair"] + human_c * (1 - P["hair"])
        shade = (0.55 + 0.9 * streak * f) * FACE_LIGHT[face]
        rgb = base[None, None, :] * shade[..., None] + np.array((16, 24, 20)) * (streak ** 6)[..., None] * P["hair"]
        return rgb, np.ones((H, W), np.float32)
    if mat == "teeth":
        f = fbm(H, W, rng2, ppu * 0.6, 2)
        col = pix((198, 196, 170)) * (0.7 + 0.4 * f)[..., None]
        gap = (np.sin(u * math.pi * 2 * max(3, int(cube.size[0] / 1.1))) > 0.86)
        col = np.where(gap[..., None], col * 0.35, col)
        if P["snarl"] < 0.3:
            col = pix((170, 118, 100)) * (0.75 + 0.3 * f)[..., None]  # lips when human
        return col * (0.65 + 0.35 * smoothstep(0, 0.4, edge))[..., None], np.ones((H, W), np.float32)
    if mat == "mouth":
        return pix((22, 8, 10)) * (0.6 + 0.5 * fbm(H, W, rng2, ppu, 2))[..., None], np.ones((H, W), np.float32)

    # ------------------------------------------------------------- skin
    big = fbm(H, W, rng2, ppu * 3.5, 3)
    fine = fbm(H, W, rng2, ppu * 0.5, 2)
    light = FACE_LIGHT[face]
    bw = 1.5 if max(cube.size) > 6 else 0.7
    bevel = smoothstep(0, bw, edge)
    t = 0.30 + 0.42 * light * (0.45 + 0.55 * bevel) + 0.30 * (big - 0.5) + 0.10 * (fine - 0.5)
    # soft top-left sheen for a wet, painted look
    sheen = np.exp(-(((u - 0.32) / 0.4) ** 2 + ((v - 0.28) / 0.36) ** 2))
    t += 0.13 * sheen * (1 if face in ("north", "up") else 0.55)
    # vertical occlusion
    t -= 0.10 * smoothstep(0.55, 1.0, v) * (0 if face in ("up", "down") else 1)

    if feat == "abs":
        # each abs block: strong rounded highlight, deep separation lines
        t = 0.30 + 0.5 * light * (0.35 + 0.65 * smoothstep(0, 1.1, edge)) + 0.22 * (big - 0.5) + 0.12 * sheen
    if feat in ("pec", "pec_top"):
        t += 0.06 * sheen
        if face == "north":
            t -= 0.24 * smoothstep(0.45, 1.0, v) * smoothstep(0, 0.6, v)  # underside shadow
    if feat in ("delt", "bicep", "tricep", "flexor", "extensor", "lat", "oblique"):
        t += 0.12 * (sheen - 0.2)
    if feat in ("fist", "finger", "thumb", "toe"):
        t -= 0.02 + 0.06 * (1 - bevel)
    if mat == "skin_dark":
        t -= 0.15

    human = P["human"]
    rage = 1.0 if stage == 4 else 0.0
    hulk_rgb = ramp(t, HULK_RAMP) * (1 - rage) + ramp(t + 0.04, RAGE_RAMP) * rage
    human_rgb = ramp(t + 0.10, HUMAN_RAMP)
    rgb = hulk_rgb * (1 - human) + human_rgb * human

    # ---- veins (bright yellow-green lightning) ---------------------------------------
    vs = P["veins"]
    if vs > 0 and mat.startswith("skin") and cube.feat not in ("skull",):
        dens = 1.0 if feat not in ("finger", "toe", "thumb") else 0.35
        core, glow = vein_layer(W, H, np.random.default_rng(cube.seed * 53 + faceidx), ppu, dens)
        vc = np.array((150, 172, 84), np.float32) if stage < 4 else np.array((150, 255, 90), np.float32)
        vm = np.clip(core * 0.55 * min(vs, 1.6), 0, 1)
        rgb = rgb * (1 - vm[..., None]) + vc * vm[..., None]
        rgb = rgb + np.array((60, 80, 20), np.float32) * (glow * 0.22 * vs)[..., None]
        if vs > 1.2:
            rgb = rgb + np.array((25, 70, 10), np.float32) * (glow * (vs - 1.2))[..., None]
    # ---- Bruce's shirt over torso/shoulders --------------------------------------------
    sh = P["shirt"]
    if sh > 0.02 and mat in ("skin_t",) and not (face == "down"):
        cover = (fbm(H, W, rng2, ppu * 2.0, 3) < (0.25 + 0.75 * sh)).astype(np.float32)
        cover = blur(cover, max(0.6, ppu / 6))
        if sh >= 0.98:
            cover = np.ones_like(cover)
        f = fbm(H, W, rng2, ppu * 1.4, 2)
        shirt = pix((104, 78, 128)) * (0.7 + 0.45 * f)[..., None] * (0.6 + 0.4 * smoothstep(0, 0.9, edge))[..., None] * light
        # torn edge highlight
        rim = np.clip(1 - np.abs(cover - 0.5) * 6, 0, 1)
        rgb = rgb * (1 - cover[..., None]) + shirt * cover[..., None]
        rgb = rgb + rim[..., None] * 18

    # ---- face details ---------------------------------------------------------------------
    if feat == "skull" and face == "north":
        rgb = paint_eyes(rgb, u, v, W, H, ppu, P, stage)
    if feat == "skull" and face in ("east", "west"):
        pass
    if feat == "brow":
        rgb = rgb * (0.7 + 0.3 * smoothstep(0, 0.5, v))[..., None] if face == "north" else rgb
    if feat == "jaw" and face == "north":
        # mouth line + snarl shadow under lip
        line = np.exp(-(((v - 0.93) / 0.05) ** 2)) * (0.5 + 0.5 * P["snarl"])
        rgb = rgb * (1 - 0.6 * line[..., None])
    if feat == "nose" and face == "north":
        for cx in (0.3, 0.7):
            nn = np.exp(-(((u - cx) / 0.13) ** 2 + ((v - 0.86) / 0.13) ** 2))
            rgb *= (1 - 0.7 * nn)[..., None]
    if feat == "toe" and face == "north":
        nail = smoothstep(0.55, 0.8, v) * (edge > 0.25)
        rgb = rgb * (1 - 0.35 * nail[..., None])
    if feat == "finger" and face == "north":
        rgb = rgb * (1 - 0.3 * smoothstep(0.6, 0.85, v))[..., None]

    rgb *= (0.90 + 0.10 * light)
    return rgb, np.ones((H, W), np.float32)


def paint_eyes(rgb, u, v, W, H, ppu, P, stage):
    glow = P["glow"]
    for cx in (0.27, 0.73):
        cy = 0.235
        # eye slit shape (angry, slanted downward toward nose)
        slant = (0.5 - abs(cx - 0.5)) * 0.0
        ex = (u - cx) / 0.15
        ey = (v - (cy + (0.06 if cx < 0.5 else 0.06) * 0.0)) / 0.055
        mask = np.clip(1 - (ex ** 2 + ey ** 2), 0, 1) ** 0.5
        # slanted lower lid
        tilt = (v - cy) - (-0.10 * (u - cx) * (1 if cx < 0.5 else -1))
        mask = mask * (tilt < 0.05)
        white = np.array((236, 240, 200), np.float32) if glow > 0.5 else np.array((230, 220, 210), np.float32)
        iris = np.array((200, 255, 70), np.float32) if glow > 0.5 else np.array((96, 60, 30), np.float32)
        core = np.exp(-((ex ** 2) + (ey ** 2)) * 1.4)
        col = iris * (1 - core[..., None]) + white * core[..., None] if glow > 0.5 else iris
        rgb = rgb * (1 - mask[..., None]) + col * mask[..., None]
        if glow > 0.3:
            halo = np.exp(-(ex ** 2 + (ey * 1.2) ** 2) * 0.35) * 0.5 * min(glow, 2.0)
            rgb = rgb + np.array((60, 110, 10), np.float32) * halo[..., None] * (1 - mask[..., None] * 0.7)
        # heavy shadow above (brow)
        rgb = rgb * (1 - 0.55 * np.exp(-(((v - (cy - 0.13)) / 0.09) ** 2)) * (np.abs(u - cx) < 0.22))[..., None]
    return rgb


# ---------------------------------------------------------------------------------- atlas
def paint_atlas(model, stage):
    atlas = np.zeros((ATLAS, ATLAS, 4), np.float32)
    for ci, c in enumerate(model.cubes):
        for fi, (f, (x, y, w, h)) in enumerate(c.uv.items()):
            if f == "down":
                rgb, a = make_face(c, f, 4, 4, stage, fi)
            else:
                rgb, a = make_face(c, f, w, h, stage, fi)
            if f == "down":
                col = rgb.reshape(-1, 3).mean(0) * 0.7
                rgb = np.ones((h, w, 3), np.float32) * col
                a = np.ones((h, w), np.float32)
            tile = np.concatenate([np.clip(rgb, 0, 255), (a * 255)[..., None]], -1)
            tile = np.pad(tile, ((PAD, PAD), (PAD, PAD), (0, 0)), mode="edge")
            atlas[y - PAD:y + h + PAD, x - PAD:x + w + PAD] = tile
    return Image.fromarray(np.clip(atlas, 0, 255).astype(np.uint8), "RGBA")
