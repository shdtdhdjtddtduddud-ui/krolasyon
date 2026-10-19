// Günün saati, ışık renkleri ve tema paleti / time of day, light colors, theme palette

#if SKY_THEME == 1
    // Pandora: turkuaz geceler, zümrüt gündüzler
    const vec3 ZENITH_DAY    = vec3(0.10, 0.42, 0.85);
    const vec3 HORIZON_DAY   = vec3(0.55, 0.88, 0.95);
    const vec3 ZENITH_SET    = vec3(0.12, 0.25, 0.55);
    const vec3 HORIZON_SET   = vec3(1.00, 0.55, 0.40);
    const vec3 ZENITH_NIGHT  = vec3(0.004, 0.025, 0.05);
    const vec3 HORIZON_NIGHT = vec3(0.02, 0.10, 0.16);
    const vec3 NEBULA_A      = vec3(0.15, 0.85, 0.90);
    const vec3 NEBULA_B      = vec3(0.35, 0.30, 1.00);
    const vec3 BIOLUM_A      = vec3(0.10, 1.00, 0.95);
    const vec3 BIOLUM_B      = vec3(0.45, 0.35, 1.00);
    const vec3 MOON_TINT     = vec3(0.45, 0.80, 1.00);
#elif SKY_THEME == 2
    // Altın Çağ: sıcak altın günbatımları, amber geceler
    const vec3 ZENITH_DAY    = vec3(0.25, 0.45, 0.90);
    const vec3 HORIZON_DAY   = vec3(0.98, 0.85, 0.70);
    const vec3 ZENITH_SET    = vec3(0.35, 0.25, 0.50);
    const vec3 HORIZON_SET   = vec3(1.00, 0.55, 0.20);
    const vec3 ZENITH_NIGHT  = vec3(0.02, 0.02, 0.06);
    const vec3 HORIZON_NIGHT = vec3(0.10, 0.07, 0.12);
    const vec3 NEBULA_A      = vec3(1.00, 0.55, 0.30);
    const vec3 NEBULA_B      = vec3(0.70, 0.30, 0.90);
    const vec3 BIOLUM_A      = vec3(1.00, 0.80, 0.35);
    const vec3 BIOLUM_B      = vec3(0.60, 1.00, 0.40);
    const vec3 MOON_TINT     = vec3(0.70, 0.75, 1.00);
#else
    // Rüya (varsayılan): lavanta gündüzler, pembe günbatımları, nebula geceler
    const vec3 ZENITH_DAY    = vec3(0.20, 0.36, 0.95);
    const vec3 HORIZON_DAY   = vec3(0.86, 0.74, 0.98);
    const vec3 ZENITH_SET    = vec3(0.30, 0.18, 0.62);
    const vec3 HORIZON_SET   = vec3(1.00, 0.42, 0.45);
    const vec3 ZENITH_NIGHT  = vec3(0.012, 0.016, 0.065);
    const vec3 HORIZON_NIGHT = vec3(0.07, 0.05, 0.17);
    const vec3 NEBULA_A      = vec3(0.95, 0.30, 0.80);
    const vec3 NEBULA_B      = vec3(0.25, 0.45, 1.00);
    const vec3 BIOLUM_A      = vec3(0.15, 0.95, 1.00);
    const vec3 BIOLUM_B      = vec3(0.85, 0.35, 1.00);
    const vec3 MOON_TINT     = vec3(0.55, 0.65, 1.00);
#endif

float sunH;      // güneşin yüksekliği (sin)
float dayF;      // 0..1 gündüz
float nightF;    // 0..1 gece
float twiF;      // 0..1 alacakaranlık
vec3 sunDirW;    // dünya uzayında güneş yönü
vec3 lightDirW;  // gölge ışığı yönü (güneş ya da ay)
vec3 lightColor; // doğrudan ışık rengi
vec3 ambientColor;
vec3 celestialAxis;

void initAtmosphere() {
    sunDirW = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    lightDirW = normalize(mat3(gbufferModelViewInverse) * shadowLightPosition);
    sunH = sunDirW.y;

    dayF = smoothstep(-0.02, 0.30, sunH);
    nightF = smoothstep(0.02, -0.20, sunH);
    twiF = exp(-sunH * sunH * 22.0);

    float sr = radians(sunPathRotation);
    celestialAxis = normalize(vec3(0.0, sin(sr), cos(sr)));

#if defined WORLD_NETHER
    lightColor = vec3(0.0);
    ambientColor = mix(vec3(0.55, 0.22, 0.12), fogColor * 1.6 + 0.05, 0.5) * 0.55 * AMBIENT_BRIGHTNESS;
#elif defined WORLD_END
    lightColor = vec3(0.55, 0.40, 0.95) * 0.9;
    ambientColor = vec3(0.20, 0.14, 0.32) * 0.55 * AMBIENT_BRIGHTNESS;
#else
    vec3 sunDay = vec3(1.00, 0.93, 0.84) * 3.2;
    vec3 sunSet = vec3(1.00, 0.52, 0.32) * 2.2;
    vec3 sunCol = mix(sunSet, sunDay, smoothstep(0.02, 0.40, sunH)) * SUN_BRIGHTNESS;
    vec3 moonCol = MOON_TINT * 0.30 * MOON_BRIGHTNESS * NIGHT_BRIGHTNESS;
    // gün doğumu/batımında ışık güneşten aya geçerken sıfıra iner (ani sıçramayı gizler)
    float swap = smoothstep(0.0, 0.07, abs(sunH));
    lightColor = (sunH > 0.0 ? sunCol : moonCol) * swap;

    vec3 ambDay = mix(HORIZON_DAY, ZENITH_DAY, 0.55) * 0.85;
    vec3 ambSet = mix(HORIZON_SET, ZENITH_SET, 0.6) * 0.45;
    vec3 ambNight = mix(HORIZON_NIGHT, MOON_TINT * 0.12, 0.6) * 1.1 * NIGHT_BRIGHTNESS;
    ambientColor = mix(ambNight, mix(ambSet, ambDay, smoothstep(0.05, 0.4, sunH)), smoothstep(-0.12, 0.05, sunH));
    ambientColor *= AMBIENT_BRIGHTNESS;

    // yağmur: ışık söner, ortam griye kayar
    lightColor *= 1.0 - rainStrength * 0.85;
    ambientColor = mix(ambientColor, vec3(luma(ambientColor)) * 0.8, rainStrength * 0.6);
#endif
}

// Gökyüzü dokusunu (yıldızlar, gezegenler) güneşle birlikte döndüren uzay
vec3 toStarSpace(vec3 d) {
    vec3 e2 = sunDirW;
    vec3 e1 = normalize(celestialAxis - e2 * dot(celestialAxis, e2));
    vec3 e3 = cross(e1, e2);
    return vec3(dot(d, e1), dot(d, e2), dot(d, e3));
}
