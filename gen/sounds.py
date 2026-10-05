"""Synthesised sound effects -> ogg (vorbis) + sounds.json."""
import os, json, subprocess, tempfile, wave
import numpy as np

SR = 44100
rng = np.random.default_rng(7)


def t(d):
    return np.arange(int(SR * d)) / SR


def env(n, a=0.01, d=0.3, curve=3.0):
    x = np.arange(n) / SR
    att = np.clip(x / max(a, 1e-4), 0, 1)
    dec = np.exp(-np.maximum(0, x - a) / max(d, 1e-4) * curve / 3)
    return att * dec


def noise(d):
    return rng.uniform(-1, 1, int(SR * d))


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def lp_fast(x, cutoff):
    from scipy.signal import lfilter
    a = np.exp(-2 * np.pi * cutoff / SR)
    return lfilter([1 - a], [1, -a], x)


def hp(x, cutoff):
    return x - lp_fast(x, cutoff)


def sweep(f0, f1, d, wave_='sine'):
    tt = t(d)
    f = f0 * (f1 / f0) ** (tt / d) if f0 > 0 and f1 > 0 else np.linspace(f0, f1, len(tt))
    ph = 2 * np.pi * np.cumsum(f) / SR
    if wave_ == 'saw':
        return 2 * ((ph / (2 * np.pi)) % 1) - 1
    if wave_ == 'square':
        return np.sign(np.sin(ph))
    return np.sin(ph)


def tone(f, d, harm=(1.0,), detune=0.0):
    tt = t(d)
    out = np.zeros_like(tt)
    for k, a in enumerate(harm):
        out += a * np.sin(2 * np.pi * f * (k + 1) * tt * (1 + detune * (k % 2)))
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def delay(x, offset):
    return np.concatenate([np.zeros(int(SR * offset)), x])


def reverb(x, amount=0.35, times=(0.031, 0.047, 0.071, 0.113, 0.167), decay=0.55):
    out = x.copy()
    for i, d in enumerate(times):
        out = mix(out, delay(x * amount * decay ** i, d))
    tail = np.zeros(int(SR * 0.6))
    return mix(out, tail)


def norm(x, peak=0.85):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * peak


