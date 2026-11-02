"""Generates the 'Hatman Analog' shaderpack (OptiFine / Iris / Oculus format) and zips it."""
import os, shutil, zipfile

ROOT = os.path.join(os.path.dirname(__file__), '..')
P = os.path.join(ROOT, 'HatmanAnalog', 'shaders')
if os.path.exists(os.path.join(ROOT, 'HatmanAnalog')):
    shutil.rmtree(os.path.join(ROOT, 'HatmanAnalog'))
os.makedirs(P + '/lib'); os.makedirs(P + '/lang')


def w(name, text):
    with open(os.path.join(P, name), 'w') as f:
        f.write(text.lstrip('\n'))


w('lib/settings.glsl', r'''
// ---- Hatman Analog settings (edited from the in-game shader options menu) ----
#define SHADOWS
#define SHADOW_SOFTNESS 1.0 // [0.5 1.0 1.5 2.0 3.0]
#define SUN_STRENGTH 1.0 // [0.5 0.75 1.0 1.25 1.5]
#define NIGHT_DARKNESS 1.0 // [0.5 0.75 1.0 1.25 1.5 2.0]
#define CANDLE_FLICKER
#define BLOCKLIGHT_STRENGTH 1.0 // [0.5 0.75 1.0 1.25 1.5 2.0]
#define EMISSIVE_STRENGTH 1.0 // [0.0 0.5 1.0 1.5 2.0 3.0]
#define WAVING_PLANTS
#define HATMAN_SHADOW_FORM
#define FOG_DENSITY 1.0 // [0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define GROUND_MIST
#define MIST_DENSITY 1.0 // [0.25 0.5 1.0 1.5 2.0]
#define BLOOM_STRENGTH 1.0 // [0.0 0.25 0.5 1.0 1.5 2.0]
#define EXPOSURE 1.0 // [0.5 0.75 1.0 1.25 1.5 2.0]
#define SATURATION 0.78 // [0.0 0.4 0.6 0.78 0.9 1.0 1.2]
#define CONTRAST 1.08 // [0.9 1.0 1.08 1.15 1.25]
#define COLOR_GRADE 1 // [0 1 2 3]
#define VIGNETTE 1.0 // [0.0 0.5 1.0 1.5 2.0]
#define FILM_GRAIN 0.6 // [0.0 0.3 0.6 1.0 1.5]
#define VHS_EFFECT
#define VHS_STRENGTH 1.0 // [0.5 1.0 1.5 2.0]
#define SCANLINES
#define REC_OVERLAY
''')

w('lib/common.glsl', r'''
float hash12(vec2 p) { vec3 p3 = fract(vec3(p.xyx) * 0.1031); p3 += dot(p3, p3.yzx + 33.33); return fract((p3.x + p3.y) * p3.z); }
float hash13(vec3 p3) { p3 = fract(p3 * 0.1031); p3 += dot(p3, p3.zyx + 31.32); return fract((p3.x + p3.y) * p3.z); }
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1, 0)), f.x), mix(hash12(i + vec2(0, 1)), hash12(i + vec2(1, 1)), f.x), f.y);
}
float fbm(vec2 p) { float a = 0.5, s = 0.0; for (int i = 0; i < 4; i++) { s += a * vnoise(p); p = p * 2.03 + 11.7; a *= 0.5; } return s; }
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
vec3 toLinear(vec3 c) { return pow(c, vec3(2.2)); }
// shadow map distortion: more resolution close to the player
vec3 distortShadow(vec3 p) { float f = length(p.xy) * 0.9 + 0.1; return vec3(p.xy / f, p.z * 0.25); }
float dayFactor(float sunAngle) { return smoothstep(-0.06, 0.2, sin(sunAngle * 6.2831853)); }
''')

