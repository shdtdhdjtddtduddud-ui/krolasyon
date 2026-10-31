"""Procedural sound design for both bosses -> mono .ogg files."""
import os, subprocess, json, numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/cille/sounds')
os.makedirs(OUT, exist_ok=True)
R = np.random.default_rng(7)


def T(d):
    return np.arange(int(d * SR)) / SR


def noise(d):
    return R.standard_normal(int(d * SR))


def brown(d):
    x = np.cumsum(R.standard_normal(int(d * SR)))
    x -= np.convolve(x, np.ones(2000) / 2000, 'same')
    return x / (np.abs(x).max() + 1e-9)


def bp(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], 'bandpass', fs=SR, output='sos'), x)


def lp(x, f, order=2):
    return sosfilt(butter(order, f, 'lowpass', fs=SR, output='sos'), x)


def hp(x, f, order=2):
    return sosfilt(butter(order, f, 'highpass', fs=SR, output='sos'), x)


def sweep_bp(x, f0, f1, q=1.5, blocks=64):
    """time-varying bandpass by block processing (crossfaded)"""
    n = len(x)
    out = np.zeros(n)
    edges = np.linspace(0, n, blocks + 1).astype(int)
    for i in range(blocks):
        f = f0 * (f1 / f0) ** (i / (blocks - 1))
        a, b = edges[i], edges[i + 1]
        seg = x[max(0, a - 512):b]
        y = bp(seg, max(20, f / q), min(SR / 2 - 100, f * q))
        out[a:b] = y[-(b - a):]
    return out


def env(n, a, d, shape=2.0):
    t = np.arange(n) / SR
    e = np.minimum(1, t / max(a, 1e-4)) * np.exp(-np.maximum(0, t - a) / max(d, 1e-4) * shape)
    return e


def adsr(n, a, s_level, r):
    t = np.arange(n) / SR
    dur = n / SR
    e = np.minimum(1, t / a)
    e = np.where(t > dur - r, e * np.clip((dur - t) / r, 0, 1), e)
    return e * s_level + (1 - s_level) * e * np.exp(-t * 3)


def osc(freq, kind='sine'):
    f = np.asarray(freq, float)
    ph = np.cumsum(2 * np.pi * f / SR)
    if kind == 'sine':
        return np.sin(ph)
    if kind == 'saw':
        return 2 * ((ph / (2 * np.pi)) % 1) - 1
    if kind == 'pulse':
        return np.sign(np.sin(ph)) * 0.8
    if kind == 'tri':
        return 2 * np.abs(2 * ((ph / (2 * np.pi)) % 1) - 1) - 1


def glide(d, f0, f1, curve='exp'):
    t = np.linspace(0, 1, int(d * SR))
    return f0 * (f1 / f0) ** t if curve == 'exp' else f0 + (f1 - f0) * t


def formant(x, vowel='a'):
    F = {'a': [(800, 1.0), (1150, 0.5), (2900, 0.25)], 'o': [(450, 1.0), (800, 0.45), (2830, 0.15)],
         'u': [(325, 1.0), (700, 0.3), (2530, 0.1)], 'e': [(400, 1.0), (1600, 0.5), (2700, 0.25)]}[vowel]
    return sum(g * bp(x, f / 1.25, f * 1.25) for f, g in F)


def bell(d, f, partials=(1, 2.76, 5.40, 8.93), decay=0.6):
    t = T(d)
    return sum(np.sin(2 * np.pi * f * p * t + R.random() * 6) * np.exp(-t / (decay / (1 + i * 0.7))) / (1 + i) for i, p in enumerate(partials))


def boom(d, f0=110, f1=35, decay=0.5):
    n = int(d * SR)
    return osc(glide(d, f0, f1)) * env(n, 0.005, decay, 1.0)


def crackle(d, density=60):
    n = int(d * SR)
    x = np.zeros(n)
    k = int(density * d)
    for p in R.integers(0, max(1, n - 400), k):
        L = int(R.integers(40, 300))
        x[p:p + L] += R.standard_normal(L) * np.exp(-np.arange(L) / (L / 4)) * R.random()
    return hp(x, 1500)


