#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
// bloom: bright pass + wide horizontal blur from the mip chain
uniform sampler2D colortex0;
uniform float viewWidth;
uniform float viewHeight;
varying vec2 texcoord;

/*
const bool colortex0MipmapEnabled = true;
*/

vec3 bright(vec2 uv, float lod) {
    vec3 c = texture2D(colortex0, uv, lod).rgb;
    float l = luma(c);
    return c * smoothstep(0.85, 2.2, l);
}

void main() {
    vec3 b = vec3(0.0);
    float w = 0.0;
    float px = 1.0 / viewWidth;
    for (int i = -6; i <= 6; i++) {
        float k = exp(-float(i * i) / 18.0);
        b += bright(texcoord + vec2(float(i) * px * 3.0, 0.0), 2.0) * k;
        b += bright(texcoord + vec2(float(i) * px * 9.0, 0.0), 4.0) * k * 0.6;
        w += k;
    }
/* DRAWBUFFERS:3 */
    gl_FragData[0] = vec4(b / w, 1.0);
}
