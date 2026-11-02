"""Hatman keyframe animations (degrees, vanilla additive semantics)."""
import math
from core import Anim
from hatman import M

NAMES = [n for n in M.order if n not in ('hat',)]
FING = [f'{s}_f{i}{k}' for s in ('right', 'left') for i in range(4) for k in 'ab'] + \
       [f'{s}_thumb_{k}' for s in ('right', 'left') for k in 'ab']


def mirror(d):
    o = {}
    for k, v in d.items():
        if k.startswith('right_'):
            o['left_' + k[6:]] = (v[0], -v[1], -v[2])
        elif k.startswith('left_'):
            o['right_' + k[5:]] = (v[0], -v[1], -v[2])
        else:
            o[k] = (v[0], -v[1], -v[2])
    return o


def sym(d):
    o = dict(d)
    for k, v in d.items():
        if k.startswith('right_'):
            o.setdefault('left_' + k[6:], (v[0], -v[1], -v[2]))
    return o


def fingers(curl, spread=0.0, side=None, tip=None):
    """curl > 0 closes the hand, spread fans fingers sideways"""
    d = {}
    for s in ([side] if side else ('right', 'left')):
        sg = -1 if s == 'right' else 1
        for i in range(4):
            fan = (i - 1.5) * spread * sg
            d[f'{s}_f{i}a'] = (-curl, 0, fan)
            d[f'{s}_f{i}b'] = (-(tip if tip is not None else curl * 1.2), 0, 0)
        d[f'{s}_thumb_a'] = (-curl * 0.6, 0, -sg * spread)
        d[f'{s}_thumb_b'] = (-curl * 0.8, 0, 0)
    return d


def merge(*ds):
    o = {}
    for d in ds:
        o.update(d)
    return o


def seq(a, frames, interp='C', pos=None, extra_names=()):
    """frames: [(t, {bone: rot})]; missing bones -> zero so poses blend cleanly"""
    names = set()
    for _, d in frames:
        names |= set(d)
    names |= set(extra_names)
    for b in sorted(names):
        keys = [(t, d.get(b, (0, 0, 0)), interp) for t, d in frames]
        a.rot(b, *keys)
    if pos:
        for b, keys in pos.items():
            a.pos(b, *[(k[0], k[1], k[2] if len(k) > 2 else interp) for k in keys])
    return a


A = {}

# ------------------------------------------------------------------ IDLE
idle_a = merge(sym({'right_upper': (-4, 0, 4), 'right_fore': (-8, 0, 0)}), fingers(6, 2),
               {'chest': (4, 0, 0), 'head': (6, 0, 8), 'neck': (2, 0, 0)})
idle_b = merge(sym({'right_upper': (-2, 0, 6), 'right_fore': (-12, 0, 0)}), fingers(14, 6),
               {'chest': (-2, 0, 0), 'waist': (-2, 0, 0), 'head': (-4, 12, -10), 'neck': (0, 8, 0)})
idle_c = merge(sym({'right_upper': (-5, 0, 3), 'right_fore': (-6, 0, 0)}),
               fingers(4, 1), {'right_f0a': (-35, 0, -10), 'right_f1a': (-25, 0, 0), 'chest': (5, 0, 0), 'head': (10, -18, 22)})
A['IDLE'] = seq(Anim('idle', 4.0, True),
                [(0, idle_a), (1.1, idle_b), (1.25, idle_b), (2.2, idle_c), (2.32, merge(idle_c, {'head': (10, -18, 34)})),
                 (3.0, idle_c), (4.0, idle_a)],
                pos={'base': [(0, (0, 0, 0)), (2.0, (0, -0.4, 0)), (4.0, (0, 0, 0))]})

