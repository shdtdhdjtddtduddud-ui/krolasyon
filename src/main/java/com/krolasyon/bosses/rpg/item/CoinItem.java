package com.krolasyon.bosses.rpg.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Currency. 1 gold = 10 silver = 100 copper. */
public class CoinItem extends Item {
    public final int value;
    private final String title;

    public CoinItem(int value, String title, Properties p) {
        super(p);
        this.value = value;
        this.title = title;
    }

    @Override
    public Component getName(ItemStack stack) { return Component.literal(title); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7Değeri: " + value + " bakır"));
    }
}
