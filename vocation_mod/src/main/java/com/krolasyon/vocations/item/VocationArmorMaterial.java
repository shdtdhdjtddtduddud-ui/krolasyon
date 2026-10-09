package com.krolasyon.vocations.item;

import com.krolasyon.vocations.VocationsMod;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * One material per vocation. The full set (helmet, armor, legs, boots) grants {@link #getSetBonus()}.
 * defense order: helmet, armor, legs, boots.
 */
public enum VocationArmorMaterial implements ArmorMaterial {
    KNIGHT("knight", 15, new int[]{3, 8, 6, 3}, 12, 3.0F, 0.1F,
            () -> Ingredient.of(Items.IRON_INGOT), SoundEvents.ARMOR_EQUIP_IRON, () -> MobEffects.DAMAGE_RESISTANCE),
    PALADIN("paladin", 33, new int[]{3, 8, 6, 3}, 15, 3.5F, 0.1F,
            () -> Ingredient.of(Items.DIAMOND), SoundEvents.ARMOR_EQUIP_DIAMOND, () -> MobEffects.REGENERATION),
    DRUID("druid", 6, new int[]{2, 5, 4, 2}, 22, 0.5F, 0.0F,
            () -> Ingredient.of(Items.LEATHER), SoundEvents.ARMOR_EQUIP_LEATHER, () -> MobEffects.MOVEMENT_SPEED),
    SORCERER("sorcerer", 8, new int[]{2, 5, 4, 2}, 25, 1.0F, 0.0F,
            () -> Ingredient.of(Items.AMETHYST_SHARD), SoundEvents.ARMOR_EQUIP_LEATHER, () -> MobEffects.DAMAGE_BOOST);

    private final String id;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantmentValue;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;
    private final SoundEvent equipSound;
    private final Supplier<MobEffect> setBonus;

    VocationArmorMaterial(String id, int durabilityMultiplier, int[] defense, int enchantmentValue, float toughness,
                          float knockbackResistance, Supplier<Ingredient> repairIngredient, SoundEvent equipSound,
                          Supplier<MobEffect> setBonus) {
        this.id = id;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantmentValue = enchantmentValue;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
        this.equipSound = equipSound;
        this.setBonus = setBonus;
    }

    public String getId() {
        return id;
    }

    public MobEffect getSetBonus() {
        return setBonus.get();
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return type.getDurability(durabilityMultiplier);
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> defense[0];
            case CHESTPLATE -> defense[1];
            case LEGGINGS -> defense[2];
            case BOOTS -> defense[3];
        };
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return VocationsMod.MODID + ":" + id;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
