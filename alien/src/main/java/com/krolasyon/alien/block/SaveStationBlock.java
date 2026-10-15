package com.krolasyon.alien.block;

import com.krolasyon.alien.ai.SaveManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Wall mounted emergency phone: saving here moves your respawn point. */
public class SaveStationBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape NORTH = Block.box(2, 0, 11, 14, 16, 16);
    private static final VoxelShape SOUTH = Block.box(2, 0, 0, 14, 16, 5);
    private static final VoxelShape WEST = Block.box(11, 0, 2, 16, 16, 14);
    private static final VoxelShape EAST = Block.box(0, 0, 2, 5, 16, 14);

    public SaveStationBlock(Properties p) {
        super(p);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) SaveManager.start(sp, pos, state.getValue(FACING));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) != 0) return;
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getStepX() * 0.2 + (r.nextDouble() - 0.5) * 0.3;
        double z = pos.getZ() + 0.5 + f.getStepZ() * 0.2 + (r.nextDouble() - 0.5) * 0.3;
        boolean green = (level.getGameTime() / 20) % 2 == 0;
        level.addParticle(new DustParticleOptions(green ? new Vector3f(0.2F, 1F, 0.3F) : new Vector3f(1F, 0.8F, 0.1F), 0.35F),
                x, pos.getY() + 0.62, z, 0, 0, 0);
    }
}
