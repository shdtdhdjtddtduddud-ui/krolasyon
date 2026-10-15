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
uniform sampler2D colortex1;

// Screen space ambient occlusion: darkens creases, corners, the ground under
// trees and between grass, where sky light cannot reach.
float computeAO(vec3 viewPos, vec3 normal, float dither) {
	float radius = 0.75;
	vec3 t = normalize(cross(normal, abs(normal.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
	vec3 b = cross(normal, t);
	float occ = 0.0;
	for (int i = 0; i < 8; i++) {
		float fi = (float(i) + dither) / 8.0;
		float ang = float(i) * 2.39996 + dither * 6.2832;
		float r = sqrt(fi) * radius;
		vec3 off = (t * cos(ang) + b * sin(ang)) * r + normal * (0.08 + r * 0.6);
		vec3 sp = viewPos + off;
		vec3 ss = viewToScreen(sp);
		if (ss.x < 0.0 || ss.x > 1.0 || ss.y < 0.0 || ss.y > 1.0) continue;
		float sd = texture2D(depthtex0, ss.xy).r;
		float sceneZ = screenToView(ss.xy, sd).z;
		float diff = sceneZ - sp.z;
		float range = smoothstep(0.0, 1.0, radius * 1.5 / max(abs(viewPos.z - sceneZ), 0.001));
		occ += step(0.02, diff) * range;
	}
	return 1.0 - occ / 8.0;
}

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

	float ao = 1.0;
#ifdef SSAO
	if (!sky && d0 > 0.56 && dist < 80.0) {
		vec4 data1 = texture2D(colortex1, uv);
		float material = floor(data1.z * 255.0 + 0.5);
		if (abs(material - MAT_WATER) > 0.5 && abs(material - MAT_HAND) > 0.5) {
			ao = computeAO(view0, decodeNormal(data1.xy), dither);
			ao = mix(ao, 1.0, smoothstep(40.0, 80.0, dist));
		}
	}
#endif

	/* DRAWBUFFERS:46 */
	gl_FragData[0] = fog;
	gl_FragData[1] = vec4(ao, 0.0, 0.0, 1.0);
}
#endif
