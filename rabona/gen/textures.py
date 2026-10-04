"""Rabona Arena doku ureticisi: toplar, formalar, bot tenleri, bloklar, esyalar."""
import os, math, json
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/rabonaarena/textures')
R = np.random.default_rng(1905)


def out(rel):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def save(arr, rel):
    a = np.clip(arr, 0, 255).astype(np.uint8)
    Image.fromarray(a, 'RGBA' if a.shape[2] == 4 else 'RGB').save(out(rel))


def hexc(h, a=255):
    h = h.lstrip('#')
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a], dtype=float)


def lerp(a, b, t):
    return a + (b - a) * t


def smooth(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)


def value_noise(h, w, scale, octaves=4, seed=0):
    rng = np.random.default_rng(seed)
    out_ = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        s = max(1, int(scale / (2 ** o)))
        gh, gw = h // s + 2, w // s + 2
        g = rng.random((gh, gw))
        ys = np.arange(h) / s
        xs = np.arange(w) / s
        y0 = ys.astype(int); x0 = xs.astype(int)
        fy = smooth(ys - y0)[:, None]; fx = smooth(xs - x0)[None, :]
        a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
        out_ += amp * lerp(lerp(a, b, fx), lerp(c, d, fx), fy)
        tot += amp
        amp *= 0.5
    return out_ / tot


# =====================================================================  TOP
PHI = (1 + 5 ** 0.5) / 2


def ball_centers():
    pent = []
    for a in (-1, 1):
        for b in (-1, 1):
            pent += [(0, a, b * PHI), (a, b * PHI, 0), (b * PHI, 0, a)]
    hexa = []
    for a in (-1, 1):
        for b in (-1, 1):
            for c in (-1, 1):
                hexa.append((a, b, c))
            hexa += [(0, a / PHI, b * PHI), (a / PHI, b * PHI, 0), (b * PHI, 0, a / PHI)]
    P = np.array(pent, float); P /= np.linalg.norm(P, axis=1)[:, None]
    H = np.array(hexa, float); H /= np.linalg.norm(H, axis=1)[:, None]
    return P, H


def sphere_dirs(w, h):
    u = (np.arange(w) + 0.5) / w
    v = (np.arange(h) + 0.5) / h
    phi = u[None, :] * 2 * np.pi
    th = v[:, None] * np.pi
    x = np.sin(th) * np.cos(phi)
    y = np.cos(th) * np.ones_like(phi)
    z = np.sin(th) * np.sin(phi)
    return np.stack([x, y, z], -1)


def panels(d):
    P, H = ball_centers()
    C = np.concatenate([P, H])
    dots = d @ C.T
    idx = np.argsort(-dots, axis=-1)
    first = np.take_along_axis(dots, idx[..., :1], -1)[..., 0]
    second = np.take_along_axis(dots, idx[..., 1:2], -1)[..., 0]
    is_pent = idx[..., 0] < 12
    edge = first - second  # ~0 at seam
    return is_pent, edge, idx[..., 0], first


