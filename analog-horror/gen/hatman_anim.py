"""Hatman animations (bedrock degrees, additive on rest pose). +x swings hanging limbs back / leans torso forward,
+z moves right limbs outward (left side mirrored)."""
import math
from geo import Anim

A = {}


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
    """curl > 0 curls fingers toward the palm, spread fans them front/back"""
    d = {}
    for s in ([side] if side else ('right', 'left')):
        sg = -1 if s == 'right' else 1
        for i in range(4):
            d[f'{s}_f{i}a'] = (-(i - 1.5) * spread, 0, -sg * curl * 0.8)
            d[f'{s}_f{i}b'] = (0, 0, -sg * curl)
            d[f'{s}_f{i}c'] = (0, 0, -sg * (tip if tip is not None else curl * 0.9))
        d[f'{s}_thumb_a'] = (-spread, 0, -sg * curl * 0.6)
        d[f'{s}_thumb_b'] = (0, 0, -sg * curl * 0.7)
    return d


def merge(*ds):
    o = {}
    for d in ds:
        o.update(d)
    return o


def seq(name, length, frames, loop=False, interp='catmullrom', pos=None):
    a = Anim(name, length, loop)
    names = set()
    for _, d in frames:
        names |= set(d)
    for b in sorted(names):
        for t, d in frames:
            a.key(b, 'rotation', t, d.get(b, (0, 0, 0)), interp)
    for b, keys in (pos or {}).items():
        for k in keys:
            a.key(b, 'position', k[0], k[1], k[2] if len(k) > 2 else interp)
    A[name] = a
    return a


def cyc(name, length, fn, n=8, bob=0.0, bob_base=0.0):
    fr = [(length * i / n, fn(i / n)) for i in range(n + 1)]
    pos = None
    if bob or bob_base:
        pos = {'base': [(length * i / n, (0, bob_base + bob * abs(math.cos(i / n * math.tau)) - bob, 0)) for i in range(n + 1)]}
    return seq(name, length, fr, True, pos=pos)


# ---------------------------------------------------------------- IDLE: breathing, finger rippling, jerky head tilts
ia = merge(sym({'right_upper': (-3, 0, 2), 'right_fore': (-4, 0, 0)}), fingers(8, 3), {'chest': (3, 0, 0), 'head': (4, 0, 6)})
ib = merge(sym({'right_upper': (-1, 0, 4), 'right_fore': (-9, 0, 0)}), fingers(18, 7), {'chest': (-3, 0, 0), 'waist': (-2, 0, 0), 'head': (-4, 14, -10), 'neck': (0, 8, 0)})
ic = merge(sym({'right_upper': (-4, 0, 2), 'right_fore': (-3, 0, 0)}), fingers(4, 2),
           {'right_f0b': (0, 0, 40), 'right_f1b': (0, 0, 28), 'left_f3b': (0, 0, -35), 'chest': (4, 0, 0), 'head': (8, -18, 24)})
seq('idle', 4.0, [(0, ia), (1.1, ib), (1.25, ib), (2.2, ic), (2.3, merge(ic, {'head': (8, -18, 38)})), (3.0, ic), (4.0, ia)], True,
    pos={'base': [(0, (0, 0, 0)), (2.0, (0, -0.35, 0)), (4.0, (0, 0, 0))]})


# ---------------------------------------------------------------- WALK: gangly, long strides
def walk(p, k=1.0):
    s = math.sin(p * math.tau); c = math.cos(p * math.tau)
    return merge({
        'right_thigh': (-30 * s * k, 0, 2), 'left_thigh': (30 * s * k, 0, -2),
        'right_shin': (max(0, 42 * c) * k, 0, 0), 'left_shin': (max(0, -42 * c) * k, 0, 0),
        'right_foot': (-12 * s * k, 0, 0), 'left_foot': (12 * s * k, 0, 0),
        'right_upper': (24 * s * k, 0, 4), 'left_upper': (-24 * s * k, 0, -4),
        'right_fore': (-10 + 12 * s * k, 0, 0), 'left_fore': (-10 - 12 * s * k, 0, 0),
        'right_hand': (18 * s * k, 0, 0), 'left_hand': (-18 * s * k, 0, 0),
        'waist': (2, 8 * s * k, 0), 'chest': (4, -10 * s * k, 3 * s * k),
        'head': (-4, 8 * s * k, -6 * s * k), 'hips': (0, -6 * s * k, 0),
    }, fingers(10 + 10 * abs(s), 3))


