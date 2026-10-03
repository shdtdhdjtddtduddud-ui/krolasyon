package com.krolasyon.bosses.realm.client.render;

import com.krolasyon.bosses.client.render.BossRenderer;
import com.krolasyon.bosses.realm.client.gen.GenModels;
import com.krolasyon.bosses.realm.entity.RealmBoss;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RealmBossRenderer extends BossRenderer<RealmBoss, GenBossModel> {
    public RealmBossRenderer(EntityRendererProvider.Context ctx, GenModels.Info info) {
        super(ctx, new GenBossModel(ctx.bakeLayer(RealmMobRenderer.layer(info.id())), info), "realm/" + info.id(), info.shadow(), info.scale());
    }
}
