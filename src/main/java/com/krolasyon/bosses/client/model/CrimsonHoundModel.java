package com.krolasyon.bosses.client.model;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.CrimsonHoundEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class CrimsonHoundModel extends BossModel<CrimsonHoundEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "crimson_hound"), "main");

    public CrimsonHoundModel(ModelPart root) {
        super(root, "head");
    }

    @Override protected AnimationDefinition idle() { return CrimsonHoundAnimations.IDLE; }
    @Override protected AnimationDefinition walk() { return CrimsonHoundAnimations.WALK; }
    @Override protected AnimationDefinition run() { return CrimsonHoundAnimations.RUN; }
    @Override protected AnimationDefinition death() { return CrimsonHoundAnimations.DEATH; }
    @Override protected float walkSpeed() { return 1.6F; }
    @Override protected float runSpeed() { return 1.1F; }

    @Override
    protected AnimationDefinition ability(int id) {
        return switch (id) {
            case CrimsonHoundEntity.MELEE -> CrimsonHoundAnimations.ATTACK;
            case CrimsonHoundEntity.LEAP -> CrimsonHoundAnimations.LEAP;
            case CrimsonHoundEntity.HOWL -> CrimsonHoundAnimations.HOWL;
            case CrimsonHoundEntity.FISSURE -> CrimsonHoundAnimations.FISSURE;
            case CrimsonHoundEntity.TAIL_BLAST -> CrimsonHoundAnimations.TAIL_BLAST;
            case CrimsonHoundEntity.FRENZY -> CrimsonHoundAnimations.FRENZY;
            default -> null;
        };
    }
}
