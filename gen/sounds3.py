"""Sound design for Aigoar, the abyssal tide lord (player transformation): water, pressure, vortex and deep roars."""
from sounds2 import *


def slosh(d, lo=200, hi=1400, rate=3.0):
    n = int(d * SR)
    t = T(d)
    m = 0.5 + 0.5 * np.sin(2 * np.pi * rate * t + np.sin(2 * np.pi * rate * 0.37 * t) * 2)
    return bp(brown(d) * 0.6 + noise(d) * 0.4, lo, hi) * m


def spray(d, a=0.005, dec=0.25, lo=2500, hi=9000):
    n = int(d * SR)
    return bp(noise(d), lo, hi) * env(n, a, dec)


def splash(d=0.8, size=1.0):
    n = int(d * SR)
    body = lp(noise(d), 1800 * size) * env(n, 0.003, 0.18 * size)
    return mix(body * 1.2, spray(d, 0.004, 0.12 * size) * 0.6, bubbles(d, int(10 * size), 180, 600) * 0.5, boom(d, 110, 45, 0.08 * size) * 0.6)


def deep_voice(d, f0, f1, vowel='o', rate=18):
    n = int(d * SR)
    t = T(d)
    f = glide(d, f0, f1) * (1 + 0.02 * np.sin(2 * np.pi * 5 * t))
    src = osc(f, 'saw') + 0.6 * osc(f * 0.5, 'saw') + 0.3 * osc(f * 1.5)
    src = src * rattle(n, rate, 0.35)
    gurgle = bubbles(d, int(d * 20), 120, 400) * 0.4
    return formant(src, vowel) + gurgle


def aigoar_ambient(v):
    d = 2.8
    n = int(d * SR)
    t = T(d)
    hum = sum(osc(f * (1 + 0.01 * np.sin(2 * np.pi * 0.6 * t + i))) for i, f in enumerate((55, 82.4, 110 if v == 1 else 98)))
    breath = bp(noise(d), 200, 900) * np.abs(np.sin(np.pi * t / d)) ** 1.6
    return reverb(mix(hum * 0.35 * adsr(n, 0.6, 0.9, 0.8), breath * 0.7, slosh(d, 150, 900, 1.2) * 0.5, bubbles(d, 18, 140, 520) * 0.35), 0.45, 1.8)


def aigoar_hurt(v):
    d = 0.6
    n = int(d * SR)
    grunt = np.tanh(deep_voice(d, [150, 130, 170][v - 1], 95, 'a', 26) * 1.6) * env(n, 0.01, 0.16)
    return reverb(mix(grunt * 0.9, splash(d, 0.7) * 0.8), 0.25, 0.7)


def aigoar_death():
    d = 3.2
    n = int(d * SR)
    t = T(d)
    f = np.interp(t, [0, 0.4, 2.4, 3.2], [140, 190, 60, 45])
    wail = formant(osc(f, 'saw') * rattle(n, 12, 0.3), 'o') * adsr(n, 0.1, 0.9, 1.0)
    collapse = np.concatenate([np.zeros(int(1.0 * SR)), splash(2.2, 2.2)])
    return reverb(mix(np.tanh(wail * 1.4), pad(collapse, d)[:n] * 1.2, bubbles(d, 40, 100, 500) * 0.4), 0.5, 2.2)


def aigoar_step(v):
    d = 0.4
    n = int(d * SR)
    return mix(boom(d, [80, 72, 88][v - 1], 42, 0.06) * 1.1, lp(noise(d), 1400) * env(n, 0.002, 0.05) * 0.6,
               spray(d, 0.002, 0.04) * 0.35, bubbles(d, 3, 250, 700) * 0.3)


def aigoar_land():
    d = 1.0
    n = int(d * SR)
    return reverb(mix(boom(d, 95, 30, 0.25) * 1.5, splash(d, 1.5)), 0.3, 1.0)


def claw_swipe(v):
    d = 0.45
    n = int(d * SR)
    wh = sweep_bp(noise(d), [900, 1200][v - 1], [5500, 7000][v - 1], 1.8) * np.sin(np.pi * T(d) / d) ** 3
    drops = spray(d, 0.12, 0.15, 3000, 10000) * 0.4
    return mix(wh * 1.3, drops, bubbles(d, 4, 400, 900) * 0.25)


