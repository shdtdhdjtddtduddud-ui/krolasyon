"""Hulk model definition (Bedrock geometry, y-up, feet at y=0, front = -z, entity right = -x).

Everything is built from boxes.  Faces get individual UV rects (per-face UV) in a 1024x1024
atlas so we can control texel density per cube (head/hands get more pixels than the legs).
"""
import math, random

ATLAS = 1024
PAD = 2


class Cube:
    def __init__(self, bone, origin, size, mat, ppu, geo, feat, seed):
        self.bone, self.origin, self.size = bone, [float(v) for v in origin], [float(v) for v in size]
        self.mat, self.ppu, self.geo, self.feat, self.seed = mat, ppu, geo, feat, seed
        self.uv = {}      # face -> (x, y, w, h) inner rect in atlas px (without pad)


YS = 0.9


def ty(y):
    """leg stretch + global height scale"""
    return (y * 1.35 if y <= 17 else y + 6.0) * YS if y >= 0 else y * YS


class Model:
    def __init__(self):
        self.bones = {}        # name -> dict(parent, pivot, geo)
        self.order = []
        self.cubes = []
        self._seed = 100

    def bone(self, name, parent, pivot, geo="hulk"):
        x, y, z = pivot
        if geo == "hulk":
            x, y, z = x * self.fx(name), ty(y), z * self.fz(name)
        self.bones[name] = dict(parent=parent, pivot=[float(x), float(y), float(z)], geo=geo)
        self.order.append(name)

    HEAD = ("h_head", "h_jaw")

    def fx(self, bone):
        return 1.0 if bone in self.HEAD else 0.78

    def fz(self, bone):
        return 1.0 if bone in self.HEAD else 0.86

    def cube(self, bone, origin, size, mat="skin", ppu=5.0, feat=None):
        if self.bones[bone]["geo"] == "hulk":
            fx, fz = self.fx(bone), self.fz(bone)
            x0, y0, z0 = origin
            y1 = y0 + size[1]
            origin = (x0 * fx, ty(y0), z0 * fz)
            size = (size[0] * fx, ty(y1) - ty(y0), size[2] * fz)
        self._seed += 1
        c = Cube(bone, origin, size, mat, ppu, self.bones[bone]["geo"], feat, self._seed)
        self.cubes.append(c)
        return c

    def mirror_cube(self, bone, origin, size, mat="skin", ppu=5.0, feat=None):
        x, y, z = origin
        w = size[0]
        return self.cube(bone, (-(x + w), y, z), size, mat, ppu, feat)

    # ---------------------------------------------------------------- packing
    def faces_of(self, c):
        w, h, d = c.size
        return {
            "north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h),
            "up": (w, d), "down": (w, d),
        }

    def pack(self, scale=1.0):
        rects = []
        for i, c in enumerate(self.cubes):
            for f, (a, b) in self.faces_of(c).items():
                k = c.ppu * scale
                if f == "up":
                    k *= 0.8
                if f == "down":
                    k *= 0.35
                W = max(2, int(math.ceil(a * k)))
                H = max(2, int(math.ceil(b * k)))
                rects.append((H, W, i, f))
        rects.sort(reverse=True)
        x = y = row_h = 0
        placed = []
        for H, W, i, f in rects:
            pw, ph = W + 2 * PAD, H + 2 * PAD
            if x + pw > ATLAS:
                x = 0
                y += row_h
                row_h = 0
            if y + ph > ATLAS:
                return False
            placed.append((i, f, x + PAD, y + PAD, W, H))
            x += pw
            row_h = max(row_h, ph)
        for i, f, px, py, W, H in placed:
            self.cubes[i].uv[f] = (px, py, W, H)
        self.used_h = y + row_h
        return True

    def auto_pack(self):
        s = 1.0
        while not self.pack(s):
            s *= 0.95
        self.pack_scale = s
        return s

    # ---------------------------------------------------------------- export
    def geometry_json(self, geo, ident, vis=(0, 0)):
        bones = []
        for name in self.order:
            b = self.bones[name]
            if b["geo"] != geo:
                continue
            cubes = []
            for c in self.cubes:
                if c.bone != name:
                    continue
                uv = {}
                for f, (x, y, w, h) in c.uv.items():
                    if f == "up":
                        uv[f] = {"uv": [x + w, y + h], "uv_size": [-w, -h]}
                    elif f == "down":
                        uv[f] = {"uv": [x + w, y], "uv_size": [-w, h]}
                    else:
                        uv[f] = {"uv": [x, y], "uv_size": [w, h]}
                cubes.append({"origin": [round(v, 3) for v in c.origin],
                              "size": [round(v, 3) for v in c.size], "uv": uv})
            e = {"name": name, "pivot": b["pivot"], "cubes": cubes}
            if b["parent"]:
                e["parent"] = b["parent"]
            if name == "rightItem":
                e.pop("cubes")
            bones.append(e)
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": ident,
                    "texture_width": ATLAS, "texture_height": ATLAS,
                    "visible_bounds_width": 6, "visible_bounds_height": 6,
                    "visible_bounds_offset": [0, 1.6, 0],
                },
                "bones": bones,
            }],
        }


