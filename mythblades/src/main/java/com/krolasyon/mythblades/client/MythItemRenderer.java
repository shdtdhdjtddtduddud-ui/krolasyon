package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.MythBlades;
import com.krolasyon.mythblades.item.CrimsonVeinItem;
import com.krolasyon.mythblades.item.MossrootCleaverItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Üç kılıcın her görünümde (el, envanter, çerçeve, yer) 3B voksel modelini ve animasyonunu çizer. */
public final class MythItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MythItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float pt = mc.getFrameTime();
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + pt;
        Player p = mc.player;
        Item item = stack.getItem();
        boolean hand = ctx.firstPerson() || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        switch (ctx) {
            case FIXED -> {
                ps.scale(1.6F, 1.6F, 1.6F);
                ps.translate(0, -0.3, 0);
            }
            case GUI, GROUND, HEAD, NONE -> {
                ps.mulPose(Axis.ZP.rotationDegrees(-45));
                float s = ctx == ItemDisplayContext.GUI ? 0.42F : 0.5F;
                ps.scale(s, s, s);
                ps.translate(0, -0.42, 0);
            }
            default -> {
                ps.translate(-0.24, -0.24, 0);
                ps.mulPose(Axis.ZP.rotationDegrees(-45));
                ps.scale(0.62F, 0.62F, 0.62F);
                ps.translate(0, 0.3, 0);
            }
        }
        ps.scale(1 / 16F, 1 / 16F, 1 / 16F);
        if (item == MythBlades.RAINBOW.get()) {
            float power = p != null && hand ? p.getCooldowns().getCooldownPercent(item, pt) : 0;
            BladeModels.rainbow(ps, buf, light, time, power);
        } else if (item == MythBlades.CRIMSON.get()) {
            BladeModels.crimson(ps, buf, light, time, CrimsonVeinItem.essence(stack) / (float) CrimsonVeinItem.MAX);
        } else {
            float charge = 0;
            if (p != null && hand && p.isUsingItem() && p.getUseItem() == stack) {
                charge = Math.min(1, (p.getTicksUsingItem() + pt) / MossrootCleaverItem.CHARGE_FULL);
            }
            BladeModels.mossroot(ps, buf, light, time, charge);
        }
        ps.popPose();
    }
}
