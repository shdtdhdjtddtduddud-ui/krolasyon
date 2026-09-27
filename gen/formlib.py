"""Shared toolkit for the transformation forms: standard humanoid rig builder, secondary-motion helpers and a library of
animation archetypes (locomotion + ability moves) that every form re-uses with its own weapon, dressing and timings.

Rotation conventions (degrees, added to the rest pose, vanilla ZYX):
  arm  x<0 raises forward/up (-90 forward, -170 overhead), x>0 swings back; right arm z>0 lifts it out sideways
  leg  x<0 forward; shin x>0 bends the knee; base x>0 leans forward; head x<0 looks up; chest x>0 bends forward
  left_* values are mirrored from right_* with (x, -y, -z)."""
import math, numpy as np
from core import Model, Anim, rot_zyx
from animlib import add_rot

R0 = np.eye(3)


# ------------------------------------------------------------------ rig helpers
class Rig:
    def __init__(self, M):
        self.M = M

    def local(self, name, parent, off, rot=(0, 0, 0)):
        M = self.M
        M.bone(name, parent, (0, 0, 0))
        M.bones[name].off = [float(v) for v in off]
        M.bones[name].rot = [float(v) for v in rot]
        return M.bones[name]

    def spike(self, name, parent, A, B, w, mat, d=None, tipmat=None):
        """tapered spike: seg bone with three stacked cubes of shrinking width"""
        M = self.M
        d = w if d is None else d
        L = M.seg(name, parent, A, B, w, d, mat, extend=0.3)
        M.bones[name].cubes[0].size[1] = L * 0.58
        M.cube_l(name, (-w * 0.32, L * 0.55, -d * 0.32), (w * 0.64, L * 0.3, d * 0.64), mat)
        M.cube_l(name, (-w * 0.15, L * 0.85, -d * 0.15), (w * 0.3, L * 0.25, d * 0.3), tipmat or mat)
        return L

    def world_pt(self, bone, p):
        W = self.M.world(bone)
        q = W @ np.array([p[0], p[1], p[2], 1.0])
        return tuple(q[:3])

    def spike_local(self, name, parent, A, B, w, mat, d=None, tipmat=None):
        """spike with endpoints given in the parent's local space"""
        return self.spike(name, parent, self.world_pt(parent, A), self.world_pt(parent, B), w, mat, d, tipmat)

    def chain(self, prefix, parent, pts, widths, mat, depth_ratio=1.0, ext=0.6):
        p = parent
        names = []
        for i in range(len(pts) - 1):
            n = f'{prefix}{i}'
            w = widths[min(i, len(widths) - 1)]
            m = mat[min(i, len(mat) - 1)] if isinstance(mat, (list, tuple)) else mat
            self.M.seg(n, p, pts[i], pts[i + 1], w, w * depth_ratio, m, extend=ext)
            p = n
            names.append(n)
        return names

    def panel_chain(self, prefix, parent, pivot, rot, x0, w, lengths, mats):
        """hanging cloth / hair ribbon made of plane segments (double sided), returns bone names"""
        M = self.M
        M.bone(prefix + '0', parent, pivot, rot)
        names = [prefix + '0']
        M.cube_l(names[0], (x0, 0, 0), (w, lengths[0], 0), mats[0], plane=True)
        for i in range(1, len(lengths)):
            n = f'{prefix}{i}'
            self.local(n, names[-1], (0, lengths[i - 1], 0))
            M.cube_l(n, (x0, 0, 0), (w, lengths[i], 0), mats[min(i, len(mats) - 1)], plane=True)
            names.append(n)
        return names


DEF = dict(
    hip_y=-24.0, shoulder=(8.4, -49.5, 0.0), elbow=(12.0, -35.5, 1.5), wrist=(14.2, -23.0, -0.5),
    hipj=(3.3, -24.5, 0.0), knee=(4.6, -3.0, -1.5), ankle=(5.6, 17.0, 0.8),
    arm_w=(3.8, 4.0), fore_w=(3.3, 3.4), leg_w=(4.4, 5.0), shin_w=(3.8, 4.2),
    chest=((-6.8, -52.0, -4.0), (13.6, 10.0, 8.0)), belly=((-4.8, -42.5, -3.3), (9.6, 7.0, 6.6)),
    waist=((-3.2, -36.5, -2.6), (6.4, 9.0, 5.2)), pelvis=((-4.5, -28.0, -3.0), (9.0, 6.0, 6.0)),
    neck=((-1.5, -56.5, -1.6), (3.0, 5.0, 3.2)), head=((-3.2, -63.0, -3.3), (6.4, 7.2, 6.6)),
    face=((-2.9, -62.2, -3.9), (5.8, 5.8, 0.7)), hand=((-1.9, -0.3, -2.2), (3.8, 4.2, 4.4)),
    foot=((-2.4, 0.0, -5.0), (4.8, 7.0, 7.4)),
    mats=dict(pelvis='pelvis', waist='waist', belly='belly', chest='chest', neck='neck', head='head', face='face',
              arm='arm', fore='fore', hand='hand', leg='leg', shin='shin', foot='foot'))


