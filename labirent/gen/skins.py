"""64x64 player-format skins for the Glade's characters, painted from their costume descriptions."""
import os
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/labirent/textures/entity/glader')
os.makedirs(OUT, exist_ok=True)


def rng(s):
    return np.random.default_rng(s)


class Skin:
    def __init__(self, seed, slim=False):
        self.img = np.zeros((64, 64, 4), np.float32)
        self.r = rng(seed)
        self.slim = slim
        self.aw = 3 if slim else 4

    # ---------------------------------------------------------------- regions (x, y, w, h) per face
    def box(self, u, v, w, h, d):
        return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
                'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}

    def head(self, layer=False): return self.box(32 if layer else 0, 0, 8, 8, 8)

    def body(self, layer=False): return self.box(16, 32 if layer else 16, 8, 12, 4)

    def rarm(self, layer=False): return self.box(40, 32 if layer else 16, self.aw, 12, 4)

    def larm(self, layer=False): return self.box(48 if layer else 32, 48, self.aw, 12, 4)

    def rleg(self, layer=False): return self.box(0, 32 if layer else 16, 4, 12, 4)

    def lleg(self, layer=False): return self.box(0 if layer else 16, 48, 4, 12, 4)

    def fill(self, rect, col, var=10, alpha=1.0):
        x, y, w, h = rect
        c = np.array(col, np.float32)
        noise = self.r.normal(0, var * 0.7, (h, w, 1))
        self.img[y:y + h, x:x + w, :3] = np.clip(c + noise, 0, 255)
        self.img[y:y + h, x:x + w, 3] = alpha * 255

    def fill_part(self, part, col, var=10, faces=None):
        for f, rect in part.items():
            if faces and f not in faces:
                continue
            self.fill(rect, col, var)

    def px(self, x, y, col, a=255):
        self.img[y, x, :3] = col
        self.img[y, x, 3] = a

    def rowcol(self, rect, rows, col, var=6):
        x, y, w, h = rect
        for ry in rows:
            for xx in range(w):
                self.px(x + xx, y + ry, np.clip(np.array(col) + self.r.normal(0, var, 3), 0, 255))

    def shade(self, part, bottom_dark=0.82):
        """darker towards the bottom of side faces, slight edge darkening"""
        for f, (x, y, w, h) in part.items():
            if f in ('top', 'bottom'):
                continue
            for yy in range(h):
                k = 1.0 - (1 - bottom_dark) * (yy / max(1, h - 1))
                self.img[y + yy, x:x + w, :3] *= k

    def dirt(self, part, amount=0.06, col=(90, 70, 50)):
        for f, (x, y, w, h) in part.items():
            m = self.r.random((h, w)) < amount
            reg = self.img[y:y + h, x:x + w]
            reg[m, :3] = reg[m, :3] * 0.6 + np.array(col) * 0.4

    def save(self, name):
        Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8), 'RGBA').save(os.path.join(OUT, f'{name}.png'))


def face(s, skin, hair, eyes, brows=None, hair_rows=2, mouth=None, beard=None, sides_hair=1):
    """paint the standard head: skin everywhere, hair on top/back, a simple face on the front"""
    h = s.head()
    s.fill_part(h, skin, 6)
    s.fill(h['top'], hair, 12)
    s.fill(h['back'], hair, 12)
    x, y, w, hh = h['front']
    for yy in range(hair_rows):
        for xx in range(8):
            s.px(x + xx, y + yy, np.clip(np.array(hair) + s.r.normal(0, 12, 3), 0, 255))
    for side in ('left', 'right'):
        sx, sy, sw, sh = h[side]
        for yy in range(hair_rows + sides_hair):
            for xx in range(8):
                s.px(sx + xx, sy + yy, np.clip(np.array(hair) + s.r.normal(0, 12, 3), 0, 255))
    # eyes on row 4: white + iris, iris on the inner side
    ey = y + 4
    for ex, inner in ((x + 1, 1), (x + 5, 0)):
        s.px(ex, ey, (236, 236, 232))
        s.px(ex + 1, ey, (236, 236, 232))
        s.px(ex + inner, ey, eyes)
    dark = tuple(int(c * 0.72) for c in skin)
    s.px(x + 3, y + 5, dark)
    s.px(x + 4, y + 5, dark)
    if brows:
        for bx in (x + 1, x + 2, x + 5, x + 6):
            s.px(bx, ey - 1, brows)
    m = mouth or tuple(int(c * 0.62) for c in skin)
    for mx in (x + 3, x + 4):
        s.px(mx, y + 6, m)
    if beard:
        for bx in range(8):
            if s.r.random() < 0.5:
                s.px(x + bx, y + 7, beard)


