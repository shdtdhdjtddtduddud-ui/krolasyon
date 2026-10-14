"""Flying dragon silhouette (top view, head up) for L-Drago's Dragon Emperor dive: textures/fx/dragon_flight.png.
rgb = fiery shading, alpha = silhouette (with a soft glow rim)."""
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'src/main/resources/assets/librabeyblade/textures/fx/dragon_flight.png')
N = 512
S = 4  # supersampling
W = N * S
img = Image.new('L', (W, W), 0)
d = ImageDraw.Draw(img)


def P(x, y):
    return (W / 2 + x * W / 2, W / 2 + y * W / 2)


# spine: head (top) -> tail (bottom), an S curve
def spine(t):
    y = -0.78 + t * 1.68
    x = 0.10 * math.sin(t * 5.2 + 0.4) * (0.3 + t)
    return x, y


def width(t):
    if t < 0.06: return 0.03 + t * 1.2            # snout
    if t < 0.14: return 0.10 - (t - 0.06) * 0.4    # head -> neck
    if t < 0.24: return 0.065
    if t < 0.45: return 0.065 + (t - 0.24) * 0.25  # chest
    if t < 0.6: return 0.118 - (t - 0.45) * 0.3
    return max(0.006, 0.073 * (1 - (t - 0.6) / 0.4) ** 1.3)


left, right = [], []
M = 160
for i in range(M + 1):
    t = i / M
    x, y = spine(t)
    x2, y2 = spine(min(1, t + 1e-3))
    tx, ty = x2 - x, y2 - y
    L = math.hypot(tx, ty) + 1e-9
    nx, ny = -ty / L, tx / L
    w = width(t)
    left.append(P(x + nx * w, y + ny * w))
    right.append(P(x - nx * w, y - ny * w))
d.polygon(left + right[::-1], fill=255)
# horns and jaw spikes
hx, hy = spine(0.07)
for s in (-1, 1):
    d.polygon([P(hx + s * 0.06, hy - 0.02), P(hx + s * 0.20, hy - 0.16), P(hx + s * 0.09, hy + 0.03)], fill=255)
    d.polygon([P(hx + s * 0.08, hy + 0.04), P(hx + s * 0.17, hy + 0.02), P(hx + s * 0.09, hy + 0.08)], fill=255)
# tail spade
tx, ty = spine(1.0)
d.polygon([P(tx, ty - 0.04), P(tx - 0.09, ty + 0.06), P(tx, ty + 0.14), P(tx + 0.09, ty + 0.06)], fill=255)
# wings: bat-like - an arm out to the wrist, finger bones fanning from it, deeply scalloped membrane
sx, sy = spine(0.32)
for s in (-1, 1):
    root = (sx + s * 0.05, sy - 0.02)
    elbow = (s * 0.30, sy - 0.30)
    wrist = (s * 0.52, sy - 0.40)
    tips = [(s * 0.99, sy - 0.30), (s * 0.95, sy - 0.02), (s * 0.80, sy + 0.24), (s * 0.55, sy + 0.36), (sx + s * 0.12, sy + 0.30)]
    pts = [P(*root), P(*elbow), P(*wrist), P(*tips[0])]
    for i in range(len(tips) - 1):
        a_, b_ = tips[i], tips[i + 1]
        for k in range(1, 8):
            f = k / 8
            x = a_[0] + (b_[0] - a_[0]) * f
            y = a_[1] + (b_[1] - a_[1]) * f
            bow = math.sin(f * math.pi) * 0.38
            pts.append(P(x + (wrist[0] - x) * bow, y + (wrist[1] - y) * bow))
        pts.append(P(*b_))
    pts.append(P(sx + s * 0.05, sy + 0.2))
    d.polygon(pts, fill=255)
    lw = int(0.02 * W / 2)
    d.line([P(*root), P(*elbow), P(*wrist)], fill=255, width=int(lw * 1.6))
    for tp in tips[:-1]:
        d.line([P(*wrist), P(*tp)], fill=255, width=lw)
    # claw on the wrist
    d.polygon([P(wrist[0], wrist[1] - 0.02), P(wrist[0] - s * 0.02, wrist[1] - 0.12), P(wrist[0] + s * 0.04, wrist[1] - 0.01)], fill=255)

mask = np.asarray(img.resize((N, N), Image.LANCZOS)).astype(np.float32) / 255
# shading: hot core along the spine, darker crimson toward wing edges, bright rim glow
yy, xx = np.mgrid[0:N, 0:N]
u = (xx + 0.5) / N * 2 - 1
v = (yy + 0.5) / N * 2 - 1
dist_in = np.asarray(Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(10))).astype(np.float32) / 255
core = np.clip(1 - np.abs(u) * 2.2, 0, 1) * np.clip(dist_in * 1.4, 0, 1)
r = 0.55 + 0.45 * core
g = 0.05 + 0.55 * core ** 2
b = 0.08 + 0.2 * core ** 3
edge = np.clip(mask - dist_in * 1.2, 0, 1)
r = np.clip(r + edge * 0.5, 0, 1); g = np.clip(g + edge * 0.35, 0, 1); b = np.clip(b + edge * 0.1, 0, 1)
glow = np.asarray(Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(7))).astype(np.float32) / 255
alpha = np.clip(mask + glow * 0.45, 0, 1)
out = np.stack([r, g, b, alpha], -1)
Image.fromarray((out * 255).astype(np.uint8), 'RGBA').save(OUT)
print('ok')
