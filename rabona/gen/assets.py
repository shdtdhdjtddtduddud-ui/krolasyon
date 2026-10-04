"""Model, blockstate, loot, tarif ve dil dosyalari."""
import os, json

BASE = os.path.join(os.path.dirname(__file__), '../src/main/resources')
A = os.path.join(BASE, 'assets/rabonaarena')
D = os.path.join(BASE, 'data/rabonaarena')
NS = 'rabonaarena'


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def bs_single(name, model=None):
    w(f'{A}/blockstates/{name}.json', {"variants": {"": {"model": f"{NS}:block/{model or name}"}}})


def bs_facing(name):
    w(f'{A}/blockstates/{name}.json', {"variants": {
        "facing=north": {"model": f"{NS}:block/{name}"},
        "facing=east": {"model": f"{NS}:block/{name}", "y": 90},
        "facing=south": {"model": f"{NS}:block/{name}", "y": 180},
        "facing=west": {"model": f"{NS}:block/{name}", "y": 270}}})


def block_item(name, model=None):
    w(f'{A}/models/item/{name}.json', {"parent": f"{NS}:block/{model or name}"})


def loot(name):
    w(f'{D}/loot_tables/blocks/{name}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"{NS}:{name}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def el(frm, to, faces, rot=None):
    e = {"from": frm, "to": to, "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def allf(tex, uv=None, skip=()):
    f = {}
    for d in ("north", "south", "east", "west", "up", "down"):
        if d in skip:
            continue
        f[d] = {"texture": tex}
        if uv:
            f[d]["uv"] = uv
    return f


def blocks():
    # cim
    for g in ("pitch_grass", "pitch_grass_dark"):
        w(f'{A}/models/block/{g}.json', {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"{NS}:block/{g}", "side": f"{NS}:block/{g}_side", "bottom": "minecraft:block/dirt"}})
        bs_single(g); block_item(g); loot(g)
    w(f'{A}/models/block/pitch_line.json', {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{NS}:block/pitch_line", "side": f"{NS}:block/pitch_grass_side", "bottom": "minecraft:block/dirt"}})
    bs_single("pitch_line"); block_item("pitch_line"); loot("pitch_line")
    for c in ("stadium_concrete", "stadium_step"):
        w(f'{A}/models/block/{c}.json', {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{c}"}})
        bs_single(c); block_item(c); loot(c)
    # direk (eksenli)
    w(f'{A}/models/block/goal_post.json', {"parent": "minecraft:block/block", "textures": {"post": f"{NS}:block/goal_post", "particle": f"{NS}:block/goal_post"},
                                          "elements": [el([5, 0, 5], [11, 16, 11], {
                                              "north": {"texture": "#post", "uv": [5, 0, 11, 16]}, "south": {"texture": "#post", "uv": [5, 0, 11, 16]},
                                              "east": {"texture": "#post", "uv": [5, 0, 11, 16]}, "west": {"texture": "#post", "uv": [5, 0, 11, 16]},
                                              "up": {"texture": "#post", "uv": [5, 5, 11, 11]}, "down": {"texture": "#post", "uv": [5, 5, 11, 11]}})]})
    w(f'{A}/blockstates/goal_post.json', {"variants": {
        "axis=y": {"model": f"{NS}:block/goal_post"},
        "axis=z": {"model": f"{NS}:block/goal_post", "x": 90},
        "axis=x": {"model": f"{NS}:block/goal_post", "x": 90, "y": 90}}})
    block_item("goal_post"); loot("goal_post")
    # file
    w(f'{A}/models/block/goal_net.json', {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout",
                                         "textures": {"all": f"{NS}:block/goal_net"}})
    bs_single("goal_net"); block_item("goal_net"); loot("goal_net")
    # korner bayragi
    w(f'{A}/models/block/corner_flag.json', {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                            "textures": {"t": f"{NS}:block/corner_flag", "particle": f"{NS}:block/corner_flag"},
                                            "elements": [
                                                el([7, 0, 7], [9, 16, 9], {d: {"texture": "#t", "uv": [0, 0, 2, 16]} for d in ("north", "south", "east", "west")} | {
                                                    "up": {"texture": "#t", "uv": [0, 0, 2, 2]}}),
                                                el([9, 9, 7.9], [16, 16, 8.1], {"north": {"texture": "#t", "uv": [2, 0, 14, 9]}, "south": {"texture": "#t", "uv": [14, 0, 2, 9]}},
                                                   {"origin": [9, 9, 8], "axis": "y", "angle": -22.5}),
                                            ]})
    bs_single("corner_flag"); loot("corner_flag")
    w(f'{A}/models/item/corner_flag.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/corner_flag"}})
    # koltuklar
    for c in ("red", "blue", "white", "gold"):
        n = f"seat_{c}"
        w(f'{A}/models/block/{n}.json', {"parent": "minecraft:block/block", "textures": {"s": f"{NS}:block/{n}", "m": "minecraft:block/iron_block", "particle": f"{NS}:block/{n}"},
                                         "elements": [
                                             el([7, 0, 6], [9, 6, 10], allf("#m")),
                                             el([1, 6, 1], [15, 8, 13], allf("#s")),
                                             el([1, 8, 12], [15, 18, 14], allf("#s"), {"origin": [8, 8, 13], "axis": "x", "angle": -22.5}),
                                         ]})
        bs_facing(n); block_item(n); loot(n)
    # projektor
    w(f'{A}/models/block/floodlight.json', {"parent": "minecraft:block/block", "textures": {"f": f"{NS}:block/floodlight", "b": f"{NS}:block/floodlight_back", "particle": f"{NS}:block/floodlight_back"},
                                           "elements": [el([0, 0, 10], [16, 16, 16], {
                                               "north": {"texture": "#f"}, "south": {"texture": "#b"}, "east": {"texture": "#b"},
                                               "west": {"texture": "#b"}, "up": {"texture": "#b"}, "down": {"texture": "#b"}})]})
    bs_facing("floodlight"); block_item("floodlight"); loot("floodlight")
    # LED pano
    w(f'{A}/models/block/led_board.json', {"parent": "minecraft:block/block", "textures": {"f": f"{NS}:block/led_board", "b": f"{NS}:block/floodlight_back", "particle": f"{NS}:block/floodlight_back"},
                                          "elements": [
                                              el([0, 0, 6], [16, 14, 10], {"north": {"texture": "#f", "uv": [0, 1, 16, 15]}, "south": {"texture": "#b"},
                                                                         "east": {"texture": "#b"}, "west": {"texture": "#b"}, "up": {"texture": "#b"}, "down": {"texture": "#b"}}),
                                              el([1, 0, 10], [3, 3, 13], allf("#b")), el([13, 0, 10], [15, 3, 13], allf("#b")),
                                          ]})
    bs_facing("led_board"); block_item("led_board"); loot("led_board")


def items():
    for s in ("classic", "gold", "inferno", "neon", "galaxy", "rabona"):
        w(f'{A}/models/item/ball_{s}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/ball_{s}"}})
    for n in ("stadium_builder", "whistle", "bot_card_red", "bot_card_blue", "golden_cleats"):
        w(f'{A}/models/item/{n}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{n}"}})


def shaped(name, pattern, keys, result, count=1):
    w(f'{D}/recipes/{name}.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                   "key": {k: {"item": v} for k, v in keys.items()}, "result": {"item": result, "count": count}})


def shapeless(name, ingredients, result, count=1):
    w(f'{D}/recipes/{name}.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
                                   "ingredients": [{"item": i} for i in ingredients], "result": {"item": result, "count": count}})


def recipes():
    shaped("ball_classic", [" W ", "WLW", " W "], {"W": "minecraft:white_wool", "L": "minecraft:leather"}, f"{NS}:ball_classic")
    shapeless("ball_gold", [f"{NS}:ball_classic", "minecraft:gold_ingot", "minecraft:gold_ingot"], f"{NS}:ball_gold")
    shapeless("ball_inferno", [f"{NS}:ball_classic", "minecraft:blaze_powder", "minecraft:magma_cream"], f"{NS}:ball_inferno")
    shapeless("ball_neon", [f"{NS}:ball_classic", "minecraft:glow_ink_sac", "minecraft:glowstone_dust"], f"{NS}:ball_neon")
    shapeless("ball_galaxy", [f"{NS}:ball_classic", "minecraft:ender_pearl", "minecraft:amethyst_shard"], f"{NS}:ball_galaxy")
    shapeless("ball_rabona", [f"{NS}:ball_classic", "minecraft:cyan_dye", "minecraft:orange_dye"], f"{NS}:ball_rabona")
    shaped("stadium_builder", ["GGG", "GMG", "III"], {"G": "minecraft:grass_block", "M": "minecraft:map", "I": "minecraft:iron_ingot"}, f"{NS}:stadium_builder")
    shaped("whistle", [" S ", "INI", " I "], {"S": "minecraft:string", "I": "minecraft:iron_nugget", "N": "minecraft:iron_ingot"}, f"{NS}:whistle")
    shaped("bot_card_red", ["PRP", "PEP", "PPP"], {"P": "minecraft:paper", "R": "minecraft:red_dye", "E": "minecraft:emerald"}, f"{NS}:bot_card_red", 2)
    shaped("bot_card_blue", ["PBP", "PEP", "PPP"], {"P": "minecraft:paper", "B": "minecraft:blue_dye", "E": "minecraft:emerald"}, f"{NS}:bot_card_blue", 2)
    shaped("golden_cleats", ["G G", "GLG"], {"G": "minecraft:gold_ingot", "L": "minecraft:leather_boots"}, f"{NS}:golden_cleats")
    shaped("pitch_grass", ["GG", "GG"], {"G": "minecraft:grass_block"}, f"{NS}:pitch_grass", 4)
    shapeless("pitch_grass_dark", [f"{NS}:pitch_grass", "minecraft:green_dye"], f"{NS}:pitch_grass_dark")
    shapeless("pitch_line", [f"{NS}:pitch_grass", "minecraft:bone_meal"], f"{NS}:pitch_line")
    shaped("goal_post", ["I", "I", "I"], {"I": "minecraft:quartz_block"}, f"{NS}:goal_post", 6)
    shaped("goal_net", ["SSS", "S S", "SSS"], {"S": "minecraft:string"}, f"{NS}:goal_net", 8)
    shaped("corner_flag", ["RW", "S ", "S "], {"R": "minecraft:red_wool", "W": "minecraft:yellow_wool", "S": "minecraft:stick"}, f"{NS}:corner_flag")
    for c, dye in (("red", "red_dye"), ("blue", "blue_dye"), ("white", "white_dye"), ("gold", "yellow_dye")):
        shaped(f"seat_{c}", ["D  ", "III", "I I"], {"D": f"minecraft:{dye}", "I": "minecraft:iron_nugget"}, f"{NS}:seat_{c}", 4)
    shaped("floodlight", ["GGG", "GRG", "III"], {"G": "minecraft:glowstone", "R": "minecraft:redstone_lamp", "I": "minecraft:iron_ingot"}, f"{NS}:floodlight", 2)
    shaped("led_board", ["GGG", "RBR"], {"G": "minecraft:glass_pane", "R": "minecraft:redstone", "B": "minecraft:black_concrete"}, f"{NS}:led_board", 3)
    shaped("stadium_concrete", ["SS", "SS"], {"S": "minecraft:smooth_stone"}, f"{NS}:stadium_concrete", 4)
    shapeless("stadium_step", [f"{NS}:stadium_concrete", "minecraft:white_dye"], f"{NS}:stadium_step")


if __name__ == '__main__':
    blocks()
    items()
    recipes()
    print('ok')
