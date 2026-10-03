"""python3 contact.py id1 id2 ... -> /tmp/claude-0/pv/contact.png : one cell per mob (yaw 35 and yaw 180); optional  id:anim:t"""
import sys, os, importlib
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from core import render
OUT = '/tmp/claude-0/pv'
os.makedirs(OUT, exist_ok=True)
items = sys.argv[1:]
S = 420
sheet = Image.new('RGB', (S * 2, S * len(items)), (60, 64, 70))
for j, it in enumerate(items):
    parts = it.split(':')
    mid = parts[0]
    mod = importlib.import_module('mobs.' + mid)
    M, P, A = mod.build()
    M.pack()
    tex, glow = P.run().images()
    pose = A[parts[1]].sample(float(parts[2])) if len(parts) > 2 else {}
    for i, yaw in enumerate((-35, 150)):
        sheet.paste(render(M, tex, pose, yaw=yaw, size=S, extra_glow=glow), (i * S, j * S))
    print(mid, 'tex', M.tw, M.th, 'rows', M.pack(), 'bones', len(M.bones))
sheet.save(f'{OUT}/contact.png')
print('saved')
