// Shadow map pass with the same wind animation as the main view.

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/waving.glsl"
#include "/lib/shadow.glsl"

varying vec2 texcoord;
varying vec4 glcolor;
varying float blockId;

#ifdef VSH
attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;
uniform mat4 shadowModelViewInverse;

void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	glcolor = gl_Color;
	blockId = mc_Entity.x;
#if defined NETHER || defined END
	gl_Position = vec4(-10.0, -10.0, -10.0, 1.0);
	return;
#endif
	vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
	vec3 feet = (shadowModelViewInverse * viewPos).xyz;
	vec2 lm = normalizeLightmap((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy);
	bool topVertex = gl_MultiTexCoord0.t < mc_midTexCoord.t;
	vec3 wave = waveOffset(feet + cameraPosition, blockId, topVertex, lm.y);
	if (dot(wave, wave) > 0.0) {
		feet += wave;
		viewPos = shadowModelView * vec4(feet, 1.0);
	}
	gl_Position = gl_ProjectionMatrix * viewPos;
	gl_Position.xyz = distortShadow(gl_Position.xyz);
}
#endif

#ifdef FSH
uniform sampler2D tex;

void main() {
	if (idIs(blockId, ID_WATER)) discard;   // water does not cast shadows
	vec4 c = texture2D(tex, texcoord) * glcolor;
	if (c.a < 0.1) discard;
	gl_FragData[0] = c;
}
#endif
