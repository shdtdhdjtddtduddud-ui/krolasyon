"""Sound design for İntikam (wet / blood) and Kalp Kırıcı İblis (demonic / thorns / hearts)."""
from sounds import *


def bubbles(d, n=12, lo=150, hi=450):
    out = np.zeros(int(d * SR))
    for _ in range(n):
        s = int(R.random() * (len(out) - 3000))
        L = 0.03 + R.random() * 0.05
        b = osc(glide(L, lo + R.random() * (hi - lo), (lo + R.random() * (hi - lo)) * 1.8)) * env(int(L * SR), 0.002, L * 0.5)
        b = b[:len(out) - s]
        out[s:s + len(b)] += b * (0.4 + R.random() * 0.6)
    return out


def heartbeat(d, bpm=70, accel=1.0):
    out = np.zeros(int(d * SR))
    t = 0.0
    period = 60 / bpm
    while t < d - 0.3:
        for off, g in ((0, 1.0), (0.18 * period, 0.7)):
            s = int((t + off) * SR)
            th = boom(0.25, 70, 35, 0.06) * g
            out[s:s + len(th)] += th[:max(0, len(out) - s)]
        t += period
        period /= accel
    return out


def breath(d, v):
    n = int(d * SR)
    t = T(d)
    e = np.abs(np.sin(np.pi * t / (d / 2))) ** 1.5
    x = bp(noise(d), 250, 1100) * e
    return x


def revenge_ambient(v):
    d = 2.4
    n = int(d * SR)
    g = growl(d, 62 if v == 1 else 70, 55, 'o', 16) * 0.6
    return reverb(mix(breath(d, v) * 0.8, bubbles(d, 14) * 0.35, np.tanh(g) * adsr(n, 0.3, 0.9, 0.6)), 0.3, 1.0)


def revenge_hurt(v):
    d = 0.6
    n = int(d * SR)
    sq = bp(noise(d), 300, 1600) * env(n, 0.002, 0.06)
    dn = osc(glide(d, [320, 280, 360][v - 1], 90)) * env(n, 0.005, 0.12)
    sn = growl(d, [140, 120, 160][v - 1], 90, 'a', 30) * env(n, 0.01, 0.15)
    return reverb(mix(sq, dn * 0.6, np.tanh(sn * 1.5) * 0.8, bubbles(d, 5) * 0.3), 0.2, 0.6)


def revenge_death():
    d = 3.4
    n = int(d * SR)
    t = T(d)
    f = np.interp(t, [0, 0.3, 2.6, 3.4], [180, 260, 70, 50])
    wail = formant(osc(f, 'saw') * rattle(n, 14, 0.3), 'a') * adsr(n, 0.1, 0.9, 1.0)
    splash = np.concatenate([np.zeros(int(1.3 * SR)), lp(noise(1.5), 2500) * env(int(1.5 * SR), 0.005, 0.4)])
    return reverb(mix(np.tanh(wail * 1.3), pad(splash, d)[:n] * 0.7, bubbles(d, 30) * 0.4, np.concatenate([np.zeros(int(1.3 * SR)), boom(1.5, 90, 30, 0.4)])), 0.45, 2.0)


def revenge_step(v):
    d = 0.4
    n = int(d * SR)
    return mix(boom(d, [72, 66, 78][v - 1], 40, 0.07) * 1.2, hp(noise(d), 900) * env(n, 0.002, 0.03) * 0.4, bubbles(d, 3, 250, 600) * 0.3)


def spear_thrust():
    d = 0.5
    n = int(d * SR)
    return mix(sweep_bp(noise(d), 600, 3800, 1.8) * np.sin(np.pi * T(d) / d) ** 2, bp(noise(d), 400, 2000) * np.concatenate([np.zeros(int(0.22 * SR)), env(n - int(0.22 * SR), 0.002, 0.05)]) * 0.8)


def lance_throw():
    d = 0.9
    n = int(d * SR)
    gr = np.tanh(growl(0.4, 150, 100, 'a', 30) * 2) * env(int(0.4 * SR), 0.01, 0.12)
    wh = sweep_bp(noise(d), 250, 4000, 1.8) * np.sin(np.pi * T(d) / d) ** 1.5
    return mix(gr * 0.8, wh * 1.1)


def lance_impact():
    d = 1.1
    n = int(d * SR)
    return reverb(mix(boom(d, 130, 40, 0.2) * 1.4, bp(noise(d), 500, 3000) * env(n, 0.002, 0.12), bubbles(d, 18) * 0.5,
                      hp(noise(d), 3000) * env(n, 0.001, 0.02) * 0.6), 0.3, 1.0)


def blood_wave():
    d = 1.3
    n = int(d * SR)
    surge = lp(brown(d), 700) * adsr(n, 0.08, 0.9, 0.6)
    sl = sweep_bp(noise(d), 300, 1500, 1.6) * adsr(n, 0.05, 0.7, 0.6) * 0.6
    return reverb(mix(surge * 1.2, sl, bubbles(d, 24) * 0.5, boom(d, 80, 35, 0.15) * 0.8), 0.3, 1.0)


