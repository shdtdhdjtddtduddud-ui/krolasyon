package com.krolasyon.emeraldblade.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public final class BladeTier implements Tier {
    public static final BladeTier INSTANCE = new BladeTier();

    @Override public int getUses() { return 2400; }
    @Override public float getSpeed() { return 9.0F; }
    @Override public float getAttackDamageBonus() { return 7.0F; }
    @Override public int getLevel() { return 4; }
    @Override public int getEnchantmentValue() { return 18; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.EMERALD); }
}
