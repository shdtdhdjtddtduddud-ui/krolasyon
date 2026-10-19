// Composite1: mipmap tabanlı çok katmanlı bloom / multi-level mipmap bloom

#include "/lib/settings.glsl"
#include "/lib/common.glsl"

#ifdef VSH

out vec2 uv;
void main() {
    gl_Position = ftransform();
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
}

#else

const bool colortex0MipmapEnabled = true;

uniform sampler2D colortex0;

in vec2 uv;

/* RENDERTARGETS: 3 */
layout(location = 0) out vec4 outBloom;

vec3 blurLod(float lod) {
    vec2 px = exp2(lod) / vec2(viewWidth, viewHeight);
    vec3 s = vec3(0.0);
    s += textureLod(colortex0, uv, lod).rgb * 4.0;
    s += textureLod(colortex0, uv + vec2( px.x, 0.0), lod).rgb * 2.0;
    s += textureLod(colortex0, uv + vec2(-px.x, 0.0), lod).rgb * 2.0;
    s += textureLod(colortex0, uv + vec2(0.0,  px.y), lod).rgb * 2.0;
    s += textureLod(colortex0, uv + vec2(0.0, -px.y), lod).rgb * 2.0;
    s += textureLod(colortex0, uv + px, lod).rgb;
    s += textureLod(colortex0, uv - px, lod).rgb;
    s += textureLod(colortex0, uv + vec2(px.x, -px.y), lod).rgb;
    s += textureLod(colortex0, uv + vec2(-px.x, px.y), lod).rgb;
    return s / 16.0;
}

void main() {
#ifdef BLOOM
    vec3 b = vec3(0.0);
    float wsum = 0.0;
    for (int i = 1; i <= 7; i++) {
        float w = pow(0.85, float(i - 1));
        b += blurLod(float(i)) * w;
        wsum += w;
    }
    b /= wsum;
    // aşırı parlak noktaların ateşböceği gibi titreşmesini sınırla
    b = b / (1.0 + luma(b) * 0.02);
    outBloom = vec4(max(b, 0.0), 1.0);
#else
    outBloom = vec4(0.0);
#endif
}

#endif
