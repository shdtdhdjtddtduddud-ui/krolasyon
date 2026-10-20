package com.krolasyon.advgear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Minecraft tarzı voksel modelleyici: şekiller (kutu, yuvarlatılmış kutu, elipsoit, döndürme profili, boru, halka,
 * ekstrüzyon) yarım piksellik küp ızgarasına "boyanır", sonra yalnızca görünen yüzler bir ağa (mesh) pişirilir.
 * Birimler piksel (1/16 blok).
 */
public final class Vox {
    public static final float S = 0.5F;
    public static final int CUT = 0, TRANS = 1;

    /** Renk fonksiyonu: yerel koordinat + voksel indeksi → ARGB. */
    public interface Col {
        int c(float x, float y, float z, int i, int j, int k);
    }

    public interface Pred {
        boolean in(float x, float y, float z);
    }

    // anahtar → renk; ayrı haritalarda malzeme kimliği, geçiş ve parıltı
    private final Long2IntOpenHashMap color = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap mat = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap flags = new Long2IntOpenHashMap();
    private Matrix4f M = new Matrix4f();
    private final Deque<Matrix4f> stack = new ArrayDeque<>();
    private int nextMat = 1;

    static long key(int x, int y, int z) {
        return ((long) (x + 2048) << 24) | ((long) (y + 2048) << 12) | (z + 2048);
    }

    // ------------------------------------------------------------------ dönüşümler

    public Vox push() {
        stack.push(new Matrix4f(M));
        return this;
    }

    public Vox pop() {
        M = stack.pop();
        return this;
    }

    public Vox translate(float x, float y, float z) {
        M.translate(x, y, z);
        return this;
    }

    public Vox rotX(float deg) {
        M.rotateX((float) Math.toRadians(deg));
        return this;
    }

    public Vox rotY(float deg) {
        M.rotateY((float) Math.toRadians(deg));
        return this;
    }

    public Vox rotZ(float deg) {
        M.rotateZ((float) Math.toRadians(deg));
        return this;
    }

    // ------------------------------------------------------------------ çekirdek doldurma

