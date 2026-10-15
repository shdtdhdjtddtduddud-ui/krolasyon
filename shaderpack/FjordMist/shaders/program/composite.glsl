// composite: sky + volumetric clouds, water shading (refraction, absorption,
// reflections, sun glints, shore foam) and wet surface reflections.

/*
const int colortex0Format = RGBA16F;
const int colortex1Format = RGBA16;
const int colortex2Format = RGBA8;
const int colortex3Format = R11F_G11F_B10F;
const vec4 colortex1ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
const vec4 colortex2ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
const vec4 colortex3ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
const bool colortex3Clear = true;
*/

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
#include "/lib/clouds.glsl"
#include "/lib/water.glsl"

uniform sampler2D colortex0;
uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform sampler2D colortex3;
uniform sampler2D depthtex0;
uniform sampler2D depthtex1;

#include "/lib/ssr.glsl"

vec3 lightDirWorld() {
	return mat3(gbufferModelViewInverse) * normalize(shadowLightPosition);
}

// Sky with clouds, used for open sky pixels and reflections.
vec3 skyWithClouds(vec3 dirView, float dither, int steps, int lightSteps, bool withSun) {
	vec3 sky = getSky(dirView, lightCol, zenithCol, horizonCol, withSun);
#if defined VOLUMETRIC_CLOUDS && !defined NETHER && !defined END
	vec3 dirW = mat3(gbufferModelViewInverse) * dirView;
	if (dirW.y > 0.0 || cameraPosition.y > cloudBottom()) {
		vec4 cl = traceClouds(dirW, 100000.0, lightDirWorld(), lightCol, ambientCol, dither, steps, lightSteps);
		sky = sky * cl.a + cl.rgb;
	}
#endif
	return sky;
}

vec3 reflectionColor(vec3 viewPos, vec3 reflDir, float skyLm, float dither, int steps, bool cheapSky, float skyFallback) {
	vec3 hit = traceSSR(viewPos, reflDir, interleavedGradient(gl_FragCoord.xy), steps);
	vec3 reflDirW = mat3(gbufferModelViewInverse) * reflDir;
	vec3 sky;
	if (cheapSky) {
		sky = getSky(reflDir, lightCol, zenithCol, horizonCol, false);
	} else {
		sky = skyWithClouds(reflDir, dither, 8, 2, false);
	}
	// reflections pointing down see the dark far shore, not the sky
	sky = mix(sky, horizonCol * 0.4, smoothstep(0.02, -0.25, reflDirW.y));
	sky *= smoothstep(0.25, 0.9, skyLm) * skyFallback;
	if (hit.z > 0.0) {
		vec3 sceneRefl = texture2D(colortex0, hit.xy).rgb;
		return mix(sky, sceneRefl, hit.z);
	}
	return sky;
}

