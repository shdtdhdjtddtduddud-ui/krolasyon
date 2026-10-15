package com.krolasyon.alien.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DeskBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(1, 0, 1, 4, 12, 4),
            Block.box(12, 0, 1, 15, 12, 4), Block.box(1, 0, 12, 4, 12, 15), Block.box(12, 0, 12, 15, 12, 15));

    public DeskBlock(Properties p) { super(p); }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
}
