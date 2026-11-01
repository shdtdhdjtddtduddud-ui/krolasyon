package com.krolasyon.cille.client.model;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.entity.KabkabaEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class KabkabaModel extends CilleModel<KabkabaEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CilleMod.MODID, "kabkaba"), "main");

    public KabkabaModel(ModelPart root) {
        super(root, root.getChild("base").getChild("body").getChild("chest").getChild("mane").getChild("head"));
    }

    @Override protected String meshName() { return "kabkaba"; }
    @Override protected AnimationDefinition idle() { return KabkabaAnimations.IDLE; }
    @Override protected AnimationDefinition walk() { return KabkabaAnimations.WALK; }
    @Override protected AnimationDefinition run() { return KabkabaAnimations.RUN; }
    @Override protected AnimationDefinition rest() { return KabkabaAnimations.SIT; }
    @Override protected AnimationDefinition death() { return KabkabaAnimations.DEATH; }
    @Override protected float walkSpeed() { return 0.9F; }
    @Override protected float runSpeed() { return 0.75F; }

    @Override
    protected boolean hardCut(int id) { return super.hardCut(id) || id == KabkabaEntity.BLINK || id == KabkabaEntity.DODGE; }

    @Override
    protected AnimationDefinition ability(int id) {
        return switch (id) {
            case KabkabaEntity.MELEE -> KabkabaAnimations.ATTACK;
            case KabkabaEntity.BLACK_HOLE -> KabkabaAnimations.BLACK_HOLE;
            case KabkabaEntity.BLINK -> KabkabaAnimations.BLINK;
            case KabkabaEntity.DRAIN -> KabkabaAnimations.DRAIN;
            case KabkabaEntity.REFLECT -> KabkabaAnimations.REFLECT;
            case KabkabaEntity.SHADOW -> KabkabaAnimations.SHADOW;
            case KabkabaEntity.GUARD -> KabkabaAnimations.GUARD;
            case KabkabaEntity.SLAM -> KabkabaAnimations.SLAM;
            case KabkabaEntity.DODGE -> KabkabaAnimations.DODGE;
            case KabkabaEntity.RELEASE -> KabkabaAnimations.RELEASE;
            case KabkabaEntity.RECALL -> KabkabaAnimations.RECALL;
            default -> null;
        };
    }
}
