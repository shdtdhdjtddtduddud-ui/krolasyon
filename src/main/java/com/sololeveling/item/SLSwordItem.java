package com.sololeveling.item;

import com.sololeveling.client.ClientHooks;
import com.sololeveling.gen.Content;
import com.sololeveling.util.Ranks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SLSwordItem extends SwordItem {
    public final Content.WeaponDef def;

    public SLSwordItem(Content.WeaponDef def) {
        super(new SLTier(def.durability()), (int) (def.damage() - 1), def.speed(),
                new Item.Properties().rarity(Ranks.rarity(def.rank())).fireResistant());
        this.def = def;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return def.rank().equals("S") || def.rank().equals("N") || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(Component.empty().append(Ranks.tag(def.rank())).append(Component.literal("  "))
                .append(Component.translatable("gui.sololeveling.req_level", def.level())
                        .withStyle(ClientHooks.playerLevel() >= def.level() ? ChatFormatting.GRAY : ChatFormatting.RED)));
        tip.add(Component.translatable("item.sololeveling." + def.id() + ".desc").withStyle(ChatFormatting.DARK_AQUA));
        if (!def.special().equals("none"))
            tip.add(Component.translatable("gui.sololeveling.special." + def.special()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
