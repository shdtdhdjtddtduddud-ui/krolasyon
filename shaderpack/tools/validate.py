#!/usr/bin/env python3
"""Compiles every program of the pack with glslangValidator.
Usage: validate.py <path to glslangValidator>"""
import os, re, subprocess, sys, tempfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "FjordMist", "shaders")
# macros Iris / OptiFine inject
IRIS_DEFINES = """#define MC_VERSION 12001
#define MC_GL_VERSION 320
#define MC_GLSL_VERSION 150
#define MC_RENDER_STAGE_SUN 3
#define MC_RENDER_STAGE_MOON 4
#define MC_RENDER_STAGE_STARS 5
"""
INC = re.compile(r'^\s*#include\s+"([^"]+)"', re.M)


def expand(path, seen=()):
    src = open(path).read()
    def repl(m):
        inc = m.group(1)
        p = os.path.join(ROOT, inc.lstrip("/")) if inc.startswith("/") else os.path.join(os.path.dirname(path), inc)
        return expand(p, seen + (path,))
    return INC.sub(repl, src)


def main():
    validator = sys.argv[1]
    failures = 0
    files = []
    for folder in ("", "world-1", "world1"):
        d = os.path.join(ROOT, folder)
        files += [os.path.join(d, f) for f in sorted(os.listdir(d)) if f.endswith((".vsh", ".fsh"))]
    for f in files:
        src = expand(f)
        lines = src.split("\n")
        src = lines[0] + "\n" + IRIS_DEFINES + "\n".join(lines[1:])
        stage = "vert" if f.endswith(".vsh") else "frag"
        with tempfile.NamedTemporaryFile("w", suffix="." + stage, delete=False) as t:
            t.write(src)
        r = subprocess.run([validator, "-S", stage, t.name], capture_output=True, text=True)
        os.unlink(t.name)
        if r.returncode != 0:
            failures += 1
            print("FAIL", os.path.relpath(f, ROOT))
            print(r.stdout)
    print("%d files, %d failures" % (len(files), failures))
    sys.exit(1 if failures else 0)


main()
