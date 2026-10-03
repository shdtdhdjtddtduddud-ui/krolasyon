"""Block states, models, item models, loot tables, recipes and tags."""
M = 'krolasyonbosses'

CUBE_ALL = ['infernal_stone', 'infernal_bricks', 'chiseled_infernal_bricks', 'ash_soil', 'crimson_sand', 'coagulated_blood',
            'cursed_obsidian', 'infernal_steel_ore', 'arcane_crystal_ore', 'infernal_steel_block', 'blood_cap', 'blood_planks',
            'shadow_crystal', 'shadow_planks', 'infernal_lantern']
BOTTOM_TOP = {'blood_moss': 'infernal_stone', 'shadow_turf': 'minecraft:block/blackstone', 'scorched_earth': 'minecraft:block/basalt_side'}
PILLARS = ['blood_stem', 'shadow_stem']
PLANTS = ['ember_flower', 'soul_lily', 'blood_thorn', 'shadow_fern']
FACTIONS = ['ash', 'blood', 'shadow', 'legion', 'soul']

HANDHELD = ['infernal_steel_sword', 'ashbringer', 'bloodthirster', 'shadowfang', 'warlord_hammer', 'soul_reaper', 'sovereign_blade']
GENERATED = ['crimson_chronicle', 'ember_key', 'ash_sigil', 'blood_sigil', 'shadow_sigil', 'legion_sigil', 'soul_sigil', 'throne_key',
             'tyrant_heart', 'raw_infernal_steel', 'infernal_steel_ingot', 'arcane_crystal', 'ash_essence', 'blood_vial', 'shadow_shard',
             'brimstone', 'soul_essence', 'mana_potion', 'tome_fireball', 'tome_meteor', 'tome_blood_lance', 'tome_shadow_step',
             'tome_soul_shield', 'tome_chain_lightning', 'tome_lava_wave', 'tome_summon_imps', 'tome_life_drain', 'tome_fear',
             'infernal_steel_helmet', 'infernal_steel_chestplate', 'infernal_steel_leggings', 'infernal_steel_boots', 'sovereign_crown',
             'ash_war_horn', 'blood_war_horn', 'shadow_war_horn', 'legion_war_horn', 'soul_war_horn', 'ally_banner']


def tex(t):
    return t if ':' in t else f'{M}:block/{t}'


