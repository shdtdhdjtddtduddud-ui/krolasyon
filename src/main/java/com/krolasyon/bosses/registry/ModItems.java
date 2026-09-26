package com.krolasyon.bosses.registry;

import com.krolasyon.bosses.KrolasyonBosses;
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

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, KrolasyonBosses.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KrolasyonBosses.MODID);

    public static final RegistryObject<Item> SEAL_WARDEN_EGG = ITEMS.register("seal_warden_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SEAL_WARDEN, 0xB89A8E, 0xF58ADB, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CRIMSON_HOUND_EGG = ITEMS.register("crimson_hound_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CRIMSON_HOUND, 0x2E2230, 0xFF3A48, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("bosses", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolasyonbosses"))
            .icon(() -> new ItemStack(SEAL_WARDEN_EGG.get()))
            .displayItems((params, output) -> {
                output.accept(SEAL_WARDEN_EGG.get());
                output.accept(CRIMSON_HOUND_EGG.get());
            })
            .build());
}
