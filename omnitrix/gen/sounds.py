"""Procedural sound design for the Omnitrix mod -> mono .ogg (needs numpy, scipy, ffmpeg)."""
import os, subprocess, numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../src/main/resources/assets/omnitrix/sounds')
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



def beep(f, d, dec=0.08):
    t = T(d)
    return (np.sign(np.sin(2 * np.pi * f * t)) * 0.4 + np.sin(2 * np.pi * f * t)) * np.exp(-t / dec)


def seq(parts):
    out = np.zeros(int(sum(p[1] for p in parts) * SR) + SR)
    pos = 0.0
    for x, dur in parts:
        i = int(pos * SR)
        out[i:i + len(x)] += x
        pos += dur
    return out[:int(pos * SR) + int(0.3 * SR)]


# --- watch
save('activate', seq([(beep(880, 0.12), 0.08), (beep(1320, 0.12), 0.08), (sweep(0.5, 400, 1800) * env(int(0.5 * SR), 0.01, 0.2) * 0.6 + crackle(0.5, 300, 0.002, 5000) * 0.2, 0.5)]))
save('dial', beep(1500, 0.07, 0.03) * 0.8 + lp(noise(0.07), 3000) * env(int(0.07 * SR), 0.001, 0.01))
save('select', seq([(lp(noise(0.08), 1500) * env(int(0.08 * SR), 0.001, 0.02) * 2, 0.06), (sweep(0.4, 300, 2500) * env(int(0.4 * SR), 0.01, 0.2), 0.4)]))
d = 1.8; n = int(d * SR); t = T(d)
chord = sum(np.sin(2 * np.pi * f * t) for f in (220, 277, 330, 440, 554)) / 5
save('transform', 0.9 * sweep(d, 120, 2600) * (t / d) ** 0.6 * np.exp(-np.maximum(0, t - 1.1) / 0.25) + 0.5 * whoosh(d, 200, 4000) * env(n, 0.3, 0.6)
     + 0.7 * chord * np.exp(-np.maximum(0, t - 1.1) / 0.4) * (t > 1.0) + 1.2 * lp(noise(d), 400) * env(n, 0.002, 0.15) * (t > 1.05))
