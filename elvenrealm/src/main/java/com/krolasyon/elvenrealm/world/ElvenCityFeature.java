package com.krolasyon.elvenrealm.world;

import com.krolasyon.elvenrealm.ElvenRealm;
import com.krolasyon.elvenrealm.entity.ElfEntity;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Her overworld chunk'ında çalışır: Sylvaren kentinin bu chunk'a düşen arazisini, yapılarını, ağaçlarını ve sakinlerini üretir. */
public final class ElvenCityFeature extends Feature<NoneFeatureConfiguration> {
    private static final Map<Long, CityPlan> PLANS = new ConcurrentHashMap<>();

    public ElvenCityFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public static CityPlan plan(CityLayout.City c, int y0) {
        if (PLANS.size() > 8) PLANS.clear();
        return PLANS.computeIfAbsent(c.seed(), k -> new CityPlan(c, y0));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        if (server == null || !server.dimension().equals(Level.OVERWORLD)) return false;
        ChunkGenerator gen = ctx.chunkGenerator();
        RandomState rs = server.getChunkSource().randomState();
        ChunkPos cp = new ChunkPos(ctx.origin());
        int minX = cp.getMinBlockX(), minZ = cp.getMinBlockZ(), maxX = cp.getMaxBlockX(), maxZ = cp.getMaxBlockZ();
        boolean any = false;
        for (CityLayout.City c : CityLayout.near(level.getSeed(), minX, minZ, maxX, maxZ)) {
            int y0 = CityLayout.base(c, gen, rs, level);
            if (y0 == Integer.MIN_VALUE) continue;
            CityPlan plan = plan(c, y0);
            terrain(level, gen, rs, plan, minX, minZ, maxX, maxZ);
            clearNatural(level, plan, cp);
            path(level, plan, minX, minZ, maxX, maxZ);
            blocks(level, plan, cp);
            for (GiantTree t : plan.trees) if (t.overlaps(minX, minZ, maxX, maxZ)) tree(level, t, minX, minZ, maxX, maxZ);
            flora(level, plan, minX, minZ, maxX, maxZ);
            spawn(level, server, plan, cp);
            any = true;
        }
        return any;
    }

