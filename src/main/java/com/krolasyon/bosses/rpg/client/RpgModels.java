package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.rpg.mob.AbilityLogic.Style;
import com.krolasyon.bosses.rpg.mob.RpgAnimatable;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/** The twelve archetype models. Geometry is authored at scale 1; renderers scale per monster. */
public final class RpgModels {
    private RpgModels() {}

    private static LayerDefinition done(MeshDefinition mesh) { return LayerDefinition.create(mesh, 128, 128); }

    private static CubeListBuilder c(int u, int v) { return CubeListBuilder.create().texOffs(u, v); }

    private static final int AU = RpgMobModel.AU, AV = RpgMobModel.AV, BU = RpgMobModel.BU, BV = RpgMobModel.BV, CU = RpgMobModel.CU,
            CV = RpgMobModel.CV, DU = RpgMobModel.DU, DV = RpgMobModel.DV, EU = RpgMobModel.EU, EV = RpgMobModel.EV, FU = RpgMobModel.FU, FV = RpgMobModel.FV;

    private static PartDefinition add(PartDefinition p, String n, CubeListBuilder c, float x, float y, float z) { return RpgMobModel.add(p, n, c, x, y, z); }

    private static PartDefinition add(PartDefinition p, String n, CubeListBuilder c, float x, float y, float z, float rx, float ry, float rz) {
        return RpgMobModel.add(p, n, c, x, y, z, rx, ry, rz);
    }

    private static PartDefinition group(PartDefinition p, String n, float x, float y, float z) { return RpgMobModel.group(p, n, x, y, z); }

    /** spikes along a back line */
    private static void spikes(PartDefinition parent, String name, float y, float z0, float z1, int n, float size) {
        PartDefinition g = group(parent, name, 0, y, 0);
        for (int i = 0; i < n; i++) {
            float z = n == 1 ? z0 : z0 + (z1 - z0) * i / (n - 1);
            add(g, "s" + i, c(CU, CV + 16).addBox(-size * 0.5F, -size * 1.6F, -size * 0.5F, size, size * 1.6F, size), 0, 0, z, -0.45F, 0, 0);
        }
    }

    private static void wingPair(PartDefinition parent, float x, float y, float z, float len, float wid) {
        PartDefinition g = group(parent, "wings", 0, y, z);
        PartDefinition l = add(g, "wing_l", c(FU, FV).addBox(0, -0.5F, 0, len, 1, wid), x, 0, 0, 0, 0, -0.3F);
        add(l, "wing_l_tip", c(FU + 60, FV).addBox(0, -0.25F, 0, len * 0.8F, 0.5F, wid * 0.8F), len, 0, 0.5F, 0, 0, 0.3F);
        PartDefinition r = add(g, "wing_r", c(FU, FV).mirror().addBox(-len, -0.5F, 0, len, 1, wid), -x, 0, 0, 0, 0, 0.3F);
        add(r, "wing_r_tip", c(FU + 60, FV).mirror().addBox(-len * 0.8F, -0.25F, 0, len * 0.8F, 0.5F, wid * 0.8F), -len, 0, 0.5F, 0, 0, -0.3F);
    }

    private static void flapWings(RpgMobModel<?> m, float age, boolean air, float speed) {
        ModelPart l = m.find("wing_l"), r = m.find("wing_r");
        if (l == null || r == null) return;
        float f = air ? Mth.sin(age * speed) * 0.9F : Mth.sin(age * 0.08F) * 0.08F - 0.9F;
        l.zRot = -f - 0.2F;
        r.zRot = f + 0.2F;
        ModelPart lt = m.find("wing_l_tip"), rt = m.find("wing_r_tip");
        if (lt != null && rt != null) {
            float g = air ? Mth.sin(age * speed - 0.8F) * 0.5F : 1.6F;
            lt.zRot = -g * 0.6F;
            rt.zRot = g * 0.6F;
        }
        if (!air) { l.yRot = -0.9F; r.yRot = 0.9F; }
    }

    private static void tail(PartDefinition parent, float y, float z, float rx, float len, float th) {
        PartDefinition t = add(parent, "tail", c(AU + 40, AV + 32).addBox(-th * 0.5F, -th * 0.5F, 0, th, th, len), 0, y, z, rx, 0, 0);
        PartDefinition t2 = add(t, "tail_mid", c(AU + 40, AV + 32).addBox(-th * 0.4F, -th * 0.4F, 0, th * 0.8F, th * 0.8F, len * 0.8F), 0, 0, len - 0.5F, 0.25F, 0, 0);
        add(t2, "tail_tip", c(CU, CV + 24).addBox(-th * 0.5F, -th * 0.5F, 0, th, th, th * 1.5F), 0, 0, len * 0.8F - 0.5F);
    }

    private static void swayTail(RpgMobModel<?> m, float age, float amt) {
        ModelPart t = m.find("tail"), mid = m.find("tail_mid");
        if (t != null) t.yRot += Mth.sin(age * 0.12F) * 0.3F + Mth.sin(age * 0.6F) * 0.1F * amt;
        if (mid != null) mid.yRot += Mth.sin(age * 0.12F - 0.7F) * 0.35F;
    }

    // =================================================================================== HUMANOID
    public static class Humanoid<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart head, body, ra, la, rl, ll;

