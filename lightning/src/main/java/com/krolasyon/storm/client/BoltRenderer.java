package com.krolasyon.storm.client;

import com.krolasyon.storm.entity.BoltProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Draws the travelling bolt as a jagged streak trailing behind the projectile. */
public class BoltRenderer extends EntityRenderer<BoltProjectile> {
    public BoltRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(BoltProjectile e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Vec3 vel = e.getDeltaMovement();
        if (vel.lengthSqr() < 1e-4) return;
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(e.getPosition(pt));
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(FxRenderTypes.ARC);
        Vec3 tail = vel.normalize().scale(-Math.min(4.0, 1.0 + e.tickCount * 0.8));
        long seed = e.getId() * 31L + e.tickCount;
        ArcGen.arc(vc, m, cam, Vec3.ZERO, tail, seed, 0.09F, 1F, 0.12F, 4, 1);
        ArcGen.arc(vc, m, cam, Vec3.ZERO, tail.scale(0.7), seed * 7, 0.04F, 0.8F, 0.25F, 3, 0);
        Vec3 right = cam.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(cam).normalize();
        ArcGen.glowQuad(vc, m, Vec3.ZERO, right, up, 0.35F, ArcGen.MID, 0.6F);
        ArcGen.glowQuad(vc, m, Vec3.ZERO, right, up, 0.15F, ArcGen.CORE, 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(BoltProjectile e) { return FxRenderer.ORB; }
}
