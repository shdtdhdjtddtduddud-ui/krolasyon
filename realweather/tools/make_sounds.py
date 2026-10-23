"""Gerçekçi hava sesleri: sayısal sentez (numpy) + ffmpeg ile OGG Vorbis.

Döngüler frekans uzayında üretilir (rastgele fazlı spektrum -> ters FFT), bu yüzden kendiliğinden kesintisiz döner;
rüzgâr esintileri gibi modülasyonlar da döngü boyuna tam bölünen periyotlarla yapılır.
"""
import os, subprocess, sys, tempfile, wave
import numpy as np

SR = 44100
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'realweather', 'sounds')
rng = np.random.default_rng(1234)


def shaped(n, gain):
    """Spektrumu gain(f) ile biçimlenmiş, döngüsel gürültü."""
    f = np.fft.rfftfreq(n, 1 / SR)
    spec = (rng.normal(size=f.size) + 1j * rng.normal(size=f.size)) * gain(f)
    spec[0] = 0
    x = np.fft.irfft(spec, n)
    return x / (np.abs(x).max() + 1e-9)


def band(f, lo, hi, slope=2.0):
    """Yumuşak bant geçiren eğri."""
    lo_g = 1 / (1 + (lo / np.maximum(f, 1)) ** (2 * slope))
    hi_g = 1 / (1 + (f / hi) ** (2 * slope))
    return lo_g * hi_g


def lfo(n, cycles, phase=0.0):
    t = np.arange(n) / n
    return np.sin(2 * np.pi * (cycles * t + phase))


def smooth_noise(n, cycles, seed_scale=1.0):
    """Döngüsel yavaş rastgele eğri (0..1)."""
    k = np.arange(1, cycles + 1)
    t = np.arange(n) / n
    y = np.zeros(n)
    for c in k:
        y += rng.normal() / c ** 1.2 * np.sin(2 * np.pi * (c * t + rng.random()))
    y = (y - y.min()) / (y.max() - y.min() + 1e-9)
    return y


def fft_filter(x, gain):
    f = np.fft.rfftfreq(x.size, 1 / SR)
    return np.fft.irfft(np.fft.rfft(x) * gain(f), x.size)


def drops(n, rate, dur_ms=(2, 9), freq=(1500, 7000), amp=(0.2, 1.0)):
    """Damlalar: kısa, sönümlenen, rezonanslı tıkırtılar (döngü sonunda başa sarar)."""
    y = np.zeros(n)
    count = int(rate * n / SR)
    for _ in range(count):
        p = rng.integers(0, n)
        d = int(SR * rng.uniform(*dur_ms) / 1000)
        t = np.arange(d) / SR
        fr = np.exp(rng.uniform(np.log(freq[0]), np.log(freq[1])))
        env = np.exp(-t * rng.uniform(300, 900))
        s = np.sin(2 * np.pi * fr * t * (1 - 0.3 * t * 40)) * env * rng.uniform(*amp)
        s += rng.normal(size=d) * env * 0.4 * rng.uniform(*amp)
        idx = (p + np.arange(d)) % n
        y[idx] += s
    return y


def norm(x, peak=0.85):
    x = x - x.mean()
    return x / (np.abs(x).max() + 1e-9) * peak


def save(name, x, stereo=False):
    os.makedirs(OUT, exist_ok=True)
    x = np.clip(x, -1, 1)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, 'a.wav')
        with wave.open(w, 'wb') as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(SR)
            f.writeframes((x * 32767).astype(np.int16).tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-ac', '1', '-c:a', 'libvorbis', '-q:a', '4',
                        os.path.join(OUT, name + '.ogg')], check=True)


L = 12 * SR  # döngü boyu