def reverb(x, amount=0.3, length=1.2):
    n = int(length * SR)
    ir = R.standard_normal(n) * np.exp(-np.arange(n) / SR * 5 / length)
    ir = lp(ir, 5000)
    wet = np.convolve(x, ir)[:len(x) + n // 2]
    wet /= np.abs(wet).max() + 1e-9
    y = np.concatenate([x, np.zeros(n // 2)])
    return y * (1 - amount) + wet * amount * np.abs(x).max()


def rattle(n, rate=28, depth=0.7):
    t = np.arange(n) / SR
    jitter = np.cumsum(R.standard_normal(n)) / SR * 40
    return 1 - depth + depth * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * rate * t + jitter)))


def fade(x, fin=0.005, fout=0.05):
    n = len(x)
    a, b = int(fin * SR), int(fout * SR)
    x = x.copy()
    if a: x[:a] *= np.linspace(0, 1, a)
    if b: x[-b:] *= np.linspace(1, 0, b)
    return x


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def pad(x, d):
    n = int(d * SR)
    return np.concatenate([x, np.zeros(max(0, n - len(x)))])[:max(n, len(x))]


def save(name, x, gain_db=-3.0):
    x = lp(np.nan_to_num(x), 5200, 2)
    x = hp(x, 25, 2)
    x = fade(x)
    x = x / (np.abs(x).max() + 1e-9) * 10 ** (gain_db / 20)
    wav = os.path.join(os.environ.get('TMPDIR', '/tmp'), f'{name}.wav')
    import wave
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', '-ac', '1',
                    os.path.join(OUT, f'{name}.ogg')], check=True)



# ======================= KABKABA (dark lion beast) — low, natural, never shrill =======================
DARK_LP = 3800


