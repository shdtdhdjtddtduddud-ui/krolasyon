// İleri (forward) aydınlatma / forward lighting
// Needs: common, atmosphere, shadow

uniform int heldBlockLightValue;
uniform int heldBlockLightValue2;

const vec3 TORCH_COLOR = vec3(1.00, 0.62, 0.32);

vec2 adjustLightmap(vec2 lm) {
    return clamp((lm - 1.0 / 32.0) * (16.0 / 15.0), 0.0, 1.0);
}

vec3 blockLight(float bl) {
    return TORCH_COLOR * (pow(bl, 2.6) * 1.6 + pow(bl, 9.0) * 2.4) * BLOCKLIGHT_BRIGHTNESS;
}

vec3 computeLighting(vec3 albedo, vec3 playerPos, vec3 worldNormal, vec2 lm, float ao,
                     float subsurface, float emission, float dither, out float directOut) {
#ifdef HANDHELD_LIGHT
    float held = float(max(heldBlockLightValue, heldBlockLightValue2));
    lm.x = max(lm.x, saturate((held - length(playerPos)) / 15.0) * 0.95);
#endif

    vec3 direct = vec3(0.0);
#if !defined WORLD_NETHER
    float NdotL = dot(worldNormal, lightDirW);
    float diff = mix(saturate(NdotL), 0.55 + 0.25 * abs(NdotL), subsurface);
#if defined WORLD_END
    float skyMask = 1.0;
#else
    float skyMask = smoothstep(0.03, 0.35, lm.y);
#endif
    if (diff * skyMask > 0.001) {
        vec3 sh = vec3(1.0);
    #ifdef SHADOWS_ACTIVE
        vec3 sn = subsurface > 0.5 ? lightDirW : worldNormal;
        sh = sampleShadow(playerPos, sn, subsurface > 0.5 ? 1.0 : NdotL, dither);
    #endif
        direct = lightColor * diff * sh * skyMask;
    }
#endif
    directOut = saturate(luma(direct) / max(luma(lightColor), 1e-3));

    // gök ışığı ortamı: üst yüzeyler biraz daha aydınlık
    float skyL = lm.y * lm.y;
    vec3 amb = ambientColor * skyL * (0.70 + 0.30 * worldNormal.y);
#if defined WORLD_NETHER || defined WORLD_END
    amb = ambientColor * (0.75 + 0.25 * worldNormal.y);
#endif
    vec3 minLight = vec3(0.020, 0.016, 0.034) * (1.0 - skyL);
    vec3 nv = vec3(0.35, 0.40, 0.45) * nightVision;
    vec3 bl = blockLight(lm.x);

    vec3 lit = direct + (amb + bl + minLight + nv) * ao;
    return albedo * lit + albedo * emission;
}
