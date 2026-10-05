package com.krolasyon.sololeveling.client.model;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic procedurally animated model for every monster. Bones are found by name and animated by rig:
 * bipeds swing arms and legs, quadrupeds walk with four legs and a tail, serpents and centipedes ripple their segments,
 * and the synced special animation (cast, slam, roar, spin, leap, shoot, summon, bite) poses the whole body.
 */
public class SLModel extends HierarchicalModel<SLMonster> {
    private final ModelPart root;
    private final Map<String, ModelPart> parts = new HashMap<>();
    private final GenModels.Rig rig;
    private final int segments, legPairs, tails;

    public SLModel(ModelPart root, MobKind kind) {
        this.root = root;
        this.rig = GenModels.rig(kind);
        for (String path : GenModels.bones(kind)) {
            ModelPart p = root;
            String[] split = path.split("/");
            boolean ok = true;
            for (String s : split) {
                if (!p.hasChild(s)) {
                    ok = false;
                    break;
                }
                p = p.getChild(s);
            }
            if (ok) parts.put(split[split.length - 1], p);
        }
        int s = 0;
        while (parts.containsKey("seg" + s)) s++;
        segments = s;
        int l = 0;
        while (parts.containsKey("legl" + l)) l++;
        legPairs = l;
        int t = 0;
        while (parts.containsKey("tail" + t)) t++;
        tails = t;
    }

    @Override
    public ModelPart root() { return root; }

    private ModelPart p(String n) { return parts.get(n); }

