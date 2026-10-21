package com.krolasyon.mythblades.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.krolasyon.mythblades.client.Vox.argb;
import static com.krolasyon.mythblades.client.Vox.hash;
import static com.krolasyon.mythblades.client.Vox.lerp;
import static com.krolasyon.mythblades.client.Vox.scale;
import static com.krolasyon.mythblades.client.Vox.solid;

/**
 * Üç efsane kılıcın voksel modelleri (birim: piksel, bıçak +Y yönünde, siper y = 0 civarında, ön yüz +Z).
 * Statik parçalar bir kez pişirilip önbelleğe alınır; animasyonlar parça dönüşümleri ve önceden pişirilmiş
 * kare dizileriyle (parıltı, damar ışığı) yapılır.
 */
public final class BladeModels {
    private BladeModels() {}

    private static final Map<String, Vox.Mesh> CACHE = new HashMap<>();

    static Vox.Mesh mesh(String key, Consumer<Vox> build) {
        Vox.Mesh m = CACHE.get(key);
        if (m == null) {
            Vox v = new Vox();
            build.accept(v);
            m = v.bake();
            CACHE.put(key, m);
        }
        return m;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    private static float fract(float f) {
        return f - (float) Math.floor(f);
    }

    // =================================================================================================== GÖKKUŞAĞI PALASI

    private static final float R_Y0 = 1.0F, R_LEN = 30.0F;
    private static final int[] RAINBOW = {0xF7A6C9, 0xFFC79A, 0xFFF0A0, 0xBDF0B4, 0xA6D9F7, 0xC4B6F7};

    /** Bıçağın orta çizgisi (t = 0 dip, 1 uç). */
    private static float rCenter(float t) {
        return -8.5F * (float) Math.sin(Math.PI * t * 0.95) + 3.2F * t * t;
    }

    private static float rWidth(float t) {
        float w = 6.0F + 1.6F * (float) Math.sin(Math.PI * Math.min(1, t * 1.1));
        if (t > 0.8F) w *= (float) Math.sqrt(Math.max(0, (1 - t) / 0.2F));
        return w;
    }

    /** Bıçak içinde mi; out[0] = enine konum (-0.5 dış kenar, 0.5 iç kenar), out[1] = t. */
    private static boolean rIn(float x, float y, float z, float[] out) {
        float t = (y - R_Y0) / R_LEN;
        if (t < 0 || t > 1) return false;
        float w = rWidth(t);
        if (w < 0.3F) return false;
        // uç dış kenara doğru kıvrılır
        float c = rCenter(t) - (t > 0.8F ? (t - 0.8F) * 6F : 0);
        float u = (x - c) / w;
        if (u < -0.5F || u > 0.5F) return false;
        // dış kenardaki çentikler
        if (u < -0.36F && (Math.abs(t - 0.6F) < 0.018F || Math.abs(t - 0.71F) < 0.014F)) return false;
        float th = 0.95F - 0.6F * (u + 0.5F);
        if (Math.abs(z) > th) return false;
        if (out != null) {
            out[0] = u;
            out[1] = t;
        }
        return true;
    }

    private static int rainbowColor(float u, float z, int i, int j, int k) {
        float b = (u + 0.5F) * RAINBOW.length;
        int idx = (int) clamp((float) Math.floor(b), 0, RAINBOW.length - 1);
        int c = 0xFF000000 | RAINBOW[idx];
        // bant geçişleri yumuşak, dış kenarda beyaz parıltı, iç kenarda cam beyazı
        float f = fract(b);
        if (f > 0.75F && idx < RAINBOW.length - 1) c = lerp(c, 0xFF000000 | RAINBOW[idx + 1], (f - 0.75F) * 2);
        if (u < -0.44F) c = lerp(c, 0xFFFFF4FA, 0.65F);
        if (u > 0.42F) c = lerp(c, 0xFFEAF6FF, 0.7F);
        // kalın sırtta açık bir pah çizgisi
        if (u > -0.36F && u < -0.31F) c = lerp(c, 0xFFFFFFFF, 0.35F);
        return scale(c, 0.97F + hash(i, j, k) * 0.06F);
    }

    private static void rainbowBlade(Vox v) {
        float[] o = new float[2];
        v.fill(-17, R_Y0, -1.2F, 8, R_Y0 + R_LEN, 1.2F, (x, y, z) -> rIn(x, y, z, null), (x, y, z, i, j, k) -> {
            rIn(x, y, z, o);
            return rainbowColor(o[0], z, i, j, k);
        }, Vox.CUT, false);
    }

    private static Vox.Col cloud(float cx, float cy, int base, int line) {
        return (x, y, z, i, j, k) -> {
            float dx = x - cx, dy = y - cy;
            float r = (float) Math.sqrt(dx * dx + dy * dy);
            float a = (float) (Math.atan2(dy, dx) / (Math.PI * 2));
            float s = fract(r * 0.42F - a);
            int c = s < 0.16F && r > 0.5F ? line : base;
            if (z > 0.6F && s > 0.55F && s < 0.7F) c = lerp(c, 0xFFD8DEFF, 0.4F);
            return scale(c, 0.96F + hash(i, j, k) * 0.08F);
        };
    }

    private static void puff(Vox v, float cx, float cy, float rx, float ry, float rz) {
        v.ell(cx, cy, 0, rx, ry, rz, cloud(cx, cy, 0xFF7C8BD2, 0xFF3F468F));
    }

    private static void rainbowHilt(Vox v) {
        // bulut siper: bıçak dibini saran kıvrımlı bulutlar
        puff(v, 0.6F, 0.4F, 2.5F, 2.0F, 1.8F);
        puff(v, -1.6F, 2.2F, 2.7F, 2.3F, 1.9F);
        puff(v, -3.6F, 4.6F, 2.2F, 2.0F, 1.6F);
        puff(v, -4.6F, 7.0F, 1.6F, 1.6F, 1.3F);
        puff(v, -2.8F, -0.4F, 1.8F, 1.6F, 1.5F);
        puff(v, 1.6F, 2.6F, 1.4F, 1.3F, 1.2F);
        // altın sargılı kabza
        v.fill(-1.2F, -10F, -1.2F, 1.2F, -1F, 1.2F, (x, y, z) -> x * x + z * z <= 1.15F * 1.15F, (x, y, z, i, j, k) -> {
            float a = (float) Math.atan2(z, x);
            boolean wrap = fract(y * 0.55F + a / (float) (Math.PI * 2)) < 0.22F;
            int c = wrap ? 0xFFA9781E : 0xFFE9B84A;
            if (!wrap && z > 0.5F) c = lerp(c, 0xFFFFE08A, 0.35F);
            return scale(c, 0.95F + hash(i, j, k) * 0.08F);
        }, Vox.CUT, false);
        v.fill(-1.5F, -10.6F, -1.5F, 1.5F, -9.6F, 1.5F, (x, y, z) -> x * x + z * z <= 2.1F, solid(0xC99A3A), Vox.CUT, false);
        // topuz: kıvrık bulut sarmalı
        float[][] spiral = new float[22][];
        for (int n = 0; n < spiral.length; n++) {
            float s = n / (float) (spiral.length - 1);
            double a = -Math.PI / 2 + s * Math.PI * 1.8;
            float r = 2.6F - 1.9F * s;
            spiral[n] = new float[]{-0.4F + (float) Math.cos(a) * r - 1.0F, -12.2F + (float) Math.sin(a) * r * 0.9F + 0.3F, 0};
        }
        v.tube(spiral, 0.85F, solid(0x7C8BD2, 0.05F));
        v.rod(new float[]{0, -10.4F, 0}, spiral[0], 0.9F, solid(0x6F7CC4, 0.05F));
        v.ell(1.2F, -12.6F, 0, 1.1F, 1.0F, 0.9F, cloud(1.2F, -12.6F, 0xFF7C8BD2, 0xFF3F468F));
    }

    /** Bıçak boyunca kayan ışık bandı (k: 0..11). */
    private static void rainbowSheen(Vox v, int k) {
        float tc = 0.06F + 0.82F * k / 11F;
        v.fill(-17, R_Y0, -1.3F, 8, R_Y0 + R_LEN, 1.3F, (x, y, z) -> {
            float[] o = new float[2];
            if (!rIn(x, y, z, o)) return false;
            float d = o[1] - tc + o[0] * 0.08F;
            return Math.abs(d) < 0.03F;
        }, (x, y, z, i, j, k2) -> argb(0xFFFFFF, 0F), Vox.TRANS, true);
    }

    /** 4 uçlu küçük yıldız (parıltı). */
    private static void star(Vox v) {
        Vox.Col c = (x, y, z, i, j, k) -> argb(Math.abs(x) + Math.abs(y) < 0.6F ? 0xFFFFFF : 0xFFF6B0, 0F);
        v.fill(-2.2F, -0.3F, -0.3F, 2.2F, 0.3F, 0.3F, (x, y, z) -> Math.abs(y) < 0.3F + 0.25F * (1 - Math.abs(x) / 2.2F), c, Vox.TRANS, true);
        v.fill(-0.3F, -2.2F, -0.3F, 0.3F, 2.2F, 0.3F, (x, y, z) -> Math.abs(x) < 0.3F + 0.25F * (1 - Math.abs(y) / 2.2F), c, Vox.TRANS, true);
    }

    private static final float[][] STARS = {{-13.0F, 15.0F, 0.6F, 0}, {-12.0F, 22.5F, -0.4F, 1.7F}, {-8.0F, 28.6F, 0.8F, 3.1F},
            {-2.5F, 32.0F, 0.2F, 4.4F}, {-14.5F, 9.2F, 0.4F, 5.6F}};

    public static void rainbow(PoseStack ps, MultiBufferSource buf, int light, float time, float power) {
        mesh("r_blade", BladeModels::rainbowBlade).render(ps, buf, light);
        ps.pushPose();
        // bulutlar hafifçe nefes alır
        float b = 1 + 0.025F * (float) Math.sin(time * 0.12F);
        ps.translate(-1, 2, 0);
        ps.scale(b, b, b);
        ps.translate(1, -2, 0);
        mesh("r_hilt", BladeModels::rainbowHilt).render(ps, buf, light);
        ps.popPose();
        int k = (int) ((time * 0.35F) % 22);
        if (k < 12) {
            final int kk = k;
            mesh("r_sheen" + k, v -> rainbowSheen(v, kk)).render(ps, buf, light);
        }
        for (float[] s : STARS) {
            float a = (float) Math.sin(time * 0.17F + s[3]);
            float sc = Math.max(0, a) * (0.55F + 0.45F * power) + 0.35F * power;
            if (sc < 0.05F) continue;
            ps.pushPose();
            ps.translate(s[0], s[1], s[2]);
            ps.mulPose(Axis.ZP.rotationDegrees(time * 3 + s[3] * 40));
            ps.scale(sc * 0.55F, sc * 0.55F, sc * 0.55F);
            mesh("r_star", BladeModels::star).render(ps, buf, light);
            ps.popPose();
        }
    }

    // =================================================================================================== KIZIL DAMAR

    private static float cHalfWidth(float y) {
        if (y < 1 || y > 25) return 0;
        if (y < 5) return 2.1F + 0.3F * (y - 1) / 4F;
        if (y < 6.6F) return 2.4F + 1.0F * (y - 5) / 1.6F;   // aşağı bakan diken
        if (y < 13) return 2.5F + 1.6F * (float) Math.sin((y - 6.6F) / 6.4F * Math.PI / 2);
        float s = (y - 13) / 12F;
        return 4.1F * (1 - (float) Math.pow(s, 1.7));
    }

    private static float cThick(float x, float y) {
        float hw = cHalfWidth(y);
        if (hw <= 0) return 0;
        return 0.35F + 0.8F * (1 - Math.abs(x) / hw);
    }

    private static final float[][] VEINS = buildVeins();

    private static float[][] buildVeins() {
        java.util.List<float[]> s = new java.util.ArrayList<>();
        s.add(new float[]{0, 3.0F, 0, 21.0F});
        float[] ys = {8.5F, 12.5F, 16.5F};
        for (float y : ys) {
            for (int side = -1; side <= 1; side += 2) {
                float tx = side * cHalfWidth(y + 3.4F) * 0.66F;
                s.add(new float[]{0, y, tx * 0.55F, y + 2.0F});
                s.add(new float[]{tx * 0.55F, y + 2.0F, tx, y + 4.2F});
            }
        }
        // iç kontur: kenara paralel kemer
        float prevL = Float.NaN, prevY = 0;
        for (float y = 6.8F; y <= 23.4F; y += 0.8F) {
            float hx = Math.max(0, cHalfWidth(y) - 1.5F);
            if (!Float.isNaN(prevL)) {
                s.add(new float[]{-prevL, prevY, -hx, y});
                s.add(new float[]{prevL, prevY, hx, y});
            }
            prevL = hx;
            prevY = y;
        }
        return s.toArray(new float[0][]);
    }

    private static float veinDist(float x, float y) {
        float best = 99;
        for (float[] g : VEINS) {
            float dx = g[2] - g[0], dy = g[3] - g[1];
            float l2 = dx * dx + dy * dy;
            float t = l2 < 1e-6F ? 0 : clamp(((x - g[0]) * dx + (y - g[1]) * dy) / l2, 0, 1);
            float ex = x - g[0] - dx * t, ey = y - g[1] - dy * t;
            best = Math.min(best, (float) Math.sqrt(ex * ex + ey * ey));
        }
        return best;
    }

    private static boolean cIn(float x, float y, float z) {
        float hw = cHalfWidth(y);
        return hw > 0 && Math.abs(x) <= hw && Math.abs(z) <= cThick(x, y);
    }

    private static void crimsonBlade(Vox v) {
        v.fill(-4.5F, 1, -1.3F, 4.5F, 25, 1.3F, BladeModels::cIn, (x, y, z, i, j, k) -> {
            float hw = cHalfWidth(y), th = cThick(x, y);
            boolean surf = Math.abs(z) > th - 0.55F;
            float vd = surf ? veinDist(x, y) : 99;
            int c;
            if (vd < 0.3F) c = y < 6 && Math.abs(x) < 0.7F ? 0xFF6E7A2E : 0xFFC8381E;
            else if (vd < 0.55F) c = y < 6 && Math.abs(x) < 1.1F ? 0xFF4C5420 : 0xFF7A2216;
            else {
                c = 0xFFB59AA5;
                if (Math.abs(x) / hw > 0.8F) c = lerp(c, 0xFFE2D0D4, 0.6F);
                if (Math.abs(x) < 0.3F && !surf) c = 0xFF8E7480;
            }
            return scale(c, 0.95F + hash(i, j, k) * 0.09F);
        }, Vox.CUT, false);
    }

    /** Damar ışığı katmanı (seviye 1..4). */
    private static void crimsonGlow(Vox v, int level) {
        float f = level / 4F;
        v.fill(-4.5F, 1, -1.4F, 4.5F, 25, 1.4F, (x, y, z) -> {
            if (!cIn(x, y, z)) return false;
            if (Math.abs(z) <= cThick(x, y) - 0.55F) return false;
            return veinDist(x, y) < 0.3F && !(y < 6 && Math.abs(x) < 0.7F);
        }, (x, y, z, i, j, k) -> argb(lerp(0xFF5A0A00, 0xFFFF6A30, f), 0F), Vox.TRANS, true);
    }

    private static void crimsonHilt(Vox v) {
        Vox.Col olive = (x, y, z, i, j, k) -> {
            int c = 0xFF7D8C36;
            if (z > 0.6F) c = lerp(c, 0xFFB2C25E, 0.4F);
            if (z < -0.6F || y < -1.2F) c = scale(c, 0.78F);
            return scale(c, 0.94F + hash(i, j, k) * 0.1F);
        };
        // siper: ortada yaprak gövdesi, iki yanda yukarı kıvrılan boynuzlar, aşağıda küçük kıvrımlar
        v.rbox(-2.6F, -1.6F, -1.4F, 2.6F, 1.0F, 1.4F, 0.8F, olive);
        v.ell(0, 0.2F, 0, 3.4F, 1.3F, 1.2F, olive);
        for (int s = -1; s <= 1; s += 2) {
            float[][] horn = new float[9][];
            for (int n = 0; n < horn.length; n++) {
                float t = n / (float) (horn.length - 1);
                horn[n] = new float[]{s * (3.0F + 1.2F * (float) Math.sin(t * 2.2)), -0.2F + 4.2F * t, 0};
            }
            v.tube(horn, 0.75F, olive);
            v.ell(s * 3.2F, 3.9F, 0, 0.6F, 0.9F, 0.6F, olive);
            float[][] curl = {{s * 2.2F, -1.2F, 0}, {s * 3.4F, -1.8F, 0}, {s * 3.9F, -1.0F, 0}, {s * 3.4F, -0.4F, 0}};
            v.tube(curl, 0.5F, olive);
        }
        // yakut yuvası
        v.ell(0, -0.3F, 1.0F, 1.25F, 1.25F, 0.7F, solid(0x4C5420));
        // altın kabza ve halkalar
        v.fill(-1.1F, -9.5F, -1.1F, 1.1F, -1.4F, 1.1F, (x, y, z) -> x * x + z * z <= sq(0.95F + 0.12F * (float) Math.sin((y + 9.5F) / 8.1F * Math.PI)),
                (x, y, z, i, j, k) -> {
                    int c = 0xFFD9A63A;
                    if (z > 0.4F) c = lerp(c, 0xFFFFDD80, 0.4F);
                    if (fract(y * 0.8F) < 0.12F) c = scale(c, 0.8F);
                    return scale(c, 0.95F + hash(i, j, k) * 0.08F);
                }, Vox.CUT, false);
        for (float y : new float[]{-2.0F, -9.0F}) v.fill(-1.5F, y - 0.4F, -1.5F, 1.5F, y + 0.4F, 1.5F, (x, yy, z) -> x * x + z * z <= 1.9F,
                Vox.metal(0xC8902A), Vox.CUT, false);
        // sivri altın topuz
        v.lathe(new float[][]{{1.35F, -9.4F}, {1.1F, -10.3F}, {0.6F, -11.8F}, {0.25F, -13.0F}, {0F, -13.6F}}, 1, 1, 8, 0,
                Vox.metal(0xD9A63A), Vox.CUT, false);
    }

    private static float sq(float f) {
        return f * f;
    }

    private static void ruby(Vox v) {
        v.ell(0, 0, 0, 0.95F, 0.95F, 0.55F, (x, y, z, i, j, k) -> {
            int c = x + y > 0.4F ? 0xFFFF8A70 : 0xFFD0201A;
            return c;
        }, Vox.CUT, true);
    }

    public static void crimson(PoseStack ps, MultiBufferSource buf, int light, float time, float essence) {
        mesh("c_blade", BladeModels::crimsonBlade).render(ps, buf, light);
        mesh("c_hilt", BladeModels::crimsonHilt).render(ps, buf, light);
        // damarlar özle dolar: 0 = sönük nabız, 1 = tam ışıl
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * (0.12F + 0.25F * essence));
        int level = (int) clamp(Math.round(essence * 3 + pulse * (0.6F + essence)), 0, 4);
        if (level > 0) {
            final int lv = level;
            mesh("c_glow" + level, v -> crimsonGlow(v, lv)).render(ps, buf, light);
        }
        ps.pushPose();
        ps.translate(0, -0.3F, 1.45F);
        float s = 1 + 0.08F * pulse;
        ps.scale(s, s, s);
        mesh("c_ruby", BladeModels::ruby).render(ps, buf, light);
        ps.popPose();
    }

