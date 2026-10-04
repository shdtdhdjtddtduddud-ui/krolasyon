"""Procedural texture painter for Krolasyon Futbol: ball, kits, bot skins, shirt numbers, pitch blocks and items."""
import os, math, numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolasyonfutbol/textures')
R = np.random.default_rng(1907)


def save(img, rel):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    if isinstance(img, np.ndarray):
        img = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')
    img.save(p)


def rgba(h, a=255):
    h = h.lstrip('#')
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a], float)


def canvas(w, h):
    return np.zeros((h, w, 4), float)


def shade(c, k):
    c = np.array(c, float).copy()
    c[:3] = c[:3] * k
    return c


def mix(a, b, t):
    return np.array(a, float) * (1 - t) + np.array(b, float) * t


# ----------------------------------------------------------------------------------------------- ball

def ball():
    W, H = 512, 256
    phi = (1 + 5 ** 0.5) / 2
    ico = []
    for a in (-1, 1):
        for b in (-1, 1):
            ico += [(0, a, b * phi), (a, b * phi, 0), (a * phi, 0, b)]
    dod = []
    for a in (-1, 1):
        for b in (-1, 1):
            for c in (-1, 1):
                dod.append((a, b, c))
            dod += [(0, a / phi, b * phi), (a / phi, b * phi, 0), (a * phi, 0, b / phi)]
    C = np.array(ico + dod, float)
    C /= np.linalg.norm(C, axis=1, keepdims=True)
    # tilt so a pentagon is not exactly on the pole (looks nicer when rolling)
    ang = 0.3
    rot = np.array([[1, 0, 0], [0, math.cos(ang), -math.sin(ang)], [0, math.sin(ang), math.cos(ang)]])
    C = C @ rot.T
    v, u = np.mgrid[0:H, 0:W]
    th = (v + 0.5) / H * math.pi
    ph = (u + 0.5) / W * 2 * math.pi
    D = np.stack([np.sin(th) * np.cos(ph), np.cos(th), np.sin(th) * np.sin(ph)], -1)
    dots = D @ C.T
    order = np.argsort(-dots, axis=-1)
    best = np.take_along_axis(dots, order[..., :1], -1)[..., 0]
    second = np.take_along_axis(dots, order[..., 1:2], -1)[..., 0]
    idx = order[..., 0]
    pent = idx < 12
    edge = np.arccos(np.clip(second, -1, 1)) - np.arccos(np.clip(best, -1, 1))
    img = canvas(W, H)
    white = np.array([246, 246, 242, 255], float)
    black = np.array([22, 24, 30, 255], float)
    base = np.where(pent[..., None], black, white)
    # puffy panels: darker towards the seams
    puff = np.clip(edge / 0.09, 0, 1) ** 0.5
    k = 0.80 + 0.20 * puff
    noise = R.normal(0, 1, (H, W)) * 0.018
    col = base.copy()
    col[..., :3] = base[..., :3] * (k + noise)[..., None]
    # pentagon glossy center highlight + thin gold ring for a premium look
    ring = pent & (edge > 0.035) & (edge < 0.05)
    col[ring] = [212, 172, 60, 255]
    # seams
    seam = edge < 0.022
    col[seam] = [92, 92, 98, 255]
    stitch = (edge >= 0.022) & (edge < 0.034) & (((u + v) % 6) < 2) & ~pent
    col[stitch] = shade(white, 0.72)
    col[..., 3] = 255
    save(col, 'entity/football.png')

    # item icon
    S = 16
    ic = canvas(S, S)
    for y in range(S):
        for x in range(S):
            dx, dy = (x + 0.5 - 8) / 7.2, (y + 0.5 - 8) / 7.2
            r2 = dx * dx + dy * dy
            if r2 > 1:
                continue
            dz = math.sqrt(1 - r2)
            d = np.array([dx, -dy, dz]) @ rot.T
            dd = C @ d
            o = np.argsort(-dd)
            p = o[0] < 12
            e = math.acos(min(1, dd[o[1]])) - math.acos(min(1, dd[o[0]]))
            c = black.copy() if p else white.copy()
            if e < 0.06 and not p:
                c = shade(white, 0.7)
            light = 0.55 + 0.45 * max(0, dx * -0.5 + -dy * 0.5 + dz * 0.7)
            c[:3] *= light
            ic[y, x] = c
    ic[2:4, 4:6, :3] = np.maximum(ic[2:4, 4:6, :3], 255)
    outline(ic, [16, 18, 24, 255])
    save(ic, 'item/football.png')


