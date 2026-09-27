package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.model.SimpleEffectModel;
import com.krolasyon.bosses.entity.TideGeyserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TideGeyserRenderer extends EntityRenderer<TideGeyserEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "tide_geyser"), "main");
    private static final ResourceLocation TEX = EruptionRenderer.tex("tide_geyser"), GLOW = EruptionRenderer.tex("tide_geyser_glow");
    private final SimpleEffectModel<TideGeyserEntity> model;

    public TideGeyserRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new SimpleEffectModel<>(ctx.bakeLayer(LAYER));
    }

    @Override
    public void render(TideGeyserEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partial - e.getWarmup();
        if (t < 0) return;
        float s;
        if (t < 3F) s = Mth.sin(t / 3F * Mth.HALF_PI) * 1.2F;
        else if (t < 6F) s = 1.2F - (t - 3F) / 3F * 0.2F;
        else if (t < 15F) s = 1F + 0.05F * Mth.sin(t * 1.9F);
        else s = Mth.clamp(1F - (t - 15F) / 8F, 0F, 1F);
        if (s <= 0.01F) return;
        float age = e.tickCount + partial;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot() + age * 11F));
        float w = 0.75F + 0.08F * Mth.sin(age * 2.3F);
        pose.scale(w * Math.min(1F, s + 0.3F), s, w * Math.min(1F, s + 0.3F));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TideGeyserEntity e) { return TEX; }
}