def transform():
    d = 3.2
    n = int(d * SR)
    t = T(d)
    rise = sweep_bp(noise(d), 150, 2500, 1.4) * np.clip(t / 1.05, 0, 1) ** 2 * (t < 1.1)
    swirl = slosh(d, 200, 1600, 6) * np.clip(t / 1.05, 0, 1) * (t < 1.15)
    drone = lp(sum(osc(glide(d, f, f * 2.2), 'saw') for f in (41, 61.5, 82)), 700) * np.clip(t / 1.05, 0, 1) ** 2 * (t < 1.12)
    burst_at = int(1.05 * SR)
    burst = np.concatenate([np.zeros(burst_at), mix(boom(2.0, 120, 28, 0.6) * 1.6, splash(2.0, 3.0) * 1.2)])
    roar_d = 1.8
    roar = np.tanh(sum(deep_voice(roar_d, f, f * 0.8, 'a', 20 + i * 3) for i, f in enumerate((62, 93, 124))) * 1.8) * adsr(int(roar_d * SR), 0.1, 0.95, 0.7)
    roar = np.concatenate([np.zeros(int(1.15 * SR)), roar])
    shimmer = np.concatenate([np.zeros(burst_at), sum(bell(1.6, f, decay=0.5) for f in (1318, 1760, 2637)) * 0.12])
    return reverb(mix(rise * 0.9, swirl * 0.6, drone * 0.5, pad(burst, d)[:n], pad(roar, d)[:n] * 0.9, pad(shimmer, d)[:n], bubbles(d, 50, 150, 700) * 0.3), 0.45, 2.2)


def revert():
    d = 1.2
    n = int(d * SR)
    down = sweep_bp(noise(d), 2500, 200, 1.5) * env(n, 0.01, 0.5)
    return reverb(mix(down, splash(d, 1.8) * 0.9, bell(d, 880, decay=0.3) * 0.1), 0.35, 1.2)


def tidal_rend():
    d = 0.9
    n = int(d * SR)
    dash = sweep_bp(noise(d), 300, 3000, 1.6) * np.sin(np.pi * np.clip(T(d) / 0.5, 0, 1)) ** 1.5
    s1 = pad(np.concatenate([np.zeros(int(0.12 * SR)), claw_swipe(1)]), d)[:n]
    s2 = pad(np.concatenate([np.zeros(int(0.45 * SR)), claw_swipe(2)]), d)[:n]
    return reverb(mix(dash * 1.1, s1, s2 * 0.9, spray(d, 0.05, 0.3) * 0.4), 0.25, 0.8)


def maelstrom():
    d = 1.6
    n = int(d * SR)
    t = T(d)
    rate = np.interp(t, [0, d], [2, 9])
    swirl = sweep_bp(noise(d), 200, 1500, 1.5) * (0.5 + 0.5 * np.sin(np.cumsum(2 * np.pi * rate / SR)))
    tone = osc(glide(d, 110, 330)) * 0.2
    return reverb(mix(swirl * adsr(n, 0.2, 0.9, 0.4) * 1.2, tone * adsr(n, 0.3, 0.8, 0.4), bubbles(d, 20, 150, 500) * 0.4), 0.4, 1.4)


def vortex_loop():
    d = 5.0
    n = int(d * SR)
    t = T(d)
    rot = 0.5 + 0.5 * np.sin(2 * np.pi * 3.2 * t)
    body = slosh(d, 120, 1100, 1.6) * (0.6 + 0.4 * rot)
    whirl = sweep_bp(noise(d), 400, 900, 1.4) * rot ** 2 * 0.6
    return reverb(mix(body, whirl, bubbles(d, 60, 120, 500) * 0.5, lp(brown(d), 200) * 0.6) * adsr(n, 0.3, 1.0, 0.6), 0.4, 1.5)


def vortex_burst():
    d = 1.6
    n = int(d * SR)
    return reverb(mix(boom(d, 140, 30, 0.4) * 1.5, splash(d, 2.5) * 1.2, sweep_bp(noise(d), 3000, 300, 1.6) * env(n, 0.01, 0.4) * 0.6), 0.4, 1.6)


