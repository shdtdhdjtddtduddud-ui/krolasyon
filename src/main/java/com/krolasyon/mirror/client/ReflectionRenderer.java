package com.krolasyon.mirror.client;

import com.krolasyon.mirror.entity.ReflectionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ReflectionRenderer extends MobRenderer<ReflectionEntity, PlayerModel<ReflectionEntity>> {
    private final PlayerModel<ReflectionEntity> wide, slim;

    public ReflectionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        wide = model;
        slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(ReflectionEntity entity, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        model = SkinHelper.slim(entity.getOwnerId()) ? slim : wide;
        super.render(entity, yaw, pt, ps, buf, light);
    }

    @Override
    protected void scale(ReflectionEntity entity, PoseStack ps, float pt) {
        ps.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public ResourceLocation getTextureLocation(ReflectionEntity entity) {
        return SkinHelper.skin(entity.getOwnerId());
    }
}
