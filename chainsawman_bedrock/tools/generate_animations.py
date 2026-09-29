#!/usr/bin/env python3
"""Writes animations + animation controller + particles + sound defs for the RP."""
import json
from pathlib import Path

RP = Path(__file__).resolve().parent.parent / "ChainsawMan_RP"
VIB = "Math.sin(query.life_time*9000)"      # engine vibration (~25 Hz)
SLOW = "Math.sin(query.life_time*140)"      # breathing
A = {}

# ---- steady-state form animations ------------------------------------------
A["animation.cm.form.idle"] = {"loop": True, "bones": {
    "body": {"rotation": [f"7 + {SLOW}*1.2", 0, f"{SLOW}*0.8"]},
    "head": {"rotation": [f"-6 + Math.sin(query.life_time*95)*1.5", f"Math.sin(query.life_time*47)*4", 3]},
    "rightArm": {"rotation": [f"-4 + {SLOW}*1.5", 0, f"-9 - {SLOW}*1.2"]},
    "leftArm": {"rotation": [f"-4 - {SLOW}*1.5", 0, f"9 + {SLOW}*1.2"]},
    "bladeR": {"scale": [1, f"1 + {VIB}*0.006", 1]},
    "bladeL": {"scale": [1, f"1 - {VIB}*0.006", 1]},
    "bar": {"position": [0, f"{VIB}*0.16", 0], "rotation": [f"{VIB}*0.7", 0, 0]},
    "handle": {"position": [0, f"{VIB}*0.1", 0]},
}}

A["animation.cm.form.sprint"] = {"loop": True, "bones": {
    "body": {"rotation": [15, 0, 0]},
    "head": {"rotation": [-10, 0, 0]},
    "rightArm": {"rotation": [32, 0, -10]},   # blades trail behind
    "leftArm": {"rotation": [32, 0, 10]},
    "bar": {"rotation": [f"{VIB}*1.4", 0, 0]},
    "bladeR": {"scale": [1, f"1 + {VIB}*0.02", 1]},
    "bladeL": {"scale": [1, f"1 - {VIB}*0.02", 1]},
}}

A["animation.cm.form.air_up"] = {"loop": True, "bones": {
    "body": {"rotation": [-6, 0, 0]},
    "head": {"rotation": [-8, 0, 0]},
    "rightArm": {"rotation": [-25, 0, -38]},
    "leftArm": {"rotation": [-25, 0, 38]},
    "rightLeg": {"rotation": [-12, 0, 0]},
    "leftLeg": {"rotation": [-28, 0, 0]},
    "bar": {"rotation": [-4, 0, 0]},
}}
A["animation.cm.form.air_down"] = {"loop": True, "bones": {
    "body": {"rotation": [5, 0, 0]},
    "head": {"rotation": [10, 0, 0]},
    "rightArm": {"rotation": [-15, 0, -60]},
    "leftArm": {"rotation": [-15, 0, 60]},
    "rightLeg": {"rotation": [-8, 0, 0]},
    "leftLeg": {"rotation": [-8, 0, 0]},
    "bar": {"rotation": [6, 0, 0]},
}}

