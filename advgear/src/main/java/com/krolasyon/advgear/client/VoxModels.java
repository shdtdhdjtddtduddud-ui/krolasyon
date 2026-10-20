package com.krolasyon.advgear.client;

import com.krolasyon.advgear.client.Vox.Col;
import com.krolasyon.advgear.client.Vox.Mesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.krolasyon.advgear.client.Vox.CUT;
import static com.krolasyon.advgear.client.Vox.TRANS;
import static com.krolasyon.advgear.client.Vox.alpha;
import static com.krolasyon.advgear.client.Vox.canvas;
import static com.krolasyon.advgear.client.Vox.grad;
import static com.krolasyon.advgear.client.Vox.leather;
import static com.krolasyon.advgear.client.Vox.metal;
import static com.krolasyon.advgear.client.Vox.solid;

/**
 * Tüm eşyaların voksel (Minecraft tarzı, yarım piksel çözünürlüklü) modelleri. Durağan parçalar bir kez pişirilip
 * önbelleğe alınır; sıvı yüzeyi, sallanan etiket/anahtarlar, dönen dişli ve açılan kitap kareler arasında hareket eder.
 * Eşya uzayı: Y yukarı, ön +Z. Giyilen modeller: oyuncu baş/gövde uzayı (Y aşağı, ön -Z).
 */
public final class VoxModels {
    private VoxModels() {}

    /** Animasyon girdileri. */
    public record Anim(float time, float tx, float tz, float wave, float open, float flip) {}

