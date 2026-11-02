# -*- coding: utf-8 -*-
"""Generates lang entries, StoryData.java, story textures/models/blockstates and tape frames."""
import os, json, math, numpy as np
from PIL import Image, ImageDraw, ImageFilter
import story_def as S

ROOT = os.path.join(os.path.dirname(__file__), '../common/src/main/resources')
A = os.path.join(ROOT, 'assets/krolasyonhorror')
JAVA = os.path.join(os.path.dirname(__file__), '../common/src/main/java/com/krolasyon/horror/story')
for d in (A + '/textures/block', A + '/textures/item', A + '/textures/tape', A + '/textures/gui', A + '/models/block', A + '/models/item', A + '/blockstates', JAVA,
          ROOT + '/data/krolasyonhorror/loot_tables/blocks', ROOT + '/data/krolasyonhorror/loot_table/blocks'):
    os.makedirs(d, exist_ok=True)
R = np.random.default_rng(13)

# ============================================================================================ lang
L = {'en_us': {}, 'tr_tr': {}}


def put(key, en, tr):
    L['en_us'][key] = en
    L['tr_tr'][key] = tr


for k, (en, tr) in S.SPEAKERS.items():
    put(f'speaker.krolasyonhorror.{k}', en, tr)
for i, (en, tr, sen,str_) in enumerate(S.CHAPTERS, 1):
    put(f'chapter.krolasyonhorror.{i}', en, tr)
    put(f'chapter.krolasyonhorror.{i}.sub', sen, str_)
for k, (en, tr) in S.OBJECTIVES.items():
    put(f'objective.krolasyonhorror.{k}', en, tr)
for k, (en, tr) in S.MESSAGES.items():
    put(f'msg.krolasyonhorror.{k}', en, tr)
for name, en, tr in S.ITEMS:
    put(f'item.krolasyonhorror.{name}', en, tr)
for i, (ten, ttr, ben, btr) in enumerate(S.NOTES):
    put(f'note.krolasyonhorror.{i}.title', ten, ttr)
    put(f'note.krolasyonhorror.{i}.body', ben, btr)
for k, (en, tr) in S.ENDINGS.items():
    put(f'ending.krolasyonhorror.{k}', en, tr)
for i, (en, tr) in enumerate(S.CREDITS):
    put(f'credits.krolasyonhorror.{i}', en, tr)
for k, (en, tr) in (('note_paper', ('Note', 'Not')), ('story_item', ('Item', 'Eşya')), ('vcr', ('VCR', 'Video Oynatıcı')),
                    ('fuse_box', ('Fuse Box', 'Sigorta Kutusu')), ('dial', ('Transmitter Dial', 'Verici Kadranı')),
                    ('straw_hat', ("Mr. Brim's Hat", 'Bay Kenar\'ın Şapkası'))):
    put(f'block.krolasyonhorror.{k}', en, tr)
for ev, (en, tr) in {'story.rain': ('Rain', 'Yağmur'), 'story.phone_beep': ('Answering machine beeps', 'Telesekreter bipliyor'),
                     'story.house_amb': ('The house settles', 'Ev gıcırdıyor'), 'story.door_slam': ('Door slams', 'Kapı çarpıyor'),
                     'story.power_on': ('Power hums back on', 'Elektrik geri geliyor'), 'story.tv_on': ('Television turns on', 'Televizyon açılıyor'),
                     'story.jingle': ('A cheerful jingle', 'Neşeli bir jenerik müziği'), 'story.music_box': ('A music box plays', 'Bir müzik kutusu çalıyor'),
                     'story.radio_sweep': ('Radio tuning', 'Radyo ayarlanıyor'), 'story.applause': ('Applause', 'Alkış'),
                     'story.fire': ('Fire crackles', 'Ateş çıtırdıyor'), 'story.page': ('Paper rustles', 'Kâğıt hışırdıyor'),
                     'story.vcr_insert': ('VCR clunks', 'Video oynatıcı takırdıyor'), 'story.brim_voice': ('Mr. Brim speaks', 'Bay Kenar konuşuyor'),
                     'story.pickup': ('Item picked up', 'Eşya alındı'), 'story.candle_light': ('Lighter flicks', 'Çakmak çakılıyor'),
                     'story.balloon_pop': ('Balloon pops', 'Balon patlıyor'), 'story.dial_click': ('Dial clicks', 'Kadran tıklıyor'),
                     'story.fuse_spark': ('Electric spark', 'Elektrik kıvılcımı')}.items():
    put(f'subtitles.krolasyonhorror.{ev}', en, tr)


