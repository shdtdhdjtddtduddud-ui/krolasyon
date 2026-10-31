"""Effect models + textures: marble / shield / black-hole sphere, shadow spike, black-hole disk, item icons."""
import math, numpy as np
from PIL import Image, ImageDraw, ImageFilter
from core import Model
from paint import Painter, glow_img
from shapes import ellipsoid_l, texel_world, Noise3, lerp, ramp, smooth

# ---------------------------------------------------------------- sphere (marble, shield, black hole core)
SPH = Model('sphere', 128, 128)
SPH.bone('ball', None, (0, 0, 0))
ellipsoid_l(SPH, 'ball', (0, 0, 0), (8, 8, 8), 'glass', axis='y', slices=6)
SPH.pack()


def sphere_painter(kind):
    p = Painter(SPH)
    nz = Noise3(31 + kind, freq=0.6)
    hl = np.array((-0.45, -0.75, -0.5)); hl /= np.linalg.norm(hl)

    @p.mat('glass')
    def glass(c, face, W, H, r):
        Pw, n = texel_world(SPH, c, face, W, H)
        nn = Pw / (np.linalg.norm(Pw, axis=-1, keepdims=True) + 1e-6)
        d = nn @ hl
        rim = 1 - np.abs(nn @ np.array((0, 0, -1.0)))
        glow = np.zeros((H, W, 4), np.float32)
        if kind == 0:      # marble: deep blue glass
            col = ramp(0.35 + 0.35 * (d + 1) / 2 + 0.1 * nz(Pw), [(10, 18, 52), (34, 58, 132), (70, 104, 190), (150, 180, 240)])
            alpha = 0.62 + 0.3 * rim
            spec = smooth(0.82, 0.93, d)
            col = lerp(col, (250, 252, 255), spec)
            alpha = np.maximum(alpha, spec)
        elif kind == 1:    # reflect / guard shield: violet energy shell
            sw = np.sin(Pw[..., 0] * 1.4 + Pw[..., 1] * 1.1 + nz(Pw) * 6)
            col = ramp(0.3 + 0.4 * smooth(0.2, 1, sw) + 0.3 * rim, [(30, 10, 60), (90, 40, 170), (170, 120, 255), (235, 220, 255)])
            alpha = 0.22 + 0.35 * smooth(0.3, 1, sw) + 0.25 * rim
            glow = glow_img(H, W, (150, 90, 255), smooth(0.6, 1, sw) * 0.5)
            glow[..., :3] = col
        else:              # black hole core: pitch black with a violet event-horizon rim
            col = np.zeros((H, W, 3), np.float32)
            col[:] = (2, 0, 6)
            alpha = np.ones((H, W))
            ring = smooth(0.7, 1.0, rim)
            glow = glow_img(H, W, (170, 80, 255), ring * 0.9)
        return col, np.clip(alpha, 0, 1), glow
    return p


# ---------------------------------------------------------------- shadow spike (rises from the ground)
SPK = Model('shadow_spike', 128, 128)
SPK.bone('root', None, (0, 24, 0))
SPK.bone('main', 'root', (0, 24, 0))
for i, (w, h) in enumerate(((10, 12), (7.5, 12), (5, 11), (2.6, 9))):
    y = 24 - sum(hh for _, hh in ((10, 12), (7.5, 12), (5, 11), (2.6, 9))[:i + 1])
    SPK.cube('main', (-w / 2, y, -w / 2), (w, h + 0.5, w), 'shadow')
for k in range(5):
    a = k * 72 + 20
    n = f'side{k}'
    SPK.bone(n, 'root', (math.cos(math.radians(a)) * 5, 24, math.sin(math.radians(a)) * 5), (math.sin(math.radians(a)) * 34, 0, -math.cos(math.radians(a)) * 34))
    for i, (w, h) in enumerate(((5, 9), (3, 8), (1.4, 6))):
        y0 = -sum(hh for _, hh in ((5, 9), (3, 8), (1.4, 6))[:i + 1])
        SPK.cube_l(n, (-w / 2, y0, -w / 2), (w, h + 0.4, w), 'shadow')
SPK.pack()


