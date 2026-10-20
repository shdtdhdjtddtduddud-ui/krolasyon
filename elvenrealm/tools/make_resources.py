#!/usr/bin/env python3
"""Gümüş Yaprak pusula kareleri (32 yön), eşya modelleri, dil dosyaları ve veri JSON'ları."""
import json, math, os
from PIL import Image, ImageDraw

R = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(R, 'assets', 'elvenrealm')
D = os.path.join(R, 'data')

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        if isinstance(obj, str): f.write(obj)
        else: json.dump(obj, f, ensure_ascii=False, indent=1)

# --- Gümüş Yaprak: yaprak ucu hedefi gösterir. Kare i, yukarıdan saat yönünde (i-16)*11.25 derece.
def leaf(angle):
    S = 64
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    pts_l, pts_r = [], []
    for k in range(21):
        t = k / 20
        y = -26 + t * 46
        wd = math.sin(t * math.pi) ** 0.8 * 11 * (1 - 0.25 * t)
        pts_l.append((-wd, y)); pts_r.append((wd, y))
    poly = pts_l + pts_r[::-1]
    ca, sa = math.cos(angle), math.sin(angle)
    tr = lambda p: (32 + p[0] * ca - p[1] * sa, 32 + p[0] * sa + p[1] * ca)
    d = ImageDraw.Draw(im)
    d.polygon([tr(p) for p in poly], fill=(196, 204, 214, 255), outline=(98, 110, 128, 255))
    # orta damar ve yan damarlar
    d.line([tr((0, -24)), tr((0, 28))], fill=(120, 132, 150, 255), width=3)
    for y in (-14, -4, 6):
        d.line([tr((0, y + 6)), tr((-7, y))], fill=(150, 160, 176, 255), width=2)
        d.line([tr((0, y + 6)), tr((7, y))], fill=(150, 160, 176, 255), width=2)
    # parlak kenar ve uçtaki zümrüt
    d.ellipse([tr((0, -22))[0] - 4, tr((0, -22))[1] - 4, tr((0, -22))[0] + 4, tr((0, -22))[1] + 4], fill=(70, 220, 120, 255))
    return im.resize((16, 16), Image.LANCZOS)

os.makedirs(os.path.join(A, 'textures', 'item'), exist_ok=True)
for i in range(32):
    leaf((i - 16) * math.pi * 2 / 32).save(os.path.join(A, 'textures', 'item', 'silver_leaf_%02d.png' % i))
    w(os.path.join(A, 'models', 'item', 'silver_leaf_%02d.json' % i), {"parent": "minecraft:item/generated", "textures": {"layer0": "elvenrealm:item/silver_leaf_%02d" % i}})
ov = [{"predicate": {"angle": 0.0}, "model": "elvenrealm:item/silver_leaf_16"}]
for k in range(1, 32):
    ov.append({"predicate": {"angle": (k - 0.5) / 32}, "model": "elvenrealm:item/silver_leaf_%02d" % ((16 + k) % 32)})
ov.append({"predicate": {"angle": 0.984375}, "model": "elvenrealm:item/silver_leaf_16"})
w(os.path.join(A, 'models', 'item', 'silver_leaf.json'), {"parent": "minecraft:item/generated", "textures": {"layer0": "elvenrealm:item/silver_leaf_16"}, "overrides": ov})

kinds = [("lirael", "Lirael", "Lirael"), ("vaelor", "Vaelor", "Vaelor"), ("caelan", "Caelan", "Caelan"), ("nimrae", "Nimrae", "Nimrae"),
         ("tavi", "Tavi", "Tavi"), ("thalion", "Thalion", "Thalion"), ("guard", "Sylvaren Muhafızı", "Sylvaren Guard")]
tr, en = {}, {}
for k, t, e in kinds:
    w(os.path.join(A, 'models', 'item', 'elf_%s_spawn_egg.json' % k), {"parent": "minecraft:item/template_spawn_egg"})
    tr["entity.elvenrealm.elf_" + k] = t; en["entity.elvenrealm.elf_" + k] = e
    tr["item.elvenrealm.elf_%s_spawn_egg" % k] = t + " Çağırma Yumurtası"; en["item.elvenrealm.elf_%s_spawn_egg" % k] = e + " Spawn Egg"
