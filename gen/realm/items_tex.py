"""32x32 item sprites drawn from shaded vector shapes."""
import math
import numpy as np
from px import Canvas, hexc, ramp, mix, rng, tile_noise

N = 32


def tri(h, lo, hi):
    """dark / base / light hex triplet of one colour"""
    c = hexc(h)
    f = lambda v: '#%02x%02x%02x' % tuple(int(min(255, max(0, x))) for x in v)
    return [f(c * lo), f(c), f(c * hi + 20)]
YY, XX = np.mgrid[0:N, 0:N].astype(np.float32) + 0.5


def local(ox, oy, ang):
    dx, dy = math.cos(ang), math.sin(ang)
    u = (XX - ox) * dx + (YY - oy) * dy
    v = -(XX - ox) * dy + (YY - oy) * dx
    return u, v


def to_px(ox, oy, ang, pts):
    dx, dy = math.cos(ang), math.sin(ang)
    return [(ox + u * dx - v * dy, oy + u * dy + v * dx) for u, v in pts]


DIAG = -math.pi / 4  # pommel bottom-left -> tip top-right


def blade_weapon(blade_stops, guard_stops, grip='#3a1a10', gem='#ff3030', L=36.0, bw=1.9, curve=0.0, guard_w=5.5,
                 rune=None, flame=None, serrated=False, glow_edge=None, wide_tip=False, ox=2.5, oy=29.5):
    c = Canvas(N)
    u, v = local(ox, oy, DIAG)
    gu = 9.0  # guard position along axis
    # blade polygon
    pts = []
    steps = 14
    for i in range(steps + 1):
        t = i / steps
        uu = gu + 1 + t * (L - gu - 1)
        w = bw * (1 - 0.15 * t) if not wide_tip else bw * (0.85 + 0.35 * math.sin(t * math.pi * 0.9))
        if t > 0.72:
            w *= max(0.0, (1 - t) / 0.28) ** 0.8
        off = curve * (t ** 2) * 6
        if serrated and 0.15 < t < 0.8 and i % 2 == 0:
            w += 0.9
        pts.append((uu, off - w))
    for i in range(steps, -1, -1):
        t = i / steps
        uu = gu + 1 + t * (L - gu - 1)
        w = bw * (1 - 0.15 * t) if not wide_tip else bw * (0.85 + 0.35 * math.sin(t * math.pi * 0.9))
        if t > 0.72:
            w *= max(0.0, (1 - t) / 0.28) ** 0.8
        off = curve * (t ** 2) * 6
        pts.append((uu, off + w * 0.85))
    bm = c.mask_poly(to_px(ox, oy, DIAG, pts))
    bend = curve * np.clip((u - gu - 1) / (L - gu - 1), 0, 1) ** 2 * 6
    tv = np.clip(0.5 - (v - bend) / (bw * 2.2), 0, 1)
    c.fill_ramp(bm, blade_stops, tv * 0.85 + 0.1 * np.clip(u / L, 0, 1))
    # fuller
    fm = bm & (np.abs(v - bend) < 0.55) & (u > gu + 2) & (u < L * 0.72)
    c.rgb[fm] = mix(c.rgb[fm], hexc(blade_stops[0]), 0.55)
    if rune:
        rm = bm & (np.abs(v - bend) < 0.5) & (u > gu + 3) & (u < L * 0.7) & (((u * 1.3).astype(int) % 3) != 0)
        c.fill(rm, hexc(rune))
        c.glow[rm] = 1
    if glow_edge:
        em = bm & ((v - bend) < -bw * 0.55)
        c.rgb[em] = mix(c.rgb[em], hexc(glow_edge), 0.7)
    # grip
    gm, gt = c.mask_line(*to_px(ox, oy, DIAG, [(1.5, 0)])[0], *to_px(ox, oy, DIAG, [(gu, 0)])[0], 2.3)
    wrap = ((u * 1.6).astype(int) % 2 == 0)
    c.fill(gm, mix(hexc(grip), hexc('#000000'), 0.0))
    c.rgb[gm & wrap] = mix(c.rgb[gm & wrap], hexc('#ffffff'), 0.18)
    # guard
    g1 = to_px(ox, oy, DIAG, [(gu - 0.6, -guard_w), (gu + 1.4, -guard_w * 0.7), (gu + 1.4, guard_w * 0.7), (gu - 0.6, guard_w), (gu - 1.6, guard_w * 0.4), (gu - 1.6, -guard_w * 0.4)])
    gmask = c.mask_poly(g1)
    c.fill_ramp(gmask, guard_stops, np.clip(0.6 - v / (guard_w * 2), 0, 1))
    c.inner_edge(gmask, '#ffffff', '#000000', 0.25)
    # pommel gem
    pc = to_px(ox, oy, DIAG, [(1.2, 0)])[0]
    pm = c.mask_circle(pc[0], pc[1], 1.9)
    c.fill_ramp(pm, guard_stops, 0.5)
    gm2 = c.mask_circle(to_px(ox, oy, DIAG, [(gu, 0)])[0][0], to_px(ox, oy, DIAG, [(gu, 0)])[0][1], 1.2)
    c.fill(gm2, hexc(gem))
    c.inner_edge(bm, '#ffffff', '#000000', 0.18)
    if flame:
        r = rng(7)
        for _ in range(9):
            uu = gu + 3 + r.random() * (L - gu - 6)
            vv = -bw - 0.5 - r.random() * 2.5
            x, y = to_px(ox, oy, DIAG, [(uu, vv)])[0]
            c.sparkle(int(x), int(y), flame, 0)
    c.outline('#140406')
    return c


