#version 120
#include "/lib/settings.glsl"
// the procedural sky is drawn in deferred; here we only keep the stars
uniform float rainStrength;
varying vec4 color;
varying vec3 viewPos;

void main() {
    bool star = false;
#ifdef STARS
    star = color.r == color.g && color.g == color.b && color.r > 0.0 && color.a > 0.0 && color.r < 0.99;
#endif
/* DRAWBUFFERS:0 */
    gl_FragData[0] = star ? vec4(vec3(color.r) * 2.0 * (1.0 - rainStrength), 1.0) : vec4(0.0, 0.0, 0.0, 1.0);
}
