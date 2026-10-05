"""Parametric 3D weapon models (swords, daggers, greatswords, staves) as JSON element models."""
import math
from blockmodel import *

CX, CZ = 8.0, 8.0


def build_blade(bm, y0, length, w0, w1, tip, thick, curve=0.0, teeth=0, tooth_len=1.0, glow=None, n=None, flare=0.0,
                notch=0.0, fuller=False):
    """blade rising along +y from y0. width tapers w0 -> w1 then a pointed tip of height `tip`."""
    n = n or max(6, int(length / 0.9))
    seg = length / n
    for i in range(n):
        t = i / n
        y = y0 + i * seg
        w = w0 + (w1 - w0) * t + flare * math.sin(t * math.pi)
        if notch and 0.30 < t < 0.42:
            w *= (1 - notch)
        xo = curve * (t ** 2)
        a = CX - w / 2 + xo
        b = CX + w / 2 + xo
        # bevelled cross-section: ridge + two thin wings
        rw = max(0.8, w * 0.46)
        bm.box((CX - rw / 2 + xo, y, CZ - thick / 2), (CX + rw / 2 + xo, y + seg + 0.02, CZ + thick / 2), 'blade')
        bm.box((a, y, CZ - thick * 0.28), (CX - rw / 2 + xo, y + seg + 0.02, CZ + thick * 0.28), 'edge')
        bm.box((CX + rw / 2 + xo, y, CZ - thick * 0.28), (b, y + seg + 0.02, CZ + thick * 0.28), 'edge')
        if glow and 0.1 < t < 0.9 and i % 3 == 0:
            bm.box((CX - 0.35 + xo, y + 0.3, CZ - thick / 2 - 0.08), (CX + 0.35 + xo, y + seg - 0.1, CZ + thick / 2 + 0.08), 'glow')
        if teeth and 0.18 < t < 0.82 and i % 4 == 2:
            for sd in (-1, 1):
                x0 = (b if sd > 0 else a - tooth_len)
                bm.box((x0, y, CZ - thick * 0.2), (x0 + tooth_len, y + seg * 0.7, CZ + thick * 0.2), 'edge')
    # tip: 3 shrinking steps
    wt = w0 + (w1 - w0) + 0
    xo = curve
    for k in range(3):
        f = 1 - (k + 1) / 4.0
        h = tip / 3.0
        y = y0 + length + k * h
        w = max(0.5, wt * f)
        bm.box((CX - w / 2 + xo, y, CZ - thick * 0.4 * f - 0.15), (CX + w / 2 + xo, y + h + 0.02, CZ + thick * 0.4 * f + 0.15), 'edge' if k else 'blade')


