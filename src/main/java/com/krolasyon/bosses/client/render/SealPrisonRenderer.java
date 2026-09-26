package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.entity.SealPrisonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the spinning triangle seal rune on the ground. */
public class SealPrisonRenderer extends EntityRenderer<SealPrisonEntity> {
    private static final ResourceLocation RUNE = EruptionRenderer.tex("seal_prison");

    public SealPrisonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SealPrisonEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partial;
        float r = (float) (SealPrisonEntity.RADIUS * (e.isEmpowered() ? 1.25 : 1.0));
        float grow = Mth.clamp(t / 8F, 0F, 1F);
        float bright;
        if (t < SealPrisonEntity.BURST_TICK) bright = 0.55F + 0.45F * Mth.clamp((t - 20F) / 24F, 0F, 1F) + 0.1F * Mth.sin(t * 0.8F);
        else bright = Mth.clamp(1F - (t - SealPrisonEntity.BURST_TICK) / 12F, 0F, 1F);
        float scale = r * (t < SealPrisonEntity.BURST_TICK ? grow : 1F + (t - SealPrisonEntity.BURST_TICK) * 0.06F);
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(RUNE));
        pose.pushPose();
        pose.translate(0, 0.03, 0);
        pose.mulPose(Axis.YP.rotationDegrees(t * 3F));
        rune(pose, vc, scale, bright);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, 0.05 + Math.max(0, t - 30) * 0.02, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-t * 5F));
        rune(pose, vc, scale * 0.55F, bright * 0.8F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static void rune(PoseStack pose, VertexConsumer vc, float s, float b) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float c = Mth.clamp(b, 0F, 1F);
        // top face
        vert(vc, m, n, -s, s, 0, 1, c); vert(vc, m, n, s, s, 1, 1, c); vert(vc, m, n, s, -s, 1, 0, c); vert(vc, m, n, -s, -s, 0, 0, c);
        // bottom face
        vert(vc, m, n, -s, -s, 0, 0, c); vert(vc, m, n, s, -s, 1, 0, c); vert(vc, m, n, s, s, 1, 1, c); vert(vc, m, n, -s, s, 0, 1, c);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float z, float u, float v, float c) {
        vc.vertex(m, x, 0, z).color(c, c, c, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SealPrisonEntity e) { return RUNE; }
}
