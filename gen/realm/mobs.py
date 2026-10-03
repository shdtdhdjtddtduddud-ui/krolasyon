"""Every creature of the Crimson Realm: look (model, texture, animation) and gameplay (stats, abilities, spawns, loot)."""
import json
import math
import os
import numpy as np
from core import Model
from paint import Painter
import mats as mt
import kit
import anims as an

M_ID = 'krolasyonbosses'

# ---------------------------------------------------------------- palettes
ASH_ARMOR = ['#0c0a0a', '#262020', '#463c38', '#6e625a', '#a09080']
ASH_SKIN = ['#141212', '#2e2a28', '#504a46', '#7a726c']
BLOOD_ARMOR = ['#1a0206', '#4a0812', '#7a121e', '#a82a32', '#d86a60']
PALE = ['#6a5a60', '#a8969a', '#d8c8c8', '#f4ecec']
SHADOW_CLOTH = ['#06040a', '#120a1e', '#221436', '#3a2258']
OBSIDIAN = ['#040208', '#0e0818', '#1e1230', '#342050', '#5a3a80']
LEGION_IRON = ['#0e0e10', '#26262a', '#46464c', '#707078', '#a8a8b0']
BRASS = ['#3a2408', '#7a5214', '#c08a24', '#f0c850']
IMP_SKIN = ['#3a0a06', '#6a160c', '#9a2a14', '#c84a24']
SOUL_CLOTH = ['#041418', '#0a2a32', '#145060', '#2a7a8a']
GHOST = ['#2a6a7a', '#60b8c8', '#a8f0f8', '#e8ffff']
BONE = ('#3a342c', '#9a8e78', '#d4c8b0', '#f4ecdc')
STONE = ['#1a1616', '#363030', '#5a5250', '#847a76']
CRIMSON_GOLD = ['#3a0006', '#7a0a12', '#b8202a', '#e85a40']

FIRE_FLAME = ('#ff2a10', '#ff8a20', '#ffe070')
SOUL_FLAME = ('#1a7aa0', '#40d0f0', '#d0ffff')
SHADOW_FLAME = ('#3a0a80', '#9a40ff', '#e8c8ff')


def std_mats(**over):
    m = {
        'claw': mt.bone(('#1a1414', '#4a4038', '#8a8070', '#c8c0b0'), crack=False),
        'blade': mt.metal(['#2a2a30', '#6a6a74', '#b8b8c4', '#ffffff']),
        'grip': mt.wood(),
        'guard': mt.plate(BRASS, rivets=None),
        'orb': mt.crystal(['#5a0a10', '#c01020', '#ff6060', '#ffd0d0'], glow_col='#ff3040'),
        'string': mt.solid('#e8e0d0'),
        'fin': mt.membrane(['#3a0a0a', '#8a1a14', '#c84020', '#ff8a40'], vein='#2a0404'),
        'flame': mt.flame(FIRE_FLAME),
    }
    m.update(over)
    return m


def A(type_, slot, cd, minr, maxr, power=1.0, radius=0.0, count=1, el='FIRE', summon='', weight=3, hit=None, dur=None):
    return dict(type=type_, slot=slot, cd=cd, minr=minr, maxr=maxr, power=power, radius=radius, count=count, el=el, summon=summon,
                weight=weight, hit=hit, dur=dur)


class Spec:
    def __init__(self, id, tr, en, faction, kind, build, stats, abilities, spawns=(), sounds=None, egg=(0x333333, 0xaa2222),
                 scale=0.5, shadow=0.5, translucent=False, walk_speed=1.0, run_speed=1.0, head='head', loot=None, special_len=1.6,
                 category='monster', desc=None, xp=10):
        self.id, self.tr, self.en, self.faction, self.kind = id, tr, en, faction, kind
        self.build_fn, self.stats, self.abilities = build, stats, abilities
        self.spawns = list(spawns)
        self.sounds = sounds or {}
        self.egg = egg
        self.scale, self.shadow, self.translucent = scale, shadow, translucent
        self.walk_speed, self.run_speed, self.head = walk_speed, run_speed, head
        self.loot = loot
        self.special_len = special_len
        self.category = 'creature' if kind == 'ENVOY' else category
        self.desc = desc
        self.xp = xp

    def lang(self):
        L = {f'entity.{M_ID}.{self.id}': (self.tr, self.en), f'item.{M_ID}.{self.id}_spawn_egg': (f'{self.tr} Çağırma Yumurtası', f'{self.en} Spawn Egg')}
        return L

    def pascal(self):
        return ''.join(w.capitalize() for w in self.id.split('_'))


SPECS = []


def spec(*a, **k):
    s = Spec(*a, **k)
    SPECS.append(s)
    return s


def snd(amb, hurt, death, step='', pitch=1.0):
    return dict(ambient=amb, hurt=hurt, death=death, step=step, pitch=pitch)


def stats(hp, dmg, armor=2, speed=0.25, kb=0.0, prefer=0.0, fire=False, size=(0.8, 2.0)):
    return dict(hp=hp, dmg=dmg, armor=armor, speed=speed, kb=kb, prefer=prefer, fire=fire, size=size)


def essence_loot(faction, extra=()):
    ess = {'ash': 'ash_essence', 'blood': 'blood_vial', 'shadow': 'shadow_shard', 'legion': 'brimstone', 'soul': 'soul_essence'}.get(faction)
    pools = []
    if ess:
        pools.append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{M_ID}:{ess}", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}},
            {"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}],
                      "conditions": [{"condition": "minecraft:killed_by_player"}]})
    for item, chance, lo, hi in extra:
        pools.append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": item if ':' in item else f"{M_ID}:{item}", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}],
                      "conditions": [{"condition": "minecraft:random_chance_with_looting", "chance": chance, "looting_multiplier": chance * 0.5}]})
    return {"type": "minecraft:entity", "pools": pools}


def boss_loot(faction, weapon, extra=()):
    ess = {'ash': 'ash_essence', 'blood': 'blood_vial', 'shadow': 'shadow_shard', 'legion': 'brimstone', 'soul': 'soul_essence'}
    items = [(weapon, 1, 1)] if weapon else []
    if faction:
        items.append((ess[faction], 8, 16))
    else:
        items += [(e, 3, 6) for e in ess.values()]
    items += [('arcane_crystal', 4, 8), ('infernal_steel_ingot', 3, 6), ('mana_potion', 2, 4)] + list(extra)
    return {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{M_ID}:{i}" if ':' not in i else i, "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}]} for i, lo, hi in items]}


# =================================================================== builders
def humanoid_mob(name, p, look, st, extra=None, tex_w=256):
    """generic humanoid builder: p = kit.humanoid params, look = callable(M, g) adding accessories, returns build fn"""
    def build():
        M = Model(name, tex_w, tex_w)
        g = kit.humanoid(M, p)
        info = look(M, g) or {}
        sty = dict(st)
        sty.update(info)
        return M, kit_anims('humanoid', sty, M)
    return build


def kit_anims(kind, st, M, extra=None):
    if kind == 'humanoid':
        return an.humanoid_set(st)
    if kind == 'quad':
        return an.quad_set(st)
    if kind == 'floater':
        return an.floater_set(st)
    if kind == 'flyer':
        return an.flyer_set(st)
    if kind == 'golem':
        return an.golem_set(st)
    raise ValueError(kind)


MAT_REGISTRY = {}


def register_mats(name, mats):
    m = std_mats(**mats)
    for k in list(m):
        if m[k] is not None and k + '_hood' not in m:
            m[k + '_hood'] = mt.open_front(m[k])
            m[k + '_helm'] = mt.open_front(m[k], keep_top=0.42)
    MAT_REGISTRY[name] = m


# =================================================================== ASH KINGDOM
register_mats('ash_walker', {
    'skin': mt.ash(), 'head': mt.with_face(mt.ash(), 'eyes2', eye='#ff8a20', mouth='#200606'),
    'hips': mt.cloth(['#0a0808', '#1e1a18', '#322c28', '#4a423c'], tattered=True)})


def b_ash_walker():
    M = Model('ash_walker', 256, 256)
    g = kit.humanoid(M, dict(height=62, hunch=16, chest=(15, 13, 9), waist=(11, 8, 7), hips=(13, 6, 8), head=(9, 10, 9), arm_w=4.8, leg_w=5.5,
                             arm_len=30, claws=4, jaw=(7, 3, 6), mats=dict(skin='skin', armor='skin', head='head', hips='hips', claw='claw')))
    kit.back_spikes(M, 'chest', -10, 4, 3, 'skin', h=4, spread=3)
    return M, an.humanoid_set(dict(weapon='claw', special='roar', death='dissolve', heavy=0.8, arm_swing=1.3))


spec('ash_walker', 'Kül Yürüyeni', 'Ash Walker', 'ash', 'GROUND', b_ash_walker,
     stats(30, 5, 2, 0.25, size=(0.8, 1.95)),
     [A('MELEE', 2, 18, 0, 2.6, el='FIRE'), A('LEAP', 4, 120, 4, 10, power=1.2, radius=2.5, el='FIRE', weight=2)],
     spawns=[('ash_wastes', 100, 2, 4), ('throne_wastes', 25, 1, 2)], sounds=snd('minecraft:entity.husk.ambient', 'minecraft:entity.husk.hurt',
                                                                                    'minecraft:entity.husk.death', 'minecraft:entity.husk.step', 0.75),
     egg=(0x3a3634, 0xff7a20), loot=essence_loot('ash', [('minecraft:coal', 0.4, 1, 2)]))

register_mats('ember_hound', {
    'body': mt.magma(), 'head': mt.with_face(mt.magma(), 'beast', eye='#ffe070'), 'leg': mt.magma(density=0.8),
    'tail': mt.magma(), 'mane': mt.flame(FIRE_FLAME)})


def b_ember_hound():
    M = Model('ember_hound', 256, 256)
    g = kit.quadruped(M, dict(length=34, height=20, girth=(13, 12), neck=(8, -30), head=(10, 9, 10), snout=(8, 6, 5), leg_w=4.5,
                              tail=(3, 6, 3), tail_droop=-10, mats=dict(body='body', head='head', leg='leg', tail='tail')))
    for k, yr in enumerate((0, 90)):
        kit.lb(M, f'mane{k}', 'neck', (0, -4, -3), (0, yr, 0))
        M.cube_l(f'mane{k}', (-7, -12, 0), (14, 13, 0), 'mane', plane=True)
    kit.lb(M, 'tailfire', 'tail2', (0, 0, 6), (-70, 0, 0))
    M.cube_l('tailfire', (-5, -12, 0), (10, 12, 0), 'mane', plane=True)
    return M, an.quad_set(dict(tail=g['tails'], flicker=['mane0', 'mane1', 'tailfire'], special='charge'))


spec('ember_hound', 'Köz Tazısı', 'Ember Hound', 'ash', 'GROUND', b_ember_hound,
     stats(24, 5, 1, 0.33, fire=True, size=(1.0, 1.1)),
     [A('MELEE', 2, 16, 0, 2.4, el='FIRE'), A('LEAP', 3, 90, 3, 11, power=1.0, radius=2.2, el='FIRE', hit=8, dur=22),
      A('BREATH', 4, 140, 0, 6, power=0.8, el='FIRE', hit=6, dur=32)],
     spawns=[('ash_wastes', 70, 2, 3), ('basalt_warfields', 30, 1, 3)], sounds=snd('minecraft:entity.wolf.growl', 'minecraft:entity.wolf.hurt',
                                                                                  'minecraft:entity.wolf.death', 'minecraft:entity.wolf.step', 0.7),
     egg=(0x2a1210, 0xff6a10), walk_speed=1.6, run_speed=1.2, loot=essence_loot('ash', [('minecraft:blaze_powder', 0.3, 1, 2)]))

register_mats('magma_tortoise', {
    'body': mt.scales(['#1a0a06', '#3a1a10', '#5a2a1a', '#7a3a22'], rim='#0a0404'), 'head': mt.with_face(mt.scales(['#1a0a06', '#3a1a10', '#5a2a1a', '#7a3a22']), 'eyes2', eye='#ffb020'),
    'shell': mt.magma(rock=('#0a0606', '#1a1210', '#2a1e1a', '#3a2c26'), density=1.6), 'vent': mt.magma(density=3.0)})


def b_magma_tortoise():
    M = Model('magma_tortoise', 256, 256)
    g = kit.quadruped(M, dict(length=36, height=12, girth=(22, 12), neck=(7, -10), head=(10, 9, 10), snout=(5, 7, 5), leg_w=7, stubby=True,
                              tail=(2, 5, 4), tail_droop=30, mats=dict(body='body', head='head', leg='body', tail='body')))
    top = g['top']
    M.cube('body', (-15, top - 7, -20), (30, 9, 34), 'shell')
    M.cube('body', (-12, top - 12, -16), (24, 6, 26), 'shell')
    M.cube('body', (-8, top - 16, -11), (16, 5, 16), 'shell')
    for k, (x, z) in enumerate(((-6, -8), (5, -2), (-3, 5), (7, 8))):
        M.cube('body', (x - 2, top - 19 + k % 2, z - 2), (4, 4 + k % 2 * 2, 4), 'vent')
    return M, an.quad_set(dict(tail=g['tails'], special='stomp', fall=4))


spec('magma_tortoise', 'Magma Kaplumbağası', 'Magma Tortoise', 'ash', 'GROUND', b_magma_tortoise,
     stats(60, 7, 12, 0.16, kb=0.8, prefer=8, fire=True, size=(1.6, 1.3)),
     [A('MELEE', 2, 24, 0, 2.8, power=1.0, el='FIRE'), A('BOMB', 3, 70, 4, 18, power=1.1, radius=2.8, count=2, el='FIRE', weight=4),
      A('SHIELD', 4, 260, 0, 8, el='FIRE', weight=1), A('SLAM', 4, 160, 0, 4, power=1.0, radius=4, el='FIRE', weight=2)],
     spawns=[('ash_wastes', 30, 1, 1)], sounds=snd('minecraft:entity.turtle.ambient_land', 'minecraft:entity.turtle.hurt_baby',
                                                     'minecraft:entity.turtle.death', 'minecraft:entity.ravager.step', 0.5),
     egg=(0x3a1a10, 0xffa020), walk_speed=2.0, run_speed=1.5, loot=essence_loot('ash', [('minecraft:magma_cream', 0.5, 1, 3)]), xp=20)

register_mats('ash_knight', {
    'armor': mt.plate(ASH_ARMOR, trim='#ff7a20', engrave='#ff9a40', glow_engrave=True), 'skin': mt.ash(),
    'head': mt.with_face(mt.plate(ASH_ARMOR, trim='#ff7a20'), 'visor', eye='#ff8a20'),
    'cape': mt.cloth(['#1a0a06', '#3a1408', '#5a200c', '#7a3010'], trim='#ff8a20', glyph='#ffb040', tattered=True),
    'guard': mt.plate(['#0a0808', '#262020', '#46403a', '#6a625a'], trim='#ff7a20'),
    'blade': mt.metal(['#2a1a14', '#6a4a3a', '#c8a080', '#fff0d0'], glow='#ff8a30')})


