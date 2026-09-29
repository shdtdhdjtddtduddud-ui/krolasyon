"""Hulk animations.  Each animation is a python function t -> {bone: {rot,pos,scale}}.
They are sampled to Bedrock keyframes (20 fps) and can also be posed by preview.py.
Sign notes (Bedrock): rx<0 swings a limb forward, right arm rz>0 = outward, y up, z+ = backwards."""
import math

S = math.sin
C = math.cos
PI = math.pi


def ease(x):
    x = max(0.0, min(1.0, x))
    return x * x * (3 - 2 * x)


def out_back(x):
    x = max(0.0, min(1.0, x))
    c1 = 1.70158
    c3 = c1 + 1
    return 1 + c3 * (x - 1) ** 3 + c1 * (x - 1) ** 2


def merge(*poses):
    out = {}
    for p in poses:
        for b, ch in p.items():
            o = out.setdefault(b, {})
            for k, v in ch.items():
                if k in o:
                    o[k] = tuple(a + c for a, c in zip(o[k], v)) if k != "scale" else tuple(a * c for a, c in zip(o[k], v))
                else:
                    o[k] = tuple(v)
    return out


def P(**kw):
    """P(torso=(x,y,z), armR=..., pos_body=(..), scale_root=(..))  rotations by default; prefix p_ = position, s_ = scale"""
    out = {}
    for k, v in kw.items():
        if k.startswith("p_"):
            out.setdefault("h_" + k[2:], {})["pos"] = tuple(v)
        elif k.startswith("s_"):
            out.setdefault("h_" + k[2:], {})["scale"] = tuple(v)
        else:
            out.setdefault("h_" + k, {})["rot"] = tuple(v)
    return out


def arms(x=0, z=0, fx=0, y=0, hx=0):
    """symmetric arms: upper arm x/z, forearm x, hand x"""
    return P(armR=(x, y, z), armL=(x, -y, -z), forearmR=(fx, 0, 0), forearmL=(fx, 0, 0),
             handR=(hx, 0, 0), handL=(hx, 0, 0))


def legs(x=0, z=0, sx=0, fx=0):
    return P(legR=(x, 0, z), legL=(x, 0, -z), shinR=(sx, 0, 0), shinL=(sx, 0, 0),
             footR=(fx, 0, 0), footL=(fx, 0, 0))


def keys(kf):
    """kf = [(t, pose, ease_fn?)]  -> function(t). Blends numerically between poses."""
    kf = sorted(kf, key=lambda k: k[0])

    def fn(t):
        if t <= kf[0][0]:
            return kf[0][1]
        if t >= kf[-1][0]:
            return kf[-1][1]
        for i in range(len(kf) - 1):
            t0, p0 = kf[i][0], kf[i][1]
            t1, p1 = kf[i + 1][0], kf[i + 1][1]
            if t0 <= t <= t1:
                e = kf[i + 1][2] if len(kf[i + 1]) > 2 else ease
                a = e((t - t0) / (t1 - t0))
                return lerp_pose(p0, p1, a)
        return kf[-1][1]
    return fn


def lerp_pose(p0, p1, a):
    out = {}
    for b in set(p0) | set(p1):
        o = {}
        for ch, dflt in (("rot", 0.0), ("pos", 0.0), ("scale", 1.0)):
            v0 = p0.get(b, {}).get(ch); v1 = p1.get(b, {}).get(ch)
            if v0 is None and v1 is None:
                continue
            v0 = v0 or (dflt,) * 3; v1 = v1 or (dflt,) * 3
            o[ch] = tuple(x + (y - x) * a for x, y in zip(v0, v1))
        out[b] = o
    return out


