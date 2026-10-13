"""Procedural sound design for Flame Libra -> mono .ogg files."""
import os, subprocess, json, numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/librabeyblade/sounds')
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


def save(name, x, gain_db=-1.0, do_fade=True):
    x = np.nan_to_num(x)
    if do_fade:
        x = fade(x)
    x = x / (np.abs(x).max() + 1e-9) * 10 ** (gain_db / 20)
    wav = f'/tmp/libra_{name}.wav'
    import wave
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', '-ac', '1',
                    os.path.join(OUT, f'{name}.ogg')], check=True)


# ======================= SEAL WARDEN (crystal / arcane / stone) =======================
def warden_ambient(v):
    d = 2.6
    n = int(d * SR)
    t = T(d)


# ======================= FLAME LIBRA =======================
def metal_ring(d, f, decay=0.35, inharm=(1, 2.32, 4.25, 6.63, 9.38)):
    t = T(d)
    return sum(np.sin(2 * np.pi * f * p * t + R.random() * 6) * np.exp(-t / (decay / (1 + i * 0.5))) / (1 + i * 0.6)
               for i, p in enumerate(inharm))


def whirr(d, f0, f1, blades=4):
    """spinning-top whine: buzzy harmonic stack + air noise, pitch glide f0->f1"""
    f = glide(d, f0, f1)
    tone = 0.6 * osc(f, 'saw') + 0.3 * osc(f * 2.01, 'sine') + 0.2 * osc(f * blades, 'tri')
    tone = lp(tone, 3500)
    air = sweep_bp(noise(d), f0 * 3, f1 * 3, 1.4) * 0.35
    return tone * 0.6 + air


def launch():
    d = 1.3
    n = int(d * SR)
    # ripcord zip: dense clicks accelerating
    t = T(0.42)
    rate = 40 + 600 * (t / 0.42) ** 1.5
    ph = np.cumsum(rate / SR)
    clicks = (np.diff(np.floor(ph), prepend=0) > 0).astype(float)
    clicks = np.convolve(clicks, np.exp(-np.arange(200) / 25) * R.standard_normal(200))
    clicks = hp(clicks, 1800) * 0.6
    zip_ = sweep_bp(noise(0.42), 900, 5000, 1.5) * np.linspace(0.3, 1, int(0.42 * SR)) * 0.6
    w = whirr(d - 0.3, 300, 900) * env(int((d - 0.3) * SR), 0.05, 0.6, 1.2)
    return mix(clicks, zip_, np.concatenate([np.zeros(int(0.3 * SR)), w]),
               np.concatenate([np.zeros(int(0.42 * SR)), metal_ring(0.6, 1900, 0.15) * 0.5]))


def land():
    d = 0.5
    return mix(metal_ring(d, 2400, 0.08) * 0.7, boom(d, 180, 60, 0.12) * 0.6, hp(noise(0.06), 3000) * env(int(0.06 * SR), 0.001, 0.03) * 0.5)


def charge():
    d = 1.0
    t = T(d)
    rate = 18 + 30 * t
    ph = np.cumsum(rate / SR)
    clicks = (np.diff(np.floor(ph), prepend=0) > 0).astype(float)
    k = np.convolve(clicks, metal_ring(0.03, 3200, 0.01))[:len(t)]
    return mix(k * 0.8, whirr(d, 120, 320) * 0.25 * np.linspace(0.2, 1, len(t)))


