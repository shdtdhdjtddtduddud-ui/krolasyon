"""Compiles and links every program of the shader pack on the local GL driver (run under xvfb-run)."""
import os, re, sys, glob
import moderngl

ROOT = os.path.join(os.path.dirname(__file__), '..', 'shaders')


def load(path, seen=None):
    seen = seen or set()
    out = []
    for line in open(path, encoding='utf-8').read().split('\n'):
        m = re.match(r'\s*#include\s+"(.+)"', line)
        if m:
            inc = os.path.join(ROOT, m.group(1).lstrip('/'))
            if inc not in seen:
                seen.add(inc)
                out.append(load(inc, seen))
            continue
        out.append(line)
    return '\n'.join(out)


ctx = moderngl.create_standalone_context(require=330)
fails = 0
for vsh in sorted(glob.glob(os.path.join(ROOT, '*.vsh'))):
    name = os.path.basename(vsh)[:-4]
    fsh = vsh[:-4] + '.fsh'
    if not os.path.exists(fsh):
        print('missing fsh for', name); fails += 1; continue
    try:
        prog = ctx.program(vertex_shader=load(vsh), fragment_shader=load(fsh))
        print(f'OK   {name}')
    except Exception as e:
        fails += 1
        print(f'FAIL {name}\n{e}')
print('failures:', fails)
sys.exit(1 if fails else 0)