def ball_texture(style, w=512, h=256):
    d = sphere_dirs(w, h)
    pent, edge, pid, near = panels(d)
    n = value_noise(h, w, 24, 4, seed=hash(style) % 1000)
    fine = value_noise(h, w, 4, 2, seed=7)
    seam = np.clip(1 - edge / 0.012, 0, 1)
    pillow = np.clip(edge / 0.09, 0, 1) ** 0.5
    col = np.zeros((h, w, 4))
    glow = np.zeros((h, w, 4)); glow[..., 3] = 255
    if style == 'classic':
        white = lerp(hexc('#d9dde3'), hexc('#fbfbfd'), pillow[..., None])
        black = lerp(hexc('#0d0f14'), hexc('#2a2e38'), pillow[..., None])
        col = np.where(pent[..., None], black, white)
        col[..., :3] *= (0.95 + 0.07 * n[..., None] + 0.03 * fine[..., None])
        # logo seridi: ekvatora yakin ince turuncu cizgi
        band = (np.abs(d[..., 1]) < 0.03) & ~pent
        col[band] = hexc('#ff7a1a')
    elif style == 'gold':
        g1 = lerp(hexc('#7a5208'), hexc('#ffd65a'), (pillow * (0.6 + 0.4 * n))[..., None])
        spec = np.clip((d[..., 1] * 0.5 + d[..., 0] * 0.6) * 1.2, 0, 1) ** 6
        g1[..., :3] += spec[..., None] * 90
        blk = lerp(hexc('#120c04'), hexc('#3b2a0a'), pillow[..., None])
        col = np.where(pent[..., None], blk, g1)
        swirl = np.sin((d[..., 0] * 9 + d[..., 2] * 7) + n * 6)
        col[..., :3] += (swirl > 0.92)[..., None] * 60 * (~pent)[..., None]
    elif style == 'inferno':
        base = lerp(hexc('#140604'), hexc('#3a0d06'), pillow[..., None])
        flame = value_noise(h, w, 18, 5, seed=99)
        f = np.clip((flame - 0.45) * 3 + (-d[..., 1]) * 0.6, 0, 1)
        fire = lerp(hexc('#b31a00'), hexc('#ffcf33'), (f ** 1.5)[..., None])
        col = lerp(base, fire, (f * 0.85)[..., None])
        col = np.where(pent[..., None], lerp(hexc('#050202'), hexc('#2a0a04'), pillow[..., None]), col)
        g = np.clip(f ** 2 * 1.2, 0, 1)
        glow[..., 0] = 255 * g; glow[..., 1] = 120 * g ** 1.4; glow[..., 2] = 10 * g
        glow[..., 0] += seam * 255; glow[..., 1] += seam * 110
    elif style == 'neon':
        base = lerp(hexc('#05060c'), hexc('#161a2c'), pillow[..., None])
        col = base.copy()
        ring = np.clip(1 - np.abs(edge - 0.035) / 0.008, 0, 1)
        col[..., :3] += (ring * pent)[..., None] * np.array([255, 40, 200]) * 0.7
        col[..., :3] += seam[..., None] * np.array([40, 255, 255]) * 0.8
        glow[..., 0] = seam * 30 + ring * pent * 255
        glow[..., 1] = seam * 240 + ring * pent * 30
        glow[..., 2] = seam * 255 + ring * pent * 210
    elif style == 'galaxy':
        neb = value_noise(h, w, 40, 5, seed=3)
        neb2 = value_noise(h, w, 20, 4, seed=11)
        c1 = lerp(hexc('#07031a'), hexc('#3c1a7a'), (neb ** 1.5)[..., None])
        col = lerp(c1, hexc('#ff4fd8'), (np.clip(neb2 - 0.55, 0, 1) * 2)[..., None])
        stars = (R.random((h, w)) > 0.995)
        col[stars] = hexc('#ffffff')
        col[..., :3] *= (0.75 + 0.35 * pillow)[..., None]
        glow[..., :3] = stars[..., None] * 255
        glow[..., 2] += seam * 200; glow[..., 0] += seam * 120
    elif style == 'rabona':
        white = lerp(hexc('#dfe6ea'), hexc('#ffffff'), pillow[..., None])
        lon = np.arctan2(d[..., 2], d[..., 0])
        wave = np.sin(lon * 3 + d[..., 1] * 5)
        s1 = np.clip(1 - np.abs(wave - 0.6) / 0.18, 0, 1)
        s2 = np.clip(1 - np.abs(wave + 0.5) / 0.12, 0, 1)
        col = white.copy()
        col = lerp(col, hexc('#00a39b'), s1[..., None])
        col = lerp(col, hexc('#ff6a13'), s2[..., None])
        col = np.where(pent[..., None], lerp(col, hexc('#0b2a3a'), 0.85), col)
        glow[..., 0] = s2 * 160; glow[..., 1] = s2 * 70 + s1 * 120; glow[..., 2] = s1 * 110
    # dikisler
    seam_col = hexc('#1a1d24') if style in ('classic', 'rabona') else hexc('#000000')
    col = lerp(col, seam_col, (seam * 0.9)[..., None])
    col[..., 3] = 255
    glow = np.clip(glow, 0, 255); glow[..., 3] = 255
    return col, glow, d


def ball_icon(col_tex, size=32):
    """Esya ikonu: dokuyu golgeli kureye isle."""
    h, w = col_tex.shape[:2]
    img = np.zeros((size, size, 4))
    c = (size - 1) / 2
    rad = size * 0.44
    for y in range(size):
        for x in range(size):
            dx, dy = (x - c) / rad, (c - y) / rad
            r2 = dx * dx + dy * dy
            if r2 > 1:
                continue
            dz = math.sqrt(1 - r2)
            # gorus: z ekseninden, hafif egik
            v = np.array([dx, dy, dz])
            ang = 0.5
            rx = np.array([[1, 0, 0], [0, math.cos(ang), -math.sin(ang)], [0, math.sin(ang), math.cos(ang)]])
            p = rx @ v
            th = math.acos(max(-1, min(1, p[1])))
            ph = math.atan2(p[2], p[0]) % (2 * math.pi)
            px = col_tex[min(h - 1, int(th / math.pi * h)), min(w - 1, int(ph / (2 * math.pi) * w))]
            light = max(0.25, 0.35 + 0.75 * (v @ np.array([-0.45, 0.6, 0.66])))
            spec = max(0, v @ np.array([-0.35, 0.5, 0.8])) ** 30 * 120
            rgb = px[:3] * light + spec
            edge = min(1, (1 - r2) * 6)
            img[y, x, :3] = rgb * (0.7 + 0.3 * edge)
            img[y, x, 3] = 255 if r2 < 0.92 else 255 * min(1, (1 - r2) * 14)
    return img


