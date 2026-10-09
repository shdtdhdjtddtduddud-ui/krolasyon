package com.krolasyon.vocations.registry;

import com.krolasyon.vocations.VocationsMod;
import com.krolasyon.vocations.item.AccessoryItem;
import com.krolasyon.vocations.item.AccessorySlot;
import com.krolasyon.vocations.item.StaffItem;
import com.krolasyon.vocations.item.VocationArmorMaterial;
import com.krolasyon.vocations.item.VocationArrowItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VocationItems {
    private VocationItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, VocationsMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VocationsMod.MODID);

    /** Every item, in creative-tab order. */
    private static final List<RegistryObject<? extends Item>> ALL = new ArrayList<>();
    /** Accessories are checked every few ticks to apply or remove their attribute bonus. */
    public static final List<RegistryObject<AccessoryItem>> ACCESSORIES = new ArrayList<>();

    private static RegistryObject<ArmorItem> armor(VocationArmorMaterial material, ArmorItem.Type type, String part) {
        RegistryObject<ArmorItem> object = ITEMS.register(material.getId() + "_" + part,
                () -> new ArmorItem(material, type, new Item.Properties()));
        ALL.add(object);
        return object;
    }

    private static RegistryObject<AccessoryItem> accessory(VocationArmorMaterial material, AccessorySlot slot,
                                                           String part, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                                           double amount, AttributeModifier.Operation operation) {
        RegistryObject<AccessoryItem> object = ITEMS.register(material.getId() + "_" + part,
                () -> new AccessoryItem(new Item.Properties(), slot, attribute, amount, operation));
        ALL.add(object);
        ACCESSORIES.add(object);
        return object;
    }

    private static RegistryObject<Item> simple(VocationArmorMaterial material, String part, java.util.function.Supplier<Item> factory) {
        RegistryObject<Item> object = ITEMS.register(material.getId() + "_" + part, factory);
        ALL.add(object);
        return object;
    }

    // Elite Knight
    public static final RegistryObject<ArmorItem> KNIGHT_HELMET = armor(VocationArmorMaterial.KNIGHT, ArmorItem.Type.HELMET, "helmet");
    public static final RegistryObject<ArmorItem> KNIGHT_ARMOR = armor(VocationArmorMaterial.KNIGHT, ArmorItem.Type.CHESTPLATE, "armor");
    public static final RegistryObject<ArmorItem> KNIGHT_LEGS = armor(VocationArmorMaterial.KNIGHT, ArmorItem.Type.LEGGINGS, "legs");
    public static final RegistryObject<ArmorItem> KNIGHT_BOOTS = armor(VocationArmorMaterial.KNIGHT, ArmorItem.Type.BOOTS, "boots");
    public static final RegistryObject<Item> KNIGHT_WEAPON = simple(VocationArmorMaterial.KNIGHT, "weapon",
            () -> new SwordItem(Tiers.DIAMOND, 6, -2.4F, new Item.Properties()));
    public static final RegistryObject<AccessoryItem> KNIGHT_AMULET = accessory(VocationArmorMaterial.KNIGHT,
            AccessorySlot.AMULET, "amulet", Attributes.MAX_HEALTH, 4.0, AttributeModifier.Operation.ADDITION);
    public static final RegistryObject<AccessoryItem> KNIGHT_RING = accessory(VocationArmorMaterial.KNIGHT,
            AccessorySlot.RING, "ring", Attributes.ARMOR, 2.0, AttributeModifier.Operation.ADDITION);
    public static final RegistryObject<Item> KNIGHT_AMMO = simple(VocationArmorMaterial.KNIGHT, "ammo",
            () -> new VocationArrowItem(new Item.Properties(), 1.0F));

    // Royal Paladin
    public static final RegistryObject<ArmorItem> PALADIN_HELMET = armor(VocationArmorMaterial.PALADIN, ArmorItem.Type.HELMET, "helmet");
    public static final RegistryObject<ArmorItem> PALADIN_ARMOR = armor(VocationArmorMaterial.PALADIN, ArmorItem.Type.CHESTPLATE, "armor");
    public static final RegistryObject<ArmorItem> PALADIN_LEGS = armor(VocationArmorMaterial.PALADIN, ArmorItem.Type.LEGGINGS, "legs");
    public static final RegistryObject<ArmorItem> PALADIN_BOOTS = armor(VocationArmorMaterial.PALADIN, ArmorItem.Type.BOOTS, "boots");
    public static final RegistryObject<Item> PALADIN_WEAPON = simple(VocationArmorMaterial.PALADIN, "weapon",
            () -> new CrossbowItem(new Item.Properties().durability(465)));
    public static final RegistryObject<AccessoryItem> PALADIN_AMULET = accessory(VocationArmorMaterial.PALADIN,
            AccessorySlot.AMULET, "amulet", Attributes.ARMOR_TOUGHNESS, 1.0, AttributeModifier.Operation.ADDITION);
    public static final RegistryObject<AccessoryItem> PALADIN_RING = accessory(VocationArmorMaterial.PALADIN,
            AccessorySlot.RING, "ring", Attributes.ATTACK_DAMAGE, 1.0, AttributeModifier.Operation.ADDITION);
    public static final RegistryObject<Item> PALADIN_AMMO = simple(VocationArmorMaterial.PALADIN, "ammo",
            () -> new VocationArrowItem(new Item.Properties(), 1.5F));

    // Elder Druid
    public static final RegistryObject<ArmorItem> DRUID_HELMET = armor(VocationArmorMaterial.DRUID, ArmorItem.Type.HELMET, "helmet");
    public static final RegistryObject<ArmorItem> DRUID_ARMOR = armor(VocationArmorMaterial.DRUID, ArmorItem.Type.CHESTPLATE, "armor");
    public static final RegistryObject<ArmorItem> DRUID_LEGS = armor(VocationArmorMaterial.DRUID, ArmorItem.Type.LEGGINGS, "legs");
    public static final RegistryObject<ArmorItem> DRUID_BOOTS = armor(VocationArmorMaterial.DRUID, ArmorItem.Type.BOOTS, "boots");
    public static final RegistryObject<Item> DRUID_WEAPON = simple(VocationArmorMaterial.DRUID, "weapon",
            () -> new StaffItem(new Item.Properties(), 5.0F, 12));
    public static final RegistryObject<AccessoryItem> DRUID_AMULET = accessory(VocationArmorMaterial.DRUID,
            AccessorySlot.AMULET, "amulet", Attributes.MOVEMENT_SPEED, 0.1, AttributeModifier.Operation.MULTIPLY_BASE);
    public static final RegistryObject<AccessoryItem> DRUID_RING = accessory(VocationArmorMaterial.DRUID,
            AccessorySlot.RING, "ring", Attributes.MAX_HEALTH, 2.0, AttributeModifier.Operation.ADDITION);
    public static final RegistryObject<Item> DRUID_AMMO = simple(VocationArmorMaterial.DRUID, "ammo",
            () -> new VocationArrowItem(new Item.Properties(), 0.5F));

    // Master Sorcerer
    public static final RegistryObject<ArmorItem> SORCERER_HELMET = armor(VocationArmorMaterial.SORCERER, ArmorItem.Type.HELMET, "helmet");
    public static final RegistryObject<ArmorItem> SORCERER_ARMOR = armor(VocationArmorMaterial.SORCERER, ArmorItem.Type.CHESTPLATE, "armor");
    public static final RegistryObject<ArmorItem> SORCERER_LEGS = armor(VocationArmorMaterial.SORCERER, ArmorItem.Type.LEGGINGS, "legs");
    public static final RegistryObject<ArmorItem> SORCERER_BOOTS = armor(VocationArmorMaterial.SORCERER, ArmorItem.Type.BOOTS, "boots");
    public static final RegistryObject<Item> SORCERER_WEAPON = simple(VocationArmorMaterial.SORCERER, "weapon",
            () -> new StaffItem(new Item.Properties(), 8.0F, 10));
    public static final RegistryObject<AccessoryItem> SORCERER_AMULET = accessory(VocationArmorMaterial.SORCERER,
            AccessorySlot.AMULET, "amulet", Attributes.ATTACK_DAMAGE, 2.0, AttributeModifier.Operation.ADDITION);
    public static final RegistryObject<AccessoryItem> SORCERER_RING = accessory(VocationArmorMaterial.SORCERER,
            AccessorySlot.RING, "ring", Attributes.ARMOR, 1.0, AttributeModifier.Operation.ADDITION);
    public static final RegistryObject<Item> SORCERER_AMMO = simple(VocationArmorMaterial.SORCERER, "ammo",
            () -> new VocationArrowItem(new Item.Properties(), 0.5F));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("vocations", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolasyonvocations"))
            .icon(() -> new ItemStack(KNIGHT_HELMET.get()))
            .displayItems((params, output) -> {
                for (RegistryObject<? extends Item> object : ALL) {
                    output.accept(object.get());
                }
            })
            .build());
}
