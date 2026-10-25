#version 120
uniform sampler2D texture;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;

void main() {
    vec4 c = texture2D(texture, texcoord) * color;
    c.rgb = pow(c.rgb, vec3(2.2)) * (0.15 + lmcoord.y * 0.5);
    c.a *= 0.45;
/* DRAWBUFFERS:0 */
    gl_FragData[0] = c;
}
