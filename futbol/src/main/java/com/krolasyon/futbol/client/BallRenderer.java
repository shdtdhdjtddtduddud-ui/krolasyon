package com.krolasyon.futbol.client;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.entity.FootballEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Smooth UV-sphere football with an equirectangular texture and an energy aura for super shots. */
public class BallRenderer extends EntityRenderer<FootballEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(FutbolMod.MODID, "textures/entity/football.png");
    private static final ResourceLocation AURA = new ResourceLocation(FutbolMod.MODID, "textures/entity/ball_aura.png");
    private static final int LAT = 14, LON = 24;
    private static final float[][] PTS = new float[(LAT + 1) * (LON + 1)][];

    static {
        for (int i = 0; i <= LAT; i++) {
            double th = Math.PI * i / LAT;
            for (int j = 0; j <= LON; j++) {
                double ph = 2 * Math.PI * j / LON;
                PTS[i * (LON + 1) + j] = new float[]{(float) (Math.sin(th) * Math.cos(ph)), (float) Math.cos(th), (float) (Math.sin(th) * Math.sin(ph)),
                        (float) j / LON, (float) i / LAT};
            }
        }
    }

    public BallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.24F;
        this.shadowStrength = 0.9F;
    }

    @Override
    public void render(FootballEntity ball, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.translate(0, FootballEntity.R, 0);
        Quaternionf q = new Quaternionf(ball.prevRot).slerp(ball.rot, partial);
        ps.mulPose(q);
        sphere(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, (float) FootballEntity.R, 255, 255, 255, 255);
        int c = auraColor(ball.getTrail());
        if (c != 0) {
            float time = ball.tickCount + partial;
            ps.mulPose(Axis.YP.rotationDegrees(time * 25F));
            ps.mulPose(Axis.XP.rotationDegrees(time * 13F));
            float pulse = 1.32F + Mth.sin(time * 0.8F) * 0.06F;
            sphere(ps, buf.getBuffer(RenderType.entityTranslucentEmissive(AURA)), 0xF000F0, (float) FootballEntity.R * pulse,
                    (c >> 16) & 255, (c >> 8) & 255, c & 255, 170);
            ps.mulPose(Axis.ZP.rotationDegrees(time * 31F));
            sphere(ps, buf.getBuffer(RenderType.entityTranslucentEmissive(AURA)), 0xF000F0, (float) FootballEntity.R * (pulse + 0.22F),
                    (c >> 16) & 255, (c >> 8) & 255, c & 255, 90);
        }
        ps.popPose();
        super.render(ball, yaw, partial, ps, buf, light);
    }

    private static int auraColor(int trail) {
        return switch (trail) {
            case FootballEntity.TR_FIRE -> 0xFF6A1A;
            case FootballEntity.TR_LIGHTNING -> 0x7FE8FF;
            case FootballEntity.TR_TORNADO -> 0xE8F4FF;
            case FootballEntity.TR_EAGLE -> 0xFFD54F;
            case FootballEntity.TR_ICE -> 0xA8E6FF;
            default -> 0;
        };
    }

    private static void sphere(PoseStack ps, VertexConsumer vc, int light, float r, int cr, int cg, int cb, int ca) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        for (int i = 0; i < LAT; i++) {
            for (int j = 0; j < LON; j++) {
                float[] a = PTS[i * (LON + 1) + j];
                float[] b = PTS[(i + 1) * (LON + 1) + j];
                float[] c = PTS[(i + 1) * (LON + 1) + j + 1];
                float[] d = PTS[i * (LON + 1) + j + 1];
                v(vc, m, n, a, r, light, cr, cg, cb, ca);
                v(vc, m, n, b, r, light, cr, cg, cb, ca);
                v(vc, m, n, c, r, light, cr, cg, cb, ca);
                v(vc, m, n, d, r, light, cr, cg, cb, ca);
            }
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] p, float r, int light, int cr, int cg, int cb, int ca) {
        vc.vertex(m, p[0] * r, p[1] * r, p[2] * r).color(cr, cg, cb, ca).uv(p[3], p[4]).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, p[0], p[1], p[2]).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FootballEntity e) { return TEX; }
}
