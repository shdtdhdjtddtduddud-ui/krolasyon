"""Player-layout skins (128x128 = 2x HD) for every NPC of Seoul."""
import os
import numpy as np
import tex as T
from model import Model, Cube

H = T.hexc
R = 2


def box(img, u, v, w, h, d, mat, decals=None):
    c = Cube(None, 0, 0, 0, w, h, d, mat, decals=decals or {})
    c.u, c.v = u, v
    m = Model('skin', 64, R)
    m.cubes = [c]
    m.tex_h = 64
    full, _ = m.paint()
    sub = full[v * R:(v + d + h) * R, u * R:(u + 2 * (w + d)) * R]
    a = sub[:, :, 3:4]
    dst = img[v * R:(v + d + h) * R, u * R:(u + 2 * (w + d)) * R]
    dst[:] = dst * (1 - a) + sub * a


def face_decal(spec):
    def d(img, g):
        h, w = img.shape[:2]
        k = h / 16
        eye = H(spec.get('eyes', '#3A2A1E'))
        for cx in (4.5, 11.5):
            x0 = cx - 1.5
            T.rect(img, x0 * k, 9 * k, (x0 + 3) * k, 11 * k, H('#F4F4F4'))
            T.rect(img, (cx - 0.5) * k if cx > 8 else (cx - 1) * k, 9 * k, (cx + 1) * k if cx > 8 else (cx + 0.5) * k, 11 * k, eye)
            T.rect(img, x0 * k, 8 * k, (x0 + 3) * k, 8.6 * k, H(spec.get('brow', spec['hair'])))
        skin = H(spec['skin'])
        T.rect(img, 7.4 * k, 11.5 * k, 8.6 * k, 12.4 * k, np.append(skin[:3] * 0.82, 1))
        T.rect(img, 6.5 * k, 13.5 * k, 9.5 * k, 14.1 * k, H(spec.get('mouth', '#9A5A50')))
        if spec.get('glasses'):
            for cx in (4.5, 11.5):
                for (a, b, c_, d_) in ((cx - 2, 8.6, cx + 2, 9), (cx - 2, 11, cx + 2, 11.4), (cx - 2, 8.6, cx - 1.6, 11.4), (cx + 1.6, 8.6, cx + 2, 11.4)):
                    T.rect(img, a * k, b * k, c_ * k, d_ * k, H(spec['glasses']))
            T.rect(img, 6.5 * k, 9.4 * k, 9.5 * k, 9.8 * k, H(spec['glasses']))
        if spec.get('sunglasses'):
            T.rect(img, 2.5 * k, 8.8 * k, 13.5 * k, 11.4 * k, H('#0A0A0E'))
            T.rect(img, 3 * k, 9.2 * k, 6 * k, 9.6 * k, H('#4A4A5A'))
        if spec.get('beard'):
            T.rect(img, 3 * k, 12.5 * k, 13 * k, 16 * k, H(spec['beard']))
            T.rect(img, 6.5 * k, 13.5 * k, 9.5 * k, 14.1 * k, H('#5A3A30'))
        if spec.get('scar'):
            T.line(img, 10 * k, 7 * k, 13 * k, 12 * k, H('#B05050'), 0.6 * k)
        if spec.get('wrinkles'):
            for y in (7.5, 12.8):
                T.rect(img, 3 * k, y * k, 5 * k, (y + 0.3) * k, np.append(skin[:3] * 0.7, 0.8))
                T.rect(img, 11 * k, y * k, 13 * k, (y + 0.3) * k, np.append(skin[:3] * 0.7, 0.8))
        style = spec.get('style', 'short')
        hair = H(spec['hair'])
        bang = {'short': 6.5, 'long': 7, 'bob': 7.5, 'slick': 4.5, 'bald': 0, 'bun': 5.5, 'ponytail': 6.5, 'spiky': 6}[style]
        if bang > 0:
            T.rect(img, 0, 0, w, bang * k, hair)
            if style in ('short', 'long', 'bob', 'ponytail', 'spiky'):
                for x in range(0, 16, 3):
                    T.rect(img, x * k, bang * k, (x + 1.5) * k, (bang + 1) * k, hair)
        if style in ('long', 'bob'):
            T.rect(img, 0, 0, 1.5 * k, (13 if style == 'long' else 11) * k, hair)
            T.rect(img, 14.5 * k, 0, w, (13 if style == 'long' else 11) * k, hair)
    return d


def hair_side(spec, length):
    def d(img, g):
        h, w = img.shape[:2]
        L = {'short': 0.45, 'long': 1.0, 'bob': 0.8, 'slick': 0.4, 'bald': 0.0, 'bun': 0.45, 'ponytail': 0.5, 'spiky': 0.5}[spec.get('style', 'short')]
        if L > 0:
            T.rect(img, 0, 0, w, h * L * length, H(spec['hair']))
    return d


