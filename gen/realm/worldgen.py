"""Dimension, noise settings, biomes and features of the Crimson Realm."""
M = 'krolasyonbosses'


def n(nid, xz, y=0.0):
    return {"type": "minecraft:noise", "noise": f"{M}:{nid}", "xz_scale": xz, "y_scale": y}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def grad(y0, y1, v0, v1):
    return {"type": "minecraft:y_clamped_gradient", "from_y": y0, "to_y": y1, "from_value": v0, "to_value": v1}


def shifted(nid, xz):
    return {"type": "minecraft:shifted_noise", "noise": f"{M}:{nid}", "xz_scale": xz, "y_scale": 0.0,
            "shift_x": "minecraft:shift_x", "shift_y": 0.0, "shift_z": "minecraft:shift_z"}


NOISES = {
    'realm_hills': {"firstOctave": -8, "amplitudes": [1.0, 1.0, 0.5, 0.25]},
    'realm_detail': {"firstOctave": -5, "amplitudes": [1.0, 0.5, 0.25]},
    'realm_peaks': {"firstOctave": -7, "amplitudes": [1.0, 0.5]},
    'realm_islands': {"firstOctave": -6, "amplitudes": [1.0, 0.5, 0.25]},
    'realm_surface': {"firstOctave": -5, "amplitudes": [1.0, 1.0]},
    'realm_temperature': {"firstOctave": -7, "amplitudes": [1.5, 0.0, 1.0, 0.0, 0.0, 0.0]},
    'realm_humidity': {"firstOctave": -7, "amplitudes": [1.0, 1.0, 0.0, 0.0, 0.0, 0.0]},
}

# (biome id, temperature range, humidity range)
BIOMES = [
    ('ash_wastes', [0.18, 1.0], [-1.0, 0.0]),
    ('basalt_warfields', [0.18, 1.0], [0.0, 1.0]),
    ('throne_wastes', [-0.18, 0.18], [-1.0, 0.0]),
    ('blood_marsh', [-0.18, 0.18], [0.0, 1.0]),
    ('soul_valley', [-1.0, -0.18], [-1.0, 0.0]),
    ('obsidian_forest', [-1.0, -0.18], [0.0, 1.0]),
]

SURFACE = {  # top, top patch (noise), filler
    'ash_wastes': ('ash_soil', 'minecraft:basalt', 'ash_soil'),
    'basalt_warfields': ('scorched_earth', 'minecraft:smooth_basalt', 'minecraft:basalt'),
    'throne_wastes': ('crimson_sand', f'{M}:infernal_stone', 'crimson_sand'),
    'blood_marsh': ('blood_moss', f'{M}:coagulated_blood', 'minecraft:netherrack'),
    'soul_valley': ('minecraft:soul_soil', 'minecraft:soul_sand', 'minecraft:soul_soil'),
    'obsidian_forest': ('shadow_turf', 'minecraft:crying_obsidian', 'minecraft:blackstone'),
}


def bid(b):
    return b if ':' in b else f'{M}:{b}'


def block(b):
    return {"type": "minecraft:block", "result_state": {"Name": bid(b)}}


def dimension_type():
    return {
        "ultrawarm": True, "natural": False, "coordinate_scale": 1.0, "has_skylight": False, "has_ceiling": False,
        "ambient_light": 0.22, "fixed_time": 18000, "monster_spawn_light_level": 15, "monster_spawn_block_light_limit": 15,
        "piglin_safe": True, "bed_works": False, "respawn_anchor_works": True, "has_raids": False,
        "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_nether",
        "effects": f"{M}:crimson_realm"}


def dimension():
    biomes = []
    for b, t, h in BIOMES:
        biomes.append({"biome": f"{M}:{b}", "parameters": {
            "temperature": t, "humidity": h, "continentalness": [-1.0, 1.0], "erosion": [-1.0, 1.0],
            "weirdness": [-1.0, 1.0], "depth": [-1.0, 1.0], "offset": 0.0}})
    return {"type": f"{M}:crimson_realm", "generator": {
        "type": "minecraft:noise", "settings": f"{M}:crimson_realm",
        "biome_source": {"type": "minecraft:multi_noise", "biomes": biomes}}}


