package com.sololeveling.system;

import com.sololeveling.SoloLeveling;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Hunter Network news feed (persisted). */
public class NewsManager extends SavedData {
    public static final int GENERAL = 0, GATE = 1, CLEARED = 2, BREAK = 3, HUNTER = 4, BOSS = 5, LORE = 6;
    private static final int MAX = 60;

    public record Entry(long time, int type, String json) {}

    public final List<Entry> entries = new ArrayList<>();

    public static NewsManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NewsManager::load, () -> {
            NewsManager m = new NewsManager();
            m.seed();
            return m;
        }, SoloLeveling.MODID + "_news");
    }

    private void seed() {
        for (int i = 1; i <= 6; i++)
            entries.add(new Entry(0, LORE, Component.Serializer.toJson(Component.translatable("news.sololeveling.lore" + i))));
        setDirty();
    }

    public static NewsManager load(CompoundTag t) {
        NewsManager m = new NewsManager();
        for (Tag e : t.getList("e", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) e;
            m.entries.add(new Entry(c.getLong("t"), c.getInt("y"), c.getString("j")));
        }
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        ListTag l = new ListTag();
        for (Entry e : entries) {
            CompoundTag c = new CompoundTag();
            c.putLong("t", e.time); c.putInt("y", e.type); c.putString("j", e.json);
            l.add(c);
        }
        t.put("e", l);
        return t;
    }

    public static void add(MinecraftServer server, int type, Component text) {
        NewsManager m = get(server);
        m.entries.add(new Entry(server.overworld().getGameTime(), type, Component.Serializer.toJson(text)));
        while (m.entries.size() > MAX) m.entries.remove(0);
        m.setDirty();
        Component msg = Component.empty().append(Component.translatable("gui.sololeveling.breaking_news").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(" ")).append(text.copy().withStyle(ChatFormatting.WHITE));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
            Net.toPlayer(p, new Packets.News(m.toTag()));
        }
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        int from = Math.max(0, entries.size() - 40);
        for (int i = entries.size() - 1; i >= from; i--) {
            Entry e = entries.get(i);
            CompoundTag c = new CompoundTag();
            c.putLong("t", e.time); c.putInt("y", e.type); c.putString("j", e.json);
            l.add(c);
        }
        t.put("e", l);
        return t;
    }

    public static void send(ServerPlayer sp) {
        Net.toPlayer(sp, new Packets.News(get(sp.server).toTag()));
    }
}
