// Screen space reflections. Needs common.glsl and depthtex0.

// Returns xy = hit uv, z = confidence (0 = miss).
vec3 traceSSR(vec3 viewPos, vec3 reflDir, float dither, int steps) {
	float stepSize = 0.15 + length(viewPos) * 0.012;
	vec3 stepV = reflDir * stepSize;
	vec3 p = viewPos + stepV * (0.3 + dither);
	vec3 prev = viewPos;

	for (int i = 0; i < 48; i++) {
		if (i >= steps) break;
		vec3 sp = viewToScreen(p);
		if (sp.x < 0.0 || sp.x > 1.0 || sp.y < 0.0 || sp.y > 1.0 || sp.z > 1.0 || p.z > -near) break;

		float d = texture2D(depthtex0, sp.xy).r;
		if (d < 1.0 && d > 0.56) {
			float sceneZ = screenToView(sp.xy, d).z;
			float diff = sceneZ - p.z;           // > 0 : ray went behind the surface
			if (diff > 0.0 && diff < length(stepV) * 1.6 + 0.25) {
				// binary refinement
				vec3 a = prev;
				vec3 b = p;
				for (int j = 0; j < 6; j++) {
					vec3 m = (a + b) * 0.5;
					vec3 ms = viewToScreen(m);
					float md = texture2D(depthtex0, ms.xy).r;
					if (screenToView(ms.xy, md).z - m.z > 0.0) b = m; else a = m;
				}
				vec3 hs = viewToScreen(b);
				float hd = texture2D(depthtex0, hs.xy).r;
				float err = abs(screenToView(hs.xy, hd).z - b.z);
				// reject hits on the back of thin objects (causes streaks)
				if (err > 0.25 + abs(b.z) * 0.02 || hd >= 1.0) return vec3(0.0);
				vec2 edge = smoothstep(vec2(0.0), vec2(0.07), hs.xy) * smoothstep(vec2(1.0), vec2(0.93), hs.xy);
				return vec3(hs.xy, edge.x * edge.y);
			}
		}
		prev = p;
		stepV *= 1.22;
		p += stepV;
	}
	return vec3(0.0);
}