# ============================================================================= LOOPS
def idle(t):
    b = S(2 * PI * t / 3.0)
    b2 = S(2 * PI * t / 3.0 - 0.8)
    return merge(
        P(body=(0, 0, 0), p_body=(0, b * 0.22, 0), torso=(-4 + b * 1.6, S(2 * PI * t / 6) * 1.5, 0),
          head=(3 - b * 1.2, 0, 0), jaw=(2 + b * 1.5, 0, 0)),
        P(s_torso=(1 + b * 0.012, 1 + b * 0.02, 1 + b * 0.012)),
        arms(x=-3 + b2 * 1.5, z=9 + b * 1.0, fx=-12 - b * 2, hx=-6),
        legs(x=-1, z=4.5, sx=2, fx=-1),
    )


def walk(t):
    ph = 2 * PI * t
    s, c = S(ph), C(ph)
    knee = lambda a: max(0.0, -C(a)) * 42
    return merge(
        P(p_body=(0, -abs(s) * 1.1 + 0.4, 0), torso=(-6, -s * 7, s * 3.5), head=(6 + abs(s) * 1.2, s * 3, -s * 1.5),
          body=(0, s * 4, 0)),
        P(armR=(-s * 24, 0, 9 + c * 1.5), armL=(s * 24, 0, -9 + c * 1.5), forearmR=(-16 - max(0.0, -s) * 22, 0, 0),
          forearmL=(-16 - max(0.0, s) * 22, 0, 0), handR=(-8, 0, 0), handL=(-8, 0, 0)),
        P(legR=(s * 30, 0, 3), legL=(-s * 30, 0, -3), shinR=(knee(ph) if True else 0, 0, 0), shinL=(knee(ph + PI), 0, 0),
          footR=(-s * 8, 0, 0), footL=(s * 8, 0, 0)),
    )


def run(t):
    ph = 2 * PI * t
    s, c = S(ph), C(ph)
    knee = lambda a: max(0.0, -C(a)) * 70
    return merge(
        P(p_body=(0, -abs(c) * 0.9 + 1.0, 0), torso=(-16, -s * 11, s * 4), head=(14, s * 5, 0), jaw=(10, 0, 0),
          body=(0, s * 6, 0)),
        P(armR=(-s * 62 - 10, 0, 6), armL=(s * 62 - 10, 0, -6), forearmR=(-55 - max(0.0, -s) * 40, 0, 0),
          forearmL=(-55 - max(0.0, s) * 40, 0, 0), handR=(-10, 0, 0), handL=(-10, 0, 0)),
        P(legR=(s * 52, 0, 2), legL=(-s * 52, 0, -2), shinR=(knee(ph), 0, 0), shinL=(knee(ph + PI), 0, 0),
          footR=(-s * 14, 0, 0), footL=(s * 14, 0, 0)),
    )


def jump_up(t):
    b = S(2 * PI * t / 1.0)
    return merge(
        P(p_body=(0, 0.5, 0), torso=(-10, 0, 0), head=(-10, 0, 0), jaw=(14, 0, 0)),
        arms(x=38, z=24, fx=-18, hx=-4),
        P(legR=(-30, 0, 6), legL=(-8, 0, -6), shinR=(46, 0, 0), shinL=(20, 0, 0), footR=(20, 0, 0), footL=(10, 0, 0)),
        P(s_torso=(1, 1 + b * 0.008, 1)),
    )


def fall(t):
    b = S(2 * PI * t / 0.8)
    return merge(
        P(p_body=(0, 0, 0), torso=(-6, 0, 0), head=(-4, 0, 0), jaw=(10 + b * 3, 0, 0)),
        arms(x=-25 + b * 6, z=48 + b * 4, fx=-24, hx=-8),
        legs(x=-14, z=10, sx=16, fx=14),
    )


def sneak(t):
    b = S(2 * PI * t / 1.6)
    return merge(
        P(p_body=(0, -5.5 + b * 0.15, 0), torso=(-26 + b * 1.5, 0, 0), head=(20, 0, 0), jaw=(6 + b * 2, 0, 0)),
        arms(x=-14, z=12, fx=-28, hx=-10),
        legs(x=-48, z=10, sx=78, fx=-30),
    )


