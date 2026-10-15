"""Small models: projectiles (pointing +z, centred on the origin), ground spikes (feet at y=48) and the wrist Omnitrix."""
import math, numpy as np
from core import Model, rot_zyx
from paint import Painter, noise2, lerp
from toon import *

MODELS = []

# ---------- crystal shard (Diamondhead) ----------
CSH = Model('crystal_shard', 64, 32)
CSH.bone('base', None, (0, 0, 0))
CSH.cube('base', (-1.5, -1.5, -5), (3, 3, 10), 'crystal')
CSH.cube('base', (-1, -1, 5), (2, 2, 3), 'crystal_tip')
CSH.cube('base', (-0.6, -0.6, 8), (1.2, 1.2, 2), 'crystal_tip')
CSH.bone('rot', 'base', (0, 0, 0), (0, 0, 45))
CSH.cube('rot', (-1.2, -1.2, -4), (2.4, 2.4, 8), 'crystal')


def crystal(c, face, W, H, r, tip=False):
    yy, xx = grid(H, W)
    col = base(H, W, (150, 250, 200) if not tip else (210, 255, 235), face, (70, 200, 150), r, 0.1, 0.5)
    facet = ((xx + yy * 0.7) % 4 < 1)
    col[facet] = lerp(col[facet], (240, 255, 250), 0.5)
    col = outline(col, H, W, (30, 110, 80), 1, 0.7)
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)), (120, 255, 190), 0.45 if not tip else 0.7)


P = Painter(CSH)
P.mats['crystal'] = crystal
P.mats['crystal_tip'] = lambda c, f, W, H, r: crystal(c, f, W, H, r, True)
MODELS.append(('crystal_shard', CSH, P))

# ---------- ice spear (Big Chill) ----------
ISP = Model('ice_spear', 64, 64)
ISP.bone('base', None, (0, 0, 0))
ISP.cube('base', (-1.5, -1.5, -14), (3, 3, 22), 'ice')
ISP.cube('base', (-2.5, -2.5, 6), (5, 5, 4), 'ice')
ISP.cube('base', (-1.6, -1.6, 10), (3.2, 3.2, 4), 'ice_tip')
ISP.cube('base', (-0.8, -0.8, 14), (1.6, 1.6, 3), 'ice_tip')
for i, a in enumerate((0, 90, 180, 270)):
    ISP.bone(f'fin{i}', 'base', (0, 0, -12), (0, 0, a))
    ISP.cube(f'fin{i}', (-0.5, 1.4, -12), (1, 3, 5), 'ice')


def ice(c, face, W, H, r, tip=False):
    yy, xx = grid(H, W)
    col = base(H, W, (200, 235, 255) if not tip else (240, 250, 255), face, (110, 170, 230), r, 0.12, 0.5)
    m = voronoi_cracks(H, W, r, 3.0, 0.35)
    col = paint(col, m * 0.6, (255, 255, 255))
    col = outline(col, H, W, (60, 110, 170), 1, 0.6)
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)), (150, 210, 255), 0.35 if not tip else 0.6)


P = Painter(ISP)
P.mats['ice'] = ice
P.mats['ice_tip'] = lambda c, f, W, H, r: ice(c, f, W, H, r, True)
MODELS.append(('ice_spear', ISP, P))

# ---------- sonic disc (Ultimate Echo Echo) ----------
SD = Model('sonic_disc', 64, 32)
SD.bone('base', None, (0, 0, 0))
SD.cube('base', (-4, -4, -0.8), (8, 8, 1.6), 'disc')
SD.cube('base', (-3, -5, -0.6), (6, 1, 1.2), 'disc_rim')
SD.cube('base', (-3, 4, -0.6), (6, 1, 1.2), 'disc_rim')
SD.cube('base', (-5, -3, -0.6), (1, 6, 1.2), 'disc_rim')
SD.cube('base', (4, -3, -0.6), (1, 6, 1.2), 'disc_rim')
SD.cube('base', (-2.4, -2.4, -1.2), (4.8, 4.8, 2.4), 'disc_core')


def disc(c, face, W, H, r):
    col = base(H, W, (40, 44, 60), face, (20, 22, 30), r, 0.05)
    col = outline(col, H, W, (5, 5, 10), 1, 0.8)
    return col, np.ones((H, W)), None


def disc_core(c, face, W, H, r):
    col = np.zeros((H, W, 3), np.float32) + (30, 255, 255)
    yy, xx = grid(H, W)
    e = ellipse(H, W, W / 2, H / 2, W * 0.32, H * 0.32)
    col = paint(col, e, (200, 255, 255))
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)), (60, 250, 255), 1.0)


