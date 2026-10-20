package com.krolasyon.elvenrealm.dialogue;

import com.krolasyon.elvenrealm.entity.ElfKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Oyuncunun elflerle ilişkisi (ölümde kaybolmaz): dostluk, görev durumu, hediye günü, yol arkadaşı. */
public final class PlayerData {
    public static final int HIRE_FRIENDSHIP = 6, MAX_FRIENDSHIP = 10;

    private PlayerData() {}

    public static CompoundTag root(Player p) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!p.getPersistentData().contains(Player.PERSISTED_NBT_TAG)) p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        if (!persisted.contains("elvenrealm")) persisted.put("elvenrealm", new CompoundTag());
        return persisted.getCompound("elvenrealm");
    }

    public static int friendship(Player p, ElfKind k) {
        return root(p).getInt("f_" + k.id);
    }

    public static void addFriendship(Player p, ElfKind k, int d) {
        root(p).putInt("f_" + k.id, Math.max(0, Math.min(MAX_FRIENDSHIP, friendship(p, k) + d)));
    }

    /** Sıradaki görevin numarası (0, 1; 2 = hepsi bitti). */
    public static int questIndex(Player p, ElfKind k) {
        return root(p).getInt("qi_" + k.id);
    }

    public static boolean questActive(Player p, ElfKind k) {
        return root(p).getBoolean("qa_" + k.id);
    }

    public static void startQuest(Player p, ElfKind k) {
        root(p).putBoolean("qa_" + k.id, true);
        root(p).putInt("qk_" + k.id, 0);
    }

    public static void finishQuest(Player p, ElfKind k) {
        CompoundTag t = root(p);
        t.putBoolean("qa_" + k.id, false);
        t.putInt("qk_" + k.id, 0);
        t.putInt("qi_" + k.id, questIndex(p, k) + 1);
    }

    public static int kills(Player p, ElfKind k) {
        return root(p).getInt("qk_" + k.id);
    }

    public static void addKill(Player p, ElfKind k) {
        root(p).putInt("qk_" + k.id, kills(p, k) + 1);
    }

    public static long lastGiftDay(Player p, ElfKind k) {
        return root(p).contains("g_" + k.id) ? root(p).getLong("g_" + k.id) : -1;
    }

    public static void setGiftDay(Player p, ElfKind k, long day) {
        root(p).putLong("g_" + k.id, day);
    }

    public static UUID companion(Player p) {
        return root(p).hasUUID("companion") ? root(p).getUUID("companion") : null;
    }

    public static void setCompanion(Player p, UUID id) {
        if (id == null) root(p).remove("companion");
        else root(p).putUUID("companion", id);
    }
}
