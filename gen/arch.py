"""Archetype helpers: geometry shortcuts and parametric animation generators (biped, quadruped, flyer, serpent, spider...).

Conventions (vanilla model space): y points DOWN, ground is y = 24, front is -z, model +x is the creature's LEFT side.
Limb bones point down: rotation x < 0 swings forward.  Spine / head bones point up: rotation x > 0 leans forward.
Position keyframes use "up is positive" like Blockbench."""
import math
import numpy as np
from core import Model, Anim, rot_zyx
from paint import Painter

TAU = math.tau
R0 = np.eye(3)


def S(ph, k=1.0, off=0.0):
    return math.sin(TAU * (ph * k + off))


def C(ph, k=1.0, off=0.0):
    return math.cos(TAU * (ph * k + off))


# ------------------------------------------------------------------ geometry
def box(M, bone, c, s, mat, **kw):
    """cube given by centre c and size s in absolute model coordinates"""
    return M.cube(bone, (c[0] - s[0] / 2, c[1] - s[1] / 2, c[2] - s[2] / 2), s, mat, **kw)


def top_box(M, bone, bottom_c, s, mat, **kw):
    """cube standing on point bottom_c (centre of its lower face); y grows upward visually"""
    return M.cube(bone, (bottom_c[0] - s[0] / 2, bottom_c[1] - s[1], bottom_c[2] - s[2] / 2), s, mat, **kw)


def local(M, name, parent, off, rot=(0, 0, 0)):
    """bone defined by its offset from the parent pivot"""
    M.bone(name, parent, (0, 0, 0))
    b = M.bones[name]
    b.off = [float(v) for v in off]
    b.rot = [float(v) for v in rot]
    pv = M.bones[parent].pivot if parent else [0, 0, 0]
    b.pivot = [pv[i] + b.off[i] for i in range(3)]
    return b


def seg(M, name, parent, A, B, w, d, mat, ext=0.0, **kw):
    return M.seg(name, parent, A, B, w, d if d else w, mat, extend=ext, **kw)


def chain(M, prefix, parent, pts, widths, mats, depth=1.0, ext=0.5, **kw):
    """series of segments through pts; returns list of bone names"""
    p = parent
    names = []
    for i in range(len(pts) - 1):
        n = f'{prefix}{i}'
        w = widths[min(i, len(widths) - 1)]
        m = mats if isinstance(mats, str) else mats[min(i, len(mats) - 1)]
        M.seg(n, p, pts[i], pts[i + 1], w, w * depth, m, extend=ext, **kw)
        names.append(n)
        p = n
    return names


def zero_bone(M, name, parent, pivot):
    """bone at absolute pivot without rotation"""
    return M.bone_w(name, parent, pivot, R0)


def plane(M, bone, pos_c, size_wh, mat, rot=None):
    """flat double-sided card (thorns, fins, flames, wings) standing in the XY plane; pos_c = centre of the lower edge"""
    w, h = size_wh
    return M.cube(bone, (pos_c[0] - w / 2, pos_c[1] - h, pos_c[2]), (w, h, 0), mat, plane=True)


def mirror_names(d):
    """mirror right_* rotation entries to left_* (y,z negated)"""
    o = dict(d)
    for k, v in d.items():
        if k.startswith('right_'):
            o['left_' + k[6:]] = (v[0], -v[1], -v[2])
    return o


# ------------------------------------------------------------------ animation helpers
def gen(a, bone, kind, fn, n=8, L=None):
    """sample fn(phase)->(x,y,z) at n+1 points over the animation length (phase 0..1) as a looping channel"""
    L = L or a.length
    keys = [(L * i / n, tuple(fn(i / n))) for i in range(n + 1)]
    {'r': a.rot, 'p': a.pos, 's': a.scl}[kind](bone, *keys)
    return a


