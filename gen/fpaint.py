"""Paint helpers shared by the fire / shadow transformation forms."""
import math, numpy as np
from paint import Painter, noise2, lerp, border, glow_img, draw_line


def ramp3(t, lo, mid, hi):
    t = np.clip(t, 0, 1)
    return np.where(t[..., None] < 0.5, lerp(lo, mid, t * 2), lerp(mid, hi, (t - 0.5) * 2))


def face_shade(face, t):
    if face == 'top':
        return np.clip(t + 0.12, 0, 1)
    if face == 'bottom':
        return t * 0.55
    return t


def outline(col, H, W, ink, k=0.5):
    b = border(H, W)
    col[b] = lerp(col[b], ink, k)
    return col


def organic(H, W, r, scale=4.0, thr=0.55):
    n = noise2(H, W, r, scale, 2)
    return n > thr


def edge_of(mask):
    e = np.zeros_like(mask)
    e[1:] |= mask[:-1] & ~mask[1:]; e[:-1] |= mask[1:] & ~mask[:-1]
    e[:, 1:] |= mask[:, :-1] & ~mask[:, 1:]; e[:, :-1] |= mask[:, 1:] & ~mask[:, :-1]
    return e


def solid(pal, ink, noise=0.45, vgrad=0.25, streak=0.0, scale=4):
    """shaded solid material from a (lo, mid, hi) palette"""
    def fn(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, scale)
        t = 0.5 + (n - 0.5) * noise - vgrad * (yy / max(H - 1, 1) - 0.5)
        if streak:
            wav = noise2(H, W, r, 7, 1)
            st = 0.5 + 0.5 * np.sin(xx * 1.3 + wav * 6)
            t = t + (st > 0.9) * streak
        t = face_shade(face, t)
        col = ramp3(t, *pal)
        outline(col, H, W, ink)
        return col, np.ones((H, W)), None
    return fn


def veins(base_fn, color, glow_color, density=2, glow=0.8, faces=None):
    """base material with glowing crack veins (lava / flame tattoos)"""
    from paint import cracks

    def fn(c, face, W, H, r):
        col, a, g = base_fn(c, face, W, H, r)
        if faces is not None and face not in faces:
            return col, a, g
        m = cracks(H, W, r, density, 0.5)
        col = col.copy()
        col[m >= 1] = color
        col[(m > 0) & (m < 1)] = lerp(col[(m > 0) & (m < 1)], color, 0.45)
        gl = glow_img(H, W, glow_color, (m >= 1) * glow + ((m > 0) & (m < 1)) * glow * 0.3)
        return col, a, gl
    return fn


def pattern(base_fn, pal2, scale=4.0, thr=0.56, ink=None, faces=None):
    """overlay organic patches of a second palette (armour ornament, tattoos)"""
    def fn(c, face, W, H, r):
        col, a, g = base_fn(c, face, W, H, r)
        if faces is not None and face not in faces:
            return col, a, g
        m = organic(H, W, r, scale, thr)
        yy = np.mgrid[0:H, 0:W][0]
        col2 = ramp3(0.6 - 0.3 * yy / max(H - 1, 1) + noise2(H, W, r, 5) * 0.2 - 0.1, *pal2)
        col = col.copy()
        col[m] = col2[m]
        if ink is not None:
            e = edge_of(m)
            col[e] = lerp(col[e], ink, 0.6)
        return col, a, g
    return fn


def flame_shape(H, W, r, start=0.35, tongues=3, jag=1.0):
    """alpha mask of a flame-tongue edge at the bottom of a plane (v grows downward)"""
    yy, xx = np.mgrid[0:H, 0:W]
    v = yy / max(H - 1, 1)
    u = xx / max(W - 1, 1)
    prof = start + (1 - start) * (0.5 + 0.5 * np.cos(u * math.tau * tongues / 2 + r.random() * 6)) ** 1.5
    prof = prof + (noise2(H, W, r, 2, 1) - 0.5) * 0.18 * jag
    return v <= prof, prof