# ------------------------------------------------------------------------------------------------ gbuffers
VSH = r'''
#version 120
%(defs)s
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
uniform mat4 gbufferModelView, gbufferModelViewInverse, shadowModelView, shadowProjection;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;
#ifdef TERRAIN
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;
#endif
varying vec2 texcoord, lmcoord;
varying vec4 color;
varying vec3 normal, viewPos, worldPos;
varying vec4 shadowClip;
varying float emissive;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = clamp(((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy * 16.0 - 0.5) / 15.0, 0.0, 1.0);
    color = gl_Color;
    normal = normalize(gl_NormalMatrix * gl_Normal);
    vec4 pp = gbufferModelViewInverse * (gl_ModelViewMatrix * gl_Vertex);
    emissive = 0.0;
#ifdef TERRAIN
    if (mc_Entity.x == 10001.0) emissive = 1.0;
    else if (mc_Entity.x == 10002.0) emissive = 2.0;
#ifdef WAVING_PLANTS
    if (mc_Entity.x == 10003.0 && gl_MultiTexCoord0.t < mc_midTexCoord.t) {
        vec3 wp = pp.xyz + cameraPosition;
        float t = frameTimeCounter;
        pp.x += (sin(t * 1.6 + wp.x * 0.8 + wp.z * 0.3) + 0.4 * sin(t * 3.7 + wp.z)) * 0.045;
        pp.z += (cos(t * 1.3 + wp.z * 0.7 + wp.x * 0.2)) * 0.04;
    }
#endif
#endif
    worldPos = pp.xyz + cameraPosition;
    viewPos = (gbufferModelView * pp).xyz;
    gl_Position = gl_ProjectionMatrix * vec4(viewPos, 1.0);
    vec3 worldNormal = mat3(gbufferModelViewInverse) * normal;
    shadowClip = shadowProjection * (shadowModelView * vec4(pp.xyz + worldNormal * 0.06, 1.0));
}
'''

