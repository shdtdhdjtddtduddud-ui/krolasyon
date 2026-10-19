// Tüm gbuffers programları için ortak kaynak. Program türü PROG_* ile seçilir.

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/atmosphere.glsl"

#if defined PROG_TERRAIN || defined PROG_WATER || defined PROG_ENTITIES || defined PROG_HAND || defined PROG_BLOCK
    #define LIT
#endif

// =====================================================================
#ifdef VSH
// =====================================================================

in vec4 mc_Entity;
in vec4 mc_midTexCoord;

out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 viewPos;
out vec3 viewNormal;
out vec3 playerPos;
flat out float matId;

#include "/lib/wave.glsl"

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = clamp(((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy - 1.0 / 32.0) * (16.0 / 15.0), 0.0, 1.0);
    glcolor = gl_Color;
    matId = 0.0;
#if defined PROG_TERRAIN || defined PROG_WATER
    matId = mc_Entity.x;
#endif

    vec4 vpos = gl_ModelViewMatrix * gl_Vertex;
    vec3 ppos = viewToPlayer(vpos.xyz);

#if defined PROG_TERRAIN
    bool isTop = gl_MultiTexCoord0.t < mc_midTexCoord.t;
    vec3 worldPos = applyWaving(ppos + cameraPosition, matId, isTop);
    if (worldPos != ppos + cameraPosition) {
        ppos = worldPos - cameraPosition;
        vpos.xyz = (gbufferModelView * vec4(ppos, 1.0)).xyz;
    }
#endif

    viewPos = vpos.xyz;
    playerPos = ppos;
    viewNormal = normalize(gl_NormalMatrix * gl_Normal);
    gl_Position = gl_ProjectionMatrix * vpos;
}

// =====================================================================
#else // FSH
// =====================================================================

uniform sampler2D gtexture;
uniform vec4 entityColor;
uniform ivec2 atlasSize;

in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 viewPos;
in vec3 viewNormal;
in vec3 playerPos;
flat in float matId;

#if defined LIT
    #include "/lib/shadow.glsl"
    #include "/lib/lighting.glsl"
#endif
#if defined PROG_WATER
    #include "/lib/sky.glsl"
#endif

#if defined LIT
/* RENDERTARGETS: 0,1,2 */
layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outNormal;
layout(location = 2) out vec4 outData;
#else
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
#endif

// --------------------------- Su dalgaları ---------------------------
#if defined PROG_WATER
float waterHeight(vec2 p) {
    float t = frameTimeCounter;
    float h = 0.0;
    h += noise2(p * vec2(0.45, 0.60) + vec2(t * 0.35, t * 0.22)) * 0.50;
    h += noise2(p * vec2(1.10, 0.90) - vec2(t * 0.28, -t * 0.40)) * 0.28;
    h += noise2(p * 2.70 + vec2(t * 0.75, t * 0.55)) * 0.12;
    h += noise2(p * 6.00 - vec2(t * 1.10, t * 0.20)) * 0.04;
    return h;
}
vec3 waterNormal(vec2 p) {
    const float e = 0.06;
    float h0 = waterHeight(p);
    float hx = waterHeight(p + vec2(e, 0.0));
    float hz = waterHeight(p + vec2(0.0, e));
    float k = 0.55 * WAVE_HEIGHT * (1.0 + rainStrength * 0.6);
    return normalize(vec3(-(hx - h0) / e * k, 1.0, -(hz - h0) / e * k));
}
#endif

void main() {
    initAtmosphere();

    // ---------------------------------------------------------------
#if defined PROG_SKY
    discard;
    return;
    // ---------------------------------------------------------------
#elif defined PROG_CLOUDS
    #if defined CUSTOM_CLOUDS && !defined WORLD_NETHER && !defined WORLD_END
    discard;
    return;
    #else
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.05) discard;
    outColor = vec4(toLinear(c.rgb) * (ambientColor * 1.2 + lightColor * 0.4), c.a);
    #endif
    // ---------------------------------------------------------------
#elif defined PROG_BASIC
    vec3 c = toLinear(glcolor.rgb);
    outColor = vec4(c * (ambientColor * lmcoord.y * lmcoord.y + vec3(0.6, 0.45, 0.3) * lmcoord.x + 0.05), glcolor.a);
    // ---------------------------------------------------------------
#elif defined PROG_EMISSIVE
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.01) discard;
    outColor = vec4(toLinear(c.rgb) * 2.5, c.a);
    // ---------------------------------------------------------------
#elif defined PROG_DAMAGE
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.01) discard;
    outColor = c;
    // ---------------------------------------------------------------