def terrain(with_detail=True):
    base = grad(0, 192, 1.0, -1.74)
    hills = mul(0.5, n('realm_hills', 1.0))
    peaks = mul(1.1, {"type": "minecraft:square", "argument": {"type": "minecraft:max", "argument1": 0.0,
                                                                "argument2": add(-0.3, n('realm_peaks', 1.0))}})
    t = add(base, add(hills, peaks))
    if with_detail:
        t = add(t, mul(0.22, n('realm_detail', 1.0, 1.4)))
    return t


def islands():
    band = {"type": "minecraft:min", "argument1": grad(132, 140, -1.0, 1.0), "argument2": grad(146, 162, 1.0, -1.0)}
    shape = mul(4.0, add(-0.42, add(n('realm_islands', 1.0), mul(0.25, n('realm_detail', 1.0, 1.0)))))
    return {"type": "minecraft:min", "argument1": band, "argument2": shape}


def noise_settings():
    final = {"type": "minecraft:interpolated", "argument": {"type": "minecraft:max", "argument1": terrain(), "argument2": islands()}}
    router = {
        "barrier": 0.0, "fluid_level_floodedness": 0.0, "fluid_level_spread": 0.0, "lava": 0.0,
        "temperature": shifted('realm_temperature', 0.25), "vegetation": shifted('realm_humidity', 0.25),
        "continents": 0.0, "erosion": 0.0, "depth": 0.0, "ridges": 0.0,
        "initial_density_without_jaggedness": terrain(False), "final_density": final,
        "vein_toggle": 0.0, "vein_ridged": 0.0, "vein_gap": 0.0}
    top_rules, fill_rules = [], []
    for b, (top, patch, filler) in SURFACE.items():
        cond = {"type": "minecraft:biome", "biome_is": [f"{M}:{b}"]}
        top_rules.append({"type": "minecraft:condition", "if_true": cond, "then_run": {"type": "minecraft:sequence", "sequence": [
            {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": f"{M}:realm_surface",
                                                         "min_threshold": 0.32, "max_threshold": 10.0}, "then_run": block(patch)},
            block(top)]}})
        fill_rules.append({"type": "minecraft:condition", "if_true": cond, "then_run": block(filler)})
    low = {"type": "minecraft:not", "invert": {"type": "minecraft:y_above", "anchor": {"absolute": 42}, "surface_depth_multiplier": 0,
                                               "add_stone_depth": False}}
    surface = {"type": "minecraft:sequence", "sequence": [
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:vertical_gradient", "random_name": f"{M}:bedrock_floor",
                                                     "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}},
         "then_run": block("minecraft:bedrock")},
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False,
                                                     "secondary_depth_range": 0, "surface_type": "floor"},
         "then_run": {"type": "minecraft:sequence", "sequence": [
             {"type": "minecraft:condition", "if_true": low, "then_run": {"type": "minecraft:sequence", "sequence": [
                 {"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": f"{M}:realm_surface",
                                                              "min_threshold": 0.0, "max_threshold": 10.0}, "then_run": block("minecraft:magma_block")},
                 block("minecraft:blackstone")]}},
             *top_rules]}},
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True,
                                                     "secondary_depth_range": 0, "surface_type": "floor"},
         "then_run": {"type": "minecraft:sequence", "sequence": fill_rules}},
    ]}
    return {"sea_level": 40, "disable_mob_generation": False, "aquifers_enabled": False, "ore_veins_enabled": False,
            "legacy_random_source": False, "default_block": {"Name": f"{M}:infernal_stone"},
            "default_fluid": {"Name": "minecraft:lava", "Properties": {"level": "0"}},
            "noise": {"min_y": 0, "height": 256, "size_horizontal": 1, "size_vertical": 2},
            "noise_router": router, "spawn_target": [], "surface_rule": surface}


