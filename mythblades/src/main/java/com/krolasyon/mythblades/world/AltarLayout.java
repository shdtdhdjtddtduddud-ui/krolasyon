package com.krolasyon.mythblades.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kor Sunakları'nın yeri: dünya tohumundan ızgara hücrelerine dağılır (hücre başına en çok bir aday). Kor Mührü
 * aynı hesapla en yakın sunağı bulur.
 */
public final class AltarLayout {
    public static final int CELL = 720, MARGIN = 80;
    private static final Map<Long, Integer> GROUND = new ConcurrentHashMap<>();

    private AltarLayout() {}

    /** Hücredeki aday konum (y yok) ya da null. */
    @Nullable
    public static BlockPos cell(long seed, int ci, int cj) {
        Random r = new Random(seed * 668265263L + ci * 374761393L + cj * 1442695041L + 0xA17A5L);
        if (r.nextFloat() > 0.8F) return null;
        int x = ci * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        int z = cj * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        return new BlockPos(x, 0, z);
    }

    public static int ground(ChunkGenerator gen, RandomState rs, LevelHeightAccessor h, int x, int z) {
        long k = ((long) x << 32) ^ (z & 0xffffffffL);
        if (GROUND.size() > 4096) GROUND.clear();
        return GROUND.computeIfAbsent(k, q -> gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, h, rs));
    }

    /** Kara üzerinde, okyanus/nehir/sahil dışında mı? */
    public static boolean valid(BlockPos p, ChunkGenerator gen, RandomState rs, LevelHeightAccessor h) {
        Holder<Biome> b = gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(p.getX()), QuartPos.fromBlock(64), QuartPos.fromBlock(p.getZ()), rs.sampler());
        if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER) || b.is(BiomeTags.IS_BEACH) || b.is(BiomeTags.IS_DEEP_OCEAN)) return false;
        int y = ground(gen, rs, h, p.getX(), p.getZ());
        if (y <= gen.getSeaLevel() + 1) return false;
        // düzce olsun
        int max = y, min = y;
        for (int[] d : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            int q = ground(gen, rs, h, p.getX() + d[0], p.getZ() + d[1]);
            max = Math.max(max, q);
            min = Math.min(min, q);
        }
        return max - min <= 4;
    }

    /** En yakın geçerli sunak (yalnızca konum; y bilinmez). */
    @Nullable
    public static BlockPos nearest(long seed, ChunkGenerator gen, RandomState rs, LevelHeightAccessor h, int x, int z, int cells) {
        int ci0 = Math.floorDiv(x, CELL), cj0 = Math.floorDiv(z, CELL);
        BlockPos best = null;
        long bd = Long.MAX_VALUE;
        for (int ci = ci0 - cells; ci <= ci0 + cells; ci++) {
            for (int cj = cj0 - cells; cj <= cj0 + cells; cj++) {
                BlockPos p = cell(seed, ci, cj);
                if (p == null) continue;
                long dx = p.getX() - x, dz = p.getZ() - z, d = dx * dx + dz * dz;
                if (d >= bd || !valid(p, gen, rs, h)) continue;
                bd = d;
                best = p;
            }
        }
        return best;
    }
}
