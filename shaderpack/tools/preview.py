"""Offline preview: renders a procedural fjord test scene through the shader
pack programs (a tiny Iris-like pipeline) and writes a PNG. Works headless
with Mesa (llvmpipe). Needs: pip install PyOpenGL glfw numpy pillow
Usage:
  xvfb-run -a python3 preview.py out.png [rain|cloudy|sunset|night|morning] [EXTRA,DEFINES] [shore|high]
Environment: PACK=<shaders dir>, SKIP=composite1,..., DUMP="x:y;x:y" (print pass outputs)"""
import os, re, sys, math, json
import numpy as np
import glfw
from OpenGL.GL import *
from PIL import Image

ROOT = os.environ.get("PACK", os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "FjordMist", "shaders"))
W, H = 960, 540
FAR = 320.0
SHADOW_RES = 2048
SHADOW_DIST = 128.0

PRESETS = {
    "rain":   dict(time="day", sunElev=0.55, rain=1.0, wet=1.0),
    "cloudy": dict(time="day", sunElev=0.6, rain=0.0, wet=0.0),
    "sunset": dict(time="sunset", sunElev=0.06, rain=0.0, wet=0.0),
    "night":  dict(time="night", sunElev=-0.5, rain=0.0, wet=0.0),
    "morning": dict(time="day", sunElev=0.2, rain=0.3, wet=0.5),
    "storm":  dict(time="day", sunElev=0.45, rain=1.0, wet=1.0, thunder=1.0),
    "torch":  dict(time="night", sunElev=-0.5, rain=0.0, wet=0.0, held=14),
    "sunny":  dict(time="day", sunElev=0.7, rain=0.0, wet=0.0, overcast_off=True),
}
preset = PRESETS[sys.argv[2] if len(sys.argv) > 2 else "rain"]
extra_defines = sys.argv[3].split(",") if len(sys.argv) > 3 and sys.argv[3] else []
cam_name = sys.argv[4] if len(sys.argv) > 4 else "shore"

IRIS_DEFINES = "#define MC_VERSION 12001\n#define MC_RENDER_STAGE_SUN 3\n#define MC_RENDER_STAGE_MOON 4\n#define MC_RENDER_STAGE_STARS 5\n"
INC = re.compile(r'^\s*#include\s+"([^"]+)"', re.M)

def expand(path):
    src = open(path).read()
    return INC.sub(lambda m: expand(os.path.join(ROOT, m.group(1).lstrip("/"))), src)

def load_program(name):
    srcs = []
    for ext, kind in (("vsh", GL_VERTEX_SHADER), ("fsh", GL_FRAGMENT_SHADER)):
        s = expand(os.path.join(ROOT, name + "." + ext))
        lines = s.split("\n")
        s = lines[0] + "\n" + IRIS_DEFINES + "".join("#define %s\n" % d for d in extra_defines) + "\n".join(lines[1:])
        sh = glCreateShader(kind)
        glShaderSource(sh, s)
        glCompileShader(sh)
        if not glGetShaderiv(sh, GL_COMPILE_STATUS):
            raise RuntimeError(name + ext + glGetShaderInfoLog(sh).decode())
        srcs.append(sh)
    p = glCreateProgram()
    for sh in srcs: glAttachShader(p, sh)
    glBindAttribLocation(p, 10, "mc_Entity")
    glBindAttribLocation(p, 11, "mc_midTexCoord")
    glLinkProgram(p)
    if not glGetProgramiv(p, GL_LINK_STATUS):
        raise RuntimeError(name + glGetProgramInfoLog(p).decode())
    return p

# ------------------------------------------------------------- math
def normalize(v): return v / np.linalg.norm(v)

def look_dir(fwd, up=np.array([0, 1, 0.0])):
    f = normalize(fwd); s = normalize(np.cross(f, up)); u = np.cross(s, f)
    m = np.identity(4)
    m[0, :3] = s; m[1, :3] = u; m[2, :3] = -f
    return m

def perspective(fovy, aspect, n, f):
    t = 1 / math.tan(math.radians(fovy) / 2)
    m = np.zeros((4, 4))
    m[0, 0] = t / aspect; m[1, 1] = t
    m[2, 2] = (f + n) / (n - f); m[2, 3] = 2 * f * n / (n - f); m[3, 2] = -1
    return m

