"""Fusion wheels of the parts system that are not beyblades of their own, from the top views in the user's photos:
Big Bang (bigbang_ref.jpg), Phoenix (phoenix_ref.jpg) and Inferno (inferno_ref.jpg).

For each: crop the top view, scale it so the disc radius is 250 px on a 540 px canvas (<name>_source_fixed.png), then
build the top mesh exactly like beffal_model.py but without a track / tip lathe (those are drawn from the fitted parts).
"""
import os, re
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage as nd

HERE = os.path.dirname(os.path.abspath(__file__))
# name: (photo, centre x, centre y, disc radius in photo px, backdrop brightness, disc cut-off radius)
WHEELS = {
    'bigbang': ('bigbang_ref.jpg', 332, 172, 104, 205, 0.99),
    'phoenix': ('phoenix_ref.jpg', 323, 229, 92, 236, 1.03),
    'inferno': ('inferno_ref.jpg', 323, 234, 79, 212, 1.04),
}
N, R_OUT = 540, 250


def prep(name, photo, cx, cy, r, light, cut):
    im = Image.open(os.path.join(HERE, photo)).convert('RGB')
    k = R_OUT / r
    big = im.resize((int(im.width * k), int(im.height * k)), Image.LANCZOS).filter(ImageFilter.UnsharpMask(2, 70, 2))
    a = np.asarray(big).astype(np.float32)
    ccx, ccy = cx * k, cy * k
    yy, xx = np.mgrid[0:a.shape[0], 0:a.shape[1]]
    d = np.hypot(xx - ccx, yy - ccy)
    # background: light grey studio backdrop, and everything outside the disc (reflections, the side view)
    bg = ((a.min(2) > light) & ((a.max(2) - a.min(2)) < 26)) | (d > R_OUT * cut)
    lab, _ = nd.label(bg)
    keep = np.isin(lab, np.unique(lab[d > R_OUT * cut]))
    a[keep] = 255
    out = Image.new('RGB', (N, N), (255, 255, 255))
    out.paste(Image.fromarray(a.astype(np.uint8)), (int(round(N / 2 - ccx)), int(round(N / 2 - ccy))))
    out.save(os.path.join(HERE, name + '_source_fixed.png'))


# the face bolt (rotation axis) is not at the disc centre in the slightly tilted Big Bang photo
AXIS = {'bigbang': (232.0, 238.0)}


def build(name):
    ax, ay = AXIS.get(name, (270.0, 270.0))
    s = open(os.path.join(HERE, 'beffal_model.py')).read()
    s = s.replace("'__main__'", "'__never__'")
    s = s.replace('beffal', name)
    s = re.sub(r"CX, CY = .*\n", "CX, CY = %.1f, %.1f\n" % (ax, ay), s)
    s = re.sub(r"RPX = .*\n", "RPX = 250.0\n", s)
    s = re.sub(r"S = 364.0 .*\n", "S = 262.0\n", s)
    s = re.sub(r"HEX_CX, HEX_CY, HEX_R, HEX_HALF_H = .*\n", "HEX_CX, HEX_CY, HEX_R, HEX_HALF_H = %.1f, %.1f, 62.0, 53.7\n" % (ax, ay), s)
    s = s.replace("    r = 238.0\n    while r < 400:", "    r = 150.0\n    while r < 320:")
    # generic colour classes: saturated clear wheel raised, chrome metal, dark recesses
    s = re.sub(r"    dark = MASK & \(mx < 0.26\).*?return pink, dark, holes, white, gems, grey, emblem",
               """    dark = MASK & (mx < 0.2)
    pink = MASK & ~dark & (sat > 0.35)
    gems = np.zeros_like(MASK)
    holes = np.zeros_like(MASK)
    white = (mn > 0.85) & MASK
    grey = MASK & ~pink & ~dark
    dx, dy = XX - HEX_CX, YY - HEX_CY
    emblem = (np.hypot(dx, dy) < HEX_R * 0.8) & (mx > 0.6)
    return pink, dark, holes, white, gems, grey, emblem""", s, flags=re.S)
    s = s.replace("    h[YELLOW & (RAD > 0.55)] = 0.30", "    h[YELLOW] = 0.26")
    s = s.replace("h = nd.gaussian_filter(h, 2.2)", "h = nd.gaussian_filter(h, 1.6)")
    s = s.replace("h += nd.gaussian_filter(LUM - 0.55, 1.4) * 0.07", "h += nd.gaussian_filter(LUM - 0.55, 1.0) * 0.07")
    # no track / tip lathe: the fitted parts are drawn by the game
    s = re.sub(r"    prof = \[.*?\n    \]\n", "    prof = []\n", s, flags=re.S)
    s = s.replace("    for k in range(len(prof) - 1):", "    for k in range(max(0, len(prof) - 1)):")
    g = {'__file__': os.path.join(HERE, name + '_model.py'), '__name__': 'wheel'}
    exec(compile(s, name + '_model', 'exec'), g)
    g['textures']()
    mesh = g['build']()
    mesh.write(os.path.join(g['ASSETS'], 'models/%s_mesh.bin' % name))
    print(name, {k: len(v) for k, v in mesh.groups.items()})


if __name__ == '__main__':
    for name, (photo, cx, cy, r, light, cut) in WHEELS.items():
        prep(name, photo, cx, cy, r, light, cut)
        build(name)
