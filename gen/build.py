"""Regenerates every asset of the mod. Run: python3 gen/build.py"""
import os, sys, json, time
sys.path.insert(0, os.path.dirname(__file__))
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main')
RES = os.path.join(ROOT, 'resources', 'assets', 'sololeveling')
DATA = os.path.join(ROOT, 'resources', 'data', 'sololeveling')
JAVA = os.path.join(ROOT, 'java', 'com', 'krolasyon', 'sololeveling')


def step(name, fn):
    t = time.time()
    fn()
    print(f'{name:10s} {time.time() - t:5.1f}s')


def eggs():
    import mobs
    for name in mobs.MODELS:
        p = os.path.join(RES, 'models', 'item', name + '_spawn_egg.json')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, 'w') as f:
            json.dump({'parent': 'minecraft:item/template_spawn_egg'}, f)


if __name__ == '__main__':
    import build_mobs, build_items, armor, blocks, gui, npcs, sounds, lang, data
    step('mobs', build_mobs.run)
    step('items', build_items.run)
    step('armor', lambda: armor.run(RES, os.path.join(JAVA, 'client', 'render', 'ArmorModels.java')))
    step('blocks', lambda: blocks.run(RES, DATA, None))
    step('gui', lambda: gui.run(RES))
    step('npcs', lambda: npcs.run(RES))
    step('sounds', lambda: sounds.run(RES))
    step('lang', lambda: lang.run(RES))
    step('data', lambda: data.run(DATA))
    step('eggs', eggs)
