// Small glowing particles floating in 3D space around the player
// (fireflies, underwater marine snow). Needs common.glsl.
// Marches cells along the view ray; every cell may hold one particle that
// wanders around. Returns accumulated glow.
vec3 particleField(vec3 dirW, float maxDist, float cellSize, float density,
                   float radius, vec3 color, float minY, float maxY, float seed, float blink) {
	vec3 sum = vec3(0.0);
	float t = frameTimeCounter;
	vec3 lastCell = vec3(-99999.0);
	float stepLen = cellSize * 0.5;
	for (int i = 0; i < 40; i++) {
		float d = (float(i) + 0.5) * stepLen;
		if (d > maxDist + cellSize) break;
		vec3 p = cameraPosition + dirW * d;
		vec3 cell = floor(p / cellSize);
		if (cell == lastCell) continue;
		lastCell = cell;
		float h = hash12(cell.xz + cell.y * 17.31 + seed);
		if (h > density) continue;
		vec3 r3 = vec3(hash12(cell.xy + seed * 3.1), hash12(cell.yz + seed * 5.7), hash12(cell.zx + seed * 7.3));
		// wandering motion
		vec3 wander = vec3(sin(t * (0.4 + r3.x * 0.5) + h * 40.0), sin(t * (0.3 + r3.y * 0.4) + h * 70.0) * 0.6, cos(t * (0.35 + r3.z * 0.5) + h * 20.0)) * cellSize * 0.25;
		vec3 pos = (cell + 0.25 + 0.5 * r3) * cellSize + wander;
		if (pos.y < minY || pos.y > maxY) continue;
		vec3 rel = pos - cameraPosition;
		float along = dot(rel, dirW);
		if (along < 0.2 || along > maxDist) continue;
		float dist2 = dot(rel, rel) - along * along;
		// never smaller than about one pixel
		float pix = along * 2.0 / (gbufferProjection[1][1] * viewHeight);
		float rr = max(radius, pix);
		float glow = exp(-dist2 / (rr * rr)) * (radius * radius) / (rr * rr);
		glow += exp(-sqrt(max(dist2, 0.0)) / (radius * 6.0)) * 0.04;      // soft halo
		// blinking (fireflies flash in a slow rhythm)
		float pulse = mix(1.0, smoothstep(0.55, 1.0, sin(t * (1.2 + r3.x) + h * 60.0) * 0.5 + 0.5), blink);
		sum += color * glow * pulse;
	}
	return sum;
}
