package com.krolasyon.futbol.game;

import net.minecraft.ChatFormatting;

public enum Team {
    NONE("İzleyici", "İZL", 0xB0B0B0, ChatFormatting.GRAY),
    RED("Kızıl Aslanlar", "KIR", 0xE53935, ChatFormatting.RED),
    BLUE("Mavi Kartallar", "MAV", 0x1E88E5, ChatFormatting.BLUE);

    public final String title;
    public final String abbr;
    public final int color;
    public final ChatFormatting chat;

    Team(String title, String abbr, int color, ChatFormatting chat) {
        this.title = title;
        this.abbr = abbr;
        this.color = color;
        this.chat = chat;
    }

    public boolean playing() { return this != NONE; }

    public Team opponent() {
        return switch (this) {
            case RED -> BLUE;
            case BLUE -> RED;
            default -> NONE;
        };
    }

    /** +1: attacks towards +X, -1: attacks towards -X */
    public int attackDir() { return this == BLUE ? -1 : 1; }

    public static Team byId(int id) {
        Team[] v = values();
        return id >= 0 && id < v.length ? v[id] : NONE;
    }

    public static Team parse(String s) {
        s = s.toLowerCase(java.util.Locale.ROOT);
        if (s.startsWith("k") || s.startsWith("r")) return RED;
        if (s.startsWith("m") || s.startsWith("b")) return BLUE;
        return NONE;
    }
}
