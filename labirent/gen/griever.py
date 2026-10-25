"""Griever (Kederci): bloated slug body, clicking mechanical legs, scorpion tail — model, textures, animations."""
import math, numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter, noise2, lerp, border, glow_img, draw_line, cracks

M = Model('griever', 256, 256)
R0 = np.eye(3)

# ---------------- skeleton (y down, ground at y = 24, front = -z) ----------------
M.bone('base', None, (0, 6, 0))
M.bone('body', 'base', (0, 6, 0))
M.cube('body', (-11, -5, -15), (22, 17, 30), 'flesh')
M.cube('body', (-9.5, -7, -16.5), (19, 13, 4), 'flesh_dark')         # neck folds            # main slug body
M.cube('body', (-8, -9, -10), (16, 6, 21), 'flesh')              # hump
M.cube('body', (-9, -2, 12), (18, 13, 10), 'flesh_abdomen')      # swollen abdomen
M.cube('body', (-7, 11.5, -11), (14, 3, 22), 'flesh_dark')       # belly
for z in (-11, -4, 3, 10):
    M.cube('body', (-4, -11.5, z), (8, 3, 5.5), 'metal_plate')    # spine plates
for sx in (-1, 1):
    x = -11.2 if sx < 0 else 9.2
    M.cube('body', (x, -3, -9), (2, 9, 15), 'metal_plate')        # side armour
    M.cube('body', (x - sx * 0.2, 1, 9), (2, 6, 4), 'metal')      # leg mounts

# bony spikes along the back, leaning backwards and out
SPIKES = [(-5, -10, -8, 9, -30, 25), (5, -10, -8, 9, -30, -25), (0, -12, -3, 11, -35, 0), (-6, -9, 2, 8, -45, 30),
          (6, -9, 2, 8, -45, -30), (0, -12, 7, 10, -50, 0), (-7, -6, 13, 7, -55, 35), (7, -6, 13, 7, -55, -35),
          (-3, -8, 16, 6, -60, 15), (3, -8, 16, 6, -60, -15)]
for i, (x, y, z, L, rx, rz) in enumerate(SPIKES):
    n = f'spike{i}'
    M.bone(n, 'body', (x, y, z), (rx, 0, rz))
    M.cube_l(n, (-0.8, -L, -0.8), (1.6, L, 1.6), 'spike')
# wiry bristles (double sided planes)
for i, (x, z, ry) in enumerate([(-6, -6, 20), (6, -6, -20), (-8, 6, 35), (8, 6, -35), (0, 12, 0)]):
    n = f'bristle{i}'
    M.bone(n, 'body', (x, -8, z), (-20, ry, 0))
    M.cube_l(n, (-3, -7, 0), (6, 7, 0), 'bristle', plane=True)

# head with dripping needle tusks
M.bone('head', 'body', (0, 3, -14))
M.cube_l('head', (-6.5, -6, -9), (13, 11, 9), 'head')
M.cube_l('head', (-4.5, -1.5, -12), (9, 6, 3.5), 'flesh_dark')
for sx in (-1, 1):
    n = 'mand_r' if sx < 0 else 'mand_l'
    M.bone(n, 'head', (sx * 3.2, 4, -10), (0, sx * -15, 0))
    M.bones[n].off = [sx * 3.2, 1.0, -10.0]
    M.cube_l(n, (-0.6, 0, -0.6), (1.2, 9, 1.2), 'needle')
    M.cube_l(n, (-1, -1, -1), (2, 2, 2), 'metal')
M.bone('sensor', 'head', (0, -6, -6))
M.bones['sensor'].off = [0.0, -6.0, 3.0]
M.cube_l('sensor', (-0.5, -7, -0.5), (1, 7, 1), 'cable')
M.cube_l('sensor', (-1.2, -9, -1.2), (2.4, 2.4, 2.4), 'eye_bulb')

# six mechanical legs: a level hip bone (for clean animation), a raised thigh, a long shin and a blade
LEGS = []
for k, z in enumerate((-9, 0, 9)):
    for side, sx in (('right', -1), ('left', 1)):
        hip = f'{side}_hip{k}'
        A = (sx * 10.5, 2, z)
        B = (sx * 24, -19, z + (k - 1) * 5)
        C = (sx * 31, 23, z + (k - 1) * 11 - 3)
        M.bone(hip, 'body', A)
        LT = M.seg(f'{side}_thigh{k}', hip, A, B, 4.0, 4.0, 'leg_metal', extend=1.5)
        M.seg(f'{side}_shin{k}', f'{side}_thigh{k}', B, C, 3.0, 3.0, 'leg_metal_light', extend=1)
        M.cube_l(f'{side}_shin{k}', (-0.6, 2, 1.4), (1.2, 14, 1.2), 'cable')
        # knee joint + piston
        M.cube_l(f'{side}_thigh{k}', (-2.6, LT - 2.6, -2.6), (5.2, 5.2, 5.2), 'joint')
        # blade at the foot (front legs get big scythes)
        L = 9 if k == 0 else 5
        M.bone_w(f'{side}_blade{k}', f'{side}_shin{k}', C, rot_zyx(math.radians(-25 if k == 0 else 0), 0, math.radians(sx * 10)))
        M.cube_l(f'{side}_blade{k}', (-0.5, -1, -L), (1, 2.5, L), 'blade')
        LEGS.append((side, k))

