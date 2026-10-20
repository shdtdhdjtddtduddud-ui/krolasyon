#!/usr/bin/env python3
"""Eşya modelleri, blok durumları, dil, tarifler, ganimet tabloları, ganimet değiştiricileri ve dünya üretimi JSON'ları."""
import json, os

R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = os.path.join(R, 'assets', 'advgear')
D = os.path.join(R, 'data', 'advgear')


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(obj if isinstance(obj, str) else json.dumps(obj, ensure_ascii=False, indent=1))


ITEMS = {
    # id: (tr adı, en adı, tr açıklama, en açıklama)
    'gold_band': ('Altın Bant', 'Gold Band', 'Şans +1 ve deneyim kürelerinden %30 fazla deneyim', '+1 Luck and 30% more experience from orbs'),
    'sapphire_ring': ('Safir Yüzük', 'Sapphire Ring', 'Suda nefes alma ve yunus çevikliği', 'Water breathing and dolphin\'s grace in water'),
    'skull_ring': ('Kurukafa Yüzüğü', 'Skull Ring', 'Vurduğun hasarın %15\'i kadar can çalar', 'Steals 15% of melee damage dealt as health'),
    'bone_hand_ring': ('Kemik El Yüzüğü', 'Bone Hand Ring', '7 blok içindeki eşyaları ve deneyimi çeker (eğilince durur)',
                       'Pulls items and experience within 7 blocks (sneak to pause)'),
    'emerald_eye_ring': ('Zümrüt Göz Yüzüğü', 'Emerald Eye Ring', 'Zehre bağışıklık ve yavaş iyileşme', 'Poison immunity and slow regeneration'),
    'explorer_goggles': ('Kâşif Gözlüğü', 'Explorer Goggles', 'Kalıcı gece görüşü', 'Permanent night vision'),
    'engineer_goggles': ('Mühendis Gözlüğü', 'Engineer Goggles', 'Eğilince 12 blok içindeki cevherleri duvarların ardından gösterir',
                         'While sneaking, reveals ores within 12 blocks through walls'),
    'hunter_goggles': ('Avcı Gözlüğü', 'Hunter Goggles', '28 blok içindeki düşmanları parlatır', 'Makes hostile creatures within 28 blocks glow'),
    'satchel': ('Heybe', 'Satchel', '18 yuvalı çanta', '18-slot bag'),
    'traveler_pack': ('Gezgin Çantası', 'Traveler\'s Pack', '27 yuvalı sırt çantası', '27-slot backpack'),
    'expedition_pack': ('Sefer Çantası', 'Expedition Pack', '36 yuvalı büyük sırt çantası', '36-slot large backpack'),
    'heart_tonic': ('Kalp Tonik', 'Heart Tonic', 'Anında iyileştirir ve yenilenme verir', 'Heals instantly and grants regeneration'),
    'ocean_draught': ('Derinlik İksiri', 'Ocean Draught', 'Su altı nefesi, yunus çevikliği ve gece görüşü', 'Water breathing, dolphin\'s grace and night vision'),
    'black_death': ('Kara Ölüm', 'Black Death', 'Fırlat: çarptığı yerde zehir, solma ve yavaşlık bulutu bırakır',
                    'Throw: leaves a cloud of poison, wither and slowness'),
    'starlight_elixir': ('Yıldız Işığı İksiri', 'Starlight Elixir', 'Yavaş düşüş, zıplama ve hız', 'Slow falling, jump boost and speed'),
    'sun_essence': ('Güneş Özü', 'Sun Essence', 'Ateşe dayanıklılık, kazma hızı ve tokluk', 'Fire resistance, haste and saturation'),
    'apprentice_spellbook': ('Çırak Büyü Kitabı', 'Apprentice Spellbook', 'Ateş Oku: üç küçük ateş topu', 'Firebolt: three small fireballs'),
    'traveler_tome': ('Gezgin Kitabı', 'Traveler\'s Tome', 'Göz Açıp Kapayıncaya: baktığın yöne 12 blok ışınlanma', 'Blink: teleport up to 12 blocks forward'),
    'black_grimoire': ('Kara Grimuar', 'Black Grimoire', 'Fırtına Çağrısı: baktığın yere yıldırım düşürür', 'Call Storm: strikes lightning where you look'),
    'traveler_belt': ('Gezgin Kemeri', 'Traveler\'s Belt', '%12 daha hızlı yürüme', '12% faster movement'),
    'tool_belt': ('Alet Kemeri', 'Tool Belt', '%25 daha hızlı kazma ve kesme', '25% faster mining'),
    'alchemist_belt': ('Simyacı Kemeri', 'Alchemist\'s Belt', 'Olumlu iksir etkileri %50 uzun sürer', 'Beneficial potion effects last 50% longer'),
    'iron_key': ('Eski Demir Anahtar', 'Old Iron Key', '', ''),
    'dungeon_key': ('Zindan Anahtarı', 'Dungeon Key', '', ''),
    'heart_key': ('Altın Kalp Anahtarı', 'Golden Heart Key', '', ''),
    'mechanic_key': ('Mekanik Anahtar', 'Mechanic Key', 'Tıkladığın bloğu döndürür (yön, eksen, açı)', 'Rotates the clicked block (facing, axis, rotation)'),
    'thorn_lockpick': ('Diken Maymuncuk', 'Thorn Lockpick', 'Kilitli sandıkları şansla açar: demir %45, kemik %30, kalp %15',
                       'Picks locked chests by chance: iron 45%, bone 30%, heart 15%'),
    'crystal_key': ('Kristal Anahtar', 'Crystal Key', 'Shift+sağ tık: bu yeri kilitle. Basılı tut: buraya geri dön (45 sn bekleme)',
                    'Sneak+use: bind this spot. Hold use: return to it (45 s cooldown)'),
    'key_ring': ('Anahtar Halkası', 'Key Ring', 'Anahtarları üstüne bırakıp (sağ tık) taşı; içindekiler kilit açar. Shift+sağ tık: boşalt',
                 'Right-click keys onto it to carry them; they open locks. Sneak+use: empty'),
}
BOOKS = ['apprentice_spellbook', 'traveler_tome', 'black_grimoire']
BAGS = ['satchel', 'traveler_pack', 'expedition_pack']
GOGGLES = ['explorer_goggles', 'engineer_goggles', 'hunter_goggles']

