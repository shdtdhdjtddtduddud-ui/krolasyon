package com.krolasyon.labirent.block;

import com.krolasyon.labirent.maze.MazeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A carved stone hidden deep in each maze section. Once the section is mapped it gives up a code word. */
public class CodeStoneBlock extends Block {
    public static final IntegerProperty SECTOR = IntegerProperty.create("sector", 0, 7);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public CodeStoneBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(SECTOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(SECTOR); }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        MazeManager m = MazeManager.get();
        if (m != null && player instanceof ServerPlayer sp) m.story.onCodeStone(sp, s.getValue(SECTOR));
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0)
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + (r.nextDouble() - 0.5), pos.getY() + 1.2, pos.getZ() + 0.5 + (r.nextDouble() - 0.5), 0, -0.4, 0);
    }
}
