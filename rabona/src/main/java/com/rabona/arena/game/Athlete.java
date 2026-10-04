package com.rabona.arena.game;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Oyuncu/bot sporcu durumu: dayaniklilik, ozel enerji, bekleme sureleri, sersemleme. */
public class Athlete {
    private static final Map<UUID, Athlete> ALL = new HashMap<>();

    public float stamina = 100, energy = 30;
    public final int[] cooldowns = new int[Move.values().length];
    public Move current;
    public int moveTick, side;
    public float power;
    public int stun, evade, ghost, thunder, magnet;
    public int goals, assists, shots, tackles, skills, saves;
    public boolean dirty = true;
    public net.minecraft.world.phys.Vec3 target, lastPos, vel = net.minecraft.world.phys.Vec3.ZERO;
    public float lastSentStamina = -1, lastSentEnergy = -1;

    public static Athlete of(LivingEntity e) {
        return ALL.computeIfAbsent(e.getUUID(), u -> new Athlete());
    }

    public static Athlete peek(UUID id) { return ALL.get(id); }

    public static void remove(UUID id) { ALL.remove(id); }

    public static void clearAll() { ALL.clear(); }

    public static void resetStats() {
        for (Athlete a : ALL.values()) {
            a.goals = a.assists = a.shots = a.tackles = a.skills = a.saves = 0;
            a.stamina = 100;
            a.energy = 30;
        }
    }

    public boolean busy() { return current != null && moveTick < current.duration; }

    public void addEnergy(float f) {
        energy = Math.min(100, energy + f);
        dirty = true;
    }

    public void tick() {
        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
        if (stun > 0) stun--;
        if (evade > 0) evade--;
        if (ghost > 0) ghost--;
        if (thunder > 0) thunder--;
        if (magnet > 0) magnet--;
    }
}