def humanoid(M, **kw):
    """standard skeleton shared by every form so the animation archetypes fit all of them"""
    S = dict(DEF)
    mats = dict(DEF['mats'])
    mats.update(kw.pop('mats', {}))
    S.update(kw)
    M.bone('base', None, (0, S['hip_y'], 0))
    M.bone('hips', 'base', (0, S['hip_y'], 0))
    M.cube('hips', *S['pelvis'], mats['pelvis'])
    M.bone('waist', 'hips', (0, -28, 0))
    M.cube('waist', *S['waist'], mats['waist'])
    M.bone('chest', 'waist', (0, -36, 0))
    M.cube('chest', *S['belly'], mats['belly'])
    M.cube('chest', *S['chest'], mats['chest'])
    M.bone('neck', 'chest', (0, -52.5, 0))
    M.cube('neck', *S['neck'], mats['neck'])
    M.bone('head', 'neck', (0, -56, 0))
    M.cube('head', *S['head'], mats['head'])
    if S['face'] is not None:
        M.cube('head', *S['face'], mats['face'])
    for side, sx in (('right', -1), ('left', 1)):
        A = (sx * S['shoulder'][0], S['shoulder'][1], S['shoulder'][2])
        B = (sx * S['elbow'][0], S['elbow'][1], S['elbow'][2])
        C = (sx * S['wrist'][0], S['wrist'][1], S['wrist'][2])
        M.seg(f'{side}_arm', 'chest', A, B, S['arm_w'][0], S['arm_w'][1], mats['arm'], extend=1.5)
        L = M.seg(f'{side}_fore', f'{side}_arm', B, C, S['fore_w'][0], S['fore_w'][1], mats['fore'], extend=1.0)
        Rig(M).local(f'{side}_hand', f'{side}_fore', (0, L, 0))
        M.cube_l(f'{side}_hand', *S['hand'], mats['hand'])
        A = (sx * S['hipj'][0], S['hipj'][1], S['hipj'][2])
        B = (sx * S['knee'][0], S['knee'][1], S['knee'][2])
        C = (sx * S['ankle'][0], S['ankle'][1], S['ankle'][2])
        M.seg(f'{side}_leg', 'hips', A, B, S['leg_w'][0], S['leg_w'][1], mats['leg'], extend=1.5)
        M.seg(f'{side}_shin', f'{side}_leg', B, C, S['shin_w'][0], S['shin_w'][1], mats['shin'], extend=1.0)
        M.bone_w(f'{side}_foot', f'{side}_shin', C, R0)
        M.cube_l(f'{side}_foot', *S['foot'], mats['foot'])
    return S


# ------------------------------------------------------------------ secondary motion
def cyc(length, n=8):
    return [(length * k / n, math.tau * k / n) for k in range(n + 1)]


def wave_chain(a, names, length, amp=1.0, speed=1, phase=0.0, axis=(1.0, 0.3, 0.6), lift=0.0, step=0.8, grow=1.25):
    """travelling sine wave down a chain of bones (hair, tails, cloth)"""
    for i, n in enumerate(names):
        g = grow ** i
        keys = []
        for t, ph in cyc(length):
            p = ph * speed - i * step + phase
            keys.append((t, (lift * (0.6 if i else 1.0) + 6 * amp * g * axis[0] * math.sin(p),
                             6 * amp * g * axis[1] * math.cos(p + 0.4), 6 * amp * g * axis[2] * math.sin(p + 1.1))))
        a.rot(n, *keys)


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


def add(*ds):
    o = {}
    for d in ds:
        for k, v in d.items():
            p = o.get(k, (0, 0, 0))
            o[k] = (p[0] + v[0], p[1] + v[1], p[2] + v[2])
    return o


SIDES = ('right', 'left')
BODY = [f'{s}_{p}' for s in SIDES for p in ('arm', 'fore', 'hand', 'leg', 'shin', 'foot')] + ['chest', 'waist', 'head', 'hips', 'neck', 'weapon']


def seq(a, frames, names=BODY, interp='C'):
    """frames: [(t, pose)]; every frame is completed with zeros so poses blend back cleanly"""
    full = []
    for t, d in frames:
        f = {n: (0, 0, 0) for n in names}
        f.update({k: v for k, v in d.items() if k in names or k not in ('base',)})
        full.append((t, f))
    add_rot(a, full, interp)


