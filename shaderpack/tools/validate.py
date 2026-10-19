#!/usr/bin/env python3
"""Shader paketini Mesa (llvmpipe) ile derleyip bağlayarak doğrular.

Kullanım: xvfb-run -a env MESA_GL_VERSION_OVERRIDE=4.5COMPAT python3 tools/validate.py
#include yönergelerini Iris gibi çözer, her .vsh/.fsh çiftini derler ve bağlar.
Ayrıca tüm açılıp kapanabilen seçenekleri kapalı hâle getirerek ikinci bir tur dener.
"""
import os
import re
import sys

import moderngl

SHADERS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "KrolasyonFantasy", "shaders")
INCLUDE_RE = re.compile(r'^\s*#include\s+"([^"]+)"', re.M)


def resolve(path, overrides, seen=None):
    seen = seen or set()
    with open(path) as f:
        src = f.read()
    if path.endswith("settings.glsl"):
        for name, val in overrides.items():
            if val is None:
                src = re.sub(rf"^#define {name}\b.*$", f"//#define {name}", src, flags=re.M)
            else:
                src = re.sub(rf"^(#define {name}) \S+", rf"\1 {val}", src, flags=re.M)

    def repl(m):
        inc = m.group(1)
        full = os.path.join(SHADERS, inc.lstrip("/")) if inc.startswith("/") else os.path.join(os.path.dirname(path), inc)
        return resolve(full, overrides, seen)

    return INCLUDE_RE.sub(repl, src)


def toggles():
    with open(os.path.join(SHADERS, "lib", "settings.glsl")) as f:
        return re.findall(r"^#define ([A-Z_]+)\s*(?://.*)?$", f.read(), re.M)


def run(ctx, overrides, label):
    failures = 0
    count = 0
    for folder in ("", "world-1", "world1"):
        d = os.path.join(SHADERS, folder)
        for fn in sorted(os.listdir(d)):
            if not fn.endswith(".vsh"):
                continue
            base = fn[:-4]
            vs = resolve(os.path.join(d, fn), overrides)
            fs = resolve(os.path.join(d, base + ".fsh"), overrides)
            count += 1
            try:
                ctx.program(vertex_shader=vs, fragment_shader=fs)
            except Exception as e:  # noqa: BLE001
                failures += 1
                print(f"[{label}] FAIL {folder or '.'}/{base}:\n{e}\n")
    print(f"[{label}] {count - failures}/{count} programs OK")
    return failures


def main():
    ctx = moderngl.create_standalone_context()
    print(ctx.info["GL_RENDERER"], ctx.info["GL_VERSION"])
    fails = run(ctx, {}, "default")
    fails += run(ctx, {t: None for t in toggles()}, "all-off")
    fails += run(ctx, {"SKY_THEME": 1, "SHADOW_SAMPLES": 16, "VL_STEPS": 24}, "theme1")
    fails += run(ctx, {"SKY_THEME": 2, "SHADOW_SAMPLES": 4}, "theme2")
    sys.exit(1 if fails else 0)


if __name__ == "__main__":
    main()
