import sys, os, importlib
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from core import render
K = importlib.import_module(sys.argv[1])
K.M.pack(); tex, glow = K.P.run().images()
ims = []
for s in sys.argv[3:]:
    a, t, y, p = s.split(':')
    pose = K.A[a].sample(float(t)) if a != '-' else None
    ims.append(render(K.M, tex, pose, yaw=float(y), pitch=float(p), size=400, extra_glow=glow))
W = Image.new('RGB', (400 * len(ims), 400))
for i, im in enumerate(ims): W.paste(im, (i * 400, 0))
W.save(sys.argv[2])
print(sum(len(b.cubes) for b in K.M.bones.values()), K.M.pack())
