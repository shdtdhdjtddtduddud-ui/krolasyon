package com.krolasyon.bosses.item;

import com.krolasyon.bosses.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public enum HellTier implements Tier {
    HELLSTEEL;

    @Override public int getUses() { return 1850; }
    @Override public float getSpeed() { return 8.5F; }
    @Override public float getAttackDamageBonus() { return 0F; }
    @Override public int getLevel() { return 4; }
    @Override public int getEnchantmentValue() { return 18; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.MATERIALS.get("hellsteel_ingot").get()); }
}
