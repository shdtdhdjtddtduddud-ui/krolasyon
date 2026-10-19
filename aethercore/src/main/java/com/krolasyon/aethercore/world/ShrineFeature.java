package com.krolasyon.aethercore.world;

import com.krolasyon.aethercore.AetherCore;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Kadim Sunak: kalsit ve kuvars bir platform, ametist taçlı dört sütun, ortada kaide üzerinde uyuyan bir
 * Eterik Çekirdek ve ganimet sandığı.
 */
public final class ShrineFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation LOOT = new ResourceLocation(AetherCore.MODID, "chests/ancient_shrine");

    public ShrineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return build(ctx.level(), ctx.origin(), ctx.random(), true);
    }

    public static boolean build(WorldGenLevel level, BlockPos origin, RandomSource random, boolean checkTerrain) {
        BlockPos o = origin;
        if (checkTerrain) {
            int y0 = o.getY();
            for (int dx = -4; dx <= 4; dx += 4) {
                for (int dz = -4; dz <= 4; dz += 4) {
                    int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX() + dx, o.getZ() + dz);
                    if (Math.abs(h - y0) > 2) return false;
                }
            }
            BlockState ground = level.getBlockState(o.below());
            if (!ground.isSolid() || !level.getFluidState(o.below()).isEmpty() || !level.getFluidState(o).isEmpty()) return false;
        }
        int flags = 2;
        // platform + temeller
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 4.4) continue;
                BlockPos p = o.offset(dx, -1, dz);
                BlockState top;
                if (d < 1.6) top = Blocks.SMOOTH_QUARTZ.defaultBlockState();
                else if (d < 2.6) top = (dx == 0 || dz == 0) ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState();
                else if (d < 3.6) top = Blocks.CALCITE.defaultBlockState();
                else top = random.nextFloat() < 0.35F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                level.setBlock(p, top, flags);
                for (int y = 2; y <= 5; y++) {
                    BlockPos b = o.offset(dx, -y, dz);
                    if (level.getBlockState(b).isSolid()) break;
                    level.setBlock(b, Blocks.STONE_BRICKS.defaultBlockState(), flags);
                }
                for (int y = 0; y <= 6; y++) level.setBlock(o.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), flags);
            }
        }
        // sütunlar
        int[][] cols = {{3, 3}, {-3, 3}, {3, -3}, {-3, -3}};
        for (int[] c : cols) {
            int h = 2 + random.nextInt(2);
            for (int y = 0; y < h; y++) level.setBlock(o.offset(c[0], y, c[1]), Blocks.QUARTZ_PILLAR.defaultBlockState(), flags);
            level.setBlock(o.offset(c[0], h, c[1]), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), flags);
            level.setBlock(o.offset(c[0], h + 1, c[1]), Blocks.AMETHYST_CLUSTER.defaultBlockState(), flags);
        }
        // kaide + çekirdek
        level.setBlock(o, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), flags);
        level.setBlock(o.above(), AetherCore.AETHER_CORE.get().defaultBlockState(), flags);
        // ganimet sandığı
        BlockPos chest = o.offset(0, 0, 3);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), flags);
        RandomizableContainerBlockEntity.setLootTable(level, random, chest, LOOT);
        // çevrede ametist parçaları
        for (int i = 0; i < 4; i++) {
            BlockPos p = o.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            if (level.getBlockState(p).isAir() && !p.equals(chest) && Math.abs(p.getX() - o.getX()) + Math.abs(p.getZ() - o.getZ()) > 1) {
                level.setBlock(p, Blocks.SMALL_AMETHYST_BUD.defaultBlockState(), flags);
            }
        }
        return true;
    }
}
