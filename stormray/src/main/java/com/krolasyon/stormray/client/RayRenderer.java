package com.krolasyon.stormray.client;

import com.krolasyon.stormray.entity.StormRayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

public final class RayRenderer extends EntityRenderer<StormRayEntity> {
    public RayRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.3F;
        shadowStrength = 0.5F;
    }

    @Override
    public void render(StormRayEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        float yRot = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        float time = e.tickCount + pt;
        ps.translate(0, 0.4 + Math.sin(time * 0.06) * 0.05, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-yRot));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, e.pitchO, e.pitch)));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.rollO, e.roll)));
        float sc = 1.25F;
        if (e.hurtTime > 0) sc *= 1 + 0.03F * Mth.sin((e.hurtTime - pt) * 1.5F);
        ps.scale(sc, sc, sc);
        float flap = Mth.lerp(pt, e.flapPhaseO, e.flapPhase);
        RayModel.render(ps, buf, light, time, flap, e.isSaddled(), Math.min(1, e.getZapTicks() / 12F));
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(StormRayEntity e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
