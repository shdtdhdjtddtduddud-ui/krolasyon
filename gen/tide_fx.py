"""Aigoar effect assets: water geyser model, vortex / tidal wave strip textures, ability icons, Tide Blade item sprite."""
import math, numpy as np
from PIL import Image
from core import Model, rot_zyx
from paint import Painter, noise2, lerp, glow_img
import aigoar

rng = np.random.default_rng(5)
CY = np.array([42, 206, 214], float)
CY_HI = np.array([200, 255, 252], float)
CY_LO = np.array([8, 90, 120], float)

# ---- water geyser (erupts from the ground) ----
GM = Model('tide_geyser', 256, 128)
GM.bone('root', None, (0, 24, 0))
for i, (w, h, yr) in enumerate(((16, 48, 0), (16, 48, 90), (12, 40, 45), (12, 40, 135))):
    GM.bone(f'j{i}', 'root', (0, 24, 0), (0, yr, 0))
    GM.cube_l(f'j{i}', (-w / 2, -h, 0), (w, h, 0), 'jet', plane=True)
GM.cube_l('root', (-2.5, -44, -2.5), (5, 44, 5), 'core')
for i in range(8):
    a = i / 8 * math.tau
    GM.bone(f's{i}', 'root', (math.cos(a) * 6, 24, math.sin(a) * 6), (-30, 90 - math.degrees(a), 0))
    GM.cube_l(f's{i}', (-5, -10, 0), (10, 10, 0), 'splash', plane=True)
GP = Painter(GM)
GP.mats['splash'] = aigoar.P.mats['splash']
GP.mats['core'] = lambda c, f, W, H, r: aigoar.water(c, f, W, H, r, glow=0.5, bright=0.1)


@GP.mat('jet')
def jet(c, face, W, H, r):
    S = 3
    Hs, Ws = H * S, W * S
    yy, xx = np.mgrid[0:Hs, 0:Ws] / S
    u = (xx - W / 2) / (W / 2)
    v = 1 - yy / H                       # 0 at ground, 1 at top
    n = noise2(Hs, Ws, r, 4 * S, 3)
    width = 0.45 + 0.35 * v ** 2 + (n - 0.5) * 0.35     # spray widens at the crest
    inside = np.abs(u) < width * (1 - np.clip((v - 0.8) / 0.2, 0, 1) * (1 - n) * 1.4)
    # droplets flying from the crest
    for _ in range(14):
        cx, cy = r.random() * W, H * (r.random() * 0.35)
        inside |= np.hypot(xx - cx, yy - cy) < 0.35 + r.random() * 0.6
    streak = 0.5 + 0.5 * np.sin(u * 9 + n * 5)
    col = lerp(CY_LO, CY, np.clip(v * 0.6 + 0.4 * streak, 0, 1))
    col = lerp(col, CY_HI, np.clip((v - 0.6) * 2 + (streak > 0.9) * 0.6, 0, 1))
    col = col.reshape(H, S, W, S, 3).mean((1, 3))
    a = (inside.astype(float).reshape(H, S, W, S).mean((1, 3)) > 0.4).astype(float)
    return col, a, glow_img(H, W, (60, 255, 245), a * 0.55)


def strip(w, h, kind):
    """tileable RGBA strips: 'vortex' swirl streaks, 'wave' water wall with foam crest"""
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    ph = xx / w * math.tau
    n = np.zeros((h, w))
    for k, amp in ((2, 1.0), (5, 0.5), (11, 0.25)):
        n += amp * np.sin(ph * k + rng.random() * 6 + yy / h * (3 + k)) * np.cos(ph * (k + 1) + rng.random() * 6)
    n = (n - n.min()) / (n.max() - n.min())
    v = yy / (h - 1)
    img = np.zeros((h, w, 4))
    if kind == 'vortex':
        streak = 0.5 + 0.5 * np.sin(ph * 6 + v * 9 + n * 4)
        a = np.clip(0.25 + 0.75 * streak ** 2, 0, 1) * np.sin(np.pi * v) ** 0.6
        col = lerp(CY_LO, CY, streak)
        col = lerp(col, CY_HI, np.clip(streak ** 6 * 1.3, 0, 1))
    else:
        crest = 0.18 + 0.08 * np.sin(ph * 3 + n * 3)
        body = v > crest
        foam = (np.abs(v - crest) < 0.07 + 0.05 * n)
        a = np.where(body, 0.55 + 0.4 * (1 - v), 0) + foam * 0.9
        a *= np.clip((1 - v) * 6, 0, 1) * 0 + 1
        col = lerp(CY, CY_LO, np.clip(v * 1.2, 0, 1))
        stre = 0.5 + 0.5 * np.sin(ph * 8 + v * 20 + n * 6)
        col = lerp(col, CY_HI, (stre > 0.93) * 0.6)
        col[foam] = (235, 255, 255)
        a = np.clip(a, 0, 1)
    img[..., :3] = col
    img[..., 3] = a * 255
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


