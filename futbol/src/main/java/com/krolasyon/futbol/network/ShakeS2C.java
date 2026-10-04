package com.krolasyon.futbol.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShakeS2C {
    public final int strength;

    public ShakeS2C(int strength) { this.strength = strength; }

    public ShakeS2C(FriendlyByteBuf buf) { this(buf.readVarInt()); }

    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(strength); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.futbol.client.ClientPackets.shake(this));
        ctx.get().setPacketHandled(true);
    }
}