def hammer(head_stops, accent='#ffb030', grip='#2a1a10'):
    c = Canvas(N)
    ox, oy = 4.5, 27.5
    u, v = local(ox, oy, DIAG)
    hm, _ = c.mask_line(ox, oy, *to_px(ox, oy, DIAG, [(24, 0)])[0], 2.4)
    c.fill(hm, hexc(grip))
    c.rgb[hm & ((u * 1.4).astype(int) % 2 == 0)] *= 1.25
    head = to_px(ox, oy, DIAG, [(18, -8), (28, -8), (29, -6), (29, 6), (28, 8), (18, 8), (17, 6), (17, -6)])
    hmask = c.mask_poly(head)
    c.fill_ramp(hmask, head_stops, np.clip(0.55 - v / 18 + 0.2 * (u - 18) / 11, 0, 1))
    band = hmask & (np.abs(u - 23) < 1.2)
    c.fill(band, hexc(accent))
    spikes = to_px(ox, oy, DIAG, [(21, -8), (23, -12), (25, -8)])
    sm = c.mask_poly(spikes)
    c.fill_ramp(sm, head_stops, 0.8)
    rm = hmask & (np.abs(v) < 1.3) & (np.abs(u - 23) < 2.5)
    c.fill(rm, hexc('#fff0a0'))
    c.inner_edge(hmask, '#ffffff', '#000000', 0.3)
    c.outline('#120808')
    return c


def scythe(blade_stops, shaft='#e8e0d0', glow='#4fd9ff'):
    c = Canvas(N)
    sm, _ = c.mask_line(6, 30, 22, 3, 2.2)
    c.fill(sm, hexc(shaft))
    c.rgb[sm & ((YY.astype(int) % 4) == 0)] = hexc('#8a8478')
    # crescent blade
    d1 = np.hypot(XX - 12, YY - 12)
    d2 = np.hypot(XX - 10, YY - 15)
    crescent = (d1 < 12) & (d2 > 11) & (YY < 13) & (XX < 23)
    c.fill_ramp(crescent, blade_stops, np.clip((12 - d1) / 4, 0, 1))
    edge = crescent & (d1 > 10.6)
    c.fill(edge, hexc(glow))
    c.glow[edge] = 1
    skull = c.mask_circle(21.5, 4.5, 2.4)
    c.fill(skull, hexc('#e8e0d0'))
    c.rgb[int(4), 20] = hexc(glow)
    c.rgb[int(4), 22] = hexc(glow)
    c.inner_edge(crescent, '#ffffff', '#000000', 0.2)
    c.outline('#06121a')
    return c