def fire_cloth(pal, flame_pal, ink, tongues=2, start=0.55, glow=0.5, top=None):
    """robe / scarf panel: base palette with flame patterned lower half and flame-tongue cut edge"""
    def fn(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        v = yy / max(H - 1, 1)
        n = noise2(H, W, r, 4)
        folds = 0.5 + 0.5 * np.sin(xx * 1.1 + noise2(H, W, r, 6, 1) * 4)
        col = ramp3(0.35 + 0.35 * folds + (n - 0.5) * 0.3, *pal)
        # flames licking up from the hem
        fl = noise2(H, W, r, 3, 2)
        flame = (v + fl * 0.5) > (0.75 if top is None else top)
        heat = np.clip((v + fl * 0.5 - 0.75) * 3, 0, 1)
        fc = ramp3(heat, *flame_pal)
        col[flame] = fc[flame]
        a = np.ones((H, W))
        gl = glow_img(H, W, flame_pal[2], flame * heat * glow)
        if face in ('north', 'south'):
            m, prof = flame_shape(H, W, r, start, tongues)
            a = m.astype(float)
            e = m & ~np.roll(m, -1, 0)
            col[e] = flame_pal[2]
        trim = (xx == 0) | (xx == W - 1)
        col[trim] = lerp(col[trim], ink, 0.4)
        return col, a, gl
    return fn


def flame_mat(pal=((150, 20, 10), (255, 110, 20), (255, 235, 140)), glow=0.85, tongues=2):
    """pure flame plane (hair tips, fire wisps, burning parts)"""
    def fn(c, face, W, H, r):
        S = 3
        Hs, Ws = H * S, W * S
        yy, xx = np.mgrid[0:Hs, 0:Ws] / S
        v = yy / H
        u = (xx - W / 2) / (W / 2)
        n = noise2(Hs, Ws, r, 3 * S, 3)
        width = (0.95 - 0.8 * v ** 1.3) + (n - 0.5) * 0.5 + 0.15 * np.sin(v * 9 + u * 3)
        inside = np.abs(u) < width
        heat = np.clip(1 - np.abs(u) / np.maximum(width, 0.05), 0, 1) * (1 - v * 0.5)
        col = ramp3(heat, *pal)
        col = col.reshape(H, S, W, S, 3).mean((1, 3))
        a = (inside.astype(float).reshape(H, S, W, S).mean((1, 3)) > 0.4).astype(float)
        if face not in ('north', 'south'):
            a = np.ones((H, W)) * (face != 'top')
            col = ramp3(np.full((H, W), 0.6), *pal)
        h = heat.reshape(H, S, W, S).mean((1, 3))
        return col, a, glow_img(H, W, pal[2], a * (0.4 + 0.6 * h) * glow)
    return fn


def emissive(color, glow_color=None, glow=1.0, hi=(255, 255, 230)):
    def fn(c, face, W, H, r):
        yy = np.mgrid[0:H, 0:W][0]
        col = lerp(color, hi, 0.25 * (1 - yy / max(H - 1, 1)))
        return col, np.ones((H, W)), glow_img(H, W, glow_color or color, np.ones((H, W)) * glow)
    return fn


def eyes_face(skin_fn, eye=(255, 230, 120), glow=(255, 200, 80), mark=None, slant=1, mask=None, ink=(20, 8, 8)):
    """face with glowing slanted eyes (optionally a forehead mark or a full mask colour)"""
    def fn(c, face, W, H, r):
        col, a, g = skin_fn(c, face, W, H, r)
        if face != 'north':
            return col, a, g
        col = col.copy()
        if mask is not None:
            col[:] = mask
            col[0, :] = lerp(mask, (255, 255, 255), 0.2)
        m = np.zeros((H, W))
        ey = max(1, H // 2 - 1)
        draw_line(m, 0, ey - slant, W // 2 - 1, ey)
        draw_line(m, W - 1, ey - slant, W // 2, ey)
        col[m > 0] = eye
        if mark is not None:
            col[0:max(1, ey - 1), W // 2 - 1:W // 2 + 1] = mark
        col[H - 1, 1:W - 1] = lerp(col[H - 1, 1:W - 1], ink, 0.5)
        return col, a, glow_img(H, W, glow, m)
    return fn


def painter_with(M, mats):
    P = Painter(M)
    P.mats.update(mats)
    return P
