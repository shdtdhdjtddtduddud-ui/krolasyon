package com.krolasyon.furniture.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** furniture that is two blocks wide: the clicked block is the RIGHT half (and owns the block entity), the LEFT half sits on the sitter's left. */
public abstract class WideBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<WidePart> PART = EnumProperty.create("part", WidePart.class);

    private final Map<Integer, VoxelShape> shapes = new ConcurrentHashMap<>();

    protected WideBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, WidePart.RIGHT));
    }

    protected abstract VoxelShape baseShape(WidePart part);

    /** blocks that must be free above the piece (0 = none) */
    protected int clearanceAbove() { return 0; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    public static Direction partnerDirection(BlockState s) {
        Direction f = s.getValue(FACING);
        return s.getValue(PART) == WidePart.RIGHT ? f.getCounterClockWise() : f.getClockWise();
    }

    /** position of the RIGHT half (the block entity owner) for any half */
    public static BlockPos basePos(BlockState s, BlockPos pos) {
        return s.getValue(PART) == WidePart.RIGHT ? pos : pos.relative(s.getValue(FACING).getClockWise());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction f = state.getValue(FACING);
        WidePart part = state.getValue(PART);
        return shapes.computeIfAbsent(part.ordinal() * 4 + f.get2DDataValue(), k -> ShapeUtil.rotate(baseShape(part), f));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = ctx.getHorizontalDirection().getOpposite();
        BlockPos base = ctx.getClickedPos();
        BlockPos other = base.relative(f.getCounterClockWise());
        Level level = ctx.getLevel();
        if (!level.getBlockState(other).canBeReplaced(ctx) || !level.getWorldBorder().isWithinBounds(other)) return null;
        for (int i = 1; i <= clearanceAbove(); i++) {
            if (!level.getBlockState(base.above(i)).canBeReplaced(ctx) || !level.getBlockState(other.above(i)).canBeReplaced(ctx)) return null;
        }
        return defaultBlockState().setValue(FACING, f).setValue(PART, WidePart.RIGHT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockPos other = pos.relative(state.getValue(FACING).getCounterClockWise());
            level.setBlock(other, state.setValue(PART, WidePart.LEFT), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState nState, LevelAccessor level, BlockPos pos, BlockPos nPos) {
        if (dir == partnerDirection(state)) {
            if (!nState.is(this) || nState.getValue(PART) == state.getValue(PART) || nState.getValue(FACING) != state.getValue(FACING)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, dir, nState, level, pos, nPos);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == WidePart.RIGHT ? net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED
                : net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }

    @SuppressWarnings("unchecked")
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> ticker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> t) {
        return expected == given ? (BlockEntityTicker<A>) t : null;
    }
}
