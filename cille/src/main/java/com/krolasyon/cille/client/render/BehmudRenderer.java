package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.BehmudModel;
import com.krolasyon.cille.entity.BehmudEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BehmudRenderer extends CilleRenderer<BehmudEntity, BehmudModel> {
    private static final ResourceLocation POWER_SWIRL = new ResourceLocation("textures/entity/creeper/creeper_armor.png");

    public BehmudRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BehmudModel(ctx.bakeLayer(BehmudModel.LAYER)), "behmud", 2.0F, 0.5F);
        this.addLayer(new SealedPowerLayer(this));
    }

    @Override protected float mouthForward() { return 2.9F; }
    @Override protected float mouthUp() { return 0.8F; }
    @Override protected float[] beamColors() { return new float[]{0.3F, 0.7F, 1.0F, 0.6F, 0.9F, 1.0F, 0.95F, 1.0F, 1.0F}; }
    @Override protected float shieldRadius() { return 3.8F; }
    @Override protected float shieldCenter() { return 1.1F; }

    /** banks into turns and noses up/down with its flight */
    @Override
    protected void setupRotations(BehmudEntity e, PoseStack pose, float ageInTicks, float yaw, float partial) {
        super.setupRotations(e, pose, ageInTicks, yaw, partial);
        if (e.deathTime > 0) return;
        double vy = e.getDeltaMovement().y;
        float pitch = Mth.clamp((float) vy * 60F, -25F, 25F);
        pose.translate(0, 1.1, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        pose.translate(0, -1.1, 0);
    }

    /** sealed power unleashed: crackling energy skin over the whole body */
    static class SealedPowerLayer extends RenderLayer<BehmudEntity, BehmudModel> {
        SealedPowerLayer(RenderLayerParent<BehmudEntity, BehmudModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, BehmudEntity e, float limbSwing, float limbAmount,
                           float partial, float age, float headYaw, float headPitch) {
            if (!e.isEmpowered()) return;
            float f = e.tickCount + partial;
            pose.pushPose();
            pose.translate(0, 1.0, 0);
            pose.scale(1.04F, 1.04F, 1.04F);
            pose.translate(0, -1.0, 0);
            this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.energySwirl(POWER_SWIRL, (f * 0.01F) % 1F, (f * 0.01F) % 1F)),
                    light, OverlayTexture.NO_OVERLAY, 0.45F, 0.85F, 1.0F, 1.0F);
            pose.popPose();
        }
    }
}
