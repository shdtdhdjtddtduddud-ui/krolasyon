// Shadow map distortion and soft shadow lookup.

const float SHADOW_DISTORT = 0.86;

float shadowDistortFactor(vec2 p) {
	return length(p) * SHADOW_DISTORT + (1.0 - SHADOW_DISTORT);
}

vec3 distortShadow(vec3 p) {
	return vec3(p.xy / shadowDistortFactor(p.xy), p.z * 0.25);
}

#ifdef SHADOW_SAMPLING
const bool shadowHardwareFiltering = true;
uniform sampler2DShadow shadowtex0;

const vec2 poissonDisk[12] = vec2[12](
	vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696,  0.457),
	vec2(-0.203,  0.621), vec2( 0.962, -0.195), vec2( 0.473, -0.480),
	vec2( 0.519,  0.767), vec2( 0.185, -0.893), vec2( 0.507,  0.064),
	vec2( 0.896,  0.412), vec2(-0.322, -0.933), vec2(-0.792, -0.598)
);

// feetPos: player relative position. Returns shadow-space screen position.
vec3 feetToShadowScreen(vec3 feetPos) {
	vec3 sv = (shadowModelView * vec4(feetPos, 1.0)).xyz;
	vec3 sc = (shadowProjection * vec4(sv, 1.0)).xyz;
	return distortShadow(sc) * 0.5 + 0.5;
}

float sampleShadowSingle(vec3 feetPos) {
	vec3 s = feetToShadowScreen(feetPos);
	if (any(greaterThan(abs(s.xy - 0.5), vec2(0.5)))) return 1.0;
	return shadow2D(shadowtex0, vec3(s.xy, s.z - 0.0002)).x;
}

// Soft shadows. normalWorld is used for normal-offset biasing.
float sampleShadow(vec3 feetPos, vec3 normalWorld, float NdotL, float dither) {
	float dist = length(feetPos);
	if (dist > shadowDistance) return 1.0;

	// normal offset bias grows with distance (shadow texels get bigger)
	float texelWorld = shadowDistance * 2.0 / float(shadowMapResolution);
	feetPos += normalWorld * texelWorld * (1.0 + dist * 0.03) * 1.2;

	vec3 sv = (shadowModelView * vec4(feetPos, 1.0)).xyz;
	vec3 sc = (shadowProjection * vec4(sv, 1.0)).xyz;
	float df = shadowDistortFactor(sc.xy);
	vec3 s = distortShadow(sc) * 0.5 + 0.5;
	if (any(greaterThan(abs(s.xy - 0.5), vec2(0.499)))) return 1.0;

	float bias = 0.00004 + 0.00012 * df * df * (1.0 - saturate(NdotL)) / max(df, 0.1);
	s.z -= bias;

	float radius = SHADOW_SOFTNESS * 1.6 / float(shadowMapResolution) / df * (0.6 + 0.4 * df);
	float ang = dither * 2.0 * PI;
	mat2 rot = mat2(cos(ang), sin(ang), -sin(ang), cos(ang));

	float sum = 0.0;
	for (int i = 0; i < 12; i++) {
		vec2 off = rot * poissonDisk[i] * radius;
		sum += shadow2D(shadowtex0, vec3(s.xy + off, s.z)).x;
	}
	float shadow = sum / 12.0;
	// fade out at the edge of the shadow distance
	return mix(shadow, 1.0, smoothstep(shadowDistance * 0.85, shadowDistance, dist));
}
#endif
