"""Sound design for the fire / shadow transformation forms (voices, steps, swings and every ability)."""
from sounds3 import *


def roarv(d, base, vowel='a', grit=1.8, rate=20):
    n = int(d * SR)
    v = np.tanh(sum(deep_voice(d, f, f * 0.8, vowel, rate + i * 3) for i, f in enumerate((base, base * 1.5, base * 2))) * grit)
    return v * adsr(n, 0.08, 0.95, d * 0.35)


def fire_bed(d, amt=1.0):
    n = int(d * SR)
    return (crackle(d, 90) * 0.5 + lp(brown(d), 600) * 0.6 + sweep_bp(noise(d), 300, 900, 1.5) * 0.3) * amt


def whoosh(d, f0=300, f1=3500, pw=1.5):
    return sweep_bp(noise(d), f0, f1, 1.7) * np.sin(np.pi * T(d) / d) ** pw


def explode(d=1.6, size=1.0):
    n = int(d * SR)
    return reverb(mix(boom(d, 120, 26, 0.5 * size) * 1.6, lp(noise(d), 2500) * env(n, 0.003, 0.35 * size), fire_bed(d, 0.7) * adsr(n, 0.05, 0.8, d * 0.6)), 0.4, 1.6)


def shatter(d=0.8):
    n = int(d * SR)
    cr = mix(*[np.concatenate([np.zeros(int(R.random() * 0.2 * SR)), bp(noise(0.06), 800, 5000) * env(int(0.06 * SR), 0.0005, 0.015)]) for _ in range(10)])
    return mix(pad(cr, d)[:n], boom(d, 90, 45, 0.1))


# ---- per form voice parameters: (growl base Hz, vowel, extra layer)
VOICES = {'solar': (70, 'a', 'fire'), 'dragon': (55, 'o', 'fire'), 'flame': (90, 'e', 'fire'), 'shade': (62, 'u', 'dark'), 'lancer': (80, 'a', 'hiss')}


def layer(kind, d):
    n = int(d * SR)
    if kind == 'fire':
        return fire_bed(d, 0.6) * adsr(n, 0.1, 0.9, d * 0.4)
    if kind == 'dark':
        return lp(brown(d), 300) * adsr(n, 0.1, 0.9, d * 0.4) + sweep_bp(noise(d), 1500, 200, 1.4) * 0.2
    return hp(noise(d), 3000) * adsr(n, 0.05, 0.6, d * 0.4) * 0.5


def ambient(form, v):
    f, vw, kind = VOICES[form]
    d = 2.6
    n = int(d * SR)
    breath = bp(noise(d), 200, 900) * np.abs(np.sin(np.pi * T(d) / d)) ** 1.6
    g = np.tanh(growl(d, f * (1 if v == 1 else 1.1), f * 0.9, vw, 14) * 1.4) * adsr(n, 0.4, 0.8, 0.8)
    return reverb(mix(breath * 0.6, g * 0.6, layer(kind, d) * 0.8), 0.4, 1.6)


def hurt(form, v):
    f, vw, kind = VOICES[form]
    d = 0.55
    n = int(d * SR)
    g = np.tanh(deep_voice(d, f * [2.0, 1.8, 2.2][v - 1], f * 1.3, 'a', 26) * 1.8) * env(n, 0.01, 0.15)
    return reverb(mix(g, layer(kind, d) * env(n, 0.005, 0.2) * 0.8), 0.25, 0.6)


def death(form):
    f, vw, kind = VOICES[form]
    d = 3.0
    n = int(d * SR)
    t = T(d)
    fr = np.interp(t, [0, 0.4, 2.3, 3.0], [f * 2, f * 2.6, f * 0.8, f * 0.6])
    wail = formant(osc(fr, 'saw') * rattle(n, 12, 0.3), vw) * adsr(n, 0.1, 0.9, 1.0)
    burst = np.concatenate([np.zeros(int(1.0 * SR)), explode(2.0, 1.2)])
    return reverb(mix(np.tanh(wail * 1.4), pad(burst, d)[:n] * 0.9, layer(kind, d) * 0.6), 0.5, 2.2)