def dark_reverb(x, amount=0.35, length=1.8):
    n = int(length * SR)
    ir = R.standard_normal(n) * np.exp(-np.arange(n) / SR * 4.5 / length)
    ir = lp(ir, 2200, 3)
    wet = np.convolve(x, ir)[:len(x) + n // 2]
    wet /= np.abs(wet).max() + 1e-9
    y = np.concatenate([x, np.zeros(n // 2)])
    return y * (1 - amount) + wet * amount * np.abs(x).max()


def glottal(d, f0, f1, jitter=0.03, sub=0.35):
    """pulse train like real vocal folds: irregular pitch, soft pulses, period-doubling sub-harmonic (rough roar)"""
    n = int(d * SR)
    f = glide(d, f0, f1)
    f = f * (1 + jitter * lp(R.standard_normal(n), 30) * 8)
    ph = np.cumsum(f / SR)
    frac = ph % 1.0
    pulse = np.exp(-frac * 9.0) * (1 - np.exp(-frac * 60))          # asymmetric glottal flow derivative-ish
    pulse = pulse - pulse.mean()
    half = np.floor(ph) % 2 == 0
    pulse = pulse * np.where(half, 1.0, 1.0 - sub)                   # period doubling
    shimmer = 1 + 0.25 * lp(R.standard_normal(n), 40) * 6
    return pulse * shimmer


def tract(x, formants):
    """vocal tract: parallel resonances (big animal = low formants)"""
    return sum(g * bp(x, f / 1.18, f * 1.18) for f, g in formants)


LION = [(260, 1.0), (520, 0.8), (980, 0.45), (1700, 0.18), (2600, 0.06)]
LION_U = [(220, 1.0), (430, 0.6), (850, 0.25), (1500, 0.08)]


def beast(d, f0, f1, formants=LION, breath=0.35, rough=0.35, jitter=0.03, fry=0.0, fry_rate=22):
    n = int(d * SR)
    src = glottal(d, f0, f1, jitter, rough)
    turb = lp(noise(d), 2500) * (0.4 + 0.6 * np.abs(src) / (np.abs(src).max() + 1e-9))
    v = tract(src + turb * breath, formants)
    if fry > 0:
        v = v * (1 - fry + fry * rattle(n, fry_rate, 0.9))
    chest = lp(src, 180) * 0.8                                       # chest resonance / sub
    return lp(v + chest, DARK_LP, 3)


def roar(d=2.4, f0=82, f1=52):
    n = int(d * SR)
    e = np.minimum(1, T(d) / 0.28) ** 1.5 * np.exp(-np.maximum(0, T(d) - 1.2) * 1.6)
    x = beast(d, f0, f1, LION, breath=0.5, rough=0.45, jitter=0.04) * e
    x += lp(brown(d), 120) * e * 0.6
    tail = beast(0.9, 55, 40, LION_U, breath=0.8, rough=0.6, fry=0.6) * env(int(0.9 * SR), 0.05, 0.5) * 0.4
    return dark_reverb(mix(x, pad_front(tail, 1.5)), 0.32, 2.2)


def pad_front(x, d):
    return np.concatenate([np.zeros(int(d * SR)), x])


def ambient(v):
    d = 2.2 if v == 1 else 2.8
    n = int(d * SR)
    x = beast(d, 46 if v == 1 else 42, 40 if v == 1 else 48, LION_U, breath=0.5, rough=0.5, fry=0.75, fry_rate=19 + v * 3)
    # exhale through the nose at the end
    ex = lp(bp(noise(d), 150, 900), 1200) * np.clip((T(d) - d * 0.6) / (d * 0.4), 0, 1) * 0.35
    return dark_reverb((x + ex) * adsr(n, 0.35, 0.85, 0.7), 0.22)


def hurt(v):
    d = 0.75
    n = int(d * SR)
    x = beast(d, 105 + v * 10, 70, LION, breath=0.6, rough=0.55, jitter=0.06)
    return dark_reverb(x * env(n, 0.015, 0.35, 1.3), 0.2, 1.2)


def death():
    d = 3.6
    n = int(d * SR)
    x = beast(d, 90, 30, LION, breath=0.6, rough=0.6, jitter=0.05) * adsr(n, 0.1, 0.9, 2.0)
    sigh = lp(bp(noise(1.6), 120, 700), 900) * env(int(1.6 * SR), 0.4, 0.8) * 0.5
    thud = boom(1.0, 70, 30, 0.35)
    return dark_reverb(mix(x, pad_front(sigh, 2.2), pad_front(thud, 1.4)), 0.38, 2.4)


def step(v):
    d = 0.4
    n = int(d * SR)
    thud = boom(d, 62 + v * 6, 32, 0.14) * 1.2
    dirt = lp(noise(d), 700 + v * 80, 3) * env(n, 0.003, 0.05)
    crunch = lp(crackle(d, 40), 1800) * env(n, 0.005, 0.06) * 0.25
    return thud + dirt * 0.6 + crunch


def whoosh(d, f0, f1, q=1.4):
    n = int(d * SR)
    t = T(d)
    e = np.sin(np.pi * np.clip(t / d, 0, 1)) ** 2
    return lp(sweep_bp(noise(d), f0, f1, q), 2600, 3) * e


def swipe():
    d = 0.55
    n = int(d * SR)
    w = whoosh(d, 250, 900) + whoosh(d, 900, 300) * 0.5
    g = beast(d, 95, 70, LION, breath=0.6, rough=0.5) * env(n, 0.02, 0.25) * 0.6
    return dark_reverb(w + g, 0.15, 0.8)


def black_hole_open():
    d = 3.2
    n = int(d * SR)
    t = T(d)
    rumble = lp(brown(d), 90, 3) * 1.4
    drone = lp(osc(glide(d, 32, 48), 'saw'), 160, 3) * (0.7 + 0.3 * np.sin(2 * np.pi * glide(d, 1.5, 5) * t))
    suck = lp(sweep_bp(noise(d), 1400, 120, 2.0), 2000) * 0.7
    return dark_reverb((rumble + drone + suck) * adsr(n, 0.7, 0.9, 0.9), 0.4, 2.6)


def black_hole_loop():
    d = 2.0
    n = int(d * SR)
    t = T(d)
    drone = lp(osc(38 + 3 * np.sin(2 * np.pi * 0.5 * t), 'saw'), 140, 3) * (0.75 + 0.25 * np.sin(2 * np.pi * 3 * t))
    wind = lp(bp(noise(d), 90, 600), 800) * (0.5 + 0.5 * np.sin(2 * np.pi * 1.0 * t + 1))
    return (drone + wind * 0.7 + lp(brown(d), 80) * 0.8) * adsr(n, 0.25, 1.0, 0.35)


def big_boom(d, f0=70, f1=20):
    n = int(d * SR)
    b = boom(d, f0, f1, 0.9) * 1.3
    body = lp(noise(d), 500, 3) * env(n, 0.004, 0.45)
    debris = lp(crackle(d, 35), 1600) * env(n, 0.05, 0.9) * 0.4
    return b + body + debris


def black_hole_collapse():
    implode = lp(sweep_bp(noise(0.7), 150, 1300, 2.0), 1800) * env(int(0.7 * SR), 0.6, 0.05)
    return dark_reverb(np.concatenate([implode, big_boom(2.4, 75, 18)]), 0.45, 2.8)


def blink():
    d = 0.9
    n = int(d * SR)
    w = whoosh(d, 900, 140, 1.6)
    thump = boom(d, 80, 35, 0.15) * 0.6
    return dark_reverb(w + pad_front(thump, 0.35)[:len(w)], 0.35, 1.4)


def drain():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    hum = lp(osc(glide(d, 70, 52) * (1 + 0.02 * np.sin(2 * np.pi * 5 * t)), 'saw'), 300, 3)
    inhale = lp(sweep_bp(noise(d), 300, 1100, 1.8), 1600) * (0.55 + 0.45 * np.sin(2 * np.pi * 2.2 * t))
    return dark_reverb((hum * 0.8 + inhale * 0.7) * adsr(n, 0.3, 0.9, 0.5), 0.35, 1.8)


def reflect():
    d = 1.3
    n = int(d * SR)
    t = T(d)
    whomp = osc(glide(d, 60, 110) + 25 * np.sin(2 * np.pi * 7 * t)) * env(n, 0.03, 0.5)
    shell = lp(bp(noise(d), 200, 1200), 1500) * env(n, 0.08, 0.5) * 0.5
    return dark_reverb(whomp + shell, 0.4, 1.6)


def reflect_hit():
    d = 0.7
    n = int(d * SR)
    t = T(d)
    metal = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * k) * a for f, k, a in ((170, 7, 1.0), (395, 10, 0.5), (660, 14, 0.25)))
    hitn = lp(noise(d), 1800) * env(n, 0.001, 0.03)
    return dark_reverb(lp(metal + hitn + boom(d, 90, 45, 0.1), 3000), 0.3, 1.0)


def shadow_rise():
    d = 1.0
    n = int(d * SR)
    crack = lp(crackle(d, 60), 2200) * env(n, 0.005, 0.25)
    rumble = lp(brown(d), 160, 3) * env(n, 0.01, 0.45) * 1.3
    rush = whoosh(0.6, 200, 700) * 0.6
    return dark_reverb(mix(crack * 0.7, rumble, rush), 0.3, 1.4)


def slam():
    return dark_reverb(big_boom(2.2, 85, 22), 0.4, 2.2)


def guard():
    d = 1.5
    n = int(d * SR)
    g = beast(d, 58, 50, LION_U, breath=0.4, rough=0.5, fry=0.5) * adsr(n, 0.08, 0.8, 0.5)
    hum = lp(osc(glide(d, 55, 75), 'saw'), 250, 3) * env(n, 0.2, 0.7) * 0.5
    return dark_reverb(g + hum, 0.35, 1.6)


def marble_throw():
    return whoosh(0.35, 500, 1100) * 0.8


def marble_clack(v):
    d = 0.3
    n = int(d * SR)
    t = T(d)
    f = 900 + v * 120
    knock = sum(np.sin(2 * np.pi * f * p * t) * np.exp(-t * (55 + 25 * i)) / (1 + i) for i, p in enumerate((1, 2.1, 3.3)))
    body = boom(d, 160, 90, 0.03) * 0.6
    return lp(knock * 0.7 + body, 3000, 3)


def marble_burst():
    d = 2.4
    n = int(d * SR)
    shards = lp(crackle(d, 70), 2800, 3) * env(n, 0.003, 0.25) * 0.5
    whoo = whoosh(1.2, 200, 900) * 0.8
    return dark_reverb(mix(big_boom(d, 70, 25), shards, whoo), 0.45, 2.4)


# ======================= BEHMUD (cosmic flying whale) — deep moans, water and ice =======================
def whale_moan(d, f0, f1, f2=None, vib=0.03):
    n = int(d * SR)
    t = T(d)
    if f2 is None:
        f = glide(d, f0, f1)
    else:
        h = n // 2
        a = f0 * (f1 / f0) ** np.linspace(0, 1, h)
        b = f1 * (f2 / f1) ** np.linspace(0, 1, n - h)
        f = np.concatenate([a, b])
    f = f * (1 + vib * np.sin(2 * np.pi * 4.5 * t))
    src = osc(f, 'saw') * 0.5 + osc(f) * 0.8 + osc(f * 2) * 0.25
    v = tract(src, [(300, 1.0), (650, 0.5), (1200, 0.2)]) + lp(src, 250) * 0.6
    return lp(v, 2400, 3)


def whale_ambient(v):
    d = 3.4 if v == 1 else 2.8
    n = int(d * SR)
    x = whale_moan(d, 95, 160, 110) if v == 1 else whale_moan(d, 140, 85, 120)
    return dark_reverb(x * adsr(n, 0.6, 0.85, 1.0), 0.5, 3.0)


def whale_hurt(v):
    d = 0.9
    n = int(d * SR)
    return dark_reverb(whale_moan(d, 180 + v * 15, 110, vib=0.08) * env(n, 0.02, 0.4, 1.2), 0.35, 1.6)


def whale_death():
    d = 4.0
    n = int(d * SR)
    return dark_reverb(whale_moan(d, 150, 60, 35, vib=0.05) * adsr(n, 0.2, 0.9, 2.2), 0.55, 3.2)


def bubbles(d, rate=30, lo=180, hi=520):
    n = int(d * SR)
    x = np.zeros(n)
    for p in R.integers(0, max(1, n - 3000), int(rate * d)):
        L = int(R.integers(800, 2600))
        tt = np.arange(L) / SR
        f = R.uniform(lo, hi) * (1 + tt * 8)
        x[p:p + L] += np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 40) * R.uniform(0.3, 1.0)
    return x


def water_rush(d, lo=150, hi=1400):
    n = int(d * SR)
    t = T(d)
    rush = lp(bp(noise(d), lo, hi), 1800) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.8 * t))
    return rush + lp(brown(d), 120) * 0.8 + bubbles(d, 20) * 0.25


