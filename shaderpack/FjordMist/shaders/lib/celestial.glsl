// Night sky and sky phenomena: stars, the milky way, shooting stars,
// northern lights, rainbows and lightning flashes. Needs common.glsl.

uniform vec4 lightningBoltPosition;   // Iris: xyz = bolt position (player relative), w = 1 while a bolt exists
uniform int worldDay;

// Brightness of the current lightning flash (0 = none). The bolt flickers
// like real return strokes and lights up clouds, sky and ground.
float lightningFlash() {
#ifdef LIGHTNING_FLASH
	if (lightningBoltPosition.w < 0.5) return 0.0;
	float d = length(lightningBoltPosition.xz);
	float strobe = step(0.35, hash12(vec2(floor(frameTimeCounter * 16.0), 3.1)));
	return (0.25 + 0.75 * exp(-d / 180.0)) * mix(0.35, 1.0, strobe);
#else
	return 0.0;
#endif
}

float hash13(vec3 p) {
	p = fract(p * 0.1031);
	p += dot(p, p.zyx + 31.32);
	return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
	p = fract(p * vec3(0.1031, 0.1030, 0.0973));
	p += dot(p, p.yxz + 33.33);
	return fract((p.xxy + p.yxx) * p.zyx);
}

// Twinkling star field + milky way. dirW: world space view direction.
vec3 getStars(vec3 dirW) {
	vec3 col = vec3(0.0);
	float t = frameTimeCounter;
	// milky way: a faint glowing band along a great circle
	vec3 bandNormal = normalize(vec3(0.35, 0.45, 0.82));
	float bandDist = dot(dirW, bandNormal);
	float band = exp(-bandDist * bandDist * 22.0);
	float dust = fbm3(dirW * 7.0, 4);
	float lanes = smoothstep(0.35, 0.65, fbm3(dirW * 13.0 + 4.0, 3));
	col += vec3(0.55, 0.6, 0.85) * band * smoothstep(0.35, 0.75, dust) * mix(1.0, 0.35, lanes) * 0.012;

	for (int layer = 0; layer < 2; layer++) {
		float scale = layer == 0 ? 160.0 : 320.0;
		vec3 p = dirW * scale;
		vec3 cell = floor(p);
		vec3 f = p - cell;
		float h = hash13(cell + float(layer) * 19.7);
		// more stars inside the milky way
		float threshold = layer == 0 ? 0.9955 : 0.990 - band * 0.006;
		if (h > threshold) {
			vec3 pos = 0.5 + (hash33(cell) - 0.5) * 0.5;
			float d = length(f - pos);
			float size = layer == 0 ? 0.14 : 0.09;
			float twinkle = 0.65 + 0.35 * sin(t * (2.0 + h * 9.0) + h * 300.0);
			vec3 temp = mix(vec3(1.0, 0.78, 0.55), vec3(0.7, 0.82, 1.0), hash13(cell * 1.7));
			float bright = (layer == 0 ? 0.25 : 0.08) * (0.4 + 1.6 * fract(h * 371.0));
			col += temp * smoothstep(size, 0.0, d) * twinkle * bright;
		}
	}
	return col;
}

// A shooting star every few seconds (not every window gets one).
vec3 getShootingStar(vec3 dirW) {
#ifdef SHOOTING_STARS
	const float period = 7.0;
	float t = frameTimeCounter / period;
	float win = floor(t);
	float r = hash12(vec2(win, 1.7));
	if (r > 0.45) return vec3(0.0);
	float age = fract(t) * period;           // seconds since the window started
	if (age > 0.9) return vec3(0.0);
	vec3 h = hash33(vec3(win, 3.3, 9.1));
	vec3 start = normalize(vec3(h.x * 2.0 - 1.0, 0.35 + h.y * 0.5, h.z * 2.0 - 1.0));
	vec3 side = normalize(cross(start, vec3(0.0, 1.0, 0.0)));
	vec3 dir = normalize(side * (hash12(vec2(win, 8.8)) > 0.5 ? 1.0 : -1.0) - vec3(0.0, 0.35, 0.0));
	vec3 head = normalize(start + dir * age * 0.35);
	vec3 tail = normalize(start + dir * max(age - 0.18, 0.0) * 0.35);
	// distance from the view direction to the head-tail segment
	vec3 ab = head - tail;
	float k = saturate(dot(dirW - tail, ab) / max(dot(ab, ab), 1e-6));
	float d = length(dirW - (tail + ab * k));
	float fade = (1.0 - age / 0.9) * smoothstep(0.0, 0.08, age);
	return vec3(0.9, 0.95, 1.0) * smoothstep(0.0025, 0.0, d) * k * fade * 1.5;
#else
	return vec3(0.0);
#endif
}

