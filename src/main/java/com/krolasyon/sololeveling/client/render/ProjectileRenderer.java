package com.krolasyon.sololeveling.client.render;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.entity.SLProjectile;
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

/** Glowing sprites for spells; thrown daggers and sword waves are drawn as crossed blades along their flight. */
public class ProjectileRenderer extends EntityRenderer<SLProjectile> {
    private static final ResourceLocation TEX = SoloLeveling.id("textures/entity/projectiles.png");

    public ProjectileRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(SLProjectile e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        SLProjectile.Kind k = e.kind();
        int frame = k.ordinal();
        float u0 = frame / 8F, u1 = (frame + 1) / 8F;
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        pose.pushPose();
        float s = k.size;
        boolean blade = k == SLProjectile.Kind.SHADOW_DAGGER || k == SLProjectile.Kind.FLAME_SLASH || k == SLProjectile.Kind.ICE_SHARD;
        if (blade) {
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.getYRot()) - 90F));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
            pose.scale(s, s, s);
            for (int i = 0; i < 2; i++) {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                quad(pose, vc, u0, u1);
            }
        } else {
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.mulPose(Axis.ZP.rotationDegrees((e.tickCount + partial) * 20));
            pose.scale(s, s, s);
            quad(pose, vc, u0, u1);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float u0, float u1) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int l = LightTexture.FULL_BRIGHT;
        vc.vertex(m, -0.5F, -0.5F, 0).color(1F, 1F, 1F, 1F).uv(u0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(l).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 0.5F, -0.5F, 0).color(1F, 1F, 1F, 1F).uv(u1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(l).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 0.5F, 0.5F, 0).color(1F, 1F, 1F, 1F).uv(u1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(l).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, -0.5F, 0.5F, 0).color(1F, 1F, 1F, 1F).uv(u0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(l).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SLProjectile e) { return TEX; }
}
