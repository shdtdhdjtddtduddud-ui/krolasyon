"""Sound design for the maze: grinding walls, the Box, Grievers, beetles and story stingers."""
import json, os
import numpy as np
from sndlib import *


def grind(d, base=40, roughness=1.0):
    """two huge stone slabs scraping against each other"""
    n = int(d * SR)
    t = T(d)
    rum = lp(brown(d), 120) * 1.4
    scrape = bp(noise(d), 180, 900) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 3.3 * t + np.sin(2 * np.pi * 0.7 * t) * 3)))
    chatter = rattle(n, 17, 0.6) * bp(noise(d), 600, 2400) * 0.25 * roughness
    tone = osc(base * (1 + 0.04 * np.sin(2 * np.pi * 0.5 * t)), 'saw')
    tone = lp(tone, 260) * 0.5
    return rum + scrape * 0.7 + chatter + tone


def wall_grind():
    d = 4.5
    n = int(d * SR)
    x = grind(d) * adsr(n, 0.6, 0.85, 1.2)
    return reverb(x, 0.45, 2.0)


def door_close():
    d = 7.0
    n = int(d * SR)
    t = T(d)
    x = grind(d, 34, 1.4) * adsr(n, 0.8, 0.9, 0.6)
    for k in range(6):
        p = int((0.9 + k * 1.0) * SR)
        b = boom(0.5, 140, 60, 0.12) * 0.5 + pad(bp(noise(0.3), 800, 3000) * env(int(0.3 * SR), 0.001, 0.05) * 0.4, 0.5)
        x[p:p + len(b)] += b[:max(0, n - p)]
    return reverb(x, 0.5, 2.5)


def door_slam():
    d = 3.5
    x = boom(d, 90, 28, 0.9) * 1.6 + lp(noise(d), 300) * env(int(d * SR), 0.002, 0.4) * 1.2
    x += bp(noise(d), 1200, 5000) * env(int(d * SR), 0.001, 0.06) * 0.5
    return reverb(x, 0.6, 3.0)


def door_open():
    d = 6.0
    n = int(d * SR)
    x = grind(d, 44, 1.0) * adsr(n, 1.2, 0.85, 1.5)
    hiss = bp(noise(d), 2000, 6000) * env(n, 0.05, 0.8) * 0.3
    return reverb(x + hiss, 0.5, 2.5)


def box_alarm():
    d = 1.8
    t = T(d)
    f = 620 + 280 * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 2.2 * t)))
    x = osc(f, 'pulse') * 0.6 + osc(f * 1.5, 'saw') * 0.2
    x = lp(x, 3500) * adsr(len(t), 0.01, 1.0, 0.1)
    return reverb(x, 0.4, 1.5)


def box_rise():
    d = 13.0
    n = int(d * SR)
    t = T(d)
    motor = osc(48 + 6 * np.sin(2 * np.pi * 0.3 * t), 'saw')
    motor = lp(motor, 300) * 0.8
    chain = rattle(n, 9, 0.85) * bp(noise(d), 900, 4000) * 0.35
    squeal = osc(1400 + 300 * np.sin(2 * np.pi * 0.13 * t), 'sine') * (np.sin(2 * np.pi * 0.21 * t) > 0.8) * 0.12
    x = (motor + chain + squeal + lp(brown(d), 90)) * adsr(n, 0.8, 0.9, 1.0)
    return reverb(x, 0.35, 1.8)


def box_arrive():
    d = 2.5
    x = boom(d, 120, 40, 0.5) * 1.2
    for k, dt in enumerate((0.0, 0.18, 0.31)):
        p = int(dt * SR)
        c = bell(0.8, 220 + k * 40, decay=0.25) * 0.4
        x[p:p + len(c)] += c[:len(x) - p]
    x += bp(noise(d), 300, 1500) * env(len(x), 0.001, 0.3) * 0.6
    return reverb(x, 0.4, 1.6)


def griever_ambient(v):
    d = 2.8
    n = int(d * SR)
    t = T(d)
    whir = osc(180 + 40 * np.sin(2 * np.pi * (3 + v) * t) + 25 * np.sin(2 * np.pi * 0.7 * t), 'saw')
    whir = bp(whir, 150, 1600) * 0.4
    clicks = crackle(d, 35 + v * 10) * 0.8
    moan = formant(osc(glide(d, 95 - v * 10, 70 - v * 6), 'saw'), 'u') * 0.9
    moan *= np.clip(np.sin(np.pi * t / d), 0, 1) ** 0.6
    breath = bp(noise(d), 400, 1200) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.9 * t)) * 0.25
    return reverb((whir + clicks + moan + breath) * adsr(n, 0.2, 0.9, 0.5), 0.4, 1.4)


def griever_hurt(v):
    d = 0.8
    n = int(d * SR)
    shriek = formant(osc(glide(d, 520 + v * 60, 260), 'saw'), 'e') * 1.1
    metal = bell(d, 700 + v * 90, decay=0.15) * 0.4
    return reverb((shriek + metal + crackle(d, 80) * 0.5) * env(n, 0.005, 0.35), 0.3)


