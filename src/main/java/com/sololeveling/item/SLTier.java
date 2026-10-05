package com.sololeveling.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public record SLTier(int uses) implements Tier {
    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return 8.0F; }
    @Override public float getAttackDamageBonus() { return 0.0F; }
    @Override public int getLevel() { return 4; }
    @Override public int getEnchantmentValue() { return 18; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
}