def paint(spec):
    img = T.new(128, 128)
    skin = T.m_skin(spec['skin'], var=0.06)
    hair = T.m_hair(spec['hair'])
    top = spec['top']
    pants = spec['pants']
    shoes = spec.get('shoes', T.m_leather('#1A1A1E'))
    box(img, 0, 0, 8, 8, 8, skin, {'front': [face_decal(spec)], 'right': [hair_side(spec, 1)], 'left': [hair_side(spec, 1)],
                                   'back': [hair_side(spec, 1)] if spec.get('style') != 'bald' else [],
                                   'top': [lambda i, g: T.rect(i, 0, 0, i.shape[1], i.shape[0], H(spec['hair']))] if spec.get('style') != 'bald' else []})
    if spec.get('style') in ('long', 'ponytail', 'bun', 'spiky', 'bob'):
        def hat(i, g, s=spec):
            hh, ww = i.shape[:2]
            T.rect(i, 0, 0, ww, hh, np.array([0, 0, 0, 0], np.float32))
        side_len = 0.95 if spec['style'] == 'long' else 0.55
        hat = T.new(128, 128)
        box(hat, 32, 0, 8, 8, 8, hair, {'front': [clear(0.32)], 'bottom': [clear(0)], 'right': [clear(side_len)], 'left': [clear(side_len)]})
        a = hat[:, :, 3:4]
        img[:] = img * (1 - a) + hat * a
    box(img, 16, 16, 8, 12, 4, top, spec.get('top_decals'))
    box(img, 40, 16, 4, 12, 4, spec.get('sleeves', top), {'bottom': [], 'all': [lambda i, g: T.rect(i, 0, i.shape[0] * 0.78, i.shape[1], i.shape[0], H(spec['skin']))]} if spec.get('short_sleeves') else None)
    box(img, 32, 48, 4, 12, 4, spec.get('sleeves', top), {'all': [lambda i, g: T.rect(i, 0, i.shape[0] * 0.78, i.shape[1], i.shape[0], H(spec['skin']))]} if spec.get('short_sleeves') else None)
    # hands
    for (u, v) in ((40, 16), (32, 48)):
        for fx, fw in ((u, 4), (u + 4, 4), (u + 8, 4), (u + 12, 4)):
            T.rect(img, fx * R, (v + 4 + 10.5) * R, (fx + fw) * R, (v + 16) * R, H(spec['skin']))
    box(img, 0, 16, 4, 12, 4, pants)
    box(img, 16, 48, 4, 12, 4, pants)
    for (u, v) in ((0, 16), (16, 48)):
        T.rect(img, u * R, (v + 4 + 9) * R, (u + 16) * R, (v + 16) * R, np.append(H(spec.get('shoe_color', '#1A1A1E'))[:3], 1))
    return img


def clear(y0, y1=1.0):
    def d(img, g):
        h = img.shape[0]
        img[int(h * y0):int(h * y1)] = 0
    return d


def suit(color, shirt='#F4F4F4', tie=None, emblem=None):
    def tie_d(img, g):
        h, w = img.shape[:2]
        T.rect(img, w * 0.38, 0, w * 0.62, h * 0.45, H(shirt))
        if tie:
            T.rect(img, w * 0.45, h * 0.02, w * 0.55, h * 0.42, H(tie))
        T.line(img, w * 0.38, 0, w * 0.5, h * 0.5, np.append(H(color)[:3] * 0.6, 1), max(1, w / 16))
        T.line(img, w * 0.62, 0, w * 0.5, h * 0.5, np.append(H(color)[:3] * 0.6, 1), max(1, w / 16))
        if emblem:
            T.draw_mask(img, T.ellipse_mask(w, h, w * 0.25, h * 0.25, w * 0.07, h * 0.04, 0.4), H(emblem))
    return T.m_cloth(color), {'front': [tie_d]}


def casual(color, accent):
    def d(img, g):
        h, w = img.shape[:2]
        T.rect(img, 0, h * 0.35, w, h * 0.42, H(accent))
    return T.m_cloth(color), {'front': [d]}


def armor_top(color, trim):
    return T.m_metal(color, trim=trim, rivets=False), {'front': [lambda img, g: T.decal_band(img, 0.45, 0.52, trim)]}


SKINS = {}


def npc(name, skin, hair, style, top, pants, **kw):
    t, td = top
    SKINS[name] = dict(skin=skin, hair=hair, style=style, top=t, top_decals=td, pants=pants, **kw)


