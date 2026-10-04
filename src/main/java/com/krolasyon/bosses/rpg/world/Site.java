package com.krolasyon.bosses.rpg.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** A place in the world that gets built when a player first comes near: capitals, towns, villages, camps, boss lairs. */
public class Site {
    public enum Type {
        CAPITAL("Başkent", 92), CITY("Şehir", 60), VILLAGE("Köy", 38), CAMP("Haydut Kampı", 20), LAIR("İn", 22), RUIN("Harabe", 18);

        public final String title;
        public final int radius;

        Type(String title, int radius) { this.title = title; this.radius = radius; }
    }

    public String id = "";
    public String name = "";
    public Type type = Type.VILLAGE;
    public int kingdom = -1;
    public int x, z;
    public int y = Integer.MIN_VALUE;
    public boolean built;
    public long seed;
    /** for lairs: monster id of the boss, and the game time it may respawn */
    public String boss = "";
    public long bossRespawn;
    public boolean bossAlive;

    public BlockPos center() { return new BlockPos(x, y == Integer.MIN_VALUE ? 64 : y, z); }

    public double distSq(double px, double pz) { return (px - x) * (px - x) + (pz - z) * (pz - z); }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Id", id);
        t.putString("Name", name);
        t.putString("Type", type.name());
        t.putInt("Kingdom", kingdom);
        t.putInt("X", x);
        t.putInt("Y", y);
        t.putInt("Z", z);
        t.putBoolean("Built", built);
        t.putLong("Seed", seed);
        t.putString("Boss", boss);
        t.putLong("BossRespawn", bossRespawn);
        t.putBoolean("BossAlive", bossAlive);
        return t;
    }

    public static Site load(CompoundTag t) {
        Site s = new Site();
        s.id = t.getString("Id");
        s.name = t.getString("Name");
        try { s.type = Type.valueOf(t.getString("Type")); } catch (IllegalArgumentException ignored) {}
        s.kingdom = t.getInt("Kingdom");
        s.x = t.getInt("X");
        s.y = t.getInt("Y");
        s.z = t.getInt("Z");
        s.built = t.getBoolean("Built");
        s.seed = t.getLong("Seed");
        s.boss = t.getString("Boss");
        s.bossRespawn = t.getLong("BossRespawn");
        s.bossAlive = t.getBoolean("BossAlive");
        return s;
    }
}