FSH = r'''
#version 120
%(defs)s
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
uniform sampler2D texture;
uniform sampler2D shadowtex0;
uniform vec3 shadowLightPosition;
uniform float sunAngle, rainStrength, frameTimeCounter;
uniform int entityId;
uniform vec4 entityColor;
uniform int isEyeInWater;
varying vec2 texcoord, lmcoord;
varying vec4 color;
varying vec3 normal, viewPos, worldPos;
varying vec4 shadowClip;
varying float emissive;

float shadowAt() {
#ifdef SHADOWS
    vec3 sp = shadowClip.xyz / shadowClip.w;
    float f = length(sp.xy) * 0.9 + 0.1;
    sp = distortShadow(sp) * 0.5 + 0.5;
    if (any(lessThan(sp.xy, vec2(0.0))) || any(greaterThan(sp.xy, vec2(1.0)))) return 1.0;
    float bias = 0.00035 * f * f / 0.1;
    float ang = hash12(gl_FragCoord.xy) * 6.2831853;
    mat2 rot = mat2(cos(ang), -sin(ang), sin(ang), cos(ang));
    float r = SHADOW_SOFTNESS / 2048.0 * 1.6;
    float lit = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 2.39996;
        vec2 o = rot * vec2(cos(a), sin(a)) * r * sqrt((float(i) + 0.5) / 8.0);
        lit += step(sp.z - bias, texture2D(shadowtex0, sp.xy + o).r);
    }
    return lit / 8.0;
#else
    return 1.0;
#endif
}

float flicker() {
#ifdef CANDLE_FLICKER
    vec3 cell = floor(worldPos * 0.25);
    float t = frameTimeCounter * (7.0 + hash13(cell) * 5.0) + hash13(cell + 7.0) * 40.0;
    return 0.86 + 0.09 * sin(t) + 0.05 * sin(t * 2.7 + 1.3) - 0.06 * step(0.985, hash12(vec2(floor(frameTimeCounter * 9.0), cell.x + cell.z)));
#else
    return 1.0;
#endif
}

/* DRAWBUFFERS:0 */
void main() {
    vec4 tex = texture2D(texture, texcoord) * color;
#ifdef ENTITIES
    tex.rgb = mix(tex.rgb, entityColor.rgb, entityColor.a);
#endif
    if (tex.a < 0.02) discard;
    vec3 albedo = toLinear(tex.rgb);
#ifdef UNLIT
    gl_FragData[0] = vec4(albedo * 1.4, tex.a);
    return;
#endif
    float day = dayFactor(sunAngle);
    vec3 n = normalize(normal);
    vec3 L = normalize(shadowLightPosition);
    float ndl = max(dot(n, L), 0.0);
    float sky = lmcoord.y, blk = lmcoord.x;
    vec3 sunCol = vec3(1.0, 0.86, 0.68) * 2.3 * SUN_STRENGTH;
    vec3 moonCol = vec3(0.32, 0.42, 0.62) * 0.32 / NIGHT_DARKNESS;
    vec3 lightCol = mix(moonCol, sunCol, day) * (1.0 - rainStrength * 0.75);
    vec3 skyAmb = mix(vec3(0.020, 0.026, 0.045) / NIGHT_DARKNESS, vec3(0.30, 0.34, 0.42), day) * (1.0 - rainStrength * 0.4);
    vec3 ambient = skyAmb * sky * sky + vec3(0.0045, 0.0042, 0.004) / NIGHT_DARKNESS;
    float sh = ndl > 0.0 ? shadowAt() : 0.0;
    vec3 direct = lightCol * ndl * sh * smoothstep(0.0, 0.25, sky);
    float fl = flicker();
    vec3 candle = vec3(1.0, 0.52, 0.22) * pow(blk, 2.6) * 2.4 * BLOCKLIGHT_STRENGTH * fl;
    vec3 lit = albedo * (ambient + direct + candle);
#ifdef HAND
    lit = albedo * (ambient + candle * 1.1 + direct * 0.6 + 0.01);
#endif
#ifdef ENTITIES
#ifdef HATMAN_SHADOW_FORM
    if (entityId == 10010) {
        // the Hatman swallows light: almost no bounce, a cold rim at grazing angles, the hat stays visible
        float rim = pow(1.0 - abs(dot(n, normalize(-viewPos))), 3.0);
        float hatTint = step(0.35, luma(tex.rgb));
        lit = albedo * (ambient * 0.35 + direct * 0.5 + candle * (0.25 + 0.75 * hatTint)) + vec3(0.12, 0.16, 0.24) * rim * 0.08;
    }
#endif
#endif
#ifdef TERRAIN
    if (emissive > 0.5) {
        float l = luma(tex.rgb);
        float glow = smoothstep(0.45, 0.85, l);
        if (emissive > 1.5) {   // the old television: static that never stops
            float s = 0.65 + 0.35 * hash12(vec2(floor(frameTimeCounter * 24.0), floor(worldPos.x + worldPos.z)));
            lit = mix(lit, albedo * vec3(0.85, 0.95, 1.2) * 5.0 * s, glow) * 1.0;
        } else {
            lit = mix(lit, albedo * 6.0 * fl, glow * EMISSIVE_STRENGTH);
        }
    }
#endif
#ifdef WATER
    float fres = pow(1.0 - max(dot(n, normalize(-viewPos)), 0.0), 4.0);
    lit = mix(lit, skyAmb * 2.0 + lightCol * 0.05, fres * 0.6);
    tex.a = mix(tex.a, 0.92, fres);
#endif
#ifdef WEATHER
    lit = albedo * (skyAmb * 3.0 + 0.02);
    tex.a *= 0.55;
#endif
    gl_FragData[0] = vec4(lit, tex.a);
}
'''

PROGRAMS = {
    'gbuffers_basic': '#define BASIC', 'gbuffers_textured': '#define TEXTURED', 'gbuffers_textured_lit': '#define TEXTURED',
    'gbuffers_terrain': '#define TERRAIN', 'gbuffers_water': '#define WATER', 'gbuffers_entities': '#define ENTITIES',
    'gbuffers_block': '#define BLOCKENTITY', 'gbuffers_hand': '#define HAND', 'gbuffers_hand_water': '#define HAND',
    'gbuffers_weather': '#define WEATHER', 'gbuffers_clouds': '#define CLOUDS', 'gbuffers_spidereyes': '#define UNLIT',
    'gbuffers_beaconbeam': '#define UNLIT', 'gbuffers_armor_glint': '#define UNLIT', 'gbuffers_entities_glowing': '#define ENTITIES',
}
for name, d in PROGRAMS.items():
    w(name + '.vsh', VSH % {'defs': d})
    w(name + '.fsh', FSH % {'defs': d})

