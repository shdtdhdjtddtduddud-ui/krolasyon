package com.krolasyon.horror.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Non-rotating decoration with a custom outline (carpets, lamps, tables, balloons). */
public class DecorBlock extends Block {
    private final VoxelShape shape;
    private final VoxelShape collision;

    public DecorBlock(Properties props, VoxelShape shape, VoxelShape collision) {
        super(props);
        this.shape = shape;
        this.collision = collision;
    }

    public DecorBlock(Properties props, VoxelShape shape) { this(props, shape, shape); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return shape; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return collision == null ? Shapes.empty() : collision;
    }
}
