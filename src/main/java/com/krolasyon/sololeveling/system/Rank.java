package com.krolasyon.sololeveling.system;

/** Hunter / gate ranks. */
public enum Rank {
    E("E", 0x9AA7B8, 1), D("D", 0x5FA8FF, 10), C("C", 0x5CE07A, 25), B("B", 0xF2D24B, 45),
    A("A", 0xFF8A3D, 65), S("S", 0xC86BFF, 85), NATIONAL("N", 0xFF3D5A, 110);

    public final String label;
    public final int color;
    /** Minimum hunter level that the Association assesses as this rank. */
    public final int minLevel;

    Rank(String label, int color, int minLevel) {
        this.label = label;
        this.color = color;
        this.minLevel = minLevel;
    }

    public static Rank forLevel(int level) {
        Rank r = E;
        for (Rank k : values()) if (level >= k.minLevel) r = k;
        return r;
    }

    public static Rank byOrdinal(int o) {
        Rank[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, o))];
    }

    public String langKey() { return "sololeveling.rank." + label; }
}
