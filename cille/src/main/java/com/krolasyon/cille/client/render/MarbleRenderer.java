package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.KabkabaModel;
import com.krolasyon.cille.client.model.SimpleModel;
import com.krolasyon.cille.entity.MarbleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** glass marble with a tiny sleeping Kabkaba inside */
public class MarbleRenderer extends EntityRenderer<MarbleEntity> {
    private static final ResourceLocation GLASS = KabkabaRenderer.tex("marble");
    private static final ResourceLocation KABKABA = KabkabaRenderer.tex("kabkaba");
    private final SimpleModel<MarbleEntity> sphere;
    private final KabkabaModel mini;

    public MarbleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.sphere = new SimpleModel<>(ctx.bakeLayer(SimpleModel.SPHERE));
        this.mini = new KabkabaModel(ctx.bakeLayer(KabkabaModel.LAYER));
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(MarbleEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        float spin = Mth.lerp(partial, e.prevSpin, e.spin);
        pose.pushPose();
        pose.translate(0, 0.16, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(spin));
        pose.scale(0.32F, 0.32F, 0.32F);

        // Kabkaba curled up inside
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(age * 2F));
        pose.scale(0.055F, 0.055F, 0.055F);
        pose.scale(-1F, -1F, 1F);
        pose.translate(0, 0.62F, 0);
        mini.poseStatic(age / 20F);
        mini.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(KABKABA)), light, OverlayTexture.NO_OVERLAY, 0.55F, 0.6F, 1F, 1F);
        pose.popPose();

        sphere.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(GLASS)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MarbleEntity e) { return GLASS; }
}