def poses(a, frames, interp='C'):
    """frames: [(t, {bone: (rx,ry,rz)})] -> rotation channels; every bone used gets a key at every frame (missing = 0)"""
    names = []
    for _, d in frames:
        for b in d:
            if b not in names:
                names.append(b)
    for b in names:
        a.rot(b, *[(t, tuple(d.get(b, (0, 0, 0))), interp) for t, d in frames])
    return a


def moves(a, frames, interp='C'):
    """frames: [(t, {bone: (x,y,z)})] -> position channels (up positive)"""
    names = []
    for _, d in frames:
        for b in d:
            if b not in names:
                names.append(b)
    for b in names:
        a.pos(b, *[(t, tuple(d.get(b, (0, 0, 0))), interp) for t, d in frames])
    return a


def scales(a, frames, interp='C'):
    names = []
    for _, d in frames:
        for b in d:
            if b not in names:
                names.append(b)
    for b in names:
        a.scl(b, *[(t, tuple(d.get(b, (1, 1, 1))), interp) for t, d in frames])
    return a


def has(M, *names):
    return all(n in M.bones for n in names)


def present(M, d):
    """drop entries for bones the model doesn't have"""
    return {k: v for k, v in d.items() if k in M.bones}


# ------------------------------------------------------------------ biped
BIPED_DEFAULT = dict(stride=32, shin=42, arm=26, elbow=22, bob=0.7, twist=7, lean=2, breathe=1.2, head_bob=2.0,
                     hips='hips', chest='chest', head='head', walk_len=1.0, run_len=0.62, run_mul=1.7, tail=None)


def biped_cycle(M, p, name, length, mul=1.0, lean=None):
    a = Anim(name, length, True)
    q = {**BIPED_DEFAULT, **p}
    st, sh, ar, el = q['stride'] * mul, q['shin'] * min(mul, 1.5), q['arm'] * mul, q['elbow'] * mul
    ln = q['lean'] if lean is None else lean
    for side, off in (('right', 0.0), ('left', 0.5)):
        sg = 1 if side == 'right' else -1
        gen(a, f'{side}_leg', 'r', lambda ph, off=off: (-st * S(ph, 1, off), 0, 0))
        gen(a, f'{side}_shin', 'r', lambda ph, off=off: (sh * max(0.0, C(ph, 1, off)) + 4, 0, 0))
        if has(M, f'{side}_foot'):
            gen(a, f'{side}_foot', 'r', lambda ph, off=off: (st * 0.35 * S(ph, 1, off) - sh * 0.25 * max(0.0, C(ph, 1, off)), 0, 0))
        gen(a, f'{side}_arm', 'r', lambda ph, off=off: (ar * S(ph, 1, off), 0, 0))
        gen(a, f'{side}_fore', 'r', lambda ph, off=off: (-el - el * 0.5 * max(0.0, S(ph, 1, off)), 0, 0))
    gen(a, q['hips'], 'p', lambda ph: (0, q['bob'] * mul * C(ph, 2, 0.1), 0))
    gen(a, q['hips'], 'r', lambda ph: (ln * 0.5, q['twist'] * 0.5 * mul * S(ph), 0))
    gen(a, q['chest'], 'r', lambda ph: (ln, q['twist'] * mul * -S(ph), 0))
    gen(a, q['head'], 'r', lambda ph: (-ln * 0.6 + q['head_bob'] * 0.4 * C(ph, 2), q['twist'] * 0.6 * mul * S(ph), 0))
    if q['tail'] and has(M, q['tail'][0]):
        for i, tb in enumerate(q['tail']):
            if tb in M.bones:
                gen(a, tb, 'r', lambda ph, i=i: (0, 8 * mul * S(ph, 1, 0.15 + i * 0.12), 0))
    return a


