package com.krolasyon.bosses.realm.client.render;

import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.entity.SpellProjectile;
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

/** glowing camera-facing orb in the colour of the spell's element */
public class SpellProjectileRenderer extends EntityRenderer<SpellProjectile> {
    private static final ResourceLocation TEX = Realm.rl("textures/entity/realm/spell_orb.png");

    public SpellProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected int getBlockLightLevel(SpellProjectile e, net.minecraft.core.BlockPos pos) { return 15; }

    @Override
    public void render(SpellProjectile e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        int c = e.element().light;
        int c2 = e.element().color;
        float s = e.isBig() ? 1.3F : 0.7F;
        float pulse = 1F + 0.12F * (float) Math.sin((e.tickCount + partial) * 0.8F);
        pose.pushPose();
        pose.translate(0, 0.25, 0);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(TEX));
        quad(pose, vc, s * pulse * 1.6F, c2, 0.75F);
        pose.mulPose(Axis.ZP.rotationDegrees((e.tickCount + partial) * 12F));
        quad(pose, vc, s * pulse, c, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float s, int rgb, float a) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255, al = (int) (a * 255);
        vertex(vc, m, n, -s, -s, 0, 1, r, g, b, al);
        vertex(vc, m, n, s, -s, 1, 1, r, g, b, al);
        vertex(vc, m, n, s, s, 1, 0, r, g, b, al);
        vertex(vc, m, n, -s, s, 0, 0, r, g, b, al);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int r, int g, int b, int a) {
        vc.vertex(m, x * 0.5F, y * 0.5F, 0).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectile e) { return TEX; }
}