def arms_with_sleeves(s, skin, shirt, sleeve_rows, cuff=None):
    for part in (s.rarm(), s.larm()):
        s.fill_part(part, skin, 6)
        for f, (x, y, w, h) in part.items():
            if f == 'top':
                s.fill((x, y, w, h), shirt, 8)
                continue
            if f == 'bottom':
                continue
            for yy in range(min(h, sleeve_rows)):
                for xx in range(w):
                    s.px(x + xx, y + yy, np.clip(np.array(shirt) + s.r.normal(0, 8, 3), 0, 255))
            if cuff and sleeve_rows < h:
                for xx in range(w):
                    s.px(x + xx, y + sleeve_rows, cuff)


def legs(s, pants, shoes, belt=None):
    for part in (s.rleg(), s.lleg()):
        s.fill_part(part, pants, 9)
        for f, (x, y, w, h) in part.items():
            if f in ('top', 'bottom'):
                continue
            for yy in range(h - 2, h):
                for xx in range(w):
                    s.px(x + xx, y + yy, np.clip(np.array(shoes) + s.r.normal(0, 6, 3), 0, 255))
        s.shade(part, 0.85)
        s.dirt(part, 0.08)


def belt_on_body(s, col, buckle):
    b = s.body()
    for f in ('front', 'back', 'left', 'right'):
        s.rowcol(b[f], [10, 11], col)
    x, y, w, h = b['front']
    s.px(x + 3, y + 10, buckle)
    s.px(x + 4, y + 10, buckle)
    s.px(x + 3, y + 11, buckle)
    s.px(x + 4, y + 11, tuple(int(c * 0.7) for c in buckle))


# ---------------------------------------------------------------- Alby: shaved head, beige henley, leather belt, pendant
def alby():
    s = Skin(1)
    skin, shirt = (96, 62, 44), (204, 192, 166)
    face(s, skin, (34, 26, 24), (40, 26, 20), brows=(30, 22, 20), hair_rows=1, sides_hair=0)
    b = s.body()
    s.fill_part(b, shirt, 9)
    x, y, w, h = b['front']
    for yy in range(0, 6):                       # button placket
        s.px(x + 3, y + yy, (182, 170, 144))
    for yy in (1, 3, 5):
        s.px(x + 4, y + yy, (232, 226, 210))
    for k, (px, py) in enumerate(((1, 0), (2, 1), (2, 2), (3, 3), (4, 3), (5, 2), (5, 1), (6, 0))):   # necklace cord
        s.px(x + px, y + py, (28, 24, 22))
    s.px(x + 3, y + 4, (150, 156, 160))           # pendant
    s.px(x + 4, y + 4, (120, 126, 130))
    s.shade(b, 0.86)
    s.dirt(b, 0.07)
    belt_on_body(s, (112, 62, 36), (176, 176, 170))
    arms_with_sleeves(s, skin, shirt, 9)
    lx, ly, lw, lh = s.larm()['front']
    for yy in range(9, 12):                      # leather wrist wrap
        for xx in range(lw):
            s.px(lx + xx, ly + yy, (120, 74, 40))
    legs(s, (98, 108, 122), (58, 44, 34))
    s.save('alby')


