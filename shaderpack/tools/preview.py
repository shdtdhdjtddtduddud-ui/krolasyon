#!/usr/bin/env python3
"""Shader paketinin deferred/composite/final zincirini sentetik bir sahnede çalıştırır.

Minecraft olmadan gökyüzü, sis, su yansıması, bloom ve renk derecelendirmesini
görmek için kullanılır. Sahne: çimen düzlüğü, su gölü ve birkaç blok sütunu.

Kullanım:
  xvfb-run -a env MESA_GL_VERSION_OVERRIDE=4.5COMPAT python3 tools/preview.py OUT_DIR [--theme N]
"""
import math
import os
import re
import sys

import moderngl
import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from validate import SHADERS, resolve  # noqa: E402

W, H = 960, 540
FULLSCREEN_VS = """#version 330 compatibility
out vec2 uv;
void main() {
    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    uv = p;
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}
"""

# Sahte G-buffer üreten sahne: gerçek aydınlatma kütüphanesini kullanır
SCENE_FS = """#version 330 compatibility
#define FSH
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/atmosphere.glsl"
#include "/lib/shadow.glsl"
#include "/lib/lighting.glsl"
in vec2 uv;
layout(location = 0) out vec4 o0;
layout(location = 1) out vec4 o1;
layout(location = 2) out vec4 o2;

// kutu kesişimi
bool box(vec3 ro, vec3 rd, vec3 bmin, vec3 bmax, out float t, out vec3 n) {
    vec3 inv = 1.0 / rd;
    vec3 t0 = (bmin - ro) * inv, t1 = (bmax - ro) * inv;
    vec3 tmin = min(t0, t1), tmax = max(t0, t1);
    float tn = max(max(tmin.x, tmin.y), tmin.z);
    float tf = min(min(tmax.x, tmax.y), tmax.z);
    if (tf < max(tn, 0.0)) return false;
    t = tn;
    n = -sign(rd) * step(tmin.yzx, tmin.xyz) * step(tmin.zxy, tmin.xyz);
    return true;
}

void main() {
    initAtmosphere();
    vec3 vp = screenToView(uv, 1.0);
    vec3 rd = normalize(mat3(gbufferModelViewInverse) * vp);
    vec3 ro = cameraPosition;
    float tBest = 1e9; vec3 nBest = vec3(0); vec3 alb = vec3(0); float mat = 0.0; float emis = 0.0; float id = 0.0;

    // zemin (y=63) + göl (x>4 bölgesinde y=62)
    if (rd.y < 0.0) {
        float t = (63.0 - ro.y) / rd.y;
        vec3 p = ro + rd * t;
        if (p.x > 4.0 && p.x < 60.0 && p.z > -40.0 && p.z < 40.0) {
            t = (62.9 - ro.y) / rd.y; tBest = t; nBest = vec3(0, 1, 0); mat = MAT_WATER;
            alb = vec3(0.02, 0.25, 0.3);
        } else if (t < 400.0) {
            tBest = t; nBest = vec3(0, 1, 0); mat = MAT_TERRAIN;
            float chk = noise2(floor(p.xz * 4.0) * 0.37);
            alb = toLinear(vec3(0.33, 0.55, 0.22) * (0.8 + 0.4 * chk));
        }
    }
    // sütunlar
    for (int i = 0; i < 6; i++) {
        vec3 c = vec3(-6.0 + float(i) * 7.0, 63.0, -18.0 + float(i % 3) * 9.0);
        float hgt = 3.0 + float(i % 3) * 4.0;
        float t; vec3 n;
        if (box(ro, rd, c - vec3(1.0, 0.0, 1.0), c + vec3(1.0, hgt, 1.0), t, n) && t < tBest && t > 0.0) {
            tBest = t; nBest = n; mat = i == 2 ? MAT_EMISSIVE : MAT_TERRAIN;
            alb = i == 2 ? vec3(1.0, 0.8, 0.4) : toLinear(vec3(0.45, 0.33, 0.22));
            emis = i == 2 ? 3.0 : 0.0;
        }
    }

    if (tBest > 1e8) { o0 = vec4(0); o1 = vec4(0); o2 = vec4(0); gl_FragDepth = 1.0; return; }
    vec3 pp = rd * tBest;
    vec3 viewP = mat3(gbufferModelView) * pp;
    vec3 sp = viewToScreen(viewP);
    gl_FragDepth = sp.z;
    float direct;
    vec3 col = computeLighting(alb, pp, nBest, vec2(0.0, 1.0), 1.0, 0.0, emis, 0.5, direct);
    if (mat == MAT_WATER) col = alb * ambientColor * 0.5;
    o0 = vec4(col, 1.0);
    o1 = vec4(encodeNormal(mat3(gbufferModelView) * nBest), (mat + 0.5) / 255.0, 1.0);
    o2 = vec4(1.0, direct, 0.0, 1.0);
}
"""


