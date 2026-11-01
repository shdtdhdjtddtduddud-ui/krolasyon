package com.krolasyon.cille.client.model;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.entity.SarkanEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class SarkanModel extends CilleModel<SarkanEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CilleMod.MODID, "sarkan"), "main");

    public SarkanModel(ModelPart root) {
        super(root, root.getChild("base").getChild("body").getChild("chest").getChild("head"));
    }

    @Override protected String meshName() { return "sarkan"; }
    @Override protected AnimationDefinition idle() { return SarkanAnimations.IDLE; }
    @Override protected AnimationDefinition walk() { return SarkanAnimations.WALK; }
    @Override protected AnimationDefinition run() { return SarkanAnimations.RUN; }
    @Override protected AnimationDefinition rest() { return SarkanAnimations.SIT; }
    @Override protected AnimationDefinition death() { return SarkanAnimations.DEATH; }
    @Override protected float walkSpeed() { return 1.0F; }
    @Override protected float runSpeed() { return 0.8F; }

    @Override
    protected boolean hardCut(int id) { return super.hardCut(id) || id == SarkanEntity.STORM; }

    @Override
    protected AnimationDefinition ability(int id) {
        return switch (id) {
            case SarkanEntity.MELEE -> SarkanAnimations.ATTACK;
            case SarkanEntity.SAND_VORTEX -> SarkanAnimations.SAND_VORTEX;
            case SarkanEntity.ATLAS -> SarkanAnimations.ATLAS;
            case SarkanEntity.DEVERAN -> SarkanAnimations.DEVERAN;
            case SarkanEntity.STORM -> SarkanAnimations.STORM;
            case SarkanEntity.SAND_SEA -> SarkanAnimations.SAND_SEA;
            case SarkanEntity.SAND_SHIELD -> SarkanAnimations.SAND_SHIELD;
            case SarkanEntity.ROCK -> SarkanAnimations.ROCK;
            case SarkanEntity.WALLS -> SarkanAnimations.WALLS;
            case SarkanEntity.RELEASE -> SarkanAnimations.RELEASE;
            case SarkanEntity.RECALL -> SarkanAnimations.RECALL;
            default -> null;
        };
    }
}
