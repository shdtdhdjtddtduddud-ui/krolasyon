package com.krolasyon.skyislands.world;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Runs once per overworld chunk and builds the parts of all nearby sky islands that fall into that chunk. */
public final class SkyIslandFeature extends Feature<NoneFeatureConfiguration> {
    private static final Map<Long, Integer> GROUND_CACHE = new ConcurrentHashMap<>();

    public SkyIslandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        if (server == null || !server.dimension().equals(Level.OVERWORLD)) return false;
        ChunkPos cp = new ChunkPos(ctx.origin());
        int minX = cp.getMinBlockX(), minZ = cp.getMinBlockZ(), maxX = cp.getMaxBlockX(), maxZ = cp.getMaxBlockZ();
        long seed = level.getSeed();
        List<Island> islands = IslandLayout.near(seed, minX, minZ, maxX, maxZ);
        if (islands.isEmpty()) return false;
        BlockSink sink = new BlockSink(level, ctx.chunkGenerator(), minX, minZ, maxX, maxZ, 2);
        boolean any = false;
        for (Island is : islands) {
            if (ElvenCities.blocks(seed, is)) continue;
            Island lifted = liftAboveTerrain(is, ctx, server, seed);
            if (lifted == null) continue;
            IslandBuilder.build(lifted, sink, ctx.random());
            any = true;
        }
        return any;
    }

    /** Moves an island up if a mountain would poke into it; returns null if it would not fit below build height. */
    private static Island liftAboveTerrain(Island is, FeaturePlaceContext<NoneFeatureConfiguration> ctx, ServerLevel server, long seed) {
        long key = Noise.mix(seed ^ (((long) is.cx()) << 32) ^ (is.cz() & 0xffffffffL));
        if (GROUND_CACHE.size() > 8192) GROUND_CACHE.clear();
        int ground = GROUND_CACHE.computeIfAbsent(key, k -> ctx.chunkGenerator().getBaseHeight(is.cx(), is.cz(),
                Heightmap.Types.WORLD_SURFACE_WG, ctx.level(), server.getChunkSource().randomState()));
        int lowest = is.baseY() - is.depth() * 2 - 10;
        if (lowest > ground) return is;
        int baseY = ground + is.depth() * 2 + 12;
        if (baseY + (is.giantTree() ? 60 : 20) > ctx.level().getMaxBuildHeight() - 2) return null;
        return new Island(is.seed(), is.cx(), is.cz(), baseY, is.radius(), is.depth(), is.pond(), is.giantTree(), is.satellite());
    }
}
