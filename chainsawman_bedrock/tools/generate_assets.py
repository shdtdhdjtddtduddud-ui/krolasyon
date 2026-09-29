#!/usr/bin/env python3
"""Generates the Chainsaw Man model, 128x128 skin texture, item icons, particle
texture, pack icons and synthesized WAV sounds for the Bedrock add-on.

Usage: python3 tools/generate_assets.py   (needs pillow + numpy)
"""
import json
import math
import random
import wave
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RP = ROOT / "ChainsawMan_RP"
BP = ROOT / "ChainsawMan_BP"

TW = TH = 128
img = Image.new("RGBA", (TW, TH), (0, 0, 0, 0))
px = img.load()
rng = random.Random(1337)


# ----------------------------------------------------------------- palette
def c(r, g, b, a=255):
    return (r, g, b, a)


SHIRT = c(238, 236, 224)
SHIRT_S = c(214, 210, 196)
SHIRT_D = c(186, 181, 167)
SKIN = c(240, 202, 168)
SKIN_S = c(216, 170, 138)
BLOOD = c(172, 18, 24)
BLOOD_D = c(104, 8, 14)
PANTS = c(46, 54, 58)
PANTS_S = c(33, 39, 43)
PANTS_L = c(64, 74, 79)
BELT = c(94, 54, 38)
BUCKLE = c(180, 182, 188)
TIE = c(22, 22, 27)
TIE_L = c(48, 48, 58)
WHITE = c(240, 240, 242)
WHITE_S = c(202, 202, 208)
SHOE_RED = c(180, 34, 38)
LACE = c(150, 150, 160)
METAL = c(152, 157, 164)
METAL_L = c(198, 202, 208)
METAL_D = c(88, 92, 100)
METAL_K = c(56, 58, 64)
ORANGE = c(208, 76, 34)
ORANGE_L = c(236, 108, 52)
ORANGE_D = c(150, 46, 22)
BLACK = c(18, 16, 21)
BLACK_L = c(40, 38, 46)
TEETH = c(236, 230, 212)
TEETH_S = c(196, 188, 170)
EYE = c(250, 236, 176)


def shade(col, amt):
    return (
        max(0, min(255, col[0] + amt)),
        max(0, min(255, col[1] + amt)),
        max(0, min(255, col[2] + amt)),
        col[3],
    )


def noisy(col, n=5):
    return shade(col, rng.randint(-n, n))


# ------------------------------------------------------------- UV allocator
class Atlas:
    def __init__(self):
        self.x = 0
        self.y = 0
        self.row_h = 0

    def alloc(self, w, h, d):
        fw, fh = 2 * (w + d), d + h
        if self.x + fw > TW:
            self.x = 0
            self.y += self.row_h
            self.row_h = 0
        if self.y + fh > TH:
            raise RuntimeError("atlas full")
        uv = (self.x, self.y)
        self.x += fw
        self.row_h = max(self.row_h, fh)
        return uv


atlas = Atlas()


def face_rects(uv, size):
    u, v = uv
    w, h, d = size
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def paint_box(uv, size, fn):
    """fn(face, x, y, w, h) -> rgba ; x/y local to the face (y=0 is top)."""
    for face, (fx, fy, fw, fh) in face_rects(uv, size).items():
        for y in range(fh):
            for x in range(fw):
                px[fx + x, fy + y] = fn(face, x, y, fw, fh)


# ------------------------------------------------------------- painters
def p_solid(col, n=3):
    return lambda f, x, y, w, h: noisy(col, n)


