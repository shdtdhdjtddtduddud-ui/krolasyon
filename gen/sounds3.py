"""Sound design for the player 'boss form' (Kalp Kırıcı transformation + abilities)."""
import json
from sounds import *
from sounds2 import laugh, heartbeat, breath


def roar(d, f0=70, f1=45, amt=1.0):
    n = int(d * SR)
    t = T(d)
    f = np.interp(t, [0, 0.15 * d, d], [f0 * 0.8, f0 * 1.3, f1])
    body = formant(osc(f, 'saw') * rattle(n, 30, 0.5) + 0.4 * noise(d), 'a')
    sub = formant(osc(f * 0.5, 'saw'), 'o')
    grit = bp(noise(d), 1500, 5000) * rattle(n, 45, 0.8) * 0.25
    return np.tanh(mix(body, sub * 0.8, grit) * 2.5 * amt) * adsr(n, 0.06, 0.9, 0.5 * d)


def transform_charge():
    d = 2.0
    n = int(d * SR)
    t = T(d)
    swell = sweep_bp(noise(d), 120, 1400, 1.5) * (t / d) ** 2 * 1.3
    drone = osc(glide(d, 40, 80), 'saw')
    drone = lp(drone, 400) * (t / d) * 0.7
    hb = heartbeat(d, 60, 1.35) * 1.1
    whisper = mix(*[laugh(d, 180 + 30 * k, 6, 'e') * 0.07 for k in range(3)]) * (t / d)
    return reverb(mix(swell, drone, hb, whisper), 0.5, 1.8)


def transform_burst():
    d = 2.6
    n = int(d * SR)
    hit = mix(boom(d, 140, 25, 0.9) * 1.4, bp(noise(d), 200, 6000) * env(n, 0.001, 0.25) * 0.8)
    shatter = np.zeros(n)
    for _ in range(30):
        s = int(R.random() * 0.5 * SR)
        b = bell(0.6, 900 + R.random() * 2800, decay=0.12)
        shatter[s:s + len(b)] += b[:n - s] * R.random() * 0.5
    r = pad(np.concatenate([np.zeros(int(0.12 * SR)), roar(1.9, 78, 42)]), d)[:n]
    lg = pad(np.concatenate([np.zeros(int(1.2 * SR)), laugh(1.3, 95, 5) * 0.4]), d)[:n]
    return reverb(mix(hit, shatter * 0.4, r * 1.1, lg), 0.5, 2.2)


def demon_revert():
    d = 1.6
    n = int(d * SR)
    t = T(d)
    whoosh = sweep_bp(noise(d), 2500, 150, 1.6) * np.sin(np.pi * t / d) ** 1.5
    fall = osc(glide(d, 400, 60)) * env(n, 0.01, 0.8) * 0.5
    sigh = breath(d, 1) * 0.5
    chime = mix(*[bell(d, f, decay=0.4) for f in (880, 1320)]) * 0.15
    return reverb(mix(whoosh, fall, sigh, chime), 0.5, 1.8)


def demon_roar():
    return reverb(roar(1.8, 85, 48), 0.45, 1.8)


def double_jump():
    d = 0.7
    n = int(d * SR)
    flap = mix(*[np.concatenate([np.zeros(int(k * 0.09 * SR)), lp(noise(0.12), 700) * env(int(0.12 * SR), 0.01, 0.04)]) for k in range(2)])
    wh = sweep_bp(noise(d), 300, 2200, 1.8) * env(n, 0.03, 0.25)
    return reverb(mix(pad(flap, d)[:n] * 1.3, wh, boom(d, 90, 40, 0.1) * 0.6), 0.3, 0.9)


def ground_slam():
    d = 3.0
    n = int(d * SR)
    rubble = crackle(d, 140) * env(n, 0.002, 0.8) * 1.3
    cr = mix(*[np.concatenate([np.zeros(int(R.random() * 0.6 * SR)), bp(noise(0.08), 500, 3500) * env(int(0.08 * SR), 0.0005, 0.02)]) for _ in range(25)])
    return reverb(mix(boom(d, 120, 22, 1.2) * 1.6, lp(noise(d), 300) * env(n, 0.002, 0.6), rubble * 0.5, pad(cr, d)[:n] * 0.6,
                      pad(roar(1.2, 90, 50), d)[:n] * 0.5), 0.45, 2.4)


def claw_swipe(v):
    d = 0.45
    n = int(d * SR)
    t = T(d)
    wh = sweep_bp(noise(d), [900, 1100, 800][v - 1], [4200, 5200, 3800][v - 1], 2.0) * np.sin(np.pi * np.clip(t / 0.22, 0, 1)) ** 2
    tear = bp(noise(d), 1500, 7000) * rattle(n, 70, 0.9) * env(n, 0.12, 0.08) * 0.5
    return reverb(mix(wh * 1.2, tear), 0.2, 0.5)


def heart_rip():
    d = 1.6
    n = int(d * SR)
    t = T(d)
    wet = bubbles(d, 18, 200, 700) if False else bp(noise(d), 200, 1200) * rattle(n, 18, 0.7) * env(n, 0.02, 0.4)
    chord = mix(*[osc(glide(d, f, f * 1.5)) * env(n, 0.3, 0.9) for f in (330, 415, 494)]) * 0.35
    return reverb(mix(wet, chord, heartbeat(d, 90)[:n] * 0.9), 0.5, 1.6)


JOBS = {'transform_charge': transform_charge, 'transform_burst': transform_burst, 'demon_revert': demon_revert,
        'demon_roar': demon_roar, 'double_jump': double_jump, 'ground_slam': ground_slam, 'heart_rip': heart_rip}
for _v in (1, 2, 3):
    JOBS[f'claw_swipe{_v}'] = (lambda v=_v: claw_swipe(v))

SUBS = {'transform_charge': 'Karanlık güç toplanıyor', 'transform_burst': 'Kalp Kırıcı uyanıyor', 'demon_revert': 'İblis formu çözülüyor',
        'demon_roar': 'İblis kükrüyor', 'double_jump': 'Gölge sıçrayışı', 'ground_slam': 'Yer yarılıyor', 'heart_rip': 'Kalpler sökülüyor',
        'claw_swipe': 'Pençe savruluyor'}
SUBS_EN = {'transform_charge': 'Dark power gathers', 'transform_burst': 'The Heartbreaker awakens', 'demon_revert': 'Demon form fades',
           'demon_roar': 'Demon roars', 'double_jump': 'Shadow leap', 'ground_slam': 'Ground shatters', 'heart_rip': 'Hearts are torn out',
           'claw_swipe': 'Claw swipes'}

if __name__ == '__main__':
    for name, fn in JOBS.items():
        save(name, fn(), -1.0)
    root = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolasyonbosses')
    p = os.path.join(root, 'sounds.json')
    sj = json.load(open(p))
    for ev in SUBS:
        files = [f'claw_swipe{v}' for v in (1, 2, 3)] if ev == 'claw_swipe' else [ev]
        sj[ev] = {'subtitle': f'subtitles.krolasyonbosses.{ev}', 'sounds': [{'name': f'krolasyonbosses:{f}', 'attenuation_distance': 32 if ev == 'claw_swipe' else 48} for f in files]}
    json.dump(sj, open(p, 'w'), indent=1, ensure_ascii=False)
    print(len(JOBS), 'sounds')
