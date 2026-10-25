package com.krolasyon.labirent.item;

import com.krolasyon.labirent.story.StoryDirector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A scrap with one word of the maze code, copied from a code stone. */
public class CodeWordItem extends Item {
    public CodeWordItem(Properties p) { super(p); }

    public static ItemStack of(int sector) {
        ItemStack s = new ItemStack(com.krolasyon.labirent.registry.ModItems.CODE_WORD.get());
        s.getOrCreateTag().putInt("Sector", sector);
        return s;
    }

    @Override
    public Component getName(ItemStack s) {
        if (s.hasTag() && s.getTag().contains("Sector")) {
            int sec = s.getTag().getInt("Sector");
            return Component.translatable("item.labirent.code_word.named", sec + 1, StoryDirector.WORDS[sec]).withStyle(ChatFormatting.AQUA);
        }
        return super.getName(s);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.labirent.code_word.tip").withStyle(ChatFormatting.GRAY));
    }
}