SKY_V = r'''
#version 120
varying vec4 color;
varying vec2 texcoord;
varying vec3 viewPos;
void main() {
    color = gl_Color;
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    gl_Position = ftransform();
}
'''
w('gbuffers_skybasic.vsh', SKY_V)
w('gbuffers_skybasic.fsh', r'''
#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
uniform float sunAngle, rainStrength;
uniform vec3 upPosition;
varying vec4 color;
varying vec2 texcoord;
varying vec3 viewPos;
/* DRAWBUFFERS:0 */
void main() {
    float day = dayFactor(sunAngle);
    float up = dot(normalize(viewPos), normalize(upPosition));
    vec3 zenith = mix(vec3(0.004, 0.006, 0.012) / NIGHT_DARKNESS, vec3(0.20, 0.30, 0.50), day);
    vec3 horizon = mix(vec3(0.016, 0.018, 0.026) / NIGHT_DARKNESS, vec3(0.52, 0.56, 0.62), day);
    vec3 sky = mix(horizon, zenith, smoothstep(-0.05, 0.6, up));
    sky = mix(sky, vec3(luma(sky)) * 0.7, rainStrength * 0.8);
    vec3 c = toLinear(color.rgb);
    // stars arrive as small bright quads with alpha; keep them faint and slightly warm
    if (color.a > 0.0 && luma(color.rgb) > 0.5 && day < 0.5) sky += c * 0.35 * (1.0 - rainStrength);
    gl_FragData[0] = vec4(sky, 1.0);
}
''')
w('gbuffers_skytextured.vsh', SKY_V)
w('gbuffers_skytextured.fsh', r'''
#version 120
#include "/lib/common.glsl"
uniform sampler2D texture;
uniform float rainStrength;
varying vec4 color;
varying vec2 texcoord;
varying vec3 viewPos;
/* DRAWBUFFERS:0 */
void main() {
    vec4 t = texture2D(texture, texcoord) * color;
    gl_FragData[0] = vec4(toLinear(t.rgb) * 2.2 * (1.0 - rainStrength * 0.85), t.a);
}
''')

# ------------------------------------------------------------------------------------------------ shadow
w('shadow.vsh', r'''
#version 120
#include "/lib/common.glsl"
varying vec2 texcoord;
varying vec4 color;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    color = gl_Color;
    gl_Position = ftransform();
    gl_Position.xyz = distortShadow(gl_Position.xyz);
}
''')
w('shadow.fsh', r'''
#version 120
uniform sampler2D texture;
varying vec2 texcoord;
varying vec4 color;
const int shadowMapResolution = 2048; // [1024 2048 3072 4096]
const float shadowDistance = 96.0; // [64.0 96.0 128.0 160.0]
const float sunPathRotation = -28.0; // [-40.0 -28.0 -15.0 0.0 15.0]
const bool shadowHardwareFiltering = false;
void main() {
    vec4 c = texture2D(texture, texcoord) * color;
    if (c.a < 0.1) discard;
    gl_FragData[0] = c;
}
''')

