package com.krolasyon.sylvanarchery.client;

import com.krolasyon.sylvanarchery.item.QuiverItem;
import com.krolasyon.sylvanarchery.item.SylvanArrowItem;
import com.krolasyon.sylvanarchery.item.SylvanBowItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Yay, ok ve sadağın her görünümde 3B animasyonlu çizimi. */
public final class SylvanItemRenderer extends BlockEntityWithoutLevelRenderer {
    public SylvanItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float pt = mc.getFrameTime();
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + pt;
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        if (stack.getItem() instanceof SylvanBowItem) {
            float pull = 0;
            Player p = mc.player;
            if (p != null && p.isUsingItem() && p.getUseItem() == stack) pull = SylvanBowItem.powerFor(p.getTicksUsingItem() + pt);
            // vanilya yay sprite'ı gibi: ok sol üste, kiriş köşegen boyunca
            ps.mulPose(Axis.ZP.rotationDegrees(135));
            float s = ctx == ItemDisplayContext.GUI ? 0.42F : ctx == ItemDisplayContext.FIXED ? 0.6F : 0.5F;
            ps.scale(s, s, s);
            BowModel.render(ps, buf, time, pull, light);
        } else if (stack.getItem() instanceof SylvanArrowItem) {
            ps.mulPose(Axis.ZP.rotationDegrees(45));
            float s = ctx == ItemDisplayContext.GUI ? 0.72F : 0.8F;
            ps.scale(s, s, s);
            ps.translate(-0.06, 0, 0);
            ArrowModel.render(buf.getBuffer(Mesh.TYPES[Mesh.CUT]), ps, light, time, 1);
        } else if (stack.getItem() instanceof QuiverItem) {
            float s = ctx == ItemDisplayContext.GUI ? 1.0F : 1.1F;
            ps.scale(s, s, s);
            ps.translate(0, -0.1, 0);
            int count = QuiverItem.count(stack);
            QuiverModel.render(ps, buf, light, time, count == 0 ? 0 : 3 + Math.round(8F * count / QuiverItem.CAPACITY));
        }
        ps.popPose();
    }
}
