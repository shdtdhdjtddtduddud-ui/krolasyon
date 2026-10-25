package com.krolasyon.labirent.block;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

/** The terminal in the Griever burrow where the code has to be entered. */
public class CodeTerminalBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public CodeTerminalBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            MazeManager m = MazeManager.get();
            if (m != null) Net.sendState(sp, m);
            Net.open(sp, Net.SCREEN_TERMINAL, 0);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(4) == 0)
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + r.nextDouble(), pos.getY() + 1.05, pos.getZ() + r.nextDouble(), 0, 0.02, 0);
    }
}