# ------------------------------------------------------------------------------------------------ composite: fog + ground mist
FULL_V = r'''
#version 120
varying vec2 texcoord;
void main() {
    gl_Position = ftransform();
    texcoord = gl_MultiTexCoord0.xy;
}
'''
w('composite.vsh', FULL_V)
w('composite.fsh', r'''
#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
/*
const int colortex0Format = RGBA16F;
*/
uniform sampler2D colortex0, depthtex0;
uniform mat4 gbufferProjectionInverse, gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float sunAngle, rainStrength, frameTimeCounter, far;
uniform ivec2 eyeBrightnessSmooth;
uniform int isEyeInWater;
varying vec2 texcoord;
/* DRAWBUFFERS:0 */
void main() {
    vec3 col = texture2D(colortex0, texcoord).rgb;
    float d = texture2D(depthtex0, texcoord).r;
    vec4 vp = gbufferProjectionInverse * vec4(vec3(texcoord, d) * 2.0 - 1.0, 1.0);
    vp /= vp.w;
    float dist = length(vp.xyz);
    vec3 wp = (gbufferModelViewInverse * vp).xyz;
    vec3 world = wp + cameraPosition;
    float day = dayFactor(sunAngle);
    float outdoors = clamp(float(eyeBrightnessSmooth.y) / 240.0, 0.0, 1.0);
    vec3 fogCol = mix(vec3(0.010, 0.012, 0.018) / NIGHT_DARKNESS, vec3(0.30, 0.32, 0.36), day);
    fogCol = mix(fogCol, vec3(luma(fogCol)) * 0.8, rainStrength);
    float dens = 0.010 * FOG_DENSITY * mix(2.2, 0.55, day) * (1.0 + rainStrength * 1.6) * mix(0.55, 1.0, outdoors);
    float fog = 1.0 - exp(-dist * dens);
    if (d >= 1.0) fog = mix(0.25, 0.9, (1.0 - day) * 0.6 + rainStrength * 0.4) * clamp(FOG_DENSITY, 0.0, 1.0);
    col = mix(col, fogCol, fog);
#ifdef GROUND_MIST
    if (d < 1.0 && isEyeInWater == 0) {
        float h = world.y - (cameraPosition.y - 1.4);
        float n = fbm(world.xz * 0.07 + vec2(frameTimeCounter * 0.035, frameTimeCounter * 0.02));
        float mist = exp(-max(h + 0.6, 0.0) * 0.55) * (1.0 - exp(-dist * 0.05)) * (0.45 + 0.85 * n);
        mist *= MIST_DENSITY * mix(1.0, 0.25, day) * outdoors;
        col = mix(col, fogCol * 1.6 + vec3(0.006, 0.007, 0.010), clamp(mist, 0.0, 0.85));
    }
#endif
    if (isEyeInWater == 1) col = mix(col, vec3(0.01, 0.03, 0.05), 1.0 - exp(-dist * 0.09));
    gl_FragData[0] = vec4(col, 1.0);
}
''')

