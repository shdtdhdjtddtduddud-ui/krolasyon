package com.krolasyon.swordvale.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Her overworld chunk'ında bir kez çalışır: içine düşen vadi patikalarını, kılıçları, kraterleri ve mezarları üretir. */
public final class SwordValeFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation GRAVE_LOOT = new ResourceLocation("swordvale", "chests/sword_grave");

    public SwordValeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
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
        for (ValeLayout.Vale v : ValeLayout.valesNear(level.getSeed(), minX, minZ, maxX, maxZ)) {
            if (!ValeLayout.valid(v, gen, rs)) continue;
            paths(level, v, minX, minZ, maxX, maxZ);
            for (ValeLayout.Sword s : ValeLayout.swords(v)) {
                int r = s.reach();
                if (s.x() + r < minX || s.x() - r > maxX || s.z() + r < minZ || s.z() - r > maxZ) continue;
                if (!ValeLayout.biomeOk(gen, rs, s.x(), s.z())) continue;
                int g = ValeLayout.ground(gen, rs, level, s.x(), s.z());
                if (g <= gen.getSeaLevel() + 1) continue;
                SwordShape shape = new SwordShape(s, g);
                crater(level, shape, minX, minZ, maxX, maxZ);
                build(level, shape, minX, minZ, maxX, maxZ);
                if (s.grave()) grave(level, shape, ctx);
                any = true;
            }
        }
        return any;
    }

    private static void build(WorldGenLevel level, SwordShape shape, int minX, int minZ, int maxX, int maxZ) {
        int[] bb = shape.bounds();
        int x0 = Math.max(minX, bb[0]), x1 = Math.min(maxX, bb[3]);
        int z0 = Math.max(minZ, bb[2]), z1 = Math.min(maxZ, bb[5]);
        int y0 = Math.max(level.getMinBuildHeight(), bb[1]), y1 = Math.min(level.getMaxBuildHeight() - 1, bb[4]);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    BlockState st = shape.blockAt(x, y, z);
                    if (st == null) continue;
                    p.set(x, y, z);
                    level.setBlock(p, st, 2);
                    // siper ve topuz üstünde yosun halısı
                    if (st.is(Blocks.MOSSY_STONE_BRICKS) || st.is(Blocks.MOSS_BLOCK)) {
                        BlockPos up = p.above();
                        if (level.getBlockState(up).isAir() && shape.blockAt(x, y + 1, z) == null && Noise.hash(shape.s.seed() + 20, x, y, z) < 0.45) {
                            level.setBlock(up, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    /** Kılıcın saplandığı yerde çatlamış toprak ve moloz. */
    private static void crater(WorldGenLevel level, SwordShape shape, int minX, int minZ, int maxX, int maxZ) {
        double rad = shape.width() * 1.3 + 2;
        int r = (int) Math.ceil(rad);
        int cx = (int) Math.floor(shape.baseX()), cz = (int) Math.floor(shape.baseZ());
        long seed = shape.s.seed();
        for (int x = Math.max(minX, cx - r); x <= Math.min(maxX, cx + r); x++) {
            for (int z = Math.max(minZ, cz - r); z <= Math.min(maxZ, cz + r); z++) {
                double d = Math.hypot(x + 0.5 - shape.baseX(), z + 0.5 - shape.baseZ()) / rad;
                if (d > 1 || Noise.hash(seed + 30, x, 0, z) > 1.2 - d) continue;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos p = new BlockPos(x, top, z);
                BlockState cur = level.getBlockState(p);
                if (!cur.is(Blocks.GRASS_BLOCK) && !cur.is(Blocks.DIRT) && !cur.is(Blocks.PODZOL) && !cur.is(Blocks.SNOW_BLOCK)) continue;
                double h = Noise.hash(seed + 31, x, 1, z);
                BlockState st = h < 0.35 ? Blocks.COARSE_DIRT.defaultBlockState() : h < 0.55 ? Blocks.ROOTED_DIRT.defaultBlockState()
                        : h < 0.75 ? Blocks.GRAVEL.defaultBlockState() : h < 0.9 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                level.setBlock(p, st, 2);
                BlockPos up = p.above();
                if (!level.getBlockState(up).isAir() && level.getBlockState(up).canBeReplaced()) level.setBlock(up, Blocks.AIR.defaultBlockState(), 2);
                if (h > 0.93 && level.getBlockState(up).isAir()) level.setBlock(up, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
            }
        }
    }

    /** Dev kılıcın önünde gömülü mezar sandığı, üstünde yosunlu bir mezar taşı ve kafatası. */
    private static void grave(WorldGenLevel level, SwordShape shape, FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        BlockPos chest = shape.gravePos();
        ChunkPos cp = new ChunkPos(ctx.origin());
        if ((chest.getX() >> 4) != cp.x || (chest.getZ() >> 4) != cp.z) return;
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, chest.getX(), chest.getZ()) - 1;
        BlockPos c = new BlockPos(chest.getX(), top - 1, chest.getZ());
        level.setBlock(c, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 2);
        RandomizableContainerBlockEntity.setLootTable(level, ctx.random(), c, GRAVE_LOOT);
        level.setBlock(c.above(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
        level.setBlock(c.above(2), Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(), 2);
        level.setBlock(c.above(3), Blocks.SKELETON_SKULL.defaultBlockState(), 2);
    }

    /** Vadinin içinde kıvrılan toprak patikalar (iki gürültü alanının sıfır eğrileri). */
    private static void paths(WorldGenLevel level, ValeLayout.Vale v, int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double inf = ValeLayout.influence(v, x, z);
                if (inf < 0.04) continue;
                double n1 = Noise.fbm2(v.seed() + 11, x * 0.0105, z * 0.0105, 3);
                double n2 = Noise.fbm2(v.seed() + 12, x * 0.0085 + 40, z * 0.0085, 3);
                double w1 = 0.011 * Math.min(1, inf * 3), w2 = 0.007 * Math.min(1, inf * 3);
                if (Math.abs(n1 - 0.5) > w1 && Math.abs(n2 - 0.5) > w2) continue;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos p = new BlockPos(x, top, z);
                BlockState cur = level.getBlockState(p);
                if (!cur.is(Blocks.GRASS_BLOCK) && !cur.is(Blocks.DIRT) && !cur.is(Blocks.PODZOL) && !cur.is(Blocks.COARSE_DIRT)) continue;
                BlockPos up = p.above();
                BlockState above = level.getBlockState(up);
                if (!above.isAir() && !above.canBeReplaced()) continue;
                if (!above.isAir()) level.setBlock(up, Blocks.AIR.defaultBlockState(), 2);
                double h = Noise.hash(v.seed() + 13, x, 0, z);
                level.setBlock(p, h < 0.08 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState(), 2);
            }
        }
    }
}
