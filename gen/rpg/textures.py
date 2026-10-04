"""Procedural textures for the RPG content (monsters, bosses, summons, projectiles, effect icons)."""
import os, zlib, math
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/krolasyonbosses')
ENT = os.path.join(ASSETS, 'textures/entity/rpg')


def rgb(c):
    return np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255], dtype=np.float64)


def seed_of(s):
    return zlib.crc32(s.encode()) & 0xFFFFFFFF


def vnoise(h, w, rng, cell=4.0, octaves=3):
    out = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for _ in range(octaves):
        gh, gw = int(h / cell) + 3, int(w / cell) + 3
        g = rng.random((gh, gw))
        ys = np.arange(h) / cell
        xs = np.arange(w) / cell
        y0 = ys.astype(int); x0 = xs.astype(int)
        fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        a = g[y0][:, x0]; b = g[y0][:, x0 + 1]; c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy)
        tot += amp
        amp *= 0.5
        cell = max(1.0, cell / 2)
    return out / tot


def shade(base, f):
    """f: HxW multiplier around 1"""
    return np.clip(base[None, None, :] * f[:, :, None], 0, 255)


def mix(a, b, t):
    t = t[:, :, None] if np.ndim(t) == 2 else t
    return a * (1 - t) + b * t


def lines_mask(h, w, rng, count, length, thick=1, branch=0.3):
    m = np.zeros((h, w))
    for _ in range(count):
        y, x = rng.random() * h, rng.random() * w
        ang = rng.random() * math.pi * 2
        for _ in range(int(length)):
            ang += (rng.random() - 0.5) * 0.9
            y += math.sin(ang); x += math.cos(ang)
            iy, ix = int(y) % h, int(x) % w
            m[iy, ix] = 1
            if thick > 1:
                m[(iy + 1) % h, ix] = 1
            if rng.random() < branch * 0.05:
                ang += (rng.random() - 0.5) * 2
    return m


