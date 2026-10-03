"""Tiny voxel structure toolkit: build with Python, write vanilla structure .nbt, preview isometrically."""
import math, os
import numpy as np
import nbtlib
from nbtlib import Compound, List, Int, String, Double, Byte

DATA_VERSION = 3465   # Minecraft 1.20.1
AIR = ('minecraft:air', ())


def B(name, **props):
    if ':' not in name:
        name = 'minecraft:' + name
    return (name, tuple(sorted((k, str(v)) for k, v in props.items())))


def st(name, facing='north', half='bottom'):
    return B(name, facing=facing, half=half, shape='straight', waterlogged='false')


def sl(name, type='bottom'):
    return B(name, type=type, waterlogged='false')


def lg(name, axis='y'):
    return B(name, axis=axis)


def banner(color, facing):
    return B(f'minecraft:{color}_wall_banner', facing=facing)


DIRS = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}
OPP = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}


class Vox:
    def __init__(self, w, h, d, name='structure'):
        self.size = (w, h, d)
        self.cells = {}
        self.entities = []
        self.nbt = {}
        self.name = name

    def inb(self, x, y, z):
        w, h, d = self.size
        return 0 <= x < w and 0 <= y < h and 0 <= z < d

    def set(self, x, y, z, b, only_air=False):
        x, y, z = int(x), int(y), int(z)
        if not self.inb(x, y, z):
            return
        if isinstance(b, str):
            b = B(b)
        if only_air and (x, y, z) in self.cells and self.cells[(x, y, z)] != AIR:
            return
        self.cells[(x, y, z)] = b

    def get(self, x, y, z):
        return self.cells.get((int(x), int(y), int(z)))

    def fill(self, x0, y0, z0, x1, y1, z1, b, only_air=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, b, only_air)

    def shell(self, x0, y0, z0, x1, y1, z1, b):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or y in (y0, y1) or z in (z0, z1):
                        self.set(x, y, z, b)

    def carve(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, AIR)

    def line(self, p0, p1, b):
        n = int(max(abs(p1[i] - p0[i]) for i in range(3))) + 1
        for i in range(n + 1):
            t = i / max(n, 1)
            self.set(round(p0[0] + (p1[0] - p0[0]) * t), round(p0[1] + (p1[1] - p0[1]) * t), round(p0[2] + (p1[2] - p0[2]) * t), b)

    def disc(self, cx, cz, r, y, b, hollow=False):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                d = math.hypot(x - cx, z - cz)
                if d <= r + 0.3 and (not hollow or d > r - 1.0):
                    self.set(x, y, z, b)

    def cylinder(self, cx, cz, r, y0, y1, b, hollow=False):
        for y in range(y0, y1 + 1):
            self.disc(cx, cz, r, y, b, hollow)

    def dome(self, cx, cy, cz, r, b, hollow=True, ymax=None):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for y in range(int(cy), int(cy + r + 2)):
                for z in range(int(cz - r - 1), int(cz + r + 2)):
                    d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2)
                    if d <= r + 0.3 and (not hollow or d > r - 1.1):
                        self.set(x, y, z, b)

    def cone(self, cx, cz, r, y0, h, b):
        for k in range(h):
            rr = r * (1 - k / h)
            self.disc(cx, cz, max(rr, 0.4), y0 + k, b)

    def pyramid(self, x0, z0, x1, z1, y0, b, hollow=False):
        k = 0
        while x0 + k <= x1 - k and z0 + k <= z1 - k:
            if hollow:
                self.shell(x0 + k, y0 + k, z0 + k, x1 - k, y0 + k, z1 - k, b)
            else:
                self.fill(x0 + k, y0 + k, z0 + k, x1 - k, y0 + k, z1 - k, b)
            k += 1
        return k

    def ent(self, x, y, z, eid, nbt=None, yaw=0.0):
        self.entities.append((x, y, z, eid, nbt or {}, yaw))

    def block_nbt(self, x, y, z, nbt):
        self.nbt[(int(x), int(y), int(z))] = nbt

    # ---------------- export
    def save(self, path):
        palette, index = [], {}
        blocks = []
        for (x, y, z), b in sorted(self.cells.items()):
            if b not in index:
                index[b] = len(palette)
                c = Compound({'Name': String(b[0])})
                if b[1]:
                    c['Properties'] = Compound({k: String(v) for k, v in b[1]})
                palette.append(c)
            e = Compound({'pos': List[Int]([x, y, z]), 'state': Int(index[b])})
            if (x, y, z) in self.nbt:
                e['nbt'] = self._compound(self.nbt[(x, y, z)])
            blocks.append(e)
        ents = []
        for x, y, z, eid, nbt, yaw in self.entities:
            n = dict(nbt)
            n['id'] = eid
            n.setdefault('PersistenceRequired', Byte(1))
            n['Rotation'] = List[nbtlib.Float]([nbtlib.Float(yaw), nbtlib.Float(0)])
            ents.append(Compound({'pos': List[Double]([Double(x + 0.5), Double(y), Double(z + 0.5)]), 'blockPos': List[Int]([int(x), int(y), int(z)]), 'nbt': self._compound(n)}))
        w, h, d = self.size
        root = Compound({'DataVersion': Int(DATA_VERSION), 'size': List[Int]([w, h, d]), 'palette': List[Compound](palette), 'blocks': List[Compound](blocks), 'entities': List[Compound](ents)})
        os.makedirs(os.path.dirname(path), exist_ok=True)
        nbtlib.File(root, gzipped=True).save(path)
        return len(blocks)

    def _compound(self, d):
        out = Compound()
        for k, v in d.items():
            if isinstance(v, dict):
                out[k] = self._compound(v)
            elif isinstance(v, str):
                out[k] = String(v)
            elif isinstance(v, bool):
                out[k] = Byte(1 if v else 0)
            elif isinstance(v, int):
                out[k] = Int(v)
            elif isinstance(v, float):
                out[k] = nbtlib.Float(v)
            else:
                out[k] = v
        return out

    # ---------------- preview
    def preview(self, path, scale=6, view=0):
        from PIL import Image, ImageDraw
        w, h, d = self.size
        cols = {}
        def color(name):
            n = name.split(':')[-1]
            table = [('lamp', (255, 200, 90)), ('lantern', (255, 210, 120)), ('magma', (220, 90, 20)), ('lava', (255, 120, 20)), ('portal', (200, 40, 70)),
                     ('hellgate', (30, 20, 40)), ('ember_rock', (120, 50, 20)), ('gilded', (110, 90, 50)), ('blackstone', (44, 38, 48)), ('obsidian', (24, 14, 40)),
                     ('bone', (232, 224, 200)), ('quartz', (236, 230, 222)), ('red_nether', (120, 20, 28)), ('nether_brick', (60, 20, 24)), ('crimson', (130, 30, 60)),
                     ('red_stained', (200, 40, 50)), ('purple_stained', (130, 60, 200)), ('amethyst', (150, 100, 210)), ('deepslate', (60, 60, 68)), ('purpur', (160, 110, 170)),
                     ('mushroom_stem', (220, 210, 190)), ('red_mushroom', (190, 50, 40)), ('brown_mushroom', (130, 90, 60)), ('mud', (80, 66, 56)), ('moss', (80, 110, 40)),
                     ('mangrove', (100, 40, 40)), ('stone', (110, 110, 116)), ('ashstone', (70, 70, 80)), ('banner', (180, 40, 40)), ('glass', (170, 200, 230)),
                     ('wool', (200, 200, 200)), ('chain', (60, 60, 70)), ('skull', (230, 230, 220)), ('shroomlight', (250, 180, 90)), ('glowstone', (250, 210, 120)),
                     ('end_rod', (240, 240, 250)), ('soul', (80, 200, 240)), ('iron', (170, 170, 180)), ('gold', (240, 200, 60)), ('dark_oak', (60, 40, 20)),
                     ('spruce', (90, 66, 40)), ('oak', (140, 110, 60)), ('sandstone', (200, 190, 140)), ('terracotta', (150, 90, 70)), ('concrete', (120, 120, 140)),
                     ('sculk', (14, 40, 50)), ('calcite', (225, 225, 220)), ('basalt', (60, 60, 64)), ('netherrack', (110, 40, 40)), ('vine', (50, 110, 40)), ('web', (230, 230, 230)),
                     ('candle', (240, 230, 200)), ('torch', (255, 200, 90)), ('chest', (150, 100, 40)), ('spawner', (20, 20, 40)), ('air', None)]
            for k, c in table:
                if k in n:
                    return c
            return (150, 150, 150)
        S = scale
        W = int((w + d) * S * 0.9) + 40
        H = int(h * S * 0.9 + (w + d) * S * 0.45) + 40
        im = Image.new('RGB', (W, H), (40, 44, 52))
        dr = ImageDraw.Draw(im)
        order = sorted(self.cells.items(), key=lambda kv: (kv[0][1], kv[0][0] + kv[0][2]))
        # painter: back-to-front along the view diagonal
        def key(kv):
            (x, y, z), _ = kv
            if view == 0:
                return (x + z, y)
            return (x + (d - z), y)
        order = sorted(self.cells.items(), key=lambda kv: (key(kv)[0], key(kv)[1]))
        for (x, y, z), b in order:
            c = color(b[0])
            if c is None:
                continue
            xx, zz = (x, z) if view == 0 else (x, d - 1 - z)
            px = (xx - zz) * S * 0.9 + (d * S * 0.9) + 20
            py = (xx + zz) * S * 0.45 - y * S * 0.9 + h * S * 0.9 + 10
            top = [(px, py), (px + S * 0.9, py + S * 0.45), (px, py + S * 0.9), (px - S * 0.9, py + S * 0.45)]
            left = [(px - S * 0.9, py + S * 0.45), (px, py + S * 0.9), (px, py + S * 1.8), (px - S * 0.9, py + S * 1.35)]
            right = [(px + S * 0.9, py + S * 0.45), (px, py + S * 0.9), (px, py + S * 1.8), (px + S * 0.9, py + S * 1.35)]
            lit = 'lamp' in b[0] or 'lava' in b[0] or 'magma' in b[0] or 'lantern' in b[0]
            f = lambda m: tuple(min(255, int(v * m)) for v in c)
            dr.polygon(left, fill=f(1.0 if lit else 0.6))
            dr.polygon(right, fill=f(1.0 if lit else 0.8))
            dr.polygon(top, fill=f(1.0 if lit else 1.1))
        im.save(path)
