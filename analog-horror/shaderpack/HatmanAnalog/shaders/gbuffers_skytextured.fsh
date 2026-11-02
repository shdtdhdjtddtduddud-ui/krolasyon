#version 120
#include "/lib/common.glsl"
uniform sampler2D texture;
uniform float rainStrength;
varying vec4 color;
varying vec2 texcoord;
varying vec3 viewPos;
/* DRAWBUFFERS:0 */
void main() {
    vec4 t = texture2D(texture, texcoord) * color;
    gl_FragData[0] = vec4(toLinear(t.rgb) * 2.2 * (1.0 - rainStrength * 0.85), t.a);
}