def spin_loop():
    d = 2.0
    n = int(d * SR)
    t = T(d)
    f = 360  # loop must be seamless: integer cycles in 2s
    tone = 0.5 * np.sin(2 * np.pi * f * t) + 0.25 * np.sign(np.sin(2 * np.pi * f * 2 * t)) * 0.4 + 0.2 * np.sin(2 * np.pi * f * 4 * t)
    trem = 0.75 + 0.25 * np.sin(2 * np.pi * 24 * t)
    air = noise(d)
    air = bp(np.concatenate([air[-SR // 2:], air]), 900, 4200)[SR // 2:]
    grind = bp(noise(d), 2500, 7000) * (0.5 + 0.5 * np.sin(2 * np.pi * 12 * t) ** 2) * 0.25
    x = lp(tone * trem, 3000) * 0.55 + air * 0.25 + grind
    # crossfade ends for seamless looping
    m = int(0.1 * SR)
    x[:m] = x[:m] * np.linspace(0, 1, m) + x[-m:] * np.linspace(1, 0, m)
    return x[:n - m]


def clash(heavy=False):
    d = 0.9 if heavy else 0.55
    n = int(d * SR)
    f = 1500 + R.random() * 500
    x = metal_ring(d, f, 0.25 if heavy else 0.14) + metal_ring(d, f * 1.37, 0.2) * 0.6
    sp = hp(noise(0.12), 4000) * env(int(0.12 * SR), 0.001, 0.05) * 1.2
    thump = boom(d, 160 if heavy else 220, 50, 0.18 if heavy else 0.08) * (1.2 if heavy else 0.6)
    y = mix(x * 0.6, sp, thump, crackle(d, 140 if heavy else 60) * env(n, 0.001, 0.25) * 0.8)
    return reverb(y, 0.25 if heavy else 0.12, 0.8)


def wall_hit():
    d = 0.35
    return mix(metal_ring(d, 1100, 0.07) * 0.6, boom(d, 120, 50, 0.08) * 0.8, crackle(d, 40) * 0.4)


def sonic_wave():
    d = 1.6
    n = int(d * SR)
    t = T(d)
    # resonant "wub" + rising harmonic shimmer, ring-modulated
    base = osc(glide(d, 220, 90)) * (0.6 + 0.4 * np.sin(2 * np.pi * 18 * t))
    shimmer = sum(np.sin(2 * np.pi * 880 * k * t * (1 + 0.002 * np.sin(2 * np.pi * 6 * t))) / k for k in (1, 1.5, 2, 3))
    whoosh = sweep_bp(noise(d), 300, 3000, 1.3)
    e = env(n, 0.01, 0.5, 1.0)
    y = mix(base * e * 1.1, shimmer * env(n, 0.03, 0.35, 1.2) * 0.25, whoosh * e * 0.7, boom(d, 90, 35, 0.4) * 0.9)
    return reverb(y, 0.45, 1.6)


def shield_up():
    d = 2.0
    n = int(d * SR)
    t = T(d)
    chord = sum(np.sin(2 * np.pi * f * t + 0.5 * np.sin(2 * np.pi * 5 * t)) for f in (523, 659, 784, 1046))
    sweep = sweep_bp(noise(d), 4000, 600, 2.0) * 0.5
    e = np.minimum(1, t / 0.25) * np.exp(-np.maximum(0, t - 0.4) * 1.6)
    return reverb(mix(chord * e * 0.3, sweep * e, bell(d, 1568, decay=0.8) * 0.3), 0.5, 1.8)


def shield_hit():
    d = 0.8
    return reverb(mix(bell(d, 2093, decay=0.3) * 0.6, bell(d, 1397, decay=0.35) * 0.4, hp(noise(0.05), 5000) * 0.5), 0.4, 1.0)


def inferno_drill():
    d = 1.6
    n = int(d * SR)
    t = T(d)
    grind = bp(noise(d), 200, 1800) * (0.6 + 0.4 * np.sin(2 * np.pi * (14 + 20 * t) * t))
    rumble = lp(brown(d), 160) * 1.5
    whine = whirr(d, 200, 700) * 0.3
    e = np.minimum(1, t / 0.2) * (0.6 + 0.4 * t / d)
    return mix(grind * e * 0.7, rumble * e, whine * e, crackle(d, 160) * e * 0.5)


def inferno_tornado():
    d = 2.4
    n = int(d * SR)
    t = T(d)
    wind = sweep_bp(noise(d), 200, 1400, 1.2) * (0.7 + 0.3 * np.sin(2 * np.pi * 3 * t))
    roar = lp(brown(d), 300) * 1.4
    fire = crackle(d, 260) * 0.7
    e = np.minimum(1, t / 0.3) * np.exp(-np.maximum(0, t - 1.6) * 3)
    return reverb(mix(wind * e, roar * e, fire * e, boom(d, 70, 30, 0.8) * 0.8), 0.35, 1.5)


def inferno_impact():
    d = 2.2
    n = int(d * SR)
    return reverb(mix(boom(d, 120, 25, 0.9) * 1.6, lp(noise(d), 1500) * env(n, 0.002, 0.5, 1.4),
                      crackle(d, 220) * env(n, 0.01, 1.0) * 0.7, metal_ring(d, 900, 0.3) * 0.3), 0.4, 2.0)


def buster_charge():
    d = 1.5
    n = int(d * SR)
    t = T(d)
    f = glide(d, 150, 1400)
    tone = osc(f, 'saw') * 0.3 + osc(f * 1.5, 'sine') * 0.4 + osc(f * 0.5, 'sine') * 0.4
    am = 0.6 + 0.4 * np.sin(2 * np.pi * (6 + 30 * t) * t)
    e = np.minimum(1, t / 0.1)
    return mix(lp(tone, 5000) * am * e, sweep_bp(noise(d), 300, 6000, 2) * e * 0.4)


def buster_fire():
    d = 2.0
    n = int(d * SR)
    t = T(d)
    beam = sum(osc(np.full(n, f) * (1 + 0.01 * np.sin(2 * np.pi * 9 * t)), 'saw') / i for i, f in enumerate((110, 220, 331, 662), 1))
    beam = lp(beam, 2800) * (0.7 + 0.3 * np.sin(2 * np.pi * 30 * t))
    e = env(n, 0.005, 1.1, 1.0)
    return reverb(mix(beam * e * 0.7, boom(d, 140, 40, 0.5) * 1.3, sweep_bp(noise(d), 5000, 400, 1.3) * e), 0.35, 1.4)


def judgment_start():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    choir = sum(formant(osc(np.full(n, f) * (1 + 0.004 * np.sin(2 * np.pi * (4 + i) * t)), 'saw'), 'a') for i, f in enumerate((131, 165, 196, 262)))
    e = np.minimum(1, t / 0.6) * np.exp(-np.maximum(0, t - 1.8) * 2)
    return reverb(mix(choir * e * 0.35, bell(d, 523, decay=1.6) * 0.5, bell(d, 784, decay=1.4) * 0.3), 0.55, 2.4)


def judgment_strike():
    d = 2.4
    n = int(d * SR)
    crack = hp(noise(0.25), 1500) * env(int(0.25 * SR), 0.001, 0.08) * 1.4
    return reverb(mix(crack, boom(d, 100, 30, 0.9) * 1.4, bell(d, 392, decay=1.2) * 0.6, bell(d, 1175, decay=0.8) * 0.3,
                      crackle(d, 90) * env(n, 0.01, 0.6) * 0.5), 0.5, 2.0)


def drain():
    d = 1.2
    n = int(d * SR)
    t = T(d)
    f = glide(d, 900, 300)
    return reverb(mix(osc(f) * 0.4 * env(n, 0.1, 0.6), sweep_bp(noise(d), 3000, 500, 2) * 0.5 * env(n, 0.1, 0.7)), 0.4, 1.2)


def recall():
    d = 0.9
    n = int(d * SR)
    return mix(sweep_bp(noise(d), 400, 4000, 1.4) * env(n, 0.15, 0.35, 1.2), whirr(d, 500, 1200) * 0.3 * env(n, 0.1, 0.4))


def catch():
    d = 0.4
    return mix(metal_ring(d, 2600, 0.06) * 0.5, boom(d, 260, 120, 0.05) * 0.5, hp(noise(0.03), 4000) * 0.4)


def defeat():
    d = 2.0
    n = int(d * SR)
    t = T(d)
    # wobbling top: clacks speeding up then settling
    rate = 6 + 30 * (t / d) ** 2
    ph = np.cumsum(rate / SR)
    clicks = (np.diff(np.floor(ph), prepend=0) > 0).astype(float) * np.exp(-t * 1.0)
    k = np.convolve(clicks, metal_ring(0.08, 1700, 0.03))[:n]
    wind = whirr(d, 500, 90) * env(n, 0.01, 0.8, 1.2) * 0.4
    return mix(k * 0.9, wind, np.concatenate([np.zeros(int(1.5 * SR)), metal_ring(0.5, 1200, 0.2) * 0.5]))


# ======================= L-DRAGO DESTRUCTION =======================
def dragon_roar():
    d = 2.6
    n = int(d * SR)
    t = T(d)
    f0 = glide(d, 110, 70)
    voice = sum(formant(osc(f0 * k * (1 + 0.02 * np.sin(2 * np.pi * (7 + k) * t)), 'saw'), v) for k, v in ((1, 'a'), (1.5, 'o'), (2.02, 'a')))
    growl = voice * (0.7 + 0.3 * np.sign(np.sin(2 * np.pi * 34 * t)))
    e = np.minimum(1, t / 0.15) * np.exp(-np.maximum(0, t - 1.2) * 2.2)
    fire = sweep_bp(noise(d), 300, 2500, 1.5) * 0.6 + crackle(d, 200) * 0.5
    return reverb(mix(np.tanh(growl * 2.2) * e * 0.8, fire * e, boom(d, 80, 30, 0.7)), 0.45, 2.0)


def vortex():
    d = 2.4
    n = int(d * SR)
    t = T(d)
    wind = sweep_bp(noise(d), 1800, 250, 1.3) * (0.6 + 0.4 * np.sin(2 * np.pi * (2 + 3 * t) * t))
    hum = osc(glide(d, 300, 90), 'saw') * 0.25
    e = np.minimum(1, t / 0.3) * np.exp(-np.maximum(0, t - 1.6) * 3)
    return reverb(mix(wind * e, lp(hum, 1200) * e, lp(brown(d), 200) * e * 0.8), 0.4, 1.6)


def dragon_rise():
    d = 1.8
    n = int(d * SR)
    t = T(d)
    jet = sweep_bp(noise(d), 200, 4000, 1.4) * np.minimum(1, t / 0.05) * np.exp(-t * 1.1)
    roar = lp(brown(d), 400) * np.exp(-t * 1.5) * 1.3
    return reverb(mix(jet, roar, boom(d, 140, 40, 0.4), crackle(d, 200) * np.exp(-t * 1.2) * 0.6), 0.35, 1.5)


def meteor_charge():
    d = 1.6
    n = int(d * SR)
    t = T(d)
    tone = osc(glide(d, 60, 240), 'saw') * 0.4 + osc(glide(d, 90, 360)) * 0.4
    fire = crackle(d, 300) * 0.6 + bp(noise(d), 200, 1500) * 0.5
    e = np.minimum(1, t / 0.2) * (0.5 + 0.5 * t / d)
    return mix(lp(tone, 2000) * e, fire * e)


def meteor_impact():
    d = 3.0
    n = int(d * SR)
    return reverb(mix(boom(d, 90, 20, 1.2) * 1.8, lp(noise(d), 1200) * env(n, 0.002, 0.7, 1.3), crackle(d, 260) * env(n, 0.01, 1.4) * 0.8,
                      metal_ring(d, 600, 0.4) * 0.25), 0.5, 2.4)


def claw_slash():
    d = 0.6
    n = int(d * SR)
    t = T(d)
    swish = sweep_bp(noise(d), 6000, 900, 2.0) * env(n, 0.01, 0.12, 1.0)
    return mix(swish * 1.2, metal_ring(d, 2200, 0.08) * 0.5, boom(d, 200, 70, 0.08) * 0.6)


def fire_breath():
    d = 2.6
    n = int(d * SR)
    t = T(d)
    roar = sweep_bp(noise(d), 300, 900, 1.2) * 0.8 + lp(brown(d), 250) * 1.2
    e = np.minimum(1, t / 0.12) * np.exp(-np.maximum(0, t - 1.9) * 4)
    return reverb(mix(roar * e, crackle(d, 320) * e * 0.8), 0.3, 1.2)


# ======================= ROCK LEONE =======================
def lion_roar():
    d = 2.4
    n = int(d * SR)
    t = T(d)
    f0 = glide(d, 140, 85)
    voice = sum(formant(osc(f0 * k * (1 + 0.015 * np.sin(2 * np.pi * (5 + k) * t)), 'saw'), v) for k, v in ((1, 'o'), (1.5, 'a'), (2.0, 'o')))
    rough = 0.65 + 0.35 * np.sign(np.sin(2 * np.pi * 26 * t))
    e = np.minimum(1, t / 0.1) * np.exp(-np.maximum(0, t - 1.0) * 2.4)
    wind = sweep_bp(noise(d), 400, 2200, 1.4) * 0.5
    return reverb(mix(np.tanh(voice * rough * 2.0) * e * 0.9, wind * e, boom(d, 70, 32, 0.6) * 0.7), 0.45, 1.8)


def wind_wall():
    d = 2.2
    n = int(d * SR)
    t = T(d)
    howl = sweep_bp(noise(d), 300, 1600, 1.8) * (0.6 + 0.4 * np.sin(2 * np.pi * 4 * t))
    whistle = osc(glide(d, 600, 900)) * 0.12 * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * t))
    e = np.minimum(1, t / 0.25) * np.exp(-np.maximum(0, t - 1.5) * 3)
    return reverb(mix(howl * e, whistle * e, lp(brown(d), 200) * e * 0.6), 0.4, 1.4)


def fang_shot():
    d = 0.35
    n = int(d * SR)
    swish = sweep_bp(noise(d), 5000, 1200, 2.2) * env(n, 0.005, 0.08, 1.0)
    return mix(swish * 1.2, osc(glide(d, 1400, 500)) * env(n, 0.003, 0.05) * 0.3)


def quake():
    d = 2.8
    n = int(d * SR)
    t = T(d)
    rumble = lp(brown(d), 120) * 1.6 * np.exp(-np.maximum(0, t - 0.4) * 1.2)
    crack = hp(noise(0.3), 800) * env(int(0.3 * SR), 0.001, 0.1) * 1.2
    debris = crackle(d, 160) * env(n, 0.05, 1.2) * 0.8
    return reverb(mix(rumble, crack, debris, boom(d, 60, 22, 1.0) * 1.4), 0.4, 2.0)


def cage_wind():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    a = sweep_bp(noise(d), 500, 2500, 2.5) * (0.5 + 0.5 * np.sin(2 * np.pi * 1.5 * t))
    b = sweep_bp(noise(d), 2500, 600, 2.5) * (0.5 + 0.5 * np.cos(2 * np.pi * 1.1 * t))
    e = np.minimum(1, t / 0.4) * np.exp(-np.maximum(0, t - 2.3) * 4)
    return reverb(mix(a * e, b * e, lp(brown(d), 180) * e), 0.5, 1.8)


def cage_blast():
    d = 2.4
    n = int(d * SR)
    return reverb(mix(boom(d, 110, 28, 0.9) * 1.5, sweep_bp(noise(d), 4000, 300, 1.4) * env(n, 0.002, 0.6, 1.2), crackle(d, 120) * env(n, 0.01, 0.8) * 0.5), 0.45, 2.0)


# ======================= EARTH EAGLE =======================
def eagle_screech():
    d = 1.6
    n = int(d * SR)
    t = T(d)
    f = 2200 + 900 * np.exp(-t * 4) - 700 * t
    tone = osc(f * (1 + 0.03 * np.sin(2 * np.pi * 38 * t)), 'saw') * 0.5 + osc(f * 1.5) * 0.3
    tone = bp(tone, 900, 7000)
    rasp = bp(noise(d), 2500, 6000) * 0.5
    e = np.minimum(1, t / 0.04) * np.exp(-np.maximum(0, t - 0.5) * 3.5)
    return reverb(mix(np.tanh(tone * 2) * e, rasp * e), 0.5, 1.6)


def feather_whirl():
    d = 1.4
    n = int(d * SR)
    t = T(d)
    whirl = sweep_bp(noise(d), 1500, 5000, 2.5) * (0.6 + 0.4 * np.sin(2 * np.pi * 14 * t))
    chimes = sum(bell(d, f, decay=0.4) * 0.12 for f in (2637, 3136, 3520, 4186))
    return reverb(mix(whirl * env(n, 0.15, 0.6, 1.0), chimes), 0.4, 1.2)


def feather_burst():
    d = 1.0
    n = int(d * SR)
    return reverb(mix(sweep_bp(noise(d), 6000, 1500, 2.0) * env(n, 0.003, 0.25, 1.2) * 1.3, metal_ring(d, 2800, 0.12) * 0.4,
                      boom(d, 180, 60, 0.15) * 0.6), 0.3, 1.0)


def wing_blade():
    d = 1.8
    n = int(d * SR)
    t = T(d)
    swish = sweep_bp(noise(d), 600, 3500, 1.6) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 3.5 * t)))
    ring = metal_ring(d, 1300, 0.6) * 0.3
    return reverb(mix(swish * env(n, 0.05, 1.0, 1.0), ring, boom(d, 120, 50, 0.3) * 0.5), 0.35, 1.3)


