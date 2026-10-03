"""Swords, tools and spell tomes: 32x32 weapon sprites drawn in a rotated local frame, 16x16 tomes."""
import math
import numpy as np
from sprites import Canvas, pal, hexc

OUT = (12, 8, 12)
S2 = math.sqrt(0.5)


class Frame:
    """local weapon space: ly runs from the pommel (0) towards the tip, lx is sideways; mapped diagonally onto the canvas"""
    def __init__(self, c, base=(3.5, 28.5)):
        self.c, self.b = c, base

    def p(self, lx, ly):
        return (self.b[0] + (ly + lx) * S2, self.b[1] - (ly - lx) * S2)

    def poly(self, pts):
        return self.c.mask_poly([self.p(x, y) for x, y in pts])

    def line(self, x0, y0, x1, y1, w):
        a, b = self.p(x0, y0), self.p(x1, y1)
        return self.c.mask_line(a[0], a[1], b[0], b[1], w)

    def circle(self, lx, ly, r):
        a = self.p(lx, ly)
        return self.c.mask_circle(a[0], a[1], r)


def _grip(f, length=7, col=('#1a0e0a', '#3a2418', '#6a4428', '#946038'), wrap=None):
    f.c.draw(f.line(0, 1.5, 0, length, 2.6), pal(*col), bevel=1.0, outline=OUT, noise=0.2)
    # wrapping lines
    for y in np.arange(2.5, length, 1.7):
        f.c.draw(f.line(-1.2, y, 1.2, y + 0.7, 0.8), pal(*(wrap or ('#0c0806', '#24160e'))), bevel=0.3, flat=0.3)


def _pommel(f, col, glow=None):
    f.c.draw(f.circle(0, 0.6, 1.9), pal(*col), bevel=1.0, outline=OUT, spec=0.5)
    if glow:
        f.c.draw(f.circle(0, 0.6, 0.8), pal(*glow), bevel=0.5, emissive=1.0)


def _guard(f, y, half, col, thick=1.8, curve=0.0, gem=None):
    pts = [(-half, y - curve), (-half * 0.6, y + thick * 0.6), (half * 0.6, y + thick * 0.6), (half, y - curve), (half * 0.55, y - thick * 0.6), (-half * 0.55, y - thick * 0.6)]
    f.c.draw(f.poly(pts), pal(*col), bevel=1.1, outline=OUT, spec=0.6)
    if gem:
        f.c.draw(f.circle(0, y, 1.1), pal(*gem), bevel=0.5, emissive=1.0)


def _blade(f, y0, y1, w, tip, pal_, fuller=None, edge=None, glow=None, serr=0, curve=0.0, w_top=None):
    wt = w if w_top is None else w_top
    L = y1 - y0
    pts_l, pts_r = [], []
    n = 8
    for i in range(n + 1):
        t = i / n
        y = y0 + L * t
        hw = (w + (wt - w) * t)
        off = curve * math.sin(t * math.pi * 0.9) if curve else 0
        pts_l.append((-hw + off, y)); pts_r.append((hw + off, y))
    top_off = curve * math.sin(math.pi * 0.9) if curve else 0
    poly = pts_l + [(top_off, y1 + tip)] + pts_r[::-1]
    if serr:
        # teeth on the right edge
        poly = pts_l + [(top_off, y1 + tip)]
        for i in range(n, -1, -1):
            x, y = pts_r[i]
            poly.append((x, y))
            if serr and i > 0:
                poly.append((x + serr, y - L / n * 0.35))
                poly.append((x, y - L / n * 0.55))
    m = f.poly(poly)
    f.c.draw(m, pal_, bevel=1.5, outline=OUT, spec=0.8)
    if fuller:
        f.c.draw(f.line(0, y0 + 1.5, curve * 0.5, y1 - 2, 1.0), pal(*fuller), bevel=0.4, flat=0.15 if fuller[0] < '#5' else 0.8)
    if edge:
        f.c.draw(f.line(w * 0.95, y0, wt * 0.95, y1 - 1, 0.7), pal(*edge), bevel=0.3, flat=0.95)
    if glow:
        for k in range(3):
            y = y0 + L * (0.25 + 0.25 * k)
            f.c.draw(f.circle(0, y, 0.7), pal(*glow), bevel=0.3, emissive=1.0)


