package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.client.model.CrimsonHoundModel;
import com.krolasyon.bosses.entity.CrimsonHoundEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class CrimsonHoundRenderer extends BossRenderer<CrimsonHoundEntity, CrimsonHoundModel> {
    public CrimsonHoundRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CrimsonHoundModel(ctx.bakeLayer(CrimsonHoundModel.LAYER)), "crimson_hound", 1.5F, 0.5F);
    }
}
