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