def build_guard(bm, y, kind, w, mat='guard', gem_mat='gem'):
    if kind == 'cross':
        bm.box((CX - w / 2, y, CZ - 1.4), (CX + w / 2, y + 1.2, CZ + 1.4), mat)
        for sd in (-1, 1):
            xe = CX + sd * (w / 2)
            bm.box((min(xe, xe + sd * 0.9), y - 0.5, CZ - 1.0), (max(xe, xe + sd * 0.9), y + 1.9, CZ + 1.0), mat)
        bm.box((CX - 0.9, y + 0.2, CZ - 1.7), (CX + 0.9, y + 1.0, CZ + 1.7), gem_mat)
    elif kind == 'wing':
        for sd in (-1, 1):
            for k in range(4):
                xa = CX + sd * (1 + k * w / 8)
                bm.box((min(xa, xa + sd * w / 8), y + k * 0.35, CZ - 1.1), (max(xa, xa + sd * w / 8), y + 1.0 + k * 0.7, CZ + 1.1), mat)
        bm.box((CX - 1.2, y - 0.2, CZ - 1.6), (CX + 1.2, y + 1.6, CZ + 1.6), mat)
        bm.box((CX - 0.7, y + 0.2, CZ - 1.9), (CX + 0.7, y + 1.1, CZ + 1.9), gem_mat)
    elif kind == 'hook':
        bm.box((CX - w / 2, y, CZ - 1.2), (CX + w / 2, y + 1.0, CZ + 1.2), mat)
        for sd in (-1, 1):
            xe = CX + sd * w / 2
            bm.box((min(xe, xe + sd * 1.0), y, CZ - 1.0), (max(xe, xe + sd * 1.0), y + 2.6, CZ + 1.0), mat)
            bm.box((min(xe - sd * 1, xe), y + 2.6, CZ - 0.8), (max(xe - sd * 1, xe), y + 3.4, CZ + 0.8), mat)
        bm.box((CX - 1, y + 0.1, CZ - 1.5), (CX + 1, y + 1.0, CZ + 1.5), gem_mat)
    elif kind == 'small':
        bm.box((CX - w / 2, y, CZ - 1.1), (CX + w / 2, y + 0.9, CZ + 1.1), mat)
        bm.box((CX - 0.7, y + 0.1, CZ - 1.4), (CX + 0.7, y + 0.8, CZ + 1.4), gem_mat)
    elif kind == 'spike':
        bm.box((CX - w / 2, y, CZ - 1.3), (CX + w / 2, y + 1.1, CZ + 1.3), mat)
        for sd in (-1, 1):
            for k in range(3):
                xe = CX + sd * (w / 2 - k * 1.1)
                bm.box((xe - 0.4, y + 1.1, CZ - 0.5), (xe + 0.4, y + 1.1 + 1.8 - k * 0.5, CZ + 0.5), mat)
        bm.box((CX - 1.0, y + 0.1, CZ - 1.6), (CX + 1.0, y + 1.0, CZ + 1.6), gem_mat)


def build_handle(bm, y0, length, thick=1.7, pommel='ball', rim=True):
    n = max(2, int(length / 1.1))
    seg = length / n
    for i in range(n):
        y = y0 + i * seg
        bm.box((CX - thick / 2, y, CZ - thick / 2), (CX + thick / 2, y + seg + 0.02, CZ + thick / 2), 'wrap')
    if rim:
        bm.box((CX - thick / 2 - 0.25, y0 + length - 0.5, CZ - thick / 2 - 0.25), (CX + thick / 2 + 0.25, y0 + length, CZ + thick / 2 + 0.25), 'guard')
    if pommel == 'ball':
        bm.box((CX - 1.3, y0 - 1.6, CZ - 1.3), (CX + 1.3, y0, CZ + 1.3), 'guard')
        bm.box((CX - 0.8, y0 - 2.2, CZ - 0.8), (CX + 0.8, y0 - 1.6, CZ + 0.8), 'gem')
    elif pommel == 'ring':
        bm.box((CX - 1.6, y0 - 0.8, CZ - 0.5), (CX + 1.6, y0, CZ + 0.5), 'guard')
        bm.box((CX - 1.6, y0 - 2.8, CZ - 0.5), (CX - 0.9, y0 - 0.8, CZ + 0.5), 'guard')
        bm.box((CX + 0.9, y0 - 2.8, CZ - 0.5), (CX + 1.6, y0 - 0.8, CZ + 0.5), 'guard')
        bm.box((CX - 1.6, y0 - 3.5, CZ - 0.5), (CX + 1.6, y0 - 2.8, CZ + 0.5), 'guard')
    elif pommel == 'spike':
        bm.box((CX - 1.2, y0 - 1.2, CZ - 1.2), (CX + 1.2, y0, CZ + 1.2), 'guard')
        bm.box((CX - 0.6, y0 - 2.4, CZ - 0.6), (CX + 0.6, y0 - 1.2, CZ + 0.6), 'guard')


