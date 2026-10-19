// Rüzgarda sallanan bitkiler / waving foliage

vec3 windOffset(vec3 worldPos, float amount) {
    float t = frameTimeCounter * (1.0 + rainStrength * 0.8);
    vec2 p = worldPos.xz;
    float gust = noise2(p * 0.05 + t * 0.12) * 0.7 + 0.3;
    float s1 = sin(t * 1.7 + dot(p, vec2(0.63, 0.77)) * 1.3);
    float s2 = sin(t * 2.9 + dot(p, vec2(-0.83, 0.42)) * 2.1);
    float s3 = sin(t * 4.3 + p.x * 3.1 + p.y * 1.7);
    vec3 off = vec3(s1 + 0.4 * s3, 0.0, s2 * 0.7 + 0.3 * s3);
    off.y = (s1 * s2) * 0.25;
    return off * amount * gust * 0.055 * WIND_STRENGTH * (1.0 + rainStrength * 1.2);
}

// matId: 10001 küçük bitki (taban sabit), 10002 yaprak, 10003 uzun bitkinin üst yarısı
vec3 applyWaving(vec3 worldPos, float matId, bool isTop) {
#if defined WAVING_PLANTS
    if (matId == 10001.0 && isTop) return worldPos + windOffset(worldPos, 1.0);
    if (matId == 10003.0) return worldPos + windOffset(worldPos, isTop ? 1.8 : 1.0);
#endif
#if defined WAVING_LEAVES
    if (matId == 10002.0) return worldPos + windOffset(worldPos, 0.45);
#endif
    return worldPos;
}
