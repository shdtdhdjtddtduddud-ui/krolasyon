package com.krolasyon.alien.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Everything audible in the station is published here: footsteps, doors, devices, gunfire, gasps...
 * The xenomorph polls recent noises every tick and decides whether they are worth investigating.
 */
public final class NoiseSystem {
    private NoiseSystem() {}

    public enum Kind { STEP, RUN, LAND, DOOR, LOCKER, DEVICE, COMBAT, FIRE, NOISEMAKER, FLARE, VOICE, TRACKER }

    public record Noise(Vec3 pos, double radius, Kind kind, @Nullable UUID source, long time) {}

    private static final Map<ResourceKey<Level>, ArrayDeque<Noise>> NOISES = new HashMap<>();

    public static void emit(ServerLevel level, BlockPos pos, double radius, Kind kind, @Nullable Entity source) {
        emit(level, Vec3.atCenterOf(pos), radius, kind, source);
    }

    public static void emit(ServerLevel level, Vec3 pos, double radius, Kind kind, @Nullable Entity source) {
        ArrayDeque<Noise> q = NOISES.computeIfAbsent(level.dimension(), k -> new ArrayDeque<>());
        q.addLast(new Noise(pos, radius, kind, source == null ? null : source.getUUID(), level.getGameTime()));
        while (q.size() > 256) q.removeFirst();
    }

    /** noises newer than {@code after} (game time). */
    public static List<Noise> since(ServerLevel level, long after) {
        ArrayDeque<Noise> q = NOISES.get(level.dimension());
        if (q == null || q.isEmpty()) return List.of();
        long now = level.getGameTime();
        while (!q.isEmpty() && now - q.peekFirst().time() > 60) q.removeFirst();
        List<Noise> out = new ArrayList<>();
        for (Iterator<Noise> it = q.descendingIterator(); it.hasNext(); ) {
            Noise n = it.next();
            if (n.time() <= after) break;
            out.add(n);
        }
        return out;
    }

    public static void clear() { NOISES.clear(); }
}