def sword(spec):
    bm = BM(spec['id'], size=spec.get('atlas', 256), ppu=spec.get('ppu', 2.0))
    h = spec.get('handle', 4.5)
    y0 = 0.0
    build_handle(bm, y0, h, pommel=spec.get('pommel', 'ball'), thick=spec.get('hthick', 1.7))
    gk = spec.get('guard', 'cross')
    build_guard(bm, y0 + h, gk, spec.get('guard_w', 6.5))
    gh = {'cross': 1.2, 'wing': 1.6, 'hook': 1.0, 'small': 0.9, 'spike': 1.1}[gk]
    build_blade(bm, h + gh, spec['len'], spec['w0'], spec['w1'], spec['tip'], spec.get('thick', 1.3),
                curve=spec.get('curve', 0.0), teeth=spec.get('teeth', 0), glow=spec.get('glow'),
                flare=spec.get('flare', 0.0), notch=spec.get('notch', 0.0))
    for x, y, z, s in spec.get('extra_gems', []):
        bm.box((x - s / 2, y, z - s / 2), (x + s / 2, y + s, z + s / 2), 'gem')
    bm.pack()
    m = {
        'blade': metal(spec['blade'][0], spec['blade'][1], fuller=True),
        'edge': metal(spec['edge'][0], spec['edge'][1], grain=0.03),
        'guard': metal(spec['guard_c'][0], spec['guard_c'][1]),
        'wrap': wrap(spec['wrap'][0], spec['wrap'][1], stripe=1),
        'gem': gem(spec['gem'][0], spec['gem'][1]),
        'glow': energy(spec.get('glow', ((255, 255, 255), (255, 255, 255)))[0], spec.get('glow', ((255, 255, 255), (255, 255, 255)))[1]),
    }
    bm.paint(m)
    return bm


def staff(spec):
    bm = BM(spec['id'], size=256, ppu=2.0)
    L = spec['len']
    n = int(L / 1.5)
    for i in range(n):
        y = i * (L / n)
        off = math.sin(i * 0.5) * 0.15
        bm.box((CX - 0.8 + off, y, CZ - 0.8), (CX + 0.8 + off, y + L / n + 0.02, CZ + 0.8), 'wood')
    for y in (L * 0.15, L * 0.5):
        bm.box((CX - 1.2, y, CZ - 1.2), (CX + 1.2, y + 0.9, CZ + 1.2), 'guard')
    ty = L
    # claw prongs holding the crystal
    for sx, sz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        for k in range(4):
            bm.box((CX + sx * (1.1 + k * 0.25) - 0.45, ty + k * 1.1, CZ + sz * (1.1 + k * 0.25) - 0.45),
                   (CX + sx * (1.1 + k * 0.25) + 0.45, ty + (k + 1) * 1.1, CZ + sz * (1.1 + k * 0.25) + 0.45), 'guard')
    # crystal
    for k, (w, h2) in enumerate(((2.0, 1.5), (3.2, 2.0), (3.6, 3.0), (2.6, 2.0), (1.4, 1.5))):
        yy = ty + 1.0 + sum(x[1] for x in ((2.0, 1.5), (3.2, 2.0), (3.6, 3.0), (2.6, 2.0), (1.4, 1.5))[:k])
        bm.box((CX - w / 2, yy, CZ - w / 2), (CX + w / 2, yy + h2, CZ + w / 2), 'crystal')
    bm.box((CX - 0.8, ty + 3.2, CZ - 0.8), (CX + 0.8, ty + 6.0, CZ + 0.8), 'core')
    bm.box((CX - 0.8, -1.0, CZ - 0.8), (CX + 0.8, 0, CZ + 0.8), 'guard')
    bm.pack()
    bm.paint({'wood': wood(spec['wood'][0], spec['wood'][1]), 'guard': metal(spec['guard_c'][0], spec['guard_c'][1]),
              'crystal': crystal(spec['crystal'][0], spec['crystal'][1], 210, 0.5), 'core': energy(spec['core'][0], spec['core'][1])})
    return bm


R = lambda *a: tuple(a)