def ortho(l, r, b, t, n, f):
    m = np.identity(4)
    m[0, 0] = 2 / (r - l); m[1, 1] = 2 / (t - b); m[2, 2] = -2 / (f - n)
    m[0, 3] = -(r + l) / (r - l); m[1, 3] = -(t + b) / (t - b); m[2, 3] = -(f + n) / (f - n)
    return m

# ------------------------------------------------------------- scene
rng = np.random.default_rng(3)

def vnoise(x, z, scale, seed):
    r = np.random.default_rng(seed)
    g = r.random((64, 64))
    xs = x / scale; zs = z / scale
    xi = np.floor(xs).astype(int); zi = np.floor(zs).astype(int)
    fx = xs - xi; fz = zs - zi
    fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz)
    a = g[xi % 64, zi % 64]; b = g[(xi + 1) % 64, zi % 64]
    c = g[xi % 64, (zi + 1) % 64]; d = g[(xi + 1) % 64, (zi + 1) % 64]
    return (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz

def height(x, z):
    # fjord running along -z, slightly curving
    cx = 18 * np.sin(z * 0.006)
    d = np.abs(x - cx)
    walls = np.clip(d - 34, 0, None)
    h = 50 + walls * 1.25 + walls ** 1.5 * 0.035
    h += (vnoise(x, z, 60, 1) - 0.5) * 50 * np.clip(walls / 40, 0, 1)
    h += (vnoise(x, z, 17, 2) - 0.5) * 8
    # a big peak at the end of the fjord
    peak = 175 * np.exp(-(((x - 20) / 140) ** 2 + ((z + 520) / 110) ** 2))
    h += peak
    # beach on the right side close to the camera
    h = h + 15 * np.exp(-((x - 52) ** 2 + (z - 24) ** 2) / 500.0)
    return h

CAM_PRESETS = {
    "shore": (np.array([36.0, 65.0, 34.0]), np.array([-0.30, 0.0, -1.0])),
    "high":  (np.array([70.0, 110.0, 60.0]), np.array([-0.4, -0.12, -1.0])),
}
cam, cam_fwd = CAM_PRESETS[cam_name]
cam = cam.copy(); cam[1] = max(cam[1], float(height(np.array([cam[0]]), np.array([cam[2]]))[0]) + 1.7)

verts = []   # x y z (feet), r g b a, nx ny nz, u v, lu lv, id, midv
def add_quad_list(arr): verts.append(arr)

def build_terrain():
    step = 2.0
    xs = np.arange(-330, 330 + step, step) + np.floor(cam[0])
    zs = np.arange(-560, 330 + step, step) + np.floor(cam[2])
    X, Z = np.meshgrid(xs, zs, indexing="ij")
    Hh = height(X, Z)
    gx = (height(X + 0.5, Z) - height(X - 0.5, Z))
    gz = (height(X, Z + 0.5) - height(X, Z - 0.5))
    N = np.stack([-gx, np.ones_like(gx), -gz], -1)
    N /= np.linalg.norm(N, axis=-1, keepdims=True)
    slope = 1 - N[..., 1]
    n2 = vnoise(X, Z, 6, 5)
    grass = np.array([0.36, 0.55, 0.26]); rock = np.array([0.42, 0.42, 0.43]); dark = np.array([0.22, 0.24, 0.22])
    sand = np.array([0.50, 0.48, 0.44])
    t_rock = np.clip((slope - 0.25) * 4, 0, 1)[..., None]
    col = grass * (1 - t_rock) + rock * t_rock
    shore = np.clip(1 - (Hh - 62) / 3, 0, 1)[..., None]
    col = col * (1 - shore) + (sand * 0.7 + dark * 0.3) * shore
    col *= (0.8 + 0.4 * n2)[..., None]
    nx, nz = X.shape
    idx = np.arange(nx * nz).reshape(nx, nz)
    a = idx[:-1, :-1].ravel(); b = idx[1:, :-1].ravel(); c = idx[1:, 1:].ravel(); d = idx[:-1, 1:].ravel()
    tris = np.stack([a, b, c, a, c, d], -1).reshape(-1)
    P = np.stack([X, Hh, Z], -1).reshape(-1, 3) - cam
    C = np.concatenate([col.reshape(-1, 3), np.ones((nx * nz, 1))], -1)
    Nn = N.reshape(-1, 3)
    UV = np.stack([X.ravel() * 0.25, Z.ravel() * 0.25], -1)
    LM = np.tile([0.03, 0.97], (nx * nz, 1))
    ID = np.zeros((nx * nz, 1)); MID = np.zeros((nx * nz, 1))
    arr = np.concatenate([P, C, Nn, UV, LM, ID, MID], -1)[tris]
    return arr, Hh, X, Z

def build_trees(Hh, X, Z):
    out = []
    pts = rng.uniform([-320, -560], [320, 320], (5000, 2)) + cam[[0, 2]]
    for px, pz in pts:
        h = float(height(np.array([px]), np.array([pz]))[0])
        if h < 64 or h > 165: continue
        gx = float(height(np.array([px + 1]), np.array([pz]))[0] - height(np.array([px - 1]), np.array([pz]))[0]) / 2
        gz = float(height(np.array([px]), np.array([pz + 1]))[0] - height(np.array([px]), np.array([pz - 1]))[0]) / 2
        if math.hypot(gx, gz) > 1.6: continue
        th = rng.uniform(9, 17); r = th * 0.28
        base = np.array([px, h + 1.5, pz]); top = base + [0, th, 0]
        col = np.array([0.20, 0.33, 0.20]) * rng.uniform(0.75, 1.1)
        seg = 7
        for i in range(seg):
            a0 = 2 * math.pi * i / seg; a1 = 2 * math.pi * (i + 1) / seg
            p0 = base + [r * math.cos(a0), 0, r * math.sin(a0)]
            p1 = base + [r * math.cos(a1), 0, r * math.sin(a1)]
            nrm = normalize(np.cross(p1 - top, p0 - top))
            if nrm[1] < 0: nrm = -nrm
            for p, uv in ((p0, (0, 1)), (p1, (1, 1)), (top, (0.5, 0))):
                out.append(list(p - cam) + list(col) + [1] + list(nrm) + list(uv) + [0.03, 0.95, 10005, 0.5])
    return np.array(out)

def build_grass():
    out = []
    pts = rng.uniform([-30, -40], [30, 25], (6000, 2)) + cam[[0, 2]]
    for px, pz in pts:
        h = float(height(np.array([px]), np.array([pz]))[0])
        if h < 64.0: continue
        gx = float(height(np.array([px + 1]), np.array([pz]))[0] - height(np.array([px - 1]), np.array([pz]))[0]) / 2
        if abs(gx) > 0.9: continue
        a = rng.uniform(0, math.pi); hh = rng.uniform(0.6, 1.1); w = 0.5
        dx, dz = math.cos(a) * w, math.sin(a) * w
        col = np.array([0.34, 0.55, 0.24]) * rng.uniform(0.7, 1.15)
        b0 = np.array([px - dx, h, pz - dz]); b1 = np.array([px + dx, h, pz + dz])
        t0 = b0 + [0, hh, 0]; t1 = b1 + [0, hh, 0]
        nrm = normalize(np.cross(b1 - b0, t0 - b0))
        for p, uv in ((b0, (0, 1)), (b1, (1, 1)), (t1, (1, 0)), (b0, (0, 1)), (t1, (1, 0)), (t0, (0, 0))):
            out.append(list(p - cam) + list(col) + [1] + list(nrm) + list(uv) + [0.03, 0.97, 10002, 0.5])
    return np.array(out)

def build_water():
    s = 4.0
    xs = np.arange(-330, 330 + s, s) + np.floor(cam[0]); zs = np.arange(-560, 330 + s, s) + np.floor(cam[2])
    out = []
    for i in range(len(xs) - 1):
        for j in range(len(zs) - 1):
            x0, x1, z0, z1 = xs[i], xs[i + 1], zs[j], zs[j + 1]
            hmin = height(np.array([x0, x1, x0, x1]), np.array([z0, z0, z1, z1])).min()
            if hmin > 63: continue
            y = 62.9
            for (x, z) in ((x0, z0), (x1, z0), (x1, z1), (x0, z0), (x1, z1), (x0, z1)):
                out.append([x - cam[0], y - cam[1], z - cam[2], 0.25, 0.4, 0.8, 0.7, 0, 1, 0, 0, 0, 0.03, 0.97, 10001, 0])
    return np.array(out)

terrain, Hh, X, Z = build_terrain()
trees = build_trees(Hh, X, Z)
grass = build_grass()
water = build_water()
opaque = np.concatenate([terrain, trees, grass]).astype(np.float32)
water = water.astype(np.float32)

# ------------------------------------------------------------- GL setup
glfw.init()
glfw.window_hint(glfw.VISIBLE, False)
win = glfw.create_window(W, H, "h", None, None)
glfw.make_context_current(win)

def vbo(data):
    b = glGenBuffers(1); glBindBuffer(GL_ARRAY_BUFFER, b)
    glBufferData(GL_ARRAY_BUFFER, data.nbytes, data, GL_STATIC_DRAW)
    return b, len(data)

def draw(buf):
    b, n = buf
    stride = 16 * 4
    glBindBuffer(GL_ARRAY_BUFFER, b)
    glEnableClientState(GL_VERTEX_ARRAY); glVertexPointer(3, GL_FLOAT, stride, ctypes.c_void_p(0))
    glEnableClientState(GL_COLOR_ARRAY); glColorPointer(4, GL_FLOAT, stride, ctypes.c_void_p(12))
    glEnableClientState(GL_NORMAL_ARRAY); glNormalPointer(GL_FLOAT, stride, ctypes.c_void_p(28))
    glClientActiveTexture(GL_TEXTURE0); glEnableClientState(GL_TEXTURE_COORD_ARRAY); glTexCoordPointer(2, GL_FLOAT, stride, ctypes.c_void_p(40))
    glClientActiveTexture(GL_TEXTURE1); glEnableClientState(GL_TEXTURE_COORD_ARRAY); glTexCoordPointer(2, GL_FLOAT, stride, ctypes.c_void_p(48))
    glEnableVertexAttribArray(10); glVertexAttribPointer(10, 1, GL_FLOAT, False, stride, ctypes.c_void_p(56))
    glEnableVertexAttribArray(11); glVertexAttribPointer(11, 2, GL_FLOAT, False, stride, ctypes.c_void_p(56))
    glDrawArrays(GL_TRIANGLES, 0, n)

import ctypes
opaque_buf = vbo(opaque); water_buf = vbo(water)
rain_quads = []
if preset["rain"] > 0:
    for _ in range(260):
        a = rng.uniform(0, 2 * math.pi); r = rng.uniform(1.0, 10.0)
        x, z = math.cos(a) * r, math.sin(a) * r
        dx, dz = -math.sin(a) * 0.5, math.cos(a) * 0.5
        y0 = -2.0; y1 = 8.0; v0 = rng.uniform(0, 4)
        for (px, py, pz, u, v) in ((x - dx, y0, z - dz, 0, v0 + 2.5), (x + dx, y0, z + dz, 1, v0 + 2.5), (x + dx, y1, z + dz, 1, v0),
                                   (x - dx, y0, z - dz, 0, v0 + 2.5), (x + dx, y1, z + dz, 1, v0), (x - dx, y1, z - dz, 0, v0)):
            rain_quads.append([px, py, pz, 1, 1, 1, 1, 0, 1, 0, u, v, 0.03, 0.97, 0, 0])
rain_buf = vbo(np.array(rain_quads, np.float32)) if rain_quads else None

def tex2d(fmt, w, h, ext_fmt=GL_RGBA, typ=GL_FLOAT, data=None, filt=GL_LINEAR, wrap=GL_CLAMP_TO_EDGE):
    t = glGenTextures(1); glBindTexture(GL_TEXTURE_2D, t)
    glTexImage2D(GL_TEXTURE_2D, 0, fmt, w, h, 0, ext_fmt, typ, data)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filt)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR if filt != GL_NEAREST else GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap)
    return t

