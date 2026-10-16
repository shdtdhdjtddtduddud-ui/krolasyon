"""Builds Evil Befall UW145EWD's 3D mesh (same method as libra_model.py) + textures straight from the reference artwork (beffal_source_fixed.png).

The top of the beyblade is a polar height-field mesh whose outline follows the artwork's silhouette exactly and whose
texture *is* the artwork, so seen from above it is pixel-for-pixel the reference. Heights come from colour classes
(metal wheel / clear wheel / inner recess) plus a luminance emboss. The hexagonal face bolt is a separate bevelled
prism, and the T125 track + ES (Eternal Sharp) tip are lathe geometry underneath.

Output:
  textures/entity/beffal_face.png   artwork (1024^2, alpha = silhouette)
  textures/entity/beffal_glow.png   emissive mask used while abilities are active
  textures/entity/beffal_parts.png  swatches for track / tip / underside
  textures/item/beffal_icon.png     inventory/HUD icon
  models/beffal_mesh.bin            quads: x y z u v nx ny nz (big endian floats)
Model units: disc radius = 1, y up, y = 0 at the tip point, +x = image right, +z = image down.
"""
import math, os, struct, sys
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage as nd

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/librabeyblade')

SRC = np.asarray(Image.open(os.path.join(HERE, 'beffal_source_fixed.png')).convert('RGB')).astype(np.float32) / 255.0
H, W, _ = SRC.shape
CX, CY = 377.0, 385.0     # rotation axis = face bolt centre
RPX = 348.0               # disc radius in pixels
S = 364.0                 # half size of the square texture window
TEX = 1024

# hex face bolt (artwork px)
HEX_CX, HEX_CY, HEX_R, HEX_HALF_H = 377.0, 387.0, 118.0, 102.2
HEX_ROT = math.radians(0)

# heights (model units)
WHEEL_BOTTOM = 0.86       # underside of the metal wheel meets the track here
TOP_BASE = 0.94           # top surface heights are added to this
HEX_TOP = TOP_BASE + 0.54


def segment():
    white = (SRC.min(2) > 232 / 255) & ((SRC.max(2) - SRC.min(2)) < 20 / 255)
    lab, _ = nd.label(white)
    bgl = {lab[0, 0], lab[0, -1], lab[-1, 0], lab[-1, -1]} - {0}
    fg = ~np.isin(lab, list(bgl))
    fg = nd.binary_opening(fg, iterations=2)
    lab2, n2 = nd.label(fg)
    sizes = nd.sum(fg, lab2, range(1, n2 + 1))
    fg = lab2 == (np.argmax(sizes) + 1)
    return nd.binary_fill_holes(fg)


MASK = segment()
YY, XX = np.mgrid[0:H, 0:W]
RAD = np.hypot(XX - CX, YY - CY) / RPX


def classes():
    r, g, b = SRC[..., 0], SRC[..., 1], SRC[..., 2]
    mx, mn = SRC.max(2), SRC.min(2)
    sat = (mx - mn) / (mx + 1e-6)
    dark = MASK & (mx < 0.26)                                          # black flame lacquer
    pink = MASK & ~dark & (sat > 0.28) & (r > g + 0.12) & (r >= b - 0.05)   # pink clear wheel
    gems = MASK & ~dark & ~pink & (sat > 0.4) & (RAD > 0.38) & (RAD < 0.62)  # rainbow gem band
    holes = np.zeros_like(MASK)
    white = (mn > 0.85) & MASK
    grey = MASK & ~pink & ~dark & ~gems
    dx, dy = XX - HEX_CX, YY - HEX_CY
    inhex = np.hypot(dx, dy) < HEX_R * 0.8
    emblem = inhex & (b > g + 0.2) & (r < 0.7) & (mx > 0.25)          # purple peacock emblem
    return pink, dark, holes, white, gems, grey, emblem


GREEN, YELLOW, NAVY, WHITE, GEMS, GREY, PINK = classes()   # GREEN = pink clear wheel, YELLOW = black flames, PINK = emblem
LUM = SRC @ np.array([0.3, 0.55, 0.15], np.float32)


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def heightmap():
    h = np.full((H, W), 0.31, np.float32)                 # chrome metal wheel
    h[GREEN] = 0.35                                        # pink clear wheel
    h[YELLOW & (RAD > 0.55)] = 0.30                        # black flames on the clear wheel
    h[GEMS] = 0.27                                         # rainbow gem band sits lower
    h = nd.gaussian_filter(h, 2.2)
    h += nd.gaussian_filter(LUM - 0.55, 1.4) * 0.07
    h -= 0.12 * smoothstep(0.86, 1.02, RAD)
    return np.clip(h, 0.06, 0.4)


