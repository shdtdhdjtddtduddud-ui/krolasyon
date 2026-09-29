"""Builds HulkMod_BP / HulkMod_RP and dist/HulkMod.mcaddon.   python3 build.py"""
import json, os, shutil, sys, zipfile, math, copy
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
from model import build as build_model
import paint, anims, preview

BP = os.path.join(ROOT, "HulkMod_BP")
RP = os.path.join(ROOT, "HulkMod_RP")
DIST = os.path.join(ROOT, "dist")

UUID = dict(
    bp="6f1c1a40-3b2e-4a8d-9c11-0a5d7e1f0001", bp_data="6f1c1a40-3b2e-4a8d-9c11-0a5d7e1f0002",
    bp_script="6f1c1a40-3b2e-4a8d-9c11-0a5d7e1f0003",
    rp="6f1c1a40-3b2e-4a8d-9c11-0a5d7e1f0004", rp_res="6f1c1a40-3b2e-4a8d-9c11-0a5d7e1f0005",
)
VERSION = [1, 0, 0]


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        if isinstance(obj, str):
            f.write(obj)
        else:
            json.dump(obj, f, indent=2, ensure_ascii=False)


def save_png(img, path, colors=None):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)


# ------------------------------------------------------------------------------ textures
def make_item_icons():
    S = 128
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # glow
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((14, 26, 114, 122), fill=(120, 255, 60, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(9))
    im = Image.alpha_composite(im, glow)
    d = ImageDraw.Draw(im)
    # apple body (two lobes)
    d.ellipse((20, 34, 70, 112), fill=(88, 190, 40, 255))
    d.ellipse((58, 34, 108, 112), fill=(88, 190, 40, 255))
    d.ellipse((30, 38, 98, 118), fill=(88, 190, 40, 255))
    # shading
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sh)
    sd.ellipse((44, 52, 112, 124), fill=(20, 90, 20, 120))
    sh = sh.filter(ImageFilter.GaussianBlur(8))
    mask = Image.new("L", (S, S), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse((20, 34, 70, 112), fill=255); md.ellipse((58, 34, 108, 112), fill=255); md.ellipse((30, 38, 98, 118), fill=255)
    sh.putalpha(Image.composite(sh.split()[3], Image.new("L", (S, S), 0), mask))
    im = Image.alpha_composite(im, sh)
    d = ImageDraw.Draw(im)
    d.ellipse((34, 46, 56, 70), fill=(190, 255, 120, 255))                     # highlight
    d.ellipse((40, 50, 50, 60), fill=(240, 255, 210, 255))
    # stem + leaf
    d.polygon([(62, 40), (70, 40), (74, 14), (66, 14)], fill=(74, 48, 24, 255))
    d.polygon([(72, 26), (108, 8), (100, 40)], fill=(60, 160, 40, 255))
    # radiation trefoil
    cx, cy, R = 64, 82, 26
    d.ellipse((cx - R, cy - R, cx + R, cy + R), fill=(250, 226, 30, 255), outline=(20, 20, 20, 255), width=3)
    for k in range(3):
        a0 = math.radians(-90 + k * 120 - 30)
        d.pieslice((cx - 20, cy - 20, cx + 20, cy + 20), math.degrees(a0), math.degrees(a0) + 60, fill=(20, 20, 20, 255))
    d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), fill=(20, 20, 20, 255))
    apple = im.resize((32, 32), Image.LANCZOS)

    # calm pill: blue/white capsule
    pill = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pd = ImageDraw.Draw(pill)
    cap = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cap)
    cd.rounded_rectangle((16, 44, 112, 84), radius=20, fill=(240, 240, 250, 255))
    cd.rounded_rectangle((16, 44, 64, 84), radius=20, fill=(60, 130, 230, 255))
    cd.rectangle((44, 44, 64, 84), fill=(60, 130, 230, 255))
    cd.rounded_rectangle((22, 48, 106, 58), radius=5, fill=(255, 255, 255, 110))
    cap = cap.rotate(35, resample=Image.BICUBIC)
    pill = Image.alpha_composite(pill, cap)
    ol = pill.filter(ImageFilter.MaxFilter(5))
    outline = Image.new("RGBA", (S, S), (25, 30, 60, 255)); outline.putalpha(ol.split()[3])
    pill = Image.alpha_composite(outline, pill).resize((32, 32), Image.LANCZOS)
    return apple, pill