def p_head(f, x, y, w, h):
    # chainsaw mask: orange crown, black lower face, jagged white grin
    if f == "front":
        if y <= 2:
            return noisy(ORANGE_L if y == 0 else ORANGE, 6)
        if y == 3:
            if x in (1, 2, 5, 6):
                return EYE
            return noisy(BLACK, 2)
        if y == 4:
            if x in (1, 2, 5, 6):
                return noisy(ORANGE_D, 4)
            return noisy(BLACK_L, 2)
        if y >= 5:
            # teeth rows: top row big triangles, then interlocking bottom row
            if y == 5:
                return TEETH if x % 2 == 0 or x in (0, 7) else noisy(BLACK, 2)
            if y == 6:
                return TEETH_S if x % 2 == 1 else noisy(BLACK_L, 2)
            return TEETH if x % 2 == 0 else noisy(BLACK, 2)
    if f in ("left", "right"):
        if y <= 3:
            return noisy(ORANGE_D if y == 3 else ORANGE, 6)
        if y == 4 and x < 2:
            return noisy(ORANGE_D, 4)
        return noisy(BLACK, 3)
    if f == "back":
        return noisy(ORANGE if y <= 5 else BLACK, 6)
    if f == "top":
        return noisy(ORANGE_L if 1 < y < 6 else ORANGE, 6)
    return noisy(BLACK, 3)


def p_snout(f, x, y, w, h):
    if f == "front":
        return TEETH if x % 2 == 0 else noisy(BLACK, 2)
    if f in ("top", "bottom"):
        return noisy(BLACK_L, 3)
    return noisy(BLACK if y else ORANGE_D, 3)


def p_body(f, x, y, w, h):
    base = SHIRT
    if f == "front":
        # placket + folds + chest pocket (viewer's right)
        if x == 4 and y > 1:
            return noisy(SHIRT_S, 2)
        if x in (1, 6) and y > 2:
            return noisy(SHIRT_S, 3)
        if 5 <= x <= 7 and y in (3, 6):
            return SHIRT_D
        if x in (5, 7) and 3 <= y <= 6:
            return SHIRT_D
        if y >= 9:
            return noisy(SHIRT_S, 3)
        return noisy(base, 4)
    if f == "back":
        if x in (2, 5) and y > 2:
            return noisy(SHIRT_S, 3)
        return noisy(base if y < 9 else SHIRT_S, 4)
    if f == "top":
        return noisy(SHIRT_S, 3)
    if f == "bottom":
        return noisy(PANTS_S, 2)
    return noisy(SHIRT_S if x in (1, 2) else base, 4)


def p_arm(left):
    def fn(f, x, y, w, h):
        if f == "top":
            return noisy(SHIRT, 3)
        if f == "bottom":
            return noisy(SKIN_S, 3)
        if y <= 4:  # rolled sleeve
            return noisy(SHIRT_S if x in (0, w - 1) else SHIRT, 4)
        if y == 5:
            return noisy(SHIRT_D, 3)
        # forearm: bare skin + dried blood streaks
        col = SKIN if x % 3 else SKIN_S
        streak = (x * 7 + y * 3 + (11 if left else 0)) % 5
        if streak == 0 and y > 6:
            col = BLOOD
        elif streak == 1 and y > 8:
            col = BLOOD_D
        if y >= 10 and (x + y) % 3 == 0:
            col = BLOOD
        return noisy(col, 4)

    return fn


def p_cuff(f, x, y, w, h):
    return noisy(SHIRT_D if y == 0 else SHIRT_S, 4)


def p_leg(f, x, y, w, h):
    if f == "bottom":
        return noisy(PANTS_S, 2)
    if f == "top":
        return noisy(PANTS, 2)
    col = PANTS
    if f == "front" and x == 1:
        col = PANTS_L  # crease
    if f == "back" and x == 2:
        col = PANTS_S
    if f in ("left", "right") and x == 1:
        col = PANTS_S
    return noisy(col, 3)


def p_legcuff(f, x, y, w, h):
    return noisy(PANTS_L if y == 0 else c(78, 88, 93), 4)


def p_shoe(f, x, y, w, h):
    if f == "bottom":
        return noisy(SHOE_RED if (x + y) % 6 else WHITE_S, 3)
    if f == "top":
        return noisy(LACE if 1 <= x <= 3 and y % 2 == 0 else WHITE, 3)
    if f == "front":  # toe cap
        return noisy(SHOE_RED if y >= h - 2 else WHITE, 3)
    if f in ("left", "right"):
        if y >= h - 1:
            return noisy(SHOE_RED, 3)  # sole stripe
        if y == 1 and 1 <= x <= 3:
            return noisy(SHOE_RED, 3)  # ankle patch
        return noisy(WHITE if x > 0 else WHITE_S, 3)
    return noisy(WHITE_S if y >= h - 1 else WHITE, 3)


