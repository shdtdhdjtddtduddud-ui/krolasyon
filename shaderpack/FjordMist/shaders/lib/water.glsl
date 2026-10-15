// Procedural fjord water: long wind-driven swells, chop and rain ripples.
// Needs common.glsl.

float waterWaveStrength() {
	// waves grow with wind and rain
	return WATER_WAVE_HEIGHT * (0.7 + getOvercast() * 0.5 + rainStrength * 0.4);
}

// Height field in blocks (roughly -1..1 scaled by strength)
float waterHeight(vec2 p) {
	float t = frameTimeCounter * WATER_WAVE_SPEED;
	vec2 wind = normalize(vec2(0.8, 0.6));
	mat2 rot = mat2(wind.x, -wind.y, wind.y, wind.x);
	vec2 q = rot * p;

	float h = 0.0;
	// long swells (stretched across the wind direction)
	h += (noise2D(q * vec2(0.055, 0.14) + vec2(t * 0.11, 0.0)) - 0.5) * 1.00;
	h += (noise2D(q * vec2(0.11, 0.24) + vec2(t * 0.17, t * 0.02)) - 0.5) * 0.55;
	// chop
	h += (noise2D(p * 0.42 + vec2(t * 0.28, -t * 0.15)) - 0.5) * 0.22;
	h += (noise2D(p * 0.85 + vec2(-t * 0.35, t * 0.31)) - 0.5) * 0.10;
	h += (noise2D(p * 1.90 + vec2(t * 0.52, t * 0.47)) - 0.5) * 0.045;
	return h;
}

// Cheap analytic version for vertex displacement (no texture reads)
float waterHeightVertex(vec2 p) {
	float t = frameTimeCounter * WATER_WAVE_SPEED;
	float h = sin(dot(p, vec2(0.21, 0.13)) + t * 1.1) * 0.5;
	h += sin(dot(p, vec2(-0.17, 0.29)) + t * 1.4) * 0.3;
	h += sin(dot(p, vec2(0.37, -0.31)) + t * 2.1) * 0.15;
	return h;
}

vec2 rainRipple(vec2 p, float t) {
	vec2 cell = floor(p);
	vec2 f = p - cell;
	float r = hash12(cell);
	vec2 center = 0.3 + 0.4 * hash22(cell + 7.31);
	float phase = fract(t * 0.9 + r);
	float radius = phase * 0.3;
	vec2 d = f - center;
	float dist = length(d);
	float ring = sin((dist - radius) * 40.0) * smoothstep(0.07, 0.0, abs(dist - radius)) * sq(1.0 - phase);
	return d / max(dist, 0.0001) * ring;
}

// World-space normal of the water surface at world position p (xz)
vec3 waterNormal(vec2 p, float distanceFade) {
	float strength = waterWaveStrength() * distanceFade;
	const float e = 0.08;
	float h0 = waterHeight(p);
	float hx = waterHeight(p + vec2(e, 0.0));
	float hz = waterHeight(p + vec2(0.0, e));
	vec2 grad = vec2(hx - h0, hz - h0) / e * strength * 0.35;

#ifdef RAIN_RIPPLES
	if (rainStrength > 0.01) {
		float t = frameTimeCounter;
		vec2 rip = rainRipple(p * 1.3, t) + rainRipple(p * 1.7 + 13.7, t * 1.13 + 0.4);
		grad += rip * rainStrength * 0.12 * sq(distanceFade);
	}
#endif

	return normalize(vec3(-grad.x, 1.0, -grad.y));
}
