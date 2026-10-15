// Opaque / cutout geometry: terrain, block entities, entities, hand,
// particles and untextured geometry.
// Variants: TERRAIN, ENTITY, HAND, PARTICLE, BASIC

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/sky.glsl"
#include "/lib/waving.glsl"
#include "/lib/water.glsl"
#include "/lib/world.glsl"

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normalView;
varying vec3 feetPos;
varying float blockId;
varying vec3 lightCol;
varying vec3 ambientCol;
varying vec4 tileInfo;     // xy = centre of this block's texture in the atlas, zw = half size
varying vec4 tangentView;  // xyz = view space tangent, w = handedness

// ============================================================== VERTEX
#ifdef VSH
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;
attribute vec4 at_tangent;

void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	lmcoord  = normalizeLightmap((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy);
	glcolor  = gl_Color;
	normalView = normalize(gl_NormalMatrix * gl_Normal);
	blockId = 0.0;
	vec2 midUV = (gl_TextureMatrix[0] * vec4(mc_midTexCoord.xy, 0.0, 1.0)).xy;
	tileInfo = vec4(midUV, abs(texcoord - midUV));
	tangentView = vec4(gl_NormalMatrix * at_tangent.xyz, at_tangent.w);

	vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
	vec3 feet = viewToFeet(viewPos.xyz);

#ifdef TERRAIN
	blockId = mc_Entity.x;
	bool topVertex = gl_MultiTexCoord0.t < mc_midTexCoord.t;
	vec3 wave = waveOffset(feet + cameraPosition, blockId, topVertex, lmcoord.y);
	if (dot(wave, wave) > 0.0) {
		feet += wave;
		viewPos = gbufferModelView * vec4(feet, 1.0);
	}
	// grass and flowers: shade with an up-facing normal so they blend
	// smoothly into the ground instead of showing flat billboard planes
	if (idIs(blockId, ID_PLANT) || idIs(blockId, ID_DOUBLE_LOWER) || idIs(blockId, ID_DOUBLE_UPPER)) {
		normalView = normalize(mix(normalView, normalize(upPosition), 0.75));
	}
#endif

	feetPos = feet;
	gl_Position = gl_ProjectionMatrix * viewPos;

	vec3 zen, hor;
	getAtmosphereColors(lightCol, ambientCol, zen, hor);
}
#endif

// ============================================================ FRAGMENT
#ifdef FSH
#define SHADOW_SAMPLING
#include "/lib/shadow.glsl"
#include "/lib/lighting.glsl"

uniform sampler2D texture;
uniform vec4 entityColor;
uniform ivec2 atlasSize;

float texHeight(vec2 uv) {
	vec2 lo = tileInfo.xy - tileInfo.zw;
	vec2 hi = tileInfo.xy + tileInfo.zw;
	return luma(texture2D(texture, clamp(uv, lo, hi)).rgb);
}

// Relief from the block texture itself: brighter texels stand out, darker
// cracks and gaps sink in, so stone, wood and dirt catch the light in 3D.
vec3 autoNormal(vec3 n, float dist) {
#ifdef AUTO_NORMALS
	if (atlasSize.x <= 0 || dot(tangentView.xyz, tangentView.xyz) < 0.01 || tileInfo.z <= 0.0) return n;
	float fade = 1.0 - smoothstep(20.0, 64.0, dist);
	if (fade <= 0.0) return n;
	vec2 texel = 1.0 / vec2(atlasSize);
	float h0 = texHeight(texcoord);
	float hx = texHeight(texcoord + vec2(texel.x, 0.0));
	float hy = texHeight(texcoord + vec2(0.0, texel.y));
	float hx2 = texHeight(texcoord - vec2(texel.x, 0.0));
	float hy2 = texHeight(texcoord - vec2(0.0, texel.y));
	vec2 grad = vec2(hx2 - hx, hy - hy2) * 0.5;
	vec3 t = normalize(tangentView.xyz);
	vec3 b = normalize(cross(t, n)) * (tangentView.w < 0.0 ? -1.0 : 1.0);
	vec3 nt = normalize(vec3(grad * 4.0 * NORMAL_STRENGTH * fade, 1.0));
	return normalize(t * nt.x + b * nt.y + n * nt.z);
#else
	return n;
#endif
}

// How close a point is to the edge of the block face it lies on (0 = edge).
float blockEdge(vec3 wp, vec3 nWorld) {
	vec3 f = fract(wp + 0.0001);
	vec3 d = min(f, 1.0 - f);
	vec3 an = abs(nWorld);
	// ignore the axis the face is facing
	float e = 1.0;
	if (an.x < 0.5) e = min(e, d.x);
	if (an.y < 0.5) e = min(e, d.y);
	if (an.z < 0.5) e = min(e, d.z);
	return e;
}

