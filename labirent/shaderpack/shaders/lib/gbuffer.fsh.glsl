// ---------------------------------------------------------------- shared fragment stage: writes albedo / normal+material / light data
uniform sampler2D texture;
uniform sampler2D lightmap;
uniform vec4 entityColor;
uniform int heldBlockLightValue;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;
varying vec3 normal;
varying vec3 viewPos;
varying float blockId;

void main() {
    vec4 albedo = texture2D(texture, texcoord) * color;
#ifdef ENTITY
    albedo.rgb = mix(albedo.rgb, entityColor.rgb, entityColor.a);
#endif
#ifndef NO_ALPHA_TEST
    if (albedo.a < 0.1) discard;
#endif
    vec3 n = normalize(normal);
    float mat = MAT_DEFAULT;
    float emissive = 0.0;
#ifdef TERRAIN
    if (blockId == 10001.0) mat = MAT_LEAVES;
    else if (blockId == 10002.0 || blockId == 10004.0) mat = MAT_PLANT;
    else if (blockId == 10005.0 || blockId == 10007.0) { mat = MAT_EMISSIVE; emissive = blockId == 10007.0 ? 1.0 : smoothstep(0.35, 0.9, luma(albedo.rgb)); }
    else if (blockId == 10003.0 || blockId == 10008.0) mat = MAT_WALL;
    if (mat == MAT_PLANT || mat == MAT_LEAVES) n = normalize(n + vec3(0.0, 0.6, 0.0));
    // fake relief on the maze walls: bend the normal along the texture's brightness gradient
    if (mat == MAT_WALL && WALL_RELIEF > 0.0) {
        float h = luma(albedo.rgb);
        vec3 dpx = dFdx(viewPos), dpy = dFdy(viewPos);
        float dhx = dFdx(h), dhy = dFdy(h);
        vec3 r1 = cross(dpy, n), r2 = cross(n, dpx);
        float det = dot(dpx, r1);
        vec3 grad = sign(det) * (dhx * r1 + dhy * r2);
        n = normalize(abs(det) * n - grad * 0.05 * WALL_RELIEF);
    }
#endif
#ifdef ENTITY
    mat = MAT_ENTITY;
#endif
#ifdef HAND
    mat = MAT_HAND;
#endif
#ifdef CLOUD
    mat = MAT_CLOUD;
#endif
/* DRAWBUFFERS:012 */
    gl_FragData[0] = albedo;
    gl_FragData[1] = vec4(n * 0.5 + 0.5, mat / 255.0);
    gl_FragData[2] = vec4(lmcoord, emissive, 1.0);
}
