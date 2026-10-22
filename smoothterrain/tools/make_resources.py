"""Yumuşak Arazi kaynakları: blok durumları, modeller, ganimet, tarifler, etiketler, dil, dünya üretimi."""
import json, os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'smoothterrain')
D = os.path.join(ROOT, 'data', 'smoothterrain')


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        if isinstance(obj, str):
            f.write(obj)
        else:
            json.dump(obj, f, indent=1, ensure_ascii=False)


# id, kaynak blok, doku (yan/üst/alt), Türkçe, İngilizce
SLABS = [
    ('dirt_slab', 'dirt', ('dirt', 'dirt', 'dirt'), 'Toprak Yarım Blok', 'Dirt Slab'),
    ('coarse_dirt_slab', 'coarse_dirt', ('coarse_dirt',) * 3, 'İri Taneli Toprak Yarım Blok', 'Coarse Dirt Slab'),
    ('podzol_slab', 'podzol', ('podzol_side', 'podzol_top', 'dirt'), 'Podzol Yarım Blok', 'Podzol Slab'),
    ('sand_slab', 'sand', ('sand',) * 3, 'Kum Yarım Blok', 'Sand Slab'),
    ('red_sand_slab', 'red_sand', ('red_sand',) * 3, 'Kızıl Kum Yarım Blok', 'Red Sand Slab'),
    ('gravel_slab', 'gravel', ('gravel',) * 3, 'Çakıl Yarım Blok', 'Gravel Slab'),
]
tr, en = {}, {}
for sid, src, (side, top, bottom), t, e in SLABS:
    tx = {"side": "minecraft:block/" + side, "top": "minecraft:block/" + top, "bottom": "minecraft:block/" + bottom}
    w(os.path.join(A, 'models', 'block', sid + '.json'), {"parent": "minecraft:block/slab", "textures": tx})
    w(os.path.join(A, 'models', 'block', sid + '_top.json'), {"parent": "minecraft:block/slab_top", "textures": tx})
    w(os.path.join(A, 'blockstates', sid + '.json'), {"variants": {
        "type=bottom": {"model": "smoothterrain:block/" + sid}, "type=top": {"model": "smoothterrain:block/" + sid + "_top"},
        "type=double": {"model": "minecraft:block/" + src}}})
    tr["block.smoothterrain." + sid] = t
    en["block.smoothterrain." + sid] = e

# çimen yarım bloğu: üstü ve yan örtüsü biyom rengiyle boyanır; yan dokunun üst yarısı (çimen saçağı) görünür
def grass(y0, y1):
    side = {"texture": "#side", "uv": [0, 0, 16, y1 - y0]}
    over = {"texture": "#overlay", "uv": [0, 0, 16, y1 - y0], "tintindex": 0}
    return {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout_mipped",
        "textures": {"particle": "minecraft:block/dirt", "bottom": "minecraft:block/dirt", "top": "minecraft:block/grass_block_top",
                     "side": "minecraft:block/grass_block_side", "overlay": "minecraft:block/grass_block_side_overlay"},
        "elements": [
            {"from": [0, y0, 0], "to": [16, y1, 16], "faces": {
                "down": {"texture": "#bottom", "cullface": "down"} if y0 == 0 else {"texture": "#bottom"},
                "up": {"texture": "#top", "tintindex": 0, "cullface": "up"} if y1 == 16 else {"texture": "#top", "tintindex": 0},
                "north": dict(side, cullface="north"), "south": dict(side, cullface="south"),
                "west": dict(side, cullface="west"), "east": dict(side, cullface="east")}},
            {"from": [0, y0, 0], "to": [16, y1, 16], "faces": {
                "north": dict(over, cullface="north"), "south": dict(over, cullface="south"),
                "west": dict(over, cullface="west"), "east": dict(over, cullface="east")}},
        ]}


