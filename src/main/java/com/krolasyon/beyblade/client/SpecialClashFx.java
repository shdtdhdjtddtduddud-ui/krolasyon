package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Two special moves meeting head on: a beam of each beyblade's colour pushes against the other, the meeting point is
 * driven towards the weaker one, and when the clash is decided it explodes. Drawn by the "leader" of the pair only.
 */
public final class SpecialClashFx {
    private SpecialClashFx() {}

    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    /** the signature colour of each beyblade */
    public static float[] color(BeybladeEntity e) {
        if (e instanceof CommonBeybladeEntity c) return c.variant().rgb(true);
        return switch (e.kindId()) {
            case "ldrago" -> new float[]{1F, 0.15F, 0.25F};
            case "leone" -> new float[]{0.35F, 1F, 0.5F};
            case "eagle" -> new float[]{0.7F, 0.45F, 1F};
            case "pegasus" -> new float[]{0.35F, 0.7F, 1F};
            case "tempo" -> new float[]{0.75F, 0.85F, 1F};
            case "variares" -> new float[]{1F, 0.2F, 0.1F};
            case "beffal" -> new float[]{1F, 0.35F, 0.75F};
            default -> new float[]{0.5F, 1F, 0.5F};
        };
    }

    public static void render(BeybladeEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        float boom = t - e.sclashBoomTime;
        if (boom >= 0 && boom < 30) renderBoom(e, boom, t, ex, ey, ez, pose, buf, cam);
        int pid = e.getSpecialClashPartner();
        if (pid < 0 || pid < e.getId()) return;       // the beyblade with the lower id draws the clash
        Entity o = e.level().getEntity(pid);
        if (!(o instanceof BeybladeEntity other)) return;
        float age = t - e.sclashClientStart;
        if (age < 0) return;
        float kk = Mth.clamp(age / BeybladeEntity.SCLASH_TICKS, 0F, 1F);
        double ox = Mth.lerp(partial, other.xo, other.getX()) - ex, oy = Mth.lerp(partial, other.yo, other.getY()) - ey,
                oz = Mth.lerp(partial, other.zo, other.getZ()) - ez;
        Vec3 a = new Vec3(0, 0.55, 0), b = new Vec3(ox, oy + 0.55, oz);
        // the meeting point wobbles while both push, then is driven towards the weaker one
        double f = BeybladeEntity.sclashMeet(e.getSpecialClashRatio(), kk) + 0.035 * Math.sin(t * 1.4) * (1 - kk * 0.6);
        Vec3 m = a.add(b.subtract(a).scale(f));
        float[] ca = color(e), cb = color(other);
        float grow = Mth.clamp(age / 5F, 0F, 1F);
        float pulse = 0.8F + 0.2F * Mth.sin(t * 2.1F);
        Matrix4f mx = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
        Fx.beam(vc, mx, a, m, 0.55F * grow * pulse, k(ca, 1.4F), k(ca, 1.6F), 0.5F, t * 0.2F);
        Fx.beam(vc, mx, b, m, 0.55F * grow * pulse, k(cb, 1.4F), k(cb, 1.6F), 0.5F, -t * 0.2F);
        vc = Fx.buf(buf, Fx.WAVE);
        Fx.beam(vc, mx, a, m, 0.9F * grow, k(ca, 0.6F), k(ca, 0.9F), 0.3F, -t * 0.15F);
        Fx.beam(vc, mx, b, m, 0.9F * grow, k(cb, 0.6F), k(cb, 0.9F), 0.3F, t * 0.15F);
        // lightning crackling out of the contact
        vc = Fx.buf(buf, Fx.BOLT);
        for (int i = 0; i < 5; i++) {
            double ang = t * 0.9 + i * 1.257;
            Vec3 tip = m.add(Math.cos(ang) * 1.6, Math.sin(ang * 1.7) * 1.1, Math.sin(ang) * 1.6);
            float[] c = i % 2 == 0 ? ca : cb;
            Fx.beamV(vc, mx, m, tip, 0.45F * grow, k(c, 1.3F), 0.5F, t * 0.8F + i);
        }
        // the contact itself: two flares fighting, a ring turning around the axis of the beams
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, m.x, m.y, m.z, (2.4F + 0.8F * kk) * grow * pulse, t * 9F, k(new float[]{1F, 1F, 1F}, 0.9F));
        Fx.billboard(vc, pose, cam, m.x, m.y, m.z, 3.6F * grow, -t * 5F, k(ca, 0.7F));
        Fx.billboard(vc, pose, cam, m.x, m.y, m.z, 3.2F * grow, t * 6F, k(cb, 0.7F));
        vc = Fx.buf(buf, Fx.WAVE);
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() > 1.0E-4) Fx.ringAround(vc, mx, m, axis.normalize(), 0.6F + 0.4F * pulse, 1.3F + 0.3F * pulse, 32, k(new float[]{1F, 0.9F, 0.6F}, grow), t * 0.1F);
        // each beyblade wrapped in its own colour
        Fx.ring(vc, mx, 0.05F, 0.8F, 1.6F, 32, BLACK, k(ca, 0.8F * grow), 4F, t * 0.08F);
        pose.pushPose();
        pose.translate(ox, oy, oz);
        Fx.ring(vc, pose.last().pose(), 0.05F, 0.8F, 1.6F, 32, BLACK, k(cb, 0.8F * grow), 4F, -t * 0.08F);
        pose.popPose();
    }

    private static void renderBoom(BeybladeEntity e, float boom, float t, double ex, double ey, double ez, PoseStack pose,
                                   MultiBufferSource buf, Quaternionf cam) {
        float f = 1F - boom / 30F;
        double x = e.boomX - ex, y = e.boomY - ey, z = e.boomZ - ez;
        float[] c = e.boomDraw ? new float[]{1F, 0.95F, 0.7F} : color(e);
        pose.pushPose();
        pose.translate(x, y, z);
        Matrix4f m = pose.last().pose();
        float r = 0.5F + (1F - (1F - boom / 14F) * (1F - boom / 14F)) * 8F;
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        Fx.ring(vc, m, -0.4F, Math.max(0F, r - 1.8F), r, 64, BLACK, k(new float[]{1F, 0.9F, 0.6F}, 1.5F * f), 6F, 0F);
        Fx.ring(vc, m, -0.3F, Math.max(0F, r * 0.6F - 1F), r * 0.6F, 64, BLACK, k(c, 1.3F * f), 6F, 0.3F);
        vc = Fx.buf(buf, Fx.BOLT);
        float len = Math.min(1F, boom / 4F) * 5F;
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5 + 0.2;
            Fx.beamV(vc, m, new Vec3(0, 0, 0), new Vec3(Math.cos(a) * len, Math.sin(a * 3) * 1.2, Math.sin(a) * len), 0.7F, k(c, 1.3F * f), 0.4F, i * 0.4F);
        }
        if (boom < 12) {
            float g = 1F - boom / 12F;
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 0, 0, 11F * g, t * 3F, k(new float[]{1F, 0.95F, 0.85F}, 1.4F * g));
            Fx.billboard(vc, pose, cam, 0, 0, 0, 7F * g, -t * 4F, k(c, 1.2F * g));
        }
        pose.popPose();
    }
}
