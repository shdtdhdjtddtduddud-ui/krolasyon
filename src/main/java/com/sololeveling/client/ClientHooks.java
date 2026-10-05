package com.sololeveling.client;

import com.sololeveling.client.gui.NpcScreen;
import com.sololeveling.client.gui.SystemScreen;
import com.sololeveling.player.SLPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-side mirror of the System state plus handlers of server -> client packets. */
public final class ClientHooks {
    private ClientHooks() {}

    public static final SLPlayer data = new SLPlayer();

    public static final class Toast {
        public final int kind;
        public final Component title, body;
        public int age = 0;
        Toast(int k, Component t, Component b) { kind = k; title = t; body = b; }
    }

    public record NewsItem(long time, int type, Component text) {}
    public record GateInfo(String rank, String theme, boolean red, int x, int y, int z) {}

    public static final List<Toast> toasts = new ArrayList<>();
    public static final List<NewsItem> news = new ArrayList<>();
    public static final List<GateInfo> gates = new ArrayList<>();
    public static final Map<String, long[]> cooldowns = new HashMap<>();   // skill -> {end, total}
    public static String fxKind = "";
    public static int fxAge = 0, fxArg = 0;
    public static int selectedSkill = 0;

    public static int playerLevel() { return data.level; }

    public static void sync(CompoundTag t) { data.load(t); }

    public static void notify(int kind, Component title, Component body) {
        toasts.add(new Toast(kind, title, body));
        while (toasts.size() > 5) toasts.remove(0);
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(kind == 5 ? SoundEvents.BEACON_ACTIVATE : (kind == 3 ? SoundEvents.NOTE_BLOCK_BIT.get() : SoundEvents.EXPERIENCE_ORB_PICKUP), kind == 3 ? 0.7F : 1.4F, 0.8F));
    }

    public static void news(CompoundTag t) {
        news.clear();
        for (Tag e : t.getList("e", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) e;
            Component text = Component.Serializer.fromJson(c.getString("j"));
            news.add(new NewsItem(c.getLong("t"), c.getInt("y"), text == null ? Component.empty() : text));
        }
    }

    public static void gates(CompoundTag t) {
        gates.clear();
        for (Tag e : t.getList("g", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) e;
            gates.add(new GateInfo(c.getString("rank"), c.getString("theme"), c.getBoolean("red"), c.getInt("x"), c.getInt("y"), c.getInt("z")));
        }
    }

    public static void openNpc(int entityId, String role, String name, String guild) {
        Minecraft.getInstance().setScreen(new NpcScreen(entityId, role, name, guild));
    }

    public static void openSystem(int tab) { Minecraft.getInstance().setScreen(new SystemScreen(tab)); }

    public static void fx(String kind, int arg) {
        fxKind = kind;
        fxArg = arg;
        fxAge = 0;
    }

    public static void cooldown(String skill, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        cooldowns.put(skill, new long[]{now + ticks, ticks});
    }

    public static float cooldownFrac(String skill) {
        long[] c = cooldowns.get(skill);
        Minecraft mc = Minecraft.getInstance();
        if (c == null || mc.level == null) return 0;
        long left = c[0] - mc.level.getGameTime();
        return left <= 0 ? 0 : (float) left / c[1];
    }
}
