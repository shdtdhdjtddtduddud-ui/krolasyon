package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.entity.ZoneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Spinning glowing ground runes of the zone abilities, plus a rising light column on the sun seal. */
public class ZoneRenderer extends EntityRenderer<ZoneEntity> {
    private static final ResourceLocation SUN = EruptionRenderer.tex("sun_seal"), EMBER = EruptionRenderer.tex("ember_circle"), GATE = EruptionRenderer.tex("hell_gate");

    public ZoneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(ZoneEntity e, Frustum f, double x, double y, double z) { return true; }

    @Override
    public void render(ZoneEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partial;
        int kind = e.getKind();
        float life = e.life();
        float grow = Mth.clamp(t / 8F, 0F, 1F);
        float fade = Mth.clamp((life - t) / 8F, 0F, 1F);
        float bright = (0.7F + 0.3F * Mth.sin(t * 0.6F)) * fade;
        float r = e.getRadius() * grow;
        if (kind == ZoneEntity.SUN_SEAL) {
            float k = Mth.clamp(t / ZoneEntity.SEAL_BURST, 0F, 1F);
            bright = t < ZoneEntity.SEAL_BURST ? 0.5F + 0.5F * k + 0.15F * Mth.sin(t * (0.5F + k)) : Mth.clamp(1F - (t - ZoneEntity.SEAL_BURST) / 10F, 0F, 1F);
            if (t >= ZoneEntity.SEAL_BURST) r *= 1F + (t - ZoneEntity.SEAL_BURST) * 0.08F;
        }
        ResourceLocation tex = switch (kind) { case ZoneEntity.SUN_SEAL -> SUN; case ZoneEntity.EMBER_RAIN -> EMBER; default -> GATE; };
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(tex));
        pose.pushPose();
        pose.translate(0, 0.04, 0);
        pose.mulPose(Axis.YP.rotationDegrees(t * (kind == ZoneEntity.HELL_GATE ? -2F : 3F)));
        quad(pose, vc, r, bright);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, 0.07 + (kind == ZoneEntity.EMBER_RAIN ? 0 : Math.max(0, t - 10) * 0.01), 0);
        pose.mulPose(Axis.YP.rotationDegrees(-t * 5F));
        quad(pose, vc, r * 0.55F, bright * 0.8F);
        pose.popPose();
        if (kind != ZoneEntity.SUN_SEAL || t < ZoneEntity.SEAL_BURST + 6) {
            // a column of light / fire above the rune
            float h = kind == ZoneEntity.SUN_SEAL ? 2F + t * 0.25F : 8F;
            float cr = kind == ZoneEntity.SUN_SEAL ? 0.4F + t * 0.02F : 0.25F;
            VertexConsumer lc = buffers.getBuffer(RenderType.lightning());
            Matrix4f m = pose.last().pose();
            float g = kind == ZoneEntity.HELL_GATE ? 0.2F : 0.6F;
            BeamRenderer.beam(m, lc, net.minecraft.world.phys.Vec3.ZERO, new net.minecraft.world.phys.Vec3(0, h, 0), cr * fade, 1F, g, 0.1F, 0.35F * bright);
            BeamRenderer.beam(m, lc, net.minecraft.world.phys.Vec3.ZERO, new net.minecraft.world.phys.Vec3(0, h * 0.8, 0), cr * 0.4F * fade, 1F, 0.95F, 0.7F, 0.6F * bright);
        }
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float s, float b) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float c = Mth.clamp(b, 0F, 1F);
        v(vc, m, n, -s, s, 0, 1, c); v(vc, m, n, s, s, 1, 1, c); v(vc, m, n, s, -s, 1, 0, c); v(vc, m, n, -s, -s, 0, 0, c);
        v(vc, m, n, -s, -s, 0, 0, c); v(vc, m, n, s, -s, 1, 0, c); v(vc, m, n, s, s, 1, 1, c); v(vc, m, n, -s, s, 0, 1, c);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float z, float u, float vv, float c) {
        vc.vertex(m, x, 0, z).color(c, c, c, 1F).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ZoneEntity e) { return SUN; }
}
