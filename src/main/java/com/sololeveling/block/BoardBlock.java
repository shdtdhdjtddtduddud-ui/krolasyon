package com.sololeveling.block;

import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.system.Quests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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

public class BoardBlock extends HorizontalDirectionalBlock {
    private final String kind;

    public BoardBlock(Properties p, String kind) {
        super(p);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        Direction d = s.getValue(FACING);
        return d.getAxis() == Direction.Axis.Z ? Block.box(0, 0, 5, 16, 16, 9) : Block.box(5, 0, 0, 9, 16, 16);
    }

    @Override
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (kind.equals("bounty_board")) {
                Quests.refreshOffers(sp);
                Net.toPlayer(sp, new Packets.OpenSystem(2));
            } else {
                Net.toPlayer(sp, new Packets.OpenSystem(4));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
