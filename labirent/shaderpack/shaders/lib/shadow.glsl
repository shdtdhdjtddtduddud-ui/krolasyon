// ---------------------------------------------------------------- shadow lookup (needs shadowtex0, shadowModelView, shadowProjection)
const vec2 poisson[20] = vec2[](
    vec2(-0.613392, 0.617481), vec2(0.170019, -0.040254), vec2(-0.299417, 0.791925), vec2(0.645680, 0.493210),
    vec2(-0.651784, 0.717887), vec2(0.421003, 0.027070), vec2(-0.817194, -0.271096), vec2(-0.705374, -0.668203),
    vec2(0.977050, -0.108615), vec2(0.063326, 0.142369), vec2(0.203528, 0.214331), vec2(-0.667531, 0.326090),
    vec2(-0.098422, -0.295755), vec2(-0.885922, 0.215369), vec2(0.566637, 0.605213), vec2(0.039766, -0.396100),
    vec2(0.751946, 0.453352), vec2(0.078707, -0.715323), vec2(-0.075838, -0.529344), vec2(0.724479, -0.580798));

vec3 shadowPos(vec3 worldRel, vec3 worldNormal) {
    // worldRel: position relative to the camera (feet-player space)
    vec4 sp = shadowProjection * (shadowModelView * vec4(worldRel + worldNormal * 0.06, 1.0));
    return sp.xyz;
}

float sampleShadow(vec3 sp, float angle, float softness) {
    float df = distortFactor(sp.xy);
    vec3 p = vec3(distort(sp.xy), sp.z * 0.2) * 0.5 + 0.5;
    if (p.x <= 0.0 || p.y <= 0.0 || p.x >= 1.0 || p.y >= 1.0) return 1.0;
    float bias = 0.00035 * df * df + 0.00008;
    float s = sin(angle), c = cos(angle);
    mat2 rot = mat2(c, -s, s, c);
    float radius = softness * 1.6 / float(shadowMapResolution) / df;
    float lit = 0.0;
    for (int i = 0; i < SHADOW_SAMPLES; i++) {
        vec2 o = rot * poisson[i] * radius;
        lit += step(p.z - bias, texture2D(shadowtex0, p.xy + o).r);
    }
    return lit / float(SHADOW_SAMPLES);
}
