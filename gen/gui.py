"""GUI and effect textures: skill icons, world map, map icons, gate swirl, projectiles."""
import os, math
import numpy as np
import tex as T

H = T.hexc


def canvas(n=128, c=(0, 0, 0, 0)):
    return T.new(n, n, c)


def ring_mask(n, cx, cy, r0, r1, soft=1.0):
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32) + 0.5
    d = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2)
    return np.clip(np.minimum(d - r0, r1 - d) / soft + 0.5, 0, 1)


def poly_mask(n, pts):
    from PIL import Image, ImageDraw
    im = Image.new('L', (n * 4, n * 4), 0)
    ImageDraw.Draw(im).polygon([(x * 4, y * 4) for x, y in pts], fill=255)
    im = im.resize((n, n), Image.LANCZOS)
    return np.asarray(im).astype(np.float32) / 255


def frame(img, color):
    n = img.shape[0]
    bg = T.ramp([H('#050A18'), H(color) * np.array([0.35, 0.35, 0.35, 1], np.float32)], np.broadcast_to(np.linspace(0, 1, n)[:, None], (n, n)))
    bg[:, :, 3] = 1
    out = bg.copy()
    T.paste(out, img, 0, 0)
    # bevelled border
    T.draw_mask(out, ring_mask(n, n / 2, n / 2, 0, 0) * 0, H('#000000'))
    b = max(2, n // 24)
    for i in range(b):
        a = 1 - i / b
        c = np.append(np.clip(H(color)[:3] * 1.3, 0, 1), a)
        T.rect(out, i, i, n - i, i + 1, c)
        T.rect(out, i, n - i - 1, n - i, n - i, c)
        T.rect(out, i, i, i + 1, n - i, c)
        T.rect(out, n - i - 1, i, n - i, n - i, c)
    return out


def glow(img, mask, color, radius=6):
    from PIL import Image, ImageFilter
    n = img.shape[0]
    m = Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius))
    g = np.asarray(m).astype(np.float32) / 255
    T.draw_mask(img, np.clip(g * 1.4, 0, 1) * 0.8, H(color))
    T.draw_mask(img, mask, H(color))


def dagger(img, x0, y0, x1, y1, color='#C8D2E0', width=10):
    n = img.shape[0]
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy)
    ux, uy = dx / L, dy / L
    px, py = -uy, ux
    hx, hy = x0 + ux * L * 0.3, y0 + uy * L * 0.3
    blade = [(hx + px * width / 2, hy + py * width / 2), (x1, y1), (hx - px * width / 2, hy - py * width / 2)]
    T.draw_mask(img, poly_mask(n, blade), H(color))
    T.draw_mask(img, poly_mask(n, [(hx, hy), (x1, y1), (hx - px * width / 2, hy - py * width / 2)]), H('#7A869A'))
    T.line(img, hx - px * width * 0.9, hy - py * width * 0.9, hx + px * width * 0.9, hy + py * width * 0.9, H('#D9A93A'), width * 0.45)
    T.line(img, x0, y0, hx, hy, H('#3A2A20'), width * 0.5)


