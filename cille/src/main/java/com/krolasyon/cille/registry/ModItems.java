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
            () -> new CilleMarbleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> KABKABA_SPAWN_EGG = ITEMS.register("kabkaba_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KABKABA, 0x161925, 0x8A8F9E, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("cille", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cille"))
            .icon(() -> new ItemStack(KABKABA_MISKETI.get()))
            .displayItems((params, output) -> {
                output.accept(KABKABA_MISKETI.get());
                output.accept(KABKABA_SPAWN_EGG.get());
            })
            .build());
}