# ============================================================================================ timelines
def dur_for(en, tr):
    return int(min(280, max(60, 46 + 1.25 * max(len(en), len(tr)))))


def lines_layout(prefix, lines, start=0):
    out = []
    t = start
    for i, (sp, en, tr, gap) in enumerate(lines):
        t += gap
        d = dur_for(en, tr)
        key = f'{prefix}.{i}'
        put(key, en, tr)
        out.append((t, d, key, sp))
        t += d
    return out, t


def jf(x):
    return f'{x:.3f}'.rstrip('0').rstrip('.') if isinstance(x, float) else str(x)


java_cs, java_tp = [], []
for cid, c in S.CUTSCENES.items():
    lines, end = lines_layout(f'story.krolasyonhorror.{cid}', c['lines'], 10)
    shots = list(c['shots'])
    total = sum(s[2] for s in shots)
    if end + 30 > total:  # stretch the last shot so every line is read
        a, b, n = shots[-1]
        shots[-1] = (a, b, n + end + 30 - total)
        total = end + 30
    t = 0
    sj = []
    for a, b, n in shots:
        sj.append(f'new Shot({t}, {t + n}, {", ".join(jf(float(v)) for v in a[:3])}, {jf(float(a[3]))}F, {jf(float(a[4]))}F, '
                  f'{", ".join(jf(float(v)) for v in b[:3])}, {jf(float(b[3]))}F, {jf(float(b[4]))}F)')
        t += n
    lj = [f'new Line({s}, {d}, "{k}", "{sp}")' for s, d, k, sp in lines]
    cj = [f'new Cue({tk}, "{snd}", {v}F)' for tk, snd, v in c['cues']]
    java_cs.append(f'        CUTSCENES.put("{cid}", new Cutscene("{cid}", {total}, new Shot[]{{{", ".join(sj)}}}, new Line[]{{{", ".join(lj)}}}, new Cue[]{{{", ".join(cj)}}}));')

for tid, tp in S.TAPES.items():
    lines, end = lines_layout(f'tape.krolasyonhorror.{tid}', tp['lines'], 20)
    total = end + 50
    put(f'tape.krolasyonhorror.{tid}.label', *tp['label'])
    fr = tp['frames']
    fj = [f'new Frame({int(total * i / len(fr))}, "{f}")' for i, f in enumerate(fr)]
    cues = list(tp['cues'])
    for s, d, k, sp in lines:
        if sp == 'brim':
            cues.append((s, 'story.brim_voice', 0.55))
    cj = [f'new Cue({tk}, "{snd}", {v}F)' for tk, snd, v in sorted(cues)]
    lj = [f'new Line({s}, {d}, "{k}", "{sp}")' for s, d, k, sp in lines]
    java_tp.append(f'        TAPES.put("{tid}", new Tape("{tid}", {total}, new Frame[]{{{", ".join(fj)}}}, new Line[]{{{", ".join(lj)}}}, new Cue[]{{{", ".join(cj)}}}));')

with open(os.path.join(JAVA, 'StoryData.java'), 'w') as f:
    f.write(f"""// GENERATED by gen/story_assets.py from gen/story_def.py - do not edit by hand
package com.krolasyon.horror.story;

import java.util.HashMap;
import java.util.Map;

/** Timelines of every cutscene and tape (ticks). Subtitle keys resolve through the lang files, so the story follows the game language. */
public final class StoryData {{
    private StoryData() {{}}

    public record Shot(int start, int end, double x0, double y0, double z0, float yaw0, float pitch0, double x1, double y1, double z1, float yaw1, float pitch1) {{}}
    public record Line(int start, int dur, String key, String speaker) {{}}
    public record Cue(int tick, String sound, float volume) {{}}
    public record Frame(int tick, String texture) {{}}
    public record Cutscene(String id, int length, Shot[] shots, Line[] lines, Cue[] cues) {{}}
    public record Tape(String id, int length, Frame[] frames, Line[] lines, Cue[] cues) {{}}

    public static final int NOTE_COUNT = {len(S.NOTES)};
    public static final int CREDIT_LINES = {len(S.CREDITS)};
    public static final Map<String, Cutscene> CUTSCENES = new HashMap<>();
    public static final Map<String, Tape> TAPES = new HashMap<>();

    static {{
{chr(10).join(java_cs)}
{chr(10).join(java_tp)}
    }}
}}
""")

for lang in ('en_us', 'tr_tr'):
    p = f'{A}/lang/{lang}.json'
    d = json.load(open(p))
    d.update(L[lang])
    json.dump(d, open(p, 'w'), ensure_ascii=False, indent=2)
