import sys, os
import tex as T, mobs, preview
names = sys.argv[2:] or list(mobs.MODELS)
out = sys.argv[1]
items = []
for n in names:
    m = mobs.MODELS[n]
    m.pack()
    items.append((n, m, T.load(f'../src/main/resources/assets/sololeveling/textures/entity/{n}.png')))
preview.sheet(items, out, cols=2, size=280)
