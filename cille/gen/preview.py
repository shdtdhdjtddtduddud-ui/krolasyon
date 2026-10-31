import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from core import render
import kabkaba as K
out = sys.argv[1]
K.M.pack()
tex, glow = K.P.run().images()
tex.save(os.path.join(out, 'tex.png'))
anim = sys.argv[2] if len(sys.argv) > 2 else None
t = float(sys.argv[3]) if len(sys.argv) > 3 else 0
pose = K.A[anim].sample(t) if anim else None
ims = [render(K.M, tex, pose, yaw=y, pitch=p, size=500, extra_glow=glow) for y, p in ((-35, 8), (90, 0), (180, 5), (0, 0))]
W = Image.new('RGB', (1000, 1000))
for i, im in enumerate(ims):
    W.paste(im, ((i % 2) * 500, (i // 2) * 500))
W.save(os.path.join(out, f'prev_{anim or "rest"}_{t}.png'))
