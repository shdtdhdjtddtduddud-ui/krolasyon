package com.krolasyon.sololeveling.client.render;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.world.GateEntity;
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

/** The gate: layered, counter-rotating swirls of light in the gate's rank colour. */
public class GateRenderer extends EntityRenderer<GateEntity> {
    private static final ResourceLocation SWIRL = SoloLeveling.id("textures/entity/gate_swirl.png");
    private static final ResourceLocation RING = SoloLeveling.id("textures/entity/gate_ring.png");
    private static final ResourceLocation CORE = SoloLeveling.id("textures/entity/gate_core.png");

    public GateRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(GateEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        float t = e.tickCount + partial;
        int c = e.isRed() ? 0xFF2236 : e.rank().color;
        float r = ((c >> 16) & 255) / 255F, g = ((c >> 8) & 255) / 255F, b = (c & 255) / 255F;
        float open = Mth.clamp(t / 30F, 0, 1);
        if (e.isClosing()) open *= 0.3F + 0.7F * Mth.clamp(1 - (t % 60) / 60F, 0, 1);
        float size = (2.2F + e.rank().ordinal() * 0.35F) * open;
        pose.pushPose();
        pose.translate(0, e.getBbHeight() * 0.5F + 0.2F, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        pose.scale(size, size * 1.35F, size);
        disc(pose, buf.getBuffer(RenderType.entityTranslucentEmissive(RING)), t * 0.6F, 1.25F, r, g, b, 0.55F);
        disc(pose, buf.getBuffer(RenderType.entityTranslucentEmissive(SWIRL)), t * 2.2F, 1.0F, r, g, b, 0.95F);
        disc(pose, buf.getBuffer(RenderType.entityTranslucentEmissive(SWIRL)), -t * 3.1F, 0.78F, Math.min(1, r + 0.3F), Math.min(1, g + 0.3F), Math.min(1, b + 0.3F), 0.8F);
        disc(pose, buf.getBuffer(RenderType.entityTranslucentEmissive(CORE)), t * 0.5F, 0.55F + 0.04F * Mth.sin(t * 0.2F), 1, 1, 1, 0.9F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }

    private static void disc(PoseStack pose, VertexConsumer vc, float rot, float s, float r, float g, float b, float a) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(rot));
        pose.scale(s, s, s);
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int l = LightTexture.FULL_BRIGHT;
        v(vc, m, n, -1, -1, 0, 1, r, g, b, a, l);
        v(vc, m, n, 1, -1, 1, 1, r, g, b, a, l);
        v(vc, m, n, 1, 1, 1, 0, r, g, b, a, l);
        v(vc, m, n, -1, 1, 0, 0, r, g, b, a, l);
        pose.popPose();
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float w, float r, float g, float b, float a, int l) {
        vc.vertex(m, x, y, 0).color(r, g, b, a).uv(u, w).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(l).normal(n, 0, 0, 1).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(GateEntity e) { return SWIRL; }
}
