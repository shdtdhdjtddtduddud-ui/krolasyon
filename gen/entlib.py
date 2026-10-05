"""Entity model helpers: procedural materials, humanoid builder, animation tags -> Java export."""
import math
import numpy as np
from core import Model
from paint import Painter, noise2, lerp, border, glow_img

INK = (10, 8, 14)


def _grid(H, W):
    return np.mgrid[0:H, 0:W]


def vgrad(H, W, face, top=1.0, bot=0.35):
    yy, xx = _grid(H, W)
    g = top + (bot - top) * (yy / max(1, H - 1))
    if face == 'top':
        g = g * 0 + top
    if face == 'bottom':
        g = g * 0 + bot * 0.6
    return g


def side_light(face):
    return {'north': 1.0, 'south': 0.85, 'east': 0.8, 'west': 0.8, 'top': 1.05, 'bottom': 0.6}[face]


def mat_skin(lo, hi, spots=None, dark_edge=0.0):
    def fn(c, face, W, H, r):
        n = noise2(H, W, r, 3)
        t = np.clip(vgrad(H, W, face, 0.95, 0.4) * 0.7 + n * 0.45, 0, 1)
        col = lerp(lo, hi, t)
        if spots is not None:
            m = r.random((H, W)) > 0.9
            col[m] = lerp(col[m], spots, 0.55)
        if dark_edge:
            b = border(H, W)
            col[b] = lerp(col[b], INK, dark_edge)
        return col, np.ones((H, W)), None
    return fn


def mat_plate(lo, hi, seams=True, rivets=True, edge=(14, 12, 18), trim=None, glow=None):
    def fn(c, face, W, H, r):
        yy, xx = _grid(H, W)
        n = noise2(H, W, r, 2)
        t = np.clip(vgrad(H, W, face, 0.95, 0.3) * 0.75 + n * 0.3, 0, 1)
        col = lerp(lo, hi, t)
        if seams and H > 6 and face not in ('top', 'bottom'):
            for y in range(4, H - 2, 5):
                col[y] = lerp(col[y], edge, 0.55)
                if y + 1 < H:
                    col[y + 1] = lerp(col[y + 1], hi, 0.25)
        if rivets and W > 5 and H > 5:
            for (x, y) in ((1, 1), (W - 2, 1), (1, H - 2), (W - 2, H - 2)):
                col[y, x] = lerp(col[y, x], hi, 0.8)
        b = border(H, W)
        col[b] = lerp(col[b], edge, 0.75)
        if trim is not None and face not in ('top', 'bottom') and H > 4:
            col[1] = lerp(col[1], trim, 0.8)
            col[-2] = lerp(col[-2], trim, 0.8)
        g = None
        if glow is not None and r.random() > 0.55 and W > 4 and H > 4:
            x = int(W * (0.3 + 0.4 * r.random()))
            m = np.zeros((H, W), np.float32)
            m[2:H - 2, x] = 1
            col[m > 0] = glow
            g = glow_img(H, W, glow, m)
        return col, np.ones((H, W)), g
    return fn


def mat_cloth(lo, hi, frayed=False, glow=None):
    def fn(c, face, W, H, r):
        yy, xx = _grid(H, W)
        n = noise2(H, W, r, 3)
        folds = 0.5 + 0.5 * np.sin(xx * 1.4 + n * 3 + r.random() * 5)
        t = np.clip(0.3 + 0.5 * folds + (n - 0.5) * 0.3 - 0.2 * (yy / max(1, H - 1)), 0, 1)
        col = lerp(lo, hi, t)
        a = np.ones((H, W))
        if frayed and face in ('north', 'south') and H > 6:
            for x in range(W):
                d = int(r.random() * H * 0.25)
                if d:
                    a[H - d:, x] = 0
        b = border(H, W)
        col[b] = lerp(col[b], INK, 0.5)
        return col, a, None
    return fn


