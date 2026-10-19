// Fantastik gökyüzü: degrade, bulutlar, yıldızlar, nebula, kutup ışıkları, gezegenler
// Needs: common.glsl, atmosphere.glsl (initAtmosphere() çağrılmış olmalı)

#if defined WORLD_END
const vec3 END_SKY_A = vec3(0.06, 0.02, 0.10);
const vec3 END_SKY_B = vec3(0.20, 0.08, 0.30);
#endif

// Göğün temel rengi (bulut/gök cisimleri hariç). Sis rengi olarak da kullanılır.
vec3 skyGradient(vec3 d) {
#if defined WORLD_NETHER
    return fogColor * 1.4 + vec3(0.04, 0.01, 0.0);
#elif defined WORLD_END
    return mix(END_SKY_B, END_SKY_A, saturate(d.y * 0.5 + 0.5));
#else
    float up = d.y;
    float h = max(up, 0.0);
    float horizonW = exp(-h * 7.0);

    float daySet = smoothstep(0.05, 0.40, sunH);
    vec3 zen = mix(ZENITH_SET, ZENITH_DAY, daySet);
    vec3 hor = mix(HORIZON_SET, HORIZON_DAY, daySet);
    float lit = smoothstep(-0.18, 0.04, sunH);
    float setDim = mix(0.75, 1.0, daySet);
    zen = mix(ZENITH_NIGHT * NIGHT_BRIGHTNESS, zen * 1.1 * setDim, lit);
    hor = mix(HORIZON_NIGHT * NIGHT_BRIGHTNESS, hor * 1.05 * setDim, lit);

    vec3 col = mix(zen, hor, horizonW);

    // güneş tarafında sıcak saçılma
    float cosS = dot(d, sunDirW);
    float sunSide = pow(saturate(cosS * 0.5 + 0.5), 3.0);
    col += HORIZON_SET * sunSide * twiF * horizonW * 0.8;
    col += vec3(1.0, 0.75, 0.55) * pow(saturate(cosS), 24.0) * (0.2 + 0.45 * twiF) * lit;
    // karşı ufukta pembe/mor "venüs kuşağı"
    col += vec3(0.55, 0.30, 0.65) * pow(saturate(-cosS * 0.5 + 0.5), 4.0) * twiF * horizonW * 0.6;

    // ufkun altı
    float below = smoothstep(0.0, -0.25, up);
    col = mix(col, col * 0.35 + ambientColor * 0.08, below);

    float rainLum = luma(col);
    col = mix(col, vec3(rainLum) * vec3(0.85, 0.88, 0.95) * 0.75, rainStrength * 0.85);
    return col;
#endif
}

// ---------- Bulutlar ----------
vec4 cloudLayer(vec3 d) {
#if defined CUSTOM_CLOUDS && !defined WORLD_NETHER && !defined WORLD_END
    if (d.y < 0.015) return vec4(0.0);
    float t = CLOUD_HEIGHT / d.y;
    vec2 wind = vec2(frameTimeCounter * 4.0, frameTimeCounter * 1.5);
    vec2 p = d.xz * t + wind + cameraPosition.xz * 0.6;
    vec2 q = p * 0.0011;

    float cov = CLOUD_COVERAGE + rainStrength * 0.35;
    float n = fbm2(q, 6);
    n += (noise2(q * 0.35 + 3.1) - 0.5) * 0.25;
    float dens = smoothstep(1.0 - cov - 0.08, 1.0 - cov + 0.32, n);
    if (dens <= 0.001) return vec4(0.0);

    // güneşe doğru kaydırılmış örnekle kendinden gölgelenme
    vec2 toSun = normalize(sunDirW.xz + 1e-4) * 0.05;
    float n2 = fbm2(q + toSun, 4) + (noise2((q + toSun) * 0.35 + 3.1) - 0.5) * 0.25;
    float occl = smoothstep(1.0 - cov - 0.08, 1.0 - cov + 0.32, n2);
    float light = exp(-occl * 2.4);

    float cosS = dot(d, sunDirW);
    vec3 sunLit = lightColor * 0.36 * (light * 0.85 + 0.15);
    vec3 silver = lightColor * pow(saturate(cosS), 10.0) * (1.0 - dens) * 1.5;
    vec3 amb = ambientColor * (0.55 + 0.30 * dens) * (1.0 - nightF * 0.45);
    // fantastik: gün batımında pembe gölge tonu
    amb += HORIZON_SET * 0.25 * twiF;
    vec3 col = (sunLit + silver + amb) * (1.0 - rainStrength * 0.4);

    float fade = smoothstep(0.015, 0.14, d.y);
    return vec4(col, dens * fade * (1.0 - nightF * 0.35));
#else
    return vec4(0.0);
#endif
}

