// Atmospheric haze, low lying mist over water, drifting mid-mountain mist
// bands and volumetric sun shafts. Needs common.glsl, sky.glsl, shadow.glsl.

#include "/lib/clouds.glsl"

float mistDensity(vec3 p, float oc, float dawn) {
	float t = frameTimeCounter * WIND_SPEED;
	// thin uniform haze: it is what makes distant mountains fade to gray-blue
	float atmo = 0.0009 * FOG_DENSITY * (0.3 + oc * 0.75 + rainStrength * 0.55);
	// thicker haze near sea level
	atmo *= 1.0 + 1.2 * exp(-max(p.y - SEA_LEVEL, 0.0) / 30.0);

	float mist = 0.0;
#ifdef MIST
	vec3 wind = vec3(t * 1.6, t * 0.05, t * 0.7);
	// low mist hugging the water surface
	float low = exp(-max(p.y - MIST_HEIGHT, 0.0) / 9.0) * smoothstep(MIST_HEIGHT - 30.0, MIST_HEIGHT - 10.0, p.y);
	if (low > 0.01) {
		float n = fbm3((p + wind) * vec3(0.03, 0.06, 0.03), 3);
		mist += low * smoothstep(0.4 - oc * 0.15, 0.78, n) * 0.02;
	}
	#ifdef MIST_BAND
	// clouds of mist drifting along the mountain sides
	float band = smoothstep(MIST_BAND_LOW, MIST_BAND_LOW + 18.0, p.y) * smoothstep(MIST_BAND_HIGH, MIST_BAND_HIGH - 30.0, p.y);
	if (band > 0.01) {
		float n2 = fbm3((p + wind * 1.4) * vec3(0.011, 0.028, 0.011), 4);
		mist += band * smoothstep(0.58 - oc * 0.12, 0.84, n2) * 0.022;
	}
	#endif
	mist *= MIST_DENSITY * (0.35 + oc * 0.55 + dawn * 0.6);
#endif
	return atmo + mist;
}

// Returns rgb = in-scatter, a = transmittance along the view ray.
vec4 traceFog(vec3 dirW, float dist, vec3 lightDirW, vec3 lightCol, vec3 ambientCol, float dither) {
	float oc = getOvercast();
	float e = getSunElevation();
	float dawn = exp(-sq((e - 0.08) * 5.0)) * (1.0 - rainStrength);

	float cosT = dot(dirW, lightDirW);
	float phase = (hgPhase(cosT, 0.72) * 0.6 + hgPhase(cosT, 0.0) * 0.4) * 4.0 * PI;
	vec3 sunScatter = lightCol * phase * VL_STRENGTH;
	vec3 ambScatter = ambientCol * 0.95;

	vec3 scatter = vec3(0.0);
	float T = 1.0;
	float cloudSh = 1.0;
	for (int i = 0; i < 32; i++) {
		if (i >= MIST_STEPS) break;
		// quadratic distribution: more samples close to the camera
		float a = (float(i) + dither) / float(MIST_STEPS);
		float b = (float(i) + 1.0 + dither) / float(MIST_STEPS);
		float t0 = a * a * dist;
		float t1 = min(b * b, 1.0) * dist;
		float ds = max(t1 - t0, 0.0);
		vec3 feet = dirW * t0;
		vec3 p = feet + cameraPosition;

		float d = mistDensity(p, oc, dawn);

		float sh = 1.0;
#if defined VOLUMETRIC_LIGHT && defined SHADOW_SAMPLING && !defined NETHER
		if (t0 < shadowDistance) sh = sampleShadowSingle(feet);
#endif
#ifdef VOLUMETRIC_LIGHT
		// light shafts break through gaps in the clouds (updated every 3 steps)
		if (mod(float(i), 3.0) < 0.5) cloudSh = cloudShadow(p, lightDirW);
		sh *= cloudSh;
#endif
		float stepT = exp(-d * ds);
		scatter += T * (sunScatter * sh + ambScatter) * (1.0 - stepT);
		T *= stepT;
		if (T < 0.01) break;
	}
	return vec4(scatter, T);
}
