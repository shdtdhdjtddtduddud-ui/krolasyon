package com.krolasyon.cille.registry;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.item.CilleMarbleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CilleMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CilleMod.MODID);

    public static final RegistryObject<Item> KABKABA_MISKETI = ITEMS.register("kabkaba_misketi",
            () -> new CilleMarbleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), ModEntities.KABKABA::get));

    public static final RegistryObject<Item> BEHMUD_MISKETI = ITEMS.register("behmud_misketi",
            () -> new CilleMarbleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), ModEntities.BEHMUD::get));

    public static final RegistryObject<Item> KABKABA_SPAWN_EGG = ITEMS.register("kabkaba_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KABKABA, 0x161925, 0x8A8F9E, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> SARKAN_MISKETI = ITEMS.register("sarkan_misketi",
            () -> new CilleMarbleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), ModEntities.SARKAN::get));

    public static final RegistryObject<Item> SARKAN_SPAWN_EGG = ITEMS.register("sarkan_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SARKAN, 0x2E3036, 0xD8C8A0, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> NIRUMEND_MISKETI = ITEMS.register("nirumend_misketi",
            () -> new CilleMarbleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), ModEntities.NIRUMEND::get));

    public static final RegistryObject<Item> NIRUMEND_SPAWN_EGG = ITEMS.register("nirumend_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.NIRUMEND, 0xCDB283, 0xD8782C, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> BEHMUD_SPAWN_EGG = ITEMS.register("behmud_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BEHMUD, 0x0A1430, 0xBFE6EC, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("cille", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cille"))
            .icon(() -> new ItemStack(KABKABA_MISKETI.get()))
            .displayItems((params, output) -> {
                output.accept(KABKABA_MISKETI.get());
                output.accept(KABKABA_SPAWN_EGG.get());
                output.accept(BEHMUD_MISKETI.get());
                output.accept(BEHMUD_SPAWN_EGG.get());
                output.accept(SARKAN_MISKETI.get());
                output.accept(SARKAN_SPAWN_EGG.get());
                output.accept(NIRUMEND_MISKETI.get());
                output.accept(NIRUMEND_SPAWN_EGG.get());
            })
            .build());
}
