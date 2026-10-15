// Sky geometry. SKYBASIC: sky dome + stars. SKYTEXTURED: sun / moon quads.
// The full sky (with clouds) is recomputed in composite for open-sky pixels;
// what is written here is only visible through glass or other translucents.
// Stars and the moon go to colortex3 so composite can add them under clouds.

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/sky.glsl"

varying vec3 viewPos;
varying vec4 glcolor;
varying vec2 texcoord;
varying vec3 lightCol;
varying vec3 zenithCol;
varying vec3 horizonCol;

#ifdef VSH
void main() {
	vec4 vp = gl_ModelViewMatrix * gl_Vertex;
	viewPos = vp.xyz;
	glcolor = gl_Color;
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	gl_Position = gl_ProjectionMatrix * vp;
	vec3 amb;
	getAtmosphereColors(lightCol, amb, zenithCol, horizonCol);
}
#endif

#ifdef FSH
uniform int renderStage;
uniform sampler2D texture;

void main() {
#ifdef SKYBASIC
	bool star = false;
	#ifdef MC_RENDER_STAGE_STARS
	star = renderStage == MC_RENDER_STAGE_STARS;
	#else
	star = glcolor.r == glcolor.g && glcolor.g == glcolor.b && glcolor.r > 0.0;
	#endif
	/* DRAWBUFFERS:03 */
	if (star) {
	#ifdef STARS
		float s = 0.0;   // procedural stars are drawn in getSky()
	#else
		float s = glcolor.r * glcolor.a * 1.6;
	#endif
		gl_FragData[0] = vec4(0.0);
		gl_FragData[1] = vec4(vec3(0.85, 0.9, 1.0) * s, 1.0);
	} else {
		vec3 dir = normalize(viewPos);
		gl_FragData[0] = vec4(getSky(dir, lightCol, zenithCol, horizonCol, true), 1.0);
		gl_FragData[1] = vec4(0.0);
	}
#else
	// SKYTEXTURED
	#ifdef MC_RENDER_STAGE_SUN
	if (renderStage == MC_RENDER_STAGE_SUN) discard;   // sun is procedural
	#endif
	#if defined END || defined NETHER || defined PROCEDURAL_MOON
	discard;
	#endif
	vec4 tex = texture2D(texture, texcoord) * glcolor;
	vec3 moon = pow(tex.rgb, vec3(2.2)) * tex.a * vec3(0.8, 0.88, 1.0) * 1.4;
	/* DRAWBUFFERS:03 */
	gl_FragData[0] = vec4(0.0);
	gl_FragData[1] = vec4(moon, 1.0);
#endif
}
#endif