def ice_beam():
    d = 2.6
    n = int(d * SR)
    t = T(d)
    hum = lp(osc(glide(d, 70, 60) + 6 * np.sin(2 * np.pi * 3 * t), 'saw'), 350, 3)
    hiss = lp(bp(noise(d), 600, 2400), 2600) * 0.6
    crack = lp(crackle(d, 50), 2400) * 0.5
    return dark_reverb((hum + hiss + crack) * adsr(n, 0.15, 0.9, 0.4), 0.35, 1.8)


def ice_form():
    d = 1.4
    n = int(d * SR)
    crack = lp(crackle(d, 80), 2600) * env(n, 0.01, 0.6)
    groan = lp(osc(glide(d, 90, 60), 'saw'), 300) * env(n, 0.05, 0.6) * 0.5
    return dark_reverb(crack + groan + boom(d, 70, 40, 0.2) * 0.5, 0.4, 1.6)


def ice_hit():
    d = 0.6
    n = int(d * SR)
    return dark_reverb(lp(crackle(d, 90), 2600) * env(n, 0.002, 0.12) + boom(d, 110, 60, 0.08), 0.3, 1.0)


def ice_break():
    d = 1.6
    n = int(d * SR)
    shards = lp(crackle(d, 160), 2800) * env(n, 0.002, 0.5)
    return dark_reverb(shards + boom(d, 80, 30, 0.3) * 0.9, 0.4, 1.8)


