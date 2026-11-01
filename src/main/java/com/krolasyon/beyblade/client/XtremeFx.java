package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.StadiumEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Beyblade X effects: sparks while the tip's gear grinds along the Xtreme Line, the Xtreme Dash streak, pocket finishes. */
public final class XtremeFx {
    private XtremeFx() {}

    private static final float[] ORANGE = {1F, 0.5F, 0.1F}, HOT = {1F, 0.85F, 0.5F}, BLUE = {0.3F, 0.6F, 1F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    public static void render(BeybladeEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        float rail = t - e.railClientTime, dash = t - e.xdashClientTime, out = t - e.outClientTime;
        if (rail > 4 && dash > 16 && out > 30) return;
        Matrix4f m = pose.last().pose();
        Vec3 vel = new Vec3(e.getX() - e.xo, 0, e.getZ() - e.zo);
        double sp = vel.length();
        Vec3 back = sp < 1.0E-3 ? new Vec3(-1, 0, 0) : vel.scale(-1 / sp);
        if (rail >= 0 && rail <= 4) {
            // the gear meshing with the rail: sparks thrown out behind the contact on the outer side
            StadiumEntity st = stadium(e);
            Vec3 out2 = st == null ? new Vec3(1, 0, 0) : new Vec3(e.getX() - st.getX(), 0, e.getZ() - st.getZ()).normalize();
            Vec3 c = out2.scale(BeybladeEntity.RADIUS * 0.9).add(0, 0.08, 0);
            VertexConsumer vc = Fx.buf(buf, Fx.BOLT);
            for (int i = 0; i < 4; i++) {
                double a = t * 1.7 + i * 1.6;
                Vec3 tip = c.add(back.scale(0.8 + 0.3 * Math.sin(a))).add(out2.scale(0.2 * Math.cos(a))).add(0, 0.15 + 0.15 * Math.sin(a * 1.3), 0);
                Fx.beamV(vc, m, c, tip, 0.25F, k(HOT, 1.4F), 0.5F, t + i);
            }
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, new Vec3(0, 0.25, 0), back.scale(Math.min(4.0, sp * 6)).add(0, 0.25, 0), 0.45F, k(ORANGE, 1.4F), k(ORANGE, 0F), 0.5F, 0F);
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, c.x, c.y, c.z, 0.9F, t * 20F, k(HOT, 1F));
        }
        if (dash >= 0 && dash <= 16) {
            // Xtreme Dash: a long blazing streak and a burst where it left the rail
            float f = 1F - dash / 16F;
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, new Vec3(0, 0.35, 0), back.scale(Math.min(7.0, sp * 8)).add(0, 0.35, 0), 0.9F * f + 0.2F, k(HOT, 1.6F * f), k(ORANGE, 0F), 0.5F, 0F);
            vc = Fx.buf(buf, Fx.WAVE);
            Fx.ring(vc, m, 0.08F, 0.6F, 1.3F, 32, new float[]{0, 0, 0}, k(ORANGE, 1.2F * f), 4F, t * 0.1F);
            if (dash < 6) {
                vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, 0, 0.4, 0, 3.2F * (1F - dash / 6F), t * 8F, k(HOT, 1.3F));
            }
        }
        if (out >= 0 && out <= 30) {
            // knocked into a pocket: a pillar of light in the zone's colour
            StadiumEntity st = stadium(e);
            boolean xtreme = st != null && StadiumEntity.zoneAt(StadiumEntity.deg(e.getX() - st.getX(), e.getZ() - st.getZ())) == StadiumEntity.Z_XTREME;
            float[] c = xtreme ? ORANGE : BLUE;
            float f = 1F - out / 30F;
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Fx.band(vc, m, 0.7F, 0F, 0.9F, 6F * Mth.clamp(out / 6F, 0F, 1F), 24, k(c, 1.4F * f), k(c, 0F), 1F, t * 0.1F, 1F, 0F);
            if (out < 10) {
                vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, 0, 0.6, 0, 5F * (1F - out / 10F), t * 4F, k(c, 1.5F));
            }
        }
    }

    private static StadiumEntity stadium(BeybladeEntity e) {
        for (StadiumEntity s : e.level().getEntitiesOfClass(StadiumEntity.class, e.getBoundingBox().inflate(16, 6, 16))) return s;
        return null;
    }
}
