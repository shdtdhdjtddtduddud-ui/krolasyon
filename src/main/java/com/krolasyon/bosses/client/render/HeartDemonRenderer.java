package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.client.model.HeartDemonModel;
import com.krolasyon.bosses.entity.HeartDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HeartDemonRenderer extends BossRenderer<HeartDemonEntity, HeartDemonModel> {
    public HeartDemonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HeartDemonModel(ctx.bakeLayer(HeartDemonModel.LAYER)), "heart_demon", 1.0F, 0.5F);
    }
}