# ---------------------------------------------------------------- features
def surf(chance=None, count=None, hm="WORLD_SURFACE_WG"):
    p = []
    if chance:
        p.append({"type": "minecraft:rarity_filter", "chance": chance})
    if count:
        p.append({"type": "minecraft:count", "count": count})
    p += [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": hm}, {"type": "minecraft:biome"}]
    return p


def patch(state_block, tries=48, spread=7):
    return {"type": "minecraft:random_patch", "config": {"tries": tries, "xz_spread": spread, "y_spread": 3, "feature": {
        "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider",
                                                                               "state": {"Name": bid(state_block)}}}},
        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
            {"type": "minecraft:would_survive", "state": {"Name": bid(state_block)}}]}}]}}}


def ore(block_id, size, targets=(f'{M}:infernal_stone',)):
    return {"type": "minecraft:ore", "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": t}, "state": {"Name": bid(block_id)}} for t in targets]}}


def fungus(stem, hat, decor, base):
    return {"type": "minecraft:huge_fungus", "config": {
        "hat_state": {"Name": bid(hat)}, "decor_state": {"Name": bid(decor)},
        "stem_state": {"Name": bid(stem), "Properties": {"axis": "y"}}, "valid_base_block": {"Name": bid(base)},
        "replaceable_blocks": {"type": "minecraft:matching_blocks", "blocks": [f"{M}:ember_flower", f"{M}:shadow_fern", f"{M}:blood_thorn"]},
        "planted": False}}


def simple(feature):
    return {"type": f"{M}:{feature}", "config": {}}


ORE_TARGETS = (f'{M}:infernal_stone', 'minecraft:blackstone', 'minecraft:basalt', 'minecraft:netherrack')

CONFIGURED = {
    'ore_infernal_steel': ore('infernal_steel_ore', 8, ORE_TARGETS),
    'ore_arcane_crystal': ore('arcane_crystal_ore', 5, ORE_TARGETS),
    'ore_magma': ore('minecraft:magma_block', 24, ORE_TARGETS),
    'patch_ember_flower': patch('ember_flower', 40),
    'patch_soul_lily': patch('soul_lily', 40),
    'patch_blood_thorn': patch('blood_thorn', 56),
    'patch_shadow_fern': patch('shadow_fern', 56),
    'patch_fire': patch('minecraft:fire', 24),
    'patch_soul_fire': patch('minecraft:soul_fire', 24),
    'blood_tree': fungus('blood_stem', 'blood_cap', 'minecraft:shroomlight', 'blood_moss'),
    'shadow_tree': fungus('shadow_stem', 'shadow_crystal', 'minecraft:crying_obsidian', 'shadow_turf'),
    'portal_ruin': simple('portal_ruin'),
    'throne_ruins': simple('throne_ruins'),
    'crystal_spire': simple('crystal_spire'),
    'bone_ribs': simple('bone_ribs'),
    'war_spikes': simple('war_spikes'),
    'blood_pool': simple('blood_pool'),
    'charred_tree': simple('charred_tree'),
}
for f in ('ash', 'blood', 'shadow', 'legion', 'soul'):
    CONFIGURED[f'{f}_keep'] = simple(f'{f}_keep')

ORE_PLACE = lambda c, top: [{"type": "minecraft:count", "count": c}, {"type": "minecraft:in_square"},
                            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"above_bottom": 5},
                                                                         "max_inclusive": {"absolute": top}}}, {"type": "minecraft:biome"}]

PLACED = {
    'ore_infernal_steel': ORE_PLACE(14, 110),
    'ore_arcane_crystal': ORE_PLACE(6, 80),
    'ore_magma': ORE_PLACE(4, 60),
    'patch_ember_flower': surf(count=3, hm="MOTION_BLOCKING"),
    'patch_soul_lily': surf(count=3, hm="MOTION_BLOCKING"),
    'patch_blood_thorn': surf(count=3, hm="MOTION_BLOCKING"),
    'patch_shadow_fern': surf(count=4, hm="MOTION_BLOCKING"),
    'patch_fire': surf(chance=2, hm="MOTION_BLOCKING"),
    'patch_soul_fire': surf(chance=2, hm="MOTION_BLOCKING"),
    'blood_tree': surf(count=5, hm="MOTION_BLOCKING"),
    'shadow_tree': surf(count=6, hm="MOTION_BLOCKING"),
    'portal_ruin': surf(chance=480),
    'throne_ruins': surf(chance=150),
    'crystal_spire': surf(chance=5),
    'bone_ribs': surf(chance=9),
    'war_spikes': surf(chance=4),
    'blood_pool': surf(chance=3),
    'charred_tree': surf(count=2),
}
for f in ('ash', 'blood', 'shadow', 'legion', 'soul'):
    PLACED[f'{f}_keep'] = surf(chance=170)