def make_particle_sheet():
    S = 128
    a = np.zeros((S, S, 4), np.float32)
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)

    def cell(ix, iy, fn):
        for y in range(32):
            for x in range(32):
                pass
    sheet = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    # cell 0: soft glow
    g = np.zeros((32, 32, 4), np.float32)
    y, x = np.mgrid[0:32, 0:32]
    r = np.hypot(x - 15.5, y - 15.5) / 15.5
    al = np.clip(1 - r, 0, 1) ** 1.6
    g[..., :3] = 255; g[..., 3] = al * 255
    sheet.paste(Image.fromarray(g.astype(np.uint8), "RGBA"), (0, 0))
    # cell 1: ring
    g = np.zeros((32, 32, 4), np.float32)
    ring = np.exp(-((r - 0.82) / 0.09) ** 2) + 0.25 * np.exp(-((r - 0.6) / 0.2) ** 2)
    g[..., :3] = 255; g[..., 3] = np.clip(ring, 0, 1) * 255 * (r < 1)
    sheet.paste(Image.fromarray(g.astype(np.uint8), "RGBA"), (32, 0))
    # cell 2: rock chip
    ch = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    ImageDraw.Draw(ch).polygon([(6, 20), (14, 6), (26, 10), (24, 24), (12, 26)], fill=(110, 116, 108, 255), outline=(60, 64, 60, 255))
    sheet.paste(ch, (64, 0))
    # cell 3: cloth rag
    cl = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    ImageDraw.Draw(cl).polygon([(4, 8), (24, 4), (28, 16), (18, 28), (8, 22)], fill=(112, 84, 140, 255), outline=(60, 44, 80, 255))
    sheet.paste(cl, (96, 0))
    return sheet


def make_pack_icon(model, atlas):
    im = preview.render(model, atlas, "hulk", pose=anims.roar(1.0), yaw=12, pitch=0, size=(256, 256), scale=4.6,
                        center=(0, 30, 0), bg=(20, 44, 26))
    # green vignette + glow
    arr = np.asarray(im).astype(np.float32)
    yy, xx = np.mgrid[0:256, 0:256]
    v = np.clip(1 - np.hypot(xx - 128, yy - 128) / 190, 0, 1)
    arr = arr * (0.55 + 0.45 * v[..., None]) + np.array([10, 40, 5]) * (v ** 2)[..., None]
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))


# ------------------------------------------------------------------------------ RP
def build_rp(model, atlases):
    if os.path.exists(RP):
        shutil.rmtree(RP)
    w(f"{RP}/manifest.json", {
        "format_version": 2,
        "header": {"name": "§aHulk Mod §7RP", "description": "Hulk dönüşümü: model, texture, animasyonlar",
                   "uuid": UUID["rp"], "version": VERSION, "min_engine_version": [1, 21, 60]},
        "modules": [{"type": "resources", "uuid": UUID["rp_res"], "version": VERSION}],
        "dependencies": [{"uuid": UUID["bp"], "version": VERSION}],
    })
    for i, a in enumerate(atlases):
        save_png(a, f"{RP}/textures/entity/hulk/hulk_s{i}.png")
    apple, pill = make_item_icons()
    save_png(apple, f"{RP}/textures/items/radioactive_apple.png")
    save_png(pill, f"{RP}/textures/items/calm_pill.png")
    save_png(make_particle_sheet(), f"{RP}/textures/particle/hulk_particles.png")
    save_png(make_pack_icon(model, atlases[3]), f"{RP}/pack_icon.png")
    w(f"{RP}/textures/item_texture.json", {
        "resource_pack_name": "hulk", "texture_name": "atlas.items",
        "texture_data": {"hulk_radioactive_apple": {"textures": "textures/items/radioactive_apple"},
                         "hulk_calm_pill": {"textures": "textures/items/calm_pill"}}})
    w(f"{RP}/models/entity/hulk.geo.json", model.geometry_json("hulk", "geometry.hulk"))
    w(f"{RP}/models/entity/hulk_fp.geo.json", model.geometry_json("hulk_fp", "geometry.hulk_fp"))
    w(f"{RP}/animations/hulk.animation.json", anims.animation_json())
    w(f"{RP}/animation_controllers/hulk.animation_controllers.json", animation_controllers())
    w(f"{RP}/render_controllers/hulk.render_controllers.json", render_controllers())
    w(f"{RP}/entity/player.entity.json", player_client_entity())
    for name, j in particles().items():
        w(f"{RP}/particles/{name}.json", j)
    lang = {
        "en_US": {"item.hulk:radioactive_apple": "Radioactive Apple", "item.hulk:calm_pill": "Calming Pill"},
        "tr_TR": {"item.hulk:radioactive_apple": "Radyoaktif Elma", "item.hulk:calm_pill": "Sakinleştirici Hap"},
    }
    for lc, d in lang.items():
        txt = "".join(f"{k}={v}\n" for k, v in d.items()) + "".join(f"{k}.name={v}\n" for k, v in d.items())
        w(f"{RP}/texts/{lc}.lang", txt)
    w(f"{RP}/texts/languages.json", ["en_US", "tr_TR"])


