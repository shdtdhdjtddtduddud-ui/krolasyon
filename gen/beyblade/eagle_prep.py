"""The Earth Eagle photo is slightly angled, so the tall face bolt sits ~18 px off the disc centre (parallax).
Shift the inner region smoothly so the bolt is concentric with the wheel; writes eagle_source_fixed.png."""
import os
import numpy as np
from PIL import Image
from scipy import ndimage as nd

HERE = os.path.dirname(os.path.abspath(__file__))
im = np.asarray(Image.open(os.path.join(HERE, 'eagle_source.png')).convert('RGB')).astype(np.float32)
H, W, _ = im.shape
disc = np.array([240.5, 251.0])     # silhouette bbox centre
bolt = np.array([222.0, 243.0])     # face bolt centre in the photo
off = disc - bolt
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
r = np.hypot(xx - disc[0], yy - disc[1])
t = np.clip((r - 95) / 75, 0, 1)
w = 1 - t * t * (3 - 2 * t)            # 1 near the centre, 0 beyond ~170 px
sx = xx - off[0] * w
sy = yy - off[1] * w
out = np.dstack([nd.map_coordinates(im[..., c], [sy, sx], order=1, mode='nearest') for c in range(3)])
Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).save(os.path.join(HERE, 'eagle_source_fixed.png'))
print('ok')
