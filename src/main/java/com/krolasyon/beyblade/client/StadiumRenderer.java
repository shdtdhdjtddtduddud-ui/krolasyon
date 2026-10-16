package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.StadiumEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static com.krolasyon.beyblade.entity.StadiumEntity.*;

/**
 * Procedural stadium mesh: curved bowl floor, lip, four hazard-striped walls with BATTLE END badges, flat pockets
 * between them, and a two-tier octagonal base. Texture atlas: textures/entity/stadium.png (gen/beyblade/stadium_tex.py).
 */
public class StadiumRenderer extends EntityRenderer<StadiumEntity> {
    private static final ResourceLocation TEX = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/entity/stadium.png");
    private static final int SEGS = 128;
    private static float[] mesh;

    public StadiumRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(StadiumEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        if (mesh == null) mesh = build();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float[] d = mesh;
        for (int i = 0; i < d.length; i += 8) {
            com.krolasyon.beyblade.compat.VClient.vertex(vc, m, n, d[i], d[i + 1], d[i + 2], 1F, 1F, 1F, 1F, d[i + 3], d[i + 4],
                    OverlayTexture.NO_OVERLAY, light, d[i + 5], d[i + 6], d[i + 7]);
        }
        renderPads(e, partial, pose, buf);
        renderScars(e, partial, pose, buf);
        super.render(e, yaw, partial, pose, buf, light);
    }

    private static final ResourceLocation CRACKS = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/fx/cracks.png");

    /** impact cracks in the bowl floor, following its curve; they fade away over a minute and a half */
    private void renderScars(StadiumEntity e, float partial, PoseStack pose, MultiBufferSource buf) {
        if (e.scars.isEmpty()) return;
        Matrix4f m = pose.last().pose();
        float now = e.tickCount + partial;
        int G = 6;
        for (float[] sc : e.scars) {
            float age = now - sc[3];
            if (age < 0 || age > SCAR_LIFE) continue;
            float a = Math.min(1F, (SCAR_LIFE - age) / 300F) * 0.9F;
            VertexConsumer vc = Fx.bufT(buf, CRACKS, a);
            float r = sc[2], cr = Mth.cos(sc[4]), sr = Mth.sin(sc[4]);
            for (int i = 0; i < G; i++) {
                for (int j = 0; j < G; j++) {
                    float[][] q = new float[4][];
                    float[][] uv = new float[4][];
                    int[][] c = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
                    for (int k = 0; k < 4; k++) {
                        float u = c[k][0] / (float) G * 2 - 1, v = c[k][1] / (float) G * 2 - 1;
                        float x = sc[0] + (u * cr - v * sr) * r, z = sc[1] + (u * sr + v * cr) * r;
                        float y = (float) surface(Math.sqrt(x * x + z * z)) + 0.02F;
                        q[k] = new float[]{x, y, z};
                        uv[k] = new float[]{(u + 1) / 2, (v + 1) / 2};
                    }
                    float[] w = {1F, 1F, 1F};
                    Fx.quad(vc, m, q[0], q[1], q[2], q[3], uv[0][0], uv[0][1], uv[2][0], uv[2][1], w, w);
                }
            }
            Fx.alpha = 1F;
        }
    }

