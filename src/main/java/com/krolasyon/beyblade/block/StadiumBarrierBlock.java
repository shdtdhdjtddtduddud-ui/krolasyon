package com.krolasyon.beyblade.block;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible wall placed in a ring around a stadium: players and mobs cannot walk through the stadium, beyblades fly
 * over it freely (they are kept inside by the stadium's own walls). Removed together with the stadium.
 */
public class StadiumBarrierBlock extends Block {
    public StadiumBarrierBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ec && ec.getEntity() instanceof BeybladeEntity) return Shapes.empty();
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1F; }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) { return false; }
}
