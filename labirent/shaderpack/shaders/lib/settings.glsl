// ---------------------------------------------------------------- Labirent Shaders: settings
#define SHADOWS
#define SHADOW_SOFTNESS 1.4 // [0.6 1.0 1.4 2.0 3.0]
#define SHADOW_SAMPLES 12 // [6 12 20]
#define SSAO
#define SSAO_STRENGTH 1.0 // [0.4 0.7 1.0 1.4 1.8]
#define VOLUMETRIC_LIGHT
#define VL_STEPS 12 // [6 12 20 32]
#define VL_STRENGTH 1.0 // [0.4 0.7 1.0 1.5 2.0]
#define FOG_DENSITY 1.0 // [0.3 0.6 1.0 1.5 2.0 3.0]
#define MAZE_HAZE 1.0 // [0.0 0.5 1.0 1.5 2.0]
#define WAVING_PLANTS
#define WAVING_IVY
#define WAVE_SPEED 1.0 // [0.5 1.0 1.5 2.0]
#define WAVE_STRENGTH 1.0 // [0.5 1.0 1.5 2.0]
#define WALL_RELIEF 1.0 // [0.0 0.5 1.0 1.5 2.0]
#define BLOOM
#define BLOOM_STRENGTH 1.0 // [0.3 0.6 1.0 1.5 2.0]
#define EXPOSURE 1.0 // [0.6 0.8 1.0 1.2 1.4 1.7]
#define SATURATION 0.9 // [0.6 0.75 0.9 1.0 1.1 1.25]
#define CINEMATIC_GRADE 1.0 // [0.0 0.5 1.0]
#define VIGNETTE 0.55 // [0.0 0.3 0.55 0.8]
#define FILM_GRAIN 0.35 // [0.0 0.2 0.35 0.6]
#define CHROMATIC_ABERRATION 0.4 // [0.0 0.4 0.8]
#define WATER_REFLECTIONS
#define STARS

const int shadowMapResolution = 2048; // [1024 2048 3072 4096]
const float shadowDistance = 120.0; // [64.0 96.0 120.0 160.0 220.0]
const float shadowDistanceRenderMul = 1.0;
const float sunPathRotation = -25.0; // [-40.0 -25.0 -10.0 0.0 10.0 25.0]
const float ambientOcclusionLevel = 0.6;
const float eyeBrightnessHalflife = 4.0;

/*
const int colortex0Format = RGBA16F;
const int colortex1Format = RGBA16;
const int colortex2Format = RGBA16;
const int colortex3Format = RGBA16F;
const bool colortex3Clear = false;
const vec4 colortex1ClearColor = vec4(0.5, 0.5, 1.0, 0.0);
const vec4 colortex2ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
*/

// material ids (stored in colortex1.a * 255)
#define MAT_DEFAULT 1.0
#define MAT_PLANT 2.0
#define MAT_LEAVES 3.0
#define MAT_EMISSIVE 4.0
#define MAT_ENTITY 5.0
#define MAT_HAND 6.0
#define MAT_CLOUD 7.0
#define MAT_WALL 8.0