STEP = {'ores': 6, 'struct': 4, 'veg': 9, 'local': 2}

BIOME_FEATURES = {
    'ash_wastes': [('struct', 'ash_keep'), ('veg', 'charred_tree'), ('veg', 'patch_ember_flower'), ('veg', 'patch_fire')],
    'basalt_warfields': [('struct', 'legion_keep'), ('struct', 'war_spikes'), ('veg', 'patch_fire'), ('veg', 'patch_ember_flower')],
    'throne_wastes': [('struct', 'throne_ruins'), ('veg', 'charred_tree'), ('veg', 'patch_ember_flower')],
    'blood_marsh': [('struct', 'blood_keep'), ('local', 'blood_pool'), ('veg', 'blood_tree'), ('veg', 'patch_blood_thorn')],
    'soul_valley': [('struct', 'soul_keep'), ('struct', 'bone_ribs'), ('veg', 'patch_soul_lily'), ('veg', 'patch_soul_fire')],
    'obsidian_forest': [('struct', 'shadow_keep'), ('struct', 'crystal_spire'), ('veg', 'shadow_tree'), ('veg', 'patch_shadow_fern')],
}

BIOME_LOOK = {  # fog, sky, particle, ambient sound set, music
    'ash_wastes': (0x5a4440, 0x7a5a50, ("minecraft:white_ash", 0.12), "basalt_deltas", "basalt_deltas"),
    'basalt_warfields': (0x3a1a10, 0x6a2a14, ("minecraft:ash", 0.05), "basalt_deltas", "nether_wastes"),
    'throne_wastes': (0x6a0a0a, 0x9a1a10, ("minecraft:ash", 0.02), "nether_wastes", "nether_wastes"),
    'blood_marsh': (0x500410, 0x8a0a1a, ("minecraft:crimson_spore", 0.03), "crimson_forest", "crimson_forest"),
    'soul_valley': (0x0a3a46, 0x1a5a6a, ("minecraft:soul_fire_flame", 0.004), "soul_sand_valley", "soul_sand_valley"),
    'obsidian_forest': (0x1a0a30, 0x3a1a5a, ("minecraft:portal", 0.02), "warped_forest", "warped_forest"),
}


def biome(b, spawners):
    fog, sky, (part, prob), amb, music = BIOME_LOOK[b]
    dim = lambda c: (int(((c >> 16) & 255) * 0.42) << 16) | (int(((c >> 8) & 255) * 0.42) << 8) | int((c & 255) * 0.42)
    fog, sky = dim(fog), dim(sky)
    feats = [[] for _ in range(11)]
    feats[6] = [f'{M}:ore_infernal_steel', f'{M}:ore_arcane_crystal', f'{M}:ore_magma']
    order = list(CONFIGURED)
    for kind, f in sorted(BIOME_FEATURES[b], key=lambda kf: order.index(kf[1])):
        feats[STEP[kind]].append(f'{M}:{f}')
    return {
        "has_precipitation": False, "temperature": 2.0, "downfall": 0.0,
        "effects": {"fog_color": fog, "sky_color": sky, "water_color": 0x8a1020, "water_fog_color": 0x300408,
                    "particle": {"options": {"type": part}, "probability": prob},
                    "ambient_sound": f"minecraft:ambient.{amb}.loop",
                    "mood_sound": {"sound": f"minecraft:ambient.{amb}.mood", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "additions_sound": {"sound": f"minecraft:ambient.{amb}.additions", "tick_chance": 0.0111},
                    "music": {"sound": f"minecraft:music.nether.{music}", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}},
        "spawners": {"monster": spawners.get('monster', []), "creature": spawners.get('creature', []), "ambient": [],
                     "axolotls": [], "underground_water_creature": [], "water_creature": [], "water_ambient": [], "misc": []},
        "spawn_costs": {}, "carvers": {}, "features": feats}
