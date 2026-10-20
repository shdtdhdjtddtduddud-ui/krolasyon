#!/usr/bin/env python3
"""Sylvaren elflerinin dokularını boyar (256x256 = 128x128 model ızgarası, her model pikseli 2x2).

Yerleşim: sol üst 64x64 standart oyuncu derisi düzeni; geri kalanı ElfModel'deki ek parçalar.
Her yüz bir fonksiyonla boyanır: f(u, v, W, H) -> renk; u, v yüzün içindeki konum (0..W, 0..H, yarım piksel adımlarıyla).
"""
import math
import os
import random
import sys

from PIL import Image

S = 2
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets',
                                                         'elvenrealm', 'textures', 'entity')


def hx(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (a[3] if len(a) > 3 else 255,)


def shade(c, f):
    return tuple(max(0, min(255, int(c[i] * f))) for i in range(3)) + (c[3] if len(c) > 3 else 255,)


CLEAR = (0, 0, 0, 0)


class Painter:
    def __init__(self, seed):
        self.img = Image.new('RGBA', (128 * S, 128 * S), CLEAR)
        self.px = self.img.load()
        self.rng = random.Random(seed)

    def noise(self, c, amt=0.06):
        return shade(c, 1 + (self.rng.random() - 0.5) * 2 * amt)

    def face(self, x0, y0, w, h, fn):
        """Bir yüzü (model pikseli cinsinden x0,y0,w,h) boyar. fn(u, v, w, h) yarım piksel çözünürlüğünde çağrılır."""
        for j in range(int(h * S)):
            for i in range(int(w * S)):
                u, v = (i + 0.5) / S, (j + 0.5) / S
                c = fn(u, v, w, h)
                if c is None:
                    continue
                X, Y = int(x0 * S) + i, int(y0 * S) + j
                if 0 <= X < 128 * S and 0 <= Y < 128 * S:
                    self.px[X, Y] = c

    def box(self, u, v, w, h, d, front=None, back=None, right=None, left=None, top=None, bottom=None, sides=None, all=None):
        """Kutu UV açılımı: üst (u+d, v), alt (u+d+w, v), sağ (u, v+d), ön (u+d, v+d), sol (u+d+w, v+d), arka (u+2d+w, v+d)."""
        sides = sides or all
        front, back = front or all, back or all
        right, left = right or sides, left or sides
        top, bottom = top or all, bottom or all
        if top: self.face(u + d, v, w, d, top)
        if bottom: self.face(u + d + w, v, w, d, bottom)
        if right: self.face(u, v + d, d, h, right)
        if front: self.face(u + d, v + d, w, h, front)
        if left: self.face(u + d + w, v + d, d, h, left)
        if back: self.face(u + d + w + d, v + d, w, h, back)

    def save(self, name):
        os.makedirs(OUT, exist_ok=True)
        self.img.save(os.path.join(OUT, name))


# ---------------------------------------------------------------- desen yardımcıları

def flat(p, c, amt=0.05):
    return lambda u, v, w, h: p.noise(c, amt)


def vgrad(p, a, b, amt=0.04):
    return lambda u, v, w, h: p.noise(mix(a, b, v / h), amt)


def hair_fn(p, base, light, dark, strands=1.0):
    """Saç: dikey tel çizgileri, parlak bant ve koyu kökler."""
    def f(u, v, w, h):
        s = math.sin(u * 3.1 * strands + math.sin(v * 0.7) * 1.5) * 0.5 + 0.5
        c = mix(dark, base, 0.45 + s * 0.55)
        if 0.25 < (v / max(h, 1)) < 0.4 and s > 0.6:
            c = mix(c, light, 0.6)
        return p.noise(c, 0.05)
    return f


def curls_fn(p, base, light, dark):
    """Kıvırcık saç: küçük halkalar."""
    def f(u, v, w, h):
        cx, cy = (u % 1.5) - 0.75, (v % 1.5) - 0.75
        r = math.hypot(cx, cy)
        c = mix(base, light, 0.6) if r < 0.35 else mix(base, dark, 0.5) if r > 0.6 else base
        return p.noise(c, 0.06)
    return f


def fur_fn(p, base, light, dark):
    def f(u, v, w, h):
        r = p.rng.random()
        return mix(base, light, r * 0.6) if r > 0.5 else mix(base, dark, (0.5 - r) * 1.4)
    return f


def stripes_fn(p, a, b, period=1.5, vertical=True):
    def f(u, v, w, h):
        t = u if vertical else v
        return p.noise(a if (t % period) < period * 0.5 else b, 0.04)
    return f


def filigree_fn(p, base, metal):
    """Gümüş sarmal işlemeler (Vaelor'un zırhı)."""
    def f(u, v, w, h):
        a = math.sin(u * 2.2 + math.sin(v * 1.3) * 2.0) * math.cos(v * 1.7 - u * 0.6)
        if abs(a) < 0.12:
            return p.noise(metal, 0.08)
        return p.noise(base, 0.05)
    return f


def rune_fn(p, base, ink):
    """Tunik üzerinde küçük rün işaretleri."""
    def f(u, v, w, h):
        gx, gy = int(u // 1.5), int(v // 2)
        lx, ly = u % 1.5, v % 2
        seed = (gx * 73856093 ^ gy * 19349663) & 0xffff
        if seed % 3 == 0 and 0.3 < lx < 1.2 and 0.4 < ly < 1.6:
            pattern = seed % 4
            if (pattern == 0 and abs(lx - 0.75) < 0.2) or (pattern == 1 and abs(ly - 1.0) < 0.2) or \
                    (pattern == 2 and abs((lx - 0.3) - (ly - 0.4) * 0.8) < 0.2) or (pattern == 3 and abs(lx - 0.75) < 0.2 and ly < 1.0):
                return p.noise(ink, 0.05)
        return p.noise(base, 0.03)
    return f


def celtic_band(p, base, gold, at_bottom=2.0):
    """Altta işlemeli bordür (Caelan'ın pelerini)."""
    def f(u, v, w, h):
        if v > h - at_bottom:
            t = (v - (h - at_bottom)) / at_bottom
            k = math.sin(u * 3.0) * 0.35 + 0.5
            if abs(t - k) < 0.18 or abs(t - (1 - k)) < 0.18:
                return p.noise(gold, 0.06)
            return p.noise(shade(base, 0.7), 0.05)
        if v > h - at_bottom - 0.5:
            return p.noise(gold, 0.05)
        fold = math.sin(u * 2.4) * 0.08
        return p.noise(shade(base, 1 + fold), 0.04)
    return f


def leaf_fn(p, base, dark):
    """Yaprak/tüy deseni (Thalion'un pelerin kürkü)."""
    def f(u, v, w, h):
        lx, ly = u % 2.0, v % 2.5
        if abs(lx - 1.0) < 0.15:
            return p.noise(shade(base, 0.7), 0.05)
        return p.noise(mix(base, dark, ly / 2.5 * 0.6), 0.07)
    return f


# ---------------------------------------------------------------- yüz

def face_fn(p, skin, hair, eye, brow=None, lips=None, fringe='side', slim_face=True, beard=None, eye_glow=False, freckles=False):
    """Kafa ön yüzü: saç çizgisi, kaşlar, badem gözler, burun gölgesi, dudak."""
    brow = brow or shade(hair, 0.75)
    lips = lips or mix(skin, hx('#b5545c'), 0.45)
    white = hx('#f4f1ea')

    def f(u, v, w, h):
        # saç çizgisi
        if fringe == 'side':
            line = 1.6 + (u / 8.0) * 1.2 if u < 5.5 else 2.6 + (u - 5.5) * 0.45
            if v < line:
                return hair_fn(p, hair, shade(hair, 1.25), shade(hair, 0.7))(u, v, w, h)
        elif fringe == 'middle':
            line = 1.3 + abs(u - 4) * 0.35
            if v < line or (v < 6.5 and (u < 0.9 or u > 7.1)):
                return hair_fn(p, hair, shade(hair, 1.25), shade(hair, 0.7))(u, v, w, h)
        elif fringe == 'curly':
            line = 2.0 + 0.4 * math.sin(u * 3.5)
            if v < line or (v < 5.5 and (u < 0.8 or u > 7.2)):
                return curls_fn(p, hair, shade(hair, 1.25), shade(hair, 0.7))(u, v, w, h)
        elif fringe == 'short':
            line = 1.5 + 0.5 * math.sin(u * 2.5) ** 2
            if v < line or (v < 4.0 and (u < 0.6 or u > 7.4)):
                return hair_fn(p, hair, shade(hair, 1.2), shade(hair, 0.7), 1.6)(u, v, w, h)
        elif fringe == 'hood':
            pass
        c = skin
        # yanak alt gölgesi ve çene
        if v > 6.6:
            c = shade(skin, 0.93)
        if slim_face and v > 6.9 and (u < 1.0 or u > 7.0):
            c = shade(skin, 0.82)
        # kaşlar
        for ex in (2.3, 5.7):
            if 3.05 < v < 3.45 and abs(u - ex) < 1.05 and not (u > 3.6 and u < 4.4):
                tilt = (u - ex) * (0.18 if ex < 4 else -0.18)
                if 3.05 + tilt < v < 3.5 + tilt:
                    return p.noise(brow, 0.03)
        # gözler (badem)
        for ex in (2.3, 5.7):
            dx, dy = u - ex, v - 4.35
            if (dx / 1.1) ** 2 + (dy / 0.55) ** 2 < 1:
                if abs(dx) < 0.45 and dy > -0.45:
                    iris = eye if not eye_glow else mix(eye, white, 0.25)
                    if abs(dx) < 0.18 and dy < 0.1:
                        return shade(iris, 0.35)
                    return p.noise(iris, 0.05)
                if dy < -0.3:
                    return shade(skin, 0.45)
                return white
            # kirpik çizgisi
            if -0.65 < dy < -0.45 and abs(dx) < 1.05:
                return shade(skin, 0.4)
        # burun
        if 4.6 < v < 5.6 and 3.85 < u < 4.25:
            return shade(skin, 0.88)
        if 5.45 < v < 5.75 and 3.6 < u < 4.4:
            return shade(skin, 0.8)
        # çiller
        if freckles and 4.9 < v < 5.6 and (1.5 < u < 3 or 5 < u < 6.5) and p.rng.random() < 0.15:
            return shade(skin, 0.86)
        # dudak
        if 6.1 < v < 6.55 and 3.2 < u < 4.8:
            return p.noise(lips if v > 6.3 else shade(lips, 0.85), 0.03)
        # sakal
        if beard and v > 6.0 and (u < 3.0 or u > 5.0 or v > 6.6):
            return hair_fn(p, beard, shade(beard, 1.2), shade(beard, 0.7))(u, v, w, h)
        return p.noise(c, 0.025)
    return f


def head(p, skin, hair, eye, fringe, back_hair=True, hat=True, ears_skin=None, **kw):
    hairf = hair_fn(p, hair, shade(hair, 1.25), shade(hair, 0.7)) if fringe != 'curly' else curls_fn(p, hair, shade(hair, 1.25), shade(hair, 0.7))
    if fringe == 'short':
        hairf = hair_fn(p, hair, shade(hair, 1.2), shade(hair, 0.7), 1.6)

    def side(u, v, w, h):
        line = 2.0 if fringe != 'short' else 1.6
        if v < line + (u / 8) * (3 if fringe != 'short' else 1.5) or (back_hair and u > 3.5 and v < 8) or (fringe != 'short' and u > 5):
            return hairf(u, v, w, h)
        if v > 6.8 and u < 2:
            return shade(skin, 0.86)
        return p.noise(skin, 0.025)

    def side_l(u, v, w, h):
        return side(8 - u, v, w, h)

    p.box(0, 0, 8, 8, 8, front=face_fn(p, skin, hair, eye, fringe=fringe, **kw), back=hairf, right=side, left=side_l, top=hairf,
          bottom=flat(p, shade(skin, 0.8)))
    if hat:
        # dış katman: saçın hacmi (önde sadece kenarlar)
        def hat_front(u, v, w, h):
            if fringe == 'curly':
                if v < 1.6 + 0.5 * math.sin(u * 3) or ((u < 0.9 or u > 7.1) and v < 7.5):
                    return curls_fn(p, hair, shade(hair, 1.3), shade(hair, 0.75))(u, v, w, h)
                return None
            if fringe == 'short':
                if v < 1.2 + 0.6 * abs(math.sin(u * 1.7)):
                    return hairf(u, v, w, h)
                return None
            if v < 1.0 or ((u < 0.6 or u > 7.4) and v < 8):
                return hairf(u, v, w, h)
            return None

        def hat_side(u, v, w, h):
            if fringe == 'short':
                return hairf(u, v, w, h) if v < 2.5 or (u > 4 and v < 5) else None
            return hairf(u, v, w, h) if v < 2.5 or u > 2.5 or v > 0 else None

        p.box(32, 0, 8, 8, 8, front=hat_front, back=hairf if fringe != 'short' else (lambda u, v, w, h: hairf(u, v, w, h) if v < 6 else None),
              right=hat_side, left=lambda u, v, w, h: hat_side(8 - u, v, w, h), top=hairf, bottom=None)


# ---------------------------------------------------------------- gövde, kollar, bacaklar

def body(p, front, back=None, sides=None, top=None, layer=None):
    """Gövde (16,16) 8x12x4; layer=(front, back, sides) dış katman (16,32)."""
    p.box(16, 16, 8, 12, 4, front=front, back=back or front, sides=sides or front, top=top or front, bottom=top or front)
    if layer:
        lf, lb, ls = layer
        p.box(16, 32, 8, 12, 4, front=lf, back=lb, sides=ls, top=None, bottom=None)


def arms(p, slim, fn_r, fn_l=None, layer_r=None, layer_l=None, hand=None):
    w = 3 if slim else 4
    fn_l = fn_l or fn_r
    p.box(40, 16, w, 12, 4, all=fn_r, bottom=hand)
    p.box(32, 48, w, 12, 4, all=fn_l, bottom=hand)
    if layer_r:
        p.box(40, 32, w, 12, 4, all=layer_r, top=None, bottom=None)
    if layer_l or layer_r:
        p.box(48, 48, w, 12, 4, all=layer_l or layer_r, top=None, bottom=None)


def legs(p, fn_r, fn_l=None, layer=None):
    fn_l = fn_l or fn_r
    p.box(0, 16, 4, 12, 4, all=fn_r)
    p.box(16, 48, 4, 12, 4, all=fn_l)
    if layer:
        p.box(0, 32, 4, 12, 4, all=layer, top=None, bottom=None)
        p.box(0, 48, 4, 12, 4, all=layer, top=None, bottom=None)


def arm_fn(p, skin, sleeve_to, sleeve, cuff=None, glove=None, bracer=None, bracer_range=(7, 11)):
    """Kol: v<sleeve_to kol kumaşı; ardından ten; el (v>10.5) eldiven; isteğe bağlı kolluk."""
    def f(u, v, w, h):
        if glove and v > 10.4:
            return p.noise(glove, 0.05)
        if bracer and bracer_range[0] < v < bracer_range[1]:
            c = bracer
            if abs(v - bracer_range[0] - 0.4) < 0.25 or abs(v - bracer_range[1] + 0.4) < 0.25:
                c = shade(bracer, 1.3)
            return p.noise(c, 0.05)
        if v < sleeve_to:
            if cuff and v > sleeve_to - 0.8:
                return p.noise(cuff, 0.04)
            if callable(sleeve):
                return sleeve(u, v, w, h)
            return p.noise(sleeve, 0.04)
        return p.noise(skin, 0.025)
    return f


def boot_leg(p, upper, boot, boot_from=7.0, top_band=None, upper_fn=None):
    def f(u, v, w, h):
        if v > boot_from:
            if top_band and v < boot_from + 0.8:
                return p.noise(top_band, 0.04)
            c = boot if v < 11.3 else shade(boot, 0.7)
            return p.noise(c, 0.05)
        if upper_fn:
            return upper_fn(u, v, w, h)
        return p.noise(upper, 0.04)
    return f


# ---------------------------------------------------------------- ek parçalar

def ears(p, skin, jewel=None):
    def ear(u, v, w, h):
        c = skin
        if v > h - 0.5:
            c = shade(skin, 0.85)
        if u > w - 1.2 and v < 0.8:
            c = shade(skin, 1.05)
        if jewel and 0.8 < u < 1.6 and v > 1.0:
            return jewel
        return p.noise(c, 0.03)
    p.box(64, 0, 4, 2, 1, all=ear)
    p.box(76, 0, 2, 1, 1, all=flat(p, skin, 0.03))
    p.box(64, 4, 4, 2, 1, all=ear)
    p.box(76, 4, 2, 1, 1, all=flat(p, skin, 0.03))


def long_hair(p, hair, length_scale=1.0, curly=False, accent=None):
    f = curls_fn(p, hair, shade(hair, 1.25), shade(hair, 0.7)) if curly else hair_fn(p, hair, shade(hair, 1.25), shade(hair, 0.7))

    def ends(u, v, w, h):
        # uçlar sivrilir
        tip = h - 1.8 * (0.5 + 0.5 * abs(math.sin(u * 1.3)))
        if v > tip:
            return None
        if accent and abs(v - 3.0) < 0.35:
            return p.noise(accent, 0.05)
        return f(u, v, w, h)
    p.box(88, 0, 9, 16, 1, all=ends, top=f)
    p.box(110, 0, 2, 8, 1, all=f)
    p.box(118, 0, 2, 8, 1, all=f)


def skirt(p, fn, fn_b=None, fn_side=None):
    p.box(64, 20, 9, 11, 1, all=fn)
    p.box(64, 32, 9, 11, 1, all=fn_b or fn)
    p.box(84, 20, 1, 11, 5, all=fn_side or fn)
    p.box(96, 20, 1, 11, 5, all=fn_side or fn)


def sheath(p, leather, metal, grip):
    def sh(u, v, w, h):
        if v < 0.8 or v > h - 1.2:
            return p.noise(metal, 0.06)
        return p.noise(leather if int(v * 2) % 5 else shade(leather, 0.8), 0.05)
    p.box(108, 20, 1, 12, 2, all=sh)
    p.box(114, 20, 1, 4, 1, all=lambda u, v, w, h: p.noise(grip if v < 3.3 else metal, 0.05))
    p.box(114, 26, 1, 1, 4, all=flat(p, metal, 0.08))


def cloak_part(p, fn, inner=None):
    p.box(64, 44, 10, 22, 1, all=fn, back=fn, front=inner or fn)


# ---------------------------------------------------------------- karakterler

def lirael():
    p = Painter(1)
    skin, hair, eye = hx('#f3d5c0'), hx('#e6c264'), hx('#4f9a4a')
    green, green_d, green_l = hx('#4f8a3a'), hx('#355f27'), hx('#8fbf5a')
    white, corset, corset_d, gold = hx('#f2ede2'), hx('#6b4428'), hx('#4a2d19'), hx('#d4a937')
    head(p, skin, hair, eye, 'side', freckles=True)
    ears(p, skin)

    def torso(u, v, w, h):
        # omuzlar açık (off-shoulder), beyaz bluz bandı, kahverengi bağcıklı korse, yeşil etek başlangıcı
        if v < 1.2:
            return p.noise(skin, 0.025)
        if v < 3.2:
            if v < 1.6:
                return p.noise(shade(white, 0.92), 0.03)
            # büzgü
            return p.noise(white if int(u * 2) % 2 else shade(white, 0.93), 0.02)
        if v < 9.5:
            c = corset
            if abs(u - w / 2) < 0.6:
                # bağcıklar
                if int(v * 2) % 2 == 0 and abs(u - w / 2) < 0.5:
                    return p.noise(hx('#efe2c0'), 0.05)
                return p.noise(shade(white, 0.85), 0.03)
            if u < 0.7 or u > w - 0.7:
                c = corset_d
            if v > 9.0:
                c = corset_d
            return p.noise(c, 0.05)
        if v < 10.3:
            if abs(u - w / 2) < 0.7:
                return p.noise(gold, 0.08)
            return p.noise(hx('#3b2414'), 0.05)
        return p.noise(green if int(u) % 3 else green_d, 0.05)

    def torso_back(u, v, w, h):
        if v < 1.2:
            return p.noise(skin, 0.025)
        if v < 3.2:
            return p.noise(white, 0.03)
        if v < 9.5:
            return p.noise(corset_d if abs(u - w / 2) < 0.4 else corset, 0.05)
        if v < 10.3:
            return p.noise(hx('#3b2414'), 0.05)
        return p.noise(green, 0.05)

    body(p, torso, torso_back, sides=torso_back)
    puffy = stripes_fn(p, green, green_l, 1.2)
    arms(p, True, arm_fn(p, skin, 5.5, puffy, cuff=white), layer_r=lambda u, v, w, h: puffy(u, v, w, h) if v < 4.5 else None,
         hand=flat(p, skin))
    # bacaklar: dizden aşağısı beyaz çizme, kahverengi bağcık
    legs(p, boot_leg(p, hx('#f0e6d8'), hx('#efe9dd'), 6.0, top_band=hx('#7a5230')))
    # etek: katlı yeşil kumaş, altta açık yeşil bordür
    def sk(u, v, w, h):
        fold = 0.85 + 0.15 * math.sin(u * 2.6)
        c = shade(green, fold)
        if v > h - 1.1:
            c = green_l if int(u * 2) % 3 else green_d
        return p.noise(c, 0.04)
    skirt(p, sk)
    long_hair(p, hair, accent=hx('#4f8a3a'))
    sheath(p, hx('#5b3a22'), gold, hx('#3b2414'))
    p.save('elf_lirael.png')


def vaelor():
    p = Painter(2)
    skin, hair, eye = hx('#b4adc0'), hx('#e6e7ec'), hx('#b06adf')
    black, black_l, silver = hx('#1c1c22'), hx('#2b2b35'), hx('#b9bcc6')
    head(p, skin, hair, eye, 'middle', eye_glow=True, brow=hx('#d6d6dd'), lips=hx('#8d7f94'))
    ears(p, skin, jewel=silver)
    armor = filigree_fn(p, black, silver)

    def torso(u, v, w, h):
        if v < 1.0:
            return p.noise(silver, 0.06)
        if 9.6 < v < 10.6:
            return p.noise(silver if abs(u - w / 2) < 0.8 else hx('#3a3a44'), 0.05)
        if abs(u - w / 2) < 0.25 and v < 9.6:
            return p.noise(silver, 0.05)
        return armor(u, v, w, h)

    body(p, torso, layer=(lambda u, v, w, h: p.noise(silver, 0.05) if v < 0.8 else None, None, None))
    arms(p, False, arm_fn(p, skin, 10.4, armor, glove=black_l, bracer=hx('#2f2f39'), bracer_range=(6.5, 10.4)), hand=flat(p, black_l))
    legs(p, boot_leg(p, black, hx('#18181d'), 6.5, top_band=silver, upper_fn=filigree_fn(p, hx('#202027'), shade(silver, 0.85))))
    # omuzluklar: siyah plaka, gümüş kenar ve sarmal
    def pauldron(u, v, w, h):
        if v > h - 0.7 or u < 0.4 or u > w - 0.4:
            return p.noise(silver, 0.06)
        return filigree_fn(p, black, silver)(u, v, w, h)
    p.box(0, 64, 6, 4, 6, all=pauldron)
    p.box(0, 74, 6, 4, 6, all=pauldron)
    # arka zırh etekliği
    p.box(24, 64, 8, 10, 1, all=lambda u, v, w, h: p.noise(silver, 0.05) if v > h - 0.7 else armor(u, v, w, h))
    # kese
    p.box(42, 64, 3, 3, 2, all=lambda u, v, w, h: p.noise(hx('#3b2a1e') if v > 0.8 else silver, 0.06))
    long_hair(p, hair)
    p.save('elf_vaelor.png')


def caelan():
    p = Painter(3)
    skin, hair, eye = hx('#f1d3bd'), hx('#d9b45a'), hx('#4a86c8')
    tunic, ink, gold = hx('#ece8dc'), hx('#5c8f8a'), hx('#c9a03f')
    cloak_c, fur = hx('#3f6b3a'), hx('#9a7c5c')
    head(p, skin, hair, eye, 'curly')
    ears(p, skin)
    tun = rune_fn(p, tunic, ink)

    def torso(u, v, w, h):
        if v < 1.0:
            return p.noise(fur, 0.12)
        if 9.6 < v < 10.6:
            return p.noise(hx('#6b4a2a') if abs(u - w / 2) > 0.6 else gold, 0.05)
        # yaka açıklığı
        if v < 3.5 and abs(u - w / 2) < (3.5 - v) * 0.35:
            return p.noise(skin, 0.03)
        return tun(u, v, w, h)

    body(p, torso)
    arms(p, False, arm_fn(p, skin, 10.4, tun, cuff=gold), hand=flat(p, skin))
    legs(p, boot_leg(p, hx('#5a4a3a'), hx('#6b4a2a'), 6.5, top_band=hx('#4a3220')))
    # tunik eteği (diz boyu), rünlü ve altın kenarlı
    def sk(u, v, w, h):
        if v > h - 0.8:
            return p.noise(gold, 0.06)
        return tun(u, v, w, h)
    skirt(p, sk)
    # pelerin: yeşil, kenarı işlemeli; iç yüzü koyu
    cloak_part(p, celtic_band(p, cloak_c, gold, 2.5), inner=flat(p, shade(cloak_c, 0.7)))
    # kürk yaka
    p.box(86, 44, 10, 3, 6, all=fur_fn(p, fur, hx('#c8b08a'), hx('#5e4630')))
    # hançer (kında)
    p.box(90, 54, 1, 7, 2, all=lambda u, v, w, h: p.noise(hx('#d0d3da') if v < 2 else hx('#4a3220') if v < 6.5 else gold, 0.05))
    long_hair(p, hair, curly=True)
    p.save('elf_caelan.png')


def nimrae():
    p = Painter(4)
    skin, hair, eye = hx('#6e4a36'), hx('#f2f0ea'), hx('#e3b04a')
    wrap, gold, ruby = hx('#efeae0'), hx('#d9ae3a'), hx('#c0262f')
    head(p, skin, hair, eye, 'middle', brow=hx('#e8e4da'), lips=hx('#7a3f3a'))
    ears(p, skin, jewel=gold)

    def wraps(u, v, w, h):
        # çapraz sargı izleri
        band = (u * 0.7 + v) % 1.6
        c = wrap if band > 0.25 else shade(wrap, 0.88)
        return p.noise(c, 0.03)

    def torso(u, v, w, h):
        # boyun takısı, göğüs sargısı, açık bel, altın kemer ve yakut
        if v < 0.9:
            return p.noise(gold if 0.3 < v else skin, 0.06)
        if v < 5.0:
            if v < 1.6 and abs(u - w / 2) < 1.6:
                return p.noise(gold, 0.08)
            return wraps(u, v, w, h)
        if v < 9.0:
            return p.noise(skin, 0.025)
        if v < 10.4:
            if abs(u - w / 2) < 0.8 and 9.3 < v < 10.1:
                return p.noise(ruby, 0.1)
            return p.noise(gold if int(u * 2) % 3 else shade(gold, 0.8), 0.06)
        return wraps(u, v, w, h)

    body(p, torso)
    # kollar: çıplak, üst kolda ve bilekte altın bilezik
    def arm(u, v, w, h):
        if 2.0 < v < 2.9 or 8.5 < v < 9.6:
            return p.noise(gold, 0.08)
        return p.noise(skin, 0.025)
    arms(p, True, arm, hand=flat(p, skin))
    # bacaklar: uzun beyaz etek altında, ayakta altın sandalet bağları
    def leg(u, v, w, h):
        if v > 9.5:
            return p.noise(gold if int(v * 2) % 2 else skin, 0.05)
        return wraps(u, v, w, h)
    legs(p, leg)
    def sk(u, v, w, h):
        if v > h - 0.7:
            return p.noise(gold, 0.06)
        fold = 0.9 + 0.1 * math.sin(u * 3.1)
        return p.noise(shade(wrap, fold), 0.03)
    skirt(p, sk)
    long_hair(p, hair, accent=gold)
    p.save('elf_nimrae.png')


def tavi():
    p = Painter(5)
    skin, hair, eye = hx('#f2d6c4'), hx('#b8bcc2'), hx('#6a8f4a')
    shirt, vest, cloak_c, leather = hx('#e4dccb'), hx('#6b5236'), hx('#4c7a3e'), hx('#7b5532')
    head(p, skin, hair, eye, 'short', back_hair=False, freckles=True)
    ears(p, skin)

    def torso(u, v, w, h):
        if v < 1.0:
            return p.noise(cloak_c, 0.05)
        if 9.6 < v < 10.6:
            return p.noise(hx('#4a3220') if abs(u - 2.0) > 0.4 else hx('#c8a050'), 0.05)
        # yelek: önü açık, içinden gömlek
        if abs(u - w / 2) < 1.2 and v < 9.6:
            return p.noise(shirt, 0.03)
        # sadak kayışı (çapraz)
        if abs((u - 1) - v * 0.55) < 0.45:
            return p.noise(hx('#4a3220'), 0.05)
        return p.noise(vest, 0.05)

    body(p, torso)
    arms(p, True, arm_fn(p, skin, 10.0, shirt, bracer=leather, bracer_range=(6.5, 10.4)), hand=flat(p, skin))
    legs(p, boot_leg(p, hx('#5b4a36'), hx('#6b4a2a'), 7.0, top_band=hx('#8a6a46')))
    # kısa pelerin
    p.box(42, 70, 9, 13, 1, all=lambda u, v, w, h: p.noise(shade(cloak_c, 0.9 + 0.1 * math.sin(u * 2.7)) if v < h - 0.6
                                                         else shade(cloak_c, 0.7), 0.04))
    # sadak + oklar
    p.box(0, 84, 3, 12, 3, all=lambda u, v, w, h: p.noise(leather if int(v) % 4 else hx('#c8a050'), 0.06))
    p.box(12, 84, 3, 3, 2, all=lambda u, v, w, h: p.noise(hx('#f2f0ea') if int(u * 2) % 2 else hx('#c0392b'), 0.05))
    # kuş: kahverengi serçe, krem göğüs, sarı gaga
    bird_b, bird_c = hx('#8a6a3a'), hx('#ecd9a8')
    p.box(22, 84, 2, 2, 3, all=lambda u, v, w, h: p.noise(bird_b, 0.08), front=flat(p, bird_c), bottom=flat(p, bird_c))

    def bird_head(u, v, w, h):
        if 0.6 < v < 1.2 and (u < 0.6 or u > 1.4):
            return hx('#111111')
        return p.noise(bird_b if v < 1 else bird_c, 0.06)
    p.box(32, 84, 2, 2, 2, all=lambda u, v, w, h: p.noise(bird_b, 0.06), front=bird_head, right=bird_head, left=bird_head)
    p.box(40, 84, 1, 1, 1, all=flat(p, hx('#e8b33a')))
    wing = lambda u, v, w, h: p.noise(hx('#6b4f2a') if int(v * 2 + u) % 2 else hx('#9a7a48'), 0.06)
    p.box(22, 90, 1, 2, 3, all=wing)
    p.box(30, 90, 1, 2, 3, all=wing)
    p.box(44, 84, 2, 1, 3, all=flat(p, hx('#5a4224'), 0.08))
    p.save('elf_tavi.png')


def thalion():
    p = Painter(6)
    skin, hair, eye = hx('#e2bfa5'), hx('#5a3e28'), hx('#5d7a52')
    hood, tunic, leather, fur = hx('#3d5f33'), hx('#2f4a2a'), hx('#5a3e26'), hx('#6b5034')
    head(p, skin, hair, eye, 'side', back_hair=False, beard=hx('#5a3e28'), hat=False)
    ears(p, skin)

    def torso(u, v, w, h):
        if v < 1.0:
            return p.noise(fur, 0.1)
        if 9.6 < v < 10.6:
            return p.noise(hx('#3a2616') if abs(u - w / 2) > 0.6 else hx('#a88a50'), 0.05)
        # deri zırh plakaları
        if int(v / 2.5) % 2 == 0 and (u < 3.5 or u > 4.5):
            return p.noise(leather, 0.06)
        if abs((w - u - 1) - v * 0.55) < 0.45:
            return p.noise(hx('#3a2616'), 0.05)
        return p.noise(tunic, 0.05)

    body(p, torso)
    arms(p, False, arm_fn(p, skin, 10.4, tunic, glove=hx('#3a2616'), bracer=leather, bracer_range=(6.0, 10.4)), hand=flat(p, hx('#3a2616')))
    legs(p, boot_leg(p, hx('#3f3324'), hx('#4a3220'), 6.0, top_band=hx('#6b5034')))
    leafy = leaf_fn(p, fur, hx('#3e2c1c'))
    cloak_part(p, lambda u, v, w, h: leafy(u, v, w, h) if v > 2 else p.noise(hood, 0.05), inner=flat(p, shade(hood, 0.7)))
    # kapüşon panelleri
    hoodf = lambda u, v, w, h: p.noise(shade(hood, 0.92 + 0.08 * math.sin(v * 2 + u)), 0.05)
    p.box(64, 68, 9, 1, 9, all=hoodf)
    p.box(100, 68, 1, 9, 9, all=hoodf)
    p.box(64, 78, 1, 9, 9, all=hoodf)
    p.box(64, 96, 9, 9, 1, all=hoodf)
    p.box(84, 96, 9, 2, 1, all=lambda u, v, w, h: p.noise(shade(hood, 0.8), 0.05))
    p.box(84, 100, 4, 5, 2, all=hoodf)
    # tabar
    p.box(0, 100, 5, 9, 1, all=lambda u, v, w, h: p.noise(tunic if v < h - 0.7 else hx('#a88a50'), 0.05))
    p.box(12, 100, 5, 9, 1, all=lambda u, v, w, h: p.noise(tunic, 0.05))
    # tüylü omuz örtüsü
    def feathers(u, v, w, h):
        lx = u % 1.5
        c = mix(hx('#7a5a3a'), hx('#d8cbb0'), v / max(h, 1))
        if abs(lx - 0.75) < 0.12:
            return shade(c, 0.6)
        return p.noise(c, 0.1)
    p.box(24, 100, 11, 4, 7, all=feathers)
    p.box(0, 84, 3, 12, 3, all=lambda u, v, w, h: p.noise(leather if int(v) % 4 else hx('#a88a50'), 0.06))
    p.box(12, 84, 3, 3, 2, all=lambda u, v, w, h: p.noise(hx('#e8e1cf') if int(u * 2) % 2 else hx('#3d5f33'), 0.05))
    p.save('elf_thalion.png')


def guard():
    p = Painter(7)
    skin, hair, eye = hx('#efd1bb'), hx('#cfc6a8'), hx('#5d7aa0')
    steel, steel_d, green, gold = hx('#b7bcc4'), hx('#7d838c'), hx('#2e5a33'), hx('#d9b45a')
    head(p, skin, hair, eye, 'middle')
    ears(p, skin)

    def chain(u, v, w, h):
        c = steel if (int(u * 2) + int(v * 2)) % 2 else steel_d
        return p.noise(c, 0.06)

    def torso(u, v, w, h):
        if v < 1.0:
            return chain(u, v, w, h)
        if 9.6 < v < 10.6:
            return p.noise(hx('#3b2414') if abs(u - w / 2) > 0.6 else gold, 0.05)
        if 1.5 < u < w - 1.5:
            # tabar üzerinde altın ağaç arması
            cx, cy = w / 2, 5.0
            if abs(u - cx) < 0.3 and 3.5 < v < 8.0:
                return p.noise(gold, 0.06)
            if (u - cx) ** 2 / 4 + (v - 3.8) ** 2 < 1.4:
                return p.noise(gold, 0.06)
            if 7.7 < v < 8.2 and abs(u - cx) < 1.4:
                return p.noise(gold, 0.06)
            return p.noise(green, 0.05)
        return chain(u, v, w, h)

    body(p, torso)
    arms(p, False, arm_fn(p, skin, 10.4, chain, glove=hx('#4a3220'), bracer=steel, bracer_range=(7.0, 10.4)), hand=flat(p, hx('#4a3220')))
    legs(p, boot_leg(p, hx('#3f4a3a'), hx('#4a3220'), 6.5, top_band=steel))
    p.box(0, 100, 5, 9, 1, all=lambda u, v, w, h: p.noise(green if v < h - 0.7 else gold, 0.05))
    p.box(12, 100, 5, 9, 1, all=lambda u, v, w, h: p.noise(green, 0.05))
    long_hair(p, hair)
    p.save('elf_guard.png')


if __name__ == '__main__':
    for f in (lirael, vaelor, caelan, nimrae, tavi, thalion, guard):
        f()
    print('ok', OUT)
