#!/usr/bin/env python3
"""Generates the per-program wrapper files (overworld, nether, end) and the
noise texture of the Fjord Mist shader pack. Run from anywhere."""
import os, random, struct, zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "FjordMist", "shaders")

PROGRAMS = {
    "shadow":                 ("shadow.glsl", []),
    "gbuffers_basic":         ("gbuffers_lit.glsl", ["BASIC"]),
    "gbuffers_textured":      ("gbuffers_lit.glsl", ["PARTICLE"]),
    "gbuffers_textured_lit":  ("gbuffers_lit.glsl", ["PARTICLE"]),
    "gbuffers_terrain":       ("gbuffers_lit.glsl", ["TERRAIN"]),
    "gbuffers_block":         ("gbuffers_lit.glsl", []),
    "gbuffers_entities":      ("gbuffers_lit.glsl", ["ENTITY"]),
    "gbuffers_hand":          ("gbuffers_lit.glsl", ["HAND"]),
    "gbuffers_water":         ("gbuffers_water.glsl", []),
    "gbuffers_hand_water":    ("gbuffers_water.glsl", ["HAND_WATER"]),
    "gbuffers_skybasic":      ("gbuffers_sky.glsl", ["SKYBASIC"]),
    "gbuffers_skytextured":   ("gbuffers_sky.glsl", ["SKYTEXTURED"]),
    "gbuffers_clouds":        ("gbuffers_misc.glsl", ["CLOUDS"]),
    "gbuffers_damagedblock":  ("gbuffers_misc.glsl", ["DAMAGED"]),
    "gbuffers_beaconbeam":    ("gbuffers_misc.glsl", ["EMISSIVE"]),
    "gbuffers_spidereyes":    ("gbuffers_misc.glsl", ["EMISSIVE"]),
    "gbuffers_armor_glint":   ("gbuffers_misc.glsl", ["GLINT"]),
    "gbuffers_weather":       ("gbuffers_misc.glsl", ["WEATHER"]),
    "composite":              ("composite.glsl", []),
    "composite1":             ("composite1.glsl", []),
    "composite2":             ("composite2.glsl", []),
    "final":                  ("final.glsl", []),
}

DIMENSIONS = {"": [], "world-1": ["NETHER"], "world1": ["END"]}


def write_wrappers():
    for folder, dim_defines in DIMENSIONS.items():
        out_dir = os.path.join(ROOT, folder)
        os.makedirs(out_dir, exist_ok=True)
        for name, (source, defines) in PROGRAMS.items():
            for ext, stage in (("vsh", "VSH"), ("fsh", "FSH")):
                lines = ["#version 120"]
                lines += ["#define " + d for d in dim_defines + [stage] + defines]
                lines.append('#include "/program/%s"' % source)
                with open(os.path.join(out_dir, "%s.%s" % (name, ext)), "w", newline="\n") as f:
                    f.write("\n".join(lines) + "\n")


def png(path, w, h, rows):
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    raw = b"".join(b"\x00" + bytes(r) for r in rows)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def write_noise():
    # R: random. G: R shifted by (37,17) -> two z slices of 3D noise per fetch.
    # B, A: independent random channels.
    n = 256
    rnd = random.Random(1337)
    r = [[rnd.randrange(256) for _ in range(n)] for _ in range(n)]
    b = [[rnd.randrange(256) for _ in range(n)] for _ in range(n)]
    a = [[rnd.randrange(256) for _ in range(n)] for _ in range(n)]
    rows = []
    for y in range(n):
        row = []
        for x in range(n):
            g = r[(y + 17) % n][(x + 37) % n]
            row += [r[y][x], g, b[y][x], a[y][x]]
        rows.append(row)
    tex_dir = os.path.join(ROOT, "textures")
    os.makedirs(tex_dir, exist_ok=True)
    png(os.path.join(tex_dir, "noise.png"), n, n, rows)
    with open(os.path.join(tex_dir, "noise.png.mcmeta"), "w") as f:
        f.write('{\n  "texture": {\n    "blur": true,\n    "clamp": false\n  }\n}\n')


if __name__ == "__main__":
    write_wrappers()
    write_noise()
    print("done")
