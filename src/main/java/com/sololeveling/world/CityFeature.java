package com.sololeveling.world;

import com.sololeveling.gen.Content;
import com.sololeveling.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Places the region cities (Seoul, Gangnam, ...) of the Hunter World chunk by chunk. */
public class CityFeature extends Feature<NoneFeatureConfiguration> {
    public CityFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        if (level.getLevel().dimension() != ModDimensions.HUNTER_WORLD) return false;
        BlockPos o = ctx.origin();
        int x0 = o.getX() & ~15, z0 = o.getZ() & ~15;
        long seed = level.getSeed();
        boolean any = false;
        double ccx = x0 + 8, ccz = z0 + 8;
        for (Content.RegionDef r : Content.REGIONS) {
            if (Math.hypot(ccx - r.cx(), ccz - r.cz()) > r.radius() + Regions.MARGIN + 14) continue;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = x0 + dx, z = z0 + dz;
                    if (Regions.at(x, z) != r) continue;
                    CityGen.column(level, r, x, z, seed);
                    any = true;
                }
            }
            CityGen.spawnNpcsInChunk(level, r, x0, z0, seed);
        }
        return any;
    }
}
