"""Parametric creature archetypes (skeleton + cubes) for the Crimson Realm.

Model space: vanilla units, y down, front = -z, feet at y = 24. Creatures are built at 2x scale (32 units = 1 block)
and rendered at 0.5 so their textures have twice the vanilla texel density."""
import math
import numpy as np
from core import Model, rot_zyx

R0 = np.eye(3)


def lb(M, name, parent, off, rot=(0, 0, 0)):
    """bone placed by local offset/rotation relative to its parent"""
    M.bone(name, parent, (0, 0, 0))
    M.bones[name].off = [float(v) for v in off]
    M.bones[name].rot = [float(v) for v in rot]
    return name


def plane(M, bone, frm, w, h, mat, yrot=None):
    """double sided vertical plane in the bone's local x/y"""
    return M.cube_l(bone, frm, (w, h, 0), mat, plane=True)


# ================================================================ HUMANOID
def humanoid(M, p):
    """p keys: height, leg (fraction), chest(w,h,d), waist(w,h,d), hips(w,h,d), head(w,h,d), arm_w, leg_w, arm_len, hunch, neck,
    mats: skin, armor, head, hand, boot, belt ...; flags: shoulder, gloves, boots, digitigrade, belly"""
    H = p.get('height', 64)
    leg = H * p.get('leg', 0.46)
    hip_y = 24 - leg
    cw, ch, cd = p.get('chest', (18, 14, 10))
    ww, wh, wd = p.get('waist', (13, 8, 8))
    hw, hh, hd = p.get('hips', (15, 6, 9))
    hx, hy, hz = p.get('head', (10, 11, 10))
    aw = p.get('arm_w', 5.5)
    lw = p.get('leg_w', 6.5)
    mats = p['mats']
    hunch = p.get('hunch', 0)
    M.bone('base', None, (0, hip_y, 0))
    M.bone('hips', 'base', (0, hip_y, 0))
    M.cube('hips', (-hw / 2, hip_y - hh + 1, -hd / 2), (hw, hh, hd), mats.get('hips', mats['armor']))
    M.bone('waist', 'hips', (0, hip_y - hh + 1, 0), (hunch * 0.4, 0, 0))
    wy = hip_y - hh + 1
    M.cube('waist', (-ww / 2, wy - wh, -wd / 2), (ww, wh, wd), mats.get('waist', mats['skin']))
    cy = wy - wh
    M.bone('chest', 'waist', (0, cy, 0), (hunch * 0.6, 0, 0))
    M.cube('chest', (-cw / 2, cy - ch, -cd / 2), (cw, ch, cd), mats.get('chest', mats['armor']))
    if p.get('belly'):
        M.cube('chest', (-cw / 2 + 2, cy - ch * 0.45, -cd / 2 - 1.5), (cw - 4, ch * 0.5, 2), mats.get('belly', mats['skin']))
    top = cy - ch
    neck = p.get('neck', 3)
    M.bone('neck', 'chest', (0, top, -cd * 0.08))
    M.cube('neck', (-hx * 0.3, top - neck - 1, -hz * 0.3), (hx * 0.6, neck + 2, hz * 0.6), mats.get('neck', mats['skin']))
    M.bone('head', 'neck', (0, top - neck, -cd * 0.08), (-hunch * 0.8, 0, 0))
    hc = M.cube('head', (-hx / 2, top - neck - hy, -hz * 0.6), (hx, hy, hz), mats['head'])
    if p.get('snout'):
        sl, sw, sh = p['snout']
        M.cube('head', (-sw / 2, top - neck - sh - 1, -hz * 0.6 - sl), (sw, sh, sl), mats.get('snout', mats['skin']))
    if p.get('jaw'):
        lb(M, 'jaw', 'head', (0, -2.5, -hz * 0.25), (8, 0, 0))
        jw = p['jaw']
        M.cube_l('jaw', (-jw[0] / 2, 0, -jw[2]), jw, mats.get('jaw', mats['skin']))
    # arms
    alen = p.get('arm_len', H * 0.42)
    spread = p.get('arm_spread', 1.0)
    for side, sx in (('right', -1), ('left', 1)):
        A = (sx * (cw / 2 + aw * 0.35), top + aw * 0.6, 0)
        B = (sx * (cw / 2 + aw * 0.55 + 2 * spread), top + aw * 0.6 + alen * 0.5, 1.5)
        C = (sx * (cw / 2 + aw * 0.45 + 2.6 * spread), top + aw * 0.6 + alen, -2.5)
        M.seg(f'{side}_arm', 'chest', A, B, aw, aw, mats.get('arm', mats['skin']), extend=aw * 0.3)
        M.seg(f'{side}_fore', f'{side}_arm', B, C, aw * 0.9, aw * 0.9, mats.get('fore', mats.get('arm', mats['skin'])), extend=aw * 0.25)
        M.bone_w(f'{side}_hand', f'{side}_fore', C, R0)
        M.cube_l(f'{side}_hand', (-aw * 0.5, 0, -aw * 0.5), (aw, aw * 0.9, aw), mats.get('hand', mats['skin']))
        if p.get('shoulder'):
            sw_ = p['shoulder']
            M.cube_l(f'{side}_arm', (-sw_ / 2, -aw * 0.55, -sw_ / 2), (sw_, aw * 1.1, sw_), mats.get('shoulder', mats['armor']), inflate=0.2)
        if p.get('claws'):
            for k in range(3):
                M.cube_l(f'{side}_hand', (-aw * 0.45 + k * aw * 0.33, aw * 0.8, -aw * 0.45), (aw * 0.22, p['claws'], aw * 0.3), mats.get('claw', 'claw'))
    # legs
    for side, sx in (('right', -1), ('left', 1)):
        A = (sx * hw * 0.27, hip_y, 0)
        if p.get('digitigrade'):
            B = (sx * hw * 0.32, hip_y + leg * 0.45, -3.5)
            C = (sx * hw * 0.32, hip_y + leg * 0.8, 3)
            D = (sx * hw * 0.32, 24 - 2.5, 1)
            M.seg(f'{side}_leg', 'hips', A, B, lw, lw * 1.1, mats.get('leg', mats['skin']), extend=lw * 0.3)
            M.seg(f'{side}_shin', f'{side}_leg', B, C, lw * 0.8, lw * 0.8, mats.get('shin', mats.get('leg', mats['skin'])), extend=lw * 0.2)
            M.seg(f'{side}_meta', f'{side}_shin', C, D, lw * 0.6, lw * 0.6, mats.get('shin', mats.get('leg', mats['skin'])), extend=1)
            M.bone_w(f'{side}_foot', f'{side}_meta', D, R0)
        else:
            B = (sx * hw * 0.29, hip_y + leg * 0.5, -1)
            C = (sx * hw * 0.29, 24 - 2.5, 0.5)
            M.seg(f'{side}_leg', 'hips', A, B, lw, lw * 1.05, mats.get('leg', mats['skin']), extend=lw * 0.3)
            M.seg(f'{side}_shin', f'{side}_leg', B, C, lw * 0.88, lw * 0.9, mats.get('shin', mats.get('leg', mats['skin'])), extend=lw * 0.2)
            M.bone_w(f'{side}_foot', f'{side}_shin', C, R0)
        M.cube_l(f'{side}_foot', (-lw * 0.55, 0, -lw * 1.05), (lw * 1.1, 2.5, lw * 1.6), mats.get('boot', mats.get('leg', mats['skin'])))
    return dict(top=top, neck_y=top - neck, head_top=top - neck - hy, cw=cw, cd=cd, hip_y=hip_y, hx=hx, hy=hy, hz=hz, aw=aw, alen=alen, chest_y=cy)


