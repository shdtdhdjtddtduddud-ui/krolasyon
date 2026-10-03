"""Sprites for materials, relics and sigils (16x16)."""
import math
import numpy as np
from sprites import Canvas, pal, hexc

OUT = (14, 10, 12)


def ember_shard(c):
    P = pal('#5a1404', '#a8340a', '#ee6a14', '#ffb040', '#fff0a0')
    c.draw(c.mask_poly([(8, 1), (11.5, 6), (10, 14.5), (8, 15), (5.5, 14), (4.5, 6)]), P, bevel=1.4, outline=OUT, spec=0.5, emissive=0.3)
    c.draw(c.mask_poly([(8, 4), (9.5, 7), (8.5, 12), (7.2, 7)]), pal('#ff9a30', '#ffd060', '#fff6c0'), bevel=1, emissive=0.8)
    return c


def bone_dust(c):
    P = pal('#6a604a', '#a89c80', '#d8cfae', '#f4ecd2')
    c.draw(c.mask_poly([(2, 14), (4, 9), (7, 6.5), (9.5, 7), (12, 9.5), (14, 14)]), P, bevel=1.8, outline=OUT, noise=0.25, seed=3)
    c.speckle(c.mask_poly([(2, 14), (4, 9), (7, 6.5), (9.5, 7), (12, 9.5), (14, 14)]), [hexc('#fff8e0'), hexc('#8a7e62')], 0.25, 4)
    c.draw(c.mask_line(9, 3, 12, 1.5, 1.6), pal('#a89c80', '#e8dfc4', '#fffbe8'), bevel=1, outline=OUT)
    c.draw(c.mask_circle(12.8, 1.4, 1.1), pal('#a89c80', '#e8dfc4'), bevel=1, outline=OUT)
    return c


def blood_vial(c):
    glass = pal('#4a5a6a', '#8aa4b8', '#c8e0f0', '#f4fbff')
    c.draw(c.mask_poly([(6, 5), (10, 5), (10, 7), (12.5, 10), (12.5, 14), (11, 15), (5, 15), (3.5, 14), (3.5, 10), (6, 7)]), glass, bevel=1.3, outline=OUT, spec=0.6)
    c.draw(c.mask_poly([(4.4, 10.5), (11.6, 10.5), (11.6, 14), (10.6, 14.4), (5.4, 14.4), (4.4, 14)]), pal('#4a0410', '#9c0c24', '#e01c3c', '#ff6078'), bevel=1, emissive=0.2)
    c.draw(c.mask_rect(6, 1.8, 10, 5), pal('#3a2410', '#7a5028', '#b88040'), bevel=1, outline=OUT)
    return c


def shadow_essence(c):
    P = pal('#1a0a38', '#3c1c88', '#7a4ae0', '#b896ff', '#efe4ff')
    m = c.mask_circle(8, 9, 4.6) | c.mask_poly([(8, 1), (11, 6), (5, 6)]) | c.mask_ellipse(8, 13.5, 3, 1.8)
    c.draw(m, P, bevel=1.6, outline=(10, 4, 20), emissive=0.4)
    c.draw(c.mask_circle(8, 9, 2.0), pal('#b896ff', '#efe4ff', '#ffffff'), bevel=1, emissive=1.0)
    return c


def rot_spore(c):
    P = pal('#26340a', '#4a6a14', '#7aa022', '#b4d84a', '#e4f890')
    c.draw(c.mask_circle(8, 9, 5.4), P, bevel=1.8, outline=(10, 18, 4), noise=0.2, seed=2)
    c.draw(c.mask_rect(7, 14, 9, 15.5), pal('#4a3a20', '#8a7448'), outline=OUT)
    for (x, y, r) in ((6, 7, 1.0), (10, 8, 1.2), (8, 11, 0.9), (5.5, 10.5, 0.7)):
        c.draw(c.mask_circle(x, y, r), pal('#d8c040', '#f8f090', '#ffffe0'), bevel=1, emissive=0.8)
    return c


def soul_ember(c):
    P = pal('#08304a', '#0f6a9a', '#30b8e8', '#8ae8ff', '#e8fcff')
    c.draw(c.mask_poly([(8, 1.5), (11.5, 6), (12.5, 10), (10.5, 14), (8, 15), (5.5, 14), (3.5, 10), (4.5, 6)]), P, bevel=1.5, outline=(4, 14, 24), emissive=0.5)
    c.draw(c.mask_ellipse(8, 10, 2.4, 3.4), pal('#8ae8ff', '#e8fcff', '#ffffff'), bevel=1, emissive=1.0)
    return c


