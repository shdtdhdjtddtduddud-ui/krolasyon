package com.krolasyon.bosses.client.model;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.HeartDemonEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class HeartDemonModel extends BossModel<HeartDemonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "heart_demon"), "main");

    public HeartDemonModel(ModelPart root) {
        super(root, "head");
    }

    @Override protected AnimationDefinition idle() { return HeartDemonAnimations.IDLE; }
    @Override protected AnimationDefinition walk() { return HeartDemonAnimations.WALK; }
    @Override protected AnimationDefinition run() { return HeartDemonAnimations.RUN; }
    @Override protected AnimationDefinition death() { return HeartDemonAnimations.DEATH; }
    @Override protected float walkSpeed() { return 1.7F; }
    @Override protected float runSpeed() { return 1.3F; }

    @Override
    protected AnimationDefinition ability(int id) {
        return switch (id) {
            case HeartDemonEntity.MELEE -> HeartDemonAnimations.ATTACK;
            case HeartDemonEntity.WHIP -> HeartDemonAnimations.WHIP;
            case HeartDemonEntity.HEART -> HeartDemonAnimations.HEART;
            case HeartDemonEntity.DASH -> HeartDemonAnimations.DASH;
            case HeartDemonEntity.CAGE -> HeartDemonAnimations.CAGE;
            case HeartDemonEntity.SPIN -> HeartDemonAnimations.SPIN;
            default -> null;
        };
    }
}