// ---------- Yıldızlar ----------
vec3 starField(vec3 sd) {
    vec3 col = vec3(0.0);
#ifdef STARS
    for (int layer = 0; layer < 2; layer++) {
        float scale = layer == 0 ? 140.0 : 260.0;
        vec3 p = sd * scale;
        vec3 cell = floor(p);
        float h = hash13(cell + float(layer) * 91.7);
        float thresh = 1.0 - 0.03 * STAR_AMOUNT * (layer == 0 ? 0.6 : 1.4);
        if (h > thresh) {
            vec3 off = hash33(cell + 7.7) * 0.6 + 0.2;
            float dist = length(p - (cell + off));
            float b = (h - thresh) / (1.0 - thresh);
            float tw = 0.65 + 0.35 * sin(frameTimeCounter * (1.5 + h * 4.0) + h * 40.0);
            float s = smoothstep(0.16, 0.0, dist) * (0.4 + b * 1.6) * tw;
            vec3 tint = mix(vec3(0.75, 0.85, 1.0), mix(vec3(1.0, 0.8, 0.95), vec3(0.7, 1.0, 1.0), hash13(cell)), 0.5);
            col += tint * s * (layer == 0 ? 6.0 : 2.5);
        }
    }
#endif
    return col;
}

// ---------- Nebula / galaksi ----------
vec3 nebula(vec3 sd) {
#ifdef NEBULA
    vec3 bandN = normalize(vec3(0.25, 0.35, 0.9));
    float band = exp(-pow2(dot(sd, bandN) / 0.32));
    if (band < 0.01) return vec3(0.0);
    float n1 = fbm3(sd * 2.6, 5);
    float n2 = fbm3(sd * 5.0 + 4.0, 4);
    float dust = smoothstep(0.45, 0.70, fbm3(sd * 7.0 + 9.0, 4));
    vec3 col = mix(NEBULA_B, NEBULA_A, smoothstep(0.35, 0.7, n2));
    col = mix(col, vec3(1.0, 0.85, 0.95), pow(band, 6.0) * 0.4);
    float intensity = band * smoothstep(0.30, 0.75, n1) * (1.0 - dust * 0.75);
    // parlak çekirdek
    intensity += pow(band, 8.0) * smoothstep(0.4, 0.8, n1) * 0.8;
    return col * intensity * 0.22;
#else
    return vec3(0.0);
#endif
}

// ---------- Kutup ışıkları ----------
vec3 aurora(vec3 d) {
#if defined AURORA && !defined WORLD_NETHER && !defined WORLD_END
    if (d.y < 0.03) return vec3(0.0);
    vec3 col = vec3(0.0);
    float t = frameTimeCounter * 0.03;
    for (int i = 0; i < 8; i++) {
        float fi = float(i) / 8.0;
        float hgt = 1.0 + fi * 0.35;
        vec2 p = d.xz / (d.y + 0.08) * hgt;
        float curtain = noise2(vec2(p.x * 0.45 + t, p.y * 0.12 - t * 0.5) * 2.0);
        float ridge = 1.0 - abs(noise2(vec2(p.x * 0.8 + curtain * 2.5, p.y * 0.25) * 1.6 + t) * 2.0 - 1.0);
        float band = pow(ridge, 7.0);
        float mask = smoothstep(0.2, 0.6, noise2(p * 0.18 + 5.0 + t * 0.2));
        col += mix(BIOLUM_A, BIOLUM_B, fi) * band * mask * (1.0 - fi) * 0.075;
    }
    float fade = smoothstep(0.03, 0.25, d.y) * smoothstep(0.85, 0.35, d.y);
    return col * fade;
#else
    return vec3(0.0);
#endif
}

