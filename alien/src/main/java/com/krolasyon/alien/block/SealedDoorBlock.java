package com.krolasyon.alien.block;

import com.krolasyon.alien.ai.NoiseSystem;
import com.krolasyon.alien.registry.ModItems;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Sealed bulkhead. Power seals open when the generator runs (see Objectives); keycard seals open with the
 * security keycard.
 */
public class SealedDoorBlock extends Block {
    private final boolean keycard;

    public SealedDoorBlock(Properties p, boolean keycard) {
        super(p);
        this.keycard = keycard;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (keycard && (player.getMainHandItem().is(ModItems.KEYCARD.get()) || player.getOffhandItem().is(ModItems.KEYCARD.get()))) {
            open((ServerLevel) level, pos);
            NoiseSystem.emit((ServerLevel) level, pos, 12, NoiseSystem.Kind.DOOR, player);
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(Component.translatable(keycard ? "message.alienisolation.need_keycard" : "message.alienisolation.need_power")
                .withStyle(ChatFormatting.RED), true);
        level.playSound(null, pos, ModSounds.FLAME_EMPTY.get(), SoundSource.BLOCKS, 0.6F, 0.7F);
        return InteractionResult.CONSUME;
    }

    /** removes this seal and every connected piece of the same seal. */
    public static void open(ServerLevel level, BlockPos start) {
        Block kind = level.getBlockState(start).getBlock();
        if (!(kind instanceof SealedDoorBlock)) return;
        Set<BlockPos> done = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start.immutable());
        while (!q.isEmpty() && done.size() < 16) {
            BlockPos p = q.poll();
            if (!done.add(p) || !level.getBlockState(p).is(kind)) continue;
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            for (BlockPos n : new BlockPos[]{p.above(), p.below(), p.north(), p.south(), p.east(), p.west()}) q.add(n);
        }
        level.playSound(null, start, ModSounds.DOOR.get(), SoundSource.BLOCKS, 1.2F, 0.7F);
    }
}
