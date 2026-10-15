package com.krolasyon.alien.ai;

import com.krolasyon.alien.block.LockerBlock;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.network.Net;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Locker hiding. While inside, sneaking = holding your breath. Breath runs out fast; when the alien leans in to
 * sniff the locker you must hold it, but if you gasp or breathe at the wrong moment it hears you.
 */
public final class HidingManager {
    private HidingManager() {}

    public static final class Hide {
        public final BlockPos locker;
        public final Direction facing;
        public float breath = 1F;
        public boolean holding;
        public int gaspCooldown;
        public boolean danger;
        int sync;

        Hide(BlockPos locker, Direction facing) { this.locker = locker; this.facing = facing; }
    }

    private static final Map<UUID, Hide> HIDDEN = new HashMap<>();

    public static boolean isHidden(Player p) { return HIDDEN.containsKey(p.getUUID()); }

    @Nullable
    public static Hide get(Player p) { return HIDDEN.get(p.getUUID()); }

    /** true when the hidden player is breathing loud enough to be noticed right now. */
    public static boolean audible(Player p) {
        Hide h = HIDDEN.get(p.getUUID());
        return h != null && (!h.holding || h.gaspCooldown > 0);
    }

    @Nullable
    public static ServerPlayer occupant(ServerLevel level, BlockPos lower) {
        for (ServerPlayer p : level.players()) {
            Hide h = HIDDEN.get(p.getUUID());
            if (h != null && h.locker.equals(lower)) return p;
        }
        return null;
    }

    public static void enter(ServerPlayer p, BlockPos lower) {
        if (isHidden(p) || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        BlockState s = level.getBlockState(lower);
        if (!(s.getBlock() instanceof LockerBlock)) return;
        Direction f = s.getValue(LockerBlock.FACING);
        HIDDEN.put(p.getUUID(), new Hide(lower.immutable(), f));
        LockerBlock.setOccupied(level, lower, true);
        place(p, HIDDEN.get(p.getUUID()));
        level.playSound(null, lower, ModSounds.LOCKER.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        NoiseSystem.emit(level, lower, 6, NoiseSystem.Kind.LOCKER, p);
        XenomorphEntity.onPlayerHid(p, lower);
        sync(p, HIDDEN.get(p.getUUID()));
    }

    private static void place(ServerPlayer p, Hide h) {
        Vec3 c = Vec3.atBottomCenterOf(h.locker).add(-h.facing.getStepX() * 0.12, 0, -h.facing.getStepZ() * 0.12);
        p.stopRiding();
        p.teleportTo(p.serverLevel(), c.x, c.y, c.z, h.facing.toYRot(), 0F);
        p.setDeltaMovement(Vec3.ZERO);
    }

    public static void exit(ServerPlayer p) {
        Hide h = HIDDEN.remove(p.getUUID());
        if (h == null) return;
        ServerLevel level = p.serverLevel();
        LockerBlock.setOccupied(level, h.locker, false);
        BlockPos out = h.locker.relative(h.facing);
        p.teleportTo(level, out.getX() + 0.5, out.getY(), out.getZ() + 0.5, h.facing.toYRot(), p.getXRot());
        level.playSound(null, h.locker, ModSounds.LOCKER.get(), SoundSource.BLOCKS, 0.9F, 0.9F);
        NoiseSystem.emit(level, h.locker, 6, NoiseSystem.Kind.LOCKER, p);
        Net.toPlayer(p, new Net.HideState(false, 1F, false, false));
    }

    /** silently drop state (death, logout, dimension change). */
    public static void release(ServerPlayer p) {
        Hide h = HIDDEN.remove(p.getUUID());
        if (h == null) return;
        LockerBlock.setOccupied(p.serverLevel(), h.locker, false);
        Net.toPlayer(p, new Net.HideState(false, 1F, false, false));
    }

    public static void tick(ServerPlayer p) {
        Hide h = HIDDEN.get(p.getUUID());
        if (h == null) return;
        ServerLevel level = p.serverLevel();
        if (!(level.getBlockState(h.locker).getBlock() instanceof LockerBlock) || p.isSpectator() || !p.isAlive()) {
            release(p);
            return;
        }
        Vec3 c = Vec3.atBottomCenterOf(h.locker);
        if (p.position().distanceToSqr(c) > 0.3 * 0.3) place(p, h);
        p.setDeltaMovement(Vec3.ZERO);

        // breath
        if (h.gaspCooldown > 0) h.gaspCooldown--;
        h.holding = p.isShiftKeyDown() && h.gaspCooldown == 0;
        if (h.holding) {
            h.breath -= 1F / 110F;
            if (h.breath <= 0F) {
                h.breath = 0F;
                h.gaspCooldown = 50;
                h.holding = false;
                level.playSound(null, h.locker, ModSounds.GASP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                NoiseSystem.emit(level, h.locker, 12, NoiseSystem.Kind.VOICE, p);
            }
        } else {
            h.breath = Math.min(1F, h.breath + 1F / 70F);
        }
        if (h.holding && p.tickCount % 30 == 0)
            p.playNotifySound(ModSounds.BREATH_HOLD.get(), SoundSource.PLAYERS, 0.5F, 1.0F);

        h.danger = !level.getEntitiesOfClass(XenomorphEntity.class, new AABB(h.locker).inflate(5.5),
                x -> !x.isHidden() && x.isAlive()).isEmpty();
        if (++h.sync % 2 == 0) sync(p, h);
    }

    private static void sync(ServerPlayer p, Hide h) {
        Net.toPlayer(p, new Net.HideState(true, h.breath, h.danger, h.holding));
    }

    /** the alien rips the door open. */
    public static void forceOut(ServerPlayer p) {
        Hide h = HIDDEN.remove(p.getUUID());
        if (h == null) return;
        LockerBlock.setOccupied(p.serverLevel(), h.locker, false);
        BlockPos out = h.locker.relative(h.facing);
        p.teleportTo(p.serverLevel(), out.getX() + 0.5, out.getY(), out.getZ() + 0.5, h.facing.toYRot(), 0F);
        p.serverLevel().playSound(null, h.locker, ModSounds.LOCKER.get(), SoundSource.BLOCKS, 1.4F, 0.6F);
        Net.toPlayer(p, new Net.HideState(false, 1F, false, false));
    }

    public static void clear() { HIDDEN.clear(); }
}