def skill_icon(i):
    n = 128
    img = canvas(n)
    c = n / 2
    if i == 0:  # sprint
        for k in range(5):
            T.line(img, 18, 30 + k * 16, 70 - k * 6, 30 + k * 16, H('#7FD8FF'), 5)
        m = poly_mask(n, [(60, 30), (100, 64), (60, 98), (72, 64)])
        glow(img, m, '#BFF0FF', 5)
        col = '#3A8AFF'
    elif i == 1:  # dagger throw
        for k in range(3):
            T.line(img, 20 + k * 6, 100 - k * 14, 50 + k * 6, 70 - k * 14, H('#9A7AFF'), 3)
        dagger(img, 40, 92, 104, 28, '#D8D0FF', 14)
        col = '#6A4AFF'
    elif i == 2:  # bloodlust: red eye
        m = T.ellipse_mask(n, n, c, c, 46, 22, 2)
        glow(img, m * 0.9, '#FF2A3A', 10)
        T.draw_mask(img, T.ellipse_mask(n, n, c, c, 16, 18, 1.5), H('#FFE04A'))
        T.draw_mask(img, T.ellipse_mask(n, n, c, c, 5, 16, 1.5), H('#1A0000'))
        col = '#B0101C'
    elif i == 3:  # mutilation: claw slashes
        for k in range(3):
            m = poly_mask(n, [(30 + k * 22, 18), (40 + k * 22, 18), (46 + k * 18, 110), (40 + k * 18, 110)])
            glow(img, m, '#FF8A3A', 5)
        col = '#C04A1A'
    elif i == 4:  # stealth: fading hooded silhouette
        m = poly_mask(n, [(64, 18), (88, 40), (92, 110), (36, 110), (40, 40)])
        ys = np.linspace(0, 1, n)[:, None]
        T.draw_mask(img, m * np.clip(1.2 - ys * 1.1, 0, 1), H('#8A8AB0'))
        T.draw_mask(img, T.ellipse_mask(n, n, 58, 46, 4, 3, 1) + T.ellipse_mask(n, n, 72, 46, 4, 3, 1), H('#B08AFF'))
        col = '#3A3A5A'
    elif i == 5:  # vital strike: crosshair
        glow(img, ring_mask(n, c, c, 30, 36), '#FF4A4A', 4)
        T.line(img, c, 14, c, 46, H('#FF4A4A'), 5)
        T.line(img, c, 82, c, 114, H('#FF4A4A'), 5)
        T.line(img, 14, c, 46, c, H('#FF4A4A'), 5)
        T.line(img, 82, c, 114, c, H('#FF4A4A'), 5)
        dagger(img, 34, 94, 92, 36, '#FFFFFF', 10)
        col = '#801018'
    elif i == 6:  # shadow exchange
        for x, colr in ((38, '#5A3A9A'), (90, '#9A7AFF')):
            T.draw_mask(img, T.ellipse_mask(n, n, x, 40, 10, 10, 1) + poly_mask(n, [(x - 14, 58), (x + 14, 58), (x + 18, 108), (x - 18, 108)]), H(colr))
        T.line(img, 44, 26, 84, 26, H('#FFFFFF'), 4)
        T.line(img, 84, 26, 76, 18, H('#FFFFFF'), 4)
        T.line(img, 84, 112, 44, 112, H('#FFFFFF'), 4)
        T.line(img, 44, 112, 52, 120, H('#FFFFFF'), 4)
        col = '#3A2A6A'
    elif i == 7:  # ruler's authority: hand + blue force
        glow(img, ring_mask(n, c, c, 38, 44) * 0.8, '#5FC8FF', 6)
        hand = poly_mask(n, [(46, 100), (82, 100), (86, 56), (78, 56), (76, 30), (70, 30), (68, 54), (64, 22), (58, 22), (58, 54), (52, 28), (46, 28), (46, 60), (38, 48), (32, 52)])
        T.draw_mask(img, hand, H('#E8F6FF'))
        col = '#1A5AB0'
    elif i == 8:  # dagger storm
        for k in range(8):
            a = k * math.pi / 4
            dagger(img, c + math.cos(a) * 14, c + math.sin(a) * 14, c + math.cos(a) * 58, c + math.sin(a) * 58, '#D8C8FF', 9)
        glow(img, T.ellipse_mask(n, n, c, c, 10, 10, 2), '#B04AFF', 6)
        col = '#4A2A8A'
    elif i == 9:  # dragon's fear: dragon eye + waves
        for r in (40, 50, 60):
            T.draw_mask(img, ring_mask(n, c, c, r, r + 3) * 0.7, H('#FF6A2A'))
        T.draw_mask(img, T.ellipse_mask(n, n, c, c, 30, 20, 2), H('#FFB020'))
        T.draw_mask(img, T.ellipse_mask(n, n, c, c, 6, 19, 1.5), H('#1A0400'))
        col = '#8A2A0A'
    elif i == 10:  # monarch's domain: purple crown on dark ground circle
        glow(img, ring_mask(n, c, 92, 0, 50) * T.ellipse_mask(n, n, c, 92, 52, 16, 2), '#6A2AFF', 6)
        crown = poly_mask(n, [(30, 80), (98, 80), (104, 34), (84, 56), (64, 24), (44, 56), (24, 34)])
        glow(img, crown, '#C8A8FF', 5)
        col = '#2A0A5A'
    elif i == 11:  # tenacity
        T.draw_mask(img, poly_mask(n, [(64, 14), (104, 30), (100, 74), (64, 114), (28, 74), (24, 30)]), H('#7A8AA0'))
        heart = T.ellipse_mask(n, n, 54, 56, 12, 12, 1) + T.ellipse_mask(n, n, 74, 56, 12, 12, 1) + poly_mask(n, [(43, 62), (85, 62), (64, 90)])
        T.draw_mask(img, np.clip(heart, 0, 1), H('#FF3A4A'))
        col = '#3A4A5A'
    elif i == 12:  # advanced dagger arts
        dagger(img, 26, 102, 100, 28, '#E0E8F0', 12)
        dagger(img, 102, 102, 28, 28, '#E0E8F0', 12)
        col = '#3A4A6A'
    elif i == 13:  # detoxification
        drop = T.ellipse_mask(n, n, c, 76, 28, 28, 1) + poly_mask(n, [(c, 14), (c + 26, 66), (c - 26, 66)])
        T.draw_mask(img, np.clip(drop, 0, 1), H('#3AD07A'))
        T.line(img, 48, 60, 80, 92, H('#FFFFFF'), 8)
        T.line(img, 80, 60, 48, 92, H('#FFFFFF'), 8)
        col = '#1A5A2A'
    elif i == 14:  # long range detection
        for r in (14, 30, 46):
            T.draw_mask(img, ring_mask(n, c, c, r, r + 3), H('#5FE8A0'))
        T.line(img, c, c, c + 40, c - 34, H('#BFFFD8'), 4)
        for (x, y) in ((40, 44), (88, 80), (78, 36)):
            glow(img, T.ellipse_mask(n, n, x, y, 4, 4, 1), '#FF4A4A', 3)
        col = '#0A3A2A'
    else:  # shadow preservation: soul flame
        fl = poly_mask(n, [(64, 12), (88, 56), (92, 84), (76, 108), (52, 108), (36, 84), (40, 56), (52, 66)])
        glow(img, fl, '#8A5AFF', 7)
        T.draw_mask(img, poly_mask(n, [(64, 48), (78, 80), (64, 100), (50, 80)]), H('#E8DAFF'))
        col = '#2A1A5A'
    return T.resize(frame(img, col), 32, 32)