def outline(img, color):
    a = img[..., 3] > 0
    h, w = a.shape
    out = np.zeros_like(a)
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        s = np.zeros_like(a)
        ys = slice(max(0, dy), h + min(0, dy))
        yd = slice(max(0, -dy), h + min(0, -dy))
        xs = slice(max(0, dx), w + min(0, dx))
        xd = slice(max(0, -dx), w + min(0, -dx))
        s[yd, xd] = a[ys, xs]
        out |= s
    out &= ~a
    img[out] = color


def aura():
    W, H = 64, 32
    img = canvas(W, H)
    v, u = np.mgrid[0:H, 0:W]
    a = 0.5 + 0.5 * np.sin(u / W * 2 * math.pi * 3 + v / H * 2 * math.pi * 1.5)
    b = 0.5 + 0.5 * np.sin(u / W * 2 * math.pi * 5 - v / H * 2 * math.pi * 2.0 + 1.3)
    n = R.random((H, W))
    val = np.clip(a * b * 1.4 + n * 0.25 - 0.25, 0, 1) ** 1.5
    img[..., :3] = 255
    img[..., 3] = val * 255
    save(img, 'entity/ball_aura.png')


# ----------------------------------------------------------------------------------------------- skins

class Skin:
    """64x64 skin painter addressing box faces by name."""

    def __init__(self):
        self.img = canvas(64, 64)

    @staticmethod
    def boxes(slim=False):
        aw = 3 if slim else 4
        return {
            'head': (0, 0, 8, 8, 8), 'hat': (32, 0, 8, 8, 8),
            'body': (16, 16, 8, 12, 4), 'jacket': (16, 32, 8, 12, 4),
            'rarm': (40, 16, aw, 12, 4), 'rsleeve': (40, 32, aw, 12, 4),
            'larm': (32, 48, aw, 12, 4), 'lsleeve': (48, 48, aw, 12, 4),
            'rleg': (0, 16, 4, 12, 4), 'rpants': (0, 32, 4, 12, 4),
            'lleg': (16, 48, 4, 12, 4), 'lpants': (0, 48, 4, 12, 4),
        }

    @staticmethod
    def faces(u, v, w, h, d):
        return {
            'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
            'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
        }

    def face(self, box, f):
        u, v, w, h, d = self.boxes()[box]
        return self.faces(u, v, w, h, d)[f]

    def paint(self, box, fn, faces=('top', 'bottom', 'right', 'front', 'left', 'back')):
        u, v, w, h, d = self.boxes()[box]
        fs = self.faces(u, v, w, h, d)
        for name in faces:
            fx, fy, fw, fh = fs[name]
            for y in range(fh):
                for x in range(fw):
                    c = fn(name, x, y, fw, fh)
                    if c is not None:
                        self.img[fy + y, fx + x] = c


def fabric(c, x, y, name, strength=0.06):
    k = 1 + R.normal(0, strength * 0.5)
    if name == 'top':
        k *= 1.06
    elif name == 'bottom':
        k *= 0.75
    elif name in ('left', 'right'):
        k *= 0.9
    return shade(c, k)


