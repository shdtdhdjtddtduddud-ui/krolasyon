#!/usr/bin/env python3
"""Zips both packs into dist/ChainsawMan.mcaddon (+ individual .mcpack files)."""
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DIST = ROOT / "dist"
DIST.mkdir(exist_ok=True)


def add_dir(z, folder, prefix):
    for f in sorted(folder.rglob("*")):
        if f.is_file():
            z.write(f, f"{prefix}/{f.relative_to(folder)}" if prefix else str(f.relative_to(folder)))


for name in ("ChainsawMan_BP", "ChainsawMan_RP"):
    with zipfile.ZipFile(DIST / f"{name}.mcpack", "w", zipfile.ZIP_DEFLATED) as z:
        add_dir(z, ROOT / name, "")
with zipfile.ZipFile(DIST / "ChainsawMan.mcaddon", "w", zipfile.ZIP_DEFLATED) as z:
    for name in ("ChainsawMan_BP", "ChainsawMan_RP"):
        add_dir(z, ROOT / name, name)
print("built", [p.name for p in DIST.iterdir()])
