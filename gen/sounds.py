# -*- coding: utf-8 -*-
"""Procedural sound design (numpy only) -> mono .ogg via ffmpeg."""
import os, wave, subprocess, json
import numpy as np

SR = 44100
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
OUT = os.path.join(ROOT, 'src/main/resources/assets/sololeveling/sounds')
R = np.random.default_rng(11)


def T(d):
    return np.arange(int(d * SR)) / SR


def env(n, a=0.01, r=0.1):
    e = np.ones(n)
    na, nr = int(a * SR), int(r * SR)
    if na > 0:
        e[:na] = np.linspace(0, 1, na)
    if nr > 0:
        e[-nr:] *= np.linspace(1, 0, nr)
    return e


def fft_band(x, lo, hi):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    m = ((f >= lo) & (f <= hi)).astype(float)
    # smooth edges
    k = 40
    m = np.convolve(m, np.ones(k) / k, 'same')
    return np.fft.irfft(X * m, len(x))


def noise(d):
    return R.standard_normal(int(d * SR))


def bell(freq, d, decay=4.0, vel=1.0):
    t = T(d)
    parts = [(1, 1.0), (2.76, 0.45), (5.4, 0.22), (8.93, 0.1), (0.5, 0.35)]
    x = sum(w * np.sin(2 * np.pi * freq * p * t) * np.exp(-t * decay * (0.6 + 0.4 * p)) for p, w in parts)
    return x * vel * env(len(t), 0.002, 0.05)


def reverb(x, tail=1.2, wet=0.3, seed=3):
    r = np.random.default_rng(seed)
    n = int(tail * SR)
    ir = r.standard_normal(n) * np.exp(-np.arange(n) / SR * (6.0 / tail))
    ir = fft_band(ir, 200, 9000)
    ir /= np.abs(ir).max()
    L = len(x) + n
    y = np.fft.irfft(np.fft.rfft(x, L) * np.fft.rfft(ir, L), L)
    y /= max(1e-6, np.abs(y).max()) / max(1e-6, np.abs(x).max())
    out = np.zeros(L)
    out[:len(x)] += x * (1 - wet)
    out += y * wet
    return out


def mix(*parts, n=None):
    n = n or max(len(p[0]) + int(p[1] * SR) for p in parts)
    out = np.zeros(n)
    for x, at in parts:
        s = int(at * SR)
        e = min(n, s + len(x))
        out[s:e] += x[:e - s]
    return out


def norm(x, peak=0.8):
    return x / max(1e-6, np.abs(x).max()) * peak


def save(name, x):
    x = norm(x)
    # trim trailing silence
    nz = np.nonzero(np.abs(x) > 0.002)[0]
    if len(nz):
        x = x[:nz[-1] + 1]
    x = x * env(len(x), 0.002, 0.03)
    wav = f'/tmp/{name}.wav'
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype('<i2').tobytes())
    os.makedirs(OUT, exist_ok=True)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '4', os.path.join(OUT, name + '.ogg')], check=True)


def whoosh(d, f0, f1, q=0.5, vol=1.0):
    n = noise(d)
    out = np.zeros_like(n)
    blocks = 40
    L = len(n) // blocks
    for b in range(blocks):
        c = f0 * (f1 / f0) ** (b / (blocks - 1))
        seg = fft_band(n[b * L:(b + 1) * L], c * (1 - q), c * (1 + q))
        out[b * L:(b + 1) * L] = seg
    out *= np.sin(np.linspace(0, np.pi, len(out))) ** 1.5
    return out * vol