def controller_states(states):
    """states: ordered {name: (anims, cond)}.  Conditions are made mutually exclusive by priority
    (first listed wins), so exactly one state is valid at any time and nothing oscillates."""
    order = list(states)
    excl = {}
    prev = []
    for n in order:
        c = f"({states[n][1]})"
        if prev:
            c += " && !(" + " || ".join(prev) + ")"
        excl[n] = c
        prev.append(f"({states[n][1]})")
    out = {}
    for name, (an, _) in states.items():
        st = {"blend_transition": 0.18, "transitions": [{o: excl[o]} for o in order if o != name]}
        if an:
            st["animations"] = an
        out[name] = st
    return out


def animation_controllers():
    A = "q.property('hulk:anim')"
    loco = {
        "off": ([], "!q.property('hulk:active')"),
        "busy": ([], f"{A} != 0"),
        "swim": (["hulk_swim"], "q.is_in_water && !q.is_on_ground"),
        "sneak": (["hulk_sneak"], "q.is_sneaking && q.is_on_ground"),
        "jump": (["hulk_jump"], "!q.is_on_ground && q.vertical_speed > 0.1"),
        "fall": (["hulk_fall"], "!q.is_on_ground"),
        "run": ([{"hulk_run": "math.clamp(q.modified_move_speed * 1.3, 0.45, 1.0)"}], "q.is_sprinting && q.modified_move_speed > 0.15"),
        "walk": ([{"hulk_walk": "math.clamp(q.modified_move_speed * 1.6, 0.35, 1.0)"}], "q.modified_move_speed > 0.04"),
        "idle": (["hulk_idle"], "1"),
    }
    # priority ordering for transitions: off, busy first, then rest
    order = ["off", "busy", "swim", "sneak", "jump", "fall", "run", "walk", "idle"]
    loco = {k: loco[k] for k in order}
    ls = controller_states(loco)
    for k in ls:  # guard: while 'off'/'busy' both must exclude other conditions
        pass
    # make non-off/busy states' conditions exclusive of off/busy (they come first in list so fine)
    action_states = {"none": ([], f"{A} == 0")}
    for n, i in (("transform", 1), ("revert", 2), ("roar", 3), ("smash", 4), ("clap", 5), ("land", 6), ("dive", 7)):
        action_states[n] = ([f"hulk_{n}"], f"{A} == {i}")
    ast = controller_states(action_states)
    attack = {
        "none": ([], "!(variable.attack_time > 0.0 && " + A + " == 0 && q.property('hulk:active'))"),
        "punch": (["hulk_attack"], "variable.attack_time > 0.0 && " + A + " == 0 && q.property('hulk:active')"),
    }
    # 'none' should not be reachable by evaluating its own inverse; build manually
    atk = {
        "initial_state": "none",
        "states": {
            "none": {"transitions": [{"punch": "variable.attack_time > 0.0 && " + A + " == 0"}]},
            "punch": {"animations": ["hulk_attack"], "blend_transition": 0.06,
                      "transitions": [{"none": "variable.attack_time <= 0.0 || " + A + " != 0"}]},
        },
    }
    return {
        "format_version": "1.10.0",
        "animation_controllers": {
            "controller.animation.hulk.loco": {"initial_state": "off", "states": ls},
            "controller.animation.hulk.action": {"initial_state": "none", "states": ast},
            "controller.animation.hulk.attack": atk,
        },
    }