def book(cover, emblem, emblem_kind='circle', pages='#efe2c4', clasp='#e0b040'):
    c = Canvas(N)
    body = c.mask_poly([(6, 5), (25, 4), (27, 6), (27, 28), (25, 29), (6, 29), (5, 27), (5, 7)])
    t = np.clip((XX - 5) / 22, 0, 1) * 0.5 + 0.25 + 0.15 * tile_noise(32, rng(len(cover)), 4, 2)
    c.fill_ramp(body, tri(cover, 0.4, 1.3), t)
    pg = c.mask_poly([(25, 6), (28, 7), (28, 27), (25, 28)])
    c.fill(pg, hexc(pages))
    c.rgb[pg & (YY.astype(int) % 2 == 0)] = hexc('#c8b898')
    spine = body & (XX < 8)
    c.rgb[spine] = mix(c.rgb[spine], hexc('#000000'), 0.4)
    for yy in (9, 25):
        bm = body & (np.abs(YY - yy) < 0.8) & (XX < 9)
        c.fill(bm, hexc(clasp))
    corners = [(6, 5), (25, 5), (6, 28), (25, 28)]
    for x, y in corners:
        c.fill(c.mask_circle(x + 0.5, y + 0.5, 1.6) & body, hexc(clasp))
    cx, cy = 16.5, 16.5
    if emblem_kind == 'circle':
        ring = np.abs(np.hypot(XX - cx, YY - cy) - 5) < 0.9
        c.fill(ring & body, hexc(clasp))
        core = np.hypot(XX - cx, YY - cy) < 3
        c.fill(core & body, hexc(emblem))
    elif emblem_kind == 'flame':
        f = c.mask_poly([(cx, 9), (cx + 4, 15), (cx + 3, 21), (cx, 23), (cx - 3, 21), (cx - 4, 15)])
        c.fill_ramp(f, ['#ff2a10', '#ff8a20', '#ffe080'], np.clip((YY - 9) / 14, 0, 1)[::-1] * 0 + np.clip(1 - np.hypot(XX - cx, YY - 19) / 6, 0, 1))
    elif emblem_kind == 'eye':
        e = (((XX - cx) / 6) ** 2 + ((YY - cy) / 3.2) ** 2) < 1
        c.fill(e & body, hexc('#f0e0c0'))
        c.fill(c.mask_circle(cx, cy, 2.2), hexc(emblem))
        c.fill(c.mask_circle(cx, cy, 0.9), hexc('#000000'))
    elif emblem_kind == 'skull':
        c.fill(c.mask_circle(cx, cy - 1, 4.2), hexc('#e8e0d0'))
        c.fill(c.mask_poly([(cx - 2.5, cy + 2), (cx + 2.5, cy + 2), (cx + 2, cy + 5), (cx - 2, cy + 5)]), hexc('#e8e0d0'))
        c.fill(c.mask_circle(cx - 1.7, cy - 1, 1.1), hexc(emblem))
        c.fill(c.mask_circle(cx + 1.7, cy - 1, 1.1), hexc(emblem))
    elif emblem_kind == 'drop':
        d = c.mask_poly([(cx, 9), (cx + 4.5, 17), (cx + 4, 21), (cx, 23.5), (cx - 4, 21), (cx - 4.5, 17)])
        c.fill_ramp(d, ['#5a0008', '#c01020', '#ff6070'], np.clip(1 - np.hypot(XX - cx - 1, YY - 18) / 6, 0, 1))
    elif emblem_kind == 'bolt':
        b = c.mask_poly([(cx + 2, 8), (cx - 4, 17), (cx, 17), (cx - 2, 25), (cx + 5, 14), (cx + 1, 14), (cx + 4, 8)])
        c.fill(b, hexc(emblem))
    elif emblem_kind == 'wave':
        w = body & (np.abs(YY - (cy + 2.5 * np.sin((XX - cx) * 0.7))) < 1.4) & (np.abs(XX - cx) < 7)
        c.fill(w, hexc(emblem))
        w2 = body & (np.abs(YY - (cy + 5 + 2 * np.sin((XX - cx) * 0.7 + 1))) < 1.0) & (np.abs(XX - cx) < 6)
        c.fill(w2, hexc('#ff8a20'))
    elif emblem_kind == 'shield':
        s = c.mask_poly([(cx - 5, 10), (cx + 5, 10), (cx + 5, 16), (cx, 23), (cx - 5, 16)])
        c.fill(s, hexc(emblem))
        c.inner_edge(s, '#ffffff', '#000000', 0.35)
    elif emblem_kind == 'step':
        for k in range(3):
            m = c.mask_poly([(cx - 6 + k * 4, 22 - k * 4), (cx - 3 + k * 4, 22 - k * 4), (cx - 1 + k * 4, 18 - k * 4), (cx - 4 + k * 4, 18 - k * 4)])
            c.fill(m, mix(hexc(emblem), hexc('#ffffff'), k * 0.25))
    elif emblem_kind == 'horns':
        c.fill(c.mask_circle(cx, cy + 2, 3.5), hexc(emblem))
        c.fill(c.mask_poly([(cx - 3, cy), (cx - 7, cy - 7), (cx - 1, cy - 2)]), hexc('#e8d8c0'))
        c.fill(c.mask_poly([(cx + 3, cy), (cx + 7, cy - 7), (cx + 1, cy - 2)]), hexc('#e8d8c0'))
    elif emblem_kind == 'meteor':
        c.fill(c.mask_circle(cx + 2, cy + 3, 3.2), hexc('#ff6a10'))
        tr, _ = c.mask_line(cx - 6, cy - 6, cx + 1, cy + 2, 2.5)
        c.fill(tr & body, hexc('#ffd060'))
        c.fill(c.mask_circle(cx + 2, cy + 3, 1.5), hexc('#fff0b0'))
    c.inner_edge(body, '#ffffff', '#000000', 0.2)
    c.outline('#120406')
    return c