HELD = {"thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]}}
for i in ITEMS:
    gen = {"parent": "minecraft:item/generated", "textures": {"layer0": "advgear:item/" + i}}
    if i in BOOKS or i in BAGS or i in GOGGLES:
        if i in BOOKS:
            disp = {"thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1.5], "scale": [0.55, 0.55, 0.55]},
                    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1.5], "scale": [0.55, 0.55, 0.55]},
                    "firstperson_righthand": {"rotation": [0, 20, 0], "translation": [-1, 3, -2], "scale": [0.6, 0.6, 0.6]},
                    "firstperson_lefthand": {"rotation": [0, -20, 0], "translation": [1, 3, -2], "scale": [0.6, 0.6, 0.6]},
                    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}}
        elif i in BAGS:
            disp = {"thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
                    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
                    "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
                    "firstperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
                    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}}
        else:
            disp = {"thirdperson_righthand": {"rotation": [10, 180, 0], "translation": [0, 3, 1], "scale": [0.5, 0.5, 0.5]},
                    "thirdperson_lefthand": {"rotation": [10, 180, 0], "translation": [0, 3, 1], "scale": [0.5, 0.5, 0.5]},
                    "firstperson_righthand": {"rotation": [0, 160, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                    "firstperson_lefthand": {"rotation": [0, 200, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
                    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}}
        model = {"loader": "forge:separate_transforms", "gui_light": "front",
                 "base": {"parent": "minecraft:builtin/entity", "display": disp},
                 "perspectives": {"gui": gen, "ground": gen, "fixed": gen}}
        w(os.path.join(A, 'models', 'item', i + '.json'), model)
    else:
        w(os.path.join(A, 'models', 'item', i + '.json'), gen if 'key' not in i and 'lockpick' not in i
          else {"parent": "minecraft:item/handheld", "textures": {"layer0": "advgear:item/" + i}})

# kilitli sandık
for lock in ('iron', 'bone', 'heart'):
    w(os.path.join(A, 'models', 'block', 'locked_chest_' + lock + '.json'), {
        "parent": "minecraft:block/block", "textures": {"particle": "advgear:block/locked_chest_side", "side": "advgear:block/locked_chest_side",
                                                       "top": "advgear:block/locked_chest_top", "front": "advgear:block/locked_chest_front",
                                                       "lock": "advgear:block/lock_" + lock},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 14, 15], "faces": {
                "north": {"uv": [1, 2, 15, 16], "texture": "#front"}, "south": {"uv": [1, 2, 15, 16], "texture": "#side"},
                "east": {"uv": [1, 2, 15, 16], "texture": "#side"}, "west": {"uv": [1, 2, 15, 16], "texture": "#side"},
                "up": {"uv": [1, 1, 15, 15], "texture": "#top"}, "down": {"uv": [1, 1, 15, 15], "texture": "#top"}}},
            {"from": [6, 4, 0], "to": [10, 9, 1], "faces": {
                "north": {"uv": [2, 6, 14, 16], "texture": "#lock"}, "east": {"uv": [2, 6, 3, 16], "texture": "#lock"},
                "west": {"uv": [2, 6, 3, 16], "texture": "#lock"}, "down": {"uv": [2, 15, 14, 16], "texture": "#lock"}}},
            {"from": [7, 9, 0.4], "to": [9, 11, 0.6], "faces": {"north": {"uv": [4, 0, 12, 6], "texture": "#lock"},
                                                                "south": {"uv": [4, 0, 12, 6], "texture": "#lock"}}}]})
