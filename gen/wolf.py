"""Kızıl Cehennem Kurdu (Crimson Hellhound) — model, textures, animations."""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, noise2, lerp, border, glow_img, draw_line, cracks

M = Model('crimson_hound', 512, 256)
R0 = np.eye(3)

# ---------------- skeleton (rest pose = crouched hunting stance of the artwork) ----------------
DY = 10   # whole torso sits low
M.bone('base', None, (0, -20 + DY, 4))
M.bone('body', 'base', (0, -20 + DY, 4))
M.cube('body', (-9.5, -40 + DY, -7), (19, 17, 15), 'fur')             # waist
M.cube('body', (-11.5, -39 + DY, 6), (23, 17, 14), 'fur')             # hips
M.cube('body', (-7.5, -42 + DY, 8), (15, 4, 10), 'fur_dark')          # hip ridge
M.bone('chest', 'body', (0, -30 + DY, -6))
M.cube('chest', (-13, -47 + DY, -26), (26, 24, 21), 'fur_chest')      # rib cage
M.cube('chest', (-9.5, -26 + DY, -24), (19, 5, 16), 'fur_crack')      # belly/sternum veins
for sx in (-1, 1):
    x = -17.5 if sx < 0 else 8
    M.cube('chest', (x, -50 + DY, -24), (9.5, 13, 15), 'fur_crack')    # shoulder humps

# mane spikes along neck, shoulders and spine (lean back)
SPIKES = [(-7, -47, -23, 18, -38, 20), (0, -48, -22, 23, -46, 0), (7, -47, -23, 18, -38, -20),
          (-10, -49, -16, 17, -55, 28), (-3, -50, -15, 22, -58, 8), (4, -50, -15, 21, -58, -8), (10, -49, -16, 16, -52, -28),
          (-6, -47, -8, 15, -64, 16), (4, -47, -8, 16, -64, -12),
          (-3, -43, 2, 11, -70, 8), (3, -43, 3, 10, -70, -8), (0, -42, 10, 9, -72, 0)]
for i, (x, y, z, L, rx, rz) in enumerate(SPIKES):
    n = f'spike{i}'
    M.bone(n, 'chest' if z < -2 else 'body', (x, y + DY, z), (rx, 0, rz))
    M.cube_l(n, (-1.8, -L, -3.6), (3.6, L, 7.2), 'fur_spike')

# neck + head
M.seg('neck', 'chest', (0, -38 + DY, -23), (0, -36 + DY, -34), 14, 14, 'fur', extend=3)
Rh = rot_zyx(math.radians(10), 0, 0)
HY, HZ = -36 + DY, -34
M.bone_w('head', 'neck', (0, HY, HZ), Rh)
M.cube_l('head', (-8, -8, -12), (16, 14, 13), 'wolf_skull')
M.cube_l('head', (-4.6, -3.5, -25), (9.2, 7, 13.2), 'muzzle')
M.cube_l('head', (-2.5, -4.2, -26.2), (5, 3, 2.4), 'nose')
M.cube_l('head', (-8.6, -9, -12.8), (17.2, 3.4, 5), 'fur_dark')      # heavy brow
M.cube_l('head', (-11, -4, -10), (3.5, 9, 8), 'fur_spike')           # cheek ruffs
M.cube_l('head', (7.5, -4, -10), (3.5, 9, 8), 'fur_spike')
M.cube_l('head', (-4.3, 3.4, -24.5), (8.6, 1.4, 11.5), 'teeth')      # upper teeth row
M.bone('jaw', 'head', (0, 0, 0))
M.bones['jaw'].off = [0.0, 3.5, -12.0]
M.bones['jaw'].rot = [14.0, 0.0, 0.0]
M.cube_l('jaw', (-4, 0, -12.5), (8, 3.4, 12.5), 'muzzle_jaw')
M.cube_l('jaw', (-3.8, -1.2, -12.3), (7.6, 1.3, 10.5), 'teeth')
for sx in (-1, 1):
    n = 'ear_r' if sx < 0 else 'ear_l'
    M.bone(n, 'head', (0, 0, 0))
    M.bones[n].off = [sx * 5.2, -8.0, -4.0]
    M.bones[n].rot = [-28.0, 0.0, sx * -22.0]
    M.cube_l(n, (-2.5, -11, -1.4), (5, 11, 2.8), 'ear')

