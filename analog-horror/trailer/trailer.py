"""Cuts the CI cinematic capture into a VHS found-footage trailer with a synthesized soundtrack.
usage: python3 trailer.py rec.mkv latest.log rec_start.txt out.mp4"""
import sys, os, re, subprocess, math, wave
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

REC, LOG, START, OUT = sys.argv[1:5]
HERE = os.path.dirname(os.path.abspath(__file__))
SND = os.path.join(HERE, '../common/src/main/resources/assets/krolasyonhorror/sounds')
FPS, W, H = 30, 960, 720
SR = 44100
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf'
FONTB = '/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf'
rng = np.random.default_rng(3)

t0 = int(open(START).read().strip()) / 1000.0
scenes = {}
for line in open(LOG, errors='ignore'):
    m = re.search(r'\[AUTOTEST\] SCENE (\w+) (\d+)', line)
    if m:
        scenes[m.group(1)] = int(m.group(2)) / 1000.0 - t0
print('scenes', {k: round(v, 2) for k, v in scenes.items()})

# (scene, offset in scene, duration, caption, sounds [(t, name, gain)])
EDIT = [
    ('hall_far', 1.0, 5.5, None, [(0, 'drone', 0.5), (2.0, 'static1', 0.25), (4.6, 'ambient2', 0.6)]),
    ('peek', 1.0, 4.5, 'BAKMADIĞINDA İZLİYOR', [(0.5, 'whisper', 0.7)]),
    ('approach', 2.0, 6.0, 'GÖZÜNÜ AYIRDIĞINDA YAKLAŞIR', [(0.3, 'step1', 0.5), (1.5, 'step2', 0.5), (2.7, 'step3', 0.5), (3.9, 'step1', 0.5), (5.1, 'step2', 0.5),
                                                          (0.0, 'heartbeat', 0.5), (1.1, 'heartbeat', 0.55), (2.1, 'heartbeat', 0.6), (3.0, 'heartbeat', 0.7), (3.8, 'heartbeat', 0.8), (4.5, 'heartbeat', 0.9), (5.2, 'heartbeat', 1.0)]),
    ('blackout', 0.0, 7.0, 'ONA BAKARKEN KIPIRDAMAZ', [(0.0, 'static2', 0.9), (1.6, 'knock', 0.8), (2.2, 'heartbeat', 1.0), (3.0, 'heartbeat', 1.0), (3.7, 'heartbeat', 1.0)]),
    ('twitch', 0.0, 5.0, None, [(0.0, 'twitch', 1.0), (2.0, 'hat_tip', 0.9)]),
    ('scream', 0.0, 3.4, 'AMA ÇOK UZUN BAKARSAN...', [(0.0, 'scream', 1.0), (2.3, 'step1', 1.0), (2.6, 'step2', 1.0), (3.0, 'jumpscare', 1.0)]),
]
EPILOGUE = [
    ('crawl', 2.0, 4.0, 'HER YERE SIĞAR', [(0.0, 'twitch', 0.6), (0.4, 'step1', 0.5), (1.2, 'step2', 0.5), (2.0, 'step3', 0.5), (2.8, 'step1', 0.5)]),
    ('emerge', 0.0, 4.0, None, [(0.0, 'emerge', 1.0)]),
]


def font(sz, bold=False):
    return ImageFont.truetype(FONTB if bold else FONT, sz)