    // =================================================================================================== YOSUNKÖK SATIRI

    private static final float M_Y0 = 2.0F, M_LEN = 28.0F;

    private static float mCenter(float t) {
        return 1.6F + 2.3F * (float) Math.sin(Math.PI * t * 1.1) - 4.2F * t * t;
    }

    private static float mWidth(float t) {
        float w = 6.4F + 1.8F * (float) Math.sin(Math.PI * Math.min(1, t * 1.25));
        if (t > 0.82F) w *= (float) Math.pow(Math.max(0, (1 - t) / 0.18F), 0.7);
        return w;
    }

    private static boolean mIn(float x, float y, float z, float[] out) {
        float t = (y - M_Y0) / M_LEN;
        if (t < 0 || t > 1) return false;
        float w = mWidth(t);
        if (w < 0.4F) return false;
        float c = mCenter(t) - (t > 0.82F ? (t - 0.82F) * 9F : 0);
        // sırt (sağ) dalgalı, ağız (sol) içbükey
        float wave = 0.35F * (float) Math.sin(t * 31);
        float u = (x - c) / w;
        if (u > 0.5F + wave / w || u < -0.5F) return false;
        // ağızda çentikler
        if (u < -0.4F && (Math.abs(t - 0.34F) < 0.02F || Math.abs(t - 0.58F) < 0.025F)) return false;
        if (u > 0.42F && Math.abs(t - 0.7F) < 0.02F) return false;
        float th = 1.6F - 1.0F * clamp(-u - 0.1F, 0, 0.4F) / 0.4F;
        if (Math.abs(z) > th) return false;
        if (out != null) {
            out[0] = u;
            out[1] = t;
        }
        return true;
    }

