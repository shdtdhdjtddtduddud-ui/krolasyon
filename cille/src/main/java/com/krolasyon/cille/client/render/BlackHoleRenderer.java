package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.SimpleModel;
import com.krolasyon.cille.entity.BlackHoleEntity;
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

/** pitch-black core with a glowing violet event horizon and two spinning accretion disks */
public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    private static final ResourceLocation CORE = KabkabaRenderer.tex("black_hole_core");
    private static final ResourceLocation CORE_GLOW = KabkabaRenderer.tex("black_hole_core_glow");
    private static final ResourceLocation DISK = KabkabaRenderer.tex("black_hole_disk");
    private final SimpleModel<BlackHoleEntity> sphere;

    public BlackHoleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.sphere = new SimpleModel<>(ctx.bakeLayer(SimpleModel.SPHERE));
    }

    @Override
    public void render(BlackHoleEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.size(partial);
        if (s <= 0.01F) return;
        float age = e.tickCount + partial;
        float big = e.isBig() ? 1.25F : 1F;
        pose.pushPose();
        pose.translate(0, 0.75, 0);

        pose.pushPose();
        float core = 2.6F * s * big * (1F + 0.05F * Mth.sin(age * 0.7F));
        pose.mulPose(Axis.YP.rotationDegrees(age * 4F));
        pose.scale(core, core, core);
        sphere.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CORE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        sphere.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(CORE_GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();

        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(i == 0 ? 14F : -24F));
            pose.mulPose(Axis.ZP.rotationDegrees(i == 0 ? 6F : -10F));
            pose.mulPose(Axis.YP.rotationDegrees(age * (i == 0 ? 7F : -11F)));
            FxUtil.flatQuad(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(DISK)), (i == 0 ? 4.6F : 3.4F) * s * big, LightTexture.FULL_BRIGHT, i == 0 ? 0.95F : 0.7F);
            pose.popPose();
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity e) { return CORE; }
}
