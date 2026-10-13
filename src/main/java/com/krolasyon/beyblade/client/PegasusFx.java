package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.PegasusEntity;
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

import static com.krolasyon.beyblade.entity.PegasusEntity.*;

/** Storm Pegasus special-move effects: sky blue wind, lightning, stars and a spirit pegasus. */
public final class PegasusFx {
    private PegasusFx() {}

    public static final ResourceLocation PEGASUS = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/pegasus.png");
    public static final ResourceLocation STAR = new ResourceLocation(LibraBeyblade.MODID, "textures/fx/star.png");

    private static final float[] SKY = {0.4F, 0.75F, 1F};
    private static final float[] WHITE = {1F, 1F, 1F};
    private static final float[] GOLD = {1F, 0.85F, 0.4F};
    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    /** spirit pegasus: solid sky-blue silhouette + white glow */
    private static void spirit(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float strength, boolean flip) {
        float a = Mth.clamp(strength, 0F, 1F);
        if (a <= 0.01F || size <= 0.01F) return;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(cam);
        if (flip) pose.scale(-1F, 1F, 1F);
        VertexConsumer vc = Fx.bufT(buf, PEGASUS, 0.88F * a);
        Fx.billboard(vc, pose, new Quaternionf(), 0, 0, 0, size, 0F, new float[]{0.2F, 0.55F, 1F});
        Fx.alpha = 1F;
        vc = Fx.buf(buf, PEGASUS);
        Fx.billboard(vc, pose, new Quaternionf(), 0, 0, 0, size * 1.05F, 0F, k(new float[]{0.7F, 0.9F, 1F}, 0.35F * a));
        pose.popPose();
    }

