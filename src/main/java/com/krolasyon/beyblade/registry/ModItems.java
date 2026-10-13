package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.item.LauncherItem;
import com.krolasyon.beyblade.item.LibraItem;
import com.krolasyon.beyblade.item.RivalLibraItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LibraBeyblade.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LibraBeyblade.MODID);

    public static final RegistryObject<Item> LIBRA = ITEMS.register("flame_libra",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> LAUNCHER = ITEMS.register("libra_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> RIVAL = ITEMS.register("rival_libra",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("beyblade", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.librabeyblade"))
            .icon(() -> new ItemStack(LIBRA.get()))
            .displayItems((params, output) -> {
                output.accept(LIBRA.get());
                output.accept(LAUNCHER.get());
                output.accept(RIVAL.get());
            })
            .build());
}
