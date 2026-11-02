"""Story sound effects + music cues (original melodies)."""
import os, json, numpy as np
import hsounds as H
from hsounds import SR, T, noise, bp, lp, hp, env, adsr, osc, glide, formant, boom, reverb, mix, save, crack, cracks, vhs, voice

R = np.random.default_rng(77)
n_ = H.n_


def note(f, d, kind='tri', decay=0.5):
    t = T(d)
    return osc(np.full(len(t), f), kind) * np.exp(-t / decay)


def toy_piano(f, d):
    t = T(d)
    x = sum(np.sin(2 * np.pi * f * k * t * (1 + 0.002 * k)) / k ** 1.5 for k in (1, 2, 3, 4.1))
    return x * np.exp(-t * 5) * (1 - np.exp(-t * 400))


def melody(notes, bpm, inst, detune=0.0, wow=0.006):
    beat = 60 / bpm
    out = np.zeros(n_(sum(b for _, b in notes) * beat + 1.5))
    t = 0
    for midi, b in notes:
        if midi:
            f = 440 * 2 ** ((midi - 69 + detune * (R.random() - 0.5)) / 12)
            x = inst(f, b * beat + 0.8)
            p = n_(t)
            out[p:p + len(x)] += x[:len(out) - p]
        t += b * beat
    tt = np.arange(len(out)) / SR
    idx = np.clip(np.arange(len(out)) * (1 + wow * np.sin(2 * np.pi * 0.6 * tt)), 0, len(out) - 1)
    return np.interp(idx, np.arange(len(out)), out)


def jingle():
    # an original, too-cheerful children's show theme
    m = [(72, .5), (76, .5), (79, .5), (84, 1), (83, .5), (79, .5), (76, 1), (77, .5), (81, .5), (79, .5), (76, .5), (74, 1), (72, 1.5)]
    bass = [(48, 1), (55, 1), (52, 1), (55, 1), (53, 1), (55, 1), (48, 2)]
    a = melody(m, 132, toy_piano, detune=0.35)
    b = melody(bass, 132, lambda f, d: note(f, d, 'tri', 0.35) * 0.5, detune=0.2)
    return vhs(reverb(mix(a, b), 0.25, 1.2), 0.5)


def music_box():
    m = [(81, 1), (79, .5), (76, .5), (77, 1), (74, 1), (76, .5), (77, .5), (79, 1), (72, 2), (81, 1), (79, .5), (76, .5), (74, 2), (72, 2)]
    box = lambda f, d: (np.sin(2 * np.pi * f * T(d)) + 0.3 * np.sin(2 * np.pi * f * 4.2 * T(d))) * np.exp(-T(d) * 3.5)
    x = melody(m, 76, box, detune=0.5, wow=0.012)
    return vhs(reverb(x, 0.4, 1.8), 0.4)


def applause():
    d = 3.6
    out = np.zeros(n_(d))
    for _ in range(900):
        p = int(R.random() * (len(out) - 2000) * (0.15 + 0.85 * R.random() ** 0.5))
        c = bp(R.standard_normal(600), 800, 5000) * np.exp(-np.arange(600) / 90)
        out[p:p + 600] += c * (0.3 + R.random())
    e = np.minimum(1, T(d) / 0.3) * np.minimum(1, (d - T(d)) / 1.2)
    return vhs(reverb(out * e, 0.3, 1.0), 0.6)


def phone_beep():
    d = 1.0
    t = T(d)
    beep = np.sin(2 * np.pi * 1000 * t) * ((t > 0.3) & (t < 0.75))
    click = np.zeros(n_(d)); c = crack(0.02, 2); click[:len(c)] += c
    return vhs(mix(beep * 0.6, click, hp(noise(d), 2000) * 0.03), 0.6)


def rain():
    d = 8.0
    x = lp(hp(noise(d), 400), 6000) * (0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 0.11 * T(d))))
    drops = np.zeros(n_(d))
    for _ in range(500):
        p = R.integers(0, len(drops) - 300)
        drops[p:p + 300] += hp(R.standard_normal(300), 3000) * np.exp(-np.arange(300) / 40) * R.random()
    e = np.minimum(1, T(d) / 1.0) * np.minimum(1, (d - T(d)) / 1.5)
    return mix(x * 0.5, drops * 0.6) * e


def house_amb():
    d = 10.0
    t = T(d)
    hum = (np.sin(2 * np.pi * 50 * t) + 0.4 * np.sin(2 * np.pi * 100 * t)) * 0.08
    room = lp(noise(d), 300) * 0.25
    creak = np.zeros(n_(d))
    for k in range(3):
        p = n_(1.5 + k * 3 + R.random())
        c = H.sweep_bp(noise(0.7), 300 + 200 * R.random(), 700, 8) * env(n_(0.7), 0.1, 0.3)
        creak[p:p + len(c)] += c * 0.6
    e = np.minimum(1, t / 1.5) * np.minimum(1, (d - t) / 1.5)
    return reverb(mix(hum, room, creak), 0.4, 1.5) * np.concatenate([e, np.zeros(n_(0.75) + 10)])[:len(reverb(mix(hum, room, creak), 0.4, 1.5))]


