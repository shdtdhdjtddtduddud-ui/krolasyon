package com.krolasyon.sololeveling.system;

import java.util.Locale;

public enum Job {
    NONE(0), NECROMANCER(8), SHADOW_MONARCH(40);

    /** Base number of shadows the player may keep. */
    public final int shadowSlots;

    Job(int shadowSlots) { this.shadowSlots = shadowSlots; }

    public String langKey() { return "sololeveling.job." + name().toLowerCase(Locale.ROOT); }

    public static Job byOrdinal(int o) {
        Job[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
