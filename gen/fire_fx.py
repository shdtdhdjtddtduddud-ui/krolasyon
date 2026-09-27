"""Effect assets for the fire / shadow forms: thrown trident model, wave strips, ground runes, projectile sprites,
HUD ability icons and the 32px weapon item sprites."""
import math, numpy as np
from PIL import Image
from core import Model
from paint import Painter, noise2, lerp, glow_img
from fpaint import solid, emissive, flame_mat
from scipy.ndimage import gaussian_filter

rng = np.random.default_rng(9)

# ---- thrown hell trident (points along -z) ----
SP = Model('hell_spear', 128, 64)
SP.bone('root', None, (0, 16, 0))
SP.cube_l('root', (-0.8, -0.8, -20), (1.6, 1.6, 44), 'shaft')
SP.cube_l('root', (-5.5, -1, -22), (11, 2, 2), 'trident')
for x, L in ((-4.6, 10), (0, 13), (4.6, 10)):
    SP.cube_l('root', (x - 0.9, -0.9, -22 - L), (1.8, 1.8, L), 'trident')
    SP.cube_l('root', (x - 0.5, -0.5, -25 - L), (1.0, 1.0, 3), 'tip')
SP.bone('fl0', 'root', (0, 16, -26))
SP.cube_l('fl0', (-5, -10, 0), (10, 12, 0), 'flame', plane=True)
SPP = Painter(SP)
SPP.mats.update({'shaft': solid(((40, 6, 6), (100, 16, 14), (150, 40, 30)), (24, 4, 4), 0.3, 0.0),
                 'trident': solid(((150, 30, 16), (230, 90, 30), (255, 170, 80)), (24, 4, 4), 0.3, -0.3),
                 'tip': emissive((255, 210, 100), (255, 180, 60), 1.0), 'flame': flame_mat()})


def strip(w, h, kind):
    """tileable wave wall strips: fire / shadow"""
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    ph = xx / w * math.tau
    n = np.zeros((h, w))
    for k, amp in ((2, 1.0), (5, 0.5), (11, 0.25)):
        n += amp * np.sin(ph * k + rng.random() * 6 + yy / h * (3 + k)) * np.cos(ph * (k + 1) + rng.random() * 6)
    n = (n - n.min()) / (n.max() - n.min())
    v = yy / (h - 1)
    img = np.zeros((h, w, 4))
    tongues = 0.25 + 0.2 * np.sin(ph * 7 + n * 4) + 0.1 * np.sin(ph * 13)
    body = v > tongues
    heat = np.clip((v - tongues) * 2.5, 0, 1)
    if kind == 'fire':
        col = lerp(lerp((255, 245, 180), (255, 150, 30), heat), (170, 30, 10), np.clip(v * 1.2 - 0.3, 0, 1))
        a = body * (0.9 - 0.3 * v)
    else:
        col = lerp(lerp((255, 60, 70), (120, 0, 16), heat), (10, 0, 4), np.clip(v * 1.2 - 0.2, 0, 1))
        a = body * (0.95 - 0.2 * v)
    streak = 0.5 + 0.5 * np.sin(ph * 9 + v * 14 + n * 6)
    col = lerp(col, (255, 255, 220) if kind == 'fire' else (255, 120, 120), (streak > 0.94) * 0.5)
    img[..., :3] = col
    img[..., 3] = np.clip(a, 0, 1) * 255
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


