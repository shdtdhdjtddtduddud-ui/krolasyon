package com.krolasyon.mythblades.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Üç efsane kılıcın pürüzsüz, dokulu 3B modelleri (birim: piksel; bıçak +Y, siper y ≈ 0, ön yüz +Z).
 * Bıçaklar bombeli kesitli parametrik yüzeylerdir; desenler (gökkuşağı bantları, kızıl damarlar, yontulmuş taş)
 * dokuya boyalıdır. Kökler, bulut kıvrımları ve boynuzlar pürüzsüz borulardır.
 */
public final class BladeModels {
    private BladeModels() {}

    // dalga (hilal) voksel ağı için eski önbellek
    private static final Map<String, Vox.Mesh> CACHE = new HashMap<>();

    static Vox.Mesh mesh(String key, Consumer<Vox> build) {
        return CACHE.computeIfAbsent(key, k -> {
            Vox v = new Vox();
            build.accept(v);
            return v.bake();
        });
    }

    private static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    private static final float[] WHITE = {1, 1, 1, 1};

    private static float[] rgb(int c, float a) {
        return new float[]{((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F, a};
    }

    /** Bombeli kesit: kenarlarda 0, sırta (s) doğru kabaran; p büyüdükçe daha düz yüz. */
    private static float lens(float u, float spine, float p) {
        float d = u < spine ? u / spine : (1 - u) / (1 - spine);
        return (float) Math.pow(Math.sin(Math.PI / 2 * clamp(d, 0, 1)), p);
    }

    // =================================================================================================== GÖKKUŞAĞI PALASI

    private static final float R_Y0 = 0.6F, R_LEN = 30.0F;

    private static float rCenter(float t) {
        return -8.5F * (float) Math.sin(Math.PI * t * 0.95) + 3.2F * t * t;
    }

    private static float rWidth(float t) {
        float w = 6.0F + 1.6F * (float) Math.sin(Math.PI * Math.min(1, t * 1.1));
        if (t > 0.8F) w *= (float) Math.sqrt(Math.max(0, (1 - t) / 0.2F));
        return Math.max(w, 0.02F);
    }

    private static final Geo.Blade RAINBOW_BLADE = (t, u, o) -> {
        float w = rWidth(t);
        float c = rCenter(t) - (t > 0.8F ? (t - 0.8F) * 6F : 0);
        o[0] = c - w / 2 + w * u;
        o[1] = R_Y0 + R_LEN * t;
        float tip = t > 0.86F ? (1 - t) / 0.14F : 1;
        o[2] = 0.95F * lens(u, 0.18F, 0.55F) * (0.35F + 0.65F * tip) + 0.02F;
    };

    private static void rainbowBlade(MultiBufferSource buf, PoseStack ps, int light, float time, float power) {
        TextureAtlasSprite s = Geo.sprite("rainbow_blade");
        Geo.blade(buf.getBuffer(Geo.CUT), ps, 44, 10, RAINBOW_BLADE, s, null, light, 0);
        // iç ışıltı + bıçak boyunca kayan parıltı bandı
        float tc = ((time * 0.022F) % 1.6F) - 0.2F;
        float base = 0.10F + 0.35F * power;
        Geo.blade(buf.getBuffer(Geo.GLOW), ps, 44, 6, RAINBOW_BLADE, s, (t, u, c) -> {
            float g = base + 0.75F * (float) Math.exp(-sq((t - tc + (u - 0.5F) * 0.12F) / 0.05F));
            c[0] = c[1] = c[2] = g;
        }, Geo.FULL, 0.03F);
    }

    private static float sq(float f) {
        return f * f;
    }

    private static final int CLOUD = 0x7C8BD2;

    /** Bulut kabarcığı: elipsoit + ön yüzünde koyu sarmal çizgi. */
    private static void puff(VertexConsumer vc, PoseStack ps, int light, float cx, float cy, float rx, float ry, float rz, float spin) {
        TextureAtlasSprite cl = Geo.sprite("cloud");
        Geo.ellipsoid(vc, ps, cx, cy, 0, rx, ry, rz, 10, 14, cl, null, light);
        List<float[]> sp = new ArrayList<>();
        for (int i = 0; i <= 22; i++) {
            float s = i / 22F;
            double a = spin + s * Math.PI * 3.2;
            float r = 0.12F + 0.72F * s;
            float x = (float) Math.cos(a) * r, y = (float) Math.sin(a) * r;
            float z = (float) Math.sqrt(Math.max(0, 1 - x * x - y * y));
            sp.add(new float[]{cx + x * rx, cy + y * ry, z * rz + 0.05F, 0.17F + 0.06F * s});
        }
        Geo.tube(vc, ps, sp.toArray(new float[0][]), 5, Geo.sprite("m_plain"), rgb(0x3E4592, 1), light);
    }

    private static void rainbowHilt(MultiBufferSource buf, PoseStack ps, int light, float time) {
        VertexConsumer vc = buf.getBuffer(Geo.CUT);
        float b = 1 + 0.03F * (float) Math.sin(time * 0.12F);
        ps.pushPose();
        ps.translate(-1, 2, 0);
        ps.scale(b, b, b);
        ps.translate(1, -2, 0);
        puff(vc, ps, light, 0.8F, 0.3F, 2.5F, 2.0F, 1.8F, 0.3F);
        puff(vc, ps, light, -1.4F, 2.3F, 2.8F, 2.4F, 2.0F, 1.7F);
        puff(vc, ps, light, -3.7F, 4.8F, 2.3F, 2.1F, 1.7F, 3.1F);
        puff(vc, ps, light, -5.0F, 7.3F, 1.7F, 1.6F, 1.35F, 0.9F);
        puff(vc, ps, light, -2.9F, -0.5F, 1.9F, 1.6F, 1.5F, 2.2F);
        puff(vc, ps, light, 1.9F, 2.8F, 1.5F, 1.35F, 1.2F, 4.0F);
        ps.popPose();
        // altın sargılı kabza ve alt bilezik
        Geo.lathe(vc, ps, new float[][]{{1.0F, -10F}, {1.12F, -8F}, {1.05F, -5.5F}, {1.12F, -3F}, {1.0F, -0.8F}}, 14, Geo.sprite("gold_wrap"), null, light);
        Geo.lathe(vc, ps, new float[][]{{0.4F, -11.0F}, {1.45F, -10.7F}, {1.5F, -10.0F}, {1.2F, -9.6F}}, 14, Geo.sprite("gold"), null, light);
        // topuz: kıvrık bulut sarmalı
        float[][] sp = new float[16][];
        for (int i = 0; i < sp.length; i++) {
            float s = i / (float) (sp.length - 1);
            double a = -Math.PI / 2 + s * Math.PI * 1.9;
            float r = 2.6F - 1.8F * s;
            sp[i] = new float[]{-1.4F + (float) Math.cos(a) * r, -12.5F + (float) Math.sin(a) * r * 0.9F, 0, 1.05F - 0.55F * s};
        }
        float[][] full = new float[sp.length + 1][];
        full[0] = new float[]{0, -10.6F, 0, 0.9F};
        System.arraycopy(sp, 0, full, 1, sp.length);
        Geo.tube(vc, ps, Geo.spline(full, 3), 10, Geo.sprite("cloud"), WHITE, light);
        puff(vc, ps, light, 1.1F, -13.0F, 1.2F, 1.1F, 1.0F, 1.2F);
    }

    private static final float[][] STARS = {{-13.0F, 15.0F, 0.9F, 0}, {-12.0F, 22.5F, -0.6F, 1.7F}, {-8.0F, 28.6F, 1.0F, 3.1F},
            {-2.5F, 32.0F, 0.4F, 4.4F}, {-14.5F, 9.2F, 0.7F, 5.6F}};

    private static void stars(MultiBufferSource buf, PoseStack ps, float time, float power) {
        VertexConsumer vc = buf.getBuffer(Geo.GLOW);
        TextureAtlasSprite w = Geo.sprite("m_plain");
        for (float[] s : STARS) {
            float a = (float) Math.sin(time * 0.17F + s[3]);
            float sc = Math.max(0, a) * (0.6F + 0.4F * power) + 0.35F * power;
            if (sc < 0.05F) continue;
            ps.pushPose();
            ps.translate(s[0], s[1], s[2]);
            ps.mulPose(Axis.ZP.rotationDegrees(time * 3 + s[3] * 40));
            ps.scale(sc, sc, sc);
            float[] c = {1, 0.96F, 0.7F, 1};
            Geo.tube(vc, ps, new float[][]{{-1.6F, 0, 0, 0.02F}, {0, 0, 0, 0.32F}, {1.6F, 0, 0, 0.02F}}, 6, w, c, Geo.FULL);
            Geo.tube(vc, ps, new float[][]{{0, -1.6F, 0, 0.02F}, {0, 0, 0, 0.32F}, {0, 1.6F, 0, 0.02F}}, 6, w, c, Geo.FULL);
            ps.popPose();
        }
    }

    public static void rainbow(PoseStack ps, MultiBufferSource buf, int light, float time, float power) {
        rainbowBlade(buf, ps, light, time, power);
        rainbowHilt(buf, ps, light, time);
        stars(buf, ps, time, power);
    }

    // =================================================================================================== KIZIL DAMAR

    private static float cHalfWidth(float y) {
        if (y < 1 || y > 25) return 0.02F;
        if (y < 5) return 2.1F + 0.3F * (y - 1) / 4F;
        if (y < 6.6F) return 2.4F + 1.0F * (y - 5) / 1.6F;
        if (y < 13) return 2.5F + 1.6F * (float) Math.sin((y - 6.6F) / 6.4F * Math.PI / 2);
        float s = (y - 13) / 12F;
        return Math.max(0.02F, 4.1F * (1 - (float) Math.pow(s, 1.7)));
    }

    private static final Geo.Blade CRIMSON_BLADE = (t, u, o) -> {
        float y = 1 + 24 * t;
        float hw = cHalfWidth(y);
        o[0] = (u * 2 - 1) * hw;
        o[1] = y;
        // elmas kesit: ortada belirgin sırt, kenarlarda keskin
        float d = 1 - Math.abs(u * 2 - 1);
        float tip = t > 0.85F ? (1 - t) / 0.15F : 1;
        o[2] = (0.12F + 0.95F * (float) Math.pow(d, 0.85)) * (0.4F + 0.6F * tip) * 0.95F;
    };

    private static void crimsonHilt(MultiBufferSource buf, PoseStack ps, int light) {
        VertexConsumer vc = buf.getBuffer(Geo.CUT);
        TextureAtlasSprite ol = Geo.sprite("olive");
        // yaprak gövdeli siper
        Geo.ellipsoid(vc, ps, 0, 0.1F, 0, 3.3F, 1.35F, 1.25F, 10, 16, ol, null, light);
        Geo.ellipsoid(vc, ps, 0, -1.3F, 0, 1.9F, 1.1F, 1.15F, 8, 14, ol, null, light);
        for (int s = -1; s <= 1; s += 2) {
            // yukarı kıvrılan boynuz (bıçak dibini sarar)
            Geo.tube(vc, ps, Geo.spline(new float[][]{{s * 2.4F, -0.3F, 0, 0.95F}, {s * 3.6F, 0.6F, 0.1F, 0.85F}, {s * 4.1F, 2.2F, 0, 0.65F},
                    {s * 3.7F, 3.9F, -0.1F, 0.45F}, {s * 3.0F, 5.0F, 0, 0.2F}}, 5), 10, ol, WHITE, light);
            // aşağı kıvrım
            Geo.tube(vc, ps, Geo.spline(new float[][]{{s * 1.6F, -1.5F, 0, 0.7F}, {s * 3.0F, -2.1F, 0.1F, 0.55F}, {s * 3.7F, -1.3F, 0, 0.4F},
                    {s * 3.3F, -0.6F, 0, 0.22F}}, 5), 8, ol, WHITE, light);
        }
        // bıçağa çıkan yeşil gövde
        Geo.tube(vc, ps, Geo.spline(new float[][]{{0, 0.5F, 0.7F, 0.6F}, {0, 3.0F, 0.75F, 0.42F}, {0, 5.5F, 0.85F, 0.12F}}, 4), 8, ol, WHITE, light);
        Geo.tube(vc, ps, Geo.spline(new float[][]{{0, 0.5F, -0.7F, 0.6F}, {0, 3.0F, -0.75F, 0.42F}, {0, 5.5F, -0.85F, 0.12F}}, 4), 8, ol, WHITE, light);
        // yakut yuvası
        Geo.ellipsoid(vc, ps, 0, -0.35F, 0.9F, 1.3F, 1.3F, 0.6F, 8, 12, ol, (t, u, c) -> { c[0] = c[1] = c[2] = 0.6F; }, light);
        // altın kabza: halkalı ve ortası hafif şişkin
        Geo.lathe(vc, ps, new float[][]{{1.25F, -1.2F}, {1.45F, -1.6F}, {1.45F, -2.4F}, {1.0F, -2.8F}, {1.12F, -5.5F}, {1.0F, -8.2F},
                {1.45F, -8.6F}, {1.45F, -9.4F}, {1.1F, -9.8F}}, 14, Geo.sprite("gold"), null, light);
        // sivri topuz
        Geo.lathe(vc, ps, new float[][]{{1.1F, -9.8F}, {1.35F, -10.3F}, {1.0F, -11.2F}, {0.55F, -12.3F}, {0.0F, -13.8F}}, 8, Geo.sprite("gold"), null, light);
    }

    public static void crimson(PoseStack ps, MultiBufferSource buf, int light, float time, float essence) {
        TextureAtlasSprite bl = Geo.sprite("crimson_blade");
        Geo.blade(buf.getBuffer(Geo.CUT), ps, 64, 10, CRIMSON_BLADE, bl, null, light, 0);
        crimsonHilt(buf, ps, light);
        // damarlar özle dolar ve nabız gibi atar
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * (0.12F + 0.25F * essence));
        float g = 0.18F + 0.25F * pulse + 0.6F * essence;
        Geo.blade(buf.getBuffer(Geo.GLOW), ps, 64, 10, CRIMSON_BLADE, Geo.sprite("crimson_glow"), (t, u, c) -> {
            c[0] = c[1] = c[2] = clamp(g, 0, 1);
        }, Geo.FULL, 0.02F);
        // yakut
        float s = 1 + 0.08F * pulse;
        ps.pushPose();
        ps.translate(0, -0.35F, 1.35F);
        ps.scale(s, s, s);
        Geo.ellipsoid(buf.getBuffer(Geo.CUT), ps, 0, 0, 0, 0.95F, 0.95F, 0.55F, 8, 12, Geo.sprite("ruby"), null, Geo.FULL);
        Geo.ellipsoid(buf.getBuffer(Geo.GLOW), ps, 0, 0, 0, 1.0F, 1.0F, 0.6F, 8, 12, Geo.sprite("ruby"), (t, u, c) -> {
            c[0] = c[1] = c[2] = 0.35F + 0.4F * pulse;
        }, Geo.FULL);
        ps.popPose();
    }

    // =================================================================================================== YOSUNKÖK SATIRI

    private static final float M_Y0 = 1.6F, M_LEN = 28.4F;

    private static float mCenter(float t) {
        return 1.6F + 2.3F * (float) Math.sin(Math.PI * t * 1.1) - 4.2F * t * t;
    }

    private static float mWidth(float t) {
        float w = 6.4F + 1.8F * (float) Math.sin(Math.PI * Math.min(1, t * 1.25));
        if (t > 0.82F) w *= (float) Math.pow(Math.max(0, (1 - t) / 0.18F), 0.7);
        return Math.max(w, 0.02F);
    }

    private static final Geo.Blade MOSS_BLADE = (t, u, o) -> {
        float w = mWidth(t);
        float c = mCenter(t) - (t > 0.82F ? (t - 0.82F) * 9F : 0);
        // sırt (u = 1) dalgalı
        float wave = 0.35F * (float) Math.sin(t * 31) * u;
        o[0] = c - w / 2 + w * u + wave;
        o[1] = M_Y0 + M_LEN * t;
        float tip = t > 0.86F ? (1 - t) / 0.14F : 1;
        o[2] = (1.75F * lens(u, 0.7F, 0.35F) + 0.04F) * (0.45F + 0.55F * tip);
    };

    private static float[] mSurf(float t, float a, float out) {
        float c = mCenter(t), w = mWidth(t);
        float x = c + (float) Math.cos(a) * (w * 0.5F + out);
        float z = (float) Math.sin(a) * (1.8F + out);
        return new float[]{x, M_Y0 + M_LEN * t, z};
    }

    /** Bıçağa sarmal tırmanan kök. */
    private static float[][] climb(float t0, float t1, float a0, float turns, float r0, float r1, float time, int idx, float sway) {
        int n = 26;
        float[][] p = new float[n + 1][];
        for (int q = 0; q <= n; q++) {
            float s = q / (float) n;
            float t = t0 + (t1 - t0) * s;
            float r = (r0 + (r1 - r0) * s) * (1 + 0.2F * (float) Math.sin(s * 31 + idx * 2.1));
            double a = a0 + turns * Math.PI * 2 * s + Math.sin(s * 9 + idx) * 0.15;
            float[] q3 = mSurf(t, (float) a, r * 0.6F);
            float tipW = s > 0.8F ? (s - 0.8F) * 5 : 0;
            q3[0] += (float) Math.sin(time * 0.09 + idx * 1.7) * 0.6F * tipW * tipW * sway;
            q3[2] += (float) Math.cos(time * 0.07 + idx) * 0.5F * tipW * tipW * sway;
            p[q] = new float[]{q3[0], q3[1], q3[2], r};
        }
        return p;
    }

    private static final float[] BARK_TINT = {1.0F, 0.95F, 0.92F, 1};

    private static void mossRoots(MultiBufferSource buf, PoseStack ps, int light, float time, float charge) {
        VertexConsumer vc = buf.getBuffer(Geo.CUT);
        TextureAtlasSprite bark = Geo.sprite("bark"), moss = Geo.sprite("moss"), leaf = Geo.sprite("leaf");
        float sway = 1 + 1.5F * charge;
        Geo.tube(vc, ps, climb(0.0F, 0.44F, 0.3F, 1.25F, 0.95F, 0.25F, time, 0, sway), 9, bark, BARK_TINT, light);
        Geo.tube(vc, ps, climb(0.01F, 0.32F, 2.6F, 0.9F, 0.8F, 0.2F, time, 1, sway), 8, bark, BARK_TINT, light);
        Geo.tube(vc, ps, climb(0.04F, 0.38F, 4.4F, 1.1F, 0.65F, 0.15F, time, 2, sway), 8, bark, BARK_TINT, light);
        Geo.tube(vc, ps, climb(0.0F, 0.2F, 1.4F, 0.7F, 1.05F, 0.35F, time, 3, sway), 9, bark, BARK_TINT, light);
        // köklerin bıçak dibindeki kalın düğümü
        Geo.ellipsoid(vc, ps, mCenter(0.03F), M_Y0 + 1.0F, 0, 3.9F, 2.3F, 2.5F, 10, 16, bark, (t, u, c) -> {
            c[0] = 0.92F; c[1] = 0.86F; c[2] = 0.84F;
        }, light);
        // yosun öbekleri
        float[][] lumps = {{0.08F, 0.6F, 1}, {0.16F, -0.35F, 1}, {0.24F, 0.3F, -1}, {0.31F, 0.1F, 1}, {0.12F, 0.0F, -1}, {0.38F, -0.3F, 1},
                {0.02F, -0.5F, 1}};
        for (float[] l : lumps) {
            float t = l[0];
            float x = mCenter(t) + l[1] * mWidth(t) * 0.6F;
            Geo.ellipsoid(vc, ps, x, M_Y0 + M_LEN * t, l[2] * 1.75F, 1.7F, 1.25F, 0.85F, 6, 10, moss, null, light);
        }
        // filizler: uçta yaprakçıklar
        float[][] sprouts = {{0.2F, 0.55F, 1, 2.0F}, {0.28F, 0.6F, -1, 1.6F}, {0.1F, -0.55F, 1, -2.0F}, {0.35F, 0.5F, 1, -1.8F}, {0.05F, 0.6F, -1, 2.2F}};
        int k = 0;
        for (float[] s : sprouts) {
            float t = s[0];
            float x = mCenter(t) + s[1] * mWidth(t), y = M_Y0 + M_LEN * t;
            float sw = (float) Math.sin(time * 0.11F + k++) * 0.3F * sway;
            Geo.tube(vc, ps, Geo.spline(new float[][]{{x, y, s[2] * 1.2F, 0.3F}, {x + s[3] * 0.6F + sw, y + 1.4F, s[2] * 1.5F, 0.22F},
                    {x + s[3] * 1.1F + sw * 1.6F, y + 2.6F, s[2] * 1.7F, 0.04F}}, 4), 6, leaf, WHITE, light);
            Geo.ellipsoid(vc, ps, x + s[3] * 1.2F + sw * 1.6F, y + 2.4F, s[2] * 1.7F, 0.55F, 0.28F, 0.55F, 5, 8, leaf, null, light);
        }
    }

    private static void mossHandle(MultiBufferSource buf, PoseStack ps, int light, float time, float charge) {
        VertexConsumer vc = buf.getBuffer(Geo.CUT);
        TextureAtlasSprite bark = Geo.sprite("bark");
        // birbirine dolanan üç kökten kabza
        for (int q = 0; q < 3; q++) {
            float[][] p = new float[22][];
            for (int n = 0; n < p.length; n++) {
                float s = n / (float) (p.length - 1);
                double a = q * Math.PI * 2 / 3 + s * Math.PI * 3;
                float y = 2.4F - 13.2F * s;
                p[n] = new float[]{1.2F * s + (float) Math.cos(a) * 0.72F, y, (float) Math.sin(a) * 0.72F, 0.75F - 0.1F * s};
            }
            Geo.tube(vc, ps, p, 9, bark, BARK_TINT, light);
        }
        Geo.ellipsoid(vc, ps, 0.4F, -4.5F, 0.7F, 0.95F, 1.15F, 0.6F, 6, 10, Geo.sprite("moss"), null, light);
        // alttaki pençe kökler (sallanır)
        float[][] dirs = {{-2.6F, -2.0F}, {-0.6F, -3.4F}, {1.6F, -2.6F}, {3.0F, -1.0F}};
        for (int n = 0; n < 4; n++) {
            float[] d = dirs[n];
            ps.pushPose();
            ps.translate(1.2F + (n - 1.5F) * 0.4F, -10.6F, 0);
            ps.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time * 0.09F + n * 1.7F) * (6 + 14 * charge)));
            ps.mulPose(Axis.XP.rotationDegrees((float) Math.cos(time * 0.07F + n) * 8));
            Geo.tube(vc, ps, Geo.spline(new float[][]{{0, 0, 0, 0.7F}, {d[0] * 0.5F, d[1] * 0.6F, 0.3F, 0.55F}, {d[0] * 0.95F, d[1] * 0.95F, 0.2F, 0.35F},
                    {d[0] * 1.05F + 0.7F, d[1] * 1.1F + 0.6F, 0, 0.05F}}, 4), 8, bark, BARK_TINT, light);
            if (n % 2 == 0) Geo.tube(vc, ps, Geo.spline(new float[][]{{d[0] * 0.4F, d[1] * 0.4F, 0.4F, 0.25F},
                    {d[0] * 0.4F - 0.8F, d[1] * 0.4F - 1.0F, 0.8F, 0.15F}, {d[0] * 0.4F - 1.3F, d[1] * 0.4F - 1.7F, 1.0F, 0.03F}}, 3), 5,
                    Geo.sprite("leaf"), WHITE, light);
            ps.popPose();
        }
    }

    public static void mossroot(PoseStack ps, MultiBufferSource buf, int light, float time, float charge) {
        ps.pushPose();
        // ağız ve sivri uç ileriye (vuruş yönüne) baksın
        ps.scale(-1, 1, 1);
        if (charge > 0) ps.translate(Math.sin(time * 2.9) * 0.25F * charge, 0, Math.cos(time * 3.3) * 0.2F * charge);
        TextureAtlasSprite st = Geo.sprite("stone_blade");
        Geo.blade(buf.getBuffer(Geo.CUT), ps, 48, 12, MOSS_BLADE, st, null, light, 0);
        mossRoots(buf, ps, light, time, charge);
        mossHandle(buf, ps, light, time, charge);
        if (charge > 0.02F) {
            float g = 0.15F + 0.6F * charge;
            Geo.blade(buf.getBuffer(Geo.TRANS), ps, 24, 6, MOSS_BLADE, Geo.sprite("moss"), (t, u, c) -> {
                float f = clamp(1 - t * 2.2F, 0, 1) * g;
                c[0] = 0.7F;
                c[1] = 1F;
                c[2] = 0.4F;
                c[3] = f;
            }, Geo.FULL, 0.05F);
        }
        ps.popPose();
    }
}