save('revert', 0.8 * sweep(1.2, 2200, 150) * env(int(1.2 * SR), 0.01, 0.5) + 0.4 * whoosh(1.2, 3000, 200) * env(int(1.2 * SR), 0.05, 0.4))
save('timeout', seq([(beep(1200, 0.15), 0.15), (beep(900, 0.15), 0.15), (beep(600, 0.3, 0.15), 0.3), (sweep(0.8, 800, 80) * env(int(0.8 * SR), 0.01, 0.4), 0.8)]))
save('beep', beep(1400, 0.12, 0.05))
# --- heatblast
save('fire_shot', whoosh(0.6, 2500, 300) * env(int(0.6 * SR), 0.005, 0.2) + 0.8 * lp(noise(0.6), 900) * env(int(0.6 * SR), 0.01, 0.25))
d = 1.6; n = int(d * SR)
save('flame', lp(noise(d), 1800) * (0.7 + 0.3 * np.sin(2 * np.pi * 7 * T(d))) * env(n, 0.05, 1.0) + 0.4 * crackle(d, 500, 0.003, 1500) * env(n, 0.05, 1.0))
d = 2.0; n = int(d * SR); t = T(d)
save('supernova', 1.3 * np.sin(2 * np.pi * (45 * t - 10 * t * t)) * env(n, 0.003, 0.4) + 1.2 * lp(noise(d), 1000) * env(n, 0.002, 0.5) + 0.6 * crackle(d, 800, 0.004, 1200) * env(n, 0.05, 0.8))
save('jet', lp(noise(1.2), 1200) * env(int(1.2 * SR), 0.02, 0.5) * 1.2 + 0.4 * sweep(1.2, 200, 700) * env(int(1.2 * SR), 0.02, 0.5))
# --- terraspin
save('wind', whoosh(1.0, 300, 1500, 0.9) * env(int(1.0 * SR), 0.05, 0.4) * 1.3)
save('tornado', whoosh(2.0, 200, 600, 1.0) * (0.6 + 0.4 * np.sin(2 * np.pi * 4 * T(2.0))) * env(int(2.0 * SR), 0.2, 1.0) * 1.3)
# --- diamondhead
d = 0.8; n = int(d * SR); t = T(d)
save('crystal', sum(np.sin(2 * np.pi * f * t) * np.exp(-t / (0.25 + k * 0.05)) for k, f in enumerate((1568, 2093, 2637, 3136))) * 0.4 + 0.5 * hp(noise(d), 6000) * env(n, 0.001, 0.04))
save('shatter', crackle(0.7, 4000, 0.002, 3000) * env(int(0.7 * SR), 0.001, 0.2) + 0.5 * hp(noise(0.7), 4000) * env(int(0.7 * SR), 0.001, 0.08))
# --- goop
d = 0.5; n = int(d * SR); t = T(d)
save('slime_shot', np.sin(2 * np.pi * np.cumsum(300 + 400 * np.exp(-t / 0.05)) / SR) * env(n, 0.003, 0.12) + 0.4 * lp(noise(d), 800) * env(n, 0.002, 0.08))
save('splat', lp(noise(0.6), 700) * env(int(0.6 * SR), 0.002, 0.12) * 1.5 + 0.5 * np.sin(2 * np.pi * np.cumsum(150 + 300 * np.exp(-T(0.6) / 0.03)) / SR) * env(int(0.6 * SR), 0.002, 0.15))
# --- xlr8
save('zoom', whoosh(0.7, 600, 5000, 0.4) * env(int(0.7 * SR), 0.01, 0.15) * 1.4 + 0.3 * sweep(0.7, 1500, 300) * env(int(0.7 * SR), 0.005, 0.1))
# --- four arms
save('clap', 1.6 * hp(noise(1.0), 800) * env(int(1.0 * SR), 0.0005, 0.03) + 1.0 * np.sin(2 * np.pi * (70 * T(1.0))) * env(int(1.0 * SR), 0.002, 0.3) + 0.6 * lp(noise(1.0), 300) * env(int(1.0 * SR), 0.01, 0.5))
d = 1.6; n = int(d * SR); t = T(d)
save('slam', 1.5 * np.sin(2 * np.pi * (40 * t - 8 * t * t)) * env(n, 0.002, 0.4) + 1.2 * lp(noise(d), 500) * env(n, 0.001, 0.35) + 0.5 * crackle(d, 600, 0.004, 800) * env(n, 0.05, 0.6))
save('punch', 1.2 * lp(noise(0.3), 1200) * env(int(0.3 * SR), 0.001, 0.04) + np.sin(2 * np.pi * 90 * T(0.3)) * env(int(0.3 * SR), 0.001, 0.08))
# --- big chill
save('freeze', hp(noise(1.4), 3000) * env(int(1.4 * SR), 0.05, 0.6) * 0.8 + 0.5 * crackle(1.4, 1200, 0.002, 4000) * env(int(1.4 * SR), 0.1, 0.6) + 0.3 * pad(1.4, [880, 1175]) * env(int(1.4 * SR), 0.1, 0.5))
save('phase', 0.6 * pad(1.0, [220, 330, 415]) * np.sin(2 * np.pi * 6 * T(1.0)) * env(int(1.0 * SR), 0.05, 0.4) + whoosh(1.0, 2500, 400) * env(int(1.0 * SR), 0.02, 0.3) * 0.5)
# --- ultimate echo echo
d = 0.8; n = int(d * SR); t = T(d)
save('sonic', np.sin(2 * np.pi * 1200 * t) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 30 * t))) * env(n, 0.005, 0.25) * 0.6 + 0.3 * sweep(d, 600, 2400) * env(n, 0.005, 0.2))
d = 1.6; n = int(d * SR); t = T(d)
save('scream', sum(np.sin(2 * np.pi * f * t + np.sin(2 * np.pi * 40 * t) * 3) for f in (600, 900, 1350)) / 3 * env(n, 0.03, 0.7) + 0.5 * bp(noise(d), 800, 5000) * env(n, 0.02, 0.6))
# --- ghostfreak
d = 1.5; n = int(d * SR); t = T(d)
save('ghost', 0.6 * np.sin(2 * np.pi * (300 + 100 * np.sin(2 * np.pi * 3 * t)) * t) * env(n, 0.2, 0.7) + 0.4 * whoosh(d, 1500, 300) * env(n, 0.1, 0.6))
d = 2.0; n = int(d * SR); t = T(d)
saw = sum(np.sign(np.sin(2 * np.pi * f * t)) for f in (55, 55.6, 82.4, 83.1))
save('terror', lp(saw, 700) * env(n, 0.1, 0.9) * 0.5 + 0.6 * whoosh(d, 3000, 100) * env(n, 0.02, 0.8) + 0.5 * np.sin(2 * np.pi * (300 + 600 * t) * t) * env(n, 0.3, 0.5))
save('block', 0.8 * lp(noise(0.4), 2500) * env(int(0.4 * SR), 0.001, 0.05) + 0.5 * pad(0.4, [330, 495]) * env(int(0.4 * SR), 0.005, 0.15))
