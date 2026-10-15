package com.krolasyon.alien.ai;

import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Emergency phone save: three tense seconds standing still, then your respawn point moves here. */
public final class SaveManager {
    private SaveManager() {}

    private record Save(BlockPos panel, Direction facing, Vec3 start, int[] ticks) {}

    private static final Map<UUID, Save> SAVING = new HashMap<>();
    public static final int DURATION = 60;

    public static void start(ServerPlayer p, BlockPos panel, Direction facing) {
        if (SAVING.containsKey(p.getUUID()) || HidingManager.isHidden(p)) return;
        SAVING.put(p.getUUID(), new Save(panel.immutable(), facing, p.position(), new int[]{0}));
        p.displayClientMessage(Component.translatable("message.alienisolation.saving").withStyle(ChatFormatting.YELLOW), true);
        NoiseSystem.emit(p.serverLevel(), panel, 8, NoiseSystem.Kind.DEVICE, p);
    }

    public static void tick(ServerPlayer p) {
        Save s = SAVING.get(p.getUUID());
        if (s == null) return;
        ServerLevel level = p.serverLevel();
        int t = ++s.ticks()[0];
        if (p.position().distanceToSqr(s.start()) > 2.25 || p.hurtTime > 0 || !p.isAlive()) {
            SAVING.remove(p.getUUID());
            p.displayClientMessage(Component.translatable("message.alienisolation.save_cancel").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (t % 15 == 1) level.playSound(null, s.panel(), ModSounds.SAVE_BEEP.get(), SoundSource.BLOCKS, 0.8F, 1.0F + t / 120F);
        int bars = Math.min(10, t * 10 / DURATION);
        p.displayClientMessage(Component.translatable("message.alienisolation.saving").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" [" + "|".repeat(bars) + " ".repeat(10 - bars) + "]").withStyle(ChatFormatting.GREEN)), true);
        if (t >= DURATION) {
            SAVING.remove(p.getUUID());
            BlockPos spot = s.panel().relative(s.facing());
            for (int i = 0; i < 4 && level.getBlockState(spot.below()).getCollisionShape(level, spot.below()).isEmpty(); i++) spot = spot.below();
            p.setRespawnPosition(level.dimension(), spot, s.facing().toYRot(), true, false);
            level.playSound(null, s.panel(), ModSounds.SAVE_DONE.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
            p.displayClientMessage(Component.translatable("message.alienisolation.saved").withStyle(ChatFormatting.GREEN), true);
        }
    }

    public static void cancel(ServerPlayer p) { SAVING.remove(p.getUUID()); }

    public static void clear() { SAVING.clear(); }
}
