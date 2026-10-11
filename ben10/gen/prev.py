"""preview: python3 prev.py <module> [anim t1 t2 ...] -> scratch/<module>_prev.png (with reference image)"""
import sys, importlib, os
from PIL import Image
from core import render
OUT = os.environ.get('PREV', '/tmp/claude-0/-home-user-krolasyon/6e802da5-944c-561d-9290-78e08616bdbd/scratchpad')
name = sys.argv[1]
mod = importlib.import_module(name)
used = mod.M.pack(); print('tex rows used', used, 'of', mod.M.th)
tex, glow = mod.P.run().images()
tex.save(f'{OUT}/{name}_tex.png'); glow.save(f'{OUT}/{name}_glow.png')
anim = sys.argv[2] if len(sys.argv) > 2 else 'NONE'
ts = [float(x) for x in sys.argv[3:]] or [0]
yaws = (0, 35, 90, 180)
S = 420
fit = getattr(mod, 'FIT', 80)
ref = Image.open(os.path.join(os.path.dirname(__file__), 'ref', getattr(mod, 'REF', name) + '.jpg')).convert('RGB')
ref.thumbnail((S, S))
out = Image.new('RGB', (S * (len(yaws) + 1), S * len(ts)), (60, 64, 70))
for j, t in enumerate(ts):
    pose = mod.A[anim].sample(t) if anim != 'NONE' else {}
    for i, yaw in enumerate(yaws):
        out.paste(render(mod.M, tex, pose, yaw=yaw, size=S, extra_glow=glow, fit=fit), (i * S, j * S))
    out.paste(ref, (len(yaws) * S, j * S))
out.save(f'{OUT}/{name}_prev.png')
print('ok')