def b_ash_knight():
    M = Model('ash_knight', 256, 256)
    g = kit.humanoid(M, dict(height=66, chest=(18, 14, 10), waist=(13, 8, 8), hips=(15, 6, 9), head=(10, 11, 10), arm_w=5.5, leg_w=6.5,
                             shoulder=8, mats=dict(skin='armor', armor='armor', head='head', arm='armor', leg='armor')))
    kit.helmet(M, g, 'armor_helm', crest='cape')
    kit.cape(M, g, 'cape')
    kit.weapon(M, 'right_hand', 'sword', {'blade': 'blade', 'grip': 'grip', 'guard': 'guard'}, 1.1)
    kit.weapon(M, 'left_hand', 'shield', {'blade': 'armor', 'guard': 'guard'}, 1.0, flip=True)
    return M, an.humanoid_set(dict(weapon='slash', special='charge', death='knees', cape=True, arm_swing=0.6))


spec('ash_knight', 'Kül Şövalyesi', 'Ash Knight', 'ash', 'GROUND', b_ash_knight,
     stats(40, 7, 8, 0.24, kb=0.3, size=(0.9, 2.1)),
     [A('MELEE', 2, 20, 0, 3.0, el='FIRE'), A('CHARGE', 4, 140, 5, 14, power=1.3, el='FIRE', hit=10, dur=32),
      A('SHIELD', 3, 300, 0, 10, el='FIRE', weight=1)],
     spawns=[('ash_wastes', 60, 1, 3)], sounds=snd('minecraft:entity.wither_skeleton.ambient', 'minecraft:entity.wither_skeleton.hurt',
                                                     'minecraft:entity.wither_skeleton.death', 'minecraft:entity.iron_golem.step', 0.8),
     egg=(0x262020, 0xff7a20), loot=essence_loot('ash', [('raw_infernal_steel', 0.25, 1, 2), ('infernal_steel_sword', 0.02, 1, 1)]), xp=15)

register_mats('flame_djinn', {
    'chest': mt.magma(density=1.0), 'head': mt.with_face(mt.magma(density=0.6), 'eyes2', eye='#fff0a0', mouth='#3a0a04'),
    'arm': mt.magma(density=1.2), 'tail': mt.flame(FIRE_FLAME), 'horn': mt.bone(('#1a0e08', '#3a2010', '#6a3a18', '#9a5a20')),
    'gold': mt.plate(BRASS, rivets=None)})


def b_flame_djinn():
    M = Model('flame_djinn', 256, 256)
    g = kit.floater(M, dict(hover=10, chest=(15, 13, 9), head=(9, 10, 9), arm_w=4.5, arm_len=22, tail_len=20, tail_segs=3, flames=True,
                            mats=dict(chest='chest', head='head', arm='arm', tail='chest', flame='tail')))
    kit.horns(M, g, 'long', 'horn', 0.8)
    for side in ('right', 'left'):
        M.cube_l(f'{side}_fore', (-3, 6, -3), (6, 3, 6), 'gold', inflate=0.3)
    return M, an.floater_set(dict(weapon='cast', cast='forward', special='spin', flicker=['flame0', 'flame1']))


spec('flame_djinn', 'Alev Cini', 'Flame Djinn', 'ash', 'FLYING', b_flame_djinn,
     stats(30, 5, 2, 0.28, prefer=9, fire=True, size=(0.9, 2.0)),
     [A('BOLT', 3, 40, 3, 20, power=1.1, radius=1.5, count=3, el='FIRE', weight=5), A('BLINK', 3, 140, 0, 12, el='FIRE', weight=1),
      A('NOVA', 4, 160, 0, 7, power=0.9, radius=1.2, count=10, el='FIRE', weight=2), A('MELEE', 2, 20, 0, 2.2, el='FIRE', weight=2)],
     spawns=[('ash_wastes', 35, 1, 2), ('basalt_warfields', 15, 1, 1)], sounds=snd('minecraft:entity.blaze.ambient', 'minecraft:entity.blaze.hurt',
                                                                                    'minecraft:entity.blaze.death', '', 0.8),
     egg=(0xff6a10, 0xffe070), translucent=False, loot=essence_loot('ash', [('minecraft:blaze_rod', 0.4, 1, 1), ('tome_fireball', 0.03, 1, 1)]), xp=15)

register_mats('cinder_beetle', {
    'body': mt.chitin(['#060404', '#1a1210', '#2e2220', '#4a3a34'], band='#ff6a10', glow='#ff7a20'),
    'head': mt.with_face(mt.chitin(['#060404', '#1a1210', '#2e2220', '#4a3a34']), 'eyes4', eye='#ff9a30'),
    'abdomen': mt.magma(density=2.0), 'leg': mt.chitin(['#060404', '#141010', '#241c1a', '#3a2e2a'])})


def b_cinder_beetle():
    M = Model('cinder_beetle', 128, 128)
    g = kit.arthropod(M, dict(body=(10, 6, 9), abdomen=(13, 9, 12), head=(7, 5, 5), pairs=3, leg_len=9, leg_w=1.8, lift=5, mandibles=4,
                              mats=dict(body='body', head='head', abdomen='abdomen', leg='leg')))
    return M, an.arthro_set(dict(), g['legs'])


spec('cinder_beetle', 'Kor Böceği', 'Cinder Beetle', 'ash', 'GROUND', b_cinder_beetle,
     stats(12, 3, 4, 0.3, fire=True, size=(0.7, 0.6)),
     [A('MELEE', 2, 16, 0, 1.8, el='FIRE'), A('EXPLODE', 4, 10, 0, 2.2, power=2.6, radius=3.0, el='FIRE', weight=6, hit=26, dur=32)],
     spawns=[('ash_wastes', 50, 3, 5), ('throne_wastes', 25, 2, 4)], sounds=snd('minecraft:entity.silverfish.ambient', 'minecraft:entity.silverfish.hurt',
                                                                                'minecraft:entity.silverfish.death', 'minecraft:entity.silverfish.step', 0.6),
     egg=(0x1a1210, 0xff6a10), walk_speed=2.5, loot=essence_loot('ash', [('minecraft:gunpowder', 0.5, 1, 2)]), xp=6)

# =================================================================== BLOOD DYNASTY
register_mats('bloodsucker', {
    'body': mt.fur(['#1a0206', '#3a0610', '#5a0c18', '#7a1420'], tip='#c02030'),
    'head': mt.with_face(mt.fur(['#1a0206', '#3a0610', '#5a0c18', '#7a1420']), 'eyes2', eye='#ff2030', mouth='#200206'),
    'membrane': mt.membrane(['#2a0208', '#5a0612', '#8a0a1a', '#b81a28'], vein='#1a0004', edge_col='#ff4050'),
    'wbone': mt.bone(('#2a0a0a', '#5a2a24', '#8a5a50', '#b88a80')), 'ear': mt.skin(['#3a0610', '#6a1018', '#9a2028', '#c84048'])})


def b_bloodsucker():
    M = Model('bloodsucker', 256, 256)
    kit.flyer(M, dict(body=(9, 8, 12), head=(8, 7, 7), span=30, ears=6, hover=14, tail=0,
                      mats=dict(body='body', head='head', membrane='membrane', wbone='wbone', ear='ear')))
    return M, an.flyer_set(dict())


spec('bloodsucker', 'Kan Emici', 'Bloodsucker', 'blood', 'FLYING', b_bloodsucker,
     stats(18, 4, 0, 0.32, prefer=0, size=(0.9, 0.9)),
     [A('MELEE', 2, 18, 0, 2.2, el='BLOOD'), A('DRAIN', 4, 120, 0, 7, power=0.8, el='BLOOD', hit=4, dur=32)],
     spawns=[('blood_marsh', 60, 2, 4), ('obsidian_forest', 15, 1, 2)], sounds=snd('minecraft:entity.bat.ambient', 'minecraft:entity.bat.hurt',
                                                                                  'minecraft:entity.bat.death', '', 0.6),
     egg=(0x3a0610, 0xc02030), loot=essence_loot('blood'))

register_mats('blood_guard', {
    'armor': mt.plate(BLOOD_ARMOR, trim='#e0b040', engrave='#ff4050', glow_engrave=True),
    'skin': mt.skin(PALE, fib=0.2),
    'head': mt.with_face(mt.skin(PALE, fib=0.1), 'eyes2', eye='#ff1020', mouth='#3a0008', teeth='#ffffff'),
    'cape': mt.cloth(['#1a0004', '#3a000a', '#5a0612', '#7a0a1a'], trim='#e0b040', glyph='#ff3040'),
    'blade': mt.metal(['#3a0008', '#8a1018', '#e04050', '#ffd0d0'], glow='#ff3040'), 'grip': mt.wood(('#0a0404', '#1a0a0a', '#2a1010', '#3a1818'))})


def b_blood_guard():
    M = Model('blood_guard', 256, 256)
    g = kit.humanoid(M, dict(height=70, chest=(16, 14, 9), waist=(11, 9, 7), hips=(13, 6, 8), head=(9, 11, 9), arm_w=5, leg_w=5.8, shoulder=7.5,
                             mats=dict(skin='skin', armor='armor', head='head', arm='armor', leg='armor', waist='armor')))
    kit.cape(M, g, 'cape')
    kit.horns(M, g, 'short', 'armor', 0.7)
    kit.weapon(M, 'right_hand', 'halberd', {'blade': 'blade', 'grip': 'grip'}, 1.0)
    return M, an.humanoid_set(dict(weapon='thrust', special='spin', death='back', cape=True, arm_swing=0.5))


spec('blood_guard', 'Kan Muhafızı', 'Blood Guard', 'blood', 'GROUND', b_blood_guard,
     stats(40, 7, 6, 0.25, size=(0.85, 2.2)),
     [A('MELEE', 2, 20, 0, 3.8, el='BLOOD'), A('SPIN', 4, 160, 0, 4, power=0.9, radius=4, el='BLOOD', hit=4, dur=32)],
     spawns=[('blood_marsh', 50, 1, 3)], sounds=snd('minecraft:entity.vindicator.ambient', 'minecraft:entity.vindicator.hurt',
                                                     'minecraft:entity.vindicator.death', 'minecraft:entity.iron_golem.step', 0.75),
     egg=(0x4a0812, 0xe0b040), loot=essence_loot('blood', [('minecraft:gold_ingot', 0.2, 1, 2)]), xp=15)

register_mats('flesh_abomination', {
    'body': mt.skin(['#3a0408', '#7a1018', '#b0302a', '#e06050'], fib=0.8, veins='#ff4030', spots='#5a0008'),
    'lump': mt.skin(['#4a0a10', '#8a2020', '#c04a40', '#f0a090'], fib=0.5),
    'maw': mt.with_face(mt.solid('#200206'), 'none', mouth='#000000'), 'arm': mt.skin(['#3a0408', '#6a0c14', '#9a2020', '#c84038'])})


def b_flesh_abomination():
    M = Model('flesh_abomination', 256, 256)
    g = kit.blob(M, dict(size=(28, 24, 24), lumps=9, arms=4, mats=dict(body='body', lump='lump', maw='maw', arm='arm')))
    for k, (x, y, z) in enumerate(((-9, -19, -12.6), (8, -13, -12.6), (2, -26, -9), (-12, -8, -9), (12, -22, -6))):
        M.cube('body', (x - 2, 24 + y - 2, z - 1), (4, 4, 1.5), 'eye')
    rs = np.random.default_rng(9)
    for k in range(7):
        a = rs.random() * math.tau
        n = f'bone{k}'
        kit.lb(M, n, 'body', (math.cos(a) * 11, -10 - rs.random() * 14, math.sin(a) * 10), (math.sin(a) * 50, 0, -math.cos(a) * 50))
        M.cube_l(n, (-1, -7 - rs.random() * 4, -1), (2, 9, 2), 'claw')
    kit.lb(M, 'maw2', 'body', (8, -6, -11), (0, -25, 0))
    M.cube_l('maw2', (-4, -2.5, -2), (8, 5, 2), 'maw')
    return M, an.blob_set(dict(arms=4))


MAT_REGISTRY['flesh_abomination']['eye'] = mt.with_face(mt.solid('#f0e0c0'), 'cyclops', eye='#ffb020', eye2='#000000')
MAT_REGISTRY['flesh_abomination']['claw'] = mt.bone(BONE)
spec('flesh_abomination', 'Et Yığını', 'Flesh Abomination', 'blood', 'GROUND', b_flesh_abomination,
     stats(70, 8, 3, 0.18, kb=0.6, prefer=6, size=(1.7, 1.8)),
     [A('MELEE', 2, 24, 0, 3.2, el='BLOOD'), A('BOMB', 3, 80, 4, 16, power=0.9, radius=2.5, count=2, el='BLOOD', weight=4),
      A('SUMMON', 4, 300, 0, 16, count=2, el='BLOOD', summon='blood_leech', weight=2)],
     spawns=[('blood_marsh', 20, 1, 1)], sounds=snd('minecraft:entity.slime.squish', 'minecraft:entity.slime.hurt', 'minecraft:entity.slime.death',
                                                     'minecraft:entity.slime.jump', 0.5),
     egg=(0x7a1018, 0xffd040), walk_speed=2.0, loot=essence_loot('blood', [('minecraft:rotten_flesh', 1.0, 2, 5)]), xp=25)

register_mats('blood_leech', {
    'body': mt.chitin(['#1a0004', '#4a0410', '#8a0a1c', '#c82030'], band='#100002', shine='#ff8090'),
    'head': mt.with_face(mt.chitin(['#1a0004', '#4a0410', '#8a0a1c', '#c82030']), 'none', mouth='#000000'),
    'jaw': mt.bone(('#3a0a0a', '#8a4a40', '#d0a090', '#fff0e0'))})


def b_blood_leech():
    M = Model('blood_leech', 128, 128)
    g = kit.serpent(M, dict(segs=5, width=6, seg_len=5, raise_=8, head=(7, 5, 7), mats=dict(body='body', head='head', jaw='jaw')))
    return M, an.serpent_set(dict(), g['segs'])


spec('blood_leech', 'Kan Sülüğü', 'Blood Leech', 'blood', 'GROUND', b_blood_leech,
     stats(14, 3, 1, 0.27, size=(0.6, 0.5)),
     [A('MELEE', 2, 14, 0, 1.8, el='BLOOD'), A('LEAP', 4, 70, 2, 8, power=1.0, radius=1.8, el='BLOOD')],
     spawns=[('blood_marsh', 40, 2, 4)], sounds=snd('minecraft:entity.silverfish.ambient', 'minecraft:entity.silverfish.hurt',
                                                     'minecraft:entity.silverfish.death', 'minecraft:entity.silverfish.step', 0.5),
     egg=(0x4a0410, 0xff8090), walk_speed=2.0, loot=essence_loot('blood'), xp=5)