def spike_painter():
    p = Painter(SPK)
    nz = Noise3(41, freq=0.4)

    @p.mat('shadow')
    def shadow(c, face, W, H, r):
        Pw, n = texel_world(SPK, c, face, W, H)
        h = 24 - Pw[..., 1]
        v = np.sin(Pw[..., 0] * 1.2 + Pw[..., 2] * 0.9 + nz(Pw) * 8)
        vein = smooth(0.82, 0.95, v)
        col = ramp(0.2 + 0.3 * nz(Pw) + 0.2 * smooth(0, 40, h), [(4, 2, 10), (16, 10, 30), (40, 24, 70)])
        col = lerp(col, (160, 90, 255), vein)
        g = glow_img(H, W, (150, 80, 255), vein * 0.9 + smooth(30, 46, h) * 0.4)
        return col, np.ones((H, W)), g
    return p


# ---------------------------------------------------------------- flat textures
def disk_texture(size=256):
    yy, xx = np.mgrid[0:size, 0:size]
    c = (size - 1) / 2
    dx, dy = (xx - c) / c, (yy - c) / c
    r = np.hypot(dx, dy)
    a = np.arctan2(dy, dx)
    swirl = 0.5 + 0.5 * np.sin(a * 3 + r * 14)
    band = smooth(0.28, 0.42, r) * (1 - smooth(0.78, 1.0, r))
    inten = band * (0.45 + 0.55 * swirl)
    img = np.zeros((size, size, 4), np.float32)
    col = ramp(inten, [(40, 0, 80), (120, 40, 220), (220, 160, 255), (255, 240, 255)])
    img[..., :3] = col
    img[..., 3] = np.clip(inten * 1.3, 0, 1) * 255
    img[..., :3] *= img[..., 3:4] / 255
    return Image.fromarray(img.astype(np.uint8), 'RGBA')


def marble_icon(size=32):
    """inventory icon: dark blue glass marble with Kabkaba's silhouette inside"""
    S = size * 8
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    yy, xx = np.mgrid[0:S, 0:S]
    c = S / 2
    r = np.hypot(xx - c, yy - c) / (S * 0.46)
    nx, ny = (xx - c) / (S * 0.46), (yy - c) / (S * 0.46)
    nz_ = np.sqrt(np.clip(1 - r ** 2, 0, 1))
    L = np.array((-0.5, -0.6, 0.62)); L /= np.linalg.norm(L)
    d = nx * L[0] + ny * L[1] + nz_ * L[2]
    col = ramp(0.25 + 0.45 * (d + 1) / 2, [(8, 14, 40), (26, 44, 108), (54, 86, 170), (120, 150, 220)])
    a = (r < 1).astype(np.float32)
    arr = np.zeros((S, S, 4), np.float32)
    arr[..., :3] = col
    arr[..., 3] = a * 255
    # silhouette of Kabkaba (mane blob, horns, tail) in darker blue-violet
    sil = Image.new('L', (S, S), 0)
    dr = ImageDraw.Draw(sil)
    k = S / 256
    dr.ellipse([88 * k, 92 * k, 168 * k, 168 * k], fill=255)                    # mane
    dr.ellipse([130 * k, 120 * k, 190 * k, 160 * k], fill=255)                  # body
    dr.ellipse([70 * k, 124 * k, 104 * k, 150 * k], fill=255)                   # muzzle
    for x0 in (96, 140, 158, 176):
        dr.rectangle([x0 * k, 150 * k, (x0 + 9) * k, 182 * k], fill=255)       # legs
    dr.polygon([(100 * k, 96 * k), (92 * k, 64 * k), (84 * k, 50 * k), (110 * k, 92 * k)], fill=255)   # horns
    dr.polygon([(124 * k, 92 * k), (124 * k, 60 * k), (116 * k, 46 * k), (134 * k, 92 * k)], fill=255)
    dr.line([(186 * k, 136 * k), (204 * k, 110 * k), (206 * k, 70 * k), (194 * k, 52 * k)], fill=255, width=int(8 * k))
    sil = np.asarray(sil.filter(ImageFilter.GaussianBlur(2 * k))).astype(np.float32) / 255 * a
    arr[..., :3] = lerp(arr[..., :3], (52, 46, 120), sil * 0.85)
    eye = np.hypot(xx - 100 * k, yy - 112 * k) < 5 * k
    arr[eye, :3] = (220, 230, 255)
    # specular highlight + rim
    spec = np.hypot(xx - c * 1.38, yy - c * 0.62) < S * 0.1
    arr[spec & (a > 0), :3] = lerp(arr[spec & (a > 0), :3], (255, 255, 255), 0.85)
    rim = (r > 0.9) & (r < 1)
    arr[rim, :3] *= 0.55
    im = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA').resize((size, size), Image.LANCZOS)
    return im


