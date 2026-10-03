"""Procedural structures of Azrakor -> vanilla structure .nbt files + jigsaw/structure JSON.   python3 gen/structures.py [preview]"""
import os, sys, json, math, random
sys.path.insert(0, os.path.dirname(__file__))
from vox import Vox, B, st, sl, lg, banner, AIR, DIRS, OPP

ROOT = os.path.join(os.path.dirname(__file__), '..')
DATA = os.path.join(ROOT, 'src/main/resources/data/krolasyonbosses')
NS = 'krolasyonbosses'
PREV = '/tmp/claude-0/pv/struct'
os.makedirs(PREV, exist_ok=True)


def mod(n):
    return B(f'{NS}:{n}')


# ------------------------------------------------------------------ palettes
STYLES = {
    'ember': dict(
        wall=B('polished_blackstone_bricks'), wall2=B('blackstone'), wall3=B('cracked_polished_blackstone_bricks'), trim=B('gilded_blackstone'), pillar=B('chiseled_polished_blackstone'),
        floor=B('polished_blackstone'), floor2=B('blackstone'), found=B('blackstone'), roof=B('obsidian'), roof_stair='polished_blackstone_brick_stairs', slab='polished_blackstone_brick_slab',
        lamp=mod('ember_lamp'), glass=B('orange_stained_glass'), accent=B('magma_block'), banner='red', banner2='orange', stair='polished_blackstone_brick_stairs',
        faction='ember'),
    'bone': dict(
        wall=B('quartz_bricks'), wall2=B('bone_block', axis='y'), wall3=B('calcite'), trim=B('chiseled_quartz_block'), pillar=B('bone_block', axis='y'),
        floor=B('smooth_quartz'), floor2=B('quartz_bricks'), found=B('calcite'), roof=B('bone_block', axis='y'), roof_stair='quartz_stairs', slab='quartz_slab',
        lamp=mod('soul_lamp'), glass=B('light_blue_stained_glass'), accent=B('soul_lantern', hanging='false'), banner='white', banner2='black', stair='quartz_stairs', faction='bone'),
    'blood': dict(
        wall=B('red_nether_bricks'), wall2=B('nether_bricks'), wall3=B('cracked_nether_bricks'), trim=B('chiseled_nether_bricks'), pillar=B('crimson_hyphae', axis='y'),
        floor=B('crimson_planks'), floor2=B('red_nether_bricks'), found=B('nether_bricks'), roof=B('red_stained_glass'), roof_stair='red_nether_brick_stairs', slab='red_nether_brick_slab',
        lamp=mod('blood_lamp'), glass=B('red_stained_glass'), accent=B('shroomlight'), banner='red', banner2='black', stair='red_nether_brick_stairs', faction='blood'),
    'shadow': dict(
        wall=B('deepslate_bricks'), wall2=B('polished_deepslate'), wall3=B('cracked_deepslate_bricks'), trim=B('crying_obsidian'), pillar=B('purpur_pillar', axis='y'),
        floor=B('polished_blackstone'), floor2=B('deepslate_tiles'), found=B('deepslate'), roof=B('obsidian'), roof_stair='deepslate_brick_stairs', slab='deepslate_brick_slab',
        lamp=mod('shadow_lamp'), glass=B('purple_stained_glass'), accent=B('amethyst_block'), banner='purple', banner2='black', stair='deepslate_brick_stairs', faction='shadow'),
    'rot': dict(
        wall=B('mud_bricks'), wall2=B('mangrove_planks'), wall3=B('packed_mud'), trim=B('mangrove_log', axis='y'), pillar=B('mushroom_stem'),
        floor=B('moss_block'), floor2=B('mud_bricks'), found=B('packed_mud'), roof=B('red_mushroom_block'), roof_stair='mud_brick_stairs', slab='mud_brick_slab',
        lamp=mod('rot_lamp'), glass=B('lime_stained_glass'), accent=B('shroomlight'), banner='green', banner2='brown', stair='mud_brick_stairs', faction='rot'),
}


# ------------------------------------------------------------------ primitives
def crenels(v, y, pts, block):
    for i, (x, z) in enumerate(pts):
        if i % 2 == 0:
            v.set(x, y, z, block)


def ring_wall(v, S, x0, z0, x1, z1, y, h, thick=2):
    for t in range(thick):
        for yy in range(y, y + h):
            for x in range(x0 + t, x1 - t + 1):
                for z in (z0 + t, z1 - t):
                    v.set(x, yy, z, S['wall'] if (yy - y) % 4 else S['wall2'])
            for z in range(z0 + t, z1 - t + 1):
                for x in (x0 + t, x1 - t):
                    v.set(x, yy, z, S['wall'] if (yy - y) % 4 else S['wall2'])
    # battlements on the outer ring
    pts = [(x, z0) for x in range(x0, x1 + 1)] + [(x1, z) for z in range(z0 + 1, z1 + 1)] + [(x, z1) for x in range(x1 - 1, x0 - 1, -1)] + [(x0, z) for z in range(z1 - 1, z0, -1)]
    crenels(v, y + h, pts, S['wall2'])
    # walkway on the inner row
    for x in range(x0 + thick, x1 - thick + 1):
        for z in (z0 + thick, z1 - thick):
            v.set(x, y + h - 2, z, S['floor2'])
    for z in range(z0 + thick, z1 - thick + 1):
        for x in (x0 + thick, x1 - thick):
            v.set(x, y + h - 2, z, S['floor2'])