# ------------------------------------------------------------------ yağmur
def rain_heavy():
    hiss = shaped(L, lambda f: band(f, 350, 9000, 1.5) * (1 + 0.6 * band(f, 2500, 6000)) / np.sqrt(np.maximum(f, 50) / 400))
    body = shaped(L, lambda f: band(f, 120, 900, 1.2))
    d = drops(L, 900, freq=(1200, 6000), amp=(0.05, 0.4)) + drops(L, 120, dur_ms=(6, 20), freq=(300, 1200), amp=(0.1, 0.5))
    surge = 0.82 + 0.18 * smooth_noise(L, 7)
    return norm((hiss * 0.55 + body * 0.35) * surge + d * 0.35, 0.8)


def rain_light():
    hiss = shaped(L, lambda f: band(f, 800, 9000, 1.5) / np.sqrt(np.maximum(f, 50) / 800))
    d = drops(L, 260, freq=(1800, 8000), amp=(0.05, 0.6)) + drops(L, 40, dur_ms=(8, 25), freq=(500, 1600), amp=(0.1, 0.6))
    return norm(hiss * 0.25 + d * 0.7, 0.7)


def rain_indoor():
    x = rain_heavy()
    x = fft_filter(x, lambda f: 1 / (1 + (f / 650) ** 4))
    thumps = drops(L, 30, dur_ms=(20, 60), freq=(80, 220), amp=(0.2, 0.9))
    return norm(x * 0.8 + thumps * 0.5, 0.7)


def rain_leaves():
    """Yaprakların üstüne düşen yağmur: daha yoğun, parlak tıkırtılar."""
    hiss = shaped(L, lambda f: band(f, 1500, 11000, 1.3))
    d = drops(L, 1600, dur_ms=(1, 5), freq=(2500, 9000), amp=(0.05, 0.5))
    return norm(hiss * 0.3 + d * 0.7, 0.7)


# ------------------------------------------------------------------ rüzgâr
def howl(n, base, cycles, depth):
    """Uluyan rüzgâr: yavaşça kayan dar bantlı gürültü (zaman uzayında birkaç dar banttan karıştırılır)."""
    out = np.zeros(n)
    for k, fr in enumerate((base, base * 1.48, base * 2.1)):
        nb = shaped(n, lambda f, fr=fr: np.exp(-((f - fr) / (fr * 0.06)) ** 2))
        out += nb * (0.6 + 0.4 * smooth_noise(n, cycles + k)) ** 2 * (1.0 / (k + 1))
    return out * depth


def wind_soft():
    rumble = shaped(L, lambda f: band(f, 40, 700, 1.0) / np.maximum(f, 20) ** 0.5)
    air = shaped(L, lambda f: band(f, 400, 3000, 1.2))
    gust = 0.35 + 0.65 * smooth_noise(L, 5) ** 1.5
    return norm((rumble * 0.8 + air * 0.25) * gust + howl(L, 420, 4, 0.12) * gust, 0.75)


def wind_strong():
    rumble = shaped(L, lambda f: band(f, 30, 900, 1.0) / np.maximum(f, 20) ** 0.4)
    air = shaped(L, lambda f: band(f, 500, 5000, 1.3))
    gust = 0.25 + 0.75 * smooth_noise(L, 9) ** 1.3
    return norm((rumble * 0.75 + air * 0.4) * gust + howl(L, 360, 7, 0.45) * gust ** 1.5, 0.85)


def blizzard():
    w = wind_strong()
    hiss = shaped(L, lambda f: band(f, 2500, 12000, 1.5))
    gust = 0.4 + 0.6 * smooth_noise(L, 11) ** 1.2
    whistle = howl(L, 780, 9, 0.35)
    return norm(w * 0.8 + hiss * 0.35 * gust + whistle * gust, 0.85)


def sandstorm():
    roar = shaped(L, lambda f: band(f, 40, 1500, 1.0) / np.maximum(f, 20) ** 0.3)
    hiss = shaped(L, lambda f: band(f, 1500, 10000, 1.2))
    grit = drops(L, 2500, dur_ms=(0.5, 2), freq=(3000, 10000), amp=(0.02, 0.35))
    gust = 0.35 + 0.65 * smooth_noise(L, 8) ** 1.3
    return norm(roar * 0.6 * gust + hiss * 0.45 * gust + grit * 0.5 * gust, 0.85)


