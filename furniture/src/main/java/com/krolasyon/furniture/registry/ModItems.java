package com.krolasyon.furniture.registry;

import com.krolasyon.furniture.FurnitureMod;
import com.krolasyon.furniture.item.BroomItem;
import com.krolasyon.furniture.item.FurnitureBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FurnitureMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FurnitureMod.MODID);

    public static final RegistryObject<Item> SOFA = ITEMS.register("sofa", () -> new FurnitureBlockItem(ModBlocks.SOFA.get(), new Item.Properties()));
    public static final RegistryObject<Item> PIANO = ITEMS.register("piano", () -> new FurnitureBlockItem(ModBlocks.PIANO.get(), new Item.Properties()));
    public static final RegistryObject<Item> CHALKBOARD = ITEMS.register("chalkboard", () -> new FurnitureBlockItem(ModBlocks.CHALKBOARD.get(), new Item.Properties()));
    public static final RegistryObject<Item> NIGHTSTAND = ITEMS.register("nightstand", () -> new FurnitureBlockItem(ModBlocks.NIGHTSTAND.get(), new Item.Properties()));
    public static final RegistryObject<Item> WARDROBE = ITEMS.register("wardrobe", () -> new FurnitureBlockItem(ModBlocks.WARDROBE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BROOM = ITEMS.register("broom", () -> new BroomItem(new Item.Properties().durability(256)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("furniture", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolasyonfurniture"))
            .icon(() -> new ItemStack(PIANO.get()))
            .displayItems((params, output) -> {
                output.accept(SOFA.get());
                output.accept(PIANO.get());
                output.accept(CHALKBOARD.get());
                output.accept(NIGHTSTAND.get());
                output.accept(WARDROBE.get());
                output.accept(BROOM.get());
            })
            .build());
}
