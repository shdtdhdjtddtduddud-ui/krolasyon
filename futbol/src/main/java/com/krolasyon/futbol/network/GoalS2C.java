package com.krolasyon.futbol.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GoalS2C {
    public final int team;
    public final String scorer;
    public final boolean own;
    public final int red, blue;

    public GoalS2C(int team, String scorer, boolean own, int red, int blue) {
        this.team = team;
        this.scorer = scorer;
        this.own = own;
        this.red = red;
        this.blue = blue;
    }

    public GoalS2C(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readUtf(64), buf.readBoolean(), buf.readVarInt(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(team);
        buf.writeUtf(scorer, 64);
        buf.writeBoolean(own);
        buf.writeVarInt(red);
        buf.writeVarInt(blue);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.futbol.client.ClientPackets.goal(this));
        ctx.get().setPacketHandled(true);
    }
}
