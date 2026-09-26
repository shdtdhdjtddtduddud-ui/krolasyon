package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.BossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BossRenderer<T extends BossEntity, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;
    private final float modelScale;

    public BossRenderer(EntityRendererProvider.Context ctx, M model, String name, float shadow, float modelScale) {
        super(ctx, model, shadow);
        this.texture = new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/" + name + ".png");
        this.modelScale = modelScale;
        this.addLayer(new GlowLayer<>(this, new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/" + name + "_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) { return texture; }

    @Override
    protected void scale(T entity, PoseStack pose, float partial) {
        pose.scale(modelScale, modelScale, modelScale);
    }

    /** no vanilla "fall over sideways" death flip — the model plays its own death animation */
    @Override
    protected void setupRotations(T entity, PoseStack pose, float ageInTicks, float yaw, float partial) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }

    @Override
    protected float getFlipDegrees(T entity) { return 0F; }
}
