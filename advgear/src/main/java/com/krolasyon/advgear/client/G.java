package com.krolasyon.advgear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Prosedürel 3D geometri: profil döndürme (lathe), yuvarlatılmış kutu, küre, ekstrüzyon, şerit süpürme, yüzeyli taş
 * ve eğik yüzeyli sıvı. Birimler piksel (1/16 blok). Her malzeme bir çizim geçişine aittir; model üç kez çağrılır ve
 * her seferinde yalnızca o geçişin malzemeleri yazılır (tamponlar karışmasın diye).
 */
public final class G {
    public enum Pass { CUT, TRANS, GLOW }

    private static final RenderType[] TYPES = {RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS),
            RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS), RenderType.eyes(InventoryMenu.BLOCK_ATLAS)};

    /** Malzeme: atlas dokusu + renk + geçiş. */
    public record Mat(ResourceLocation tex, float r, float g, float b, float a, Pass pass) {
        public Mat tint(float f) {
            return new Mat(tex, Math.min(1, r * f), Math.min(1, g * f), Math.min(1, b * f), a, pass);
        }

        public Mat alpha(float na) {
            return new Mat(tex, r, g, b, na, pass);
        }

        public Mat as(Pass p) {
            return new Mat(tex, r, g, b, a, p);
        }
    }

    public static ResourceLocation tex(String n) {
        return new ResourceLocation("advgear", "block/m_" + n);
    }

    public static Mat mat(String tex, int rgb, Pass p) {
        return new Mat(tex(tex), (rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, 1F, p);
    }

    public static Mat mat(String tex, int rgb, float a, Pass p) {
        return new Mat(tex(tex), (rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, a, p);
    }

    // ------------------------------------------------------------------ çizim bağlamı

    public final PoseStack ps;
    private final MultiBufferSource buf;
    public int light;
    public Pass pass;
    private VertexConsumer vc;
    private TextureAtlasSprite sprite;
    private ResourceLocation spriteKey;
    private Matrix4f m;
    private Matrix3f n;

    public G(PoseStack ps, MultiBufferSource buf, int light) {
        this.ps = ps;
        this.buf = buf;
        this.light = light;
    }

    /** Modeli üç geçişte çizer. */
    public void draw(Runnable model) {
        for (Pass p : Pass.values()) {
            pass = p;
            vc = null;
            model.run();
        }
    }

    private boolean begin(Mat mat) {
        if (mat.pass != pass) return false;
        if (vc == null) vc = buf.getBuffer(TYPES[pass.ordinal()]);
        if (spriteKey != mat.tex) {
            sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(mat.tex);
            spriteKey = mat.tex;
        }
        m = ps.last().pose();
        n = ps.last().normal();
        return true;
    }

    private void v(Mat c, float x, float y, float z, float u, float vv, float nx, float ny, float nz, float shade) {
        float uu = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * clamp01(u);
        float vvv = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * clamp01(vv);
        int l = pass == Pass.GLOW ? 0xF000F0 : light;
        vc.vertex(m, x, y, z).color(c.r * shade, c.g * shade, c.b * shade, c.a).uv(uu, vvv).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(l).normal(n, nx, ny, nz).endVertex();
    }

    private static float clamp01(float f) {
        return f < 0 ? 0 : f > 1 ? 1 : f;
    }

    // ------------------------------------------------------------------ dörtgen ve üçgen

    public void quad(Mat c, float[] a, float[] b, float[] cc, float[] d) {
        if (!begin(c)) return;
        Vector3f nn = normal(a, b, d);
        v(c, a[0], a[1], a[2], a.length > 3 ? a[3] : 0, a.length > 4 ? a[4] : 0, nn.x, nn.y, nn.z, 1);
        v(c, b[0], b[1], b[2], b.length > 3 ? b[3] : 1, b.length > 4 ? b[4] : 0, nn.x, nn.y, nn.z, 1);
        v(c, cc[0], cc[1], cc[2], cc.length > 3 ? cc[3] : 1, cc.length > 4 ? cc[4] : 1, nn.x, nn.y, nn.z, 1);
        v(c, d[0], d[1], d[2], d.length > 3 ? d[3] : 0, d.length > 4 ? d[4] : 1, nn.x, nn.y, nn.z, 1);
    }

    private static Vector3f normal(float[] a, float[] b, float[] d) {
        Vector3f e1 = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        Vector3f e2 = new Vector3f(d[0] - a[0], d[1] - a[1], d[2] - a[2]);
        Vector3f r = e1.cross(e2);
        if (r.lengthSquared() < 1e-12F) return new Vector3f(0, 1, 0);
        return r.normalize();
    }

    // ------------------------------------------------------------------ profil döndürme

    private static float[] cs(double a, float ax, float az, float p) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        if (p != 2F) {
            c = Math.signum(c) * (float) Math.pow(Math.abs(c), 2 / p);
            s = Math.signum(s) * (float) Math.pow(Math.abs(s), 2 / p);
        }
        return new float[]{c * ax, s * az};
    }

    /**
     * Profil (r, y) noktalarını Y ekseni etrafında döndürür. ax/az kesit en-boy oranı, p süperelips üssü
     * (2 = daire, 4-6 = köşeleri yuvarlatılmış dikdörtgen). smooth=false yüzeyli (taş/kesik cam) görünüm verir.
     */
    public void lathe(Mat c, float[][] prof, int seg, boolean smooth, float ax, float az, float p) {
        if (!begin(c)) return;
        int np = prof.length;
        float len = 0;
        float[] acc = new float[np];
        for (int i = 1; i < np; i++) {
            len += (float) Math.hypot(prof[i][0] - prof[i - 1][0], prof[i][1] - prof[i - 1][1]);
            acc[i] = len;
        }
        float[][] rim = new float[seg + 1][];
        for (int k = 0; k <= seg; k++) rim[k] = cs(Math.PI * 2 * k / seg, ax, az, p);
        for (int i = 0; i < np - 1; i++) {
            float[] p0 = prof[i], p1 = prof[i + 1];
            float[] n0 = profNormal(prof, i), n1 = profNormal(prof, i + 1);
            float v0 = len > 0 ? acc[i] / len : 0, v1 = len > 0 ? acc[i + 1] / len : 1;
            for (int k = 0; k < seg; k++) {
                float[] c0 = rim[k], c1 = rim[k + 1];
                float[] A = {c0[0] * p0[0], p0[1], c0[1] * p0[0]}, B = {c1[0] * p0[0], p0[1], c1[1] * p0[0]};
                float[] C = {c1[0] * p1[0], p1[1], c1[1] * p1[0]}, D = {c0[0] * p1[0], p1[1], c0[1] * p1[0]};
                float u0 = k / (float) seg, u1 = (k + 1) / (float) seg;
                if (smooth) {
                    v(c, A[0], A[1], A[2], u0, v0, c0[0] * n0[0] / ax, n0[1], c0[1] * n0[0] / az, 1);
                    v(c, B[0], B[1], B[2], u1, v0, c1[0] * n0[0] / ax, n0[1], c1[1] * n0[0] / az, 1);
                    v(c, C[0], C[1], C[2], u1, v1, c1[0] * n1[0] / ax, n1[1], c1[1] * n1[0] / az, 1);
                    v(c, D[0], D[1], D[2], u0, v1, c0[0] * n1[0] / ax, n1[1], c0[1] * n1[0] / az, 1);
                } else {
                    Vector3f nn = normal(A, B, D);
                    float mx = (A[0] + C[0]) / 2, mz = (A[2] + C[2]) / 2;
                    if (nn.x * mx + nn.z * mz < 0 && (Math.abs(mx) + Math.abs(mz)) > 1e-3) nn.mul(-1);
                    float sh = 0.82F + 0.18F * (float) Math.abs(Math.sin(k * 1.7));
                    v(c, A[0], A[1], A[2], u0, v0, nn.x, nn.y, nn.z, sh);
                    v(c, B[0], B[1], B[2], u1, v0, nn.x, nn.y, nn.z, sh);
                    v(c, C[0], C[1], C[2], u1, v1, nn.x, nn.y, nn.z, sh);
                    v(c, D[0], D[1], D[2], u0, v1, nn.x, nn.y, nn.z, sh);
                }
            }
        }
    }

    public void lathe(Mat c, float[][] prof, int seg) {
        lathe(c, prof, seg, true, 1, 1, 2);
    }

    private static float[] profNormal(float[][] prof, int i) {
        float[] a = prof[Math.max(0, i - 1)], b = prof[Math.min(prof.length - 1, i + 1)];
        float dr = b[0] - a[0], dy = b[1] - a[1];
        float l = (float) Math.hypot(dr, dy);
        if (l < 1e-6) return new float[]{1, 0};
        return new float[]{dy / l, -dr / l};
    }

    /** Kapalı halka profili (yüzük bandı vb.): merkez yarıçap R, kesit genişliği w (radyal) ve yüksekliği h, köşe yuvarlaklığı. */
    public static float[][] bandProfile(float R, float w, float h, float round, int perCorner) {
        float r0 = R - w / 2, r1 = R + w / 2, y0 = -h / 2, y1 = h / 2;
        float rr = Math.min(round, Math.min(w, h) / 2 - 1e-3F);
        float[][] corners = {{r1 - rr, y0 + rr, -90}, {r1 - rr, y1 - rr, 0}, {r0 + rr, y1 - rr, 90}, {r0 + rr, y0 + rr, 180}};
        float[][] out = new float[corners.length * (perCorner + 1) + 1][];
        int k = 0;
        for (float[] cc : corners) {
            for (int j = 0; j <= perCorner; j++) {
                double a = Math.toRadians(cc[2] + 90.0 * j / perCorner);
                out[k++] = new float[]{cc[0] + rr * (float) Math.cos(a), cc[1] + rr * (float) Math.sin(a)};
            }
        }
        out[k] = out[0];
        return out;
    }

    // ------------------------------------------------------------------ küre ve yuvarlatılmış kutu

    public void sphere(Mat c, float cx, float cy, float cz, float rx, float ry, float rz, int seg, int rings) {
        if (!begin(c)) return;
        for (int i = 0; i < rings; i++) {
            double t0 = Math.PI * i / rings - Math.PI / 2, t1 = Math.PI * (i + 1) / rings - Math.PI / 2;
            for (int k = 0; k < seg; k++) {
                double a0 = Math.PI * 2 * k / seg, a1 = Math.PI * 2 * (k + 1) / seg;
                double[][] pts = {{t0, a0}, {t0, a1}, {t1, a1}, {t1, a0}};
                float[] uvs = {k / (float) seg, (k + 1) / (float) seg};
                int j = 0;
                for (double[] q : pts) {
                    float nx = (float) (Math.cos(q[0]) * Math.cos(q[1])), ny = (float) Math.sin(q[0]), nz = (float) (Math.cos(q[0]) * Math.sin(q[1]));
                    v(c, cx + nx * rx, cy + ny * ry, cz + nz * rz, uvs[j == 0 || j == 3 ? 0 : 1], (float) ((q[0] + Math.PI / 2) / Math.PI), nx, ny, nz, 1);
                    j++;
                }
            }
        }
    }

    /** Kenarları yuvarlatılmış kutu (yumuşak kumaş/deri görünümü). */
    public void rbox(Mat c, float x0, float y0, float z0, float x1, float y1, float z1, float rad, int sub) {
        if (!begin(c)) return;
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, cz = (z0 + z1) / 2;
        float hx = (x1 - x0) / 2, hy = (y1 - y0) / 2, hz = (z1 - z0) / 2;
        float r = Math.min(rad, Math.min(hx, Math.min(hy, hz)) * 0.98F);
        float ix = hx - r, iy = hy - r, iz = hz - r;
        // 6 yüz: (eksen, işaret)
        int[][] faces = {{0, 1}, {0, -1}, {1, 1}, {1, -1}, {2, 1}, {2, -1}};
        for (int[] f : faces) {
            for (int i = 0; i < sub; i++) {
                for (int j = 0; j < sub; j++) {
                    float[][] q = new float[4][];
                    float[][] nq = new float[4][];
                    float[][] uq = new float[4][];
                    float[][] st = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                    for (int k = 0; k < 4; k++) {
                        float a = st[k][0] / sub * 2 - 1, b = st[k][1] / sub * 2 - 1;
                        float[] cube = new float[3];
                        int ax = f[0], u = (ax + 1) % 3, w = (ax + 2) % 3;
                        cube[ax] = f[1];
                        cube[u] = f[1] > 0 ? a : -a;
                        cube[w] = b;
                        // küp noktasını yarıçaplı kutuya eşle
                        float px = cube[0] * hx, py = cube[1] * hy, pz = cube[2] * hz;
                        float qx = Math.max(-ix, Math.min(ix, px)), qy = Math.max(-iy, Math.min(iy, py)), qz = Math.max(-iz, Math.min(iz, pz));
                        float dx = px - qx, dy = py - qy, dz = pz - qz;
                        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (l < 1e-6F) {
                            dx = ax == 0 ? f[1] : 0;
                            dy = ax == 1 ? f[1] : 0;
                            dz = ax == 2 ? f[1] : 0;
                            l = 1;
                        }
                        dx /= l;
                        dy /= l;
                        dz /= l;
                        q[k] = new float[]{cx + qx + dx * r, cy + qy + dy * r, cz + qz + dz * r};
                        nq[k] = new float[]{dx, dy, dz};
                        uq[k] = new float[]{st[k][0] / sub, st[k][1] / sub};
                    }
                    for (int k = 0; k < 4; k++) v(c, q[k][0], q[k][1], q[k][2], uq[k][0], uq[k][1], nq[k][0], nq[k][1], nq[k][2], 1);
                }
            }
        }
    }

    /** Keskin köşeli kutu. */
    public void box(Mat c, float x0, float y0, float z0, float x1, float y1, float z1) {
        if (!begin(c)) return;
        float[][] P = {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] F = {{0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {3, 7, 6, 2}, {0, 1, 5, 4}};
        float[][] N = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        float[][] UV = {{0, 1}, {0, 0}, {1, 0}, {1, 1}};
        for (int f = 0; f < 6; f++) {
            for (int k = 0; k < 4; k++) {
                float[] p = P[F[f][k]];
                v(c, p[0], p[1], p[2], UV[k][0], UV[k][1], N[f][0], N[f][1], N[f][2], 1);
            }
        }
    }

    // ------------------------------------------------------------------ ekstrüzyon

    /** 2B çokgeni (x, y) z0..z1 arasında kalınlaştırır (yıldız biçimli şekiller; kapaklar merkezden yelpaze). */
    public void extrude(Mat c, float[][] poly, float z0, float z1, Mat side) {
        int np = poly.length;
        float cx = 0, cy = 0, minx = 1e9F, maxx = -1e9F, miny = 1e9F, maxy = -1e9F;
        for (float[] p : poly) {
            cx += p[0];
            cy += p[1];
            minx = Math.min(minx, p[0]);
            maxx = Math.max(maxx, p[0]);
            miny = Math.min(miny, p[1]);
            maxy = Math.max(maxy, p[1]);
        }
        cx /= np;
        cy /= np;
        float sx = Math.max(1e-3F, maxx - minx), sy = Math.max(1e-3F, maxy - miny);
        if (begin(c)) {
            for (int face = 0; face < 2; face++) {
                float z = face == 0 ? z0 : z1, nz = face == 0 ? -1 : 1;
                for (int i = 0; i < np; i++) {
                    float[] a = poly[i], b = poly[(i + 1) % np];
                    float[] A = face == 0 ? a : b, B = face == 0 ? b : a;
                    v(c, cx, cy, z, (cx - minx) / sx, 1 - (cy - miny) / sy, 0, 0, nz, 1);
                    v(c, A[0], A[1], z, (A[0] - minx) / sx, 1 - (A[1] - miny) / sy, 0, 0, nz, 1);
                    v(c, B[0], B[1], z, (B[0] - minx) / sx, 1 - (B[1] - miny) / sy, 0, 0, nz, 1);
                    v(c, B[0], B[1], z, (B[0] - minx) / sx, 1 - (B[1] - miny) / sy, 0, 0, nz, 1);
                }
            }
        }
        if (begin(side)) {
            float per = 0;
            for (int i = 0; i < np; i++) {
                float[] a = poly[i], b = poly[(i + 1) % np];
                float ex = b[0] - a[0], ey = b[1] - a[1];
                float l = (float) Math.hypot(ex, ey);
                if (l < 1e-6) continue;
                float nx = ey / l, ny = -ex / l;
                if (nx * (a[0] - cx) + ny * (a[1] - cy) < 0) {
                    nx = -nx;
                    ny = -ny;
                }
                float u0 = (per % 8) / 8F, u1 = u0 + Math.min(1 - u0, l / 8F);
                per += l;
                v(side, a[0], a[1], z0, u0, 0, nx, ny, 0, 0.9F);
                v(side, b[0], b[1], z0, u1, 0, nx, ny, 0, 0.9F);
                v(side, b[0], b[1], z1, u1, 1, nx, ny, 0, 0.9F);
                v(side, a[0], a[1], z1, u0, 1, nx, ny, 0, 0.9F);
            }
        }
    }

    // ------------------------------------------------------------------ süpürme (kayış, tel, boru)

    /**
     * Yol boyunca dikdörtgen/yuvarlak kesit süpürür. {@code up} kesitin "yukarı" yönünü sabitler (kayışlar düz kalsın).
     * shape: birim kesit (x, y) noktaları, w/h ölçek.
     */
    public void sweep(Mat c, float[][] path, float[][] shape, float w, float h, float[] up, boolean closed) {
        if (!begin(c)) return;
        int np = path.length, ns = shape.length;
        Vector3f U = new Vector3f(up[0], up[1], up[2]);
        float[][][] ring = new float[np][ns][];
        float[][][] nor = new float[np][ns][];
        float acc = 0;
        float[] vs = new float[np];
        for (int i = 0; i < np; i++) {
            float[] a = path[closed ? (i - 1 + np) % np : Math.max(0, i - 1)], b = path[closed ? (i + 1) % np : Math.min(np - 1, i + 1)];
            Vector3f T = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
            if (T.lengthSquared() < 1e-12F) T.set(1, 0, 0);
            T.normalize();
            Vector3f S = new Vector3f(T).cross(U);
            if (S.lengthSquared() < 1e-8F) S = new Vector3f(T).cross(new Vector3f(1, 0, 0));
            S.normalize();
            Vector3f Uu = new Vector3f(S).cross(T).normalize();
            for (int k = 0; k < ns; k++) {
                float sx = shape[k][0] * w, sy = shape[k][1] * h;
                ring[i][k] = new float[]{path[i][0] + S.x * sx + Uu.x * sy, path[i][1] + S.y * sx + Uu.y * sy, path[i][2] + S.z * sx + Uu.z * sy};
                float nx = shape[k][0] / Math.max(w, 1e-3F), ny = shape[k][1] / Math.max(h, 1e-3F);
                float l = (float) Math.hypot(nx, ny);
                nx /= Math.max(l, 1e-6F);
                ny /= Math.max(l, 1e-6F);
                nor[i][k] = new float[]{S.x * nx + Uu.x * ny, S.y * nx + Uu.y * ny, S.z * nx + Uu.z * ny};
            }
            if (i > 0) acc += (float) Math.sqrt(sq(path[i][0] - path[i - 1][0]) + sq(path[i][1] - path[i - 1][1]) + sq(path[i][2] - path[i - 1][2]));
            vs[i] = acc;
        }
        int segs = closed ? np : np - 1;
        for (int i = 0; i < segs; i++) {
            int i1 = (i + 1) % np;
            float v0 = (vs[i] % 6) / 6F, v1 = v0 + Math.min(1 - v0, Math.abs(vs[i1] - vs[i]) / 6F);
            for (int k = 0; k < ns; k++) {
                int k1 = (k + 1) % ns;
                float u0 = k / (float) ns, u1 = (k + 1) / (float) ns;
                float[] A = ring[i][k], B = ring[i][k1], C = ring[i1][k1], D = ring[i1][k];
                v(c, A[0], A[1], A[2], u0, v0, nor[i][k][0], nor[i][k][1], nor[i][k][2], 1);
                v(c, B[0], B[1], B[2], u1, v0, nor[i][k1][0], nor[i][k1][1], nor[i][k1][2], 1);
                v(c, C[0], C[1], C[2], u1, v1, nor[i1][k1][0], nor[i1][k1][1], nor[i1][k1][2], 1);
                v(c, D[0], D[1], D[2], u0, v1, nor[i1][k][0], nor[i1][k][1], nor[i1][k][2], 1);
            }
        }
    }

    private static float sq(float f) {
        return f * f;
    }

    public static final float[][] RECT = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
    public static final float[][] ROUND8 = new float[8][];

    static {
        for (int k = 0; k < 8; k++) ROUND8[k] = new float[]{(float) Math.cos(Math.PI * 2 * k / 8), (float) Math.sin(Math.PI * 2 * k / 8)};
    }

    /** Silindir tel/boru (iki uç arası). */
    public void rod(Mat c, float[] a, float[] b, float r) {
        float[] up = Math.abs(b[1] - a[1]) > Math.abs(b[0] - a[0]) + Math.abs(b[2] - a[2]) ? new float[]{1, 0, 0} : new float[]{0, 1, 0};
        sweep(c, new float[][]{a, b}, ROUND8, r, r, up, false);
    }

    /** XY düzleminde elips yay noktaları (z sabit). */
    public static float[][] arc(float cx, float cy, float z, float rx, float ry, float a0, float a1, int n) {
        float[][] out = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            double a = Math.toRadians(a0 + (a1 - a0) * i / n);
            out[i] = new float[]{cx + rx * (float) Math.cos(a), cy + ry * (float) Math.sin(a), z};
        }
        return out;
    }

    /** XZ düzleminde elips (yatay halka) noktaları. */
    public static float[][] loopXZ(float rx, float rz, float y, int n) {
        float[][] out = new float[n][];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            out[i] = new float[]{rx * (float) Math.cos(a), y, rz * (float) Math.sin(a)};
        }
        return out;
    }

    /** XZ'de yuvarlatılmış dikdörtgen halka (kemer, kayış, gözlük bandı). */
    public static float[][] roundRectXZ(float hx, float hz, float r, float y, int perCorner) {
        float[][] cc = {{hx - r, hz - r, 0}, {-(hx - r), hz - r, 90}, {-(hx - r), -(hz - r), 180}, {hx - r, -(hz - r), 270}};
        float[][] out = new float[4 * perCorner][];
        int k = 0;
        for (float[] c : cc) {
            for (int j = 0; j < perCorner; j++) {
                double a = Math.toRadians(c[2] + 90.0 * j / (perCorner - 1));
                out[k++] = new float[]{c[0] + r * (float) Math.cos(a), y, c[1] + r * (float) Math.sin(a)};
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ kesme taş

    /** Pırlanta kesim: tabla, taç, kuşak, alt kısım; yüzeyli (flat). Y ekseni yukarı, taban y=0. */
    public void gem(Mat c, float r, float h, int facets) {
        float[][] prof = {{0, -h * 0.55F}, {r * 0.98F, -h * 0.02F}, {r, 0}, {r * 0.98F, h * 0.06F}, {r * 0.62F, h * 0.38F}, {r * 0.55F, h * 0.42F}, {0, h * 0.42F}};
        lathe(c, prof, facets, false, 1, 1, 2);
    }

    /** Kabaşon (yarım elipsoit, cilalı). */
    public void cabochon(Mat c, float rx, float rz, float h) {
        float[][] prof = new float[8][];
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 2 * i / 7;
            prof[i] = new float[]{(float) Math.cos(a), (float) Math.sin(a) * h};
        }
        prof[7][0] = 0;
        lathe(c, prof, 18, true, rx, rz, 2);
    }

    // ------------------------------------------------------------------ sıvı

    /**
     * Şişe içindeki sıvı: iç profil (r, y; altta r=0) ile tanımlı hacmin, eğik ve dalgalı bir yüzeyin altında kalan kısmı.
     * fill 0..1, (tx, tz) yüzey eğimi, wave dalga genliği, t zaman.
     */
    public void liquid(Mat c, float[][] prof, float ax, float az, float p, float fill, float tx, float tz, float wave, float t, int seg) {
        if (!begin(c)) return;
        float y0 = prof[0][1], y1 = prof[prof.length - 1][1];
        float h = y0 + (y1 - y0) * fill;
        float[] yp = new float[seg + 1];
        float[][] dir = new float[seg + 1][];
        float rh = rAt(prof, h);
        for (int k = 0; k <= seg; k++) {
            double a = Math.PI * 2 * k / seg;
            dir[k] = cs(a, ax, az, p);
            float w = wave * (float) (Math.sin(a * 2 + t * 0.9) * 0.6 + Math.sin(a * 3 - t * 1.3) * 0.4);
            yp[k] = clamp(h + (tx * dir[k][0] + tz * dir[k][1]) * rh + w, y0 + 0.02F, y1 - 0.02F);
        }
        // yan yüzeyler
        for (int i = 0; i < prof.length - 1; i++) {
            if (Math.abs(prof[i + 1][1] - prof[i][1]) < 1e-4F) {
                // yatay basamak (şişe tabanı / omuz): halka şeklinde düz yüz
                for (int k = 0; k < seg; k++) {
                    if (prof[i][1] > yp[k] && prof[i][1] > yp[k + 1]) continue;
                    float[] d0 = dir[k], d1 = dir[k + 1];
                    float ra = prof[i][0], rb = prof[i + 1][0], y = prof[i][1];
                    v(c, d0[0] * ra, y, d0[1] * ra, 0.5F, 0.5F, 0, -1, 0, 0.8F);
                    v(c, d1[0] * ra, y, d1[1] * ra, 0.5F, 0.5F, 0, -1, 0, 0.8F);
                    v(c, d1[0] * rb, y, d1[1] * rb, 0.6F, 0.6F, 0, -1, 0, 0.8F);
                    v(c, d0[0] * rb, y, d0[1] * rb, 0.6F, 0.6F, 0, -1, 0, 0.8F);
                }
                continue;
            }
            for (int k = 0; k < seg; k++) {
                float ya0 = Math.min(prof[i][1], yp[k]), yb0 = Math.min(prof[i + 1][1], yp[k]);
                float ya1 = Math.min(prof[i][1], yp[k + 1]), yb1 = Math.min(prof[i + 1][1], yp[k + 1]);
                if (ya0 >= yb0 - 1e-4F && ya1 >= yb1 - 1e-4F) continue;
                float ra0 = rAt(prof, ya0), rb0 = rAt(prof, yb0), ra1 = rAt(prof, ya1), rb1 = rAt(prof, yb1);
                float[] d0 = dir[k], d1 = dir[k + 1];
                float u0 = k / (float) seg, u1 = (k + 1) / (float) seg;
                v(c, d0[0] * ra0, ya0, d0[1] * ra0, u0, 0.2F, d0[0], 0, d0[1], 0.85F);
                v(c, d1[0] * ra1, ya1, d1[1] * ra1, u1, 0.2F, d1[0], 0, d1[1], 0.85F);
                v(c, d1[0] * rb1, yb1, d1[1] * rb1, u1, 0.8F, d1[0], 0, d1[1], 0.85F);
                v(c, d0[0] * rb0, yb0, d0[1] * rb0, u0, 0.8F, d0[0], 0, d0[1], 0.85F);
            }
        }
        // yüzey: iki halkalı yelpaze, dalgacıklı
        float ring = 0.55F;
        float hc = h + wave * 0.5F * (float) Math.sin(t * 1.7);
        for (int k = 0; k < seg; k++) {
            float[] d0 = dir[k], d1 = dir[k + 1];
            float r0 = rAt(prof, yp[k]), r1 = rAt(prof, yp[k + 1]);
            float m0 = h + (yp[k] - h) * ring + wave * 0.6F * (float) Math.sin(t * 2.3 + k), m1 = h + (yp[k + 1] - h) * ring + wave * 0.6F * (float) Math.sin(t * 2.3 + k + 1);
            float nx = -tx * 0.6F, nz = -tz * 0.6F;
            v(c, 0, hc, 0, 0.5F, 0.5F, nx, 1, nz, 1.15F);
            v(c, d0[0] * r0 * ring, m0, d0[1] * r0 * ring, 0.3F, 0.3F, nx, 1, nz, 1.15F);
            v(c, d1[0] * r1 * ring, m1, d1[1] * r1 * ring, 0.7F, 0.3F, nx, 1, nz, 1.15F);
            v(c, d1[0] * r1 * ring, m1, d1[1] * r1 * ring, 0.7F, 0.3F, nx, 1, nz, 1.15F);
            v(c, d0[0] * r0 * ring, m0, d0[1] * r0 * ring, 0.3F, 0.3F, nx, 1, nz, 1.1F);
            v(c, d1[0] * r1 * ring, m1, d1[1] * r1 * ring, 0.7F, 0.3F, nx, 1, nz, 1.1F);
            v(c, d1[0] * r1, yp[k + 1], d1[1] * r1, 0.9F, 0.1F, nx, 1, nz, 1.1F);
            v(c, d0[0] * r0, yp[k], d0[1] * r0, 0.1F, 0.1F, nx, 1, nz, 1.1F);
        }
    }

    /** Sıvı yüzeyinin belirli bir noktadaki yüksekliği (kabarcıkları kırpmak için). */
    public static float surface(float[][] prof, float fill, float tx, float tz, float x, float z) {
        float y0 = prof[0][1], y1 = prof[prof.length - 1][1];
        return y0 + (y1 - y0) * fill + tx * x + tz * z;
    }

    public static float rAt(float[][] prof, float y) {
        if (y <= prof[0][1]) return prof[0][0];
        for (int i = 0; i < prof.length - 1; i++) {
            float a = prof[i][1], b = prof[i + 1][1];
            if (y >= a && y <= b) {
                float t = b - a < 1e-6F ? 0 : (y - a) / (b - a);
                return prof[i][0] + (prof[i + 1][0] - prof[i][0]) * t;
            }
        }
        return prof[prof.length - 1][0];
    }

    private static float clamp(float v, float a, float b) {
        return v < a ? a : v > b ? b : v;
    }
}