# ------------------------------------------------------------------ WALK (gangly, long strides)
def walk_frame(p, k=1.0, lean=8):
    s = math.sin(p * math.tau); c = math.cos(p * math.tau)
    d = {
        'right_thigh': (-30 * s * k, 0, 2), 'left_thigh': (30 * s * k, 0, -2),
        'right_shin': (max(0, 40 * c) * k + 6, 0, 0), 'left_shin': (max(0, -40 * c) * k + 6, 0, 0),
        'right_foot': (-10 * s * k, 0, 0), 'left_foot': (10 * s * k, 0, 0),
        'right_upper': (24 * s * k, 0, 5), 'left_upper': (-24 * s * k, 0, -5),
        'right_fore': (-14 + 12 * s * k, 0, 0), 'left_fore': (-14 - 12 * s * k, 0, 0),
        'right_hand': (16 * s * k, 0, 0), 'left_hand': (-16 * s * k, 0, 0),
        'waist': (lean * 0.4, 8 * s * k, 0), 'chest': (lean, -10 * s * k, 3 * s * k),
        'head': (-lean * 0.6, 8 * s * k, -6 * s * k), 'hips': (0, -6 * s * k, 0),
    }
    d.update(fingers(10 + 8 * abs(s), 3))
    return d


def cyc(name, length, fn, n=8, bob=0.0, bob_base=0.0):
    fr = [(length * i / n, fn(i / n)) for i in range(n + 1)]
    pos = None
    if bob:
        pos = {'base': [(length * i / n, (0, bob_base - bob * abs(math.cos(i / n * math.tau)), 0)) for i in range(n + 1)]}
    return seq(Anim(name, length, True), fr, pos=pos)


A['WALK'] = cyc('walk', 1.6, lambda p: walk_frame(p), bob=0.8)


# ------------------------------------------------------------------ RUN (loping, bent forward, arms trailing)
def run_frame(p):
    s = math.sin(p * math.tau); c = math.cos(p * math.tau)
    d = {
        'right_thigh': (-55 * s - 15, 0, 6), 'left_thigh': (55 * s - 15, 0, -6),
        'right_shin': (max(0, 75 * c) + 15, 0, 0), 'left_shin': (max(0, -75 * c) + 15, 0, 0),
        'right_foot': (-20 * s, 0, 0), 'left_foot': (20 * s, 0, 0),
        'right_upper': (55 + 25 * s, 0, 18), 'left_upper': (55 - 25 * s, 0, -18),
        'right_fore': (-30 + 20 * c, 0, 0), 'left_fore': (-30 - 20 * c, 0, 0),
        'right_hand': (25, 0, 0), 'left_hand': (25, 0, 0),
        'waist': (18, 6 * s, 0), 'chest': (24, -8 * s, 6 * s), 'neck': (-10, 0, 0),
        'head': (-28, 10 * s, -10 * s), 'hips': (8, -8 * s, 0),
    }
    d.update(fingers(-8, 10 + 4 * s))
    return d


A['RUN'] = cyc('run', 0.9, run_frame, bob=2.0, bob_base=-1.0)


# ------------------------------------------------------------------ CRAWL (all fours, spider-like)
def crawl_frame(p):
    s = math.sin(p * math.tau); c = math.cos(p * math.tau)
    d = {
        'waist': (55, 0, 6 * s), 'chest': (30, 8 * s, 0), 'neck': (-20, 0, 0), 'head': (-55, 12 * s, 0),
        'right_upper': (-75 + 25 * s, 0, 25), 'left_upper': (-75 - 25 * s, 0, -25),
        'right_fore': (-50 + max(0, 30 * c), 0, 0), 'left_fore': (-50 + max(0, -30 * c), 0, 0),
        'right_hand': (60, 0, 0), 'left_hand': (60, 0, 0),
        'right_thigh': (-80 - 25 * s, 0, 35), 'left_thigh': (-80 + 25 * s, 0, -35),
        'right_shin': (125 - max(0, 30 * c), 0, 0), 'left_shin': (125 - max(0, -30 * c), 0, 0),
        'right_foot': (-40, 0, 0), 'left_foot': (-40, 0, 0),
    }
    d.update(fingers(-20, 14))
    return d


