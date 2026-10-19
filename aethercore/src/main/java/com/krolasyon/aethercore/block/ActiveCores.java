package com.krolasyon.aethercore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Sunucu tarafında aktif çekirdeklerin konumları (canavar doğmasını engellemek için). */
public final class ActiveCores {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE = new ConcurrentHashMap<>();

    private ActiveCores() {}

    public static void set(Level level, BlockPos pos, boolean active) {
        if (level.isClientSide) return;
        Set<BlockPos> set = ACTIVE.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());
        if (active) set.add(pos.immutable()); else set.remove(pos);
    }

    public static boolean isProtected(Level level, double x, double y, double z) {
        Set<BlockPos> set = ACTIVE.get(level.dimension());
        if (set == null || set.isEmpty()) return false;
        double r2 = AetherCoreBlockEntity.RADIUS * AetherCoreBlockEntity.RADIUS;
        for (BlockPos p : set) {
            double dx = x - (p.getX() + 0.5), dy = y - (p.getY() + 0.5), dz = z - (p.getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz <= r2) return true;
        }
        return false;
    }
}
