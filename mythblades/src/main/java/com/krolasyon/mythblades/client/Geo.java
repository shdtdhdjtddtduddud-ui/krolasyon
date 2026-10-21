package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.MythBlades;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Pürüzsüz, dokulu yüzeyler: parametrik ızgara (bıçak yüzleri, döndürme gövdeleri, elipsoitler) ve paralel taşımalı
 * boru (kökler, kıvrımlar). Normaller sonlu farklarla hesaplanır; birim piksel.
 */
public final class Geo {
    private Geo() {}

    public static final int FULL = 0xF000F0;
    public static final RenderType CUT = RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS);
    public static final RenderType GLOW = RenderType.eyes(InventoryMenu.BLOCK_ATLAS);
    public static final RenderType TRANS = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);

    public static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(MythBlades.MODID, "block/" + name));
    }

    /** Yüzey: (t, u) ∈ [0,1]² → konum. */
    public interface Surf {
        void at(float t, float u, float[] out);
    }

    /** Köşe rengi (r, g, b, a çarpanı); null = beyaz. */
    public interface Tint {
        void at(float t, float u, float[] rgba);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] p, float[] nr, float[] c, float u, float v, int light) {
        vc.vertex(m, p[0], p[1], p[2]).color(c[0], c[1], c[2], c[3]).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(n, nr[0], nr[1], nr[2]).endVertex();
    }

    /**
     * Izgarayı çizer. side = +1/-1 normal yönünü belirler (ön/arka yüz); uMin..uMax ve vMin..vMax doku penceresi
     * (sprite içinde 0..1).
     */
    public static void grid(VertexConsumer vc, PoseStack ps, int nt, int nu, Surf f, TextureAtlasSprite s, Tint tint, int light, float side,
                            float u0, float u1, float v0, float v1) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float[][][] P = new float[nt + 1][nu + 1][3];
        float[][][] N = new float[nt + 1][nu + 1][3];
        for (int i = 0; i <= nt; i++) for (int j = 0; j <= nu; j++) f.at(i / (float) nt, j / (float) nu, P[i][j]);
        float[] a = new float[3], b = new float[3];
        for (int i = 0; i <= nt; i++) {
            for (int j = 0; j <= nu; j++) {
                int i0 = Math.max(0, i - 1), i1 = Math.min(nt, i + 1), j0 = Math.max(0, j - 1), j1 = Math.min(nu, j + 1);
                for (int d = 0; d < 3; d++) {
                    a[d] = P[i1][j][d] - P[i0][j][d];
                    b[d] = P[i][j1][d] - P[i][j0][d];
                }
                Vector3f c = new Vector3f(b[0], b[1], b[2]).cross(a[0], a[1], a[2]);
                if (c.lengthSquared() < 1e-12F) c.set(0, 0, 1);
                c.normalize().mul(side);
                N[i][j][0] = c.x;
                N[i][j][1] = c.y;
                N[i][j][2] = c.z;
            }
        }
        float[] col = {1, 1, 1, 1};
        float[][] cols = new float[4][];
        for (int i = 0; i < nt; i++) {
            for (int j = 0; j < nu; j++) {
                int[][] q = side > 0 ? new int[][]{{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}} : new int[][]{{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int k = 0; k < 4; k++) {
                    int ii = q[k][0], jj = q[k][1];
                    float t = ii / (float) nt, u = jj / (float) nu;
                    float[] cc = col.clone();
                    if (tint != null) tint.at(t, u, cc);
                    cols[k] = cc;
                    float su = s.getU0() + (s.getU1() - s.getU0()) * (u0 + (u1 - u0) * u);
                    float sv = s.getV0() + (s.getV1() - s.getV0()) * (v1 - (v1 - v0) * t);
                    vert(vc, m, n, P[ii][jj], N[ii][jj], cc, su, sv, light);
                }
            }
        }
    }

    public static void grid(VertexConsumer vc, PoseStack ps, int nt, int nu, Surf f, TextureAtlasSprite s, Tint tint, int light, float side) {
        grid(vc, ps, nt, nu, f, s, tint, light, side, 0, 1, 0, 1);
    }

    /** Kalınlık profili: t boyunca, u enine; ±z simetrik iki yüz olarak çizilir. */
    public interface Blade {
        /** out = {x, y, yarıKalınlık} */
        void at(float t, float u, float[] out);
    }

    public static void blade(VertexConsumer vc, PoseStack ps, int nt, int nu, Blade b, TextureAtlasSprite s, Tint tint, int light, float inflate) {
        float[] o = new float[3];
        for (int side = -1; side <= 1; side += 2) {
            final int sd = side;
            grid(vc, ps, nt, nu, (t, u, out) -> {
                b.at(t, u, o);
                out[0] = o[0];
                out[1] = o[1];
                out[2] = sd * (o[2] + inflate);
            }, s, tint, light, side);
        }
    }

    /** Döndürme gövdesi: profil {r, y} noktaları (Y ekseni etrafında). */
    public static void lathe(VertexConsumer vc, PoseStack ps, float[][] prof, int seg, TextureAtlasSprite s, Tint tint, int light) {
        int np = prof.length;
        grid(vc, ps, np - 1, seg, (t, u, out) -> {
            float f = t * (np - 1);
            int i = Math.min(np - 2, (int) f);
            float k = f - i;
            float r = prof[i][0] + (prof[i + 1][0] - prof[i][0]) * k;
            float y = prof[i][1] + (prof[i + 1][1] - prof[i][1]) * k;
            double a = u * Math.PI * 2;
            out[0] = (float) Math.cos(a) * r;
            out[1] = y;
            out[2] = (float) -Math.sin(a) * r;
        }, s, tint, light, prof[np - 1][1] > prof[0][1] ? 1 : -1);
    }

    public static void ellipsoid(VertexConsumer vc, PoseStack ps, float cx, float cy, float cz, float rx, float ry, float rz, int nt, int nu,
                                 TextureAtlasSprite s, Tint tint, int light) {
        grid(vc, ps, nt, nu, (t, u, out) -> {
            double lat = -Math.PI / 2 + t * Math.PI, lon = u * Math.PI * 2;
            out[0] = cx + (float) (Math.cos(lat) * Math.cos(lon)) * rx;
            out[1] = cy + (float) Math.sin(lat) * ry;
            out[2] = cz + (float) (Math.cos(lat) * -Math.sin(lon)) * rz;
        }, s, tint, light, 1);
    }

    /** Catmull-Rom (x, y, z, r). */
    public static float[][] spline(float[][] c, int per) {
        int n = c.length;
        float[][] out = new float[(n - 1) * per + 1][];
        int k = 0;
        for (int i = 0; i < n - 1; i++) {
            float[] p0 = c[Math.max(0, i - 1)], p1 = c[i], p2 = c[i + 1], p3 = c[Math.min(n - 1, i + 2)];
            for (int j = 0; j < per; j++) {
                float t = j / (float) per, t2 = t * t, t3 = t2 * t;
                float[] p = new float[4];
                for (int d = 0; d < 4; d++) {
                    p[d] = 0.5F * ((2 * p1[d]) + (-p0[d] + p2[d]) * t + (2 * p0[d] - 5 * p1[d] + 4 * p2[d] - p3[d]) * t2
                            + (-p0[d] + 3 * p1[d] - 3 * p2[d] + p3[d]) * t3);
                }
                out[k++] = p;
            }
        }
        out[k] = c[n - 1].clone();
        return out;
    }

    /** Noktalar (x, y, z, r) boyunca pürüzsüz boru; doku sarmal biçimde uzunluk boyunca tekrar eder. */
    public static void tube(VertexConsumer vc, PoseStack ps, float[][] pts, int seg, TextureAtlasSprite s, float[] rgba, int light) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int np = pts.length;
        if (np < 2) return;
        Vector3f[] T = new Vector3f[np];
        for (int i = 0; i < np; i++) {
            float[] a = pts[Math.max(0, i - 1)], b = pts[Math.min(np - 1, i + 1)];
            T[i] = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
            if (T[i].lengthSquared() < 1e-12F) T[i].set(0, 1, 0);
            T[i].normalize();
        }
        Vector3f N = Math.abs(T[0].y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        N.sub(new Vector3f(T[0]).mul(N.dot(T[0]))).normalize();
        Vector3f[] Ns = new Vector3f[np], Bs = new Vector3f[np];
        float[] len = new float[np];
        for (int i = 0; i < np; i++) {
            if (i > 0) {
                N.sub(new Vector3f(T[i]).mul(N.dot(T[i])));
                if (N.lengthSquared() < 1e-10F) N.set(Math.abs(T[i].y) < 0.9F ? 0 : 1, Math.abs(T[i].y) < 0.9F ? 1 : 0, 0);
                N.normalize();
                float dx = pts[i][0] - pts[i - 1][0], dy = pts[i][1] - pts[i - 1][1], dz = pts[i][2] - pts[i - 1][2];
                len[i] = len[i - 1] + (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            Ns[i] = new Vector3f(N);
            Bs[i] = new Vector3f(T[i]).cross(N).normalize();
        }
        for (int i = 0; i < np - 1; i++) {
            float va = tri(len[i] / 6F), vb = tri(len[i + 1] / 6F);
            for (int k = 0; k < seg; k++) {
                float a0 = (float) (Math.PI * 2 * k / seg), a1 = (float) (Math.PI * 2 * (k + 1) / seg);
                float ua = k / (float) seg, ub = (k + 1) / (float) seg;
                ring(vc, m, n, pts[i], Ns[i], Bs[i], a0, s, ua, va, rgba, light);
                ring(vc, m, n, pts[i], Ns[i], Bs[i], a1, s, ub, va, rgba, light);
                ring(vc, m, n, pts[i + 1], Ns[i + 1], Bs[i + 1], a1, s, ub, vb, rgba, light);
                ring(vc, m, n, pts[i + 1], Ns[i + 1], Bs[i + 1], a0, s, ua, vb, rgba, light);
            }
        }
    }

    private static float tri(float x) {
        float f = x - (float) Math.floor(x);
        return f < 0.5F ? f * 2 : 2 - f * 2;
    }

    private static void ring(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] p, Vector3f N, Vector3f B, float a, TextureAtlasSprite s,
                             float u, float v, float[] c, int light) {
        float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
        float dx = N.x * cs + B.x * sn, dy = N.y * cs + B.y * sn, dz = N.z * cs + B.z * sn;
        float r = p[3];
        vc.vertex(m, p[0] + dx * r, p[1] + dy * r, p[2] + dz * r).color(c[0], c[1], c[2], c[3])
                .uv(s.getU0() + (s.getU1() - s.getU0()) * u, s.getV0() + (s.getV1() - s.getV0()) * v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, dx, dy, dz).endVertex();
    }

    /** Tüp yolunun yarıçapını uca doğru incelt (r0 → r1). */
    public static float[][] taper(float[][] pts, float r0, float r1) {
        float[][] o = new float[pts.length][];
        for (int i = 0; i < pts.length; i++) {
            float s = i / (float) (pts.length - 1);
            o[i] = new float[]{pts[i][0], pts[i][1], pts[i][2], r0 + (r1 - r0) * s};
        }
        return o;
    }
}
