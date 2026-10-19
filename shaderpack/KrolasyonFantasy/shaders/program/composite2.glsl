// Composite2: bloom birleştirme, pozlama, ton eşleme ve fantastik renk derecelendirme

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH

out vec2 uv;
void main() {
    gl_Position = ftransform();
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
}

#else

uniform sampler2D colortex0;
uniform sampler2D colortex3;

in vec2 uv;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

// ACES (Narkowicz) + omuz yumuşatma
vec3 tonemap(vec3 x) {
    const float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    return saturate((x * (a * x + b)) / (x * (c * x + d) + e));
}

void main() {
    initAtmosphere();
    vec3 color = texture(colortex0, uv).rgb;

#ifdef BLOOM
    vec3 bloom = texture(colortex3, uv).rgb;
    float bloomAmt = 0.10 * BLOOM_STRENGTH * (1.0 + nightF * 0.6);
    if (isEyeInWater == 1) bloomAmt *= 2.0;
    color = mix(color, bloom, bloomAmt) + bloom * 0.02 * BLOOM_STRENGTH;
#endif

    // otomatik pozlama (göz parlaklığı + günün saati)
    float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
    float eyeBlock = float(eyeBrightnessSmooth.x) / 240.0;
#if defined WORLD_NETHER || defined WORLD_END
    float exposure = 1.6;
#else
    float dayExp = 0.85;
    float nightExp = 1.5 * NIGHT_BRIGHTNESS;
    float outdoor = mix(nightExp, dayExp, smoothstep(-0.1, 0.25, sunH));
    float cave = mix(2.8, 1.6, eyeBlock);
    float exposure = mix(cave, outdoor, eyeSky);
#endif
    color *= exposure * EXPOSURE;

    color = tonemap(color);

    // ---- fantastik renk derecelendirme ----
    float l = luma(color);
    // gölgeler mor/turkuaz, parlak tonlar sıcak pembe-altın
    vec3 shadowTint = mix(vec3(0.95, 0.90, 1.12), BIOLUM_A * 0.3 + vec3(0.75, 0.85, 0.95), nightF * 0.6);
    vec3 highTint = mix(vec3(1.06, 1.00, 0.94), vec3(1.08, 0.96, 1.02), twiF);
    vec3 graded = color * mix(shadowTint, highTint, smoothstep(0.1, 0.8, l));
    color = mix(color, graded, FANTASY_GRADE);

    // gece: hafif mavi kayma (Purkinje etkisi)
    color = mix(color, vec3(luma(color)) * vec3(0.75, 0.85, 1.15), nightF * 0.30 * (1.0 - smoothstep(0.0, 0.5, luma(color))));

    // canlılık (doygunluğu düşük renkleri daha çok artır)
    float mx = max(color.r, max(color.g, color.b));
    float mn = min(color.r, min(color.g, color.b));
    float sat = mx - mn;
    float vib = (SATURATION - 1.0) * 1.5 * (1.0 - sat);
    l = luma(color);
    color = mix(vec3(l), color, SATURATION + vib * 0.5);

#ifdef VIGNETTE
    vec2 vc = uv - 0.5;
    float v = 1.0 - dot(vc, vc) * VIGNETTE_STRENGTH * 1.6;
    color *= saturate(v);
#endif

    color = pow(saturate(color), vec3(1.0 / 2.2));
    // bantlanmayı önlemek için hafif titreşim
    color += (ign(gl_FragCoord.xy) - 0.5) / 255.0;
    outColor = vec4(color, 1.0);
}

#endif
