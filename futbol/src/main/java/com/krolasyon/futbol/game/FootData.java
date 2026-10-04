package com.krolasyon.futbol.game;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/** Per-entity football state on the server (cooldowns, evasion windows). */
public final class FootData {
    public long busyUntil;
    public long superReadyAt;
    public long evadeUntil;
    public long stunnedUntil;
    public int lastMove = -1;
    public final java.util.EnumMap<Move, Long> readyAt = new java.util.EnumMap<>(Move.class);
    public long passRequestUntil;

    private static final Map<LivingEntity, FootData> DATA = new WeakHashMap<>();

    public static FootData of(LivingEntity e) { return DATA.computeIfAbsent(e, k -> new FootData()); }
}