LIGHT, MID, TAN = '#F2D6C0', '#E8C4A6', '#D8AA88'
npc('receptionist', LIGHT, '#4A2E1E', 'long', suit('#1E2E5A', tie='#3A8AFF', emblem='#5FC8FF'), T.m_cloth('#1E2E5A'), eyes='#2A1A10', mouth='#C0606A')
npc('woo_jinchul', MID, '#14141A', 'short', suit('#14141A', tie='#14141A'), T.m_cloth('#14141A'), sunglasses=True)
npc('chairman', '#E0BCA0', '#C8C8CC', 'slick', suit('#3A3A42', tie='#7A1A20'), T.m_cloth('#3A3A42'), wrinkles=True, brow='#E0E0E0', beard=None)
npc('master_hunters', LIGHT, '#8A6A3A', 'slick', suit('#F0F0F2', shirt='#1A1A1A', tie='#B01A1A'), T.m_cloth('#E8E8EC'), glasses='#1A1A1A')
npc('master_white_tiger', TAN, '#F4F4F4', 'spiky', casual('#1A1A1E', '#F4F4F4'), T.m_cloth('#2A2A30'), eyes='#3A6AFF', scar=True)
npc('master_fiend', MID, '#1A1010', 'short', armor_top('#5A1414', '#1A0A0A'), T.m_cloth('#1A0A0A'), scar=True, eyes='#7A1010')
npc('master_knights', LIGHT, '#E8C860', 'short', armor_top('#C0C8D8', '#3A6AC0'), T.m_metal('#9AA4B4', rivets=False), eyes='#2A5AA0')
npc('yoo_jinho', LIGHT, '#6A4A2A', 'short', casual('#3A6AC0', '#F4F4F4'), T.m_cloth('#2A3A5A'), short_sleeves=False)
npc('cha_haein', LIGHT, '#F0D070', 'bob', armor_top('#E8ECF2', '#C0303A'), T.m_cloth('#2A2A34'), eyes='#3A2A1A', mouth='#D06A70')
npc('healer', LIGHT, '#1A1A1E', 'bun', suit('#F4F4F4', shirt='#9AD0F0'), T.m_cloth('#E8E8EC'), mouth='#C0606A')
npc('merchant', TAN, '#3A2A1A', 'short', casual('#7A5A3A', '#C8A070'), T.m_cloth('#4A3A2A'), beard='#3A2A1A')
npc('blacksmith', TAN, '#2A2A2A', 'bald', casual('#5A3A22', '#2A1A10'), T.m_cloth('#2A2A30'), beard='#4A3A2A', short_sleeves=True)
npc('alchemist', LIGHT, '#5A3A7A', 'long', casual('#4A1E5E', '#D9A93A'), T.m_cloth('#2A1A3A'), glasses='#D9A93A')
npc('guide', MID, '#2A1A10', 'short', suit('#1E2E5A', tie='#5FC8FF', emblem='#5FC8FF'), T.m_cloth('#1E2E5A'))
npc('reporter', LIGHT, '#5A3A2A', 'bob', suit('#C8B48A', shirt='#F4F4F4'), T.m_cloth('#4A4A52'), mouth='#C0606A')
npc('jinah', LIGHT, '#14141A', 'ponytail', suit('#1A2A4A', tie='#C0303A'), T.m_cloth('#1A2A4A'), mouth='#D06A70')
cits = [('#2A6AC0', '#F4F4F4'), ('#C0303A', '#1A1A1A'), ('#3A8A4A', '#E8E8E8'), ('#E8E8EC', '#3A3A3A'), ('#F0B030', '#2A2A2A'), ('#7A4AA0', '#E0E0E0'),
        ('#1A1A1E', '#C0C0C0'), ('#C8784A', '#F4F4F4')]
hairs = ['#14141A', '#2A1A10', '#4A2E1E', '#14141A', '#6A4A2A', '#14141A', '#8A6A3A', '#2A1A10']
styles = ['short', 'long', 'short', 'bob', 'short', 'ponytail', 'spiky', 'long']
for i in range(8):
    npc(f'citizen_{i}', [LIGHT, MID, TAN][i % 3], hairs[i], styles[i], casual(*cits[i]), T.m_cloth(['#2A3A5A', '#1A1A1E', '#4A4A52', '#6A5A4A'][i % 4]),
        short_sleeves=i % 2 == 0)
gear = [('#3A4A5E', '#4AD8FF'), ('#4A3A2A', '#E8C547'), ('#2A2A30', '#FF4A4A'), ('#3A5A3A', '#9AFF7A'), ('#5A5A62', '#FFFFFF'), ('#2A1E3A', '#B08AFF')]
for i in range(6):
    npc(f'hunter_{i}', [MID, LIGHT, TAN][i % 3], hairs[i + 1], ['short', 'spiky', 'bob', 'short', 'slick', 'long'][i], armor_top(*gear[i]), T.m_cloth('#1E2026'))


def run(res_root):
    d = os.path.join(res_root, 'textures', 'entity', 'npc')
    os.makedirs(d, exist_ok=True)
    for name, spec in SKINS.items():
        T.save(paint(spec), os.path.join(d, name + '.png'))
    return list(SKINS)
