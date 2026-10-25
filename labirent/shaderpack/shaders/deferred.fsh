#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"

uniform sampler2D colortex0;
uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform sampler2D depthtex0;
uniform sampler2D shadowtex0;
uniform sampler2D noisetex;
uniform mat4 gbufferProjection;
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
uniform float sunAngle;
uniform float rainStrength;
uniform float frameTimeCounter;
uniform float viewWidth;
uniform float viewHeight;
uniform float nightVision;
uniform float near;
uniform float far;
uniform int isEyeInWater;

varying vec2 texcoord;

#include "/lib/sky.glsl"
#include "/lib/shadow.glsl"

vec3 viewFromDepth(vec2 uv, float d) {
    vec4 p = gbufferProjectionInverse * vec4(vec3(uv, d) * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

float ssao(vec3 vp, vec3 n, float dither) {
    float occ = 0.0;
    const int N = 8;
    float radius = 0.55;
    for (int i = 0; i < N; i++) {
        float a = (float(i) + dither) * 2.39996;
        float r = (float(i) + 0.5) / float(N);
        vec3 t = normalize(cross(n, abs(n.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
        vec3 b = cross(n, t);
        vec3 dir = normalize(t * cos(a) * r + b * sin(a) * r + n * (1.0 - r * 0.6));
        vec3 sp = vp + dir * radius * (0.25 + r);
        vec4 cp = gbufferProjection * vec4(sp, 1.0);
        vec2 suv = cp.xy / cp.w * 0.5 + 0.5;
        if (suv.x < 0.0 || suv.y < 0.0 || suv.x > 1.0 || suv.y > 1.0) continue;
        float sd = texture2D(depthtex0, suv).r;
        vec3 sv = viewFromDepth(suv, sd);
        float diff = sv.z - sp.z;
        occ += step(0.03, diff) * smoothstep(1.6, 0.0, diff);
    }
    return 1.0 - occ / float(N);
}

void main() {
    vec4 raw = texture2D(colortex0, texcoord);
    float depth = texture2D(depthtex0, texcoord).r;
    vec3 vp = viewFromDepth(texcoord, depth);
    vec3 viewDir = normalize(vp);

    if (depth >= 1.0) {
        // sky: procedural gradient + stars/sun/moon written by the sky programs
        vec3 sky = skyGradient(viewDir) * mix(1.6, 0.85, greySky());
        sky += raw.rgb * (1.0 - greySky());
/* DRAWBUFFERS:0 */
        gl_FragData[0] = vec4(sky, 1.0);
        return;
    }

    vec4 nm = texture2D(colortex1, texcoord);
    vec4 ld = texture2D(colortex2, texcoord);
    vec3 n = normalize(nm.xyz * 2.0 - 1.0);
    float mat = floor(nm.a * 255.0 + 0.5);
    vec3 albedo = toLinear(raw.rgb);
    float emissive = ld.b;

    if (mat == MAT_CLOUD) {
        vec3 c = albedo * (sunColor() * 0.35 + ambientColor() * 1.2);
        gl_FragData[0] = vec4(c, 1.0);
        return;
    }

    vec3 lightDir = normalize(shadowLightPosition);
    float NdotL = dot(n, lightDir);
    bool foliage = mat == MAT_PLANT || mat == MAT_LEAVES;
    float diffuse = foliage ? clamp(NdotL * 0.5 + 0.5, 0.0, 1.0) : clamp(NdotL, 0.0, 1.0);

    float shadow = 1.0;
#ifdef SHADOWS
    vec3 worldRel = (gbufferModelViewInverse * vec4(vp, 1.0)).xyz;
    vec3 worldN = mat3(gbufferModelViewInverse) * n;
    if (diffuse > 0.0 && mat != MAT_HAND) {
        vec3 sp = shadowPos(worldRel, worldN);
        float dither = ign(gl_FragCoord.xy + mod(frameTimeCounter * 60.0, 64.0));
        shadow = sampleShadow(sp, dither * 6.2831, SHADOW_SOFTNESS);
    }
#endif
    // sky light from the lightmap fades the sun out where the sky is not visible (deep in corridors)
    float skyLight = ld.g;
    float skyVis = smoothstep(0.0, 0.95, skyLight);
    float sunVis = diffuse * shadow * mix(skyVis, 1.0, 0.25);

    float ao = 1.0;
#ifdef SSAO
    if (mat != MAT_HAND) {
        ao = ssao(vp, n, ign(gl_FragCoord.xy));
        ao = mix(1.0, ao, SSAO_STRENGTH * 0.85);
    }
#endif

    vec3 sunC = sunColor();
    vec3 ambC = ambientColor();
    float sss = foliage ? pow(max(dot(viewDir, lightDir), 0.0), 6.0) * shadow * 1.2 : 0.0;

    vec3 light = sunC * (sunVis + sss);
    light += ambC * (skyLight * skyLight * 1.1 + 0.035) * ao;
    // block light: warm, falls off quickly, flickers slightly
    float bl = ld.r;
    float flicker = 0.94 + 0.06 * noise2(vec2(frameTimeCounter * 8.0, 0.0));
    light += vec3(1.0, 0.62, 0.32) * pow(bl, 2.4) * 1.3 * flicker * ao;
    light += vec3(0.05) * nightVision;

    vec3 col = albedo * light;
    if (mat == MAT_EMISSIVE) col += albedo * emissive * 1.6;
    if (emissive > 0.0 && mat != MAT_EMISSIVE) col += toLinear(raw.rgb) * emissive * 3.0;
    // wet sheen in the rain
    col *= 1.0 - rainStrength * 0.12 * skyVis;

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(col, 1.0);
}