variants = {}
for lock in ('iron', 'bone', 'heart'):
    for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        variants['facing=%s,lock=%s' % (f, lock)] = {"model": "advgear:block/locked_chest_" + lock, "y": y}
w(os.path.join(A, 'blockstates', 'locked_chest.json'), {"variants": variants})
w(os.path.join(A, 'models', 'item', 'locked_chest.json'), {"parent": "advgear:block/locked_chest_iron"})

# dil
tr = {"itemGroup.advgear": "Maceracı Kuşanımı", "block.advgear.locked_chest": "Kilitli Sandık", "entity.advgear.black_death": "Kara Ölüm",
      "gui.advgear.gear": "Kuşanım", "gui.advgear.slot.ring": "Yüzük yuvası", "gui.advgear.slot.goggles": "Gözlük yuvası",
      "gui.advgear.slot.belt": "Kemer yuvası", "gui.advgear.slot.back": "Sırt yuvası (çanta)",
      "key.categories.advgear": "Maceracı Kuşanımı", "key.advgear.gear": "Kuşanım ekranı", "key.advgear.backpack": "Sırttaki çantayı aç",
      "tooltip.advgear.slot.ring": "Yüzük — G ya da shift+sağ tık ile tak", "tooltip.advgear.slot.goggles": "Gözlük — G ya da shift+sağ tık ile tak",
      "tooltip.advgear.slot.belt": "Kemer — G ya da shift+sağ tık ile tak", "tooltip.advgear.slot.back": "Sırt — sağ tık: aç, shift+sağ tık: sırta tak, B: sırttakini aç",
      "tooltip.advgear.backpack_fill": "Dolu yuva: %s / %s", "tooltip.advgear.heals": "Anında %s kalp iyileştirir",
      "tooltip.advgear.spell_cost": "Bedel: %s deneyim puanı", "tooltip.advgear.spell_use": "Basılı tut: kitap açılır, büyü hazırlanır; bırak: büyü!",
      "tooltip.advgear.key_iron": "Demir kilitli sandıkları açar", "tooltip.advgear.key_bone": "Kemik (zindan) kilitli sandıkları açar",
      "tooltip.advgear.key_heart": "Altın kalp kilitli sandıkları açar", "tooltip.advgear.key_ring_count": "Anahtarlar: %s / %s",
      "msg.advgear.equipped": "Kuşanıldı: %s", "msg.advgear.slot_full": "Bu tür için boş yuva yok (G ile kuşanımını düzenle)",
      "msg.advgear.no_backpack": "Sırtında çanta yok", "msg.advgear.no_xp": "Yeterli deneyim yok (%s puan gerekli)",
      "msg.advgear.pick_failed": "Maymuncuk kaydı... tekrar dene", "msg.advgear.locked_iron": "Kilitli — Eski Demir Anahtar gerekir",
      "msg.advgear.locked_bone": "Kilitli — Zindan Anahtarı gerekir", "msg.advgear.locked_heart": "Kilitli — Altın Kalp Anahtarı gerekir",
      "msg.advgear.crystal_bound": "Kristal bu yere bağlandı: %s, %s, %s", "msg.advgear.crystal_unbound": "Kristal henüz bir yere bağlı değil (shift+sağ tık)",
      "msg.advgear.crystal_dim": "Kristalin bağlı olduğu yer başka bir boyutta"}
