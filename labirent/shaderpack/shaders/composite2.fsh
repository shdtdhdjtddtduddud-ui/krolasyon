#version 120
// bloom: vertical blur
uniform sampler2D colortex3;
uniform float viewHeight;
varying vec2 texcoord;

void main() {
    vec3 b = vec3(0.0);
    float w = 0.0;
    float py = 1.0 / viewHeight;
    for (int i = -8; i <= 8; i++) {
        float k = exp(-float(i * i) / 28.0);
        b += texture2D(colortex3, texcoord + vec2(0.0, float(i) * py * 4.0)).rgb * k;
        w += k;
    }
/* DRAWBUFFERS:3 */
    gl_FragData[0] = vec4(b / w, 1.0);
}
