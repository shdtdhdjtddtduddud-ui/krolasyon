package com.krolasyon.futbol.client;

import com.krolasyon.futbol.anim.Anim;
import com.krolasyon.futbol.anim.Anims;
import com.krolasyon.futbol.anim.Part;
import com.krolasyon.futbol.game.Move;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/** Plays the synced move animations on any humanoid model (players and bots). */
public final class ClientAnims {
    private ClientAnims() {}

    private record Playing(Move move, float start, int variant) {}

    private static final Map<Integer, Playing> PLAYING = new HashMap<>();
    private static final Part[] LIMBS = {Part.HEAD, Part.BODY, Part.RARM, Part.LARM, Part.RLEG, Part.LLEG};
    private static final float[] TMP = new float[3];

    public static void start(LivingEntity e, Move m, int variant) { PLAYING.put(e.getId(), new Playing(m, e.tickCount, variant)); }

    public static void clear() { PLAYING.clear(); }

    public static boolean isPlaying(LivingEntity e) { return PLAYING.containsKey(e.getId()); }

    private static Playing current(LivingEntity e, float age) {
        Playing p = PLAYING.get(e.getId());
        if (p == null) return null;
        Anim a = Anims.get(p.move);
        float t = age - p.start;
        if (t > a.length || t < -2) {
            PLAYING.remove(e.getId());
            return null;
        }
        return p;
    }

    private static Part mirror(Part p) {
        return switch (p) {
            case RARM -> Part.LARM;
            case LARM -> Part.RARM;
            case RLEG -> Part.LLEG;
            case LLEG -> Part.RLEG;
            default -> p;
        };
    }

    private static ModelPart part(HumanoidModel<?> m, Part p) {
        return switch (p) {
            case HEAD -> m.head;
            case BODY -> m.body;
            case RARM -> m.rightArm;
            case LARM -> m.leftArm;
            case RLEG -> m.rightLeg;
            default -> m.leftLeg;
        };
    }

    public static void applyLimbs(HumanoidModel<?> model, LivingEntity e, float age) {
        if (ClientState.HOLDERS.contains(e.getId())) {
            model.rightArm.xRot = -1.15F;
            model.leftArm.xRot = -1.15F;
            model.rightArm.zRot = 0.25F;
            model.leftArm.zRot = -0.25F;
            model.rightArm.yRot = -0.2F;
            model.leftArm.yRot = 0.2F;
        } else if (ClientState.CONTROLLERS.contains(e.getId())) {
            model.rightArm.zRot += 0.14F;
            model.leftArm.zRot -= 0.14F;
            model.head.xRot = Math.max(model.head.xRot, 0.25F);
        }
        Playing p = current(e, age);
        if (p == null) return;
        Anim a = Anims.get(p.move);
        float t = age - p.start;
        float w = a.weight(t);
        boolean mir = p.variant == 1;
        for (Part part : LIMBS) {
            Part src = mir ? mirror(part) : part;
            if (!a.has(src)) continue;
            a.sample(src, t, TMP);
            float x = TMP[0] * Mth.DEG_TO_RAD, y = TMP[1] * Mth.DEG_TO_RAD, z = TMP[2] * Mth.DEG_TO_RAD;
            if (mir) {
                y = -y;
                z = -z;
            }
            ModelPart mp = part(model, part);
            mp.xRot = Mth.lerp(w, mp.xRot, x);
            mp.yRot = Mth.lerp(w, mp.yRot, y);
            mp.zRot = Mth.lerp(w, mp.zRot, z);
        }
    }

    /** whole body rotation / offset around the hips, applied before the renderer's own transforms */
    public static void applyRoot(PoseStack ps, LivingEntity e, float partial) {
        float age = e.tickCount + partial;
        Playing p = current(e, age);
        if (p == null) return;
        Anim a = Anims.get(p.move);
        if (!a.has(Part.ROT) && !a.has(Part.POS)) return;
        float t = age - p.start;
        boolean mir = p.variant == 1;
        float[] r = new float[3], q = new float[3];
        a.sample(Part.ROT, t, r);
        a.sample(Part.POS, t, q);
        if (mir) {
            r[1] = -r[1];
            r[2] = -r[2];
            q[0] = -q[0];
        }
        float s = e.getScale();
        float yaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.translate(q[0] / 16F * s, q[1] / 16F * s, q[2] / 16F * s);
        ps.translate(0, 0.9F * s, 0);
        ps.mulPose(Axis.YP.rotationDegrees(r[1]));
        ps.mulPose(Axis.XP.rotationDegrees(r[0]));
        ps.mulPose(Axis.ZP.rotationDegrees(r[2]));
        ps.translate(0, -0.9F * s, 0);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
    }
}
