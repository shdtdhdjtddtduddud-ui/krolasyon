#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
// volumetric light shafts + height fog (thick, low haze between the maze walls) + underwater fog

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform sampler2D shadowtex0;
uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform vec3 sunPosition;
uniform vec3 moonPosition;
uniform vec3 upPosition;
uniform vec3 shadowLightPosition;
uniform vec3 fogColor;
uniform vec3 skyColor;
uniform vec3 cameraPosition;
uniform float rainStrength;
uniform float frameTimeCounter;
uniform float blindness;
uniform float darknessFactor;
uniform int isEyeInWater;
uniform ivec2 eyeBrightnessSmooth;

varying vec2 texcoord;

#include "/lib/sky.glsl"

float shadowAt(vec3 worldRel) {
    vec4 sp = shadowProjection * (shadowModelView * vec4(worldRel, 1.0));
    vec3 p = vec3(distort(sp.xy), sp.z * 0.2) * 0.5 + 0.5;
    if (p.x <= 0.0 || p.y <= 0.0 || p.x >= 1.0 || p.y >= 1.0) return 1.0;
    return step(p.z - 0.0006, texture2D(shadowtex0, p.xy).r);
}

float density(float y) {
    float ground = 65.0;
    float haze = MAZE_HAZE * 0.0075 * exp(-max(y - ground, 0.0) / 8.0);
    float base = 0.0011;
    float d = (base + haze) * FOG_DENSITY;
    d *= 1.0 + rainStrength * 1.5 + greySky() * 0.8;
    return d;
}

void main() {
    vec3 col = texture2D(colortex0, texcoord).rgb;
    float depth = texture2D(depthtex0, texcoord).r;
    vec4 vp4 = gbufferProjectionInverse * vec4(vec3(texcoord, depth) * 2.0 - 1.0, 1.0);
    vec3 vp = vp4.xyz / vp4.w;
    vec3 viewDir = normalize(vp);
    float dist = depth >= 1.0 ? 160.0 : length(vp);
    vec3 endRel = (gbufferModelViewInverse * vec4(viewDir * dist, 0.0)).xyz;

    if (isEyeInWater == 1) {
        float t = exp(-dist * 0.09);
        vec3 water = vec3(0.02, 0.09, 0.11) * (ambientColor() * 2.0 + sunColor() * 0.3);
        col = mix(water, col * vec3(0.6, 0.85, 0.9), t);
    } else if (isEyeInWater == 2) {
        col = mix(vec3(1.2, 0.35, 0.05), col, exp(-dist * 0.6));
    } else {
        vec3 lightDir = normalize(shadowLightPosition);
        float mu = dot(viewDir, lightDir);
        float g = 0.62;
        float phase = (1.0 - g * g) / (4.0 * 3.14159 * pow(1.0 + g * g - 2.0 * g * mu, 1.5)) * 3.0 + 0.12;
        vec3 sunC = sunColor() * VL_STRENGTH;
        vec3 ambC = ambientColor() * 0.55;
        float skyExposure = eyeBrightnessSmooth.y / 240.0;
        float transmittance = 1.0;
        vec3 scatter = vec3(0.0);
        float dither = ign(gl_FragCoord.xy + mod(frameTimeCounter * 61.0, 64.0));
        float stepLen = dist / float(VL_STEPS);
        for (int i = 0; i < VL_STEPS; i++) {
            float f = (float(i) + dither) / float(VL_STEPS);
            vec3 p = endRel * f;
            float d = density(p.y + cameraPosition.y);
            float lit = 1.0;
#ifdef VOLUMETRIC_LIGHT
            lit = shadowAt(p);
#endif
            vec3 s = (sunC * phase * lit * mix(0.03, 1.0, skyExposure) + ambC * mix(0.25, 1.0, skyExposure)) * d * stepLen;
            scatter += s * transmittance;
            transmittance *= exp(-d * stepLen);
        }
        col = col * transmittance + scatter;
    }
    if (blindness > 0.0 || darknessFactor > 0.0) col *= exp(-dist * 0.25 * max(blindness, darknessFactor * 0.6));

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(col, 1.0);
}
