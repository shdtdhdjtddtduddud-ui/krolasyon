package com.krolasyon.sololeveling.system;

import java.util.Locale;

/** Korean hunter guilds present in Seoul. */
public enum Guild {
    NONE(0xAAAAAA, Rank.E),
    HUNTERS(0xE8C547, Rank.C),
    WHITE_TIGER(0xF2F2F2, Rank.D),
    FIEND(0xD6453B, Rank.D),
    KNIGHTS(0x4B86E8, Rank.E),
    AHJIN(0x8E5BFF, Rank.A);

    public final int color;
    /** Minimum hunter rank required to join (or found, for Ahjin). */
    public final Rank minRank;

    Guild(int color, Rank minRank) {
        this.color = color;
        this.minRank = minRank;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String langKey() { return "sololeveling.guild." + id(); }

    public static Guild byOrdinal(int o) {
        Guild[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }

    public static int guildRankFor(int rep) {
        if (rep >= 2000) return 4;
        if (rep >= 900) return 3;
        if (rep >= 350) return 2;
        if (rep >= 100) return 1;
        return 0;
    }
}
