/*
    Krolasyon Fantasy Shaders - ayarlar / settings
    Her #define satırı oyun içi shader menüsünde bir seçenek olarak görünür.
*/

// ---------------- Gölgeler / Shadows ----------------
#define SHADOWS
const int shadowMapResolution = 2048; // [1024 1536 2048 3072 4096]
const float shadowDistance = 128.0; // [64.0 96.0 128.0 160.0 192.0 256.0]
const float shadowDistanceRenderMul = 1.0;
const float shadowIntervalSize = 2.0;
const bool shadowHardwareFiltering = true;
const float sunPathRotation = -25.0; // [-40.0 -30.0 -25.0 -20.0 -10.0 0.0 10.0 20.0 30.0 40.0]
#define SHADOW_SOFTNESS 1.0 // [0.5 1.0 1.5 2.0 3.0]
#define SHADOW_SAMPLES 8 // [4 8 12 16]
#define COLORED_SHADOWS

// ---------------- Işık / Lighting ----------------
#define SUN_BRIGHTNESS 1.0 // [0.5 0.75 1.0 1.25 1.5 2.0]
#define MOON_BRIGHTNESS 1.0 // [0.5 0.75 1.0 1.5 2.0 3.0]
#define AMBIENT_BRIGHTNESS 1.0 // [0.5 0.75 1.0 1.25 1.5 2.0]
#define BLOCKLIGHT_BRIGHTNESS 1.0 // [0.5 0.75 1.0 1.25 1.5 2.0]
#define HANDHELD_LIGHT
#define SSAO
#define SSAO_STRENGTH 1.0 // [0.5 0.75 1.0 1.5 2.0]
const float ambientOcclusionLevel = 1.0;
const float eyeBrightnessHalflife = 4.0;
const float wetnessHalflife = 300.0;
const float drynessHalflife = 60.0;

// ---------------- Fantastik efektler / Fantasy effects ----------------
#define SKY_THEME 0 // [0 1 2]
#define BIOLUMINESCENCE
#define BIOLUM_STRENGTH 1.0 // [0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define LOG_VEINS
#define FIREFLIES
#define FIREFLY_AMOUNT 1.0 // [0.5 1.0 1.5 2.0 3.0]
#define MAGIC_DUST
#define CRYSTAL_SPARKLE

// ---------------- Gökyüzü / Sky ----------------
#define PLANETS
#define NEBULA
#define AURORA
#define STARS
#define STAR_AMOUNT 1.0 // [0.5 1.0 1.5 2.0]
#define CUSTOM_CLOUDS
#define CLOUD_COVERAGE 0.5 // [0.2 0.3 0.4 0.5 0.6 0.7 0.8]
#define CLOUD_HEIGHT 900.0 // [500.0 700.0 900.0 1200.0 1600.0]

// ---------------- Su / Water ----------------
#define WATER_WAVES
#define WAVE_HEIGHT 1.0 // [0.25 0.5 0.75 1.0 1.5 2.0]
#define WATER_REFLECTIONS
#define WATER_GLOW
#define CAUSTICS
#define WATER_ALPHA 0.55 // [0.3 0.4 0.5 0.55 0.6 0.7 0.8]

// ---------------- Atmosfer / Atmosphere ----------------
#define VOLUMETRIC_LIGHT
#define VL_STRENGTH 1.0 // [0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define VL_STEPS 12 // [6 8 12 16 24]
#define FOG_DENSITY 1.0 // [0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define BORDER_FOG
#define WEATHER_FOG // Hava modlarının (Krolasyon Gerçekçi Hava) sis mesafesini ve rengini uygula
#define WEATHER_FOG_BRIGHTNESS 1.0 // [0.5 0.75 1.0 1.25 1.5]

// ---------------- Hareket / Motion ----------------
#define WAVING_PLANTS
#define WAVING_LEAVES
#define WIND_STRENGTH 1.0 // [0.25 0.5 0.75 1.0 1.5 2.0]

// ---------------- Kamera / Camera ----------------
#define BLOOM
#define BLOOM_STRENGTH 1.0 // [0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define EXPOSURE 1.0 // [0.5 0.75 1.0 1.25 1.5 2.0]
#define NIGHT_BRIGHTNESS 1.0 // [0.5 0.75 1.0 1.5 2.0]
#define SATURATION 1.12 // [0.8 0.9 1.0 1.06 1.12 1.2 1.3 1.4]
#define FANTASY_GRADE 1.0 // [0.0 0.25 0.5 0.75 1.0 1.25 1.5]
#define VIGNETTE
#define VIGNETTE_STRENGTH 0.35 // [0.15 0.25 0.35 0.5 0.7]
#define FXAA
#define SHARPEN 0.25 // [0.0 0.1 0.25 0.4 0.6]

// ---------------- Dahili / Internal ----------------
#if defined SHADOWS && !defined WORLD_NETHER
    #define SHADOWS_ACTIVE
#endif

// Iris/Oculus, bileşik "#if defined" ifadelerindeki anahtarları menüde tanımayabilir;
// her anahtarı basit bir #ifdef ile de anarak menüde görünmelerini sağla.
#ifdef SHADOWS
#endif
#ifdef VOLUMETRIC_LIGHT
#endif
#ifdef AURORA
#endif
#ifdef CUSTOM_CLOUDS
#endif
#ifdef WAVING_PLANTS
#endif
#ifdef WAVING_LEAVES
#endif
