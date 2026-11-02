"""Procedural sound design for the Hatman -> mono .ogg."""
import os, sys, json, numpy as np
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../gen'))
import sounds as S
from sounds import SR, T, noise, brown, bp, lp, hp, sweep_bp, env, adsr, osc, glide, formant, boom, crackle, reverb, rattle, mix, pad, save

S.OUT = os.path.join(os.path.dirname(__file__), '../common/src/main/resources/assets/krolasyonhorror/sounds')
R = np.random.default_rng(1337)


def n_(d):
    return int(d * SR)


def crack(d=0.05, bright=1.0):
    n = n_(d)
    x = R.standard_normal(n) * np.exp(-np.arange(n) / (n / 6))
    return hp(x, 900 * bright) + bp(x, 200, 900) * 0.4


def cracks(d, count, spread=1.0):
    out = np.zeros(n_(d))
    for _ in range(count):
        p = int(R.random() * spread * (len(out) - n_(0.06)))
        c = crack(0.03 + R.random() * 0.05, 0.7 + R.random())
        out[p:p + len(c)] += c * (0.4 + R.random() * 0.6)
    return out


def vhs(x, amount=0.25):
    """tape hiss + wow/flutter + band limiting"""
    n = len(x)
    t = np.arange(n) / SR
    wob = 1 + 0.004 * np.sin(2 * np.pi * 0.7 * t) + 0.002 * np.sin(2 * np.pi * 7 * t)
    idx = np.clip(np.cumsum(wob) - 1, 0, n - 1)
    y = np.interp(idx, np.arange(n), x)
    y = bp(y, 80, 7000)
    hiss = hp(R.standard_normal(n), 3000) * amount * 0.05
    return y + hiss


def voice(d, f0, f1, vowel='a', jitter=0.02, harsh=1.0):
    f = glide(d, f0, f1) * (1 + jitter * np.cumsum(R.standard_normal(n_(d))) / np.sqrt(SR) * 8)
    src = osc(f, 'saw') + 0.5 * osc(f * 1.007, 'saw') + 0.3 * osc(f * 0.5, 'pulse')
    return np.tanh(formant(src, vowel) * 2.5 * harsh)


def ambient(v):
    d = 3.2
    breath = bp(noise(d), 300, 1400) * np.sin(np.pi * T(d) / d) ** 2 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.6 * T(d)))
    clicks = np.zeros(n_(d))
    for k in range(10 + v * 4):
        p = int((0.2 + 0.6 * R.random()) * len(clicks))
        c = crack(0.012, 2.5)
        clicks[p:p + len(c)] += c * 0.7
    low = voice(d, 55 + v * 6, 48, 'o', 0.03) * env(n_(d), 0.6, 1.4) * 0.35
    return vhs(reverb(mix(breath * 0.8, clicks, low), 0.45, 2.0))


def breath():
    d = 2.6
    t = T(d)
    e = np.sin(np.pi * np.clip(t / 1.5, 0, 1)) ** 2 * (t < 1.5) + 0.7 * np.sin(np.pi * np.clip((t - 1.5) / 1.1, 0, 1)) * (t >= 1.5)
    x = sweep_bp(noise(d), 500, 1500, 2.0) * e + bp(noise(d), 120, 400) * e * 0.4
    return vhs(reverb(x, 0.3, 1.0))


def step(v):
    d = 0.35
    thud = lp(noise(d), 260) * env(n_(d), 0.002, 0.06) * 2.5 + boom(d, 90, 40, 0.06) * 0.6
    wet = bp(noise(d), 800, 3000) * env(n_(d), 0.01, 0.05) * 0.25
    click = np.zeros(n_(d)); c = crack(0.02, 2); p = n_(0.02 + 0.03 * v); click[p:p + len(c)] += c * 0.5
    return mix(thud, wet, click)


def hurt(v):
    d = 0.9
    sc = voice(d, 420 + 80 * v, 260, 'e', 0.05, 1.6) * env(n_(d), 0.01, 0.35)
    return vhs(reverb(mix(sc, cracks(d, 5, 0.3) * 0.8), 0.3, 1.2))


def death():
    d = 4.0
    t = T(d)
    wail = voice(d, 520, 70, 'a', 0.06, 1.8) * adsr(n_(d), 0.02, 0.9, 1.5)
    wail *= 1 - 0.5 * (np.sin(2 * np.pi * glide(d, 9, 2) * t) > 0)
    return vhs(reverb(mix(wail, cracks(d, 40, 0.8) * 0.7, hp(noise(d), 2000) * env(n_(d), 2.5, 1.0) * 0.25), 0.5, 2.5), 0.6)


