package com.krolasyon.advgear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Diken Maymuncuk: her kilidi şansla açar (demir %45, kemik %30, kalp %15); her deneme yıpratır. */
public class LockpickItem extends Item {
    public LockpickItem(Properties props) {
        super(props.durability(12));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear.thorn_lockpick").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.IRON_NUGGET);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.advgear.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
