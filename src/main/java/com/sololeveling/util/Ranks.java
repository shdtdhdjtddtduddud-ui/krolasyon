package com.sololeveling.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Rarity;

public final class Ranks {
    private Ranks() {}

    public static final String[] ORDER = {"E", "D", "C", "B", "A", "S", "N"};

    public static int index(String rank) {
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i].equals(rank)) return i;
        return 0;
    }

    public static int color(String rank) {
        return switch (rank) {
            case "E" -> 0xB0B8C4;
            case "D" -> 0x6BE070;
            case "C" -> 0x58A8FF;
            case "B" -> 0xC070FF;
            case "A" -> 0xFFB43C;
            case "S" -> 0xFF4A56;
            default -> 0x8A5CFF;
        };
    }

    public static Rarity rarity(String rank) {
        return switch (rank) {
            case "E" -> Rarity.COMMON;
            case "D", "C" -> Rarity.UNCOMMON;
            case "B", "A" -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }

    public static MutableComponent tag(String rank) {
        return Component.translatable("rank.sololeveling." + rank).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color(rank))).withBold(true));
    }

    /** rank of the gate that fits a player's level */
    public static String forLevel(int level) {
        if (level >= 100) return "N";
        if (level >= 80) return "S";
        if (level >= 60) return "A";
        if (level >= 40) return "B";
        if (level >= 25) return "C";
        if (level >= 10) return "D";
        return "E";
    }
}
