// Composite: yansımalar, su derinliği, kostikler, sis, hacimsel ışık, ateş böcekleri

#include "/lib/settings.glsl"
#include "/lib/buffers.glsl"
#include "/lib/common.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH

out vec2 uv;
void main() {
    gl_Position = ftransform();
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
}

#else

#include "/lib/shadow.glsl"
#include "/lib/sky.glsl"

uniform sampler2D colortex0;
uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform sampler2D depthtex0;
uniform sampler2D depthtex1;

in vec2 uv;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

// ------------------------------------------------------------------
// Ekran uzayı yansımaları
// ------------------------------------------------------------------
vec4 screenSpaceReflection(vec3 viewPos, vec3 nView, float dither) {
    vec3 rd = normalize(reflect(normalize(viewPos), nView));
    float len = 0.35;
    vec3 pos = viewPos + rd * len * dither;
    vec3 prev = pos;
    for (int i = 0; i < 28; i++) {
        prev = pos;
        pos += rd * len;
        len *= 1.28;
        vec3 sp = viewToScreen(pos);
        if (sp.x < 0.0 || sp.x > 1.0 || sp.y < 0.0 || sp.y > 1.0 || sp.z < 0.0 || sp.z > 1.0) break;
        float d = texture(depthtex1, sp.xy).r;
        if (d < 0.56) continue;
        float sceneZ = screenToView(sp.xy, d).z;
        float diff = sceneZ - pos.z;
        if (diff > 0.0 && diff < len * 2.0 + 0.3) {
            // ikili arama ile incelt
            vec3 a = prev, b = pos;
            for (int j = 0; j < 5; j++) {
                vec3 m = (a + b) * 0.5;
                vec3 ms = viewToScreen(m);
                float md = texture(depthtex1, ms.xy).r;
                if (screenToView(ms.xy, md).z > m.z) b = m; else a = m;
            }
            vec3 hs = viewToScreen(b);
            if (texture(depthtex1, hs.xy).r >= 1.0) return vec4(0.0);
            vec2 edge = smoothstep(vec2(0.0), vec2(0.08), hs.xy) * smoothstep(vec2(1.0), vec2(0.92), hs.xy);
            return vec4(texture(colortex0, hs.xy).rgb, edge.x * edge.y);
        }
    }
    return vec4(0.0);
}

