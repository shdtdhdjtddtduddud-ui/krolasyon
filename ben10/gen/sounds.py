"""Synthesised Omnitrix sounds -> mono .ogg (ffmpeg/libvorbis)."""
import os, subprocess, json, numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolben10/sounds')
os.makedirs(OUT, exist_ok=True)
R = np.random.default_rng(10)


def T(d):
    return np.arange(int(d * SR)) / SR


def noise(d):
    return R.standard_normal(int(d * SR))


def bp(x, lo, hi):
    return sosfilt(butter(2, [lo, hi], 'bandpass', fs=SR, output='sos'), x)


def lp(x, f):
    return sosfilt(butter(2, f, 'lowpass', fs=SR, output='sos'), x)


def env(t, a, d):
    return np.minimum(1, t / max(a, 1e-4)) * np.exp(-np.maximum(0, t - a) / d)


def chirp(t, f0, f1):
    f = f0 * (f1 / f0) ** (t / t[-1])
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def write(name, x):
    x = x / (np.abs(x).max() + 1e-9) * 0.9
    raw = (x * 32767).astype('<i2').tobytes()
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', '-', '-c:a', 'libvorbis', '-q:a', '5',
                    os.path.join(OUT, name + '.ogg')], input=raw, check=True)


def pad(x, n):
    return np.pad(x, (0, max(0, n - len(x))))[:n]


# transform: charging whine up, slam thump, bright green "shing" with shimmer, DNA-rewrite whoosh
t = T(1.6)
whine = chirp(t, 300, 2400) * np.clip(t / 0.45, 0, 1) * (t < 0.5) * 0.5
slam = np.zeros_like(t)
k = int(0.5 * SR)
th = T(0.4)
slam[k:k + len(th)] += (np.sin(2 * np.pi * 70 * th) * np.exp(-th * 14) + lp(noise(0.4), 600) * 0.6 * np.exp(-th * 20))
sh = T(1.1)
shing = sum(np.sin(2 * np.pi * f * sh + R.random() * 6) * a for f, a in ((1320, 0.5), (1980, 0.35), (2640, 0.25), (3960, 0.15))) * env(sh, 0.005, 0.35)
shing *= 1 + 0.3 * np.sin(2 * np.pi * 9 * sh)
slam[k:k + len(sh)] += shing * 0.7
wh = bp(noise(1.1), 400, 3000) * np.sin(np.pi * np.clip(sh / 1.1, 0, 1)) ** 2 * 0.5
slam[k:k + len(sh)] += wh
write('transform', whine + slam)

# detransform: reverse shimmer + descending tone
t = T(1.0)
x = chirp(t, 1800, 220) * env(t, 0.02, 0.4) * 0.6 + bp(noise(1.0), 500, 2500) * np.exp(-t * 3) * 0.4
x += sum(np.sin(2 * np.pi * f * t) * a for f, a in ((990, 0.3), (1485, 0.2))) * np.exp(-t * 5)
write('detransform', x)

# timeout: three warning beeps then power-down
parts = []
for i in range(3):
    bt = T(0.12)
    parts.append(np.sin(2 * np.pi * 1760 * bt) * env(bt, 0.003, 0.05) * 0.7)
    parts.append(np.zeros(int(0.06 * SR)))
t = T(0.9)
parts.append(chirp(t, 900, 60) * np.exp(-t * 2.5) + lp(noise(0.9), 300) * np.exp(-t * 4) * 0.4)
write('timeout', np.concatenate(parts))

# beep
t = T(0.16)
write('beep', np.sin(2 * np.pi * 1568 * t) * env(t, 0.003, 0.06) + np.sin(2 * np.pi * 3136 * t) * env(t, 0.003, 0.03) * 0.3)

# dial click: mechanical ratchet
t = T(0.09)
x = bp(noise(0.09), 2000, 7000) * np.exp(-t * 90) + np.sin(2 * np.pi * 950 * t) * np.exp(-t * 60) * 0.4
write('dial', x)

# dial open: twist + rising hum
t = T(0.7)
x = bp(noise(0.7), 1500, 5000) * np.exp(-t * 18) * 0.6
x += chirp(t, 180, 520) * env(t, 0.1, 0.4) * 0.5 + np.sin(2 * np.pi * 1040 * t) * env(t, 0.25, 0.25) * 0.25
write('dial_open', x)

# sonic pulse: warbling ring-mod tone
t = T(0.5)
car = np.sin(2 * np.pi * (900 + 400 * np.sin(2 * np.pi * 14 * t)) * t)
x = car * np.sin(2 * np.pi * 63 * t) * env(t, 0.01, 0.18) + bp(noise(0.5), 1500, 4000) * env(t, 0.005, 0.05) * 0.4
write('sonic', x)

# speed whoosh
t = T(0.6)
x = bp(noise(0.6), 300, 2500) * np.sin(np.pi * t / 0.6) ** 3
x = np.concatenate([x[: len(x) // 3], x[len(x) // 3:] * np.linspace(1, 0.4, len(x) - len(x) // 3)])
write('whoosh', x)

sounds = {n: {"sounds": [f"krolben10:{n}"], "subtitle": f"subtitle.krolben10.{n}"} for n in
          ('transform', 'detransform', 'timeout', 'beep', 'dial', 'dial_open', 'sonic', 'whoosh')}
with open(os.path.join(OUT, '..', 'sounds.json'), 'w') as f:
    json.dump(sounds, f, indent=2)
print('sounds ok')