def mat_fur(lo, hi, streak=True):
    def fn(c, face, W, H, r):
        yy, xx = _grid(H, W)
        n = noise2(H, W, r, 2)
        t = np.clip(vgrad(H, W, face, 0.95, 0.35) * 0.6 + n * 0.5, 0, 1)
        col = lerp(lo, hi, t)
        if streak:
            s = r.random((H, W)) > 0.82
            col[s] = lerp(col[s], hi, 0.5)
            s2 = r.random((H, W)) > 0.88
            col[s2] = lerp(col[s2], INK, 0.4)
        return col, np.ones((H, W)), None
    return fn


def mat_scale(lo, hi, edge=(10, 14, 12)):
    def fn(c, face, W, H, r):
        yy, xx = _grid(H, W)
        row = yy // 2
        off = (row % 2) * 2
        cell = ((xx + off) % 4 == 0) | (yy % 2 == 1) & ((xx + off) % 4 == 2)
        t = np.clip(vgrad(H, W, face, 0.95, 0.4) * 0.7 + noise2(H, W, r, 2) * 0.3, 0, 1)
        col = lerp(lo, hi, t)
        col[cell] = lerp(col[cell], edge, 0.5)
        return col, np.ones((H, W)), None
    return fn


def mat_stone(lo, hi, rune=None, crack=(20, 20, 26)):
    def fn(c, face, W, H, r):
        n = noise2(H, W, r, 2)
        col = lerp(lo, hi, np.clip(vgrad(H, W, face, 0.9, 0.4) * 0.5 + n * 0.6, 0, 1))
        k = (r.random((H, W)) > 0.93)
        col[k] = lerp(col[k], crack, 0.7)
        b = border(H, W)
        col[b] = lerp(col[b], crack, 0.6)
        g = None
        if rune is not None and W > 5 and H > 5:
            m = np.zeros((H, W), np.float32)
            cx = W // 2
            m[2:H - 2, cx] = 1
            for yy in range(3, H - 3, 4):
                m[yy, max(1, cx - 2):min(W - 1, cx + 3)] = 1
            col[m > 0] = rune
            g = glow_img(H, W, rune, m)
        return col, np.ones((H, W)), g
    return fn


