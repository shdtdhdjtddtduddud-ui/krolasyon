package com.sololeveling.system;

import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Helpers for "[System]" style messages. */
public final class Sys {
    private Sys() {}

    public static final int INFO = 0, LEVELUP = 1, QUEST = 2, WARN = 3, REWARD = 4, ARISE = 5;

    public static void notify(ServerPlayer sp, int kind, Component title, Component body) {
        Net.toPlayer(sp, new Packets.Notify(kind, title, body));
    }

    public static void notify(ServerPlayer sp, int kind, String titleKey, Component body) {
        notify(sp, kind, Component.translatable(titleKey), body);
    }

    public static void info(ServerPlayer sp, String key, Object... args) {
        notify(sp, INFO, Component.translatable("gui.sololeveling.system"), Component.translatable(key, args));
    }

    public static void warn(ServerPlayer sp, String key, Object... args) {
        notify(sp, WARN, Component.translatable("gui.sololeveling.warning"), Component.translatable(key, args));
    }
}