# scorpion tail arching over the back
TAIL = [(0, -2, 18), (0, -14, 27), (0, -28, 28), (0, -39, 21), (0, -44, 9), (0, -41, -2)]
W = (6.5, 6, 5.4, 4.8, 4.2)
parent = 'body'
for i in range(5):
    n = f'tail{i}'
    M.seg(n, parent, TAIL[i], TAIL[i + 1], W[i], W[i], 'tail_metal', extend=0.8)
    L = float(np.linalg.norm(np.array(TAIL[i + 1]) - np.array(TAIL[i])))
    M.cube_l(n, (-W[i] / 2 - 0.6, L * 0.35, -W[i] / 2 - 0.6), (W[i] + 1.2, 2.2, W[i] + 1.2), 'metal_plate')   # ring
    M.cube_l(n, (-0.7, 0, W[i] / 2), (1.4, L, 1.2), 'cable')                                              # cable along the back
    parent = n
M.seg('stinger', 'tail4', TAIL[5], (0, -31, -8), 3, 3, 'stinger', extend=0.5)
M.bone_w('stinger_tip', 'stinger', (0, -31, -8), M.world('stinger')[:3, :3])
M.cube_l('stinger_tip', (-0.6, 0, -0.6), (1.2, 6, 1.2), 'needle')

# ---------------- textures ----------------
P = Painter(M)
FLESH_D = (58, 62, 28)
FLESH_M = (122, 128, 54)
FLESH_L = (186, 184, 104)
METAL_D = (22, 23, 26)
METAL_M = (52, 54, 60)
METAL_L = (110, 114, 122)


def flesh(c, face, W, H, r, light=1.0, spots=True, veins=0.8):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 3.5)
    n2 = noise2(H, W, r, 1.5, 2)
    t = np.clip((0.2 + 0.55 * n + 0.25 * n2) * light, 0, 1)
    if face == 'top':
        t = np.clip(t + 0.08, 0, 1)
    if face == 'bottom':
        t *= 0.55
    col = np.where(t[..., None] < 0.5, lerp(FLESH_D, FLESH_M, t * 2), lerp(FLESH_M, FLESH_L, (t - 0.5) * 2))
    if veins > 0:
        cm = cracks(H, W, r, max(1, int(veins * W * H / 70)), length=0.45, branch=0.5)
        col[cm > 0] = lerp(col[cm > 0], (60, 26, 34), 0.55)
        col[cm >= 1] = (88, 34, 44)
    if spots:
        m = r.random((H, W)) < 0.035
        col[m] = (38, 34, 18)
        hi = r.random((H, W)) < 0.03
        col[hi] = lerp(col[hi], (230, 230, 170), 0.6)   # wet highlights
    b = border(H, W)
    col[b] = lerp(col[b], (30, 30, 14), 0.35)
    return col, np.ones((H, W)), None


P.mats['flesh'] = lambda c, f, W, H, r: flesh(c, f, W, H, r)
P.mats['flesh_dark'] = lambda c, f, W, H, r: flesh(c, f, W, H, r, light=0.6, veins=0.4)
P.mats['flesh_abdomen'] = lambda c, f, W, H, r: flesh(c, f, W, H, r, light=1.1, veins=1.4)


