#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
// translucent pass: water gets waves, fresnel sky reflection and a sun glint; glass & ice are just lit

uniform sampler2D texture;
uniform sampler2D shadowtex0;
uniform sampler2D depthtex1;
uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjectionInverse;
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
uniform float viewWidth;
uniform float viewHeight;
uniform float near;
uniform float far;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;
varying vec3 normal;
varying vec3 viewPos;
varying vec3 worldPos;
varying float blockId;

#include "/lib/sky.glsl"
#include "/lib/shadow.glsl"

float waveHeight(vec2 p) {
    float t = frameTimeCounter * 0.9;
    return noise2(p * 0.9 + vec2(t * 0.6, t * 0.3)) * 0.5 + noise2(p * 2.3 - vec2(t * 0.9, -t * 0.4)) * 0.3
           + noise2(p * 5.1 + vec2(-t, t * 1.2)) * 0.2;
}

float linDepth(float d) { return 2.0 * near * far / (far + near - (d * 2.0 - 1.0) * (far - near)); }

void main() {
    vec4 tex = texture2D(texture, texcoord) * color;
    vec3 n = normalize(normal);
    vec3 viewDir = normalize(viewPos);
    vec3 worldRel = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz;
    vec3 sunC = sunColor(), ambC = ambientColor();
    float shadow = 1.0;
#ifdef SHADOWS
    shadow = sampleShadow(shadowPos(worldRel, mat3(gbufferModelViewInverse) * n), ign(gl_FragCoord.xy) * 6.28, SHADOW_SOFTNESS);
#endif
    vec3 lightDir = normalize(shadowLightPosition);

    if (blockId == 10006.0) {
        // waves -> normal
        vec2 p = worldPos.xz;
        float e = 0.08;
        float h0 = waveHeight(p), hx = waveHeight(p + vec2(e, 0.0)), hz = waveHeight(p + vec2(0.0, e));
        vec3 wn = normalize(vec3((h0 - hx) * 0.6, 1.0, (h0 - hz) * 0.6));
        vec3 vn = normalize(mat3(gl_ModelViewMatrix) * wn);
        if (dot(n, normalize(upPosition)) < 0.5) vn = n;
        float fres = 0.02 + 0.98 * pow(1.0 - max(dot(-viewDir, vn), 0.0), 5.0);
        vec3 refl = reflect(viewDir, vn);
        vec3 sky = skyGradient(refl) * 1.4 * lmcoord.y;
        float spec = pow(max(dot(refl, lightDir), 0.0), 220.0) * shadow * 12.0;
        // thickness from the opaque depth behind
        float behind = linDepth(texture2D(depthtex1, gl_FragCoord.xy / vec2(viewWidth, viewHeight)).r) - linDepth(gl_FragCoord.z);
        float thick = clamp(behind / 6.0, 0.0, 1.0);
        vec3 deep = vec3(0.02, 0.10, 0.12) * (ambC * 1.5 + sunC * 0.15 * shadow);
        vec3 col = mix(deep, sky, fres) + sunC * spec;
        float a = mix(0.35, 0.92, max(thick, fres));
#ifndef WATER_REFLECTIONS
        col = deep + sunC * spec;
#endif
/* DRAWBUFFERS:0 */
        gl_FragData[0] = vec4(col, a);
        return;
    }
    vec3 albedo = toLinear(tex.rgb);
    vec3 light = sunC * max(dot(n, lightDir), 0.0) * shadow + ambC * lmcoord.y + vec3(1.0, 0.62, 0.32) * pow(lmcoord.x, 3.0) * 2.0;
    gl_FragData[0] = vec4(albedo * light, tex.a);
}
