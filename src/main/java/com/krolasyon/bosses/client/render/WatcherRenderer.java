package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.client.model.SimpleEffectModel;
import com.krolasyon.bosses.entity.WatcherOrbEntity;
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

public class WatcherRenderer extends EntityRenderer<WatcherOrbEntity> {
    private static final ResourceLocation TEX = EruptionRenderer.tex("watcher"), GLOW = EruptionRenderer.tex("watcher_glow");
    private final SimpleEffectModel<WatcherOrbEntity> model;

    public WatcherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new SimpleEffectModel<>(ctx.bakeLayer(SimpleEffectModel.WATCHER));
        this.shadowRadius = 0.2F;
    }

    @Override
    public void render(WatcherOrbEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float y = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        float p = Mth.lerp(partial, e.xRotO, e.getXRot());
        float age = e.tickCount + partial;
        float appear = Mth.clamp(age / 10F, 0F, 1F);
        pose.pushPose();
        pose.translate(0.0F, 0.5F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180F - y));
        pose.mulPose(Axis.XP.rotationDegrees(-p));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 4F));
        pose.scale(appear, appear, appear);
        pose.translate(0.0F, -0.5F, 0.0F);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutout(TEX)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WatcherOrbEntity e) { return TEX; }
}
