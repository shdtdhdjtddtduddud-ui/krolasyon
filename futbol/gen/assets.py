"""Generates blockstates, block/item models, language files and sounds.json."""
import os, json

A = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolasyonfutbol')
M = 'krolasyonfutbol'


def w(rel, obj):
    p = os.path.join(A, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def simple_state(name):
    w(f'blockstates/{name}.json', {'variants': {'': {'model': f'{M}:block/{name}'}}})


def facing_state(name, model=None):
    model = model or name
    w(f'blockstates/{name}.json', {'variants': {
        f'facing={f}': ({'model': f'{M}:block/{model}', 'y': y} if y else {'model': f'{M}:block/{model}'})
        for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})


def item_block(name):
    w(f'models/item/{name}.json', {'parent': f'{M}:block/{name}'})


def item_gen(name, tex):
    w(f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': tex}})


def faces(tex, uvs=None, cull=None):
    out = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        f = {'texture': tex}
        if uvs and d in uvs:
            f['uv'] = uvs[d]
        if cull and d in cull:
            f['cullface'] = d
        out[d] = f
    return out


# grass / lines / steps
for n in ('pitch_grass_light', 'pitch_grass_dark', 'pitch_line'):
    simple_state(n)
    w(f'models/block/{n}.json', {'parent': 'minecraft:block/cube_bottom_top',
                                 'textures': {'top': f'{M}:block/{n}', 'side': f'{M}:block/pitch_side', 'bottom': 'minecraft:block/dirt'}})
    item_block(n)
simple_state('stand_step')
w('models/block/stand_step.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{M}:block/stand_step'}})
item_block('stand_step')

# goal post
w('models/block/goal_post.json', {
    'parent': 'minecraft:block/block', 'textures': {'particle': f'{M}:block/goal_post', 'all': f'{M}:block/goal_post'},
    'elements': [{'from': [5, 0, 5], 'to': [11, 16, 11],
                  'faces': {**{d: {'uv': [5, 0, 11, 16], 'texture': '#all'} for d in ('north', 'south', 'east', 'west')},
                            'up': {'uv': [5, 5, 11, 11], 'texture': '#all', 'cullface': 'up'},
                            'down': {'uv': [5, 5, 11, 11], 'texture': '#all', 'cullface': 'down'}}}]})
w('blockstates/goal_post.json', {'variants': {
    'axis=y': {'model': f'{M}:block/goal_post'},
    'axis=z': {'model': f'{M}:block/goal_post', 'x': 90},
    'axis=x': {'model': f'{M}:block/goal_post', 'x': 90, 'y': 90}}})
item_block('goal_post')

# goal net (pane)
for part in ('post', 'side', 'side_alt', 'noside', 'noside_alt'):
    w(f'models/block/goal_net_{part}.json', {'parent': f'minecraft:block/template_glass_pane_{part}', 'render_type': 'minecraft:cutout',
                                             'textures': {'pane': f'{M}:block/goal_net', 'edge': f'{M}:block/goal_net_edge',
                                                          'particle': f'{M}:block/goal_net'}})
g = f'{M}:block/goal_net_'
w('blockstates/goal_net.json', {'multipart': [
    {'apply': {'model': g + 'post'}},
    {'when': {'north': 'true'}, 'apply': {'model': g + 'side'}},
    {'when': {'east': 'true'}, 'apply': {'model': g + 'side', 'y': 90}},
    {'when': {'south': 'true'}, 'apply': {'model': g + 'side_alt'}},
    {'when': {'west': 'true'}, 'apply': {'model': g + 'side_alt', 'y': 90}},
    {'when': {'north': 'false'}, 'apply': {'model': g + 'noside'}},
    {'when': {'east': 'false'}, 'apply': {'model': g + 'noside_alt'}},
    {'when': {'south': 'false'}, 'apply': {'model': g + 'noside_alt', 'y': 90}},
    {'when': {'west': 'false'}, 'apply': {'model': g + 'noside', 'y': 270}}]})
item_gen('goal_net', f'{M}:block/goal_net')

# net roof
w('models/block/goal_net_roof.json', {
    'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
    'textures': {'particle': f'{M}:block/goal_net', 'net': f'{M}:block/goal_net'},
    'elements': [{'from': [0, 15, 0], 'to': [16, 16, 16], 'faces': {
        'up': {'uv': [0, 0, 16, 16], 'texture': '#net'}, 'down': {'uv': [0, 0, 16, 16], 'texture': '#net'}}}]})
simple_state('goal_net_roof')
item_gen('goal_net_roof', f'{M}:block/goal_net')

# corner flag
w('models/block/corner_flag.json', {
    'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
    'textures': {'particle': f'{M}:block/corner_flag', 'f': f'{M}:block/corner_flag'},
    'elements': [
        {'from': [7.5, 0, 7.5], 'to': [8.5, 16, 8.5], 'faces': {d: {'uv': [0, 0, 1, 16], 'texture': '#f'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}},
        {'from': [8.5, 9, 8], 'to': [15.5, 16, 8], 'faces': {
            'north': {'uv': [9, 0, 2, 8], 'texture': '#f'}, 'south': {'uv': [2, 0, 9, 8], 'texture': '#f'}}},
    ]})
simple_state('corner_flag')
item_gen('corner_flag', f'{M}:block/corner_flag')

# seats
for n in ('seat_red', 'seat_blue', 'seat_white'):
    w(f'models/block/{n}.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': f'{M}:block/{n}', 's': f'{M}:block/{n}', 'leg': 'minecraft:block/gray_concrete'},
        'elements': [
            {'from': [6, 0, 6], 'to': [10, 6, 10], 'faces': faces('#leg')},
            {'from': [1, 6, 1], 'to': [15, 8, 14], 'faces': faces('#s')},
            {'from': [1, 8, 13], 'to': [15, 16, 15], 'faces': faces('#s')},
        ]})
    facing_state(n)
    item_block(n)