def metal(c, face, W, H, r, light=1.0, rivets=True, leds=0.0):
    yy, xx = np.mgrid[0:H, 0:W]
    n = noise2(H, W, r, 2.5)
    scratch = (np.sin(xx * 0.9 + yy * 3.1 + n * 4) > 0.93)
    t = np.clip((0.25 + 0.5 * n) * light, 0, 1)
    if face == 'top':
        t = np.clip(t + 0.15, 0, 1)
    col = np.where(t[..., None] < 0.5, lerp(METAL_D, METAL_M, t * 2), lerp(METAL_M, METAL_L, (t - 0.5) * 2))
    col[scratch] = lerp(col[scratch], (160, 166, 176), 0.5)
    rust = noise2(H, W, r, 2) > 0.72
    col[rust] = lerp(col[rust], (92, 52, 30), 0.5)
    if rivets and W >= 4 and H >= 4:
        for (ry, rx) in ((1, 1), (1, W - 2), (H - 2, 1), (H - 2, W - 2)):
            col[ry, rx] = (170, 176, 184)
    b = border(H, W)
    col[b] = lerp(col[b], (8, 8, 10), 0.6)
    glow = None
    if leds > 0:
        m = (r.random((H, W)) < leds) & ~b
        col[m] = (255, 40, 30)
        glow = glow_img(H, W, (255, 40, 30), m * 1.0)
    return col, np.ones((H, W)), glow


P.mats['metal'] = lambda c, f, W, H, r: metal(c, f, W, H, r)
P.mats['metal_plate'] = lambda c, f, W, H, r: metal(c, f, W, H, r, light=1.15, leds=0.012)
P.mats['leg_metal'] = lambda c, f, W, H, r: metal(c, f, W, H, r, light=0.9, rivets=False)
P.mats['leg_metal_light'] = lambda c, f, W, H, r: metal(c, f, W, H, r, light=1.2, rivets=False)
P.mats['tail_metal'] = lambda c, f, W, H, r: metal(c, f, W, H, r, light=0.85, leds=0.01)
P.mats['joint'] = lambda c, f, W, H, r: metal(c, f, W, H, r, light=1.3, leds=0.05)


