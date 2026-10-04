package com.krolasyon.futbol.network;

import com.krolasyon.futbol.game.Move;
import com.krolasyon.futbol.game.MoveExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MoveC2S {
    public final int move;
    public final float charge;

    public MoveC2S(int move, float charge) {
        this.move = move;
        this.charge = charge;
    }

    public MoveC2S(FriendlyByteBuf buf) {
        this.move = buf.readVarInt();
        this.charge = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(move);
        buf.writeFloat(charge);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        Move m = Move.byId(move);
        if (p != null && m != null && !p.isSpectator()) MoveExecutor.perform(p, m, charge);
        ctx.get().setPacketHandled(true);
    }
}
