package com.krolasyon.elvenrealm.world;

import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sylvaren kentlerinin yeri: dünya tohumundan ızgara hücrelerine dağılır. Uygun (ormanlık, düzce, kara) bir yerdeyse
 * kent kurulur; taban yüksekliği arazinin ortancasıdır.
 */
public final class CityLayout {
    public static final int CELL = 1100;
    /** Kentin kapladığı alan (merkeze göre): x ∈ [-MAX_X, MAX_X], z ∈ [MIN_Z, MAX_Z]. Dışında BLEND kadar yumuşak geçiş. */
    public static final int MAX_X = 60, MIN_Z = -118, MAX_Z = 48, BLEND = 20;
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, new ResourceLocation("elvenrealm", "has_elven_city"));

    public record City(long seed, int cx, int cz) {}

    private static final Map<Long, Integer> BASE = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> GROUND = new ConcurrentHashMap<>();

    private CityLayout() {}

    public static City cell(long worldSeed, int ci, int cj) {
        Random r = new Random(Noise.mix(worldSeed * 31337L + ci * 7368787L + cj * 2750159L + 1234L));
        if (r.nextDouble() > 0.7) return null;
        int margin = 220;
        int cx = ci * CELL + margin + r.nextInt(CELL - 2 * margin);
        int cz = cj * CELL + margin + r.nextInt(CELL - 2 * margin);
        return new City(r.nextLong(), cx, cz);
    }

    public static boolean biomeOk(ChunkGenerator gen, RandomState rs, int x, int z) {
        return gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(72), QuartPos.fromBlock(z), rs.sampler()).is(BIOMES);
    }

    public static int ground(ChunkGenerator gen, RandomState rs, LevelHeightAccessor h, int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        if (GROUND.size() > 40000) GROUND.clear();
        return GROUND.computeIfAbsent(key, k -> gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, h, rs));
    }

    /** Kentin taban yüksekliği (zemin bloğu); uygun değilse Integer.MIN_VALUE. */
    public static int base(City c, ChunkGenerator gen, RandomState rs, LevelHeightAccessor h) {
        return BASE.computeIfAbsent(c.seed(), k -> {
            if (!biomeOk(gen, rs, c.cx(), c.cz() - 30)) return Integer.MIN_VALUE;
            int ok = 0;
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                if (biomeOk(gen, rs, c.cx() + (int) (Math.cos(a) * 70), c.cz() - 30 + (int) (Math.sin(a) * 80))) ok++;
            }
            if (ok < 5) return Integer.MIN_VALUE;
            int[] hs = new int[25];
            int n = 0;
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    int x = c.cx() - MAX_X + i * (2 * MAX_X) / 4, z = c.cz() + MIN_Z + j * (MAX_Z - MIN_Z) / 4;
                    hs[n++] = ground(gen, rs, h, x, z) - 1;
                }
            }
            Arrays.sort(hs);
            int sea = gen.getSeaLevel();
            if (hs[3] <= sea) return Integer.MIN_VALUE;
            if (hs[21] - hs[3] > 28) return Integer.MIN_VALUE;
            int med = hs[12];
            return Math.max(sea + 3, Math.min(med, h.getMaxBuildHeight() - 140));
        });
    }

    public static boolean valid(City c, ChunkGenerator gen, RandomState rs, LevelHeightAccessor h) {
        return base(c, gen, rs, h) != Integer.MIN_VALUE;
    }

    public static City[] near(long worldSeed, int minX, int minZ, int maxX, int maxZ) {
        City[] out = new City[0];
        int pad = MAX_X + BLEND + 10;
        for (int ci = Math.floorDiv(minX - 250, CELL); ci <= Math.floorDiv(maxX + 250, CELL); ci++) {
            for (int cj = Math.floorDiv(minZ - 250, CELL); cj <= Math.floorDiv(maxZ + 250, CELL); cj++) {
                City c = cell(worldSeed, ci, cj);
                if (c == null) continue;
                if (c.cx() + pad >= minX && c.cx() - pad <= maxX && c.cz() + MIN_Z - BLEND - 10 <= maxZ && c.cz() + MAX_Z + 80 >= minZ) {
                    out = Arrays.copyOf(out, out.length + 1);
                    out[out.length - 1] = c;
                }
            }
        }
        return out;
    }

    public static City nearest(long worldSeed, ChunkGenerator gen, RandomState rs, LevelHeightAccessor h, int x, int z, int cells) {
        City best = null;
        long bd = Long.MAX_VALUE;
        int ci0 = Math.floorDiv(x, CELL), cj0 = Math.floorDiv(z, CELL);
        for (int ci = ci0 - cells; ci <= ci0 + cells; ci++) {
            for (int cj = cj0 - cells; cj <= cj0 + cells; cj++) {
                City c = cell(worldSeed, ci, cj);
                if (c == null) continue;
                long dx = c.cx() - x, dz = c.cz() - z, d = dx * dx + dz * dz;
                if (d >= bd || !valid(c, gen, rs, h)) continue;
                bd = d;
                best = c;
            }
        }
        return best;
    }

    /** Oyuncu kentin içinde mi? */
    public static boolean inside(City c, int x, int z) {
        int dx = x - c.cx(), dz = z - c.cz();
        return Math.abs(dx) <= MAX_X + 6 && dz >= MIN_Z && dz <= MAX_Z + 20;
    }
}