ct0 = tex2d(GL_RGBA16F, W, H); ct0b = tex2d(GL_RGBA16F, W, H)
ct5 = tex2d(GL_RGBA16F, W, H); ct1 = tex2d(GL_RGBA16, W, H); ct2 = tex2d(GL_RGBA8, W, H); ct3 = tex2d(GL_R11F_G11F_B10F, W, H, GL_RGB)
dt0 = tex2d(GL_DEPTH_COMPONENT32F, W, H, GL_DEPTH_COMPONENT, GL_FLOAT, filt=GL_NEAREST)
dt1 = tex2d(GL_DEPTH_COMPONENT32F, W, H, GL_DEPTH_COMPONENT, GL_FLOAT, filt=GL_NEAREST)
st0 = tex2d(GL_DEPTH_COMPONENT32F, SHADOW_RES, SHADOW_RES, GL_DEPTH_COMPONENT, GL_FLOAT)
glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE)
glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL)
sc0 = tex2d(GL_RGBA8, SHADOW_RES, SHADOW_RES)

img = Image.open(os.path.join(ROOT, "textures/noise.png")).convert("RGBA")
noise = tex2d(GL_RGBA8, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, np.array(img).tobytes(), GL_LINEAR, GL_REPEAT)
# block texture: subtle grain
bt = (rng.uniform(0.82, 1.0, (16, 16, 1)) * 255).astype(np.uint8)
bt = np.concatenate([bt, bt, bt, np.full((16, 16, 1), 255, np.uint8)], -1)
blocktex = tex2d(GL_RGBA8, 16, 16, GL_RGBA, GL_UNSIGNED_BYTE, bt.tobytes(), GL_NEAREST, GL_REPEAT)
# rain texture: a few thin drops per column, like the vanilla rain.png
rt = np.zeros((256, 64, 4), np.uint8)
for col in range(64):
    for _ in range(1):
        if rng.random() < 0.12:
            y = rng.integers(0, 240); rt[y:y + 14, col] = (200, 210, 230, 170)
