package com.rabona.arena.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/** Vanilla oyuncu modeli + Rabona hareket animasyonlari. */
public class AnimatedPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
    public AnimatedPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbAmount, age, headYaw, headPitch);
        if (age > 0) ClientAnims.apply(this, e, age, limbSwing, limbAmount);
    }
}