en = {"itemGroup.advgear": "Adventurer's Gear", "block.advgear.locked_chest": "Locked Chest", "entity.advgear.black_death": "Black Death",
      "gui.advgear.gear": "Gear", "gui.advgear.slot.ring": "Ring slot", "gui.advgear.slot.goggles": "Goggles slot", "gui.advgear.slot.belt": "Belt slot",
      "gui.advgear.slot.back": "Back slot (bag)", "key.categories.advgear": "Adventurer's Gear", "key.advgear.gear": "Gear screen",
      "key.advgear.backpack": "Open worn backpack", "tooltip.advgear.slot.ring": "Ring — equip with G or sneak+use",
      "tooltip.advgear.slot.goggles": "Goggles — equip with G or sneak+use", "tooltip.advgear.slot.belt": "Belt — equip with G or sneak+use",
      "tooltip.advgear.slot.back": "Back — use: open, sneak+use: wear, B: open worn bag", "tooltip.advgear.backpack_fill": "Used slots: %s / %s",
      "tooltip.advgear.heals": "Heals %s hearts instantly", "tooltip.advgear.spell_cost": "Cost: %s experience points",
      "tooltip.advgear.spell_use": "Hold: the book opens and the spell charges; release: cast!",
      "tooltip.advgear.key_iron": "Opens iron-locked chests", "tooltip.advgear.key_bone": "Opens bone (dungeon) locked chests",
      "tooltip.advgear.key_heart": "Opens golden heart locked chests", "tooltip.advgear.key_ring_count": "Keys: %s / %s",
      "msg.advgear.equipped": "Equipped: %s", "msg.advgear.slot_full": "No free slot of this kind (press G to manage gear)",
      "msg.advgear.no_backpack": "You are not wearing a bag", "msg.advgear.no_xp": "Not enough experience (%s points needed)",
      "msg.advgear.pick_failed": "The lockpick slipped... try again", "msg.advgear.locked_iron": "Locked — needs an Old Iron Key",
      "msg.advgear.locked_bone": "Locked — needs a Dungeon Key", "msg.advgear.locked_heart": "Locked — needs a Golden Heart Key",
      "msg.advgear.crystal_bound": "Crystal bound to %s, %s, %s", "msg.advgear.crystal_unbound": "The crystal is not bound yet (sneak+use)",
      "msg.advgear.crystal_dim": "The bound place is in another dimension"}
for i, (t, e, dt, de) in ITEMS.items():
    tr['item.advgear.' + i] = t
    en['item.advgear.' + i] = e
    if dt:
        tr['tooltip.advgear.' + i] = dt
        en['tooltip.advgear.' + i] = de
tr['item.advgear.locked_chest'] = 'Kilitli Sandık'
en['item.advgear.locked_chest'] = 'Locked Chest'
w(os.path.join(A, 'lang', 'tr_tr.json'), tr)
w(os.path.join(A, 'lang', 'en_us.json'), en)


