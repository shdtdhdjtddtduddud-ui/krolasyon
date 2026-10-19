// Gölge haritası bozulması ve örnekleme / shadow distortion & sampling

const float SHADOW_DISTORT = 0.85;

vec3 distortShadow(vec3 p) {
    float f = length(p.xy) * SHADOW_DISTORT + (1.0 - SHADOW_DISTORT);
    p.xy /= f;
    p.z *= 0.2;
    return p;
}

#if defined SHADOWS_ACTIVE && !defined SHADOW_PASS
uniform sampler2DShadow shadowtex0;
uniform sampler2DShadow shadowtex1;
uniform sampler2D shadowcolor0;

const vec2 POISSON[16] = vec2[16](
    vec2(-0.9465, -0.1484), vec2(-0.7431, 0.5353), vec2(-0.5863, -0.5174), vec2(-0.3730, 0.1035),
    vec2(-0.2154, 0.8634), vec2(-0.1491, -0.8954), vec2(-0.0306, -0.3253), vec2(0.0791, 0.4385),
    vec2(0.2155, -0.6418), vec2(0.3191, 0.0430), vec2(0.3984, 0.8263), vec2(0.4931, -0.3044),
    vec2(0.6152, 0.4398), vec2(0.7340, -0.6670), vec2(0.8687, 0.0721), vec2(0.9402, -0.3108)
);

// Oyuncu uzayındaki (kamera merkezli dünya) konum için renkli, yumuşak gölge
vec3 sampleShadow(vec3 playerPos, vec3 worldNormal, float NdotL, float dither) {
    float dist = length(playerPos);
    float fade = smoothstep(shadowDistance * 0.92, shadowDistance * 0.75, dist);
    if (fade <= 0.0) return vec3(1.0);

    // normal yönünde kaydırma (gölge akne önlemi)
    float bias = (0.035 + dist * 0.0018) * (2048.0 / float(shadowMapResolution));
    vec3 p = playerPos + worldNormal * bias * (1.5 - abs(NdotL) * 0.5);

    vec3 sv = (shadowModelView * vec4(p, 1.0)).xyz;
    vec3 sc = (shadowProjection * vec4(sv, 1.0)).xyz;
    sc = distortShadow(sc) * 0.5 + 0.5;
    sc.z -= 0.00008;

    float radius = SHADOW_SOFTNESS * 1.5 / float(shadowMapResolution);
    float ang = dither * TAU;
    mat2 rot = mat2(cos(ang), sin(ang), -sin(ang), cos(ang));

    float s0 = 0.0, s1 = 0.0;
    vec3 tint = vec3(0.0);
    for (int i = 0; i < SHADOW_SAMPLES; i++) {
        vec2 off = rot * POISSON[i * (16 / SHADOW_SAMPLES)] * radius;
        vec3 q = vec3(sc.xy + off, sc.z);
        s0 += texture(shadowtex0, q);
#ifdef COLORED_SHADOWS
        s1 += texture(shadowtex1, q);
#endif
    }
    s0 /= float(SHADOW_SAMPLES);
    vec3 shadow = vec3(s0);
#ifdef COLORED_SHADOWS
    s1 /= float(SHADOW_SAMPLES);
    if (s1 > s0 + 0.001) {
        vec4 sc0 = texture(shadowcolor0, sc.xy);
        vec3 glassCol = toLinear(sc0.rgb) * (1.0 - sc0.a * 0.4);
        shadow += glassCol * (s1 - s0);
    }
#endif
    return mix(vec3(1.0), shadow, fade);
}

// Hacimsel ışık için tek örnek (sert)
float shadowAt(vec3 playerPos) {
    vec3 sc = (shadowProjection * vec4((shadowModelView * vec4(playerPos, 1.0)).xyz, 1.0)).xyz;
    sc = distortShadow(sc) * 0.5 + 0.5;
    if (any(lessThan(sc.xy, vec2(0.0))) || any(greaterThan(sc.xy, vec2(1.0)))) return 1.0;
    return texture(shadowtex0, sc);
}
#endif
