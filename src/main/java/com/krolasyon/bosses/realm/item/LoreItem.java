package com.krolasyon.bosses.realm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Item with translated lore lines ("<item key>.desc", ".desc2", ...) and optional enchantment glint. */
public class LoreItem extends Item {
    private final boolean foil;

    public LoreItem(Properties props, boolean foil) {
        super(props);
        this.foil = foil;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return foil || super.isFoil(stack); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        addLore(this.getDescriptionId(), lines);
        super.appendHoverText(stack, level, lines, flag);
    }

    public static void addLore(String key, List<Component> lines) {
        Language lang = Language.getInstance();
        for (int i = 1; i <= 6; i++) {
            String k = key + ".desc" + (i == 1 ? "" : String.valueOf(i));
            if (!lang.has(k)) break;
            lines.add(Component.translatable(k).withStyle(i == 1 ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