cyc('walk', 1.6, walk, bob=0.9)


# ---------------------------------------------------------------- RUN: loping, bent forward, arms flailing behind
def run(p):
    s = math.sin(p * math.tau); c = math.cos(p * math.tau)
    return merge({
        'right_thigh': (-55 * s - 15, 0, 6), 'left_thigh': (55 * s - 15, 0, -6),
        'right_shin': (max(0, 80 * c) + 12, 0, 0), 'left_shin': (max(0, -80 * c) + 12, 0, 0),
        'right_foot': (-22 * s, 0, 0), 'left_foot': (22 * s, 0, 0),
        'right_upper': (60 + 25 * s, 0, 20), 'left_upper': (60 - 25 * s, 0, -20),
        'right_fore': (-30 + 22 * c, 0, 0), 'left_fore': (-30 - 22 * c, 0, 0),
        'right_hand': (25, 0, 0), 'left_hand': (25, 0, 0),
        'waist': (18, 6 * s, 0), 'chest': (22, -8 * s, 6 * s), 'neck': (-10, 0, 0),
        'head': (-25, 10 * s, -12 * s), 'hips': (8, -8 * s, 0),
    }, fingers(-10, 12 + 5 * s))


cyc('run', 0.85, run, bob=2.2, bob_base=-1.0)


# ---------------------------------------------------------------- CRAWL: all fours, spider-like
def crawl(p):
    s = math.sin(p * math.tau); c = math.cos(p * math.tau)
    return merge({
        'waist': (42, 0, 6 * s), 'chest': (10, 8 * s, 0), 'neck': (-35, 0, 0), 'head': (-5, 14 * s, 0),
        'right_upper': (-40 + 22 * s, 0, 55), 'left_upper': (-40 - 22 * s, 0, -55),
        'right_fore': (-15 + max(0, 25 * c), 0, -75), 'left_fore': (-15 + max(0, -25 * c), 0, 75),
        'right_hand': (20, 0, 10), 'left_hand': (20, 0, -10),
        'right_thigh': (-55 - 22 * s, 0, 50), 'left_thigh': (-55 + 22 * s, 0, -50),
        'right_shin': (95 - max(0, 30 * c), 0, 0), 'left_shin': (95 - max(0, -30 * c), 0, 0),
        'right_foot': (-30, 0, 0), 'left_foot': (-30, 0, 0),
    }, fingers(-20, 16))


cyc('crawl', 1.15, crawl, bob=0.7, bob_base=-13.0)
CR = crawl(0)
seq('crawl_idle', 3.0, [(0, CR), (1.5, merge(CR, {'head': (-30, 40, 25)})), (1.6, merge(CR, {'head': (-30, 40, 45)})), (3.0, CR)], True,
    pos={'base': [(0, (0, -13.7, 0)), (1.5, (0, -14.2, 0)), (3.0, (0, -13.7, 0))]})


# ---------------------------------------------------------------- STALK: slow tip-toe creep, hands raised, fingers fanned
def stalk(p):
    s = math.sin(p * math.tau); c = math.cos(p * math.tau)
    return merge({
        'right_thigh': (-35 - 30 * max(0, s), 0, 12), 'left_thigh': (-35 - 30 * max(0, -s), 0, -12),
        'right_shin': (55 + 25 * max(0, s), 0, 0), 'left_shin': (55 + 25 * max(0, -s), 0, 0),
        'right_foot': (-22, 0, 0), 'left_foot': (-22, 0, 0),
        'right_upper': (-35 + 10 * s, 0, 32), 'left_upper': (-35 - 10 * s, 0, -32),
        'right_fore': (-55, 0, -10), 'left_fore': (-55, 0, 10),
        'right_hand': (-30, 0, 0), 'left_hand': (-30, 0, 0),
        'waist': (12, 5 * s, 0), 'chest': (14, -6 * s, 0), 'head': (-22, 14 * c, 8 * s),
    }, fingers(-10 + 12 * c, 18))


cyc('stalk', 2.4, stalk, bob=0.5, bob_base=-5.0)