def swim(t):
    ph = 2 * PI * t
    return merge(P(torso=(-36, 0, 0), head=(30, 0, 0), p_body=(0, -2, 0)),
                 P(armR=(-100 + S(ph) * 40, 0, 20), armL=(-100 - S(ph) * 40, 0, -20), forearmR=(-20, 0, 0), forearmL=(-20, 0, 0)),
                 P(legR=(S(ph * 2) * 30, 0, 0), legL=(-S(ph * 2) * 30, 0, 0)))


# ============================================================================= ATTACK OVERLAY (variable.attack_time)
def attack(a):
    """a = attack_time 0..1 : right-hand hook + torso twist (additive)"""
    s = S(a * PI)
    w = S(a * PI * 0.5) ** 2
    return merge(
        P(torso=(0, -34 * s, 0), body=(0, 6 * s, 0), head=(0, 18 * s, 0), p_torso=(0, 0, 0)),
        P(armR=(-96 * s, -18 * s, -6 * s), forearmR=(-34 * s, 0, 0), handR=(-10 * s, 0, 0)),
        P(armL=(22 * s, 0, 0), forearmL=(-20 * s, 0, 0)),
        P(legR=(14 * s, 0, 0), legL=(-10 * s, 0, 0)),
    )


# ============================================================================= ONE SHOTS
def _sh(t, amp, f):
    return S(t * f) * amp


def transform(t):
    """3.6 s.  0-1.2 convulse + hunch, 1.2-2.6 grow & bulge, 2.6-3.1 ROAR pose, 3.1-3.6 settle"""
    sc = keys([(0, {"h_root": {"scale": (0.60, 0.60, 0.60)}}),
               (0.9, {"h_root": {"scale": (0.62, 0.62, 0.62)}}),
               (1.6, {"h_root": {"scale": (0.74, 0.74, 0.74)}}),
               (2.3, {"h_root": {"scale": (0.90, 0.90, 0.90)}}),
               (2.8, {"h_root": {"scale": (1.07, 1.07, 1.07)}}),
               (3.6, {"h_root": {"scale": (1.0, 1.0, 1.0)}})])(t)
    pose = keys([
        (0.0, merge(P(torso=(-22, 0, 0), head=(18, 0, 0), jaw=(6, 0, 0), p_body=(0, -3.5, 0)), arms(x=-10, z=6, fx=-30), legs(x=-30, z=6, sx=50, fx=-20))),
        (1.2, merge(P(torso=(-30, 0, 0), head=(26, 0, 0), jaw=(14, 0, 0), p_body=(0, -4.5, 0)), arms(x=-14, z=10, fx=-70, hx=-30), legs(x=-34, z=8, sx=58, fx=-24))),
        (2.1, merge(P(torso=(-12, 0, 0), head=(6, 0, 0), jaw=(24, 0, 0), p_body=(0, -2.2, 0)), arms(x=8, z=30, fx=-40, hx=-30), legs(x=-20, z=10, sx=30, fx=-10))),
        (2.7, merge(P(torso=(22, 0, 0), head=(-34, 0, 0), jaw=(52, 0, 0), p_body=(0, 0.5, 0)), arms(x=6, z=58, fx=-22, hx=-40), legs(x=-6, z=12, sx=8, fx=-4))),
        (3.1, merge(P(torso=(20, 0, 0), head=(-30, 0, 0), jaw=(48, 0, 0), p_body=(0, 0.5, 0)), arms(x=6, z=62, fx=-20, hx=-40), legs(x=-6, z=12, sx=8, fx=-4))),
        (3.6, merge(P(torso=(-4, 0, 0), head=(3, 0, 0), jaw=(2, 0, 0)), arms(x=-3, z=9, fx=-12, hx=-6), legs(x=-1, z=4.5, sx=2, fx=-1))),
    ])(t)
    # tremble (fades as the roar starts)
    k = max(0.0, 1 - t / 2.4) * (1 if t < 2.4 else 0)
    if t >= 2.4:
        k = 0
    trem = merge(
        P(torso=(S(t * 91) * 2.4 * (0.4 + k), S(t * 73) * 3.0 * (0.4 + k), S(t * 83) * 2.0 * k),
          head=(S(t * 97) * 3 * (0.4 + k), S(t * 67) * 4 * k, 0)),
        P(armR=(S(t * 89) * 4 * k, 0, S(t * 71) * 3 * k), armL=(S(t * 79) * 4 * k, 0, -S(t * 61) * 3 * k)),
    )
    # muscle bulging pulses
    p1 = 0.5 + 0.5 * S(t * 9.0)
    g = ease((t - 0.8) / 1.8) * (1 - ease((t - 2.9) / 0.7))
    bulge = P(s_torso=(1 + 0.10 * g * p1, 1 + 0.06 * g * p1, 1 + 0.10 * g * p1),
              s_armR=(1 + 0.12 * g * p1,) * 3, s_armL=(1 + 0.12 * g * p1,) * 3,
              s_forearmR=(1 + 0.12 * g,) * 3, s_forearmL=(1 + 0.12 * g,) * 3)
    return merge(pose, sc, trem, bulge)