        public Humanoid(ModelPart root) {
            super(root);
            head = find("head"); body = find("body"); ra = find("right_arm"); la = find("left_arm"); rl = find("right_leg"); ll = find("left_leg");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition body = add(root, "body", c(AU, AV).addBox(-4, 0, -2, 8, 12, 4), 0, -24, 0);
            add(body, "belt", c(EU, EV).addBox(-4.5F, 9, -2.5F, 9, 2, 5), 0, 0, 0);
            PartDefinition head = add(root, "head", c(AU, AV + 16).addBox(-4, -8, -4, 8, 8, 8), 0, -24, 0);
            RpgMobModel.eyes(head, -5, -4, 1, 2, 1);
            add(head, "mouth", c(EU, EV).addBox(-2, -2.5F, -4.4F, 4, 1, 1), 0, 0, 0);
            add(head, "mane", c(AU, AV + 32).addBox(-5, -9, -1, 10, 11, 6), 0, 0, 0);
            RpgMobModel.headParts(head, -8, -4, 4, 8);
            PartDefinition ra = add(root, "right_arm", c(BU, BV).addBox(-3, -2, -2, 4, 12, 4), -5, -22, 0);
            PartDefinition la = add(root, "left_arm", c(BU, BV).mirror().addBox(-1, -2, -2, 4, 12, 4), 5, -22, 0);
            PartDefinition cr = group(ra, "claws", -1, 10, -1);
            PartDefinition cl = group(la, "claws2", 1, 10, -1);
            for (int i = 0; i < 3; i++) {
                add(cr, "c" + i, c(CU + 24, CV).addBox(-0.5F, 0, -0.5F, 1, 3, 1), -1 + i, 0, -0.5F, -0.3F, 0, 0);
                add(cl, "c" + i, c(CU + 24, CV).addBox(-0.5F, 0, -0.5F, 1, 3, 1), -1 + i, 0, -0.5F, -0.3F, 0, 0);
            }
            add(root, "right_leg", c(BU + 16, BV + 16).addBox(-2, 0, -2, 4, 12, 4), -1.9F, -12, 0);
            add(root, "left_leg", c(BU + 16, BV + 16).mirror().addBox(-2, 0, -2, 4, 12, 4), 1.9F, -12, 0);
            spikes(body, "spikes", 1, 2.5F, 2.5F, 1, 1.6F);
            PartDefinition sp = group(body, "spikes2", 0, 5, 2.5F);
            add(sp, "s", c(CU, CV + 16).addBox(-0.8F, -2.5F, -0.8F, 1.6F, 2.5F, 1.6F), 0, 0, 0, -0.6F, 0, 0);
            add(body, "shell", c(CU, CV + 32).addBox(-4.5F, -0.5F, 2, 9, 11, 2), 0, 0, 0);
            tail(body, 10, 1.5F, 0.9F, 7, 2);
            wingPair(body, 1.5F, 2, 2.2F, 12, 9);
            add(body, "hood2", c(BU, BV + 48).addBox(-4.5F, 0, 2, 9, 15, 1), 0, 0, 0);
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            look(head, yaw, pitch);
            rl.xRot = swing(limb, 0.6662F, 0) * 1.4F * amt;
            ll.xRot = swing(limb, 0.6662F, Mth.PI) * 1.4F * amt;
            ra.xRot = swing(limb, 0.6662F, Mth.PI) * amt;
            la.xRot = swing(limb, 0.6662F, 0) * amt;
            ra.zRot = Mth.cos(age * 0.09F) * 0.05F + 0.05F;
            la.zRot = -Mth.cos(age * 0.09F) * 0.05F - 0.05F;
            ra.xRot += Mth.sin(age * 0.067F) * 0.05F;
            la.xRot -= Mth.sin(age * 0.067F) * 0.05F;
            melee(ra, la, this.attackTime);
            armsCast(ra, la, head, body, style, charge, release, ca);
            swayTail(this, age, amt);
            flapWings(this, age, e.airborne(), 0.9F);
            if (e.airborne()) { rl.xRot = 0.3F; ll.xRot = 0.2F; root.y += Mth.sin(age * 0.15F) * 1.5F; }
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== BRUTE
    public static class Brute<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart head, body, ra, la, rl, ll, jaw;

        public Brute(ModelPart root) {
            super(root);
            head = find("head"); body = find("body"); ra = find("right_arm"); la = find("left_arm"); rl = find("right_leg"); ll = find("left_leg"); jaw = find("jaw");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition body = add(root, "body", c(AU, AV).addBox(-7, -14, -4.5F, 14, 14, 9), 0, -18, 0, 0.35F, 0, 0);
            add(body, "belly", c(BU, BV + 40).addBox(-5.5F, -6, -5.2F, 11, 7, 2), 0, 0, 0);
            add(body, "hips", c(EU, EV).addBox(-6, -1, -4, 12, 3, 8), 0, 0, 0);
            PartDefinition head = add(body, "head", c(AU, AV + 24).addBox(-4, -7, -7, 8, 8, 8), 0, -13, -3, -0.35F, 0, 0);
            RpgMobModel.eyes(head, -4.5F, -7, 1, 2, 1);
            PartDefinition jaw = add(head, "jaw", c(EU, EV + 8).addBox(-3.5F, 0, -6.5F, 7, 2, 7), 0, 1, 0);
            add(jaw, "teeth", c(CU + 32, CV).addBox(-3, -1, -6.4F, 6, 1, 1), 0, 0, 0);
            RpgMobModel.headParts(head, -7, -7, 4, 8);
            add(body, "mane", c(AU, AV + 40).addBox(-7.5F, -15.5F, -2, 15, 6, 9), 0, 0, 0);
            PartDefinition ra = add(root, "right_arm", c(BU, BV).addBox(-5, -2, -3, 6, 18, 6), -8.5F, -29, -2);
            PartDefinition la = add(root, "left_arm", c(BU, BV).mirror().addBox(-1, -2, -3, 6, 18, 6), 8.5F, -29, -2);
            add(ra, "fist_r", c(BU + 24, BV + 24).addBox(-5.5F, 14, -3.5F, 7, 5, 7), 0, 0, 0);
            add(la, "fist_l", c(BU + 24, BV + 24).mirror().addBox(-1.5F, 14, -3.5F, 7, 5, 7), 0, 0, 0);
            PartDefinition cr = group(ra, "claws", -2, 19, -1);
            PartDefinition cl = group(la, "claws2", 2, 19, -1);
            for (int i = 0; i < 3; i++) {
                add(cr, "c" + i, c(CU + 24, CV).addBox(-0.5F, 0, -0.5F, 1, 4, 1), -2 + i * 2, 0, -1, -0.4F, 0, 0);
                add(cl, "c" + i, c(CU + 24, CV).addBox(-0.5F, 0, -0.5F, 1, 4, 1), -2 + i * 2, 0, -1, -0.4F, 0, 0);
            }
            add(root, "right_leg", c(BU + 24, BV + 40).addBox(-3, 0, -3, 6, 10, 6), -4, -10, 0);
            add(root, "left_leg", c(BU + 24, BV + 40).mirror().addBox(-3, 0, -3, 6, 10, 6), 4, -10, 0);
            spikes(body, "spikes", -14, 0, 3.5F, 3, 2.2F);
            add(body, "shell", c(CU, CV + 32).addBox(-7.5F, -14.5F, 3.5F, 15, 13, 3), 0, 0, 0);
            tail(body, -1, 3.5F, 1.1F, 9, 3);
            wingPair(body, 3, -12, 4, 16, 12);
            add(body, "hood2", c(BU, BV + 48).addBox(-7.5F, -14.5F, 4.5F, 15, 18, 1), 0, 0, 0);
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            look(head, yaw * 0.7F, pitch * 0.6F);
            float sp = 0.55F;
            rl.xRot = swing(limb, sp, 0) * 1.0F * amt;
            ll.xRot = swing(limb, sp, Mth.PI) * 1.0F * amt;
            ra.xRot = swing(limb, sp, Mth.PI) * 0.8F * amt - 0.1F;
            la.xRot = swing(limb, sp, 0) * 0.8F * amt - 0.1F;
            ra.zRot = 0.15F + Mth.sin(age * 0.07F) * 0.04F;
            la.zRot = -0.15F - Mth.sin(age * 0.07F) * 0.04F;
            body.y += Mth.abs(Mth.cos(limb * sp)) * 1.2F * amt + Mth.sin(age * 0.08F) * 0.3F;
            body.zRot = Mth.cos(limb * sp) * 0.06F * amt;
            if (jaw != null) jaw.xRot = 0.1F + Mth.sin(age * 0.05F) * 0.06F;
            melee(ra, la, this.attackTime);
            if (this.attackTime > 0) la.xRot -= Mth.sin(this.attackTime * Mth.PI) * 1.2F;
            armsCast(ra, la, head, body, style, charge, release, ca);
            if (jaw != null && (style == Style.ROAR || style == Style.BREATH)) jaw.xRot += 0.7F * charge;
            swayTail(this, age, amt);
            flapWings(this, age, e.airborne(), 0.6F);
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== QUADRUPED
    public static class Quadruped<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart head, body, jaw, fr, fl, br, bl;

        public Quadruped(ModelPart root) {
            super(root);
            head = find("head"); body = find("body"); jaw = find("jaw");
            fr = find("leg_fr"); fl = find("leg_fl"); br = find("leg_br"); bl = find("leg_bl");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition body = add(root, "body", c(AU, AV).addBox(-5, -4, -9, 10, 8, 18), 0, -13, 0);
            add(body, "chest", c(BU, BV + 40).addBox(-4.5F, -1, -9.5F, 9, 6, 7), 0, 0, 0);
            PartDefinition neck = add(body, "neck", c(AU, AV + 26).addBox(-3, -3, -5, 6, 6, 5), 0, -2, -8, -0.4F, 0, 0);
            PartDefinition head = add(neck, "head", c(AU + 22, AV + 26).addBox(-4, -4, -6, 8, 7, 7), 0, -1, -4, 0.4F, 0, 0);
            add(head, "snout", c(BU, BV).addBox(-2.5F, -1, -10, 5, 3, 4), 0, 0, 0);
            add(head, "nose", c(EU, EV).addBox(-1, -1.3F, -10.3F, 2, 1, 1), 0, 0, 0);
            PartDefinition jaw = add(head, "jaw", c(EU, EV + 8).addBox(-2.5F, 0, -4, 5, 1.5F, 4), 0, 2, -6);
            add(jaw, "teeth", c(CU + 32, CV).addBox(-2, -0.8F, -3.8F, 4, 1, 1), 0, 0, 0);
            RpgMobModel.eyes(head, -2.5F, -6, 1.2F, 1.8F, 1);
            RpgMobModel.headParts(head, -4, -6, 4, 7);
            add(head, "mane", c(AU, AV + 46).addBox(-5, -5.5F, -1, 10, 9, 6), 0, 0, 0);
            add(body, "mane2", c(AU, AV + 46).addBox(-5.5F, -5.5F, -9, 11, 6, 8), 0, 0, 0);
            add(root, "leg_fr", c(BU, BV + 16).addBox(-2, 0, -2, 4, 9, 4), -3.2F, -9, -6);
            add(root, "leg_fl", c(BU, BV + 16).mirror().addBox(-2, 0, -2, 4, 9, 4), 3.2F, -9, -6);
            add(root, "leg_br", c(BU + 16, BV + 16).addBox(-2, 0, -2, 4, 9, 4), -3.2F, -9, 7);
            add(root, "leg_bl", c(BU + 16, BV + 16).mirror().addBox(-2, 0, -2, 4, 9, 4), 3.2F, -9, 7);
            PartDefinition claws = group(root, "claws", 0, -0.5F, -8.5F);
            for (int i = 0; i < 2; i++) {
                float x = i == 0 ? -3.2F : 3.2F;
                add(claws, "c" + i, c(CU + 24, CV).addBox(-1.5F, -0.5F, -1, 3, 1, 1.5F), x, 0, 0);
            }
            spikes(body, "spikes", -4, -7, 7, 5, 1.8F);
            add(body, "shell", c(CU, CV + 32).addBox(-5.5F, -5.5F, -7, 11, 3, 15), 0, 0, 0);
            tail(body, -2.5F, 8.5F, -0.6F, 8, 2.5F);
            wingPair(body, 4.5F, -4, -5, 18, 12);
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            look(head, yaw * 0.8F, pitch * 0.8F);
            float sp = 0.6662F;
            fr.xRot = swing(limb, sp, 0) * 1.3F * amt;
            bl.xRot = swing(limb, sp, 0) * 1.3F * amt;
            fl.xRot = swing(limb, sp, Mth.PI) * 1.3F * amt;
            br.xRot = swing(limb, sp, Mth.PI) * 1.3F * amt;
            body.y += Mth.sin(limb * sp * 2) * 0.5F * amt;
            body.xRot = Mth.sin(age * 0.06F) * 0.015F;
            if (jaw != null) jaw.xRot = 0.05F + Math.max(0F, Mth.sin(age * 0.07F)) * 0.08F;
            if (this.attackTime > 0) {
                float s = Mth.sin(this.attackTime * Mth.PI);
                head.xRot += 0.4F * s;
                if (jaw != null) jaw.xRot += 0.8F * s;
                body.xRot += 0.1F * s;
            }
            if (style != null) {
                switch (style) {
                    case ROAR, BREATH -> { head.xRot -= (style == Style.ROAR ? 0.7F : -0.1F) * charge; if (jaw != null) jaw.xRot += 0.9F * charge; body.xRot -= 0.15F * charge; }
                    case LUNGE, SWIPE -> {
                        body.xRot += (release > 0 ? -0.35F : 0.25F) * charge;
                        fr.xRot -= 1.0F * charge; fl.xRot -= 1.0F * charge;
                        br.xRot += 0.6F * charge; bl.xRot += 0.6F * charge;
                        if (jaw != null) jaw.xRot += 0.6F * charge;
                    }
                    case SLAM -> { body.xRot -= 0.5F * charge; fr.xRot -= 1.4F * charge; fl.xRot -= 1.4F * charge; }
                    case RAISE, SHOOT, SELF -> { head.xRot -= 0.4F * charge; if (jaw != null) jaw.xRot += 0.5F * charge; }
                    default -> {}
                }
            }
            swayTail(this, age, amt);
            ModelPart t = find("tail");
            if (t != null && amt > 0.3F) t.xRot += Mth.sin(limb * sp) * 0.2F;
            flapWings(this, age, e.airborne(), 0.7F);
            if (e.airborne()) { fr.xRot = 0.8F; fl.xRot = 0.8F; br.xRot = 1.0F; bl.xRot = 1.0F; }
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== ARACHNID
    public static class Arachnid<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart head, body, abdomen;
        private final ModelPart[] legs = new ModelPart[8];

        public Arachnid(ModelPart root) {
            super(root);
            head = find("head"); body = find("body"); abdomen = find("abdomen");
            for (int i = 0; i < 8; i++) legs[i] = find("leg" + i);
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition body = add(root, "body", c(AU, AV).addBox(-4, -3, -4, 8, 6, 8), 0, -9, 0);
            PartDefinition head = add(body, "head", c(AU, AV + 16).addBox(-3.5F, -3, -6, 7, 6, 6), 0, 0, -4);
            head.addOrReplaceChild("eyes0", c(DU, DV).addBox(-2.5F, -2, -6.6F, 1.5F, 1.5F, 1).addBox(1, -2, -6.6F, 1.5F, 1.5F, 1)
                    .addBox(-1.5F, -0.2F, -6.6F, 1, 1, 1).addBox(0.5F, -0.2F, -6.6F, 1, 1, 1), PartPose.ZERO);
            add(head, "fang_l", c(CU + 16, CV).addBox(-0.5F, 0, -0.5F, 1, 3, 1), 1.2F, 2, -5.5F, -0.3F, 0, 0);
            add(head, "fang_r", c(CU + 16, CV).addBox(-0.5F, 0, -0.5F, 1, 3, 1), -1.2F, 2, -5.5F, -0.3F, 0, 0);
            RpgMobModel.headParts(head, -3, -6, 3.5F, 6);
            PartDefinition ab = add(body, "abdomen", c(AU, AV + 32).addBox(-6, -5, 0, 12, 10, 12), 0, -1, 3.5F, 0.15F, 0, 0);
            add(ab, "pattern", c(CU, CV + 48).addBox(-3, -5.6F, 2, 6, 1, 7), 0, 0, 0);
            spikes(ab, "spikes", -5, 2, 10, 3, 2.0F);
            add(ab, "shell", c(CU, CV + 32).addBox(-6.5F, -5.5F, 0.5F, 13, 3, 11), 0, 0, 0);
            PartDefinition tail = add(ab, "tail", c(AU + 40, AV + 32).addBox(-1.5F, -1.5F, 0, 3, 3, 6), 0, -2, 11, -0.9F, 0, 0);
            PartDefinition tm = add(tail, "tail_mid", c(AU + 40, AV + 32).addBox(-1.2F, -1.2F, 0, 2.4F, 2.4F, 6), 0, 0, 5.5F, -0.9F, 0, 0);
            add(tm, "tail_tip", c(CU, CV + 24).addBox(-1, -1, 0, 2, 2, 4), 0, 0, 5.5F, -0.9F, 0, 0);
            PartDefinition claws = group(head, "claws", 0, 1, -5);
            add(claws, "claw_r", c(CU + 24, CV + 8).addBox(-1.5F, -1.5F, -6, 3, 3, 6), -3, 0, 0, 0, 0.4F, 0);
            add(claws, "claw_l", c(CU + 24, CV + 8).mirror().addBox(-1.5F, -1.5F, -6, 3, 3, 6), 3, 0, 0, 0, -0.4F, 0);
            float[] zs = {-3, -1, 1, 3};
            float[] ys = {0.75F, 0.25F, -0.25F, -0.75F};
            for (int i = 0; i < 4; i++) {
                PartDefinition r = add(root, "leg" + i, c(BU, BV).addBox(-14, -1, -1, 14, 2, 2), -3.5F, -9, zs[i], 0, ys[i], 0.65F);
                add(r, "knee" + i, c(BU, BV + 4).addBox(-10, -0.8F, -0.8F, 10, 1.6F, 1.6F), -13, 0, 0, 0, 0, -1.1F);
                PartDefinition l = add(root, "leg" + (i + 4), c(BU, BV).mirror().addBox(0, -1, -1, 14, 2, 2), 3.5F, -9, zs[i], 0, -ys[i], -0.65F);
                add(l, "knee" + (i + 4), c(BU, BV + 4).mirror().addBox(0, -0.8F, -0.8F, 10, 1.6F, 1.6F), 13, 0, 0, 0, 0, 1.1F);
            }
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            look(head, yaw * 0.5F, pitch * 0.5F);
            float sp = 0.9F;
            for (int i = 0; i < 4; i++) {
                float ph = i % 2 == 0 ? 0 : Mth.PI;
                float sw = swing(limb, sp, ph) * 0.45F * amt;
                float lift = Mth.abs(Mth.sin(limb * sp + ph)) * 0.35F * amt;
                legs[i].yRot += sw;
                legs[i].zRot += lift;
                legs[i + 4].yRot -= -swing(limb, sp, ph + Mth.PI) * 0.45F * amt;
                legs[i + 4].zRot -= Mth.abs(Mth.sin(limb * sp + ph + Mth.PI)) * 0.35F * amt;
            }
            abdomen.xRot += Mth.sin(age * 0.08F) * 0.04F;
            abdomen.yRot += Mth.sin(limb * sp) * 0.06F * amt;
            if (this.attackTime > 0) {
                float s = Mth.sin(this.attackTime * Mth.PI);
                head.xRot += 0.3F * s;
                legs[0].zRot -= 0.8F * s; legs[4].zRot += 0.8F * s;
            }
            if (style != null) {
                body.xRot -= 0.35F * charge;
                legs[0].zRot -= 0.9F * charge; legs[4].zRot += 0.9F * charge;
                legs[1].zRot -= 0.5F * charge; legs[5].zRot += 0.5F * charge;
                abdomen.xRot += (style == Style.SHOOT || style == Style.RAISE ? 0.5F : 0.2F) * charge;
            }
            ModelPart tail = find("tail");
            if (tail != null) tail.xRot += Mth.sin(age * 0.1F) * 0.1F + (this.attackTime > 0 ? -0.6F * Mth.sin(this.attackTime * Mth.PI) : 0);
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== INSECT
    public static class Insect<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart head, body, abdomen;
        private final ModelPart[] legs = new ModelPart[6];

        public Insect(ModelPart root) {
            super(root);
            head = find("head"); body = find("body"); abdomen = find("abdomen");
            for (int i = 0; i < 6; i++) legs[i] = find("leg" + i);
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition body = add(root, "body", c(AU, AV).addBox(-3.5F, -3, -4, 7, 6, 8), 0, -8, 0);
            PartDefinition head = add(body, "head", c(AU, AV + 16).addBox(-3, -3, -5, 6, 5, 5), 0, -0.5F, -4);
            RpgMobModel.eyes(head, -2.5F, -5, 1.5F, 2, 2);
            add(head, "mandible_l", c(CU + 16, CV).addBox(-0.5F, -0.5F, -3, 1, 1, 3), 1.5F, 1.5F, -4.5F, 0, -0.4F, 0);
            add(head, "mandible_r", c(CU + 16, CV).addBox(-0.5F, -0.5F, -3, 1, 1, 3), -1.5F, 1.5F, -4.5F, 0, 0.4F, 0);
            add(head, "antenna_l", c(CU + 24, CV).addBox(-0.25F, -6, -0.25F, 0.5F, 6, 0.5F), 1.2F, -3, -4, -0.6F, 0, 0.3F);
            add(head, "antenna_r", c(CU + 24, CV).addBox(-0.25F, -6, -0.25F, 0.5F, 6, 0.5F), -1.2F, -3, -4, -0.6F, 0, -0.3F);
            RpgMobModel.headParts(head, -3, -5, 3, 5);
            PartDefinition ab = add(body, "abdomen", c(AU, AV + 32).addBox(-4.5F, -3.5F, 0, 9, 7, 13), 0, 0, 3.5F, -0.1F, 0, 0);
            add(ab, "stripes", c(EU, EV).addBox(-4.8F, -3.8F, 3, 9.6F, 7.6F, 1).addBox(-4.8F, -3.8F, 7, 9.6F, 7.6F, 1), 0, 0, 0);
            add(ab, "shell", c(CU, CV + 32).addBox(-5, -4.5F, -0.5F, 10, 3, 13), 0, 0, 0);
            spikes(ab, "spikes", -3.5F, 2, 10, 3, 1.6F);
            add(ab, "tail", c(CU, CV + 24).addBox(-0.5F, -0.5F, 0, 1, 1, 4), 0, 0, 12.5F);
            PartDefinition claws = group(body, "claws", 0, 0, -3);
            add(claws, "raptor_r", c(CU + 24, CV + 8).addBox(-0.75F, 0, -0.75F, 1.5F, 7, 1.5F), -3, 0, 0, -1.2F, 0, 0.1F);
            add(claws, "raptor_l", c(CU + 24, CV + 8).addBox(-0.75F, 0, -0.75F, 1.5F, 7, 1.5F), 3, 0, 0, -1.2F, 0, -0.1F);
            PartDefinition wings = group(body, "wings", 0, -3, -1);
            add(wings, "wing_l", c(FU, FV).addBox(0, -0.25F, 0, 6, 0.5F, 14), 1, 0, 0, 0, -0.2F, 0);
            add(wings, "wing_r", c(FU, FV).mirror().addBox(-6, -0.25F, 0, 6, 0.5F, 14), -1, 0, 0, 0, 0.2F, 0);
            for (int i = 0; i < 3; i++) {
                float z = -2 + i * 2.5F;
                PartDefinition r = add(root, "leg" + i, c(BU, BV).addBox(-9, -0.75F, -0.75F, 9, 1.5F, 1.5F), -3, -8, z, 0, (1 - i) * 0.5F, 0.6F);
                add(r, "shin" + i, c(BU, BV + 4).addBox(-7, -0.6F, -0.6F, 7, 1.2F, 1.2F), -9, 0, 0, 0, 0, -1.0F);
                PartDefinition l = add(root, "leg" + (i + 3), c(BU, BV).mirror().addBox(0, -0.75F, -0.75F, 9, 1.5F, 1.5F), 3, -8, z, 0, -(1 - i) * 0.5F, -0.6F);
                add(l, "shin" + (i + 3), c(BU, BV + 4).mirror().addBox(0, -0.6F, -0.6F, 7, 1.2F, 1.2F), 9, 0, 0, 0, 0, 1.0F);
            }
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            look(head, yaw * 0.5F, pitch * 0.5F);
            float sp = 1.1F;
            for (int i = 0; i < 3; i++) {
                float ph = i % 2 == 0 ? 0 : Mth.PI;
                legs[i].yRot += swing(limb, sp, ph) * 0.5F * amt;
                legs[i].zRot += Mth.abs(Mth.sin(limb * sp + ph)) * 0.3F * amt;
                legs[i + 3].yRot += swing(limb, sp, ph + Mth.PI) * 0.5F * amt;
                legs[i + 3].zRot -= Mth.abs(Mth.sin(limb * sp + ph + Mth.PI)) * 0.3F * amt;
            }
            ModelPart ml = find("mandible_l"), mr = find("mandible_r");
            float chew = Mth.sin(age * 0.4F) * 0.15F + (this.attackTime > 0 ? Mth.sin(this.attackTime * Mth.PI) * 0.5F : 0);
            if (ml != null) ml.yRot -= chew;
            if (mr != null) mr.yRot += chew;
            ModelPart al = find("antenna_l"), ar = find("antenna_r");
            if (al != null) al.xRot += Mth.sin(age * 0.2F) * 0.15F;
            if (ar != null) ar.xRot += Mth.sin(age * 0.2F + 1) * 0.15F;
            ModelPart wl = find("wing_l"), wr = find("wing_r");
            if (wl != null && wr != null) {
                if (e.airborne()) { float f = Mth.sin(age * 2.2F) * 0.7F; wl.zRot = -0.3F - f; wr.zRot = 0.3F + f; wl.yRot = -0.6F; wr.yRot = 0.6F; }
                else { wl.zRot = Mth.sin(age * 0.05F) * 0.03F; wr.zRot = -wl.zRot; }
            }
            ModelPart rr = find("raptor_r"), rl = find("raptor_l");
            if (rr != null && this.attackTime > 0) { rr.xRot += Mth.sin(this.attackTime * Mth.PI) * 1.4F; rl.xRot += Mth.sin(this.attackTime * Mth.PI) * 1.4F; }
            abdomen.xRot += Mth.sin(age * 0.1F) * 0.05F;
            if (style != null) { body.xRot -= 0.3F * charge; abdomen.xRot += 0.4F * charge; if (rr != null) { rr.xRot -= 0.8F * charge; rl.xRot -= 0.8F * charge; } }
            if (e.airborne()) root.y += Mth.sin(age * 0.2F) * 1.2F;
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== FLYER
    public static class Flyer<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart head, body, lw, rw, lwt, rwt, legL, legR;

        public Flyer(ModelPart root) {
            super(root);
            head = find("head"); body = find("body"); lw = find("fwing_l"); rw = find("fwing_r"); lwt = find("fwing_l_tip"); rwt = find("fwing_r_tip");
            legL = find("fleg_l"); legR = find("fleg_r");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition body = add(root, "body", c(AU, AV).addBox(-3, -3, -6, 6, 6, 12), 0, -10, 0);
            add(body, "breast", c(BU, BV + 40).addBox(-2.5F, -1, -6.5F, 5, 4.5F, 6), 0, 0, 0);
            PartDefinition head = add(body, "head", c(AU, AV + 20).addBox(-3, -4, -5, 6, 5, 5), 0, -1, -6);
            add(head, "beak", c(CU, CV + 48).addBox(-1, -1.5F, -8, 2, 2, 3), 0, 0, 0);
            RpgMobModel.eyes(head, -3, -5, 1.2F, 1.5F, 1.5F);
            RpgMobModel.headParts(head, -4, -5, 3, 5);
            PartDefinition lw = add(body, "fwing_l", c(FU, FV).addBox(0, -0.5F, -3, 12, 1, 9), 3, -2, -2);
            add(lw, "fwing_l_tip", c(FU + 44, FV).addBox(0, -0.25F, -3, 11, 0.5F, 8), 12, 0, 0);
            PartDefinition rw = add(body, "fwing_r", c(FU, FV).mirror().addBox(-12, -0.5F, -3, 12, 1, 9), -3, -2, -2);
            add(rw, "fwing_r_tip", c(FU + 44, FV).mirror().addBox(-11, -0.25F, -3, 11, 0.5F, 8), -12, 0, 0);
            add(body, "tailfan", c(AU, AV + 32).addBox(-3, -0.5F, 0, 6, 1, 6), 0, -1, 5.5F, -0.15F, 0, 0);
            tail(body, 0, 5.5F, 0.15F, 9, 2);
            PartDefinition ll = add(body, "fleg_l", c(BU, BV).addBox(-0.75F, 0, -0.75F, 1.5F, 5, 1.5F), 1.5F, 2.5F, 1);
            PartDefinition lr = add(body, "fleg_r", c(BU, BV).addBox(-0.75F, 0, -0.75F, 1.5F, 5, 1.5F), -1.5F, 2.5F, 1);
            PartDefinition cl = group(ll, "claws", 0, 5, 0);
            add(cl, "c", c(CU + 24, CV).addBox(-1, 0, -1.5F, 2, 1, 2), 0, 0, 0);
            PartDefinition cr = group(lr, "claws2", 0, 5, 0);
            add(cr, "c", c(CU + 24, CV).addBox(-1, 0, -1.5F, 2, 1, 2), 0, 0, 0);
            spikes(body, "spikes", -3, -4, 3, 3, 1.4F);
            add(body, "mane", c(AU, AV + 40).addBox(-3.5F, -4, -7, 7, 6, 4), 0, 0, 0);
            add(body, "shell", c(CU, CV + 32).addBox(-3.5F, -3.8F, -5, 7, 2, 10), 0, 0, 0);
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            look(head, yaw, pitch);
            boolean air = e.airborne() || !e.onGround();
            if (air) {
                float f = Mth.sin(age * 0.75F) * 0.9F;
                lw.zRot = -f; rw.zRot = f;
                lwt.zRot = -Mth.sin(age * 0.75F - 0.9F) * 0.5F; rwt.zRot = -lwt.zRot;
                legL.xRot = 1.0F; legR.xRot = 1.0F;
                root.y += Mth.sin(age * 0.75F + 1) * 1.2F;
                body.xRot = 0.1F + Mth.clamp(amt, 0, 1) * 0.25F;
            } else {
                lw.zRot = -1.2F; rw.zRot = 1.2F; lw.yRot = -0.5F; rw.yRot = 0.5F;
                lwt.zRot = 2.3F; rwt.zRot = -2.3F;
                legL.xRot = swing(limb, 0.9F, 0) * 0.8F * amt;
                legR.xRot = swing(limb, 0.9F, Mth.PI) * 0.8F * amt;
                body.xRot = -0.2F;
                head.xRot += 0.2F;
            }
            if (this.attackTime > 0) { float s = Mth.sin(this.attackTime * Mth.PI); head.xRot += 0.5F * s; legL.xRot -= 1.2F * s; legR.xRot -= 1.2F * s; }
            if (style != null) {
                switch (style) {
                    case LUNGE -> { body.xRot += 0.8F * charge; lw.zRot = -0.2F; rw.zRot = 0.2F; lw.yRot = -0.8F * charge; rw.yRot = 0.8F * charge; }
                    case ROAR, BREATH -> head.xRot -= 0.5F * charge;
                    default -> { lw.zRot = Mth.lerp(charge, lw.zRot, -1.3F); rw.zRot = Mth.lerp(charge, rw.zRot, 1.3F); head.xRot -= 0.3F * charge; }
                }
            }
            swayTail(this, age, amt);
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== SLIME
    public static class Slime<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart outer, core;

        public Slime(ModelPart root) {
            super(root);
            outer = find("outer"); core = find("core");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition outer = add(root, "outer", c(AU, AV).addBox(-8, -16, -8, 16, 16, 16), 0, 0, 0);
            add(root, "core", c(EU, EV).addBox(-3, -3, -3, 6, 6, 6), 0, -8, 0);
            outer.addOrReplaceChild("eyes0", c(DU, DV).addBox(-5, -12, -8.6F, 3, 3, 1).addBox(2, -12, -8.6F, 3, 3, 1), PartPose.ZERO);
            add(outer, "mouth", c(EU, EV + 16).addBox(-3, -6, -8.5F, 6, 2, 1), 0, 0, 0);
            PartDefinition head = group(outer, "top", 0, 0, 0);
            RpgMobModel.headParts(head, -16, -8, 8, 16);
            spikes(outer, "spikes", -16, -5, 5, 3, 3);
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            float hop = Mth.abs(Mth.sin(limb * 0.35F)) * amt;
            float squish = Mth.sin(age * 0.15F) * 0.04F + (hop < 0.2F ? 0.1F * amt : 0);
            root.y -= hop * 5;
            outer.yScale = 1 + hop * 0.15F - squish;
            outer.xScale = 1 - hop * 0.08F + squish * 0.8F;
            outer.zScale = outer.xScale;
            core.yRot = age * 0.05F;
            core.xRot = age * 0.03F;
            core.y += Mth.sin(age * 0.1F) * 0.8F;
            if (this.attackTime > 0) { float s = Mth.sin(this.attackTime * Mth.PI); outer.yScale -= 0.25F * s; outer.xScale += 0.2F * s; outer.zScale += 0.2F * s; }
            if (style != null) {
                float pulse = release > 0 ? 1 + release * 0.35F : 1 - charge * 0.15F;
                outer.xScale *= pulse; outer.yScale *= (2 - pulse); outer.zScale *= pulse;
            }
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== SERPENT
    public static class Serpent<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private static final int SEGS = 7;
        private final ModelPart head, jaw;
        private final ModelPart[] segs = new ModelPart[SEGS];

        public Serpent(ModelPart root) {
            super(root);
            head = find("head"); jaw = find("jaw");
            for (int i = 0; i < SEGS; i++) segs[i] = find("seg" + i);
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition head = add(root, "head", c(AU, AV).addBox(-4, -4, -8, 8, 5, 9), 0, -9, -6);
            PartDefinition jaw = add(head, "jaw", c(EU, EV).addBox(-3.5F, 0, -7.5F, 7, 2, 8), 0, 1, 0);
            add(jaw, "fangs", c(CU + 16, CV).addBox(-2.5F, -1.5F, -7, 1, 2, 1).addBox(1.5F, -1.5F, -7, 1, 2, 1), 0, 0, 0);
            RpgMobModel.eyes(head, -3.2F, -6, 2.2F, 1.5F, 1.2F);
            RpgMobModel.headParts(head, -4, -8, 4, 9);
            PartDefinition fins = group(head, "ears2", 0, -2, -1);
            add(fins, "fin_l", c(FU, FV).addBox(0, -3, 0, 0.5F, 5, 6), 4, 0, 0, 0, 0.4F, 0);
            add(fins, "fin_r", c(FU, FV).addBox(-0.5F, -3, 0, 0.5F, 5, 6), -4, 0, 0, 0, -0.4F, 0);
            for (int i = 0; i < SEGS; i++) {
                float s = 1.0F - i * 0.09F;
                float w = 7 * s, h = 6 * s;
                PartDefinition seg = add(root, "seg" + i, c(AU, AV + 16).addBox(-w / 2, -h / 2, 0, w, h, 8), 0, -3.5F * s - (i == 0 ? 2.5F : 0), i * 7.5F - 5);
                add(seg, "belly" + i, c(BU, BV).addBox(-w / 2 + 0.5F, h / 2 - 1.2F, 0.5F, w - 1, 1.5F, 7), 0, 0, 0);
                if (i >= 1 && i <= 4) {
                    PartDefinition sp = group(seg, i == 1 ? "spikes" : "spikes" + i, 0, -h / 2, 4);
                    add(sp, "s", c(CU, CV + 16).addBox(-0.8F, -2.5F * s, -0.8F, 1.6F, 2.5F * s, 1.6F), 0, 0, 0, -0.4F, 0, 0);
                }
            }
            PartDefinition tailEnd = group(root, "tail", 0, -1.5F, SEGS * 7.5F - 5);
            add(tailEnd, "rattle", c(CU, CV + 24).addBox(-1.5F, -1.5F, 0, 3, 3, 5), 0, 0, 0);
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            float speed = 0.25F + amt * 0.5F;
            float amp = 1.2F + amt * 2.5F;
            float t = age * speed + limb * 0.5F;
            for (int i = 0; i < SEGS; i++) {
                float off = Mth.sin(t - i * 0.9F) * amp * (0.4F + i * 0.15F);
                segs[i].x += off;
                segs[i].yRot = Mth.cos(t - i * 0.9F) * 0.35F * (amt + 0.3F);
                segs[i].y += Mth.sin(t * 0.5F - i * 0.6F) * 0.3F;
            }
            ModelPart tail = find("tail");
            if (tail != null) tail.x += Mth.sin(t - SEGS * 0.9F) * amp * (0.4F + SEGS * 0.15F);
            head.x += Mth.sin(t + 0.9F) * amp * 0.2F;
            look(head, yaw * 0.6F, pitch * 0.5F);
            head.y += Mth.sin(age * 0.08F) * 0.6F;
            if (jaw != null) jaw.xRot = 0.08F + Math.max(0F, Mth.sin(age * 0.06F)) * 0.1F;
            if (this.attackTime > 0) {
                float s = Mth.sin(this.attackTime * Mth.PI);
                head.z -= 4 * s; head.y += 2 * s;
                if (jaw != null) jaw.xRot += 0.9F * s;
            }
            if (style != null) {
                head.y -= 5 * charge;
                head.xRot -= 0.5F * charge;
                segs[0].y -= 3 * charge;
                segs[0].xRot = -0.5F * charge;
                if (jaw != null) jaw.xRot += (style == Style.ROAR || style == Style.BREATH || style == Style.SHOOT ? 0.9F : 0.4F) * charge;
                if (release > 0) head.z -= 3 * release;
            }
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== GOLEM
    public static class Golem<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart head, body, ra, la, rl, ll;

        public Golem(ModelPart root) {
            super(root);
            head = find("head"); body = find("body"); ra = find("right_arm"); la = find("left_arm"); rl = find("right_leg"); ll = find("left_leg");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition body = add(root, "body", c(AU, AV).addBox(-8, -14, -5, 16, 14, 10), 0, -10, 0);
            add(body, "core_gem", c(DU, DV + 8).addBox(-2, -10, -5.6F, 4, 4, 1), 0, 0, 0);
            add(body, "waist", c(BU, BV + 40).addBox(-5, -1, -3.5F, 10, 3, 7), 0, 0, 0);
            PartDefinition head = add(body, "head", c(AU, AV + 24).addBox(-4, -8, -4.5F, 8, 8, 8), 0, -14, -1.5F);
            add(head, "brow", c(EU, EV).addBox(-4.5F, -6.5F, -5, 9, 1.5F, 1.5F), 0, 0, 0);
            RpgMobModel.eyes(head, -5, -4.5F, 1, 2, 1.5F);
            RpgMobModel.headParts(head, -8, -4.5F, 4, 8);
            PartDefinition ra = add(root, "right_arm", c(BU, BV).addBox(-4, -2, -3, 6, 20, 6), -10, -22, 0);
            PartDefinition la = add(root, "left_arm", c(BU, BV).mirror().addBox(-2, -2, -3, 6, 20, 6), 10, -22, 0);
            add(ra, "shoulder_r", c(CU, CV + 32).addBox(-5, -3.5F, -3.5F, 8, 4, 7), 0, 0, 0);
            add(la, "shoulder_l", c(CU, CV + 32).mirror().addBox(-3, -3.5F, -3.5F, 8, 4, 7), 0, 0, 0);
            PartDefinition cr = group(ra, "claws", -1, 18, -1);
            PartDefinition cl = group(la, "claws2", 1, 18, -1);
            for (int i = 0; i < 3; i++) {
                add(cr, "c" + i, c(CU + 24, CV).addBox(-0.75F, 0, -0.75F, 1.5F, 4, 1.5F), -2 + i * 2, 0, -1, -0.3F, 0, 0);
                add(cl, "c" + i, c(CU + 24, CV).addBox(-0.75F, 0, -0.75F, 1.5F, 4, 1.5F), -2 + i * 2, 0, -1, -0.3F, 0, 0);
            }
            add(root, "right_leg", c(BU + 24, BV + 24).addBox(-3, 0, -3, 6, 10, 6), -4.5F, -10, 0);
            add(root, "left_leg", c(BU + 24, BV + 24).mirror().addBox(-3, 0, -3, 6, 10, 6), 4.5F, -10, 0);
            PartDefinition sp = group(body, "spikes", 0, -14, 0);
            add(sp, "cr0", c(CU, CV + 16).addBox(-1.5F, -6, -1.5F, 3, 6, 3), -6, 0, 1, -0.2F, 0, -0.4F);
            add(sp, "cr1", c(CU, CV + 16).addBox(-1.5F, -6, -1.5F, 3, 6, 3), 6, 0, 1, -0.2F, 0, 0.4F);
            add(sp, "cr2", c(CU, CV + 16).addBox(-1, -4, -1, 2, 4, 2), -2, 0, 4, -0.5F, 0, -0.2F);
            add(sp, "cr3", c(CU, CV + 16).addBox(-1, -5, -1, 2, 5, 2), 3, 0, 3, -0.6F, 0, 0.3F);
            add(body, "shell", c(CU, CV + 32).addBox(-8.5F, -14.5F, 4.5F, 17, 14, 3), 0, 0, 0);
            tail(body, -2, 5, 0.9F, 8, 3);
            wingPair(body, 4, -12, 5, 18, 12);
            add(body, "mane", c(AU, AV + 46).addBox(-8.5F, -15, -5.5F, 17, 4, 11), 0, 0, 0);
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            look(head, yaw * 0.6F, pitch * 0.6F);
            float sp = 0.45F;
            rl.xRot = swing(limb, sp, 0) * 0.9F * amt;
            ll.xRot = swing(limb, sp, Mth.PI) * 0.9F * amt;
            ra.xRot = swing(limb, sp, Mth.PI) * 0.6F * amt;
            la.xRot = swing(limb, sp, 0) * 0.6F * amt;
            body.yRot = Mth.cos(limb * sp) * 0.08F * amt;
            body.y += Mth.sin(age * 0.05F) * 0.3F;
            if (this.attackTime > 0) {
                float s = Mth.sin(this.attackTime * Mth.PI);
                ra.xRot = -2.0F * s; la.xRot = -2.0F * s;
            }
            armsCast(ra, la, head, body, style, charge, release, ca);
            ModelPart gem = find("core_gem");
            if (gem != null) gem.zScale = gem.xScale = gem.yScale = 1 + Mth.sin(age * 0.2F) * 0.08F + charge * 0.3F;
            swayTail(this, age, amt);
            flapWings(this, age, e.airborne(), 0.5F);
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== FLOATER
    public static class Floater<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart core, eye;
        private final ModelPart[] tent = new ModelPart[6];
        private final ModelPart[] tips = new ModelPart[6];

        public Floater(ModelPart root) {
            super(root);
            core = find("core"); eye = find("eye");
            for (int i = 0; i < 6; i++) { tent[i] = find("tent" + i); tips[i] = find("tip" + i); }
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition core = add(root, "core", c(AU, AV).addBox(-5, -5, -5, 10, 10, 10), 0, -16, 0);
            PartDefinition eye = add(core, "eye", c(DU, DV).addBox(-2.5F, -2.5F, -0.5F, 5, 5, 1), 0, -0.5F, -5);
            add(eye, "pupil", c(EU, EV).addBox(-1, -1, -0.8F, 2, 2, 1), 0, 0, 0);
            core.addOrReplaceChild("eyes0", c(DU, DV + 6).addBox(-4, 2.5F, -5.4F, 1, 1, 1).addBox(3, 2.5F, -5.4F, 1, 1, 1), PartPose.ZERO);
            add(core, "hood2", c(BU, BV + 40).addBox(-5.6F, -5.8F, -4.2F, 11.2F, 11.2F, 10), 0, 0, 0);
            add(core, "hood3", c(BU, BV).addBox(-5, 4, -4, 10, 8, 9), 0, 0, 0);
            PartDefinition crown = group(core, "crown2", 0, -5, 0);
            for (int i = 0; i < 5; i++) {
                float a = i * Mth.TWO_PI / 5;
                add(crown, "p" + i, c(CU, CV + 16).addBox(-0.75F, -4, -0.75F, 1.5F, 4, 1.5F), Mth.cos(a) * 3.5F, 0, Mth.sin(a) * 3.5F, Mth.sin(a) * 0.4F, 0, -Mth.cos(a) * 0.4F);
            }
            PartDefinition sp = group(core, "spikes", 0, 0, 0);
            for (int i = 0; i < 6; i++) {
                float a = i * Mth.TWO_PI / 6;
                add(sp, "s" + i, c(CU, CV + 16).addBox(-0.75F, -0.75F, 0, 1.5F, 1.5F, 4), Mth.cos(a) * 5, Mth.sin(a) * 2, Mth.sin(a) * 5, 0, -a + Mth.HALF_PI, 0);
            }
            PartDefinition stalks = group(core, "eyes2", 0, -5, 0);
            for (int i = 0; i < 4; i++) {
                float a = i * Mth.TWO_PI / 4 + 0.4F;
                PartDefinition st = add(stalks, "stalk" + i, c(BU, BV + 32).addBox(-0.5F, -5, -0.5F, 1, 5, 1), Mth.cos(a) * 3, 0, Mth.sin(a) * 3, Mth.sin(a) * 0.5F, 0, -Mth.cos(a) * 0.5F);
                add(st, "seye" + i, c(DU, DV).addBox(-1, -2, -1, 2, 2, 2), 0, -5, 0);
            }
            RpgMobModel.headParts(group(core, "headparts", 0, 0, 0), -5, -5, 5, 10);
            for (int i = 0; i < 6; i++) {
                float a = i * Mth.TWO_PI / 6;
                PartDefinition t = add(core, "tent" + i, c(BU, BV + 32).addBox(-0.75F, 0, -0.75F, 1.5F, 8, 1.5F), Mth.cos(a) * 3.5F, 4.5F, Mth.sin(a) * 3.5F);
                add(t, "tip" + i, c(BU + 8, BV + 32).addBox(-0.5F, 0, -0.5F, 1, 6, 1), 0, 7.5F, 0);
            }
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            root.y += Mth.sin(age * 0.1F) * 1.6F - 2;
            core.yRot = yaw * 0.7F + Mth.sin(age * 0.03F) * 0.2F;
            core.xRot = pitch * 0.6F + amt * 0.3F;
            if (eye != null) { eye.x += Mth.sin(age * 0.05F) * 0.4F; eye.y += Mth.cos(age * 0.07F) * 0.3F; }
            for (int i = 0; i < 6; i++) {
                float ph = i * 1.05F;
                tent[i].xRot = Mth.sin(age * 0.15F + ph) * 0.35F + amt * 0.6F;
                tent[i].zRot = Mth.cos(age * 0.13F + ph) * 0.3F;
                tips[i].xRot = Mth.sin(age * 0.15F + ph - 0.8F) * 0.5F;
            }
            for (int i = 0; i < 4; i++) {
                ModelPart st = find("stalk" + i);
                if (st != null) st.xRot += Mth.sin(age * 0.11F + i) * 0.25F;
            }
            if (this.attackTime > 0) { float s = Mth.sin(this.attackTime * Mth.PI); core.xRot += 0.5F * s; for (ModelPart t : tent) t.xRot -= 1.0F * s; }
            if (style != null) {
                float spread = charge * 0.9F;
                for (int i = 0; i < 6; i++) { tent[i].xRot -= spread * Mth.sin(i * 1.05F); tent[i].zRot += spread * Mth.cos(i * 1.05F); }
                if (eye != null) eye.xScale = eye.yScale = 1 + charge * 0.4F;
                core.y -= charge * 2;
            }
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== TREANT
    public static class Treant<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart trunk, canopy, ra, la, rl, ll;

        public Treant(ModelPart root) {
            super(root);
            trunk = find("trunk"); canopy = find("canopy"); ra = find("right_arm"); la = find("left_arm"); rl = find("right_leg"); ll = find("left_leg");
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition trunk = add(root, "trunk", c(AU, AV).addBox(-5, -20, -4, 10, 20, 8), 0, -6, 0);
            trunk.addOrReplaceChild("eyes0", c(DU, DV).addBox(-3.5F, -15, -4.6F, 2, 1.5F, 1).addBox(1.5F, -15, -4.6F, 2, 1.5F, 1), PartPose.ZERO);
            add(trunk, "mouth", c(EU, EV).addBox(-2.5F, -11, -4.5F, 5, 2.5F, 1), 0, 0, 0);
            add(trunk, "knot", c(EU, EV + 8).addBox(-1, -6, -4.4F, 2, 2, 1), 2, 0, 0);
            PartDefinition canopy = add(trunk, "canopy", c(BU, BV).addBox(-8, -10, -7, 16, 10, 14), 0, -19, 0);
            add(canopy, "canopy_top", c(BU, BV + 24).addBox(-5.5F, -5, -5, 11, 5, 10), 0, -9.5F, 0);
            PartDefinition crown = group(canopy, "crown2", 0, -10, 0);
            for (int i = 0; i < 5; i++) {
                float a = i * Mth.TWO_PI / 5;
                add(crown, "bloom" + i, c(DU + 8, DV).addBox(-1, -1, -1, 2, 2, 2), Mth.cos(a) * 6, 3 + (i % 2) * 2, Mth.sin(a) * 5.5F);
            }
            PartDefinition head = group(trunk, "headparts", 0, 0, 0);
            RpgMobModel.headParts(head, -20, -4, 5, 8);
            PartDefinition ra = add(trunk, "right_arm", c(AU + 36, AV).addBox(-2, -1, -2, 3, 16, 3), -5, -16, 0, 0, 0, 0.35F);
            add(ra, "twig_r", c(AU + 36, AV + 20).addBox(-1, 0, -1, 1.5F, 6, 1.5F), -1, 13, 0, 0, 0, -0.6F);
            add(ra, "leaf_r", c(BU, BV + 40).addBox(-2.5F, -2, -2.5F, 5, 4, 5), -1, 4, 0);
            PartDefinition la = add(trunk, "left_arm", c(AU + 36, AV).mirror().addBox(-1, -1, -2, 3, 16, 3), 5, -16, 0, 0, 0, -0.35F);
            add(la, "twig_l", c(AU + 36, AV + 20).addBox(-0.5F, 0, -1, 1.5F, 6, 1.5F), 1, 13, 0, 0, 0, 0.6F);
            add(la, "leaf_l", c(BU, BV + 40).addBox(-2.5F, -2, -2.5F, 5, 4, 5), 1, 4, 0);
            PartDefinition cr = group(ra, "claws", 0, 15, 0);
            add(cr, "thorn", c(CU + 24, CV).addBox(-0.5F, 0, -0.5F, 1, 4, 1), 0, 0, 0, 0.3F, 0, 0);
            PartDefinition cl = group(la, "claws2", 0, 15, 0);
            add(cl, "thorn", c(CU + 24, CV).addBox(-0.5F, 0, -0.5F, 1, 4, 1), 0, 0, 0, 0.3F, 0, 0);
            add(root, "right_leg", c(AU + 48, AV).addBox(-2.5F, 0, -2.5F, 5, 6, 5), -2.8F, -6, 0);
            add(root, "left_leg", c(AU + 48, AV).mirror().addBox(-2.5F, 0, -2.5F, 5, 6, 5), 2.8F, -6, 0);
            PartDefinition roots = group(root, "roots0", 0, 0, 0);
            for (int i = 0; i < 4; i++) {
                float a = i * Mth.HALF_PI + 0.78F;
                add(roots, "r" + i, c(AU + 36, AV + 20).addBox(-0.75F, -0.75F, 0, 1.5F, 1.5F, 6), Mth.cos(a) * 3, -0.75F, Mth.sin(a) * 3, 0.1F, -a + Mth.HALF_PI, 0);
            }
            PartDefinition sp = group(trunk, "spikes", 0, 0, 0);
            for (int i = 0; i < 6; i++) {
                add(sp, "t" + i, c(CU, CV + 16).addBox(-0.5F, -0.5F, 0, 1, 1, 3), (i % 2 == 0 ? -5 : 5), -3 - i * 3, (i % 3 - 1) * 2, 0, (i % 2 == 0 ? -1.6F : 1.6F), 0);
            }
            add(trunk, "mane", c(BU, BV + 40).addBox(-6, -21, -5, 12, 5, 10), 0, 0, 0);
            add(trunk, "shell", c(CU, CV + 32).addBox(-5.5F, -19, 3.5F, 11, 18, 2), 0, 0, 0);
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            float sp = 0.4F;
            rl.xRot = swing(limb, sp, 0) * 0.8F * amt;
            ll.xRot = swing(limb, sp, Mth.PI) * 0.8F * amt;
            trunk.zRot = Mth.cos(limb * sp) * 0.06F * amt + Mth.sin(age * 0.03F) * 0.02F;
            trunk.yRot = yaw * 0.3F;
            canopy.yRot = Mth.sin(age * 0.04F) * 0.06F;
            canopy.zRot = Mth.sin(age * 0.05F + 1) * 0.04F;
            ra.xRot = swing(limb, sp, Mth.PI) * 0.5F * amt + Mth.sin(age * 0.05F) * 0.05F;
            la.xRot = swing(limb, sp, 0) * 0.5F * amt - Mth.sin(age * 0.05F) * 0.05F;
            if (this.attackTime > 0) { float s = Mth.sin(this.attackTime * Mth.PI); ra.xRot -= 2.2F * s; la.xRot -= 1.4F * s; trunk.xRot += 0.15F * s; }
            armsCast(ra, la, null, trunk, style, charge, release, ca);
            if (style != null) canopy.yScale = 1 + charge * 0.15F;
            bodyCast(style, charge, ca);
        }
    }

    // =================================================================================== CRUSTACEAN
    public static class Crustacean<T extends LivingEntity & RpgAnimatable> extends RpgMobModel<T> {
        private final ModelPart body, cr, cl, pr, pl;
        private final ModelPart[] legs = new ModelPart[6];

        public Crustacean(ModelPart root) {
            super(root);
            body = find("body"); cr = find("arm_r"); cl = find("arm_l"); pr = find("pincer_r"); pl = find("pincer_l");
            for (int i = 0; i < 6; i++) legs[i] = find("leg" + i);
        }

        public static LayerDefinition layer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
            PartDefinition body = add(root, "body", c(AU, AV).addBox(-7, -3, -6, 14, 6, 12), 0, -7, 0);
            add(body, "underside", c(BU, BV + 40).addBox(-6, 2.5F, -5, 12, 1, 10), 0, 0, 0);
            PartDefinition eye1 = add(body, "stalk_l", c(BU, BV + 32).addBox(-0.5F, -4, -0.5F, 1, 4, 1), 2, -2.5F, -5.5F, -0.2F, 0, 0.15F);
            add(eye1, "eyeball_l", c(DU, DV).addBox(-1, -2, -1, 2, 2, 2), 0, -4, 0);
            PartDefinition eye2 = add(body, "stalk_r", c(BU, BV + 32).addBox(-0.5F, -4, -0.5F, 1, 4, 1), -2, -2.5F, -5.5F, -0.2F, 0, -0.15F);
            add(eye2, "eyeball_r", c(DU, DV).addBox(-1, -2, -1, 2, 2, 2), 0, -4, 0);
            add(body, "mouth", c(EU, EV).addBox(-2, -0.5F, -6.4F, 4, 2, 1), 0, 0, 0);
            PartDefinition claws = group(body, "claws0", 0, 0, 0);
            PartDefinition ar = add(claws, "arm_r", c(BU, BV).addBox(-1.5F, -1.5F, -6, 3, 3, 6), -6, 0, -5, 0, 0.5F, 0);
            PartDefinition pr = add(ar, "pincer_r", c(CU, CV).addBox(-3, -2.5F, -6, 6, 5, 6), 0, 0, -6);
            add(pr, "finger_r", c(CU + 24, CV).addBox(-1, -1, -4, 2, 2, 4), -1.5F, 1.5F, -5);
            PartDefinition al = add(claws, "arm_l", c(BU, BV).mirror().addBox(-1.5F, -1.5F, -6, 3, 3, 6), 6, 0, -5, 0, -0.5F, 0);
            PartDefinition pl = add(al, "pincer_l", c(CU, CV).mirror().addBox(-3, -2.5F, -6, 6, 5, 6), 0, 0, -6);
            add(pl, "finger_l", c(CU + 24, CV).addBox(-1, -1, -4, 2, 2, 4), 1.5F, 1.5F, -5);
            PartDefinition big = group(body, "claws", 0, 0, 0);
            add(big, "bigclaw", c(CU, CV + 32).addBox(-4, -3.5F, -1, 8, 7, 3), -9, 0, -16, 0, 0.5F, 0);
            PartDefinition tail = add(body, "tail", c(AU + 40, AV + 32).addBox(-1.5F, -1.5F, 0, 3, 3, 7), 0, -2, 5.5F, -1.0F, 0, 0);
            PartDefinition tm = add(tail, "tail_mid", c(AU + 40, AV + 32).addBox(-1.2F, -1.2F, 0, 2.4F, 2.4F, 7), 0, 0, 6.5F, -0.9F, 0, 0);
            PartDefinition tt = add(tm, "tail_tip", c(CU, CV + 24).addBox(-1.5F, -1.5F, 0, 3, 3, 3), 0, 0, 6.5F, -0.9F, 0, 0);
            add(tt, "sting", c(CU + 16, CV).addBox(-0.5F, -0.5F, 0, 1, 1, 3), 0, 0, 2.5F, 0.8F, 0, 0);
            add(body, "shell", c(CU, CV + 32).addBox(-7.5F, -4.5F, -5.5F, 15, 2, 11), 0, 0, 0);
            spikes(body, "spikes", -3, -4, 4, 4, 1.6F);
            RpgMobModel.headParts(group(body, "headparts", 0, 0, 0), -3, -6, 5, 4);
            for (int i = 0; i < 3; i++) {
                float z = -2 + i * 3;
                add(root, "leg" + i, c(BU, BV + 8).addBox(-10, -1, -1, 10, 2, 2), -6.5F, -7, z, 0, (1 - i) * 0.3F, 0.55F);
                add(root, "leg" + (i + 3), c(BU, BV + 8).mirror().addBox(0, -1, -1, 10, 2, 2), 6.5F, -7, z, 0, -(1 - i) * 0.3F, -0.55F);
            }
            return done(mesh);
        }

        @Override
        protected void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float ca) {
            float sp = 1.0F;
            for (int i = 0; i < 3; i++) {
                float ph = i % 2 == 0 ? 0 : Mth.PI;
                legs[i].yRot += swing(limb, sp, ph) * 0.4F * amt;
                legs[i].zRot += Mth.abs(Mth.sin(limb * sp + ph)) * 0.3F * amt;
                legs[i + 3].yRot += swing(limb, sp, ph + Mth.PI) * 0.4F * amt;
                legs[i + 3].zRot -= Mth.abs(Mth.sin(limb * sp + ph + Mth.PI)) * 0.3F * amt;
            }
            body.yRot = yaw * 0.4F;
            body.y += Mth.sin(age * 0.1F) * 0.3F;
            float snap = Mth.sin(age * 0.12F) * 0.08F;
            cr.yRot += snap; cl.yRot -= snap;
            if (this.attackTime > 0) {
                float s = Mth.sin(this.attackTime * Mth.PI);
                cr.xRot -= 0.9F * s; cl.xRot -= 0.5F * s;
                pr.yRot -= 0.4F * s;
            }
            if (style != null) { cr.xRot -= 1.0F * charge; cl.xRot -= 1.0F * charge; cr.yRot += 0.3F * charge; cl.yRot -= 0.3F * charge; body.xRot -= 0.2F * charge; }
            ModelPart tail = find("tail");
            if (tail != null) tail.xRot += Mth.sin(age * 0.1F) * 0.12F - (this.attackTime > 0 ? Mth.sin(this.attackTime * Mth.PI) * 0.5F : 0);
            bodyCast(style, charge, ca);
        }
    }
}
