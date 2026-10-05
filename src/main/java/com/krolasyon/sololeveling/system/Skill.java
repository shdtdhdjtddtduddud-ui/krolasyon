package com.krolasyon.sololeveling.system;

import java.util.Locale;

/**
 * Every System skill. Active skills can be put in one of the five quick slots; passive skills work on their own.
 * Skills unlock automatically at {@link #unlockLevel} (when the job requirement is met) or earlier through rune stones.
 */
public enum Skill {
    // active
    SPRINT(true, 20, 20 * 20, 1, Job.NONE, 0),
    DAGGER_THROW(true, 15, 3 * 20, 8, Job.NONE, 1),
    BLOODLUST(true, 40, 30 * 20, 15, Job.NONE, 2),
    MUTILATION(true, 30, 8 * 20, 20, Job.NONE, 3),
    STEALTH(true, 50, 40 * 20, 25, Job.NONE, 4),
    VITAL_STRIKE(true, 45, 14 * 20, 32, Job.NONE, 5),
    SHADOW_EXCHANGE(true, 40, 10 * 20, 45, Job.NECROMANCER, 6),
    RULERS_AUTHORITY(true, 60, 12 * 20, 50, Job.NECROMANCER, 7),
    DAGGER_STORM(true, 80, 25 * 20, 60, Job.NONE, 8),
    DRAGONS_FEAR(true, 100, 60 * 20, 80, Job.NONE, 9),
    MONARCHS_DOMAIN(true, 150, 90 * 20, 90, Job.SHADOW_MONARCH, 10),
    // passive
    TENACITY(false, 0, 0, 1, Job.NONE, 11),
    ADVANCED_DAGGER_ARTS(false, 0, 0, 10, Job.NONE, 12),
    DETOXIFICATION(false, 0, 0, 30, Job.NONE, 13),
    LONG_RANGE_DETECTION(false, 0, 0, 35, Job.NONE, 14),
    SHADOW_PRESERVATION(false, 0, 0, 40, Job.NECROMANCER, 15);

    public final boolean active;
    public final int mana;
    public final int cooldown;
    public final int unlockLevel;
    public final Job job;
    /** Index into the skill icon atlas. */
    public final int icon;

    Skill(boolean active, int mana, int cooldown, int unlockLevel, Job job, int icon) {
        this.active = active;
        this.mana = mana;
        this.cooldown = cooldown;
        this.unlockLevel = unlockLevel;
        this.job = job;
        this.icon = icon;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String langKey() { return "sololeveling.skill." + id(); }

    public String descKey() { return "sololeveling.skill." + id() + ".desc"; }

    public static Skill byId(String id) {
        for (Skill s : values()) if (s.id().equals(id)) return s;
        return null;
    }

    public static Skill byOrdinal(int o) {
        Skill[] v = values();
        return o >= 0 && o < v.length ? v[o] : null;
    }
}