KITS = {
    'red': dict(shirt='#C62828', shirt2='#9E1B1B', trim='#FFD54F', collar='#FFFFFF', shorts='#F5F5F5', shorts2='#C62828',
                socks='#C62828', socks2='#FFFFFF', boot='#1B1B1F', boot2='#FFC107', pattern='stripes'),
    'blue': dict(shirt='#1565C0', shirt2='#0D47A1', trim='#FFFFFF', collar='#0A2A5E', shorts='#0A2A5E', shorts2='#42A5F5',
                 socks='#1565C0', socks2='#FFFFFF', boot='#F2F2F2', boot2='#1E88E5', pattern='sash'),
    'red_gk': dict(shirt='#64DD17', shirt2='#33691E', trim='#111111', collar='#111111', shorts='#1A1A1A', shorts2='#64DD17',
                   socks='#1A1A1A', socks2='#64DD17', boot='#111111', boot2='#76FF03', pattern='hoops', gloves='#F1F8E9', gloves2='#64DD17'),
    'blue_gk': dict(shirt='#FF9800', shirt2='#E65100', trim='#111111', collar='#111111', shorts='#1A1A1A', shorts2='#FF9800',
                    socks='#1A1A1A', socks2='#FF9800', boot='#111111', boot2='#FFAB40', pattern='hoops', gloves='#FFF3E0', gloves2='#FF6D00'),
}


