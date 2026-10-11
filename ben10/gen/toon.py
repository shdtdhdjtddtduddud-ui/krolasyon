"""Cartoon (Ben 10 style) painting helpers: flat cel colours, ink outlines, glow masks, Omnitrix symbols.
Models are built at 2 texels per vanilla pixel (32 units = 1 block, feet at y=48)."""
import math, numpy as np
from paint import noise2, lerp, border

INK = (18, 18, 22)
FACES = ['top', 'bottom', 'west', 'north', 'east', 'south']


def grid(H, W):
    yy, xx = np.mgrid[0:H, 0:W]
    return yy.astype(np.float32), xx.astype(np.float32)


def base(H, W, col, face, shade=None, r=None, grain=0.05, grad=0.12):
    """flat colour with a gentle vertical cel gradient; top faces lighter, bottom darker"""
    col = np.array(col, np.float32)
    shade = np.array(shade if shade is not None else col * 0.72, np.float32)
    yy, xx = grid(H, W)
    if face == 'top':
        t = np.full((H, W), 0.0)
    elif face == 'bottom':
        t = np.full((H, W), 0.85)
    else:
        t = grad * (yy / max(H - 1, 1))
        if face in ('west', 'east', 'south'):
            t = t + 0.3
    if r is not None and grain:
        t = t + (noise2(H, W, r, 3) - 0.5) * grain
    return lerp(col, shade, np.clip(t, 0, 1))


def outline(col, H, W, ink=INK, w=1, strength=0.9, sides='all'):
    yy, xx = grid(H, W)
    m = np.zeros((H, W), bool)
    if 'all' in sides or 't' in sides:
        m |= yy < w
    if 'all' in sides or 'b' in sides:
        m |= yy >= H - w
    if 'all' in sides or 'l' in sides:
        m |= xx < w
    if 'all' in sides or 'r' in sides:
        m |= xx >= W - w
    col[m] = lerp(col[m], ink, strength)
    return col


def flat(color, shade=None, ink=INK, ow=1, grain=0.04, glow=None, glow_alpha=1.0, sides='all', alpha=1.0, grad=0.12):
    """material factory: flat cartoon colour with ink outline"""
    def fn(c, face, W, H, r):
        col = base(H, W, color, face, shade, r, grain, grad)
        if ow:
            col = outline(col, H, W, ink, ow, 0.85, sides)
        g = None
        if glow is not None:
            g = np.zeros((H, W, 4), np.float32)
            g[..., :3] = col if glow == 'self' else glow
            g[..., 3] = 255 * glow_alpha
        return col, np.full((H, W), alpha), g
    return fn


def gmask(H, W, mask, color, a=1.0):
    g = np.zeros((H, W, 4), np.float32)
    g[..., :3] = color
    g[..., 3] = np.clip(mask, 0, 1) * 255 * a
    return g


def merge_glow(g, g2):
    if g is None:
        return g2
    if g2 is None:
        return g
    a2 = g2[..., 3:4] / 255.0
    out = g.copy()
    out[..., :3] = g[..., :3] * (1 - a2) + g2[..., :3] * a2
    out[..., 3] = np.maximum(g[..., 3], g2[..., 3])
    return out


def ellipse(H, W, cx, cy, rx, ry, ss=3):
    """anti-aliased ellipse coverage"""
    yy, xx = np.mgrid[0:H * ss, 0:W * ss] / ss + 0.5 / ss
    d = ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2
    m = (d <= 1).astype(np.float32)
    return m.reshape(H, ss, W, ss).mean(axis=(1, 3))


def poly(H, W, pts, ss=3):
    """anti-aliased polygon coverage, pts in texel coords"""
    yy, xx = np.mgrid[0:H * ss, 0:W * ss] / ss + 0.5 / ss
    inside = np.zeros(xx.shape, bool)
    n = len(pts)
    for i in range(n):
        x0, y0 = pts[i]
        x1, y1 = pts[(i + 1) % n]
        cond = ((y0 > yy) != (y1 > yy))
        with np.errstate(divide='ignore', invalid='ignore'):
            xi = (x1 - x0) * (yy - y0) / (y1 - y0 + 1e-12) + x0
        inside ^= cond & (xx < xi)
    return inside.astype(np.float32).reshape(H, ss, W, ss).mean(axis=(1, 3))


def band_h(H, W, y0, y1):
    yy, xx = grid(H, W)
    return ((yy >= y0) & (yy < y1)).astype(np.float32)


