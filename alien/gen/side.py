import sys, importlib
from PIL import Image
from core import render
OUT = sys.argv[1]; anim = sys.argv[2]; yaw = float(sys.argv[3]); ts = [float(x) for x in sys.argv[4:]] or [0]
mod = importlib.import_module('xeno'); mod.M.pack()
tex, _ = mod.P.run().images()
S = 520
out = Image.new('RGB', (S * len(ts), S))
for j, t in enumerate(ts):
    pose = mod.A[anim].sample(t) if anim != 'NONE' else {}
    out.paste(render(mod.M, tex, pose, yaw=yaw, size=S, bg=(150, 155, 160)), (j * S, 0))
out.save(OUT)
