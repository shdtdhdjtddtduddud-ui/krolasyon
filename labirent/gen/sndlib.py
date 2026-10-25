"""Procedural sound helpers -> mono .ogg files."""
import os, subprocess, json, tempfile, numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/labirent/sounds')
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
    wav = os.path.join(tempfile.gettempdir(), f'{name}.wav')
    import wave
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', '-ac', '1',
                    os.path.join(OUT, f'{name}.ogg')], check=True)