// ---------- Gezegenler ----------
// Birim uzaklıkta küre + isteğe bağlı halka. sonuç: rgb renk, a örtücülük
vec4 planet(vec3 d, vec3 c, float r, vec3 L, vec3 colA, vec3 colB, vec3 pole, float ringInner, float ringOuter) {
    vec4 res = vec4(0.0);
    float b = dot(d, c);
    vec3 diff = c - d * b;
    float dist2 = dot(diff, diff);
    float r2 = r * r;
    float tS = 1e9;
    if (b > 0.0 && dist2 < r2) {
        tS = b - sqrt(r2 - dist2);
        vec3 N = normalize(d * tS - c);
        float lat = dot(N, pole);
        float bands = fbm3(vec3(lat * 9.0, N.x * 1.5, N.z * 1.5), 4);
        bands = mix(bands, sin(lat * 26.0 + bands * 6.0) * 0.5 + 0.5, 0.5);
        vec3 surf = mix(colA, colB, bands);
        float diffL = saturate(dot(N, L));
        float rim = pow(1.0 - saturate(dot(N, -d)), 3.0);
        vec3 col = surf * (diffL * 1.3 + 0.04) + mix(colA, vec3(0.8, 0.9, 1.0), 0.5) * rim * (diffL + 0.15) * 1.5;
        float edge = smoothstep(r2, r2 * 0.985, dist2);
        res = vec4(col, edge);
    }
    if (ringOuter > 0.0) {
        float denom = dot(d, pole);
        if (abs(denom) > 1e-4) {
            float tR = dot(c, pole) / denom;
            if (tR > 0.0) {
                float rr = length(d * tR - c) / r;
                if (rr > ringInner && rr < ringOuter) {
                    float x = (rr - ringInner) / (ringOuter - ringInner);
                    float ringPat = 0.55 + 0.25 * sin(x * 31.0) + 0.2 * sin(x * 11.0 + 1.3) + (noise2(vec2(x * 40.0, 0.5)) - 0.5) * 0.3;
                    ringPat *= smoothstep(0.0, 0.08, x) * smoothstep(1.0, 0.85, x);
                    ringPat *= 1.0 - 0.7 * smoothstep(0.42, 0.45, x) * smoothstep(0.52, 0.49, x); // Cassini boşluğu
                    vec3 ringCol = mix(colB, vec3(1.0, 0.92, 0.9), 0.5) * (0.35 + 0.9 * saturate(dot(pole, L) * 0.5 + 0.6));
                    float a = ringPat * 0.85;
                    if (tR < tS) {
                        res.rgb = mix(res.rgb, ringCol, a);
                        res.a = max(res.a, a);
                    } else if (res.a < 1.0) {
                        res.rgb = mix(ringCol, res.rgb, res.a);
                        res.a = max(res.a, a);
                    }
                }
            }
        }
    }
    return res;
}