def door_slam():
    d = 1.6
    return reverb(mix(boom(d, 90, 40, 0.15) * 1.5, lp(noise(d), 600) * env(n_(d), 0.002, 0.08) * 2, crack(0.06, 1) * 0.8), 0.45, 1.4)


def power_on():
    d = 2.4
    t = T(d)
    hum = np.sin(2 * np.pi * glide(d, 30, 60) * t) * np.minimum(1, t / 1.2) * 0.6 + np.sin(2 * np.pi * 120 * t) * 0.15 * np.minimum(1, t)
    clicks = np.zeros(n_(d))
    for p in (0.05, 0.4, 0.62):
        c = crack(0.03, 2); clicks[n_(p):n_(p) + len(c)] += c
    return mix(hum * env(n_(d), 0.8, 2.0, 0.5), clicks)


def tv_on():
    d = 2.0
    t = T(d)
    thunk = boom(d, 70, 30, 0.12) * 1.4
    whine = np.sin(2 * np.pi * 15734 * t) * 0.08 * np.minimum(1, t / 0.2) * np.exp(-t / 1.5)
    deg = bp(noise(d), 200, 2000) * env(n_(d), 0.01, 0.3) * 0.5
    return mix(thunk, whine, deg)


def radio_sweep():
    d = 3.0
    t = T(d)
    whistle = np.sin(2 * np.pi * np.cumsum(glide(d, 400, 2600) * (1 + 0.3 * np.sin(2 * np.pi * 3 * t))) / SR) * 0.2
    return vhs(mix(H.sweep_bp(noise(d), 300, 4000, 2) * 0.7, whistle), 0.4)


def fire():
    d = 5.0
    x = lp(noise(d), 800) * 0.5 + H.crackle(d, 50) * 0.8
    return x * np.minimum(1, T(d) / 0.5) * np.minimum(1, (d - T(d)) / 1.0)


def page():
    d = 0.6
    return H.sweep_bp(noise(d), 2000, 6000, 1.5) * env(n_(d), 0.05, 0.2) * 0.8


def vcr_insert():
    d = 2.2
    t = T(d)
    clunk = boom(0.3, 140, 70, 0.05)
    out = np.zeros(n_(d)); out[:len(clunk)] += clunk
    c = crack(0.04, 1.5); out[n_(0.25):n_(0.25) + len(c)] += c
    motor = (osc(np.full(n_(d), 95.0), 'saw') * 0.08 + bp(noise(d), 200, 900) * 0.15) * ((t > 0.4) & (t < 1.9))
    return mix(out, lp(motor, 1200))


def brim_voice():
    # a low, gentle, unintelligible murmur - never quite words
    d = 2.3
    t = T(d)
    syl = np.clip(np.sin(2 * np.pi * 3.4 * t + 1.5 * np.sin(2 * np.pi * 0.9 * t)), 0, 1) ** 1.5
    v = sum(voice(d, f, f * 0.92, vv, 0.02, 0.7) * w for f, vv, w in ((98, 'o', 0.7), (147, 'u', 0.3)))
    return vhs(reverb(v * syl * env(n_(d), 0.05, 1.8, 0.6), 0.3, 1.0), 0.5)


def pickup():
    d = 0.4
    return mix(bp(noise(d), 1500, 5000) * env(n_(d), 0.01, 0.08) * 0.5, crack(0.02, 2) * 0.5)


def candle_light():
    d = 1.0
    return mix(crack(0.03, 3), bp(noise(d), 500, 3000) * env(n_(d), 0.1, 0.3) * 0.6)


def balloon_pop():
    d = 0.5
    return reverb(hp(noise(d), 300) * env(n_(d), 0.001, 0.02) * 2.5, 0.3, 0.6)


def dial_click():
    return np.concatenate([crack(0.04, 2.5), np.zeros(n_(0.3))])


def fuse_spark():
    d = 0.8
    return mix(H.crackle(d, 200) * 1.2, np.sin(2 * np.pi * 120 * T(d)) * env(n_(d), 0.01, 0.4) * 0.4)


JOBS = dict(jingle=jingle, music_box=music_box, applause=applause, phone_beep=phone_beep, rain=rain, house_amb=house_amb,
            door_slam=door_slam, power_on=power_on, tv_on=tv_on, radio_sweep=radio_sweep, fire=fire, page=page, vcr_insert=vcr_insert,
            brim_voice=brim_voice, pickup=pickup, candle_light=candle_light, balloon_pop=balloon_pop, dial_click=dial_click, fuse_spark=fuse_spark)
GAIN = {'rain': -6, 'house_amb': -4, 'brim_voice': -3, 'page': -6, 'pickup': -6}
if __name__ == '__main__':
    for name, fn in JOBS.items():
        save('s_' + name, fn(), GAIN.get(name, -1.0))
    p = os.path.join(H.S.OUT, '..', 'sounds.json')
    sj = json.load(open(p))
    for name in JOBS:
        stream = name in ('rain', 'house_amb')
        sj[f'story.{name}'] = {'subtitle': f'subtitles.krolasyonhorror.story.{name}',
                               'sounds': [{'name': f'krolasyonhorror:s_{name}', 'stream': stream}] if stream else [f'krolasyonhorror:s_{name}']}
    json.dump(sj, open(p, 'w'), indent=2)
    print(len(JOBS), 'story sounds')