# ---- transformation (3.2 s). Model swaps to Chainsaw Man at t = 1.6 s ------
SHK = "Math.sin(query.anim_time*2600)"
A["animation.cm.transform"] = {"animation_length": 3.2, "bones": {
    "body": {"rotation": {
        "0": [0, 0, 0], "0.5": [24, 0, 0],
        "0.6": [f"26 + {SHK}*4", 0, f"{SHK}*6"], "1.4": [f"30 + {SHK}*7", 0, f"{SHK}*9"],
        "1.7": [-20, 0, 0], "2.2": [-12, 0, 0], "3.2": [7, 0, 0]}},
    "head": {"rotation": {
        "0": [0, 0, 0], "0.5": [26, 0, 0], "1.4": [f"30 + {SHK}*6", f"{SHK}*10", 0],
        "1.7": [-42, 0, 0], "2.3": [-32, 0, 0], "3.2": [-6, 0, 3]}},
    "rightArm": {"rotation": {
        "0": [0, 0, 0], "0.5": [-70, 0, 22], "1.4": [f"-72 + {SHK}*8", 0, 22],
        "1.7": [-15, 0, -75], "2.3": [-10, 0, -60], "3.2": [-4, 0, -9]}},
    "leftArm": {"rotation": {
        "0": [0, 0, 0], "0.5": [-60, 0, -22], "1.4": [f"-62 + {SHK}*8", 0, -22],
        "1.7": [-15, 0, 75], "2.3": [-10, 0, 60], "3.2": [-4, 0, 9]}},
    "rightLeg": {"rotation": {"0": [0, 0, 0], "0.5": [-20, 0, 0], "1.7": [-10, 0, 0], "3.2": [0, 0, 0]}},
    "leftLeg": {"rotation": {"0": [0, 0, 0], "0.5": [14, 0, 0], "1.7": [8, 0, 0], "3.2": [0, 0, 0]}},
    "bladeR": {"scale": {"0": [1, 0.02, 1], "1.6": [1, 0.02, 1], "2.1": [1, 1.35, 1], "2.5": [1, 0.95, 1], "3.0": [1, 1, 1]}},
    "bladeL": {"scale": {"0": [1, 0.02, 1], "1.6": [1, 0.02, 1], "2.15": [1, 1.35, 1], "2.55": [1, 0.95, 1], "3.0": [1, 1, 1]}},
    "bar": {"scale": {"0": [1, 1, 0.02], "1.6": [1, 1, 0.02], "2.0": [1, 1, 1.3], "2.4": [1, 1, 0.94], "2.8": [1, 1, 1]},
            "rotation": [f"{VIB}*1.6", 0, 0]},
    "handle": {"scale": {"0": [0.02, 0.02, 0.02], "1.6": [0.02, 0.02, 0.02], "1.9": [1.2, 1.2, 1.2], "2.3": [1, 1, 1]}},
    "root": {"position": {"0": [0, 0, 0], "0.5": [0, -3, 0], "1.7": [0, 2, 0], "2.2": [0, 0, 0]}},
}}

A["animation.cm.untransform"] = {"animation_length": 1.5, "bones": {
    "body": {"rotation": {"0": [8, 0, 0], "0.4": [30, 0, 0], "0.9": [22, 0, 0], "1.5": [0, 0, 0]}},
    "head": {"rotation": {"0": [-6, 0, 3], "0.4": [30, 0, 0], "1.5": [0, 0, 0]}},
    "rightArm": {"rotation": {"0": [-4, 0, -9], "0.4": [-30, 0, 15], "1.0": [-20, 0, 6], "1.5": [0, 0, 0]}},
    "leftArm": {"rotation": {"0": [-4, 0, 9], "0.4": [-30, 0, -15], "1.0": [-20, 0, -6], "1.5": [0, 0, 0]}},
    "bladeR": {"scale": {"0": [1, 1, 1], "0.55": [1, 0.02, 1]}},
    "bladeL": {"scale": {"0": [1, 1, 1], "0.55": [1, 0.02, 1]}},
    "bar": {"scale": {"0": [1, 1, 1], "0.55": [1, 1, 0.02]}},
    "handle": {"scale": {"0": [1, 1, 1], "0.5": [0.02, 0.02, 0.02]}},
}}

# ---- abilities --------------------------------------------------------------
A["animation.cm.slash"] = {"animation_length": 0.9, "bones": {
    "body": {"rotation": {"0": [10, 0, 0], "0.1": [10, -35, 0], "0.2": [14, 42, 0], "0.3": [12, 0, 0],
                          "0.4": [12, 35, 0], "0.5": [14, -42, 0], "0.6": [12, 0, 0],
                          "0.7": [-6, 0, 0], "0.8": [32, 0, 0], "0.9": [12, 0, 0]}},
    "head": {"rotation": {"0": [-4, 0, 0], "0.2": [-2, -25, 0], "0.5": [-2, 25, 0], "0.8": [8, 0, 0], "0.9": [-4, 0, 0]}},
    "rightArm": {"rotation": {"0": [0, 0, -9], "0.1": [70, 0, -35], "0.2": [-135, 8, -12], "0.3": [-45, 0, -8],
                              "0.6": [-30, 0, -10], "0.7": [130, 0, -25], "0.8": [-120, 0, -10], "0.9": [-20, 0, -9]}},
    "leftArm": {"rotation": {"0": [0, 0, 9], "0.3": [0, 0, 12], "0.4": [70, 0, 35], "0.5": [-135, -8, 12],
                             "0.6": [-45, 0, 8], "0.7": [130, 0, 25], "0.8": [-120, 0, 10], "0.9": [-20, 0, 9]}},
    "bladeR": {"scale": {"0": [1, 1, 1], "0.2": [1, 1.25, 1], "0.3": [1, 1, 1], "0.8": [1, 1.3, 1], "0.9": [1, 1, 1]}},
    "bladeL": {"scale": {"0": [1, 1, 1], "0.5": [1, 1.25, 1], "0.6": [1, 1, 1], "0.8": [1, 1.3, 1], "0.9": [1, 1, 1]}},
    "bar": {"rotation": [f"{VIB}*1.5", 0, 0]},
    "rightLeg": {"rotation": {"0": [0, 0, 0], "0.2": [-25, 0, 0], "0.5": [15, 0, 0], "0.8": [-30, 0, 0], "0.9": [0, 0, 0]}},
    "leftLeg": {"rotation": {"0": [0, 0, 0], "0.2": [15, 0, 0], "0.5": [-25, 0, 0], "0.8": [20, 0, 0], "0.9": [0, 0, 0]}},
}}

