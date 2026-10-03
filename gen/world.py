"""Datapack JSON for the realm of Azrakor: dimension type, dimension, noise settings, biomes, features, tags.
python3 gen/world.py"""
import os, sys, json, shutil
ROOT = os.path.join(os.path.dirname(__file__), '..')
DATA = os.path.join(ROOT, 'src/main/resources/data/krolasyonbosses')
NS = 'krolasyonbosses'

for d in ('dimension_type', 'dimension', 'worldgen/noise_settings', 'worldgen/biome', 'worldgen/configured_feature', 'worldgen/placed_feature',
          'tags/worldgen/biome', 'tags/blocks'):
    p = os.path.join(DATA, d)
    shutil.rmtree(p, ignore_errors=True)
    os.makedirs(p, exist_ok=True)


def dump(path, obj):
    os.makedirs(os.path.dirname(os.path.join(DATA, path)), exist_ok=True)
    with open(os.path.join(DATA, path), 'w') as f:
        json.dump(obj, f, indent=1)


def blk(name, **props):
    d = {'Name': name}
    if props:
        d['Properties'] = props
    return d


# ------------------------------------------------------------------ dimension type + dimension
dump('dimension_type/azrakor.json', {
    'ultrawarm': False, 'natural': False, 'coordinate_scale': 1.0, 'has_skylight': True, 'has_ceiling': False, 'ambient_light': 0.12,
    'fixed_time': 13200, 'piglin_safe': False, 'bed_works': False, 'respawn_anchor_works': True, 'has_raids': False,
    'logical_height': 256, 'min_y': 0, 'height': 256, 'infiniburn': f'#{NS}:infiniburn_azrakor', 'effects': f'{NS}:azrakor',
    'monster_spawn_light_level': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 0, 'max_inclusive': 15}},
    'monster_spawn_block_light_limit': 15})

dump('tags/blocks/infiniburn_azrakor.json', {'replace': False, 'values': [f'{NS}:ashstone', f'{NS}:ember_rock', f'{NS}:ashstone_bricks', 'minecraft:netherrack', 'minecraft:magma_block', 'minecraft:bedrock']})

# biome table: id, faction, fog, sky, water, particle(id, prob), surface (top, filler), mood, loop, features
BIOMES = {
    'ember_wastes': dict(fac='ember', fog=0x4A1408, sky=0x5E1A0A, particle=('minecraft:ash', 0.03), top=f'{NS}:ember_rock', fill=f'{NS}:ashstone',
                         mood='minecraft:ambient.basalt_deltas.mood', loop='minecraft:ambient.basalt_deltas.loop',
                         veg=['spire_basalt', 'spire_obsidian', 'ruin_ember', 'crystals_ember']),
    'ashen_barrens': dict(fac='ember', fog=0x3C3236, sky=0x4A3C40, particle=('minecraft:white_ash', 0.06), top=f'{NS}:ashstone', fill=f'{NS}:ashstone',
                          mood='minecraft:ambient.nether_wastes.mood', loop='minecraft:ambient.nether_wastes.loop',
                          veg=['spire_obsidian', 'dead_tree_ash', 'ruin_ember']),
    'bone_marches': dict(fac='bone', fog=0x2C3640, sky=0x3A4852, particle=('minecraft:white_ash', 0.04), top=f'{NS}:bone_sand', fill=f'{NS}:ashstone',
                         mood='minecraft:ambient.soul_sand_valley.mood', loop='minecraft:ambient.soul_sand_valley.loop',
                         veg=['ribs_bone', 'spire_bone', 'ruin_bone', 'skulls']),
    'ossuary_fields': dict(fac='bone', fog=0x24302E, sky=0x34403E, particle=('minecraft:soul', 0.01), top=f'{NS}:bone_sand', fill=f'{NS}:ashstone',
                           mood='minecraft:ambient.soul_sand_valley.mood', loop='minecraft:ambient.soul_sand_valley.loop',
                           veg=['ribs_bone', 'ruin_bone', 'skulls', 'dead_tree_bone']),
    'crimson_gardens': dict(fac='blood', fog=0x3A0814, sky=0x5C1024, particle=('minecraft:crimson_spore', 0.05), top=f'{NS}:blood_moss', fill=f'{NS}:ashstone',
                            mood='minecraft:ambient.crimson_forest.mood', loop='minecraft:ambient.crimson_forest.loop',
                            veg=['dead_tree_blood', 'ruin_blood', 'spire_blood']),
    'blood_spires': dict(fac='blood', fog=0x2A0510, sky=0x4C0C1C, particle=('minecraft:crimson_spore', 0.04), top=f'{NS}:blood_moss', fill=f'{NS}:ashstone',
                         mood='minecraft:ambient.crimson_forest.mood', loop='minecraft:ambient.crimson_forest.loop',
                         veg=['spire_blood', 'spire_blood', 'crystals_blood', 'ruin_blood']),
    'umbral_reach': dict(fac='shadow', fog=0x14082A, sky=0x22104A, particle=('minecraft:warped_spore', 0.04), top=f'{NS}:shadow_loam', fill=f'{NS}:ashstone',
                         mood='minecraft:ambient.warped_forest.mood', loop='minecraft:ambient.warped_forest.loop',
                         veg=['crystals_shadow', 'spire_shadow', 'ruin_shadow']),
    'gloam_forest': dict(fac='shadow', fog=0x0E0A22, sky=0x1A1240, particle=('minecraft:warped_spore', 0.06), top=f'{NS}:shadow_loam', fill=f'{NS}:ashstone',
                         mood='minecraft:ambient.warped_forest.mood', loop='minecraft:ambient.warped_forest.loop',
                         veg=['dead_tree_shadow', 'dead_tree_shadow', 'dead_tree_shadow', 'crystals_shadow']),
    'rot_bog': dict(fac='rot', fog=0x1A2A0A, sky=0x2C4812, particle=('minecraft:spore_blossom_air', 0.05), top=f'{NS}:rot_mud', fill=f'{NS}:ashstone',
                    mood='minecraft:ambient.warped_forest.mood', loop='minecraft:ambient.warped_forest.loop',
                    veg=['huge_brown', 'huge_red', 'spire_rot', 'ruin_rot']),
    'weeping_marsh': dict(fac='rot', fog=0x1E3018, sky=0x30481E, particle=('minecraft:spore_blossom_air', 0.07), top='minecraft:moss_block', fill=f'{NS}:rot_mud',
                          mood='minecraft:ambient.warped_forest.mood', loop='minecraft:ambient.warped_forest.loop',
                          veg=['huge_brown', 'dead_tree_rot', 'spire_rot', 'ruin_rot']),
}
FACTION_ORDER = ['ember', 'bone', 'blood', 'shadow', 'rot']