# front arms (long, muscular, elbows splayed)
for side, sx in (('right', -1), ('left', 1)):
    A = (sx * 14, -39 + DY, -17)
    B = (sx * 29, -9, -18)
    C = (sx * 30, 18, -27)
    M.seg(f'{side}_arm', 'chest', A, B, 11, 11, 'fur_arm', extend=2.5)
    M.seg(f'{side}_fore', f'{side}_arm', B, C, 9.5, 9.5, 'fur_arm', extend=2)
    M.bone_w(f'{side}_hand', f'{side}_fore', C, rot_zyx(0, math.radians(sx * -12), 0))
    M.cube_l(f'{side}_hand', (-6, 0.5, -8.5), (12, 5.5, 12), 'fur_crack')
    M.bone_w(f'{side}_claws', f'{side}_hand', (C[0] + sx * 1.5, C[1] + 2, C[2] - 7.5), rot_zyx(0, math.radians(sx * -12), 0))
    for i, fx in enumerate((-5.2, -2.3, 0.6, 3.5)):
        M.cube_l(f'{side}_claws', (fx, -0.5, -4.5), (2.4, 4, 5), 'fur_crack')
        M.cube_l(f'{side}_claws', (fx + 0.3, 1.4, -10), (1.8, 2.4, 6), 'claw')
    M.cube_l(f'{side}_claws', (-sx * 7 - 0.9, 0.5, -1), (1.8, 2.6, 5), 'claw')   # thumb claw

# hind legs (digitigrade, heavy haunches)
for side, sx in (('right', -1), ('left', 1)):
    A = (sx * 10.5, -31 + DY, 13)
    B = (sx * 15, -5, 1)
    C = (sx * 14.5, 9, 20)
    D = (sx * 14.5, 19, 16)
    M.seg(f'{side}_thigh', 'body', A, B, 13, 14, 'fur_leg', extend=4)
    M.seg(f'{side}_shin', f'{side}_thigh', B, C, 9, 9, 'fur_leg', extend=2)
    M.seg(f'{side}_meta', f'{side}_shin', C, D, 7, 7, 'fur_crack', extend=1.5)
    M.bone_w(f'{side}_paw', f'{side}_meta', D, R0)
    M.cube_l(f'{side}_paw', (-5, 0, -7.5), (10, 5, 11), 'fur_crack')
    for fx in (-4.2, -1.4, 1.4):
        M.cube_l(f'{side}_paw', (fx + 0.2, 2.2, -11.5), (2, 2.8, 4.5), 'claw')

# three blazing tails
TAILS = {
    'tail_c': [(0, -24, 19), (0, -38, 33), (0, -50, 48), (0, -54, 66)],
    'tail_r': [(-4, -23, 19), (-16, -32, 31), (-30, -38, 42), (-42, -37, 56)],
    'tail_l': [(4, -23, 19), (16, -32, 31), (30, -38, 42), (42, -37, 56)],
}
for t, pts in TAILS.items():
    widths = (10, 13, 10.5)
    parent = 'body'
    for i in range(3):
        n = f'{t}{i}'
        M.seg(n, parent, pts[i], pts[i + 1], widths[i], widths[i], f'tail{i}', extend=1.5 if i else 0.5)
        parent = n
    # tip flame
    M.bone(f'{t}_flame', f'{t}2', M.bones[f'{t}2'].pivot)
    b = M.bones[f'{t}_flame']
    b.off = [0.0, float(np.linalg.norm(np.array(pts[3]) - np.array(pts[2]))), 0.0]
    for k, yr in enumerate((0, 90)):
        fb = f'{t}_fl{k}'
        M.bone(fb, f'{t}_flame', (0, 0, 0), (0, yr, 0))
        M.bones[fb].off = [0.0, -2.0, 0.0]
        M.cube_l(fb, (-7.5, -3, 0), (15, 20, 0), 'flame_tail', plane=True)

# flame wisps on shoulders and back
WISPS = [('wisp_sr', (-13, -38, -18), 13, 22), ('wisp_sl', (13, -38, -18), 13, 22), ('wisp_back', (0, -32, 0), 14, 20),
         ('wisp_hip', (0, -30, 12), 11, 16)]
for n, p, w, h in WISPS:
    M.bone(n, 'chest' if p[2] < -10 else 'body', p)
    for k, yr in enumerate((30, 120)):
        fb = f'{n}{k}'
        M.bone(fb, n, p, (0, yr, 0))
        M.cube_l(fb, (-w / 2, -h, 0), (w, h, 0), 'flame', plane=True)

# ---------------- textures ----------------
P = Painter(M)
FUR_D = (26, 16, 28)
FUR_M = (50, 36, 52)
FUR_L = (82, 66, 86)
CRACK = (255, 58, 72)
CRACK_HOT = (255, 170, 150)
CRACK_DARK = (140, 16, 40)