register_mats('blood_witch', {
    'robe': mt.cloth(['#100004', '#2a000a', '#4a0614', '#6a0c1e'], trim='#e0b040', glyph='#ff3040', tattered=True),
    'skin': mt.skin(PALE, fib=0.1), 'head': mt.with_face(mt.cloth(['#100004', '#2a000a', '#4a0614', '#6a0c1e']), 'hood', eye='#ff2030'),
    'grip': mt.bone(BONE), 'guard': mt.plate(BRASS, rivets=None)})


def b_blood_witch():
    M = Model('blood_witch', 256, 256)
    g = kit.humanoid(M, dict(height=64, hunch=6, chest=(14, 13, 8), waist=(10, 8, 7), hips=(13, 6, 8), head=(9, 10, 9), arm_w=4.3, leg_w=5,
                             mats=dict(skin='skin', armor='robe', head='head', arm='robe', leg='robe', hand='skin')))
    kit.hood(M, g, 'robe_hood')
    kit.robe(M, g, 'robe')
    kit.weapon(M, 'right_hand', 'staff', {'grip': 'grip', 'guard': 'guard', 'orb': 'orb'}, 1.0)
    return M, an.humanoid_set(dict(weapon='cast', cast='raise', special='channel', death='dissolve', skirt=True, arm_swing=0.4, special_len=2.0))


spec('blood_witch', 'Kan Cadısı', 'Blood Witch', 'blood', 'GROUND', b_blood_witch,
     stats(30, 5, 1, 0.24, prefer=10, size=(0.8, 2.0)),
     [A('BOLT', 3, 50, 3, 20, power=1.2, count=2, el='BLOOD', weight=5), A('HEAL', 3, 220, 0, 20, power=1.0, radius=10, el='BLOOD', weight=2),
      A('ERUPT', 4, 160, 3, 14, power=1.1, count=8, el='BLOOD', weight=3), A('MELEE', 2, 20, 0, 2.2, power=0.6, el='BLOOD', weight=1)],
     spawns=[('blood_marsh', 30, 1, 2)], sounds=snd('minecraft:entity.witch.ambient', 'minecraft:entity.witch.hurt', 'minecraft:entity.witch.death', '', 0.9),
     egg=(0x2a000a, 0xff3040), special_len=2.0, loot=essence_loot('blood', [('tome_blood_lance', 0.03, 1, 1), ('arcane_crystal', 0.1, 1, 1)]), xp=15)

register_mats('bone_spider', {
    'body': mt.bone(BONE), 'head': mt.with_face(mt.bone(BONE), 'eyes4', eye='#ff2030'), 'abdomen': mt.bone(('#2a2420', '#6a5e50', '#a89a84', '#d8ccb4')),
    'leg': mt.bone(BONE), 'leg_tip': mt.bone(('#1a1410', '#3a3028', '#6a5a4a', '#8a7a6a'))})


def b_bone_spider():
    M = Model('bone_spider', 256, 256)
    g = kit.arthropod(M, dict(body=(14, 7, 12), abdomen=(17, 13, 17), head=(10, 7, 8), pairs=4, leg_len=17, leg_w=2.2, lift=7, mandibles=5,
                              mats=dict(body='body', head='head', abdomen='abdomen', leg='leg', leg_tip='leg_tip')))
    for i in range(3):
        M.cube_l('abdomen', (-7, -6.5 - 0.5, 3 + i * 4.5), (14, 1, 1.5), 'leg_tip')
    return M, an.arthro_set(dict(), g['legs'])


spec('bone_spider', 'Kemik Örümceği', 'Bone Spider', 'blood', 'GROUND', b_bone_spider,
     stats(28, 5, 3, 0.3, size=(1.4, 0.9)),
     [A('MELEE', 2, 16, 0, 2.4, el='POISON'), A('WEB', 3, 100, 3, 12, power=0.6, el='BONE', weight=3), A('LEAP', 4, 120, 4, 12, power=1.0, radius=2.4, el='POISON')],
     spawns=[('blood_marsh', 30, 1, 3), ('soul_valley', 25, 1, 2)], sounds=snd('minecraft:entity.spider.ambient', 'minecraft:entity.spider.hurt',
                                                                               'minecraft:entity.spider.death', 'minecraft:entity.spider.step', 0.7),
     egg=(0xd4c8b0, 0xff2030), walk_speed=1.8, loot=essence_loot('blood', [('minecraft:bone', 0.6, 1, 3), ('minecraft:string', 0.5, 1, 2)]), xp=10)

# =================================================================== SHADOW COUNCIL
register_mats('shadow_assassin', {
    'cloth': mt.cloth(SHADOW_CLOTH, trim='#8a40ff', glyph='#c890ff'), 'skin': mt.cloth(SHADOW_CLOTH, folds=False),
    'head': mt.with_face(mt.cloth(SHADOW_CLOTH), 'mask', eye='#c060ff'), 'blade': mt.metal(['#120820', '#3a2060', '#8a60d0', '#f0e0ff'], glow='#a050ff')})


def b_shadow_assassin():
    M = Model('shadow_assassin', 256, 256)
    g = kit.humanoid(M, dict(height=64, hunch=8, chest=(14, 13, 8), waist=(10, 8, 7), hips=(12, 6, 8), head=(9, 10, 9), arm_w=4.3, leg_w=5,
                             mats=dict(skin='skin', armor='cloth', head='head', arm='cloth', leg='cloth')))
    kit.hood(M, g, 'cloth_hood')
    kit.cape(M, g, 'cloth', length=34, width=12)
    kit.weapon(M, 'right_hand', 'dagger', {'blade': 'blade'}, 1.2)
    kit.weapon(M, 'left_hand', 'dagger', {'blade': 'blade'}, 1.2)
    return M, an.humanoid_set(dict(weapon='claw', special='spin', death='dissolve', cape=True, arm_swing=0.8, special_len=1.2))


spec('shadow_assassin', 'Gölge Suikastçısı', 'Shadow Assassin', 'shadow', 'GROUND', b_shadow_assassin,
     stats(26, 7, 2, 0.31, size=(0.8, 2.0)),
     [A('MELEE', 2, 14, 0, 2.6, el='SHADOW'), A('VANISH', 3, 240, 6, 20, el='SHADOW', weight=2), A('BLINK', 3, 100, 5, 18, el='SHADOW', weight=3),
      A('SPIN', 4, 140, 0, 3.5, power=0.8, radius=3.5, el='SHADOW', hit=3, dur=24)],
     spawns=[('obsidian_forest', 50, 1, 3)], sounds=snd('minecraft:entity.enderman.ambient', 'minecraft:entity.enderman.hurt',
                                                         'minecraft:entity.enderman.death', '', 1.3),
     egg=(0x120a1e, 0xa050ff), special_len=1.2, loot=essence_loot('shadow', [('tome_shadow_step', 0.03, 1, 1)]), xp=15)

register_mats('void_watcher', {
    'body': mt.chitin(OBSIDIAN[:4], band='#000000', shine='#c890ff'), 'tentacle': mt.skin(['#0a0414', '#1e0e30', '#3a1e5a', '#5a3080'], fib=0.6),
    'eye': None, 'lid': mt.chitin(OBSIDIAN[:4]), 'spike': mt.crystal(['#2a0a5a', '#6a2ab0', '#a060f0', '#e0c0ff'], glow_col='#b070ff')})


def _eye_mat(iris='#a040ff', pupil='#000000', sclera=('#d8c8e8', '#f8f0ff')):
    base = mt.chitin(OBSIDIAN[:4])

    def f(c, face, W, H, r):
        col, a, g = base(c, face, W, H, r)
        if face != 'north':
            return col, a, g
        yy, xx = np.mgrid[0:H, 0:W]
        d = np.hypot((xx - W / 2 + 0.5) / (W * 0.45), (yy - H / 2 + 0.5) / (H * 0.42))
        sc = d < 1
        col[sc] = mt._ramp(['#5a4a6a', sclera[0], sclera[1]], 1 - d[sc] * 0.7)
        di = np.hypot(xx - W / 2 + 0.5, yy - H / 2 + 0.5)
        ir = di < min(W, H) * 0.28
        col[ir] = mt._ramp(['#1a0430', iris, '#f0d0ff'], 1 - di[ir] / (min(W, H) * 0.28))
        pu = (np.abs(xx - W / 2 + 0.5) < max(0.6, W * 0.05)) & (di < min(W, H) * 0.22)
        col[pu] = mt.hexc(pupil)
        gl = mt.glow_layer(H, W, iris, ir * 1.0)
        col[int(H * 0.35), int(W * 0.38)] = mt.hexc('#ffffff')
        return col, a, mt.merge_glow(g, gl)
    return f


MAT_REGISTRY['void_watcher']['eye'] = _eye_mat()


def b_void_watcher():
    M = Model('void_watcher', 256, 256)
    g = kit.orb(M, dict(radius=9, hover=18, tentacles=6, t_len=7, spikes=5, mats=dict(body='body', eye='eye', tentacle='tentacle', lid='lid', spike='spike')))
    return M, an.orb_set(dict(special_len=2.4), g['tentacles'])


spec('void_watcher', 'Boşluk Gözcüsü', 'Void Watcher', 'shadow', 'FLYING', b_void_watcher,
     stats(34, 5, 4, 0.22, prefer=10, size=(1.1, 1.2)),
     [A('BEAM', 4, 120, 3, 16, power=1.0, el='SHADOW', hit=8, dur=48, weight=4), A('BOLT', 3, 50, 3, 20, power=1.0, count=2, el='SHADOW', weight=4),
      A('MELEE', 2, 20, 0, 2.0, el='SHADOW', weight=1)],
     spawns=[('obsidian_forest', 30, 1, 2)], sounds=snd('minecraft:entity.guardian.ambient', 'minecraft:entity.guardian.hurt',
                                                         'minecraft:entity.guardian.death', '', 0.6),
     egg=(0x1e1230, 0xa040ff), special_len=2.4, loot=essence_loot('shadow', [('minecraft:ender_pearl', 0.3, 1, 1)]), xp=15)

register_mats('nightmare_steed', {
    'body': mt.fur(['#04020a', '#0e0818', '#1a1028', '#2a1a40'], tip='#5a2a9a'), 'head': mt.with_face(mt.fur(['#04020a', '#0e0818', '#1a1028', '#2a1a40']), 'beast', eye='#d070ff'),
    'leg': mt.fur(['#04020a', '#0a0612', '#140c20', '#1e1430']), 'foot': mt.magma(rock=('#0a0410', '#1a0a28', '#2a1040', '#3a1a5a'), hot='#a040ff', core='#f0d0ff'),
    'mane': mt.flame(SHADOW_FLAME), 'tail': mt.fur(['#04020a', '#0e0818', '#1a1028', '#2a1a40']), 'horn': mt.crystal(['#2a0a5a', '#6a2ab0', '#a060f0', '#e0c0ff'], glow_col='#b070ff')})


def b_nightmare_steed():
    M = Model('nightmare_steed', 256, 256)
    g = kit.quadruped(M, dict(length=40, height=30, girth=(14, 14), neck=(14, -55), head=(9, 9, 12), snout=(9, 7, 6), leg_w=4.5,
                              tail=(3, 7, 3), tail_droop=10, mats=dict(body='body', head='head', leg='leg', foot='foot', tail='tail')))
    for k, yr in enumerate((0, 90)):
        kit.lb(M, f'mane{k}', 'neck', (0, -5, -6), (0, yr, 0))
        M.cube_l(f'mane{k}', (-6, -10, 0), (12, 16, 0), 'mane', plane=True)
    kit.lb(M, 'horn', 'head', (0, -6, -5), (-35, 0, 0))
    M.cube_l('horn', (-1, -9, -1), (2, 9, 2), 'horn')
    kit.lb(M, 'tailfire', 'tail2', (0, 0, 7), (-60, 0, 0))
    M.cube_l('tailfire', (-5, -12, 0), (10, 13, 0), 'mane', plane=True)
    return M, an.quad_set(dict(tail=g['tails'], flicker=['mane0', 'mane1', 'tailfire'], weapon='gore', special='charge', fall=12))


spec('nightmare_steed', 'Kabus Atı', 'Nightmare Steed', 'shadow', 'GROUND', b_nightmare_steed,
     stats(44, 7, 3, 0.34, kb=0.4, size=(1.3, 1.9)),
     [A('MELEE', 2, 18, 0, 2.8, el='SHADOW'), A('CHARGE', 4, 110, 5, 18, power=1.3, el='SHADOW', hit=8, dur=32),
      A('ROAR', 3, 240, 0, 8, radius=8, el='SHADOW', weight=1)],
     spawns=[('obsidian_forest', 25, 1, 2)], sounds=snd('minecraft:entity.skeleton_horse.ambient', 'minecraft:entity.skeleton_horse.hurt',
                                                         'minecraft:entity.skeleton_horse.death', 'minecraft:entity.horse.gallop', 0.7),
     egg=(0x0e0818, 0xd070ff), walk_speed=1.2, run_speed=1.0, loot=essence_loot('shadow', [('minecraft:leather', 0.5, 1, 2)]), xp=15)

register_mats('whisperer', {
    'chest': mt.ghost(['#1a0a2a', '#3a1a5a', '#7a50b0', '#d0b0ff'], alpha=150), 'head': mt.with_face(mt.ghost(['#1a0a2a', '#3a1a5a', '#7a50b0', '#d0b0ff'], alpha=170), 'hood', eye='#ffffff'),
    'arm': mt.ghost(['#1a0a2a', '#3a1a5a', '#7a50b0', '#d0b0ff'], alpha=140), 'claw': mt.ghost(['#e0d0ff', '#f0e8ff', '#ffffff', '#ffffff'], alpha=220),
    'tail': mt.ghost(['#0a0414', '#2a1048', '#5a3090', '#b090f0'], alpha=110), 'flame': mt.flame(SHADOW_FLAME)})


def b_whisperer():
    M = Model('whisperer', 256, 256)
    g = kit.floater(M, dict(hover=12, chest=(12, 12, 7), head=(9, 10, 9), arm_w=3.8, arm_len=28, tail_len=24, tail_segs=3,
                            mats=dict(chest='chest', head='head', arm='arm', tail='tail')))
    kit.hood(M, g, 'chest_hood')
    for side in ('right_hand', 'left_hand'):
        for k in range(3):
            M.cube_l(side, (-1.6 + k * 1.4, 3.5, -1.5), (0.8, 7, 0.8), 'claw')
    return M, an.floater_set(dict(weapon='claw', cast='summon', special='spin'))


spec('whisperer', 'Fısıltı', 'Whisperer', 'shadow', 'FLYING', b_whisperer,
     stats(24, 6, 0, 0.27, size=(0.8, 2.0)),
     [A('MELEE', 2, 16, 0, 2.6, el='SHADOW'), A('AURA', 3, 160, 0, 6, power=0.6, radius=6, el='SHADOW', weight=3), A('BLINK', 3, 120, 4, 16, el='SHADOW', weight=2)],
     spawns=[('obsidian_forest', 30, 1, 2), ('soul_valley', 15, 1, 1)], sounds=snd('minecraft:entity.vex.ambient', 'minecraft:entity.vex.hurt',
                                                                                    'minecraft:entity.vex.death', '', 0.6),
     egg=(0x3a1a5a, 0xffffff), translucent=True, loot=essence_loot('shadow', [('minecraft:phantom_membrane', 0.3, 1, 1)]), xp=12)

