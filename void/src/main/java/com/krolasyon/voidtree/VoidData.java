package com.krolasyon.voidtree;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumSet;
import java.util.Set;

/** Per-player skill state. Lives in a capability on the server and is mirrored to the owning client. */
public class VoidData {
    public static final int SLOTS = 4;
    public static final float BASE_MAX_ENERGY = 100F;

    public final Set<Skill> unlocked = EnumSet.noneOf(Skill.class);
    public final Skill[] slots = new Skill[SLOTS];
    public final int[] cooldowns = new int[Skill.VALUES.length];
    public float energy = BASE_MAX_ENERGY;
    /** remaining ticks of the timed skills */
    public int phantomTicks, ascendTicks;

    public boolean has(Skill s) { return unlocked.contains(s); }

    public float maxEnergy() { return BASE_MAX_ENERGY + (has(Skill.VOID_MANTLE) ? 50F : 0F); }

    public float regenPerTick() {
        float r = has(Skill.VOID_MANTLE) ? 0.175F : 0.1F;
        return ascendTicks > 0 ? r * 2F : r;
    }

    public void copyFrom(VoidData o) {
        unlocked.clear();
        unlocked.addAll(o.unlocked);
        System.arraycopy(o.slots, 0, slots, 0, SLOTS);
        System.arraycopy(o.cooldowns, 0, cooldowns, 0, cooldowns.length);
        energy = o.energy;
        phantomTicks = o.phantomTicks;
        ascendTicks = o.ascendTicks;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        int mask = 0;
        for (Skill s : unlocked) mask |= 1 << s.ordinal();
        t.putInt("Unlocked", mask);
        int[] sl = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) sl[i] = slots[i] == null ? -1 : slots[i].ordinal();
        t.putIntArray("Slots", sl);
        t.putIntArray("Cooldowns", cooldowns.clone());
        t.putFloat("Energy", energy);
        t.putInt("Phantom", phantomTicks);
        t.putInt("Ascend", ascendTicks);
        return t;
    }

    public void load(CompoundTag t) {
        unlocked.clear();
        int mask = t.getInt("Unlocked");
        for (Skill s : Skill.VALUES) if ((mask & (1 << s.ordinal())) != 0) unlocked.add(s);
        int[] sl = t.getIntArray("Slots");
        for (int i = 0; i < SLOTS; i++) slots[i] = i < sl.length ? Skill.byId(sl[i]) : null;
        int[] cd = t.getIntArray("Cooldowns");
        for (int i = 0; i < cooldowns.length; i++) cooldowns[i] = i < cd.length ? cd[i] : 0;
        energy = t.contains("Energy") ? t.getFloat("Energy") : BASE_MAX_ENERGY;
        phantomTicks = t.getInt("Phantom");
        ascendTicks = t.getInt("Ascend");
    }
}
