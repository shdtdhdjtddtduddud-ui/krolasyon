// Night sky and sky phenomena: stars, the milky way, shooting stars,
// northern lights, rainbows and lightning flashes. Needs common.glsl.

uniform vec4 lightningBoltPosition;   // Iris: xyz = bolt position (player relative), w = 1 while a bolt exists
uniform int worldDay;
uniform int moonPhase;

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

// Rotates a world direction into the frame of the night sky, so stars and
// the milky way slowly wheel across the sky with the time of day.
vec3 celestialFrame(vec3 dirW) {
	float rot = radians(sunPathRotation);
	// undo the tilt of the sun path (around the east-west axis)
	vec3 d = vec3(dirW.x, dirW.y * cos(rot) + dirW.z * sin(rot), -dirW.y * sin(rot) + dirW.z * cos(rot));
	// spin around the celestial axis with the sun
	float a = sunAngle * 2.0 * PI;
	return vec3(d.x * cos(a) + d.y * sin(a), -d.x * sin(a) + d.y * cos(a), d.z);
}

// Milky way: a glowing band with a bright, warm galactic core, pink and
// violet star clouds, nebulae and dark dust lanes running through it.
vec3 getMilkyWay(vec3 d) {
	vec3 n  = normalize(vec3(0.42, 0.28, 0.86));          // band normal
	vec3 e1 = normalize(cross(n, vec3(0.0, 0.0, 1.0)));
	vec3 e2 = cross(n, e1);
	float lat = dot(d, n);
	float lon = atan(dot(d, e2), dot(d, e1));             // 0 = galactic core

	// the band wiggles and gets wider towards the core
	float coreAmt = exp(-lon * lon * 0.9);
	float width = mix(0.13, 0.24, coreAmt);
	float latW = lat + (fbm3(d * 3.0, 3) - 0.5) * 0.08;
	float band = exp(-sq(latW / width) * 1.6);
	float inner = exp(-sq(latW / (width * 0.45)) * 1.6);

	// clumpy star clouds
	// noise in band coordinates, stretched along the band
	vec3 bc = vec3(lon * 3.2, latW * 16.0, 0.0);
	float clouds = fbm3(bc + d * 4.0 + 2.0, 5);
	clouds = smoothstep(0.3, 0.75, clouds);
	float fine = fbm3(d * 28.0, 4);

	// dark dust lanes along the centre of the band
	float laneNoise = fbm3(vec3(lon * 4.5, latW * 34.0, 3.7) + d * 3.0, 5);
	float laneShape = exp(-sq((latW - sin(lon * 2.5) * 0.02) / (width * 0.32)) * 1.4);
	float lanes = smoothstep(0.42, 0.68, laneNoise) * laneShape;

	vec3 blue = vec3(0.30, 0.38, 1.0);
	vec3 violet = vec3(0.62, 0.38, 1.0);
	vec3 pink = vec3(1.0, 0.35, 0.72);
	vec3 warm = vec3(1.0, 0.62, 0.45);
	vec3 col = mix(blue, violet, clouds) * band * 0.7;
	col += mix(pink, warm, coreAmt * 0.7) * inner * (0.5 + 1.0 * clouds) * (0.45 + coreAmt * 1.5);
	col *= 0.55 + 0.9 * fine;
	col *= 1.0 - lanes * 0.88;

	// a few pink emission nebulae near the band
	float neb = smoothstep(0.7, 0.9, fbm3(d * 6.0 + 11.0, 4)) * smoothstep(0.0, 0.6, band);
	col += vec3(1.0, 0.2, 0.55) * neb * 0.9;

	return col * 0.11 * MILKY_WAY;
}