# ------------------------------------------------------------------ the anim set builder
class FormAnims:
    """builds the full animation set of a form.
    dress(a, length, amp, speed, flare, back, lift): secondary motion (hair, capes, tails, flames)
    carry: pose added to all locomotion frames (how the weapon is held while idle / moving)
    move_carry: carry used while walking / running"""

    def __init__(self, dress, carry=None, move_carry=None, heavy=1.0, two_handed=False):
        self.dress = dress
        self.carry = carry or {}
        self.move = move_carry if move_carry is not None else self.carry
        self.heavy = heavy
        self.two = two_handed
        self.A = {}
        self.durations = []

    # ---------------- locomotion
    def locomotion(self):
        A, C, MV = self.A, self.carry, self.move
        idle = Anim('idle', 4.0, True)
        f0 = C
        f1 = add(C, {'chest': (-2.5, 0, 0), 'head': (3, -5, 2), 'right_arm': (-2, 0, -2), 'left_arm': (-3, 0, 3)})
        f2 = add(C, {'head': (-2, 5, -2)})
        seq(idle, [(0, f0), (1.3, f1), (2.7, f2), (4.0, f0)])
        idle.pos('base', (0, (0, 0, 0)), (2, (0, -0.6, 0)), (4, (0, 0, 0)))
        self.dress(idle, 4.0, 0.7, 1, 0, 0, 0)
        A['IDLE'] = idle

        def walk_frame(s, big=1.0):
            return {'right_leg': (-28 * s * big, 0, 0), 'right_shin': (max(0, 36 * s) * big + 5, 0, 0),
                    'left_leg': (28 * s * big, 0, 0), 'left_shin': (max(0, -36 * s) * big + 5, 0, 0),
                    'right_arm': (8 * s * big, 0, 0), 'left_arm': (-22 * s * big, 0, 0),
                    'left_fore': (-10 + 8 * s, 0, 0), 'hips': (0, 7 * s, 0), 'chest': (3 * big, -9 * s, 0), 'head': (0, 6 * s, 0)}

        walk = Anim('walk', 1.2, True)
        seq(walk, [(t, add(MV, walk_frame(s))) for t, s in ((0, 1), (0.3, 0), (0.6, -1), (0.9, 0), (1.2, 1))])
        walk.pos('base', (0, (0, 0, 0)), (0.3, (0, 1.2, 0)), (0.6, (0, 0, 0)), (0.9, (0, 1.2, 0)), (1.2, (0, 0, 0)))
        self.dress(walk, 1.2, 1.2, 2, 3, 6, 0)
        A['WALK'] = walk

        def run_frame(s):
            f = walk_frame(s, 1.7)
            f.update({'head': (-14, 8 * s, 0), 'chest': (6, -12 * s, 0), 'left_arm': (-45 * s + 10, 0, 12), 'left_fore': (-45, 0, 0),
                      'right_arm': (15 * s, 0, 0)})
            return f

        run = Anim('run', 0.72, True)
        seq(run, [(t, add(MV, run_frame(s))) for t, s in ((0, 1), (0.18, 0), (0.36, -1), (0.54, 0), (0.72, 1))])
        run.rot('base', (0, (16, 0, 0)), (0.72, (16, 0, 0)))
        run.pos('base', (0, (0, 0, 0)), (0.18, (0, 2.2, 0)), (0.36, (0, 0, 0)), (0.54, (0, 2.2, 0)), (0.72, (0, 0, 0)))
        self.dress(run, 0.72, 1.6, 2, 6, 35, 8)
        A['RUN'] = run

        jp = add(MV, {'right_leg': (-45, 0, 8), 'right_shin': (70, 0, 0), 'left_leg': (-10, 0, -6), 'left_shin': (35, 0, 0),
                      'right_arm': (-25, 0, 25), 'left_arm': (-40, 0, -45), 'left_fore': (-30, 0, 0), 'chest': (8, 0, 0), 'head': (-10, 0, 0)})
        jump = Anim('jump', 1.0, True)
        seq(jump, [(0, jp), (0.5, add(jp, {'right_leg': (-4, 0, 0), 'left_arm': (-4, 0, -4)})), (1.0, jp)])
        self.dress(jump, 1.0, 1.4, 2, 10, -18, -8)
        A['JUMP'] = jump

        fp = add(MV, {'right_leg': (-18, 0, 12), 'right_shin': (25, 0, 0), 'left_leg': (8, 0, -12), 'left_shin': (15, 0, 0),
                      'right_arm': (-40, 0, 45), 'left_arm': (-70, 0, -70), 'left_fore': (-20, 0, 0), 'chest': (-6, 0, 0), 'head': (12, 0, 0)})
        fall = Anim('fall', 1.0, True)
        seq(fall, [(0, fp), (0.5, add(fp, {'right_arm': (4, 0, 0), 'left_arm': (4, 0, 0)})), (1.0, fp)])
        self.dress(fall, 1.0, 2.0, 3, -8, -35, -15)
        A['FALL'] = fall

        cp = add(C, {'right_leg': (-38, 0, 6), 'right_shin': (60, 0, 0), 'left_leg': (-38, 0, -6), 'left_shin': (60, 0, 0), 'right_foot': (-22, 0, 0),
                     'left_foot': (-22, 0, 0), 'chest': (22, 0, 0), 'waist': (10, 0, 0), 'head': (-25, 0, 0), 'left_arm': (-30, 0, 15), 'left_fore': (-35, 0, 0)})
        crouch = Anim('crouch', 2.0, True)
        seq(crouch, [(0, cp), (1.0, add(cp, {'chest': (2, 0, 0)})), (2.0, cp)])
        crouch.pos('base', (0, (0, -7, -1)), (1, (0, -7.4, -1)), (2, (0, -7, -1)))
        self.dress(crouch, 2.0, 0.6, 1, 12, 0, 0)
        A['CROUCH'] = crouch

        sw = lambda s: {'right_arm': (-150 + 50 * s, 0, 30 - 20 * s), 'left_arm': (-150 - 50 * s, 0, -30 - 20 * s), 'weapon': (-40, 0, 0),
                        'right_leg': (22 * s, 0, 0), 'left_leg': (-22 * s, 0, 0), 'right_shin': (20 + 15 * s, 0, 0), 'left_shin': (20 - 15 * s, 0, 0),
                        'head': (-40, 0, 0), 'chest': (0, 8 * s, 0)}
        swim = Anim('swim', 1.4, True)
        seq(swim, [(t, sw(s)) for t, s in ((0, 1), (0.35, 0), (0.7, -1), (1.05, 0), (1.4, 1))])
        self.dress(swim, 1.4, 1.8, 2, 0, -50, 0)
        A['SWIM'] = swim

        # weapon swings: forehand diagonal and backhand
        C0 = self.move
        atk = Anim('attack_r', 0.55)
        seq(atk, [(0.0, C0),
                  (0.12, {'right_arm': (-160, 25, -10), 'right_fore': (-35, 0, 0), 'weapon': (-10, 0, 0), 'chest': (-4, 30, 0), 'waist': (0, 10, 0),
                          'left_arm': (10, 0, 12)}),
                  (0.26, {'right_arm': (-40, -60, 15), 'right_fore': (-5, 0, 0), 'weapon': (-30, 0, 0), 'chest': (12, -32, 0), 'waist': (4, -12, 0),
                          'left_arm': (-20, 0, 20), 'right_leg': (-20, 0, 0), 'left_leg': (15, 0, 0)}),
                  (0.55, C0)])
        atk.pos('base', (0, (0, 0, 0)), (0.26, (0, -1, -3)), (0.55, (0, 0, 0)))
        self.dress(atk, 0.55, 1.4, 1, 8, 10, 0)
        A['ATTACK_R'] = atk
        atk2 = Anim('attack_l', 0.55)
        seq(atk2, [(0.0, C0),
                   (0.12, {'right_arm': (-80, -70, -20), 'right_fore': (-40, 0, 0), 'weapon': (-20, 0, 0), 'chest': (0, -35, 0), 'waist': (0, -10, 0),
                           'left_arm': (-10, 0, 20)}),
                   (0.26, {'right_arm': (-85, 50, 40), 'right_fore': (-5, 0, 0), 'weapon': (-40, 0, 0), 'chest': (8, 32, 0), 'waist': (3, 12, 0),
                           'left_arm': (15, 0, 15), 'right_leg': (-15, 0, 0)}),
                   (0.55, C0)])
        atk2.pos('base', (0, (0, 0, 0)), (0.26, (0, -1, -2)), (0.55, (0, 0, 0)))
        self.dress(atk2, 0.55, 1.4, 1, 8, 10, 0)
        A['ATTACK_L'] = atk2

        tr = Anim('transform', 2.4)
        curl = {'right_leg': (-70, 0, 10), 'right_shin': (110, 0, 0), 'left_leg': (-70, 0, -10), 'left_shin': (110, 0, 0),
                'right_foot': (-30, 0, 0), 'left_foot': (-30, 0, 0), 'chest': (40, 0, 0), 'waist': (20, 0, 0), 'head': (35, 0, 0),
                'right_arm': (-60, -30, -40), 'left_arm': (-70, 40, 50), 'right_fore': (-70, 0, 0), 'left_fore': (-80, 0, 0), 'weapon': (-60, 0, 0)}
        roar = {'right_arm': (-150, 0, 30), 'left_arm': (-40, 0, -75), 'right_fore': (-10, 0, 0), 'left_fore': (-20, 0, 0), 'weapon': (0, 0, 0),
                'chest': (-22, 0, 0), 'waist': (-8, 0, 0), 'head': (-38, 0, 0), 'right_leg': (-8, 0, 12), 'left_leg': (-8, 0, -12)}
        seq(tr, [(0.0, curl), (0.75, {k: (v[0] * 0.85, v[1], v[2]) for k, v in curl.items()}), (1.05, roar),
                 (1.4, add(roar, {'chest': (-3, 0, 0), 'head': (-3, 0, 0)})), (1.75, roar), (2.4, C)])
        tr.pos('base', (0, (0, -16, 0)), (0.75, (0, -13, 0)), (1.05, (0, 3, 0)), (1.3, (0, 0, 0)), (2.4, (0, 0, 0)))
        tr.scl('base', (0, (0.35, 0.35, 0.35)), (0.75, (0.8, 0.8, 0.8)), (1.05, (1.12, 1.12, 1.12)), (1.3, (1, 1, 1)), (2.4, (1, 1, 1)))
        self.dress(tr, 2.4, 1.8, 2, 20, 0, -10)
        A['TRANSFORM'] = tr

        death = Anim('death', 1.0)
        seq(death, [(0.0, C), (0.25, {'head': (-35, 0, 0), 'chest': (-15, 0, 0), 'right_arm': (-30, 0, 45), 'left_arm': (-30, 0, -45)}),
                    (1.0, {'head': (40, 0, 10), 'chest': (40, 0, 0), 'right_arm': (30, 0, -10), 'left_arm': (20, 0, 20), 'right_leg': (-90, 0, 0),
                           'left_leg': (-70, 0, 0), 'right_shin': (100, 0, 0), 'left_shin': (110, 0, 0), 'weapon': (60, 0, 30)})])
        death.pos('base', (0, (0, 0, 0)), (0.25, (0, 2, 0)), (1.0, (0, -26, 2)))
        A['DEATH'] = death
        return self

    # ---------------- ability archetypes
    def ability(self, idx, kind, **kw):
        a = getattr(self, 'ab_' + kind)(**kw)
        self.A[f'ABILITY{idx}'] = a
        return a

    def _finish(self, a, amp=1.6, speed=2, flare=14, back=10, lift=0):
        self.dress(a, a.length, amp, speed, flare, back, lift)
        return a

    def ab_slam(self, length=1.4, hit=0.55, leap=6):
        """two-handed overhead slam"""
        a = Anim('slam', length)
        C = self.carry
        up = {'right_arm': (-170, 0, -12), 'left_arm': (-165, 0, 25), 'right_fore': (-25, 0, 0), 'left_fore': (-40, 0, 0), 'weapon': (-20, 0, 0),
              'chest': (-18, 0, 0), 'head': (-20, 0, 0), 'right_leg': (-45, 0, 0), 'right_shin': (70, 0, 0), 'left_leg': (-20, 0, 0), 'left_shin': (60, 0, 0)}
        down = {'right_arm': (-45, 0, -8), 'left_arm': (-50, 0, 30), 'right_fore': (-5, 0, 0), 'left_fore': (-20, 0, 0), 'weapon': (-10, 0, 0),
                'chest': (40, 0, 0), 'waist': (15, 0, 0), 'head': (-15, 0, 0), 'right_leg': (-75, 0, 10), 'right_shin': (100, 0, 0),
                'left_leg': (-15, 0, -10), 'left_shin': (90, 0, 0), 'left_foot': (-25, 0, 0)}
        seq(a, [(0, C), (hit * 0.55, up), (hit, down), (hit + 0.4, add(down, {'chest': (-3, 0, 0)})), (length, C)])
        a.pos('base', (0, (0, 0, 0)), (hit * 0.55, (0, leap, 0)), (hit, (0, -11, -2)), (hit + 0.4, (0, -11, -2)), (length, (0, 0, 0)))
        return self._finish(a, flare=25)

    def ab_spin(self, length=1.6, turns=2, t0=0.3, low=False):
        """whirling spin with the weapon held out sideways"""
        a = Anim('spin', length)
        C = self.carry
        t1 = length - 0.35
        out = {'right_arm': (-10, 0, 85), 'right_fore': (-5, 0, 0), 'weapon': (-80, 0, 0), 'left_arm': (-20, 0, -70), 'chest': (10, 0, 0),
               'right_leg': (-30, 0, 20), 'left_leg': (-30, 0, -20), 'right_shin': (40, 0, 0), 'left_shin': (40, 0, 0)}
        if low:
            out = add(out, {'right_leg': (-20, 0, 10), 'left_leg': (-20, 0, -10), 'right_shin': (30, 0, 0), 'left_shin': (30, 0, 0), 'chest': (10, 0, 0)})
        seq(a, [(0, C), (t0, add(out, {'chest': (0, 40, 0)})), (t1, out), (length, C)])
        keys = [(0, (0, 0, 0)), (t0, (0, 30, 0))]
        n = int(turns * 4)
        for k in range(1, n + 1):
            keys.append((t0 + (t1 - t0) * k / n, (0, 30 - 90 * k, 0), 'L'))
        keys.append((length, (0, -360 * turns, 0)))
        a.rot('base', *keys)
        a.pos('base', (0, (0, 0, 0)), (t0, (0, -4 if low else -2, 0)), (t1, (0, -4 if low else -2, 0)), (length, (0, 0, 0)))
        return self._finish(a, amp=2.2, speed=3, flare=35, back=0)

    def ab_thrust(self, length=1.1, hit=0.3):
        """wind up and lunge, weapon thrust forward"""
        a = Anim('thrust', length)
        C = self.carry
        wind = {'right_arm': (45, 20, 20), 'right_fore': (-70, 0, 0), 'weapon': (-80, 0, 0), 'chest': (10, 35, 0), 'left_arm': (-50, 0, -30),
                'right_leg': (-40, 0, 0), 'right_shin': (60, 0, 0), 'left_leg': (20, 0, 0), 'left_shin': (35, 0, 0)}
        lunge = {'right_arm': (-88, -5, 0), 'right_fore': (-5, 0, 0), 'weapon': (-88, 0, 0), 'chest': (15, -25, 0), 'left_arm': (35, 0, -25),
                 'head': (-25, 0, 0), 'right_leg': (-60, 0, 0), 'right_shin': (40, 0, 0), 'left_leg': (45, 0, 0), 'left_shin': (15, 0, 0)}
        seq(a, [(0, C), (hit * 0.6, wind), (hit, lunge), (hit + 0.35, add(lunge, {'chest': (-5, 5, 0)})), (length, C)])
        a.rot('base', (0, (0, 0, 0)), (hit * 0.6, (10, 0, 0)), (hit, (30, 0, 0)), (hit + 0.35, (25, 0, 0)), (length, (0, 0, 0)))
        a.pos('base', (0, (0, 0, 0)), (hit * 0.6, (0, -4, 2)), (hit, (0, -4, -8)), (hit + 0.35, (0, -3, -6)), (length, (0, 0, 0)))
        return self._finish(a, amp=2.0, flare=10, back=35)

    def ab_cast_up(self, length=1.6, t=0.7, float_up=0):
        """raise the weapon to the sky, left hand up, head back"""
        a = Anim('cast_up', length)
        C = self.carry
        up = {'right_arm': (-175, 0, -8), 'right_fore': (-5, 0, 0), 'weapon': (0, 0, 0), 'left_arm': (-150, 0, -35), 'left_fore': (-20, 0, 0),
              'head': (-40, 0, 0), 'chest': (-20, 0, 0), 'waist': (-8, 0, 0), 'right_leg': (-10, 0, 10), 'left_leg': (-10, 0, -10)}
        seq(a, [(0, C), (t * 0.6, add(up, {'right_arm': (15, 0, 0), 'left_arm': (20, 0, 0)})), (t, up), (length - 0.35, add(up, {'head': (5, 0, 0)})), (length, C)])
        a.pos('base', (0, (0, 0, 0)), (t, (0, 2 + float_up, 0)), (length - 0.35, (0, 1 + float_up, 0)), (length, (0, 0, 0)))
        return self._finish(a, amp=2.0, speed=3, flare=20, lift=-10)

    def ab_cast_forward(self, length=1.4, t=0.6):
        """thrust the free hand forward, weapon drawn back"""
        a = Anim('cast_forward', length)
        C = self.carry
        wind = {'left_arm': (30, 0, -20), 'left_fore': (-80, 0, 0), 'right_arm': (-30, 0, 50), 'chest': (0, -30, 0), 'head': (0, 20, 0),
                'right_leg': (-10, 0, 0), 'left_leg': (15, 0, 0)}
        push = {'left_arm': (-95, -10, 0), 'left_fore': (-5, 0, 0), 'right_arm': (25, 0, 45), 'right_fore': (-30, 0, 0), 'weapon': (-30, 0, 0),
                'chest': (10, 30, 0), 'head': (-5, -20, 0), 'right_leg': (25, 0, 0), 'left_leg': (-35, 0, 0), 'left_shin': (25, 0, 0)}
        seq(a, [(0, C), (t * 0.6, wind), (t, push), (length - 0.35, add(push, {'left_arm': (3, 0, 0)})), (length, C)])
        a.pos('base', (0, (0, 0, 0)), (t, (0, -2, -3)), (length - 0.35, (0, -2, -3)), (length, (0, 0, 0)))
        return self._finish(a, amp=1.8, flare=12, back=15)

    def ab_channel(self, length=2.6, t0=0.5, t1=2.2):
        """lean forward and channel (breath / beam), trembling"""
        a = Anim('channel', length)
        C = self.carry
        pose = {'head': (-10, 0, 0), 'neck': (15, 0, 0), 'chest': (18, 0, 0), 'right_arm': (20, 0, 35), 'left_arm': (20, 0, -35), 'left_fore': (-30, 0, 0),
                'right_leg': (-30, 0, 8), 'right_shin': (35, 0, 0), 'left_leg': (20, 0, -8), 'left_shin': (15, 0, 0)}
        frames = [(0, C), (t0 * 0.6, add(pose, {'head': (-30, 0, 0), 'chest': (-15, 0, 0)})), (t0, pose)]
        k = 0
        t = t0 + 0.15
        while t < t1:
            j = 1.5 if k % 2 else -1.5
            frames.append((t, add(pose, {'chest': (j, 0, 0), 'head': (j, 0, 0)})))
            t += 0.15
            k += 1
        frames += [(t1, pose), (length, C)]
        seq(a, frames)
        a.pos('base', (0, (0, 0, 0)), (t0, (0, -3, 1)), (t1, (0, -3, 1)), (length, (0, 0, 0)))
        return self._finish(a, amp=1.6, speed=4, flare=10, back=30)

    def ab_leap(self, length=2.2, jump=0.3, land=1.0, flip=False):
        """crouch, leap, raise the weapon and dive down"""
        a = Anim('leap', length)
        C = self.carry
        crouch = {'right_leg': (-70, 0, 8), 'right_shin': (110, 0, 0), 'left_leg': (-70, 0, -8), 'left_shin': (110, 0, 0), 'right_foot': (-35, 0, 0),
                  'left_foot': (-35, 0, 0), 'chest': (35, 0, 0), 'right_arm': (45, 0, 20), 'left_arm': (45, 0, -20), 'head': (-30, 0, 0)}
        air = {'right_arm': (-175, 0, -15), 'left_arm': (-160, 0, 30), 'right_fore': (-20, 0, 0), 'weapon': (-15, 0, 0), 'chest': (-20, 0, 0),
               'right_leg': (-60, 0, 10), 'right_shin': (90, 0, 0), 'left_leg': (10, 0, -10), 'left_shin': (40, 0, 0), 'head': (-10, 0, 0)}
        dive = {'right_arm': (-40, 0, -10), 'left_arm': (-35, 0, 30), 'right_fore': (-5, 0, 0), 'weapon': (-15, 0, 0), 'chest': (45, 0, 0), 'waist': (15, 0, 0),
                'head': (-25, 0, 0), 'right_leg': (-90, 0, 20), 'right_shin': (110, 0, 0), 'left_leg': (-40, 0, -20), 'left_shin': (120, 0, 0), 'left_foot': (-30, 0, 0)}
        seq(a, [(0, C), (jump, crouch), (jump + 0.2, air), (land - 0.12, add(air, {'right_arm': (20, 0, 0), 'chest': (15, 0, 0)})), (land, dive),
                (land + 0.55, dive), (length, C)])
        if flip:
            a.rot('base', (0, (0, 0, 0)), (jump, (12, 0, 0)), (jump + 0.1, (-40, 0, 0), 'L'), (jump + 0.3, (-200, 0, 0), 'L'), (land - 0.1, (-360, 0, 0), 'L'),
                  (land, (-352, 0, 0)), (land + 0.55, (-352, 0, 0)), (length, (-360, 0, 0)))
        a.pos('base', (0, (0, 0, 0)), (jump, (0, -10, 0)), (jump + 0.3, (0, 8, 0)), (land - 0.12, (0, 4, 0)), (land, (0, -12, -2)),
              (land + 0.55, (0, -12, -2)), (length, (0, 0, 0)))
        return self._finish(a, amp=2.2, speed=3, flare=22, back=-10, lift=-10)

    def ab_throw(self, length=1.3, release=0.55):
        """overhand throw of the weapon"""
        a = Anim('throw', length)
        C = self.carry
        wind = {'right_arm': (-160, 30, 20), 'right_fore': (-60, 0, 0), 'weapon': (-100, 0, 0), 'chest': (-10, 40, 0), 'waist': (0, 15, 0),
                'left_arm': (-80, 0, -20), 'right_leg': (20, 0, 0), 'left_leg': (-30, 0, 0), 'left_shin': (20, 0, 0)}
        rel = {'right_arm': (-70, -20, 0), 'right_fore': (-5, 0, 0), 'weapon': (-70, 0, 0), 'chest': (20, -30, 0), 'waist': (8, -12, 0),
               'left_arm': (20, 0, -30), 'right_leg': (-40, 0, 0), 'right_shin': (30, 0, 0), 'left_leg': (35, 0, 0), 'head': (-15, 0, 0)}
        seq(a, [(0, C), (release * 0.7, wind), (release, rel), (release + 0.35, add(rel, {'right_arm': (20, 0, 0)})), (length, C)])
        a.pos('base', (0, (0, 0, 0)), (release * 0.7, (0, -1, 3)), (release, (0, -2, -5)), (length, (0, 0, 0)))
        a.scl('weapon', (0, (1, 1, 1)), (release - 0.02, (1, 1, 1)), (release + 0.02, (0.01, 0.01, 0.01)), (length - 0.25, (0.01, 0.01, 0.01)), (length, (1, 1, 1)))
        return self._finish(a, amp=1.8, flare=12, back=20)

    def ab_roar(self, length=2.0, t=0.7):
        """gather (crouched, arms crossed), then explode outward roaring"""
        a = Anim('roar', length)
        C = self.carry
        gather = {'right_arm': (-70, -40, -50), 'left_arm': (-70, 40, 50), 'right_fore': (-80, 0, 0), 'left_fore': (-80, 0, 0), 'chest': (35, 0, 0),
                  'head': (30, 0, 0), 'right_leg': (-50, 0, 10), 'right_shin': (80, 0, 0), 'left_leg': (-50, 0, -10), 'left_shin': (80, 0, 0)}
        burst = {'right_arm': (-45, 0, 80), 'left_arm': (-45, 0, -80), 'right_fore': (-15, 0, 0), 'left_fore': (-15, 0, 0), 'chest': (-25, 0, 0),
                 'waist': (-8, 0, 0), 'head': (-40, 0, 0), 'right_leg': (-10, 0, 15), 'left_leg': (-10, 0, -15)}
        seq(a, [(0, C), (t * 0.8, gather), (t, burst), (t + 0.4, add(burst, {'chest': (-4, 0, 0), 'head': (-4, 0, 0)})), (length - 0.4, burst), (length, C)])
        a.pos('base', (0, (0, 0, 0)), (t * 0.8, (0, -8, 0)), (t, (0, 3, 0)), (t + 0.3, (0, 0, 0)), (length, (0, 0, 0)))
        return self._finish(a, amp=2.2, speed=3, flare=30, lift=-15)

    def ab_stab(self, length=1.5, t=0.55):
        """plunge the weapon vertically into the ground in front"""
        a = Anim('stab', length)
        C = self.carry
        up = {'right_arm': (-175, 0, 5), 'left_arm': (-170, 0, -20), 'right_fore': (-10, 0, 0), 'left_fore': (-30, 0, 0), 'weapon': (180, 0, 0),
              'chest': (-15, 0, 0), 'head': (-25, 0, 0)}
        down = {'right_arm': (-70, 0, 5), 'left_arm': (-70, 0, -20), 'right_fore': (-20, 0, 0), 'left_fore': (-30, 0, 0), 'weapon': (110, 0, 0),
                'chest': (35, 0, 0), 'waist': (10, 0, 0), 'head': (-10, 0, 0), 'right_leg': (-60, 0, 10), 'right_shin': (90, 0, 0),
                'left_leg': (-20, 0, -10), 'left_shin': (85, 0, 0), 'left_foot': (-25, 0, 0)}
        seq(a, [(0, C), (t * 0.65, up), (t, down), (length - 0.4, add(down, {'chest': (-3, 0, 0)})), (length, C)])
        a.pos('base', (0, (0, 0, 0)), (t * 0.65, (0, 3, 0)), (t, (0, -10, -1)), (length - 0.4, (0, -10, -1)), (length, (0, 0, 0)))
        return self._finish(a, amp=1.8, flare=25)

    def ab_flurry(self, length=1.4, n=5, t0=0.15):
        """rapid alternating slashes"""
        a = Anim('flurry', length)
        C = self.carry
        s1 = {'right_arm': (-150, 20, -10), 'right_fore': (-30, 0, 0), 'weapon': (-20, 0, 0), 'chest': (0, 30, 0), 'left_arm': (-40, 0, -30)}
        s2 = {'right_arm': (-50, -60, 15), 'right_fore': (-5, 0, 0), 'weapon': (-40, 0, 0), 'chest': (12, -30, 0), 'left_arm': (-110, 0, -40), 'left_fore': (-20, 0, 0)}
        s3 = {'right_arm': (-80, 60, 40), 'right_fore': (-5, 0, 0), 'weapon': (-50, 0, 0), 'chest': (8, 35, 0), 'left_arm': (10, 0, -20)}
        cyc_ = [s1, s2, s3, s2]
        frames = [(0, C)]
        span = length - t0 - 0.35
        for k in range(n + 1):
            frames.append((t0 + span * k / n, add(cyc_[k % 4], {'right_leg': (-25, 0, 0), 'right_shin': (35, 0, 0), 'left_leg': (15, 0, 0)})))
        frames.append((length, C))
        seq(a, frames)
        a.pos('base', (0, (0, 0, 0)), (t0, (0, -2, -2)), (length - 0.35, (0, -2, -6)), (length, (0, 0, 0)))
        a.rot('base', (0, (0, 0, 0)), (t0, (8, 0, 0)), (length - 0.35, (8, 0, 0)), (length, (0, 0, 0)))
        return self._finish(a, amp=1.8, speed=3, flare=12, back=20)

    def ab_blink(self, length=1.1, vanish=0.25, back=0.45):
        """crouch, vanish into the element, reappear with a strike"""
        a = Anim('blink', length)
        C = self.carry
        crouch = {'right_leg': (-45, 0, 8), 'right_shin': (70, 0, 0), 'left_leg': (-45, 0, -8), 'left_shin': (70, 0, 0), 'chest': (25, 0, 0),
                  'right_arm': (35, 0, 20), 'left_arm': (35, 0, -20)}
        strike = {'right_arm': (-45, -60, 15), 'right_fore': (-5, 0, 0), 'weapon': (-40, 0, 0), 'chest': (15, -30, 0), 'left_arm': (-20, 0, -40),
                  'right_leg': (-50, 0, 0), 'right_shin': (40, 0, 0), 'left_leg': (40, 0, 0)}
        seq(a, [(0, C), (vanish, crouch), (back, add(strike, {'right_arm': (-100, 80, 0)})), (back + 0.15, strike), (length, C)])
        a.scl('base', (0, (1, 1, 1)), (vanish - 0.05, (1, 1, 1)), (vanish + 0.05, (0.05, 1.6, 0.05)), (back - 0.05, (0.05, 1.6, 0.05)),
              (back + 0.05, (1.1, 0.95, 1.1)), (back + 0.2, (1, 1, 1)), (length, (1, 1, 1)))
        a.pos('base', (0, (0, 0, 0)), (vanish, (0, -6, 0)), (back, (0, -2, -4)), (length, (0, 0, 0)))
        return self._finish(a, amp=2.0, speed=3, flare=10, back=25)

    def ab_hover(self, length=3.0, rise=0.5, fall=2.5):
        """ascend and hover with arms spread, then drop"""
        a = Anim('hover', length)
        C = self.carry
        fly = {'right_arm': (-60, 0, 70), 'left_arm': (-60, 0, -70), 'right_fore': (-15, 0, 0), 'left_fore': (-15, 0, 0), 'weapon': (-40, 0, 0),
               'chest': (-15, 0, 0), 'head': (-20, 0, 0), 'right_leg': (-15, 0, 8), 'right_shin': (35, 0, 0), 'left_leg': (5, 0, -8), 'left_shin': (20, 0, 0)}
        frames = [(0, C), (rise * 0.5, {'right_leg': (-50, 0, 0), 'right_shin': (80, 0, 0), 'left_leg': (-50, 0, 0), 'left_shin': (80, 0, 0), 'chest': (25, 0, 0)}),
                  (rise, fly)]
        t = rise + 0.4
        k = 0
        while t < fall:
            frames.append((t, add(fly, {'right_arm': (8 if k % 2 else -8, 0, 0), 'left_arm': (8 if k % 2 else -8, 0, 0)})))
            t += 0.4
            k += 1
        frames += [(fall, add(fly, {'right_arm': (-110, 0, -60), 'left_arm': (-110, 0, 60)})), (length, C)]
        seq(a, frames)
        a.pos('base', (0, (0, 0, 0)), (rise * 0.5, (0, -6, 0)), (rise, (0, 3, 0)), (fall, (0, 3, 0)), (length, (0, 0, 0)))
        return self._finish(a, amp=2.4, speed=4, flare=20, back=-20, lift=-20)