A['CRAWL'] = cyc('crawl', 1.2, crawl_frame, bob=0.6, bob_base=-15.0)


# ------------------------------------------------------------------ STALK (slow tip-toe creep, hands raised, fingers spread)
def stalk_frame(p):
    s = math.sin(p * math.tau); c = math.cos(p * math.tau)
    d = {
        'right_thigh': (-35 - 30 * max(0, s), 0, 12), 'left_thigh': (-35 - 30 * max(0, -s), 0, -12),
        'right_shin': (55 + 25 * max(0, s), 0, 0), 'left_shin': (55 + 25 * max(0, -s), 0, 0),
        'right_foot': (-25, 0, 0), 'left_foot': (-25, 0, 0),
        'right_upper': (-35 + 10 * s, 0, 30), 'left_upper': (-35 - 10 * s, 0, -30),
        'right_fore': (-55, 0, -10), 'left_fore': (-55, 0, 10),
        'right_hand': (-30, 0, 0), 'left_hand': (-30, 0, 0),
        'waist': (16, 5 * s, 0), 'chest': (18, -6 * s, 0), 'head': (-30, 14 * c, 8 * s),
    }
    d.update(fingers(-10 + 10 * c, 16))
    return d


A['STALK'] = cyc('stalk', 2.4, stalk_frame, bob=0.5, bob_base=-5.0)

# ------------------------------------------------------------------ FROZEN: the photograph pose (observed / statue)
PHOTO = merge({
    'right_upper': (-15, 10, 62), 'right_fore': (-5, 0, 22), 'right_hand': (10, 0, 150),
    'left_upper': (-10, 0, -112), 'left_fore': (-10, 0, 108), 'left_hand': (-10, 0, 8),
    'right_thigh': (-55, 0, 62), 'right_shin': (95, 0, 0), 'right_foot': (-35, 0, 0),
    'left_thigh': (-35, 0, -48), 'left_shin': (75, 0, 0), 'left_foot': (-20, 0, 0),
    'waist': (14, 10, -8), 'chest': (10, 12, -6), 'neck': (-6, 0, 0), 'head': (-12, -14, 16),
}, fingers(-14, 22, 'right'), fingers(-6, 14, 'left'))
PHOTO_B = merge(PHOTO, {'head': (-10, -10, 20), 'right_hand': (12, 0, 154), 'chest': (11, 13, -5)},
                fingers(-18, 25, 'right'))
A['FROZEN'] = seq(Anim('frozen', 3.0, True),
                  [(0, PHOTO), (1.4, PHOTO_B), (1.45, PHOTO), (2.2, PHOTO_B), (3.0, PHOTO)],
                  pos={'base': [(0, (0, -9, 0)), (1.5, (0, -8.6, 0)), (3.0, (0, -9, 0))]})

# ------------------------------------------------------------------ PEEK (leans out sideways, head tilted 80 deg)
peek = merge({'hips': (0, 0, 0), 'waist': (0, 0, 22), 'chest': (0, 0, 28), 'neck': (0, 0, 15), 'head': (0, 15, 55),
              'right_upper': (-40, 0, 40), 'right_fore': (-60, 0, 0), 'left_upper': (0, 0, -25),
              'right_thigh': (0, 0, 8), 'left_thigh': (0, 0, -18), 'left_shin': (10, 0, 0)}, fingers(-10, 20, 'right'), fingers(20, 2, 'left'))
peek_b = merge(peek, {'head': (6, 20, 72), 'chest': (0, 0, 31)})
A['PEEK'] = seq(Anim('peek', 4.0, True), [(0, peek), (1.2, peek_b), (1.3, peek_b), (2.5, peek), (2.55, merge(peek, {'head': (0, 15, 40)})), (4.0, peek)])

