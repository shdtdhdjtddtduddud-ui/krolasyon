package com.rabona.arena.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum Team {
    NONE("izleyici", 0xBBBBBB, ChatFormatting.GRAY),
    RED("kirmizi", 0xE53935, ChatFormatting.RED),
    BLUE("mavi", 0x1E88E5, ChatFormatting.BLUE);

    public final String key;
    public final int color;
    public final ChatFormatting chat;

    Team(String key, int color, ChatFormatting chat) {
        this.key = key;
        this.color = color;
        this.chat = chat;
    }

    public Team opponent() {
        return this == RED ? BLUE : this == BLUE ? RED : NONE;
    }

    public boolean playing() { return this != NONE; }

    public Component displayName() {
        return Component.translatable("team.rabonaarena." + key).withStyle(chat);
    }

    public static Team byId(int i) {
        return i >= 0 && i < values().length ? values()[i] : NONE;
    }

    public static Team byKey(String k) {
        for (Team t : values()) if (t.key.equalsIgnoreCase(k) || t.name().equalsIgnoreCase(k)) return t;
        return NONE;
    }
}
