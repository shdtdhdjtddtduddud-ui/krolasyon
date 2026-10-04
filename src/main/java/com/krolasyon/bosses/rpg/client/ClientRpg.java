package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.rpg.client.gui.DialogScreen;
import com.krolasyon.bosses.rpg.client.gui.RpgScreen;
import com.krolasyon.bosses.rpg.net.RpgNet;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Client copy of the player's RPG data and the entry point for server → client packets. */
public final class ClientRpg {
    private ClientRpg() {}

    public static CompoundTag data = new CompoundTag();
    public static long lastSync;

    public static void handle(byte kind, CompoundTag t) {
        Minecraft mc = Minecraft.getInstance();
        switch (kind) {
            case RpgNet.SYNC -> {
                data = t;
                lastSync = System.currentTimeMillis();
                if (mc.screen instanceof RpgScreen s) s.refresh();
            }
            case RpgNet.DIALOG -> mc.setScreen(new DialogScreen(t));
            case RpgNet.CLOSE -> { if (mc.screen instanceof DialogScreen) mc.setScreen(null); }
            default -> {}
        }
    }
}
