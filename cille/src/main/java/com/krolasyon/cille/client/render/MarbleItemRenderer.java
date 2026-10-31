package com.krolasyon.cille.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** the marble in hand / inventory / on the ground: smooth 3D glass with its Cille turning inside */
public class MarbleItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static MarbleItemRenderer instance;

    public static MarbleItemRenderer get() {
        if (instance == null) instance = new MarbleItemRenderer();
        return instance;
    }

    private MarbleItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float age = (mc.level != null ? mc.level.getGameTime() % 24000L : 0L) + mc.getFrameTime();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        SmoothSphere.marble(pose, buffers, MiniCille.forItem(stack.getItem(), mc.getEntityModels()), 0.42F, age, 0F, light);
        pose.popPose();
    }
}
