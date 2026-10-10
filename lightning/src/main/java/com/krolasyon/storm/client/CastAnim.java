package com.krolasyon.storm.client;

import com.krolasyon.storm.Skill;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Key-framed casting poses. Third person: applied at the end of HumanoidModel#setupAnim (mixin), blended with the
 * vanilla pose. First person: applied to the hand pose stack.
 */
public final class CastAnim {
    private record Active(Skill.Anim anim, long start) {}

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();

    private CastAnim() {}

    public static void start(int entityId, Skill skill) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || skill.anim == Skill.Anim.NONE) return;
        ACTIVE.put(entityId, new Active(skill.anim, mc.level.getGameTime()));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }
        long now = mc.level.getGameTime();
        ACTIVE.values().removeIf(a -> now - a.start > a.anim.duration + 2 || now < a.start);
    }

    private static float progress(Active a) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        float t = (mc.level.getGameTime() - a.start + mc.getFrameTime()) / a.anim.duration;
        return t >= 0 && t <= 1 ? t : -1;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** 0..1 window helper: rises over [a, b] */
    private static float seg(float t, float a, float b) { return smooth((t - a) / (b - a)); }

    private static float envelope(float t) { return seg(t, 0, 0.1F) * (1 - seg(t, 0.82F, 1F)); }

    private static void blend(ModelPart p, float w, float x, float y, float z) {
        p.xRot = Mth.lerp(w, p.xRot, x);
        p.yRot = Mth.lerp(w, p.yRot, y);
        p.zRot = Mth.lerp(w, p.zRot, z);
    }

    public static void apply(HumanoidModel<?> model, LivingEntity entity, float ageInTicks) {
        if (ACTIVE.isEmpty()) return;
        Active a = ACTIVE.get(entity.getId());
        if (a == null) return;
        float t = progress(a);
        if (t < 0) return;
        float w = envelope(t);
        float hx = model.head.xRot, hy = model.head.yRot;
        float aimX = -Mth.HALF_PI + hx;
        float tremble = Mth.sin(ageInTicks * 2.9F) * 0.06F;
        ModelPart r = model.rightArm, l = model.leftArm;
        switch (a.anim) {
            case STRIKE -> {
                float up = seg(t, 0, 0.38F), slam = seg(t, 0.38F, 0.52F);
                float rx = Mth.lerp(slam, Mth.lerp(up, -0.6F, -3.0F), aimX + 0.15F);
                float rz = Mth.lerp(slam, Mth.lerp(up, 0.1F, 0.25F), 0.0F);
                blend(r, w, rx + (1 - slam) * up * tremble, hy * slam, rz);
                blend(l, w, -0.35F, 0, -0.45F * up);
            }
            case THRUST -> {
                float back = seg(t, 0, 0.22F), go = seg(t, 0.22F, 0.36F);
                float rx = Mth.lerp(go, Mth.lerp(back, -0.4F, -1.05F), aimX);
                blend(r, w, rx, Mth.lerp(go, 0.35F, hy - 0.06F), 0.05F);
                blend(l, w, -0.5F * go, 0.25F, -0.25F);
            }
            case BURST -> {
                float gather = seg(t, 0, 0.35F), fling = seg(t, 0.35F, 0.5F);
                float x = Mth.lerp(fling, Mth.lerp(gather, -0.4F, -1.25F), -1.75F);
                float z = Mth.lerp(fling, -0.55F * gather, 1.45F);
                blend(r, w, x + (1 - fling) * tremble, 0, z);
                blend(l, w, x - (1 - fling) * tremble, 0, -z);
            }
            case DOME -> {
                float up = seg(t, 0, 0.4F), sweep = seg(t, 0.45F, 0.7F);
                float x = Mth.lerp(sweep, Mth.lerp(up, -0.3F, -3.05F), -0.55F);
                float z = Mth.lerp(sweep, -0.22F * up, 1.25F);
                blend(r, w, x, 0, z);
                blend(l, w, x, 0, -z);
            }
            case CIRCLE -> {
                float raise = seg(t, 0, 0.15F);
                float ang = t * Mth.TWO_PI * 2.2F;
                blend(r, w, Mth.lerp(raise, -0.3F, -1.6F + Mth.sin(ang) * 0.55F), 0, 0.15F + Mth.cos(ang) * 0.55F * raise);
                blend(l, w, -0.25F, 0, -0.35F);
            }
            case AVATAR -> {
                float cross = seg(t, 0, 0.3F), burst = seg(t, 0.3F, 0.42F);
                float x = Mth.lerp(burst, Mth.lerp(cross, -0.2F, -1.1F), -2.65F);
                float z = Mth.lerp(burst, -0.65F * cross, 0.75F);
                float shake = tremble * (cross * (1 - burst) * 2 + burst);
                blend(r, w, x + shake, 0, z);
                blend(l, w, x - shake, 0, -z);
                blend(model.head, w * burst, -0.55F, model.head.yRot, 0);
            }
            case GRASP -> {
                float reach = seg(t, 0, 0.2F), pull = seg(t, 0.62F, 0.78F);
                float rx = Mth.lerp(pull, Mth.lerp(reach, -0.5F, aimX - 0.05F), -0.85F);
                float held = reach * (1 - pull);
                blend(r, w, rx + held * tremble * 1.5F, Mth.lerp(reach, 0, hy), 0.05F);
                blend(l, w, Mth.lerp(pull, -0.75F * reach, -0.3F), 0.3F, -0.2F);
            }
            case PUSH -> {
                float pullIn = seg(t, 0, 0.3F), push = seg(t, 0.3F, 0.45F);
                float x = Mth.lerp(push, Mth.lerp(pullIn, -0.4F, -0.75F), aimX);
                blend(r, w, x, Mth.lerp(push, -0.65F * pullIn, hy - 0.32F), 0);
                blend(l, w, x, Mth.lerp(push, 0.65F * pullIn, hy + 0.32F), 0);
            }
            case NOVA -> {
                float rise = seg(t, 0, 0.45F), slam = seg(t, 0.45F, 0.53F);
                float shake = tremble * rise * (1 - slam) * 2.2F;
                float x = Mth.lerp(slam, Mth.lerp(rise, -0.3F, -3.0F), -0.25F);
                float z = Mth.lerp(slam, 0.18F * rise, 1.1F);
                blend(r, w, x + shake, 0, z);
                blend(l, w, x - shake, 0, -z);
                blend(model.rightLeg, w * slam, 0, 0, 0.3F);
                blend(model.leftLeg, w * slam, 0, 0, -0.3F);
                blend(model.head, w * rise * (1 - slam), -0.6F, model.head.yRot, 0);
            }
            case SPEAR -> {
                float wind = seg(t, 0, 0.35F), thr = seg(t, 0.38F, 0.46F);
                float rx = Mth.lerp(thr, Mth.lerp(wind, -0.5F, -2.7F) + wind * tremble, aimX + 0.1F);
                blend(r, w, rx, Mth.lerp(thr, 0.35F * wind, hy), Mth.lerp(thr, 0.35F * wind, 0));
                blend(l, w, Mth.lerp(thr, Mth.lerp(wind, -0.3F, aimX), 0.35F), Mth.lerp(thr, hy + 0.25F, 0), -0.15F);
            }
            default -> {}
        }
    }

    /** first person hand motion for the local player */
    public static void applyFirstPerson(PoseStack ps, int playerId) {
        Active a = ACTIVE.get(playerId);
        if (a == null) return;
        float t = progress(a);
        if (t < 0) return;
        float w = envelope(t);
        float tr = Mth.sin(t * 90F) * 0.012F;
        switch (a.anim) {
            case STRIKE -> {
                float up = seg(t, 0, 0.38F), slam = seg(t, 0.38F, 0.5F);
                ps.translate(0, w * (0.16F * up * (1 - slam) - 0.05F * slam), -w * 0.22F * slam);
                ps.mulPose(Axis.XP.rotationDegrees(w * (22 * up * (1 - slam) - 18 * slam)));
            }
            case THRUST, PUSH -> {
                float back = seg(t, 0, 0.25F), go = seg(t, 0.25F, 0.4F);
                ps.translate(-0.08F * w * go, 0.05F * w, w * (0.04F * back * (1 - go) - 0.35F * go));
                ps.mulPose(Axis.XP.rotationDegrees(w * 12 * go));
            }
            case BURST, NOVA -> {
                float gather = seg(t, 0, a.anim == Skill.Anim.NOVA ? 0.45F : 0.35F);
                float fling = seg(t, a.anim == Skill.Anim.NOVA ? 0.45F : 0.35F, a.anim == Skill.Anim.NOVA ? 0.53F : 0.5F);
                ps.translate(w * (-0.2F * gather * (1 - fling) + 0.25F * fling) + tr * gather, w * (0.15F * gather - 0.2F * fling), -0.1F * w);
                ps.mulPose(Axis.ZP.rotationDegrees(w * (25 * gather * (1 - fling) - 35 * fling)));
            }
            case DOME, AVATAR -> {
                float up = seg(t, 0, 0.4F), down = seg(t, 0.45F, 0.7F);
                ps.translate(tr, w * (0.18F * up * (1 - down) - 0.1F * down), -0.12F * w);
                ps.mulPose(Axis.XP.rotationDegrees(w * 25 * up * (1 - down)));
            }
            case CIRCLE -> {
                float ang = t * Mth.TWO_PI * 2.2F;
                ps.translate(w * Mth.cos(ang) * 0.12F, w * Mth.sin(ang) * 0.12F, -0.15F * w);
            }
            case GRASP -> {
                float reach = seg(t, 0, 0.2F), pull = seg(t, 0.62F, 0.78F);
                ps.translate(tr * reach * (1 - pull) * 2, 0.05F * w, w * (-0.35F * reach * (1 - pull) - 0.05F * pull));
                ps.mulPose(Axis.XP.rotationDegrees(w * (15 * reach - 30 * pull)));
            }
            case SPEAR -> {
                float wind = seg(t, 0, 0.35F), thr = seg(t, 0.38F, 0.46F);
                ps.translate(0.06F * w * wind * (1 - thr) + tr * wind * (1 - thr), w * (0.1F * wind * (1 - thr)), -w * (0.05F * wind * (1 - thr) + 0.4F * thr));
                ps.mulPose(Axis.ZP.rotationDegrees(w * -12 * wind * (1 - thr)));
                ps.mulPose(Axis.XP.rotationDegrees(w * 10 * thr));
            }
            default -> {}
        }
    }
}
