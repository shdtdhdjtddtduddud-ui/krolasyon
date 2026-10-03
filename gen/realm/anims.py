"""Keyframe animation sets for the creature archetypes.

Slots: 0 idle, 1 walk, 2 attack, 3 cast, 4 special, 5 death, 6 run.
Timing contract with the Java ability specs: attack impact at 0.35 s (tick 7), cast release at 0.55 s (tick 11),
special impact at half of its length."""
import math
from core import Anim

ATK_LEN, ATK_HIT = 0.8, 0.35
CAST_LEN, CAST_HIT = 1.2, 0.55


def wave(a, bone, length, amp, phase=0.0, cycles=1, base=(0, 0, 0), kind='rot', samples=8):
    keys = []
    for k in range(samples + 1):
        t = length * k / samples
        ph = math.tau * cycles * k / samples + phase
        keys.append((t, (base[0] + amp[0] * math.sin(ph), base[1] + amp[1] * math.sin(ph + 1.3), base[2] + amp[2] * math.cos(ph))))
    (a.rot if kind == 'rot' else a.pos if kind == 'pos' else a.scl)(bone, *keys)


def frames(a, seq, kind='rot'):
    """seq: [(t, {bone: vec})]; bones missing in a frame hold zero (rot/pos) or one (scale)"""
    bones = set()
    for _, d in seq:
        bones |= set(d)
    zero = (1, 1, 1) if kind == 'scl' else (0, 0, 0)
    for b in sorted(bones):
        keys = [(t, d.get(b, zero)) for t, d in seq]
        (a.rot if kind == 'rot' else a.pos if kind == 'pos' else a.scl)(b, *keys)


def mirror(d):
    o = dict(d)
    for k, v in d.items():
        if k.startswith('right_'):
            o['left_' + k[6:]] = (v[0], -v[1], -v[2])
    return o


