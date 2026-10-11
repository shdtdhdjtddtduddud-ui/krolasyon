package com.krolasyon.ben10.client.render;

import com.krolasyon.ben10.entity.DebrisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class DebrisRenderer extends EntityRenderer<DebrisEntity> {
    private final BlockRenderDispatcher blocks;

    public DebrisRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        blocks = ctx.getBlockRenderDispatcher();
        shadowRadius = 0.15F;
    }

    @Override
    public void render(DebrisEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float shrink = Math.min(1F, Math.max(0F, (e.life() - age) / 8F));
        float s = 0.42F * Math.max(0.05F, shrink);
        ps.pushPose();
        ps.translate(0, 0.2, 0);
        ps.mulPose(Axis.XP.rotationDegrees(age * e.spinX));
        ps.mulPose(Axis.ZP.rotationDegrees(age * e.spinZ));
        ps.scale(s, s, s);
        ps.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(e.block(), ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(DebrisEntity e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