tr.update({
 "item.elvenrealm.silver_leaf": "Gümüş Yaprak",
 "tooltip.elvenrealm.compass": "Ucu en yakın Sylvaren elf kentini gösterir (sağ tık: yeniden ara)",
 "tooltip.elvenrealm.compass_target": "Sylvaren kapısı: %s, %s (%s blok)",
 "tooltip.elvenrealm.compass_none": "Yakında bir elf kenti hissedilmiyor",
 "title.elvenrealm.city": "Sylvaren",
 "title.elvenrealm.sub": "Kadim ağaçların gölgesindeki elf kenti",
 "msg.elvenrealm.fallen": "%s ağır yaralandı ve iyileşmek için Sylvaren'e çekildi. Dostluğunuz zedelendi (-2).",
 "msg.elvenrealm.hire_soon": "%s ile sözleşmenin bitmesine az kaldı.",
})
en.update({
 "item.elvenrealm.silver_leaf": "Silver Leaf",
 "tooltip.elvenrealm.compass": "Its tip points to the nearest elven city of Sylvaren (right click: search again)",
 "tooltip.elvenrealm.compass_target": "Gate of Sylvaren: %s, %s (%s blocks)",
 "tooltip.elvenrealm.compass_none": "No elven city can be sensed nearby",
 "title.elvenrealm.city": "Sylvaren",
 "title.elvenrealm.sub": "The elven city beneath the ancient trees",
 "msg.elvenrealm.fallen": "%s was gravely wounded and withdrew to Sylvaren to heal. Your friendship suffered (-2).",
 "msg.elvenrealm.hire_soon": "Your contract with %s is about to end.",
})
w(os.path.join(A, 'lang', 'tr_tr.json'), tr)
w(os.path.join(A, 'lang', 'en_us.json'), en)

E = os.path.join(D, 'elvenrealm')
w(os.path.join(E, 'worldgen', 'configured_feature', 'elven_city.json'), {"type": "elvenrealm:elven_city", "config": {}})
w(os.path.join(E, 'worldgen', 'placed_feature', 'elven_city.json'), {"feature": "elvenrealm:elven_city", "placement": []})
w(os.path.join(E, 'forge', 'biome_modifier', 'add_elven_city.json'), {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
  "features": "elvenrealm:elven_city", "step": "top_layer_modification"})
w(os.path.join(E, 'tags', 'worldgen', 'biome', 'has_elven_city.json'), {"replace": False, "values": [
  "minecraft:forest", "minecraft:birch_forest", "minecraft:flower_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest",
  "minecraft:meadow", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:cherry_grove"]})
w(os.path.join(E, 'recipes', 'silver_leaf.json'), {"type": "minecraft:crafting_shaped", "category": "misc",
  "pattern": [" E ", "FCF", " S "], "key": {"E": {"item": "minecraft:emerald"}, "F": {"item": "minecraft:feather"},
  "C": {"item": "minecraft:compass"}, "S": {"tag": "minecraft:saplings"}}, "result": {"item": "elvenrealm:silver_leaf", "count": 1}})
w(os.path.join(R, 'pack.mcmeta'), {"pack": {"description": "Krolasyon Elven Realm resources", "pack_format": 15}})
w(os.path.join(R, 'META-INF', 'mods.toml'), '''modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"

[[mods]]
modId="elvenrealm"
version="1.0.0"
displayName="Krolasyon Elf Diyarı: Sylvaren"
authors="krolasyon"
description=\'\'\'
İki kadim dev ağacın gölgesinde, mavi camlı kuleleri ve turuncu külahlarıyla Sylvaren elf kenti.
Altı eşsiz elf (Lirael, Vaelor, Caelan, Nimrae, Tavi, Thalion) ve kent muhafızları: kendi kişilikleri ve konuşma tarzlarıyla
diyaloglar, görevler, hediyeler, dostluk ve zorlu paralı asker sözleşmeleri.
\'\'\'

[[dependencies.elvenrealm]]
    modId="forge"
    mandatory=true
    versionRange="[47,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.elvenrealm]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.21)"
    ordering="NONE"
    side="BOTH"
''')
print("ok")
