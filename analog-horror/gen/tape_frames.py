"""320x240 VHS frames for the in-game tapes (no text inside - all words come from the lang files)."""
import os, math, numpy as np
from PIL import Image, ImageDraw, ImageFilter
import hatman, paint_hatman, hatman_anim
from geo import render

OUT = os.path.join(os.path.dirname(__file__), '../common/src/main/resources/assets/krolasyonhorror/textures/tape')
os.makedirs(OUT, exist_ok=True)
G = hatman.G; G.pack(); TEX = G.paint(paint_hatman.PAINTERS)
W, H = 320, 240
rng = np.random.default_rng(8)
KEY = (255, 0, 255)


def brim(anim, t, yaw=0, pitch=0, size=600, light=(0.3, 0.6, -0.8)):
    pose = hatman_anim.A[anim].sample(t) if anim else {}
    im, _ = render(G, TEX, pose, yaw=yaw, pitch=pitch, size=size, bg=KEY, light=light)
    a = np.asarray(im).astype(np.int32)
    mask = ~((np.abs(a[..., 0] - 255) < 8) & (a[..., 1] < 8) & (np.abs(a[..., 2] - 255) < 8))
    rgba = np.dstack([a, mask * 255]).astype(np.uint8)
    return Image.fromarray(rgba, 'RGBA')


def paste_fig(bg, fig, cx, bottom, height, tint=None, alpha=1.0):
    bbox = fig.getbbox()
    fig = fig.crop(bbox)
    s = height / fig.height
    fig = fig.resize((max(1, int(fig.width * s)), int(height)), Image.BILINEAR)
    if tint is not None:
        arr = np.asarray(fig).astype(np.float32)
        arr[..., :3] = arr[..., :3] * 0.1 + np.array(tint) * 0.9
        arr[..., 3] *= alpha
        fig = Image.fromarray(arr.astype(np.uint8), 'RGBA')
    bg.alpha_composite(fig, (int(cx - fig.width / 2), int(bottom - fig.height)))


