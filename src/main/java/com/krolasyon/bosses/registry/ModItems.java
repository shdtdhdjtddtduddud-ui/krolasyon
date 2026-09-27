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

    public static final RegistryObject<Item> REVENGE_EGG = ITEMS.register("revenge_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.REVENGE, 0x8A1620, 0xD8D8DE, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> HEART_DEMON_EGG = ITEMS.register("heart_demon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HEART_DEMON, 0xD85060, 0x1A080E, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> TIDE_BLADE = blade("tide_blade", 0, 3, -2.3F);
    public static final RegistryObject<Item> SOLAR_GREATSWORD = blade("solar_greatsword", 1, 6, -2.9F);
    public static final RegistryObject<Item> DRAGON_GLAIVE = blade("dragon_glaive", 2, 5, -2.7F);
    public static final RegistryObject<Item> EMBER_BLADE = blade("ember_blade", 3, 3, -2.1F);
    public static final RegistryObject<Item> SHADE_BLADE = blade("shade_blade", 4, 3, -2.0F);
    public static final RegistryObject<Item> HELL_TRIDENT = blade("hell_trident", 5, 4, -2.5F);

    private static RegistryObject<Item> blade(String name, int form, int dmg, float speed) {
        return ITEMS.register(name, () -> new com.krolasyon.bosses.item.FormBladeItem(form, dmg, speed, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("bosses", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolasyonbosses"))
            .icon(() -> new ItemStack(TIDE_BLADE.get()))
            .displayItems((params, output) -> {
                output.accept(TIDE_BLADE.get());
                output.accept(SOLAR_GREATSWORD.get());
                output.accept(DRAGON_GLAIVE.get());
                output.accept(EMBER_BLADE.get());
                output.accept(SHADE_BLADE.get());
                output.accept(HELL_TRIDENT.get());
                output.accept(SEAL_WARDEN_EGG.get());
                output.accept(CRIMSON_HOUND_EGG.get());
                output.accept(REVENGE_EGG.get());
                output.accept(HEART_DEMON_EGG.get());
            })
            .build());
}