def griever_death():
    d = 3.2
    n = int(d * SR)
    t = T(d)
    wail = formant(osc(glide(d, 380, 60), 'saw'), 'o') * env(n, 0.02, 1.4, 1.2)
    sparks = crackle(d, 140) * env(n, 0.01, 1.0) * 0.8
    winddown = osc(glide(d, 300, 20), 'saw')
    winddown = lp(winddown, 600) * env(n, 0.05, 1.8, 1.0) * 0.6
    thud = boom(d, 90, 30, 0.3)
    p = int(1.6 * SR)
    th = np.zeros(n); th[p:] = thud[:n - p]
    return reverb(wail + sparks + winddown + th, 0.45, 2.0)


def griever_step(v):
    d = 0.25
    n = int(d * SR)
    click = bp(noise(d), 2500 + v * 400, 7000) * env(n, 0.0005, 0.02) * 1.2
    clank = bell(d, 900 + v * 130, decay=0.05) * 0.4
    return click + clank + lp(noise(d), 200) * env(n, 0.001, 0.04) * 0.5


def griever_sting():
    d = 0.9
    n = int(d * SR)
    whoosh = sweep_bp(noise(d), 300, 4000, 1.8) * env(n, 0.15, 0.2) * 1.0
    stab = bp(noise(d), 1500, 6000) * env(n, 0.001, 0.03)
    p = int(0.35 * SR)
    s = np.zeros(n); s[p:] = stab[:n - p] * 1.4
    hiss = bp(noise(d), 3000, 9000) * env(n, 0.3, 0.3) * 0.3
    return whoosh + s + hiss


def griever_roll():
    d = 1.2
    n = int(d * SR)
    t = T(d)
    spin = rattle(n, 24, 0.9) * bp(noise(d), 300, 2500) * 1.0
    whir = osc(glide(d, 90, 260), 'saw') * 0.3
    return reverb((spin + lp(whir, 900)) * env(n, 0.05, 0.6), 0.3)


def beetle_scuttle(v):
    d = 0.35
    n = int(d * SR)
    ticks = np.zeros(n)
    for k in range(6 + v * 2):
        p = int(k * n / (7 + v * 2))
        L = 200
        ticks[p:p + L] += bp(R.standard_normal(L), 3000, 9000) * np.exp(-np.arange(L) / 40)
    return ticks + osc(np.full(n, 4200 + v * 300), 'sine') * env(n, 0.001, 0.05) * 0.1


def story_sting():
    d = 4.0
    n = int(d * SR)
    t = T(d)
    low = boom(d, 60, 40, 1.4) * 1.2
    chord = sum(osc(np.full(n, f), 'saw') for f in (110, 130.8, 164.8, 220))
    chord = lp(chord, 1200) * adsr(n, 0.4, 0.7, 1.5) * 0.25
    high = osc(np.full(n, 880), 'sine') * env(n, 0.8, 1.6) * 0.15
    return reverb(low + chord + high, 0.5, 2.5)


def maze_ambient(v):
    d = 6.0
    n = int(d * SR)
    t = T(d)
    wind = sweep_bp(noise(d), 300 + v * 100, 900, 2.0) * (0.4 + 0.3 * np.sin(2 * np.pi * 0.2 * t))
    distant = lp(grind(d, 30, 0.4), 200) * 0.4 * env(n, 1.5, 2.0)
    leaves = crackle(d, 20) * 0.2
    return reverb((wind + distant + leaves) * adsr(n, 1.5, 0.8, 2.0), 0.6, 3.0)


def serum_inject():
    d = 1.4
    n = int(d * SR)
    click = bp(noise(0.05), 1500, 6000) * 0.8
    hiss = bp(noise(d), 2500, 8000) * env(n, 0.05, 0.4) * 0.6
    gulp = osc(glide(d, 300, 900), 'sine') * env(n, 0.1, 0.5) * 0.25
    x = hiss + gulp
    x[:len(click)] += click
    return reverb(x, 0.3)


def terminal_beep():
    d = 1.0
    n = int(d * SR)
    x = np.zeros(n)
    for k, f in enumerate((880, 1175, 1568)):
        p = int(k * 0.12 * SR)
        b = osc(np.full(int(0.1 * SR), f), 'pulse') * 0.4
        x[p:p + len(b)] += b
    return reverb(x, 0.2)


def escape():
    d = 8.0
    n = int(d * SR)
    t = T(d)
    rise = sum(osc(glide(d, f, f * 2), 'saw') for f in (110, 165, 220))
    rise = lp(rise, 2000) * adsr(n, 2.0, 0.8, 2.0) * 0.25
    bells = sum(bell(d, f, decay=2.0) for f in (523.3, 659.3, 784.0)) * 0.3
    p = int(3.0 * SR)
    b = np.zeros(n); b[p:] = bells[:n - p]
    wind = sweep_bp(noise(d), 200, 3000, 2.0) * env(n, 2.5, 3.0) * 0.4
    return reverb(rise + b + wind, 0.5, 3.0)


