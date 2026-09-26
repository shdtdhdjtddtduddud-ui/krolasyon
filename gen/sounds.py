"""Procedural sound design for both bosses -> mono .ogg files."""
import os, subprocess, json, numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolasyonbosses/sounds')
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


def save(name, x, gain_db=-1.0):
    x = fade(np.nan_to_num(x))
    x = x / (np.abs(x).max() + 1e-9) * 10 ** (gain_db / 20)
    wav = f'/tmp/claude-0/{name}.wav'
    import wave
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    import shutil
    if shutil.which('ffmpeg'):
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', '-ac', '1',
                        os.path.join(OUT, f'{name}.ogg')], check=True)
    else:
        import soundfile
        soundfile.write(os.path.join(OUT, f'{name}.ogg'), x.astype(np.float32), SR, format='OGG', subtype='VORBIS')


# ======================= SEAL WARDEN (crystal / arcane / stone) =======================
def warden_ambient(v):
    d = 2.6
    n = int(d * SR)
    t = T(d)
    base = 55 if v == 1 else 49
    hum = osc(base * (1 + 0.02 * np.sin(2 * np.pi * 0.7 * t)), 'saw')
    hum = lp(hum, 400) * (0.6 + 0.4 * np.sin(2 * np.pi * 3.1 * t))
    shimmer = sum(np.sin(2 * np.pi * f * t) * (0.5 + 0.5 * np.sin(2 * np.pi * (1.3 + i) * t)) for i, f in enumerate((1318, 1760, 2349 if v == 1 else 2093)))
    x = hum * 0.9 + shimmer * 0.12 + formant(osc(glide(d, base * 2, base * 1.7), 'saw'), 'o') * 0.4
    return reverb(x * adsr(n, 0.4, 0.8, 0.9), 0.35)


def warden_hurt(v):
    d = 0.6
    n = int(d * SR)
    crack = hp(noise(d), 1800) * env(n, 0.001, 0.05)
    crunch = bp(noise(d), 200, 1200) * env(n, 0.002, 0.12)
    ping = bell(d, [1250, 1480, 1110][v - 1], decay=0.25) * 0.5
    groan = formant(osc(glide(d, [150, 135, 165][v - 1], 90), 'saw'), 'a') * env(n, 0.02, 0.2) * 0.8
    return reverb(mix(crack * 0.8, crunch, ping, groan), 0.25, 0.8)