    private static void mossBlade(Vox v) {
        float[] o = new float[2];
        v.fill(-10, M_Y0, -1.8F, 10, M_Y0 + M_LEN, 1.8F, (x, y, z) -> mIn(x, y, z, null), (x, y, z, i, j, k) -> {
            mIn(x, y, z, o);
            float u = o[0], t = o[1];
            int c = 0xFF9C8A92;
            // yontulmuş taş yüzeyler
            float facet = hash((int) Math.floor(x / 2.2F), (int) Math.floor(y / 2.6F), 7);
            c = scale(c, 0.9F + facet * 0.2F);
            if (u > 0.36F) c = lerp(c, 0xFFC4B4BA, 0.5F);
            if (u < -0.42F) c = lerp(c, 0xFFD6CACE, 0.55F);
            // çatlaklar
            float cr = Math.abs(fract(x * 0.23F + y * 0.11F + (float) Math.sin(y * 0.7F) * 0.4F) - 0.5F);
            if (cr < 0.03F && t > 0.3F && Math.abs(z) > 1.1F) c = 0xFF5E4E56;
            // gövdeye yakın yosun lekeleri
            float moss = hash(i / 3, j / 3, k / 3);
            if (t < 0.5F && moss > 0.62F - (0.5F - t) * 0.7F && Math.abs(z) > 0.9F) c = moss > 0.85F ? 0xFF9CC23E : 0xFF6E9A2A;
            return scale(c, 0.96F + hash(i, j, k) * 0.07F);
        }, Vox.CUT, false);
    }