A["animation.cm.spin"] = {"animation_length": 1.2, "bones": {
    "root": {"rotation": {"0": [0, 0, 0], "0.15": [0, 70, 0], "1.0": [0, 1050, 0], "1.2": [0, 1080, 0]}},
    "body": {"rotation": {"0": [8, 0, 0], "0.15": [14, 0, 0], "1.0": [14, 0, 0], "1.2": [8, 0, 0]}},
    "rightArm": {"rotation": {"0": [0, 0, -9], "0.15": [0, 0, -88], "1.0": [0, 0, -88], "1.2": [0, 0, -9]}},
    "leftArm": {"rotation": {"0": [0, 0, 9], "0.15": [0, 0, 88], "1.0": [0, 0, 88], "1.2": [0, 0, 9]}},
    "bladeR": {"scale": {"0": [1, 1, 1], "0.15": [1, 1.3, 1], "1.0": [1, 1.3, 1], "1.2": [1, 1, 1]}},
    "bladeL": {"scale": {"0": [1, 1, 1], "0.15": [1, 1.3, 1], "1.0": [1, 1.3, 1], "1.2": [1, 1, 1]}},
    "bar": {"scale": {"0": [1, 1, 1], "0.2": [1, 1, 1.25], "1.0": [1, 1, 1.25], "1.2": [1, 1, 1]},
            "rotation": [f"{VIB}*2", 0, 0]},
    "head": {"rotation": [-8, 0, 0]},
    "rightLeg": {"rotation": [-12, 0, 0]},
    "leftLeg": {"rotation": [12, 0, 0]},
}}

A["animation.cm.dash"] = {"animation_length": 0.8, "bones": {
    "body": {"rotation": {"0": [10, 0, 0], "0.12": [48, 0, 0], "0.6": [48, 0, 0], "0.8": [8, 0, 0]}},
    "head": {"rotation": {"0": [-4, 0, 0], "0.12": [-38, 0, 0], "0.6": [-38, 0, 0], "0.8": [-4, 0, 0]}},
    "rightArm": {"rotation": {"0": [0, 0, -9], "0.12": [95, 0, -8], "0.6": [95, 0, -8], "0.8": [-4, 0, -9]}},
    "leftArm": {"rotation": {"0": [0, 0, 9], "0.12": [95, 0, 8], "0.6": [95, 0, 8], "0.8": [-4, 0, 9]}},
    "rightLeg": {"rotation": {"0": [0, 0, 0], "0.12": [-55, 0, 0], "0.6": [-55, 0, 0], "0.8": [0, 0, 0]}},
    "leftLeg": {"rotation": {"0": [0, 0, 0], "0.12": [42, 0, 0], "0.6": [42, 0, 0], "0.8": [0, 0, 0]}},
    "bladeR": {"scale": [1, f"1.15 + {VIB}*0.03", 1]},
    "bladeL": {"scale": [1, f"1.15 - {VIB}*0.03", 1]},
    "bar": {"scale": {"0": [1, 1, 1], "0.12": [1, 1, 1.2], "0.6": [1, 1, 1.2], "0.8": [1, 1, 1]},
            "rotation": [f"{VIB}*1.8", 0, 0]},
}}