def leaves():
    base = shaped(L, lambda f: band(f, 1200, 9000, 1.3))
    flutter = 0.5 + 0.5 * np.abs(shaped(L, lambda f: band(f, 2, 14, 1.0)))
    gust = 0.2 + 0.8 * smooth_noise(L, 6) ** 2
    crackle = drops(L, 500, dur_ms=(1, 4), freq=(2000, 8000), amp=(0.02, 0.3))
    return norm(base * flutter * gust * 0.7 + crackle * gust, 0.7)


# ------------------------------------------------------------------ gece böcekleri
def crickets():
    y = np.zeros(L)
    # her biri kendi perdesi ve ritmiyle birkaç cırcır böceği (ritim döngüye tam sığar)
    for c in range(7):
        fr = rng.uniform(3900, 5200)
        period = L / int(L / (SR * rng.uniform(0.45, 0.9)))
        amp = rng.uniform(0.2, 1.0) ** 2
        pulses = rng.integers(2, 5)
        start = rng.uniform(0, period)
        t_p = np.arange(int(SR * 0.012)) / SR
        pulse = np.sin(2 * np.pi * fr * t_p) * np.sin(np.pi * t_p / t_p[-1]) ** 2
        k = 0
        while start + k * period < L:
            for q in range(pulses):
                p = int(start + k * period + q * SR * 0.028)
                idx = (p + np.arange(pulse.size)) % L
                y[idx] += pulse * amp
            k += 1
    bed = shaped(L, lambda f: band(f, 3000, 7000, 2.0)) * 0.03
    return norm(y + bed, 0.6)


# ------------------------------------------------------------------ gök gürültüsü
def thunder(near, seed):
    r = np.random.default_rng(seed)
    n = int(SR * (7.5 if near else 9.0))
    t = np.arange(n) / SR
    rumble = np.zeros(n)
    # birbirini izleyen gürleme dalgaları
    for k in range(r.integers(4, 8)):
        t0 = r.uniform(0.0, 0.8) + k * r.uniform(0.35, 0.9)
        dec = r.uniform(0.6, 1.8)
        env = np.where(t < t0, 0, (1 - np.exp(-(t - t0) * r.uniform(6, 14))) * np.exp(-(t - t0) / dec))
        rumble += env * r.uniform(0.4, 1.0)
    rumble /= rumble.max() + 1e-9
    cut = 900 if near else 260
    noise = shaped(n, lambda f: band(f, 18, cut, 1.0) / np.maximum(f, 15) ** 0.7)
    x = noise * rumble
    if near:
        # yakın yıldırım: çatırtı ve yırtılma
        crack_env = np.exp(-t / 0.05) * (t < 0.5)
        crack = shaped(n, lambda f: band(f, 300, 12000, 1.0)) * crack_env
        crackle = drops(n, 900, dur_ms=(0.5, 3), freq=(1000, 9000), amp=(0.1, 1.0)) * np.exp(-t / 0.35)
        x = x * 0.9 + crack * 0.9 + crackle * 0.5
        lead = int(SR * 0.02)
    else:
        lead = int(SR * 0.3)
    x = np.concatenate([np.zeros(lead), x])
    fade = np.linspace(1, 0, int(SR * 0.6))
    x[-fade.size:] *= fade
    return norm(x, 0.95)


if __name__ == '__main__':
    for name, fn in (('rain_heavy', rain_heavy), ('rain_light', rain_light), ('rain_indoor', rain_indoor), ('rain_leaves', rain_leaves),
                     ('wind_soft', wind_soft), ('wind_strong', wind_strong), ('blizzard', blizzard), ('sandstorm', sandstorm),
                     ('leaves', leaves), ('crickets', crickets)):
        save(name, fn())
        print('ok', name)
    for i in range(3):
        save('thunder_near%d' % (i + 1), thunder(True, 100 + i))
        save('thunder_far%d' % (i + 1), thunder(False, 200 + i))
    print('thunder ok')