HM = heightmap()


def sample(img, px, py):
    """bilinear sample of a 2D array at artwork pixel coords"""
    px = np.clip(px, 0, W - 1.001)
    py = np.clip(py, 0, H - 1.001)
    x0, y0 = int(px), int(py)
    fx, fy = px - x0, py - y0
    a, b, c, d = img[y0, x0], img[y0, x0 + 1], img[y0 + 1, x0], img[y0 + 1, x0 + 1]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def uv(px, py):
    return (px - (CX - S)) / (2 * S), (py - (CY - S)) / (2 * S)


def edge_radius(theta):
    """outermost silhouette pixel along a ray from the centre (artwork px)"""
    dx, dy = math.cos(theta), math.sin(theta)
    last = 0.0
    r = 238.0
    while r < 400:
        x, y = int(round(CX + dx * r)), int(round(CY + dy * r))
        if 0 <= x < W and 0 <= y < H and MASK[y, x]:
            last = r
        r += 0.5
    return last - 1.0


class Mesh:
    def __init__(self):
        self.groups = {}

    def quad(self, group, verts):
        """verts: 4 x (x, y, z, u, v, nx, ny, nz)"""
        self.groups.setdefault(group, []).append(verts)

    def write(self, path):
        with open(path, 'wb') as f:
            f.write(struct.pack('>ii', 0x4C425241, len(self.groups)))
            for gid in sorted(self.groups):
                qs = self.groups[gid]
                f.write(struct.pack('>ii', gid, len(qs)))
                for q in qs:
                    for v in q:
                        f.write(struct.pack('>8f', *[float(c) for c in v]))


GROUP_TOP, GROUP_SIDE, GROUP_PARTS = 0, 1, 2

# parts swatches inside eagle_parts.png (64x64): name -> (u0, v0) of a 16x16 cell
SW = {'track': (0, 0), 'tip': (16, 0), 'under': (32, 0), 'point': (48, 0), 'track_light': (0, 16), 'bearing': (16, 16)}


def sw_uv(name, fu, fv):
    u0, v0 = SW[name]
    return (u0 + 0.5 + fu * 15) / 64.0, (v0 + 0.5 + fv * 15) / 64.0