    private static final Vox.Col ROOT = (x, y, z, i, j, k) -> {
        float h = hash(i, j, k);
        int c = (j & 3) == 0 ? 0xFF6A2E1E : 0xFF8E4630;
        if (h > 0.9F) c = 0xFFB0603C;
        return scale(c, 0.92F + h * 0.12F);
    };

    private static final Vox.Col MOSS = (x, y, z, i, j, k) -> {
        float h = hash(i, j, k);
        return h > 0.82F ? 0xFFB4D85A : h < 0.2F ? 0xFF4F7A1E : 0xFF7BAA30;
    };

    /** Bıçağa sarılan kökler (taş yüzeyinde sarmal). */
    private static float[][] climb(float t0, float t1, float a0, float turns, int n) {
        float[][] p = new float[n + 1][];
        for (int q = 0; q <= n; q++) {
            float s = q / (float) n;
            float t = t0 + (t1 - t0) * s;
            float y = M_Y0 + M_LEN * t;
            double a = a0 + turns * Math.PI * 2 * s;
            float c = mCenter(t), w = mWidth(t) * 0.55F;
            p[q] = new float[]{c + (float) Math.cos(a) * w, y, (float) Math.sin(a) * 2.1F};
        }
        return p;
    }

    private static void mossRoots(Vox v) {
        v.tube(climb(0.0F, 0.42F, 0.3F, 1.3F, 30), 0.85F, ROOT);
        v.tube(climb(0.02F, 0.30F, 2.6F, 0.9F, 24), 0.75F, ROOT);
        v.tube(climb(0.05F, 0.36F, 4.4F, 1.1F, 26), 0.6F, ROOT);
        v.tube(climb(0.0F, 0.18F, 1.4F, 0.7F, 16), 1.0F, ROOT);
        // köklerin dibinde kalın düğüm
        v.ell(mCenter(0.03F), M_Y0 + 1.2F, 0, 3.6F, 2.4F, 2.4F, ROOT);
        // yosun öbekleri ve yaprak filizleri
        float[][] lumps = {{0.08F, 0.6F, 2.0F}, {0.16F, -0.4F, 2.1F}, {0.24F, 0.3F, -2.0F}, {0.31F, 0.1F, 1.9F}, {0.12F, 0.0F, -2.2F}, {0.38F, -0.3F, 1.7F}};
        for (float[] l : lumps) {
            float t = l[0];
            float x = mCenter(t) + l[1] * mWidth(t) * 0.6F;
            v.ell(x, M_Y0 + M_LEN * t, l[2], 1.6F, 1.3F, 0.9F, MOSS);
        }
        float[][] sprouts = {{0.2F, 0.55F, 1, 2.2F}, {0.28F, 0.6F, -1, 1.6F}, {0.1F, -0.55F, 1, -2.0F}, {0.35F, 0.5F, 1, -1.8F}};
        for (float[] s : sprouts) {
            float t = s[0];
            float x = mCenter(t) + s[1] * mWidth(t), y = M_Y0 + M_LEN * t;
            v.rod(new float[]{x, y, 0}, new float[]{x + s[3] * 1.3F, y + 2.6F, s[2] * 0.6F}, 0.35F, solid(0x9CCB45));
        }
    }

