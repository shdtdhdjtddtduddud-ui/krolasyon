// composite1: ray-marched haze, mist and volumetric light (written to colortex4).

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/sky.glsl"

varying vec2 texcoord;
varying vec3 lightCol;
varying vec3 ambientCol;
varying vec3 zenithCol;
varying vec3 horizonCol;

#ifdef VSH
void main() {
	texcoord = gl_MultiTexCoord0.xy;
	gl_Position = ftransform();
	getAtmosphereColors(lightCol, ambientCol, zenithCol, horizonCol);
}
#endif

#ifdef FSH
#define SHADOW_SAMPLING
#include "/lib/shadow.glsl"
#include "/lib/fog.glsl"

uniform sampler2D depthtex0;

// Only the ray-marched haze / mist / light shafts are computed here and
// stored in colortex4; composite2 denoises them and applies them.
void main() {
	vec2 uv = texcoord;
	float d0 = texture2D(depthtex0, uv).r;
	vec3 view0 = screenToView(uv, d0);
	vec3 feet = viewToFeet(view0);
	vec3 dirW = normalize(mat3(gbufferModelViewInverse) * normalize(view0));
	bool sky = d0 >= 1.0;
	float dist = sky ? far * 1.6 : length(feet);
	float dither = interleavedGradient(gl_FragCoord.xy);

	vec4 fog = vec4(0.0, 0.0, 0.0, 1.0);
#if !defined NETHER && !defined END
	if (isEyeInWater == 0) {
		// darker fog when the player is underground
		float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
		float outside = mix(0.08, 1.0, smoothstep(0.0, 0.8, eyeSky));
		vec3 lightDirW = mat3(gbufferModelViewInverse) * normalize(shadowLightPosition);
		fog = traceFog(dirW, dist, lightDirW, lightCol * outside, ambientCol * outside, dither);
	}
#endif

	/* DRAWBUFFERS:4 */
	gl_FragData[0] = fog;
}
#endif
