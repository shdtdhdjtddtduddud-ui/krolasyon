package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.client.model.SimpleEffectModel;
import com.krolasyon.bosses.entity.BloodLanceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BloodLanceRenderer extends EntityRenderer<BloodLanceEntity> {
    private static final ResourceLocation TEX = EruptionRenderer.tex("blood_lance"), GLOW = EruptionRenderer.tex("blood_lance_glow");
    private final SimpleEffectModel<BloodLanceEntity> model;

    public BloodLanceRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new SimpleEffectModel<>(ctx.bakeLayer(SimpleEffectModel.BLOOD_LANCE));
    }

    @Override
    public void render(BloodLanceEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float y = Mth.lerp(partial, e.yRotO, e.getYRot());
        float p = Mth.lerp(partial, e.xRotO, e.getXRot());
        pose.pushPose();
        pose.translate(0.0F, 0.25F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(y));
        pose.mulPose(Axis.XP.rotationDegrees(-p));
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        pose.mulPose(Axis.ZP.rotationDegrees((e.tickCount + partial) * 20F));
        pose.translate(0.0F, -0.5F, 0.0F);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutout(TEX)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodLanceEntity e) { return TEX; }
}
