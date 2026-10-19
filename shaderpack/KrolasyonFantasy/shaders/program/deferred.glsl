// Deferred: opak sahneye ekran-uzayı ortam kapatma (SSAO) + fantastik gökyüzü

#include "/lib/settings.glsl"
#include "/lib/buffers.glsl"
#include "/lib/common.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH

out vec2 uv;
void main() {
    gl_Position = ftransform();
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
}

#else

#include "/lib/sky.glsl"

uniform sampler2D colortex0;
uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform sampler2D depthtex0;

in vec2 uv;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

#ifdef SSAO
float computeSSAO(vec3 viewPos, vec3 n, float dither) {
    const int N = 8;
    float radius = 0.6;
    float occ = 0.0;
    vec3 t1 = normalize(cross(n, abs(n.y) < 0.9 ? vec3(0, 1, 0) : vec3(1, 0, 0)));
    vec3 t2 = cross(n, t1);
    for (int i = 0; i < N; i++) {
        float fi = (float(i) + dither) / float(N);
        float a = fi * TAU * 2.618 + dither * TAU;
        float r = sqrt(fi);
        float z = sqrt(1.0 - r * r);
        vec3 dir = t1 * cos(a) * r + t2 * sin(a) * r + n * z;
        vec3 s = viewPos + dir * radius * (0.25 + 0.75 * fi);
        vec3 sp = viewToScreen(s);
        if (sp.x < 0.0 || sp.x > 1.0 || sp.y < 0.0 || sp.y > 1.0) continue;
        float sd = texture(depthtex0, sp.xy).r;
        if (sd < 0.56) continue;
        float sceneZ = screenToView(sp.xy, sd).z;
        float diff = sceneZ - s.z;
        occ += step(0.03, diff) * smoothstep(radius * 2.0, 0.0, diff);
    }
    return 1.0 - occ / float(N);
}
#endif

void main() {
    initAtmosphere();
    vec3 color = texture(colortex0, uv).rgb;
    float depth = texture(depthtex0, uv).r;
    vec3 viewPos = screenToView(uv, depth);

    if (depth >= 1.0) {
        vec3 dir = normalize(mat3(gbufferModelViewInverse) * viewPos);
        color = fullSky(dir, true);
    }
#ifdef SSAO
    else if (depth > 0.56) {
        vec4 nd = texture(colortex1, uv);
        float mat = floor(nd.b * 255.0);
        if (mat > 0.5 && mat != MAT_EMISSIVE) {
            vec3 n = decodeNormal(nd.rg);
            float ao = computeSSAO(viewPos, n, ign(gl_FragCoord.xy));
            float direct = texture(colortex2, uv).g;
            float w = (1.0 - direct * 0.85) * SSAO_STRENGTH;
            color *= mix(1.0, ao, saturate(w) * 0.85);
        }
    }
#endif

    outColor = vec4(color, 1.0);
}

#endif
