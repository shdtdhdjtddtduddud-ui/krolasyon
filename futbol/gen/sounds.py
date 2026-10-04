"""Procedural football sound design -> mono .ogg (vorbis) files."""
import os, subprocess, tempfile, numpy as np
from scipy.signal import butter, sosfilt
from scipy.io import wavfile

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolasyonfutbol/sounds')
os.makedirs(OUT, exist_ok=True)
R = np.random.default_rng(1905)


def T(d): return np.arange(int(d * SR)) / SR
def noise(d): return R.standard_normal(int(d * SR))
def bp(x, lo, hi, o=2): return sosfilt(butter(o, [lo, hi], 'bandpass', fs=SR, output='sos'), x)
def lp(x, f, o=2): return sosfilt(butter(o, f, 'lowpass', fs=SR, output='sos'), x)
def hp(x, f, o=2): return sosfilt(butter(o, f, 'highpass', fs=SR, output='sos'), x)


def env(n, a, d):
    t = np.arange(n) / SR
    return np.minimum(1, t / max(a, 1e-4)) * np.exp(-np.maximum(0, t - a) / max(d, 1e-4))


def fade(x, fi=0.005, fo=0.02):
    n = len(x)
    a, b = int(fi * SR), int(fo * SR)
    x = x.copy()
    if a: x[:a] *= np.linspace(0, 1, a)
    if b: x[-b:] *= np.linspace(1, 0, b)
    return x


def pad(x, d):
    out = np.zeros(int(d * SR))
    out[:min(len(x), len(out))] = x[:len(out)]
    return out


def write(name, x, peak=0.9):
    x = fade(np.asarray(x, float))
    m = np.abs(x).max() + 1e-9
    x = x / m * peak
    with tempfile.TemporaryDirectory() as td:
        wav = os.path.join(td, 'a.wav')
        wavfile.write(wav, SR, (x * 32767).astype(np.int16))
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', '-ac', '1',
                        os.path.join(OUT, f'{name}.ogg')], check=True)


def thump(f0, f1, d, decay):
    t = T(d)
    f = f1 + (f0 - f1) * np.exp(-t / 0.02)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * env(len(t), 0.001, decay)


def kick(seed, power=1.0):
    d = 0.35 + 0.25 * power
    body = thump(180 + seed * 15, 70 + seed * 6, d, 0.06 + 0.03 * power)
    slap = bp(noise(d), 900, 4200) * env(int(d * SR), 0.0005, 0.012)
    leather = bp(noise(d), 300, 900) * env(int(d * SR), 0.001, 0.03)
    x = body * 1.0 + slap * (0.55 + 0.2 * seed) + leather * 0.4
    if power > 1:
        wh = bp(noise(d), 600, 3000) * env(int(d * SR), 0.03, 0.18) * 0.35
        sweep = np.linspace(1, 0.2, len(wh))
        x = x + wh * sweep
    return x