# ---------------- HUD icons (5 abilities + transform) ----------------
def icon(kind, S=32):
    Q = 4
    s = S * Q
    yy, xx = (np.mgrid[0:s, 0:s] + 0.5) / Q
    c = S / 2
    d = np.hypot(xx - c, yy - c)
    ang = np.arctan2(yy - c, xx - c)
    img = np.zeros((s, s, 4))
    bg = d < S / 2 - 0.5
    img[bg] = [14, 20, 38, 255]
    ring = (d > S / 2 - 2.2) & bg
    img[ring] = [120, 132, 150, 255]
    m = np.zeros((s, s))
    if kind == 0:      # tidal rend: three claw slashes
        for k in (-1, 0, 1):
            off = k * 5.5
            t = (xx - yy + off * 1.4)
            band = np.abs(t) < 1.3 - 0.9 * np.abs((xx + yy - S) / S) ** 1.5 * 1.0
            m = np.maximum(m, (band & (np.abs(xx + yy - S) < S * 0.62)).astype(float))
    elif kind == 1:    # maelstrom spiral
        r = d / (S / 2)
        spiral = np.abs(((ang + r * 9) / (2 * np.pi) * 2) % 1 - 0.5) < 0.16
        m = (spiral & (r < 0.78) & (r > 0.08)).astype(float) * np.clip(1.2 - r, 0, 1)
    elif kind == 2:    # geysers
        for x0, h in ((10, 16), (16, 22), (22, 14)):
            col = (np.abs(xx - x0) < 1.8 - (S - 5 - yy) / h * 0.9) & (yy > S - 5 - h) & (yy < S - 5)
            m = np.maximum(m, col.astype(float))
            m = np.maximum(m, (np.hypot(xx - x0, yy - (S - 7 - h)) < 1.6).astype(float))
        m = np.maximum(m, ((yy > S - 6.5) & (yy < S - 4.5) & (np.abs(xx - c) < 11)).astype(float) * 0.7)
    elif kind == 3:    # pressure beam
        orb = np.hypot(xx - 9, yy - c) < 5
        beam = (xx > 9) & (np.abs(yy - c) < 2.4 - (xx - 9) / S * 1.2) & (xx < S - 4)
        m = np.maximum(orb * 1.0, beam * 1.0)
        m = np.maximum(m, (np.abs(np.hypot(xx - 9, yy - c) - 7) < 0.7) * 0.6)
    elif kind == 4:    # tsunami curl
        r = np.hypot(xx - 17, yy - 15)
        curl = (np.abs(r - 7) < 2.4) & ~((xx < 17) & (yy > 15))
        base = (yy > 21) & (yy < 26) & (np.abs(xx - c) < 12) & ((yy - 21) > (np.sin(xx * 0.6) * 1.5))
        m = np.maximum(curl * 1.0, base * 0.85)
    else:              # transform: crown sigil
        for k in range(5):
            a = math.radians(-90 + (k - 2) * 26)
            L = 12 - abs(k - 2) * 2.5
            px, py = c + math.cos(a) * L, c + 6 + math.sin(a) * L
            t = np.clip(((xx - c) * (px - c) + (yy - c - 6) * (py - c - 6)) / (L * L), 0, 1)
            dist = np.hypot(xx - (c + t * (px - c)), yy - (c + 6 + t * (py - c - 6)))
            m = np.maximum(m, (dist < 1.6 * (1 - t) + 0.3).astype(float))
        m = np.maximum(m, (np.abs(xx - c) + np.abs(yy - c - 8) < 3).astype(float))
    m *= bg
    col = lerp(CY, CY_HI, m ** 2)
    img[..., :3] = np.where(m[..., None] > 0.05, lerp(img[..., :3], col, np.clip(m * 1.4, 0, 1)), img[..., :3])
    # soft glow
    from scipy.ndimage import gaussian_filter
    g = gaussian_filter(m, 5) * 0.8 * bg
    img[..., :3] = np.clip(img[..., :3] + g[..., None] * np.array([40, 180, 190]), 0, 255)
    img = img.reshape(S, Q, S, Q, 4).mean((1, 3))
    return img