raintex = tex2d(GL_RGBA8, 64, 256, GL_RGBA, GL_UNSIGNED_BYTE, rt.tobytes(), GL_NEAREST, GL_REPEAT)

def fbo(colors, depth):
    f = glGenFramebuffers(1); glBindFramebuffer(GL_FRAMEBUFFER, f)
    for i, c in enumerate(colors):
        if c is not None: glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + i, GL_TEXTURE_2D, c, 0)
    if depth: glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0)
    assert glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE
    return f

main_fbo = fbo([ct0, ct1, ct2, ct3, None, ct5], dt0)
shadow_fbo = fbo([sc0], st0)
comp_fbo_a = fbo([ct0b], None)
comp_fbo_b = fbo([ct0], None)
out_tex = tex2d(GL_RGBA8, W, H)
out_fbo = fbo([out_tex], None)

# ------------------------------------------------------------- uniforms
sun_rot = math.radians(-25.0)
e = preset["sunElev"]
# sun moves in the x/y plane, tilted around z by sunPathRotation; morning sun from +x
az = math.acos(max(-1, min(1, e)))
sun_w = np.array([math.cos(az) * (1 if preset["time"] != "sunset" else -1), math.sin(math.asin(e)) if False else e, 0.0])
sun_w = np.array([math.sqrt(max(0, 1 - e * e)), e, 0.0])
if preset["time"] == "sunset": sun_w[0] = -sun_w[0]
# sunPathRotation: rotate around x axis (MC rotates the celestial path around the north axis)
c, s = math.cos(sun_rot), math.sin(sun_rot)
sun_w = np.array([sun_w[0], sun_w[1] * c, sun_w[1] * s])
sun_w = normalize(sun_w + np.array([0, 0, -0.35]))
moon_w = -sun_w
light_w = sun_w if e > 0 else moon_w