vec3 celestial(vec3 d, vec3 col, bool drawSun) {
#if defined WORLD_NETHER
    return col;
#else
    vec3 sd = toStarSpace(d);
    float clearSky = 1.0 - rainStrength;
#if defined WORLD_END
    float nightVis = 1.0;
#else
    float nightVis = nightF;
#endif

    // yıldızlar + nebula + aurora
    float horizonFade = smoothstep(-0.05, 0.15, d.y);
    col += (starField(sd) + nebula(sd)) * nightVis * clearSky * horizonFade;
    col += aurora(d) * nightVis * nightVis * clearSky;

#ifdef PLANETS
    // Büyük halkalı gezegen (gündüz bile soluk görünür)
    float vis = mix(0.28, 1.0, nightVis) * clearSky * smoothstep(-0.02, 0.06, d.y);
    vec3 Lp = normalize(vec3(-0.55, 0.45, 0.70));
    vec3 pole1 = normalize(vec3(0.30, 0.85, -0.42));
    vec4 p1 = planet(sd, normalize(vec3(0.40, -0.55, 0.73)), 0.085, Lp, vec3(0.55, 0.32, 0.85), vec3(1.00, 0.55, 0.70), pole1, 1.35, 2.35);
    col = mix(col, p1.rgb * mix(0.6, 1.0, nightVis), p1.a * vis);
    // Küçük buzlu ay
    vec4 p2 = planet(sd, normalize(vec3(-0.62, -0.35, 0.70)), 0.028, Lp, vec3(0.35, 0.70, 1.00), vec3(0.85, 0.95, 1.00), vec3(0.0, 1.0, 0.0), 0.0, 0.0);
    col = mix(col, p2.rgb * mix(0.6, 1.0, nightVis), p2.a * vis);
    // Uzak pembe ay
    vec4 p3 = planet(sd, normalize(vec3(0.15, -0.25, -0.95)), 0.018, Lp, vec3(1.0, 0.5, 0.75), vec3(1.0, 0.8, 0.9), vec3(0.0, 1.0, 0.0), 0.0, 0.0);
    col = mix(col, p3.rgb * mix(0.6, 1.0, nightVis), p3.a * vis);
#endif

#if !defined WORLD_END
    if (drawSun) {
        // Güneş
        float cosS = dot(d, sunDirW);
        float sunDisk = smoothstep(0.99955, 0.99975, cosS);
        vec3 sunCol = mix(vec3(1.0, 0.45, 0.25), vec3(1.0, 0.92, 0.80), smoothstep(0.0, 0.3, sunH));
        col += sunCol * sunDisk * 60.0 * clearSky;
        col += sunCol * pow(saturate(cosS), 900.0) * 3.0 * clearSky;

        // Ay (evreli)
        vec3 moonDir = -sunDirW;
        float b = dot(d, moonDir);
        float mr = 0.032;
        vec3 diffM = moonDir - d * b;
        float dist2 = dot(diffM, diffM);
        col += MOON_TINT * (pow(saturate(b), 350.0) * 0.6 + pow(saturate(b), 40.0) * 0.06) * clearSky * NIGHT_BRIGHTNESS;
        if (b > 0.0 && dist2 < mr * mr) {
            float tS = b - sqrt(mr * mr - dist2);
            vec3 N = normalize(d * tS - moonDir);
            float pa = float(moonPhase) * (PI / 4.0);
            vec3 T = normalize(cross(moonDir, vec3(0.0, 0.0, 1.0)) + 1e-4);
            vec3 Lm = normalize(-moonDir * cos(pa) + T * sin(pa));
            float lit = smoothstep(-0.05, 0.15, dot(N, Lm));
            float crater = fbm3(N * 6.0, 4);
            vec3 mcol = mix(vec3(0.55, 0.58, 0.70), vec3(1.0, 0.98, 1.05), smoothstep(0.35, 0.65, crater));
            float edge = smoothstep(mr * mr, mr * mr * 0.95, dist2);
            col = mix(col, mcol * MOON_TINT * (lit * 3.0 + 0.03) * NIGHT_BRIGHTNESS, edge * clearSky);
        }
    }
#endif
    return col;
#endif
}

// Tam gökyüzü (deferred ve yansımalar için)
vec3 fullSky(vec3 d, bool drawSun) {
    vec3 col = skyGradient(d);
    col = celestial(d, col, drawSun);
    vec4 cl = cloudLayer(d);
    col = mix(col, cl.rgb, cl.a);
    return col;
}