    /** Yerel kutu sınırları içinde yüklemi sağlayan voksellere renk koyar (pass: CUT/TRANS, glow: parıltı kopyası). */
    public Vox fill(float x0, float y0, float z0, float x1, float y1, float z1, Pred p, Col c, int pass, boolean glow) {
        int id = nextMat++;
        Matrix4f inv = new Matrix4f(M).invert();
        float[] mn = {1e9F, 1e9F, 1e9F}, mx = {-1e9F, -1e9F, -1e9F};
        for (int k = 0; k < 8; k++) {
            Vector3f v = M.transformPosition(new Vector3f((k & 1) == 0 ? x0 : x1, (k & 2) == 0 ? y0 : y1, (k & 4) == 0 ? z0 : z1));
            mn[0] = Math.min(mn[0], v.x); mn[1] = Math.min(mn[1], v.y); mn[2] = Math.min(mn[2], v.z);
            mx[0] = Math.max(mx[0], v.x); mx[1] = Math.max(mx[1], v.y); mx[2] = Math.max(mx[2], v.z);
        }
        int i0 = (int) Math.floor(mn[0] / S) - 1, i1 = (int) Math.ceil(mx[0] / S) + 1;
        int j0 = (int) Math.floor(mn[1] / S) - 1, j1 = (int) Math.ceil(mx[1] / S) + 1;
        int k0 = (int) Math.floor(mn[2] / S) - 1, k1 = (int) Math.ceil(mx[2] / S) + 1;
        Vector3f w = new Vector3f();
        for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
                for (int k = k0; k <= k1; k++) {
                    w.set((i + 0.5F) * S, (j + 0.5F) * S, (k + 0.5F) * S);
                    inv.transformPosition(w);
                    if (w.x < x0 - S || w.x > x1 + S || w.y < y0 - S || w.y > y1 + S || w.z < z0 - S || w.z > z1 + S) continue;
                    if (!p.in(w.x, w.y, w.z)) continue;
                    long key = key(i, j, k);
                    if (c == null) {
                        color.remove(key);
                        mat.remove(key);
                        flags.remove(key);
                        continue;
                    }
                    color.put(key, c.c(w.x, w.y, w.z, i, j, k));
                    mat.put(key, id);
                    flags.put(key, pass | (glow ? 2 : 0));
                }
            }
        }
        return this;
    }

    public Vox erase(float x0, float y0, float z0, float x1, float y1, float z1, Pred p) {
        return fill(x0, y0, z0, x1, y1, z1, p, null, 0, false);
    }

    // ------------------------------------------------------------------ şekiller

    public Vox box(float x0, float y0, float z0, float x1, float y1, float z1, Col c) {
        return fill(x0, y0, z0, x1, y1, z1, (x, y, z) -> x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1, c, CUT, false);
    }

    /** Köşeleri pahlı/yuvarlatılmış kutu (vokselde basamaklı köşeler). */
    public Vox rbox(float x0, float y0, float z0, float x1, float y1, float z1, float r, Col c) {
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, cz = (z0 + z1) / 2, hx = (x1 - x0) / 2 - r, hy = (y1 - y0) / 2 - r, hz = (z1 - z0) / 2 - r;
        return fill(x0, y0, z0, x1, y1, z1, (x, y, z) -> {
            float dx = Math.max(0, Math.abs(x - cx) - hx), dy = Math.max(0, Math.abs(y - cy) - hy), dz = Math.max(0, Math.abs(z - cz) - hz);
            return dx * dx + dy * dy + dz * dz <= r * r;
        }, c, CUT, false);
    }

    public Vox ell(float cx, float cy, float cz, float rx, float ry, float rz, Col c, int pass, boolean glow) {
        return fill(cx - rx, cy - ry, cz - rz, cx + rx, cy + ry, cz + rz, (x, y, z) -> sq((x - cx) / rx) + sq((y - cy) / ry) + sq((z - cz) / rz) <= 1,
                c, pass, glow);
    }

    public Vox ell(float cx, float cy, float cz, float rx, float ry, float rz, Col c) {
        return ell(cx, cy, cz, rx, ry, rz, c, CUT, false);
    }

    /**
     * Döndürme profili (r, y) — Y ekseni etrafında; ax/az kesit oranı, p süperelips üssü (2 daire, 6 köşeli kare).
     * thick > 0 ise kabuk (cam şişe) olarak doldurur.
     */
    public Vox lathe(float[][] prof, float ax, float az, float p, float thick, Col c, int pass, boolean glow) {
        float rmax = 0;
        for (float[] q : prof) rmax = Math.max(rmax, q[0]);
        float y0 = prof[0][1], y1 = prof[prof.length - 1][1];
        float R = rmax;
        return fill(-R * ax, Math.min(y0, y1), -R * az, R * ax, Math.max(y0, y1), R * az, (x, y, z) -> {
            float r = norm(x / ax, z / az, p);
            float lim = rAt(prof, y);
            if (lim <= 0 || y < Math.min(y0, y1) || y > Math.max(y0, y1)) return false;
            return r <= lim && (thick <= 0 || r >= lim - thick);
        }, c, pass, glow);
    }

    public Vox lathe(float[][] prof, Col c) {
        return lathe(prof, 1, 1, 2, 0, c, CUT, false);
    }

    /** Yol boyunca yuvarlak kesitli boru. */
    public Vox tube(float[][] path, float r, Col c) {
        float[] mn = {1e9F, 1e9F, 1e9F}, mx = {-1e9F, -1e9F, -1e9F};
        for (float[] q : path) for (int d = 0; d < 3; d++) {
            mn[d] = Math.min(mn[d], q[d] - r);
            mx[d] = Math.max(mx[d], q[d] + r);
        }
        return fill(mn[0], mn[1], mn[2], mx[0], mx[1], mx[2], (x, y, z) -> {
            for (int i = 0; i < path.length - 1; i++) if (segDist2(x, y, z, path[i], path[i + 1]) <= r * r) return true;
            return false;
        }, c, CUT, false);
    }

    public Vox rod(float[] a, float[] b, float r, Col c) {
        return tube(new float[][]{a, b}, r, c);
    }

    /** Yassı kesitli şerit (kayış): yol boyunca genişlik w (yukarı vektör yönünde), kalınlık t. */
    public Vox strap(float[][] path, float w, float t, float[] up, Col c) {
        float[] mn = {1e9F, 1e9F, 1e9F}, mx = {-1e9F, -1e9F, -1e9F};
        float e = Math.max(w, t);
        for (float[] q : path) for (int d = 0; d < 3; d++) {
            mn[d] = Math.min(mn[d], q[d] - e);
            mx[d] = Math.max(mx[d], q[d] + e);
        }
        Vector3f U = new Vector3f(up[0], up[1], up[2]).normalize();
        return fill(mn[0], mn[1], mn[2], mx[0], mx[1], mx[2], (x, y, z) -> {
            for (int i = 0; i < path.length - 1; i++) {
                float[] a = path[i], b = path[i + 1];
                float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
                float l2 = dx * dx + dy * dy + dz * dz;
                float tt = l2 < 1e-9F ? 0 : Math.max(0, Math.min(1, ((x - a[0]) * dx + (y - a[1]) * dy + (z - a[2]) * dz) / l2));
                float px = x - (a[0] + dx * tt), py = y - (a[1] + dy * tt), pz = z - (a[2] + dz * tt);
                float along = px * U.x + py * U.y + pz * U.z;
                float rest = (float) Math.sqrt(Math.max(0, px * px + py * py + pz * pz - along * along));
                if (Math.abs(along) <= w / 2 && rest <= t / 2) return true;
            }
            return false;
        }, c, CUT, false);
    }

    /** XY düzleminde (z merkezli) elips halka. */
    public Vox ringXY(float cx, float cy, float cz, float rx, float ry, float r, Col c) {
        return fill(cx - rx - r, cy - ry - r, cz - r, cx + rx + r, cy + ry + r, cz + r, (x, y, z) -> {
            float ang = (float) Math.atan2((y - cy) / ry, (x - cx) / rx);
            float px = cx + rx * (float) Math.cos(ang), py = cy + ry * (float) Math.sin(ang);
            return sq(x - px) + sq(y - py) + sq(z - cz) <= r * r;
        }, c, CUT, false);
    }

    /** XY düzlemindeki çokgeni z0..z1 arasında kalınlaştırır. */
    public Vox extrude(float[][] poly, float z0, float z1, Col c) {
        float x0 = 1e9F, x1 = -1e9F, y0 = 1e9F, y1 = -1e9F;
        for (float[] q : poly) {
            x0 = Math.min(x0, q[0]); x1 = Math.max(x1, q[0]);
            y0 = Math.min(y0, q[1]); y1 = Math.max(y1, q[1]);
        }
        return fill(x0, y0, z0, x1, y1, z1, (x, y, z) -> z >= z0 && z <= z1 && inPoly(poly, x, y), c, CUT, false);
    }

    // ------------------------------------------------------------------ yardımcı matematik

    static float sq(float f) {
        return f * f;
    }

    static float norm(float x, float z, float p) {
        if (p == 2F) return (float) Math.sqrt(x * x + z * z);
        if (p == 8F) return Math.max(Math.max(Math.abs(x), Math.abs(z)), (Math.abs(x) + Math.abs(z)) * 0.7071F); // sekizgen
        return (float) Math.pow(Math.pow(Math.abs(x), p) + Math.pow(Math.abs(z), p), 1 / p);
    }

    public static float rAt(float[][] prof, float y) {
        float best = 0;
        for (int i = 0; i < prof.length - 1; i++) {
            float a = prof[i][1], b = prof[i + 1][1], lo = Math.min(a, b), hi = Math.max(a, b);
            if (y < lo || y > hi) continue;
            if (hi - lo < 1e-6F) best = Math.max(best, Math.max(prof[i][0], prof[i + 1][0]));
            else best = Math.max(best, prof[i][0] + (prof[i + 1][0] - prof[i][0]) * (y - a) / (b - a));
        }
        return best;
    }

    static float segDist2(float x, float y, float z, float[] a, float[] b) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float l2 = dx * dx + dy * dy + dz * dz;
        float t = l2 < 1e-9F ? 0 : Math.max(0, Math.min(1, ((x - a[0]) * dx + (y - a[1]) * dy + (z - a[2]) * dz) / l2));
        return sq(x - a[0] - dx * t) + sq(y - a[1] - dy * t) + sq(z - a[2] - dz * t);
    }

    static boolean inPoly(float[][] poly, float x, float y) {
        boolean in = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            if ((poly[i][1] > y) != (poly[j][1] > y) && x < (poly[j][0] - poly[i][0]) * (y - poly[i][1]) / (poly[j][1] - poly[i][1]) + poly[i][0]) in = !in;
        }
        return in;
    }

    public static float hash(int i, int j, int k) {
        int h = i * 73856093 ^ j * 19349663 ^ k * 83492791;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535F;
    }

    // ------------------------------------------------------------------ renkler

    public static int argb(int rgb, float a) {
        return ((int) (a * 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static int scale(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 255) * f)), g = Math.min(255, (int) (((argb >> 8) & 255) * f)), b = Math.min(255, (int) ((argb & 255) * f));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        int al = (int) (((a >>> 24) & 255) + (((b >>> 24) & 255) - ((a >>> 24) & 255)) * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }

    /** Düz renk + hafif voksel gürültüsü. */
    public static Col solid(int rgb, float noise) {
        int base = 0xFF000000 | rgb;
        return (x, y, z, i, j, k) -> scale(base, 1 + (hash(i, j, k) - 0.5F) * 2 * noise);
    }

    public static Col solid(int rgb) {
        return solid(rgb, 0.05F);
    }

    public static Col alpha(int rgb, float a) {
        int base = argb(rgb, a);
        return (x, y, z, i, j, k) -> scale(base, 1 + (hash(i, j, k) - 0.5F) * 0.06F);
    }

    /** Metal: üstte parlak bant, altta koyu, rastgele parıltılı voksel. */
    public static Col metal(int rgb) {
        int base = 0xFF000000 | rgb;
        return (x, y, z, i, j, k) -> {
            float h = hash(i, j, k);
            float f = 0.88F + 0.18F * h + ((j & 3) == 0 ? 0.08F : 0);
            if (h > 0.93F) f = 1.3F;
            return scale(base, f);
        };
    }

    /** Dikey degrade. */
    public static Col grad(int top, int bottom, float y0, float y1, float noise) {
        int a = 0xFF000000 | top, b = 0xFF000000 | bottom;
        return (x, y, z, i, j, k) -> scale(lerp(b, a, (y - y0) / (y1 - y0)), 1 + (hash(i, j, k) - 0.5F) * 2 * noise);
    }

    /** Deri: koyu lekeli tanecik. */
    public static Col leather(int rgb) {
        int base = 0xFF000000 | rgb;
        return (x, y, z, i, j, k) -> {
            float h = hash(i, j, k);
            return scale(base, h < 0.08F ? 0.8F : 0.93F + h * 0.12F);
        };
    }

    /** Kanvas: dama örgü. */
    public static Col canvas(int rgb) {
        int base = 0xFF000000 | rgb;
        return (x, y, z, i, j, k) -> scale(base, ((i + j + k) & 1) == 0 ? 1.04F : 0.95F);
    }

    // ------------------------------------------------------------------ pişirme

    public static final class Mesh {
        final FloatArrayList[] q = {new FloatArrayList(), new FloatArrayList(), new FloatArrayList()};

        public boolean isEmpty() {
            return q[0].isEmpty() && q[1].isEmpty() && q[2].isEmpty();
        }

        private static final RenderType[] TYPES = {RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS),
                RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS), RenderType.eyes(InventoryMenu.BLOCK_ATLAS)};
        private static TextureAtlasSprite white;

        public void render(PoseStack ps, MultiBufferSource buf, int light) {
            white = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("advgear", "block/m_plain"));
            float u = (white.getU0() + white.getU1()) / 2, v = (white.getV0() + white.getV1()) / 2;
            Matrix4f m = ps.last().pose();
            Matrix3f n = ps.last().normal();
            for (int p = 0; p < 3; p++) {
                FloatArrayList l = q[p];
                if (l.isEmpty()) continue;
                VertexConsumer vc = buf.getBuffer(TYPES[p]);
                int lt = p == 2 ? 0xF000F0 : light;
                float[] a = l.elements();
                for (int i = 0, sz = l.size(); i < sz; i += 10) {
                    vc.vertex(m, a[i], a[i + 1], a[i + 2]).color(a[i + 3], a[i + 4], a[i + 5], a[i + 6]).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(lt).normal(n, a[i + 7], a[i + 8], a[i + 9]).endVertex();
                }
            }
        }
    }

    private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final float[] SHADE = {0.9F, 0.86F, 1.0F, 0.7F, 0.95F, 0.92F};

    public Mesh bake() {
        Mesh mesh = new Mesh();
        for (var e : color.long2IntEntrySet()) {
            long key = e.getLongKey();
            int c = e.getIntValue();
            int i = (int) ((key >> 24) & 0xFFF) - 2048, j = (int) ((key >> 12) & 0xFFF) - 2048, k = (int) (key & 0xFFF) - 2048;
            int fl = flags.get(key), pass = fl & 1, id = mat.get(key);
            boolean glow = (fl & 2) != 0;
            for (int d = 0; d < 6; d++) {
                int[] D = DIRS[d];
                long nk = key(i + D[0], j + D[1], k + D[2]);
                if (color.containsKey(nk)) {
                    int nf = flags.get(nk) & 1;
                    if (nf == CUT) continue;                       // komşu opak: gizli
                    if (pass == TRANS && mat.get(nk) == id) continue;  // aynı saydam malzeme
                }
                face(mesh.q[pass], i, j, k, d, c, SHADE[d]);
                if (glow) face(mesh.q[2], i, j, k, d, scale(c, 0.6F), 1);
            }
        }
        return mesh;
    }

    private static void face(FloatArrayList l, int i, int j, int k, int d, int c, float sh) {
        float x0 = i * S, y0 = j * S, z0 = k * S, x1 = x0 + S, y1 = y0 + S, z1 = z0 + S;
        float r = ((c >> 16) & 255) / 255F * sh, g = ((c >> 8) & 255) / 255F * sh, b = (c & 255) / 255F * sh, a = ((c >>> 24) & 255) / 255F;
        float[][] v = switch (d) {
            case 0 -> new float[][]{{x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}};
            case 1 -> new float[][]{{x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}, {x0, y0, z0}};
            case 2 -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case 3 -> new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case 4 -> new float[][]{{x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}, {x0, y0, z1}};
            default -> new float[][]{{x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}};
        };
        int[] D = DIRS[d];
        for (float[] p : v) {
            l.add(p[0]); l.add(p[1]); l.add(p[2]);
            l.add(r); l.add(g); l.add(b); l.add(a);
            l.add(D[0]); l.add(D[1]); l.add(D[2]);
        }
    }
}
