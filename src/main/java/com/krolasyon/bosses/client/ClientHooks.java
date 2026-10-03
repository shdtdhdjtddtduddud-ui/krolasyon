package com.krolasyon.bosses.client;

import com.krolasyon.bosses.client.gui.DialogueScreen;
import com.krolasyon.bosses.client.gui.JournalScreen;
import com.krolasyon.bosses.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Entry points called from network packets on the client (kept in their own class so servers never load client code). */
public final class ClientHooks {
    private ClientHooks() {}

    public static CompoundTag data = new CompoundTag();

    public static void sync(CompoundTag tag) {
        data = tag;
    }

    public static int rep(String factionId) { return data.getInt("rep_" + factionId); }

    public static float mana() { return data.contains("mana") ? data.getFloat("mana") : 100f; }

    public static void openDialogue(Net.DialogueMsg m) {
        Minecraft mc = Minecraft.getInstance();
        // replacing a page must not tell the server the previous one was "closed"
        mc.setScreen(new DialogueScreen(m));
    }

    public static void openJournal() {
        Minecraft.getInstance().setScreen(new JournalScreen());
    }
}