def render_controllers():
    tex = ["Texture.hulk0", "Texture.hulk1", "Texture.hulk2", "Texture.hulk3", "Texture.hulk4"]
    arrays = {"textures": {"Array.skins": tex}}
    idx = "Array.skins[math.clamp(q.property('hulk:stage'), 0, 4)]"
    return {
        "format_version": "1.8.0",
        "render_controllers": {
            "controller.render.hulk": {
                "arrays": arrays, "geometry": "Geometry.hulk",
                "materials": [{"*": "Material.default"}], "textures": [idx],
            },
            "controller.render.hulk_fp": {
                "arrays": arrays, "geometry": "Geometry.hulk_fp",
                "materials": [{"*": "Material.default"}], "textures": [idx],
                "part_visibility": [
                    {"*": False},
                    {"rightArm": "query.get_equipped_item_name(0, 1) == '' || query.get_equipped_item_name(0, 1) == 'filled_map'"},
                    {"leftArm": "(query.get_equipped_item_name(0, 1) == 'filled_map' && query.get_equipped_item_name('off_hand') != 'shield') || (query.get_equipped_item_name('off_hand') == 'filled_map' && !query.item_is_charged) || (!query.item_is_charged && (variable.item_use_normalized > 0 && variable.item_use_normalized < 1.0))"},
                ],
            },
        },
    }


def player_client_entity():
    with open(os.path.join(HERE, "vanilla", "player.entity.json"), encoding="utf-8") as f:
        d = json.load(f)
    desc = d["minecraft:client_entity"]["description"]
    for i in range(5):
        desc["textures"][f"hulk{i}"] = f"textures/entity/hulk/hulk_s{i}"
    desc["geometry"]["hulk"] = "geometry.hulk"
    desc["geometry"]["hulk_fp"] = "geometry.hulk_fp"
    desc["scripts"]["scale"] = "q.property('hulk:active') ? 1.0 : 0.9375"
    an = desc["animations"]
    for n in ("idle", "walk", "run", "jump", "fall", "sneak", "swim", "transform", "revert", "roar", "smash", "clap", "land", "dive", "attack", "look"):
        an[f"hulk_{n}"] = f"animation.hulk.{n}"
    an["hulk_loco"] = "controller.animation.hulk.loco"
    an["hulk_action"] = "controller.animation.hulk.action"
    an["hulk_attack_ctl"] = "controller.animation.hulk.attack"
    on = "q.property('hulk:active')"
    desc["scripts"]["animate"] = ["root",
                                  {"hulk_look": on}, {"hulk_loco": on}, {"hulk_action": on}, {"hulk_attack_ctl": on}]
    rcs = desc["render_controllers"]
    for rc in rcs:
        for k in list(rc):
            if k in ("controller.render.player.first_person", "controller.render.player.third_person"):
                rc[k] = f"({rc[k]}) && !{on}"
    rcs.append({"controller.render.hulk": f"{on} && !variable.is_first_person && !variable.map_face_icon && !query.is_spectator"})
    rcs.append({"controller.render.hulk_fp": f"{on} && variable.is_first_person && !query.is_spectator"})
    return d


def particle(ident, cell, count, life, speed, size, gradient, accel=(0, 0, 0), drag=0.0, shape=None, facing="lookat_xyz", active=1.0, mat="particles_blend", spin=False):
    comps = {
        "minecraft:emitter_rate_instant": {"num_particles": count},
        "minecraft:emitter_lifetime_once": {"active_time": active},
        "minecraft:emitter_shape_sphere": shape or {"radius": 0.5, "direction": "outwards"},
        "minecraft:particle_lifetime_expression": {"max_lifetime": life},
        "minecraft:particle_initial_speed": speed,
        "minecraft:particle_motion_dynamic": {"linear_acceleration": list(accel), "linear_drag_coefficient": drag},
        "minecraft:particle_appearance_billboard": {
            "size": [size, size], "facing_camera_mode": facing,
            "uv": {"texture_width": 128, "texture_height": 128, "uv": [cell * 32, 0], "uv_size": [32, 32]},
        },
        "minecraft:particle_appearance_tinting": {
            "color": {"interpolant": "v.particle_age / v.particle_lifetime", "gradient": gradient}},
    }
    return {"format_version": "1.10.0", "particle_effect": {
        "description": {"identifier": ident, "basic_render_parameters": {"material": mat, "texture": "textures/particle/hulk_particles"}},
        "components": comps}}


