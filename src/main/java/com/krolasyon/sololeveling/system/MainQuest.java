package com.krolasyon.sololeveling.system;

import java.util.Locale;

/** The main story chain the System walks the player through. */
public enum MainQuest {
    AWAKEN(0, 0),
    REGISTER(0, 200),
    FIRST_GATE(0, 400),
    DOUBLE_DUNGEON(0, 800),
    REACH_20(20, 600),
    KASAKA(0, 1000),
    JOB_CHANGE(40, 2500),
    RED_GATE(0, 2500),
    ORC_CHIEFTAIN(0, 3000),
    DEMON_CASTLE(0, 5000),
    JEJU(0, 8000),
    MONARCH(100, 20000),
    DONE(0, 0);

    /** Level needed (for level goals / gated content). */
    public final int level;
    public final int goldReward;

    MainQuest(int level, int goldReward) {
        this.level = level;
        this.goldReward = goldReward;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String langKey() { return "sololeveling.quest." + id(); }

    public String descKey() { return "sololeveling.quest." + id() + ".desc"; }

    public MainQuest next() { return this == DONE ? DONE : values()[ordinal() + 1]; }

    public static MainQuest byOrdinal(int o) {
        MainQuest[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }
}