register_mats('crystal_golem', {
    'body': mt.plate(OBSIDIAN, trim='#8a40ff', rivets=None, engrave='#b070ff', glow_engrave=True),
    'head': mt.with_face(mt.plate(OBSIDIAN, rivets=None), 'cyclops', eye='#d090ff'),
    'arm': mt.plate(OBSIDIAN, rivets=None), 'fist': mt.crystal(['#2a0a5a', '#6a2ab0', '#a060f0', '#e0c0ff'], glow_col='#b070ff'),
    'crystal': mt.crystal(['#2a0a5a', '#6a2ab0', '#a060f0', '#e0c0ff'], glow_col='#b070ff')})


def b_crystal_golem():
    M = Model('crystal_golem', 256, 256)
    g = kit.golem(M, dict(height=78, torso=(30, 24, 18), head=(10, 9, 10), arm_w=10, leg_w=10, hunch=14, shoulder=True,
                          mats=dict(body='body', head='head', arm='arm', fist='fist', shoulder='crystal')))
    rs = np.random.default_rng(4)
    for k in range(7):
        x = (rs.random() - 0.5) * 22
        h = 8 + rs.random() * 12
        n = f'cr{k}'
        kit.lb(M, n, 'chest', (x, -24 + rs.random() * 6, 6 + rs.random() * 3), (-25 - rs.random() * 30, 0, x * 2))
        M.cube_l(n, (-2.5, -h, -2.5), (5, h, 5), 'crystal')
    return M, an.golem_set(dict(special='slam'))


spec('crystal_golem', 'Kristal Golem', 'Crystal Golem', 'shadow', 'GROUND', b_crystal_golem,
     stats(90, 11, 14, 0.2, kb=0.9, size=(1.6, 2.6)),
     [A('MELEE', 2, 26, 0, 3.4, el='CRYSTAL'), A('SLAM', 4, 140, 0, 5, power=1.2, radius=5, el='CRYSTAL', weight=3),
      A('ERUPT', 3, 160, 4, 16, power=1.1, count=10, el='CRYSTAL', weight=3)],
     spawns=[('obsidian_forest', 12, 1, 1)], sounds=snd('minecraft:entity.iron_golem.hurt', 'minecraft:block.amethyst_block.break',
                                                         'minecraft:entity.iron_golem.death', 'minecraft:entity.iron_golem.step', 0.6),
     egg=(0x1e1230, 0xb070ff), scale=0.55, walk_speed=1.4, loot=essence_loot('shadow', [('arcane_crystal', 0.6, 1, 3), ('minecraft:amethyst_shard', 0.6, 2, 5)]), xp=30)

register_mats('shadow_scorpion', {
    'body': mt.chitin(['#06030c', '#150a26', '#2a1648', '#46286e'], band='#000000', shine='#c890ff', glow='#a050ff'),
    'head': mt.with_face(mt.chitin(['#06030c', '#150a26', '#2a1648', '#46286e']), 'eyes4', eye='#c070ff'),
    'leg': mt.chitin(['#06030c', '#120820', '#1e1036', '#2e1a4e']), 'tail': mt.chitin(['#06030c', '#150a26', '#2a1648', '#46286e'], glow='#a050ff'),
    'claw_arm': mt.chitin(['#06030c', '#150a26', '#2a1648', '#46286e']), 'claw': mt.crystal(['#2a0a5a', '#6a2ab0', '#a060f0', '#e0c0ff'], glow_col='#b070ff')})


def b_shadow_scorpion():
    M = Model('shadow_scorpion', 256, 256)
    g = kit.arthropod(M, dict(body=(14, 7, 14), abdomen=(12, 7, 10), head=(9, 5, 6), pairs=4, leg_len=13, leg_w=2.2, lift=6, pincers=True, stinger=4,
                              abd_tilt=0, mats=dict(body='body', head='head', abdomen='body', leg='leg', tail='tail', claw_arm='claw_arm', claw='claw')))
    return M, an.arthro_set(dict(sting=4, pincers=True), g['legs'])


spec('shadow_scorpion', 'Gölge Akrebi', 'Shadow Scorpion', 'shadow', 'GROUND', b_shadow_scorpion,
     stats(34, 6, 6, 0.27, size=(1.3, 1.0)),
     [A('MELEE', 2, 18, 0, 2.8, el='POISON'), A('BOLT', 3, 70, 3, 14, power=0.9, el='POISON', weight=3), A('WEB', 4, 140, 0, 5, power=0.6, el='SHADOW', weight=2)],
     spawns=[('obsidian_forest', 30, 1, 2), ('basalt_warfields', 10, 1, 1)], sounds=snd('minecraft:entity.spider.ambient', 'minecraft:entity.spider.hurt',
                                                                                        'minecraft:entity.spider.death', 'minecraft:entity.spider.step', 0.5),
     egg=(0x150a26, 0xc070ff), walk_speed=1.6, loot=essence_loot('shadow', [('minecraft:spider_eye', 0.5, 1, 2)]), xp=14)

# =================================================================== IRON LEGION
register_mats('legion_footsoldier', {
    'skin': mt.skin(IMP_SKIN, fib=0.5), 'armor': mt.plate(LEGION_IRON, trim='#c08a24', bands=1),
    'head': mt.with_face(mt.skin(IMP_SKIN), 'eyes2', eye='#ffd040', mouth='#200404'), 'horn': mt.bone(('#1a1008', '#3a2a18', '#6a5030', '#9a7a50')),
    'cloth': mt.cloth(['#3a0606', '#6a0c0c', '#9a1a14', '#c02a20'], trim='#c08a24'), 'guard': mt.plate(['#3a0606', '#6a0c0c', '#9a1a14', '#c84a30'], trim='#e0b040')})


def b_legion_footsoldier():
    M = Model('legion_footsoldier', 256, 256)
    g = kit.humanoid(M, dict(height=54, chest=(16, 12, 9), waist=(12, 7, 7), hips=(13, 6, 8), head=(10, 10, 9), arm_w=5, leg_w=5.5, shoulder=7,
                             digitigrade=True, mats=dict(skin='skin', armor='armor', head='head', arm='skin', leg='skin', hips='cloth')))
    kit.helmet(M, g, 'armor_helm', crest='cloth')
    kit.horns(M, g, 'side', 'horn', 0.8)
    kit.tail(M, 'hips', (0, g['hip_y'] - 2, 4), 3, 'skin', 6, 2.5, droop=30)
    kit.weapon(M, 'right_hand', 'spear', {'blade': 'blade', 'grip': 'grip'}, 0.9)
    kit.weapon(M, 'left_hand', 'shield', {'blade': 'armor', 'guard': 'guard'}, 0.9, flip=True)
    return M, an.humanoid_set(dict(weapon='thrust', special='charge', death='back', tail=['tail0', 'tail1', 'tail2'], arm_swing=0.5))


spec('legion_footsoldier', 'Lejyon Piyadesi', 'Legion Footsoldier', 'legion', 'GROUND', b_legion_footsoldier,
     stats(30, 6, 7, 0.26, size=(0.8, 1.7)),
     [A('MELEE', 2, 18, 0, 3.3, el='BRIMSTONE'), A('CHARGE', 4, 160, 4, 12, power=1.2, el='BRIMSTONE', hit=8, dur=32), A('SHIELD', 3, 300, 0, 10, el='BRIMSTONE', weight=1)],
     spawns=[('basalt_warfields', 80, 2, 4)], sounds=snd('minecraft:entity.piglin_brute.ambient', 'minecraft:entity.piglin_brute.hurt',
                                                          'minecraft:entity.piglin_brute.death', 'minecraft:entity.piglin_brute.step', 1.3),
     egg=(0x46464c, 0xc02a20), loot=essence_loot('legion', [('minecraft:iron_ingot', 0.25, 1, 2)]), xp=12)

register_mats('legion_archer', {
    'skin': mt.skin(IMP_SKIN, fib=0.5), 'armor': mt.cloth(['#2a1a0a', '#4a2e14', '#6a4420', '#8a5a2a'], trim='#c08a24'),
    'head': mt.with_face(mt.skin(IMP_SKIN), 'eyes2', eye='#ffd040', mouth='#200404'), 'horn': mt.bone(('#1a1008', '#3a2a18', '#6a5030', '#9a7a50')),
    'hood': mt.cloth(['#1a0a06', '#3a160c', '#5a2414', '#7a3420'])})


def b_legion_archer():
    M = Model('legion_archer', 256, 256)
    g = kit.humanoid(M, dict(height=52, chest=(14, 12, 8), waist=(11, 7, 7), hips=(12, 6, 8), head=(10, 10, 9), arm_w=4.5, leg_w=5,
                             digitigrade=True, mats=dict(skin='skin', armor='armor', head='head', arm='skin', leg='armor')))
    kit.hood(M, g, 'hood_hood')
    kit.horns(M, g, 'short', 'horn', 0.9)
    kit.tail(M, 'hips', (0, g['hip_y'] - 2, 4), 3, 'skin', 6, 2.5, droop=30)
    kit.weapon(M, 'left_hand', 'bow', {'grip': 'grip', 'string': 'string'}, 0.9)
    M.cube('chest', (2, g['top'] + 1, g['cd'] / 2), (4, 14, 4), 'hood')  # quiver
    return M, an.humanoid_set(dict(weapon='bow', cast='forward', special='stomp', death='back', tail=['tail0', 'tail1', 'tail2'], arm_swing=0.6))


spec('legion_archer', 'Lejyon Okçusu', 'Legion Archer', 'legion', 'GROUND', b_legion_archer,
     stats(24, 4, 3, 0.26, prefer=12, size=(0.7, 1.65)),
     [A('BOLT', 2, 30, 4, 22, power=1.4, count=1, el='BRIMSTONE', hit=7, dur=16, weight=6), A('BOLT', 3, 120, 4, 22, power=1.0, count=3, el='FIRE', weight=2),
      A('MELEE', 2, 20, 0, 2.0, power=0.6, el='BRIMSTONE', weight=1)],
     spawns=[('basalt_warfields', 45, 1, 3)], sounds=snd('minecraft:entity.piglin.ambient', 'minecraft:entity.piglin.hurt',
                                                          'minecraft:entity.piglin.death', 'minecraft:entity.piglin.step', 1.4),
     egg=(0x4a2e14, 0xffd040), loot=essence_loot('legion', [('minecraft:arrow', 0.6, 2, 6)]), xp=10)

register_mats('war_bull', {
    'body': mt.fur(['#140a06', '#2a160c', '#422414', '#5a341e']), 'head': mt.with_face(mt.fur(['#140a06', '#2a160c', '#422414', '#5a341e']), 'beast', eye='#ff4010'),
    'armor': mt.plate(LEGION_IRON, trim='#c08a24', bands=2), 'horn': mt.bone(('#2a2018', '#6a5a40', '#b0a080', '#f0e8d0')), 'leg': mt.fur(['#0a0604', '#1a0e08', '#2a1810', '#3a2216'])})


def b_war_bull():
    M = Model('war_bull', 256, 256)
    g = kit.quadruped(M, dict(length=44, height=22, girth=(22, 20), neck=(6, -10), head=(14, 12, 12), snout=(5, 10, 7), leg_w=7,
                              tail=(2, 7, 2.5), tail_droop=30, mats=dict(body='body', head='head', leg='leg', tail='body')))
    top = g['top']
    M.cube('chest', (-13, top - 4, -21), (26, 8, 20), 'armor', inflate=0.4)
    M.cube('body', (-12, top - 2, 0), (24, 6, 20), 'armor', inflate=0.4)
    for sx, s in ((-1, 'r'), (1, 'l')):
        kit.lb(M, f'bhorn_{s}', 'head', (sx * 6, -6, -6), (0, 0, sx * 80))
        M.cube_l(f'bhorn_{s}', (-2, -8, -2), (4, 8, 4), 'horn')
        kit.lb(M, f'bhorn_{s}b', f'bhorn_{s}', (0, -8, 0), (-30, 0, sx * -75))
        M.cube_l(f'bhorn_{s}b', (-1.4, -9, -1.4), (2.8, 9, 2.8), 'horn')
    M.cube_l('head', (-2.5, 2, -g['hd'] * 0.8 - 5.5), (5, 1, 1), 'armor')
    return M, an.quad_set(dict(tail=g['tails'], weapon='gore', special='charge', fall=10))


spec('war_bull', 'Savaş Boğası', 'War Bull', 'legion', 'GROUND', b_war_bull,
     stats(70, 10, 8, 0.27, kb=0.8, size=(1.8, 1.9)),
     [A('MELEE', 2, 22, 0, 3.2, el='BRIMSTONE'), A('CHARGE', 4, 100, 5, 20, power=1.5, el='BRIMSTONE', hit=8, dur=32, weight=4),
      A('SLAM', 3, 160, 0, 4.5, power=1.0, radius=4.5, el='BRIMSTONE', weight=2)],
     spawns=[('basalt_warfields', 20, 1, 1)], sounds=snd('minecraft:entity.ravager.ambient', 'minecraft:entity.ravager.hurt',
                                                          'minecraft:entity.ravager.death', 'minecraft:entity.ravager.step', 0.8),
     egg=(0x2a160c, 0xc08a24), scale=0.55, walk_speed=1.2, run_speed=0.9, loot=essence_loot('legion', [('minecraft:leather', 0.8, 1, 3), ('minecraft:beef', 0.8, 2, 4)]), xp=25)

register_mats('brimstone_mortar', {
    'body': mt.plate(LEGION_IRON, trim='#e0a020', rivets='#ffd040', bands=2), 'head': mt.with_face(mt.plate(LEGION_IRON, rivets=None), 'visor', eye='#ffd040'),
    'arm': mt.plate(LEGION_IRON, rivets=None), 'fist': mt.plate(BRASS), 'cannon': mt.metal(['#0a0a0c', '#2a2a30', '#5a5a64', '#a0a0aa']),
    'core': mt.magma(rock=('#2a1a04', '#4a3008', '#6a440c', '#8a5a14'), hot='#ffd040', core='#ffffff')})


def b_brimstone_mortar():
    M = Model('brimstone_mortar', 256, 256)
    g = kit.golem(M, dict(height=58, torso=(24, 18, 16), head=(8, 7, 8), arm_w=7, leg_w=8, hunch=6, arm_len=30,
                          mats=dict(body='body', head='head', arm='arm', fist='fist')))
    kit.lb(M, 'cannon', 'chest', (0, -16, 4), (-35, 0, 0))
    M.cube_l('cannon', (-5, -26, -5), (10, 26, 10), 'cannon')
    M.cube_l('cannon', (-6, -27, -6), (12, 4, 12), 'cannon')
    M.cube_l('cannon', (-6, -6, -6), (12, 6, 12), 'core')
    M.cube('chest', (-5, g['top'] + 5, -g['td'] / 2 - 1), (10, 8, 1.5), 'core')
    return M, an.golem_set(dict(special='stomp', cast='forward', sway=['cannon']))