# ---------------------------------------------------------------- swords
def ember_brand(c):
    f = Frame(c)
    _pommel(f, ('#2a0c08', '#6a2010', '#c04a18', '#ff8a30'), ('#ffd060', '#fff6c0'))
    _grip(f, 7)
    _guard(f, 8, 4.4, ('#2a1008', '#5a1c0c', '#b43c14', '#ff7a28'), curve=1.0, gem=('#ff6a20', '#ffd060'))
    _blade(f, 9, 29, 2.0, 4, pal('#4a0a04', '#b8300c', '#f06a1c', '#ffb048', '#fff2b8'), fuller=('#ffd060', '#fff6c0'), edge=('#fff0c0', '#ffffff'), glow=('#ffe080', '#ffffff'), w_top=1.6)
    return c


def bone_reaper(c):
    f = Frame(c)
    _pommel(f, ('#4a4230', '#9a8e70', '#d8cfae', '#f4ecd2'))
    _grip(f, 7, col=('#2a2418', '#5a4e38', '#8a7c5c', '#b8a880'), wrap=('#1a140c', '#3a2e1c'))
    _guard(f, 8, 4.8, ('#5a4e38', '#9a8e70', '#d8cfae', '#f4ecd2'), curve=-0.8)
    # curved bone scimitar with jagged back
    _blade(f, 9, 28, 2.6, 4, pal('#3a3220', '#7a6e54', '#b8aa88', '#e0d6b4', '#fffaec'), fuller=('#4a4230', '#6a6048'), edge=('#fffaec', '#ffffff'), serr=1.3, curve=2.6, w_top=1.6)
    return c


def blood_thirst(c):
    f = Frame(c)
    _pommel(f, ('#2a0408', '#6a0c18', '#b81c30', '#ff5a70'), ('#ff8a9a', '#ffe0e4'))
    _grip(f, 7, col=('#1a0408', '#400c14', '#701c28', '#a0303e'))
    _guard(f, 8, 4.0, ('#1a1014', '#3a2630', '#6a4856', '#a07088'), curve=1.6, gem=('#e01c3c', '#ff8a9a'))
    _blade(f, 9, 28, 1.9, 4.5, pal('#240408', '#6a0c1c', '#b8203c', '#e86a80', '#ffd0d8'), fuller=('#400410', '#a01028'), edge=('#ffd0d8', '#ffffff'), glow=('#ff5a70', '#ffd0d8'), w_top=1.3)
    # drip
    c.draw(c.mask_circle(26.5, 12.5, 0.9), pal('#a01028', '#ff5a70'), bevel=0.4, emissive=0.6)
    return c


def shadow_fang(c):
    f = Frame(c)
    _pommel(f, ('#0c0818', '#241848', '#4a3490', '#8a6ae0'), ('#b896ff', '#efe4ff'))
    _grip(f, 7, col=('#08060e', '#1c1430', '#2e2250', '#483a78'))
    _guard(f, 8, 3.4, ('#0c0818', '#241848', '#4a3490', '#8a6ae0'), curve=-1.4, gem=('#8a5cff', '#e4d6ff'))
    _blade(f, 9, 25, 1.8, 6, pal('#080414', '#1c1038', '#3a2680', '#7a56d8', '#c8b4ff'), fuller=('#08040e', '#2a1a58'), edge=('#e4d6ff', '#ffffff'), glow=('#b896ff', '#ffffff'), w_top=1.0)
    return c