def particles():
    P = {}
    P["gamma_burst"] = particle("hulk:gamma_burst", 0, 34, "0.9 + math.random(0, 0.7)", "2.0 + math.random(0, 3.0)",
                                "0.34 * (1 - v.particle_age / v.particle_lifetime) + 0.05",
                                {"0.0": "#FFD8FF6A", "0.4": "#FF6CF03C", "1.0": "#0020A020"}, accel=(0, 2.2, 0), drag=1.8,
                                shape={"radius": 0.7, "direction": "outwards", "offset": [0, 1.2, 0]})
    P["gamma_aura"] = particle("hulk:gamma_aura", 0, 3, "0.7 + math.random(0, 0.5)", "0.2",
                               "0.22 * (1 - v.particle_age / v.particle_lifetime) + 0.04",
                               {"0.0": "#CCA0FF50", "1.0": "#0030B020"}, accel=(0, 1.6, 0), drag=0.5,
                               shape={"radius": 0.9, "direction": "inwards", "offset": [0, 1.4, 0], "plane_normal": [0, 1, 0]} if False else {"radius": 0.9, "direction": "outwards", "offset": [0, 1.4, 0]})
    P["shockwave"] = particle("hulk:shockwave", 1, 1, "0.55", "0", "1.0 + (v.particle_age / v.particle_lifetime) * 16",
                              {"0.0": "#FFF0FFB0", "0.5": "#C0B0FF70", "1.0": "#0090D060"}, facing="emitter_transform_xz",
                              shape={"radius": 0.01, "direction": "outwards", "offset": [0, 0.08, 0]})
    P["shockwave_big"] = particle("hulk:shockwave_big", 1, 1, "0.8", "0", "2.0 + (v.particle_age / v.particle_lifetime) * 28",
                                  {"0.0": "#FFFFFFB0", "0.5": "#C0C0FF80", "1.0": "#00A0D070"}, facing="emitter_transform_xz",
                                  shape={"radius": 0.01, "direction": "outwards", "offset": [0, 0.08, 0]})
    P["debris"] = particle("hulk:debris", 2, 22, "0.8 + math.random(0, 0.6)", "3.0 + math.random(0, 4.0)",
                           "0.16 + math.random(0, 0.08)", {"0.0": "#FFFFFFFF", "0.8": "#FFC8C8C8", "1.0": "#00FFFFFF"},
                           accel=(0, -14, 0), drag=0.6, shape={"radius": 0.4, "direction": "outwards", "offset": [0, 0.3, 0]}, mat="particles_alpha")
    P["cloth"] = particle("hulk:cloth", 3, 16, "1.0 + math.random(0, 0.8)", "1.5 + math.random(0, 2.5)",
                          "0.2 + math.random(0, 0.12)", {"0.0": "#FFFFFFFF", "0.8": "#FFD0D0D0", "1.0": "#00FFFFFF"},
                          accel=(0, -4, 0), drag=1.6, shape={"radius": 0.5, "direction": "outwards", "offset": [0, 1.2, 0]}, mat="particles_alpha")
    return P