spec('brimstone_mortar', 'Kükürt Topçusu', 'Brimstone Mortar', 'legion', 'GROUND', b_brimstone_mortar,
     stats(60, 6, 10, 0.18, kb=0.7, prefer=12, fire=True, size=(1.3, 2.0)),
     [A('BOMB', 3, 50, 5, 26, power=1.3, radius=3.2, count=2, el='BRIMSTONE', weight=6), A('SLAM', 4, 120, 0, 4, power=1.0, radius=4, el='BRIMSTONE', weight=2),
      A('MELEE', 2, 24, 0, 2.8, el='BRIMSTONE', weight=1)],
     spawns=[('basalt_warfields', 15, 1, 1)], sounds=snd('minecraft:entity.iron_golem.hurt', 'minecraft:entity.iron_golem.damage',
                                                          'minecraft:entity.iron_golem.death', 'minecraft:entity.iron_golem.step', 0.9),
     egg=(0x46464c, 0xffd040), walk_speed=1.4, loot=essence_loot('legion', [('minecraft:gunpowder', 0.8, 2, 4)]), xp=25)

register_mats('chain_demon', {
    'skin': mt.skin(['#1a0404', '#3a0a08', '#6a1810', '#9a2a18'], fib=0.9, veins='#ff6020'), 'head': mt.with_face(mt.skin(['#1a0404', '#3a0a08', '#6a1810', '#9a2a18'], fib=0.5), 'eyes2', eye='#ffb020', mouth='#100000'),
    'chain': mt.plate(['#141416', '#34343a', '#5a5a64', '#8a8a94'], rivets=None), 'horn': mt.bone(('#0a0806', '#2a2420', '#5a5048', '#8a8070')),
    'cloth': mt.cloth(['#100808', '#2a1410', '#401e16', '#5a2a1e'], tattered=True)})


def b_chain_demon():
    M = Model('chain_demon', 256, 256)
    g = kit.humanoid(M, dict(height=72, chest=(22, 16, 12), waist=(15, 8, 10), hips=(16, 6, 10), head=(10, 10, 10), arm_w=7, leg_w=7.5,
                             mats=dict(skin='skin', armor='skin', head='head', hips='cloth', arm='skin', leg='skin')))
    kit.horns(M, g, 'ram', 'horn', 1.0)
    for k in range(2):
        M.cube('chest', (-11.5, g['top'] + 4 + k * 6, -6.5), (23, 1.6, 13), 'chain', inflate=0.2)
    kit.weapon(M, 'right_hand', 'chain', {'grip': 'chain', 'blade': 'chain'}, 1.0)
    kit.weapon(M, 'left_hand', 'chain', {'grip': 'chain', 'blade': 'chain'}, 1.0)
    return M, an.humanoid_set(dict(weapon='smash', cast='forward', special='spin', death='knees', jaw=True, heavy=1.3, sway=['right_hand_weapon', 'left_hand_weapon']))


spec('chain_demon', 'Zincir İblisi', 'Chain Demon', 'legion', 'GROUND', b_chain_demon,
     stats(55, 9, 5, 0.25, kb=0.4, size=(1.0, 2.3)),
     [A('MELEE', 2, 22, 0, 3.4, el='BRIMSTONE'), A('PULL', 3, 120, 4, 14, power=0.8, el='BRIMSTONE', weight=4),
      A('SPIN', 4, 160, 0, 4.5, power=0.9, radius=4.5, el='BRIMSTONE', hit=4, dur=32, weight=2)],
     spawns=[('basalt_warfields', 25, 1, 1)], sounds=snd('minecraft:entity.zoglin.ambient', 'minecraft:entity.zoglin.hurt',
                                                          'minecraft:entity.zoglin.death', 'minecraft:entity.zoglin.step', 0.7),
     egg=(0x3a0a08, 0x8a8a94), loot=essence_loot('legion', [('minecraft:chain', 0.5, 1, 3)]), xp=20)

register_mats('iron_sentinel', {
    'body': mt.plate(LEGION_IRON, trim='#c08a24', engrave='#ffb020', glow_engrave=True, bands=1), 'head': mt.with_face(mt.plate(LEGION_IRON, trim='#c08a24'), 'visor', eye='#ff9020'),
    'arm': mt.plate(LEGION_IRON, trim='#c08a24'), 'fist': mt.plate(BRASS), 'core': mt.magma(hot='#ffb020', core='#ffffff', density=2),
    'blade': mt.metal(['#2a2a30', '#6a6a74', '#c8c8d4', '#ffffff'], glow='#ffb020')})


def b_iron_sentinel():
    M = Model('iron_sentinel', 256, 256)
    g = kit.golem(M, dict(height=84, torso=(26, 26, 16), head=(10, 11, 10), arm_w=8, leg_w=9, hunch=2, arm_len=46, shoulder=True,
                          mats=dict(body='body', head='head', arm='arm', fist='fist', shoulder='body')))
    M.cube('chest', (-5, g['top'] + 6, -g['td'] / 2 - 1), (10, 10, 1.5), 'core')
    kit.weapon(M, 'right_hand', 'halberd', {'blade': 'blade', 'grip': 'arm'}, 1.3)
    return M, an.golem_set(dict(special='spin', weapon='slash', heavy=1.6, special_len=1.6))


spec('iron_sentinel', 'Demir Bekçi', 'Iron Sentinel', 'legion', 'GROUND', b_iron_sentinel,
     stats(80, 12, 16, 0.2, kb=1.0, size=(1.4, 2.8)),
     [A('MELEE', 2, 24, 0, 4.2, el='BRIMSTONE'), A('SPIN', 4, 150, 0, 5, power=1.0, radius=5, el='BRIMSTONE', hit=4, dur=32, weight=3),
      A('SHIELD', 3, 300, 0, 12, el='BRIMSTONE', weight=1)],
     spawns=[('basalt_warfields', 10, 1, 1), ('throne_wastes', 6, 1, 1)], sounds=snd('minecraft:entity.iron_golem.hurt', 'minecraft:entity.iron_golem.damage',
                                                                                     'minecraft:entity.iron_golem.death', 'minecraft:entity.iron_golem.step', 0.6),
     egg=(0x26262a, 0xffb020), scale=0.55, walk_speed=1.2, loot=essence_loot('legion', [('infernal_steel_ingot', 0.4, 1, 2)]), xp=30)

register_mats('infernal_imp', {
    'skin': mt.skin(IMP_SKIN, fib=0.6, veins='#ffb020'), 'head': mt.with_face(mt.skin(IMP_SKIN, fib=0.3), 'eyes2', eye='#ffe040', mouth='#200404'),
    'horn': mt.bone(('#1a1008', '#3a2a18', '#6a5030', '#9a7a50')),
    'membrane': mt.membrane(['#3a0806', '#6a140c', '#9a2414', '#c83a1e'], vein='#200402'), 'wbone': mt.skin(IMP_SKIN)})


def b_infernal_imp():
    M = Model('infernal_imp', 128, 128)
    g = kit.humanoid(M, dict(height=38, chest=(11, 8, 7), waist=(8, 4, 6), hips=(9, 4, 6), head=(9, 9, 8), arm_w=3.4, leg_w=3.6, claws=2.5,
                             digitigrade=True, mats=dict(skin='skin', armor='skin', head='head')))
    kit.horns(M, g, 'long', 'horn', 0.5)
    kit.wings(M, g, 'wbone', 'membrane', span=22)
    kit.tail(M, 'hips', (0, g['hip_y'] - 1, 3), 3, 'skin', 4, 1.8, droop=30)
    return M, an.humanoid_set(dict(weapon='claw', cast='forward', special='slam', death='back', wings=True, tail=['tail0', 'tail1', 'tail2'], arm_swing=1.2))


spec('infernal_imp', 'Cehennem İmpi', 'Infernal Imp', 'legion', 'GROUND', b_infernal_imp,
     stats(16, 4, 1, 0.3, fire=True, size=(0.6, 1.2)),
     [A('MELEE', 2, 14, 0, 2.0, el='FIRE'), A('BOLT', 3, 60, 3, 14, power=0.9, el='FIRE', weight=3), A('LEAP', 4, 90, 3, 9, power=1.0, radius=2, el='FIRE')],
     spawns=[('basalt_warfields', 30, 2, 3), ('ash_wastes', 15, 1, 2)], sounds=snd('minecraft:entity.vex.ambient', 'minecraft:entity.vex.hurt',
                                                                                  'minecraft:entity.vex.death', '', 1.3),
     egg=(0x9a2a14, 0xffe040), walk_speed=1.6, loot=essence_loot('legion'), xp=6)

# =================================================================== SOUL CULT
register_mats('soul_priest', {
    'robe': mt.cloth(SOUL_CLOTH, trim='#d8ccb4', glyph='#40e0ff', tattered=True), 'skin': mt.bone(BONE),
    'head': mt.with_face(mt.bone(BONE), 'skull', eye='#40e0ff'), 'grip': mt.bone(BONE), 'guard': mt.plate(['#1a1a1a', '#3a3a3a', '#6a6a6a', '#9a9a9a'], rivets=None),
    'orb': mt.crystal(['#0a3a48', '#2090b0', '#80e8ff', '#e0ffff'], glow_col='#40e0ff')})


def b_soul_priest():
    M = Model('soul_priest', 256, 256)
    g = kit.humanoid(M, dict(height=66, hunch=10, chest=(14, 13, 8), waist=(10, 8, 7), hips=(13, 6, 8), head=(9, 10, 9), arm_w=4.2, leg_w=5,
                             mats=dict(skin='skin', armor='robe', head='head', arm='robe', hand='skin', leg='robe')))
    kit.hood(M, g, 'robe_hood')
    kit.robe(M, g, 'robe')
    kit.weapon(M, 'right_hand', 'lantern', {'grip': 'grip', 'guard': 'guard', 'orb': 'orb'}, 1.0)
    return M, an.humanoid_set(dict(weapon='cast', cast='summon', special='channel', death='dissolve', skirt=True, arm_swing=0.4, sway=['right_hand_weaponl']))


spec('soul_priest', 'Ruh Rahibi', 'Soul Priest', 'soul', 'GROUND', b_soul_priest,
     stats(28, 4, 1, 0.23, prefer=10, size=(0.8, 2.05)),
     [A('BOLT', 3, 50, 3, 20, power=1.1, count=2, el='SOUL', weight=5), A('HEAL', 3, 220, 0, 20, power=1.0, radius=10, el='SOUL', weight=2),
      A('SUMMON', 4, 320, 0, 20, count=2, el='SOUL', summon='skull_swarm', weight=2)],
     spawns=[('soul_valley', 35, 1, 2)], sounds=snd('minecraft:entity.evoker.ambient', 'minecraft:entity.evoker.hurt', 'minecraft:entity.evoker.death', '', 0.8),
     egg=(0x0a2a32, 0x40e0ff), loot=essence_loot('soul', [('tome_soul_shield', 0.03, 1, 1), ('arcane_crystal', 0.1, 1, 1)]), xp=15)

register_mats('phantom_knight', {
    'armor': mt.ghost(GHOST, alpha=180), 'head': mt.with_face(mt.ghost(GHOST, alpha=200), 'visor', eye='#ffffff'),
    'cape': mt.ghost(['#0a3040', '#1a6a80', '#40b0c8', '#a0f0ff'], alpha=120), 'blade': mt.ghost(['#a0f0ff', '#d0ffff', '#ffffff', '#ffffff'], alpha=230),
    'grip': mt.ghost(GHOST, alpha=200)})


def b_phantom_knight():
    M = Model('phantom_knight', 256, 256)
    g = kit.humanoid(M, dict(height=68, chest=(18, 14, 10), waist=(13, 8, 8), hips=(15, 6, 9), head=(10, 11, 10), arm_w=5.5, leg_w=6.2, shoulder=8,
                             mats=dict(skin='armor', armor='armor', head='head')))
    kit.helmet(M, g, 'armor_helm', crest='cape')
    kit.cape(M, g, 'cape')
    kit.weapon(M, 'right_hand', 'spear', {'blade': 'blade', 'grip': 'grip'}, 1.1)
    return M, an.humanoid_set(dict(weapon='thrust', special='charge', death='dissolve', cape=True, arm_swing=0.5))


spec('phantom_knight', 'Hayalet Şövalye', 'Phantom Knight', 'soul', 'GROUND', b_phantom_knight,
     stats(36, 7, 6, 0.27, size=(0.9, 2.15)),
     [A('MELEE', 2, 18, 0, 3.6, el='SOUL'), A('CHARGE', 4, 120, 5, 16, power=1.3, el='SOUL', hit=8, dur=32, weight=3), A('BLINK', 3, 160, 6, 18, el='SOUL', weight=1)],
     spawns=[('soul_valley', 40, 1, 2)], sounds=snd('minecraft:entity.skeleton.ambient', 'minecraft:entity.skeleton.hurt',
                                                     'minecraft:entity.skeleton.death', 'minecraft:entity.skeleton.step', 0.6),
     egg=(0x60b8c8, 0xffffff), translucent=True, loot=essence_loot('soul', [('minecraft:phantom_membrane', 0.3, 1, 1)]), xp=15)

register_mats('soul_eater', {
    'body': mt.bone(BONE), 'head': mt.with_face(mt.bone(BONE), 'skull', eye='#40e0ff'), 'leg': mt.bone(BONE),
    'core': mt.ghost(['#0a5a70', '#20a0c0', '#80f0ff', '#ffffff'], alpha=210), 'jaw': mt.bone(BONE), 'flame': mt.flame(SOUL_FLAME)})


def b_soul_eater():
    M = Model('soul_eater', 256, 256)
    g = kit.quadruped(M, dict(length=40, height=22, girth=(16, 14), neck=(9, -25), head=(12, 10, 12), snout=(8, 8, 5), leg_w=4.6,
                              tail=(4, 6, 2.5), tail_droop=15, mats=dict(body='body', head='head', leg='leg', tail='body', jaw='jaw')))
    M.cube('chest', (-6, g['top'] + 2, -16), (12, 9, 14), 'core')
    for i in range(5):
        M.cube('chest', (-9.5, g['top'] - 1 + i * 0.3, -18 + i * 3.5), (19, 12, 1.4), 'body')
    for k, yr in enumerate((0, 90)):
        kit.lb(M, f'sflame{k}', 'chest', (0, -10, -8), (0, yr, 0))
        M.cube_l(f'sflame{k}', (-7, -12, 0), (14, 12, 0), 'flame', plane=True)
    return M, an.quad_set(dict(tail=g['tails'], flicker=['sflame0', 'sflame1'], special='stomp', fall=9))


