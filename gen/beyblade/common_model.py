"""The ten ordinary (starter) beyblades: one shared mesh + a procedurally painted top texture per variant.

Shape: a four-bladed metal wheel (saw-tooth rim), a coloured clear wheel ring, a dark recess and a round face bolt with
the variant's emblem. The texture window is the disc radius * 1.04 (BeyMesh ghostHalf = 1.04).

Output:
  models/common_mesh.bin
  textures/entity/common_N_face.png / _glow.png / _parts.png
  textures/item/common_N_icon.png
"""
import math, os, struct
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade')

VARIANTS = [  # id, primary, secondary, emblem
    ('iron_bull', 0xD83228, 0xFFC23A, 'horns'),
    ('frost_wolf', 0x6EC8FF, 0xF0FAFF, 'crescent'),
    ('jade_turtle', 0x2FA24A, 0xB8FF9A, 'hexagon'),
    ('thunder_viper', 0xF5D21E, 0x9A4CFF, 'bolt'),
    ('shadow_bat', 0x6B2FB5, 0xD9B8FF, 'wings'),
    ('sky_falcon', 0x3E8EF0, 0xFFFFFF, 'star'),
    ('crimson_scorpion', 0xA0102A, 0xFF8A3A, 'cross'),
    ('golden_phoenix', 0xFFB020, 0xFF5A1A, 'sun'),
    ('ocean_shark', 0x1A4FA0, 0x40E0D0, 'fin'),
    ('storm_fox', 0xFF7A1A, 0xFFF2E0, 'fox'),
]

S = 1.04          # texture window half size (model units)
TEX = 512
TOP_BASE = 0.70
WHEEL_BOTTOM = 0.62
BLADES = 4


def rgb(c):
    return np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255], np.float32) / 255.0


def edge(theta):
    """rim radius: four saw-tooth blades (leading edge steep, trailing edge sloping)"""
    f = (theta * BLADES / (2 * math.pi)) % 1.0
    tooth = f ** 0.8 if f < 0.86 else (1 - f) / 0.14 * 0.86 ** 0.8
    return 0.86 + 0.14 * tooth


def height(r, theta):
    if r < 0.27:
        return 0.50 - 0.04 * (r / 0.27) ** 4          # face bolt dome
    if r < 0.30:
        return 0.50 - (r - 0.27) / 0.03 * 0.34         # bolt rim down to recess
    if r < 0.36:
        return 0.15
    if r < 0.70:
        bump = 0.03 * max(0.0, math.sin(theta * BLADES * 2 + r * 9))
        return 0.33 + bump                              # clear wheel
    e = edge(theta)
    k = max(0.0, (r - 0.9 * e) / (0.1 * e))
    return 0.28 - 0.12 * min(1.0, k) ** 2               # metal wheel falling off at the rim


class Mesh:
    def __init__(self):
        self.groups = {}

    def quad(self, g, vs):
        self.groups.setdefault(g, []).append(vs)

    def write(self, path):
        with open(path, 'wb') as f:
            f.write(struct.pack('>ii', 0x4C425241, len(self.groups)))
            for gid in sorted(self.groups):
                qs = self.groups[gid]
                f.write(struct.pack('>ii', gid, len(qs)))
                for q in qs:
                    for v in q:
                        f.write(struct.pack('>8f', *[float(c) for c in v]))


SW = {'track': (0, 0), 'tip': (16, 0), 'under': (32, 0), 'point': (48, 0), 'track_light': (0, 16), 'bearing': (16, 16)}


def sw_uv(name, fu, fv):
    u0, v0 = SW[name]
    return (u0 + 0.5 + fu * 15) / 64.0, (v0 + 0.5 + fv * 15) / 64.0


def tex_uv(x, z):
    return (x + S) / (2 * S), (z + S) / (2 * S)