def build():
    m = Mesh()
    NS = 168
    rings = np.concatenate([np.linspace(0.27, 0.80, 22), np.linspace(0.80, 1.0, 13)[1:]])
    thetas = [2 * math.pi * i / NS for i in range(NS)]
    edge = [edge_radius(t) / RPX for t in thetas]
    edge = [float(np.median([edge[(i + k) % NS] for k in (-1, 0, 1)])) for i in range(NS)]

    # ---- top height field ----
    grid = []
    for j, rr in enumerate(rings):
        row = []
        for i, t in enumerate(thetas):
            rad = rr
            if rr > 0.8:
                # stretch the outer band so its last ring sits exactly on the silhouette
                k = (rr - 0.8) / 0.2
                rad = 0.8 + (edge[i] - 0.8) * k
            px, py = CX + math.cos(t) * rad * RPX, CY + math.sin(t) * rad * RPX
            hgt = float(sample(HM, px, py))
            row.append([rad * math.cos(t), TOP_BASE + hgt, rad * math.sin(t), px, py])
        grid.append(row)
    grid = np.array(grid)  # [ring, sector, 5]

    def normal_at(j, i):
        jm, jp = max(j - 1, 0), min(j + 1, len(rings) - 1)
        a = grid[jp, i, :3] - grid[jm, i, :3]
        b = grid[j, (i + 1) % NS, :3] - grid[j, (i - 1) % NS, :3]
        n = np.cross(b, a)
        if n[1] < 0:
            n = -n
        return n / (np.linalg.norm(n) + 1e-9)

    normals = np.array([[normal_at(j, i) for i in range(NS)] for j in range(len(rings))])

    def tv(j, i):
        p = grid[j, i]
        u, v = uv(p[3], p[4])
        n = normals[j, i]
        return (p[0], p[1], p[2], u, v, n[0], n[1], n[2])

    for j in range(len(rings) - 1):
        for i in range(NS):
            i2 = (i + 1) % NS
            m.quad(GROUP_TOP, [tv(j, i), tv(j + 1, i), tv(j + 1, i2), tv(j, i2)])

    # ---- outer wall: rim edge down to the underside lip ----
    jl = len(rings) - 1
    lip_y = TOP_BASE + 0.02
    for i in range(NS):
        i2 = (i + 1) % NS
        pts = []
        for ii in (i, i2):
            p = grid[jl, ii]
            t = thetas[ii]
            # sample colour slightly inside the silhouette so we get metal, not background
            px, py = CX + math.cos(t) * (edge[ii] * RPX - 6), CY + math.sin(t) * (edge[ii] * RPX - 6)
            u, v = uv(px, py)
            n = (math.cos(t), 0.0, math.sin(t))
            pts.append(((p[0], p[1], p[2], u, v) + n, (p[0] * 0.995, lip_y, p[2] * 0.995, u, v) + n))
        (a_top, a_bot), (b_top, b_bot) = pts
        m.quad(GROUP_SIDE, [a_top, a_bot, b_bot, b_top])

    # ---- underside of the metal wheel: lip -> track ----
    under_r = [1.0, 0.93, 0.75, 0.55, 0.36, 0.30]
    under_y = [lip_y, TOP_BASE - 0.02, TOP_BASE - 0.05, TOP_BASE - 0.065, WHEEL_BOTTOM + 0.01, WHEEL_BOTTOM]
    for k in range(len(under_r) - 1):
        for i in range(NS):
            i2 = (i + 1) % NS
            q = []
            for (rk, yk, ii) in ((k, k, i), (k + 1, k + 1, i), (k + 1, k + 1, i2), (k, k, i2)):
                t = thetas[ii]
                rad = under_r[rk] * (edge[ii] * 0.995 if rk == 0 else 1.0)
                if rk == 1:
                    rad = under_r[rk] * edge[ii]
                dr = under_r[k] - under_r[k + 1]
                dy = under_y[k] - under_y[k + 1]
                nn = np.array([dy * math.cos(t), -dr, dy * math.sin(t)])
                nn /= np.linalg.norm(nn) + 1e-9
                u, v = sw_uv('under', ii / NS, rk / (len(under_r) - 1))
                q.append((rad * math.cos(t), under_y[yk], rad * math.sin(t), u, v, nn[0], nn[1], nn[2]))
            m.quad(GROUP_PARTS, q)

    # ---- hexagonal face bolt ----
    hx = []
    for k in range(6):
        a = math.radians(60 * k)
        hx.append((HEX_CX + HEX_R * math.cos(a + HEX_ROT), HEX_CY + HEX_R * math.sin(a + HEX_ROT)))
    inset = 0.9
    hx_in = [(HEX_CX + (x - HEX_CX) * inset, HEX_CY + (y - HEX_CY) * inset) for x, y in hx]

    def mp(px, py, y):
        return ((px - CX) / RPX, y, (py - CY) / RPX)

    top_y, bevel_y, base_y = HEX_TOP, HEX_TOP - 0.035, TOP_BASE + 0.30
    # top face as a triangle fan made of degenerate quads (centre, a, b, b)
    c = mp(HEX_CX, HEX_CY, top_y)
    cu = uv(HEX_CX, HEX_CY)
    for k in range(6):
        a, b = hx_in[k], hx_in[(k + 1) % 6]
        va = mp(*a, top_y) + uv(*a) + (0, 1, 0)
        vb = mp(*b, top_y) + uv(*b) + (0, 1, 0)
        m.quad(GROUP_TOP, [c + cu + (0, 1, 0), vb, va, va])
    for k in range(6):
        a, b = hx[k], hx[(k + 1) % 6]
        ai, bi = hx_in[k], hx_in[(k + 1) % 6]
        mid = ((a[0] + b[0]) / 2 - HEX_CX, (a[1] + b[1]) / 2 - HEX_CY)
        L = math.hypot(*mid)
        nx, nz = mid[0] / L, mid[1] / L
        bn = np.array([nx, 1.4, nz]); bn /= np.linalg.norm(bn)
        m.quad(GROUP_TOP, [mp(*ai, top_y) + uv(*ai) + tuple(bn), mp(*a, bevel_y) + uv(*a) + tuple(bn),
                           mp(*b, bevel_y) + uv(*b) + tuple(bn), mp(*bi, top_y) + uv(*bi) + tuple(bn)])
        # vertical side: colour taken just inside the bevel
        ua = uv(a[0] - nx * 5, a[1] - nz * 5)
        ub = uv(b[0] - nx * 5, b[1] - nz * 5)
        sn = (nx, 0.0, nz)
        m.quad(GROUP_SIDE, [mp(*a, bevel_y) + ua + sn, mp(*a, base_y) + ua + sn, mp(*b, base_y) + ub + sn, mp(*b, bevel_y) + ub + sn])

    # ---- T125 track + ES tip (lathe) ----
    # profile from the wheel down to the tip point: (radius, y, swatch)
    prof = [                                   # UW145 track (upper wing) + EWD (eternal wide defense) tip
        (0.30, WHEEL_BOTTOM, 'track_light'),
        (0.30, WHEEL_BOTTOM - 0.04, 'track'),
        (0.21, WHEEL_BOTTOM - 0.08, 'track'),
        (0.21, 0.68, 'track'),
        (0.42, 0.64, 'track_light'),
        (0.42, 0.60, 'track_light'),
        (0.22, 0.56, 'track'),
        (0.19, 0.50, 'track'),
        (0.28, 0.46, 'track_light'),
        (0.40, 0.38, 'tip'),
        (0.44, 0.30, 'tip'),
        (0.42, 0.21, 'tip'),
        (0.30, 0.12, 'tip'),
        (0.18, 0.04, 'tip'),
        (0.08, 0.008, 'point'),
        (0.0, 0.0, 'point'),
    ]
    NL = 40
    for k in range(len(prof) - 1):
        r0, y0, s0 = prof[k]
        r1, y1, _ = prof[k + 1]
        dr, dy = r1 - r0, y1 - y0
        for i in range(NL):
            q = []
            for (rr, yy, ii, fv) in ((r0, y0, i, 0), (r1, y1, i, 1), (r1, y1, i + 1, 1), (r0, y0, i + 1, 0)):
                t = 2 * math.pi * ii / NL
                nn = np.array([-dy * math.cos(t), dr, -dy * math.sin(t)])
                nn = nn / (np.linalg.norm(nn) + 1e-9)
                u, v = sw_uv(s0, (ii % NL) / NL, fv)
                q.append((rr * math.cos(t), yy, rr * math.sin(t), u, v, nn[0], nn[1], nn[2]))
            m.quad(GROUP_PARTS, q)
    return m