spec('soul_eater', 'Ruh Yiyen', 'Soul Eater', 'soul', 'GROUND', b_soul_eater,
     stats(50, 8, 4, 0.3, size=(1.4, 1.6)),
     [A('MELEE', 2, 18, 0, 2.8, el='SOUL'), A('DRAIN', 4, 140, 0, 8, power=0.8, el='SOUL', hit=4, dur=32, weight=3),
      A('ROAR', 3, 200, 0, 8, radius=8, el='SOUL', weight=2)],
     spawns=[('soul_valley', 25, 1, 2)], sounds=snd('minecraft:entity.skeleton_horse.ambient', 'minecraft:entity.wolf.hurt',
                                                     'minecraft:entity.skeleton.death', 'minecraft:entity.skeleton.step', 0.5),
     egg=(0xd4c8b0, 0x40e0ff), walk_speed=1.3, run_speed=1.0, loot=essence_loot('soul', [('minecraft:bone', 0.8, 2, 4)]), xp=20)

register_mats('skull_swarm', {
    'body': mt.bone(BONE), 'eye': mt.with_face(mt.bone(BONE), 'skull', eye='#40e0ff'), 'lid': mt.bone(BONE),
    'tentacle': mt.flame(SOUL_FLAME), 'spike': mt.bone(BONE)})


def b_skull_swarm():
    M = Model('skull_swarm', 128, 128)
    g = kit.orb(M, dict(radius=5, hover=20, tentacles=0, mats=dict(body='body', eye='eye', lid='lid')))
    for k, yr in enumerate((0, 90)):
        kit.lb(M, f'sflame{k}', 'body', (0, 2, 3), (0, yr, 0))
        M.cube_l(f'sflame{k}', (-5, -2, 0), (10, 12, 0), 'tentacle', plane=True)
        M.bones[f'sflame{k}'].rot = [160.0, float(yr), 0.0]
    kit.lb(M, 'jaw', 'body', (0, 3.5, -1))
    M.cube_l('jaw', (-3.5, 0, -4), (7, 2, 6), 'body')
    return M, an.orb_set(dict(special_len=1.6), 0)


spec('skull_swarm', 'Uçan Kafatası', 'Soul Skull', 'soul', 'FLYING', b_skull_swarm,
     stats(10, 3, 0, 0.3, prefer=6, size=(0.6, 0.6)),
     [A('BOLT', 3, 40, 2, 16, power=1.0, el='SOUL', weight=4), A('MELEE', 2, 16, 0, 1.6, el='SOUL', weight=2)],
     spawns=[('soul_valley', 30, 2, 4)], sounds=snd('minecraft:entity.skeleton.ambient', 'minecraft:entity.skeleton.hurt',
                                                     'minecraft:entity.skeleton.death', '', 1.6),
     egg=(0xf4ecdc, 0x40e0ff), loot=essence_loot('soul'), xp=5)

register_mats('bell_bearer', {
    'robe': mt.cloth(['#0a1214', '#16282c', '#264046', '#3a5a62'], trim='#d8ccb4', tattered=True), 'skin': mt.bone(BONE),
    'head': mt.with_face(mt.bone(BONE), 'skull', eye='#40e0ff'), 'guard': mt.metal(['#1a1004', '#5a3a0a', '#b08020', '#ffe080']),
    'grip': mt.plate(['#141416', '#34343a', '#5a5a64', '#8a8a94'], rivets=None), 'arm': mt.bone(BONE)})


def b_bell_bearer():
    M = Model('bell_bearer', 256, 256)
    g = kit.golem(M, dict(height=74, torso=(26, 22, 18), head=(10, 10, 10), arm_w=7, leg_w=8, hunch=28, arm_len=48,
                          mats=dict(body='robe', head='head', arm='arm', fist='skin', leg='robe', hips='robe')))
    M.cube('chest', (-14, g['top'] - 3, -6), (28, 8, 20), 'robe')
    kit.weapon(M, 'right_hand', 'bell', {'grip': 'grip', 'guard': 'guard'}, 1.4)
    return M, an.golem_set(dict(special='slam', cast='raise', sway=['right_hand_weaponb'], weapon='smash'))


spec('bell_bearer', 'Çan Taşıyıcı', 'Bellbearer', 'soul', 'GROUND', b_bell_bearer,
     stats(85, 10, 8, 0.19, kb=0.9, size=(1.4, 2.6)),
     [A('STUN', 3, 140, 0, 7, power=0.8, radius=7, el='SOUL', weight=4), A('SLAM', 4, 160, 0, 5, power=1.2, radius=5, el='SOUL', weight=2),
      A('MELEE', 2, 26, 0, 3.6, el='SOUL')],
     spawns=[('soul_valley', 8, 1, 1)], sounds=snd('minecraft:entity.warden.ambient', 'minecraft:entity.warden.hurt', 'minecraft:entity.warden.death',
                                                    'minecraft:entity.warden.step', 1.2),
     egg=(0x264046, 0xffe080), scale=0.55, walk_speed=1.4, loot=essence_loot('soul', [('minecraft:bell', 0.15, 1, 1)]), xp=30)

register_mats('bone_serpent', {
    'body': mt.bone(BONE), 'head': mt.with_face(mt.bone(BONE), 'skull', eye='#40e0ff'), 'jaw': mt.bone(BONE),
    'ribs': mt.bone(('#2a241c', '#7a6e58', '#b8ac94', '#e4dccc')), 'fin': mt.flame(SOUL_FLAME)})


def b_bone_serpent():
    M = Model('bone_serpent', 256, 256)
    g = kit.serpent(M, dict(segs=8, width=10, seg_len=8, raise_=22, head=(13, 9, 15), ribs=True, fins=6,
                            mats=dict(body='body', head='head', jaw='jaw', ribs='ribs', fin='fin')))
    return M, an.serpent_set(dict(), g['segs'])


spec('bone_serpent', 'Kemik Yılanı', 'Bone Serpent', 'soul', 'GROUND', b_bone_serpent,
     stats(60, 9, 6, 0.24, kb=0.5, size=(1.3, 1.4)),
     [A('MELEE', 2, 20, 0, 3.2, el='SOUL'), A('BREATH', 4, 140, 0, 8, power=0.8, el='SOUL', hit=6, dur=32, weight=3),
      A('BOLT', 3, 90, 4, 18, power=1.0, count=3, el='BONE', weight=2)],
     spawns=[('soul_valley', 12, 1, 1)], sounds=snd('minecraft:entity.skeleton.ambient', 'minecraft:entity.skeleton.hurt',
                                                     'minecraft:entity.skeleton.death', 'minecraft:entity.skeleton.step', 0.4),
     egg=(0xd4c8b0, 0x1a7aa0), walk_speed=1.4, loot=essence_loot('soul', [('minecraft:bone_block', 0.4, 1, 2)]), xp=25)

# =================================================================== THRONE WASTES
register_mats('throne_gargoyle', {
    'body': mt.plate(STONE, rivets=None, bands=1), 'head': mt.with_face(mt.plate(STONE, rivets=None), 'eyes2', eye='#ff3020', mouth='#0a0606'),
    'membrane': mt.membrane(['#141010', '#2a2222', '#463a38', '#5e504c'], vein='#0a0606', edge_col='#ff3020'), 'wbone': mt.plate(STONE, rivets=None),
    'horn': mt.bone(('#0a0808', '#262020', '#4a403c', '#6a605a')), 'tail': mt.plate(STONE, rivets=None)})


def b_throne_gargoyle():
    M = Model('throne_gargoyle', 256, 256)
    kit.flyer(M, dict(body=(12, 11, 14), head=(10, 9, 9), span=34, hover=12, tail=3, ears=5,
                      mats=dict(body='body', head='head', membrane='membrane', wbone='wbone', ear='horn', tail='tail')))
    return M, an.flyer_set(dict(tail=['tail0', 'tail1', 'tail2']))


spec('throne_gargoyle', 'Taht Gargoyl\'ü', 'Throne Gargoyle', None, 'FLYING', b_throne_gargoyle,
     stats(40, 7, 10, 0.3, kb=0.5, size=(1.2, 1.2)),
     [A('MELEE', 2, 18, 0, 2.6, el='BONE'), A('LEAP', 4, 100, 3, 12, power=1.3, radius=3, el='BONE', weight=2)],
     spawns=[('throne_wastes', 40, 1, 2)], sounds=snd('minecraft:entity.phantom.ambient', 'minecraft:entity.phantom.hurt',
                                                       'minecraft:entity.phantom.death', '', 0.6),
     egg=(0x363030, 0xff3020), loot=essence_loot(None, [('minecraft:stone', 0.6, 1, 3), ('arcane_crystal', 0.1, 1, 1)]), xp=15)

register_mats('magma_worm', {
    'body': mt.magma(density=1.6), 'head': mt.with_face(mt.magma(density=1.0), 'eyes4', eye='#fff0a0'), 'jaw': mt.magma(),
    'fin': mt.flame(FIRE_FLAME)})


def b_magma_worm():
    M = Model('magma_worm', 256, 256)
    g = kit.serpent(M, dict(segs=9, width=11, seg_len=8, raise_=26, head=(14, 10, 14), fins=5, mats=dict(body='body', head='head', jaw='jaw', fin='fin')))
    return M, an.serpent_set(dict(), g['segs'])


spec('magma_worm', 'Magma Solucanı', 'Magma Worm', None, 'GROUND', b_magma_worm,
     stats(55, 9, 6, 0.22, kb=0.6, fire=True, size=(1.4, 1.5)),
     [A('MELEE', 2, 20, 0, 3.2, el='FIRE'), A('BREATH', 4, 120, 0, 8, power=0.9, el='FIRE', hit=6, dur=32, weight=3),
      A('ERUPT', 3, 140, 3, 14, power=1.0, count=8, el='FIRE', weight=2)],
     spawns=[('throne_wastes', 18, 1, 1), ('ash_wastes', 8, 1, 1)], sounds=snd('minecraft:entity.magma_cube.squish', 'minecraft:entity.magma_cube.hurt',
                                                                               'minecraft:entity.magma_cube.death', '', 0.5),
     egg=(0x2a1210, 0xffe070), walk_speed=1.4, loot=essence_loot(None, [('minecraft:magma_cream', 0.6, 1, 3), ('ash_essence', 0.5, 1, 2)]), xp=20)

register_mats('crimson_revenant', {
    'armor': mt.plate(CRIMSON_GOLD, trim='#ffd040', engrave='#ff6030', glow_engrave=True, bands=1), 'skin': mt.bone(BONE),
    'head': mt.with_face(mt.plate(CRIMSON_GOLD, trim='#ffd040'), 'helm_eyes', eye='#ff4020'),
    'cape': mt.cloth(['#2a0004', '#5a000a', '#8a0a14', '#b01a20'], trim='#ffd040', glyph='#ff6030', tattered=True),
    'blade': mt.metal(['#3a0006', '#8a1a10', '#e06040', '#fff0c0'], glow='#ff5020'), 'guard': mt.plate(BRASS, rivets=None)})


def b_crimson_revenant():
    M = Model('crimson_revenant', 256, 256)
    g = kit.humanoid(M, dict(height=70, chest=(19, 15, 10), waist=(13, 8, 8), hips=(15, 6, 9), head=(10, 11, 10), arm_w=5.6, leg_w=6.5, shoulder=9,
                             mats=dict(skin='skin', armor='armor', head='head', arm='armor', leg='armor')))
    kit.crown(M, g, 'guard', spikes=5, h=3)
    kit.cape(M, g, 'cape')
    kit.weapon(M, 'right_hand', 'greatsword', {'blade': 'blade', 'grip': 'grip', 'guard': 'guard'}, 0.9)
    return M, an.humanoid_set(dict(weapon='smash', special='slam', death='knees', cape=True, arm_swing=0.5, heavy=1.2))


spec('crimson_revenant', 'Kızıl Hortlak', 'Crimson Revenant', None, 'GROUND', b_crimson_revenant,
     stats(55, 10, 10, 0.25, kb=0.4, size=(0.95, 2.2)),
     [A('MELEE', 2, 22, 0, 3.6, el='FIRE'), A('LEAP', 4, 120, 4, 14, power=1.3, radius=3.2, el='FIRE', weight=3), A('ROAR', 3, 260, 0, 10, radius=10, el='FIRE', weight=1)],
     spawns=[('throne_wastes', 30, 1, 2)], sounds=snd('minecraft:entity.wither_skeleton.ambient', 'minecraft:entity.wither_skeleton.hurt',
                                                       'minecraft:entity.wither_skeleton.death', 'minecraft:entity.iron_golem.step', 0.7),
     egg=(0x7a0a12, 0xffd040), loot=essence_loot(None, [('minecraft:gold_ingot', 0.4, 1, 3), ('infernal_steel_ingot', 0.2, 1, 1)]), xp=25)


# =================================================================== ENVOYS
def envoy(fid, tr, en, pal, face, eye, trim, look):
    name = f'{fid}_envoy'
    register_mats(name, {'robe': mt.cloth(pal, trim=trim, glyph=eye), 'skin': look['skin'], 'head': mt.with_face(look['skin'], face, eye=eye, mouth=look.get('mouth')),
                         'guard': mt.plate(BRASS, rivets=None), 'grip': mt.wood(), 'orb': mt.crystal(['#3a2a0a', '#a07a20', '#ffd060', '#fff8d0'], glow_col=eye)})

    def build():
        M = Model(name, 256, 256)
        g = kit.humanoid(M, dict(height=64, chest=(15, 13, 9), waist=(11, 8, 7), hips=(13, 6, 8), head=(9, 10, 9), arm_w=4.5, leg_w=5,
                                 mats=dict(skin='skin', armor='robe', head='head', arm='robe', hand='skin', leg='robe')))
        kit.robe(M, g, 'robe', flare=6)
        kit.cape(M, g, 'robe')
        if look.get('horns'):
            kit.horns(M, g, look['horns'], 'guard', 0.7)
        if look.get('hood'):
            kit.hood(M, g, 'robe_hood')
        if look.get('crown'):
            kit.crown(M, g, 'guard', spikes=3, h=2)
        kit.weapon(M, 'left_hand', 'staff', {'grip': 'grip', 'guard': 'guard', 'orb': 'orb'}, 0.9)
        A_ = an.humanoid_set(dict(weapon='cast', cast='forward', special='roar', death='knees', skirt=True, cape=True, arm_swing=0.5))
        return M, A_
    spec(name, f'{tr} Elçisi', f'{en} Envoy', fid, 'ENVOY', build, stats(40, 2, 2, 0.22, size=(0.8, 2.0)), [],
         spawns=[({'ash': 'ash_wastes', 'blood': 'blood_marsh', 'shadow': 'obsidian_forest', 'legion': 'basalt_warfields', 'soul': 'soul_valley'}[fid], 6, 1, 1)],
         sounds=snd('minecraft:entity.villager.ambient', 'minecraft:entity.villager.hurt', 'minecraft:entity.villager.death', '', 0.7),
         egg=(int(pal[2][1:], 16), int(eye[1:], 16)), loot={"type": "minecraft:entity", "pools": []}, xp=0)


