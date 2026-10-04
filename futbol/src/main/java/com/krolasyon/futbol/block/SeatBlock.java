package com.krolasyon.futbol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Stadium seat; FACING is the direction a sitting spectator looks at. */
public class SeatBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape BASE = Block.box(1, 0, 1, 15, 8, 15);
    private static final VoxelShape N = Shapes.or(BASE, Block.box(1, 8, 12, 15, 16, 15));
    private static final VoxelShape S = Shapes.or(BASE, Block.box(1, 8, 1, 15, 16, 4));
    private static final VoxelShape E = Shapes.or(BASE, Block.box(1, 8, 1, 4, 16, 15));
    private static final VoxelShape W = Shapes.or(BASE, Block.box(12, 8, 1, 15, 16, 15));

    public SeatBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> S;
            case EAST -> E;
            case WEST -> W;
            default -> N;
        };
    }
}
