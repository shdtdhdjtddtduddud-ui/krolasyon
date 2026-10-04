package com.krolasyon.futbol.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/** Vanilla player model that layers the football move animations on top of the vanilla pose. */
public class AnimatedPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
    public AnimatedPlayerModel(ModelPart root, boolean slim) { super(root, slim); }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ClientAnims.applyLimbs(this, e, ageInTicks);
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
    }
}