def rot_cleaver(c):
    f = Frame(c)
    _pommel(f, ('#1a2208', '#3a4c14', '#6a8a22', '#a4cc40'))
    _grip(f, 7, col=('#10140a', '#2a3416', '#4a5c26', '#6a843a'))
    _guard(f, 8, 4.6, ('#1c2410', '#3e5018', '#6e8a28', '#a8cc48'), curve=0.3)
    # wide, pitted cleaver
    _blade(f, 9, 26, 3.4, 3, pal('#1e260c', '#46561a', '#7a9630', '#b0d050', '#e4f890'), fuller=('#2a360e', '#4e6420'), edge=('#e4f890', '#f8ffc0'), w_top=3.4)
    m = f.circle(1.4, 17, 1.0); c.draw(m, pal('#10160a', '#26300e'), flat=0.2)
    m = f.circle(-1.2, 22, 0.8); c.draw(m, pal('#10160a', '#26300e'), flat=0.2)
    for (lx, ly) in ((-0.8, 13), (0.9, 20), (-0.6, 24.5)):
        c.draw(f.circle(lx, ly, 0.55), pal('#d8c040', '#ffffe0'), bevel=0.3, emissive=1.0)
    return c


def sealbreaker(c):
    f = Frame(c)
    _pommel(f, ('#1c1c24', '#3c3c4c', '#6a6a82', '#a0a0b8'), ('#ff7a8a', '#fff0f0'))
    _grip(f, 8, col=('#18141c', '#34283a', '#54445c', '#74607c'))
    _guard(f, 9.4, 5.6, ('#24202c', '#4a4258', '#7a6e90', '#b0a4c8'), thick=2.4, gem=('#e0243c', '#ff8a96'))
    _blade(f, 10.5, 29, 2.6, 3, pal('#101018', '#2c2c3c', '#56566e', '#8a8aa8', '#d0d0e8'), fuller=('#e0243c', '#ff8a96'), edge=('#f0f0ff', '#ffffff'), w_top=2.2)
    return c


def chain_whip(c):
    pal_l = pal('#14101a', '#34284a', '#5e4c80', '#9a86c4')
    f = Frame(c)
    _pommel(f, ('#1c1020', '#3a2040', '#6a3a78', '#a468b8'))
    _grip(f, 8, col=('#1a0e0a', '#3a2418', '#6a4428', '#946038'))
    _guard(f, 9, 2.6, ('#24202c', '#4a4258', '#7a6e90', '#b0a4c8'), thick=1.4)
    # chain of links curving outward, ending in a barbed hook
    for i in range(11):
        t = i / 10
        ly = 11 + t * 17
        lx = 4.5 * math.sin(t * math.pi * 1.3)
        c.draw(f.circle(lx, ly, 1.35 - 0.0 * t), pal_l if i % 2 == 0 else pal('#1c1c24', '#4a4a5c', '#8a8aa0', '#c8c8dc'), bevel=0.8, outline=OUT, spec=0.5)
    ex, ey = 4.5 * math.sin(1.3 * math.pi), 28.5
    c.draw(f.poly([(ex - 1.8, ey - 1), (ex + 1.8, ey - 1), (ex + 0.3, ey + 4.5)]), pal('#2a1030', '#6a2a88', '#c060e8', '#f0b8ff'), bevel=0.8, outline=OUT, emissive=0.5, spec=0.8)
    return c


def obsidian_greataxe(c):
    f = Frame(c, base=(3, 29))
    _pommel(f, ('#10101a', '#26263a', '#46466a', '#7a7aa8'))
    c.draw(f.line(0, 0.5, 0, 25, 2.4), pal('#1a0e0a', '#3a2418', '#6a4428', '#946038'), bevel=1.0, outline=OUT, noise=0.2)
    for y in (3, 6, 9):
        c.draw(f.line(-1.2, y, 1.2, y + 0.6, 0.9), pal('#0c0806', '#24160e'), bevel=0.3, flat=0.3)
    # double bit head
    P = pal('#08080e', '#1a1a2a', '#32324e', '#5a5a86', '#9696c8')
    c.draw(f.poly([(0, 21), (6.6, 19.5), (8.8, 24.5), (6.4, 30), (0, 28.5)]), P, bevel=1.6, outline=OUT, spec=0.8)
    c.draw(f.poly([(0, 21), (-5, 19.8), (-6.4, 24), (-4.6, 28), (0, 27.5)]), P, bevel=1.6, outline=OUT, spec=0.8)
    c.draw(f.line(1.5, 22, 6.4, 25, 0.8), pal('#e0243c', '#ff8a96'), bevel=0.3, emissive=0.9)
    c.draw(f.circle(0, 24.5, 1.5), pal('#e0243c', '#ff9aa6'), bevel=0.5, emissive=1.0, outline=OUT)
    return c


