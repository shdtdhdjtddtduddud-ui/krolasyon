package com.krolasyon.futbol.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMenuS2C {
    public OpenMenuS2C() {}

    public OpenMenuS2C(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.krolasyon.futbol.client.ClientPackets::openMenu);
        ctx.get().setPacketHandled(true);
    }
}
