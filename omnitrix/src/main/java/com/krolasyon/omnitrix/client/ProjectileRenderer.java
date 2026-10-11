package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.entity.AlienProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/** Fireballs, crystal shards, acid blobs, sonic discs and thrown boulders. */
public class ProjectileRenderer extends EntityRenderer<AlienProjectile> {
    public ProjectileRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(AlienProjectile e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        Vec3 vel = e.getDeltaMovement();
        Vec3 dir = vel.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : vel.normalize();
        Matrix4f m = ps.last().pose();
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(e.getPosition(pt));
        switch (e.kind()) {
            case FIREBALL -> {
                ps.pushPose();
                ps.mulPose(entityRenderDispatcher.cameraOrientation());
                Matrix4f bm = ps.last().pose();
                VertexConsumer vc = buf.getBuffer(OmniRenderTypes.sprite(((int) age / 2) % 2 == 0 ? FxRenderer.FIRE_TEX : FxRenderer.FIRE_TEX2));
                for (int i = 0; i < 3; i++) Geo.sprite(vc, bm, new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(0, 1, 0), 0.5F - i * 0.1F, age * 0.3F + i * 2, 1, 1, 1, 1);
                ps.popPose();
                Geo.ribbon(buf.getBuffer(OmniRenderTypes.GLOW), m, cam, List.of(Vec3.ZERO, dir.scale(-1.6)), 0.5F, 0xFF8A20, 0.7F, true);
            }
            case CRYSTAL -> {
                VertexConsumer vc = buf.getBuffer(OmniRenderTypes.SOFT);
                Geo.crystal(vc, m, dir.scale(-0.45), dir.scale(0.45), 0.12, 0x9FF5C8, 0.95F, age * 0.3);
            }
            case ACID, ACID_BOMB -> {
                VertexConsumer vc = buf.getBuffer(OmniRenderTypes.SOFT);
                double s = e.kind() == AlienProjectile.Kind.ACID_BOMB ? 0.35 : 0.2;
                float wob = Mth.sin(age * 0.8F) * 0.15F;
                Geo.sphere(vc, m, Vec3.ZERO, s * (1 + wob), s * (1 - wob), s * (1 + wob), 0x6ACC22, 0.95F, 5, 8);
                Geo.sphere(vc, m, new Vec3(s * 0.3, s * 0.35, 0), s * 0.3, s * 0.25, s * 0.3, 0xC8FF80, 0.9F, 3, 6);
            }
            case SONIC_DISC -> {
                ps.pushPose();
                if (e.stuckTo() < 0) ps.mulPose(Axis.YP.rotationDegrees(age * 25));
                Matrix4f dm = ps.last().pose();
                VertexConsumer vc = buf.getBuffer(OmniRenderTypes.SOFT);
                Geo.sphere(vc, dm, Vec3.ZERO, 0.28, 0.07, 0.28, 0x1A1E26, 1F, 4, 14);
                Geo.sphere(vc, dm, new Vec3(0, 0.03, 0), 0.14, 0.05, 0.14, 0x18FFF0, 1F, 3, 12);
                ps.popPose();
                if (e.stuckTo() >= 0) {
                    float p = (age % 10) / 10F;
                    Geo.ribbon(buf.getBuffer(OmniRenderTypes.GLOW), m, cam, Geo.flatCircle(Vec3.ZERO, 0.3 + p * 1.2, 24, 0), 0.06F, 0x18FFF0, 1 - p, false);
                }
            }
            case BOULDER -> {
                ps.pushPose();
                ps.mulPose(Axis.XP.rotationDegrees(age * 12));
                ps.mulPose(Axis.ZP.rotationDegrees(age * 7));
                ps.scale(1.2F, 1.2F, 1.2F);
                ps.translate(-0.5, -0.5, -0.5);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(e.block(), ps, buf, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AlienProjectile e) { return FxRenderer.FIRE_TEX; }
}