    private static final Map<String, Mesh> CACHE = new HashMap<>();
    private static final Map<String, Mesh> DYN = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Mesh> e) {
            return size() > 400;
        }
    };

    private static Mesh mesh(String key, Consumer<Vox> build) {
        Mesh m = CACHE.get(key);
        if (m == null) {
            Vox v = new Vox();
            build.accept(v);
            m = v.bake();
            CACHE.put(key, m);
        }
        return m;
    }

    // ------------------------------------------------------------------ renk paleti
    static final int GOLD = 0xF2BE3C, GOLD_D = 0xB8862A, SILVER = 0xD8DEE3, SILVER_D = 0x8E979E, IRON = 0xA9B0B6, IRON_D = 0x5E656C;
    static final int BRASS = 0xD49840, BRASS_D = 0x9A6A28, DARK = 0x1C1818, CORK = 0xB5864F, PAPER = 0xF2ECDB, BONE = 0xEDE5CC;
    static final int GLASS = 0xDDF2FF;

    // ------------------------------------------------------------------ sıvı (dinamik, nicemlenmiş önbellek)

    private static float q(float v, float step) {
        return Math.round(v / step) * step;
    }

    /** İç profilli şişedeki sıvı + yüzeyde dalga; eğim ve dalga fazı nicemlenir (voksel basamakları zaten kaba). */
    private static void liquid(PoseStack ps, MultiBufferSource buf, int light, String name, float[][] in, float ax, float az, float p, float fill,
                               int col, boolean glow, Anim a) {
        float tx = q(a.tx * 0.9F, 0.06F), tz = q(a.tz * 0.9F, 0.06F), wave = q(Math.min(0.9F, a.wave), 0.1F);
        float ph = q((a.time * 0.12F) % 6.2832F, 0.524F);
        String key = name + "|" + tx + "|" + tz + "|" + wave + "|" + ph;
        Mesh m = DYN.get(key);
        if (m == null) {
            Vox v = new Vox();
            float y0 = in[0][1], y1 = in[in.length - 1][1];
            float h = y0 + (y1 - y0) * fill;
            float R = 0;
            for (float[] r : in) R = Math.max(R, r[0]);
            Col c = (x, y, z, i, j, k) -> {
                float top = h + tx * x + tz * z;
                int base = 0xFF000000 | col;
                if (y > top - 0.6F) return Vox.scale(Vox.argb(col, 0.88F), 1.35F);      // yüzey açık renk
                return Vox.scale(Vox.argb(col, 0.82F), 0.9F + Vox.hash(i, j, k) * 0.15F);
            };
            v.fill(-R * ax, y0, -R * az, R * ax, y1, R * az, (x, y, z) -> {
                float lim = Vox.rAt(in, y);
                if (lim <= 0 || Vox.norm(x / ax, z / az, p) > lim) return false;
                float surf = h + tx * x + tz * z + wave * 0.7F * (float) (Math.sin(x * 0.9 + ph) * Math.cos(z * 0.8 - ph * 0.7));
                return y <= surf;
            }, c, TRANS, glow);
            m = v.bake();
            DYN.put(key, m);
        }
        m.render(ps, buf, light);
    }

    /** Sıvının içinde yükselen kabarcıklar (tek voksel küpler). */
    private static void bubbles(PoseStack ps, MultiBufferSource buf, int light, float[][] in, float fill, int n, int col, Anim a) {
        Mesh b = mesh("bubble" + col, v -> v.fill(0, 0, 0, 0.5F, 0.5F, 0.5F, (x, y, z) -> true, Vox.alpha(col, 0.7F), TRANS, false));
        float y0 = in[0][1], y1 = in[in.length - 1][1], top = y0 + (y1 - y0) * fill - 0.8F;
        for (int i = 0; i < n; i++) {
            float ph = (a.time * (0.018F + 0.006F * (i % 3)) + i * 0.37F) % 1F;
            float y = y0 + 0.5F + ph * (top - y0);
            float rr = Vox.rAt(in, y) * 0.55F;
            float ang = i * 2.4F;
            ps.pushPose();
            ps.translate(Math.round(Math.cos(ang) * rr * 2) / 2F, Math.round(y * 2) / 2F, Math.round(Math.sin(ang) * rr * 2) / 2F);
            b.render(ps, buf, light);
            ps.popPose();
        }
    }

    // ================================================================== İKSİRLER

    public static void heartTonic(PoseStack ps, MultiBufferSource buf, int light, Anim a) {
        float[][] out = {{2.9F, -6.6F}, {3.4F, -6.0F}, {3.45F, 2.2F}, {3.1F, 3.0F}, {1.9F, 3.5F}, {1.7F, 4.6F}};
        float[][] in = {{2.7F, -6.0F}, {2.85F, -5.6F}, {2.85F, 2.0F}, {2.5F, 2.7F}, {1.3F, 3.1F}};
        liquid(ps, buf, light, "tonic", in, 1, 0.72F, 6, 0.66F, 0x6B4FE0, true, a);
        bubbles(ps, buf, light, in, 0.66F, 5, 0xDCD4FF, a);
        mesh("tonic", v -> {
            v.lathe(out, 1, 0.72F, 6, 0.55F, alpha(GLASS, 0.3F), TRANS, false);
            v.lathe(new float[][]{{0, -6.9F}, {3.0F, -6.9F}, {3.0F, -6.4F}}, 1, 0.72F, 6, 0, alpha(GLASS, 0.3F), TRANS, false);
            v.lathe(new float[][]{{3.6F, -6.9F}, {3.6F, -5.4F}}, 1, 0.72F, 6, 0.55F, solid(0x2E4A72), CUT, false);
            v.lathe(new float[][]{{2.05F, 3.6F}, {2.05F, 4.4F}}, 1, 1, 2, 0.6F, metal(0x84C6E8), CUT, false);
            v.lathe(new float[][]{{0, 4.5F}, {2.2F, 4.5F}, {2.2F, 6.6F}, {1.6F, 7.1F}, {0, 7.1F}}, 1, 1, 2, 0,
                    (x, y, z, i, j, k) -> Vox.scale(0xFF2C2C33, (j & 1) == 0 ? 1.15F : 0.85F), CUT, false);
            // cam parlaması
            v.box(-2.6F, -4.5F, 2.2F, -2.1F, 1.5F, 2.7F, alpha(0xFFFFFF, 0.55F));
        }).render(ps, buf, light);
        // sallanan kalp etiketi
        ps.pushPose();
        ps.translate(2.0F, 4.0F, 0);
        ps.mulPose(Axis.ZP.rotation(-0.3F - a.tx * 0.9F));
        ps.mulPose(Axis.XP.rotation(a.tz * 0.6F));
        mesh("tonic_tag", v -> {
            v.rod(new float[]{0, 0, 0}, new float[]{1.4F, -2.4F, 0.8F}, 0.25F, solid(0xD9CBA8));
            v.push().translate(1.4F, -2.4F, 0.8F).rotY(-25);
            v.box(-1.5F, -5.0F, -0.25F, 1.5F, 0F, 0.25F, (x, y, z, i, j, k) -> {
                float hx = x / 1.05F, hy = -(y + 2.7F) / 1.05F;
                if (Vox.sq(x) + Vox.sq(y + 0.6F) < 0.2F) return 0xFF5A5040;
                boolean heart = Math.pow(hx * hx + hy * hy - 1, 3) - hx * hx * hy * hy * hy < 0;
                return heart ? 0xFFD8304A : Vox.scale(0xFFF4EFE2, 0.95F + Vox.hash(i, j, k) * 0.08F);
            });
            v.pop();
        }).render(ps, buf, light);
        ps.popPose();
    }

    public static void oceanDraught(PoseStack ps, MultiBufferSource buf, int light, Anim a) {
        float[][] out = {{2.4F, -6.8F}, {3.8F, -6.0F}, {4.6F, -4.2F}, {4.6F, -2.4F}, {4.0F, -0.4F}, {2.9F, 1.8F}, {1.8F, 3.7F}, {1.3F, 4.8F}, {1.4F, 5.6F}};
        float[][] in = {{2.0F, -6.2F}, {3.4F, -5.6F}, {4.0F, -4.1F}, {4.0F, -2.5F}, {3.45F, -0.5F}, {2.4F, 1.6F}, {1.3F, 3.4F}};
        liquid(ps, buf, light, "ocean", in, 1, 1, 2, 0.58F, 0x1FB3C9, false, a);
        bubbles(ps, buf, light, in, 0.58F, 9, 0xE6FDFF, a);
        mesh("ocean", v -> {
            v.lathe(out, 1, 1, 2, 0.55F, alpha(GLASS, 0.28F), TRANS, false);
            v.lathe(new float[][]{{0, -7.1F}, {2.5F, -7.1F}, {2.5F, -6.6F}}, 1, 1, 2, 0, alpha(GLASS, 0.28F), TRANS, false);
            v.lathe(new float[][]{{0, 5.1F}, {1.25F, 5.1F}, {1.5F, 7.3F}, {0, 7.6F}}, (x, y, z, i, j, k) -> Vox.scale(0xFF000000 | CORK,
                    Vox.hash(i, j, k) < 0.15F ? 0.7F : 0.92F + Vox.hash(i, j, k) * 0.15F));
            v.lathe(new float[][]{{1.65F, 5.4F}, {1.65F, 5.9F}}, 1, 1, 2, 0.5F, solid(0x6F4A2A), CUT, false);
            v.box(-3.6F, -4.5F, 2.6F, -3.1F, -1.0F, 3.1F, alpha(0xFFFFFF, 0.55F));
        }).render(ps, buf, light);
    }

    public static void blackDeath(PoseStack ps, MultiBufferSource buf, int light, Anim a) {
        float[][] out = {{4.4F, -7.0F}, {4.6F, -6.4F}, {1.7F, 3.0F}, {1.3F, 3.6F}, {1.3F, 4.3F}};
        float[][] in = {{3.8F, -6.4F}, {3.95F, -5.8F}, {1.35F, 2.4F}, {0.9F, 3.0F}};
        liquid(ps, buf, light, "death", in, 1, 1, 8, 0.55F, 0xE0257E, true, a);
        bubbles(ps, buf, light, in, 0.55F, 4, 0xFFC0E0, a);
        mesh("death", v -> {
            v.lathe(out, 1, 1, 8, 0.55F, alpha(0xFF7AB8, 0.38F), TRANS, false);
            v.lathe(new float[][]{{0, -7.3F}, {4.4F, -7.3F}, {4.4F, -6.8F}}, 1, 1, 8, 0, alpha(0xFF7AB8, 0.38F), TRANS, false);
            v.lathe(new float[][]{{1.8F, 4.2F}, {1.8F, 5.0F}}, 1, 1, 2, 0.6F, metal(SILVER), CUT, false);
            // kurukafa tıpa
            v.ell(0, 6.6F, 0, 2.0F, 1.9F, 1.9F, metal(SILVER));
            v.lathe(new float[][]{{1.4F, 7.6F}, {0.9F, 8.8F}, {0.3F, 9.9F}}, metal(SILVER));
            v.box(-1.3F, 4.9F, -0.4F, 1.3F, 5.6F, 1.7F, metal(SILVER_D));
            for (int i = -2; i <= 2; i++) v.box(i * 0.5F - 0.2F, 4.9F, 1.6F, i * 0.5F + 0.2F, 5.6F, 1.95F, solid(BONE));
            v.ell(-0.75F, 6.5F, 1.55F, 0.6F, 0.65F, 0.55F, solid(DARK));
            v.ell(0.75F, 6.5F, 1.55F, 0.6F, 0.65F, 0.55F, solid(DARK));
            v.ell(-0.75F, 6.5F, 1.75F, 0.25F, 0.25F, 0.3F, solid(0xFF3070), CUT, true);
            v.ell(0.75F, 6.5F, 1.75F, 0.25F, 0.25F, 0.3F, solid(0xFF3070), CUT, true);
            v.box(-0.2F, 5.7F, 1.6F, 0.2F, 6.0F, 2.0F, solid(DARK));
            v.box(-2.4F, -5.5F, 1.6F, -1.9F, 0.0F, 2.1F, alpha(0xFFFFFF, 0.5F));
        }).render(ps, buf, light);
    }

    public static void starlightElixir(PoseStack ps, MultiBufferSource buf, int light, Anim a) {
        float R = 5.0F, cy = -1.2F;
        float[][] in = new float[13][];
        for (int i = 0; i <= 12; i++) {
            double t = -Math.PI / 2 + Math.PI * i / 12;
            in[i] = new float[]{(float) Math.cos(t) * (R - 0.6F), cy + (float) Math.sin(t) * (R - 0.6F)};
        }
        liquid(ps, buf, light, "star", in, 1, 1, 2, 0.6F, 0x2B3BD8, true, a);
        // yıldız tozu: parıldayan tek vokseller
        Mesh spark = mesh("spark", v -> v.fill(0, 0, 0, 0.5F, 0.5F, 0.5F, (x, y, z) -> true, solid(0xF6F8FF), CUT, true));
        for (int i = 0; i < 8; i++) {
            float ph = a.time * 0.03F + i * 0.8F;
            if (Math.sin(ph * 3 + i) < 0.2) continue;
            ps.pushPose();
            ps.translate(Math.round(Math.cos(ph + i) * 4.4F) / 2F, Math.round((cy - 2.6F + (i % 4) * 0.7F) * 2) / 2F, Math.round(Math.sin(ph * 1.3F + i) * 4.4F) / 2F);
            spark.render(ps, buf, light);
            ps.popPose();
        }
        mesh("star", v -> {
            v.fill(-R, cy - R, -R, R, cy + R, R, (x, y, z) -> {
                float d = (float) Math.sqrt(x * x + Vox.sq(y - cy) + z * z);
                return d <= R && d >= R - 0.55F && !(y > cy + R - 1.2F && Math.abs(x) < 1.2F && Math.abs(z) < 1.2F);
            }, alpha(GLASS, 0.24F), TRANS, false);
            // gümüş kafes
            v.push().translate(0, cy, 0).rotX(90);
            v.ringXY(0, 0, 0, R + 0.3F, R + 0.3F, 0.35F, metal(SILVER_D));
            v.pop();
            for (int kk = 0; kk < 4; kk++) {
                v.push().translate(0, cy, 0).rotY(45 * kk);
                v.ringXY(0, 0, 0, R + 0.3F, R + 0.3F, 0.3F, metal(SILVER_D));
                v.pop();
            }
            v.lathe(new float[][]{{2.4F, cy - R - 0.6F}, {2.4F, cy - R + 0.4F}, {1.6F, cy - R + 0.6F}}, metal(SILVER));
            v.lathe(new float[][]{{1.6F, 3.7F}, {1.6F, 5.6F}, {0.7F, 6.0F}, {0.6F, 6.8F}}, metal(SILVER));
            v.ringXY(-0.85F, 7.6F, 0, 0.8F, 0.8F, 0.3F, metal(SILVER));
            v.ringXY(0.85F, 7.6F, 0, 0.8F, 0.8F, 0.3F, metal(SILVER));
            v.box(-3.7F, -2.0F, 3.0F, -3.2F, 1.0F, 3.5F, alpha(0xFFFFFF, 0.5F));
        }).render(ps, buf, light);
    }

    public static void sunEssence(PoseStack ps, MultiBufferSource buf, int light, Anim a) {
        float[][] out = {{1.4F, -6.3F}, {1.9F, -5.2F}, {2.0F, -3F}, {2.0F, 3.6F}, {1.8F, 4.7F}, {1.4F, 5.2F}};
        float[][] in = {{1.2F, -5.7F}, {1.45F, -5.0F}, {1.5F, -3F}, {1.5F, 3.5F}, {1.2F, 4.5F}};
        liquid(ps, buf, light, "sun", in, 1, 1, 2, 0.82F, 0xFFB21E, true, a);
        bubbles(ps, buf, light, in, 0.82F, 4, 0xFFF4C0, a);
        mesh("sun", v -> {
            v.lathe(out, 1, 1, 2, 0.5F, alpha(GLASS, 0.25F), TRANS, false);
            v.lathe(new float[][]{{0.2F, -7.9F}, {1.4F, -6.8F}, {2.1F, -6.0F}, {2.1F, -5.2F}}, metal(BRASS));
            v.lathe(new float[][]{{2.3F, -0.4F}, {2.3F, 0.6F}}, 1, 1, 2, 0.6F, metal(BRASS), CUT, false);
            v.lathe(new float[][]{{2.0F, 4.6F}, {2.0F, 6.2F}, {1.4F, 6.8F}, {0.2F, 7.0F}}, metal(BRASS));
            v.ringXY(0, 8.3F, 0, 1.2F, 1.2F, 0.35F, metal(0x3A3A3E));
            v.box(-1.6F, -4.0F, 1.2F, -1.1F, 3.0F, 1.7F, alpha(0xFFFFFF, 0.5F));
        }).render(ps, buf, light);
    }

    // ================================================================== YÜZÜKLER (bant XY düzleminde)

    private static void band(Vox v, float R, float w, float h, Col c) {
        v.fill(-R - w, -R - w, -h / 2, R + w, R + w, h / 2, (x, y, z) -> {
            float d = (float) Math.sqrt(x * x + y * y);
            return Math.abs(d - R) <= w / 2 && Math.abs(z) <= h / 2;
        }, c, CUT, false);
    }

    public static void ring(PoseStack ps, MultiBufferSource buf, int light, int kind) {
        mesh("ring" + kind, v -> {
            switch (kind) {
                case 0 -> band(v, 4.3F, 1.6F, 3.0F, (x, y, z, i, j, k) -> {
                    // kazınmış rünler dış yüzeyde
                    float ang = (float) Math.atan2(y, x);
                    int seg = (int) Math.floor((ang + Math.PI) / (Math.PI * 2) * 22);
                    boolean outer = Math.sqrt(x * x + y * y) > 4.6F;
                    boolean mark = outer && Math.abs(z) < 0.8F && (seg % 2 == 0) && ((seg * 7 + j) % 3 == 0);
                    return Vox.scale(0xFF000000 | GOLD, mark ? 0.6F : 0.9F + Vox.hash(i, j, k) * 0.2F);
                });
                case 1 -> {
                    band(v, 4.0F, 1.2F, 2.0F, metal(GOLD));
                    v.lathe(new float[][]{{0.8F, 4.5F}, {2.1F, 5.0F}, {2.2F, 5.8F}}, metal(SILVER));
                    for (int kk = 0; kk < 4; kk++) {
                        double an = Math.PI / 4 + Math.PI / 2 * kk;
                        float x = (float) Math.cos(an) * 2.0F, z = (float) Math.sin(an) * 2.0F;
                        v.rod(new float[]{x, 5.4F, z}, new float[]{x * 0.8F, 7.4F, z * 0.8F}, 0.35F, metal(SILVER));
                    }
                    v.push().translate(0, 5.6F, 0).rotY(45);
                    v.lathe(new float[][]{{0, 4.0F}, {1.5F, 3.6F}, {2.3F, 2.4F}, {2.3F, 1.9F}, {0, 0}}, 1, 1, 8, 0,
                            (x, y, z, i, j, k) -> Vox.argb(Vox.lerp(0xFF2448D8, 0xFF7A98FF, (Vox.hash(i, j, k) + (y - 1) / 3) * 0.6F), 0.88F), TRANS, true);
                    v.pop();
                }
                case 2 -> {
                    band(v, 3.9F, 1.2F, 1.9F, metal(SILVER));
                    float y = 6.6F;
                    v.ell(0, y, 0, 2.4F, 2.4F, 2.2F, metal(SILVER));
                    v.lathe(new float[][]{{1.8F, y + 1.2F}, {1.0F, y + 2.8F}, {0.3F, y + 4.0F}}, metal(SILVER));
                    v.rbox(-1.6F, y - 2.8F, -0.6F, 1.6F, y - 1.3F, 1.9F, 0.5F, metal(SILVER));
                    for (int i = -2; i <= 2; i++) v.box(i * 0.55F - 0.2F, y - 2.5F, 1.7F, i * 0.55F + 0.2F, y - 1.6F, 2.1F, solid(0xF4F0E4));
                    v.ell(-0.9F, y + 0.1F, 1.8F, 0.7F, 0.75F, 0.6F, solid(DARK));
                    v.ell(0.9F, y + 0.1F, 1.8F, 0.7F, 0.75F, 0.6F, solid(DARK));
                    v.box(-0.25F, y - 1.1F, 1.8F, 0.25F, y - 0.6F, 2.3F, solid(DARK));
                }
                case 3 -> {
                    band(v, 3.8F, 1.0F, 1.6F, metal(SILVER_D));
                    for (int f = 0; f < 4; f++) {
                        float z = -1.6F + f * 1.07F, prevA = 72 + f * 4;
                        for (int s = 0; s < 3; s++) {
                            float a0 = prevA, a1 = a0 - 38 - s * 6, r = 4.9F;
                            float[] p0 = {(float) Math.cos(Math.toRadians(a0)) * r, (float) Math.sin(Math.toRadians(a0)) * r, z};
                            float[] p1 = {(float) Math.cos(Math.toRadians(a1)) * r, (float) Math.sin(Math.toRadians(a1)) * r, z};
                            v.rod(p0, p1, 0.45F - s * 0.05F, solid(0xE6E0D0));
                            v.ell(p1[0], p1[1], p1[2], 0.55F, 0.55F, 0.55F, solid(0xCFC8B6));
                            prevA = a1;
                        }
                    }
                    v.rbox(-2.6F, 4.6F, -2.3F, 2.2F, 6.4F, 2.3F, 0.6F, solid(0xE6E0D0));
                    for (int f = 0; f < 4; f++) v.box(-2.4F, 6.3F, -1.8F + f * 1.07F, 1.6F, 6.6F, -1.5F + f * 1.07F, solid(0xB8B09C));
                }
                default -> {
                    band(v, 4.0F, 1.1F, 1.8F, metal(SILVER));
                    for (int side = -1; side <= 1; side += 2) {
                        float[][] curl = new float[16][];
                        for (int i = 0; i < 16; i++) {
                            double t = i / 15.0 * Math.PI * 2.0;
                            float rr = 1.5F * (1 - i / 20F);
                            curl[i] = new float[]{side * (2.7F + (float) Math.cos(t) * rr), 4.5F + (float) Math.sin(t) * rr, 1.0F};
                        }
                        v.tube(curl, 0.3F, metal(SILVER_D));
                    }
                    v.push().translate(0, 4.5F, 0);
                    v.lathe(new float[][]{{1.7F, 0}, {2.4F, 0.3F}, {2.4F, 1.2F}, {1.8F, 1.4F}}, 1.25F, 1, 2, 0.7F, metal(GOLD), CUT, false);
                    v.fill(-2.2F, 0.6F, -1.8F, 2.2F, 2.6F, 1.8F, (x, y, z) -> y >= 0.6F && Vox.sq(x / 2.0F) + Vox.sq((y - 0.6F) / 1.6F) + Vox.sq(z / 1.6F) <= 1,
                            (x, y, z, i, j, k) -> Vox.argb(Vox.lerp(0xFF0E8A4C, 0xFF6AF0A8, (y - 0.6F) / 1.6F + Vox.hash(i, j, k) * 0.2F), 0.9F), TRANS, true);
                    v.box(-0.25F, 0.8F, -1.0F, 0.25F, 2.3F, 1.0F, solid(0x063A1E));
                    v.pop();
                }
            }
        }).render(ps, buf, light);
    }

    // ================================================================== GÖZLÜKLER (baş uzayı: Y aşağı, yüz z=-4)

    public static void goggles(PoseStack ps, MultiBufferSource buf, int light, int variant) {
        mesh("goggles" + variant, v -> {
            float L = variant == 2 ? 3.0F : 1.8F;
            int lens = variant == 0 ? 0x9ED8F5 : variant == 1 ? 0x2FC9B8 : 0xE0262A;
            // kayış: başın çevresinde
            float[][] loop = new float[25][];
            for (int i = 0; i <= 24; i++) {
                double an = Math.PI * 2 * i / 24;
                float c = (float) Math.cos(an), s = (float) Math.sin(an);
                float r = 4.6F / (float) Math.pow(Math.pow(Math.abs(c), 6) + Math.pow(Math.abs(s), 6), 1 / 6.0);
                loop[i] = new float[]{c * r, -4.6F, s * r};
            }
            v.strap(loop, 1.4F, 0.5F, new float[]{0, 1, 0}, leather(0x6E4A30));
            v.box(4.4F, -5.6F, 0.4F, 5.1F, -3.6F, 2.2F, metal(BRASS_D));
            v.box(-5.1F, -5.6F, 0.4F, -4.4F, -3.6F, 2.2F, metal(BRASS_D));
            for (int side = -1; side <= 1; side += 2) {
                v.push().translate(side * 2.15F, -4.6F, -4.4F).rotX(-90);
                if (variant == 2) {
                    v.lathe(new float[][]{{2.0F, 0}, {2.0F, 1.0F}, {2.35F, 1.2F}, {2.35F, 2.0F}, {2.65F, 2.2F}, {2.65F, L}}, 1, 1, 2, 0.6F, metal(BRASS), CUT, false);
                    v.lathe(new float[][]{{2.5F, 1.3F}, {2.5F, 1.9F}}, 1, 1, 2, 0.5F, metal(BRASS_D), CUT, false);
                } else {
                    v.lathe(new float[][]{{2.0F, 0}, {2.0F, L - 0.4F}, {2.4F, L - 0.3F}, {2.4F, L}}, 1, 1, 2, 0.6F, metal(BRASS), CUT, false);
                }
                float lr = variant == 2 ? 2.1F : 1.9F;
                v.lathe(new float[][]{{lr, L - 0.6F}, {lr * 0.7F, L - 0.1F}, {0, L + 0.1F}}, 1, 1, 2, 0,
                        (x, y, z, i, j, k) -> Vox.argb(Vox.scale(0xFF000000 | lens, (x < -0.5F && z < -0.2F) ? 1.5F : 0.9F + Vox.hash(i, j, k) * 0.1F),
                                variant == 0 ? 0.5F : 0.7F), TRANS, variant != 0);
                // perçinler
                for (int kk = 0; kk < 6; kk++) {
                    double an = Math.PI * 2 * kk / 6;
                    float rr = variant == 2 ? 2.7F : 2.45F;
                    v.box((float) Math.cos(an) * rr - 0.25F, L - 0.5F, (float) Math.sin(an) * rr - 0.25F, (float) Math.cos(an) * rr + 0.25F, L, (float) Math.sin(an) * rr + 0.25F,
                            solid(BRASS_D));
                }
                v.pop();
            }
            v.rod(new float[]{-0.7F, -4.4F, -5.0F}, new float[]{0.7F, -4.4F, -5.0F}, 0.35F, metal(BRASS));
            if (variant == 1) {
                v.box(-5.0F, -6.8F, -5.0F, -4.2F, -6.0F, -4.2F, metal(BRASS_D));
                v.rod(new float[]{-4.6F, -6.4F, -4.6F}, new float[]{-3.2F, -6.0F, -7.2F}, 0.25F, metal(BRASS_D));
                v.push().translate(-2.3F, -5.4F, -6.7F).rotX(-90);
                v.lathe(new float[][]{{1.2F, 0}, {1.2F, 1.3F}, {1.45F, 1.4F}, {1.45F, 1.7F}}, 1, 1, 2, 0.5F, metal(BRASS), CUT, false);
                v.lathe(new float[][]{{1.1F, 1.2F}, {0, 1.6F}}, 1, 1, 2, 0, alpha(0x2FC9B8, 0.7F), TRANS, true);
                v.pop();
            }
        }).render(ps, buf, light);
    }

    // ================================================================== SIRT ÇANTALARI (gövde uzayı: Y aşağı, sırt z=+2)

    private static void buckle(Vox v, float x, float y, float z, float w, float h, int col) {
        v.box(x - w, y - h, z, x + w, y - h + 0.5F, z + 0.5F, metal(col));
        v.box(x - w, y + h - 0.5F, z, x + w, y + h, z + 0.5F, metal(col));
        v.box(x - w, y - h, z, x - w + 0.5F, y + h, z + 0.5F, metal(col));
        v.box(x + w - 0.5F, y - h, z, x + w, y + h, z + 0.5F, metal(col));
        v.box(x - 0.25F, y - h, z + 0.3F, x + 0.25F, y + h * 0.3F, z + 0.8F, metal(col));
    }

    private static void mapWindow(Vox v, float x0, float y0, float x1, float y1, float z) {
        v.box(x0, y0, z, x1, y1, z + 0.5F, (x, y, z2, i, j, k) -> {
            float u = (x - x0) / (x1 - x0), t = (y - y0) / (y1 - y0);
            float coast = 0.55F + 0.18F * (float) Math.sin(u * 7 + 1);
            int c = t > coast ? 0xFF7FA8C8 : 0xFFEAD9A8;
            if (Math.abs(t - coast) < 0.08F) c = 0xFF5A4A32;
            if (Math.abs(u - 0.7F) < 0.07F && Math.abs(t - 0.28F) < 0.1F) c = 0xFFC02020;
            if (Math.abs(t - (0.3F + 0.1F * (float) Math.sin(u * 20))) < 0.06F && u < 0.55F && ((i & 1) == 0)) c = 0xFF8A3A2A;
            return c;
        });
        buckle(v, (x0 + x1) / 2, (y0 + y1) / 2, z + 0.2F, (x1 - x0) / 2 + 0.5F, (y1 - y0) / 2 + 0.5F, BRASS);
        v.erase(x0 - 0.1F, y0 - 0.1F, z + 0.6F, x1 + 0.1F, y1 + 0.1F, z + 1.4F, (x, y, z2) -> x > x0 && x < x1 && y > y0 && y < y1);
    }

    private static void shoulderStrap(Vox v, float x, int col) {
        float s = Math.signum(x);
        float[][] path = {{x, 2.5F, 2.4F}, {x, -0.2F, 1.6F}, {x, -0.5F, 0}, {x, -0.2F, -2.3F}, {x, 2.0F, -2.5F}, {x * 1.05F, 8.0F, -2.5F},
                {s * 4.1F, 9.5F, -2.2F}, {s * 4.3F, 10.0F, 0}, {s * 3.6F, 9.6F, 2.3F}};
        v.strap(path, 1.1F, 0.5F, new float[]{1, 0, 0}, leather(col));
    }

    public static void backpack(PoseStack ps, MultiBufferSource buf, int light, int variant, boolean worn) {
        mesh("pack" + variant + worn, v -> {
            if (variant == 0) {
                v.rbox(-3.8F, 3.0F, 2.0F, 3.8F, 10.3F, 5.4F, 1.0F, canvas(0x9CA468));
                v.rbox(3.3F, 5.0F, 2.6F, 4.7F, 9.8F, 4.9F, 0.5F, canvas(0x8E9660));
                v.rbox(-4.0F, 2.6F, 5.0F, 4.0F, 7.8F, 5.9F, 0.4F, canvas(0xB6BE80));
                v.box(-3.8F, 7.4F, 5.5F, 3.8F, 7.8F, 6.0F, solid(0x646A3C));
                v.box(-0.6F, 3.0F, 5.7F, 0.6F, 9.7F, 6.2F, leather(0x7A6A3A));
                buckle(v, 0, 8.4F, 6.1F, 1.0F, 0.8F, SILVER);
                if (worn) v.strap(new float[][]{{-3.2F, 3.4F, 2.1F}, {-3.4F, -0.3F, 1.2F}, {-3.0F, -0.5F, -1.0F}, {-2.4F, 0.6F, -2.5F}, {3.8F, 9.2F, -2.5F},
                        {4.4F, 10.0F, -0.6F}, {4.1F, 9.6F, 1.8F}, {3.4F, 8.6F, 2.3F}}, 1.2F, 0.5F, new float[]{1, 0, 0}, leather(0x7A6A3A));
                else v.strap(new float[][]{{-2.2F, 3.0F, 3.7F}, {-1.8F, 0.8F, 3.7F}, {0, 0.2F, 3.7F}, {1.8F, 0.8F, 3.7F}, {2.2F, 3.0F, 3.7F}}, 0.9F, 0.5F,
                        new float[]{0, 0, 1}, leather(0x7A6A3A));
            } else if (variant == 1) {
                v.rbox(-4.0F, 0.6F, 2.0F, 4.0F, 10.9F, 6.3F, 1.2F, canvas(0xB4A88E));
                v.rbox(-4.2F, 0.2F, 1.9F, 4.2F, 3.4F, 6.7F, 0.9F, canvas(0xCFC3A6));
                v.rbox(-3.7F, 2.6F, 6.2F, 3.7F, 7.0F, 6.9F, 0.3F, canvas(0xCFC3A6));
                mapWindow(v, -2.3F, 3.2F, 2.3F, 6.0F, 6.8F);
                v.rbox(-2.9F, 7.0F, 6.1F, 2.9F, 10.2F, 7.3F, 0.5F, canvas(0xA89C82));
                v.box(-0.6F, 6.6F, 6.9F, 0.6F, 8.0F, 7.6F, metal(BRASS));
                for (int sx = -1; sx <= 1; sx += 2) {
                    v.rbox(sx * 4.0F - 0.7F, 5.5F, 2.8F, sx * 4.0F + 0.7F, 10.2F, 5.4F, 0.4F, canvas(0xA89C82));
                    v.box(sx * 3.7F - 0.6F, 10.3F, 5.2F, sx * 3.7F + 0.6F, 11.1F, 6.5F, metal(BRASS));
                    if (worn) shoulderStrap(v, sx * 2.5F, 0x6E4E30);
                }
            } else {
                v.rbox(-4.6F, 0.0F, 2.0F, 4.6F, 11.4F, 7.4F, 1.2F, leather(0xC67C38));
                v.push().translate(0, -1.5F, 4.7F).rotZ(90);
                v.lathe(new float[][]{{1.6F, -5.3F}, {1.9F, -4.9F}, {1.9F, 4.9F}, {1.6F, 5.3F}}, (x, y, z, i, j, k) ->
                        Vox.scale(0xFF8E3E2C, ((j / 2) % 3 == 0 ? 0.8F : 1.0F) * (0.95F + Vox.hash(i, j, k) * 0.1F)));
                v.pop();
                for (int sx = -1; sx <= 1; sx += 2) {
                    v.push().translate(sx * 3.0F, -1.5F, 4.7F).rotY(90);
                    v.ringXY(0, 0, 0, 2.0F, 2.0F, 0.4F, leather(0x6A4024));
                    v.pop();
                }
                v.rbox(-4.7F, -0.2F, 7.0F, 4.7F, 5.4F, 7.8F, 0.3F, leather(0xDB944C));
                mapWindow(v, -2.5F, 0.8F, 2.5F, 3.8F, 7.7F);
                for (int sx = -1; sx <= 1; sx += 2) {
                    v.rbox(sx * 2.4F - 2.0F, 6.2F, 7.0F, sx * 2.4F + 2.0F, 10.6F, 8.3F, 0.5F, leather(0xDB944C));
                    buckle(v, sx * 2.4F, 7.2F, 8.2F, 0.7F, 0.6F, BRASS);
                    v.rbox(sx * 4.6F - 0.8F, 4.0F, 3.0F, sx * 4.6F + 0.8F, 10.0F, 6.2F, 0.5F, leather(0xB87034));
                    if (worn) shoulderStrap(v, sx * 2.6F, 0x6A4024);
                }
                v.box(-1.0F, 5.6F, 7.6F, 1.0F, 7.3F, 8.5F, metal(BRASS));
                v.box(-0.2F, 6.3F, 8.4F, 0.2F, 6.8F, 8.7F, solid(DARK));
                v.box(-0.8F, 4.6F, 7.9F, -0.4F, 5.7F, 8.3F, metal(SILVER));
                v.box(0.4F, 4.6F, 7.9F, 0.8F, 5.7F, 8.3F, metal(SILVER));
                v.box(-0.8F, 4.4F, 7.9F, 0.8F, 4.8F, 8.3F, metal(SILVER));
            }
        }).render(ps, buf, light);
    }

    // ================================================================== KEMERLER (gövde uzayı)

    public static void belt(PoseStack ps, MultiBufferSource buf, int light, int variant) {
        mesh("belt" + variant, v -> {
            int col = variant == 0 ? 0x8C7458 : variant == 1 ? 0x7E8248 : 0x6C4428;
            int met = variant == 0 ? SILVER : BRASS;
            float[][] loop = new float[29][];
            for (int i = 0; i <= 28; i++) {
                double an = Math.PI * 2 * i / 28;
                float c = (float) Math.cos(an), s = (float) Math.sin(an);
                float r = 1F / (float) Math.pow(Math.pow(Math.abs(c), 6) + Math.pow(Math.abs(s), 6), 1 / 6.0);
                loop[i] = new float[]{c * r * 4.5F, 11.0F, s * r * 2.5F};
            }
            v.strap(loop, 1.8F, 0.55F, new float[]{0, 1, 0}, (x, y, z, i, j, k) -> {
                boolean stitch = (Math.abs(y - 10.35F) < 0.26F || Math.abs(y - 11.65F) < 0.26F) && ((i + k) & 1) == 0;
                return stitch ? 0xFFE8D8B0 : Vox.scale(0xFF000000 | col, 0.9F + Vox.hash(i, j, k) * 0.15F);
            });
            buckle(v, 0, 11.0F, -3.3F, 1.3F, 1.2F, met);
            v.box(1.5F, 10.3F, -3.2F, 2.3F, 11.7F, -2.7F, solid(col));
            if (variant >= 1) {
                int pc = variant == 1 ? 0x8E9256 : 0x7E5232;
                v.rbox(2.4F, 10.3F, -3.4F, 4.6F, 13.8F, -1.3F, 0.5F, leather(pc));
                v.rbox(2.2F, 10.0F, -3.6F, 4.8F, 11.6F, -1.2F, 0.4F, leather(Vox.scale(0xFF000000 | pc, 1.12F) & 0xFFFFFF));
                v.box(3.3F, 11.2F, -3.9F, 3.8F, 11.7F, -3.5F, metal(BRASS));
            }
            if (variant == 1) {
                v.rbox(-1.9F, 10.3F, 2.2F, 1.9F, 13.5F, 4.0F, 0.5F, leather(0x8E9256));
                v.rod(new float[]{-4.8F, 10.2F, -0.5F}, new float[]{-4.8F, 14.6F, -0.2F}, 0.35F, metal(IRON_D));
                v.box(-5.3F, 9.5F, -1.5F, -4.3F, 10.5F, 0.7F, metal(IRON_D));
            }
            if (variant == 2) {
                v.rbox(-4.6F, 10.3F, -3.0F, -3.3F, 13.2F, -0.8F, 0.4F, leather(0x7E5232));
                int[] cols = {0xD8303A, 0x30C86A, 0x3A7AE0};
                float[] xs = {-2.6F, -1.4F, 2.1F};
                for (int i = 0; i < 3; i++) {
                    float x = xs[i];
                    int lc = cols[i];
                    v.fill(x - 0.6F, 9.0F, -4.2F, x + 0.6F, 12.8F, -3.0F, (px, py, pz) -> Vox.sq((px - x) / 0.6F) + Vox.sq((pz + 3.6F) / 0.6F) <= 1 && py >= 9.6F && py <= 12.8F,
                            (px, py, pz, ii, jj, kk) -> py < 10.4F ? 0xFFB5864F : Vox.argb(Vox.scale(0xFF000000 | lc, py > 11.8F ? 0.85F : 1.15F), py > 12.0F ? 0.45F : 0.9F),
                            TRANS, false);
                }
            }
        }).render(ps, buf, light);
    }

    // ================================================================== KİTAPLAR

    private static final float BW = 7.0F, BH = 4.8F, TH = 0.6F, HALF = 1.2F;

    /** Kitabın bir yarısı (kapak + sayfa yarısı + süsler). s = +1 ön, -1 arka. */
    private static void bookHalf(Vox v, int variant, int s) {
        int cover = variant == 0 ? 0xC8803A : variant == 1 ? 0x8A4E2C : 0x2C2B32;
        int pages = variant == 2 ? 0xE8CF7A : 0xF4EBD2;
        float zc = s * (HALF + TH / 2);
        v.box(0, -BH - 0.25F, zc - TH / 2, BW + 0.4F, BH + 0.25F, zc + TH / 2, leather(cover));
        float z0 = s > 0 ? 0 : -HALF, z1 = s > 0 ? HALF : 0;
        v.box(0.25F, -BH + 0.25F, z0, BW, BH - 0.25F, z1, (x, y, z, i, j, k) -> Vox.scale(0xFF000000 | pages, (j & 1) == 0 ? 1.0F : 0.88F));
        float zo = zc + s * TH / 2;
        if (variant >= 1) {
            int cm = variant == 2 ? BRASS : SILVER;
            for (int cy = -1; cy <= 1; cy += 2) {
                float yy = cy * (BH + 0.25F);
                v.extrude(new float[][]{{BW + 0.5F, yy}, {BW - 1.6F, yy}, {BW + 0.5F, yy - cy * 2.1F}}, Math.min(zc - s * TH / 2, zo + s * 0.3F) - 0.1F,
                        Math.max(zc - s * TH / 2, zo + s * 0.3F) + 0.1F, metal(cm));
            }
        }
        if (s > 0) {
            if (variant == 0) {
                // SPELLBOOK etiketi (voksel harfler)
                v.box(1.2F, -0.8F, zo, BW - 0.6F, 2.0F, zo + 0.5F, (x, y, z, i, j, k) -> label(x - 1.2F, y + 0.8F) ? 0xFF2A2622
                        : Vox.scale(0xFFF2ECDC, 0.95F + Vox.hash(i, j, k) * 0.07F));
                for (float yy : new float[]{-3.4F, 3.4F}) {
                    v.box(0, yy - 0.25F, zo, 3.6F, yy + 0.25F, zo + 0.5F, solid(0xD8CBA8));
                    v.box(3.4F, yy - 0.5F, zo, 4.1F, yy + 0.5F, zo + 0.6F, solid(0xC8BA98));
                }
            } else if (variant == 1) {
                for (float yy = -BH + 0.5F; yy < BH; yy += 1.5F) v.box(0.3F, yy, zo, 0.8F, yy + 0.5F, zo + 0.3F, solid(0xB07A4A));
            } else {
                // kabartma yıldız mühür
                float[][] star = new float[10][];
                for (int i = 0; i < 10; i++) {
                    double an = Math.PI * 2 * i / 10 + Math.PI / 2;
                    float rr = i % 2 == 0 ? 1.8F : 0.7F;
                    star[i] = new float[]{BW / 2 + 0.3F + (float) Math.cos(an) * rr, (float) Math.sin(an) * rr};
                }
                v.extrude(star, zo, zo + 0.5F, metal(BRASS_D));
            }
        }
    }

    /** "SPELLBOOK" — 3x5 yazı tipi, etiket uzayında (genişlik ~5.2, yükseklik 2.8). */
    private static boolean label(float x, float y) {
        String[][] font = {{"111", "100", "111", "001", "111"}, {"111", "101", "111", "100", "100"}, {"111", "100", "110", "100", "111"},
                {"100", "100", "100", "100", "111"}, {"100", "100", "100", "100", "111"}, {"110", "101", "110", "101", "110"},
                {"111", "101", "101", "101", "111"}, {"111", "101", "101", "101", "111"}, {"101", "110", "100", "110", "101"}};
        float cw = 0.6F, px = 0.12F;
        int ch = (int) Math.floor((x - 0.3F) / cw);
        if (ch < 0 || ch >= 9) return false;
        float lx = (x - 0.3F - ch * cw) / (cw * 0.85F), ly = (2.3F - y) / 1.8F;
        if (lx < 0 || lx >= 1 || ly < 0 || ly >= 1) return false;
        return font[ch][(int) (ly * 5)].charAt((int) (lx * 3)) == '1';
    }

    public static void book(PoseStack ps, MultiBufferSource buf, int light, int variant, float open, float flip) {
        float ang = open * 78F;
        mesh("bookspine" + variant, v -> {
            int cover = variant == 0 ? 0xC8803A : variant == 1 ? 0x8A4E2C : 0x2C2B32;
            v.fill(-1.0F, -BH - 0.25F, -HALF - TH, 0.4F, BH + 0.25F, HALF + TH, (x, y, z) -> Vox.sq(x / 1.0F) + Vox.sq(z / (HALF + TH)) <= 1 || x >= 0,
                    leather(Vox.scale(0xFF000000 | cover, 0.85F) & 0xFFFFFF), CUT, false);
            if (variant >= 1) {
                int rib = variant == 1 ? 0x8A5AC8 : 0xC8283C;
                v.box(BW * 0.55F, BH - 0.3F, -0.25F, BW * 0.55F + 0.6F, BH + 1.8F, 0.25F, solid(rib));
                if (variant == 2) v.box(BW * 0.72F, BH - 0.3F, -0.25F, BW * 0.72F + 0.5F, BH + 1.3F, 0.25F, solid(0xD8B030));
            }
        }).render(ps, buf, light);
        for (int s = -1; s <= 1; s += 2) {
            final int ss = s;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-s * ang));
            mesh("bookhalf" + variant + s, v -> bookHalf(v, variant, ss)).render(ps, buf, light);
            if (open > 0.05F && variant == 2) {
                float zf = s > 0 ? 0.05F : -0.55F;
                mesh("runes" + s, v -> v.fill(0.6F, -BH + 0.6F, zf, BW - 0.4F, BH - 0.6F, zf + 0.5F,
                        (x, y, z) -> ((int) Math.floor(x * 2) * 7 + (int) Math.floor(y * 2) * 3) % 5 == 0 && ((int) Math.floor(y * 2) % 3 != 2),
                        solid(0xB45AFF), CUT, true)).render(ps, buf, light);
            }
            ps.popPose();
        }
        if (open > 0.1F) {
            Mesh page = mesh("page" + variant, v -> v.box(0.3F, -BH + 0.4F, -0.25F, BW - 0.3F, BH - 0.4F, 0.25F,
                    (x, y, z, i, j, k) -> ((j % 3 == 1) && x > 1 && x < BW - 1 && Vox.hash(i, j, 0) > 0.3F) ? 0xFF8A7E68 : 0xFFF6EFDC));
            for (int i = 0; i < 2; i++) {
                float ph = (flip + i * 0.5F) % 1F;
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(ang - ph * ang * 2));
                page.render(ps, buf, light);
                ps.popPose();
            }
        }
        if (open < 0.05F && variant >= 1) {
            mesh("bookstrap" + variant, v -> {
                int c = variant == 1 ? 0xEDE5D3 : 0x9A5E32;
                for (float yy : variant == 1 ? new float[]{-0.4F} : new float[]{-1.6F, 1.6F}) {
                    float zz = HALF + TH + 0.3F;
                    v.box(-0.6F, yy - 0.6F, -zz, BW + 0.9F, yy + 0.6F, zz, solid(c));
                    v.erase(-0.4F, yy - 0.7F, -zz + 0.5F, BW + 0.4F, yy + 0.7F, zz - 0.5F, (x, y, z) -> true);
                    if (variant == 2) buckle(v, BW - 1.2F, yy, zz - 0.2F, 0.8F, 0.75F, BRASS);
                }
            }).render(ps, buf, light);
        }
    }

    // ================================================================== ANAHTARLAR (X ekseni boyunca, 45° basamaklı)

    private static void bit(Vox v, float x, float len, float depth, int notches, Col c) {
        float[][] poly = new float[4 + notches * 2][];
        int k = 0;
        poly[k++] = new float[]{x, 0.3F};
        poly[k++] = new float[]{x + len, 0.3F};
        poly[k++] = new float[]{x + len, -depth};
        float step = len / (notches + 1);
        for (int i = notches; i >= 1; i--) {
            poly[k++] = new float[]{x + step * i + step * 0.3F, -depth};
            poly[k++] = new float[]{x + step * i, -depth * 0.5F};
        }
        poly[k] = new float[]{x, -depth};
        v.extrude(poly, -0.5F, 0.5F, c);
    }

    private static void shaft(Vox v, float x0, float x1, float r, Col c) {
        v.rod(new float[]{x0, 0, 0}, new float[]{x1, 0, 0}, r, c);
    }

    private static void collar(Vox v, float x, float r, Col c) {
        v.fill(x - 0.5F, -r, -r, x + 0.5F, r, r, (px, y, z) -> y * y + z * z <= r * r, c, CUT, false);
    }

    public static void key(PoseStack ps, MultiBufferSource buf, int light, int kind, Anim a) {
        if (kind == 6) {
            keyRing(ps, buf, light, a);
            return;
        }
        if (kind == 3) {
            // dişli kendi ekseninde döner
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(45));
            ps.translate(-4.6F, 0, 0);
            ps.mulPose(Axis.ZP.rotation(a.time * 0.03F));
            mesh("gear", v -> {
                float[][] gear = new float[32][];
                for (int i = 0; i < 32; i++) {
                    double an = Math.PI * 2 * i / 32;
                    float r = (i / 2) % 2 == 0 ? 3.0F : 2.3F;
                    gear[i] = new float[]{(float) Math.cos(an) * r, (float) Math.sin(an) * r};
                }
                v.extrude(gear, -0.6F, 0.6F, solid(0xD8742C));
                v.fill(-1.1F, -1.1F, -0.9F, 1.1F, 1.1F, 0.9F, (x, y, z) -> x * x + y * y <= 1.2F, metal(IRON_D), CUT, false);
            }).render(ps, buf, light);
            ps.popPose();
        }
        mesh("key" + kind, v -> {
            v.push().rotZ(45);
            switch (kind) {
                case 0 -> {
                    shaft(v, -2.8F, 6.8F, 0.6F, metal(IRON));
                    collar(v, -2.0F, 0.95F, metal(IRON_D));
                    collar(v, 0.6F, 0.9F, metal(IRON_D));
                    v.ringXY(-5.0F, 0, 0, 2.3F, 1.9F, 0.65F, metal(IRON));
                    bit(v, 4.6F, 2.2F, 2.6F, 2, metal(IRON));
                }
                case 1 -> {
                    v.push().rotX(90);
                    v.ringXY(-6.0F, 0, 0, 2.0F, 2.0F, 0.4F, metal(IRON_D));
                    v.pop();
                    for (int i = 0; i < 2; i++) {
                        v.push().translate(-6.0F, 0, 0).rotZ(i == 0 ? 14 : -26).translate(4.4F, 0, i * 1.0F - 0.5F);
                        Col c = metal(i == 0 ? 0x99A2A8 : 0x828A90);
                        shaft(v, -2.2F, 6.2F, 0.55F, c);
                        collar(v, -1.5F, 0.85F, metal(IRON_D));
                        v.ringXY(-3.9F, 0, 0, 1.6F, 1.4F, 0.5F, c);
                        bit(v, 4.2F, 1.9F, 2.1F, i + 1, c);
                        v.pop();
                    }
                }
                case 2 -> {
                    float[][] heart = new float[40][];
                    for (int i = 0; i < 40; i++) {
                        double t = Math.PI * 2 * i / 40;
                        float hx = 16 * (float) Math.pow(Math.sin(t), 3);
                        float hy = (float) (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));
                        heart[i] = new float[]{-4.2F - hy * 0.17F, hx * 0.17F, 0};
                    }
                    float[][] closed = new float[41][];
                    System.arraycopy(heart, 0, closed, 0, 40);
                    closed[40] = heart[0];
                    v.tube(closed, 0.6F, metal(GOLD));
                    shaft(v, -1.6F, 5.4F, 0.5F, metal(GOLD));
                    collar(v, -1.0F, 0.8F, metal(GOLD_D));
                    bit(v, 3.6F, 1.8F, 1.9F, 1, metal(GOLD));
                    v.ell(-4.4F, 0, 0, 0.8F, 0.8F, 0.6F, solid(0xE0304A), CUT, true);
                }
                case 3 -> {
                    shaft(v, -2.0F, 6.0F, 0.55F, metal(IRON));
                    v.box(-0.8F, -0.9F, -0.9F, 0.8F, 0.9F, 0.9F, solid(0xD8742C));
                    v.box(5.4F, -0.35F, -0.35F, 7.2F, 0.35F, 0.35F, metal(IRON_D));
                    v.box(6.6F, -1.3F, -0.4F, 7.2F, 1.3F, 0.4F, metal(IRON));
                    bit(v, 3.8F, 1.4F, 1.6F, 1, metal(IRON));
                }
                case 4 -> {
                    shaft(v, -1.5F, 7.4F, 0.5F, metal(0x5A5C66));
                    for (int i = 0; i < 6; i++) {
                        float x = 0.4F + i * 1.1F, dir = (i % 2 == 0) ? 1 : -1;
                        v.rod(new float[]{x, 0, 0}, new float[]{x + 0.9F, dir * (1.6F - i * 0.1F), (i % 3 - 1) * 0.5F}, 0.3F, metal(0x7C7E8A));
                    }
                    v.rod(new float[]{7.0F, 0, 0}, new float[]{8.8F, 0, 0}, 0.4F, metal(SILVER));
                    v.rod(new float[]{-6.0F, 0, 0}, new float[]{-1.4F, 0, 0}, 0.85F, solid(BONE));
                    v.ell(-6.2F, 0, 0, 1.2F, 1.2F, 1.2F, solid(BONE));
                    v.ell(-1.4F, 0, 0, 1.0F, 1.0F, 1.0F, solid(BONE));
                }
                default -> {
                    shaft(v, -2.4F, 4.0F, 0.45F, metal(SILVER));
                    collar(v, -2.0F, 0.8F, metal(SILVER_D));
                    Col cr = (x, y, z, i, j, k) -> Vox.argb(Vox.lerp(0xFF2E70D8, 0xFFB8E0FF, Vox.hash(i, j, k) * 0.6F + Math.abs(y) * 0.1F), 0.82F);
                    v.push().translate(-4.8F, 0, 0).rotZ(90);
                    v.lathe(new float[][]{{0, -2.4F}, {1.7F, -1.4F}, {1.7F, 1.4F}, {0, 2.4F}}, 1, 1, 8, 0, cr, TRANS, true);
                    v.pop();
                    float[][] tips = {{4.2F, 0, 0, 0, 2.6F}, {4.4F, 0.6F, 0.4F, 35, 1.8F}, {4.3F, -0.6F, -0.3F, -40, 1.6F}, {5.4F, 0.2F, 0, 15, 1.5F}};
                    for (float[] t : tips) {
                        v.push().translate(t[0], t[1], t[2]).rotZ(-90 + t[3]);
                        v.lathe(new float[][]{{0.7F, 0}, {0.7F, t[4]}, {0, t[4] + 1.0F}}, 1, 1, 8, 0, cr, TRANS, true);
                        v.pop();
                    }
                }
            }
            v.pop();
        }).render(ps, buf, light);
    }

    private static void keyRing(PoseStack ps, MultiBufferSource buf, int light, Anim a) {
        mesh("keyring", v -> v.ringXY(0, 4.0F, 0, 3.4F, 3.4F, 0.45F, metal(IRON))).render(ps, buf, light);
        int[] cols = {IRON, GOLD, 0x8A9298, BRASS};
        for (int i = 0; i < 4; i++) {
            double base = Math.toRadians(-125 + i * 23);
            float px = (float) Math.cos(base) * 3.4F, py = 4.0F + (float) Math.sin(base) * 3.4F;
            float sw = (float) Math.sin(a.time * 0.12F + i * 1.3F) * 0.08F + a.tx * (0.9F + i * 0.15F);
            ps.pushPose();
            ps.translate(px, py, (i - 1.5F) * 0.5F);
            ps.mulPose(Axis.ZP.rotation(-1.57F + (i - 1.5F) * 0.22F + sw));
            ps.mulPose(Axis.XP.rotation(a.tz * 0.5F));
            final int c = cols[i], idx = i;
            mesh("ringkey" + i, v -> {
                v.ringXY(1.2F, 0, 0, 1.1F, 0.9F, 0.4F, metal(c));
                shaft(v, 2.2F, 8.0F, 0.4F, metal(c));
                bit(v, 6.3F, 1.5F, 1.3F, 1 + idx % 2, metal(c));
            }).render(ps, buf, light);
            ps.popPose();
        }
    }

    public static void clear() {
        CACHE.clear();
        DYN.clear();
    }
}
