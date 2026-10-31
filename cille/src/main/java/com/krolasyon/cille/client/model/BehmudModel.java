package com.krolasyon.cille.client.model;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.entity.BehmudEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class BehmudModel extends CilleModel<BehmudEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CilleMod.MODID, "behmud"), "main");

    public BehmudModel(ModelPart root) {
        super(root, root.getChild("base").getChild("body").getChild("head"));
    }

    @Override protected AnimationDefinition idle() { return BehmudAnimations.IDLE; }
    @Override protected AnimationDefinition walk() { return BehmudAnimations.WALK; }
    @Override protected AnimationDefinition run() { return BehmudAnimations.RUN; }
    @Override protected AnimationDefinition rest() { return BehmudAnimations.SIT; }
    @Override protected AnimationDefinition death() { return BehmudAnimations.DEATH; }
    @Override protected float walkSpeed() { return 0.6F; }
    @Override protected float runSpeed() { return 0.5F; }

    @Override
    protected boolean hardCut(int id) {
        return super.hardCut(id) || id == BehmudEntity.WHIRLPOOL || id == BehmudEntity.QUICK_GUARD || id == BehmudEntity.SPACE || id == BehmudEntity.SEAL;
    }

    @Override
    protected AnimationDefinition ability(int id) {
        return switch (id) {
            case BehmudEntity.MELEE -> BehmudAnimations.ATTACK;
            case BehmudEntity.ICE_BEAM -> BehmudAnimations.ICE_BEAM;
            case BehmudEntity.WHIRLPOOL -> BehmudAnimations.WHIRLPOOL;
            case BehmudEntity.SONIC -> BehmudAnimations.SONIC;
            case BehmudEntity.FISH_BOMB -> BehmudAnimations.FISH_BOMB;
            case BehmudEntity.TORPEDO -> BehmudAnimations.TORPEDO;
            case BehmudEntity.RUSH -> BehmudAnimations.RUSH;
            case BehmudEntity.WATERSPOUT -> BehmudAnimations.WATERSPOUT;
            case BehmudEntity.ICE_SHIELD -> BehmudAnimations.ICE_SHIELD;
            case BehmudEntity.QUICK_GUARD -> BehmudAnimations.QUICK_GUARD;
            case BehmudEntity.ERKEMAS -> BehmudAnimations.ERKEMAS;
            case BehmudEntity.SEAL -> BehmudAnimations.SEAL;
            case BehmudEntity.SPACE -> BehmudAnimations.SPACE;
            case BehmudEntity.RELEASE -> BehmudAnimations.RELEASE;
            case BehmudEntity.RECALL -> BehmudAnimations.RECALL;
            default -> null;
        };
    }
}
