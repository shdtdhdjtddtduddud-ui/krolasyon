"""Prosedurel ses tasarimi -> mono .ogg (ffmpeg/libvorbis)."""
import os, json, subprocess, tempfile, wave
import numpy as np
from scipy.signal import butter, sosfilt, fftconvolve

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/rabonaarena/sounds')
SJ = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/rabonaarena/sounds.json')
os.makedirs(OUT, exist_ok=True)
R = np.random.default_rng(1907)


def T(d): return np.arange(int(d * SR)) / SR
def noise(d): return R.standard_normal(int(d * SR))
def bp(x, lo, hi, o=2): return sosfilt(butter(o, [lo, hi], 'bandpass', fs=SR, output='sos'), x)
def lp(x, f, o=2): return sosfilt(butter(o, f, 'lowpass', fs=SR, output='sos'), x)
def hp(x, f, o=2): return sosfilt(butter(o, f, 'highpass', fs=SR, output='sos'), x)
def env(n, a, d): t = np.arange(n) / SR; return np.minimum(1, t / max(a, 1e-4)) * np.exp(-np.maximum(0, t - a) / d)


def pad(x, n):
    return np.pad(x, (0, max(0, n - len(x))))[:n]


def mix(*xs):
    n = max(len(x) for x in xs)
    return sum(pad(x, n) for x in xs)


def reverb(x, size=0.6, wet=0.25):
    ir = noise(size) * np.exp(-T(size) / (size / 5))
    ir = lp(ir, 5000)
    y = fftconvolve(x, ir)[:len(x) + int(size * SR * 0.6)]
    y = y / (np.abs(y).max() + 1e-9) * np.abs(x).max()
    return mix(x * (1 - wet), y * wet)


def sweep_noise(d, f0, f1, q=0.3, blocks=48):
    x = noise(d)
    n = len(x)
    out = np.zeros(n)
    edges = np.linspace(0, n, blocks + 1).astype(int)
    for i in range(blocks):
        f = f0 + (f1 - f0) * i / blocks
        y = bp(x, max(40, f * (1 - q)), min(SR / 2 - 100, f * (1 + q)))
        out[edges[i]:edges[i + 1]] = y[edges[i]:edges[i + 1]]
    return out


def thump(f, d, k=1.0):
    t = T(d)
    fr = f * (1 + 1.5 * np.exp(-t * 60))
    ph = 2 * np.pi * np.cumsum(fr) / SR
    return np.sin(ph) * np.exp(-t * 18 / d * 0.15 * 6) * k


def slap(d=0.03, lo=900, hi=5000):
    return bp(noise(d), lo, hi) * env(int(d * SR), 0.0005, d / 4)


def save(name, x, gain=0.9):
    x = np.asarray(x, float)
    x = x / (np.abs(x).max() + 1e-9) * gain
    fade = min(len(x), int(0.01 * SR))
    x[-fade:] *= np.linspace(1, 0, fade)
    pcm = (x * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tf:
        with wave.open(tf.name, 'wb') as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', tf.name, '-c:a', 'libvorbis', '-q:a', '4',
                        os.path.join(OUT, name + '.ogg')], check=True)
        os.unlink(tf.name)


def voices(d, n, f_lo, f_hi, vowel='a', rise=0.0, vib=5.0):
    """Kalabalik sesleri: formantli harmonik sesler."""
    t = T(d)
    out = np.zeros(len(t))
    form = {'a': [(700, 1100), (1100, 1600)], 'o': [(350, 650), (700, 1000)], 'e': [(450, 700), (1800, 2400)]}[vowel]
    for i in range(n):
        f0 = R.uniform(f_lo, f_hi)
        st = R.uniform(0, d * 0.3)
        fr = f0 * (1 + rise * np.clip((t - st) / d, 0, 1)) * (1 + 0.012 * np.sin(2 * np.pi * R.uniform(min(3, vib), vib) * t + R.uniform(0, 6)))
        ph = 2 * np.pi * np.cumsum(fr) / SR
        sig = sum(np.sin(k * ph) / k for k in range(1, 12))
        e = np.clip((t - st) / 0.25, 0, 1) * np.clip((d - t) / 0.6, 0, 1) * R.uniform(0.4, 1)
        out += sig * e
    y = sum(bp(out, lo, hi) for lo, hi in form)
    return y + 0.3 * lp(out, 500)


