import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from fmodels import *
from core import render
from PIL import Image
SP = '/tmp/claude-0/-home-user-krolasyon/bd0e7efe-b3eb-5778-9553-eceee0bfa652/scratchpad/'

def build(name):
    m, _ = BUILDERS[name]()
    used = m.pack()
    P = make_painter(m)
    board_painter(P, False)
    tex, glow = P.run().images()
    return m, tex, used

if __name__ == '__main__':
    names = sys.argv[1:] or list(BUILDERS)
    tiles = []
    for n in names:
        m, tex, used = build(n)
        print(n, 'tex rows used', used, 'of', m.th)
        tiles.append(render(m, tex, yaw=-35, pitch=-28, size=520, bg=(59, 23, 37)))
        tex.save(SP + f'tex_{n}.png')
    cols = min(3, len(tiles))
    sheet = Image.new('RGB', (520 * cols, 520 * ((len(tiles) + 2) // 3)))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 3) * 520, (i // 3) * 520))
    sheet.save(SP + 'sheet.png')
