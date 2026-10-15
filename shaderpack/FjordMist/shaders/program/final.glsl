// final: bloom, exposure, tonemapping, moody fjord colour grading,
// rain drops on the lens, vignette and film grain.

/*
const bool colortex0MipmapEnabled = true;
*/

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/sky.glsl"

varying vec2 texcoord;
varying float exposure;
varying vec3 flareCol;

#ifdef VSH
void main() {
	texcoord = gl_MultiTexCoord0.xy;
	gl_Position = ftransform();

	vec3 lc, amb, zen, hor;
	getAtmosphereColors(lc, amb, zen, hor);
	float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
	float e = EXPOSURE;
#ifdef AUTO_EXPOSURE
	// the eye must not adapt to lightning flashes, or they would vanish
	float flashLum = lightningFlash() * luma(vec3(0.55, 0.6, 0.9)) * 1.4;
	float outdoorLum = max(luma(amb) - flashLum, 0.0) + luma(lc) * 0.25;
	float outdoorExp = clamp(pow(0.55 / (outdoorLum + 0.03), 0.55), 0.9, 4.0);
	float caveExp = 2.6;
	e *= mix(caveExp, outdoorExp, smoothstep(0.0, 0.9, eyeSky));
#endif
	exposure = e;
	// flare colour follows the sun; fades with clouds, rain and at night
	float sunUp = smoothstep(0.0, 0.1, getSunElevation());
	flareCol = lc / max(1.0 - getOvercast() * 0.86, 0.05) * sunUp * (1.0 - rainStrength) * LENS_FLARE_STRENGTH;
}
#endif

#ifdef FSH
uniform sampler2D colortex0;
uniform float lensWet;    // custom uniform: fades out over a few seconds after leaving water

// Water running down the lens after you come out of the water.
vec2 lensWater(vec2 uv, out float mask) {
	mask = 0.0;
#ifdef LENS_WATER
	float amount = isEyeInWater == 0 ? lensWet : 0.0;
	if (amount < 0.01) return vec2(0.0);
	vec2 aspect = vec2(viewWidth / viewHeight, 1.0);
	vec2 offset = vec2(0.0);
	float t = frameTimeCounter;
	for (int layer = 0; layer < 3; layer++) {
		float scale = 5.0 + float(layer) * 4.0;
		vec2 p = uv * aspect * scale + float(layer) * 7.7;
		float col = floor(p.x);
		float speed = 0.25 + hash12(vec2(col, float(layer))) * 0.5;
		p.y += t * speed;                                  // drops slide down
		vec2 cell = floor(p);
		vec2 f = fract(p) - 0.5;
		float h = hash12(cell + float(layer) * 11.3);
		vec2 c = (hash22(cell) - 0.5) * vec2(0.5, 0.3);
		vec2 dv = f - c;
		float r = 0.12 + h * 0.15;
		float drop = smoothstep(r, r * 0.2, length(dv * vec2(1.0, 0.8))) * step(0.55, h);
		// faint wet trail left behind the sliding drop
		float trail = smoothstep(0.03, 0.0, abs(dv.x)) * smoothstep(-0.45, 0.0, dv.y) * step(0.0, -dv.y + r) * step(0.55, h) * 0.12;
		offset += dv * drop * 0.2 / scale;
		mask = max(mask, max(drop, trail * smoothstep(0.0, 0.5, f.y + 0.5)));
	}
	mask *= amount;
	return offset * amount;
#else
	return vec2(0.0);
#endif
}

vec3 acesFitted(vec3 v) {
	v *= 0.6;
	const float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
	return saturate((v * (a * v + b)) / (v * (c * v + d) + e));
}

vec2 lensDrops(vec2 uv, out float dropMask) {
	dropMask = 0.0;
#ifdef RAIN_ON_LENS
	float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
	float amount = rainStrength * smoothstep(0.7, 1.0, eyeSky) * float(isEyeInWater == 0);
	if (amount < 0.01) return vec2(0.0);
	vec2 aspect = vec2(viewWidth / viewHeight, 1.0);
	vec2 offset = vec2(0.0);
	for (int layer = 0; layer < 2; layer++) {
		float scale = layer == 0 ? 7.0 : 13.0;
		vec2 p = uv * aspect * scale + float(layer) * 17.3;
		vec2 cell = floor(p);
		vec2 f = fract(p) - 0.5;
		float h = hash12(cell + float(layer) * 3.1);
		float life = fract(frameTimeCounter * (0.12 + h * 0.1) + h * 7.0);
		vec2 center = (hash22(cell + floor(frameTimeCounter * (0.12 + h * 0.1) + h * 7.0)) - 0.5) * 0.6;
		// drops slowly slide down
		center.y += life * 0.25;
		vec2 dv = f - center;
		float r = (0.08 + h * 0.12) * (1.0 - life * 0.6);
		float d = length(dv / vec2(1.0, 1.25));
		float m = smoothstep(r, r * 0.6, d) * step(0.45, h) * (1.0 - life);
		offset += dv * m * 0.06 / scale;
		dropMask = max(dropMask, m);
	}
	dropMask *= amount;
	return offset * amount;
#else
	return vec2(0.0);
#endif
}