def rune(kind, S=256):
    """premultiplied additive ground runes"""
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    c = S / 2
    d = np.hypot(xx - c, yy - c) / c
    ang = np.arctan2(yy - c, xx - c)
    m = np.exp(-((d - 0.95) / 0.012) ** 2)
    m = np.maximum(m, np.exp(-((d - 0.86) / 0.008) ** 2) * 0.8)
    ticks = (np.abs(((ang / (2 * np.pi) * 36) % 1) - 0.5) < 0.14) & (d > 0.865) & (d < 0.945)
    m = np.maximum(m, ticks * 0.9)
    if kind == 'sun':
        rays = (0.5 + 0.5 * np.cos(ang * 12)) ** 6
        m = np.maximum(m, (d < 0.75) * (d > 0.3) * rays * np.clip(1 - (d - 0.3) / 0.45, 0, 1))
        m = np.maximum(m, np.exp(-((d - 0.3) / 0.02) ** 2))
        m = np.maximum(m, (d < 0.22) * 0.9)
        col = (255, 190, 70)
    elif kind == 'ember':
        for k in range(3):
            m = np.maximum(m, np.exp(-((d - (0.35 + k * 0.17)) / 0.01) ** 2) * (0.6 + 0.4 * (np.sin(ang * (6 + k * 3)) > 0)))
        col = (255, 120, 40)
    else:  # hell gate: pentagram-like star
        pts = [(c + math.cos(math.radians(-90 + k * 144)) * c * 0.8, c + math.sin(math.radians(-90 + k * 144)) * c * 0.8) for k in range(5)]
        for k in range(5):
            (x0, y0), (x1, y1) = pts[k], pts[(k + 1) % 5]
            vx, vy = x1 - x0, y1 - y0
            t = np.clip(((xx - x0) * vx + (yy - y0) * vy) / (vx * vx + vy * vy), 0, 1)
            dist = np.hypot(xx - (x0 + t * vx), yy - (y0 + t * vy))
            m = np.maximum(m, np.exp(-(dist / (S * 0.008)) ** 2))
        col = (255, 50, 30)
    img = np.zeros((S, S, 4))
    img[..., :3] = np.array(col) * np.clip(0.8 + 0.2 * m, 0, 1)[..., None]
    img[..., :3] = lerp(img[..., :3], (255, 255, 230), np.clip(m - 0.7, 0, 1) * 1.5)
    img[..., 3] = np.clip(m, 0, 1) * 255
    img[..., :3] *= img[..., 3:4] / 255.0
    return Image.fromarray(img.astype(np.uint8), 'RGBA')


def sprite(kind, S=64):
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    c = S / 2
    d = np.hypot(xx - c, yy - c) / c
    ang = np.arctan2(yy - c, xx - c)
    img = np.zeros((S, S, 4))
    if kind == 'fireball':
        n = noise2(S, S, rng, 6, 3)
        rr = 0.62 + 0.3 * n + 0.08 * np.sin(ang * 7)
        a = np.clip((rr - d) * 6, 0, 1)
        heat = np.clip(1 - d / rr, 0, 1)
        col = lerp(lerp((200, 40, 10), (255, 150, 30), np.clip(heat * 2, 0, 1)), (255, 250, 210), np.clip(heat * 2 - 1, 0, 1))
    elif kind == 'shadowball':
        n = noise2(S, S, rng, 6, 3)
        rr = 0.62 + 0.3 * n
        a = np.clip((rr - d) * 6, 0, 1)
        heat = np.clip(1 - d / rr, 0, 1)
        col = lerp((120, 0, 20), (255, 60, 70), heat)
    elif kind == 'crescent':
        # crescent opening toward +y (forward), texture wide
        u = (xx - c) / c
        v = (yy - c) / c
        outer = np.hypot(u, v * 1.8) < 1.0
        inner = np.hypot(u, (v - 0.35) * 1.8) < 0.95
        m = outer & ~inner & (v < 0.25)
        a = gaussian_filter(m.astype(float), 1.2)
        heat = np.clip(a * 1.5, 0, 1)
        col = lerp((255, 100, 20), (255, 245, 200), heat)
    else:  # flame dragon head seen from above/front, stylised
        u = (xx - c) / c
        v = (yy - c) / c
        head = (np.abs(u) < 0.55 - 0.35 * np.clip(v, 0, 1)) & (v > -0.7) & (v < 0.9)
        horns = ((np.abs(np.abs(u) - 0.45 - (-v - 0.4) * 0.5) < 0.1) & (v < -0.3) & (v > -0.95))
        jaw = (np.abs(u) < 0.32) & (v > 0.4) & (v < 0.95)
        m = head | horns | jaw
        n = noise2(S, S, rng, 5, 3)
        a = gaussian_filter(m.astype(float), 1.0) * (0.7 + 0.3 * n)
        eyes = (np.hypot(np.abs(u) - 0.25, v + 0.15) < 0.09)
        col = lerp((200, 40, 10), (255, 170, 40), np.clip(n + 0.2, 0, 1))
        col[eyes] = (255, 255, 220)
        a = np.maximum(a, eyes * 1.0)
    img[..., :3] = col
    img[..., 3] = np.clip(a, 0, 1) * 255
    img[..., :3] *= img[..., 3:4] / 255.0
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


