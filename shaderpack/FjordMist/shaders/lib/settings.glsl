// ============================================================================
//  Fjord Mist Shaders - user settings
//  Every option below shows up in the in-game shader options menu.
// ============================================================================

// ---------------------------------------------------------------- Atmosphere
#define OVERCAST 0.45           // [0.0 0.1 0.2 0.3 0.35 0.4 0.45 0.5 0.6 0.7 0.8 0.9 1.0]
#define FOG_DENSITY 1.0         // [0.0 0.25 0.5 0.75 1.0 1.25 1.5 2.0 2.5 3.0 4.0]
#define MIST
#define MIST_DENSITY 1.0        // [0.25 0.5 0.75 1.0 1.25 1.5 2.0 2.5 3.0]
#define MIST_HEIGHT 72.0        // [50.0 56.0 60.0 64.0 68.0 72.0 80.0 90.0 100.0 120.0]
#define MIST_BAND
#define MIST_BAND_LOW 85.0      // [60.0 70.0 80.0 85.0 90.0 100.0 110.0 120.0]
#define MIST_BAND_HIGH 150.0    // [100.0 120.0 130.0 140.0 150.0 170.0 190.0 220.0]
#define MIST_STEPS 14           // [6 8 10 12 14 16 20 24 32]
#define VOLUMETRIC_LIGHT
#define VL_STRENGTH 1.0         // [0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define WIND_SPEED 1.0          // [0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]

// -------------------------------------------------------------------- Clouds
#define VOLUMETRIC_CLOUDS
#define CLOUD_HEIGHT 165.0      // [110.0 120.0 130.0 140.0 150.0 165.0 180.0 200.0 230.0 260.0]
#define CLOUD_THICKNESS 110.0   // [40.0 60.0 80.0 110.0 140.0 180.0 220.0]
#define CLOUD_COVERAGE 0.55     // [0.2 0.3 0.4 0.45 0.5 0.55 0.6 0.7 0.8 0.9]
#define CLOUD_STEPS 22          // [8 12 16 22 28 36 48 64]
#define CLOUD_LIGHT_STEPS 4     // [2 3 4 5 6 8]
#define CLOUDS_ON_TERRAIN
#define CLOUD_SCALE 1.0         // [0.5 0.75 1.0 1.25 1.5 2.0]

// --------------------------------------------------------------------- Water
#define WATER_WAVE_HEIGHT 1.0   // [0.0 0.25 0.5 0.75 1.0 1.25 1.5 2.0 3.0]
#define WATER_WAVE_SPEED 1.0    // [0.25 0.5 0.75 1.0 1.5 2.0]
#define WATER_CLARITY 1.0       // [0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define WATER_VERTEX_WAVES
#define WATER_REFRACTION
#define WATER_FOAM
#define FOAM_AMOUNT 1.0         // [0.0 0.5 0.75 1.0 1.5 2.0]
#define RAIN_RIPPLES
#define RAIN_STREAKS
#define RAIN_VISIBILITY 1.0     // [0.25 0.5 0.75 1.0 1.25 1.5 2.0]
#define SSR_STEPS 22            // [8 12 16 22 28 36 48]
#define WET_REFLECTIONS
#define PUDDLES

// -------------------------------------------------------------- Vegetation
#define WAVING_PLANTS
#define WAVING_LEAVES
#define WAVING_STRENGTH 1.0     // [0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define FOLIAGE_TRANSLUCENCY 1.0 // [0.0 0.5 1.0 1.5 2.0]

// ------------------------------------------------------------ Sky wonders
#define STARS
#define STAR_DENSITY 1.0        // [0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define MILKY_WAY 1.0           // [0.0 0.5 0.75 1.0 1.5 2.0 3.0]
#define NIGHT_GLOW 1.0          // [0.0 0.5 1.0 1.5 2.0]
#define CLEAR_NIGHTS
#define PROCEDURAL_MOON
#define MOON_SIZE 1.0           // [0.5 0.75 1.0 1.25 1.5 2.0]
#define SHOOTING_STARS
#define AURORA 1                // [0 1 2]
#define AURORA_STRENGTH 1.0     // [0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define RAINBOW
#define LIGHTNING_FLASH
#define LENS_FLARE
#define LENS_FLARE_STRENGTH 1.0 // [0.25 0.5 0.75 1.0 1.5 2.0]

// ------------------------------------------------------------------ Lighting
const int shadowMapResolution = 2048;   // [1024 1536 2048 3072 4096 6144 8192]
const float shadowDistance = 128.0;     // [64.0 96.0 128.0 160.0 192.0 256.0 320.0]
const float sunPathRotation = -25.0;    // [-45.0 -40.0 -35.0 -30.0 -25.0 -20.0 -15.0 -10.0 0.0 10.0 20.0 30.0]
#define SHADOW_SOFTNESS 1.0     // [0.0 0.5 1.0 1.5 2.0 3.0 4.0]
#define SUN_BRIGHTNESS 1.0      // [0.25 0.5 0.75 1.0 1.25 1.5 2.0]
#define AMBIENT_BRIGHTNESS 1.0  // [0.25 0.5 0.75 1.0 1.25 1.5 2.0]
#define TORCH_BRIGHTNESS 1.0    // [0.25 0.5 0.75 1.0 1.25 1.5 2.0 3.0]
#define MIN_LIGHT 1.0           // [0.0 0.5 1.0 2.0 3.0 5.0]
#define SSAO
#define AO_STRENGTH 1.0         // [0.25 0.5 0.75 1.0 1.25 1.5 2.0]
#define CAUSTICS
#define WETNESS_STRENGTH 1.0    // [0.0 0.25 0.5 0.75 1.0 1.5]

// ------------------------------------------------------------------- Camera
#define EXPOSURE 1.0            // [0.25 0.5 0.75 0.85 1.0 1.15 1.3 1.5 2.0 3.0]
#define AUTO_EXPOSURE
#define BLOOM
#define BLOOM_STRENGTH 1.0      // [0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define FJORD_GRADE
#define SATURATION 0.88         // [0.5 0.6 0.7 0.8 0.88 0.95 1.0 1.1 1.2 1.3]
#define CONTRAST 1.05           // [0.8 0.9 0.95 1.0 1.05 1.1 1.2 1.3]
#define VIGNETTE 0.35           // [0.0 0.15 0.25 0.35 0.5 0.75 1.0]
#define FILM_GRAIN 0.5          // [0.0 0.25 0.5 1.0 1.5 2.0]
#define RAIN_ON_LENS
