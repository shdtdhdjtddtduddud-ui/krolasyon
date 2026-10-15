// Wind animation for grass, flowers, crops, leaves and vines.
// Block ids come from block.properties.
//
// The wind is layered like real wind on vegetation:
//   1. gusts  - slow pressure waves that travel across the landscape
//   2. sway   - whole plants / tree crowns lean and spring back with the gust
//   3. flutter- fast, small, per-leaf trembling
// Storms (rain / thunder) make every layer stronger and faster.

const float ID_WATER        = 10001.0;
const float ID_PLANT        = 10002.0;
const float ID_DOUBLE_LOWER = 10003.0;
const float ID_DOUBLE_UPPER = 10004.0;
const float ID_LEAVES       = 10005.0;
const float ID_VINE         = 10006.0;
const float ID_EMISSIVE     = 10008.0;
const float ID_LILY         = 10009.0;

bool idIs(float id, float target) { return abs(id - target) < 0.5; }

bool isFoliageId(float id) {
	return id > 10001.5 && id < 10006.5 || idIs(id, ID_LILY);
}

float stormAmount() {
	return saturate(rainStrength * 0.6 + thunderStrength * 0.8);
}

vec2 windDirection() {
	return normalize(vec2(0.8, 0.45));
}

// Strength of the gust at a position: waves of wind rolling over the land.
float windGust(vec3 worldPos, float t) {
	vec2 dir = windDirection();
	float along = dot(worldPos.xz, dir);
	float across = dot(worldPos.xz, vec2(-dir.y, dir.x));
	float speed = 6.0 + stormAmount() * 8.0;
	float g = sin(along * 0.045 - t * speed * 0.045) * 0.5 + 0.5;
	g *= sin(along * 0.11 + across * 0.07 - t * speed * 0.1) * 0.25 + 0.75;
	g = g * g * (3.0 - 2.0 * g);
	return mix(0.25, 1.0, g);
}

// Displacement of a plant point. bend = 0 at the root, 1 at the tip.
vec3 windVector(vec3 worldPos, float t, float bend, float flutterAmount, float swayFreq) {
	vec2 dir = windDirection();
	float storm = stormAmount();
	float gust = windGust(worldPos, t);
	float strength = (0.55 + storm * 1.6) * gust;

	// steady lean in the wind direction + springy sway around it
	float phase = dot(worldPos.xz, vec2(0.37, 0.29)) * 0.35;
	float sway = sin(t * swayFreq * (1.0 + storm * 0.6) + phase) * 0.6
	           + sin(t * swayFreq * 2.3 + phase * 1.7) * 0.25;
	vec2 lean = dir * (0.45 + sway * 0.55) * strength;
	// small sideways motion perpendicular to the wind
	lean += vec2(-dir.y, dir.x) * sin(t * swayFreq * 1.3 + phase * 2.1) * 0.25 * strength;

	// fast flutter, uncorrelated between neighbouring leaves
	float fp = dot(worldPos, vec3(2.1, 1.7, 2.9));
	vec3 flutter = vec3(sin(t * 9.0 + fp), sin(t * 11.0 + fp * 1.3), sin(t * 10.0 + fp * 0.7))
	             * flutterAmount * (0.35 + gust * 0.65) * (1.0 + storm);

	vec3 offset = vec3(lean.x, 0.0, lean.y) * bend;
	// bending a stem pulls its tip down a little (keeps the length)
	offset.y -= dot(lean, lean) * bend * 0.35;
	return offset + flutter * bend;
}

vec3 waveOffset(vec3 worldPos, float id, bool topVertex, float skyLight) {
	vec3 offset = vec3(0.0);
	float t = frameTimeCounter * WIND_SPEED;
	float exposure = smoothstep(0.2, 0.8, skyLight);   // no wind in caves
#ifdef WAVING_PLANTS
	if (idIs(id, ID_PLANT) && topVertex) {
		offset = windVector(worldPos, t, 1.0, 0.012, 2.2) * 0.16;
	} else if (idIs(id, ID_DOUBLE_LOWER) && topVertex) {
		offset = windVector(worldPos, t, 0.45, 0.01, 1.8) * 0.2;
	} else if (idIs(id, ID_DOUBLE_UPPER)) {
		offset = windVector(worldPos - vec3(0.0, float(topVertex), 0.0), t, topVertex ? 1.0 : 0.45, 0.012, 1.8) * 0.2;
	} else if (idIs(id, ID_LILY)) {
		float storm = stormAmount();
		offset.y = sin(t * (1.2 + storm) + worldPos.x * 0.7 + worldPos.z * 0.5) * (0.02 + storm * 0.04);
	}
#endif
#ifdef WAVING_LEAVES
	if (idIs(id, ID_LEAVES)) {
		// crown sway: a smooth, low-frequency motion field, so the leaves of
		// one tree lean together instead of jittering independently
		vec3 crown = windVector(worldPos * 0.15, t, 1.0, 0.0, 0.9) * 0.10;
		// branch motion, mid frequency
		vec3 branch = windVector(worldPos, t * 1.3, 1.0, 0.0, 1.7) * 0.035;
		// leaf flutter
		float fp = dot(worldPos, vec3(3.1, 2.3, 2.7));
		vec3 flutter = vec3(sin(t * 8.0 + fp), sin(t * 10.5 + fp * 1.4), sin(t * 9.3 + fp * 0.6))
		             * (0.012 + stormAmount() * 0.02) * windGust(worldPos, t);
		offset = crown + branch + flutter;
	} else if (idIs(id, ID_VINE)) {
		offset = windVector(worldPos, t, 1.0, 0.01, 1.4) * 0.06;
	}
#endif
	return offset * WAVING_STRENGTH * exposure;
}
