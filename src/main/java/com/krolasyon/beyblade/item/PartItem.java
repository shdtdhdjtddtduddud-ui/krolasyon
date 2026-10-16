package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A loose beyblade part. Assemble it into a beyblade at the Beyblade Workbench; its upgrade level travels with it. */
public class PartItem extends Item {
    private final BeyPart part;

    public PartItem(Properties props, BeyPart part) {
        super(props);
        this.part = part;
    }

    public BeyPart part() { return part; }

    public static int level(ItemStack s) {
        CompoundTag t = V.tag(s);
        return t == null ? 0 : Math.max(0, Math.min(Loadout.MAX_LEVEL, t.getInt("PartLevel")));
    }

    public static void setLevel(ItemStack s, int lvl) {
        V.editTag(s, t -> t.putInt("PartLevel", lvl));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return level(stack) >= 3; }

    @Override
    public Component getName(ItemStack stack) {
        int lvl = level(stack);
        Component base = Component.translatable(part.nameKey());
        return lvl > 0 ? Component.empty().append(base).append(Component.literal(" " + "★".repeat(lvl)).withStyle(ChatFormatting.GOLD)) : base;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        appendPartLines(part, level(stack), tip);
        tip.add(Component.translatable("item.librabeyblade.part.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** type, stats and specials of a part (also used by the workbench screen) */
    public static void appendPartLines(BeyPart part, int lvl, List<Component> tip) {
        ChatFormatting rc = switch (part.rarity()) { case 3 -> ChatFormatting.LIGHT_PURPLE; case 2 -> ChatFormatting.GOLD; case 1 -> ChatFormatting.AQUA; default -> ChatFormatting.GRAY; };
        tip.add(Component.translatable(part.type().nameKey()).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("  ")).append(Component.translatable("rarity.librabeyblade." + part.rarity()).withStyle(rc)));
        float k = 1F + Loadout.LEVEL_BONUS * lvl;
        tip.add(stat("atk", part.attack() * k, ChatFormatting.RED).append(Component.literal("  "))
                .append(stat("def", part.defense() * k, ChatFormatting.BLUE)).append(Component.literal("  "))
                .append(stat("sta", part.stamina() * k, ChatFormatting.GREEN)).append(Component.literal("  "))
                .append(stat("spd", part.speed() * k, ChatFormatting.AQUA)));
        tip.add(Component.translatable("stat.librabeyblade.weight", String.format(java.util.Locale.ROOT, "%.1f", part.weight())).withStyle(ChatFormatting.GRAY));
        if (part.type() == PartType.TRACK)
            tip.add(Component.translatable("stat.librabeyblade.height", String.format(java.util.Locale.ROOT, "%.1f", part.height() / 10F)).withStyle(ChatFormatting.GRAY));
        if (part.type() == PartType.TIP) tip.add(Component.translatable("tip.librabeyblade." + part.tip().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.DARK_AQUA));
        if (part.trait() != BeyPart.Trait.NONE)
            tip.add(Component.translatable("trait.librabeyblade." + part.trait().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.DARK_AQUA));
        if (lvl > 0) tip.add(Component.translatable("item.librabeyblade.part.level", lvl, Loadout.MAX_LEVEL).withStyle(ChatFormatting.GOLD));
    }

    private static net.minecraft.network.chat.MutableComponent stat(String key, float v, ChatFormatting c) {
        return Component.translatable("stat.librabeyblade." + key + ".short").withStyle(c).append(Component.literal(" " + Math.round(v)).withStyle(ChatFormatting.WHITE));
    }
}