def biped_idle(M, p, length=2.6):
    q = {**BIPED_DEFAULT, **p}
    a = Anim('IDLE', length, True)
    gen(a, q['chest'], 'r', lambda ph: (q['breathe'] * S(ph), 0, 0))
    gen(a, q['hips'], 'p', lambda ph: (0, 0.25 * S(ph, 1, 0.25), 0))
    gen(a, q['head'], 'r', lambda ph: (1.5 * S(ph, 1, 0.1) - 1, 3 * S(ph, 1, 0.6), 0.6 * S(ph, 2)))
    for side, sg in (('right', 1), ('left', -1)):
        gen(a, f'{side}_arm', 'r', lambda ph, sg=sg: (2.0 * S(ph, 1, 0.2), 0, sg * (1.5 + 1.2 * S(ph, 1, 0.1))))
        gen(a, f'{side}_fore', 'r', lambda ph: (-8 - 2 * S(ph, 1, 0.3), 0, 0))
    if q['tail'] and has(M, q['tail'][0]):
        for i, tb in enumerate(q['tail']):
            if tb in M.bones:
                gen(a, tb, 'r', lambda ph, i=i: (1.5 * S(ph, 1, i * 0.1), 7 * S(ph, 1, 0.2 + i * 0.12), 0))
    return a


def biped_death(M, p, length=1.2, kind='fall_back'):
    q = {**BIPED_DEFAULT, **p}
    a = Anim('DEATH', length, False)
    L = length
    t = lambda f: L * f
    arms = lambda x, z: {'right_arm': (x, 0, -z), 'left_arm': (x, 0, z)}
    if kind == 'fall_back':
        poses(a, [(0, {}),
                  (t(0.25), {'base': (-8, 0, 0), q['head']: (-12, 0, 0), **arms(-38, 25), 'right_shin': (14, 0, 0), 'left_shin': (8, 0, 0)}),
                  (t(0.7), {'base': (-80, 0, 4), q['head']: (-18, 5, 0), **arms(-20, 40), 'right_shin': (30, 0, 0), 'left_shin': (10, 0, 0), 'right_leg': (-10, 0, 0)}),
                  (t(0.85), {'base': (-92, 0, 4), q['head']: (-8, 8, 0), **arms(-6, 50), 'right_shin': (22, 0, 0), 'left_shin': (6, 0, 0)}),
                  (L, {'base': (-90, 0, 4), q['head']: (-6, 10, 0), **arms(-4, 55), 'right_shin': (20, 0, 0), 'left_shin': (5, 0, 0)})])
        moves(a, [(0, {}), (t(0.85), {'base': (0, 0.8, 0)}), (L, {'base': (0, 0.8, 0)})])
    elif kind == 'collapse':
        poses(a, [(0, {}),
                  (t(0.3), {'right_leg': (-45, 0, 0), 'left_leg': (-30, 0, 0), 'right_shin': (80, 0, 0), 'left_shin': (60, 0, 0), q['chest']: (14, 0, 0), q['head']: (16, 0, 0), **arms(-10, 12)}),
                  (t(0.7), {'right_leg': (-82, 0, 6), 'left_leg': (-68, 0, -6), 'right_shin': (122, 0, 0), 'left_shin': (118, 0, 0), q['chest']: (46, 0, 8), q['head']: (34, 0, 0), **arms(8, 18)}),
                  (L, {'right_leg': (-86, 0, 6), 'left_leg': (-72, 0, -6), 'right_shin': (130, 0, 0), 'left_shin': (126, 0, 0), q['chest']: (62, 0, 10), q['head']: (40, 0, 0), **arms(16, 24)})])
        moves(a, [(0, {}), (t(0.7), {q['hips']: (0, -9, 0)}), (L, {q['hips']: (0, -11.5, 0)})])
    elif kind == 'shrink':
        poses(a, [(0, {}), (t(0.5), {q['chest']: (20, 0, 0), q['head']: (25, 0, 0), **arms(-20, 20)}), (L, {q['chest']: (30, 0, 0), q['head']: (35, 0, 0), **arms(-10, 30)})])
        scales(a, [(0, {'base': (1, 1, 1)}), (t(0.6), {'base': (1.05, 0.8, 1.05)}), (L, {'base': (0.1, 0.02, 0.1)})])
    elif kind == 'fall_forward':
        poses(a, [(0, {}),
                  (t(0.3), {'base': (10, 0, 0), q['head']: (14, 0, 0), **arms(-30, 20)}),
                  (t(0.75), {'base': (82, 0, 3), q['head']: (-30, 0, 0), **arms(-130, 12)}),
                  (L, {'base': (90, 0, 3), q['head']: (-34, 4, 0), **arms(-140, 14)})])
        moves(a, [(0, {}), (t(0.75), {'base': (0, 0.8, 0)}), (L, {'base': (0, 0.8, 0)})])
    return a


