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
    public static void apply(HumanoidModel<?> m, LivingEntity e, float age, float swing, float amount) {
        Active act = get(e, age);
        if (act == null) {
            ambient(m, e, age, swing, amount);
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

    /** Topu kontrol eden varliklar (her istemci tick'inde guncellenir). */
    public static final java.util.Set<Integer> CONTROLLERS = new java.util.HashSet<>();

    /**
     * Prosedurel atletik animasyon: kosuda govde donmesi ve one egilme, depar kol savurmasi,
     * top surerken asagi bakan bas ve dengede acik kollar, macta bekleme durusu, nefes.
     */
    private static void ambient(HumanoidModel<?> m, LivingEntity e, float age, float swing, float amount) {
        if (e.isPassenger() || e.isFallFlying() || e.isSwimming() || e.isSleeping()) return;
        boolean ball = CONTROLLERS.contains(e.getId());
        boolean athlete = e instanceof com.rabona.arena.entity.FootballerEntity || ClientState.teamOf(e).playing() || ball;
        if (!athlete) return;
        float sp = Mth.clamp(amount, 0, 1);
        float ph = swing * 0.6662f;
        boolean sprint = e.isSprinting();
        if (sp > 0.08f) {
            float lean = (sprint ? 0.24f : 0.1f) * sp;
            m.body.xRot += lean;
            m.body.yRot += Mth.sin(ph) * 0.2f * sp;
            m.head.xRot -= lean * 0.7f;
            m.head.yRot -= Mth.sin(ph) * 0.12f * sp;
            float armAmp = sprint ? 1.35f : 1.1f;
            m.rightArm.xRot = m.rightArm.xRot * armAmp - lean * 0.6f;
            m.leftArm.xRot = m.leftArm.xRot * armAmp - lean * 0.6f;
            m.rightArm.zRot += 0.12f * sp;
            m.leftArm.zRot -= 0.12f * sp;
            float legAmp = sprint ? 1.12f : 1f;
            m.rightLeg.xRot *= legAmp;
            m.leftLeg.xRot *= legAmp;
            m.rightLeg.xRot -= lean * 0.5f;
            m.leftLeg.xRot -= lean * 0.5f;
        } else {
            // hazir durus: ayaklar acik, govde hafif onde, kollar dengede, nefes
            float breathe = Mth.sin(age * 0.09f) * 0.025f;
            m.body.xRot += 0.1f + breathe;
            m.head.xRot -= 0.06f;
            m.rightLeg.zRot += 0.07f;
            m.leftLeg.zRot -= 0.07f;
            m.rightLeg.xRot -= 0.06f;
            m.leftLeg.xRot -= 0.06f;
            m.rightArm.zRot += 0.16f + breathe;
            m.leftArm.zRot -= 0.16f + breathe;
            m.rightArm.xRot -= 0.12f;
            m.leftArm.xRot -= 0.12f;
            m.head.yRot += Mth.sin(age * 0.03f) * 0.15f;
        }
        if (ball) {
            m.head.xRot += 0.38f;
            m.rightArm.zRot += 0.3f;
            m.leftArm.zRot -= 0.3f;
            m.body.xRot += 0.06f;
            m.rightLeg.xRot *= 0.85f;
            m.leftLeg.xRot *= 0.85f;
        }
        sync(m);
    }

    /** Tekrar icin: verilen baslangicla animasyon. */
    public static void playAt(LivingEntity e, Move m, float start, boolean mirror) {
        Anim a = AnimLibrary.get(m);
        if (a == null) return;
        Active cur = ACTIVE.get(e.getId());
        if (cur != null && cur.move() == m && Math.abs(cur.start() - start) < 0.01f) return;
        ACTIVE.put(e.getId(), new Active(m, a, start, mirror));
    }

    public static void clearFor(LivingEntity e) { ACTIVE.remove(e.getId()); }

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
        if (act == null) {
            // kosu zipla-yuru salinimi
            float partial = age - e.tickCount;
            float sp = e.walkAnimation.speed(partial);
            if (sp < 0.15f || !(e instanceof com.rabona.arena.entity.FootballerEntity || ClientState.teamOf(e).playing())) return null;
            float ph = e.walkAnimation.position(partial) * 0.6662f;
            float bob = Math.abs(Mth.sin(ph)) * 0.07f * Math.min(1, sp);
            return new float[]{0, 0, Mth.sin(ph) * 1.5f * sp, 0, bob, 0, 0.9f};
        }
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
