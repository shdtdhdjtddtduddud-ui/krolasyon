"""Item models: weapons (diagonal 3D blades), potions (glass + glowing liquid), keys, stones, materials."""
import math
import numpy as np
import tex as T
from itemmodel import ItemModel, HANDHELD, OBJECT

H = T.hexc
ROT45 = ('z', -45, [8, 8, 8])


# ------------------------------------------------------------------ materials

def m_blade(base, edge='#FFFFFF', dark=None, pattern=None, glowline=None):
    b = H(base)
    e = H(edge)
    d = H(dark) if dark else b * np.array([0.45, 0.45, 0.5, 1], np.float32)

    def f(w, h, seed, face):
        xs = np.linspace(-1, 1, w, dtype=np.float32)[None, :]
        n = T.value_noise(w, h, max(2, w / 2), seed, 2)
        center = 1 - np.abs(xs)  # ridge in the middle
        t = np.clip(0.35 + center * 0.45 + (n - 0.5) * 0.25, 0, 1)
        img = T.ramp([d, b, np.clip(b * 1.35, 0, 1)], np.broadcast_to(t, (h, w)))
        img[:, :, 3] = 1
        if w >= 3:
            T.rect(img, 0, 0, 1, h, np.append(e[:3], 0.9))
            T.rect(img, w - 1, 0, w, h, np.append(e[:3], 0.7))
        if pattern == 'rust':
            sp = T.value_noise(w, h, max(1.5, w / 3), seed + 4, 2)
            T.draw_mask(img, np.clip((sp - 0.55) * 5, 0, 1) * 0.8, H('#7A3A14'))
        if pattern == 'damascus':
            ys = np.arange(h)[:, None]
            wave = (np.sin(ys * 0.9 + np.sin(np.arange(w)[None, :] * 1.3) * 2 + n * 4) + 1) / 2
            img = T.shade(img, 0.85 + wave * 0.3)
        if glowline and w >= 3:
            T.rect(img, w // 2 - max(0, w // 8), 0, w // 2 + max(1, w // 8), h, H(glowline))
        return img
    return f


def m_glass(tint, alpha=0.35):
    t = H(tint)

    def f(w, h, seed, face):
        img = T.new(w, h, np.array([t[0], t[1], t[2], alpha], np.float32))
        xs = np.linspace(0, 1, w)[None, :]
        hl = np.exp(-((xs - 0.25) / 0.12) ** 2) * 0.55
        img[:, :, :3] = np.clip(img[:, :, :3] + hl[:, :, None], 0, 1)
        img[:, :, 3] = np.clip(alpha + hl * 0.6, 0, 1)
        if w > 2 and h > 2:
            img[0, :, 3] = img[-1, :, 3] = 0.7
            img[:, 0, 3] = img[:, -1, 3] = 0.6
        return img
    return f


def m_liquid(color, light=None):
    c = H(color)
    l = H(light) if light else np.clip(c * 1.6 + 0.15, 0, 1)

    def f(w, h, seed, face):
        n = T.value_noise(w, h, max(1.5, w / 3), seed, 2)
        ys = np.linspace(0, 1, h)[:, None]
        t = np.clip(n * 0.5 + (1 - ys) * 0.5, 0, 1)
        img = T.ramp([c * np.array([0.6, 0.6, 0.6, 1], np.float32), c, l], np.broadcast_to(t, (h, w)))
        img[:, :, 3] = 1
        r = T.rng(seed)
        for _ in range(max(1, w * h // 25)):  # bubbles
            x, y = r.uniform(0, w), r.uniform(0, h)
            T.draw_mask(img, T.ellipse_mask(w, h, x, y, 0.8, 0.8, 0.5), np.append(l[:3], 0.8))
        return img
    return f


def m_gem(color):
    c = H(color)

    def f(w, h, seed, face):
        ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
        d = np.sqrt(((xs + 0.5) / w - 0.35) ** 2 + ((ys + 0.5) / h - 0.3) ** 2)
        t = np.clip(1 - d * 1.6, 0, 1)
        img = T.ramp([c * np.array([0.35, 0.35, 0.35, 1], np.float32), c, np.array([1, 1, 1, 1], np.float32)], t)
        img[:, :, 3] = 1
        return img
    return f


def m_paper(base='#EEE6D2', lines='#4A4A4A', photo=False):
    b = H(base)

    def f(w, h, seed, face):
        img = T.colorize(b, T.value_noise(w, h, max(2, w / 3), seed, 2), 0.9, 1.05)
        if face == 'top' and h > 6:
            for y in range(3, h - 2, 3):
                T.rect(img, 2, y, w - 2, y + 1, np.append(H(lines)[:3], 0.55))
            if photo:
                T.rect(img, 2, 2, w // 3, h // 2, H('#5A6A80'))
        return img
    return f


# ------------------------------------------------------------------ building blocks

def blade(im, y0, length, width, thick, mat, tip_taper=0.35, x=8, z=8, curve=0.0, seg=2.0, glowcore=None):
    """Stack of segments narrowing to the tip; returns top y."""
    n = max(1, int(length / seg))
    for i in range(n):
        t = i / n
        w = width * (1 - max(0, t - (1 - tip_taper)) / tip_taper * 0.75) if t > 1 - tip_taper else width
        cx = x + curve * (t ** 2) * length / 4
        y = y0 + i * seg
        h = min(seg, y0 + length - y)
        im.cel(cx, y, z, max(0.4, w), h, thick, mat)
        if glowcore and w > 1:
            im.cel(cx, y, z, max(0.25, w * 0.25), h, thick + 0.1, glowcore, glow=True)
    tip_x = x + curve * length / 4
    im.cel(tip_x, y0 + length, z, max(0.3, width * 0.2), 1, max(0.3, thick * 0.6), mat)
    return y0 + length


def grip(im, y0, length, w, mat, pommel=None, pommel_glow=False):
    im.cel(8, y0, 8, w, length, w, mat)
    for k in range(int(length / 1.5)):
        im.cel(8, y0 + 0.4 + k * 1.5, 8, w + 0.2, 0.4, w + 0.2, T.m_leather('#1A1210'))
    if pommel:
        im.cel(8, y0 - 1, 8, w + 0.8, 1, w + 0.8, pommel, glow=pommel_glow)


def weapon(name):
    im = ItemModel(name, density=4)
    im.global_rot = ROT45
    return im


# ------------------------------------------------------------------ weapons

WEAPONS = {}


def wdef(name, display=HANDHELD):
    def deco(fn):
        WEAPONS[name] = (fn, display)
        return fn
    return deco


@wdef('rusty_dagger')
def rusty_dagger():
    im = weapon('rusty_dagger')
    grip(im, 1, 4, 1.4, T.m_leather('#5A3A22'), T.m_metal('#6A5A4A', rivets=False))
    im.cel(8, 5, 8, 4, 1, 1.6, T.m_metal('#7A6048', rivets=False))
    blade(im, 6, 8, 2.4, 0.6, m_blade('#8C8070', '#C8B8A0', pattern='rust'))
    return im


@wdef('steel_dagger')
def steel_dagger():
    im = weapon('steel_dagger')
    grip(im, 1, 4, 1.4, T.m_leather('#2A2A30'), T.m_metal('#9AA4B0', rivets=False))
    im.cel(8, 5, 8, 4.5, 1, 1.8, T.m_metal('#B8C2CC', rivets=False))
    blade(im, 6, 9, 2.4, 0.6, m_blade('#C4CCD6', '#FFFFFF'))
    return im


@wdef('kasaka_venom_fang')
def kasaka_venom_fang():
    im = weapon('kasaka_venom_fang')
    grip(im, 0.5, 5, 1.6, T.m_scales('#2C6AB8', '#7FC0FF', '#0E2A55', 2), m_gem('#9A4AFF'), True)
    im.cel(8, 5.5, 8, 3.2, 1.2, 2.2, T.m_scales('#1C4E92', '#5FA8FF', '#081C3E', 2))
    blade(im, 6.7, 10, 2.8, 1.0, m_blade('#E8E0C8', '#FFFFFF', dark='#9A8A6A'), tip_taper=0.6, curve=1.2, glowcore=T.m_glow('#B04AFF', '#F0C0FF'))
    return im


@wdef('knight_killer')
def knight_killer():
    im = weapon('knight_killer')
    grip(im, 0.5, 4.5, 1.5, T.m_leather('#1A1414'), m_gem('#E02A2A'), True)
    im.cel(8, 5, 8, 5, 1.2, 1.8, T.m_metal('#2A2A30', trim='#B01A1A'))
    im.cel(6, 5.6, 8, 1, 1.4, 1.2, T.m_metal('#3A3A40'))
    im.cel(10, 5.6, 8, 1, 1.4, 1.2, T.m_metal('#3A3A40'))
    blade(im, 6.2, 10, 2.8, 0.7, m_blade('#3A3E48', '#FF4A4A', dark='#14161C'), tip_taper=0.5)
    return im


@wdef('baruka_dagger')
def baruka_dagger():
    im = weapon('baruka_dagger')
    grip(im, 0.5, 4.5, 1.4, T.m_leather('#1A2A4A'), m_gem('#7FE6FF'), True)
    im.cel(8, 5, 8, 4.2, 1, 1.8, T.m_metal('#BFE8FF', rivets=False))
    blade(im, 6, 11, 2.2, 0.7, m_blade('#9FE6FF', '#FFFFFF', dark='#3A8AB0'), glowcore=T.m_glow('#CFF6FF'))
    for k, y in enumerate((8, 11, 14)):
        im.cel(8 + (1.4 if k % 2 else -1.4), y, 8, 0.6, 1.4, 0.5, T.m_crystal('#BFF2FF'), glow=True)
    return im


@wdef('demon_king_dagger')
def demon_king_dagger():
    im = weapon('demon_king_dagger')
    grip(im, 0.5, 4.5, 1.5, T.m_leather('#1A1420'), m_gem('#B08AFF'), True)
    im.cel(8, 5, 8, 5, 1.2, 2, T.m_metal('#1E1A26', trim='#7A3AFF'))
    for s in (-1, 1):
        im.cel(8 + s * 2.6, 6.2, 8, 0.8, 1.6, 1, T.m_bone('#2A2026'))
    blade(im, 6.2, 11, 2.6, 0.8, m_blade('#3A2E52', '#C8A8FF', dark='#120E1C', pattern='damascus'), glowcore=T.m_glow('#9A6AFF', '#E8D8FF'))
    return im


@wdef('kamish_wrath')
def kamish_wrath():
    im = weapon('kamish_wrath')
    grip(im, 0, 5, 1.6, T.m_leather('#2A0E0E'), m_gem('#FF5A2A'), True)
    im.cel(8, 5, 8, 6, 1.4, 2.2, T.m_metal('#2A1414', trim='#FF7A2A'))
    for s in (-1, 1):  # dragon wing guard
        im.cel(8 + s * 3.3, 5.6, 8, 1, 2.4, 1.2, T.m_scales('#3A0E0E', '#FF5A2A', '#140404', 2))
        im.cel(8 + s * 3.8, 7.6, 8, 0.6, 1.4, 0.8, T.m_bone('#1A0A0A'))
    blade(im, 6.4, 13, 3.0, 0.9, m_blade('#4A1A1A', '#FFB070', dark='#1A0606', pattern='damascus'), tip_taper=0.45, curve=0.6,
          glowcore=T.m_glow('#FF4A1A', '#FFE0A0'))
    return im


@wdef('hunter_sword')
def hunter_sword():
    im = weapon('hunter_sword')
    grip(im, 0, 5, 1.4, T.m_leather('#3A2A1E'), T.m_metal('#8A949E', rivets=False))
    im.cel(8, 5, 8, 6, 1, 1.6, T.m_metal('#8A949E', rivets=False))
    blade(im, 6, 17, 2.4, 0.6, m_blade('#C4CCD6', '#FFFFFF'))
    return im


@wdef('crimson_greatsword')
def crimson_greatsword():
    im = weapon('crimson_greatsword')
    grip(im, -2, 7, 1.8, T.m_leather('#5E0E14'), m_gem('#FF2A2A'), True)
    im.cel(8, 5, 8, 9, 1.6, 2.4, T.m_metal('#D4A841', rivets=False))
    for s in (-1, 1):
        im.cel(8 + s * 4.3, 6.6, 8, 0.8, 1.6, 1.6, T.m_metal('#D4A841', rivets=False))
    blade(im, 6.6, 23, 4.0, 1.0, m_blade('#4A4E58', '#FF4A4A', dark='#1E2026'), tip_taper=0.2, glowcore=m_blade('#8C1622', '#D4A841'))
    return im


@wdef('demon_king_longsword')
def demon_king_longsword():
    im = weapon('demon_king_longsword')
    grip(im, -1, 6, 1.6, T.m_leather('#1A1420'), m_gem('#C08AFF'), True)
    im.cel(8, 5, 8, 7, 1.4, 2, T.m_metal('#1A1420', trim='#7A3AFF'))
    for s in (-1, 1):
        im.cel(8 + s * 3.5, 6.2, 8, 0.8, 2.2, 1, T.m_bone('#2A2026'))
    blade(im, 6.4, 22, 2.8, 0.8, m_blade('#2A2238', '#D0B8FF', dark='#0E0A16', pattern='damascus'), tip_taper=0.25,
          glowcore=T.m_glow('#A07AFF', '#F0E8FF'))
    return im


@wdef('haein_sword')
def haein_sword():
    im = weapon('haein_sword')
    grip(im, 0, 5.5, 1.2, T.m_leather('#F0E8E0'), T.m_metal('#E0C060', rivets=False))
    im.cel(8, 5.5, 8, 4.5, 0.8, 1.4, T.m_metal('#E8C868', rivets=False))
    im.cel(8, 4.8, 8, 2, 0.7, 1.6, T.m_metal('#E8C868', rivets=False))
    blade(im, 6.3, 21, 1.8, 0.5, m_blade('#E6ECF2', '#FFFFFF', dark='#8A96A4'), tip_taper=0.15, glowcore=T.m_glow('#FFF4C8'))
    return im


@wdef('orc_war_axe')
def orc_war_axe():
    im = weapon('orc_war_axe')
    im.cel(8, -2, 8, 1.6, 24, 1.6, T.m_wood('#5A3B22'))
    for y in (2, 5, 8):
        im.cel(8, y, 8, 1.9, 0.6, 1.9, T.m_leather('#2A1A10'))
    im.el((8.8, 14, 7.5), (14, 21, 8.5), m_blade('#6A6E74', '#D8DCE0', pattern='rust'))
    im.el((13.5, 12.5, 7.6), (15, 22.5, 8.4), m_blade('#7A7E84', '#FFFFFF'))
    im.el((4.5, 16, 7.6), (7.2, 19, 8.4), T.m_metal('#5A5E64'))
    im.cel(8, 22, 8, 1, 2, 1, T.m_bone('#E8E0C8'))
    return im


@wdef('flame_staff')
def flame_staff():
    im = weapon('flame_staff')
    im.cel(8, -3, 8, 1.4, 22, 1.4, T.m_wood('#4A1E14'))
    im.cel(8, -3.5, 8, 2, 1, 2, T.m_metal('#D9A93A', rivets=False))
    for s in (-1, 1):
        im.cel(8 + s * 1.6, 17, 8, 0.7, 4, 0.7, T.m_metal('#D9A93A', rivets=False))
    im.cel(8, 18, 8, 2.6, 3.4, 2.6, T.m_glow('#FF6A1A', '#FFF0A0'), glow=True)
    im.cel(8, 19, 8, 1.4, 4, 1.4, T.m_glow('#FFB020', '#FFFFFF'), glow=True)
    return im


@wdef('orb_of_avarice', OBJECT)
def orb_of_avarice():
    im = ItemModel('orb_of_avarice', density=4)
    im.cel(8, 4.5, 8, 4, 4, 4, T.m_glow('#B04AFF', '#FFE0FF'), glow=True)
    for (w, y, h) in ((4, 3, 1), (6, 4, 1), (7, 5, 3), (6, 8, 1), (4, 9, 1)):
        im.cel(8, y, 8, w, h, w, m_glass('#6A2AB0', 0.45), translucent=True)
    im.cel(8, 2.4, 8, 6, 0.6, 6, T.m_metal('#D9A93A', rivets=False))
    for s in (-1, 1):
        im.cel(8 + s * 3.2, 2.4, 8, 0.6, 4, 0.6, T.m_metal('#D9A93A', rivets=False))
        im.cel(8, 2.4, 8 + s * 3.2, 0.6, 4, 0.6, T.m_metal('#D9A93A', rivets=False))
    im.cel(8, 0.5, 8, 4, 2, 4, T.m_metal('#2A2030', trim='#D9A93A'))
    return im


# ------------------------------------------------------------------ potions

POTIONS = {}


def pdef(name):
    def deco(fn):
        POTIONS[name] = fn
        return fn
    return deco


def flask(im, profile, liquid, glass='#E8F4FF', cork='#8A6A44', fill=0.8, glow=True, neck_metal=None):
    """profile: list of (width, height) from bottom to top; last ones are the neck."""
    y = 0.5
    total = sum(h for _, h in profile)
    level = 0.5 + total * fill
    for i, (w, h) in enumerate(profile):
        im.cel(8, y, 8, w, h, w, m_glass(glass), translucent=True)
        top = min(y + h, level)
        if top > y + 0.2 and w > 1.4:
            im.cel(8, y + 0.15, 8, w - 0.8, top - y - 0.15, w - 0.8, m_liquid(liquid), glow=glow)
        y += h
    nw = profile[-1][0]
    if neck_metal:
        im.cel(8, y - 1.2, 8, nw + 0.6, 0.8, nw + 0.6, T.m_metal(neck_metal, rivets=False))
    im.cel(8, y, 8, max(1, nw - 0.4), 1.4, max(1, nw - 0.4), T.m_wood(cork) if cork.startswith('#8') or cork.startswith('#6') else T.m_metal(cork, rivets=False))
    return y + 1.4


@pdef('healing_potion')
def p_heal():
    im = ItemModel('healing_potion')
    flask(im, [(4, 1), (6, 1), (7, 3), (6, 1), (4, 1), (2, 2)], '#E0283A')
    return im


@pdef('greater_healing_potion')
def p_gheal():
    im = ItemModel('greater_healing_potion')
    flask(im, [(5, 1), (7, 1), (8, 4), (7, 1), (5, 1), (2.4, 3)], '#C8102E', neck_metal='#D9A93A')
    im.cel(8, 3.2, 8, 8.6, 1.2, 8.6, T.m_metal('#D9A93A', rivets=False))
    return im


@pdef('mana_potion')
def p_mana():
    im = ItemModel('mana_potion')
    flask(im, [(3.5, 7), (2.4, 1), (1.6, 2)], '#2A6AFF', cork='#C8D8E8')
    return im


@pdef('greater_mana_potion')
def p_gmana():
    im = ItemModel('greater_mana_potion')
    top = flask(im, [(5, 1), (6, 5), (5, 1), (2.4, 3)], '#1A3ADF', neck_metal='#C0C8D0', cork='#C0C8D0')
    im.cel(8, top, 8, 2, 2, 2, T.m_crystal('#6AB0FF'), glow=True)
    return im


@pdef('elixir_of_life')
def p_elixir():
    im = ItemModel('elixir_of_life')
    top = flask(im, [(5, 1), (7, 1), (8, 3), (7, 1), (5, 1), (2.4, 2)], '#3AE07A', glass='#F0FFF0', neck_metal='#E8C040', cork='#E8C040')
    for y in (1.6, 4.0):
        im.cel(8, y, 8, 8.4, 0.5, 8.4, T.m_metal('#E8C040', rivets=False))
    for s in (-1, 1):
        im.el((8 + s * 4.2 - (0 if s > 0 else 2), 4, 7.6), (8 + s * 4.2 + (2 if s > 0 else 0), 7, 8.4), T.m_metal('#E8C040', rivets=False))
    im.cel(8, top, 8, 1.6, 1.6, 1.6, m_gem('#3AFF8A'), glow=True)
    return im


@pdef('holy_water_of_life')
def p_holy():
    im = ItemModel('holy_water_of_life')
    flask(im, [(3, 1), (5, 1), (6, 2), (5, 2), (4, 2), (3, 1), (2, 1), (1.4, 2)], '#FFF4C8', glass='#FFFFFF', cork='#F0D070', neck_metal='#F0D070')
    for s in (-1, 1):
        im.el((8 + s * 3 - (0 if s > 0 else 3), 5, 7.8), (8 + s * 3 + (3 if s > 0 else 0), 8, 8.2), T.m_cloth('#FFFFFF', trim='#F0D070'))
        im.el((8 + s * 5.5 - (0 if s > 0 else 2), 6.5, 7.8), (8 + s * 5.5 + (2 if s > 0 else 0), 9.5, 8.2), T.m_cloth('#FFFFFF'))
    return im


@pdef('fatigue_recovery_potion')
def p_fatigue():
    im = ItemModel('fatigue_recovery_potion')
    flask(im, [(5, 6), (3, 1), (2, 2)], '#7ACF3A')
    im.el((5.4, 2, 5.4), (10.6, 4.5, 5.45), m_paper('#F4E8C8', '#3A6A1A'))
    return im


@pdef('strength_elixir')
def p_str():
    im = ItemModel('strength_elixir')
    flask(im, [(6, 5), (4, 1), (2, 2)], '#FF5A1A', neck_metal='#5A3A2A')
    im.el((5.0, 1.6, 4.95), (11.0, 4.6, 5.0), m_paper('#2A1A10', '#FF7A3A'))
    return im


@pdef('agility_elixir')
def p_agi():
    im = ItemModel('agility_elixir')
    flask(im, [(3, 1), (4, 6), (3, 1), (1.6, 2)], '#3AF0D0', cork='#C8D8E8')
    return im


@pdef('perception_draught')
def p_per():
    im = ItemModel('perception_draught')
    flask(im, [(4, 1), (6, 3), (5, 1), (3, 1), (2, 2)], '#B04AFF')
    im.cel(8, 2.4, 4.9, 2, 1.2, 0.2, T.m_glow('#FFE0FF', '#FFFFFF'), glow=True)
    return im


@pdef('stealth_tonic')
def p_stealth():
    im = ItemModel('stealth_tonic')
    flask(im, [(5, 1), (6, 4), (4, 1), (2, 2)], '#2A2A3A', glass='#B0B0C0', cork='#3A3A40', glow=False)
    return im


@pdef('antidote')
def p_anti():
    im = ItemModel('antidote')
    flask(im, [(3, 5), (2, 1), (1.4, 1.5)], '#3AD0C0', cork='#E8E8E8')
    return im


# ------------------------------------------------------------------ misc items

MISC = {}


def mdef(name, display=OBJECT):
    def deco(fn):
        MISC[name] = (fn, display)
        return fn
    return deco


def crystal_cluster(name, color):
    im = ItemModel(name)
    mat = T.m_crystal(color)
    im.cel(8, 0.5, 8, 3, 9, 3, mat, glow=True)
    im.el((4.5, 0.5, 6.5), (6.5, 6, 8.5), mat, glow=True)
    im.el((9.5, 0.5, 7.5), (11.5, 5, 9.5), mat, glow=True)
    im.el((6.5, 0.5, 9.5), (8.5, 4, 11.5), mat, glow=True)
    im.cel(8, 0, 8, 8, 1, 6, T.m_stone('#4A4A52'))
    return im


for rank, col in (('e', '#9AA7B8'), ('d', '#5FA8FF'), ('c', '#5CE07A'), ('b', '#F2D24B'), ('a', '#FF8A3D'), ('s', '#C86BFF')):
    MISC['magic_stone_' + rank] = ((lambda n=rank, c=col: crystal_cluster('magic_stone_' + n, c)), OBJECT)


@mdef('return_stone')
def return_stone():
    im = ItemModel('return_stone')
    im.cel(8, 0.5, 8, 7, 9, 3, T.m_stone('#3A4A6A', cracks=False))
    im.cel(8, 9.5, 8, 5, 1.5, 2.6, T.m_stone('#3A4A6A', cracks=False))
    for z in (6.4, 9.6):
        im.cel(8, 3, z, 3, 4, 0.2, T.m_glow('#5FD0FF', '#FFFFFF'), glow=True)
    return im


def key(name, metal, gem, bow_glow):
    im = ItemModel(name)
    im.global_rot = ROT45
    im.cel(8, 3, 8, 1.4, 11, 1.4, T.m_metal(metal, rivets=False))
    im.el((8.7, 3, 7.6), (11, 4.2, 8.4), T.m_metal(metal, rivets=False))
    im.el((8.7, 5, 7.6), (10.2, 6, 8.4), T.m_metal(metal, rivets=False))
    for (x0, y0, x1, y1) in ((5, 14, 11, 15), (5, 18, 11, 19), (5, 14, 6, 19), (10, 14, 11, 19)):
        im.el((x0, y0, 7.4), (x1, y1, 8.6), T.m_metal(metal, rivets=False))
    im.cel(8, 15.2, 8, 2.8, 2.6, 0.8, m_gem(gem), glow=bow_glow)
    return im


@mdef('instant_dungeon_key', HANDHELD)
def instant_key():
    return key('instant_dungeon_key', '#C0C8D8', '#4AA8FF', True)


@mdef('demon_castle_key', HANDHELD)
def demon_key():
    im = key('demon_castle_key', '#3A1A1A', '#FF2A2A', True)
    for s in (-1, 1):
        im.el((8 + s * 3.4 - 0.5, 19, 7.6), (8 + s * 3.4 + 0.5, 21.5, 8.4), T.m_bone('#1A1010'))
    return im


@mdef('random_box')
def random_box():
    im = ItemModel('random_box')
    im.cel(8, 0.5, 8, 10, 8, 10, T.m_wood('#6A4424'))
    for y in (0.5, 8):
        im.cel(8, y, 8, 10.4, 0.8, 10.4, T.m_metal('#D9A93A', rivets=False))
    im.cel(8, 0.5, 8, 1.2, 8.6, 10.4, T.m_metal('#D9A93A', rivets=False))
    for z in (2.85, 13.15):
        im.cel(8, 3, z, 3, 3, 0.2, T.m_glow('#5FD0FF', '#FFFFFF'), glow=True)
    im.cel(8, 8.6, 8, 8, 1, 8, T.m_wood('#7A5030'))
    return im


@mdef('hunter_license')
def license_():
    im = ItemModel('hunter_license')
    im.el((3, 0, 4), (13, 0.5, 12), m_paper('#E8EEF8', '#2A4A8A', photo=True))
    im.el((9.5, 0.5, 5), (12, 0.7, 7), m_gem('#D9A93A'))
    return im


@mdef('newspaper')
def newspaper():
    im = ItemModel('newspaper')
    im.el((2.5, 0, 3), (13.5, 0.6, 13), m_paper('#EDE6D4', '#2A2A2A', photo=True))
    im.el((2.5, 0.6, 3), (13.5, 0.8, 4.5), T.m_flat('#B0141E'))
    return im


def rune(name, color):
    im = ItemModel(name)
    im.cel(8, 0.5, 8, 8, 10, 3, T.m_stone('#4A4A55'))
    im.cel(8, 10.5, 8, 6, 1, 2.6, T.m_stone('#4A4A55'))
    for z in (6.4, 9.6):
        im.cel(8, 2.5, z, 4, 6, 0.2, T.m_glow(color, '#FFFFFF'), glow=True)
    return im


for nm, col in (('bloodlust', '#FF2A3A'), ('mutilation', '#FF8A2A'), ('stealth', '#8A8AFF'), ('dagger_storm', '#B04AFF')):
    MISC['rune_stone_' + nm] = ((lambda n=nm, c=col: rune('rune_stone_' + n, c)), OBJECT)


def fang(name, color, glow=None, curve=1.0):
    im = ItemModel(name)
    im.global_rot = ROT45
    for i in range(6):
        w = 2.4 * (1 - i / 7)
        im.cel(8 + curve * (i / 6) ** 2 * 2, 2 + i * 2, 8, w, 2, w, T.m_bone(color))
    if glow:
        im.cel(8, 2, 8, 1, 6, 2.6, T.m_glow(glow), glow=True)
    return im


def plate(name, color, gloss=None, n=3):
    im = ItemModel(name)
    for i in range(n):
        im.el((3 + i, 0.5 + i * 0.8, 3 + i * 1.5), (13 - i, 1.3 + i * 0.8, 9 + i * 1.5), T.m_chitin(color, gloss))
    return im


MATS = {
    'goblin_ear': lambda: plate('goblin_ear', '#6C9A3E', n=2),
    'lycan_fang': lambda: fang('lycan_fang', '#D8E0E8'),
    'kasaka_scale': lambda: plate('kasaka_scale', '#2C6AB8', '#7FC0FF'),
    'statue_fragment': lambda: crystal_cluster_stone('statue_fragment', '#C9B48A'),
    'ice_bear_pelt': lambda: pelt('ice_bear_pelt', '#E4EEF4'),
    'elf_frost_crystal': lambda: crystal_cluster('elf_frost_crystal', '#9FE6FF'),
    'orc_tusk': lambda: fang('orc_tusk', '#F0E8D0', curve=1.6),
    'demon_horn': lambda: fang('demon_horn', '#2A1818', curve=2.0),
    'cerberus_fang': lambda: fang('cerberus_fang', '#F0E6D0', glow='#FF5A1A'),
    'ant_carapace': lambda: plate('ant_carapace', '#2E2440', '#9A7ACF'),
    'red_knight_plume': lambda: pelt('red_knight_plume', '#B0101C'),
    'baran_heart': lambda: heart('baran_heart'),
}


def crystal_cluster_stone(name, color):
    im = ItemModel(name)
    m = T.m_stone(color)
    im.el((3, 0.5, 4), (10, 5, 11), m)
    im.el((8, 0.5, 6), (13, 7, 10), m)
    im.el((5, 5, 6), (9, 8, 9), m)
    return im


def pelt(name, color):
    im = ItemModel(name)
    m = T.m_fur(color)
    im.el((2.5, 0.5, 3), (13.5, 1.8, 13), m)
    im.el((4, 1.8, 4), (12, 2.8, 11), m)
    return im


def heart(name):
    im = ItemModel(name)
    for (w, y, h) in ((3, 1, 1), (6, 2, 2), (7, 4, 3), (6, 7, 1)):
        im.cel(8, y, 8, w, h, w * 0.8, T.m_skin('#5A0A1A', spots='#1A0008'))
    im.cel(8, 3, 8, 3, 4, 3.4, T.m_glow('#B04AFF', '#FFD0FF'), glow=True)
    for s in (-1, 1):
        im.cel(8 + s * 2, 8, 8, 1.2, 2.5, 1.2, T.m_skin('#3A0610'))
    return im
