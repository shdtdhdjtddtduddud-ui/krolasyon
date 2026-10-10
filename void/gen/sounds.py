"""Procedural void/shadow sound design for the void skill tree -> mono .ogg (needs numpy, scipy, ffmpeg)."""
import os, subprocess, numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../src/main/resources/assets/voidtree/sounds')
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



def whoosh(d, f0, f1, q=0.6):
    """noise swept through a moving band (block processed)"""
    n = int(d * SR); x = noise(d); out = np.zeros(n); blk = 1024
    for i in range(0, n, blk):
        f = f0 * (f1 / f0) ** (i / n)
        seg = x[max(0, i - 2048):i + blk]
        y = bp(seg, max(40, f * (1 - q / 2)), min(SR / 2 - 100, f * (1 + q / 2)))
        out[i:i + blk] = y[-len(out[i:i + blk]):]
    return out


def pad(d, freqs, detune=0.4):
    t = T(d)
    return sum(np.sin(2 * np.pi * (f + detune * k) * t + k) for k, f in enumerate(freqs)) / len(freqs)


d = 0.6; n = int(d * SR)
save('whoosh', whoosh(d, 300, 2500) * np.sin(np.pi * np.arange(n) / n) ** 1.5 + 0.3 * lp(noise(d), 200) * env(n, 0.01, 0.15))
d = 1.3; n = int(d * SR); t = T(d)
save('phantom', 0.5 * pad(d, [220, 330, 415]) * np.sin(2 * np.pi * 5 * t) * env(n, 0.15, 0.5) + whoosh(d, 2000, 400) * env(n, 0.05, 0.4) * 0.6)
d = 1.6; n = int(d * SR); t = T(d)
save('vortex', whoosh(d, 150, 600, 0.9) * (0.6 + 0.4 * np.sin(2 * np.pi * 3 * t)) * env(n, 0.2, 0.9) + 0.4 * lp(noise(d), 120) * env(n, 0.1, 0.8))
d = 0.45; n = int(d * SR)
save('slash', whoosh(d, 4000, 900, 0.5) * env(n, 0.005, 0.12) + 0.5 * hp(noise(d), 5000) * env(n, 0.001, 0.03))
d = 1.2; n = int(d * SR); t = T(d)
save('prison', 0.6 * pad(d, [110, 164.8, 233]) * env(n, 0.02, 0.6) + 0.5 * sweep(d, 1200, 300) * env(n, 0.005, 0.3) + 0.3 * crackle(d, 500, 0.003, 3000) * env(n, 0.05, 0.5))
d = 0.6; n = int(d * SR)
save('blade', whoosh(d, 3000, 500) * env(n, 0.01, 0.15) + 0.8 * lp(noise(d), 600) * env(n, 0.002, 0.08))
d = 2.2; n = int(d * SR); t = T(d)
save('hole', 0.9 * np.sin(2 * np.pi * (38 + 6 * np.sin(2 * np.pi * 0.7 * t)) * t) * env(n, 0.3, 1.2) + whoosh(d, 800, 100, 1.0) * env(n, 0.4, 1.0) * 0.7)
d = 2.0; n = int(d * SR); t = T(d)
save('implode', 1.2 * sweep(d, 900, 40) * env(n, 0.002, 0.25) + 1.3 * np.sin(2 * np.pi * (50 * t - 12 * t * t)) * env(n, 0.01, 0.6) + lp(noise(d), 700) * env(n, 0.003, 0.4))
d = 0.9; n = int(d * SR); t = T(d)
save('rift', 0.7 * sweep(d, 200, 1600) * env(n, 0.005, 0.25) + crackle(d, 1500, 0.003, 2500) * env(n, 0.002, 0.3) + 0.4 * whoosh(d, 600, 3000) * env(n, 0.01, 0.3))
d = 2.0; n = int(d * SR); t = T(d)
ch = pad(d, [261.6, 329.6, 392, 523.3], 0.6) * (1 - np.exp(-t / 0.3)) * np.exp(-np.maximum(0, t - 1.0) / 0.5)
save('ascend', 0.8 * ch + 0.3 * sweep(d, 300, 2400) * env(n, 0.4, 0.6) + 0.3 * crackle(d, 600, 0.002, 5000) * env(n, 0.3, 0.8))
d = 1.6; n = int(d * SR); t = T(d)
save('seal', 0.8 * pad(d, [73.4, 110, 146.8]) * env(n, 0.05, 0.9) + 0.5 * np.sin(2 * np.pi * 880 * t) * np.exp(-t / 0.3) + 0.3 * np.sin(2 * np.pi * 1318 * t) * np.exp(-t / 0.25))
d = 1.5; n = int(d * SR); t = T(d)
save('summon', 0.6 * pad(d, [98, 146.8, 196], 1.2) * env(n, 0.1, 0.7) + whoosh(d, 3000, 200) * env(n, 0.01, 0.5) * 0.7 + 0.3 * lp(noise(d), 150) * env(n, 0.05, 0.6))
d = 3.0; n = int(d * SR); t = T(d)
twinkle = sum(np.sin(2 * np.pi * f * t) * np.exp(-((t - st) / 0.08) ** 2) for f, st in [(1568, 0.2), (2093, 0.5), (1760, 0.8), (2637, 1.1), (1975, 1.4), (2349, 1.7)])
save('cosmic', 0.7 * pad(d, [55, 82.4, 110, 164.8], 0.5) * env(n, 0.6, 1.6) + 0.4 * twinkle + 0.5 * whoosh(d, 100, 900, 0.8) * env(n, 1.0, 1.2))
d = 1.6; n = int(d * SR)
x = np.zeros(n)
for k, (f, st) in enumerate([(392, 0), (466.2, 0.09), (587.3, 0.18), (740, 0.27), (932.3, 0.36)]):
    m = int(st * SR); tt = T(d - st)
    x[m:] += (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * f * 2.76 * tt)) * np.exp(-tt / 0.5) * 0.5
save('unlock', x + 0.3 * whoosh(d, 400, 3000) * env(n, 0.3, 0.4))
d = 0.5; n = int(d * SR)
save('evade', whoosh(d, 2500, 600) * env(n, 0.005, 0.12) + 0.3 * pad(d, [440, 660]) * env(n, 0.01, 0.1))