def whirlpool():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    swirl = lp(sweep_bp(noise(d), 200, 900, 1.6), 1500) * (0.6 + 0.4 * np.sin(2 * np.pi * 2.0 * t))
    return dark_reverb((water_rush(d) + swirl * 0.7) * adsr(n, 0.4, 0.95, 0.6), 0.4, 2.0)


def sonic_wave():
    d = 2.2
    n = int(d * SR)
    t = T(d)
    out = np.zeros(n)
    for k in range(3):
        st = int(k * 0.32 * SR)
        L = n - st
        tt = np.arange(L) / SR
        pulse = np.sin(2 * np.pi * np.cumsum(55 * (1 + 0.6 * np.exp(-tt * 6))) / SR) * np.exp(-tt * 3.5)
        out[st:] += pulse * (1.0 - k * 0.15)
    moan = whale_moan(d, 120, 90) * env(n, 0.05, 0.8) * 0.5
    return dark_reverb(out + moan + lp(noise(d), 400) * env(n, 0.005, 0.2) * 0.6, 0.45, 2.4)


def fish_launch():
    d = 0.5
    n = int(d * SR)
    bloop = osc(glide(d, 140, 380)) * env(n, 0.005, 0.12)
    return lp(bloop + whoosh(d, 300, 900) * 0.6 + bubbles(d, 30) * 0.3, 2400)