# ---------------- HUD icons ----------------
def _seg(xx, yy, p0, p1, w):
    (x0, y0), (x1, y1) = p0, p1
    vx, vy = x1 - x0, y1 - y0
    L2 = vx * vx + vy * vy + 1e-9
    t = np.clip(((xx - x0) * vx + (yy - y0) * vy) / L2, 0, 1)
    return np.hypot(xx - (x0 + t * vx), yy - (y0 + t * vy)) < w


def glyph(name, xx, yy, S):
    c = S / 2
    d = np.hypot(xx - c, yy - c)
    ang = np.arctan2(yy - c, xx - c)
    m = np.zeros_like(xx, dtype=bool)
    if name == 'slam':
        m |= _seg(xx, yy, (c, 5), (c, 21), 2.2)
        m |= _seg(xx, yy, (c - 5, 8), (c + 5, 8), 1.2)
        for k in (-1, 1):
            m |= _seg(xx, yy, (c, 25), (c + k * 10, 20), 1.0)
            m |= _seg(xx, yy, (c, 25), (c + k * 12, 27), 1.0)
    elif name == 'spin':
        m |= (np.abs(d - 9) < 1.6) & (np.sin(ang * 3) > -0.3)
        m |= _seg(xx, yy, (c, c), (c + 12, c - 4), 1.4)
    elif name == 'seal':
        m |= np.abs(d - 11) < 1.2
        m |= (d < 11) & ((0.5 + 0.5 * np.cos(ang * 8)) ** 8 > 0.5) & (d > 4)
        m |= d < 3.5
    elif name == 'dash':
        m |= _seg(xx, yy, (6, c + 5), (24, c - 5), 1.8)
        for k in (0, 5, 10):
            m |= _seg(xx, yy, (4 + k * 0.5, c + 9 - k * 0.3), (12 + k * 0.6, c + 5 - k * 0.5), 0.8)
        m |= (np.hypot(xx - 24, yy - (c - 5)) < 3)
    elif name == 'meteor':
        m |= np.hypot(xx - 21, yy - 21) < 5
        for k in (-2, 0, 2):
            m |= _seg(xx, yy, (21 + k, 21 - k), (7 + k * 2, 7 - k * 2), 1.1)
    elif name == 'breath':
        m |= (xx > 8) & (np.abs(yy - c) < (xx - 8) * 0.55) & (xx < 28) & (np.sin(xx * 0.9 + yy * 0.5) > -0.4)
        m |= np.hypot(xx - 7, yy - c) < 4
    elif name == 'twin':
        m |= np.hypot(xx - 10, yy - 11) < 4.5
        m |= np.hypot(xx - 22, yy - 21) < 4.5
        m |= _seg(xx, yy, (4, 5), (10, 11), 1.0) | _seg(xx, yy, (16, 15), (22, 21), 1.0)
    elif name == 'shield':
        m |= (np.abs(xx - c) < 9 - np.clip(yy - 16, 0, 20) * 0.6) & (yy > 6) & (yy < 28)
        m &= ~((np.abs(xx - c) < 6 - np.clip(yy - 16, 0, 20) * 0.6) & (yy > 9) & (yy < 25) & ((xx + yy) % 4 > 1))
    elif name == 'dive':
        m |= _seg(xx, yy, (c, 4), (c, 22), 1.8)
        m |= _seg(xx, yy, (c - 6, 16), (c, 23), 1.4) | _seg(xx, yy, (c + 6, 16), (c, 23), 1.4)
        m |= (np.abs(yy - 27) < 1.2) & (np.abs(xx - c) < 11)
    elif name == 'dragon':
        t = np.linspace(0, 1, 60)
        for i in range(59):
            x0, y0 = 5 + 22 * t[i], c + 8 * math.sin(t[i] * 7)
            x1, y1 = 5 + 22 * t[i + 1], c + 8 * math.sin(t[i + 1] * 7)
            m |= _seg(xx, yy, (x0, y0), (x1, y1), 1.2 + 2.2 * t[i])
    elif name == 'blink':
        m |= (np.abs(d - 10) < 1.2) & (np.cos(ang) > 0)
        m |= np.hypot(xx - 9, yy - c) < 3
        m |= np.hypot(xx - 23, yy - c) < 3
    elif name == 'rain':
        for x0 in (8, 14, 20, 26):
            for y0 in (6, 16):
                m |= _seg(xx, yy, (x0 + (y0 % 5), y0), (x0 - 3 + (y0 % 5), y0 + 7), 1.2)
    elif name == 'nova':
        m |= (np.abs(d - 11) < 1.6)
        m |= (d < 9) & ((0.5 + 0.5 * np.cos(ang * 6)) ** 4 > 0.6)
    elif name == 'crescent':
        for k, dy in enumerate((-7, 0, 7)):
            dd = np.hypot(xx - 10, (yy - c - dy) * 1.8)
            m |= (np.abs(dd - 14) < 1.4) & (xx > 16)
    elif name == 'phoenix':
        m |= _seg(xx, yy, (c, 8), (c, 24), 1.6)
        for k in (-1, 1):
            m |= _seg(xx, yy, (c, 14), (c + k * 12, 6), 1.4)
            m |= _seg(xx, yy, (c + k * 12, 6), (c + k * 9, 14), 1.0)
        m |= np.hypot(xx - c, yy - 7) < 2.5
    elif name == 'flurry':
        for k in range(4):
            m |= _seg(xx, yy, (6 + k * 5, 6), (10 + k * 5, 26), 1.0)
    elif name == 'tails':
        for k in (-1, 1):
            t = np.linspace(0, 1, 40)
            for i in range(39):
                x0, y0 = c + k * 12 * t[i], 6 + 20 * t[i] - 6 * math.sin(t[i] * 3.14)
                x1, y1 = c + k * 12 * t[i + 1], 6 + 20 * t[i + 1] - 6 * math.sin(t[i + 1] * 3.14)
                m |= _seg(xx, yy, (x0, y0), (x1, y1), 1.3)
            m |= np.hypot(xx - (c + k * 12), yy - 26) < 2.5
    elif name == 'thorns':
        for x0, h in ((8, 14), (14, 20), (20, 16), (26, 10)):
            m |= (np.abs(xx - x0) < 2.2 * (yy - (28 - h)) / h) & (yy > 28 - h) & (yy < 28)
    elif name == 'awaken':
        for k in (-1, 1):
            m |= _seg(xx, yy, (c + k * 4, 14), (c + k * 11, 4), 1.6)
        m |= np.hypot((xx - c) / 1.2, yy - 19) < 6
        m &= ~(np.hypot(xx - c - 2.5, yy - 18) < 1.2) & ~(np.hypot(xx - c + 2.5, yy - 18) < 1.2)
    elif name == 'spear':
        m |= _seg(xx, yy, (5, 27), (25, 7), 1.0)
        for k in (-3, 0, 3):
            m |= _seg(xx, yy, (21 + k * 0.7, 11 + k * 0.7), (27 + k * 0.7, 5 + k * 0.7), 1.0)
    elif name == 'charge':
        m |= _seg(xx, yy, (4, c), (28, c), 1.4)
        m |= _seg(xx, yy, (22, c - 5), (28, c), 1.2) | _seg(xx, yy, (22, c + 5), (28, c), 1.2)
        for k in (-6, 6):
            m |= _seg(xx, yy, (4, c + k), (14, c + k), 0.8)
    elif name == 'whirl':
        m |= (np.abs(d - 7) < 1.4) | (np.abs(d - 12) < 1.1) & (np.cos(ang * 2) > 0)
        m |= np.hypot(xx - (c + 12), yy - c) < 2.2
    elif name == 'pillars':
        for x0, h in ((7, 10), (13, 18), (19, 22), (25, 14)):
            m |= (np.abs(xx - x0) < 1.8) & (yy > 28 - h) & (yy < 28)
        m |= (np.abs(yy - 28) < 1) & (np.abs(xx - c) < 13)
    elif name == 'gate':
        m |= np.abs(d - 11) < 1.2
        for k in range(5):
            a0 = math.radians(-90 + k * 144)
            a1 = math.radians(-90 + (k + 2) * 144)
            m |= _seg(xx, yy, (c + math.cos(a0) * 10, c + math.sin(a0) * 10), (c + math.cos(a1) * 10, c + math.sin(a1) * 10), 0.8)
    else:  # crest
        for k in range(5):
            a = math.radians(-90 + (k - 2) * 26)
            L = 12 - abs(k - 2) * 2.5
            m |= _seg(xx, yy, (c, c + 6), (c + math.cos(a) * L, c + 6 + math.sin(a) * L), 1.6 * (1 - 0.2 * abs(k - 2)))
    return m


