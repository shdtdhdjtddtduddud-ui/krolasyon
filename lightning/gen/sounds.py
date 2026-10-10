"""Procedural electric sound design for the lightning skill tree -> mono .ogg (needs numpy, scipy, ffmpeg)."""
import os, subprocess, numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../src/main/resources/assets/stormtree/sounds')
os.makedirs(OUT, exist_ok=True)
R = np.random.default_rng(11)


def T(d): return np.arange(int(d * SR)) / SR
def noise(d): return R.standard_normal(int(d * SR))
def bp(x, lo, hi, o=2): return sosfilt(butter(o, [lo, hi], 'bandpass', fs=SR, output='sos'), x)
def lp(x, f, o=2): return sosfilt(butter(o, f, 'lowpass', fs=SR, output='sos'), x)
def hp(x, f, o=2): return sosfilt(butter(o, f, 'highpass', fs=SR, output='sos'), x)
def env(n, a, d): t = np.arange(n) / SR; return np.minimum(1, t / max(a, 1e-4)) * np.exp(-t / d)
def fit(x, n): return np.pad(x, (0, max(0, n - len(x))))[:n]


def crackle(d, density=900, decay=0.004, bright=2500):
    """random sparks: sparse impulses through short decays, high passed"""
    n = int(d * SR)
    x = np.zeros(n)
    k = R.random(n) < density / SR
    x[k] = R.uniform(-1, 1, k.sum()) * R.uniform(0.3, 1, k.sum())
    ker = np.exp(-np.arange(int(decay * 6 * SR)) / (decay * SR)) * R.standard_normal(int(decay * 6 * SR))
    y = np.convolve(x, ker)[:n]
    return hp(y, bright)


def buzz(d, f=60, harm=12):
    t = T(d)
    s = sum(np.sin(2 * np.pi * f * h * t + R.random() * 6) / h ** 0.7 for h in range(1, harm))
    return s * (1 + 0.5 * np.sign(np.sin(2 * np.pi * f * 2 * t)))


def sweep(d, f0, f1):
    t = T(d)
    f = f0 * (f1 / f0) ** (t / d)
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def norm(x, peak=0.89):
    x = x - np.mean(x)
    fade = min(len(x), int(0.01 * SR))
    x[-fade:] *= np.linspace(1, 0, fade)
    return x / (np.abs(x).max() + 1e-9) * peak


def save(name, x):
    raw = os.path.join(OUT, name + '.raw')
    (norm(x) * 32767).astype('<i2').tofile(raw)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', raw,
                    '-c:a', 'libvorbis', '-q:a', '5', os.path.join(OUT, name + '.ogg')], check=True)
    os.remove(raw)
    print('wrote', name)


for i in range(1, 4):
    d = 0.32 + 0.06 * i
    n = int(d * SR)
    x = crackle(d, 1600 + 300 * i, 0.003, 2000) * env(n, 0.002, 0.09)
    x += 0.5 * bp(noise(d), 1500, 7000) * env(n, 0.001, 0.025)
    x += 0.25 * buzz(d, 55 + 10 * i) * env(n, 0.003, 0.06)
    save('zap%d' % i, x)

d = 0.7; n = int(d * SR)
x = crackle(d, 2600, 0.004, 1800) * (0.6 + 0.4 * np.abs(np.sin(T(d) * 40))) * env(n, 0.005, 0.3)
x += 0.3 * buzz(d, 70) * env(n, 0.01, 0.25)
save('arc', x)

d = 1.0; n = int(d * SR); t = T(d)
rise = (t / d) ** 1.5
x = 0.6 * sweep(d, 180, 1400) * rise + 0.3 * sweep(d, 362, 2810) * rise
x += crackle(d, 400 + 3000 * 1, 0.003, 2500) * rise * 0.8
x += 0.2 * buzz(d, 50) * rise
x *= np.minimum(1, (d - t) / 0.03)
save('charge', x)

