package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.model.SimpleEffectModel;
import com.krolasyon.bosses.entity.FormProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Fireball billboards, flame crescents, the thrown trident model and the serpent trail of the flame dragon. */
public class FormProjectileRenderer extends EntityRenderer<FormProjectileEntity> {
    public static final ModelLayerLocation SPEAR_LAYER = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "hell_spear"), "main");
    private static final ResourceLocation FIREBALL = EruptionRenderer.tex("fireball"), CRESCENT = EruptionRenderer.tex("crescent");
    private static final ResourceLocation SPEAR = EruptionRenderer.tex("hell_spear"), SPEAR_GLOW = EruptionRenderer.tex("hell_spear_glow");
    private final SimpleEffectModel<FormProjectileEntity> spear;

    public FormProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.spear = new SimpleEffectModel<>(ctx.bakeLayer(SPEAR_LAYER));
    }

    @Override
    public void render(FormProjectileEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        int kind = e.getKind();
        float t = e.tickCount + partial;
        switch (kind) {
            case FormProjectileEntity.SPEAR, FormProjectileEntity.FALLING_SPEAR -> {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot()) + 180F));
                pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
                pose.scale(-0.7F, -0.7F, 0.7F);
                pose.translate(0, -1.0F, 0);
                spear.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SPEAR)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
                spear.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(SPEAR_GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
                pose.popPose();
            }
            case FormProjectileEntity.CRESCENT -> {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yRotO, e.getYRot())));
                float s = 1.6F + Math.min(t, 10F) * 0.06F;
                float fade = Mth.clamp((22F - t) / 6F, 0F, 1F);
                VertexConsumer vc = buffers.getBuffer(RenderType.eyes(CRESCENT));
                Matrix4f m = pose.last().pose();
                Matrix3f n = pose.last().normal();
                // flat crescent lying in the horizontal plane, opening backwards
                v(vc, m, n, -s * 1.5F, 0, -s * 0.2F, 0, 1, fade); v(vc, m, n, s * 1.5F, 0, -s * 0.2F, 1, 1, fade);
                v(vc, m, n, s * 1.5F, 0, s * 1.2F, 1, 0, fade); v(vc, m, n, -s * 1.5F, 0, s * 1.2F, 0, 0, fade);
                v(vc, m, n, -s * 1.5F, 0, s * 1.2F, 0, 0, fade); v(vc, m, n, s * 1.5F, 0, s * 1.2F, 1, 0, fade);
                v(vc, m, n, s * 1.5F, 0, -s * 0.2F, 1, 1, fade); v(vc, m, n, -s * 1.5F, 0, -s * 0.2F, 0, 1, fade);
                pose.popPose();
            }
            default -> {
                float size = switch (kind) { case FormProjectileEntity.METEOR -> 2.6F; case FormProjectileEntity.DRAGON -> 1.6F; case FormProjectileEntity.EMBER -> 0.55F; default -> 0.9F; };
                billboard(pose, buffers, size * (1F + 0.08F * Mth.sin(t * 1.3F)), t * 12F);
                billboard(pose, buffers, size * 0.7F, -t * 18F);
                if (kind == FormProjectileEntity.DRAGON && !e.trail.isEmpty()) {
                    Vec3 base = e.getPosition(partial);
                    VertexConsumer lc = buffers.getBuffer(RenderType.lightning());
                    Matrix4f m = pose.last().pose();
                    Vec3 prev = Vec3.ZERO;
                    int i = 0;
                    for (Vec3 q : e.trail) {
                        Vec3 cur = q.subtract(base);
                        float k = 1F - i / (float) e.trail.size();
                        BeamRenderer.beam(m, lc, prev, cur, 0.55F * k + 0.1F, 1F, 0.45F, 0.08F, 0.5F * k);
                        BeamRenderer.beam(m, lc, prev, cur, 0.25F * k + 0.05F, 1F, 0.85F, 0.4F, 0.7F * k);
                        prev = cur;
                        i++;
                    }
                }
            }
        }
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private void billboard(PoseStack pose, MultiBufferSource buffers, float s, float spin) {
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(FIREBALL));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float h = s / 2F;
        q(vc, m, n, -h, -h, 0, 1); q(vc, m, n, h, -h, 1, 1); q(vc, m, n, h, h, 1, 0); q(vc, m, n, -h, h, 0, 0);
        pose.popPose();
    }

    private static void q(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v) {
        vc.vertex(m, x, y, 0).color(1F, 1F, 1F, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float vv, float c) {
        vc.vertex(m, x, y, z).color(c, c, c, 1F).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FormProjectileEntity e) { return FIREBALL; }
}