# ------------------------------------------------------------------ LEAP (crouch, spring into photo pose, land)
crouch = merge(sym({'right_thigh': (-70, 0, 20), 'right_shin': (120, 0, 0), 'right_foot': (-50, 0, 0),
                    'right_upper': (30, 0, 30), 'right_fore': (-40, 0, 0)}), {'waist': (30, 0, 0), 'chest': (20, 0, 0), 'head': (-40, 0, 0)}, fingers(-10, 20))
A['LEAP'] = seq(Anim('leap', 1.3), [(0, {}), (0.25, crouch), (0.45, PHOTO), (0.9, PHOTO_B), (1.05, crouch), (1.3, {})],
                pos={'base': [(0, (0, 0, 0)), (0.25, (0, -12, 0)), (0.45, (0, 6, -4)), (0.9, (0, 4, -8)), (1.05, (0, -10, 0)), (1.3, (0, 0, 0))]})

# ------------------------------------------------------------------ ATTACKS
wind_r = merge({'right_upper': (-150, 0, 45), 'right_fore': (-60, 0, 0), 'right_hand': (-20, 0, 0),
                'chest': (-6, 35, 0), 'waist': (0, 15, 0), 'head': (-10, -30, 0),
                'left_upper': (-20, 0, -25)}, fingers(-25, 25, 'right'))
slash_r = merge({'right_upper': (-20, 0, -45), 'right_fore': (-10, 0, 0), 'right_hand': (30, 0, 0),
                 'chest': (22, -40, 0), 'waist': (10, -15, 0), 'head': (-30, 30, 0),
                 'left_upper': (10, 0, -30), 'right_thigh': (-25, 0, 0), 'right_shin': (30, 0, 0)}, fingers(30, 5, 'right'))
A['ATTACK_R'] = seq(Anim('attack_r', 0.85), [(0, {}), (0.3, wind_r), (0.42, slash_r), (0.6, slash_r), (0.85, {})])
A['ATTACK_L'] = seq(Anim('attack_l', 0.85), [(0, {}), (0.3, mirror(wind_r)), (0.42, mirror(slash_r)), (0.6, mirror(slash_r)), (0.85, {})])

reach = merge(sym({'right_upper': (-95, 0, 12), 'right_fore': (-10, 0, 0), 'right_hand': (-10, 0, 0)}),
              {'chest': (25, 0, 0), 'head': (-20, 0, 0)}, fingers(-25, 25))
clutch = merge(sym({'right_upper': (-80, 0, -5), 'right_fore': (-40, 0, 0), 'right_hand': (10, 0, 0)}), {'chest': (10, 0, 0)}, fingers(70, 0))
lift = merge(sym({'right_upper': (-165, 0, -5), 'right_fore': (-20, 0, 0)}), {'chest': (-18, 0, 0), 'waist': (-10, 0, 0), 'head': (-45, 0, 0)}, fingers(70, 0))
slam = merge(sym({'right_upper': (-40, 0, -5), 'right_fore': (-10, 0, 0), 'right_thigh': (-40, 0, 10), 'right_shin': (60, 0, 0)}),
             {'chest': (45, 0, 0), 'waist': (25, 0, 0), 'head': (-30, 0, 0)}, fingers(-20, 25))
A['GRAB'] = seq(Anim('grab', 2.0), [(0, {}), (0.35, reach), (0.55, clutch), (1.0, lift), (1.2, lift), (1.4, slam), (1.65, slam), (2.0, {})],
                pos={'base': [(0, (0, 0, 0)), (1.0, (0, 3, 0)), (1.4, (0, -5, 0)), (2.0, (0, 0, 0))]})

# ------------------------------------------------------------------ SCREAM (arms flung wide, violent shudder)
scr = merge(sym({'right_upper': (-30, 0, 100), 'right_fore': (-20, 0, -15), 'right_hand': (0, 0, 20),
                 'right_thigh': (-15, 0, 15), 'right_shin': (25, 0, 0)}),
            {'chest': (-20, 0, 0), 'waist': (-8, 0, 0), 'neck': (-25, 0, 0), 'head': (-40, 0, 0)}, fingers(-30, 30))
