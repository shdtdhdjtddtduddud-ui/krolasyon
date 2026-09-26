package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.client.model.SealWardenModel;
import com.krolasyon.bosses.entity.SealWardenEntity;
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

public class SealWardenRenderer extends BossRenderer<SealWardenEntity, SealWardenModel> {
    public SealWardenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SealWardenModel(ctx.bakeLayer(SealWardenModel.LAYER)), "seal_warden", 1.1F, 0.5F);
    }

    @Override
    public boolean shouldRender(SealWardenEntity e, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(e, frustum, x, y, z) || e.getBeamTarget() != null;
    }

    @Override
    public void render(SealWardenEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(e, yaw, partial, pose, buffers, light);
        Entity target = e.getBeamTarget();
        if (target == null || !e.isAlive()) return;
        Vec3 base = e.getPosition(partial);
        Vec3 aim = target.getPosition(partial).add(0, target.getBbHeight() * 0.5, 0).subtract(base);
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        float time = e.tickCount + partial;
        float pulse = 1F + 0.25F * Mth.sin(time * 0.9F);
        Matrix4f m = pose.last().pose();
        for (Vec3 orb : e.orbPositions(partial)) {
            Vec3 from = orb.subtract(base);
            BeamRenderer.beam(m, vc, from, aim, 0.07F * pulse, 1F, 0.95F, 1F, 0.95F);
            BeamRenderer.beam(m, vc, from, aim, 0.17F * pulse, 1F, 0.45F, 0.85F, 0.55F);
            BeamRenderer.beam(m, vc, from, aim, 0.34F * pulse, 0.9F, 0.25F, 0.7F, 0.22F);
        }
        // triangle seal between the orbs while firing
        Vec3[] o = e.orbPositions(partial);
        for (int i = 0; i < 3; i++) {
            BeamRenderer.beam(m, vc, o[i].subtract(base), o[(i + 1) % 3].subtract(base), 0.09F * pulse, 1F, 0.6F, 0.9F, 0.8F);
        }
    }
}
