package com.krolasyon.omnitrix;

import net.minecraft.nbt.CompoundTag;

/** Per-player Omnitrix state (server authoritative, mirrored to the owner). */
public class OmniData {
    /** transformation length and recharge time, ticks */
    public static final int TRANSFORM_TIME = 20 * 180, RECHARGE_TIME = 20 * 20;

    public boolean wearing;
    public int alien = -1;
    public int timer, recharge;
    public final int[] cooldowns = new int[Ability.VALUES.length];
    public boolean blocking;
    /** timed ability states */
    public int flightTicks, phaseTicks, puddleTicks, overdriveTicks, bladesTicks;
    /** server only: last cast for the animation */
    public int castAbility = -1;
    public long castTime, transformTime, detransformTime;

    public Alien current() { return Alien.byId(alien); }

    public boolean transformed() { return alien >= 0; }

    public void clearStates() {
        flightTicks = phaseTicks = puddleTicks = overdriveTicks = bladesTicks = 0;
        blocking = false;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Wearing", wearing);
        t.putInt("Alien", alien);
        t.putInt("Timer", timer);
        t.putInt("Recharge", recharge);
        t.putIntArray("Cooldowns", cooldowns.clone());
        t.putInt("Flight", flightTicks);
        t.putInt("Phase", phaseTicks);
        t.putInt("Puddle", puddleTicks);
        t.putInt("Overdrive", overdriveTicks);
        t.putInt("Blades", bladesTicks);
        t.putBoolean("Blocking", blocking);
        return t;
    }

    public void load(CompoundTag t) {
        wearing = t.getBoolean("Wearing");
        alien = t.contains("Alien") ? t.getInt("Alien") : -1;
        timer = t.getInt("Timer");
        recharge = t.getInt("Recharge");
        int[] cd = t.getIntArray("Cooldowns");
        for (int i = 0; i < cooldowns.length; i++) cooldowns[i] = i < cd.length ? cd[i] : 0;
        flightTicks = t.getInt("Flight");
        phaseTicks = t.getInt("Phase");
        puddleTicks = t.getInt("Puddle");
        overdriveTicks = t.getInt("Overdrive");
        bladesTicks = t.getInt("Blades");
        blocking = t.getBoolean("Blocking");
    }

    public void copyFrom(OmniData o) { load(o.save()); }

    /** what other clients need to draw this player */
    public CompoundTag appearance() {
        CompoundTag t = new CompoundTag();
        t.putByte("A", (byte) alien);
        t.putBoolean("W", wearing);
        t.putBoolean("B", blocking);
        int flags = (flightTicks > 0 ? 1 : 0) | (phaseTicks > 0 ? 2 : 0) | (puddleTicks > 0 ? 4 : 0) | (overdriveTicks > 0 ? 8 : 0) | (bladesTicks > 0 ? 16 : 0);
        t.putByte("F", (byte) flags);
        t.putByte("C", (byte) castAbility);
        t.putLong("CT", castTime);
        t.putLong("TT", transformTime);
        t.putLong("DT", detransformTime);
        return t;
    }
}
