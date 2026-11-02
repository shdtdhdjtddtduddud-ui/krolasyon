package com.krolasyon.horror.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Facing device with a number on it: the fuse box (fuses 0-3) and the transmitter dials (digit 0-9). */
public class CounterBlock extends FacingDecorBlock {
    public static final IntegerProperty FUSES = IntegerProperty.create("fuses", 0, 3);
    public static final IntegerProperty DIGIT = IntegerProperty.create("digit", 0, 9);
    private static IntegerProperty pending;
    private final IntegerProperty counter;

    public static CounterBlock fuseBox(Properties p) {
        pending = FUSES;
        return new CounterBlock(p, true, new double[][]{{3, 2, 13, 13, 14, 16}}, FUSES);
    }

    public static CounterBlock dial(Properties p) {
        pending = DIGIT;
        return new CounterBlock(p, false, new double[][]{{2, 0, 4, 14, 12, 14}}, DIGIT);
    }

    private CounterBlock(Properties p, boolean wall, double[][] boxes, IntegerProperty counter) {
        super(p, wall, boxes);
        this.counter = counter;
        this.registerDefaultState(this.defaultBlockState().setValue(counter, 0));
    }

    public IntegerProperty counter() { return counter; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(pending);
    }
}
