"""Variares photo (360 px): upscale x1.5, centre the disc on the canvas -> variares_source_fixed.png (540^2)."""
import os
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage as nd

HERE = os.path.dirname(os.path.abspath(__file__))
im = Image.open(os.path.join(HERE, 'variares_source.jpg')).convert('RGB')
k = 1.5
big = im.resize((int(im.width * k), int(im.height * k)), Image.LANCZOS).filter(ImageFilter.UnsharpMask(2, 60, 2))
a = np.asarray(big).astype(np.float32)
white = (a.min(2) > 232) & ((a.max(2) - a.min(2)) < 22)
lab, _ = nd.label(white)
bg = {lab[0, 0], lab[0, -1], lab[-1, 0], lab[-1, -1]} - {0}
fg = nd.binary_fill_holes(~np.isin(lab, list(bg)))
ys, xs = np.nonzero(fg)
cx, cy = (xs.min() + xs.max()) / 2, (ys.min() + ys.max()) / 2
print('disc centre', cx, cy, 'radius', (xs.max() - xs.min()) / 2, (ys.max() - ys.min()) / 2)
N = 540
canvas = Image.new('RGB', (N, N), (255, 255, 255))
canvas.paste(big, (int(round(N / 2 - cx)), int(round(N / 2 - cy))))
canvas.save(os.path.join(HERE, 'variares_source_fixed.png'))
