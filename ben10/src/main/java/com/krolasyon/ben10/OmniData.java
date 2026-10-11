package com.krolasyon.ben10;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player Omnitrix state. The server copy is authoritative; clients receive a mirror through {@link com.krolasyon.ben10.net.Net.Sync}. */
public final class OmniData {
    private static final Map<UUID, OmniData> SERVER = new HashMap<>();
    private static final Map<UUID, OmniData> CLIENT = new HashMap<>();

    // runtime flags
    public static final int F_ROLLING = 1, F_PHASED = 2, F_OVERDRIVE = 4, F_ENGULF = 8, F_GRAB = 16, F_POSSESS = 32, F_CHARGING = 64, F_AIRBORNE = 128, F_WATCH = 1 << 20;

    public Alien alien;
    public int timeLeft;
    public int recharge;
    public final int[] cooldown = new int[4];
    public final int[] cooldownMax = new int[4];
    public int ability = -1;
    public int abilityTick;
    public boolean blocking;
    public int flags;
    public int targetId = -1;
    public Vec3 vec = Vec3.ZERO;
    public int counter;
    public int sinceTransform = 1000;
    public int beep;
    public boolean dirty;

    // client only: game time when the current ability started (for smooth animation)
    public long clientAbilityStart;
    public long clientTransformTime = -1000;
    public Alien clientPrevAlien;
    public float blockAmount, blockAmountO;

    public static OmniData get(Player p) {
        Map<UUID, OmniData> m = p.level().isClientSide ? CLIENT : SERVER;
        return m.computeIfAbsent(p.getUUID(), u -> new OmniData());
    }

    public static OmniData peekClient(UUID id) {
        return CLIENT.get(id);
    }

    public static void forget(Player p) {
        (p.level().isClientSide ? CLIENT : SERVER).remove(p.getUUID());
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public boolean has(int f) {
        return (flags & f) != 0;
    }

    public void set(int f, boolean on) {
        flags = on ? flags | f : flags & ~f;
        dirty = true;
    }

    public boolean busy() {
        return ability >= 0;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("alien", alien == null ? -1 : alien.ordinal());
        t.putInt("time", timeLeft);
        t.putInt("recharge", recharge);
        return t;
    }

    public void load(CompoundTag t) {
        alien = t.contains("alien") ? Alien.byId(t.getInt("alien")) : null;
        timeLeft = t.getInt("time");
        recharge = t.getInt("recharge");
    }
}