HUMAN_LIMBS = ['base', 'hips', 'waist', 'chest', 'neck', 'head', 'right_arm', 'right_fore', 'right_hand', 'left_arm', 'left_fore', 'left_hand',
               'right_leg', 'right_shin', 'right_foot', 'left_leg', 'left_shin', 'left_foot']


# ---------------------------------------------------------------- humanoid accessories
def horns(M, g, kind, mat, size=1.0):
    top = g['head_top']
    hx = g['hx']
    for sx in (-1, 1):
        n = f'horn_{"r" if sx < 0 else "l"}'
        if kind == 'ram':
            lb(M, n, 'head', (sx * hx * 0.45, -g['hy'] + 2, -1), (0, 0, 0))
            M.cube_l(n, (-1.5, -2, -1.5), (3, 3.5 * size, 3), mat)
            lb(M, n + 'b', n, (sx * 1.5, -1.5, 1.5), (40, 0, sx * -70))
            M.cube_l(n + 'b', (-1.2, -6 * size, -1.2), (2.4, 6 * size, 2.4), mat)
            lb(M, n + 'c', n + 'b', (0, -6 * size, 0), (70, 0, 0))
            M.cube_l(n + 'c', (-0.9, -5 * size, -0.9), (1.8, 5 * size, 1.8), mat)
        elif kind == 'long':
            lb(M, n, 'head', (sx * hx * 0.4, -g['hy'] + 1, -1), (-25, 0, sx * -25))
            M.cube_l(n, (-1.4, -7 * size, -1.4), (2.8, 7 * size, 2.8), mat)
            lb(M, n + 'b', n, (0, -7 * size, 0), (-25, 0, sx * 15))
            M.cube_l(n + 'b', (-1.0, -7 * size, -1.0), (2.0, 7 * size, 2.0), mat)
            lb(M, n + 'c', n + 'b', (0, -7 * size, 0), (-20, 0, sx * 10))
            M.cube_l(n + 'c', (-0.6, -5 * size, -0.6), (1.2, 5 * size, 1.2), mat)
        elif kind == 'side':
            lb(M, n, 'head', (sx * hx * 0.5, -g['hy'] * 0.6, -1), (0, 0, sx * -80))
            M.cube_l(n, (-1.4, -5 * size, -1.4), (2.8, 5 * size, 2.8), mat)
            lb(M, n + 'b', n, (0, -5 * size, 0), (0, 0, sx * 70))
            M.cube_l(n + 'b', (-1.0, -6 * size, -1.0), (2.0, 6 * size, 2.0), mat)
        elif kind == 'short':
            lb(M, n, 'head', (sx * hx * 0.32, -g['hy'] + 1, -2), (-20, 0, sx * -15))
            M.cube_l(n, (-1.1, -4 * size, -1.1), (2.2, 4 * size, 2.2), mat)


def crown(M, g, mat, gem=None, spikes=5, h=4):
    hx, top = g['hx'], -g['hy']
    lb(M, 'crown', 'head', (0, top, -g['hz'] * 0.1))
    M.cube_l('crown', (-hx / 2 - 0.6, -1.5, -g['hz'] / 2 - 0.6), (hx + 1.2, 2, g['hz'] + 1.2), mat)
    for k in range(spikes):
        x = -hx / 2 + (k + 0.5) * hx / spikes - 0.6
        hh = h * (1.0 if k % 2 == 0 else 0.65)
        M.cube_l('crown', (x, -1.5 - hh, -g['hz'] / 2 - 0.6), (1.2, hh, 1.2), mat)
        if gem and k % 2 == 0:
            M.cube_l('crown', (x - 0.1, -1.4, -g['hz'] / 2 - 1.0), (1.4, 1.2, 0.5), gem)


def helmet(M, g, mat, crest=None, face=None):
    hx, hy, hz = g['hx'], g['hy'], g['hz']
    M.cube('head', (-hx / 2, g['neck_y'] - hy, -hz * 0.6), (hx, hy * 0.55, hz), mat, inflate=0.7)
    if crest:
        M.cube('head', (-0.8, g['neck_y'] - hy - 3.5, -hz * 0.55), (1.6, 3.5, hz * 1.05), crest)


def hood(M, g, mat):
    """hood mats should be wrapped in mats.open_front so the face shows"""
    hx, hy, hz = g['hx'], g['hy'], g['hz']
    M.cube('head', (-hx / 2, g['neck_y'] - hy, -hz * 0.6), (hx, hy, hz), mat, inflate=0.9)
    M.cube('head', (-hx * 0.35, g['neck_y'] - hy - 1.5, hz * 0.1), (hx * 0.7, 3, hz * 0.45), mat)