def step(form, v):
    d = 0.4
    n = int(d * SR)
    base = {'solar': 70, 'dragon': 60, 'flame': 85, 'shade': 75, 'lancer': 95}[form] + v * 4
    metal = {'solar': 0.3, 'dragon': 0.5, 'flame': 0.0, 'shade': 0.0, 'lancer': 0.1}[form]
    s = mix(boom(d, base, 40, 0.06) * 1.1, lp(noise(d), 1400) * env(n, 0.002, 0.05) * 0.5,
            bell(d, 1400 + v * 120, decay=0.08) * metal * 0.3)
    if form in ('solar', 'flame', 'dragon', 'lancer'):
        s = mix(s, crackle(d, 40) * env(n, 0.01, 0.12) * 0.4)
    return s


def swing(kind, v):
    d = {'heavy': 0.7, 'blade': 0.45, 'spear': 0.55}[kind]
    n = int(d * SR)
    f0, f1 = {'heavy': (200, 2500), 'blade': (900, 6500), 'spear': (500, 4500)}[kind]
    wh = sweep_bp(noise(d), f0 * (1 + 0.15 * v), f1, 1.8) * np.sin(np.pi * T(d) / d) ** 3
    return mix(wh * 1.3, fire_bed(d, 0.4) * env(n, 0.05, 0.2), bell(d, 2600, decay=0.1) * 0.08 * (kind == 'blade'))


def transform(form):
    f, vw, kind = VOICES[form]
    d = 3.2
    n = int(d * SR)
    t = T(d)
    rise = sweep_bp(noise(d), 150, 2500, 1.4) * np.clip(t / 1.05, 0, 1) ** 2 * (t < 1.1)
    bed = layer(kind, d) * np.clip(t / 1.05, 0, 1) * (t < 1.15) * 1.3
    burst = np.concatenate([np.zeros(int(1.05 * SR)), explode(2.0, 1.4)])
    roar = np.concatenate([np.zeros(int(1.15 * SR)), roarv(1.8, f, vw)])
    return reverb(mix(rise * 0.9, bed, pad(burst, d)[:n], pad(roar, d)[:n] * 0.9), 0.45, 2.2)


def revert_f(form):
    d = 1.2
    n = int(d * SR)
    return reverb(mix(sweep_bp(noise(d), 2500, 200, 1.5) * env(n, 0.01, 0.5), layer(VOICES[form][2], d) * env(n, 0.01, 0.4)), 0.35, 1.2)


# ---- abilities
def cleave():
    d = 1.6
    return reverb(mix(whoosh(0.5, 200, 2500) * 1.2, pad(np.concatenate([np.zeros(int(0.4 * SR)), explode(1.2, 1.2)]), d)[:int(d * SR)]), 0.3, 1.2)


def cyclone():
    d = 1.8
    n = int(d * SR)
    t = T(d)
    rate = np.interp(t, [0, d], [3, 8])
    mod = 0.5 + 0.5 * np.sin(np.cumsum(2 * np.pi * rate / SR))
    return reverb(mix(sweep_bp(noise(d), 300, 2000, 1.5) * mod ** 2 * adsr(n, 0.1, 0.9, 0.4) * 1.2, fire_bed(d) * adsr(n, 0.1, 0.9, 0.4)), 0.35, 1.2)


def seal_charge():
    d = 1.8
    n = int(d * SR)
    tone = sum(osc(glide(d, f, f * 2)) for f in (220, 330, 440)) * np.linspace(0.1, 1, n) ** 2 * 0.3
    return reverb(mix(tone, fire_bed(d, 0.6) * np.linspace(0.2, 1, n), bell(d, 1760, decay=0.8) * 0.1), 0.5, 1.8)


def phoenix_dash():
    d = 1.0
    n = int(d * SR)
    cry = formant(osc(glide(0.6, 900, 1400), 'saw'), 'e') * env(int(0.6 * SR), 0.02, 0.2) * 0.4
    return reverb(mix(whoosh(d, 200, 3000, 1.2) * 1.2, fire_bed(d) * env(n, 0.02, 0.4), cry), 0.35, 1.0)


def meteor_call():
    d = 2.2
    n = int(d * SR)
    fall = osc(glide(d, 1400, 180)) * np.linspace(0.1, 1, n) * 0.3
    return reverb(mix(sweep_bp(noise(d), 3000, 300, 1.5) * np.linspace(0.2, 1, n), fall, fire_bed(d, 0.6)), 0.4, 1.4)


