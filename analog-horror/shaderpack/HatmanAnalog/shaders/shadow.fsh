#version 120
uniform sampler2D texture;
varying vec2 texcoord;
varying vec4 color;
const int shadowMapResolution = 2048; // [1024 2048 3072 4096]
const float shadowDistance = 96.0; // [64.0 96.0 128.0 160.0]
const float sunPathRotation = -28.0; // [-40.0 -28.0 -15.0 0.0 15.0]
const bool shadowHardwareFiltering = false;
void main() {
    vec4 c = texture2D(texture, texcoord) * color;
    if (c.a < 0.1) discard;
    gl_FragData[0] = c;
}