def sigil(rim_stops, core, symbol, sym_col='#fff4d0'):
    c = Canvas(N)
    d = np.hypot(XX - 16, YY - 16)
    disk = d < 12.5
    ang = np.arctan2(YY - 16, XX - 16)
    rim = disk & (d > 9.5)
    teeth = (d >= 12.5) & (d < 14.2) & (np.cos(ang * 8) > 0.55)
    c.fill_ramp(rim | teeth, rim_stops, np.clip(0.5 - (YY - 16) / 28 + 0.1 * np.cos(ang * 16), 0, 1))
    inner = disk & ~rim
    c.fill_ramp(inner, tri(core, 0.35, 1.3),
                np.clip(1 - d / 9.5, 0, 1))
    S = sym_col
    if symbol == 'flame':
        m = c.mask_poly([(16, 9), (20, 15), (19.5, 20), (16, 23), (12.5, 20), (12, 15), (14, 17)])
        c.fill(m, hexc(S))
    elif symbol == 'drop':
        m = c.mask_poly([(16, 9), (20, 17), (19, 21), (16, 23), (13, 21), (12, 17)])
        c.fill(m, hexc(S))
    elif symbol == 'eye':
        e = (((XX - 16) / 6) ** 2 + ((YY - 16) / 3.4) ** 2) < 1
        c.fill(e, hexc(S))
        c.fill(c.mask_circle(16, 16, 2), hexc(core))
        c.fill(c.mask_circle(16, 16, 0.8), hexc('#000000'))
    elif symbol == 'swords':
        a, _ = c.mask_line(10, 22, 22, 10, 1.8)
        b, _ = c.mask_line(10, 10, 22, 22, 1.8)
        c.fill(a | b, hexc(S))
    elif symbol == 'skull':
        c.fill(c.mask_circle(16, 15, 4.3), hexc(S))
        c.fill(c.mask_poly([(13.5, 18), (18.5, 18), (18, 22), (14, 22)]), hexc(S))
        c.fill(c.mask_circle(14.3, 15, 1.2) | c.mask_circle(17.7, 15, 1.2), hexc(core))
    elif symbol == 'crown':
        m = c.mask_poly([(10, 21), (22, 21), (23, 12), (19.5, 16), (16, 10), (12.5, 16), (9, 12)])
        c.fill(m, hexc(S))
    c.inner_edge(disk, '#ffffff', '#000000', 0.3)
    c.sparkle(11, 10, '#ffffff')
    c.outline('#140808')
    return c


def key(shaft_stops, gem, big=False):
    c = Canvas(N)
    sh, _ = c.mask_line(7, 25, 23, 9, 2.6 if big else 2.0)
    c.fill_ramp(sh, shaft_stops, np.clip((XX - YY + 20) / 40, 0, 1))
    bow = np.abs(np.hypot(XX - 24.5, YY - 7.5) - (5 if big else 4)) < 1.4
    c.fill_ramp(bow, shaft_stops, 0.7)
    c.fill(c.mask_circle(24.5, 7.5, 2.2 if big else 1.6), hexc(gem))
    for k, (x, y) in enumerate(((8, 24), (10.5, 21.5))):
        t, _ = c.mask_line(x, y, x - 3, y - 3, 1.8)
        c.fill_ramp(t, shaft_stops, 0.5)
    if big:
        cr = c.mask_poly([(20, 2), (22, 0.5), (24.5, 2), (27, 0.5), (29, 2), (28, 4), (21, 4)])
        c.fill(cr, hexc('#ffd040'))
    c.inner_edge(sh | bow, '#ffffff', '#000000', 0.3)
    c.outline('#140808')
    return c


