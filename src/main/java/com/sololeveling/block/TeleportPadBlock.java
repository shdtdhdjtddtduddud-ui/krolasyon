package com.sololeveling.block;

import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TeleportPadBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public TeleportPadBlock(Properties p) { super(p); }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) Net.toPlayer(sp, new Packets.OpenSystem(3));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