def textures():
    os.makedirs(os.path.join(ASSETS, 'textures/entity'), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, 'textures/item'), exist_ok=True)
    img = Image.open(os.path.join(HERE, 'beffal_source_fixed.png')).convert('RGB')
    ia0 = np.asarray(img).copy()
    img = Image.fromarray(ia0)
    alpha = Image.fromarray((nd.binary_erosion(MASK, iterations=1) * 255).astype(np.uint8))
    rgba = img.copy()
    rgba.putalpha(alpha)
    # pad outward so bilinear sampling near the silhouette never picks up white background
    arr = np.asarray(rgba).astype(np.float32)
    inside = arr[..., 3] > 0
    idx = nd.distance_transform_edt(~inside, return_distances=False, return_indices=True)
    padded = arr.copy()
    padded[..., :3] = arr[idx[0], idx[1], :3]
    padded[..., 3] = arr[..., 3]
    rgba = Image.fromarray(padded.astype(np.uint8), 'RGBA')
    box = (int(CX - S), int(CY - S), int(CX + S), int(CY + S))
    canvas = Image.new('RGBA', (box[2] - box[0], box[3] - box[1]), (0, 0, 0, 0))
    canvas.paste(rgba.crop((max(0, box[0]), max(0, box[1]), min(W, box[2]), min(H, box[3]))),
                 (max(0, -box[0]), max(0, -box[1])))
    face = canvas.resize((TEX, TEX), Image.LANCZOS)
    fa = np.asarray(face).astype(np.float32)
    fa[..., 3] = np.where(fa[..., 3] > 128, 255, 0)
    # spin direction matters for L-Drago (left spin) but the artwork itself needs no change
    # slight contrast / saturation lift so it reads well under Minecraft lighting
    rgb = fa[..., :3] / 255.0
    lum = rgb @ np.array([0.3, 0.59, 0.11])
    rgb = np.clip(lum[..., None] + (rgb - lum[..., None]) * 1.12, 0, 1)
    rgb = np.clip((rgb - 0.5) * 1.06 + 0.52, 0, 1)
    fa[..., :3] = rgb * 255
    face = Image.fromarray(fa.astype(np.uint8), 'RGBA')
    face.save(os.path.join(ASSETS, 'textures/entity/beffal_face.png'))

    # emissive mask: purple wheel glows violet, red parts crimson, the eagle emblem gold
    g = np.zeros((H, W, 4), np.float32)
    g[GREEN] = (255, 70, 170, 150)
    g[GEMS] = np.concatenate([SRC[GEMS] * 255, np.full((GEMS.sum(), 1), 230)], 1)
    g[PINK] = (215, 110, 255, 255)
    ge = Image.fromarray(g.astype(np.uint8), 'RGBA').filter(ImageFilter.GaussianBlur(1.2))
    garr = np.asarray(ge).astype(np.float32)
    garr[..., :3] *= garr[..., 3:4] / 255.0       # premultiplied for additive blending
    garr[..., 3] = 255
    canvas = Image.new('RGBA', (box[2] - box[0], box[3] - box[1]), (0, 0, 0, 255))
    canvas.paste(Image.fromarray(garr.astype(np.uint8), 'RGBA').crop((max(0, box[0]), max(0, box[1]), min(W, box[2]), min(H, box[3]))),
                 (max(0, -box[0]), max(0, -box[1])))
    canvas.resize((512, 512), Image.LANCZOS).save(os.path.join(ASSETS, 'textures/entity/beffal_glow.png'))

    # parts swatches
    rng = np.random.default_rng(3)
    P = np.zeros((64, 64, 4), np.float32)
    P[..., 3] = 255
    yy, xx = np.mgrid[0:16, 0:16]

    def cell(name, base, stripes=0.0, noise=0.03):
        u0, v0 = SW[name]
        c = np.array(base, np.float32)[None, None, :] / 255.0
        shade = 1.0 + stripes * np.sin(xx / 16 * math.tau * 4)[..., None] + rng.normal(0, noise, (16, 16, 1))
        shade = shade * (0.85 + 0.3 * (1 - yy / 15))[..., None]
        P[v0:v0 + 16, u0:u0 + 16, :3] = np.clip(c * shade, 0, 1) * 255

    cell('track', (60, 40, 70), 0.08)
    cell('track_light', (200, 70, 130), 0.06)
    cell('tip', (150, 60, 160), 0.05)           # EWD tip: purple wide disk
    cell('under', (170, 172, 180), 0.05)
    cell('point', (205, 208, 214), 0.02)
    cell('bearing', (210, 60, 60), 0.1)
    Image.fromarray(P.astype(np.uint8), 'RGBA').save(os.path.join(ASSETS, 'textures/entity/beffal_parts.png'))

    # eagle emblem (from the face bolt) as an effect sprite: rgb = core + halo (additive), alpha = sharp silhouette
    x0, y0 = int(HEX_CX - HEX_R), int(HEX_CY - HEX_R)
    em = PINK[y0:y0 + int(2 * HEX_R), x0:x0 + int(2 * HEX_R)].astype(np.float32)
    core = Image.fromarray((em * 255).astype(np.uint8)).resize((256, 256), Image.LANCZOS)
    halo = core.filter(ImageFilter.GaussianBlur(6))
    a = np.clip(np.asarray(core).astype(np.float32) / 255 + np.asarray(halo).astype(np.float32) / 255 * 0.9, 0, 1)
    sharp = np.asarray(core).astype(np.float32) / 255
    os.makedirs(os.path.join(ASSETS, 'textures/fx'), exist_ok=True)
    # (the effect sprite is a drawn spread-wing eagle, see fx_textures.py)

    # item / HUD icon
    icon = face.resize((64, 64), Image.LANCZOS)
    ia = np.asarray(icon).copy()
    ia[..., 3] = np.where(ia[..., 3] > 100, 255, 0)
    Image.fromarray(ia, 'RGBA').save(os.path.join(ASSETS, 'textures/item/beffal_icon.png'))
    return face


if __name__ == '__main__':
    textures()
    mesh = build()
    os.makedirs(os.path.join(ASSETS, 'models'), exist_ok=True)
    mesh.write(os.path.join(ASSETS, 'models/beffal_mesh.bin'))
    print({k: len(v) for k, v in mesh.groups.items()})