    private static void mossHandle(Vox v) {
        // birbirine dolanan üç kökten kabza
        for (int q = 0; q < 3; q++) {
            float[][] p = new float[20][];
            for (int n = 0; n < p.length; n++) {
                float s = n / (float) (p.length - 1);
                double a = q * Math.PI * 2 / 3 + s * Math.PI * 3;
                float y = 2.5F - 13.5F * s;
                float lean = 1.2F * s;
                p[n] = new float[]{lean + (float) Math.cos(a) * 0.75F, y, (float) Math.sin(a) * 0.75F};
            }
            v.tube(p, 0.72F, ROOT);
        }
        v.ell(0.2F, -4.5F, 0.6F, 0.9F, 1.1F, 0.6F, MOSS);
    }

    /** Alttaki pençe gibi kök uçları (her biri ayrı, sallanır). */
    private static void mossClaw(Vox v, int n) {
        float[][] dirs = {{-2.6F, -2.0F}, {-0.6F, -3.4F}, {1.6F, -2.6F}, {3.0F, -1.0F}};
        float[] d = dirs[n];
        float[][] p = {{0, 0, 0}, {d[0] * 0.5F, d[1] * 0.6F, 0.3F}, {d[0] * 0.9F, d[1] * 0.9F, 0.2F}, {d[0] * 1.05F + 0.6F, d[1] * 1.15F + 0.5F, 0}};
        v.tube(p, 0.55F, ROOT);
        v.rod(p[2], new float[]{p[3][0] + 0.4F, p[3][1] + 0.8F, 0}, 0.28F, solid(0x5A2618));
        if (n % 2 == 0) v.rod(new float[]{d[0] * 0.4F, d[1] * 0.4F, 0.4F}, new float[]{d[0] * 0.4F - 1.2F, d[1] * 0.4F - 1.6F, 1.0F}, 0.3F,
                solid(0x9CCB45));
    }

