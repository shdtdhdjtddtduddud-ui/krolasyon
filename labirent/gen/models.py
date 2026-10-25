"""Blockstates, block models and item models."""
import os, json

A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/labirent')
for d in ('blockstates', 'models/block', 'models/item'):
    os.makedirs(os.path.join(A, d), exist_ok=True)


def w(path, obj):
    with open(os.path.join(A, path), 'w') as f:
        json.dump(obj, f, indent=2)


def simple(name, tex=None):
    w(f'blockstates/{name}.json', {"variants": {"": {"model": f"labirent:block/{name}"}}})
    w(f'models/block/{name}.json', {"parent": "minecraft:block/cube_all", "textures": {"all": f"labirent:block/{tex or name}"}})
    w(f'models/item/{name}.json', {"parent": f"labirent:block/{name}"})


for n in ('maze_wall', 'maze_wall_mossy', 'maze_wall_cracked', 'maze_door', 'maze_floor', 'maze_floor_mossy', 'box_floor',
          'wicked_plate', 'lab_wall', 'lab_light'):
    simple(n)

# ivy wall: stone cube + a leafy shell floating just outside every face
faces = {d: {"texture": "#leaves", "cullface": d, "uv": [0, 0, 16, 16]} for d in ("north", "south", "east", "west", "up", "down")}
base_faces = {d: {"texture": "#all", "cullface": d} for d in ("north", "south", "east", "west", "up", "down")}
w('blockstates/maze_wall_ivy.json', {"variants": {"": [{"model": "labirent:block/maze_wall_ivy"},
                                                     {"model": "labirent:block/maze_wall_ivy", "y": 90},
                                                     {"model": "labirent:block/maze_wall_ivy", "y": 180},
                                                     {"model": "labirent:block/maze_wall_ivy", "y": 270}]}})
w('models/block/maze_wall_ivy.json', {
    "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
    "textures": {"all": "labirent:block/maze_wall_ivy_base", "leaves": "labirent:block/maze_wall_ivy_leaves",
                 "particle": "labirent:block/maze_wall_ivy_base"},
    "elements": [
        {"from": [0, 0, 0], "to": [16, 16, 16], "faces": base_faces},
        {"from": [-0.6, -0.01, -0.6], "to": [16.6, 16.01, 16.6], "shade": True,
         "faces": {d: v for d, v in faces.items() if d not in ("up", "down")}}]})
w('models/item/maze_wall_ivy.json', {"parent": "labirent:block/maze_wall_ivy"})

# code stone: a carved pedestal, numeral by sector
variants = {}
for k in range(8):
    nm = f'code_stone_{k + 1}'
    w(f'models/block/{nm}.json', {
        "parent": "minecraft:block/block", "textures": {"all": f"labirent:block/{nm}", "particle": f"labirent:block/{nm}"},
        "elements": [{"from": [2, 0, 2], "to": [14, 14, 14],
                      "faces": {d: {"texture": "#all", **({"cullface": "down"} if d == "down" else {})}
                                for d in ("north", "south", "east", "west", "up", "down")}}]})
    variants[f"sector={k}"] = {"model": f"labirent:block/{nm}"}
w('blockstates/code_stone.json', {"variants": variants})
w('models/item/code_stone.json', {"parent": "labirent:block/code_stone_1"})

# map table
w('blockstates/map_table.json', {"variants": {"": {"model": "labirent:block/map_table"}}})
w('models/block/map_table.json', {
    "parent": "minecraft:block/block",
    "textures": {"top": "labirent:block/map_table_top", "side": "labirent:block/map_table_side", "particle": "labirent:block/map_table_side"},
    "elements": [
        {"from": [0, 12, 0], "to": [16, 14, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"},
                                                          "north": {"texture": "#side"}, "south": {"texture": "#side"},
                                                          "east": {"texture": "#side"}, "west": {"texture": "#side"}}},
        *[{"from": [x, 0, z], "to": [x + 2, 12, z + 2], "faces": {d: {"texture": "#side"} for d in ("north", "south", "east", "west", "down")}}
          for x in (1, 13) for z in (1, 13)]]})
w('models/item/map_table.json', {"parent": "labirent:block/map_table"})

# terminal
w('models/block/code_terminal.json', {"parent": "minecraft:block/orientable",
                                       "textures": {"front": "labirent:block/code_terminal_front", "side": "labirent:block/code_terminal_side",
                                                    "top": "labirent:block/code_terminal_side"}})
w('blockstates/code_terminal.json', {"variants": {f"facing={d}": {"model": "labirent:block/code_terminal", **({"y": r} if r else {})}
                                                  for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
w('models/item/code_terminal.json', {"parent": "labirent:block/code_terminal"})

# griever hole: invisible in the world
w('models/block/griever_hole.json', {"textures": {"particle": "labirent:block/griever_hole"}})
w('blockstates/griever_hole.json', {"variants": {"": {"model": "labirent:block/griever_hole"}}})
w('models/item/griever_hole.json', {"parent": "minecraft:item/generated", "textures": {"layer0": "labirent:item/griever_hole"}})

# items
for n in ('glade_journal', 'runner_map', 'grief_serum', 'bandage', 'griever_stinger', 'griever_plate', 'griever_core', 'beetle_eye',
          'code_word', 'glade_stew', 'glade_bread', 'frypan_breakfast'):
    w(f'models/item/{n}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"labirent:item/{n}"}})
for n in ('captain_baton', 'machete', 'glade_spear', 'frying_pan'):
    w(f'models/item/{n}.json', {"parent": "minecraft:item/handheld", "textures": {"layer0": f"labirent:item/{n}"}})
for n in ('griever_spawn_egg', 'glader_spawn_egg', 'beetle_blade_spawn_egg'):
    w(f'models/item/{n}.json', {"parent": "minecraft:item/template_spawn_egg"})
print('models written')