A["animation.cm.roar"] = {"animation_length": 2.0, "bones": {
    "body": {"rotation": {"0": [8, 0, 0], "0.3": [-14, 0, 0], "1.7": [-14, 0, 0], "2.0": [8, 0, 0]}},
    "head": {"rotation": {"0": [-6, 0, 3], "0.3": [-40, 0, 0], "1.7": [f"-40 + {VIB}*2", 0, 0], "2.0": [-6, 0, 3]}},
    "rightArm": {"rotation": {"0": [-4, 0, -9], "0.3": [-25, 0, -48], "1.7": [-25, 0, -48], "2.0": [-4, 0, -9]}},
    "leftArm": {"rotation": {"0": [-4, 0, 9], "0.3": [-25, 0, 48], "1.7": [-25, 0, 48], "2.0": [-4, 0, 9]}},
    "bladeR": {"scale": [1, f"1.2 + {VIB}*0.04", 1]},
    "bladeL": {"scale": [1, f"1.2 - {VIB}*0.04", 1]},
    "bar": {"scale": {"0": [1, 1, 1], "0.3": [1, 1, 1.28], "1.7": [1, 1, 1.28], "2.0": [1, 1, 1]},
            "rotation": [f"{VIB}*2.6", 0, 0], "position": [0, f"{VIB}*0.3", 0]},
    "handle": {"position": [0, f"{VIB}*0.25", 0]},
    "root": {"position": [0, f"Math.abs({VIB})*0.25", 0]},
}}

A["animation.cm.spray"] = {"animation_length": 1.2, "bones": {
    "body": {"rotation": {"0": [8, 0, 0], "0.2": [22, 0, 0], "1.0": [22, 0, 0], "1.2": [8, 0, 0]}},
    "head": {"rotation": {"0": [-6, 0, 0], "0.2": [10, 0, 0], "1.0": [10, 0, 0], "1.2": [-6, 0, 0]}},
    "rightArm": {"rotation": {"0": [-4, 0, -9], "0.2": [-85, 0, -16], "1.0": [-85, 0, -16], "1.2": [-4, 0, -9]}},
    "leftArm": {"rotation": {"0": [-4, 0, 9], "0.2": [-85, 0, 16], "1.0": [-85, 0, 16], "1.2": [-4, 0, 9]}},
    "bar": {"rotation": [f"{VIB}*1.2", 0, 0]},
}}

(RP / "animations/chainsawman.animation.json").write_text(json.dumps(
    {"format_version": "1.8.0", "animations": A}, indent=1))

# ---- animation controller ------------------------------------------------------
states = {"default": {"animations": [], "transitions": [], "blend_transition": 0.15}}
for idx, (state, anim) in enumerate([("transform", "cm_transform"), ("untransform", "cm_untransform"),
                                     ("slash", "cm_slash"), ("spin", "cm_spin"), ("dash", "cm_dash"),
                                     ("roar", "cm_roar"), ("spray", "cm_spray")], start=1):
    states["default"]["transitions"].append({state: f"query.property('cm:anim') == {idx}"})
    states[state] = {"animations": [anim], "blend_transition": 0.1,
                     "transitions": [{"default": f"query.property('cm:anim') != {idx}"}]}
(RP / "animation_controllers/chainsawman.ac.json").write_text(json.dumps({
    "format_version": "1.10.0",
    "animation_controllers": {"controller.animation.cm.ability": {"initial_state": "default", "states": states}},
}, indent=1))


# ---- particles -------------------------------------------------------------------
def particle(name, n, life, speed, size, color, grav=-6.0, radius=0.3, drag=1.0, dur=1.0,
             direction="outwards", shape="sphere", rate=None, fade=True, up=0.0):
    comps = {
        "minecraft:emitter_rate_instant": {"num_particles": n} if rate is None else None,
        "minecraft:emitter_lifetime_once": {"active_time": dur},
        "minecraft:particle_lifetime_expression": {"max_lifetime": life},
        "minecraft:particle_initial_speed": speed,
        "minecraft:particle_motion_dynamic": {"linear_acceleration": [0, grav + up, 0], "linear_drag_coefficient": drag},
        "minecraft:particle_appearance_billboard": {
            "size": [size, size], "facing_camera_mode": "lookat_xyz",
            "uv": {"texture_width": 8, "texture_height": 8, "uv": [0, 0], "uv_size": [8, 8]}},
        "minecraft:particle_appearance_tinting": {"color": color},
    }
    if shape == "sphere":
        comps["minecraft:emitter_shape_sphere"] = {"radius": radius, "direction": direction}
    else:
        comps["minecraft:emitter_shape_disc"] = {"radius": radius, "plane_normal": [0, 1, 0], "direction": direction}
    comps = {k: v for k, v in comps.items() if v is not None}
    if fade:
        comps["minecraft:particle_appearance_tinting"] = {"color": {
            "interpolant": "v.particle_age / v.particle_lifetime",
            "gradient": {"0.0": color, "0.7": color, "1.0": color[:3] + [0.0]}}}
    return {"format_version": "1.10.0", "particle_effect": {
        "description": {"identifier": f"cm:{name}", "basic_render_parameters": {
            "material": "particles_alpha", "texture": "textures/particle/cm_dot"}},
        "components": comps}}


