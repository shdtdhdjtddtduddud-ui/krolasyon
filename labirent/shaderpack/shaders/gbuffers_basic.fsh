#version 120
#include "/lib/settings.glsl"
varying vec2 lmcoord;
varying vec4 color;
varying vec3 normal;

void main() {
    if (color.a < 0.05) discard;
/* DRAWBUFFERS:012 */
    gl_FragData[0] = color;
    gl_FragData[1] = vec4(normalize(normal) * 0.5 + 0.5, MAT_DEFAULT / 255.0);
    gl_FragData[2] = vec4(lmcoord, 0.0, 1.0);
}
