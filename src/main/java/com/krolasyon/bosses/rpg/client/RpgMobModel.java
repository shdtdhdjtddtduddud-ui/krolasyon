package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.mob.AbilityLogic.Style;
import com.krolasyon.bosses.rpg.mob.RpgAnimatable;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base for the twelve archetype models. Texture layout (128x128) is shared by every monster texture:
 * A body (0,0) - B secondary (64,0) - C accent (0,64) - D eyes (64,64) - E dark (96,64) - F membrane (0,96).
 * Optional body parts live in groups named after {@link RpgDefs#PART_NAMES} (plus "2".."4" suffixes).
 */
public abstract class RpgMobModel<T extends LivingEntity & RpgAnimatable> extends HierarchicalModel<T> {
    public static final int AU = 0, AV = 0, BU = 64, BV = 0, CU = 0, CV = 64, DU = 64, DV = 64, EU = 96, EV = 64, FU = 0, FV = 96;

    protected final ModelPart root;
    private final List<List<ModelPart>> groups = new ArrayList<>();

    protected RpgMobModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        for (String n : RpgDefs.PART_NAMES) {
            List<ModelPart> l = new ArrayList<>();
            opt(n, l);
            for (int i = 2; i <= 4; i++) opt(n + i, l);
            groups.add(l);
        }
    }

    private void opt(String name, List<ModelPart> out) {
        ModelPart p = find(name);
        if (p != null) out.add(p);
    }

    @Nullable
    protected ModelPart find(String name) {
        return getAnyDescendantWithName(name).orElse(null);
    }

    @Override
    public ModelPart root() { return root; }

    // ------------------------------------------------------------------ building helpers
    protected static CubeListBuilder c(int u, int v) { return CubeListBuilder.create().texOffs(u, v); }

    protected static PartDefinition add(PartDefinition parent, String name, CubeListBuilder cubes, float x, float y, float z) {
        return parent.addOrReplaceChild(name, cubes, PartPose.offset(x, y, z));
    }

    protected static PartDefinition add(PartDefinition parent, String name, CubeListBuilder cubes, float x, float y, float z, float rx, float ry, float rz) {
        return parent.addOrReplaceChild(name, cubes, PartPose.offsetAndRotation(x, y, z, rx, ry, rz));
    }

    protected static PartDefinition group(PartDefinition parent, String name, float x, float y, float z) {
        return parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, y, z));
    }

    protected static CubeDeformation inf(float f) { return new CubeDeformation(f); }

    /** pair of glowing eyes on a face plane at z */
    protected static void eyes(PartDefinition head, float y, float z, float spread, float w, float h) {
        head.addOrReplaceChild("eyes0", c(DU, DV).addBox(-spread - w, y, z - 0.6F, w, h, 1).addBox(spread, y, z - 0.6F, w, h, 1), PartPose.ZERO);
    }

    /** horns, ears, crown etc on a head whose top is at {@code top} and front at {@code front}, half width hw */
    protected static void headParts(PartDefinition head, float top, float front, float hw, float depth) {
        PartDefinition horns = group(head, "horns", 0, top, front + depth * 0.4F);
        add(horns, "horn_l", c(CU, CV).addBox(-0.9F, -6, -0.9F, 1.8F, 6, 1.8F), hw * 0.6F, 0.5F, 0, -0.35F, 0, 0.35F);
        add(add(horns, "horn_l2", CubeListBuilder.create(), hw * 0.6F, 0.5F, 0, -0.35F, 0, 0.35F), "horn_l_tip", c(CU + 8, CV).addBox(-0.5F, -4, -0.5F, 1, 4, 1), 0, -5.5F, 0, -0.5F, 0, 0);
        add(horns, "horn_r", c(CU, CV).addBox(-0.9F, -6, -0.9F, 1.8F, 6, 1.8F), -hw * 0.6F, 0.5F, 0, -0.35F, 0, -0.35F);
        add(add(horns, "horn_r2", CubeListBuilder.create(), -hw * 0.6F, 0.5F, 0, -0.35F, 0, -0.35F), "horn_r_tip", c(CU + 8, CV).addBox(-0.5F, -4, -0.5F, 1, 4, 1), 0, -5.5F, 0, -0.5F, 0, 0);

        PartDefinition ears = group(head, "ears", 0, top + 2.5F, front + depth * 0.55F);
        add(ears, "ear_l", c(AU + 40, AV).addBox(0, -1.5F, -0.5F, 4, 3, 1), hw, 0, 0, 0, 0.2F, -0.5F);
        add(ears, "ear_r", c(AU + 40, AV).mirror().addBox(-4, -1.5F, -0.5F, 4, 3, 1), -hw, 0, 0, 0, -0.2F, 0.5F);

        PartDefinition tusks = group(head, "tusks", 0, top + depth * 0.85F, front);
        add(tusks, "tusk_l", c(CU + 16, CV).addBox(-0.5F, -3, -0.5F, 1, 3, 1), hw * 0.55F, 1.0F, -0.3F, -0.3F, 0, 0.15F);
        add(tusks, "tusk_r", c(CU + 16, CV).addBox(-0.5F, -3, -0.5F, 1, 3, 1), -hw * 0.55F, 1.0F, -0.3F, -0.3F, 0, -0.15F);

        PartDefinition crown = group(head, "crown", 0, top, front + depth * 0.5F);
        add(crown, "band", c(CU, CV + 8).addBox(-hw - 0.5F, -2, -depth * 0.5F - 0.5F, hw * 2 + 1, 2, depth + 1), 0, 0, 0);
        for (int i = 0; i < 4; i++) {
            float px = i % 2 == 0 ? -hw : hw - 1;
            float pz = i < 2 ? -depth * 0.5F - 0.5F : depth * 0.5F - 0.5F;
            add(crown, "point" + i, c(CU + 20, CV).addBox(0, -3, 0, 1.5F, 3, 1.5F), px, -2, pz);
        }
        add(crown, "gem", c(DU + 8, DV).addBox(-1, -3, -0.5F, 2, 2, 1), 0, -1.2F, -depth * 0.5F - 0.9F);

        PartDefinition extra = group(head, "eyes", 0, 0, front);
        extra.addOrReplaceChild("third", c(DU, DV + 4).addBox(-1, top + depth * 0.25F, -0.7F, 2, 2, 1), PartPose.ZERO);
        extra.addOrReplaceChild("small", c(DU, DV + 4).addBox(-hw + 0.5F, top + depth * 0.15F, -0.6F, 1, 1, 1).addBox(hw - 1.5F, top + depth * 0.15F, -0.6F, 1, 1, 1), PartPose.ZERO);

        head.addOrReplaceChild("hood", c(BU, BV + 40).addBox(-hw - 0.6F, top - 0.7F, front + 0.6F, hw * 2 + 1.2F, depth + 0.4F, depth + 0.5F), PartPose.ZERO);
    }

    // ------------------------------------------------------------------ animation
    @Override
    public void setupAnim(T e, float limb, float amt, float age, float yaw, float pitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        int mask = e.bodyParts();
        for (int i = 0; i < groups.size(); i++) {
            boolean v = (mask & (1 << i)) != 0;
            for (ModelPart p : groups.get(i)) p.visible = v;
        }
        float partial = age - e.tickCount;
        float ca = e.castAge(partial);
        Style style = ca >= 0 ? e.castStyle() : null;
        float w = Math.max(1, e.castWindup());
        float charge = 0, release = 0;
        if (style != null) {
            charge = ca < w ? smooth(ca / w) : Math.max(0, 1 - (ca - w - 4) / 8F);
            release = ca < w ? 0 : Math.max(0, 1 - (ca - w) / 6F);
        }
        animate(e, limb, Math.min(amt, 1.0F), age, yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD, style, charge, release, ca);
    }

    protected abstract void animate(T e, float limb, float amt, float age, float yaw, float pitch, @Nullable Style style, float charge, float release, float castAge);

    protected static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    protected static float swing(float limb, float speed, float phase) { return Mth.cos(limb * speed + phase); }

    protected static void look(@Nullable ModelPart head, float yaw, float pitch) {
        if (head == null) return;
        head.yRot += yaw;
        head.xRot += pitch;
    }

    /** tail sway: the part and every child chain named name_tip */
    protected static void sway(@Nullable ModelPart p, float age, float amount, float speed) {
        if (p == null) return;
        p.yRot += Mth.sin(age * speed) * amount;
    }

    /** generic arm pose for cast styles on a biped-like body */
    protected static void armsCast(@Nullable ModelPart r, @Nullable ModelPart l, @Nullable ModelPart head, @Nullable ModelPart body,
                                   @Nullable Style style, float charge, float release, float castAge) {
        if (style == null || r == null || l == null) return;
        switch (style) {
            case RAISE -> {
                r.xRot = Mth.lerp(charge, r.xRot, -2.7F); l.xRot = Mth.lerp(charge, l.xRot, -2.7F);
                r.zRot += 0.35F * charge; l.zRot -= 0.35F * charge;
                if (head != null) head.xRot -= 0.35F * charge;
                r.xRot += release * 1.2F; l.xRot += release * 1.2F;
            }
            case SHOOT -> {
                r.xRot = Mth.lerp(charge, r.xRot, -1.65F) - release * 0.4F;
                r.yRot -= 0.15F * charge;
                l.xRot = Mth.lerp(charge, l.xRot, -0.6F);
            }
            case SLAM -> {
                float up = release > 0 ? -2.9F + (1 - release) * 2.6F : -2.9F * charge;
                r.xRot = up; l.xRot = up;
                if (body != null) body.xRot += (release > 0 ? 0.35F * (1 - release) : -0.15F * charge);
            }
            case ROAR -> {
                r.zRot += 1.1F * charge; l.zRot -= 1.1F * charge;
                r.xRot -= 0.4F * charge; l.xRot -= 0.4F * charge;
                if (head != null) head.xRot -= 0.6F * charge;
                if (body != null) body.xRot -= 0.15F * charge;
            }
            case LUNGE, SWIPE -> {
                r.xRot = Mth.lerp(charge, r.xRot, -1.9F) + release * 1.6F;
                l.xRot = Mth.lerp(charge, l.xRot, -1.2F);
                r.yRot += (style == Style.SWIPE ? 0.8F * release - 0.6F * charge : 0);
                if (body != null) body.xRot += 0.3F * charge;
            }
            case BREATH -> {
                r.xRot -= 0.5F * charge; l.xRot -= 0.5F * charge;
                r.zRot += 0.6F * charge; l.zRot -= 0.6F * charge;
                if (head != null) head.xRot += 0.15F * charge;
            }
            case SELF -> {
                r.xRot = Mth.lerp(charge, r.xRot, -1.1F); r.yRot += 0.7F * charge;
                l.xRot = Mth.lerp(charge, l.xRot, -1.1F); l.yRot -= 0.7F * charge;
            }
            case SPIN, BURROW -> {
                r.zRot += 1.4F * charge; l.zRot -= 1.4F * charge;
            }
        }
    }

    /** whole-body effects shared by all archetypes */
    protected void bodyCast(@Nullable Style style, float charge, float castAge) {
        if (style == Style.SPIN && castAge >= 0) root.yRot += castAge * 0.9F;
        if (style == Style.BURROW && castAge >= 0) root.y += Math.min(castAge * 1.2F, 40) * (castAge < 50 ? 1 : 0);
    }

    protected static void melee(@Nullable ModelPart r, @Nullable ModelPart l, float attackTime) {
        if (attackTime <= 0 || r == null) return;
        float s = Mth.sin(attackTime * Mth.PI);
        float s2 = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
        r.xRot -= s2 * 1.8F;
        r.yRot += s * 0.4F;
        if (l != null) l.xRot -= s * 0.6F;
    }
}