    public static void render(PegasusEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        if (e.isShielded()) wings(e, t, ex, ey, ez, pose, buf);
        float st = t - e.starTime;
        float nv = t - e.novaTime;
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        if (nv >= 0 && nv < 24) nova(e, nv, t, ex, ey, ez, pose, buf, cam, ab == PG_GALLOP || (ab == 0 && e.starTime < e.novaTime - 60));
        float ra = t - e.ricoTime;
        if (ra >= 0 && ra < 8) {
            VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
            float f = 1F - ra / 8F;
            Fx.ring(vc, pose.last().pose(), 0.3F, 0.2F + ra * 0.25F, 0.6F + ra * 0.3F, 32, BLACK, k(SKY, 1.3F * f), 3F, 0F);
        }
        if (ab == 0 || at < 0) return;
        switch (ab) {
            case PG_STORM -> storm(e, at, t, partial, ex, ey, ez, pose, buf, cam);
            case PG_STAR -> star(e, at, t, ex, ey, ez, pose, buf, cam);
            case PG_RICO -> {
                float g = Mth.clamp(at / 4F, 0F, 1F) * (at > PG_DURATION[PG_RICO] - 6 ? Math.max(0F, (PG_DURATION[PG_RICO] - at) / 6F) : 1F);
                double vx = e.getX() - e.xo;
                spirit(buf, pose, cam, 0, 0.9, 0, 3.0F, g, vx > 0.01);
                VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, 0, 0.4, 0, 2.0F * g, t * 8F, k(SKY, g));
            }
            case PG_GALLOP -> gallop(e, at, t, ex, ey, ez, pose, buf, cam);
            default -> {}
        }
    }

    // ================= PEGASUS WINGS =================
    private static void wings(PegasusEntity e, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf) {
        int rem = e.shieldRemaining();
        float fade = rem < 14 ? rem / 14F : 1F;
        float grow = easeOut((WINGS_TICKS - rem) / 8F);
        double vx = e.getX() - e.xo, vz = e.getZ() - e.zo;
        Vec3 fwd = vx * vx + vz * vz > 1.0E-4 ? new Vec3(vx, 0, vz).normalize() : new Vec3(0, 0, -1);
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, EagleFx.FEATHER);
        float flap = Mth.sin(t * 0.55F);
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 0; i < 6; i++) {
                double spread = Math.toRadians(-10 + i * 16 + flap * 12);
                // feathers fan out sideways and up, swept back
                Vec3 dir = side.scale(s * Math.cos(spread)).add(0, Math.sin(spread) + 0.25, 0).add(fwd.scale(-0.45)).normalize();
                float len = (1.5F - i * 0.1F) * grow;
                Vec3 c = side.scale(s * 0.4).add(0, 0.6, 0).add(dir.scale(len / 2 + 0.1));
                EagleFx.feather(vc, m, c, dir, len, 0.38F, k(new float[]{0.55F, 0.85F, 1F}, 1.1F * fade));
            }
        }
        // star trail
        vc = Fx.buf(buf, STAR);
        for (int i = 1; i < 6; i++) {
            float f = 1F - i / 6F;
            Fx.billboard(vc, pose, Minecraft.cam(), e.trailX[i] - ex, e.trailY[i] - ey + 0.5, e.trailZ[i] - ez, 0.6F * f, t * 10F + i * 40F, k(WHITE, 0.8F * f * fade));
        }
    }

    // ================= STORM BRINGER =================
    private static void storm(PegasusEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        float dur = PG_DURATION[PG_STORM];
        float f = easeOut(at / S_FORM) * (at > dur - 10 ? Math.max(0F, (dur - at) / 10F) : 1F);
        double bx = e.abilityX - ex, by = e.abilityY - ey, bz = e.abilityZ - ez;
        double cy = by + S_CLOUD_Y;
        float R = (float) S_RANGE * 0.85F * f;
        // churning storm cloud (two counter-rotating layers)
        for (int l = 0; l < 2; l++) {
            pose.pushPose();
            pose.translate(bx, cy + l * 0.4, bz);
            pose.mulPose(Axis.YP.rotationDegrees((l == 0 ? 1 : -1) * t * 1.5F));
            VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.85F * f);
            Fx.ring(vc, pose.last().pose(), 0F, 0F, R * (1F - l * 0.15F), 40, new float[]{0.18F, 0.2F, 0.28F}, new float[]{0.35F, 0.38F, 0.48F}, 3F, l * 0.5F);
            pose.popPose();
        }
        Fx.alpha = 1F;
        Matrix4f m = pose.last().pose();
        for (int k = 0; k < S_STRIKES; k++) {
            float sa = at - (S_FORM + k * S_STEP);
            if (sa < -1 || sa > 5) continue;
            Entity tgt = e.strikeTarget(k);
            Vec3 tp = tgt != null
                    ? new Vec3(Mth.lerp(partial, tgt.xo, tgt.getX()) - ex, Mth.lerp(partial, tgt.yo, tgt.getY()) - ey, Mth.lerp(partial, tgt.zo, tgt.getZ()) - ez)
                    : strikePoint(k, e.abilityX, e.abilityY, e.abilityZ).subtract(ex, ey, ez);
            float g = sa < 0 ? 0.4F : Math.max(0F, 1F - sa / 5F);
            Vec3 top = new Vec3(tp.x + Math.sin(k * 1.7) * 1.5, cy, tp.z + Math.cos(k * 1.3) * 1.5);
            // jagged bolt: three kinked segments, each drawn with the bolt texture
            Vec3 a1 = top.add(tp.subtract(top).scale(0.35)).add(Math.sin(k * 3.1) * 0.8, 0, Math.cos(k * 2.3) * 0.8);
            Vec3 a2 = top.add(tp.subtract(top).scale(0.7)).add(Math.cos(k * 1.9) * 0.6, 0, Math.sin(k * 2.9) * 0.6);
            VertexConsumer vc = Fx.buf(buf, Fx.BOLT);
            for (Vec3[] seg : new Vec3[][]{{top, a1}, {a1, a2}, {a2, tp.add(0, 0.2, 0)}}) {
                Fx.beamV(vc, m, seg[0], seg[1], 1.1F, k(new float[]{0.75F, 0.9F, 1F}, 1.4F * g), 0.5F, t * 0.7F + k);
            }
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, top, tp, 0.25F, k(WHITE, 0.6F * g), k(WHITE, 1.0F * g), 0.2F, 0F);
            if (sa >= 0) {
                vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, tp.x, tp.y + 0.3, tp.z, 3.2F * g, k * 30F, k(new float[]{0.8F, 0.9F, 1F}, 1.2F * g));
                vc = Fx.buf(buf, Fx.WAVE);
                pose.pushPose();
                pose.translate(tp.x, tp.y, tp.z);
                float r = 0.3F + sa * 0.45F;
                Fx.ring(vc, pose.last().pose(), 0.06F, r - 0.6F, r, 32, BLACK, k(SKY, 1.2F * g), 3F, 0F);
                pose.popPose();
                // the cloud lights up from inside
                pose.pushPose();
                pose.translate(top.x, top.y, top.z);
                vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, 0, 0, 0, 6F * g, 0F, k(new float[]{0.6F, 0.75F, 1F}, 0.8F * g));
                pose.popPose();
            }
        }
    }

    // ================= STAR CONSTELLATION BLAST =================
    private static Vec3 starPoint(int i, float r) {
        double a = Math.toRadians(-90 + i * 72);
        return new Vec3(Math.cos(a) * r, 0.6, Math.sin(a) * r);
    }

    private static void star(PegasusEntity e, float at, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        if (at > C_BLAST) return;
        pose.pushPose();
        pose.translate(e.starX - ex, e.starY - ey, e.starZ - ez);
        Matrix4f m = pose.last().pose();
        float collapse = at > C_COLLAPSE ? easeOut((at - C_COLLAPSE) / (C_BLAST - C_COLLAPSE)) : 0F;
        float r = (float) C_RADIUS * (1F - collapse);
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        Fx.ring(vc, m, 0.06F, r - 0.6F, r, 64, BLACK, k(GOLD, 0.9F), 5F, t * 0.02F);
        if (at >= C_LINES) {
            float lf = Mth.clamp((at - C_LINES) / 6F, 0F, 1F);
            vc = Fx.buf(buf, Fx.SOFT);
            for (int i = 0; i < 5; i++) {
                Vec3 a = starPoint(i, r), b = starPoint((i + 2) % 5, r);
                Fx.beam(vc, m, a, a.add(b.subtract(a).scale(lf)), 0.3F, k(GOLD, 1.1F), k(WHITE, 1.1F), 0.5F, t * 0.3F);
            }
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.ring(vc, m, 0.05F, 0F, r, 48, k(GOLD, 0.35F * lf), BLACK, 2F, 0F);
        }
        vc = Fx.buf(buf, STAR);
        for (int i = 0; i < 5; i++) {
            float sa = at - (C_STAR0 + i * C_STAR_STEP);
            if (sa < 0) continue;
            float g = easeOut(sa / 4F);
            Vec3 p = starPoint(i, r);
            Fx.billboard(vc, pose, cam, p.x, p.y + 0.2 * Mth.sin(t * 0.3F + i), p.z, 1.5F * g * (1F + 0.15F * Mth.sin(t * 0.8F + i)), t * 3F + i * 72F,
                    k(new float[]{1F, 0.95F, 0.7F}, 1.2F));
        }
        pose.popPose();
    }

    private static void nova(PegasusEntity e, float nv, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam, boolean atSelf) {
        float f = 1F - nv / 24F;
        pose.pushPose();
        if (!atSelf) pose.translate(e.starX - ex, e.starY - ey, e.starZ - ez);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        float r = 0.5F + easeOut(nv / 12F) * 8F;
        Fx.ring(vc, m, 0.08F, r - 1.8F, r, 64, BLACK, k(GOLD, 1.5F * f), 6F, 0F);
        Fx.ring(vc, m, 0.1F, r * 0.6F - 1F, r * 0.6F, 64, BLACK, k(SKY, 1.2F * f), 6F, 0.3F);
        vc = Fx.buf(buf, STAR);
        Fx.billboard(vc, pose, cam, 0, 1.2, 0, 7F * easeOut(nv / 5F) * f + 0.5F, t * 4F, k(WHITE, 1.3F * f));
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, 0, 0.8, 0, 9F * f, t * 2F, k(new float[]{0.8F, 0.9F, 1F}, f));
        pose.popPose();
    }

    // ================= STAR GALLOP =================
    private static void gallop(PegasusEntity e, float at, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float dur = PG_DURATION[PG_GALLOP];
        float g = Mth.clamp((at - G_START + 6) / 8F, 0F, 1F) * (at > G_END ? Math.max(0F, 1F - (at - G_END) / 8F) : 1F);
        double bx = e.abilityX - ex, by = e.abilityY - ey, bz = e.abilityZ - ez;
        double ang = gallopAngle(at);
        Vec3 sp = new Vec3(bx + Math.cos(ang) * G_RADIUS, by, bz + Math.sin(ang) * G_RADIUS);
        // blue fire trail along the ring behind the spirit
        pose.pushPose();
        pose.translate(bx, by, bz);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WISP);
        int steps = 24;
        double len = Math.min(ang, Math.PI * 1.3);
        for (int i = 0; i < steps; i++) {
            double a0 = ang - len * i / steps, a1 = ang - len * (i + 1) / steps;
            float f0 = 1F - (float) i / steps, f1 = 1F - (float) (i + 1) / steps;
            Vec3 p0 = new Vec3(Math.cos(a0) * G_RADIUS, 0.35, Math.sin(a0) * G_RADIUS), p1 = new Vec3(Math.cos(a1) * G_RADIUS, 0.35, Math.sin(a1) * G_RADIUS);
            Fx.beam(vc, m, p0, p1, 1.4F * f0, k(SKY, 1.1F * f0 * g), k(SKY, 1.1F * f1 * g), 0.6F, t * 0.2F + i * 0.4F);
        }
        vc = Fx.buf(buf, Fx.SOFT);
        Fx.ring(vc, m, 0.05F, (float) G_RADIUS - 1.2F, (float) G_RADIUS + 0.4F, 64, BLACK, k(SKY, 0.6F * g), 4F, 0F);
        pose.popPose();
        if (at >= G_START - 6 && at < G_END + 8) {
            boolean flip = Math.sin(ang) * 1 < 0;
            spirit(buf, pose, cam, sp.x, sp.y + 1.6, sp.z, 4.6F, g, flip);
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, sp.x, sp.y + 0.3, sp.z, 2.4F * g, t * 9F, k(SKY, g));
        }
        // Pegasus at the centre channels the stampede
        vc = Fx.buf(buf, Fx.SOFT);
        Fx.beam(vc, pose.last().pose(), new Vec3(0, 0, 0), new Vec3(0, 7, 0), 0.9F * g, k(SKY, 0.9F * g), BLACK, 0.3F, -t * 0.3F);
        if (dur < 0) return;
    }

    /** camera orientation helper for sprites drawn outside the renderer call chain */
    private static final class Minecraft {
        static Quaternionf cam() {
            return net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        }
    }
}