P = Painter(SD)
P.mats['disc'] = disc
P.mats['disc_rim'] = disc
P.mats['disc_core'] = disc_core
MODELS.append(('sonic_disc', SD, P))

# ---------- crystal spike cluster (Diamondhead ground spikes) ----------
def spike_model(name, mat, tipmat):
    M = Model(name, 128, 64)
    M.bone('base', None, (0, 48, 0))
    spikes = [(0, 0, 0, 5, 30), (5, 2, 25, 3.6, 18), (-5, 1, -24, 3.6, 20), (1, -5, 0, 3.2, 16),
              (-2, 5, 0, 3.0, 14), (4, -3, 30, 2.6, 11), (-4, -3, -28, 2.6, 12)]
    for i, (x, z, lean, s, L) in enumerate(spikes):
        n = f's{i}'
        ang = math.atan2(z, x) if (x or z) else 0
        M.bone_w(n, 'base', (x, 48, z), rot_zyx(math.radians(lean * math.sin(ang + 1)), math.radians(i * 37), math.radians(-lean * math.cos(ang + 1))))
        M.cube_l(n, (-s / 2, -L, -s / 2), (s, L, s), mat)
        t = s * 0.55
        M.cube_l(n, (-t / 2, -L - L * 0.25, -t / 2), (t, L * 0.25, t), tipmat)
    return M


CS = spike_model('crystal_spike', 'crystal', 'crystal_tip')
P = Painter(CS)
P.mats['crystal'] = crystal
P.mats['crystal_tip'] = lambda c, f, W, H, r: crystal(c, f, W, H, r, True)
MODELS.append(('crystal_spike', CS, P))

IS = spike_model('ice_spike', 'ice', 'ice_tip')
P = Painter(IS)
P.mats['ice'] = ice
P.mats['ice_tip'] = lambda c, f, W, H, r: ice(c, f, W, H, r, True)
MODELS.append(('ice_spike', IS, P))

# ---------- wrist Omnitrix (human player, centred on the arm, D=2 units) ----------
W_ = Model('watch', 64, 64)
W_.bone('base', None, (0, 0, 0))
W_.cube('base', (-4.6, -2.2, -4.6), (9.2, 4.4, 9.2), 'band')
W_.cube('base', (-4.9, -1.2, -5.0), (9.8, 2.4, 1.0), 'white')
W_.cube('base', (-4.9, -1.2, 4.0), (9.8, 2.4, 1.0), 'white')
# the dial core on the outer (+x) side of the left arm; it pops up and turns when selecting an alien
W_.bone('core', 'base', (4.6, 0, 0))
W_.cube('core', (4.4, -3.2, -3.0), (1.6, 6.4, 6.0), 'face_ring')
W_.cube('core', (5.8, -2.4, -2.2), (0.8, 4.8, 4.4), 'face')
W_.cube('core', (4.6, 2.6, -1.0), (1.2, 1.4, 2.0), 'button')
for i, (y, z) in enumerate(((-3.6, -3.4), (-3.6, 2.4), (2.6, -3.4), (2.6, 2.4))):
    W_.cube('core', (4.2, y, z), (1.0, 1.0, 1.0), 'button')


def band(c, face, W, H, r):
    col = base(H, W, (60, 62, 66), face, (32, 34, 38), r, 0.05)
    col = outline(col, H, W, (12, 12, 14), 1, 0.8)
    return col, np.ones((H, W)), None


def white(c, face, W, H, r):
    col = base(H, W, (232, 232, 232), face, (170, 170, 170), r, 0.03)
    col = outline(col, H, W, (60, 60, 60), 1, 0.7)
    return col, np.ones((H, W)), None


def face_(c, face, W, H, r):
    if face != 'east':
        return band(c, face, W, H, r)
    col = np.zeros((H, W, 3), np.float32) + (20, 24, 20)
    layers = omnitrix_symbol(H, W, W / 2, H / 2, min(W, H) / 2 - 0.1, ring=(30, 30, 30), rim=(90, 90, 90))
    col = apply(col, layers)
    return col, np.ones((H, W)), symbol_glow(H, W, W / 2, H / 2, min(W, H) / 2 - 0.1)


def button(c, face, W, H, r):
    col = np.zeros((H, W, 3), np.float32) + (110, 230, 60)
    return col, np.ones((H, W)), gmask(H, W, np.ones((H, W)), (110, 255, 60), 0.8)


P = Painter(W_)
P.mats.update({'band': band, 'face_ring': band, 'face': face_, 'white': white, 'button': button})
MODELS.append(('watch', W_, P))