w(os.path.join(A, 'models', 'block', 'grass_slab.json'), grass(0, 8))
w(os.path.join(A, 'models', 'block', 'grass_slab_top.json'), grass(8, 16))
w(os.path.join(A, 'blockstates', 'grass_slab.json'), {"variants": {
    "type=bottom": {"model": "smoothterrain:block/grass_slab"}, "type=top": {"model": "smoothterrain:block/grass_slab_top"},
    "type=double": {"model": "minecraft:block/grass_block"}}})
tr["block.smoothterrain.grass_slab"] = "Çimen Yarım Blok"
en["block.smoothterrain.grass_slab"] = "Grass Slab"

ALL = [s[0] for s in SLABS] + ['grass_slab']
SRC = {s[0]: s[1] for s in SLABS}
SRC['grass_slab'] = 'grass_block'
for sid in ALL:
    w(os.path.join(A, 'models', 'item', sid + '.json'), {"parent": "smoothterrain:block/" + sid})
    w(os.path.join(D, 'loot_tables', 'blocks', sid + '.json'), {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
        "type": "minecraft:item", "name": "smoothterrain:" + sid, "functions": [
            {"function": "minecraft:set_count", "count": 2, "add": False,
             "conditions": [{"condition": "minecraft:block_state_property", "block": "smoothterrain:" + sid, "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    w(os.path.join(D, 'recipes', sid + '.json'), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["###"],
                                                  "key": {"#": {"item": "minecraft:" + SRC[sid]}}, "result": {"item": "smoothterrain:" + sid, "count": 6}})
    w(os.path.join(D, 'recipes', sid + '_to_block.json'), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["#", "#"],
                                                           "key": {"#": {"item": "smoothterrain:" + sid}}, "result": {"item": "minecraft:" + SRC[sid], "count": 1}})

MC = os.path.join(ROOT, 'data', 'minecraft', 'tags', 'blocks')
w(os.path.join(MC, 'mineable', 'shovel.json'), {"replace": False, "values": ["smoothterrain:" + s for s in ALL]})
w(os.path.join(MC, 'slabs.json'), {"replace": False, "values": ["smoothterrain:" + s for s in ALL]})
w(os.path.join(ROOT, 'data', 'minecraft', 'tags', 'items', 'slabs.json'), {"replace": False, "values": ["smoothterrain:" + s for s in ALL]})
w(os.path.join(A, 'lang', 'tr_tr.json'), tr)
w(os.path.join(A, 'lang', 'en_us.json'), en)

w(os.path.join(D, 'worldgen', 'configured_feature', 'smooth_steps.json'), {"type": "smoothterrain:smooth_steps", "config": {}})
w(os.path.join(D, 'worldgen', 'placed_feature', 'smooth_steps.json'), {"feature": "smoothterrain:smooth_steps", "placement": []})
w(os.path.join(D, 'forge', 'biome_modifier', 'add_smooth_steps.json'), {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
                                                                         "features": "smoothterrain:smooth_steps", "step": "top_layer_modification"})
w(os.path.join(ROOT, 'pack.mcmeta'), {"pack": {"description": "Krolasyon Smooth Terrain resources", "pack_format": 15}})
w(os.path.join(ROOT, 'META-INF', 'mods.toml'), '''modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"

[[mods]]
modId="smoothterrain"
version="1.0.0"
displayName="Krolasyon Yumuşak Arazi"
authors="krolasyon"
description=\'\'\'
Dünya üretilirken bir blokluk her basamağın dibine yüzeye uyan yarım blok konur (çimen, toprak, podzol, kum, kızıl kum,
çakıl, taş, kumtaşı ve yarım kat kar). Tepeler, dağlar ve ovalar daha yumuşak görünür; yokuşlar zıplamadan yürünür.
\'\'\'

[[dependencies.smoothterrain]]
    modId="forge"
    mandatory=true
    versionRange="[47,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.smoothterrain]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.21)"
    ordering="NONE"
    side="BOTH"
''')
print('ok')