# climate points: factions on a ring (temperature, humidity); each faction has an inner and an outer biome
import math
CLIMATE = {}
for i, f in enumerate(FACTION_ORDER):
    a = math.radians(i * 72)
    for k, (bid, b) in enumerate([(bid, b) for bid, b in BIOMES.items() if b['fac'] == f]):
        r = 0.45 if k == 0 else 0.85
        CLIMATE[bid] = (round(math.cos(a) * r, 3), round(math.sin(a) * r, 3))

spawns = json.load(open(os.path.join(os.path.dirname(__file__), '_spawns.json')))

# ------------------------------------------------------------------ placed / configured features
def spawner_list(biome):
    out = []
    for mob, s in spawns.items():
        if biome in s['biomes'] and s['weight'] > 0:
            out.append({'type': f'{NS}:{mob}', 'weight': s['weight'], 'minCount': s['min'], 'maxCount': s['max']})
    return out


def simple(state):
    return {'to_place': {'type': 'minecraft:simple_state_provider', 'state': state}}


CF = {
    'spire_basalt': ('spire', simple(blk('minecraft:blackstone'))),
    'spire_obsidian': ('spire', simple(blk('minecraft:obsidian'))),
    'spire_bone': ('spire', simple(blk('minecraft:bone_block', axis='y'))),
    'spire_blood': ('spire', simple(blk('minecraft:red_nether_bricks'))),
    'spire_shadow': ('spire', simple(blk('minecraft:deepslate_tiles'))),
    'spire_rot': ('spire', simple(blk('minecraft:mushroom_stem'))),
    'dead_tree_ash': ('dead_tree', simple(blk('minecraft:stripped_dark_oak_log', axis='y'))),
    'dead_tree_bone': ('dead_tree', simple(blk('minecraft:bone_block', axis='y'))),
    'dead_tree_blood': ('dead_tree', simple(blk('minecraft:crimson_stem', axis='y'))),
    'dead_tree_shadow': ('dead_tree', simple(blk('minecraft:stripped_dark_oak_log', axis='y'))),
    'dead_tree_rot': ('dead_tree', simple(blk('minecraft:mangrove_log', axis='y'))),
    'ribs_bone': ('ribs', simple(blk('minecraft:bone_block', axis='x'))),
    'ruin_ember': ('ruin', simple(blk('minecraft:polished_blackstone_bricks'))),
    'ruin_bone': ('ruin', simple(blk('minecraft:quartz_bricks'))),
    'ruin_blood': ('ruin', simple(blk('minecraft:red_nether_bricks'))),
    'ruin_shadow': ('ruin', simple(blk('minecraft:deepslate_bricks'))),
    'ruin_rot': ('ruin', simple(blk('minecraft:mud_bricks'))),
    'crystals_ember': ('crystals', simple(blk('minecraft:magma_block'))),
    'crystals_blood': ('crystals', simple(blk('minecraft:crimson_hyphae', axis='y'))),
    'crystals_shadow': ('crystals', simple(blk('minecraft:amethyst_block'))),
}
for name, (ftype, cfg) in CF.items():
    dump(f'worldgen/configured_feature/{name}.json', {'type': f'{NS}:{ftype}', 'config': cfg})
    chance = {'spire': 8, 'dead_tree': 2, 'ribs': 12, 'ruin': 14, 'crystals': 10}[ftype]
    count = {'spire': 1, 'dead_tree': 2, 'ribs': 1, 'ruin': 1, 'crystals': 1}[ftype]
    if ftype == 'dead_tree':
        placement = [{'type': 'minecraft:count', 'count': {'type': 'minecraft:uniform', 'min_inclusive': 2, 'max_inclusive': 5}}]
    else:
        placement = [{'type': 'minecraft:rarity_filter', 'chance': max(1, chance // 3)}]
    placement += [{'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}]
    dump(f'worldgen/placed_feature/{name}.json', {'feature': f'{NS}:{name}', 'placement': placement})

# vanilla-based decoration
dump('worldgen/configured_feature/skulls.json', {'type': 'minecraft:simple_block', 'config': {'to_place': {'type': 'minecraft:simple_state_provider', 'state': blk('minecraft:skeleton_skull', rotation='4')}}})
dump('worldgen/placed_feature/skulls.json', {'feature': f'{NS}:skulls', 'placement': [{'type': 'minecraft:count', 'count': 3}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}]})
for nm, mush in (('huge_brown', 'minecraft:huge_brown_mushroom'), ('huge_red', 'minecraft:huge_red_mushroom')):
    dump(f'worldgen/placed_feature/{nm}.json', {'feature': mush, 'placement': [{'type': 'minecraft:count', 'count': {'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 3}}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}]})

dump('worldgen/configured_feature/ore_hellsteel.json', {'type': 'minecraft:ore', 'config': {'size': 9, 'discard_chance_on_air_exposure': 0.0,
     'targets': [{'target': {'predicate_type': 'minecraft:block_match', 'block': f'{NS}:ashstone'}, 'state': blk(f'{NS}:hellsteel_ore')}]}})
dump('worldgen/placed_feature/ore_hellsteel.json', {'feature': f'{NS}:ore_hellsteel', 'placement': [
    {'type': 'minecraft:count', 'count': 18}, {'type': 'minecraft:in_square'},
    {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': 4}, 'max_inclusive': {'absolute': 110}}}, {'type': 'minecraft:biome'}]})

# ------------------------------------------------------------------ biomes
GLOBAL_ORDER = list(CF) + ['skulls', 'huge_brown', 'huge_red']
for bid, b in BIOMES.items():
    feats = [[] for _ in range(11)]
    feats[6] = [f'{NS}:ore_hellsteel']
    feats[9] = [f'{NS}:{v}' for v in sorted(dict.fromkeys(b['veg']), key=GLOBAL_ORDER.index)]
    dump(f'worldgen/biome/{bid}.json', {
        'has_precipitation': False, 'temperature': 2.0, 'downfall': 0.0,
        'effects': {'fog_color': b['fog'], 'sky_color': b['sky'], 'water_color': 0x3F76E4, 'water_fog_color': 0x050533,
                    'particle': {'probability': b['particle'][1], 'options': {'type': b['particle'][0]}},
                    'ambient_sound': b['loop'], 'mood_sound': {'sound': b['mood'], 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}},
        'spawners': {'monster': spawner_list(bid), 'creature': [], 'ambient': [], 'axolotls': [], 'underground_water_creature': [], 'water_creature': [], 'water_ambient': [], 'misc': []},
        'spawn_costs': {}, 'carvers': {}, 'features': feats})

for f in FACTION_ORDER:
    dump(f'tags/worldgen/biome/{f}_lands.json', {'replace': False, 'values': [f'{NS}:{bid}' for bid, b in BIOMES.items() if b['fac'] == f]})
dump('tags/worldgen/biome/azrakor.json', {'replace': False, 'values': [f'{NS}:{bid}' for bid in BIOMES]})

# ------------------------------------------------------------------ noise settings (terrain + surface)
def shifted(noise):
    return {'type': 'minecraft:shifted_noise', 'noise': f'minecraft:{noise}', 'xz_scale': 0.2, 'y_scale': 0.0,
            'shift_x': 'minecraft:shift_x', 'shift_y': 0.0, 'shift_z': 'minecraft:shift_z'}


terrain = {
    'type': 'minecraft:squeeze', 'argument': {'type': 'minecraft:mul', 'argument1': 0.64, 'argument2': {'type': 'minecraft:interpolated', 'argument': {'type': 'minecraft:blend_density', 'argument': {
        'type': 'minecraft:add',
        'argument1': {'type': 'minecraft:y_clamped_gradient', 'from_y': 30, 'to_y': 150, 'from_value': 1.7, 'to_value': -1.7},
        'argument2': {'type': 'minecraft:add',
                      'argument1': {'type': 'minecraft:mul', 'argument1': 0.8, 'argument2': {'type': 'minecraft:old_blended_noise', 'xz_scale': 0.25, 'y_scale': 0.375, 'xz_factor': 80.0, 'y_factor': 60.0, 'smear_scale_multiplier': 8.0}},
                      'argument2': {'type': 'minecraft:mul', 'argument1': 0.55, 'argument2': {'type': 'minecraft:noise', 'noise': 'minecraft:continentalness', 'xz_scale': 0.3, 'y_scale': 0.0}}}}}}}}

def biome_rule(bid, b):
    return {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [f'{NS}:{bid}']},
            'then_run': {'type': 'minecraft:sequence', 'sequence': [
                {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': 'floor', 'add_surface_depth': False, 'secondary_depth_range': 0},
                 'then_run': {'type': 'minecraft:block', 'result_state': blk(b['top'])}},
                {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': 'floor', 'add_surface_depth': True, 'secondary_depth_range': 0},
                 'then_run': {'type': 'minecraft:block', 'result_state': blk(b['fill'])}}]}}


dump('worldgen/noise_settings/azrakor.json', {
    'sea_level': 44, 'disable_mob_generation': False, 'aquifers_enabled': False, 'ore_veins_enabled': False, 'legacy_random_source': False,
    'default_block': blk(f'{NS}:ashstone'), 'default_fluid': blk('minecraft:lava', level='0'),
    'noise': {'min_y': 0, 'height': 256, 'size_horizontal': 1, 'size_vertical': 2},
    'noise_router': {
        'barrier': 0, 'fluid_level_floodedness': 0, 'fluid_level_spread': 0, 'lava': 0,
        'temperature': shifted('temperature'), 'vegetation': shifted('vegetation'), 'continents': 0, 'erosion': 0, 'depth': 0, 'ridges': 0,
        'initial_density_without_jaggedness': 0, 'final_density': terrain, 'vein_toggle': 0, 'vein_ridged': 0, 'vein_gap': 0},
    'spawn_target': [],
    'surface_rule': {'type': 'minecraft:sequence', 'sequence': [
        {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:vertical_gradient', 'random_name': 'minecraft:bedrock_floor', 'true_at_and_below': {'above_bottom': 0}, 'false_at_and_above': {'above_bottom': 5}},
         'then_run': {'type': 'minecraft:block', 'result_state': blk('minecraft:bedrock')}}] + [biome_rule(bid, b) for bid, b in BIOMES.items()]}})

# ------------------------------------------------------------------ dimension
dump('dimension/azrakor.json', {'type': f'{NS}:azrakor', 'generator': {
    'type': 'minecraft:noise', 'settings': f'{NS}:azrakor',
    'biome_source': {'type': 'minecraft:multi_noise', 'biomes': [
        {'biome': f'{NS}:{bid}', 'parameters': {'temperature': t, 'humidity': h, 'continentalness': 0.0, 'erosion': 0.0, 'depth': 0.0, 'weirdness': 0.0, 'offset': 0.0}}
        for bid, (t, h) in CLIMATE.items()]}}})
print('biomes', len(BIOMES), 'features', len(CF) + 3)