view = look_dir(cam_fwd)
proj = perspective(70, W / H, 0.05, FAR * 4)
sv = look_dir(-light_w, np.array([0, 0, 1.0]) if abs(light_w[1]) > 0.99 else np.array([0, 1, 0.0]))
sv_t = np.identity(4); sv_t[:3, 3] = -(light_w * 100)
shadow_mv = sv @ sv_t
shadow_proj = ortho(-SHADOW_DIST, SHADOW_DIST, -SHADOW_DIST, SHADOW_DIST, 0.05, 256)

def v4(m, v): return (m @ np.append(v, 0))[:3]

U = {
    "sunPosition": v4(view, sun_w * 100), "moonPosition": v4(view, moon_w * 100),
    "shadowLightPosition": v4(view, light_w * 100), "upPosition": v4(view, np.array([0, 100.0, 0])),
    "cameraPosition": cam, "fogColor": np.array([0.6, 0.65, 0.7]), "skyColor": np.array([0.5, 0.6, 0.8]),
    "gbufferModelView": view, "gbufferModelViewInverse": np.linalg.inv(view),
    "gbufferProjection": proj, "gbufferProjectionInverse": np.linalg.inv(proj),
    "shadowModelView": shadow_mv, "shadowModelViewInverse": np.linalg.inv(shadow_mv), "shadowProjection": shadow_proj,
    "frameTimeCounter": 123.4, "rainStrength": preset["rain"], "wetness": preset["wet"],
    "thunderStrength": float(preset.get("thunder", 0.0)), "heldBlockLightValue": int(preset.get("held", 0)), "heldBlockLightValue2": 0,
    "viewWidth": float(W), "viewHeight": float(H), "near": 0.05, "far": FAR, "sunAngle": 0.25,
    "isEyeInWater": 0, "worldTime": 6000, "frameCounter": 7, "eyeBrightnessSmooth": (0, 240), "renderStage": 0,
    "entityColor": np.zeros(4),
}
SAMPLERS = {"texture": (0, blocktex), "tex": (0, blocktex), "noisetex": (1, noise), "colortex0": (2, ct0), "colortex1": (3, ct1),
            "colortex2": (4, ct2), "colortex5": (10, ct5), "colortex3": (5, ct3), "depthtex0": (6, dt0), "depthtex1": (7, dt1), "shadowtex0": (8, st0)}

