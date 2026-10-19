package com.krolasyon.aethercore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Envanterde ve elde de aynı animasyonlu model (yarı açık). */
public final class AetherItemRenderer extends BlockEntityWithoutLevelRenderer {
    public AetherItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + mc.getFrameTime();
        float open = 0.55F + 0.15F * (float) Math.sin(time * 0.03);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (ctx == ItemDisplayContext.GUI) pose.scale(1.15F, 1.15F, 1.15F);
        CoreModel.render(pose, buffers, time, open, 0, light, overlay);
        pose.popPose();
    }
}
