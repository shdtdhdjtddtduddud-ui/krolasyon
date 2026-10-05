package com.krolasyon.sololeveling.system;

import java.util.Locale;

/** Titles granted by the System. Only the equipped title gives its bonus. */
public enum Title {
    NONE,
    WOLF_ASSASSIN,      // +40% damage against beasts
    ONE_WHO_OVERCAME,   // +30% all damage while below 30% health
    DEMON_HUNTER,       // +40% damage against demons
    KING_SLAYER,        // +15% damage against bosses
    ANT_EXTERMINATOR,   // +40% damage against insects
    SHADOW_MONARCH;     // +10% everything, shadows +20%

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String langKey() { return "sololeveling.title." + id(); }

    public static Title byOrdinal(int o) {
        Title[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
