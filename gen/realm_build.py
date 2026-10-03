"""Generates every Crimson Realm resource: textures, block/item models, worldgen, recipes, loot, tags, lang."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, 'realm'))
sys.path.insert(0, HERE)
import blocks_tex, items_tex, worldgen, assets, lang  # noqa: E402

ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources')
M = 'krolasyonbosses'


def write(rel, obj):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)


def save(rel, image):
    p = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    image.save(p)


def main():
    try:
        import mobs
        mob_specs = mobs.SPECS
    except ImportError:
        mob_specs = []
    # textures
    for name, im in blocks_tex.build().items():
        save(f'assets/{M}/textures/block/{name}.png', im)
    save(f'assets/{M}/textures/block/realm_portal.png', blocks_tex.portal())
    write(f'assets/{M}/textures/block/realm_portal.png.mcmeta', {"animation": {"frametime": 2, "interpolate": True}})
    save(f'assets/{M}/textures/environment/eclipse.png', blocks_tex.eclipse())
    save(f'assets/{M}/textures/environment/shattered_moon.png', blocks_tex.shattered_moon())
    for name, c in items_tex.build().items():
        save(f'assets/{M}/textures/item/{name}.png', c.image())
    # models, loot, recipes, tags
    eggs = [s.id for s in mob_specs]
    for rel, obj in assets.files(eggs).items():
        write(rel, obj)
    # worldgen
    D = f'data/{M}'
    write(f'{D}/dimension_type/crimson_realm.json', worldgen.dimension_type())
    write(f'{D}/dimension/crimson_realm.json', worldgen.dimension())
    write(f'{D}/worldgen/noise_settings/crimson_realm.json', worldgen.noise_settings())
    for k, v in worldgen.NOISES.items():
        write(f'{D}/worldgen/noise/{k}.json', v)
    for k, v in worldgen.CONFIGURED.items():
        write(f'{D}/worldgen/configured_feature/{k}.json', v)
    for k, v in worldgen.PLACED.items():
        write(f'{D}/worldgen/placed_feature/{k}.json', {"feature": f"{M}:{k}", "placement": v})
    spawns = {b: {} for b, _, _ in worldgen.BIOMES}
    for s in mob_specs:
        for b, w, lo, hi in s.spawns:
            spawns[b].setdefault(s.category, []).append({"type": f"{M}:{s.id}", "weight": w, "minCount": lo, "maxCount": hi})
    for b, _, _ in worldgen.BIOMES:
        write(f'{D}/worldgen/biome/{b}.json', worldgen.biome(b, spawns[b]))
    write(f'{D}/forge/biome_modifier/portal_ruins.json', {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
                                                           "features": f"{M}:portal_ruin", "step": "surface_structures"})
    # lang (merged into the existing files)
    entries = dict(lang.L)
    for s in mob_specs:
        entries.update(s.lang())
    for code, idx in (('tr_tr', 0), ('en_us', 1)):
        p = os.path.join(ROOT, f'assets/{M}/lang/{code}.json')
        cur = {}
        if os.path.exists(p):
            with open(p, encoding='utf-8') as f:
                cur = json.load(f)
        for k, v in entries.items():
            cur[k] = v[idx]
        write(f'assets/{M}/lang/{code}.json', cur)
    if mob_specs:
        mobs.write_all(ROOT)
    print('realm resources generated')


if __name__ == '__main__':
    main()