def vhs(img, dark=1.0, seed=0):
    r = np.random.default_rng(seed)
    a = np.asarray(img.convert('RGB')).astype(np.float32)
    a = np.asarray(Image.fromarray(a.astype(np.uint8)).resize((W // 2, H // 2), Image.BILINEAR).resize((W, H), Image.BILINEAR)).astype(np.float32)
    lum = a.mean(2, keepdims=True)
    a = lum + (a - lum) * 0.75
    a = a * np.array([1.05, 1.0, 0.9]) * dark + 10
    a[..., 0] = np.roll(a[..., 0], 2, 1); a[..., 2] = np.roll(a[..., 2], -2, 1)
    a += r.normal(0, 7, (H, W, 1))
    a[::2] *= 0.92
    yy, xx = np.mgrid[0:H, 0:W]
    vig = 1 - 0.5 * np.clip(np.hypot((xx - W / 2) / (W / 2), (yy - H / 2) / (H / 2)) - 0.5, 0, 1)
    a *= vig[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def studio(lights=1.0):
    im = Image.new('RGBA', (W, H), (40, 12, 14, 255))
    d = ImageDraw.Draw(im)
    for x in range(0, W, 12):  # curtain folds
        c = 70 + int(30 * math.sin(x / 12 * 1.7))
        d.rectangle((x, 0, x + 12, 170), fill=(int(c * lights), int(c * 0.22 * lights), int(c * 0.25 * lights), 255))
    d.rectangle((60, 40, 260, 160), fill=(int(150 * lights), int(170 * lights), int(190 * lights), 255))  # painted sky backdrop
    d.polygon([(110, 160), (110, 100), (160, 70), (210, 100), (210, 160)], fill=(int(200 * lights), int(180 * lights), int(140 * lights), 255))
    d.rectangle((150, 120, 170, 160), fill=(int(90 * lights), int(60 * lights), int(40 * lights), 255))
    d.rectangle((122, 108, 140, 124), fill=(int(40 * lights), int(40 * lights), int(60 * lights), 255))
    d.ellipse((222, 50, 252, 80), fill=(int(230 * lights), int(220 * lights), int(190 * lights), 255))  # clock without hands
    d.rectangle((0, 170, W, H), fill=(int(110 * lights), int(70 * lights), int(40 * lights), 255))  # stage floor
    for x in range(0, W, 20):
        d.line((x, 170, x - 30, H), fill=(int(80 * lights), int(50 * lights), int(30 * lights), 255))
    d.rectangle((0, 0, W, 10), fill=(20, 6, 6, 255))
    return im


def save(name, img, **kw):
    vhs(img, **kw).save(os.path.join(OUT, name + '.png'))
    print(name)


# ----------------------------------------------------------------- studio (Mr. Brim's Neighborly Hour)
save('studio_title', studio(), seed=1)
for name, anim, t, yaw, h, cx in (('studio_idle', 'idle', 1.0, 10, 190, 165), ('studio_tip', 'hat_tip', 1.0, 6, 185, 160),
                                  ('studio_look', 'look', 1.3, 0, 190, 170)):
    bg = studio()
    paste_fig(bg, brim(anim, t, yaw=yaw), cx, 220, h)
    save(name, bg, seed=hash(name) % 99)
bg = studio(0.8)
f = brim('idle', 2.3, yaw=6, pitch=-6, size=900)
f = f.crop(f.getbbox()); f = f.crop((0, 0, f.width, int(f.height * 0.42)))
f = f.resize((int(f.width * 300 / f.height), 300), Image.BILINEAR)
bg.alpha_composite(f, (int(165 - f.width / 2), -20))
save('studio_close', bg, seed=5)
bg = studio(0.7)
d = ImageDraw.Draw(bg)
d.rectangle((150, 150, 186, 158), fill=(50, 30, 20, 255)); d.rectangle((154, 158, 158, 200), fill=(50, 30, 20, 255)); d.rectangle((178, 158, 182, 200), fill=(50, 30, 20, 255))
d.ellipse((140, 136, 196, 150), fill=(190, 165, 120, 255)); d.rectangle((156, 124, 180, 142), fill=(190, 165, 120, 255))
save('studio_empty', bg, seed=6)

# ----------------------------------------------------------------- home video
def room(dark=0.35):
    im = Image.new('RGBA', (W, H), (int(40 * dark), int(36 * dark), int(30 * dark), 255))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 160, W, H), fill=(int(70 * dark), int(30 * dark), int(28 * dark), 255))
    return im


bg = room()
d = ImageDraw.Draw(bg)
d.rectangle((120, 70, 200, 140), fill=(30, 30, 30, 255))
tvs = Image.fromarray((rng.random((60, 70)) * 200 + 40).astype(np.uint8)).convert('RGBA')
bg.alpha_composite(tvs, (125, 75))
paste_fig(bg, brim('idle', 2.0, size=300), 160, 133, 50, tint=(30, 30, 30))
d.polygon([(110, 140), (210, 140), (240, 240), (80, 240)], fill=(200, 210, 220, 40))
save('home_tv', bg, seed=11)
bg = room(0.3)
d = ImageDraw.Draw(bg)
d.rectangle((130, 60, 210, 120), fill=(170, 180, 190, 255))
for cx, h in ((140, 60), (195, 46)):
    d.ellipse((cx - 14, 230 - h - 20, cx + 14, 230 - h + 6), fill=(12, 10, 10, 255))
    d.rectangle((cx - 18, 230 - h, cx + 18, 240), fill=(12, 10, 10, 255))
save('home_kids', bg, seed=12)
bg = room(0.25)
d = ImageDraw.Draw(bg)
d.ellipse((110, 40, 210, 150), fill=(14, 12, 12, 255)); d.rectangle((70, 140, 250, 240), fill=(14, 12, 12, 255))
d.arc((110, 40, 210, 150), 200, 330, fill=(150, 170, 200, 255), width=3)
save('home_mira', bg, seed=13)
bg = room(0.3)
d = ImageDraw.Draw(bg)
d.rectangle((90, 30, 230, 180), fill=(20, 28, 48, 255)); d.rectangle((86, 26, 234, 30), fill=(70, 60, 50, 255))
d.line((160, 30, 160, 180), fill=(70, 60, 50, 255), width=4); d.line((90, 105, 230, 105), fill=(70, 60, 50, 255), width=4)
paste_fig(bg, brim('frozen', 0.0, size=500), 190, 210, 190, tint=(5, 6, 10))
d = ImageDraw.Draw(bg)
d.line((160, 30, 160, 180), fill=(70, 60, 50, 255), width=4); d.line((90, 105, 230, 105), fill=(70, 60, 50, 255), width=4)
save('home_window', bg, seed=14)

# ----------------------------------------------------------------- party
bg = room(0.15)
d = ImageDraw.Draw(bg)
d.ellipse((100, 150, 220, 200), fill=(200, 190, 170, 255)); d.rectangle((100, 130, 220, 175), fill=(220, 200, 180, 255))
d.ellipse((100, 115, 220, 145), fill=(240, 230, 220, 255))
for i, (x, c) in enumerate(((125, (20, 20, 20)), (150, (180, 30, 30)), (175, (40, 60, 180)), (200, (240, 240, 240)))):
    d.rectangle((x - 2, 108, x + 2, 128), fill=c + (255,))
    d.ellipse((x - 4, 98, x + 4, 110), fill=(255, 200, 100, 255))
glow = Image.new('RGBA', (W, H), (0, 0, 0, 0)); gd = ImageDraw.Draw(glow); gd.ellipse((60, 40, 260, 220), fill=(255, 160, 60, 70))
bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(30)))
save('party_cake', bg, seed=21)
bg = room(0.2)
d = ImageDraw.Draw(bg)
for x, y, c in ((80, 60, (230, 230, 225)), (130, 40, (200, 180, 100)), (200, 70, (230, 230, 225)), (250, 50, (200, 180, 100))):
    d.ellipse((x - 26, y - 32, x + 26, y + 32), fill=c + (255,)); d.line((x, y + 32, x + 5, 240), fill=(40, 40, 40, 255))
