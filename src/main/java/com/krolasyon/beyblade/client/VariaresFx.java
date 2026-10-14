package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.VariaresEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static com.krolasyon.beyblade.entity.VariaresEntity.*;

/** Variares: crimson war aura, lightning sword cuts, war shield, orbiting blades and the giant falling sword. */
public final class VariaresFx {
    private VariaresFx() {}

    public static final ResourceLocation SWORD = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/sword.png");
    private static final float[] CRIMSON = {1F, 0.12F, 0.12F};
    private static final float[] GOLD = {1F, 0.8F, 0.3F};
    private static final float[] VOLT = {1F, 0.95F, 0.6F};
    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    /** vertical sword quad facing the camera around the vertical axis, tip at (x, tipY, z), length len */
    private static void sword(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double tipY, double z, float len, float a) {
        if (a <= 0.01F) return;
        // face the camera horizontally: take the camera's right vector projected on the ground
        org.joml.Vector3f right = new org.joml.Vector3f(1, 0, 0);
        cam.transform(right);
        Vec3 r = new Vec3(right.x(), 0, right.z());
        if (r.lengthSqr() < 1.0E-4) r = new Vec3(1, 0, 0);
        r = r.normalize().scale(len / 8F);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.bufT(buf, SWORD, a);
        float[] w = {1F, 1F, 1F};
        float[] p0 = {(float) (x - r.x), (float) (tipY + len), (float) (z - r.z)};
        float[] p1 = {(float) (x - r.x), (float) tipY, (float) (z - r.z)};
        float[] p2 = {(float) (x + r.x), (float) tipY, (float) (z + r.z)};
        float[] p3 = {(float) (x + r.x), (float) (tipY + len), (float) (z + r.z)};
        Fx.quad(vc, m, p0, p1, p2, p3, 0F, 0F, 1F, 1F, w, w);
        Fx.alpha = 1F;
    }

