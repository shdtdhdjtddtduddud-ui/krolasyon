package com.rabona.arena.registry;

import com.rabona.arena.RabonaArena;
import com.rabona.arena.block.ShapedBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RabonaArena.MODID);

    static BlockBehaviour.Properties grass() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.6f).sound(SoundType.GRASS);
    }

    static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2f).sound(SoundType.METAL).noOcclusion();
    }

    // koltuk: oturak + arkalik (kuzeye bakarken arkalik guneyde)
    static final double[][] SEAT = {{1, 0, 1, 15, 8, 15}, {1, 8, 12, 15, 18, 15}};
    static final double[][] PANEL = {{0, 0, 10, 16, 16, 16}};
    static final double[][] BOARD = {{0, 0, 6, 16, 14, 10}};

    public static final RegistryObject<Block> PITCH_GRASS = reg("pitch_grass", () -> new Block(grass()));
    public static final RegistryObject<Block> PITCH_GRASS_DARK = reg("pitch_grass_dark", () -> new Block(grass()));
    public static final RegistryObject<Block> PITCH_LINE = reg("pitch_line", () -> new Block(grass().mapColor(MapColor.SNOW)));
    public static final RegistryObject<Block> GOAL_POST = reg("goal_post", () -> new ShapedBlocks.GoalPost(metal()));
    public static final RegistryObject<Block> GOAL_NET = reg("goal_net", () -> new ShapedBlocks.Net(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.4f).sound(SoundType.WOOL).noOcclusion()
                    .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    public static final RegistryObject<Block> CORNER_FLAG = reg("corner_flag", () -> new ShapedBlocks.CornerFlag(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.3f).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> SEAT_RED = reg("seat_red", () -> new ShapedBlocks.Facing(seat(MapColor.COLOR_RED), SEAT));
    public static final RegistryObject<Block> SEAT_BLUE = reg("seat_blue", () -> new ShapedBlocks.Facing(seat(MapColor.COLOR_BLUE), SEAT));
    public static final RegistryObject<Block> SEAT_WHITE = reg("seat_white", () -> new ShapedBlocks.Facing(seat(MapColor.SNOW), SEAT));
    public static final RegistryObject<Block> SEAT_GOLD = reg("seat_gold", () -> new ShapedBlocks.Facing(seat(MapColor.GOLD), SEAT));
    public static final RegistryObject<Block> FLOODLIGHT = reg("floodlight", () -> new ShapedBlocks.Facing(
            metal().lightLevel(s -> 15), PANEL));
    public static final RegistryObject<Block> LED_BOARD = reg("led_board", () -> new ShapedBlocks.Facing(
            metal().lightLevel(s -> 11).mapColor(MapColor.COLOR_BLACK), BOARD));
    public static final RegistryObject<Block> STADIUM_CONCRETE = reg("stadium_concrete", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6f).sound(SoundType.STONE)));
    public static final RegistryObject<Block> STADIUM_STEP = reg("stadium_step", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.5f, 6f).sound(SoundType.STONE)));

    static BlockBehaviour.Properties seat(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(1f).sound(SoundType.METAL).noOcclusion();
    }

    private static RegistryObject<Block> reg(String n, Supplier<Block> s) {
        RegistryObject<Block> b = BLOCKS.register(n, s);
        ModItems.blockItem(n, b);
        return b;
    }
}
