package com.krolasyon.horror.registry;

import com.krolasyon.horror.HorrorMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REG = DeferredRegister.create(ForgeRegistries.ITEMS, HorrorMod.MODID);

    public static final RegistryObject<Item> HATMAN_SPAWN_EGG = REG.register("hatman_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HATMAN, 0x17120F, 0xB89C6A, new Item.Properties()));

    private ModItems() {}
}