def hellsteel_ingot(c):
    P = pal('#0e0c14', '#241c2e', '#463a56', '#74688a', '#a89cc0')
    c.draw(c.mask_poly([(2, 10), (4, 5), (13, 5), (14.5, 10), (12.5, 13), (3, 13)]), P, bevel=1.4, outline=OUT, spec=0.4)
    c.draw(c.mask_poly([(4.5, 6), (12.5, 6), (13.4, 9.6), (3.6, 9.6)]), pal('#463a56', '#74688a', '#a89cc0', '#d4cae8'), bevel=1, flat=0.6)
    c.draw(c.mask_line(3.5, 12, 13, 12, 0.9), pal('#8a1020', '#e0243c', '#ff6a7a'), bevel=0.5, emissive=0.8)
    return c


def hellsteel_nugget(c):
    P = pal('#0e0c14', '#241c2e', '#463a56', '#74688a', '#a89cc0')
    c.draw(c.mask_poly([(4, 10), (6, 6.5), (10, 6), (12, 9), (11, 12.5), (6, 13)]), P, bevel=1.4, outline=OUT, spec=0.4)
    c.draw(c.mask_circle(9.5, 8.5, 0.9), pal('#e0243c', '#ff8a96'), bevel=0.5, emissive=0.8)
    return c


def raw_hellsteel(c):
    P = pal('#16121c', '#2e2638', '#4c4060', '#6e6088')
    m = c.mask_poly([(2.5, 11), (3, 6.5), (6, 3.5), (10, 3), (13, 6), (13.5, 10.5), (10, 13.5), (5, 13.5)])
    c.draw(m, P, bevel=1.6, outline=OUT, noise=0.3, seed=5)
    c.speckle(m, [hexc('#e0243c'), hexc('#ff6a7a'), hexc('#a89cc0')], 0.18, 6)
    return c


def _medallion(c, rim, face, emblem):
    c.draw(c.mask_circle(8, 8, 7.2), rim, bevel=1.6, outline=OUT, spec=0.5)
    c.draw(c.mask_circle(8, 8, 5.4), face, bevel=1.2)
    emblem(c)
    return c


def ember_sigil(c):
    def em(c):
        c.draw(c.mask_poly([(8, 3), (10.8, 7.5), (10.4, 11.2), (8, 13), (5.6, 11.2), (5.2, 7.5)]), pal('#a8340a', '#ee6a14', '#ffb040', '#fff0a0'), bevel=1, emissive=0.6)
        c.draw(c.mask_ellipse(8, 10.4, 1.5, 2.2), pal('#ffd060', '#fffbe0'), bevel=0.8, emissive=1.0)
    return _medallion(c, pal('#3a2410', '#8f5a1c', '#d8a030', '#ffe070'), pal('#2a0c06', '#4a1608', '#6c200c'), em)


def bone_sigil(c):
    def em(c):
        c.draw(c.mask_ellipse(8, 7.4, 3.2, 3.0), pal('#a89c80', '#e0d6b8', '#fff8e0'), bevel=1, outline=(30, 24, 18))
        c.draw(c.mask_rect(6.4, 9.6, 9.6, 12), pal('#a89c80', '#e0d6b8'), bevel=0.8, outline=(30, 24, 18))
        c.pixel(6, 7, (110, 230, 255)); c.pixel(10, 7, (110, 230, 255)); c.pixel(7, 6, (110, 230, 255)); c.pixel(9, 6, (110, 230, 255))
    return _medallion(c, pal('#3a2410', '#8f5a1c', '#d8a030', '#ffe070'), pal('#0e1014', '#1e2128', '#363b46'), em)


def blood_sigil(c):
    def em(c):
        c.draw(c.mask_poly([(8, 2.6), (11.4, 8), (11, 11), (8, 13.4), (5, 11), (4.6, 8)]), pal('#4a0410', '#9c0c24', '#e01c3c', '#ff6078'), bevel=1.2, emissive=0.4, spec=0.8)
    return _medallion(c, pal('#3a2410', '#8f5a1c', '#d8a030', '#ffe070'), pal('#14040a', '#2c0a14', '#4c1022'), em)


def shadow_sigil(c):
    def em(c):
        c.draw(c.mask_ellipse(8, 8, 4.4, 2.6), pal('#c8c0dc', '#e4dff0', '#ffffff'), bevel=0.8, outline=(20, 10, 40))
        c.draw(c.mask_circle(8, 8, 1.9), pal('#5a2ac0', '#8a5cff', '#c8a0ff'), bevel=0.8, emissive=0.8)
        c.pixel(8, 8, (6, 2, 14))
    return _medallion(c, pal('#3a2410', '#8f5a1c', '#d8a030', '#ffe070'), pal('#07040f', '#140c26', '#251748'), em)