def perspective(fov, aspect, near, far):
    f = 1.0 / math.tan(math.radians(fov) / 2)
    return np.array([
        [f / aspect, 0, 0, 0],
        [0, f, 0, 0],
        [0, 0, (far + near) / (near - far), 2 * far * near / (near - far)],
        [0, 0, -1, 0]], dtype=np.float64)


def look(yaw, pitch):
    # dünya → görüş: önce yaw (Y ekseni), sonra pitch (X ekseni)
    cy, sy = math.cos(yaw), math.sin(yaw)
    cp, sp = math.cos(pitch), math.sin(pitch)
    ry = np.array([[cy, 0, -sy, 0], [0, 1, 0, 0], [sy, 0, cy, 0], [0, 0, 0, 1]])
    rx = np.array([[1, 0, 0, 0], [0, cp, -sp, 0], [0, sp, cp, 0], [0, 0, 0, 1]])
    return rx @ ry


def sun_dir(elev_deg, path_rot_deg=-25.0):
    th = math.radians(path_rot_deg)
    east = np.array([1.0, 0.0, 0.0])
    up = np.array([0.0, math.cos(th), -math.sin(th)])
    e = math.radians(elev_deg)
    return math.cos(e) * east + math.sin(e) * up


def gl_mat(m):
    return tuple(np.asarray(m, dtype="f4").T.flatten())


class Pipeline:
    def __init__(self, ctx, overrides):
        self.ctx = ctx
        self.overrides = overrides
        self.vao_cache = {}

        def fs(name):
            return resolve(os.path.join(SHADERS, f"{name}.fsh"), overrides)

        self.progs = {n: ctx.program(vertex_shader=FULLSCREEN_VS, fragment_shader=fs(n))
                      for n in ("deferred", "composite", "composite1", "composite2", "final")}
        scene_src = SCENE_FS
        scene_src = re.sub(r'#include "([^"]+)"',
                           lambda m: resolve(os.path.join(SHADERS, m.group(1).lstrip("/")), overrides), scene_src)
        self.progs["scene"] = ctx.program(vertex_shader=FULLSCREEN_VS, fragment_shader=scene_src)

        def tex(dtype="f2"):
            return ctx.texture((W, H), 4, dtype=dtype)

        self.c0 = [tex(), tex()]
        self.c1 = tex("f2")
        self.c2 = tex("f1")
        self.c3 = tex()
        self.depth = ctx.depth_texture((W, H))
        self.depth.compare_func = ""
        self.depth_copy = ctx.depth_texture((W, H))
        self.shadow = ctx.depth_texture((64, 64))
        self.shadow.compare_func = "<="
        self.shadow_fbo = ctx.framebuffer(depth_attachment=self.shadow)
        self.shadowcolor = ctx.texture((64, 64), 4)
        self.out = ctx.texture((W, H), 4)

    def vao(self, prog):
        if prog not in self.vao_cache:
            self.vao_cache[prog] = self.ctx.vertex_array(self.progs[prog], [])
        return self.vao_cache[prog]

    def set_uniforms(self, prog, u):
        p = self.progs[prog]
        for k, v in u.items():
            if k in p:
                p[k].value = v

    def run(self, u, path):
        ctx = self.ctx
        self.shadow_fbo.clear(depth=1.0)
        units = {"colortex0": 0, "colortex1": 1, "colortex2": 2, "colortex3": 3,
                 "depthtex0": 4, "depthtex1": 5, "shadowtex0": 6, "shadowtex1": 7, "shadowcolor0": 8}

        def bind(prog, c0):
            c0.use(0); self.c1.use(1); self.c2.use(2); self.c3.use(3)
            self.depth.use(4); self.depth.use(5); self.shadow.use(6); self.shadow.use(7); self.shadowcolor.use(8)
            p = self.progs[prog]
            for k, v in units.items():
                if k in p:
                    p[k].value = v
            self.set_uniforms(prog, u)

        # sahne → c0[0], c1, c2, derinlik
        fbo = ctx.framebuffer([self.c0[0], self.c1, self.c2], self.depth)
        fbo.clear(depth=1.0)
        ctx.enable(moderngl.DEPTH_TEST)
        ctx.depth_func = "1"
        self.set_uniforms("scene", u)
        for k in ("shadowtex0", "shadowtex1", "shadowcolor0"):
            if k in self.progs["scene"]:
                self.progs["scene"][k].value = units[k]
        self.shadow.use(6); self.shadow.use(7); self.shadowcolor.use(8)
        fbo.use()
        self.vao("scene").render(moderngl.TRIANGLES, vertices=3)
        ctx.disable(moderngl.DEPTH_TEST)

        cur = 0
        for prog in ("deferred", "composite"):
            dst = 1 - cur
            f = ctx.framebuffer([self.c0[dst]])
            bind(prog, self.c0[cur])
            f.use()
            self.vao(prog).render(moderngl.TRIANGLES, vertices=3)
            cur = dst
        self.c0[cur].build_mipmaps()
        f = ctx.framebuffer([self.c3])
        bind("composite1", self.c0[cur]); f.use()
        self.vao("composite1").render(moderngl.TRIANGLES, vertices=3)
        self.c0[cur].filter = (moderngl.LINEAR, moderngl.LINEAR)
        dst = 1 - cur
        f = ctx.framebuffer([self.c0[dst]])
        bind("composite2", self.c0[cur]); f.use()
        self.vao("composite2").render(moderngl.TRIANGLES, vertices=3)
        cur = dst
        f = ctx.framebuffer([self.out])
        bind("final", self.c0[cur]); f.use()
        self.vao("final").render(moderngl.TRIANGLES, vertices=3)
        img = Image.frombytes("RGBA", (W, H), self.out.read()).transpose(Image.FLIP_TOP_BOTTOM)
        img.convert("RGB").save(path)


