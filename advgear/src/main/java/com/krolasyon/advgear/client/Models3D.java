package com.krolasyon.advgear.client;

import com.krolasyon.advgear.client.G.Mat;
import com.krolasyon.advgear.client.G.Pass;
import com.mojang.math.Axis;

import static com.krolasyon.advgear.client.G.mat;

/**
 * Tüm eşyaların 3D modelleri. Birimler piksel, Y yukarı, ön yüz +Z (eşya uzayı). Giyilen modeller (gözlük, çanta, kemer)
 * oyuncu modelinin baş/gövde uzayında (Y aşağı, ön -Z) tanımlanır.
 */
public final class Models3D {
    private Models3D() {}

    // ------------------------------------------------------------------ malzemeler
    static final Pass C = Pass.CUT, T = Pass.TRANS, L = Pass.GLOW;
    static final Mat GOLD = mat("metal", 0xFFCC4E, C), GOLD_D = mat("metal", 0xC08A2A, C), ENGRAVE = mat("engrave", 0xFFCC4E, C);
    static final Mat SILVER = mat("metal", 0xE6EDF2, C), SILVER_D = mat("metal", 0x8C969E, C), IRON = mat("metal", 0xA7AFB5, C);
    static final Mat STEEL_D = mat("metal", 0x5C646B, C), BRASS = mat("metal", 0xD39A45, C), BRASS_D = mat("metal", 0x9A6A2A, C);
    static final Mat DARK = mat("plain", 0x1C1818, C), BLACK_CAP = mat("plain", 0x2C2C32, C), BONE = mat("bone", 0xEDE5CF, C);
    static final Mat CORK = mat("cork", 0xB98A58, C), PAPER = mat("paper", 0xF4EEDF, C), TAG = mat("tag", 0xFFFFFF, C);
    static final Mat STRING = mat("plain", 0xD9CBA8, C), RED = mat("plain", 0xC8283C, C), ORANGE_PAINT = mat("plain", 0xD8742C, C);
    static final Mat GLASS = mat("glass", 0xE8F6FF, 0.28F, T), HILITE = mat("plain", 0xFFFFFF, 0.55F, T);

    // ------------------------------------------------------------------ yardımcılar

    /** Sıvı + kabarcıklar + parıltılar. */
    private static void liquid(G g, float[][] prof, float ax, float az, float p, float fill, int color, int glow, float glowA, Anim a, int seg,
                               int bubbles, int bubbleColor) {
        Mat liq = mat("liquid", color, 0.82F, T);
        g.liquid(liq, prof, ax, az, p, fill, a.tx, a.tz, a.wave, a.time * 0.15F, seg);
        if (glow != 0) g.liquid(mat("liquid", glow, glowA, L), prof, ax * 0.98F, az * 0.98F, p, fill, a.tx, a.tz, a.wave, a.time * 0.15F, seg);
        float y0 = prof[0][1], y1 = prof[prof.length - 1][1];
        for (int i = 0; i < bubbles; i++) {
            float ph = (a.time * (0.02F + 0.007F * (i % 3)) + i * 0.37F) % 1F;
            float ang = i * 2.4F + a.time * 0.01F;
            float rr = G.rAt(prof, y0 + 1) * 0.55F * ((i * 7 % 5) / 5F + 0.2F);
            float x = (float) Math.cos(ang) * rr * ax, z = (float) Math.sin(ang) * rr * az;
            float y = y0 + 0.6F + ph * (y1 - y0);
            float top = G.surface(prof, fill, a.tx * G.rAt(prof, y), a.tz * G.rAt(prof, y), 0, 0) - 0.3F;
            if (y > top) continue;
            float s = 0.18F + 0.12F * (i % 3);
            g.sphere(mat("plain", bubbleColor, 0.65F, T), x, y, z, s, s, s, 6, 4);
        }
    }

    /** Camın üstünde dikey parlama şeridi. */
    private static void glint(G g, float x, float z, float y0, float y1, float w) {
        g.quad(HILITE, new float[]{x, y0, z, 0, 1}, new float[]{x + w, y0, z, 1, 1}, new float[]{x + w * 0.8F, y1, z, 1, 0}, new float[]{x, y1, z, 0, 0});
    }

