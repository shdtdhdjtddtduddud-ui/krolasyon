package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.EagleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static com.krolasyon.beyblade.entity.EagleEntity.*;

/** Earth Eagle special-move effects: violet light, silver blade feathers and a giant spectral eagle. */
public final class EagleFx {
    private EagleFx() {}

    public static final ResourceLocation EAGLE = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/eagle.png");
    public static final ResourceLocation FEATHER = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/feather.png");
    public static final ResourceLocation CLAW = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/claw.png");

    private static final float[] VIOLET = {0.65F, 0.35F, 1F};
    private static final float[] SILVER = {0.85F, 0.85F, 1F};
    private static final float[] GOLD = {1F, 0.8F, 0.3F};
    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    /** the spectral eagle: solid violet silhouette with a gold glow; flap squashes the wings */
    private static void eagle(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float strength, float flap) {
        float a = Mth.clamp(strength, 0F, 1F);
        if (a <= 0.01F || size <= 0.01F) return;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(cam);
        pose.scale(1F + 0.12F * flap, 1F - 0.18F * flap, 1F);
        VertexConsumer vc = Fx.bufT(buf, EAGLE, 0.88F * a);
        Fx.billboard(vc, pose, new Quaternionf(), 0, 0, 0, size, 0F, new float[]{0.55F, 0.25F, 0.95F});
        Fx.alpha = 1F;
        vc = Fx.buf(buf, EAGLE);
        Fx.billboard(vc, pose, new Quaternionf(), 0, 0, 0, size * 1.05F, 0F, k(GOLD, 0.45F * a));
        pose.popPose();
    }

