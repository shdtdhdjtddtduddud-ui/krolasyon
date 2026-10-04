package com.krolasyon.bosses.rpg.data;

import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

/** World-wide RPG state: map, settlements, kingdom politics and every player's progress. Lives on the overworld. */
public class RpgWorldData extends SavedData {
    private static final String NAME = "krolasyon_rpg";

    public boolean initialized;
    public BlockPos origin = BlockPos.ZERO;
    public final int[] capX = new int[Kingdom.COUNT];
    public final int[] capZ = new int[Kingdom.COUNT];
    public final List<Site> sites = new ArrayList<>();
    public final Map<UUID, PlayerRpg> players = new HashMap<>();
    /** -100 (blood feud) .. 100 (sworn allies) */
    public final int[][] relations = new int[Kingdom.COUNT][Kingdom.COUNT];
    public final boolean[][] war = new boolean[Kingdom.COUNT][Kingdom.COUNT];
    public final int[] warPoints = new int[Kingdom.COUNT];
    public long day = -1;
    public final Map<String, UUID> unique = new HashMap<>();
    public final LinkedList<String> news = new LinkedList<>();
    public boolean kingDead;
    /** named story positions (family home, temple, palace...) */
    public final Map<String, Long> anchors = new HashMap<>();

    public static RpgWorldData get(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        return ow.getDataStorage().computeIfAbsent(RpgWorldData::load, RpgWorldData::new, NAME);
    }

    public static RpgWorldData get(ServerLevel level) { return get(level.getServer()); }

    public static PlayerRpg player(Player p) {
        RpgWorldData d = get(((ServerPlayer) p).getServer());
        PlayerRpg r = d.players.computeIfAbsent(p.getUUID(), PlayerRpg::new);
        d.setDirty();
        return r;
    }

    public void news(String s) {
        news.addFirst(s);
        while (news.size() > 20) news.removeLast();
        setDirty();
    }

    @Nullable
    public Site site(String id) {
        for (Site s : sites) if (s.id.equals(id)) return s;
        return null;
    }

    @Nullable
    public BlockPos anchor(String key) {
        Long l = anchors.get(key);
        return l == null ? null : BlockPos.of(l);
    }

    public void setAnchor(String key, BlockPos pos) {
        anchors.put(key, pos.asLong());
        setDirty();
    }

    public boolean atWar(int a, int b) { return a >= 0 && b >= 0 && a != b && war[a][b]; }

    public void setWar(int a, int b, boolean w) {
        war[a][b] = w;
        war[b][a] = w;
        setDirty();
    }

    public void changeRelation(int a, int b, int d) {
        if (a < 0 || b < 0 || a == b) return;
        relations[a][b] = Math.max(-100, Math.min(100, relations[a][b] + d));
        relations[b][a] = relations[a][b];
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putBoolean("Init", initialized);
        t.putLong("Origin", origin.asLong());
        t.putIntArray("CapX", capX);
        t.putIntArray("CapZ", capZ);
        ListTag s = new ListTag();
        for (Site x : sites) s.add(x.save());
        t.put("Sites", s);
        ListTag p = new ListTag();
        for (PlayerRpg x : players.values()) p.add(x.save());
        t.put("Players", p);
        int[] rel = new int[Kingdom.COUNT * Kingdom.COUNT];
        int[] w = new int[Kingdom.COUNT * Kingdom.COUNT];
        for (int i = 0; i < Kingdom.COUNT; i++) for (int j = 0; j < Kingdom.COUNT; j++) {
            rel[i * Kingdom.COUNT + j] = relations[i][j];
            w[i * Kingdom.COUNT + j] = war[i][j] ? 1 : 0;
        }
        t.putIntArray("Relations", rel);
        t.putIntArray("War", w);
        t.putIntArray("WarPoints", warPoints);
        t.putLong("Day", day);
        CompoundTag u = new CompoundTag();
        for (Map.Entry<String, UUID> e : unique.entrySet()) u.putUUID(e.getKey(), e.getValue());
        t.put("Unique", u);
        ListTag n = new ListTag();
        for (String x : news) n.add(StringTag.valueOf(x));
        t.put("News", n);
        t.putBoolean("KingDead", kingDead);
        CompoundTag an = new CompoundTag();
        for (Map.Entry<String, Long> e : anchors.entrySet()) an.putLong(e.getKey(), e.getValue());
        t.put("Anchors", an);
        return t;
    }

    public static RpgWorldData load(CompoundTag t) {
        RpgWorldData d = new RpgWorldData();
        d.initialized = t.getBoolean("Init");
        d.origin = BlockPos.of(t.getLong("Origin"));
        int[] cx = t.getIntArray("CapX"), cz = t.getIntArray("CapZ");
        System.arraycopy(cx, 0, d.capX, 0, Math.min(cx.length, Kingdom.COUNT));
        System.arraycopy(cz, 0, d.capZ, 0, Math.min(cz.length, Kingdom.COUNT));
        for (Tag x : t.getList("Sites", Tag.TAG_COMPOUND)) d.sites.add(Site.load((CompoundTag) x));
        for (Tag x : t.getList("Players", Tag.TAG_COMPOUND)) {
            PlayerRpg p = PlayerRpg.load((CompoundTag) x);
            d.players.put(p.id, p);
        }
        int[] rel = t.getIntArray("Relations"), w = t.getIntArray("War");
        if (rel.length == Kingdom.COUNT * Kingdom.COUNT) for (int i = 0; i < Kingdom.COUNT; i++) for (int j = 0; j < Kingdom.COUNT; j++) {
            d.relations[i][j] = rel[i * Kingdom.COUNT + j];
            d.war[i][j] = w.length == rel.length && w[i * Kingdom.COUNT + j] != 0;
        }
        int[] wp = t.getIntArray("WarPoints");
        System.arraycopy(wp, 0, d.warPoints, 0, Math.min(wp.length, Kingdom.COUNT));
        d.day = t.getLong("Day");
        CompoundTag u = t.getCompound("Unique");
        for (String k : u.getAllKeys()) if (u.hasUUID(k)) d.unique.put(k, u.getUUID(k));
        for (Tag x : t.getList("News", Tag.TAG_STRING)) d.news.add(x.getAsString());
        d.kingDead = t.getBoolean("KingDead");
        CompoundTag an = t.getCompound("Anchors");
        for (String k : an.getAllKeys()) d.anchors.put(k, an.getLong(k));
        return d;
    }
}