def vial(liquid, glass='#d0e8f0', glow=False):
    c = Canvas(N)
    body = c.mask_circle(16, 21, 7.5)
    neck = c.mask_poly([(13.5, 7), (18.5, 7), (18.5, 15), (13.5, 15)])
    c.fill(body | neck, hexc(glass), 160)
    liq = body & (YY > 18)
    c.fill_ramp(liq, tri(liquid, 0.4, 1.5),
                np.clip(1 - np.hypot(XX - 14, YY - 21) / 8, 0, 1))
    c.fill(c.mask_poly([(13, 4), (19, 4), (19, 7), (13, 7)]), hexc('#6a4020'))
    hl, _ = c.mask_line(12, 17, 11, 23, 1.3)
    c.fill(hl & body, hexc('#ffffff'), 220)
    if glow:
        c.glow[liq] = 1
    c.outline('#101018')
    return c


def gem_cluster(stops, n=3, seed=1):
    c = Canvas(N)
    r = rng(seed)
    shards = [((16, 3), 5, 0), ((9, 9), 4, -0.5), ((23, 10), 4, 0.5)][:n]
    for (x, y), w, lean in shards:
        pts = [(x, y), (x + w, y + 7), (x + w * 0.8 + lean * 6, 28), (x - w * 0.8 + lean * 6, 28), (x - w, y + 7)]
        m = c.mask_poly(pts)
        c.fill_ramp(m, stops, np.clip(0.4 + (x - XX) / (w * 3) + 0.2 * (YY < y + 7), 0, 1))
        c.inner_edge(m, '#ffffff', '#000000', 0.3)
    base = c.mask_poly([(5, 26), (27, 26), (25, 30), (7, 30)])
    c.fill(base, hexc('#3a1a1a'))
    c.sparkle(15, 8, '#ffffff')
    c.outline('#140818')
    return c


def chunk(stops, seed, glint='#ffffff', ingot=False):
    c = Canvas(N)
    if ingot:
        m = c.mask_poly([(4, 18), (11, 11), (28, 11), (28, 15), (21, 22), (4, 22)])
        top = c.mask_poly([(4.5, 18), (11, 11.5), (27.5, 11.5), (21, 18)])
        c.fill_ramp(m, stops, 0.35)
        c.fill_ramp(top, stops, np.clip(0.55 + (XX - YY) / 50, 0, 1))
        side = c.mask_poly([(21, 18), (27.5, 11.5), (28, 15), (21, 22)])
        c.fill_ramp(side, stops, 0.2)
        c.sparkle(13, 14, glint)
    else:
        r = rng(seed)
        pts = []
        for k in range(9):
            a = k / 9 * math.tau
            rad = 9 + r.random() * 4
            pts.append((16 + math.cos(a) * rad, 17 + math.sin(a) * rad * 0.85))
        m = c.mask_poly(pts)
        n = tile_noise(32, r, 4, 3)
        c.fill_ramp(m, stops, np.clip(n * 0.7 + 0.3 - (YY - 17) / 30, 0, 1))
        for _ in range(5):
            x, y = r.integers(10, 23, 2)
            if m[y, x]:
                c.sparkle(int(x), int(y), glint, 0)
    c.inner_edge(c.a > 0, '#ffffff', '#000000', 0.25)
    c.outline('#120808')
    return c


def orb(stops, swirl='#ffffff', wisps=False):
    c = Canvas(N)
    d = np.hypot(XX - 16, YY - 16)
    m = d < 10
    ang = np.arctan2(YY - 16, XX - 16)
    sw = 0.5 + 0.5 * np.sin(ang * 3 + d * 0.8)
    c.fill_ramp(m, stops, np.clip(1 - d / 10 + 0.25 * sw - 0.1, 0, 1))
    c.fill(m & (sw > 0.92) & (d > 3), hexc(swirl))
    c.sparkle(12, 11, '#ffffff')
    if wisps:
        for k in range(4):
            a = k * math.pi / 2 + 0.4
            x, y = 16 + math.cos(a) * 12, 16 + math.sin(a) * 12
            c.sparkle(int(x), int(y), stops[-1], 0)
    c.glow[m] = 1
    c.outline('#06080e')
    return c