def biped_set(M, p, death='fall_back', death_len=1.2, run=True):
    """standard IDLE / WALK / RUN / DEATH animations for a biped rig"""
    q = {**BIPED_DEFAULT, **p}
    out = {'idle': biped_idle(M, p), 'walk': biped_cycle(M, p, 'WALK', q['walk_len'])}
    if run:
        out['run'] = biped_cycle(M, p, 'RUN', q['run_len'], q['run_mul'], lean=(q['lean'] + 12))
    out['death'] = biped_death(M, p, death_len, death)
    return out


# ------------------------------------------------------------------ quadruped
QUAD_DEFAULT = dict(stride=30, shin=36, bob=0.8, sway=3, head='head', body='body', jaw=None, tail=None, ears=None,
                    legs=(('fr', 0.0), ('bl', 0.0), ('fl', 0.5), ('br', 0.5)), walk_len=0.9, run_len=0.5, run_mul=1.55,
                    neck=None, front_stride=1.0, back_stride=1.0)


def quad_cycle(M, p, name, length, mul=1.0):
    q = {**QUAD_DEFAULT, **p}
    a = Anim(name, length, True)
    for lg, off in q['legs']:
        k = q['front_stride'] if lg[0] == 'f' else q['back_stride']
        st = q['stride'] * mul * k
        gen(a, f'{lg}_leg', 'r', lambda ph, off=off, st=st: (st * S(ph, 1, off), 0, 0))
        if f'{lg}_shin' in M.bones:
            gen(a, f'{lg}_shin', 'r', lambda ph, off=off: (-q['shin'] * min(mul, 1.4) * max(0.0, C(ph, 1, off)) - 3 if lg[0] == 'b' else q['shin'] * min(mul, 1.4) * max(0.0, C(ph, 1, off)), 0, 0))
        if f'{lg}_foot' in M.bones:
            gen(a, f'{lg}_foot', 'r', lambda ph, off=off: (-st * 0.3 * S(ph, 1, off), 0, 0))
    gen(a, q['body'], 'p', lambda ph: (0, q['bob'] * mul * C(ph, 2, 0.05), 0))
    gen(a, q['body'], 'r', lambda ph: (1.5 * mul * S(ph, 2), q['sway'] * S(ph), 1.2 * S(ph)))
    gen(a, q['head'], 'r', lambda ph: (-1.5 * mul * S(ph, 2, 0.1), -q['sway'] * 1.2 * S(ph), 0))
    if q['neck'] and q['neck'] in M.bones:
        gen(a, q['neck'], 'r', lambda ph: (2 * S(ph, 2, 0.3), -q['sway'] * 0.5 * S(ph), 0))
    if q['jaw'] and q['jaw'] in M.bones:
        gen(a, q['jaw'], 'r', lambda ph: (4 + 3 * S(ph, 2), 0, 0))
    for i, tb in enumerate(q['tail'] or []):
        if tb in M.bones:
            gen(a, tb, 'r', lambda ph, i=i: (3 * S(ph, 2, 0.2 + i * 0.1), 14 * mul * S(ph, 1, 0.3 + i * 0.13), 0))
    for i, eb in enumerate(q['ears'] or []):
        if eb in M.bones:
            gen(a, eb, 'r', lambda ph, i=i: (6 * S(ph, 2, 0.4), 0, (4 if i % 2 else -4) * S(ph, 2, 0.4)))
    return a


