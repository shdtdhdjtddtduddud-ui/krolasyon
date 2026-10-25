"""Beetle blade: WICKED's little mechanical spy lizard with a red eye."""
import math, numpy as np
from core import Model, Anim
from paint import Painter, noise2, lerp, border, glow_img

M = Model('beetle_blade', 64, 64)
M.bone('body', None, (0, 20, 0))
M.cube('body', (-2.5, 18, -5), (5, 3, 10), 'shell')
M.cube('body', (-1.5, 17.2, -3.5), (3, 1, 7), 'stripe')
M.bone('head', 'body', (0, 19.5, -5))
M.cube('head', (-2, 18, -8.5), (4, 2.5, 3.5), 'head')
M.bone('tail', 'body', (0, 19.5, 5))
M.cube('tail', (-1, 18.8, 5), (2, 1.5, 6), 'shell')
M.bone('tail2', 'tail', (0, 19.5, 11))
M.cube('tail2', (-0.5, 19.1, 11), (1, 1, 5), 'shell')
for k, z in enumerate((-3.5, 0, 3.5)):
    for side, sx in (('right', -1), ('left', 1)):
        n = f'{side}_leg{k}'
        M.bone(n, 'body', (sx * 2.5, 20, z))
        x = sx * 2.5 if sx > 0 else sx * 2.5 - 4
        M.cube(n, (x, 19.5, z - 0.5), (4, 1, 1), 'leg')
        M.cube(n, (sx * 6 - 0.5, 19.5, z - 0.5), (1, 4.5, 1), 'leg')

P = Painter(M)


@P.mat('shell')
def shell(c, face, W, H, r):
    n = noise2(H, W, r, 2)
    col = lerp((58, 60, 68), (150, 156, 166), n)
    col[border(H, W)] = (14, 14, 18)
    return col, np.ones((H, W)), None


@P.mat('stripe')
def stripe(c, face, W, H, r):
    col = np.zeros((H, W, 3)) + (150, 20, 20)
    yy, xx = np.mgrid[0:H, 0:W]
    col[(yy + xx) % 3 == 0] = (230, 230, 230)
    return col, np.ones((H, W)), None


@P.mat('leg')
def leg(c, face, W, H, r):
    return lerp((20, 20, 24), (80, 84, 92), noise2(H, W, r, 1.5)), np.ones((H, W)), None


@P.mat('head')
def head(c, face, W, H, r):
    col, a, g = shell(c, face, W, H, r)
    g = None
    if face == 'north':
        yy, xx = np.mgrid[0:H, 0:W]
        eye = (np.abs(xx - (W - 1) / 2) < 1) & (yy == 1)
        col[eye] = (255, 30, 30)
        g = glow_img(H, W, (255, 30, 30), eye * 1.0)
    return col, a, g


A = {}
walk = Anim('walk', 0.4, loop=True)
for k in range(3):
    for side, sgn in (('right', -1), ('left', 1)):
        ph = 0.0 if (k + (side == 'left')) % 2 == 0 else 0.5
        keys = []
        for st in range(5):
            u = (st / 4 + ph) * math.tau
            keys.append((st * 0.1, (0, sgn * 25 * math.cos(u), -sgn * max(0.0, 20 * math.sin(u)))))
        walk.rot(f'{side}_leg{k}', *keys)
walk.rot('tail', (0, (0, 15, 0)), (0.2, (0, -15, 0)), (0.4, (0, 15, 0)))
walk.rot('tail2', (0, (0, 20, 0)), (0.2, (0, -20, 0)), (0.4, (0, 20, 0)))
A['WALK'] = walk
idle = Anim('idle', 2.0, loop=True)
idle.rot('head', (0, (0, 0, 0)), (0.5, (0, 30, 0)), (1.0, (-10, 0, 0)), (1.5, (0, -30, 0)), (2.0, (0, 0, 0)))
idle.rot('tail', (0, (0, 10, 0)), (1.0, (0, -10, 0)), (2.0, (0, 10, 0)))
A['IDLE'] = idle