def hellfire_scythe(c):
    f = Frame(c, base=(3, 29))
    _pommel(f, ('#2a0c08', '#6a2010', '#c04a18', '#ff8a30'))
    c.draw(f.line(0, 0.5, 0, 26, 2.2), pal('#1a0e0a', '#3a2418', '#5a3a24', '#7a5030'), bevel=1.0, outline=OUT, noise=0.25)
    # crescent blade sweeping to the left of the haft top
    pts = []
    for i in range(0, 13):
        t = i / 12
        a = math.pi * (0.1 + 0.8 * t)
        pts.append((-math.cos(a) * 11 + 2, 26 + math.sin(a) * 4.8 + 0.0))
    inner = []
    for i in range(12, -1, -1):
        t = i / 12
        a = math.pi * (0.1 + 0.8 * t)
        inner.append((-math.cos(a) * 10.4 + 2.2, 26 + math.sin(a) * 2.6 - 0.8))
    c.draw(f.poly(pts + inner), pal('#4a0a04', '#b8300c', '#f06a1c', '#ffb048', '#fff2b8'), bevel=1.2, outline=OUT, spec=0.8, emissive=0.3)
    c.draw(f.circle(0, 26, 1.5), pal('#ff6a20', '#ffd060'), bevel=0.5, emissive=1.0, outline=OUT)
    return c


def sovereign_blade(c):
    f = Frame(c)
    _pommel(f, ('#4a3408', '#a87c18', '#e8b830', '#fff0a0'), ('#ff5a40', '#ffe0c0'))
    _grip(f, 7, col=('#10080a', '#2a1418', '#4a2830', '#6e3c48'))
    _guard(f, 8.2, 5.8, ('#4a3408', '#a87c18', '#e8b830', '#fff0a0'), thick=2.2, curve=1.8, gem=('#ff3a30', '#ffd0c0'))
    _blade(f, 9.5, 29, 2.5, 4, pal('#0c0c12', '#26263a', '#4c4c6c', '#8686ac', '#d4d4f0'), fuller=('#a87c18', '#fff0a0'), edge=('#ffffff', '#ffffff'), glow=('#ffd060', '#ffffff'), w_top=1.8)
    c.draw(f.line(2.2, 11, 2.2, 27, 0.5), pal('#e8b830', '#fff0a0'), bevel=0.2, emissive=0.8)
    c.draw(f.line(-2.2, 11, -2.2, 27, 0.5), pal('#e8b830', '#fff0a0'), bevel=0.2, emissive=0.8)
    return c


# ---------------------------------------------------------------- tomes (16x16)
def _tome(c, cover, trim, emblem_fn):
    c.draw(c.mask_poly([(3, 2), (13, 2.8), (13.4, 14), (3.4, 14.4)]), pal(*cover), bevel=1.4, outline=OUT, spec=0.4)
    c.draw(c.mask_rect(3.0, 2.2, 4.6, 14.2), pal(*trim), bevel=0.8, flat=0.5)
    c.draw(c.mask_rect(4.8, 13.4, 13.2, 14.2), pal('#d8cfae', '#f4ecd2'), flat=0.8)
    emblem_fn(c)
    for y in (4, 7.5, 11):
        pass
    return c


def _e_flame(c):
    c.draw(c.mask_poly([(8.5, 11.5), (6.8, 9), (7.6, 6), (8.6, 4.4), (9.6, 6.4), (10.6, 8.4), (9.6, 11.5)]), pal('#ff6a14', '#ffb040', '#fff0a0'), bevel=0.7, emissive=0.9)


def _e_rain(c):
    for (x, y) in ((7, 5), (10, 7), (8, 9), (11, 11), (6, 11)):
        c.draw(c.mask_line(x, y, x - 1, y + 2, 0.9), pal('#ff6a14', '#ffd060'), bevel=0.3, emissive=1.0)