def band_v(H, W, x0, x1):
    yy, xx = grid(H, W)
    return ((xx >= x0) & (xx < x1)).astype(np.float32)


def paint(col, mask, color):
    m = np.clip(mask, 0, 1)[..., None]
    return col * (1 - m) + np.array(color, np.float32) * m


def omnitrix_symbol(H, W, cx, cy, R, ring=(30, 30, 30), rim=(150, 150, 150), green=(90, 230, 40), dark=(20, 30, 20),
                    ultimate=False):
    """returns (paint list) colour layers for the hourglass symbol: dial ring + green hourglass"""
    layers = []
    layers.append((ellipse(H, W, cx, cy, R, R), rim))
    layers.append((ellipse(H, W, cx, cy, R * 0.86, R * 0.86), ring))
    layers.append((ellipse(H, W, cx, cy, R * 0.66, R * 0.66), green))
    # hourglass: two dark wedges left/right leave green hourglass in the middle
    w = R * 0.66
    left = [(cx - w * 1.1, cy - w * 1.1), (cx - w * 0.18, cy), (cx - w * 1.1, cy + w * 1.1)]
    right = [(cx + w * 1.1, cy - w * 1.1), (cx + w * 0.18, cy), (cx + w * 1.1, cy + w * 1.1)]
    inner = ellipse(H, W, cx, cy, R * 0.66, R * 0.66)
    layers.append((poly(H, W, left) * inner, dark))
    layers.append((poly(H, W, right) * inner, dark))
    if ultimate:
        for a in (45, 135, 225, 315):
            ar = math.radians(a)
            px, py = cx + math.cos(ar) * R * 1.05, cy + math.sin(ar) * R * 1.05
            layers.insert(0, (ellipse(H, W, px, py, R * 0.28, R * 0.28), rim))
    return layers


def apply(col, layers):
    for m, c in layers:
        col = paint(col, m, c)
    return col


def symbol_glow(H, W, cx, cy, R, green=(110, 255, 60)):
    inner = ellipse(H, W, cx, cy, R * 0.66, R * 0.66)
    w = R * 0.66
    left = [(cx - w * 1.1, cy - w * 1.1), (cx - w * 0.18, cy), (cx - w * 1.1, cy + w * 1.1)]
    right = [(cx + w * 1.1, cy - w * 1.1), (cx + w * 0.18, cy), (cx + w * 1.1, cy + w * 1.1)]
    m = inner * (1 - poly(H, W, left)) * (1 - poly(H, W, right))
    return gmask(H, W, m, green, 0.85)


def stroke(H, W, pts, width, ss=3):
    """anti-aliased polyline coverage"""
    yy, xx = np.mgrid[0:H * ss, 0:W * ss] / ss + 0.5 / ss
    m = np.zeros(xx.shape, np.float32)
    for (x0, y0), (x1, y1) in zip(pts[:-1], pts[1:]):
        dx, dy = x1 - x0, y1 - y0
        L2 = dx * dx + dy * dy + 1e-9
        t = np.clip(((xx - x0) * dx + (yy - y0) * dy) / L2, 0, 1)
        d = np.hypot(xx - (x0 + t * dx), yy - (y0 + t * dy))
        m = np.maximum(m, (d <= width / 2).astype(np.float32))
    return m.reshape(H, ss, W, ss).mean(axis=(1, 3))


def voronoi_cracks(H, W, r, cell=4.0, width=0.55, jitter=0.8):
    """tile network like Heatblast's lava rock (jittered-grid voronoi edges): returns crack mask (0..1)"""
    ss = 3
    pts = []
    for gy in range(-1, int(H / cell) + 2):
        for gx in range(-1, int(W / cell) + 2):
            pts.append(((gx + 0.5 + (r.random() - 0.5) * jitter) * cell, (gy + 0.5 + (r.random() - 0.5) * jitter) * cell))
    yy, xx = np.mgrid[0:H * ss, 0:W * ss] / ss + 0.5 / ss
    d = np.stack([np.hypot(xx - p[0], yy - p[1]) for p in pts], 0)
    d.sort(axis=0)
    m = ((d[1] - d[0]) < width).astype(np.float32)
    return m.reshape(H, ss, W, ss).mean(axis=(1, 3))


def box_helpers(M):
    """returns cube helper taking height-above-ground coordinates: hc(bone, x, h_top, z, w, h, d, mat)"""
    def hc(bone, x, htop, z, w, h, d, mat, **kw):
        return M.cube(bone, (x, 48 - htop, z), (w, h, d), mat, **kw)
    return hc