def warden_death():
    d = 3.2
    n = int(d * SR)
    groan = formant(osc(glide(d, 130, 38), 'saw') * rattle(n, 18, 0.4), 'o') * adsr(n, 0.1, 0.9, 1.2)
    shatter = np.zeros(n)
    for i in range(70):
        s = int(R.random() ** 0.7 * (n - SR // 2) + SR * 0.9)
        if s >= n: continue
        b = bell(0.5, 900 + R.random() * 3000, decay=0.12 + R.random() * 0.2) * (0.3 + R.random() * 0.7)
        shatter[s:s + len(b)] += b[:n - s]
    thud = pad(np.zeros(int(0.9 * SR)), 0.9)
    bm = np.concatenate([thud, boom(2.0, 90, 30, 0.6)])
    return reverb(mix(groan * 1.2, shatter * 0.35, bm * 1.4), 0.4, 2.0)


def warden_step(v):
    d = 0.4
    n = int(d * SR)
    thud = boom(d, [70, 64, 76][v - 1], 40, 0.08)
    grit = lp(noise(d), 1200) * env(n, 0.001, 0.05)
    clink = bell(d, [2600, 3100, 2300][v - 1], decay=0.05) * 0.15
    return mix(thud * 1.3, grit * 0.5, clink)


def warden_swipe():
    d = 0.6
    n = int(d * SR)
    wh = sweep_bp(noise(d), 400, 3500, 1.8) * np.sin(np.pi * np.clip(T(d) / 0.45, 0, 1)) ** 2
    ch = bell(d, 1760, decay=0.2) * 0.25 * env(n, 0.15, 0.3)
    return mix(wh * 1.2, ch)


def laser_charge():
    d = 0.9
    n = int(d * SR)
    t = T(d)
    f = glide(d, 180, 1400)
    trem = 0.5 + 0.5 * np.sin(2 * np.pi * np.cumsum(glide(d, 6, 40)) / SR)
    x = (osc(f) + 0.5 * osc(f * 1.5) + 0.3 * osc(f * 2.01, 'tri')) * trem
    sw = sweep_bp(noise(d), 300, 6000, 1.4) * 0.5
    return reverb(mix(x * np.linspace(0.2, 1, n) ** 2, sw * np.linspace(0, 1, n) ** 3), 0.2, 0.6)


def laser_fire():
    d = 1.3
    n = int(d * SR)
    t = T(d)
    f = 170 * (1 + 0.04 * np.sin(2 * np.pi * 9 * t))
    buzz = osc(f, 'saw') + 0.6 * osc(f * 2.003, 'saw') + 0.3 * osc(f * 3.01, 'pulse')
    buzz = bp(buzz, 150, 4000)
    sizzle = hp(noise(d), 4000) * (0.6 + 0.4 * np.sin(2 * np.pi * 31 * t))
    blast = boom(0.6, 200, 60, 0.15)
    e = adsr(n, 0.01, 0.85, 0.4)
    return reverb(mix(buzz * e * 0.7, sizzle * e * 0.35, blast), 0.25, 0.8)


def crystal_erupt():
    d = 0.9
    n = int(d * SR)
    base = 1000 + R.random() * 300
    b = mix(*[bell(d, base * r, decay=0.3) * g for r, g in ((1, 1), (1.19, 0.7), (1.5, 0.6), (2.02, 0.4))])
    burst = bp(noise(d), 1500, 9000) * env(n, 0.001, 0.07)
    th = boom(d, 120, 45, 0.12)
    grind = bp(noise(d), 300, 1500) * env(n, 0.005, 0.2) * 0.5
    return reverb(mix(b * 0.45, burst * 0.9, th * 1.2, grind), 0.3, 1.0)


def prison_form():
    d = 1.6
    n = int(d * SR)
    t = T(d)
    chord = sum(osc(f * (1 + 0.003 * np.sin(2 * np.pi * 5 * t + i))) for i, f in enumerate((220, 261.6, 329.6, 440)))
    sh = sum(np.sin(2 * np.pi * f * t) for f in (1760, 2093, 2637)) * (0.5 + 0.5 * np.sin(2 * np.pi * 12 * t))
    wind = sweep_bp(noise(d), 200, 2500, 1.5)
    e = adsr(n, 0.5, 0.9, 0.5)
    return reverb(mix(chord * e * 0.3, sh * e * 0.06, wind * e * 0.5), 0.45, 1.5)


def prison_burst():
    d = 1.5
    n = int(d * SR)
    bm = boom(d, 140, 32, 0.45)
    ex = lp(noise(d), 2500) * env(n, 0.002, 0.25)
    bells = mix(*[pad(np.zeros(int(R.random() * 0.2 * SR)), 0) if False else bell(d, f, decay=0.5) for f in (880, 1108, 1318, 1760)])
    return reverb(mix(bm * 1.5, ex * 0.9, bells * 0.15), 0.35, 1.5)


def blink():
    d = 0.7
    n = int(d * SR)
    t = T(d)
    rev = sweep_bp(noise(d), 300, 5000, 1.6) * (t / d) ** 3
    zap = osc(glide(d, 2200, 180)) * env(n, 0.3, 0.2)
    pop = boom(0.25, 400, 80, 0.05)
    return mix(rev * 1.0, zap * 0.5, pad(np.zeros(int(0.55 * SR)), 0) if False else np.concatenate([np.zeros(int(0.55 * SR)), pop]))


def summon():
    d = 2.2
    n = int(d * SR)
    t = T(d)
    voices = sum(osc(f * (1 + 0.006 * np.sin(2 * np.pi * (4.5 + i * 0.3) * t)), 'saw') for i, f in enumerate((220, 221.5, 277.2, 329.6, 330.8)))
    choir = formant(voices, 'a')
    bells = mix(*[np.concatenate([np.zeros(int((0.3 + i * 0.25) * SR)), bell(1.2, f, decay=0.4)]) for i, f in enumerate((1318, 1568, 1760, 2093))])
    e = adsr(n, 0.6, 0.9, 0.7)
    return reverb(mix(choir * e * 0.5, pad(bells, d)[:n] * 0.12), 0.5, 2.0)


def bolt_shoot():
    d = 0.35
    n = int(d * SR)
    zap = osc(glide(d, 1900, 380)) * env(n, 0.002, 0.12)
    cl = hp(noise(d), 3000) * env(n, 0.001, 0.02)
    return mix(zap, cl * 0.5, osc(glide(d, 950, 190), 'tri') * env(n, 0.002, 0.1) * 0.4)


def bolt_hit():
    d = 0.45
    n = int(d * SR)
    return reverb(mix(bell(d, 1500 + R.random() * 400, decay=0.15) * 0.6, bp(noise(d), 1000, 8000) * env(n, 0.001, 0.05)), 0.2, 0.5)


def warden_phase():
    d = 2.8
    n = int(d * SR)
    t = T(d)
    roar = formant(sum(osc(f * (1 + 0.01 * np.sin(2 * np.pi * 6 * t)), 'saw') for f in (65, 97.5, 130.8)) * rattle(n, 22, 0.5), 'a')
    sh = sum(osc(f) for f in (1318, 1661, 1975)) * (0.5 + 0.5 * np.sin(2 * np.pi * 17 * t))
    bm = boom(d, 100, 30, 0.6)
    e = adsr(n, 0.15, 0.9, 1.0)
    return reverb(mix(np.tanh(roar * 2) * e, sh * e * 0.08, bm * 1.2), 0.45, 2.0)


# ======================= CRIMSON HOUND (feral / fire) =======================
def growl(d, f0, f1, vowel='o', rate=26):
    n = int(d * SR)
    t = T(d)
    f = glide(d, f0, f1) * (1 + 0.03 * np.sin(2 * np.pi * 4 * t))
    src = osc(f, 'pulse') + 0.5 * osc(f * 1.01, 'saw')
    src = src * rattle(n, rate, 0.75)
    breath = bp(noise(d), 300, 3000) * 0.35
    return formant(src + breath, vowel)


def hound_ambient(v):
    d = 2.0
    n = int(d * SR)
    g = growl(d, 78 if v == 1 else 70, 68 if v == 1 else 84, 'o')
    fire = crackle(d, 40) * 0.2 + lp(noise(d), 700) * 0.1
    return reverb(mix(np.tanh(g * 1.5) * adsr(n, 0.25, 0.9, 0.6), fire), 0.2, 0.8)


def hound_hurt(v):
    d = 0.55
    n = int(d * SR)
    f0 = [520, 460, 600][v - 1]
    yelp = formant(osc(glide(d, f0, f0 * 0.55), 'saw'), 'a') * env(n, 0.01, 0.18)
    snarl = growl(d, 120, 95, 'a', 34) * env(n, 0.005, 0.15) * 0.8
    return reverb(mix(yelp, snarl), 0.2, 0.6)


def hound_death():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    f = np.interp(t, [0, 0.4, 2.2, 3.0], [380, 520, 170, 120]) * (1 + 0.02 * np.sin(2 * np.pi * 5 * t))
    howl = formant(osc(f, 'saw') + 0.4 * osc(f * 2), 'u') * adsr(n, 0.2, 0.9, 1.2)
    g = growl(d, 90, 50, 'o') * np.linspace(0.3, 1, n) * np.linspace(1, 0, n) ** 0.5
    fire = crackle(d, 50) * np.linspace(1, 0.2, n) * 0.4
    return reverb(mix(howl * 0.9, np.tanh(g) * 0.6, fire), 0.45, 2.0)


def hound_step(v):
    d = 0.32
    n = int(d * SR)
    th = boom(d, [85, 78, 92][v - 1], 45, 0.06)
    cl = hp(noise(d), 3500) * env(n, 0.001, 0.012)
    return mix(th * 1.2, cl * 0.25, lp(noise(d), 900) * env(n, 0.001, 0.04) * 0.4)


def hound_bite():
    d = 0.45
    n = int(d * SR)
    snap = hp(noise(d), 1500) * env(n, 0.0005, 0.012)
    thock = boom(0.2, 300, 120, 0.03)
    snarl = growl(d, 140, 100, 'a', 38) * env(n, 0.01, 0.12)
    return mix(snap * 1.2, thock, np.tanh(snarl * 2) * 0.7)


def hound_leap():
    d = 0.7
    n = int(d * SR)
    grunt = growl(0.3, 150, 110, 'a', 30) * env(int(0.3 * SR), 0.01, 0.08)
    wh = sweep_bp(noise(d), 250, 2200, 1.8) * np.sin(np.pi * T(d) / d) ** 2
    fire = lp(noise(d), 1200) * np.sin(np.pi * T(d) / d) * 0.5
    return mix(np.tanh(grunt * 2), wh * 0.9, fire)


def hound_land():
    d = 1.3
    n = int(d * SR)
    bm = boom(d, 120, 30, 0.35)
    debris = lp(noise(d), 3000) * env(n, 0.002, 0.25)
    fire = sweep_bp(noise(d), 1500, 300, 1.8) * env(n, 0.02, 0.5) * 0.7
    return reverb(mix(bm * 1.6, debris, fire, crackle(d, 50) * 0.4), 0.3, 1.2)


def hound_howl():
    d = 3.4
    n = int(d * SR)
    t = T(d)
    f = np.interp(t, [0, 0.35, 1.2, 2.6, 3.4], [260, 470, 540, 500, 380])
    vib = 1 + 0.018 * np.sin(2 * np.pi * 5.2 * t) * np.clip((t - 0.6) / 0.6, 0, 1)
    f = f * vib
    voice = osc(f, 'saw') + 0.45 * osc(f * 2.0) + 0.2 * osc(f * 3.0)
    howl = 0.6 * formant(voice, 'u') + 0.4 * formant(voice, 'o')
    layer = formant(osc(f * 0.5, 'saw'), 'o') * 0.4
    roar = lp(noise(d), 900) * 0.15
    e = adsr(n, 0.35, 0.95, 1.1)
    return reverb(mix(howl * e, layer * e, roar * e), 0.55, 2.5)


def fissure():
    d = 1.3
    n = int(d * SR)
    rumble = lp(brown(d), 180) * adsr(n, 0.02, 0.9, 0.7)
    cracks = mix(*[np.concatenate([np.zeros(int(R.random() * 0.8 * SR)), hp(noise(0.08), 800) * env(int(0.08 * SR), 0.0005, 0.02)]) for _ in range(9)])
    fire = sweep_bp(noise(d), 300, 1800, 1.6) * adsr(n, 0.1, 0.8, 0.6) * 0.6
    return reverb(mix(rumble * 1.5, pad(cracks, d) * 0.5, fire, boom(d, 90, 35, 0.2)), 0.3, 1.2)


def fireball_shoot():
    d = 0.8
    n = int(d * SR)
    wh = sweep_bp(noise(d), 200, 2400, 1.7) * env(n, 0.03, 0.35)
    return mix(wh * 1.2, crackle(d, 60) * env(n, 0.01, 0.4) * 0.5, boom(0.4, 180, 70, 0.08) * 0.6)


def fireball_hit():
    d = 1.2
    n = int(d * SR)
    return reverb(mix(boom(d, 110, 35, 0.3) * 1.5, lp(noise(d), 3500) * env(n, 0.002, 0.25), crackle(d, 70) * np.linspace(1, 0, n) * 0.6), 0.3, 1.0)


def frenzy_slash():
    d = 0.38
    n = int(d * SR)
    wh = sweep_bp(noise(d), 700, 4500, 1.8) * np.sin(np.pi * T(d) / d) ** 3
    scrape = bp(noise(d), 3000, 9000) * env(n, 0.12, 0.05) * 0.5
    return mix(wh * 1.2, scrape)


def hound_phase():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    voices = sum(growl(d, f, f * 0.8, 'a', 24 + i * 3) for i, f in enumerate((62, 93, 124)))
    fire = sweep_bp(noise(d), 200, 2000, 1.6) * 0.5 + crackle(d, 90) * 0.4
    e = adsr(n, 0.12, 0.95, 1.0)
    return reverb(mix(np.tanh(voices * 1.8) * e, fire * e, boom(d, 90, 28, 0.6)), 0.4, 2.0)


if __name__ == '__main__':
    jobs = {
        'warden_ambient1': lambda: warden_ambient(1), 'warden_ambient2': lambda: warden_ambient(2),
        'warden_hurt1': lambda: warden_hurt(1), 'warden_hurt2': lambda: warden_hurt(2), 'warden_hurt3': lambda: warden_hurt(3),
        'warden_death': warden_death,
        'warden_step1': lambda: warden_step(1), 'warden_step2': lambda: warden_step(2), 'warden_step3': lambda: warden_step(3),
        'warden_swipe': warden_swipe, 'laser_charge': laser_charge, 'laser_fire': laser_fire, 'crystal_erupt': crystal_erupt,
        'prison_form': prison_form, 'prison_burst': prison_burst, 'blink': blink, 'summon': summon,
        'bolt_shoot': bolt_shoot, 'bolt_hit': bolt_hit, 'warden_phase': warden_phase,
        'hound_ambient1': lambda: hound_ambient(1), 'hound_ambient2': lambda: hound_ambient(2),
        'hound_hurt1': lambda: hound_hurt(1), 'hound_hurt2': lambda: hound_hurt(2), 'hound_hurt3': lambda: hound_hurt(3),
        'hound_death': hound_death,
        'hound_step1': lambda: hound_step(1), 'hound_step2': lambda: hound_step(2), 'hound_step3': lambda: hound_step(3),
        'hound_bite': hound_bite, 'hound_leap': hound_leap, 'hound_land': hound_land, 'hound_howl': hound_howl,
        'fissure': fissure, 'fireball_shoot': fireball_shoot, 'fireball_hit': fireball_hit,
        'frenzy_slash': frenzy_slash, 'hound_phase': hound_phase,
    }
    quiet = {'warden_step1': -6, 'warden_step2': -6, 'warden_step3': -6, 'hound_step1': -7, 'hound_step2': -7, 'hound_step3': -7,
             'warden_ambient1': -3, 'warden_ambient2': -3, 'hound_ambient1': -2, 'hound_ambient2': -2}
    for name, fn in jobs.items():
        save(name, fn(), quiet.get(name, -1.0))
    print(len(jobs), 'sounds')