SWORDS = [
    dict(id='hunter_dagger', rank='E', len=7.5, w0=2.4, w1=2.0, tip=2.5, thick=1.0, handle=3.5, guard='small', guard_w=4.5,
         blade=((120, 130, 142), (205, 214, 226)), edge=((150, 160, 172), (235, 240, 248)), guard_c=((70, 72, 80), (130, 134, 146)),
         wrap=((52, 34, 24), (84, 56, 38)), gem=((120, 200, 255), (40, 90, 170)), pommel='ball'),
    dict(id='iron_blade', rank='D', len=15, w0=3.0, w1=2.6, tip=3.5, thick=1.3, handle=4.0, guard='cross', guard_w=7,
         blade=((95, 100, 110), (190, 196, 208)), edge=((140, 146, 156), (232, 236, 242)), guard_c=((80, 60, 40), (150, 120, 70)),
         wrap=((40, 40, 48), (70, 70, 84)), gem=((255, 200, 120), (170, 90, 30))),
    dict(id='knight_killer', rank='C', len=12, w0=2.8, w1=2.2, tip=3, thick=1.2, handle=4.0, guard='hook', guard_w=6,
         blade=((70, 90, 120), (170, 200, 235)), edge=((120, 150, 190), (225, 238, 252)), guard_c=((46, 52, 66), (112, 124, 150)),
         wrap=((30, 36, 52), (58, 70, 100)), gem=((120, 190, 255), (30, 80, 160)), notch=0.25, pommel='ring'),
    dict(id='mithril_sword', rank='B', len=17, w0=3.2, w1=2.6, tip=4, thick=1.2, handle=4.2, guard='wing', guard_w=8,
         blade=((120, 170, 205), (220, 245, 255)), edge=((170, 215, 240), (250, 255, 255)), guard_c=((60, 100, 130), (160, 210, 235)),
         wrap=((30, 54, 80), (60, 100, 140)), gem=((140, 255, 255), (20, 140, 190)), glow=((140, 235, 255), (230, 255, 255))),
    dict(id='kasaka_venom_fang', rank='B', len=11, w0=3.4, w1=1.8, tip=3, thick=1.3, handle=3.8, guard='small', guard_w=5,
         curve=-3.0, blade=((30, 90, 40), (150, 230, 110)), edge=((70, 150, 60), (215, 255, 170)), guard_c=((40, 30, 50), (110, 84, 130)),
         wrap=((28, 40, 24), (60, 90, 44)), gem=((190, 255, 80), (50, 140, 20)), pommel='spike'),
    dict(id='baruka_dagger', rank='A', len=10, w0=3.0, w1=2.0, tip=3.2, thick=1.3, handle=3.8, guard='spike', guard_w=6,
         teeth=1, blade=((70, 130, 190), (200, 240, 255)), edge=((130, 190, 235), (245, 252, 255)), guard_c=((50, 70, 110), (150, 190, 235)),
         wrap=((24, 40, 70), (56, 96, 150)), gem=((220, 250, 255), (60, 170, 255)), glow=((120, 220, 255), (240, 255, 255)), pommel='ring'),
    dict(id='obsidian_greatsword', rank='A', len=22, w0=4.4, w1=3.6, tip=4.5, thick=1.6, handle=5.0, guard='cross', guard_w=10,
         hthick=2.0, blade=((22, 20, 30), (84, 78, 110)), edge=((60, 54, 84), (150, 140, 190)), guard_c=((30, 28, 38), (96, 90, 120)),
         wrap=((20, 18, 26), (48, 44, 62)), gem=((190, 120, 255), (80, 20, 160)), glow=((120, 50, 200), (210, 150, 255))),
    dict(id='igris_blade', rank='A', len=21, w0=4.0, w1=3.0, tip=5, thick=1.5, handle=5.2, guard='wing', guard_w=11,
         hthick=1.9, blade=((90, 14, 24), (230, 70, 80)), edge=((150, 30, 40), (255, 150, 150)), guard_c=((40, 30, 34), (120, 100, 106)),
         wrap=((24, 18, 20), (60, 40, 46)), gem=((255, 120, 120), (160, 10, 20)), glow=((190, 20, 30), (255, 130, 110)), pommel='spike'),
    dict(id='demon_king_dagger', rank='S', len=11, w0=3.0, w1=2.0, tip=3.5, thick=1.3, handle=3.8, guard='spike', guard_w=7,
         teeth=1, curve=1.6, blade=((70, 20, 120), (190, 90, 255)), edge=((120, 50, 190), (235, 170, 255)), guard_c=((26, 20, 36), (84, 60, 110)),
         wrap=((26, 14, 40), (70, 40, 100)), gem=((255, 160, 255), (120, 20, 200)), glow=((150, 40, 255), (240, 190, 255)), pommel='spike'),
    dict(id='demon_monarch_longsword', rank='S', len=23, w0=4.2, w1=3.2, tip=5, thick=1.5, handle=5.0, guard='hook', guard_w=10,
         hthick=1.9, teeth=1, blade=((20, 8, 20), (110, 24, 60)), edge=((70, 16, 40), (230, 60, 100)), guard_c=((24, 10, 18), (130, 30, 60)),
         wrap=((18, 8, 14), (60, 20, 34)), gem=((255, 90, 120), (150, 0, 30)), glow=((220, 20, 60), (255, 150, 160)), pommel='spike'),
    dict(id='kamish_wrath', rank='S', len=13, w0=3.6, w1=2.2, tip=3.5, thick=1.5, handle=4.0, guard='wing', guard_w=8,
         teeth=1, tooth_len=1.0, blade=((150, 90, 20), (255, 215, 90)), edge=((210, 140, 40), (255, 245, 190)), guard_c=((100, 24, 20), (230, 80, 50)),
         wrap=((70, 20, 16), (150, 50, 30)), gem=((255, 240, 140), (230, 40, 20)), glow=((255, 120, 20), (255, 240, 150)), pommel='ball'),
    dict(id='shadow_monarch_sword', rank='N', len=20, w0=3.6, w1=2.8, tip=5, thick=1.4, handle=4.6, guard='wing', guard_w=10,
         blade=((8, 6, 20), (60, 40, 120)), edge=((40, 24, 90), (170, 120, 255)), guard_c=((14, 10, 28), (80, 56, 150)),
         wrap=((10, 8, 22), (40, 30, 80)), gem=((210, 160, 255), (90, 30, 200)), glow=((110, 40, 255), (230, 190, 255)), pommel='ring'),
]

