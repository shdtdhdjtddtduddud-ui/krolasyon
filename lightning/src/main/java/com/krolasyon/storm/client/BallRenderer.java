package com.krolasyon.storm.client;

import com.krolasyon.storm.entity.BallLightningEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Pulsing orb sprite (Higgsfield texture) with crackling arcs around it. */
public class BallRenderer extends EntityRenderer<BallLightningEntity> {
    public BallRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(BallLightningEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float grow = Mth.clamp(age / 5F, 0, 1);
        float pulse = 1 + 0.12F * Mth.sin(age * 0.9F) + 0.05F * Mth.sin(age * 2.3F);
        Vec3 c = new Vec3(0, 0.45, 0);
        // orb billboards (two layers, counter rotating)
        for (int layer = 0; layer < 2; layer++) {
            ps.pushPose();
            ps.translate(0, 0.45, 0);
            ps.mulPose(entityRenderDispatcher.cameraOrientation());
            ps.mulPose(Axis.ZP.rotationDegrees(age * (layer == 0 ? 6 : -9)));
            Matrix4f m = ps.last().pose();
            VertexConsumer vc = buf.getBuffer(FxRenderTypes.glow(FxRenderer.ORB));
            float s = (layer == 0 ? 0.95F : 0.65F) * pulse * grow;
            float a = layer == 0 ? 1F : 0.8F;
            ArcGen.t(vc, m, new Vec3(-s, -s, 0), 1, 1, 1, a, 0, 1);
            ArcGen.t(vc, m, new Vec3(s, -s, 0), 1, 1, 1, a, 1, 1);
            ArcGen.t(vc, m, new Vec3(s, s, 0), 1, 1, 1, a, 1, 0);
            ArcGen.t(vc, m, new Vec3(-s, s, 0), 1, 1, 1, a, 0, 0);
            ps.popPose();
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(e.getPosition(pt));
        VertexConsumer vc = buf.getBuffer(FxRenderTypes.ARC);
        Matrix4f m = ps.last().pose();
        RandomSource r = RandomSource.create(e.getId() * 977L + e.tickCount / 2);
        for (int i = 0; i < 5; i++) {
            Vec3 d = new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize();
            ArcGen.arc(vc, m, cam, c.add(d.scale(0.25)), c.add(d.scale(0.8 + r.nextDouble() * 0.9)), r.nextLong(), 0.035F, 0.9F * grow, 0.3F, 3, 1);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BallLightningEntity e) { return FxRenderer.ORB; }
}
