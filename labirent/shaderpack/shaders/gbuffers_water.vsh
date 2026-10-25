#version 120
attribute vec4 mc_Entity;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;
varying vec3 normal;
varying vec3 viewPos;
varying vec3 worldPos;
varying float blockId;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = clamp((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy, 0.0, 1.0);
    color = gl_Color;
    normal = normalize(gl_NormalMatrix * gl_Normal);
    blockId = mc_Entity.x;
    vec4 view = gl_ModelViewMatrix * gl_Vertex;
    viewPos = view.xyz;
    worldPos = (gbufferModelViewInverse * view).xyz + cameraPosition;
    gl_Position = gl_ProjectionMatrix * view;
}