d = 2.0; n = int(d * SR); t = T(d)
boom = np.sin(2 * np.pi * (45 * t - 15 * t * t)) * env(n, 0.004, 0.35)
blast = lp(noise(d), 900) * env(n, 0.002, 0.25)
crack = bp(noise(d), 1200, 9000) * env(n, 0.001, 0.05)
tail = crackle(d, 1800, 0.004, 1500) * env(n, 0.05, 0.6)
save('nova', 1.4 * boom + 1.2 * blast + 0.8 * crack + 0.7 * tail)

d = 1.3; n = int(d * SR); t = T(d)
hum = sum(np.sin(2 * np.pi * f * t) for f in (110, 165, 220, 277)) * (1 - np.exp(-t / 0.15)) * np.exp(-np.maximum(0, t - 0.5) / 0.4)
shimmer = bp(noise(d), 3000, 9000) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t)) * env(n, 0.1, 0.5)
x = 0.4 * hum * (1 + 0.2 * np.sin(2 * np.pi * 6 * t)) + 0.4 * shimmer + 0.5 * crackle(d, 600, 0.003) * env(n, 0.05, 0.5)
save('dome', x)

d = 1.0; n = int(d * SR); t = T(d)
x = buzz(d, 90, 8) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t)) * env(n, 0.08, 0.5)
x += bp(noise(d), 400, 3000) * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * t)) * env(n, 0.1, 0.5) * 0.6
x += 0.6 * crackle(d, 900) * env(n, 0.02, 0.4)
save('ring', x)

d = 0.9; n = int(d * SR); t = T(d)
x = crackle(d, 2400, 0.004, 1500) * env(n, 0.002, 0.25)
x += 0.6 * buzz(d, 48, 14) * np.abs(np.sin(2 * np.pi * 5 * t)) * env(n, 0.02, 0.4)
x += 0.5 * bp(noise(d), 200, 900) * env(n, 0.003, 0.2)
save('grasp', x)

d = 1.4; n = int(d * SR); t = T(d)
x = sum(np.sin(2 * np.pi * f * t + np.sin(2 * np.pi * 4 * t) * 0.8) / k for k, f in enumerate((110, 220, 330, 440), 1))
x = x * (0.7 + 0.3 * np.sin(2 * np.pi * 3.3 * t)) * env(n, 0.1, 0.8) * 0.6 + 0.6 * crackle(d, 1100) * env(n, 0.05, 0.7)
save('ball', x)

d = 1.3; n = int(d * SR); t = T(d)
x = 1.2 * bp(noise(d), 1500, 10000) * env(n, 0.0005, 0.04)
x += 0.8 * sweep(d, 2400, 150) * env(n, 0.002, 0.18)
x += 0.9 * lp(noise(d), 300) * env(n, 0.01, 0.5)
x += 0.6 * crackle(d, 2500, 0.003, 1500) * env(n, 0.01, 0.4)
save('spear', x)

d = 1.6; n = int(d * SR); t = T(d)
x = np.zeros(n)
for k, (f, st) in enumerate([(523.25, 0), (659.25, 0.08), (783.99, 0.16), (1046.5, 0.24), (1318.5, 0.32)]):
    m = int(st * SR)
    tt = T(d - st)
    bell = (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * f * 2.76 * tt)) * np.exp(-tt / 0.45)
    x[m:] += bell * 0.5
x += 0.4 * crackle(d, 700, 0.002, 4000) * env(n, 0.2, 0.5)
x += 0.25 * sweep(d, 300, 2000) * env(n, 0.3, 0.3)
save('unlock', x)

d = 1.3; n = int(d * SR); t = T(d)
saw = sum(np.sign(np.sin(2 * np.pi * f * t)) for f in (73.4, 74.1, 110.0, 110.9))
x = lp(saw, 900) * env(n, 0.05, 0.5) * 0.5
x += bp(noise(d), 300, 1500) * np.exp(-((t - 0.3) / 0.25) ** 2) * 0.8
x += crackle(d, 1500) * env(n, 0.001, 0.2)
save('reaper', x)

d = 0.8; n = int(d * SR); t = T(d)
x = 1.2 * lp(noise(d), 1200) * env(n, 0.001, 0.08) + crackle(d, 3000, 0.003, 2000) * env(n, 0.002, 0.25) + 0.3 * buzz(d, 65) * env(n, 0.002, 0.15)
save('burst', x)