# ------------------------------------------------------------------------------------------------ final: bloom, tonemap, grade, analog
w('final.vsh', FULL_V)
w('final.fsh', r'''
#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
uniform sampler2D colortex0;
const bool colortex0MipmapEnabled = true;
uniform float frameTimeCounter, viewWidth, viewHeight, sunAngle;
varying vec2 texcoord;

vec3 aces(vec3 x) { return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0); }

vec3 sampleScene(vec2 uv) { return texture2D(colortex0, uv).rgb; }

void main() {
    vec2 uv = texcoord;
    float t = frameTimeCounter;
#ifdef VHS_EFFECT
    float line = floor(uv.y * 240.0);
    float wob = (vnoise(vec2(line * 0.3, t * 6.0)) - 0.5) * 0.0016 * VHS_STRENGTH;
    float bandPos = fract(t * 0.07);
    float band = smoothstep(0.03, 0.0, abs(uv.y - bandPos)) * VHS_STRENGTH;
    uv.x += wob + band * (hash12(vec2(line, floor(t * 30.0))) - 0.5) * 0.02;
    uv.x += (1.0 - smoothstep(0.0, 0.035, uv.y)) * 0.01 * sin(t * 40.0 + uv.y * 300.0) * VHS_STRENGTH;   // head switching
    float ca = 0.0018 * VHS_STRENGTH;
    vec3 c = vec3(sampleScene(uv + vec2(ca, 0.0)).r, sampleScene(uv).g, sampleScene(uv - vec2(ca, 0.0)).b);
#else
    vec3 c = sampleScene(uv);
#endif
    // bloom from the mip chain (emissive surfaces are written above 1.0)
    vec3 bloom = vec3(0.0);
    float wsum = 0.0;
    for (int i = 2; i <= 7; i++) {
        float lod = float(i);
        vec3 b = texture2D(colortex0, uv, lod).rgb;
        float wgt = 1.0 / (1.0 + float(i - 2) * 0.6);
        bloom += max(b - vec3(0.55), 0.0) * wgt;
        wsum += wgt;
    }
    c += bloom / wsum * BLOOM_STRENGTH * 1.4;
    c *= EXPOSURE * 1.35;
    c = aces(c);
    c = pow(c, vec3(1.0 / 2.2));
    // grade
    float l = luma(c);
    c = mix(vec3(l), c, SATURATION);
    c = (c - 0.5) * CONTRAST + 0.5;
#if COLOR_GRADE == 1   // analog: cold shadows, sodium-warm highlights, lifted blacks
    c = mix(c * vec3(0.90, 0.97, 1.08), c * vec3(1.08, 1.0, 0.86), smoothstep(0.15, 0.75, l));
    c = c * 0.94 + vec3(0.022, 0.020, 0.026);
#elif COLOR_GRADE == 2 // sepia found footage
    c = mix(c, vec3(l) * vec3(1.10, 0.95, 0.74), 0.65) + vec3(0.02, 0.015, 0.01);
#elif COLOR_GRADE == 3 // bleach bypass
    c = mix(c, vec3(l) * 1.1, 0.45);
    c = (c - 0.5) * 1.15 + 0.5;
#endif
    // vignette
    vec2 q = texcoord - 0.5;
    c *= 1.0 - VIGNETTE * 0.55 * smoothstep(0.25, 0.85, length(q * vec2(viewWidth / viewHeight, 1.0) * 0.8));
#ifdef SCANLINES
    c *= 0.95 + 0.05 * sin(texcoord.y * viewHeight * 3.14159);
#endif
#ifdef VHS_EFFECT
    c += band * 0.12 * (hash12(texcoord * vec2(viewWidth, viewHeight) + t) - 0.3);
    c = mix(c, vec3(luma(c)), 0.06 * VHS_STRENGTH);
#endif
    // animated grain
    float g = hash12(texcoord * vec2(viewWidth, viewHeight) + fract(t * 13.7) * 100.0) - 0.5;
    c += g * 0.07 * FILM_GRAIN * (1.2 - luma(c));
#ifdef REC_OVERLAY
    vec2 px = texcoord * vec2(viewWidth, viewHeight);
    vec2 dotPos = vec2(viewWidth - 38.0, viewHeight - 34.0);
    float r = length(px - dotPos);
    float blink = step(0.5, fract(t * 0.8));
    c = mix(c, vec3(0.9, 0.08, 0.06), smoothstep(9.0, 7.5, r) * blink);
#endif
    gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
''')

# ------------------------------------------------------------------------------------------------ properties + lang
w('block.properties', '''
# emissive light sources (only their bright texels glow)
block.10001=minecraft:torch minecraft:wall_torch minecraft:soul_torch minecraft:soul_wall_torch minecraft:lantern minecraft:soul_lantern \\
 minecraft:glowstone minecraft:sea_lantern minecraft:jack_o_lantern minecraft:shroomlight minecraft:ochre_froglight minecraft:verdant_froglight \\
 minecraft:pearlescent_froglight minecraft:campfire:lit=true minecraft:soul_campfire:lit=true minecraft:fire minecraft:soul_fire minecraft:lava \\
 minecraft:redstone_lamp:lit=true minecraft:candle:lit=true minecraft:white_candle:lit=true minecraft:black_candle:lit=true minecraft:red_candle:lit=true \\
 minecraft:blue_candle:lit=true minecraft:magma_block minecraft:end_rod \\
 krolasyonhorror:candle_sconce:lit=true krolasyonhorror:ceiling_lamp:lit=true krolasyonhorror:bulb_fixture:lit=true
# the old television
block.10002=krolasyonhorror:crt_tv:lit=true
# waving foliage
block.10003=minecraft:grass minecraft:short_grass minecraft:tall_grass minecraft:fern minecraft:large_fern minecraft:dead_bush \\
 minecraft:sweet_berry_bush minecraft:dandelion minecraft:poppy minecraft:blue_orchid minecraft:allium minecraft:azure_bluet \\
 minecraft:oxeye_daisy minecraft:cornflower minecraft:lily_of_the_valley minecraft:wheat minecraft:carrots minecraft:potatoes
''')
w('entity.properties', 'entity.10010=krolasyonhorror:hatman\n')