def heart():
    c = Canvas(N)
    m = (((XX - 16) / 11) ** 2 + ((YY - 15 - 0.35 * np.abs(XX - 16)) / 9) ** 2 < 1) & (YY > 6)
    m |= c.mask_circle(11.5, 11, 5.5) | c.mask_circle(20.5, 11, 5.5)
    c.fill_ramp(m, ['#3a0006', '#8a0a14', '#d01a28', '#ff6060'], np.clip(1 - np.hypot(XX - 13, YY - 11) / 13, 0, 1))
    r = rng(3)
    for _ in range(5):
        x, y = 16.0, 18.0
        a = r.random() * math.tau
        for _ in range(8):
            a += r.normal(0, 0.5)
            x += math.cos(a)
            y += math.sin(a)
            if 0 <= int(y) < N and 0 <= int(x) < N and m[int(y), int(x)]:
                c.rgb[int(y), int(x)] = hexc('#ffb040')
    c.fill(c.mask_circle(16, 16, 2.3), hexc('#ffe090'))
    tub = c.mask_poly([(15, 3), (18, 3), (18, 8), (15, 8)])
    c.fill(tub, hexc('#6a0a10'))
    c.sparkle(11, 9, '#ffffff')
    c.outline('#140204')
    return c


def horn(stops, band, mouth):
    c = Canvas(N)
    cen, wid = [], []
    for i in range(25):
        t = i / 24
        cen.append((4 + t * 21, 27 - t * 17 - math.sin(t * math.pi) * 3))
        wid.append(1.1 + t ** 2.2 * 6.5)
    up, dn = [], []
    for i, ((x, y), w) in enumerate(zip(cen, wid)):
        x2, y2 = cen[min(i + 1, 24)]
        x1, y1 = cen[max(i - 1, 0)]
        dx, dy = x2 - x1, y2 - y1
        L = math.hypot(dx, dy)
        nx, ny = -dy / L, dx / L
        up.append((x + nx * w, y + ny * w))
        dn.append((x - nx * w, y - ny * w))
    m = c.mask_poly(up + dn[::-1])
    c.fill_ramp(m, stops, np.clip((XX - 4) / 22 * 0.6 + 0.25 - (YY - 16) / 40, 0, 1))
    for k in (8, 15, 20):
        (x, y), w = cen[k], wid[k]
        bm = m & (np.hypot(XX - x, YY - y) < w + 0.6) & (np.abs((XX - x) * 0.6 + (YY - y) * 0.8) < 0.9)
        c.fill(bm, hexc(band))
    (x, y), w = cen[24], wid[24]
    mm = m & (np.hypot(XX - x, YY - y) < w * 0.75)
    c.fill(mm, hexc(mouth))
    c.inner_edge(m, '#ffffff', '#000000', 0.3)
    c.outline('#120808')
    return c


def banner(field, emblem):
    c = Canvas(N)
    pole, _ = c.mask_line(6, 2, 6, 31, 2)
    c.fill(pole, hexc('#4a2a10'))
    c.fill(c.mask_circle(6, 2.5, 1.8), hexc('#ffd040'))
    cloth = c.mask_poly([(7, 4), (26, 4), (26, 24), (21.5, 20), (16.5, 25), (12, 20), (7, 24)])
    c.fill_ramp(cloth, tri(field, 0.45, 1.3),
                np.clip(0.75 - (XX - 7) / 30 + 0.12 * np.sin(XX * 0.9), 0, 1))
    trim = cloth & (YY < 6)
    c.fill(trim, hexc('#ffd040'))
    cr = c.mask_poly([(11.5, 16), (21.5, 16), (22.5, 9), (19.5, 12.5), (16.5, 8), (13.5, 12.5), (10.5, 9)])
    c.fill(cr, hexc(emblem))
    c.outline('#120808')
    return c


