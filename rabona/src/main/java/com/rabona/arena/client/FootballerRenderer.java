package com.rabona.arena.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rabona.arena.RabonaArena;
import com.rabona.arena.entity.FootballerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class FootballerRenderer extends HumanoidMobRenderer<FootballerEntity, AnimatedPlayerModel<FootballerEntity>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[FootballerEntity.SKINS];

    static {
        for (int i = 0; i < SKINS.length; i++) SKINS[i] = RabonaArena.id("textures/entity/footballer/skin_" + i + ".png");
    }

    public FootballerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AnimatedPlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new KitLayer<>(this, new PlayerModel<>(ctx.bakeLayer(ClientSetup.KIT), false)));
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(FootballerEntity e) {
        return SKINS[Math.floorMod(e.getSkinId(), SKINS.length)];
    }

    @Override
    protected void scale(FootballerEntity e, PoseStack ps, float partial) {
        ps.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
