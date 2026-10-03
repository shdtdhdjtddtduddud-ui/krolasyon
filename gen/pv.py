"""Preview a mob: python3 pv.py cinder_imp [anim] [t1 t2 ...]   -> /tmp/claude-0/pv_<id>.png (also dumps the texture)"""
import sys, os, importlib
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from core import render

OUT = '/tmp/claude-0/pv'
os.makedirs(OUT, exist_ok=True)
mid = sys.argv[1]
mod = importlib.import_module('mobs.' + mid)
M, P, A = mod.build()
used = M.pack()
tex, glow = P.run().images()
tex.save(f'{OUT}/{mid}_tex.png'); glow.save(f'{OUT}/{mid}_glow.png')
print('tex', M.tw, M.th, 'rows used', used, 'bones', len(M.bones), 'cubes', sum(len(b.cubes) for b in M.bones.values()), 'anims', list(A))
anim = sys.argv[2] if len(sys.argv) > 2 else 'NONE'
ts = [float(x) for x in sys.argv[3:]] or [0]
yaws = (-35, 35, 90, 180) if anim == 'NONE' else (-35, 55, 150)
S = 460
out = Image.new('RGB', (S * len(yaws), S * len(ts)))
for j, t in enumerate(ts):
    pose = A[anim].sample(t) if anim != 'NONE' else {}
    for i, yaw in enumerate(yaws):
        out.paste(render(M, tex, pose, yaw=yaw, size=S, extra_glow=glow), (i * S, j * S))
out.save(f'{OUT}/{mid}_prev.png')
print('saved', f'{OUT}/{mid}_prev.png')