def icons(names, col, hi, bg=(30, 12, 10)):
    S = 32
    Q = 4
    sheet = np.zeros((S, S * 6, 4))
    for k, name in enumerate(list(names) + ['crest']):
        yy, xx = (np.mgrid[0:S * Q, 0:S * Q] + 0.5) / Q
        c = S / 2
        d = np.hypot(xx - c, yy - c)
        img = np.zeros((S * Q, S * Q, 4))
        bgm = d < S / 2 - 0.5
        img[bgm] = [*bg, 255]
        ring = (d > S / 2 - 2.2) & bgm
        img[ring] = [150, 120, 70, 255]
        m = glyph(name, xx, yy, S) & bgm & ~ring
        mf = m.astype(float)
        img[..., :3] = np.where(m[..., None], lerp(col, hi, np.clip(1 - d / S * 1.5, 0, 1)), img[..., :3])
        g = gaussian_filter(mf, 5) * 0.8 * bgm
        img[..., :3] = np.clip(img[..., :3] + g[..., None] * np.array(col) * 0.8, 0, 255)
        sheet[:, k * S:(k + 1) * S] = img.reshape(S, Q, S, Q, 4).mean((1, 3))
    return Image.fromarray(np.clip(sheet, 0, 255).astype(np.uint8), 'RGBA')