def round_tower(v, S, cx, cz, r, y, h, roof_h=None, roof=None):
    roof = roof or S['roof']
    v.cylinder(cx, cz, r, y, y + h, S['wall'], hollow=True)
    for k in range(0, h, 4):
        v.disc(cx, cz, r, y + k, S['wall2'], hollow=True)
    v.disc(cx, cz, r - 1, y, S['floor'])
    v.disc(cx, cz, r - 1, y + h // 2, S['floor2'])
    # slit windows in the four directions
    for dx, dz in ((r, 0), (-r, 0), (0, r), (0, -r)):
        for yy in (y + 3, y + h // 2 + 3):
            v.set(cx + dx, yy, cz + dz, S['glass'])
            v.set(cx + dx, yy + 1, cz + dz, S['glass'])
    # parapet
    v.disc(cx, cz, r + 1, y + h, S['wall2'])
    v.disc(cx, cz, r - 0.5, y + h, AIR)
    for a in range(0, 360, 30):
        x = round(cx + math.cos(math.radians(a)) * (r + 1)); z = round(cz + math.sin(math.radians(a)) * (r + 1))
        v.set(x, y + h + 1, z, S['wall2'])
    # roof cone
    rh = roof_h or r * 2 + 2
    v.cone(cx, cz, r + 1, y + h + 1, rh, roof)
    v.set(cx, y + h + 1 + rh, cz, S['lamp'])
    # door slit at base and lamp ring
    v.set(cx, y + 1, cz + r, AIR); v.set(cx, y + 2, cz + r, AIR)
    for a in (45, 135, 225, 315):
        x = round(cx + math.cos(math.radians(a)) * (r + 1)); z = round(cz + math.sin(math.radians(a)) * (r + 1))
        v.set(x, y + 4, z, S['lamp'])


def stairs_run(v, S, x, z, y, direction, width, steps, perp, fill=None):
    """straight staircase ascending towards `direction`; (x,z,y) is the first (lowest) step"""
    dx, dz = DIRS[direction]
    px, pz = (1, 0) if perp == 'x' else (0, 1)
    for i in range(steps):
        for w in range(width):
            xx, zz = x + dx * i + px * w, z + dz * i + pz * w
            v.set(xx, y + i, zz, st(S['stair'], direction))
            for yy in range(y, y + i):
                v.set(xx, yy, zz, fill or S['wall2'])


def brazier(v, S, x, y, z, h=2):
    for k in range(h):
        v.set(x, y + k, z, S['pillar'])
    v.set(x, y + h, z, S['lamp'])


def lamp_post(v, S, x, y, z, h=3):
    for k in range(h):
        v.set(x, y + k, z, S['wall2'])
    v.set(x, y + h, z, S['lamp'])


def wall_banners(v, S, x, y, z, facing, n=3, gap=4, colors=None):
    dx, dz = (1, 0) if facing in ('north', 'south') else (0, 1)
    for i in range(n):
        c = (colors or [S['banner'], S['banner2']])[i % 2]
        v.set(x + dx * i * gap, y, z + dz * i * gap, banner(c, facing))
        v.set(x + dx * i * gap, y + 1, z + dz * i * gap, banner(c, facing))


def terrace_stack(v, S, F, levels):
    """levels: list of (x0,z0,x1,z1,h); stacks solid terraces with trim"""
    y = F
    for (x0, z0, x1, z1, h) in levels:
        for yy in range(y, y + h):
            v.fill(x0, yy, z0, x1, yy, z1, S['found'])
            # facing skin
            v.shell(x0, yy, z0, x1, yy, z1, S['wall'] if (yy - y) % 3 else S['wall2'])
        v.shell(x0, y + h - 1, z0, x1, y + h - 1, z1, S['trim'])
        v.fill(x0 + 1, y + h - 1, z0 + 1, x1 - 1, y + h - 1, z1 - 1, S['floor'])
        # corner lamps
        for (cx, cz) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            brazier(v, S, cx, y + h, cz, 2)
        y += h
    return y


def throne_hall(v, S, x0, z0, x1, z1, y, h, boss=None, boss_extra=(), doors=('south',)):
    """hollow hall with columns, lamps, dais and a throne at the north end"""
    v.shell(x0, y, z0, x1, y + h, z1, S['wall'])
    v.carve(x0 + 1, y + 1, z0 + 1, x1 - 1, y + h - 1, z1 - 1)
    v.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, S['floor'])
    cx = (x0 + x1) // 2
    # carpet
    for z in range(z0 + 2, z1):
        for x in range(cx - 1, cx + 2):
            v.set(x, y, z, B(f'{S["banner"]}_carpet') if S['banner'] != 'white' else B('white_carpet'))
    # columns
    for z in range(z0 + 3, z1 - 1, 4):
        for x in (x0 + 2, x1 - 2):
            for yy in range(y + 1, y + h):
                v.set(x, yy, z, S['pillar'])
            v.set(x, y + h - 1, z, S['trim'])
            v.set(x + (1 if x < cx else -1), y + 4, z, S['lamp'])
    # ceiling beams
    for z in range(z0 + 1, z1, 4):
        v.fill(x0 + 1, y + h, z, x1 - 1, y + h, z, S['wall2'])
    # windows
    for z in range(z0 + 3, z1 - 1, 4):
        for yy in (y + 3, y + 4, y + 5):
            v.set(x0, yy, z, S['glass']); v.set(x1, yy, z, S['glass'])
    # dais + throne
    for k in range(3):
        v.fill(cx - 4 + k, y + 1 + (2 - k), z0 + 1, cx + 4 - k, y + 1 + (2 - k), z0 + 2 + (2 - k) * 0 + (k == 0) * 0, S['trim'] if k == 2 else S['wall'])
    v.fill(cx - 5, y + 1, z0 + 1, cx + 5, y + 1, z0 + 4, S['trim'])
    v.fill(cx - 3, y + 2, z0 + 1, cx + 3, y + 2, z0 + 3, S['wall'])
    v.fill(cx - 2, y + 3, z0 + 1, cx + 2, y + 3, z0 + 2, S['trim'])
    v.set(cx, y + 4, z0 + 2, st(S['stair'], 'south'))
    for yy in range(y + 4, y + 9):
        v.set(cx, yy, z0 + 1, S['pillar'])
    v.set(cx - 1, y + 4, z0 + 1, S['pillar']); v.set(cx + 1, y + 4, z0 + 1, S['pillar'])
    v.set(cx, y + 9, z0 + 1, S['lamp'])
    for dx in (-3, 3):
        brazier(v, S, cx + dx, y + 3, z0 + 3, 2)
    wall_banners(v, S, cx - 4, y + 5, z0 + 1, 'south', 2, 8)
    if 'south' in doors:
        v.carve(cx - 2, y + 1, z1, cx + 2, y + 5, z1)
        for yy in range(y + 1, y + 6):
            v.set(cx - 3, yy, z1, S['pillar']); v.set(cx + 3, yy, z1, S['pillar'])
        for xx in range(cx - 3, cx + 4):
            v.set(xx, y + 6, z1, S['trim'])
    if boss:
        v.ent(cx, y + 4, z0 + 2, boss, yaw=0.0)
    for (ex, ey, ez, eid) in boss_extra:
        v.ent(ex, ey, ez, eid)


def roof_pyramid(v, S, x0, z0, x1, z1, y, tall=True):
    k = 0
    while x0 + k <= x1 - k and z0 + k <= z1 - k:
        v.shell(x0 - 1 + k, y + k, z0 - 1 + k, x1 + 1 - k, y + k, z1 + 1 - k, S['roof'])
        k += 1
        if not tall and k > 5:
            break
    return y + k


# ------------------------------------------------------------------ citadel (generic, per style)
def citadel(style, signature):
    S = STYLES[style]
    n = 49
    v = Vox(n, 58, n, f'{style}_citadel')
    F = 6
    v.fill(0, 0, 0, n - 1, F - 2, n - 1, S['found'])
    v.carve(1, F, 1, n - 2, F + 32, n - 2)
    rnd = random.Random(hash(style) & 0xFFFF)
    for x in range(n):
        for z in range(n):
            v.set(x, F - 1, z, S['floor'] if (x // 2 + z // 2) % 2 else S['floor2'])
    ring_wall(v, S, 2, 2, n - 3, n - 3, F, 8)
    # gate
    v.carve(21, F, n - 5, 27, F + 6, n - 2)
    for yy in range(F, F + 7):
        v.set(20, yy, n - 4, S['pillar']); v.set(28, yy, n - 4, S['pillar'])
    for xx in range(20, 29):
        v.set(xx, F + 7, n - 4, S['trim']); v.set(xx, F + 7, n - 3, S['trim'])
    for (bx, bz) in ((19, n - 5), (29, n - 5)):
        brazier(v, S, bx, F, bz, 3)
    for (cx, cz) in ((5, 5), (n - 6, 5), (5, n - 6), (n - 6, n - 6)):
        round_tower(v, S, cx, cz, 4, F, 15)
    # inner keep: terraces
    top = terrace_stack(v, S, F, [(9, 9, 39, 39, 5), (13, 13, 35, 35, 5), (17, 17, 31, 31, 5)])
    # staircases (south side), each ascending to the next terrace
    stairs_run(v, S, 21, 40, F, 'north', 7, 5, 'x')
    v.carve(21, F, 40 - 1, 27, F + 5, 40 - 1)
    stairs_run(v, S, 21, 36, F + 5, 'north', 7, 5, 'x')
    stairs_run(v, S, 21, 32, F + 10, 'north', 7, 4, 'x')
    for z in (39, 35, 31):
        pass
    # courtyard lamps
    for z in range(n - 8, 40, -4):
        for x in (18, 30):
            lamp_post(v, S, x, F, z, 3)
    wall_banners(v, S, 11, F + 3, 40, 'south', 7, 4)
    wall_banners(v, S, 15, F + 8, 36, 'south', 5, 4)
    # throne hall on the highest terrace
    throne_hall(v, S, 17, 17, 31, 31, top - 1, 12, boss=signature.get('boss'), boss_extra=signature.get('guards', ()))
    roof_top = roof_pyramid(v, S, 17, 17, 31, 31, top + 11)
    v.set(24, roof_top, 24, S['lamp'])
    for k in range(1, 6):
        v.set(24, roof_top + k, 24, S['pillar'])
    v.set(24, roof_top + 6, 24, S['lamp'])
    signature['decor'](v, S, F, top)
    for (ex, ez, eid) in signature.get('yard', ()):
        v.ent(ex, F, ez, eid)
    return v


# ------------------------------------------------------------------ signature decorations
def decor_ember(v, S, F, top):
    # lava-glow vents: rows of magma and ember lamps along the courtyard, obsidian spikes on the walls
    for x in range(6, 43, 6):
        for z in (7, 41):
            v.set(x, F, z, S['accent'])
    for x in range(8, 41, 4):
        for h in (1, 2):
            v.set(x, F + 8 + h, 2, B('obsidian'))
    # big forge pit
    v.fill(21, F - 1, 25, 27, F - 1, 31, S['accent'])
    v.fill(23, F - 1, 27, 25, F - 1, 29, mod('ember_lamp'))


def decor_bone(v, S, F, top):
    # rib arches over the courtyard path and skull friezes
    for z in range(36, 46, 3):
        for a in range(0, 181, 15):
            x = round(24 + math.cos(math.radians(a)) * 5); y = F + 1 + round(math.sin(math.radians(a)) * 7)
            v.set(x, y, z, B('bone_block', axis='x'))
    for x in range(8, 41, 3):
        v.set(x, F + 7, 3, B('skeleton_skull', rotation='8'))
    for (cx, cz) in ((5, 5), (43, 5), (5, 43), (43, 43)):
        for k in range(3):
            v.set(cx, F + 22 + k, cz, B('bone_block', axis='y'))
    # soul fire braziers
    for (x, z) in ((20, 22), (28, 22)):
        v.set(x, F + 1, z, B('soul_campfire', lit='true'))


def decor_blood(v, S, F, top):
    # glass domes on towers and a blood fountain in the courtyard
    for (cx, cz) in ((5, 5), (43, 5), (5, 43), (43, 43)):
        v.dome(cx, F + 16, cz, 5, B('red_stained_glass'), hollow=True)
    v.disc(24, 44, 4, F, B('red_nether_bricks'), hollow=True)
    v.disc(24, 44, 3, F, B('red_concrete'))
    v.set(24, F + 1, 44, B('red_nether_bricks')); v.set(24, F + 2, 44, B('red_nether_bricks')); v.set(24, F + 3, 44, S['lamp'])
    for k in range(8, 41, 4):
        v.set(k, F, 8, B('crimson_roots'))
        v.set(k, F - 1, 8, B('crimson_nylium'))


def decor_shadow(v, S, F, top):
    # central black obelisk with a halo of floating amethyst blocks
    for k in range(0, 26):
        w = max(1, 3 - k // 9)
        v.fill(24 - w, top + 16 + k, 24 - w, 24 + w, top + 16 + k, 24 + w, B('obsidian'))
    v.set(24, top + 43, 24, S['lamp'])
    for a in range(0, 360, 30):
        x = round(24 + math.cos(math.radians(a)) * 9); z = round(24 + math.sin(math.radians(a)) * 9)
        v.set(x, top + 24 + (a // 30) % 3 * 2, z, B('amethyst_block'))
        v.set(x, top + 25 + (a // 30) % 3 * 2, z, S['lamp'])
    for x in range(8, 41, 8):
        v.set(x, F, 8, B('amethyst_cluster', facing='up'))


def decor_rot(v, S, F, top):
    # giant mushrooms in the yard and hanging vines
    for (cx, cz, r, h) in ((10, 40, 5, 9), (38, 40, 5, 8), (10, 10, 4, 7), (38, 10, 4, 7)):
        for k in range(h):
            v.set(cx, F + k, cz, B('mushroom_stem'))
        v.dome(cx, F + h - 1, cz, r, B('red_mushroom_block'), hollow=True)
    for x in range(4, 45, 2):
        for k in range(1, 4):
            v.set(x, F + 7 - k, 2, B('vine', south='true') if k < 3 else AIR)
    for x in range(6, 43, 3):
        v.set(x, F, 44, B('shroomlight'))


SIG = {
    'ember': dict(boss=f'{NS}:ember_queen', guards=((22, 29, 29, f'{NS}:magma_brute'),), yard=((8, 40, f'{NS}:crimson_hound'), (40, 40, f'{NS}:crimson_hound')), decor=decor_ember),
    'bone': dict(boss=f'{NS}:bone_king', guards=((22, 29, 29, f'{NS}:bone_knight'), (26, 29, 29, f'{NS}:bone_knight')), yard=((15, 40, f'{NS}:bone_hound'), (33, 40, f'{NS}:bone_hound')), decor=decor_bone),
    'blood': dict(boss=f'{NS}:heart_demon', guards=((22, 29, 29, f'{NS}:crimson_duelist'), (26, 29, 29, f'{NS}:blood_acolyte')), yard=((24, 40, f'{NS}:revenge'),), decor=decor_blood),
    'shadow': dict(boss=f'{NS}:hierophant_nyx', guards=((22, 29, 29, f'{NS}:void_cultist'), (26, 29, 29, f'{NS}:void_cultist')), yard=(), decor=decor_shadow),
    'rot': dict(boss=f'{NS}:rot_mother', guards=((22, 29, 29, f'{NS}:spore_shambler'),), yard=((10, 30, f'{NS}:bog_maw'),), decor=decor_rot),
}


# ------------------------------------------------------------------ outposts (envoy camps)
def outpost(style):
    S = STYLES[style]
    n = 21
    v = Vox(n, 18, n, f'{style}_outpost')
    F = 3
    v.fill(0, 0, 0, n - 1, F - 2, n - 1, S['found'])
    v.carve(0, F, 0, n - 1, F + 12, n - 1)
    for x in range(n):
        for z in range(n):
            v.set(x, F - 1, z, S['floor'] if (x + z) % 3 else S['floor2'])
    ring_wall(v, S, 1, 1, n - 2, n - 2, F, 4, thick=1)
    v.carve(9, F, n - 3, 11, F + 3, n - 2)
    for yy in range(F, F + 5):
        v.set(8, yy, n - 2, S['pillar']); v.set(12, yy, n - 2, S['pillar'])
    for xx in range(8, 13):
        v.set(xx, F + 5, n - 2, S['trim'])
    brazier(v, S, 7, F, n - 3, 2); brazier(v, S, 13, F, n - 3, 2)
    # watch tower in a corner
    round_tower(v, S, 4, 4, 3, F, 9, roof_h=5)
    # envoy pavilion: open pillars with a roof, banner and a table
    v.fill(7, F, 5, 13, F, 11, S['floor'])
    for (x, z) in ((7, 5), (13, 5), (7, 11), (13, 11)):
        for yy in range(F + 1, F + 5):
            v.set(x, yy, z, S['pillar'])
    v.fill(6, F + 5, 4, 14, F + 5, 12, S['wall2'])
    for k in range(3):
        v.fill(7 + k, F + 6 + k, 5 + k, 13 - k, F + 6 + k, 11 - k, S['roof'])
    v.set(10, F + 9, 8, S['lamp'])
    v.set(10, F + 1, 9, B('lectern', facing='south'))
    v.set(9, F + 1, 10, S['lamp']); v.set(11, F + 1, 10, S['lamp'])
    wall_banners(v, S, 8, F + 3, 4, 'south', 2, 4)
    v.set(15, F + 1, 7, B('barrel', facing='up', open='false'))
    v.set(16, F + 1, 7, B('barrel', facing='up', open='false'))
    v.set(15, F + 1, 12, B('chest', facing='west', type='single', waterlogged='false'))
    v.block_nbt(15, F + 1, 12, {'id': 'minecraft:chest', 'LootTable': f'{NS}:chests/outpost_{S["faction"]}'})
    v.ent(10, F + 1, 8, f'{NS}:envoy_{S["faction"]}')
    for (x, z) in ((5, 15), (16, 16)):
        lamp_post(v, S, x, F, z, 2)
    return v


# ------------------------------------------------------------------ the overworld hellgate ruins (three variants)
def hellgate(variant):
    n = 17
    v = Vox(n, 14, n, f'hellgate_{variant}')
    F = 4
    fr = mod('hellgate_stone')
    v.fill(0, 0, 0, n - 1, F - 2, n - 1, B('deepslate'))
    rnd = random.Random(variant * 77 + 5)
    for x in range(n):
        for z in range(n):
            if math.hypot(x - 8, z - 8) < 8.2:
                for y in range(F, F + 12):
                    v.set(x, y, z, AIR)
    # ash apron with irregular edge
    for x in range(n):
        for z in range(n):
            d = math.hypot(x - 8, z - 8)
            if d < 8.4 - rnd.random() * 2.2:
                v.set(x, F - 1, z, rnd.choice([mod('ashstone_bricks'), mod('ashstone'), B('blackstone'), B('cracked_polished_blackstone_bricks')]))
                if d < 6 and rnd.random() < 0.35:
                    v.set(x, F, z, rnd.choice([B('blackstone_slab', type='bottom', waterlogged='false'), B('magma_block')]))
    cx, cz = 8, 8
    axis_x = True
    # frame 4 wide x 5 tall, interior 2x3 -- placed so that portal blocks can be filled in at (cx-1..cx, F..F+2)
    for y in range(F - 1, F + 4):
        v.set(cx - 2, y, cz, fr); v.set(cx + 1, y, cz, fr)
    for a in (cx - 1, cx):
        v.set(a, F - 1, cz, fr); v.set(a, F + 3, cz, fr)
        for y in range(F, F + 3):
            v.set(a, y, cz, B(f'{NS}:hell_portal', axis='x'))
    if variant == 0:       # intact arch with side pillars and braziers
        for dx in (-4, 3):
            for y in range(F, F + 6):
                v.set(cx + dx, y, cz, B('chiseled_polished_blackstone') if y % 2 else B('polished_blackstone_bricks'))
            v.set(cx + dx, F + 6, cz, mod('ember_lamp'))
        v.fill(cx - 2, F + 4, cz, cx + 1, F + 4, cz, B('gilded_blackstone'))
        v.set(cx - 2, F + 5, cz, mod('ember_lamp')); v.set(cx + 1, F + 5, cz, mod('ember_lamp'))
    elif variant == 1:     # broken arch: one pillar fallen, rubble
        for y in range(F + 4, F + 7):
            v.set(cx - 2, y, cz, fr)
        v.set(cx + 1, F + 4, cz, fr)
        for (dx, dz) in ((3, 1), (4, 2), (-4, 1), (-3, 2), (4, 0)):
            v.set(cx + dx, F, cz + dz, B('polished_blackstone_bricks'))
            v.set(cx + dx, F + 1, cz + dz, B('cracked_polished_blackstone_bricks')) if rnd.random() < 0.6 else None
        for dx in (-4, 4):
            for y in range(F, F + 3 + rnd.randint(0, 3)):
                v.set(cx + dx, y, cz + 3, B('blackstone'))
            v.set(cx + dx, F + 4, cz + 3, B('soul_lantern', hanging='false')) if False else None
        v.set(cx - 3, F, cz + 2, mod('ember_lamp'))
    else:                  # altar gate: stepped dais and skulls, chains
        for k in range(3):
            v.fill(cx - 4 - k, F - 1 + 0, cz + 1 + k, cx + 3 + k, F - 1 + 0, cz + 1 + k, B('polished_blackstone_bricks'))
            v.fill(cx - 3 - k, F, cz + 1 + k, cx + 2 + k, F, cz + 1 + k, st('polished_blackstone_brick_stairs', 'north'))
        for dx in (-5, 4):
            for y in range(F, F + 5):
                v.set(cx + dx, y, cz + 1, B('polished_blackstone_bricks'))
            v.set(cx + dx, F + 5, cz + 1, B('skeleton_skull', rotation='0'))
            v.set(cx + dx, F + 6, cz + 1, mod('ember_lamp'))
        for y in range(F + 4, F + 8):
            v.set(cx - 1, y, cz, B('chain', axis='y'))
        v.set(cx - 1, F + 8, cz, B('blackstone'))
        v.set(cx + 1, F + 4, cz + 1, B('red_candle', candles='3', lit='true', waterlogged='false'))
    # nearby ruined stubs
    for (dx, dz) in ((-6, -5), (6, -4), (-5, 6), (6, 6)):
        h = rnd.randint(1, 4)
        for y in range(F, F + h):
            v.set(cx + dx, y, cz + dz, B('blackstone') if y % 2 else B('cracked_polished_blackstone_bricks'))
    return v


# ------------------------------------------------------------------ the throne hall
def throne():
    n = 73
    v = Vox(n, 52, n, 'throne_hall')
    F = 8
    S = dict(STYLES['ember'])
    S.update(wall=B('stone_bricks'), wall2=B('polished_andesite'), wall3=B('cracked_stone_bricks'), trim=B('chiseled_stone_bricks'), pillar=B('polished_andesite'),
             floor=B('smooth_stone'), floor2=B('stone_bricks'), found=B('andesite'), roof=B('tuff_bricks'), lamp=mod('hellgate_stone') if False else mod('ember_lamp'),
             glass=B('gray_stained_glass'), banner='gray', banner2='black', stair='stone_brick_stairs')
    v.fill(0, 0, 0, n - 1, F - 2, n - 1, S['found'])
    v.carve(1, F, 1, n - 2, F + 44, n - 2)
    for x in range(n):
        for z in range(n):
            v.set(x, F - 1, z, S['floor'] if (x // 3 + z // 3) % 2 else S['floor2'])
    # outer rim wall with 8 round towers
    ring_wall(v, S, 2, 2, n - 3, n - 3, F, 10)
    for (cx, cz) in ((6, 6), (n - 7, 6), (6, n - 7), (n - 7, n - 7), (n // 2, 6), (6, n // 2), (n - 7, n // 2), (n // 2, n - 7)):
        round_tower(v, S, cx, cz, 4, F, 14)
    # gate on the south
    v.carve(33, F, n - 5, 39, F + 8, n - 2)
    # plaza lamps and avenue
    for z in range(n - 8, 46, -5):
        for x in (31, 41):
            lamp_post(v, S, x, F, z, 4)
    # central hall (the Great Hall)
    top = F
    v.fill(14, F, 14, 58, F + 3, 58, S['found'])
    v.shell(14, F, 14, 58, F + 3, 58, S['wall2'])
    v.fill(15, F + 3, 15, 57, F + 3, 57, S['floor'])
    # grand stair to the hall
    stairs_run(v, S, 30, 59, F, 'north', 13, 3, 'x')
    throne_hall(v, S, 16, 16, 56, 50, F + 3, 18, boss=None)
    # five alcoves along the west/east walls, one per kingdom (pedestals for sigils)
    for k, (fac, col) in enumerate((('ember', 'red'), ('bone', 'white'), ('blood', 'red'), ('shadow', 'purple'), ('rot', 'green'))):
        z = 20 + k * 6
        x = 19
        for xx in (x, 53):
            v.fill(xx, F + 4, z, xx, F + 4, z + 1, S['trim'])
            v.set(xx + (1 if xx < 36 else -1), F + 4, z, B('lectern', facing='east' if xx < 36 else 'west'))
            v.set(xx + (1 if xx < 36 else -1), F + 5, z + 1, STYLES[fac]['lamp'])
            v.set(xx, F + 8, z, banner(col, 'east' if xx < 36 else 'west'))
            v.set(xx, F + 9, z, banner(col, 'east' if xx < 36 else 'west'))
    # the throne itself: a colossal ashen seat behind a sealed gate
    cx = 36
    for k in range(5):
        v.fill(cx - 7 + k, F + 4 + k, 17, cx + 7 - k, F + 4 + k, 18 + 0, S['trim'])
    v.fill(cx - 3, F + 9, 17, cx + 3, F + 9, 19, S['wall2'])
    for yy in range(F + 10, F + 18):
        v.fill(cx - 3, yy, 17, cx - 2, yy, 18, S['pillar']); v.fill(cx + 2, yy, 17, cx + 3, yy, 18, S['pillar'])
        v.fill(cx - 3, yy, 17, cx + 3, yy, 17, S['pillar']) if yy > F + 13 else None
    v.fill(cx - 1, F + 10, 19, cx + 1, F + 10, 19, S['wall2'])
    v.set(cx, F + 10, 19, st('stone_brick_stairs', 'south'))
    v.set(cx, F + 11, 18, mod('ember_lamp'))
    # sealed gate in front of the throne: obsidian wall that the ritual removes (marked by hellgate stone)
    for y in range(F + 4, F + 12):
        for x in range(cx - 4, cx + 5):
            v.set(x, y, 24, mod('hellgate_stone'))
    v.ent(cx, F + 4, 26, f'{NS}:seal_warden')
    return v


STRUCTS = {}


def dump(path, obj):
    p = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=1)


FACTION_MOBS = {
    'ember': ['cinder_imp', 'magma_brute', 'ember_archer'], 'bone': ['bone_knight', 'bone_marksman', 'bone_hound'],
    'blood': ['crimson_duelist', 'blood_acolyte', 'leech_bat'], 'shadow': ['shade_stalker', 'void_cultist', 'mirror_phantom'], 'rot': ['spore_shambler', 'thorn_strider', 'venom_wasp']}
FAC_MATERIAL = {'ember': 'ember_shard', 'bone': 'bone_dust', 'blood': 'blood_vial', 'shadow': 'shadow_essence', 'rot': 'rot_spore'}


def emit_worldgen(floors):
    def pool(name, elements):
        dump(f'worldgen/template_pool/{name}.json', {'fallback': 'minecraft:empty', 'elements': [
            {'weight': w, 'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:{loc}', 'processors': 'minecraft:empty', 'projection': 'rigid'}} for loc, w in elements]})

    def structure(name, biomes, pool_name, floor, spawns=None, adapt='beard_thin', dist=80):
        d = {'type': 'minecraft:jigsaw', 'biomes': biomes, 'step': 'surface_structures', 'spawn_overrides': {}, 'terrain_adaptation': adapt,
             'start_pool': f'{NS}:{pool_name}', 'size': 1, 'start_height': {'absolute': -floor}, 'project_start_to_heightmap': 'WORLD_SURFACE_WG',
             'max_distance_from_center': dist, 'use_expansion_hack': False}
        if spawns:
            d['spawn_overrides'] = {'monster': {'bounding_box': 'full', 'spawns': [{'type': f'{NS}:{m}', 'weight': 1, 'minCount': 1, 'maxCount': 2} for m in spawns]}}
        dump(f'worldgen/structure/{name}.json', d)

    def sset(name, struct, spacing, sep, salt):
        dump(f'worldgen/structure_set/{name}.json', {'structures': [{'structure': f'{NS}:{struct}', 'weight': 1}],
             'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': sep, 'salt': salt}})

    # overworld hellgates
    pool('hellgate/start', [(f'hellgate_{i}', 1) for i in range(3)])
    structure('hellgate_ruin', '#minecraft:is_overworld', 'hellgate/start', 4, adapt='beard_thin', dist=16)
    sset('hellgates', 'hellgate_ruin', 18, 7, 8841223)
    for i, f in enumerate(('ember', 'bone', 'blood', 'shadow', 'rot')):
        pool(f'{f}_citadel/start', [(f'{f}_citadel', 1)])
        structure(f'{f}_citadel', f'#{NS}:{f}_lands', f'{f}_citadel/start', 6, spawns=FACTION_MOBS[f], dist=80)
        sset(f'{f}_citadels', f'{f}_citadel', 24, 10, 5500 + i * 131)
        pool(f'{f}_outpost/start', [(f'{f}_outpost', 1)])
        structure(f'{f}_outpost', f'#{NS}:{f}_lands', f'{f}_outpost/start', 3, spawns=None, dist=40)
        sset(f'{f}_outposts', f'{f}_outpost', 9, 4, 7700 + i * 97)
        dump(f'loot_tables/chests/outpost_{f}.json', {'type': 'minecraft:chest', 'pools': [
            {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 5}, 'entries': [
                {'type': 'minecraft:item', 'name': f'{NS}:{FAC_MATERIAL[f]}', 'weight': 10, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 5}}]},
                {'type': 'minecraft:item', 'name': f'{NS}:hellsteel_nugget', 'weight': 8, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 4}}]},
                {'type': 'minecraft:item', 'name': 'minecraft:golden_apple', 'weight': 2},
                {'type': 'minecraft:item', 'name': f'{NS}:soul_ember', 'weight': 4, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}}]},
                {'type': 'minecraft:item', 'name': 'minecraft:experience_bottle', 'weight': 5, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}}]}]}]})
    pool('throne_hall/start', [('throne_hall', 1)])
    structure('throne_hall', f'#{NS}:azrakor', 'throne_hall/start', 8, spawns=None, dist=100)
    dump('worldgen/structure_set/throne_hall.json', {'structures': [{'structure': f'{NS}:throne_hall', 'weight': 1}],
         'placement': {'type': 'minecraft:concentric_rings', 'distance': 20, 'spread': 2, 'count': 1, 'preferred_biomes': f'#{NS}:azrakor'}})
    dump('tags/worldgen/structure/throne.json', {'replace': False, 'values': [f'{NS}:throne_hall']})
    dump('tags/worldgen/structure/citadels.json', {'replace': False, 'values': [f'{NS}:{f}_citadel' for f in ('ember', 'bone', 'blood', 'shadow', 'rot')]})
    dump('tags/worldgen/structure/outposts.json', {'replace': False, 'values': [f'{NS}:{f}_outpost' for f in ('ember', 'bone', 'blood', 'shadow', 'rot')]})
    dump('tags/worldgen/structure/hellgates.json', {'replace': False, 'values': [f'{NS}:hellgate_ruin']})


def build_all(preview=False):
    out = {}
    for style in ('ember', 'bone', 'blood', 'shadow', 'rot'):
        out[f'{style}_citadel'] = (citadel(style, SIG[style]), 6)
        out[f'{style}_outpost'] = (outpost(style), 3)
    for i in range(3):
        out[f'hellgate_{i}'] = (hellgate(i), 4)
    out['throne_hall'] = (throne(), 8)
    emit_worldgen({k: f for k, (v, f) in out.items()})
    for name, (v, f) in out.items():
        n = v.save(os.path.join(DATA, 'structures', f'{name}.nbt'))
        print(f'{name}: {n} blocks, size {v.size}, {len(v.entities)} entities')
        if preview:
            v.preview(f'{PREV}/{name}.png', scale=5 if v.size[0] > 40 else 8)
    return out


if __name__ == '__main__':
    build_all(preview='preview' in sys.argv)
