package com.krolasyon.mirror.client;

import com.krolasyon.mirror.block.MirrorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Shared animation clock for the reflection (renderer and knock sounds must agree). */
public final class MirrorClientEffects {
    public static final int ACT_MIMIC = -1, ACT_STARE = 0, ACT_WAVE = 1, ACT_KNOCK = 2;
    public static final int KNOCK_CYCLE = 40;

    private static long offset(BlockPos pos) {
        return pos.asLong() & 63;
    }

    /** While awakening the reflection mostly copies you, but now and then does its own thing. */
    public static int awakenAction(long gameTime, BlockPos pos) {
        long t = gameTime + offset(pos);
        long c = t % 240;
        if (c < 140 || c >= 200) return ACT_MIMIC;
        return (int) ((t / 240) % 3);
    }

    public static int awakenDelay(long gameTime, BlockPos pos) {
        long c = (gameTime + offset(pos)) % 240;
        return c < 140 ? 10 : 3;
    }

    /** 0..1 pulse that peaks on each knock ("tak tak ... tak tak"). */
    public static float knock(float t) {
        float k = t % KNOCK_CYCLE;
        float a = k - 1.0F, b = k - 8.0F;
        return (float) (Math.exp(-a * a / 3.0) + Math.exp(-b * b / 3.0));
    }

    public static boolean knocking(MirrorBlockEntity be, long gameTime) {
        if (be.thanksUntil > gameTime || be.shakeUntil > gameTime) return false;
        return be.getPhase() == MirrorBlockEntity.REQUEST
                || (be.getPhase() == MirrorBlockEntity.AWAKEN && awakenAction(gameTime, be.getBlockPos()) == ACT_KNOCK);
    }

    public static void tick(Level level, BlockPos pos, MirrorBlockEntity be) {
        long t = level.getGameTime();
        long k = t % KNOCK_CYCLE;
        if ((k == 1 || k == 8) && knocking(be, t)) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SoundEvents.GLASS_HIT, SoundSource.BLOCKS,
                    0.9F, k == 1 ? 0.75F : 0.85F, false);
        }
    }

    private MirrorClientEffects() {}
}
