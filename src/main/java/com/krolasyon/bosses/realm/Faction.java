package com.krolasyon.bosses.realm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/** The five kingdoms of the Crimson Realm and their politics. */
public enum Faction {
    ASH("ash", 0xFF8A3D, ChatFormatting.GOLD),
    BLOOD("blood", 0xD8263A, ChatFormatting.RED),
    SHADOW("shadow", 0x9A5CFF, ChatFormatting.DARK_PURPLE),
    LEGION("legion", 0xE0B040, ChatFormatting.YELLOW),
    SOUL("soul", 0x4FD9FF, ChatFormatting.AQUA);

    public static final Faction[] ALL = values();

    // relation matrix: 1 = allied blocs, -1 = at war, 0 = neutral
    private static final int[][] REL = {
            //  ash blood shadow legion soul
            {0, 0, -1, 1, -1},   // ash
            {0, 0, 1, -1, -1},   // blood
            {-1, 1, 0, -1, 0},   // shadow
            {1, -1, -1, 0, 0},   // legion
            {-1, -1, 0, 0, 0}};  // soul

    public final String id;
    public final int color;
    public final ChatFormatting chat;

    Faction(String id, int color, ChatFormatting chat) {
        this.id = id;
        this.color = color;
        this.chat = chat;
    }

    public boolean atWarWith(Faction o) { return o != null && REL[ordinal()][o.ordinal()] < 0; }
    public boolean alliedWith(Faction o) { return o != null && REL[ordinal()][o.ordinal()] > 0; }

    public Component title() { return Component.translatable("faction.krolasyonbosses." + id).withStyle(chat); }
    public Component lord() { return Component.translatable("faction.krolasyonbosses." + id + ".lord"); }

    @Nullable
    public static Faction byId(int i) { return i >= 0 && i < ALL.length ? ALL[i] : null; }
}
