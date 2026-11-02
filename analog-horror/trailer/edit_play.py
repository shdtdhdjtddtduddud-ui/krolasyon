"""Cuts the CI playthrough capture (video + game audio) into a gameplay video using the [AUTOTEST] SCENE markers.
usage: python3 edit_play.py play.mkv latest.log rec_start.txt out.mp4"""
import sys, os, re, subprocess
from PIL import Image, ImageDraw, ImageFont

REC, LOG, START, OUT = sys.argv[1:5]
TMP = '/tmp/claude-0/edit_play'
os.makedirs(TMP, exist_ok=True)
FB = '/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf'
F = '/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf'
t0 = int(open(START).read().strip()) / 1000.0
sc = {}
for line in open(LOG, errors='ignore'):
    m = re.search(r'\[AUTOTEST\] SCENE (\w+) (\d+)', line)
    if m:
        sc[m.group(1)] = int(m.group(2)) / 1000.0 - t0
print({k: round(v, 1) for k, v in sc.items()})

# (scene, offset, duration, caption)
EDIT = [
    ('p_exterior', 2.0, 26.0, 'Ardel Evi — Dişbudak Sokağı No:13'),
    ('p_story', 0.5, 66.0, 'Giriş'),
    ('p_ch1', 0.0, 48.0, None),
    ('p_tape1', 0.5, 22.0, None),
    ('p_apparition', 0.0, 9.0, None),
    ('p_ch2', 0.0, 38.0, None),
    ('p_fusebox', 0.0, 16.0, None),
    ('p_tape2', 2.0, 26.0, None),
    ('p_lookaway', 1.0, 28.0, None),
    ('p_ch3', 0.0, 40.0, None),
    ('p_tape3', 1.0, 18.0, None),
    ('p_key', 0.0, 12.0, None),
    ('p_ch4', 0.0, 32.0, None),
    ('p_tape4', 0.5, 22.0, None),
    ('p_chase', 0.0, 13.0, None),
    ('p_ch5', 0.0, 70.0, None),
    ('p_ending', 0.0, 55.0, None),
]


def card(path, lines):
    im = Image.new('RGB', (1280, 720), (6, 6, 8))
    d = ImageDraw.Draw(im)
    y = 360 - sum(s for _, s, _ in lines) // 2 - 12 * len(lines)
    for text, size, bold in lines:
        f = ImageFont.truetype(FB if bold else F, size)
        w = d.textlength(text, font=f)
        d.text(((1280 - w) / 2, y), text, font=f, fill=(236, 228, 206))
        y += size + 24
    im.save(path)


def enc_card(img, dur, out):
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-loop', '1', '-t', str(dur), '-i', img, '-f', 'lavfi', '-t', str(dur), '-i',
                    'anullsrc=r=48000:cl=stereo', '-vf', f'fps=30,format=yuv420p,fade=in:0:15,fade=out:st={dur - 0.5}:d=0.5', '-c:v', 'libx264',
                    '-preset', 'medium', '-crf', '22', '-c:a', 'aac', '-b:a', '160k', '-ar', '48000', '-shortest', out], check=True)


parts = []
card(f'{TMP}/title.png', [('ŞAPKALI ADAM: KİRA GÜNÜ', 54, True), ('THE HATMAN: RENT DUE', 26, False), ('', 6, False),
                          ('Oynanış videosu — hikâye modu', 24, False), ('Forge 1.20.1 & 1.21.1', 20, False)])
enc_card(f'{TMP}/title.png', 5, f'{TMP}/p00.mp4')
parts.append(f'{TMP}/p00.mp4')
for i, (name, off, dur, cap) in enumerate(EDIT, 1):
    if name not in sc:
        print('missing', name)
        continue
    start = max(0.0, sc[name] + off)
    vf = 'fps=30,format=yuv420p'
    if cap:
        vf += (f",drawtext=fontfile={FB}:text='{cap}':x=40:y=h-80:fontsize=28:fontcolor=white@0.9:borderw=2:bordercolor=black@0.8:"
               f"enable='between(t,0.5,5)'")
    out = f'{TMP}/p{i:02d}.mp4'
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-ss', f'{start:.2f}', '-i', REC, '-t', str(dur), '-vf', vf + f',fade=in:0:8,fade=out:st={dur - 0.3}:d=0.3',
                    '-af', f'afade=in:st=0:d=0.3,afade=out:st={dur - 0.4}:d=0.4,aresample=48000', '-ac', '2',
                    '-c:v', 'libx264', '-preset', 'medium', '-crf', '22', '-c:a', 'aac', '-b:a', '160k', out], check=True)
    parts.append(out)
card(f'{TMP}/end.png', [('/hatman hikaye', 40, True), ('ile başlat', 24, False), ('', 6, False), ('Lütfen gözünü ayırma.', 26, False)])
enc_card(f'{TMP}/end.png', 5, f'{TMP}/p99.mp4')
parts.append(f'{TMP}/p99.mp4')
with open(f'{TMP}/list.txt', 'w') as fl:
    for p in parts:
        fl.write(f"file '{p}'\n")
subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', f'{TMP}/list.txt', '-c', 'copy', '-movflags', '+faststart', OUT], check=True)
print('wrote', OUT)
