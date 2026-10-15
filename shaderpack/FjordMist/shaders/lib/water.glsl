// Procedural fjord water: long wind-driven swells, chop, storm waves and
// physically inspired raindrop ripples. Needs common.glsl.

float stormWaves() {
	return saturate(rainStrength * 0.5 + thunderStrength);
}

float waterWaveStrength() {
	// calm on clear days, rough under rain, big rolling waves in thunderstorms
	return WATER_WAVE_HEIGHT * (0.7 + getOvercast() * 0.4 + rainStrength * 0.4 + thunderStrength * 1.8);
}

// Height field in blocks (roughly -1..1 scaled by strength)
float waterHeight(vec2 p) {
	float storm = stormWaves();
	float t = frameTimeCounter * WATER_WAVE_SPEED * (1.0 + storm * 0.7);
	vec2 wind = normalize(vec2(0.8, 0.6));
	mat2 rot = mat2(wind.x, -wind.y, wind.y, wind.x);
	vec2 q = rot * p;

	float h = 0.0;
	// long swells (stretched across the wind direction); storms make them longer
	float swellScale = mix(1.0, 0.7, storm);
	h += (noise2D(q * vec2(0.055, 0.14) * swellScale + vec2(t * 0.11, 0.0)) - 0.5) * 1.00;
	h += (noise2D(q * vec2(0.11, 0.24) * swellScale + vec2(t * 0.17, t * 0.02)) - 0.5) * 0.55;
	// sharper crests in storms
	float crest = noise2D(q * vec2(0.08, 0.2) + vec2(t * 0.14, 0.3));
	h += (1.0 - abs(crest * 2.0 - 1.0) - 0.5) * 0.6 * storm;
	// chop
	h += (noise2D(p * 0.42 + vec2(t * 0.28, -t * 0.15)) - 0.5) * (0.22 + storm * 0.15);
	h += (noise2D(p * 0.85 + vec2(-t * 0.35, t * 0.31)) - 0.5) * (0.10 + storm * 0.08);
	h += (noise2D(p * 1.90 + vec2(t * 0.52, t * 0.47)) - 0.5) * 0.045;
	return h;
}

// Cheap analytic version for vertex displacement (no texture reads)
float waterHeightVertex(vec2 p) {
	float storm = stormWaves();
	float t = frameTimeCounter * WATER_WAVE_SPEED * (1.0 + storm * 0.7);
	float h = sin(dot(p, vec2(0.21, 0.13)) * mix(1.0, 0.6, storm) + t * 1.1) * 0.5;
	h += sin(dot(p, vec2(-0.17, 0.29)) + t * 1.4) * 0.3;
	h += sin(dot(p, vec2(0.37, -0.31)) + t * 2.1) * 0.15;
	return h;
}

// One layer of raindrop impacts. Every cell receives one drop per cycle at a
// random moment. A drop makes a short splash, then a packet of capillary
// waves expands outwards (~0.3 blocks/s), losing energy as it spreads.
// Returns xy = surface slope, z = splash intensity.
vec3 rainDropLayer(vec2 p, float t) {
	vec2 cell = floor(p);
	vec3 acc = vec3(0.0);
	for (int i = -1; i <= 1; i++) {
		for (int j = -1; j <= 1; j++) {
			vec2 c = cell + vec2(float(i), float(j));
			vec2 h = hash22(c);
			float r = hash12(c + 3.7);
			vec2 center = c + 0.15 + 0.7 * h;
			float life = fract(t * (0.75 + r * 0.3) + r);   // 0..1 over ~1.2 s
			vec2 d = p - center;
			float dist = length(d);
			float radius = life * 0.85;
			float x = dist - radius;
			// wave packet: a few crests trailing the leading front
			float packet = smoothstep(-0.32, -0.03, x) * smoothstep(0.03, -0.01, x);
			float energy = sq(1.0 - life) / sqrt(0.15 + radius * 4.0);
			float slope = cos(x * 42.0) * packet * energy;
			acc.xy += d / max(dist, 0.0001) * slope;
			// impact splash in the first moments
			acc.z += smoothstep(0.08, 0.0, dist) * smoothstep(0.08, 0.0, life);
		}
	}
	return acc;
}

vec3 rainDrops(vec2 p, float rain) {
	float t = frameTimeCounter;
	vec3 r = rainDropLayer(p * 1.6, t);
	if (rain > 0.35) r += rainDropLayer(p * 2.1 + 17.3, t * 1.07 + 0.37) * saturate((rain - 0.35) * 3.0);
	if (rain > 0.7)  r += rainDropLayer(p * 2.7 + 41.9, t * 0.93 + 0.71) * saturate((rain - 0.7) * 3.0);
	return r;
}

// World-space normal of the water surface at world position p (xz)
vec3 waterNormal(vec2 p, float distanceFade, float rippleFade) {
	float strength = waterWaveStrength() * distanceFade;
	const float e = 0.08;
	float h0 = waterHeight(p);
	float hx = waterHeight(p + vec2(e, 0.0));
	float hz = waterHeight(p + vec2(0.0, e));
	vec2 grad = vec2(hx - h0, hz - h0) / e * strength * 0.35;

#ifdef RAIN_RIPPLES
	if (rainStrength > 0.01 && rippleFade > 0.01 && biome_precipitation != 2) {
		vec3 drops = rainDrops(p, rainStrength);
		grad += drops.xy * rainStrength * 0.22 * rippleFade;
	}
#endif

	return normalize(vec3(-grad.x, 1.0, -grad.y));
}

// Caustics: bright, wobbling light lines that waves focus onto the ground
// under shallow water.
float waterCaustics(vec2 p) {
	float t = frameTimeCounter * 0.55 * WATER_WAVE_SPEED;
	float a = noise2D(p * 0.95 + vec2(t * 0.42, t * 0.23));
	float b = noise2D(p * 1.15 + vec2(-t * 0.31, t * 0.37) + 7.3);
	float c1 = pow(saturate(1.0 - abs(a - b) * 3.2), 7.0);
	float a2 = noise2D(p * 2.1 + vec2(t * 0.6, -t * 0.45) + 3.1);
	float b2 = noise2D(p * 2.4 + vec2(-t * 0.52, -t * 0.3) + 11.7);
	float c2 = pow(saturate(1.0 - abs(a2 - b2) * 3.0), 6.0);
	return c1 * 0.75 + c2 * 0.45;
}
