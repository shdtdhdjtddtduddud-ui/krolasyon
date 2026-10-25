package com.krolasyon.labirent.client.render;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.client.model.GrieverModel;
import com.krolasyon.labirent.entity.GrieverEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GrieverRenderer extends MobRenderer<GrieverEntity, GrieverModel> {
    private static final ResourceLocation TEX = new ResourceLocation(Labirent.MODID, "textures/entity/griever.png");

    public GrieverRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GrieverModel(ctx.bakeLayer(GrieverModel.LAYER)), 1.3F);
        addLayer(new GlowLayer<>(this, new ResourceLocation(Labirent.MODID, "textures/entity/griever_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(GrieverEntity e) { return TEX; }

    @Override
    protected void scale(GrieverEntity e, PoseStack pose, float partial) { pose.scale(1.15F, 1.15F, 1.15F); }

    /** no vanilla death flip: the model has its own collapse */
    @Override
    protected void setupRotations(GrieverEntity e, PoseStack pose, float age, float yaw, float partial) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }
}