    /** glowing launch pads at both ends: blue (side A, +z) and red (side B, -z) */
    private void renderPads(StadiumEntity e, float partial, PoseStack pose, MultiBufferSource buf) {
        float t = e.tickCount + partial;
        boolean live = e.matchState() != StadiumEntity.M_IDLE;
        for (int side = 0; side < 2; side++) {
            float[] c = side == 0 ? new float[]{0.25F, 0.55F, 1F} : new float[]{1F, 0.25F, 0.2F};
            float pulse = 0.65F + 0.35F * Mth.sin(t * (live ? 0.3F : 0.1F) + side * 3F);
            pose.pushPose();
            pose.translate(0, 0.03, side == 0 ? PAD_R : -PAD_R);
            Matrix4f m = pose.last().pose();
            VertexConsumer vc = Fx.buf(buf, Fx.SIGIL);
            Fx.groundQuad(vc, m, 0.02F, PAD_HALF, Fx.c(c[0], c[1], c[2], 0.9F * pulse));
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.ring(vc, m, 0.03F, PAD_HALF * 0.95F, PAD_HALF * 1.15F, 32, Fx.c(c[0], c[1], c[2], pulse), 1F, 0F);
            Fx.band(vc, m, PAD_HALF * 1.05F, 0F, PAD_HALF * 1.05F, 1.4F * pulse, 32, Fx.c(c[0], c[1], c[2], 0.5F * pulse), new float[]{0, 0, 0}, 1F, 0F, 1F, 0F);
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(StadiumEntity e) { return TEX; }

    // ------------------------------------------------------------------ mesh
    private static final List<float[]> V = new ArrayList<>();

    private static void vert(double x, double y, double z, double u, double v, double nx, double ny, double nz) {
        double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1.0E-6) l = 1;
        V.add(new float[]{(float) x, (float) y, (float) z, (float) (u / 512.0), (float) (v / 512.0), (float) (nx / l), (float) (ny / l), (float) (nz / l)});
    }

    private static double ang(int i) { return i * Math.PI * 2 / SEGS; }

    private static double octR(double apothem, double a) {
        double deg = Math.toDegrees(a);
        double phi = ((deg % 45.0) + 45.0) % 45.0 - 22.5;      // edge centres at 0, 45, 90 ...
        return apothem / Math.cos(Math.toRadians(phi));
    }

    /** horizontal-ish ring band between radius functions at two heights, uv from region (u0..u1, v0..v1) */
    private interface RF { double r(double a); }

    private static void band(RF r0, double y0, RF r1, double y1, double u0, double v0, double u1, double v1, boolean flatTop) {
        for (int i = 0; i < SEGS; i++) {
            double a0 = ang(i), a1 = ang(i + 1);
            double ra0 = r0.r(a0), ra1 = r0.r(a1), rb0 = r1.r(a0), rb1 = r1.r(a1);
            double uu0 = u0 + (u1 - u0) * i / SEGS, uu1 = u0 + (u1 - u0) * (i + 1) / SEGS;
            double am = (a0 + a1) / 2;
            double nx, ny, nz;
            if (flatTop) {
                nx = 0; ny = 1; nz = 0;
            } else {
                // profile goes from (r0, y0) to (r1, y1): this normal faces up/outward for slopes and walls alike
                double dr = r1.r(am) - r0.r(am), dy = y1 - y0;
                nx = -dy * Math.cos(am); ny = dr; nz = -dy * Math.sin(am);
            }
            vert(Math.cos(a0) * ra0, y0, Math.sin(a0) * ra0, uu0, v0, nx, ny, nz);
            vert(Math.cos(a0) * rb0, y1, Math.sin(a0) * rb0, uu0, v1, nx, ny, nz);
            vert(Math.cos(a1) * rb1, y1, Math.sin(a1) * rb1, uu1, v1, nx, ny, nz);
            vert(Math.cos(a1) * ra1, y0, Math.sin(a1) * ra1, uu1, v0, nx, ny, nz);
        }
    }

    private static float[] build() {
        V.clear();
        // ---- bowl floor (planar uv into the 256x256 floor region)
        int RINGS = 32;
        for (int j = 0; j < RINGS; j++) {
            double r0 = BOWL_R * j / RINGS, r1 = BOWL_R * (j + 1) / RINGS;
            for (int i = 0; i < SEGS; i++) {
                double a0 = ang(i), a1 = ang(i + 1);
                double[][] p = {{r0, a0}, {r1, a0}, {r1, a1}, {r0, a1}};
                for (double[] q : p) {
                    double x = Math.cos(q[1]) * q[0], z = Math.sin(q[1]) * q[0];
                    double s = slope(q[0]);
                    vert(x, surface(q[0]), z, 128 + x / BOWL_R * 124, 128 + z / BOWL_R * 124, -s * Math.cos(q[1]), 1, -s * Math.sin(q[1]));
                }
            }
        }
        // ---- lip and the flat ledge (walls stand on it, pockets open through it)
        band(a -> BOWL_R, FLOOR_Y0 + FLOOR_RISE, a -> LIP_R, LIP_Y, 256, 160, 512, 200, false);
        band(a -> LIP_R, LIP_Y, a -> LEDGE_R, LIP_Y, 256, 200, 512, 250, true);
        // ---- outer slope down to tier 2, tier 2 top and side, tier 1 top and side
        band(a -> LEDGE_R, LIP_Y, a -> SLOPE_R, TIER2_Y, 256, 130, 512, 160, false);
        band(a -> SLOPE_R, TIER2_Y, a -> octR(APOTHEM2, a), TIER2_Y, 256, 140, 512, 250, true);
        band(a -> octR(APOTHEM2, a), TIER2_Y, a -> octR(APOTHEM2, a), TIER1_Y, 256, 10, 512, 60, false);
        band(a -> octR(APOTHEM2, a), TIER1_Y, a -> octR(APOTHEM1, a), TIER1_Y, 256, 140, 512, 250, true);
        // tier 1 side: plain grey except the front edge (+z), which carries the stadium label
        for (int i = 0; i < SEGS; i++) {
            double a0 = ang(i), a1 = ang(i + 1), am = (a0 + a1) / 2;
            double r0 = octR(APOTHEM1, a0), r1 = octR(APOTHEM1, a1);
            double deg = Math.toDegrees(am);
            boolean front = deg > 67.5 && deg < 112.5;
            double u0, u1, v0, v1;
            if (front) {
                double f0 = (Math.toDegrees(a0) - 67.5) / 45.0, f1 = (Math.toDegrees(a1) - 67.5) / 45.0;
                u0 = 512 * (1 - f0); u1 = 512 * (1 - f1); v0 = 448; v1 = 384;
            } else {
                u0 = 256 + 256.0 * i / SEGS; u1 = 256 + 256.0 * (i + 1) / SEGS; v0 = 120; v1 = 70;
            }
            double nx = Math.cos(Math.toRadians(Math.round(Math.toDegrees(am) / 45.0) * 45.0)), nz = Math.sin(Math.toRadians(Math.round(Math.toDegrees(am) / 45.0) * 45.0));
            vert(Math.cos(a0) * r0, 0, Math.sin(a0) * r0, u0, v0, nx, 0, nz);
            vert(Math.cos(a0) * r0, TIER1_Y, Math.sin(a0) * r0, u0, v1, nx, 0, nz);
            vert(Math.cos(a1) * r1, TIER1_Y, Math.sin(a1) * r1, u1, v1, nx, 0, nz);
            vert(Math.cos(a1) * r1, 0, Math.sin(a1) * r1, u1, v0, nx, 0, nz);
        }
        // ---- four striped walls
        int WS = 40;
        for (int w = 0; w < 4; w++) {
            double c = Math.toRadians(45 + 90 * w), h = Math.toRadians(WALL_HALF_DEG);
            for (int i = 0; i < WS; i++) {
                double a0 = c - h + 2 * h * i / WS, a1 = c - h + 2 * h * (i + 1) / WS;
                double cu0 = 512.0 * i / WS, cu1 = 512.0 * (i + 1) / WS;
                double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                double am = (a0 + a1) / 2;
                // inner face (stripes + badge), facing the centre
                vert(c0 * WALL_IN, LIP_Y, s0 * WALL_IN, cu0, 318, -Math.cos(am), 0, -Math.sin(am));
                vert(c0 * WALL_IN, WALL_TOP, s0 * WALL_IN, cu0, 258, -Math.cos(am), 0, -Math.sin(am));
                vert(c1 * WALL_IN, WALL_TOP, s1 * WALL_IN, cu1, 258, -Math.cos(am), 0, -Math.sin(am));
                vert(c1 * WALL_IN, LIP_Y, s1 * WALL_IN, cu1, 318, -Math.cos(am), 0, -Math.sin(am));
                // top
                double tu0 = 512.0 * i / WS, tu1 = 512.0 * (i + 1) / WS;
                vert(c0 * WALL_IN, WALL_TOP, s0 * WALL_IN, tu0 * 0.5 + 256, 150, 0, 1, 0);
                vert(c0 * WALL_OUT, WALL_TOP, s0 * WALL_OUT, tu0 * 0.5 + 256, 160, 0, 1, 0);
                vert(c1 * WALL_OUT, WALL_TOP, s1 * WALL_OUT, tu1 * 0.5 + 256, 160, 0, 1, 0);
                vert(c1 * WALL_IN, WALL_TOP, s1 * WALL_IN, tu1 * 0.5 + 256, 150, 0, 1, 0);
                // outer face
                vert(c0 * WALL_OUT, LIP_Y, s0 * WALL_OUT, tu0 * 0.5 + 256, 240, Math.cos(am), 0, Math.sin(am));
                vert(c0 * WALL_OUT, WALL_TOP, s0 * WALL_OUT, tu0 * 0.5 + 256, 200, Math.cos(am), 0, Math.sin(am));
                vert(c1 * WALL_OUT, WALL_TOP, s1 * WALL_OUT, tu1 * 0.5 + 256, 200, Math.cos(am), 0, Math.sin(am));
                vert(c1 * WALL_OUT, LIP_Y, s1 * WALL_OUT, tu1 * 0.5 + 256, 240, Math.cos(am), 0, Math.sin(am));
            }
            // end caps
            for (double a : new double[]{c - h, c + h}) {
                double ca = Math.cos(a), sa = Math.sin(a);
                double tx = -sa * (a < c ? -1 : 1), tz = ca * (a < c ? -1 : 1);
                vert(ca * WALL_IN, LIP_Y, sa * WALL_IN, 300, 240, tx, 0, tz);
                vert(ca * WALL_IN, WALL_TOP, sa * WALL_IN, 300, 200, tx, 0, tz);
                vert(ca * WALL_OUT, WALL_TOP, sa * WALL_OUT, 330, 200, tx, 0, tz);
                vert(ca * WALL_OUT, LIP_Y, sa * WALL_OUT, 330, 240, tx, 0, tz);
            }
        }
        float[] out = new float[V.size() * 8];
        for (int i = 0; i < V.size(); i++) System.arraycopy(V.get(i), 0, out, i * 8, 8);
        V.clear();
        return out;
    }
}
