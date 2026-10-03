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


def world_bone(M, name, parent, pivot, rx=0.0, ry=0.0, rz=0.0):
    """bone at an absolute pivot with an absolute (world) rest rotation in degrees"""
    return M.bone_w(name, parent, pivot, rot_zyx(math.radians(rx), math.radians(ry), math.radians(rz)))


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


# ------------------------------------------------------------------ humanoid builder
def _rx(p, pivot, deg):
    """rotate point p about the x axis through pivot (positive deg = things above the pivot lean forward, i.e. towards -z)"""
    a = math.radians(deg)
    y, z = p[1] - pivot[1], p[2] - pivot[2]
    return (p[0], pivot[1] + y * math.cos(a) - z * math.sin(a), pivot[2] + y * math.sin(a) + z * math.cos(a))


HUMAN_MATS = dict(pelvis='pelvis', torso='torso', head='head', arm='arm', fore='fore', hand='hand', leg='leg', shin='shin', foot='foot')


def humanoid(M, leg=(11, 11), leg_w=4.6, foot=(5.2, 2.6, 7.5), hip_w=9.5, pelvis_h=4.6, torso=(11, 12, 6.5), shoulder_w=None, neck=2.2,
             head=(8, 8, 8), arm=(9.5, 9.5), arm_w=3.9, hand=(3.3, 3.8, 3.3), hunch=0.0, arm_out=1.0, arm_fwd=1.0, bend=0.0,
             mats=None, head_fwd=0.0, stance=0.0, hand_bone=True, foot_bone=True, torso_taper=1.0):
    """standard rig: base > hips > chest(seg) > neck > head, arms on chest, legs on hips.
    returns dict of key points.  Everything is in absolute rest coordinates (ground y=24, front -z)."""
    mt = {**HUMAN_MATS, **(mats or {})}
    foot_h = foot[1]
    ankle_y = 24 - foot_h
    hip_y = ankle_y - leg[0] - leg[1]
    M.bone('base', None, (0, 24, 0))
    M.bone('hips', 'base', (0, hip_y, 0))
    box(M, 'hips', (0, hip_y - pelvis_h * 0.35, 0), (hip_w, pelvis_h, torso[2] * 0.92), mt['pelvis'])
    waist = (0, hip_y - pelvis_h * 0.7, 0)
    Rv = lambda v: _rx((v[0], v[1], v[2]), waist, hunch)
    P1 = Rv((0, waist[1] - torso[1], 0))
    M.seg('chest', 'hips', waist, P1, torso[0], torso[2], mt['torso'], extend=0.3)
    sw = shoulder_w if shoulder_w else torso[0] / 2 + arm_w / 2 - 0.4
    K = dict(hip_y=hip_y, ankle_y=ankle_y, waist=waist, P1=P1, leg_w=leg_w)
    # neck + head
    npv = (0, P1[1] - 0.3, P1[2] - head_fwd * 0.3)
    zero_bone(M, 'neck', 'chest', npv)
    if neck > 0.1:
        box(M, 'neck', (npv[0], npv[1] - neck / 2 + 0.3, npv[2]), (head[0] * 0.5, neck + 0.6, head[2] * 0.5), mt['torso'])
    hpv = (0, npv[1] - neck, npv[2] - head_fwd)
    zero_bone(M, 'head', 'neck', hpv)
    hc = (0, hpv[1] - head[1] / 2 + 0.6, hpv[2])
    box(M, 'head', hc, head, mt['head'])
    K.update(neck=npv, head_pivot=hpv, head_c=hc, head_top=hc[1] - head[1] / 2)
    # arms
    K['hands'] = {}
    bends = bend if isinstance(bend, (tuple, list)) else (bend, bend)
    for sx, s in ((-1, 'right'), (1, 'left')):
        bd = bends[0] if sx < 0 else bends[1]
        A = Rv((sx * sw, waist[1] - torso[1] + 1.2, 0))
        B = (A[0] + sx * arm_out * 0.9, A[1] + arm[0], A[2] - 0.8 * arm_fwd - bd * 0.5)
        C = (B[0] - sx * 0.2 * arm_out, B[1] + arm[1] * (1 - bd * 0.6), B[2] - 2.0 * arm_fwd - bd * arm[1] * 0.8)
        M.seg(f'{s}_arm', 'chest', A, B, arm_w, arm_w, mt['arm'], extend=0.7)
        M.seg(f'{s}_fore', f'{s}_arm', B, C, arm_w * 0.9, arm_w * 0.9, mt['fore'], extend=0.5)
        if hand_bone:
            zero_bone(M, f'{s}_hand', f'{s}_fore', C)
            M.cube_l(f'{s}_hand', (-hand[0] / 2, -0.3, -hand[2] / 2), hand, mt['hand'])
        K['hands'][s] = C
        K[f'{s}_A'], K[f'{s}_B'] = A, B
    # legs
    for sx, s in ((-1, 'right'), (1, 'left')):
        x = sx * (hip_w / 2 - leg_w / 2 + 0.2 + stance)
        A = (x, hip_y - 0.2, 0.0)
        B = (x + sx * stance * 0.4, hip_y + leg[0], -0.5)
        C = (x + sx * stance * 0.2, ankle_y, 0.4)
        M.seg(f'{s}_leg', 'hips', A, B, leg_w, leg_w, mt['leg'], extend=0.6)
        M.seg(f'{s}_shin', f'{s}_leg', B, C, leg_w * 0.9, leg_w * 0.9, mt['shin'], extend=0.4)
        if foot_bone:
            zero_bone(M, f'{s}_foot', f'{s}_shin', C)
            M.cube_l(f'{s}_foot', (-foot[0] / 2, -0.4, -foot[2] * 0.62), (foot[0], foot[1] + 0.4, foot[2]), mt['foot'])
        K[f'{s}_knee'] = B
    return K