def scream():
    d = 2.4
    t = T(d)
    layers = [voice(d, f, f * 1.3, vv, 0.08, 2.2) for f, vv in ((310, 'a'), (465, 'e'), (622, 'a'), (157, 'o'))]
    v = sum(layers) / 2.5
    ring = v * (0.6 + 0.4 * np.sin(2 * np.pi * 38 * t))
    shriek = sweep_bp(noise(d), 1500, 5000, 1.3) * 0.5
    e = adsr(n_(d), 0.04, 0.95, 0.5)
    trem = 1 - 0.35 * (np.sin(2 * np.pi * 13 * t) > 0.3)
    return vhs(reverb(np.tanh(mix(ring, shriek) * 1.6) * e * trem, 0.35, 2.0), 0.4)


def swipe():
    d = 0.5
    return mix(sweep_bp(noise(d), 500, 5000, 1.8) * np.sin(np.pi * T(d) / d) ** 3, crack(0.05, 3) * 0.3)


def hit():
    d = 0.5
    return mix(lp(noise(d), 400) * env(n_(d), 0.002, 0.08) * 2.0, cracks(d, 6, 0.2), boom(d, 120, 50, 0.1))


def grab():
    d = 1.4
    return reverb(mix(cracks(d, 25, 0.5) * 1.2, lp(noise(d), 300) * env(n_(d), 0.4, 0.2) * 1.5, boom(d, 70, 30, 0.4) * 0.8), 0.3, 1.0)


def leap():
    d = 0.8
    return mix(sweep_bp(noise(d), 300, 3000, 1.6) * env(n_(d), 0.05, 0.25) * 1.2, cracks(d, 10, 0.15))


