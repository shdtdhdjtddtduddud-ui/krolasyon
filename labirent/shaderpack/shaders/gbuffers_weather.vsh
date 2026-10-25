#version 120
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = clamp((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy, 0.0, 1.0);
    color = gl_Color;
    gl_Position = ftransform();
}
