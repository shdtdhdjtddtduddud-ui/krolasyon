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

#ifdef VSH
void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	lmcoord  = normalizeLightmap((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy);
	glcolor  = gl_Color;
	gl_Position = ftransform();
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
	vec4 tex = texture2D(texture, texcoord) * glcolor;
	vec4 outCol;
#if defined DAMAGED
	outCol = tex;                                       // multiplicative blend
#elif defined GLINT
	outCol = vec4(pow(tex.rgb, vec3(2.2)) * 1.5, tex.a);
#elif defined WEATHER
	if (tex.a < 0.02) discard;
	// rain streaks catch the soft gray light of the clouds
	vec3 rainLight = ambientCol * 1.35 + lightCol * 0.15 + vec3(0.02);
	float a = tex.a * 0.55 * (0.5 + 0.5 * rainStrength);
	outCol = vec4(rainLight * mix(vec3(1.0), pow(tex.rgb, vec3(2.2)) * 1.6, 0.5), a);
#else
	outCol = vec4(pow(tex.rgb, vec3(2.2)) * 3.0, tex.a);  // emissive
#endif
	/* DRAWBUFFERS:0 */
	gl_FragData[0] = outCol;
}
#endif
