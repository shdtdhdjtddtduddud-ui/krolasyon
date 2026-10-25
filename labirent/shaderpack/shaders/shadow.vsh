#version 120
#include "/lib/settings.glsl"
#include "/lib/common.glsl"

attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;
attribute vec4 at_midBlock;
uniform mat4 shadowModelViewInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;
uniform float rainStrength;

varying vec2 texcoord;
varying vec4 color;

#include "/lib/waving.glsl"

void main() {
    texcoord = gl_MultiTexCoord0.xy;
    color = gl_Color;
    vec4 view = gl_ModelViewMatrix * gl_Vertex;
    vec4 world = shadowModelViewInverse * view;
    world.xyz = wave(world.xyz, mc_Entity.x, gl_MultiTexCoord0.t < mc_midTexCoord.t, at_midBlock.xyz);
    vec4 p = shadowProjection * (shadowModelView * world);
    p.xy = distort(p.xy);
    p.z *= 0.2;
    gl_Position = p;
}