// ------------------------------------------------------------------
// Ateş böcekleri (gece), büyülü toz (gündüz), köz (Nether)
// ------------------------------------------------------------------
vec3 floatingLights(vec3 rd, float maxDist) {
    float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
    float t = frameTimeCounter;
#if defined WORLD_NETHER
    float flyAmt = 0.0;
    float dustAmt = 0.0;
    float emberAmt = 1.0;
#else
    float flyAmt = 0.0, dustAmt = 0.0;
    float emberAmt = 0.0;
    #ifdef FIREFLIES
    flyAmt = nightF * (1.0 - rainStrength) * mix(0.25, 1.0, eyeSky) * FIREFLY_AMOUNT;
    #endif
    #ifdef MAGIC_DUST
    dustAmt = dayF * (1.0 - rainStrength) * eyeSky * 0.6;
    #endif
    #if defined WORLD_END
    flyAmt = 0.8 * FIREFLY_AMOUNT;
    #endif
#endif
    if (flyAmt + dustAmt + emberAmt < 0.01) return vec3(0.0);

    const float S = 2.5; // hücre boyu (blok)
    vec3 ro = (cameraPosition + vec3(0.0, 0.0, 0.0)) / S;
    vec3 cell = floor(ro);
    vec3 stepv = sign(rd);
    vec3 invd = 1.0 / max(abs(rd), vec3(1e-5));
    vec3 tMax = (stepv * (cell - ro) + max(stepv, 0.0)) * invd;
    float limit = min(maxDist, 40.0) / S;

    vec3 sum = vec3(0.0);
    for (int i = 0; i < 24; i++) {
        vec3 h = hash33(cell);
        float present = flyAmt > 0.0 ? step(h.x, 0.11 * min(flyAmt, 3.0)) : (emberAmt > 0.0 ? step(h.x, 0.25) : step(h.x, 0.12 * dustAmt));
        // oyuncunun çevresindeki yükseklik bandıyla sınırla (gökyüzüne saçılmasın)
        float relY = (cell.y + 0.5) * S - cameraPosition.y;
        #if !defined WORLD_NETHER && !defined WORLD_END
        present *= step(-5.0, relY) * step(relY, flyAmt > 0.0 ? 6.0 : 10.0);
        #endif
        if (present > 0.5) {
            vec3 h2 = hash33(cell + 17.31);
            vec3 fp = cell + 0.25 + 0.5 * h2;
            fp += vec3(sin(t * (0.3 + h.y * 0.4) + h.z * 20.0), sin(t * (0.5 + h.z * 0.3) + h.y * 13.0), cos(t * (0.35 + h2.x * 0.3) + h.x * 9.0)) * 0.18;
            float life = 1.0;
            if (emberAmt > 0.0) {
                float ph = fract(h.y + t * (0.05 + h.z * 0.05));
                fp.y = cell.y + ph;
                life = sin(ph * PI);
            }
            vec3 rel = fp - ro;
            float tc = dot(rel, rd);
            if (tc > 0.05 && tc < limit) {
                float d = length(rel - rd * tc) * S; // blok cinsinden ışından uzaklık
                float dist = tc * S;
                float fade = smoothstep(limit * S, limit * S * 0.6, dist);
                if (flyAmt > 0.0) {
                    float blink = pow(0.5 + 0.5 * sin(t * (0.8 + h.y * 1.6) + h.z * 31.0), 3.0);
                    float r = 0.03 + dist * 0.0015;
                    float g = 1.0 / (1.0 + pow2(d / r)) * 2.2;
                    vec3 c = mix(vec3(0.55, 1.0, 0.30), BIOLUM_A, h2.z);
                    sum += c * g * blink * fade * flyAmt;
                } else if (emberAmt > 0.0) {
                    float r = 0.02 + dist * 0.001;
                    sum += vec3(1.0, 0.45, 0.12) * (1.0 / (1.0 + pow2(d / r))) * 1.5 * life * fade;
                } else {
                    float r = 0.012 + dist * 0.001;
                    float tw = 0.5 + 0.5 * sin(t * 2.0 + h.z * 40.0);
                    sum += mix(vec3(1.0, 0.75, 0.9), vec3(1.0, 0.95, 0.7), h2.y) * (1.0 / (1.0 + pow2(d / r))) * 0.6 * tw * fade * dustAmt;
                }
            }
        }
        // sonraki hücre (DDA)
        if (tMax.x < tMax.y && tMax.x < tMax.z) { cell.x += stepv.x; if (tMax.x > limit) break; tMax.x += invd.x; }
        else if (tMax.y < tMax.z)               { cell.y += stepv.y; if (tMax.y > limit) break; tMax.y += invd.y; }
        else                                     { cell.z += stepv.z; if (tMax.z > limit) break; tMax.z += invd.z; }
    }
    return sum;
}