# tarifler
def shaped(name, pattern, key, count=1):
    k = {c: ({"tag": v[1:]} if v.startswith('#') else {"item": v}) for c, v in key.items()}
    w(os.path.join(D, 'recipes', name + '.json'), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": k,
                                                    "result": {"item": "advgear:" + name, "count": count}})


def shapeless(name, items, count=1):
    w(os.path.join(D, 'recipes', name + '.json'), {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                    "ingredients": [{"tag": i[1:]} if i.startswith('#') else {"item": i} for i in items],
                                                    "result": {"item": "advgear:" + name, "count": count}})


shaped('gold_band', [" G ", "G G", " G "], {"G": "minecraft:gold_ingot"})
shaped('sapphire_ring', [" L ", "G G", " G "], {"L": "minecraft:heart_of_the_sea", "G": "minecraft:gold_ingot"})
shaped('skull_ring', [" W ", "I I", " I "], {"W": "minecraft:wither_skeleton_skull", "I": "minecraft:iron_ingot"})
shaped('bone_hand_ring', [" B ", "ILI", " I "], {"B": "minecraft:bone_block", "I": "minecraft:iron_ingot", "L": "minecraft:lodestone"})
shaped('emerald_eye_ring', [" E ", "IDI", " I "], {"E": "minecraft:emerald_block", "I": "minecraft:iron_ingot", "D": "minecraft:diamond"})
shaped('explorer_goggles', ["LCL", "G G"], {"L": "minecraft:leather", "C": "minecraft:copper_ingot", "G": "minecraft:glass_pane"})
shaped('engineer_goggles', ["LCL", "GSG", " R "], {"L": "minecraft:leather", "C": "minecraft:copper_ingot", "G": "minecraft:cyan_stained_glass_pane",
                                                   "S": "minecraft:spyglass", "R": "minecraft:redstone"})
shaped('hunter_goggles', ["LCL", "GEG"], {"L": "minecraft:leather", "C": "minecraft:copper_block", "G": "minecraft:red_stained_glass_pane",
                                          "E": "minecraft:ender_eye"})
shaped('satchel', ["SLS", "LCL", "LLL"], {"S": "minecraft:string", "L": "minecraft:leather", "C": "minecraft:chest"})
shaped('traveler_pack', ["LML", "LSL", "LIL"], {"L": "minecraft:leather", "M": "minecraft:map", "S": "advgear:satchel", "I": "minecraft:iron_ingot"})
shaped('expedition_pack', ["WGW", "LTL", "LDL"], {"W": "#minecraft:wool", "G": "minecraft:gold_ingot", "L": "minecraft:leather",
                                                  "T": "advgear:traveler_pack", "D": "minecraft:diamond"})
shapeless('heart_tonic', ["minecraft:glass_bottle", "minecraft:glistering_melon_slice", "minecraft:ghast_tear", "minecraft:sweet_berries"])
shapeless('ocean_draught', ["minecraft:glass_bottle", "minecraft:pufferfish", "minecraft:kelp", "minecraft:nautilus_shell"])
shapeless('black_death', ["minecraft:glass_bottle", "minecraft:spider_eye", "minecraft:wither_rose", "minecraft:gunpowder"], 2)
shapeless('starlight_elixir', ["minecraft:glass_bottle", "minecraft:phantom_membrane", "minecraft:glowstone_dust", "minecraft:feather"])
shapeless('sun_essence', ["minecraft:glass_bottle", "minecraft:blaze_powder", "minecraft:honey_bottle", "minecraft:sunflower"])
shaped('apprentice_spellbook', [" P ", "BBL", " S "], {"P": "minecraft:paper", "B": "minecraft:book", "L": "minecraft:lapis_lazuli",
                                                        "S": "minecraft:blaze_rod"})
shaped('traveler_tome', ["LEL", "PBP", "LEL"], {"L": "minecraft:leather", "E": "minecraft:ender_pearl", "P": "minecraft:popped_chorus_fruit",
                                                "B": "advgear:apprentice_spellbook"})