def breath():
    d = 2.2
    n = int(d * SR)
    roar = lp(noise(d), 1500) * adsr(n, 0.1, 0.95, 0.4) * 1.3
    return reverb(mix(roar, fire_bed(d, 1.2) * adsr(n, 0.1, 0.95, 0.4), roarv(0.8, 55, 'o') * 0.4), 0.3, 1.0)


def fireball_whoosh():
    d = 0.8
    n = int(d * SR)
    return mix(whoosh(d, 250, 2200, 1.3) * 1.1, fire_bed(d) * env(n, 0.02, 0.3), boom(d, 140, 70, 0.08) * 0.5)


def shield_up():
    d = 1.4
    n = int(d * SR)
    return reverb(mix(bell(d, 660, decay=0.8) * 0.5, bell(d, 990, decay=0.6) * 0.3, boom(d, 100, 40, 0.3), fire_bed(d, 0.5) * env(n, 0.02, 0.6)), 0.5, 1.6)


def dive():
    d = 1.4
    n = int(d * SR)
    return reverb(mix(whoosh(0.7, 3500, 300, 1.2), pad(np.concatenate([np.zeros(int(0.5 * SR)), explode(0.9, 1.0)]), d)[:n]), 0.3, 1.0)


def dragon_summon():
    d = 2.6
    return reverb(mix(roarv(d, 48, 'o', 2.2, 16) * 1.1, fire_bed(d, 1.0), whoosh(d, 150, 1500, 1.0) * 0.6), 0.5, 2.0)


def flame_blink():
    d = 0.8
    n = int(d * SR)
    return reverb(mix(sweep_bp(noise(d), 4000, 400, 1.5) * env(n, 0.005, 0.15), pad(np.concatenate([np.zeros(int(0.2 * SR)), fireball_whoosh()]), d)[:n] * 0.8), 0.3, 0.8)


def ember_rain():
    d = 2.6
    n = int(d * SR)
    out = np.zeros(n)
    for _ in range(14):
        s = int(R.random() * (n - 0.5 * SR))
        x = whoosh(0.35, 3000, 500, 1.0) * 0.5 + boom(0.35, 120, 50, 0.05) * 0.6
        out[s:s + len(x)] += x[:n - s]
    return reverb(mix(out, fire_bed(d, 0.6)), 0.4, 1.4)


def nova():
    return mix(explode(2.0, 1.6), roarv(1.0, 90, 'e') * 0.4)


def crescent():
    d = 0.6
    n = int(d * SR)
    return mix(sweep_bp(noise(d), 800, 6000, 1.8) * np.sin(np.pi * T(d) / d) ** 2 * 1.3, fire_bed(d, 0.6) * env(n, 0.02, 0.3), bell(d, 2400, decay=0.1) * 0.1)


def phoenix_rise():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    cry = formant(osc(np.interp(t, [0, 0.5, 1.5, 3], [700, 1300, 1100, 900]), 'saw'), 'e') * adsr(n, 0.2, 0.7, 1.0) * 0.5
    return reverb(mix(cry, fire_bed(d, 1.2) * adsr(n, 0.2, 0.9, 0.8), whoosh(d, 200, 2500, 0.8) * 0.8), 0.5, 2.0)


def shadow_step():
    d = 0.9
    n = int(d * SR)
    return reverb(mix(sweep_bp(noise(d), 200, 1800, 1.8) * np.sin(np.pi * T(d) / d) ** 1.2, boom(d, 60, 30, 0.2) * 0.9,
                      formant(osc(glide(d, 70, 55), 'saw'), 'u') * env(n, 0.05, 0.3) * 0.5), 0.4, 1.0)


def blood_flurry():
    d = 1.4
    n = int(d * SR)
    out = np.zeros(n)
    for k in range(6):
        s = int((0.12 + k * 0.19) * SR)
        x = swing('blade', 1 + k % 2) * 0.9
        out[s:s + len(x)] += x[:n - s]
    return reverb(mix(out, bubbles(d, 10, 120, 400) * 0.3), 0.25, 0.8)


