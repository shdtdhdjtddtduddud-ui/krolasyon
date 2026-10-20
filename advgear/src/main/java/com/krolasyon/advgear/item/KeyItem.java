package com.krolasyon.advgear.item;

import com.krolasyon.advgear.block.LockType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Belirli bir kilit türünü açan anahtar (açınca kilitte kalır). */
public class KeyItem extends Item {
    public final LockType lock;

    public KeyItem(LockType lock, Properties props) {
        super(props.stacksTo(16));
        this.lock = lock;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear.key_" + lock.id).withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.advgear.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