def use(p, overrides={}):
    glUseProgram(p)
    for k, v in {**U, **overrides}.items():
        loc = glGetUniformLocation(p, k)
        if loc == -1: continue
        if isinstance(v, np.ndarray) and v.shape == (4, 4): glUniformMatrix4fv(loc, 1, GL_TRUE, v.astype(np.float32))
        elif isinstance(v, tuple): glUniform2i(loc, *v)
        elif isinstance(v, int): glUniform1i(loc, v)
        elif isinstance(v, float): glUniform1f(loc, v)
        elif len(v) == 3: glUniform3f(loc, *v)
        elif len(v) == 4: glUniform4f(loc, *v)
    for k, (unit, t) in SAMPLERS.items():
        loc = glGetUniformLocation(p, k)
        if loc == -1: continue
        glActiveTexture(GL_TEXTURE0 + unit); glBindTexture(GL_TEXTURE_2D, t); glUniform1i(loc, unit)
    glActiveTexture(GL_TEXTURE0)

def set_matrices(mv, pr):
    glMatrixMode(GL_PROJECTION); glLoadMatrixf(pr.T.astype(np.float32))
    glMatrixMode(GL_MODELVIEW); glLoadMatrixf(mv.T.astype(np.float32))

def fullscreen():
    set_matrices(np.identity(4), ortho(0, 1, 0, 1, -1, 1))
    glBegin(GL_QUADS)
    for x, y in ((0, 0), (1, 0), (1, 1), (0, 1)):
        glMultiTexCoord2f(GL_TEXTURE0, x, y); glVertex3f(x, y, 0)
    glEnd()

P = {n: load_program(n) for n in ["shadow", "gbuffers_skybasic", "gbuffers_terrain", "gbuffers_water", "composite", "composite1", "composite2", "final", "gbuffers_weather"]}

glDisable(GL_CULL_FACE)
# shadow
glBindFramebuffer(GL_FRAMEBUFFER, shadow_fbo); glViewport(0, 0, SHADOW_RES, SHADOW_RES)
glClearColor(1, 1, 1, 1); glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
glEnable(GL_DEPTH_TEST)
use(P["shadow"]); set_matrices(shadow_mv, shadow_proj); draw(opaque_buf)

