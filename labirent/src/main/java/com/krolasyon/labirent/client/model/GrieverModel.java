package com.krolasyon.labirent.client.model;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.entity.GrieverEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Keyframe-animated Griever with smooth blending between idle, walk, run and its attacks. */
public class GrieverModel extends HierarchicalModel<GrieverEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(Labirent.MODID, "griever"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final Vector3f cache = new Vector3f();

    public GrieverModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("base").getChild("body").getChild("head");
    }

    @Override
    public ModelPart root() { return root; }

    private void play(AnimationDefinition def, float seconds, float weight) {
        if (weight <= 0.001F) return;
        KeyframeAnimations.animate(this, def, (long) (seconds * 1000F), weight, cache);
    }

    @Override
    public void setupAnim(GrieverEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        if (e.deathTime > 0 || !e.isAlive()) {
            play(GrieverAnimations.DEATH, Math.min(2.4F, (e.deathTime + ageInTicks - e.tickCount) / 20F), 1F);
            return;
        }
        float castW = 0F;
        AnimationDefinition cast = null;
        float castT = 0F;
        if (e.clientAnim >= 0) {
            cast = switch (e.clientAnim) {
                case GrieverEntity.ANIM_CLAW -> GrieverAnimations.CLAW;
                case GrieverEntity.ANIM_STING -> GrieverAnimations.STING;
                case GrieverEntity.ANIM_ROLL -> GrieverAnimations.ROLL;
                default -> null;
            };
            castT = (ageInTicks - e.clientAnimStart) / 20F;
            if (cast == null || castT >= cast.lengthInSeconds()) {
                e.clientAnim = -1;
                cast = null;
            } else castW = Mth.clamp(castT / 0.1F, 0F, 1F) * Mth.clamp((cast.lengthInSeconds() - castT) / 0.2F, 0F, 1F);
        }
        float base = 1F - castW;
        float move = Math.min(limbSwingAmount * 2.0F, 1F);
        float run = Mth.clamp((limbSwingAmount - 0.45F) * 3F, 0F, 1F);
        play(GrieverAnimations.IDLE, ageInTicks / 20F, base * (1F - move));
        if (move > 0.001F) {
            play(GrieverAnimations.WALK, limbSwing * 0.09F, base * move * (1F - run));
            play(GrieverAnimations.RUN, limbSwing * 0.06F, base * move * run);
        }
        if (cast != null) play(cast, castT, castW);
        float look = 1F - castW * 0.7F;
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.5F * look;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F * look;
    }
}