    public static void render(VariaresEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        Matrix4f m = pose.last().pose();
        if (e.isShielded()) {
            // War God's Fury: crimson flame aura and a gold war ring
            float p = 0.75F + 0.25F * Mth.sin(t * 0.6F);
            VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.75F * p);
            Fx.band(vc, m, 0.55F, 0F, 0.3F, 2.6F, 24, new float[]{1F, 0.25F, 0.1F}, new float[]{0.6F, 0.02F, 0.05F}, 2F, t * 0.2F, 1.5F, -t * 0.12F);
            Fx.alpha = 1F;
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.ring(vc, m, 0.06F, 0.9F, 1.25F, 40, k(GOLD, 0.8F * p), BLACK, 1F, 0F);
            for (int i = 0; i < 3; i++)
                Fx.helix(vc, m, 0F, 2.2F, 0.8F, 0.4F, t * 0.3F + i * Mth.TWO_PI / 3, 1.2F, 0.18F, 20, k(CRIMSON, p), k(GOLD, 0.3F * p), t * 0.1F);
        }
        float sl = t - e.slashTime;
        if (sl >= 0 && sl < 9) {
            // the lightning cut: a blazing line through the target plus forked bolts
            float f = 1F - sl / 9F;
            Vec3 a = new Vec3(e.slashX0 - ex, e.slashY - ey, e.slashZ0 - ez), b = new Vec3(e.slashX1 - ex, e.slashY - ey, e.slashZ1 - ez);
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, a, b, 0.5F * f + 0.1F, k(VOLT, 1.5F * f), k(CRIMSON, 1.2F * f), 0.5F, 0F);
            vc = Fx.buf(buf, Fx.BOLT);
            Fx.beamV(vc, m, a, b, 1.1F * f, k(VOLT, 1.2F * f), 0.4F, t * 0.7F);
            vc = Fx.buf(buf, Fx.FLARE);
            Vec3 mid = a.add(b).scale(0.5);
            Fx.billboard(vc, pose, cam, mid.x, mid.y, mid.z, 2.6F * f, t * 12F, k(VOLT, f));
        }
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        float hit = t - e.swordHitTime;
        if (hit >= 0 && hit < 40) swordImpact(e, hit, t, ex, ey, ez, pose, buf, cam);
        if (ab == 0 || at < 0) return;
        switch (ab) {
            case VA_SHIELD -> {
                // crimson war shield in front, in the direction of travel
                double vx = e.getX() - e.xo, vz = e.getZ() - e.zo;
                double l = Math.sqrt(vx * vx + vz * vz);
                if (l < 1.0E-3) break;
                float ang = (float) Math.atan2(vz / l, vx / l);
                float f = Mth.clamp(at / 4F, 0F, 1F);
                VertexConsumer vc = Fx.bufT(buf, Fx.HEX, 0.65F * f);
                int N = 12;
                for (int i = 0; i < N; i++) {
                    float a0 = ang - 1.1F + 2.2F * i / N, a1 = ang - 1.1F + 2.2F * (i + 1) / N;
                    float r = 1.0F;
                    Fx.quad(vc, m, new float[]{Mth.cos(a0) * r, 0F, Mth.sin(a0) * r}, new float[]{Mth.cos(a0) * r, 1.3F, Mth.sin(a0) * r},
                            new float[]{Mth.cos(a1) * r, 1.3F, Mth.sin(a1) * r}, new float[]{Mth.cos(a1) * r, 0F, Mth.sin(a1) * r},
                            i / (float) N, 0F, (i + 1) / (float) N, 1F, CRIMSON, new float[]{1F, 0.5F, 0.3F});
                }
                Fx.alpha = 1F;
            }
            case VA_BLADES -> {
                float f = Mth.clamp(at / 6F, 0F, 1F) * Mth.clamp((VA_DURATION[VA_BLADES] - at) / 6F, 0F, 1F);
                VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
                for (int i = 0; i < BLADES_N; i++) {
                    double a = t * 0.55 * e.spinDir() + i * Math.PI * 2 / BLADES_N;
                    Vec3 c = new Vec3(Math.cos(a) * BLADES_R, 0.45, Math.sin(a) * BLADES_R);
                    Vec3 tan = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(e.spinDir());
                    // a curved crimson blade with a golden edge, trailing its path
                    Fx.beam(vc, m, c.subtract(tan.scale(0.9)), c.add(tan.scale(0.6)), 0.35F * f, k(CRIMSON, 1.3F * f), k(GOLD, 1.3F * f), 0.5F, 0F);
                    for (int s = 1; s <= 5; s++) {
                        double a2 = a - 0.12 * s * e.spinDir();
                        Vec3 c2 = new Vec3(Math.cos(a2) * BLADES_R, 0.45, Math.sin(a2) * BLADES_R);
                        double a3 = a - 0.12 * (s - 1) * e.spinDir();
                        Vec3 c3 = new Vec3(Math.cos(a3) * BLADES_R, 0.45, Math.sin(a3) * BLADES_R);
                        Fx.beam(vc, m, c3, c2, 0.3F * f, k(CRIMSON, 0.6F * f * (1 - s / 6F)), k(CRIMSON, 0.5F * f * (1 - s / 6F)), 0.5F, 0F);
                    }
                }
                vc = Fx.buf(buf, Fx.WAVE);
                Fx.ring(vc, m, 0.08F, (float) BLADES_R - 0.3F, (float) BLADES_R + 0.3F, 48, BLACK, k(CRIMSON, 0.7F * f), 4F, t * 0.05F);
            }
            case VA_FINAL -> {
                if (e.swordTime < e.clientAbilityStart) break;
                double sx = e.swordX - ex, sy = e.swordY - ey, sz = e.swordZ - ez;
                float form = easeOut(at / FINAL_FORM);
                float tip;
                if (at < FINAL_FORM) tip = (float) SWORD_HEIGHT;
                else if (at < FINAL_FALL) {
                    float k = (at - FINAL_FORM) / (FINAL_FALL - FINAL_FORM);
                    tip = (float) SWORD_HEIGHT * (1F - k * k);
                } else tip = 0F;
                if (at < FINAL_FALL + 2) {
                    sword(buf, pose, cam, sx, sy + tip - 0.5, sz, 9F, form);
                    // lightning gathering around the blade
                    VertexConsumer vc = Fx.buf(buf, Fx.BOLT);
                    for (int i = 0; i < 3; i++) {
                        double a = t * 0.3 + i * 2.1;
                        Vec3 p = new Vec3(sx + Math.cos(a) * 1.4, sy + tip + 2 + i * 2, sz + Math.sin(a) * 1.4);
                        Fx.beamV(vc, m, p, new Vec3(sx, sy + tip + 1 + i * 2.5, sz), 0.7F * form, k(VOLT, form), 0.4F, t * 0.6F + i);
                    }
                    vc = Fx.buf(buf, Fx.WAVE);
                    // a target circle on the ground where it will land
                    pose.pushPose();
                    pose.translate(sx, sy + 0.1, sz);
                    Fx.ring(vc, pose.last().pose(), 0F, 2.6F - form, 3F, 48, BLACK, k(CRIMSON, 1.2F * form), 5F, t * 0.04F);
                    pose.popPose();
                }
            }
            default -> {}
        }
    }

    private static void swordImpact(VariaresEntity e, float hit, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float f = 1F - hit / 40F;
        double sx = e.swordX - ex, sy = e.swordY - ey, sz = e.swordZ - ez;
        // the sword stays buried in the ground for a moment, then fades
        sword(buf, pose, cam, sx, sy - 1.5, sz, 9F, f);
        pose.pushPose();
        pose.translate(sx, sy + 0.1, sz);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        float r = 0.5F + easeOut(hit / 14F) * 9F;
        Fx.ring(vc, m, 0.05F, r - 2F, r, 72, BLACK, k(CRIMSON, 1.6F * f), 6F, 0F);
        Fx.ring(vc, m, 0.08F, r * 0.6F - 1F, r * 0.6F, 72, BLACK, k(VOLT, 1.2F * f), 6F, 0.3F);
        vc = Fx.buf(buf, Fx.BOLT);
        float len = Math.min(1F, hit / 4F) * 6.5F;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + 0.3;
            Fx.beamV(vc, m, new Vec3(0, 0.05, 0), new Vec3(Math.cos(a) * len, 0.05, Math.sin(a) * len), 0.8F, k(VOLT, 1.3F * f), 0.4F, i * 0.3F);
        }
        if (hit < 12) {
            float g = 1F - hit / 12F;
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 1.2, 0, 13F * g, t * 2F, k(new float[]{1F, 0.6F, 0.4F}, 1.3F * g));
        }
        pose.popPose();
    }
}
