package com.krolasyon.bosses.rpg.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Height queries that load (or generate) the chunk first. Level#getHeight silently answers the bottom of the world
 * for chunks that are not loaded, which would put whole towns at bedrock.
 */
public final class Heights {
    private Heights() {}

    public static int get(ServerLevel level, Heightmap.Types type, int x, int z) {
        ChunkAccess c = level.getChunk(x >> 4, z >> 4);
        return c.getHeight(type, x & 15, z & 15) + 1;
    }

    public static int ground(ServerLevel level, int x, int z) { return get(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z); }

    /** only if the chunk is already loaded, otherwise Integer.MIN_VALUE */
    public static int loadedGround(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return Integer.MIN_VALUE;
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }
}
