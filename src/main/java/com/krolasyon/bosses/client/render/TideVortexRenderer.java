package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.entity.TideVortexEntity;
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

/** Stacked, counter-rotating funnels of scrolling water around a bright core column. */
public class TideVortexRenderer extends EntityRenderer<TideVortexEntity> {
    static final ResourceLocation TEX = EruptionRenderer.tex("tide_vortex");
    static final ResourceLocation WAVE = EruptionRenderer.tex("tide_wave");

    public TideVortexRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(TideVortexEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.strength(partial);
        if (s <= 0.01F) return;
        float time = e.tickCount + partial;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        // funnel layers: (bottom radius, top radius, bottom y, top y, spin speed, scroll)
        float R = (float) TideVortexEntity.RADIUS;
        float[][] layers = {
                {1.2F, R * 0.95F, 0.0F, 4.6F, 9F, 0.02F},
                {0.9F, R * 0.7F, 0.2F, 3.8F, -13F, -0.03F},
                {0.6F, R * 0.45F, 0.1F, 3.0F, 17F, 0.04F},
                {0.35F, 1.1F, 0.0F, 5.5F, -24F, 0.06F}};
        for (int li = 0; li < layers.length; li++) {
            float[] L = layers[li];
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * L[4]));
            float alpha = (1.0F - li * 0.08F) * s;
            ring(pose, vc, L[0] * s, L[1] * s, L[2], L[3] * s, time * L[5], alpha, 24);
            pose.popPose();
        }
        // foam disc on the ground
        VertexConsumer wv = buffers.getBuffer(RenderType.entityTranslucentEmissive(WAVE));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-time * 6F));
        ring(pose, wv, R * 0.3F * s, R * 1.05F * s, 0.05F, 0.08F, time * 0.01F, 0.7F * s, 32);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** truncated cone strip (double sided) with UVs wrapping the strip texture */
    static void ring(PoseStack pose, VertexConsumer vc, float r0, float r1, float y0, float y1, float scroll, float alpha, int seg) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int i = 0; i < seg; i++) {
            float a0 = i * Mth.TWO_PI / seg, a1 = (i + 1) * Mth.TWO_PI / seg;
            float u0 = (float) i / seg * 2F + scroll, u1 = (float) (i + 1) / seg * 2F + scroll;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            v(vc, m, n, c0 * r0, y0, s0 * r0, u0, 1, alpha, c0, s0);
            v(vc, m, n, c1 * r0, y0, s1 * r0, u1, 1, alpha, c1, s1);
            v(vc, m, n, c1 * r1, y1, s1 * r1, u1, 0, alpha, c1, s1);
            v(vc, m, n, c0 * r1, y1, s0 * r1, u0, 0, alpha, c0, s0);
            v(vc, m, n, c0 * r1, y1, s0 * r1, u0, 0, alpha, -c0, -s0);
            v(vc, m, n, c1 * r1, y1, s1 * r1, u1, 0, alpha, -c1, -s1);
            v(vc, m, n, c1 * r0, y0, s1 * r0, u1, 1, alpha, -c1, -s1);
            v(vc, m, n, c0 * r0, y0, s0 * r0, u0, 1, alpha, -c0, -s0);
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float vv, float a, float nx, float nz) {
        vc.vertex(m, x, y, z).color(1F, 1F, 1F, a).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, nx, 0, nz).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(TideVortexEntity e) { return TEX; }
}