FORM_ICONS = {
    'solar': (('slam', 'spin', 'seal', 'dash', 'meteor'), (255, 150, 40), (255, 240, 170)),
    'dragon': (('breath', 'twin', 'shield', 'dive', 'dragon'), (255, 110, 40), (255, 220, 140)),
    'flame': (('blink', 'rain', 'nova', 'crescent', 'phoenix'), (255, 170, 50), (255, 250, 200)),
    'shade': (('blink', 'flurry', 'tails', 'thorns', 'awaken'), (230, 30, 40), (255, 160, 160)),
    'lancer': (('spear', 'charge', 'whirl', 'pillars', 'gate'), (255, 90, 30), (255, 210, 120)),
}


# ---------------- weapon item sprites ----------------
def _outline(img):
    shape = img[..., 3] > 0
    o = np.zeros_like(shape)
    o[1:] |= shape[:-1]; o[:-1] |= shape[1:]; o[:, 1:] |= shape[:, :-1]; o[:, :-1] |= shape[:, 1:]
    o &= ~shape
    img[o] = [20, 8, 6, 255]
    return img


def weapon_sprite(kind):
    S = 32
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    G = np.array([9.5, 22.5])
    u = np.array([1, -1]) / math.sqrt(2)
    v = np.array([1, 1]) / math.sqrt(2)
    px, py = xx - G[0], yy - G[1]
    s = px * u[0] + py * u[1]
    w = px * v[0] + py * v[1]
    img = np.zeros((S, S, 4))

    def paint(mask, color):
        img[mask] = [*color, 255]

    if kind == 'solar':
        L = 27
        hw = np.where(s < L - 6, 3.4, 3.4 * np.clip((L - s) / 6, 0, 1))
        blade = (s > 1) & (s < L) & (np.abs(w) < hw)
        paint(blade, (86, 26, 26))
        paint(blade & (np.abs(w + 0.4 * np.sin(s * 0.6)) < 0.8), (255, 200, 60))
        paint(blade & (w > hw - 0.9), (140, 60, 50))
        guard = (np.abs(w) < 6.5) & (s > -1) & (s < 1.6 + np.abs(w) * 0.3)
        paint(guard, (214, 160, 54))
        paint(np.hypot(s - 0.3, w) < 1.3, (255, 70, 30))
        paint((s > -7) & (s <= -1) & (np.abs(w) < 1.1), (40, 24, 20))
        paint(np.hypot(s + 8, w) < 1.6, (240, 190, 70))
    elif kind == 'dragon':
        shaft = (s > -9) & (s < 18) & (np.abs(w) < 0.8)
        paint(shaft, (60, 24, 18))
        paint(shaft & (np.round(s) % 5 == 0), (206, 160, 64))
        curve = w - (s - 17) ** 2 * 0.06
        blade = (s > 16) & (s < 29) & (np.abs(curve) < 2.6 - (s - 16) * 0.12) & (curve > -2.2)
        paint(blade, (70, 22, 18))
        paint(blade & (np.abs(curve - 0.3 * np.sin(s)) < 0.5), (255, 120, 30))
        ring = np.abs(np.hypot(s - 15, w) - 2.6) < 0.8
        paint(ring, (206, 160, 64))
        paint(np.hypot(s - 15, w) < 1.6, (255, 110, 40))
        fl = (s < -8) & (s > -12) & (np.abs(w) < (s + 12) * 0.5)
        paint(fl, (255, 160, 40))
    elif kind == 'flame':
        L = 26
        hw = np.where(s < L - 8, 2.2, 2.2 * np.clip((L - s) / 8, 0, 1)) + 0.4 * np.sin(s * 1.3)
        blade = (s > 1) & (s < L) & (np.abs(w) < hw)
        heat = np.clip(1 - np.abs(w) / np.maximum(hw, 0.1), 0, 1)
        img[blade] = np.concatenate([lerp((255, 110, 20), (255, 245, 190), heat[blade]), np.full((blade.sum(), 1), 255)], 1)
        paint((np.abs(w) < 4.5) & (s > -0.8) & (s < 1), (200, 160, 80))
        paint((s > -7) & (s <= -0.8) & (np.abs(w) < 1.0), (70, 44, 30))
        paint(np.hypot(s + 8, w) < 1.4, (255, 150, 40))
    elif kind == 'shade':
        L = 27
        hw = np.where(s < L - 5, 2.6, 2.6 * np.clip((L - s) / 5, 0, 1)) + (np.sin(s * 2.1) > 0.6) * 1.2 * (w > 0)
        blade = (s > 1) & (s < L) & (np.abs(w) < hw)
        paint(blade, (24, 20, 26))
        paint(blade & (np.abs(w - np.sin(s * 0.8) * 0.8) < 0.5), (230, 20, 30))
        paint(blade & (w < -hw + 0.8), (70, 64, 74))
        paint((np.abs(w) < 3.5) & (s > -0.8) & (s < 1), (40, 36, 44))
        paint((s > -7) & (s <= -0.8) & (np.abs(w) < 1.0), (90, 20, 26))
    else:  # lancer trident
        shaft = (s > -11) & (s < 19) & (np.abs(w) < 0.8)
        paint(shaft, (110, 20, 16))
        paint(shaft & (np.round(s) % 4 == 0), (200, 50, 30))
        paint((s > 18) & (s < 20) & (np.abs(w) < 4.5), (230, 90, 30))
        for off in (-3.6, 0, 3.6):
            L = 29 if off == 0 else 26.5
            prong = (s >= 19) & (s < L) & (np.abs(w - off) < 0.8)
            paint(prong, (230, 90, 30))
            paint(prong & (s > L - 2), (255, 220, 110))
        fl = (s < -10) & (s > -13) & (np.abs(w) < (s + 13) * 0.6)
        paint(fl, (255, 150, 40))
    return Image.fromarray(np.clip(_outline(img), 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    out = Image.new('RGBA', (32 * 5 * 6, 32), (40, 40, 50, 255))
    for i, k in enumerate(('solar', 'dragon', 'flame', 'shade', 'lancer')):
        out.alpha_composite(weapon_sprite(k), (i * 32, 0))
    out.resize((out.width * 4, 128), Image.NEAREST).save('/tmp/claude-0/weapons.png')
    sh = Image.new('RGBA', (192, 32 * 5), (0, 0, 0, 255))
    for i, (k, (n, c, h)) in enumerate(FORM_ICONS.items()):
        sh.alpha_composite(icons(n, c, h), (0, i * 32))
    sh.resize((192 * 3, 160 * 3), Image.NEAREST).save('/tmp/claude-0/ficons.png')
    ims = [rune('sun'), rune('ember'), rune('gate'), sprite('fireball').resize((256, 256)), sprite('crescent').resize((256, 256)), sprite('dragon').resize((256, 256))]
    o = Image.new('RGBA', (256 * 6, 256 + 128), (30, 30, 36, 255))
    for i, im in enumerate(ims):
        o.alpha_composite(im, (i * 256, 0))
    o.alpha_composite(strip(128, 64, 'fire').resize((768, 128)), (0, 256))
    o.alpha_composite(strip(128, 64, 'shadow').resize((768, 128)), (768, 256))
    o.save('/tmp/claude-0/ffx.png')
