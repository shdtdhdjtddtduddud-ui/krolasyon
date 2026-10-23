package com.krolasyon.realweather;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bir yerde hava cephesinin nasıl göründüğü: yağmur, kar ya da (kurak yerlerde) kum ve toz. */
public final class Climate {
    private Climate() {}

    public enum Kind { RAIN, SNOW, SAND, DRY }

    public static Kind kind(Level level, BlockPos pos) {
        Biome b = level.getBiome(pos).value();
        if (!b.hasPrecipitation()) {
            if (level.getBiome(pos).is(BiomeTags.IS_BADLANDS)) return Kind.SAND;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1;
            BlockState g = level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
            return g.is(Blocks.SAND) || g.is(Blocks.RED_SAND) || g.is(Blocks.SANDSTONE) || g.is(net.minecraft.tags.BlockTags.TERRACOTTA)
                    || b.getBaseTemperature() >= 1.9F ? Kind.SAND : Kind.DRY;
        }
        return b.coldEnoughToSnow(pos) ? Kind.SNOW : Kind.RAIN;
    }

    /** Gökyüzüne açık mı (yaprak altı ve çatı altı kapalı sayılır)? */
    public static boolean exposed(Level level, BlockPos pos) {
        return pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
    }

    /** Bataklık, nehir, mangrov gibi nemli yerler (sabah pusu). */
    public static boolean humid(Level level, BlockPos pos) {
        var h = level.getBiome(pos);
        return h.is(BiomeTags.IS_RIVER) || h.is(net.minecraft.world.level.biome.Biomes.SWAMP) || h.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)
                || h.is(BiomeTags.IS_JUNGLE) || h.is(BiomeTags.IS_BEACH);
    }
}
