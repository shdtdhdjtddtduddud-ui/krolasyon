// Gölge haritası geçişi / shadow map pass
#define SHADOW_PASS

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/shadow.glsl"

uniform mat4 shadowModelViewInverse;

#ifdef VSH

in vec4 mc_Entity;
in vec4 mc_midTexCoord;

out vec2 texcoord;
out vec4 glcolor;
flat out float matId;

#include "/lib/wave.glsl"

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
    matId = mc_Entity.x;

    vec4 sv = gl_ModelViewMatrix * gl_Vertex;
    vec3 ppos = (shadowModelViewInverse * sv).xyz;
    bool isTop = gl_MultiTexCoord0.t < mc_midTexCoord.t;
    vec3 worldPos = applyWaving(ppos + cameraPosition, matId, isTop);
    sv = shadowModelView * vec4(worldPos - cameraPosition, 1.0);

    vec4 clip = gl_ProjectionMatrix * sv;
    clip.xyz = distortShadow(clip.xyz);
    gl_Position = clip;
}

#else

uniform sampler2D gtexture;

in vec2 texcoord;
in vec4 glcolor;
flat in float matId;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 shadowColor;

void main() {
    // Su gölge atmaz: suyun altı aydınlık kalır, kostikler görünür
    if (floor(matId + 0.5) == 10004.0) discard;
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.1) discard;
    shadowColor = c;
}

#endif