def wind_gust(v):
    d = 5.0
    n = int(d * SR)
    t = T(d)
    w = sweep_bp(noise(d), 200 + v * 80, 1200 + v * 300, 2.2) * np.sin(np.pi * t / d) ** 1.5
    whistle = osc(700 + v * 150 + 120 * np.sin(2 * np.pi * 0.3 * t), 'sine') * np.sin(np.pi * t / d) ** 3 * 0.06
    rustle = crackle(d, 50) * np.sin(np.pi * t / d) ** 2 * 0.35
    return reverb(w + whistle + rustle, 0.5, 2.0)


def heartbeat():
    d = 0.9
    n = int(d * SR)
    x = np.zeros(n)
    for dt, a in ((0.0, 1.0), (0.22, 0.7)):
        p = int(dt * SR)
        b = boom(0.25, 70, 38, 0.06) * a
        x[p:p + len(b)] += b[:n - p]
    return lp(x, 300)


def distant_cry(v):
    d = 4.0
    n = int(d * SR)
    t = T(d)
    cry = formant(osc(glide(d, 420 + v * 60, 180), 'saw') * (1 + 0.1 * np.sin(2 * np.pi * 9 * t)), 'u')
    cry *= env(n, 0.4, 1.4, 1.0)
    clicks = crackle(d, 25) * env(n, 0.8, 1.6) * 0.4
    x = lp(cry * 0.8 + clicks, 2200)
    return reverb(x, 0.75, 3.5)


def birds(v):
    d = 3.0
    n = int(d * SR)
    x = np.zeros(n)
    for k in range(4 + v * 2):
        p = int(R.random() * (d - 0.4) * SR)
        f0 = 2400 + R.random() * 1800
        L = 0.06 + R.random() * 0.12
        chirp = osc(glide(L, f0, f0 * (0.7 + R.random() * 0.8)), 'sine') * env(int(L * SR), 0.005, L * 0.6)
        x[p:p + len(chirp)] += chirp[:n - p] * (0.4 + R.random() * 0.6)
    return reverb(x, 0.35, 1.5)


def crickets():
    d = 4.0
    n = int(d * SR)
    t = T(d)
    chirp = osc(np.full(n, 4300.0), 'sine') * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 32 * t))) * (np.sin(2 * np.pi * 1.6 * t) > 0.2)
    chirp2 = osc(np.full(n, 3900.0), 'sine') * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 29 * t + 1))) * (np.sin(2 * np.pi * 1.3 * t + 2) > 0.3)
    return reverb((chirp + chirp2 * 0.7) * 0.4 * adsr(n, 0.3, 1.0, 0.5), 0.4, 1.5)


def tension():
    d = 8.0
    n = int(d * SR)
    t = T(d)
    drone = sum(osc(np.full(n, f) * (1 + 0.003 * np.sin(2 * np.pi * (0.1 + i * 0.07) * t)), 'saw') for i, f in enumerate((55, 58.3, 82.4)))
    drone = lp(drone, 420) * adsr(n, 2.5, 0.9, 2.5) * 0.5
    air = sweep_bp(noise(d), 600, 2400, 3.0) * adsr(n, 3.0, 0.6, 2.5) * 0.15
    return reverb(drone + air, 0.6, 3.0)


os.makedirs(OUT, exist_ok=True)
SOUNDS = {
    'wall_grind': [wall_grind()], 'door_close': [door_close()], 'door_slam': [door_slam()], 'door_open': [door_open()],
    'box_alarm': [box_alarm()], 'box_rise': [box_rise()], 'box_arrive': [box_arrive()],
    'griever_ambient': [griever_ambient(v) for v in (0, 1, 2)], 'griever_hurt': [griever_hurt(v) for v in (0, 1)],
    'griever_death': [griever_death()], 'griever_step': [griever_step(v) for v in (0, 1, 2)], 'griever_sting': [griever_sting()],
    'griever_roll': [griever_roll()], 'beetle_scuttle': [beetle_scuttle(v) for v in (0, 1)], 'story_sting': [story_sting()],
    'maze_ambient': [maze_ambient(v) for v in (0, 1)], 'serum_inject': [serum_inject()], 'terminal_beep': [terminal_beep()],
    'escape': [escape()], 'wind_gust': [wind_gust(v) for v in (0, 1, 2)], 'heartbeat': [heartbeat()],
    'distant_cry': [distant_cry(v) for v in (0, 1, 2)], 'birds': [birds(v) for v in (0, 1)], 'crickets': [crickets()],
    'tension': [tension()],
}
STREAM = {'box_rise', 'door_close', 'door_open', 'escape', 'maze_ambient', 'tension'}
index = {}
for name, variants in SOUNDS.items():
    files = []
    for i, x in enumerate(variants):
        fn = name if len(variants) == 1 else f'{name}{i + 1}'
        save(fn, x)
        files.append({"name": f"labirent:{fn}", **({"stream": True} if name in STREAM else {})})
    index[name] = {"subtitle": f"subtitles.labirent.{name}", "sounds": files}
with open(os.path.join(OUT, '..', 'sounds.json'), 'w') as f:
    json.dump(index, f, indent=2)
print('sounds written')