def cape(M, g, mat, length=None, width=None):
    top = g['top']
    L = length or (24 - top) * 0.92
    W = width or g['cw'] * 0.95
    lb(M, 'cape', 'chest', (0, top - g['chest_y'] + 1, g['cd'] / 2 + 0.6), (6, 0, 0))
    M.cube_l('cape', (-W / 2, 0, 0), (W, L, 0), mat, plane=True)


def robe(M, g, mat, length=None, flare=8):
    hip = g['hip_y'] - 4
    L = length or (24 - hip) + 0.5
    cw = g['cw']
    lb(M, 'skirt_f', 'hips', (0, -3, -5.2), (-flare * 0.6, 0, 0))
    M.cube_l('skirt_f', (-cw * 0.45, 0, 0), (cw * 0.9, L, 0), mat, plane=True)
    lb(M, 'skirt_b', 'hips', (0, -3, 5.2), (flare * 0.8, 0, 0))
    M.cube_l('skirt_b', (-cw * 0.45, 0, 0), (cw * 0.9, L, 0), mat, plane=True)
    for sx, n in ((-1, 'skirt_r'), (1, 'skirt_l')):
        lb(M, n, 'hips', (sx * cw * 0.42, -3, 0), (0, 90, sx * -flare))
        M.cube_l(n, (-5.5, 0, 0), (11, L, 0), mat, plane=True)


def wings(M, g, mat_bone, mat_mem, span=26, kind='bat'):
    top = g['top']
    for sx, s in ((-1, 'r'), (1, 'l')):
        lb(M, f'wing_{s}', 'chest', (sx * 3, top - g['chest_y'] + 3, g['cd'] / 2), (10, sx * -25, sx * -20))
        M.cube_l(f'wing_{s}', (-1, -1, -1) if sx > 0 else (-1, -1, -1), (2, 2, 2), mat_bone)
        lb(M, f'wing_{s}1', f'wing_{s}', (0, 0, 0), (0, 0, sx * -60))
        M.cube_l(f'wing_{s}1', (-1, -span * 0.5, -1), (2, span * 0.5, 2), mat_bone)
        lb(M, f'wing_{s}2', f'wing_{s}1', (0, -span * 0.5, 0), (0, 0, sx * 70))
        M.cube_l(f'wing_{s}2', (-0.8, -span * 0.55, -0.8), (1.6, span * 0.55, 1.6), mat_bone)
        # membranes: plane spanning from body to the wing bones (local plane faces z)
        M.cube_l(f'wing_{s}1', (-span * 0.45 if sx > 0 else 0, -span * 0.5, 0), (span * 0.45, span * 0.5, 0), mat_mem, plane=True, feat={'wing': 'l' if sx > 0 else 'r'})
        M.cube_l(f'wing_{s}2', (-span * 0.5 if sx > 0 else 0, -span * 0.55, 0), (span * 0.5, span * 0.55, 0), mat_mem, plane=True, feat={'wing': 'l' if sx > 0 else 'r'})


def tail(M, parent, start, segs, mat, length=8.0, w=4.0, droop=25, prefix='tail'):
    names = []
    prev = parent
    for i in range(segs):
        n = f'{prefix}{i}'
        if i == 0:
            M.bone(n, prev, start, (-droop, 0, 0))
        else:
            lb(M, n, prev, (0, 0, length), (-8, 0, 0))
        ww = w * (1 - i / (segs + 1))
        M.cube_l(n, (-ww / 2, -ww / 2, 0), (ww, ww, length + 0.5), mat)
        names.append(n)
        prev = n
    return names


def back_spikes(M, bone, y0, z, n, mat, h=5, spread=0.0):
    for i in range(n):
        nm = f'spike{i}'
        lb(M, nm, bone, ((i - (n - 1) / 2) * spread, y0, z), (-35 - i * 3, 0, (i - (n - 1) / 2) * 12))
        M.cube_l(nm, (-1, -h, -1), (2, h, 2), mat)