def p_blade(bloody_seed):
    r = random.Random(bloody_seed)
    splat = {(r.randrange(6), r.randrange(18)) for _ in range(20)}

    def fn(f, x, y, w, h):
        if f in ("left", "right"):  # flat sides: 6 wide x 18 tall
            if x in (0, w - 1):
                return noisy(METAL_K, 2)
            if x in (1, w - 2) and y % 2 == 0:
                return noisy(METAL_D, 3)
            col = METAL_L if x == 2 else METAL
            if x == 3:
                col = METAL_D
            if y % 4 == 2 and x in (2, 3):
                col = METAL_K  # bolts
            if (x, y) in splat or (y > 12 and (x + y) % 4 == 0):
                col = BLOOD if r.random() < 0.7 else BLOOD_D
            return noisy(col, 3)
        if f == "bottom":
            return noisy(METAL_K, 2)
        if f == "top":
            return noisy(METAL_D, 2)
        return noisy(METAL_K if y % 2 else METAL_D, 3)  # chain edge

    return fn


def p_bar(f, x, y, w, h):
    if f in ("left", "right"):  # 22 long x 4 tall
        if y in (0, h - 1):
            return noisy(METAL_K, 2)
        col = METAL_L if y == 1 else METAL
        if x % 5 == 2 and y == 2:
            col = METAL_K
        if x > w - 6 and (x + y) % 3 == 0:
            col = BLOOD
        return noisy(col, 3)
    if f in ("top", "bottom"):
        return noisy(METAL_K if (x + y) % 2 else METAL_D, 2)
    return noisy(METAL_K, 2)


def p_tooth(f, x, y, w, h):
    return noisy(METAL_K if (x + y) % 2 else METAL_D, 2)


def p_belt(f, x, y, w, h):
    return noisy(BELT, 4) if y else noisy(shade(BELT, -18), 3)


# ------------------------------------------------------------ geometry build
bones = []
tex = {}


def add_bone(name, parent=None, pivot=(0, 0, 0), cubes=None, rotation=None):
    b = {"name": name, "pivot": list(pivot)}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    if cubes:
        b["cubes"] = cubes
    bones.append(b)


def cube(origin, size, uv, inflate=None):
    cb = {"origin": list(origin), "size": list(size), "uv": list(uv)}
    if inflate:
        cb["inflate"] = inflate
    return cb


def box(origin, size, painter, inflate=None, uv=None):
    uv = uv or atlas.alloc(*size)
    paint_box(uv, size, painter)
    return cube(origin, size, uv, inflate)


add_bone("root")
add_bone("waist", "root", (0, 12, 0))

# torso -----------------------------------------------------------------
add_bone("body", "waist", (0, 24, 0), [box((-4, 12, -2), (8, 12, 4), p_body)])
add_bone("collar", "body", (0, 24, 0), [box((-3, 23.5, -2.4), (6, 2, 4), p_cuff, inflate=0.3)])
tie_uv = atlas.alloc(2, 9, 1)
paint_box(tie_uv, (2, 9, 1), lambda f, x, y, w, h: noisy(TIE_L if x == 0 else TIE, 2))
knot_uv = atlas.alloc(2, 2, 1)
paint_box(knot_uv, (2, 2, 1), p_solid(c(12, 12, 16), 1))
add_bone("tie", "body", (0, 23, -2.4), [
    cube((-1, 14, -2.5), (2, 9, 1), tie_uv),
    cube((-1, 22.5, -2.6), (2, 2, 1), knot_uv, 0.05),
])
buckle_uv = atlas.alloc(2, 2, 1)
paint_box(buckle_uv, (2, 2, 1), p_solid(BUCKLE, 6))
add_bone("belt", "body", (0, 12, 0), [
    box((-4, 12, -2), (8, 2, 4), p_belt, inflate=0.25),
    cube((-1, 12, -2.6), (2, 2, 1), buckle_uv, 0.05),
])