def rot_sigil(c):
    def em(c):
        c.draw(c.mask_ellipse(8, 6.4, 4.2, 2.6), pal('#8c3220', '#c05a30', '#e8904a'), bevel=1, outline=(30, 10, 6))
        c.draw(c.mask_rect(7, 8, 9, 12), pal('#a89c80', '#e0d6b8'), bevel=0.8, outline=(30, 24, 18))
        for (x, y) in ((6, 5.6), (9.5, 6.4), (8, 4.8)):
            c.pixel(int(x), int(y), (250, 240, 210))
    return _medallion(c, pal('#3a2410', '#8f5a1c', '#d8a030', '#ffe070'), pal('#10200a', '#223a12', '#3c5e1e'), em)


def sovereign_seal(c):
    c.draw(c.mask_circle(8, 8, 7.4), pal('#3a2a08', '#8a6a14', '#d8a828', '#ffe070', '#fffbd0'), bevel=1.8, outline=OUT, spec=0.8)
    c.draw(c.mask_circle(8, 8, 5.2), pal('#14081c', '#2a1240', '#4a2078'), bevel=1.2)
    # crown
    c.draw(c.mask_poly([(4, 10.5), (4.6, 5.6), (6.4, 8), (8, 4.6), (9.6, 8), (11.4, 5.6), (12, 10.5)]), pal('#8a6a14', '#d8a828', '#ffe070', '#fffbd0'), bevel=1, outline=(30, 20, 4), spec=0.6)
    c.draw(c.mask_circle(8, 8.6, 1.2), pal('#ff4060', '#ff90a0'), bevel=0.6, emissive=0.8)
    return c


def queen_heart(c):
    c.draw(c.mask_heart(8, 8.6, 5.2), pal('#6a0c04', '#c8300a', '#ff7a14', '#ffc040', '#fff0a0'), bevel=1.6, outline=OUT, emissive=0.5, spec=0.6)
    c.draw(c.mask_heart(8, 8.8, 2.6), pal('#ffb040', '#fff0a0', '#ffffff'), bevel=0.8, emissive=1.0)
    return c


def bone_crown(c):
    c.draw(c.mask_poly([(2.5, 12), (3, 6), (5.5, 9), (8, 3.5), (10.5, 9), (13, 6), (13.5, 12)]), pal('#6a604a', '#a89c80', '#d8cfae', '#f4ecd2'), bevel=1.2, outline=OUT, spec=0.5, noise=0.2)
    c.draw(c.mask_rect(2.5, 11.4, 13.5, 13.4), pal('#8a6a14', '#d8a828', '#ffe070'), bevel=0.8, outline=OUT)
    for x in (4, 8, 12):
        c.pixel(x, 12, (110, 230, 255))
    c.pixel(8, 7, (110, 230, 255))
    return c


def nyx_eye(c):
    c.draw(c.mask_ellipse(8, 8, 6.4, 4.4), pal('#9c90b8', '#cfc4e4', '#f4eeff'), bevel=1.4, outline=(20, 10, 40), spec=0.6)
    c.draw(c.mask_circle(8, 8, 3.4), pal('#2a1060', '#5a2ac0', '#8a5cff', '#c8a0ff'), bevel=1.2, emissive=0.7)
    c.draw(c.mask_rect(7.4, 5.4, 8.6, 10.6), pal('#04020a', '#0a0414'), bevel=0.4)
    return c


def rot_heart(c):
    c.draw(c.mask_circle(8, 9, 5.6), pal('#26340a', '#4a6a14', '#7aa022', '#b4d84a', '#e4f890'), bevel=1.8, outline=(10, 18, 4), noise=0.2, seed=9)
    c.draw(c.mask_heart(8, 9.4, 2.8), pal('#6a0c28', '#c8204c', '#ff6a8a'), bevel=0.8, emissive=0.7)
    for (x, y) in ((5, 6), (11, 7), (6, 12.5), (11.5, 12)):
        c.draw(c.mask_circle(x, y, 0.9), pal('#d8c040', '#fff8a0'), bevel=0.6, emissive=0.6)
    return c


def obsidian_core(c):
    c.draw(c.mask_poly([(8, 1.5), (13, 6), (12, 12), (8, 14.5), (4, 12), (3, 6)]), pal('#040308', '#0c0a14', '#18142a', '#2c2450', '#4a3c80'), bevel=1.4, outline=OUT, spec=0.7)
    c.draw(c.mask_poly([(8, 5), (10.4, 8), (8, 11.5), (5.6, 8)]), pal('#ff5a10', '#ffb040', '#fff0a0'), bevel=0.8, emissive=1.0)
    return c