assert set(L['en_us']) == set(L['tr_tr'])


# ============================================================================================ textures
def noise(h, w, scale, seed):
    r = np.random.default_rng(seed)
    g = r.random((max(2, int(h / scale)) + 1, max(2, int(w / scale)) + 1))
    return np.asarray(Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)).astype(np.float32) / 255


def save(path, arr):
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).save(path)


def item_tex(name, draw):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw(ImageDraw.Draw(im))
    im.save(f'{A}/textures/item/{name}.png')


def tape(label):
    def d(g):
        g.rectangle((1, 4, 14, 12), fill=(24, 22, 22, 255), outline=(10, 10, 10, 255))
        g.rectangle((3, 5, 12, 8), fill=label)
        g.ellipse((4, 9, 6, 11), fill=(120, 120, 120, 255)); g.ellipse((9, 9, 11, 11), fill=(120, 120, 120, 255))
        g.line((4, 6, 11, 6), fill=(60, 60, 60, 255))
    return d


item_tex('tape_birthday', tape((236, 228, 200, 255)))
item_tex('tape_party', tape((240, 190, 80, 255)))
item_tex('master_tape', tape((170, 30, 30, 255)))
item_tex('fuse', lambda g: (g.rectangle((5, 2, 10, 13), fill=(200, 220, 230, 180), outline=(150, 170, 180, 255)),
                            g.rectangle((5, 1, 10, 3), fill=(180, 180, 180, 255)), g.rectangle((5, 12, 10, 14), fill=(180, 180, 180, 255)),
                            g.line((7, 4, 8, 11), fill=(220, 120, 40, 255))))
item_tex('stair_key', lambda g: (g.ellipse((2, 2, 7, 7), outline=(200, 160, 70, 255), width=2), g.line((6, 6, 13, 13), fill=(200, 160, 70, 255), width=2),
                                 g.line((11, 11, 13, 9), fill=(200, 160, 70, 255)), g.line((12, 12, 14, 10), fill=(200, 160, 70, 255))))
item_tex('attic_key', lambda g: (g.ellipse((2, 2, 8, 8), outline=(120, 124, 130, 255), width=2), g.line((7, 7, 14, 14), fill=(120, 124, 130, 255), width=2),
                                 g.rectangle((11, 12, 13, 14), fill=(120, 124, 130, 255))))
item_tex('lighter', lambda g: (g.rectangle((4, 5, 11, 14), fill=(190, 190, 196, 255), outline=(120, 120, 126, 255)),
                               g.rectangle((5, 2, 10, 5), fill=(150, 150, 156, 255)), g.ellipse((6, 0, 9, 3), fill=(255, 170, 60, 255))))
item_tex('eviction_notice', lambda g: (g.rectangle((3, 1, 13, 14), fill=(236, 228, 206, 255), outline=(160, 150, 130, 255)),
                                       [g.line((5, y, 11, y), fill=(90, 80, 70, 255)) for y in (4, 6, 8)], g.ellipse((8, 9, 12, 13), outline=(180, 30, 30, 255))))
B = A + '/textures/block'
paper = np.zeros((16, 16, 3)) + (232, 224, 204)
paper = paper * (0.9 + 0.1 * noise(16, 16, 4, 3))[..., None]
for y in range(3, 14, 2):
    paper[y, 3:13] = (120, 110, 100)
save(f'{B}/note_paper.png', paper)
gp = np.zeros((320, 256, 3)) + (226, 214, 186)
gp = gp * (0.86 + 0.14 * noise(320, 256, 40, 5))[..., None]
st = noise(320, 256, 60, 9)
gp = gp * (1 - 0.15 * np.clip((st - 0.6) * 4, 0, 1))[..., None]
gp[:, :6] *= 0.85; gp[:, -6:] *= 0.85; gp[:6] *= 0.85; gp[-6:] *= 0.85
save(f'{A}/textures/gui/paper.png', gp)
for i in range(4):
    fb = np.zeros((16, 16, 3)) + (120, 124, 120)
    fb = fb * (0.85 + 0.15 * noise(16, 16, 3, 20 + i))[..., None]
    fb[1:15, 1] = fb[1:15, 14] = (70, 72, 70)
    for k in range(3):
        x = 3 + k * 4
        fb[5:11, x:x + 2] = (30, 30, 30)
        if k < i:
            fb[5:11, x:x + 2] = (200, 220, 230); fb[7:9, x:x + 2] = (230, 140, 50)
    fb[2, 3:13] = (240, 200, 40)
    save(f'{B}/fuse_box_{i}.png', fb)
