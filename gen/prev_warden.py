import sys
from PIL import Image
import warden as W
from core import render
used = W.M.pack(); print('tex rows used', used)
tex, glow = W.P.run().images()
tex.save('/tmp/claude-0/w_tex.png'); glow.save('/tmp/claude-0/w_glow.png')
anim = sys.argv[1] if len(sys.argv) > 1 else 'IDLE'
ts = [float(x) for x in sys.argv[2:]] or [0]
imgs = []
for t in ts:
    pose = W.A[anim].sample(t) if anim != 'NONE' else {}
    for yaw in (0, 35, 90):
        imgs.append(render(W.M, tex, pose, yaw=yaw, size=420, extra_glow=glow))
out = Image.new('RGB', (420 * 3, 420 * len(ts)))
for i, im in enumerate(imgs):
    out.paste(im, ((i % 3) * 420, (i // 3) * 420))
out.save('/tmp/claude-0/w_prev.png')
