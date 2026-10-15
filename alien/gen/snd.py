"""Procedural sound design for the Sevastopol station and its hunter -> mono .ogg + sounds.json"""
import json, os, numpy as np
from sndlib import *

SOUNDS = {}


def out(event, name, x, gain=-1.0, subtitle=None):
    save(name, x, gain)
    SOUNDS.setdefault(event, {'sounds': [], 'subtitle': f'subtitles.alienisolation.{subtitle or event}'})
    SOUNDS[event]['sounds'].append(f'alienisolation:{name}')


def drive(x, k=3.0):
    return np.tanh(x * k) / np.tanh(k)


# ------------------------------------------------------------------ xenomorph
def hiss(v):
    d = 1.5
    n = int(d * SR)
    breath = bp(noise(d), 1800, 7500) * adsr(n, 0.25, 0.8, 0.5)
    throat = formant(osc(glide(d, 70 + v * 6, 55), 'saw') * rattle(n, 22 + v * 4, 0.6), 'o') * adsr(n, 0.2, 0.6, 0.6)
    spit = hp(noise(d), 4000) * env(n, 0.002, 0.04) * 0.5
    return reverb(mix(breath * 1.1, throat * 0.6, spit), 0.25, 1.0)


def screech(v):
    d = 1.9
    n = int(d * SR)
    t = T(d)
    f = glide(d, 700 + v * 90, 1500 + v * 120) * (1 + 0.04 * np.sin(2 * np.pi * 9 * t))
    shriek = formant(osc(f, 'saw'), 'e') + formant(osc(f * 1.5, 'saw'), 'a') * 0.5
    grit = bp(noise(d), 2500, 9000) * 0.5
    low = formant(osc(glide(d, 160, 120), 'saw') * rattle(n, 30, 0.5), 'a') * 0.5
    x = drive(mix(shriek, grit, low) * adsr(n, 0.04, 0.85, 0.7), 2.5)
    return reverb(x, 0.35, 1.6)


def step(v):
    d = 0.35
    n = int(d * SR)
    thud = boom(d, 85 - v * 5, 42, 0.07)
    click = bell(d, 2400 + v * 300, partials=(1, 1.7, 2.9), decay=0.03) * 0.5
    scrape = bp(noise(d), 600, 2500) * env(n, 0.003, 0.06) * 0.4
    return reverb(mix(thud * 1.4, click, scrape), 0.2, 0.6)


def vent_thump(v):
    d = 0.9
    n = int(d * SR)
    hit = boom(d, 120 + v * 20, 50, 0.12)
    ring = bell(d, 180 + v * 40, partials=(1, 2.3, 3.7, 5.1), decay=0.35) * 0.6
    scrape = bp(noise(d), 300, 1800) * env(n, 0.01, 0.25) * 0.5
    return reverb(lp(mix(hit * 1.3, ring, scrape), 3500), 0.4, 1.2)


def sniff(v):
    d = 1.1
    out_ = np.zeros(int(d * SR))
    for i in range(3 + v):
        s = int((0.05 + i * 0.22) * SR)
        b = bp(noise(0.14), 900, 3200) * env(int(0.14 * SR), 0.02, 0.05)
        out_[s:s + len(b)] += b[:len(out_) - s]
    return reverb(out_, 0.2, 0.6)


def kill():
    d = 2.2
    n = int(d * SR)
    s = pad(screech(2) * 0.7, d)
    crunch = np.zeros(n)
    for at in (1.55, 1.95):
        k = int(at * SR)
        c = lp(crackle(0.4, 300), 3000) * 2 + boom(0.4, 140, 40, 0.08) * 1.5
        crunch[k:k + len(c)] += c[:n - k]
    snap = np.zeros(n)
    k = int(1.52 * SR)
    sn = hp(noise(0.05), 2000) * env(int(0.05 * SR), 0.0005, 0.01)
    snap[k:k + len(sn)] += sn * 2
    return reverb(mix(s, crunch, snap), 0.3, 1.2)


