package com.rabona.arena.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Stadyum bloklari: direk, file, korner bayragi, koltuk, projektor, LED pano. */
public final class ShapedBlocks {
    private ShapedBlocks() {}

    /** Ince beyaz kale diregi; eksene gore yatay veya dikey. */
    public static class GoalPost extends RotatedPillarBlock {
        static final VoxelShape Y = Block.box(5, 0, 5, 11, 16, 11);
        static final VoxelShape X = Block.box(0, 5, 5, 16, 11, 11);
        static final VoxelShape Z = Block.box(5, 5, 0, 11, 11, 16);

        public GoalPost(Properties p) { super(p); }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return switch (s.getValue(AXIS)) {
                case X -> X;
                case Z -> Z;
                default -> Y;
            };
        }
    }

    /** Kale filesi: gorunur ag, tam carpisma (top fileye carpar). */
    public static class Net extends Block {
        public Net(Properties p) { super(p); }

        @Override
        public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }

        @Override
        public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) { return 1.0f; }

        @Override
        public boolean skipRendering(BlockState s, BlockState other, Direction d) {
            return other.is(this) || super.skipRendering(s, other, d);
        }
    }

    public static class CornerFlag extends Block {
        static final VoxelShape SHAPE = Block.box(7, 0, 7, 9, 16, 9);

        public CornerFlag(Properties p) { super(p); }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

        @Override
        public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    }

    public static class Facing extends HorizontalDirectionalBlock {
        private final VoxelShape[] shapes;

        /** shape tanimi: kuzeye bakarken (minX,minY,minZ,maxX,maxY,maxZ) kutulari. */
        public Facing(Properties p, double[][] boxes) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
            shapes = new VoxelShape[4];
            for (Direction d : Direction.Plane.HORIZONTAL) {
                VoxelShape v = Shapes.empty();
                for (double[] b : boxes) v = Shapes.or(v, rotate(b, d));
                shapes[d.get2DDataValue()] = v;
            }
        }

        private static VoxelShape rotate(double[] b, Direction d) {
            double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
            return switch (d) {
                case SOUTH -> Block.box(16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0);
                case WEST -> Block.box(z0, b[1], 16 - x1, z1, b[4], 16 - x0);
                case EAST -> Block.box(16 - z1, b[1], x0, 16 - z0, b[4], x1);
                default -> Block.box(x0, b[1], z0, x1, b[4], z1);
            };
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return shapes[s.getValue(FACING).get2DDataValue()];
        }
    }
}
