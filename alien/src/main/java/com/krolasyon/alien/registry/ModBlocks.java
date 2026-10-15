package com.krolasyon.alien.registry;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AlienMod.MODID);

    /** Sevastopol pressure doors: open by hand (so the alien's pathfinding treats them like wooden doors). */
    public static final BlockSetType STATION_DOOR_TYPE = BlockSetType.register(new BlockSetType("alienisolation_station", true, SoundType.METAL,
            SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON));

    static BlockBehaviour.Properties metal(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(3.0F, 1200.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static <T extends Block> RegistryObject<T> reg(String name, Supplier<T> b) {
        return BLOCKS.register(name, b);
    }

    public static final RegistryObject<Block> STATION_WALL = reg("station_wall", () -> new Block(metal(MapColor.SNOW)));
    public static final RegistryObject<Block> STATION_WALL_DARK = reg("station_wall_dark", () -> new Block(metal(MapColor.COLOR_GRAY)));
    public static final RegistryObject<Block> STATION_WALL_DIRTY = reg("station_wall_dirty", () -> new Block(metal(MapColor.TERRACOTTA_WHITE)));
    public static final RegistryObject<Block> STATION_WALL_GREEN = reg("station_wall_green", () -> new Block(metal(MapColor.COLOR_GREEN)));
    public static final RegistryObject<Block> STATION_FLOOR = reg("station_floor", () -> new Block(metal(MapColor.METAL)));
    public static final RegistryObject<Block> FLOOR_GRATE = reg("floor_grate", () -> new Block(metal(MapColor.COLOR_BLACK)));
    public static final RegistryObject<Block> STATION_CEILING = reg("station_ceiling", () -> new Block(metal(MapColor.SNOW)));
    public static final RegistryObject<Block> HAZARD_PANEL = reg("hazard_panel", () -> new Block(metal(MapColor.COLOR_RED)));
    public static final RegistryObject<Block> STATION_TRIM = reg("station_trim", () -> new Block(metal(MapColor.COLOR_GRAY)));
    public static final RegistryObject<Block> CEILING_LIGHT = reg("ceiling_light", () -> new CeilingLightBlock(metal(MapColor.SNOW)
            .lightLevel(s -> s.getValue(CeilingLightBlock.LIT) ? 14 : 0).randomTicks()));
    public static final RegistryObject<Block> EMERGENCY_LIGHT = reg("emergency_light", () -> new Block(metal(MapColor.COLOR_RED)
            .lightLevel(s -> 7).noOcclusion()));
    public static final RegistryObject<Block> VENT = reg("vent", () -> new VentBlock(metal(MapColor.COLOR_GRAY).noOcclusion()));
    public static final RegistryObject<Block> LOCKER = reg("locker", () -> new LockerBlock(metal(MapColor.COLOR_LIGHT_GRAY)
            .noOcclusion().isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Block> SAVE_STATION = reg("save_station", () -> new SaveStationBlock(metal(MapColor.COLOR_BLUE)
            .noOcclusion().lightLevel(s -> 5)));
    public static final RegistryObject<Block> TERMINAL = reg("terminal", () -> new TerminalBlock(metal(MapColor.COLOR_GRAY)
            .noOcclusion().lightLevel(s -> 4)));
    public static final RegistryObject<Block> DESK = reg("desk", () -> new DeskBlock(metal(MapColor.COLOR_LIGHT_GRAY).noOcclusion()));
    public static final RegistryObject<Block> DEBRIS = reg("debris", () -> new DebrisBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE).strength(0.1F).sound(SoundType.WOOL).noCollission().noOcclusion()));
    public static final RegistryObject<Block> LAB_DOOR = reg("lab_door", () -> new StationDoorBlock(metal(MapColor.COLOR_GREEN)
            .noOcclusion().pushReaction(PushReaction.DESTROY), STATION_DOOR_TYPE));
    public static final RegistryObject<Block> SECURITY_DOOR = reg("security_door", () -> new StationDoorBlock(metal(MapColor.COLOR_ORANGE)
            .noOcclusion().pushReaction(PushReaction.DESTROY), STATION_DOOR_TYPE));
}
