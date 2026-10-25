// ---------------------------------------------------------------- shared vertex stage for opaque geometry
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;
attribute vec4 at_midBlock;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;
uniform float rainStrength;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;
varying vec3 normal;
varying vec3 viewPos;
varying float blockId;

#include "/lib/waving.glsl"

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = clamp((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy, 0.0, 1.0);
    color = gl_Color;
    normal = normalize(gl_NormalMatrix * gl_Normal);
#ifdef TERRAIN
    blockId = mc_Entity.x;
    vec4 view = gl_ModelViewMatrix * gl_Vertex;
    vec4 world = gbufferModelViewInverse * view;
    world.xyz = wave(world.xyz, blockId, gl_MultiTexCoord0.t < mc_midTexCoord.t, at_midBlock.xyz);
    view = gbufferModelView * world;
    viewPos = view.xyz;
    gl_Position = gl_ProjectionMatrix * view;
#else
    blockId = 0.0;
    vec4 view = gl_ModelViewMatrix * gl_Vertex;
    viewPos = view.xyz;
    gl_Position = gl_ProjectionMatrix * view;
#endif
}
