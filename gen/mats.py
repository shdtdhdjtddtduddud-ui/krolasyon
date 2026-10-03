"""Material kit: hand-made-looking pixel-art style surfaces for box-UV cubes.

Every factory returns a function  f(cube, face, W, H, rng) -> (rgb[H,W,3], alpha[H,W], glow[H,W,4] | None)
that plugs into paint.Painter.mats.  Surfaces use palette *ramps* (dark -> light) with ordered dithering,
directional face lighting, edge occlusion and procedural detail (scales, plates, cracks, veins, weave...).
"""
import math
import numpy as np
from paint import noise2, lerp, border, glow_img, draw_line

BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]], np.float32) / 16.0 + 1 / 32.0

# directional light offsets per face (top lit, bottom shadowed)
FACE_L = {'top': 0.20, 'north': 0.04, 'west': -0.02, 'east': -0.10, 'south': -0.14, 'bottom': -0.28}


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def P(*hexes):
    return [hexc(h) if isinstance(h, str) else tuple(h) for h in hexes]


def ramp(t, pal, dither=True):
    """t: HxW in [0,1] -> HxWx3 using palette pal (list of rgb), ordered dithering between steps"""
    H, W = t.shape
    n = len(pal)
    x = np.clip(t, 0, 0.9999) * (n - 1)
    lo = np.floor(x).astype(int)
    fr = x - lo
    if dither:
        bay = np.tile(BAYER, (H // 4 + 1, W // 4 + 1))[:H, :W]
        # sharpen: only the middle of the transition dithers
        pick = lo + (np.clip((fr - 0.25) * 2.0, 0, 1) > bay)
    else:
        pick = lo + (fr > 0.5)
    pick = np.clip(pick, 0, n - 1)
    arr = np.array(pal, np.float32)
    return arr[pick]


def lightmap(face, H, W, vgrad=0.16, ao=0.10):
    yy, xx = np.mgrid[0:H, 0:W]
    t = np.full((H, W), 0.5, np.float32) + FACE_L.get(face, 0)
    if face not in ('top', 'bottom'):
        t = t + vgrad * (0.5 - yy / max(H - 1, 1))
    if ao:
        b = border(H, W)
        t = t - ao * b
        if H > 4 and W > 4:
            b2 = border(H, W, 2) & ~b
            t = t - ao * 0.4 * b2
    return t


def edge_hl(t, face, H, W, amt=0.12):
    """bevel: lighter top/left rim for side faces"""
    if H > 3 and W > 3 and face not in ('bottom',):
        t = t.copy()
        t[1, 1:W - 1] += amt
        t[1:H - 1, 1] += amt * 0.5
    return t


def finish(pal, t, alpha=None, glow=None, dither=True):
    H, W = t.shape
    col = ramp(t, pal, dither)
    return col, (np.ones((H, W)) if alpha is None else alpha), glow


def voronoi(H, W, r, cell, jitter=0.9):
    """returns (cell_id grid, distance to nearest seed, distance gap to 2nd nearest) in pixel units"""
    gh, gw = int(H / cell) + 3, int(W / cell) + 3
    sy = (np.arange(gh)[:, None] + 0.5 + (r.random((gh, gw)) - 0.5) * jitter) * cell - cell
    sx = (np.arange(gw)[None, :] + 0.5 + (r.random((gh, gw)) - 0.5) * jitter) * cell - cell
    yy, xx = np.mgrid[0:H, 0:W] + 0.5
    best = np.full((H, W), 1e9, np.float32)
    second = np.full((H, W), 1e9, np.float32)
    ids = np.zeros((H, W), np.int32)
    for i in range(gh):
        for j in range(gw):
            d = np.hypot(yy - sy[i, j], xx - sx[i, j])
            m = d < best
            second = np.where(m, best, np.minimum(second, d))
            ids = np.where(m, i * 100 + j, ids)
            best = np.where(m, d, best)
    return ids, best, second - best


def rand_by_id(ids, r):
    u = np.unique(ids)
    vals = r.random(len(u))
    lut = dict(zip(u.tolist(), vals.tolist()))
    return np.vectorize(lambda k: lut[k])(ids).astype(np.float32)


def stretch_noise(H, W, r, sx, sy, oct=2):
    """noise elongated along y (fur / muscle fibres) when sy>sx"""
    big = noise2(int(H * sy) + 2, int(W * sx) + 2, r, 3, oct)
    ys = (np.arange(H) * sy).astype(int)
    xs = (np.arange(W) * sx).astype(int)
    return big[ys][:, xs]


# ------------------------------------------------------------------ surfaces
def skin(pal, contrast=0.35, veins=None, spots=0.0, pores=0.0, scar=0.0, vgrad=0.18):
    def f(c, face, W, H, r):
        t = lightmap(face, H, W, vgrad)
        n = noise2(H, W, r, 3.0)
        t = t + (n - 0.5) * contrast
        if pores:
            t = t - (r.random((H, W)) < pores) * 0.18
        if spots:
            m = noise2(H, W, r, 2.0, 2) > (1 - spots)
            t = t - m * 0.2
        if scar:
            m = np.zeros((H, W))
            for _ in range(int(scar * W * H / 80) + (1 if scar > 0 else 0)):
                x0, y0 = r.random() * W, r.random() * H
                a = r.random() * math.tau
                L = 2 + r.random() * 5
                draw_line(m, x0, y0, x0 + math.cos(a) * L, y0 + math.sin(a) * L)
            t = t - m * 0.3
        glow = None
        if veins is not None:
            from paint import cracks
            vm = cracks(H, W, r, max(1, W * H // 150), 0.5, 0.4)
            vm = np.where(vm > 0.7, 1.0, vm * 0.0)
            t = t - vm * 0.25
            glow = glow_img(H, W, veins, vm * 0.8)
        t = edge_hl(t, face, H, W)
        col, a, _ = finish(pal, t)
        return col, a, glow
    return f


def scales(pal, size=3.0, glow_edge=None, shine=0.2, vgrad=0.16):
    def f(c, face, W, H, r):
        ids, d, gap = voronoi(H, W, r, size, 0.55)
        rv = rand_by_id(ids, r)
        t = lightmap(face, H, W, vgrad, 0.06)
        # each scale: lit at its top, dark on its rim
        t = t + (rv - 0.5) * 0.28 - np.clip(1.0 - gap / 1.2, 0, 1) * 0.35 + (np.clip(d / size, 0, 1) < 0.35) * shine * 0.5
        t = edge_hl(t, face, H, W)
        glow = None
        if glow_edge is not None:
            gm = np.clip(1.0 - gap / 1.0, 0, 1) * (rv > 0.55)
            glow = glow_img(H, W, glow_edge, gm * 0.7)
        col, a, _ = finish(pal, t)
        return col, a, glow
    return f


def plates(pal, rivets=True, band=5, seam=0.32, rust=0.15, glow_seam=None, vgrad=0.2):
    """layered metal / chitin armour: horizontal overlapping plates with bevels and rivets"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        t = lightmap(face, H, W, vgrad, 0.12)
        if face in ('top', 'bottom'):
            t = t + (noise2(H, W, r, 3) - 0.5) * 0.2
        else:
            b = max(3, int(band))
            row = yy % b
            plate_id = yy // b
            off = (r.random(H // b + 2) * 3).astype(int)[plate_id]
            # plate gradient: bright upper bevel -> dark lower lip
            t = t + (0.18 - row / b * 0.34)
            t = t - (row == b - 1) * seam
            # vertical seams, staggered
            vs = ((xx + off) % max(6, int(b * 2.4)) == 0)
            t = t - vs * seam * 0.7
            if rivets and W > 6 and H > 5:
                rv = (((xx + off) % max(6, int(b * 2.4))) == 2) & (row == 1)
                t = t + rv * 0.28
        t = t + (noise2(H, W, r, 2.5) - 0.5) * rust
        t = edge_hl(t, face, H, W, 0.14)
        glow = None
        if glow_seam is not None and face not in ('top', 'bottom') and H > 4:
            gm = ((yy % max(3, int(band))) == max(3, int(band)) - 1) * 0.35 * (noise2(H, W, r, 2) > 0.6)
            glow = glow_img(H, W, glow_seam, gm)
        col, a, _ = finish(pal, t)
        return col, a, glow
    return f


def bone(pal, cracks_n=0.4, marrow=None):
    def f(c, face, W, H, r):
        t = lightmap(face, H, W, 0.2, 0.12)
        ridge = stretch_noise(H, W, r, 1.0, 3.0) if H > 3 else noise2(H, W, r, 2)
        t = t + (ridge - 0.5) * 0.4 + (noise2(H, W, r, 1.7) - 0.5) * 0.12
        m = np.zeros((H, W))
        for _ in range(int(cracks_n * (W + H) / 6) + 1):
            x0, y0 = r.random() * W, r.random() * H
            a = math.pi / 2 + r.normal(0, 0.7)
            L = 2 + r.random() * 5
            draw_line(m, x0, y0, x0 + math.cos(a) * L, y0 + math.sin(a) * L)
        t = t - m * 0.32
        t = edge_hl(t, face, H, W)
        col, a, _ = finish(pal, t)
        glow = None
        if marrow is not None and (r.random() < 0.0):
            glow = None
        return col, a, glow
    return f


def cloth(pal, folds=0.6, weave=True, hem=None, stripe=None, vgrad=0.24, trim=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        t = lightmap(face, H, W, vgrad, 0.08)
        n = noise2(H, W, r, 3)
        ph = r.random() * 6
        fold = np.sin(xx * (1.0 + r.random() * 0.5) + n * 4.0 + ph)
        t = t + fold * 0.13 * folds + (n - 0.5) * 0.12
        if weave:
            t = t + (((xx + yy) % 2) == 0) * 0.04
        col, a, g = finish(pal, np.clip(t, 0, 1))
        if hem is not None and face not in ('top', 'bottom') and H > 3:
            col[H - 2:] = np.array(hem, np.float32)
            col[H - 2:, ::2] *= 0.8
        if stripe is not None and face not in ('top', 'bottom') and H > 6:
            col[H // 3:H // 3 + 1] = np.array(stripe, np.float32)
        if trim is not None and face not in ('top', 'bottom'):
            col[:, :1] = np.array(trim, np.float32)
            col[:, W - 1:] = np.array(trim, np.float32)
        return col, a, g
    return f


def fur(pal, streak=3.5, tips=None):
    def f(c, face, W, H, r):
        t = lightmap(face, H, W, 0.2, 0.08)
        sn = stretch_noise(H, W, r, 1.0, streak)
        t = t + (sn - 0.5) * 0.55
        fine = r.random((H, W))
        t = t + (fine - 0.5) * 0.1
        t = edge_hl(t, face, H, W, 0.06)
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        if tips is not None and face not in ('top', 'bottom') and H > 3:
            col[H - 2:] = lerp(col[H - 2:], tips, 0.5)
        return col, a, None
    return f


def rock(pal, lava=None, cell=6.0, crack_w=0.9, vgrad=0.14, moss=None):
    """cracked stone / obsidian / ash crust; optional glowing magma in the cracks"""
    def f(c, face, W, H, r):
        ids, d, gap = voronoi(H, W, r, cell, 0.85)
        rv = rand_by_id(ids, r)
        t = lightmap(face, H, W, vgrad, 0.1)
        t = t + (rv - 0.5) * 0.35 + (noise2(H, W, r, 2.5) - 0.5) * 0.25
        crack = gap < crack_w
        # chipped bevel next to the cracks
        t = t - np.clip(1.0 - gap / (crack_w * 3), 0, 1) * 0.18
        t = np.where(crack, 0.0, t)
        t = edge_hl(t, face, H, W)
        col, a, _ = finish(pal, t)
        glow = None
        if lava is not None:
            gm = crack.astype(np.float32)
            gm = np.maximum(gm, np.clip(1.0 - gap / (crack_w * 2.4), 0, 1) * 0.35)
            col = np.where(crack[..., None], np.array(lava, np.float32) * 0.9, col)
            glow = glow_img(H, W, lava, gm)
        if moss is not None:
            mm = noise2(H, W, r, 2.0) > 0.68
            col = np.where(mm[..., None], np.array(moss, np.float32), col)
        return col, a, glow
    return f


def fungus(pal, spot=(224, 214, 120), spot_n=0.12, gills=True, glow_spot=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        t = lightmap(face, H, W, 0.2, 0.1)
        t = t + (noise2(H, W, r, 2.5) - 0.5) * 0.4
        if gills and face in ('bottom',):
            t = t + (np.sin(xx * 2.2) * 0.12)
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        glow = None
        if spot_n > 0 and H > 2 and W > 2:
            m = np.zeros((H, W))
            for _ in range(int(spot_n * W * H / 6) + 1):
                cx, cy = r.random() * W, r.random() * H
                rad = 0.8 + r.random() * 1.4
                m = np.maximum(m, (np.hypot(xx - cx, yy - cy) < rad).astype(float))
            col = np.where(m[..., None] > 0, np.array(spot, np.float32) * (0.85 + 0.15 * lightmap(face, H, W, 0.2, 0)[..., None] * 2), col)
            if glow_spot is not None:
                glow = glow_img(H, W, glow_spot, m * 0.6)
        return col, a, glow
    return f


def chitin(pal, seg=7, shine=True, ridge_glow=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        t = lightmap(face, H, W, 0.3, 0.14)
        t = t + (noise2(H, W, r, 4) - 0.5) * 0.18
        if face not in ('top', 'bottom') and H > seg:
            t = t - ((yy % seg) == seg - 1) * 0.3 + ((yy % seg) == 1) * 0.12
        if shine:
            hl = (np.abs(xx - W * 0.28 - yy * 0.2) < 0.8) & (yy > 1) & (yy < H - 2)
            t = t + hl * 0.28
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        glow = None
        if ridge_glow is not None:
            gm = ((yy % seg) == seg - 1) * 0.35 * (noise2(H, W, r, 2) > 0.5)
            glow = glow_img(H, W, ridge_glow, gm)
        return col, a, glow
    return f


def flesh(pal, vein=(120, 20, 40), stitch=(30, 14, 16), stitches=True, glow_vein=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        t = lightmap(face, H, W, 0.2, 0.1)
        n = noise2(H, W, r, 3)
        t = t + (n - 0.5) * 0.45
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        from paint import cracks
        vm = cracks(H, W, r, max(1, W * H // 200), 0.5, 0.5)
        col = np.where(vm[..., None] > 0.7, np.array(vein, np.float32), col)
        if stitches and H > 6 and W > 6 and r.random() < 0.7:
            y0 = int(r.random() * (H - 4)) + 1
            col[y0, :] = lerp(col[y0, :], stitch, 0.6)
            for x in range(1, W, 2):
                col[max(0, y0 - 1):y0 + 2, x] = stitch
        glow = glow_img(H, W, glow_vein, np.where(vm > 0.7, 0.5, 0)) if glow_vein is not None else None
        return col, a, glow
    return f


def ghost(pal, wisp=0.55, glow_col=None, ragged=True):
    """wispy translucent-looking spirit: ragged dithered alpha, brighter core"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 2.5, 3)
        t = lightmap(face, H, W, 0.1, 0.0) + (n - 0.5) * 0.5
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        alpha = np.ones((H, W))
        if ragged and face not in ('top',):
            fall = np.clip((yy / max(H - 1, 1) - 0.45) * 1.8, 0, 1)
            alpha = (n + 0.15 > fall * wisp * 1.6 + 0.12).astype(float)
        glow = glow_img(H, W, glow_col, np.clip(t * 0.6, 0, 0.5) * alpha) if glow_col is not None else None
        return col, alpha, glow
    return f


def metal(pal, brushed=True, rivet_edge=True, glow_edge=None):
    def f(c, face, W, H, r):
        t = lightmap(face, H, W, 0.22, 0.14)
        sn = stretch_noise(H, W, r, 3.0, 1.0) if brushed else noise2(H, W, r, 2)
        t = t + (sn - 0.5) * 0.28
        t = edge_hl(t, face, H, W, 0.2)
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        glow = None
        if glow_edge is not None:
            gm = border(H, W) * 0.4
            glow = glow_img(H, W, glow_edge, gm)
        return col, a, glow
    return f


def crystal(pal, glow_col, facets=4.0):
    def f(c, face, W, H, r):
        ids, d, gap = voronoi(H, W, r, facets, 1.0)
        rv = rand_by_id(ids, r)
        t = lightmap(face, H, W, 0.3, 0.1) + (rv - 0.5) * 0.5
        t = t + np.clip(1 - gap, 0, 1) * 0.25
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        return col, a, glow_img(H, W, glow_col, np.clip(0.3 + (rv - 0.3) * 0.7, 0, 1) * 0.9)
    return f


def horn(pal, rings=3, tip_glow=None):
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        t = lightmap(face, H, W, 0.35, 0.1)
        t = t + (noise2(H, W, r, 3) - 0.5) * 0.2
        if face not in ('top', 'bottom'):
            t = t - ((yy % max(3, rings)) == 0) * 0.22
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        glow = None
        if tip_glow is not None and face not in ('top', 'bottom'):
            tip = np.clip(1 - yy / max(H - 1, 1) - 0.65, 0, 1) * 2.2
            glow = glow_img(H, W, tip_glow, np.clip(tip, 0, 0.8))
        return col, a, glow
    return f


def solid(rgb, glow=True, strength=1.0):
    """fully emissive cube (eyes, flames, runes, cores)"""
    def f(c, face, W, H, r):
        col = np.zeros((H, W, 3), np.float32) + np.array(rgb, np.float32)
        n = noise2(H, W, r, 2.0)
        col = col * (0.82 + 0.3 * n[..., None])
        g = None
        if glow:
            g = glow_img(H, W, rgb, np.full((H, W), strength))
        return np.clip(col, 0, 255), np.ones((H, W)), g
    return f


def flame(core=(255, 244, 190), mid=(255, 150, 40), edge=(200, 40, 10), sway=0.5):
    """emissive fire tongue (tip at the top of the face, bright base); ragged alpha cut-out"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        v = (yy + 0.5) / max(H, 1)           # 0 at the tip (top of the face), 1 at the base
        n = noise2(H, W, r, 2.0, 3)
        cx = W / 2 + np.sin(v * 5 + r.random() * 6) * W * 0.12 * sway
        half = (W / 2) * (np.clip(v, 0, 1) ** 0.75) * (0.75 + 0.5 * n)
        d = np.abs(xx + 0.5 - cx)
        inside = d < half
        t = np.clip(v * 0.9 + (1 - d / np.maximum(half, 0.5)) * 0.35 + (n - 0.5) * 0.25 - 0.1, 0, 1)
        col = ramp(t, [edge, edge, mid, mid, core])
        alpha = inside.astype(float)
        return col, alpha, glow_img(H, W, mid, alpha * 0.9)
    return f


# ------------------------------------------------------------------ face painters
def face_on(base, face_name='north', eyes=None, mouth=None, brow=None, nose=None, extra=None, eye_col=(255, 240, 210), glow_col=None,
            eye_glow=1.0, teeth=None, mask_alpha=None):
    """wrap a base material and paint a face on one side of a cube.

    eyes : list of (cx, cy, w, h, slant, shape)   all in 0..1 face fractions; the pair is auto mirrored if 'mirror'
    mouth: dict(y, w, h, col, teeth=int)
    brow : dict(y, w, th, col, angle)
    """
    glow_col = glow_col or eye_col

    def f(c, face, W, H, r):
        col, alpha, glow = base(c, face, W, H, r)
        col = col.copy()
        if face != face_name or W < 4 or H < 4:
            return col, alpha, glow
        S = 5
        Y, X = np.mgrid[0:H * S, 0:W * S] / S
        m_eye = np.zeros((H * S, W * S), np.float32)
        m_dark = np.zeros((H * S, W * S), np.float32)
        for e in (eyes or []):
            cx, cy, w, h, slant = e[:5]
            shape = e[5] if len(e) > 5 else 'almond'
            for sx in ((-1, 1) if (len(e) <= 6 or e[6]) else (1,)):
                ex = W * (0.5 + sx * cx)
                ey = H * cy
                u = (X - ex) * sx
                v = (Y - ey) + u * slant
                ww, hh = W * w, H * h
                if shape == 'almond':
                    inside = (u / ww) ** 2 + (v / hh) ** 2 < 1
                    inside &= (np.abs(v) < hh * (1 - np.abs(u) / ww * 0.6))
                elif shape == 'slit':
                    inside = (np.abs(u) < ww) & (np.abs(v) < hh * (1 - (np.abs(u) / ww) ** 2))
                elif shape == 'round':
                    inside = (u / ww) ** 2 + (v / hh) ** 2 < 1
                else:  # box
                    inside = (np.abs(u) < ww) & (np.abs(v) < hh)
                m_dark = np.maximum(m_dark, ((u / (ww * 1.5)) ** 2 + (v / (hh * 1.9)) ** 2 < 1).astype(np.float32))
                m_eye = np.maximum(m_eye, inside.astype(np.float32))
        if mouth:
            my = H * mouth['y']
            mw = W * mouth['w']
            mh = max(0.5, H * mouth.get('h', 0.08))
            curve = mouth.get('curve', 0.0)
            yy = my + curve * ((X - W / 2) / max(mw, 1)) ** 2 * H * 0.12
            inside = (np.abs(X - W / 2) < mw) & (np.abs(Y - yy) < mh)
            mcol = np.array(mouth.get('col', (20, 6, 8)), np.float32)
            mm = inside.reshape(H, S, W, S).mean((1, 3))
            col = lerp(col, mcol, np.clip(mm * 1.5, 0, 1))
            nt = mouth.get('teeth', 0)
            if nt:
                tcol = np.array(teeth or (236, 226, 200), np.float32)
                for k in range(nt):
                    tx = W / 2 - mw + (k + 0.5) * (2 * mw / nt)
                    th = (np.abs(X - tx) < 2 * mw / nt * 0.28) & (np.abs(Y - (yy - mh * 0.5)) < mh * 1.1) & (Y < yy + mh * (0.9 if k % 2 else 0.4))
                    tm = th.reshape(H, S, W, S).mean((1, 3))
                    col = lerp(col, tcol, np.clip(tm * 1.6, 0, 1))
        if brow:
            by = H * brow['y']
            bw = W * brow['w']
            th_ = max(0.6, H * brow.get('th', 0.07))
            ang = brow.get('angle', 0.3)
            bcol = np.array(brow.get('col', (20, 10, 10)), np.float32)
            bm = np.zeros((H * S, W * S), np.float32)
            for sx in (-1, 1):
                u = (X - W * (0.5 + sx * brow.get('cx', 0.22))) * sx
                v = (Y - by) + u * ang * 1.0
                bm = np.maximum(bm, ((np.abs(u) < bw) & (np.abs(v) < th_)).astype(np.float32))
            bm = bm.reshape(H, S, W, S).mean((1, 3))
            col = lerp(col, bcol, np.clip(bm * 1.5, 0, 1))
        if nose:
            nx, ny, nw, nh = nose.get('x', 0.5), nose.get('y', 0.55), nose.get('w', 0.08), nose.get('h', 0.12)
            nm = (np.abs(X - W * nx) < W * nw) & (np.abs(Y - H * ny) < H * nh)
            nm = nm.reshape(H, S, W, S).mean((1, 3))
            col = lerp(col, np.array(nose.get('col', (20, 10, 10)), np.float32), np.clip(nm * 1.4, 0, 1))
        dm = m_dark.reshape(H, S, W, S).mean((1, 3))
        col = lerp(col, (12, 6, 10), np.clip(dm * 0.7, 0, 1))
        em = m_eye.reshape(H, S, W, S).mean((1, 3))
        col = lerp(col, np.array(eye_col, np.float32), np.clip(em * 1.3, 0, 1))
        g = np.zeros((H, W, 4), np.float32)
        if glow is not None:
            g = glow.copy()
        g[..., :3] = np.where(em[..., None] > 0.05, np.array(glow_col, np.float32), g[..., :3])
        g[..., 3] = np.maximum(g[..., 3], np.clip(em * 1.4, 0, 1) * 255 * eye_glow)
        if extra is not None:
            col, g = extra(col, g, H, W, r)
        return col, alpha, g
    return f


def with_glow_streaks(base, glow_col, density=0.15, vertical=True):
    """add glowing vein streaks on any material"""
    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        col = col.copy()
        m = np.zeros((H, W))
        for _ in range(int(density * (W + H) / 4) + 1):
            x0, y0 = r.random() * W, r.random() * H
            ang = (math.pi / 2 if vertical else 0) + r.normal(0, 0.5)
            L = 2 + r.random() * 6
            draw_line(m, x0, y0, x0 + math.cos(ang) * L, y0 + math.sin(ang) * L)
        gl = glow_img(H, W, glow_col, m * 0.8)
        if g is not None:
            gl[..., 3] = np.maximum(gl[..., 3], g[..., 3])
            gl[..., :3] = np.where(g[..., 3:4] > gl[..., 3:4] * 0.5, g[..., :3], gl[..., :3])
        col = np.where(m[..., None] > 0, lerp(col, glow_col, 0.6), col)
        return col, a, gl
    return f


def membrane(pal, vein_col=None, scallops=3, edge_dark=True, vgrad=0.05):
    """bat-wing skin on a vertical plane: bone-side (top) is attached, bottom edge is scalloped & ragged"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        t = lightmap(face, H, W, vgrad, 0.0)
        n = noise2(H, W, r, 3)
        t = t + (n - 0.5) * 0.35 - (yy / max(H - 1, 1)) * 0.12
        col, a, _ = finish(pal, np.clip(t, 0, 1))
        # scalloped lower edge
        k = max(1, scallops)
        cell = W / k
        arc = (1 - np.abs(((xx % cell) / cell) * 2 - 1) ** 2) * (H * 0.22)
        cut = yy > (H - 1 - arc * (0.6 + 0.4 * r.random())) 
        alpha = np.where(cut, 0.0, 1.0)
        glow = None
        if vein_col is not None:
            vm = np.zeros((H, W))
            # veins radiate from the top-left (attachment) toward the scallop tips
            for i in range(k + 1):
                x1 = i * cell
                draw_line(vm, 0, 0, x1, H - 1, 1.0)
            vm *= alpha
            col = np.where(vm[..., None] > 0, lerp(col, vein_col, 0.55), col)
            glow = glow_img(H, W, vein_col, vm * 0.55)
        return col, alpha, glow
    return f


def feather(pal, tip=None, glow_tip=None, rachis=True, width=0.5, tip_start=0.55):
    """feather on a vertical card; quill at the top, tip at the bottom; lens shaped alpha"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        v = (yy + 0.5) / max(H, 1)
        n = noise2(H, W, r, 2.0)
        prof = np.sin(np.clip(v, 0, 1) * math.pi * 0.95 + 0.15) * width + 0.08
        d = np.abs((xx + 0.5) / W - 0.5)
        inside = d < prof * 0.5 + 0.02
        t = 0.55 - v * 0.2 + (n - 0.5) * 0.35 - d * 0.5
        col = ramp(np.clip(t, 0, 1), pal)
        g = None
        if rachis:
            m = np.abs((xx + 0.5) - W / 2) < 0.6
            col = np.where(m[..., None], np.array(pal[-1], np.float32), col)
        if tip is not None:
            m = np.clip((v - tip_start) / (1 - tip_start), 0, 1)
            col = lerp(col, tip, m[..., None][..., 0] * 0.9) if False else col * (1 - m[..., None]) + np.array(tip, np.float32) * m[..., None]
            if glow_tip is not None:
                g = glow_img(H, W, glow_tip, m * 0.9 * inside)
        return col, inside.astype(float), g
    return f


def feather_sheet(pal, tip=None, glow_tip=None, rows=3, fw=3, tip_depth=5, tip_start=0.55, vein=None):
    """a wing panel painted as rows of overlapping feathers (roof-tile style); bottom edge ends in pointed feather tips"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        n = noise2(H, W, r, 2.0)
        rh = max(3, H // (rows + 1))
        row = yy // rh
        off = (row * (fw // 2 + 1)) % fw
        fx = (xx + off) % fw
        # each feather: lighter centre line, darker edges, curved bottom lip
        inrow = yy % rh
        t = 0.62 - (inrow / rh) * 0.25 + (1 - np.abs(fx - (fw - 1) / 2) / (fw / 2)) * 0.12 + (n - 0.5) * 0.3
        t = t - ((inrow == rh - 1) * 0.22)
        col = ramp(np.clip(t, 0, 1), pal)
        # pointed tips along the bottom edge
        tipw = max(2, fw)
        cx = (xx % tipw) / max(tipw - 1, 1)
        point = (1 - np.abs(cx * 2 - 1)) * tip_depth * (0.7 + 0.5 * noise2(H, W, r, 1.5))
        alpha = (yy < (H - tip_depth + point)).astype(float)
        v = np.clip(yy / max(H - 1, 1), 0, 1)
        g = None
        if tip is not None:
            m = np.clip((v - tip_start) / (1 - tip_start), 0, 1)[..., None]
            col = col * (1 - m * 0.85) + np.array(tip, np.float32) * m * 0.85
            if glow_tip is not None:
                g = glow_img(H, W, glow_tip, m[..., 0] * 0.85 * alpha)
        return col, alpha, g
    return f


def ragged(base, depth=3.0, freq=1.6, top_frac=0.0):
    """wrap a material: lower edge is torn into ragged strips (alpha cut-out)"""
    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        yy, xx = np.mgrid[0:H, 0:W]
        if face in ('top', 'bottom') or H < 4:
            return col, a, g
        strip = np.zeros(W)
        n = noise2(1, W, r, freq, 2)[0]
        cut = (1.0 - n) * depth + (r.random(W) * 1.2)
        keep = yy < (H - cut[None, :])
        a = a * keep
        return col, a, g
    return f


def eyeball(iris=(255, 200, 40), sclera=(236, 228, 200), pupil=(10, 6, 4), glow_iris=True):
    """small round eye on every face: sclera ring, coloured iris, dark slit pupil"""
    def f(c, face, W, H, r):
        yy, xx = np.mgrid[0:H, 0:W]
        cx, cy = W / 2, H / 2
        d = np.hypot(xx + 0.5 - cx, yy + 0.5 - cy) / (max(W, H) / 2)
        col = np.zeros((H, W, 3), np.float32) + np.array(sclera, np.float32) * (0.6 + 0.4 * (1 - d))[..., None]
        col = np.where((d < 0.72)[..., None], np.array(iris, np.float32), col)
        col = np.where(((np.abs(xx + 0.5 - cx) < max(0.6, W * 0.1)) & (d < 0.6))[..., None], np.array(pupil, np.float32), col)
        g = None
        if glow_iris:
            g = glow_img(H, W, iris, np.where(d < 0.72, 0.6, 0))
        return col, np.ones((H, W)), g
    return f