shaped('traveler_belt', ["LIL"], {"L": "minecraft:leather", "I": "minecraft:iron_ingot"})
shaped('tool_belt', ["LGL", "P P"], {"L": "minecraft:leather", "G": "minecraft:gold_ingot", "P": "minecraft:rabbit_hide"})
shaped('alchemist_belt', ["BGB", "LNL"], {"B": "minecraft:glass_bottle", "G": "minecraft:gold_ingot", "L": "minecraft:leather",
                                          "N": "minecraft:nether_wart"})
shaped('mechanic_key', ["  C", " I ", "R  "], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"})
shaped('thorn_lockpick', ["  I", " N ", "B  "], {"I": "minecraft:iron_nugget", "N": "minecraft:iron_ingot", "B": "minecraft:bone"})
shaped('crystal_key', [" AA", " EA", "G  "], {"A": "minecraft:amethyst_shard", "E": "minecraft:ender_pearl", "G": "minecraft:gold_ingot"})
shaped('key_ring', [" N ", "N N", " N "], {"N": "minecraft:iron_nugget"})


# ganimet tabloları (kilitli sandıklar)
def entry(name, weight, mn=1, mx=1):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if mx > 1:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"min": mn, "max": mx}}]
    return e


def table(name, rolls, entries, gear_rolls, gear):
    w(os.path.join(D, 'loot_tables', 'chests', name + '.json'), {"type": "minecraft:chest", "pools": [
        {"rolls": {"min": rolls[0], "max": rolls[1]}, "entries": entries},
        {"rolls": {"min": gear_rolls[0], "max": gear_rolls[1]}, "entries": gear}]})


table('locked_iron', (4, 7), [entry("minecraft:iron_ingot", 20, 2, 7), entry("minecraft:gold_ingot", 12, 1, 5), entry("minecraft:emerald", 12, 2, 6),
                              entry("minecraft:bread", 10, 2, 5), entry("minecraft:redstone", 10, 4, 12), entry("minecraft:lapis_lazuli", 8, 3, 9),
                              entry("minecraft:experience_bottle", 6, 1, 4)],
      (1, 2), [entry("advgear:gold_band", 10), entry("advgear:satchel", 10), entry("advgear:explorer_goggles", 8), entry("advgear:traveler_belt", 10),
               entry("advgear:apprentice_spellbook", 6), entry("advgear:heart_tonic", 14, 1, 3), entry("advgear:sun_essence", 8, 1, 2),
               entry("advgear:dungeon_key", 6), entry("advgear:thorn_lockpick", 8)])
table('locked_bone', (4, 7), [entry("minecraft:diamond", 10, 1, 3), entry("minecraft:gold_ingot", 14, 2, 6), entry("minecraft:emerald", 14, 3, 8),
                              entry("minecraft:bone", 10, 3, 8), entry("minecraft:ender_pearl", 8, 1, 3), entry("minecraft:experience_bottle", 10, 2, 6),
                              entry("minecraft:golden_apple", 4)],
      (1, 3), [entry("advgear:skull_ring", 8), entry("advgear:bone_hand_ring", 8), entry("advgear:engineer_goggles", 8), entry("advgear:tool_belt", 8),
               entry("advgear:traveler_pack", 8), entry("advgear:traveler_tome", 6), entry("advgear:black_death", 10, 2, 4),
               entry("advgear:ocean_draught", 8, 1, 2), entry("advgear:heart_key", 5), entry("advgear:key_ring", 6)])
table('locked_heart', (4, 8), [entry("minecraft:diamond", 14, 2, 5), entry("minecraft:emerald", 14, 4, 10), entry("minecraft:gold_block", 6, 1, 2),
                               entry("minecraft:enchanted_golden_apple", 2), entry("minecraft:golden_apple", 8, 1, 2), entry("minecraft:netherite_scrap", 4)],
      (2, 3), [entry("advgear:emerald_eye_ring", 8), entry("advgear:sapphire_ring", 8), entry("advgear:hunter_goggles", 8),
               entry("advgear:alchemist_belt", 8), entry("advgear:expedition_pack", 8), entry("advgear:black_grimoire", 6),
               entry("advgear:crystal_key", 6), entry("advgear:starlight_elixir", 10, 1, 2)])