@P.mat('cable')
def cable(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    col = lerp((10, 10, 12), (40, 40, 46), 0.5 + 0.5 * np.sin(yy * 1.7))
    return col, np.ones((H, W)), None


@P.mat('spike')
def spike(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    t = yy / max(H - 1, 1)                            # 0 = tip
    col = lerp((210, 200, 170), (40, 36, 30), np.clip(t * 1.4, 0, 1))
    return col, np.ones((H, W)), None


@P.mat('needle')
def needle(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    col = lerp((236, 230, 210), (150, 140, 120), noise2(H, W, r, 2) * 0.6)
    glow = glow_img(H, W, (160, 255, 120), (yy > H * 0.8) * 0.5)
    return col, np.ones((H, W)), glow


@P.mat('blade')
def blade(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    col = lerp((60, 62, 68), (200, 206, 214), np.clip(noise2(H, W, r, 2) * 0.5 + (xx / max(W - 1, 1)) * 0.5, 0, 1))
    col[:, :1] = (20, 20, 24)
    return col, np.ones((H, W)), None


@P.mat('stinger')
def stinger(c, face, W, H, r):
    col, a, g = metal(c, face, W, H, r, light=0.8)
    yy, xx = np.mgrid[0:H, 0:W]
    m = (yy > H * 0.5)
    col[m] = lerp(col[m], (90, 120, 40), 0.6)
    g = glow_img(H, W, (150, 255, 90), m * (0.25 + 0.25 * (r.random((H, W)) < 0.3)))
    return col, a, g


@P.mat('bristle')
def bristle(c, face, W, H, r):
    yy, xx = np.mgrid[0:H, 0:W]
    alpha = np.zeros((H, W))
    for k in range(W * 2):
        x0 = r.random() * W
        lean = (r.random() - 0.5) * 1.6
        top = int(r.random() * H * 0.6)
        for y in range(top, H):
            x = int(x0 + lean * (y - top) / max(H, 1))
            if 0 <= x < W:
                alpha[y, x] = 1
    col = np.zeros((H, W, 3)) + (30, 26, 18)
    return col, alpha, None


@P.mat('eye_bulb')
def eye_bulb(c, face, W, H, r):
    col = np.zeros((H, W, 3)) + (255, 50, 30)
    return col, np.ones((H, W)), glow_img(H, W, (255, 60, 40), np.ones((H, W)))


@P.mat('head')
def head(c, face, W, H, r):
    col, a, g = flesh(c, face, W, H, r, light=0.95, veins=0.6)
    g = np.zeros((H, W, 4), np.float32)
    if face == 'north':
        yy, xx = np.mgrid[0:H, 0:W]
        # cluster of small red eyes
        for (ex, ey) in ((2.5, 2.5), (W - 3.5, 2.5), (4.5, 1.5), (W - 5.5, 1.5), (W / 2 - 0.5, 3.5)):
            d = np.hypot(xx - ex, yy - ey)
            col[d < 1.3] = (20, 4, 4)
            eye = d < 0.8
            col[eye] = (255, 40, 30)
            g = np.maximum(g, glow_img(H, W, (255, 50, 30), eye * 1.0))
        # wet mouth slit
        mouth = (yy >= H - 4) & (np.abs(xx - W / 2 + 0.5) < W * 0.32)
        col[mouth] = lerp((60, 10, 14), (120, 30, 30), noise2(H, W, r, 2))[mouth]
        teeth = mouth & (xx % 2 == 0) & (yy == H - 4)
        col[teeth] = (230, 220, 200)
    return col, a, g


# ---------------- animations ----------------
A = {}
HIPS = [f'{s}_hip{k}' for s in ('right', 'left') for k in range(3)]
TAILS = [f'tail{i}' for i in range(5)] + ['stinger']


def leg_cycle(a, length, swing=16, lift=22, offset_groups=True):
    """tripod gait on the hip bones: (right0,left1,right2) vs (left0,right1,left2)"""
    for side in ('right', 'left'):
        sgn = -1 if side == 'right' else 1
        for k in range(3):
            grp = (k + (0 if side == 'right' else 1)) % 2
            ph = 0.5 * grp if offset_groups else 0
            keys = []
            for step in range(9):
                t = step / 8
                u = (t + ph) % 1.0
                # forward swing while lifted in the first half, push back on the ground in the second
                if u < 0.5:
                    y = swing * math.cos(u / 0.5 * math.pi)          # back -> front
                    z = lift * math.sin(u / 0.5 * math.pi)
                else:
                    y = -swing * math.cos((u - 0.5) / 0.5 * math.pi)  # front -> back
                    z = 0
                keys.append((t * length, (0, sgn * -y, -sgn * z)))
            a.rot(f'{side}_hip{k}', *keys)


def tail_sway(a, length, amp=6, sway=6, n=4):
    for i, t in enumerate(TAILS):
        keys = []
        for s in range(n + 1):
            tt = s / n * length
            ph = s / n * math.tau - i * 0.6
            keys.append((tt, (amp * math.sin(ph) * (0.5 + i * 0.12), 0, sway * math.cos(ph) * (0.3 + i * 0.1))))
        a.rot(t, *keys)


idle = Anim('idle', 3.0, loop=True)
idle.scl('body', (0, (1, 1, 1)), (1.5, (1.04, 1.06, 1.02)), (3.0, (1, 1, 1)))
idle.rot('head', (0, (0, 0, 0)), (1.0, (6, 8, 0)), (2.0, (-4, -6, 0)), (3.0, (0, 0, 0)))
idle.rot('mand_r', (0, (0, 0, 0)), (0.4, (0, -20, 0)), (0.8, (0, 0, 0)), (2.2, (0, 0, 0)), (2.5, (0, -15, 0)), (3.0, (0, 0, 0)))
idle.rot('mand_l', (0, (0, 0, 0)), (0.4, (0, 20, 0)), (0.8, (0, 0, 0)), (2.2, (0, 0, 0)), (2.5, (0, 15, 0)), (3.0, (0, 0, 0)))
idle.rot('sensor', (0, (0, 0, 0)), (0.75, (15, 0, 10)), (1.5, (0, 0, -10)), (2.25, (-10, 0, 0)), (3.0, (0, 0, 0)))
for hp in HIPS:
    d = 2 if hp.startswith('right') else -2
    idle.rot(hp, (0, (0, 0, 0)), (1.5, (0, 0, d)), (3.0, (0, 0, 0)))
tail_sway(idle, 3.0)
A['IDLE'] = idle

walk = Anim('walk', 1.0, loop=True)
leg_cycle(walk, 1.0)
walk.pos('body', (0, (0, 0, 0)), (0.25, (0, 0.8, 0)), (0.5, (0, 0, 0)), (0.75, (0, 0.8, 0)), (1.0, (0, 0, 0)))
walk.rot('body', (0, (0, 0, 2)), (0.5, (0, 0, -2)), (1.0, (0, 0, 2)))
walk.rot('head', (0, (0, -6, 0)), (0.5, (0, 6, 0)), (1.0, (0, -6, 0)))
tail_sway(walk, 1.0, amp=4, sway=8, n=4)
A['WALK'] = walk

run = Anim('run', 0.6, loop=True)
leg_cycle(run, 0.6, swing=24, lift=30)
run.rot('body', (0, (4, 0, 3)), (0.3, (4, 0, -3)), (0.6, (4, 0, 3)))
run.pos('body', (0, (0, 0, 0)), (0.15, (0, 1.5, 0)), (0.3, (0, 0, 0)), (0.45, (0, 1.5, 0)), (0.6, (0, 0, 0)))
tail_sway(run, 0.6, amp=8, sway=4, n=4)
A['RUN'] = run

# claw strike with both scythe legs
claw = Anim('claw', 0.8)
claw.rot('right_hip0', (0, (0, 0, 0)), (0.3, (0, -35, 55)), (0.45, (0, -40, -10)), (0.8, (0, 0, 0)))
claw.rot('left_hip0', (0, (0, 0, 0)), (0.3, (0, 35, -55)), (0.45, (0, 40, 10)), (0.8, (0, 0, 0)))
claw.rot('body', (0, (0, 0, 0)), (0.3, (-12, 0, 0)), (0.45, (10, 0, 0)), (0.8, (0, 0, 0)))
claw.pos('body', (0, (0, 0, 0)), (0.3, (0, 2, 2)), (0.45, (0, -1, -4)), (0.8, (0, 0, 0)))
claw.rot('head', (0, (0, 0, 0)), (0.3, (-20, 0, 0)), (0.45, (15, 0, 0)), (0.8, (0, 0, 0)))
claw.rot('mand_r', (0, (0, 0, 0)), (0.3, (0, -30, 0)), (0.45, (0, 5, 0)), (0.8, (0, 0, 0)))
claw.rot('mand_l', (0, (0, 0, 0)), (0.3, (0, 30, 0)), (0.45, (0, -5, 0)), (0.8, (0, 0, 0)))
A['CLAW'] = claw

# sting: tail rears back, then whips over the head
sting = Anim('sting', 1.2)
coil = {'tail0': 14, 'tail1': 12, 'tail2': 8, 'tail3': 6, 'tail4': 4, 'stinger': 8}
strike = {'tail0': -26, 'tail1': -22, 'tail2': -16, 'tail3': -14, 'tail4': -10, 'stinger': -30}
for t in TAILS:
    sting.rot(t, (0, (0, 0, 0)), (0.35, (coil[t], 0, 0)), (0.55, (coil[t] * 1.1, 0, 0)), (0.68, (strike[t], 0, 0)),
              (0.9, (strike[t] * 0.8, 0, 0)), (1.2, (0, 0, 0)))
sting.rot('body', (0, (0, 0, 0)), (0.5, (8, 0, 0)), (0.68, (-10, 0, 0)), (1.2, (0, 0, 0)))
sting.pos('body', (0, (0, 0, 0)), (0.5, (0, 1, 3)), (0.68, (0, -1, -3)), (1.2, (0, 0, 0)))
A['STING'] = sting

# roll: curls up and tumbles forward like the book's Grievers
roll = Anim('roll', 1.0)
roll.rot('base', (0, (0, 0, 0), 'L'), (0.2, (0, 0, 0), 'L'), (0.4, (-120, 0, 0), 'L'), (0.6, (-240, 0, 0), 'L'), (0.8, (-360, 0, 0), 'L'),
         (1.0, (-360, 0, 0), 'L'))
roll.pos('base', (0, (0, 0, 0)), (0.2, (0, 4, 0)), (0.5, (0, 10, -8)), (0.8, (0, 4, -4)), (1.0, (0, 0, 0)))
for hp in HIPS:
    s = 1 if hp.startswith('right') else -1
    roll.rot(hp, (0, (0, 0, 0)), (0.2, (0, 0, s * 70)), (0.8, (0, 0, s * 70)), (1.0, (0, 0, 0)))
for t in TAILS:
    roll.rot(t, (0, (0, 0, 0)), (0.2, (25, 0, 0)), (0.8, (25, 0, 0)), (1.0, (0, 0, 0)))
A['ROLL'] = roll

death = Anim('death', 2.5)
for hp in HIPS:
    s = 1 if hp.startswith('right') else -1
    death.rot(hp, (0, (0, 0, 0)), (0.3, (0, 0, s * 40)), (1.2, (0, 0, -s * 35)), (2.5, (0, 0, -s * 38)))
death.pos('base', (0, (0, 0, 0)), (0.3, (0, -3, 0)), (1.2, (0, -10, 0)), (2.5, (0, -11, 0)))
death.rot('base', (0, (0, 0, 0)), (0.3, (-10, 0, 0)), (1.2, (6, 0, 12)), (2.5, (6, 0, 14)))
for i, t in enumerate(TAILS):
    death.rot(t, (0, (0, 0, 0)), (0.4, (-15, 0, 10)), (1.4, (40, 0, 20)), (2.5, (45, 0, 22)))
death.rot('head', (0, (0, 0, 0)), (0.3, (-30, 0, 0)), (1.2, (25, 0, 10)), (2.5, (28, 0, 12)))
A['DEATH'] = death
