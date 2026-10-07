"""Synthesised upright-piano samples (C4 and C5) -> ogg. numpy only."""
import os, subprocess, wave, numpy as np
SR = 44100
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolasyonfurniture/sounds')
os.makedirs(OUT, exist_ok=True)
R = np.random.default_rng(11)


def piano(freq, dur=3.2):
    n = int(SR * dur)
    t = np.arange(n) / SR
    out = np.zeros(n)
    B = 0.00035  # inharmonicity
    for k in range(1, 15):
        f = freq * k * np.sqrt(1 + B * k * k)
        if f > 9000:
            break
        amp = (1.0 / k ** 1.15) * (1.0 if k != 7 else 0.4)
        decay = 1.1 + 4.0 / (1 + 0.55 * k) * (1.0 if freq < 400 else 0.75)
        # two slightly detuned strings -> gentle beating like a real piano
        for det, a2 in ((0.0, 1.0), (0.0016 * (1 + 0.1 * k), 0.8)):
            ph = R.random() * 6.283
            out += amp * a2 * np.sin(2 * np.pi * f * (1 + det) * t + ph) * np.exp(-t / decay * 1.0)
    # hammer thump + short noise burst
    th = np.sin(2 * np.pi * freq * 0.5 * t) * np.exp(-t * 38) * 0.35
    nz = R.standard_normal(n) * np.exp(-t * 90) * 0.12
    k = np.ones(40) / 40
    nz = np.convolve(nz, k, 'same')
    out += th + nz
    att = np.minimum(1, t / 0.004)
    rel = np.minimum(1, (dur - t) / 0.25)
    out *= att * rel
    out /= np.abs(out).max()
    return (out * 0.8)


for name, f in (('piano_c4', 261.63), ('piano_c5', 523.25)):
    x = piano(f)
    wav = os.path.join('/tmp', name + '_tmp.wav')
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype('<i2').tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5',
                    os.path.join(OUT, name + '.ogg')], check=True)
    os.remove(wav)
import json
with open(os.path.join(OUT, '../sounds.json'), 'w') as f:
    json.dump({n: {"sounds": [{"name": f"krolasyonfurniture:{n}", "stream": False}]} for n in ('piano_c4', 'piano_c5')}, f, indent=2)
print('sounds ok')
