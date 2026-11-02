package com.krolasyon.horror.block;

import com.krolasyon.horror.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** An old television that never stops showing static - it hisses, and the screen flickers. */
public class CrtTvBlock extends FacingDecorBlock {
    public CrtTvBlock(Properties props, double[][] boxes) { super(props, false, boxes); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (rnd.nextInt(5) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, ModSounds.STATIC.get(), SoundSource.BLOCKS,
                    0.12F + rnd.nextFloat() * 0.1F, 0.8F + rnd.nextFloat() * 0.3F, false);
        }
        if (rnd.nextInt(3) == 0) {
            Direction f = state.getValue(FACING);
            double x = pos.getX() + 0.5 + f.getStepX() * 0.55 + (rnd.nextDouble() - 0.5) * 0.5;
            double z = pos.getZ() + 0.5 + f.getStepZ() * 0.55 + (rnd.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.WHITE_ASH, x, pos.getY() + 0.6 + rnd.nextDouble() * 0.5, z, 0, 0, 0);
        }
    }
}
