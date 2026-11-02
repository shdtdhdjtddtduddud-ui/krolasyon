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
