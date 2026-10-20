#!/usr/bin/env python3
"""Maceracı Kuşanımı dokuları.

- Eşya simgeleri: 512px tuvalde kalın konturlu, gölgeli resim olarak çizilir, 32x32'ye küçültülür (referans çizimdeki stil).
- 3D model dokuları (gözlük, çanta, kemer, kitap): kutu UV açılımına göre 2x çözünürlükte boyanır.
- Kilitli sandık blok dokuları (16x16).
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'advgear', 'textures')
W = 512
OUT = (34, 24, 18, 255)


def hx(c, a=255):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def S(v):
    return v * 2


def P(pts):
    return [(S(x), S(y)) for x, y in pts]


def B(b):
    return [S(b[0]), S(b[1]), S(b[2]), S(b[3])]


class Icon:
    def __init__(self):
        self.img = Image.new('RGBA', (W, W), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    # --- temel şekiller (koordinatlar 0..256 uzayında)
    def poly(self, pts, fill, ow=5, outline=OUT):
        self.d.polygon(P(pts), fill=fill, outline=outline if ow else None, width=S(ow) if ow else 0)

    def ell(self, b, fill, ow=5, outline=OUT):
        self.d.ellipse(B(b), fill=fill, outline=outline if ow else None, width=S(ow) if ow else 0)

    def rect(self, b, fill, ow=5, r=6, outline=OUT):
        self.d.rounded_rectangle(B(b), radius=S(r), fill=fill, outline=outline if ow else None, width=S(ow) if ow else 0)

    def line(self, pts, fill, w=5):
        self.d.line(P(pts), fill=fill, width=S(w), joint='curve')

    def clear_ell(self, b):
        m = Image.new('L', (W, W), 0)
        ImageDraw.Draw(m).ellipse(B(b), fill=255)
        self.img.paste((0, 0, 0, 0), (0, 0), m)
        self.d = ImageDraw.Draw(self.img)

    def grad(self, shape_fn, c1, c2, vertical=True, ow=5):
        """Şekli c1→c2 degrade ile doldurur, sonra kontur çizer. shape_fn(draw, fill) maskeye çizer."""
        m = Image.new('L', (W, W), 0)
        shape_fn(ImageDraw.Draw(m), 255)
        g = Image.new('RGBA', (W, W))
        gp = g.load()
        bb = m.getbbox()
        if not bb:
            return
        for y in range(bb[1], bb[3]):
            for x in range(bb[0], bb[2]):
                t = (y - bb[1]) / max(1, bb[3] - bb[1]) if vertical else (x - bb[0]) / max(1, bb[2] - bb[0])
                gp[x, y] = mix(c1, c2, t)
        self.img.paste(g, (0, 0), m)
        self.d = ImageDraw.Draw(self.img)

    def gpoly(self, pts, c1, c2, vertical=True, ow=5):
        self.grad(lambda d, f: d.polygon(P(pts), fill=f), c1, c2, vertical)
        if ow:
            self.d.polygon(P(pts), outline=OUT, width=S(ow))

    def gell(self, b, c1, c2, vertical=True, ow=5):
        self.grad(lambda d, f: d.ellipse(B(b), fill=f), c1, c2, vertical)
        if ow:
            self.d.ellipse(B(b), outline=OUT, width=S(ow))

    def save(self, name, size=32):
        im = self.img.resize((size, size), Image.LANCZOS)
        px = im.load()
        for y in range(size):
            for x in range(size):
                r, g, b, a = px[x, y]
                if a < 90:
                    px[x, y] = (0, 0, 0, 0)
                else:
                    f = 255 / a
                    px[x, y] = (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), 255)
        os.makedirs(os.path.join(ROOT, 'item'), exist_ok=True)
        im.save(os.path.join(ROOT, 'item', name + '.png'))


GOLD, GOLD_L, GOLD_D = hx('#e2b23c'), hx('#fbe08a'), hx('#9a6a1c')
SILV, SILV_L, SILV_D = hx('#aeb6bd'), hx('#eef2f4'), hx('#626c74')
BRASS, BRASS_L, BRASS_D = hx('#c58f3c'), hx('#f0c878'), hx('#7d5520')
LEATH, LEATH_L, LEATH_D = hx('#7a5034'), hx('#a8774f'), hx('#4a2e1c')
WHITE = (255, 255, 255, 255)


# ------------------------------------------------------------------ yüzükler

def ring_band(ic, c, cl, cd, box=(22, 78, 234, 206), inner=(72, 110, 184, 168), wall_shift=-10):
    ic.gell(box, cl, cd)
    # iç duvar
    ic.ell((inner[0] - 4, inner[1] + wall_shift, inner[2] + 4, inner[3] + wall_shift), shade(cd, 0.85), ow=3)
    ic.clear_ell((inner[0], inner[1], inner[2], inner[3]))
    ic.d.ellipse(B(inner), outline=OUT, width=S(4))
    # parlama
    ic.d.arc(B((box[0] + 14, box[1] + 8, box[2] - 14, box[3] - 8)), 195, 250, fill=WHITE, width=S(5))


def gold_band():
    ic = Icon()
    ring_band(ic, GOLD, GOLD_L, GOLD_D)
    # kazınmış yazı
    for x in range(78, 182, 14):
        y = 182 + 6 * math.sin((x - 128) / 60)
        ic.line([(x, y), (x + 7, y)], shade(GOLD_D, 0.8), 3)
    ic.save('gold_band')


def sapphire_ring():
    ic = Icon()
    ring_band(ic, GOLD, GOLD_L, GOLD_D, box=(46, 110, 238, 210), inner=(78, 130, 206, 186))
    # yuva ve taş
    ic.poly([(40, 120), (80, 40), (140, 70), (110, 150)], SILV, ow=5)
    ic.poly([(56, 116), (84, 56), (128, 78), (104, 136)], hx('#3a54d6'), ow=3)
    ic.poly([(84, 56), (128, 78), (94, 96)], hx('#8fa8ff'), ow=0)
    ic.poly([(56, 116), (94, 96), (104, 136)], hx('#1d2c86'), ow=0)
    ic.poly([(84, 56), (94, 96), (56, 116)], hx('#5d78f0'), ow=0)
    ic.line([(84, 62), (98, 70)], WHITE, 4)
    ic.save('sapphire_ring')


def skull_ring():
    ic = Icon()
    ring_band(ic, SILV, SILV_L, SILV_D, box=(36, 150, 220, 222), inner=(70, 164, 186, 210), wall_shift=-6)
    # sivri başlıklı kafatası
    ic.gpoly([(128, 20), (172, 70), (184, 120), (166, 170), (90, 170), (72, 120), (84, 70)], SILV_L, SILV_D)
    ic.ell((94, 96, 122, 124), hx('#1a1414'), ow=3)
    ic.ell((134, 96, 162, 124), hx('#1a1414'), ow=3)
    ic.poly([(128, 128), (120, 144), (136, 144)], hx('#1a1414'), ow=2)
    ic.rect((98, 150, 158, 172), SILV, ow=4, r=4)
    for x in range(106, 156, 10):
        ic.line([(x, 152), (x, 170)], SILV_D, 3)
    ic.line([(110, 50), (124, 32)], WHITE, 4)
    ic.save('skull_ring')


def bone_hand_ring():
    ic = Icon()
    # gümüş kemik parmaklar halkayı sarar
    ring_band(ic, SILV, SILV_L, SILV_D, box=(24, 70, 232, 196), inner=(58, 96, 198, 168))
    for i, x in enumerate(range(60, 200, 28)):
        top = 74 + 10 * math.sin(i)
        ic.rect((x, top, x + 24, top + 36), SILV, ow=5, r=9)
        ic.line([(x + 4, top + 18), (x + 20, top + 18)], SILV_D, 4)
        ic.rect((x + 2, 166, x + 24, 202), shade(SILV, 0.85), ow=5, r=8)
    ic.save('bone_hand_ring')


def emerald_eye_ring():
    ic = Icon()
    ring_band(ic, SILV, SILV_L, SILV_D, box=(22, 84, 234, 196), inner=(56, 108, 200, 170))
    # süslü kıvrımlar
    ic.line([(40, 120), (70, 96), (100, 110)], SILV_D, 5)
    ic.line([(216, 120), (186, 96), (156, 110)], SILV_D, 5)
    ic.ell((84, 64, 172, 140), GOLD, ow=5)
    ic.gell((98, 76, 158, 128), hx('#7af0a8'), hx('#0e7a44'), ow=3)
    ic.ell((110, 84, 126, 98), hx('#e8fff0'), ow=0)
    ic.save('emerald_eye_ring')


# ------------------------------------------------------------------ gözlükler

def goggles(name, lens, lens_l, deep=False, loupe=False):
    ic = Icon()
    # kayış (arkadan dolanan)
    ic.d.arc(B((20, 70, 236, 230)), 200, 340, fill=OUT, width=S(26))
    ic.d.arc(B((20, 70, 236, 230)), 200, 340, fill=LEATH, width=S(18))
    ic.line([(28, 140), (36, 170)], LEATH_D, 18)
    ic.line([(228, 140), (220, 170)], LEATH_D, 18)
    # köprü
    ic.rect((112, 136, 144, 152), BRASS, ow=4, r=4)
    for cx in (76, 180):
        if deep:
            ic.gell((cx - 52, 98, cx + 52, 202), BRASS_L, BRASS_D)
            ic.ell((cx - 40, 110, cx + 40, 190), shade(BRASS, 0.8), ow=4)
            ic.gell((cx - 30, 120, cx + 30, 180), lens_l, lens, ow=4)
        else:
            ic.gell((cx - 46, 104, cx + 46, 196), BRASS_L, BRASS_D)
            ic.gell((cx - 34, 116, cx + 34, 184), lens_l, lens, ow=4)
        ic.d.arc(B((cx - 26, 124, cx + 26, 176)), 200, 260, fill=WHITE, width=S(6))
        # perçinler
        for a in range(0, 360, 60):
            r = 41 if not deep else 46
            x, y = cx + r * math.cos(math.radians(a)), 150 + r * math.sin(math.radians(a))
            ic.ell((x - 3, y - 3, x + 3, y + 3), BRASS_D, ow=0)
    if loupe:
        cx = 196
        ic.line([(170, 98), (188, 70)], BRASS_D, 8)
        ic.gell((cx - 26, 40, cx + 26, 92), BRASS_L, BRASS_D)
        ic.gell((cx - 17, 49, cx + 17, 83), lens_l, lens, ow=3)
        ic.d.arc(B((cx - 12, 53, cx + 12, 77)), 200, 260, fill=WHITE, width=S(4))
    ic.save(name)


# ------------------------------------------------------------------ çantalar

def stitch(ic, pts, col, step=12):
    for i in range(len(pts) - 1):
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        n = int(math.hypot(x1 - x0, y1 - y0) // step)
        for k in range(n):
            t0, t1 = k / max(n, 1), (k + 0.5) / max(n, 1)
            ic.line([(x0 + (x1 - x0) * t0, y0 + (y1 - y0) * t0), (x0 + (x1 - x0) * t1, y0 + (y1 - y0) * t1)], col, 2)


def satchel():
    ic = Icon()
    body, light, dark = hx('#9aa266'), hx('#c5cb8e'), hx('#646a3c')
    ic.gpoly([(30, 70), (226, 70), (232, 220), (24, 220)], light, dark)
    # yan cep
    ic.gpoly([(214, 130), (248, 136), (244, 214), (226, 220)], body, dark)
    # kapak
    ic.gpoly([(24, 50), (232, 50), (228, 150), (28, 150)], light, body)
    stitch(ic, [(36, 140), (220, 140)], dark)
    stitch(ic, [(36, 60), (220, 60)], dark)
    # kayış ve toka
    ic.rect((112, 58, 144, 196), hx('#8a8a56'), ow=4, r=3)
    ic.rect((100, 158, 156, 198), SILV, ow=5, r=4)
    ic.rect((112, 168, 144, 190), shade(SILV, 0.5), ow=3, r=2)
    ic.line([(128, 168), (128, 190)], SILV_L, 4)
    ic.save('satchel')


def map_window(ic, b):
    ic.rect(b, BRASS, ow=5, r=4)
    x0, y0, x1, y1 = b
    ic.rect((x0 + 8, y0 + 8, x1 - 8, y1 - 8), hx('#e9dcb0'), ow=3, r=2)
    for k in range(4):
        y = y0 + 14 + k * (y1 - y0 - 24) / 3
        ic.line([(x0 + 12, y), (x0 + 30, y - 4), (x1 - 30, y + 3), (x1 - 12, y)], hx('#7a9a6a') if k % 2 else hx('#6a8aaa'), 2)


def traveler_pack():
    ic = Icon()
    body, light, dark = hx('#b0a48c'), hx('#d8ccb2'), hx('#776a54')
    ic.gpoly([(44, 60), (212, 60), (224, 232), (32, 232)], light, dark)
    ic.gpoly([(36, 40), (220, 40), (214, 150), (42, 150)], light, body)
    map_window(ic, (84, 60, 172, 120))
    stitch(ic, [(48, 142), (208, 142)], dark)
    # pirinç kilit dili
    ic.poly([(108, 146), (148, 146), (156, 168), (100, 168)], BRASS, ow=4)
    ic.rect((116, 168, 140, 200), BRASS_L, ow=4, r=3)
    ic.ell((122, 176, 134, 188), BRASS_D, ow=0)
    # yan köşe pirinçleri
    ic.poly([(32, 200), (60, 232), (32, 232)], BRASS, ow=4)
    ic.poly([(224, 200), (196, 232), (224, 232)], BRASS, ow=4)
    ic.save('traveler_pack')


def expedition_pack():
    ic = Icon()
    body, light, dark = hx('#c27a3a'), hx('#e6a05c'), hx('#7a4418')
    ic.gpoly([(36, 50), (220, 50), (230, 236), (26, 236)], light, dark)
    # üstte rulo
    ic.rect((40, 22, 216, 52), hx('#8a5a32'), ow=5, r=12)
    for x in range(56, 210, 16):
        ic.line([(x, 26), (x - 4, 48)], hx('#5a3a20'), 3)
    # harita penceresi
    map_window(ic, (80, 62, 176, 118))
    # yan ve ön cepler
    for bx in ((40, 150, 104, 214), (152, 150, 216, 214)):
        ic.gpoly([(bx[0], bx[1]), (bx[2], bx[1]), (bx[2], bx[3]), (bx[0], bx[3])], light, body)
        ic.rect((bx[0] + 22, bx[1] + 6, bx[0] + 42, bx[1] + 26), BRASS, ow=3, r=3)
    # asma kilit
    ic.d.arc(B((112, 112, 144, 144)), 180, 360, fill=OUT, width=S(10))
    ic.d.arc(B((112, 112, 144, 144)), 180, 360, fill=SILV, width=S(6))
    ic.rect((106, 126, 150, 160), BRASS, ow=4, r=4)
    ic.ell((124, 136, 132, 146), OUT, ow=0)
    ic.save('expedition_pack')


# ------------------------------------------------------------------ iksirler

def heart_tonic():
    ic = Icon()
    ic.rect((86, 14, 150, 44), hx('#3a3a3a'), ow=5, r=4)
    ic.rect((92, 44, 144, 66), hx('#84c4e0'), ow=4, r=2)
    ic.gpoly([(66, 66), (170, 66), (176, 236), (60, 236)], hx('#c6ecff'), hx('#5aa6d6'))
    ic.gpoly([(70, 120), (166, 120), (172, 230), (64, 230)], hx('#9a7ae8'), hx('#3a2a9a'), ow=0)
    ic.line([(70, 120), (166, 120)], hx('#d0c4ff'), 4)
    ic.rect((60, 214, 176, 238), hx('#3a5a7a'), ow=4, r=3)
    ic.line([(80, 80), (80, 200)], WHITE, 6)
    # etiket ve kalp
    ic.line([(150, 70), (196, 100)], hx('#d6c8a8'), 4)
    ic.poly([(176, 92), (232, 112), (220, 216), (172, 196)], hx('#f4efe4'), ow=4)
    h = [(204, 170), (190, 156), (190, 146), (198, 142), (204, 148), (210, 142), (218, 146), (218, 156)]
    ic.poly(h, hx('#e0405a'), ow=2)
    ic.save('heart_tonic')


def ocean_draught():
    ic = Icon()
    ic.rect((110, 8, 146, 38), hx('#b58a5a'), ow=5, r=6)
    ic.gpoly([(104, 38), (152, 38), (150, 70), (196, 150), (206, 200), (180, 236), (76, 236), (50, 200), (60, 150), (106, 70)],
             hx('#9ff2ff'), hx('#1a8aa6'))
    ic.gpoly([(66, 140), (190, 140), (204, 200), (180, 232), (78, 232), (54, 200)], hx('#4ad4e0'), hx('#0e5a76'), ow=0)
    for (x, y, r) in ((100, 170, 8), (140, 190, 6), (160, 160, 5), (118, 206, 4)):
        ic.ell((x - r, y - r, x + r, y + r), hx('#d8fbff'), ow=2, outline=hx('#2a8aa6'))
    ic.line([(84, 150), (72, 196)], WHITE, 6)
    ic.save('ocean_draught')


def black_death():
    ic = Icon()
    # kurukafa tıpa
    ic.gpoly([(128, 4), (156, 28), (154, 58), (102, 58), (100, 28)], SILV_L, SILV_D)
    ic.ell((108, 26, 124, 42), OUT, ow=0)
    ic.ell((132, 26, 148, 42), OUT, ow=0)
    ic.rect((106, 58, 150, 74), SILV, ow=4, r=3)
    # yüzeyli pembe şişe
    ic.gpoly([(110, 74), (146, 74), (204, 210), (184, 240), (72, 240), (52, 210)], hx('#ff6ab0'), hx('#8a1050'))
    ic.poly([(128, 80), (184, 214), (128, 236)], hx('#d02a7a'), ow=0)
    ic.poly([(128, 80), (72, 214), (128, 236)], hx('#ff90c4'), ow=0)
    ic.line([(128, 80), (128, 236)], OUT, 3)
    ic.line([(52, 210), (204, 210)], OUT, 3)
    ic.line([(96, 120), (78, 186)], WHITE, 5)
    ic.save('black_death')


def starlight_elixir():
    ic = Icon()
    # anahtar benzeri tepe
    ic.ell((106, 6, 128, 28), None, ow=5, outline=SILV_D)
    ic.ell((128, 6, 150, 28), None, ow=5, outline=SILV_D)
    ic.rect((120, 24, 136, 64), SILV, ow=4, r=2)
    ic.rect((108, 60, 148, 76), SILV_D, ow=4, r=3)
    ic.gell((40, 70, 216, 236), hx('#6a8aff'), hx('#14147a'))
    # kafes bantları
    ic.d.arc(B((40, 70, 216, 236)), 0, 360, fill=OUT, width=S(5))
    ic.line([(128, 74), (128, 234)], SILV_D, 6)
    ic.d.arc(B((40, 120, 216, 186)), 0, 180, fill=SILV_D, width=S(6))
    ic.d.arc(B((40, 120, 216, 186)), 180, 360, fill=SILV_D, width=S(4))
    ic.d.arc(B((60, 90, 196, 216)), 210, 260, fill=WHITE, width=S(8))
    for (x, y) in ((96, 140), (160, 116), (150, 200), (84, 190)):
        ic.ell((x - 4, y - 4, x + 4, y + 4), hx('#f4f4ff'), ow=0)
    ic.rect((92, 228, 164, 246), SILV, ow=4, r=4)
    ic.save('starlight_elixir')


def sun_essence():
    ic = Icon()
    ic.ell((116, 2, 140, 26), None, ow=6, outline=hx('#3a3a3a'))
    ic.rect((110, 22, 146, 52), BRASS, ow=4, r=4)
    ic.gpoly([(104, 52), (152, 52), (164, 200), (140, 246), (116, 246), (92, 200)], hx('#fff4a0'), hx('#d48a10'), vertical=False)
    ic.gpoly([(118, 60), (138, 60), (146, 196), (128, 236), (110, 196)], hx('#fffbe0'), hx('#f0b830'), ow=0)
    ic.rect((96, 124, 160, 140), BRASS, ow=4, r=3)
    ic.line([(110, 70), (104, 190)], WHITE, 5)
    ic.save('sun_essence')


# ------------------------------------------------------------------ kitaplar

def book_base(ic, cover, cover_l, cover_d, pages=hx('#f0e2c0')):
    ic.gpoly([(70, 30), (220, 22), (226, 222), (76, 234)], pages, shade(pages, 0.8))
    for y in range(40, 220, 10):
        ic.line([(214, y), (222, y - 1)], shade(pages, 0.7), 2)
    ic.gpoly([(30, 26), (200, 18), (206, 220), (36, 236)], cover_l, cover_d)
    ic.gpoly([(24, 28), (44, 26), (50, 236), (30, 238)], shade(cover, 0.7), shade(cover, 0.5))


def apprentice_spellbook():
    ic = Icon()
    book_base(ic, hx('#c47a34'), hx('#e09a4c'), hx('#8a4a18'))
    # kağıt etiket
    ic.poly([(72, 92), (176, 86), (180, 136), (76, 142)], hx('#f2ece0'), ow=4)
    # SPELLBOOK harfleri (karalama)
    for i, x in enumerate(range(82, 172, 10)):
        ic.line([(x, 120 - i * 0.6), (x + 5, 104 - i * 0.6)], hx('#2a2a2a'), 3)
        ic.line([(x + 5, 104 - i * 0.6), (x + 8, 118 - i * 0.6)], hx('#2a2a2a'), 3)
    # bağ ipleri
    for y in (54, 196):
        ic.line([(26, y), (120, y - 4)], hx('#d8c8a0'), 5)
        ic.ell((116, y - 10, 128, y + 2), hx('#d8c8a0'), ow=3)
    ic.save('apprentice_spellbook')


def traveler_tome():
    ic = Icon()
    book_base(ic, hx('#8a4e2a'), hx('#a8663a'), hx('#5a2e16'))
    # mor ayraç
    ic.poly([(150, 4), (172, 4), (172, 50), (161, 40), (150, 50)], hx('#8a5ac8'), ow=4)
    ic.poly([(70, 30), (84, 30), (90, 120), (76, 120)], hx('#9a6ad0'), ow=3)
    # metal köşeler
    ic.poly([(176, 20), (200, 18), (202, 46)], SILV, ow=4)
    ic.poly([(206, 196), (206, 220), (180, 222)], SILV, ow=4)
    ic.poly([(36, 210), (36, 236), (62, 234)], SILV, ow=4)
    # sarılı kayış
    ic.gpoly([(20, 140), (232, 120), (234, 152), (22, 172)], hx('#ece4d4'), hx('#a89c88'))
    ic.line([(126, 132), (132, 164)], hx('#8a7e6a'), 3)
    ic.save('traveler_tome')


def black_grimoire():
    ic = Icon()
    book_base(ic, hx('#2a2a30'), hx('#3e3e48'), hx('#141418'), pages=hx('#e8c860'))
    # pirinç köşeler ve kenar
    for pts in ([(30, 26), (70, 24), (32, 62)], [(200, 18), (204, 58), (164, 20)], [(36, 236), (34, 196), (76, 234)], [(206, 220), (166, 222), (206, 182)]):
        ic.poly(pts, BRASS, ow=4)
    # deri kemerler
    ic.gpoly([(18, 120), (236, 108), (238, 146), (20, 160)], hx('#a8683a'), hx('#6a3a1a'))
    ic.rect((176, 104, 214, 150), BRASS, ow=4, r=3)
    ic.rect((186, 114, 204, 140), shade(BRASS, 0.5), ow=2, r=2)
    ic.line([(100, 22), (104, 236)], hx('#5a3a1a'), 12)
    ic.line([(100, 22), (104, 236)], hx('#9a6a3a'), 6)
    # kurdeleler
    ic.poly([(120, 0), (138, 2), (136, 30), (122, 28)], hx('#c03050'), ow=3)
    ic.poly([(150, 6), (164, 6), (162, 30), (150, 30)], hx('#d0b030'), ow=3)
    ic.save('black_grimoire')


# ------------------------------------------------------------------ kemerler

def belt(name, band, band_d, buckle, pouches=0, vials=False):
    ic = Icon()
    # kemer halkası (perspektif)
    ic.d.ellipse(B((10, 96, 246, 190)), outline=OUT, width=S(40))
    ic.d.ellipse(B((10, 96, 246, 190)), outline=band, width=S(30))
    ic.d.arc(B((14, 100, 242, 186)), 180, 360, fill=band_d, width=S(14))
    # delikler
    for x in (40, 58, 76):
        ic.ell((x - 3, 170, x + 3, 176), band_d, ow=0)
    if vials:
        for i, (x, col) in enumerate(((70, '#d03a3a'), (96, '#3ac06a'), (186, '#3a7ad0'))):
            ic.rect((x - 9, 120, x + 9, 176), hx(col), ow=4, r=6)
            ic.rect((x - 7, 112, x + 7, 124), hx('#b08a5a'), ow=3, r=2)
            ic.line([(x - 4, 128), (x - 4, 168)], WHITE, 3)
    for i in range(pouches):
        x = 150 + i * 48 if not vials else 210
        if vials and i > 0:
            x = 40
        ic.gpoly([(x - 22, 140), (x + 22, 140), (x + 20, 200), (x - 20, 200)], shade(band, 1.15), band_d)
        ic.poly([(x - 24, 136), (x + 24, 136), (x + 20, 162), (x - 20, 162)], shade(band, 1.25), ow=4)
        ic.ell((x - 4, 152, x + 4, 160), BRASS, ow=2)
    # toka
    ic.rect((104, 150, 152, 194), buckle, ow=5, r=4)
    ic.rect((114, 160, 142, 184), shade(band, 0.8), ow=3, r=2)
    ic.line([(128, 160), (128, 184)], shade(buckle, 1.3), 5)
    ic.save(name)


# ------------------------------------------------------------------ anahtarlar

def key_shape(ic, x0, y0, x1, y1, c, cl, cd, bow='oval', teeth=2, w=24):
    """(x0,y0) halka ucu, (x1,y1) diş ucu."""
    ang = math.atan2(y1 - y0, x1 - x0)
    nx, ny = -math.sin(ang), math.cos(ang)
    ic.line([(x0, y0), (x1, y1)], OUT, w + 10)
    ic.line([(x0, y0), (x1, y1)], c, w)
    ic.line([(x0 + nx * 3, y0 + ny * 3), (x1 + nx * 3, y1 + ny * 3)], cl, 3)
    # dişler
    for k in range(teeth):
        t = 0.82 + k * 0.09
        bx, by = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        pts = [(bx, by), (bx + nx * 38, by + ny * 38), (bx + nx * 38 + (x1 - x0) * 0.08, by + ny * 38 + (y1 - y0) * 0.08),
               (bx + (x1 - x0) * 0.08, by + (y1 - y0) * 0.08)]
        ic.poly(pts, c, ow=4)
    # halka
    if bow == 'oval':
        ic.ell((x0 - 46, y0 - 46, x0 + 46, y0 + 46), c, ow=7)
        ic.clear_ell((x0 - 20, y0 - 20, x0 + 20, y0 + 20))
        ic.d.ellipse(B((x0 - 20, y0 - 20, x0 + 20, y0 + 20)), outline=OUT, width=S(5))
        ic.d.arc(B((x0 - 36, y0 - 36, x0 + 36, y0 + 36)), 200, 260, fill=cl, width=S(6))
    elif bow == 'heart':
        s = 54
        pts = []
        for i in range(48):
            t = i / 48 * math.pi * 2
            hx_ = 16 * math.sin(t) ** 3
            hy = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
            pts.append((x0 + hx_ * s / 16, y0 + hy * s / 16))
        ic.poly(pts, c, ow=6)
        ic.clear_ell((x0 - 12, y0 - 14, x0 + 12, y0 + 6))
        ic.d.ellipse(B((x0 - 12, y0 - 14, x0 + 12, y0 + 6)), outline=OUT, width=S(5))


def iron_key():
    ic = Icon()
    key_shape(ic, 60, 196, 214, 42, SILV, SILV_L, SILV_D, teeth=2)
    ic.save('iron_key')


def dungeon_key():
    ic = Icon()
    # halkaya geçirilmiş iki eski anahtar
    ic.d.ellipse(B((22, 150, 98, 226)), outline=OUT, width=S(12))
    ic.d.ellipse(B((22, 150, 98, 226)), outline=SILV, width=S(6))
    key_shape(ic, 72, 160, 214, 30, hx('#8a949a'), SILV_L, SILV_D, teeth=2, w=18)
    key_shape(ic, 100, 196, 240, 124, hx('#7a8288'), SILV_L, SILV_D, teeth=2, w=18)
    ic.save('dungeon_key')


def heart_key():
    ic = Icon()
    key_shape(ic, 70, 180, 206, 50, GOLD, GOLD_L, GOLD_D, bow='heart', teeth=1, w=20)
    ic.save('heart_key')


def mechanic_key():
    ic = Icon()
    ic.line([(70, 190), (196, 64)], OUT, 30)
    ic.line([(70, 190), (196, 64)], SILV, 20)
    # dişli baş
    cx, cy = 66, 194
    pts = []
    for i in range(16):
        a = i / 16 * math.pi * 2
        r = 38 if i % 2 == 0 else 28
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    ic.poly(pts, hx('#d07a30'), ow=5)
    ic.ell((cx - 12, cy - 12, cx + 12, cy + 12), SILV_D, ow=4)
    # sıkıştırma halkası ve uç
    ic.rect((130, 108, 160, 138), hx('#d07a30'), ow=4, r=4)
    ic.poly([(196, 64), (214, 30), (226, 40), (206, 76)], SILV_L, ow=4)
    ic.poly([(176, 84), (204, 92), (198, 104), (170, 96)], SILV, ow=3)
    ic.save('mechanic_key')


def thorn_lockpick():
    ic = Icon()
    ic.line([(64, 196), (212, 40)], OUT, 34)
    ic.line([(64, 196), (212, 40)], hx('#5a5a66'), 22)
    for t in (0.3, 0.48, 0.66, 0.84):
        x, y = 64 + 148 * t, 196 - 156 * t
        ic.poly([(x - 8, y - 6), (x + 44, y + 16), (x + 8, y + 12)], hx('#8a8a98'), ow=4)
        ic.poly([(x - 8, y - 6), (x - 18, y - 46), (x - 14, y + 4)], hx('#8a8a98'), ow=4)
    ic.poly([(212, 40), (240, 12), (226, 50)], SILV_L, ow=4)
    # kemik sap
    ic.gpoly([(30, 214), (50, 176), (88, 190), (70, 232)], hx('#e8e0c8'), hx('#a89c80'))
    ic.ell((40, 160, 66, 186), hx('#e8e0c8'), ow=4)
    ic.save('thorn_lockpick')


def crystal_key():
    ic = Icon()
    ic.line([(80, 200), (190, 64)], OUT, 36)
    ic.line([(80, 200), (190, 64)], hx('#b8c8d8'), 24)
    # kristal sap ve uç
    ic.gpoly([(186, 20), (232, 44), (214, 96), (170, 80)], hx('#bfe6ff'), hx('#2a64d0'))
    ic.poly([(186, 20), (214, 96), (196, 60)], hx('#7ab0ff'), ow=0)
    ic.gpoly([(36, 214), (60, 150), (104, 170), (84, 236)], hx('#9ad8ff'), hx('#1a40b0'))
    ic.poly([(60, 150), (84, 236), (72, 190)], hx('#d8f0ff'), ow=0)
    ic.line([(48, 200), (62, 166)], WHITE, 4)
    ic.save('crystal_key')


def key_ring():
    ic = Icon()
    ic.d.ellipse(B((60, 10, 196, 120)), outline=OUT, width=S(14))
    ic.d.ellipse(B((60, 10, 196, 120)), outline=SILV, width=S(7))
    cols = [(SILV, SILV_L, SILV_D), (GOLD, GOLD_L, GOLD_D), (hx('#8a949a'), SILV_L, SILV_D), (BRASS, BRASS_L, BRASS_D)]
    for i, (x, ang) in enumerate(((84, 1.9), (112, 1.7), (142, 1.45), (170, 1.25))):
        x1, y1 = x + 120 * math.cos(ang), 104 + 120 * math.sin(ang)
        c = cols[i]
        ic.line([(x, 104), (x1, y1)], OUT, 20)
        ic.line([(x, 104), (x1, y1)], c[0], 11)
        ic.rect((x1 - 14, y1 - 22, x1 + 4, y1 - 8), c[0], ow=3, r=2)
        ic.ell((x - 12, 96, x + 12, 120), c[0], ow=4)
    ic.save('key_ring')


# ------------------------------------------------------------------ 3D model dokuları (kutu UV)

class Tex:
    def __init__(self, w, h, scale=2, seed=1):
        self.s = scale
        self.img = Image.new('RGBA', (w * scale, h * scale), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rng = random.Random(seed)

    def noise(self, c, a=0.06):
        f = 1 + (self.rng.random() - 0.5) * 2 * a
        return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)

    def face(self, x0, y0, w, h, fn):
        for j in range(int(h * self.s)):
            for i in range(int(w * self.s)):
                u, v = (i + 0.5) / self.s, (j + 0.5) / self.s
                c = fn(u, v, w, h)
                if c is None:
                    continue
                X, Y = int(x0 * self.s) + i, int(y0 * self.s) + j
                if 0 <= X < self.img.width and 0 <= Y < self.img.height:
                    self.px[X, Y] = c

    def box(self, u, v, w, h, d, all=None, front=None, back=None, sides=None, top=None, bottom=None):
        front, back, sides, top, bottom = front or all, back or all, sides or all, top or all, bottom or all
        self.face(u + d, v, w, d, top)
        self.face(u + d + w, v, w, d, bottom)
        self.face(u, v + d, d, h, sides)
        self.face(u + d, v + d, w, h, front)
        self.face(u + d + w, v + d, d, h, sides)
        self.face(u + d + w + d, v + d, w, h, back)

    def save(self, name):
        os.makedirs(os.path.join(ROOT, 'models'), exist_ok=True)
        self.img.save(os.path.join(ROOT, 'models', name + '.png'))


def flat(t, c, a=0.05):
    return lambda u, v, w, h: t.noise(c, a)


def leather_fn(t, c, stitch_col=None):
    def f(u, v, w, h):
        col = c
        if stitch_col and (abs(v - 0.35) < 0.15 or abs(v - (h - 0.35)) < 0.15) and int(u * 2) % 2 == 0:
            col = stitch_col
        return t.noise(col, 0.08)
    return f


def canvas_fn(t, c):
    def f(u, v, w, h):
        k = (int(u * 2) + int(v * 2)) % 2
        return t.noise(shade(c, 1.04 if k else 0.96), 0.04)
    return f


def rim_fn(t, c, rim, glass=None, border=0.6):
    def f(u, v, w, h):
        if u < border or u > w - border or v < border or v > h - border:
            return t.noise(rim, 0.06)
        if glass:
            return glass(u, v, w, h)
        return t.noise(c, 0.05)
    return f


def glass_fn(t, c, hl):
    def f(u, v, w, h):
        if abs(u - v) < 0.4 and u < w * 0.6:
            return hl
        return t.noise(c, 0.03)
    return f


def goggles_tex(v, lens):
    t = Tex(64, 32, seed=v)
    strap = leather_fn(t, LEATH, hx('#c8a878'))
    t.box(0, 0, 9, 2, 1, all=strap)
    t.box(0, 4, 1, 2, 9, all=strap)
    t.box(20, 4, 1, 2, 9, all=strap)
    t.box(20, 0, 2, 1, 1, all=flat(t, BRASS))
    frame = rim_fn(t, BRASS, BRASS_D, border=0.5)
    hole = rim_fn(t, BRASS_L, BRASS_D, glass=lambda u, vv, w, h: None, border=0.6)
    t.box(0, 16, 4, 4, 2, all=frame, front=hole, back=hole)
    t.box(12, 16, 4, 4, 2, all=frame, front=hole, back=hole)
    g = glass_fn(t, lens, (255, 255, 255, 220))
    for u in (24, 32):
        t.box(u, 16, 3, 3, 1, all=g)
        t.box(u, 20, 3, 3, 1, all=g)
    t.box(0, 22, 4, 4, 1, all=rim_fn(t, BRASS, BRASS_D, border=0.5), front=hole, back=hole)
    t.box(10, 22, 4, 4, 1, all=rim_fn(t, BRASS, BRASS_D, border=0.5), front=hole, back=hole)
    t.box(40, 0, 2, 2, 2, all=rim_fn(t, BRASS_L, BRASS_D, border=0.4))
    t.box(48, 0, 2, 2, 1, all=g)
    t.box(40, 4, 1, 1, 3, all=flat(t, BRASS_D))
    t.save('goggles_%d' % v)


def belt_tex(v, band, buckle):
    t = Tex(64, 32, seed=10 + v)

    def bandf(u, vv, w, h):
        c = band
        if vv < 0.3 or vv > h - 0.3:
            c = shade(band, 0.75)
        elif abs(vv - 0.55) < 0.12 and int(u * 2) % 2 == 0:
            c = hx('#d8c8a0')
        return t.noise(c, 0.07)
    t.box(0, 0, 9, 2, 5, all=bandf)
    t.box(28, 0, 3, 2, 1, all=rim_fn(t, shade(band, 0.6), buckle, border=0.45))
    pouch = lambda u, vv, w, h: t.noise(shade(band, 1.15) if vv < 1.0 else shade(band, 0.95), 0.07)
    t.box(36, 0, 2, 3, 3, all=pouch)
    t.box(46, 0, 2, 3, 3, all=pouch)
    t.box(0, 8, 4, 3, 2, all=pouch, back=lambda u, vv, w, h: t.noise(BRASS if abs(u - 2) < 0.4 and abs(vv - 1.0) < 0.4 else shade(band, 1.1), 0.06))
    for i, (u, col) in enumerate(((12, '#d03a3a'), (16, '#3ac06a'), (20, '#3a7ad0'))):
        t.box(u, 8, 1, 3, 1, all=lambda uu, vv, w, h, col=col: t.noise(hx('#b08a5a') if vv < 0.7 else hx(col), 0.05))
    t.save('belt_%d' % v)


def backpack_tex(v):
    t = Tex(64, 64, seed=20 + v)
    strap = leather_fn(t, hx('#6a4228'), hx('#b89870'))
    t.box(0, 0, 1, 9, 1, all=strap)
    t.box(4, 0, 1, 1, 5, all=strap)
    t.box(16, 0, 1, 9, 1, all=strap)
    t.box(20, 0, 1, 1, 5, all=strap)
    if v == 0:
        body = hx('#9aa266')
        cf = canvas_fn(t, body)
        t.box(0, 10, 7, 7, 3, all=cf)
        t.box(20, 10, 7, 6, 1, all=canvas_fn(t, shade(body, 1.12)),
              back=lambda u, vv, w, h: t.noise(hx('#646a3c') if vv > h - 0.4 else shade(body, 1.12), 0.05))
        t.box(36, 10, 1, 16, 1, all=strap)
        t.box(40, 10, 2, 2, 1, all=rim_fn(t, shade(SILV, 0.5), SILV, border=0.4))
    elif v == 1:
        body = hx('#b0a48c')
        cf = canvas_fn(t, body)
        t.box(0, 27, 8, 10, 4, all=cf)
        t.box(24, 27, 8, 3, 5, all=canvas_fn(t, shade(body, 1.1)))

        def mapflap(u, vv, w, h):
            if 1.5 < u < w - 1.5 and 0.8 < vv < h - 1.2:
                if u < 2.0 or u > w - 2.0 or vv < 1.2 or vv > h - 1.6:
                    return t.noise(BRASS, 0.05)
                k = math.sin(u * 2.2 + vv * 1.3)
                return t.noise(hx('#e9dcb0') if abs(k) > 0.25 else hx('#7a9a6a'), 0.04)
            return t.noise(shade(body, 1.1), 0.05)
        t.box(24, 35, 8, 5, 1, all=canvas_fn(t, shade(body, 1.1)), back=mapflap)
        t.box(50, 27, 5, 4, 1, all=canvas_fn(t, shade(body, 0.95)))
        t.box(50, 32, 1, 2, 1, all=flat(t, BRASS))
    else:
        body = hx('#c27a3a')
        lf = leather_fn(t, body, hx('#f0d0a0'))
        t.box(0, 41, 9, 11, 5, all=lf)
        t.box(28, 41, 10, 3, 3, all=lambda u, vv, w, h: t.noise(hx('#3a2a20') if int(u) % 4 == 0 else hx('#8a5a32'), 0.07))
        t.box(28, 47, 2, 5, 3, all=leather_fn(t, shade(body, 1.08)))
        t.box(38, 47, 2, 5, 3, all=leather_fn(t, shade(body, 1.08)))
        t.box(48, 47, 6, 4, 1, all=leather_fn(t, shade(body, 1.1)),
              back=lambda u, vv, w, h: t.noise(BRASS if abs(u - 3) < 0.6 and vv < 1.4 else shade(body, 1.1), 0.05))

        def mapflap(u, vv, w, h):
            if 2 < u < w - 2 and 0.8 < vv < h - 0.8:
                if u < 2.5 or u > w - 2.5 or vv < 1.2 or vv > h - 1.2:
                    return t.noise(BRASS, 0.05)
                k = math.sin(u * 2.0 + vv * 1.7)
                return t.noise(hx('#e9dcb0') if abs(k) > 0.25 else hx('#6a8aaa'), 0.04)
            return t.noise(shade(body, 1.12), 0.05)
        t.box(0, 57, 9, 5, 1, all=leather_fn(t, shade(body, 1.12)), back=mapflap)
        t.box(20, 57, 1, 2, 1, all=flat(t, SILV))
    t.save('backpack_%d' % v)


def book_tex(v, cover, trim, pages, deco):
    """Vanilla kitap modeli (64x32) yerleşimi: kapaklar (0,0)/(16,0), sırt (12,0), sayfalar (0,10)/(12,10), dönen sayfa (24,10)."""
    t = Tex(64, 32, seed=30 + v)

    def cov(u, vv, w, h):
        if u < 0.5 or u > w - 0.5 or vv < 0.5 or vv > h - 0.5:
            return t.noise(trim, 0.06)
        return deco(u, vv, w, h)
    for x in (0, 6, 16, 22):
        t.face(x, 0, 6, 10, cov)
    t.face(12, 0, 4, 10, lambda u, vv, w, h: t.noise(trim if int(vv) % 3 == 0 else shade(cover, 0.8), 0.06))

    def page(u, vv, w, h):
        if int(vv * 2) % 3 == 1 and 0.6 < u < w - 0.6 and (u * 7 + vv * 3) % 5 > 1:
            return t.noise(shade(pages, 0.55), 0.05)
        return t.noise(pages, 0.03)
    t.box(0, 10, 5, 8, 1, all=page)
    t.box(12, 10, 5, 8, 1, all=page)
    t.face(24, 10, 10, 8, page)
    t.save('book_%d' % v)
    return t


def make_books():
    def orange(u, v, w, h):
        if 1.2 < u < w - 1.2 and 3.0 < v < 5.5:
            return (242, 236, 224, 255) if not (2.0 < v < 2.3) else (40, 40, 40, 255)
        if abs(v - 1.6) < 0.2 or abs(v - 8.4) < 0.2:
            return (216, 200, 160, 255)
        return (196, 122, 52, 255)
    book_tex(0, hx('#c47a34'), hx('#8a4a18'), hx('#f0e2c0'), orange)

    def brown(u, v, w, h):
        if 4.6 < v < 5.8:
            return (236, 228, 212, 255)
        if u < 1.2 and (v < 1.2 or v > h - 1.2):
            return SILV
        return (138, 78, 42, 255) if (int(u * 3) + int(v * 3)) % 7 else (118, 66, 34, 255)
    book_tex(1, hx('#8a4e2a'), hx('#5a2e16'), hx('#f0e2c0'), brown)

    def black(u, v, w, h):
        if 4.4 < v < 5.8:
            return (168, 104, 58, 255) if not (w - 2 < u < w - 0.8) else BRASS
        if 2.2 < u < 2.9:
            return (154, 106, 58, 255)
        if (u < 1.3 and v < 1.3) or (u < 1.3 and v > h - 1.3):
            return BRASS
        return (42, 42, 48, 255)
    t = book_tex(2, hx('#2a2a30'), BRASS_D, hx('#e8c860'), black)
    # parlayan rünler (yalnızca sayfalarda)
    g = Tex(64, 32, seed=99)

    def rune(u, v, w, h):
        if (int(u * 2) * 7 + int(v * 2) * 3) % 5 == 0 and 0.5 < u < w - 0.5 and 0.5 < v < h - 0.5:
            return (190, 90, 255, 255)
        return None
    g.box(0, 10, 5, 8, 1, all=rune)
    g.box(12, 10, 5, 8, 1, all=rune)
    g.face(24, 10, 10, 8, rune)
    g.save('book_2_glow')


# ------------------------------------------------------------------ kilitli sandık blok dokuları

def block_textures():
    os.makedirs(os.path.join(ROOT, 'block'), exist_ok=True)
    rng = random.Random(5)
    wood, wood_d, iron = hx('#6a4628'), hx('#4a2e18'), hx('#5a5e64')

    def plank(img, iron_rows=(), iron_cols=()):
        px = img.load()
        for y in range(16):
            for x in range(16):
                c = wood if (y // 4) % 2 == 0 else shade(wood, 0.9)
                if y % 4 == 3:
                    c = wood_d
                c = shade(c, 1 + (rng.random() - 0.5) * 0.12)
                if y in iron_rows or x in iron_cols:
                    c = shade(iron, 1 + (rng.random() - 0.5) * 0.15)
                    if (x + y) % 5 == 0:
                        c = hx('#9aa0a6')
                px[x, y] = c

    side = Image.new('RGBA', (16, 16))
    plank(side, iron_rows=(1, 2, 13, 14), iron_cols=(0, 15))
    side.save(os.path.join(ROOT, 'block', 'locked_chest_side.png'))
    top = Image.new('RGBA', (16, 16))
    plank(top, iron_cols=(0, 7, 8, 15))
    top.save(os.path.join(ROOT, 'block', 'locked_chest_top.png'))
    front = Image.new('RGBA', (16, 16))
    plank(front, iron_rows=(1, 14), iron_cols=(0, 15))
    fp = front.load()
    for x in range(16):
        fp[x, 6] = hx('#1e140a')
    front.save(os.path.join(ROOT, 'block', 'locked_chest_front.png'))
    locks = {'iron': (hx('#a8aeb4'), hx('#5a6066'), None), 'bone': (hx('#e8e0c8'), hx('#8a7e64'), hx('#2a2420')),
             'heart': (hx('#e6b83c'), hx('#8a6018'), hx('#d0283c'))}
    for name, (c, d, mark) in locks.items():
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        p = im.load()
        for y in range(16):
            for x in range(16):
                # kilit gövdesi (alt yarı) ve kanca (üst yarı)
                if 7 <= y <= 15 and 2 <= x <= 13:
                    col = c if not (y in (7, 15) or x in (2, 13)) else d
                    if 10 <= y <= 12 and 7 <= x <= 8:
                        col = hx('#141010')
                    p[x, y] = shade(col, 1 + (rng.random() - 0.5) * 0.1)
                elif 0 <= y <= 6 and (x in (4, 5, 10, 11) or (y <= 1 and 4 <= x <= 11)):
                    p[x, y] = shade(hx('#c0c6cc'), 1 + (rng.random() - 0.5) * 0.1)
        if mark is not None and name == 'heart':
            for (x, y) in ((5, 9), (6, 9), (9, 9), (10, 9), (4, 10), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (11, 10),
                           (5, 11), (6, 11), (9, 11), (10, 11), (6, 12), (9, 12), (7, 13), (8, 13)):
                p[x, y] = mark
        if mark is not None and name == 'bone':
            for (x, y) in ((5, 9), (6, 9), (9, 9), (10, 9), (5, 10), (10, 10), (7, 13), (8, 13), (6, 13), (9, 13)):
                p[x, y] = mark
        im.save(os.path.join(ROOT, 'block', 'lock_%s.png' % name))


def materials():
    """3D modellerin malzeme dokuları (blok atlasına girer; çoğu gri tonlu, köşe rengiyle boyanır)."""
    out = os.path.join(ROOT, 'block')
    os.makedirs(out, exist_ok=True)
    rng = random.Random(42)

    def save(name, im):
        im.save(os.path.join(out, 'm_' + name + '.png'))

    def gray(n, fn):
        im = Image.new('RGBA', (n, n))
        p = im.load()
        for y in range(n):
            for x in range(n):
                v = max(0, min(255, int(fn(x / n, y / n, x, y))))
                p[x, y] = (v, v, v, 255)
        return im

    save('plain', gray(16, lambda u, v, x, y: 245 + rng.random() * 10))
    streak = [rng.random() for _ in range(64)]
    save('metal', gray(64, lambda u, v, x, y: 175 + 45 * math.exp(-((v - 0.32) / 0.12) ** 2) + 30 * math.exp(-((v - 0.75) / 0.08) ** 2)
                       + 18 * (streak[y] - 0.5) + 8 * math.sin(u * 40 + streak[y] * 6)))

    def engrave(u, v, x, y):
        base = 185 + 40 * math.exp(-((v - 0.35) / 0.15) ** 2) + 10 * (streak[y] - 0.5)
        gx = int(u * 16)
        lx, ly = (u * 16) % 1, v
        if 0.3 < ly < 0.7 and gx % 2 == 0:
            k = (gx * 7) % 4
            if (k == 0 and abs(lx - 0.5) < 0.12) or (k == 1 and abs(ly - 0.5) < 0.06) or (k == 2 and abs((lx - 0.2) - (ly - 0.3) * 1.5) < 0.12) \
                    or (k == 3 and (abs(lx - 0.3) < 0.1 or abs(ly - 0.62) < 0.05)):
                return 95
        return base
    save('engrave', gray(64, engrave))
    save('bone', gray(32, lambda u, v, x, y: 232 + rng.random() * 18 - (30 if rng.random() < 0.04 else 0)))
    save('cork', gray(32, lambda u, v, x, y: 205 + rng.random() * 50 - (70 if rng.random() < 0.12 else 0)))
    save('paper', gray(32, lambda u, v, x, y: 238 + rng.random() * 14 - (25 if (y % 4 == 2 and 0.15 < u < 0.85 and rng.random() < 0.7) else 0)))
    save('glass', gray(32, lambda u, v, x, y: 235 + 20 * math.exp(-((u - 0.25) / 0.05) ** 2) + rng.random() * 5))
    save('liquid', gray(32, lambda u, v, x, y: 215 + 35 * (1 - v) + 10 * math.sin(u * 18 + v * 6)))
    save('gem', gray(32, lambda u, v, x, y: 200 + 55 * (((int(u * 6) + int(v * 6)) % 3) / 2) - (40 if abs((u * 6) % 1 - 0.5) < 0.05 else 0)))
    save('crystal', gray(32, lambda u, v, x, y: 215 + 40 * abs(math.sin(u * 9 + v * 3)) + rng.random() * 6))
    grain = [[rng.random() for _ in range(64)] for _ in range(64)]
    save('leather', gray(64, lambda u, v, x, y: 205 + 30 * (grain[y][x] - 0.5) - (35 if grain[(y * 3) % 64][(x * 5) % 64] < 0.04 else 0)))

    def stitch(u, v, x, y):
        b = 205 + 28 * (grain[y][x] - 0.5)
        if (abs(v - 0.14) < 0.035 or abs(v - 0.86) < 0.035) and (x // 3) % 2 == 0:
            return 255
        if v < 0.05 or v > 0.95:
            return b * 0.75
        return b
    save('stitch', gray(64, stitch))
    save('canvas', gray(64, lambda u, v, x, y: 200 + (22 if (x + (y // 2) * 2) % 4 < 2 else -10) * (0.6 if y % 2 else 1) + 12 * (grain[y][x] - 0.5)))
    save('cloth', gray(32, lambda u, v, x, y: 200 + (40 if (x // 4) % 3 == 0 else 0) - (50 if (y // 4) % 4 == 1 else 0) + rng.random() * 10))
    save('pages', gray(32, lambda u, v, x, y: 245 if y % 3 else 200))

    # renkli dokular
    def colored(n, fn):
        im = Image.new('RGBA', (n, n))
        p = im.load()
        for y in range(n):
            for x in range(n):
                p[x, y] = fn(x / n, y / n, x, y)
        return im

    parch = hx('#ead9a8')

    def mapf(u, v, x, y):
        c = shade(parch, 0.95 + rng.random() * 0.08)
        coast = 0.5 + 0.18 * math.sin(u * 7 + 1) + 0.08 * math.sin(u * 17)
        if v > coast:
            c = mix(hx('#7fa8c8'), parch, 0.35)
        if abs(v - coast) < 0.025:
            c = hx('#5a4a32')
        if 0.2 < u < 0.45 and abs(v - (0.3 + 0.1 * math.sin(u * 20))) < 0.02 and int(u * 60) % 2 == 0:
            c = hx('#8a3a2a')
        if abs(u - 0.68) < 0.05 and abs(v - 0.28) < 0.05 and (abs((u - 0.68) - (v - 0.28)) < 0.02 or abs((u - 0.68) + (v - 0.28)) < 0.02):
            c = hx('#c02020')
        if (u < 0.04 or u > 0.96 or v < 0.04 or v > 0.96):
            c = shade(parch, 0.7)
        return c
    save('map', colored(64, mapf))

    def tag(u, v, x, y):
        c = shade(hx('#f4efe2'), 0.96 + rng.random() * 0.06)
        if math.hypot(u - 0.5, v - 0.12) < 0.07:
            return shade(c, 0.55)
        hx_, hy = (u - 0.5) / 0.32, -(v - 0.58) / 0.32
        if (hx_ ** 2 + hy ** 2 - 1) ** 3 - hx_ ** 2 * hy ** 3 < 0:
            return hx('#d8304a')
        if u < 0.05 or u > 0.95 or v > 0.96:
            return shade(c, 0.8)
        return c
    save('tag', colored(32, tag))

    # SPELLBOOK etiketi (3x5 piksel yazı tipi)
    font = {'S': ['111', '100', '111', '001', '111'], 'P': ['111', '101', '111', '100', '100'], 'E': ['111', '100', '110', '100', '111'],
            'L': ['100', '100', '100', '100', '111'], 'B': ['110', '101', '110', '101', '110'], 'O': ['111', '101', '101', '101', '111'],
            'K': ['101', '110', '100', '110', '101']}
    lab = Image.new('RGBA', (64, 32), hx('#f2ecdc'))
    lp = lab.load()
    for y in range(32):
        for x in range(64):
            lp[x, y] = shade(hx('#f2ecdc'), 0.95 + rng.random() * 0.07)
            if x < 2 or x > 61 or y < 2 or y > 29:
                lp[x, y] = shade(hx('#f2ecdc'), 0.78)
    word = 'SPELLBOOK'
    x0 = (64 - (len(word) * 4 * 1.5)) / 2
    for i, ch in enumerate(word):
        g = font[ch]
        for ry in range(5):
            for rx in range(3):
                if g[ry][rx] == '1':
                    for sy in range(2):
                        for sx in range(1):
                            X, Y = int(x0 + i * 6 + rx * 1.5) + sx, 12 + ry * 2 + sy
                            if 0 <= X < 64:
                                lp[X, Y] = hx('#2a2622')
                                if X + 1 < 64:
                                    lp[X + 1, Y] = hx('#2a2622')
    save('label', lab)

    def rune(u, v, x, y):
        gx, gy = int(u * 6), int(v * 8)
        lx, ly = (u * 6) % 1, (v * 8) % 1
        k = (gx * 13 + gy * 7) % 5
        on = (k == 0 and abs(lx - 0.5) < 0.12) or (k == 1 and abs(ly - 0.5) < 0.1 and 0.2 < lx < 0.8) or \
             (k == 2 and abs(lx - ly) < 0.12) or (k == 3 and math.hypot(lx - 0.5, ly - 0.5) < 0.35 and math.hypot(lx - 0.5, ly - 0.5) > 0.2)
        return (190, 90, 255, 255) if on and 0.15 < ly < 0.85 else (0, 0, 0, 255)
    save('rune', colored(64, rune))


def main():
    for f in (gold_band, sapphire_ring, skull_ring, bone_hand_ring, emerald_eye_ring, satchel, traveler_pack, expedition_pack,
              heart_tonic, ocean_draught, black_death, starlight_elixir, sun_essence, apprentice_spellbook, traveler_tome,
              black_grimoire, iron_key, dungeon_key, heart_key, mechanic_key, thorn_lockpick, crystal_key, key_ring):
        f()
    goggles('explorer_goggles', hx('#5ab0d8'), hx('#d6f2ff'))
    goggles('engineer_goggles', hx('#1a9a9a'), hx('#9ff0e8'), loupe=True)
    goggles('hunter_goggles', hx('#a01818'), hx('#ff8a6a'), deep=True)
    belt('traveler_belt', hx('#8a7258'), hx('#5a4836'), SILV)
    belt('tool_belt', hx('#7c8048'), hx('#4e5230'), BRASS, pouches=2)
    belt('alchemist_belt', hx('#6a4228'), hx('#3e2616'), BRASS, pouches=2, vials=True)
    materials()
    block_textures()
    print('ok')


if __name__ == '__main__':
    main()