    private static float ease(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    @Override
    public void setupAnim(SLMonster e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float amt = Math.min(1F, limbAmount * 1.4F);
        float w = limbSwing * rig.walkFreq();
        float sw = Mth.cos(w) * rig.walkAmp() * amt;
        float partial = age - (int) age;
        float attack = e.getAttackAnim(partial);
        int anim = e.getAnim();
        float t = anim == SLMonster.A_NONE ? 0 : e.animTime(partial);
        ModelPart head = p("head"), body = p("body"), jaw = p("jaw");
        float breathe = Mth.sin(age * 0.08F);
        // ---- look
        if (head != null) {
            head.yRot += headYaw * Mth.DEG_TO_RAD * 0.8F;
            head.xRot += headPitch * Mth.DEG_TO_RAD * 0.8F;
        }
        // ---- rig specific locomotion
        switch (rig.type()) {
            case BIPED -> biped(sw, w, amt, age, breathe);
            case QUAD -> quad(sw, w, amt, age, breathe);
            case SERPENT -> serpent(w, amt, age, attack);
            case CENTIPEDE -> centipede(w, amt, age);
        }
        // ---- generic secondary motion
        for (int i = 0; i < tails; i++) {
            ModelPart tp = p("tail" + i);
            tp.yRot += Mth.sin(age * 0.12F - i * 0.7F) * (0.12F + 0.25F * amt);
            tp.xRot += Mth.sin(age * 0.07F - i * 0.5F) * 0.05F;
        }
        ModelPart cape = p("cape");
        if (cape != null) cape.xRot += 0.08F + amt * 0.55F + Mth.sin(age * 0.11F) * 0.06F;
        for (String n : new String[]{"antenna_l", "antenna_r"}) {
            ModelPart a = p(n);
            if (a != null) a.xRot += Mth.sin(age * 0.3F + n.length()) * 0.12F;
        }
        for (String n : new String[]{"wing_l", "wing_r"}) {
            ModelPart wg = p(n);
            if (wg == null) continue;
            float s = n.endsWith("l") ? 1 : -1;
            float flap = anim == SLMonster.A_LEAP || !e.onGround() ? Mth.sin(age * 1.4F) * 0.9F : Mth.sin(age * 0.15F) * 0.08F;
            wg.zRot += s * (0.25F + flap);
            wg.yRot += s * 0.2F;
        }
        if (jaw != null) jaw.xRot += 0.05F + Mth.sin(age * 0.1F) * 0.03F + attack * 0.9F;
        for (String n : new String[]{"mandible_l", "mandible_r"}) {
            ModelPart m = p(n);
            if (m != null) m.yRot += (n.endsWith("l") ? -1 : 1) * (0.1F + Mth.sin(age * 0.5F) * 0.08F + attack * 0.5F);
        }
        for (String n : new String[]{"head2", "head3"}) {
            ModelPart h2 = p(n);
            if (h2 != null) {
                h2.yRot += headYaw * Mth.DEG_TO_RAD * 0.5F + Mth.sin(age * 0.09F + n.length()) * 0.15F;
                h2.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F;
            }
        }
        // ---- melee attack
        if (attack > 0) meleeAttack(attack);
        // ---- special animations
        if (anim != SLMonster.A_NONE) special(anim, t, age);
        if (e.isElite() || e.kind.boss) {
            // subtle menacing sway
            if (body != null) body.zRot += Mth.sin(age * 0.05F) * 0.015F;
        }
    }

    private void biped(float sw, float w, float amt, float age, float breathe) {
        ModelPart ll = p("leg_l"), lr = p("leg_r"), al = p("arm_l"), ar = p("arm_r"), body = p("body");
        if (ll != null) ll.xRot += sw;
        if (lr != null) lr.xRot -= sw;
        ModelPart sl = p("shin_l"), sr = p("shin_r");
        if (sl != null) sl.xRot += Math.max(0, -Mth.sin(w)) * 0.8F * amt;
        if (sr != null) sr.xRot += Math.max(0, Mth.sin(w)) * 0.8F * amt;
        if (al != null) {
            al.xRot -= sw * 0.8F;
            al.zRot -= 0.06F + breathe * 0.04F;
        }
        if (ar != null) {
            ar.xRot += sw * 0.8F;
            ar.zRot += 0.06F + breathe * 0.04F;
        }
        ModelPart fl = p("forearm_l"), fr = p("forearm_r");
        if (fl != null) fl.xRot -= 0.15F + amt * 0.25F;
        if (fr != null) fr.xRot -= 0.15F + amt * 0.25F;
        if (body != null) {
            body.yRot += Mth.sin(w) * 0.06F * amt;
            body.xRot += breathe * 0.015F + amt * rig.lean();
        }
        root.y += -Math.abs(Mth.sin(w)) * 0.8F * amt * rig.walkAmp();
    }

    private void quad(float sw, float w, float amt, float age, float breathe) {
        ModelPart fl = p("fleg_l"), fr = p("fleg_r"), bl = p("bleg_l"), br = p("bleg_r"), body = p("body");
        if (fl != null) fl.xRot += sw;
        if (br != null) br.xRot += sw;
        if (fr != null) fr.xRot -= sw;
        if (bl != null) bl.xRot -= sw;
        ModelPart pl = p("fpaw_l"), pr = p("fpaw_r");
        if (pl != null) pl.xRot += Math.max(0, Mth.sin(w)) * 0.6F * amt;
        if (pr != null) pr.xRot += Math.max(0, -Mth.sin(w)) * 0.6F * amt;
        if (body != null) body.xRot += Mth.sin(w * 2) * 0.03F * amt + breathe * 0.01F;
        root.y += -Math.abs(Mth.cos(w)) * 0.6F * amt;
    }

    private void serpent(float w, float amt, float age, float attack) {
        for (int i = 0; i < segments; i++) {
            ModelPart s = p("seg" + i);
            float phase = age * 0.15F + w * 0.6F - i * 0.65F;
            s.yRot += Mth.sin(phase) * (0.12F + 0.18F * amt) * (i == 0 ? 0.3F : 1F);
        }
        ModelPart neck = p("neck");
        if (neck != null) {
            neck.xRot += Mth.sin(age * 0.06F) * 0.05F - attack * 0.5F;
        }
    }

    private void centipede(float w, float amt, float age) {
        for (int i = 0; i < segments; i++) {
            ModelPart s = p("seg" + i);
            s.yRot += Mth.sin(age * 0.2F + w * 0.5F - i * 0.5F) * (0.05F + 0.12F * amt);
        }
        for (int i = 0; i < legPairs; i++) {
            ModelPart l = p("legl" + i), r = p("legr" + i);
            float ph = w * 2.2F + i * 1.1F;
            if (l != null) {
                l.yRot += Mth.sin(ph) * 0.5F * Math.max(0.2F, amt);
                l.zRot += Mth.cos(ph) * 0.2F;
            }
            if (r != null) {
                r.yRot -= Mth.sin(ph) * 0.5F * Math.max(0.2F, amt);
                r.zRot -= Mth.cos(ph) * 0.2F;
            }
        }
    }

    private void meleeAttack(float a) {
        float s = Mth.sin(a * Mth.PI);
        float s2 = Mth.sin(Mth.sqrt(a) * Mth.PI * 2);
        ModelPart ar = p("arm_r"), al = p("arm_l"), body = p("body"), head = p("head");
        switch (rig.type()) {
            case BIPED -> {
                if (ar != null) {
                    ar.xRot -= s * 2.1F + 0.2F;
                    ar.yRot += s2 * 0.35F;
                    ar.zRot += s * 0.2F;
                }
                if (rig.twoHanded() && al != null) {
                    al.xRot -= s * 2.0F + 0.2F;
                    al.yRot -= s2 * 0.3F;
                }
                if (body != null) body.yRot += s2 * 0.25F;
            }
            case QUAD -> {
                if (head != null) head.xRot += s * 0.6F;
                ModelPart fl = p("fleg_l"), fr = p("fleg_r");
                if (fl != null) fl.xRot -= s * 0.9F;
                if (fr != null) fr.xRot -= s * 0.7F;
                if (body != null) body.xRot -= s * 0.1F;
            }
            case SERPENT -> {
                ModelPart neck = p("neck");
                if (neck != null) neck.xRot += s * 0.8F;
            }
            case CENTIPEDE -> {
                if (head != null) head.xRot += s * 0.5F;
            }
        }
    }

    private void special(int anim, float t, float age) {
        ModelPart ar = p("arm_r"), al = p("arm_l"), body = p("body"), head = p("head"), jaw = p("jaw");
        ModelPart fl = p("fleg_l"), fr = p("fleg_r"), neck = p("neck");
        switch (anim) {
            case SLMonster.A_CAST -> {
                float k = ease(t / 8F);
                if (ar != null) {
                    ar.xRot = -2.5F * k + Mth.sin(age * 0.5F) * 0.05F;
                    ar.zRot = 0.35F * k;
                }
                if (al != null) {
                    al.xRot = -2.5F * k + Mth.cos(age * 0.5F) * 0.05F;
                    al.zRot = -0.35F * k;
                }
                if (head != null) head.xRot -= 0.3F * k;
                if (body != null) body.xRot -= 0.1F * k;
                if (neck != null) neck.xRot -= 0.4F * k;
                if (jaw != null) jaw.xRot += 0.5F * k;
            }
            case SLMonster.A_SLAM -> {
                float up = ease(t / 10F), down = ease((t - 10) / 4F);
                float x = -2.9F * up + 3.2F * down;
                if (ar != null) ar.xRot = x;
                if (al != null) al.xRot = x;
                if (body != null) body.xRot += -0.25F * up + 0.6F * down;
                if (fl != null) fl.xRot -= 1.4F * up - 1.6F * down;
                if (fr != null) fr.xRot -= 1.4F * up - 1.6F * down;
                if (rig.type() == GenModels.RigType.QUAD) root.y -= 6 * up * (1 - down);
            }
            case SLMonster.A_ROAR -> {
                float k = ease(t / 6F) * (1 - ease((t - 24) / 6F));
                if (head != null) head.xRot -= 0.7F * k;
                if (jaw != null) jaw.xRot += 0.9F * k;
                if (neck != null) neck.xRot -= 0.6F * k;
                if (ar != null) {
                    ar.zRot += 1.1F * k;
                    ar.xRot -= 0.4F * k;
                }
                if (al != null) {
                    al.zRot -= 1.1F * k;
                    al.xRot -= 0.4F * k;
                }
                if (body != null) body.xRot -= 0.25F * k;
                if (head != null) head.zRot += Mth.sin(age * 2F) * 0.06F * k;
            }
            case SLMonster.A_SPIN -> {
                root.yRot += t * 0.75F;
                if (ar != null) {
                    ar.zRot += 1.4F;
                    ar.xRot -= 0.3F;
                }
                if (al != null) al.zRot -= 1.4F;
            }
            case SLMonster.A_LEAP -> {
                float k = ease(t / 4F);
                if (ar != null) ar.xRot -= 1.6F * k;
                if (al != null) al.xRot -= 1.6F * k;
                if (body != null) body.xRot += 0.35F * k;
                ModelPart ll = p("leg_l"), lr = p("leg_r");
                if (ll != null) ll.xRot -= 0.8F * k;
                if (lr != null) lr.xRot += 0.5F * k;
                if (fl != null) fl.xRot -= 1.0F * k;
                if (fr != null) fr.xRot -= 1.0F * k;
                ModelPart bl = p("bleg_l"), br = p("bleg_r");
                if (bl != null) bl.xRot += 0.9F * k;
                if (br != null) br.xRot += 0.9F * k;
                if (jaw != null) jaw.xRot += 0.6F * k;
                if (neck != null) neck.xRot += 0.5F * k;
            }
            case SLMonster.A_SHOOT -> {
                float k = ease(t / 6F);
                if (ar != null) {
                    ar.xRot = -1.55F * k + (head != null ? head.xRot : 0);
                    ar.yRot = -0.1F;
                }
                if (al != null) al.xRot -= 0.5F * k;
                if (head != null) head.xRot -= 0.1F * k;
                if (jaw != null) jaw.xRot += 0.7F * k;
                if (neck != null) neck.xRot -= 0.3F * k;
            }
            case SLMonster.A_SUMMON -> {
                float k = ease(t / 10F);
                if (ar != null) {
                    ar.xRot = -2.0F * k;
                    ar.zRot = 0.9F * k;
                }
                if (al != null) {
                    al.xRot = -2.0F * k;
                    al.zRot = -0.9F * k;
                }
                if (head != null) head.xRot -= 0.5F * k;
                if (body != null) body.xRot -= 0.2F * k;
                if (jaw != null) jaw.xRot += 0.6F * k;
            }
            case SLMonster.A_BITE -> {
                float k = Mth.sin(Mth.clamp(t / 12F, 0, 1) * Mth.PI);
                if (head != null) head.xRot += 0.5F * k;
                if (jaw != null) jaw.xRot += 0.9F * k;
                if (neck != null) neck.xRot += 0.9F * k;
            }
            default -> {}
        }
    }
}