# ---------------------------------------------------------------- FROZEN: the photograph pose (when watched)
PHOTO = merge({
    'right_upper': (-15, 10, 62), 'right_fore': (-5, 0, 24), 'right_hand': (10, 0, 150),
    'left_upper': (-10, 0, -112), 'left_fore': (-10, 0, 108), 'left_hand': (-10, 0, 8),
    'right_thigh': (-55, 0, 62), 'right_shin': (95, 0, 0), 'right_foot': (-35, 0, 0),
    'left_thigh': (-35, 0, -48), 'left_shin': (75, 0, 0), 'left_foot': (-20, 0, 0),
    'waist': (4, 10, -8), 'chest': (2, 12, -6), 'neck': (-6, 0, 0), 'head': (-6, -14, 16),
}, fingers(-14, 22, 'right'), fingers(-6, 14, 'left'))
PHOTO_B = merge(PHOTO, {'head': (-4, -10, 21), 'right_hand': (12, 0, 154), 'chest': (3, 13, -5)}, fingers(-18, 25, 'right'))
seq('frozen', 3.0, [(0, PHOTO), (1.4, PHOTO_B), (1.45, PHOTO), (2.2, PHOTO_B), (3.0, PHOTO)], True,
    pos={'base': [(0, (0, -8.5, 0)), (1.5, (0, -8.1, 0)), (3.0, (0, -8.5, 0))]})

# ---------------------------------------------------------------- PEEK: leaning out sideways, head tilted 70 deg
peek = merge({'waist': (0, 0, 22), 'chest': (0, 0, 26), 'neck': (0, 0, 15), 'head': (0, 15, 50),
              'right_upper': (-40, 0, 40), 'right_fore': (-60, 0, 0), 'left_upper': (0, 0, -25),
              'right_thigh': (0, 0, 8), 'left_thigh': (0, 0, -18), 'left_shin': (10, 0, 0)}, fingers(-10, 20, 'right'), fingers(22, 2, 'left'))
peek_b = merge(peek, {'head': (6, 20, 70), 'chest': (0, 0, 29)})
seq('peek', 4.0, [(0, peek), (1.2, peek_b), (1.3, peek_b), (2.5, peek), (2.55, merge(peek, {'head': (0, 15, 36)})), (4.0, peek)], True)

# ---------------------------------------------------------------- LEAP
crouch = merge(sym({'right_thigh': (-70, 0, 20), 'right_shin': (120, 0, 0), 'right_foot': (-50, 0, 0),
                    'right_upper': (30, 0, 30), 'right_fore': (-40, 0, 0)}), {'waist': (25, 0, 0), 'chest': (15, 0, 0), 'head': (-30, 0, 0)}, fingers(-10, 20))
seq('leap', 1.3, [(0, {}), (0.25, crouch), (0.45, PHOTO), (0.9, PHOTO_B), (1.05, crouch), (1.3, {})],
    pos={'base': [(0, (0, 0, 0)), (0.25, (0, -11, 0)), (0.45, (0, 6, -4)), (0.9, (0, 4, -8)), (1.05, (0, -10, 0)), (1.3, (0, 0, 0))]})

# ---------------------------------------------------------------- ATTACKS
wind_r = merge({'right_upper': (-150, 0, 45), 'right_fore': (-60, 0, 0), 'right_hand': (-20, 0, 0),
                'chest': (-8, 35, 0), 'waist': (0, 15, 0), 'head': (-10, -30, 0), 'left_upper': (-20, 0, -25)}, fingers(-25, 25, 'right'))
slash_r = merge({'right_upper': (-20, 0, -45), 'right_fore': (-10, 0, 0), 'right_hand': (30, 0, 0),
                 'chest': (18, -40, 0), 'waist': (8, -15, 0), 'head': (-25, 30, 0),
                 'left_upper': (10, 0, -30), 'right_thigh': (-25, 0, 0), 'right_shin': (30, 0, 0)}, fingers(35, 5, 'right'))
seq('attack_r', 0.85, [(0, {}), (0.3, wind_r), (0.42, slash_r), (0.6, slash_r), (0.85, {})])
seq('attack_l', 0.85, [(0, {}), (0.3, mirror(wind_r)), (0.42, mirror(slash_r)), (0.6, mirror(slash_r)), (0.85, {})])