def make_balls():
    for s in ['classic', 'gold', 'inferno', 'neon', 'galaxy', 'rabona']:
        col, glow, _ = ball_texture(s)
        save(col, f'entity/ball/{s}.png')
        save(glow, f'entity/ball/{s}_glow.png')
        save(ball_icon(col), f'item/ball_{s}.png')


# =====================================================================  SKIN YARDIMCILARI
def box_faces(ox, oy, w, h, d):
    """Minecraft kutu UV yuzleri: ad -> (x, y, genislik, yukseklik)."""
    return {
        'top': (ox + d, oy, w, d), 'bottom': (ox + d + w, oy, w, d),
        'right': (ox, oy + d, d, h), 'front': (ox + d, oy + d, w, h),
        'left': (ox + d + w, oy + d, d, h), 'back': (ox + 2 * d + w, oy + d, w, h),
    }


HEAD = (0, 0, 8, 8, 8)
HAT = (32, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)
RARM = (40, 16, 4, 12, 4)
LARM = (32, 48, 4, 12, 4)
RLEG = (0, 16, 4, 12, 4)
LLEG = (16, 48, 4, 12, 4)


def paint(img, part, fn):
    for name, (x, y, w, h) in box_faces(*part).items():
        for j in range(h):
            for i in range(w):
                c = fn(name, i, j, w, h)
                if c is not None:
                    img[y + j, x + i] = c


def shade(c, k):
    c = np.array(c, float)
    c[:3] = np.clip(c[:3] * k, 0, 255)
    return c


# =====================================================================  BOT TENLERI
SKIN_TONES = ['#f1c7a5', '#e0ac85', '#c68863', '#9a6544', '#6e452c', '#f6d2b8', '#b97a52', '#d9a07a']
HAIR = [('#1b120c', 'short'), ('#4a2b17', 'fade'), ('#d9b25b', 'long'), ('#0c0a09', 'afro'),
        ('#7a3c1d', 'buzz'), ('#2a2422', 'bun'), ('#b8b8b8', 'mohawk'), ('#18100a', 'curly')]
EYES = ['#3b6fb6', '#5a3a22', '#2e7d32', '#4e342e', '#263238', '#6d8fb3', '#3e2723', '#5d4037']


