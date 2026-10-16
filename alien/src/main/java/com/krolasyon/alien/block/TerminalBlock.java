package com.krolasyon.alien.block;

import com.krolasyon.alien.ai.NoiseSystem;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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

/** Retro green-screen terminal with crew logs from the station. */
public class TerminalBlock extends HorizontalDirectionalBlock {
    public static final int LOGS = 14;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public TerminalBlock(Properties p) {
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
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int id = (int) (Math.floorMod(pos.asLong() * 31L + level.getGameTime() / 200L, LOGS));
        level.playSound(null, pos, ModSounds.TERMINAL.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        player.sendSystemMessage(Component.literal("> SEEGSON TERMINAL // ").withStyle(ChatFormatting.DARK_GREEN)
                .append(Component.translatable("terminal.alienisolation.title." + (id % 4)).withStyle(ChatFormatting.GREEN)));
        player.sendSystemMessage(Component.translatable("terminal.alienisolation.log." + id).withStyle(ChatFormatting.GREEN));
        int stage = com.krolasyon.alien.ai.Objectives.get((ServerLevel) level).stage();
        if (stage > 0 && stage < 5)
            player.sendSystemMessage(Component.translatable("terminal.alienisolation.hint." + stage).withStyle(ChatFormatting.YELLOW));
        NoiseSystem.emit((ServerLevel) level, pos, 5, NoiseSystem.Kind.DEVICE, player);
        return InteractionResult.CONSUME;
    }
}