def blade_slash():
    d = 0.4
    n = int(d * SR)
    wh = sweep_bp(noise(d), 900, 6000, 1.8) * np.sin(np.pi * T(d) / d) ** 3
    ring = bell(d, 2800, decay=0.12) * 0.15
    return mix(wh * 1.2, ring, bp(noise(d), 300, 1500) * env(n, 0.15, 0.04) * 0.4)


def drain():
    d = 2.0
    n = int(d * SR)
    t = T(d)
    suck = sweep_bp(noise(d), 3000, 250, 1.6) * adsr(n, 0.3, 0.8, 0.5)
    tone = osc(glide(d, 440, 110)) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t)) * 0.3
    return reverb(mix(suck, tone * adsr(n, 0.2, 0.8, 0.5), heartbeat(d, 80) * 0.9), 0.4, 1.5)


def vengeance_charge():
    d = 2.2
    n = int(d * SR)
    t = T(d)
    drone = sum(osc(glide(d, f, f * 1.8), 'saw') for f in (55, 82.5, 110))
    drone = lp(drone, 900) * np.linspace(0.2, 1, n) ** 2
    return reverb(mix(drone * 0.5, heartbeat(d, 60, 1.25) * 1.2, sweep_bp(noise(d), 200, 2500, 1.5) * np.linspace(0, 0.5, n)), 0.35, 1.5)


def vengeance_burst():
    d = 2.2
    n = int(d * SR)
    scream = formant(osc(glide(d, 300, 120), 'saw') * rattle(n, 30, 0.5), 'a') * env(n, 0.02, 0.5)
    return reverb(mix(boom(d, 140, 28, 0.7) * 1.6, lp(noise(d), 3000) * env(n, 0.002, 0.35), np.tanh(scream * 2) * 0.6, bubbles(d, 30) * 0.4), 0.45, 2.0)


def revenge_phase():
    d = 2.8
    n = int(d * SR)
    roar = np.tanh(sum(growl(d, f, f * 0.85, 'a', 20 + i * 4) for i, f in enumerate((58, 87, 116))) * 1.8)
    return reverb(mix(roar * adsr(n, 0.12, 0.95, 1.0), heartbeat(d, 90) * 0.9, boom(d, 90, 28, 0.5)), 0.45, 2.0)


# -------- demon --------
def laugh(d, f0, pulses, vowel='a'):
    n = int(d * SR)
    out = np.zeros(n)
    step = d / (pulses + 1)
    for k in range(pulses):
        L = step * 0.75
        m = int(L * SR)
        f = glide(L, f0 * (1.1 - k * 0.04), f0 * (0.9 - k * 0.04))
        v = formant(osc(f, 'saw') + 0.3 * noise(L), vowel) * env(m, 0.01, L * 0.35)
        s = int(k * step * SR)
        out[s:s + m] += v[:max(0, n - s)]
    return out


def demon_ambient(v):
    d = 2.2
    n = int(d * SR)
    lg = laugh(d, 105 if v == 1 else 95, 4 if v == 1 else 5)
    lo = laugh(d, 52 if v == 1 else 47, 4 if v == 1 else 5, 'o') * 0.7
    return reverb(np.tanh(mix(lg, lo) * 2.2) * adsr(n, 0.05, 0.9, 0.4), 0.45, 1.6)


def demon_hurt(v):
    d = 0.5
    n = int(d * SR)
    g = formant(osc(glide(d, [180, 160, 200][v - 1], 110), 'saw'), 'a') * env(n, 0.01, 0.12)
    return reverb(mix(np.tanh(g * 2), hp(noise(d), 2500) * env(n, 0.01, 0.1) * 0.4), 0.25, 0.7)


def demon_death():
    d = 3.2
    n = int(d * SR)
    t = T(d)
    f = np.interp(t, [0, 0.3, 2.4, 3.2], [220, 320, 90, 60])
    scr = np.tanh(formant(osc(f, 'saw') * rattle(n, 24, 0.4), 'a') * 2) * adsr(n, 0.05, 0.9, 1.0)
    shatter = np.zeros(n)
    for _ in range(40):
        s = int((1.0 + R.random() * 1.2) * SR)
        b = bell(0.5, 1200 + R.random() * 2500, decay=0.1 + R.random() * 0.15)
        shatter[s:s + len(b)] += b[:n - s] * R.random()
    return reverb(mix(scr * 0.9, shatter * 0.3, heartbeat(1.2, 70) * 0.8, np.concatenate([np.zeros(int(1.0 * SR)), boom(2.0, 100, 30, 0.5)])), 0.45, 2.0)


def demon_step(v):
    d = 0.35
    n = int(d * SR)
    th = boom(d, [90, 84, 96][v - 1], 45, 0.05)
    jingle = mix(*[bell(0.3, f, decay=0.05) for f in (3200 + v * 200, 4100)]) * 0.12
    return mix(th * 1.2, lp(noise(d), 1500) * env(n, 0.001, 0.03) * 0.4, pad(np.concatenate([np.zeros(int(0.03 * SR)), jingle]), d)[:n])


