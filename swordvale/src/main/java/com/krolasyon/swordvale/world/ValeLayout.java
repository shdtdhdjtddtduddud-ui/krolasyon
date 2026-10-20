package com.krolasyon.swordvale.world;

import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kılıçlar Vadisi yerleşimi. Dünya tohumundan, ızgara hücrelerine dağılmış vadiler ve her vadinin içindeki
 * kılıçlar türetilir; her chunk aynı sonuca bağımsız olarak ulaşır.
 */
public final class ValeLayout {
    public static final int CELL = 720;
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, new ResourceLocation("swordvale", "has_sword_vale"));

    public record Vale(long seed, int cx, int cz, int radius) {}

    /** Bir kılıç: taban (x, z), uzunluk, bıçak genişliği, eğim, eğim yönü, bıçak dönüşü, mezar sandığı. */
    public record Sword(long seed, int x, int z, float length, float width, float lean, float leanDir, float twist, boolean grave) {
        public int reach() {
            return (int) Math.ceil(length * Math.sin(lean) + length * 0.2F + width) + 2;
        }
    }

    private static final Map<Long, Boolean> VALID = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> GROUND = new ConcurrentHashMap<>();

    private ValeLayout() {}

    public static Vale cell(long worldSeed, int ci, int cj) {
        Random r = new Random(Noise.mix(worldSeed * 7919L + ci * 6542989L + cj * 1299827L + 77L));
        if (r.nextDouble() > 0.55) return null;
        int radius = 110 + r.nextInt(70);
        int margin = radius + 20;
        int cx = ci * CELL + margin + r.nextInt(CELL - 2 * margin);
        int cz = cj * CELL + margin + r.nextInt(CELL - 2 * margin);
        return new Vale(r.nextLong(), cx, cz, radius);
    }

    public static boolean biomeOk(ChunkGenerator gen, RandomState rs, int x, int z) {
        return gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(72), QuartPos.fromBlock(z), rs.sampler()).is(BIOMES);
    }

    /** Vadi merkezinin ve çevresinin çoğu uygun biyomdaysa geçerlidir. */
    public static boolean valid(Vale v, ChunkGenerator gen, RandomState rs) {
        long key = Noise.mix(v.seed());
        return VALID.computeIfAbsent(key, k -> {
            if (!biomeOk(gen, rs, v.cx(), v.cz())) return false;
            int ok = 0;
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                if (biomeOk(gen, rs, v.cx() + (int) (Math.cos(a) * v.radius() * 0.6), v.cz() + (int) (Math.sin(a) * v.radius() * 0.6))) ok++;
            }
            return ok >= 4;
        });
    }

    public static int ground(ChunkGenerator gen, RandomState rs, LevelHeightAccessor h, int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        if (GROUND.size() > 20000) GROUND.clear();
        return GROUND.computeIfAbsent(key, k -> gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, h, rs));
    }

    /** Vadinin içinde mi? (yumuşak kenarlı) */
    public static double influence(Vale v, int x, int z) {
        double dx = x - v.cx(), dz = z - v.cz();
        double d = Math.sqrt(dx * dx + dz * dz) / v.radius();
        return d >= 1 ? 0 : 1 - d * d;
    }

    private static final Map<Long, List<Sword>> SWORDS = new ConcurrentHashMap<>();

    public static List<Sword> swords(Vale v) {
        if (SWORDS.size() > 64) SWORDS.clear();
        return SWORDS.computeIfAbsent(v.seed(), k -> computeSwords(v));
    }

    private static List<Sword> computeSwords(Vale v) {
        List<Sword> out = new ArrayList<>();
        int step = 22;
        int n = v.radius() / step + 1;
        for (int i = -n; i <= n; i++) {
            for (int j = -n; j <= n; j++) {
                Random r = new Random(Noise.mix(v.seed() + i * 92821L + j * 68917L));
                int x = v.cx() + i * step + r.nextInt(step - 4) - (step - 4) / 2;
                int z = v.cz() + j * step + r.nextInt(step - 4) - (step - 4) / 2;
                double inf = influence(v, x, z);
                if (r.nextDouble() > inf * 0.95) continue;
                double roll = r.nextDouble();
                float length, lean;
                if (roll < 0.14 && inf > 0.25) {
                    length = 44 + r.nextFloat() * 18;
                    lean = (float) Math.toRadians(r.nextFloat() * 14);
                } else if (roll < 0.5) {
                    length = 22 + r.nextFloat() * 12;
                    lean = (float) Math.toRadians(r.nextFloat() * 22);
                } else {
                    length = 11 + r.nextFloat() * 8;
                    lean = (float) Math.toRadians(r.nextFloat() < 0.15 ? 62 + r.nextFloat() * 18 : r.nextFloat() * 30);
                }
                float width = Math.max(1.6F, length * 0.105F);
                float leanDir = r.nextFloat() * (float) (Math.PI * 2);
                float twist = r.nextFloat() * (float) Math.PI;
                boolean grave = length > 40 && r.nextFloat() < 0.45F;
                out.add(new Sword(r.nextLong(), x, z, length, width, lean, leanDir, twist, grave));
            }
        }
        return out;
    }

    /** Bir dikdörtgene yakın tüm vadiler. */
    public static List<Vale> valesNear(long worldSeed, int minX, int minZ, int maxX, int maxZ) {
        List<Vale> out = new ArrayList<>();
        for (int ci = Math.floorDiv(minX - 260, CELL); ci <= Math.floorDiv(maxX + 260, CELL); ci++) {
            for (int cj = Math.floorDiv(minZ - 260, CELL); cj <= Math.floorDiv(maxZ + 260, CELL); cj++) {
                Vale v = cell(worldSeed, ci, cj);
                if (v == null) continue;
                int reach = v.radius() + 60;
                if (v.cx() + reach >= minX && v.cx() - reach <= maxX && v.cz() + reach >= minZ && v.cz() - reach <= maxZ) out.add(v);
            }
        }
        return out;
    }

    /** En yakın geçerli vadi (pusula ve giriş başlığı için). */
    public static Vale nearest(long worldSeed, ChunkGenerator gen, RandomState rs, int x, int z, int cells) {
        Vale best = null;
        long bd = Long.MAX_VALUE;
        int ci0 = Math.floorDiv(x, CELL), cj0 = Math.floorDiv(z, CELL);
        for (int ci = ci0 - cells; ci <= ci0 + cells; ci++) {
            for (int cj = cj0 - cells; cj <= cj0 + cells; cj++) {
                Vale v = cell(worldSeed, ci, cj);
                if (v == null || !valid(v, gen, rs)) continue;
                long dx = v.cx() - x, dz = v.cz() - z, d = dx * dx + dz * dz;
                if (d < bd) {
                    bd = d;
                    best = v;
                }
            }
        }
        return best;
    }
}