def main():
    # ---- top
    save('kick_soft', mix(thump(95, 0.16, 1.0), slap(0.025) * 0.7))
    save('kick_hard', mix(thump(75, 0.25, 1.2), slap(0.035, 700, 4500) * 1.1))
    save('kick_power', mix(thump(60, 0.3, 1.4), slap(0.04, 600, 5000) * 1.3,
                           np.pad(sweep_noise(0.7, 3000, 500, 0.4) * env(int(0.7 * SR), 0.05, 0.25) * 0.8, (int(0.02 * SR), 0))))
    save('touch', mix(thump(140, 0.08, 0.6), slap(0.015, 1200, 4000) * 0.4), 0.6)
    save('bounce', mix(thump(120, 0.12, 1.0), slap(0.02, 800, 3000) * 0.4), 0.7)
    t = T(1.6)
    ring = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t * dcy) for f, a, dcy in
               [(523, 1, 2.2), (1347, 0.6, 3.1), (2211, 0.45, 4.0), (3412, 0.3, 5.5), (4870, 0.15, 7)])
    save('post', reverb(mix(ring, slap(0.02, 1500, 8000) * 2, thump(180, 0.1, 0.8)), 0.8, 0.3))
    sw = bp(noise(0.7), 1800, 7000) * env(int(0.7 * SR), 0.02, 0.18)
    rust = bp(noise(0.7), 400, 1500) * env(int(0.7 * SR), 0.05, 0.25) * 0.5
    save('net', mix(sw, rust, thump(90, 0.12, 0.5)), 0.8)
    # ---- duduk
    def whistle(d):
        tt = T(d)
        f = 2950 + 160 * np.sign(np.sin(2 * np.pi * 32 * tt)) * 0.5 + 60 * np.sin(2 * np.pi * 32 * tt)
        ph = 2 * np.pi * np.cumsum(f) / SR
        s = np.sin(ph) + 0.25 * np.sin(2 * ph) + 0.1 * bp(noise(d), 2500, 4000)
        return s * np.clip(tt / 0.02, 0, 1) * np.clip((d - tt) / 0.04, 0, 1)
    save('whistle', reverb(whistle(0.55), 0.7, 0.2), 0.85)
    gap = np.zeros(int(0.12 * SR))
    save('whistle_end', reverb(np.concatenate([whistle(0.35), gap, whistle(0.35), gap, whistle(1.1)]), 0.9, 0.25), 0.85)
    # ---- tribun
    d = 5.0
    roar = voices(d, 70, 180, 380, 'a', rise=0.25)
    crowd_noise = bp(noise(d), 300, 3500) * 0.5
    swell = np.clip(T(d) / 0.6, 0, 1) * np.clip((d - T(d)) / 1.8, 0, 1)
    claps = np.zeros(len(T(d)))
    for _ in range(400):
        p = int(R.uniform(0.5, d - 0.3) * SR)
        c = slap(0.012, 1000, 6000) * R.uniform(0.2, 0.6)
        claps[p:p + len(c)] += c[:len(claps) - p]
    save('crowd_goal', reverb(mix(roar * swell, crowd_noise * swell, claps * 2), 1.2, 0.35), 0.95)
    d = 1.8
    ooh = voices(d, 50, 150, 300, 'o', rise=-0.2)
    save('crowd_ooh', reverb(ooh * np.clip(T(d) / 0.3, 0, 1) * np.clip((d - T(d)) / 0.7, 0, 1), 1.0, 0.35), 0.8)
    d = 6.0
    chant = voices(d, 45, 160, 260, 'e', rise=0.0, vib=3)
    beat = np.zeros(len(T(d)))
    for k in range(int(d / 0.5)):  # ritmik alkis: tak tak tak - tak tak
        if k % 4 == 3:
            continue
        p = int(k * 0.5 * SR)
        for _ in range(25):
            c = slap(0.015, 900, 5000) * R.uniform(0.3, 0.7)
            q = p + int(R.uniform(0, 0.03) * SR)
            beat[q:q + len(c)] += c[:max(0, len(beat) - q)]
    amb = mix(chant * 0.6 * (0.7 + 0.3 * np.sin(2 * np.pi * 0.5 * T(d))), bp(noise(d), 200, 2500) * 0.4, beat * 1.5)
    amb *= np.clip(T(d) / 0.8, 0, 1) * np.clip((d - T(d)) / 0.8, 0, 1)
    save('crowd_ambient', reverb(amb, 1.2, 0.35), 0.6)
    t = T(2.2)
    horn = sum(np.sign(np.sin(2 * np.pi * f * t)) * 0.3 + np.sin(2 * np.pi * f * t) for f in (220, 277.2, 329.6))
    horn = lp(horn, 1800) * np.clip(t / 0.08, 0, 1) * np.clip((2.2 - t) / 0.4, 0, 1)
    save('goal_horn', reverb(horn, 1.0, 0.3), 0.8)
    # ---- mucadele
    d = 0.75
    scrape = lp(noise(d), 1800) * (0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 23 * T(d)))) * env(int(d * SR), 0.03, 0.3)
    save('slide', mix(scrape, bp(noise(d), 2000, 6000) * 0.2 * env(int(d * SR), 0.01, 0.2)), 0.8)
    grunt = bp(np.sign(np.sin(2 * np.pi * 120 * T(0.25))) + 0.3 * noise(0.25), 300, 900) * env(int(0.25 * SR), 0.01, 0.08)
    save('tackle', mix(thump(70, 0.2, 1.2), slap(0.03, 500, 3000), np.pad(grunt * 0.5, (int(0.03 * SR), 0))), 0.85)
    save('skill', sweep_noise(0.28, 600, 4500, 0.35) * env(int(0.28 * SR), 0.06, 0.08), 0.6)
    save('catch', mix(thump(85, 0.18, 1.2), bp(noise(0.06), 400, 2500) * env(int(0.06 * SR), 0.001, 0.02) * 1.2), 0.85)
    # ---- yetenekler
    d = 1.6
    fire = lp(noise(d), 900) * env(int(d * SR), 0.05, 0.5) * 1.5
    crack = np.zeros(int(d * SR))
    for _ in range(80):
        p = int(R.uniform(0, d - 0.05) * SR)
        c = slap(0.006, 2000, 9000) * R.uniform(0.2, 1)
        crack[p:p + len(c)] += c
    save('ability_fire', reverb(mix(fire, crack, thump(55, 0.4, 1.5)), 0.8, 0.25))
    d = 2.2
    thunder = mix(slap(0.05, 300, 8000) * 3, lp(noise(d), 300) * env(int(d * SR), 0.08, 0.6) * 2.5)
    save('ability_thunder', reverb(thunder, 1.4, 0.4))
    d = 1.6
    wind = sweep_noise(d, 300, 2400, 0.25) * (0.5 + 0.5 * np.sin(2 * np.pi * 6 * T(d))) * np.clip(T(d) / 0.2, 0, 1) * np.clip((d - T(d)) / 0.5, 0, 1)
    save('ability_wind', reverb(wind, 0.8, 0.3))
    t = T(1.8)
    ghost = sum(np.sin(2 * np.pi * np.cumsum(f0 * (1 + 0.3 * np.sin(2 * np.pi * 0.7 * t + k))) / SR) for k, f0 in enumerate((440, 554, 659)))
    ghost *= np.clip(t / 0.3, 0, 1) * np.clip((1.8 - t) / 0.6, 0, 1)
    save('ability_ghost', reverb(ghost, 1.5, 0.6), 0.6)
    t = T(1.6)
    hum = (np.sin(2 * np.pi * 110 * t) + 0.5 * np.sin(2 * np.pi * 220 * t)) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * t))
    zap = sweep_noise(1.6, 800, 3500, 0.15) * 0.4
    save('ability_magnet', mix(hum * np.clip((1.6 - t) / 0.4, 0, 1), zap * env(len(t), 0.2, 0.6)), 0.7)
    t = T(1.6)
    tink = np.zeros(len(t))
    for _ in range(30):
        f = R.uniform(2500, 6500)
        p = int(R.uniform(0, 1.2) * SR)
        s = np.sin(2 * np.pi * f * T(0.4)) * np.exp(-T(0.4) * 12) * R.uniform(0.3, 1)
        tink[p:p + len(s)] += s[:len(tink) - p]
    save('ability_ice', reverb(mix(tink, slap(0.04, 1500, 9000) * 2, lp(noise(0.5), 600) * env(int(0.5 * SR), 0.01, 0.15)), 1.0, 0.4), 0.8)

    names = ["kick_soft", "kick_hard", "kick_power", "touch", "post", "net", "bounce", "whistle", "whistle_end", "crowd_goal",
             "crowd_ooh", "crowd_ambient", "goal_horn", "slide", "tackle", "skill", "catch", "ability_fire", "ability_thunder",
             "ability_wind", "ability_ghost", "ability_magnet", "ability_ice"]
    stream = {"crowd_goal", "crowd_ambient", "goal_horn"}
    sj = {}
    for n in names:
        e = {"name": f"rabonaarena:{n}"}
        if n in stream:
            e["stream"] = True
        sj[n] = {"subtitle": f"subtitles.rabonaarena.{n}", "sounds": [e]}
    with open(SJ, 'w') as f:
        json.dump(sj, f, indent=1)


if __name__ == '__main__':
    main()
    print('ok')