def revert(t):
    """2.0s shrink"""
    sc = keys([(0, {"h_root": {"scale": (1, 1, 1)}}), (0.5, {"h_root": {"scale": (0.95, 0.95, 0.95)}}),
               (1.5, {"h_root": {"scale": (0.62, 0.62, 0.62)}}), (2.0, {"h_root": {"scale": (0.60, 0.60, 0.60)}})])(t)
    pose = keys([
        (0.0, merge(P(torso=(0, 0, 0)), arms(x=0, z=9, fx=-10), legs(x=0, z=4))),
        (0.7, merge(P(torso=(-16, 0, 0), head=(14, 0, 0), jaw=(20, 0, 0), p_body=(0, -2, 0)), arms(x=-4, z=30, fx=-20), legs(x=-18, z=6, sx=30, fx=-12))),
        (1.6, merge(P(torso=(-28, 0, 0), head=(22, 0, 0), p_body=(0, -4, 0)), arms(x=-8, z=6, fx=-20), legs(x=-32, z=6, sx=54, fx=-22))),
        (2.0, merge(P(torso=(-24, 0, 0), head=(18, 0, 0), p_body=(0, -3.5, 0)), arms(x=-8, z=6, fx=-16), legs(x=-28, z=6, sx=48, fx=-20))),
    ])(t)
    k = max(0.0, 1 - t / 1.4)
    trem = P(torso=(S(t * 91) * 2.5 * k, S(t * 71) * 3 * k, 0), head=(S(t * 83) * 3 * k, 0, 0))
    return merge(pose, sc, trem)


def roar(t):
    """2.4s: crouch back, blow chest out, scream, beat air with fists, settle"""
    kf = keys([
        (0.0, merge(P(torso=(-4, 0, 0), head=(3, 0, 0)), arms(x=-3, z=9, fx=-12), legs(x=-1, z=4.5))),
        (0.35, merge(P(torso=(-30, 0, 0), head=(26, 0, 0), jaw=(0, 0, 0), p_body=(0, -3.5, 0)), arms(x=-30, z=6, fx=-60, hx=-30), legs(x=-32, z=8, sx=56, fx=-22))),
        (0.75, merge(P(torso=(26, 0, 0), head=(-36, 0, 0), jaw=(54, 0, 0), p_body=(0, 0.8, 0)), arms(x=8, z=64, fx=-10, hx=-40), legs(x=-4, z=14, sx=6, fx=-2))),
        (1.7, merge(P(torso=(22, 0, 0), head=(-30, 0, 0), jaw=(50, 0, 0), p_body=(0, 0.6, 0)), arms(x=6, z=58, fx=-12, hx=-40), legs(x=-4, z=14, sx=6, fx=-2))),
        (2.4, merge(P(torso=(-4, 0, 0), head=(3, 0, 0), jaw=(2, 0, 0)), arms(x=-3, z=9, fx=-12, hx=-6), legs(x=-1, z=4.5, sx=2, fx=-1))),
    ])
    p = kf(t)
    k = ease((t - 0.7) / 0.2) * (1 - ease((t - 1.8) / 0.5))
    shake = P(torso=(S(t * 95) * 1.6 * k, S(t * 77) * 2.4 * k, 0), head=(S(t * 101) * 2.2 * k, 0, 0),
              armR=(0, 0, S(t * 60) * 5 * k), armL=(0, 0, S(t * 66) * 5 * k))
    chest = P(s_torso=(1 + 0.08 * k, 1 + 0.04 * k, 1 + 0.08 * k))
    return merge(p, shake, chest)


