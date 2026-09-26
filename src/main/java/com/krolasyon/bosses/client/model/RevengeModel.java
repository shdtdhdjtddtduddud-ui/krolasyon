package com.krolasyon.bosses.client.model;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.RevengeEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class RevengeModel extends BossModel<RevengeEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "revenge"), "main");

    public RevengeModel(ModelPart root) {
        super(root, "head");
    }

    @Override protected AnimationDefinition idle() { return RevengeAnimations.IDLE; }
    @Override protected AnimationDefinition walk() { return RevengeAnimations.WALK; }
    @Override protected AnimationDefinition run() { return RevengeAnimations.RUN; }
    @Override protected AnimationDefinition death() { return RevengeAnimations.DEATH; }
    @Override protected float walkSpeed() { return 1.9F; }
    @Override protected float runSpeed() { return 1.6F; }

    @Override
    protected AnimationDefinition ability(int id) {
        return switch (id) {
            case RevengeEntity.MELEE -> RevengeAnimations.ATTACK;
            case RevengeEntity.LANCE -> RevengeAnimations.LANCE;
            case RevengeEntity.TIDE -> RevengeAnimations.TIDE;
            case RevengeEntity.DANCE -> RevengeAnimations.DANCE;
            case RevengeEntity.DRAIN -> RevengeAnimations.DRAIN;
            case RevengeEntity.VENGEANCE -> RevengeAnimations.VENGEANCE;
            default -> null;
        };
    }
}
