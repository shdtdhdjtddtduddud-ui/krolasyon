#version 120
// emissive layers (Griever eyes, LEDs, beetle eyes): additive and flagged so they skip lighting and feed bloom
uniform sampler2D texture;
varying vec2 texcoord;
varying vec4 color;

void main() {
    vec4 c = texture2D(texture, texcoord) * color;
/* DRAWBUFFERS:02 */
    gl_FragData[0] = vec4(c.rgb * 2.5, c.a);
    gl_FragData[1] = vec4(0.0, 0.0, c.a * 2.0, 0.0);
}
