#!/usr/bin/env python3
"""Generates the source tree of a ported game version from the main (1.20.1) sources.

    python3 versions/port.py 1.21.1

Copies src/main (beyblade mod only) into versions/<ver>/build-src, applies the textual changes listed in
versions/<ver>/rules.py and then copies versions/<ver>/overlay over it (whole files that differ per version).
Every rule must match, so a change in the main sources that breaks a rule fails the build loudly.
"""
import importlib.util, json, os, re, shutil, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ver = sys.argv[1]
vdir = os.path.join(ROOT, 'versions', ver)
out = os.path.join(vdir, 'build-src')
shutil.rmtree(out, ignore_errors=True)

src = os.path.join(ROOT, 'src', 'main')
def skip(rel):
    return 'krolasyon/bosses' in rel or 'assets/krolasyonbosses' in rel
for d, _, files in os.walk(src):
    for f in files:
        p = os.path.join(d, f)
        rel = os.path.relpath(p, src)
        if skip(rel.replace(os.sep, '/')): continue
        q = os.path.join(out, rel)
        os.makedirs(os.path.dirname(q), exist_ok=True)
        shutil.copy2(p, q)

spec = importlib.util.spec_from_file_location('rules', os.path.join(vdir, 'rules.py'))
rules = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rules)

JAVA = os.path.join(out, 'java', 'com', 'krolasyon', 'beyblade')

def java_files():
    for d, _, files in os.walk(JAVA):
        for f in files:
            if f.endswith('.java'): yield os.path.join(d, f)

failed = []
# global regex rules: (pattern, replacement, min total matches)
for pat, rep, need in getattr(rules, 'GLOBAL', []):
    total = 0
    for p in java_files():
        s = open(p, encoding='utf-8').read()
        s2, n = re.subn(pat, rep, s)
        if n:
            total += n
            open(p, 'w', encoding='utf-8').write(s2)
    if total < need: failed.append('global %r matched %d < %d' % (pat, total, need))
# per file literal rules: file -> [(old, new), ...]
for rel, subs in getattr(rules, 'FILES', {}).items():
    p = os.path.join(JAVA, rel)
    s = open(p, encoding='utf-8').read()
    for old, new in subs:
        if old not in s: failed.append('%s: %r not found' % (rel, old[:80]))
        s = s.replace(old, new)
    open(p, 'w', encoding='utf-8').write(s)
# resource moves (old dir -> new dir under resources/)
res = os.path.join(out, 'resources')
for a, b in getattr(rules, 'MOVES', []):
    pa, pb = os.path.join(res, a), os.path.join(res, b)
    if os.path.isdir(pa):
        os.makedirs(os.path.dirname(pb), exist_ok=True)
        shutil.move(pa, pb)
if hasattr(rules, 'fix_resources'): rules.fix_resources(res)

ov = os.path.join(vdir, 'overlay')
for d, _, files in os.walk(ov):
    for f in files:
        p = os.path.join(d, f)
        q = os.path.join(out, os.path.relpath(p, ov))
        os.makedirs(os.path.dirname(q), exist_ok=True)
        shutil.copy2(p, q)

if failed:
    print('\n'.join('PORT RULE FAILED: ' + f for f in failed))
    sys.exit(1)
print('port %s generated in %s' % (ver, out))
