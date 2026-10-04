package com.krolasyon.bosses.rpg.town;

import com.krolasyon.bosses.rpg.world.Kingdom;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Building materials of a culture. */
public record Palette(Block foundation, Block wall, Block frame, Block floor, Block roof, Block roofSlab, Block window, Block door,
                      Block fence, Block path, Block plaza, Block light, Block accent, Block cityWall, Block wallTop, Block carpet,
                      Block bed, Block leaves, int wallHeight, int doorHeight) {

    public BlockState s(Block b) { return b.defaultBlockState(); }

    public static Palette of(int kingdom) {
        if (kingdom < 0) return BANDIT;
        return switch (Kingdom.of(kingdom)) {
            case ALDORIA -> ALDORIA;
            case SYLVARIEN -> ELF;
            case KHAZDUR -> DWARF;
            case INFERNAX -> DEMON;
            case YMIRHEIM -> GIANT;
            case GORMASH -> ORC;
            case FELARIS -> BEAST;
            case VALDREN -> VALDREN;
            case MERIDIA -> MERIDIA;
            case NOCTHERA -> DARK_ELF;
        };
    }

    public static final Palette ALDORIA = new Palette(Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.STRIPPED_OAK_LOG, Blocks.SPRUCE_PLANKS,
            Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.GLASS_PANE, Blocks.OAK_DOOR, Blocks.OAK_FENCE, Blocks.GRAVEL,
            Blocks.STONE_BRICKS, Blocks.LANTERN, Blocks.YELLOW_WOOL, Blocks.STONE_BRICKS, Blocks.STONE_BRICK_WALL, Blocks.BLUE_CARPET,
            Blocks.BLUE_BED, Blocks.OAK_LEAVES, 4, 2);
    public static final Palette ELF = new Palette(Blocks.MOSSY_STONE_BRICKS, Blocks.BIRCH_PLANKS, Blocks.STRIPPED_BIRCH_LOG, Blocks.MOSSY_STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.LIME_STAINED_GLASS_PANE, Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE,
            Blocks.MOSS_BLOCK, Blocks.CALCITE, Blocks.SHROOMLIGHT, Blocks.WHITE_WOOL, Blocks.STRIPPED_BIRCH_LOG, Blocks.AZALEA_LEAVES, Blocks.GREEN_CARPET,
            Blocks.LIME_BED, Blocks.FLOWERING_AZALEA_LEAVES, 5, 2);
    public static final Palette DWARF = new Palette(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_BASALT, Blocks.POLISHED_DEEPSLATE,
            Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.IRON_DOOR, Blocks.DEEPSLATE_BRICK_WALL,
            Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE, Blocks.LANTERN, Blocks.GOLD_BLOCK, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_WALL,
            Blocks.RED_CARPET, Blocks.RED_BED, Blocks.AIR, 4, 2);
    public static final Palette DEMON = new Palette(Blocks.BLACKSTONE, Blocks.NETHER_BRICKS, Blocks.CRIMSON_STEM, Blocks.POLISHED_BLACKSTONE,
            Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICK_SLAB, Blocks.RED_STAINED_GLASS_PANE, Blocks.CRIMSON_DOOR, Blocks.NETHER_BRICK_FENCE,
            Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.SOUL_LANTERN, Blocks.MAGMA_BLOCK, Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.BLACKSTONE_WALL, Blocks.BLACK_CARPET, Blocks.BLACK_BED, Blocks.NETHER_WART_BLOCK, 5, 3);
    public static final Palette GIANT = new Palette(Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS,
            Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE,
            Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.LANTERN, Blocks.WHITE_WOOL, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL, Blocks.WHITE_CARPET,
            Blocks.WHITE_BED, Blocks.SPRUCE_LEAVES, 8, 4);
    public static final Palette ORC = new Palette(Blocks.MUD_BRICKS, Blocks.MUD_BRICKS, Blocks.DARK_OAK_LOG, Blocks.COARSE_DIRT,
            Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.AIR, Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_FENCE, Blocks.COARSE_DIRT,
            Blocks.PACKED_MUD, Blocks.LANTERN, Blocks.BONE_BLOCK, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_FENCE, Blocks.BROWN_CARPET,
            Blocks.BROWN_BED, Blocks.AIR, 5, 2);
    public static final Palette BEAST = new Palette(Blocks.MOSSY_COBBLESTONE, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_LOG, Blocks.JUNGLE_PLANKS,
            Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB, Blocks.AIR, Blocks.JUNGLE_DOOR, Blocks.BAMBOO_FENCE, Blocks.DIRT_PATH,
            Blocks.MUD_BRICKS, Blocks.LANTERN, Blocks.HAY_BLOCK, Blocks.BAMBOO_BLOCK, Blocks.BAMBOO_FENCE, Blocks.ORANGE_CARPET, Blocks.ORANGE_BED,
            Blocks.JUNGLE_LEAVES, 4, 2);
    public static final Palette VALDREN = new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS,
            Blocks.BLACKSTONE_STAIRS, Blocks.BLACKSTONE_SLAB, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_FENCE,
            Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.LANTERN, Blocks.RED_WOOL, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_WALL,
            Blocks.RED_CARPET, Blocks.RED_BED, Blocks.DARK_OAK_LEAVES, 4, 2);
    public static final Palette MERIDIA = new Palette(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.STRIPPED_ACACIA_LOG, Blocks.SMOOTH_QUARTZ,
            Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICK_SLAB, Blocks.GLASS_PANE, Blocks.ACACIA_DOOR, Blocks.ACACIA_FENCE, Blocks.SMOOTH_SANDSTONE,
            Blocks.SMOOTH_SANDSTONE, Blocks.LANTERN, Blocks.CYAN_WOOL, Blocks.SANDSTONE, Blocks.SANDSTONE_WALL, Blocks.CYAN_CARPET, Blocks.CYAN_BED,
            Blocks.ACACIA_LEAVES, 4, 2);
    public static final Palette DARK_ELF = new Palette(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.PURPUR_PILLAR, Blocks.POLISHED_BLACKSTONE,
            Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_SLAB, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.WARPED_DOOR, Blocks.WARPED_FENCE,
            Blocks.DEEPSLATE_TILES, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.SOUL_LANTERN, Blocks.AMETHYST_BLOCK, Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.PURPLE_CARPET, Blocks.PURPLE_BED, Blocks.DARK_OAK_LEAVES, 5, 2);
    public static final Palette BANDIT = new Palette(Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.COARSE_DIRT,
            Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.AIR, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT,
            Blocks.LANTERN, Blocks.BLACK_WOOL, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_FENCE, Blocks.BLACK_CARPET, Blocks.BROWN_BED, Blocks.AIR, 3, 2);
    public static final Palette SLUM = new Palette(Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.COARSE_DIRT,
            Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.AIR, Blocks.OAK_DOOR, Blocks.OAK_FENCE, Blocks.MUD, Blocks.COARSE_DIRT,
            Blocks.LANTERN, Blocks.BROWN_WOOL, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL, Blocks.BROWN_CARPET, Blocks.BROWN_BED, Blocks.AIR, 3, 2);
}