def water_boom():
    d = 2.0
    n = int(d * SR)
    splash = lp(noise(d), 1600, 3) * env(n, 0.004, 0.35)
    return dark_reverb(mix(big_boom(d, 75, 24), splash * 0.9, bubbles(d, 40) * 0.35), 0.4, 2.0)


def torpedo():
    d = 0.9
    n = int(d * SR)
    return lp(whoosh(d, 200, 1200, 1.2) * 1.2 + bubbles(d, 50) * 0.3 + boom(d, 90, 50, 0.1) * 0.5, 2400)


def rush_dash():
    d = 0.8
    return lp(whoosh(d, 150, 700, 1.3) * 1.3 + lp(brown(d), 120) * env(int(d * SR), 0.1, 0.3), 2200)


def waterspout():
    d = 3.4
    n = int(d * SR)
    t = T(d)
    roar = water_rush(d, 100, 1100) * (0.6 + 0.4 * np.sin(2 * np.pi * 3.0 * t))
    wind = lp(sweep_bp(noise(d), 150, 700, 1.4), 1000) * 0.6
    return dark_reverb((roar + wind) * adsr(n, 0.5, 0.95, 0.8), 0.4, 2.2)


def clamp_bite():
    d = 0.9
    n = int(d * SR)
    crunch = lp(crackle(d, 120), 2200) * env(n, 0.002, 0.15)
    thump = boom(d, 120, 50, 0.12)
    groan = whale_moan(d, 110, 80) * env(n, 0.05, 0.35) * 0.5
    return dark_reverb(crunch + thump + groan, 0.3, 1.2)


def seal_release():
    d = 4.0
    n = int(d * SR)
    t = T(d)
    rise = lp(osc(glide(2.0, 30, 90), 'saw'), 300, 3) * np.linspace(0, 1, int(2.0 * SR)) ** 2
    rise = np.concatenate([rise, np.zeros(n - len(rise))])
    burst = np.concatenate([np.zeros(int(2.0 * SR)), big_boom(2.0, 70, 18)])[:n]
    choir = lp(sum(osc(f * (1 + 0.004 * np.sin(2 * np.pi * (0.3 + i * 0.1) * t))) for i, f in enumerate((55, 82.5, 110, 165))), 600) * env(n, 2.0, 1.4) * 0.5
    return dark_reverb(rise + burst * 1.2 + choir, 0.5, 3.0)


def space_power():
    d = 3.4
    n = int(d * SR)
    t = T(d)
    drone = lp(sum(osc(f * (1 + 0.01 * np.sin(2 * np.pi * (0.2 + i * 0.13) * t)), 'saw') for i, f in enumerate((41, 61.5, 82))), 500, 3)
    shimmer = lp(bp(noise(d), 500, 1800), 2000) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.7 * t)) * 0.3
    return dark_reverb((drone + shimmer) * adsr(n, 0.8, 0.9, 1.0), 0.55, 3.0)