# ---------------------------------------------------------------- weapons (in the right / left hand bones)
def weapon(M, hand, kind, mats, size=1.0, flip=False):
    """built along the hand's local -z (forward) for thrusting weapons or -y (up) for swords"""
    s = size
    w = f'{hand}_weapon'
    upright = kind in ('staff', 'spear', 'halberd', 'scythe', 'bow')
    lb(M, w, hand, (0, 4, -1), (180, 0, 0) if upright else (-62, 0, 0))   # local +y: up for poles, forward-down for blades
    blade, grip, guard = mats.get('blade', 'blade'), mats.get('grip', 'grip'), mats.get('guard', 'guard')
    if kind == 'sword':
        M.cube_l(w, (-0.9, -5 * s, -0.9), (1.8, 7 * s, 1.8), grip)
        M.cube_l(w, (-3.5 * s, 1.5, -1.2), (7 * s, 1.6, 2.4), guard)
        M.cube_l(w, (-1.6 * s, 3, -0.5), (3.2 * s, 22 * s, 1.0), blade)
        M.cube_l(w, (-0.9 * s, 3 + 22 * s, -0.4), (1.8 * s, 3 * s, 0.8), blade)
    elif kind == 'greatsword':
        M.cube_l(w, (-1.1, -9 * s, -1.1), (2.2, 11 * s, 2.2), grip)
        M.cube_l(w, (-5 * s, 1.5, -1.5), (10 * s, 2.2, 3), guard)
        M.cube_l(w, (-2.6 * s, 3.5, -0.7), (5.2 * s, 32 * s, 1.4), blade)
        M.cube_l(w, (-1.6 * s, 3.5 + 32 * s, -0.6), (3.2 * s, 4 * s, 1.2), blade)
    elif kind == 'spear':
        M.cube_l(w, (-0.8, -14 * s, -0.8), (1.6, 40 * s, 1.6), grip)
        M.cube_l(w, (-1.8 * s, 26 * s, -0.5), (3.6 * s, 7 * s, 1.0), blade)
        M.cube_l(w, (-1.0 * s, 33 * s, -0.4), (2.0 * s, 3 * s, 0.8), blade)
    elif kind == 'halberd':
        M.cube_l(w, (-0.8, -14 * s, -0.8), (1.6, 42 * s, 1.6), grip)
        M.cube_l(w, (0.5, 22 * s, -0.5), (7 * s, 8 * s, 1.0), blade)
        M.cube_l(w, (-1.0 * s, 28 * s, -0.4), (2.0 * s, 6 * s, 0.8), blade)
    elif kind == 'hammer':
        M.cube_l(w, (-1.0, -8 * s, -1.0), (2.0, 32 * s, 2.0), grip)
        M.cube_l(w, (-6 * s, 20 * s, -4 * s), (12 * s, 8 * s, 8 * s), guard)
        M.cube_l(w, (-4 * s, 22 * s, -4.6 * s), (8 * s, 4 * s, 9.2 * s), blade)
    elif kind == 'axe':
        M.cube_l(w, (-0.9, -6 * s, -0.9), (1.8, 26 * s, 1.8), grip)
        M.cube_l(w, (0.6, 13 * s, -0.6), (8 * s, 9 * s, 1.2), blade)
        M.cube_l(w, (-3 * s, 15 * s, -0.6), (3.6 * s, 5 * s, 1.2), blade)
    elif kind == 'staff':
        M.cube_l(w, (-0.9, -18 * s, -0.9), (1.8, 40 * s, 1.8), grip)
        M.cube_l(w, (-2.5 * s, 21 * s, -2.5 * s), (5 * s, 1.5, 5 * s), guard)
        M.cube_l(w, (-2 * s, 22.5 * s, -2 * s), (4 * s, 4 * s, 4 * s), mats.get('orb', 'orb'))
        for k in range(3):
            lb(M, f'{w}_prong{k}', w, (0, 22 * s, 0), (0, k * 120, 25))
            M.cube_l(f'{w}_prong{k}', (-0.4, -1, -0.4), (0.8, 6 * s, 0.8), guard)
    elif kind == 'scythe':
        M.cube_l(w, (-0.9, -16 * s, -0.9), (1.8, 42 * s, 1.8), grip)
        lb(M, f'{w}_blade', w, (0, 25 * s, 0), (0, 0, 0))
        M.cube_l(f'{w}_blade', (-14 * s, -1.5, -0.5), (14 * s, 3.5, 1.0), blade)
        M.cube_l(f'{w}_blade', (-17 * s, -0.5, -0.4), (4 * s, 2.5, 0.8), blade)
    elif kind == 'bow':
        M.cube_l(w, (-0.7, -14 * s, -0.7), (1.4, 28 * s, 1.4), grip)
        M.cube_l(w, (-0.2, -13 * s, 1.5), (0.4, 26 * s, 0.4), mats.get('string', 'string'))
    elif kind == 'claws':
        for k in range(3):
            M.cube_l(w, (-1.8 + k * 1.6, 0, -0.4), (0.8, 9 * s, 0.8), blade)
    elif kind == 'dagger':
        M.cube_l(w, (-0.7, -3 * s, -0.7), (1.4, 4 * s, 1.4), grip)
        M.cube_l(w, (-2 * s, 1, -0.8), (4 * s, 1, 1.6), guard)
        M.cube_l(w, (-1.0 * s, 2, -0.4), (2.0 * s, 10 * s, 0.8), blade)
    elif kind == 'shield':
        lb(M, w + 's', hand, (2.2 if not flip else -2.2, 1, -1), (0, 90 if not flip else -90, 0))
        M.cube_l(w + 's', (-7 * s, -9 * s, -0.6), (14 * s, 18 * s, 1.6), guard)
        M.cube_l(w + 's', (-2.5 * s, -3 * s, -1.4), (5 * s, 6 * s, 1.0), blade)
    elif kind == 'bell':
        lb(M, w + 'b', hand, (0, 6, 0))
        M.cube_l(w + 'b', (-0.4, 0, -0.4), (0.8, 5, 0.8), grip)
        M.cube_l(w + 'b', (-5 * s, 5, -5 * s), (10 * s, 10 * s, 10 * s), guard)
        M.cube_l(w + 'b', (-6 * s, 13 * s, -6 * s), (12 * s, 2, 12 * s), guard)
    elif kind == 'chain':
        for k in range(5):
            M.cube_l(w, (-1.2, k * 3.2, -0.6), (2.4, 2.4, 1.2) if k % 2 == 0 else (1.2, 2.4, 2.4), grip)
        M.cube_l(w, (-2.5, 16, -2.5), (5, 5, 5), blade)
    elif kind == 'orb':
        lb(M, w + 'o', hand, (0, 1, -3))
        M.cube_l(w + 'o', (-2.5, -2.5, -2.5), (5, 5, 5), mats.get('orb', 'orb'))
    elif kind == 'lantern':
        lb(M, w + 'l', hand, (0, 5, 0))
        M.cube_l(w + 'l', (-0.4, 0, -0.4), (0.8, 4, 0.8), grip)
        M.cube_l(w + 'l', (-3, 4, -3), (6, 7, 6), guard)
        M.cube_l(w + 'l', (-2, 5, -2), (4, 5, 4), mats.get('orb', 'orb'), inflate=0.3)
    return w