def sky_dive():
    d = 1.8
    n = int(d * SR)
    t = T(d)
    wind = sweep_bp(noise(d), 300, 4500, 1.4) * np.minimum(1, t / 0.6) * np.exp(-np.maximum(0, t - 1.2) * 4)
    return mix(wind * 1.2, eagle_screech()[:n] * 0.5)


def lance_rain():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    out = np.zeros(n)
    for k in range(18):
        p = int((0.2 + k * 0.14 + R.random() * 0.05) * SR)
        L = int(0.35 * SR)
        if p + L >= n:
            break
        seg = sweep_bp(noise(0.35), 5000, 800, 2.5) * env(L, 0.01, 0.12, 1.0)[:L]
        out[p:p + L] += seg[:L] * 0.7 + boom(0.35, 150, 60, 0.06)[:L] * 0.4
    choir = sum(formant(osc(np.full(n, f), 'saw'), 'a') for f in (220, 277, 330)) * 0.12 * np.minimum(1, t / 0.5) * np.exp(-np.maximum(0, t - 2.4) * 3)
    return reverb(mix(out, choir), 0.45, 1.6)


if __name__ == '__main__':
    jobs = {
        'launch': launch, 'land': land, 'charge': charge, 'spin_loop': spin_loop,
        'clash1': lambda: clash(), 'clash2': lambda: clash(), 'clash3': lambda: clash(), 'clash_heavy': lambda: clash(True),
        'wall_hit': wall_hit, 'sonic_wave': sonic_wave, 'shield_up': shield_up, 'shield_hit': shield_hit,
        'inferno_drill': inferno_drill, 'inferno_tornado': inferno_tornado, 'inferno_impact': inferno_impact,
        'buster_charge': buster_charge, 'buster_fire': buster_fire,
        'judgment_start': judgment_start, 'judgment_strike': judgment_strike, 'drain': drain,
        'recall': recall, 'catch': catch, 'defeat': defeat,
        'dragon_roar': dragon_roar, 'vortex': vortex, 'dragon_rise': dragon_rise, 'meteor_charge': meteor_charge,
        'meteor_impact': meteor_impact, 'claw_slash': claw_slash, 'fire_breath': fire_breath,
        'eagle_screech': eagle_screech, 'feather_whirl': feather_whirl, 'feather_burst': feather_burst, 'wing_blade': wing_blade,
        'sky_dive': sky_dive, 'lance_rain': lance_rain,
        'lion_roar': lion_roar, 'wind_wall': wind_wall, 'fang_shot': fang_shot, 'quake': quake, 'cage_wind': cage_wind, 'cage_blast': cage_blast,
    }
    import sys
    if len(sys.argv) > 1:
        jobs = {k: v for k, v in jobs.items() if k in sys.argv[1:]}
    quiet = {'spin_loop': -4, 'charge': -4, 'land': -3, 'wall_hit': -4}
    for name, fn in jobs.items():
        save(name, fn(), quiet.get(name, -1.0), name != 'spin_loop')
    print(len(jobs), 'sounds')
