package com.krolasyon.horror.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A story prop lying flat on the floor or (raised) on a table below it: notes (VARIANT = note number) and
 * pickups (VARIANT = item index).
 */
public class PropBlock extends Block {
    public static final BooleanProperty RAISED = BooleanProperty.create("raised");
    private static final VoxelShape FLOOR = Block.box(2, 0, 2, 14, 1, 14);
    private final IntegerProperty variant;

    public static PropBlock notes(Properties p) { return new PropBlock(p, NOTE); }

    public static PropBlock items(Properties p) { return new PropBlock(p, ITEM); }

    public static final IntegerProperty NOTE = IntegerProperty.create("note", 0, 19);
    public static final IntegerProperty ITEM = IntegerProperty.create("item", 0, 7);
    private static IntegerProperty pending;

    private PropBlock(Properties p, IntegerProperty variant) {
        super(prep(p, variant));
        this.variant = variant;
        this.registerDefaultState(this.stateDefinition.any().setValue(variant, 0).setValue(RAISED, false));
    }

    private static Properties prep(Properties p, IntegerProperty v) {
        pending = v;
        return p;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(pending, RAISED);
    }

    public IntegerProperty variant() { return variant; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(RAISED) ? Block.box(2, 0, 2, 14, 1, 14).move(0, -6 / 16.0, 0) : FLOOR;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }
}