def fur_paint(c, face, W, H, r, veins=0.0, light=1.0, tip=None):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 3)
    # strands: streaks along the long (vertical) axis
    streak = noise2(H, max(W, 2), r, 1.2, 1)[:, :W]
    strands = 0.5 + 0.5 * np.sin(xx * 2.1 + streak * 6 + yy * 0.25)
    t = np.clip((0.25 + 0.35 * n + 0.3 * strands) * light, 0, 1)
    if face == 'top':
        t = np.clip(t + 0.15, 0, 1)
    if face == 'bottom':
        t = t * 0.5
    col = np.where(t[..., None] < 0.5, lerp(FUR_D, FUR_M, t * 2), lerp(FUR_M, FUR_L, (t - 0.5) * 2))
    glow = np.zeros((H, W, 4), np.float32)
    if tip is not None:
        # red gradient towards the tip (tip at local y = H)
        g = np.clip((yy / max(H - 1, 1) - tip) / (1 - tip), 0, 1)
        col = lerp(col, (205, 40, 55), g * (0.55 + 0.45 * strands))
        glow = glow_img(H, W, (255, 60, 60), g ** 1.5 * 0.8 * (0.6 + 0.4 * strands))
    if veins > 0 and face not in ('bottom',):
        cm = cracks(H, W, r, max(1, int(veins * (W * H) / 90 + r.random() * veins)), length=0.5)
        core = cm >= 1
        halo = (cm > 0) & ~core
        col[halo] = lerp(col[halo], CRACK_DARK, 0.75)
        col[core] = CRACK
        hot = core & (r.random((H, W)) < 0.25)
        col[hot] = CRACK_HOT
        gm = np.maximum(core * 1.0, halo * 0.35)
        gg = glow_img(H, W, CRACK, gm)
        gg[hot, :3] = CRACK_HOT
        a = gg[..., 3:4] / 255.0
        glow[..., :3] = glow[..., :3] * (1 - a) + gg[..., :3] * a
        glow[..., 3] = np.maximum(glow[..., 3], gg[..., 3])
    b = border(H, W)
    col[b] = lerp(col[b], (12, 6, 14), 0.55)
    return col, np.ones((H, W)), glow


P.mats['fur'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=0.12)
P.mats['fur_dark'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=0.0, light=0.6)
P.mats['fur_chest'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=0.6)
P.mats['fur_crack'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=1.6)
P.mats['fur_arm'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=1.8)
P.mats['fur_leg'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=1.3)
P.mats['tail0'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=0.3, light=1.1)
P.mats['tail1'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=0.2, light=1.1, tip=0.55)
P.mats['tail2'] = lambda c, f, W, H, r: fur_paint(c, f, W, H, r, veins=0.0, light=1.0, tip=-0.2)


@P.mat('fur_spike')
def fur_spike(c, face, W, H, r):
    col, a, g = fur_paint(c, face, W, H, r, veins=0.0, light=0.9)
    yy, xx = np.mgrid[0:H, 0:W]
    tipm = np.clip(1 - yy / max(H * 0.45, 1), 0, 1)   # top of spike = local -y = texture top
    col = lerp(col, (190, 34, 50), tipm * 0.8)
    g = glow_img(H, W, (255, 50, 60), tipm ** 2 * 0.9)
    return col, a, g


@P.mat('wolf_skull')
def skull(c, face, W, H, r):
    col, a, g = fur_paint(c, face, W, H, r, veins=0.3 if face != 'north' else 0.0)
    if face == 'north':
        yy, xx = np.mgrid[0:H, 0:W]
        for ex in (2.6, W - 3.6):
            ey = 4.2
            d = np.hypot((xx - ex) * 0.8, (yy - ey) * 1.3)
            col[d < 2.2] = (20, 4, 10)
            eye = d < 1.4
            col[eye] = (255, 40, 50)
            col[d < 0.6] = (255, 220, 200)
            gg = glow_img(H, W, (255, 40, 50), eye * 1.0)
            gg[d < 0.6, :3] = (255, 220, 200)
            g = np.where(eye[..., None], gg, g)
        # angry brow creases
        m = np.zeros((H, W))
        draw_line(m, 0, 1, 5, 3); draw_line(m, W - 1, 1, W - 6, 3)
        col[m > 0] = (14, 6, 12)
    return col, a, g


@P.mat('muzzle')
def muzzle(c, face, W, H, r):
    col, a, g = fur_paint(c, face, W, H, r, veins=0.5 if face == 'top' else 0.0)
    if face in ('west', 'east', 'north'):
        col[-2:] = (12, 4, 10)   # lip line
    return col, a, g


@P.mat('muzzle_jaw')
def muzzle_jaw(c, face, W, H, r):
    col, a, g = fur_paint(c, face, W, H, r, veins=0.0, light=0.8)
    if face == 'top':
        col[:] = lerp((120, 20, 40), (70, 8, 24), noise2(H, W, r, 2))   # mouth interior
    return col, a, g