def kit(name, k):
    s = Skin()
    shirt, shirt2, trim, collar = rgba(k['shirt']), rgba(k['shirt2']), rgba(k['trim']), rgba(k['collar'])
    keeper = 'gloves' in k

    def body(face, x, y, w, h):
        c = shirt
        if k['pattern'] == 'stripes' and face in ('front', 'back') and x % 3 == 1:
            c = shirt2
        if k['pattern'] == 'sash' and face == 'front' and (x + y) in (7, 8, 9):
            c = rgba('#FFFFFF')
        if k['pattern'] == 'sash' and face == 'back' and (w - 1 - x + y) in (7, 8, 9):
            c = rgba('#E3F2FD')
        if k['pattern'] == 'hoops' and face != 'top' and (y // 2) % 2 == 1:
            c = shirt2
        if face == 'top':
            c = shade(shirt, 1.0)
            if 2 <= x <= 5:
                c = collar
        if face == 'front':
            if y == 0 and 1 <= x <= 6:
                c = collar
            if y == 1 and 3 <= x <= 4:
                c = collar
            if y == 2 and x in (3, 4):
                c = shade(collar, 0.85)
            if 2 <= y <= 3 and x == 5:
                c = rgba('#FFD54F') if name == 'red' else rgba('#FFFFFF')  # crest
            if 2 <= y <= 3 and x == 6:
                c = rgba('#FFF8E1') if name == 'red' else rgba('#BBDEFB')
            if y == 6 and 1 <= x <= 6 and not keeper:
                c = shade(c, 1.25)  # sponsor band
            if y == 11:
                c = shade(c, 0.78)
        if face == 'back':
            if y == 0 and 1 <= x <= 6:
                c = collar
            if y == 11:
                c = shade(c, 0.78)
        if face in ('left', 'right') and y == 11:
            c = shade(c, 0.78)
        if face == 'bottom':
            c = shade(shirt, 0.6)
        out = fabric(c, x, y, face)
        if face in ('front', 'back', 'left', 'right'):
            out = shade(out, 1.05 - 0.12 * (y / max(1, h - 1)))
        return out

    s.paint('body', body)

    def arm(face, x, y, w, h):
        sleeve = 10 if keeper else 4
        if face == 'top':
            return fabric(shirt, x, y, face)
        if face == 'bottom':
            if keeper:
                return fabric(rgba(k['gloves']), x, y, face)
            return None
        if y < sleeve:
            c = shirt
            if k['pattern'] == 'hoops' and (y // 2) % 2 == 1:
                c = shirt2
            if y == sleeve - 1:
                c = trim
            return fabric(c, x, y, face)
        if keeper and y >= 10:
            c = rgba(k['gloves']) if y == 11 or x % 2 == 0 else rgba(k['gloves2'])
            return fabric(c, x, y, face)
        return None

    s.paint('rarm', arm)
    s.paint('larm', arm)

    def leg(face, x, y, w, h):
        shorts, shorts2 = rgba(k['shorts']), rgba(k['shorts2'])
        socks, socks2 = rgba(k['socks']), rgba(k['socks2'])
        boot, boot2 = rgba(k['boot']), rgba(k['boot2'])
        if face == 'top':
            return fabric(shorts, x, y, face)
        if face == 'bottom':
            return shade(boot, 0.5) if (x + y) % 2 else shade(boot2, 0.7)
        if y <= 4:
            c = shorts
            if face in ('left', 'right') and x == 1:
                c = shorts2
            if y == 4:
                c = shade(c, 0.85)
            return fabric(c, x, y, face)
        if y == 5:
            return None
        if y <= 9:
            c = socks
            if y == 6:
                c = socks2
            if y == 7 and k['pattern'] == 'stripes':
                c = socks2
            return fabric(c, x, y, face, 0.04)
        c = boot
        if y == 10 and face in ('left', 'right'):
            c = boot2
        if y == 11 and face == 'front':
            c = shade(boot, 1.3)
        if y == 11:
            c = mix(c, boot2, 0.25)
        return fabric(c, x, y, face, 0.03)

    s.paint('rleg', leg)
    s.paint('lleg', leg)
    save(s.img, f'entity/jersey_{name}.png')


SKIN_TONES = ['#F2CBA7', '#E0AC83', '#C68A5E', '#A0663F', '#7A4A2B', '#5A3520', '#EAC09A', '#B57A50']
HAIR = [('#1A1410', 'short'), ('#4A2E1A', 'buzz'), ('#D9B25A', 'quiff'), ('#141010', 'curly'), ('#2A1A10', 'bald'),
        ('#3B2414', 'long'), ('#A3441C', 'short'), ('#0E0B09', 'afro')]
EYES = ['#3E2723', '#1B5E20', '#0D47A1', '#4E342E', '#212121', '#33691E', '#5D4037', '#1565C0']


def bot_skin(i):
    s = Skin()
    skin = rgba(SKIN_TONES[i])
    hair, style = HAIR[i]
    hair = rgba(hair)
    eye = rgba(EYES[i])

    def skinfn(face, x, y, w, h):
        c = skin
        if face == 'bottom':
            c = shade(skin, 0.8)
        if face in ('left', 'right'):
            c = shade(skin, 0.92)
        return shade(c, 1 + R.normal(0, 0.02))

    for b in ('body', 'rarm', 'larm', 'rleg', 'lleg'):
        s.paint(b, skinfn)

    def head(face, x, y, w, h):
        c = shade(skin, 1 + R.normal(0, 0.015))
        if face in ('left', 'right'):
            c = shade(c, 0.93)
        if face == 'bottom':
            c = shade(skin, 0.8)
        top_rows = {'short': 2, 'buzz': 1, 'quiff': 2, 'curly': 3, 'bald': 0, 'long': 2, 'afro': 3}[style]
        if face == 'top' and style != 'bald':
            c = shade(hair, 1 + R.normal(0, 0.08))
        if face == 'top' and style == 'bald':
            c = shade(skin, 1.08)
        if face in ('front', 'left', 'right', 'back') and y < top_rows:
            c = shade(hair, 1 + R.normal(0, 0.08))
        if face == 'back' and style in ('long', 'afro', 'curly') and y < 6:
            c = shade(hair, 1 + R.normal(0, 0.08))
        if face == 'back' and style in ('short', 'quiff', 'buzz') and y < 4:
            c = shade(hair, 1 + R.normal(0, 0.08))
        if face in ('left', 'right') and style != 'bald' and y < 4 and (x < 3 if face == 'right' else x > 4):
            c = shade(hair, 1 + R.normal(0, 0.08))
        if face == 'front':
            # brows
            if y == 3 and x in (1, 2, 5, 6) and style != 'bald':
                c = shade(hair, 0.9)
            if y == 3 and x in (1, 2, 5, 6) and style == 'bald':
                c = rgba('#2A1A10')
            # eyes
            if y == 4 and x in (1, 6):
                c = rgba('#F5F5F5')
            if y == 4 and x in (2, 5):
                c = eye
            # nose shading
            if y == 5 and x in (3, 4):
                c = shade(skin, 0.86)
            # mouth
            if y == 6 and 2 <= x <= 5:
                c = rgba('#8D4B3A') if x in (3, 4) else shade(skin, 0.8)
            # beard for some
            if style == 'bald' and y >= 5 and (x <= 1 or x >= 6 or y == 7):
                c = shade(rgba('#2A1A10'), 1 + R.normal(0, 0.1))
            if style == 'bald' and y == 6 and 2 <= x <= 5 and x not in (3, 4):
                c = shade(rgba('#2A1A10'), 1.1)
            if i in (5, 3) and y == 7 and 1 <= x <= 6:
                c = shade(hair, 1.0)
        return c

    s.paint('head', head)

    def hat(face, x, y, w, h):
        if style == 'bald' or style == 'buzz':
            return None
        hc = shade(hair, 1 + R.normal(0, 0.1))
        if face == 'top':
            return hc if style != 'short' or (x + y) % 3 else None
        if face == 'bottom':
            return None
        if style == 'quiff' and face == 'front' and y < 2:
            return shade(hair, 1.15)
        if style == 'afro' and face in ('front', 'left', 'right', 'back') and y < (2 if face == 'front' else 6):
            return hc
        if style == 'curly' and y < 2 and (x + y) % 2 == 0:
            return hc
        if style == 'long' and face == 'back' and y < 7:
            return hc
        if style == 'long' and face in ('left', 'right') and y < 5 and (x < 2 if face == 'right' else x > 5):
            return hc
        return None

    s.paint('hat', hat)
    save(s.img, f'entity/footballer_{i}.png')


# ----------------------------------------------------------------------------------------------- shirt numbers

SEG = {
    '0': 'abcdef', '1': 'bc', '2': 'abged', '3': 'abgcd', '4': 'fgbc', '5': 'afgcd', '6': 'afgedc', '7': 'abc', '8': 'abcdefg', '9': 'abcfgd'
}


def numbers():
    W, H = 256, 32
    img = canvas(W, H)
    for d in range(10):
        g = np.zeros((24, 16), bool)
        t = 3
        x0, x1, y0, ym, y1 = 2, 13, 1, 11, 22
        segs = SEG[str(d)]
        if 'a' in segs: g[y0:y0 + t, x0:x1 + 1] = True
        if 'g' in segs: g[ym:ym + t, x0:x1 + 1] = True
        if 'd' in segs: g[y1 - t + 1:y1 + 1, x0:x1 + 1] = True
        if 'f' in segs: g[y0:ym + t, x0:x0 + t] = True
        if 'b' in segs: g[y0:ym + t, x1 - t + 1:x1 + 1] = True
        if 'e' in segs: g[ym:y1 + 1, x0:x0 + t] = True
        if 'c' in segs: g[ym:y1 + 1, x1 - t + 1:x1 + 1] = True
        if d == 1:
            g[:, :] = False
            g[y0:y1 + 1, 7:10] = True
            g[y0:y0 + 3, 5:8] = True
        cell = canvas(16, 24)
        cell[g] = [255, 255, 255, 255]
        # subtle top highlight to bottom shade
        for yy in range(24):
            cell[yy, :, :3] *= np.where(g[yy], 1.0 - 0.15 * yy / 23, 1)[:, None]
        outline(cell, [20, 20, 28, 255])
        img[0:24, d * 16:(d + 1) * 16] = cell
    save(img, 'entity/numbers.png')


# ----------------------------------------------------------------------------------------------- blocks

def grass(base, name, stripe_dir=0):
    img = canvas(16, 16)
    b = rgba(base)
    for y in range(16):
        for x in range(16):
            n = R.normal(0, 0.06)
            blade = 0.06 * math.sin((x * 1.7 + y * 0.4) + R.normal(0, 0.6))
            c = shade(b, 1 + n + blade)
            if R.random() < 0.05:
                c = shade(b, 1.18)
            if R.random() < 0.03:
                c = shade(b, 0.82)
            img[y, x] = c
    img[..., 3] = 255
    save(img, f'block/{name}.png')
    return img


def blocks():
    grass('#5DAE3E', 'pitch_grass_light')
    grass('#478F31', 'pitch_grass_dark')
    line = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            c = shade(rgba('#F4F6F2'), 1 + R.normal(0, 0.025))
            if R.random() < 0.06:
                c = shade(rgba('#7FBF5E'), 1 + R.normal(0, 0.05))
            line[y, x] = c
    line[..., 3] = 255
    save(line, 'block/pitch_line.png')

    side = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            dirt = shade(rgba('#7A5636'), 1 + R.normal(0, 0.09))
            if R.random() < 0.08:
                dirt = shade(rgba('#5E4027'), 1.0)
            g = shade(rgba('#529F39'), 1 + R.normal(0, 0.07))
            edge = 3 + (1 if (x * 7) % 5 < 2 else 0) + (1 if x % 4 == 0 else 0)
            side[y, x] = g if y < edge else dirt
    side[..., 3] = 255
    save(side, 'block/pitch_side.png')

    post = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            k = 0.86 + 0.14 * math.sin((x - 3) / 10 * math.pi)
            c = shade(rgba('#F5F7FA'), k + R.normal(0, 0.01))
            if y in (0, 15):
                c = shade(c, 0.9)
            post[y, x] = c
    post[..., 3] = 255
    save(post, 'block/goal_post.png')

    net = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                net[y, x] = shade(rgba('#F0F0F0'), 0.92 + R.normal(0, 0.04))
    save(net, 'block/goal_net.png')
    edge = canvas(16, 16)
    edge[:, 7:9] = rgba('#EDEDED')
    save(edge, 'block/goal_net_edge.png')

    flag = canvas(16, 16)
    for y in range(16):
        flag[y, 0:2] = rgba('#FFFFFF') if (y // 2) % 2 == 0 else rgba('#E53935')
    for y in range(0, 7):
        for x in range(2, 16):
            wave = int(1.2 * math.sin(x / 3.0))
            yy = y + wave + 1
            if 0 <= yy < 16:
                c = rgba('#FFEB3B') if ((x // 3) + (y // 3)) % 2 == 0 else rgba('#E53935')
                flag[yy, x] = shade(c, 0.9 + 0.1 * math.sin(x / 2.0))
    save(flag, 'block/corner_flag.png')

    for name, col in (('seat_red', '#D32F2F'), ('seat_blue', '#1976D2'), ('seat_white', '#ECEFF1')):
        t = canvas(16, 16)
        for y in range(16):
            for x in range(16):
                k = 0.88 + 0.12 * math.cos((x - 7.5) / 9 * math.pi) * math.cos((y - 7.5) / 9 * math.pi)
                c = shade(rgba(col), k + R.normal(0, 0.015))
                if x in (0, 15) or y in (0, 15):
                    c = shade(c, 0.8)
                if 3 <= x <= 5 and 2 <= y <= 3:
                    c = shade(c, 1.2)
                t[y, x] = c
        t[..., 3] = 255
        save(t, f'block/{name}.png')

    step = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            c = shade(rgba('#9EA3A8'), 1 + R.normal(0, 0.035))
            if y == 15 or x == 15:
                c = shade(c, 0.85)
            if R.random() < 0.02:
                c = shade(c, 0.8)
            step[y, x] = c
    step[..., 3] = 255
    save(step, 'block/stand_step.png')

    front = canvas(16, 16)
    front[...] = rgba('#2B2F33')
    for cy in range(3):
        for cx in range(3):
            x0, y0 = 1 + cx * 5, 1 + cy * 5
            for y in range(4):
                for x in range(4):
                    d = math.hypot(x - 1.5, y - 1.5) / 2.2
                    front[y0 + y, x0 + x] = mix(rgba('#FFFFFF'), rgba('#FFE082'), min(1, d))
    save(front, 'block/floodlight_front.png')
    sidet = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            c = shade(rgba('#3A3F45'), 1 + R.normal(0, 0.04))
            if y % 4 == 0:
                c = shade(c, 0.75)
            sidet[y, x] = c
    sidet[..., 3] = 255
    save(sidet, 'block/floodlight_side.png')


# ----------------------------------------------------------------------------------------------- items

def items():
    pb = canvas(16, 16)
    for y in range(2, 14):
        for x in range(1, 15):
            c = rgba('#4CAF50') if (x // 2) % 2 == 0 else rgba('#43A047')
            pb[y, x] = shade(c, 1 + R.normal(0, 0.03))
    w = rgba('#FFFFFF')
    pb[2, 1:15] = w
    pb[13, 1:15] = w
    pb[2:14, 1] = w
    pb[2:14, 14] = w
    pb[2:14, 7] = w
    for (x, y) in ((6, 6), (6, 9), (8, 6), (8, 9), (7, 5), (7, 10), (5, 7), (5, 8), (9, 7), (9, 8)):
        pb[y, x] = w
    pb[5:11, 3] = w
    pb[5, 1:4] = w
    pb[10, 1:4] = w
    pb[5:11, 12] = w
    pb[5, 12:15] = w
    pb[10, 12:15] = w
    pb[6:10, 0] = rgba('#E0E0E0')
    pb[6:10, 15] = rgba('#E0E0E0')
    outline(pb, [20, 40, 20, 255])
    save(pb, 'item/pitch_builder.png')

    wh = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 9.5, y - 9.5
            if dx * dx + dy * dy < 16:
                k = 0.7 + 0.3 * max(0, (-dx - dy) / 5)
                wh[y, x] = shade(rgba('#CFD8DC'), k)
    for x in range(2, 9):
        for y in range(7, 11):
            wh[y, x] = shade(rgba('#B0BEC5'), 0.8 + 0.2 * (y == 7))
    wh[8:10, 2] = rgba('#37474F')
    wh[9, 9] = rgba('#263238')
    wh[9, 10] = rgba('#263238')
    for i in range(6):
        wh[12 + i // 3, 10 - i] = rgba('#E53935')
        wh[2 + i, 12 + i // 4] = rgba('#E53935')
    outline(wh, [30, 34, 40, 255])
    save(wh, 'item/whistle.png')

    for name, col, col2 in (('bot_red', '#C62828', '#FFD54F'), ('bot_blue', '#1565C0', '#FFFFFF')):
        it = canvas(16, 16)
        shape = [
            "................",
            "....##....##....",
            "..####.##.####..",
            ".##############.",
            "################",
            "####.######.####",
            ".##..######..##.",
            ".....######.....",
            ".....######.....",
            ".....######.....",
            ".....######.....",
            ".....######.....",
            ".....######.....",
            ".....######.....",
            "................",
            "................",
        ]
        for y, row in enumerate(shape):
            for x, ch in enumerate(row):
                if ch == '#':
                    c = rgba(col)
                    k = 1.08 - 0.25 * y / 15 + R.normal(0, 0.02)
                    it[y, x] = shade(c, k)
        it[1, 6:10] = rgba(col2)
        it[8, 6] = rgba(col2)
        it[9, 6:9] = rgba(col2)
        it[8:13, 9] = rgba(col2)
        it[10:13, 6] = rgba(col2)
        it[12, 6:10] = rgba(col2)
        outline(it, [18, 18, 24, 255])
        save(it, f'item/{name}.png')


def fan():
    img = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            img[y, x] = shade(rgba('#FFFFFF'), 0.9 + 0.1 * R.random())
    img[..., 3] = 255
    save(img, 'entity/fan.png')


if __name__ == '__main__':
    fan()
    ball()
    aura()
    for n, k in KITS.items():
        kit(n, k)
    for i in range(8):
        bot_skin(i)
    numbers()
    blocks()
    items()
    print('textures done')
