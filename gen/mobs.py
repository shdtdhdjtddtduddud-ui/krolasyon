"""Monster models. Coordinates: pixels, y up from the feet, the monster looks towards -z, +x is its left side."""
import tex as T
from model import Model

H = T.hexc


def eyes(color, pupil=None, y=0.42, spacing=0.22, size=0.11, slant=0.0, shape='round'):
    return lambda img, g: T.decal_eyes(img, color, pupil, y, spacing, size, slant, g, shape)


def mouth(y=0.75, width=0.55, teeth='#F2EEDC', open_=0.1, color='#2A0606'):
    return lambda img, g: T.decal_mouth(img, y, width, teeth, open_, color)


def band(y0, y1, color):
    return lambda img, g: T.decal_band(img, y0, y1, color)


def vstripe(x0, x1, color):
    return lambda img, g: T.decal_vstripe(img, x0, x1, color)


def runes(color, seed=1, density=0.6):
    return lambda img, g: T.decal_runes(img, color, seed, g, density)


def slit(color, y=0.45, h=0.12):
    def d(img, g):
        hh, w = img.shape[:2]
        c = H(color)
        T.rect(img, int(w * 0.12), int(hh * y), int(w * 0.88), int(hh * y) + max(1, int(hh * h)), H('#050505'))
        for cx in (0.3, 0.7):
            m = T.ellipse_mask(w, hh, w * cx, hh * y + hh * h / 2, w * 0.1, hh * h * 0.6, 0.4)
            T.draw_mask(img, m, c)
            T.draw_mask(g, m, c)
    return d


def glowspot(color, x=0.5, y=0.5, rx=0.3, ry=0.3):
    def d(img, g):
        hh, w = img.shape[:2]
        m = T.ellipse_mask(w, hh, w * x, hh * y, w * rx, hh * ry, 0.6)
        T.draw_mask(img, m, H(color))
        T.draw_mask(g, m, H(color))
    return d


def emblem_cross(color):
    def d(img, g):
        hh, w = img.shape[:2]
        T.rect(img, w * 0.42, hh * 0.15, w * 0.58, hh * 0.85, H(color))
        T.rect(img, w * 0.2, hh * 0.35, w * 0.8, hh * 0.48, H(color))
    return d


# --------------------------------------------------------------------------------------------- builders