@P.mat('nose')
def nose(c, face, W, H, r):
    col = np.zeros((H, W, 3)) + (18, 10, 16)
    col[0] = (60, 50, 60)
    return col, np.ones((H, W)), None


@P.mat('teeth')
def teeth(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    col = lerp((200, 188, 176), (250, 244, 236), noise2(H, W, r, 2))
    alpha = np.ones((H, W))
    if face in ('north', 'west', 'east', 'south'):
        # jagged teeth silhouette
        for x in range(W):
            if x % 2 == 1:
                col[:, x] = lerp(col[:, x], (120, 100, 90), 0.6)
    return col, alpha, None


@P.mat('ear')
def ear(c, face, W, H, r):
    col, a, g = fur_paint(c, face, W, H, r, veins=0.0, light=0.85)
    if face == 'north':
        yy, xx = np.mgrid[0:H, 0:W]
        inner = (np.abs(xx - (W - 1) / 2) < (yy / H) * W * 0.35) & (yy > 1)
        col[inner] = (110, 20, 40)
    return col, a, g


@P.mat('claw')
def claw(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    col = lerp((120, 10, 30), (255, 70, 80), np.clip(noise2(H, W, r, 2) * 0.6 + 0.4, 0, 1))
    col[:, :1] = (80, 5, 20)
    return col, np.ones((H, W)), glow_img(H, W, (255, 60, 70), np.full((H, W), 0.55))


@P.mat('flame')
def flame(c, face, W, H, r):
    """flame tongue on a plane: base at bottom (local +y = texture bottom), tip at top"""
    S = 3
    Hs, Ws = H * S, W * S
    yy, xx = np.mgrid[0:Hs, 0:Ws] / S
    u = (xx - W / 2) / (W / 2)          # -1..1
    v = 1 - yy / H                      # 0 bottom .. 1 top
    wob = noise2(Hs, Ws, r, 6 * S, 2)
    width = (1 - v) ** 0.7 * (0.85 + 0.3 * (wob - 0.5))
    tongues = 0.5 + 0.5 * np.sin(u * 7 + v * 5 + wob * 5)
    inside = np.abs(u) < width * (0.65 + 0.35 * tongues)
    inside &= v < 0.98
    heat = np.clip(1 - np.abs(u) / np.maximum(width, 1e-3), 0, 1) * (1 - v * 0.7)
    col = np.where((heat > 0.55)[..., None], lerp((255, 120, 90), (255, 225, 190), (heat - 0.55) / 0.45),
                   lerp((150, 10, 35), (255, 60, 60), heat / 0.55))
    alpha = inside.astype(float)
    col = col.reshape(H, S, W, S, 3).mean((1, 3))
    alpha = (alpha.reshape(H, S, W, S).mean((1, 3)) > 0.45).astype(float)
    g = np.zeros((H, W, 4), np.float32)
    g[..., :3] = col
    g[..., 3] = alpha * 255
    return col, alpha, g


P.mats['flame_tail'] = lambda c, f, W, H, r: tuple(x[::-1] if x is not None else None for x in flame(c, f, W, H, r))


# ---------------- animations ----------------
A = {}


def add_rot(a, frames, interp='C'):
    out = {}
    for t, d in frames:
        for b, v in d.items():
            out.setdefault(b, []).append((t, v))
    for b, keys in out.items():
        a.rot(b, *[(t, v, interp) for t, v in keys])


def sym(d):
    o = dict(d)
    for k, v in d.items():
        if k.startswith('right_'):
            o['left_' + k[6:]] = (v[0], -v[1], -v[2])
    return o


def tails(a, length, amp=1.0, speed=1.0, lift=0.0):
    """sinuous tail waves on all three tails"""
    n = 8
    for ti, t in enumerate(('tail_c', 'tail_r', 'tail_l')):
        for s in range(3):
            keys = []
            for k in range(n + 1):
                tt = length * k / n
                ph = 2 * math.pi * (k / n) * speed - s * 0.9 - ti * 2.1
                keys.append((tt, (lift + 6 * amp * math.sin(ph + 1.3), 14 * amp * math.sin(ph), 5 * amp * math.cos(ph))))
            a.rot(f'{t}{s}', *keys)


def flames(a, length, strength=1.0):
    names = [f'{t}_flame' for t in ('tail_c', 'tail_r', 'tail_l')] + [w for w, *_ in WISPS]
    for i, n in enumerate(names):
        keys = []
        for k in range(9):
            tt = length * k / 8
            s = 1 + 0.25 * strength * math.sin(k * 2.3 + i * 1.7)
            keys.append((tt, (1 + 0.1 * math.sin(k * 3.1 + i), s, 1 + 0.1 * math.cos(k * 2.7 + i))))
        a.scl(n, *keys)


idle = Anim('idle', 3.0, True)
idle.pos('base', (0, (0, 0, 0)), (1.5, (0, -0.8, 0)), (3, (0, 0, 0)))
idle.rot('chest', (0, (0, 0, 0)), (1.5, (-2, 0, 0)), (3, (0, 0, 0)))
idle.rot('head', (0, (0, 0, 0)), (0.8, (4, 6, 0)), (1.6, (2, -4, 2)), (2.3, (5, 2, -2)), (3, (0, 0, 0)))
idle.rot('jaw', (0, (0, 0, 0)), (1.5, (6, 0, 0)), (3, (0, 0, 0)))
idle.rot('ear_r', (0, (0, 0, 0)), (1.0, (0, 0, 0)), (1.1, (-12, 0, 0)), (1.25, (0, 0, 0)), (3, (0, 0, 0)))
tails(idle, 3.0, 0.7, 1.0)
flames(idle, 3.0)
A['IDLE'] = idle

walk = Anim('walk', 1.2, True)
def wf(s):
    return {'right_arm': (-22 * s, 0, 0), 'right_fore': (18 * max(0, s), 0, 0), 'left_arm': (22 * s, 0, 0), 'left_fore': (18 * max(0, -s), 0, 0),
            'right_thigh': (24 * s, 0, 0), 'right_shin': (-14 * max(0, -s), 0, 0), 'left_thigh': (-24 * s, 0, 0), 'left_shin': (-14 * max(0, s), 0, 0),
            'chest': (0, 6 * s, 3 * s), 'body': (0, -4 * s, 0), 'head': (0, -6 * s, 0)}
add_rot(walk, [(0, wf(1)), (0.3, wf(0)), (0.6, wf(-1)), (0.9, wf(0)), (1.2, wf(1))])
walk.pos('base', (0, (0, 0, 0)), (0.3, (0, 1.2, 0)), (0.6, (0, 0, 0)), (0.9, (0, 1.2, 0)), (1.2, (0, 0, 0)))
tails(walk, 1.2, 0.6, 1.0)
flames(walk, 1.2)
A['WALK'] = walk

# gallop: bounding leaps, spine flexes, tails stream behind
run = Anim('run', 0.6, True)
add_rot(run, [
    (0.0, {'right_arm': (-55, 0, 0), 'left_arm': (-45, 0, 0), 'right_fore': (-10, 0, 0), 'left_fore': (-10, 0, 0),
           'right_thigh': (40, 0, 0), 'left_thigh': (35, 0, 0), 'right_shin': (-10, 0, 0), 'left_shin': (-10, 0, 0),
           'chest': (-10, 0, 0), 'body': (6, 0, 0), 'head': (12, 0, 0)}),
    (0.15, {'right_arm': (10, 0, 0), 'left_arm': (20, 0, 0), 'right_fore': (35, 0, 0), 'left_fore': (35, 0, 0),
            'right_thigh': (-10, 0, 0), 'left_thigh': (-5, 0, 0), 'right_shin': (-30, 0, 0), 'left_shin': (-30, 0, 0),
            'chest': (4, 0, 0), 'body': (-2, 0, 0), 'head': (-4, 0, 0)}),
    (0.3, {'right_arm': (40, 0, 0), 'left_arm': (45, 0, 0), 'right_fore': (20, 0, 0), 'left_fore': (15, 0, 0),
           'right_thigh': (-45, 0, 0), 'left_thigh': (-40, 0, 0), 'right_shin': (20, 0, 0), 'left_shin': (20, 0, 0),
           'chest': (8, 0, 0), 'body': (-8, 0, 0), 'head': (-10, 0, 0)}),
    (0.45, {'right_arm': (-20, 0, 0), 'left_arm': (-10, 0, 0), 'right_fore': (-30, 0, 0), 'left_fore': (-25, 0, 0),
            'right_thigh': (10, 0, 0), 'left_thigh': (15, 0, 0), 'right_shin': (-5, 0, 0), 'left_shin': (-5, 0, 0),
            'chest': (-4, 0, 0), 'body': (2, 0, 0), 'head': (6, 0, 0)}),
    (0.6, {'right_arm': (-55, 0, 0), 'left_arm': (-45, 0, 0), 'right_fore': (-10, 0, 0), 'left_fore': (-10, 0, 0),
           'right_thigh': (40, 0, 0), 'left_thigh': (35, 0, 0), 'right_shin': (-10, 0, 0), 'left_shin': (-10, 0, 0),
           'chest': (-10, 0, 0), 'body': (6, 0, 0), 'head': (12, 0, 0)}),
])
run.pos('base', (0, (0, 0, 0)), (0.15, (0, 4, 0)), (0.3, (0, 1, 0)), (0.45, (0, 3, 0)), (0.6, (0, 0, 0)))
tails(run, 0.6, 0.5, 1.0, lift=18)
flames(run, 0.6, 1.6)
A['RUN'] = run

# claw swipe + bite
atk = Anim('attack', 0.8)
add_rot(atk, [
    (0.0, {'right_arm': (0, 0, 0), 'right_fore': (0, 0, 0), 'chest': (0, 0, 0), 'head': (0, 0, 0), 'jaw': (0, 0, 0)}),
    (0.2, {'right_arm': (-95, -20, 25), 'right_fore': (-40, 0, 0), 'chest': (-14, 18, 0), 'head': (-10, -10, 0), 'jaw': (25, 0, 0)}),
    (0.4, {'right_arm': (20, 30, -20), 'right_fore': (10, 0, 0), 'chest': (12, -15, 0), 'head': (18, 8, 0), 'jaw': (0, 0, 0)}),
    (0.55, {'right_arm': (5, 10, -5), 'right_fore': (5, 0, 0), 'chest': (6, -6, 0), 'head': (10, 0, 0), 'jaw': (0, 0, 0)}),
    (0.8, {'right_arm': (0, 0, 0), 'right_fore': (0, 0, 0), 'chest': (0, 0, 0), 'head': (0, 0, 0), 'jaw': (0, 0, 0)}),
])
atk.pos('base', (0, (0, 0, 0)), (0.2, (0, 2, 3)), (0.4, (0, -1, -6)), (0.8, (0, 0, 0)))
A['ATTACK'] = atk

# pounce leap: crouch, launch, airborne tuck, land
leap = Anim('leap', 1.4)
add_rot(leap, [
    (0.0, {'chest': (0, 0, 0), 'body': (0, 0, 0), 'head': (0, 0, 0), 'right_arm': (0, 0, 0), 'left_arm': (0, 0, 0),
           'right_fore': (0, 0, 0), 'left_fore': (0, 0, 0), 'right_thigh': (0, 0, 0), 'left_thigh': (0, 0, 0),
           'right_shin': (0, 0, 0), 'left_shin': (0, 0, 0), 'jaw': (0, 0, 0)}),
    (0.3, {'chest': (14, 0, 0), 'body': (-4, 0, 0), 'head': (-12, 0, 0), 'right_arm': (25, 0, 0), 'left_arm': (25, 0, 0),
           'right_fore': (20, 0, 0), 'left_fore': (20, 0, 0), 'right_thigh': (-25, 0, 0), 'left_thigh': (-25, 0, 0),
           'right_shin': (30, 0, 0), 'left_shin': (30, 0, 0), 'jaw': (10, 0, 0)}),
    (0.45, {'chest': (-25, 0, 0), 'body': (-10, 0, 0), 'head': (20, 0, 0), 'right_arm': (-110, 0, 10), 'left_arm': (-110, 0, -10),
            'right_fore': (-20, 0, 0), 'left_fore': (-20, 0, 0), 'right_thigh': (60, 0, 0), 'left_thigh': (60, 0, 0),
            'right_shin': (-30, 0, 0), 'left_shin': (-30, 0, 0), 'jaw': (35, 0, 0)}),
    (0.9, {'chest': (-15, 0, 0), 'body': (-6, 0, 0), 'head': (10, 0, 0), 'right_arm': (-120, 0, 20), 'left_arm': (-120, 0, -20),
           'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'right_thigh': (50, 0, 0), 'left_thigh': (50, 0, 0),
           'right_shin': (-40, 0, 0), 'left_shin': (-40, 0, 0), 'jaw': (40, 0, 0)}),
    (1.05, {'chest': (18, 0, 0), 'body': (4, 0, 0), 'head': (-8, 0, 0), 'right_arm': (10, 0, 0), 'left_arm': (10, 0, 0),
            'right_fore': (15, 0, 0), 'left_fore': (15, 0, 0), 'right_thigh': (-20, 0, 0), 'left_thigh': (-20, 0, 0),
            'right_shin': (25, 0, 0), 'left_shin': (25, 0, 0), 'jaw': (0, 0, 0)}),
    (1.4, {'chest': (0, 0, 0), 'body': (0, 0, 0), 'head': (0, 0, 0), 'right_arm': (0, 0, 0), 'left_arm': (0, 0, 0),
           'right_fore': (0, 0, 0), 'left_fore': (0, 0, 0), 'right_thigh': (0, 0, 0), 'left_thigh': (0, 0, 0),
           'right_shin': (0, 0, 0), 'left_shin': (0, 0, 0), 'jaw': (0, 0, 0)}),
])
leap.pos('base', (0, (0, 0, 0)), (0.3, (0, -6, 2)), (0.45, (0, 4, -4)), (1.05, (0, -5, 0)), (1.4, (0, 0, 0)))
tails(leap, 1.4, 0.4, 1.0, lift=25)
flames(leap, 1.4, 2.0)
A['LEAP'] = leap

# howl: rear up, head to sky
howl = Anim('howl', 2.6)
add_rot(howl, [
    (0.0, {'chest': (0, 0, 0), 'head': (0, 0, 0), 'neck': (0, 0, 0), 'jaw': (0, 0, 0), 'right_arm': (0, 0, 0), 'left_arm': (0, 0, 0),
           'right_fore': (0, 0, 0), 'left_fore': (0, 0, 0), 'body': (0, 0, 0), 'right_thigh': (0, 0, 0), 'left_thigh': (0, 0, 0)}),
    (0.5, {'chest': (-30, 0, 0), 'head': (-25, 0, 0), 'neck': (-30, 0, 0), 'jaw': (40, 0, 0), 'right_arm': (-30, 0, 15), 'left_arm': (-30, 0, -15),
           'right_fore': (-30, 0, 0), 'left_fore': (-30, 0, 0), 'body': (-12, 0, 0), 'right_thigh': (15, 0, 0), 'left_thigh': (15, 0, 0)}),
    (2.0, {'chest': (-34, 0, 0), 'head': (-30, 0, 0), 'neck': (-34, 0, 0), 'jaw': (45, 0, 0), 'right_arm': (-35, 0, 18), 'left_arm': (-35, 0, -18),
           'right_fore': (-32, 0, 0), 'left_fore': (-32, 0, 0), 'body': (-14, 0, 0), 'right_thigh': (18, 0, 0), 'left_thigh': (18, 0, 0)}),
    (2.6, {'chest': (0, 0, 0), 'head': (0, 0, 0), 'neck': (0, 0, 0), 'jaw': (0, 0, 0), 'right_arm': (0, 0, 0), 'left_arm': (0, 0, 0),
           'right_fore': (0, 0, 0), 'left_fore': (0, 0, 0), 'body': (0, 0, 0), 'right_thigh': (0, 0, 0), 'left_thigh': (0, 0, 0)}),
])
howl.pos('base', (0, (0, 0, 0)), (0.5, (0, 5, 3)), (2.0, (0, 6, 3)), (2.6, (0, 0, 0)))
tails(howl, 2.6, 0.5, 2.0, lift=20)
flames(howl, 2.6, 2.5)
A['HOWL'] = howl

# fissure: rear up on hind legs then smash both claws into the ground
fis = Anim('fissure', 1.6)
add_rot(fis, [
    (0.0, sym({'right_arm': (0, 0, 0), 'right_fore': (0, 0, 0)}) | {'chest': (0, 0, 0), 'body': (0, 0, 0), 'head': (0, 0, 0), 'jaw': (0, 0, 0)}),
    (0.5, sym({'right_arm': (-150, 0, 10), 'right_fore': (-40, 0, 0)}) | {'chest': (-40, 0, 0), 'body': (-25, 0, 0), 'head': (35, 0, 0), 'jaw': (30, 0, 0)}),
    (0.75, sym({'right_arm': (-10, 0, -5), 'right_fore': (10, 0, 0)}) | {'chest': (25, 0, 0), 'body': (8, 0, 0), 'head': (-10, 0, 0), 'jaw': (10, 0, 0)}),
    (1.1, sym({'right_arm': (-5, 0, -5), 'right_fore': (8, 0, 0)}) | {'chest': (20, 0, 0), 'body': (6, 0, 0), 'head': (-8, 0, 0), 'jaw': (10, 0, 0)}),
    (1.6, sym({'right_arm': (0, 0, 0), 'right_fore': (0, 0, 0)}) | {'chest': (0, 0, 0), 'body': (0, 0, 0), 'head': (0, 0, 0), 'jaw': (0, 0, 0)}),
])
fis.pos('base', (0, (0, 0, 0)), (0.5, (0, 14, 8)), (0.75, (0, -4, -4)), (1.1, (0, -3, -3)), (1.6, (0, 0, 0)))
tails(fis, 1.6, 0.6, 1.0, lift=10)
flames(fis, 1.6, 2.0)
A['FISSURE'] = fis

# fireballs: tails whip forward over the back, mouth open
fb = Anim('tail_blast', 1.8)
fb.rot('chest', (0, (0, 0, 0)), (0.4, (10, 0, 0)), (1.4, (10, 0, 0)), (1.8, (0, 0, 0)))
fb.rot('head', (0, (0, 0, 0)), (0.4, (-15, 0, 0)), (1.4, (-15, 0, 0)), (1.8, (0, 0, 0)))
fb.rot('jaw', (0, (0, 0, 0)), (0.4, (30, 0, 0)), (1.4, (30, 0, 0)), (1.8, (0, 0, 0)))
for ti, t in enumerate(('tail_c', 'tail_r', 'tail_l')):
    d = [0.45, 0.7, 0.95][ti]
    fb.rot(f'{t}0', (0, (0, 0, 0)), (0.3, (-25, 0, 0)), (d, (60, 0, 0)), (d + 0.15, (40, 0, 0)), (1.8, (0, 0, 0)))
    fb.rot(f'{t}1', (0, (0, 0, 0)), (0.3, (-20, 0, 0)), (d, (50, 0, 0)), (d + 0.15, (30, 0, 0)), (1.8, (0, 0, 0)))
    fb.rot(f'{t}2', (0, (0, 0, 0)), (0.3, (-10, 0, 0)), (d, (40, 0, 0)), (d + 0.15, (20, 0, 0)), (1.8, (0, 0, 0)))
    fb.scl(f'{t}_flame', (0, (1, 1, 1)), (0.3, (1.8, 2.2, 1.8)), (d, (2.2, 2.6, 2.2)), (d + 0.1, (0.6, 0.6, 0.6)), (1.8, (1, 1, 1)))
A['TAIL_BLAST'] = fb

# frenzy: three rapid alternating claw strikes
fr = Anim('frenzy', 1.2)
add_rot(fr, [
    (0.0, {'right_arm': (0, 0, 0), 'left_arm': (0, 0, 0), 'chest': (0, 0, 0), 'head': (0, 0, 0), 'jaw': (0, 0, 0)}),
    (0.15, {'right_arm': (-100, -20, 30), 'left_arm': (0, 0, 0), 'chest': (-10, 20, 0), 'head': (0, -10, 0), 'jaw': (25, 0, 0)}),
    (0.3, {'right_arm': (15, 25, -20), 'left_arm': (-100, 20, -30), 'chest': (10, -20, 0), 'head': (8, 10, 0), 'jaw': (10, 0, 0)}),
    (0.5, {'right_arm': (-100, -20, 30), 'left_arm': (15, -25, 20), 'chest': (10, 20, 0), 'head': (8, -10, 0), 'jaw': (25, 0, 0)}),
    (0.7, {'right_arm': (-140, 0, 10), 'left_arm': (-140, 0, -10), 'chest': (-25, 0, 0), 'head': (-10, 0, 0), 'jaw': (40, 0, 0)}),
    (0.85, {'right_arm': (10, 0, -5), 'left_arm': (10, 0, 5), 'chest': (22, 0, 0), 'head': (15, 0, 0), 'jaw': (5, 0, 0)}),
    (1.2, {'right_arm': (0, 0, 0), 'left_arm': (0, 0, 0), 'chest': (0, 0, 0), 'head': (0, 0, 0), 'jaw': (0, 0, 0)}),
])
fr.pos('base', (0, (0, 0, 0)), (0.15, (0, 1, -3)), (0.3, (0, 0, -6)), (0.5, (0, 1, -9)), (0.7, (0, 6, -8)), (0.85, (0, -3, -12)), (1.2, (0, 0, 0)))
flames(fr, 1.2, 2.2)
A['FRENZY'] = fr

death = Anim('death', 3.0)
add_rot(death, [
    (0.0, {'chest': (0, 0, 0), 'head': (0, 0, 0), 'jaw': (0, 0, 0), 'base': (0, 0, 0), 'right_arm': (0, 0, 0), 'left_arm': (0, 0, 0)}),
    (0.4, {'chest': (-25, 0, 0), 'head': (-30, 0, 0), 'jaw': (40, 0, 0), 'base': (0, 0, 0), 'right_arm': (-30, 0, 0), 'left_arm': (-30, 0, 0)}),
    (1.3, {'chest': (10, 0, 0), 'head': (20, 0, 10), 'jaw': (20, 0, 0), 'base': (0, 0, 80), 'right_arm': (-20, 0, 40), 'left_arm': (10, 0, -20)}),
    (3.0, {'chest': (10, 0, 0), 'head': (25, 0, 15), 'jaw': (25, 0, 0), 'base': (0, 0, 85), 'right_arm': (-20, 0, 45), 'left_arm': (10, 0, -20)}),
])
death.pos('base', (0, (0, 0, 0)), (0.4, (0, 5, 0)), (1.3, (-4, -18, 0)), (3.0, (-4, -19, 0)))
flames(death, 3.0, 0.5)
names = [f'{t}_flame' for t in ('tail_c', 'tail_r', 'tail_l')] + [w for w, *_ in WISPS]
for n in names:
    death.ch = [c for c in death.ch if not (c[0] == n)]
    death.scl(n, (0, (1, 1, 1)), (1.5, (1, 1, 1)), (3.0, (0.01, 0.01, 0.01)))
A['DEATH'] = death