# ---------------------------------------------------------------- equirectangular textures for the smooth spheres
def _equirect(W=256, H=128):
    yy, xx = np.mgrid[0:H, 0:W]
    lon = (xx + 0.5) / W * 2 * math.pi           # u
    lat = math.pi / 2 - (yy + 0.5) / H * math.pi  # v: top = +90
    n = np.stack([np.cos(lat) * np.cos(lon), np.sin(lat), np.cos(lat) * np.sin(lon)], -1)
    return n


def _img(col, alpha):
    a = np.zeros(col.shape[:2] + (4,), np.float32)
    a[..., :3] = col
    a[..., 3] = np.clip(alpha, 0, 1) * 255
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), 'RGBA')


def marble_glass_tex():
    """deep blue glass: lighter on top, dark underneath, soft swirl, one big specular highlight"""
    n = _equirect()
    nz = Noise3(77, freq=2.2)
    sw = nz(n * 3)
    up = n[..., 1]
    col = ramp(0.42 + 0.3 * (up + 1) / 2 + 0.12 * (sw - 0.5), [(6, 12, 40), (24, 44, 120), (52, 86, 178), (110, 150, 230)])
    alpha = 0.58 + 0.18 * (1 - (up + 1) / 2) + 0.06 * sw
    # specular highlight up-front-right and a faint secondary one
    for d, w, s in (((0.45, 0.62, -0.64), 0.9, 1.0), ((-0.5, -0.35, -0.8), 0.96, 0.35)):
        d = np.array(d); d /= np.linalg.norm(d)
        k = smooth(w, w + (1 - w) * 0.75, n @ d) * s
        col = lerp(col, (250, 252, 255), k)
        alpha = np.maximum(alpha, k * 0.95)
    return _img(col, alpha)


def shield_tex():
    n = _equirect()
    nz = Noise3(78, freq=1.6)
    lon = np.arctan2(n[..., 2], n[..., 0])
    hexes = 0.5 + 0.5 * np.sin(lon * 9) * np.sin(n[..., 1] * 14)
    sw = nz(n * 4)
    k = smooth(0.55, 0.95, sw) * 0.6 + smooth(0.75, 0.95, hexes) * 0.4
    col = ramp(0.25 + 0.7 * k, [(40, 14, 80), (110, 50, 200), (190, 140, 255), (240, 225, 255)])
    return _img(col, 0.18 + 0.55 * k)


def hole_tex():
    n = _equirect()
    nz = Noise3(79, freq=2.0)
    v = smooth(0.82, 0.95, nz(n * 4))
    col = lerp(np.zeros(n.shape[:2] + (3,), np.float32) + (3, 1, 8), (90, 40, 160), v)
    return _img(col, np.ones(n.shape[:2]))


def halo_tex():
    n = _equirect()
    nz = Noise3(80, freq=1.4)
    k = 0.5 + 0.5 * nz(n * 3)
    col = ramp(k, [(30, 8, 60), (90, 34, 170), (150, 90, 240)])
    return _img(col, np.ones(n.shape[:2]))


# ---------------------------------------------------------------- exploding fish (Behmud's fish bombs / torpedoes)
FISH = Model('fish', 128, 128)
FISH.bone('fish', None, (0, 0, 0))
ellipsoid_l(FISH, 'fish', (0, 0, 0), (3.2, 3.6, 7), 'fish', axis='z', slices=5)
FISH.bone('fish_tail', 'fish', (0, 0, 6))
FISH.cube_l('fish_tail', (-0.4, -4, 0), (0.8, 8, 5), 'fish_fin')
FISH.cube_l('fish', (-0.4, -6, -2), (0.8, 3, 5), 'fish_fin')
for sx in (-1, 1):
    FISH.cube_l('fish', (sx * 3.0 - 0.5, -1.6, -5.2), (1, 1.6, 1.6), 'fish_eye')