def bot_skin(i):
    img = np.zeros((64, 64, 4))
    tone = hexc(SKIN_TONES[i])
    hair_c, style = HAIR[i]
    hair = hexc(hair_c)
    eye = hexc(EYES[i])
    rng = np.random.default_rng(i + 40)

    def skin(name, x, y, w, h):
        k = 1.0 - 0.04 * rng.random()
        if name == 'bottom':
            k *= 0.8
        if name in ('right', 'left'):
            k *= 0.93
        return shade(tone, k)

    for part in (BODY, RARM, LARM, RLEG, LLEG):
        paint(img, part, skin)

    # bas
    def head(name, x, y, w, h):
        c = shade(tone, 1.0 - 0.03 * rng.random() - (0.06 if name in ('left', 'right') else 0))
        top_hair = {'short': 2, 'fade': 2, 'long': 2, 'afro': 3, 'buzz': 1, 'bun': 2, 'mohawk': 1, 'curly': 2}[style]
        if name == 'top':
            if style == 'mohawk' and not (3 <= x <= 4):
                return shade(tone, 0.85)
            return shade(hair, 0.9 + 0.2 * rng.random())
        if name == 'bottom':
            return shade(tone, 0.75)
        if name == 'back':
            hl = 8 if style in ('long', 'afro', 'curly', 'bun', 'short') else 5
            if style == 'mohawk':
                return shade(hair, 0.9) if 3 <= x <= 4 else shade(tone, 0.9)
            if y < hl:
                return shade(hair, 0.85 + 0.2 * rng.random())
            return c
        if name in ('left', 'right'):
            if y < top_hair + (3 if style in ('long', 'afro', 'curly') else 1):
                if not (style == 'mohawk'):
                    return shade(hair, 0.85 + 0.2 * rng.random())
            if style == 'long' and y < 7 and ((name == 'right' and x < 3) or (name == 'left' and x > 4)):
                return shade(hair, 0.9)
            # kulak
            if y in (4, 5) and ((name == 'right' and x == 4) or (name == 'left' and x == 3)):
                return shade(tone, 0.82)
            if style == 'fade' and y < 4:
                return shade(hair, 0.7)
            return c
        if name == 'front':
            if y < top_hair and style != 'mohawk' and style != 'buzz':
                if style == 'long' or not (y == top_hair - 1 and 2 <= x <= 5 and rng.random() < 0.5):
                    return shade(hair, 0.9 + 0.15 * rng.random())
            if style == 'buzz' and y == 0:
                return shade(hair, 0.8)
            if style == 'mohawk' and y == 0 and 3 <= x <= 4:
                return shade(hair, 0.9)
            if style == 'long' and (x == 0 or x == 7) and y < 7:
                return shade(hair, 0.85)
            # kaslar
            if y == 3 and x in (1, 2, 5, 6):
                return shade(hair, 0.7)
            # gozler
            if y == 4:
                if x in (1, 6):
                    return hexc('#f5f5f5')
                if x in (2, 5):
                    return eye
            # burun
            if y == 5 and x in (3, 4):
                return shade(tone, 0.86)
            # agiz
            if y == 6 and 2 <= x <= 5:
                return shade(hexc('#8d4b3c'), 1.0) if 3 <= x <= 4 else shade(tone, 0.8)
            # sakal (bazi botlar)
            if i in (3, 6) and y == 7 and 1 <= x <= 6:
                return shade(hair, 0.8)
            if y == 7:
                return shade(tone, 0.92)
            return c
        return c

    paint(img, HEAD, head)

    # sapka katmani: hacimli sac / bandana
    def hat(name, x, y, w, h):
        if style == 'afro':
            if name == 'top' or (name in ('left', 'right', 'back')) or (name == 'front' and y < 2):
                if name in ('left', 'right') and y > 5:
                    return None
                if name == 'back' and y > 6:
                    return None
                return shade(hair, 0.8 + 0.3 * rng.random())
        if style == 'curly' and (name == 'top' or (name != 'bottom' and y < 2)) and rng.random() < 0.75:
            return shade(hair, 0.85 + 0.3 * rng.random())
        if style == 'bun' and name == 'top' and 2 <= x <= 5 and 4 <= y <= 7:
            return shade(hair, 1.1)
        if style == 'long' and name == 'back' and y < 8:
            return shade(hair, 0.9 + 0.2 * rng.random())
        if i == 5 and name in ('front', 'left', 'right', 'back') and y == 1:  # sac bandi
            return hexc('#ffffff')
        return None

    paint(img, HAT, hat)
    return img


def make_skins():
    for i in range(8):
        save(bot_skin(i), f'entity/footballer/skin_{i}.png')


# =====================================================================  FORMALAR
KITS = {
    'red': dict(shirt='#c62828', shirt2='#8e0000', trim='#ffd54f', shorts='#fafafa', shorts_trim='#c62828',
                socks='#c62828', sock_band='#ffd54f', boots='#ffca28', boot2='#212121', pattern='pinstripe'),
    'blue': dict(shirt='#1565c0', shirt2='#fafafa', trim='#e3f2fd', shorts='#0d47a1', shorts_trim='#fafafa',
                 socks='#fafafa', sock_band='#1565c0', boots='#00e5ff', boot2='#fafafa', pattern='sash'),
    'red_gk': dict(shirt='#d4e157', shirt2='#212121', trim='#212121', shorts='#212121', shorts_trim='#d4e157',
                   socks='#d4e157', sock_band='#212121', boots='#ff1744', boot2='#212121', pattern='geo', gloves='#ffffff'),
    'blue_gk': dict(shirt='#2e7d32', shirt2='#a5d6a7', trim='#1b5e20', shorts='#1b5e20', shorts_trim='#a5d6a7',
                    socks='#2e7d32', sock_band='#a5d6a7', boots='#ff9100', boot2='#212121', pattern='geo', gloves='#ff9100'),
}


