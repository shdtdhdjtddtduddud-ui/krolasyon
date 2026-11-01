package com.krolasyon.cille.client.model;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.entity.NirumendEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class NirumendModel extends CilleModel<NirumendEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CilleMod.MODID, "nirumend"), "main");

    public NirumendModel(ModelPart root) {
        super(root, root.getChild("base").getChild("body").getChild("head"));
    }

    @Override protected String meshName() { return "nirumend"; }
    @Override protected AnimationDefinition idle() { return NirumendAnimations.IDLE; }
    @Override protected AnimationDefinition walk() { return NirumendAnimations.WALK; }
    @Override protected AnimationDefinition run() { return NirumendAnimations.RUN; }
    @Override protected AnimationDefinition rest() { return NirumendAnimations.SIT; }
    @Override protected AnimationDefinition death() { return NirumendAnimations.DEATH; }
    @Override protected float walkSpeed() { return 0.7F; }
    @Override protected float runSpeed() { return 0.6F; }

    @Override
    protected AnimationDefinition ability(int id) {
        return switch (id) {
            case NirumendEntity.MELEE -> NirumendAnimations.ATTACK;
            case NirumendEntity.BUMEHEN -> NirumendAnimations.BUMEHEN;
            case NirumendEntity.AJIRAK -> NirumendAnimations.AJIRAK;
            case NirumendEntity.SKULL_BASH -> NirumendAnimations.SKULL_BASH;
            case NirumendEntity.HARD_ROCK -> NirumendAnimations.HARD_ROCK;
            case NirumendEntity.ELECTRIC_SHIELD -> NirumendAnimations.ELECTRIC_SHIELD;
            case NirumendEntity.DUST_CLOUD -> NirumendAnimations.DUST_CLOUD;
            case NirumendEntity.STOMP -> NirumendAnimations.STOMP;
            case NirumendEntity.RELEASE -> NirumendAnimations.RELEASE;
            case NirumendEntity.RECALL -> NirumendAnimations.RECALL;
            default -> null;
        };
    }
}