def read_clip(start, dur):
    cmd = ['ffmpeg', '-loglevel', 'error', '-ss', f'{max(0, start):.3f}', '-i', REC, '-t', f'{dur:.3f}', '-vf',
           f'fps={FPS},crop=960:720:160:0', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-']
    raw = subprocess.run(cmd, capture_output=True, check=True).stdout
    n = len(raw) // (W * H * 3)
    fr = np.frombuffer(raw[:n * W * H * 3], np.uint8).reshape(n, H, W, 3)
    need = int(round(dur * FPS))
    if n < need:
        fr = np.concatenate([fr, np.repeat(fr[-1:], need - n, 0)]) if n else np.zeros((need, H, W, 3), np.uint8)
    return fr[:need]


yy, xx = np.mgrid[0:H, 0:W]
VIG = 1 - 0.55 * np.clip(np.hypot((xx - W / 2) / (W / 2), (yy - H / 2) / (H / 2)) - 0.45, 0, 1) ** 1.5


def vhs(frame, k, glitch=0.0):
    f = frame.astype(np.float32)
    # soft, low-res tape look
    img = Image.fromarray(frame).resize((W // 2, H // 2), Image.BILINEAR).resize((W, H), Image.BILINEAR)
    f = np.asarray(img).astype(np.float32)
    # warm fluorescent grade, lifted blacks, reduced saturation
    lum = f.mean(2, keepdims=True)
    f = lum + (f - lum) * 0.72
    f = f * np.array([1.06, 1.0, 0.86]) * 0.95 + np.array([14, 12, 10])
    # chroma bleed
    f[..., 0] = np.roll(f[..., 0], 3 + int(glitch * 8), 1)
    f[..., 2] = np.roll(f[..., 2], -3, 1)
    # head-switching noise at the bottom and line jitter
    rows = rng.random(H) < 0.004 + glitch * 0.05
    for y in np.nonzero(rows)[0]:
        f[y:y + 2] = np.roll(f[y:y + 2], int(rng.integers(-24, 24)), 1)
    f[H - 14:] = np.roll(f[H - 14:], int(rng.integers(8, 30)), 1) * 0.8 + rng.normal(0, 30, (14, W, 1))
    # rolling tracking band
    band = int((k * 9) % (H + 120)) - 60
    if 0 <= band < H - 12:
        f[band:band + 12] = f[band:band + 12] * 0.6 + rng.normal(150, 60, (12, W, 1)) * 0.4
    f += rng.normal(0, 9 + glitch * 40, (H, W, 1))
    f[::2] *= 0.9
    f *= VIG[..., None]
    return np.clip(f, 0, 255).astype(np.uint8)


def osd(frame, k, caption=None, cap_t=0.0):
    im = Image.fromarray(frame)
    d = ImageDraw.Draw(im)
    if (k // 15) % 2 == 0:
        d.text((40, 34), 'PLAY ▶', font=font(34, True), fill=(235, 235, 235))
    secs = 3 * 3600 + 17 * 60 + 4 + k // FPS
    d.text((W - 330, H - 84), f'AM {secs // 3600 % 12:02d}:{secs // 60 % 60:02d}:{secs % 60:02d}', font=font(32, True), fill=(235, 235, 235))
    d.text((W - 330, H - 46), 'KAS. 02 1998', font=font(28, True), fill=(235, 235, 235))
    d.text((40, H - 60), 'SP', font=font(30, True), fill=(235, 235, 235))
    if caption and cap_t > 0.4:
        a = min(1.0, (cap_t - 0.4) / 0.4)
        fnt = font(38, True)
        tw = d.textlength(caption, font=fnt)
        x = (W - tw) / 2
        c = int(235 * a)
        d.text((x + 3, H * 0.80 + 3), caption, font=fnt, fill=(0, 0, 0))
        d.text((x, H * 0.80), caption, font=fnt, fill=(c, c, c))
    return np.asarray(im)


def card(lines, n, k0, glitch=0.15):
    out = []
    for i in range(n):
        im = Image.new('RGB', (W, H), (8, 8, 10))
        d = ImageDraw.Draw(im)
        a = min(1, i / 12) * min(1, (n - i) / 10)
        y = H / 2 - sum(s for _, s, _ in lines) / 2 - 10 * len(lines)
        for text, size, bold in lines:
            fnt = font(size, bold)
            tw = d.textlength(text, font=fnt)
            c = int(225 * a)
            jx = int(rng.integers(-2, 3)) if rng.random() < 0.3 else 0
            d.text(((W - tw) / 2 + jx, y), text, font=fnt, fill=(c, int(c * 0.95), int(c * 0.88)))
            y += size + 20
        out.append(vhs(np.asarray(im), k0 + i, glitch))
    return out


def noise_frames(n, k0):
    return [vhs(np.clip(rng.normal(110, 70, (H, W, 1)).repeat(3, 2), 0, 255).astype(np.uint8), k0 + i, 0.8) for i in range(n)]


# ------------------------------------------------------------ assemble
frames = []
cues = []  # (time sec, sound, gain)


def add_sounds(base, lst):
    for t, s, g in lst:
        cues.append((base + t, s, g))


cues.append((0.0, 'static2', 0.9))
frames += noise_frames(int(1.2 * FPS), 0)
frames += card([('BULUNAN KASET #03', 44, True), ('"koridorda bir şey var"', 30, False)], int(3.2 * FPS), len(frames))
cues.append((1.4, 'distant_call', 0.7))


def run_segments(segs):
    global frames
    for name, off, dur, cap, snd in segs:
        if name not in scenes:
            print('missing scene', name)
            continue
        base = len(frames) / FPS
        clip = read_clip(scenes[name] + off, dur)
        add_sounds(base, snd)
        for i, fr in enumerate(clip):
            k = len(frames)
            g = 0.5 if i < 3 else 0.0
            frames.append(osd(vhs(fr, k, g), k, cap, i / FPS))
        cues.append((len(frames) / FPS - 0.1, 'static1', 0.35))
        frames += noise_frames(5, len(frames))


run_segments(EDIT)
cues.append((len(frames) / FPS, 'static2', 0.7))
frames += [np.zeros((H, W, 3), np.uint8)] * int(1.0 * FPS)
tb = len(frames) / FPS
frames += card([('ŞAPKALI ADAM', 92, True), ('THE HATMAN', 34, False), ('', 10, False),
                ('Analog Korku Modu  •  Forge 1.20.1 & 1.21.1', 28, False)], int(5.5 * FPS), len(frames), 0.1)
cues += [(tb, 'hit', 1.0), (tb + 0.2, 'drone', 0.6)]
frames += noise_frames(6, len(frames))
run_segments(EPILOGUE)
tb = len(frames) / FPS
frames += card([('O ZATEN ARKANDA.', 54, True)], int(3.0 * FPS), len(frames), 0.25)
cues += [(tb, 'whisper', 0.8), (tb + 2.6, 'jumpscare', 0.9)]
frames += noise_frames(int(0.8 * FPS), len(frames))
frames += [np.zeros((H, W, 3), np.uint8)] * int(1.0 * FPS)

# ------------------------------------------------------------ soundtrack
total = len(frames) / FPS
N = int(total * SR) + SR
mixbuf = np.zeros(N)


def load(name):
    wav = f'/tmp/claude-0/trailer_{name}.wav'
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', os.path.join(SND, name + '.ogg'), '-ar', str(SR), '-ac', '1', wav], check=True)
    with wave.open(wav) as w:
        return np.frombuffer(w.readframes(w.getnframes()), np.int16).astype(np.float32) / 32768


cache = {}
for t, s, g in cues:
    if s not in cache:
        cache[s] = load(s)
    x = cache[s] * g
    p = int(t * SR)
    mixbuf[p:p + len(x)] += x[:max(0, N - p)]
# tape hiss + low hum bed
hiss = np.random.default_rng(1).standard_normal(N) * 0.012
hum = 0.02 * np.sin(2 * np.pi * 60 * np.arange(N) / SR)
mixbuf += hiss + hum
mixbuf = np.tanh(mixbuf * 1.1) * 0.9
with wave.open('/tmp/claude-0/trailer_audio.wav', 'wb') as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((mixbuf * 32767).astype(np.int16).tobytes())

# ------------------------------------------------------------ encode
p = subprocess.Popen(['ffmpeg', '-y', '-loglevel', 'error', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-s', f'{W}x{H}', '-r', str(FPS), '-i', '-',
                      '-i', '/tmp/claude-0/trailer_audio.wav', '-c:v', 'libx264', '-preset', 'slow', '-crf', '21', '-pix_fmt', 'yuv420p',
                      '-c:a', 'aac', '-b:a', '160k', '-shortest', '-movflags', '+faststart', OUT], stdin=subprocess.PIPE)
for fr in frames:
    p.stdin.write(np.ascontiguousarray(fr).tobytes())
p.stdin.close(); p.wait()
print('wrote', OUT, f'{total:.1f}s')
