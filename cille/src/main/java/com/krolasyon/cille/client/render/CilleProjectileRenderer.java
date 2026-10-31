package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.SimpleModel;
import com.krolasyon.cille.entity.CilleProjectileEntity;
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

/** exploding fish / torpedo fish (glowing belly, nose-first along the flight path) and falling stars */
public class CilleProjectileRenderer extends EntityRenderer<CilleProjectileEntity> {
    private static final ResourceLocation FISH = CilleRenderer.tex("fish");
    private static final ResourceLocation FISH_GLOW = CilleRenderer.tex("fish_glow");
    private static final ResourceLocation STAR = CilleRenderer.tex("star");
    private final SimpleModel<CilleProjectileEntity> fish;

    public CilleProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.fish = new SimpleModel<>(ctx.bakeLayer(SimpleModel.FISH));
    }

    @Override
    public void render(CilleProjectileEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        pose.pushPose();
        pose.translate(0, 0.3, 0);
        if (e.getKind() == CilleProjectileEntity.KIND_ROCK) {
            // a torn-out boulder: the real block, tumbling
            pose.mulPose(Axis.XP.rotationDegrees(age * 18F));
            pose.mulPose(Axis.ZP.rotationDegrees(age * 11F));
            pose.scale(1.4F, 1.4F, 1.4F);
            pose.translate(-0.5, -0.5, -0.5);
            net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(e.getBlock(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        } else if (e.getKind() == CilleProjectileEntity.KIND_STAR) {
            float r = 0.35F + 0.06F * Mth.sin(age * 1.3F);
            pose.mulPose(Axis.YP.rotationDegrees(age * 12F));
            SmoothSphere.draw(pose, buffers.getBuffer(RenderType.eyes(STAR)), r, 16, 10, 1F, 1F, 1F, 1F, LightTexture.FULL_BRIGHT);
            SmoothSphere.draw(pose, buffers.getBuffer(RenderType.eyes(STAR)), r * 1.8F, 16, 10, 0.25F, 0.3F, 0.5F, 1F, LightTexture.FULL_BRIGHT);
        } else {
            float yRot = Mth.lerp(partial, e.yRotO, e.getYRot());
            float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
            pose.mulPose(Axis.YP.rotationDegrees(yRot + 180F));
            pose.mulPose(Axis.XP.rotationDegrees(xRot));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.8F) * 12F));
            float s = e.getKind() == CilleProjectileEntity.KIND_TORPEDO ? 1.1F : 0.8F;
            pose.scale(-s, -s, s);
            fish.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(FISH)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
            fish.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(FISH_GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CilleProjectileEntity e) { return FISH; }
}