# head ------------------------------------------------------------------
head_cubes = [box((-4, 24, -4), (8, 8, 8), p_head, uv=atlas.alloc(8, 8, 8))]
add_bone("head", "body", (0, 24, 0), head_cubes)
add_bone("snout", "head", (0, 24, -4), [box((-3, 24, -6), (6, 3, 3), p_snout)])

# handle (rear top loop) and the saw bar sticking out of the forehead
add_bone("handle", "head", (0, 32, 1), [
    box((2, 32, 0), (1, 4, 2), p_solid(BLACK_L, 3)),
    box((-3, 32, 0), (1, 4, 2), p_solid(BLACK_L, 3)),
    box((-3, 35, 0), (6, 1, 2), p_solid(BLACK, 3)),
])
bar_len = 22
bar_cubes = [box((-1.5, 28.5, -4 - bar_len), (3, 4, bar_len), p_bar)]
tooth_uv = atlas.alloc(4, 1, 1)
paint_box(tooth_uv, (4, 1, 1), p_tooth)
z = -5.0
while z > -4 - bar_len:
    bar_cubes.append(cube((-2, 32.5, z - 1), (4, 1, 1), tooth_uv))
    bar_cubes.append(cube((-2, 28.0, z - 1), (4, 1, 1), tooth_uv))
    z -= 2.0
add_bone("bar", "head", (0, 30.5, -4), bar_cubes)

# arms ------------------------------------------------------------------
for side, sgn in (("right", -1), ("left", 1)):
    left = sgn == 1
    n = "left" if left else "right"
    pivot = (5 * sgn, 22, 0)
    arm_o = (4 if left else -8, 12, -2)
    add_bone(f"{n}Arm", "body", pivot, [box(arm_o, (4, 12, 4), p_arm(left))])
    add_bone(f"{n}Cuff", f"{n}Arm", pivot, [
        box((3.6 if left else -8.4, 16.8, -2.4), (4, 2, 4), p_cuff, inflate=0.0)
    ])
    add_bone(f"{n}Item", f"{n}Arm", (6 * sgn, 15, 1))
    # blade bolted to the outside of the forearm, hanging past the knee
    bx = 8 if left else -9.8
    blade = [box((bx, 1, -3), (2, 18, 6), p_blade(7 if left else 3))]
    tuv = atlas.alloc(1, 1, 1)
    paint_box(tuv, (1, 1, 1), p_tooth)
    y = 2.0
    while y < 19:
        blade.append(cube((bx + 0.4, y, -3.9), (1, 1, 1), tuv))
        blade.append(cube((bx + 0.4, y, 2.9), (1, 1, 1), tuv))
        y += 2.0
    add_bone(f"blade{'L' if left else 'R'}", f"{n}Arm", (bx + 1, 19, 0), blade)

# legs ------------------------------------------------------------------
for n, sgn in (("right", -1), ("left", 1)):
    pivot = (1.9 * sgn, 12, 0)
    add_bone(f"{n}Leg", "root", pivot, [box((-0.1 if sgn == 1 else -3.9, 0, -2), (4, 12, 4), p_leg)])
    add_bone(f"{n}LegCuff", f"{n}Leg", pivot, [
        box((-0.1 if sgn == 1 else -3.9, 4.6, -2), (4, 2, 4), p_legcuff, inflate=0.3)
    ])
    add_bone(f"{n}Shoe", f"{n}Leg", pivot, [
        box((-0.3 if sgn == 1 else -4.1, 0, -3), (4, 4, 5), p_shoe, inflate=0.2)
    ])

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.chainsawman",
            "texture_width": TW,
            "texture_height": TH,
            "visible_bounds_width": 5,
            "visible_bounds_height": 4,
            "visible_bounds_offset": [0, 1.5, 0],
        },
        "bones": bones,
    }],
}
(RP / "models/entity/chainsawman.geo.json").write_text(json.dumps(geo, indent=1))
img.save(RP / "textures/entity/chainsawman.png")
print("geometry bones:", len(bones), "cubes:", sum(len(b.get("cubes", [])) for b in bones))

