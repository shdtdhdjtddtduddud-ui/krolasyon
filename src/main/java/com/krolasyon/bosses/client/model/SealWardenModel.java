package com.krolasyon.bosses.client.model;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.SealWardenEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class SealWardenModel extends BossModel<SealWardenEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "seal_warden"), "main");

    public SealWardenModel(ModelPart root) {
        super(root, "head");
    }

    @Override protected AnimationDefinition idle() { return SealWardenAnimations.IDLE; }
    @Override protected AnimationDefinition walk() { return SealWardenAnimations.WALK; }
    @Override protected AnimationDefinition run() { return SealWardenAnimations.RUN; }
    @Override protected AnimationDefinition death() { return SealWardenAnimations.DEATH; }
    @Override protected float walkSpeed() { return 1.9F; }
    @Override protected float runSpeed() { return 1.7F; }

    @Override
    protected AnimationDefinition ability(int id) {
        return switch (id) {
            case SealWardenEntity.MELEE -> SealWardenAnimations.ATTACK;
            case SealWardenEntity.LASER -> SealWardenAnimations.CAST_LASER;
            case SealWardenEntity.CRYSTALS -> SealWardenAnimations.CAST_GROUND;
            case SealWardenEntity.PRISON -> SealWardenAnimations.CAST_PRISON;
            case SealWardenEntity.BLINK -> SealWardenAnimations.BLINK;
            case SealWardenEntity.SUMMON -> SealWardenAnimations.SUMMON;
            default -> null;
        };
    }
}