    /** Arazi: kent alanında teras seviyelerine düzle, çevrede yumuşakça karıştır, üstteki ağaç/bitkileri temizle. */
    private static void terrain(WorldGenLevel level, ChunkGenerator gen, RandomState rs, CityPlan plan, int minX, int minZ, int maxX, int maxZ) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState(), stone = Blocks.STONE.defaultBlockState();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double w = plan.weight(x, z);
                if (w <= 0) continue;
                boolean in = plan.footprint(x, z);
                int h = CityLayout.ground(gen, rs, level, x, z) - 1;
                int t = plan.target(x, z);
                int d = in ? t : (int) Math.round(h + (t - h) * w);
                if (d > h) {
                    for (int y = h + 1; y <= d; y++) {
                        p.set(x, y, z);
                        level.setBlock(p, y == d ? CityPlan.grass() : y >= d - 3 ? CityPlan.dirt() : stone, 2);
                    }
                    p.set(x, h, z);
                    if (level.getBlockState(p).is(Blocks.GRASS_BLOCK)) level.setBlock(p, CityPlan.dirt(), 2);
                }
                int clearTop = Math.max(h, d) + (in || w > 0.5 ? 40 : 12);
                for (int y = d + 1; y <= clearTop; y++) {
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir()) level.setBlock(p, air, 2);
                }
                p.set(x, d, z);
                BlockState top = level.getBlockState(p);
                if (top.is(Blocks.DIRT) || top.is(Blocks.STONE) || top.is(Blocks.COARSE_DIRT) || top.is(Blocks.PODZOL) || top.isAir()
                        || top.is(Blocks.SAND) || top.is(Blocks.GRAVEL) || !top.getFluidState().isEmpty()) {
                    level.setBlock(p, CityPlan.grass(), 2);
                }
            }
        }
    }

    /**
     * Komşu chunk'lardaki doğal ağaçlar sonradan kentin içine taşabilir; bu chunk ve çevresindeki 8 chunk'ta kent alanına
     * düşen doğal yaprak, kütük ve mantarları temizler (kentin kendi blokları kalıcı yaprak ve soyulmuş kütük kullanır).
     */
    private static void clearNatural(WorldGenLevel level, CityPlan plan, ChunkPos cp) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = cp.getMinBlockX() - 16; x <= cp.getMaxBlockX() + 16; x++) {
            for (int z = cp.getMinBlockZ() - 16; z <= cp.getMaxBlockZ() + 16; z++) {
                if (plan.weight(x, z) < 0.6) continue;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                int t = plan.target(x, z);
                for (int y = t - 2; y <= t + 45; y++) {
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;
                    if (natural(s)) level.setBlock(p, air, 2);
                }
            }
        }
    }

    private static boolean natural(BlockState s) {
        if (s.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) return !s.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT);
        if (s.is(Blocks.OAK_LOG) || s.is(Blocks.BIRCH_LOG) || s.is(Blocks.DARK_OAK_LOG) || s.is(Blocks.SPRUCE_LOG) || s.is(Blocks.CHERRY_LOG)
                || s.is(Blocks.JUNGLE_LOG) || s.is(Blocks.ACACIA_LOG) || s.is(Blocks.MANGROVE_LOG)) return true;
        return s.is(Blocks.BROWN_MUSHROOM_BLOCK) || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.MUSHROOM_STEM) || s.is(Blocks.BEE_NEST);
    }

    /** Kapıdan güneye uzanan, fenerli toprak yol. */
    private static void path(WorldGenLevel level, CityPlan plan, int minX, int minZ, int maxX, int maxZ) {
        int z0 = plan.cz + 46, z1 = plan.cz + 130;
        if (maxZ < z0 || minZ > z1) return;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int z = Math.max(minZ, z0); z <= Math.min(maxZ, z1); z++) {
            double wig = Math.sin((z - z0) * 0.06) * 4 * Math.min(1, (z - z0) / 30.0);
            int center = plan.cx + (int) Math.round(wig);
            for (int x = Math.max(minX, center - 3); x <= Math.min(maxX, center + 3); x++) {
                int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                p.set(x, top, z);
                BlockState s = level.getBlockState(p);
                while (top > level.getMinBuildHeight() && (s.is(net.minecraft.tags.BlockTags.LEAVES) || s.is(net.minecraft.tags.BlockTags.LOGS)
                        || s.canBeReplaced() && !s.isAir())) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    top--;
                    p.set(x, top, z);
                    s = level.getBlockState(p);
                }
                if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.COARSE_DIRT)) {
                    boolean edge = Math.abs(x - center) == 3;
                    double r = Noise.hash(plan.city.seed() + 77, x, 0, z);
                    if (!edge || r < 0.5) level.setBlock(p, r < 0.1 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState(), 2);
                }
            }
            // fener direği (yolun doğusunda)
            int lx = center + 4;
            if ((z - z0) % 12 == 6 && lx >= minX && lx <= maxX) {
                int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, lx, z);
                p.set(lx, top, z);
                level.setBlock(p, Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                level.setBlock(p.above(), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                level.setBlock(p.above(2), Blocks.LANTERN.defaultBlockState(), 2);
            }
        }
    }

    private static void blocks(WorldGenLevel level, CityPlan plan, ChunkPos cp) {
        Map<Long, BlockState> m = plan.chunk(cp.x, cp.z);
        if (m == null) return;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Map.Entry<Long, BlockState> e : m.entrySet()) {
            long k = e.getKey();
            p.set(CityPlan.unpackX(k), CityPlan.unpackY(k), CityPlan.unpackZ(k));
            if (p.getY() < level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight()) continue;
            level.setBlock(p, e.getValue(), 2);
        }
    }

    private static void tree(WorldGenLevel level, GiantTree t, int minX, int minZ, int maxX, int maxZ) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int y0 = Math.max(level.getMinBuildHeight(), t.minY), y1 = Math.min(level.getMaxBuildHeight() - 1, t.maxY);
        for (int x = Math.max(minX, t.minX); x <= Math.min(maxX, t.maxX); x++) {
            for (int z = Math.max(minZ, t.minZ); z <= Math.min(maxZ, t.maxZ); z++) {
                for (int y = y0; y <= y1; y++) {
                    BlockState s = t.blockAt(x, y, z);
                    if (s == null) continue;
                    p.set(x, y, z);
                    BlockState cur = level.getBlockState(p);
                    boolean wood = s.is(Blocks.OAK_WOOD) || s.is(Blocks.MOSS_BLOCK);
                    if (wood) {
                        if (cur.is(Blocks.STONE_BRICKS) || cur.is(Blocks.MOSSY_STONE_BRICKS) || cur.is(Blocks.CRACKED_STONE_BRICKS)) continue;
                        level.setBlock(p, s, 2);
                    } else if (cur.isAir() || cur.is(Blocks.GRASS) || cur.is(Blocks.TALL_GRASS)) {
                        level.setBlock(p, s, 2);
                    }
                }
            }
        }
    }

    /** Teraslarda çiçekler, çalılar ve çim. */
    private static void flora(WorldGenLevel level, CityPlan plan, int minX, int minZ, int maxX, int maxZ) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState[] flowers = {Blocks.GRASS.defaultBlockState(), Blocks.GRASS.defaultBlockState(), Blocks.GRASS.defaultBlockState(),
                Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(),
                Blocks.AZURE_BLUET.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), Blocks.FERN.defaultBlockState(),
                Blocks.AZALEA.defaultBlockState(), Blocks.FLOWERING_AZALEA.defaultBlockState()};
        long seed = plan.city.seed();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (plan.weight(x, z) <= 0.3) continue;
                double r = Noise.hash(seed + 55, x, 0, z);
                if (r > 0.16) continue;
                int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                p.set(x, top, z);
                if (!level.getBlockState(p).is(Blocks.GRASS_BLOCK)) continue;
                p.set(x, top + 1, z);
                if (!level.getBlockState(p).isAir()) continue;
                level.setBlock(p, flowers[(int) (Noise.hash(seed + 56, x, 1, z) * flowers.length)], 2);
            }
        }
    }

    private static void spawn(WorldGenLevel level, ServerLevel server, CityPlan plan, ChunkPos cp) {
        CityCensus census = CityCensus.get(server);
        for (CityPlan.Npc n : plan.npcs) {
            if ((n.x() >> 4) != cp.x || (n.z() >> 4) != cp.z) continue;
            if (n.v2() && census.has(plan.city.seed(), n.post())) continue;
            ElfEntity e = create(server, plan, n);
            if (e == null) continue;
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.STRUCTURE, null, null);
            level.addFreshEntityWithPassengers(e);
            census.mark(plan.city.seed(), n.post());
        }
    }

    /** Planlı bir sakini yaratır ve kentin günlük programına bağlar. */
    @javax.annotation.Nullable
    public static ElfEntity create(ServerLevel server, CityPlan plan, CityPlan.Npc n) {
        ElfEntity e = ElvenRealm.TYPES.get(n.kind()).get().create(server);
        if (e == null) return null;
        e.moveTo(n.x() + 0.5, n.y(), n.z() + 0.5, n.post().startsWith("royal") || n.kind() == com.krolasyon.elvenrealm.entity.ElfKind.AERENDIL
                ? 0F : Noise.hash(plan.city.seed(), n.x(), 0, n.z()) < 0.5 ? 180F : 0F, 0F);
        e.setHome(new BlockPos(n.x(), n.y(), n.z()));
        e.setCity(new BlockPos(plan.cx, plan.yA, plan.cz), n.post(), true);
        return e;
    }
}
