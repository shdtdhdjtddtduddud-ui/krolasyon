package com.krolasyon.voidtree.client;

import com.krolasyon.voidtree.entity.RiftOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/** Spinning rift tear (Higgsfield texture) trailing a shadow streak. */
public class RiftOrbRenderer extends EntityRenderer<RiftOrbEntity> {
    public RiftOrbRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(RiftOrbEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float grow = Mth.clamp(age / 4F, 0, 1);
        ps.pushPose();
        ps.mulPose(entityRenderDispatcher.cameraOrientation());
        ps.mulPose(Axis.ZP.rotationDegrees(age * 25));
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(FxRenderTypes.glow(FxRenderer.RIFT));
        float s = 0.55F * grow * (1 + 0.1F * Mth.sin(age));
        ArcGen.t(vc, m, new Vec3(-s, -s, 0), 1, 1, 1, 1, 0, 1);
        ArcGen.t(vc, m, new Vec3(s, -s, 0), 1, 1, 1, 1, 1, 1);
        ArcGen.t(vc, m, new Vec3(s, s, 0), 1, 1, 1, 1, 1, 0);
        ArcGen.t(vc, m, new Vec3(-s, s, 0), 1, 1, 1, 1, 0, 0);
        VertexConsumer st = buf.getBuffer(FxRenderTypes.glow(FxRenderer.STAR));
        float s2 = 0.3F * grow;
        ArcGen.t(st, m, new Vec3(-s2, -s2, 0), 1, 1, 1, 1, 0, 1);
        ArcGen.t(st, m, new Vec3(s2, -s2, 0), 1, 1, 1, 1, 1, 1);
        ArcGen.t(st, m, new Vec3(s2, s2, 0), 1, 1, 1, 1, 1, 0);
        ArcGen.t(st, m, new Vec3(-s2, s2, 0), 1, 1, 1, 1, 0, 0);
        ps.popPose();
        Vec3 vel = e.getDeltaMovement();
        if (vel.lengthSqr() > 1e-4) {
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(e.getPosition(pt));
            Vec3 tail = vel.normalize().scale(-Math.min(3.0, 0.5 + age * 0.4));
            List<Vec3> pts = ArcGen.wave(Vec3.ZERO, tail, 0.15, 1.5, age * 0.6, 10);
            ArcGen.taper(buf.getBuffer(FxRenderTypes.ARC), ps.last().pose(), cam, pts, 0.25F, ArcGen.MID, 0.8F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(RiftOrbEntity e) { return FxRenderer.RIFT; }
}
