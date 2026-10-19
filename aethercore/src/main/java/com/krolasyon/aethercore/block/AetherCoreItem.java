package com.krolasyon.aethercore.block;

import com.krolasyon.aethercore.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public final class AetherCoreItem extends BlockItem {
    public AetherCoreItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.aethercore.desc1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.aethercore.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aethercore.desc3").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ClientHooks.itemExtensions());
    }
}
