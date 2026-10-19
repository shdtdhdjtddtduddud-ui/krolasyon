package com.krolasyon.emeraldblade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.ArrayList;
import java.util.List;

/**
 * The Emerald Root Blade, modelled along +Y with the guard at y = 0: a long faceted emerald blade with a flared
 * flat tip and crystal shoulders, a braided root grip, root crossguard with curling tips, roots climbing and
 * webbing over the blade, and an emerald pommel. Animated: sap-light flowing up the blade core, swaying root
 * tendrils, and a charge state that makes everything glow and tremble.
 */
public final class BladeModel {
    private BladeModel() {}

    private static final float W = 0.125F, T = 0.048F;

    /** Blade rings: y, half width, half thickness. */
    private static final float[][] BLADE = {
            {-0.05F, 0.085F, 0.040F}, {0.03F, 0.150F, 0.050F}, {0.09F, 0.205F, 0.055F}, {0.15F, 0.150F, 0.050F},
            {0.23F, W, T}, {0.70F, W, T}, {1.36F, W, T}, {1.44F, 0.150F, 0.050F}, {1.50F, 0.205F, 0.050F},
            {1.535F, 0.218F, 0.045F}, {1.565F, 0.190F, 0.030F}};

    /**
     * @param time   animation time (ticks)
     * @param charge 0..1 while the wave ability is being charged
     */
    public static void render(PoseStack ps, MultiBufferSource buffers, float time, float charge, int light) {
        ps.pushPose();
        if (charge > 0) {
            float tr = 0.004F * charge;
            ps.translate(Math.sin(time * 2.7) * tr, 0, Math.cos(time * 3.1) * tr);
        }
        List<float[][]> roots = roots(time, charge);
        for (int pass = 0; pass < 3; pass++) {
            VertexConsumer vc = buffers.getBuffer(Mesh.TYPES[pass]);
            switch (pass) {
                case Mesh.CUT -> cutPass(vc, ps, roots, light);
                case Mesh.GLOW -> glowPass(vc, ps, roots, time, charge);
                default -> transPass(vc, ps, time, charge);
            }
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ passes

    private static void cutPass(VertexConsumer vc, PoseStack ps, List<float[][]> roots, int light) {
        TextureAtlasSprite bark = Mesh.sprite(Mesh.BARK);
        for (float[][] r : roots) Mesh.tube(vc, ps, r, 7, bark, 1, 1, 1, 1, light, 1);
    }

    private static void glowPass(VertexConsumer vc, PoseStack ps, List<float[][]> roots, float time, float charge) {
        TextureAtlasSprite core = Mesh.sprite(Mesh.CORE), vein = Mesh.sprite(Mesh.BARK_GLOW);
        // akan özsu ışığı: halkalar boyunca yukarı ilerleyen parlaklık dalgası
        float[][] rings = new float[BLADE.length + 8][];
        int k = 0;
        for (int i = 0; i < BLADE.length; i++) {
            float y = BLADE[i][0];
            float wave = 0.5F + 0.5F * (float) Math.sin(y * 7.0 - time * 0.16);
            float b = Math.min(1, 0.32F + 0.28F * wave + 0.5F * charge);
            rings[k++] = new float[]{y, BLADE[i][1] * 0.55F, BLADE[i][2] * 0.42F, 0, 0, b};
            if (i == 5) {
                for (int j = 1; j <= 8; j++) {
                    float yy = 0.70F + (1.36F - 0.70F) * j / 9F;
                    float w2 = 0.5F + 0.5F * (float) Math.sin(yy * 7.0 - time * 0.16);
                    rings[k++] = new float[]{yy, W * 0.55F, T * 0.42F, 0, 0, Math.min(1, 0.32F + 0.28F * w2 + 0.5F * charge)};
                }
            }
        }
        float[][] sorted = sortByY(rings, k);
        Mesh.loft(vc, ps, Mesh.HEX_BLADE, sorted, core, 1, 1, 1, 1, Mesh.FULL_BRIGHT, true);
        Mesh.loft(vc, ps, Mesh.reversed(Mesh.HEX_BLADE), sorted, core, 1, 1, 1, 1, Mesh.FULL_BRIGHT, true);
        // topuz özü
        float[][] pom = {{-0.66F, 0.0F, 0.0F, 0, 0, 0.9F}, {-0.625F, 0.04F, 0.035F, 0, 0, 0.9F}, {-0.585F, 0.03F, 0.025F, 0, 0, 0.9F}};
        Mesh.loft(vc, ps, Mesh.HEX, pom, core, 1, 1, 1, 1, Mesh.FULL_BRIGHT, true);
        Mesh.loft(vc, ps, Mesh.reversed(Mesh.HEX), pom, core, 1, 1, 1, 1, Mesh.FULL_BRIGHT, true);
        // köklerde parlayan özsu damarları
        float vi = 0.18F + 0.8F * charge + 0.08F * (float) Math.sin(time * 0.2);
        for (float[][] r : roots) {
            Mesh.tube(vc, ps, r, 7, vein, vi, vi, vi, 1, Mesh.FULL_BRIGHT, 1.06F, false);
            Mesh.tube(vc, ps, r, 7, vein, vi, vi, vi, 1, Mesh.FULL_BRIGHT, 1.06F, true);
        }
    }

    private static void transPass(VertexConsumer vc, PoseStack ps, float time, float charge) {
        TextureAtlasSprite crystal = Mesh.sprite(Mesh.CRYSTAL);
        float glint = 0.9F + 0.1F * (float) Math.sin(time * 0.1) + 0.2F * charge;
        float[][] rings = new float[BLADE.length][];
        for (int i = 0; i < BLADE.length; i++) rings[i] = new float[]{BLADE[i][0], BLADE[i][1], BLADE[i][2], 0, 0, 1};
        Mesh.loft(vc, ps, Mesh.HEX_BLADE, rings, crystal, 0.75F * glint, 1.0F * glint, 0.82F * glint, 0.80F, Mesh.FULL_BRIGHT, true);

        // omuzlardaki aşağı bakan kristal dişler
        for (int s = -1; s <= 1; s += 2) {
            ps.pushPose();
            ps.translate(s * 0.17F, 0.075F, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(s * 128));
            float[][] fin = {{0, 0.045F, 0.030F, 0, 0, 1}, {0.07F, 0.05F, 0.032F, 0, 0, 1}, {0.15F, 0.0F, 0.0F, 0, 0, 1}};
            Mesh.loft(vc, ps, Mesh.HEX, fin, crystal, 0.7F * glint, 1, 0.8F * glint, 0.85F, Mesh.FULL_BRIGHT, true);
            ps.popPose();
        }
        // zümrüt topuz
        float[][] pom = {{-0.69F, 0.0F, 0.0F, 0, 0, 1}, {-0.63F, 0.072F, 0.06F, 0, 0, 1}, {-0.60F, 0.065F, 0.052F, 0, 0, 1}, {-0.565F, 0.035F, 0.03F, 0, 0, 1}};
        Mesh.loft(vc, ps, Mesh.HEX, pom, crystal, 0.75F * glint, 1, 0.82F * glint, 0.85F, Mesh.FULL_BRIGHT, true);
    }

    private static float[][] sortByY(float[][] rings, int n) {
        float[][] r = new float[n][];
        System.arraycopy(rings, 0, r, 0, n);
        java.util.Arrays.sort(r, (a, b) -> Float.compare(a[0], b[0]));
        return r;
    }

    // ------------------------------------------------------------------ kökler

    private static List<float[][]> roots(float time, float charge) {
        List<float[][]> out = new ArrayList<>();
        float sway = 1 + 1.5F * charge;

        // örgülü kabza: üç iç içe sarmal kök
        for (int k = 0; k < 3; k++) {
            List<float[]> pts = new ArrayList<>();
            for (int i = 0; i <= 18; i++) {
                float y = -0.585F + 0.62F * i / 18F;
                double a = k * Math.PI * 2 / 3 + y * 10.5;
                float r = 0.030F + 0.008F * (float) Math.max(0, (y + 0.1) / 0.12);
                pts.add(new float[]{(float) Math.cos(a) * 0.026F, y, (float) Math.sin(a) * 0.026F, r});
            }
            out.add(pts.toArray(new float[0][]));
        }
        // topuz bileziği
        out.add(ring(-0.565F, 0.05F, 0.042F, 0.022F, 10, 0.4F));
        // siper: iki yana uzanan, uçları kıvrılan kökler
        for (int s = -1; s <= 1; s += 2) {
            float tip = (float) Math.sin(time * 0.05 + s) * 0.008F * sway;
            out.add(Mesh.spline(new float[][]{
                    {0, 0.0F, 0, 0.055F}, {s * 0.12F, -0.015F, 0.012F, 0.048F}, {s * 0.25F, -0.035F, -0.01F, 0.038F},
                    {s * 0.37F, -0.03F, 0.008F, 0.028F}, {s * 0.445F, 0.0F + tip, 0.02F, 0.018F}, {s * 0.47F, 0.045F + tip, 0.0F, 0.009F}}, 5));
            out.add(Mesh.spline(new float[][]{
                    {s * 0.2F, -0.03F, 0, 0.022F}, {s * 0.26F, -0.08F, 0.03F, 0.014F}, {s * 0.29F, -0.11F + tip, 0.02F, 0.006F}}, 4));
        }
        // siper düğümü: bıçak tabanını saran kök halkaları
        out.add(ring(0.01F, W + 0.035F, T + 0.03F, 0.045F, 14, 0.0F));
        out.add(ring(0.065F, W + 0.03F, T + 0.025F, 0.032F, 14, 1.3F));
        // bıçağa tırmanan ana kökler (yüzeyde sarmal)
        out.add(climb(0.02F, 0.80F, 0.4F, 2.0F, 0.058F, 0.012F, time, 0, sway));
        out.add(climb(0.04F, 0.60F, 3.3F, 1.4F, 0.044F, 0.009F, time, 1, sway));
        out.add(climb(0.10F, 0.98F, 1.9F, 1.1F, 0.028F, 0.006F, time, 2, sway));
        out.add(climb(0.06F, 0.45F, 5.0F, 0.85F, 0.038F, 0.009F, time, 3, sway));
        // ön ve arka yüzde ağ gibi bağlantılar
        float[] ys = {0.2F, 0.33F, 0.47F, 0.6F};
        for (int i = 0; i < ys.length; i++) {
            for (int f = -1; f <= 1; f += 2) {
                float y = ys[i] + (f > 0 ? 0 : 0.06F);
                float x0 = (i % 2 == 0 ? -1 : 1) * W * 0.95F;
                out.add(Mesh.spline(new float[][]{
                        {x0, y, f * (T + 0.016F), 0.022F}, {x0 * 0.3F, y + 0.03F, f * (T + 0.028F), 0.018F},
                        {-x0 * 0.4F, y + 0.07F, f * (T + 0.022F), 0.015F}, {-x0 * 0.95F, y + 0.1F, f * (T + 0.012F), 0.010F}}, 4));
            }
        }
        return out;
    }

    /** Closed loop of root around the blade at height y. */
    private static float[][] ring(float y, float rx, float rz, float r, int n, float phase) {
        float[][] pts = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 2 * i / n + phase;
            float wob = 0.008F * (float) Math.sin(a * 3 + phase);
            pts[i] = new float[]{(float) Math.cos(a) * rx, y + wob, (float) Math.sin(a) * rz, r * (0.85F + 0.15F * (float) Math.sin(a * 2))};
        }
        return pts;
    }

    /** Root spiralling up the blade surface from y0 to y1, with a swaying tendril tip. */
    private static float[][] climb(float y0, float y1, float a0, float turns, float r0, float r1, float time, int idx, float sway) {
        int n = 26;
        float[][] pts = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float s = i / (float) n;
            float y = y0 + (y1 - y0) * s;
            double a = a0 + turns * Math.PI * 2 * s + Math.sin(s * 9 + idx) * 0.15;
            float r = (r0 + (r1 - r0) * s) * (1 + 0.22F * (float) Math.sin(s * 37 + idx * 2.1));
            float ex = W + r * 0.7F + 0.006F, ez = T + r * 0.7F + 0.006F;
            float tipW = s > 0.75F ? (s - 0.75F) * 4 : 0;
            float sx = (float) Math.sin(time * 0.09 + idx * 1.7) * 0.03F * tipW * tipW * sway;
            float sz = (float) Math.cos(time * 0.07 + idx) * 0.02F * tipW * tipW * sway;
            pts[i] = new float[]{(float) Math.cos(a) * ex + sx, y, (float) Math.sin(a) * ez + sz, r};
        }
        return pts;
    }
}
