package com.krolasyon.stormray.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Fırtına Vatozu modeli (ileri = +Z, kanatlar ±X). Gövdeden uçlara ilerleyen dalgalı kanat çırpışı,
 * testere dişli arka kenar, öne kıvrık boynuz yüzgeçler, parlayan gözler ve çekirdek, biyolüminesan benekler,
 * S şeklinde sallanan kuyruk ve dört köşeli yıldız uç, altından sarkan titreşen şimşek telleri.
 */
public final class RayModel {
    private RayModel() {}

    private static final int NU = 16, NV = 8;

    /** Kanat yüzeyindeki nokta. s = ±1 kanat tarafı, u açıklık (0 gövde, 1 uç), v kiriş (0 ön, 1 arka). */
    static float[] wing(int s, float u, float v, float flap, boolean top) {
        float lead = 0.62F - 0.86F * (float) Math.pow(u, 1.25);
        float trail = -0.62F + 0.33F * (float) Math.pow(u, 1.6);
        float x = s * (0.02F + 1.68F * u);
        float z = lead + (trail - lead) * v;
        float th = 0.17F * (float) Math.pow(1 - u, 1.8) * (float) Math.pow(Math.sin(Math.PI * v), 0.7) + 0.01F;
        float wave = (float) Math.sin(flap - u * 1.6F);
        float y = 0.42F * wave * (float) Math.pow(u, 1.4)
                + 0.10F * u * (float) Math.sin(flap - u * 1.6F - 1.1F) * (v - 0.3F)
                - 0.08F * u * u;
        y += top ? th : -th * 0.45F;
        return new float[]{x, y, z};
    }

