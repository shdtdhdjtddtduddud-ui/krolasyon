package com.krolasyon.labirent.client.render;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.client.model.BeetleBladeModel;
import com.krolasyon.labirent.entity.BeetleBladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BeetleBladeRenderer extends MobRenderer<BeetleBladeEntity, BeetleBladeModel> {
    private static final ResourceLocation TEX = new ResourceLocation(Labirent.MODID, "textures/entity/beetle_blade.png");

    public BeetleBladeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BeetleBladeModel(ctx.bakeLayer(BeetleBladeModel.LAYER)), 0.3F);
        addLayer(new GlowLayer<>(this, new ResourceLocation(Labirent.MODID, "textures/entity/beetle_blade_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(BeetleBladeEntity e) { return TEX; }

    @Override
    protected void setupRotations(BeetleBladeEntity e, PoseStack pose, float age, float yaw, float partial) {
        super.setupRotations(e, pose, age, yaw, partial);
        if (e.isClimbing()) {
            pose.translate(0, 0.25, 0.1);
            pose.mulPose(Axis.XP.rotationDegrees(-90F));
        }
    }
}