def icons():
    sheet = np.zeros((32, 32 * 6, 4))
    for k in range(6):
        sheet[:, k * 32:(k + 1) * 32] = icon(k)
    return Image.fromarray(np.clip(sheet, 0, 255).astype(np.uint8), 'RGBA')


# ---------------- Tide Blade item sprite (32x32) ----------------
def sword():
    """shape classification in blade space (s along the blade, w across) then outlined"""
    S = 32
    Q = 1
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    G = np.array([9.5, 22.5])
    u = np.array([1, -1]) / math.sqrt(2)
    v = np.array([1, 1]) / math.sqrt(2)
    px, py = xx - G[0], yy - G[1]
    sa = px * u[0] + py * u[1]
    wa = px * v[0] + py * v[1]
    L = 27.0
    hw = np.where(sa < L - 7, 3.0, 3.0 * np.clip((L - sa) / 7, 0, 1))
    blade = (sa > 0.8) & (sa < L) & (np.abs(wa) < hw)
    wing = (np.abs(wa) < 7.3) & (sa > -1.0 + np.abs(wa) * 0.45) & (sa < 1.3 + np.abs(wa) * 0.45 - (np.abs(wa) > 5.8) * (np.abs(wa) - 5.8) * 0.9)
    hilt = (sa > -7.5) & (sa <= -0.9) & (np.abs(wa) < 1.15)
    pommel = np.hypot(sa + 8.8, wa) < 1.7
    img = np.zeros((S, S, 4))
    NAVY, NAVY_L = np.array([34, 44, 74]), np.array([62, 80, 120])
    SIL, SIL_L, SIL_D = np.array([168, 175, 186]), np.array([232, 236, 242]), np.array([100, 106, 120])
    CYC, CYH = np.array([50, 222, 230]), np.array([200, 255, 252])
    col = np.zeros((S, S, 3))
    col[blade] = NAVY
    stripe = blade & (np.abs(wa) < 0.5)
    col[stripe] = CYC
    edge = blade & (np.abs(wa) > hw - 0.8)
    col[edge] = CYC
    col[edge & (wa < 0)] = CYH
    col[blade & ((np.round(sa) % 5) == 0) & ~edge & ~stripe] = NAVY_L
    col[wing] = SIL
    col[wing & (np.abs(wa) > 5.0)] = SIL_L
    col[wing & (sa < 0.2 + np.abs(wa) * 0.45)] = SIL_D
    col[hilt] = NAVY
    col[hilt & ((np.floor(sa) % 2) == 0)] = np.array([46, 180, 196])
    col[pommel] = CYC
    col[np.hypot(sa + 9.1, wa + 0.4) < 0.8] = CYH
    gem = np.hypot(sa - 0.2, wa) < 1.1
    col[gem] = CYH
    shape = blade | wing | hilt | pommel | gem
    img[..., :3] = col
    img[..., 3] = shape * 255
    # dark outline around the silhouette
    o = np.zeros_like(shape)
    o[1:] |= shape[:-1]; o[:-1] |= shape[1:]; o[:, 1:] |= shape[:, :-1]; o[:, :-1] |= shape[:, 1:]
    o &= ~shape
    img[o] = [10, 14, 26, 255]
    for (x, y) in ((25, 12), (28, 16), (19, 4), (14, 8)):
        if img[y, x, 3] == 0:
            img[y, x] = [120, 240, 245, 200]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    icons().resize((192 * 4, 32 * 4), Image.NEAREST).save('/tmp/claude-0/icons.png')
    sword().resize((256, 256), Image.NEAREST).save('/tmp/claude-0/sword.png')
    strip(128, 64, 'vortex').save('/tmp/claude-0/vortex.png')
    strip(128, 64, 'wave').save('/tmp/claude-0/wave.png')