def _e_chain(c):
    for i, (x, y) in enumerate(((7, 5), (9, 6.6), (7.6, 8.4), (9.6, 10), (8, 11.6))):
        c.draw(c.mask_circle(x, y, 1.1), pal('#30b8e8', '#e8fcff'), bevel=0.5, emissive=0.8)


def _e_blood(c):
    c.draw(c.mask_poly([(8.5, 4), (11, 8), (10.6, 10.6), (8.5, 12), (6.4, 10.6), (6, 8)]), pal('#6a0c18', '#e01c3c', '#ff8a9a'), bevel=0.8, emissive=0.5)


def _e_void(c):
    c.draw(c.mask_circle(8.5, 8, 3.4), pal('#08040e', '#2a1a58', '#8a5cff'), bevel=0.8, emissive=0.5)
    c.draw(c.mask_circle(8.5, 8, 1.3), pal('#000000', '#08040e'), flat=0.0)


def _e_thorn(c):
    for a in range(0, 360, 60):
        r = math.radians(a)
        c.draw(c.mask_line(8.5, 8, 8.5 + math.cos(r) * 3.6, 8 + math.sin(r) * 3.6, 0.9), pal('#4a6a14', '#b4d84a'), bevel=0.3, emissive=0.4)
    c.draw(c.mask_circle(8.5, 8, 1.2), pal('#7aa022', '#e4f890'), bevel=0.4)


def _e_skull(c):
    c.draw(c.mask_circle(8.5, 7.4, 2.9), pal('#8a7e62', '#d8cfae', '#fffaec'), bevel=0.8)
    c.draw(c.mask_rect(7.2, 9, 9.8, 11.4), pal('#8a7e62', '#d8cfae'), flat=0.6)
    c.draw(c.mask_circle(7.4, 7.4, 0.8), pal('#08040e', '#1a1010'), flat=0.0)
    c.draw(c.mask_circle(9.6, 7.4, 0.8), pal('#08040e', '#1a1010'), flat=0.0)


def _e_imp(c):
    c.draw(c.mask_circle(8.5, 8.4, 2.8), pal('#6a1004', '#d8401c', '#ff8a40'), bevel=0.8, emissive=0.3)
    c.draw(c.mask_poly([(6.2, 6.4), (5.6, 3.6), (7.8, 5.6)]), pal('#6a1004', '#ff8a40'), flat=0.6)
    c.draw(c.mask_poly([(10.8, 6.4), (11.4, 3.6), (9.2, 5.6)]), pal('#6a1004', '#ff8a40'), flat=0.6)
    c.draw(c.mask_circle(7.6, 8, 0.6), pal('#ffe060', '#ffffff'), bevel=0.2, emissive=1.0)
    c.draw(c.mask_circle(9.4, 8, 0.6), pal('#ffe060', '#ffffff'), bevel=0.2, emissive=1.0)


def _e_crown(c):
    c.draw(c.mask_poly([(5.4, 11), (5.6, 5.6), (7.2, 8), (8.5, 4.6), (9.8, 8), (11.4, 5.6), (11.6, 11)]), pal('#8a6a14', '#e8b830', '#fff0a0'), bevel=0.8, emissive=0.5)