# ================================================================ QUADRUPED
def quadruped(M, p):
    """p: length, height (shoulder), girth (w,h), neck (len, angle), head (w,h,d), snout (l,w,h), leg_w, tail(segs,len,w), mats"""
    L = p.get('length', 34)
    sh = p.get('height', 22)
    bw, bh = p.get('girth', (16, 14))
    mats = p['mats']
    top = 24 - sh - bh * 0.5
    M.bone('base', None, (0, top + bh / 2, 0))
    M.bone('body', 'base', (0, top + bh / 2, 0))
    M.cube('body', (-bw / 2, top, -2), (bw, bh, L * 0.5 + 2), mats.get('hind', mats['body']))
    M.bone('chest', 'body', (0, top + bh / 2, -2))
    M.cube('chest', (-bw * 0.55, top - 1.5, -L * 0.5), (bw * 1.1, bh + 2, L * 0.5 + 1), mats.get('chest', mats['body']))
    nl, na = p.get('neck', (10, -35))
    M.bone('neck', 'chest', (0, top + 2, -L * 0.5 + 2), (na, 0, 0))
    nw = bw * 0.55
    M.cube_l('neck', (-nw / 2, -nw / 2, -nl), (nw, nw, nl + 2), mats.get('neck', mats['body']))
    hw_, hh_, hd_ = p.get('head', (11, 10, 11))
    lb(M, 'head', 'neck', (0, 0, -nl), (-na - 5, 0, 0))
    M.cube_l('head', (-hw_ / 2, -hh_ / 2 - 1, -hd_ * 0.8), (hw_, hh_, hd_), mats['head'])
    sl, sw, shh = p.get('snout', (7, 7, 5))
    M.cube_l('head', (-sw / 2, -1, -hd_ * 0.8 - sl), (sw, shh, sl + 0.5), mats.get('snout', mats['body']))
    lb(M, 'jaw', 'head', (0, shh - 1, -hd_ * 0.5), (6, 0, 0))
    M.cube_l('jaw', (-sw * 0.45, 0, -sl - hd_ * 0.3), (sw * 0.9, 2.5, sl + hd_ * 0.3), mats.get('jaw', mats.get('snout', mats['body'])))
    lw = p.get('leg_w', 6)
    legs = {}
    for name, sx, z, front in (('fr', -1, -L * 0.36, True), ('fl', 1, -L * 0.36, True), ('br', -1, L * 0.33, False), ('bl', 1, L * 0.33, False)):
        parent = 'chest' if front else 'body'
        A = (sx * bw * 0.38, top + bh * 0.55, z)
        if p.get('stubby'):
            B = (sx * bw * 0.46, 24 - sh * 0.45, z - 1)
        else:
            B = (sx * bw * 0.42, 24 - sh * 0.48, z + (2.5 if front else -3))
        C = (sx * bw * 0.42, 24 - 2, z + (0 if front else 1))
        M.seg(f'{name}_leg', parent, A, B, lw * (1.1 if not front else 1.0), lw * 1.15, mats.get('leg', mats['body']), extend=lw * 0.3)
        M.seg(f'{name}_shin', f'{name}_leg', B, C, lw * 0.8, lw * 0.8, mats.get('shin', mats.get('leg', mats['body'])), extend=1)
        M.bone_w(f'{name}_foot', f'{name}_shin', C, R0)
        M.cube_l(f'{name}_foot', (-lw * 0.6, 0, -lw * 0.9), (lw * 1.2, 2, lw * 1.4), mats.get('foot', mats.get('leg', mats['body'])))
        legs[name] = True
    tsegs, tl, tw = p.get('tail', (3, 7, 3.5))
    tnames = tail(M, 'body', (0, top + 2, L * 0.5), tsegs, mats.get('tail', mats['body']), tl, tw, droop=p.get('tail_droop', 25)) if tsegs else []
    return dict(top=top, L=L, bw=bw, bh=bh, hw=hw_, hh=hh_, hd=hd_, tails=tnames)


QUAD_LEGS = ['fr_leg', 'fr_shin', 'fl_leg', 'fl_shin', 'br_leg', 'br_shin', 'bl_leg', 'bl_shin']


# ================================================================ ARTHROPOD
def arthropod(M, p):
    """p: body (w,h,l), abdomen (w,h,l), head (w,h,d), pairs, leg_len, leg_w, mandibles, pincers, stinger, mats"""
    mats = p['mats']
    bw, bh, bl = p.get('body', (14, 7, 12))
    aw, ah, al = p.get('abdomen', (16, 11, 16))
    hw, hh, hd = p.get('head', (9, 6, 7))
    ll = p.get('leg_len', 14)
    lift = p.get('lift', max(2.0, 0.46 * ll - bh / 2))
    y0 = 24 - lift - bh
    M.bone('base', None, (0, y0 + bh / 2, 0))
    M.bone('body', 'base', (0, y0 + bh / 2, 0))
    M.cube('body', (-bw / 2, y0, -bl / 2), (bw, bh, bl), mats['body'])
    lb(M, 'head', 'body', (0, 0, -bl / 2), (0, 0, 0))
    M.cube_l('head', (-hw / 2, -hh / 2 - 1, -hd), (hw, hh, hd), mats['head'])
    if p.get('mandibles'):
        for sx, s in ((-1, 'r'), (1, 'l')):
            lb(M, f'mand_{s}', 'head', (sx * hw * 0.3, 1, -hd), (0, sx * 20, 0))
            M.cube_l(f'mand_{s}', (-0.8, -0.8, -p['mandibles']), (1.6, 1.6, p['mandibles']), mats.get('claw', 'claw'))
    if al > 0:
        lb(M, 'abdomen', 'body', (0, -1, bl / 2 - 1), (p.get('abd_tilt', -12), 0, 0))
        M.cube_l('abdomen', (-aw / 2, -ah / 2, 0), (aw, ah, al), mats.get('abdomen', mats['body']))
    pairs = p.get('pairs', 3)
    ll = p.get('leg_len', 14)
    lw = p.get('leg_w', 2.4)
    legn = []
    for i in range(pairs):
        z = -bl / 2 + 2 + i * (bl - 4) / max(pairs - 1, 1)
        for sx, s in ((-1, 'r'), (1, 'l')):
            n = f'leg{i}{s}'
            yaw = (i - (pairs - 1) / 2) * 22 * -1
            lb(M, n, 'body', (sx * bw / 2, 0, z), (0, yaw * sx, sx * 25))
            M.cube_l(n, (-lw / 2, -lw / 2, -lw / 2), (lw, lw, lw), mats.get('leg', mats['body']))
            # femur rises outward, the tibia bends down to the ground
            lb(M, n + 'a', n, (0, 0, 0), (0, 0, sx * 45))
            M.cube_l(n + 'a', (-lw / 2, -ll * 0.55, -lw / 2), (lw, ll * 0.55, lw), mats.get('leg', mats['body']))
            lb(M, n + 'b', n + 'a', (0, -ll * 0.55, 0), (0, 0, sx * 80))
            M.cube_l(n + 'b', (-lw * 0.4, -ll * 0.75, -lw * 0.4), (lw * 0.8, ll * 0.75, lw * 0.8), mats.get('leg_tip', mats.get('leg', mats['body'])))
            legn.append((n, sx))
    if p.get('pincers'):
        for sx, s in ((-1, 'r'), (1, 'l')):
            n = f'pincer_{s}'
            lb(M, n, 'body', (sx * bw * 0.4, 1, -bl / 2), (-10, sx * 35, 0))
            M.cube_l(n, (-1.5, -1.5, -8), (3, 3, 8), mats.get('claw_arm', mats['body']))
            lb(M, n + 'c', n, (0, 0, -8), (0, sx * -45, 0))
            M.cube_l(n + 'c', (-2.5, -2, -6), (5, 4, 6), mats.get('claw_arm', mats['body']))
            lb(M, n + 'f', n + 'c', (sx * 1.5, 0, -6), (0, sx * 15, 0))
            M.cube_l(n + 'f', (-0.8, -1, -5), (1.6, 2, 5), mats.get('claw', 'claw'))
    if p.get('stinger'):
        segs = p['stinger']
        prev = 'abdomen' if al > 0 else 'body'
        z = al if al > 0 else bl / 2
        for i in range(segs):
            n = f'sting{i}'
            lb(M, n, prev, (0, -1, z if i == 0 else 5), (38 if i else 55, 0, 0))
            w_ = 4.5 - i * 0.5
            M.cube_l(n, (-w_ / 2, -w_ / 2, 0), (w_, w_, 5.5), mats.get('tail', mats['body']))
            prev = n
            z = 5
        lb(M, 'stinger', prev, (0, 0, 5), (45, 0, 0))
        M.cube_l('stinger', (-1.5, -1.5, 0), (3, 3, 3), mats.get('tail', mats['body']))
        M.cube_l('stinger', (-0.5, -0.5, 3), (1, 1, 4), mats.get('claw', 'claw'))
    return dict(legs=legn, pairs=pairs)


