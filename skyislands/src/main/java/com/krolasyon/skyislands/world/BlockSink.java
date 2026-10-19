package com.krolasyon.skyislands.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluids;

/** Where island blocks go: a single chunk during world generation, or the live level for the Island Seed. */
public final class BlockSink {
    private final WorldGenLevel level;
    private final ChunkGenerator generator;
    private final int minX, minZ, maxX, maxZ;
    private final int flags;

    public BlockSink(WorldGenLevel level, ChunkGenerator generator, int minX, int minZ, int maxX, int maxZ, int flags) {
        this.level = level;
        this.generator = generator;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.flags = flags;
    }

    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxZ() { return maxZ; }
    public int minY() { return level.getMinBuildHeight(); }
    public int maxY() { return level.getMaxBuildHeight() - 1; }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean contains(BlockPos p) {
        return contains(p.getX(), p.getZ()) && p.getY() >= minY() && p.getY() <= maxY();
    }

    public BlockState get(BlockPos p) {
        return level.getBlockState(p);
    }

    public void set(BlockPos p, BlockState s) {
        if (contains(p)) level.setBlock(p, s, flags);
    }

    public boolean isAir(BlockPos p) {
        return contains(p) && level.getBlockState(p).isAir();
    }

    public void water(BlockPos p) {
        if (!contains(p)) return;
        level.setBlock(p, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), flags);
        level.scheduleTick(p, Fluids.WATER, 1);
    }

    public void tree(ResourceKey<ConfiguredFeature<?, ?>> key, BlockPos pos, RandomSource random) {
        if (!contains(pos)) return;
        level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(key)
                .ifPresent(h -> h.value().place(level, generator, random, pos));
    }
}
