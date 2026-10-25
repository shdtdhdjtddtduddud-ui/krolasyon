package com.krolasyon.labirent.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/** A sharpened wooden spear with a metal tip: long reach, the keepers' weapon against Grievers. */
public class SpearItem extends SwordItem {
    private static final UUID REACH = UUID.fromString("2f0a6d8e-5b8c-4e1f-9d3a-7a5c1b2e4f60");
    private final Multimap<Attribute, AttributeModifier> mods;

    public SpearItem(Properties p) {
        super(Tiers.IRON, 3, -2.6F, p);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 6.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.6, AttributeModifier.Operation.ADDITION));
        b.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH, "Spear reach", 1.5, AttributeModifier.Operation.ADDITION));
        mods = b.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND ? mods : super.getAttributeModifiers(slot, stack);
    }
}