# ================================================================ FLOATER (spirit / djinn / wraith)
def floater(M, p):
    """upper body + head + arms; the lower body is a tapering tail of wisps; floats above the ground"""
    mats = p['mats']
    hover = p.get('hover', 8)
    cw, ch, cd = p.get('chest', (14, 12, 8))
    hx, hy, hz = p.get('head', (9, 10, 9))
    aw = p.get('arm_w', 4.5)
    tail_len = p.get('tail_len', 22)
    base_y = 24 - hover - tail_len
    M.bone('base', None, (0, base_y, 0))
    M.bone('chest', 'base', (0, base_y, 0))
    M.cube('chest', (-cw / 2, base_y - ch, -cd / 2), (cw, ch, cd), mats['chest'])
    top = base_y - ch
    M.bone('head', 'chest', (0, top, 0))
    M.cube('head', (-hx / 2, top - hy, -hz * 0.6), (hx, hy, hz), mats['head'])
    alen = p.get('arm_len', 20)
    for side, sx in (('right', -1), ('left', 1)):
        A = (sx * (cw / 2 + aw * 0.3), top + aw * 0.5, 0)
        B = (sx * (cw / 2 + aw * 0.6 + 2), top + aw * 0.5 + alen * 0.5, -1)
        C = (sx * (cw / 2 + aw * 0.6 + 2.5), top + aw * 0.5 + alen, -4)
        M.seg(f'{side}_arm', 'chest', A, B, aw, aw, mats.get('arm', mats['chest']), extend=1)
        M.seg(f'{side}_fore', f'{side}_arm', B, C, aw * 0.9, aw * 0.9, mats.get('arm', mats['chest']), extend=1)
        M.bone_w(f'{side}_hand', f'{side}_fore', C, R0)
        M.cube_l(f'{side}_hand', (-aw * 0.5, 0, -aw * 0.5), (aw, aw, aw), mats.get('hand', mats.get('arm', mats['chest'])))
    # tail of wisps
    segs = p.get('tail_segs', 3)
    prev = 'chest'
    w = cw * 0.8
    seg_len = tail_len / segs
    for i in range(segs):
        n = f'wisp{i}'
        lb(M, n, prev, (0, 0 if i == 0 else seg_len, 0), (8 if i else 0, 0, 0))
        ww = w * (1 - i / (segs + 0.5))
        M.cube_l(n, (-ww / 2, 0, -ww * 0.3), (ww, seg_len + 1, ww * 0.6), mats.get('tail', mats['chest']))
        prev = n
    if p.get('flames'):
        for k, yr in enumerate((0, 90)):
            lb(M, f'flame{k}', 'chest', (0, tail_len + 2, 0), (180, yr + 45, 0))
            M.cube_l(f'flame{k}', (-w * 0.7, -2, 0), (w * 1.4, tail_len + hover * 0.6, 0), mats.get('flame', 'flame'), plane=True)
    return dict(top=top, neck_y=top, head_top=top - hy, cw=cw, cd=cd, hx=hx, hy=hy, hz=hz, hip_y=base_y, aw=aw, chest_y=base_y)


FLOAT_LIMBS = ['base', 'chest', 'head', 'right_arm', 'right_fore', 'right_hand', 'left_arm', 'left_fore', 'left_hand', 'wisp0', 'wisp1', 'wisp2']