P = {
    "blood_burst": particle("blood_burst", 44, "0.45 + Math.random(0, 0.5)", "3 + Math.random(0, 4)",
                            "0.10 + Math.random(0, 0.08)", [0.72, 0.03, 0.06, 1.0], grav=-12, radius=0.35),
    "blood_mist": particle("blood_mist", 26, "0.9 + Math.random(0, 0.8)", "0.6 + Math.random(0, 1.2)",
                           "0.35 + Math.random(0, 0.3)", [0.5, 0.02, 0.05, 0.75], grav=0.8, radius=0.5, drag=2),
    "spark": particle("spark", 16, "0.25 + Math.random(0, 0.3)", "3 + Math.random(0, 5)",
                      "0.07 + Math.random(0, 0.05)", [1.0, 0.62, 0.15, 1.0], grav=-10, radius=0.15),
    "pulse": particle("pulse", 14, "0.8 + Math.random(0, 0.4)", "0.4", "0.16",
                      [0.85, 0.05, 0.08, 0.9], grav=1.5, radius=0.5, drag=0.5),
    "smoke": particle("smoke", 12, "1.0 + Math.random(0, 1.0)", "0.5 + Math.random(0, 0.8)",
                      "0.4 + Math.random(0, 0.3)", [0.12, 0.11, 0.13, 0.7], grav=1.2, radius=0.4, drag=1.5),
}
for k, v in P.items():
    (RP / f"particles/{k}.json").write_text(json.dumps(v, indent=1))

# ---- sounds / lang ----------------------------------------------------------------
sd = {"format_version": "1.14.0", "sound_definitions": {
    "cm.engine_idle": {"category": "player", "max_distance": 32, "sounds": [{"name": "sounds/cm/engine_idle", "volume": 0.55}]},
    "cm.engine_rev": {"category": "player", "max_distance": 48, "sounds": [{"name": "sounds/cm/engine_rev", "volume": 0.9}]},
    "cm.engine_roar": {"category": "player", "max_distance": 64, "sounds": [{"name": "sounds/cm/engine_roar", "volume": 1.0}]},
    "cm.blade_hit": {"category": "player", "max_distance": 32, "sounds": [{"name": "sounds/cm/blade_hit", "volume": 0.9}]},
    "cm.blade_swing": {"category": "player", "max_distance": 32, "sounds": [{"name": "sounds/cm/blade_swing", "volume": 0.9}]},
}}
(RP / "sounds/sound_definitions.json").write_text(json.dumps(sd, indent=1))

names = {
    "pochita_heart": ("Pochita'nın Kalbi", "Pochita's Heart"),
    "blade_slash": ("Testere Kesik Serisi", "Chainsaw Slash Combo"),
    "hurricane": ("Testere Kasırgası", "Chainsaw Hurricane"),
    "chainsaw_dash": ("Testere Atılışı", "Chainsaw Dash"),
    "roar": ("Kan Kükremesi", "Blood Roar"),
    "blood_spray": ("Kan Püskürtme", "Blood Spray"),
}
for i, lang in enumerate(("tr_TR", "en_US")):
    lines = []
    for k, v in names.items():
        lines += [f"item.chainsaw_man:{k}={v[i]}", f"item.chainsaw_man:{k}.name={v[i]}"]
    (RP / f"texts/{lang}.lang").write_text("\n".join(lines) + "\n")
    (ROOT := RP.parent / "ChainsawMan_BP" / "texts" / f"{lang}.lang").write_text(
        "pack.name=Chainsaw Man Addon\npack.description=Denji / Chainsaw Man dönüşümü\n"
        if lang == "tr_TR" else "pack.name=Chainsaw Man Addon\npack.description=Denji / Chainsaw Man transformation\n")
(RP / "texts/languages.json").write_text('["en_US","tr_TR"]')
(RP.parent / "ChainsawMan_BP/texts/languages.json").write_text('["en_US","tr_TR"]')
print("ok")