reach = merge(sym({'right_upper': (-95, 0, 12), 'right_fore': (-10, 0, 0), 'right_hand': (-10, 0, 0)}), {'chest': (20, 0, 0), 'head': (-20, 0, 0)}, fingers(-25, 25))
clutch = merge(sym({'right_upper': (-80, 0, -5), 'right_fore': (-40, 0, 0), 'right_hand': (10, 0, 0)}), {'chest': (8, 0, 0)}, fingers(75, 0))
lift = merge(sym({'right_upper': (-165, 0, -5), 'right_fore': (-20, 0, 0)}), {'chest': (-20, 0, 0), 'waist': (-10, 0, 0), 'head': (-40, 0, 0)}, fingers(75, 0))
slam = merge(sym({'right_upper': (-40, 0, -5), 'right_fore': (-10, 0, 0), 'right_thigh': (-40, 0, 10), 'right_shin': (60, 0, 0)}),
             {'chest': (40, 0, 0), 'waist': (22, 0, 0), 'head': (-30, 0, 0)}, fingers(-20, 25))
seq('grab', 2.0, [(0, {}), (0.35, reach), (0.55, clutch), (1.0, lift), (1.2, lift), (1.4, slam), (1.65, slam), (2.0, {})],
    pos={'base': [(0, (0, 0, 0)), (1.0, (0, 3, 0)), (1.4, (0, -5, 0)), (2.0, (0, 0, 0))]})

# ---------------------------------------------------------------- SCREAM: arms flung wide, violent shudder
scr = merge(sym({'right_upper': (-30, 0, 100), 'right_fore': (-20, 0, -15), 'right_hand': (0, 0, 20),
                 'right_thigh': (-15, 0, 15), 'right_shin': (25, 0, 0)}),
            {'chest': (-22, 0, 0), 'waist': (-8, 0, 0), 'neck': (-25, 0, 0), 'head': (-30, 0, 0)}, fingers(-30, 30))
fr = [(0, {}), (0.3, merge(scr, {'head': (25, 0, 0), 'chest': (25, 0, 0)}))]
for i in range(12):
    j = 1 if i % 2 else -1
    fr.append((0.45 + i * 0.1, merge(scr, {'head': (-30 + 6 * j, 10 * j, 8 * j), 'chest': (-22 + 3 * j, 4 * j, 0),
                                          'right_hand': (8 * j, 0, 20), 'left_hand': (-8 * j, 0, -20)})))
fr += [(1.85, scr), (2.3, {})]
seq('scream', 2.3, fr, interp='linear',
    pos={'base': [(0, (0, 0, 0), 'linear'), (0.3, (0, -2, 0), 'linear'), (0.45, (0, 2, 0), 'linear'), (1.85, (0, 2, 0), 'linear'), (2.3, (0, 0, 0), 'linear')]})

# ---------------------------------------------------------------- TWITCH: glitchy snaps incl. a full head spin
seq('twitch', 1.0, [(0, {}),
                    (0.05, {'head': (0, 90, 30), 'right_upper': (-60, 0, 30), 'chest': (0, 0, 15)}),
                    (0.1, {'head': (0, 90, 30), 'right_upper': (-60, 0, 30), 'chest': (0, 0, 15)}),
                    (0.15, {'head': (20, 180, 0), 'left_upper': (-120, 0, -50), 'chest': (0, 0, -18), 'right_fore': (-90, 0, 0)}),
                    (0.35, {'head': (20, 180, 0), 'left_upper': (-120, 0, -50), 'chest': (0, 0, -18), 'right_fore': (-90, 0, 0)}),
                    (0.4, merge({'head': (-30, 270, 40), 'waist': (0, 25, 0), 'right_thigh': (-40, 0, 25)}, fingers(-40, 30))),
                    (0.6, merge({'head': (-30, 270, 40), 'waist': (0, 25, 0), 'right_thigh': (-40, 0, 25)}, fingers(-40, 30))),
                    (0.65, {'head': (0, 360, 0), 'chest': (10, 0, 10)}),
                    (0.75, {'head': (0, 360, -20), 'chest': (10, 0, 10)}),
                    (1.0, {'head': (0, 360, 0)})], interp='linear')

# ---------------------------------------------------------------- HAT TIP: a polite, wrong greeting
tip_r = merge({'right_upper': (-150, -20, -25), 'right_fore': (-75, 0, 0), 'right_hand': (-20, 0, 0),
               'head': (20, 0, 0), 'chest': (22, 0, 0), 'waist': (10, 0, 0)}, fingers(35, 0, 'right'))
tip_b = merge(tip_r, {'head': (35, 0, -8), 'chest': (38, 0, 0), 'hat': (-30, 0, 0)})
a = seq('hat_tip', 2.0, [(0, {}), (0.45, tip_r), (0.75, tip_b), (1.3, tip_b), (1.6, tip_r), (2.0, {})])
for t, v in ((0, (0, 0, 0)), (0.45, (0, 0, 0)), (0.75, (0, 1.6, -0.6)), (1.3, (0, 1.6, -0.6)), (1.6, (0, 0, 0)), (2.0, (0, 0, 0))):
    a.key('hat', 'position', t, v)