def humanoid(m, p):
    """Generic biped. Returns key heights. p keys: leg, legw, hipw, torso, chestw, chestd, waistw, neck, head(w,h,d),
    arm, armw, mats{skin,legs,feet,hips,torso,chest,arm,forearm,hand,head}, gap"""
    mats = p['mats']
    leg, legw = p['leg'], p['legw']
    gap = p.get('gap', 0)
    hipw, torso, chestw, chestd = p['hipw'], p['torso'], p['chestw'], p['chestd']
    waistw = p.get('waistw', chestw - 2)
    hx = hipw / 2 - legw / 2 + gap / 2
    thigh = leg // 2
    # legs
    for side, s in (('l', 1), ('r', -1)):
        m.bone('leg_' + side, None, (s * hx, leg, 0))
        m.cbox('leg_' + side, s * hx, leg - thigh, 0, legw, thigh, legw, mats['legs'], mirror=s < 0)
        m.bone('shin_' + side, 'leg_' + side, (s * hx, leg - thigh, 0))
        m.cbox('shin_' + side, s * hx, 2, 0, legw - 0.0, leg - thigh - 2, legw, mats.get('shin', mats['legs']), inflate=-0.01, mirror=s < 0)
        m.cbox('shin_' + side, s * hx, 0, -1 if p.get('feetfwd', True) else 0, legw + 1, 2, legw + 2, mats['feet'], mirror=s < 0)
    # body
    m.bone('body', None, (0, leg, 0))
    hiph = max(2, torso // 4)
    m.cbox('body', 0, leg, 0, hipw, hiph, chestd - 1, mats['hips'])
    m.cbox('body', 0, leg + hiph, 0, waistw, torso // 3, chestd - 1, mats['torso'])
    chesty = leg + hiph + torso // 3
    chesth = torso - hiph - torso // 3
    m.cbox('body', 0, chesty, 0, chestw, chesth, chestd, mats['chest'], decals=p.get('chest_decals'))
    top = leg + torso
    # head
    hw, hh, hd = p['head']
    neck = p.get('neck', 1)
    m.bone('head', 'body', (0, top + neck, p.get('headz', 0)))
    if neck > 0:
        m.cbox('head', 0, top - 1, p.get('headz', 0), max(2, hw // 2), neck + 1, max(2, hd // 2), mats['skin'])
    m.cbox('head', 0, top + neck, p.get('headz', 0) - 0.5, hw, hh, hd, mats['head'], decals=p.get('face'))
    if p.get('jaw'):
        jw, jh, jd = p['jaw']
        m.bone('jaw', 'head', (0, top + neck + 1, p.get('headz', 0) + hd / 2 - 2))
        m.cbox('jaw', 0, top + neck - jh + 1, p.get('headz', 0) - 0.5 - (jd - hd) / 2 - 0.5, jw, jh, jd, mats.get('jaw', mats['head']), decals={'front': [mouth(0.3, 0.7, open_=0.2)]})
    # arms
    arm, armw = p['arm'], p['armw']
    up = arm // 2
    sy = top - 1
    ax = chestw / 2 + armw / 2
    for side, s in (('l', 1), ('r', -1)):
        m.bone('arm_' + side, 'body', (s * ax, sy, 0))
        m.cbox('arm_' + side, s * ax, sy - up, 0, armw, up + 1, armw, mats['arm'], mirror=s < 0)
        m.bone('forearm_' + side, 'arm_' + side, (s * ax, sy - up, 0))
        m.cbox('forearm_' + side, s * ax, sy - arm + 2, 0, armw, arm - up - 2, armw, mats['forearm'], inflate=-0.01, mirror=s < 0)
        m.cbox('forearm_' + side, s * ax, sy - arm, 0, armw, 2, armw, mats['hand'], inflate=0.1, mirror=s < 0)
    return {'top': top, 'neck': top + neck, 'headtop': top + neck + hh, 'shoulder': sy, 'ax': ax, 'hand': sy - arm,
            'chestw': chestw, 'chestd': chestd, 'leg': leg, 'hipw': hipw, 'headw': hw, 'headd': hd, 'headz': p.get('headz', 0),
            'chesty': chesty}


def weapon_sword(m, k, side='r', length=14, width=2, blade='#C9D2DC', hilt='#3A2A1E', guard='#B8962E', glow=None, curve=False):
    s = 1 if side == 'l' else -1
    b = m.bone('weapon_' + side, 'forearm_' + side, (s * k['ax'], k['hand'] + 1, 0))
    x = s * k['ax']
    y = k['hand'] + 0.5
    m.cbox(b, x, y - 0.5, 0, 1.6, 1.6, 4, T.m_leather(hilt), inflate=0.0)
    m.cbox(b, x, y - 1, -2.5, 1, 3, 1, T.m_metal(guard, rivets=False))
    m.cbox(b, x, y - 0.5, -2.5 - length, 1, width, length, T.m_metal(blade, rivets=False), glow=glow is not None,
           decals={'right': [runes(glow, 3)]} if glow else None)
    m.cbox(b, x, y - 0.5, -3.5 - length, 1, max(1, width - 1), 1, T.m_metal(blade, rivets=False))
    return b


def weapon_axe(m, k, side='r', handle=16, head='#7A7F88'):
    s = 1 if side == 'l' else -1
    b = m.bone('weapon_' + side, 'forearm_' + side, (s * k['ax'], k['hand'] + 1, 0))
    x, y = s * k['ax'], k['hand'] + 0.5
    m.cbox(b, x, y - 0.5, -handle / 2 + 3, 1.5, 1.5, handle, T.m_wood('#5A3B22'))
    m.cbox(b, x, y - 0.5, -handle + 2, 1, 7, 5, T.m_metal(head))
    m.cbox(b, x, y + 5, -handle + 3, 1, 2, 2, T.m_metal('#A0A6AE'))
    return b


def weapon_staff(m, k, side='r', length=26, orb='#8A5CFF', wood='#4A2E1A'):
    s = 1 if side == 'l' else -1
    b = m.bone('weapon_' + side, 'forearm_' + side, (s * k['ax'], k['hand'] + 1, 0))
    x, y = s * k['ax'], k['hand'] + 0.5
    m.cbox(b, x, y - 0.5, -length / 2 + 4, 1.5, 1.5, length, T.m_wood(wood))
    m.cbox(b, x, y - 1.5, -length + 2, 3, 3, 3, T.m_glow(orb), glow=True)
    m.cbox(b, x, y - 1.5, -length + 2, 3, 3, 3, T.m_glow(orb), inflate=0.6, glow=True)
    return b


def weapon_dagger(m, k, side='r', blade='#9FB4C8', glow=None):
    s = 1 if side == 'l' else -1
    b = m.bone('weapon_' + side, 'forearm_' + side, (s * k['ax'], k['hand'] + 1, 0))
    x, y = s * k['ax'], k['hand'] + 0.5
    m.cbox(b, x, y - 0.5, 0, 1.2, 1.2, 3, T.m_leather('#2A1E18'))
    m.cbox(b, x, y - 0.5, -6, 1, 2, 6, T.m_metal(blade, rivets=False), glow=glow is not None)
    return b


def cape(m, k, mat, length=None, width=None):
    b = m.bone('cape', 'body', (0, k['top'], k['chestd'] / 2 + 0.5))
    L = length or int(k['top'] * 0.8)
    W = width or k['chestw']
    m.cbox(b, 0, k['top'] - L, k['chestd'] / 2 + 0.5, W, L, 1, mat)
    return b


def quadruped(m, p):
    """p: legh, legw, bodyl, bodyw, bodyh, head(w,h,d), snout(w,h,d), neck, mats, tail"""
    mats = p['mats']
    lh, lw = p['legh'], p['legw']
    bl, bw, bh = p['bodyl'], p['bodyw'], p['bodyh']
    m.bone('body', None, (0, lh + bh / 2, 0))
    m.cbox('body', 0, lh, 0, bw, bh, bl, mats['body'], decals=p.get('body_decals'))
    m.cbox('body', 0, lh + 1, -bl / 2 + 3, bw + 2, bh + 1, 6, mats.get('chest', mats['body']), decals=p.get('chest_decals'))
    if p.get('mane'):
        m.cbox('body', 0, lh + bh - 1, -bl / 2 + 4, bw + 1, 3, 9, mats['mane'])
    fz, bz = -bl / 2 + 3, bl / 2 - 3
    lx = bw / 2 - lw / 2
    for name, z in (('fleg', fz), ('bleg', bz)):
        for side, s in (('l', 1), ('r', -1)):
            bn = m.bone(f'{name}_{side}', None, (s * lx, lh, z))
            m.cbox(bn, s * lx, 2, z, lw, lh - 2, lw, mats['leg'], mirror=s < 0)
            m.cbox(bn, s * lx, 0, z - 0.5, lw + 1, 2, lw + 1.5, mats['paw'], mirror=s < 0)
            if name == 'fleg':
                m.cbox(bn, s * lx, 0, z - lw / 2 - 1.5, lw, 1, 1, T.m_bone('#E8E2CF'), mirror=s < 0)
    heads = p.get('heads', [0])
    hw, hh, hd = p['head']
    sw, sh, sd = p['snout']
    neck = p.get('neck', 3)
    hy = lh + bh - 2
    for i, offx in enumerate(heads):
        nm = 'head' if i == 0 else f'head{i + 1}'
        hz = -bl / 2 - neck
        m.bone(nm, 'body', (offx, hy, -bl / 2 + 1), rot=(0, (25 if offx > 0 else -25) if offx else 0, 0))
        m.cbox(nm, offx, hy - 2, -bl / 2 - neck / 2 + 1, hw - 2, hh - 1, neck + 2, mats['body'])
        m.cbox(nm, offx, hy - 1, hz - hd / 2, hw, hh, hd, mats['head'], decals=p.get('face'))
        m.cbox(nm, offx, hy - 1, hz - hd - sd / 2 + 0.5, sw, sh, sd, mats['snout'], decals={'front': [lambda img, g: T.rect(img, img.shape[1] * 0.3, 0, img.shape[1] * 0.7, img.shape[0] * 0.3, H('#111111'))]})
        jaw = 'jaw' if i == 0 else f'jaw{i + 1}'
        m.bone(jaw, nm, (offx, hy - 1, hz - hd / 2))
        m.cbox(jaw, offx, hy - 3, hz - hd - sd / 2 + 1, sw - 0.5, 2, sd, mats['snout'], decals={'top': [mouth(0.5, 0.9, open_=0.25)]})
        if p.get('fangs'):
            for s in (1, -1):
                m.cbox(nm, offx + s * (sw / 2 - 1), hy - 2.5, hz - hd - sd + 1.2, 1, 2, 1, T.m_metal(p['fangs'], rivets=False, bevel=False))
        for s in (1, -1):
            m.cbox(nm, offx + s * (hw / 2 - 1), hy - 1 + hh, hz - hd / 2 + 1, 2, p.get('ear', 3), 1, mats['ear'], mirror=s < 0)
    if p.get('tail', 0):
        prev, ty, tz = 'body', lh + bh - 2, bl / 2
        for i in range(p['tail']):
            nm = f'tail{i}'
            m.bone(nm, prev, (0, ty, tz), rot=(-25 if i == 0 else 8, 0, 0))
            m.cbox(nm, 0, ty - 1.5, tz + 2.5, p.get('tailw', 3), 3, 5, mats.get('tail', mats['body']))
            prev, tz = nm, tz + 5
    return {'lh': lh, 'bh': bh, 'bl': bl, 'bw': bw}


# --------------------------------------------------------------------------------------------- monsters

MODELS = {}
RIGS = {}
SCALES = {}


def reg(name, rig, freq=0.66, amp=1.0, lean=0.0, two=False, scale=1.0, shadow=0.5, texw=128, res=4):
    def deco(fn):
        m = Model(name, texw, res)
        fn(m)
        MODELS[name] = m
        RIGS[name] = (rig, freq, amp, lean, two, scale, shadow)
        return fn
    return deco


@reg('goblin', 'BIPED', 0.9, 1.0, 0.15, shadow=0.4)
def goblin(m):
    skin = T.m_skin('#6C9A3E', spots='#4E7A2C')
    k = humanoid(m, dict(leg=6, legw=2, hipw=5, torso=7, chestw=6, chestd=4, waistw=5, neck=0, head=(7, 6, 6), arm=8, armw=2, headz=-0.5,
                         mats=dict(skin=skin, legs=skin, feet=T.m_skin('#4E7A2C'), hips=T.m_leather('#5B3A22'), torso=skin,
                                   chest=skin, arm=skin, forearm=skin, hand=T.m_skin('#5A8434'), head=skin),
                         face={'front': [eyes('#FFE14A', '#2A1A00', 0.42, 0.24, 0.12, 0.3), mouth(0.78, 0.6, open_=0.09)]}))
    for s in (1, -1):  # big pointed ears
        m.box('head', (3.5 if s > 0 else -6.5), k['neck'] + 3, -1, 3, 2, 1, skin, mirror=s < 0)
    m.cbox('head', 0, k['neck'] + 1, -3.9, 1, 2, 1, T.m_skin('#5A8434'))  # nose
    m.cbox('body', 0, k['top'] - 1, -2.3, 5, 1, 1, T.m_bone(), decals=None)  # bone necklace
    weapon_dagger(m, k, 'r', '#8A6E52')


@reg('hobgoblin', 'BIPED', 0.7, 1.0, 0.1, shadow=0.6)
def hobgoblin(m):
    skin = T.m_skin('#4E6E2E', spots='#3B5522')
    leather = T.m_leather('#4A3020', '#8C6A44')
    k = humanoid(m, dict(leg=12, legw=4, hipw=9, torso=12, chestw=11, chestd=6, waistw=9, neck=1, head=(8, 8, 7), arm=13, armw=4, headz=-1,
                         mats=dict(skin=skin, legs=T.m_cloth('#3C2C1E'), feet=T.m_leather('#2C1E14'), hips=leather, torso=leather,
                                   chest=T.m_leather('#5A3A24', '#A07A4A'), arm=skin, forearm=skin, hand=skin, head=skin),
                         jaw=(7, 2, 6), face={'front': [eyes('#FF4A2A', None, 0.45, 0.22, 0.11, 0.4)]}))
    for s in (1, -1):
        m.cbox('head', s * 4.5, k['neck'] + 5, 0, 1, 2, 3, skin)
        m.cbox('arm_' + ('l' if s > 0 else 'r'), s * k['ax'], k['shoulder'] - 2, 0, 5, 3, 5, T.m_metal('#5D5A55'), inflate=0.2)
    # spiked club
    b = m.bone('weapon_r', 'forearm_r', (-k['ax'], k['hand'] + 1, 0))
    m.cbox(b, -k['ax'], k['hand'], -6, 2, 2, 12, T.m_wood('#5C3D20'))
    m.cbox(b, -k['ax'], k['hand'] - 1, -15, 4, 4, 6, T.m_wood('#4A2F18'))
    for dz in (-14, -11):
        m.cbox(b, -k['ax'], k['hand'] + 3, dz, 1, 1, 1, T.m_metal('#B0B4BA', rivets=False))
        m.cbox(b, -k['ax'] + 2.5, k['hand'] + 1, dz, 1, 1, 1, T.m_metal('#B0B4BA', rivets=False))


@reg('steel_fanged_lycan', 'QUAD', 0.9, 1.0, shadow=0.7)
def lycan(m):
    fur = T.m_fur('#707784', '#B8C0CC', '#3A3F4A')
    quadruped(m, dict(legh=10, legw=3, bodyl=20, bodyw=8, bodyh=8, head=(7, 6, 6), snout=(4, 3, 5), neck=3, tail=3, mane=True, ear=4,
                      fangs='#D8E0E8',
                      mats=dict(body=fur, chest=T.m_fur('#9AA3B0', '#D6DCE4', '#5A606C'), leg=fur, paw=T.m_fur('#3E434D'), head=fur,
                                snout=T.m_fur('#8A93A0', '#C9D0DA'), ear=T.m_fur('#4C525E'), mane=T.m_fur('#4A505C', '#8E96A4', '#24282F', 4)),
                      face={'front': [eyes('#FF3030', '#300000', 0.4, 0.26, 0.11, 0.5, 'slit')]}))


@reg('giant_centipede', 'CENTIPEDE', 0.8, 1.0, shadow=0.9)
def centipede(m):
    shell = T.m_chitin('#6A2416', '#E0805A', 2)
    legm = T.m_chitin('#C8902C', '#FFE08A')
    n = 9
    prev = None
    for i in range(n):
        z = -16 + i * 4.5
        w = 8 - abs(i - 2) * 0.35 if i < 6 else 8 - (i - 3) * 0.8
        w = max(4, int(w))
        nm = f'seg{i}'
        m.bone(nm, prev, (0, 4, z - 2))
        m.cbox(nm, 0, 1, z, w, 4, 5, shell)
        m.bone(f'legl{i}', nm, (w / 2, 3, z))
        m.box(f'legl{i}', w / 2, 0, z - 0.5, 4, 1, 1, legm)
        m.box(f'legl{i}', w / 2 + 3, -1, z - 0.5, 1, 3, 1, legm)
        m.bone(f'legr{i}', nm, (-w / 2, 3, z))
        m.box(f'legr{i}', -w / 2 - 4, 0, z - 0.5, 4, 1, 1, legm, mirror=True)
        m.box(f'legr{i}', -w / 2 - 4, -1, z - 0.5, 1, 3, 1, legm, mirror=True)
        prev = nm
    m.bone('head', 'seg0', (0, 4, -18))
    m.cbox('head', 0, 1, -21, 7, 4, 5, T.m_chitin('#5A1A10', '#E09070'), decals={'front': [eyes('#FFD54A', None, 0.35, 0.3, 0.1)]})
    for s, nm in ((1, 'mandible_l'), (-1, 'mandible_r')):
        m.bone(nm, 'head', (s * 2.5, 2, -23))
        m.box(nm, s * 2.5 - (0 if s > 0 else 1), 1, -27, 1, 1, 4, T.m_chitin('#2A0A06', '#FF8A5A'))
    for s, nm in ((1, 'antenna_l'), (-1, 'antenna_r')):
        m.bone(nm, 'head', (s * 2, 5, -21), rot=(-30, s * 20, 0))
        m.box(nm, s * 2 - 0.5, 5, -29, 1, 1, 8, legm)


@reg('kasaka', 'SERPENT', 0.5, 1.0, shadow=1.4, texw=256, res=2)
def kasaka(m):
    scales = T.m_scales('#2C6AB8', '#7FC0FF', '#14386E', 5)
    belly = T.m_scales('#B8DCF4', '#FFFFFF', '#6A9AC0', 2)
    # coiled body on the ground
    n = 8
    prev = None
    for i in range(n):
        nm = f'seg{i}'
        z = 10 - i * 5
        w = 12 - i * 0.6
        m.bone(nm, prev, (0, 6, z + 2.5) if i else (0, 6, 14))
        m.cbox(nm, 0, 0, z, w, 9 - i * 0.4, 6, scales, decals={'bottom': []})
        m.cbox(nm, 0, 0, z, w - 4, 1, 6, belly, inflate=0.1)
        prev = nm
    # rearing neck
    m.bone('neck', 'seg7', (0, 8, -26), rot=(0, 0, 0))
    m.cbox('neck', 0, 6, -28, 9, 12, 7, scales, decals={'front': [vstripe(0.25, 0.75, '#B8DCF4')]})
    m.cbox('neck', 0, 16, -28, 8, 10, 6, scales, decals={'front': [vstripe(0.22, 0.78, '#B8DCF4')]})
    for s in (1, -1):  # hood fins
        m.box('neck', s * 4.5 - (0 if s > 0 else 4), 14, -27, 4, 12, 1, T.m_scales('#1C4E92', '#5FA8FF', '#081C3E', 3), mirror=s < 0)
    m.bone('head', 'neck', (0, 27, -26))
    m.cbox('head', 0, 26, -33, 10, 6, 12, scales,
           decals={'front': [eyes('#FFE34A', '#1A0E00', 0.35, 0.3, 0.11, 0.6, 'slit')], 'top': [runes('#8FD0FF', 5, 0.3)]})
    m.bone('jaw', 'head', (0, 27, -28))
    m.cbox('jaw', 0, 23, -33, 9, 3, 11, belly, decals={'top': [mouth(0.5, 0.9, open_=0.3, teeth=None, color='#5A1030')]})
    for s in (1, -1):
        m.cbox('head', s * 2.5, 23.5, -38, 1, 3, 1, T.m_glow('#B04AFF', '#E0B0FF'), glow=True)  # venom fangs
    for s in (1, -1):
        m.box('head', s * 3 - (0 if s > 0 else 2), 32, -30, 2, 2, 5, T.m_scales('#1C4E92'))  # brow horns


@reg('stone_statue', 'BIPED', 0.5, 0.7, 0.05, two=True, shadow=0.6)
def stone_statue(m):
    st = T.m_stone('#C9B48A', moss='#7A8A4A')
    carved = T.m_stone('#B8A276', carved=True)
    k = humanoid(m, dict(leg=14, legw=4, hipw=9, torso=14, chestw=12, chestd=6, waistw=9, neck=1, head=(8, 9, 8), arm=14, armw=4,
                         mats=dict(skin=st, legs=st, feet=carved, hips=carved, torso=st, chest=carved, arm=st, forearm=st, hand=st, head=st),
                         face={'front': [eyes('#FF2A2A', None, 0.45, 0.2, 0.1), band(0.1, 0.18, '#8C7A56')]}))
    m.cbox('head', 0, k['headtop'] - 1, 0, 9, 3, 9, carved)  # helmet rim
    m.cbox('head', 0, k['headtop'] + 1, 0, 2, 4, 6, carved)  # crest
    for s in (1, -1):
        m.cbox('arm_' + ('l' if s > 0 else 'r'), s * k['ax'], k['shoulder'] - 3, 0, 6, 4, 6, carved, inflate=0.2)
    # stone mace
    b = m.bone('weapon_r', 'forearm_r', (-k['ax'], k['hand'] + 1, 0))
    m.cbox(b, -k['ax'], k['hand'], -7, 2, 2, 14, carved)
    m.cbox(b, -k['ax'], k['hand'] - 2, -18, 5, 5, 6, st)


@reg('statue_of_god', 'BIPED', 0.2, 0.2, 0.0, two=True, scale=2.0, shadow=2.0, texw=256, res=2)
def statue_of_god(m):
    marble = T.m_stone('#DCCDA4', cracks=True)
    gold = T.m_metal('#D9A93A', rivets=False)
    robe = T.m_stone('#CDB98C', carved=True)
    k = humanoid(m, dict(leg=18, legw=7, hipw=16, torso=16, chestw=17, chestd=9, waistw=14, neck=2, head=(11, 12, 11), arm=18, armw=6,
                         mats=dict(skin=marble, legs=robe, feet=marble, hips=robe, torso=marble, chest=marble, arm=marble, forearm=marble,
                                   hand=marble, head=marble),
                         face={'front': [eyes('#FFD24A', '#FFFFFF', 0.4, 0.22, 0.11, -0.2),
                                         mouth(0.74, 0.62, teeth='#F6F0DC', open_=0.07, color='#3A2A10')]}))
    # seated pose: thighs forward, shins down
    m.bones['leg_l'].rot = (-90, 0, 0)
    m.bones['leg_r'].rot = (-90, 0, 0)
    m.bones['shin_l'].rot = (90, 0, 0)
    m.bones['shin_r'].rot = (90, 0, 0)
    m.bones['arm_l'].rot = (-40, 0, -5)
    m.bones['arm_r'].rot = (-40, 0, 5)
    m.cbox('head', 0, k['headtop'], 0, 12, 2, 12, gold)  # crown band
    for i, x in enumerate((-4, 0, 4)):
        m.cbox('head', x, k['headtop'] + 2, -4, 2, 4 if i != 1 else 6, 2, gold, glow=False)
    m.cbox('body', 0, k['top'] - 3, 0, 18, 2, 10, gold)  # collar
    cape(m, k, T.m_cloth('#B89A5A', trim='#D9A93A'), length=30, width=16)


@reg('castle_knight', 'BIPED', 0.62, 1.0, 0.05, shadow=0.55)
def castle_knight(m):
    plate = T.m_metal('#454B57', trim='#8A93A3')
    dark = T.m_metal('#2C3038')
    cloth = T.m_cloth('#7A1A20', trim='#C9A040')
    k = humanoid(m, dict(leg=13, legw=4, hipw=9, torso=12, chestw=10, chestd=6, waistw=8, neck=1, head=(8, 8, 8), arm=12, armw=4,
                         mats=dict(skin=dark, legs=plate, feet=dark, hips=cloth, torso=dark, chest=plate, arm=plate, forearm=dark, hand=dark,
                                   head=plate),
                         chest_decals={'front': [emblem_cross('#C9A040')]},
                         face={'front': [slit('#FF3A2A', 0.4, 0.12)]}))
    for s in (1, -1):
        m.cbox('arm_' + ('l' if s > 0 else 'r'), s * k['ax'] + s * 0.5, k['shoulder'] - 3, 0, 5, 4, 6, plate, inflate=0.3)
    m.cbox('body', 0, k['leg'] - 4, -3.1, 6, 7, 1, cloth)  # tabard front
    m.cbox('head', 0, k['headtop'], 0, 1, 2, 7, T.m_metal('#8A93A3'))  # crest
    weapon_sword(m, k, 'r', 13, 2)
    # shield on left arm
    m.cbox('forearm_l', k['ax'] + 2.5, k['hand'] + 1, 0, 1, 10, 8, T.m_metal('#3A3F4A', trim='#C9A040'), decals={'left': [emblem_cross('#7A1A20')]})


@reg('igris', 'BIPED', 0.62, 1.0, 0.08, two=True, shadow=0.6)
def igris(m):
    red = T.m_metal('#8C1622', trim='#D4A841')
    black = T.m_metal('#1E1A1E', trim='#8C1622')
    k = humanoid(m, dict(leg=16, legw=4, hipw=9, torso=14, chestw=11, chestd=6, waistw=8, neck=1, head=(8, 9, 8), arm=14, armw=4,
                         mats=dict(skin=black, legs=red, feet=black, hips=T.m_cloth('#5E0E14', trim='#D4A841'), torso=black, chest=red,
                                   arm=red, forearm=black, hand=black, head=red),
                         chest_decals={'front': [band(0.15, 0.22, '#D4A841'), band(0.85, 0.92, '#1E1A1E')]},
                         face={'front': [slit('#FF2020', 0.45, 0.1), vstripe(0.46, 0.54, '#D4A841')]}))
    # pauldrons
    for s in (1, -1):
        sd = 'l' if s > 0 else 'r'
        m.cbox('arm_' + sd, s * k['ax'] + s * 0.8, k['shoulder'] - 3, 0, 6, 5, 7, red, inflate=0.3)
        m.cbox('arm_' + sd, s * k['ax'] + s * 1.5, k['shoulder'] + 1.5, 0, 4, 1, 6, black)
    # helmet: plume of red horsehair falling backwards
    m.cbox('head', 0, k['headtop'], 0, 2, 2, 8, T.m_metal('#D4A841', rivets=False))
    hair = T.m_hair('#B0101C', '#FF5A50')
    m.cbox('head', 0, k['headtop'] + 1, 1, 3, 2, 8, hair)
    m.bone('plume', 'head', (0, k['headtop'] + 2, 4.5), rot=(20, 0, 0))
    m.cbox('plume', 0, k['headtop'] - 12, 4.5, 4, 14, 2, hair)
    cape(m, k, T.m_cloth('#7A0E18', trim='#D4A841'), length=24, width=12)
    weapon_sword(m, k, 'r', 20, 3, blade='#B8C0CC', hilt='#1E1A1E', guard='#D4A841', glow='#FF3030')


@reg('ice_elf', 'BIPED', 0.7, 1.0, 0.03, shadow=0.45)
def ice_elf(m):
    skin = T.m_skin('#D6EAF4', var=0.08)
    robe = T.m_cloth('#2E5E9A', trim='#CFEFFF')
    k = humanoid(m, dict(leg=13, legw=3, hipw=7, torso=11, chestw=8, chestd=5, waistw=6, neck=1, head=(7, 8, 7), arm=11, armw=3,
                         mats=dict(skin=skin, legs=T.m_cloth('#1E3E6A'), feet=T.m_leather('#E0F0FA'), hips=robe, torso=robe, chest=robe,
                                   arm=robe, forearm=skin, hand=skin, head=skin),
                         face={'front': [eyes('#7FE6FF', '#E0FFFF', 0.48, 0.22, 0.1, 0.3), mouth(0.8, 0.3, teeth=None, open_=0.04)]}))
    hair = T.m_hair('#EEF6FF', '#FFFFFF')
    m.cbox('head', 0, k['headtop'] - 1, 1, 8, 3, 8, hair)
    m.cbox('head', 0, k['neck'], 3.5, 8, 7, 2, hair)
    for s in (1, -1):  # long pointed ears
        m.box('head', (3.5 if s > 0 else -7.5), k['neck'] + 4, 0, 4, 1, 2, skin, mirror=s < 0)
    m.cbox('body', 0, k['leg'] - 6, 0, 8, 7, 6, robe)  # robe skirt
    weapon_staff(m, k, 'r', 22, '#9FF0FF', '#CFEFFF')


@reg('ice_bear', 'QUAD', 0.6, 0.9, shadow=1.1)
def ice_bear(m):
    fur = T.m_fur('#E4EEF4', '#FFFFFF', '#9AB4C4')
    quadruped(m, dict(legh=12, legw=6, bodyl=26, bodyw=16, bodyh=15, head=(10, 9, 8), snout=(6, 5, 5), neck=3, tail=1, tailw=4, ear=3,
                      mats=dict(body=fur, chest=T.m_fur('#D0E2EE', '#FFFFFF', '#8AA8BC'), leg=fur, paw=T.m_fur('#B8CCDA'), head=fur,
                                snout=T.m_fur('#CADAE6', '#FFFFFF', '#7E9AAE'), ear=T.m_fur('#AFC6D6')),
                      body_decals={'top': [runes('#9FE8FF', 2, 0.3)]},
                      face={'front': [eyes('#4AD8FF', '#002233', 0.42, 0.25, 0.09)]}))
    for z in (-6, 0, 6):  # ice crystals on the back
        m.cbox('body', (z % 4) - 1, 27, z, 2, 3 + abs(z) // 3, 2, T.m_crystal('#8FE4FF'), glow=True)


@reg('baruka', 'BIPED', 0.75, 1.0, 0.1, shadow=0.5)
def baruka(m):
    skin = T.m_skin('#C8E2F0', var=0.08)
    cloak = T.m_cloth('#1A2E5C', trim='#9FE6FF')
    k = humanoid(m, dict(leg=14, legw=3, hipw=7, torso=12, chestw=9, chestd=5, waistw=7, neck=1, head=(7, 8, 7), arm=12, armw=3,
                         mats=dict(skin=skin, legs=T.m_leather('#22324E'), feet=T.m_leather('#141E30'), hips=cloak, torso=T.m_leather('#2A3C5E'),
                                   chest=T.m_metal('#9FC6E0', rivets=False, trim='#E6F6FF'), arm=cloak, forearm=skin, hand=skin, head=skin),
                         face={'front': [eyes('#4AE8FF', '#E0FFFF', 0.48, 0.22, 0.1, 0.5), mouth(0.8, 0.35, teeth=None, open_=0.04)]}))
    hair = T.m_hair('#F4FAFF', '#FFFFFF')
    m.cbox('head', 0, k['headtop'] - 1, 1, 8, 3, 8, hair)
    m.cbox('head', 0, k['neck'] - 6, 4, 8, 13, 2, hair)
    m.cbox('head', 0, k['headtop'] - 2, -3.6, 8, 1, 1, T.m_metal('#BFEFFF', rivets=False))  # circlet
    for s in (1, -1):
        m.box('head', (3.5 if s > 0 else -7.5), k['neck'] + 4, 0, 4, 1, 2, skin, mirror=s < 0)
    cape(m, k, T.m_cloth('#0E1A3A', trim='#9FE6FF'), length=22, width=10)
    weapon_dagger(m, k, 'r', '#BFF2FF', glow=True)
    weapon_dagger(m, k, 'l', '#BFF2FF', glow=True)


@reg('high_orc', 'BIPED', 0.6, 1.0, 0.1, two=True, shadow=0.75)
def high_orc(m):
    skin = T.m_skin('#6E8048', spots='#56663A')
    iron = T.m_metal('#5A5E64', trim='#9A7A44')
    fur = T.m_fur('#5A4632', '#9A7C5A', '#2E2216')
    k = humanoid(m, dict(leg=16, legw=5, hipw=11, torso=16, chestw=14, chestd=8, waistw=11, neck=1, head=(9, 9, 8), arm=16, armw=5, headz=-1,
                         mats=dict(skin=skin, legs=T.m_leather('#3E3024'), feet=T.m_leather('#2A1E14'), hips=T.m_leather('#4A3828', '#A07A4A'),
                                   torso=skin, chest=iron, arm=skin, forearm=skin, hand=skin, head=skin),
                         jaw=(8, 3, 7), face={'front': [eyes('#FFB020', '#2A1000', 0.4, 0.23, 0.1, 0.5)]}))
    for s in (1, -1):
        m.cbox('jaw', s * 3, k['neck'] + 1, -5.2, 1, 3, 1, T.m_bone('#F0E8D0'))  # tusks
        sd = 'l' if s > 0 else 'r'
        m.cbox('arm_' + sd, s * k['ax'] + s * 0.5, k['shoulder'] - 2, 0, 7, 4, 7, fur, inflate=0.3)
    m.cbox('head', 0, k['headtop'], 2, 3, 4, 3, T.m_hair('#1A1410'))  # topknot
    weapon_axe(m, k, 'r', 20)


@reg('kargalgan', 'BIPED', 0.55, 0.9, 0.05, shadow=0.75)
def kargalgan(m):
    skin = T.m_skin('#5E7044', spots='#4A5A34')
    robe = T.m_cloth('#4A1E5E', trim='#D9A93A')
    k = humanoid(m, dict(leg=16, legw=5, hipw=11, torso=17, chestw=13, chestd=8, waistw=11, neck=1, head=(9, 9, 8), arm=16, armw=5, headz=-1,
                         mats=dict(skin=skin, legs=robe, feet=T.m_leather('#2A1E14'), hips=robe, torso=robe, chest=robe, arm=robe,
                                   forearm=skin, hand=skin, head=skin),
                         jaw=(8, 3, 7),
                         chest_decals={'front': [runes('#D97AFF', 4, 0.6)]},
                         face={'front': [eyes('#D27AFF', '#FFFFFF', 0.4, 0.23, 0.1, 0.3)]}))
    m.cbox('body', 0, 2, 0, 13, k['leg'] - 2, 9, robe)  # long robe
    for s in (1, -1):
        m.cbox('jaw', s * 3, k['neck'] + 1, -5.2, 1, 3, 1, T.m_bone('#F0E8D0'))
    # feather headdress
    for i, x in enumerate((-4, -2, 0, 2, 4)):
        m.cbox('head', x, k['headtop'], 1, 1, 5 + (2 - abs(i - 2)) * 2, 2, T.m_cloth(['#C0302A', '#E0B040', '#2A6AC0', '#E0B040', '#C0302A'][i]))
    m.cbox('body', 0, k['top'] - 3, -4.3, 8, 2, 1, T.m_bone(), decals={'front': [glowspot('#FF4AFF', 0.5, 0.5, 0.12, 0.4)]})
    weapon_staff(m, k, 'r', 30, '#C04AFF', '#3A2414')


@reg('demon', 'BIPED', 0.7, 1.0, 0.12, shadow=0.55)
def demon(m):
    skin = T.m_skin('#9C2A20', spots='#6A140E')
    plate = T.m_metal('#2A1A1A', trim='#C0501A')
    k = humanoid(m, dict(leg=15, legw=4, hipw=9, torso=13, chestw=11, chestd=6, waistw=8, neck=1, head=(8, 8, 7), arm=14, armw=4,
                         mats=dict(skin=skin, legs=plate, feet=T.m_bone('#2A2020'), hips=T.m_cloth('#1A0E0E', trim='#C0501A'), torso=skin,
                                   chest=plate, arm=skin, forearm=skin, hand=T.m_bone('#1A1414'), head=skin),
                         jaw=(7, 2, 6), face={'front': [eyes('#FFD21A', '#FF4A00', 0.42, 0.24, 0.12, 0.6)]}))
    horn = T.m_bone('#1E1818')
    for s in (1, -1):
        m.bone('horn_' + ('l' if s > 0 else 'r'), 'head', (s * 3, k['headtop'] - 1, 0), rot=(-25, 0, s * 35))
        m.cbox('horn_' + ('l' if s > 0 else 'r'), s * 3, k['headtop'] - 1, 0, 2, 6, 2, horn)
        m.cbox('horn_' + ('l' if s > 0 else 'r'), s * 3, k['headtop'] + 4, 0.5, 1, 3, 1, horn)
    prev, tz = 'body', 3
    for i in range(3):
        nm = f'tail{i}'
        m.bone(nm, prev, (0, k['leg'] + 1, tz), rot=(-40 if i == 0 else 15, 0, 0))
        m.cbox(nm, 0, k['leg'], tz + 2.5, 2, 2, 5, skin)
        prev, tz = nm, tz + 5
    m.cbox('tail2', 0, k['leg'] - 0.5, tz - 1, 3, 3, 2, horn)


@reg('cerberus', 'QUAD', 0.6, 1.0, scale=1.6, shadow=1.6, texw=256, res=2)
def cerberus(m):
    fur = T.m_fur('#221C22', '#5A3A3A', '#0A080A')
    quadruped(m, dict(legh=11, legw=5, bodyl=24, bodyw=12, bodyh=11, head=(8, 7, 7), snout=(5, 4, 5), neck=4, tail=3, tailw=3, ear=4,
                      heads=[0, 8.5, -8.5], fangs='#F0E6D0', mane=True,
                      mats=dict(body=fur, chest=T.m_fur('#2A2026', '#7A3A2A', '#100A0E'), leg=fur, paw=T.m_bone('#1A1414'), head=fur,
                                snout=T.m_fur('#1A1418', '#4A2A2A'), ear=T.m_fur('#120E12'),
                                mane=T.m_fur('#8A1A0A', '#FF7A2A', '#3A0A04', 4), tail=T.m_fur('#3A0E08', '#FF6A1A')),
                      body_decals={'top': [runes('#FF5A1A', 7, 0.4)]},
                      face={'front': [eyes('#FF3A0A', '#FFE0A0', 0.4, 0.25, 0.12, 0.6)]}))
    # spiked collar
    for x in (-6, 0, 6):
        m.cbox('body', x * 0.9, 18, -12, 4, 2, 3, T.m_metal('#3A3A40', trim='#9A2A1A'))


@reg('baran', 'BIPED', 0.55, 0.9, 0.05, two=True, scale=1.5, shadow=1.2, texw=256, res=2)
def baran(m):
    armor = T.m_metal('#1E1A26', trim='#7A3AFF')
    red = T.m_metal('#5A0E18', trim='#C9A040')
    skin = T.m_skin('#5A4A5E')
    k = humanoid(m, dict(leg=14, legw=4, hipw=10, torso=13, chestw=12, chestd=7, waistw=9, neck=1, head=(8, 8, 8), arm=13, armw=4,
                         mats=dict(skin=skin, legs=armor, feet=armor, hips=T.m_cloth('#2A0A12', trim='#C9A040'), torso=armor, chest=red,
                                   arm=armor, forearm=armor, hand=armor, head=armor),
                         chest_decals={'front': [runes('#9A6AFF', 11, 0.8)]},
                         face={'front': [eyes('#C08AFF', '#FFFFFF', 0.45, 0.22, 0.11, 0.6, 'slit')]}))
    horn = T.m_bone('#2A2026')
    for s in (1, -1):
        nm = 'horn_' + ('l' if s > 0 else 'r')
        m.bone(nm, 'head', (s * 4, k['headtop'] - 2, 0), rot=(-30, 0, s * 60))
        m.cbox(nm, s * 4, k['headtop'] - 2, 0, 2, 9, 2, horn)
        m.cbox(nm, s * 4, k['headtop'] + 6, 1, 1, 4, 1, horn)
        sd = 'l' if s > 0 else 'r'
        m.cbox('arm_' + sd, s * k['ax'] + s, k['shoulder'] - 3, 0, 7, 5, 7, red, inflate=0.3)
        m.cbox('arm_' + sd, s * k['ax'] + s * 2, k['shoulder'] + 2, 0, 2, 3, 2, horn)
    for x in (-3, 0, 3):  # crown
        m.cbox('head', x, k['headtop'], -3, 1, 3 if x else 4, 1, T.m_metal('#C9A040', rivets=False))
    cape(m, k, T.m_cloth('#2A0A12', trim='#7A3AFF'), length=24, width=13)
    weapon_sword(m, k, 'r', 22, 3, blade='#4A3A6A', hilt='#1A1420', guard='#C9A040', glow='#B08AFF')


@reg('ant_soldier', 'BIPED', 0.8, 1.0, 0.25, shadow=0.6)
def ant_soldier(m):
    ch = T.m_chitin('#2E2440', '#9A7ACF', 3)
    dark = T.m_chitin('#1A1426', '#7A5ACF')
    k = humanoid(m, dict(leg=14, legw=3, hipw=8, torso=12, chestw=10, chestd=7, waistw=4, neck=1, head=(8, 7, 9), arm=14, armw=3,
                         mats=dict(skin=dark, legs=dark, feet=dark, hips=ch, torso=dark, chest=ch, arm=ch, forearm=dark, hand=dark, head=ch),
                         face={'front': [eyes('#C07AFF', '#FFFFFF', 0.35, 0.33, 0.14, 0.2)]}))
    # abdomen behind
    m.cbox('body', 0, k['leg'] - 2, 7, 9, 8, 10, T.m_chitin('#3A2A50', '#B08AE0', 4))
    for s, nm in ((1, 'mandible_l'), (-1, 'mandible_r')):
        m.bone(nm, 'head', (s * 2.5, k['neck'] + 1, -4.5))
        m.box(nm, s * 2.5 - 0.5, k['neck'], -8, 1, 1, 4, T.m_chitin('#120C1C', '#FF9AFF'))
    for s, nm in ((1, 'antenna_l'), (-1, 'antenna_r')):
        m.bone(nm, 'head', (s * 2, k['headtop'], -3), rot=(-35, s * 25, 0))
        m.box(nm, s * 2 - 0.5, k['headtop'], -12, 1, 1, 9, dark)
    for s in (1, -1):  # blade claws
        m.cbox('forearm_' + ('l' if s > 0 else 'r'), s * k['ax'], k['hand'] - 4, -1, 1, 6, 2, T.m_chitin('#4A3A6A', '#E0C0FF'))


@reg('beru', 'BIPED', 0.85, 1.0, 0.15, shadow=0.6)
def beru(m):
    ch = T.m_chitin('#1A1424', '#A07AFF', 3)
    plate = T.m_chitin('#2A1E3A', '#C8A8FF')
    k = humanoid(m, dict(leg=17, legw=3, hipw=8, torso=15, chestw=11, chestd=6, waistw=6, neck=2, head=(8, 9, 8), arm=16, armw=3,
                         mats=dict(skin=ch, legs=ch, feet=ch, hips=plate, torso=ch, chest=plate, arm=plate, forearm=ch, hand=ch, head=plate),
                         chest_decals={'front': [runes('#B080FF', 21, 0.5)]},
                         face={'front': [eyes('#E04AFF', '#FFFFFF', 0.4, 0.26, 0.13, 0.7)]}))
    # head crest
    for x, hgt in ((-3, 5), (0, 7), (3, 5)):
        m.cbox('head', x, k['headtop'], 1, 1, hgt, 2, plate)
    for s, nm in ((1, 'mandible_l'), (-1, 'mandible_r')):
        m.bone(nm, 'head', (s * 2.5, k['neck'] + 1, -4))
        m.box(nm, s * 2.5 - 0.5, k['neck'], -7, 1, 2, 3, T.m_chitin('#120C1C', '#FF9AFF'))
    for s, nm in ((1, 'wing_l'), (-1, 'wing_r')):
        m.bone(nm, 'body', (s * 2, k['top'] - 2, 3.5))
        wing = T.m_chitin('#2C1E50', '#B89AFF')
        m.box(nm, s * 1 - (0 if s > 0 else 4), k['top'] - 16, 3.5, 4, 18, 1, wing, mirror=s < 0)
        m.box(nm, s * 4 - (0 if s > 0 else 4), k['top'] - 13, 3.5, 4, 14, 1, wing, mirror=s < 0)
        m.box(nm, s * 7 - (0 if s > 0 else 3), k['top'] - 9, 3.5, 3, 9, 1, wing, mirror=s < 0)
    prev, tz = 'body', 3
    for i in range(3):
        nm = f'tail{i}'
        m.bone(nm, prev, (0, k['leg'], tz), rot=(-30 if i == 0 else 12, 0, 0))
        m.cbox(nm, 0, k['leg'] - 1, tz + 2.5, 3 - i * 0.5, 3 - i * 0.5, 5, plate)
        prev, tz = nm, tz + 5
    m.cbox('tail2', 0, k['leg'] - 1, tz, 1, 1, 3, T.m_glow('#E04AFF'), glow=True)
    for s in (1, -1):
        m.cbox('forearm_' + ('l' if s > 0 else 'r'), s * k['ax'], k['hand'] - 3, -1.5, 1, 5, 1, T.m_chitin('#5A3A8A', '#F0C0FF'))
