package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Additive, full-bright effect geometry (RenderType.eyes). All shapes are emitted double sided. Colours are
 * intensities: additive blending ignores alpha, so fading = scaling the colour.
 */
public final class Fx {
    private Fx() {}

    public static final ResourceLocation SOFT = tex("soft"), WAVE = tex("wave"), HEX = tex("hex"), WISP = tex("wisp"),
            FLARE = tex("flare"), SIGIL = tex("sigil"), BOLT = tex("bolt");

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(LibraBeyblade.MODID, "textures/fx/" + n + ".png");
    }

    /** vertex alpha, only meaningful for translucent buffers (see {@link #bufT}); reset to 1 after use */
    public static float alpha = 1F;

    public static VertexConsumer buf(MultiBufferSource b, ResourceLocation tex) {
        alpha = 1F;
        return b.getBuffer(RenderType.eyes(tex));
    }

    /** translucent, full-bright: keeps its real colour against bright sky (additive would wash out to white) */
    public static VertexConsumer bufT(MultiBufferSource b, ResourceLocation tex, float a) {
        alpha = Mth.clamp(a, 0F, 1F);
        return b.getBuffer(RenderType.entityTranslucentEmissive(tex));
    }

    public static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float u, float v) {
        vc.vertex(m, x, y, z).color(clamp(r), clamp(g), clamp(b), alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(0F, 1F, 0F).endVertex();
    }

    private static float clamp(float c) { return Mth.clamp(c, 0F, 1F); }

    /** quad p0..p3 with uv (u0,v0)-(u1,v1), both windings; c0 for p0/p3 and c1 for p1/p2 */
    public static void quad(VertexConsumer vc, Matrix4f m, float[] p0, float[] p1, float[] p2, float[] p3,
                            float u0, float v0, float u1, float v1, float[] c0, float[] c1) {
        v(vc, m, p0[0], p0[1], p0[2], c0[0], c0[1], c0[2], u0, v0);
        v(vc, m, p1[0], p1[1], p1[2], c1[0], c1[1], c1[2], u0, v1);
        v(vc, m, p2[0], p2[1], p2[2], c1[0], c1[1], c1[2], u1, v1);
        v(vc, m, p3[0], p3[1], p3[2], c0[0], c0[1], c0[2], u1, v0);
        v(vc, m, p3[0], p3[1], p3[2], c0[0], c0[1], c0[2], u1, v0);
        v(vc, m, p2[0], p2[1], p2[2], c1[0], c1[1], c1[2], u1, v1);
        v(vc, m, p1[0], p1[1], p1[2], c1[0], c1[1], c1[2], u0, v1);
        v(vc, m, p0[0], p0[1], p0[2], c0[0], c0[1], c0[2], u0, v0);
    }

    public static float[] c(float r, float g, float b, float k) { return new float[]{r * k, g * k, b * k}; }

    /** flat annulus in the XZ plane at height y; v = 0 on the inner edge, 1 on the outer edge */
    public static void ring(VertexConsumer vc, Matrix4f m, float y, float r0, float r1, int segs, float[] col, float uRepeat, float uOff) {
        ring(vc, m, y, r0, r1, segs, col, col, uRepeat, uOff);
    }

    public static void ring(VertexConsumer vc, Matrix4f m, float y, float r0, float r1, int segs, float[] cIn, float[] cOut, float uRepeat, float uOff) {
        if (r1 <= 0) return;
        r0 = Math.max(0, r0);
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * Math.PI * 2 / segs), a1 = (float) ((i + 1) * Math.PI * 2 / segs);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float u0 = uOff + uRepeat * i / segs, u1 = uOff + uRepeat * (i + 1) / segs;
            quad(vc, m, new float[]{c0 * r0, y, s0 * r0}, new float[]{c0 * r1, y, s0 * r1}, new float[]{c1 * r1, y, s1 * r1},
                    new float[]{c1 * r0, y, s1 * r0}, u0, 0F, u1, 1F, cIn, cOut);
        }
    }

    /** vertical band (cylinder/cone wall): radius r0 at y0 to r1 at y1; v = 0 at y0, 1 at y1 */
    public static void band(VertexConsumer vc, Matrix4f m, float r0, float y0, float r1, float y1, int segs, float[] cBot, float[] cTop,
                            float uRepeat, float uOff, float vRepeat, float vOff) {
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * Math.PI * 2 / segs), a1 = (float) ((i + 1) * Math.PI * 2 / segs);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float u0 = uOff + uRepeat * i / segs, u1 = uOff + uRepeat * (i + 1) / segs;
            quad(vc, m, new float[]{c0 * r0, y0, s0 * r0}, new float[]{c0 * r1, y1, s0 * r1}, new float[]{c1 * r1, y1, s1 * r1},
                    new float[]{c1 * r0, y0, s1 * r0}, u0, vOff, u1, vOff + vRepeat, cBot, cTop);
        }
    }

    /** sphere (or a cap of it: latitude from 0 = north pole to latMax radians) centred at (0, cy, 0) */
    public static void sphere(VertexConsumer vc, Matrix4f m, float cy, float r, float latMax, int lat, int lon, float[] col,
                              float uRepeat, float vRepeat, float uOff, float vOff) {
        for (int j = 0; j < lat; j++) {
            float t0 = latMax * j / lat, t1 = latMax * (j + 1) / lat;
            float y0 = cy + r * Mth.cos(t0), y1 = cy + r * Mth.cos(t1);
            float rr0 = r * Mth.sin(t0), rr1 = r * Mth.sin(t1);
            for (int i = 0; i < lon; i++) {
                float a0 = (float) (i * Math.PI * 2 / lon), a1 = (float) ((i + 1) * Math.PI * 2 / lon);
                float u0 = uOff + uRepeat * i / lon, u1 = uOff + uRepeat * (i + 1) / lon;
                float v0 = vOff + vRepeat * j / lat, v1 = vOff + vRepeat * (j + 1) / lat;
                quad(vc, m, new float[]{Mth.cos(a0) * rr0, y0, Mth.sin(a0) * rr0}, new float[]{Mth.cos(a0) * rr1, y1, Mth.sin(a0) * rr1},
                        new float[]{Mth.cos(a1) * rr1, y1, Mth.sin(a1) * rr1}, new float[]{Mth.cos(a1) * rr0, y0, Mth.sin(a1) * rr0},
                        u0, v0, u1, v1, col, col);
            }
        }
    }

    /** two crossed ribbons from a to b; u along the beam (uPerBlock per block), v across; colour ca at a, cb at b */
    public static void beam(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float width, float[] ca, float[] cb, float uPerBlock, float uOff) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-4) return;
        Vec3 dn = d.scale(1 / len);
        Vec3 up = Math.abs(dn.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 s1 = dn.cross(up).normalize().scale(width / 2);
        Vec3 s2 = dn.cross(s1).normalize().scale(width / 2);
        float u1 = uOff + (float) len * uPerBlock;
        for (Vec3 s : new Vec3[]{s1, s2}) {
            float[] p0 = p(a.subtract(s)), p1 = p(a.add(s)), p2 = p(b.add(s)), p3 = p(b.subtract(s));
            v(vc, m, p0[0], p0[1], p0[2], ca[0], ca[1], ca[2], uOff, 0F);
            v(vc, m, p1[0], p1[1], p1[2], ca[0], ca[1], ca[2], uOff, 1F);
            v(vc, m, p2[0], p2[1], p2[2], cb[0], cb[1], cb[2], u1, 1F);
            v(vc, m, p3[0], p3[1], p3[2], cb[0], cb[1], cb[2], u1, 0F);
            v(vc, m, p3[0], p3[1], p3[2], cb[0], cb[1], cb[2], u1, 0F);
            v(vc, m, p2[0], p2[1], p2[2], cb[0], cb[1], cb[2], u1, 1F);
            v(vc, m, p1[0], p1[1], p1[2], ca[0], ca[1], ca[2], uOff, 1F);
            v(vc, m, p0[0], p0[1], p0[2], ca[0], ca[1], ca[2], uOff, 0F);
        }
    }

    /** beam whose texture runs along v (for the bolt texture): u across, v along */
    public static void beamV(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float width, float[] col, float vPerBlock, float vOff) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-4) return;
        Vec3 dn = d.scale(1 / len);
        Vec3 up = Math.abs(dn.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 s1 = dn.cross(up).normalize().scale(width / 2);
        Vec3 s2 = dn.cross(s1).normalize().scale(width / 2);
        float v1 = vOff + (float) len * vPerBlock;
        for (Vec3 s : new Vec3[]{s1, s2}) {
            float[] p0 = p(a.subtract(s)), p1 = p(b.subtract(s)), p2 = p(b.add(s)), p3 = p(a.add(s));
            v(vc, m, p0[0], p0[1], p0[2], col[0], col[1], col[2], 0F, vOff);
            v(vc, m, p1[0], p1[1], p1[2], col[0], col[1], col[2], 0F, v1);
            v(vc, m, p2[0], p2[1], p2[2], col[0], col[1], col[2], 1F, v1);
            v(vc, m, p3[0], p3[1], p3[2], col[0], col[1], col[2], 1F, vOff);
            v(vc, m, p3[0], p3[1], p3[2], col[0], col[1], col[2], 1F, vOff);
            v(vc, m, p2[0], p2[1], p2[2], col[0], col[1], col[2], 1F, v1);
            v(vc, m, p1[0], p1[1], p1[2], col[0], col[1], col[2], 0F, v1);
            v(vc, m, p0[0], p0[1], p0[2], col[0], col[1], col[2], 0F, vOff);
        }
    }

    /** ring lying in the plane perpendicular to axis, centred at c */
    public static void ringAround(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 axis, float r0, float r1, int segs, float[] col, float uOff) {
        Vec3 an = axis.normalize();
        Vec3 up = Math.abs(an.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 e1 = an.cross(up).normalize();
        Vec3 e2 = an.cross(e1).normalize();
        for (int i = 0; i < segs; i++) {
            double a0 = i * Math.PI * 2 / segs, a1 = (i + 1) * Math.PI * 2 / segs;
            Vec3 d0 = e1.scale(Math.cos(a0)).add(e2.scale(Math.sin(a0)));
            Vec3 d1 = e1.scale(Math.cos(a1)).add(e2.scale(Math.sin(a1)));
            float u0 = uOff + (float) i / segs, u1 = uOff + (float) (i + 1) / segs;
            quad(vc, m, p(c.add(d0.scale(r0))), p(c.add(d0.scale(r1))), p(c.add(d1.scale(r1))), p(c.add(d1.scale(r0))), u0, 0F, u1, 1F, col, col);
        }
    }

    /** camera-facing sprite */
    public static void billboard(VertexConsumer vc, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float rot, float[] col) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(cam);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rot));
        Matrix4f m = pose.last().pose();
        float s = size / 2;
        quad(vc, m, new float[]{-s, -s, 0}, new float[]{-s, s, 0}, new float[]{s, s, 0}, new float[]{s, -s, 0}, 1F, 1F, 0F, 0F, col, col);
        pose.popPose();
    }

    /** helical ribbon around the Y axis: angle0 at y0 sweeping `turns` turns up to y1, radius r0 -> r1, ribbon height w */
    public static void helix(VertexConsumer vc, Matrix4f m, float y0, float y1, float r0, float r1, float angle0, float turns, float w,
                             int steps, float[] cBot, float[] cTop, float uOff) {
        for (int i = 0; i < steps; i++) {
            float f0 = (float) i / steps, f1 = (float) (i + 1) / steps;
            float a0 = angle0 + f0 * turns * Mth.TWO_PI, a1 = angle0 + f1 * turns * Mth.TWO_PI;
            float rr0 = Mth.lerp(f0, r0, r1), rr1 = Mth.lerp(f1, r0, r1);
            float yy0 = Mth.lerp(f0, y0, y1), yy1 = Mth.lerp(f1, y0, y1);
            float[] col0 = lerp(f0, cBot, cTop), col1 = lerp(f1, cBot, cTop);
            float x0 = Mth.cos(a0) * rr0, z0 = Mth.sin(a0) * rr0, x1 = Mth.cos(a1) * rr1, z1 = Mth.sin(a1) * rr1;
            // ribbon spans vertically (w) at each point
            v(vc, m, x0, yy0 - w / 2, z0, col0[0], col0[1], col0[2], uOff + f0 * 3, 0F);
            v(vc, m, x0, yy0 + w / 2, z0, col0[0], col0[1], col0[2], uOff + f0 * 3, 1F);
            v(vc, m, x1, yy1 + w / 2, z1, col1[0], col1[1], col1[2], uOff + f1 * 3, 1F);
            v(vc, m, x1, yy1 - w / 2, z1, col1[0], col1[1], col1[2], uOff + f1 * 3, 0F);
            v(vc, m, x1, yy1 - w / 2, z1, col1[0], col1[1], col1[2], uOff + f1 * 3, 0F);
            v(vc, m, x1, yy1 + w / 2, z1, col1[0], col1[1], col1[2], uOff + f1 * 3, 1F);
            v(vc, m, x0, yy0 + w / 2, z0, col0[0], col0[1], col0[2], uOff + f0 * 3, 1F);
            v(vc, m, x0, yy0 - w / 2, z0, col0[0], col0[1], col0[2], uOff + f0 * 3, 0F);
        }
    }

    /** flat textured square on the XZ plane (sigils) */
    public static void groundQuad(VertexConsumer vc, Matrix4f m, float y, float half, float[] col) {
        quad(vc, m, new float[]{-half, y, -half}, new float[]{-half, y, half}, new float[]{half, y, half}, new float[]{half, y, -half},
                0F, 0F, 1F, 1F, col, col);
    }

    public static float[] lerp(float f, float[] a, float[] b) {
        return new float[]{Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2])};
    }

    private static float[] p(Vec3 v) { return new float[]{(float) v.x, (float) v.y, (float) v.z}; }
}