def build():
    # --- system chime (UI notification)
    x = mix((bell(1318.5, 0.9, 5), 0), (bell(1760, 0.9, 5, 0.7), 0.09), (bell(2217, 0.7, 6, 0.4), 0.18))
    save('system_chime', reverb(x, 0.9, 0.25))
    # --- quest
    x = mix((bell(784, 0.8, 4), 0), (bell(1174.7, 1.0, 4, 0.9), 0.13))
    save('quest', reverb(x, 0.8, 0.22))
    # --- warning
    t = T(0.18)
    b = np.sign(np.sin(2 * np.pi * 233 * t)) * 0.5 + np.sin(2 * np.pi * 233 * t) * 0.5
    b = b * env(len(t), 0.005, 0.06)
    save('warning', mix((b, 0), (b, 0.22)))
    # --- level up
    notes = [523.25, 659.25, 783.99, 1046.5, 1318.5]
    parts = [(bell(f, 1.2, 3.5), i * 0.085) for i, f in enumerate(notes)]
    t = T(1.8)
    pad = sum(np.sin(2 * np.pi * f * t + p) for f, p in ((261.6, 0), (329.6, 1), (392, 2), (523.25, 3))) * np.sin(np.linspace(0, np.pi, len(t))) ** 2 * 0.25
    spark = fft_band(noise(1.4), 6000, 14000) * np.exp(-T(1.4) * 3) * 0.15
    save('level_up', reverb(mix(*parts, (pad, 0.35), (spark, 0.3)), 1.6, 0.35))
    # --- ARISE
    d = 3.6
    t = T(d)
    drone = np.sin(2 * np.pi * 41 * t) * 0.9 + np.sin(2 * np.pi * (82 + 3 * np.sin(t * 2)) * t) * 0.5
    drone *= np.minimum(1, t / 1.2) * np.exp(-np.maximum(0, t - 2.2) * 0.9)
    sw = whoosh(1.7, 150, 3500, 0.6, 0.9)
    sw *= np.linspace(0.1, 1, len(sw)) ** 2
    ti = T(1.8)
    kick = np.sin(2 * np.pi * (35 + 110 * np.exp(-ti * 9)) * ti) * np.exp(-ti * 2.2)
    burst = fft_band(noise(1.8), 60, 1800) * np.exp(-ti * 6) * 0.8
    pad = np.zeros(int(2.4 * SR))
    tp = T(2.4)
    for f in (110, 164.8, 220, 277.2, 329.6, 440):
        for dt in (-0.4, 0.0, 0.5):
            ph = R.random() * 6
            pad += sum(np.sin(2 * np.pi * (f + dt) * h * tp + ph) / h ** 1.4 for h in (1, 2, 3, 4)) * 0.12
    pad *= np.sin(np.linspace(0, np.pi, len(tp))) ** 1.3
    pad = fft_band(pad, 80, 2800)
    whisper = fft_band(noise(2.0), 2500, 6500) * np.sin(np.linspace(0, np.pi, int(2.0 * SR))) ** 2 * 0.12
    x = mix((drone, 0), (sw, 0.0), (kick, 1.65), (burst, 1.65), (pad, 1.7), (whisper, 0.4))
    save('arise', reverb(x, 2.4, 0.4))
    # --- gate hum (ambient loop-ish)
    d = 4.0
    t = T(d)
    base = sum(np.sin(2 * np.pi * f * t + p) * a for f, p, a in ((70, 0, 1), (105.3, 1, 0.6), (140.2, 2, 0.35), (211, 3, 0.2)))
    base *= 0.6 + 0.4 * np.sin(2 * np.pi * 0.5 * t)
    sh = fft_band(noise(d), 400, 1100) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.35 * t + 1)) * 0.5
    hi = fft_band(noise(d), 4000, 9000) * (0.5 + 0.5 * np.sin(2 * np.pi * 1.1 * t)) * 0.06
    x = (base * 0.5 + sh + hi) * env(len(t), 0.4, 0.5)
    save('gate_hum', x)
    # --- gate enter whoosh
    d = 1.7
    t = T(d)
    w = whoosh(d, 180, 5000, 0.55, 1.0)
    sine = np.sin(2 * np.pi * np.cumsum(120 + 900 * (t / d) ** 2) / SR) * 0.35 * np.sin(np.linspace(0, np.pi, len(t)))
    save('gate_enter', reverb(w + sine, 1.2, 0.3))
    # --- shadow step
    w = whoosh(0.55, 2500, 300, 0.7, 1.0)
    thump = np.sin(2 * np.pi * (60 + 80 * np.exp(-T(0.3) * 14)) * T(0.3)) * np.exp(-T(0.3) * 9) * 0.7
    save('shadow_step', mix((w, 0), (thump, 0.3)))
    # --- skill cast zap
    t = T(0.7)
    zap = np.sin(2 * np.pi * np.cumsum(1400 * np.exp(-t * 4) + 200) / SR) * np.exp(-t * 5)
    zap += fft_band(noise(0.7), 1500, 9000) * np.exp(-t * 9) * 0.5
    save('skill_cast', reverb(zap, 0.6, 0.25))
    # --- dungeon break alarm
    t = T(1.4)
    sirens = np.sin(2 * np.pi * np.cumsum(520 + 160 * np.sign(np.sin(2 * np.pi * 2.2 * t))) / SR) * 0.6
    sirens += np.sign(np.sin(2 * np.pi * np.cumsum(520 + 160 * np.sign(np.sin(2 * np.pi * 2.2 * t))) / SR)) * 0.15
    save('alarm', sirens * env(len(t), 0.02, 0.2))
    names = ['system_chime', 'quest', 'warning', 'level_up', 'arise', 'gate_hum', 'gate_enter', 'shadow_step', 'skill_cast', 'alarm']
    sj = {n: {'sounds': [{'name': f'sololeveling:{n}', 'stream': n in ('gate_hum',)}], 'subtitle': f'subtitles.sololeveling.{n}'} for n in names}
    with open(os.path.join(ROOT, 'src/main/resources/assets/sololeveling/sounds.json'), 'w') as f:
        json.dump(sj, f, indent=1)
    return names


if __name__ == '__main__':
    print(build())
