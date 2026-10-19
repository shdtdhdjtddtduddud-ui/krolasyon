// Final: FXAA + uyarlanabilir keskinleştirme

#include "/lib/settings.glsl"

uniform float viewWidth;
uniform float viewHeight;

#ifdef VSH

out vec2 uv;
void main() {
    gl_Position = ftransform();
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
}

#else

uniform sampler2D colortex0;

in vec2 uv;

layout(location = 0) out vec4 fragColor;

float lumaG(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

vec3 fxaa(vec2 p, vec2 px) {
    vec3 rgbM = texture(colortex0, p).rgb;
    vec3 rgbNW = texture(colortex0, p + vec2(-1.0, -1.0) * px).rgb;
    vec3 rgbNE = texture(colortex0, p + vec2( 1.0, -1.0) * px).rgb;
    vec3 rgbSW = texture(colortex0, p + vec2(-1.0,  1.0) * px).rgb;
    vec3 rgbSE = texture(colortex0, p + vec2( 1.0,  1.0) * px).rgb;
    float lM = lumaG(rgbM), lNW = lumaG(rgbNW), lNE = lumaG(rgbNE), lSW = lumaG(rgbSW), lSE = lumaG(rgbSE);
    float lMin = min(lM, min(min(lNW, lNE), min(lSW, lSE)));
    float lMax = max(lM, max(max(lNW, lNE), max(lSW, lSE)));
    if (lMax - lMin < max(0.0312, lMax * 0.125)) return rgbM;

    vec2 dir = vec2(-((lNW + lNE) - (lSW + lSE)), ((lNW + lSW) - (lNE + lSE)));
    float reduce = max((lNW + lNE + lSW + lSE) * 0.25 * 0.125, 1.0 / 128.0);
    float rcp = 1.0 / (min(abs(dir.x), abs(dir.y)) + reduce);
    dir = clamp(dir * rcp, vec2(-8.0), vec2(8.0)) * px;

    vec3 a = 0.5 * (texture(colortex0, p + dir * (1.0 / 3.0 - 0.5)).rgb + texture(colortex0, p + dir * (2.0 / 3.0 - 0.5)).rgb);
    vec3 b = a * 0.5 + 0.25 * (texture(colortex0, p + dir * -0.5).rgb + texture(colortex0, p + dir * 0.5).rgb);
    float lB = lumaG(b);
    return (lB < lMin || lB > lMax) ? a : b;
}

void main() {
    vec2 px = 1.0 / vec2(viewWidth, viewHeight);
#ifdef FXAA
    vec3 color = fxaa(uv, px);
#else
    vec3 color = texture(colortex0, uv).rgb;
#endif

    if (SHARPEN > 0.0) {
    vec3 n = texture(colortex0, uv + vec2(px.x, 0.0)).rgb + texture(colortex0, uv - vec2(px.x, 0.0)).rgb
           + texture(colortex0, uv + vec2(0.0, px.y)).rgb + texture(colortex0, uv - vec2(0.0, px.y)).rgb;
    vec3 detail = color - n * 0.25;
    color += clamp(detail * float(SHARPEN), -0.04, 0.04);
    }

    fragColor = vec4(color, 1.0);
}

#endif