def geyser_slam():
    d = 1.2
    n = int(d * SR)
    return reverb(mix(boom(d, 110, 30, 0.35) * 1.7, lp(noise(d), 2500) * env(n, 0.002, 0.2) * 0.9, splash(d, 1.4)), 0.35, 1.2)


def geyser_erupt():
    d = 1.0
    n = int(d * SR)
    jet = bp(noise(d), 700, 6000) * adsr(n, 0.02, 0.9, 0.6)
    return reverb(mix(jet * 1.1, boom(d, 90, 40, 0.1), bubbles(d, 14, 200, 800) * 0.5, spray(d, 0.1, 0.4) * 0.5), 0.3, 0.9)


def beam_charge():
    d = 1.0
    n = int(d * SR)
    t = T(d)
    whine = osc(glide(d, 180, 900)) * (0.6 + 0.4 * np.sin(2 * np.pi * 18 * t)) * np.linspace(0.2, 1, n) ** 2
    suck = sweep_bp(noise(d), 4000, 400, 1.6) * adsr(n, 0.2, 0.8, 0.1)
    return reverb(mix(whine * 0.45, suck, bubbles(d, 30, 300, 1200) * 0.5), 0.35, 1.0)


def beam_fire():
    d = 2.2
    n = int(d * SR)
    t = T(d)
    hiss = hp(noise(d), 1500) * 0.8
    roar = lp(noise(d), 500) * 1.2
    tone = sum(osc(f * (1 + 0.004 * np.sin(2 * np.pi * 7 * t))) for f in (220, 330, 440)) * 0.15
    e = adsr(n, 0.03, 0.95, 0.35)
    kick = boom(d, 160, 40, 0.2) * 1.3
    return reverb(mix((hiss + roar + tone) * e, kick, bubbles(d, 40, 300, 1200) * 0.3), 0.3, 1.0)


def tsunami_leap():
    d = 0.9
    n = int(d * SR)
    return reverb(mix(sweep_bp(noise(d), 250, 3500, 1.6) * np.sin(np.pi * T(d) / d) ** 1.4 * 1.2, splash(d, 1.0) * 0.8), 0.3, 1.0)


def tsunami_crash():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    impact = boom(d, 130, 25, 0.7) * 1.8
    wave = lp(brown(d) + noise(d) * 0.5, 2200) * adsr(n, 0.05, 0.9, 1.6)
    surf = bp(noise(d), 1500, 7000) * adsr(n, 0.2, 0.6, 1.8) * 0.5
    return reverb(mix(impact, wave * 1.2, surf, bubbles(d, 60, 120, 600) * 0.5), 0.5, 2.4)


def double_jump():
    d = 0.6
    n = int(d * SR)
    return mix(sweep_bp(noise(d), 400, 3500, 1.6) * env(n, 0.02, 0.2), boom(d, 120, 60, 0.07) * 0.7, spray(d, 0.01, 0.15) * 0.6, bubbles(d, 6, 300, 900) * 0.4)


if __name__ == '__main__':
    jobs = {}
    for v in (1, 2):
        jobs[f'aigoar_ambient{v}'] = (lambda v=v: aigoar_ambient(v))
        jobs[f'claw_swipe{v}'] = (lambda v=v: claw_swipe(v))
    for v in (1, 2, 3):
        jobs[f'aigoar_hurt{v}'] = (lambda v=v: aigoar_hurt(v))
        jobs[f'aigoar_step{v}'] = (lambda v=v: aigoar_step(v))
    for f in (aigoar_death, aigoar_land, transform, revert, tidal_rend, maelstrom, vortex_loop, vortex_burst, geyser_slam, geyser_erupt,
              beam_charge, beam_fire, tsunami_leap, tsunami_crash, double_jump):
        jobs[f.__name__] = f
    quiet = {k: -7 for k in jobs if 'step' in k}
    quiet.update({k: -4 for k in jobs if 'ambient' in k})
    for name, fn in jobs.items():
        save(name, fn(), quiet.get(name, -1.0))
    print(len(jobs), 'sounds')