# ---------------------------------------------------------------- LOOK / SNIFF: impossible head turns
lk = [(0, {}), (0.4, {'head': (-20, 0, 0), 'neck': (-15, 0, 0)}), (0.6, {'head': (-25, 0, 6), 'neck': (-15, 0, 0)}),
      (1.2, {'head': (0, 110, 15), 'chest': (0, 20, 0)}), (1.5, {'head': (0, 110, 15), 'chest': (0, 20, 0)}),
      (2.1, {'head': (0, -120, -20), 'chest': (0, -22, 0)}), (2.4, {'head': (0, -120, -35), 'chest': (0, -22, 0)}), (3.0, {})]
seq('look', 3.0, [(t, merge(d, fingers(10, 3))) for t, d in lk])

# ---------------------------------------------------------------- EXTINGUISH: pinches out a light
ex1 = merge({'right_upper': (-110, 0, 15), 'right_fore': (-25, 0, 0), 'chest': (18, 10, 0), 'head': (-20, 10, 0)}, fingers(-10, 25, 'right'))
ex2 = merge(ex1, fingers(80, 0, 'right'))
seq('extinguish', 1.3, [(0, {}), (0.45, ex1), (0.65, ex2), (0.95, ex2), (1.3, {})])

# ---------------------------------------------------------------- DEATH: convulse, fold, collapse; hat falls
d1 = merge(scr, {'head': (-50, 0, 30)})
d2 = merge(sym({'right_thigh': (-90, 0, 30), 'right_shin': (150, 0, 0), 'right_upper': (-20, 0, 70), 'right_fore': (-90, 0, 0)}),
           {'waist': (55, 0, 15), 'chest': (35, 0, 20), 'head': (45, 40, 30)}, fingers(80, 0))
d3 = merge(d2, {'waist': (80, 0, 20), 'chest': (45, 0, 25), 'right_upper': (60, 0, 80), 'left_upper': (40, 0, -90)})
a = seq('death', 2.6, [(0, {}), (0.2, d1), (0.35, merge(d1, {'head': (-40, 20, -20)})), (0.5, d1), (1.3, d2), (1.8, d3), (2.6, d3)],
        pos={'base': [(0, (0, 0, 0)), (0.5, (0, 2, 0)), (1.3, (0, -14, 2)), (1.8, (0, -19, 4)), (2.6, (0, -19, 4))]})
for t, v in ((0, (0, 0, 0)), (0.5, (0, 0, 0)), (1.0, (-70, 40, 0)), (1.6, (-150, 60, 0)), (2.6, (-160, 60, 0))):
    a.key('hat', 'rotation', t, v)
for t, v in ((0, (0, 0, 0)), (0.5, (0, 4, 0)), (1.0, (0, 2, -8)), (1.6, (0, -6, -14)), (2.6, (0, -6, -14))):
    a.key('hat', 'position', t, v)

# ---------------------------------------------------------------- EMERGE: unfolds joint by joint from a heap
fold = merge(sym({'right_thigh': (-100, 0, 40), 'right_shin': (155, 0, 0), 'right_foot': (-50, 0, 0),
                  'right_upper': (40, 0, 60), 'right_fore': (-120, 0, 0)}), {'waist': (75, 0, 0), 'chest': (45, 0, 0), 'neck': (30, 0, 0), 'head': (50, 0, 0)}, fingers(80, 0))
half = merge(crouch, {'head': (30, 90, 0)})
seq('emerge', 3.0, [(0, fold), (0.8, merge(fold, {'right_upper': (-30, 0, 60), 'right_fore': (-20, 0, 0)}, fingers(-20, 25, 'right'))),
                    (1.5, half), (2.0, merge(half, {'head': (-20, 0, 0)})), (2.1, merge(half, {'head': (-20, 0, 30)})), (3.0, {})],
    pos={'base': [(0, (0, -21, 0)), (0.8, (0, -18, 0)), (1.5, (0, -11, 0)), (2.1, (0, -9, 0)), (3.0, (0, 0, 0))]})

if __name__ == '__main__':
    for k, v in A.items():
        print(k, v.length, len(v.ch))
