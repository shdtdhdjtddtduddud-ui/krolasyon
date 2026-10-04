package com.krolasyon.futbol.network;

import com.krolasyon.futbol.game.CardData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Club screen actions: sync, open pack, toggle squad, sell. */
public class ClubC2S {
    public static final int SYNC = 0, OPEN = 1, SQUAD = 2, SELL = 3;
    public final int action, arg;

    public ClubC2S(int action, int arg) {
        this.action = action;
        this.arg = arg;
    }

    public ClubC2S(FriendlyByteBuf b) { this(b.readVarInt(), b.readVarInt()); }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(action);
        b.writeVarInt(arg);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p == null) return;
        switch (action) {
            case OPEN -> CardData.openPack(p, arg);
            case SQUAD -> CardData.toggleSquad(p, arg);
            case SELL -> CardData.sell(p, arg);
            default -> CardData.sync(p, -1);
        }
    }
}