// One layer of stars on a 3D grid around the view sphere.
vec3 starLayer(vec3 d, float scale, float density, float brightness, float seed) {
	vec3 p = d * scale;
	vec3 cell = floor(p);
	vec3 f = p - cell;
	float h = hash13(cell + seed);
	if (h < 1.0 - density) return vec3(0.0);
	vec3 pos = 0.5 + (hash33(cell + seed) - 0.5) * 0.6;
	float r = length(f - pos);
	// brightness follows a power law: few bright stars, many faint ones
	float mag = pow(hash13(cell * 1.31 + seed), 6.0);
	// keep every star at least ~1.5 pixels wide, whatever the field of view
	float pixelAngle = 2.0 / (gbufferProjection[1][1] * viewHeight);
	float sigma = max(0.03 + mag * 0.05, pixelAngle * scale * 0.6);
	float star = exp(-r * r / (2.0 * sigma * sigma));
	// bright stars get a soft glow around them
	star += exp(-r * 14.0) * mag * 0.4;
	float twinkle = 0.7 + 0.3 * sin(frameTimeCounter * (1.5 + h * 7.0) + h * 400.0);
	float tempK = hash13(cell * 2.17 + seed);
	vec3 temp = tempK < 0.25 ? vec3(1.0, 0.72, 0.5) : (tempK < 0.75 ? vec3(1.0, 0.95, 0.9) : vec3(0.65, 0.78, 1.0));
	return temp * star * twinkle * brightness * (0.35 + mag * 6.0);
}

// Star field + milky way. dirW: world space view direction.
vec3 getStars(vec3 dirW) {
	vec3 d = celestialFrame(dirW);
	vec3 n = normalize(vec3(0.42, 0.28, 0.86));
	float inBand = exp(-sq(dot(d, n) / 0.16));
	float density = STAR_DENSITY;

	vec3 col = getMilkyWay(d);
	col += starLayer(d, 110.0, 0.014 * density, 1.0, 0.0);
	col += starLayer(d, 210.0, (0.03 + inBand * 0.08) * density, 0.4, 17.0);
	col += starLayer(d, 380.0, (0.06 + inBand * 0.2) * density, 0.17, 41.0);

	// atmospheric extinction: stars fade and redden towards the horizon
	float alt = saturate(dirW.y);
	col *= mix(vec3(0.45, 0.35, 0.3), vec3(1.0), smoothstep(0.0, 0.35, alt));
	return col;
}

// Moon with real phases: a lit sphere with maria and craters, a soft
// terminator, faint earthshine on the dark side and a halo.
// Returns rgb = moon radiance, also writes how much of the disc covers dirView.
vec3 getMoon(vec3 dirView, out float coverage) {
	coverage = 0.0;
#ifdef PROCEDURAL_MOON
	vec3 md = normalize(moonPosition);
	float radius = 0.022 * MOON_SIZE;
	float c = dot(dirView, md);
	if (c < cos(radius * 1.2)) return vec3(0.0);
	vec3 upV = normalize(upPosition);
	vec3 right = normalize(cross(md, upV));
	vec3 upM = cross(right, md);
	vec2 p = vec2(dot(dirView, right), dot(dirView, upM)) / radius;
	float r2 = dot(p, p);
	float px = 1.5 / (viewHeight * radius);               // ~1.5 px antialiasing
	coverage = smoothstep(1.0 + px, 1.0 - px, sqrt(r2));
	if (coverage <= 0.0) return vec3(0.0);
	vec3 n = vec3(p, sqrt(max(1.0 - r2, 0.0)));          // sphere normal (towards viewer = +z)

	// phase: 0 = full, 4 = new. Light comes from the side facing the sun.
	float phaseAngle = float(moonPhase) * PI * 0.25;
	vec3 L = normalize(vec3(sin(phaseAngle), 0.0, cos(phaseAngle)));
	float lit = smoothstep(-0.06, 0.12, dot(n, L));

	// surface: dark maria, bright highlands, craters
	vec3 sp = n * 2.2;
	float maria = smoothstep(0.44, 0.6, fbm3(sp + 3.0, 4));
	float craters = fbm3(n * 9.0, 4);
	float albedo = mix(1.0, 0.45, maria) * (0.75 + 0.5 * craters);
	// limb darkening
	albedo *= 0.75 + 0.25 * n.z;
	vec3 surface = vec3(1.0, 0.97, 0.9) * albedo;

	vec3 moon = surface * lit * 2.6 + surface * 0.004;   // + faint earthshine
	return moon * coverage;
#else
	return vec3(0.0);
#endif
}

float moonIllumination() {
	return 0.5 + 0.5 * cos(float(moonPhase) * PI * 0.25);
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
	return col / 14.0 * vis * horizonFade * 0.55;
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
