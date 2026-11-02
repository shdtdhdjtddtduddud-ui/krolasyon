"""CurseForge artwork: project logos (400x400), description banners (1200x400) and gallery images for the mod and the shaderpack."""
import os, sys, numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import hatman, paint_hatman, hatman_anim
from geo import render

J = sys.argv[1]          # folder with run-21 screenshots (vt-1.20.1) and shader screenshots (sh/stest)
F = sys.argv[2]          # folder with frames from the gameplay capture
OUT = os.path.join(os.path.dirname(__file__), '../curseforge')
SERIF = '/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf'
MONO = '/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf'
SANS = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
rng = np.random.default_rng(5)


def vhs(img, grain=10, shift=2):
    a = np.asarray(img.convert('RGB')).astype(np.float32)
    a[..., 0] = np.roll(a[..., 0], shift, 1); a[..., 2] = np.roll(a[..., 2], -shift, 1)
    a += rng.normal(0, grain, a.shape[:2] + (1,))
    a[::3] *= 0.94
    h, w = a.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    a *= (1 - 0.55 * np.clip(np.hypot((xx - w / 2) / (w / 2), (yy - h / 2) / (h / 2)) - 0.45, 0, 1) ** 1.4)[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def text(d, xy, s, font, fill=(236, 228, 206), anchor='mm', shadow=True):
    if shadow:
        d.text((xy[0] + 3, xy[1] + 3), s, font=font, fill=(0, 0, 0), anchor=anchor)
    d.text(xy, s, font=font, fill=fill, anchor=anchor)


def rec(d, x, y):
    d.ellipse((x - 9, y - 9, x + 9, y + 9), fill=(220, 30, 26))
    d.text((x + 16, y), 'REC', font=ImageFont.truetype(MONO, 22), fill=(236, 236, 236), anchor='lm')


def cover(path, size):
    im = Image.open(path).convert('RGB')
    w, h = size
    s = max(w / im.width, h / im.height)
    im = im.resize((int(im.width * s) + 1, int(im.height * s) + 1), Image.LANCZOS)
    return im.crop(((im.width - w) // 2, (im.height - h) // 2, (im.width - w) // 2 + w, (im.height - h) // 2 + h))


# ---------------------------------------------------------------------------------------- the Hatman render
G = hatman.G; G.pack(); TEX = G.paint(paint_hatman.PAINTERS)
pose = hatman_anim.A['frozen'].sample(0.0)
fig, _ = render(G, TEX, pose, yaw=12, pitch=-4, size=1400, bg=(255, 0, 255), light=(0.2, 0.5, -0.9))
a = np.asarray(fig).astype(np.int32)
mask = ~((np.abs(a[..., 0] - 255) < 8) & (a[..., 1] < 8) & (np.abs(a[..., 2] - 255) < 8))
fig = Image.fromarray(np.dstack([a, mask * 255]).astype(np.uint8), 'RGBA')
fig = fig.crop(fig.getbbox())


def paste_fig(bg, height, cx, bottom, brighten=1.0):
    f = fig.resize((int(fig.width * height / fig.height), height), Image.LANCZOS)
    arr = np.asarray(f).astype(np.float32)
    arr[..., :3] = np.clip(arr[..., :3] * brighten, 0, 255)
    f = Image.fromarray(arr.astype(np.uint8), 'RGBA')
    glow = Image.new('RGBA', bg.size, (0, 0, 0, 0))
    glow.paste((0, 0, 0, 200), (int(cx - f.width / 2), bottom - f.height), f)
    bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(14)))
    bg.alpha_composite(f, (int(cx - f.width / 2), bottom - f.height))


# ---------------------------------------------------------------------------------------- mod logo 400x400
bg = cover(f'{J}/sh/stest/htest_0610_c_hall_far.png', (400, 400)).filter(ImageFilter.GaussianBlur(1.2))
bg = Image.fromarray((np.asarray(bg).astype(np.float32) * 0.55).astype(np.uint8)).convert('RGBA')
paste_fig(bg, 300, 200, 372, 1.25)
d = ImageDraw.Draw(bg)
d.rectangle((0, 0, 400, 70), fill=(0, 0, 0, 170))
text(d, (200, 36), 'THE HATMAN', ImageFont.truetype(SERIF, 50))
d.rectangle((0, 362, 400, 400), fill=(0, 0, 0, 170))
text(d, (200, 381), 'ANALOG HORROR', ImageFont.truetype(MONO, 22), fill=(232, 198, 106))
logo = vhs(bg, grain=7, shift=2)
os.makedirs(f'{OUT}/mod/gallery', exist_ok=True); os.makedirs(f'{OUT}/shaderpack/gallery', exist_ok=True)
logo.save(f'{OUT}/mod/logo_400.png')

# ---------------------------------------------------------------------------------------- mod banner 1200x400
bg = cover(f'{F}/night_exterior.png', (1200, 400)).convert('RGBA')
grad = np.zeros((400, 1200, 4), np.uint8)
grad[..., 3] = (np.clip(1.0 - np.linspace(0, 1, 1200) * 1.4, 0, 1) * 220).astype(np.uint8)[None, :]
bg.alpha_composite(Image.fromarray(grad, 'RGBA'))
paste_fig(bg, 360, 1060, 392, 1.2)
d = ImageDraw.Draw(bg)
text(d, (60, 120), 'THE HATMAN', ImageFont.truetype(SERIF, 92), anchor='lm')
text(d, (64, 196), 'RENT DUE  ·  KİRA GÜNÜ', ImageFont.truetype(MONO, 30), fill=(232, 198, 106), anchor='lm')
text(d, (64, 262), 'Analog horror creature · haunted house · 5-chapter story', ImageFont.truetype(SANS, 24), anchor='lm')
text(d, (64, 298), 'Forge 1.20.1 & 1.21.1  ·  English / Türkçe', ImageFont.truetype(SANS, 24), fill=(200, 192, 176), anchor='lm')
rec(d, 1120, 36)
vhs(bg, grain=6).save(f'{OUT}/mod/banner_1200x400.png')

# ---------------------------------------------------------------------------------------- mod gallery
gal = [
    (f'{F}/night_exterior.png', '01_ardel_house_night'),
    (f'{F}/path.png', '02_porch_intro'),
    (f'{J}/vt-1.20.1/htest_0070_model_front.png', '03_hatman_model'),
    (f'{J}/vt-1.20.1/htest_1180_c_frozen.png', '04_frozen_like_the_photo'),
    (f'{J}/vt-1.20.1/htest_2745_house_hatman_close.png', '05_waiting_in_the_doorway'),
    (f'{J}/vt-1.20.1/htest_2635_house_hall_upper.png', '06_upstairs_hallway'),
    (f'{J}/vt-1.20.1/htest_2515_house_living.png', '07_living_room_tv'),
    (f'{J}/vt-1.20.1/htest_2880_story_intro_a.png', '08_cutscene_subtitles'),
    (f'{J}/vt-1.20.1/htest_4720_story_tape2_b.png', '09_vhs_tape_player'),
    (f'{J}/vt-1.20.1/htest_4770_story_note19.png', '10_notes'),
    (f'{J}/vt-1.20.1/htest_4170_story_chapter1.png', '11_chapter_card'),
    (f'{J}/vt-1.20.1/htest_5150_story_tr_chapter3.png', '12_turkish_language'),
    (f'{F}/studio.png', '13_studio_finale'),
    (f'{J}/vt-1.20.1/htest_1394_c_scream.png', '14_scream'),
]
for src, name in gal:
    if os.path.exists(src):
        Image.open(src).convert('RGB').save(f'{OUT}/mod/gallery/{name}.png', optimize=True)

# ---------------------------------------------------------------------------------------- shaderpack logo / banner / gallery
bg = cover(f'{J}/sh/stest/htest_2635_house_hall_upper.png', (400, 400)).convert('RGBA')
d = ImageDraw.Draw(bg)
d.rectangle((0, 300, 400, 400), fill=(0, 0, 0, 175))
text(d, (200, 334), 'HATMAN', ImageFont.truetype(SERIF, 46))
text(d, (200, 374), 'A N A L O G', ImageFont.truetype(MONO, 24), fill=(232, 198, 106))
rec(d, 26, 28)
vhs(bg, grain=5, shift=1).save(f'{OUT}/shaderpack/logo_400.png')
bg = cover(f'{J}/sh/stest/htest_2395_house_exterior.png', (1200, 400)).convert('RGBA')
grad = np.zeros((400, 1200, 4), np.uint8)
grad[..., 3] = (np.clip(1.0 - np.linspace(0, 1, 1200) * 1.6, 0, 1) * 210).astype(np.uint8)[None, :]
bg.alpha_composite(Image.fromarray(grad, 'RGBA'))
d = ImageDraw.Draw(bg)
text(d, (60, 130), 'HATMAN ANALOG', ImageFont.truetype(SERIF, 80), anchor='lm')
text(d, (64, 205), 'shaderpack for The Hatman', ImageFont.truetype(MONO, 28), fill=(232, 198, 106), anchor='lm')
text(d, (64, 265), 'soft shadows · candle flicker · fog & ground mist · bloom · VHS', ImageFont.truetype(SANS, 24), anchor='lm')
text(d, (64, 300), 'Oculus / Iris / OptiFine  ·  English / Türkçe', ImageFont.truetype(SANS, 24), fill=(200, 192, 176), anchor='lm')
rec(d, 1120, 36)
vhs(bg, grain=5, shift=1).save(f'{OUT}/shaderpack/banner_1200x400.png')
sgal = [('htest_2395_house_exterior', '01_house_daylight_shadows'), ('htest_2455_house_hall_ground', '02_lamplit_hallway'),
        ('htest_2635_house_hall_upper', '03_candle_sconces'), ('htest_2515_house_living', '04_tv_glow'),
        ('htest_0610_c_hall_far', '05_fluorescent_corridor'), ('htest_2880_story_intro_a', '06_story_cutscene_night'),
        ('htest_0070_model_front', '07_hatman_shadow_form'), ('htest_2745_house_hatman_close', '08_doorway')]
for src, name in sgal:
    p = f'{J}/sh/stest/{src}.png'
    if os.path.exists(p):
        Image.open(p).convert('RGB').save(f'{OUT}/shaderpack/gallery/{name}.png', optimize=True)
print('done')