# ---------------------------------------------------------------- Chuck: curly brown hair, rust vest over a beige tee, satchel strap
def chuck():
    s = Skin(2)
    skin = (236, 196, 166)
    hair = (112, 66, 38)
    face(s, skin, hair, (96, 64, 40), brows=(92, 56, 32), hair_rows=2, sides_hair=2, mouth=(188, 120, 110))
    # curls on the hat layer
    hl = s.head(True)
    for f in ('top', 'back', 'left', 'right', 'front'):
        x, y, w, h = hl[f]
        rows = h if f in ('top', 'back') else (4 if f != 'front' else 2)
        for yy in range(rows):
            for xx in range(w):
                if s.r.random() < (0.85 if yy < 2 else 0.6):
                    c = np.array(hair) * (0.8 + 0.4 * s.r.random())
                    s.px(x + xx, y + yy, np.clip(c, 0, 255))
    tee, vest = (206, 188, 164), (162, 84, 46)
    b = s.body()
    s.fill_part(b, vest, 10)
    x, y, w, h = b['front']
    for yy in range(0, 3):
        for xx in range(3, 5):
            s.px(x + xx, y + yy, tee)
    s.px(x + 3, y + 3, (120, 60, 32))
    for k in range(12):                          # satchel strap: shoulder to hip
        sx = x + 1 + int(k * 6 / 11)
        s.px(sx, y + k, (52, 36, 26))
    s.shade(b, 0.8)
    s.dirt(b, 0.05)
    for part in (s.rarm(), s.larm()):
        s.fill_part(part, skin, 6)
        for f, (ax, ay, aw, ah) in part.items():
            if f in ('top',):
                s.fill((ax, ay, aw, ah), tee, 6)
            elif f != 'bottom':
                for yy in range(4):
                    for xx in range(aw):
                        s.px(ax + xx, ay + yy, np.clip(np.array(tee) + s.r.normal(0, 6, 3), 0, 255))
    legs(s, (142, 112, 82), (70, 52, 38))
    s.save('chuck')


# ---------------------------------------------------------------- Minho: black spiky hair, chambray shirt, leather harness, wrist guards
def minho():
    s = Skin(3)
    skin, shirt, leather = (214, 172, 132), (142, 176, 190), (106, 70, 42)
    face(s, skin, (24, 22, 24), (30, 22, 20), brows=(24, 20, 20), hair_rows=2, sides_hair=1)
    hl = s.head(True)
    x, y, w, h = hl['top']
    for yy in range(h):
        for xx in range(w):
            if s.r.random() < 0.45:
                s.px(x + xx, y + yy, (30, 28, 30))
    fx, fy, fw, fh = hl['front']
    for xx in range(fw):
        if s.r.random() < 0.5:
            s.px(fx + xx, fy, (28, 26, 28))
    b = s.body()
    s.fill_part(b, shirt, 9)
    x, y, w, h = b['front']
    for yy in range(12):
        s.px(x + 3, y + yy, (120, 154, 168))     # button line
    for px_ in (1, 6):                           # shoulder straps
        for yy in range(0, 8):
            s.px(x + px_, y + yy, leather)
    for xx in range(1, 7):                       # chest strap with buckle
        s.px(x + xx, y + 4, leather)
    s.px(x + 3, y + 4, (150, 160, 162))
    s.px(x + 4, y + 4, (150, 160, 162))
    bx, by, bw, bh = b['back']
    for px_ in (1, 6):
        for yy in range(0, 9):
            s.px(bx + px_, by + yy, leather)
    s.shade(b, 0.84)
    s.dirt(b, 0.06)
    belt_on_body(s, (122, 84, 50), (160, 160, 156))
    arms_with_sleeves(s, skin, shirt, 5, cuff=(118, 150, 162))
    for part in (s.rarm(), s.larm()):
        for f, (ax, ay, aw, ah) in part.items():
            if f in ('top', 'bottom'):
                continue
            for yy in range(8, 11):
                for xx in range(aw):
                    s.px(ax + xx, ay + yy, (86, 112, 120) if yy == 8 else (112, 74, 44))
            for xx in range(aw):                 # scratches on the forearm
                if s.r.random() < 0.3:
                    s.px(ax + xx, ay + 6, (170, 70, 60))
    legs(s, (72, 70, 56), (52, 40, 30))
    s.save('minho')