def kit_texture(k):
    img = np.zeros((64, 64, 4))
    sh, sh2, trim = hexc(k['shirt']), hexc(k['shirt2']), hexc(k['trim'])
    rng = np.random.default_rng(len(k['shirt']) * 7 + ord(k['shirt'][1]))
    fab = value_noise(64, 64, 3, 2, seed=5)

    def fabric(c, x, y, k_=1.0):
        return shade(c, k_ * (0.94 + 0.08 * fab[y % 64, x % 64]))

    def shirt_px(name, x, y, w, h, gx):
        """gx: govde boyunca yatay konum (desen icin)."""
        base = sh
        pat = k['pattern']
        if pat == 'pinstripe' and gx % 3 == 1:
            base = sh2
        if pat == 'sash':
            # sol omuzdan sag kalcaya beyaz kusak
            if name in ('front', 'back') and abs((x if name == 'front' else 7 - x) - y * 0.6 - 1) < 1.2:
                base = sh2
        if pat == 'geo':
            if ((x + y) // 2 + (x - y) // 3) % 4 == 0:
                base = lerp(sh, sh2, 0.35)
        light = 1.06 - 0.012 * y
        if name == 'bottom':
            light = 0.8
        if name in ('left', 'right'):
            light *= 0.94
        return fabric(base, gx, y, light)

    def body(name, x, y, w, h):
        if name == 'top':
            if 2 <= x <= 5 and y >= 1:
                return None if y > 2 else fabric(trim, x, y)
            return shirt_px(name, x, y, w, h, x)
        if name == 'bottom':
            return fabric(sh, x, y, 0.75)
        c = shirt_px(name, x, y, w, h, x + (8 if name == 'back' else 0))
        if name == 'front':
            if y == 0 and 2 <= x <= 5:
                return fabric(trim, x, y)  # yaka
            if y == 1 and x in (3, 4):
                return fabric(trim, x, y, 0.9)  # V yaka
            if y == 2 and x == 5:  # arma
                return hexc('#ffd54f')
            if y == 3 and x == 5:
                return shade(hexc('#ffd54f'), 0.8)
            if y == 3 and x == 2:  # marka
                return fabric(trim, x, y)
            if y == 11:
                return fabric(sh, x, y, 0.85)
        if name == 'back' and y == 0:
            return fabric(trim, x, y)
        if y >= 11:
            return fabric(sh, x, y, 0.85)
        return c

    paint(img, BODY, body)

    def sleeve(name, x, y, w, h):
        gk = 'gloves' in k
        if gk and y >= 9:
            g = hexc(k['gloves'])
            return fabric(g, x, y, 1.0 if y < 11 else 0.85)
        if name == 'top':
            return shirt_px(name, x, y, w, h, x)
        if name == 'bottom':
            return fabric(hexc(k['gloves']), x, y, 0.8) if gk else None
        long_ = 12 if gk else 5
        if y < long_ - 1:
            return shirt_px(name, x, y, w, h, x)
        if y == long_ - 1:
            return fabric(trim, x, y)
        return None

    paint(img, RARM, sleeve)
    paint(img, LARM, sleeve)

    shorts, strim = hexc(k['shorts']), hexc(k['shorts_trim'])
    socks, band = hexc(k['socks']), hexc(k['sock_band'])
    boot, boot2 = hexc(k['boots']), hexc(k['boot2'])

    def leg(name, x, y, w, h):
        if name == 'top':
            return fabric(shorts, x, y)
        if name == 'bottom':
            return shade(boot2, 0.6) if (x + y) % 2 else shade(boot2, 0.45)  # krampon tabani
        if y <= 4:
            if y == 4:
                return fabric(strim, x, y)
            if name == 'left' and x == 1:
                return fabric(strim, x, y)
            if name == 'right' and x == 2:
                return fabric(strim, x, y)
            return fabric(shorts, x, y, 1.0 - 0.02 * y)
        if y in (5, 6):
            return None  # diz
        if y == 7:
            return fabric(band, x, y)
        if y <= 9:
            return fabric(socks, x, y, 0.95)
        # krampon
        if y == 10:
            return shade(boot, 1.05) if not (name == 'front' and x == 1) else shade(boot2, 1.0)
        return shade(boot2, 0.9) if name != 'front' else shade(boot, 0.85)

    paint(img, RLEG, leg)
    paint(img, LLEG, leg)
    return img


def digits():
    """Sirt numaralari: 10 hucre x 16x32, beyaz rakam + koyu kontur."""
    F = {
        '0': ["01110", "11011", "11011", "11011", "11011", "11011", "11011", "11011", "01110"],
        '1': ["00110", "01110", "11110", "00110", "00110", "00110", "00110", "00110", "11111"],
        '2': ["01110", "11011", "00011", "00110", "01100", "11000", "11000", "11000", "11111"],
        '3': ["11110", "00011", "00011", "01110", "00011", "00011", "00011", "00011", "11110"],
        '4': ["00011", "00111", "01111", "11011", "11011", "11111", "00011", "00011", "00011"],
        '5': ["11111", "11000", "11000", "11110", "00011", "00011", "00011", "11011", "01110"],
        '6': ["01110", "11000", "11000", "11110", "11011", "11011", "11011", "11011", "01110"],
        '7': ["11111", "00011", "00011", "00110", "00110", "01100", "01100", "01100", "01100"],
        '8': ["01110", "11011", "11011", "01110", "11011", "11011", "11011", "11011", "01110"],
        '9': ["01110", "11011", "11011", "11011", "01111", "00011", "00011", "00011", "01110"],
    }
    img = np.zeros((32, 160, 4))
    for d in range(10):
        g = F[str(d)]
        mask = np.zeros((32, 16), bool)
        for r, row in enumerate(g):
            for c, ch in enumerate(row):
                if ch == '1':
                    mask[2 + r * 3:2 + r * 3 + 3, 1 + c * 3:1 + c * 3 + 3] = True
        outline = np.zeros_like(mask)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                outline |= np.roll(np.roll(mask, dy, 0), dx, 1)
        cell = np.zeros((32, 16, 4))
        cell[outline] = hexc('#1a1a1a')
        ys = np.arange(32)[:, None] * np.ones((1, 16))
        white = np.stack([255 - ys * 1.2, 255 - ys * 1.0, 255 - ys * 0.6, np.full_like(ys, 255)], -1)
        cell[mask] = white[mask]
        img[:, d * 16:(d + 1) * 16] = cell
    return img


def make_kits():
    for name, k in KITS.items():
        save(kit_texture(k), f'entity/kit/{name}.png')
    save(digits(), 'entity/kit/digits.png')
    # taraftar dokulari: ten + forma tek dokuda (forma alfa > 0 olan pikseller tene islenir)
    for team in ('red', 'blue'):
        kit = kit_texture(KITS[team])
        for i in range(8):
            skin = bot_skin(i)
            m = kit[..., 3] > 0
            skin[m] = kit[m]
            save(skin, f'entity/fan/{team}_{i}.png')


# =====================================================================  BLOKLAR
def grass_tile(seed, dark=False, size=16):
    n = value_noise(size, size, 2, 2, seed=seed)
    rng = np.random.default_rng(seed)
    base = hexc('#3f8f2a') if not dark else hexc('#33781f')
    hi = hexc('#62b545') if not dark else hexc('#4f9a35')
    img = np.zeros((size, size, 4))
    for y in range(size):
        for x in range(size):
            t = n[y, x] * 0.7 + 0.3 * rng.random()
            c = lerp(base, hi, t)
            # cim yapraklari: dikey kisa cizgiler
            if rng.random() < 0.18:
                c = shade(c, 1.15)
            if rng.random() < 0.08:
                c = shade(c, 0.82)
            img[y, x] = c
    img[..., 3] = 255
    return img


def grass_side(top):
    size = top.shape[0]
    img = np.zeros((size, size, 4))
    rng = np.random.default_rng(3)
    for y in range(size):
        for x in range(size):
            edge = 3 + (1 if rng.random() < 0.4 else 0)
            if y < edge:
                img[y, x] = top[y, x]
            else:
                img[y, x] = shade(hexc('#7a5536'), 0.85 + 0.25 * rng.random())
    return img


def line_tile():
    g = grass_tile(77)
    rng = np.random.default_rng(78)
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.9:
                g[y, x] = shade(hexc('#f4f6f2'), 0.92 + 0.08 * rng.random())
    return g


def make_blocks():
    g1 = grass_tile(10); g2 = grass_tile(11, dark=True)
    save(g1, 'block/pitch_grass.png'); save(g2, 'block/pitch_grass_dark.png')
    save(grass_side(g1), 'block/pitch_grass_side.png'); save(grass_side(g2), 'block/pitch_grass_dark_side.png')
    save(line_tile(), 'block/pitch_line.png')
    # direk: beyaz parlak boya
    post = np.zeros((16, 16, 4))
    for y in range(16):
        for x in range(16):
            k = 0.86 + 0.14 * math.cos((x - 6) / 16 * math.pi) + 0.02 * R.random()
            post[y, x] = shade(hexc('#f7f7f7'), k)
    save(post, 'block/goal_post.png')
    # file: elmas ag deseni
    net = np.zeros((16, 16, 4))
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                net[y, x] = shade(hexc('#f2f2f2'), 0.9 + 0.1 * R.random())
    save(net, 'block/goal_net.png')
    # korner bayragi
    flag = np.zeros((16, 16, 4))
    for y in range(16):
        for x in range(16):
            if x < 2:
                flag[y, x] = hexc('#ffeb3b') if (y // 2) % 2 == 0 else hexc('#212121')
            elif 2 <= x < 14 and y < 9:
                c = hexc('#e53935') if ((x - 2) // 3 + y // 3) % 2 == 0 else hexc('#ffeb3b')
                flag[y, x] = shade(c, 0.9 + 0.1 * math.sin(x * 0.8))
    save(flag, 'block/corner_flag.png')
    # koltuklar
    for name, col in [('red', '#d32f2f'), ('blue', '#1976d2'), ('white', '#eceff1'), ('gold', '#ffb300')]:
        s = np.zeros((16, 16, 4))
        for y in range(16):
            for x in range(16):
                k = 0.85 + 0.2 * (1 - y / 16) + 0.03 * R.random()
                if x in (0, 15) or y in (0, 15):
                    k *= 0.8
                s[y, x] = shade(hexc(col), k)
        s[2:4, 3:13] = shade(hexc(col), 1.18)
        save(s, f'block/seat_{name}.png')
    # beton + basamak
    n = value_noise(16, 16, 3, 3, seed=21)
    con = np.zeros((16, 16, 4))
    con[..., :3] = (np.array([150, 152, 156]) * (0.85 + 0.25 * n)[..., None])
    con[..., 3] = 255
    save(con, 'block/stadium_concrete.png')
    st = con.copy(); st[..., :3] *= 0.7
    st[0:2] = hexc('#e0e0e0'); st[2] = hexc('#9e9e9e')
    save(st, 'block/stadium_step.png')
    # projektor
    fl = np.zeros((16, 16, 4))
    fl[...] = hexc('#37474f')
    for gy in range(3):
        for gx in range(3):
            cx, cy = 2 + gx * 5 + 2, 2 + gy * 5 + 2
            for y in range(16):
                for x in range(16):
                    d = math.hypot(x - cx + 0.5, y - cy + 0.5)
                    if d < 2.3:
                        fl[y, x] = lerp(hexc('#fffde7'), hexc('#fff59d'), min(1, d / 2.3))
    save(fl, 'block/floodlight.png')
    metal = np.zeros((16, 16, 4)); metal[...] = hexc('#455a64')
    for y in range(16):
        metal[y, :, :3] *= 0.9 + 0.1 * ((y % 4) == 0)
    save(metal, 'block/floodlight_back.png')
    led_board()


GLYPH = {
    'R': ["1110", "1001", "1110", "1010", "1001"], 'A': ["0110", "1001", "1111", "1001", "1001"],
    'B': ["1110", "1001", "1110", "1001", "1110"], 'O': ["0110", "1001", "1001", "1001", "0110"],
    'N': ["1001", "1101", "1011", "1001", "1001"], 'G': ["0111", "1000", "1011", "1001", "0111"],
    'L': ["1000", "1000", "1000", "1000", "1111"], '!': ["0100", "0100", "0100", "0000", "0100"],
}


def led_board():
    frames = []
    W = 16
    # 1) kayan ok desenleri
    for f in range(8):
        im = np.zeros((16, W, 4)); im[...] = hexc('#060a12')
        for y in range(16):
            for x in range(W):
                if 3 <= y <= 12:
                    k = (x - abs(y - 7.5) + f * 2) % 8
                    if k < 2:
                        hue = ((f * 0.05 + y * 0.01) % 1)
                        c = np.array(Image.new('HSV', (1, 1), (int(hue * 255), 220, 255)).convert('RGB').getpixel((0, 0)) + (255,), float)
                        im[y, x] = c
        frames.append(im)
    # 2) harfler
    for ch, col in zip("RABONA", ['#ff5252', '#ffd740', '#69f0ae', '#40c4ff', '#e040fb', '#ff6e40']):
        for _ in range(2):
            im = np.zeros((16, W, 4)); im[...] = hexc('#060a12')
            g = GLYPH[ch]
            for r, row in enumerate(g):
                for c, v in enumerate(row):
                    if v == '1':
                        im[3 + r * 2:5 + r * 2, 4 + c * 2:6 + c * 2] = hexc(col)
            frames.append(im)
    # 3) GOL! yanip sonen
    for ch in "GOL!":
        for k in range(2):
            im = np.zeros((16, W, 4)); im[...] = hexc('#060a12') if k else hexc('#1b0000')
            for r, row in enumerate(GLYPH[ch]):
                for c, v in enumerate(row):
                    if v == '1':
                        im[3 + r * 2:5 + r * 2, 4 + c * 2:6 + c * 2] = hexc('#ffea00') if k == 0 else hexc('#ff1744')
            frames.append(im)
    # 4) takim renkleri dalgasi
    for f in range(8):
        im = np.zeros((16, W, 4))
        for y in range(16):
            for x in range(W):
                t = (math.sin((x + f * 2) * 0.4 + y * 0.2) + 1) / 2
                im[y, x] = lerp(hexc('#c62828'), hexc('#1565c0'), t)
        frames.append(im)
    # LED piksel izgarasi efekti
    for im in frames:
        for y in range(16):
            for x in range(W):
                if x % 2 == 1 or y % 2 == 1:
                    im[y, x, :3] *= 0.72
    sheet = np.concatenate(frames, 0)
    save(sheet, 'block/led_board.png')
    with open(out('block/led_board.png.mcmeta'), 'w') as fp:
        json.dump({"animation": {"frametime": 4, "interpolate": False}}, fp)


# =====================================================================  ESYALAR
def make_items():
    S = 32
    # stadyum kurucu: mavi plan kagidi + saha cizimi
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([3, 5, 28, 26], 3, fill=(21, 101, 192, 255), outline=(10, 40, 90, 255))
    d.rectangle([6, 8, 25, 23], outline=(220, 240, 255, 255))
    d.line([15, 8, 15, 23], fill=(220, 240, 255, 255))
    d.ellipse([12, 13, 18, 18], outline=(220, 240, 255, 255))
    d.rectangle([6, 12, 9, 19], outline=(220, 240, 255, 255))
    d.rectangle([22, 12, 25, 19], outline=(220, 240, 255, 255))
    d.rectangle([1, 3, 5, 28], fill=(240, 230, 200, 255), outline=(150, 120, 80, 255))
    d.ellipse([20, 20, 30, 30], fill=(255, 255, 255, 255), outline=(30, 30, 30, 255))
    d.polygon([(25, 22), (27, 24), (26, 27), (24, 27), (23, 24)], fill=(30, 30, 30, 255))
    im.save(out('item/stadium_builder.png'))
    # duduk
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line([4, 4, 10, 16], fill=(200, 30, 30, 255), width=2)
    d.line([4, 4, 22, 6], fill=(200, 30, 30, 255), width=2)
    d.ellipse([10, 13, 24, 27], fill=(205, 210, 216, 255), outline=(90, 95, 100, 255))
    d.rectangle([18, 12, 29, 18], fill=(215, 220, 226, 255), outline=(90, 95, 100, 255))
    d.ellipse([13, 16, 18, 21], fill=(250, 250, 250, 255))
    d.ellipse([15, 19, 21, 25], fill=(60, 60, 70, 255))
    im.save(out('item/whistle.png'))
    # bot kartlari
    for team, col, col2 in [('red', (198, 40, 40), (255, 213, 79)), ('blue', (21, 101, 192), (227, 242, 253))]:
        im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        d.rounded_rectangle([6, 2, 26, 30], 3, fill=col2 + (255,), outline=(40, 40, 40, 255))
        d.rounded_rectangle([8, 4, 24, 28], 2, fill=col + (255,))
        d.ellipse([12, 7, 20, 15], fill=(240, 200, 160, 255))
        d.rectangle([11, 16, 21, 24], fill=col2 + (255,))
        d.rectangle([9, 25, 23, 27], fill=(30, 30, 30, 255))
        d.text((10, 24), "", fill=(255, 255, 255, 255))
        im.save(out(f'item/bot_card_{team}.png'))
    # altin krampon
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon([(4, 14), (14, 12), (18, 18), (28, 20), (29, 25), (4, 25)], fill=(255, 202, 40, 255), outline=(120, 80, 0, 255))
    d.line([6, 17, 26, 22], fill=(255, 241, 150, 255))
    d.line([10, 15, 12, 20], fill=(30, 30, 30, 255))
    d.line([13, 15, 15, 20], fill=(30, 30, 30, 255))
    for x in (6, 11, 17, 23, 27):
        d.rectangle([x, 26, x + 1, 28], fill=(60, 60, 60, 255))
    d.rectangle([4, 25, 29, 26], fill=(40, 40, 40, 255))
    im.save(out('item/golden_cleats.png'))
    # zirh katmani (botlar)
    layer = np.zeros((32, 64, 4))
    gold = hexc('#ffca28')

    def boots(name, x, y, w, h):
        if y < 8:
            return None
        if y == 8:
            return hexc('#212121')
        return shade(gold, 1.1 - 0.05 * (y - 8)) if not (name == 'front' and y == 10) else hexc('#fff59d')

    for name, (bx, by, w, h) in box_faces(0, 16, 4, 12, 4).items():
        for j in range(h):
            for i in range(w):
                c = boots(name, i, j, w, h)
                if c is not None:
                    layer[by + j, bx + i] = c
    save(layer, 'models/armor/golden_cleats_layer_1.png')
    save(np.zeros((32, 64, 4)), 'models/armor/golden_cleats_layer_2.png')


if __name__ == '__main__':
    make_balls()
    make_skins()
    make_kits()
    make_blocks()
    make_items()
    print('ok')
