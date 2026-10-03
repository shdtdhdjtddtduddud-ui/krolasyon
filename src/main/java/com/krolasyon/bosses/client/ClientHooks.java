package com.krolasyon.bosses.client;

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
}