TOMES = [
    # spell, tr, en, cover, trim, emblem, mana, cooldown, rarity
    ('fireball', 'Ateş Topu Kitabı', 'Tome of Fireballs', ('#3a0c04', '#8a2208', '#d8481c', '#ff8a40'), ('#1a0804', '#4a1408'), _e_flame, 15, 20, 'common'),
    ('hellrain', 'Cehennem Yağmuru Kitabı', 'Tome of Hellrain', ('#2a0804', '#6a1608', '#b83810', '#f07a28'), ('#12060a', '#3a1010'), _e_rain, 45, 120, 'rare'),
    ('soul_chain', 'Ruh Zinciri Kitabı', 'Tome of Soul Chains', ('#04182a', '#0c4a6a', '#2890b8', '#70d0f0'), ('#020c18', '#0a2a40'), _e_chain, 25, 60, 'uncommon'),
    ('blood_pact', 'Kan Anlaşması Kitabı', 'Tome of the Blood Pact', ('#2a040a', '#6a0c1a', '#b81c34', '#f05a74'), ('#14040a', '#400812'), _e_blood, 10, 80, 'uncommon'),
    ('void_step', 'Boşluk Adımı Kitabı', 'Tome of the Void Step', ('#0a0618', '#241848', '#4a3490', '#8a6ae0'), ('#04020c', '#140c30'), _e_void, 20, 40, 'rare'),
    ('thorn_burst', 'Diken Patlaması Kitabı', 'Tome of Thorn Burst', ('#0e1604', '#2a3c0c', '#587c1c', '#98c440'), ('#080e02', '#1c2a08'), _e_thorn, 20, 50, 'uncommon'),
    ('bone_prison', 'Kemik Zindanı Kitabı', 'Tome of the Bone Prison', ('#2a2418', '#5a4e38', '#9a8c6c', '#d8cfae'), ('#14100a', '#3a3222'), _e_skull, 30, 100, 'rare'),
    ('summon_imp', 'Ufaklık Çağırma Kitabı', 'Tome of Imp Summoning', ('#2a0a04', '#6a1c08', '#b44018', '#f08a4a'), ('#140604', '#3a1208'), _e_imp, 35, 200, 'uncommon'),
    ('decree', 'Egemen Fermanı', "Sovereign's Decree", ('#12101a', '#2c2640', '#54486e', '#8a7cb0'), ('#a87c18', '#fff0a0'), _e_crown, 80, 400, 'epic'),
]

# id, tr, en, drawer, damage, speed, rarity, mana, cooldown
SWORDS = [
    ('ember_brand', 'Kor Pençesi', 'Ember Brand', ember_brand, 5, -2.4, 'rare', 25, 140),
    ('bone_reaper', 'Kemik Biçer', 'Bone Reaper', bone_reaper, 5, -2.2, 'rare', 25, 120),
    ('blood_thirst', 'Kan Susuzluğu', 'Bloodthirst', blood_thirst, 4, -2.0, 'rare', 20, 160),
    ('shadow_fang', 'Gölge Dişi', 'Shadow Fang', shadow_fang, 3, -1.6, 'rare', 25, 100),
    ('rot_cleaver', 'Çürük Satır', 'Rot Cleaver', rot_cleaver, 6, -2.8, 'rare', 25, 140),
    ('sealbreaker', 'Mühür Kıran', 'Sealbreaker', sealbreaker, 6, -2.6, 'epic', 40, 200),
    ('chain_whip', 'Zincir Kırbaç', 'Chain Whip', chain_whip, 3, -2.0, 'rare', 20, 80),
    ('obsidian_greataxe', 'Obsidyen Balta', 'Obsidian Greataxe', obsidian_greataxe, 11, -3.3, 'epic', 35, 160),
    ('hellfire_scythe', 'Cehennem Ateşi Tırpanı', 'Hellfire Scythe', hellfire_scythe, 6, -2.5, 'epic', 40, 180),
    ('sovereign_blade', 'Egemenin Kılıcı', "Sovereign's Blade", sovereign_blade, 9, -2.2, 'epic', 60, 240),
]


def chronicle(c):
    c.draw(c.mask_poly([(2.5, 2), (13.5, 2.6), (13.8, 14.2), (2.8, 14.6)]), pal('#1a1410', '#3c2c20', '#6a4c34', '#946a48'), bevel=1.4, outline=OUT, spec=0.4)
    c.draw(c.mask_rect(2.6, 2.2, 4.2, 14.4), pal('#8a6a14', '#e8b830'), bevel=0.6, flat=0.6)
    c.draw(c.mask_circle(9, 8.3, 2.6), pal('#4a0a04', '#e04010', '#ffd060'), bevel=0.8, emissive=0.8, outline=(30, 8, 4))
    c.draw(c.mask_circle(9, 8.3, 0.9), pal('#fff0a0', '#ffffff'), bevel=0.3, emissive=1.0)
    return c
