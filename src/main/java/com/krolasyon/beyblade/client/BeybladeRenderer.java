package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.krolasyon.beyblade.entity.LDragoEntity;
import com.krolasyon.beyblade.entity.LeoneEntity;
import com.krolasyon.beyblade.entity.EagleEntity;
import com.krolasyon.beyblade.entity.PegasusEntity;
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
        boolean drago = e instanceof LDragoEntity;
        boolean leone = e instanceof LeoneEntity;
        boolean eagle = e instanceof EagleEntity;
        boolean pegasus = e instanceof PegasusEntity;
        CommonBeybladeEntity common = e instanceof CommonBeybladeEntity c ? c : null;
        BeyMesh mesh = BeyMesh.of(e);
        int dir = e.spinDir();
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
        if (common != null) {
            if (ab != AB_NONE) jitter = Mth.sin(t * 13F) * 0.015F;
        } else if (pegasus) {
            if (ab == PegasusEntity.PG_STORM || ab == PegasusEntity.PG_STAR) jitter = Mth.sin(t * 12F) * 0.015F;
        } else if (eagle) {
            if (ab == EagleEntity.EA_FEATHER) {
                jitter = Mth.sin(t * 13F) * 0.02F;
            } else if (ab == EagleEntity.EA_TALON && at >= EagleEntity.T_SWOOP && t - ((EagleEntity) e).grabTime > 40) {
                Entity tgt = e.level().getEntity(e.getTargetId());
                if (tgt != null) {
                    double dx = tgt.getX() - e.getX(), dz = tgt.getZ() - e.getZ();
                    double dl = Math.sqrt(dx * dx + dz * dz);
                    if (dl > 1.0E-3) {
                        tx = (float) (dz / dl) * 40F;
                        tz = (float) (-dx / dl) * 40F;
                    }
                }
            }
        } else if (leone) {
            if (ab == LeoneEntity.LE_POUNCE && at < LeoneEntity.P_LEAP) {
                lift = -0.18F * Mth.clamp(at / 4F, 0F, 1F);
                jitter = Mth.sin(t * 14F) * 0.02F;
            } else if ((ab == LeoneEntity.LE_FANG || ab == LeoneEntity.LE_CAGE) && at > 2) {
                Entity tgt = e.level().getEntity(e.getTargetId());
                if (tgt != null) {
                    double dx = tgt.getX() - e.getX(), dz = tgt.getZ() - e.getZ();
                    double dl = Math.sqrt(dx * dx + dz * dz);
                    if (dl > 1.0E-3) {
                        float k = Mth.clamp((at - 2F) / 6F, 0F, 1F) * 22F;
                        tx = (float) (dz / dl) * k;
                        tz = (float) (-dx / dl) * k;
                    }
                }
                jitter = Mth.sin(t * 16F) * 0.015F;
            }
        } else if (drago) {
            if (ab == LDragoEntity.DR_VORTEX) {
                jitter = Mth.sin(t * 12F) * 0.02F;
                lift = -0.08F;
            } else if (ab == LDragoEntity.DR_METEOR && at < LDragoEntity.M_RISE) {
                lift = -0.22F * Mth.clamp(at / 5F, 0F, 1F);
                jitter = Mth.sin(t * 15F) * 0.025F;
            } else if (ab == LDragoEntity.DR_ROAR && at > 3 && at < LDragoEntity.R_END) {
                Entity tgt = e.level().getEntity(e.getTargetId());
                if (tgt != null) {
                    double dx = tgt.getX() - e.getX(), dz = tgt.getZ() - e.getZ();
                    double dl = Math.sqrt(dx * dx + dz * dz);
                    if (dl > 1.0E-3) {
                        float k = Mth.clamp((at - 3F) / 10F, 0F, 1F) * 30F;
                        tx = (float) (dz / dl) * k;
                        tz = (float) (-dx / dl) * k;
                    }
                }
                if (at > LDragoEntity.R_BREATH) jitter = Mth.sin(t * 19F) * 0.03F;
            }
        } else if (ab == AB_SONIC_WAVE) {
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
        float[] glowCol = drago ? (e.isRival() ? new float[]{0.65F, 0.2F, 1F} : new float[]{1F, 0.15F, 0.28F})
                : (e.isRival() ? new float[]{1F, 0.25F, 0.15F} : new float[]{0.55F, 1F, 0.45F});
        if (pegasus) glowCol = e.isRival() ? new float[]{1F, 0.4F, 0.2F} : new float[]{0.4F, 0.75F, 1F};
        if (eagle) glowCol = e.isRival() ? new float[]{1F, 0.3F, 0.3F} : new float[]{0.7F, 0.4F, 1F};
        if (leone) glowCol = e.isRival() ? new float[]{1F, 0.6F, 0.1F} : new float[]{0.3F, 1F, 0.45F};
        if (common != null) glowCol = common.variant().rgb(true);
        float glow = 0.12F;
        if (common != null) {
            if (ab != AB_NONE) glow = 0.85F + 0.15F * Mth.sin(t * 1.1F);
            else if (e.isShielded()) glow = 0.55F + 0.2F * Mth.sin(t * 0.8F);
        } else if (pegasus) {
            if (ab != AB_NONE) glow = 0.8F + 0.2F * Mth.sin(t * 1.0F);
            else if (e.isShielded()) glow = 0.65F + 0.25F * Mth.sin(t * 0.9F);
        } else if (eagle) {
            if (ab != AB_NONE) glow = 0.8F + 0.2F * Mth.sin(t * 1.0F);
            else if (e.isShielded()) glow = 0.6F + 0.25F * Mth.sin(t * 0.9F);
        } else if (leone) {
            if (ab != AB_NONE) {
                glow = 0.8F + 0.2F * Mth.sin(t * 1.0F);
                if (ab == LeoneEntity.LE_QUAKE) glowCol = new float[]{1F, 0.8F, 0.35F};
            } else if (e.isShielded()) {
                glow = 0.55F + 0.2F * Mth.sin(t * 0.7F);
            }
        } else if (drago) {
            if (ab != AB_NONE) {
                glow = 0.8F + 0.2F * Mth.sin(t * 1.1F);
                if (ab == LDragoEntity.DR_METEOR || ab == LDragoEntity.DR_ROAR) glowCol = new float[]{1F, 0.45F, 0.1F};
            } else if (e.isShielded()) {
                glow = 0.7F + 0.3F * Mth.sin(t * 0.8F);
            }
        } else if (ab != AB_NONE) {
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
        pose.mulPose(Axis.YP.rotationDegrees(-spin * dir));
        mesh.render(pose, buf, light, overlay, 1F, 1F, 1F);
        float g = glow + flash * 0.8F;
        mesh.renderGlow(pose, buf, glowCol[0] * g, glowCol[1] * g, glowCol[2] * g);
        pose.popPose();

        // ---- spin motion blur: translucent copies of the face trailing the rotation
        if (state == STATE_BATTLE && ratio > 0.08F) {
            float speed = BeybladeEntity.spinSpeedFor(ratio);
            float a = 0.4F * Math.min(1F, ratio * 1.6F);
            // motion blur: copies spread over the angle covered during one frame
            for (int k = 1; k <= 6; k++) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees((-spin + speed * 0.09F * k) * dir));
                mesh.renderGhostDisc(pose, buf, light, a * (1F - k * 0.14F), mesh.TOP_Y + 0.006F * k);
                pose.popPose();
            }
            // speed streaks around the rim
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Matrix4f m = pose.last().pose();
            float k = 0.35F * ratio;
            float[] streak = common != null ? common.variant().rgb(true) : drago ? new float[]{1F, 0.45F, 0.5F} : (leone ? new float[]{0.6F, 1F, 0.6F} : (eagle ? new float[]{0.8F, 0.6F, 1F} : (pegasus ? new float[]{0.6F, 0.85F, 1F} : new float[]{0.8F, 1F, 0.8F})));
            for (int i = 0; i < 3; i++) {
                float a0 = -spin * dir * Mth.DEG_TO_RAD * 0.5F + i * Mth.TWO_PI / 3F;
                Fx.helix(vc, m, 1.08F, 1.08F, 1.06F, 1.06F, a0, -0.16F * dir, 0.22F, 8, Fx.c(streak[0], streak[1], streak[2], k), Fx.c(streak[0], streak[1], streak[2], 0F), t * 0.1F);
            }
        }
        pose.popPose();

        float gr = t - e.grindTime;
        if (gr >= 0 && gr < 5) renderSparks(e, gr, t, pose, buf);
        float ev = t - e.evadeTime;
        if (ev >= 0 && ev < 10) {
            float f = 1F - ev / 10F;
            VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
            Fx.ring(vc, pose.last().pose(), 0.25F, 0.3F + ev * 0.12F, 0.6F + ev * 0.18F, 32, new float[]{0, 0, 0}, Fx.c(0.8F, 0.95F, 1F, 1.2F * f), 3F, 0F);
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.band(vc, pose.last().pose(), 0.5F, 0F, 0.7F, 1.1F * f, 24, Fx.c(glowCol[0], glowCol[1], glowCol[2], 0.7F * f), new float[]{0, 0, 0}, 1F, 0F, 1F, 0F);
        }
        float sh = t - e.shockTime;
        if (sh >= 0 && sh < 14) renderShock(e, sh, ex, ey, ez, pose, buf);
        float sa = t - e.spiritTime;
        if (e.isSpiritActive() || (sa >= 0 && sa < 24)) renderSpirit(e, t, sa, pose, buf, glowCol);
        if (common != null) CommonFx.render(common, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
        else if (pegasus) PegasusFx.render((PegasusEntity) e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
        else if (eagle) EagleFx.render((EagleEntity) e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
        else if (leone) LeoneFx.render((LeoneEntity) e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation(), light);
        else if (drago) LDragoFx.render((LDragoEntity) e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
        else LibraFx.render(e, partial, t, ex, ey, ez, pose, buf, this.entityRenderDispatcher.cameraOrientation());
        super.render(e, yaw, partial, pose, buf, light);
    }

    /** sparks spraying from the contact point while two tops grind against each other */
    private void renderSparks(BeybladeEntity e, float age, float t, PoseStack pose, MultiBufferSource buf) {
        BeybladeEntity other = null;
        double best = 2.5;
        for (BeybladeEntity b : e.level().getEntitiesOfClass(BeybladeEntity.class, e.getBoundingBox().inflate(1.6), b -> b != e)) {
            double d = b.distanceToSqr(e);
            if (d < best) { best = d; other = b; }
        }
        if (other == null) return;
        Vec3 dir = new Vec3(other.getX() - e.getX(), 0, other.getZ() - e.getZ());
        if (dir.lengthSqr() < 1.0E-4) return;
        dir = dir.normalize();
        Vec3 c = dir.scale(RADIUS).add(0, 0.38, 0);
        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(e.spinDir());
        VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
        Matrix4f m = pose.last().pose();
        float f = 1F - age / 5F;
        int seed = (int) (t * 3) + e.getId() * 31;
        for (int i = 0; i < 14; i++) {
            float h1 = hash(seed + i * 17), h2 = hash(seed + i * 29), h3 = hash(seed + i * 43);
            // sprayed tangentially (the spin throws them sideways) and a bit up
            Vec3 d = side.scale(0.6 + h1).add(dir.scale(h2 - 0.5)).add(0, 0.2 + h3 * 0.6, 0).normalize();
            float len = 0.25F + h2 * 0.6F;
            Vec3 a = c.add(d.scale(age * 0.12));
            Vec3 b = a.add(d.scale(len)).add(0, -age * age * 0.01, 0);
            float k = f * (0.6F + 0.4F * h3);
            Fx.beam(vc, m, a, b, 0.07F, Fx.c(1F, 0.95F, 0.7F, 1.4F * k), Fx.c(1F, 0.45F, 0.1F, 0.6F * k), 1F, 0F);
        }
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation(), c.x, c.y, c.z, 0.9F * f, t * 20F, Fx.c(1F, 0.8F, 0.4F, 1.2F * f));
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / (float) 0x7fffffff;
    }

    /** directional shock wave from a strong hit: an arc of compressed air racing away from the impact */
    private void renderShock(BeybladeEntity e, float age, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf) {
        float f = 1F - age / 14F;
        float k = 1F - (1F - Math.min(1F, age / 11F)) * (1F - Math.min(1F, age / 11F)) * (1F - Math.min(1F, age / 11F));
        float r = 0.5F + k * 5.5F;
        float base = (float) Math.atan2(e.shockDirZ, e.shockDirX);
        float cx = (float) (e.shockX - ex), cy = (float) (e.shockY - ey) + 0.3F, cz = (float) (e.shockZ - ez);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        int N = 18;
        float span = 1.15F;
        float[] cIn = Fx.c(0.75F, 0.9F, 1F, 0F), cOut = Fx.c(0.8F, 0.92F, 1F, 1.3F * f);
        float[] wall = Fx.c(0.85F, 0.95F, 1F, 0.9F * f), top = Fx.c(0F, 0F, 0F, 0F);
        float h = 1.1F * f + 0.2F;
        for (int i = 0; i < N; i++) {
            float a0 = base - span + 2 * span * i / N, a1 = base - span + 2 * span * (i + 1) / N;
            // fade toward the ends of the arc
            float w0 = 1F - Math.abs(i / (float) N * 2 - 1), w1 = 1F - Math.abs((i + 1) / (float) N * 2 - 1);
            float ri = Math.max(0F, r - 1.3F);
            float[] o0 = Fx.c(cOut[0], cOut[1], cOut[2], w0), o1 = Fx.c(cOut[0], cOut[1], cOut[2], w1);
            Fx.quad(vc, m, new float[]{cx + Mth.cos(a0) * ri, cy, cz + Mth.sin(a0) * ri}, new float[]{cx + Mth.cos(a0) * r, cy, cz + Mth.sin(a0) * r},
                    new float[]{cx + Mth.cos(a1) * r, cy, cz + Mth.sin(a1) * r}, new float[]{cx + Mth.cos(a1) * ri, cy, cz + Mth.sin(a1) * ri},
                    i / (float) N, 0F, (i + 1) / (float) N, 1F, cIn, Fx.lerp(0.5F, o0, o1));
            float[] wc = Fx.c(wall[0], wall[1], wall[2], (w0 + w1) * 0.5F);
            Fx.quad(vc, m, new float[]{cx + Mth.cos(a0) * r, cy - 0.2F, cz + Mth.sin(a0) * r}, new float[]{cx + Mth.cos(a0) * r, cy + h, cz + Mth.sin(a0) * r},
                    new float[]{cx + Mth.cos(a1) * r, cy + h, cz + Mth.sin(a1) * r}, new float[]{cx + Mth.cos(a1) * r, cy - 0.2F, cz + Mth.sin(a1) * r},
                    i / (float) N, 1F, (i + 1) / (float) N, 0F, wc, top);
        }
    }

    /** Blader Spirit: a golden aura column around the top while the burst lasts */
    private void renderSpirit(BeybladeEntity e, float t, float sa, PoseStack pose, MultiBufferSource buf, float[] col) {
        float in = sa >= 0 && sa < 24 ? 1F - sa / 24F : 0F;
        float on = e.isSpiritActive() ? Math.min(1F, e.spiritRemaining() / 20F) : 0F;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WISP);
        float[] gold = {1F, 0.8F, 0.3F};
        float a = on * (0.6F + 0.2F * Mth.sin(t * 0.8F));
        if (a > 0.01F) {
            Fx.band(vc, m, 0.55F, 0F, 0.9F, 2.2F, 24, Fx.c(gold[0], gold[1], gold[2], a), new float[]{0, 0, 0}, 2F, t * 0.15F, 1F, -t * 0.08F);
            Fx.band(vc, m, 0.5F, 0F, 0.6F, 1.4F, 24, Fx.c(col[0], col[1], col[2], a * 0.8F), new float[]{0, 0, 0}, 3F, -t * 0.2F, 1F, -t * 0.1F);
        }
        if (in > 0.01F) {
            vc = Fx.buf(buf, Fx.WAVE);
            float r = 0.6F + (1F - in) * 4F;
            Fx.ring(vc, m, 0.1F, r - 0.9F, r, 48, new float[]{0, 0, 0}, Fx.c(gold[0], gold[1], gold[2], 1.4F * in), 4F, 0F);
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.band(vc, m, 0.3F, 0F, 0.1F, 8F * in, 16, Fx.c(gold[0], gold[1], gold[2], in), new float[]{0, 0, 0}, 1F, 0F, 1F, 0F);
        }
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
            pose.mulPose(Axis.YP.rotationDegrees((-e.spinAngle + i * 40F) * e.spinDir()));
            BeyMesh mesh = BeyMesh.of(e);
            mesh.renderGhostDisc(pose, buf, light, base * k, mesh.TOP_Y - 0.15F);
            pose.popPose();
        }
        VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
        Matrix4f m = pose.last().pose();
        float[] col = e instanceof CommonBeybladeEntity c ? c.variant().rgb(true) : e instanceof PegasusEntity ? new float[]{0.4F, 0.75F, 1F} : e instanceof EagleEntity ? new float[]{0.7F, 0.45F, 1F} : e instanceof LeoneEntity ? new float[]{0.4F, 1F, 0.5F} : e instanceof LDragoEntity ? new float[]{1F, 0.15F, 0.25F} : (e.isRival() ? new float[]{1F, 0.3F, 0.2F} : new float[]{0.5F, 1F, 0.5F});
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
        return BeyMesh.of(e).FACE;
    }
}