    private static float[][] circleXY(float cx, float cy, float z, float r, int n) {
        float[][] o = new float[n][];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            o[i] = new float[]{cx + r * (float) Math.cos(a), cy + r * (float) Math.sin(a), z};
        }
        return o;
    }

    private static float[][] reverse(float[][] p) {
        float[][] r = new float[p.length][];
        for (int i = 0; i < p.length; i++) r[i] = p[p.length - 1 - i];
        return r;
    }

    /** Animasyon girdileri. */
    public record Anim(float time, float tx, float tz, float wave, float open, float swing) {}

    // ================================================================== İKSİRLER

    public static void heartTonic(G g, Anim a) {
        float[][] out = {{0, -6.6F}, {2.9F, -6.6F}, {3.3F, -6.3F}, {3.4F, -5.6F}, {3.4F, 2.2F}, {3.15F, 2.9F}, {1.85F, 3.4F}, {1.7F, 3.7F}, {1.7F, 4.6F}};
        float[][] in = {{0, -6.1F}, {2.95F, -6.1F}, {3.05F, -5.6F}, {3.05F, 2.1F}, {2.8F, 2.7F}, {1.5F, 3.15F}, {1.4F, 4.4F}};
        g.draw(() -> {
            liquid(g, in, 1, 0.72F, 5, 0.66F, 0x6B52E6, 0x7A4CFF, 0.35F, a, 28, 7, 0xDCD4FF);
            g.lathe(GLASS, out, 28, true, 1, 0.72F, 5);
            glint(g, -2.6F, 2.5F, -5F, 1.8F, 0.7F);
            // taban bandı ve boyun halkası
            g.lathe(mat("plain", 0x2F4A6E, C), new float[][]{{3.45F, -6.75F}, {3.6F, -6.6F}, {3.6F, -5.4F}, {3.45F, -5.25F}}, 28, true, 1, 0.72F, 5);
            g.lathe(mat("metal", 0x7FC1E3, C), new float[][]{{1.75F, 3.7F}, {2.0F, 3.8F}, {2.0F, 4.4F}, {1.75F, 4.5F}}, 20);
            // kapak: tırtıklı siyah tıpa
            g.lathe(BLACK_CAP, new float[][]{{0, 4.5F}, {1.95F, 4.5F}, {2.1F, 4.7F}, {2.1F, 6.5F}, {1.9F, 6.9F}, {0, 7.0F}}, 16, false, 1, 1, 2);
            // ip ve kalp etiketi (sallanır)
            g.ps.pushPose();
            g.ps.translate(1.9F, 4.0F, 0);
            g.ps.mulPose(Axis.ZP.rotation(-0.35F - a.tx * 0.9F));
            g.ps.mulPose(Axis.XP.rotation(a.tz * 0.6F));
            g.rod(STRING, new float[]{0, 0, 0}, new float[]{1.6F, -2.6F, 0.9F}, 0.12F);
            g.ps.translate(1.6F, -2.6F, 0.9F);
            g.ps.mulPose(Axis.YP.rotationDegrees(-25));
            g.quad(TAG, new float[]{-1.3F, -4.6F, 0.06F, 0, 1}, new float[]{1.6F, -4.6F, 0.06F, 1, 1}, new float[]{1.6F, 0.2F, 0.06F, 1, 0},
                    new float[]{-1.3F, 0.2F, 0.06F, 0, 0});
            g.box(mat("paper", 0xE6DCC4, C), -1.3F, -4.6F, -0.06F, 1.6F, 0.2F, 0.05F);
            g.ps.popPose();
        });
    }

    public static void oceanDraught(G g, Anim a) {
        float[][] out = {{0, -6.8F}, {2.4F, -6.8F}, {3.8F, -6.0F}, {4.6F, -4.2F}, {4.6F, -2.4F}, {4.0F, -0.4F}, {2.9F, 1.8F}, {1.8F, 3.7F}, {1.3F, 4.8F},
                {1.45F, 5.3F}, {1.35F, 5.8F}};
        float[][] in = {{0, -6.35F}, {2.3F, -6.35F}, {3.5F, -5.7F}, {4.2F, -4.1F}, {4.2F, -2.5F}, {3.65F, -0.5F}, {2.6F, 1.6F}, {1.5F, 3.5F}, {1.0F, 4.6F}};
        g.draw(() -> {
            liquid(g, in, 1, 1, 2, 0.58F, 0x1FB3C9, 0x3FE8FF, 0.25F, a, 28, 12, 0xE6FDFF);
            g.lathe(GLASS.alpha(0.3F), out, 28, true, 1, 1, 2);
            glint(g, -3.3F, 2.6F, -4.5F, 0.2F, 0.8F);
            g.lathe(CORK, new float[][]{{0, 5.2F}, {1.15F, 5.2F}, {1.35F, 6.2F}, {1.45F, 7.3F}, {1.2F, 7.6F}, {0, 7.65F}}, 14);
            g.lathe(mat("plain", 0x6F4A2A, C), new float[][]{{1.4F, 5.55F}, {1.55F, 5.6F}, {1.55F, 5.9F}, {1.4F, 5.95F}}, 14);
        });
    }

    public static void blackDeath(G g, Anim a) {
        float[][] out = {{0, -7}, {4.0F, -7}, {4.6F, -6.4F}, {4.4F, -5.6F}, {1.7F, 3.0F}, {1.25F, 3.5F}, {1.25F, 4.3F}};
        float[][] in = {{0, -6.5F}, {3.9F, -6.5F}, {3.95F, -5.7F}, {1.45F, 2.6F}, {1.0F, 3.2F}};
        g.draw(() -> {
            liquid(g, in, 1, 1, 2, 0.55F, 0xE0257E, 0xFF3FA0, 0.45F, a, 8, 5, 0xFFC0E0);
            g.lathe(mat("glass", 0xFF7AB8, 0.38F, T), out, 8, false, 1, 1, 2);
            glint(g, -2.4F, 1.6F, -5.5F, 0.6F, 0.6F);
            // gümüş boyun ve kurukafa tıpa
            g.lathe(SILVER, new float[][]{{1.3F, 4.2F}, {1.7F, 4.35F}, {1.7F, 5.0F}, {1.2F, 5.2F}}, 12);
            g.sphere(SILVER, 0, 6.5F, 0, 1.9F, 1.8F, 1.85F, 14, 10);
            g.lathe(SILVER, new float[][]{{1.35F, 7.6F}, {0.8F, 8.8F}, {0, 9.9F}}, 10);
            g.box(SILVER_D, -1.2F, 4.95F, -0.4F, 1.2F, 5.6F, 1.55F);
            for (int i = -2; i <= 2; i++) g.box(BONE, i * 0.45F - 0.18F, 4.95F, 1.5F, i * 0.45F + 0.18F, 5.5F, 1.75F);
            g.sphere(DARK, -0.72F, 6.45F, 1.45F, 0.55F, 0.6F, 0.4F, 8, 6);
            g.sphere(DARK, 0.72F, 6.45F, 1.45F, 0.55F, 0.6F, 0.4F, 8, 6);
            g.sphere(DARK, 0, 5.75F, 1.7F, 0.22F, 0.3F, 0.2F, 6, 4);
            g.sphere(mat("plain", 0xFF3070, 0.9F, L), -0.72F, 6.45F, 1.5F, 0.22F, 0.22F, 0.2F, 6, 4);
            g.sphere(mat("plain", 0xFF3070, 0.9F, L), 0.72F, 6.45F, 1.5F, 0.22F, 0.22F, 0.2F, 6, 4);
        });
    }

    public static void starlightElixir(G g, Anim a) {
        float R = 5.0F, cy = -1.2F;
        float[][] in = new float[13][];
        for (int i = 0; i <= 12; i++) {
            double t = -Math.PI / 2 + Math.PI * i / 12;
            in[i] = new float[]{(float) Math.cos(t) * (R - 0.4F), cy + (float) Math.sin(t) * (R - 0.4F)};
        }
        in[0][0] = 0;
        in[12] = new float[]{1.0F, cy + R - 0.45F};
        g.draw(() -> {
            liquid(g, in, 1, 1, 2, 0.6F, 0x2B3BD8, 0x4A6AFF, 0.4F, a, 28, 4, 0xB8C8FF);
            // yıldız tozu parıltıları
            for (int i = 0; i < 9; i++) {
                float ph = a.time * 0.03F + i * 0.7F;
                float x = (float) Math.cos(ph + i) * 2.4F, z = (float) Math.sin(ph * 1.3F + i) * 2.4F, y = cy - 2.6F + (i % 4) * 0.7F + (float) Math.sin(ph * 2) * 0.4F;
                float s = 0.12F + 0.1F * (float) Math.abs(Math.sin(ph * 3));
                g.sphere(mat("plain", 0xF6F8FF, L), x, y, z, s, s, s, 4, 3);
            }
            g.sphere(GLASS.alpha(0.24F), 0, cy, 0, R, R, R, 28, 18);
            g.lathe(GLASS, new float[][]{{1.2F, cy + R - 0.3F}, {1.25F, 4.6F}, {1.2F, 5.4F}}, 14);
            // gümüş kafes: ekvator ve dört meridyen
            g.sweep(SILVER_D, G.loopXZ(R + 0.12F, R + 0.12F, cy, 40), G.RECT, 0.12F, 0.35F, new float[]{0, 1, 0}, true);
            for (int k = 0; k < 4; k++) {
                g.ps.pushPose();
                g.ps.mulPose(Axis.YP.rotationDegrees(45 * k));
                g.sweep(SILVER_D, G.arc(0, cy, 0, R + 0.12F, R + 0.12F, -90, 90, 20), G.RECT, 0.35F, 0.12F, new float[]{0, 0, 1}, false);
                g.ps.popPose();
            }
            g.lathe(SILVER, new float[][]{{0, cy - R - 0.4F}, {2.2F, cy - R - 0.3F}, {2.4F, cy - R + 0.2F}, {1.6F, cy - R + 0.5F}}, 20);
            // boyun ve anahtar biçimli tepe
            g.lathe(SILVER, new float[][]{{1.35F, 4.2F}, {1.55F, 4.4F}, {1.55F, 5.6F}, {0.6F, 6.0F}, {0.5F, 6.8F}}, 14);
            g.sweep(SILVER, circleXY(-0.85F, 7.6F, 0, 0.75F, 16), G.ROUND8, 0.2F, 0.2F, new float[]{0, 0, 1}, true);
            g.sweep(SILVER, circleXY(0.85F, 7.6F, 0, 0.75F, 16), G.ROUND8, 0.2F, 0.2F, new float[]{0, 0, 1}, true);
            glint(g, -3.6F, 3.3F, -3.2F, 1.4F, 0.9F);
        });
    }

    public static void sunEssence(G g, Anim a) {
        float[][] out = {{0, -6.4F}, {1.4F, -6.3F}, {1.9F, -5.2F}, {2.0F, -3F}, {2.0F, 3.6F}, {1.8F, 4.7F}, {1.4F, 5.2F}};
        float[][] in = {{0, -5.9F}, {1.3F, -5.8F}, {1.65F, -5.0F}, {1.7F, -3F}, {1.7F, 3.5F}, {1.45F, 4.5F}, {1.1F, 4.9F}};
        g.draw(() -> {
            liquid(g, in, 1, 1, 2, 0.82F, 0xFFB21E, 0xFFD24A, 0.55F, a, 22, 6, 0xFFF4C0);
            g.lathe(GLASS.alpha(0.25F), out, 22);
            glint(g, -1.4F, 1.6F, -4.5F, 3.6F, 0.45F);
            g.lathe(BRASS, new float[][]{{0, -7.9F}, {0.4F, -7.6F}, {1.4F, -6.8F}, {2.05F, -6.0F}, {2.1F, -5.3F}, {1.9F, -5.1F}}, 18);
            g.lathe(BRASS, new float[][]{{2.0F, -0.4F}, {2.2F, -0.3F}, {2.2F, 0.6F}, {2.0F, 0.7F}}, 18);
            g.lathe(BRASS, new float[][]{{1.85F, 4.6F}, {2.0F, 4.9F}, {1.9F, 6.2F}, {1.4F, 6.8F}, {0, 7.0F}}, 18);
            g.sweep(mat("metal", 0x3A3A3E, C), circleXY(0, 8.2F, 0, 1.15F, 18), G.ROUND8, 0.3F, 0.3F, new float[]{0, 0, 1}, true);
        });
    }

    // ================================================================== YÜZÜKLER (bant XY düzleminde, ön +Z)

    private static void band(G g, Mat m, float R, float w, float h) {
        g.ps.pushPose();
        g.ps.mulPose(Axis.XP.rotationDegrees(90));
        g.lathe(m, G.bandProfile(R, w, h, Math.min(w, h) * 0.45F, 3), 32);
        g.ps.popPose();
    }

    public static void goldBand(G g, Anim a) {
        g.draw(() -> band(g, ENGRAVE, 4.2F, 1.3F, 2.6F));
    }

    public static void sapphireRing(G g, Anim a) {
        g.draw(() -> {
            band(g, GOLD, 4.0F, 1.1F, 1.8F);
            // yuva
            g.lathe(SILVER, new float[][]{{0.8F, 4.4F}, {2.0F, 5.0F}, {2.2F, 5.6F}, {1.9F, 5.8F}}, 16);
            for (int k = 0; k < 4; k++) {
                double an = Math.PI / 4 + Math.PI / 2 * k;
                float x = (float) Math.cos(an) * 2.05F, z = (float) Math.sin(an) * 2.05F;
                g.rod(SILVER, new float[]{x, 5.4F, z}, new float[]{x * 0.82F, 7.2F, z * 0.82F}, 0.28F);
            }
            g.ps.pushPose();
            g.ps.translate(0, 5.7F, 0);
            g.ps.mulPose(Axis.YP.rotationDegrees(45));
            g.gem(mat("gem", 0x2F54E6, 0.85F, T), 2.3F, 3.4F, 8);
            g.gem(mat("gem", 0x5A80FF, 0.35F, L), 2.25F, 3.35F, 8);
            g.ps.popPose();
        });
    }

    public static void skullRing(G g, Anim a) {
        g.draw(() -> {
            band(g, SILVER, 3.9F, 1.1F, 1.7F);
            float y = 6.4F;
            g.sphere(SILVER, 0, y, 0, 2.3F, 2.3F, 2.1F, 18, 12);
            g.lathe(SILVER, new float[][]{{1.7F, y + 1.4F}, {0.9F, y + 3.0F}, {0, y + 4.0F}}, 12);
            g.rbox(SILVER, -1.5F, y - 2.6F, -0.6F, 1.5F, y - 1.2F, 1.85F, 0.5F, 3);
            for (int i = -2; i <= 2; i++) g.box(mat("bone", 0xF4F0E4, C), i * 0.55F - 0.2F, y - 2.3F, 1.75F, i * 0.55F + 0.2F, y - 1.4F, 1.95F);
            g.sphere(DARK, -0.85F, y + 0.1F, 1.7F, 0.65F, 0.7F, 0.45F, 10, 6);
            g.sphere(DARK, 0.85F, y + 0.1F, 1.7F, 0.65F, 0.7F, 0.45F, 10, 6);
            g.sphere(DARK, 0, y - 0.9F, 2.0F, 0.25F, 0.35F, 0.2F, 6, 4);
            g.sphere(SILVER_D, 0, y - 0.4F, -0.4F, 2.4F, 1.0F, 1.9F, 12, 6);
        });
    }

    public static void boneHandRing(G g, Anim a) {
        g.draw(() -> {
            band(g, SILVER_D, 3.8F, 0.9F, 1.4F);
            // dört parmak kemiği bandı sarar
            for (int f = 0; f < 4; f++) {
                float z = -1.6F + f * 1.07F;
                float prevA = 70 + f * 4;
                for (int s = 0; s < 3; s++) {
                    float a0 = prevA, a1 = a0 - 38 - s * 6;
                    float r = 4.8F;
                    float[] p0 = {(float) Math.cos(Math.toRadians(a0)) * r, (float) Math.sin(Math.toRadians(a0)) * r, z};
                    float[] p1 = {(float) Math.cos(Math.toRadians(a1)) * r, (float) Math.sin(Math.toRadians(a1)) * r, z};
                    g.rod(SILVER, p0, p1, 0.42F - s * 0.05F);
                    g.sphere(SILVER, p1[0], p1[1], p1[2], 0.5F, 0.5F, 0.5F, 8, 6);
                    prevA = a1;
                }
                g.sphere(SILVER, (float) Math.cos(Math.toRadians(70 + f * 4)) * 4.8F, (float) Math.sin(Math.toRadians(70 + f * 4)) * 4.8F, z, 0.62F, 0.62F, 0.62F, 8, 6);
            }
            // elin sırtı
            g.rbox(SILVER, -2.6F, 4.6F, -2.2F, 2.2F, 6.2F, 2.2F, 0.7F, 3);
            for (int f = 0; f < 4; f++) g.rod(SILVER_D, new float[]{-2.0F + f * 1.25F, 6.25F, -1.6F + f * 1.07F}, new float[]{1.3F, 5.5F, -1.6F + f * 1.07F}, 0.18F);
        });
    }

    public static void emeraldEyeRing(G g, Anim a) {
        g.draw(() -> {
            band(g, SILVER, 4.0F, 1.0F, 1.6F);
            // iki yanda kıvrık telkari
            for (int side = -1; side <= 1; side += 2) {
                float[][] curl = new float[18][];
                for (int i = 0; i < 18; i++) {
                    double t = i / 17.0 * Math.PI * 2.2;
                    float rr = 1.4F * (1 - i / 22F);
                    curl[i] = new float[]{side * (2.6F + (float) Math.cos(t) * rr), 4.6F + (float) Math.sin(t) * rr, 0.9F};
                }
                g.sweep(SILVER_D, curl, G.ROUND8, 0.22F, 0.22F, new float[]{0, 0, 1}, false);
            }
            g.ps.pushPose();
            g.ps.translate(0, 4.5F, 0);
            g.lathe(GOLD, new float[][]{{1.6F, 0}, {2.2F, 0.3F}, {2.3F, 1.0F}, {2.0F, 1.3F}, {1.7F, 1.2F}}, 22, true, 1.25F, 1, 2);
            g.ps.translate(0, 1.0F, 0);
            g.cabochon(mat("gem", 0x18B868, 0.88F, T), 2.0F, 1.6F, 1.4F);
            g.cabochon(mat("gem", 0x3AFF9A, 0.4F, L), 1.95F, 1.55F, 1.38F);
            g.sphere(mat("plain", 0x063A1E, 0.9F, T), 0, 0.9F, 0, 0.4F, 0.3F, 0.9F, 8, 6);
            g.ps.popPose();
        });
    }

    // ================================================================== GÖZLÜKLER (baş uzayı: Y aşağı, yüz z=-4)

    public static void goggles(G g, int variant, boolean worn) {
        float L0 = variant == 2 ? 3.0F : 1.7F;
        int lensCol = variant == 0 ? 0x9ED8F5 : variant == 1 ? 0x2FC9B8 : 0xE0262A;
        Mat lens = mat("glass", lensCol, variant == 0 ? 0.42F : 0.6F, T);
        Mat leather = mat("leather", 0x6E4A30, C);
        g.draw(() -> {
            // kayış
            g.sweep(leather, G.roundRectXZ(4.55F, 4.55F, 1.6F, -4.6F, 6), G.RECT, 0.16F, 0.85F, new float[]{0, 1, 0}, true);
            g.box(BRASS_D, 4.5F, -5.4F, 0.6F, 4.95F, -3.8F, 2.0F);
            g.box(BRASS_D, -4.95F, -5.4F, 0.6F, -4.5F, -3.8F, 2.0F);
            for (int side = -1; side <= 1; side += 2) {
                float cx = side * 2.15F;
                g.ps.pushPose();
                g.ps.translate(cx, -4.6F, -4.45F);
                g.ps.mulPose(Axis.XP.rotationDegrees(-90));
                if (variant == 2) {
                    g.lathe(BRASS, new float[][]{{1.6F, 0}, {1.95F, 0.2F}, {2.05F, 1.0F}, {2.3F, 1.2F}, {2.35F, 2.0F}, {2.55F, 2.2F}, {2.6F, L0}, {2.1F, L0 + 0.15F}}, 22);
                    g.lathe(BRASS_D, new float[][]{{2.32F, 1.35F}, {2.42F, 1.45F}, {2.42F, 1.85F}, {2.32F, 1.95F}}, 22);
                } else {
                    g.lathe(BRASS, new float[][]{{1.6F, 0}, {1.95F, 0.15F}, {2.0F, L0 - 0.35F}, {2.3F, L0 - 0.25F}, {2.35F, L0}, {1.8F, L0 + 0.12F}}, 22);
                }
                float lr = variant == 2 ? 2.15F : 1.85F;
                g.lathe(lens, new float[][]{{lr, L0}, {lr * 0.7F, L0 + 0.28F}, {0, L0 + 0.38F}}, 20);
                if (variant != 0) g.lathe(mat("glass", lensCol, 0.35F, L), new float[][]{{lr, L0}, {0, L0 + 0.36F}}, 20);
                // perçinler
                for (int k = 0; k < 6; k++) {
                    double an = Math.PI * 2 * k / 6;
                    float rr = variant == 2 ? 2.6F : 2.33F;
                    g.sphere(BRASS_D, (float) Math.cos(an) * rr, L0 - 0.1F, (float) Math.sin(an) * rr, 0.16F, 0.16F, 0.16F, 5, 3);
                }
                g.ps.popPose();
            }
            // burun köprüsü
            g.sweep(BRASS, G.arc(0, -4.2F, -5.1F, 0.6F, 0.5F, 180, 360, 8), G.ROUND8, 0.22F, 0.22F, new float[]{0, 0, 1}, false);
            if (variant == 1) {
                // büyüteç kolu ve ek mercek (sağ gözün önünde)
                g.sphere(BRASS_D, -4.6F, -6.4F, -4.6F, 0.4F, 0.4F, 0.4F, 8, 6);
                g.rod(BRASS_D, new float[]{-4.6F, -6.4F, -4.6F}, new float[]{-3.4F, -6.0F, -7.4F}, 0.16F);
                g.ps.pushPose();
                g.ps.translate(-2.4F, -5.2F, -6.6F);
                g.ps.mulPose(Axis.XP.rotationDegrees(-90));
                g.lathe(BRASS, new float[][]{{0.9F, 0}, {1.1F, 0.1F}, {1.15F, 1.3F}, {1.3F, 1.4F}, {1.0F, 1.5F}}, 16);
                g.lathe(lens, new float[][]{{1.0F, 1.45F}, {0, 1.65F}}, 14);
                g.lathe(mat("glass", lensCol, 0.4F, L), new float[][]{{1.0F, 1.45F}, {0, 1.64F}}, 14);
                g.ps.popPose();
            }
        });
    }

    // ================================================================== SIRT ÇANTALARI (gövde uzayı: Y aşağı, sırt z=+2)

    private static final float[] UP_Z = {0, 0, 1};

    private static void buckle(G g, Mat m, float x, float y, float z, float w, float h) {
        float[][] r = {{x - w, y - h, z}, {x + w, y - h, z}, {x + w, y + h, z}, {x - w, y + h, z}};
        g.sweep(m, r, G.ROUND8, 0.16F, 0.16F, UP_Z, true);
        g.rod(m, new float[]{x, y - h, z + 0.05F}, new float[]{x, y + h * 0.4F, z + 0.15F}, 0.1F);
    }

    private static void mapWindow(G g, float x0, float y0, float x1, float y1, float z) {
        g.quad(mat("map", 0xFFFFFF, C), new float[]{x0, y0, z, 0, 0}, new float[]{x1, y0, z, 1, 0}, new float[]{x1, y1, z, 1, 1}, new float[]{x0, y1, z, 0, 1});
        g.sweep(BRASS, new float[][]{{x0, y0, z}, {x1, y0, z}, {x1, y1, z}, {x0, y1, z}}, G.RECT, 0.28F, 0.12F, UP_Z, true);
        for (float[] c : new float[][]{{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}}) g.sphere(BRASS_D, c[0], c[1], z + 0.15F, 0.2F, 0.2F, 0.2F, 6, 4);
    }

    /** Omuz kayışı: sırttan omuz üstünden öne iner, koltuk altından arkaya döner. */
    private static void shoulderStrap(G g, Mat m, float x) {
        float s = Math.signum(x);
        float[][] path = {{x, 2.5F, 2.4F}, {x, -0.2F, 1.6F}, {x, -0.45F, 0}, {x, -0.2F, -2.2F}, {x, 2.0F, -2.42F}, {x * 1.05F, 8.0F, -2.42F},
                {s * 4.1F, 9.5F, -2.1F}, {s * 4.25F, 10.0F, 0}, {s * 3.6F, 9.6F, 2.3F}};
        g.sweep(m, path, G.RECT, 0.55F, 0.1F, new float[]{1, 0, 0}, false);
    }

    public static void backpack(G g, int variant, float sway) {
        backpack(g, variant, sway, true);
    }

    public static void backpack(G g, int variant, float sway, boolean worn) {
        g.draw(() -> {
            if (variant == 0) {
                Mat canvas = mat("canvas", 0x9CA468, C), canvasL = mat("canvas", 0xB6BE80, C), strap = mat("leather", 0x7A6A3A, C);
                g.rbox(canvas, -3.7F, 3.0F, 2.05F, 3.7F, 10.2F, 5.3F, 1.0F, 4);
                g.rbox(canvas.tint(0.9F), 3.4F, 5.0F, 2.6F, 4.6F, 9.8F, 4.8F, 0.5F, 2);
                g.ps.pushPose();
                g.ps.translate(0, 2.8F, 5.2F);
                g.ps.mulPose(Axis.XP.rotation(sway));
                g.rbox(canvasL, -3.85F, -0.3F, -0.2F, 3.85F, 5.0F, 0.45F, 0.35F, 3);
                g.ps.popPose();
                g.rbox(strap, -0.55F, 3.0F, 5.4F, 0.55F, 9.6F, 5.75F, 0.15F, 1);
                buckle(g, SILVER, 0, 8.2F, 5.85F, 0.9F, 0.75F);
                // çapraz omuz askısı
                float[][] diag = {{-3.2F, 3.4F, 2.1F}, {-3.4F, -0.3F, 1.2F}, {-3.0F, -0.45F, -1.0F}, {-2.4F, 0.6F, -2.4F}, {3.8F, 9.2F, -2.42F},
                        {4.35F, 10.0F, -0.6F}, {4.1F, 9.6F, 1.8F}, {3.4F, 8.6F, 2.3F}};
                if (worn) g.sweep(strap, diag, G.RECT, 0.6F, 0.1F, new float[]{1, 0, 0}, false);
                else g.sweep(strap, G.arc(0, 3.0F, 3.7F, 2.2F, 1.8F, 180, 360, 12), G.RECT, 0.6F, 0.12F, UP_Z, false);
            } else if (variant == 1) {
                Mat canvas = mat("canvas", 0xB4A88E, C), canvasL = mat("canvas", 0xCFC3A6, C), strap = mat("leather", 0x6E4E30, C);
                g.rbox(canvas, -4.0F, 0.6F, 2.05F, 4.0F, 10.9F, 6.3F, 1.2F, 4);
                g.rbox(canvasL, -4.2F, 0.3F, 1.95F, 4.2F, 3.4F, 6.6F, 0.9F, 3);
                g.ps.pushPose();
                g.ps.translate(0, 2.6F, 6.35F);
                g.ps.mulPose(Axis.XP.rotation(sway));
                g.rbox(canvasL, -3.7F, 0, -0.1F, 3.7F, 4.4F, 0.45F, 0.3F, 3);
                mapWindow(g, -2.3F, 0.6F, 2.3F, 3.4F, 0.48F);
                g.ps.popPose();
                g.rbox(canvas.tint(0.92F), -2.9F, 7.0F, 6.1F, 2.9F, 10.2F, 7.2F, 0.5F, 3);
                g.box(BRASS, -0.6F, 6.6F, 6.9F, 0.6F, 8.0F, 7.35F);
                g.sphere(BRASS_D, 0, 7.6F, 7.4F, 0.25F, 0.25F, 0.25F, 6, 4);
                for (int sx = -1; sx <= 1; sx += 2) {
                    g.rbox(canvas.tint(0.9F), sx * 4.0F - 0.6F, 5.5F, 2.8F, sx * 4.0F + 0.6F, 10.2F, 5.4F, 0.45F, 2);
                    g.box(BRASS, sx * 3.7F - 0.5F, 10.4F, 5.4F, sx * 3.7F + 0.5F, 11.0F, 6.4F);
                    if (worn) shoulderStrap(g, strap, sx * 2.5F);
                }
            } else {
                Mat leather = mat("leather", 0xC67C38, C), leatherL = mat("leather", 0xDB944C, C), strap = mat("leather", 0x6A4024, C);
                g.rbox(leather, -4.6F, 0.0F, 2.05F, 4.6F, 11.3F, 7.3F, 1.2F, 4);
                // üstte rulo battaniye
                g.ps.pushPose();
                g.ps.translate(0, -1.5F, 4.7F);
                g.ps.mulPose(Axis.ZP.rotationDegrees(90));
                g.lathe(mat("cloth", 0x8E3E2C, C), new float[][]{{0, -5.3F}, {1.5F, -5.3F}, {1.75F, -4.9F}, {1.75F, 4.9F}, {1.5F, 5.3F}, {0, 5.3F}}, 18);
                g.ps.popPose();
                for (int sx = -1; sx <= 1; sx += 2) {
                    g.ps.pushPose();
                    g.ps.translate(sx * 3.0F, -1.5F, 4.7F);
                    g.ps.mulPose(Axis.YP.rotationDegrees(90));
                    g.sweep(strap, circleXY(0, 0, 0, 1.85F, 18), G.RECT, 0.45F, 0.12F, UP_Z, true);
                    g.ps.popPose();
                }
                g.ps.pushPose();
                g.ps.translate(0, -0.2F, 7.25F);
                g.ps.mulPose(Axis.XP.rotation(sway));
                g.rbox(leatherL, -4.7F, 0, -0.2F, 4.7F, 5.4F, 0.4F, 0.3F, 3);
                mapWindow(g, -2.5F, 0.8F, 2.5F, 3.8F, 0.43F);
                g.ps.popPose();
                // ön cepler ve asma kilit
                for (int sx = -1; sx <= 1; sx += 2) {
                    g.rbox(leatherL, sx * 2.4F - 1.9F, 6.2F, 7.0F, sx * 2.4F + 1.9F, 10.6F, 8.2F, 0.5F, 3);
                    buckle(g, BRASS, sx * 2.4F, 7.0F, 8.25F, 0.6F, 0.5F);
                    g.rbox(leather.tint(0.92F), sx * 4.6F - 0.7F, 4.0F, 3.0F, sx * 4.6F + 0.7F, 10.0F, 6.2F, 0.5F, 2);
                    if (worn) shoulderStrap(g, strap, sx * 2.6F);
                }
                g.box(BRASS, -0.9F, 5.6F, 7.45F, 0.9F, 7.2F, 8.1F);
                g.sweep(SILVER, G.arc(0, 5.6F, 7.75F, 0.6F, 0.8F, 180, 360, 10), G.ROUND8, 0.13F, 0.13F, UP_Z, false);
                g.sphere(DARK, 0, 6.5F, 8.12F, 0.18F, 0.25F, 0.05F, 6, 4);
            }
        });
    }

    // ================================================================== KEMERLER (gövde uzayı)

    public static void belt(G g, int variant, Anim a) {
        int col = variant == 0 ? 0x8C7458 : variant == 1 ? 0x7E8248 : 0x6C4428;
        Mat band = mat("stitch", col, C);
        Mat metal = variant == 0 ? SILVER : BRASS;
        g.draw(() -> {
            g.sweep(band, G.roundRectXZ(4.42F, 2.45F, 1.1F, 11.0F, 6), G.RECT, 0.14F, 0.95F, new float[]{0, 1, 0}, true);
            // toka
            g.sweep(metal, new float[][]{{-1.2F, 10.0F, -2.65F}, {1.2F, 10.0F, -2.65F}, {1.2F, 12.0F, -2.65F}, {-1.2F, 12.0F, -2.65F}}, G.ROUND8, 0.2F, 0.2F,
                    UP_Z, true);
            g.rod(metal, new float[]{-1.2F, 11.0F, -2.75F}, new float[]{0.6F, 11.0F, -2.85F}, 0.11F);
            g.box(band.tint(0.85F), 1.4F, 10.3F, -2.75F, 2.0F, 11.7F, -2.55F);
            if (variant >= 1) {
                Mat pouch = mat("leather", variant == 1 ? 0x8E9256 : 0x7E5232, C);
                g.rbox(pouch, 2.4F, 10.4F, -3.2F, 4.4F, 13.6F, -1.4F, 0.5F, 3);
                g.rbox(pouch.tint(1.1F), 2.3F, 10.2F, -3.3F, 4.5F, 11.6F, -1.3F, 0.35F, 2);
                g.sphere(BRASS, 3.4F, 11.4F, -3.35F, 0.22F, 0.22F, 0.15F, 6, 4);
            }
            if (variant == 1) {
                g.rbox(mat("leather", 0x8E9256, C), -1.8F, 10.4F, 2.2F, 1.8F, 13.4F, 3.9F, 0.5F, 3);
                g.rod(STEEL_D, new float[]{-4.6F, 10.2F, -0.5F}, new float[]{-4.6F, 14.6F, -0.2F}, 0.22F);
                g.box(STEEL_D, -5.0F, 9.6F, -1.4F, -4.2F, 10.4F, 0.6F);
            }
            if (variant == 2) {
                g.rbox(mat("leather", 0x7E5232, C), -4.4F, 10.4F, -2.9F, -3.4F, 13.2F, -0.9F, 0.4F, 2);
                int[] cols = {0xD8303A, 0x30C86A, 0x3A7AE0};
                float[] xs = {-2.7F, -1.6F, 2.0F};
                for (int i = 0; i < 3; i++) {
                    g.ps.pushPose();
                    g.ps.translate(xs[i], 11.4F, -2.95F);
                    g.ps.scale(0.32F, -0.32F, -0.32F);
                    miniVial(g, cols[i], a);
                    g.ps.popPose();
                }
            }
        });
    }

    /** Kemerdeki küçük şişe (Y yukarı uzayda; sıvısı çalkalanır). */
    private static void miniVial(G g, int col, Anim a) {
        float[][] out = {{0, -5}, {1.6F, -5}, {1.9F, -4.4F}, {1.9F, 3}, {1.3F, 3.8F}};
        float[][] in = {{0, -4.6F}, {1.55F, -4.6F}, {1.6F, 2.8F}, {1.0F, 3.4F}};
        liquid(g, in, 1, 1, 2, 0.7F, col, col, 0.3F, a, 12, 0, 0);
        g.lathe(GLASS, out, 12);
        g.lathe(CORK, new float[][]{{0, 3.6F}, {1.1F, 3.6F}, {1.2F, 5.4F}, {0, 5.6F}}, 10);
    }

    // ================================================================== KİTAPLAR

    /**
     * Kitap: sırt X=0'da, kapaklar +X yönüne uzanır, ön kapak +Z. open 0..1 kapakları sırt etrafında açar,
     * flip sayfa çevirme fazı.
     */
    public static void book(G g, int variant, float open, float flip) {
        Mat cover = variant == 0 ? mat("leather", 0xC8803A, C) : variant == 1 ? mat("leather", 0x8A4E2C, C) : mat("leather", 0x2C2B32, C);
        Mat pages = mat("pages", variant == 2 ? 0xE8CF7A : 0xF4EBD2, C);
        Mat pageFace = mat("paper", 0xF6EFDC, C);
        Mat corner = variant == 2 ? BRASS : SILVER;
        float W = 7.0F, H = 4.8F, th = 0.45F, half = 1.15F;
        float ang = open * 78F;
        g.draw(() -> {
            // sırt
            g.ps.pushPose();
            g.ps.translate(-0.15F, 0, 0);
            g.lathe(cover.tint(0.85F), new float[][]{{0, -H - 0.1F}, {half + th, -H - 0.1F}, {half + th, H + 0.1F}, {0, H + 0.1F}}, 14, true, 0.55F, 1, 2);
            g.ps.popPose();
            for (int s = -1; s <= 1; s += 2) {
                g.ps.pushPose();
                g.ps.mulPose(Axis.YP.rotationDegrees(-s * ang));
                float zc = s * (half + th / 2);
                // kapak
                g.rbox(cover, 0, -H - 0.15F, zc - th / 2, W + 0.35F, H + 0.15F, zc + th / 2, 0.2F, 2);
                // sayfa yarısı
                float z0 = s > 0 ? 0 : -half, z1 = s > 0 ? half : 0;
                g.rbox(pages, 0.2F, -H + 0.2F, z0, W, H - 0.2F, z1, 0.15F, 2);
                if (open > 0.05F) {
                    float fz = s > 0 ? 0.01F : -0.01F;
                    g.quad(pageFace, new float[]{0.4F, -H + 0.4F, fz, 0, 1}, new float[]{W - 0.2F, -H + 0.4F, fz, 1, 1}, new float[]{W - 0.2F, H - 0.4F, fz, 1, 0},
                            new float[]{0.4F, H - 0.4F, fz, 0, 0});
                    if (variant == 2) g.quad(mat("rune", 0xFFFFFF, open, L), new float[]{0.6F, -H + 0.6F, fz * 2, 0, 1},
                            new float[]{W - 0.4F, -H + 0.6F, fz * 2, 1, 1}, new float[]{W - 0.4F, H - 0.6F, fz * 2, 1, 0}, new float[]{0.6F, H - 0.6F, fz * 2, 0, 0});
                }
                // köşeler
                float zo = zc + s * (th / 2 + 0.02F);
                if (variant >= 1) {
                    for (int cy = -1; cy <= 1; cy += 2) {
                        float yy = cy * (H + 0.15F);
                        g.ps.pushPose();
                        g.ps.translate(W + 0.35F, yy, 0);
                        float[][] tri = {{0, 0}, {-1.6F, 0}, {0, -cy * 1.6F}};
                        g.extrude(corner, tri, zo - s * 0.62F - 0.06F, zo + 0.08F, corner);
                        g.ps.popPose();
                    }
                }
                if (s > 0) frontDecor(g, variant, W, H, zo, open);
                g.ps.popPose();
            }
            // çevrilen sayfalar
            if (open > 0.1F) {
                for (int i = 0; i < 2; i++) {
                    float ph = (flip + i * 0.5F) % 1F;
                    float pa = -ang + ph * ang * 2;
                    g.ps.pushPose();
                    g.ps.mulPose(Axis.YP.rotationDegrees(-pa));
                    float[][] strip = new float[7][];
                    for (int k = 0; k < 7; k++) {
                        float x = 0.3F + (W - 0.6F) * k / 6F;
                        float bend = (float) Math.sin(ph * Math.PI) * 1.2F * (k / 6F) * (k / 6F) * Math.signum(0.5F - ph);
                        strip[k] = new float[]{x, 0, bend};
                    }
                    for (int k = 0; k < 6; k++) {
                        g.quad(pageFace, new float[]{strip[k][0], -H + 0.45F, strip[k][2], k / 6F, 1}, new float[]{strip[k + 1][0], -H + 0.45F, strip[k + 1][2], (k + 1) / 6F, 1},
                                new float[]{strip[k + 1][0], H - 0.45F, strip[k + 1][2], (k + 1) / 6F, 0}, new float[]{strip[k][0], H - 0.45F, strip[k][2], k / 6F, 0});
                    }
                    g.ps.popPose();
                }
            }
            // kapalıyken kitabı saran kayış / kemerler
            if (open < 0.05F && variant == 1) {
                g.ps.pushPose();
                g.ps.translate(W / 2 + 0.15F, 0, 0);
                g.sweep(mat("plain", 0xEDE5D3, C), G.roundRectXZ(W / 2 + 0.25F, half + th + 0.12F, 0.5F, -0.4F, 4), G.RECT, 0.08F, 1.1F, new float[]{0, 1, 0}, true);
                g.ps.popPose();
            }
            if (open < 0.05F && variant == 2) {
                for (float yy : new float[]{-1.4F, 1.7F}) {
                    g.ps.pushPose();
                    g.ps.translate(W / 2 + 0.15F, 0, 0);
                    g.sweep(mat("leather", 0x9A5E32, C), G.roundRectXZ(W / 2 + 0.25F, half + th + 0.15F, 0.5F, yy, 4), G.RECT, 0.09F, 1.0F, new float[]{0, 1, 0}, true);
                    g.ps.popPose();
                    buckle(g, BRASS, W - 0.9F, yy, half + th + 0.28F, 0.75F, 0.6F);
                }
            }
            // kurdeleler (sayfaların arasından sarkar)
            if (variant >= 1) {
                Mat rib = variant == 1 ? mat("plain", 0x8A5AC8, C) : RED;
                g.box(rib, W * 0.55F, H - 0.2F, -0.08F, W * 0.55F + 0.6F, H + 1.6F, 0.08F);
                if (variant == 2) g.box(mat("plain", 0xD8B030, C), W * 0.72F, H - 0.2F, -0.08F, W * 0.72F + 0.5F, H + 1.2F, 0.08F);
            }
        });
    }

    private static void frontDecor(G g, int variant, float W, float H, float z, float open) {
        if (variant == 0) {
            g.quad(mat("label", 0xFFFFFF, C), new float[]{1.2F, -0.4F, z + 0.03F, 0, 1}, new float[]{W - 0.6F, -0.6F, z + 0.03F, 1, 1},
                    new float[]{W - 0.6F, 1.9F, z + 0.03F, 1, 0}, new float[]{1.2F, 2.1F, z + 0.03F, 0, 0});
            for (float yy : new float[]{-3.4F, 3.4F}) {
                g.rod(STRING, new float[]{0.2F, yy, z + 0.12F}, new float[]{3.4F, yy - 0.2F, z + 0.14F}, 0.12F);
                g.sphere(STRING, 3.5F, yy - 0.2F, z + 0.15F, 0.3F, 0.3F, 0.2F, 6, 4);
            }
        } else if (variant == 2) {
            // kabartma mühür
            g.ps.pushPose();
            g.ps.translate(W / 2 + 0.3F, 0, z);
            float[][] star = new float[10][];
            for (int i = 0; i < 10; i++) {
                double an = Math.PI * 2 * i / 10 + Math.PI / 2;
                float rr = i % 2 == 0 ? 1.5F : 0.6F;
                star[i] = new float[]{(float) Math.cos(an) * rr, (float) Math.sin(an) * rr};
            }
            g.extrude(BRASS_D, star, 0, 0.12F, BRASS_D);
            g.ps.popPose();
        }
    }

    // ================================================================== ANAHTARLAR (X ekseni boyunca; çağıran 45° döndürür)

    private static void keyShaft(G g, Mat m, float x0, float x1, float r) {
        g.rod(m, new float[]{x0, 0, 0}, new float[]{x1, 0, 0}, r);
        g.sphere(m, x1, 0, 0, r, r, r, 8, 6);
    }

    private static void collar(G g, Mat m, float x, float r) {
        g.ps.pushPose();
        g.ps.translate(x, 0, 0);
        g.ps.mulPose(Axis.ZP.rotationDegrees(90));
        g.lathe(m, new float[][]{{r * 0.6F, -0.35F}, {r, -0.25F}, {r, 0.25F}, {r * 0.6F, 0.35F}}, 12);
        g.ps.popPose();
    }

    private static void bit(G g, Mat m, float x, float len, float depth, int notches) {
        float[][] poly = new float[4 + notches * 2][];
        int k = 0;
        poly[k++] = new float[]{x, 0.2F};
        poly[k++] = new float[]{x + len, 0.2F};
        poly[k++] = new float[]{x + len, -depth};
        float step = len / (notches + 1);
        for (int i = notches; i >= 1; i--) {
            poly[k++] = new float[]{x + step * i + step * 0.25F, -depth};
            poly[k++] = new float[]{x + step * i, -depth * 0.55F};
        }
        poly[k] = new float[]{x, -depth};
        g.extrude(m, poly, -0.35F, 0.35F, m.tint(0.85F));
    }

    private static void ovalBow(G g, Mat m, float cx, float rx, float ry, float tube) {
        g.sweep(m, G.arc(cx, 0, 0, rx, ry, 0, 360, 28), G.ROUND8, tube, tube, UP_Z, true);
    }

    public static void ironKey(G g, Anim a) {
        g.draw(() -> {
            keyShaft(g, IRON, -2.8F, 6.8F, 0.55F);
            collar(g, STEEL_D, -2.0F, 0.85F);
            collar(g, STEEL_D, 0.6F, 0.8F);
            ovalBow(g, IRON, -5.0F, 2.3F, 1.9F, 0.55F);
            bit(g, IRON, 4.6F, 2.2F, 2.6F, 2);
        });
    }

    public static void dungeonKey(G g, Anim a) {
        g.draw(() -> {
            g.sweep(STEEL_D, G.arc(-6.0F, 0, 0, 2.0F, 2.0F, 0, 360, 24), G.ROUND8, 0.35F, 0.35F, new float[]{0, 1, 0}, true);
            for (int i = 0; i < 2; i++) {
                g.ps.pushPose();
                g.ps.translate(-6.0F, 0, 0);
                g.ps.mulPose(Axis.ZP.rotationDegrees(i == 0 ? 14 + a.tx * 20 : -26 - a.tx * 25));
                g.ps.mulPose(Axis.XP.rotationDegrees(i * 20));
                g.ps.translate(4.4F, 0, i * 0.6F);
                Mat m = i == 0 ? mat("metal", 0x99A2A8, C) : mat("metal", 0x828A90, C);
                keyShaft(g, m, -2.2F, 6.2F, 0.5F);
                collar(g, STEEL_D, -1.5F, 0.75F);
                ovalBow(g, m, -3.9F, 1.6F, 1.4F, 0.45F);
                bit(g, m, 4.2F, 1.9F, 2.1F, i + 1);
                g.ps.popPose();
            }
        });
    }

    public static void heartKey(G g, Anim a) {
        g.draw(() -> {
            float[][] heart = new float[40][];
            for (int i = 0; i < 40; i++) {
                double t = Math.PI * 2 * i / 40;
                float hx = 16 * (float) Math.pow(Math.sin(t), 3);
                float hy = (float) (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));
                // kalp sola bakar (sapa doğru ucu)
                heart[i] = new float[]{-4.2F - hy * 0.16F, hx * 0.16F, 0};
            }
            g.sweep(GOLD, heart, G.ROUND8, 0.5F, 0.5F, UP_Z, true);
            keyShaft(g, GOLD, -1.6F, 5.4F, 0.45F);
            collar(g, GOLD_D, -1.0F, 0.7F);
            bit(g, GOLD, 3.6F, 1.8F, 1.9F, 1);
            g.sphere(mat("gem", 0xE0304A, 0.9F, T), -4.4F, 0, 0, 0.75F, 0.75F, 0.5F, 10, 6);
        });
    }

    public static void mechanicKey(G g, Anim a) {
        g.draw(() -> {
            float[][] gear = new float[32][];
            for (int i = 0; i < 32; i++) {
                double an = Math.PI * 2 * i / 32;
                float r = (i / 2) % 2 == 0 ? 2.9F : 2.3F;
                gear[i] = new float[]{-4.6F + (float) Math.cos(an) * r, (float) Math.sin(an) * r};
            }
            g.ps.pushPose();
            g.ps.translate(-4.6F, 0, 0);
            g.ps.mulPose(Axis.ZP.rotation(a.time * 0.02F));
            g.ps.translate(4.6F, 0, 0);
            g.extrude(ORANGE_PAINT, gear, -0.5F, 0.5F, mat("metal", 0xB05A20, C));
            g.ps.popPose();
            g.ps.pushPose();
            g.ps.translate(-4.6F, 0, 0);
            g.ps.mulPose(Axis.XP.rotationDegrees(90));
            g.lathe(STEEL_D, new float[][]{{0, -0.7F}, {1.0F, -0.7F}, {1.0F, 0.7F}, {0, 0.7F}}, 12);
            g.ps.popPose();
            keyShaft(g, IRON, -2.0F, 6.0F, 0.5F);
            g.box(ORANGE_PAINT, -0.8F, -0.85F, -0.85F, 0.8F, 0.85F, 0.85F);
            g.box(STEEL_D, 5.4F, -0.3F, -0.3F, 7.2F, 0.3F, 0.3F);
            g.box(IRON, 6.6F, -1.2F, -0.35F, 7.2F, 1.2F, 0.35F);
            bit(g, IRON, 3.8F, 1.4F, 1.6F, 1);
        });
    }

    public static void thornLockpick(G g, Anim a) {
        Mat dark = mat("metal", 0x5A5C66, C);
        g.draw(() -> {
            keyShaft(g, dark, -1.5F, 7.4F, 0.45F);
            for (int i = 0; i < 6; i++) {
                float x = 0.4F + i * 1.1F;
                g.ps.pushPose();
                g.ps.translate(x, 0, 0);
                g.ps.mulPose(Axis.XP.rotationDegrees(i * 70));
                g.ps.mulPose(Axis.ZP.rotationDegrees(-35));
                g.lathe(mat("metal", 0x7C7E8A, C), new float[][]{{0.35F, 0}, {0, 1.6F - i * 0.1F}}, 6, false, 1, 1, 2);
                g.ps.popPose();
            }
            g.lathe(SILVER, new float[][]{{0.45F, 7.0F}, {0, 8.8F}}, 6, false, 1, 1, 2);
            g.ps.pushPose();
            g.ps.mulPose(Axis.ZP.rotationDegrees(-90));
            g.lathe(SILVER, new float[][]{{0.45F, 7.0F}, {0, 8.6F}}, 6, false, 1, 1, 2);
            g.ps.popPose();
            g.rod(BONE, new float[]{-6.0F, 0, 0}, new float[]{-1.4F, 0, 0}, 0.8F);
            g.sphere(BONE, -6.2F, 0, 0, 1.15F, 1.15F, 1.15F, 10, 8);
            g.sphere(BONE, -1.4F, 0, 0, 0.95F, 0.95F, 0.95F, 10, 8);
        });
    }

    public static void crystalKey(G g, Anim a) {
        Mat crystal = mat("crystal", 0x6AB4FF, 0.75F, T), glow = mat("crystal", 0x4A8CFF, 0.4F, L);
        g.draw(() -> {
            keyShaft(g, SILVER, -2.4F, 4.0F, 0.42F);
            collar(g, SILVER_D, -2.0F, 0.75F);
            // tutamak: altıgen kristal
            g.ps.pushPose();
            g.ps.translate(-4.8F, 0, 0);
            g.ps.mulPose(Axis.ZP.rotationDegrees(90));
            float[][] hex = {{0, -2.4F}, {1.6F, -1.4F}, {1.6F, 1.4F}, {0, 2.4F}};
            g.lathe(crystal, hex, 6, false, 1, 1, 2);
            g.lathe(glow, hex, 6, false, 0.9F, 0.9F, 2);
            g.ps.popPose();
            // uç: kristal kümesi
            float[][] tips = {{4.2F, 0, 0, 0, 2.6F}, {4.4F, 0.6F, 0.4F, 35, 1.8F}, {4.3F, -0.6F, -0.3F, -40, 1.6F}, {5.4F, 0.2F, 0, 15, 1.5F}};
            for (float[] t : tips) {
                g.ps.pushPose();
                g.ps.translate(t[0], t[1], t[2]);
                g.ps.mulPose(Axis.ZP.rotationDegrees(-90 + t[3]));
                float[][] prism = {{0, 0}, {0.6F, 0.3F}, {0.6F, t[4]}, {0, t[4] + 0.9F}};
                g.lathe(crystal, prism, 6, false, 1, 1, 2);
                g.lathe(glow, prism, 6, false, 0.95F, 0.95F, 2);
                g.ps.popPose();
            }
        });
    }

    public static void keyRing(G g, Anim a) {
        g.draw(() -> {
            g.sweep(IRON, G.arc(0, 3.0F, 0, 3.4F, 3.4F, 0, 360, 32), G.ROUND8, 0.35F, 0.35F, UP_Z, true);
            Mat[] mats = {IRON, GOLD, mat("metal", 0x8A9298, C), BRASS};
            for (int i = 0; i < 4; i++) {
                double base = Math.toRadians(-125 + i * 23);
                float px = (float) Math.cos(base) * 3.4F, py = 3.0F + (float) Math.sin(base) * 3.4F;
                float sw = (float) Math.sin(a.time * 0.12F + i * 1.3F) * 0.08F + a.tx * (0.9F + i * 0.15F);
                g.ps.pushPose();
                g.ps.translate(px, py, (i - 1.5F) * 0.35F);
                g.ps.mulPose(Axis.ZP.rotation(-1.57F + (i - 1.5F) * 0.22F + sw));
                g.ps.mulPose(Axis.XP.rotation(a.tz * 0.5F));
                Mat m = mats[i];
                ovalBow(g, m, 1.3F, 1.1F, 0.9F, 0.3F);
                keyShaft(g, m, 2.3F, 8.0F, 0.32F);
                bit(g, m, 6.3F, 1.5F, 1.3F, 1 + i % 2);
                g.ps.popPose();
            }
        });
    }
}