    /** a blade feather lying along dir (crossed horizontal + vertical planes so it reads from every angle) */
    private static void feather(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 dir, float len, float w, float[] col) {
        Vec3 d = dir.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = d.cross(up);
        if (side.lengthSqr() < 1.0E-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 tip = c.add(d.scale(len / 2)), back = c.subtract(d.scale(len / 2));
        for (Vec3 s : new Vec3[]{side.scale(w / 2), up.scale(w / 2)}) {
            Fx.quad(vc, m, f(tip.subtract(s)), f(back.subtract(s)), f(back.add(s)), f(tip.add(s)), 0F, 0F, 1F, 1F, col, col);
        }
    }

    private static float[] f(Vec3 v) { return new float[]{(float) v.x, (float) v.y, (float) v.z}; }

    public static void render(EagleEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        if (e.isShielded()) eye(e, t, pose, buf, cam);
        float da = t - e.dodgeTime;
        if (da >= 0 && da < 10) dodge(e, da, ex, ey, ez, pose, buf);
        float sa = t - e.slamTime;
        if (sa >= 0 && sa < 20) slam(e, sa, t, partial, ex, ey, ez, pose, buf, cam);
        float sc = t - e.screechTime;
        if (sc >= 0 && sc < 24) screech(sc, pose, buf, cam);
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        if (ab == 0 || at < 0) return;
        switch (ab) {
            case EA_FEATHER -> featherStorm(at, t, pose, buf, cam);
            case EA_TALON -> talon(e, at, t, pose, buf, cam);
            case EA_WING -> wings(at, t, pose, buf);
            case EA_SKY -> sky(e, at, t, partial, ex, ey, ez, pose, buf, cam);
            default -> {}
        }
    }

    // ================= EAGLE EYE =================
    private static void eye(EagleEntity e, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        int rem = e.shieldRemaining();
        float fade = rem < 14 ? rem / 14F : 1F;
        float grow = easeOut((EYE_TICKS - rem) / 8F);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, FEATHER);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + t * 0.12;
            Vec3 c = new Vec3(Math.cos(a) * 0.95 * grow, 0.55 + 0.1 * Math.sin(t * 0.3 + i), Math.sin(a) * 0.95 * grow);
            feather(vc, m, c, new Vec3(-Math.sin(a), 0.15, Math.cos(a)), 0.7F, 0.28F, k(SILVER, 0.9F * fade));
        }
        vc = Fx.buf(buf, Fx.SOFT);
        Fx.ring(vc, m, 0.04F, 0.6F, 1.25F, 40, BLACK, k(VIOLET, 0.8F * fade), 3F, t * 0.05F);
        eagle(buf, pose, cam, 0, 1.7 + 0.1 * Mth.sin(t * 0.2F), 0, 1.2F * grow, 0.85F * fade, Mth.sin(t * 0.4F));
    }

    private static void dodge(EagleEntity e, float da, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf) {
        float f = 1F - da / 10F;
        pose.pushPose();
        pose.translate(e.dodgeX - ex, e.dodgeY - ey, e.dodgeZ - ez);
        pose.scale(com.krolasyon.beyblade.entity.BeybladeEntity.RADIUS, com.krolasyon.beyblade.entity.BeybladeEntity.RADIUS, com.krolasyon.beyblade.entity.BeybladeEntity.RADIUS);
        BeyMesh.EAGLE.renderGhostDisc(pose, buf, 0xF000F0, 0.5F * f, BeyMesh.EAGLE.TOP_Y - 0.2F);
        pose.popPose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        pose.pushPose();
        pose.translate(e.dodgeX - ex, e.dodgeY - ey, e.dodgeZ - ez);
        Fx.ring(vc, pose.last().pose(), 0.3F, 0.2F + da * 0.12F, 0.5F + da * 0.15F, 32, BLACK, k(VIOLET, 1.2F * f), 3F, 0F);
        pose.popPose();
    }

    // ================= FEATHER STORM =================
    private static void featherStorm(float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, FEATHER);
        if (at < F_GATHER + 2 * F_GAP + 2) {
            // ring of blade feathers orbiting, thinning out as each burst is released
            int left = 12 - Math.max(0, Math.min(3, (int) ((at - F_GATHER) / F_GAP) + (at >= F_GATHER ? 1 : 0))) * 4;
            float grow = easeOut(at / 10F);
            for (int i = 0; i < left; i++) {
                double a = i * Math.PI * 2 / 12 + t * 0.35;
                Vec3 c = new Vec3(Math.cos(a) * 1.5 * grow, 0.6 + 0.15 * Math.sin(a * 3 + t * 0.2), Math.sin(a) * 1.5 * grow);
                feather(vc, m, c, new Vec3(-Math.sin(a), 0, Math.cos(a)), 0.9F, 0.32F, k(SILVER, 1.1F));
            }
        }
        for (int b = 0; b < F_BURSTS; b++) {
            float age = at - (F_GATHER + b * F_GAP);
            if (age < 0 || age >= F_LIFE) continue;
            float front = 0.8F + age * F_SPEED;
            float fade = 1F - age / F_LIFE;
            for (int i = 0; i < F_COUNT; i++) {
                double a = i * Math.PI * 2 / F_COUNT + b * 0.2;
                Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
                Vec3 c = dir.scale(front).add(0, 0.55, 0);
                feather(vc, m, c, dir, 1.0F, 0.35F, k(new float[]{0.9F, 0.8F, 1F}, 1.3F * fade));
            }
        }
        vc = Fx.buf(buf, Fx.WAVE);
        for (int b = 0; b < F_BURSTS; b++) {
            float age = at - (F_GATHER + b * F_GAP);
            if (age < 0 || age >= F_LIFE) continue;
            float front = 0.8F + age * F_SPEED;
            Fx.ring(vc, m, 0.55F, front - 1.0F, front, 64, BLACK, k(VIOLET, 0.9F * (1F - age / F_LIFE)), 6F, b * 0.3F);
        }
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, 0, 0.55, 0, 1.6F + 0.4F * Mth.sin(t * 1.5F), t * 6F, k(VIOLET, 0.8F));
    }

    // ================= TALON GRAB =================
    private static void talon(EagleEntity e, float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float end = EA_DURATION[EA_TALON];
        float g = Mth.clamp(at / 8F, 0F, 1F) * (at > end - 8 ? Math.max(0F, (end - at) / 8F) : 1F);
        boolean carrying = t - e.grabTime < T_CARRY && e.grabTime > e.clientAbilityStart;
        eagle(buf, pose, cam, 0, 0.8, 0, 4.2F + (carrying ? 0.6F : 0F), g, Mth.sin(t * (carrying ? 0.9F : 0.5F)));
        Matrix4f m = pose.last().pose();
        if (at >= T_SWOOP) {
            Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
            if (vel.lengthSqr() > 0.01) {
                Vec3 tail = vel.normalize().scale(-5);
                VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 side = new Vec3(-vel.z, 0, vel.x);
                    side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize().scale(1.4 * s);
                    Fx.beam(vc, m, side.add(0, 0.6, 0), side.add(tail).add(0, 0.6, 0), 0.4F, k(SILVER, 0.9F * g), BLACK, 0.5F, t);
                }
            }
        }
        float ga = t - e.grabTime;
        if (ga >= 0 && ga < 12) {
            VertexConsumer vc = Fx.buf(buf, CLAW);
            float f = 1F - ga / 12F;
            Fx.billboard(vc, pose, cam, 0, -0.3, 0, 2.6F, 90F, k(VIOLET, 1.5F * f));
        }
    }

    private static void slam(EagleEntity e, float sa, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                             MultiBufferSource buf, Quaternionf cam) {
        Entity tgt = e.level().getEntity(e.getTargetId());
        if (tgt == null) return;
        float f = 1F - sa / 20F;
        pose.pushPose();
        pose.translate(Mth.lerp(partial, tgt.xo, tgt.getX()) - ex, Mth.lerp(partial, tgt.yo, tgt.getY()) - ey, Mth.lerp(partial, tgt.zo, tgt.getZ()) - ez);
        VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
        Fx.beam(vc, pose.last().pose(), new Vec3(0, 6, 0), new Vec3(0, 0.3, 0), 1.2F * f, k(VIOLET, 0.3F * f), k(SILVER, 1.1F * f), 0.4F, t);
        if (sa > 4) {
            vc = Fx.buf(buf, Fx.WAVE);
            float r = 0.5F + easeOut((sa - 4) / 10F) * 4F;
            Fx.ring(vc, pose.last().pose(), 0.06F, r - 1F, r, 48, BLACK, k(VIOLET, 1.4F * f), 4F, 0F);
        }
        pose.popPose();
    }

    // ================= METAL WING SMASH =================
    private static void wings(float at, float t, PoseStack pose, MultiBufferSource buf) {
        float open = at < W_OPEN ? easeOut(at / W_OPEN) : (at > W_CLOSE ? Math.max(0F, 1F - (at - W_CLOSE) / 8F) : 1F);
        if (open <= 0F) return;
        float R = (float) W_RADIUS * open;
        float rot = -t * 38F;   // right spin = clockwise from above
        pose.pushPose();
        pose.translate(0, 0.45, 0);
        pose.mulPose(Axis.YP.rotationDegrees(rot));
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, FEATHER);
        for (int i = 0; i < 2; i++) {
            Vec3 dir = new Vec3(i == 0 ? 1 : -1, 0, 0);
            Vec3 c = dir.scale(0.35 + R / 2);
            feather(vc, m, c, dir, R, 1.0F * open, k(SILVER, 1.25F));
            feather(vc, m, c.add(0, 0.05, 0), dir, R * 0.85F, 0.55F * open, k(VIOLET, 1.0F));
        }
        pose.popPose();
        // the blur of the sweeping blades and a ring of sparks at their tips
        vc = Fx.buf(buf, Fx.SOFT);
        m = pose.last().pose();
        Fx.ring(vc, m, 0.45F, R * 0.35F, R, 64, k(VIOLET, 0.15F * open), k(SILVER, 0.5F * open), 2F, 0F);
        vc = Fx.buf(buf, Fx.WAVE);
        Fx.ring(vc, m, 0.45F, R - 0.35F, R + 0.05F, 64, BLACK, k(SILVER, 1.0F * open), 8F, t * 0.1F);
    }

    // ================= SKY EAGLE LANCE RAIN =================
    private static void sky(EagleEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                            MultiBufferSource buf, Quaternionf cam) {
        float end = EA_DURATION[EA_SKY];
        float fade = at > end - 10 ? Math.max(0F, (end - at) / 10F) : 1F;
        double bx = e.abilityX - ex, by = e.abilityY - ey, bz = e.abilityZ - ez;
        float hY = (float) (9 + 13 * (1 - easeOut(at / S_DESCEND)));
        Vec3 sky = new Vec3(bx, by + hY, bz);
        eagle(buf, pose, cam, sky.x, sky.y, sky.z, 13F, Mth.clamp(at / 6F, 0F, 1F) * fade, Mth.sin(t * 0.25F));
        Matrix4f m = pose.last().pose();
        // shadow of the eagle's wings on the ground
        pose.pushPose();
        pose.translate(bx, by, bz);
        VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
        Fx.ring(vc, pose.last().pose(), 0.05F, 0F, (float) S_RANGE * 0.75F, 48, k(VIOLET, 0.35F * fade), BLACK, 2F, 0F);
        pose.popPose();
        for (int k = 0; k < S_LANCES; k++) {
            float spawn = S_DESCEND + k * S_STEP, impact = spawn + S_FALL;
            if (at < spawn || at > impact + 8) continue;
            Entity tgt = e.lanceTarget(k);
            Vec3 tp;
            if (tgt != null) {
                tp = new Vec3(Mth.lerp(partial, tgt.xo, tgt.getX()) - ex, Mth.lerp(partial, tgt.yo, tgt.getY()) - ey, Mth.lerp(partial, tgt.zo, tgt.getZ()) - ez);
            } else {
                double a = k * 0.9;
                tp = new Vec3(bx + Math.cos(a) * 4, by, bz + Math.sin(a) * 4);
            }
            if (at < impact) {
                float fr = (at - spawn) / S_FALL;
                Vec3 head = sky.add(tp.add(0, 0.4, 0).subtract(sky).scale(fr * fr));
                Vec3 dir = tp.subtract(sky).normalize();
                vc = Fx.buf(buf, Fx.SOFT);
                Fx.beam(vc, m, head.subtract(dir.scale(3.5)), head, 0.45F, BLACK, k(GOLD, 1.1F), 0.6F, 0F);
                vc = Fx.buf(buf, FEATHER);
                feather(vc, m, head, dir, 1.3F, 0.45F, k(new float[]{1F, 0.9F, 1F}, 1.3F));
                vc = Fx.buf(buf, Fx.WAVE);
                pose.pushPose();
                pose.translate(tp.x, tp.y, tp.z);
                Fx.ring(vc, pose.last().pose(), 0.06F, 0.7F * (1 - fr), 1.0F * (1 - fr) + 0.2F, 24, BLACK, k(VIOLET, 0.9F), 2F, 0F);
                pose.popPose();
            } else {
                float g = 1F - (at - impact) / 8F;
                vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, tp.x, tp.y + 0.4, tp.z, 2.2F * g, k * 40F, k(GOLD, g));
                vc = Fx.buf(buf, Fx.WAVE);
                pose.pushPose();
                pose.translate(tp.x, tp.y, tp.z);
                float r = 0.3F + (at - impact) * 0.25F;
                Fx.ring(vc, pose.last().pose(), 0.06F, r - 0.5F, r, 24, BLACK, k(VIOLET, 1.2F * g), 2F, 0F);
                pose.popPose();
            }
        }
    }

    private static void screech(float sc, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float f = 1F - sc / 24F;
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        float r = 0.5F + easeOut(sc / 14F) * (float) S_RANGE;
        Fx.ring(vc, pose.last().pose(), 0.5F, r - 2.0F, r, 72, BLACK, k(VIOLET, 1.5F * f), 8F, 0F);
        Fx.ring(vc, pose.last().pose(), 0.1F, r * 0.7F - 1.2F, r * 0.7F, 72, BLACK, k(GOLD, 0.9F * f), 8F, 0.4F);
        eagle(buf, pose, cam, 0, 2.8, 0, 6F * easeOut(sc / 5F), f, 1F);
    }
}