def quad_idle(M, p, length=2.8):
    q = {**QUAD_DEFAULT, **p}
    a = Anim('IDLE', length, True)
    gen(a, q['body'], 'r', lambda ph: (1.0 * S(ph), 0, 0))
    gen(a, q['body'], 'p', lambda ph: (0, 0.25 * S(ph, 1, 0.25), 0))
    gen(a, q['head'], 'r', lambda ph: (3 * S(ph, 1, 0.1), 7 * S(ph, 1, 0.5), 1.5 * S(ph, 2)))
    if q['neck'] and q['neck'] in M.bones:
        gen(a, q['neck'], 'r', lambda ph: (2 * S(ph, 1, 0.2), 3 * S(ph, 1, 0.5), 0))
    if q['jaw'] and q['jaw'] in M.bones:
        gen(a, q['jaw'], 'r', lambda ph: (3 + 3 * S(ph), 0, 0))
    for i, tb in enumerate(q['tail'] or []):
        if tb in M.bones:
            gen(a, tb, 'r', lambda ph, i=i: (2 * S(ph, 1, i * 0.1), 12 * S(ph, 1, 0.1 + i * 0.1), 0))
    for i, eb in enumerate(q['ears'] or []):
        if eb in M.bones:
            gen(a, eb, 'r', lambda ph, i=i: (0, 0, (5 if i % 2 else -5) * S(ph, 2, 0.1 + i * 0.2)))
    return a


def quad_death(M, p, length=1.2, kind='side'):
    q = {**QUAD_DEFAULT, **p}
    a = Anim('DEATH', length, False)
    L = length
    t = lambda f: L * f
    legs = {}
    for lg, _ in q['legs']:
        legs[f'{lg}_leg'] = (-20 if lg[0] == 'f' else 20, 0, 0)
    if kind == 'side':
        poses(a, [(0, {}), (t(0.3), {'base': (0, 0, 14), q['head']: (-14, 8, 0)}),
                  (t(0.75), {'base': (0, 0, 86), q['head']: (-18, 12, 0), **legs}),
                  (L, {'base': (0, 0, 90), q['head']: (-20, 14, 0), **legs})])
        moves(a, [(0, {}), (t(0.8), {'base': (0, 0.6, 0)}), (L, {'base': (0, 0.6, 0)})])
    else:
        poses(a, [(0, {}), (t(0.5), {q['body']: (6, 0, 0), q['head']: (20, 0, 0)}), (L, {q['body']: (8, 0, 0), q['head']: (28, 0, 0)})])
        scales(a, [(0, {'base': (1, 1, 1)}), (L, {'base': (0.1, 0.02, 0.1)})])
    return a


def quad_set(M, p, death='side', death_len=1.2, run=True):
    q = {**QUAD_DEFAULT, **p}
    out = {'idle': quad_idle(M, p), 'walk': quad_cycle(M, p, 'WALK', q['walk_len'])}
    if run:
        out['run'] = quad_cycle(M, p, 'RUN', q['run_len'], q['run_mul'])
    out['death'] = quad_death(M, p, death_len, death)
    return out


# ------------------------------------------------------------------ flyer
FLY_DEFAULT = dict(body='body', head='head', wings=(('right_wing', -1), ('left_wing', 1)), wing_segs=('0', '1', '2'),
                   flap=38, flap_len=0.55, idle_len=1.1, tail=None, legs=None, bob=1.2, lean=10, jaw=None)


