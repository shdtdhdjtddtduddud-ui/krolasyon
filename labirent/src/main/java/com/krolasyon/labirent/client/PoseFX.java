package com.krolasyon.labirent.client;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.entity.GladerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Procedural body language on top of the vanilla human pose: a real sprint (forward lean, pumping arms, long
 * strides, torso twist, bounce) and the trip / slip fall, for players and Gladers alike.
 */
@Mod.EventBusSubscriber(modid = Labirent.MODID, value = Dist.CLIENT)
public final class PoseFX {
    private PoseFX() {}

    private static boolean applies(LivingEntity e) { return e instanceof Player || e instanceof GladerEntity; }

    /** 0..1: how much of the run pose to use */
    private static float runFactor(LivingEntity e, float amount) {
        float byLimbs = Mth.clamp((amount - 0.5F) / 0.4F, 0F, 1F);
        if (e instanceof Player p) return p.isSprinting() ? Math.max(0.65F, byLimbs) : byLimbs * 0.4F;
        return byLimbs;
    }

    /** fall curve: down quickly, lie, get back up */
    public static float fallCurve(float t) {
        if (t < 0) return 0F;
        if (t < 0.22F) return Mth.sin(t / 0.22F * Mth.HALF_PI);
        if (t < 0.7F) return 1F;
        float u = (t - 0.7F) / 0.3F;
        return 1F - u * u * (3 - 2 * u);
    }

    public static void apply(HumanoidModel<?> m, LivingEntity e, float limbSwing, float amount, float age) {
        if (!applies(e) || e.isPassenger() || e.isSleeping() || e.isSwimming() || e.isFallFlying() || e.getPose() == Pose.CROUCHING) return;
        float partial = Minecraft.getInstance().getFrameTime();
        float fall = ClientState.fall(e.getId(), partial);
        if (fall >= 0) {
            poseFall(m, e, fall, age);
            m.hat.copyFrom(m.head);
            return;
        }
        float r = runFactor(e, amount);
        if (r <= 0.01F) return;
        float f = limbSwing * 0.6662F;
        float a = Math.min(amount, 1.2F);
        // long strides and pumping arms
        m.rightLeg.xRot = Mth.cos(f) * 1.55F * a;
        m.leftLeg.xRot = Mth.cos(f + Mth.PI) * 1.55F * a;
        float armSwing = 1.25F + 0.45F * r;
        if (m.rightArmPose == HumanoidModel.ArmPose.EMPTY || m.rightArmPose == HumanoidModel.ArmPose.ITEM)
            m.rightArm.xRot = Mth.cos(f + Mth.PI) * armSwing * a * 0.75F - 0.25F * r;
        if (m.leftArmPose == HumanoidModel.ArmPose.EMPTY || m.leftArmPose == HumanoidModel.ArmPose.ITEM)
            m.leftArm.xRot = Mth.cos(f) * armSwing * a * 0.75F - 0.25F * r;
        m.rightArm.zRot += 0.12F * r;
        m.leftArm.zRot -= 0.12F * r;
        // forward lean with the legs shifted back underneath, torso counter-twist, head kept level
        float lean = 0.32F * r;
        m.body.xRot += lean;
        m.body.yRot += Mth.sin(f) * 0.14F * r;
        m.rightLeg.z += 12F * Mth.sin(lean);
        m.leftLeg.z += 12F * Mth.sin(lean);
        m.rightLeg.y -= 12F * (1 - Mth.cos(lean));
        m.leftLeg.y -= 12F * (1 - Mth.cos(lean));
        m.head.xRot -= lean * 0.55F;
        m.head.z -= 1.4F * r;
        m.rightArm.z -= 0.6F * r;
        m.leftArm.z -= 0.6F * r;
        // bounce of the upper body at each foot strike
        float bob = Math.abs(Mth.cos(f)) * 1.1F * r * a;
        m.head.y += bob;
        m.body.y += bob;
        m.rightArm.y += bob;
        m.leftArm.y += bob;
        m.hat.copyFrom(m.head);
    }

    private static void poseFall(HumanoidModel<?> m, LivingEntity e, float t, float age) {
        float c = fallCurve(t);
        boolean slip = ClientState.fallKind(e.getId()) == 1;
        float flail = t < 0.22F ? Mth.sin(age * 1.6F) * 0.4F : 0F;
        if (slip) {
            // arms thrown back, legs up
            m.rightArm.xRot = Mth.lerp(c, m.rightArm.xRot, -2.6F + flail);
            m.leftArm.xRot = Mth.lerp(c, m.leftArm.xRot, -2.4F - flail);
            m.rightArm.zRot = Mth.lerp(c, m.rightArm.zRot, 0.6F);
            m.leftArm.zRot = Mth.lerp(c, m.leftArm.zRot, -0.6F);
            m.rightLeg.xRot = Mth.lerp(c, m.rightLeg.xRot, -0.5F);
            m.leftLeg.xRot = Mth.lerp(c, m.leftLeg.xRot, -0.2F);
            m.head.xRot = Mth.lerp(c, m.head.xRot, -0.5F);
        } else {
            // face down, catching the fall with both hands
            m.rightArm.xRot = Mth.lerp(c, m.rightArm.xRot, -2.9F + flail);
            m.leftArm.xRot = Mth.lerp(c, m.leftArm.xRot, -2.9F - flail);
            m.rightArm.zRot = Mth.lerp(c, m.rightArm.zRot, 0.35F);
            m.leftArm.zRot = Mth.lerp(c, m.leftArm.zRot, -0.35F);
            m.rightLeg.xRot = Mth.lerp(c, m.rightLeg.xRot, 0.25F);
            m.leftLeg.xRot = Mth.lerp(c, m.leftLeg.xRot, -0.1F);
            m.head.xRot = Mth.lerp(c, m.head.xRot, -0.9F);
        }
    }

    // ------------------------------------------------------------------ whole-body tilt for the fall
    @SubscribeEvent
    public static void pre(RenderLivingEvent.Pre<?, ?> ev) {
        LivingEntity e = ev.getEntity();
        if (!applies(e)) return;
        float t = ClientState.fall(e.getId(), ev.getPartialTick());
        if (t < 0) return;
        float c = fallCurve(t);
        boolean slip = ClientState.fallKind(e.getId()) == 1;
        float bodyYaw = Mth.rotLerp(ev.getPartialTick(), e.yBodyRotO, e.yBodyRot);
        PoseStack ps = ev.getPoseStack();
        ps.pushPose();
        ps.translate(0, 0.18 * c, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        ps.mulPose(Axis.XP.rotationDegrees((slip ? -82F : 84F) * c));
        ps.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        TILTED.add(e.getId());
    }

    private static final java.util.Set<Integer> TILTED = new java.util.HashSet<>();

    @SubscribeEvent
    public static void post(RenderLivingEvent.Post<?, ?> ev) {
        if (TILTED.remove(ev.getEntity().getId())) ev.getPoseStack().popPose();
    }

    /** first person: the world lurches when you go down */
    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles ev) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float t = ClientState.fall(mc.player.getId(), (float) ev.getPartialTick());
        if (t < 0) return;
        float c = fallCurve(t);
        boolean slip = ClientState.fallKind(mc.player.getId()) == 1;
        ev.setPitch(ev.getPitch() + (slip ? -55F : 60F) * c);
        ev.setRoll(ev.getRoll() + (slip ? -14F : 16F) * c);
    }
}
