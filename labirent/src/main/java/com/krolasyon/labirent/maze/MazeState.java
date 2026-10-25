package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.story.Objective;
import com.krolasyon.labirent.team.Role;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Everything about the run that must survive a restart. Stored with the overworld data. */
public class MazeState extends SavedData {
    public static final String NAME = "labirent_state";

    // flags for one-shot story events
    public static final int EV_NPCS = 1, EV_BEN = 2, EV_ALBY_WAKE = 4, EV_TERESA = 8, EV_SUN_GONE = 16, EV_HOLE_HINT = 32,
            EV_CHAMBER_WAVE = 64, EV_ESCAPED = 128, EV_CHUCK = 256, EV_FIRST_DOORS = 512;

    public boolean built;
    public long seed;
    @Nullable public MazeLayout layout;
    public int lastShiftNight = -1;
    public int shiftCount;
    public int shiftedSectorsMask;
    @Nullable public UUID captain;
    public String captainName = "";
    public final Map<UUID, Role> roles = new HashMap<>();
    public final Map<UUID, String> names = new HashMap<>();
    public final Set<UUID> arrived = new HashSet<>();
    public int chapter;
    public final int[] progress = new int[Objective.values().length];
    public int lastArrivalDay = -1;
    public int arrivals;
    public int events;
    public int stonesFound;   // bitmask of sectors whose code stone was found
    public int wordsFound;    // bitmask of sectors whose word was revealed
    public int sunGoneNight = -1;
    public int lastSurvivedNight = -1;
    public int memoriesUnlocked;
    /** map knowledge per unit: bit0 known, bit1 wall, bit2 fresh (seen since the last shift) */
    public byte[] known = new byte[MazeLayout.G * MazeLayout.G];

    public static MazeState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MazeState::load, MazeState::new, NAME);
    }

    public boolean has(int ev) { return (events & ev) != 0; }

    public void set(int ev) { events |= ev; setDirty(); }

    public Role role(UUID id) { return roles.getOrDefault(id, Role.SERBEST); }

    public int progress(Objective o) { return progress[o.ordinal()]; }

    public static MazeState load(CompoundTag t) {
        MazeState s = new MazeState();
        s.built = t.getBoolean("built");
        s.seed = t.getLong("seed");
        if (t.contains("layout")) s.layout = MazeLayout.fromBytes(t.getByteArray("layout"));
        s.lastShiftNight = t.getInt("lastShiftNight");
        s.shiftCount = t.getInt("shiftCount");
        s.shiftedSectorsMask = t.getInt("shiftedMask");
        if (t.hasUUID("captain")) s.captain = t.getUUID("captain");
        s.captainName = t.getString("captainName");
        ListTag rl = t.getList("roles", Tag.TAG_COMPOUND);
        for (int k = 0; k < rl.size(); k++) {
            CompoundTag e = rl.getCompound(k);
            UUID id = e.getUUID("id");
            s.roles.put(id, Role.byId(e.getInt("role")));
            s.names.put(id, e.getString("name"));
        }
        ListTag al = t.getList("arrived", Tag.TAG_COMPOUND);
        for (int k = 0; k < al.size(); k++) s.arrived.add(al.getCompound(k).getUUID("id"));
        s.chapter = t.getInt("chapter");
        int[] p = t.getIntArray("progress");
        System.arraycopy(p, 0, s.progress, 0, Math.min(p.length, s.progress.length));
        s.lastArrivalDay = t.contains("lastArrivalDay") ? t.getInt("lastArrivalDay") : -1;
        s.arrivals = t.getInt("arrivals");
        s.events = t.getInt("events");
        s.stonesFound = t.getInt("stones");
        s.wordsFound = t.getInt("words");
        s.sunGoneNight = t.contains("sunGoneNight") ? t.getInt("sunGoneNight") : -1;
        s.lastSurvivedNight = t.contains("lastSurvived") ? t.getInt("lastSurvived") : -1;
        s.memoriesUnlocked = t.getInt("memories");
        byte[] k = t.getByteArray("known");
        if (k.length == s.known.length) s.known = k;
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putBoolean("built", built);
        t.putLong("seed", seed);
        if (layout != null) t.putByteArray("layout", layout.toBytes());
        t.putInt("lastShiftNight", lastShiftNight);
        t.putInt("shiftCount", shiftCount);
        t.putInt("shiftedMask", shiftedSectorsMask);
        if (captain != null) t.putUUID("captain", captain);
        t.putString("captainName", captainName);
        ListTag rl = new ListTag();
        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            c.putInt("role", e.getValue().ordinal());
            c.putString("name", names.getOrDefault(e.getKey(), "?"));
            rl.add(c);
        }
        t.put("roles", rl);
        ListTag al = new ListTag();
        for (UUID id : arrived) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            al.add(c);
        }
        t.put("arrived", al);
        t.putInt("chapter", chapter);
        t.putIntArray("progress", progress);
        t.putInt("lastArrivalDay", lastArrivalDay);
        t.putInt("arrivals", arrivals);
        t.putInt("events", events);
        t.putInt("stones", stonesFound);
        t.putInt("words", wordsFound);
        t.putInt("sunGoneNight", sunGoneNight);
        t.putInt("lastSurvived", lastSurvivedNight);
        t.putInt("memories", memoriesUnlocked);
        t.putByteArray("known", known);
        return t;
    }
}