def skills_atlas(path):
    atlas = T.new(256, 256)
    for i in range(16):
        T.put(atlas, skill_icon(i), (i % 8) * 32, (i // 8) * 32)
    T.save(atlas, path)


# ------------------------------------------------------------------ world map

def world_map(path):
    W_, H_ = 512, 320
    ys, xs = np.mgrid[0:H_, 0:W_].astype(np.float32)
    u, v = xs / W_, ys / H_
    n = T.value_noise(W_, H_, 40, 77, 5, 0.55)
    # landmass: main peninsula + islands
    def blob(cx, cy, rx, ry, k=0.35):
        return 1 - (((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2) + (n - 0.5) * k
    land = np.maximum.reduce([blob(0.5, 0.48, 0.38, 0.42), blob(0.24, 0.3, 0.16, 0.2), blob(0.8, 0.24, 0.14, 0.16), blob(0.83, 0.74, 0.12, 0.16),
                              blob(0.13, 0.7, 0.1, 0.14)])
    jeju = blob(0.25, 0.88, 0.07, 0.06, 0.2)
    ocean = T.ramp([H('#081A33'), H('#0E2E55'), H('#1A4A7A')], np.clip(n * 1.2 - 0.1 + (1 - v) * 0.1, 0, 1))
    img = ocean.copy()
    img[:, :, 3] = 1
    is_land = (land > 0) | (jeju > 0)
    elev = np.clip(np.maximum(land, jeju) * 1.4 + (n - 0.5), 0, 1)
    landc = T.ramp([H('#3A5A3A'), H('#5A7A4A'), H('#8A8A62'), H('#C8C0A0')], elev)
    img[is_land] = landc[is_land]
    coast = (np.maximum(land, jeju) > -0.04) & ~is_land
    img[coast] = (img[coast] * 0.5 + H('#7FB8D8') * 0.5)
    # penalty desert
    desert = blob(0.13, 0.7, 0.09, 0.12) > 0
    img[desert] = T.colorize(H('#D8B870'), n, 0.85, 1.1)[desert]
    # demon castle realm
    demon = blob(0.83, 0.74, 0.1, 0.13) > 0
    img[demon] = T.colorize(H('#5A1414'), n, 0.7, 1.3)[demon]
    # job change: dark castle lands
    jc = blob(0.8, 0.24, 0.11, 0.12) > 0
    img[jc] = T.colorize(H('#3A3A4A'), n, 0.7, 1.2)[jc]
    # mountains
    r = T.rng(5)
    for _ in range(70):
        x, y = r.uniform(0.2, 0.8) * W_, r.uniform(0.15, 0.85) * H_
        if not is_land[int(y), int(x)] or abs(x / W_ - 0.5) < 0.12 and abs(y / H_ - 0.5) < 0.15:
            continue
        T.line(img, x - 6, y + 5, x, y - 5, H('#2A2A22'), 1.4)
        T.line(img, x, y - 5, x + 6, y + 5, H('#2A2A22'), 1.4)
        T.line(img, x - 2, y - 2, x, y - 5, H('#F0F0F0'), 1.2)
    # Seoul: city circle with road grid
    cx, cy = 0.5 * W_, 0.5 * H_
    city = T.ellipse_mask(W_, H_, cx, cy, 0.09 * W_, 0.09 * W_ * 1.0, 2)
    T.draw_mask(img, city * 0.85, H('#5A5E6A'))
    for k in range(-4, 5):
        T.line(img, cx + k * 9, cy - 42, cx + k * 9, cy + 42, np.append(H('#C8CCD6')[:3], 0.55), 1)
        T.line(img, cx - 42, cy + k * 9, cx + 42, cy + k * 9, np.append(H('#C8CCD6')[:3], 0.55), 1)
    T.draw_mask(img, T.ellipse_mask(W_, H_, cx, cy, 6, 6, 1), H('#5FD0FF'))
    # river (Han river)
    pts = [(0.3, 0.56), (0.4, 0.53), (0.5, 0.55), (0.6, 0.52), (0.72, 0.5)]
    for a, b in zip(pts, pts[1:]):
        T.line(img, a[0] * W_, a[1] * H_, b[0] * W_, b[1] * H_, H('#2A6AAA'), 3)
    # parchment frame, grid, compass
    for gx in range(0, W_, 64):
        T.line(img, gx, 0, gx, H_, np.append(H('#FFFFFF')[:3], 0.06), 1)
    for gy in range(0, H_, 64):
        T.line(img, 0, gy, W_, gy, np.append(H('#FFFFFF')[:3], 0.06), 1)
    ccx, ccy = W_ - 40, H_ - 40
    for a, L in ((0, 22), (math.pi / 2, 14), (math.pi, 14), (-math.pi / 2, 14)):
        T.line(img, ccx, ccy, ccx + math.sin(a) * L, ccy - math.cos(a) * L, H('#E8D8A0'), 3)
    T.draw_mask(img, T.ellipse_mask(W_, H_, ccx, ccy, 4, 4, 1), H('#E8D8A0'))
    vign = np.clip(1.15 - (((u - 0.5) * 1.6) ** 2 + ((v - 0.5) * 1.6) ** 2) * 0.5, 0.6, 1)
    img = T.shade(img, vign)
    for i in range(3):
        T.rect(img, i, i, W_ - i, i + 1, H('#6FD8FF'))
        T.rect(img, i, H_ - i - 1, W_ - i, H_ - i, H('#6FD8FF'))
        T.rect(img, i, i, i + 1, H_ - i, H('#6FD8FF'))
        T.rect(img, W_ - i - 1, i, W_ - i, H_ - i, H('#6FD8FF'))
    T.save(img, path)


def map_icons(path):
    atlas = T.new(256, 64)
    defs = [('#5FD0FF', 'gate'), ('#3A8AFF', 'hex'), ('#E8C547', 'banner'), ('#FF3A4A', 'cross'), ('#FFB347', 'bag'), ('#7CFF9A', 'house'),
            ('#E8D8A0', 'face'), ('#C8C8D8', 'sword'), ('#FF4A2A', 'castle'), ('#B07AFF', 'ant'), ('#D8B870', 'skull'), ('#5ACF5A', 'tree')]
    for i, (col, kind) in enumerate(defs):
        n = 128
        img = canvas(n)
        T.draw_mask(img, T.ellipse_mask(n, n, 64, 64, 58, 58, 2), H('#0A1428'))
        T.draw_mask(img, ring_mask(n, 64, 64, 52, 58), H(col))
        c = H(col)
        if kind == 'gate':
            glow(img, ring_mask(n, 64, 64, 18, 32), col, 4)
            T.draw_mask(img, T.ellipse_mask(n, n, 64, 64, 12, 12, 2), H('#FFFFFF'))
        elif kind == 'hex':
            hexm = poly_mask(n, [(64 + 34 * math.cos(a), 64 + 34 * math.sin(a)) for a in [k * math.pi / 3 for k in range(6)]])
            T.draw_mask(img, hexm, c)
        elif kind == 'banner':
            T.draw_mask(img, poly_mask(n, [(40, 26), (88, 26), (88, 100), (64, 84), (40, 100)]), c)
        elif kind == 'cross':
            T.rect(img, 54, 26, 74, 102, c)
            T.rect(img, 26, 54, 102, 74, c)
        elif kind == 'bag':
            T.draw_mask(img, T.ellipse_mask(n, n, 64, 76, 30, 26, 2), c)
            T.draw_mask(img, ring_mask(n, 64, 44, 10, 16), c)
        elif kind == 'house':
            T.draw_mask(img, poly_mask(n, [(28, 62), (64, 28), (100, 62), (90, 62), (90, 100), (38, 100), (38, 62)]), c)
        elif kind == 'face':
            T.draw_mask(img, T.ellipse_mask(n, n, 64, 64, 30, 36, 2), c)
            for x in (52, 76):
                T.draw_mask(img, T.ellipse_mask(n, n, x, 56, 6, 4, 1), H('#FFD24A'))
            T.draw_mask(img, T.ellipse_mask(n, n, 64, 82, 14, 4, 1), H('#3A2A10'))
        elif kind == 'sword':
            dagger(img, 34, 94, 96, 32, '#E8E8F0', 14)
        elif kind == 'castle':
            T.draw_mask(img, poly_mask(n, [(30, 100), (30, 50), (40, 40), (50, 50), (50, 60), (58, 60), (64, 26), (70, 60), (78, 60), (78, 50), (88, 40), (98, 50), (98, 100)]), c)
        elif kind == 'ant':
            for (y, r) in ((40, 12), (64, 10), (88, 16)):
                T.draw_mask(img, T.ellipse_mask(n, n, 64, y, r, r * 1.1, 1), c)
            for k in range(3):
                T.line(img, 40, 56 + k * 10, 88, 72 + k * 10 - 20, c, 3)
        elif kind == 'skull':
            T.draw_mask(img, T.ellipse_mask(n, n, 64, 58, 28, 26, 2), c)
            T.rect(img, 50, 74, 78, 92, c)
            for x in (52, 76):
                T.draw_mask(img, T.ellipse_mask(n, n, x, 58, 7, 8, 1), H('#0A1428'))
        else:
            T.draw_mask(img, T.ellipse_mask(n, n, 64, 52, 30, 26, 2), c)
            T.rect(img, 58, 70, 70, 100, H('#7A5030'))
        T.put(atlas, T.resize(img, 32, 32), (i % 8) * 32, (i // 8) * 32)
    T.save(atlas, path)


# ------------------------------------------------------------------ gate / projectiles

def gate_textures(d):
    n = 128
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32) + 0.5
    dx, dy = xs - n / 2, ys - n / 2
    r = np.sqrt(dx * dx + dy * dy) / (n / 2)
    a = np.arctan2(dy, dx)
    nz = T.value_noise(n, n, 10, 3, 3)
    swirl = (np.sin(a * 4 + r * 10 + nz * 2) + 1) / 2
    alpha = np.clip((1 - r) * 2.2, 0, 1) * (0.35 + swirl * 0.65) * np.clip(r * 3, 0.3, 1)
    img = T.new(n, n, (1, 1, 1, 1))
    img[:, :, :3] = np.clip(0.55 + swirl[:, :, None] * 0.45, 0, 1)
    img[:, :, 3] = alpha
    T.save(img, os.path.join(d, 'gate_swirl.png'))
    ring = T.new(n, n, (1, 1, 1, 1))
    ring[:, :, 3] = np.clip(1 - np.abs(r - 0.82) / 0.12, 0, 1) * (0.7 + nz * 0.3) + np.clip(1 - np.abs(r - 0.82) / 0.3, 0, 1) * 0.25
    T.save(ring, os.path.join(d, 'gate_ring.png'))
    core = T.new(n, n, (1, 1, 1, 1))
    core[:, :, 3] = np.clip(1 - r, 0, 1) ** 1.6
    T.save(core, os.path.join(d, 'gate_core.png'))


def projectiles(path):
    atlas = T.new(256, 32)
    cols = [('#B08AFF', 'blade'), ('#BFF2FF', 'shard'), ('#FF7A1A', 'orb'), ('#7AE04A', 'orb'), ('#C8B0FF', 'spark'), ('#5FC8FF', 'orb'), ('#8A7A6A', 'rock'),
            ('#FF3A2A', 'crescent')]
    for i, (col, kind) in enumerate(cols):
        n = 128
        img = canvas(n)
        if kind == 'blade':
            dagger(img, 10, 64, 120, 64, '#E8DCFF', 22)
            glow(img, poly_mask(n, [(40, 54), (120, 64), (40, 74)]) * 0.5, col, 6)
        elif kind == 'shard':
            glow(img, poly_mask(n, [(8, 64), (60, 50), (122, 64), (60, 78)]), col, 5)
            T.draw_mask(img, poly_mask(n, [(30, 64), (60, 58), (110, 64), (60, 70)]), H('#FFFFFF'))
        elif kind == 'orb':
            glow(img, T.ellipse_mask(n, n, 64, 64, 34, 34, 6), col, 12)
            T.draw_mask(img, T.ellipse_mask(n, n, 64, 64, 18, 18, 4), H('#FFFFFF'))
        elif kind == 'spark':
            glow(img, T.ellipse_mask(n, n, 64, 64, 22, 22, 4), col, 10)
            r = T.rng(i)
            for _ in range(8):
                a = r.uniform(0, 2 * math.pi)
                x, y = 64.0, 64.0
                for _k in range(4):
                    x2, y2 = x + math.cos(a + r.uniform(-0.6, 0.6)) * 12, y + math.sin(a + r.uniform(-0.6, 0.6)) * 12
                    T.line(img, x, y, x2, y2, H('#FFFFFF'), 3)
                    x, y = x2, y2
        elif kind == 'rock':
            T.draw_mask(img, poly_mask(n, [(30, 50), (64, 28), (98, 46), (100, 84), (66, 102), (32, 86)]), H(col))
        else:
            m = np.clip(ring_mask(n, 30, 64, 60, 76, 2) * (np.mgrid[0:n, 0:n][1] > 40), 0, 1)
            glow(img, m, col, 6)
        img[:, :, 3] *= T.ellipse_mask(n, n, 64, 64, 63, 63, 0.8)
        T.put(atlas, T.resize(img, 32, 32), i * 32, 0)
    T.save(atlas, path)


def run(res_root):
    gd = os.path.join(res_root, 'textures', 'gui')
    ed = os.path.join(res_root, 'textures', 'entity')
    os.makedirs(gd, exist_ok=True)
    os.makedirs(ed, exist_ok=True)
    skills_atlas(os.path.join(gd, 'skills.png'))
    world_map(os.path.join(gd, 'world_map.png'))
    map_icons(os.path.join(gd, 'map_icons.png'))
    gate_textures(ed)
    projectiles(os.path.join(ed, 'projectiles.png'))
