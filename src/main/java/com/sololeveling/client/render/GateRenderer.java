package com.sololeveling.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.GateEntity;
import com.sololeveling.util.Ranks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders the swirling gate portal: runic ring, two counter-rotating vortices and a glowing core. */
public class GateRenderer extends EntityRenderer<GateEntity> {
    private static final ResourceLocation SWIRL = new ResourceLocation(SoloLeveling.MODID, "textures/entity/gate_swirl.png");
    private static final ResourceLocation SWIRL2 = new ResourceLocation(SoloLeveling.MODID, "textures/entity/gate_swirl2.png");
    private static final ResourceLocation RING = new ResourceLocation(SoloLeveling.MODID, "textures/entity/gate_ring.png");
    private static final ResourceLocation ORB = new ResourceLocation(SoloLeveling.MODID, "textures/entity/glow_orb.png");

    public GateRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(GateEntity e) { return SWIRL; }

    @Override
    public boolean shouldRender(GateEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return true; }

    @Override
    public void render(GateEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float time = e.tickCount + pt;
        int rgb;
        if (e.mode() == GateEntity.EXIT) rgb = 0x66FFAA;
        else if (e.isRed() || e.isBroken()) rgb = 0xFF3030;
        else rgb = Ranks.color(e.rank());
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        float pulse = 0.85F + 0.15F * (float) Math.sin(time * 0.1F);
        ps.pushPose();
        ps.translate(0, 2.4, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        float w = 1.8F, h = 2.35F;
        // inner void
        ps.pushPose();
        ps.scale(1F, 1F, 1F);
        quad(ps, buf.getBuffer(RenderType.eyes(ORB)), w * 0.95F, h * 0.95F, r / 4, g / 4, b / 4, 255);
        ps.popPose();
        // vortices
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(time * 3.2F));
        quadSquareScaled(ps, buf.getBuffer(RenderType.eyes(SWIRL)), 2.2F, w / 2.2F * 1.0F, h / 2.2F, r, g, b, (int) (255 * pulse));
        ps.popPose();
        ps.pushPose();
        ps.translate(0, 0, 0.02);
        ps.mulPose(Axis.ZP.rotationDegrees(-time * 5.0F));
        quadSquareScaled(ps, buf.getBuffer(RenderType.eyes(SWIRL2)), 1.8F, w / 1.8F * 0.95F, h / 1.8F * 0.95F, Math.min(255, r + 60), Math.min(255, g + 60), Math.min(255, b + 60), 200);
        ps.popPose();
        // ring
        ps.pushPose();
        ps.translate(0, 0, 0.04);
        quadSquareScaled(ps, buf.getBuffer(RenderType.eyes(RING)), 2.3F, w / 2.3F * 1.12F, h / 2.3F * 1.1F, r, g, b, 255);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(time * 0.8F));
        ps.translate(0, 0, 0.06);
        quadSquareScaled(ps, buf.getBuffer(RenderType.eyes(RING)), 2.3F, w / 2.3F * 0.8F, h / 2.3F * 0.78F, 255, 255, 255, 120);
        ps.popPose();
        // core glow
        quad(ps, buf.getBuffer(RenderType.eyes(ORB)), 0.9F * pulse, 1.3F * pulse, 255, 255, 255, 200);
        ps.popPose();
        // name
        if (e.hasCustomName() && this.entityRenderDispatcher.distanceToSqr(e) < 40 * 40)
            this.renderNameTag(e, e.getDisplayName(), ps, buf, light);
    }

    private static void quadSquareScaled(PoseStack ps, VertexConsumer vc, float half, float sx, float sy, int r, int g, int b, int a) {
        quad(ps, vc, half * sx, half * sy, r, g, b, a);
    }

    private static void quad(PoseStack ps, VertexConsumer vc, float hx, float hy, int r, int g, int b, int a) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        vert(vc, m, n, -hx, -hy, 0, 0, 1, r, g, b, a);
        vert(vc, m, n, hx, -hy, 0, 1, 1, r, g, b, a);
        vert(vc, m, n, hx, hy, 0, 1, 0, r, g, b, a);
        vert(vc, m, n, -hx, hy, 0, 0, 0, r, g, b, a);
        vert(vc, m, n, -hx, hy, 0, 0, 0, r, g, b, a);
        vert(vc, m, n, hx, hy, 0, 1, 0, r, g, b, a);
        vert(vc, m, n, hx, -hy, 0, 1, 1, r, g, b, a);
        vert(vc, m, n, -hx, -hy, 0, 0, 1, r, g, b, a);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        vc.vertex(m, x, y, z).color(r * a / 255, g * a / 255, b * a / 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(n, 0, 1, 0).endVertex();
    }
}
