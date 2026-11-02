package com.krolasyon.horror.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Wall candle; the flame flickers and smokes. */
public class SconceBlock extends FacingDecorBlock {
    public SconceBlock(Properties props, double[][] boxes) {
        super(props, true, boxes, null);
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(BlockStateProperties.LIT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (!state.getValue(BlockStateProperties.LIT)) return;
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getStepX() * 0.19, z = pos.getZ() + 0.5 + f.getStepZ() * 0.19;
        if (rnd.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 0.82, z, 0, 0.002, 0);
        if (rnd.nextInt(6) == 0) level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.9, z, 0, 0.01, 0);
    }
}