envoy('ash', 'Kül Krallığı', 'Ash Kingdom', ['#1a1210', '#3a2a22', '#5a4434', '#7a5e48'], 'eyes2', '#ff8a20', '#ff7a20',
      dict(skin=mt.ash(), horns='short', crown=True))
envoy('blood', 'Kan Hanedanı', 'Blood Dynasty', ['#1a0004', '#3a000a', '#5a0612', '#7a0a1a'], 'eyes2', '#ff2030', '#e0b040',
      dict(skin=mt.skin(PALE, fib=0.1), crown=True, mouth='#3a0008'))
envoy('shadow', 'Gölge Konseyi', 'Shadow Council', SHADOW_CLOTH, 'mask', '#c060ff', '#8a40ff', dict(skin=mt.cloth(SHADOW_CLOTH), hood=True))
envoy('legion', 'Demir Lejyonu', 'Iron Legion', ['#2a0606', '#4a0c0c', '#6a1a14', '#8a2a20'], 'eyes2', '#ffd040', '#c08a24',
      dict(skin=mt.skin(IMP_SKIN), horns='side', mouth='#200404'))
envoy('soul', 'Ruh Tarikatı', 'Soul Cult', SOUL_CLOTH, 'skull', '#40e0ff', '#d8ccb4', dict(skin=mt.bone(BONE), hood=True))


# =================================================================== LORDS & TYRANT
register_mats('varkhas', {
    'armor': mt.plate(ASH_ARMOR, trim='#ff7a20', engrave='#ffb040', glow_engrave=True, bands=1), 'skin': mt.magma(),
    'head': mt.with_face(mt.plate(ASH_ARMOR, trim='#ff7a20'), 'visor', eye='#ffb020'),
    'cape': mt.cloth(['#0a0606', '#1e1210', '#2e1c16', '#3e2a20'], trim='#ff7a20', glyph='#ffb040', tattered=True),
    'blade': mt.magma(rock=('#2a1a14', '#4a3028', '#6a4838', '#8a6048'), hot='#ff8a20', core='#fff0a0', density=0.8),
    'guard': mt.plate(['#0a0808', '#2a2222', '#5a4a40', '#8a7a68'], trim='#ff7a20'), 'crown': mt.magma(hot='#ffb020', core='#ffffff', density=2.5)})


def b_varkhas():
    M = Model('varkhas', 512, 512)
    g = kit.humanoid(M, dict(height=72, chest=(22, 16, 12), waist=(15, 9, 10), hips=(17, 7, 11), head=(11, 12, 11), arm_w=6.5, leg_w=7.5, shoulder=11,
                             mats=dict(skin='skin', armor='armor', head='head', arm='armor', leg='armor', waist='armor')))
    kit.helmet(M, g, 'armor_helm')
    kit.crown(M, g, 'crown', spikes=7, h=5)
    kit.cape(M, g, 'cape')
    kit.weapon(M, 'right_hand', 'greatsword', {'blade': 'blade', 'grip': 'grip', 'guard': 'guard'}, 1.15)
    for k in range(3):
        M.cube_l('right_arm', (-4, 2 + k * 3, -4), (8, 1.2, 8), 'crown')
    return M, an.humanoid_set(dict(weapon='smash', cast='raise', special='slam', death='knees', cape=True, heavy=1.5, arm_swing=0.5))


spec('varkhas', 'Kül Kralı Varkhas', 'Varkhas, the Ash King', 'ash', 'LORD', b_varkhas,
     stats(420, 16, 14, 0.28, kb=0.9, fire=True, size=(1.4, 4.0)),
     [A('MELEE', 2, 22, 0, 4.4, power=1.0, el='FIRE'), A('ERUPT', 3, 140, 3, 20, power=0.9, count=12, radius=2, el='FIRE', weight=3),
      A('METEOR', 3, 260, 4, 26, power=0.9, radius=6, count=8, el='FIRE', hit=11, dur=48, weight=2),
      A('SLAM', 4, 160, 0, 6, power=1.2, radius=6.5, el='FIRE', weight=3), A('CHARGE', 4, 200, 6, 18, power=1.2, el='FIRE', hit=10, dur=32, weight=2),
      A('SUMMON', 3, 500, 0, 30, count=2, el='FIRE', summon='ash_knight', weight=1)],
     sounds=snd('minecraft:entity.wither_skeleton.ambient', 'minecraft:entity.iron_golem.hurt', 'minecraft:entity.wither.death', 'minecraft:entity.iron_golem.step', 0.5),
     egg=(0x262020, 0xffb020), scale=0.88, shadow=1.4, loot=boss_loot('ash', 'ashbringer'), xp=400)

register_mats('serathis', {
    'gown': mt.cloth(['#140004', '#300008', '#500a14', '#701020'], trim='#e0b040', glyph='#ff3040'), 'skin': mt.skin(PALE, fib=0.1),
    'head': mt.with_face(mt.skin(PALE, fib=0.05), 'eyes2', eye='#ff1020', mouth='#5a0010', teeth='#ffffff'),
    'crown': mt.plate(BRASS, rivets=None), 'gem': mt.solid('#ff2030', glow=True),
    'membrane': mt.membrane(['#1a0004', '#40000a', '#6a0814', '#901020'], vein='#0a0002', edge_col='#ff3040'), 'wbone': mt.bone(('#2a0a0a', '#5a2a24', '#8a5a50', '#b88a80')),
    'hair': mt.fur(['#0a0004', '#1a0008', '#2a0410', '#3a0a18'])})


def b_serathis():
    M = Model('serathis', 512, 512)
    g = kit.humanoid(M, dict(height=74, chest=(15, 14, 9), waist=(10, 9, 7), hips=(14, 6, 9), head=(9, 11, 9), arm_w=4.6, leg_w=5.5, arm_len=32,
                             mats=dict(skin='skin', armor='gown', head='head', arm='skin', leg='gown', hand='skin', waist='gown')))
    kit.robe(M, g, 'gown', flare=14)
    kit.crown(M, g, 'crown', gem='gem', spikes=5, h=4)
    kit.wings(M, g, 'wbone', 'membrane', span=44)
    for k in range(4):
        kit.lb(M, f'hair{k}', 'head', (-3 + k * 2, -9, 4), (20, 0, (k - 1.5) * 8))
        M.cube_l(f'hair{k}', (-1.2, 0, 0), (2.4, 20, 1.5), 'hair')
    kit.weapon(M, 'right_hand', 'staff', {'grip': 'wbone', 'guard': 'crown', 'orb': 'orb'}, 1.1)
    return M, an.humanoid_set(dict(weapon='claw', cast='raise', special='channel', death='dissolve', skirt=True, wings=True, special_len=2.4,
                                   sway=['hair0', 'hair1', 'hair2', 'hair3'], arm_swing=0.6))


spec('serathis', 'Kan Kraliçesi Serathis', 'Serathis, the Blood Queen', 'blood', 'LORD', b_serathis,
     stats(360, 13, 8, 0.3, kb=0.7, size=(1.2, 4.0)),
     [A('MELEE', 2, 18, 0, 3.6, el='BLOOD'), A('BOLT', 3, 60, 3, 24, power=0.9, count=5, el='BLOOD', weight=4),
      A('DRAIN', 4, 160, 0, 12, power=0.9, el='BLOOD', hit=6, dur=48, weight=3), A('BLINK', 3, 140, 4, 24, el='BLOOD', weight=2),
      A('SUMMON', 3, 360, 0, 30, count=3, el='BLOOD', summon='bloodsucker', weight=1), A('ERUPT', 3, 180, 3, 18, power=1.0, count=10, radius=2, el='BLOOD', weight=2)],
     sounds=snd('minecraft:entity.witch.ambient', 'minecraft:entity.witch.hurt', 'minecraft:entity.wither.death', '', 0.7),
     egg=(0x500a14, 0xe0b040), scale=0.85, shadow=1.2, special_len=2.4, loot=boss_loot('blood', 'bloodthirster'), xp=400)

register_mats('nyxar', {
    'chest': mt.cloth(SHADOW_CLOTH, trim='#8a40ff', glyph='#d090ff'), 'head': mt.with_face(mt.cloth(SHADOW_CLOTH), 'mask', eye='#d090ff'),
    'arm': mt.cloth(['#06040a', '#120a1e', '#221436', '#3a2258']), 'hand': mt.ghost(['#3a1a5a', '#7a50b0', '#b090f0', '#f0e0ff'], alpha=230),
    'tail': mt.ghost(['#0a0414', '#2a1048', '#5a3090', '#b090f0'], alpha=150), 'flame': mt.flame(SHADOW_FLAME),
    'orb': mt.crystal(['#2a0a5a', '#6a2ab0', '#a060f0', '#e0c0ff'], glow_col='#b070ff')})


def b_nyxar():
    M = Model('nyxar', 512, 512)
    g = kit.floater(M, dict(hover=10, chest=(16, 16, 10), head=(10, 11, 10), arm_w=4.5, arm_len=26, tail_len=26, tail_segs=3, flames=True,
                            mats=dict(chest='chest', head='head', arm='arm', hand='hand', tail='tail', flame='flame')))
    kit.hood(M, g, 'chest_hood')
    # second pair of arms
    top = g['top']
    for side, sx in (('right2', -1), ('left2', 1)):
        A_ = (sx * 9, top + 8, 0)
        B = (sx * 15, top + 18, -3)
        C = (sx * 17, top + 26, -8)
        M.seg(f'{side}_arm', 'chest', A_, B, 3.8, 3.8, 'arm', extend=1)
        M.seg(f'{side}_fore', f'{side}_arm', B, C, 3.4, 3.4, 'arm', extend=1)
        M.bone_w(f'{side}_hand', f'{side}_fore', C, kit.R0)
        M.cube_l(f'{side}_hand', (-2, 0, -2), (4, 4, 4), 'hand')
        kit.lb(M, f'{side}_orb', f'{side}_hand', (0, 6, -2))
        M.cube_l(f'{side}_orb', (-2.5, -2.5, -2.5), (5, 5, 5), 'orb')
    for k in range(3):
        kit.lb(M, f'halo{k}', 'head', (0, -14, 6), (0, k * 60, 0))
        M.cube_l(f'halo{k}', (-9, -0.5, -0.5), (18, 1, 1), 'orb')
    A_ = an.floater_set(dict(weapon='cast', cast='summon', special='spin', flicker=['flame0', 'flame1'], special_len=1.8))
    for a in A_.values():
        a.rot('right2_arm', (0, (-30, 0, 20)), (a.length / 2, (-50, 0, 35)), (a.length, (-30, 0, 20)))
        a.rot('left2_arm', (0, (-30, 0, -20)), (a.length / 2, (-50, 0, -35)), (a.length, (-30, 0, -20)))
    return M, A_


spec('nyxar', "Gölge Veziri Nyx'ar", "Nyx'ar, the Shadow Vizier", 'shadow', 'LORD', b_nyxar,
     stats(340, 12, 8, 0.3, kb=0.6, size=(1.3, 3.8)),
     [A('MELEE', 2, 18, 0, 3.4, el='SHADOW'), A('BOLT', 3, 50, 3, 26, power=0.85, count=6, el='SHADOW', weight=4),
      A('BLINK', 3, 110, 3, 26, el='SHADOW', weight=3), A('AURA', 3, 200, 0, 8, power=0.8, radius=8, el='SHADOW', weight=2),
      A('BEAM', 4, 200, 4, 20, power=1.1, el='SHADOW', hit=8, dur=36, weight=2), A('SUMMON', 3, 400, 0, 30, count=2, el='SHADOW', summon='shadow_assassin', weight=1),
      A('NOVA', 4, 220, 0, 10, power=0.9, radius=1.5, count=16, el='SHADOW', hit=18, dur=36, weight=2)],
     sounds=snd('minecraft:entity.illusioner.ambient', 'minecraft:entity.illusioner.hurt', 'minecraft:entity.illusioner.death', '', 0.6),
     egg=(0x120a1e, 0xd090ff), scale=0.95, shadow=1.0, special_len=1.8, loot=boss_loot('shadow', 'shadowfang'), xp=400)

register_mats('grommak', {
    'skin': mt.skin(['#1a0606', '#3a100c', '#6a2016', '#9a3420'], fib=1.0, veins='#ffa020'), 'head': mt.with_face(mt.skin(['#1a0606', '#3a100c', '#6a2016', '#9a3420'], fib=0.5), 'eyes2', eye='#ffd040', mouth='#100000'),
    'armor': mt.plate(LEGION_IRON, trim='#c08a24', rivets='#ffd040', bands=2), 'horn': mt.bone(('#0a0806', '#3a3028', '#7a6a58', '#b0a088')),
    'chain': mt.plate(['#141416', '#34343a', '#5a5a64', '#8a8a94'], rivets=None), 'cloth': mt.cloth(['#2a0606', '#4a0c0c', '#6a1a14', '#8a2a20'], trim='#c08a24', tattered=True),
    'guard': mt.plate(['#0e0e10', '#2a2a30', '#4a4a52', '#7a7a84'], trim='#c08a24'), 'blade': mt.plate(BRASS, rivets=None)})


def b_grommak():
    M = Model('grommak', 512, 512)
    g = kit.humanoid(M, dict(height=74, hunch=10, chest=(28, 18, 15), waist=(19, 9, 12), hips=(20, 7, 12), head=(11, 11, 11), arm_w=9, leg_w=9.5, shoulder=13,
                             arm_len=34, jaw=(9, 4, 7), mats=dict(skin='skin', armor='armor', head='head', hips='cloth', arm='skin', leg='skin', waist='skin')))
    kit.horns(M, g, 'long', 'horn', 1.1)
    M.cube('chest', (-14.5, g['top'] - 0.5, -8), (29, 8, 16), 'armor', inflate=0.3)
    for k in range(2):
        M.cube('chest', (-14.2, g['top'] + 9 + k * 4, -7.8), (28.4, 1.6, 15.6), 'chain')
    kit.weapon(M, 'right_hand', 'hammer', {'blade': 'blade', 'grip': 'chain', 'guard': 'guard'}, 1.15)
    return M, an.humanoid_set(dict(weapon='smash', cast='forward', special='slam', death='knees', jaw=True, heavy=1.8, arm_swing=0.6))


