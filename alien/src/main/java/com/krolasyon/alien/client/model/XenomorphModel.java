package com.krolasyon.alien.client.model;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.entity.XenomorphEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Keyframe animated xenomorph with weighted blending between locomotion and one-shot actions. */
public class XenomorphModel extends HierarchicalModel<XenomorphEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(AlienMod.MODID, "xenomorph"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart neck;
    private final Vector3f cache = new Vector3f();

    public XenomorphModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.head = root.getChild("base").getChild("hips").getChild("waist").getChild("chest").getChild("neck").getChild("head");
        this.neck = root.getChild("base").getChild("hips").getChild("waist").getChild("chest").getChild("neck");
    }

    @Override
    public ModelPart root() { return root; }

    private void play(AnimationDefinition def, float seconds, float weight) {
        if (weight <= 0.001F) return;
        KeyframeAnimations.animate(this, def, (long) (seconds * 1000F), weight, cache);
    }

    private static AnimationDefinition action(int id) {
        return switch (id) {
            case XenomorphEntity.ANIM_SEARCH -> XenoAnimations.SEARCH;
            case XenomorphEntity.ANIM_HISS -> XenoAnimations.HISS;
            case XenomorphEntity.ANIM_ATTACK -> XenoAnimations.ATTACK;
            case XenomorphEntity.ANIM_KILL -> XenoAnimations.KILL;
            case XenomorphEntity.ANIM_VENT_ENTER -> XenoAnimations.VENT_ENTER;
            case XenomorphEntity.ANIM_VENT_EXIT -> XenoAnimations.VENT_EXIT;
            case XenomorphEntity.ANIM_FLINCH -> XenoAnimations.FLINCH;
            case XenomorphEntity.ANIM_LOCKER -> XenoAnimations.LOCKER_CHECK;
            default -> null;
        };
    }

    @Override
    public void setupAnim(XenomorphEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partial = ageInTicks - e.tickCount;
        if (e.deathTime > 0 || !e.isAlive()) {
            play(XenoAnimations.DEATH, (e.deathTime + partial) / 20F, 1F);
            return;
        }
        AnimationDefinition act = null;
        float actT = 0F, actW = 0F;
        if (e.clientAnimId >= 0) {
            act = action(e.clientAnimId);
            actT = (ageInTicks - e.clientAnimStart) / 20F;
            float len = act == null ? 0F : act.lengthInSeconds();
            if (act == null || actT >= len) {
                e.clientAnimId = -1;
                act = null;
            } else {
                boolean hard = e.clientAnimId == XenomorphEntity.ANIM_VENT_EXIT || e.clientAnimId == XenomorphEntity.ANIM_VENT_ENTER;
                actW = (hard ? 1F : Mth.clamp(actT / 0.12F, 0F, 1F)) * Mth.clamp((len - actT) / 0.25F, 0F, 1F);
                if (hard && e.clientAnimId == XenomorphEntity.ANIM_VENT_ENTER) actW = 1F;
            }
        }
        float base = 1F - actW;
        float move = Math.min(limbSwingAmount * 2.4F, 1F);
        float run = Mth.lerp(partial, e.prevRunBlend, e.runBlend);
        play(XenoAnimations.IDLE, ageInTicks / 20F, base * (1F - move));
        if (move > 0.001F) {
            play(XenoAnimations.WALK, limbSwing * 0.2F, base * move * (1F - run));
            play(XenoAnimations.RUN, limbSwing * 0.05F, base * move * run);
        }
        if (act != null) play(act, actT, actW);
        float look = 1F - actW * 0.8F;
        this.neck.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.3F * look;
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.4F * look;
        this.head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F * look;
    }
}
