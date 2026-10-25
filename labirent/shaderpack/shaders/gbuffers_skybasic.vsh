#version 120
varying vec4 color;
varying vec3 viewPos;

void main() {
    color = gl_Color;
    viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    gl_Position = ftransform();
}