def smash(t):
    """1.3s: both fists overhead then a devastating slam at t~0.55"""
    kf = keys([
        (0.0, merge(P(torso=(-4, 0, 0)), arms(x=-3, z=9, fx=-12), legs(x=-1, z=4.5))),
        (0.42, merge(P(torso=(22, 0, 0), head=(-14, 0, 0), jaw=(30, 0, 0), p_body=(0, 0.5, 0)), arms(x=-172, z=10, fx=-24, hx=-10), legs(x=6, z=8, sx=4, fx=0))),
        (0.58, merge(P(torso=(-40, 0, 0), head=(26, 0, 0), jaw=(40, 0, 0), p_body=(0, -5.0, 0)), arms(x=-58, z=4, fx=-8, hx=-6), legs(x=-40, z=10, sx=70, fx=-26)), lambda x: x ** 3),
        (0.95, merge(P(torso=(-34, 0, 0), head=(20, 0, 0), jaw=(30, 0, 0), p_body=(0, -4.6, 0)), arms(x=-52, z=6, fx=-10, hx=-6), legs(x=-38, z=10, sx=66, fx=-24))),
        (1.3, merge(P(torso=(-4, 0, 0), head=(3, 0, 0)), arms(x=-3, z=9, fx=-12), legs(x=-1, z=4.5))),
    ])
    return kf(t)


def clap(t):
    """1.4s Thunderclap: arms wide, then both fists crash together in front"""
    kf = keys([
        (0.0, merge(P(torso=(-4, 0, 0)), arms(x=-3, z=9, fx=-12), legs(x=-1, z=4.5))),
        (0.4, merge(P(torso=(8, 0, 0), head=(-8, 0, 0), jaw=(30, 0, 0), p_body=(0, 0.3, 0)),
                    P(armR=(-10, 42, 78), armL=(-10, -42, -78), forearmR=(-14, 0, 0), forearmL=(-14, 0, 0)), legs(x=-4, z=12, sx=8))),
        (0.55, merge(P(torso=(-16, 0, 0), head=(12, 0, 0), jaw=(36, 0, 0), p_body=(0, -2.0, 0)),
                     P(armR=(-92, -46, 6), armL=(-92, 46, -6), forearmR=(-14, 0, 0), forearmL=(-14, 0, 0), handR=(-10, 0, 0), handL=(-10, 0, 0)),
                     legs(x=-18, z=10, sx=32, fx=-10)), lambda x: x ** 3),
        (1.0, merge(P(torso=(-12, 0, 0), head=(8, 0, 0), jaw=(20, 0, 0), p_body=(0, -1.5, 0)),
                    P(armR=(-90, -44, 4), armL=(-90, 44, -4), forearmR=(-14, 0, 0), forearmL=(-14, 0, 0)), legs(x=-16, z=10, sx=30, fx=-10))),
        (1.4, merge(P(torso=(-4, 0, 0), head=(3, 0, 0)), arms(x=-3, z=9, fx=-12), legs(x=-1, z=4.5))),
    ])
    return kf(t)


