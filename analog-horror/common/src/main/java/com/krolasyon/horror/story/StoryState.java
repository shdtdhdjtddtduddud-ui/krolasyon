package com.krolasyon.horror.story;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-player story progress, persisted in the player's Forge persistent data. */
public final class StoryState {
    private static final String KEY = "krolasyonhorror_story";
    private static final Map<UUID, StoryState> CACHE = new HashMap<>();

    public boolean active, finished;
    public BlockPos origin = BlockPos.ZERO;
    public String step = "intro";
    public int stepTicks, chapterTicks, chapter;
    public final Set<String> flags = new HashSet<>();
    public final Set<Integer> notes = new HashSet<>();
    public double cpX, cpY, cpZ;
    public float cpYaw;
    public int fusesInserted;
    public String candles = "";
    public int[] dials = new int[4];
    // transient
    public boolean media;
    public int appSeen, caughtTimer, knocks, knockTimer, trickTimer = 1600, flickerTimer, respawnTimer, hostTimer;
    public String objective = "";
    public int objectiveArg = -1;

    public static StoryState get(Player p) {
        return CACHE.computeIfAbsent(p.getUUID(), id -> load(p));
    }

    public static void forget(Player p) { CACHE.remove(p.getUUID()); }

    public static boolean isActive(Player p) { return get(p).active; }

    private static StoryState load(Player p) {
        StoryState s = new StoryState();
        CompoundTag t = p.getPersistentData().getCompound(KEY);
        if (t.isEmpty()) return s;
        s.active = t.getBoolean("active");
        s.finished = t.getBoolean("finished");
        s.origin = new BlockPos(t.getInt("ox"), t.getInt("oy"), t.getInt("oz"));
        s.step = t.getString("step");
        s.chapter = t.getInt("chapter");
        s.chapterTicks = t.getInt("chapterTicks");
        ListTag fl = t.getList("flags", Tag.TAG_STRING);
        for (int i = 0; i < fl.size(); i++) s.flags.add(fl.getString(i));
        for (int n : t.getIntArray("notes")) s.notes.add(n);
        s.cpX = t.getDouble("cpX");
        s.cpY = t.getDouble("cpY");
        s.cpZ = t.getDouble("cpZ");
        s.cpYaw = t.getFloat("cpYaw");
        s.fusesInserted = t.getInt("fuses");
        s.candles = t.getString("candles");
        int[] d = t.getIntArray("dials");
        if (d.length == 4) s.dials = d;
        return s;
    }

    public void save(Player p) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("active", active);
        t.putBoolean("finished", finished);
        t.putInt("ox", origin.getX());
        t.putInt("oy", origin.getY());
        t.putInt("oz", origin.getZ());
        t.putString("step", step);
        t.putInt("chapter", chapter);
        t.putInt("chapterTicks", chapterTicks);
        ListTag fl = new ListTag();
        for (String f : flags) fl.add(StringTag.valueOf(f));
        t.put("flags", fl);
        t.putIntArray("notes", notes.stream().mapToInt(Integer::intValue).toArray());
        t.putDouble("cpX", cpX);
        t.putDouble("cpY", cpY);
        t.putDouble("cpZ", cpZ);
        t.putFloat("cpYaw", cpYaw);
        t.putInt("fuses", fusesInserted);
        t.putString("candles", candles);
        t.putIntArray("dials", dials);
        p.getPersistentData().put(KEY, t);
    }

    public BlockPos at(int x, int y, int z) { return origin.offset(x, y, z); }

    public boolean has(String f) { return flags.contains(f); }
}
