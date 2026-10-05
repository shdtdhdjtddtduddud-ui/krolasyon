package com.krolasyon.sololeveling.system;

import java.util.Locale;

/** The five System stats. */
public enum Stat {
    STR(0xFF6B6B), AGI(0x6BFFB5), VIT(0xFFC46B), INT(0x6BB5FF), PER(0xD36BFF);

    public final int color;

    Stat(int color) { this.color = color; }

    public String key() { return name().toLowerCase(Locale.ROOT); }
}
