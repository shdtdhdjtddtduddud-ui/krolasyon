package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.LDragoEntity;
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

import static com.krolasyon.beyblade.entity.LDragoEntity.*;

/** L-Drago Destruction special-move effects: crimson/black/gold dragon fire. Pose is at the beyblade, unrotated. */
public final class LDragoFx {
    private LDragoFx() {}

    public static final ResourceLocation DRAGON = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/dragon.png");
    public static final ResourceLocation CLAW = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/claw.png");

    private static final float[] CRIMSON = {1F, 0.1F, 0.22F};
    private static final float[] MAGENTA = {1F, 0.25F, 0.65F};
    private static final float[] ORANGE = {1F, 0.45F, 0.08F};
    private static final float[] GOLD = {1F, 0.78F, 0.2F};
    private static final float[] WHITE = {1F, 1F, 1F};
    private static final float[] BLACK = {0F, 0F, 0F};

    /** the L-Drago dragon emblem: solid crimson-magenta silhouette (translucent, keeps its colour on bright sky) + additive glow */
    private static void emblem(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float strength) {
        float a = Mth.clamp(strength, 0F, 1F);
        if (a <= 0.01F || size <= 0.01F) return;
        VertexConsumer vc = Fx.bufT(buf, DRAGON, 0.9F * a);
        Fx.billboard(vc, pose, cam, x, y, z, size, 0F, new float[]{0.9F, 0.06F, 0.3F});
        Fx.alpha = 1F;
        vc = Fx.buf(buf, DRAGON);
        Fx.billboard(vc, pose, cam, x, y, z, size * 1.04F, 0F, k(MAGENTA, 0.35F * a));
    }

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    public static void render(LDragoEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        if (e.isShielded()) awaken(e, t, pose, buf, cam);
        float aw = t - e.awakenTime;
        if (aw >= 0 && aw < 34) awakenBurst(aw, t, pose, buf, cam);
        claws(e, t, ex, ey, ez, pose, buf, cam);
        float ia = t - e.impactTime;
        if (ia >= 0 && ia < 34) meteorImpact(ia, t, pose, buf, cam);
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        if (ab == 0 || at < 0) return;
        double bx = e.abilityX - ex, by = e.abilityY - ey, bz = e.abilityZ - ez;
        switch (ab) {
            case DR_VORTEX -> vortex(e, at, t, partial, ex, ey, ez, pose, buf, cam);
            case DR_METEOR -> meteor(e, at, t, bx, by, bz, pose, buf, cam);
            case DR_CLAW -> {
                VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, 0, 0.35, 0, 2.2F + 0.3F * Mth.sin(t * 2F), t * 9F, k(CRIMSON, 0.8F));
            }
            case DR_ROAR -> roar(e, at, t, partial, ex, ey, ez, pose, buf, cam);
            default -> {}
        }
    }

    // ================= DRAGON EMPEROR AWAKENING (buff) =================
    private static void awaken(LDragoEntity e, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        int rem = e.shieldRemaining();
        float fade = rem < 16 ? rem / 16F : 1F;
        VertexConsumer vc = Fx.buf(buf, Fx.WISP);
        Matrix4f m = pose.last().pose();
        // twin flame serpents coiling around the top
        for (int i = 0; i < 2; i++) {
            Fx.helix(vc, m, 0.0F, 1.7F, 1.15F, 0.55F, t * 0.32F + i * Mth.PI, 1.6F, 0.45F, 36,
                    k(CRIMSON, 1.1F * fade), k(ORANGE, 0.5F * fade), -t * 0.15F);
        }
        Fx.ring(vc, m, 0.04F, 0.35F, 1.35F, 40, k(ORANGE, 0.9F * fade), k(CRIMSON, 0.1F * fade), 3F, t * 0.08F);
        float pulse = 0.8F + 0.2F * Mth.sin(t * 0.6F);
        emblem(buf, pose, cam, 0, 1.55, 0, 1.0F * pulse, 0.9F * fade);
    }

    private static void awakenBurst(float aw, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float f = 1F - aw / 34F;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        float r = 0.5F + easeOut(aw / 14F) * 5F;
        Fx.ring(vc, m, 0.1F, r - 1.2F, r, 64, BLACK, k(CRIMSON, 1.5F * f), 6F, 0F);
        float grow = easeOut(aw / 8F);
        emblem(buf, pose, cam, 0, 2.6, 0, 5.5F * grow, 1.2F * f);
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, 0, 0.4, 0, 4F * f, t * 5F, k(CRIMSON, f));
    }

    // ================= SPIN-STEAL VORTEX =================
    private static void vortex(LDragoEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                               MultiBufferSource buf, Quaternionf cam) {
        float dur = DR_DURATION[DR_VORTEX];
        float f = Mth.clamp(at / 8F, 0F, 1F) * (at > dur - 10 ? Math.max(0F, (dur - at) / 10F) : 1F);
        float R = (float) VORTEX_RADIUS * easeOut(at / 10F);
        Matrix4f m = pose.last().pose();
        // dark whirlpool floor (translucent, real colour)
        VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.6F * f);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(t * 9F));
        Fx.ring(vc, pose.last().pose(), 0.03F, 0F, R, 48, new float[]{0.08F, 0F, 0.02F}, new float[]{0.35F, 0.02F, 0.06F}, 3F, 0F);
        pose.popPose();
        Fx.alpha = 1F;
        // glowing spiral arms turning counter-clockwise
        vc = Fx.buf(buf, Fx.SOFT);
        for (int i = 0; i < 6; i++) {
            spiralArm(vc, m, 0.06F, 0.4F, R, i * Mth.TWO_PI / 6F + t * 0.22F, 0.55F, 0.35F + 0.25F * (R / (float) VORTEX_RADIUS), 30,
                    k(MAGENTA, 1.1F * f), k(CRIMSON, 0.15F * f));
        }
        vc = Fx.buf(buf, Fx.WAVE);
        Fx.ring(vc, m, 0.08F, R - 0.8F, R, 64, BLACK, k(CRIMSON, 1.1F * f), 8F, -t * 0.05F);
        // stolen spin flowing from every enemy into L-Drago
        vc = Fx.buf(buf, Fx.SOFT);
        Vec3 core = new Vec3(0, 0.45, 0);
        for (Entity en : e.enemiesWithin(VORTEX_RADIUS)) {
            Vec3 pe = new Vec3(Mth.lerp(partial, en.xo, en.getX()) - ex, Mth.lerp(partial, en.yo, en.getY()) + en.getBbHeight() * 0.5 - ey,
                    Mth.lerp(partial, en.zo, en.getZ()) - ez);
            Fx.beam(vc, m, pe, core, 0.28F * f, k(MAGENTA, 0.3F * f), k(CRIMSON, 1.1F * f), 0.9F, t * 0.7F);
        }
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, 0, 0.4, 0, 2.6F * f + 0.3F * Mth.sin(t), -t * 6F, k(CRIMSON, 0.9F * f));
    }

    /** flat glowing ribbon lying on the ground, spiralling from r0 outward to r1 */
    private static void spiralArm(VertexConsumer vc, Matrix4f m, float y, float r0, float r1, float a0, float turns, float width, int steps,
                                  float[] cIn, float[] cOut) {
        for (int i = 0; i < steps; i++) {
            float f0 = (float) i / steps, f1 = (float) (i + 1) / steps;
            float rr0 = Mth.lerp(f0, r0, r1), rr1 = Mth.lerp(f1, r0, r1);
            // counter-clockwise as it goes outward (left spin)
            float aa0 = a0 - f0 * turns * Mth.TWO_PI, aa1 = a0 - f1 * turns * Mth.TWO_PI;
            float w0 = width * (0.3F + 0.7F * f0), w1 = width * (0.3F + 0.7F * f1);
            float[] c0 = Fx.lerp(f0, cIn, cOut), c1 = Fx.lerp(f1, cIn, cOut);
            float cx0 = Mth.cos(aa0), sx0 = Mth.sin(aa0), cx1 = Mth.cos(aa1), sx1 = Mth.sin(aa1);
            float[] p0 = {cx0 * (rr0 - w0 / 2), y, sx0 * (rr0 - w0 / 2)}, p1 = {cx0 * (rr0 + w0 / 2), y, sx0 * (rr0 + w0 / 2)};
            float[] p2 = {cx1 * (rr1 + w1 / 2), y, sx1 * (rr1 + w1 / 2)}, p3 = {cx1 * (rr1 - w1 / 2), y, sx1 * (rr1 - w1 / 2)};
            Fx.v(vc, m, p0[0], p0[1], p0[2], c0[0], c0[1], c0[2], f0, 0F);
            Fx.v(vc, m, p1[0], p1[1], p1[2], c0[0], c0[1], c0[2], f0, 1F);
            Fx.v(vc, m, p2[0], p2[1], p2[2], c1[0], c1[1], c1[2], f1, 1F);
            Fx.v(vc, m, p3[0], p3[1], p3[2], c1[0], c1[1], c1[2], f1, 0F);
            Fx.v(vc, m, p3[0], p3[1], p3[2], c1[0], c1[1], c1[2], f1, 0F);
            Fx.v(vc, m, p2[0], p2[1], p2[2], c1[0], c1[1], c1[2], f1, 1F);
            Fx.v(vc, m, p1[0], p1[1], p1[2], c0[0], c0[1], c0[2], f0, 1F);
            Fx.v(vc, m, p0[0], p0[1], p0[2], c0[0], c0[1], c0[2], f0, 0F);
        }
    }

    // ================= DRAGON EMPEROR SOARING DESTRUCTION (meteor) =================
    private static void meteor(LDragoEntity e, float at, float t, double bx, double by, double bz, PoseStack pose,
                               MultiBufferSource buf, Quaternionf cam) {
        Matrix4f m = pose.last().pose();
        if (at < M_RISE + 6) {
            float f = at < M_RISE ? Mth.clamp(at / 4F, 0F, 1F) : 1F - (at - M_RISE) / 6F;
            VertexConsumer vc = Fx.buf(buf, Fx.WISP);
            Fx.ring(vc, m, 0.04F, 0F, 1.8F, 40, k(ORANGE, 1.2F * f), k(CRIMSON, 0.1F * f), 3F, t * 0.1F);
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 0.2, 0, 2.2F * f, t * 8F, k(ORANGE, f));
        }
        if (at >= M_RISE && at < M_DIVE + 2) {
            // fire pillar from the launch point up to L-Drago
            float f = at < M_DIVE ? 1F : 1F - (at - M_DIVE) / 2F;
            Vec3 base = new Vec3(bx, by, bz);
            Vec3 top = new Vec3(0, 0.3, 0);
            VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.75F * f);
            Fx.beam(vc, m, base, top, 1.6F, new float[]{1F, 0.35F, 0.05F}, new float[]{0.9F, 0.1F, 0.1F}, 0.25F, -t * 0.4F);
            Fx.alpha = 1F;
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, base, top, 0.6F, k(new float[]{1F, 0.8F, 0.4F}, 0.9F * f), k(ORANGE, 0.5F * f), 0.4F, -t * 0.8F);
        }
        if (at >= M_HANG - 4 && at < M_DIVE + 30) {
            // the flaming orb L-Drago becomes, and the dragon emblem blazing in the sky behind it
            float f = Mth.clamp((at - M_HANG + 4) / 8F, 0F, 1F);
            VertexConsumer vc = Fx.buf(buf, Fx.WISP);
            Fx.sphere(vc, m, 0.35F, 1.1F + 0.15F * Mth.sin(t * 1.4F), Mth.PI, 10, 20, k(ORANGE, 0.9F * f), 2F, 2F, t * 0.05F, -t * 0.08F);
            if (at < M_DIVE + 4) {
                float g = at < M_DIVE ? f : 1F - (at - M_DIVE) / 4F;
                vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, 0, 0.35, 0, 5F * f, t * 4F, k(ORANGE, 0.9F * g));
            }
        }
        if (at >= M_HANG - 4 && e.impactTime < e.clientAbilityStart) {
            // the Dragon Emperor itself: a huge winged dragon silhouette around L-Drago, head pointing where it flies
            float f = Mth.clamp((at - M_HANG + 4) / 6F, 0F, 1F);
            Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
            Vec3 dir = at < M_DIVE || vel.lengthSqr() < 1.0E-3 ? new Vec3(0, -1, 0) : vel.normalize();
            boolean grab = e.grabTime > e.clientAbilityStart;
            dragonSilhouette(buf, pose, cam, 0, 0.6, 0, grab ? 11F : 9F, f, dir, t);
        }
        if (at >= M_DIVE && e.impactTime < e.clientAbilityStart) {
            // meteor tail streaming back along the dive
            Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
            if (vel.lengthSqr() > 1.0E-3) {
                Vec3 tail = vel.normalize().scale(-7);
                VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.85F);
                Fx.beam(vc, m, new Vec3(0, 0.35, 0), tail, 2.2F, new float[]{1F, 0.5F, 0.1F}, new float[]{0.6F, 0.02F, 0.05F}, 0.3F, t * 0.5F);
                Fx.alpha = 1F;
                vc = Fx.buf(buf, Fx.SOFT);
                Fx.beam(vc, m, new Vec3(0, 0.35, 0), tail.scale(0.6), 0.9F, k(new float[]{1F, 0.85F, 0.5F}, 1.1F), BLACK, 0.5F, t);
            }
        }
    }

    public static final ResourceLocation DRAGON_FLIGHT = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/dragon_flight.png");

    /** camera-facing dragon sprite rotated so its head points along the given world direction on screen */
    private static void dragonSilhouette(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float a,
                                         Vec3 dir, float t) {
        if (a <= 0.01F) return;
        org.joml.Vector3f v = new org.joml.Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        new Quaternionf(cam).conjugate().transform(v);
        float rot = (float) Math.toDegrees(Math.atan2(v.y, -v.x)) - 90F;
        float flap = 1F + 0.06F * Mth.sin(t * 0.9F);
        VertexConsumer vc = Fx.bufT(buf, DRAGON_FLIGHT, 0.92F * a);
        Fx.billboard(vc, pose, cam, x, y, z, size * flap, rot, new float[]{1F, 1F, 1F});
        Fx.alpha = 1F;
        vc = Fx.buf(buf, DRAGON_FLIGHT);
        Fx.billboard(vc, pose, cam, x, y, z, size * flap * 1.05F, rot, k(ORANGE, 0.45F * a));
    }

    private static void meteorImpact(float ia, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float f = 1F - ia / 34F;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        float r = 0.5F + easeOut(ia / 16F) * (float) M_RADIUS * 1.6F;
        Fx.ring(vc, m, 0.08F, r - 2.4F, r, 72, BLACK, k(CRIMSON, 1.6F * f), 6F, 0F);
        Fx.ring(vc, m, 0.1F, r * 0.6F - 1.2F, r * 0.6F, 72, BLACK, k(ORANGE, 1.0F * f), 6F, 0.4F);
        // radial fissures glowing through the ground
        vc = Fx.buf(buf, Fx.BOLT);
        float len = Math.min(1F, ia / 5F) * 7F;
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI * 2 / 10 + (i % 2) * 0.2;
            float l = len * (0.65F + 0.35F * ((i * 7) % 5) / 4F);
            Vec3 b = new Vec3(Math.cos(a) * l, 0.05, Math.sin(a) * l);
            Fx.beamV(vc, m, new Vec3(0, 0.05, 0), b, 0.7F, k(ORANGE, 1.4F * f), 0.4F, i * 0.37F);
        }
        vc = Fx.bufT(buf, Fx.WISP, 0.85F * f);
        Fx.band(vc, m, r * 0.8F, 0F, r * 0.8F + 0.3F, 1.6F * f, 64, new float[]{1F, 0.35F, 0.05F}, new float[]{0.7F, 0.05F, 0.08F}, 8F, t * 0.1F, 1F, -t * 0.12F);
        Fx.alpha = 1F;
        if (ia < 14) {
            float g = 1F - ia / 14F;
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 0.8, 0, 11F * g, t * 2F, k(new float[]{1F, 0.6F, 0.3F}, 1.2F * g));
            emblem(buf, pose, cam, 0, 3.0, 0, 7F * easeOut(ia / 6F), 1.2F * g);
        }
    }

    // ================= DRAGON CLAW BARRAGE =================
    private static void claws(LDragoEntity e, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        VertexConsumer vc = null;
        for (int i = 0; i < e.clawTimes.length; i++) {
            float ca = t - e.clawTimes[i];
            if (ca < 0 || ca >= 12) continue;
            if (vc == null) vc = Fx.buf(buf, CLAW);
            float f = 1F - ca / 12F;
            float grow = easeOut(ca / 3F);
            double x = e.clawPos[i][0] - ex, y = e.clawPos[i][1] - ey, z = e.clawPos[i][2] - ez;
            Fx.billboard(vc, pose, cam, x, y, z, 3.4F * (0.6F + 0.4F * grow), e.clawRot[i], k(CRIMSON, 1.5F * f));
            Fx.billboard(vc, pose, cam, x, y, z, 2.6F * (0.6F + 0.4F * grow), e.clawRot[i], k(WHITE, 0.7F * f * f));
        }
    }

    // ================= DESTRUCTION DRAGON ROAR (fire breath) =================
    private static void roar(LDragoEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                             MultiBufferSource buf, Quaternionf cam) {
        Entity tgt = e.level().getEntity(e.getTargetId());
        if (tgt == null) return;
        Vec3 fromW = new Vec3(ex, ey + 0.4, ez);
        Vec3 toW = new Vec3(Mth.lerp(partial, tgt.xo, tgt.getX()), Mth.lerp(partial, tgt.yo, tgt.getY()) + tgt.getBbHeight() * 0.5,
                Mth.lerp(partial, tgt.zo, tgt.getZ()));
        Vec3 dir = breathDir(Math.max(at, R_BREATH), fromW, toW);
        Vec3 from = new Vec3(0, 0.4, 0);
        Vec3 mouth = from.add(dir.scale(0.7));
        Matrix4f m = pose.last().pose();
        // the dragon looms behind L-Drago
        float in = easeOut(at / 14F), out = at > R_END ? Math.max(0F, 1F - (at - R_END) / 10F) : 1F;
        Vec3 behind = from.add(dir.scale(-1.2)).add(0, 2.0, 0);
        emblem(buf, pose, cam, behind.x, behind.y, behind.z, 6.5F * in, 1.0F * in * out);
        VertexConsumer vc;
        if (at < R_BREATH) {
            float ch = at / R_BREATH;
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, mouth.x, mouth.y, mouth.z, 0.5F + 2.2F * ch, t * 7F, k(ORANGE, 0.6F + 0.6F * ch));
            vc = Fx.buf(buf, Fx.WAVE);
            for (int i = 0; i < 3; i++) {
                float f = (at * 0.15F + i / 3F) % 1F;
                Fx.ringAround(vc, m, mouth, dir, 2.2F * (1F - f), 2.2F * (1F - f) + 0.3F, 32, k(ORANGE, f), 0F);
            }
            return;
        }
        float ff = at < R_BREATH + 3 ? (at - R_BREATH) / 3F : out * (at > R_END - 4 ? Math.max(0F, (R_END - at) / 4F + 0.3F) : 1F);
        float L = (float) R_LENGTH * Mth.clamp((at - R_BREATH) / 5F, 0F, 1F);
        float tan = (float) Math.tan(R_HALF_ANGLE);
        // translucent fire cone + additive inner core
        vc = Fx.bufT(buf, Fx.WISP, 0.8F * ff);
        cone(vc, m, mouth, dir, L, tan, 28, 8, new float[]{1F, 0.85F, 0.35F}, new float[]{0.85F, 0.12F, 0.04F}, -t * 0.25F);
        Fx.alpha = 1F;
        vc = Fx.buf(buf, Fx.WISP);
        cone(vc, m, mouth, dir, L * 0.8F, tan * 0.45F, 20, 6, k(new float[]{1F, 0.75F, 0.3F}, 1.0F * ff), k(ORANGE, 0.2F * ff), -t * 0.4F);
        vc = Fx.buf(buf, Fx.WAVE);
        for (int i = 0; i < 7; i++) {
            float s = ((at - R_BREATH) * 0.9F + i * L / 7F) % Math.max(0.5F, L);
            float r = 0.25F + s * tan;
            Fx.ringAround(vc, m, mouth.add(dir.scale(s)), dir, r * 0.55F, r, 32, k(ORANGE, ff * (1.1F - s / Math.max(0.5F, L))), 0F);
        }
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, mouth.x, mouth.y, mouth.z, 2.4F * ff, t * 9F, k(new float[]{1F, 0.8F, 0.5F}, ff));
    }

    /** open cone along axis from apex: radius = tan * distance; u around, v along */
    private static void cone(VertexConsumer vc, Matrix4f m, Vec3 apex, Vec3 axis, float length, float tan, int segs, int rings,
                             float[] cNear, float[] cFar, float vOff) {
        Vec3 an = axis.normalize();
        Vec3 up = Math.abs(an.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 e1 = an.cross(up).normalize(), e2 = an.cross(e1).normalize();
        for (int j = 0; j < rings; j++) {
            float s0 = length * j / rings, s1 = length * (j + 1) / rings;
            float r0 = 0.15F + s0 * tan, r1 = 0.15F + s1 * tan;
            float[] c0 = Fx.lerp((float) j / rings, cNear, cFar), c1 = Fx.lerp((float) (j + 1) / rings, cNear, cFar);
            for (int i = 0; i < segs; i++) {
                double a0 = i * Math.PI * 2 / segs, a1 = (i + 1) * Math.PI * 2 / segs;
                Vec3 d0 = e1.scale(Math.cos(a0)).add(e2.scale(Math.sin(a0))), d1 = e1.scale(Math.cos(a1)).add(e2.scale(Math.sin(a1)));
                Vec3 p0 = apex.add(an.scale(s0)).add(d0.scale(r0)), p1 = apex.add(an.scale(s1)).add(d0.scale(r1));
                Vec3 p2 = apex.add(an.scale(s1)).add(d1.scale(r1)), p3 = apex.add(an.scale(s0)).add(d1.scale(r0));
                float u0 = 2F * i / segs, u1 = 2F * (i + 1) / segs, v0 = vOff + (float) j / rings * 2F, v1 = vOff + (float) (j + 1) / rings * 2F;
                Fx.quad(vc, m, f(p0), f(p1), f(p2), f(p3), u0, v0, u1, v1, c0, c1);
            }
        }
    }

    private static float[] f(Vec3 v) { return new float[]{(float) v.x, (float) v.y, (float) v.z}; }
}
