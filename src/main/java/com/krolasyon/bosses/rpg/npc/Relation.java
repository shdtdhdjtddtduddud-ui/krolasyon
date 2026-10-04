package com.krolasyon.bosses.rpg.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/** How one NPC feels about one player. */
public class Relation {
    public int affinity;
    public int trust = 20;
    public long lastChatDay = -1;
    public long lastGiftDay = -1;
    public int chats;
    public boolean met, rescued, married, betrayed, enemy, questGiven;

    public void add(int aff, int tr) {
        affinity = Mth.clamp(affinity + aff, -100, 100);
        trust = Mth.clamp(trust + tr, 0, 100);
        if (affinity <= -60) enemy = true;
        if (affinity >= 0) enemy = false;
    }

    public String affinityTitle() {
        if (married) return "Eşin";
        if (affinity <= -60) return "Düşman";
        if (affinity <= -25) return "Hasım";
        if (affinity < 0) return "Soğuk";
        if (affinity < 15) return "Yabancı";
        if (affinity < 35) return "Tanıdık";
        if (affinity < 60) return "Arkadaş";
        if (affinity < 85) return "Dost";
        return "Can Dostu";
    }

    public String trustTitle() {
        if (trust < 15) return "Hiç güvenmiyor";
        if (trust < 35) return "Şüpheli";
        if (trust < 60) return "Güveniyor";
        if (trust < 85) return "Çok güveniyor";
        return "Canını emanet eder";
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("A", affinity);
        t.putInt("T", trust);
        t.putLong("Chat", lastChatDay);
        t.putLong("Gift", lastGiftDay);
        t.putInt("Chats", chats);
        int f = (met ? 1 : 0) | (rescued ? 2 : 0) | (married ? 4 : 0) | (betrayed ? 8 : 0) | (enemy ? 16 : 0) | (questGiven ? 32 : 0);
        t.putInt("F", f);
        return t;
    }

    public static Relation load(CompoundTag t) {
        Relation r = new Relation();
        r.affinity = t.getInt("A");
        r.trust = t.getInt("T");
        r.lastChatDay = t.getLong("Chat");
        r.lastGiftDay = t.getLong("Gift");
        r.chats = t.getInt("Chats");
        int f = t.getInt("F");
        r.met = (f & 1) != 0; r.rescued = (f & 2) != 0; r.married = (f & 4) != 0; r.betrayed = (f & 8) != 0; r.enemy = (f & 16) != 0; r.questGiven = (f & 32) != 0;
        return r;
    }
}
