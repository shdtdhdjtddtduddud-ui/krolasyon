// Sky and light colours. Needs common.glsl.
// Everything is driven by the sun elevation and by the overcast amount, so
// that a rainy day turns into the soft, gray, wet light of a northern fjord.

float getSunElevation() {
	return dot(normalize(sunPosition), normalize(upPosition));
}

// Colours of the main light (sun or moon), ambient sky light and sky gradient.
void getAtmosphereColors(out vec3 lightCol, out vec3 ambientCol, out vec3 zenithCol, out vec3 horizonCol) {
	float e = getSunElevation();
	float oc = getOvercast();

	float day    = smoothstep(-0.06, 0.28, e);
	float night  = 1.0 - smoothstep(-0.22, 0.02, e);
	float sunset = exp(-sq((e - 0.04) * 6.5)) * (1.0 - oc * 0.6);

#if defined NETHER
	lightCol = vec3(0.0);
	ambientCol = pow(fogColor, vec3(0.8)) * 0.6 + vec3(0.06, 0.03, 0.02);
	zenithCol = fogColor;
	horizonCol = fogColor;
	return;
#elif defined END
	lightCol = vec3(0.55, 0.45, 0.7) * 0.6;
	ambientCol = vec3(0.12, 0.09, 0.16);
	zenithCol = vec3(0.03, 0.02, 0.05);
	horizonCol = vec3(0.09, 0.06, 0.12);
	return;
#endif

	// ---- direct light
	vec3 sunNoon   = vec3(1.00, 0.94, 0.86) * 2.3;
	vec3 sunLow    = vec3(1.00, 0.52, 0.22) * 1.6;
	vec3 sunCol    = mix(sunNoon, sunLow, saturate(sunset * 1.3 + (1.0 - day) * 0.5));
	vec3 moonCol   = vec3(0.50, 0.62, 0.90) * 0.14;
	float crossFade = smoothstep(0.0, 0.07, abs(e));   // hide the sun/moon swap
	lightCol = (e > 0.0 ? sunCol : moonCol) * crossFade;
	lightCol *= 1.0 - oc * 0.86;
	lightCol *= SUN_BRIGHTNESS;

	// ---- sky gradient
	vec3 zenClear = vec3(0.16, 0.34, 0.78) * 1.1;
	vec3 horClear = vec3(0.58, 0.71, 0.88) * 1.2;
	vec3 zenGray  = vec3(0.44, 0.48, 0.52);
	vec3 horGray  = vec3(0.60, 0.64, 0.67);

	zenithCol  = mix(zenClear, zenGray, oc) * day;
	horizonCol = mix(horClear, horGray, oc) * day;
	horizonCol += vec3(1.10, 0.52, 0.24) * sunset * 0.9;
	zenithCol  += vec3(0.35, 0.25, 0.35) * sunset * 0.25;

	vec3 nightZen = mix(vec3(0.006, 0.010, 0.024), vec3(0.010, 0.012, 0.015), oc);
	vec3 nightHor = mix(vec3(0.014, 0.020, 0.036), vec3(0.016, 0.018, 0.021), oc);
	zenithCol  += nightZen * night;
	horizonCol += nightHor * night;

	// storms are darker
	float storm = 1.0 - rainStrength * 0.2 - thunderStrength * 0.15;
	zenithCol  *= storm;
	horizonCol *= storm;

	// ---- ambient
	ambientCol = mix(zenithCol, horizonCol, 0.45) * (0.85 + oc * 0.25);
	ambientCol = mix(ambientCol, vec3(luma(ambientCol)), oc * 0.35);
	ambientCol *= AMBIENT_BRIGHTNESS;
}

float hgPhase(float cosTheta, float g) {
	float g2 = g * g;
	return (1.0 - g2) / (4.0 * PI * pow(1.0 + g2 - 2.0 * g * cosTheta, 1.5));
}

// Sky radiance in a view-space direction.
vec3 getSky(vec3 dirView, vec3 lightCol, vec3 zenithCol, vec3 horizonCol, bool withSun) {
#if defined NETHER
	return fogColor * 0.6;
#endif
	vec3 upDir = normalize(upPosition);
	float up = dot(dirView, upDir);
	float oc = getOvercast();

	float h = 1.0 - saturate(up);
	vec3 sky = mix(zenithCol, horizonCol, pow(h, 2.6));
	// below the horizon: darker ground haze
	sky = mix(sky, horizonCol * 0.55, smoothstep(0.0, -0.25, up));

#if defined END
	return sky;
#endif

	vec3 sunDir = normalize(sunPosition);
	float cs = dot(dirView, sunDir);
	float e = getSunElevation();
	vec3 sunGlowCol = e > 0.0 ? lightCol / max(1.0 - oc * 0.86, 0.01) : vec3(0.0);
	float clearAmt = 1.0 - oc;

	// mie halo around the sun, fully diffused under clouds
	sky += sunGlowCol * (hgPhase(cs, 0.76) * 0.045 * clearAmt + pow(saturate(cs * 0.5 + 0.5), 4.0) * 0.07);

	if (withSun) {
		float disc = smoothstep(0.99955, 0.99975, cs);
		sky += sunGlowCol * disc * 22.0 * pow(clearAmt, 3.0);
	}

	// moon halo
	float cm = dot(dirView, normalize(moonPosition));
	sky += vec3(0.35, 0.45, 0.7) * pow(saturate(cm), 40.0) * 0.06 * (1.0 - smoothstep(-0.1, 0.1, e));

	return sky;
}
