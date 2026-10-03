package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.client.model.MobModel;
import com.krolasyon.bosses.entity.HellMob;
import com.krolasyon.bosses.entity.mob.MobSpec;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HellMobRenderer extends BossRenderer<HellMob, MobModel> {
    public HellMobRenderer(EntityRendererProvider.Context ctx, MobSpec spec) {
        super(ctx, new MobModel(ctx.bakeLayer(MobModel.layer(spec.id)), spec), spec.id, spec.shadow, spec.modelScale);
    }
}
