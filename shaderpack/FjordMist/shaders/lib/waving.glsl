// Wind animation for grass, flowers, crops, leaves and vines.
// Block ids come from block.properties.

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

vec3 windVector(vec3 worldPos, float t) {
	float phase = dot(worldPos.xz, vec2(0.61, 0.37)) + worldPos.y * 0.2;
	float gust = 0.55 + 0.45 * sin(t * 0.35 + worldPos.x * 0.045 + worldPos.z * 0.031);
	gust *= 0.75 + 0.25 * sin(t * 1.3 + worldPos.z * 0.11);
	float x = sin(t * 1.9 + phase) * 0.55 + sin(t * 3.7 + phase * 1.7) * 0.22 + 0.35;
	float z = sin(t * 1.47 + phase * 1.3) * 0.40 + sin(t * 4.1 + phase * 0.7) * 0.12;
	float storm = 1.0 + rainStrength * 1.2;
	return vec3(x, 0.0, z) * gust * storm;
}

vec3 waveOffset(vec3 worldPos, float id, bool topVertex, float skyLight) {
	vec3 offset = vec3(0.0);
	float t = frameTimeCounter * WIND_SPEED;
	float exposure = smoothstep(0.2, 0.8, skyLight);   // no wind in caves
#ifdef WAVING_PLANTS
	if (idIs(id, ID_PLANT) && topVertex) {
		offset = windVector(worldPos, t) * 0.11;
	} else if (idIs(id, ID_DOUBLE_LOWER) && topVertex) {
		offset = windVector(worldPos, t) * 0.07;
	} else if (idIs(id, ID_DOUBLE_UPPER)) {
		offset = windVector(worldPos, t) * (topVertex ? 0.16 : 0.07);
	} else if (idIs(id, ID_LILY)) {
		offset.y = sin(t * 1.2 + worldPos.x * 0.7 + worldPos.z * 0.5) * 0.025;
	}
#endif
#ifdef WAVING_LEAVES
	if (idIs(id, ID_LEAVES)) {
		vec3 w = windVector(worldPos, t);
		offset = w * 0.045 + vec3(0.0, sin(t * 2.3 + dot(worldPos, vec3(0.9, 0.6, 1.1))) * 0.025, 0.0) * (0.5 + 0.5 * length(w));
	} else if (idIs(id, ID_VINE)) {
		offset = windVector(worldPos, t) * 0.05;
	}
#endif
	return offset * WAVING_STRENGTH * exposure;
}
