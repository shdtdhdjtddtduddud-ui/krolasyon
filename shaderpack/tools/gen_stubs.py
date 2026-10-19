#!/usr/bin/env python3
"""Shader paketindeki program giriş dosyalarını (stub) üretir.

Her program için kök klasöre ve boyut klasörlerine (world-1 = Nether, world1 = End)
küçük .vsh/.fsh dosyaları yazar; asıl kod shaders/program/*.glsl içindedir.
"""
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "KrolasyonFantasy", "shaders")

GBUFFERS = {
    "gbuffers_basic": "PROG_BASIC",
    "gbuffers_textured": "PROG_TEXTURED",
    "gbuffers_textured_lit": "PROG_TEXTURED",
    "gbuffers_skybasic": "PROG_SKY",
    "gbuffers_skytextured": "PROG_SKY",
    "gbuffers_clouds": "PROG_CLOUDS",
    "gbuffers_terrain": "PROG_TERRAIN",
    "gbuffers_water": "PROG_WATER",
    "gbuffers_hand_water": "PROG_WATER",
    "gbuffers_entities": "PROG_ENTITIES",
    "gbuffers_block": "PROG_BLOCK",
    "gbuffers_hand": "PROG_HAND",
    "gbuffers_weather": "PROG_WEATHER",
    "gbuffers_beaconbeam": "PROG_EMISSIVE",
    "gbuffers_spidereyes": "PROG_EMISSIVE",
    "gbuffers_armor_glint": "PROG_EMISSIVE",
    "gbuffers_damagedblock": "PROG_DAMAGE",
}
POST = ["shadow", "deferred", "composite", "composite1", "composite2", "final"]
WORLDS = {"": None, "world-1": "WORLD_NETHER", "world1": "WORLD_END"}


def stub(stage, world_define, prog_define, include):
    lines = ["#version 330 compatibility", ""]
    if world_define:
        lines.append(f"#define {world_define}")
    lines.append(f"#define {stage}")
    if prog_define:
        lines.append(f"#define {prog_define}")
    lines.append("")
    lines.append(f'#include "/program/{include}.glsl"')
    return "\n".join(lines) + "\n"


def main():
    for folder, wdef in WORLDS.items():
        out = os.path.join(ROOT, folder)
        os.makedirs(out, exist_ok=True)
        for name, prog in GBUFFERS.items():
            for ext, stage in (("vsh", "VSH"), ("fsh", "FSH")):
                with open(os.path.join(out, f"{name}.{ext}"), "w") as f:
                    f.write(stub(stage, wdef, prog, "gbuffers"))
        for name in POST:
            if name == "shadow" and wdef == "WORLD_NETHER":
                continue  # Nether'de güneş yok: gölge geçişini atla
            for ext, stage in (("vsh", "VSH"), ("fsh", "FSH")):
                with open(os.path.join(out, f"{name}.{ext}"), "w") as f:
                    f.write(stub(stage, wdef, None, name))


if __name__ == "__main__":
    main()