def main():
    for i in range(3):
        write(f'kick{i + 1}', kick(i))
    write('kick_power', kick(1, 2.0) + 0.3 * lp(noise(0.85), 200) * env(int(0.85 * SR), 0.001, 0.08))
    for i in range(2):
        x = thump(260 + 30 * i, 120, 0.25, 0.035) + 0.4 * bp(noise(0.25), 1500, 5000) * env(int(0.25 * SR), 0.0005, 0.008)
        write(f'pass{i + 1}', x, 0.7)
    write('header', thump(150, 80, 0.3, 0.05) + 0.5 * lp(noise(0.3), 1200) * env(int(0.3 * SR), 0.001, 0.025))
    for i in range(2):
        write(f'bounce{i + 1}', thump(140 + 20 * i, 75, 0.25, 0.045) + 0.2 * bp(noise(0.25), 800, 2500) * env(int(0.25 * SR), 0.0005, 0.01), 0.7)

    # post: inharmonic metal clang
    d = 1.6
    t = T(d)
    x = np.zeros(len(t))
    for f, a, dec in ((612, 1.0, 0.9), (1489, 0.6, 0.6), (2350, 0.45, 0.45), (3610, 0.3, 0.3), (5120, 0.2, 0.2), (887, 0.5, 0.7)):
        x += a * np.sin(2 * np.pi * f * t * (1 + 0.0015 * np.sin(2 * np.pi * 5 * t))) * np.exp(-t / dec)
    x += 0.8 * bp(noise(d), 2000, 8000) * env(len(t), 0.0005, 0.006)
    x += 0.6 * thump(200, 90, d, 0.04)
    write('post_hit', x)

    # net swish
    d = 0.7
    n = bp(noise(d), 1800, 7000) * env(int(d * SR), 0.01, 0.18)
    rustle = n * (0.6 + 0.4 * np.sin(2 * np.pi * 23 * T(d)) ** 2)
    write('net_hit', rustle + 0.4 * thump(120, 70, d, 0.05), 0.75)

    # referee whistle with pea trill
    def whistle(d, f=2950):
        t = T(d)
        trill = 0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 38 * t + 0.3 * np.sin(2 * np.pi * 3 * t)))
        trill = lp(trill, 300)
        tone = np.sin(2 * np.pi * f * t + 0.6 * np.sin(2 * np.pi * 38 * t)) + 0.25 * np.sin(2 * np.pi * 2 * f * t)
        breath = bp(noise(d), 2500, 6000) * 0.12
        e = np.minimum(1, t / 0.02) * np.minimum(1, (d - t) / 0.04)
        return (tone * trill + breath) * e
    write('whistle', pad(whistle(0.55), 0.65), 0.8)
    end = np.concatenate([whistle(0.32), np.zeros(int(0.12 * SR)), whistle(0.32), np.zeros(int(0.12 * SR)), whistle(1.1, 2900)])
    write('whistle_end', end, 0.8)

    # crowd synthesis helpers
    def crowd_bed(d, level=1.0):
        t = T(d)
        base = np.zeros(len(t))
        for lo, hi, a in ((200, 500, 1.0), (500, 1200, 0.8), (1200, 3000, 0.45)):
            band = bp(noise(d), lo, hi)
            mod = 0.75 + 0.25 * lp(R.standard_normal(len(t)), 3) / 0.05
            base += a * band * np.clip(mod, 0.3, 1.6)
        return base * level

    def voices(d, n, f_lo, f_hi, rise=False):
        t = T(d)
        out = np.zeros(len(t))
        for _ in range(n):
            f0 = R.uniform(f_lo, f_hi)
            st = R.uniform(0, d * 0.5)
            ln = R.uniform(0.6, d - st)
            mask = (t >= st) & (t < st + ln)
            tt = t[mask] - st
            glide = 1 + (0.25 * tt / ln if rise else 0.08 * np.sin(np.pi * tt / ln))
            vib = 1 + 0.02 * np.sin(2 * np.pi * R.uniform(4, 7) * tt)
            ph = 2 * np.pi * np.cumsum(f0 * glide * vib) / SR
            sig = np.sign(np.sin(ph)) * 0.3 + np.sin(ph)
            e = np.sin(np.pi * tt / ln) ** 1.5
            out[mask] += sig * e * R.uniform(0.3, 1.0)
        return bp(out, 250, 2600)

    d = 5.0
    t = T(d)
    swell = np.clip(t / 0.25, 0, 1) * np.exp(-np.maximum(0, t - 1.8) / 1.6)
    cheer = crowd_bed(d) * swell + 0.55 * voices(d, 70, 220, 520, rise=True) * swell
    claps = np.zeros(len(t))
    for _ in range(260):
        p = int(R.uniform(0.2, 4.6) * SR)
        c = bp(R.standard_normal(600), 800, 4000) * np.exp(-np.arange(600) / 90)
        claps[p:p + 600] += c * R.uniform(0.2, 0.6)
    write('crowd_cheer', cheer + claps * swell * 0.5, 0.95)

    for i in range(2):
        d = 7.0
        t = T(d)
        e = np.minimum(1, t / 0.8) * np.minimum(1, (d - t) / 0.8)
        amb = crowd_bed(d, 0.8) * e + 0.18 * voices(d, 25, 180, 420) * e
        if i == 1:
            chant = np.zeros(len(t))
            for k in range(6):
                st = 0.6 + k * 0.95
                m = (t >= st) & (t < st + 0.5)
                tt = t[m] - st
                chant[m] += np.sin(2 * np.pi * (240 if k % 2 == 0 else 300) * tt) * np.sin(np.pi * tt / 0.5)
            amb += 0.25 * bp(chant + 0.3 * crowd_bed(d, 0.3), 200, 2000) * e
        write(f'crowd_ambient{i + 1}', amb, 0.6)

    d = 2.0
    t = T(d)
    ooh = np.zeros(len(t))
    for _ in range(40):
        f0 = R.uniform(150, 320)
        ph = 2 * np.pi * np.cumsum(f0 * (1 + 0.18 * np.sin(np.pi * t / d))) / SR
        ooh += (np.sin(ph) + 0.4 * np.sin(2 * ph) + 0.2 * np.sin(3 * ph)) * R.uniform(0.5, 1)
    ooh = bp(ooh, 200, 900) * (np.sin(np.pi * t / d) ** 1.2) + 0.3 * crowd_bed(d) * np.sin(np.pi * t / d)
    write('crowd_ooh', ooh, 0.85)

    # stadium goal horn
    d = 2.2
    t = T(d)
    horn = np.zeros(len(t))
    for f in (233.08, 277.18, 349.23):
        for h in range(1, 8):
            horn += np.sin(2 * np.pi * f * h * t * (1 + 0.002 * np.sin(2 * np.pi * 5 * t))) / h
    e = np.minimum(1, t / 0.06) * np.minimum(1, (d - t) / 0.3)
    write('goal_horn', lp(horn, 2400, 3) * e, 0.85)

    d = 0.7
    sl = bp(noise(d), 400, 2500) * env(int(d * SR), 0.02, 0.25) * (0.7 + 0.3 * np.sin(2 * np.pi * 31 * T(d)))
    write('slide', sl + 0.3 * lp(noise(d), 300) * env(int(d * SR), 0.01, 0.2), 0.75)

    d = 1.0
    t = T(d)
    f = 200 * (8 ** (t / d))
    ph = 2 * np.pi * np.cumsum(f) / SR
    shimmer = np.sin(ph) * 0.5 + np.sin(ph * 1.5) * 0.25 + np.sin(ph * 2.01) * 0.2
    wh = bp(noise(d), 500, 6000) * (t / d) ** 2
    e = np.minimum(1, t / 0.05) * np.minimum(1, (d - t) / 0.08)
    write('super_charge', (shimmer + wh * 0.8) * e, 0.8)

    d = 0.3
    t = T(d)
    sk = bp(noise(d), 700, 4000) * np.sin(np.pi * t / d) ** 2
    write('skill', sk + 0.3 * thump(300, 160, d, 0.03), 0.6)

    write('tackle', thump(110, 60, 0.3, 0.05) + 0.6 * bp(noise(0.3), 500, 3000) * env(int(0.3 * SR), 0.002, 0.04), 0.8)
    write('catch', 0.9 * bp(noise(0.25), 1000, 6000) * env(int(0.25 * SR), 0.0005, 0.012) + thump(160, 90, 0.25, 0.04), 0.8)
    print('sounds done')


if __name__ == '__main__':
    main()
