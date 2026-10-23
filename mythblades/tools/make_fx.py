"""Kor Kılıcı dövüş efektleri: animasyonlu ateş dokuları (numpy) ve sentezlenmiş sesler (ffmpeg ile OGG)."""
import os, subprocess, tempfile, wave
import numpy as np
from PIL import Image

HERE = os.path.dirname(__file__)
A = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'mythblades')
TEX = os.path.join(A, 'textures', 'fx')
SND = os.path.join(A, 'sounds')
os.makedirs(TEX, exist_ok=True)
os.makedirs(SND, exist_ok=True)
rng = np.random.default_rng(31)


def save(name, rgba):
    Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), 'RGBA').save(os.path.join(TEX, name + '.png'))


def value_noise(h, w, cells_y, cells_x, tile=True):
    g = rng.random((cells_y + 1, cells_x + 1))
    if tile:
        g[-1, :] = g[0, :]
        g[:, -1] = g[:, 0]
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((w + w // cells_x, h + h // cells_y), Image.BICUBIC)
    a = np.asarray(img, float)[:h, :w] / 255
    return a


def periodic_noise(h, w, fy, fx):
    """Her iki yönde tam döşenebilen yumuşak gürültü (frekans uzayında alçak geçiren)."""
    spec = np.fft.fft2(rng.normal(size=(h, w)))
    ky = np.fft.fftfreq(h)[:, None] * h
    kx = np.fft.fftfreq(w)[None, :] * w
    g = np.exp(-((ky / fy) ** 2 + (kx / fx) ** 2))
    a = np.real(np.fft.ifft2(spec * g))
    return (a - a.min()) / (a.max() - a.min() + 1e-9)


def ramp(t):
    """Sıcaklık 0..1 -> ateş rengi ve saydamlık."""
    t = np.clip(t, 0, 1)
    stops = np.array([[0.0, 60, 8, 2, 0], [0.18, 150, 20, 4, 90], [0.38, 230, 70, 10, 200], [0.6, 255, 150, 30, 245],
                      [0.8, 255, 220, 110, 255], [1.0, 255, 255, 235, 255]])
    out = np.zeros(t.shape + (4,))
    for c in range(4):
        out[..., c] = np.interp(t, stops[:, 0], stops[:, c + 1])
    return out


# ------------------------------------------------------------------ alev animasyonu: 8x4 = 32 kare, döngüsel
def fire_sheet():
    F, S = 32, 64
    # dikeyde döşenebilir gürültü: her kare yukarı kayan pencere (32 karede tam bir dönem)
    period = S * 2
    n1 = periodic_noise(period, S, 4, 2.5)
    n2 = periodic_noise(period, S, 9, 5)
    n3 = periodic_noise(period, S, 18, 10)
    sheet = np.zeros((S * 4, S * 8, 4))
    yy, xx = np.mgrid[0:S, 0:S] / S
    for f in range(F):
        off = int(f / F * period)
        win = lambda n: n[(np.arange(S) + off) % period][:, :]
        turb = win(n1) * 0.55 + win(n2) * 0.3 + win(n3) * 0.15
        # alevin gövdesi: altta geniş, yukarı sivrilir; türbülansla kıvrılır
        cx = 0.5 + (turb - 0.5) * 0.35 * (1 - yy)
        width = 0.36 * (yy ** 0.9) + 0.02
        d = np.abs(xx - cx) / width
        body = np.clip(1 - d, 0, 1) * np.clip(yy * 1.6, 0, 1)
        heat = body * (0.55 + 0.75 * turb) * (0.4 + 0.6 * yy)
        heat = np.clip(heat * 1.3 - 0.15, 0, 1)
        col = ramp(heat)
        col[..., 3] *= np.clip(heat * 3, 0, 1)
        r, c = divmod(f, 8)
        sheet[r * S:(r + 1) * S, c * S:(c + 1) * S] = col  # üst = alevin ucu, alt = geniş gövde
    save('fire_sheet', sheet)


# ------------------------------------------------------------------ kesik izi: u yay boyunca (1 = kılıcın önü), v içten dışa
def slash():
    W, H = 256, 64
    u = np.linspace(0, 1, W)[None, :]
    v = np.linspace(0, 1, H)[:, None]
    streak = np.repeat(value_noise(H, 64, 16, 4), 4, axis=1)[:, :W] * 0.6 + value_noise(H, W, 32, 8) * 0.4
    core = np.exp(-((v - 0.78) / 0.07) ** 2)
    band = np.clip((v - 0.15) / 0.65, 0, 1) ** 1.6 * (v < 0.97)
    heat = (core * 0.9 + band * 0.55 * streak) * (u ** 0.7) * np.clip((1 - u) * 18, 0, 1) ** 0.3
    heat = np.clip(heat * 1.25, 0, 1)
    col = ramp(heat)
    col[..., 3] = np.clip(heat * 2.2, 0, 1) * 255
    save('slash', col)


def shock():
    W, H = 256, 32
    v = np.linspace(0, 1, H)[:, None]
    streak = value_noise(H, W, 8, 32)
    heat = np.exp(-((v - 0.65) / 0.18) ** 2) * (0.6 + 0.6 * streak)
    heat = np.clip(heat, 0, 1)
    col = ramp(heat)
    col[..., 3] = np.clip(heat * 1.8, 0, 1) * 255
    save('shock', col)


def glow():
    S = 64
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    d = np.sqrt((xx - S / 2) ** 2 + (yy - S / 2) ** 2) / (S / 2)
    a = np.clip(1 - d, 0, 1) ** 2.2
    img = np.zeros((S, S, 4))
    img[..., 0], img[..., 1], img[..., 2] = 255, 200 + 55 * a, 120 + 135 * a
    img[..., 3] = a * 255
    save('glow', img)


def shield():
    S = 128
    yy, xx = np.mgrid[0:S, 0:S] + 0.5
    dx, dy = (xx - S / 2) / (S / 2), (yy - S / 2) / (S / 2)
    r = np.sqrt(dx ** 2 + dy ** 2)
    a = np.arctan2(dy, dx)
    n = value_noise(S, S, 8, 8)
    swirl = 0.5 + 0.5 * np.sin(a * 5 + r * 9 + n * 4)
    rim = np.exp(-((r - 0.86) / 0.08) ** 2)
    heat = (rim * 0.95 + swirl * 0.35 * np.clip(1 - r, 0, 1)) * (r < 1)
    col = ramp(np.clip(heat, 0, 1))
    col[..., 3] = np.clip(heat * 1.6, 0, 1) * 255
    save('shield', col)


def speedlines():
    W, H = 512, 288
    yy, xx = np.mgrid[0:H, 0:W] + 0.5
    dx, dy = (xx - W / 2) / (W / 2), (yy - H / 2) / (H / 2)
    r = np.sqrt(dx ** 2 + dy ** 2)
    a = np.arctan2(dy, dx)
    alpha = np.zeros((H, W))
    for _ in range(70):
        ang = rng.uniform(-np.pi, np.pi)
        wid = rng.uniform(0.004, 0.012)
        r0 = rng.uniform(0.5, 0.9)
        da = np.abs(np.angle(np.exp(1j * (a - ang))))
        line = np.clip(1 - da / wid, 0, 1) * np.clip((r - r0) / 0.25, 0, 1)
        alpha = np.maximum(alpha, line * rng.uniform(0.5, 1))
    img = np.zeros((H, W, 4))
    img[..., 0], img[..., 1], img[..., 2] = 255, 210, 160
    img[..., 3] = alpha * 200
    save('speedlines', img)


# ------------------------------------------------------------------ sesler
SR = 44100


def write(name, x):
    x = np.clip(x / (np.abs(x).max() + 1e-9) * 0.92, -1, 1)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, 'a.wav')
        with wave.open(w, 'wb') as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(SR)
            f.writeframes((x * 32767).astype(np.int16).tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-c:a', 'libvorbis', '-q:a', '5', os.path.join(SND, name + '.ogg')], check=True)


def bandpass(x, lo, hi):
    f = np.fft.rfftfreq(x.size, 1 / SR)
    g = 1 / (1 + (lo / np.maximum(f, 1)) ** 4) / (1 + (f / hi) ** 4)
    return np.fft.irfft(np.fft.rfft(x) * g, x.size)


def sweep_noise(dur, f0, f1, q=0.5, chunk=1024):
    """Zamanla kayan bant geçiren gürültü (örtüşmeli pencerelerle)."""
    n = int(dur * SR)
    noise = rng.normal(size=n + chunk)
    out = np.zeros(n + chunk)
    win = np.hanning(chunk)
    for s in range(0, n, chunk // 2):
        t = s / n
        fc = f0 * (f1 / f0) ** t
        seg = noise[s:s + chunk] * win
        out[s:s + chunk] += bandpass(seg, fc * (1 - q), fc * (1 + q * 2))
    return out[:n]


def crackle(n, rate, amp=0.5):
    y = np.zeros(n)
    for _ in range(int(rate * n / SR)):
        p = rng.integers(0, n - 200)
        d = rng.integers(20, 160)
        y[p:p + d] += rng.normal(size=d) * np.exp(-np.arange(d) / (d / 4)) * rng.uniform(0.2, 1) * amp
    return bandpass(y, 1500, 12000)


def env(n, attack, peak, decay):
    t = np.arange(n) / SR
    return np.where(t < peak, (t / peak) ** attack, np.exp(-(t - peak) / decay))


def swing(i):
    dur = 0.42
    n = int(dur * SR)
    w = sweep_noise(dur, 500 + i * 120, 2600 + i * 300, 0.6)
    e = env(n, 2.0, 0.13 + 0.02 * i, 0.08)
    fire = bandpass(rng.normal(size=n), 200, 1500) * env(n, 1.5, 0.16, 0.12) * 0.5
    return w * e + fire + crackle(n, 120, 0.3) * e


def dash():
    dur = 0.75
    n = int(dur * SR)
    w = sweep_noise(dur, 300, 3500, 0.7) * env(n, 1.5, 0.12, 0.2)
    roar = bandpass(rng.normal(size=n), 60, 700) * env(n, 1.2, 0.1, 0.3)
    return w + roar * 0.9 + crackle(n, 300, 0.4) * env(n, 1, 0.1, 0.35)


def parry():
    dur = 1.6
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    # metalik çınlama: uyumsuz kısmi tonlar
    for f, a, d in ((1180, 1.0, 0.55), (1897, 0.7, 0.4), (2763, 0.5, 0.3), (3911, 0.35, 0.22), (5320, 0.25, 0.15), (612, 0.4, 0.7)):
        x += a * np.sin(2 * np.pi * f * t * (1 + 0.002 * np.sin(t * 30))) * np.exp(-t / d)
    hit = rng.normal(size=n) * np.exp(-t / 0.015)
    burst = bandpass(rng.normal(size=n), 200, 3000) * env(n, 1, 0.03, 0.25) * 0.6
    return x * 0.6 + hit * 0.8 + burst + crackle(n, 200, 0.5) * np.exp(-t / 0.4)


def block():
    dur = 0.6
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t / d) for f, a, d in ((420, 1.0, 0.12), (980, 0.6, 0.08), (1630, 0.3, 0.05)))
    return x + rng.normal(size=n) * np.exp(-t / 0.02) * 0.7 + bandpass(rng.normal(size=n), 100, 900) * np.exp(-t / 0.08)


def impact():
    dur = 2.4
    n = int(dur * SR)
    t = np.arange(n) / SR
    boom = np.sin(2 * np.pi * np.cumsum(90 * np.exp(-t / 0.35) + 32) / SR) * np.exp(-t / 0.6)
    blast = bandpass(rng.normal(size=n), 40, 2500) * np.exp(-t / 0.18)
    debris = crackle(n, 600, 0.6) * np.exp(-t / 0.7)
    roar = bandpass(rng.normal(size=n), 80, 900) * env(n, 1, 0.08, 0.9) * 0.5
    return boom * 1.2 + blast + debris + roar


def wave_snd():
    dur = 1.1
    n = int(dur * SR)
    w = sweep_noise(dur, 250, 1800, 0.8) * env(n, 1.3, 0.15, 0.4)
    return w + crackle(n, 500, 0.5) * env(n, 1, 0.1, 0.5) + bandpass(rng.normal(size=n), 60, 500) * env(n, 1, 0.1, 0.4) * 0.7


if __name__ == '__main__':
    fire_sheet(); slash(); shock(); glow(); shield(); speedlines()
    for i in range(3):
        write('ember_swing%d' % (i + 1), swing(i))
    write('ember_dash', dash())
    write('ember_parry', parry())
    write('ember_block', block())
    write('ember_impact', impact())
    write('ember_wave', wave_snd())
    print('fx ok')
