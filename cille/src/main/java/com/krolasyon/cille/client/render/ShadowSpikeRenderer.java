package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.SimpleModel;
import com.krolasyon.cille.entity.ShadowSpikeEntity;
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

public class ShadowSpikeRenderer extends EntityRenderer<ShadowSpikeEntity> {
    private static final ResourceLocation TEX = KabkabaRenderer.tex("shadow_spike");
    private static final ResourceLocation GLOW = KabkabaRenderer.tex("shadow_spike_glow");
    private final SimpleModel<ShadowSpikeEntity> model;

    public ShadowSpikeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new SimpleModel<>(ctx.bakeLayer(SimpleModel.SHADOW_SPIKE));
    }

    @Override
    public void render(ShadowSpikeEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partial - e.getWarmup();
        if (t < 0) return;
        float s;
        if (t < 3F) s = Mth.sin(t / 3F * Mth.HALF_PI) * 1.18F;
        else if (t < 5F) s = 1.18F - (t - 3F) / 2F * 0.18F;
        else if (t < 15F) s = 1F;
        else s = Mth.clamp(1F - (t - 15F) / 8F, 0F, 1F);
        if (s <= 0.01F) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        pose.scale(0.85F * Math.min(1F, s + 0.3F), 0.85F * s, 0.85F * Math.min(1F, s + 0.3F));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowSpikeEntity e) { return TEX; }
}