def flap(M, p, name, length, amp=1.0, lean=0.0, bobmul=1.0):
    q = {**FLY_DEFAULT, **p}
    a = Anim(name, length, True)
    for wn, sg in q['wings']:
        for i, sfx in enumerate(q['wing_segs']):
            b = wn + sfx
            if b in M.bones:
                lag = 0.07 * i
                gen(a, b, 'r', lambda ph, sg=sg, lag=lag, i=i: (4 * S(ph, 1, -lag), 5 * S(ph, 1, -lag + 0.1) * (i > 0), sg * q['flap'] * amp * (1 - 0.25 * i) * S(ph, 1, -lag)), n=10)
    gen(a, q['body'], 'p', lambda ph: (0, q['bob'] * bobmul * C(ph, 1, 0.05), 0), n=10)
    gen(a, q['body'], 'r', lambda ph: (lean + 1.5 * S(ph), 0, 0), n=10)
    gen(a, q['head'], 'r', lambda ph: (-lean * 0.7 - 2 * S(ph, 1, 0.2), 0, 0), n=10)
    for i, tb in enumerate(q['tail'] or []):
        if tb in M.bones:
            gen(a, tb, 'r', lambda ph, i=i: (5 * S(ph, 1, 0.2 + i * 0.1), 6 * S(ph, 1 / 2, i * 0.15), 0), n=10)
    for lg in (q['legs'] or []):
        if lg in M.bones:
            gen(a, lg, 'r', lambda ph: (-25 + 3 * S(ph, 1, 0.3), 0, 0), n=10)
    if q['jaw'] and q['jaw'] in M.bones:
        gen(a, q['jaw'], 'r', lambda ph: (5 + 4 * S(ph, 2), 0, 0), n=10)
    return a


def flyer_death(M, p, length=1.2, drop=24.0):
    q = {**FLY_DEFAULT, **p}
    a = Anim('DEATH', length, False)
    L = length
    t = lambda f: L * f
    fr = []
    for wn, sg in q['wings']:
        for i, sfx in enumerate(q['wing_segs']):
            fr.append((wn + sfx, sg, i))
    f0 = {'base': (0, 0, 0)}
    frames = [(0, {}), (t(0.35), {'base': (-10, 0, 25), q['head']: (-20, 0, 0)}), (L, {'base': (-30, 0, 110), q['head']: (-30, 10, 0)})]
    for wb, sg, i in fr:
        if wb in M.bones:
            frames[1][1][wb] = (0, 0, sg * (-20 + i * 10))
            frames[2][1][wb] = (0, 0, sg * (-60 + i * 12))
    poses(a, frames)
    moves(a, [(0, {}), (t(0.5), {'base': (0, -drop * 0.3, 0)}), (t(0.9), {'base': (0, -drop, 0)}), (L, {'base': (0, -drop, 0)})])
    return a


def flyer_set(M, p, death_len=1.2, drop=24.0):
    q = {**FLY_DEFAULT, **p}
    return {'idle': flap(M, p, 'IDLE', q['idle_len'], 0.55, 0, 0.6),
            'walk': flap(M, p, 'WALK', q['flap_len'], 1.0, q['lean'], 1.0),
            'death': flyer_death(M, p, death_len, drop)}


