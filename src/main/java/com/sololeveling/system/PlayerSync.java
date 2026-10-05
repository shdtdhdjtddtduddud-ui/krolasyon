package com.sololeveling.system;

import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.player.ModCaps;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerSync {
    private PlayerSync() {}

    public static void sync(ServerPlayer sp) {
        Net.toPlayer(sp, new Packets.Sync(ModCaps.get(sp).save()));
    }
}
