package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.item.CoinItem;
import com.krolasyon.bosses.rpg.item.RpgSwordItem;
import com.krolasyon.bosses.rpg.item.SpellTomeItem;
import com.krolasyon.bosses.rpg.item.StaffItem;
import com.krolasyon.bosses.rpg.world.Race;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;

/** How much an NPC appreciates a gift: base value of the item and the tastes of each race. */
public final class Gifts {
    private Gifts() {}

    public static int value(ItemStack st) {
        Item i = st.getItem();
        int n = st.getCount();
        if (i instanceof CoinItem c) return c.value * n;
        if (i instanceof RpgSwordItem s) return 150 + s.def.tier() * 120;
        if (i instanceof SpellTomeItem) return 120;
        if (i instanceof StaffItem s) return 100 + s.tier * 150;
        if (i == Items.DIAMOND || i == Items.EMERALD) return 60 * n;
        if (i == Items.GOLD_INGOT) return 25 * n;
        if (i == Items.IRON_INGOT) return 8 * n;
        if (i == Items.NETHERITE_INGOT) return 500 * n;
        if (i == Items.CAKE) return 30;
        if (st.isEdible()) return 3 * n;
        if (st.is(ItemTags.FLOWERS)) return 4 * n;
        if (i instanceof ArmorItem a) return 40 + a.getDefense() * 15;
        if (i instanceof TieredItem t) return 15 + t.getTier().getLevel() * 30;
        if (st.getRarity() == Rarity.EPIC) return 300;
        if (st.getRarity() == Rarity.RARE) return 150;
        return n;
    }

    /** multiplier: 2 = loves, 1 = neutral, 0 = indifferent, -1 = insulted */
    public static float taste(Race race, ItemStack st) {
        Item i = st.getItem();
        boolean flower = st.is(ItemTags.FLOWERS);
        boolean meat = i == Items.COOKED_BEEF || i == Items.COOKED_PORKCHOP || i == Items.COOKED_MUTTON || i == Items.COOKED_CHICKEN || i == Items.BEEF || i == Items.PORKCHOP;
        boolean gold = i == Items.GOLD_INGOT || i == Items.GOLD_NUGGET || i == Items.GOLD_BLOCK || i instanceof CoinItem;
        boolean gem = i == Items.DIAMOND || i == Items.EMERALD || i == Items.AMETHYST_SHARD;
        boolean sweet = i == Items.CAKE || i == Items.HONEY_BOTTLE || i == Items.COOKIE || i == Items.PUMPKIN_PIE || i == Items.SWEET_BERRIES;
        boolean weapon = i instanceof SwordItem || i instanceof AxeItem;
        boolean magic = i instanceof SpellTomeItem || i instanceof StaffItem || i == Items.ENCHANTED_BOOK;
        return switch (race) {
            case ELF -> flower || i == Items.APPLE || magic ? 2.0F : meat ? -1.0F : i == Items.IRON_INGOT ? 0.3F : 1.0F;
            case DARK_ELF -> magic || gem || i == Items.SPIDER_EYE ? 2.0F : flower ? 0.3F : 1.0F;
            case DWARF -> gold || gem || i == Items.IRON_INGOT || i instanceof PickaxeItem ? 2.0F : flower ? 0.0F : 1.0F;
            case DEMON -> i == Items.BLAZE_ROD || i == Items.MAGMA_CREAM || i == Items.WITHER_SKELETON_SKULL || gem ? 2.0F : flower || sweet ? -1.0F : 1.0F;
            case GIANT -> meat || i == Items.CAKE || i == Items.PUMPKIN_PIE ? 2.5F : i == Items.SNOWBALL ? 1.5F : 0.8F;
            case ORC -> meat || weapon ? 2.0F : flower || sweet ? -1.0F : 1.0F;
            case BEASTKIN -> meat || i == Items.SALMON || i == Items.COD || i == Items.BONE ? 2.0F : 1.0F;
            case HALFLING -> sweet || i == Items.BREAD || i == Items.MUSHROOM_STEW || i == Items.BAKED_POTATO ? 2.5F : weapon ? 0.3F : 1.0F;
            case HUMAN -> gold || sweet || flower ? 1.5F : 1.0F;
        };
    }
}