def uniforms(sun_elev, yaw_deg, pitch_deg, t=100.0, rain=0.0, eye_water=0, cam_y=70.0):
    proj = perspective(70.0, W / H, 0.05, 512.0)
    mv = look(math.radians(yaw_deg), math.radians(pitch_deg))
    sd = sun_dir(sun_elev)
    r = mv[:3, :3]
    sun_v = r @ sd * 100.0
    light = sun_v if sun_elev >= 0 else -sun_v
    return {
        "gbufferProjection": gl_mat(proj),
        "gbufferProjectionInverse": gl_mat(np.linalg.inv(proj)),
        "gbufferModelView": gl_mat(mv),
        "gbufferModelViewInverse": gl_mat(np.linalg.inv(mv)),
        "shadowModelView": gl_mat(np.eye(4)),
        "shadowProjection": gl_mat(np.diag([0.0, 0.0, 0.0, 1.0])),
        "cameraPosition": (0.0, cam_y, 0.0),
        "sunPosition": tuple(sun_v),
        "moonPosition": tuple(-sun_v),
        "shadowLightPosition": tuple(light),
        "upPosition": tuple(r @ np.array([0, 100.0, 0])),
        "fogColor": (0.6, 0.7, 0.9),
        "frameTimeCounter": t,
        "rainStrength": rain,
        "viewWidth": float(W), "viewHeight": float(H),
        "near": 0.05, "far": 192.0,
        "moonPhase": 1,
        "isEyeInWater": eye_water,
        "eyeBrightnessSmooth": (0, 240),
        "atlasSize": (1024, 1024),
    }


SHOTS = [
    ("noon", 70, -60, -8),
    ("morning", 12, -95, -4),
    ("sunset", 3, -90, -2),
    ("dusk", -6, -90, 0),
    ("night", -45, 200, 12),
    ("night_planet", -45, 250, 25),
    ("night_lake", -40, -110, -10),
    ("noon_lake", 60, -110, -12),
    ("rain", 50, -60, -5),
    ("sky_n", -80, 0, -35),
    ("sky_e", -80, -90, -35),
    ("sky_s", -80, 180, -35),
    ("sky_w", -80, 90, -35),
    ("sky_zenith", -80, 0, -80),
    ("day_sky", 35, 60, -30),
]


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "preview_out"
    theme = 0
    if "--theme" in sys.argv:
        theme = int(sys.argv[sys.argv.index("--theme") + 1])
    os.makedirs(out, exist_ok=True)
    ctx = moderngl.create_standalone_context()
    pipe = Pipeline(ctx, {"SKY_THEME": theme})
    for name, elev, yaw, pitch in SHOTS:
        u = uniforms(elev, yaw, pitch, rain=1.0 if name == "rain" else 0.0)
        path = os.path.join(out, f"t{theme}_{name}.png")
        pipe.run(u, path)
        print("wrote", path)


if __name__ == "__main__":
    main()
