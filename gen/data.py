"""Data pack: dimensions, biomes, worldgen features, recipes."""
import os, json

NS = 'sololeveling'


def W(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=1)


def dim_type(**kw):
    base = {'ultrawarm': False, 'natural': True, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': True, 'has_raids': False,
            'has_skylight': True, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.0, 'logical_height': 320, 'min_y': 0, 'height': 320,
            'infiniburn': '#minecraft:infiniburn_overworld', 'effects': 'minecraft:overworld', 'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0}
    base.update(kw)
    return base


def biome(sky, fog, water, water_fog, grass, foliage, features, particle=None, temp=0.7):
    b = {'has_precipitation': True, 'temperature': temp, 'downfall': 0.4,
         'effects': {'sky_color': sky, 'fog_color': fog, 'water_color': water, 'water_fog_color': water_fog, 'grass_color': grass, 'foliage_color': foliage,
                     'mood_sound': {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}},
         'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': features}
    if particle:
        b['effects']['particle'] = particle
    return b


def run(data_root):
    d = os.path.join(data_root)
    W(os.path.join(d, 'dimension_type', 'seoul.json'), dim_type())
    W(os.path.join(d, 'dimension_type', 'dungeon.json'), dim_type(natural=False, bed_works=False, fixed_time=13200, ambient_light=0.08))
    W(os.path.join(d, 'dimension', 'seoul.json'), {'type': f'{NS}:seoul', 'generator': {'type': 'minecraft:flat', 'settings': {
        'biome': f'{NS}:seoul_city', 'lakes': False, 'features': True, 'structure_overrides': [],
        'layers': [{'block': 'minecraft:bedrock', 'height': 1}, {'block': 'minecraft:stone', 'height': 59}, {'block': 'minecraft:dirt', 'height': 3},
                   {'block': 'minecraft:grass_block', 'height': 1}]}}})
    W(os.path.join(d, 'dimension', 'dungeon.json'), {'type': f'{NS}:dungeon', 'generator': {'type': 'minecraft:flat', 'settings': {
        'biome': f'{NS}:dungeon_void', 'lakes': False, 'features': False, 'structure_overrides': [], 'layers': [{'block': 'minecraft:air', 'height': 1}]}}})
    city_features = [[], [], [], [], [], [], [], [], [], [f'{NS}:city'], []]
    W(os.path.join(d, 'worldgen', 'biome', 'seoul_city.json'), biome(7907327, 12638463, 4159204, 329011, 7979098, 6975545, city_features))
    W(os.path.join(d, 'worldgen', 'biome', 'dungeon_void.json'),
      biome(3014709, 1709864, 2368548, 1053727, 5926454, 4874304, [], {'options': {'type': 'minecraft:white_ash'}, 'probability': 0.004}, 0.5))
    W(os.path.join(d, 'worldgen', 'configured_feature', 'city.json'), {'type': f'{NS}:city', 'config': {}})
    W(os.path.join(d, 'worldgen', 'placed_feature', 'city.json'), {'feature': f'{NS}:city', 'placement': []})
    recipes(os.path.join(d, 'recipes'))


def shaped(out, pattern, keys, count=1):
    return {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {k: {'item': v} if ':' in v and not v.startswith('#') else {'tag': v[1:]} for k, v in keys.items()},
            'result': {'item': out, 'count': count}}


def shapeless(out, items, count=1):
    return {'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': i} for i in items], 'result': {'item': out, 'count': count}}