# gbuffers
glBindFramebuffer(GL_FRAMEBUFFER, main_fbo); glViewport(0, 0, W, H)
glDrawBuffers(6, [GL_COLOR_ATTACHMENT0 + i if i != 4 else GL_NONE for i in range(6)])
glClearColor(0, 0, 0, 0); glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
# sky: big box with depth writes off
glDepthMask(False); glDisable(GL_DEPTH_TEST)
glDrawBuffers(2, [GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT3])
use(P["gbuffers_skybasic"]); set_matrices(view, proj)
glBegin(GL_QUADS)
R = 300
for face in [((-R,-R,-R),(R,-R,-R),(R,R,-R),(-R,R,-R)), ((-R,-R,R),(R,-R,R),(R,R,R),(-R,R,R)),
             ((-R,-R,-R),(-R,R,-R),(-R,R,R),(-R,-R,R)), ((R,-R,-R),(R,R,-R),(R,R,R),(R,-R,R)),
             ((-R,R,-R),(R,R,-R),(R,R,R),(-R,R,R)), ((-R,-R,-R),(R,-R,-R),(R,-R,R),(-R,-R,R))]:
    for p in face: glColor4f(0.5, 0.6, 0.7, 1); glVertex3f(*p)
glEnd()
glDepthMask(True); glEnable(GL_DEPTH_TEST)
glDrawBuffers(3, [GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2])
use(P["gbuffers_terrain"]); set_matrices(view, proj); draw(opaque_buf)
# depthtex1 = copy before translucents
glBindTexture(GL_TEXTURE_2D, dt1); glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, W, H)
glEnablei(GL_BLEND, 0); glDisablei(GL_BLEND, 1); glDisablei(GL_BLEND, 2)
glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
use(P["gbuffers_water"]); set_matrices(view, proj); draw(water_buf)
if rain_buf:
    glDepthMask(False); glDrawBuffers(1, [GL_COLOR_ATTACHMENT5])
    glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
    SAMPLERS["texture"] = (0, raintex)
    use(P["gbuffers_weather"]); set_matrices(view, proj); draw(rain_buf)
    SAMPLERS["texture"] = (0, blocktex); glDepthMask(True)
glDisable(GL_BLEND)
glDisable(GL_DEPTH_TEST)

# composite passes
ct4 = tex2d(GL_RGBA16F, W, H)
fbo4 = fbo([ct4], None)
SAMPLERS["colortex4"] = (9, ct4)
def dump(name):
    if os.environ.get("DUMP"):
        for q in os.environ["DUMP"].split(";"):
            x, y = map(int, q.split(":"))
            print(name, (x, y), np.array(glReadPixels(x, H - 1 - y, 1, 1, GL_RGBA, GL_FLOAT)).ravel())
passes = [("composite", ct0, comp_fbo_a), ("composite1", ct0b, fbo4), ("composite2", ct0b, comp_fbo_b)]
for name, srcTex, target in passes:
    if name in os.environ.get("SKIP", "").split(","):
        continue
    glBindFramebuffer(GL_FRAMEBUFFER, target); glDrawBuffers(1, [GL_COLOR_ATTACHMENT0])
    SAMPLERS["colortex0"] = (2, srcTex)
    use(P[name]); fullscreen(); dump(name)
src = ct0 if "composite2" not in os.environ.get("SKIP", "") else ct0b
glBindTexture(GL_TEXTURE_2D, src); glGenerateMipmap(GL_TEXTURE_2D); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
SAMPLERS["colortex0"] = (2, src)
glBindFramebuffer(GL_FRAMEBUFFER, out_fbo); glDrawBuffers(1, [GL_COLOR_ATTACHMENT0])
use(P["final"]); fullscreen()
glFinish()
data = glReadPixels(0, 0, W, H, GL_RGBA, GL_UNSIGNED_BYTE)
im = Image.frombytes("RGBA", (W, H), data).transpose(Image.FLIP_TOP_BOTTOM).convert("RGB")
im.save(sys.argv[1])
arr = np.asarray(im).astype(float)
print("saved", sys.argv[1], "mean", arr.mean(axis=(0, 1)).round(1), "nan-ish black px", int((arr.sum(-1) == 0).sum()))
