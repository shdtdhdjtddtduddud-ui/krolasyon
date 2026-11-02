package com.krolasyon.horror.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Horizontally rotating decoration. Boxes are given in pixels for the north-facing model and rotated like the blockstate.
 * Wall-mounted variants take their facing from the clicked wall face.
 */
public class FacingDecorBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> collisions = new EnumMap<>(Direction.class);
    private final boolean wallMounted;

    public FacingDecorBlock(Properties props, boolean wallMounted, double[][] boxes, double[][] collisionBoxes) {
        super(props);
        this.wallMounted = wallMounted;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            shapes.put(d, rotate(boxes, d));
            collisions.put(d, collisionBoxes == null ? Shapes.empty() : rotate(collisionBoxes, d));
        }
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public FacingDecorBlock(Properties props, boolean wallMounted, double[][] boxes) { this(props, wallMounted, boxes, boxes); }

    private static VoxelShape rotate(double[][] boxes, Direction d) {
        VoxelShape s = Shapes.empty();
        for (double[] b : boxes) {
            double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
            double[] r = switch (d) {
                case EAST -> new double[]{16 - z1, y0, x0, 16 - z0, y1, x1};
                case SOUTH -> new double[]{16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0};
                case WEST -> new double[]{z0, y0, 16 - x1, z1, y1, 16 - x0};
                default -> b;
            };
            s = Shapes.or(s, Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
        }
        return s;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        if (wallMounted && face.getAxis().isHorizontal()) return this.defaultBlockState().setValue(FACING, face);
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return collisions.get(state.getValue(FACING));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) { return state.setValue(FACING, rot.rotate(state.getValue(FACING))); }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
}