# =====================================================================================
def build():
    m = Model()
    B = m.bone
    # ---- skeleton (all pivots absolute)
    B("h_root", None, (0, 0, 0))
    B("h_body", "h_root", (0, 17, 0))
    B("h_torso", "h_body", (0, 23, 0))
    B("h_head", "h_torso", (0, 38, 0))
    B("h_jaw", "h_head", (0, 41, -0.5))
    for s, sg in (("R", -1), ("L", 1)):
        B(f"h_arm{s}", "h_torso", (sg * 15.5, 36, 0))
        B(f"h_forearm{s}", f"h_arm{s}", (sg * 17.2, 22.4, 0))
        B(f"h_hand{s}", f"h_forearm{s}", (sg * 17.4, 10.6, 0))
        B(f"h_leg{s}", "h_root", (sg * 7, 17, 0))
        B(f"h_shin{s}", f"h_leg{s}", (sg * 7, 8, 0))
        B(f"h_foot{s}", f"h_shin{s}", (sg * 7, 2.8, 0))
    B("rightItem", "h_handR", (-17.4, 6.0, -4.0))

    both = lambda fn: (fn(-1), fn(1))

    def cube(bone, o, s, mat="skin", ppu=5.0, feat=None, side=-1):
        """define for side=-1 (right, x<0); for side=+1 mirror."""
        if side == -1:
            return m.cube(bone, o, s, mat, ppu, feat)
        return m.mirror_cube(bone, o, s, mat, ppu, feat)

    # ---- pelvis / shorts ------------------------------------------------------------
    m.cube("h_body", (-10.6, 16.6, -6.2), (21.2, 6.2, 12.4), "shorts", 5)
    m.cube("h_body", (-10.9, 22.0, -6.5), (21.8, 1.8, 13.0), "waist", 5)
    m.cube("h_body", (-9.0, 18.0, 5.7), (18.0, 3.5, 1.0), "shorts", 3)  # seat patch

    R = random.Random(7)
    for side, sg in ((-1, "R"), (1, "L")):
        L, S, F = f"h_leg{sg}", f"h_shin{sg}", f"h_foot{sg}"
        # shorts tube on the thigh
        cube(L, (-13.8, 13.4, -6.9), (13.4, 5.8, 13.8), "shorts", 5, side=side)
        # frayed hem strips
        for i in range(5):
            ln = R.uniform(1.4, 3.2)
            cube(L, (-13.5 + i * 2.6, 13.4 - ln, -6.95), (2.5, ln, 0.9), "fray", 5, "fray", side)
            ln = R.uniform(1.2, 3.0)
            cube(L, (-13.5 + i * 2.6, 13.4 - ln, 6.05), (2.5, ln, 0.9), "fray", 5, "fray", side)
        for i in range(4):
            ln = R.uniform(1.2, 2.8)
            cube(L, (-14.2, 13.4 - ln, -6.0 + i * 3.2), (0.9, ln, 3.0), "fray", 5, "fray", side)
        for i in range(3):
            ln = R.uniform(1.2, 2.6)
            cube(L, (-0.7, 13.4 - ln, -5.0 + i * 3.6), (0.9, ln, 3.4), "fray", 4, "fray", side)
        # thigh skin below hem + knee
        cube(L, (-12.3, 7.4, -6.3), (11.6, 6.2, 12.6), "skin", 5, side=side)
        cube(S, (-11.0, 6.3, -7.2), (8.6, 3.2, 1.8), "skin", 5, side=side)          # knee cap
        # calf
        cube(S, (-11.7, 2.8, -5.6), (9.8, 5.4, 11.0), "skin", 5, side=side)
        cube(S, (-11.2, 3.2, 0.4), (8.8, 5.2, 5.4), "skin", 5, side=side)           # gastroc
        cube(S, (-10.2, 3.0, -6.3), (5.6, 4.8, 1.2), "skin", 5, side=side)          # shin crest
        # foot + toes
        cube(F, (-12.2, 0.0, -7.6), (10.4, 2.9, 13.6), "skin", 5, side=side)
        cube(F, (-11.4, 0.0, 5.6), (8.8, 3.6, 2.2), "skin", 4, side=side)           # heel
        for (x0, w0, l0, h0) in ((-4.9, 2.9, 4.0, 2.6), (-7.1, 2.3, 3.6, 2.3), (-9.0, 2.0, 3.2, 2.1),
                                 (-10.8, 1.9, 2.8, 1.9), (-12.3, 1.8, 2.4, 1.7)):
            cube(F, (x0, 0.0, -7.6 - l0), (w0, h0, l0), "skin", 7, "toe", side)

    # ---- torso -----------------------------------------------------------------------
    m.cube("h_torso", (-8.6, 22.6, -4.6), (17.2, 7.6, 9.2), "skin_t", 5)
    rows = [(23.2, 7.4), (25.4, 7.0), (27.6, 6.6)]
    for r, (y0, wd) in enumerate(rows):
        for side in (-1, 1):
            cube("h_torso", (-0.7 - wd, y0, -6.0), (wd, 2.0, 1.5), "skin_t", 6, "abs", side)
    for side in (-1, 1):
        cube("h_torso", (-11.0, 23.2, -4.2), (2.9, 6.8, 8.4), "skin_t", 5, "oblique", side)
        cube("h_torso", (-14.4, 26.0, 4.0), (12.6, 11.4, 2.9), "skin_t", 5, "lat", side)   # lats
        cube("h_torso", (-13.6, 29.8, -6.4), (13.0, 8.4, 12.6), "skin_t", 5, "ribs", side)
        cube("h_torso", (-13.4, 30.2, -9.0), (12.8, 6.4, 3.4), "skin_t", 7, "pec", side)   # pecs
        cube("h_torso", (-12.6, 34.2, -8.4), (11.2, 2.6, 3.0), "skin_t", 6, "pec_top", side)
        cube("h_torso", (-12.0, 36.6, -4.4), (5.6, 2.4, 8.8), "skin_t", 5, "trap", side)
    m.cube("h_torso", (-7.0, 30.4, 5.0), (14.0, 7.6, 1.4), "skin_t", 5, "back")
    m.cube("h_torso", (-6.4, 36.8, -4.0), (12.8, 2.6, 8.0), "skin_t", 5, "trap")

    # ---- head -----------------------------------------------------------------------
    m.cube("h_head", (-3.5, 37.0, -3.4), (7.0, 3.0, 6.6), "skin", 6)                       # neck
    m.cube("h_head", (-4.1, 41.0, -4.2), (8.2, 5.6, 8.6), "skin", 12, "skull")
    m.cube("h_head", (-4.6, 43.4, -5.0), (9.2, 1.7, 1.4), "skin", 10, "brow")
    m.cube("h_head", (-1.3, 41.2, -5.5), (2.6, 2.4, 1.6), "skin", 12, "nose")
    for side in (-1, 1):
        cube("h_head", (-4.8, 41.2, -4.6), (1.4, 2.8, 3.4), "skin", 8, "cheek", side)
        cube("h_head", (-4.6, 39.6, -2.8), (1.0, 2.6, 4.0), "skin", 6, "jaw_side", side)  # jaw hinge
    m.cube("h_head", (-3.3, 40.4, -3.9), (6.6, 1.0, 7.0), "mouth", 4)
    m.cube("h_head", (-3.4, 40.7, -5.05), (6.8, 0.9, 0.6), "teeth", 12, "teeth_up")
    m.cube("h_jaw", (-3.7, 38.6, -4.5), (7.4, 2.4, 8.0), "skin", 10, "jaw")
    m.cube("h_jaw", (-2.4, 38.3, -5.3), (4.8, 1.6, 1.3), "skin", 10, "chin")
    m.cube("h_jaw", (-3.2, 40.2, -4.85), (6.4, 0.85, 0.6), "teeth", 12, "teeth_low")
    # hair
    m.cube("h_head", (-4.4, 45.6, -4.7), (8.8, 1.5, 9.4), "hair", 8)
    m.cube("h_head", (-4.5, 42.0, 3.6), (9.0, 4.6, 1.7), "hair", 6)
    m.cube("h_head", (-4.5, 44.0, -3.2), (0.9, 2.4, 6.6), "hair", 5)
    m.cube("h_head", (3.6, 44.0, -3.2), (0.9, 2.4, 6.6), "hair", 5)
    Rh = random.Random(21)
    for i in range(7):
        x = -3.9 + i * 1.25 + Rh.uniform(-0.3, 0.3)
        m.cube("h_head", (x, 46.6, Rh.uniform(-4.4, 1.5)), (Rh.uniform(1.6, 2.4), Rh.uniform(0.8, 1.5), Rh.uniform(2.2, 3.4)), "hair", 6)
    for i in range(4):
        m.cube("h_head", (-3.6 + i * 2.1, 45.0, -5.15), (1.9, 1.3 + (i % 2) * 0.5, 0.7), "hair", 8)  # fringe

    # ---- arms ------------------------------------------------------------------------
    for side, sg in ((-1, "R"), (1, "L")):
        A, Fa, H = f"h_arm{sg}", f"h_forearm{sg}", f"h_hand{sg}"
        cube(A, (-23.0, 30.4, -5.6), (11.0, 9.8, 11.2), "skin_t", 5, "delt", side)
        cube(A, (-22.0, 39.6, -5.0), (9.0, 1.6, 10.0), "skin_t", 5, "delt_cap", side)
        cube(A, (-22.4, 21.6, -5.0), (9.8, 9.6, 10.0), "skin_a", 5, side=side)         # upper arm
        cube(A, (-21.4, 24.2, -6.9), (7.8, 6.6, 2.0), "skin_a", 6, "bicep", side)
        cube(A, (-21.8, 23.0, 4.6), (8.2, 7.4, 2.2), "skin_a", 5, "tricep", side)
        cube(Fa, (-22.6, 9.6, -5.6), (11.0, 13.0, 11.0), "skin_a", 5, side=side)        # forearm
        cube(Fa, (-21.6, 13.8, -7.4), (8.6, 7.0, 2.0), "skin_a", 6, "flexor", side)
        cube(Fa, (-23.6, 11.4, -3.6), (1.6, 8.6, 7.2), "skin_a", 5, "extensor", side)
        cube(H, (-21.0, 7.8, -4.2), (7.6, 3.2, 8.4), "skin_a", 6, side=side)             # wrist
        cube(H, (-21.8, 3.2, -4.8), (9.2, 4.8, 9.6), "skin_a", 7, "fist", side)           # fist body
        for i in range(4):
            ln = (3.6, 3.9, 3.7, 3.2)[i]
            cube(H, (-21.5 + i * 2.35, 7.6 - ln, -7.6), (2.3, ln, 2.9), "skin_a", 8, "finger", side)
        cube(H, (-12.6, 4.8, -5.0), (2.4, 3.6, 4.0), "skin_a", 8, "thumb", side)

    # ---- first person arms (own geometry, same atlas) ------------------------------
    m.bone("rightArm", None, (-5, 22, 0), geo="hulk_fp")
    m.bone("leftArm", None, (5, 22, 0), geo="hulk_fp")
    for side, nm in ((-1, "rightArm"), (1, "leftArm")):
        cube(nm, (-10.5, 9.0, -4.6), (10.0, 15.2, 9.2), "skin_a", 8, "fp_arm", side)
        cube(nm, (-10.8, 4.6, -5.2), (10.6, 5.2, 10.4), "skin_a", 9, "fist", side)
        for i in range(4):
            cube(nm, (-10.4 + i * 2.55, 1.6, -8.4), (2.5, 4.4, 3.2), "skin_a", 9, "finger", side)
        cube(nm, (-0.6, 4.4, -5.2), (2.4, 3.8, 4.4), "skin_a", 9, "thumb", side)
    return m


if __name__ == "__main__":
    m = build()
    s = m.auto_pack()
    print("cubes", len(m.cubes), "scale", s, "used_h", m.used_h)
