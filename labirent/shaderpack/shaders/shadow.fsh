#version 120
uniform sampler2D tex;
varying vec2 texcoord;
varying vec4 color;

void main() {
    vec4 c = texture2D(tex, texcoord) * color;
    if (c.a < 0.1) discard;
    gl_FragData[0] = c;
}
