package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.registry.ModBlocks;
import com.krolasyon.sololeveling.system.Rank;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Look and inhabitants of a dungeon instance. */
public enum DungeonTheme {
    GOBLIN_CAVE(Style.CAVE, List.of(MobKind.GOBLIN, MobKind.GOBLIN, MobKind.HOBGOBLIN), MobKind.HOBGOBLIN, true,
            () -> Blocks.STONE, () -> Blocks.COBBLESTONE, () -> Blocks.MOSSY_COBBLESTONE, () -> Blocks.LANTERN, () -> Blocks.COARSE_DIRT),
    LYCAN_FOREST(Style.FOREST, List.of(MobKind.STEEL_FANGED_LYCAN), MobKind.STEEL_FANGED_LYCAN, true,
            () -> Blocks.SPRUCE_LOG, () -> Blocks.PODZOL, () -> Blocks.MOSS_BLOCK, () -> Blocks.SHROOMLIGHT, () -> Blocks.SPRUCE_LEAVES),
    KASAKA_LAIR(Style.CAVE, List.of(MobKind.GIANT_CENTIPEDE, MobKind.GOBLIN), MobKind.KASAKA, false,
            () -> Blocks.DEEPSLATE, () -> Blocks.MUD, () -> Blocks.MOSSY_COBBLESTONE, () -> Blocks.OCHRE_FROGLIGHT, () -> Blocks.CLAY),
    STATUE_HALL(Style.TEMPLE, List.of(MobKind.STONE_STATUE), MobKind.STONE_STATUE, true,
            () -> ModBlocks.TEMPLE_STONE.get(), () -> Blocks.SMOOTH_SANDSTONE, () -> ModBlocks.TEMPLE_PILLAR.get(), () -> Blocks.SEA_LANTERN, () -> Blocks.CHISELED_SANDSTONE),
    KNIGHT_CASTLE(Style.HALLS, List.of(MobKind.CASTLE_KNIGHT), MobKind.CASTLE_KNIGHT, true,
            () -> Blocks.STONE_BRICKS, () -> Blocks.POLISHED_ANDESITE, () -> Blocks.CRACKED_STONE_BRICKS, () -> Blocks.SOUL_LANTERN, () -> Blocks.RED_CARPET),
    ICE_FOREST(Style.FOREST, List.of(MobKind.ICE_ELF, MobKind.ICE_ELF, MobKind.ICE_BEAR), MobKind.BARUKA, false,
            () -> Blocks.PACKED_ICE, () -> Blocks.SNOW_BLOCK, () -> Blocks.BLUE_ICE, () -> Blocks.SEA_LANTERN, () -> Blocks.SPRUCE_LEAVES),
    ORC_FORTRESS(Style.HALLS, List.of(MobKind.HIGH_ORC), MobKind.KARGALGAN, false,
            () -> Blocks.DARK_OAK_PLANKS, () -> Blocks.PACKED_MUD, () -> Blocks.MUD_BRICKS, () -> Blocks.CAMPFIRE, () -> Blocks.HAY_BLOCK),
    DEMON_RUINS(Style.HALLS, List.of(MobKind.DEMON), MobKind.CERBERUS, false,
            () -> ModBlocks.DEMON_BRICKS.get(), () -> Blocks.BLACKSTONE, () -> Blocks.CRIMSON_NYLIUM, () -> Blocks.SHROOMLIGHT, () -> Blocks.MAGMA_BLOCK),
    ANT_NEST(Style.NEST, List.of(MobKind.ANT_SOLDIER), MobKind.ANT_SOLDIER, true,
            () -> ModBlocks.HIVE_WALL.get(), () -> Blocks.PACKED_MUD, () -> Blocks.MUD, () -> Blocks.PEARLESCENT_FROGLIGHT, () -> Blocks.ROOTED_DIRT);

    public enum Style { CAVE, FOREST, TEMPLE, HALLS, NEST }

    public final Style style;
    public final List<MobKind> mobs;
    public final MobKind boss;
    /** The boss is an elite version of a normal monster. */
    public final boolean eliteBoss;
    public final Supplier<Block> wall, floor, accent, light, deco;

    DungeonTheme(Style style, List<MobKind> mobs, MobKind boss, boolean eliteBoss, Supplier<Block> wall, Supplier<Block> floor,
                 Supplier<Block> accent, Supplier<Block> light, Supplier<Block> deco) {
        this.style = style;
        this.mobs = mobs;
        this.boss = boss;
        this.eliteBoss = eliteBoss;
        this.wall = wall;
        this.floor = floor;
        this.accent = accent;
        this.light = light;
        this.deco = deco;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static DungeonTheme byOrdinal(int o) {
        DungeonTheme[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }

    public static DungeonTheme pick(Rank rank, boolean red, RandomSource r) {
        if (red) return r.nextBoolean() ? ICE_FOREST : KNIGHT_CASTLE;
        return switch (rank) {
            case E -> GOBLIN_CAVE;
            case D -> r.nextBoolean() ? LYCAN_FOREST : GOBLIN_CAVE;
            case C -> r.nextBoolean() ? KASAKA_LAIR : STATUE_HALL;
            case B -> r.nextBoolean() ? KNIGHT_CASTLE : ICE_FOREST;
            case A -> ORC_FORTRESS;
            default -> r.nextBoolean() ? DEMON_RUINS : ANT_NEST;
        };
    }
}
