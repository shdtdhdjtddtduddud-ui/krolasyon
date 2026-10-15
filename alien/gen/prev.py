import sys, importlib, os
from PIL import Image
from core import render
OUT = sys.argv[1]
mod = importlib.import_module('xeno')
print('rows used', mod.M.pack())
tex, glow = mod.P.run().images()
anim = sys.argv[2]
ts = [float(x) for x in sys.argv[3:]] or [0]
yaws = (0, 45, 90, 180)
S = 360
out = Image.new('RGB', (S * len(yaws), S * len(ts)))
for j, t in enumerate(ts):
    pose = mod.A[anim].sample(t) if anim != 'NONE' else {}
    for i, yaw in enumerate(yaws):
        out.paste(render(mod.M, tex, pose, yaw=yaw, size=S), (i * S, j * S))
out.save(OUT)
tex.save(OUT.replace('.png', '_tex.png'))