# ---------------------------------------------------------------- Newt: tousled dirty-blond hair, worn cream shirt, leather strap
def newt():
    s = Skin(4)
    skin, hair = (238, 204, 176), (180, 144, 86)
    face(s, skin, hair, (100, 74, 50), brows=(150, 112, 64), hair_rows=2, sides_hair=2)
    hl = s.head(True)
    for f in ('top', 'front', 'left', 'right', 'back'):
        x, y, w, h = hl[f]
        rows = h if f in ('top', 'back') else (3 if f != 'front' else 2)
        for yy in range(rows):
            for xx in range(w):
                if s.r.random() < (0.75 if f != 'front' else 0.55):
                    c = np.array(hair) * (0.82 + 0.38 * s.r.random())
                    s.px(x + xx, y + yy, np.clip(c, 0, 255))
    shirt, strap = (216, 202, 172), (78, 54, 36)
    b = s.body()
    s.fill_part(b, shirt, 10)
    x, y, w, h = b['front']
    for k in range(12):                          # strap over the left shoulder
        s.px(x + 6 - int(k * 5 / 11), y + k, strap)
        if k % 4 == 0:
            s.px(x + 6 - int(k * 5 / 11) + 1, y + k, strap)
    s.px(x + 4, y + 6, (170, 170, 164))           # buckle
    for (tx, ty) in ((6, 7), (6, 8), (7, 8)):     # tear
        s.px(x + tx, y + ty, skin)
    s.shade(b, 0.82)
    s.dirt(b, 0.12, (110, 92, 66))
    arms_with_sleeves(s, skin, shirt, 9)
    rx, ry, rw, rh = s.rarm()['front']
    for xx in range(rw):
        s.px(rx + xx, ry + 10, (150, 120, 80))   # rope bracelet
    legs(s, (110, 98, 80), (62, 48, 36))
    s.save('newt')


# ---------------------------------------------------------------- Gally: buzzed dark hair, cold blue eyes, dark tee under a black tactical vest
def gally():
    s = Skin(5)
    skin = (226, 180, 156)
    face(s, skin, (58, 44, 36), (120, 160, 190), brows=(54, 40, 32), hair_rows=1, sides_hair=1, mouth=(170, 112, 104))
    tee, vest = (62, 68, 64), (34, 35, 38)
    b = s.body()
    s.fill_part(b, vest, 6)
    x, y, w, h = b['front']
    for yy in range(0, 2):
        for xx in range(3, 5):
            s.px(x + xx, y + yy, tee)
    for (px_, py_) in ((1, 4), (2, 4), (5, 4), (6, 4), (1, 7), (2, 7), (5, 7), (6, 7)):   # pouches
        s.px(x + px_, y + py_, (56, 58, 60))
    for yy in (3, 6, 9):
        for xx in range(8):
            if s.r.random() < 0.6:
                s.px(x + xx, y + yy, (22, 22, 24))
    s.px(x + 3, y + 3, (120, 120, 116))
    s.px(x + 4, y + 3, (120, 120, 116))
    s.shade(b, 0.85)
    arms_with_sleeves(s, skin, tee, 5)
    legs(s, (56, 54, 50), (36, 32, 28))
    belt_on_body(s, (28, 28, 30), (110, 110, 106))
    s.save('gally')


# ---------------------------------------------------------------- Teresa (slim): long dark hair, pale grey top
def teresa():
    s = Skin(6, slim=True)
    skin, hair = (236, 204, 182), (54, 36, 28)
    face(s, skin, hair, (60, 80, 110), brows=(46, 30, 24), hair_rows=2, sides_hair=5, mouth=(196, 120, 120))
    hl = s.head(True)
    x, y, w, h = hl['back']
    for yy in range(h):
        for xx in range(w):
            s.px(x + xx, y + yy, np.clip(np.array(hair) * (0.85 + 0.3 * s.r.random()), 0, 255))
    jb = s.body(True)
    x, y, w, h = jb['back']
    for yy in range(5):
        for xx in range(w):
            s.px(x + xx, y + yy, np.clip(np.array(hair) * (0.85 + 0.3 * s.r.random()), 0, 255))
    b = s.body()
    s.fill_part(b, (196, 198, 200), 8)
    s.shade(b, 0.85)
    s.dirt(b, 0.05)
    arms_with_sleeves(s, skin, (196, 198, 200), 2)
    legs(s, (150, 132, 100), (70, 56, 44))
    s.save('teresa')


for fn in (alby, chuck, minho, newt, gally, teresa):
    fn()
print('skins written')
