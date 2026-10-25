package com.krolasyon.labirent.registry;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.block.CodeStoneBlock;
import com.krolasyon.labirent.block.CodeTerminalBlock;
import com.krolasyon.labirent.block.GrieverHoleBlock;
import com.krolasyon.labirent.block.MapTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Labirent.MODID);

    private static BlockBehaviour.Properties unbreakable(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1.0F, 3600000.0F).sound(SoundType.STONE)
                .pushReaction(PushReaction.BLOCK).noLootTable().isValidSpawn((s, l, p, e) -> false);
    }

    public static final RegistryObject<Block> MAZE_WALL = reg("maze_wall", () -> new Block(unbreakable(MapColor.STONE)));
    public static final RegistryObject<Block> MAZE_WALL_MOSSY = reg("maze_wall_mossy", () -> new Block(unbreakable(MapColor.STONE)));
    public static final RegistryObject<Block> MAZE_WALL_CRACKED = reg("maze_wall_cracked", () -> new Block(unbreakable(MapColor.STONE)));
    public static final RegistryObject<Block> MAZE_WALL_IVY = reg("maze_wall_ivy", () -> new Block(unbreakable(MapColor.PLANT).sound(SoundType.MOSS)));
    public static final RegistryObject<Block> MAZE_DOOR = reg("maze_door", () -> new Block(unbreakable(MapColor.METAL)));
    public static final RegistryObject<Block> MAZE_FLOOR = reg("maze_floor", () -> new Block(unbreakable(MapColor.DIRT).sound(SoundType.GRAVEL)));
    public static final RegistryObject<Block> MAZE_FLOOR_MOSSY = reg("maze_floor_mossy", () -> new Block(unbreakable(MapColor.PLANT).sound(SoundType.MOSS)));
    public static final RegistryObject<Block> BOX_FLOOR = reg("box_floor", () -> new Block(unbreakable(MapColor.METAL).sound(SoundType.METAL)));
    public static final RegistryObject<Block> WICKED_PLATE = reg("wicked_plate", () -> new Block(unbreakable(MapColor.METAL).sound(SoundType.METAL)));
    public static final RegistryObject<Block> LAB_WALL = reg("lab_wall", () -> new Block(unbreakable(MapColor.SNOW).sound(SoundType.METAL)));
    public static final RegistryObject<Block> LAB_LIGHT = reg("lab_light", () -> new Block(unbreakable(MapColor.SNOW).sound(SoundType.GLASS).lightLevel(s -> 15)));
    public static final RegistryObject<Block> CODE_STONE = reg("code_stone", () -> new CodeStoneBlock(unbreakable(MapColor.STONE).lightLevel(s -> 6)));
    public static final RegistryObject<Block> MAP_TABLE = reg("map_table", () -> new MapTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
            .strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> CODE_TERMINAL = reg("code_terminal", () -> new CodeTerminalBlock(unbreakable(MapColor.METAL)
            .sound(SoundType.METAL).lightLevel(s -> 10).noOcclusion()));
    public static final RegistryObject<Block> GRIEVER_HOLE = reg("griever_hole", () -> new GrieverHoleBlock(unbreakable(MapColor.COLOR_BLACK)
            .noOcclusion().lightLevel(s -> 3).sound(SoundType.AMETHYST)));

    private static <T extends Block> RegistryObject<T> reg(String name, Supplier<T> s) {
        RegistryObject<T> r = BLOCKS.register(name, s);
        ModItems.blockItem(name, r);
        return r;
    }
}