# ======================= ŞARKAN (sand & rock warrior) — heavy, earthy, low =======================
def warrior_voice(d, f0, f1, vowel_set=None, rough=0.5, breath=0.5):
    return beast(d, f0, f1, vowel_set or [(400, 1.0), (800, 0.6), (1300, 0.25), (2400, 0.06)], breath=breath, rough=rough, jitter=0.03)


def sarkan_ambient(v):
    d = 1.6 if v == 1 else 2.0
    n = int(d * SR)
    x = warrior_voice(d, 70 if v == 1 else 64, 58, rough=0.6) * adsr(n, 0.15, 0.7, 0.6)
    ex = lp(bp(noise(d), 200, 1200), 1500) * np.clip((T(d) - d * 0.5) / (d * 0.5), 0, 1) * 0.3
    return dark_reverb(x + ex, 0.25, 1.4)


def sarkan_hurt(v):
    d = 0.6
    n = int(d * SR)
    return dark_reverb(warrior_voice(d, 120 + v * 12, 85, rough=0.6) * env(n, 0.01, 0.25, 1.2), 0.2, 1.0)


def sarkan_death():
    d = 3.0
    n = int(d * SR)
    x = warrior_voice(d, 110, 45, rough=0.7) * adsr(n, 0.05, 0.85, 1.6)
    thud = pad_front(big_boom(1.2, 70, 25), 1.6)
    return dark_reverb(mix(x, thud * 0.8), 0.35, 2.0)


def armour_step(v):
    d = 0.45
    n = int(d * SR)
    thud = boom(d, 58 + v * 6, 30, 0.13) * 1.3
    clank = lp(sum(np.sin(2 * np.pi * f * T(d)) * np.exp(-T(d) * k) for f, k in ((310 + v * 20, 30), (520, 45))), 1500) * 0.25
    grit = lp(crackle(d, 40), 1600) * env(n, 0.003, 0.06) * 0.4
    return thud + clank + grit


def punch(v):
    d = 0.5
    n = int(d * SR)
    hit = boom(d, 110 + v * 15, 45, 0.07) * 1.4
    slap = lp(noise(d), 1400, 3) * env(n, 0.001, 0.04)
    swish = whoosh(0.25, 300, 900) * 0.5
    return dark_reverb(mix(pad_front(swish, 0), pad_front(hit + slap, 0.12)), 0.15, 0.8)


def sand_rush(d, lo=200, hi=2200):
    n = int(d * SR)
    t = T(d)
    hiss = lp(bp(noise(d), lo, hi), 2600, 3) * (0.6 + 0.4 * np.sin(2 * np.pi * 1.3 * t))
    grains = lp(crackle(d, 220), 2600) * 0.35
    return hiss + grains + lp(brown(d), 120) * 0.7


def sand_vortex():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    swirl = lp(sweep_bp(noise(d), 300, 1200, 1.6), 1800) * (0.6 + 0.4 * np.sin(2 * np.pi * 2.2 * t))
    return dark_reverb((sand_rush(d) + swirl * 0.6) * adsr(n, 0.4, 0.95, 0.6), 0.35, 1.8)


def sandstorm():
    d = 3.5
    n = int(d * SR)
    t = T(d)
    wind = lp(sweep_bp(noise(d), 150, 600, 1.4), 900) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.6 * t))
    return dark_reverb((sand_rush(d, 150, 1800) * 0.8 + wind) * adsr(n, 0.6, 0.95, 0.8), 0.4, 2.2)


def sand_wave():
    d = 2.0
    n = int(d * SR)
    return dark_reverb(mix(sand_rush(d) * adsr(n, 0.1, 0.9, 0.5), big_boom(d, 70, 30) * 0.6), 0.35, 1.8)


def atlas_smash():
    d = 2.4
    return dark_reverb(mix(big_boom(d, 95, 20) * 1.3, lp(crackle(d, 90), 2000) * env(int(d * SR), 0.01, 0.8) * 0.5), 0.45, 2.4)


def rock_rip():
    d = 1.2
    n = int(d * SR)
    grind = lp(crackle(d, 140), 1800) * env(n, 0.05, 0.6)
    groan = lp(osc(glide(d, 60, 45), 'saw'), 250, 3) * env(n, 0.05, 0.6) * 0.6
    return dark_reverb(grind + groan + boom(d, 80, 40, 0.2) * 0.6, 0.3, 1.4)