# ------------------------------------------------------------- item icons
def icon(name, draw, size=32):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    p = im.load()
    draw(p, size)
    im.save(RP / "textures/items" / f"{name}.png")
    return im


def line(p, x0, y0, x1, y1, col, th=1):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        x = x0 + (x1 - x0) * t
        y = y0 + (y1 - y0) * t
        for dx in range(th):
            for dy in range(th):
                xi, yi = int(x) + dx, int(y) + dy
                if 0 <= xi < 32 and 0 <= yi < 32:
                    p[xi, yi] = col


def disc(p, cx, cy, r, col):
    for y in range(32):
        for x in range(32):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                p[x, y] = col


def d_heart(p, s):
    for y in range(s):
        for x in range(s):
            u = (x - 15.5) / 10.5
            v = -(y - 14.0) / 10.5
            if (u * u + v * v - 1) ** 3 - u * u * v ** 3 <= 0:
                edge = (u * u + v * v - 1) ** 3 - u * u * v ** 3 > -0.08
                p[x, y] = BLOOD_D if edge else (BLOOD if u + v < 0.7 else c(220, 60, 60))
    # tiny saw teeth around the heart: Pochita's cord
    line(p, 22, 4, 28, 1, METAL, 1)
    line(p, 22, 6, 29, 3, METAL_D, 1)
    for i in range(3):
        p[23 + i * 2, 4 - i] = METAL_L
    disc(p, 16, 26, 2, ORANGE)


