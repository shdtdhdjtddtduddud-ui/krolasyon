package com.krolasyon.advgear.world;

import com.krolasyon.advgear.AdvGear;
import com.krolasyon.advgear.block.LockType;
import com.krolasyon.advgear.block.LockedChestBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Mağara zeminlerine kilitli sandık yerleştirir (yanında bazen bir kafatası ve mum). */
public final class LockedChestFeature extends Feature<NoneFeatureConfiguration> {
    public LockedChestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        for (int attempt = 0; attempt < 6; attempt++) {
            int x = (o.getX() & ~15) + r.nextInt(16), z = (o.getZ() & ~15) + r.nextInt(16);
            int y0 = level.getMinBuildHeight() + 8 + r.nextInt(Math.max(1, 70 - (level.getMinBuildHeight() + 8)));
            for (int y = y0; y > level.getMinBuildHeight() + 6; y--) {
                BlockPos p = new BlockPos(x, y, z);
                if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) continue;
                if (!level.getBlockState(p.below()).isSolidRender(level, p.below())) continue;
                if (level.canSeeSkyFromBelowWater(p)) break;
                float roll = r.nextFloat();
                LockType lock = roll < 0.6F ? LockType.IRON : roll < 0.9F ? LockType.BONE : LockType.HEART;
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(r);
                level.setBlock(p, AdvGear.LOCKED_CHEST.get().defaultBlockState().setValue(LockedChestBlock.FACING, dir).setValue(LockedChestBlock.LOCK, lock), 2);
                BlockPos side = p.relative(dir.getClockWise());
                if (level.getBlockState(side).isAir() && level.getBlockState(side.below()).isSolidRender(level, side.below())) {
                    if (r.nextFloat() < 0.4F) level.setBlock(side, Blocks.SKELETON_SKULL.defaultBlockState()
                            .setValue(SkullBlock.ROTATION, r.nextInt(16)), 2);
                    else level.setBlock(side, Blocks.CANDLE.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 1 + r.nextInt(3))
                            .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true), 2);
                }
                return true;
            }
        }
        return false;
    }
}
