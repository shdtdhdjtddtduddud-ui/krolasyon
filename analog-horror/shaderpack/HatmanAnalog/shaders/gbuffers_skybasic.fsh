#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"
uniform float sunAngle, rainStrength;
uniform vec3 upPosition;
varying vec4 color;
varying vec2 texcoord;
varying vec3 viewPos;
/* DRAWBUFFERS:0 */
void main() {
    float day = dayFactor(sunAngle);
    float up = dot(normalize(viewPos), normalize(upPosition));
    vec3 zenith = mix(vec3(0.004, 0.006, 0.012) / NIGHT_DARKNESS, vec3(0.20, 0.30, 0.50), day);
    vec3 horizon = mix(vec3(0.016, 0.018, 0.026) / NIGHT_DARKNESS, vec3(0.52, 0.56, 0.62), day);
    vec3 sky = mix(horizon, zenith, smoothstep(-0.05, 0.6, up));
    sky = mix(sky, vec3(luma(sky)) * 0.7, rainStrength * 0.8);
    vec3 c = toLinear(color.rgb);
    // stars arrive as small bright quads with alpha; keep them faint and slightly warm
    if (color.a > 0.0 && luma(color.rgb) > 0.5 && day < 0.5) sky += c * 0.35 * (1.0 - rainStrength);
    gl_FragData[0] = vec4(sky, 1.0);
}
