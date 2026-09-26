package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.client.model.RevengeModel;
import com.krolasyon.bosses.entity.RevengeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class RevengeRenderer extends BossRenderer<RevengeEntity, RevengeModel> {
    public RevengeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RevengeModel(ctx.bakeLayer(RevengeModel.LAYER)), "revenge", 1.2F, 0.5F);
    }

    @Override
    public boolean shouldRender(RevengeEntity e, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(e, frustum, x, y, z) || !e.getDrainTargets().isEmpty();
    }

    @Override
    public void render(RevengeEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(e, yaw, partial, pose, buffers, light);
        List<Entity> targets = e.getDrainTargets();
        if (targets.isEmpty() || !e.isAlive()) return;
        Vec3 base = e.getPosition(partial);
        Vec3 chest = e.chestPos(partial).subtract(base);
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        float time = e.tickCount + partial;
        for (Entity t : targets) {
            Vec3 to = t.getPosition(partial).add(0, t.getBbHeight() * 0.5, 0).subtract(base);
            // pulsing blood tether with a slight sag
            Vec3 mid = chest.add(to).scale(0.5).add(0, -0.4 + 0.15 * Mth.sin(time * 0.5F), 0);
            float pulse = 1F + 0.3F * Mth.sin(time * 0.8F + t.getId());
            for (Vec3[] seg : new Vec3[][]{{chest, mid}, {mid, to}}) {
                BeamRenderer.beam(m, vc, seg[0], seg[1], 0.06F * pulse, 1F, 0.35F, 0.35F, 0.9F);
                BeamRenderer.beam(m, vc, seg[0], seg[1], 0.15F * pulse, 0.85F, 0.05F, 0.1F, 0.6F);
                BeamRenderer.beam(m, vc, seg[0], seg[1], 0.3F * pulse, 0.5F, 0.0F, 0.05F, 0.3F);
            }
        }
    }
}
