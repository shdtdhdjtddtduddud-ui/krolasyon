package com.krolasyon.labirent.client.render;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.client.model.GrieverModel;
import com.krolasyon.labirent.entity.GrieverCorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Draws a dead Griever with the last frame of its death animation. */
public class GrieverCorpseRenderer extends EntityRenderer<GrieverCorpseEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(Labirent.MODID, "textures/entity/griever.png");
    private final GrieverModel model;

    public GrieverCorpseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new GrieverModel(ctx.bakeLayer(GrieverModel.LAYER));
        this.shadowRadius = 1.1F;
    }

    @Override
    public void render(GrieverCorpseEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180F - e.bodyYaw()));
        pose.scale(-1.15F, -1.15F, 1.15F);
        pose.translate(0, -1.501F, 0);
        model.poseCorpse(e.tickCount + partial, e.stripped());
        float dim = e.stripped() ? 0.6F : 0.85F;
        model.renderToBuffer(pose, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY, dim, dim, dim, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GrieverCorpseEntity e) { return TEX; }
}