void main() {
    initAtmosphere();
    vec3 color = texture(colortex0, uv).rgb;
    float depth0 = texture(depthtex0, uv).r;
    float depth1 = texture(depthtex1, uv).r;
    vec4 nd = texture(colortex1, uv);
    vec4 data = texture(colortex2, uv);
    float mat = floor(nd.b * 255.0);
    float dither = ign(gl_FragCoord.xy);

    vec3 viewPos = screenToView(uv, depth0);
    vec3 playerPos = viewToPlayer(viewPos);
    vec3 dirW = normalize(mat3(gbufferModelViewInverse) * viewPos);
    float dist = length(viewPos);
    bool isSky = depth0 >= 1.0;
    bool isHand = depth0 < 0.56;
    float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;

    // ---------------- Su / cam yansıması ----------------
    if ((mat == MAT_WATER || mat == MAT_GLASS) && !isHand) {
        vec3 nView = decodeNormal(nd.rg);
        vec3 V = normalize(viewPos);
        float cosT = saturate(dot(-V, nView));
        float fres = 0.02 + 0.98 * pow(1.0 - cosT, 5.0);
        if (mat == MAT_GLASS) fres *= 0.6;

        // su derinliği: sis + kostikler
        if (mat == MAT_WATER && isEyeInWater == 0) {
            vec3 viewPos1 = screenToView(uv, depth1);
            float wd = depth1 >= 1.0 ? 64.0 : length(viewPos1 - viewPos);
            float skyL = data.r;
            vec3 waterFog = vec3(0.015, 0.16, 0.20) * (luma(ambientColor) * (skyL * skyL + 0.05) + luma(lightColor) * 0.06 * skyL);
            float absorb = 1.0 - exp(-wd * 0.11);
        #ifdef CAUSTICS
            if (depth1 < 1.0) {
                vec3 wp1 = viewToPlayer(viewPos1) + cameraPosition;
                vec2 cp = wp1.xz + wp1.y * 0.3;
                float tt = frameTimeCounter;
                float c1 = pow(1.0 - abs(noise2(cp * 1.3 + vec2(tt * 0.35, tt * 0.2)) * 2.0 - 1.0), 5.0);
                float c2 = pow(1.0 - abs(noise2(cp * 1.7 - vec2(tt * 0.25, -tt * 0.3)) * 2.0 - 1.0), 5.0);
                float caustic = c1 * c2 * 2.5;
                color += lightColor * caustic * 0.12 * exp(-wd * 0.3) * skyL * (1.0 - absorb);
            }
        #endif
            color = mix(color, waterFog, absorb * 0.85);
        }

    #ifdef WATER_REFLECTIONS
        vec3 rdW = reflect(dirW, mat3(gbufferModelViewInverse) * nView);
        vec3 skyRefl = rdW.y > 0.0 ? fullSky(rdW, true) : skyGradient(rdW) * 0.6;
        skyRefl *= smoothstep(0.2, 0.9, data.r);
        if (isEyeInWater == 1) skyRefl = vec3(0.01, 0.08, 0.1) * luma(ambientColor);
        vec4 ssr = screenSpaceReflection(viewPos, nView, dither);
        vec3 refl = mix(skyRefl, ssr.rgb, ssr.a);
        color = mix(color, refl, fres);
    #endif
    }

    // ---------------- Sis ----------------
    if (isEyeInWater == 0 && !isHand) {
        if (!isSky) {
            float worldY = playerPos.y + cameraPosition.y;
        #if defined WORLD_NETHER
            float dens = 0.018 * FOG_DENSITY;
            vec3 fogCol = skyGradient(dirW) * 0.8;
        #elif defined WORLD_END
            float dens = 0.006 * FOG_DENSITY;
            vec3 fogCol = skyGradient(dirW) * 1.4;
        #else
            float dens = (0.0012 + 0.0038 * twiF + 0.0015 * nightF + 0.010 * rainStrength) * FOG_DENSITY;
            dens *= mix(0.35, 1.0, exp(-max(worldY - 63.0, 0.0) * 0.02));
            vec3 fogCol = skyGradient(dirW);
            fogCol = mix(vec3(0.03, 0.02, 0.05) + ambientColor * 0.1, fogCol, eyeSky);
        #endif
            float amount = 1.0 - exp(-dist * dens);
            color = mix(color, fogCol, amount);

        #ifdef BORDER_FOG
            float b = smoothstep(far * 0.62, far * 0.95, length(playerPos.xz));
            if (b > 0.0) {
            #if defined WORLD_NETHER
                color = mix(color, fogCol, b);
            #else
                color = mix(color, mix(fogCol, fullSky(dirW, false), eyeSky), b);
            #endif
            }
        #endif
        }
    }

    // ---------------- Hacimsel ışık ----------------
#if defined VOLUMETRIC_LIGHT && defined SHADOWS_ACTIVE
    if (isEyeInWater != 2 && !isHand) {
        float rayLen = isSky ? 96.0 : min(dist, 96.0);
        rayLen = min(rayLen, shadowDistance * 0.9);
        vec3 rayDir = dirW;
        float acc = 0.0;
        for (int i = 0; i < VL_STEPS; i++) {
            float f = (float(i) + dither) / float(VL_STEPS);
            acc += shadowAt(rayDir * rayLen * f * f);
        }
        acc /= float(VL_STEPS);
        float cosT = dot(dirW, lightDirW);
        float g = 0.65;
        float mie = (1.0 - g * g) / pow(1.0 + g * g - 2.0 * g * cosT, 1.5) / (4.0 * PI);
        float phase = mie * 0.8 + 0.08;
    #if defined WORLD_END
        float vlDens = 0.25;
    #else
        float vlDens = (0.10 + 0.55 * twiF + 0.15 * nightF) * (1.0 - rainStrength * 0.6);
    #endif
        vec3 vl = lightColor * acc * phase * vlDens * (1.0 - exp(-rayLen * 0.02)) * VL_STRENGTH;
        if (isEyeInWater == 1) vl *= vec3(0.3, 1.2, 1.3) * 2.0;
        else vl *= mix(0.2, 1.0, eyeSky);
        color += vl;
    }
#endif

    // ---------------- Uçuşan ışıklar ----------------
    if (!isHand && isEyeInWater == 0) {
        color += floatingLights(dirW, isSky ? 64.0 : dist);
    }

    // ---------------- Hava sisi (Krolasyon Gerçekçi Hava: tipi, kum fırtınası, yoğun sis, sağanak) ----------------
    // Mod vanilya sis mesafesini kısaltır; burada o mesafe ve rengiyle paketin kendi sisinin üstüne örtülür.
#ifdef WEATHER_FOG
    if (isEyeInWater == 0 && !isHand && fogEnd > 0.5 && fogEnd < far * 0.8 && blindness < 0.01 && darknessFactor < 0.01) {
        float d = isSky ? fogEnd * 3.0 : dist;
        // yakın plan net kalsın, sis uzaklıkla üstel artsın (paketin kendi sisiyle üst üste binmesin diye biraz yumuşak)
        float start = max(fogStart, 0.0) + (fogEnd - max(fogStart, 0.0)) * 0.12;
        float wf = 1.0 - exp(-max(d - start, 0.0) / max(fogEnd - start, 1.0) * 2.2);
        float strength = saturate(1.0 - fogEnd / (far * 0.8));
        vec3 wcol = toLinear(fogColor) * 1.15 * WEATHER_FOG_BRIGHTNESS * (0.35 + 0.65 * eyeSky);
        color = mix(color, wcol, wf * mix(0.5, 0.92, strength));
    }
#endif

    // ---------------- Su altı / lav / toz kar ----------------
    if (isEyeInWater == 1) {
        vec3 uw = vec3(0.01, 0.13, 0.17) * (luma(ambientColor) * (eyeSky + 0.15) + 0.02) + nightVision * 0.02;
        color *= mix(vec3(1.0), vec3(0.45, 0.85, 0.95), saturate(dist * 0.08));
        color = mix(color, uw, 1.0 - exp(-dist * 0.055));
    } else if (isEyeInWater == 2) {
        color = mix(color, vec3(1.2, 0.35, 0.05), 1.0 - exp(-dist * 0.6));
    } else if (isEyeInWater == 3) {
        color = mix(color, vec3(0.75, 0.8, 0.9) * luma(ambientColor + 0.1), 1.0 - exp(-dist * 0.5));
    }

    // körlük / karanlık efekti
    float blind = max(blindness, darknessFactor);
    if (blind > 0.0) color *= 1.0 - blind * smoothstep(1.5, 8.0, isSky ? 100.0 : dist);

    outColor = vec4(color, 1.0);
}

#endif