# ================================================================ FLYER (bat / gargoyle / skull)
def flyer(M, p):
    mats = p['mats']
    bw, bh, bl = p.get('body', (9, 8, 13))
    hx, hy, hz = p.get('head', (8, 7, 8))
    hover = p.get('hover', 14)
    y = 24 - hover - bh
    M.bone('base', None, (0, y + bh / 2, 0))
    M.bone('body', 'base', (0, y + bh / 2, 0))
    M.cube('body', (-bw / 2, y, -bl / 2), (bw, bh, bl), mats['body'])
    lb(M, 'head', 'body', (0, -1, -bl / 2))
    M.cube_l('head', (-hx / 2, -hy / 2 - 1, -hz), (hx, hy, hz), mats['head'])
    if p.get('ears'):
        for sx in (-1, 1):
            n = f'ear_{"r" if sx < 0 else "l"}'
            lb(M, n, 'head', (sx * hx * 0.32, -hy / 2 - 1, -hz * 0.5), (-10, 0, sx * -15))
            M.cube_l(n, (-1.5, -p['ears'], -0.5), (3, p['ears'], 1), mats.get('ear', mats['head']))
    span = p.get('span', 22)
    for sx, s in ((-1, 'r'), (1, 'l')):
        lb(M, f'wing_{s}', 'body', (sx * bw / 2, -bh * 0.3, -bl * 0.2), (0, 0, sx * -22))
        M.cube_l(f'wing_{s}', (0 if sx > 0 else -span * 0.5, -1, -1), (span * 0.5, 2, 2), mats.get('wbone', mats['body']))
        M.cube_l(f'wing_{s}', (0 if sx > 0 else -span * 0.5, 0, 0), (span * 0.5, 0.01, bl * 0.9), mats['membrane'], plane=False)
        lb(M, f'wing_{s}2', f'wing_{s}', (sx * span * 0.5, 0, 0), (0, 0, sx * 18))
        M.cube_l(f'wing_{s}2', (0 if sx > 0 else -span * 0.55, -0.8, -0.8), (span * 0.55, 1.6, 1.6), mats.get('wbone', mats['body']))
        M.cube_l(f'wing_{s}2', (0 if sx > 0 else -span * 0.55, 0, 0), (span * 0.55, 0.01, bl * 0.75), mats['membrane'], plane=False)
    if p.get('legs', True):
        for sx, s in ((-1, 'r'), (1, 'l')):
            lb(M, f'leg_{s}', 'body', (sx * bw * 0.3, bh / 2, bl * 0.25), (30, 0, 0))
            M.cube_l(f'leg_{s}', (-1, 0, -1), (2, 6, 2), mats.get('leg', mats['body']))
            M.cube_l(f'leg_{s}', (-1.5, 5, -2.5), (3, 1.5, 3.5), mats.get('claw', 'claw'))
    if p.get('tail'):
        tail(M, 'body', (0, y + 2, bl / 2), p['tail'], mats.get('tail', mats['body']), 6, 2.5, droop=15)
    return dict(span=span)


# ================================================================ EYE ORB / SKULL
def orb(M, p):
    mats = p['mats']
    r = p.get('radius', 8)
    hover = p.get('hover', 16)
    cy = 24 - hover - r
    M.bone('base', None, (0, cy, 0))
    M.bone('body', 'base', (0, cy, 0))
    # rounded sphere from three crossing boxes
    M.cube('body', (-r, cy - r * 0.7, -r * 0.7), (2 * r, r * 1.4, r * 1.4), mats['body'])
    M.cube('body', (-r * 0.7, cy - r, -r * 0.7), (r * 1.4, 2 * r, r * 1.4), mats['body'])
    M.cube('body', (-r * 0.7, cy - r * 0.7, -r), (r * 1.4, r * 1.4, 2 * r), mats['eye'])
    lb(M, 'lid', 'body', (0, -r * 0.65, -r * 0.7))
    M.cube_l('lid', (-r * 0.75, -0.5, -r * 0.35), (r * 1.5, 2, r * 0.8), mats.get('lid', mats['body']))
    n_t = p.get('tentacles', 6)
    t_len = p.get('t_len', 6)
    names = []
    for k in range(n_t):
        ang = k / n_t * math.tau
        bx, bz = math.cos(ang) * r * 0.55, math.sin(ang) * r * 0.55 + r * 0.2
        prev = 'body'
        for i in range(3):
            n = f't{k}_{i}'
            if i == 0:
                lb(M, n, prev, (bx, r * 0.6, bz), (math.cos(ang) * -25 + 10, 0, math.sin(ang) * 25))
            else:
                lb(M, n, prev, (0, t_len, 0), (8, 0, 0))
            w = 2.4 - i * 0.6
            M.cube_l(n, (-w / 2, 0, -w / 2), (w, t_len + 0.5, w), mats.get('tentacle', mats['body']))
            prev = n
        names.append(k)
    if p.get('spikes'):
        for k in range(p['spikes']):
            a = k / p['spikes'] * math.tau
            n = f'sp{k}'
            lb(M, n, 'body', (math.cos(a) * r * 0.6, -r * 0.4, math.sin(a) * r * 0.6), (math.sin(a) * -40, 0, math.cos(a) * 40))
            M.cube_l(n, (-1, -6, -1), (2, 6, 2), mats.get('spike', mats['body']))
    return dict(r=r, tentacles=n_t)


