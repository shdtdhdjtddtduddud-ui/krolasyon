package com.krolasyon.mythblades.world;

import com.krolasyon.mythblades.MythBlades;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Kor Sunağı: yosunlu kırık taştan basamaklı bir kaide ve ortasında, içine kılıç saplanmış düzgün taş blok.
 * Her overworld chunk'ında çalışır; sunak yalnızca adayın düştüğü chunk'ta kurulur.
 */
public final class AltarFeature extends Feature<NoneFeatureConfiguration> {
    public AltarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        if (server == null || !server.dimension().equals(Level.OVERWORLD)) return false;
        ChunkPos cp = new ChunkPos(ctx.origin());
        long seed = level.getSeed();
        int ci = Math.floorDiv(cp.getMiddleBlockX(), AltarLayout.CELL), cj = Math.floorDiv(cp.getMiddleBlockZ(), AltarLayout.CELL);
        BlockPos c = AltarLayout.cell(seed, ci, cj);
        if (c == null || (c.getX() >> 4) != cp.x || (c.getZ() >> 4) != cp.z) return false;
        if (!AltarLayout.valid(c, ctx.chunkGenerator(), server.getChunkSource().randomState(), level)) return false;
        build(level, c.getX(), AltarLayout.ground(ctx.chunkGenerator(), server.getChunkSource().randomState(), level, c.getX(), c.getZ()) - 1, c.getZ(), seed);
        return true;
    }

    private static double hash(long seed, int x, int z) {
        long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
        h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h & 0xffffff) / (double) 0x1000000;
    }

    /** y0: zemin bloğunun yüksekliği. Sunak bloğu y0 + 2'de. */
    public static void build(WorldGenLevel level, int cx, int y0, int cz, long seed) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // alanı temizle ve altını doldur
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz > 30) continue;
                for (int y = y0 + 1; y <= y0 + 14; y++) {
                    p.set(cx + dx, y, cz + dz);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && (s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)
                            || s.is(Blocks.SNOW))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                for (int y = y0; y >= y0 - 4; y--) {
                    p.set(cx + dx, y, cz + dz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced()) level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                    else break;
                }
            }
        }
        // zemin katı: yosunlu/kırık taş ve yosun öbekleri (düzensiz kenar)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double r = hash(seed, cx + dx, cz + dz);
                int d2 = dx * dx + dz * dz;
                if (d2 > 20 && r < 0.6) continue;
                if (d2 > 25) continue;
                p.set(cx + dx, y0, cz + dz);
                BlockState s = r < 0.3 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : r < 0.45 ? Blocks.MOSS_BLOCK.defaultBlockState()
                        : r < 0.6 ? Blocks.STONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
                level.setBlock(p, s, 2);
                if (d2 > 12 && r > 0.82) level.setBlock(p.above(), Blocks.MOSS_CARPET.defaultBlockState(), 2);
            }
        }
        // birinci basamak: taş levhalar ve iri bloklar
        BlockState slab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState cobSlab = Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState mossSlab = Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 11) continue;
                double r = hash(seed + 1, cx + dx, cz + dz);
                p.set(cx + dx, y0 + 1, cz + dz);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
                else if (d2 > 8 && r < 0.3) continue;
                else level.setBlock(p, r < 0.5 ? slab : r < 0.75 ? cobSlab : r < 0.9 ? mossSlab : Blocks.COBBLESTONE.defaultBlockState(), 2);
            }
        }
        // ikinci basamak: haç biçiminde levhalar, ortada kılıçlı sunak
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}}) {
            p.set(cx + d[0], y0 + 2, cz + d[1]);
            level.setBlock(p, slab, 2);
        }
        p.set(cx, y0 + 2, cz);
        level.setBlock(p, MythBlades.ALTAR.get().defaultBlockState(), 2);
    }
}
