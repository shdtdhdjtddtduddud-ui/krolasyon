package com.krolasyon.labirent.team;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Jobs of the Glade. Players and NPC Gladers both carry one. */
public enum Role {
    SERBEST("serbest", ChatFormatting.GRAY, 0xA0A0A0),
    KOSUCU("kosucu", ChatFormatting.AQUA, 0x4FD0FF),
    ASCI("asci", ChatFormatting.GOLD, 0xFFB040),
    BAHCIVAN("bahcivan", ChatFormatting.GREEN, 0x60D060),
    KASAP("kasap", ChatFormatting.RED, 0xD04848),
    SIFACI("sifaci", ChatFormatting.LIGHT_PURPLE, 0xF080F0),
    BEKCI("bekci", ChatFormatting.YELLOW, 0xF0E040);

    public final String id;
    public final ChatFormatting color;
    public final int rgb;

    Role(String id, ChatFormatting color, int rgb) {
        this.id = id;
        this.color = color;
        this.rgb = rgb;
    }

    public MutableComponent title() { return Component.translatable("role.labirent." + id).withStyle(color); }

    public MutableComponent description() { return Component.translatable("role.labirent." + id + ".desc"); }

    public static Role byId(int ord) { return ord >= 0 && ord < values().length ? values()[ord] : SERBEST; }

    public static Role byName(String s) {
        for (Role r : values()) if (r.id.equalsIgnoreCase(s) || r.name().equalsIgnoreCase(s)) return r;
        return null;
    }
}