def sword(M, hand_bone, length=14, width=2.2, thick=0.9, mat='blade', guard=(5.5, 1.2, 1.6), guard_mat='guard', grip=4.0, grip_mat='grip',
          pommel=None, dir='up', flip=False, offset=(0, 0, 0)):
    """blade held in the hand bone; dir 'up' (blade along -y) or 'fwd' (along -z)"""
    ox, oy, oz = offset
    if dir == 'up':
        M.cube_l(hand_bone, (-thick / 2 + ox, -grip - length + oy, -width / 2 + oz), (thick, length, width), mat)
        M.cube_l(hand_bone, (-guard[2] / 2 + ox, -grip + oy, -guard[0] / 2 + oz), (guard[2], guard[1], guard[0]), guard_mat)
        M.cube_l(hand_bone, (-0.8 + ox, -grip + 0.8 + oy, -0.8 + oz), (1.6, grip, 1.6), grip_mat)
        if pommel:
            M.cube_l(hand_bone, (-1.2 + ox, grip * 0.6 + oy, -1.2 + oz), (2.4, 1.6, 2.4), pommel)
    else:
        M.cube_l(hand_bone, (-thick / 2 + ox, -width / 2 + 1.0 + oy, -grip - length + oz), (thick, width, length), mat)
        M.cube_l(hand_bone, (-guard[2] / 2 + ox, -guard[0] / 2 + 1.0 + oy, -grip + oz), (guard[2], guard[0], guard[1]), guard_mat)
        M.cube_l(hand_bone, (-0.8 + ox, 0.2 + oy, -grip + oz), (1.6, 1.6, grip), grip_mat)


# ------------------------------------------------------------------ pixel stamps (emblems painted on faces)
def stamp(col, bitmap, x0, y0, colors, glow=None, glow_chars=''):
    """paint a small ascii bitmap into col[H,W,3]; colors maps char -> rgb; chars in glow_chars also write glow"""
    H, W = col.shape[:2]
    g = None
    for j, row in enumerate(bitmap):
        for i, ch in enumerate(row):
            if ch == ' ' or ch == '.':
                continue
            x, y = x0 + i, y0 + j
            if 0 <= x < W and 0 <= y < H and ch in colors:
                col[y, x] = colors[ch]
                if glow is not None and ch in glow_chars:
                    glow[y, x] = (*colors[ch], 255)
    return col


# ------------------------------------------------------------------ quadruped builder
QUAD_MATS = dict(body='body', neck='body', head='head', snout='head', jaw='jaw', leg='leg', shin='leg', foot='foot', tail='tail')