def ash_crown(c):
    c.draw(c.mask_poly([(2, 12.5), (2.4, 5), (5.2, 8.6), (8, 2.4), (10.8, 8.6), (13.6, 5), (14, 12.5)]), pal('#18181e', '#34343e', '#5a5a68', '#8c8c9c'), bevel=1.3, outline=OUT, spec=0.6, noise=0.2)
    c.draw(c.mask_rect(2, 11.6, 14, 13.8), pal('#8a6a14', '#d8a828', '#ffe070'), bevel=0.8, outline=OUT)
    for (x, y, col) in ((8, 8, (255, 90, 40)), (4.5, 10, (255, 200, 80)), (11.5, 10, (255, 200, 80))):
        c.draw(c.mask_circle(x, y, 1.1), pal(col, tuple(min(255, v + 60) for v in col)), bevel=0.6, emissive=0.9)
    return c


def hell_spark(c):
    c.draw(c.mask_circle(8, 8, 5.4), pal('#3a0808', '#8a1c08', '#e04010', '#ff9a30', '#fff0a0'), bevel=1.6, outline=OUT, emissive=0.6)
    for ang in range(0, 360, 45):
        a = math.radians(ang)
        c.draw(c.mask_line(8 + math.cos(a) * 5.4, 8 + math.sin(a) * 5.4, 8 + math.cos(a) * 7.4, 8 + math.sin(a) * 7.4, 1.0), pal('#e04010', '#ff9a30'), bevel=0.4, emissive=0.8)
    c.draw(c.mask_circle(8, 8, 2.2), pal('#ffd060', '#fffbe0'), bevel=0.6, emissive=1.0)
    return c


MATERIALS = [
    # id, tr, en, drawer, rarity
    ('ember_shard', 'Kor Parçası', 'Ember Shard', ember_shard, 'common'),
    ('bone_dust', 'Kemik Tozu', 'Bone Dust', bone_dust, 'common'),
    ('blood_vial', 'Kan Şişesi', 'Blood Vial', blood_vial, 'uncommon'),
    ('shadow_essence', 'Gölge Özü', 'Shadow Essence', shadow_essence, 'uncommon'),
    ('rot_spore', 'Çürük Spor', 'Rot Spore', rot_spore, 'common'),
    ('soul_ember', 'Ruh Közü', 'Soul Ember', soul_ember, 'uncommon'),
    ('raw_hellsteel', 'Ham Cehennem Çeliği', 'Raw Hellsteel', raw_hellsteel, 'common'),
    ('hellsteel_ingot', 'Cehennem Çeliği Külçesi', 'Hellsteel Ingot', hellsteel_ingot, 'uncommon'),
    ('hellsteel_nugget', 'Cehennem Çeliği Parçası', 'Hellsteel Nugget', hellsteel_nugget, 'common'),
    ('ember_sigil', 'Kor Mührü', 'Ember Sigil', ember_sigil, 'epic'),
    ('bone_sigil', 'Kemik Mührü', 'Bone Sigil', bone_sigil, 'epic'),
    ('blood_sigil', 'Kan Mührü', 'Blood Sigil', blood_sigil, 'epic'),
    ('shadow_sigil', 'Gölge Mührü', 'Shadow Sigil', shadow_sigil, 'epic'),
    ('rot_sigil', 'Çürük Mührü', 'Rot Sigil', rot_sigil, 'epic'),
    ('sovereign_seal', 'Hükümdar Mührü', 'Sovereign Seal', sovereign_seal, 'epic'),
    ('queen_heart', 'Kraliçenin Kalbi', "Queen's Heart", queen_heart, 'rare'),
    ('bone_crown', 'Kemik Taç', 'Bone Crown', bone_crown, 'rare'),
    ('nyx_eye', 'Nyx\'in Gözü', "Nyx's Eye", nyx_eye, 'rare'),
    ('rot_heart', 'Çürük Kalp', 'Rot Heart', rot_heart, 'rare'),
    ('obsidian_core', 'Obsidyen Çekirdek', 'Obsidian Core', obsidian_core, 'rare'),
    ('ash_crown', 'Kül Tacı', 'Ash Crown', ash_crown, 'epic'),
    ('hell_spark', 'Cehennem Kıvılcımı', 'Hell Spark', hell_spark, 'uncommon'),
]
