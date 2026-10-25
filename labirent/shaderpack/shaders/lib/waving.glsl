// ---------------------------------------------------------------- wind (needs cameraPosition, frameTimeCounter, rainStrength)
vec3 windOffset(vec3 wp, float amount) {
    float t = frameTimeCounter * WAVE_SPEED;
    float gust = 0.6 + 0.4 * sin(t * 0.35 + wp.x * 0.02) + rainStrength * 0.8;
    float a = sin(t * 1.7 + wp.x * 0.45 + wp.z * 0.25) + sin(t * 2.9 + wp.z * 0.6) * 0.5;
    float b = cos(t * 1.3 + wp.z * 0.5 + wp.x * 0.15) + sin(t * 3.7 + wp.x * 0.9) * 0.35;
    return vec3(a, 0.25 * sin(t * 2.2 + wp.x), b) * amount * gust * WAVE_STRENGTH;
}

// world: position relative to the camera; id: block id from block.properties
vec3 wave(vec3 world, float id, bool topVertex, vec3 midBlock) {
    vec3 wp = world + cameraPosition;
#ifdef WAVING_PLANTS
    if (id == 10001.0) return world + windOffset(wp, 0.045);
    if (id == 10002.0 && topVertex) return world + windOffset(wp, 0.09) * vec3(1.0, 0.3, 1.0);
    if (id == 10004.0) return world + windOffset(wp, 0.035);
#endif
#ifdef WAVING_IVY
    // ivy walls: only the leafy shell floating just outside the stone moves
    if (id == 10003.0) {
        float outside = step(33.0, max(max(abs(midBlock.x), abs(midBlock.y)), abs(midBlock.z)));
        return world + windOffset(wp, 0.035) * outside * vec3(1.0, 0.4, 1.0);
    }
#endif
    return world;
}