def build():
    m = Mesh()
    NS = 160
    rings = np.concatenate([np.linspace(0.002, 0.30, 10), np.linspace(0.30, 0.86, 26)[1:], np.linspace(0.86, 1.0, 10)[1:]])
    thetas = [2 * math.pi * i / NS for i in range(NS)]
    grid = np.zeros((len(rings), NS, 3))
    for j, rr in enumerate(rings):
        for i, t in enumerate(thetas):
            rad = rr
            if rr > 0.86:
                k = (rr - 0.86) / 0.14
                rad = 0.86 + (edge(t) - 0.86) * k
            grid[j, i] = (rad * math.cos(t), TOP_BASE + height(rad, t), rad * math.sin(t))

    def normal(j, i):
        jm, jp = max(j - 1, 0), min(j + 1, len(rings) - 1)
        a = grid[jp, i] - grid[jm, i]
        b = grid[j, (i + 1) % NS] - grid[j, (i - 1) % NS]
        n = np.cross(b, a)
        if n[1] < 0:
            n = -n
        return n / (np.linalg.norm(n) + 1e-9)

    def tv(j, i):
        p = grid[j, i]
        u, v = tex_uv(p[0], p[2])
        n = normal(j, i)
        return (p[0], p[1], p[2], u, v, n[0], n[1], n[2])

    for j in range(len(rings) - 1):
        for i in range(NS):
            i2 = (i + 1) % NS
            m.quad(0, [tv(j, i), tv(j + 1, i), tv(j + 1, i2), tv(j, i2)])
    # outer wall
    jl = len(rings) - 1
    lip = TOP_BASE + 0.02
    for i in range(NS):
        i2 = (i + 1) % NS
        pts = []
        for ii in (i, i2):
            p = grid[jl, ii]
            t = thetas[ii]
            e = edge(t) * 0.97
            u, v = tex_uv(e * math.cos(t), e * math.sin(t))
            n = (math.cos(t), 0.0, math.sin(t))
            pts.append(((p[0], p[1], p[2], u, v) + n, (p[0] * 0.995, lip, p[2] * 0.995, u, v) + n))
        (at, ab), (bt, bb) = pts
        m.quad(1, [at, ab, bb, bt])
    # underside lip -> track
    under_r = [1.0, 0.93, 0.75, 0.55, 0.36, 0.30]
    under_y = [lip, TOP_BASE - 0.02, TOP_BASE - 0.05, TOP_BASE - 0.065, WHEEL_BOTTOM + 0.01, WHEEL_BOTTOM]
    for k in range(len(under_r) - 1):
        for i in range(NS):
            q = []
            for (rk, ii) in ((k, i), (k + 1, i), (k + 1, (i + 1) % NS), (k, (i + 1) % NS)):
                t = thetas[ii]
                rad = under_r[rk] * (edge(t) * 0.995 if rk <= 1 else 1.0)
                dr, dy = under_r[k] - under_r[k + 1], under_y[k] - under_y[k + 1]
                nn = np.array([dy * math.cos(t), -dr, dy * math.sin(t)])
                nn /= np.linalg.norm(nn) + 1e-9
                u, v = sw_uv('under', ii / NS, rk / (len(under_r) - 1))
                q.append((rad * math.cos(t), under_y[rk], rad * math.sin(t), u, v, nn[0], nn[1], nn[2]))
            m.quad(2, q)
    # track + flat tip (lathe)
    prof = [(0.30, WHEEL_BOTTOM, 'track_light'), (0.30, WHEEL_BOTTOM - 0.04, 'track'), (0.22, WHEEL_BOTTOM - 0.08, 'track'),
            (0.20, 0.38, 'track'), (0.26, 0.33, 'track_light'), (0.27, 0.25, 'bearing'), (0.24, 0.16, 'tip'),
            (0.20, 0.05, 'tip'), (0.14, 0.01, 'tip'), (0.0, 0.0, 'point')]
    NL = 40
    for k in range(len(prof) - 1):
        r0, y0, s0 = prof[k]
        r1, y1, _ = prof[k + 1]
        dr, dy = r1 - r0, y1 - y0
        for i in range(NL):
            q = []
            for (rr, yy, ii, fv) in ((r0, y0, i, 0), (r1, y1, i, 1), (r1, y1, i + 1, 1), (r0, y0, i + 1, 0)):
                t = 2 * math.pi * ii / NL
                nn = np.array([-dy * math.cos(t), dr, -dy * math.sin(t)])
                nn /= np.linalg.norm(nn) + 1e-9
                u, v = sw_uv(s0, (ii % NL) / NL, fv)
                q.append((rr * math.cos(t), yy, rr * math.sin(t), u, v, nn[0], nn[1], nn[2]))
            m.quad(2, q)
    return m


# ------------------------------------------------------------------ textures
YY, XX = np.mgrid[0:TEX, 0:TEX]
MX = (XX + 0.5) / TEX * 2 * S - S          # model x
MZ = (YY + 0.5) / TEX * 2 * S - S          # model z
R = np.hypot(MX, MZ)
TH = np.arctan2(MZ, MX) % (2 * math.pi)
EDGE = np.vectorize(edge)(TH)
rng = np.random.default_rng(5)