// Northern lights: folded curtains of green light with purple tops.
// AURORA 1 = on some nights, 2 = every clear night.
vec3 getAurora(vec3 dirW, float night) {
#if AURORA > 0
	if (dirW.y < 0.03 || night < 0.01) return vec3(0.0);
	#if AURORA == 1
	float tonight = hash12(vec2(float(worldDay), 5.1));
	if (tonight < 0.55) return vec3(0.0);
	float nightStrength = smoothstep(0.55, 0.75, tonight);
	#else
	float nightStrength = 1.0;
	#endif
	float vis = night * nightStrength * (1.0 - rainStrength) * (1.0 - smoothstep(0.55, 0.9, getOvercast())) * AURORA_STRENGTH;
	if (vis < 0.001) return vec3(0.0);

	float t = frameTimeCounter * 0.03;
	vec3 col = vec3(0.0);
	float jitter = 0.5;
	#ifdef FSH
	jitter = interleavedGradient(gl_FragCoord.xy);   // hides the layer steps
	#endif
	for (int i = 0; i < 14; i++) {
		float h = (float(i) + jitter) / 14.0;
		// curtains hang between two altitudes; march up through them
		vec2 p = dirW.xz / dirW.y * (1.0 + h * 0.55) * 1.6;
		float fold = noise2D(p * 0.55 + vec2(t, -t * 0.6)) * 4.0;
		float s = p.y + p.x * 0.35 + fold;
		float ribbon = exp(-sq(fract(s * 0.18) - 0.5) * 70.0);
		float rays = 0.55 + 0.45 * noise2D(vec2(s * 6.0, t * 8.0 + h));
		vec3 c = mix(vec3(0.08, 1.0, 0.45), vec3(0.55, 0.18, 0.9), smoothstep(0.35, 1.0, h));
		col += c * ribbon * rays * (1.0 - h) ;
	}
	float horizonFade = smoothstep(0.03, 0.25, dirW.y);
	return col / 14.0 * vis * horizonFade * 0.9;
#else
	return vec3(0.0);
#endif
}

vec3 spectrum(float x) {
	// x: 0 = violet, 1 = red
	float h = (1.0 - saturate(x)) * 0.75;
	return saturate(vec3(abs(h * 6.0 - 3.0) - 1.0, 2.0 - abs(h * 6.0 - 2.0), 2.0 - abs(h * 6.0 - 4.0)));
}

// Rainbow opposite the sun after (or at the end of) rain, only when the sun is
// below 42 degrees and actually shining. dirView: view space direction.
vec3 getRainbow(vec3 dirView, vec3 dirW, vec3 brightness) {
#ifdef RAINBOW
	float sunElev = dot(normalize(sunPosition), normalize(upPosition));
	float amt = saturate(wetness * 1.4 - rainStrength * 2.2) * smoothstep(0.0, 0.08, sunElev) * smoothstep(0.7, 0.55, sunElev);
	amt *= 1.0 - smoothstep(0.6, 0.95, getOvercast());
	if (amt < 0.001 || dirW.y < -0.02) return vec3(0.0);
	float c = clamp(dot(dirView, -normalize(sunPosition)), -1.0, 1.0);
	float theta = degrees(acos(c));
	float x1 = (theta - 40.4) / 2.0;                    // primary, red outside
	float b1 = smoothstep(-0.15, 0.1, x1) * smoothstep(1.15, 0.9, x1);
	float x2 = 1.0 - (theta - 50.2) / 3.2;              // secondary, reversed
	float b2 = smoothstep(-0.15, 0.1, x2) * smoothstep(1.15, 0.9, x2);
	vec3 bow = spectrum(x1) * b1 + spectrum(x2) * b2 * 0.4;
	return bow * amt * brightness * 0.35 * smoothstep(-0.02, 0.06, dirW.y);
#else
	return vec3(0.0);
#endif
}
