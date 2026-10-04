package com.krolasyon.futbol.client;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.entity.FootballerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FootballerRenderer extends MobRenderer<FootballerEntity, AnimatedPlayerModel<FootballerEntity>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[FootballerEntity.SKINS];

    static {
        for (int i = 0; i < SKINS.length; i++) SKINS[i] = new ResourceLocation(FutbolMod.MODID, "textures/entity/footballer_" + i + ".png");
    }

    public FootballerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AnimatedPlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new JerseyLayer<>(this, ctx.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(FootballerEntity e) { return SKINS[Math.floorMod(e.getSkin(), SKINS.length)]; }

    @Override
    protected void scale(FootballerEntity e, PoseStack ps, float partial) { ps.scale(0.9375F, 0.9375F, 0.9375F); }

    @Override
    public void render(FootballerEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ClientAnims.applyRoot(ps, e, partial);
        super.render(e, yaw, partial, ps, buf, light);
        ps.popPose();
    }
}
