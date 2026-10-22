package com.krolasyon.smoothterrain;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;

/**
 * Her chunk'ta, yüzeyin en son işlenen adımında çalışır. Bir sütunun komşularından biri (çaprazlar dahil) bir ya da birkaç
 * blok yüksekse, o sütunun üstüne yüzey bloğuna uyan yarım blok konur. Böylece bir blokluk basamaklar yarım yarım çıkılır.
 */
public final class SmoothFeature extends Feature<NoneFeatureConfiguration> {
    private static final int NONE = Integer.MIN_VALUE;
    private static final int[][] NB = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public SmoothFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Yüzey bloğuna uyan yarım blok (null: bu yüzey düzeltilmez). */
    @Nullable
    static BlockState slabFor(BlockState top) {
        Block b = top.getBlock();
        Block s = null;
        if (b == Blocks.GRASS_BLOCK) s = SmoothTerrain.GRASS_SLAB.get();
        else if (b == Blocks.DIRT || b == Blocks.ROOTED_DIRT || b == Blocks.MYCELIUM) s = SmoothTerrain.DIRT_SLAB.get();
        else if (b == Blocks.COARSE_DIRT) s = SmoothTerrain.COARSE_DIRT_SLAB.get();
        else if (b == Blocks.PODZOL) s = SmoothTerrain.PODZOL_SLAB.get();
        else if (b == Blocks.SAND) s = SmoothTerrain.SAND_SLAB.get();
        else if (b == Blocks.RED_SAND) s = SmoothTerrain.RED_SAND_SLAB.get();
        else if (b == Blocks.GRAVEL) s = SmoothTerrain.GRAVEL_SLAB.get();
        else if (b == Blocks.STONE) s = Blocks.STONE_SLAB;
        else if (b == Blocks.ANDESITE) s = Blocks.ANDESITE_SLAB;
        else if (b == Blocks.GRANITE) s = Blocks.GRANITE_SLAB;
        else if (b == Blocks.DIORITE) s = Blocks.DIORITE_SLAB;
        else if (b == Blocks.SANDSTONE) s = Blocks.SANDSTONE_SLAB;
        else if (b == Blocks.RED_SANDSTONE) s = Blocks.RED_SANDSTONE_SLAB;
        else if (b == Blocks.DEEPSLATE) s = Blocks.COBBLED_DEEPSLATE_SLAB;
        else if (b == Blocks.SNOW_BLOCK || b == Blocks.POWDER_SNOW) return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4);
        if (s == null) return null;
        return s.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    /** Arazinin üst bloğunun yüksekliği (bitki, yaprak, kütük ve kar katmanları atlanır); suyla kaplıysa NONE. */
    static int height(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        for (int y = top + 2; y >= top - 10; y--) {
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            if (!s.getFluidState().isEmpty()) return NONE;
            if (s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(Blocks.SNOW)) continue;
            return y;
        }
        return NONE;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos cp = new ChunkPos(ctx.origin());
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        // 18x18 yükseklik ızgarası (çevredeki bir sıra komşu chunk'lardan)
        int[][] h = new int[18][18];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 18; i++) for (int j = 0; j < 18; j++) h[i][j] = height(level, p, x0 + i - 1, z0 + j - 1);
        int placed = 0;
        for (int i = 1; i <= 16; i++) {
            for (int j = 1; j <= 16; j++) {
                int y = h[i][j];
                if (y == NONE) continue;
                boolean step = false;
                for (int[] d : NB) {
                    int n = h[i + d[0]][j + d[1]];
                    if (n == NONE) continue;
                    // köşegende yalnızca tek bloklu basamak (sivri köşeler yuvarlansın, uçurum dibine yığılmasın)
                    if (d[0] != 0 && d[1] != 0 ? n == y + 1 : n >= y + 1 && n <= y + 3) {
                        step = true;
                        break;
                    }
                }
                if (!step) continue;
                int x = x0 + i - 1, z = z0 + j - 1;
                p.set(x, y, z);
                BlockState top = level.getBlockState(p);
                p.set(x, y + 1, z);
                BlockState above = level.getBlockState(p);
                if (!above.isAir() && !(above.canBeReplaced() && above.getFluidState().isEmpty())) continue;
                // kar katmanı varsa yarım kat kar
                BlockState put = above.is(Blocks.SNOW) ? Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4) : slabFor(top);
                if (put == null) continue;
                p.set(x, y + 2, z);
                BlockState over = level.getBlockState(p);
                if (!over.isAir() && !over.canBeReplaced() && !over.is(BlockTags.LEAVES)) continue;
                if (over.is(BlockTags.REPLACEABLE_BY_TREES) || over.canBeReplaced()) {
                    // iki katlı bitkinin üst yarısı havada kalmasın
                    if (over.getBlock() instanceof net.minecraft.world.level.block.DoublePlantBlock) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                p.set(x, y + 1, z);
                level.setBlock(p, put, 2);
                if (top.is(Blocks.GRASS_BLOCK) && !put.is(Blocks.SNOW)) {
                    p.set(x, y, z);
                    level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                }
                placed++;
            }
        }
        return placed > 0;
    }
}