# ------------------------------------------------------------------------------ BP
def build_bp():
    if os.path.exists(BP):
        shutil.rmtree(BP)
    w(f"{BP}/manifest.json", {
        "format_version": 2,
        "header": {"name": "§aHulk Mod §7BP", "description": "Radyoaktif elmayı ye, HULK ol!",
                   "uuid": UUID["bp"], "version": VERSION, "min_engine_version": [1, 21, 60]},
        "modules": [
            {"type": "data", "uuid": UUID["bp_data"], "version": VERSION},
            {"type": "script", "language": "javascript", "uuid": UUID["bp_script"], "version": VERSION, "entry": "scripts/main.js"},
        ],
        "dependencies": [
            {"uuid": UUID["rp"], "version": VERSION},
            {"module_name": "@minecraft/server", "version": "1.16.0"},
        ],
    })
    shutil.copy(os.path.join(ROOT, "HulkMod_RP", "pack_icon.png"), f"{BP}/pack_icon.png")
    os.makedirs(f"{BP}/scripts", exist_ok=True)
    shutil.copy(os.path.join(HERE, "main.js"), f"{BP}/scripts/main.js")

    def food(ident, icon, nutrition, stack=16, use=1.6):
        # same shape as the vanilla apple item (format 1.20.x): numeric saturation, texture icon object
        return {
            "format_version": "1.20.80",
            "minecraft:item": {
                "description": {"identifier": ident,
                                "menu_category": {"category": "items", "group": "itemGroup.name.miscFood"}},
                "components": {
                    "minecraft:icon": {"texture": icon},
                    "minecraft:display_name": {"value": f"item.{ident}.name"},
                    "minecraft:max_stack_size": stack,
                    "minecraft:use_animation": "eat",
                    "minecraft:use_modifiers": {"use_duration": use, "movement_modifier": 0.35},
                    "minecraft:food": {"nutrition": nutrition, "saturation_modifier": 0.6, "can_always_eat": True},
                },
            },
        }
    w(f"{BP}/items/radioactive_apple.json", food("hulk:radioactive_apple", "hulk_radioactive_apple", 6, 16, 1.6))
    w(f"{BP}/items/calm_pill.json", food("hulk:calm_pill", "hulk_calm_pill", 1, 16, 1.2))
    w(f"{BP}/recipes/radioactive_apple.json", {
        "format_version": "1.20.10",
        "minecraft:recipe_shapeless": {
            "description": {"identifier": "hulk:radioactive_apple"}, "tags": ["crafting_table"],
            "ingredients": [{"item": "minecraft:apple"}, {"item": "minecraft:glowstone_dust"}, {"item": "minecraft:glowstone_dust"},
                            {"item": "minecraft:slime_ball"}, {"item": "minecraft:slime_ball"}],
            "result": {"item": "hulk:radioactive_apple", "count": 1}}})
    w(f"{BP}/recipes/calm_pill.json", {
        "format_version": "1.20.10",
        "minecraft:recipe_shapeless": {
            "description": {"identifier": "hulk:calm_pill"}, "tags": ["crafting_table"],
            "ingredients": [{"item": "minecraft:lapis_lazuli"}, {"item": "minecraft:sugar"}, {"item": "minecraft:bone_meal"}],
            "result": {"item": "hulk:calm_pill", "count": 2}}})
    w(f"{BP}/entities/player.json", player_bp())
    w(f"{BP}/texts/languages.json", ["en_US", "tr_TR"])
    w(f"{BP}/texts/en_US.lang", "pack.name=Hulk Mod\npack.description=Eat the Radioactive Apple and become the HULK!\n")
    w(f"{BP}/texts/tr_TR.lang", "pack.name=Hulk Modu\npack.description=Radyoaktif Elmayı ye ve HULK ol!\n")


def player_bp():
    with open(os.path.join(HERE, "vanilla", "player.json"), encoding="utf-8") as f:
        d = json.load(f)
    e = d["minecraft:entity"]
    e["description"]["properties"] = {
        "hulk:active": {"type": "bool", "default": False, "client_sync": True},
        "hulk:stage": {"type": "int", "range": [0, 4], "default": 0, "client_sync": True},
        "hulk:anim": {"type": "int", "range": [0, 9], "default": 0, "client_sync": True},
    }
    e["component_groups"]["hulk:form"] = {"minecraft:collision_box": {"width": 1.4, "height": 3.0}}
    e["events"]["hulk:transform"] = {"add": {"component_groups": ["hulk:form"]}}
    e["events"]["hulk:revert"] = {"remove": {"component_groups": ["hulk:form"]}}
    return d


def make_mcaddon():
    os.makedirs(DIST, exist_ok=True)
    out = os.path.join(DIST, "HulkMod.mcaddon")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for base in (BP, RP):
            for dp, _, fs in os.walk(base):
                for f in fs:
                    p = os.path.join(dp, f)
                    z.write(p, os.path.join(os.path.basename(base), os.path.relpath(p, base)))
    return out


def main():
    model = build_model()
    model.auto_pack()
    print("packed cubes:", len(model.cubes), "atlas rows used", model.used_h)
    atlases = [paint.paint_atlas(model, s) for s in range(5)]
    build_rp(model, atlases)
    build_bp()
    out = make_mcaddon()
    print("wrote", out, round(os.path.getsize(out) / 1e6, 2), "MB")


if __name__ == "__main__":
    main()
