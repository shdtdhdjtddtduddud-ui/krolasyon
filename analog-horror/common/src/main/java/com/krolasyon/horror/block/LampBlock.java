package com.krolasyon.horror.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ceiling light that the story can switch off (power cuts, flicker). */
public class LampBlock extends DecorBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LampBlock(Properties props, VoxelShape shape) {
        super(props, shape, null);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }
}
