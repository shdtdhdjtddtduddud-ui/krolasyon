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

#ifdef VSH
void main() {
	texcoord = gl_MultiTexCoord0.xy;
	gl_Position = ftransform();

	vec3 lc, amb, zen, hor;
	getAtmosphereColors(lc, amb, zen, hor);
	float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
	float e = EXPOSURE;
#ifdef AUTO_EXPOSURE
	float outdoorLum = luma(amb) + luma(lc) * 0.25;
	float outdoorExp = clamp(pow(0.55 / (outdoorLum + 0.03), 0.55), 0.9, 4.0);
	float caveExp = 2.6;
	e *= mix(caveExp, outdoorExp, smoothstep(0.0, 0.9, eyeSky));
#endif
	exposure = e;
}
#endif

#ifdef FSH
uniform sampler2D colortex0;

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

void main() {
	float dropMask;
	vec2 uv = texcoord + lensDrops(texcoord, dropMask);
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

	color *= exposure;
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