def thorn_whip():
    d = 0.7
    n = int(d * SR)
    wh = sweep_bp(noise(d), 300, 3000, 1.8) * np.clip(T(d) / 0.25, 0, 1) ** 2 * (T(d) < 0.3)
    crack = hp(noise(0.05), 2000) * env(int(0.05 * SR), 0.0003, 0.008)
    crack = np.concatenate([np.zeros(int(0.28 * SR)), crack * 3])
    return reverb(mix(wh, crack, crackle(d, 40) * np.concatenate([np.zeros(int(0.3 * SR)), np.linspace(1, 0, n - int(0.3 * SR))]) * 0.5), 0.3, 0.8)


def heart_shoot():
    d = 0.8
    n = int(d * SR)
    t = T(d)
    whum = osc(glide(d, 180, 520)) * env(n, 0.02, 0.25) * (0.6 + 0.4 * np.sin(2 * np.pi * 14 * t))
    sparkle = mix(*[np.concatenate([np.zeros(int(k * 0.05 * SR)), bell(0.4, f, decay=0.1)]) for k, f in enumerate((1568, 1976, 2349))])
    return reverb(mix(whum, heartbeat(0.6, 100)[:n] * 0.9, pad(sparkle, d)[:n] * 0.15), 0.35, 1.0)


def heart_hit():
    d = 0.5
    n = int(d * SR)
    return reverb(mix(boom(d, 220, 90, 0.05), bell(d, 1760, decay=0.1) * 0.3, bp(noise(d), 800, 5000) * env(n, 0.001, 0.04) * 0.6), 0.25, 0.6)


def shadow_dash():
    d = 0.9
    n = int(d * SR)
    return reverb(mix(sweep_bp(noise(d), 200, 1800, 1.8) * np.sin(np.pi * T(d) / d) ** 1.2 * 1.2, boom(d, 70, 35, 0.25) * 0.9,
                      formant(osc(glide(d, 80, 60), 'saw'), 'o') * env(n, 0.05, 0.3) * 0.5), 0.35, 1.0)


def thorn_erupt():
    d = 0.9
    n = int(d * SR)
    cr = mix(*[np.concatenate([np.zeros(int(R.random() * 0.25 * SR)), bp(noise(0.06), 600, 4000) * env(int(0.06 * SR), 0.0005, 0.015)]) for _ in range(10)])
    return reverb(mix(pad(cr, d)[:n] * 1.1, boom(d, 110, 50, 0.1), lp(noise(d), 900) * env(n, 0.005, 0.15) * 0.5), 0.25, 0.8)


def scythe_spin():
    d = 1.4
    n = int(d * SR)
    t = T(d)
    rate = np.interp(t, [0, d], [5, 9])
    mod = 0.5 + 0.5 * np.sin(np.cumsum(2 * np.pi * rate / SR))
    wh = sweep_bp(noise(d), 500, 2500, 1.6) * mod ** 3
    return reverb(mix(wh * adsr(n, 0.1, 0.9, 0.3) * 1.2, bell(d, 2200, decay=0.3) * 0.08), 0.3, 0.9)


def demon_phase():
    d = 3.2
    n = int(d * SR)
    lg = np.tanh(mix(laugh(d, 95, 7), laugh(d, 47, 7, 'o') * 0.8, laugh(d, 190, 7, 'e') * 0.3) * 2.2)
    return reverb(mix(lg * adsr(n, 0.05, 0.95, 0.6), boom(d, 100, 28, 0.6) * 0.9), 0.5, 2.2)


if __name__ == '__main__':
    jobs = {}
    for v in (1, 2):
        jobs[f'revenge_ambient{v}'] = (lambda v=v: revenge_ambient(v))
        jobs[f'demon_ambient{v}'] = (lambda v=v: demon_ambient(v))
    for v in (1, 2, 3):
        jobs[f'revenge_hurt{v}'] = (lambda v=v: revenge_hurt(v))
        jobs[f'revenge_step{v}'] = (lambda v=v: revenge_step(v))
        jobs[f'demon_hurt{v}'] = (lambda v=v: demon_hurt(v))
        jobs[f'demon_step{v}'] = (lambda v=v: demon_step(v))
    for f in (revenge_death, spear_thrust, lance_throw, lance_impact, blood_wave, blade_slash, drain, vengeance_charge, vengeance_burst,
              revenge_phase, demon_death, thorn_whip, heart_shoot, heart_hit, shadow_dash, thorn_erupt, scythe_spin, demon_phase):
        jobs[f.__name__] = f
    quiet = {k: -6 for k in jobs if 'step' in k}
    quiet.update({k: -3 for k in jobs if 'ambient' in k})
    for name, fn in jobs.items():
        save(name, fn(), quiet.get(name, -1.0))
    print(len(jobs), 'sounds')
