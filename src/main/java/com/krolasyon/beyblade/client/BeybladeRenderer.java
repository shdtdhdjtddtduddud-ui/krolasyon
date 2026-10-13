package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static com.krolasyon.beyblade.entity.BeybladeEntity.*;

/** Spinning Flame Libra: spin + motion blur, precession wobble, attack lean, hit shake, afterimages, ability poses. */
public class BeybladeRenderer extends EntityRenderer<BeybladeEntity> {
    public BeybladeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.42F;
        this.shadowStrength = 0.8F;
    }

    @Override
    public void render(BeybladeEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        float t = e.tickCount + partial;
        int state = e.getState();
        float ratio = e.spinRatio();
        float spin = Mth.lerp(partial, e.spinAngleO, e.spinAngle);
        float prec = Mth.lerp(partial, e.precessO, e.precess) * Mth.DEG_TO_RAD;
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());

        // ---- tilt = precession wobble + motion lean + hit shake + ability pose (axis-angle vector in the XZ plane, degrees)
        double vx = e.getX() - e.xo, vz = e.getZ() - e.zo;
        float sp = (float) Math.sqrt(vx * vx + vz * vz);
        float wob;
        if (state == STATE_DEFEATED) {
            float k = Mth.clamp((t - e.clientStateStart) / 22F, 0F, 1F);
            wob = Mth.lerp(k * k, 10F, 76F);
        } else if (state == STATE_RECALL) {
            wob = 20F;
        } else {
            wob = ratio < 0.4F ? 2.5F + (0.4F - ratio) / 0.4F * 17F : 1.2F + (1F - ratio) * 2.5F;
        }
        float tx = Mth.cos(prec) * wob, tz = Mth.sin(prec) * wob;
        if (sp > 0.02F && state != STATE_RECALL) {
            float lean = Math.min(17F, sp * 48F);
            tx += (float) (vz / sp) * lean;
            tz += (float) (-vx / sp) * lean;
        }
        float hitAge = t - e.hitTime;
        if (hitAge >= 0 && hitAge < 9) {
            float k = 1F - hitAge / 9F;
            float amp = (t - e.heavyHitTime < 9 ? 22F : 12F) * k;
            tx += Mth.sin(t * 2.7F) * amp;
            tz += Mth.cos(t * 3.1F) * amp;
        }
        float lift = 0F;
        float jitter = 0F;
        if (ab == AB_SONIC_WAVE) {
            jitter = Mth.sin(t * 11F) * 0.025F;
        } else if (ab == AB_INFERNO && at < INF_TORNADO) {
            lift = -0.24F * Mth.clamp(at / 5F, 0F, 1F);
            jitter = Mth.sin(t * 13F) * 0.02F;
        } else if (ab == AB_BUSTER && at > 4 && at < BUS_END) {
            Entity tgt = e.level().getEntity(e.getTargetId());
            if (tgt != null) {
                double dx = tgt.getX() - e.getX(), dz = tgt.getZ() - e.getZ();
                double dl = Math.sqrt(dx * dx + dz * dz);
                if (dl > 1.0E-3) {
                    float k = Mth.clamp((at - 4F) / 8F, 0F, 1F) * 34F;
                    tx = (float) (dz / dl) * k;
                    tz = (float) (-dx / dl) * k;
                }
            }
            if (at > BUS_FIRE) jitter = Mth.sin(t * 17F) * 0.03F;
        }
        float tilt = (float) Math.sqrt(tx * tx + tz * tz);

        // ---- glow colour / intensity
        float[] glowCol = e.isRival() ? new float[]{1F, 0.25F, 0.15F} : new float[]{0.55F, 1F, 0.45F};
        float glow = 0.12F;
        if (ab != AB_NONE) {
            glow = 0.75F + 0.25F * Mth.sin(t * 0.9F);
            switch (ab) {
                case AB_INFERNO -> glowCol = new float[]{1F, 0.55F, 0.15F};
                case AB_BUSTER -> glowCol = new float[]{0.5F, 1F, 0.95F};
                case AB_JUDGMENT -> glowCol = new float[]{1F, 0.85F, 0.4F};
                default -> {}
            }
        } else if (e.isShielded()) {
            glow = 0.45F + 0.15F * Mth.sin(t * 0.5F);
        }
        if (state == STATE_DEFEATED) glow = 0F;
        float flash = hitAge >= 0 && hitAge < 4 ? (1F - hitAge / 4F) : 0F;
        int overlay = OverlayTexture.pack(OverlayTexture.u(flash * 0.7F), OverlayTexture.v(false));

        // ---- afterimages while dashing / flying back
        if (sp > 0.17F || state == STATE_RECALL) renderTrail(e, ex, ey, ez, pose, buf, light, sp);

        pose.pushPose();
        pose.translate(0, lift + jitter, 0);
        if (tilt > 0.01F) pose.mulPose(new Quaternionf().rotateAxis(tilt * Mth.DEG_TO_RAD, tx / tilt, 0F, tz / tilt));
        pose.scale(RADIUS, RADIUS, RADIUS);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-spin));
        LibraMesh.render(pose, buf, light, overlay, 1F, 1F, 1F);
        float g = glow + flash * 0.8F;
        LibraMesh.renderGlow(pose, buf, glowCol[0] * g, glowCol[1] * g, glowCol[2] * g);
        pose.popPose();

        // ---- spin motion blur: translucent copies of the face trailing the rotation
        if (state == STATE_BATTLE && ratio > 0.08F) {
            float speed = 30F + 62F * ratio;
            float a = 0.3F * Math.min(1F, ratio * 1.6F);
            for (int k = 1; k <= 2; k++) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(-spin + speed * 0.3F * k));
                LibraMesh.renderGhostDisc(pose, buf, light, a / k, LibraMesh.TOP_Y + 0.01F * k);
                pose.popPose();
            }
            // speed streaks around the rim
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Matrix4f m = pose.last().pose();
            float k = 0.35F * ratio;
            for (int i = 0; i < 3; i++) {
                float a0 = -spin * Mth.DEG_TO_RAD * 0.5F + i * Mth.TWO_PI / 3F;
                Fx.helix(vc, m, 1.08F, 1.08F, 1.06F, 1.06F, a0, -0.16F, 0.22F, 8, Fx.c(0.8F, 1F, 0.8F, k), Fx.c(0.8F, 1F, 0.8F, 0F), t * 0.1F);
            }
        }
        pose.popPose();

        LibraFx.render(e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
        super.render(e, yaw, partial, pose, buf, light);
    }

    private void renderTrail(BeybladeEntity e, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, int light, float sp) {
        int n = 5;
        float base = e.getState() == STATE_RECALL ? 0.25F : Mth.clamp((sp - 0.17F) * 2.5F, 0F, 0.3F);
        if (base <= 0.01F) return;
        for (int i = 1; i <= n; i++) {
            float k = 1F - i / (float) (n + 1);
            pose.pushPose();
            pose.translate(e.trailX[i] - ex, e.trailY[i] - ey, e.trailZ[i] - ez);
            pose.scale(RADIUS, RADIUS, RADIUS);
            pose.mulPose(Axis.YP.rotationDegrees(-e.spinAngle + i * 40F));
            LibraMesh.renderGhostDisc(pose, buf, light, base * k, LibraMesh.TOP_Y - 0.15F);
            pose.popPose();
        }
        VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
        Matrix4f m = pose.last().pose();
        float[] col = e.isRival() ? new float[]{1F, 0.3F, 0.2F} : new float[]{0.5F, 1F, 0.5F};
        for (int i = 0; i < n; i++) {
            Vec3 a = new Vec3(e.trailX[i] - ex, e.trailY[i] - ey + 0.32, e.trailZ[i] - ez);
            Vec3 b = new Vec3(e.trailX[i + 1] - ex, e.trailY[i + 1] - ey + 0.32, e.trailZ[i + 1] - ez);
            if (i == 0) a = new Vec3(0, 0.32, 0);
            float k0 = (1F - i / (float) n) * base * 2.2F, k1 = (1F - (i + 1) / (float) n) * base * 2.2F;
            Fx.beam(vc, m, a, b, 0.55F, Fx.c(col[0], col[1], col[2], k0), Fx.c(col[0], col[1], col[2], k1), 0.5F, 0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BeybladeEntity e) {
        return LibraMesh.FACE;
    }
}