# ------------------------------------------------------------------ serpent / chain bodies
def serpent_set(M, segs, head_bone, p=None):
    """segs: list of bone names from neck to tail tip"""
    p = p or {}
    wave = p.get('wave', 16)
    n = len(segs)
    out = {}
    for name, L, amp, spd in (('IDLE', 3.0, 0.35, 1), ('WALK', 1.1, 1.0, 1)):
        a = Anim(name, L, True)
        for i, s in enumerate(segs):
            gen(a, s, 'r', lambda ph, i=i, amp=amp: (2.0 * amp * S(ph, 1, i * 0.1), wave * amp * S(ph, 1, -i * 0.11) * (0.6 + 0.4 * i / n), 3 * amp * S(ph, 1, i * 0.07)), n=10)
        gen(a, head_bone, 'r', lambda ph, amp=amp: (-2 * amp * S(ph, 1, 0.1), -wave * 0.5 * amp * S(ph, 1, 0.1), 0), n=10)
        out[name.lower()] = a
    d = Anim('DEATH', p.get('death_len', 1.4), False)
    L = d.length
    fr = [(0, {})]
    fr.append((L * 0.4, {s: (8 + 2 * i, (12 if i % 2 else -12) * 1.5, 0) for i, s in enumerate(segs)}))
    fr.append((L, {s: (14 + 3 * i, (24 if i % 2 else -24), 0) for i, s in enumerate(segs)}))
    poses(d, fr)
    moves(d, [(0, {}), (L, {'base': (0, -1.5, 0)})])
    scales(d, [(0, {'base': (1, 1, 1)}), (L * 0.7, {'base': (1, 1, 1)}), (L, {'base': (1.0, 0.45, 1.0)})])
    out['death'] = d
    return out


# ------------------------------------------------------------------ spider
def spider_set(M, p=None):
    """legs named leg{0..7}_a / _b ; even = right, odd = left"""
    p = p or {}
    out = {}
    nleg = p.get('nleg', 8)
    for name, L, amp in (('IDLE', 3.0, 0.15), ('WALK', 0.8, 1.0), ('RUN', 0.5, 1.5)):
        a = Anim(name, L, True)
        for i in range(nleg):
            side = 1 if i % 2 == 0 else -1
            pair = i // 2
            off = ((pair % 2) * 0.5 + (0 if side == 1 else 0.5)) % 1.0
            ra = f'leg{i}_a'
            rb = f'leg{i}_b'
            if ra in M.bones:
                gen(a, ra, 'r', lambda ph, off=off, side=side, pair=pair: (0, 22 * amp * S(ph, 1, off) * (1 if pair < 2 else -1) * 0.8, side * 9 * amp * max(0.0, C(ph, 1, off)) + side * 0), n=8)
            if rb in M.bones:
                gen(a, rb, 'r', lambda ph, off=off, side=side: (0, 0, side * 14 * amp * max(0.0, C(ph, 1, off))), n=8)
        if 'body' in M.bones:
            gen(a, 'body', 'p', lambda ph: (0, 0.5 * amp * C(ph, 2), 0), n=8)
            gen(a, 'body', 'r', lambda ph: (0, 2 * amp * S(ph), 0), n=8)
        if 'abdomen' in M.bones:
            gen(a, 'abdomen', 'r', lambda ph: (2 * S(ph, 2, 0.2) + (1.5 if name == 'IDLE' else 0), 3 * S(ph, 1, 0.3), 0), n=8)
        if 'head' in M.bones:
            gen(a, 'head', 'r', lambda ph: (1.5 * S(ph, 2), 2 * S(ph, 1, 0.5), 0), n=8)
        out[name.lower()] = a
    d = Anim('DEATH', p.get('death_len', 1.2), False)
    L = d.length
    fr = [(0, {}), (L * 0.5, {'base': (0, 0, 30)}), (L, {'base': (0, 0, 180)})]
    for i in range(nleg):
        side = 1 if i % 2 == 0 else -1
        for sfx in ('a', 'b'):
            if f'leg{i}_{sfx}' in M.bones:
                fr[1][1][f'leg{i}_{sfx}'] = (0, 0, side * (30 if sfx == 'a' else 55))
                fr[2][1][f'leg{i}_{sfx}'] = (0, 0, side * (50 if sfx == 'a' else 95))
    poses(d, fr)
    moves(d, [(0, {}), (L, {'base': (0, 0.8, 0)})])
    out['death'] = d
    return out


# ------------------------------------------------------------------ attack-pose helpers
def swing(a_name, length, frames, M=None, loop=False):
    """build an ability animation from [(t, {bone: rot})]"""
    a = Anim(a_name, length, loop)
    poses(a, frames)
    return a