def land(t):
    """1.0s heavy landing: deep crouch, one fist planted"""
    kf = keys([
        (0.0, merge(P(torso=(-20, 0, 0), head=(16, 0, 0), p_body=(0, -6.5, 0)), P(armR=(-46, 0, 14), armL=(30, 0, -24), forearmR=(-20, 0, 0)),
                    legs(x=-62, z=12, sx=100, fx=-40))),
        (0.55, merge(P(torso=(-22, 0, 0), head=(14, 0, 0), jaw=(30, 0, 0), p_body=(0, -6.0, 0)), P(armR=(-48, 0, 14), armL=(30, 0, -24), forearmR=(-20, 0, 0)),
                     legs(x=-58, z=12, sx=96, fx=-38))),
        (1.0, merge(P(torso=(-4, 0, 0), head=(3, 0, 0)), arms(x=-3, z=9, fx=-12), legs(x=-1, z=4.5))),
    ])
    return kf(t)


def dive(t):
    b = S(2 * PI * t / 0.5)
    return merge(P(torso=(-42, 0, 0), head=(-12, 0, 0), jaw=(30, 0, 0), p_body=(0, 0, 0)),
                 arms(x=-165 + b * 4, z=8, fx=-6, hx=-10), legs(x=6, z=3, sx=0, fx=20),
                 P(s_torso=(1, 1, 1)))


# name -> (fn, length, loop)
ANIMS = {
    "idle": (idle, 3.0, True), "walk": (walk, 1.0, True), "run": (run, 0.72, True),
    "jump": (jump_up, 1.0, True), "fall": (fall, 0.8, True), "sneak": (sneak, 1.6, True), "swim": (swim, 1.0, True),
    "transform": (transform, 3.6, False), "revert": (revert, 2.0, False), "roar": (roar, 2.4, False),
    "smash": (smash, 1.3, False), "clap": (clap, 1.4, False), "land": (land, 1.0, False), "dive": (dive, 0.5, True),
}


def r(v):
    return [round(float(x), 3) for x in v]


def sample(fn, length, loop, fps=20):
    n = int(round(length * fps))
    tracks = {}
    for i in range(n + 1):
        t = i / fps
        p = fn(t if not (loop and i == n) else 0.0)
        for b, chs in p.items():
            for ch, v in chs.items():
                tracks.setdefault(b, {}).setdefault(ch, {})[f"{t:.2f}"] = r(v)
    bones = {}
    for b, chs in tracks.items():
        e = {}
        for ch, kv in chs.items():
            key = {"rot": "rotation", "pos": "position", "scale": "scale"}[ch]
            default = 1.0 if ch == "scale" else 0.0
            # collapse constant channels
            vals = list(kv.values())
            if all(v == vals[0] for v in vals):
                if all(abs(x - default) < 1e-6 for x in vals[0]):
                    continue
                e[key] = vals[0]
            else:
                e[key] = kv
        if e:
            bones[b] = e
    return bones


def animation_json():
    anims = {}
    for name, (fn, length, loop) in ANIMS.items():
        d = {"loop": bool(loop), "animation_length": length, "bones": sample(fn, length, loop)}
        anims[f"animation.hulk.{name}"] = d
    # speed-scaled locomotion playback
    for n in ("walk", "run"):
        anims[f"animation.hulk.{n}"]["anim_time_update"] = "q.anim_time + q.delta_time * math.clamp(q.modified_move_speed * 1.15, 0.5, 2.2)"
    # head tracking (always on) + melee swing
    anims["animation.hulk.look"] = {"loop": True, "bones": {"h_head": {"rotation": ["math.clamp(q.target_x_rotation, -50, 50)", "math.clamp(q.target_y_rotation, -75, 75)", 0]}}}
    at = attack
    atk = {}
    for i in range(0, 11):
        a = i / 10
        for b, chs in at(a).items():
            for ch, v in chs.items():
                atk.setdefault(b, {}).setdefault("rotation" if ch == "rot" else ch, {})[f"{a:.1f}"] = r(v)
    anims["animation.hulk.attack"] = {"loop": False, "animation_length": 1.0,
                                     "anim_time_update": "variable.attack_time", "bones": atk}
    return {"format_version": "1.8.0", "animations": anims}