def growl(v):
    d = 2.2
    n = int(d * SR)
    purr = formant(osc(58 + v * 7, 'saw') * rattle(n, 16 + v * 5, 0.75), 'o') * adsr(n, 0.3, 0.8, 0.6)
    clicks = np.zeros(n)
    for _ in range(9):
        k = int(R.random() * (n - 2000))
        c = bell(0.04, 3000 + R.random() * 2000, partials=(1, 1.5), decay=0.01)
        clicks[k:k + len(c)] += c * 0.5
    return reverb(mix(purr, clicks, bp(noise(d), 1500, 5000) * 0.08), 0.3, 1.2)


def flinch(v):
    d = 1.2
    n = int(d * SR)
    f = glide(d, 1300 + v * 150, 900)
    x = formant(osc(f, 'saw'), 'e') + bp(noise(d), 3000, 9000) * 0.6
    return reverb(drive(x * adsr(n, 0.01, 0.8, 0.5), 3), 0.3, 1.2)


def drop():
    d = 1.4
    n = int(d * SR)
    thud = boom(d, 95, 38, 0.18)
    clang = bell(d, 230, partials=(1, 2.1, 3.4, 4.9, 6.3), decay=0.5) * 0.7
    tail = pad(np.zeros(int(0.35 * SR)), 0.35)
    h = np.concatenate([tail, hiss(1) * 0.6])
    return reverb(mix(thud * 1.5, clang, h), 0.35, 1.4)


# ------------------------------------------------------------------ devices
def tracker_ping():
    d = 0.35
    n = int(d * SR)
    x = osc(glide(d, 900, 860)) * env(n, 0.003, 0.07)
    return reverb(x, 0.3, 0.5)


def tracker_beep():
    d = 0.28
    n = int(d * SR)
    one = osc(1500.0 * np.ones(int(0.06 * SR)), 'pulse') * 0.6
    x = np.zeros(n)
    x[:len(one)] += one
    k = int(0.11 * SR)
    x[k:k + len(one)] += one
    return lp(x, 5000)


def flamethrower():
    d = 0.75
    n = int(d * SR)
    roar = lp(brown(d), 900) * 1.4 + bp(noise(d), 300, 3000) * 0.5
    x = roar * (0.85 + 0.15 * np.sin(2 * np.pi * 13 * T(d))) + crackle(d, 120) * 0.4
    return x * adsr(n, 0.03, 0.9, 0.08)


def flame_empty():
    d = 0.4
    x = np.zeros(int(d * SR))
    for at in (0.0, 0.15):
        c = bell(0.06, 3500, partials=(1, 2.2), decay=0.01) + hp(noise(0.06), 2000) * env(int(0.06 * SR), 0.001, 0.01)
        k = int(at * SR)
        x[k:k + len(c)] += c
    return x


def noisemaker():
    d = 0.6
    x = np.zeros(int(d * SR))
    for i in range(3):
        b = osc(1900.0 * np.ones(int(0.09 * SR)), 'pulse') * 0.7
        k = int(i * 0.17 * SR)
        x[k:k + len(b)] += b
    return reverb(lp(x, 6000), 0.25, 0.7)


def flare():
    d = 1.3
    n = int(d * SR)
    fizz = hp(noise(d), 2500) * adsr(n, 0.02, 0.6, 0.4) + crackle(d, 200) * 0.6
    pop = boom(0.2, 300, 100, 0.03)
    return mix(fizz, pop)