    private static void mossGlow(Vox v) {
        float[][] lumps = {{0.08F, 0.6F, 2.0F}, {0.16F, -0.4F, 2.1F}, {0.24F, 0.3F, -2.0F}, {0.31F, 0.1F, 1.9F}, {0.12F, 0.0F, -2.2F}, {0.38F, -0.3F, 1.7F}};
        for (float[] l : lumps) {
            float t = l[0];
            float x = mCenter(t) + l[1] * mWidth(t) * 0.6F;
            v.ell(x, M_Y0 + M_LEN * t, l[2], 1.7F, 1.4F, 1.0F, (xx, y, z, i, j, k) -> argb(0x9CFF40, 0F), Vox.TRANS, true);
        }
    }

    public static void mossroot(PoseStack ps, MultiBufferSource buf, int light, float time, float charge) {
        ps.pushPose();
        if (charge > 0) ps.translate(Math.sin(time * 2.9) * 0.25F * charge, 0, Math.cos(time * 3.3) * 0.2F * charge);
        mesh("m_blade", BladeModels::mossBlade).render(ps, buf, light);
        mesh("m_roots", BladeModels::mossRoots).render(ps, buf, light);
        mesh("m_handle", BladeModels::mossHandle).render(ps, buf, light);
        for (int n = 0; n < 4; n++) {
            final int nn = n;
            ps.pushPose();
            ps.translate(1.2F + (n - 1.5F) * 0.4F, -10.6F, 0);
            float sway = (float) Math.sin(time * 0.09F + n * 1.7F) * (6 + 14 * charge);
            ps.mulPose(Axis.ZP.rotationDegrees(sway));
            ps.mulPose(Axis.XP.rotationDegrees((float) Math.cos(time * 0.07F + n) * 8));
            mesh("m_claw" + n, v -> mossClaw(v, nn)).render(ps, buf, light);
            ps.popPose();
        }
        if (charge > 0.05F || Math.sin(time * 0.05F) > 0.92F) mesh("m_glow", BladeModels::mossGlow).render(ps, buf, light);
        ps.popPose();
    }
}
