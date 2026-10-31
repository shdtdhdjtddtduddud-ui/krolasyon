package com.krolasyon.cille.client.render;

import com.krolasyon.cille.entity.VortexEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** spinning funnel of water: a wide whirlpool or a tall waterspout */
public class VortexRenderer extends EntityRenderer<VortexEntity> {
    private static final ResourceLocation WATER = CilleRenderer.tex("water_swirl");

    public VortexRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(VortexEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.size(partial);
        if (s <= 0.01F) return;
        float age = e.tickCount + partial;
        boolean spout = e.getKind() == VortexEntity.KIND_SPOUT;
        float R = e.radius() * s, H = e.height() * s;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WATER));
        funnel(pose, vc, R, H, spout, age * (spout ? 0.035F : 0.022F), age * 0.02F, 0.85F, light);
        funnel(pose, vc, R * 0.7F, H * 0.95F, spout, age * (spout ? 0.06F : 0.04F), age * 0.035F + 0.5F, 0.6F, light);
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static float radiusAt(float f, float R, boolean spout) {
        return spout ? 0.7F + (R - 0.7F) * (float) Math.pow(f, 1.6) + 0.25F * Mth.sin(f * 9F) : R * (0.18F + 0.82F * (float) Math.pow(f, 0.55));
    }

    private static void funnel(PoseStack pose, VertexConsumer vc, float R, float H, boolean spout, float uScroll, float vScroll, float alpha, int light) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int rings = spout ? 16 : 6, slices = 24;
        for (int i = 0; i < rings; i++) {
            float f0 = i / (float) rings, f1 = (i + 1) / (float) rings;
            float r0 = radiusAt(f0, R, spout), r1 = radiusAt(f1, R, spout);
            float y0 = f0 * H, y1 = f1 * H;
            float twist0 = f0 * 1.2F, twist1 = f1 * 1.2F;
            for (int j = 0; j < slices; j++) {
                float a0 = j / (float) slices * Mth.TWO_PI, a1 = (j + 1) / (float) slices * Mth.TWO_PI;
                float u0 = j / (float) slices * 3F + uScroll, u1 = (j + 1) / (float) slices * 3F + uScroll;
                float v0 = f0 * (spout ? 3F : 1F) - vScroll, v1 = f1 * (spout ? 3F : 1F) - vScroll;
                v(m, n, vc, r0, a0 + twist0, y0, u0, v0, alpha, light);
                v(m, n, vc, r1, a0 + twist1, y1, u0, v1, alpha, light);
                v(m, n, vc, r1, a1 + twist1, y1, u1, v1, alpha, light);
                v(m, n, vc, r0, a1 + twist0, y0, u1, v0, alpha, light);
            }
        }
    }

    private static void v(Matrix4f m, Matrix3f n, VertexConsumer vc, float r, float a, float y, float u, float vv, float alpha, int light) {
        float c = Mth.cos(a), s = Mth.sin(a);
        vc.vertex(m, c * r, y, s * r).color(1F, 1F, 1F, alpha).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, c, 0F, s).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(VortexEntity e) { return WATER; }
}
