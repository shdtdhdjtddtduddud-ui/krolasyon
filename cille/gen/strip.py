import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from core import render
import kabkaba as K
K.M.pack(); tex, glow = K.P.run().images()
specs = [s.split(':') for s in sys.argv[2:]]
W = Image.new('RGB', (300 * len(specs), 300))
for i, (a, t) in enumerate(specs):
    W.paste(render(K.M, tex, K.A[a].sample(float(t)), yaw=60, pitch=5, size=300, extra_glow=glow), (i * 300, 0))
W.save(sys.argv[1])
