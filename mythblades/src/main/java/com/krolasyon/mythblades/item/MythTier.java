package com.krolasyon.mythblades.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** Efsane kılıçların malzeme düzeyi. */
public record MythTier(int uses, float speed, float bonus, int enchant, ItemLike repair) implements Tier {
    public static final MythTier RAINBOW = new MythTier(1900, 9F, 5F, 22, Items.AMETHYST_SHARD);
    public static final MythTier CRIMSON = new MythTier(2100, 9F, 4F, 18, Items.REDSTONE);
    public static final MythTier MOSSROOT = new MythTier(2600, 10F, 5F, 14, Items.MOSS_BLOCK);
    public static final MythTier EMBER = new MythTier(2300, 9F, 5F, 20, Items.BLAZE_ROD);

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return bonus; }
    @Override public int getLevel() { return 4; }
    @Override public int getEnchantmentValue() { return enchant; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(repair); }
}