FISH.pack()


def fish_painter():
    p = Painter(FISH)

    @p.mat('fish')
    def fish(c, face, W, H, r):
        Pw, n = texel_world(FISH, c, face, W, H)
        belly = smooth(-0.5, 2.5, Pw[..., 1])
        col = lerp((30, 70, 120), (255, 150, 60), belly)
        stripe = smooth(0.7, 0.95, np.sin(Pw[..., 2] * 1.4))
        col = lerp(col, (20, 30, 60), stripe * (1 - belly))
        g = glow_img(H, W, (255, 140, 50), belly * 0.9)
        return col, np.ones((H, W)), g

    @p.mat('fish_fin')
    def fin(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32); col[:] = (90, 170, 220)
        return col, np.ones((H, W)), None

    @p.mat('fish_eye')
    def eye(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32); col[:] = (255, 240, 200)
        return col, np.ones((H, W)), glow_img(H, W, (255, 220, 160), np.ones((H, W)))
    return p


def star_tex():
    n = _equirect()
    nz = Noise3(81, freq=2.5)
    k = nz(n * 3)
    col = ramp(k, [(120, 160, 255), (200, 220, 255), (255, 255, 255)])
    return _img(col, np.ones(n.shape[:2]))


def water_tex(W=128, H=256):
    """tileable swirling water for the whirlpool / waterspout mesh (scrolls in the renderer)"""
    yy, xx = np.mgrid[0:H, 0:W]
    u, v = xx / W * 2 * math.pi, yy / H * 2 * math.pi
    s = np.sin(u * 3 + v * 2 + np.sin(v * 3) * 1.5) * 0.5 + np.sin(u * 5 - v * 4) * 0.3 + np.sin(u * 2 + v * 6) * 0.2
    t = 0.5 + 0.5 * s / 1.0
    col = ramp(t, [(20, 70, 140), (50, 130, 200), (140, 210, 240), (235, 250, 255)])
    alpha = 0.45 + 0.4 * smooth(0.55, 0.95, t)
    return _img(col, alpha)


def behmud_icon(size=32):
    """particle / fallback icon: pale cyan marble with a dark whale inside"""
    S = size * 8
    yy, xx = np.mgrid[0:S, 0:S]
    c = S / 2
    r = np.hypot(xx - c, yy - c) / (S * 0.46)
    nz_ = np.sqrt(np.clip(1 - r ** 2, 0, 1))
    d = (-(xx - c) * 0.5 - (yy - c) * 0.6) / (S * 0.46) + nz_ * 0.62
    col = ramp(0.3 + 0.4 * (d + 1) / 2, [(20, 60, 90), (60, 140, 170), (140, 210, 225), (220, 245, 250)])
    a = (r < 1).astype(np.float32)
    arr = np.zeros((S, S, 4), np.float32)
    arr[..., :3] = col; arr[..., 3] = a * 255
    whale = (((xx - c * 0.95) / (S * 0.3)) ** 2 + ((yy - c * 1.05) / (S * 0.1)) ** 2) < 1
    arr[whale & (a > 0), :3] = (14, 22, 44)
    spec = np.hypot(xx - c * 1.38, yy - c * 0.62) < S * 0.1
    arr[spec & (a > 0), :3] = (250, 252, 255)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA').resize((size, size), Image.LANCZOS)


def sand_tex(W=128, H=256):
    """tileable whirling sand for the sand vortex mesh"""
    yy, xx = np.mgrid[0:H, 0:W]
    u, v = xx / W * 2 * math.pi, yy / H * 2 * math.pi
    s = np.sin(u * 3 + v * 2 + np.sin(v * 3) * 1.5) * 0.5 + np.sin(u * 7 - v * 5) * 0.25 + np.sin(u * 2 + v * 9) * 0.25
    grain = np.random.default_rng(3).random((H, W))
    t = np.clip(0.5 + 0.45 * s + 0.15 * (grain - 0.5), 0, 1)
    col = ramp(t, [(120, 92, 56), (178, 142, 92), (214, 186, 132), (240, 222, 176)])
    alpha = 0.55 + 0.35 * smooth(0.4, 0.9, t)
    return _img(col, alpha)


