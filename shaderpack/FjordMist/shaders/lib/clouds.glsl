// Ray-marched volumetric cloud layer. Needs common.glsl and sky.glsl (hgPhase).
// The layer is low enough that tall mountains poke into it, like in a fjord.

float cloudBottom() { return CLOUD_HEIGHT - rainStrength * 25.0; }
float cloudTop()    { return cloudBottom() + CLOUD_THICKNESS; }

float cloudCoverage() {
	float cov = mix(CLOUD_COVERAGE, 0.96, getOvercast() * 0.92);
#ifdef CLEAR_NIGHTS
	// dry nights are usually clear: the cloud deck breaks up after sunset
	float night = 1.0 - smoothstep(-0.2, 0.02, dot(normalize(sunPosition), normalize(upPosition)));
	cov = mix(cov, cov * 0.45, night * (1.0 - rainStrength));
#endif
	return cov;
}

float cloudDensity(vec3 p, float coverage, int octaves) {
	float bot = cloudBottom();
	float hf = (p.y - bot) / CLOUD_THICKNESS;
	if (hf <= 0.0 || hf >= 1.0) return 0.0;

	float t = frameTimeCounter * WIND_SPEED;
	vec3 q = (p + vec3(t * 3.0, 0.0, t * 1.4)) * (0.0042 / CLOUD_SCALE);
	q.y *= 1.6;

	// large scale weather pattern so the sky has gaps and towers
	float weather = noise2D(p.xz * (0.00045 / CLOUD_SCALE) + t * 0.0012);
	float n = fbm3(q, octaves);
	n = saturate((n - 0.22) / 0.56);

	// rounded bottom, wispy top
	float profile = smoothstep(0.0, 0.18, hf) * smoothstep(1.0, 0.4, hf);
	float cov = saturate(coverage + (weather - 0.5) * 0.45);
	float d = n * profile - (1.0 - cov);
	return saturate(d * 2.8);
}

// Returns rgb = in-scattered light, a = transmittance.
vec4 traceClouds(vec3 dirW, float maxDist, vec3 lightDirW, vec3 lightCol, vec3 ambientCol, float dither, int steps, int lightSteps) {
	float bot = cloudBottom();
	float top = cloudTop();
	float camY = cameraPosition.y;
	if (abs(dirW.y) < 0.0005) dirW.y = 0.0005;

	float tb = (bot - camY) / dirW.y;
	float tt = (top - camY) / dirW.y;
	float tNear, tFar;
	if (camY > bot && camY < top) {
		tNear = 0.0;
		tFar = max(tb, tt);
	} else {
		tNear = min(tb, tt);
		tFar  = max(tb, tt);
		if (tFar < 0.0) return vec4(0.0, 0.0, 0.0, 1.0);
		tNear = max(tNear, 0.0);
	}
	tFar = min(tFar, min(maxDist, tNear + 2500.0));
	if (tFar <= tNear || tNear > 9000.0) return vec4(0.0, 0.0, 0.0, 1.0);

	float coverage = cloudCoverage();
	float stepLen = (tFar - tNear) / float(steps);
	vec3 pos = cameraPosition + dirW * (tNear + stepLen * dither);

	float cosT = dot(dirW, lightDirW);
	float phase = mix(hgPhase(cosT, 0.65), hgPhase(cosT, -0.15), 0.35) * 4.0 * PI;
	float lightStepLen = CLOUD_THICKNESS / float(lightSteps) * 0.5;
	const float sigma = 0.055;
	float stormDark = 1.0 - rainStrength * 0.35;

	vec3 scatter = vec3(0.0);
	float T = 1.0;
	for (int i = 0; i < 64; i++) {
		if (i >= steps) break;
		float d = cloudDensity(pos, coverage, 4);
		if (d > 0.002) {
			float od = 0.0;
			for (int j = 0; j < 8; j++) {
				if (j >= lightSteps) break;
				vec3 lp = pos + lightDirW * (float(j) + 0.5) * lightStepLen;
				od += cloudDensity(lp, coverage, 2) * lightStepLen;
			}
			float hf = (pos.y - bot) / CLOUD_THICKNESS;
			float beer = exp(-od * sigma) + 0.25 * exp(-od * sigma * 0.2);
			float powder = 1.0 - exp(-d * 6.0);
			vec3 sunL = lightCol * beer * phase * mix(0.6, 1.0, powder) * 0.7;
			vec3 ambL = ambientCol * mix(0.45, 1.15, hf) * stormDark;
			float stepT = exp(-d * sigma * stepLen);
			scatter += T * (sunL + ambL) * (1.0 - stepT);
			T *= stepT;
			if (T < 0.015) break;
		}
		pos += dirW * stepLen;
	}

	// distant clouds dissolve into the haze of the horizon
	float fade = exp(-tNear * 0.00028);
	return vec4(scatter * fade, mix(1.0, T, fade));
}
