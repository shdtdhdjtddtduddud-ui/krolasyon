package com.krolasyon.bosses.faction;

import javax.annotation.Nullable;

/**
 * The five great houses of Azrakor (arranged as a pentagram: neighbours are allies, the two houses
 * across are blood enemies) plus the unaligned Outcasts.
 */
public enum Faction {
    EMBER("ember", "Kor Hanedanı", "Ember Dominion", 0xFF7A2E),
    BONE("bone", "Kemik Krallığı", "Bone Kingdom", 0xE8E0C8),
    BLOOD("blood", "Kan Konseyi", "Blood Conclave", 0xC41E3A),
    SHADOW("shadow", "Gölge Tarikatı", "Shadow Covenant", 0x8A5CFF),
    ROT("rot", "Çürük Divanı", "Rot Court", 0x7DB03A),
    OUTCAST("outcast", "Yetimler", "Outcasts", 0x9A9AA6);

    public final String id;
    public final String trName;
    public final String enName;
    public final int color;

    Faction(String id, String trName, String enName, int color) {
        this.id = id;
        this.trName = trName;
        this.enName = enName;
        this.color = color;
    }

    public static final Faction[] HOUSES = {EMBER, BONE, BLOOD, SHADOW, ROT};

    public boolean isHouse() { return this != OUTCAST; }

    public int idx() { return ordinal(); }

    public boolean isAllyOf(Faction o) {
        if (!isHouse() || !o.isHouse() || o == this) return false;
        int d = Math.abs(ordinal() - o.ordinal());
        return d == 1 || d == 4;
    }

    public boolean isEnemyOf(Faction o) {
        if (!isHouse() || !o.isHouse() || o == this) return false;
        int d = Math.abs(ordinal() - o.ordinal());
        return d == 2 || d == 3;
    }

    @Nullable
    public static Faction byId(String id) {
        for (Faction f : values()) if (f.id.equals(id)) return f;
        return null;
    }
}