def mat_chitin(lo, hi, edge=(12, 6, 10), glow=None):
    def fn(c, face, W, H, r):
        yy, xx = _grid(H, W)
        n = noise2(H, W, r, 2)
        t = np.clip(vgrad(H, W, face, 1.0, 0.25) * 0.8 + n * 0.25, 0, 1)
        col = lerp(lo, hi, t)
        # glossy highlight stripe + ridges
        hl = (xx == max(1, W // 4)) & (yy > 1) & (yy < H - 2)
        col[hl] = lerp(col[hl], (255, 255, 255), 0.35)
        if H > 6:
            for y in range(3, H - 1, 4):
                col[y] = lerp(col[y], edge, 0.5)
        b = border(H, W)
        col[b] = lerp(col[b], edge, 0.8)
        g = None
        if glow is not None:
            m = (r.random((H, W)) > 0.96).astype(np.float32)
            col[m > 0] = glow
            g = glow_img(H, W, glow, m)
        return col, np.ones((H, W)), g
    return fn


def mat_glow(color, core=(255, 255, 255)):
    def fn(c, face, W, H, r):
        yy, xx = _grid(H, W)
        d = np.hypot((xx - (W - 1) / 2) / max(1, W / 2), (yy - (H - 1) / 2) / max(1, H / 2))
        col = lerp(core, color, np.clip(d * 1.1, 0, 1))
        g = np.zeros((H, W, 4), np.float32)
        g[..., :3] = col
        g[..., 3] = 255
        return col, np.ones((H, W)), g
    return fn


def mat_flat(color, var=0.08, edge=0.4):
    def fn(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32) + np.array(color, np.float32)
        col *= (1 + (r.random((H, W, 1)) - 0.5) * var * 2) * side_light(face)
        b = border(H, W)
        col[b] *= (1 - edge * 0.5)
        return col, np.ones((H, W)), None
    return fn


def mat_eye(color, white=(255, 255, 255), base=(20, 18, 24)):
    """front-face glowing eye"""
    def fn(c, face, W, H, r):
        yy, xx = _grid(H, W)
        col = np.zeros((H, W, 3), np.float32) + np.array(base, np.float32)
        g = np.zeros((H, W, 4), np.float32)
        if face == 'north':
            col[:] = color
            g[..., :3] = color
            g[..., 3] = 255
            if W >= 3:
                col[:, W // 2] = white
                g[:, W // 2, :3] = white
        return col, np.ones((H, W)), g
    return fn


# -------------------------------------------------------------------- humanoid
def humanoid(M, m_head, m_body, m_arm, m_leg, head=(8, 8, 8), torso=(8, 12, 4), arm=(4, 12, 4), leg=(4, 12, 4),
             gap=0.0, body_mat2=None, shoulder_y=2.0, parent=None):
    """standard humanoid; returns dict of reference coordinates. Bones: head, body, right_arm, left_arm, right_leg, left_leg."""
    tw, th, td = torso
    lh = leg[1]
    y0 = 24 - lh - th     # shoulder line
    M.bone('body', parent, (0, y0 + th, 0))
    M.cube('body', (-tw / 2, y0, -td / 2), torso, m_body)
    M.bone('head', parent, (0, y0, 0))
    M.cube('head', (-head[0] / 2, y0 - head[1], -head[2] / 2), head, m_head)
    for sd, side in ((-1, 'right'), (1, 'left')):
        px = sd * (tw / 2 + arm[0] / 2 + gap)
        M.bone(f'{side}_arm', parent, (px, y0 + shoulder_y, 0))
        M.cube(f'{side}_arm', (px - arm[0] / 2, y0, -arm[2] / 2), arm, m_arm)
        lx = sd * leg[0] / 2 * 1.0
        M.bone(f'{side}_leg', parent, (lx, 24 - lh, 0))
        M.cube(f'{side}_leg', (lx - leg[0] / 2, 24 - lh, -leg[2] / 2), leg, m_leg)
    return dict(y0=y0, tw=tw, th=th, td=td, head=head, arm=arm, leg=leg, gap=gap,
                armx=tw / 2 + arm[0] / 2 + gap)


def add_eyes(M, bone, y, zfront, spread, size, mat, depth=0.4):
    for sd in (-1, 1):
        M.cube(bone, (sd * spread - size[0] / 2, y, zfront - depth), (size[0], size[1], depth), mat)


def mirror(M, bone_r, bone_l, src, fn):
    pass


# -------------------------------------------------------------------- animation tags
WALK, ATTACK, IDLE, WAVE, FLAP, LOOK, CAST, WALKY, LIFTZ = range(9)


class Anims:
    def __init__(self):
        self.rows = []

    def add(self, bone, kind, a=0.0, b=0.0, c=0):
        self.rows.append((bone, kind, a, b, c))

    def humanoid(self, arm_amp=1.0, leg_amp=1.4, attack_arm='right_arm', two_hand=False, idle=0.04):
        self.add('right_leg', WALK, leg_amp, 0.0)
        self.add('left_leg', WALK, leg_amp, math.pi)
        self.add('right_arm', WALK, arm_amp, math.pi)
        self.add('left_arm', WALK, arm_amp, 0.0)
        self.add('right_arm', IDLE, idle, 0.0, 2)
        self.add('left_arm', IDLE, -idle, 0.0, 2)
        self.add('head', LOOK)
        self.add(attack_arm, ATTACK, 1.5, 0.0)
        if two_hand:
            other = 'left_arm' if attack_arm == 'right_arm' else 'right_arm'
            self.add(other, ATTACK, 1.5, 0.0)
        self.add('right_arm', CAST, 1.9, 0.0)
        self.add('left_arm', CAST, 1.9, 0.0)

    def quad(self, front=('leg_fr', 'leg_fl'), back=('leg_br', 'leg_bl'), amp=1.2, head='head'):
        self.add(front[0], WALK, amp, 0.0)
        self.add(front[1], WALK, amp, math.pi)
        self.add(back[0], WALK, amp, math.pi)
        self.add(back[1], WALK, amp, 0.0)
        if head:
            self.add(head, LOOK)

    def java(self, name):
        rows = ",\n            ".join(
            f'new BoneAnim("{b}", {k}, {a:.4f}F, {p:.4f}F, {c})' for b, k, a, p, c in self.rows)
        return f'        MAP.put("{name}", new BoneAnim[] {{\n            {rows}\n        }});'
