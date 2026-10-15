// composite2: denoises the volumetric fog and applies it, plus underwater,
// lava and powder snow fog and the render-distance fade.

/*
const int colortex4Format = RGBA16F;
const int colortex5Format = RGBA16F;
const vec4 colortex5ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
*/

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/sky.glsl"

varying vec2 texcoord;
varying vec3 lightCol;
varying vec3 ambientCol;
varying vec3 zenithCol;
varying vec3 horizonCol;

#ifdef VSH
void main() {
	texcoord = gl_MultiTexCoord0.xy;
	gl_Position = ftransform();
	getAtmosphereColors(lightCol, ambientCol, zenithCol, horizonCol);
}
#endif

#ifdef FSH
uniform sampler2D colortex0;
uniform sampler2D colortex4;
uniform sampler2D colortex5;
uniform sampler2D depthtex0;

// depth aware blur of the noisy ray-marched fog
vec4 denoiseFog(vec2 uv, float centerDist) {
	vec2 px = 1.0 / vec2(viewWidth, viewHeight);
	vec4 sum = vec4(0.0);
	float wsum = 0.0;
	for (int x = -2; x <= 2; x++) {
		for (int y = -2; y <= 2; y++) {
			vec2 o = vec2(float(x), float(y)) * 1.5 * px;
			float d = texture2D(depthtex0, uv + o).r;
			float dist = d >= 1.0 ? far * 1.6 : length(screenToView(uv + o, d));
			float w = exp(-abs(dist - centerDist) / (0.5 + centerDist * 0.05)) * exp(-float(x * x + y * y) * 0.15);
			sum += texture2D(colortex4, uv + o) * w;
			wsum += w;
		}
	}
	return sum / max(wsum, 0.0001);
}

// Distant curtains of falling rain. Anchored to world directions so they
// stay put when the camera turns; only visible in front of distant objects.
float rainCurtain(vec3 dirW, float dist) {
	float az = atan(dirW.z, dirW.x);
	float el = asin(clamp(dirW.y, -1.0, 1.0));
	float t = frameTimeCounter;
	float sum = 0.0;
	for (int k = 0; k < 2; k++) {
		float cols = k == 0 ? 260.0 : 410.0;
		float speed = k == 0 ? 7.0 : 5.0;
		vec2 g = vec2(az / (2.0 * PI) * cols + el * 6.0, el * cols * 0.18 + t * speed);
		float column = floor(g.x);
		float r = hash12(vec2(column, float(k) * 13.1));
		float r2 = hash12(vec2(column + 0.5, float(k) * 7.7));
		float x = fract(g.x) - 0.5;
		float y = fract(g.y * 0.35 + r * 7.0);
		float streak = smoothstep(0.16, 0.0, abs(x)) * smoothstep(0.0, 0.05, y) * smoothstep(0.45, 0.1, y) * step(0.45, r2);
		sum += streak * (k == 0 ? 1.0 : 0.6);
	}
	float depthMask = smoothstep(6.0, 30.0, dist);
	float angleMask = 1.0 - smoothstep(0.55, 0.9, abs(dirW.y));
	return sum * depthMask * angleMask;
}

void main() {
	vec2 uv = texcoord;
	vec3 color = texture2D(colortex0, uv).rgb;
	float d0 = texture2D(depthtex0, uv).r;
	vec3 view0 = screenToView(uv, d0);
	vec3 dirView = normalize(view0);
	vec3 dirW = normalize(mat3(gbufferModelViewInverse) * dirView);
	bool sky = d0 >= 1.0;
	float dist = sky ? far * 1.6 : length(view0);

	float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
	float outside = mix(0.08, 1.0, smoothstep(0.0, 0.8, eyeSky));

	if (isEyeInWater == 1) {
		vec3 sigma = vec3(0.32, 0.09, 0.075) / WATER_CLARITY + 0.04;
		vec3 trans = exp(-sigma * min(dist, 96.0));
		vec3 waterCol = (ambientCol * vec3(0.06, 0.12, 0.12) + lightCol * vec3(0.02, 0.05, 0.05)) * outside;
		color = color * trans + waterCol * (1.0 - trans);
	} else if (isEyeInWater == 2) {
		color = mix(color, vec3(1.6, 0.45, 0.08), 1.0 - exp(-dist * 0.9));
	} else if (isEyeInWater == 3) {
		color = mix(color, vec3(0.65, 0.72, 0.8) * (ambientCol + 0.05), 1.0 - exp(-dist * 0.6));
	} else {
#if defined NETHER
		color = mix(color, pow(fogColor, vec3(2.2)) * 1.4, 1.0 - exp(-dist * 0.012 * FOG_DENSITY));
#elif defined END
		color = mix(color, vec3(0.08, 0.05, 0.11), 1.0 - exp(-dist * 0.004 * FOG_DENSITY));
#else
		vec4 fog = denoiseFog(uv, dist);
		if (sky) {
			// keep the sky readable: haze mostly near the horizon
			float horizon = 1.0 - smoothstep(0.0, 0.35, dirW.y);
			fog.a = mix(1.0, fog.a, horizon);
			fog.rgb *= horizon;
		}
		color = color * fog.a + fog.rgb;

		if (!sky) {
			// melt the edge of the render distance into the sky
			float border = smoothstep(far * 0.72, far * 0.98, dist);
			vec3 skyCol = getSky(dirView, lightCol, zenithCol, horizonCol, false);
			color = mix(color, skyCol * mix(1.0, fog.a, 0.5) + fog.rgb * 0.5, border);
		}
	#ifdef RAIN_STREAKS
		if (rainStrength > 0.01 && eyeSky > 0.3) {
			float streaks = rainCurtain(dirW, dist) * rainStrength * 0.07 * RAIN_VISIBILITY;
			color = mix(color, ambientCol * 1.5 + lightCol * 0.15, saturate(streaks) * outside);
		}
	#endif
#endif
	}

	// rain streaks (premultiplied)
	vec4 rain = texture2D(colortex5, uv);
	color = color * (1.0 - saturate(rain.a)) + rain.rgb;

	/* DRAWBUFFERS:0 */
	gl_FragData[0] = vec4(color, 1.0);
}
#endif