def recipes(d):
    s = NS + ':'
    R = {
        'return_stone': shaped(s + 'return_stone', [' L ', 'LML', ' L '], {'L': 'minecraft:lapis_lazuli', 'M': s + 'magic_stone_e'}),
        'instant_dungeon_key': shaped(s + 'instant_dungeon_key', [' G ', ' M ', ' I '], {'G': 'minecraft:gold_ingot', 'M': s + 'magic_stone_c', 'I': 'minecraft:iron_ingot'}),
        'steel_dagger': shaped(s + 'steel_dagger', [' I', 'M ', 'S '], {'I': 'minecraft:iron_ingot', 'M': s + 'magic_stone_e', 'S': 'minecraft:stick'}),
        'hunter_sword': shaped(s + 'hunter_sword', [' I ', ' I ', 'MS '], {'I': 'minecraft:iron_ingot', 'M': s + 'magic_stone_d', 'S': 'minecraft:stick'}),
        'knight_killer': shaped(s + 'knight_killer', [' F', 'M ', 'S '], {'F': s + 'statue_fragment', 'M': s + 'magic_stone_b', 'S': 'minecraft:stick'}),
        'orc_war_axe': shaped(s + 'orc_war_axe', ['TI ', 'TS ', ' S '], {'T': s + 'orc_tusk', 'I': 'minecraft:iron_block', 'S': 'minecraft:stick'}),
        'hunter_helmet': shaped(s + 'hunter_helmet', ['ILI', 'IMI'], {'I': 'minecraft:iron_ingot', 'L': 'minecraft:leather', 'M': s + 'magic_stone_d'}),
        'hunter_chestplate': shaped(s + 'hunter_chestplate', ['IMI', 'LIL', 'III'], {'I': 'minecraft:iron_ingot', 'L': 'minecraft:leather', 'M': s + 'magic_stone_d'}),
        'hunter_leggings': shaped(s + 'hunter_leggings', ['IMI', 'L L', 'I I'], {'I': 'minecraft:iron_ingot', 'L': 'minecraft:leather', 'M': s + 'magic_stone_d'}),
        'hunter_boots': shaped(s + 'hunter_boots', ['L L', 'IMI'], {'I': 'minecraft:iron_ingot', 'L': 'minecraft:leather', 'M': s + 'magic_stone_d'}),
        'high_orc_helmet': shaped(s + 'high_orc_helmet', ['TPT', 'P P'], {'T': s + 'orc_tusk', 'P': s + 'ice_bear_pelt'}),
        'high_orc_chestplate': shaped(s + 'high_orc_chestplate', ['P P', 'PTP', 'PIP'], {'T': s + 'orc_tusk', 'P': s + 'ice_bear_pelt', 'I': 'minecraft:iron_block'}),
        'high_orc_leggings': shaped(s + 'high_orc_leggings', ['PIP', 'P P', 'T T'], {'T': s + 'orc_tusk', 'P': s + 'ice_bear_pelt', 'I': 'minecraft:iron_block'}),
        'high_orc_boots': shaped(s + 'high_orc_boots', ['P P', 'T T'], {'T': s + 'orc_tusk', 'P': s + 'ice_bear_pelt'}),
        'shadow_monarch_boots': shaped(s + 'shadow_monarch_boots', ['C C', 'A A'], {'C': s + 'cerberus_fang', 'A': s + 'ant_carapace'}),
        'healing_potion': shapeless(s + 'healing_potion', ['minecraft:glass_bottle', 'minecraft:apple', s + 'magic_stone_e']),
        'mana_potion': shapeless(s + 'mana_potion', ['minecraft:glass_bottle', 'minecraft:lapis_lazuli', s + 'magic_stone_e']),
        'antidote': shapeless(s + 'antidote', ['minecraft:glass_bottle', 'minecraft:sugar', s + 'goblin_ear']),
        'greater_healing_potion': shapeless(s + 'greater_healing_potion', [s + 'healing_potion', s + 'healing_potion', s + 'magic_stone_c']),
        'greater_mana_potion': shapeless(s + 'greater_mana_potion', [s + 'mana_potion', s + 'mana_potion', s + 'magic_stone_c']),
        'fatigue_recovery_potion': shapeless(s + 'fatigue_recovery_potion', ['minecraft:glass_bottle', 'minecraft:bread', s + 'magic_stone_e']),
        'strength_elixir': shapeless(s + 'strength_elixir', ['minecraft:glass_bottle', s + 'lycan_fang', s + 'magic_stone_d']),
        'agility_elixir': shapeless(s + 'agility_elixir', ['minecraft:glass_bottle', 'minecraft:feather', s + 'magic_stone_d']),
        'perception_draught': shapeless(s + 'perception_draught', ['minecraft:glass_bottle', 'minecraft:spider_eye', s + 'elf_frost_crystal']),
        'stealth_tonic': shapeless(s + 'stealth_tonic', ['minecraft:glass_bottle', 'minecraft:ink_sac', s + 'magic_stone_c']),
        'news_board': shaped(s + 'news_board', ['III', 'GRG', 'III'], {'I': 'minecraft:iron_ingot', 'G': 'minecraft:glass_pane', 'R': 'minecraft:redstone'}),
        'asphalt': shaped(s + 'asphalt', ['GC', 'CG'], {'G': 'minecraft:gravel', 'C': 'minecraft:coal'}, 4),
        'road_line': shapeless(s + 'road_line', [s + 'asphalt', 'minecraft:yellow_dye']),
        'road_crossing': shapeless(s + 'road_crossing', [s + 'asphalt', 'minecraft:white_dye']),
        'sidewalk': shaped(s + 'sidewalk', ['SS', 'SS'], {'S': 'minecraft:smooth_stone'}, 4),
        'concrete_panel': shaped(s + 'concrete_panel', ['CC', 'CC'], {'C': 'minecraft:white_concrete'}, 4),
        'dark_panel': shaped(s + 'dark_panel', ['CC', 'CC'], {'C': 'minecraft:black_concrete'}, 4),
        'glass_facade': shaped(s + 'glass_facade', ['GG', 'GG'], {'G': 'minecraft:light_blue_stained_glass'}, 4),
        'office_window': shapeless(s + 'office_window', ['minecraft:glass', 'minecraft:glowstone_dust']),
        'dark_window': shapeless(s + 'dark_window', ['minecraft:glass', 'minecraft:blue_dye']),
        'neon_blue': shapeless(s + 'neon_blue', ['minecraft:glowstone', 'minecraft:light_blue_dye']),
        'neon_red': shapeless(s + 'neon_red', ['minecraft:glowstone', 'minecraft:red_dye']),
        'neon_purple': shapeless(s + 'neon_purple', ['minecraft:glowstone', 'minecraft:purple_dye']),
        'neon_gold': shapeless(s + 'neon_gold', ['minecraft:glowstone', 'minecraft:yellow_dye']),
        'dungeon_bricks': shaped(s + 'dungeon_bricks', ['DD', 'DD'], {'D': 'minecraft:deepslate_bricks'}, 4),
        'rune_bricks': shapeless(s + 'rune_bricks', [s + 'dungeon_bricks', s + 'magic_stone_e']),
        'temple_stone': shaped(s + 'temple_stone', ['SS', 'SS'], {'S': 'minecraft:smooth_sandstone'}, 4),
        'temple_pillar': shaped(s + 'temple_pillar', ['S', 'S'], {'S': s + 'temple_stone'}, 2),
        'demon_bricks': shaped(s + 'demon_bricks', ['NM', 'MN'], {'N': 'minecraft:nether_bricks', 'M': 'minecraft:magma_block'}, 4),
    }
    for k, v in R.items():
        W(os.path.join(d, k + '.json'), v)
