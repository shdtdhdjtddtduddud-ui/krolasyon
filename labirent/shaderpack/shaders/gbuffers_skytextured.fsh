#version 120
// sun & moon sprites, made brighter so they bloom
uniform sampler2D texture;
varying vec2 texcoord;
varying vec4 color;

void main() {
    vec4 c = texture2D(texture, texcoord) * color;
/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(pow(c.rgb, vec3(2.2)) * 6.0, c.a);
}
