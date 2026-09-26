"""Small effect models: crystal spike cluster, fire pillar, watcher orb + prison rune texture."""
import math, numpy as np
from PIL import Image
from core import Model, rot_zyx
from paint import Painter, noise2, lerp, glow_img
import warden, wolf

# ---- crystal spike cluster (erupts from the ground) ----
CS = Model('crystal_spike', 128, 64)
CS.bone('root', None, (0, 24, 0))
spikes = [(0, 0, 0, 0, 5, 30), (5, 0, 2, 25, 3.6, 18), (-5, 0, 1, -24, 3.6, 20), (1, 0, -5, 0, 3.2, 16),
          (-2, 0, 5, 0, 3.0, 14), (4, 0, -3, 30, 2.6, 11), (-4, 0, -3, -28, 2.6, 12)]
for i, (x, y, z, lean, s, L) in enumerate(spikes):
    n = f's{i}'
    ang = math.atan2(z, x) if (x or z) else 0
    CS.bone_w(n, 'root', (x, 24, z), rot_zyx(math.radians(lean * math.sin(ang + 1)), math.radians(i * 37), math.radians(-lean * math.cos(ang + 1))))
    CS.cube_l(n, (-s / 2, -L, -s / 2), (s, L, s), 'crystal')
    t = s * 0.55
    CS.cube_l(n, (-t / 2, -L - L * 0.22, -t / 2), (t, L * 0.22, t), 'crystal_tip')
CSP = Painter(CS)
CSP.mats.update({k: warden.P.mats[k] for k in ('crystal', 'crystal_tip')})

# ---- fire pillar (hellfire fissure eruption) ----
FP = Model('fire_pillar', 128, 128)
FP.bone('root', None, (0, 24, 0))
for i, (w, h, yr) in enumerate(((20, 44, 0), (20, 44, 90), (14, 30, 45), (14, 30, 135))):
    FP.bone(f'f{i}', 'root', (0, 24, 0), (0, yr, 0))
    FP.cube_l(f'f{i}', (-w / 2, -h, 0), (w, h, 0), 'flame', plane=True)
FPP = Painter(FP)
FPP.mats['flame'] = wolf.P.mats['flame']

# ---- watcher orb (summoned floating seal eye) ----
WO = Model('watcher', 64, 64)
WO.bone('root', None, (0, 16, 0))
WO.cube_l('root', (-6, -6, -1), (12, 12, 2), 'orb')
for i in range(3):
    a = math.radians(90 + i * 120)
    n = f'c{i}'
    WO.bone_w(n, 'root', (math.cos(a) * 6.5, 16 - math.sin(a) * 6.5, 0.5), rot_zyx(0, 0, -a + math.radians(90)))
    WO.cube_l(n, (-1, -5, -1), (2, 5, 2), 'crystal')
WOP = Painter(WO)
WOP.mats.update({k: warden.P.mats[k] for k in ('orb', 'crystal')})


def prison_rune(size=256):
    S = size
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    c = S / 2
    d = np.hypot(xx - c, yy - c) / c
    ang = np.arctan2(yy - c, xx - c)
    m = np.zeros((S, S))
    m = np.maximum(m, np.exp(-((d - 0.95) / 0.012) ** 2))
    m = np.maximum(m, np.exp(-((d - 0.88) / 0.008) ** 2) * 0.8)
    # rune ticks between rings
    ticks = (np.abs(((ang / (2 * np.pi) * 48) % 1) - 0.5) < 0.12) & (d > 0.885) & (d < 0.945)
    m = np.maximum(m, ticks * 0.9)
    # triangle
    pts = [(c + math.cos(math.radians(-90 + k * 120)) * c * 0.78, c + math.sin(math.radians(-90 + k * 120)) * c * 0.78) for k in range(3)]
    for k in range(3):
        (x0, y0), (x1, y1) = pts[k], pts[(k + 1) % 3]
        vx, vy = x1 - x0, y1 - y0
        L2 = vx * vx + vy * vy
        t = np.clip(((xx - x0) * vx + (yy - y0) * vy) / L2, 0, 1)
        dist = np.hypot(xx - (x0 + t * vx), yy - (y0 + t * vy))
        m = np.maximum(m, np.exp(-(dist / (S * 0.008)) ** 2))
    # eye circles at vertices
    for (x0, y0) in pts:
        dd = np.hypot(xx - x0, yy - y0) / (S * 0.085)
        m = np.maximum(m, np.exp(-((dd - 1) / 0.12) ** 2))
        m = np.maximum(m, (dd < 0.45) * 1.0)
    # inner star
    dd = np.hypot(xx - c, yy - c) / (S * 0.16)
    star = dd < (0.55 + 0.45 * (0.5 + 0.5 * np.cos(6 * ang)) ** 3)
    m = np.maximum(m, star * 0.8)
    m = np.maximum(m, np.exp(-((dd - 1.25) / 0.06) ** 2) * 0.7)
    img = np.zeros((S, S, 4))
    img[..., 0] = 255 * np.clip(0.9 + 0.1 * m, 0, 1)
    img[..., 1] = 255 * np.clip(0.35 + 0.55 * m ** 2, 0, 1)
    img[..., 2] = 255 * np.clip(0.8 + 0.2 * m, 0, 1)
    img[..., 3] = 255 * np.clip(m, 0, 1)
    img[..., :3] *= img[..., 3:4] / 255.0
    return Image.fromarray(img.astype(np.uint8), 'RGBA')
