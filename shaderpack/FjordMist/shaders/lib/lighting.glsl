// Forward lighting used by all lit gbuffers programs.
// Needs common.glsl, sky.glsl and (optionally) shadow.glsl with SHADOW_SAMPLING.

const vec3 TORCH_COLOR = vec3(1.0, 0.55, 0.24);

vec3 computeLighting(
	vec3 albedo, vec3 normalView, vec3 feetPos, vec2 lm,
	float translucency, float emissive,
	vec3 lightCol, vec3 ambientCol, float dither, out float shadowOut)
{
	vec3 lightDir = normalize(shadowLightPosition);
	vec3 upDir    = normalize(upPosition);
	float NdotL   = dot(normalView, lightDir);
	float NdotU   = dot(normalView, upDir);

	// ---- direct
	float shadow = 1.0;
	float diffuse = saturate(NdotL);
	// foliage: wrapped diffuse so grass blades never go pitch black
	diffuse = mix(diffuse, saturate(NdotL * 0.5 + 0.5) * 0.8, saturate(translucency));

#if defined SHADOW_SAMPLING && !defined NETHER
	if (max(diffuse, translucency) > 0.001 && lm.y > 0.01) {
		vec3 normalWorld = mat3(gbufferModelViewInverse) * normalView;
		// for foliage, bias towards the light instead of the (fake) normal
		vec3 biasDir = mix(normalWorld, mat3(gbufferModelViewInverse) * lightDir, saturate(translucency));
		shadow = sampleShadow(feetPos, biasDir, abs(NdotL), dither);
	} else {
		shadow = 0.0;
	}
#else
	shadow = smoothstep(0.7, 0.95, lm.y);
#endif
	// beyond shadow range / underground: use skylight as a rough occlusion term
	shadow *= smoothstep(0.05, 0.6, lm.y);
	shadowOut = shadow;

	// subsurface: light passing through leaves and grass from behind
	vec3 viewDir = normalize(-(gbufferModelView * vec4(feetPos, 0.0)).xyz);
	float backLit = pow(saturate(dot(-viewDir, lightDir)), 4.0) * 0.8 + 0.2;
	vec3 sss = lightCol * albedo * translucency * backLit * FOLIAGE_TRANSLUCENCY * 0.9;

	vec3 direct = lightCol * diffuse * shadow;

	// ---- sky ambient (overcast light is very soft and comes from everywhere)
	float skyL = lm.y * lm.y;
	float ambientDir = 0.62 + 0.38 * NdotU;
	ambientDir = mix(ambientDir, 0.85, getOvercast() * 0.5);
	vec3 ambient = ambientCol * skyL * ambientDir;

	// ---- block light
	float torch = pow(lm.x, 3.2) * 1.4 + pow(lm.x, 12.0) * 2.0;
	vec3 block = TORCH_COLOR * torch * TORCH_BRIGHTNESS;

	// ---- minimum light for caves
	vec3 minLight = vec3(0.012, 0.013, 0.016) * MIN_LIGHT * (1.0 - lm.y);

	vec3 light = direct + ambient + block + minLight;
	vec3 col = albedo * light + sss * shadow;
	col += albedo * emissive * 3.5;
	return col;
}
