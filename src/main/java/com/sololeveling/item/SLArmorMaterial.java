package com.sololeveling.item;

import com.sololeveling.SoloLeveling;
import com.sololeveling.gen.Content;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class SLArmorMaterial implements ArmorMaterial {
    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};
    public final Content.ArmorDef def;

    public SLArmorMaterial(Content.ArmorDef def) { this.def = def; }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.ordinal()] * (20 + def.level() / 3);
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> def.head();
            case CHESTPLATE -> def.chest();
            case LEGGINGS -> def.legs();
            case BOOTS -> def.boots();
        };
    }

    @Override public int getEnchantmentValue() { return 18; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    @Override public String getName() { return SoloLeveling.MODID + ":" + def.id(); }
    @Override public float getToughness() { return def.toughness(); }
    @Override public float getKnockbackResistance() { return def.kbRes(); }
}
