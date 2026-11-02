#version 120
#define ENTITIES
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