spec('grommak', 'Savaş Lordu Grommak', 'Grommak, the Warlord', 'legion', 'LORD', b_grommak,
     stats(480, 18, 16, 0.27, kb=1.0, size=(1.8, 4.1)),
     [A('MELEE', 2, 24, 0, 4.4, el='BRIMSTONE'), A('SLAM', 4, 140, 0, 7, power=1.2, radius=7, el='BRIMSTONE', weight=3),
      A('CHARGE', 4, 180, 6, 20, power=1.3, el='BRIMSTONE', hit=10, dur=32, weight=2), A('PULL', 3, 160, 5, 18, power=0.7, el='BRIMSTONE', weight=2),
      A('ROAR', 3, 300, 0, 14, radius=14, el='BRIMSTONE', weight=1), A('SUMMON', 3, 420, 0, 30, count=3, el='BRIMSTONE', summon='legion_footsoldier', weight=1),
      A('LEAP', 4, 200, 6, 22, power=1.4, radius=5, el='BRIMSTONE', weight=2)],
     sounds=snd('minecraft:entity.ravager.ambient', 'minecraft:entity.ravager.hurt', 'minecraft:entity.ravager.death', 'minecraft:entity.ravager.step', 0.6),
     egg=(0x46464c, 0xffd040), scale=0.9, shadow=1.6, loot=boss_loot('legion', 'warlord_hammer'), xp=400)

register_mats('ilvaine', {
    'chest': mt.cloth(SOUL_CLOTH, trim='#d8ccb4', glyph='#80f0ff'), 'head': mt.with_face(mt.bone(BONE), 'skull', eye='#80f0ff'),
    'arm': mt.cloth(['#041418', '#0a2a32', '#145060', '#2a7a8a']), 'hand': mt.bone(BONE), 'tail': mt.ghost(['#0a3040', '#1a6a80', '#40b0c8', '#a0f0ff'], alpha=150),
    'flame': mt.flame(SOUL_FLAME), 'skull': mt.with_face(mt.bone(BONE), 'skull', eye='#40e0ff'), 'grip': mt.bone(BONE),
    'guard': mt.metal(['#1a1004', '#5a3a0a', '#b08020', '#ffe080']), 'orb': mt.crystal(['#0a3a48', '#2090b0', '#80e8ff', '#e0ffff'], glow_col='#40e0ff')})


def b_ilvaine():
    M = Model('ilvaine', 512, 512)
    g = kit.floater(M, dict(hover=8, chest=(15, 15, 9), head=(10, 11, 10), arm_w=4.4, arm_len=26, tail_len=28, tail_segs=3, flames=True,
                            mats=dict(chest='chest', head='head', arm='arm', hand='hand', tail='tail', flame='flame')))
    kit.hood(M, g, 'chest_hood')
    kit.lb(M, 'halo', 'head', (0, -8, 3))
    for k in range(7):
        a = k / 7 * math.tau
        n = f'hskull{k}'
        kit.lb(M, n, 'halo', (math.cos(a) * 13, math.sin(a) * 13 - 4, 4))
        M.cube_l(n, (-2.5, -2.5, -2.5), (5, 5, 5), 'skull')
    kit.weapon(M, 'right_hand', 'lantern', {'grip': 'grip', 'guard': 'guard', 'orb': 'orb'}, 1.3)
    A_ = an.floater_set(dict(weapon='cast', cast='summon', special='spin', flicker=['flame0', 'flame1'], special_len=1.8))
    for a in A_.values():
        a.rot('halo', *[(a.length * k / 4, (0, 0, 90 * k)) for k in range(5)], interp='L')
    return M, A_


spec('ilvaine', 'Ruh Kâhini Ilvaine', 'Ilvaine, the Soul Oracle', 'soul', 'LORD', b_ilvaine,
     stats(340, 11, 8, 0.27, kb=0.7, size=(1.2, 3.8)),
     [A('MELEE', 2, 20, 0, 3.4, el='SOUL'), A('BOLT', 3, 50, 3, 24, power=0.9, count=4, el='SOUL', weight=4),
      A('SUMMON', 3, 300, 0, 30, count=3, el='SOUL', summon='skull_swarm', weight=2), A('HEAL', 3, 400, 0, 30, power=1.2, radius=10, el='SOUL', weight=1),
      A('STUN', 4, 180, 0, 8, power=0.8, radius=8, el='SOUL', weight=2), A('ERUPT', 3, 160, 3, 20, power=1.0, count=12, radius=2, el='SOUL', weight=2),
      A('NOVA', 4, 200, 0, 10, power=0.9, radius=1.2, count=14, el='SOUL', hit=18, dur=36, weight=2)],
     sounds=snd('minecraft:entity.evoker.ambient', 'minecraft:entity.evoker.hurt', 'minecraft:entity.wither.death', '', 0.6),
     egg=(0x0a2a32, 0x80f0ff), scale=0.95, shadow=1.0, special_len=1.8, loot=boss_loot('soul', 'soul_reaper'), xp=400)

register_mats('azgaroth', {
    'skin': mt.skin(['#1a0204', '#3a0608', '#6a0e10', '#9a1a18'], fib=1.0, veins='#ff7020'),
    'armor': mt.plate(CRIMSON_GOLD, trim='#ffd040', engrave='#ffb040', glow_engrave=True, bands=1),
    'head': mt.with_face(mt.skin(['#1a0204', '#3a0608', '#6a0e10', '#9a1a18'], fib=0.4), 'eyes2', eye='#ffe040', mouth='#100000'),
    'horn': mt.bone(('#060404', '#1a1414', '#3a3030', '#5a4a48')), 'crown': mt.plate(BRASS, rivets=None), 'gem': mt.solid('#ff3020', glow=True),
    'membrane': mt.membrane(['#1a0204', '#3a0608', '#6a1010', '#9a2018'], vein='#0a0000', edge_col='#ff6020'), 'wbone': mt.skin(['#1a0204', '#3a0608', '#5a0c0e', '#7a1414']),
    'cape': mt.cloth(['#1a0004', '#3a0008', '#5a0610', '#7a0a16'], trim='#ffd040', glyph='#ff8030', tattered=True),
    'blade': mt.magma(rock=('#3a0a06', '#6a140a', '#9a2a10', '#c84a18'), hot='#ffe060', core='#ffffff', density=0.7),
    'guard': mt.plate(BRASS), 'claw': mt.bone(('#060404', '#1a1414', '#3a3030', '#5a4a48'))})


def b_azgaroth():
    M = Model('azgaroth', 512, 512)
    g = kit.humanoid(M, dict(height=82, chest=(26, 18, 14), waist=(17, 10, 11), hips=(19, 7, 12), head=(12, 12, 12), arm_w=8, leg_w=9, shoulder=12,
                             digitigrade=True, arm_len=36, jaw=(9, 4, 8), claws=4,
                             mats=dict(skin='skin', armor='armor', head='head', arm='skin', leg='skin', waist='skin', hips='armor', shoulder='armor')))
    kit.horns(M, g, 'ram', 'horn', 1.3)
    kit.horns(M, g, 'long', 'horn', 1.0)
    kit.crown(M, g, 'crown', gem='gem', spikes=5, h=4)
    kit.wings(M, g, 'wbone', 'membrane', span=56)
    kit.cape(M, g, 'cape', width=20)
    kit.tail(M, 'hips', (0, g['hip_y'] - 3, 6), 4, 'skin', 9, 5, droop=35)
    kit.weapon(M, 'right_hand', 'greatsword', {'blade': 'blade', 'grip': 'horn', 'guard': 'guard'}, 1.3)
    return M, an.humanoid_set(dict(weapon='smash', cast='summon', special='slam', death='knees', jaw=True, wings=True, cape=True, heavy=1.6,
                                   tail=['tail0', 'tail1', 'tail2', 'tail3'], arm_swing=0.5))


spec('azgaroth', 'Kızıl Tahtın Tiranı Azgaroth', 'Azgaroth, Tyrant of the Crimson Throne', None, 'TYRANT', b_azgaroth,
     stats(900, 22, 18, 0.3, kb=1.0, fire=True, size=(2.0, 5.2)),
     [A('MELEE', 2, 22, 0, 5.2, el='FIRE'), A('METEOR', 3, 220, 4, 30, power=0.9, radius=7, count=12, el='FIRE', hit=11, dur=56, weight=3),
      A('ERUPT', 3, 140, 3, 22, power=0.9, count=14, radius=2, el='FIRE', weight=3), A('BREATH', 4, 200, 0, 10, power=0.8, el='FIRE', hit=6, dur=40, weight=2),
      A('LEAP', 4, 200, 6, 24, power=1.4, radius=6, el='FIRE', weight=2), A('SUMMON', 3, 420, 0, 30, count=2, el='FIRE', summon='crimson_revenant', weight=1),
      A('NOVA', 4, 180, 0, 12, power=0.8, radius=1.5, count=18, el='FIRE', weight=2), A('DRAIN', 4, 260, 0, 12, power=0.9, el='BLOOD', hit=6, dur=40, weight=1)],
     sounds=snd('minecraft:entity.ender_dragon.growl', 'minecraft:entity.wither.hurt', 'minecraft:entity.ender_dragon.death', 'minecraft:entity.ravager.step', 0.6),
     egg=(0x6a0e10, 0xffd040), scale=1.0, shadow=2.0, loot=boss_loot(None, 'sovereign_blade', [('tyrant_heart', 1, 2), ('minecraft:nether_star', 1, 1)]), xp=1000)


# =================================================================== export
def tex_size(M):
    for w in (128, 256, 512, 1024):
        M.tw, M.th = w, w * 4
        try:
            need = M.pack()
        except RuntimeError:
            continue
        if need <= w:
            h = 1
            while h < need:
                h *= 2
            M.th = max(h, 32)
            return
    raise RuntimeError(f'{M.name} needs a bigger texture')


def anim_slots(A_):
    out = []
    for slot in range(7):
        out.append(A_.get(slot))
    return out


def java_spec(s):
    st = s.stats
    w, h = st['size']
    ab = []
    for a in s.abilities:
        slot = a['slot']
        if a['hit'] is not None:
            hit, dur = a['hit'], a['dur']
        elif slot == 2:
            hit, dur = 7, 16
        elif slot == 3:
            hit, dur = 11, 24
        else:
            hit, dur = int(s.special_len * 10), int(s.special_len * 20)
        ab.append(f'new Ability(Ability.Type.{a["type"]}, {slot}, {dur}, {hit}, {a["cd"]}, {a["minr"]}F, {a["maxr"]}F, {a["power"]}F, '
                  f'{a["radius"]}F, {a["count"]}, Ability.Element.{a["el"]}, "{a["summon"]}", {a["weight"]})')
    fac = f'Faction.{s.faction.upper()}' if s.faction else 'null'
    so = s.sounds or {}
    abil = (',\n            ' + ',\n            '.join(ab)) if ab else ''
    return (f'    public static final MobSpec {s.id.upper()} = new MobSpec("{s.id}", {fac}, MobSpec.Kind.{s.kind}, {w}F, {h}F, {st["hp"]}, {st["dmg"]}, '
            f'{st["armor"]}, {st["speed"]}, {st["kb"]}, {st["prefer"]}, {str(st["fire"]).lower()}, {s.xp},\n            "{so.get("ambient", "")}", '
            f'"{so.get("hurt", "")}", "{so.get("death", "")}", "{so.get("step", "")}", {so.get("pitch", 1.0)}F, 0x{s.egg[0]:06X}, 0x{s.egg[1]:06X}{abil});')


def write_all(root):
    import sys
    java_root = os.path.join(root, '..', 'java', 'com', 'krolasyon', 'bosses', 'realm')
    gen_dir = os.path.join(java_root, 'client', 'gen')
    os.makedirs(gen_dir, exist_ok=True)
    tex_dir = os.path.join(root, f'assets/{M_ID}/textures/entity/realm')
    os.makedirs(tex_dir, exist_ok=True)
    reg = []
    for s in SPECS:
        M, A_ = s.build_fn()
        M.name = s.id
        tex_size(M)
        P = Painter(M)
        P.mats.update(MAT_REGISTRY[s.id])
        tex, glow = P.run().images()
        tex.save(os.path.join(tex_dir, f'{s.id}.png'))
        glow.save(os.path.join(tex_dir, f'{s.id}_glow.png'))
        bones = set(M.order)
        slots = anim_slots(A_)
        consts = []
        body = []
        for i, a in enumerate(slots):
            if a is None:
                consts.append('null')
                continue
            a.ch = [c for c in a.ch if c[0] in bones]
            if not a.ch:
                a.ch = [(M.order[0], 'ROTATION', [(0.0, (0, 0, 0), 'L'), (a.length, (0, 0, 0), 'L')])]
            k = f'A{i}'
            body.append(a.java(k))
            consts.append(k)
        cls = f'{s.pascal()}Model'
        java = f"""// GENERATED by gen/realm/mobs.py - do not edit by hand
package com.krolasyon.bosses.realm.client.gen;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

@SuppressWarnings("unused")
public final class {cls} {{
    private {cls}() {{}}

    public static LayerDefinition layer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{M.java_layer()}
        return LayerDefinition.create(mesh, {M.tw}, {M.th});
    }}

{chr(10).join(body)}

    public static final AnimationDefinition[] ANIMS = {{{', '.join(consts)}}};
}}
"""
        with open(os.path.join(gen_dir, f'{cls}.java'), 'w') as f:
            f.write(java)
        head = s.head if s.head in bones else ('body' if 'body' in bones else M.order[0])
        reg.append(f'        add("{s.id}", {cls}::layer, {cls}.ANIMS, {s.scale}F, {s.shadow}F, {str(s.translucent).lower()}, {s.walk_speed}F, {s.run_speed}F, "{head}");')
        # loot
        lp = os.path.join(root, f'data/{M_ID}/loot_tables/entities/{s.id}.json')
        os.makedirs(os.path.dirname(lp), exist_ok=True)
        with open(lp, 'w') as f:
            json.dump(s.loot or essence_loot(s.faction), f, indent=1)
        print(f'  {s.id}: {len(M.order)} bones, tex {M.tw}x{M.th}', file=sys.stderr)
    with open(os.path.join(gen_dir, 'GenModels.java'), 'w') as f:
        f.write("""// GENERATED by gen/realm/mobs.py - do not edit by hand
package com.krolasyon.bosses.realm.client.gen;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class GenModels {
    private GenModels() {}

    public record Info(String id, Supplier<LayerDefinition> layer, AnimationDefinition[] anims, float scale, float shadow, boolean translucent,
                       float walkSpeed, float runSpeed, String head) {}

    public static final Map<String, Info> ALL = new LinkedHashMap<>();

    private static void add(String id, Supplier<LayerDefinition> layer, AnimationDefinition[] anims, float scale, float shadow, boolean translucent,
                            float walk, float run, String head) {
        ALL.put(id, new Info(id, layer, anims, scale, shadow, translucent, walk, run, head));
    }

    static {
""" + '\n'.join(reg) + """
    }
}
""")
    with open(os.path.join(java_root, 'entity', 'GenSpecs.java'), 'w') as f:
        f.write("""// GENERATED by gen/realm/mobs.py - do not edit by hand
package com.krolasyon.bosses.realm.entity;

import com.krolasyon.bosses.realm.Faction;

import java.util.List;

public final class GenSpecs {
    private GenSpecs() {}

""" + '\n\n'.join(java_spec(s) for s in SPECS) + """

    public static final List<MobSpec> ALL = List.of(""" + ', '.join(s.id.upper() for s in SPECS) + """);
}
""")