# floodlight
w('models/block/floodlight.json', {'parent': 'minecraft:block/orientable', 'textures': {
    'top': f'{M}:block/floodlight_side', 'front': f'{M}:block/floodlight_front', 'side': f'{M}:block/floodlight_side'}})
facing_state('floodlight')
item_block('floodlight')

# items
for n in ('football', 'pitch_builder', 'whistle', 'bot_red', 'bot_blue'):
    item_gen(n, f'{M}:item/{n}')

LANG = {
    'itemGroup.krolasyonfutbol': 'Krolasyon Futbol',
    'item.krolasyonfutbol.football': 'Futbol Topu',
    'item.krolasyonfutbol.pitch_builder': 'Saha Kurucu',
    'item.krolasyonfutbol.whistle': 'Hakem Düdüğü',
    'item.krolasyonfutbol.bot_red': 'Kızıl Aslanlar Oyuncusu',
    'item.krolasyonfutbol.bot_blue': 'Mavi Kartallar Oyuncusu',
    'block.krolasyonfutbol.pitch_grass_light': 'Saha Çimi (Açık)',
    'block.krolasyonfutbol.pitch_grass_dark': 'Saha Çimi (Koyu)',
    'block.krolasyonfutbol.pitch_line': 'Saha Çizgisi',
    'block.krolasyonfutbol.goal_post': 'Kale Direği',
    'block.krolasyonfutbol.goal_net': 'Kale Filesi',
    'block.krolasyonfutbol.goal_net_roof': 'Kale Filesi (Tavan)',
    'block.krolasyonfutbol.corner_flag': 'Korner Bayrağı',
    'block.krolasyonfutbol.seat_red': 'Tribün Koltuğu (Kırmızı)',
    'block.krolasyonfutbol.seat_blue': 'Tribün Koltuğu (Mavi)',
    'block.krolasyonfutbol.seat_white': 'Tribün Koltuğu (Beyaz)',
    'block.krolasyonfutbol.floodlight': 'Stadyum Projektörü',
    'block.krolasyonfutbol.stand_step': 'Tribün Basamağı',
    'entity.krolasyonfutbol.football': 'Futbol Topu',
    'entity.krolasyonfutbol.footballer': 'Futbolcu',
    'key.categories.krolasyonfutbol': 'Krolasyon Futbol',
    'key.krolasyonfutbol.shoot': 'Şut (basılı tut: güç)',
    'key.krolasyonfutbol.pass': 'Pas',
    'key.krolasyonfutbol.tackle': 'Top Çal / Müdahale',
    'key.krolasyonfutbol.skill': 'Çalım 1',
    'key.krolasyonfutbol.super': 'Süper Yetenek',
    'key.krolasyonfutbol.moves': 'Hareket Menüsü',
    'key.krolasyonfutbol.match': 'Maç / Takım Menüsü',
    'key.krolasyonfutbol.celebrate': 'Gol Sevinci',
    'key.krolasyonfutbol.skill2': 'Çalım 2',
    'key.krolasyonfutbol.skill3': 'Çalım 3',
    'key.krolasyonfutbol.request': 'Pas İste',
    'key.krolasyonfutbol.camera': 'Kamera Modu (Normal / TV / Üstten)',
    'key.krolasyonfutbol.replay': 'Gol Tekrarı',
    'key.krolasyonfutbol.club': 'Kulüp (Jeton & Kartlar)',
}
SUB = {'kick': 'Topa vuruldu', 'kick_power': 'Sert şut', 'pass': 'Pas', 'header': 'Kafa vuruşu', 'bounce': 'Top sekti',
       'post_hit': 'Direk!', 'net_hit': 'Top fileye gitti', 'whistle': 'Hakem düdüğü', 'whistle_end': 'Maç sonu düdüğü',
       'crowd_cheer': 'Seyirci coşuyor', 'crowd_ambient': 'Tribün uğultusu', 'crowd_ooh': 'Seyirci: Uuuh!', 'goal_horn': 'Gol kornası',
       'slide': 'Kayarak müdahale', 'super_charge': 'Süper yetenek', 'skill': 'Çalım', 'tackle': 'Müdahale', 'catch': 'Kaleci topu tuttu'}
for k, v in SUB.items():
    LANG[f'subtitles.krolasyonfutbol.{k}'] = v
w('lang/tr_tr.json', LANG)
w('lang/en_us.json', LANG)

SOUNDS = ['kick', 'kick_power', 'pass', 'header', 'bounce', 'post_hit', 'net_hit', 'whistle', 'whistle_end', 'crowd_cheer',
          'crowd_ambient', 'crowd_ooh', 'goal_horn', 'slide', 'super_charge', 'skill', 'tackle', 'catch']
VARIANTS = {'kick': 3, 'pass': 2, 'bounce': 2, 'crowd_ambient': 2}
snd = {}
for s in SOUNDS:
    n = VARIANTS.get(s, 1)
    files = [f'{M}:{s}' if n == 1 else f'{M}:{s}{i + 1}' for i in range(n)]
    entry = {'sounds': [{'name': f, 'stream': s.startswith('crowd')} if s.startswith('crowd') else f for f in files]}
    entry['subtitle'] = f'subtitles.{M}.{s}'
    snd[s] = entry
w('sounds.json', snd)
print('assets done')