STAVES = [
    dict(id='ice_elf_staff', rank='A', len=22, wood=((120, 150, 190), (200, 230, 255)), guard_c=((70, 100, 140), (190, 225, 250)),
         crystal=((90, 170, 255), (230, 250, 255)), core=((120, 230, 255), (255, 255, 255))),
    dict(id='necromancer_staff', rank='S', len=22, wood=((20, 14, 30), (70, 40, 100)), guard_c=((30, 24, 44), (110, 80, 150)),
         crystal=((90, 30, 190), (220, 150, 255)), core=((180, 80, 255), (255, 230, 255))),
]


def all_models():
    out = {}
    for s in SWORDS:
        out[s['id']] = (sword(s), DISPLAY_SWORD, s)
    for s in STAVES:
        out[s['id']] = (staff(s), DISPLAY_SWORD, s)
    return out


if __name__ == '__main__':
    import os
    os.makedirs('/tmp/prev', exist_ok=True)
    ms = all_models()
    tiles = []
    for k, (bm, _, s) in ms.items():
        im = bm.render(yaw=-30, pitch=12, roll=-35, size=360, zoom=1.0)
        tiles.append(im)
    cols = 4
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new('RGB', (360 * cols, 360 * rows))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % cols) * 360, (i // cols) * 360))
    sheet.save('/tmp/prev/weapons.png')
    print(len(ms), 'models', [len(bm.els) for bm, _, _ in ms.values()])