def sarkan_icon(size=32):
    S = size * 8
    yy, xx = np.mgrid[0:S, 0:S]
    c = S / 2
    r = np.hypot(xx - c, yy - c) / (S * 0.46)
    nz_ = np.sqrt(np.clip(1 - r ** 2, 0, 1))
    d = (-(xx - c) * 0.5 - (yy - c) * 0.6) / (S * 0.46) + nz_ * 0.62
    col = ramp(0.3 + 0.4 * (d + 1) / 2, [(70, 50, 24), (150, 112, 60), (214, 180, 120), (245, 230, 190)])
    a = (r < 1).astype(np.float32)
    arr = np.zeros((S, S, 4), np.float32)
    arr[..., :3] = col; arr[..., 3] = a * 255
    body = (np.abs(xx - c) < S * 0.1) & (yy > c * 0.7) & (yy < c * 1.45)
    head = np.hypot(xx - c, yy - c * 0.6) < S * 0.07
    arr[(body | head) & (a > 0), :3] = (36, 36, 42)
    spec = np.hypot(xx - c * 1.38, yy - c * 0.62) < S * 0.1
    arr[spec & (a > 0), :3] = (250, 250, 245)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA').resize((size, size), Image.LANCZOS)


def rock_spike_painter():
    """same jagged spike model, as broken earth / stone for Nirumend"""
    p = Painter(SPK)
    nz = Noise3(44, freq=0.5)

    @p.mat('shadow')
    def rock(c, face, W, H, r):
        Pw, n = texel_world(SPK, c, face, W, H)
        h = 24 - Pw[..., 1]
        t = 0.35 + 0.35 * nz(Pw) + 0.15 * smooth(0, 40, h)
        col = ramp(t, [(70, 56, 40), (118, 96, 70), (160, 136, 104), (196, 176, 144)])
        crack = smooth(0.86, 0.96, np.abs(np.sin(Pw[..., 0] * 1.1 + Pw[..., 1] * 0.7 + nz(Pw) * 6)))
        col = lerp(col, (40, 30, 22), crack * 0.8)
        return col, np.ones((H, W)), np.zeros((H, W, 4), np.float32)
    return p


def electric_tex():
    """crackling electric shell: branching bright lines on a faint blue haze"""
    n = _equirect()
    nz = Noise3(82, freq=2.0)
    v = np.abs(np.sin(nz(n * 3) * 14)) 
    bolt = smooth(0.1, 0.0, v)
    col = ramp(0.2 + 0.8 * bolt, [(40, 60, 120), (120, 170, 255), (220, 240, 255), (255, 255, 255)])
    return _img(col, 0.12 + 0.85 * bolt)


def nirumend_icon(size=32):
    S = size * 8
    yy, xx = np.mgrid[0:S, 0:S]
    c = S / 2
    r = np.hypot(xx - c, yy - c) / (S * 0.46)
    nz_ = np.sqrt(np.clip(1 - r ** 2, 0, 1))
    d = (-(xx - c) * 0.5 - (yy - c) * 0.6) / (S * 0.46) + nz_ * 0.62
    col = ramp(0.3 + 0.4 * (d + 1) / 2, [(80, 60, 30), (160, 128, 80), (220, 196, 140), (248, 236, 200)])
    a = (r < 1).astype(np.float32)
    arr = np.zeros((S, S, 4), np.float32)
    arr[..., :3] = col; arr[..., 3] = a * 255
    body = (((xx - c) / (S * 0.24)) ** 2 + ((yy - c * 1.05) / (S * 0.14)) ** 2) < 1
    arr[body & (a > 0), :3] = (120, 90, 50)
    tusk = (np.abs(xx - c * 0.72) < S * 0.02) & (yy > c) & (yy < c * 1.35)
    arr[tusk & (a > 0), :3] = (240, 180, 80)
    spec = np.hypot(xx - c * 1.38, yy - c * 0.62) < S * 0.1
    arr[spec & (a > 0), :3] = (250, 250, 245)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA').resize((size, size), Image.LANCZOS)
