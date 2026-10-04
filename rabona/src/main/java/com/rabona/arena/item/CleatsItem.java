package com.rabona.arena.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Altin Krampon: hiz ve sut gucu bonusu. */
public class CleatsItem extends ArmorItem {
    static final UUID SPEED = UUID.fromString("4d9a3c1e-7b21-4f0e-9a5d-2f6c8e1b0a77");

    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        public int getDurabilityForType(Type t) { return 600; }
        public int getDefenseForType(Type t) { return 2; }
        public int getEnchantmentValue() { return 18; }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_GOLD; }
        public Ingredient getRepairIngredient() { return Ingredient.of(Items.GOLD_INGOT); }
        public String getName() { return "rabonaarena:golden_cleats"; }
        public float getToughness() { return 0; }
        public float getKnockbackResistance() { return 0; }
    };

    public CleatsItem(Type type, Properties p) { super(MATERIAL, type, p.stacksTo(1)); }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.FEET) return super.getAttributeModifiers(slot, stack);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.putAll(super.getAttributeModifiers(slot, stack));
        b.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPEED, "Krampon hizi", 0.12, AttributeModifier.Operation.MULTIPLY_BASE));
        return b.build();
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.rabonaarena.cleats"));
    }

    public static boolean wearing(net.minecraft.world.entity.LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof CleatsItem;
    }
}