frames = [(0, {}), (0.3, merge(scr, {'head': (20, 0, 0), 'chest': (25, 0, 0)}))]
for i in range(12):
    t = 0.45 + i * 0.1
    j = 1 if i % 2 else -1
    frames.append((t, merge(scr, {'head': (-40 + 6 * j, 10 * j, 8 * j), 'chest': (-20 + 3 * j, 4 * j, 0),
                                  'right_hand': (8 * j, 0, 20), 'left_hand': (-8 * j, 0, -20)})))
frames += [(1.85, scr), (2.3, {})]
A['SCREAM'] = seq(Anim('scream', 2.3), frames, interp='L', pos={'base': [(0, (0, 0, 0), 'L'), (0.3, (0, -2, 0), 'L'), (0.45, (0, 2, 0), 'L'), (1.85, (0, 2, 0), 'L'), (2.3, (0, 0, 0), 'L')]})

# ------------------------------------------------------------------ TWITCH (glitchy snaps incl. full head spin)
tw = [(0, {}),
      (0.05, {'head': (0, 90, 30), 'right_upper': (-60, 0, 30), 'chest': (0, 0, 15)}),
      (0.1, {'head': (0, 90, 30), 'right_upper': (-60, 0, 30), 'chest': (0, 0, 15)}),
      (0.15, {'head': (20, 180, 0), 'left_upper': (-120, 0, -50), 'chest': (0, 0, -18), 'right_fore': (-90, 0, 0)}),
      (0.35, {'head': (20, 180, 0), 'left_upper': (-120, 0, -50), 'chest': (0, 0, -18), 'right_fore': (-90, 0, 0)}),
      (0.4, merge({'head': (-30, 270, 40), 'waist': (0, 25, 0), 'right_thigh': (-40, 0, 25)}, fingers(-40, 30))),
      (0.6, merge({'head': (-30, 270, 40), 'waist': (0, 25, 0), 'right_thigh': (-40, 0, 25)}, fingers(-40, 30))),
      (0.65, {'head': (0, 360, 0), 'chest': (10, 0, 10)}),
      (0.75, {'head': (0, 360, -20), 'chest': (10, 0, 10)}),
      (1.0, {'head': (0, 360, 0)})]
A['TWITCH'] = seq(Anim('twitch', 1.0), tw, interp='L')

# ------------------------------------------------------------------ HAT TIP (polite, wrong)
tip_r = merge({'right_upper': (-150, -20, -25), 'right_fore': (-75, 0, 0), 'right_hand': (-20, 0, 0),
               'head': (20, 0, 0), 'chest': (25, 0, 0), 'waist': (10, 0, 0)}, fingers(30, 0, 'right'))
tip_b = merge(tip_r, {'head': (35, 0, -8), 'chest': (40, 0, 0)})
A['HAT_TIP'] = seq(Anim('hat_tip', 2.0), [(0, {}), (0.45, tip_r), (0.75, tip_b), (1.3, tip_b), (1.6, tip_r), (2.0, {})])
A['HAT_TIP'].rot('hat', (0, (0, 0, 0)), (0.45, (0, 0, 0)), (0.75, (-30, 0, 0)), (1.3, (-30, 0, 0)), (1.6, (0, 0, 0)), (2.0, (0, 0, 0)))
A['HAT_TIP'].pos('hat', (0, (0, 0, 0)), (0.45, (0, 0, 0)), (0.75, (0, 1.6, -0.5)), (1.3, (0, 1.6, -0.5)), (1.6, (0, 0, 0)), (2.0, (0, 0, 0)))

