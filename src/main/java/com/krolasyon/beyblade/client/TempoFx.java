package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.TempoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static com.krolasyon.beyblade.entity.TempoEntity.*;

/** Twisted Tempo's clockwork effects: pale silver-blue dials, clock hands and time ripples. Pose is at the beyblade. */
public final class TempoFx {
    private TempoFx() {}

    public static final ResourceLocation CLOCK = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/clock.png");
    private static final float[] SILVER = {0.75F, 0.85F, 1F};
    private static final float[] ICE = {0.45F, 0.75F, 1F};
    private static final float[] GOLD = {1F, 0.85F, 0.45F};
    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    /** flat clock dial of radius r at height y, turned by rot (radians) */
    private static void dial(MultiBufferSource buf, PoseStack pose, float y, float r, float rot, float[] col) {
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotation(-rot));
        VertexConsumer vc = Fx.buf(buf, CLOCK);
        Fx.groundQuad(vc, pose.last().pose(), y, r, col);
        pose.popPose();
    }

    private static void hand(VertexConsumer vc, Matrix4f m, double angle, float len, float width, float y, float[] col) {
        Vec3 a = new Vec3(0, y, 0), b = new Vec3(Math.cos(angle) * len, y, Math.sin(angle) * len);
        Fx.beam(vc, m, a, b, width, col, k(col, 0.5F), 0.4F, 0F);
        // arrow head
        Vec3 tip = new Vec3(Math.cos(angle) * (len + 0.6), y, Math.sin(angle) * (len + 0.6));
        Fx.beam(vc, m, b.subtract(new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(width)), tip, width * 0.8F, col, col, 0.4F, 0F);
        Fx.beam(vc, m, b.add(new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(width)), tip, width * 0.8F, col, col, 0.4F, 0F);
    }

    public static void render(TempoEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        Matrix4f m = pose.last().pose();
        if (e.isShielded()) {
            // Time Stop: a huge dial on the ground, its hands frozen, ticking only once a second
            float s = 1F;
            float pulse = 0.75F + 0.25F * Mth.sin(t * 0.3F);
            dial(buf, pose, 0.06F, (float) STOP_RADIUS, 0F, k(SILVER, 0.75F * pulse));
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            double tick = Math.floor(t / 20F) * Math.PI / 30;
            hand(vc, m, -Math.PI / 2 + tick, (float) STOP_RADIUS * 0.85F, 0.25F, 0.1F, k(ICE, 0.9F));
            hand(vc, m, Math.PI / 3, (float) STOP_RADIUS * 0.55F, 0.4F, 0.11F, k(ICE, 0.9F));
            vc = Fx.bufT(buf, Fx.HEX, 0.12F * pulse * s);
            Fx.band(vc, m, (float) STOP_RADIUS, 0F, (float) STOP_RADIUS, 3F, 64, ICE, ICE, 8F, 0F, 2F, 0F);
            Fx.alpha = 1F;
            // frozen enemies: a small dial under each
            for (Entity o : e.enemiesWithin(STOP_RADIUS)) {
                pose.pushPose();
                pose.translate(Mth.lerp(partial, o.xo, o.getX()) - ex, Mth.lerp(partial, o.yo, o.getY()) - ey, Mth.lerp(partial, o.zo, o.getZ()) - ez);
                dial(buf, pose, 0.08F, 1.1F, 0F, k(ICE, 0.9F));
                pose.popPose();
            }
        }
        float rw = t - e.rewindTime;
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        if (ab == TE_REWIND || (rw >= 0 && rw < 30)) {
            // Rewind: a dial spinning backwards and blue ripples running inward
            float f = Mth.clamp(1F - Math.max(0F, rw - 22F) / 8F, 0F, 1F);
            dial(buf, pose, 0.08F, 2.6F, t * 0.35F, k(ICE, f));
            VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
            for (int i = 0; i < 3; i++) {
                float ph = ((t * 0.08F) + i / 3F) % 1F;
                float r = 3.2F * (1F - ph);
                Fx.ring(vc, m, 0.12F, Math.max(0, r - 0.5F), r, 40, BLACK, k(ICE, f * ph), 3F, 0F);
            }
        }
        float tl = t - e.tollTime;
        if (tl >= 0 && tl < 30) toll(e, tl, t, pose, buf, cam);
        if (ab == 0 || at < 0) return;
        switch (ab) {
            case TE_HANDS -> {
                float f = Mth.clamp(at / HANDS_START, 0F, 1F) * (at > HANDS_END ? Math.max(0F, 1F - (at - HANDS_END) / 4F) : 1F);
                dial(buf, pose, 0.06F, (float) HANDS_LONG + 0.8F, 0F, k(SILVER, 0.8F * f));
                VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
                hand(vc, m, minuteAngle(at), (float) HANDS_LONG, 0.55F, 0.35F, k(GOLD, 1.2F * f));
                hand(vc, m, hourAngle(at), (float) HANDS_SHORT, 0.85F, 0.3F, k(SILVER, 1.2F * f));
                // sweep trail behind the minute hand
                vc = Fx.buf(buf, Fx.WISP);
                double a0 = minuteAngle(at);
                for (int i = 0; i < 10; i++) {
                    double a1 = a0 - 0.08 * (i + 1), a2 = a0 - 0.08 * i;
                    float g = (1F - i / 10F) * 0.6F * f;
                    Fx.quad(vc, m, new float[]{0, 0.3F, 0}, new float[]{(float) (Math.cos(a2) * HANDS_LONG), 0.3F, (float) (Math.sin(a2) * HANDS_LONG)},
                            new float[]{(float) (Math.cos(a1) * HANDS_LONG), 0.3F, (float) (Math.sin(a1) * HANDS_LONG)}, new float[]{0, 0.3F, 0},
                            0F, 0F, 1F, 1F, k(GOLD, g * 0.3F), k(GOLD, g));
                }
                VertexConsumer fl = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(fl, pose, cam, 0, 0.4, 0, 1.6F * f, t * 6F, k(SILVER, f));
            }
            case TE_ACCEL -> {
                // a racing second hand ring around the top
                float f = Mth.clamp(at / 4F, 0F, 1F);
                dial(buf, pose, 0.07F, 1.6F, -t * 0.9F, k(GOLD, f));
                VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
                hand(vc, m, t * 0.9F, 1.5F, 0.15F, 0.12F, k(GOLD, f));
            }
            case TE_CLOCK -> clock(e, at, t, pose, buf, cam);
            default -> {}
        }
    }

    /** the great clock hanging over the battle, hands racing toward twelve */
    private static void clock(TempoEntity e, float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float f = easeOut((at - 2) / CLOCK_FORM) * (at > CLOCK_TOLL ? Math.max(0F, 1F - (at - CLOCK_TOLL) / 20F) : 1F);
        if (f <= 0.01F) return;
        float R = (float) CLOCK_RANGE * 0.85F * f;
        float y = (float) CLOCK_HEIGHT;
        dial(buf, pose, y, R, 0F, k(SILVER, 1.1F * f));
        dial(buf, pose, 0.06F, R, 0F, k(ICE, 0.35F * f));
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
        float p = Mth.clamp((at - CLOCK_FORM) / (CLOCK_TOLL - CLOCK_FORM), 0F, 1F);
        // the hands race round and meet at twelve when the clock strikes
        double minute = -Math.PI / 2 + (1 - p) * Math.PI * 6;
        double hour = -Math.PI / 2 + (1 - p) * Math.PI * 1.5;
        pose.pushPose();
        pose.translate(0, y + 0.05, 0);
        Matrix4f hm = pose.last().pose();
        hand(vc, hm, minute, R * 0.85F, 0.6F, 0F, k(GOLD, 1.2F * f));
        hand(vc, hm, hour, R * 0.55F, 1.0F, 0.02F, k(SILVER, 1.2F * f));
        pose.popPose();
        // a beam of time from the dial down to every enemy being aged
        if (at >= CLOCK_FORM && at < CLOCK_TOLL) {
            vc = Fx.buf(buf, Fx.WISP);
            for (Entity o : e.enemiesWithin(CLOCK_RANGE)) {
                Vec3 rel = o.position().subtract(e.position());
                Fx.beam(vc, m, new Vec3(rel.x * 0.4, y, rel.z * 0.4), rel.add(0, 0.4, 0), 0.5F, k(ICE, 0.7F * f), k(SILVER, 0.3F * f), 0.5F, -t * 0.2F);
            }
            vc = Fx.buf(buf, Fx.WAVE);
            float r = R * (1F - ((t * 0.04F) % 1F));
            Fx.ring(vc, m, 0.1F, Math.max(0, r - 1.2F), r, 64, BLACK, k(ICE, 0.8F * f), 6F, 0F);
        }
    }

    private static void toll(TempoEntity e, float tl, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float f = 1F - tl / 30F;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        float r = 0.5F + easeOut(tl / 14F) * (float) CLOCK_RANGE * 1.2F;
        Fx.ring(vc, m, 0.1F, r - 2F, r, 72, BLACK, k(SILVER, 1.6F * f), 8F, 0F);
        if (tl < 12) {
            float g = 1F - tl / 12F;
            vc = Fx.buf(buf, Fx.SOFT);
            for (Entity o : e.enemiesWithin(CLOCK_RANGE + 2)) {
                Vec3 rel = o.position().subtract(e.position());
                Fx.beam(vc, m, rel.add(0, CLOCK_HEIGHT, 0), rel, 1.4F * g, k(SILVER, 1.4F * g), k(ICE, g), 0.3F, 0F);
            }
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, CLOCK_HEIGHT, 0, 14F * g, t * 2F, k(SILVER, g));
        }
    }
}