def emblem_mask(kind):
    """emblem drawn on the face bolt (r < 0.24), as a soft 0..1 mask"""
    size = 512
    img = Image.new('L', (size, size), 0)
    d = ImageDraw.Draw(img)
    c = size / 2
    sc = size / (2 * S) * 0.22          # px per emblem unit (emblem radius ~0.22 model units)

    def P(x, y):
        return (c + x * sc, c + y * sc)

    if kind == 'horns':
        d.polygon([P(-0.15, 0.55), P(0.15, 0.55), P(0.35, -0.1), P(-0.35, -0.1)], fill=255)
        d.polygon([P(-0.35, -0.1), P(-0.95, -0.85), P(-0.55, 0.05)], fill=255)
        d.polygon([P(0.35, -0.1), P(0.95, -0.85), P(0.55, 0.05)], fill=255)
    elif kind == 'crescent':
        d.ellipse([P(-0.8, -0.8), P(0.8, 0.8)], fill=255)
        d.ellipse([P(-0.45, -0.95), P(1.0, 0.55)], fill=0)
    elif kind == 'hexagon':
        d.polygon([P(0.85 * math.cos(a), 0.85 * math.sin(a)) for a in [k * math.pi / 3 for k in range(6)]], fill=255)
        d.polygon([P(0.5 * math.cos(a), 0.5 * math.sin(a)) for a in [k * math.pi / 3 for k in range(6)]], fill=0)
        d.polygon([P(0.3 * math.cos(a), 0.3 * math.sin(a)) for a in [k * math.pi / 3 for k in range(6)]], fill=255)
    elif kind == 'bolt':
        d.polygon([P(0.15, -0.95), P(-0.45, 0.1), P(-0.02, 0.1), P(-0.2, 0.95), P(0.45, -0.15), P(0.04, -0.15)], fill=255)
    elif kind == 'wings':
        d.polygon([P(0, -0.25), P(-0.95, -0.55), P(-0.7, 0.05), P(-0.85, 0.45), P(-0.3, 0.2), P(0, 0.55),
                   P(0.3, 0.2), P(0.85, 0.45), P(0.7, 0.05), P(0.95, -0.55)], fill=255)
    elif kind == 'star':
        pts = []
        for k in range(10):
            a = -math.pi / 2 + k * math.pi / 5
            rr = 0.92 if k % 2 == 0 else 0.4
            pts.append(P(rr * math.cos(a), rr * math.sin(a)))
        d.polygon(pts, fill=255)
    elif kind == 'cross':
        d.polygon([P(-0.2, -0.9), P(0.2, -0.9), P(0.2, -0.2), P(0.9, -0.2), P(0.9, 0.2), P(0.2, 0.2), P(0.2, 0.9),
                   P(-0.2, 0.9), P(-0.2, 0.2), P(-0.9, 0.2), P(-0.9, -0.2), P(-0.2, -0.2)], fill=255)
        d.ellipse([P(-0.12, -0.12), P(0.12, 0.12)], fill=0)
    elif kind == 'sun':
        d.ellipse([P(-0.45, -0.45), P(0.45, 0.45)], fill=255)
        for k in range(8):
            a = k * math.pi / 4
            d.polygon([P(0.55 * math.cos(a - 0.18), 0.55 * math.sin(a - 0.18)), P(0.95 * math.cos(a), 0.95 * math.sin(a)),
                       P(0.55 * math.cos(a + 0.18), 0.55 * math.sin(a + 0.18))], fill=255)
    elif kind == 'fin':
        d.polygon([P(-0.8, 0.55), P(0.75, 0.55), P(0.1, -0.95), P(0.0, -0.2)], fill=255)
        d.polygon([P(-0.95, 0.75), P(0.95, 0.75), P(0.95, 0.62), P(-0.95, 0.62)], fill=255)
    elif kind == 'fox':
        d.polygon([P(-0.75, -0.85), P(-0.35, -0.2), P(-0.6, 0.05)], fill=255)
        d.polygon([P(0.75, -0.85), P(0.35, -0.2), P(0.6, 0.05)], fill=255)
        d.polygon([P(-0.7, -0.2), P(0.7, -0.2), P(0.0, 0.9)], fill=255)
        d.ellipse([P(-0.38, 0.0), P(-0.18, 0.2)], fill=0)
        d.ellipse([P(0.18, 0.0), P(0.38, 0.2)], fill=0)
    m = np.asarray(img.filter(ImageFilter.GaussianBlur(1.2))).astype(np.float32) / 255
    return m


