import sys
from PIL import Image
from geo import render
import hatman, paint_hatman, hatman_anim
G = hatman.G; G.pack(); tex = G.paint(paint_hatman.PAINTERS)
specs = sys.argv[1:]  # name:time
yaws = (0, 60, 100)
S = 340
out = Image.new('RGB', (S * len(specs), S * len(yaws)))
for j, sp in enumerate(specs):
    n, t = sp.split(':')
    pose = hatman_anim.A[n].sample(float(t))
    for i, y in enumerate(yaws):
        im, _ = render(G, tex, pose, yaw=y, size=S)
        out.paste(im, (j * S, i * S))
out.save('/tmp/claude-0/anim_prev.png')
