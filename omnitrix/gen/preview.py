import sys
from PIL import Image
import meshlib
names = sys.argv[2:]
out = sys.argv[1]
rows = []
for n in names:
    v, f, uv, tex = meshlib.load(n)
    tiles = []
    for view in ('front', 'side', 'back'):
        im, cam = meshlib.render(v, f, uv, tex, view)
        tiles.append(meshlib.grid(im, view, cam))
    row = Image.new('RGB', (sum(t.width for t in tiles), tiles[0].height))
    x = 0
    for t in tiles:
        row.paste(t, (x, 0)); x += t.width
    rows.append(row)
S = Image.new('RGB', (rows[0].width, sum(r.height for r in rows)))
y = 0
for r in rows:
    S.paste(r, (0, y)); y += r.height
S.save(out)
