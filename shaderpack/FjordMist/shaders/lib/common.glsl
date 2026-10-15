// Shared uniforms, constants and helper functions.

uniform vec3 sunPosition;
uniform vec3 moonPosition;
uniform vec3 shadowLightPosition;
uniform vec3 upPosition;
uniform vec3 cameraPosition;
uniform vec3 fogColor;
uniform vec3 skyColor;

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjection;
uniform mat4 gbufferProjectionInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;

uniform float frameTimeCounter;
uniform float rainStrength;
uniform float wetness;
uniform float viewWidth;
uniform float viewHeight;
uniform float near;
uniform float far;
uniform float sunAngle;
uniform int isEyeInWater;
uniform int worldTime;
uniform int frameCounter;
uniform ivec2 eyeBrightnessSmooth;

uniform sampler2D noisetex;

const int noiseTextureResolution = 256;
const float PI = 3.14159265359;
const float SEA_LEVEL = 63.0;

// Material ids written to colortex1.z
const float MAT_NONE    = 0.0;
const float MAT_TERRAIN = 1.0;
const float MAT_FOLIAGE = 2.0;
const float MAT_WATER   = 3.0;
const float MAT_GLASS   = 4.0;
const float MAT_ENTITY  = 5.0;
const float MAT_HAND    = 6.0;
const float MAT_EMISSIVE = 7.0;

float saturate(float x) { return clamp(x, 0.0, 1.0); }
vec2  saturate(vec2 x)  { return clamp(x, 0.0, 1.0); }
vec3  saturate(vec3 x)  { return clamp(x, 0.0, 1.0); }
float sq(float x) { return x * x; }
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

float worldTimeAnim() { return frameTimeCounter * WIND_SPEED; }

// Overall "weather" amount: 0 = clear, 1 = full storm.
// OVERCAST keeps a permanent layer of gray fjord mood even without rain.
float getOvercast() {
	return saturate(rainStrength + OVERCAST * (1.0 - rainStrength));
}

// ---------------------------------------------------------------- dithering
float bayer2(vec2 a) { a = floor(a); return fract(dot(a, vec2(0.5, a.y * 0.75))); }
float bayer4(vec2 a) { return bayer2(0.5 * a) * 0.25 + bayer2(a); }
float bayer8(vec2 a) { return bayer4(0.5 * a) * 0.25 + bayer2(a); }
float bayer16(vec2 a) { return bayer8(0.5 * a) * 0.25 + bayer2(a); }

float interleavedGradient(vec2 p) {
	return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

float hash12(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * 0.1031);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.xx + p3.yz) * p3.zy);
}

// ------------------------------------------------------------------- noise
// noise.png: R = random, G = R shifted by (37,17) so one fetch gives two
// z-slices of 3D value noise. B/A are independent random channels.
float noise2D(vec2 p) {
	vec2 i = floor(p);
	vec2 f = fract(p);
	f = f * f * (3.0 - 2.0 * f);
	return texture2D(noisetex, (i + f + 0.5) / 256.0).b;
}

float noise3D(vec3 p) {
	vec3 i = floor(p);
	vec3 f = fract(p);
	f = f * f * (3.0 - 2.0 * f);
	vec2 uv = i.xy + vec2(37.0, 17.0) * i.z + f.xy;
	vec2 rg = texture2D(noisetex, (uv + 0.5) / 256.0).rg;
	return mix(rg.x, rg.y, f.z);
}

float fbm3(vec3 p, int octaves) {
	float sum = 0.0;
	float amp = 0.5;
	float norm = 0.0;
	for (int i = 0; i < 6; i++) {
		if (i >= octaves) break;
		sum += noise3D(p) * amp;
		norm += amp;
		amp *= 0.5;
		p = p * 2.03 + vec3(0.13, -0.27, 0.41);
	}
	return sum / norm;
}

// ----------------------------------------------------------- normal coding
vec2 signNotZero(vec2 v) { return vec2(v.x >= 0.0 ? 1.0 : -1.0, v.y >= 0.0 ? 1.0 : -1.0); }

vec2 encodeNormal(vec3 n) {
	n /= abs(n.x) + abs(n.y) + abs(n.z);
	vec2 e = n.z >= 0.0 ? n.xy : (1.0 - abs(n.yx)) * signNotZero(n.xy);
	return e * 0.5 + 0.5;
}

vec3 decodeNormal(vec2 e) {
	e = e * 2.0 - 1.0;
	vec3 n = vec3(e, 1.0 - abs(e.x) - abs(e.y));
	if (n.z < 0.0) n.xy = (1.0 - abs(n.yx)) * signNotZero(n.xy);
	return normalize(n);
}

// --------------------------------------------------------------- transforms
vec3 projectAndDivide(mat4 m, vec3 p) {
	vec4 h = m * vec4(p, 1.0);
	return h.xyz / h.w;
}

vec3 screenToView(vec2 uv, float depth) {
	return projectAndDivide(gbufferProjectionInverse, vec3(uv, depth) * 2.0 - 1.0);
}

vec3 viewToScreen(vec3 viewPos) {
	return projectAndDivide(gbufferProjection, viewPos) * 0.5 + 0.5;
}

vec3 viewToFeet(vec3 viewPos) {
	return mat3(gbufferModelViewInverse) * viewPos + gbufferModelViewInverse[3].xyz;
}

float linearizeDepth(float d) {
	return (2.0 * near * far) / (far + near - (d * 2.0 - 1.0) * (far - near));
}

// Lightmap in [0,1]
vec2 normalizeLightmap(vec2 lm) {
	return saturate((lm - 1.0 / 32.0) * (32.0 / 30.0));
}
