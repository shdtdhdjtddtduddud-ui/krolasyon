// Living world: seasons, snow line, frost, moss, natural colour variation,
// sparkling minerals and damp caves. Needs common.glsl, celestial.glsl.

// Weights of the four seasons (x spring, y summer, z autumn, w winter).
// A year lasts 4 * SEASON_LENGTH Minecraft days; seasons blend smoothly.
vec4 seasonWeights() {
#ifdef SEASONS
	float day = float(worldDay) + float(worldTime) / 24000.0 + float(START_SEASON) * SEASON_LENGTH;
	float s = fract(day / (4.0 * SEASON_LENGTH)) * 4.0;
	vec4 w;
	for (int i = 0; i < 4; i++) {
		float d = abs(s - (float(i) + 0.5));
		d = min(d, 4.0 - d);
		float wi = saturate(1.0 - d);
		wi = wi * wi * (3.0 - 2.0 * wi);
		if (i == 0) w.x = wi; else if (i == 1) w.y = wi; else if (i == 2) w.z = wi; else w.w = wi;
	}
	return w / max(w.x + w.y + w.z + w.w, 0.0001);
#else
	return vec4(0.0, 1.0, 0.0, 0.0);
#endif
}

// Is precipitation falling as snow right now?
bool isSnowing() {
#if defined SEASONS && defined SNOW_COVER
	if (seasonWeights().w > 0.55) return true;
#endif
	return biome_precipitation == 2;
}

// How strongly a surface is tinted by the biome colour (grass, leaves, vines).
float biomeTint(vec4 glcolor) {
	return smoothstep(0.06, 0.18, glcolor.g - min(glcolor.r, glcolor.b));
}

// Seasonal colour of biome-tinted vegetation. Evergreens keep their needles.
vec3 seasonalFoliage(vec3 color, float tinted, vec3 wp, bool leaves, bool evergreen, vec4 season) {
	float l = luma(color);
	// spring: fresh, light green
	vec3 spring = color * vec3(0.92, 1.1, 0.8);
	// autumn: every tree picks its own colour (orange, red or gold)
	float h = hash12(floor(wp.xz / 5.0) + floor(wp.y / 7.0) * 3.7);
	vec3 autumnLeaf = h < 0.35 ? vec3(0.95, 0.36, 0.06) : (h < 0.6 ? vec3(0.72, 0.1, 0.05) : vec3(0.95, 0.68, 0.1));
	vec3 autumn = leaves ? autumnLeaf * l * 2.6 : mix(color, vec3(0.62, 0.5, 0.2) * l * 2.2, 0.65);
	// winter: dry, pale vegetation (bare-looking trees)
	vec3 winter = leaves ? vec3(0.36, 0.29, 0.22) * l * 2.0 : mix(vec3(l), vec3(0.5, 0.45, 0.32) * l * 2.0, 0.6);
	if (evergreen) {
		autumn = color * vec3(0.95, 0.95, 0.9);
		winter = color * vec3(0.85, 0.9, 0.9);
	}
	vec3 seasonal = spring * season.x + color * season.y + autumn * season.z + winter * season.w;
	return mix(color, seasonal, tinted);
}

// Glitter: tiny facets that flash as the view direction changes.
float sparkle(vec3 wp, vec3 viewDirW, float density) {
	vec3 g = floor(wp * 14.0);
	float h = hash13(g);
	if (h < 1.0 - density) return 0.0;
	vec3 f = fract(wp * 14.0) - 0.5;
	float point = smoothstep(0.35, 0.0, length(f));
	float facet = fract(h * 91.7 + dot(viewDirW, vec3(3.1, 4.7, 2.3)) * 1.6 + frameTimeCounter * 0.05);
	return point * smoothstep(0.9, 0.98, facet);
}
