package com.krolasyon.sylvanarchery.client;

import com.krolasyon.sylvanarchery.entity.SylvanArrowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

public final class SylvanArrowRenderer extends EntityRenderer<SylvanArrowEntity> {
    public SylvanArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SylvanArrowEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, e.yRotO, e.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
        float shake = e.shakeTime - pt;
        if (shake > 0) ps.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
        ps.scale(0.85F, 0.85F, 0.85F);
        ps.translate(-0.3, 0, 0);
        ArrowModel.render(buf.getBuffer(Mesh.TYPES[Mesh.CUT]), ps, light, e.tickCount + pt, 1);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SylvanArrowEntity e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
