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
    gl_FragData[0] = vec4(clamp(c, 0.0, 1.0), 1.0);
}
