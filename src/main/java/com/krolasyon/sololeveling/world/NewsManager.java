package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.system.Guild;
import com.krolasyon.sololeveling.system.Rank;
import com.krolasyon.sololeveling.system.Sys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** The living world's news feed: gates, raids, dungeon breaks, hunters ranking up and daily headlines. */
public class NewsManager extends SavedData {
    public static final int MAX = 40;
    private final List<CompoundTag> news = new ArrayList<>();
    private int flavorTimer = 1200;

    public static NewsManager get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(NewsManager::load, NewsManager::new, "sololeveling_news");
    }

    static NewsManager load(CompoundTag t) {
        NewsManager n = new NewsManager();
        for (Tag x : t.getList("news", Tag.TAG_COMPOUND)) n.news.add((CompoundTag) x);
        return n;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        ListTag l = new ListTag();
        l.addAll(news);
        t.put("news", l);
        return t;
    }

    /** cat: 0 gate, 1 raid, 2 break, 3 hunter, 4 guild, 5 world */
    public void post(MinecraftServer s, int cat, Component headline, boolean broadcast) {
        CompoundTag e = new CompoundTag();
        e.putLong("day", s.overworld().getDayTime() / 24000L + 1);
        e.putInt("cat", cat);
        e.putString("text", Component.Serializer.toJson(headline));
        news.add(0, e);
        while (news.size() > MAX) news.remove(news.size() - 1);
        setDirty();
        if (broadcast) for (ServerPlayer p : s.getPlayerList().getPlayers()) Sys.notify(p, Sys.NEWS, Sys.t("news.breaking"), headline);
    }

    public void tick(MinecraftServer s) {
        if (--flavorTimer > 0) return;
        flavorTimer = 20 * 60 * 4 + s.overworld().random.nextInt(20 * 60 * 4);
        if (s.getPlayerList().getPlayerCount() == 0) return;
        var r = s.overworld().random;
        int k = r.nextInt(24);
        post(s, 5, Sys.t("news.flavor." + k), false);
    }

    public void gateAppeared(MinecraftServer s, Rank rank, boolean red, String where) {
        post(s, 0, Sys.t(red ? "news.red_gate" : "news.gate", rank.label, Component.translatable("sololeveling.place." + where)), rank.ordinal() >= Rank.A.ordinal() || red);
    }

    public void gateCleared(MinecraftServer s, Rank rank, Component by) {
        post(s, 1, Sys.t("news.cleared", rank.label, by), rank.ordinal() >= Rank.B.ordinal());
    }

    public void dungeonBreak(MinecraftServer s, Rank rank, String where) {
        post(s, 2, Sys.t("news.break", rank.label, Component.translatable("sololeveling.place." + where)), true);
    }

    public void playerMilestone(ServerPlayer p, int level) {
        post(p.server, 3, Sys.t("news.milestone", p.getName(), level), level >= 50);
    }

    public void rankUp(ServerPlayer p, Rank r) {
        post(p.server, 3, Sys.t(r == Rank.S ? "news.new_s_rank" : "news.rank_up", p.getName(), Component.translatable(r.langKey())), r.ordinal() >= Rank.A.ordinal());
    }

    public void guildJoin(ServerPlayer p, Guild g) {
        post(p.server, 4, Sys.t(g == Guild.AHJIN ? "news.ahjin" : "news.guild_join", p.getName(), Component.translatable(g.langKey())), g == Guild.AHJIN);
    }

    public void monarch(ServerPlayer p) {
        post(p.server, 3, Sys.t("news.monarch", p.getName()), true);
    }

    public void open(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        l.addAll(news);
        t.put("news", l);
        t.put("gates", GateManager.mapList(p.server));
        Net.to(p, new Net.Open("news", t));
    }
}