#elif defined PROG_WEATHER
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.01) discard;
    vec3 l = ambientColor * 1.6 + lightColor * 0.15 + vec3(1.0, 0.62, 0.32) * pow(lmcoord.x, 2.6);
    outColor = vec4(toLinear(c.rgb) * l, c.a * 0.55);
    // ---------------------------------------------------------------
#elif defined PROG_TEXTURED
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.1) discard;
    vec3 alb = toLinear(c.rgb);
    float skyMask = smoothstep(0.03, 0.35, lmcoord.y);
    vec3 l = ambientColor * lmcoord.y * lmcoord.y + lightColor * 0.55 * skyMask
           + vec3(1.0, 0.62, 0.32) * pow(lmcoord.x, 2.6) * 1.8 + vec3(0.02, 0.016, 0.034) + nightVision * 0.3;
    outColor = vec4(alb * l, c.a);
    // ---------------------------------------------------------------
#elif defined LIT
    vec4 tex = texture(gtexture, texcoord);
    vec4 albedo;
    float ao = 1.0;
    #if defined PROG_TERRAIN || defined PROG_WATER
        // separateAo: köşe renginin alfa kanalı vanilya AO değeridir
        albedo = vec4(tex.rgb * glcolor.rgb, tex.a);
        ao = glcolor.a;
    #else
        albedo = tex * glcolor;
    #endif
    #if defined PROG_ENTITIES || defined PROG_HAND
        albedo.rgb = mix(albedo.rgb, entityColor.rgb, entityColor.a);
    #endif

    #if defined PROG_WATER
        if (albedo.a < 0.01) discard;
    #else
        if (albedo.a < 0.1) discard;
    #endif

    vec3 alb = toLinear(albedo.rgb);
    vec3 nView = normalize(viewNormal);
    if (!gl_FrontFacing) nView = -nView;
    vec3 nWorld = mat3(gbufferModelViewInverse) * nView;
    vec3 worldPos = playerPos + cameraPosition;
    float dither = ign(gl_FragCoord.xy);
    float t = frameTimeCounter;

    float mat = MAT_TERRAIN;
    float subsurface = 0.0;
    float emission = 0.0;
    vec3 emissiveAdd = vec3(0.0);

    #if defined PROG_ENTITIES
        mat = MAT_ENTITY;
    #elif defined PROG_HAND
        mat = MAT_HAND;
    #endif

    // ----- Materyal özellikleri -----
    #if defined PROG_TERRAIN
    float id = floor(matId + 0.5);
    if (id == 10001.0 || id == 10002.0 || id == 10003.0 || id == 10011.0) {
        subsurface = 1.0;
        mat = MAT_PLANT;
    }
    if (id == 10005.0) {
        // ışık kaynakları: parlak pikseller ışır (meşale alevi, fener camı…)
        float l = luma(albedo.rgb);
        emission = smoothstep(0.45, 0.85, l) * 3.5 + smoothstep(0.75, 1.0, l) * 2.0;
        mat = MAT_EMISSIVE;
    }
    if (id == 10009.0) {
        emission = 2.8;
        mat = MAT_EMISSIVE;
    }

    #ifdef BIOLUMINESCENCE
        #if defined WORLD_NETHER
        float glowNight = 0.6;
        #elif defined WORLD_END
        float glowNight = 1.0;
        #else
        float glowNight = mix(0.0, 1.0, nightF) * (1.0 - rainStrength * 0.5);
        #endif
        glowNight *= BIOLUM_STRENGTH;

        vec3 blockPos = floor(worldPos - nWorld * 0.05);
        vec2 texel = floor(texcoord * vec2(atlasSize));

        if (glowNight > 0.001) {
            // Yapraklar: parlayan damar ağı + göz kırpan ışık noktaları
            if (id == 10002.0) {
                float n = noise3(worldPos * 0.9 + vec3(0.0, t * 0.04, 0.0));
                float vein = pow(1.0 - abs(n * 2.0 - 1.0), 16.0);
                float h = hash13(vec3(texel, 0.0) + blockPos * 17.0);
                float sparkle = step(0.965, h) * pow(0.5 + 0.5 * sin(t * (1.5 + h * 3.0) + h * 80.0), 3.0);
                vec3 gc = mix(BIOLUM_A, BIOLUM_B, step(0.985, h));
                emissiveAdd += gc * (vein * 0.35 * albedo.a + sparkle * 2.5) * glowNight;
            }
            // Çiçekler: taç yaprakları kendi renginde ışıldar
            if (id == 10001.0 || id == 10003.0) {
                float mx = max(albedo.r, max(albedo.g, albedo.b));
                float sat = (mx - min(albedo.r, min(albedo.g, albedo.b))) / max(mx, 1e-3);
                float petal = smoothstep(0.35, 0.65, sat) * step(albedo.g, max(albedo.r, albedo.b) + 0.02);
                float h = hash13(vec3(texel, 1.0) + blockPos * 13.0);
                float speck = step(0.985, h) * (0.5 + 0.5 * sin(t * 2.0 + h * 50.0));
                emissiveAdd += alb * petal * 3.0 * glowNight + BIOLUM_A * speck * 1.2 * glowNight;
            }
            // Mantarlar
            if (id == 10011.0) {
                emissiveAdd += alb * 2.2 * glowNight + BIOLUM_B * 0.08 * glowNight;
            }
            #ifdef LOG_VEINS
            // Kütükler: yukarı akan ışık damarları (yalnız yan yüzlerde)
            if (id == 10013.0 && abs(nWorld.y) < 0.5) {
                vec3 q = vec3(worldPos.x * 2.2, worldPos.y * 0.22, worldPos.z * 2.2);
                float n = noise3(q);
                float vein = pow(1.0 - abs(n * 2.0 - 1.0), 22.0);
                float pulse = 0.55 + 0.45 * sin(worldPos.y * 1.6 - t * 1.8 + n * 6.0);
                emissiveAdd += mix(BIOLUM_A, BIOLUM_B, n) * vein * pulse * 1.6 * glowNight;
            }
            #endif
        }
    #endif

    #ifdef CRYSTAL_SPARKLE
        if (id == 10012.0) {
            vec3 bp = floor(worldPos - nWorld * 0.05);
            float h = hash13(vec3(floor(texcoord * vec2(atlasSize)), 2.0) + bp * 7.0);
            float tw = pow(0.5 + 0.5 * sin(t * (2.0 + h * 4.0) + h * 60.0), 6.0);
            emissiveAdd += mix(vec3(1.0, 0.85, 1.0), BIOLUM_A, h) * step(0.93, h) * tw * 3.0;
            emission = max(emission, 0.15);
        }
    #endif
    #endif // PROG_TERRAIN

    float alpha = albedo.a;
    float direct = 0.0;
    vec3 color;

    #if defined PROG_WATER
    if (floor(matId + 0.5) == 10004.0) {
        mat = MAT_WATER;
        #ifdef WATER_WAVES
        if (nWorld.y > 0.5) {
            nWorld = waterNormal(worldPos.xz);
            nView = mat3(gbufferModelView) * nWorld;
        }
        #endif
        vec3 tint = toLinear(glcolor.rgb);
        vec3 waterCol = mix(tint * 0.6, vec3(0.02, 0.32, 0.42), 0.6) * (0.5 + luma(tex.rgb));
        vec3 irr = computeLighting(vec3(1.0), playerPos, nWorld, lmcoord, 1.0, 0.0, 0.0, dither, direct);
        color = waterCol * irr * 0.6;

        // güneş/ay parıltısı
        vec3 V = normalize(playerPos);
        vec3 H = normalize(lightDirW - V);
        float spec = pow(saturate(dot(nWorld, H)), 350.0) * 10.0 + pow(saturate(dot(nWorld, H)), 60.0) * 0.4;
        color += lightColor * spec * direct;

        #ifdef WATER_GLOW
        // gece: biyolüminesan plankton
            #if defined WORLD_NETHER
            float wg = 0.0;
            #else
            float wg = nightF * BIOLUM_STRENGTH;
            #endif
        if (wg > 0.001) {
            vec2 p = worldPos.xz * 2.2 + vec2(t * 0.15, t * 0.08);
            vec2 cell = floor(p);
            vec2 o = hash22(cell);
            float dd = length(fract(p) - (0.2 + o * 0.6));
            float pl = smoothstep(0.09, 0.0, dd) * step(0.72, hash12(cell + 3.3)) * (0.5 + 0.5 * sin(t * 2.5 + o.x * 40.0));
            float swirl = pow(noise2(worldPos.xz * 0.35 + t * 0.05), 3.0);
            color += (BIOLUM_A * pl * 3.0 + BIOLUM_A * swirl * 0.12) * wg;
        }
        #endif
        alpha = WATER_ALPHA;
        color /= alpha; // karışımda kararmayı telafi et
    } else
    #endif
    {
        #if defined PROG_WATER
        mat = MAT_GLASS;
        #endif
        color = computeLighting(alb, playerPos, nWorld, lmcoord, ao, subsurface, emission, dither, direct);
        color += emissiveAdd;
    }

    outColor = vec4(color, alpha);
    outNormal = vec4(encodeNormal(nView), (mat + 0.5) / 255.0, 1.0);
    outData = vec4(lmcoord.y, direct, saturate(emission * 0.3 + luma(emissiveAdd)), 1.0);
    // ---------------------------------------------------------------
#endif
}

#endif