def save(x, path):
    x = norm(x)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as f:
        tmp = f.name
    with wave.open(tmp, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', tmp, '-c:a', 'libvorbis', '-q:a', '4', path], check=True)
    os.unlink(tmp)


def chime(freqs, step=0.09, d=0.9):
    parts = []
    for i, f in enumerate(freqs):
        s = tone(f, d, (1, 0.35, 0.12, 0.05)) * env(int(SR * d), 0.005, 0.35)
        parts.append(delay(s, i * step))
    return reverb(mix(*parts), 0.3)


def growl(base, d, rough=0.6):
    tt = t(d)
    f = base * (1 + 0.15 * np.sin(2 * np.pi * 7 * tt) + 0.05 * rng.standard_normal(len(tt)).cumsum() / 200)
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = 2 * ((ph / (2 * np.pi)) % 1) - 1
    s = lp_fast(s, base * 6) + rough * lp_fast(noise(d), 900) * np.sin(2 * np.pi * 31 * tt)
    return s * env(len(tt), 0.06, d * 0.6)


SOUNDS = {
    'system': lambda: chime([1046.5, 1568.0], 0.07, 0.8),
    'system_warn': lambda: mix(sweep(320, 320, 0.16, 'square') * env(int(SR * .16), 0.005, 0.15), delay(sweep(260, 260, 0.22, 'square') * env(int(SR * .22), 0.005, 0.2), 0.2)) * 0.5,
    'level_up': lambda: chime([523.25, 659.25, 783.99, 1046.5, 1318.5], 0.08, 1.2),
    'news': lambda: chime([659.25, 880.0, 987.77], 0.13, 0.7),
    'arise': lambda: reverb(mix(sweep(40, 90, 2.2, 'saw') * env(int(SR * 2.2), 0.6, 1.4) * 0.8,
                                lp_fast(noise(2.0), 400) * env(int(SR * 2.0), 0.9, 0.8),
                                tone(55, 2.4, (1, 0.6, 0.4, 0.3, 0.2)) * env(int(SR * 2.4), 0.05, 1.6),
                                delay(tone(110, 1.6, (1, .5, .3), 0.003) * env(int(SR * 1.6), 0.3, 1.0) * 0.6, 0.4)), 0.4),
    'dash': lambda: hp(noise(0.35), 600) * env(int(SR * .35), 0.04, 0.2) * sweep(1, 1, 0.35),
    'slash': lambda: mix(hp(noise(0.22), 2500) * env(int(SR * .22), 0.003, 0.12), tone(2400, 0.3, (1, .3)) * env(int(SR * .3), 0.002, 0.15) * 0.25),
    'bloodlust': lambda: reverb(mix(tone(48, 0.25) * env(int(SR * .25), 0.005, 0.1), delay(tone(44, 0.3) * env(int(SR * .3), 0.005, 0.12), 0.32),
                                    (tone(110, 1.4, (1,)) + tone(116.5, 1.4, (1,))) * env(int(SR * 1.4), 0.3, 0.8) * 0.4), 0.3),
    'authority': lambda: reverb(sweep(60, 160, 0.9) * env(int(SR * .9), 0.05, 0.5) + hp(noise(0.9), 3000) * 0.05, 0.4),
    'roar': lambda: reverb(growl(85, 1.6, 0.8), 0.3),
    'domain': lambda: reverb(mix(tone(41, 2.5, (1, .5, .3)) * env(int(SR * 2.5), 1.0, 1.4), lp_fast(noise(2.5), 300) * env(int(SR * 2.5), 1.2, 1.0) * 0.6), 0.5),
    'howl': lambda: reverb(np.sin(2 * np.pi * np.cumsum(np.concatenate([np.linspace(330, 620, int(SR * .5)), np.linspace(620, 560, int(SR * .9)),
                                                                        np.linspace(560, 380, int(SR * .4))]) * (1 + 0.012 * np.sin(2 * np.pi * 6 * t(1.8)))) / SR)
                           * env(int(SR * 1.8), 0.25, 1.0), 0.45),
    'laser': lambda: mix(sweep(300, 2400, 0.8, 'saw') * env(int(SR * .8), 0.7, 0.3) * 0.3, delay(lp_fast(noise(1.2), 2000) * env(int(SR * 1.2), 0.02, 0.8), 0.7)),
    'fire': lambda: lp_fast(noise(0.9), 1400) * env(int(SR * .9), 0.08, 0.5) + hp(noise(0.9), 4000) * (rng.random(int(SR * .9)) > 0.995) * 0.8,
    'thunder': lambda: reverb(mix(hp(noise(0.15), 1500) * env(int(SR * .15), 0.001, 0.05) * 1.5, lp_fast(noise(2.2), 180) * env(int(SR * 2.2), 0.05, 1.2) * 1.4), 0.4),
    'slam': lambda: mix(tone(45, 0.6, (1, .5)) * env(int(SR * .6), 0.002, 0.25), lp_fast(noise(0.7), 700) * env(int(SR * .7), 0.003, 0.3)),
    'boss_death': lambda: reverb(mix(sweep(120, 30, 2.5, 'saw') * env(int(SR * 2.5), 0.01, 1.6) * 0.6, lp_fast(noise(2.5), 250) * env(int(SR * 2.5), 0.01, 1.4),
                                     delay(chime([392, 523.25, 659.25], 0.15, 1.5) * 0.6, 1.0)), 0.4),
    'gate_hum': lambda: mix(tone(55, 2.5, (1, .6, .3, .2), 0.002), tone(82.4, 2.5, (1, .3))) * (0.7 + 0.3 * np.sin(2 * np.pi * 1.5 * t(2.5))) * env(int(SR * 2.5), 0.6, 2.0),
    'gate_enter': lambda: reverb(mix(sweep(200, 1600, 0.7) * env(int(SR * .7), 0.1, 0.4) * 0.5, hp(noise(0.8), 1200) * env(int(SR * .8), 0.2, 0.4) * 0.4,
                                     delay(chime([1318.5, 1760.0], 0.06, 0.5) * 0.5, 0.5)), 0.35),
    'small_voice': lambda: mix(*[delay(sweep(700 + i * 80, 500 + i * 120, 0.09, 'square') * env(int(SR * .09), 0.005, 0.06) * 0.4, i * 0.11) for i in range(3)]),
    'beast_growl': lambda: growl(110, 0.9, 0.5),
    'insect_click': lambda: mix(*[delay(hp(noise(0.02), 3000) * env(int(SR * .02), 0.0005, 0.01), i * 0.05) for i in range(6)]),
    'stone_grind': lambda: lp_fast(noise(1.0), 300) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * t(1.0))) * env(int(SR * 1.0), 0.1, 0.6),
    'armor_clank': lambda: mix(tone(620, 0.35, (1, .7, .5, .4)) * env(int(SR * .35), 0.001, 0.12), delay(tone(880, 0.3, (1, .5)) * env(int(SR * .3), 0.001, 0.1) * 0.6, 0.12)),
    'demon_growl': lambda: reverb(growl(62, 1.2, 1.0), 0.3),
    'serpent_hiss': lambda: hp(noise(1.0), 3500) * env(int(SR * 1.0), 0.15, 0.6),
    'hurt_flesh': lambda: mix(tone(90, 0.2) * env(int(SR * .2), 0.002, 0.08), lp_fast(noise(0.2), 1200) * env(int(SR * .2), 0.002, 0.08)),
    'hurt_hard': lambda: mix(tone(420, 0.25, (1, .6, .4)) * env(int(SR * .25), 0.001, 0.08), hp(noise(0.15), 2000) * env(int(SR * .15), 0.001, 0.05)),
}

VARIANTS = {'beast_growl', 'small_voice', 'insect_click', 'demon_growl', 'hurt_flesh', 'hurt_hard', 'slash', 'armor_clank'}


def run(res_root):
    d = os.path.join(res_root, 'sounds')
    os.makedirs(d, exist_ok=True)
    sj = {}
    for name, fn in SOUNDS.items():
        files = []
        n = 2 if name in VARIANTS else 1
        for k in range(n):
            x = fn()
            if k:
                # resample variant (pitch shift)
                idx = np.arange(0, len(x) - 1, 1.12)
                x = np.interp(idx, np.arange(len(x)), x)
            fn_ = f'{name}{k + 1 if n > 1 else ""}'
            save(x, os.path.join(d, fn_ + '.ogg'))
            files.append({'name': 'sololeveling:' + fn_, 'volume': 1.0})
        sj[name] = {'subtitle': 'subtitles.sololeveling.' + name, 'sounds': files}
    with open(os.path.join(res_root, 'sounds.json'), 'w') as f:
        json.dump(sj, f, indent=1)
    return list(SOUNDS)