# ================================================================ BLOB
def blob(M, p):
    mats = p['mats']
    w, h, d = p.get('size', (26, 22, 24))
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', (0, 24, 0))
    M.cube('body', (-w / 2, 24 - h, -d / 2), (w, h, d), mats['body'])
    M.cube('body', (-w * 0.4, 24 - h - 5, -d * 0.35), (w * 0.8, 6, d * 0.7), mats['body'])
    rs = np.random.default_rng(p.get('seed', 3))
    for k in range(p.get('lumps', 5)):
        lw = 6 + rs.random() * 7
        a_ = rs.random() * math.tau
        x = math.cos(a_) * w * 0.5
        z = math.sin(a_) * d * 0.5
        y = 24 - h * (0.25 + rs.random() * 0.7)
        M.cube('body', (x - lw / 2, y - lw / 2, z - lw / 2), (lw, lw, lw), mats.get('lump', mats['body']))
    lb(M, 'maw', 'body', (0, -h * 0.35, -d / 2))
    M.cube_l('maw', (-w * 0.3, -3, -2), (w * 0.6, 6, 2), mats['maw'])
    lb(M, 'jaw', 'maw', (0, 3, 0), (10, 0, 0))
    M.cube_l('jaw', (-w * 0.3, 0, -3), (w * 0.6, 3, 3), mats['maw'])
    for k in range(p.get('arms', 4)):
        a = (k / max(p.get('arms', 4), 1) - 0.5) * 2.6
        n = f'arm{k}'
        sx = -1 if k % 2 == 0 else 1
        lb(M, n, 'body', (sx * w * 0.45, -h * (0.4 + 0.15 * (k // 2)), -d * 0.1 + k), (0, 0, sx * -50))
        M.cube_l(n, (-1.5, 0, -1.5), (3, 9, 3), mats.get('arm', mats['body']))
        lb(M, n + 'b', n, (0, 9, 0), (-30, 0, sx * 30))
        M.cube_l(n + 'b', (-1.2, 0, -1.2), (2.4, 8, 2.4), mats.get('arm', mats['body']))
    return dict(w=w, h=h, d=d)


# ================================================================ SERPENT
def serpent(M, p):
    mats = p['mats']
    segs = p.get('segs', 6)
    w = p.get('width', 8)
    sl = p.get('seg_len', 8)
    raise_ = p.get('raise', 18)
    y = 24 - w / 2
    M.bone('base', None, (0, y, 0))
    M.bone('seg0', 'base', (0, y, 0), (raise_ * 0, 0, 0))
    names = ['seg0']
    M.cube_l('seg0', (-w / 2, -w / 2, -2), (w, w, sl + 2), mats['body'])
    prev = 'seg0'
    for i in range(1, segs):
        n = f'seg{i}'
        lb(M, n, prev, (0, 0, sl))
        ww = w * (1 - 0.6 * i / segs)
        M.cube_l(n, (-ww / 2, -ww / 2, 0), (ww, ww, sl + 0.5), mats['body'])
        if p.get('ribs'):
            M.cube_l(n, (-ww / 2 - 1.5, -ww * 0.4, sl * 0.4), (ww + 3, 1.2, 1.2), mats['ribs'])
        names.append(n)
        prev = n
    # neck rising up toward the head
    lb(M, 'neck0', 'seg0', (0, 0, -1), (-raise_ * 1.4, 0, 0))
    M.cube_l('neck0', (-w * 0.45, -w * 0.45, -sl), (w * 0.9, w * 0.9, sl + 1), mats['body'])
    lb(M, 'neck1', 'neck0', (0, 0, -sl), (raise_ * 0.6, 0, 0))
    M.cube_l('neck1', (-w * 0.42, -w * 0.42, -sl), (w * 0.84, w * 0.84, sl + 1), mats['body'])
    hw, hh, hd = p.get('head', (w * 1.2, w * 0.8, w * 1.4))
    lb(M, 'head', 'neck1', (0, 0, -sl), (raise_ * 0.8, 0, 0))
    M.cube_l('head', (-hw / 2, -hh / 2 - 0.5, -hd), (hw, hh, hd), mats['head'])
    lb(M, 'jaw', 'head', (0, hh / 2 - 0.5, -1), (5, 0, 0))
    M.cube_l('jaw', (-hw * 0.45, 0, -hd + 0.5), (hw * 0.9, 2.5, hd - 0.5), mats.get('jaw', mats['head']))
    if p.get('fins'):
        for i in range(1, segs - 1, 2):
            M.cube_l(f'seg{i}', (-0.5, -w * 0.5 - p['fins'], 1), (1, p['fins'], sl * 0.7), mats.get('fin', 'fin'))
    return dict(segs=names)


# ================================================================ GOLEM
def golem(M, p):
    mats = p['mats']
    H = p.get('height', 72)
    lw = p.get('leg_w', 10)
    leg = H * 0.28
    hip = 24 - leg
    tw, th, td = p.get('torso', (30, 24, 18))
    M.bone('base', None, (0, hip, 0))
    M.bone('hips', 'base', (0, hip, 0))
    M.cube('hips', (-tw * 0.4, hip - 6, -td * 0.4), (tw * 0.8, 7, td * 0.8), mats.get('hips', mats['body']))
    M.bone('chest', 'hips', (0, hip - 5, 0), (p.get('hunch', 10), 0, 0))
    ty = hip - 5 - th
    M.cube('chest', (-tw / 2, ty, -td / 2), (tw, th, td), mats['body'])
    hx, hy, hz = p.get('head', (10, 9, 10))
    M.bone('head', 'chest', (0, ty + 3, -td * 0.35), (-p.get('hunch', 10), 0, 0))
    M.cube('head', (-hx / 2, ty + 3 - hy, -td * 0.35 - hz * 0.6), (hx, hy, hz), mats['head'])
    aw = p.get('arm_w', 10)
    alen = p.get('arm_len', H * 0.62)
    for side, sx in (('right', -1), ('left', 1)):
        A = (sx * (tw / 2 + aw * 0.3), ty + aw * 0.5, 0)
        B = (sx * (tw / 2 + aw * 0.55), ty + alen * 0.5, -2)
        C = (sx * (tw / 2 + aw * 0.5), ty + alen, -4)
        M.seg(f'{side}_arm', 'chest', A, B, aw, aw, mats.get('arm', mats['body']), extend=aw * 0.3)
        M.seg(f'{side}_fore', f'{side}_arm', B, C, aw * 1.1, aw * 1.1, mats.get('fore', mats.get('arm', mats['body'])), extend=aw * 0.2)
        M.bone_w(f'{side}_hand', f'{side}_fore', C, R0)
        M.cube_l(f'{side}_hand', (-aw * 0.65, 0, -aw * 0.65), (aw * 1.3, aw, aw * 1.3), mats.get('fist', mats.get('arm', mats['body'])))
        if p.get('shoulder'):
            M.cube_l(f'{side}_arm', (-aw * 0.75, -aw * 0.6, -aw * 0.75), (aw * 1.5, aw * 0.9, aw * 1.5), mats.get('shoulder', mats['body']))
    for side, sx in (('right', -1), ('left', 1)):
        A = (sx * tw * 0.24, hip, 0)
        B = (sx * tw * 0.26, hip + leg * 0.5, -1)
        C = (sx * tw * 0.26, 24 - 3, 0)
        M.seg(f'{side}_leg', 'hips', A, B, lw, lw, mats.get('leg', mats['body']), extend=2)
        M.seg(f'{side}_shin', f'{side}_leg', B, C, lw * 0.95, lw * 0.95, mats.get('leg', mats['body']), extend=1)
        M.bone_w(f'{side}_foot', f'{side}_shin', C, R0)
        M.cube_l(f'{side}_foot', (-lw * 0.6, 0, -lw * 0.85), (lw * 1.2, 3, lw * 1.5), mats.get('foot', mats.get('leg', mats['body'])))
    return dict(top=ty, tw=tw, th=th, td=td, hip=hip, hx=hx, hy=hy, hz=hz, neck_y=ty + 3, head_top=ty + 3 - hy, cw=tw, cd=td, aw=aw, chest_y=hip - 5, hip_y=hip)