OPTS = {
    'SHADOWS': ('Shadows', 'Gölgeler'), 'SHADOW_SOFTNESS': ('Shadow Softness', 'Gölge Yumuşaklığı'), 'SUN_STRENGTH': ('Sunlight', 'Güneş Işığı'),
    'NIGHT_DARKNESS': ('Night Darkness', 'Gece Karanlığı'), 'CANDLE_FLICKER': ('Candle Flicker', 'Mum Titremesi'),
    'BLOCKLIGHT_STRENGTH': ('Lamp & Candle Light', 'Lamba ve Mum Işığı'), 'EMISSIVE_STRENGTH': ('Glowing Lights', 'Parlayan Işıklar'),
    'WAVING_PLANTS': ('Waving Plants', 'Sallanan Bitkiler'), 'HATMAN_SHADOW_FORM': ('Hatman Shadow Form', 'Şapkalı Adam Gölge Formu'),
    'FOG_DENSITY': ('Fog Density', 'Sis Yoğunluğu'), 'GROUND_MIST': ('Ground Mist', 'Yer Pusu'), 'MIST_DENSITY': ('Mist Density', 'Pus Yoğunluğu'),
    'BLOOM_STRENGTH': ('Bloom', 'Parlama (Bloom)'), 'EXPOSURE': ('Exposure', 'Pozlama'), 'SATURATION': ('Saturation', 'Doygunluk'),
    'CONTRAST': ('Contrast', 'Kontrast'), 'COLOR_GRADE': ('Color Grade', 'Renk Ayarı'), 'VIGNETTE': ('Vignette', 'Vinyet'),
    'FILM_GRAIN': ('Film Grain', 'Film Greni'), 'VHS_EFFECT': ('VHS Tape', 'VHS Kaset Efekti'), 'VHS_STRENGTH': ('VHS Strength', 'VHS Gücü'),
    'SCANLINES': ('Scanlines', 'Tarama Çizgileri'), 'REC_OVERLAY': ('REC Light', 'Kayıt Işığı (REC)'),
    'shadowMapResolution': ('Shadow Resolution', 'Gölge Çözünürlüğü'), 'shadowDistance': ('Shadow Distance', 'Gölge Mesafesi'),
    'sunPathRotation': ('Sun Angle', 'Güneş Açısı'),
}
SCREENS = {'LIGHTING': ('Lighting', 'Işıklandırma'), 'ATMOSPHERE': ('Atmosphere', 'Atmosfer'), 'CAMERA': ('Camera & Color', 'Kamera ve Renk'),
           'ANALOG': ('Analog Tape', 'Analog Kaset')}
