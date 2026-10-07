package com.krolasyon.furniture.registry;

import com.krolasyon.furniture.FurnitureMod;
import com.krolasyon.furniture.menu.WardrobeMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FurnitureMod.MODID);

    public static final RegistryObject<MenuType<WardrobeMenu>> WARDROBE = MENUS.register("wardrobe",
            () -> new MenuType<>(WardrobeMenu::new, FeatureFlags.VANILLA_SET));
}
