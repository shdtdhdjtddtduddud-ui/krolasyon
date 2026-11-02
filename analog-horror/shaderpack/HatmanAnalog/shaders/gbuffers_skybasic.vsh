#version 120
varying vec4 color;
varying vec2 texcoord;
varying vec3 viewPos;
void main() {
    color = gl_Color;
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    gl_Position = ftransform();
}
