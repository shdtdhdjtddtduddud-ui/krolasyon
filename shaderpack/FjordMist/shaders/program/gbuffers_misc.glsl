// Unlit special geometry: beacon beams, glowing eyes, enchantment glint,
// block breaking cracks, vanilla clouds (hidden) and rain / snow.
// Variants: EMISSIVE, GLINT, DAMAGED, CLOUDS, WEATHER

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/sky.glsl"

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 lightCol;
varying vec3 ambientCol;
varying float viewDist;

#ifdef VSH
void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	lmcoord  = normalizeLightmap((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy);
	glcolor  = gl_Color;
	vec4 vp = gl_ModelViewMatrix * gl_Vertex;
	viewDist = length(vp.xyz);
	gl_Position = gl_ProjectionMatrix * vp;
	vec3 zen, hor;
	getAtmosphereColors(lightCol, ambientCol, zen, hor);
}
#endif

#ifdef FSH
uniform sampler2D texture;

void main() {
#ifdef CLOUDS
	discard;
#endif
#ifdef LIGHTNING
	vec4 tex = glcolor;
#else
	vec4 tex = texture2D(texture, texcoord) * glcolor;
#endif
	vec4 outCol;
#if defined DAMAGED
	outCol = tex;                                       // multiplicative blend
#elif defined LIGHTNING
	outCol = vec4(vec3(0.75, 0.82, 1.0) * 12.0, tex.a);  // blinding white-blue bolt
#elif defined GLINT
	outCol = vec4(pow(tex.rgb, vec3(2.2)) * 1.5, tex.a);
#elif defined WEATHER
	// streaks: smear the drop texture along the fall direction (motion blur)
	vec4 t0 = texture2D(texture, texcoord);
	#ifdef SNOW_COVER
	if (biome_precipitation == 2) {
		// snowflakes: soft, bright, no motion blur
		if (t0.a < 0.05) discard;
		vec3 snowLight = ambientCol * 2.0 + lightCol * 0.5 + vec3(0.02);
		float sa = t0.a * glcolor.a * smoothstep(0.4, 2.0, viewDist) * 0.9 * RAIN_VISIBILITY;
		/* DRAWBUFFERS:5 */
		gl_FragData[0] = vec4(snowLight * sa, sa);
		return;
	}
	#endif
	vec4 t1 = texture2D(texture, texcoord + vec2(0.0, 0.012));
	vec4 t2 = texture2D(texture, texcoord - vec2(0.0, 0.012));
	vec4 t3 = texture2D(texture, texcoord + vec2(0.0, 0.024));
	float a = max(max(t0.a, t1.a * 0.85), max(t2.a * 0.85, t3.a * 0.6)) * glcolor.a;
	if (a < 0.02) discard;
	// drops refract the bright sky: lit mostly by the ambient light
	vec3 rainLight = ambientCol * 1.6 + lightCol * 0.2 + vec3(0.015);
	float nearFade = smoothstep(0.6, 3.5, viewDist);       // no giant blobs on the lens
	float farFade = 1.0 - smoothstep(18.0, 40.0, viewDist) * 0.5;
	a *= (0.35 + 0.3 * rainStrength) * nearFade * farFade * RAIN_VISIBILITY;
	vec3 tint = pow(max(t0.rgb, vec3(0.0)), vec3(2.2));
	outCol = vec4(rainLight * mix(vec3(1.0), tint * 1.4, 0.35), saturate(a));
#else
	outCol = vec4(pow(tex.rgb, vec3(2.2)) * 3.0, tex.a);  // emissive
#endif
#ifdef WEATHER
	// rain goes to its own buffer and is laid over the image in composite2,
	// so water does not reflect it and fog does not swallow it
	/* DRAWBUFFERS:5 */
	gl_FragData[0] = vec4(outCol.rgb * outCol.a, outCol.a);
#else
	/* DRAWBUFFERS:0 */
	gl_FragData[0] = outCol;
#endif
}
#endif
