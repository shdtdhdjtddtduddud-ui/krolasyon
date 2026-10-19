package com.krolasyon.emeraldblade.client;

import com.krolasyon.emeraldblade.item.EmeraldRootBladeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Kılıcın her görünümde (el, envanter, çerçeve, yer) 3B animasyonlu modelini çizer. */
public final class BladeItemRenderer extends BlockEntityWithoutLevelRenderer {
    public BladeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float pt = mc.getFrameTime();
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + pt;
        float charge = 0;
        Player p = mc.player;
        if (p != null && p.isUsingItem() && p.getUseItem() == stack) {
            charge = Math.min(1, (p.getTicksUsingItem() + pt) / EmeraldRootBladeItem.CHARGE_FULL);
        }
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        switch (ctx) {
            case GUI, GROUND, FIXED, HEAD, NONE -> {
                ps.mulPose(Axis.ZP.rotationDegrees(-45));
                float s = ctx == ItemDisplayContext.GUI ? 0.44F : 0.5F;
                ps.scale(s, s, s);
                ps.translate(0, -0.45, 0);
            }
            default -> {
                // vanilya kılıç sprite'ı gibi: kabza sol altta, bıçak sağ üste doğru
                ps.translate(-0.24, -0.24, 0);
                ps.mulPose(Axis.ZP.rotationDegrees(-45));
                ps.scale(0.62F, 0.62F, 0.62F);
                ps.translate(0, 0.28, 0);
            }
        }
        BladeModel.render(ps, buffers, time, charge, light);
        ps.popPose();
    }
}
