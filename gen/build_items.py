import os, json, time
import numpy as np
import tex as T
import items as I
from itemmodel import HANDHELD, OBJECT

RES = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'sololeveling')
TEX = os.path.join(RES, 'textures', 'item')
MOD = os.path.join(RES, 'models', 'item')


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=1)


def emit(name, im, display, icon_view, render_type=None):
    im.pack_and_paint()
    os.makedirs(os.path.join(TEX, '3d'), exist_ok=True)
    T.save(im.texture, os.path.join(TEX, '3d', name + '.png'))
    write(os.path.join(MOD, '3d', name + '.json'), im.json('sololeveling:item/3d/' + name, display, render_type))
    icon = im.icon(32, *icon_view)
    T.save(icon, os.path.join(TEX, name + '.png'))
    write(os.path.join(MOD, name + '.json'), {
        'loader': 'forge:separate_transforms',
        'gui_light': 'front',
        'base': {'parent': 'sololeveling:item/3d/' + name},
        'perspectives': {'gui': {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'sololeveling:item/' + name}}},
    })
    return icon


def run():
    icons = []
    for name, (fn, disp) in I.WEAPONS.items():
        icons.append((name, emit(name, fn(), disp, (12, 8) if disp is HANDHELD else (-30, 22), 'minecraft:translucent' if name == 'orb_of_avarice' else None)))
    for name, fn in I.POTIONS.items():
        icons.append((name, emit(name, fn(), OBJECT, (-25, 18), 'minecraft:translucent')))
    for name, (fn, disp) in I.MISC.items():
        icons.append((name, emit(name, fn(), disp, (12, 8) if disp is HANDHELD else (-30, 25))))
    for name, fn in I.MATS.items():
        icons.append((name, emit(name, fn(), OBJECT, (-30, 25))))
    return icons


if __name__ == '__main__':
    import sys
    t = time.time()
    icons = run()
    print(len(icons), 'items', round(time.time() - t, 1), 's')
    if len(sys.argv) > 1:
        cols = 10
        big = [T.nearest(i, 96, 96) for _, i in icons]
        bg = T.new(96, 96, T.hexc('#2A3040'))
        tiles = []
        for b in big:
            c = bg.copy(); T.paste(c, b, 0, 0); tiles.append(c)
        while len(tiles) % cols: tiles.append(bg.copy())
        rows = [np.concatenate(tiles[i:i + cols], 1) for i in range(0, len(tiles), cols)]
        T.save(np.concatenate(rows, 0), sys.argv[1])
