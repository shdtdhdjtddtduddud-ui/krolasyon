#!/usr/bin/env python3
"""Packs FjordMist/ into FjordMist.zip (ready to drop into .minecraft/shaderpacks)."""
import os, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "FjordMist")
OUT = os.path.join(HERE, "..", "FjordMist.zip")

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for root, _, files in os.walk(SRC):
        for f in sorted(files):
            full = os.path.join(root, f)
            arc = os.path.relpath(full, SRC).replace(os.sep, "/")
            info = zipfile.ZipInfo(arc, date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            with open(full, "rb") as fh:
                z.writestr(info, fh.read())
print("wrote", os.path.normpath(OUT))
