package com.krolasyon.smoothterrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Çimen yarım bloğu: üstü kapanırsa (çift ya da üst yarım değilken üstüne katı blok gelirse) toprağa döner. */
public final class GrassSlabBlock extends SlabBlock {
    public GrassSlabBlock(Properties p) {
        super(p.randomTicks());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (state.getValue(TYPE) == SlabType.BOTTOM) return;
        BlockState above = level.getBlockState(pos.above());
        if (above.isSolidRender(level, pos.above())) {
            level.setBlockAndUpdate(pos, SmoothTerrain.DIRT_SLAB.get().defaultBlockState().setValue(TYPE, state.getValue(TYPE))
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED)));
        }
    }
}