    public static void render(PoseStack ps, MultiBufferSource buf, int light, float time, float flap, boolean saddled, float zap) {
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = buf.getBuffer(Mesh.TYPES[pass]);
            for (int s = -1; s <= 1; s += 2) wingSide(vc, ps, pass, s, flap, time, light, zap);
            if (pass == Mesh.CUT) {
                horns(vc, ps, time, light);
                tail(vc, ps, time, light);
                if (saddled) saddle(vc, ps, light);
            } else {
                glowParts(vc, ps, time, zap);
            }
        }
    }

    private static void wingSide(VertexConsumer vc, PoseStack ps, int pass, int s, float flap, float time, int light, float zap) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float[][][] top = new float[NU + 1][NV + 1][], bot = new float[NU + 1][NV + 1][];
        for (int i = 0; i <= NU; i++) {
            for (int j = 0; j <= NV; j++) {
                float u = i / (float) NU, v = j / (float) NV;
                top[i][j] = wing(s, u, v, flap, true);
                bot[i][j] = wing(s, u, v, flap, false);
            }
        }
        if (pass == Mesh.CUT) {
            TextureAtlasSprite skin = Mesh.sprite(Mesh.SKIN), belly = Mesh.sprite(Mesh.BELLY), rim = Mesh.sprite(Mesh.RIM);
            surface(vc, m, n, top, skin, 1, 1, 1, 1, light, 1, false);
            surface(vc, m, n, bot, belly, 1, 1, 1, 1, light, -1, false);
            // ön ve arka kenar, kanat ucu
            for (int i = 0; i < NU; i++) {
                strip(vc, m, n, top[i][0], top[i + 1][0], bot[i + 1][0], bot[i][0], rim, light);
                strip(vc, m, n, top[i][NV], top[i + 1][NV], bot[i + 1][NV], bot[i][NV], rim, light);
            }
            for (int j = 0; j < NV; j++) strip(vc, m, n, top[NU][j], top[NU][j + 1], bot[NU][j + 1], bot[NU][j], rim, light);
            // testere dişli arka kenar
            Random r = new Random(7 + s);
            for (int i = 2; i < NU - 1; i += 1) {
                float[] a = mid(top[i][NV], bot[i][NV]), b = mid(top[i + 1][NV], bot[i + 1][NV]);
                float len = 0.10F + 0.08F * r.nextFloat() + 0.04F * (1 - i / (float) NU);
                float[] t = {(a[0] + b[0]) / 2 + s * 0.03F, (a[1] + b[1]) / 2 - 0.01F, (a[2] + b[2]) / 2 - len};
                Mesh.face(vc, ps, new float[][]{{a[0], a[1], a[2], 0, 0}, {b[0], b[1], b[2], 1, 0}, {t[0], t[1], t[2], 0.5F, 1}, {t[0], t[1], t[2], 0.5F, 1}},
                        rim, 0.7F, 0.7F, 0.8F, light);
            }
            // ön kenarda uca yakın küçük dikenler
            for (int i = NU - 5; i < NU; i += 2) {
                float[] a = mid(top[i][0], bot[i][0]), b = mid(top[i + 1][0], bot[i + 1][0]);
                float[] t = {(a[0] + b[0]) / 2 + s * 0.07F, (a[1] + b[1]) / 2 + 0.01F, (a[2] + b[2]) / 2 + 0.07F};
                Mesh.face(vc, ps, new float[][]{{a[0], a[1], a[2], 0, 0}, {b[0], b[1], b[2], 1, 0}, {t[0], t[1], t[2], 0.5F, 1}, {t[0], t[1], t[2], 0.5F, 1}},
                        rim, 0.8F, 0.8F, 1, light);
            }
        } else {
            TextureAtlasSprite glow = Mesh.sprite(Mesh.GLOW_TEX);
            float g = Math.min(1, 0.75F + 0.25F * (float) Math.sin(time * 0.15) + zap * 0.5F);
            surface(vc, m, n, top, glow, g, g, g, 1, Mesh.FULL_BRIGHT, 1, false);
            surface(vc, m, n, top, glow, g, g, g, 1, Mesh.FULL_BRIGHT, 1, true);
        }
    }

    private static float[] mid(float[] a, float[] b) {
        return new float[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
    }

    private static void surface(VertexConsumer vc, Matrix4f m, Matrix3f n, float[][][] g, TextureAtlasSprite s,
                                float cr, float cg, float cb, float ca, int light, float facing, boolean flip) {
        int nu = g.length - 1, nv = g[0].length - 1;
        for (int i = 0; i < nu; i++) {
            for (int j = 0; j < nv; j++) {
                int[][] idx = flip ? new int[][]{{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}} : new int[][]{{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] k : idx) {
                    float[] p = g[k[0]][k[1]];
                    Vector3f nn = normalAt(g, k[0], k[1], facing);
                    Mesh.v(vc, m, n, p[0], p[1], p[2], cr, cg, cb, ca,
                            Mesh.su(s, k[0] / (float) nu), Mesh.sv(s, k[1] / (float) nv), light, nn.x, nn.y, nn.z);
                }
            }
        }
    }

    private static Vector3f normalAt(float[][][] g, int i, int j, float facing) {
        int nu = g.length - 1, nv = g[0].length - 1;
        float[] a = g[Math.min(nu, i + 1)][j], b = g[Math.max(0, i - 1)][j];
        float[] c = g[i][Math.min(nv, j + 1)], d = g[i][Math.max(0, j - 1)];
        Vector3f du = new Vector3f(a[0] - b[0], a[1] - b[1], a[2] - b[2]);
        Vector3f dv = new Vector3f(c[0] - d[0], c[1] - d[1], c[2] - d[2]);
        Vector3f nn = du.cross(dv);
        if (nn.lengthSquared() < 1e-12F) return new Vector3f(0, facing, 0);
        nn.normalize();
        if (nn.y * facing < 0) nn.mul(-1);
        return nn;
    }

    private static void strip(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] a, float[] b, float[] c, float[] d, TextureAtlasSprite s, int light) {
        Vector3f e1 = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        Vector3f e2 = new Vector3f(d[0] - a[0], d[1] - a[1], d[2] - a[2]);
        Vector3f nn = e1.cross(e2);
        if (nn.lengthSquared() < 1e-12F) nn.set(0, 0, 1);
        nn.normalize();
        float[][] p = {a, b, c, d};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int k = 0; k < 4; k++) {
            Mesh.v(vc, m, n, p[k][0], p[k][1], p[k][2], 1, 1, 1, 1, Mesh.su(s, uv[k][0]), Mesh.sv(s, uv[k][1]), light, nn.x, nn.y, nn.z);
        }
    }

    // ------------------------------------------------------------------ baş, kuyruk, eyer

    private static void horns(VertexConsumer vc, PoseStack ps, float time, int light) {
        TextureAtlasSprite skin = Mesh.sprite(Mesh.SKIN);
        float curl = 0.02F * (float) Math.sin(time * 0.07);
        for (int s = -1; s <= 1; s += 2) {
            Mesh.tube(vc, ps, Mesh.spline(new float[][]{
                    {s * 0.17F, 0.10F, 0.48F, 0.055F}, {s * 0.21F, 0.17F, 0.66F, 0.045F}, {s * 0.17F, 0.28F + curl, 0.79F, 0.03F},
                    {s * 0.09F, 0.36F + curl, 0.77F, 0.016F}, {s * 0.05F, 0.33F + curl, 0.70F, 0.006F}}, 4), 7, skin, 0.85F, 0.9F, 1, 1, light, 1);
            // göz çukurları
            Mesh.sphere(vc, ps, s * 0.16F, 0.15F, 0.40F, 0.05F, 0.035F, 0.05F, 5, 8, skin, 0.4F, 0.45F, 0.5F, 1, light);
        }
    }

    private static float[][] tailPoints(float time) {
        int n = 22;
        float[][] pts = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float x = 0.45F * t * (float) Math.sin(t * 3.0 + time * 0.1) + 0.25F * t * t;
            float y = -0.02F - 0.35F * t * t + 0.12F * (float) Math.sin(t * Math.PI) + 0.05F * t * (float) Math.sin(time * 0.13 + t * 4);
            float z = -0.56F - 2.4F * t;
            float r = 0.075F * (float) Math.pow(1 - t, 1.2) + 0.012F;
            pts[i] = new float[]{x, y, z, r};
        }
        return pts;
    }

    private static void tail(VertexConsumer vc, PoseStack ps, float time, int light) {
        TextureAtlasSprite skin = Mesh.sprite(Mesh.SKIN), rim = Mesh.sprite(Mesh.RIM);
        float[][] pts = tailPoints(time);
        Mesh.tube(vc, ps, pts, 7, skin, 1, 1, 1, 1, light, 1);
        // dört köşeli yıldız uç
        float[] e = pts[pts.length - 1], p = pts[pts.length - 3];
        Vector3f d = new Vector3f(e[0] - p[0], e[1] - p[1], e[2] - p[2]).normalize();
        Vector3f sd = new Vector3f(d).cross(new Vector3f(0, 1, 0)).normalize();
        Vector3f[] dirs = {d, new Vector3f(d).mul(-1), sd, new Vector3f(sd).mul(-1)};
        float[] lens = {0.34F, 0.18F, 0.28F, 0.28F};
        float cx = e[0] + d.x * 0.12F, cy = e[1] + d.y * 0.12F, cz = e[2] + d.z * 0.12F;
        for (int k = 0; k < 4; k++) {
            Vector3f dir = dirs[k];
            Vector3f perp = k < 2 ? sd : d;
            float in = 0.06F;
            float[][] q = {
                    {cx + perp.x * in, cy + perp.y * in, cz + perp.z * in, 0, 0},
                    {cx + dir.x * lens[k], cy + dir.y * lens[k], cz + dir.z * lens[k], 0.5F, 1},
                    {cx - perp.x * in, cy - perp.y * in, cz - perp.z * in, 1, 0},
                    {cx, cy + 0.02F, cz, 0.5F, 0.2F}};
            Mesh.face(vc, ps, q, rim, 0.55F, 0.6F, 0.75F, light);
        }
    }

    private static void saddle(VertexConsumer vc, PoseStack ps, int light) {
        TextureAtlasSprite s = Mesh.sprite(Mesh.SADDLE);
        Mesh.beam(vc, ps, new float[]{0, 0.20F, -0.14F}, new float[]{0, 0.20F, 0.22F}, 0.17F, 0.035F, 0.17F, 0.035F, s, 1, 1, 1, light);
        Mesh.beam(vc, ps, new float[]{0, 0.21F, 0.18F}, new float[]{0, 0.31F, 0.21F}, 0.04F, 0.03F, 0.03F, 0.02F, s, 1, 1, 1, light);
        Mesh.beam(vc, ps, new float[]{0, 0.21F, -0.13F}, new float[]{0, 0.27F, -0.16F}, 0.12F, 0.02F, 0.12F, 0.02F, s, 1, 1, 1, light);
    }

    // ------------------------------------------------------------------ parlayan parçalar

    private static void glowParts(VertexConsumer vc, PoseStack ps, float time, float zap) {
        TextureAtlasSprite core = Mesh.sprite(Mesh.CORE), eye = Mesh.sprite(Mesh.EYE), bolt = Mesh.sprite(Mesh.BOLT), glow = Mesh.sprite(Mesh.GLOW_TEX);
        float pulse = 0.7F + 0.3F * (float) Math.sin(time * 0.2) + zap * 0.4F;
        float c = Math.min(1, pulse);
        Mesh.sphere(vc, ps, 0, 0.165F, 0.08F, 0.16F, 0.05F, 0.21F, 6, 12, core, c, c, c, 1, Mesh.FULL_BRIGHT);
        for (int s = -1; s <= 1; s += 2) {
            Mesh.sphere(vc, ps, s * 0.165F, 0.165F, 0.41F, 0.03F, 0.022F, 0.03F, 5, 8, eye, 1, 1, 1, 1, Mesh.FULL_BRIGHT);
        }
        // kuyruk boyunca ince parıltı
        Mesh.tube(vc, ps, tailPoints(time), 7, glow, 0.8F, 0.8F, 0.8F, 1, Mesh.FULL_BRIGHT, 1.04F, false);
        Mesh.tube(vc, ps, tailPoints(time), 7, glow, 0.8F, 0.8F, 0.8F, 1, Mesh.FULL_BRIGHT, 1.04F, true);
        // sarkan şimşek telleri (her ~2.5 tikte bir yeniden çakar)
        float[][] starts = {{0.30F, -0.07F, 0.05F}, {-0.22F, -0.07F, -0.08F}, {0.06F, -0.06F, -0.32F}, {-0.08F, -0.07F, 0.25F}};
        int seed = (int) (time / 2.5F);
        float len = 1.1F + 0.7F * zap;
        for (int k = 0; k < starts.length; k++) {
            Random r = new Random(seed * 131L + k * 977L);
            float flick = 0.55F + 0.45F * r.nextFloat() + zap * 0.4F;
            float[] p = starts[k].clone();
            int segs = 9;
            float sl = len * (0.75F + 0.4F * ((k * 37) % 10) / 10F) / segs;
            for (int i = 0; i < segs; i++) {
                float[] q = {p[0] + (r.nextFloat() - 0.5F) * 0.16F + 0.02F, p[1] - sl, p[2] + (r.nextFloat() - 0.5F) * 0.16F - 0.03F};
                float w = 0.008F * (1 - i / (float) segs) + 0.003F;
                float b = Math.min(1, flick);
                Mesh.beam(vc, ps, p, q, w, w, w, w, bolt, b * 0.75F, b, b, Mesh.FULL_BRIGHT);
                p = q;
            }
        }
    }
}
