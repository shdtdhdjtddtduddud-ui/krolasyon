package com.krolasyon.alien.block;

import com.krolasyon.alien.ai.VentNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Air duct grate. Every vent in a level forms the network the xenomorph travels through, unseen. */
public class VentBlock extends Block {
    public VentBlock(Properties p) { super(p); }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (level instanceof ServerLevel sl && !old.is(this)) VentNetwork.get(sl).add(pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (level instanceof ServerLevel sl && !now.is(this)) VentNetwork.get(sl).remove(pos);
        super.onRemove(state, level, pos, now, moving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(6) != 0) return;
        if (level.getBlockState(pos.below()).isAir()) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + r.nextDouble(), pos.getY() - 0.05, pos.getZ() + r.nextDouble(), 0, -0.02, 0);
            if (r.nextInt(5) == 0)
                level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() - 0.1, pos.getZ() + 0.5, 0, -0.015, 0);
        }
    }
}