def armor(kind, stops, trim='#ffcf40', gem='#ff3030'):
    c = Canvas(N)
    if kind == 'helmet':
        m = c.mask_poly([(7, 22), (7, 12), (10, 7), (16, 5), (22, 7), (25, 12), (25, 22), (21, 24), (19, 17), (13, 17), (11, 24)])
        c.fill_ramp(m, stops, np.clip(0.75 - (YY - 5) / 25 + (16 - np.abs(XX - 16)) / 40, 0, 1))
        for s in (-1, 1):
            hm = c.mask_poly([(16 + s * 7, 10), (16 + s * 13, 2), (16 + s * 10, 11)])
            c.fill(hm, hexc('#2a1a12'))
        c.fill(m & (np.abs(XX - 16) < 1) & (YY < 16), hexc(trim))
        c.fill(c.mask_circle(16, 10, 1.4), hexc(gem))
    elif kind == 'chestplate':
        m = c.mask_poly([(5, 7), (12, 5), (16, 8), (20, 5), (27, 7), (27, 14), (24, 14), (24, 27), (8, 27), (8, 14), (5, 14)])
        c.fill_ramp(m, stops, np.clip(0.7 - (YY - 5) / 30 + (11 - np.abs(XX - 16)) / 30, 0, 1))
        c.fill(m & (np.abs(XX - 16) < 0.8), hexc('#1a0808'))
        c.fill(m & (np.abs(YY - 21) < 0.8), hexc(trim))
        c.fill(c.mask_circle(16, 14, 2.2), hexc(gem))
    elif kind == 'leggings':
        m = c.mask_poly([(8, 5), (24, 5), (25, 28), (18, 28), (16, 14), (14, 28), (7, 28)])
        c.fill_ramp(m, stops, np.clip(0.7 - (YY - 5) / 40 + (10 - np.abs(XX - 16)) / 30, 0, 1))
        c.fill(m & (YY < 8), hexc(trim))
        c.fill(m & (np.abs(YY - 19) < 0.7), hexc('#1a0808'))
    elif kind == 'boots':
        m = c.mask_poly([(6, 10), (13, 10), (13, 22), (15, 26), (4, 26), (4, 22), (6, 18)]) | \
            c.mask_poly([(19, 10), (26, 10), (26, 18), (28, 22), (28, 26), (17, 26), (19, 22)])
        c.fill_ramp(m, stops, np.clip(0.75 - (YY - 10) / 25, 0, 1))
        c.fill(m & (np.abs(YY - 12) < 1), hexc(trim))
    elif kind == 'crown':
        m = c.mask_poly([(4, 24), (28, 24), (29, 10), (23, 16), (20, 6), (16, 14), (12, 6), (9, 16), (3, 10)])
        c.fill_ramp(m, ['#5a3008', '#b07a10', '#f0c030', '#fff4a0'], np.clip(0.7 - (YY - 6) / 30 + 0.1 * np.sin(XX), 0, 1))
        c.fill(m & (YY > 20), hexc('#8a1020'))
        for x, col in ((9, '#ff3040'), (16, '#ffffff'), (23, '#ff3040')):
            c.fill(c.mask_circle(x, 22, 1.4), hexc(col))
        for x, y in ((3, 10), (12, 6), (20, 6), (29, 10)):
            c.fill(c.mask_circle(x, y, 1.3), hexc('#ff4050'))
    c.inner_edge(c.a > 0, '#ffffff', '#000000', 0.3)
    c.outline('#120606')
    return c


STEEL = ['#3a0a0e', '#7a1a20', '#b83a3a', '#e88a80', '#fff0e8']