def files(spawn_eggs):
    F = {}
    A = 'assets/' + M
    D = 'data/' + M

    def bs(name, obj):
        F[f'{A}/blockstates/{name}.json'] = obj

    def bm(name, obj):
        F[f'{A}/models/block/{name}.json'] = obj

    def im(name, obj):
        F[f'{A}/models/item/{name}.json'] = obj

    def single(name, model=None):
        bs(name, {"variants": {"": {"model": f"{M}:block/{model or name}"}}})

    for b in CUBE_ALL:
        single(b)
        bm(b, {"parent": "minecraft:block/cube_all", "textures": {"all": tex(b)}})
        im(b, {"parent": f"{M}:block/{b}"})
    for b, bottom in BOTTOM_TOP.items():
        single(b)
        bm(b, {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": tex(b + '_top'), "side": tex(b + '_side'), "bottom": tex(bottom)}})
        im(b, {"parent": f"{M}:block/{b}"})
    for b in PILLARS:
        bs(b, {"variants": {"axis=y": {"model": f"{M}:block/{b}"},
                            "axis=z": {"model": f"{M}:block/{b}", "x": 90},
                            "axis=x": {"model": f"{M}:block/{b}", "x": 90, "y": 90}}})
        bm(b, {"parent": "minecraft:block/cube_column", "textures": {"end": tex(b + '_top'), "side": tex(b)}})
        im(b, {"parent": f"{M}:block/{b}"})
    for b in PLANTS:
        single(b)
        bm(b, {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": tex(b)}})
        im(b, {"parent": "minecraft:item/generated", "textures": {"layer0": tex(b)}})
    # portal
    bs('realm_portal', {"variants": {"axis=x": {"model": f"{M}:block/realm_portal_ns"}, "axis=z": {"model": f"{M}:block/realm_portal_ew"}}})
    face = {"uv": [0, 0, 16, 16], "texture": "#portal"}
    bm('realm_portal_ns', {"render_type": "minecraft:translucent", "textures": {"particle": tex('realm_portal'), "portal": tex('realm_portal')},
                           "elements": [{"from": [0, 0, 6], "to": [16, 16, 10], "faces": {"north": face, "south": face}}]})
    bm('realm_portal_ew', {"render_type": "minecraft:translucent", "textures": {"particle": tex('realm_portal'), "portal": tex('realm_portal')},
                           "elements": [{"from": [6, 0, 0], "to": [10, 16, 16], "faces": {"east": face, "west": face}}]})
    # altar
    variants = {}
    for i, f in enumerate(FACTIONS):
        variants[f"faction={i}"] = {"model": f"{M}:block/altar_{f}"}

        def fc(t, uv=(0, 0, 16, 16)):
            return {d: {"uv": list(uv), "texture": t} for d in ("north", "south", "east", "west")}
        bm(f'altar_{f}', {"parent": "minecraft:block/block", "textures": {"particle": tex(f'altar_{f}_side'), "side": tex(f'altar_{f}_side'),
                                                                           "top": tex(f'altar_{f}_top'), "base": "minecraft:block/polished_blackstone"},
                          "elements": [
                              {"from": [0, 0, 0], "to": [16, 4, 16], "faces": {**fc("#base", (0, 12, 16, 16)), "up": {"texture": "#base"}, "down": {"texture": "#base"}}},
                              {"from": [2, 4, 2], "to": [14, 12, 14], "faces": {**fc("#side", (2, 4, 14, 12))}},
                              {"from": [1, 12, 1], "to": [15, 15, 15], "faces": {**fc("#side", (1, 0, 15, 3)), "up": {"texture": "#top"}, "down": {"texture": "#base"}}}]})
    bs('lord_altar', {"variants": variants})
    im('lord_altar', {"parent": f"{M}:block/altar_ash"})
    # throne
    g, c = "#gold", "#cushion"

    def box(fr, to, top=g, side=g):
        return {"from": fr, "to": to, "faces": {"north": {"texture": side}, "south": {"texture": side}, "east": {"texture": side},
                                                 "west": {"texture": side}, "up": {"texture": top}, "down": {"texture": g}}}
    bm('crimson_throne', {"parent": "minecraft:block/block", "textures": {"particle": tex('throne_gold'), "gold": tex('throne_gold'), "cushion": tex('throne_cushion')},
                          "elements": [box([1, 0, 1], [15, 6, 15]), box([2, 6, 2], [14, 8, 14], top=c, side=c),
                                       box([1, 6, 12], [15, 24, 15]), box([2, 8, 11], [14, 22, 12], top=c, side=c),
                                       box([0, 6, 1], [2, 12, 12]), box([14, 6, 1], [16, 12, 12]),
                                       box([0, 24, 13], [3, 28, 15]), box([13, 24, 13], [16, 28, 15]), box([6.5, 24, 13], [9.5, 29, 15])]})
    bs('crimson_throne', {"variants": {"facing=north": {"model": f"{M}:block/crimson_throne", "y": 180},
                                       "facing=south": {"model": f"{M}:block/crimson_throne"},
                                       "facing=west": {"model": f"{M}:block/crimson_throne", "y": 90},
                                       "facing=east": {"model": f"{M}:block/crimson_throne", "y": 270}}})
    im('crimson_throne', {"parent": f"{M}:block/crimson_throne"})
    for it in HANDHELD:
        im(it, {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{M}:item/{it}"}})
    for it in GENERATED:
        im(it, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{M}:item/{it}"}})
    for e in spawn_eggs:
        im(f'{e}_spawn_egg', {"parent": "minecraft:item/template_spawn_egg"})

    # ---------------------------------------------------------------- loot tables
    def drop_self(b):
        return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{M}:{b}"}],
                                                      "conditions": [{"condition": "minecraft:survives_explosion"}]}]}

    def ore_drop(b, item, lo, hi):
        silk = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
        return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{M}:{b}", "conditions": [silk]},
            {"type": "minecraft:item", "name": f"{M}:{item}", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]}
    for b in CUBE_ALL + list(BOTTOM_TOP) + PILLARS + PLANTS:
        F[f'{D}/loot_tables/blocks/{b}.json'] = drop_self(b)
    F[f'{D}/loot_tables/blocks/infernal_steel_ore.json'] = ore_drop('infernal_steel_ore', 'raw_infernal_steel', 1, 2)
    F[f'{D}/loot_tables/blocks/arcane_crystal_ore.json'] = ore_drop('arcane_crystal_ore', 'arcane_crystal', 1, 3)

    def chest(entries, rolls=(4, 8)):
        return {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": [
            {"type": "minecraft:item", "name": n if ':' in n else f"{M}:{n}", "weight": w,
             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": a, "max": b}}]}
            for n, w, a, b in entries]}]}
    F[f'{D}/loot_tables/chests/portal_ruin.json'] = chest([
        ('cursed_obsidian', 10, 1, 4), ('ember_key', 6, 1, 1), ('crimson_chronicle', 5, 1, 1), ('minecraft:obsidian', 10, 1, 4),
        ('minecraft:gold_nugget', 12, 3, 12), ('minecraft:flint_and_steel', 6, 1, 1), ('minecraft:fire_charge', 8, 1, 4),
        ('raw_infernal_steel', 6, 1, 3), ('arcane_crystal', 4, 1, 2), ('minecraft:golden_apple', 2, 1, 1), ('mana_potion', 4, 1, 2)])
    F[f'{D}/loot_tables/chests/kingdom_keep.json'] = chest([
        ('infernal_steel_ingot', 10, 1, 4), ('arcane_crystal', 8, 1, 4), ('mana_potion', 8, 1, 3), ('minecraft:gold_ingot', 10, 2, 8),
        ('minecraft:diamond', 3, 1, 2), ('minecraft:golden_apple', 4, 1, 2), ('minecraft:enchanted_golden_apple', 1, 1, 1),
        ('tome_fireball', 2, 1, 1), ('tome_shadow_step', 2, 1, 1), ('tome_soul_shield', 2, 1, 1), ('tome_lava_wave', 2, 1, 1),
        ('ash_essence', 5, 1, 4), ('blood_vial', 5, 1, 4), ('shadow_shard', 5, 1, 4), ('brimstone', 5, 1, 4), ('soul_essence', 5, 1, 4),
        ('infernal_steel_helmet', 1, 1, 1), ('infernal_steel_sword', 2, 1, 1)], (5, 9))

    # ---------------------------------------------------------------- recipes
    R = {}

    def shaped(name, pattern, key, result, count=1):
        R[name] = {"type": "minecraft:crafting_shaped", "pattern": pattern,
                   "key": {k: ({"tag": v[1:]} if v.startswith('#') else {"item": v if ':' in v else f"{M}:{v}"}) for k, v in key.items()},
                   "result": {"item": result if ':' in result else f"{M}:{result}", "count": count}}

    def shapeless(name, items, result, count=1):
        R[name] = {"type": "minecraft:crafting_shapeless",
                   "ingredients": [{"item": i if ':' in i else f"{M}:{i}"} for i in items],
                   "result": {"item": result if ':' in result else f"{M}:{result}", "count": count}}

    def smelt(name, item, result, xp=0.7):
        for kind, t in (("smelting", 200), ("blasting", 100)):
            R[f'{name}_{kind}'] = {"type": f"minecraft:{kind}", "ingredient": {"item": f"{M}:{item}"}, "result": f"{M}:{result}",
                                   "experience": xp, "cookingtime": t}
    smelt('infernal_steel_ingot', 'raw_infernal_steel', 'infernal_steel_ingot')
    smelt('infernal_steel_ingot_from_ore', 'infernal_steel_ore', 'infernal_steel_ingot')
    shaped('infernal_steel_block', ["###", "###", "###"], {"#": "infernal_steel_ingot"}, 'infernal_steel_block')
    shapeless('infernal_steel_ingot_from_block', ['infernal_steel_block'], 'infernal_steel_ingot', 9)
    shaped('infernal_bricks', ["##", "##"], {"#": "infernal_stone"}, 'infernal_bricks', 4)
    shaped('chiseled_infernal_bricks', ["#", "#"], {"#": "infernal_bricks"}, 'chiseled_infernal_bricks')
    shapeless('blood_planks', ['blood_stem'], 'blood_planks', 4)
    shapeless('shadow_planks', ['shadow_stem'], 'shadow_planks', 4)
    shaped('infernal_lantern', ["B#B", "#E#", "B#B"], {"#": "infernal_bricks", "B": "minecraft:blaze_powder", "E": "ash_essence"}, 'infernal_lantern', 4)
    shaped('cursed_obsidian', [" B ", "BOB", " M "], {"B": "minecraft:blaze_powder", "O": "minecraft:obsidian", "M": "minecraft:magma_cream"}, 'cursed_obsidian', 2)
    shaped('ember_key', ["  G", " I ", "F  "], {"G": "minecraft:gold_ingot", "I": "minecraft:iron_ingot", "F": "minecraft:flint"}, 'ember_key')
    shapeless('throne_key', ['ash_sigil', 'blood_sigil', 'shadow_sigil', 'legion_sigil', 'soul_sigil', 'minecraft:gold_block'], 'throne_key')
    shaped('mana_potion', [" C ", "CBC", " G "], {"C": "arcane_crystal", "B": "minecraft:glass_bottle", "G": "minecraft:glowstone_dust"}, 'mana_potion', 2)
    shaped('crimson_chronicle', ["E", "B"], {"E": "minecraft:blaze_powder", "B": "minecraft:book"}, 'crimson_chronicle')
    shaped('ally_banner', ["GSG", "TBT", " S "], {"G": "minecraft:gold_ingot", "S": "minecraft:stick", "T": "tyrant_heart", "B": "minecraft:red_banner"}, 'ally_banner')
    shaped('infernal_steel_sword', ["I", "I", "S"], {"I": "infernal_steel_ingot", "S": "minecraft:blaze_rod"}, 'infernal_steel_sword')
    shaped('infernal_steel_helmet', ["III", "I I"], {"I": "infernal_steel_ingot"}, 'infernal_steel_helmet')
    shaped('infernal_steel_chestplate', ["I I", "III", "III"], {"I": "infernal_steel_ingot"}, 'infernal_steel_chestplate')
    shaped('infernal_steel_leggings', ["III", "I I", "I I"], {"I": "infernal_steel_ingot"}, 'infernal_steel_leggings')
    shaped('infernal_steel_boots', ["I I", "I I"], {"I": "infernal_steel_ingot"}, 'infernal_steel_boots')
    tome = {'tome_fireball': 'ash_essence', 'tome_meteor': 'brimstone', 'tome_blood_lance': 'blood_vial', 'tome_shadow_step': 'shadow_shard',
            'tome_soul_shield': 'soul_essence', 'tome_chain_lightning': 'minecraft:lightning_rod', 'tome_lava_wave': 'minecraft:magma_cream',
            'tome_summon_imps': 'brimstone', 'tome_life_drain': 'blood_vial', 'tome_fear': 'soul_essence'}
    for t, ess in tome.items():
        if t in ('tome_meteor', 'tome_summon_imps', 'tome_life_drain', 'tome_fear'):
            shaped(t, ["CEC", "EBE", "CEC"], {"C": "arcane_crystal", "E": ess, "B": "minecraft:book"}, t)
        else:
            shaped(t, [" C ", "EBE", " C "], {"C": "arcane_crystal", "E": ess, "B": "minecraft:book"}, t)
    for f in FACTIONS:
        ess = {'ash': 'ash_essence', 'blood': 'blood_vial', 'shadow': 'shadow_shard', 'legion': 'brimstone', 'soul': 'soul_essence'}[f]
        shaped(f'{f}_war_horn', ["EEG", "EG ", "G  "], {"E": ess, "G": "minecraft:gold_ingot"}, f'{f}_war_horn')
    for name, r in R.items():
        F[f'{D}/recipes/{name}.json'] = r

    # ---------------------------------------------------------------- tags
    pick = ['infernal_stone', 'infernal_bricks', 'chiseled_infernal_bricks', 'cursed_obsidian', 'infernal_steel_ore', 'arcane_crystal_ore',
            'infernal_steel_block', 'infernal_lantern', 'shadow_crystal', 'lord_altar', 'crimson_throne', 'scorched_earth']
    shovel = ['ash_soil', 'crimson_sand', 'blood_moss', 'shadow_turf', 'coagulated_blood']
    axe = ['blood_stem', 'shadow_stem', 'blood_planks', 'shadow_planks']
    hoe = ['blood_cap']
    ids = lambda xs: {"replace": False, "values": [f"{M}:{x}" for x in xs]}
    F['data/minecraft/tags/blocks/mineable/pickaxe.json'] = ids(pick)
    F['data/minecraft/tags/blocks/mineable/shovel.json'] = ids(shovel)
    F['data/minecraft/tags/blocks/mineable/axe.json'] = ids(axe)
    F['data/minecraft/tags/blocks/mineable/hoe.json'] = ids(hoe)
    F['data/minecraft/tags/blocks/needs_iron_tool.json'] = ids(['infernal_steel_ore', 'arcane_crystal_ore', 'infernal_steel_block'])
    F['data/minecraft/tags/blocks/needs_diamond_tool.json'] = ids(['cursed_obsidian'])
    F[f'{D}/tags/blocks/realm_soil.json'] = ids(['infernal_stone', 'ash_soil', 'blood_moss', 'shadow_turf', 'scorched_earth', 'crimson_sand', 'coagulated_blood'])
    F['data/minecraft/tags/blocks/infiniburn_nether.json'] = ids(['infernal_stone', 'scorched_earth'])
    return F


def advancements():
    A = {}

    def disp(icon, key, frame='task', bg=None, hidden=False):
        d = {"icon": {"item": icon if ':' in icon else f"{M}:{icon}"}, "title": {"translate": f"advancements.{M}.{key}.title"},
             "description": {"translate": f"advancements.{M}.{key}.description"}, "frame": frame, "show_toast": True,
             "announce_to_chat": True, "hidden": hidden}
        if bg:
            d["background"] = bg
        return d

    def inv(items):
        return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [i if ':' in i else f"{M}:{i}" for i in items]}]}}
    A['root'] = {"display": disp('cursed_obsidian', 'root', bg=f"{M}:textures/block/infernal_bricks.png"),
                 "criteria": {"enter": {"trigger": "minecraft:changed_dimension", "conditions": {"to": f"{M}:crimson_realm"}}}}
    A['first_sigil'] = {"parent": f"{M}:realm/root", "display": disp('ash_sigil', 'first_sigil', 'goal'),
                        "criteria": {"sigil": inv(['ash_sigil', 'blood_sigil', 'shadow_sigil', 'legion_sigil', 'soul_sigil'])}}
    A['kill_lord'] = {"parent": f"{M}:realm/root", "display": disp('ashbringer', 'kill_lord', 'goal'),
                      "criteria": {l: {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [{"condition": "minecraft:entity_properties",
                                   "entity": "this", "predicate": {"type": f"{M}:{l}"}}]}} for l in ('varkhas', 'serathis', 'nyxar', 'grommak', 'ilvaine')},
                      "requirements": [['varkhas', 'serathis', 'nyxar', 'grommak', 'ilvaine']]}
    A['throne_key'] = {"parent": f"{M}:realm/first_sigil", "display": disp('throne_key', 'throne_key', 'goal'), "criteria": {"key": inv(['throne_key'])}}
    A['sovereign'] = {"parent": f"{M}:realm/throne_key", "display": disp('sovereign_crown', 'sovereign', 'challenge'),
                      "criteria": {"crown": inv(['sovereign_crown'])}, "rewards": {"experience": 1000}}
    A['spell'] = {"parent": f"{M}:realm/root", "display": disp('tome_fireball', 'spell'),
                  "criteria": {"tome": inv(['tome_fireball', 'tome_meteor', 'tome_blood_lance', 'tome_shadow_step', 'tome_soul_shield',
                                            'tome_chain_lightning', 'tome_lava_wave', 'tome_summon_imps', 'tome_life_drain', 'tome_fear'])}}
    A['legendary'] = {"parent": f"{M}:realm/kill_lord", "display": disp('sovereign_blade', 'legendary', 'challenge'),
                      "criteria": {"w": inv(['ashbringer', 'bloodthirster', 'shadowfang', 'warlord_hammer', 'soul_reaper', 'sovereign_blade'])}}
    return {f'data/{M}/advancements/realm/{k}.json': v for k, v in A.items()}