save('party_balloons', bg, seed=22)
bg = room(0.1)
d = ImageDraw.Draw(bg)
d.rectangle((120, 20, 200, 200), fill=(160, 110, 60, 255))
paste_fig(bg, brim('idle', 0.5, size=500), 160, 205, 185, tint=(4, 3, 3))
save('party_door', bg, seed=23)
save('black', Image.new('RGBA', (W, H), (0, 0, 0, 255)), dark=0.2, seed=24)

# ----------------------------------------------------------------- attic (Victor's confession)
def tvwall(face=False):
    im = Image.new('RGBA', (W, H), (10, 10, 12, 255))
    d = ImageDraw.Draw(im)
    for ix in range(5):
        for iy in range(3):
            x, y = 20 + ix * 58, 20 + iy * 60
            d.rectangle((x, y, x + 52, y + 50), fill=(30, 30, 30, 255))
            n = Image.fromarray((rng.random((40, 42)) * 170 + 50).astype(np.uint8)).convert('RGBA')
            im.alpha_composite(n, (x + 5, y + 5))
            if face and (ix + iy) % 3 == 0:
                paste_fig(im, brim('frozen', 0.0, size=300), x + 26, y + 46, 38, tint=(10, 10, 10))
    return im


bg = tvwall()
d = ImageDraw.Draw(bg)
d.ellipse((120, 110, 200, 190), fill=(8, 8, 8, 255)); d.rectangle((90, 180, 230, 240), fill=(8, 8, 8, 255))
save('attic_victor', bg, seed=31)
save('attic_tvs', tvwall(True), seed=32)
bg = Image.new('RGBA', (W, H), (20, 16, 14, 255))
d = ImageDraw.Draw(bg)
d.rectangle((90, 110, 230, 150), fill=(18, 18, 18, 255)); d.rectangle((110, 116, 210, 130), fill=(200, 190, 160, 255))
d.ellipse((60, 120, 120, 200), fill=(150, 110, 90, 255)); d.ellipse((200, 120, 260, 200), fill=(150, 110, 90, 255))
save('attic_hands', bg, seed=33)
bg = tvwall()
d = ImageDraw.Draw(bg)
d.ellipse((120, 110, 200, 190), fill=(8, 8, 8, 255)); d.rectangle((90, 180, 230, 240), fill=(8, 8, 8, 255))
paste_fig(bg, brim('frozen', 0.0, size=600), 230, 245, 230, tint=(30, 26, 22))
save('attic_brim', bg, seed=34)
st = Image.fromarray((rng.random((H, W)) * 255).astype(np.uint8)).convert('RGBA')
save('static', st, seed=35)