def pattern(name, h, w, base, sec, acc, rng):
    """returns (rgb HxWx3, glowmask HxW, alpha HxW)"""
    n = vnoise(h, w, rng, 4, 3)
    fine = vnoise(h, w, rng, 1.5, 2)
    glow = np.zeros((h, w))
    alpha = np.full((h, w), 255.0)
    yy, xx = np.mgrid[0:h, 0:w]
    if name == 'fur':
        streak = vnoise(h, w, rng, 1.2, 1) * 0.5 + np.repeat(vnoise(h // 4 + 1, w, rng, 1, 1), 4, axis=0)[:h] * 0.5
        img = shade(base, 0.75 + 0.45 * n + 0.25 * (streak - 0.5))
        img = mix(img, shade(sec, np.ones((h, w))), np.clip((n - 0.62) * 3, 0, 0.6))
    elif name == 'scales':
        sy = (yy % 4)
        sx = ((xx + (yy // 4 % 2) * 2) % 4)
        edge = ((sy == 3) | (sx == 0)).astype(float)
        img = shade(base, 0.85 + 0.3 * n - 0.3 * edge + 0.15 * (sy == 0))
        img = mix(img, shade(sec, np.ones((h, w))), np.clip(n - 0.55, 0, 1) * 1.5)
    elif name == 'spots':
        img = shade(base, 0.8 + 0.35 * n)
        for _ in range(int(h * w / 90)):
            cy, cx, r = rng.random() * h, rng.random() * w, 1 + rng.random() * 2.5
            d = (yy - cy) ** 2 + (xx - cx) ** 2
            img[d < r * r] = sec * (0.8 + 0.3 * rng.random())
    elif name == 'stripes':
        band = (np.sin((xx * 0.5 + yy * 0.25 + n * 6)) > 0.45).astype(float)
        img = shade(base, 0.85 + 0.3 * n)
        img = mix(img, shade(sec, 0.8 + 0.2 * fine), band)
    elif name == 'bone':
        img = shade(base, 0.82 + 0.25 * n)
        cr = lines_mask(h, w, rng, h * w // 300, 10)
        img = mix(img, shade(base, np.full((h, w), 0.55)), cr * 0.8)
        ribs = ((yy % 5) == 0).astype(float) * 0.25
        img = img * (1 - ribs[:, :, None] * 0.6)
    elif name == 'rock':
        blocks = vnoise(h, w, rng, 6, 1)
        img = shade(base, 0.7 + 0.5 * blocks + 0.15 * fine)
        cr = lines_mask(h, w, rng, h * w // 250, 14)
        img = mix(img, shade(base, np.full((h, w), 0.4)), cr * 0.9)
        img = mix(img, shade(acc, np.ones((h, w))), np.clip((n - 0.75) * 3, 0, 0.6))
    elif name == 'crystal':
        pts = rng.random((24, 2)) * [h, w]
        d = np.full((h, w), 1e9); idx = np.zeros((h, w), int)
        for i, (py, px) in enumerate(pts):
            dd = (yy - py) ** 2 + (xx - px) ** 2
            m = dd < d; d[m] = dd[m]; idx[m] = i
        bright = rng.random(24)[idx]
        img = mix(shade(base, 0.7 + 0.6 * bright), shade(sec, np.ones((h, w))), np.clip(bright - 0.6, 0, 1) * 1.5)
        edge = np.zeros((h, w)); edge[:, 1:] += idx[:, 1:] != idx[:, :-1]; edge[1:, :] += idx[1:, :] != idx[:-1, :]
        img = mix(img, np.full((h, w, 3), 255.0), np.clip(edge, 0, 1) * 0.6)
        glow = np.clip(edge, 0, 1) * 0.7 + (bright > 0.85) * 0.5
    elif name == 'slime':
        img = shade(base, 0.8 + 0.4 * vnoise(h, w, rng, 8, 2))
        for _ in range(h * w // 120):
            cy, cx, r = rng.random() * h, rng.random() * w, 0.8 + rng.random() * 1.6
            dd = (yy - cy) ** 2 + (xx - cx) ** 2
            img[dd < r * r] = np.clip(base * 1.4 + 30, 0, 255)
        alpha[:] = 200
    elif name == 'bark':
        groove = np.sin(xx * 1.3 + vnoise(h, w, rng, 3, 2) * 8)
        img = shade(base, 0.75 + 0.25 * groove + 0.2 * fine)
        img = mix(img, shade(sec, np.ones((h, w))), np.clip(vnoise(h, w, rng, 5, 2) - 0.62, 0, 1) * 2.5)
    elif name == 'cloth':
        weave = ((xx + yy) % 2) * 0.06 + ((xx // 2 + yy // 2) % 2) * 0.04
        folds = np.sin(xx * 0.7 + n * 5) * 0.12
        img = shade(base, 0.85 + weave + folds + 0.1 * fine)
        trim = ((yy % 16) < 2).astype(float)
        img = mix(img, shade(acc, np.ones((h, w))), trim * 0.85)
    elif name == 'metal':
        plate = (((yy % 8) == 0) | ((xx % 12) == 0)).astype(float)
        rivet = (((yy % 8) == 2) & ((xx % 6) == 3)).astype(float)
        img = shade(base, 0.8 + 0.25 * vnoise(h, w, rng, 10, 1) + 0.15 * ((yy % 8) == 1) - 0.35 * plate + 0.4 * rivet)
        img = mix(img, shade(sec, np.ones((h, w))), np.clip(n - 0.7, 0, 1) * 2)
        rust = np.clip(vnoise(h, w, rng, 3, 2) - 0.7, 0, 1) * 2
        img = mix(img, shade(np.array([140, 80, 40.0]), np.ones((h, w))), rust * 0.4)
    elif name == 'ghost':
        img = shade(base, 0.8 + 0.4 * vnoise(h, w, rng, 6, 2))
        alpha = 120 + 110 * vnoise(h, w, rng, 5, 2)
        glow = np.clip(n - 0.6, 0, 1) * 1.5
    elif name == 'magma':
        img = shade(base, 0.7 + 0.5 * n)
        cr = lines_mask(h, w, rng, h * w // 160, 16, 1)
        cr2 = np.clip(vnoise(h, w, rng, 3, 2) - 0.68, 0, 1) * 3
        hot = np.clip(cr + cr2, 0, 1)
        img = mix(img, shade(sec, 0.9 + 0.3 * fine), hot)
        img = mix(img, shade(acc, np.ones((h, w))), np.clip(cr2 - 0.5, 0, 1))
        glow = hot
    elif name == 'feather':
        chev = np.sin(yy * 1.2 + np.abs((xx % 8) - 4) * 1.4)
        img = shade(base, 0.82 + 0.15 * chev + 0.2 * n)
        tips = ((yy % 6) == 5).astype(float)
        img = mix(img, shade(sec, np.ones((h, w))), tips * 0.5 + np.clip(n - 0.65, 0, 1) * 2)
    elif name == 'chitin':
        band = np.sin(yy * 0.9) * 0.5 + 0.5
        img = shade(base, 0.65 + 0.45 * band + 0.1 * fine)
        hi = ((yy % 7) == 1).astype(float)
        img = mix(img, shade(sec, 1.1 * np.ones((h, w))), hi * 0.6)
    elif name == 'flesh':
        img = shade(base, 0.8 + 0.35 * n)
        veins = lines_mask(h, w, rng, h * w // 200, 12)
        img = mix(img, shade(sec, np.ones((h, w))), veins * 0.8)
        stitches = (((xx % 16) == 7) & ((yy % 3) != 0)).astype(float)
        img = mix(img, np.full((h, w, 3), 30.0), stitches * 0.6)
    elif name == 'sand':
        img = shade(base, 0.85 + 0.2 * fine + 0.15 * np.sin(yy * 0.6 + n * 4))
    elif name == 'ice':
        img = shade(base, 0.85 + 0.3 * n)
        cr = lines_mask(h, w, rng, h * w // 260, 12)
        img = mix(img, np.full((h, w, 3), 255.0), cr * 0.7)
        streak = np.clip(np.sin((xx - yy) * 0.35) - 0.85, 0, 1) * 5
        img = mix(img, np.full((h, w, 3), 255.0), streak * 0.5)
        glow = cr * 0.3
    elif name == 'void':
        img = shade(base, 0.6 + 0.6 * n)
        img = mix(img, shade(sec, np.ones((h, w))), np.clip(vnoise(h, w, rng, 7, 2) - 0.55, 0, 1) * 2)
        stars = (rng.random((h, w)) > 0.985).astype(float)
        img = mix(img, np.full((h, w, 3), 255.0), stars)
        glow = stars + np.clip(vnoise(h, w, rng, 6, 2) - 0.7, 0, 1) * 1.5
    elif name == 'coral':
        img = shade(base, 0.8 + 0.3 * n)
        for _ in range(h * w // 50):
            cy, cx = rng.random() * h, rng.random() * w
            dd = (yy - cy) ** 2 + (xx - cx) ** 2
            img[dd < 1.6] = np.clip(sec * 1.2, 0, 255)
        glow = np.clip(n - 0.7, 0, 1)
    elif name == 'mushroom':
        img = shade(sec, 0.85 + 0.25 * n)
        for _ in range(h * w // 70):
            cy, cx, r = rng.random() * h, rng.random() * w, 1 + rng.random() * 1.8
            dd = (yy - cy) ** 2 + (xx - cx) ** 2
            img[dd < r * r] = np.array([240, 235, 225.0])
        glow = np.clip(vnoise(h, w, rng, 3, 1) - 0.8, 0, 1) * 2
    elif name == 'rune':
        img = shade(base, 0.75 + 0.4 * n)
        r = np.zeros((h, w))
        for _ in range(h * w // 180):
            cy, cx = int(rng.random() * (h - 5)), int(rng.random() * (w - 5))
            k = rng.integers(0, 4)
            if k == 0: r[cy:cy + 5, cx + 2] = 1; r[cy + 2, cx:cx + 5] = 1
            elif k == 1: r[cy, cx:cx + 5] = 1; r[cy:cy + 5, cx] = 1; r[cy + 4, cx:cx + 5] = 1
            elif k == 2:
                for i in range(5): r[cy + i, cx + i] = 1; r[cy + i, cx + 4 - i] = 1
            else: r[cy:cy + 5, cx] = 1; r[cy:cy + 5, cx + 4] = 1; r[cy + 2, cx:cx + 5] = 1
        img = mix(img, shade(acc, np.ones((h, w))), r)
        glow = r
    else:
        img = shade(base, 0.8 + 0.4 * n)
    return np.clip(img, 0, 255), np.clip(glow, 0, 1), np.clip(alpha, 0, 255)


GHOSTS = {'will_o_wisp', 'sand_wraith', 'wind_elemental', 'wraith', 'banshee', 'thunder_jelly', 'pirate_ghost', 'ice_wraith', 'highway_ghost'}


def monster_texture(mid, colors, pat, boss=False, glow_on=False, translucent=False):
    base, sec, acc, eye = [rgb(c) for c in colors]
    rng = np.random.default_rng(seed_of(mid))
    W = H = 128
    img = np.zeros((H, W, 4))
    glow = np.zeros((H, W, 4))

    def put(x, y, w, h, rgbimg, alpha=None, gl=None, gcol=None):
        img[y:y + h, x:x + w, :3] = rgbimg
        img[y:y + h, x:x + w, 3] = 255 if alpha is None else alpha
        if gl is not None and gcol is not None:
            glow[y:y + h, x:x + w, :3] = gcol
            glow[y:y + h, x:x + w, 3] = np.maximum(glow[y:y + h, x:x + w, 3], gl * 255)

    # A body
    a_rgb, a_glow, a_alpha = pattern(pat, 64, 64, base, sec, acc, rng)
    if boss:
        trim = np.zeros((64, 64)); trim[::12, :] = 1; trim[:, ::16] = 0.5
        a_rgb = mix(a_rgb, shade(acc, np.ones((64, 64))), trim * 0.35)
    put(0, 0, 64, 64, a_rgb, a_alpha if translucent else None, a_glow if glow_on else None, eye if pat != 'magma' else sec)
    # B secondary
    b_rgb, b_glow, b_alpha = pattern(pat if pat in ('fur', 'scales', 'feather', 'chitin', 'rock', 'metal', 'bark') else 'fur', 64, 64, sec, base, acc, rng)
    b_rgb = mix(b_rgb, shade(base, 0.9 * np.ones((64, 64))), np.full((64, 64), 0.25))
    put(64, 0, 64, 64, b_rgb, b_alpha if translucent else None)
    # C accent: gradient bands
    yy, xx = np.mgrid[0:32, 0:64]
    grad = 0.75 + 0.45 * (1 - yy / 31.0) + 0.1 * vnoise(32, 64, rng, 2, 2)
    c_rgb = shade(acc, grad)
    put(0, 64, 64, 32, c_rgb, None, (vnoise(32, 64, rng, 3, 1) > 0.75) * 0.6 if glow_on and boss else None, acc)
    # D eyes: bright eye colour with soft centre highlight
    yy, xx = np.mgrid[0:16, 0:32]
    e_rgb = shade(eye, 0.9 + 0.2 * np.sin(xx * 1.7) * np.sin(yy * 1.7))
    e_rgb = np.clip(e_rgb + 25, 0, 255)
    put(64, 64, 32, 16, e_rgb, None, np.ones((16, 32)), np.clip(eye * 1.1 + 20, 0, 255))
    # gem / core strip (D+8 row) slightly darker
    put(64, 80, 32, 16, shade(eye, 0.8 + 0.3 * vnoise(16, 32, rng, 2, 1)), None, np.ones((16, 32)) * (1.0 if glow_on else 0.0), eye)
    # E dark
    e2 = shade(base, 0.22 + 0.12 * vnoise(32, 32, rng, 3, 1))
    e2[16:, :] = np.clip(shade(acc, np.full((16, 32), 0.9)), 0, 255)  # teeth / rattle region (EV+16)
    e2[8:16, :] = shade(base, np.full((8, 32), 0.15))
    put(96, 64, 32, 32, e2)
    # F membrane
    mem = sec * 0.6 + base * 0.4
    f_rgb, _, f_alpha = pattern('ghost' if translucent else 'flesh' if pat == 'flesh' else 'sand', 32, 128, mem, base, acc, rng)
    veins = np.zeros((32, 128)); veins[:, ::10] = 1; veins[::8, :] = 0.6
    f_rgb = mix(f_rgb, shade(base, np.full((32, 128), 0.55)), veins * 0.6)
    put(0, 96, 128, 32, f_rgb, f_alpha if translucent else np.full((32, 128), 235.0))
    tex = Image.fromarray(img.astype(np.uint8), 'RGBA')
    gl = Image.fromarray(glow.astype(np.uint8), 'RGBA')
    return tex, gl


def orb_textures():
    s = 32
    yy, xx = np.mgrid[0:s, 0:s]
    d = np.sqrt((yy - 15.5) ** 2 + (xx - 15.5) ** 2) / 15.5
    a = np.clip(1 - d, 0, 1) ** 1.6
    img = np.zeros((s, s, 4))
    img[..., :3] = 255
    img[..., 3] = a * 255
    Image.fromarray(img.astype(np.uint8), 'RGBA').save(os.path.join(ENT, 'orb.png'))
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    rays = np.clip(np.cos(ang * 6) * 0.5 + 0.5, 0, 1) ** 4
    a2 = np.clip(rays * np.clip(1 - d, 0, 1) * 1.4 + np.clip(0.5 - d, 0, 1), 0, 1)
    img[..., 3] = a2 * 255
    Image.fromarray(img.astype(np.uint8), 'RGBA').save(os.path.join(ENT, 'orb_star.png'))


SUMMONS = {
    'summon_wolf': ((0xB0B0B8, 0x707078, 0xE0E0F0, 0x80C0FF), 'fur'),
    'summon_spirit_wolf': ((0x60FFE0, 0x2090A0, 0xD0FFF8, 0xFFFFFF), 'ghost'),
    'summon_skeleton': ((0xE0D8C0, 0x5A5040, 0x40A0A0, 0x40FFFF), 'bone'),
    'summon_angel': ((0xFFF4D0, 0xFFFFFF, 0xFFD040, 0x80E0FF), 'feather'),
    'summon_treant': ((0x5A4028, 0x50A030, 0xA0E050, 0xFFE080), 'bark'),
    'summon_blood_golem': ((0x8A1018, 0x4A0008, 0xFF4040, 0xFFC0C0), 'flesh'),
    'summon_spirit_warrior': ((0x60FFE0, 0x208080, 0xFFFFFF, 0xFFFFFF), 'ghost'),
    'summon_mirror': ((0xE0E8F0, 0xA0A8C0, 0xFFFFFF, 0x80C0FF), 'crystal'),
}


def effect_icons():
    out = os.path.join(ASSETS, 'textures/mob_effect')
    os.makedirs(out, exist_ok=True)
    icons = {'bleed': 0xC01020, 'stun': 0xF0E060, 'mana_flow': 0x40A0FF, 'divine_shield': 0xFFF0A0, 'blood_pact': 0x800010, 'thorn_aura': 0x60C040}
    for name, col in icons.items():
        c = rgb(col)
        img = np.zeros((18, 18, 4))
        yy, xx = np.mgrid[0:18, 0:18]
        if name == 'bleed':
            m = ((xx - 9) ** 2 / 16 + (yy - 11) ** 2 / 25 < 1) | ((np.abs(xx - 9) < (yy - 2) * 0.45) & (yy < 10) & (yy > 2))
        elif name == 'stun':
            m = np.zeros((18, 18), bool)
            for k in range(5):
                a = k * 2 * math.pi / 5 - math.pi / 2
                m |= (np.abs(xx - 9 - math.cos(a) * 5) < 1.6) & (np.abs(yy - 9 - math.sin(a) * 5) < 1.6)
            m |= (np.abs(xx - 9) + np.abs(yy - 9)) < 3
        elif name == 'mana_flow':
            m = ((xx - 9) ** 2 / 20 + (yy - 11) ** 2 / 20 < 1) | ((np.abs(xx - 9) < (yy - 1) * 0.5) & (yy < 9) & (yy > 1))
        elif name == 'divine_shield':
            m = (np.abs(xx - 9) < 7 - np.clip(yy - 9, 0, 9) * 0.8) & (yy > 1) & (yy < 17)
        elif name == 'blood_pact':
            m = ((xx - 6) ** 2 + (yy - 7) ** 2 < 14) | ((xx - 12) ** 2 + (yy - 7) ** 2 < 14) | ((np.abs(xx - 9) < (16 - yy) * 0.9) & (yy > 7) & (yy < 16))
        else:
            m = np.zeros((18, 18), bool)
            for k in range(8):
                a = k * math.pi / 4
                for r in range(2, 9):
                    m[int(9 + math.sin(a) * r), int(9 + math.cos(a) * r)] = True
            m |= (xx - 9) ** 2 + (yy - 9) ** 2 < 9
        shade_ = 0.7 + 0.5 * (1 - yy / 17)
        img[m, :3] = np.clip(c[None, :] * shade_[m][:, None] + 20, 0, 255)
        img[m, 3] = 255
        edge = m & ~(np.roll(m, 1, 0) & np.roll(m, -1, 0) & np.roll(m, 1, 1) & np.roll(m, -1, 1))
        img[edge, :3] = c * 0.4
        Image.fromarray(img.astype(np.uint8), 'RGBA').save(os.path.join(out, name + '.png'))


def main(defs):
    os.makedirs(ENT, exist_ok=True)
    for m in defs.MONSTERS + defs.BOSSES:
        tex, gl = monster_texture(m.id, m.colors, m.pattern, m.boss, m.glow, m.id in GHOSTS or m.arch == 'SLIME')
        tex.save(os.path.join(ENT, m.id + '.png'))
        if m.glow:
            gl.save(os.path.join(ENT, m.id + '_glow.png'))
    for name, (cols, pat) in SUMMONS.items():
        tex, _ = monster_texture(name, cols, pat, False, True, pat == 'ghost')
        tex.save(os.path.join(ENT, name + '.png'))
    orb_textures()
    effect_icons()