def paint(primary, secondary, kind):
    p, s = rgb(primary), rgb(secondary)
    img = np.zeros((TEX, TEX, 4), np.float32)
    glow = np.zeros((TEX, TEX, 3), np.float32)
    # metal wheel: brushed chrome tinted toward the secondary colour, darker grooves at the blade steps
    brushed = 0.72 + 0.12 * np.sin(TH * 60 + rng.random() * 6) * 0.3 + rng.normal(0, 0.025, R.shape)
    light = 0.85 + 0.25 * np.cos(TH - 2.3)                      # fake top-left light
    chrome = np.stack([brushed] * 3, -1) * light[..., None]
    chrome = chrome * 0.82 + s[None, None, :] * 0.18
    f = (TH * BLADES / (2 * math.pi)) % 1.0
    groove = np.exp(-((f - 0.86) / 0.012) ** 2) + np.exp(-((f - 0.02) / 0.01) ** 2) * 0.5
    chrome *= (1 - 0.45 * groove)[..., None]
    rim = np.clip((R - 0.9 * EDGE) / (0.1 * EDGE), 0, 1)
    chrome *= (1 - 0.35 * rim)[..., None]
    col = chrome.copy()
    # clear wheel ring: primary colour with a lighter swept pattern
    cw = (R > 0.36) & (R < 0.70)
    swirl = 0.5 + 0.5 * np.sin(TH * BLADES * 2 + R * 9)
    cwc = p[None, None, :] * (0.65 + 0.45 * swirl[..., None]) + (s[None, None, :] - p[None, None, :]) * 0.25 * (swirl[..., None] > 0.85)
    edge_line = (np.abs(R - 0.36) < 0.01) | (np.abs(R - 0.70) < 0.012)
    col[cw] = cwc[cw]
    col[edge_line] = col[edge_line] * 0.4
    glow[cw] = p * (0.55 + 0.45 * swirl[cw][:, None])
    # recess
    rc = (R >= 0.30) & (R <= 0.36)
    col[rc] = np.array([0.12, 0.12, 0.14])
    # face bolt: dark with a metal rim and the emblem
    fb = R < 0.30
    bolt = np.array([0.16, 0.16, 0.2]) * (0.9 + 0.2 * (1 - R / 0.3))[..., None]
    col[fb] = bolt[fb]
    ring = (R > 0.25) & (R < 0.30)
    col[ring] = np.array([0.75, 0.76, 0.8]) * (0.8 + 0.2 * np.cos(TH[ring] - 2.3))[:, None]
    em = emblem_mask(kind)
    col = col * (1 - em[..., None]) + s[None, None, :] * em[..., None]
    glow = glow * (1 - em[..., None]) + np.clip(s * 1.1 + 0.15, 0, 1)[None, None, :] * em[..., None]
    alpha = (R < EDGE + 0.004).astype(np.float32)
    img[..., :3] = np.clip(col, 0, 1)
    img[..., 3] = alpha
    return img, glow * alpha[..., None]


def parts(primary, secondary):
    P = np.zeros((64, 64, 4), np.float32)
    P[..., 3] = 255
    yy, xx = np.mgrid[0:16, 0:16]
    r = np.random.default_rng(3)

    def cell(name, base, stripes=0.0, noise=0.03):
        u0, v0 = SW[name]
        c = np.array(base, np.float32)[None, None, :]
        shade = 1.0 + stripes * np.sin(xx / 16 * math.tau * 4)[..., None] + r.normal(0, noise, (16, 16, 1))
        shade = shade * (0.85 + 0.3 * (1 - yy / 15))[..., None]
        P[v0:v0 + 16, u0:u0 + 16, :3] = np.clip(c * shade, 0, 1) * 255

    p, s = rgb(primary), rgb(secondary)
    cell('track', p * 0.35 + 0.1, 0.08)
    cell('track_light', p * 0.8, 0.06)
    cell('tip', (0.85, 0.85, 0.88), 0.05)
    cell('under', (0.62, 0.64, 0.68), 0.05)
    cell('point', (0.4, 0.4, 0.42), 0.02)
    cell('bearing', s * 0.9, 0.1)
    return P


def main():
    os.makedirs(os.path.join(ASSETS, 'models'), exist_ok=True)
    build().write(os.path.join(ASSETS, 'models/common_mesh.bin'))
    for i, (vid, p, s, kind) in enumerate(VARIANTS):
        img, glow = paint(p, s, kind)
        Image.fromarray((img * 255).astype(np.uint8), 'RGBA').save(os.path.join(ASSETS, f'textures/entity/common_{i}_face.png'))
        g = np.zeros((TEX, TEX, 4), np.uint8)
        g[..., :3] = (np.clip(glow, 0, 1) * 255).astype(np.uint8)
        g[..., 3] = 255
        Image.fromarray(g, 'RGBA').filter(ImageFilter.GaussianBlur(0.8)).save(os.path.join(ASSETS, f'textures/entity/common_{i}_glow.png'))
        Image.fromarray(parts(p, s).astype(np.uint8), 'RGBA').save(os.path.join(ASSETS, f'textures/entity/common_{i}_parts.png'))
        icon = Image.fromarray((img * 255).astype(np.uint8), 'RGBA').resize((64, 64), Image.LANCZOS)
        ia = np.asarray(icon).copy()
        ia[..., 3] = np.where(ia[..., 3] > 100, 255, 0)
        Image.fromarray(ia, 'RGBA').save(os.path.join(ASSETS, f'textures/item/common_{i}_icon.png'))
        print('variant', i, vid)


if __name__ == '__main__':
    main()
