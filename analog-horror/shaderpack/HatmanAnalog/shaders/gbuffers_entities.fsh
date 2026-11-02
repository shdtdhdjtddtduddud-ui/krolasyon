#version 120
#define ENTITIES
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
