package com.krolasyon.futbol.registry;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.block.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FutbolMod.MODID);

    public static final RegistryObject<Block> GRASS_LIGHT = reg("pitch_grass_light", () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)));
    public static final RegistryObject<Block> GRASS_DARK = reg("pitch_grass_dark", () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)));
    public static final RegistryObject<Block> LINE = reg("pitch_line", () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK).mapColor(MapColor.SNOW)));
    public static final RegistryObject<Block> GOAL_POST = reg("goal_post", () -> new GoalPostBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).strength(2.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> GOAL_NET = reg("goal_net", () -> new GoalNetBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL).strength(0.4F).sound(SoundType.WOOL).noOcclusion()));
    public static final RegistryObject<Block> GOAL_NET_ROOF = reg("goal_net_roof", () -> new NetRoofBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL).strength(0.4F).sound(SoundType.WOOL).noOcclusion()));
    public static final RegistryObject<Block> CORNER_FLAG = reg("corner_flag", () -> new CornerFlagBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(0.3F).sound(SoundType.WOOD).noOcclusion().noCollission()));
    public static final RegistryObject<Block> SEAT_RED = reg("seat_red", () -> new SeatBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> SEAT_BLUE = reg("seat_blue", () -> new SeatBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLUE).strength(1.0F).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> SEAT_WHITE = reg("seat_white", () -> new SeatBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).strength(1.0F).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> FLOODLIGHT = reg("floodlight", () -> new FloodlightBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).lightLevel(s -> 15)));
    public static final RegistryObject<Block> STAND_STEP = reg("stand_step", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE)));

    private static RegistryObject<Block> reg(String name, Supplier<Block> sup) {
        RegistryObject<Block> r = BLOCKS.register(name, sup);
        ModItems.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties()));
        return r;
    }
}