# ================================================================ humanoid
def humanoid_set(st):
    """st: dict(weapon=slash|thrust|smash|claw|punch|cast|bow, cast=raise|forward|summon, special=roar|slam|spin|charge|stomp|channel,
    death=back|knees|dissolve, heavy=float, special_len=float, cape, wings, tail(list), skirt, jaw)"""
    A = {}
    heavy = st.get('heavy', 1.0)
    idle = Anim('idle', 3.0, True)
    wave(idle, 'chest', 3.0, (2.5 * heavy, 0, 0))
    wave(idle, 'head', 3.0, (3, 8, 0), phase=1.0)
    wave(idle, 'right_arm', 3.0, (3, 0, 2), phase=0.5)
    wave(idle, 'left_arm', 3.0, (3, 0, -2), phase=2.0)
    idle.pos('base', (0, (0, 0, 0)), (1.5, (0, -0.4 * heavy, 0)), (3.0, (0, 0, 0)))
    _extras(idle, st, 3.0, 0.6)
    A[0] = idle

    def wf(s, k=1.0):
        return {'right_leg': (-30 * s * k, 0, 0), 'right_shin': (max(0, 34 * s) * k + 3, 0, 0), 'left_leg': (30 * s * k, 0, 0),
                'left_shin': (max(0, -34 * s) * k + 3, 0, 0), 'right_arm': (26 * s * k * st.get('arm_swing', 1), 0, 0),
                'left_arm': (-26 * s * k * st.get('arm_swing', 1), 0, 0), 'right_fore': (-15 * k, 0, 0), 'left_fore': (-15 * k, 0, 0),
                'hips': (0, 7 * s, 0), 'chest': (2 * k, -9 * s, 0)}
    walk = Anim('walk', 1.0, True)
    frames(walk, [(0, wf(1)), (0.25, wf(0)), (0.5, wf(-1)), (0.75, wf(0)), (1.0, wf(1))])
    walk.pos('base', (0, (0, 0, 0)), (0.25, (0, 1.2 * heavy, 0)), (0.5, (0, 0, 0)), (0.75, (0, 1.2 * heavy, 0)), (1.0, (0, 0, 0)))
    _extras(walk, st, 1.0, 1.0)
    A[1] = walk
    run = Anim('run', 0.6, True)
    rf = lambda s: {**wf(s, 1.6), 'base': (14, 0, 0), 'head': (-12, 0, 0), 'right_fore': (-70, 0, 0), 'left_fore': (-70, 0, 0)}
    frames(run, [(0, rf(1)), (0.15, rf(0)), (0.3, rf(-1)), (0.45, rf(0)), (0.6, rf(1))])
    run.pos('base', (0, (0, 0, 0)), (0.15, (0, 2.2, 0)), (0.3, (0, 0, 0)), (0.45, (0, 2.2, 0)), (0.6, (0, 0, 0)))
    _extras(run, st, 0.6, 1.6, lift=-25)
    A[6] = run

    # attack
    w = st.get('weapon', 'slash')
    atk = Anim('attack', ATK_LEN)
    if w == 'slash':
        seq = [(0, {}), (0.2, {'right_arm': (-150, 10, 25), 'right_fore': (-30, 0, 0), 'chest': (-8, 22, 0), 'waist': (0, 10, 0), 'left_arm': (-20, 0, -15)}),
               (ATK_HIT, {'right_arm': (-20, -25, -30), 'right_fore': (-5, 0, 0), 'chest': (14, -28, 0), 'waist': (6, -12, 0), 'left_arm': (20, 0, -10)}),
               (0.55, {'right_arm': (10, -20, -25), 'chest': (10, -22, 0), 'waist': (4, -8, 0)}), (ATK_LEN, {})]
    elif w == 'thrust':
        seq = [(0, {}), (0.2, {'right_arm': (-40, 0, 20), 'right_fore': (-80, 0, 0), 'chest': (-4, 30, 0), 'right_leg': (-15, 0, 0), 'left_leg': (15, 0, 0)}),
               (ATK_HIT, {'right_arm': (-88, 0, -5), 'right_fore': (0, 0, 0), 'chest': (14, -20, 0), 'right_leg': (-30, 0, 0), 'right_shin': (25, 0, 0), 'left_leg': (25, 0, 0)}),
               (0.55, {'right_arm': (-80, 0, -5), 'chest': (10, -15, 0), 'right_leg': (-25, 0, 0), 'left_leg': (20, 0, 0)}), (ATK_LEN, {})]
    elif w == 'smash':
        seq = [(0, {}), (0.22, {'right_arm': (-170, 0, 12), 'left_arm': (-170, 0, -12), 'right_fore': (-25, 0, 0), 'left_fore': (-25, 0, 0), 'chest': (-18, 0, 0), 'head': (-10, 0, 0)}),
               (ATK_HIT, {'right_arm': (-45, 0, 8), 'left_arm': (-45, 0, -8), 'right_fore': (-10, 0, 0), 'left_fore': (-10, 0, 0), 'chest': (30, 0, 0), 'waist': (10, 0, 0),
                          'right_leg': (-20, 0, 0), 'right_shin': (30, 0, 0), 'left_leg': (-20, 0, 0), 'left_shin': (30, 0, 0)}),
               (0.55, {'right_arm': (-40, 0, 8), 'left_arm': (-40, 0, -8), 'chest': (25, 0, 0), 'waist': (8, 0, 0), 'right_leg': (-15, 0, 0), 'left_leg': (-15, 0, 0)}), (ATK_LEN, {})]
        atk.pos('base', (0, (0, 0, 0)), (0.22, (0, 1.5, 0)), (ATK_HIT, (0, -3, -2)), (0.55, (0, -2.5, -2)), (ATK_LEN, (0, 0, 0)))
    elif w == 'claw':
        seq = [(0, {}), (0.15, {'right_arm': (-120, 0, 40), 'right_fore': (-40, 0, 0), 'chest': (-6, 20, 0)}),
               (0.28, {'right_arm': (-40, -10, -20), 'right_fore': (-10, 0, 0), 'chest': (10, -20, 0), 'left_arm': (-120, 0, -40), 'left_fore': (-40, 0, 0)}),
               (ATK_HIT + 0.08, {'left_arm': (-40, 10, 20), 'left_fore': (-10, 0, 0), 'chest': (10, 20, 0)}), (ATK_LEN, {})]
    elif w == 'punch':
        seq = [(0, {}), (0.18, {'right_arm': (-20, 0, 25), 'right_fore': (-100, 0, 0), 'chest': (0, 25, 0)}),
               (ATK_HIT, {'right_arm': (-90, 0, 0), 'right_fore': (-5, 0, 0), 'chest': (8, -25, 0), 'left_leg': (15, 0, 0)}), (ATK_LEN, {})]
    elif w == 'bow':
        seq = [(0, {}), (0.2, {'left_arm': (-90, 15, 0), 'left_fore': (0, 0, 0), 'right_arm': (-90, -30, 0), 'right_fore': (-60, 0, 0), 'chest': (0, 35, 0), 'head': (0, -30, 0)}),
               (ATK_HIT, {'left_arm': (-90, 15, 0), 'right_arm': (-85, -50, 0), 'right_fore': (-100, 0, 0), 'chest': (0, 35, 0), 'head': (0, -30, 0)}),
               (0.45, {'left_arm': (-88, 15, 0), 'right_arm': (-70, -10, 20), 'right_fore': (-20, 0, 0), 'chest': (0, 30, 0), 'head': (0, -28, 0)}), (ATK_LEN, {})]
    else:  # cast-like jab
        seq = [(0, {}), (0.2, {'right_arm': (-60, 0, 30), 'right_fore': (-50, 0, 0)}), (ATK_HIT, {'right_arm': (-95, 0, 0), 'right_fore': (0, 0, 0), 'chest': (8, -15, 0)}), (ATK_LEN, {})]
    frames(atk, seq)
    _extras(atk, st, ATK_LEN, 1.2)
    A[2] = atk

    # cast
    c = st.get('cast', 'raise')
    cast = Anim('cast', CAST_LEN)
    if c == 'raise':
        seq = [(0, {}), (0.35, mirror({'right_arm': (-160, 0, 25), 'right_fore': (-20, 0, 0)}) | {'chest': (-15, 0, 0), 'head': (-25, 0, 0)}),
               (CAST_HIT, mirror({'right_arm': (-110, 0, 45), 'right_fore': (0, 0, 0)}) | {'chest': (8, 0, 0), 'head': (5, 0, 0)}),
               (0.85, mirror({'right_arm': (-90, 0, 35)}) | {'chest': (5, 0, 0)}), (CAST_LEN, {})]
    elif c == 'forward':
        seq = [(0, {}), (0.35, mirror({'right_arm': (-40, 0, 40), 'right_fore': (-90, 0, 0)}) | {'chest': (-10, 0, 0)}),
               (CAST_HIT, mirror({'right_arm': (-90, 0, 10), 'right_fore': (0, 0, 0)}) | {'chest': (12, 0, 0), 'head': (8, 0, 0)}),
               (0.85, mirror({'right_arm': (-85, 0, 10)}) | {'chest': (10, 0, 0)}), (CAST_LEN, {})]
    else:  # summon: arms spread wide and up, head back
        seq = [(0, {}), (0.4, mirror({'right_arm': (-30, 0, 100), 'right_fore': (-40, 0, 0)}) | {'chest': (-20, 0, 0), 'head': (-35, 0, 0)}),
               (CAST_HIT, mirror({'right_arm': (-160, 0, 60), 'right_fore': (-10, 0, 0)}) | {'chest': (-25, 0, 0), 'head': (-40, 0, 0)}),
               (0.9, mirror({'right_arm': (-150, 0, 50)}) | {'chest': (-15, 0, 0)}), (CAST_LEN, {})]
    frames(cast, seq)
    cast.pos('base', (0, (0, 0, 0)), (CAST_HIT, (0, 1.5, 0)), (CAST_LEN, (0, 0, 0)))
    _extras(cast, st, CAST_LEN, 1.4)
    A[3] = cast

    # special
    sp = st.get('special', 'roar')
    L = st.get('special_len', 1.6)
    h = L / 2
    spa = Anim('special', L)
    if sp == 'roar':
        frames(spa, [(0, {}), (h * 0.6, mirror({'right_arm': (-10, 0, 30), 'right_fore': (-60, 0, 0)}) | {'chest': (15, 0, 0), 'head': (20, 0, 0)}),
                     (h, mirror({'right_arm': (-40, 0, 85), 'right_fore': (-20, 0, 0)}) | {'chest': (-25, 0, 0), 'head': (-40, 0, 0), 'jaw': (40, 0, 0)}),
                     (h * 1.6, mirror({'right_arm': (-35, 0, 80)}) | {'chest': (-22, 0, 0), 'head': (-35, 0, 0), 'jaw': (35, 0, 0)}), (L, {})])
    elif sp == 'slam':
        frames(spa, [(0, {}), (h * 0.5, mirror({'right_arm': (-170, 0, 15)}) | {'chest': (-15, 0, 0), 'right_leg': (-40, 0, 0), 'right_shin': (60, 0, 0),
                                                                                  'left_leg': (-40, 0, 0), 'left_shin': (60, 0, 0)}),
                     (h, mirror({'right_arm': (-30, 0, 20)}) | {'chest': (35, 0, 0), 'right_leg': (-50, 0, 0), 'right_shin': (80, 0, 0), 'left_leg': (-50, 0, 0), 'left_shin': (80, 0, 0)}),
                     (h * 1.5, mirror({'right_arm': (-25, 0, 20)}) | {'chest': (30, 0, 0), 'right_leg': (-40, 0, 0), 'right_shin': (60, 0, 0), 'left_leg': (-40, 0, 0), 'left_shin': (60, 0, 0)}),
                     (L, {})])
        spa.pos('base', (0, (0, 0, 0)), (h * 0.5, (0, 10, 0)), (h, (0, -5, -3)), (h * 1.5, (0, -4, -3)), (L, (0, 0, 0)))
    elif sp == 'spin':
        spa.rot('base', *[(L * k / 8, (0, k * 360 / 8 * 2 if 0 < k < 8 else (720 if k == 8 else 0), 0)) for k in range(9)], interp='L')
        frames(spa, [(0, {}), (0.2, mirror({'right_arm': (-90, 0, 80)})), (L - 0.2, mirror({'right_arm': (-90, 0, 80)})), (L, {})])
    elif sp == 'charge':
        frames(spa, [(0, {}), (h * 0.6, mirror({'right_arm': (40, 0, 20)}) | {'chest': (35, 0, 0), 'head': (-25, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (30, 0, 0)}),
                     (h, mirror({'right_arm': (50, 0, 15)}) | {'chest': (40, 0, 0), 'head': (-30, 0, 0), 'right_leg': (30, 0, 0), 'left_leg': (-30, 0, 0)}),
                     (h * 1.4, mirror({'right_arm': (50, 0, 15)}) | {'chest': (40, 0, 0), 'head': (-30, 0, 0), 'right_leg': (-30, 0, 0), 'left_leg': (30, 0, 0)}),
                     (h * 1.8, mirror({'right_arm': (45, 0, 15)}) | {'chest': (38, 0, 0), 'right_leg': (30, 0, 0), 'left_leg': (-30, 0, 0)}), (L, {})])
    elif sp == 'stomp':
        frames(spa, [(0, {}), (h * 0.6, {'right_leg': (-70, 0, 0), 'right_shin': (50, 0, 0), 'chest': (-10, 0, 0), 'left_arm': (-30, 0, -30), 'right_arm': (-30, 0, 30)}),
                     (h, {'right_leg': (-5, 0, 0), 'right_shin': (5, 0, 0), 'chest': (15, 0, 0)}), (L, {})])
    else:  # channel: one arm forward, sustained
        frames(spa, [(0, {}), (0.3, {'right_arm': (-95, -10, 0), 'right_fore': (0, 0, 0), 'left_arm': (-40, 0, -40), 'chest': (5, -15, 0), 'head': (5, 0, 0)}),
                     (L - 0.3, {'right_arm': (-95, 10, 0), 'right_fore': (-5, 0, 0), 'left_arm': (-45, 0, -45), 'chest': (5, -10, 0), 'head': (5, 0, 0)}), (L, {})])
    _extras(spa, st, L, 1.5)
    A[4] = spa

    # death
    d = st.get('death', 'back')
    death = Anim('death', 1.5)
    if d == 'back':
        frames(death, [(0, {}), (0.4, mirror({'right_arm': (-60, 0, 30)}) | {'chest': (-25, 0, 0), 'head': (-30, 0, 0)}),
                       (1.0, mirror({'right_arm': (-150, 0, 40)}) | {'base': (-88, 0, 0), 'head': (-10, 0, 0)}), (1.5, mirror({'right_arm': (-160, 0, 50)}) | {'base': (-90, 0, 0)})])
        death.pos('base', (0, (0, 0, 0)), (1.0, (0, -14, 6)), (1.5, (0, -16, 8)))
    elif d == 'knees':
        frames(death, [(0, {}), (0.5, {'right_leg': (-90, 0, 0), 'right_shin': (90, 0, 0), 'left_leg': (-90, 0, 0), 'left_shin': (90, 0, 0), 'chest': (20, 0, 0), 'head': (25, 0, 0)}),
                       (1.0, {'right_leg': (-90, 0, 0), 'right_shin': (90, 0, 0), 'left_leg': (-90, 0, 0), 'left_shin': (90, 0, 0), 'chest': (40, 0, 0), 'head': (35, 0, 0), 'base': (60, 0, 0)}),
                       (1.5, {'right_leg': (-90, 0, 0), 'right_shin': (90, 0, 0), 'left_leg': (-90, 0, 0), 'left_shin': (90, 0, 0), 'chest': (40, 0, 0), 'base': (85, 0, 0)})])
        death.pos('base', (0, (0, 0, 0)), (0.5, (0, -12, 0)), (1.5, (0, -18, -4)))
    else:  # dissolve
        frames(death, [(0, {}), (0.6, mirror({'right_arm': (-120, 0, 60)}) | {'chest': (-25, 0, 0), 'head': (-40, 0, 0)}), (1.5, mirror({'right_arm': (-160, 0, 80)}))])
        death.scl('base', (0, (1, 1, 1)), (0.6, (1.05, 1.05, 1.05)), (1.5, (0.05, 1.6, 0.05)))
        death.pos('base', (0, (0, 0, 0)), (1.5, (0, 6, 0)))
    A[5] = death
    return A


def _extras(a, st, length, intensity, lift=0.0):
    """secondary motion: cape, wings, tails, skirts, wisps, hair"""
    if st.get('cape'):
        wave(a, 'cape', length, (6 * intensity, 0, 3 * intensity), base=(lift * -0.3 + 4 * intensity, 0, 0))
    if st.get('wings'):
        for s, sx in (('r', -1), ('l', 1)):
            wave(a, f'wing_{s}', length, (6 * intensity, 10 * intensity * sx, 8 * intensity * sx), cycles=max(1, round(length * intensity)))
            wave(a, f'wing_{s}2', length, (0, 0, 14 * intensity * sx), phase=0.6, cycles=max(1, round(length * intensity)))
    for i, t in enumerate(st.get('tail', [])):
        wave(a, t, length, (5 * intensity, 12 * intensity, 0), phase=-i * 0.9, base=(lift * 0.2, 0, 0))
    if st.get('skirt'):
        for n, ph in (('skirt_f', 0), ('skirt_b', 1.5), ('skirt_r', 0.7), ('skirt_l', 2.2)):
            wave(a, n, length, (4 * intensity, 0, 0), phase=ph)
    for i in range(st.get('wisps', 0)):
        wave(a, f'wisp{i}', length, (8 * intensity, 10 * intensity, 6), phase=-i * 1.1, cycles=max(1, round(length)))
    for n in st.get('flicker', []):
        wave(a, n, length, (0.12, 0.2, 0.12), base=(1, 1, 1), kind='scl', cycles=max(1, round(length * 2)))
    for n in st.get('sway', []):
        wave(a, n, length, (8 * intensity, 6 * intensity, 6 * intensity), phase=sum(map(ord, n)) % 7)


# ================================================================ quadruped
def quad_set(st):
    A = {}
    tails = st.get('tail', [])
    idle = Anim('idle', 3.0, True)
    wave(idle, 'chest', 3.0, (2, 0, 0))
    wave(idle, 'neck', 3.0, (4, 6, 0), phase=1)
    wave(idle, 'head', 3.0, (3, 8, 0), phase=2)
    wave(idle, 'jaw', 3.0, (4, 0, 0), base=(4, 0, 0))
    for i, t in enumerate(tails):
        wave(idle, t, 3.0, (4, 14, 0), phase=-i)
    _extras(idle, st, 3.0, 0.6)
    A[0] = idle

    def leg(s, k=1.0):
        return {'fr_leg': (-28 * s * k, 0, 0), 'fr_shin': (max(0, -30 * s) * k, 0, 0), 'bl_leg': (-28 * s * k, 0, 0), 'bl_shin': (max(0, 30 * s) * k, 0, 0),
                'fl_leg': (28 * s * k, 0, 0), 'fl_shin': (max(0, 30 * s) * k, 0, 0), 'br_leg': (28 * s * k, 0, 0), 'br_shin': (max(0, -30 * s) * k, 0, 0),
                'chest': (0, 4 * s, 0), 'head': (3 * s, 0, 0)}
    walk = Anim('walk', 1.0, True)
    frames(walk, [(0, leg(1)), (0.25, leg(0)), (0.5, leg(-1)), (0.75, leg(0)), (1.0, leg(1))])
    walk.pos('base', (0, (0, 0, 0)), (0.25, (0, 0.8, 0)), (0.5, (0, 0, 0)), (0.75, (0, 0.8, 0)), (1.0, (0, 0, 0)))
    for i, t in enumerate(tails):
        wave(walk, t, 1.0, (5, 16, 0), phase=-i)
    _extras(walk, st, 1.0, 1.0)
    A[1] = walk

    def gal(s, k=1.0):
        return {'fr_leg': (-45 * s, 0, 0), 'fl_leg': (-40 * s, 0, 0), 'br_leg': (45 * s, 0, 0), 'bl_leg': (40 * s, 0, 0),
                'fr_shin': (max(0, 50 * s), 0, 0), 'fl_shin': (max(0, 50 * s), 0, 0), 'br_shin': (max(0, -40 * s), 0, 0), 'bl_shin': (max(0, -40 * s), 0, 0),
                'body': (6 * s, 0, 0), 'neck': (-8 * s, 0, 0)}
    run = Anim('run', 0.55, True)
    frames(run, [(0, gal(1)), (0.1375, gal(0)), (0.275, gal(-1)), (0.4125, gal(0)), (0.55, gal(1))])
    run.pos('base', (0, (0, 0, 0)), (0.1375, (0, 2.5, 0)), (0.275, (0, 0, 0)), (0.4125, (0, 2.5, 0)), (0.55, (0, 0, 0)))
    for i, t in enumerate(tails):
        wave(run, t, 0.55, (6, 10, 0), phase=-i, base=(-15, 0, 0))
    _extras(run, st, 0.55, 1.6)
    A[6] = run

    atk = Anim('attack', ATK_LEN)
    if st.get('weapon', 'bite') == 'bite':
        frames(atk, [(0, {}), (0.2, {'neck': (-25, 0, 0), 'head': (-20, 0, 0), 'jaw': (45, 0, 0), 'chest': (-6, 0, 0), 'fr_leg': (10, 0, 0), 'fl_leg': (10, 0, 0)}),
                     (ATK_HIT, {'neck': (25, 0, 0), 'head': (15, 0, 0), 'jaw': (0, 0, 0), 'chest': (8, 0, 0), 'fr_leg': (-25, 0, 0), 'fl_leg': (-25, 0, 0)}),
                     (0.55, {'neck': (15, 0, 0), 'head': (10, 0, 0), 'chest': (5, 0, 0)}), (ATK_LEN, {})])
        atk.pos('base', (0, (0, 0, 0)), (0.2, (0, 0, 2)), (ATK_HIT, (0, 0, -5)), (ATK_LEN, (0, 0, 0)))
    else:  # gore / headbutt
        frames(atk, [(0, {}), (0.2, {'neck': (-30, 0, 0), 'head': (-25, 0, 0), 'chest': (-10, 0, 0)}),
                     (ATK_HIT, {'neck': (35, 0, 0), 'head': (25, 0, 0), 'chest': (10, 0, 0), 'br_leg': (25, 0, 0), 'bl_leg': (25, 0, 0)}), (ATK_LEN, {})])
        atk.pos('base', (0, (0, 0, 0)), (ATK_HIT, (0, 0, -6)), (ATK_LEN, (0, 0, 0)))
    A[2] = atk

    cast = Anim('cast', CAST_LEN)   # rear up and roar
    frames(cast, [(0, {}), (0.35, {'body': (-30, 0, 0), 'fr_leg': (-60, 0, 0), 'fl_leg': (-50, 0, 0), 'fr_shin': (60, 0, 0), 'fl_shin': (60, 0, 0),
                                   'neck': (-20, 0, 0), 'head': (-20, 0, 0), 'jaw': (40, 0, 0), 'br_leg': (30, 0, 0), 'bl_leg': (30, 0, 0)}),
                  (CAST_HIT, {'body': (-35, 0, 0), 'fr_leg': (-70, 0, 0), 'fl_leg': (-70, 0, 0), 'fr_shin': (40, 0, 0), 'fl_shin': (40, 0, 0), 'neck': (-25, 0, 0),
                              'head': (-25, 0, 0), 'jaw': (50, 0, 0), 'br_leg': (35, 0, 0), 'bl_leg': (35, 0, 0)}),
                  (0.9, {'body': (5, 0, 0), 'neck': (10, 0, 0), 'jaw': (20, 0, 0)}), (CAST_LEN, {})])
    cast.pos('base', (0, (0, 0, 0)), (CAST_HIT, (0, 3, 2)), (0.9, (0, -1, 0)), (CAST_LEN, (0, 0, 0)))
    A[3] = cast

    L = st.get('special_len', 1.6)
    h = L / 2
    sp = Anim('special', L)
    if st.get('special', 'charge') == 'charge':
        frames(sp, [(0, {}), (h * 0.5, {'neck': (25, 0, 0), 'head': (20, 0, 0), 'chest': (8, 0, 0), 'br_leg': (30, 0, 0), 'bl_leg': (30, 0, 0)})]
               + [(h * 0.5 + (L - h * 0.5 - 0.2) * k / 6, {**gal(1 if k % 2 == 0 else -1), 'neck': (25, 0, 0), 'head': (20, 0, 0)}) for k in range(1, 7)] + [(L, {})])
    else:  # stomp / quake
        frames(sp, [(0, {}), (h * 0.7, {'body': (-25, 0, 0), 'fr_leg': (-50, 0, 0), 'fl_leg': (-50, 0, 0), 'neck': (-15, 0, 0)}),
                    (h, {'body': (10, 0, 0), 'fr_leg': (-10, 0, 0), 'fl_leg': (-10, 0, 0), 'neck': (20, 0, 0), 'head': (10, 0, 0)}), (L, {})])
        sp.pos('base', (0, (0, 0, 0)), (h * 0.7, (0, 4, 1)), (h, (0, -1.5, 0)), (L, (0, 0, 0)))
    _extras(sp, st, L, 1.4)
    A[4] = sp

    death = Anim('death', 1.5)
    frames(death, [(0, {}), (0.6, {'base': (0, 0, 70), 'fr_leg': (-20, 0, 20), 'fl_leg': (20, 0, 20), 'neck': (20, 0, 0), 'jaw': (30, 0, 0)}),
                   (1.5, {'base': (0, 0, 90), 'fr_leg': (-30, 0, 30), 'fl_leg': (30, 0, 30), 'br_leg': (-20, 0, 20), 'bl_leg': (20, 0, 20), 'neck': (30, 0, 0),
                          'head': (10, 0, 0), 'jaw': (25, 0, 0)})])
    death.pos('base', (0, (0, 0, 0)), (0.6, (0, -st.get('fall', 8), 0)), (1.5, (0, -st.get('fall', 8) - 2, 0)))
    A[5] = death
    return A


# ================================================================ arthropod
def arthro_set(st, legs):
    A = {}
    idle = Anim('idle', 2.4, True)
    wave(idle, 'body', 2.4, (1.5, 2, 0))
    wave(idle, 'abdomen', 2.4, (4, 3, 0), phase=1)
    wave(idle, 'mand_r', 2.4, (0, 10, 0), cycles=2)
    wave(idle, 'mand_l', 2.4, (0, -10, 0), cycles=2)
    for i in range(st.get('sting', 0)):
        wave(idle, f'sting{i}', 2.4, (6, 4, 0), phase=-i * 0.7)
    for k, (n, sx) in enumerate(legs):
        wave(idle, n + 'a', 2.4, (0, 0, 3 * sx), phase=k)
    A[0] = idle

    def gait(s):
        d = {}
        for k, (n, sx) in enumerate(legs):
            pair = int(n[3])
            grp = (pair + (0 if sx < 0 else 1)) % 2
            v = s if grp == 0 else -s
            d[n] = (0, 22 * v * sx, 0)
            d[n + 'a'] = (0, 0, (-14 if v > 0 else 0) * sx)
        return d
    for slot, L in ((1, 0.8), (6, 0.45)):
        w = Anim('walk' if slot == 1 else 'run', L, True)
        frames(w, [(0, gait(1)), (L * 0.25, gait(0)), (L * 0.5, gait(-1)), (L * 0.75, gait(0)), (L, gait(1))])
        w.pos('base', (0, (0, 0, 0)), (L * 0.25, (0, 0.6, 0)), (L * 0.5, (0, 0, 0)), (L * 0.75, (0, 0.6, 0)), (L, (0, 0, 0)))
        for i in range(st.get('sting', 0)):
            wave(w, f'sting{i}', L, (5, 3, 0), phase=-i * 0.7)
        A[slot] = w

    atk = Anim('attack', ATK_LEN)
    if st.get('sting'):
        seq = [(0, {}), (0.2, {f'sting{i}': (-20, 0, 0) for i in range(st['sting'])} | {'stinger': (-30, 0, 0), 'body': (-8, 0, 0)}),
               (ATK_HIT, {f'sting{i}': (30, 0, 0) for i in range(st['sting'])} | {'stinger': (40, 0, 0), 'body': (10, 0, 0)}), (ATK_LEN, {})]
    else:
        seq = [(0, {}), (0.2, {'head': (-20, 0, 0), 'mand_r': (0, 40, 0), 'mand_l': (0, -40, 0), 'body': (-10, 0, 0), 'leg0ra': (0, 0, 30), 'leg0la': (0, 0, -30)}),
               (ATK_HIT, {'head': (15, 0, 0), 'mand_r': (0, -15, 0), 'mand_l': (0, 15, 0), 'body': (10, 0, 0)}), (ATK_LEN, {})]
    if st.get('pincers'):
        seq[1][1].update({'pincer_r': (-30, -30, 0), 'pincer_l': (-30, 30, 0), 'pincer_rf': (0, -40, 0), 'pincer_lf': (0, 40, 0)})
        seq[2][1].update({'pincer_r': (10, 20, 0), 'pincer_l': (10, -20, 0), 'pincer_rf': (0, 10, 0), 'pincer_lf': (0, -10, 0)})
    frames(atk, seq)
    atk.pos('base', (0, (0, 0, 0)), (ATK_HIT, (0, 0, -3)), (ATK_LEN, (0, 0, 0)))
    A[2] = atk
    cast = Anim('cast', CAST_LEN)
    up = {'body': (-35, 0, 0), 'head': (-10, 0, 0), 'leg0ra': (0, 0, 40), 'leg0la': (0, 0, -40), 'leg0r': (-40, 0, 0), 'leg0l': (-40, 0, 0),
          'mand_r': (0, 35, 0), 'mand_l': (0, -35, 0)}
    frames(cast, [(0, {}), (0.4, up), (CAST_HIT, {**up, 'body': (-40, 0, 0)}), (0.9, {'body': (5, 0, 0)}), (CAST_LEN, {})])
    cast.pos('base', (0, (0, 0, 0)), (0.4, (0, 4, 3)), (CAST_HIT, (0, 5, 3)), (0.9, (0, 0, 0)))
    A[3] = cast
    L = st.get('special_len', 1.6)
    sp = Anim('special', L)
    frames(sp, [(0, {}), (L * 0.35, {'body': (-15, 0, 0), 'abdomen': (-30, 0, 0)}), (L * 0.5, {'body': (12, 0, 0), 'abdomen': (25, 0, 0)}),
                (L * 0.7, {'body': (-10, 0, 0), 'abdomen': (-20, 0, 0)}), (L, {})])
    sp.pos('base', (0, (0, 0, 0)), (L * 0.35, (0, 3, 0)), (L * 0.5, (0, -1, 0)), (L, (0, 0, 0)))
    A[4] = sp
    death = Anim('death', 1.5)
    curl = {}
    for n, sx in legs:
        curl[n + 'a'] = (0, 0, 50 * sx)
        curl[n + 'b'] = (0, 0, 40 * sx)
    frames(death, [(0, {}), (0.5, {**curl, 'base': (0, 0, 120)}), (1.5, {**curl, 'base': (0, 0, 180)})])
    death.pos('base', (0, (0, 0, 0)), (0.5, (0, 4, 0)), (1.5, (0, -st.get('fall', 6), 0)))
    A[5] = death
    return A


# ================================================================ floater
def floater_set(st):
    st = dict(st)
    st.setdefault('wisps', 3)
    A = {}
    idle = Anim('idle', 2.6, True)
    idle.pos('base', (0, (0, 0, 0)), (1.3, (0, 2.0, 0)), (2.6, (0, 0, 0)))
    wave(idle, 'chest', 2.6, (4, 4, 3))
    wave(idle, 'head', 2.6, (4, 10, 0), phase=1)
    wave(idle, 'right_arm', 2.6, (8, 0, 6), phase=0.5, base=(-10, 0, 10))
    wave(idle, 'left_arm', 2.6, (8, 0, -6), phase=2.0, base=(-10, 0, -10))
    _extras(idle, st, 2.6, 0.8)
    A[0] = idle
    for slot, L, lean in ((1, 1.4, 18), (6, 0.8, 32)):
        m = Anim('move', L, True)
        m.pos('base', (0, (0, 0, 0)), (L / 2, (0, 1.5, 0)), (L, (0, 0, 0)))
        wave(m, 'chest', L, (3, 0, 4), base=(lean, 0, 0))
        wave(m, 'head', L, (2, 4, 0), base=(-lean * 0.7, 0, 0))
        wave(m, 'right_arm', L, (10, 0, 4), base=(25, 0, 15))
        wave(m, 'left_arm', L, (10, 0, -4), phase=math.pi, base=(25, 0, -15))
        _extras(m, st, L, 1.4)
        A[slot] = m
    hst = dict(st)
    sub = humanoid_set({'weapon': st.get('weapon', 'claw'), 'cast': st.get('cast', 'raise'), 'special': st.get('special', 'spin'),
                        'special_len': st.get('special_len', 1.6), 'death': 'dissolve'})
    for slot in (2, 3, 4, 5):
        a = sub[slot]
        a.ch = [c for c in a.ch if not (c[0] in ('right_leg', 'left_leg', 'right_shin', 'left_shin', 'hips', 'waist'))]
        _extras(a, hst, a.length, 1.6)
        A[slot] = a
    return A


# ================================================================ flyer
def flyer_set(st):
    A = {}
    tails = st.get('tail', [])

    def flap(a, L, k=1.0, cycles=2, pitch=0.0):
        keys1, keys2, keysr, keysr2, body = [], [], [], [], []
        n = 8 * cycles
        for i in range(n + 1):
            t = L * i / n
            ph = math.tau * cycles * i / n
            up = math.sin(ph)
            keys1.append((t, (0, 0, -40 * up * k)))
            keysr.append((t, (0, 0, 40 * up * k)))
            keys2.append((t, (0, 0, -25 * math.sin(ph - 0.8) * k)))
            keysr2.append((t, (0, 0, 25 * math.sin(ph - 0.8) * k)))
            body.append((t, (0, -1.2 * up * k, 0)))
        a.rot('wing_l', *keys1)
        a.rot('wing_r', *keysr)
        a.rot('wing_l2', *keys2)
        a.rot('wing_r2', *keysr2)
        a.pos('base', *body)
        if pitch:
            a.rot('body', (0, (pitch, 0, 0)), (L, (pitch, 0, 0)))
        for i, tn in enumerate(tails):
            wave(a, tn, L, (6, 8, 0), phase=-i, cycles=cycles)
    idle = Anim('idle', 1.6, True)
    flap(idle, 1.6, 0.8, 2)
    wave(idle, 'head', 1.6, (3, 8, 0))
    A[0] = idle
    w = Anim('fly', 1.0, True)
    flap(w, 1.0, 1.0, 2, pitch=12)
    A[1] = w
    r = Anim('fast', 0.6, True)
    flap(r, 0.6, 1.1, 2, pitch=22)
    A[6] = r
    atk = Anim('attack', ATK_LEN)
    frames(atk, [(0, {}), (0.2, {'body': (-30, 0, 0), 'head': (-20, 0, 0), 'wing_l': (0, 0, -60), 'wing_r': (0, 0, 60), 'leg_r': (-40, 0, 0), 'leg_l': (-40, 0, 0)}),
                 (ATK_HIT, {'body': (35, 0, 0), 'head': (15, 0, 0), 'wing_l': (0, 0, 40), 'wing_r': (0, 0, -40), 'leg_r': (-90, 0, 0), 'leg_l': (-90, 0, 0)}),
                 (ATK_LEN, {})])
    atk.pos('base', (0, (0, 0, 0)), (0.2, (0, 3, 2)), (ATK_HIT, (0, -4, -5)), (ATK_LEN, (0, 0, 0)))
    A[2] = atk
    cast = Anim('cast', CAST_LEN)
    frames(cast, [(0, {}), (0.4, {'wing_l': (0, 0, -70), 'wing_r': (0, 0, 70), 'wing_l2': (0, 0, -20), 'wing_r2': (0, 0, 20), 'head': (-30, 0, 0), 'body': (-20, 0, 0)}),
                  (CAST_HIT, {'wing_l': (0, 0, 30), 'wing_r': (0, 0, -30), 'head': (10, 0, 0), 'body': (10, 0, 0)}), (CAST_LEN, {})])
    cast.pos('base', (0, (0, 0, 0)), (0.4, (0, 4, 0)), (CAST_HIT, (0, -1, 0)), (CAST_LEN, (0, 0, 0)))
    A[3] = cast
    L = st.get('special_len', 1.6)
    sp = Anim('special', L)
    flap(sp, L, 1.3, 3, pitch=-10)
    sp.rot('head', (0, (0, 0, 0)), (L / 2, (-30, 0, 0)), (L, (0, 0, 0)))
    A[4] = sp
    death = Anim('death', 1.5)
    frames(death, [(0, {}), (0.4, {'wing_l': (0, 0, 80), 'wing_r': (0, 0, -80), 'body': (40, 0, 30)}),
                   (1.5, {'wing_l': (0, 0, 100), 'wing_r': (0, 0, -100), 'body': (80, 0, 60), 'head': (30, 0, 0)})])
    death.pos('base', (0, (0, 0, 0)), (0.5, (0, -6, 0)), (1.0, (0, -st.get('fall', 14), 0)), (1.5, (0, -st.get('fall', 14), 0)))
    A[5] = death
    return A


# ================================================================ orb
def orb_set(st, n_t):
    A = {}

    def tent(a, L, k=1.0, cycles=1):
        for t in range(n_t):
            for i in range(3):
                wave(a, f't{t}_{i}', L, (14 * k, 0, 12 * k), phase=t * 1.1 - i * 0.9, cycles=cycles)
    idle = Anim('idle', 2.4, True)
    idle.pos('base', (0, (0, 0, 0)), (1.2, (0, 2, 0)), (2.4, (0, 0, 0)))
    wave(idle, 'body', 2.4, (6, 10, 4))
    wave(idle, 'lid', 2.4, (6, 0, 0), base=(4, 0, 0))
    tent(idle, 2.4, 0.8)
    for n in st.get('spin', []):
        idle.rot(n, *[(2.4 * k / 4, (0, 90 * k, 0)) for k in range(5)], interp='L')
    A[0] = idle
    for slot, L in ((1, 1.2), (6, 0.7)):
        m = Anim('move', L, True)
        m.pos('base', (0, (0, 0, 0)), (L / 2, (0, 1.5, 0)), (L, (0, 0, 0)))
        wave(m, 'body', L, (4, 6, 2), base=(10, 0, 0))
        tent(m, L, 1.3, 1)
        A[slot] = m
    atk = Anim('attack', ATK_LEN)
    atk.scl('body', (0, (1, 1, 1)), (0.2, (0.85, 0.85, 0.85)), (ATK_HIT, (1.25, 1.25, 1.25)), (ATK_LEN, (1, 1, 1)))
    atk.pos('base', (0, (0, 0, 0)), (0.2, (0, 0, 3)), (ATK_HIT, (0, 0, -5)), (ATK_LEN, (0, 0, 0)))
    tent(atk, ATK_LEN, 2.5, 2)
    A[2] = atk
    cast = Anim('cast', CAST_LEN)
    cast.rot('lid', (0, (0, 0, 0)), (0.3, (-35, 0, 0)), (CAST_HIT, (-50, 0, 0)), (1.0, (-40, 0, 0)), (CAST_LEN, (0, 0, 0)))
    cast.scl('body', (0, (1, 1, 1)), (0.4, (1.15, 1.15, 1.15)), (CAST_HIT, (0.9, 0.9, 0.9)), (CAST_LEN, (1, 1, 1)))
    tent(cast, CAST_LEN, 2.0, 2)
    A[3] = cast
    L = st.get('special_len', 2.4)
    sp = Anim('special', L)
    sp.rot('lid', (0, (0, 0, 0)), (0.3, (-50, 0, 0)), (L - 0.3, (-50, 0, 0)), (L, (0, 0, 0)))
    wave(sp, 'body', L, (3, 3, 3), cycles=4)
    tent(sp, L, 2.5, 3)
    A[4] = sp
    death = Anim('death', 1.5)
    death.scl('body', (0, (1, 1, 1)), (0.3, (1.3, 1.3, 1.3)), (1.5, (0.1, 0.1, 0.1)))
    death.pos('base', (0, (0, 0, 0)), (1.5, (0, -st.get('fall', 10), 0)))
    death.rot('body', (0, (0, 0, 0)), (1.5, (90, 180, 0)))
    A[5] = death
    return A


# ================================================================ blob
def blob_set(st):
    A = {}
    idle = Anim('idle', 2.4, True)
    idle.scl('body', (0, (1, 1, 1)), (0.6, (1.05, 0.94, 1.05)), (1.2, (0.97, 1.05, 0.97)), (1.8, (1.04, 0.96, 1.04)), (2.4, (1, 1, 1)))
    wave(idle, 'jaw', 2.4, (10, 0, 0), base=(12, 0, 0))
    for k in range(st.get('arms', 4)):
        wave(idle, f'arm{k}', 2.4, (15, 10, 10), phase=k)
        wave(idle, f'arm{k}b', 2.4, (20, 0, 10), phase=k + 1)
    A[0] = idle
    for slot, L in ((1, 1.2), (6, 0.7)):
        m = Anim('move', L, True)
        m.scl('body', (0, (1, 1, 1)), (L * 0.25, (1.1, 0.88, 1.1)), (L * 0.5, (0.94, 1.08, 0.94)), (L * 0.75, (1.1, 0.88, 1.1)), (L, (1, 1, 1)))
        m.rot('body', (0, (0, 0, 4)), (L / 2, (0, 0, -4)), (L, (0, 0, 4)))
        for k in range(st.get('arms', 4)):
            wave(m, f'arm{k}', L, (30, 10, 10), phase=k * 1.4)
        A[slot] = m
    atk = Anim('attack', ATK_LEN)
    atk.rot('jaw', (0, (0, 0, 0)), (0.2, (60, 0, 0)), (ATK_HIT, (0, 0, 0)), (ATK_LEN, (0, 0, 0)))
    atk.rot('body', (0, (0, 0, 0)), (0.2, (-12, 0, 0)), (ATK_HIT, (18, 0, 0)), (ATK_LEN, (0, 0, 0)))
    atk.pos('base', (0, (0, 0, 0)), (ATK_HIT, (0, 0, -6)), (ATK_LEN, (0, 0, 0)))
    for k in range(st.get('arms', 4)):
        atk.rot(f'arm{k}', (0, (0, 0, 0)), (0.2, (-80, 0, 0)), (ATK_HIT, (40, 0, 0)), (ATK_LEN, (0, 0, 0)))
    A[2] = atk
    cast = Anim('cast', CAST_LEN)
    cast.scl('body', (0, (1, 1, 1)), (0.45, (1.25, 1.2, 1.25)), (CAST_HIT, (0.9, 0.9, 0.9)), (CAST_LEN, (1, 1, 1)))
    cast.rot('jaw', (0, (0, 0, 0)), (0.45, (70, 0, 0)), (CAST_HIT, (70, 0, 0)), (CAST_LEN, (0, 0, 0)))
    A[3] = cast
    L = st.get('special_len', 1.6)
    sp = Anim('special', L)
    sp.scl('body', (0, (1, 1, 1)), (L * 0.4, (1.3, 0.7, 1.3)), (L * 0.5, (0.8, 1.4, 0.8)), (L * 0.7, (1.2, 0.85, 1.2)), (L, (1, 1, 1)))
    sp.pos('base', (0, (0, 0, 0)), (L * 0.5, (0, 8, 0)), (L * 0.6, (0, 0, 0)))
    A[4] = sp
    death = Anim('death', 1.5)
    death.scl('base', (0, (1, 1, 1)), (0.5, (1.2, 0.6, 1.2)), (1.5, (1.6, 0.1, 1.6)))
    death.rot('jaw', (0, (0, 0, 0)), (0.5, (60, 0, 0)), (1.5, (70, 0, 0)))
    A[5] = death
    return A


# ================================================================ serpent
def serpent_set(st, segs):
    A = {}

    def slither(a, L, amp=12, cycles=1):
        for i, n in enumerate(segs[1:], 1):
            wave(a, n, L, (0, amp, 0), phase=-i * 1.0, cycles=cycles)
        wave(a, 'seg0', L, (0, amp * 0.6, 0), cycles=cycles)
    idle = Anim('idle', 2.6, True)
    slither(idle, 2.6, 6)
    wave(idle, 'neck0', 2.6, (5, 4, 0), phase=1)
    wave(idle, 'head', 2.6, (4, 8, 0), phase=2)
    wave(idle, 'jaw', 2.6, (6, 0, 0), base=(6, 0, 0))
    A[0] = idle
    for slot, L, amp in ((1, 1.0, 16), (6, 0.6, 20)):
        m = Anim('move', L, True)
        slither(m, L, amp)
        wave(m, 'neck0', L, (3, 6, 0))
        A[slot] = m
    atk = Anim('attack', ATK_LEN)
    frames(atk, [(0, {}), (0.2, {'neck0': (-25, 0, 0), 'neck1': (-20, 0, 0), 'head': (-10, 0, 0), 'jaw': (50, 0, 0)}),
                 (ATK_HIT, {'neck0': (30, 0, 0), 'neck1': (25, 0, 0), 'head': (10, 0, 0), 'jaw': (0, 0, 0)}), (0.55, {'neck0': (20, 0, 0), 'neck1': (15, 0, 0)}), (ATK_LEN, {})])
    atk.pos('base', (0, (0, 0, 0)), (ATK_HIT, (0, 0, -6)), (ATK_LEN, (0, 0, 0)))
    A[2] = atk
    cast = Anim('cast', CAST_LEN)
    frames(cast, [(0, {}), (0.4, {'neck0': (-30, 0, 0), 'neck1': (-15, 0, 0), 'head': (-20, 0, 0), 'jaw': (60, 0, 0)}),
                  (CAST_HIT, {'neck0': (-25, 0, 0), 'neck1': (5, 0, 0), 'head': (15, 0, 0), 'jaw': (60, 0, 0)}), (0.9, {'jaw': (30, 0, 0)}), (CAST_LEN, {})])
    A[3] = cast
    L = st.get('special_len', 1.6)
    sp = Anim('special', L)
    slither(sp, L, 28, 2)
    sp.rot('jaw', (0, (0, 0, 0)), (L / 2, (55, 0, 0)), (L, (0, 0, 0)))
    A[4] = sp
    death = Anim('death', 1.5)
    frames(death, [(0, {}), (0.5, {'neck0': (40, 0, 0), 'neck1': (30, 0, 0), 'jaw': (40, 0, 0), 'seg0': (0, 0, 60)}),
                   (1.5, {'neck0': (60, 0, 0), 'neck1': (30, 0, 0), 'jaw': (50, 0, 0), 'seg0': (0, 0, 90)})])
    death.pos('base', (0, (0, 0, 0)), (1.5, (0, -2, 0)))
    A[5] = death
    return A


# ================================================================ golem
def golem_set(st):
    st = dict(st)
    st.setdefault('heavy', 2.0)
    st.setdefault('weapon', 'smash')
    st.setdefault('arm_swing', 0.6)
    return humanoid_set(st)
