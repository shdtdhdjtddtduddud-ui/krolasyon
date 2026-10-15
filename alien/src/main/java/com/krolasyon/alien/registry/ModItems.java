package com.krolasyon.alien.registry;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.entity.ThrownDeviceEntity;
import com.krolasyon.alien.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AlienMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AlienMod.MODID);

    private static final List<RegistryObject<? extends Item>> ORDER = new ArrayList<>();

    private static <T extends Item> RegistryObject<T> reg(String name, Supplier<T> s) {
        RegistryObject<T> r = ITEMS.register(name, s);
        ORDER.add(r);
        return r;
    }

    private static RegistryObject<Item> block(RegistryObject<? extends Block> b) {
        return reg(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final RegistryObject<Item> STATION_CORE = reg("station_core", () -> new StationCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> XENOMORPH_EGG = reg("xenomorph_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.XENOMORPH, 0x161512, 0x6E6A5E, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> MOTION_TRACKER = reg("motion_tracker", () -> new MotionTrackerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FLAMETHROWER = reg("flamethrower", () -> new FlamethrowerItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> FUEL = reg("fuel", () -> new Item(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> NOISEMAKER = reg("noisemaker", () -> new ThrowableDeviceItem(new Item.Properties().stacksTo(8), ThrownDeviceEntity.NOISEMAKER));
    public static final RegistryObject<Item> FLARE = reg("flare", () -> new ThrowableDeviceItem(new Item.Properties().stacksTo(8), ThrownDeviceEntity.FLARE));
    public static final RegistryObject<Item> FLASHLIGHT = reg("flashlight", () -> new FlashlightItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MEDKIT = reg("medkit", () -> new MedkitItem(new Item.Properties().stacksTo(4)));

    public static final RegistryObject<Item> STATION_WALL = block(ModBlocks.STATION_WALL);
    public static final RegistryObject<Item> STATION_WALL_DARK = block(ModBlocks.STATION_WALL_DARK);
    public static final RegistryObject<Item> STATION_WALL_DIRTY = block(ModBlocks.STATION_WALL_DIRTY);
    public static final RegistryObject<Item> STATION_WALL_GREEN = block(ModBlocks.STATION_WALL_GREEN);
    public static final RegistryObject<Item> STATION_FLOOR = block(ModBlocks.STATION_FLOOR);
    public static final RegistryObject<Item> FLOOR_GRATE = block(ModBlocks.FLOOR_GRATE);
    public static final RegistryObject<Item> STATION_CEILING = block(ModBlocks.STATION_CEILING);
    public static final RegistryObject<Item> HAZARD_PANEL = block(ModBlocks.HAZARD_PANEL);
    public static final RegistryObject<Item> STATION_TRIM = block(ModBlocks.STATION_TRIM);
    public static final RegistryObject<Item> CEILING_LIGHT = block(ModBlocks.CEILING_LIGHT);
    public static final RegistryObject<Item> EMERGENCY_LIGHT = block(ModBlocks.EMERGENCY_LIGHT);
    public static final RegistryObject<Item> VENT = block(ModBlocks.VENT);
    public static final RegistryObject<Item> LOCKER = block(ModBlocks.LOCKER);
    public static final RegistryObject<Item> SAVE_STATION = block(ModBlocks.SAVE_STATION);
    public static final RegistryObject<Item> TERMINAL = block(ModBlocks.TERMINAL);
    public static final RegistryObject<Item> DESK = block(ModBlocks.DESK);
    public static final RegistryObject<Item> DEBRIS = block(ModBlocks.DEBRIS);
    public static final RegistryObject<Item> LAB_DOOR = reg("lab_door", () -> new DoubleHighBlockItem(ModBlocks.LAB_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> SECURITY_DOOR = reg("security_door", () -> new DoubleHighBlockItem(ModBlocks.SECURITY_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("sevastopol", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.alienisolation"))
            .icon(() -> new ItemStack(MOTION_TRACKER.get()))
            .displayItems((params, out) -> ORDER.forEach(r -> out.accept(r.get())))
            .build());
}