def quadruped(M, body=(11, 10, 20), leg=(7.5, 8.0), leg_w=3.4, foot=(3.8, 2.2, 5.6), neck_len=4.5, neck_rise=4.5, neck_w=5.5,
              head=(8, 7.5, 7), snout=(5.2, 4.0, 6.0), jaw=(4.6, 1.8, 6.0), tail_segs=4, tail_len=4.2, tail_w=2.2, mats=None, rise=0.0):
    mt = {**QUAD_MATS, **(mats or {})}
    ankle_y = 24 - foot[1]
    knee_y = ankle_y - leg[1]
    hip_y = knee_y - leg[0]
    bc = (0, hip_y - body[1] / 2 + 2.0 - rise, 0)
    M.bone('base', None, (0, 24, 0))
    M.bone('body', 'base', bc)
    box(M, 'body', bc, body, mt['body'])
    K = dict(body_c=bc, hip_y=hip_y, ankle_y=ankle_y, knee_y=knee_y)
    # neck + head
    nb = (0, bc[1] - body[1] * 0.2, bc[2] - body[2] / 2 + 1.5)
    nt = (0, nb[1] - neck_rise, nb[2] - neck_len)
    M.seg('neck', 'body', nb, nt, neck_w, neck_w, mt['neck'], extend=0.8)
    hp = (0, nt[1] - 0.5, nt[2] - 0.2)
    zero_bone(M, 'head', 'neck', hp)
    hc = (0, hp[1] - head[1] * 0.1, hp[2] - head[2] / 2 + 1.0)
    box(M, 'head', hc, head, mt['head'])
    sc = (0, hc[1] + head[1] * 0.12, hc[2] - head[2] / 2 - snout[2] / 2 + 0.6)
    box(M, 'head', sc, snout, mt['snout'])
    jp = (0, hc[1] + head[1] / 2 - 0.4, hc[2] + head[2] / 2 - 1.0)
    zero_bone(M, 'jaw', 'head', jp)
    M.cube_l('jaw', (-jaw[0] / 2, -jaw[1] / 2 + 0.2, -jaw[2] - head[2] * 0.6 + 1.2), jaw, mt['jaw'])
    K.update(head_c=hc, snout_c=sc, jaw_p=jp, neck_base=nb, head_p=hp)
    # legs
    for fb, sx, sz in (('fr', -1, -1), ('fl', 1, -1), ('br', -1, 1), ('bl', 1, 1)):
        x = sx * (body[0] / 2 - leg_w * 0.35)
        z = sz * (body[2] / 2 - leg_w * 0.9)
        back = sz > 0
        A = (x, hip_y, z)
        B = (x, knee_y, z + (1.8 if back else -0.6))
        C = (x, ankle_y, z + (-0.8 if back else 0.6))
        M.seg(f'{fb}_leg', 'body', A, B, leg_w * (1.15 if back else 1.0), leg_w, mt['leg'], extend=0.5)
        M.seg(f'{fb}_shin', f'{fb}_leg', B, C, leg_w * 0.85, leg_w * 0.85, mt['shin'], extend=0.3)
        zero_bone(M, f'{fb}_foot', f'{fb}_shin', C)
        M.cube_l(f'{fb}_foot', (-foot[0] / 2, -0.4, -foot[2] * 0.65), (foot[0], foot[1] + 0.4, foot[2]), mt['foot'])
        K[f'{fb}_knee'] = B
    # tail
    pts = [(0, bc[1] - body[1] * 0.1, bc[2] + body[2] / 2 - 1)]
    for i in range(tail_segs):
        a = pts[-1]
        pts.append((0, a[1] + 1.2 - i * 0.2, a[2] + tail_len))
    tn = chain(M, 'tail', 'body', pts, (tail_w * 1.1, tail_w, tail_w * 0.8, tail_w * 0.6), mt['tail'], ext=0.4)
    K['tail_names'] = tn
    K['tail_pts'] = pts
    return K


# ------------------------------------------------------------------ wings
def wings(M, parent, shoulder, tips, heights, strut_mat, sheet_mats, sx_list=(-1, 1), widths=(2.4, 2.0, 1.6), prefix=('right_wing', 'left_wing'), cov_mat=None):
    """symmetric 3-segment wings.  shoulder=(x,y,z) for the +x (left) wing; tips=(e1,e2,e3) points likewise for +x.
    sheet_mats: material name (or per-segment tuple) for the vertical wing panels; heights per segment."""
    sm = sheet_mats if isinstance(sheet_mats, (tuple, list)) else (sheet_mats,) * 3
    for sx, s in ((-1, prefix[0]), (1, prefix[1])):
        sh = (shoulder[0] * sx, shoulder[1], shoulder[2])
        pts = [(t[0] * sx, t[1], t[2]) for t in tips]
        M.seg(s + '0', parent, sh, pts[0], widths[0], widths[0], strut_mat, extend=0.4)
        M.seg(s + '1', s + '0', pts[0], pts[1], widths[1], widths[1], strut_mat, extend=0.3)
        M.seg(s + '2', s + '1', pts[1], pts[2], widths[2], widths[2], strut_mat, extend=0.3)
        spans = ((sh, pts[0]), (pts[0], pts[1]), (pts[1], pts[2]))
        for i, (pa, pb) in enumerate(spans):
            zero_bone(M, f'{s}_m{i}', s + str(i), pa)
            x0, x1 = sorted((pa[0], pb[0]))
            M.cube(f'{s}_m{i}', (x0, min(pa[1], pb[1]) + 0.2, (pa[2] + pb[2]) / 2), (x1 - x0, heights[i], 0), sm[i], plane=True)
        if cov_mat:
            zero_bone(M, f'{s}_cov', s + '0', sh)
            x0, x1 = sorted((sh[0], pts[1][0]))
            M.cube(f'{s}_cov', (x0, min(sh[1], pts[1][1]) - 0.4, sh[2] - 1.0), (x1 - x0, heights[0] * 0.55, 0), cov_mat, plane=True)


def add_wing_flap(a, amp=1.0, speed=1.0, base=0.0, names=('right_wing', 'left_wing'), segs=3):
    """add a flapping channel set to an existing looping Anim"""
    for sx, nm in ((-1, names[0]), (1, names[1])):
        for i in range(segs):
            gen(a, f'{nm}{i}', 'r', lambda ph, sx=sx, i=i: (3 * amp * S(ph, speed, -0.07 * i), 0, sx * (base + 26 * amp * (1 - 0.2 * i) * S(ph, speed, -0.07 * i))), n=10)
