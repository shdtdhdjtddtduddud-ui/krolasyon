import sys, importlib
from PIL import Image
from core import render
mod = importlib.import_module(sys.argv[1])
used = mod.M.pack(); print('tex rows used', used)
tex, glow = mod.P.run().images()
tex.save(f'/tmp/claude-0/{sys.argv[1]}_tex.png'); glow.save(f'/tmp/claude-0/{sys.argv[1]}_glow.png')
anim = sys.argv[2]
ts = [float(x) for x in sys.argv[3:]] or [0]
yaws = (0, 40, 90, 160)
S = 360
out = Image.new('RGB', (S * len(yaws), S * len(ts)))
for j, t in enumerate(ts):
    pose = mod.A[anim].sample(t) if anim != 'NONE' else {}
    for i, yaw in enumerate(yaws):
        out.paste(render(mod.M, tex, pose, yaw=yaw, size=S, extra_glow=glow), (i * S, j * S))
out.save(f'/tmp/claude-0/{sys.argv[1]}_prev.png')
