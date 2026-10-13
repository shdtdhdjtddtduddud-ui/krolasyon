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
    private static final ResourceLocation TEX = new ResourceLocation(LibraBeyblade.MODID, "textures/entity/stadium.png");
    private static final int SEGS = 96;
    private static final float TIER2_Y = 0.85F, TIER1_Y = 0.45F, APOTHEM2 = 8.9F, APOTHEM1 = 9.6F, SLOPE_R = 8.6F;
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
            vc.vertex(m, d[i], d[i + 1], d[i + 2]).color(1F, 1F, 1F, 1F).uv(d[i + 3], d[i + 4]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(n, d[i + 5], d[i + 6], d[i + 7]).endVertex();
        }
        super.render(e, yaw, partial, pose, buf, light);
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
        int RINGS = 24;
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
        int WS = 24;
        for (int w = 0; w < 4; w++) {
            double c = Math.toRadians(45 + 90 * w), h = Math.toRadians(WALL_HALF_DEG);
            for (int i = 0; i < WS; i++) {
                double a0 = c - h + 2 * h * i / WS, a1 = c - h + 2 * h * (i + 1) / WS;
                double cu0 = 512.0 * (WS - i) / WS, cu1 = 512.0 * (WS - i - 1) / WS;   // reversed so the badge text reads from inside
                double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                double am = (a0 + a1) / 2;
                // inner face (stripes + badge), facing the centre
                vert(c0 * WALL_IN, LIP_Y, s0 * WALL_IN, cu0, 318, -Math.cos(am), 0, -Math.sin(am));
                vert(c0 * WALL_IN, WALL_TOP, s0 * WALL_IN, cu0, 258, -Math.cos(am), 0, -Math.sin(am));
                vert(c1 * WALL_IN, WALL_TOP, s1 * WALL_IN, cu1, 258, -Math.cos(am), 0, -Math.sin(am));
                vert(c1 * WALL_IN, LIP_Y, s1 * WALL_IN, cu1, 318, -Math.cos(am), 0, -Math.sin(am));
                // top
                double tu0 = 512.0 * i / WS, tu1 = 512.0 * (i + 1) / WS;
                vert(c0 * WALL_IN, WALL_TOP, s0 * WALL_IN, tu0, 330, 0, 1, 0);
                vert(c0 * WALL_OUT, WALL_TOP, s0 * WALL_OUT, tu0, 340, 0, 1, 0);
                vert(c1 * WALL_OUT, WALL_TOP, s1 * WALL_OUT, tu1, 340, 0, 1, 0);
                vert(c1 * WALL_IN, WALL_TOP, s1 * WALL_IN, tu1, 330, 0, 1, 0);
                // outer face
                vert(c0 * WALL_OUT, LIP_Y, s0 * WALL_OUT, tu0, 380, Math.cos(am), 0, Math.sin(am));
                vert(c0 * WALL_OUT, WALL_TOP, s0 * WALL_OUT, tu0, 345, Math.cos(am), 0, Math.sin(am));
                vert(c1 * WALL_OUT, WALL_TOP, s1 * WALL_OUT, tu1, 345, Math.cos(am), 0, Math.sin(am));
                vert(c1 * WALL_OUT, LIP_Y, s1 * WALL_OUT, tu1, 380, Math.cos(am), 0, Math.sin(am));
            }
            // end caps
            for (double a : new double[]{c - h, c + h}) {
                double ca = Math.cos(a), sa = Math.sin(a);
                double tx = -sa * (a < c ? -1 : 1), tz = ca * (a < c ? -1 : 1);
                vert(ca * WALL_IN, LIP_Y, sa * WALL_IN, 10, 380, tx, 0, tz);
                vert(ca * WALL_IN, WALL_TOP, sa * WALL_IN, 10, 330, tx, 0, tz);
                vert(ca * WALL_OUT, WALL_TOP, sa * WALL_OUT, 40, 330, tx, 0, tz);
                vert(ca * WALL_OUT, LIP_Y, sa * WALL_OUT, 40, 380, tx, 0, tz);
            }
        }
        float[] out = new float[V.size() * 8];
        for (int i = 0; i < V.size(); i++) System.arraycopy(V.get(i), 0, out, i * 8, 8);
        V.clear();
        return out;
    }
}
