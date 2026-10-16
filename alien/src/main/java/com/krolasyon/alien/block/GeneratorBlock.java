package com.krolasyon.alien.block;

import com.krolasyon.alien.ai.Objectives;
import com.krolasyon.alien.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Emergency generator: three fuses bring the station's power back. */
public class GeneratorBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty FUSES = IntegerProperty.create("fuses", 0, 3);

    public GeneratorBlock(Properties p) {
        super(p);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FUSES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, FUSES); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Objectives.onGeneratorUsed((ServerLevel) level, (ServerPlayer) player, pos, player.getItemInHand(hand).is(ModItems.FUSE.get()) ? hand : null);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (state.getValue(FUSES) == 3 && r.nextInt(4) == 0)
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + r.nextDouble(), pos.getY() + 1.02, pos.getZ() + r.nextDouble(), 0, 0.05, 0);
        else if (state.getValue(FUSES) < 3 && r.nextInt(10) == 0)
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 0, 0.02, 0);
    }
}