def build():
    I = {}
    I['crimson_chronicle'] = book('#7a0a18', '#ff3a3a', 'eye')
    I['ember_key'] = key(['#3a1a08', '#a05010', '#ffb040', '#fff0a0'], '#ff4a10')
    I['throne_key'] = key(['#4a0810', '#a01a20', '#ff5a40', '#ffd0a0'], '#ffffff', big=True)
    I['ash_sigil'] = sigil(['#2a2420', '#6a5a50', '#b0a090', '#f0e0d0'], '#ff7a20', 'flame')
    I['blood_sigil'] = sigil(['#3a0408', '#8a1018', '#d03040', '#ffb0b0'], '#c01020', 'drop')
    I['shadow_sigil'] = sigil(['#0a0614', '#2a1a48', '#5a3a90', '#c0a0ff'], '#8a3aff', 'eye')
    I['legion_sigil'] = sigil(['#2a1a04', '#7a5010', '#d0a030', '#fff0a0'], '#5a2a10', 'swords')
    I['soul_sigil'] = sigil(['#042a30', '#106a78', '#40c0d8', '#d0ffff'], '#0a3a48', 'skull')
    I['tyrant_heart'] = heart()
    I['raw_infernal_steel'] = chunk(['#2a0808', '#5a1414', '#9a3030', '#d07060'], 4, '#ffd0c0')
    I['infernal_steel_ingot'] = chunk(STEEL, 5, '#ffffff', ingot=True)
    I['arcane_crystal'] = gem_cluster(['#2a0a5a', '#6a2ab0', '#b070ff', '#f0d8ff'])
    I['ash_essence'] = orb(['#2a2422', '#6a5e58', '#b0a49a', '#ffd0a0'], '#ff8a2a')
    I['blood_vial'] = vial('#c01020', glow=False)
    I['shadow_shard'] = gem_cluster(['#0a0418', '#3a1a6a', '#8a4ae0', '#e0c8ff'], n=1, seed=3)
    I['brimstone'] = chunk(['#3a2a04', '#8a6a10', '#e0b020', '#fff080'], 6, '#ffffff')
    I['soul_essence'] = orb(['#04202a', '#106a80', '#40d0f0', '#e0ffff'], '#ffffff', wisps=True)
    I['mana_potion'] = vial('#6a40ff', glow=True)
    # weapons
    I['infernal_steel_sword'] = blade_weapon(STEEL, ['#2a0808', '#7a2020', '#c05040'], gem='#ff4040')
    I['ashbringer'] = blade_weapon(['#2a2220', '#5a4c46', '#a09080', '#f0e0c8', '#fffaf0'], ['#3a1a08', '#a05010', '#ffb040'],
                                   gem='#ff6a10', bw=2.6, rune='#ff7a20', flame='#ffb040', wide_tip=True)
    I['bloodthirster'] = blade_weapon(['#3a0006', '#8a0a14', '#d02030', '#ff8080', '#ffe0e0'], ['#1a0408', '#5a1018', '#a02030'],
                                      gem='#ff2030', curve=0.45, bw=1.9, serrated=True, glow_edge='#ff6070')
    I['shadowfang'] = blade_weapon(['#06020c', '#22103a', '#5a2a9a', '#b080ff', '#f0e0ff'], ['#0a0614', '#3a2060', '#8a50e0'],
                                   gem='#d0a0ff', L=30, bw=1.6, rune='#c890ff', guard_w=5.0, ox=4.5, oy=27.5)
    I['warlord_hammer'] = hammer(['#141416', '#3a3a40', '#6a6a72', '#a8a8b0'], accent='#ffc040')
    I['soul_reaper'] = scythe(['#04161c', '#0a3a48', '#2a8aa0', '#a0f0ff'])
    I['sovereign_blade'] = blade_weapon(['#2a0004', '#7a0a10', '#e02a20', '#ffb040', '#fff8d0'], ['#5a3008', '#c08a10', '#ffe060'],
                                        gem='#ffffff', bw=2.3, rune='#ffe080', flame='#ff6a20', guard_w=7.5)
    # tomes
    tomes = {'tome_fireball': ('#8a2a08', '#ff6a10', 'flame'), 'tome_meteor': ('#5a1006', '#ff6a10', 'meteor'),
             'tome_blood_lance': ('#5a0610', '#d01020', 'drop'), 'tome_shadow_step': ('#1a0a30', '#a060ff', 'step'),
             'tome_soul_shield': ('#0a3040', '#40d0f0', 'shield'), 'tome_chain_lightning': ('#1a1a40', '#a0c0ff', 'bolt'),
             'tome_lava_wave': ('#3a1406', '#ff4010', 'wave'), 'tome_summon_imps': ('#3a2a06', '#ffb030', 'horns'),
             'tome_life_drain': ('#3a0418', '#ff3a7a', 'circle'), 'tome_fear': ('#202020', '#b0ffb0', 'skull')}
    for k, (cov, em, kind) in tomes.items():
        I[k] = book(cov, em, kind)
    # armour
    for part in ('helmet', 'chestplate', 'leggings', 'boots'):
        I[f'infernal_steel_{part}'] = armor(part, STEEL[:4])
    I['sovereign_crown'] = armor('crown', STEEL)
    # challenge horns & banner
    for fid, (stops, band, mouth) in {'ash': (['#2a2420', '#6a5a50', '#c0b0a0'], '#ff8a2a', '#3a1a10'),
                                      'blood': (['#3a0408', '#8a1018', '#e04050'], '#ffd060', '#2a0004'),
                                      'shadow': (['#0a0614', '#3a1a60', '#9a6ae0'], '#e0c0ff', '#05020a'),
                                      'legion': (['#2a1a04', '#8a6010', '#e0c040'], '#3a3a40', '#1a1004'),
                                      'soul': (['#a09a88', '#d8d2c0', '#ffffff'], '#40d0f0', '#0a2a30')}.items():
        I[f'{fid}_war_horn'] = horn(stops, band, mouth)
    I['ally_banner'] = banner('#8a0a18', '#ffd040')
    return I
