package com.krolasyon.labirent.client.render;

import com.krolasyon.labirent.entity.GladerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/** Gladers wear the default player skins (Teresa the slim one) and hold their job's tool. */
public class GladerRenderer extends HumanoidMobRenderer<GladerEntity, PlayerModel<GladerEntity>> {
    private final PlayerModel<GladerEntity> wide;
    private final PlayerModel<GladerEntity> slim;

    public GladerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = this.model;
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public void render(GladerEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        this.model = e.getSkin() >= GladerEntity.SKINS ? slim : wide;
        this.model.crouching = e.isNewcomer();
        this.model.rightArmPose = e.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        super.render(e, yaw, partial, pose, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GladerEntity e) {
        int s = e.getSkin();
        if (s >= GladerEntity.SKINS) return new ResourceLocation("textures/entity/player/slim/alex.png");
        return new ResourceLocation("textures/entity/player/wide/" + GladerEntity.SKIN_NAMES[Math.max(0, s)] + ".png");
    }

    @Override
    protected void scale(GladerEntity e, PoseStack pose, float partial) { pose.scale(0.9375F, 0.9375F, 0.9375F); }
}
