package com.sololeveling.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.MagicBoltEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MagicBoltRenderer extends EntityRenderer<MagicBoltEntity> {
    private static final ResourceLocation ORB = new ResourceLocation(SoloLeveling.MODID, "textures/entity/glow_orb.png");
    private static final ResourceLocation SLASH = new ResourceLocation(SoloLeveling.MODID, "textures/entity/slash.png");

    public MagicBoltRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public ResourceLocation getTextureLocation(MagicBoltEntity e) { return ORB; }

    static int[] color(String kind) {
        return switch (kind) {
            case "ice" -> new int[]{120, 210, 255};
            case "fire" -> new int[]{255, 140, 40};
            case "poison" -> new int[]{140, 255, 60};
            case "beam" -> new int[]{255, 70, 90};
            default -> new int[]{170, 90, 255};
        };
    }

    @Override
    public void render(MagicBoltEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        int[] c = color(e.kind());
        boolean beam = e.kind().equals("beam");
        ps.pushPose();
        ps.translate(0, 0.2, 0);
        if (beam) {
            ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot() + 90F));
            ps.mulPose(Axis.ZP.rotationDegrees(e.getXRot()));
            quad(ps, buf.getBuffer(RenderType.eyes(SLASH)), 1.3F, c, 1F, true);
            ps.mulPose(Axis.XP.rotationDegrees(90F));
            quad(ps, buf.getBuffer(RenderType.eyes(SLASH)), 1.0F, c, 0.8F, true);
        } else {
            ps.mulPose(entityRenderDispatcher.cameraOrientation());
            float s = 0.45F + 0.08F * (float) Math.sin((e.tickCount + pt) * 0.8F);
            quad(ps, buf.getBuffer(RenderType.eyes(ORB)), s, c, 1F, false);
            quad(ps, buf.getBuffer(RenderType.eyes(ORB)), s * 0.5F, new int[]{255, 255, 255}, 1F, false);
        }
        ps.popPose();
    }

    private static void quad(PoseStack ps, VertexConsumer vc, float s, int[] c, float a, boolean flat) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int r = c[0], g = c[1], b = c[2], al = (int) (a * 255);
        float x0 = flat ? -s : -s, x1 = flat ? s : s;
        v(vc, m, n, x0, -s, 0, 0, 1, r, g, b, al);
        v(vc, m, n, x1, -s, 0, 1, 1, r, g, b, al);
        v(vc, m, n, x1, s, 0, 1, 0, r, g, b, al);
        v(vc, m, n, x0, s, 0, 0, 0, r, g, b, al);
        v(vc, m, n, x0, s, 0, 0, 0, r, g, b, al);
        v(vc, m, n, x1, s, 0, 1, 0, r, g, b, al);
        v(vc, m, n, x1, -s, 0, 1, 1, r, g, b, al);
        v(vc, m, n, x0, -s, 0, 0, 1, r, g, b, al);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(n, 0, 1, 0).endVertex();
    }
}
