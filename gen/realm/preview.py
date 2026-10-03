"""renders contact sheets of the realm creatures with the software renderer"""
import sys, os
sys.path.insert(0, os.path.dirname(__file__)); sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
from PIL import Image, ImageDraw
import mobs
from core import render
from paint import Painter

OUT = sys.argv[1]
names = sys.argv[2].split(',') if len(sys.argv) > 2 and sys.argv[2] else [s.id for s in mobs.SPECS]
slot = int(sys.argv[3]) if len(sys.argv) > 3 else -1
t = float(sys.argv[4]) if len(sys.argv) > 4 else 0.3
yaw = float(sys.argv[5]) if len(sys.argv) > 5 else 30
S = 300
cols = min(5, len(names))
rows = (len(names) + cols - 1) // cols
sheet = Image.new('RGB', (cols * S, rows * S), (40, 40, 48))
d = ImageDraw.Draw(sheet)
for i, n in enumerate(names):
    s = next(x for x in mobs.SPECS if x.id == n)
    M, A = s.build_fn()
    mobs.tex_size(M)
    P = Painter(M)
    P.mats.update(mobs.MAT_REGISTRY[n])
    tex, glow = P.run().images()
    pose = A[slot].sample(t * A[slot].length) if slot >= 0 and A.get(slot) else None
    im = render(M, tex, pose=pose, yaw=yaw, pitch=8, size=S, extra_glow=glow)
    sheet.paste(im, ((i % cols) * S, (i // cols) * S))
    d.text(((i % cols) * S + 6, (i // cols) * S + 4), n, fill=(255, 255, 255))
sheet.save(OUT)
