package com.krolasyon.bosses.realm.item;

import com.krolasyon.bosses.realm.registry.RealmItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public final class RealmTiers {
    private RealmTiers() {}

    public static final Tier INFERNAL = new SimpleTier(1900, 9.0F, 3.5F, 4, 18, () -> Ingredient.of(RealmItems.INFERNAL_STEEL_INGOT.get()));
    public static final Tier LEGENDARY = new SimpleTier(3600, 10.0F, 5.0F, 4, 24, () -> Ingredient.of(RealmItems.TYRANT_HEART.get()));

    record SimpleTier(int uses, float speed, float bonus, int level, int ench, java.util.function.Supplier<Ingredient> repair) implements Tier {
        @Override public int getUses() { return uses; }
        @Override public float getSpeed() { return speed; }
        @Override public float getAttackDamageBonus() { return bonus; }
        @Override public int getLevel() { return level; }
        @Override public int getEnchantmentValue() { return ench; }
        @Override public Ingredient getRepairIngredient() { return repair.get(); }
    }

    public static final ArmorMaterial INFERNAL_ARMOR = new Material("krolasyonbosses:infernal_steel", 42, new int[]{4, 7, 9, 4}, 18,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.15F);
    public static final ArmorMaterial SOVEREIGN = new Material("krolasyonbosses:sovereign", 60, new int[]{5, 8, 10, 5}, 30,
            SoundEvents.ARMOR_EQUIP_GOLD, 4.0F, 0.2F);

    record Material(String name, int durMul, int[] defense, int ench, SoundEvent sound, float tough, float kb) implements ArmorMaterial {
        private static final int[] BASE = {13, 15, 16, 11};

        private static int idx(ArmorItem.Type t) {
            return switch (t) {
                case BOOTS -> 0;
                case LEGGINGS -> 1;
                case CHESTPLATE -> 2;
                case HELMET -> 3;
            };
        }

        @Override public int getDurabilityForType(ArmorItem.Type t) { return BASE[idx(t)] * durMul; }
        @Override public int getDefenseForType(ArmorItem.Type t) { return defense[idx(t)]; }
        @Override public int getEnchantmentValue() { return ench; }
        @Override public SoundEvent getEquipSound() { return sound; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(RealmItems.INFERNAL_STEEL_INGOT.get()); }
        @Override public String getName() { return name; }
        @Override public float getToughness() { return tough; }
        @Override public float getKnockbackResistance() { return kb; }
    }
}
