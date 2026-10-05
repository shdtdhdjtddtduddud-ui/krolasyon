package com.krolasyon.sololeveling.entity;

import com.krolasyon.sololeveling.system.Rank;

import java.util.Locale;

/** Every monster of the Solo Leveling world with its base stats. */
public enum MobKind {
    //            rank     hp    dmg  arm  speed  xp     category            boss   w     h     shadow name
    GOBLIN(Rank.E, 16, 3, 0, 0.30, 12, Category.HUMANOID, false, 0.6F, 1.25F, null),
    HOBGOBLIN(Rank.E, 34, 5, 3, 0.27, 24, Category.HUMANOID, false, 0.85F, 2.1F, null),
    STEEL_FANGED_LYCAN(Rank.D, 44, 6, 3, 0.36, 40, Category.BEAST, false, 0.95F, 1.45F, null),
    GIANT_CENTIPEDE(Rank.D, 56, 7, 4, 0.30, 50, Category.INSECT, false, 1.4F, 0.9F, null),
    KASAKA(Rank.C, 320, 11, 6, 0.27, 450, Category.BEAST, true, 2.2F, 2.0F, null),
    STONE_STATUE(Rank.C, 90, 9, 10, 0.25, 90, Category.CONSTRUCT, false, 0.9F, 2.5F, null),
    STATUE_OF_GOD(Rank.B, 1000, 16, 14, 0.0, 2600, Category.CONSTRUCT, true, 3.6F, 8.8F, null),
    CASTLE_KNIGHT(Rank.B, 130, 11, 12, 0.28, 150, Category.KNIGHT, false, 0.8F, 2.15F, null),
    IGRIS(Rank.A, 900, 18, 16, 0.33, 3200, Category.KNIGHT, true, 0.9F, 2.55F, "Igris"),
    ICE_ELF(Rank.B, 95, 9, 4, 0.32, 130, Category.HUMANOID, false, 0.6F, 1.95F, null),
    ICE_BEAR(Rank.B, 170, 14, 6, 0.30, 190, Category.BEAST, false, 1.7F, 1.95F, "Tank"),
    BARUKA(Rank.A, 800, 17, 8, 0.38, 2800, Category.HUMANOID, true, 0.7F, 2.05F, null),
    HIGH_ORC(Rank.A, 210, 16, 10, 0.29, 280, Category.HUMANOID, false, 1.0F, 2.6F, null),
    KARGALGAN(Rank.A, 1000, 14, 10, 0.27, 3600, Category.HUMANOID, true, 1.0F, 2.75F, "Tusk"),
    DEMON(Rank.S, 230, 18, 10, 0.31, 340, Category.DEMON, false, 0.85F, 2.35F, null),
    CERBERUS(Rank.S, 1600, 22, 14, 0.33, 5200, Category.BEAST, true, 2.6F, 2.7F, null),
    BARAN(Rank.S, 2600, 28, 18, 0.30, 12000, Category.DEMON, true, 1.4F, 3.4F, null),
    ANT_SOLDIER(Rank.S, 290, 20, 14, 0.36, 420, Category.INSECT, false, 1.0F, 2.4F, null),
    BERU(Rank.NATIONAL, 3400, 34, 18, 0.40, 16000, Category.INSECT, true, 1.0F, 2.9F, "Beru");

    public enum Category { HUMANOID, BEAST, INSECT, CONSTRUCT, KNIGHT, DEMON }

    public final Rank rank;
    public final double hp, damage, armor, speed;
    public final int xp;
    public final Category category;
    public final boolean boss;
    public final float width, height;
    /** Name given to the shadow when this monster is extracted (null = generic). */
    public final String shadowName;

    MobKind(Rank rank, double hp, double damage, double armor, double speed, int xp, Category category, boolean boss,
            float width, float height, String shadowName) {
        this.rank = rank;
        this.hp = hp;
        this.damage = damage;
        this.armor = armor;
        this.speed = speed;
        this.xp = xp;
        this.category = category;
        this.boss = boss;
        this.width = width;
        this.height = height;
        this.shadowName = shadowName;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Max health is capped at 1024 by vanilla; the rest is turned into damage resistance. */
    public double cappedHp() { return Math.min(1000, hp); }

    public float damageTakenFactor() { return hp > 1000 ? (float) (1000 / hp) : 1F; }
}
