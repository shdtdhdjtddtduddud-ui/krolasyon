package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.rpg.entity.MagicBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Camera facing glowing orb with a bright core and a slowly spinning coloured halo. */
public class MagicBoltRenderer extends EntityRenderer<MagicBolt> {
    private static final ResourceLocation ORB = new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/rpg/orb.png");
    private static final ResourceLocation STAR = new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/rpg/orb_star.png");

    public MagicBoltRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MagicBolt e, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        float s = e.size();
        float age = e.tickCount + partial;
        ps.pushPose();
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        ps.mulPose(Axis.ZP.rotationDegrees(age * 12));
        quad(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(STAR)), s * 2.4F, e.color2(), 0.55F);
        ps.mulPose(Axis.ZP.rotationDegrees(-age * 20));
        quad(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(ORB)), s * 1.6F * (1 + 0.1F * (float) Math.sin(age * 0.8F)), e.color(), 0.95F);
        quad(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(ORB)), s * 0.7F, 0xFFFFFF, 1.0F);
        ps.popPose();
        super.render(e, yaw, partial, ps, buffers, light);
    }

    private static void quad(PoseStack ps, VertexConsumer vc, float size, int color, float alpha) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255, a = (int) (alpha * 255);
        float h = size * 0.5F;
        vertex(vc, m, n, -h, -h, 0, 1, r, g, b, a);
        vertex(vc, m, n, h, -h, 1, 1, r, g, b, a);
        vertex(vc, m, n, h, h, 1, 0, r, g, b, a);
        vertex(vc, m, n, -h, h, 0, 0, r, g, b, a);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int r, int g, int b, int a) {
        vc.vertex(m, x, y, 0).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(MagicBolt e) { return ORB; }
}