def rock_impact():
    d = 1.8
    return dark_reverb(big_boom(d, 90, 28) + lp(crackle(d, 130), 2200) * env(int(d * SR), 0.003, 0.5) * 0.6, 0.35, 1.8)


def sand_shield():
    d = 1.4
    n = int(d * SR)
    return dark_reverb(sand_rush(d, 250, 1800) * env(n, 0.05, 0.6) + boom(d, 70, 45, 0.3) * 0.7, 0.3, 1.4)


def earth_rise():
    d = 1.6
    n = int(d * SR)
    rumble = lp(brown(d), 140, 3) * env(n, 0.05, 0.8) * 1.4
    grind = lp(crackle(d, 100), 1800) * env(n, 0.1, 0.7) * 0.5
    return dark_reverb(rumble + grind, 0.35, 1.8)


SOUNDS = {
    'kabkaba_ambient': [lambda: ambient(1), lambda: ambient(2)],
    'kabkaba_hurt': [lambda: hurt(0), lambda: hurt(1), lambda: hurt(2)],
    'kabkaba_death': [death],
    'kabkaba_step': [lambda: step(0), lambda: step(1), lambda: step(2)],
    'kabkaba_roar': [roar],
    'claw_swipe': [swipe],
    'black_hole_open': [black_hole_open],
    'black_hole_loop': [black_hole_loop],
    'black_hole_collapse': [black_hole_collapse],
    'blink': [blink],
    'drain': [drain],
    'reflect': [reflect],
    'reflect_hit': [reflect_hit],
    'shadow_rise': [shadow_rise],
    'slam': [slam],
    'guard': [guard],
    'marble_throw': [marble_throw],
    'marble_clack': [lambda: marble_clack(0), lambda: marble_clack(1)],
    'marble_burst': [marble_burst],
    'ice_hit': [ice_hit],
    'ice_break': [ice_break],
    'behmud_ambient': [lambda: whale_ambient(1), lambda: whale_ambient(2)],
    'behmud_hurt': [lambda: whale_hurt(0), lambda: whale_hurt(1)],
    'behmud_death': [whale_death],
    'ice_beam': [ice_beam],
    'ice_form': [ice_form],
    'whirlpool': [whirlpool],
    'sonic_wave': [sonic_wave],
    'fish_launch': [fish_launch],
    'water_boom': [water_boom],
    'torpedo': [torpedo],
    'rush_dash': [rush_dash],
    'waterspout': [waterspout],
    'clamp_bite': [clamp_bite],
    'seal_release': [seal_release],
    'space_power': [space_power],
    'sarkan_ambient': [lambda: sarkan_ambient(1), lambda: sarkan_ambient(2)],
    'sarkan_hurt': [lambda: sarkan_hurt(0), lambda: sarkan_hurt(1), lambda: sarkan_hurt(2)],
    'sarkan_death': [sarkan_death],
    'armour_step': [lambda: armour_step(0), lambda: armour_step(1), lambda: armour_step(2)],
    'punch': [lambda: punch(0), lambda: punch(1), lambda: punch(2)],
    'sand_vortex': [sand_vortex],
    'sandstorm': [sandstorm],
    'sand_wave': [sand_wave],
    'atlas_smash': [atlas_smash],
    'rock_rip': [rock_rip],
    'rock_impact': [rock_impact],
    'sand_shield': [sand_shield],
    'earth_rise': [earth_rise],
}

if __name__ == '__main__':
    import json
    js = {}
    for ev, gens in SOUNDS.items():
        files = []
        for i, g in enumerate(gens):
            fn = ev if len(gens) == 1 else f'{ev}{i + 1}'
            save(fn, g())
            files.append({'name': f'cille:{fn}', 'attenuation_distance': 48})
        js[ev] = {'subtitle': f'subtitles.cille.{ev}', 'sounds': files}
    with open(os.path.join(OUT, '..', 'sounds.json'), 'w') as f:
        json.dump(js, f, indent=1)
    print('sounds ok')
