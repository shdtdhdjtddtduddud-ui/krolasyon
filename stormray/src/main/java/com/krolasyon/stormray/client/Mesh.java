package com.krolasyon.stormray.client;

import com.krolasyon.stormray.StormRay;
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

/** Procedural mesh helpers: twisted root tubes and faceted crystal lofts. */
public final class Mesh {
    private Mesh() {}

    public static final int FULL_BRIGHT = 0xF000F0;
    public static final int CUT = 0, GLOW = 1, TRANS = 2;
    public static final RenderType[] TYPES = {
            RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS),
            RenderType.eyes(InventoryMenu.BLOCK_ATLAS),
            RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)};

    public static final ResourceLocation SKIN = tex("ray_skin");
    public static final ResourceLocation BELLY = tex("ray_belly");
    public static final ResourceLocation RIM = tex("ray_rim");
    public static final ResourceLocation GLOW_TEX = tex("ray_glow");
    public static final ResourceLocation CORE = tex("ray_core");
    public static final ResourceLocation BOLT = tex("ray_bolt");
    public static final ResourceLocation EYE = tex("ray_eye");
    public static final ResourceLocation SADDLE = tex("ray_saddle");

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(StormRay.MODID, "block/" + n);
    }

    public static TextureAtlasSprite sprite(ResourceLocation rl) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(rl);
    }

    static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float r, float g, float b, float a,
                  float u, float vv, int light, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    static float su(TextureAtlasSprite s, float t) { return s.getU0() + (s.getU1() - s.getU0()) * t; }
    static float sv(TextureAtlasSprite s, float t) { return s.getV0() + (s.getV1() - s.getV0()) * t; }

    /** Catmull-Rom interpolation of control points (x, y, z, radius). */
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

    /** Smooth tube along points (x, y, z, radius) with parallel-transport frames; bark-like UVs. */
    public static void tube(VertexConsumer vc, PoseStack ps, float[][] pts, int seg, TextureAtlasSprite s,
                            float cr, float cg, float cb, float ca, int light, float inflate) {
        tube(vc, ps, pts, seg, s, cr, cg, cb, ca, light, inflate, false);
    }

    /** {@code flip} reverses the winding (used to draw both sides with culled render types). */
    public static void tube(VertexConsumer vc, PoseStack ps, float[][] pts, int seg, TextureAtlasSprite s,
                            float cr, float cg, float cb, float ca, int light, float inflate, boolean flip) {
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
        for (int i = 0; i < np; i++) {
            if (i > 0) {
                N.sub(new Vector3f(T[i]).mul(N.dot(T[i])));
                if (N.lengthSquared() < 1e-10F) N.set(Math.abs(T[i].y) < 0.9F ? 0 : 1, Math.abs(T[i].y) < 0.9F ? 1 : 0, 0);
                N.normalize();
            }
            Ns[i] = new Vector3f(N);
            Bs[i] = new Vector3f(T[i]).cross(N).normalize();
        }
        for (int i = 0; i < np - 1; i++) {
            float v0 = tri(i / 3F), v1 = tri((i + 1) / 3F);
            for (int k = 0; k < seg; k++) {
                float a0 = (float) (Math.PI * 2 * k / seg), a1 = (float) (Math.PI * 2 * (k + 1) / seg);
                float u0 = k / (float) seg, u1 = (k + 1) / (float) seg;
                if (flip) {
                    float t = a0; a0 = a1; a1 = t;
                    t = u0; u0 = u1; u1 = t;
                }
                emitRing(vc, m, n, pts[i], Ns[i], Bs[i], a0, inflate, s, u0, v0, cr, cg, cb, ca, light);
                emitRing(vc, m, n, pts[i + 1], Ns[i + 1], Bs[i + 1], a0, inflate, s, u0, v1, cr, cg, cb, ca, light);
                emitRing(vc, m, n, pts[i + 1], Ns[i + 1], Bs[i + 1], a1, inflate, s, u1, v1, cr, cg, cb, ca, light);
                emitRing(vc, m, n, pts[i], Ns[i], Bs[i], a1, inflate, s, u1, v0, cr, cg, cb, ca, light);
            }
        }
    }

    private static float tri(float x) {
        float f = x - (float) Math.floor(x);
        return f < 0.5F ? f * 2 : 2 - f * 2;
    }

    private static void emitRing(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] p, Vector3f N, Vector3f B, float a, float inflate,
                                 TextureAtlasSprite s, float u, float vv, float cr, float cg, float cb, float ca, int light) {
        float c = (float) Math.cos(a), sn = (float) Math.sin(a);
        float dx = N.x * c + B.x * sn, dy = N.y * c + B.y * sn, dz = N.z * c + B.z * sn;
        float r = p[3] * inflate;
        v(vc, m, n, p[0] + dx * r, p[1] + dy * r, p[2] + dz * r, cr, cg, cb, ca, su(s, u), sv(s, vv), light, dx, dy, dz);
    }

    /**
     * Flat-shaded crystal loft along +Y. {@code shape} is the unit cross section (x, z); each ring is
     * (y, scaleX, scaleZ, offsetX, offsetZ, brightness). Ends are capped.
     */
    public static void loft(VertexConsumer vc, PoseStack ps, float[][] shape, float[][] rings, TextureAtlasSprite s,
                            float cr, float cg, float cb, float ca, int light, boolean caps) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int ns = shape.length, nr = rings.length;
        float y0 = rings[0][0], y1 = rings[nr - 1][0];
        float span = Math.max(1e-4F, y1 - y0);
        float[][][] P = new float[nr][ns][3];
        for (int i = 0; i < nr; i++) {
            for (int k = 0; k < ns; k++) {
                P[i][k][0] = shape[k][0] * rings[i][1] + rings[i][3];
                P[i][k][1] = rings[i][0];
                P[i][k][2] = shape[k][1] * rings[i][2] + rings[i][4];
            }
        }
        for (int i = 0; i < nr - 1; i++) {
            float br0 = rings[i][5], br1 = rings[i + 1][5];
            float v0 = (rings[i][0] - y0) / span, v1 = (rings[i + 1][0] - y0) / span;
            float cx = (rings[i][3] + rings[i + 1][3]) * 0.5F, cz = (rings[i][4] + rings[i + 1][4]) * 0.5F;
            for (int k = 0; k < ns; k++) {
                int k1 = (k + 1) % ns;
                float[] A = P[i][k], B = P[i][k1], C = P[i + 1][k1], D = P[i + 1][k];
                Vector3f nn = faceNormal(A, B, D, C);
                float mx = (A[0] + B[0] + C[0] + D[0]) / 4 - cx, mz = (A[2] + B[2] + C[2] + D[2]) / 4 - cz;
                if (nn.x * mx + nn.z * mz < 0) nn.mul(-1);
                float u0 = k / (float) ns, u1 = (k + 1) / (float) ns;
                v(vc, m, n, A[0], A[1], A[2], cr * br0, cg * br0, cb * br0, ca, su(s, u0), sv(s, 1 - v0), light, nn.x, nn.y, nn.z);
                v(vc, m, n, B[0], B[1], B[2], cr * br0, cg * br0, cb * br0, ca, su(s, u1), sv(s, 1 - v0), light, nn.x, nn.y, nn.z);
                v(vc, m, n, C[0], C[1], C[2], cr * br1, cg * br1, cb * br1, ca, su(s, u1), sv(s, 1 - v1), light, nn.x, nn.y, nn.z);
                v(vc, m, n, D[0], D[1], D[2], cr * br1, cg * br1, cb * br1, ca, su(s, u0), sv(s, 1 - v1), light, nn.x, nn.y, nn.z);
            }
        }
        if (caps) {
            cap(vc, m, n, P[0], rings[0], s, cr, cg, cb, ca, light, -1);
            cap(vc, m, n, P[nr - 1], rings[nr - 1], s, cr, cg, cb, ca, light, 1);
        }
    }

    private static Vector3f faceNormal(float[] a, float[] b, float[] d, float[] c) {
        Vector3f e1 = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        Vector3f e2 = new Vector3f(d[0] - a[0], d[1] - a[1], d[2] - a[2]);
        if (e1.lengthSquared() < 1e-12F) e1.set(c[0] - d[0], c[1] - d[1], c[2] - d[2]);
        if (e2.lengthSquared() < 1e-12F) e2.set(c[0] - b[0], c[1] - b[1], c[2] - b[2]);
        Vector3f nn = e1.cross(e2);
        if (nn.lengthSquared() < 1e-12F) return new Vector3f(0, 1, 0);
        return nn.normalize();
    }

    private static void cap(VertexConsumer vc, Matrix4f m, Matrix3f n, float[][] ring, float[] r, TextureAtlasSprite s,
                            float cr, float cg, float cb, float ca, int light, float ny) {
        if (r[1] < 1e-4F && r[2] < 1e-4F) return;
        float b = r[5];
        for (int k = 0; k < ring.length; k++) {
            float[] p0 = ring[k], p1 = ring[(k + 1) % ring.length];
            v(vc, m, n, r[3], r[0], r[4], cr * b, cg * b, cb * b, ca, su(s, 0.5F), sv(s, 0.5F), light, 0, ny, 0);
            v(vc, m, n, p0[0], p0[1], p0[2], cr * b, cg * b, cb * b, ca, su(s, 0.1F), sv(s, 0.1F), light, 0, ny, 0);
            v(vc, m, n, p1[0], p1[1], p1[2], cr * b, cg * b, cb * b, ca, su(s, 0.9F), sv(s, 0.1F), light, 0, ny, 0);
            v(vc, m, n, p1[0], p1[1], p1[2], cr * b, cg * b, cb * b, ca, su(s, 0.9F), sv(s, 0.1F), light, 0, ny, 0);
        }
    }

    public static float[][] reversed(float[][] shape) {
        float[][] r = new float[shape.length][];
        for (int i = 0; i < shape.length; i++) r[i] = shape[shape.length - 1 - i];
        return r;
    }

    /** Elongated hexagon cross-section (blade). */
    public static final float[][] HEX_BLADE = {{1, 0}, {0.55F, 1}, {-0.55F, 1}, {-1, 0}, {-0.55F, -1}, {0.55F, -1}};
    /** Regular hexagon cross-section (spikes, pommel). */
    public static final float[][] HEX = new float[6][];

    static {
        for (int k = 0; k < 6; k++) HEX[k] = new float[]{(float) Math.cos(Math.PI / 3 * k), (float) Math.sin(Math.PI / 3 * k)};
    }

    /**
     * Rectangular beam from a to b. {@code up} is the axis the width runs along (usually Z, out of the bow plane);
     * half sizes are given at both ends so beams can taper to blade-like spikes.
     */
    public static void beam(VertexConsumer vc, PoseStack ps, float[] a, float[] b, float w0, float t0, float w1, float t1,
                            TextureAtlasSprite s, float cr, float cg, float cb, int light) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        Vector3f d = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        if (d.lengthSquared() < 1e-10F) return;
        d.normalize();
        Vector3f up = Math.abs(d.z) < 0.9F ? new Vector3f(0, 0, 1) : new Vector3f(1, 0, 0);
        Vector3f side = new Vector3f(d).cross(up).normalize();
        up = new Vector3f(side).cross(d).normalize();
        float[][] corners = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
        float[][] A = new float[4][3], B = new float[4][3];
        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < 3; i++) {
                float sv = i == 0 ? side.x : i == 1 ? side.y : side.z;
                float uv = i == 0 ? up.x : i == 1 ? up.y : up.z;
                A[k][i] = a[i] + sv * t0 * corners[k][0] + uv * w0 * corners[k][1];
                B[k][i] = b[i] + sv * t1 * corners[k][0] + uv * w1 * corners[k][1];
            }
        }
        for (int k = 0; k < 4; k++) {
            int k1 = (k + 1) % 4;
            Vector3f e1 = new Vector3f(A[k1][0] - A[k][0], A[k1][1] - A[k][1], A[k1][2] - A[k][2]);
            Vector3f e2 = new Vector3f(B[k][0] - A[k][0], B[k][1] - A[k][1], B[k][2] - A[k][2]);
            Vector3f nn = e2.cross(e1);
            if (nn.lengthSquared() < 1e-12F) nn.set(up);
            nn.normalize();
            float u0 = k / 4F, u1 = (k + 1) / 4F;
            v(vc, m, n, A[k][0], A[k][1], A[k][2], cr, cg, cb, 1, su(s, u0), sv(s, 1), light, nn.x, nn.y, nn.z);
            v(vc, m, n, A[k1][0], A[k1][1], A[k1][2], cr, cg, cb, 1, su(s, u1), sv(s, 1), light, nn.x, nn.y, nn.z);
            v(vc, m, n, B[k1][0], B[k1][1], B[k1][2], cr, cg, cb, 1, su(s, u1), sv(s, 0), light, nn.x, nn.y, nn.z);
            v(vc, m, n, B[k][0], B[k][1], B[k][2], cr, cg, cb, 1, su(s, u0), sv(s, 0), light, nn.x, nn.y, nn.z);
        }
        for (int end = 0; end < 2; end++) {
            float[][] C = end == 0 ? A : B;
            float sg = end == 0 ? -1 : 1;
            for (int k : new int[]{0, 1, 2, 3}) {
                float[] p = C[k];
                v(vc, m, n, p[0], p[1], p[2], cr, cg, cb, 1, su(s, k == 0 || k == 3 ? 0.1F : 0.9F), sv(s, k < 2 ? 0.1F : 0.9F), light, d.x * sg, d.y * sg, d.z * sg);
            }
        }
    }

    /** Flat quad given 4 corners (x, y, z, u, v); used for feathers and plates (cutout, no cull). */
    public static void face(VertexConsumer vc, PoseStack ps, float[][] c, TextureAtlasSprite s, float cr, float cg, float cb, int light) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        Vector3f e1 = new Vector3f(c[1][0] - c[0][0], c[1][1] - c[0][1], c[1][2] - c[0][2]);
        Vector3f e2 = new Vector3f(c[3][0] - c[0][0], c[3][1] - c[0][1], c[3][2] - c[0][2]);
        Vector3f nn = e1.cross(e2);
        if (nn.lengthSquared() < 1e-12F) nn.set(0, 0, 1);
        nn.normalize();
        for (float[] p : c) v(vc, m, n, p[0], p[1], p[2], cr, cg, cb, 1, su(s, p[3]), sv(s, p[4]), light, nn.x, nn.y, nn.z);
    }

    /** Band around a vertical cylinder of radius r, centred at height y, tilted around X by {@code tilt} radians. */
    public static void band(VertexConsumer vc, PoseStack ps, float r, float y, float tilt, float h, int seg, TextureAtlasSprite s, int light) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float tt = (float) Math.tan(tilt);
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float y0 = y + tt * r * s0, y1 = y + tt * r * s1;
            float u0 = (i % 4) / 4F, u1 = (i % 4 + 1) / 4F;
            v(vc, m, n, c0 * r, y0 - h / 2, s0 * r, 1, 1, 1, 1, su(s, u0), sv(s, 1), light, c0, 0, s0);
            v(vc, m, n, c1 * r, y1 - h / 2, s1 * r, 1, 1, 1, 1, su(s, u1), sv(s, 1), light, c1, 0, s1);
            v(vc, m, n, c1 * r, y1 + h / 2, s1 * r, 1, 1, 1, 1, su(s, u1), sv(s, 0), light, c1, 0, s1);
            v(vc, m, n, c0 * r, y0 + h / 2, s0 * r, 1, 1, 1, 1, su(s, u0), sv(s, 0), light, c0, 0, s0);
        }
    }

    /** UV sphere (ellipsoid with radii rx, ry, rz) centred at (cx, cy, cz). */
    public static void sphere(VertexConsumer vc, PoseStack ps, float cx, float cy, float cz, float rx, float ry, float rz,
                              int lat, int lon, TextureAtlasSprite s, float cr, float cg, float cb, float ca, int light) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] k : idx) {
                    double th = Math.PI * k[0] / lat, ph = Math.PI * 2 * k[1] / lon;
                    float nx = (float) (Math.sin(th) * Math.cos(ph)), ny = (float) Math.cos(th), nz = (float) (Math.sin(th) * Math.sin(ph));
                    v(vc, m, n, cx + nx * rx, cy + ny * ry, cz + nz * rz, cr, cg, cb, ca, su(s, k[1] / (float) lon), sv(s, k[0] / (float) lat), light, nx, ny, nz);
                }
            }
        }
    }
}