# ------------------------------------------------------------------ LOOK AROUND / SNIFF (impossible head turns)
lk = [(0, {}), (0.4, {'head': (-20, 0, 0), 'neck': (-15, 0, 0)}), (0.6, {'head': (-25, 0, 6), 'neck': (-15, 0, 0)}),
      (1.2, {'head': (0, 110, 15), 'chest': (0, 20, 0)}), (1.5, {'head': (0, 110, 15), 'chest': (0, 20, 0)}),
      (2.1, {'head': (0, -120, -20), 'chest': (0, -22, 0)}), (2.4, {'head': (0, -120, -35), 'chest': (0, -22, 0)}),
      (3.0, {})]
A['LOOK'] = seq(Anim('look', 3.0), [(t, merge(d, fingers(10, 3))) for t, d in lk])

# ------------------------------------------------------------------ EXTINGUISH (pinches out a light)
ex1 = merge({'right_upper': (-110, 0, 15), 'right_fore': (-25, 0, 0), 'chest': (20, 10, 0), 'head': (-25, 10, 0)}, fingers(-10, 25, 'right'))
ex2 = merge(ex1, fingers(75, 0, 'right'))
A['EXTINGUISH'] = seq(Anim('extinguish', 1.3), [(0, {}), (0.45, ex1), (0.65, ex2), (0.95, ex2), (1.3, {})])

# ------------------------------------------------------------------ DEATH (convulse, fold, collapse; hat falls off)
d1 = merge(scr, {'head': (-60, 0, 30)})
d2 = merge(sym({'right_thigh': (-90, 0, 30), 'right_shin': (150, 0, 0), 'right_upper': (-20, 0, 70), 'right_fore': (-90, 0, 0)}),
           {'waist': (60, 0, 15), 'chest': (40, 0, 20), 'head': (50, 40, 30)}, fingers(80, 0))
d3 = merge(d2, {'waist': (85, 0, 20), 'chest': (50, 0, 25), 'right_upper': (60, 0, 80), 'left_upper': (40, 0, -90)})
A['DEATH'] = seq(Anim('death', 2.6), [(0, {}), (0.2, d1), (0.35, merge(d1, {'head': (-40, 20, -20)})), (0.5, d1),
                                       (1.3, d2), (1.8, d3), (2.6, d3)],
                 pos={'base': [(0, (0, 0, 0)), (0.5, (0, 2, 0)), (1.3, (0, -14, 2)), (1.8, (0, -19, 4)), (2.6, (0, -19, 4))]})
A['DEATH'].rot('hat', (0, (0, 0, 0)), (0.5, (0, 0, 0)), (1.0, (-70, 40, 0)), (1.6, (-150, 60, 0)), (2.6, (-160, 60, 0)))
A['DEATH'].pos('hat', (0, (0, 0, 0)), (0.5, (0, 4, 0)), (1.0, (0, 2, -8)), (1.6, (0, -6, -14)), (2.6, (0, -6, -14)))

# ------------------------------------------------------------------ EMERGE (unfolds joint by joint from a heap)
fold = merge(sym({'right_thigh': (-100, 0, 40), 'right_shin': (155, 0, 0), 'right_foot': (-50, 0, 0),
                  'right_upper': (40, 0, 60), 'right_fore': (-120, 0, 0)}), {'waist': (80, 0, 0), 'chest': (50, 0, 0), 'neck': (30, 0, 0), 'head': (60, 0, 0)}, fingers(80, 0))
half = merge(crouch, {'head': (40, 90, 0)})
A['EMERGE'] = seq(Anim('emerge', 3.0), [(0, fold), (0.8, merge(fold, {'right_upper': (-30, 0, 60), 'right_fore': (-20, 0, 0)}, fingers(-20, 25, 'right'))),
                                         (1.5, half), (2.0, merge(half, {'head': (-20, 0, 0)})), (2.1, merge(half, {'head': (-20, 0, 30)})), (3.0, {})],
                  pos={'base': [(0, (0, -21, 0)), (0.8, (0, -18, 0)), (1.5, (0, -11, 0)), (2.1, (0, -9, 0)), (3.0, (0, 0, 0))]})

if __name__ == '__main__':
    for k, a in A.items():
        print(k, a.length, len(a.ch))
