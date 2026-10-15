package com.krolasyon.alien.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Papers, wrappers and cables littering the floor. */
public class DebrisBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public DebrisBlock(Properties p) { super(p); }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        return !level.isEmptyBlock(pos.below());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor level, BlockPos pos, BlockPos np) {
        return !s.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(s, d, n, level, pos, np);
    }
}