def medkit():
    d = 0.9
    n = int(d * SR)
    rip = bp(noise(d), 1500, 6000) * rattle(n, 45, 0.9) * env(n, 0.01, 0.25)
    spray = hp(noise(d), 3000) * np.concatenate([np.zeros(n // 2), adsr(n - n // 2, 0.02, 0.6, 0.1)]) * 0.6
    return mix(rip, spray)


def flashlight():
    d = 0.12
    return bell(d, 2600, partials=(1, 1.9), decay=0.008) + hp(noise(d), 3000) * env(int(d * SR), 0.0005, 0.006)


# ------------------------------------------------------------------ station
def door():
    d = 1.3
    n = int(d * SR)
    hiss_ = bp(noise(d), 1500, 8000) * adsr(n, 0.05, 0.7, 0.4)
    motor = lp(osc(glide(d, 90, 70), 'saw'), 400) * adsr(n, 0.05, 0.5, 0.3) * 0.5
    clunk = np.zeros(n)
    c = boom(0.3, 110, 50, 0.05) + bell(0.3, 420, decay=0.08) * 0.4
    k = int(1.0 * SR)
    clunk[k:k + len(c)] += c[:n - k]
    return reverb(mix(hiss_, motor, clunk * 1.3), 0.3, 0.9)


def locker_snd():
    d = 0.7
    n = int(d * SR)
    clang = bell(d, 310, partials=(1, 2.4, 3.9, 5.6), decay=0.18)
    latch = hp(noise(d), 2000) * env(n, 0.001, 0.02)
    return reverb(mix(clang, latch, boom(d, 90, 50, 0.05) * 0.6), 0.3, 0.7)


def save_beep():
    d = 0.22
    return osc(1250.0 * np.ones(int(d * SR))) * env(int(d * SR), 0.004, 0.06) + osc(2500.0 * np.ones(int(d * SR))) * env(int(d * SR), 0.004, 0.03) * 0.3


def save_done():
    d = 0.8
    x = np.zeros(int(d * SR))
    for i, f in enumerate((880, 1175, 1568)):
        b = osc(f * np.ones(int(0.2 * SR))) * env(int(0.2 * SR), 0.005, 0.08)
        k = int(i * 0.12 * SR)
        x[k:k + len(b)] += b
    return reverb(x, 0.3, 0.6)


def terminal():
    d = 0.8
    x = np.zeros(int(d * SR))
    for i in range(10):
        f = R.choice([1200, 1600, 2000, 2400])
        b = osc(f * np.ones(int(0.035 * SR)), 'pulse') * 0.4
        k = int(i * 0.07 * SR)
        x[k:k + len(b)] += b
    return lp(x, 5000)


def station_hum():
    d = 4.0
    n = int(d * SR)
    t = T(d)
    hum = osc(50 * np.ones(n)) + osc(100 * np.ones(n)) * 0.5 + lp(osc(150 * np.ones(n), 'saw'), 600) * 0.15
    air = lp(brown(d), 300) * 0.5
    return (hum * (0.8 + 0.2 * np.sin(2 * np.pi * 0.3 * t)) + air) * adsr(n, 0.8, 0.9, 1.2)


def creak(v):
    d = 2.2
    n = int(d * SR)
    src = noise(d) * rattle(n, 30 + v * 15, 0.85)
    x = sweep_bp(src, 300 + v * 80, 700 + v * 150, q=1.08, blocks=48)
    groan = lp(osc(glide(d, 70, 55), 'saw'), 300) * 0.4
    return reverb(mix(x * 1.5, groan) * adsr(n, 0.2, 0.8, 0.6), 0.45, 2.0)


def alarm():
    d = 3.0
    n = int(d * SR)
    t = T(d)
    f = np.where((t * 1.5) % 1 < 0.5, 620.0, 470.0)
    x = lp(osc(f, 'saw'), 1500) * adsr(n, 0.3, 0.8, 0.8)
    return reverb(lp(x, 1200), 0.6, 2.5)


def station_vent():
    d = 3.0
    n = int(d * SR)
    air = bp(brown(d), 80, 600) * 1.3 + bp(noise(d), 800, 2500) * 0.15
    ping = np.zeros(n)
    k = int(R.random() * SR * 1.5)
    b = bell(1.0, 260 + R.random() * 200, decay=0.3) * 0.4
    ping[k:k + len(b)] += b[:n - k]
    return reverb(mix(air * adsr(n, 0.6, 0.8, 0.8), ping), 0.4, 1.5)


def light_buzz():
    d = 0.5
    n = int(d * SR)
    return lp(osc(120 * np.ones(n), 'saw'), 2500) * rattle(n, 60, 0.6) * adsr(n, 0.005, 0.8, 0.1) + crackle(d, 40) * 0.3


# ------------------------------------------------------------------ fear
def stinger():
    d = 2.8
    n = int(d * SR)
    t = T(d)
    cluster = sum(lp(osc(f * (1 + 0.006 * np.sin(2 * np.pi * (5 + i) * t)), 'saw'), 3500) for i, f in enumerate((233, 247, 262, 349, 370, 523, 554)))
    sfz = np.minimum(1, t / 0.06) * (0.35 + 0.65 * np.exp(-t * 2.2))
    trem = 0.75 + 0.25 * np.sin(2 * np.pi * 14 * t)
    hit = boom(d, 70, 30, 0.6) * 2
    shriek = formant(osc(glide(d, 1700, 1200), 'saw'), 'e') * env(n, 0.05, 0.4) * 0.4
    return reverb(drive(mix(cluster * sfz * trem * 0.25, hit, shriek), 1.5), 0.4, 2.2)


def heartbeat():
    d = 0.7
    x = np.zeros(int(d * SR))
    for at, a in ((0.0, 1.0), (0.22, 0.7)):
        b = boom(0.25, 60, 35, 0.06) * a
        k = int(at * SR)
        x[k:k + len(b)] += b
    return lp(x, 300)


def breath_hold():
    d = 0.9
    n = int(d * SR)
    return lp(bp(noise(d), 300, 1800), 1200) * adsr(n, 0.3, 0.5, 0.4) * 0.6


def gasp():
    d = 0.8
    n = int(d * SR)
    x = formant(bp(noise(d), 300, 5000), 'a') * adsr(n, 0.02, 0.7, 0.35)
    return mix(x * 1.4, bp(noise(d), 2000, 6000) * env(n, 0.01, 0.2) * 0.5)


def build():
    for v in (1, 2, 3):
        out('xeno_hiss', f'xeno_hiss{v}', hiss(v))
        out('xeno_step', f'xeno_step{v}', step(v), -4)
        out('xeno_vent', f'xeno_vent{v}', vent_thump(v))
    for v in (1, 2):
        out('xeno_screech', f'xeno_screech{v}', screech(v))
        out('xeno_sniff', f'xeno_sniff{v}', sniff(v), -3)
        out('xeno_growl', f'xeno_growl{v}', growl(v))
        out('xeno_flinch', f'xeno_flinch{v}', flinch(v))
        out('station_creak', f'station_creak{v}', creak(v), -4)
    out('xeno_kill', 'xeno_kill', kill())
    out('xeno_drop', 'xeno_drop', drop())
    out('tracker_ping', 'tracker_ping', tracker_ping(), -6)
    out('tracker_beep', 'tracker_beep', tracker_beep(), -5)
    out('flamethrower', 'flamethrower', flamethrower())
    out('flame_empty', 'flame_empty', flame_empty(), -4)
    out('noisemaker', 'noisemaker', noisemaker(), -3)
    out('flare', 'flare', flare(), -3)
    out('medkit', 'medkit', medkit(), -4)
    out('flashlight', 'flashlight', flashlight(), -6)
    out('door', 'door', door(), -2)
    out('locker', 'locker', locker_snd(), -2)
    out('save_beep', 'save_beep', save_beep(), -6)
    out('save_done', 'save_done', save_done(), -4)
    out('terminal', 'terminal', terminal(), -8)
    out('station_hum', 'station_hum', station_hum(), -6)
    out('station_alarm', 'station_alarm', alarm(), -8)
    out('station_vent', 'station_vent', station_vent(), -5)
    out('light_buzz', 'light_buzz', light_buzz(), -8)
    out('stinger', 'stinger', stinger())
    out('heartbeat', 'heartbeat', heartbeat(), -2)
    out('breath_hold', 'breath_hold', breath_hold(), -6)
    out('gasp', 'gasp', gasp(), -2)
    return SOUNDS
