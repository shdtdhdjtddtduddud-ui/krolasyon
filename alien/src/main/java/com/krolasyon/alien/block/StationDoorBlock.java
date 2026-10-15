package com.krolasyon.alien.block;

import com.krolasyon.alien.ai.NoiseSystem;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Hydraulic station door. Loud: anything that opens one can be heard. */
public class StationDoorBlock extends DoorBlock {
    public StationDoorBlock(Properties p, BlockSetType type) { super(p, type); }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult r = super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide) {
            level.playSound(null, pos, ModSounds.DOOR.get(), SoundSource.BLOCKS, 0.9F, 0.9F + level.random.nextFloat() * 0.2F);
            NoiseSystem.emit((ServerLevel) level, pos, 11, NoiseSystem.Kind.DOOR, player);
        }
        return r;
    }

    @Override
    public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
        boolean was = state.is(this) && state.getValue(OPEN);
        super.setOpen(entity, level, state, pos, open);
        if (!level.isClientSide && was != open)
            level.playSound(null, pos, ModSounds.DOOR.get(), SoundSource.BLOCKS, 0.9F, 0.85F + level.random.nextFloat() * 0.2F);
    }
}