def d_slash(p, s):
    for i in (0, 5, 10):
        line(p, 6 + i, 3, 22 + i - 8, 28, METAL_L, 2)
        line(p, 8 + i, 3, 24 + i - 8, 28, METAL_D, 1)
    for i in range(0, 24, 3):
        p[4 + i // 2, 6 + i] = BLOOD


def d_hurricane(p, s):
    for a in range(0, 360, 6):
        for rr, col in ((11, METAL_L), (8, METAL), (5, ORANGE)):
            th = math.radians(a + rr * 14)
            x = int(15.5 + rr * math.cos(th))
            y = int(15.5 + rr * math.sin(th))
            if 0 <= x < 32 and 0 <= y < 32:
                p[x, y] = col
    disc(p, 15, 15, 2, BLOOD)


def d_dash(p, s):
    line(p, 3, 16, 25, 16, METAL_L, 3)
    for i in range(0, 12):
        line(p, 25 - i // 2, 16 - i, 25 - i // 2, 16 + i, METAL, 1)
    for k in (8, 12, 20):
        line(p, 2, k, 9, k, BLOOD_D, 1)
    for k in (22, 26):
        line(p, 3, k, 12, k, BLOOD, 1)


def d_roar(p, s):
    for y in range(6, 26):
        for x in range(4, 28):
            if (x - 16) ** 2 / 144 + (y - 15) ** 2 / 90 <= 1:
                p[x, y] = ORANGE if y < 14 else BLACK
    for x in range(6, 26):
        if x % 3 != 2:
            for y in range(18, 21):
                p[x, y] = TEETH
    p[11, 13] = EYE
    p[12, 13] = EYE
    p[19, 13] = EYE
    p[20, 13] = EYE
    line(p, 16, 2, 16, 8, METAL_L, 2)


def d_spray(p, s):
    for (cx, cy, r) in ((8, 20, 3), (15, 14, 4), (22, 9, 3), (12, 26, 2), (24, 20, 2)):
        disc(p, cx, cy, r, BLOOD)
        disc(p, cx - 1, cy - 1, max(1, r - 2), c(220, 60, 60))
    line(p, 4, 28, 28, 4, BLOOD_D, 1)


for nm, fn in (("cm_heart", d_heart), ("cm_slash", d_slash), ("cm_hurricane", d_hurricane),
               ("cm_dash", d_dash), ("cm_roar", d_roar), ("cm_spray", d_spray)):
    icon(nm, fn)

(RP / "textures/item_texture.json").write_text(json.dumps({
    "resource_pack_name": "chainsaw_man",
    "texture_name": "atlas.items",
    "texture_data": {n: {"textures": f"textures/items/{n}"} for n in
                     ("cm_heart", "cm_slash", "cm_hurricane", "cm_dash", "cm_roar", "cm_spray")},
}, indent=1))

# ----------------------------------------------------------- particle tex
dot = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
dp = dot.load()
for y in range(8):
    for x in range(8):
        d = math.hypot(x - 3.5, y - 3.5)
        if d < 4:
            dp[x, y] = (255, 255, 255, int(255 * max(0.0, min(1.0, (4 - d) / 1.6))))
dot.save(RP / "textures/particle/cm_dot.png")

# ------------------------------------------------------------ pack icons
icon128 = Image.new("RGBA", (128, 128), (120, 10, 14, 255))
ip = icon128.load()
for y in range(128):
    for x in range(128):
        ip[x, y] = (int(150 - y * 0.6), 8, 14, 255)
fx, fy, fw, fh = face_rects((0, 0), (8, 8, 8))["front"]
for y in range(8):
    for x in range(8):
        col = img.getpixel((fx + x, fy + y))
        for dy in range(12):
            for dx in range(12):
                ip[16 + x * 12 + dx, 24 + y * 12 + dy] = col
for y in range(0, 24):  # saw bar above
    for x in range(52, 76):
        ip[x, y] = METAL if (x + y) % 6 else METAL_D
for f in ("BP", "RP"):
    icon128.save((BP if f == "BP" else RP) / "pack_icon.png")

# ------------------------------------------------------------------ sounds
SR = 44100


def write_wav(name, data):
    data = np.clip(data, -1, 1)
    pcm = (data * 32000).astype("<i2")
    with wave.open(str(RP / "sounds/cm" / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def env(n, a=0.02, r=0.2):
    t = np.arange(n) / SR
    d = n / SR
    return np.minimum(1, t / a) * np.minimum(1, (d - t) / r)


def engine(dur, f_start, f_end, grit=0.35, seed=0):
    r = np.random.RandomState(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = np.linspace(f_start, f_end, n) + 3 * np.sin(2 * np.pi * 7 * t)
    phase = 2 * np.pi * np.cumsum(f) / SR
    out = np.zeros(n)
    for k in range(1, 14):  # buzzy saw-ish harmonics
        out += np.sin(phase * k + r.rand() * 6.28) / k ** 0.8
    # two-stroke sputter: amplitude pulses at half the firing rate
    out *= 0.65 + 0.35 * np.sign(np.sin(phase * 0.5))
    noise = r.randn(n)
    noise = np.convolve(noise, np.ones(6) / 6, mode="same")
    out = out / np.max(np.abs(out)) + grit * noise
    return out / np.max(np.abs(out)) * 0.8


write_wav("engine_idle", engine(2.0, 34, 36, 0.3, 1) * env(int(2.0 * SR), 0.1, 0.25))
write_wav("engine_rev", engine(2.6, 34, 130, 0.4, 2) * env(int(2.6 * SR), 0.05, 0.5))
write_wav("engine_roar", engine(3.2, 30, 95, 0.55, 3) * env(int(3.2 * SR), 0.05, 0.9))
r = np.random.RandomState(5)
n = int(0.45 * SR)
t = np.arange(n) / SR
hit = r.randn(n) * np.exp(-t * 14)
for fr, am in ((1800, 0.5), (3100, 0.35), (4700, 0.25)):
    hit += am * np.sin(2 * np.pi * fr * t) * np.exp(-t * 18)
hit += engine(0.45, 70, 110, 0.2, 4) * 0.6 * np.exp(-t * 5)
write_wav("blade_hit", hit / np.max(np.abs(hit)) * 0.85)
n = int(0.7 * SR)
t = np.arange(n) / SR
sw = r.randn(n) * np.sin(np.pi * t / 0.7) ** 2
sw = np.convolve(sw, np.ones(4) / 4, mode="same")
sw += 0.6 * engine(0.7, 60, 140, 0.1, 6) * np.sin(np.pi * t / 0.7)
write_wav("blade_swing", sw / np.max(np.abs(sw)) * 0.8)
print("done")