vec3 lensFlare(vec2 uv) {
#ifdef LENS_FLARE
	if (sunPosition.z > 0.0 || dot(flareCol, flareCol) < 1e-6) return vec3(0.0);
	vec2 sp = viewToScreen(sunPosition).xy;
	if (any(lessThan(sp, vec2(-0.2))) || any(greaterThan(sp, vec2(1.2)))) return vec3(0.0);
	// is the sun actually visible? (terrain or thick clouds hide its bright disc)
	vec2 spc = clamp(sp, vec2(0.002), vec2(0.998));
	float sunLum = luma(texture2D(colortex0, spc, 1.5).rgb);
	float vis = smoothstep(3.0, 12.0, sunLum) * (1.0 - smoothstep(0.85, 1.2, max(abs(sp.x - 0.5), abs(sp.y - 0.5)) * 2.0));
	if (vis < 0.001) return vec3(0.0);

	vec2 aspect = vec2(viewWidth / viewHeight, 1.0);
	vec2 toCenter = vec2(0.5) - sp;
	vec3 f = vec3(0.0);
	// soft glow and anamorphic streak around the sun
	vec2 ds = (uv - sp) * aspect;
	f += exp(-length(ds) * 9.0) * 0.06;
	f += exp(-abs(ds.y) * 220.0) * exp(-abs(ds.x) * 2.0) * 0.06 * vec3(0.7, 0.85, 1.0);
	// ghosts mirrored through the screen centre
	const float ghostPos[5] = float[5](0.45, 0.8, 1.15, 1.45, 1.9);
	const float ghostSize[5] = float[5](0.035, 0.07, 0.025, 0.11, 0.05);
	const vec3 ghostCol[5] = vec3[5](vec3(1.0, 0.6, 0.3), vec3(0.4, 0.8, 1.0), vec3(1.0, 0.9, 0.5), vec3(0.5, 1.0, 0.6), vec3(0.8, 0.5, 1.0));
	for (int i = 0; i < 5; i++) {
		vec2 gp = sp + toCenter * ghostPos[i];
		float d = length((uv - gp) * aspect);
		float disc = smoothstep(ghostSize[i], ghostSize[i] * 0.6, d);
		float ring = smoothstep(ghostSize[i] * 0.12, 0.0, abs(d - ghostSize[i])) * 0.6;
		f += ghostCol[i] * (disc * 0.5 + ring) * 0.03;
	}
	return f * flareCol * vis;
#else
	return vec3(0.0);
#endif
}

void main() {
	float dropMask;
	vec2 uv = texcoord + lensDrops(texcoord, dropMask);
	float wetMask;
	uv += lensWater(texcoord, wetMask);
	dropMask = max(dropMask, wetMask);
	vec3 color = texture2D(colortex0, uv).rgb;

#ifdef BLOOM
	vec3 bloom = vec3(0.0);
	bloom += texture2D(colortex0, uv, 2.0).rgb * 0.30;
	bloom += texture2D(colortex0, uv, 3.5).rgb * 0.25;
	bloom += texture2D(colortex0, uv, 5.0).rgb * 0.22;
	bloom += texture2D(colortex0, uv, 6.5).rgb * 0.23;
	// a lot of soft glow through mist and rain
	float bloomAmt = 0.045 * (1.0 + getOvercast() * 0.8 + rainStrength) * BLOOM_STRENGTH;
	color = mix(color, bloom, bloomAmt);
#endif
	// lens drops blur what is behind them
	color = mix(color, texture2D(colortex0, uv, 3.0).rgb * 1.08, dropMask * 0.6);
	// water films on the lens catch a little light at their edges
	color *= 1.0 + wetMask * (1.0 - wetMask) * 0.35;

	color += lensFlare(texcoord);
	color *= exposure;

#ifdef PURKINJE
	// night vision: in very low light the eye switches to rods, colours fade
	// and the world turns a dim blue
	float sceneLum = luma(color);
	float rods = smoothstep(0.06, 0.004, sceneLum) * PURKINJE_STRENGTH;
	color = mix(color, vec3(sceneLum) * vec3(0.62, 0.8, 1.15), rods * 0.65);
#endif
	color = acesFitted(color);

#ifdef FJORD_GRADE
	// cool, slightly green shadows; neutral misty highlights
	float l = luma(color);
	vec3 shadowTint = vec3(0.90, 1.00, 1.03);
	vec3 highTint   = vec3(1.00, 1.00, 0.98);
	color *= mix(shadowTint, highTint, smoothstep(0.0, 0.6, l));
	color += vec3(0.004, 0.008, 0.010) * (1.0 - l);       // lifted, airy blacks
#endif
	color = mix(vec3(luma(color)), color, SATURATION);
	color = pow(saturate(color), vec3(1.0 / 2.2));
	color = saturate((color - 0.5) * CONTRAST + 0.5);

	// vignette
	vec2 vc = texcoord - 0.5;
	color *= 1.0 - dot(vc, vc) * VIGNETTE * 1.4;

	// film grain + dither against banding
	float grain = hash12(gl_FragCoord.xy + fract(frameTimeCounter) * 1000.0) - 0.5;
	color += grain * (FILM_GRAIN * 0.025 + 1.0 / 255.0);

	gl_FragColor = vec4(color, 1.0);
}
#endif
