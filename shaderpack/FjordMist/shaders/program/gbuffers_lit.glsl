// Opaque / cutout geometry: terrain, block entities, entities, hand,
// particles and untextured geometry.
// Variants: TERRAIN, ENTITY, HAND, PARTICLE, BASIC

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/sky.glsl"
#include "/lib/waving.glsl"
#include "/lib/water.glsl"

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normalView;
varying vec3 feetPos;
varying float blockId;
varying vec3 lightCol;
varying vec3 ambientCol;

// ============================================================== VERTEX
#ifdef VSH
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	lmcoord  = normalizeLightmap((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy);
	glcolor  = gl_Color;
	normalView = normalize(gl_NormalMatrix * gl_Normal);
	blockId = 0.0;

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

	float translucency = 0.0;
	float emissive = 0.0;
	float material = MAT_TERRAIN;
	float reflectivity = 0.0;
	float wetSpec = 0.0;
	vec3 storedNormal = normal;

#ifdef TERRAIN
	bool foliage = isFoliageId(blockId);
	if (foliage) {
		translucency = idIs(blockId, ID_LEAVES) ? 0.65 : 1.0;
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
	if (idIs(blockId, ID_SHORE) && lm.y > 0.5) {
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