void main() {
	vec2 uv = texcoord;
	vec3 color = texture2D(colortex0, uv).rgb;
	float d0 = texture2D(depthtex0, uv).r;
	float d1 = texture2D(depthtex1, uv).r;
	vec3 view0 = screenToView(uv, d0);
	vec3 dirView = normalize(view0);
	float dither = bayer16(gl_FragCoord.xy);

	if (d0 >= 1.0) {
		// ------------------------------------------------------- open sky
		vec3 sky = getSky(dirView, lightCol, zenithCol, horizonCol, true);
		sky += texture2D(colortex3, uv).rgb;          // stars + moon
#if defined NETHER
		sky = fogColor * 0.5;
#elif defined END
		float stars = smoothstep(0.985, 1.0, noise2D(gl_FragCoord.xy * 0.9));
		sky += vec3(0.7, 0.6, 1.0) * stars * 0.15;
#endif
#if defined VOLUMETRIC_CLOUDS && !defined NETHER && !defined END
		vec3 dirW = mat3(gbufferModelViewInverse) * dirView;
		if (dirW.y > 0.0 || cameraPosition.y > cloudBottom()) {
			vec4 cl = traceClouds(dirW, 100000.0, lightDirWorld(), lightCol, ambientCol, dither, CLOUD_STEPS, CLOUD_LIGHT_STEPS);
			sky = sky * cl.a + cl.rgb;
		}
#endif
		color = sky;
	} else {
		vec4 data1 = texture2D(colortex1, uv);
		vec4 data2 = texture2D(colortex2, uv);
		float material = floor(data1.z * 255.0 + 0.5);
		vec3 normal = decodeNormal(data1.xy);
		float skyLm = data1.w;
		vec3 feet0 = viewToFeet(view0);
		float NdotV = saturate(dot(-dirView, normal));

		if (abs(material - MAT_WATER) < 0.5) {
			// ===================================================== WATER
			vec3 wp0 = feet0 + cameraPosition;
			if (isEyeInWater == 1) {
				// looking up through the surface
				color *= vec3(0.75, 0.9, 0.92);
			} else {
				vec3 view1 = d1 >= 1.0 ? view0 + dirView * 40.0 : screenToView(uv, d1);
				float thick = length(view1 - view0);

				vec2 refrUV = uv;
	#ifdef WATER_REFRACTION
				vec3 upView = normalize(upPosition);
				vec2 off = (normal - upView * dot(normal, upView)).xy;
				off *= 0.05 * saturate(thick * 0.35) / (1.0 + length(view0) * 0.05);
				vec2 cand = clamp(uv + off, vec2(0.001), vec2(0.999));
				float cd1 = texture2D(depthtex1, cand).r;
				float cm = floor(texture2D(colortex1, cand).z * 255.0 + 0.5);
				if (abs(cm - MAT_WATER) < 0.5 && cd1 > d0) {
					refrUV = cand;
					view1 = cd1 >= 1.0 ? view0 + dirView * 40.0 : screenToView(refrUV, cd1);
					thick = length(view1 - view0);
				}
	#endif
				vec3 behind = texture2D(colortex0, refrUV).rgb;

				// absorption: dark, slightly green fjord water
				vec3 sigmaA = vec3(0.30, 0.085, 0.075) / WATER_CLARITY;
				float sigmaS = 0.045 / WATER_CLARITY;
				vec3 trans = exp(-(sigmaA + sigmaS) * thick);

				float surfShadow = sampleShadowSingle(feet0 + vec3(0.0, 0.1, 0.0));
				surfShadow *= smoothstep(0.4, 0.9, skyLm);
				vec3 deepCol = ambientCol * vec3(0.045, 0.085, 0.085) * (0.3 + 0.7 * skyLm)
				             + lightCol * vec3(0.02, 0.05, 0.05) * surfShadow;
				vec3 water = behind * trans + deepCol * (1.0 - trans);

				// reflection
				vec3 R = reflect(dirView, normal);
				float F = 0.02 + 0.98 * pow(1.0 - NdotV, 5.0);
				// rough, wind-blown water never acts as a perfect mirror of the bright sky
				vec3 refl = reflectionColor(view0, R, skyLm, dither, SSR_STEPS, false, 0.7);
				color = mix(water, refl, F);

				// sun / moon glints (only when the sun peeks through)
				vec3 lightDir = normalize(shadowLightPosition);
				float spec = pow(saturate(dot(R, lightDir)), 600.0) * 60.0 + pow(saturate(dot(R, lightDir)), 60.0) * 0.6;
				color += lightCol * spec * surfShadow * F * 4.0;

	#ifdef WATER_FOAM
				// shore foam: bands of foam rolling in towards the beach
				vec3 wp1 = viewToFeet(view1) + cameraPosition;
				float depthBelow = max(wp0.y - wp1.y, 0.0);
				float t = frameTimeCounter * WATER_WAVE_SPEED;
				float n1 = noise2D(wp0.xz * 1.7 + vec2(t * 0.35, t * 0.12));
				float n2 = noise2D(wp0.xz * 4.3 - vec2(t * 0.21, -t * 0.4));
				float pattern = n1 * 0.65 + n2 * 0.35;
				float shoreZone = 1.0 - smoothstep(0.0, 2.2, depthBelow);
				float edge = 1.0 - smoothstep(0.0, 0.35, depthBelow);
				float bands = sin(depthBelow * 5.5 - t * 2.4 + n1 * 5.0) * 0.5 + 0.5;
				float lines = smoothstep(0.72, 0.95, bands) * shoreZone;
				float crest = smoothstep(0.32, 0.5, waterHeight(wp0.xz) * waterWaveStrength() * 0.85) * (0.15 + rainStrength * 0.3 + getOvercast() * 0.15);
				float foam = edge * smoothstep(0.25, 0.6, pattern) + lines * smoothstep(0.4, 0.7, pattern) + crest * smoothstep(0.55, 0.8, pattern);
				foam = saturate(foam * FOAM_AMOUNT) * (1.0 - smoothstep(80.0, 200.0, length(view0)));
				vec3 foamCol = ambientCol * 0.95 * (0.3 + 0.7 * skyLm) + lightCol * 0.6 * surfShadow;
				color = mix(color, foamCol, foam * 0.92);
	#endif
			}
		} else if (abs(material - MAT_HAND) > 0.5 && abs(material - MAT_GLASS) > 0.5) {
			// ============================================ WET SURFACES
	#ifdef WET_REFLECTIONS
			float reflectivity = data2.x;
			if (reflectivity > 0.02 && d0 == d1) {
				vec3 R = reflect(dirView, normal);
				float F = 0.04 + 0.96 * pow(1.0 - NdotV, 5.0);
				vec3 refl = reflectionColor(view0, R, skyLm, dither, SSR_STEPS * 2 / 3, true, 1.0);
				color = mix(color, refl, saturate(F * reflectivity + reflectivity * 0.06));
			}
	#endif
		}

	#if defined CLOUDS_ON_TERRAIN && defined VOLUMETRIC_CLOUDS && !defined NETHER && !defined END
		// mountain peaks rising into the clouds
		if (abs(material - MAT_HAND) > 0.5) {
			float topY = max(feet0.y + cameraPosition.y, cameraPosition.y);
			if (topY > cloudBottom()) {
				vec3 dirW = normalize(feet0);
				vec4 cl = traceClouds(dirW, length(feet0), lightDirWorld(), lightCol, ambientCol, dither, CLOUD_STEPS / 2 + 4, 2);
				color = color * cl.a + cl.rgb;
			}
		}
	#endif
	}

	/* DRAWBUFFERS:0 */
	gl_FragData[0] = vec4(color, 1.0);
}
#endif
