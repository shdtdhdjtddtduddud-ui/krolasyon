package com.sololeveling.item;

import com.sololeveling.client.ClientHooks;
import com.sololeveling.gen.Content;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SLArmorItem extends ArmorItem {
    public final Content.ArmorDef def;

    public SLArmorItem(Content.ArmorDef def, Type type) {
        super(new SLArmorMaterial(def), type, new Item.Properties().rarity(def.level() >= 60 ? Rarity.EPIC : Rarity.RARE).fireResistant());
        this.def = def;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("gui.sololeveling.req_level", def.level())
                .withStyle(ClientHooks.playerLevel() >= def.level() ? ChatFormatting.GRAY : ChatFormatting.RED));
        tip.add(Component.translatable("gui.sololeveling.set_bonus." + def.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
