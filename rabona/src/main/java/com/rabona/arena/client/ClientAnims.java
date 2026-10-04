package com.rabona.arena.client;

import com.rabona.arena.client.anim.Anim;
import com.rabona.arena.client.anim.AnimLibrary;
import com.rabona.arena.game.Move;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/** Varlik basina aktif animasyonlar ve modele uygulanmasi. */
public final class ClientAnims {
    public record Active(Move move, Anim anim, float start, boolean mirror) {}

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();

    private ClientAnims() {}

    public static void play(LivingEntity e, Move m, int side) {
        Anim a = AnimLibrary.get(m);
        if (a == null) return;
        ACTIVE.put(e.getId(), new Active(m, a, e.tickCount, side < 0));
    }

    public static Active get(LivingEntity e, float age) {
        Active a = ACTIVE.get(e.getId());
        if (a == null) return null;
        float t = age - a.start();
        if (t < -2 || t > a.anim().length + 1) {
            if (t > a.anim().length + 1 || t < -40) ACTIVE.remove(e.getId());
            return null;
        }
        return a;
    }

    public static void clear() { ACTIVE.clear(); }

    /** Model kemiklerine animasyon uygula (setupAnim sonrasinda). */
    public static void apply(HumanoidModel<?> m, LivingEntity e, float age) {
        Active act = get(e, age);
        if (act == null) {
            ambient(m, e, age);
            return;
        }
        Anim a = act.anim();
        float t = Math.max(0, age - act.start());
        float w = a.weight(t);
        boolean mir = act.mirror();
        part(m.head, a, mir ? Anim.HEAD : Anim.HEAD, t, w, mir);
        part(m.body, a, Anim.BODY, t, w, mir);
        part(mir ? m.leftArm : m.rightArm, a, Anim.RARM, t, w, mir);
        part(mir ? m.rightArm : m.leftArm, a, Anim.LARM, t, w, mir);
        part(mir ? m.leftLeg : m.rightLeg, a, Anim.RLEG, t, w, mir);
        part(mir ? m.rightLeg : m.leftLeg, a, Anim.LLEG, t, w, mir);
        sync(m);
    }

    private static void part(ModelPart p, Anim a, int ch, float t, float w, boolean mir) {
        float[] v = a.eval(ch, t);
        if (v == null) return;
        float x = v[0] * Mth.DEG_TO_RAD, y = v[1] * Mth.DEG_TO_RAD, z = v[2] * Mth.DEG_TO_RAD;
        if (mir) {
            y = -y;
            z = -z;
        }
        // kollar/bacaklar icin vanilla sallanmayi kismen koru
        p.xRot = Mth.lerp(w, p.xRot, x);
        p.yRot = Mth.lerp(w, p.yRot, y);
        p.zRot = Mth.lerp(w, p.zRot, z);
    }

    /** Topla kosarken denge icin acik kollar, depar egilmesi. */
    private static void ambient(HumanoidModel<?> m, LivingEntity e, float age) {
        if (e.isSprinting() && !e.isPassenger() && !e.isFallFlying() && !e.isSwimming()) {
            m.body.xRot += 0.12f;
            m.head.xRot -= 0.1f;
            m.rightArm.xRot *= 1.25f;
            m.leftArm.xRot *= 1.25f;
            m.rightArm.zRot += 0.08f;
            m.leftArm.zRot -= 0.08f;
            sync(m);
        }
    }

    public static void sync(HumanoidModel<?> m) {
        m.hat.copyFrom(m.head);
        if (m instanceof PlayerModel<?> pm) {
            pm.jacket.copyFrom(pm.body);
            pm.rightSleeve.copyFrom(pm.rightArm);
            pm.leftSleeve.copyFrom(pm.leftArm);
            pm.rightPants.copyFrom(pm.rightLeg);
            pm.leftPants.copyFrom(pm.leftLeg);
        }
    }

    /** Kok donusu/konumu: {rx, ry, rz, px, py, pz} ya da null. */
    public static float[] root(LivingEntity e, float age) {
        Active act = get(e, age);
        if (act == null) return null;
        Anim a = act.anim();
        if (!a.has(Anim.ROOT) && !a.has(Anim.POS)) return null;
        float t = Math.max(0, age - act.start());
        float w = a.weight(t);
        float[] r = a.eval(Anim.ROOT, t);
        float[] p = a.eval(Anim.POS, t);
        float[] out = new float[7];
        if (r != null) {
            out[0] = wrap(r[0]) * w;
            out[1] = wrap(r[1]) * w;
            out[2] = wrap(r[2]) * w;
        }
        if (p != null) {
            out[3] = p[0] * w;
            out[4] = p[1] * w;
            out[5] = p[2] * w;
        }
        if (act.mirror()) {
            out[1] = -out[1];
            out[2] = -out[2];
            out[3] = -out[3];
        }
        out[6] = a.pivot;
        return out;
    }

    private static float wrap(float deg) {
        float d = deg % 360f;
        if (d > 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }
}