GRADES = (('Neutral', 'Nötr'), ('Analog', 'Analog'), ('Sepia Found Footage', 'Sepya Buluntu Kaset'), ('Bleach Bypass', 'Ağartma Baypası'))
PROFILES = {'LOW': ('Low', 'Düşük'), 'MEDIUM': ('Medium', 'Orta'), 'HIGH': ('High', 'Yüksek'), 'TAPE': ('Found Tape', 'Buluntu Kaset')}
for lang, i in (('en_us', 0), ('tr_tr', 1)):
    lines = [f'option.{k}={v[i]}' for k, v in OPTS.items()]
    lines += [f'screen.{k}={v[i]}' for k, v in SCREENS.items()]
    lines += [f'value.COLOR_GRADE.{n}={g[i]}' for n, g in enumerate(GRADES)]
    lines += [f'profile.{k}={v[i]}' for k, v in PROFILES.items()]
    lines += ['option.SHADOWS.comment=' + ('Soft shadows from the sun and the moon.' if i == 0 else 'Güneş ve aydan yumuşak gölgeler.'),
              'option.HATMAN_SHADOW_FORM.comment=' + ('The Hatman absorbs light and only shows a cold rim.' if i == 0 else 'Şapkalı Adam ışığı yutar, sadece soğuk bir kenar ışığı görünür.'),
              'option.VHS_EFFECT.comment=' + ('Tape wobble, colour bleed and tracking bands.' if i == 0 else 'Bant titremesi, renk kayması ve iz bantları.')]
    with open(os.path.join(P, 'lang', lang + '.lang'), 'w') as f:
        f.write('\n'.join(lines) + '\n')

w('shaders.properties', '''
# Hatman Analog - shader options layout
screen=<profile> <empty> [LIGHTING] [ATMOSPHERE] [CAMERA] [ANALOG]
screen.LIGHTING=SHADOWS shadowMapResolution SHADOW_SOFTNESS shadowDistance SUN_STRENGTH sunPathRotation NIGHT_DARKNESS BLOCKLIGHT_STRENGTH CANDLE_FLICKER EMISSIVE_STRENGTH HATMAN_SHADOW_FORM WAVING_PLANTS
screen.ATMOSPHERE=FOG_DENSITY GROUND_MIST MIST_DENSITY
screen.CAMERA=EXPOSURE BLOOM_STRENGTH SATURATION CONTRAST COLOR_GRADE VIGNETTE FILM_GRAIN
screen.ANALOG=VHS_EFFECT VHS_STRENGTH SCANLINES REC_OVERLAY
sliders=SHADOW_SOFTNESS SUN_STRENGTH NIGHT_DARKNESS BLOCKLIGHT_STRENGTH EMISSIVE_STRENGTH FOG_DENSITY MIST_DENSITY BLOOM_STRENGTH EXPOSURE SATURATION CONTRAST VIGNETTE FILM_GRAIN VHS_STRENGTH shadowMapResolution shadowDistance sunPathRotation
profile.LOW=!SHADOWS !GROUND_MIST BLOOM_STRENGTH=0.5 !VHS_EFFECT shadowMapResolution=1024
profile.MEDIUM=SHADOWS GROUND_MIST !VHS_EFFECT shadowMapResolution=2048 SHADOW_SOFTNESS=1.0
profile.HIGH=SHADOWS GROUND_MIST VHS_EFFECT shadowMapResolution=3072 SHADOW_SOFTNESS=1.5
profile.TAPE=SHADOWS GROUND_MIST VHS_EFFECT VHS_STRENGTH=1.5 COLOR_GRADE=2 FILM_GRAIN=1.0 VIGNETTE=1.5 REC_OVERLAY SCANLINES
oldLighting=false
separateAo=false
clouds=off
''')

with open(os.path.join(ROOT, 'HatmanAnalog', 'README.txt'), 'w') as f:
    f.write('Hatman Analog - shaderpack for the "Analog Horror: The Hatman" mod.\n'
            'Works with Oculus (Forge 1.20.1), Iris and OptiFine. Shader options are in English and Turkish.\n')

zp = os.path.join(ROOT, 'HatmanAnalog.zip')
with zipfile.ZipFile(zp, 'w', zipfile.ZIP_DEFLATED) as z:
    for base, _, files in os.walk(os.path.join(ROOT, 'HatmanAnalog')):
        for fn in files:
            full = os.path.join(base, fn)
            z.write(full, os.path.relpath(full, os.path.join(ROOT, 'HatmanAnalog')))
print('wrote', zp, sum(len(fs) for _, _, fs in os.walk(os.path.join(ROOT, 'HatmanAnalog'))), 'files')