# anahtar ve eşyalar vanilla sandıklarda
glm = []


def add(name, table_id, item, chance, count=1):
    w(os.path.join(D, 'loot_modifiers', name + '.json'), {"type": "advgear:add_item", "conditions": [
        {"condition": "forge:loot_table_id", "loot_table_id": table_id}, {"condition": "minecraft:random_chance", "chance": chance}],
        "item": item, "count": count})
    glm.append("advgear:" + name)


for t, c in (("simple_dungeon", 0.5), ("abandoned_mineshaft", 0.35), ("shipwreck_treasure", 0.3), ("village/village_weaponsmith", 0.2),
             ("pillager_outpost", 0.3), ("ruined_portal", 0.25)):
    add("iron_key_" + t.replace('/', '_'), "minecraft:chests/" + t, "advgear:iron_key", c)
for t, c in (("stronghold_corridor", 0.45), ("ancient_city", 0.35), ("bastion_other", 0.3), ("desert_pyramid", 0.3), ("jungle_temple", 0.35),
             ("simple_dungeon", 0.15)):
    add("dungeon_key_" + t, "minecraft:chests/" + t, "advgear:dungeon_key", c)
for t, c in (("woodland_mansion", 0.45), ("end_city_treasure", 0.35), ("buried_treasure", 0.4), ("ancient_city", 0.15), ("bastion_treasure", 0.4)):
    add("heart_key_" + t, "minecraft:chests/" + t, "advgear:heart_key", c)
for name, t, item, c in (("skull_ring_fortress", "nether_bridge", "skull_ring", 0.12), ("grimoire_mansion", "woodland_mansion", "black_grimoire", 0.15),
                         ("goggles_mineshaft", "abandoned_mineshaft", "engineer_goggles", 0.06), ("satchel_village", "village/village_cartographer", "satchel", 0.25),
                         ("tonic_dungeon", "simple_dungeon", "heart_tonic", 0.3), ("lockpick_outpost", "pillager_outpost", "thorn_lockpick", 0.25),
                         ("spellbook_stronghold", "stronghold_library", "apprentice_spellbook", 0.3), ("ocean_shipwreck", "shipwreck_supply", "ocean_draught", 0.25)):
    add(name, "minecraft:chests/" + t, "advgear:" + item, c)
w(os.path.join(R, 'data', 'forge', 'loot_modifiers', 'global_loot_modifiers.json'), {"replace": False, "entries": glm})

# dünya üretimi
w(os.path.join(D, 'worldgen', 'configured_feature', 'locked_chest.json'), {"type": "advgear:locked_chest", "config": {}})
w(os.path.join(D, 'worldgen', 'placed_feature', 'locked_chest.json'), {"feature": "advgear:locked_chest",
                                                                         "placement": [{"type": "minecraft:rarity_filter", "chance": 7}]})
w(os.path.join(D, 'forge', 'biome_modifier', 'add_locked_chest.json'), {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
                                                                         "features": "advgear:locked_chest", "step": "underground_decoration"})
w(os.path.join(R, 'pack.mcmeta'), {"pack": {"description": "Krolasyon Adventurer's Gear resources", "pack_format": 15}})
w(os.path.join(R, 'META-INF', 'mods.toml'), '''modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"

[[mods]]
modId="advgear"
version="1.0.0"
displayName="Krolasyon Maceracı Kuşanımı"
authors="krolasyon"
description=\'\'\'
Yüzükler, gözlükler, sırt çantaları, iksirler, büyü kitapları, kemerler ve anahtarlar: kendi kuşanım yuvaları (G),
oyuncunun üstünde 3D görünen gözlük/çanta/kemer, açılıp sayfaları dönen büyü kitapları ve mağaralarda kilitli sandıklar.
\'\'\'

[[dependencies.advgear]]
    modId="forge"
    mandatory=true
    versionRange="[47,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.advgear]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.21)"
    ordering="NONE"
    side="BOTH"
''')
print('ok', len(glm))