def tail_lash():
    d = 1.4
    n = int(d * SR)
    t = T(d)
    mod = 0.5 + 0.5 * np.sin(2 * np.pi * 5 * t)
    crack = pad(np.concatenate([np.zeros(int(0.6 * SR)), hp(noise(0.08), 1500) * env(int(0.08 * SR), 0.001, 0.02) * 2]), d)[:n]
    return reverb(mix(sweep_bp(noise(d), 300, 2500, 1.6) * mod ** 2 * adsr(n, 0.1, 0.9, 0.3), crack), 0.3, 0.9)


def awaken():
    d = 3.0
    n = int(d * SR)
    return reverb(mix(roarv(d, 50, 'u', 2.4, 14) * 1.1, heartbeat(d, 80, 1.02) * 0.9, lp(brown(d), 200) * adsr(n, 0.1, 0.9, 1.0), boom(d, 80, 24, 0.6)), 0.5, 2.2)


def spear_throw():
    d = 0.9
    n = int(d * SR)
    grunt = np.tanh(growl(0.35, 150, 110, 'a', 30) * 2) * env(int(0.35 * SR), 0.01, 0.1)
    return mix(grunt * 0.6, whoosh(d, 300, 5000, 1.6) * 1.1, fire_bed(d, 0.5) * env(n, 0.05, 0.3))


def spear_impact():
    return mix(explode(1.4, 1.0), shatter(1.4) * 0.8)


def charge():
    d = 1.2
    n = int(d * SR)
    return reverb(mix(whoosh(d, 150, 2000, 0.8) * 1.2, boom(d, 70, 40, 0.3) * 0.8, roarv(0.7, 80, 'a') * 0.5, fire_bed(d, 0.5)), 0.3, 1.0)


def lava_erupt():
    d = 1.8
    n = int(d * SR)
    out = np.zeros(n)
    for k in range(3):
        s = int(k * 0.3 * SR)
        x = explode(1.2, 0.8) * (1 - k * 0.2)
        out[s:s + len(x)] += x[:n - s]
    return mix(out, shatter(d) * 0.6)


def hell_gate():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    drone = lp(sum(osc(glide(d, f, f * 0.7), 'saw') for f in (55, 58, 82)), 800) * adsr(n, 0.4, 0.9, 1.0)
    return reverb(mix(drone * 0.7, fire_bed(d, 1.0), roarv(1.5, 45, 'o') * 0.5, bell(d, 330, decay=1.5) * 0.2), 0.55, 2.4)


def spear_fall():
    d = 0.8
    n = int(d * SR)
    return mix(sweep_bp(noise(d), 4000, 500, 1.5) * np.linspace(0.3, 1, n) * 0.8, pad(np.concatenate([np.zeros(int(0.55 * SR)), boom(0.25, 150, 60, 0.05)]), d)[:n])


FORMS = ('solar', 'dragon', 'flame', 'shade', 'lancer')

if __name__ == '__main__':
    jobs = {}
    for fm in FORMS:
        for v in (1, 2):
            jobs[f'{fm}_ambient{v}'] = (lambda fm=fm, v=v: ambient(fm, v))
        for v in (1, 2, 3):
            jobs[f'{fm}_hurt{v}'] = (lambda fm=fm, v=v: hurt(fm, v))
            jobs[f'{fm}_step{v}'] = (lambda fm=fm, v=v: step(fm, v))
        jobs[f'{fm}_death'] = (lambda fm=fm: death(fm))
        jobs[f'{fm}_transform'] = (lambda fm=fm: transform(fm))
        jobs[f'{fm}_revert'] = (lambda fm=fm: revert_f(fm))
    for k in ('heavy', 'blade', 'spear'):
        for v in (1, 2):
            jobs[f'{k}_swing{v}'] = (lambda k=k, v=v: swing(k, v))
    for f in (cleave, cyclone, seal_charge, phoenix_dash, meteor_call, breath, fireball_whoosh, shield_up, dive, dragon_summon, flame_blink,
              ember_rain, nova, crescent, phoenix_rise, shadow_step, blood_flurry, tail_lash, awaken, spear_throw, spear_impact, charge,
              lava_erupt, hell_gate, spear_fall):
        jobs[f.__name__] = f
    jobs['fire_explode'] = lambda: explode(1.6, 1.2)
    quiet = {k: -7 for k in jobs if 'step' in k}
    quiet.update({k: -4 for k in jobs if 'ambient' in k})
    for name, fn in jobs.items():
        save(name, fn(), quiet.get(name, -1.0))
    print(len(jobs), 'sounds')
