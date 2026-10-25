// ---------------------------------------------------------------- lighting colours and procedural sky
// needs: sunPosition, moonPosition, upPosition (view space), sunAngle, rainStrength, fogColor, skyColor

float sunHeight() { return dot(normalize(sunPosition), normalize(upPosition)); }

// sky that has lost its sun (the mod hides the vanilla sky and greys the fog)
float greySky() {
    float sat = max(max(fogColor.r, fogColor.g), fogColor.b) - min(min(fogColor.r, fogColor.g), fogColor.b);
    return clamp(1.0 - sat * 14.0, 0.0, 1.0) * step(0.2, luma(fogColor));
}

vec3 sunColor() {
    float h = sunHeight();
    vec3 noon = vec3(1.0, 0.96, 0.88) * 3.2;
    vec3 golden = vec3(1.0, 0.55, 0.25) * 2.2;
    vec3 night = vec3(0.35, 0.45, 0.75) * 0.25;
    vec3 c = h > 0.0 ? mix(golden, noon, smoothstep(0.0, 0.45, h)) : mix(golden * 0.4, night, smoothstep(0.0, 0.12, -h));
    c *= 1.0 - rainStrength * 0.75;
    return mix(c, vec3(0.6, 0.62, 0.65) * 0.55, greySky());
}

vec3 ambientColor() {
    float h = sunHeight();
    vec3 day = vec3(0.42, 0.55, 0.75) * 0.75;
    vec3 dusk = vec3(0.45, 0.35, 0.35) * 0.45;
    vec3 night = vec3(0.10, 0.13, 0.22) * 0.32;
    vec3 c = h > 0.0 ? mix(dusk, day, smoothstep(0.0, 0.35, h)) : mix(dusk, night, smoothstep(0.0, 0.15, -h));
    c = mix(c, vec3(luma(c)) * 0.9, rainStrength * 0.6);
    return mix(c, vec3(0.42, 0.43, 0.45) * 0.7, greySky());
}

vec3 skyGradient(vec3 viewDir) {
    vec3 up = normalize(upPosition);
    vec3 sunDir = normalize(sunPosition);
    float y = dot(viewDir, up);
    float h = sunHeight();
    vec3 zenithDay = vec3(0.16, 0.34, 0.75), horizonDay = vec3(0.62, 0.74, 0.92);
    vec3 zenithNight = vec3(0.01, 0.015, 0.04), horizonNight = vec3(0.04, 0.05, 0.09);
    float day = smoothstep(-0.12, 0.2, h);
    vec3 zenith = mix(zenithNight, zenithDay, day), horizon = mix(horizonNight, horizonDay, day);
    float t = pow(clamp(1.0 - max(y, 0.0), 0.0, 1.0), 3.5);
    vec3 c = mix(zenith, horizon, t);
    // sunset glow around the sun
    float sd = max(dot(viewDir, sunDir), 0.0);
    float dusk = 1.0 - smoothstep(0.0, 0.35, abs(h));
    c += vec3(1.0, 0.42, 0.15) * pow(sd, 6.0) * dusk * 1.4 * t;
    c += vec3(1.0, 0.75, 0.5) * pow(sd, 64.0) * day * 0.6;
    if (y < 0.0) c = mix(c, horizon * 0.6, clamp(-y * 3.0, 0.0, 1.0));
    c = mix(c, vec3(luma(c)) * 0.75, rainStrength * 0.7);
    return mix(c, fogColor * 0.36, greySky());
}
