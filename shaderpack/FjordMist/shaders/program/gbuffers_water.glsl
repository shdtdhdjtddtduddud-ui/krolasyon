// Translucent geometry: water, stained glass, ice, slime, translucent hand items.
// Water itself is only "marked" here (normal + material id); the actual water
// shading (refraction, absorption, reflections, foam) happens in composite.

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

void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	lmcoord  = normalizeLightmap((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy);
	glcolor  = gl_Color;
	normalView = normalize(gl_NormalMatrix * gl_Normal);
	blockId = mc_Entity.x;

	vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
	vec3 feet = viewToFeet(viewPos.xyz);

#if defined WATER_VERTEX_WAVES && !defined HAND_WATER
	if (idIs(blockId, ID_WATER) && gl_Normal.y > 0.5) {
		vec3 wp = feet + cameraPosition;
		feet.y += waterHeightVertex(wp.xz) * 0.065 * waterWaveStrength() * smoothstep(0.5, 0.9, lmcoord.y);
		viewPos = gbufferModelView * vec4(feet, 1.0);
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

void main() {
	vec4 albedo = texture2D(texture, texcoord) * glcolor;
	vec3 normal = normalize(normalView);
	if (!gl_FrontFacing) normal = -normal;
	vec2 lm = lmcoord;

#ifndef HAND_WATER
	if (idIs(blockId, ID_WATER)) {
		vec3 nWorld = mat3(gbufferModelViewInverse) * normal;
		if (abs(nWorld.y) > 0.5) {
			float dist = length(feetPos);
			float fade = 1.0 - smoothstep(60.0, 220.0, dist) * 0.7;
			// ripples are only a few pixels wide far away: fade them out to avoid shimmering
			float rippleFade = 1.0 - smoothstep(12.0, 40.0, dist);
			vec3 wn = waterNormal(feetPos.xz + cameraPosition.xz, fade, rippleFade);
			if (nWorld.y < 0.0) wn.y = -wn.y;   // seen from below
			normal = normalize(mat3(gbufferModelView) * wn);
		}
		// faint base tint; the real colour is computed in composite
		vec3 tint = ambientCol * vec3(0.05, 0.085, 0.09) * lm.y;
		/* DRAWBUFFERS:012 */
		gl_FragData[0] = vec4(tint, 0.12);
		gl_FragData[1] = vec4(encodeNormal(normal), MAT_WATER / 255.0, lm.y);
		gl_FragData[2] = vec4(1.0, 1.0, lm.x, 1.0);
		return;
	}
#endif

	if (albedo.a < 0.01) discard;
	vec3 color = pow(albedo.rgb, vec3(2.2));
	float shadow;
	float dither = interleavedGradient(gl_FragCoord.xy);
	vec3 lit = computeLighting(color, normal, feetPos, lm, 0.0, 0.0, lightCol, ambientCol, dither, shadow);

	// glass gets a soft sky sheen
	vec3 viewDir = normalize((gbufferModelView * vec4(feetPos, 0.0)).xyz);
	float fresnel = pow(1.0 - saturate(dot(-viewDir, normal)), 5.0);
	lit += ambientCol * fresnel * 0.4 * lm.y;
	float alpha = mix(albedo.a, 1.0, fresnel * 0.5);

	gl_FragData[0] = vec4(lit, alpha);
	gl_FragData[1] = vec4(encodeNormal(normal), MAT_GLASS / 255.0, lm.y);
	gl_FragData[2] = vec4(0.0, shadow, lm.x, 1.0);
}
#endif
