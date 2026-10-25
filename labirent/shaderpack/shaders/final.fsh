#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
// exposure, bloom, ACES tonemap, cinematic grade (cool shadows / warm highlights), vignette, grain, chromatic fringe

uniform sampler2D colortex0;
uniform sampler2D colortex3;
uniform ivec2 eyeBrightnessSmooth;
uniform float frameTimeCounter;
uniform float viewWidth;
uniform float viewHeight;
varying vec2 texcoord;

vec3 aces(vec3 x) {
    const float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

vec3 sampleScene(vec2 uv) {
    vec3 c;
    vec2 dir = uv - 0.5;
    float k = CHROMATIC_ABERRATION * 0.004 * dot(dir, dir) * 4.0;
    c.r = texture2D(colortex0, uv - dir * k).r;
    c.g = texture2D(colortex0, uv).g;
    c.b = texture2D(colortex0, uv + dir * k).b;
    return c;
}

void main() {
    vec3 col = sampleScene(texcoord);
#ifdef BLOOM
    col += texture2D(colortex3, texcoord).rgb * 0.22 * BLOOM_STRENGTH;
#endif
    float sky = eyeBrightnessSmooth.y / 240.0;
    float exposure = mix(1.45, 1.0, sky) * EXPOSURE;
    col *= exposure;
    col = aces(col);
    // grade
    float l = luma(col);
    vec3 shadowsTint = vec3(0.92, 1.0, 1.04), highTint = vec3(1.05, 1.0, 0.92);
    vec3 graded = col * mix(shadowsTint, highTint, smoothstep(0.15, 0.75, l));
    graded = mix(vec3(luma(graded)), graded, SATURATION);
    graded = (graded - 0.5) * 1.06 + 0.5;
    col = mix(col, graded, CINEMATIC_GRADE);
    col = toSRGB(col);
    // vignette
    vec2 v = texcoord - 0.5;
    col *= 1.0 - VIGNETTE * smoothstep(0.25, 0.9, dot(v, v) * 2.2);
    // film grain
    float g = hash12(gl_FragCoord.xy + fract(frameTimeCounter) * 1000.0) - 0.5;
    col += g * FILM_GRAIN * 0.06;
    gl_FragData[0] = vec4(col, 1.0);
}