DIG = {0: ['111', '101', '101', '101', '111'], 1: ['010', '110', '010', '010', '111'], 2: ['111', '001', '111', '100', '111'], 3: ['111', '001', '111', '001', '111'],
       4: ['101', '101', '111', '001', '001'], 5: ['111', '100', '111', '001', '111'], 6: ['111', '100', '111', '101', '111'], 7: ['111', '001', '010', '010', '010'],
       8: ['111', '101', '111', '101', '111'], 9: ['111', '101', '111', '001', '111']}
for d, rows in DIG.items():
    t = np.zeros((16, 16, 3)) + (40, 40, 44)
    t[2:14, 3:13] = (16, 18, 16)
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == '1':
                t[3 + y * 2:5 + y * 2, 4 + x * 3:6 + x * 3] = (120, 240, 140)
    save(f'{B}/dial_{d}.png', t)
vcr = np.zeros((16, 16, 3)) + (40, 40, 42)
vcr[4:7, 2:9] = (12, 12, 12); vcr[4:6, 11] = (220, 40, 40); vcr[9:11, 10:14] = (90, 220, 120)
save(f'{B}/vcr_front.png', vcr)
save(f'{B}/glass_dome_off.png', np.zeros((16, 16, 3)) + (120, 116, 108))
save(f'{B}/bulb_off.png', np.zeros((16, 16, 3)) + (110, 108, 100))
off = np.zeros((16, 16, 3)) + (20, 24, 26)
off[2:5, 2:6] = (60, 66, 70)
save(f'{B}/crt_screen_off.png', off)
straw = np.zeros((16, 16, 3)) + (188, 162, 118)
yy, xx = np.mgrid[0:16, 0:16]
straw[((xx + yy) // 2) % 2 == 0] = (214, 192, 150)
straw = straw * (0.85 + 0.15 * noise(16, 16, 3, 33))[..., None]
save(f'{B}/straw.png', straw)
save(f'{B}/hat_band.png', np.zeros((16, 16, 3)) + (30, 22, 18))

# ============================================================================================ models / blockstates
M = {}


def el(fr, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    return {'from': fr, 'to': to, 'faces': {f: {'texture': '#' + tex} for f in faces}}


def model(name, textures, elements, particle=None):
    tx = {k: (v if ':' in v else f'krolasyonhorror:block/{v}') for k, v in textures.items()}
    tx['particle'] = tx[particle] if particle else list(tx.values())[0]
    M[name] = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': tx, 'elements': elements}


for raised in (False, True):
    o = -6 if raised else 0
    sfx = '_raised' if raised else ''
    model('note_paper' + sfx, {'p': 'note_paper'}, [el([3, o, 3], [13, o + 0.25, 13], 'p')])
    for name, _, _ in S.ITEMS:
        model(f'story_{name}{sfx}', {'i': f'krolasyonhorror:item/{name}'}, [el([2, o, 2], [14, o + 0.3, 14], 'i', faces=('up', 'down'))])
model('vcr', {'b': 'crt_body', 'f': 'vcr_front'}, [{'from': [1, 0, 3], 'to': [15, 3.5, 13], 'faces': {
    'north': {'texture': '#f'}, 'south': {'texture': '#b'}, 'east': {'texture': '#b'}, 'west': {'texture': '#b'}, 'up': {'texture': '#b'}, 'down': {'texture': '#b'}}}])
for i in range(4):
    model(f'fuse_box_{i}', {'f': f'fuse_box_{i}', 'b': 'crt_body'}, [{'from': [3, 2, 13], 'to': [13, 14, 16], 'faces': {
        'north': {'texture': '#f'}, 'south': {'texture': '#b'}, 'east': {'texture': '#b'}, 'west': {'texture': '#b'}, 'up': {'texture': '#b'}, 'down': {'texture': '#b'}}}])
for d in range(10):
    model(f'dial_{d}', {'f': f'dial_{d}', 'b': 'crt_body', 'k': 'brass'}, [
        {'from': [2, 0, 4], 'to': [14, 9, 14], 'faces': {'north': {'texture': '#f'}, 'south': {'texture': '#b'}, 'east': {'texture': '#b'},
                                                         'west': {'texture': '#b'}, 'up': {'texture': '#b'}, 'down': {'texture': '#b'}}},
        el([6, 9, 7], [10, 12, 11], 'k')])
model('straw_hat', {'s': 'straw', 'b': 'hat_band'}, [el([0, -6, 0], [16, -5, 16], 's'), el([4, -5, 4], [12, -1, 12], 's'), el([3.8, -5, 3.8], [12.2, -4, 12.2], 'b')])
# lamp "off" variants
model('ceiling_lamp_off', {'brass': 'brass', 'glass': 'glass_dome_off'}, [
    el([5, 15, 5], [11, 16, 11], 'brass'), el([5.5, 12, 5.5], [10.5, 15, 10.5], 'glass'), el([4.5, 12.5, 6.5], [11.5, 14.5, 9.5], 'glass'),
    el([6.5, 12.5, 4.5], [9.5, 14.5, 11.5], 'glass'), el([7.5, 11.2, 7.5], [8.5, 12, 8.5], 'brass')], particle='glass')
model('bulb_fixture_off', {'brass': 'brass', 'bulb': 'bulb_off'}, [
    el([6, 15, 6], [10, 16, 10], 'brass'), el([7, 13, 7], [9, 15, 9], 'brass'), el([6.5, 10, 6.5], [9.5, 13, 9.5], 'bulb')], particle='bulb')
model('candle_sconce_off', {'brass': 'brass', 'wax': 'candle_wax'}, [
    el([6.5, 3, 15], [9.5, 13, 16], 'brass'), el([7.5, 6, 11], [8.5, 7, 15], 'brass'), el([6, 7, 9], [10, 8, 13], 'brass'), el([7, 8, 10], [9, 12, 12], 'wax')])
tv = json.load(open(f'{A}/models/block/crt_tv.json'))
tv['textures']['screen'] = 'krolasyonhorror:block/crt_screen_off'
M['crt_tv_off'] = tv

for n, m in M.items():
    json.dump(m, open(f'{A}/models/block/{n}.json', 'w'), indent=1)

FACE = (('north', 0), ('east', 90), ('south', 180), ('west', 270))


def bs(name, variants):
    json.dump({'variants': variants}, open(f'{A}/blockstates/{name}.json', 'w'), indent=1)


def mref(n, y=0):
    return {'model': f'krolasyonhorror:block/{n}', **({'y': y} if y else {})}


bs('note_paper', {f'note={i},raised={str(r).lower()}': mref('note_paper' + ('_raised' if r else '')) for i in range(20) for r in (False, True)})
bs('story_item', {f'item={i},raised={str(r).lower()}': mref(f'story_{S.ITEMS[i][0]}' + ('_raised' if r else '')) for i in range(len(S.ITEMS)) for r in (False, True)})
bs('vcr', {f'facing={f}': mref('vcr', y) for f, y in FACE})
bs('fuse_box', {f'facing={f},fuses={i}': mref(f'fuse_box_{i}', y) for f, y in FACE for i in range(4)})
bs('dial', {f'digit={d},facing={f}': mref(f'dial_{d}', y) for f, y in FACE for d in range(10)})
bs('straw_hat', {'': mref('straw_hat')})
bs('ceiling_lamp', {'lit=true': mref('ceiling_lamp'), 'lit=false': mref('ceiling_lamp_off')})
bs('bulb_fixture', {'lit=true': mref('bulb_fixture'), 'lit=false': mref('bulb_fixture_off')})
bs('candle_sconce', {f'facing={f},lit={l}': mref('candle_sconce' + ('' if l == 'true' else '_off'), y) for f, y in FACE for l in ('true', 'false')})
bs('crt_tv', {f'facing={f},lit={l}': mref('crt_tv' + ('' if l == 'true' else '_off'), y) for f, y in FACE for l in ('true', 'false')})

for n, parent in (('note_paper', 'note_paper'), ('story_item', 'story_tape_birthday'), ('vcr', 'vcr'), ('fuse_box', 'fuse_box_0'), ('dial', 'dial_0'), ('straw_hat', 'straw_hat')):
    json.dump({'parent': f'krolasyonhorror:block/{parent}'}, open(f'{A}/models/item/{n}.json', 'w'))
for name, _, _ in S.ITEMS:
    json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'krolasyonhorror:item/{name}'}}, open(f'{A}/models/item/{name}.json', 'w'))
for n in ('vcr', 'fuse_box', 'dial', 'straw_hat'):
    loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'krolasyonhorror:{n}'}]}]}
    for folder in ('loot_tables', 'loot_table'):
        json.dump(loot, open(f'{ROOT}/data/krolasyonhorror/{folder}/blocks/{n}.json', 'w'))
print('lang keys', len(L['en_us']), 'models', len(M))