#ifdef BASIC
// lines, outlines, leashes: unlit
void main() {
	vec4 albedo = glcolor;
	if (albedo.a < 0.1) discard;
	/* DRAWBUFFERS:0 */
	gl_FragData[0] = vec4(pow(albedo.rgb, vec3(2.2)) * 0.6, albedo.a);
}
#else
void main() {
	vec4 albedo = texture2D(texture, texcoord) * glcolor;
	if (albedo.a < 0.1) discard;

#ifdef ENTITY
	albedo.rgb = mix(albedo.rgb, entityColor.rgb, entityColor.a);
#endif

	vec3 color = pow(albedo.rgb, vec3(2.2));
	vec3 normal = normalize(normalView);
	if (!gl_FrontFacing) normal = -normal;
	vec2 lm = lmcoord;
	float dither = interleavedGradient(gl_FragCoord.xy);
	vec3 geoNormal = normal;

	float translucency = 0.0;
	float emissive = 0.0;
	float material = MAT_TERRAIN;
	float reflectivity = 0.0;
	float wetSpec = 0.0;
	float sparkleAmt = 0.0;
	float caveWet = 0.0;
	vec3 storedNormal = normal;

#ifdef TERRAIN
	bool foliage = isFoliageId(blockId);
	if (foliage) {
		translucency = (idIs(blockId, ID_LEAVES) || idIs(blockId, ID_EVERGREEN)) ? 0.65 : 1.0;
		material = MAT_FOLIAGE;
	}
	if (idIs(blockId, ID_EMISSIVE)) {
		float l = luma(color);
		emissive = smoothstep(0.25, 0.75, l) * 1.2 + 0.08;
		material = MAT_EMISSIVE;
	}

	// ---- rain soaked world: darker, more saturated surfaces + puddles
	vec3 upDir = normalize(upPosition);
	float upness = dot(normal, upDir);
	float exposed = smoothstep(0.86, 0.96, lm.y);
	float wet = saturate(wetness * exposed * WETNESS_STRENGTH * (0.45 + 0.55 * saturate(upness)));
	float puddle = 0.0;
	#ifdef PUDDLES
	if (!foliage && upness > 0.9 && wet > 0.01) {
		vec2 wp = feetPos.xz + cameraPosition.xz;
		float n = noise2D(wp * 0.22) * 0.65 + noise2D(wp * 0.9 + 3.1) * 0.35;
		puddle = smoothstep(0.50, 0.60, n) * wet;
		storedNormal = upDir;
		#ifdef RAIN_RIPPLES
		// raindrops make rings on the puddles too
		float pd = length(feetPos);
		if (puddle > 0.05 && rainStrength > 0.01 && pd < 24.0) {
			vec3 drops = rainDrops(wp, rainStrength);
			float k = rainStrength * 0.25 * (1.0 - smoothstep(10.0, 24.0, pd));
			storedNormal = normalize(mat3(gbufferModelView) * normalize(vec3(-drops.x * k, 1.0, -drops.y * k)));
		}
		#endif
	}
	#endif
	#ifdef SHORE_WETNESS
	// sand and stone at the water line are soaked by the waves; the wet band
	// creeps up and down as the waves roll in
	if ((idIs(blockId, ID_SHORE) || idIs(blockId, ID_STONE)) && lm.y > 0.5) {
		vec3 wpos = feetPos + cameraPosition;
		float t = frameTimeCounter * WATER_WAVE_SPEED;
		float reach = 1.1 + sin(t * 0.7 + dot(wpos.xz, vec2(0.21, 0.17))) * 0.35 + waterWaveStrength() * 0.4;
		float shore = (1.0 - smoothstep(reach - 0.6, reach, wpos.y - (SEA_LEVEL - 0.1))) * step(SEA_LEVEL - 1.5, wpos.y);
		wet = max(wet, shore);
	}
	#endif
	float snow = 0.0;
	#ifdef SNOW_COVER
	// snowy biomes: snow piles up on everything facing the sky while it snows
	if (biome_precipitation == 2) {
		vec3 wpos = feetPos + cameraPosition;
		float n = noise2D(wpos.xz * 0.6) * 0.5 + noise2D(wpos.xz * 2.3) * 0.5;
		snow = smoothstep(0.35, 0.75, upness + n * 0.35) * smoothstep(0.86, 0.96, lm.y) * saturate(wetness * 1.5);
		wet *= 0.2;
		puddle = 0.0;
		storedNormal = mix(storedNormal, normal, snow);
	}
	#endif
	vec3 wpW = feetPos + cameraPosition;
	vec4 season = seasonWeights();
	float tinted = biomeTint(glcolor);
	bool leafBlock = idIs(blockId, ID_LEAVES) || idIs(blockId, ID_EVERGREEN);

	#ifdef COLOR_VARIATION
	// natural variation: patches of lusher / drier vegetation and lighter /
	// darker ground, so large areas never look copy-pasted
	{
		float m = noise2D(wpW.xz * 0.03) * 0.6 + noise2D(wpW.xz * 0.11 + 5.0) * 0.4;
		color *= mix(vec3(1.0), mix(vec3(1.07, 1.02, 0.82), vec3(0.88, 1.03, 1.05), m), tinted);
		color *= 0.93 + 0.14 * noise2D(wpW.xz * 0.07 + 11.0);
	}
	#endif

	#ifdef SEASONS
	// seasons: spring green, summer, autumn reds and golds, pale winter
	color = seasonalFoliage(color, tinted, wpW, leafBlock, idIs(blockId, ID_EVERGREEN), season);
	#endif

	#ifdef MOSS
	// moss creeps over stone in damp, sheltered places and near water
	if (idIs(blockId, ID_STONE) && upness > -0.3) {
		float damp = saturate(wetness * 0.5 + (1.0 - smoothstep(SEA_LEVEL + 2.0, SEA_LEVEL + 14.0, wpW.y)) * 0.6 + season.x * 0.2 + 0.15);
		float n = noise2D(wpW.xz * 0.35 + wpW.y * 0.27) * 0.6 + noise2D(wpW.xz * 1.7 - wpW.y * 0.9) * 0.4;
		float moss = smoothstep(0.62 - damp * 0.25, 0.8 - damp * 0.2, n) * (0.4 + 0.6 * saturate(upness + 0.3)) * MOSS_AMOUNT;
		moss *= smoothstep(0.2, 0.6, lm.y);
		vec3 mossCol = vec3(0.13, 0.24, 0.06) * (0.7 + 0.6 * noise2D(wpW.xz * 4.0));
		color = mix(color, mossCol, saturate(moss) * 0.8);
	}
	#endif

	#ifdef SNOW_LINE
	// snow caps on the mountain tops; the snow line drops in winter
	{
		float n = noise2D(wpW.xz * 0.05) * 18.0 + noise2D(wpW.xz * 0.4) * 5.0;
		float line = SNOW_LINE_HEIGHT - season.w * 45.0 + season.y * 15.0 + n;
		float alt = smoothstep(line - 4.0, line + 6.0, wpW.y);
		float face = smoothstep(0.15, 0.55, upness + noise2D(wpW.xz * 1.3) * 0.25);
		snow = max(snow, alt * face * smoothstep(0.4, 0.8, lm.y));
	}
	#endif

	#if defined SEASONS && defined SNOW_COVER
	// winter: snow covers the land (deeper the longer winter lasts)
	if (season.w > 0.3) {
		float n = noise2D(wpW.xz * 0.6) * 0.5 + noise2D(wpW.xz * 2.3) * 0.5;
		float cover = smoothstep(0.3, 0.9, season.w) * smoothstep(0.3, 0.7, upness + n * 0.4) * exposed;
		snow = max(snow, cover);
		wet *= 1.0 - cover;
		puddle *= 1.0 - cover;
	}
	#endif

	#ifdef FROST
	// hoarfrost: on clear, cold nights grass and leaves turn white; it melts
	// once the sun climbs
	{
		float coldNight = 1.0 - smoothstep(0.04, 0.3, getSunElevation());
		float cold = saturate(season.w + season.z * 0.6 + season.x * 0.2 + smoothstep(100.0, 150.0, wpW.y));
		float frost = coldNight * cold * (1.0 - rainStrength) * exposed * max(tinted, foliage ? 1.0 : 0.0) * FROST_AMOUNT;
		frost *= smoothstep(0.0, 0.5, upness + 0.3 + noise2D(wpW.xz * 3.0) * 0.4);
		color = mix(color, vec3(0.72, 0.8, 0.9) * (0.8 + 0.3 * luma(color)), saturate(frost) * 0.7);
		sparkleAmt = max(sparkleAmt, saturate(frost));
	}
	#endif

	#ifdef CAVE_DAMPNESS
	// caves are damp: stone walls are darker and glisten in torch light
	if (lm.y < 0.15 && wpW.y < SEA_LEVEL - 5.0 && !foliage) {
		float damp = smoothstep(0.4, 0.75, noise2D(wpW.xz * 0.25 + wpW.y * 0.31)) * (1.0 - lm.y / 0.15);
		wet = max(wet, damp * 0.7);
		caveWet = damp;
	}
	#endif

	#ifdef SPARKLES
	if (idIs(blockId, ID_ORE)) sparkleAmt = max(sparkleAmt, 0.6);
	if (idIs(blockId, ID_SNOWICE)) sparkleAmt = max(sparkleAmt, 0.35);
	#endif
	sparkleAmt = max(sparkleAmt, snow * 0.35);

	float porosity = foliage ? 0.35 : 1.0;
	color = pow(color, vec3(1.0 + 0.35 * wet * porosity));
	color *= 1.0 - 0.30 * wet * porosity;
	color *= 1.0 - 0.18 * puddle;
	reflectivity = foliage ? wet * 0.06 : wet * 0.22 + puddle * 0.78;
	color = mix(color, vec3(0.86, 0.9, 0.96), snow);
	reflectivity *= 1.0 - snow;
	wetSpec = foliage ? wet * 0.5 : wet;
#endif

#ifdef ENTITY
	material = MAT_ENTITY;
#endif
#ifdef HAND
	material = MAT_HAND;
#endif
#ifdef PARTICLE
	translucency = 0.5;
	normal = normalize(upPosition);
#endif

#ifdef TERRAIN
	if (!isFoliageId(blockId) && snow < 0.5) {
		float dist = length(feetPos);
		normal = autoNormal(normal, dist);
	#ifdef BLOCK_EDGES
		// soft bevel: block faces darken slightly towards their edges, so
		// every block reads as a solid cube
		vec3 nW = mat3(gbufferModelViewInverse) * geoNormal;
		if (max(abs(nW.x), max(abs(nW.y), abs(nW.z))) > 0.95) {
			float e = blockEdge(feetPos + cameraPosition, nW);
			float bevel = smoothstep(0.0, 0.07, e);
			float edgeFade = 1.0 - smoothstep(12.0, 36.0, dist);
			color *= mix(1.0 - 0.16 * EDGE_STRENGTH * edgeFade, 1.0, bevel);
		}
	#endif
	}
#endif
	float shadow;
	vec3 lit = computeLighting(color, normal, feetPos, lm, translucency, emissive, lightCol, ambientCol, dither, shadow);

#ifdef WET_GLINTS
	// wet stones and leaves glisten in the sun / moon
	if (wetSpec > 0.01) {
		vec3 V = -normalize((gbufferModelView * vec4(feetPos, 1.0)).xyz);
		vec3 Lh = normalize(normalize(shadowLightPosition) + V);
		float sp = pow(saturate(dot(normal, Lh)), 90.0) * 3.0 + pow(saturate(dot(normal, Lh)), 12.0) * 0.08;
		float fres = 0.04 + 0.96 * pow(1.0 - saturate(dot(normal, V)), 5.0);
		lit += lightCol * sp * shadow * wetSpec * (0.25 + fres);
	}
#endif

#ifdef TERRAIN
	vec3 Vw = normalize(-feetPos);
	#ifdef SPARKLES
	// glittering minerals, snow, ice and frost
	if (sparkleAmt > 0.01) {
		float sp = sparkle(feetPos + cameraPosition, -Vw, 0.12) * sparkleAmt;
		float torchL = pow(max(lm.x, saturate((float(max(heldBlockLightValue, heldBlockLightValue2)) - length(feetPos)) / 15.0)), 2.0);
		lit += sp * (lightCol * shadow * 2.0 + ambientCol * lm.y * 0.6 + TORCH_COLOR * torchL * 1.5) * 1.5;
	}
	#endif
	#ifdef CAVE_DAMPNESS
	// water drops on damp cave walls catch the light of a torch in your hand
	if (caveWet > 0.01) {
		float heldL = saturate((float(max(heldBlockLightValue, heldBlockLightValue2)) - length(feetPos)) / 15.0);
		float lightHere = max(heldL, pow(lm.x, 2.0));
		vec3 V = -normalize((gbufferModelView * vec4(feetPos, 1.0)).xyz);
		float spec = pow(saturate(dot(normal, V)), 30.0) * heldL;
		float drops = sparkle(feetPos + cameraPosition, -Vw, 0.08);
		lit += TORCH_COLOR * (spec * 0.4 + drops * 1.2) * caveWet * lightHere * TORCH_BRIGHTNESS;
	}
	#endif
#endif

#ifdef PARTICLE
	/* DRAWBUFFERS:0 */
	gl_FragData[0] = vec4(lit, albedo.a);
#else
	/* DRAWBUFFERS:012 */
	gl_FragData[0] = vec4(lit, albedo.a);
	gl_FragData[1] = vec4(encodeNormal(storedNormal), material / 255.0, lm.y);
	gl_FragData[2] = vec4(reflectivity, shadow, lm.x, 1.0);
#endif
}
#endif // BASIC
#endif
