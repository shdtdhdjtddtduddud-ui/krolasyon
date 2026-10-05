package com.krolasyon.sololeveling.system;

import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Helpers to talk to the player "as the System". */
public final class Sys {
    public static final int INFO = 0, LEVEL = 1, QUEST = 2, WARN = 3, NEWS = 4, REWARD = 5, ARISE = 6;

    private Sys() {}

    public static Component t(String key, Object... args) { return Component.translatable("sololeveling." + key, args); }

    public static void notify(ServerPlayer p, int kind, Component title, Component body) {
        int color = switch (kind) {
            case WARN -> 0xFF4455;
            case NEWS -> 0xFFB347;
            case REWARD -> 0xFFE066;
            case LEVEL -> 0x7FE3FF;
            case ARISE -> 0xA77BFF;
            default -> 0x5BC8FF;
        };
        Net.to(p, new Net.Notify(kind, title, body, color));
        SoundEvent s = switch (kind) {
            case LEVEL -> ModSounds.LEVEL_UP.get();
            case WARN -> ModSounds.SYSTEM_WARN.get();
            case NEWS -> ModSounds.NEWS.get();
            default -> ModSounds.SYSTEM.get();
        };
        p.playNotifySound(s, SoundSource.MASTER, 0.8F, 1.0F);
    }

    public static void info(ServerPlayer p, String key, Object... args) {
        notify(p, INFO, t("system.title"), t(key, args));
    }

    public static void warn(ServerPlayer p, String key, Object... args) {
        notify(p, WARN, t("system.warning"), t(key, args));
    }

    public static void sync(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        Net.to(p, new Net.Sync(d.save()));
        d.dirty = false;
    }
}