def twitch():
    d = 1.1
    x = cracks(d, 45, 0.95) * 1.3
    x *= (R.random(n_(d)) > 0.0)
    glitch = osc(np.full(n_(d), 2400.0), 'pulse') * ((np.arange(n_(d)) // 2205) % 3 == 0) * 0.08
    return vhs(mix(x, glitch))


def hat_tip():
    d = 2.2
    t = T(d)
    hum = voice(d, 92, 84, 'o', 0.02, 0.8) * (0.55 + 0.45 * np.sin(2 * np.pi * 4.6 * t) ** 2) * adsr(n_(d), 0.2, 0.8, 0.6)
    return vhs(reverb(hum, 0.5, 2.2), 0.6)


def static_burst(v):
    d = 0.9 + 0.4 * v
    x = noise(d)
    gate = np.repeat(R.random(int(d * 60) + 1) > 0.25, SR // 60 + 1)[:n_(d)]
    x = bp(x, 400, 9000) * gate * env(n_(d), 0.01, 0.6)
    return x + osc(np.full(n_(d), 15734.0)) * 0.03


def vanish():
    d = 1.6
    t = T(d)
    swell = bp(noise(d), 300, 8000) * (t / d) ** 3
    swell[int(0.9 * len(swell)):] *= 0.05
    return mix(swell, boom(d, 60, 25, 0.5) * 0.4)


def heartbeat():
    d = 0.9
    a = boom(0.3, 70, 38, 0.08)
    out = np.zeros(n_(d))
    out[:len(a)] += a
    out[n_(0.26):n_(0.26) + len(a)] += a * 0.7
    return lp(out, 200)


def jumpscare():
    d = 2.0
    t = T(d)
    cl = sum(osc(np.full(n_(d), f), 'saw') for f in (98, 104, 147, 155, 233, 247, 311))
    cl = np.tanh(cl * 0.8) * env(n_(d), 0.005, 0.6)
    burst = bp(noise(d), 200, 9000) * env(n_(d), 0.002, 0.3) * 1.5
    return vhs(reverb(mix(cl, burst, scream()[:n_(d)] * 0.7, boom(d, 80, 30, 0.5)), 0.3, 1.5))


def knock():
    d = 2.0
    out = np.zeros(n_(d))
    for i, p in enumerate((0.0, 0.32, 0.6)):
        k = bp(noise(0.12), 150, 1100) * env(n_(0.12), 0.001, 0.025) * 2 + boom(0.12, 180, 120, 0.03)
        out[n_(p):n_(p) + len(k)] += k * (1 - 0.15 * i)
    return reverb(out, 0.45, 1.8)


def whisper():
    d = 2.8
    t = T(d)
    syl = np.clip(np.sin(2 * np.pi * 3.1 * t + 2 * np.sin(2 * np.pi * 0.7 * t)), 0, 1) ** 2
    x = sum(formant(hp(noise(d), 300), vv) * w for vv, w in (('e', 0.6), ('a', 0.4)))
    x = x * syl * np.sin(np.pi * t / d)
    return vhs(reverb(hp(x, 600), 0.6, 2.4), 0.5)


def fake_step():
    d = 0.5
    creak = sweep_bp(noise(d), 500, 300, 6) * env(n_(d), 0.05, 0.2) * 0.4
    return reverb(mix(step(1) * 0.8, creak), 0.4, 1.0)


def extinguish():
    d = 0.9
    return mix(hp(noise(d), 2500) * env(n_(d), 0.02, 0.3) * 0.5, crack(0.03, 2) * 0.6)


def drone():
    d = 9.0
    t = T(d)
    x = sum(osc(np.full(n_(d), f) * (1 + 0.003 * np.sin(2 * np.pi * 0.1 * k * t)), 'saw') for k, f in enumerate((41.2, 43.65, 61.7, 82.4, 87.3)))
    x = lp(x, 400) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.25 * t))
    tick = np.zeros(n_(d))
    for k in range(int(d * 2)):
        c = crack(0.01, 3); p = n_(k * 0.5)
        tick[p:p + len(c)] += c * 0.25
    e = np.minimum(1, t / 1.0) * np.minimum(1, (d - t) / 1.0)
    return vhs(mix(x * 0.6, tick) * e, 0.5)


def emerge():
    d = 3.2
    return vhs(reverb(mix(cracks(d, 60, 0.9) * 1.1, lp(noise(d), 350) * env(n_(d), 1.0, 1.5) * 0.7,
                          voice(d, 60, 90, 'u', 0.04, 0.6) * env(n_(d), 1.6, 1.0) * 0.4), 0.4, 1.6))


def distant_call():
    d = 4.0
    v = voice(d, 210, 190, 'o', 0.03, 1.0) * adsr(n_(d), 0.6, 0.8, 1.6)
    v = lp(v, 1500) * 0.6
    return vhs(reverb(v, 0.8, 3.5), 0.8)


JOBS = {
    'ambient1': lambda: ambient(1), 'ambient2': lambda: ambient(2), 'ambient3': lambda: ambient(3), 'breath': breath,
    'step1': lambda: step(1), 'step2': lambda: step(2), 'step3': lambda: step(3),
    'hurt1': lambda: hurt(1), 'hurt2': lambda: hurt(2), 'death': death, 'scream': scream, 'swipe': swipe, 'hit': hit,
    'grab': grab, 'leap': leap, 'twitch': twitch, 'hat_tip': hat_tip, 'static1': lambda: static_burst(0),
    'static2': lambda: static_burst(1), 'vanish': vanish, 'heartbeat': heartbeat, 'jumpscare': jumpscare, 'knock': knock,
    'whisper': whisper, 'fake_step': fake_step, 'extinguish': extinguish, 'drone': drone, 'emerge': emerge,
    'distant_call': distant_call,
}
GAIN = {'step1': -8, 'step2': -8, 'step3': -8, 'ambient1': -3, 'ambient2': -3, 'ambient3': -3, 'breath': -5, 'heartbeat': -2,
        'drone': -4, 'whisper': -4, 'fake_step': -5}
EVENTS = {  # sound event -> files
    'hatman.ambient': ['ambient1', 'ambient2', 'ambient3', 'breath'], 'hatman.step': ['step1', 'step2', 'step3'],
    'hatman.hurt': ['hurt1', 'hurt2'], 'hatman.death': ['death'], 'hatman.scream': ['scream'], 'hatman.swipe': ['swipe'],
    'hatman.hit': ['hit'], 'hatman.grab': ['grab'], 'hatman.leap': ['leap'], 'hatman.twitch': ['twitch'],
    'hatman.hat_tip': ['hat_tip'], 'hatman.vanish': ['vanish'], 'hatman.emerge': ['emerge'], 'hatman.extinguish': ['extinguish'],
    'fx.static': ['static1', 'static2'], 'fx.heartbeat': ['heartbeat'], 'fx.jumpscare': ['jumpscare'], 'fx.knock': ['knock'],
    'fx.whisper': ['whisper'], 'fx.fake_step': ['fake_step'], 'fx.drone': ['drone'], 'fx.distant_call': ['distant_call'],
}

if __name__ == '__main__':
    os.makedirs(S.OUT, exist_ok=True)
    for name, fn in JOBS.items():
        save(name, fn(), GAIN.get(name, -1.0))
    sj = {ev: {'subtitle': f'subtitles.krolasyonhorror.{ev}', 'sounds': [f'krolasyonhorror:{f}' for f in files]} for ev, files in EVENTS.items()}
    with open(os.path.join(S.OUT, '..', 'sounds.json'), 'w') as f:
        json.dump(sj, f, indent=2)
    print(len(JOBS), 'sounds,', len(EVENTS), 'events')
